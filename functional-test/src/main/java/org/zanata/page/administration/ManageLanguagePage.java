/*
 * Copyright 2010, Red Hat, Inc. and individual contributors as indicated by the
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
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.zanata.page.BasePage;
import org.zanata.util.TableRow;
import org.zanata.util.WebElementUtil;

import java.util.List;

@Slf4j
public class ManageLanguagePage extends BasePage {

    public static final int LOCALE_COLUMN = 0;
    public static final int ENABLED_COLUMN = 3;

    private By languageTable = By.id("languageForm:threads");
    private By addLanguageButton = By.linkText("Add New Language");

    public ManageLanguagePage(WebDriver driver) {
        super(driver);
    }

    public AddLanguagePage addNewLanguage() {
        waitForWebElement(addLanguageButton).click();
        return new AddLanguagePage(getDriver());
    }

    public ManageLanguageTeamMemberPage manageTeamMembersFor(
            final String localeId) {
        log.info("Click team {}", localeId);
        TableRow matchedRow = findRowByLocale(localeId);

        log.debug("for locale [{}] found table row: {}", localeId, matchedRow);
        List<WebElement> cells = matchedRow.getCells();
        int teamMemberCellIndex = cells.size() - 1;
        WebElement teamMemberCell = cells.get(teamMemberCellIndex);
        WebElement teamMemberButton =
                teamMemberCell.findElement(By
                        .xpath(".//input[@value='Team Members']"));
        teamMemberButton.click();
        return new ManageLanguageTeamMemberPage(getDriver());
    }

    private TableRow findRowByLocale(final String localeId) {
        TableRow matchedRow =
                waitForAMoment().until(new Function<WebDriver, TableRow>() {
                    @Override
                    public TableRow apply(WebDriver driver) {
                        List<TableRow> tableRows =
                                WebElementUtil.getTableRows(getDriver(),
                                        languageTable);
                        Optional<TableRow> matchedRow =
                                Iterables.tryFind(tableRows,
                                        new Predicate<TableRow>() {
                                            @Override
                                            public boolean
                                                    apply(TableRow input) {
                                                List<String> cellContents =
                                                        input.getCellContents();
                                                String localeCell =
                                                        cellContents.get(
                                                                LOCALE_COLUMN)
                                                                .trim();
                                                return localeCell
                                                        .equalsIgnoreCase(localeId);
                                            }
                                        });

                        // we keep looking for the locale until timeout
                        return matchedRow.isPresent() ? matchedRow.get() : null;
                    }
                });
        return matchedRow;
    }

    public ManageLanguagePage enableLanguageByDefault(String localeId) {
        log.info("Click to enable {}", localeId);
        TableRow matchedRow = findRowByLocale(localeId);

        WebElement enabledCell = matchedRow.getCells().get(ENABLED_COLUMN);
        WebElement enabledCheckbox =
                enabledCell.findElement(By.tagName("input"));
        if (!enabledCheckbox.isSelected()) {
            enabledCheckbox.click();
            getDriver().switchTo().alert().accept();
        }

        return new ManageLanguagePage(getDriver());
    }

    public boolean languageIsEnabled(String localeId) {
        log.info("Query is language enabled {}", localeId);
        return findRowByLocale(localeId).getCells().get(ENABLED_COLUMN)
                .findElement(By.tagName("input")).isSelected();
    }

    public List<String> getLanguageLocales() {
        log.info("Query list of languages");
        return WebElementUtil.getColumnContents(getDriver(), languageTable,
                LOCALE_COLUMN);
    }
}
