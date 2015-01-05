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
package org.zanata.page.administration;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.zanata.page.BasePage;
import org.zanata.util.Checkbox;
import org.zanata.util.TableRow;
import org.zanata.util.WebElementUtil;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;

/**
 * @author Patrick Huang <a
 *         href="mailto:pahuang@redhat.com">pahuang@redhat.com</a>
 */
@Slf4j
public class ManageLanguageTeamMemberPage extends BasePage {

    private By memberPanel = By.id("memberPanel");
    private By memberPanelRows = By.id("memberPanel:threads");
    private By joinLanguageTeamButton = By.linkText("Join Language Team");
    private By addTeamMemberButton = By.id("addTeamMemberLink");
    private By addUserPanel = By.id("userAddPanel_container");
    private By addUserSearchInput = By.id("searchForm:searchField");
    private By addUserSearchButton = By.id("searchForm:searchBtn");
    private By personTable = By.id("resultForm:personTable");
    private By addSelectedButton = By.id("resultForm:addSelectedBtn");
    private By closeSearchButton = By.id("searchForm:closeBtn");

    public static final int USERNAME_COLUMN = 0;
    public static final int SEARCH_RESULT_PERSON_COLUMN = 0;
    public static final int IS_TRANSLATOR_COLUMN = 2;
    public static final int IS_REVIEWER_COLUMN = 3;
    public static final int IS_COORDINATOR_COLUMN = 4;

    public ManageLanguageTeamMemberPage(WebDriver driver) {
        super(driver);
    }

    private String getMembersInfo() {
        log.info("Query members info");
        return waitForWebElement(memberPanel)
                .findElement(By.xpath(".//p")).getText();
    }

    public List<String> getMemberUsernames() {
        log.info("Query username list");
        if (getMembersInfo().contains("0 members")) {
            log.info("no members yet for this language");
            return Collections.emptyList();
        }
        List<String> usernameColumn = WebElementUtil.getColumnContents(
                getDriver(),
                memberPanelRows,
                USERNAME_COLUMN);
        log.info("username column: {}", usernameColumn);
        return usernameColumn;
    }

    public ManageLanguageTeamMemberPage joinLanguageTeam() {
        log.info("Click Join");
        waitForWebElement(joinLanguageTeamButton).click();
        // we need to wait for this join to finish before returning the page
        waitForAMoment().until(new Function<WebDriver, Boolean>() {
            @Override
            public Boolean apply(WebDriver driver) {
                return driver.findElements(joinLanguageTeamButton).isEmpty();
            }
        });
        return new ManageLanguageTeamMemberPage(getDriver());
    }

    public ManageLanguageTeamMemberPage clickAddTeamMember() {
        log.info("Click Add Team Member");
        waitForWebElement(addTeamMemberButton).click();
        return this;
    }

    public ManageLanguageTeamMemberPage searchPersonAndAddToTeam(
            final String personName, TeamPermission... permissions) {
        log.info("Enter username search {}", personName);
        // Wait for the search field under the add user panel
        waitForWebElement(waitForElementExists(addUserPanel), addUserSearchInput)
                .sendKeys(personName);

        log.info("Click search button");
        waitForWebElement(addUserSearchButton).click();

        TableRow firstRow = tryGetFirstRowInSearchPersonResult(personName);

        final String personUsername = firstRow.getCellContents()
                .get(SEARCH_RESULT_PERSON_COLUMN);
        log.info("Username to be added: {}", personUsername);
        // if permissions is empty, default add as translator
        Set<TeamPermission> permissionToAdd = Sets.newHashSet(permissions);
        permissionToAdd.add(TeamPermission.Translator);

        for (final TeamPermission permission : permissionToAdd) {
            log.info("Set checked as {}", permission.name());
            waitForAMoment().until(new Predicate<WebDriver>() {
                @Override
                public boolean apply(@Nullable WebDriver webDriver) {
                    TableRow tableRow =
                            tryGetFirstRowInSearchPersonResult(personName);
                    WebElement input =
                            tableRow.getCells().get(permission.columnIndex)
                                    .findElement(By.tagName("input"));
                    Checkbox checkbox = Checkbox.of(input);
                    checkbox.check();
                    return checkbox.checked();
                }
            });
        }
        log.info("Click Add Selected");
        waitForWebElement(addSelectedButton).click();
        log.info("Click Close");
        waitForWebElement(closeSearchButton).click();
        return confirmAdded(personName);
    }

    private TableRow tryGetFirstRowInSearchPersonResult(final String personName) {
        // we want to wait until search result comes back
        return waitForAMoment().until(new Function<WebDriver, List<TableRow>>() {
            @Override
            public List<TableRow> apply(WebDriver input) {
                log.debug("waiting for search result refresh...");
                List<TableRow> tableRows = WebElementUtil
                        .getTableRows(getDriver(),
                                waitForWebElement(personTable));
                if (tableRows.isEmpty()) {
                    log.debug("waiting for search result refresh...");
                    throw new NoSuchElementException("");
                }
                if (!tableRows.get(0).getCellContents()
                        .get(SEARCH_RESULT_PERSON_COLUMN).contains(personName)) {
                    throw new NoSuchElementException("User not in pos 0");
                }
                return tableRows;
            }
        }).get(0);
    }

    private ManageLanguageTeamMemberPage confirmAdded(
            final String personUsername) {
        // we need to wait for the page to refresh
        return refreshPageUntil(this, new Predicate<WebDriver>() {
            @Override
            public boolean apply(WebDriver driver) {
                List<String> usernameColumn = WebElementUtil
                        .getColumnContents(getDriver(),
                        memberPanelRows,
                        USERNAME_COLUMN);
                log.info("username column: {}", usernameColumn);
                return usernameColumn.contains(personUsername);
            }
        });
    }

    public static enum TeamPermission {
        Translator(IS_TRANSLATOR_COLUMN), Reviewer(IS_REVIEWER_COLUMN), Coordinator(IS_COORDINATOR_COLUMN);
        private final int columnIndex;

        TeamPermission(int columnIndex) {
            this.columnIndex = columnIndex;
        }

    }
}
