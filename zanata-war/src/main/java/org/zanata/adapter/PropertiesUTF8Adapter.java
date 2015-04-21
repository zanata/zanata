/*
 * Copyright 2015, Red Hat, Inc. and individual contributors as indicated by the
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

package org.zanata.adapter;

import java.io.OutputStream;
import java.net.URI;
import java.util.Map;

import org.zanata.common.LocaleId;
import org.zanata.exception.FileFormatAdapterException;
import org.zanata.rest.dto.resource.Resource;
import org.zanata.rest.dto.resource.TextFlowTarget;
import org.zanata.rest.dto.resource.TranslationsResource;

import com.google.common.base.Optional;

/**
 * Adapter for reading and write {@link org.zanata.common.DocumentType#PROPERTIES_UTF8} file
 *
 * TODO: Convert to okapi properties adapter once all client conversion is
 * migrated to server
 *
 * @author Alex Eng <a href="mailto:aeng@redhat.com">aeng@redhat.com</a>
 */
public class PropertiesUTF8Adapter extends PropertiesAdapter {
    @Override
    public Resource parseDocumentFile(URI fileUri, LocaleId sourceLocale,
            Optional<String> filterParams)
            throws FileFormatAdapterException, IllegalArgumentException {

        return parseDocumentFile(fileUri, sourceLocale, filterParams,
                UTF_8);
    }

    @Override
    public TranslationsResource parseTranslationFile(URI fileUri,
            String localeId, Optional<String> params)
            throws FileFormatAdapterException, IllegalArgumentException {

        return parseTranslationFile(fileUri, localeId, params, UTF_8);
    }

    @Override
    public void writeTranslatedFile(OutputStream output, URI originalFile,
            Map<String, TextFlowTarget> translations, String locale,
            Optional<String> params)
            throws FileFormatAdapterException, IllegalArgumentException {
        writeTranslatedFile(output, originalFile, translations, locale, params,
                UTF_8);
    }
}
