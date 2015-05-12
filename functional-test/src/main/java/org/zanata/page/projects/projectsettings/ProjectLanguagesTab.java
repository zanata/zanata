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
package org.zanata.page.projects.projectsettings;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.zanata.page.projects.ProjectBasePage;
import org.zanata.util.WebElementUtil;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * This class represents the project language settings page.
 *
 * @author Damian Jansen
 * <a href="mailto:djansen@redhat.com">djansen@redhat.com</a>
 */
@Slf4j
public class ProjectLanguagesTab extends ProjectBasePage {

    private By addNewLanguageField = By.id("languageAutocomplete-autocomplete__input");
    private By settingsLanguagesForm = By.id("settings-languages-form");

    public ProjectLanguagesTab(WebDriver driver) {
        super(driver);
    }

    /**
     * Get a list of locales enabled in this project
     *
     * @return String list of language/locale names
     */
    public List<String> getEnabledLocaleList() {
        log.info("Query enabled locales");
        return Lists.transform(getEnabledLocaleListElement(),
                new Function<WebElement, String>() {
                        @Override
                        public String apply(WebElement li) {
                            return li.getText();
                        }
                });
    }

    public ProjectLanguagesTab expectEnabledLocaleListCount(final int count) {
        waitForAMoment().until(new Predicate<WebDriver>() {
            @Override
            public boolean apply(WebDriver input) {
                return getEnabledLocaleList().size() == count;
            }
        });
        return new ProjectLanguagesTab(getDriver());
    }

    private List<WebElement> getEnabledLocaleListElement() {
        return readyElement(settingsLanguagesForm)
                .findElement(By.className("list--slat"))
                .findElements(By.className("reveal--list-item"));
    }

    public ProjectLanguagesTab expectLocaleListVisible() {
        log.info("Wait for locale list visible");
        waitForPageSilence();
        readyElement(existingElement(settingsLanguagesForm), By.className("list--slat"));
        return new ProjectLanguagesTab(getDriver());
    }

    /**
     * Enter text into the language search field
     * @param languageQuery text to search for
     * @return new language settings tab
     */
    public ProjectLanguagesTab enterSearchLanguage(String languageQuery) {
        log.info("Enter language search {}", languageQuery);
        WebElementUtil.searchAutocomplete(getDriver(), "languageAutocomplete",
                languageQuery);
        return new ProjectLanguagesTab(getDriver());
    }

    /**
     * Add a language to the languages list.
     * Assumes that the search string has already been entered.
     *
     * @param localeId language to select
     * @return new language settings, anticipating the language has been added.
     */
    public ProjectLanguagesTab addLanguage(final String localeId) {
        log.info("Click Add language on {}", localeId);
        waitForAMoment().until(new Predicate<WebDriver>() {
            @Override
            public boolean apply(WebDriver driver) {
                List<WebElement> searchResults =
                        WebElementUtil.getSearchAutocompleteResults(
                                getDriver(),
                                "settings-languages-form",
                                "languageAutocomplete");

                boolean clickedLocale = false;
                for (WebElement searchResult : searchResults) {
                    if (searchResult.getText().contains(localeId)) {
                        searchResult.click();
                        clickedLocale = true;
                        break;
                    }
                }
                return clickedLocale;
            }
        });


        refreshPageUntil(this, new Predicate<WebDriver>() {
            @Override
            public boolean apply(WebDriver driver) {
                return getEnabledLocaleList().contains(localeId);
            }
        });

        return new ProjectLanguagesTab(getDriver());
    }

    /**
     * Click the removal link for a language.
     *
     * @param localeId language to remove
     * @return new language settings tab
     */
    public ProjectLanguagesTab removeLocale(final String localeId) {
        log.info("Click Remove on {}", localeId);
        boolean removedLocale = false;
        for (WebElement localeLi : getEnabledLocaleListElement()) {
            String displayedLocaleId =
                    localeLi.findElement(By.xpath(".//span")).getText()
                            .replace("[", "").replace("]", "");
            if (displayedLocaleId.equals(localeId)) {
                localeLi.findElement(By.tagName("a")).click();
                removedLocale = true;
                break;
            }
        }
        Preconditions.checkState(removedLocale, "can not remove locale: %s",
                localeId);

        refreshPageUntil(this, new Predicate<WebDriver>() {
            @Override
            public boolean apply(WebDriver driver) {
                return !getEnabledLocaleList().contains(localeId);
            }
        });

        return new ProjectLanguagesTab(getDriver());
    }
}
