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
package org.zanata.page.account;

import lombok.extern.slf4j.Slf4j;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.zanata.page.CorePage;
import org.zanata.page.dashboard.DashboardBasePage;
import org.zanata.page.googleaccount.GoogleAccountPage;

@Slf4j
public class SignInPage extends CorePage {

    public static final String LOGIN_FAILED_ERROR = "Login failed";

    @FindBy(id = "loginForm:username")
    private WebElement usernameField;

    @FindBy(id = "loginForm:password")
    private WebElement passwordField;

    @FindBy(id = "loginForm:loginButton")
    private WebElement signInButton;

    @FindBy(linkText = "Forgot your password?")
    private WebElement forgotPasswordLink;

    public SignInPage(final WebDriver driver) {
        super(driver);
    }

    public SignInPage enterUsername(String username) {
        log.info("Enter username {}", username);
        usernameField.sendKeys(username);
        return new SignInPage(getDriver());
    }

    public SignInPage enterPassword(String password) {
        log.info("Enter password {}", password);
        passwordField.sendKeys(password);
        return new SignInPage(getDriver());
    }

    public DashboardBasePage clickSignIn() {
        log.info("Click Sign In");
        signInButton.click();
        return new DashboardBasePage(getDriver());
    }

    public SignInPage clickSignInExpectError() {
        log.info("Click Sign In");
        signInButton.click();
        return new SignInPage(getDriver());
    }

    public GoogleAccountPage selectGoogleOpenID() {
        log.info("Click 'Google'");
        getDriver().findElement(By.linkText("Google")).click();
        return new GoogleAccountPage(getDriver());
    }

    public ResetPasswordPage goToResetPassword() {
        log.info("Click Forgot Password");
        forgotPasswordLink.click();
        return new ResetPasswordPage(getDriver());
    }

    public RegisterPage goToRegister() {
        log.info("Click Sign Up");
        getDriver().findElement(By.linkText("Sign Up")).click();
        return new RegisterPage(getDriver());
    }

    public String getPageTitle() {
        log.info("Query page title");
        return getDriver().findElement(By.className("heading--sub"))
                .getText();
    }
}
