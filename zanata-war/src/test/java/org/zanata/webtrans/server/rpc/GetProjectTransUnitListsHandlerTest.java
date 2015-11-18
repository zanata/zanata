package org.zanata.webtrans.server.rpc;

import java.util.List;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.zanata.ZanataTest;
import org.zanata.common.ContentState;
import org.zanata.common.LocaleId;
import org.zanata.exception.ZanataServiceException;
import org.zanata.model.HDocument;
import org.zanata.model.HLocale;
import org.zanata.model.HTextFlow;
import org.zanata.model.TestFixture;
import org.zanata.rest.service.ResourceUtils;
import org.zanata.seam.SeamAutowire;
import org.zanata.search.FilterConstraints;
import org.zanata.security.ZanataIdentity;
import org.zanata.service.LocaleService;
import org.zanata.service.TextFlowSearchService;
import org.zanata.webtrans.shared.model.WorkspaceId;
import org.zanata.webtrans.shared.rpc.GetProjectTransUnitLists;
import org.zanata.webtrans.shared.rpc.GetProjectTransUnitListsResult;

import com.google.common.collect.Lists;

import lombok.extern.slf4j.Slf4j;
import net.customware.gwt.dispatch.shared.ActionException;
import static org.hamcrest.MatcherAssert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.zanata.model.TestFixture.makeHTextFlow;

/**
 * @author Patrick Huang <a
 *         href="mailto:pahuang@redhat.com">pahuang@redhat.com</a>
 */
@Slf4j
public class GetProjectTransUnitListsHandlerTest extends ZanataTest {
    public static final long DOC_ID = 1L;
    private GetProjectTransUnitListsHandler handler;
    @Mock
    private ZanataIdentity identity;
    @Mock
    private LocaleService localeService;
    @Mock
    private ResourceUtils resourceUtils;
    @Mock
    private TextFlowSearchService textFlowSearchServiceImpl;
    private List<HTextFlow> textFlows;
    private HLocale hLocale;
    @Captor
    private ArgumentCaptor<FilterConstraints> constraintCaptor;
    private LocaleId localeId = LocaleId.DE;
    private WorkspaceId workspaceId;

    @Before
    public void setUpData() {
        hLocale = TestFixture.setId(3L, new HLocale(LocaleId.DE));
        // @formatter:off
        textFlows = Lists.newArrayList(
            textFlow(1L, "File is removed", ""),
            textFlow(2L, "file", "open file"),
            textFlow(3L, " file ", null),
            textFlow(4L, " File", "FILE   ")
        );
        // @formatter:on
        workspaceId = TestFixture.workspaceId(localeId);
    }

    @Before
    @SuppressWarnings("unchecked")
    public void beforeMethod() {
        SeamAutowire seam = SeamAutowire.instance().reset();
        MockitoAnnotations.initMocks(this);
        // must create before transUnitTransformer
        seam.use("resourceUtils", resourceUtils);
        TransUnitTransformer transUnitTransformer =
                seam.autowire(TransUnitTransformer.class);
        // @formatter:off
        handler = seam
            .use("identity", identity)
            .use("localeServiceImpl", localeService)
            .use("transUnitTransformer", transUnitTransformer)
            .use("textFlowSearchServiceImpl", textFlowSearchServiceImpl)
            .ignoreNonResolvable()
            .autowire(GetProjectTransUnitListsHandler.class);
        // @formatter:on
        when(
                localeService.validateLocaleByProjectIteration(localeId,
                        workspaceId.getProjectIterationId().getProjectSlug(),
                        workspaceId.getProjectIterationId().getIterationSlug()))
                .thenReturn(hLocale);
        when(resourceUtils.getNumPlurals(any(HDocument.class), any(HLocale.class))).thenReturn(1);
    }

    private HTextFlow textFlow(long id, String sourceContent,
            String targetContent) {
        HTextFlow hTextFlow =
                makeHTextFlow(id, hLocale, ContentState.NeedReview);
        TestFixture.setId(DOC_ID, hTextFlow.getDocument());
        hTextFlow.setContent0(sourceContent);
        if (targetContent != null) {
            hTextFlow.getTargets().get(hLocale.getId())
                    .setContent0(targetContent);
        }
        log.debug("text flow - id: {}, source : [{}], target: [{}]",
                id, sourceContent, targetContent);
        return hTextFlow;
    }

    @Test(expected = ActionException.class)
    public void exceptionIfLocaleIsInvalid() throws Exception {
        GetProjectTransUnitLists action =
                new GetProjectTransUnitLists("a", true, true, false);
        action.setWorkspaceId(workspaceId);
        when(
                localeService.validateLocaleByProjectIteration(localeId,
                        workspaceId.getProjectIterationId().getProjectSlug(),
                        workspaceId.getProjectIterationId().getIterationSlug()))
                .thenThrow(new ZanataServiceException("bad"));

        GetProjectTransUnitListsResult result = handler.execute(action, null);
    }

    @Test
    public void emptySearchTermWillReturnEmpty() throws Exception {
        GetProjectTransUnitLists action =
                new GetProjectTransUnitLists("", true, true, false);
        action.setWorkspaceId(workspaceId);

        GetProjectTransUnitListsResult result = handler.execute(action, null);

        verify(identity).checkLoggedIn();
        assertThat(result.getDocumentIds(), Matchers.<Long> emptyIterable());
        verifyZeroInteractions(textFlowSearchServiceImpl);
    }

    @Test
    public void searchWithNoLeadingAndTrailingWhiteSpace() throws Exception {
        GetProjectTransUnitLists action =
                new GetProjectTransUnitLists("file", true, true, true);
        action.setWorkspaceId(workspaceId);
        when(
                textFlowSearchServiceImpl.findTextFlows(
                        eq(action.getWorkspaceId()),
                        eq(action.getDocumentPaths()),
                        constraintCaptor.capture())).thenReturn(textFlows);

        // When: search in target only and case sensitive
        GetProjectTransUnitListsResult result = handler.execute(action, null);

        verify(identity).checkLoggedIn();
        FilterConstraints constraints = constraintCaptor.getValue();
        assertThat(constraints.isSearchInSource(), Matchers.equalTo(true));
        assertThat(constraints.isSearchInTarget(), Matchers.equalTo(true));
        assertThat(constraints.isCaseSensitive(), Matchers.equalTo(true));
        assertThat(result.getDocumentIds(), Matchers.contains(DOC_ID));
        assertThat(TestFixture.asIds(result.getUnits(DOC_ID)),
                Matchers.contains(1, 2, 3, 4));
    }

    @Test
    public void searchWithLeadingWhiteSpace() throws Exception {
        GetProjectTransUnitLists action =
                new GetProjectTransUnitLists(" file", true, true, true);
        action.setWorkspaceId(workspaceId);
        when(
                textFlowSearchServiceImpl.findTextFlows(
                        eq(action.getWorkspaceId()),
                        eq(action.getDocumentPaths()),
                        constraintCaptor.capture())).thenReturn(textFlows);

        // When: search in source and target and case sensitive
        GetProjectTransUnitListsResult result = handler.execute(action, null);

        verify(identity).checkLoggedIn();
        FilterConstraints constraints = constraintCaptor.getValue();
        assertThat(constraints.isSearchInSource(), Matchers.equalTo(true));
        assertThat(constraints.isSearchInTarget(), Matchers.equalTo(true));
        assertThat(constraints.isCaseSensitive(), Matchers.equalTo(true));
        assertThat(result.getDocumentIds(), Matchers.contains(DOC_ID));
        assertThat(TestFixture.asIds(result.getUnits(DOC_ID)),
                Matchers.contains(2, 3));
    }

    @Test
    public void searchWithTrailingWhiteSpace() throws Exception {
        GetProjectTransUnitLists action =
                new GetProjectTransUnitLists("file ", true, false, false);
        action.setWorkspaceId(workspaceId);
        when(
                textFlowSearchServiceImpl.findTextFlows(
                        eq(action.getWorkspaceId()),
                        eq(action.getDocumentPaths()),
                        constraintCaptor.capture())).thenReturn(textFlows);

        // When: search in source only and case insensitive
        GetProjectTransUnitListsResult result = handler.execute(action, null);

        verify(identity).checkLoggedIn();
        FilterConstraints constraints = constraintCaptor.getValue();
        assertThat(constraints.isSearchInSource(), Matchers.equalTo(true));
        assertThat(constraints.isSearchInTarget(), Matchers.equalTo(false));
        assertThat(constraints.isCaseSensitive(), Matchers.equalTo(false));
        assertThat(result.getDocumentIds(), Matchers.contains(DOC_ID));
        assertThat(TestFixture.asIds(result.getUnits(DOC_ID)),
                Matchers.contains(1, 3));
    }

    @Test
    public void testRollback() throws Exception {
        handler.rollback(null, null, null);
    }
}
