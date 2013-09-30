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
package org.zanata.feature.startNewProject;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.zanata.util.ResetDatabaseRule;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RunWith(Suite.class)
// @formatter:off
@Suite.SuiteClasses({
    StartNewProjectTest.class,
    CreateNewProjectTest.class,
    CreateVersionAndAddToProjectTest.class,
    AddLanguageTest.class,
    TranslatorJoinsLanguageTeamTest.class,
    PushPodirPluralProjectTest.class,
    DocumentListInWebTransTest.class
})
// @formatter:on
public class CreateSampleProjectTestSuite {
    @ClassRule
    public static ResetDatabaseRule resetDatabaseRule = new ResetDatabaseRule();

}
