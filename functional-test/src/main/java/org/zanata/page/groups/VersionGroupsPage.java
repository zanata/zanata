package org.zanata.page.groups;

import com.google.common.base.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.zanata.page.BasePage;
import org.zanata.util.WebElementUtil;

import java.util.List;

/**
 * @author Patrick Huang <a
 *         href="mailto:pahuang@redhat.com">pahuang@redhat.com</a>
 */
@Slf4j
public class VersionGroupsPage extends BasePage {
    public static final int GROUP_NAME_COLUMN = 0;
    public static final int GROUP_DESCRIPTION_COLUMN = 1;
    public static final int GROUP_TIMESTAMP_COLUMN = 2;
    public static final int GROUP_STATUS_COLUMN = 3;

    @FindBy(id = "groupForm:groupTable")
    private WebElement groupTable;

    @FindBy(className = "infomsg.icon-info-circle-2")
    private WebElement infomsg;

    public VersionGroupsPage(WebDriver driver) {
        super(driver);
    }

    public List<String> getGroupNames() {
        By by = By.id("groupForm:groupTable");
        return WebElementUtil.getColumnContents(getDriver(), by,
                GROUP_NAME_COLUMN);
    }

    public CreateVersionGroupPage createNewGroup() {
        log.info("Click New Group button");
        WebElement createLink =
                getDriver().findElement(By.id("group-create"));
        createLink.click();
        return new CreateVersionGroupPage(getDriver());
    }

    public VersionGroupPage goToGroup(String groupName) {
        log.info("Click group {}", groupName);
        groupTable.findElement(By.linkText(groupName)).click();
        return new VersionGroupPage(getDriver());
    }

    public VersionGroupsPage toggleObsolete(final boolean show) {
        WebElement showObsolete =
                getDriver().findElement(By.id("groupForm:showObsolete"));
        if (show != showObsolete.isSelected()) {
            showObsolete.click();
        }
        waitForTenSec().until(new Predicate<WebDriver>() {
            @Override
            public boolean apply(WebDriver input) {
                WebElement table =
                        input.findElement(By.id("groupForm:groupTable"));
                return table.findElements(By.className("obsolete_link"))
                        .isEmpty() == !show;
            }
        });
        return new VersionGroupsPage(getDriver());
    }

    public String getInfoMessage() {
        log.info("Test info msg");
        log.info(getDriver().findElement(
                By.className("infomsg.icon-info-circle-2")).getText());
        return getDriver().findElement(
                By.className("infomsg.icon-info-circle-2")).getText();
    }

}
