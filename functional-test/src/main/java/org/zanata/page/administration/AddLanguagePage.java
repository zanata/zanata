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

import com.google.common.base.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.zanata.page.BasePage;
import org.zanata.util.WebElementUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class AddLanguagePage extends BasePage {

    private By saveButton = By.xpath("//input[@value='Save']");
    private By enabledByDefaultCheckbox = By.id("addLanguageForm:enabled");
    private By languageDetailPanel = By.id("addLanguageForm:output");
    public AddLanguagePage(final WebDriver driver) {
        super(driver);
    }

    public AddLanguagePage enterSearchLanguage(
            String languageQuery) {
        log.info("Enter language search {}", languageQuery);
        WebElementUtil.searchAutocomplete(getDriver(),
                "localeAutocomplete", languageQuery);
        return new AddLanguagePage(getDriver());
    }

    public AddLanguagePage selectSearchLanguage(final String language) {
        log.info("Select language {}", language);
        waitForAMoment().until(new Predicate<WebDriver>() {
            @Override
            public boolean apply(WebDriver driver) {
                List<WebElement> searchResults =
                        WebElementUtil.getSearchAutocompleteResults(
                                driver,
                                "addLanguageForm",
                                "localeAutocomplete");
                boolean clickedUser = false;
                for (WebElement searchResult : searchResults) {
                    if (searchResult.getText().contains(language)) {
                        searchResult.click();
                        clickedUser = true;
                        break;
                    }
                }
                return clickedUser;
            }
        });
        return new AddLanguagePage(getDriver());
    }

    public AddLanguagePage enableLanguageByDefault() {
        log.info("Click Enable by default");
        if (!waitForWebElement(enabledByDefaultCheckbox).isSelected()) {
            waitForWebElement(enabledByDefaultCheckbox).click();
        }
        return new AddLanguagePage(getDriver());
    }

    public AddLanguagePage disableLanguageByDefault() {
        log.info("Click Disable by default");
        if (waitForWebElement(enabledByDefaultCheckbox).isSelected()) {
            waitForWebElement(enabledByDefaultCheckbox).click();
        }
        return new AddLanguagePage(getDriver());
    }

    public Map<String, String> getLanguageDetails() {
        log.info("Query language details");
        Map<String, String> map = new HashMap();
        // Wait for the fields to be populated
        waitForAMoment().until(new Predicate<WebDriver>() {
            @Override
            public boolean apply(WebDriver input) {
                return !waitForWebElement(languageDetailPanel)
                        .findElement(By.className("l--push-top-half"))
                        .findElement(By.className("txt--meta"))
                        .getText()
                        .isEmpty();
            }
        });
        for (WebElement item : waitForWebElement(languageDetailPanel)
                .findElements(By.className("l--push-top-half"))) {
            String key = item.getText().trim();
            String value = item.findElement(By.className("txt--meta")).getText();
            if (value.isEmpty()) {
                map.put(key, "");
            } else {
                map.put(key.substring(0, key.lastIndexOf(value)), value);
            }
        }
        return map;
    }

    public ManageLanguagePage saveLanguage() {
        log.info("Click Save");
        clickAndCheckErrors(waitForWebElement(saveButton));
        return new ManageLanguagePage(getDriver());
    }
}
