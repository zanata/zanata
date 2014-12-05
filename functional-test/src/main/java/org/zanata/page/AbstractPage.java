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
package org.zanata.page;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import lombok.extern.slf4j.Slf4j;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.openqa.selenium.*;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.pagefactory.AjaxElementLocatorFactory;
import org.openqa.selenium.support.ui.FluentWait;
import org.zanata.util.WebElementUtil;

import com.google.common.base.Function;
import com.google.common.base.Predicate;

/**
 * The base class for the page driver. Contains functionality not generally of
 * a user visible nature.
 */
@Slf4j
public class AbstractPage {
    private final WebDriver driver;
    private final FluentWait<WebDriver> ajaxWaitForSec;

    public AbstractPage(final WebDriver driver) {
        PageFactory.initElements(new AjaxElementLocatorFactory(driver, 10),
                this);
        this.driver = driver;
        ajaxWaitForSec = WebElementUtil.waitForAMoment(driver);
        waitForPageSilence();
    }

    public void reload() {
        log.info("Sys: Reload");
        getDriver().navigate().refresh();
    }

    public void deleteCookiesAndRefresh() {
        log.info("Sys: Delete cookies, reload");
        getDriver().manage().deleteAllCookies();
        Set<Cookie> cookies = getDriver().manage().getCookies();
        if (cookies.size() > 0) {
            log.warn("Failed to delete cookies: {}", cookies);
        }
        getDriver().navigate().refresh();
    }

    public WebDriver getDriver() {
        return driver;
    }

    public String getUrl() {
        return driver.getCurrentUrl();
    }

    public FluentWait<WebDriver> waitForAMoment() {
        return ajaxWaitForSec;
    }

    /**
     * Wait for all necessary elements to be available on page.
     * @param elementBys
     *            selenium search criteria for locating elements
     */
    public void waitForPage(List<By> elementBys) {
        for (final By by : elementBys) {
            waitForElementExists(by);
        }
    }

    public Alert switchToAlert() {
        return waitForAMoment().until(new Function<WebDriver, Alert>() {
            @Override
            public Alert apply(WebDriver driver) {
                try {
                    return getDriver().switchTo().alert();
                } catch (NoAlertPresentException noAlertPresent) {
                    return null;
                }
            }
        });
    }

    protected <P extends AbstractPage> P refreshPageUntil(P currentPage,
            Predicate<WebDriver> predicate) {
        waitForAMoment().until(predicate);
        PageFactory.initElements(driver, currentPage);
        return currentPage;
    }

    protected <P extends AbstractPage, T> T refreshPageUntil(P currentPage,
            Function<WebDriver, T> function) {
        T done = waitForAMoment().until(function);
        PageFactory.initElements(driver, currentPage);
        return done;
    }

    /**
     * Wait for certain condition to happen.
     *
     * For example, wait for a translation updated event gets broadcast to editor.
     *
     * @param callable a callable that returns a result
     * @param matcher a matcher that matches to expected result
     * @param <T> result type
     */
    public <T> void
            waitFor(final Callable<T> callable, final Matcher<T> matcher) {
        waitForAMoment().until(new Predicate<WebDriver>() {
            @Override
            public boolean apply(WebDriver input) {
                try {
                    T result = callable.call();
                    if (!matcher.matches(result)) {
                        matcher.describeMismatch(result,
                                new Description.NullDescription());
                    }
                    return matcher.matches(result);
                } catch (Exception e) {
                    log.warn("exception", e);
                    return false;
                }
            }
        });
    }

    /**
     * Wait for jQuery and Ajax calls to be 0
     * If either are not defined, they can be assumed to be 0.
     */
    public void waitForPageSilence() {
        // Wait for jQuery calls to be 0
        waitForAMoment().until(new Predicate<WebDriver>() {
            @Override
            public boolean apply(WebDriver input) {
                int ajaxCalls;
                int jQueryCalls;
                try {
                    jQueryCalls = Integer.parseInt(
                            ((JavascriptExecutor) getDriver())
                                    .executeScript("return jQuery.active")
                                    .toString()
                    );
                } catch (WebDriverException jCall) {
                    jQueryCalls = 0;
                }

                try {
                    ajaxCalls = Integer.parseInt(
                            ((JavascriptExecutor) getDriver())
                                    .executeScript(
                                            "return Ajax.activeRequestCount")
                                    .toString()
                    );
                } catch (WebDriverException jCall) {
                    ajaxCalls = 0;
                }
                return ajaxCalls + jQueryCalls == 0;
            }
        });
    }

    /**
     * Wait for an element to be visible, and return it
     * @param elementBy WebDriver By locator
     * @return target WebElement
     */
    public WebElement waitForWebElement(final By elementBy) {
        waitForPageSilence();
        return waitForAMoment().until(new Function<WebDriver, WebElement>() {
            @Override
            public WebElement apply(WebDriver input) {
                WebElement targetElement = waitForElementExists(elementBy);
                if (!elementIsReady(targetElement)) {
                    throw new NoSuchElementException("Waiting for element");
                }
                return targetElement;
            }
        });
    }

    /**
     * Wait for a child element to be visible, and return it
     * @param parentElement parent element of target
     * @param elementBy WebDriver By locator
     * @return target WebElement
     */
    public WebElement waitForWebElement(final WebElement parentElement,
                                        final By elementBy) {
        waitForPageSilence();
        return waitForAMoment().until(new Function<WebDriver, WebElement>() {
            @Override
            public WebElement apply(WebDriver input) {
                WebElement targetElement = waitForElementExists(parentElement,
                        elementBy);
                if (!elementIsReady(targetElement)) {
                    throw new NoSuchElementException("Waiting for element");
                }
                return targetElement;
            }
        });
    }

    /**
     * Wait for an element to exist on the page, and return it.
     * Generally used for situations where checking on the state of an element,
     * e.g isVisible, rather than clicking on it or getting its text.
     * @param elementBy WebDriver By locator
     * @return target WebElement
     */
    public WebElement waitForElementExists(final By elementBy) {
        waitForPageSilence();
        return waitForAMoment().until(new Function<WebDriver, WebElement>() {
            @Override
            public WebElement apply(WebDriver input) {
                return getDriver().findElement(elementBy);
            }
        });
    }

    /**
     * Wait for a child element to exist on the page, and return it.
     * Generally used for situations where checking on the state of an element,
     * e.g isVisible, rather than clicking on it or getting its text.
     * @param elementBy WebDriver By locator
     * @return target WebElement
     */
    public WebElement waitForElementExists(final WebElement parentElement,
                                           final By elementBy) {
        waitForPageSilence();
        return waitForAMoment().until(new Function<WebDriver, WebElement>() {
            @Override
            public WebElement apply(WebDriver input) {
                return parentElement.findElement(elementBy);
            }
        });
    }

    private boolean elementIsReady(WebElement targetElement) {
        return targetElement.isDisplayed() && targetElement.isEnabled();
    }
}
