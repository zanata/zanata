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

import java.io.File;

import org.hamcrest.Matchers;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.experimental.categories.Category;
import org.junit.experimental.theories.DataPoint;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;
import org.zanata.feature.testharness.ZanataTestCase;
import org.zanata.feature.testharness.TestPlan.DetailedTest;
import org.zanata.page.webtrans.EditorPage;
import org.zanata.util.CleanDocumentStorageRule;
import org.zanata.util.SampleProjectRule;
import org.zanata.util.TestFileGenerator;
import org.zanata.workflow.LoginWorkFlow;
import org.zanata.workflow.ProjectWorkFlow;

import static org.hamcrest.MatcherAssert.assertThat;

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
    public static SampleProjectRule sampleProjectRule = new SampleProjectRule();

    @ClassRule
    public static CleanDocumentStorageRule documentStorageRule =
            new CleanDocumentStorageRule();

    private TestFileGenerator testFileGenerator = new TestFileGenerator();

    @DataPoint
    public static String TEST_htm = "htm";
    @DataPoint
    public static String TEST_html = "html";

    @BeforeClass
    public static void beforeClass() {
        new LoginWorkFlow().signIn("admin", "admin");
        new ProjectWorkFlow().createNewProjectVersion("about fedora",
                "html-translate", "File");
    }

    @Theory
    public void translateBasicHTMLFile(String extension) {
        File testfile =
                testFileGenerator.generateTestFileWithContent("basichtml", "."
                        + extension, "<html><body>Line One<p>Line Two<p>"
                        + "Line Three</body></html>");

        EditorPage editorPage = new ProjectWorkFlow()
                .goToProjectByName("about fedora")
                .gotoVersion("html-translate")
                .gotoSettingsTab()
                .gotoSettingsDocumentsTab()
                .pressUploadFileButton()
                .enterFilePath(testfile.getAbsolutePath())
                .submitUpload()
                .clickUploadDone()
                .gotoLanguageTab()
                .translate("fr", testfile.getName());

        assertThat("Item 1 shows Line One",
                editorPage.getMessageSourceAtRowIndex(0),
                Matchers.equalTo("Line One"));
        assertThat("Item 2 shows Line Two",
                editorPage.getMessageSourceAtRowIndex(1),
                Matchers.equalTo("Line Two"));
        assertThat("Item 3 shows Line Three",
                editorPage.getMessageSourceAtRowIndex(2),
                Matchers.equalTo("Line Three"));

        editorPage =
                editorPage.translateTargetAtRowIndex(0, "Une Ligne")
                        .approveTranslationAtRow(0);
        editorPage =
                editorPage.translateTargetAtRowIndex(1, "Deux Ligne")
                        .approveTranslationAtRow(1);
        editorPage =
                editorPage.translateTargetAtRowIndex(2, "Ligne Trois")
                        .approveTranslationAtRow(2);

        assertThat("Item 1 shows a translation of Line One",
                editorPage.getBasicTranslationTargetAtRowIndex(0),
                Matchers.equalTo("Une Ligne"));
        assertThat("Item 2 shows a translation of Line Two",
                editorPage.getBasicTranslationTargetAtRowIndex(1),
                Matchers.equalTo("Deux Ligne"));
        assertThat("Item 3 shows a translation of Line Three",
                editorPage.getBasicTranslationTargetAtRowIndex(2),
                Matchers.equalTo("Ligne Trois"));

        // Close and reopen the editor to test save, switches to CodeMirror
        editorPage.reload();

        assertThat("Item 1 shows a translation of Line One",
                editorPage.getBasicTranslationTargetAtRowIndex(0),
                Matchers.equalTo("Une Ligne"));
        assertThat("Item 2 shows a translation of Line Two",
                editorPage.getBasicTranslationTargetAtRowIndex(1),
                Matchers.equalTo("Deux Ligne"));
        assertThat("Item 3 shows a translation of Line Three",
                editorPage.getBasicTranslationTargetAtRowIndex(2),
                Matchers.equalTo("Ligne Trois"));
    }
}
