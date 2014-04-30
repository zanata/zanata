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
import java.util.HashMap;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.zanata.feature.DetailedTest;
import org.zanata.page.projectversion.VersionLanguagesPage;
import org.zanata.page.webtrans.EditorPage;
import org.zanata.util.CleanDocumentStorageRule;
import org.zanata.util.SampleProjectRule;
import org.zanata.util.TestFileGenerator;
import org.zanata.workflow.BasicWorkFlow;
import org.zanata.workflow.LoginWorkFlow;
import org.zanata.workflow.ProjectWorkFlow;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.zanata.util.FunctionalTestHelper.assumeFalse;

/**
 * @author Damian Jansen <a
 *         href="mailto:djansen@redhat.com">djansen@redhat.com</a>
 */
@Category(DetailedTest.class)
public class TranslateOdsTest {

    @Rule
    public SampleProjectRule sampleProjectRule = new SampleProjectRule();

    @Rule
    public CleanDocumentStorageRule documentStorageRule =
            new CleanDocumentStorageRule();

    private TestFileGenerator testFileGenerator = new TestFileGenerator();

    @Before
    public void before() {
        new BasicWorkFlow().goToHome().deleteCookiesAndRefresh();
        assumeFalse(
                "",
                new File(CleanDocumentStorageRule.getDocumentStoragePath()
                        .concat(File.separator).concat("documents")
                        .concat(File.separator)).exists());
        new LoginWorkFlow().signIn("admin", "admin");
    }

    @Test
    public void translateBasicOdsFile() {
        File testfile = testFileGenerator.openTestFile("test-ods.ods");

        HashMap<String, String> projectSettings =
                ProjectWorkFlow.projectDefaults();
        projectSettings.put("Project ID", "ods-project");
        projectSettings.put("Name", "ods-project");
        projectSettings.put("Project Type", "File");

        VersionLanguagesPage projectVersionPage =
                new ProjectWorkFlow().createNewProject(projectSettings)
                        .clickCreateVersionLink().inputVersionId("ods")
                        .saveVersion()
                        .gotoSettingsTab()
                        .gotoSettingsDocumentsTab()
                        .pressUploadFileButton()
                        .enterFilePath(testfile.getAbsolutePath())
                        .submitUpload();

        EditorPage editorPage =
                projectVersionPage.translate("fr", testfile.getName());

        editorPage.setSyntaxHighlighting(false);

        assertThat("Item 1 shows TestODS (the sheet name)",
                editorPage.getMessageSourceAtRowIndex(0),
                Matchers.equalTo("TestODS"));
        assertThat("Item 2 shows First (the page name)",
                editorPage.getMessageSourceAtRowIndex(1),
                Matchers.equalTo("First"));
        assertThat("Item 3 shows Line One",
                editorPage.getMessageSourceAtRowIndex(2),
                Matchers.equalTo("Line One"));
        assertThat("Item 4 shows Line Two",
                editorPage.getMessageSourceAtRowIndex(3),
                Matchers.equalTo("Line Two"));
        assertThat("Item 5 shows Line Three",
                editorPage.getMessageSourceAtRowIndex(4),
                Matchers.equalTo("Line Three"));

        editorPage =
                editorPage.translateTargetAtRowIndex(0, "TestODS")
                        .approveTranslationAtRow(0);
        editorPage =
                editorPage.translateTargetAtRowIndex(1, "Début")
                        .approveTranslationAtRow(1);
        editorPage =
                editorPage.translateTargetAtRowIndex(2, "Une Ligne")
                        .approveTranslationAtRow(2);
        editorPage =
                editorPage.translateTargetAtRowIndex(3, "Deux Ligne")
                        .approveTranslationAtRow(3);
        editorPage =
                editorPage.translateTargetAtRowIndex(4, "Ligne Trois")
                        .approveTranslationAtRow(4);

        assertThat("Item 1 shows a translation of the sheet name",
                editorPage.getBasicTranslationTargetAtRowIndex(0),
                Matchers.equalTo("TestODS"));
        assertThat("Item 1 shows a translation of page name",
                editorPage.getBasicTranslationTargetAtRowIndex(1),
                Matchers.equalTo("Début"));
        assertThat("Item 1 shows a translation of Line One",
                editorPage.getBasicTranslationTargetAtRowIndex(2),
                Matchers.equalTo("Une Ligne"));
        assertThat("Item 1 shows a translation of Line One",
                editorPage.getBasicTranslationTargetAtRowIndex(3),
                Matchers.equalTo("Deux Ligne"));
        assertThat("Item 1 shows a translation of Line One",
                editorPage.getBasicTranslationTargetAtRowIndex(4),
                Matchers.equalTo("Ligne Trois"));

        // Close and reopen the editor to test save, switches to CodeMirror
        editorPage.reload();

        assertThat("Item 1 shows a translation of the sheet name",
                editorPage.getMessageTargetAtRowIndex(0),
                Matchers.equalTo("TestODS"));
        assertThat("Item 1 shows a translation of page name",
                editorPage.getMessageTargetAtRowIndex(1),
                Matchers.equalTo("Début"));
        assertThat("Item 1 shows a translation of Line One",
                editorPage.getMessageTargetAtRowIndex(2),
                Matchers.equalTo("Une Ligne"));
        assertThat("Item 1 shows a translation of Line One",
                editorPage.getMessageTargetAtRowIndex(3),
                Matchers.equalTo("Deux Ligne"));
        assertThat("Item 1 shows a translation of Line One",
                editorPage.getMessageTargetAtRowIndex(4),
                Matchers.equalTo("Ligne Trois"));
    }
}
