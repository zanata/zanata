package org.zanata.page.groups;

import java.util.*;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.zanata.page.AbstractPage;
import org.zanata.util.TableRow;
import org.zanata.util.WebElementUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Patrick Huang <a href="mailto:pahuang@redhat.com">pahuang@redhat.com</a>
 */
@Slf4j
public class VersionGroupsPage extends AbstractPage
{
   public static final int GROUP_NAME_COLUMN = 0;
   public static final int GROUP_DESCRIPTION_COLUMN = 1;
   public static final int GROUP_TIMESTAMP_COLUMN = 2;
   public static final int GROUP_STATUS_COLUMN = 3;

   @FindBy(id = "groupForm:groupTable")
   private WebElement groupTable;

   @FindBy(className = "infomsg.icon-info-circle-2")
   private WebElement infomsg;

   @FindBy(linkText = "Show obsolete groups")
   private WebElement obsoleteCheckbox;

   public VersionGroupsPage(WebDriver driver)
   {
      super(driver);
   }

   public List<String> getGroupNames()
   {
      By by = By.id("groupForm:groupTable");
      return WebElementUtil.getColumnContents(getDriver(), by, GROUP_NAME_COLUMN);
   }

   public CreateVersionGroupPage createNewGroup()
   {
      WebElement createLink = getDriver().findElement(By.linkText("Create New Group"));
      createLink.click();
      return new CreateVersionGroupPage(getDriver());
   }

   public VersionGroupPage goToGroup(String groupName)
   {
      WebElement groupLink = groupTable.findElement(By.linkText(groupName));
      groupLink.click();
      return new VersionGroupPage(getDriver());
   }

   public VersionGroupsPage toggleObsolete()
   {
      getDriver().findElement(By.xpath("//div/input[2]")).click();
      return this;
   }

   public String getInfoMessage()
   {
      log.info("Test info msg");
      log.info(getDriver().findElement(By.className("infomsg.icon-info-circle-2")).getText());
      return getDriver().findElement(By.className("infomsg.icon-info-circle-2")).getText();
   }

}
