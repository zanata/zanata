/*
 * Copyright 2012, Red Hat, Inc. and individual contributors as indicated by the
 * @author tags. See the copyright.txt file in the distribution for a full
 * listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this software; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA, or see the FSF
 * site: http://www.fsf.org.
 */
package org.zanata.service.impl;

import java.util.*;

import javax.annotation.*;
import javax.persistence.*;

import lombok.extern.slf4j.*;

import org.apache.commons.lang.*;
import org.hibernate.*;
import org.jboss.seam.*;
import org.jboss.seam.annotations.*;
import org.jboss.seam.core.*;
import org.jboss.seam.security.management.*;
import org.jboss.seam.util.*;
import org.zanata.async.*;
import org.zanata.common.*;
import org.zanata.common.util.*;
import org.zanata.dao.*;
import org.zanata.events.*;
import org.zanata.exception.*;
import org.zanata.lock.*;
import org.zanata.model.*;
import org.zanata.rest.dto.resource.*;
import org.zanata.rest.service.*;
import org.zanata.security.*;
import org.zanata.service.*;
import org.zanata.webtrans.shared.model.*;

import com.google.common.base.*;
import com.google.common.collect.*;

@Name("translationServiceImpl")
@Scope(ScopeType.STATELESS)
@Transactional
@Slf4j
public class TranslationServiceImpl implements TranslationService
{

   @In
   private EntityManager entityManager;

   @In
   private ProjectIterationDAO projectIterationDAO;

   @In
   private DocumentDAO documentDAO;

   @In
   private PersonDAO personDAO;

   @In
   private TextFlowDAO textFlowDAO;

   @In
   private TextFlowTargetDAO textFlowTargetDAO;

   @In
   private ResourceUtils resourceUtils;

   @In
   private LocaleService localeServiceImpl;

   @In
   private LockManagerService lockManagerServiceImpl;

   @In
   private ValidationService validationServiceImpl;

   @In(value = JpaIdentityStore.AUTHENTICATED_USER, scope = ScopeType.SESSION, required = false)
   private HAccount authenticatedAccount;

   @In
   private ZanataIdentity identity;

   @In
   private TranslationMergeServiceFactory translationMergeServiceFactory;

   @Override
   public List<TranslationResult> translate(LocaleId localeId, List<TransUnitUpdateRequest> translationRequests)
   {
      return translate(localeId, translationRequests, true);
   }

   /**
    * This is used when reverting translation
    *
    * @param localeId
    * @param translationRequests
    * @return
    */
   private List<TranslationResult> translateWithoutValidating(LocaleId localeId, List<TransUnitUpdateRequest> translationRequests)
   {
      return translate(localeId, translationRequests, false);
   }

   private List<TranslationResult> translate(LocaleId localeId, List<TransUnitUpdateRequest> translationRequests, boolean runValidation)
   {
      List<TranslationResult> results = new ArrayList<TranslationResult>();

      // avoid locale check if there is nothing to translate
      if (translationRequests.isEmpty())
      {
         return results;
      }

      // single locale check - assumes update requests are all from the same
      // project-iteration
      HTextFlow sampleHTextFlow = entityManager.find(HTextFlow.class, translationRequests.get(0).getTransUnitId()
            .getValue());
      HProjectIteration projectIteration = sampleHTextFlow.getDocument().getProjectIteration();
      HLocale hLocale = validateLocale(localeId, projectIteration);

      // single permission check - assumes update requests are all from same project
      validateReviewPermissionIfApplicable(translationRequests, projectIteration, hLocale);

      for (TransUnitUpdateRequest request : translationRequests)
      {
         HTextFlow hTextFlow = entityManager.find(HTextFlow.class, request.getTransUnitId().getValue());

         TranslationResultImpl result = new TranslationResultImpl();

         if (runValidation)
         {
            String validationMessage = validateTranslations(request.getNewContentState(), projectIteration, request
                  .getTransUnitId().toString(), hTextFlow.getContents(), request.getNewContents());

            if (!StringUtils.isEmpty(validationMessage))
            {
               log.warn(validationMessage);
               result.isSuccess = false;
               result.errorMessage = validationMessage;
               continue;
            }
         }

         HTextFlowTarget hTextFlowTarget = textFlowTargetDAO.getOrCreateTarget(hTextFlow, hLocale);
         // if hTextFlowTarget is created, any further hibernate fetch will trigger an implicit flush
         // (which will save this target even if it's not fully ready!!!)
         if (request.hasTargetComment())
         {
            hTextFlowTarget.setComment(new HSimpleComment(request.getTargetComment()));
         }

         result.baseVersion = hTextFlowTarget.getVersionNum();
         result.baseContentState = hTextFlowTarget.getState();

         if (request.getBaseTranslationVersion() == hTextFlowTarget.getVersionNum())
         {
            try
            {
               int nPlurals = getNumPlurals(hLocale, hTextFlow);
               result.targetChanged = translate(hTextFlowTarget, request.getNewContents(),
                     request.getNewContentState(),
                     nPlurals, projectIteration.getRequireTranslationReview());
               result.isSuccess = true;
            }
            catch (HibernateException e)
            {
               result.isSuccess = false;
               log.warn("HibernateException while translating");
            }
         }
         else
         {
            // concurrent edits not allowed
            String errorMessage = "translation failed for textflow " + hTextFlow.getId() + ": base versionNum + "
                  + request.getBaseTranslationVersion() + " does not match current versionNum "
                  + hTextFlowTarget.getVersionNum();

            log.warn(errorMessage);

            result.errorMessage = errorMessage;
            result.isSuccess = false;
         }
         result.translatedTextFlowTarget = hTextFlowTarget;
         results.add(result);
      }

      return results;
   }

   private void validateReviewPermissionIfApplicable(List<TransUnitUpdateRequest> translationRequests,
         HProjectIteration projectIteration,
         HLocale hLocale)
   {
      Optional<TransUnitUpdateRequest> hasReviewRequest = Iterables.tryFind(translationRequests,
            new Predicate<TransUnitUpdateRequest>()
            {
               @Override
               public boolean apply(TransUnitUpdateRequest input)
               {
                  return isReviewState(input.getNewContentState());
               }
            });
      if (hasReviewRequest.isPresent())
      {
         identity.checkPermission("translation-review", projectIteration.getProject(), hLocale);
      }
   }

   private static boolean isReviewState(ContentState contentState)
   {
      return contentState == ContentState.Approved || contentState == ContentState.Rejected;
   }

   /**
    * Generate a {@link HLocale} for the given localeId and check that
    * translations for this locale are permitted.
    * 
    * @param localeId
    * @param projectIteration
    * @return the valid hLocale
    * @throws ZanataServiceException if the locale is not enabled for the
    *            project-iteration or server
    */
   private HLocale validateLocale(LocaleId localeId, HProjectIteration projectIteration) throws ZanataServiceException
   {
      String projectSlug = projectIteration.getProject().getSlug();
      return localeServiceImpl.validateLocaleByProjectIteration(localeId, projectSlug, projectIteration.getSlug());
   }

   /**
    * Sends out an event to signal that a Text Flow target has been translated
    */
   private void signalPostTranslateEvent(HTextFlowTarget hTextFlowTarget)
   {
      if (Events.exists())
      {
         HTextFlow textFlow = hTextFlowTarget.getTextFlow();
         HDocument document = textFlow.getDocument();
         // TODO remove hasError from DocumentStatus, so that we can pass everything else directly to cache
         //         DocumentStatus docStatus = new DocumentStatus(
         //               new DocumentId(document.getId(), document.getDocId()), hasError,
         //               hTextFlowTarget.getLastChanged(), hTextFlowTarget.getLastModifiedBy().getAccount().getUsername());

         Events.instance().raiseTransactionSuccessEvent(
               TextFlowTargetStateEvent.EVENT_NAME,
               new TextFlowTargetStateEvent(
                     document.getId(),
                     textFlow.getId(),
                     hTextFlowTarget.getLocale().getLocaleId(),
                     hTextFlowTarget.getId(),
                     hTextFlowTarget.getState()
               ));
      }
   }

   private boolean translate(@Nonnull HTextFlowTarget hTextFlowTarget, @Nonnull List<String> contentsToSave,
         ContentState requestedState,
         int nPlurals, Boolean requireTranslationReview)
   {
      boolean targetChanged = false;
      targetChanged |= setContentIfChanged(hTextFlowTarget, contentsToSave);
      targetChanged |= setContentStateIfChanged(requestedState, hTextFlowTarget, nPlurals, requireTranslationReview);

      if (targetChanged || hTextFlowTarget.getVersionNum() == 0)
      {
         HTextFlow textFlow = hTextFlowTarget.getTextFlow();
         hTextFlowTarget.setVersionNum(hTextFlowTarget.getVersionNum() + 1);
         hTextFlowTarget.setTextFlowRevision(textFlow.getRevision());
         hTextFlowTarget.setLastModifiedBy(authenticatedAccount.getPerson());
         log.debug("last modified by :{}", authenticatedAccount.getPerson().getName());
      }

      // save the target histories
      entityManager.flush();

      //fire event after flush
      if (targetChanged || hTextFlowTarget.getVersionNum() == 0)
      {
         this.signalPostTranslateEvent(hTextFlowTarget);
      }

      return targetChanged;
   }

   /**
    * @return true if the content was changed, false otherwise
    */
   private boolean setContentIfChanged(@Nonnull HTextFlowTarget hTextFlowTarget, @Nonnull List<String> contentsToSave)
   {
      if (!contentsToSave.equals(hTextFlowTarget.getContents()))
      {
         hTextFlowTarget.setContents(contentsToSave);
         return true;
      }
      else
      {
         return false;
      }
   }

   /**
    * Check that requestedState is valid for the given content, adjust if
    * necessary and set the new state if it has changed.
    * 
    * @return true if the content state or contents list were updated, false
    *         otherwise
    * @see #adjustContentsAndState(org.zanata.model.HTextFlowTarget, int,
    *      java.util.List)
    */
   private boolean setContentStateIfChanged(@Nonnull ContentState requestedState, @Nonnull HTextFlowTarget target,
         int nPlurals,
         boolean requireTranslationReview)
   {
      boolean changed = false;
      ContentState previousState = target.getState();
      target.setState(requestedState);
      ArrayList<String> warnings = new ArrayList<String>();
      changed |= adjustContentsAndState(target, nPlurals, warnings);
      for (String warning : warnings)
      {
         log.warn(warning);
      }
      if (requireTranslationReview)
      {
         if (isReviewState(target.getState()))
         {
            // reviewer saved it
            target.setReviewer(authenticatedAccount.getPerson());
         }
         else
         {
            target.setTranslator(authenticatedAccount.getPerson());
         }
      }
      else
      {
         if (target.getState() == ContentState.Approved)
         {
            target.setState(ContentState.Translated);
         }
         target.setTranslator(authenticatedAccount.getPerson());
      }
      if (target.getState() != previousState)
      {
         changed = true;
      }
      return changed;
   }

   /**
    * Checks target state against its contents. If necessary, modifies target
    * state and generates a warning
    * 
    * @param target HTextFlowTarget to check/modify
    * @param nPlurals number of plurals for this locale for this message: use 1
    *           if message does not support plurals
    * @param warnings a warning string will be added if state is adjusted
    * @return true if and only if some state was changed
    */
   private static boolean adjustContentsAndState(@Nonnull HTextFlowTarget target, int nPlurals,
         @Nonnull List<String> warnings)
   {
      ContentState oldState = target.getState();
      String resId = target.getTextFlow().getResId();
      boolean contentsChanged = ensureContentsSize(target, nPlurals, resId, warnings);

      List<String> contents = target.getContents();
      target.setState(ContentStateUtil.determineState(oldState, contents, resId, warnings));
      boolean stateChanged = (oldState != target.getState());
      return contentsChanged || stateChanged;
   }

   /**
    * Ensures that target.contents has exactly legalSize elements
    * 
    * @param target HTextFlowTarget to check/modify
    * @param legalSize required number of contents
    * @param resId ID of target
    * @param warnings if elements were added or removed
    * @return
    */
   private static boolean ensureContentsSize(HTextFlowTarget target, int legalSize, String resId,
         @Nonnull List<String> warnings)
   {
      int contentsSize = target.getContents().size();
      if (contentsSize < legalSize)
      {
         String warning = "Should have " + legalSize + " contents; adding empty strings: TextFlowTarget " + resId
               + " with contents: " + target.getContents();
         warnings.add(warning);
         List<String> newContents = new ArrayList<String>(legalSize);
         newContents.addAll(target.getContents());
         while (newContents.size() < legalSize)
         {
            newContents.add("");
         }
         target.setContents(newContents);
         return true;
      }
      else if (contentsSize > legalSize)
      {
         String warning = "Should have " + legalSize + " contents; discarding extra strings: TextFlowTarget " + resId
               + " with contents: " + target.getContents();
         warnings.add(warning);
         List<String> newContents = new ArrayList<String>(legalSize);
         for (int i = 0; i < contentsSize; i++)
         {
            String content = target.getContents().get(i);
            newContents.add(content);
         }
         target.setContents(newContents);
         return true;
      }
      return false;
   }

   @Override
   // This will not run in a transaction. Instead, transactions are controlled within the method itself.
   @Transactional(TransactionPropagationType.NEVER)
   public List<String> translateAllInDoc(String projectSlug, String iterationSlug, String docId, LocaleId locale,
         TranslationsResource translations, Set<String> extensions, MergeType mergeType,
         boolean lock)
   {
      // Lock this document for push
      Lock transLock = null;
      if (lock)
      {
         transLock = new Lock(projectSlug, iterationSlug, docId, locale, "push");
         lockManagerServiceImpl.attain(transLock);
      }

      List<String> messages = Lists.newArrayList();
      try
      {
         messages = this.translateAllInDoc(projectSlug, iterationSlug, docId, locale, translations, extensions,
               mergeType);
      }
      finally
      {
         if (lock)
         {
            lockManagerServiceImpl.release(transLock);
         }
      }
      return messages;
   }

   /**
    * Run enforced validation check(Error) if target has changed and translation saving as 'Translated' or 'Approved'
    * @param newState
    * @param projectVersion
    * @param targetId
    * @param sources
    * @param translations
    * @return error messages
    */
   private String validateTranslations(ContentState newState, HProjectIteration projectVersion, String targetId,
         List<String> sources, List<String> translations)
   {
      String message = null;
      if (newState.isTranslated())
      {
         List<String> validationMessages = validationServiceImpl.validateWithServerRules(
               projectVersion, sources, translations, ValidationAction.State.Error);

         if (!validationMessages.isEmpty())
         {
            StringBuilder sb = new StringBuilder();
            sb.append("Translation ").append(targetId).append(" contains validation error - \n");
            for (String validationMessage : validationMessages)
            {
               sb.append(validationMessage).append("\n");
            }
            message = sb.toString();
         }
      }
      return message;
   }

   @Override
   public List<String> translateAllInDoc(final String projectSlug, final String iterationSlug, final String docId,
         final LocaleId locale,
         final TranslationsResource translations, final Set<String> extensions, final MergeType mergeType)
   {
      final HProjectIteration hProjectIteration = projectIterationDAO.getBySlug(projectSlug, iterationSlug);

      if (hProjectIteration == null)
      {
         throw new ZanataServiceException("Version '" + iterationSlug + "' for project '" + projectSlug + "' ");
      }

      if (mergeType == MergeType.IMPORT)
      {
         identity.checkPermission("import-translation", hProjectIteration);
      }

      ResourceUtils.validateExtensions(extensions);

      log.debug("pass evaluate");
      final HDocument document = documentDAO.getByDocIdAndIteration(hProjectIteration, docId);
      if (document.isObsolete())
      {
         throw new ZanataServiceException("A document was not found.", 404);
      }

      log.debug("start put translations entity:{}", translations);

      boolean changed = false;

      final HLocale hLocale = localeServiceImpl.validateLocaleByProjectIteration(locale, projectSlug, iterationSlug);
      final Optional<AsyncTaskHandle> handleOp = AsyncUtils.getEventAsyncHandle(AsyncTaskHandle.class);

      if (handleOp.isPresent())
      {
         handleOp.get().setMaxProgress(translations.getTextFlowTargets().size());
      }

      try
      {
         changed |=
               new Work<Boolean>()
               {
                  @Override
                  protected Boolean work() throws Exception
                  {
                     // handle extensions
                     boolean changed =
                           resourceUtils.transferFromTranslationsResourceExtensions(translations.getExtensions(true),
                                 document, extensions, hLocale, mergeType);
                     return changed;
                  }
               }.workInTransaction();
      }
      catch (Exception e)
      {
         throw new ZanataServiceException("Error during translation.", 500, e);
      }

      // NB: removedTargets only applies for MergeType.IMPORT
      final Collection<HTextFlowTarget> removedTargets = new HashSet<HTextFlowTarget>();
      final List<String> warnings = new ArrayList<String>();

      if (mergeType == MergeType.IMPORT)
      {
         for (HTextFlow textFlow : document.getTextFlows())
         {
            HTextFlowTarget hTarget = textFlow.getTargets().get(hLocale.getId());
            if (hTarget != null)
            {
               removedTargets.add(hTarget);
            }
         }
      }

      // Break the target into batches
      List<List<TextFlowTarget>> batches = Lists.partition(translations.getTextFlowTargets(), BATCH_SIZE);

      for (final List<TextFlowTarget> batch : batches)
      {
         try
         {
            changed |=
                  new Work<Boolean>()
                  {
                     @Override
                     protected Boolean work() throws Exception
                     {
                        boolean changed = false;

                        for (TextFlowTarget incomingTarget : batch)
                        {
                           String resId = incomingTarget.getResId();
                           HTextFlow textFlow = textFlowDAO.getById(document, resId);
                           if (textFlow == null)
                           {
                              // return warning for unknown resId to caller
                              String warning = "Could not find TextFlow for TextFlowTarget " + resId
                                    + " with contents: " + incomingTarget.getContents();
                              warnings.add(warning);
                              log.warn("skipping TextFlowTarget with unknown resId: {}", resId);
                           }
                           else
                           {
                              String validationMessage = validateTranslations(incomingTarget.getState(),
                                    hProjectIteration, incomingTarget.getResId(), textFlow.getContents(),
                                    incomingTarget.getContents());

                              if (!StringUtils.isEmpty(validationMessage))
                              {
                                 warnings.add(validationMessage);
                                 log.warn(validationMessage);
                                 continue;
                              }

                              int nPlurals = getNumPlurals(hLocale, textFlow);
                              HTextFlowTarget hTarget = textFlowTargetDAO.getTextFlowTarget(textFlow, hLocale);

                              if (mergeType == MergeType.IMPORT)
                              {
                                 removedTargets.remove(hTarget);
                              }

                              TranslationMergeServiceFactory.MergeContext mergeContext = new TranslationMergeServiceFactory.MergeContext(
                                    mergeType, textFlow, hLocale, hTarget, nPlurals);
                              TranslationMergeService mergeService = translationMergeServiceFactory
                                    .getMergeService(mergeContext);

                              boolean targetChanged = mergeService.merge(incomingTarget, hTarget, extensions);
                              if (hTarget == null)
                              {
                                 // in case hTarget was null, we need to retrieve it after merge
                                 hTarget = textFlow.getTargets().get(hLocale.getId());
                              }
                              targetChanged |= adjustContentsAndState(hTarget, nPlurals, warnings);

                              // update translation information if applicable
                              if (targetChanged)
                              {
                                 hTarget.setVersionNum(hTarget.getVersionNum() + 1);

                                 changed = true;
                                 if (incomingTarget.getTranslator() != null)
                                 {
                                    String email = incomingTarget.getTranslator().getEmail();
                                    HPerson hPerson = personDAO.findByEmail(email);
                                    if (hPerson == null)
                                    {
                                       hPerson = new HPerson();
                                       hPerson.setEmail(email);
                                       hPerson.setName(incomingTarget.getTranslator().getName());
                                       personDAO.makePersistent(hPerson);
                                    }
                                    hTarget.setTranslator(hPerson);
                                    hTarget.setLastModifiedBy(hPerson);
                                 }
                                 else
                                 {
                                    hTarget.setTranslator(null);
                                    hTarget.setLastModifiedBy(null);
                                 }
                                 textFlowTargetDAO.makePersistent(hTarget);
                              }
                              signalPostTranslateEvent(hTarget);
                           }

                           personDAO.flush();
                           textFlowTargetDAO.flush();
                           personDAO.clear();
                           textFlowTargetDAO.clear();
                           if (handleOp.isPresent())
                           {
                              handleOp.get().increaseProgress(1);
                           }
                        }

                        return changed;
                     }
                  }.workInTransaction();
         }
         catch (Exception e)
         {
            throw new ZanataServiceException("Error during translation.", 500, e);
         }

      }

      if (changed || !removedTargets.isEmpty())
      {
         try
         {
            new Work<Void>()
            {
               @Override
               protected Void work() throws Exception
               {
                  for (HTextFlowTarget target : removedTargets)
                  {
                     target = textFlowTargetDAO.findById(target.getId(), true); // need to refresh from persistence
                     target.clear();
                  }
                  textFlowTargetDAO.flush();

                  documentDAO.flush();
                  return null;
               }
            }.workInTransaction();

            if (Events.exists())
            {
               Events.instance().raiseEvent(
                     DocumentUploadedEvent.EVENT_NAME,
                     new DocumentUploadedEvent(document.getId(), false, hLocale.getLocaleId()
                     ));
            }
         }
         catch (Exception e)
         {
            throw new ZanataServiceException("Error during translation.", 500, e);
         }
      }

      return warnings;
   }

   private int getNumPlurals(HLocale hLocale, HTextFlow textFlow)
   {
      int nPlurals;
      if (!textFlow.isPlural())
      {
         nPlurals = 1;
      }
      else
      {
         nPlurals = resourceUtils.getNumPlurals(textFlow.getDocument(), hLocale);
      }
      return nPlurals;
   }

   public static class TranslationResultImpl implements TranslationResult
   {
      private HTextFlowTarget translatedTextFlowTarget;
      private boolean isSuccess;
      private boolean targetChanged = false;
      private int baseVersion;
      private ContentState baseContentState;
      private String errorMessage;

      @Override
      public boolean isTranslationSuccessful()
      {
         return isSuccess;
      }

      @Override
      public boolean isTargetChanged()
      {
         return targetChanged;
      }

      @Override
      public HTextFlowTarget getTranslatedTextFlowTarget()
      {
         return translatedTextFlowTarget;
      }

      @Override
      public int getBaseVersionNum()
      {
         return baseVersion;
      }

      @Override
      public ContentState getBaseContentState()
      {
         return baseContentState;
      }

      @Override
      public String getErrorMessage()
      {
         return errorMessage;
      }

   }

   @Override
   public List<TranslationResult> revertTranslations(LocaleId localeId, List<TransUnitUpdateInfo> translationsToRevert)
   {
      List<TranslationResult> results = new ArrayList<TranslationResult>();
      List<TransUnitUpdateRequest> updateRequests = new ArrayList<TransUnitUpdateRequest>();
      if (!translationsToRevert.isEmpty())
      {

         HTextFlow sampleHTextFlow = entityManager.find(HTextFlow.class, translationsToRevert.get(0).getTransUnit()
               .getId().getValue());
         HLocale hLocale = validateLocale(localeId, sampleHTextFlow.getDocument().getProjectIteration());
         for (TransUnitUpdateInfo info : translationsToRevert)
         {
            if (!info.isSuccess() || !info.isTargetChanged())
            {
               continue;
            }

            TransUnitId tuId = info.getTransUnit().getId();
            HTextFlow hTextFlow = entityManager.find(HTextFlow.class, tuId.getValue());
            HTextFlowTarget hTextFlowTarget = textFlowTargetDAO.getOrCreateTarget(hTextFlow, hLocale);

            // check that version has not advanced
            // TODO probably also want to check that source has not been updated
            Integer versionNum = hTextFlowTarget.getVersionNum();
            log.debug("about to revert hTextFlowTarget version {} to TransUnit version {}", versionNum, info
                  .getTransUnit().getVerNum());
            if (versionNum.equals(info.getTransUnit().getVerNum()))
            {
               // look up replaced version
               HTextFlowTargetHistory oldTarget = hTextFlowTarget.getHistory().get(info.getPreviousVersionNum());
               if (oldTarget != null)
               {
                  // generate request
                  List<String> oldContents = oldTarget.getContents();
                  ContentState oldState = oldTarget.getState();
                  TransUnitUpdateRequest request = new TransUnitUpdateRequest(tuId, oldContents, oldState, versionNum);
                  // add to list
                  updateRequests.add(request);
               }
               else
               {
                  log.info(
                        "got null previous target for tu with id {}, version {}. Assuming previous state is untranslated",
                        hTextFlow.getId(),
                        info.getPreviousVersionNum());
                  List<String> emptyContents = Lists.newArrayList();
                  for (int i = 0; i < hTextFlowTarget.getContents().size(); i++)
                  {
                     emptyContents.add("");
                  }
                  TransUnitUpdateRequest request = new TransUnitUpdateRequest(tuId, emptyContents, ContentState.New,
                        versionNum);
                  updateRequests.add(request);
               }
            }
            else
            {
               log.info(
                     "attempt to revert target version {} for tu with id {}, but current version is {}. Not reverting.",
                     new Object[] {
                           info.getTransUnit().getVerNum(), tuId, versionNum });
               results.add(buildFailResult(hTextFlowTarget));
            }
         }
      }
      results.addAll(translateWithoutValidating(localeId, updateRequests));
      return results;
   }

   /**
    * @param hTextFlowTarget
    * @return
    */
   private TranslationResultImpl buildFailResult(HTextFlowTarget hTextFlowTarget)
   {
      TranslationResultImpl result = new TranslationResultImpl();
      result.baseVersion = hTextFlowTarget.getVersionNum();
      result.baseContentState = hTextFlowTarget.getState();
      result.isSuccess = false;
      result.translatedTextFlowTarget = hTextFlowTarget;
      result.errorMessage = null;
      return result;
   }
}
