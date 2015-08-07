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


import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang.ArrayUtils;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import com.google.common.base.Preconditions;

/**
 * Helps with Auto-wiring of Seam components for integrated tests without the
 * need for a full Seam environment. It's a singleton class that upon first use
 * will change the way Seam's {@link org.jboss.seam.Component} class works by
 * returning its own auto-wired components.
 *
 * Supports components injected using: {@link In} {@link Logger}
 * {@link org.jboss.seam.Component#getInstance(String)} and similar methods...
 * and that have no-arg constructors
 *
 * @author Carlos Munoz <a
 *         href="mailto:camunoz@redhat.com">camunoz@redhat.com</a>
 */
@Slf4j
public class SeamAutowire {

    private static final Object PLACEHOLDER = new Object();

    private static SeamAutowire instance;

    private Map<String, Object> namedComponents = new HashMap<String, Object>();

    private Map<Class<?>, Class<?>> componentImpls =
            new HashMap<Class<?>, Class<?>>();

    private boolean ignoreNonResolvable;

    private boolean allowCycles;

    static {
        rewireSeamComponentClass();
        rewireSeamTransactionClass();
        rewireSeamContextsClass();
    }

    protected SeamAutowire() {
    }

    public SeamAutowire allowCycles() {
        allowCycles = true;
        return this;
    }

    /**
     * Initializes and returns the SeamAutowire instance.
     *
     * @return The Singleton instance of the SeamAutowire class.
     */
    public static final SeamAutowire instance() {
        if (instance == null) {
            instance = new SeamAutowire();
        }
        return instance;
    }

    /**
     * Clears out any components and returns to it's initial value.
     */
    public SeamAutowire reset() {
        // TODO create a new instance instead, to be sure of clearing all state
        ignoreNonResolvable = false;
        namedComponents.clear();
        componentImpls.clear();
        allowCycles = false;
        AutowireContexts.simulateSessionContext(false);
        return this;
    }

    /**
     * Indicates if the presence of a session context will be simulated.
     * By default contexts are not simulated.
     */
    public SeamAutowire simulateSessionContext(boolean simulate) {
        AutowireContexts.simulateSessionContext(simulate);
        return this;
    }

    /**
     * Indicates if the presence of an event context will be simulated.
     * By default contexts are not simulated.
     */
    public SeamAutowire simulateEventContext(boolean simulate) {
        AutowireContexts.simulateEventContext(simulate);
        return this;
    }

    /**
     * Indicates a specific instance of a component to use.
     *
     * @param name
     *            The name of the component. When another component injects
     *            using <code>@In(value = "name")</code> or
     *            <code>@In varName</code>, the provided component will be used.
     * @param component
     *            The component instance to use under the provided name.
     */
    public SeamAutowire use(String name, Object component) {
        if (namedComponents.containsKey(name)) {
            throw new RuntimeException("Component "+name+" was already created.  You should register it before it is resolved.");
        }
        namedComponents.put(name, component);
        return this;
    }

    /**
     * Registers an implementation to use for components. This method is
     * provided for components which are injected by interface rather than name.
     *
     * @param cls
     *            The class to register.
     */
    public SeamAutowire useImpl(Class<?> cls) {
        if (cls.isInterface()) {
            throw new RuntimeException("Class " + cls.getName()
                    + " is an interface.");
        }
        this.registerInterfaces(cls);

        return this;
    }

    /**
     * Indicates that a warning should be logged if for some reason a component
     * cannot be resolved. Otherwise, an exception will be thrown.
     */
    public SeamAutowire ignoreNonResolvable() {
        this.ignoreNonResolvable = true;
        return this;
    }

    /**
     * Returns a component by name.
     *
     * @param name
     *            Component's name.
     * @return The component registered under the provided name, or null if such
     *         a component has not been auto wired or cannot be resolved
     *         otherwise.
     */
    public Object getComponent(String name) {
        return namedComponents.get(name);
    }

    /**
     * Creates (but does not autowire) the component instance for the provided
     * class.
     *
     * @param componentClass
     *            The component class to create - may be an interface if useImpl
     *            was called, otherwise must have a no-arg constructor per Seam
     *            spec.
     * @return The component.
     */
    private <T> T create(Class<T> fieldClass) {
        // field might be an interface, but we need to find the
        // implementation class
        Class<T> componentClass = getImplClass(fieldClass);

        try {
            Constructor<T> constructor =
                    componentClass.getDeclaredConstructor(); // No-arg
                                                             // constructor
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (NoSuchMethodException e) {
            // The component class might be an interface
            if (componentClass.isInterface()) {
                throw new RuntimeException(
                        ""
                                + "Could not auto-wire component of type "
                                + componentClass.getName()
                                + ". Component is defined as an interface, but no implementations have been defined for it.",
                        e);
            } else {
                throw new RuntimeException(""
                        + "Could not auto-wire component of type "
                        + componentClass.getName()
                        + ". No no-args constructor.", e);
            }
        } catch (InvocationTargetException e) {
            throw new RuntimeException(""
                    + "Could not auto-wire component of type "
                    + componentClass.getName()
                    + ". Exception thrown from constructor.", e);
        } catch (Exception e) {
            throw new RuntimeException("Could not auto-wire component of type "
                    + componentClass.getName(), e);
        }
    }

    private <T> Class<T> getImplClass(Class<T> fieldClass) {
        // If the component type is an interface, try to find a declared
        // implementation
        // TODO field class might be abstract, or a concrete superclass
        // of the impl class
        if (fieldClass.isInterface()
                && this.componentImpls.containsKey(fieldClass)) {
            fieldClass = (Class<T>) this.componentImpls.get(fieldClass);
        }
        return fieldClass;
    }

    /**
     * Autowires and returns the component instance for the provided class.
     *
     * @param componentClass
     *            The component class to create - may be an interface if useImpl
     *            was called, otherwise must have a no-arg constructor per Seam
     *            spec.
     * @return The autowired component.
     */
    public <T> T autowire(Class<T> componentClass) {
        Predicate<Object> predicate = Predicates.instanceOf(componentClass);
        Optional<Object> namedOptional = Iterables.tryFind(
                namedComponents.values(), predicate);
        if (namedOptional.isPresent()) {
            return (T) namedOptional.get();
        }
        Optional<Class<?>> implOptional =
                Iterables.tryFind(componentImpls.values(), predicate);
        if (implOptional.isPresent()) {
            return (T) implOptional.get();
        }
        return autowire(create(componentClass));
    }

    /**
     * Autowires a component instance. The provided instance of the component
     * will be autowired instead of creating a new one.
     *
     * @param component
     *            The component instance to autowire.
     * @param <T>
     * @return Returns component.
     */
    public <T> T autowire(T component) {
        Class<T> componentClass = (Class<T>) component.getClass();

        // Register all interfaces for this class
        this.registerInterfaces(componentClass);
        // Resolve injected Components
        for (ComponentAccessor accessor : getAllComponentAccessors(component)) {
            // Another annotated component
            In inAnnotation = accessor.getAnnotation(In.class);
            if (inAnnotation != null) {
                Object fieldVal = null;
                String compName = accessor.getComponentName();
                Class<?> compType = accessor.getComponentType();
                Class<?> implType = getImplClass(compType);

                // TODO stateless components should not / need not be cached
                // autowire the component if not done yet
                if (!namedComponents.containsKey(compName)) {
                    boolean required = inAnnotation.required();
                    boolean autoCreate = implType.isAnnotationPresent(AutoCreate.class);
                    Scope scopeAnn = implType.getAnnotation(Scope.class);
                    boolean stateless = false;
                    if (scopeAnn != null) {
                        stateless = scopeAnn.value() == ScopeType.STATELESS;
                    }
                    boolean mayCreate = inAnnotation.create() || autoCreate || stateless;
                    if (required && !mayCreate) {
                        String msg = "Not allowed to create required component "+compName+" with impl "+implType+". Try @AutoCreate or @In(create=true).";
                        if (ignoreNonResolvable) {
                            log.warn(msg);
                        } else {
                            throw new RuntimeException(msg);
                        }
                    }
                    Object newComponent = null;
                    try {
                        newComponent = create(compType);
                    } catch (RuntimeException e) {
                        if (ignoreNonResolvable) {
                            log.warn("Could not build component of type: "
                                    + compType + ".", e);
                        } else {
                            throw e;
                        }
                    }

                    if (allowCycles) {
                        namedComponents.put(compName, newComponent);
                    } else {
                        // to detect mutual injection
                        namedComponents.put(compName, PLACEHOLDER);
                    }

                    try {
                        autowire(newComponent);
                    } catch (RuntimeException e) {
                        if (ignoreNonResolvable) {
                            log.warn("Could not autowire component of type: "
                                    + compType + ".", e);
                        } else {
                            throw e;
                        }
                    }

                    if (!allowCycles) {
                        // replace placeholder with the injected object
                        namedComponents.put(compName, newComponent);
                    }
                }

                fieldVal = namedComponents.get(compName);
                if (fieldVal == PLACEHOLDER) {
                    throw new RuntimeException(
                            "Recursive dependency: unable to inject "
                                    + compName + " into component of type "
                                    + component.getClass().getName());
                }
                try {
                    accessor.setValue(component, fieldVal);
                } catch (RuntimeException e) {
                    if (ignoreNonResolvable) {
                        log.warn("Could not set autowire field "
                                + accessor.getComponentName()
                                + " on component of type "
                                + component.getClass().getName()
                                + " to value of type "
                                + fieldVal.getClass().getName());
                    } else {
                        throw e;
                    }
                }
            } else if (accessor.getAnnotation(Logger.class) != null) {
                // Logs
                throw new RuntimeException("Please use Slf4j, not Seam Logger");
            }
        }

        // call post constructor
        invokePostConstructMethod(component);

        return component;
    }

    private static void rewireSeamContextsClass() {
        try {
            ClassPool pool = ClassPool.getDefault();
            CtClass contextsCls = pool.get("org.jboss.seam.contexts.Contexts");

            // Replace Component's method bodies with the ones in
            // AutowireComponent
            contextsCls.getDeclaredMethod("isSessionContextActive")
                    .setBody("{ return org.zanata.seam.AutowireContexts.isSessionContextActive(); }");

            contextsCls.getDeclaredMethod("isEventContextActive")
                    .setBody("{ return org.zanata.seam.AutowireContexts.isEventContextActive(); }");

            contextsCls.getDeclaredMethod("getEventContext")
                    .setBody("{ return org.zanata.seam.AutowireContexts.getInstance().getEventContext(); }");

            contextsCls.getDeclaredMethod("getSessionContext")
                    .setBody("{ return org.zanata.seam.AutowireContexts.getInstance().getSessionContext(); }");

            contextsCls.toClass();
        } catch (NotFoundException e) {
            throw new RuntimeException(
                    "Problem rewiring Seam's Contexts class", e);
        } catch (CannotCompileException e) {
            throw new RuntimeException(
                    "Problem rewiring Seam's Contexts class", e);
        }

    }

    private static void rewireSeamComponentClass() {
        try {
            ClassPool pool = ClassPool.getDefault();
            CtClass componentCls = pool.get("org.jboss.seam.Component");

            // Commonly used CtClasses
            final CtClass stringCls = pool.get("java.lang.String");
            final CtClass booleanCls = pool.get("boolean");
            final CtClass objectCls = pool.get("java.lang.Object");
            final CtClass scopeTypeCls = pool.get("org.jboss.seam.ScopeType");
            final CtClass classCls = pool.get("java.lang.Class");

            // Replace Component's method bodies with the ones in
            // AutowireComponent
            replaceGetInstance(pool, componentCls, stringCls, booleanCls,
                    booleanCls);
            replaceGetInstance(pool, componentCls, stringCls, scopeTypeCls,
                    booleanCls, booleanCls);
            replaceGetInstance(pool, componentCls, classCls);
            replaceGetInstance(pool, componentCls, classCls, scopeTypeCls);
            try {
                // Seam 2.2.2
                replaceGetInstance(pool, componentCls, stringCls, booleanCls,
                        booleanCls, objectCls, scopeTypeCls);
            } catch (NotFoundException e) {
                // Seam 2.2.0
                replaceGetInstance(pool, componentCls, stringCls, booleanCls,
                        booleanCls, objectCls);
            }

            componentCls.toClass();
        } catch (NotFoundException e) {
            throw new RuntimeException(
                    "Problem rewiring Seam's Component class", e);
        } catch (CannotCompileException e) {
            throw new RuntimeException(
                    "Problem rewiring Seam's Component class", e);
        }

    }

    /**
     * Replaces Component.getInstance(params) method body with that of
     * AutowireComponent.getInstance(params).
     *
     * @param pool
     *            Class pool to get class instances.
     * @param componentCls
     *            Class that represents the jboss Component class.
     * @param params
     *            Parameters for the getComponent method that will be replaced
     * @throws NotFoundException
     * @throws CannotCompileException
     */
    private static void replaceGetInstance(ClassPool pool,
            CtClass componentCls, CtClass... params) throws NotFoundException,
            CannotCompileException {
        CtMethod methodToReplace =
                componentCls.getDeclaredMethod("getInstance", params);
        methodToReplace.setBody(pool.get(AutowireComponent.class.getName())
                .getDeclaredMethod("getInstance", params), null);
    }

    private static void rewireSeamTransactionClass() {
        try {
            ClassPool pool = ClassPool.getDefault();
            CtClass cls = pool.get("org.jboss.seam.transaction.Transaction");

            // Replace Component's method bodies with the ones in
            // AutowireComponent
            CtMethod methodToReplace =
                    cls.getDeclaredMethod("instance", new CtClass[] {});
            methodToReplace
                    .setBody("{ return org.zanata.seam.AutowireTransaction.instance(); }");

            cls.toClass();
        } catch (NotFoundException e) {
            throw new RuntimeException(
                    "Problem rewiring Seam's Transaction class", e);
        } catch (CannotCompileException e) {
            throw new RuntimeException(
                    "Problem rewiring Seam's Transaction class", e);
        }
    }

    private static ComponentAccessor[]
            getAllComponentAccessors(Object component) {
        Collection<ComponentAccessor> props =
                new ArrayList<ComponentAccessor>();

        for (Field f : getAllComponentFields(component)) {
            if (f.getAnnotation(In.class) != null
                    || f.getAnnotation(Logger.class) != null) {
                props.add(ComponentAccessor.newInstance(f));
            }
        }
        for (Method m : getAllComponentMethods(component)) {
            if (m.getAnnotation(In.class) != null
                    || m.getAnnotation(Logger.class) != null) {
                props.add(ComponentAccessor.newInstance(m));
            }
        }

        return props.toArray(new ComponentAccessor[props.size()]);
    }

    private static Field[] getAllComponentFields(Object component) {
        Field[] fields = component.getClass().getDeclaredFields();
        Class<?> superClass = component.getClass().getSuperclass();

        while (superClass != null) {
            fields =
                    (Field[]) ArrayUtils.addAll(fields,
                            superClass.getDeclaredFields());
            superClass = superClass.getSuperclass();
        }

        return fields;
    }

    private static Method[] getAllComponentMethods(Object component) {
        Method[] methods = component.getClass().getDeclaredMethods();
        Class<?> superClass = component.getClass().getSuperclass();

        while (superClass != null) {
            methods =
                    (Method[]) ArrayUtils.addAll(methods,
                            superClass.getDeclaredMethods());
            superClass = superClass.getSuperclass();
        }

        return methods;
    }

    private void registerInterfaces(Class<?> cls) {
        assert !cls.isInterface();
        // register all interfaces registered by this component
        for (Class<?> iface : getAllInterfaces(cls)) {
            this.componentImpls.put(iface, cls);
        }
    }

    private static Set<Class<?>> getAllInterfaces(Class<?> cls) {
        Set<Class<?>> interfaces = new HashSet<Class<?>>();

        for (Class<?> superClass : cls.getInterfaces()) {
            interfaces.add(superClass);
            interfaces.addAll(getAllInterfaces(superClass));
        }

        return interfaces;
    }

    /**
     * Invokes a single method (the first found) annotated with
     * {@link javax.annotation.PostConstruct},
     * {@link org.jboss.seam.annotations.intercept.PostConstruct}, or
     * {@link org.jboss.seam.annotations.Create} annotations.
     */
    private static void invokePostConstructMethod(Object component) {
        Class<?> compClass = component.getClass();
        boolean postConstructAlreadyFound = false;

        for (Method m : compClass.getDeclaredMethods()) {
            // Call the first Post Constructor found. Per the spec, there should
            // be only one
            if (m.getAnnotation(javax.annotation.PostConstruct.class) != null
                    || m.getAnnotation(org.jboss.seam.annotations.intercept.PostConstruct.class) != null
                    || m.getAnnotation(org.jboss.seam.annotations.Create.class) != null) {
                if (postConstructAlreadyFound) {
                    log.warn("More than one PostConstruct method found for class "
                            + compClass.getName()
                            + ", only one will be invoked");
                    break;
                }

                try {
                    m.invoke(component); // there should be no params
                    postConstructAlreadyFound = true;
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(
                            "Error invoking Post construct method in component of class: "
                                    + compClass.getName(), e);
                } catch (InvocationTargetException e) {
                    throw new RuntimeException(
                            "Error invoking Post construct method in component of class: "
                                    + compClass.getName(), e);
                }
            }
        }
    }

    public static String getComponentName(Class componentClass) {
        Annotation name = componentClass.getAnnotation(Name.class);
        Preconditions.checkNotNull(name);
        return ((Name) name).value();
    }


}
