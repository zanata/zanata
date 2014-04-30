/*
 * Copyright 2010, Red Hat, Inc. and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.zanata.service.impl;

import static org.zanata.common.ContentState.Approved;
import static org.zanata.common.ContentState.NeedReview;
import static org.zanata.common.ContentState.New;
import static org.zanata.common.ContentState.Translated;
import static org.zanata.model.HCopyTransOptions.ConditionRuleAction.DOWNGRADE_TO_FUZZY;
import static org.zanata.model.HCopyTransOptions.ConditionRuleAction.REJECT;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.util.Work;
import org.zanata.async.AsyncUtils;
import org.zanata.async.tasks.CopyTransTask.CopyTransTaskHandle;
import org.zanata.common.ContentState;
import org.zanata.dao.DocumentDAO;
import org.zanata.dao.ProjectDAO;
import org.zanata.dao.TextFlowTargetDAO;
import org.zanata.events.TextFlowTargetStateEvent;
import org.zanata.model.HCopyTransOptions;
import org.zanata.model.HDocument;
import org.zanata.model.HLocale;
import org.zanata.model.HProjectIteration;
import org.zanata.model.HSimpleComment;
import org.zanata.model.HTextFlow;
import org.zanata.model.HTextFlowTarget;
import org.zanata.rest.service.TranslatedDocResourceService;
import org.zanata.service.CopyTransService;
import org.zanata.service.LocaleService;
import org.zanata.service.ValidationService;
import org.zanata.service.VersionStateCache;
import org.zanata.webtrans.shared.model.ValidationAction;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

//TODO unit test suite for this class

@Name("copyTransServiceImpl")
@Scope(ScopeType.STATELESS)
@Slf4j
public class CopyTransServiceImpl implements CopyTransService {

    private static final int COPY_TRANS_BATCH_SIZE = 20;

    @In
    private LocaleService localeServiceImpl;

    @In
    private TextFlowTargetDAO textFlowTargetDAO;

    @In
    private DocumentDAO documentDAO;

    @In
    private ProjectDAO projectDAO;

    @In
    private ValidationService validationServiceImpl;

    @In
    private VersionStateCache versionStateCacheImpl;

    @Observer(TranslatedDocResourceService.EVENT_COPY_TRANS)
    public void runCopyTrans(Long docId, String project, String iterationSlug) {
        HDocument document = documentDAO.findById(docId, true);
        log.info("copyTrans start: document \"{}\"", document.getDocId());
        List<HLocale> localelist =
                localeServiceImpl.getSupportedLanguageByProjectIteration(
                    project, iterationSlug);

        // TODO iterate over document's textflows, then call
        // copyTransForTextFlow(textFlow, localeList)
        // refer patch from https://bugzilla.redhat.com/show_bug.cgi?id=746899
        for (HLocale locale : localelist) {
            copyTransForLocale(document, locale);
        }
        log.info("copyTrans finished: document \"{}\"", document.getDocId());
    }

    private String createComment(HTextFlowTarget target) {
        String author;
        HDocument document = target.getTextFlow().getDocument();
        String projectname =
                document.getProjectIteration().getProject().getName();
        String version = document.getProjectIteration().getSlug();
        String documentid = document.getDocId();
        if (target.getLastModifiedBy() != null) {
            author = ", author " + target.getLastModifiedBy().getName();
        } else {
            author = "";
        }

        return "translation auto-copied from project " + projectname
                + ", version " + version + ", document " + documentid + author;
    }

    @Override
    public void copyTransForLocale(HDocument document, HLocale locale) {
        this.copyTransForLocale(document.getId(), locale, new HCopyTransOptions());
    }

    public void copyTransForLocale(final Long documentId, final HLocale locale,
            final HCopyTransOptions options) {

        int numCopied = 0;
        int start = 0;
        HDocument document = documentDAO.findById(documentId);

        while (start < document.getTextFlows().size()) {
            numCopied +=
                    copyTransForBatch(documentId, start,
                            COPY_TRANS_BATCH_SIZE, locale, options);
            start += COPY_TRANS_BATCH_SIZE;
        }

        // Advance the task handler if there is one
        Optional<CopyTransTaskHandle> taskHandle =
                AsyncUtils.getEventAsyncHandle(CopyTransTaskHandle.class);
        if (taskHandle.isPresent()) {
            taskHandle.get().increaseProgress(1);
        }

        log.info("copyTrans: {} {} translations for document \"{}{}\" ",
                numCopied, locale.getLocaleId(), document.getPath(),
                document.getName());
    }

    /**
     * Perform copy trans on a batch of text flows for a document.
     * @param batchStart The text flow position to start copying.
     * @param batchLength The number of text flows on which to perform copy
     *                      trans, starting from batchStart.
     * @return The number of actual copied translations for the segment.
     */
    private int copyTransForBatch(final Long documentId,
            final int batchStart, final int batchLength,
            final HLocale locale, final HCopyTransOptions options) {

        try {
            return new Work<Integer>() {
                @Override
                protected Integer work() throws Exception {

                    int numCopied = 0;
                    boolean checkContext = false,
                            checkProject = false,
                            checkDocument = false;
                    HDocument document = documentDAO.findById(documentId);
                    boolean requireTranslationReview =
                            document.getProjectIteration()
                                    .getRequireTranslationReview();

                    // Only outright reject copies if the options say so
                    if (options.getDocIdMismatchAction() == REJECT) {
                        checkDocument = true;
                    }
                    if (options.getProjectMismatchAction() == REJECT) {
                        checkProject = true;
                    }
                    if (options.getContextMismatchAction() == REJECT) {
                        checkContext = true;
                    }

                    List<HTextFlow> docTextFlowTargets =
                            document.getTextFlows();
                    int batchEnd = Math.min(batchStart + batchLength,
                            docTextFlowTargets.size());
                    List<HTextFlow> copyTargets =
                            docTextFlowTargets
                                    .subList(batchStart, batchEnd);

                    for (HTextFlow textFlow : copyTargets) {
                        if (shouldFindMatch(textFlow, locale,
                                requireTranslationReview)) {
                            Optional<HTextFlowTarget> bestMatch =
                                    textFlowTargetDAO
                                            .findBestMatchingTranslation(
                                                    textFlow, locale,
                                                    checkContext,
                                                    checkDocument, checkProject);

                            if (bestMatch.isPresent()) {
                                numCopied++;

                                saveCopyTransMatch(bestMatch.get(), textFlow,
                                        options, requireTranslationReview);

                            }
                        }
                    }

                    return numCopied;
                }
            }.workInTransaction();
        } catch (Exception e) {
            log.warn("exception during copy trans", e);
            return 0;
        }
    }

    /**
     * Indicates if a given text flow should have a match found for a given
     * locale, or if it is already good enough.
     */
    private boolean shouldFindMatch(HTextFlow textFlow, HLocale locale,
        boolean requireTranslationReview) {
        HTextFlowTarget targetForLocale = textFlow.getTargets().get(locale.getId());

        if (targetForLocale == null ||
            targetForLocale.getState() == NeedReview) {
            return true;
        } else if (requireTranslationReview && targetForLocale.getState() != Approved) {
            return true;
        } else if (!requireTranslationReview && targetForLocale.getState() != Translated) {
            return true;
        } else {
            return false;
        }
    }

    private void saveCopyTransMatch(HTextFlowTarget matchingTarget,
        HTextFlow originalTf, HCopyTransOptions options,
        boolean requireTranslationReview) {
        HProjectIteration matchingTargetProjectIteration =
            matchingTarget.getTextFlow().getDocument()
                .getProjectIteration();
        ContentState copyState =
            determineContentState(
                originalTf.getResId()
                    .equals(matchingTarget.getTextFlow()
                        .getResId()),
                originalTf
                    .getDocument()
                    .getProjectIteration()
                    .getProject()
                    .getId()
                    .equals(matchingTargetProjectIteration
                        .getProject().getId()),
                originalTf
                    .getDocument()
                    .getDocId()
                    .equals(matchingTarget.getTextFlow()
                        .getDocument().getDocId()),
                options, requireTranslationReview,
                matchingTarget.getState());

        boolean hasValidationError =
            validationTranslations(copyState,
                matchingTargetProjectIteration,
                originalTf.getContents(),
                matchingTarget.getContents());

        if (hasValidationError) {
            return;
        }

        HTextFlowTarget hTarget =
            textFlowTargetDAO.getOrCreateTarget(originalTf, matchingTarget.getLocale());
        ContentState prevState = hTarget.getId() == null ? New : hTarget.getState();
        if (shouldOverwrite(hTarget, copyState)) {
            // NB we don't touch creationDate
            hTarget.setTextFlowRevision(originalTf.getRevision());
            hTarget.setLastChanged(matchingTarget.getLastChanged());
            hTarget.setLastModifiedBy(matchingTarget
                .getLastModifiedBy());
            hTarget.setTranslator(matchingTarget.getTranslator());
            // TODO rhbz953734 - will need a new copyTran option for
            // review state
            if (copyState == ContentState.Approved) {
                hTarget.setReviewer(matchingTarget.getReviewer());
            }
            hTarget.setContents(matchingTarget.getContents());
            hTarget.setState(copyState);
            HSimpleComment hcomment = hTarget.getComment();
            if (hcomment == null) {
                hcomment = new HSimpleComment();
                hTarget.setComment(hcomment);
            }
            hcomment.setComment(createComment(matchingTarget));

            // TODO Maybe we should think about registering a Hibernate
            // integrator for these updates
            signalCopiedTranslation(hTarget, prevState);
        }
    }

    /**
     * Determines the content state for a translation given a list of rules and
     * their evaluation result, and the initial state that it was copied as.
     *
     * @param pairs
     *            List of evaluated rules and the match result.
     * @param initialState
     *            The initial content state of the translation that the content
     *            was copied from.
     * @return The content state that the copied translation should have. 'New'
     *         indicates that the translation should not copied.
     */
    private static ContentState determineContentStateFromMatchRules(
            List<MatchRulePair> pairs, ContentState initialState) {
        if (pairs.isEmpty()) {
            return initialState;
        }

        MatchRulePair p = pairs.get(0);
        if (shouldReject(p.getMatchResult(), p.getRuleAction())) {
            return New;
        } else if (shouldDowngradeToFuzzy(p.getMatchResult(), p.getRuleAction())) {
            return determineContentStateFromMatchRules(
                    pairs.subList(1, pairs.size()), NeedReview);
        } else {
            return determineContentStateFromMatchRules(
                    pairs.subList(1, pairs.size()), initialState);
        }
    }

    /**
     * Determines the content state for a translation given a list of rules and
     * their evaluation result.
     *
     * @param pairs
     *            List of evaluated rules and their result.
     * @param requireTranslationReview
     *            Whether the project to copy the translation to requires
     *            translations to be reviewed.
     * @param matchingTargetState
     *            The initial state of the matching translation (the translation
     *            that will be copied over).
     * @return The content state that the copied translation should have. 'New'
     *         indicates that the translation should not copied.
     */
    static ContentState determineContentStateFromRuleList(
            List<MatchRulePair> pairs, boolean requireTranslationReview,
            ContentState matchingTargetState) {
        assert matchingTargetState == Translated
                || matchingTargetState == Approved;
        return determineContentStateFromMatchRules(pairs,
                requireTranslationReview ? matchingTargetState : Translated);
    }

    /**
     * Determines the content state that a copied translation should have.
     *
     * @param contextMatches
     *            Indicates if there is a context match between the match and
     *            copy-target text flows.
     * @param projectMatches
     *            Indicates if there is a project match between the match and
     *            copy-target text flows.
     * @param docIdMatches
     *            Indicates if there is a doc Id match between the match and
     *            copy-target text flows.
     * @param options
     *            The copy trans options that are effective.
     * @param requireTranslationReview
     *            Whether the project to copy the translation to requires
     *            translations to be reviewed.
     * @param matchingTargetState
     *            he initial state of the matching translation (the translation
     *            that will be copied over).
     * @return The content state that the copied translation should have. 'New'
     *         indicates that the translation should not copied.
     */
    static ContentState determineContentState(boolean contextMatches,
            boolean projectMatches, boolean docIdMatches,
            HCopyTransOptions options, boolean requireTranslationReview,
            ContentState matchingTargetState) {
        List rules =
                ImmutableList.of(
                        new MatchRulePair(contextMatches, options
                                .getContextMismatchAction()),
                        new MatchRulePair(projectMatches, options
                                .getProjectMismatchAction()),
                        new MatchRulePair(docIdMatches, options
                                .getDocIdMismatchAction()));

        return determineContentStateFromRuleList(rules,
                requireTranslationReview, matchingTargetState);
    }

    /**
     * Indicates if a copied translation should be rejected.
     *
     * @param match
     *            The result of a match evaluating condition.
     * @param action
     *            The selected action to take based on the result of the
     *            condition evaluation.
     * @return True, if the translation should be outright rejected based on the
     *         evaluated condition.
     */
    static boolean shouldReject(boolean match,
            HCopyTransOptions.ConditionRuleAction action) {
        return !match && action == REJECT;
    }

    /**
     * Indicates if a copied translation should be downgraded to fuzzy/
     *
     * @param match
     *            The result of a match evaluating condition.
     * @param action
     *            The selected action to take based on the result of the
     *            condition evaluation.
     * @return True, if the translation should be downgraded to fuzzy based on
     *         the evaluated condition.
     */
    static boolean shouldDowngradeToFuzzy(boolean match,
            HCopyTransOptions.ConditionRuleAction action) {
        return !match && action == DOWNGRADE_TO_FUZZY;
    }

    @Override
    public void copyTransForDocument(HDocument document) {
        copyTransForDocument(document, null);
    }

    @Override
    public void copyTransForDocument(HDocument document,
            HCopyTransOptions copyTransOpts) {
        // use project level options
        if (copyTransOpts == null) {
            // NB: Need to reload the options from the db
            copyTransOpts =
                    projectDAO
                            .findById(
                                    document.getProjectIteration().getProject()
                                            .getId(), false)
                            .getDefaultCopyTransOpts();
        }
        // use the global default options
        if (copyTransOpts == null) {
            copyTransOpts = new HCopyTransOptions();
        }

        log.info("copyTrans start: document \"{}\"", document.getDocId());
        Optional<CopyTransTaskHandle> taskHandleOpt =
                AsyncUtils.getEventAsyncHandle(CopyTransTaskHandle.class);
        List<HLocale> localeList =
                localeServiceImpl.getSupportedLanguageByProjectIteration(
                    document.getProjectIteration().getProject().getSlug(),
                    document.getProjectIteration().getSlug());

        for (HLocale locale : localeList) {
            if (taskHandleOpt.isPresent() && taskHandleOpt.get().isCancelled()) {
                return;
            }
            copyTransForLocale(document.getId(), locale, copyTransOpts);
        }

        if (taskHandleOpt.isPresent()) {
            taskHandleOpt.get().incrementDocumentsProcessed();
        }
        log.info("copyTrans finished: document \"{}\"", document.getDocId());
    }

    @Override
    public void copyTransForIteration(HProjectIteration iteration,
            HCopyTransOptions copyTransOptions) {
        Optional<CopyTransTaskHandle> taskHandleOpt =
                AsyncUtils.getEventAsyncHandle(CopyTransTaskHandle.class);

        for (HDocument doc : iteration.getDocuments().values()) {
            if (taskHandleOpt.isPresent() && taskHandleOpt.get().isCancelled()) {
                return;
            }
            this.copyTransForDocument(doc, copyTransOptions);
        }
    }

    /**
     * Indicates if a Copy Trans found match should overwrite the currently
     * stored one based on their states.
     */
    private static boolean shouldOverwrite(HTextFlowTarget currentlyStored,
            ContentState matchState) {
        if (matchState == New) {
            return false;
        } else if (currentlyStored != null) {
            if (currentlyStored.getState().isRejectedOrFuzzy()
                    && matchState.isTranslated()) {
                return true; // If it's fuzzy, replace only with approved ones
            } else if (currentlyStored.getState() == ContentState.Translated
                    && matchState.isApproved()) {
                return true; // If it's Translated and found an Approved one
            } else if (currentlyStored.getState() == New) {
                return true; // If it's new, replace always
            } else {
                return false;
            }
        }
        return true;
    }

    /**
     * Run enforced validation check(Error) if translation is saving as
     * 'Translated'.
     *
     * @param newState
     * @param projectVersion
     * @param sources
     * @param translations
     * @return if translation has validation error
     */
    private boolean validationTranslations(ContentState newState,
            HProjectIteration projectVersion, List<String> sources,
            List<String> translations) {
        if (newState.isTranslated()) {
            List<String> validationMessages =
                    validationServiceImpl.validateWithServerRules(
                            projectVersion, sources, translations,
                            ValidationAction.State.Error);

            if (!validationMessages.isEmpty()) {
                log.warn(validationMessages.toString());
                return true;
            }
        }
        return false;
    }

    private void signalCopiedTranslation(HTextFlowTarget target,
            ContentState previousState) {
        /* Using a direct method call instead of an event because it's easier to
         * read. Since these events are being called synchronously (as opposed
         * to an 'after Transaction' events), there is no big performance gain
         * and makes the code easier to read and navigate.
         */
        HDocument document = target.getTextFlow()
                .getDocument();
        TextFlowTargetStateEvent updateEvent =
                new TextFlowTargetStateEvent(null, document
                        .getProjectIteration().getId(), document.getId(), target
                        .getTextFlow().getId(), target.getLocaleId(),
                        target.getId(), target.getState(), previousState);
        versionStateCacheImpl.textFlowStateUpdated(updateEvent);
    }

    /**
     * Holds the result of a match evaluation in the form of a boolean, and the
     * corresponding action to be taken for the result.
     */
    @AllArgsConstructor
    @Getter
    static final class MatchRulePair {
        private final Boolean matchResult;
        private final HCopyTransOptions.ConditionRuleAction ruleAction;
    }
}
