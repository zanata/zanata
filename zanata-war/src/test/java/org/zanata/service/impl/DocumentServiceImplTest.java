/*
 * Copyright 2014, Red Hat, Inc. and individual contributors as indicated by the
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

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.zanata.ApplicationConfiguration;
import org.zanata.common.ContentState;
import org.zanata.common.LocaleId;
import org.zanata.dao.DocumentDAO;
import org.zanata.dao.ProjectIterationDAO;
import org.zanata.events.DocumentMilestoneEvent;
import org.zanata.events.DocumentStatisticUpdatedEvent;
import org.zanata.i18n.Messages;
import org.zanata.model.HDocument;
import org.zanata.model.HProject;
import org.zanata.model.HProjectIteration;
import org.zanata.model.WebHook;
import org.zanata.service.DocumentService;
import org.zanata.service.TranslationStateCache;
import org.zanata.ui.model.statistic.WordStatistic;
import org.zanata.util.StatisticsUtil;

import com.google.common.collect.Lists;
import org.zanata.util.UrlUtil;

/**
 * @author Alex Eng <a href="mailto:aeng@redhat.com">aeng@redhat.com</a>
 */
public class DocumentServiceImplTest {

    @Mock
    private ProjectIterationDAO projectIterationDAO;

    @Mock
    private DocumentDAO documentDAO;

    @Mock
    private TranslationStateCache translationStateCacheImpl;

    @Mock
    private Messages msgs;

    @Mock
    private UrlUtil urlUtil;

    @Mock
    private ApplicationConfiguration applicationConfiguration;

    private DocumentServiceImpl documentService;
    private DocumentServiceImpl spyService;

    private Long docId = 1L, versionId = 1L, tfId = 1L;
    private LocaleId localeId = LocaleId.DE;
    private String docIdString = "documentId";
    private String projectSlug = "project-slug";
    private String versionSlug = "version-slug";

    private int milestone = DocumentService.DOC_EVENT_MILESTONE;

    List<WebHook> webHooks = Lists.newArrayList();

    private String testUrl = "http://localhost/test/doc/url";

    private String key = null;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        documentService = new DocumentServiceImpl();
        documentService.init(projectIterationDAO, documentDAO,
            translationStateCacheImpl, urlUtil, applicationConfiguration, msgs);
        // TODO spy should only be needed for legacy code
        spyService = Mockito.spy(documentService);
        // avoid triggering HTTP requests in a unit test:
        doNothing().when(spyService).publishDocumentMilestoneEvent(
                any(WebHook.class), any(DocumentMilestoneEvent.class));

        HProjectIteration version = Mockito.mock(HProjectIteration.class);
        HProject project = Mockito.mock(HProject.class);
        HDocument document = Mockito.mock(HDocument.class);

        webHooks = Lists.newArrayList();
        webHooks.add(new WebHook(project, "http://test.example.com", key));
        webHooks.add(new WebHook(project, "http://test1.example.com", key));

        when(projectIterationDAO.findById(versionId)).thenReturn(version);
        when(version.getProject()).thenReturn(project);
        when(version.getSlug()).thenReturn(versionSlug);
        when(project.getSlug()).thenReturn(projectSlug);
        when(project.getWebHooks()).thenReturn(webHooks);
        when(documentDAO.getById(docId)).thenReturn(document);
        when(document.getDocId()).thenReturn(docIdString);

        when(urlUtil.fullEditorDocumentUrl(anyString(), anyString(),
                any(LocaleId.class), any(LocaleId.class), anyString()))
                .thenReturn(testUrl);
    }

    @Test
    public void documentMilestoneEventTranslatedTest() {
        doNothing().when(spyService).publishDocumentMilestoneEvent(
                any(WebHook.class), any(DocumentMilestoneEvent.class));
        WordStatistic stats = new WordStatistic(0, 0, 0, 10, 0);
        when(translationStateCacheImpl.getDocumentStatistics(docId, localeId))
            .thenReturn(stats);
        runDocumentStatisticUpdatedTest(spyService, ContentState.New,
                ContentState.Translated, stats);

        DocumentMilestoneEvent milestoneEvent =
                new DocumentMilestoneEvent(projectSlug, versionSlug,
                        docIdString, localeId,
                        msgs.format("jsf.webhook.response.state", milestone,
                                ContentState.Translated), testUrl);

        verify(spyService).publishDocumentMilestoneEvent(webHooks.get(0),
                milestoneEvent);
        verify(spyService).publishDocumentMilestoneEvent(webHooks.get(1),
                milestoneEvent);
    }

    @Test
    public void documentMilestoneEventTranslatedNot100Test() {
        WordStatistic stats = new WordStatistic(0, 1, 0, 9, 0);
        when(translationStateCacheImpl.getDocumentStatistics(docId, localeId))
            .thenReturn(stats);
        runDocumentStatisticUpdatedTest(spyService, ContentState.New,
                ContentState.Translated, stats);

        DocumentMilestoneEvent milestoneEvent =
                new DocumentMilestoneEvent(projectSlug, versionSlug,
                        docIdString, localeId, testUrl,
                        msgs.format("jsf.webhook.response.state", milestone,
                                ContentState.Translated));

        verify(spyService, never()).publishDocumentMilestoneEvent(
                webHooks.get(0), milestoneEvent);
        verify(spyService, never()).publishDocumentMilestoneEvent(
                webHooks.get(1), milestoneEvent);
    }

    @Test
    public void documentMilestoneEventApprovedTest() {
        doNothing().when(spyService).publishDocumentMilestoneEvent(
            any(WebHook.class), any(DocumentMilestoneEvent.class));
        WordStatistic stats = new WordStatistic(10, 0, 0, 0, 0);
        when(translationStateCacheImpl.getDocumentStatistics(docId, localeId))
            .thenReturn(stats);
        runDocumentStatisticUpdatedTest(spyService, ContentState.Translated,
                ContentState.Approved, stats);

        DocumentMilestoneEvent milestoneEvent =
                new DocumentMilestoneEvent(projectSlug,
                        versionSlug, docIdString,
                        localeId, msgs.format("jsf.webhook.response.state",
                                milestone, ContentState.Approved), testUrl);
        verify(spyService).publishDocumentMilestoneEvent(webHooks.get(0),
                milestoneEvent);
        verify(spyService).publishDocumentMilestoneEvent(webHooks.get(1),
                milestoneEvent);
    }

    @Test
    public void documentMilestoneEventApprovedNot100Test() {
        WordStatistic stats = new WordStatistic(9, 0, 0, 1, 0);
        when(translationStateCacheImpl.getDocumentStatistics(docId, localeId))
            .thenReturn(stats);
        runDocumentStatisticUpdatedTest(spyService, ContentState.Translated,
                ContentState.Approved, stats);

        DocumentMilestoneEvent milestoneEvent =
                new DocumentMilestoneEvent(projectSlug, versionSlug,
                        docIdString, localeId, msgs.format(
                                "jsf.webhook.response.state", milestone,
                                ContentState.Approved), testUrl);

        verify(spyService, never()).publishDocumentMilestoneEvent(
                webHooks.get(0), milestoneEvent);
        verify(spyService, never()).publishDocumentMilestoneEvent(
                webHooks.get(1), milestoneEvent);
    }

    @Test
    public void documentMilestoneEventSameStateTest1() {
        WordStatistic stats = new WordStatistic(10, 0, 0, 0, 0);
        when(translationStateCacheImpl.getDocumentStatistics(docId, localeId))
            .thenReturn(stats);

        runDocumentStatisticUpdatedTest(spyService, ContentState.Approved,
                ContentState.Approved, stats);

        DocumentMilestoneEvent milestoneEvent =
                new DocumentMilestoneEvent(projectSlug, versionSlug,
                        docIdString, localeId, msgs.format(
                                "jsf.webhook.response.state", milestone,
                                ContentState.Approved), testUrl);
        verify(spyService, never()).publishDocumentMilestoneEvent(
                webHooks.get(0), milestoneEvent);
        verify(spyService, never()).publishDocumentMilestoneEvent(
                webHooks.get(1), milestoneEvent);
    }

    @Test
    public void documentMilestoneEventSameStateTest2() {
        WordStatistic stats = new WordStatistic(0, 0, 0, 10, 0);

        when(translationStateCacheImpl.getDocumentStatistics(docId, localeId))
            .thenReturn(stats);

        runDocumentStatisticUpdatedTest(spyService, ContentState.Translated,
            ContentState.Translated, stats);

        DocumentMilestoneEvent milestoneEvent =
                new DocumentMilestoneEvent(projectSlug, versionSlug,
                        docIdString, localeId, msgs.format(
                                "jsf.webhook.response.state", milestone,
                                ContentState.Translated), testUrl);
        verify(spyService, never()).publishDocumentMilestoneEvent(
                webHooks.get(0), milestoneEvent);
        verify(spyService, never()).publishDocumentMilestoneEvent(
                webHooks.get(1), milestoneEvent);
    }

    private void runDocumentStatisticUpdatedTest(
            DocumentServiceImpl spyService,
            ContentState oldState, ContentState newState, WordStatistic stats) {

        int wordCount = 10;

        DocumentStatisticUpdatedEvent event =
                new DocumentStatisticUpdatedEvent(versionId,
                        docId, localeId, wordCount, oldState, newState);

        spyService.documentStatisticUpdated(event);
    }
}
