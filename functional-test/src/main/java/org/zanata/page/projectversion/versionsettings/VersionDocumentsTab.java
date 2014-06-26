package org.zanata.page.projectversion.versionsettings;

import java.util.List;

import com.google.common.base.Predicate;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.zanata.page.projectversion.VersionBasePage;

/**
 * @author Damian Jansen <a
 *         href="mailto:djansen@redhat.com">djansen@redhat.com</a>
 */
public class VersionDocumentsTab extends VersionBasePage {

    public VersionDocumentsTab(WebDriver driver) {
        super(driver);
    }

    public VersionDocumentsTab pressUploadFileButton() {
        clickLinkAfterAnimation(By.id("file-upload-component-toggle-button"));
        return new VersionDocumentsTab(getDriver());
    }

    /**
     * Query for the status of the upload button in the submit dialog
     *
     * @return boolean can submit file upload
     */
    public boolean canSubmitDocument() {
        return getDriver().findElement(
                By.id("file-upload-component-start-upload"))
                .isEnabled();
    }

    public VersionDocumentsTab cancelUpload() {
        getDriver()
                .findElement(By.id("file-upload-component-cancel-upload"))
                .click();
        waitForTenSec().until(new Predicate<WebDriver>() {
            @Override
            public boolean apply( WebDriver input) {
                return !getDriver().findElement(By.id("file-upload-component"))
                        .isDisplayed();
            }
        });
        return new VersionDocumentsTab(getDriver());
    }

    public VersionDocumentsTab enterFilePath(String filePath) {
        // Make the hidden input element slightly not hidden
        ((JavascriptExecutor)getDriver())
                .executeScript("arguments[0].style.visibility = 'visible'; " +
                        "arguments[0].style.height = '1px'; " +
                        "arguments[0].style.width = '1px'; " +
                        "arguments[0].style.opacity = 1",
                        getDriver().findElement(
                                By.id("file-upload-component-file-input")));

        getDriver().findElement(
                By.id("file-upload-component-file-input"))
                .sendKeys(filePath);
        return new VersionDocumentsTab(getDriver());
    }

    public VersionDocumentsTab submitUpload() {
        waitForTenSec().until(new Predicate<WebDriver>() {
            @Override
            public boolean apply(WebDriver input) {
                return getDriver().findElement(
                        By.id("file-upload-component-start-upload"))
                        .isEnabled();
            }
        });
        getDriver().findElement(
                By.id("file-upload-component-start-upload")).click();
        return new VersionDocumentsTab(getDriver());
    }

    public VersionDocumentsTab clickUploadDone() {
        waitForTenSec().until(new Predicate<WebDriver>() {
            @Override
            public boolean apply(WebDriver input) {
                return getDriver()
                .findElement(By.id("file-upload-component-done-upload"))
                .isEnabled();
            }
        });
        getDriver().findElement(By.id("file-upload-component-done-upload"))
                .click();
        return new VersionDocumentsTab(getDriver());
    }

    public boolean sourceDocumentsContains(String document) {
        List<WebElement> documentLabelList =
                getDriver()
                        .findElement(By.id("settings-document_form"))
                        .findElement(By.tagName("ul"))
                        .findElements(
                                By.xpath(".//li/label[@class='form__checkbox__label']"));
        for (WebElement label : documentLabelList) {
            if (label.getText().contains(document)) {
                return true;
            }
        }
        return false;
    }

    public String getUploadError() {
        waitForTenSec().until(new Predicate<WebDriver>() {
            @Override
            public boolean apply(WebDriver input) {
                return getDriver().findElement(By.id("file-upload-component"))
                        .findElement(By.className("txt--danger")).isDisplayed();
            }
        });
        return getDriver().findElement(By.id("file-upload-component"))
                .findElement(By.className("txt--danger")).getText();
    }
}
