/*
 * Copyright 2012, Red Hat, Inc. and individual contributors
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
package org.zanata.adapter;

import java.io.OutputStream;
import java.net.URI;
import java.util.Map;

import org.zanata.common.LocaleId;
import org.zanata.exception.FileFormatAdapterException;
import org.zanata.file.GlobalDocumentId;
import org.zanata.rest.dto.resource.Resource;
import org.zanata.rest.dto.resource.TextFlowTarget;
import org.zanata.rest.dto.resource.TranslationsResource;

import com.google.common.base.Optional;

/**
 * Common interface for classes wrapping Okapi filters. Each implementation must
 * have a public no-arg constructor.
 *
 * @author David Mason, <a
 *         href="mailto:damason@redhat.com">damason@redhat.com</a>
 *
 */
public interface FileFormatAdapter {
    /**
     * Extract source strings from the given document content.
     *
     * @param documentUri
     * @param sourceLocale
     * @param params
     *            adapter-specific parameter string. See documentation for
     *            individual adapters.
     * @return representation of the strings in the document
     * @throws IllegalArgumentException
     *             if documentUri or sourceLocale is null
     * @throws FileFormatAdapterException
     *             if the document cannot be parsed
     */
    Resource parseDocumentFile(URI documentUri, LocaleId sourceLocale,
            Optional<String> params) throws FileFormatAdapterException,
            IllegalArgumentException;

    /**
     * Extract translation strings from the given translation document.
     *
     * @param translatedDocumentContent
     *            translated document to parse
     * @param params
     *            adapter-specific parameter string. See documentation for
     *            individual adapters.
     * @return representation of the translations in the document
     * @throws FileFormatAdapterException
     *             if the document cannot be parsed
     * @throws IllegalArgumentException
     *             if translatedDocumentContent or localeId is null
     */
    TranslationsResource parseTranslationFile(URI fileUri, String localeId,
            Optional<String> params) throws FileFormatAdapterException,
            IllegalArgumentException;

    /**
     * Write translated file to the given output, using the given list of
     * translations.
     *
     * @param output
     *            stream to write translated document
     * @param original
     *            source document
     * @param translations
     *            to use in generating translated file
     * @param documentId
     *            information of the source document file
     * @param locale
     *            to use for translated document
     * @param params
     *            adapter-specific parameter string. See documentation for
     *            individual adapters.
     * @throws FileFormatAdapterException
     *             if there is any problem parsing the original file or writing
     *             the translated file
     * @throws IllegalArgumentException
     *             if any parameters are null
     */
    void writeTranslatedFile(OutputStream output, URI originalFile,
            Resource resource, TranslationsResource translationsResource,
            String locale, Optional<String> params)
            throws FileFormatAdapterException, IllegalArgumentException;

}
