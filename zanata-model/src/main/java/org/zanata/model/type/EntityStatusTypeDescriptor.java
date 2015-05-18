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

package org.zanata.model.type;

import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.AbstractTypeDescriptor;
import org.zanata.common.EntityStatus;

/**
 * @author Alex Eng <a href="mailto:aeng@redhat.com">aeng@redhat.com</a>
 */
public class EntityStatusTypeDescriptor extends
    AbstractTypeDescriptor<EntityStatus> {

    public static final EntityStatusTypeDescriptor INSTANCE =
        new EntityStatusTypeDescriptor();

    protected EntityStatusTypeDescriptor() {
        super(EntityStatus.class);
    }

    @Override
    public String toString(EntityStatus value) {
        return String.valueOf((value).getInitial());
    }

    @Override
    public EntityStatus fromString(String string) {
        if (string == null) {
            return null;
        } else {
            return EntityStatus.valueOf(string.charAt(0));
        }
    }

    @Override
    public <X> X unwrap(EntityStatus value, Class<X> type,
        WrapperOptions options) {
        if (value == null) {
            return null;
        }
        if (String.class.isAssignableFrom(type)) {
            return (X) String.valueOf((value).getInitial());
        }
        throw unknownUnwrap(type);
    }

    @Override
    public <X> EntityStatus wrap(X value, WrapperOptions options) {
        if (value == null) {
            return null;
        }
        if (String.class.isInstance(value)) {
            return EntityStatus.valueOf(((String) value).charAt(0));
        }
        throw unknownWrap(value.getClass());
    }
}
