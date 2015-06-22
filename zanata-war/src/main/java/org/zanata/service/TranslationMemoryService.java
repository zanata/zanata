/*
 * Copyright 2014, Red Hat, Inc. and individual contributors as indicated by the
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
package org.zanata.service;

import java.util.List;

import org.zanata.common.LocaleId;
import org.zanata.model.HLocale;
import org.zanata.model.HTextFlow;
import org.zanata.model.HTextFlowTarget;
import org.zanata.rest.editor.dto.suggestion.Suggestion;
import org.zanata.webtrans.shared.model.TransMemoryDetails;
import org.zanata.webtrans.shared.model.TransMemoryQuery;
import org.zanata.webtrans.shared.model.TransMemoryResultItem;

import com.google.common.base.Optional;

/**
 * @author Alex Eng <a href="mailto:aeng@redhat.com">aeng@redhat.com</a>
 */
public interface TranslationMemoryService extends TranslationFinder {

    public static final int SEARCH_MAX_RESULTS = 10;

    TransMemoryDetails getTransMemoryDetail(HLocale hLocale, HTextFlow tf);

    Optional<HTextFlowTarget> searchBestMatchTransMemory(HTextFlow textFlow,
            LocaleId targetLocaleId, LocaleId sourceLocaleId,
            boolean checkContext, boolean checkDocument, boolean checkProject);

    Optional<TransMemoryResultItem> searchBestMatchTransMemory(
            HTextFlow textFlow, LocaleId targetLocaleId,
            LocaleId sourceLocaleId, boolean checkContext,
            boolean checkDocument, boolean checkProject, int thresholdPercent);

    List<TransMemoryResultItem> searchTransMemory(LocaleId targetLocaleId,
            LocaleId sourceLocaleId, TransMemoryQuery transMemoryQuery);

    /**
     * Run the given query to generate suggestions.
     *
     * @param transMemoryQuery the query type and text to search.
     * @return a list of suggested translations for the query.
     */
    List<Suggestion> searchTransMemoryWithDetails(
            LocaleId targetLocaleId, LocaleId sourceLocaleId,
            TransMemoryQuery transMemoryQuery);
}
