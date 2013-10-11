/*
 * Copyright 2010, Red Hat, Inc. and individual contributors as indicated by the
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
package org.zanata.webtrans.client.presenter;

import net.customware.gwt.presenter.client.EventBus;
import net.customware.gwt.presenter.client.PresenterRevealedEvent;
import net.customware.gwt.presenter.client.PresenterRevealedHandler;
import net.customware.gwt.presenter.client.widget.WidgetPresenter;

import org.zanata.common.LocaleId;
import org.zanata.rest.dto.stats.ContainerTranslationStatistics;
import org.zanata.rest.dto.stats.TranslationStatistics;
import org.zanata.rest.dto.stats.TranslationStatistics.StatUnit;
import org.zanata.webtrans.client.events.AttentionModeActivationEvent;
import org.zanata.webtrans.client.events.AttentionModeActivationEventHandler;
import org.zanata.webtrans.client.events.DocumentStatsUpdatedEvent;
import org.zanata.webtrans.client.events.DocumentStatsUpdatedEventHandler;
import org.zanata.webtrans.client.events.KeyShortcutEvent;
import org.zanata.webtrans.client.events.KeyShortcutEventHandler;
import org.zanata.webtrans.client.events.NotificationEvent;
import org.zanata.webtrans.client.events.NotificationEvent.Severity;
import org.zanata.webtrans.client.events.NotificationEventHandler;
import org.zanata.webtrans.client.events.ProjectStatsUpdatedEvent;
import org.zanata.webtrans.client.events.ProjectStatsUpdatedEventHandler;
import org.zanata.webtrans.client.events.RefreshPageEvent;
import org.zanata.webtrans.client.events.ShowSideMenuEvent;
import org.zanata.webtrans.client.events.ShowSideMenuEventHandler;
import org.zanata.webtrans.client.events.WorkspaceContextUpdateEvent;
import org.zanata.webtrans.client.events.WorkspaceContextUpdateEventHandler;
import org.zanata.webtrans.client.history.History;
import org.zanata.webtrans.client.history.HistoryToken;
import org.zanata.webtrans.client.history.Window;
import org.zanata.webtrans.client.keys.KeyShortcut;
import org.zanata.webtrans.client.keys.Keys;
import org.zanata.webtrans.client.keys.ShortcutContext;
import org.zanata.webtrans.client.resources.WebTransMessages;
import org.zanata.webtrans.client.view.AppDisplay;
import org.zanata.webtrans.shared.model.DocumentId;
import org.zanata.webtrans.shared.model.DocumentInfo;
import org.zanata.webtrans.shared.model.UserWorkspaceContext;

import com.allen_sauer.gwt.log.client.Log;
import com.google.common.base.Strings;
import com.google.gwt.core.client.GWT;
import com.google.inject.Inject;

public class AppPresenter extends WidgetPresenter<AppDisplay> implements
        ShowSideMenuEventHandler, WorkspaceContextUpdateEventHandler,
        DocumentStatsUpdatedEventHandler, PresenterRevealedHandler,
        AttentionModeActivationEventHandler, ProjectStatsUpdatedEventHandler,
        NotificationEventHandler, AppDisplay.Listener {

    private static final String WORKSPACE_TITLE_QUERY_PARAMETER_KEY = "title";

    private final KeyShortcutPresenter keyShortcutPresenter;
    private final AttentionKeyShortcutPresenter attentionKeyShortcutPresenter;
    private final DocumentListPresenter documentListPresenter;
    private final TranslationPresenter translationPresenter;
    private final SearchResultsPresenter searchResultsPresenter;
    private final SideMenuPresenter sideMenuPresenter;
    private final History history;
    private final Window window;
    private final Window.Location windowLocation;
    private final UserWorkspaceContext userWorkspaceContext;
    private final WebTransMessages messages;
    private final LocaleId localeId;

    // states
    private DocumentInfo selectedDocument;
    private ContainerTranslationStatistics selectedDocumentStats =
            new ContainerTranslationStatistics();
    private ContainerTranslationStatistics projectStats =
            new ContainerTranslationStatistics();
    private ContainerTranslationStatistics currentDisplayStats =
            new ContainerTranslationStatistics();
    private MainView currentView = null;

    @Inject
    public AppPresenter(AppDisplay display, EventBus eventBus,
            final SideMenuPresenter sideMenuPresenter,
            final AttentionKeyShortcutPresenter attentionKeyShortcutPresenter,
            final KeyShortcutPresenter keyShortcutPresenter,
            final TranslationPresenter translationPresenter,
            final DocumentListPresenter documentListPresenter,
            final SearchResultsPresenter searchResultsPresenter,
            final UserWorkspaceContext userWorkspaceContext,
            final WebTransMessages messages, final History history,
            final Window window, final Window.Location windowLocation) {
        super(display, eventBus);
        this.userWorkspaceContext = userWorkspaceContext;
        this.attentionKeyShortcutPresenter = attentionKeyShortcutPresenter;
        this.keyShortcutPresenter = keyShortcutPresenter;
        this.history = history;
        this.messages = messages;
        this.documentListPresenter = documentListPresenter;
        this.translationPresenter = translationPresenter;
        this.searchResultsPresenter = searchResultsPresenter;
        this.sideMenuPresenter = sideMenuPresenter;
        this.window = window;
        this.windowLocation = windowLocation;

        localeId =
                userWorkspaceContext.getWorkspaceContext().getWorkspaceId()
                        .getLocaleId();

        display.setListener(this);
    }

    @Override
    protected void onBind() {
        keyShortcutPresenter.bind();
        attentionKeyShortcutPresenter.bind();
        documentListPresenter.bind();
        translationPresenter.bind();
        searchResultsPresenter.bind();
        sideMenuPresenter.bind();

        registerHandler(eventBus.addHandler(ShowSideMenuEvent.getType(), this));
        registerHandler(eventBus.addHandler(
                WorkspaceContextUpdateEvent.getType(), this));
        registerHandler(eventBus.addHandler(
                DocumentStatsUpdatedEvent.getType(), this));
        registerHandler(eventBus.addHandler(PresenterRevealedEvent.getType(),
                this));
        registerHandler(eventBus.addHandler(
                AttentionModeActivationEvent.getType(), this));
        registerHandler(eventBus.addHandler(ProjectStatsUpdatedEvent.getType(),
                this));
        registerHandler(eventBus.addHandler(NotificationEvent.getType(), this));

        if (selectedDocument == null) {
            display.enableTab(MainView.Editor, false);
        }

        registerKeyShortcuts();

        display.setProjectLinkLabel(userWorkspaceContext.getWorkspaceContext()
                .getWorkspaceId().getProjectIterationId().getProjectSlug());
        display.setVersionLinkLabel(userWorkspaceContext.getWorkspaceContext()
                .getWorkspaceId().getProjectIterationId().getIterationSlug());
        display.setFilesLinkLabel("Documents ("
                + userWorkspaceContext.getWorkspaceContext().getWorkspaceId()
                        .getLocaleId().getId() + ")");

        String workspaceTitle =
                windowLocation
                        .getParameter(WORKSPACE_TITLE_QUERY_PARAMETER_KEY);
        if (!Strings.isNullOrEmpty(workspaceTitle)) {
            window.setTitle(messages.windowTitle2(userWorkspaceContext
                    .getWorkspaceContext().getWorkspaceName(),
                    userWorkspaceContext.getWorkspaceContext().getLocaleName(),
                    workspaceTitle));
        } else {
            window.setTitle(messages.windowTitle(userWorkspaceContext
                    .getWorkspaceContext().getWorkspaceName(),
                    userWorkspaceContext.getWorkspaceContext().getLocaleName()));
        }

        display.setReadOnlyVisible(userWorkspaceContext.hasReadOnlyAccess());
    }

    private void registerKeyShortcuts() {
        keyShortcutPresenter.register(KeyShortcut.Builder.builder()
                .addKey(new Keys(Keys.ALT_KEY, 'L'))
                .setContext(ShortcutContext.Application)
                .setDescription(messages.showDocumentListKeyShortcut())
                .setHandler(new KeyShortcutEventHandler() {
                    @Override
                    public void onKeyShortcut(KeyShortcutEvent event) {
                        HistoryToken token = history.getHistoryToken();
                        token.setView(MainView.Documents);
                        history.newItem(token.toTokenString());
                    }
                }).build());

        keyShortcutPresenter.register(KeyShortcut.Builder.builder()
                .addKey(new Keys(Keys.ALT_KEY, 'O'))
                .setContext(ShortcutContext.Application)
                .setDescription(messages.showEditorKeyShortcut())
                .setHandler(new KeyShortcutEventHandler() {
                    @Override
                    public void onKeyShortcut(KeyShortcutEvent event) {
                        if (selectedDocument == null) {
                            eventBus.fireEvent(new NotificationEvent(
                                    Severity.Warning, messages
                                            .noDocumentSelected()));
                        } else {
                            HistoryToken token = history.getHistoryToken();
                            token.setView(MainView.Editor);
                            history.newItem(token.toTokenString());
                        }
                    }
                }).build());

        keyShortcutPresenter.register(KeyShortcut.Builder.builder()
                .addKey(new Keys(Keys.ALT_KEY, 'P'))
                .setContext(ShortcutContext.Application)
                .setDescription(messages.showProjectWideSearch())
                .setHandler(new KeyShortcutEventHandler() {
                    @Override
                    public void onKeyShortcut(KeyShortcutEvent event) {
                        HistoryToken token = history.getHistoryToken();
                        token.setView(MainView.Search);
                        history.newItem(token.toTokenString());
                    }
                }).build());
    }

    @Override
    protected void onUnbind() {
    }

    @Override
    public void onRevealDisplay() {
    }

    public void showView(MainView viewToShow) {
        if (currentView == viewToShow) {
            return;
        }

        if (currentView == MainView.Editor) {
            translationPresenter.saveEditorPendingChange();
        }
        Log.info("view to show is:" + viewToShow);
        switch (viewToShow) {
        // TODO use revealDisplay/concealDisplay for editor and document views
        case Editor:
            if (selectedDocument != null) {
                display.setDocumentLabel(selectedDocument.getPath(),
                        selectedDocument.getName());
            } else {
                display.setDocumentLabel("", messages.noDocumentSelected());
            }
            currentDisplayStats = selectedDocumentStats;
            translationPresenter.revealDisplay();
            searchResultsPresenter.concealDisplay();
            sideMenuPresenter.showValidationOptions(true);
            sideMenuPresenter.setOptionMenu(MainView.Editor);
            break;
        case Search:
            // these two lines temporarily here until PresenterRevealedHandler
            // is fully functional
            display.setDocumentLabel("", messages.projectWideSearchAndReplace());
            currentDisplayStats = projectStats;
            translationPresenter.concealDisplay();
            searchResultsPresenter.revealDisplay();
            sideMenuPresenter.showValidationOptions(false);
            sideMenuPresenter.setOptionMenu(MainView.Search);
            break;
        case Documents:
        default:
            display.setDocumentLabel("", messages.documentListTitle());
            currentDisplayStats = projectStats;
            translationPresenter.concealDisplay();
            searchResultsPresenter.concealDisplay();
            sideMenuPresenter.showValidationOptions(true);
            sideMenuPresenter.setOptionMenu(MainView.Documents);
            break;
        }
        display.showInMainView(viewToShow);
        refreshStatsDisplay();
        currentView = viewToShow;
        if (currentView == MainView.Editor) {
            eventBus.fireEvent(RefreshPageEvent.REFRESH_CODEMIRROR_EVENT);
        }
    }

    /**
     * Set selected document to the given document, update name and stats to
     * match the newly selected document.
     *
     * @param docId
     *            id of the document to select
     */
    public void selectDocument(DocumentId docId) {
        if (selectedDocument == null || !docId.equals(selectedDocument.getId())) {
            DocumentInfo docInfo = documentListPresenter.getDocumentInfo(docId);
            if (docInfo != null) {
                display.enableTab(MainView.Editor, true);
                selectedDocument = docInfo;
                if (selectedDocument.getStats() != null) {
                    selectedDocumentStats.copyFrom(selectedDocument.getStats());
                }
                if (currentView == MainView.Editor) {
                    display.setDocumentLabel(selectedDocument.getPath(),
                            selectedDocument.getName());
                    refreshStatsDisplay();
                }
            }
        }
    }

    /**
     * Ensure current stats are displayed in the display.
     */
    private void refreshStatsDisplay() {
        display.setStats(currentDisplayStats, true);
    }

    public DocumentId getSelectedDocIdOrNull() {
        return selectedDocument == null ? null : selectedDocument.getId();
    }

    public DocumentInfo getSelectedDocumentInfoOrNull() {
        return selectedDocument == null ? null : selectedDocument;
    }

    @Override
    public void onShowSideMenu(ShowSideMenuEvent event) {
        display.showSideMenu(event.isShowing());
    }

    @Override
    public void onWorkspaceContextUpdated(WorkspaceContextUpdateEvent event) {
        userWorkspaceContext.setProjectActive(event.isProjectActive());
        userWorkspaceContext.getWorkspaceContext().getWorkspaceId()
                .getProjectIterationId().setProjectType(event.getProjectType());
        if (userWorkspaceContext.hasReadOnlyAccess()) {
            eventBus.fireEvent(new NotificationEvent(
                    NotificationEvent.Severity.Info, messages
                            .notifyReadOnlyWorkspace()));
        } else {
            eventBus.fireEvent(new NotificationEvent(
                    NotificationEvent.Severity.Info, messages
                            .notifyEditableWorkspace()));
        }
        display.setReadOnlyVisible(userWorkspaceContext.hasReadOnlyAccess());
    }

    @Override
    public void onDocumentStatsUpdated(DocumentStatsUpdatedEvent event) {
        if (selectedDocument != null
                && event.getDocId().equals(selectedDocument.getId())) {
            selectedDocumentStats.copyFrom(event.getNewStats());
            if (currentView.equals(MainView.Editor)) {
                refreshStatsDisplay();
            }
        }
    }

    @Override
    public void onSearchAndReplaceClicked() {
        HistoryToken token = HistoryToken.fromTokenString(history.getToken());
        if (!token.getView().equals(MainView.Search)) {
            token.setView(MainView.Search);
            history.newItem(token.toTokenString());
        }
    }

    @Override
    public void onEditorClicked() {
        if (selectedDocument != null) {
            HistoryToken token =
                    HistoryToken.fromTokenString(history.getToken());
            if (!token.getView().equals(MainView.Editor)) {
                token.setView(MainView.Editor);
                history.newItem(token.toTokenString());
            }
        }
    }

    @Override
    public void onDocumentListClicked() {
        HistoryToken token = HistoryToken.fromTokenString(history.getToken());
        if (!token.getView().equals(MainView.Documents)) {
            token.setView(MainView.Documents);
            history.newItem(token.toTokenString());
        }
    }

    @Override
    public void onKeyShortcutsClicked() {
        keyShortcutPresenter.showShortcuts();
    }

    @Override
    public void onPresenterRevealed(PresenterRevealedEvent event) {
        // TODO disabled until tests are updated
        // if (event.getPresenter() == searchResultsPresenter)
        // {
        // display.setDocumentLabel("",
        // messages.projectWideSearchAndReplace());
        // currentDisplayStats = projectStats;
        // }
    }

    /**
     * Facilitate unit testing. Will be no-op if in client(GWT compiled) mode.
     *
     * @param projectStats
     *            project stats
     * @param selectedDocumentStats
     *            selected document stats
     * @param currentView
     *            current view
     * @param selectedDocument
     */
    protected void setStatesForTest(
            ContainerTranslationStatistics projectStats,
            ContainerTranslationStatistics selectedDocumentStats,
            MainView currentView, DocumentInfo selectedDocument) {
        if (!GWT.isClient()) {
            this.selectedDocument = selectedDocument;
            this.projectStats = projectStats;
            this.currentView = currentView;
            this.selectedDocumentStats = selectedDocumentStats;
        }
    }

    @Override
    public void onAttentionModeActivationChanged(
            AttentionModeActivationEvent event) {
        display.setKeyboardShorcutColor(event.isActive());
    }

    @Override
    public void onProjectStatsUpdated(ProjectStatsUpdatedEvent event) {
        TranslationStatistics msgStats =
                event.getNewStats()
                        .getStats(localeId.getId(), StatUnit.MESSAGE);
        TranslationStatistics currentMsgStats =
                projectStats.getStats(localeId.getId(), StatUnit.MESSAGE);

        if (currentMsgStats == null) {
            if (msgStats != null) {
                projectStats.addStats(msgStats);
            }
        } else {
            currentMsgStats.add(msgStats);
        }

        TranslationStatistics wordStats =
                event.getNewStats().getStats(localeId.getId(), StatUnit.WORD);
        TranslationStatistics currentWordStats =
                projectStats.getStats(localeId.getId(), StatUnit.WORD);

        if (currentWordStats == null) {
            if (wordStats != null) {
                projectStats.addStats(wordStats);
            }
        } else {
            currentWordStats.add(wordStats);
        }

        refreshStatsDisplay();
    }

    @Override
    public void onNotification(NotificationEvent event) {
        if (event.getSeverity() == Severity.Warning
                || event.getSeverity() == Severity.Error) {
            display.showNotification(event);
        }
    }
}
