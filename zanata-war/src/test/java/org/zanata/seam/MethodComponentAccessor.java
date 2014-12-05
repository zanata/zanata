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
package org.zanata.seam;

import org.jboss.seam.annotations.In;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Property accessor that relies on getter / setter methods.
 */
class MethodComponentAccessor extends ComponentAccessor {
    private String fieldName;
    private Method setter;
    private Method getter;

    MethodComponentAccessor(Method method) {
        this.initialize(method);
    }

    private void initialize(Method method) {
        String methodPrefix = null;
        Class[] parameterTypes = new Class[] {};
        // getter
        if (method.getName().startsWith("get")) {
            getter = method;
            methodPrefix = "set";
            parameterTypes = new Class[] { method.getReturnType() };
        } else if (method.getName().startsWith("set")) {
            // setter
            setter = method;
            methodPrefix = "get";
        } else {
            throw new RuntimeException(
                    "Property Accessor methods must be either getters or setters");
        }

        // Get the other method
        String methodName = methodPrefix + method.getName().substring(3);
        Method inverseMethod = null;
        try {
            inverseMethod =
                    method.getDeclaringClass().getMethod(methodName,
                            parameterTypes);
        } catch (NoSuchMethodException e) {
            inverseMethod = null;
        }

        if (getter == null) {
            getter = inverseMethod;
        } else if (setter == null) {
            setter = inverseMethod;
        }
    }

    @Override
    public Object getValue(Object instance) {
        if (getter == null) {
            throw new RuntimeException("No getter for field " + fieldName
                    + " found");
        }

        try {
            return getter.invoke(instance);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Error accessing method "
                    + getter.getName() + " on instance of type "
                    + instance.getClass().getName(), e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException("Error accessing method "
                    + getter.getName() + " on instance of type "
                    + instance.getClass().getName(), e);
        }
    }

    @Override
    public void setValue(Object instance, Object value) {
        if (setter == null) {
            throw new RuntimeException("No setter for field " + fieldName
                    + " found");
        }

        try {
            setter.invoke(instance, value);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Error accessing method "
                    + setter.getName() + " on instance of type "
                    + instance.getClass().getName(), e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException("Error accessing method "
                    + setter.getName() + " on instance of type "
                    + instance.getClass().getName(), e);
        }
    }

    @Override
    public <T extends Annotation> T getAnnotation(Class<T> annotation) {
        T ann = null;

        if (getter != null) {
            ann = getter.getAnnotation(annotation);
        }
        if (ann == null && setter != null) {
            ann = setter.getAnnotation(annotation);
        }

        return ann;
    }

    @Override
    public String getComponentName() {
        In inAnnot = this.getAnnotation(In.class);
        String compName = null;

        if (inAnnot != null) {
            if (inAnnot.value().trim().isEmpty()) {
                if (getter != null) {
                    compName = getter.getName().substring(3);
                } else if (setter != null) {
                    compName = setter.getName().substring(3);
                }
                compName =
                        compName.substring(0, 1).toLowerCase()
                                + compName.substring(1);
            } else {
                return inAnnot.value();
            }
        }
        return compName;
    }

    @Override
    public Class getComponentType() {
        if (getter != null) {
            return getter.getReturnType();
        } else if (setter != null) {
            return setter.getParameterTypes()[0];
        }

        return null; // should not happen
    }
}
