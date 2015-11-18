package org.zanata.webtrans.server.rpc;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.List;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.zanata.ZanataTest;
import org.zanata.common.ContentState;
import org.zanata.common.LocaleId;
import org.zanata.common.ProjectType;
import org.zanata.model.HDocument;
import org.zanata.model.HLocale;
import org.zanata.model.HTextFlow;
import org.zanata.model.HTextFlowTarget;
import org.zanata.model.TestFixture;
import org.zanata.rest.service.ResourceUtils;
import org.zanata.seam.SeamAutowire;
import org.zanata.service.SecurityService;
import org.zanata.service.TranslationService;
import org.zanata.webtrans.server.TranslationWorkspace;
import org.zanata.webtrans.shared.model.DocumentId;
import org.zanata.webtrans.shared.model.ProjectIterationId;
import org.zanata.webtrans.shared.model.TransUnitUpdateInfo;
import org.zanata.webtrans.shared.model.WorkspaceId;
import org.zanata.webtrans.shared.rpc.RevertTransUnitUpdates;
import org.zanata.webtrans.shared.rpc.UpdateTransUnitResult;

import com.google.common.collect.Lists;

/**
 * @author Patrick Huang <a
 *         href="mailto:pahuang@redhat.com">pahuang@redhat.com</a>
 */
public class RevertTransUnitUpdatesHandlerTest extends ZanataTest {
    private RevertTransUnitUpdatesHandler handler;
    @Mock
    private ResourceUtils resourceUtils;
    @Mock
    private TranslationService translationServiceImpl;
    @Mock
    private SecurityService securityServiceImpl;
    @Mock
    private TranslationWorkspace translationWorkspace;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        SeamAutowire seam = SeamAutowire.instance().reset();
        // must create before transUnitTransformer
        seam.use("resourceUtils", resourceUtils);
        TransUnitTransformer transUnitTransformer =
                seam.autowire(TransUnitTransformer.class);
        // @formatter:off
        handler = seam
                .use("translationServiceImpl", translationServiceImpl)
            .use("transUnitTransformer", transUnitTransformer)
            .use("securityServiceImpl", securityServiceImpl)
            .ignoreNonResolvable()
            .autowire(RevertTransUnitUpdatesHandler.class);
      // @formatter:on
    }

    @Test
    public void testExecute() throws Exception {
        List<TransUnitUpdateInfo> updatesToRevert =
                Lists.newArrayList(new TransUnitUpdateInfo(true, true,
                        new DocumentId(new Long(1), ""), TestFixture
                                .makeTransUnit(1), 0, 0, ContentState.Approved));
        RevertTransUnitUpdates action =
                new RevertTransUnitUpdates(updatesToRevert);
        action.setWorkspaceId(new WorkspaceId(new ProjectIterationId("", "",
                ProjectType.File), LocaleId.EN_US));

        TranslationService.TranslationResult translationResult =
                mockTranslationResult(ContentState.NeedReview, 0);
        when(
                translationServiceImpl.revertTranslations(LocaleId.EN_US,
                        action.getUpdatesToRevert())).thenReturn(
                Lists.newArrayList(translationResult));

        UpdateTransUnitResult result = handler.execute(action, null);

        assertThat(result.getUpdateInfoList(), Matchers.hasSize(1));
        assertThat(result.getUpdateInfoList().get(0).getPreviousState(),
                Matchers.equalTo(ContentState.NeedReview));
    }

    private static TranslationService.TranslationResult mockTranslationResult(
            ContentState baseContentState, int baseVersionNum) {
        TranslationService.TranslationResult translationResult =
                mock(TranslationService.TranslationResult.class);
        when(translationResult.isTargetChanged()).thenReturn(true);
        when(translationResult.isTranslationSuccessful()).thenReturn(true);
        when(translationResult.getBaseContentState()).thenReturn(
                baseContentState);
        when(translationResult.getBaseVersionNum()).thenReturn(baseVersionNum);
        HTextFlow hTextFlow =
                TestFixture.makeHTextFlow(1, new HLocale(LocaleId.EN_US),
                        ContentState.Approved);
        HDocument spy = spy(new HDocument());
        when(spy.getId()).thenReturn(1L);
        hTextFlow.setDocument(spy);
        when(translationResult.getTranslatedTextFlowTarget()).thenReturn(
                new HTextFlowTarget(hTextFlow, new HLocale(LocaleId.DE)));

        return translationResult;
    }

    @Test
    public void testRollback() throws Exception {
        handler.rollback(null, null, null);
    }
}
