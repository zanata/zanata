package org.zanata.page.utility;

import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.zanata.page.BasePage;
import org.zanata.util.WebElementUtil;

/**
 * @author Patrick Huang <a
 *         href="mailto:pahuang@redhat.com">pahuang@redhat.com</a>
 */
@Slf4j
public class ContactAdminFormPage extends BasePage {
    @FindBy(id = "contactAdminForm:subjectField:subject")
    private WebElement subjectField;

    @FindBy(id = "contactAdminForm:messageField:message")
    private WebElement messageField;

    @FindBy(id = "contactAdminForm:send")
    private WebElement sendButton;

    public ContactAdminFormPage(WebDriver driver) {
        super(driver);
    }

    public ContactAdminFormPage inputSubject(String subject) {
        log.info("Enter subject {}", subject);
        subjectField.clear();
        subjectField.sendKeys(subject);
        return new ContactAdminFormPage(getDriver());
    }

    public ContactAdminFormPage inputMessage(String message) {
        log.info("Enter message {}", message);
        WebElementUtil
                .setRichTextEditorContent(getDriver(), messageField, message);
        return new ContactAdminFormPage(getDriver());
    }

    public HelpPage send() {
        log.info("Click Send");
        sendButton.click();
        return new HelpPage(getDriver());
    }
}
