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
package org.zanata.feature.dashboard;

import lombok.extern.slf4j.Slf4j;

import org.concordion.api.extension.Extensions;
import org.concordion.ext.ScreenshotExtension;
import org.concordion.ext.TimestampFormatterExtension;
import org.concordion.integration.junit4.ConcordionRunner;
import org.junit.ClassRule;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.zanata.concordion.CustomResourceExtension;
import org.zanata.feature.ConcordionTest;
import org.zanata.page.utility.HomePage;
import org.zanata.util.ResetDatabaseRule;
import org.zanata.workflow.BasicWorkFlow;
import org.zanata.workflow.LoginWorkFlow;

@RunWith(ConcordionRunner.class)
@Extensions({ ScreenshotExtension.class, TimestampFormatterExtension.class, CustomResourceExtension.class })
@Category(ConcordionTest.class)
@Slf4j
public class DashboardTest
{
   @ClassRule
   public static ResetDatabaseRule rule = new ResetDatabaseRule(ResetDatabaseRule.Config.NoResetAfter, ResetDatabaseRule.Config.WithData);

   private HomePage homePage;

   public boolean signInAs(String username, String password)
   {
      homePage = new LoginWorkFlow().signIn(username, password);

      return homePage.hasLoggedIn();
   }

   public boolean hasMyActivitiesSection()
   {
      return homePage.containActivityListSection();
   }

   public boolean hasMaintainedProjectsSection()
   {
      return homePage.containMyMaintainedProjectsSection();
   }

   public void gotoDashboard()
   {
      new BasicWorkFlow().goToHome();
   }

   public boolean myActivitiesListNotEmpty()
   {
      return !homePage.getMyActivityList().isEmpty();
   }
   
   public int myActivitiesCount()
   {
      return homePage.getMyActivityList().size();
   }
   
   public boolean myActivitiesCountIsMoreThan(int compareTo)
   {
      return homePage.getMyActivityList().size() > compareTo;
   }

   public boolean maintainedProjectNotEmpty()
   {
      return !homePage.getMyMaintainedProject().isEmpty();
   }
   
   public void clickMoreActivity()
   {
      homePage.clickMoreActivity();
   }
}
