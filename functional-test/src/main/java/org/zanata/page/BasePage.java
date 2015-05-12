/*
 * Copyright 2013, Red Hat, Inc. and individual contributors as indicated by the
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
package org.zanata.page;

import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.zanata.page.account.RegisterPage;
import org.zanata.page.account.SignInPage;
import org.zanata.page.administration.AdministrationPage;
import org.zanata.page.dashboard.DashboardBasePage;
import org.zanata.page.glossary.GlossaryPage;
import org.zanata.page.groups.VersionGroupsPage;
import org.zanata.page.languages.LanguagesPage;
import org.zanata.page.projects.ProjectVersionsPage;
import org.zanata.page.projects.ProjectsPage;
import org.zanata.page.utility.HelpPage;
import org.zanata.page.utility.HomePage;
import org.zanata.util.WebElementUtil;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A Base Page is an extension of the Core Page, providing the navigation bar
 * and sidebar links common to most pages outside of the editor.
 *
 * @author Damian Jansen <a
 *         href="mailto:djansen@redhat.com">djansen@redhat.com</a>
 */
@Slf4j
public class BasePage extends CorePage {

    private final By NavMenuBy = By.id("nav-main");
    private By projectsLink = By.id("projects_link");
    private By groupsLink = By.id("version-groups_link");
    private By languagesLink = By.id("languages_link");
    private By glossaryLink = By.id("glossary_link");
    private By userAvatar = By.id("user--avatar");
    private static final By BY_SIGN_IN = By.id("signin_link");
    private static final By BY_SIGN_OUT = By.id("right_menu_sign_out_link");
    private static final By BY_DASHBOARD_LINK = By.id("dashboard");
    private static final By BY_ADMINISTRATION_LINK = By.id("administration");
    private By searchInput = By.id("projectAutocomplete-autocomplete__input");
    private By registrationLink = By.id("register_link_internal_auth");
    public BasePage(final WebDriver driver) {
        super(driver);
    }

    public DashboardBasePage goToMyDashboard() {
        log.info("Click Dashboard menu link");
        readyElement(userAvatar).click();
        clickLinkAfterAnimation(BY_DASHBOARD_LINK);
        return new DashboardBasePage(getDriver());
    }

    public ProjectsPage goToProjects() {
        log.info("Click Projects");
        clickNavMenuItem(existingElement(projectsLink));
        return new ProjectsPage(getDriver());
    }

    private void clickNavMenuItem(final WebElement menuItem) {
        scrollToTop();
        slightPause();
        if (!menuItem.isDisplayed()) {
            // screen is too small the menu become dropdown
            readyElement(existingElement(By.id("nav-main")), By.tagName("a")).click();
        }
        waitForAMoment().withMessage("displayed: " + menuItem).until(
                new Predicate<WebDriver>() {
                    @Override
                    public boolean apply(WebDriver input) {
                        return menuItem.isDisplayed();
                    }
                });
        waitForAMoment().withMessage("clickable: " + menuItem).until(
                ExpectedConditions.elementToBeClickable(menuItem));
        menuItem.click();
    }

    public VersionGroupsPage goToGroups() {
        log.info("Click Groups");
        clickNavMenuItem(existingElement(groupsLink));
        return new VersionGroupsPage(getDriver());
    }

    public LanguagesPage goToLanguages() {
        log.info("Click Languages");
        clickNavMenuItem(existingElement(languagesLink));
        return new LanguagesPage(getDriver());
    }

    public GlossaryPage goToGlossary() {
        log.info("Click Glossary");
        // Dynamically find the link, as it is not present for every user
        clickNavMenuItem(existingElement(glossaryLink));
        return new GlossaryPage(getDriver());
    }

    public AdministrationPage goToAdministration() {
        log.info("Click Administration menu link");
        clickElement(userAvatar);
        clickLinkAfterAnimation(BY_ADMINISTRATION_LINK);
        return new AdministrationPage(getDriver());
    }

    public RegisterPage goToRegistration() {
        log.info("Click Sign Up");
        Preconditions.checkArgument(!hasLoggedIn(),
                "User has logged in! You should sign out or delete cookie " +
                        "first in your test.");

        clickElement(registrationLink);
        return new RegisterPage(getDriver());
    }

    public SignInPage clickSignInLink() {
        log.info("Click Log In");
        clickElement(BY_SIGN_IN);
        return new SignInPage(getDriver());
    }

    public boolean hasLoggedIn() {
        log.info("Query user is logged in");
        List<WebElement> signInLink = getDriver().findElements(BY_SIGN_IN);
        return signInLink.size() == 0;
    }

    public String loggedInAs() {
        log.info("Query logged in user name");
        return existingElement(userAvatar).getAttribute("data-original-title");
    }

    public HomePage logout() {
        log.info("Click Log Out");
        clickElement(userAvatar);
        clickLinkAfterAnimation(BY_SIGN_OUT);
        return new HomePage(getDriver());
    }

    public List<String> getBreadcrumbLinks() {
        List<WebElement> breadcrumbs =
                getDriver().findElement(By.id("breadcrumbs_panel"))
                        .findElements(By.className("breadcrumbs_link"));
        return WebElementUtil.elementsToText(breadcrumbs);
    }

    public String getLastBreadCrumbText() {
        WebElement breadcrumb =
                getDriver().findElement(By.id("breadcrumbs_panel"))
                        .findElement(By.className("breadcrumbs_display"));
        return breadcrumb.getText();
    }

    public <P> P clickBreadcrumb(final String link, Class<P> pageClass) {
        List<WebElement> breadcrumbs =
                getDriver().findElement(By.id("breadcrumbs_panel"))
                        .findElements(By.className("breadcrumbs_link"));
        Predicate<WebElement> predicate = new Predicate<WebElement>() {
            @Override
            public boolean apply(WebElement input) {
                return input.getText().equals(link);
            }
        };
        Optional<WebElement> breadcrumbLink =
                Iterables.tryFind(breadcrumbs, predicate);
        if (breadcrumbLink.isPresent()) {
            breadcrumbLink.get().click();
            return PageFactory.initElements(getDriver(), pageClass);
        }
        throw new RuntimeException("can not find " + link + " in breadcrumb: "
                + WebElementUtil.elementsToText(breadcrumbs));
    }

    public <P> P goToPage(String navLinkText, Class<P> pageClass) {
        readyElement(existingElement(NavMenuBy),
                By.linkText(navLinkText)).click();
        return PageFactory.initElements(getDriver(), pageClass);
    }

    /**
     * This is a workaround for
     * https://code.google.com/p/selenium/issues/detail?id=2766 Elemenet not
     * clickable at point due to the change coordinate of element in page.
     *
     * @param locator
     */
    public void clickLinkAfterAnimation(By locator) {
        clickLinkAfterAnimation(existingElement(locator));
    }

    public void clickLinkAfterAnimation(WebElement element) {
        JavascriptExecutor executor = (JavascriptExecutor) getDriver();
        executor.executeScript("arguments[0].click();", element);
    }

    public HelpPage goToHelp() {
        log.info("Click Help");
        clickNavMenuItem(existingElement(By.id("help_link")));
        return new HelpPage(getDriver());
    }

    public BasePage enterSearch(String searchText) {
        log.info("Enter Project search {}", searchText);
        WebElementUtil.searchAutocomplete(getDriver(), "projectAutocomplete",
                searchText);
        return new BasePage(getDriver());
    }

    public ProjectsPage submitSearch() {
        log.info("Press Enter on Project search");
        existingElement(searchInput).sendKeys(Keys.ENTER);
        return new ProjectsPage(getDriver());
    }

    public BasePage expectSearchListContains(final String expected) {
        waitForPageSilence();
        String msg = "Project search list contains " + expected;
        waitForAMoment().withMessage("Waiting for search contains").until(
                new Predicate<WebDriver>() {
                    @Override
                    public boolean apply(WebDriver input) {
                        return getProjectSearchAutocompleteItems()
                                .contains(expected);
                    }
                }
        );
        assertThat(getProjectSearchAutocompleteItems()).as(msg).contains(
                expected);
        return new BasePage(getDriver());
    }

    public List<String> getProjectSearchAutocompleteItems() {
        log.info("Query Projects search results list");
        return WebElementUtil.getSearchAutocompleteItems(getDriver(),
                "general-search-form", "projectAutocomplete");
    }

    public ProjectVersionsPage clickSearchEntry(final String searchEntry) {
        log.info("Click Projects search result {}", searchEntry);
        String msg = "search result " + searchEntry;
        WebElement searchItem =
                waitForAMoment().withMessage(msg).until(
                        new Function<WebDriver, WebElement>() {
                            @Override
                            public WebElement apply(WebDriver driver) {
                                List<WebElement> items =
                                        WebElementUtil
                                                .getSearchAutocompleteResults(
                                                        driver,
                                                        "general-search-form",
                                                        "projectAutocomplete");

                                for (WebElement item : items) {
                                    if (item.getText().equals(searchEntry)) {
                                        return item;
                                    }
                                }
                                return null;
                            }
                        });
        searchItem.click();
        return new ProjectVersionsPage(getDriver());
    }

    public void clickWhenTabEnabled(final WebElement tab) {
        waitForPageSilence();
        clickElement(tab);
    }

    public String getHtmlSource(WebElement webElement) {
        return (String) ((JavascriptExecutor) getDriver()).executeScript(
                "return arguments[0].innerHTML;", webElement);
    }

    /**
     * Check if the page has the home button, expecting a valid base page
     * @return boolean is valid
     */
    public boolean isPageValid() {
        return (getDriver().findElements(By.id("home"))).size() > 0;
    }

    /**
     * Convenience function for clicking elements.  Removes obstructing
     * elements, scrolls the item into view and clicks it when it is ready.
     * @param findby
     */
    public void clickElement(By findby) {
        clickElement(readyElement(findby));
    }

    public void clickElement(final WebElement element) {
        removeNotifications();
        waitForNotificationsGone();
        scrollIntoView(element);
        waitForAMoment().withMessage("clickable: " + element.toString()).until(
                ExpectedConditions.elementToBeClickable(element));
        element.click();
    }

    /**
     * Remove any visible notifications
     */
    public void removeNotifications() {
        @SuppressWarnings("unchecked")
        List<WebElement> notifications = (List<WebElement>) getExecutor()
                .executeScript("return (typeof $ == 'undefined') ?  [] : " +
                        "$('a.message__remove').toArray()");
        if (notifications.isEmpty()) {
            return;
        }
        log.info("Closing {} notifications", notifications.size());
        for (WebElement notification : notifications) {
            try {
                notification.click();
            } catch (WebDriverException exc) {
                log.info("Missed a notification X click");
            }
        }
        // Finally, forcibly un-is-active the message container - for speed
        String script = "return (typeof $ == 'undefined') ?  [] : " +
                "$('ul.message--global').toArray()";
        @SuppressWarnings("unchecked")
        List<WebElement> messageBoxes = ((List<WebElement>) getExecutor()
                .executeScript(script));
        for (WebElement messageBox : messageBoxes) {
            getExecutor().executeScript(
                    "arguments[0].setAttribute('class', arguments[1]);",
                    messageBox,
                    messageBox.getAttribute("class").replace("is-active", ""));
        }
    }

    /**
     * Wait for the notifications box to go. Assumes test has dealt with
     * removing it, or is waiting for it to time out.
     */
    public void waitForNotificationsGone() {
        final String script = "return (typeof $ == 'undefined') ?  [] : " +
                "$('ul.message--global').toArray()";
        final String message = "Waiting for notifications box to go";
        waitForAMoment().withMessage(message).until(new Predicate<WebDriver>() {
            @Override
            public boolean apply(WebDriver input) {
                @SuppressWarnings("unchecked")
                List<WebElement> boxes = (List<WebElement>) getExecutor()
                        .executeScript(script);
                for (WebElement box : boxes) {
                    if (box.isDisplayed()) {
                        log.info(message);
                        return false;
                    }
                }
                return true;
            }
        });
    }

}
