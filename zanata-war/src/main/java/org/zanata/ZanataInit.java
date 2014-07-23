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
package org.zanata;

import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.LinkRef;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.servlet.ServletContext;

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.io.FileUtils;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.contexts.ServletLifecycle;
import org.jboss.seam.core.Events;
import org.zanata.exception.ZanataInitializationException;
import org.zanata.rest.dto.VersionInfo;
import org.zanata.util.VersionUtility;

/**
 * Doesn't do much useful stuff except printing a log message and firing the
 * "Zanata.startup" event.
 *
 * @author Christian Bauer
 */
@Name("zanataInit")
@Scope(ScopeType.STATELESS)
@Slf4j
public class ZanataInit {
    static {
        // Prevent AS 7 from warning about gwt-servlet's
        // org.hibernate.validator.ValidationMessages
        Logger.getLogger("org.jboss.modules").setLevel(Level.SEVERE);
        // Disable "RP discovery / realm validation disabled;"
        Logger.getLogger("org.openid4java.server.RealmVerifier").setLevel(
                Level.SEVERE);
    }

    public static final String EVENT_Zanata_Startup = "Zanata.startup";

    @In
    private ApplicationConfiguration applicationConfiguration;

    @Observer("org.jboss.seam.postInitialization")
    public void initZanata() throws Exception {
        ServletContext servletContext =
                ServletLifecycle.getCurrentServletContext();
        String appServerHome = servletContext.getRealPath("/");

        File manifestFile = new File(appServerHome, "META-INF/MANIFEST.MF");

        VersionInfo ver = new VersionInfo();
        Attributes atts = null;
        if (manifestFile.canRead()) {
            Manifest mf = new Manifest();
            final FileInputStream fis = new FileInputStream(manifestFile);
            try {
                mf.read(fis);
            } finally {
                fis.close();
            }
            atts = mf.getMainAttributes();
        }
        ver = VersionUtility.getVersionInfo(atts, ZanataInit.class);

        this.applicationConfiguration.setVersion(ver.getVersionNo());
        this.applicationConfiguration.setBuildTimestamp(ver.getBuildTimeStamp());
        this.applicationConfiguration.setScmDescribe(ver.getScmDescribe());

        logBanner(ver);

        if (this.applicationConfiguration.isDebug()) {
            log.info("debug: enabled");
        }
        boolean authlogged = false;

        if (applicationConfiguration.isInternalAuth()) {
            log.info("Internal authentication: enabled");
            authlogged = true;
        }
        if (applicationConfiguration.isOpenIdAuth()) {
            log.info("OpenID authentication: enabled");
            authlogged = true;
        }
        if (applicationConfiguration.isKerberosAuth()) {
            log.info("SPNEGO/Kerberos authentication: enabled");
            authlogged = true;
        }
        if (!authlogged) {
            log.info("Using JAAS authentication");
        }
        log.info("Enable copyTrans: {}",
                this.applicationConfiguration.isCopyTransEnabled());
        String javamelodyDir =
                System.getProperty("javamelody.storage-directory");
        log.info("JavaMelody stats directory: " + javamelodyDir);
        String indexBase =
                System.getProperty("hibernate.search.default.indexBase");
        log.info("Lucene index directory: " + indexBase);
        if (indexBase != null) {
            checkLuceneLocks(new File(indexBase));
        }

        // Email server information
        log.info("Notification Email Host: "
                + this.applicationConfiguration.getEmailServerHost() + ":"
                + this.applicationConfiguration.getEmailServerPort());

        Events.instance().raiseEvent(EVENT_Zanata_Startup);

        log.info("Started Zanata...");
    }

    private void checkLuceneLocks(File indexDir)
            throws ZanataInitializationException {
        if (!indexDir.exists()) {
            if (indexDir.mkdirs()) {
                log.info("Created lucene index directory.");
            } else {
                log.warn("Could not create lucene index directory");
            }
        }
        Collection<File> lockFiles =
                FileUtils.listFiles(indexDir, new String[] { "lock" }, true);
        Collection<String> lockedDirs = Lists.newArrayList();
        for (File f : lockFiles) {
            lockedDirs.add(f.getParent());
        }
        if (!lockFiles.isEmpty()) {
            log.error("Lucene lock files found. Check if Zanata is already running. Otherwise, Zanata was not shut down cleanly: delete the locked directories: "
                    + lockedDirs);
            throw new ZanataInitializationException("Found lock files: "
                    + lockFiles);
        }
    }

    /** Utility to debug JBoss JNDI problems */
    public static String listJNDITree(String namespace) {
        StringBuffer buffer = new StringBuffer(4096);
        try {
            Properties props = new Properties();
            Context context = new InitialContext(props); // From jndi.properties
            if (namespace != null) {
                context = (Context) context.lookup(namespace);
            }
            buffer.append("Namespace: ").append(namespace).append("\n");
            buffer.append("#####################################\n");
            list(context, " ", buffer, true);
            buffer.append("#####################################\n");
        } catch (NamingException e) {
            buffer.append("Failed to get InitialContext, ").append(
                    e.toString(true));
        }
        return buffer.toString();
    }

    private static void list(Context ctx, String indent, StringBuffer buffer,
            boolean verbose) {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        try {
            NamingEnumeration<NameClassPair> ne = ctx.list("");
            while (ne.hasMore()) {
                NameClassPair pair = ne.next();

                String name = pair.getName();
                String className = pair.getClassName();
                boolean recursive = false;
                boolean isLinkRef = false;
                boolean isProxy = false;
                Class<?> c = null;
                try {
                    c = loader.loadClass(className);

                    if (Context.class.isAssignableFrom(c)) {
                        recursive = true;
                    }
                    if (LinkRef.class.isAssignableFrom(c)) {
                        isLinkRef = true;
                    }

                    isProxy = Proxy.isProxyClass(c);
                } catch (ClassNotFoundException cnfe) {
                    // If this is a $Proxy* class its a proxy
                    if (className.startsWith("$Proxy")) {
                        isProxy = true;
                        // We have to get the class from the binding
                        try {
                            Object p = ctx.lookup(name);
                            c = p.getClass();
                        } catch (NamingException e) {
                            Throwable t = e.getRootCause();
                            if (t instanceof ClassNotFoundException) {
                                // Get the class name from the exception msg
                                String msg = t.getMessage();
                                if (msg != null) {
                                    // Reset the class name to the CNFE class
                                    className = msg;
                                }
                            }
                        }
                    }
                }

                buffer.append(indent).append(" +- ").append(name);

                // Display reference targets
                if (isLinkRef) {
                    // Get the
                    try {
                        Object obj = ctx.lookupLink(name);

                        LinkRef link = (LinkRef) obj;
                        buffer.append("[link -> ");
                        buffer.append(link.getLinkName());
                        buffer.append(']');
                    } catch (Throwable t) {
                        buffer.append("invalid]");
                    }
                }

                // Display proxy interfaces
                if (isProxy) {
                    buffer.append(" (proxy: ").append(pair.getClassName());
                    if (c != null) {
                        Class<?>[] ifaces = c.getInterfaces();
                        buffer.append(" implements ");
                        for (Class<?> iface : ifaces) {
                            buffer.append(iface);
                            buffer.append(',');
                        }
                        buffer.setCharAt(buffer.length() - 1, ')');
                    } else {
                        buffer.append(" implements ").append(className)
                                .append(")");
                    }
                } else if (verbose) {
                    buffer.append(" (class: ").append(pair.getClassName())
                            .append(")");
                }

                buffer.append('\n');
                if (recursive) {
                    try {
                        Object value = ctx.lookup(name);
                        if (value instanceof Context) {
                            Context subctx = (Context) value;
                            list(subctx, indent + " |  ", buffer, verbose);
                        } else {
                            buffer.append(indent).append(" |   NonContext: ")
                                    .append(value);
                            buffer.append('\n');
                        }
                    } catch (Throwable t) {
                        buffer.append("Failed to lookup: ").append(name)
                                .append(", errmsg=").append(t.getMessage());
                        buffer.append('\n');
                    }
                }
            }
            ne.close();
        } catch (NamingException ne) {
            buffer.append("error while listing context ")
                    .append(ctx.toString()).append(": ")
                    .append(ne.toString(true));
        }
    }

    private void logBanner(VersionInfo ver) {
        log.info("============================================");
        log.info("   _____                 _         ");
        log.info("  |__  /__ _ _ __   __ _| |_ __ _  ");
        log.info("    / // _' | '_ \\ / _' | __/ _' | ");
        log.info("   / /| (_| | | | | (_| | || (_| | ");
        log.info("  /____\\__,_|_| |_|\\__,_|\\__\\__,_| ");
        log.info("                                   ");
        log.info("  Version: " + ver.getVersionNo());
        log.info("  SCM: " + ver.getScmDescribe());
        log.info("  Red Hat Inc 2008-2014");
        log.info("============================================");
    }
}
