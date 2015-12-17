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
package org.zanata.config;

import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Property Store that delegates to system properties.
 *
 * @author Carlos Munoz <a
 *         href="mailto:camunoz@redhat.com">camunoz@redhat.com</a>
 */
@Named("systemPropertyConfigStore")
@javax.enterprise.context.Dependent
public class SystemPropertyConfigStore implements ConfigStore {
    private static final Logger log =
            LoggerFactory.getLogger(SystemPropertyConfigStore.class);

    @Override
    public String get(String propertyName) {
        return System.getProperty(propertyName);
    }

    @Override
    public int get(String propertyName, int defaultValue) {
        String value = get(propertyName);
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException e) {
            log.warn(
                    "Invalid system property value [{}] is given to {}. Fall back to default {}",
                    value, propertyName, defaultValue);
            return defaultValue;
        }
    }
}
