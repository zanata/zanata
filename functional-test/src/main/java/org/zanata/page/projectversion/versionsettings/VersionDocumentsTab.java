package org.zanata.page.projectversion.versionsettings;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.zanata.page.projectversion.VersionBasePage;
import org.zanata.page.projectversion.VersionLanguagesPage;

import java.util.List;

/**
 * @author Damian Jansen <a href="mailto:djansen@redhat.com">djansen@redhat.com</a>
 */
public class VersionDocumentsTab extends VersionBasePage {

    public VersionDocumentsTab(WebDriver driver) {
        super(driver);
    }

    public VersionDocumentsTab pressUploadFileButton() {
        clickLinkAfterAnimation(By.id("button--upload-new-document"));
        return new VersionDocumentsTab(getDriver());
    }

    /**
     * Query for the status of the upload button in the submit dialog
     *
     * @return boolean can submit file upload
     */
    public boolean canSubmitDocument() {
        return getDriver().findElement(
                By.id("uploadDocForm:generalDocSubmitUploadButton"))
                .isEnabled();
    }

    public VersionDocumentsTab cancelUpload() {
        getDriver().findElement(
                By.id("uploadDocForm:generalDocCancelUploadButton")).click();
        return new VersionDocumentsTab(getDriver());
    }

    public VersionDocumentsTab enterFilePath(String filePath) {
        getDriver().findElement(By.id("uploadDocForm:generalDocFileUpload"))
                .sendKeys(filePath);
        return new VersionDocumentsTab(getDriver());
    }

    public VersionLanguagesPage submitUpload() {
        getDriver().findElement(
                By.id("uploadDocForm:generalDocSubmitUploadButton")).click();
        return new VersionLanguagesPage(getDriver());
    }

    public boolean sourceDocumentsContains(String document) {
        gotoDocumentTab();
        List<WebElement> documentList = getDriver()
                .findElement(By.id("settings-document_form"))
                .findElement(By.tagName("ul"))
                .findElements(By.tagName("li"));
        for (WebElement tableRow : documentList) {
            if (tableRow.findElement(By.tagName("label"))
                    .getText().contains(document)) {
                return true;
            }
        }
        return false;
    }
}
