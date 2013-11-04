/*
 * Copyright 2010, Red Hat, Inc. and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.zanata.action;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.security.Identity;
import org.jboss.seam.security.management.JpaIdentityStore;
import org.zanata.annotation.CachedMethodResult;
import org.zanata.annotation.CachedMethods;
import org.zanata.common.LocaleId;
import org.zanata.common.TransUnitWords;
import org.zanata.dao.ProjectIterationDAO;
import org.zanata.model.HAccount;
import org.zanata.model.HLocale;
import org.zanata.service.LocaleService;

@Name("iterationTranslationAction")
@Scope(ScopeType.PAGE)
@CachedMethods
public class IterationTranslationAction implements Serializable {
    private static final long serialVersionUID = 1L;

    @In
    LocaleService localeServiceImpl;

    @In
    Identity identity;

    @In(required = false, value = JpaIdentityStore.AUTHENTICATED_USER)
    HAccount authenticatedAccount;

    @In
    ProjectIterationDAO projectIterationDAO;

    public List<HLocale> getTranslationLocale(String projectSlug,
            String iterationSlug) {
        if (authenticatedAccount == null) {
            return Collections.emptyList();
        }
        return localeServiceImpl.getTranslation(projectSlug, iterationSlug,
                authenticatedAccount.getUsername());
    }

    @CachedMethodResult(ScopeType.PAGE)
    public TransUnitWords getWordStatsForContainer(Long iterationId,
            LocaleId localeId) {
        return projectIterationDAO.getWordStatsForContainer(iterationId,
                localeId);
    }
}
