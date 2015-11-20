/*
 * Copyright 2013, Red Hat, Inc. and individual contributors as indicated by the
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
package org.zanata.feature.document;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.zanata.feature.Feature;
import org.zanata.feature.testharness.TestPlan.DetailedTest;
import org.zanata.feature.testharness.ZanataTestCase;
import org.zanata.page.projectversion.VersionDocumentsPage;
import org.zanata.page.projectversion.versionsettings.VersionDocumentsTab;
import org.zanata.page.webtrans.EditorPage;
import org.zanata.util.Constants;
import org.zanata.util.PropertiesHolder;
import org.zanata.util.TestFileGenerator;
import org.zanata.util.ZanataRestCaller;
import org.zanata.workflow.LoginWorkFlow;
import org.zanata.workflow.ProjectWorkFlow;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Damian Jansen
 * <a href="mailto:djansen@redhat.com">djansen@redhat.com</a>
 */
@Category(DetailedTest.class)
public class HTMLDocumentTypeTest extends ZanataTestCase {

    private TestFileGenerator testFileGenerator = new TestFileGenerator();

    @Before
    public void before() {
        new ZanataRestCaller("translator", PropertiesHolder.getProperty(
                Constants.zanataTranslatorKey.value()))
                .createProjectAndVersion("html-project", "html-upload", "file");
        new LoginWorkFlow().signIn("translator", "translator");
    }

    @Feature(bugzilla = 980670,
            summary = "Maintainer can upload a HTML file for translation",
            tcmsTestCaseIds = { 377837 },
            tcmsTestPlanIds = { 5316 })
    @Test(timeout = ZanataTestCase.MAX_SHORT_TEST_DURATION)
    public void uploadHTMLFileAsMaintainer() throws Exception {
        File htmlfile = testFileGenerator.generateTestFileWithContent(
                "testhtmlfile", ".html",
                "<html><title>Test content</title>" +
                "<br>This is <b>Bold</b> text</html>"
        );
        String testFileName = htmlfile.getName();
        VersionDocumentsTab versionDocumentsTab = new ProjectWorkFlow()
                .goToProjectByName("html-project")
                .gotoVersion("html-upload")
                .gotoSettingsTab()
                .gotoSettingsDocumentsTab()
                .pressUploadFileButton()
                .enterFilePath(htmlfile.getAbsolutePath())
                .submitUpload()
                .clickUploadDone();

        VersionDocumentsPage versionDocumentsPage =
                versionDocumentsTab.gotoDocumentTab();

        assertThat(versionDocumentsPage
                .sourceDocumentsContains(htmlfile.getName()))
                .as("Document shows in table");

        EditorPage editorPage = versionDocumentsPage
                .gotoLanguageTab()
                .translate("pl", testFileName);

        assertThat(editorPage.getMessageSourceAtRowIndex(0))
                .isEqualTo("Test content")
                .as("The first translation source is correct");
        assertThat(editorPage.getMessageSourceAtRowIndex(1))
                .isEqualTo("This is <g2>Bold</g2> text")
                .as("The second translation source is correct");
    }
}
