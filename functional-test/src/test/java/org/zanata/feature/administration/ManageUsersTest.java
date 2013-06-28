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
package org.zanata.feature.administration;

import org.concordion.api.extension.Extensions;
import org.concordion.ext.ScreenshotExtension;
import org.concordion.ext.TimestampFormatterExtension;
import org.concordion.integration.junit4.ConcordionRunner;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.zanata.concordion.CustomResourceExtension;
import org.zanata.page.HomePage;
import org.zanata.workflow.LoginWorkFlow;
import org.zanata.util.ResetDatabaseRule;
import org.zanata.page.administration.ManageUserPage;
import org.zanata.page.administration.ManageUserAccountPage;

@RunWith(ConcordionRunner.class)
@Extensions({ScreenshotExtension.class, TimestampFormatterExtension.class, CustomResourceExtension.class})
public class ManageUsersTest
{
   @ClassRule
   public static ResetDatabaseRule resetDatabaseRule = new ResetDatabaseRule();
   private HomePage homePage;

   @Before
   public void before()
   {
      homePage = new LoginWorkFlow().signIn("admin", "admin");
   }

   public ManageUserPage goToUserAdministration()
   {
      return homePage.goToAdministration().goToManageUserPage();
   }

   public ManageUserAccountPage editUserAccount(ManageUserPage manageUserPage, String username)
   {
      return manageUserPage.editUserAccount(username);
   }

   public ManageUserPage changeUsernameAndPassword(ManageUserAccountPage manageUserAccount, String username, String password)
   {
      return manageUserAccount.clearFields().enterUsername(username).enterPassword(password).enterConfirmPassword(password).saveUser();
   }

   public boolean userListContains(ManageUserPage manageUserPage, String username)
   {
      return manageUserPage.getUserList().contains(username);
   }

}
