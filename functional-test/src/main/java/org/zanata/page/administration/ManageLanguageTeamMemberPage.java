package org.zanata.page.administration;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.zanata.page.BasePage;
import org.zanata.util.Checkbox;
import org.zanata.util.TableRow;
import org.zanata.util.WebElementUtil;

import java.util.Collections;
import java.util.List;

/**
 * @author Patrick Huang <a
 *         href="mailto:pahuang@redhat.com">pahuang@redhat.com</a>
 */
@Slf4j
public class ManageLanguageTeamMemberPage extends BasePage {
    @FindBy(id = "memberPanel")
    private WebElement memberPanel;

    public static final int USERNAME_COLUMN = 0;
    public static final int SEARCH_RESULT_PERSON_COLUMN = 0;
    public static final int ISTRANSLATOR_COLUMN = 2;

    public ManageLanguageTeamMemberPage(WebDriver driver) {
        super(driver);
    }

    private String getMembersInfo() {
        log.info("Query members info");
        WebElement memberInfo = memberPanel.findElement(By.xpath(".//p"));
        return memberInfo.getText();
    }

    public List<String> getMemberUsernames() {
        log.info("Query username list");
        if (getMembersInfo().contains("0 members")) {
            log.info("no members yet for this language");
            return Collections.emptyList();
        }
        By by = By.id("memberPanel:threads");
        List<String> usernameColumn =
                WebElementUtil.getColumnContents(getDriver(), by,
                        USERNAME_COLUMN);
        log.info("username column: {}", usernameColumn);
        return usernameColumn;
    }

    public ManageLanguageTeamMemberPage joinLanguageTeam() {
        log.info("Click Join");
        // Waiting 10 seconds for an element to be present on the page, checking
        // for its presence once every 1 second.
        WebElement joinLanguageTeamLink =
                waitForAMoment().until(new Function<WebDriver, WebElement>() {
                    public WebElement apply(WebDriver driver) {
                        return driver.findElement(By
                                .linkText("Join Language Team"));
                    }
                });
        joinLanguageTeamLink.click();
        // we need to wait for this join to finish before returning the page
        waitForAMoment().until(new Function<WebDriver, Boolean>() {
            @Override
            public Boolean apply(WebDriver driver) {
                List<WebElement> joinLanguageTeam =
                        driver.findElements(By.linkText("Join Language Team"));
                return joinLanguageTeam.isEmpty();
            }
        });
        return new ManageLanguageTeamMemberPage(getDriver());
    }

    public ManageLanguageTeamMemberPage clickAddTeamMember() {
        log.info("Click Add Team Member");
        WebElement addTeamMemberLink =
                waitForAMoment().until(new Function<WebDriver, WebElement>() {
                    public WebElement apply(WebDriver driver) {
                        return driver.findElement(By.id("addTeamMemberLink"));
                    }
                });
        addTeamMemberLink.click();
        return this;
    }

    public ManageLanguageTeamMemberPage searchPersonAndAddToTeam(
            final String personName) {
        log.info("Enter username search {}", personName);
        final WebElement addUserPanel =
                getDriver().findElement(By.id("userAddPanel_container"));

        WebElement searchInput =
                waitForAMoment().until(new Function<WebDriver, WebElement>() {
                    public WebElement apply(WebDriver driver) {
                        return addUserPanel.findElement(By
                                .id("searchForm:searchField"));
                    }
                });
        searchInput.sendKeys(personName);
        WebElement searchButton =
                getDriver().findElement(By.id("searchForm:searchBtn"));
        searchButton.click();

        return waitForAMoment().until(
                new Function<WebDriver, ManageLanguageTeamMemberPage>() {
                    @Override
                    public ManageLanguageTeamMemberPage apply(WebDriver driver) {
                        TableRow firstRow =
                                tryGetFirstRowInSearchPersonResult(driver,
                                        personName);

                        final String personUsername =
                                firstRow.getCellContents().get(
                                        SEARCH_RESULT_PERSON_COLUMN);
                        log.info("username to be added: {}", personUsername);
                        WebElement isTranslatorCheckBox =
                                firstRow.getCells().get(ISTRANSLATOR_COLUMN)
                                        .findElement(By.tagName("input"));
                        Checkbox.of(isTranslatorCheckBox).check();

                        getDriver().findElement(
                                By.id("resultForm:addSelectedBtn")).click();
                        getDriver().findElement(By.id("searchForm:closeBtn"))
                                .click();
                        return confirmAdded(personName);
                    }
                });

    }

    private TableRow tryGetFirstRowInSearchPersonResult(WebDriver driver,
            String personName) {
        WebElement table = driver.findElement(By.id("resultForm:personTable"));
        List<TableRow> tableRows =
                WebElementUtil.getTableRows(getDriver(), table);
        // we want to wait until search result comes back
        if (tableRows.isEmpty()
                || !tableRows.get(0).getCellContents()
                        .get(SEARCH_RESULT_PERSON_COLUMN).contains(personName)) {
            log.debug("waiting for search result refresh...");
            throw new NoSuchElementException("result is not shown yet");
        }
        return tableRows.get(0);
    }

    private ManageLanguageTeamMemberPage confirmAdded(
            final String personUsername) {
        // we need to wait for the page to refresh
        return refreshPageUntil(this, new Predicate<WebDriver>() {
            @Override
            public boolean apply(WebDriver driver) {
                By byId = By.id("memberPanel:threads");
                List<String> usernameColumn =
                        WebElementUtil.getColumnContents(getDriver(), byId,
                                USERNAME_COLUMN);
                log.info("username column: {}", usernameColumn);
                return usernameColumn.contains(personUsername);
            }
        });
    }
}
