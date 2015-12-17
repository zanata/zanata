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


import java.lang.annotation.Annotation;
import java.util.Optional;

import javax.enterprise.inject.Alternative;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.apache.deltaspike.core.api.exclude.Exclude;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zanata.seam.SeamAutowire;

/**
 * Replacement class for our ServiceLocator. Tests that use
 * the {@link org.zanata.seam.SeamAutowire} class will use this class instead of the real one to
 * request components.
 *
 * @author Carlos Munoz <a
 *         href="mailto:camunoz@redhat.com">camunoz@redhat.com</a>
 */
@Alternative
@Exclude
public class AutowireLocator implements IServiceLocator {
    private static final Logger log =
            LoggerFactory.getLogger(AutowireLocator.class);

    public static IServiceLocator instance() {
        if (SeamAutowire.useRealServiceLocator) {
            return ServiceLocator.INSTANCE;
        } else {
            return new AutowireLocator();
        }

    }

    public <T> BeanHolder<T> getDependent(Class<T> clazz, Annotation... qualifiers) {
        return new BeanHolder<T>(getInstance(clazz, qualifiers));
    }

    public <T> T getInstance(Class<T> clazz, Annotation... qualifiers) {
        try {
            if (!SeamAutowire.disableRealServiceLocator) {
                T bean = ServiceLocator.INSTANCE.getInstance(clazz,
                        qualifiers);
                log.debug("Returning CDI bean: {}", bean);
                return bean;
            }
        } catch (IllegalStateException e) {
            log.debug("Can't find CDI bean, trying SeamAutowire",
                    e.getMessage());
        }
        return SeamAutowire.instance().getComponent(clazz, qualifiers);
    }

    public <T> Optional<T> getOptionalInstance(Class<T> clazz, Annotation... qualifiers) {
        return Optional.ofNullable(SeamAutowire.instance().getComponent(
                clazz, qualifiers));
    }

    @Override
    public EntityManager getEntityManager() {
        return getInstance(EntityManager.class);
    }

    @Override
    public EntityManagerFactory getEntityManagerFactory() {
        return getInstance(EntityManagerFactory.class);
    }

    @Override
    public <T> T getJndiComponent(String jndiName, Class<T> clazz)
            throws NamingException {
        T instance = (T) SeamAutowire.instance().getComponent(jndiName);
        if (instance == null) {
            throw new NamingException("component with JNDI name " + jndiName +
                    " has not been registered with Autowire");
        }
        return instance;
    }

}
