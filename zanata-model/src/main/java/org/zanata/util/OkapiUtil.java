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
package org.zanata.util;

import java.io.ByteArrayInputStream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.okapi.common.LocaleId;
import net.sf.okapi.steps.tokenization.Tokenizer;
import net.sf.okapi.steps.tokenization.tokens.Tokens;

public class OkapiUtil {
    private static final Logger log = LoggerFactory.getLogger(OkapiUtil.class);

    private OkapiUtil() {
    }

    @SuppressWarnings("null")
    public static @Nonnull
    LocaleId toOkapiLocale(@Nonnull org.zanata.common.LocaleId zanataLocale) {
        return LocaleId.fromBCP47(zanataLocale.getId());
    }

    @SuppressWarnings("null")
    public static @Nonnull
    LocaleId toOkapiLocaleOrEmpty(@Nullable org.zanata.common.LocaleId locale) {
        if (locale == null) {
            // TMXWriter demands a non-null target locale, but if you write
            // your TUs with writeTUFull(), it is never actually used.
            return LocaleId.EMPTY;
        }
        return toOkapiLocale(locale);
    }

    /**
     * Count words using Okapi's WordCounter, which tries to implement the LISA
     * standard <a href=
     * "http://web.archive.org/web/20090403134742/http://www.lisa.org/Global-information-m.105.0.html"
     * >GMX-V</a>
     *
     * @param s
     * @param bcp47Locale
     * @return
     */
    public static long countWords(String s, String bcp47Locale) {
        if (s == null) {
            log.debug("null string");
            return 0;
        }
        try {
            LocaleId locale;
            try {
                locale = LocaleId.fromBCP47(bcp47Locale);
            } catch (Exception e) {
                log.error(
                        "can't understand '{}' as a BCP-47 locale; defaulting to English",
                        bcp47Locale);
                locale = LocaleId.ENGLISH;
            }

            Tokens tokens = StringTokenizer.tokenizeString(s, locale, "WORD");
            return tokens.size();
        } catch (Exception e) {
            Object[] args = new Object[] { s, bcp47Locale, e };
            log.error("unable to count words in string '{}' for locale '{}'",
                    args);
            return 0;
        }
    }

    /**
     * Extracts plain text from a TMX entry. This ignores the TMX elements that
     * mark up native code sequences: {@code
     * <bpt></bpt>
     * <ept></ept>
     * <it></it>
     * <ph></ph>
     * <seg></seg>}
     *
     * @param content
     *            The tmx marked up content.
     * @return A string with all tmx mark-up content stripped out. Essentially a
     *         plain text version of the string.
     */
    public static String removeFormattingMarkup(String content) {
        // The content must be a fully formed <seg> element with optional
        // attributes and with no leading/trailing whitespace
        assert content.startsWith("<seg") && content.endsWith("</seg>");

        try {
            XMLInputFactory inputFactory = XMLInputFactory.newFactory();
            inputFactory.setProperty(XMLInputFactory.IS_VALIDATING, false);
            inputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
            XMLEventReader reader =
                    inputFactory.createXMLEventReader(new ByteArrayInputStream(
                            content.getBytes()));
            StringBuilder writer = new StringBuilder();

            // Nesting level. When this is > 0 it means we are ignoring events
            int ignoreLevel = 0;

            while (reader.hasNext()) {
                XMLEvent nextEv = reader.nextEvent();

                switch (nextEv.getEventType()) {
                case XMLStreamConstants.START_ELEMENT:
                    ignoreLevel =
                            handleStartElem(ignoreLevel,
                                    nextEv.asStartElement());
                    break;
                case XMLStreamConstants.END_ELEMENT:
                    ignoreLevel =
                            handleEndElem(ignoreLevel, nextEv.asEndElement());
                    break;
                case XMLStreamConstants.CHARACTERS:
                    if (ignoreLevel == 0)
                        writer.append(nextEv.asCharacters().getData());
                    break;
                }
            }

            return writer.toString();
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }
    }

    private static int handleStartElem(int ignoreLevel, StartElement startElem) {
        String elemName = startElem.getName().getLocalPart();
        if (ignoreElement(elemName)) {
            return ignoreLevel + 1;
        }
        return ignoreLevel;
    }

    private static int handleEndElem(int ignoreLevel, EndElement endElem) {
        String elemName = endElem.getName().getLocalPart();

        if (ignoreElement(elemName)) {
            if (ignoreLevel > 0) {
                return ignoreLevel - 1;
            }
        }
        return ignoreLevel;
    }

    private static boolean ignoreElement(String elemName) {
        // NB we do want the contents of 'hi' elements, but not these elements:
        return elemName.equals("bpt") || elemName.equals("ept")
                || elemName.equals("it") || elemName.equals("ph")
                || elemName.equals("sub");
    }

    private static class StringTokenizer extends Tokenizer {
        public static Tokens tokenizeString(String text, LocaleId language,
                String... tokenNames) {
            synchronized (Tokenizer.class) {
                return Tokenizer.tokenizeString(text, language, tokenNames);
            }
        }
    }

}
