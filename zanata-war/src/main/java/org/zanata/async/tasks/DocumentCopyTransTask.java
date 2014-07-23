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
package org.zanata.async.tasks;

import java.util.List;

import org.zanata.dao.TextFlowDAO;
import org.zanata.model.HCopyTransOptions;
import org.zanata.model.HDocument;
import org.zanata.model.HLocale;
import org.zanata.service.CopyTransService;
import org.zanata.service.LocaleService;
import org.zanata.service.impl.CopyTransServiceImpl;
import org.zanata.service.impl.LocaleServiceImpl;
import org.zanata.util.ServiceLocator;

/**
 * Copy Trans task that runs copy trans on a single document and all languages
 * available to its iteration.
 *
 * @author Carlos Munoz <a
 *         href="mailto:camunoz@redhat.com">camunoz@redhat.com</a>
 */
public class DocumentCopyTransTask extends CopyTransTask {
    private HDocument document;

    public DocumentCopyTransTask(HDocument document,
            HCopyTransOptions copyTransOptions) {
        super(copyTransOptions, "DocumentCopyTransTask: " + document.getDocId());
        this.document = document;
    }

    @Override
    protected int getMaxProgress() {
        LocaleService localeService =
                ServiceLocator.instance().getInstance(LocaleServiceImpl.class);

        List<HLocale> localeList =
                localeService.getSupportedLanguageByProjectIteration(document
                        .getProjectIteration().getProject().getSlug(), document
                        .getProjectIteration().getSlug());
        int localeCount = localeList.size();

        int textFlowCount = ServiceLocator.instance().getInstance(
                TextFlowDAO.class)
                .countActiveTextFlowsInDocument(
                        document.getId());
        return localeCount * textFlowCount;
    }

    @Override
    protected void callCopyTrans() {
        CopyTransService copyTransServiceImpl =
                ServiceLocator.instance().getInstance(
                        CopyTransServiceImpl.class);
        copyTransServiceImpl.copyTransForDocument(document, copyTransOptions);
    }
}
