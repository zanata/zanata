package org.zanata.page.groups;

import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.zanata.page.BasePage;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;

/**
 * @author Patrick Huang <a
 *         href="mailto:pahuang@redhat.com">pahuang@redhat.com</a>
 */
@Slf4j
public class CreateVersionGroupPage extends BasePage {

    public final static String LENGTH_ERROR =
            "value must be shorter than or equal to 100 characters";

    public final static String VALIDATION_ERROR =
            "must start and end with letter or number, and contain only " +
            "letters, numbers, periods, underscores and hyphens.";

    @FindBy(id = "group-form:descriptionField:description")
    private WebElement groupDescriptionField;

    @FindBy(id = "group-form:group-create-new")
    private WebElement saveButton;

    public CreateVersionGroupPage(WebDriver driver) {
        super(driver);
        List<By> elementBys =
                ImmutableList.<By> builder()
                        .add(By.id("group-form:slugField:slug"))
                        .add(By.id("group-form:nameField:name"))
                        .add(By.id("group-form:descriptionField:description"))
                        .add(By.id("group-form:group-create-new")).build();
        waitForPage(elementBys);
    }

    public CreateVersionGroupPage inputGroupId(String groupId) {
        log.info("Enter Group ID {}", groupId);
        getGroupSlugField().sendKeys(groupId);
        return new CreateVersionGroupPage(getDriver());
    }

    private WebElement getGroupSlugField() {
        return getDriver().findElement(By.id("group-form:slugField:slug"));
    }

    public String getGroupIdValue() {
        log.info("Query Group ID");
        return getGroupSlugField().getAttribute("value");
    }

    public CreateVersionGroupPage inputGroupName(String groupName) {
        log.info("Enter Group name {}", groupName);
        getGroupNameField().sendKeys(groupName);
        return new CreateVersionGroupPage(getDriver());
    }

    private WebElement getGroupNameField() {
        return getDriver().findElement(By.id("group-form:nameField:name"));
    }

    public CreateVersionGroupPage inputGroupDescription(String desc) {
        log.info("Enter Group description {}", desc);
        groupDescriptionField.sendKeys(desc);
        return this;
    }

    public VersionGroupsPage saveGroup() {
        log.info("Click Save");
        clickAndCheckErrors(saveButton);
        return new VersionGroupsPage(getDriver());
    }

    public CreateVersionGroupPage saveGroupFailure() {
        log.info("Click Save");
        saveButton.click();
        return new CreateVersionGroupPage(getDriver());
    }

    public CreateVersionGroupPage clearFields() {
        getGroupSlugField().clear();
        getGroupNameField().clear();
        groupDescriptionField.clear();
        waitForAMoment().until(new Predicate<WebDriver>() {
            @Override
            public boolean apply(WebDriver input) {
                return getGroupIdValue().equals("") && getGroupNameField().getAttribute("value").equals("");
            }
        });
        return new CreateVersionGroupPage(getDriver());
    }
}
