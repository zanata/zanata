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

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.zanata.page.CorePage;
import org.zanata.page.utility.HomePage;

import java.util.List;
import java.util.Map;

/**
 * @author Damian Jansen <a
 *         href="mailto:djansen@redhat.com">djansen@redhat.com</a>
 */
public class RegisterPage extends CorePage {

    public static final String USERNAME_VALIDATION_ERROR =
            "Between 3 and 20 lowercase letters, numbers and underscores only";

    public static final String USERNAME_UNAVAILABLE_ERROR =
            "This username is not available";

    public static final String MALFORMED_EMAIL_ERROR =
            "not a well-formed email address";

    public static final String REQUIRED_FIELD_ERROR = "value is required";

    public static final String USERNAME_LENGTH_ERROR =
            "size must be between 3 and 20";

    @FindBy(id = "loginForm:name")
    private WebElement nameField;

    @FindBy(id = "loginForm:emailField:email")
    private WebElement emailField;

    @FindBy(id = "loginForm:usernameField:username")
    private WebElement usernameField;

    @FindBy(id = "loginForm:passwordField:password")
    private WebElement passwordField;

    @FindBy(xpath = "//input[@value='Sign Up']")
    private WebElement registerButton;

    public RegisterPage(WebDriver driver) {
        super(driver);
        List<By> elementBys = ImmutableList.<By> builder()
                .add(By.id("loginForm:name"))
                .add(By.id("loginForm:emailField:email"))
                .add(By.id("loginForm:usernameField:username"))
                .add(By.id("loginForm:passwordField:password"))
                .add(By.xpath("//input[@value='Sign Up']")).build();
        waitForPage(elementBys);
    }

    public RegisterPage enterName(String name) {
        nameField.sendKeys(name);
        return new RegisterPage(getDriver());
    }

    public RegisterPage enterUserName(String userName) {
        usernameField.sendKeys(userName);
        return new RegisterPage(getDriver());
    }

    public RegisterPage enterEmail(String email) {
        emailField.sendKeys(email);
        return new RegisterPage(getDriver());
    }

    public RegisterPage enterPassword(String password) {
        passwordField.sendKeys(password);
        return new RegisterPage(getDriver());
    }

    // TODO: Add a "signup success" page
    public HomePage register() {
        registerButton.click();
        return new HomePage(getDriver());
    }

    public RegisterPage registerFailure() {
        registerButton.click();
        return new RegisterPage(getDriver());
    }

    public RegisterPage clearFields() {
        nameField.clear();
        emailField.clear();
        usernameField.clear();
        passwordField.clear();
        return new RegisterPage(getDriver());
    }

    /*
     * Pass in a map of strings, to be entered into the registration fields.
     * Fields: name, email, username, password, confirmpassword
     */
    public RegisterPage setFields(Map<String, String> fields) {
        return clearFields()
            .enterName(fields.get("name"))
            .enterEmail(fields.get("email"))
            .enterUserName(fields.get("username"))
            .enterPassword(fields.get("password"));
    }

    public List<String> waitForErrors() {
        waitForTenSec().until(new Function<WebDriver, WebElement>() {
            @Override
            public WebElement apply(WebDriver driver) {
                return getDriver().findElement(
                        By.xpath("//span[@class='errors']"));
            }
        });
        return getErrors();
    }

    public String getPageTitle() {
        return getDriver().findElement(By.className("heading--sub"))
                .getText();
    }

    public SignInPage goToSignIn() {
        getDriver().findElement(By.linkText("Log In")).click();
        return new SignInPage(getDriver());
    }

    public RegisterPage clickPasswordShowToggle() {
        getDriver().findElement(By.className("js-form-password-toggle")).click();
        return new RegisterPage(getDriver());
    }

    public String getPassword() {
        return passwordField.getAttribute("value");
    }

    public String getPasswordFieldType() {
        return passwordField.getAttribute("type");
    }
}
