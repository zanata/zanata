package org.zanata.feature.misc;

import java.util.concurrent.Callable;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.zanata.feature.DetailedTest;
import org.zanata.page.projects.ProjectVersionPage;
import org.zanata.page.webtrans.EditorPage;
import org.zanata.rest.dto.resource.Resource;
import org.zanata.util.SampleProjectRule;
import org.zanata.util.ZanataRestCaller;
import org.zanata.workflow.BasicWorkFlow;
import org.zanata.workflow.LoginWorkFlow;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.zanata.util.ZanataRestCaller.buildSourceResource;
import static org.zanata.util.ZanataRestCaller.buildTextFlow;
import static org.zanata.workflow.BasicWorkFlow.EDITOR_TEMPLATE;
import static org.zanata.workflow.BasicWorkFlow.PROJECT_VERSION_TEMPLATE;

/**
 * TCMS test case <a
 * href="https://tcms.engineering.redhat.com/case/137717/">137717</a>
 *
 * @author Patrick Huang <a
 *         href="mailto:pahuang@redhat.com">pahuang@redhat.com</a>
 */
@Category(DetailedTest.class)
public class ObsoleteTextTest {
    @Rule
    public SampleProjectRule rule = new SampleProjectRule();
    private ZanataRestCaller restCaller;

    @Before
    public void setUp() throws Exception {
        restCaller = new ZanataRestCaller();
    }

    /**
     * Below is a modified version of what's in TCMS test cases.
     *
     * Project "obsolete-test" is created, and version "master" is created, and
     * source texts (message1 and message2) are pushed. Language fr should be
     * enabled. User "admin" exists and join fr language team.
     *
     * <pre>
     * 1. Open obsolete-test in lang fr.
     * 2. Start editing the first entry and save.
     * 3. Leave the translation editor.
     * 4. In command line: remove message2 but keep message1.
     * 5. In command line: edit message1, remove other entries except the first three.
     * 6. Push with pushTrans=true and copyTrans=true.
     * 7. Back to Web UI: translate and save the 2nd and 3rd entries.
     * 8. Click on "Leave Workspace".
     * 9. Go project page of AboutFedora and Check whether the bem is 100% translated.
     * 10. Enter translation editor for bem and see whether the project and document are 100%translated.
     * </pre>
     */
    @Test
    public void obsoleteTextTest() {
        restCaller
                .createProjectAndVersion("obsolete-test", "master", "gettext");
        Resource resource1 =
                buildSourceResource("message1",
                        buildTextFlow("res1", "message one"),
                        buildTextFlow("res2", "message two"),
                        buildTextFlow("res3", "message three"),
                        buildTextFlow("res4", "message four"));

        Resource resource2 =
                buildSourceResource("message2",
                        buildTextFlow("res1", "message one"),
                        buildTextFlow("res2", "message two"),
                        buildTextFlow("res3", "message three"),
                        buildTextFlow("res4", "message four"));

        restCaller.postSourceDocResource("obsolete-test", "master", resource1,
                false);
        restCaller.postSourceDocResource("obsolete-test", "master", resource2,
                false);

        // edit first entry and save
        new LoginWorkFlow().signIn("admin", "admin");
        final EditorPage editorPage = openEditor();
        editorPage.translateTargetAtRowIndex(0, "message one translated")
                .approveTranslationAtRow(0);

        // delete resource 2
        resource2.getTextFlows().clear();
        restCaller.deleteSourceDoc("obsolete-test", "master", "message2");
        // remove last entry from resource 1
        resource1.getTextFlows().remove(3);
        restCaller.putSourceDocResource("obsolete-test", "master", "message1",
                resource1, true);

        final EditorPage editorPageFinal =
                openEditor()
                        .translateTargetAtRowIndex(1, "message two translated")
                        .approveTranslationAtRow(1)
                        .translateTargetAtRowIndex(2, "translated")
                        .approveTranslationAtRow(2);

        editorPageFinal.waitFor(new Callable<String>() {
            @Override
            public String call() throws Exception {
                return editorPageFinal.getStatistics();
            }
        }, Matchers.containsString("100%"));

        ProjectVersionPage versionPage =
                new BasicWorkFlow().goToPage(String.format(
                        PROJECT_VERSION_TEMPLATE, "obsolete-test", "master"),
                        ProjectVersionPage.class);
        assertThat(versionPage.getStatisticsForLocale("fr"),
                Matchers.equalTo("100.0%"));
    }

    private static EditorPage openEditor() {
        String url =
                String.format(EDITOR_TEMPLATE, "obsolete-test", "master", "fr",
                        "message1");
        return new BasicWorkFlow().goToPage(url, EditorPage.class)
                .setSyntaxHighlighting(false);
    }
}
