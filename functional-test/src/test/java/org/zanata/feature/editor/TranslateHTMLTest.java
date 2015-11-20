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
package org.zanata.feature.editor;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.experimental.categories.Category;
import org.junit.experimental.theories.DataPoint;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;
import org.zanata.feature.Feature;
import org.zanata.feature.testharness.TestPlan.DetailedTest;
import org.zanata.feature.testharness.ZanataTestCase;
import org.zanata.page.webtrans.EditorPage;
import org.zanata.util.CleanDocumentStorageRule;
import org.zanata.util.TestFileGenerator;
import org.zanata.util.ZanataRestCaller;
import org.zanata.workflow.BasicWorkFlow;
import org.zanata.workflow.LoginWorkFlow;
import org.zanata.workflow.ProjectWorkFlow;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;
import static org.zanata.util.FunctionalTestHelper.assumeFalse;

/**
 * @author Damian Jansen <a
 *         href="mailto:djansen@redhat.com">djansen@redhat.com</a>
 */
@RunWith(Theories.class)
@Category(DetailedTest.class)
public class TranslateHTMLTest extends ZanataTestCase {

    @Rule
    public Timeout timeout = new Timeout(ZanataTestCase.MAX_LONG_TEST_DURATION);

    @ClassRule
    public static CleanDocumentStorageRule documentStorageRule =
            new CleanDocumentStorageRule();

    private ZanataRestCaller zanataRestCaller = new ZanataRestCaller();
    private TestFileGenerator testFileGenerator = new TestFileGenerator();

    @DataPoint
    public static String TEST_htm = "htm";
    @DataPoint
    public static String TEST_html = "html";

    @BeforeClass
    public static void beforeClass() {
        new BasicWorkFlow().goToHome().deleteCookiesAndRefresh();
        assumeFalse(
                "",
                new File(CleanDocumentStorageRule.getDocumentStoragePath()
                        .concat(File.separator).concat("documents")
                        .concat(File.separator)).exists());
    }

    @Before
    public void before() {
        assertThat(new LoginWorkFlow().signIn("admin", "admin").loggedInAs())
                .isEqualTo("admin")
                .as("Admin is logged in");
    }

    @Feature(summary = "The user can translate HyperText Markup Language files",
            tcmsTestPlanIds = 5316, tcmsTestCaseIds = 0)
    @Theory
    public void translateBasicHTMLFile(String extension) {
        File testfile = testFileGenerator.generateTestFileWithContent(
                "basichtml", "." + extension,
                "<html><body>Line One<p>Line Two<p>Line Three</body></html>");
        zanataRestCaller.createProjectAndVersion(extension + "-translate",
                extension, "file");

        EditorPage editorPage = new ProjectWorkFlow()
                .goToProjectByName(extension + "-translate")
                .gotoVersion(extension)
                .gotoSettingsTab()
                .gotoSettingsDocumentsTab()
                .pressUploadFileButton()
                .enterFilePath(testfile.getAbsolutePath())
                .submitUpload()
                .clickUploadDone()
                .gotoLanguageTab()
                .translate("fr", testfile.getName());

        assertThat(editorPage.getMessageSourceAtRowIndex(0))
                .isEqualTo("Line One")
                .as("Item 1 shows Line One");

        assertThat(editorPage.getMessageSourceAtRowIndex(1))
                .isEqualTo("Line Two")
                .as("Item 2 shows Line Two");
        assertThat(editorPage.getMessageSourceAtRowIndex(2))
                .isEqualTo("Line Three")
                .as("Item 3 shows Line Three");

        editorPage = editorPage
                .translateTargetAtRowIndex(0, "Une Ligne")
                .approveTranslationAtRow(0)
                .translateTargetAtRowIndex(1, "Deux Ligne")
                .approveTranslationAtRow(1)
                .translateTargetAtRowIndex(2, "Ligne Trois")
                .approveTranslationAtRow(2);

        assertTranslations(editorPage);

        // Close and reopen the editor to test save
        editorPage.reload();

        assertTranslations(editorPage);
    }

    private void assertTranslations(EditorPage editorPage) {
        assertThat(editorPage.getBasicTranslationTargetAtRowIndex(0))
                .isEqualTo("Une Ligne")
                .as("Item 1 shows a translation of Line One");
        assertThat(editorPage.getBasicTranslationTargetAtRowIndex(1))
                .isEqualTo("Deux Ligne")
                .as("Item 2 shows a translation of Line Two");
        assertThat(editorPage.getBasicTranslationTargetAtRowIndex(2))
                .isEqualTo("Ligne Trois")
                .as("Item 3 shows a translation of Line Three");
    }
}
