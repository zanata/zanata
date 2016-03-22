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
package org.zanata.config;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameClassPair;
import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

import javax.annotation.PostConstruct;
import javax.inject.Named;
import org.zanata.util.Synchronized;
import org.zanata.ServerConstants;

/**
 * Singleton configuration store implementation that is backed by JNDI properties.
 *
 * @author Carlos Munoz <a
 *         href="mailto:camunoz@redhat.com">camunoz@redhat.com</a>
 */
@Named("jndiBackedConfig")
@javax.enterprise.context.ApplicationScoped

// TODO populate HashMap in constructor; remove @Synchronized
@Synchronized(timeout = ServerConstants.DEFAULT_TIMEOUT)
public class JndiBackedConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final String KEY_AUTH_POLICY =
            "java:global/zanata/security/auth-policy-names/";
    private static final String KEY_ADMIN_USERS =
            "java:global/zanata/security/admin-users";
    private static final String KEY_DEFAULT_FROM_ADDRESS =
            "java:global/zanata/email/default-from-address";
    private static final String KEY_DOCUMENT_FILE_STORE =
            "java:global/zanata/files/document-storage-directory";
    private final Map<String, String> configurationValues =
            new HashMap<String, String>();

    private String getConfigValue(String key) {
        if (!configurationValues.containsKey(key)) {
            try {
                Context ctx = new InitialContext();
                String configVal = null;
                configVal = (String) ctx.lookup(key);
                configurationValues.put(key, configVal);
            } catch (NameNotFoundException nnfex) {
                // Name not found, just cache a null value
                configurationValues.put(key, null);
            } catch (NamingException nex) {
                throw new RuntimeException(
                        "Problem when fetching Jndi config key '" + key + "'",
                        nex);
            }
        }
        return configurationValues.get(key);
    }

    private boolean containsKey(String key) {
        // Only one way to know if it's there... and that is to go look for it
        getConfigValue(key);
        return configurationValues.containsKey(key);
    }

    /**
     * Resets the store by clearing out all values. This means that values will
     * need to be reloaded as they are requested.
     */
    @PostConstruct
    public void reset() {
        configurationValues.clear();
    }

    /**
     * Resets a single value of the configuration. This value will be reloaded
     * from the configuration store the next time it's requested.
     *
     * @param key
     *            Configuration key to reset.
     */
    public void reset(String key) {
        configurationValues.remove(key);
    }

    /**
     * Specific to Directory-based configuration, this method returns a set of
     * sub-keys available for the given "parent" key. For example, if there are
     * two Jndi properties called 'org/zanata/prop1' and 'org/zanata/prop2', and
     * the following invocation is made: <code>getSubKeys("org/zanata")</code>,
     * the return value would be an array containing "prop1" and "prop2".
     *
     * @param base
     *            The base context to look for sub keys.
     * @return An array with all the available keys in the context.
     */
    private Set<String> getSubKeys(String base) {
        try {
            Context ctx = new InitialContext();
            NamingEnumeration<NameClassPair> pairs = ctx.list(base);
            Set<String> results = new HashSet<String>();
            if(pairs != null) {
                while (pairs.hasMore()) {
                    NameClassPair pair = pairs.next();
                    results.add(pair.getName());
                }
            }

            return results;
        } catch (NamingException e) {
            throw new RuntimeException(
                    "Problem when fetching Jndi sub keys for '" + base + "'", e);
        }
    }

    /**
     * ========================================================================
     * ========================================== Specific property accessor
     * methods for configuration values
     * ==========================================
     * ========================================================================
     */
    public Set<String> getEnabledAuthenticationPolicies() {
        return getSubKeys(KEY_AUTH_POLICY);
    }

    public String getAuthPolicyName(String authType) {
        return getConfigValue(KEY_AUTH_POLICY + authType);
    }

    public String getAdminUsersList() {
        return getConfigValue(KEY_ADMIN_USERS);
    }

    public String getDefaultFromEmailAddress() {
        return getConfigValue(KEY_DEFAULT_FROM_ADDRESS);
    }

    public String getDocumentFileStorageLocation() {
        return getConfigValue(KEY_DOCUMENT_FILE_STORE);
    }
}
