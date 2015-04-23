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
package org.zanata.page.account;

import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.zanata.page.BasePage;
import org.zanata.page.utility.HomePage;

/**
 * @author Damian Jansen <a
 *         href="mailto:djansen@redhat.com">djansen@redhat.com</a>
 */
@Slf4j
public class ResetPasswordPage extends BasePage {

    private By usernameEmailField = By.id("passwordResetRequestForm:usernameEmailField:usernameEmail");
    private By submitButton = By.id("passwordResetRequestForm:submitRequest");

    public ResetPasswordPage(WebDriver driver) {
        super(driver);
    }

    public ResetPasswordPage enterUserNameEmail(String usernameEmail) {
        log.info("Enter username or email {}", usernameEmail);
        waitForWebElement(usernameEmailField).sendKeys(usernameEmail);

    public ResetPasswordPage clearFields() {
        log.info("Clear fields");
        waitForWebElement(usernameEmailField).clear();
        defocus(usernameEmailField);
        waitForPageSilence();
        return new ResetPasswordPage(getDriver());
    }

    public HomePage resetPassword() {
        log.info("Click Submit");
        defocus(usernameEmailField);
        waitForPageSilence();
        waitForWebElement(submitButton).click();
        return new HomePage(getDriver());
    }

    public ResetPasswordPage resetFailure() {
        log.info("Click Submit");
        defocus(usernameEmailField);
        waitForPageSilence();
        waitForWebElement(submitButton).click();
        return new ResetPasswordPage(getDriver());
    }
}
