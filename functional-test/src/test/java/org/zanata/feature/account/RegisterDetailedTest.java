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
package org.zanata.feature.account;

import org.hamcrest.Matchers;
import org.junit.*;
import org.zanata.page.HomePage;
import org.zanata.page.account.RegisterPage;
import org.zanata.util.ResetDatabaseRule;
import org.zanata.util.RFC2822;
import org.zanata.workflow.AbstractWebWorkFlow;

import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Damian Jansen <a href="mailto:djansen@redhat.com">djansen@redhat.com</a>
 */
@Slf4j
public class RegisterDetailedTest
{
   @ClassRule
   public static ResetDatabaseRule resetDatabaseRule = new ResetDatabaseRule(ResetDatabaseRule.Config.WithData, ResetDatabaseRule.Config.NoResetAfter);

   Map<String, String> fields;
   private HomePage homePage;

   @Before
   public void before()
   {
      // fields contains a set of data that can be successfully registered
      fields = new HashMap<String, String>();

      // Conflicting fields - must be set for each test function to avoid "not available" errors
      fields.put("email", "test@test.com");
      fields.put("username", "testusername");

      fields.put("name", "test");
      fields.put("password", "testpassword");
      fields.put("confirmpassword", "testpassword");
      fields.put("captcha", "555"); // TODO: Expect captcha error, fix
      homePage = new AbstractWebWorkFlow().goToHome();
   }

   @Test
   @Ignore("Captcha prevents test completion")
   public void registerSuccessful()
   {
      RegisterPage registerPage = homePage.goToRegistration();
      registerPage = registerPage.setFields(fields);
      assertThat("No errors are shown", registerPage.getErrors().size(), Matchers.equalTo(0));
      registerPage.register();
   }

   @Test
   public void usernameLengthValidation()
   {
      String errorMsg = "size must be between 3 and 20";
      fields.put("email", "length.test@test.com");
      RegisterPage registerPage = homePage.goToRegistration();

      fields.put("username", "bo");
      registerPage = registerPage.setFields(fields);
      assertThat("Size errors are shown for string too short", registerPage.getErrors(), Matchers.hasItem(errorMsg));

      fields.put("username", "testusername");
      registerPage = registerPage.setFields(fields);
      assertThat("Size errors are not shown", registerPage.getErrors(), Matchers.not(Matchers.hasItem(errorMsg)));

      fields.put("username", "12345678901234567890a");
      registerPage = registerPage.setFields(fields);
      assertThat("Size errors are shown for string too long", registerPage.getErrors(), Matchers.hasItem(errorMsg));
   }

   @Test
   public void usernameCharacterValidation()
   {
      String errorMsg = "lowercase letters and digits (regex \"^[a-z\\d_]{3,20}$\")";
      fields.put("email", "character.test@example.com");
      for (Map.Entry<String, String> entry : usernameCharacterValidationData().entrySet())
      {
         log.info("Test " + entry.getKey() + ":" + entry.getValue());
         fields.put("username", entry.getValue());
         RegisterPage registerPage = new AbstractWebWorkFlow().goToHome().goToRegistration().setFields(fields);
         assertThat("Validation errors are shown", registerPage.getErrors(), Matchers.hasItem(errorMsg));
      }
   }

   @Test
   @Ignore("Captcha prevents test completion")
   public void usernamePreExisting()
   {
      String errorMsg = "This username is not available";
      fields.put("email", "exists.test@test.com");
      fields.put("username", "alreadyexists");
      RegisterPage registerPage = new AbstractWebWorkFlow().goToHome().goToRegistration().setFields(fields);
      registerPage.register();

      fields.put("email", "exists2.test@test.com");
      registerPage = new AbstractWebWorkFlow().goToHome().goToRegistration().setFields(fields);
      assertThat("Username not available message is shown", registerPage.getErrors(), Matchers.hasItem(errorMsg));
   }

   @Test
   public void emailValidation()
   {
      String errorMsg = "not a well-formed email address";
      fields.put("username", "emailvalidation");
      for (Map.Entry<String, String> entry : RFC2822.invalidEmailAddresses().entrySet())
      {
         log.info("Test " + entry.getKey() + ":" + entry.getValue());
         fields.put("email", entry.getValue());
         RegisterPage registerPage = new AbstractWebWorkFlow().goToHome().goToRegistration().setFields(fields);
         assertThat("Email validation errors are shown", registerPage.getErrors(), Matchers.hasItem(errorMsg));
      }
   }

   @Test
   public void validEmailAcceptance()
   {
      String errorMsg = "not a well-formed email address";
      fields.put("username", "emailvalidation");
      for (Map.Entry<String, String> entry : RFC2822.validEmailAddresses().entrySet())
      {
         log.info("Test " + entry.getKey() + ":" + entry.getValue());
         fields.put("email", entry.getValue());
         RegisterPage registerPage = new AbstractWebWorkFlow().goToHome().goToRegistration().setFields(fields);
         assertThat("Email validation errors are not shown", registerPage.getErrors(),
               Matchers.not(Matchers.hasItem(errorMsg)));
      }
   }

   @Test
   public void rejectIncorrectCaptcha()
   {
      String errorMsg = "incorrect response";
      fields.put("username", "rejectbadcaptcha");
      fields.put("email", "rejectbadcaptcha@example.com");
      fields.put("captcha", "9000");

      RegisterPage registerPage = new AbstractWebWorkFlow().goToHome().goToRegistration().setFields(fields).registerFailure();
      assertThat("The Captcha entry is rejected", registerPage.getErrors(), Matchers.contains(errorMsg));
   }

   @Test
   public void passwordsMatch()
   {
      String errorMsg = "Passwords do not match";
      fields.put("username", "passwordsmatch");
      fields.put("email", "passwordsmatchtest@example.com");
      fields.put("password", "passwordsmatch");
      fields.put("confirmpassword", "passwordsdonotmatch");

      RegisterPage registerPage = new AbstractWebWorkFlow().goToHome().goToRegistration().setFields(fields);
      assertThat("Passwords fail to match error is shown", registerPage.getErrors(), Matchers.contains(errorMsg));
   }

   @Test
   public void requiredFields()
   {
      String errorMsg = "value is required";
      fields.put("name", "");
      fields.put("username", "");
      fields.put("email", "");
      fields.put("password", "");
      fields.put("confirmpassword", "");

      RegisterPage registerPage = new AbstractWebWorkFlow().goToHome().goToRegistration().setFields(fields);
      assertThat("Value is required shows for all fields", registerPage.getErrors(),
            Matchers.contains(errorMsg, errorMsg, errorMsg, errorMsg, errorMsg));
   }

   /*
      Bugs
    */
   @Test(expected = AssertionError.class)
   public void bug981082_inaccurateErrorMessage()
   {
      String errorMsg = "size must be between 3 and 20";
      fields.put("email", "bug981082test@test.com");
      fields.put("username", "mo");

      RegisterPage registerPage = new AbstractWebWorkFlow().goToHome().goToRegistration().setFields(fields);
      assertThat("Size errors are shown for string too short", registerPage.getErrors(), Matchers.hasItem(errorMsg));

      fields.put("username", "johndoeusernamevalidation");
      registerPage = registerPage.setFields(fields);
      assertThat("Size errors are shown for string too long", registerPage.getErrors(), Matchers.hasItem(errorMsg));
   }

   @Test(expected = AssertionError.class)
   public void bug981498_underscoreRules()
   {
      String errorMsg = "lowercase letters and digits (regex \"^[a-z\\d_]{3,20}$\")";
      fields.put("email", "bug981498test@example.com");
      fields.put("username", "______");
      RegisterPage registerPage = new AbstractWebWorkFlow().goToHome().goToRegistration().setFields(fields);
      assertThat("A username of all underscores is not valid", registerPage.getErrors(), Matchers.hasItem(errorMsg));
   }

   /*
      Returns a hash of invalid characters for a username
      Hash is String reason for invalid 'character', String character
    */
   private LinkedHashMap<String, String> usernameCharacterValidationData()
   {
      LinkedHashMap<String, String> inputData = new LinkedHashMap<String, String>(100);
      inputData.put("Invalid char |", "user|name");
      inputData.put("Invalid char /", "user/name");
      inputData.put("Invalid char ", "user\\name");
      inputData.put("Invalid char +", "user+name");
      inputData.put("Invalid char *", "user*name");
      inputData.put("Invalid char |", "user|name");
      inputData.put("Invalid char (", "user(name");
      inputData.put("Invalid char )", "user)name");
      inputData.put("Invalid char $", "user$name");
      inputData.put("Invalid char [", "user[name");
      inputData.put("Invalid char ]", "user]name");
      inputData.put("Invalid char :", "user:name");
      inputData.put("Invalid char ;", "user;name");
      inputData.put("Invalid char '", "user'name");
      inputData.put("Invalid char ,", "user,name");
      inputData.put("Invalid char ?", "user?name");
      inputData.put("Invalid char !", "user!name");
      inputData.put("Invalid char @", "user@name");
      inputData.put("Invalid char #", "user#name");
      inputData.put("Invalid char %", "user%name");
      inputData.put("Invalid char ^", "user^name");
      inputData.put("Invalid char =", "user=name");
      inputData.put("Invalid char .", "user.name");
      inputData.put("Invalid char {", "user{name");
      inputData.put("Invalid char }", "user}name");
      // Capital letters are prohibited
      for (char c = 'A'; c <= 'Z'; c++) {
         String letter = String.valueOf(c);
         inputData.put("Invalid capital char ".concat(letter), "user".concat(letter).concat("name"));
      }
      return inputData;
   }
}
