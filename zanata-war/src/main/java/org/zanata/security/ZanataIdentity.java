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
package org.zanata.security;

import static org.jboss.seam.ScopeType.SESSION;
import static org.jboss.seam.annotations.Install.APPLICATION;

import java.io.Serializable;
import java.security.Principal;
import java.security.acl.Group;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.annotation.Nullable;
import javax.security.auth.Subject;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import com.google.common.annotations.VisibleForTesting;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Startup;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.contexts.Contexts;
import org.zanata.exception.AuthorizationException;
import org.zanata.exception.NotLoggedInException;
import org.jboss.seam.web.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zanata.events.AlreadyLoggedInEvent;
import org.zanata.events.LoginFailedEvent;
import org.zanata.events.LoginSuccessfulEvent;
import org.zanata.events.Logout;
import org.zanata.events.NotLoggedInEvent;
import org.zanata.model.HAccount;
import org.zanata.model.HasUserFriendlyToString;
import org.zanata.seam.security.ZanataJpaIdentityStore;
import org.zanata.security.jaas.InternalLoginModule;
import org.zanata.security.permission.CustomPermissionResolver;
import org.zanata.security.permission.MultiTargetList;
import org.zanata.util.Event;
import org.zanata.util.ServiceLocator;

import com.google.common.collect.Lists;

@Name("org.jboss.seam.security.identity")
@Scope(SESSION)
@Install(precedence = APPLICATION)
@BypassInterceptors
@Startup
public class ZanataIdentity implements Identity, Serializable {
    private static final Logger log = LoggerFactory.getLogger(
            ZanataIdentity.class);

    public static final String JAAS_DEFAULT = "default";
    public static final String ROLES_GROUP = "Roles";

    private static final long serialVersionUID = -5488977241602567930L;

    protected static boolean securityEnabled = true;
    // Seam Context variables
    // TODO [CDI] revisit this (the value and its usage in CDI)
    private static final String LOGIN_TRIED = "org.jboss.seam.security.loginTried";
    private static final String SILENT_LOGIN = "org.jboss.seam.security.silentLogin";

    private transient ThreadLocal<Boolean> systemOp;

    private String apiKey;

    private boolean preAuthenticated;
    private Subject subject;
    private Principal principal;
    private List<String> preAuthenticationRoles = new ArrayList<>();
    CustomPermissionResolver permissionResolver;
    private ZanataCredentials credentials;
    private boolean authenticating;
    private String jaasConfigName = "zanata";

    @Create
    public void create() {
        subject = new Subject();

        if (Contexts.isApplicationContextActive()) {
            permissionResolver = ServiceLocator.instance()
                    .getInstance(CustomPermissionResolver.class);
        }

        if (Contexts.isSessionContextActive()) {
            credentials =
                    ServiceLocator.instance().getInstance(ZanataCredentials.class);
        }

        if (credentials == null) {
            // Must have credentials for unit tests
            credentials = new ZanataCredentials();
        }
    }

    public static boolean isSecurityEnabled() {
        return securityEnabled;
    }

    public static void setSecurityEnabled(boolean securityEnabled) {
        ZanataIdentity.securityEnabled = securityEnabled;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
        getCredentials().setPassword(apiKey);
    }

    public boolean isApiRequest() {
        return apiKey != null;
    }

    public static ZanataIdentity instance() {
        if (!Contexts.isSessionContextActive()) {
            throw new IllegalStateException("No active session context");
        }

        ZanataIdentity instance =
                ServiceLocator.instance().getInstance(ZanataIdentity.class);

        if (instance == null) {
            throw new IllegalStateException("No Identity could be created");
        }

        return instance;
    }

    public void checkLoggedIn() {
        if (!isLoggedIn()) {
            throw new NotLoggedInException();
        }
    }

    public boolean isLoggedIn() {
        return getPrincipal() != null;
    }

    public ZanataCredentials getCredentials() {
        return credentials;
    }

    public Subject getSubject() {
        return subject;
    }

    public void acceptExternallyAuthenticatedPrincipal(Principal principal) {
        getSubject().getPrincipals().add(principal);
        this.principal = principal;
    }

    @Observer("org.jboss.seam.preDestroyContext.SESSION")
    public void logout() {
        if (getCredentials() != null) {
            getLogoutEvent().fire(new Logout(getCredentials().getUsername()));
        }
        if (isLoggedIn()) {
            unAuthenticate();
            Session.instance().invalidate();
        }
    }

    private Event<Logout> getLogoutEvent() {
        return ServiceLocator.instance().getInstance("event", Event.class);
    }

    public boolean hasRole(String role) {
        if (!securityEnabled)
            return true;
        if (systemOp != null && Boolean.TRUE.equals(systemOp.get()))
            return true;

        tryLogin();

        for (Group sg : getSubject().getPrincipals(Group.class)) {
            if (ROLES_GROUP.equals(sg.getName())) {
                return sg.isMember(new Role(role));
            }
        }
        return false;
    }

    public void checkRole(String role) {
        tryLogin();

        if (!hasRole(role)) {
            if (!isLoggedIn()) {
                // used by org.zanata.security.FacesSecurityEvents.addNotLoggedInMessage()
                getNotLoggedInEvent().fire(new NotLoggedInEvent());
                throw new NotLoggedInException();
            } else {
                throw new AuthorizationException(String.format(
                        "Authorization check failed for role [%s]", role));
            }
        }
    }

    /**
     * Resets all security state and credentials
     */
    public void unAuthenticate() {
        principal = null;
        subject = new Subject();

        credentials.clear();
    }

    public boolean hasPermission(Object target, String action) {
        log.trace("ENTER hasPermission({}, {})", target, action);
        boolean result = resolvePermission(target, action);
        if (result) {
            if (log.isDebugEnabled()) {
                log.debug("ALLOWED hasPermission({}, {}) for user {}",
                        target, action, getAccountUsername());
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug("DENIED hasPermission({}, {}) for user {}",
                        target, action, getAccountUsername());
            }
        }
        log.trace("EXIT hasPermission(): {}", result);
        return result;
    }

    private boolean resolvePermission(Object target, String action) {
        if (!securityEnabled) {
            return true;
        }
        if (systemOp != null && Boolean.TRUE.equals(systemOp.get())) {
            return true;
        }
        if (permissionResolver == null) {
            return false;
        }
        if (target == null) {
            return false;
        }

        return permissionResolver.hasPermission(target, action);
    }

    public boolean hasPermission(String name, String action, Object... arg) {
        if (log.isTraceEnabled()) {
            log.trace("ENTER hasPermission({})",
                    Lists.newArrayList(name, action, arg));
        }
        boolean result = resolvePermission(name, action, arg);
        if (result) {
            if (log.isDebugEnabled()) {
                log.debug("ALLOWED hasPermission({}, {}, {}) for user {}",
                        name, action, Lists.newArrayList(arg), getAccountUsername());
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug("DENIED hasPermission({}, {}, {}) for user {}",
                        name, action, Lists.newArrayList(arg), getAccountUsername());
            }
        }
        log.trace("EXIT hasPermission(): {}", result);
        return result;
    }

    private boolean resolvePermission(String name, String action, Object... arg) {
        if (!securityEnabled) {
            return true;
        }
        if (systemOp != null && Boolean.TRUE.equals(systemOp.get())) {
            return true;
        }
        if (permissionResolver == null) {
            return false;
        }

        if (arg != null && arg.length > 0) {
            return permissionResolver.hasPermission(arg[0], action);
        } else {
            return permissionResolver.hasPermission(name, action);
        }
    }

    /**
     * Indicates if the user has permission to perform an action on a variable
     * number of targets. This is provided as an extension to Seam's single
     * target permission capabilities.
     *
     * @param action
     *            The permission action.
     * @param targets
     *            Targets for permissions.
     */
    public boolean hasPermission(String action, Object... targets) {
        return hasPermission(MultiTargetList.fromTargets(targets), action);
    }

    /**
     * Checks permissions on a variable number of targets.This is provided as an
     * extension to Seam's single target permission capabilities.
     *
     * @param action
     *            The permission action.
     * @param targets
     *            Targets for permissions.
     * @throws NotLoggedInException
     *             if not authorised and not logged in
     * @throws org.zanata.exception.AuthorizationException
     *             if logged in but not authorised
     */
    public void checkPermission(String action, Object... targets) {
        try {
            internalCheckPermission(MultiTargetList.fromTargets(targets),
                    action);
        } catch (AuthorizationException exception) {
            // try to produce a better than default error message
            List<String> meaningfulTargets = Lists.newArrayList();
            for (Object target : targets) {
                if (target instanceof HasUserFriendlyToString) {
                    String targetString = ((HasUserFriendlyToString) target).userFriendlyToString();
                    meaningfulTargets.add(targetString);
                } else {
                    log.warn(
                            "target [{}] may not have user friendly string representation",
                            target.getClass());
                    meaningfulTargets.add(target.toString());
                }
            }
            throw new AuthorizationException(
                    String.format(
                            "Failed to obtain permission('%s') with following facts(%s)",
                            action, meaningfulTargets));
        }
    }

    public void checkPermission(Object target, String action) {
        internalCheckPermission(target, action);
    }

    // based on org.jboss.seam.security.Identity
    private void internalCheckPermission(Object target, String action) {
        if (systemOp != null && Boolean.TRUE.equals(systemOp.get())) return;

        tryLogin();

        if (!hasPermission(target, action)) {
            if (!isLoggedIn()) {
                // used by
                // org.zanata.security.FacesSecurityEvents.addNotLoggedInMessage()
                getNotLoggedInEvent().fire(new NotLoggedInEvent());
                throw new NotLoggedInException();
            } else {
                throw new AuthorizationException(String.format(
                        "Authorization check failed for permission[%s,%s]",
                        target, action));
            }
        }
    }

    private Event<NotLoggedInEvent> getNotLoggedInEvent() {
        return ServiceLocator.instance().getInstance("event", Event.class);
    }

    // copied from org.jboss.seam.security.Identity.tryLogin()
    public boolean tryLogin() {
        if (!authenticating && getPrincipal() == null && credentials.isSet() &&
                Contexts.isEventContextActive() &&
                !Contexts.getEventContext().isSet(LOGIN_TRIED)) {
            Contexts.getEventContext().set(LOGIN_TRIED, true);
            quietLogin();
        }

        return isLoggedIn();
    }

    // copied from org.jboss.seam.security.Identity.quietLogin()
    private void quietLogin() {
        try {
            // N.B. this will trigger Seam's RememberMe functionality and causes
            // a class cast exception (ZanataIdentity is no loger Identity)
//            if (Events.exists()) Events.instance().raiseEvent(Identity.EVENT_QUIET_LOGIN);

            // Ensure that we haven't been authenticated as a result of the EVENT_QUIET_LOGIN event
            if (!isLoggedIn()) {
                if (credentials.isSet()) {
                    authenticate();
                    if (isLoggedIn() && Contexts.isEventContextActive()) {
                        Contexts.getEventContext().set(SILENT_LOGIN, true);
                    }
                }
            }
        } catch (LoginException ex) {
            credentials.invalidate();
        }
    }

    // based on org.jboss.seam.security.Identity.authenticate()
    private synchronized void authenticate() throws LoginException {
        // If we're already authenticated, then don't authenticate again
        if (!isLoggedIn() && !credentials.isInvalid()) {
            principal = null;
            subject = new Subject();
            try {
                authenticating = true;
                preAuthenticate();
                getLoginContext().login();
                postAuthenticate();
            } finally {
                // Set password to null whether authentication is successful or not
                credentials.setPassword(null);
                authenticating = false;
            }
        }
    }

    // copied from org.jboss.seam.security.Identity
    private void preAuthenticate() {
        preAuthenticationRoles.clear();
    }

    // copied from org.jboss.seam.security.Identity
    protected void postAuthenticate() {
        // Populate the working memory with the user's principals
        for (Principal p : getSubject().getPrincipals()) {
            if (!(p instanceof Group)) {
                if (principal == null) {
                    principal = p;
                    break;
                }
            }
        }

        if (!preAuthenticationRoles.isEmpty() && isLoggedIn()) {
            for (String role : preAuthenticationRoles) {
                addRole(role);
            }
            preAuthenticationRoles.clear();
        }

        credentials.setPassword(null);

        // It's used in:
        // - org.jboss.seam.security.management.JpaIdentityStore.setUserAccountForSession()
        // - org.jboss.seam.security.FacesSecurityEvents.postAuthenticate(Identity)
        // -org.jboss.seam.security.RememberMe.postAuthenticate(Identity)
        // to avoid a class cast exception, we pass Identity here (FacesSecurityEvents is not doing anything with it)
        // We already set authenticatedUser in session so no need to raise this event any more
//        if (Events.exists()) {
//            Events.instance().raiseEvent(Identity.EVENT_POST_AUTHENTICATE,
//                    new Identity());
//        }
    }

    // copied from org.jboss.seam.security.Identity
    public boolean addRole(String role) {
        if (role == null || "".equals(role)) {
            return false;
        }

        if (!isLoggedIn()) {
            preAuthenticationRoles.add(role);
            return false;
        } else {
            for (Group sg : getSubject().getPrincipals(Group.class)) {
                if (ROLES_GROUP.equals(sg.getName())) {
                    return sg.addMember(new Role(role));
                }
            }

            Group roleGroup = new SimpleGroup(ROLES_GROUP);
            roleGroup.addMember(new Role(role));
            getSubject().getPrincipals().add(roleGroup);
            return true;
        }
    }

    public String getJaasConfigName() {
        return jaasConfigName;
    }

    public void setJaasConfigName(String jaasConfigName) {
        this.jaasConfigName = jaasConfigName;
    }

    public LoginContext getLoginContext() throws LoginException {
        if (isApiRequest()) {
            return new LoginContext(JAAS_DEFAULT, getSubject(),
                    getCredentials().createCallbackHandler(),
                    ZanataConfiguration.INSTANCE);
        }
        if (getJaasConfigName() != null
                && !getJaasConfigName().equals(JAAS_DEFAULT)) {
            return new LoginContext(getJaasConfigName(), getSubject(),
                    getCredentials().createCallbackHandler());
        }

        return new LoginContext(JAAS_DEFAULT, getSubject(), getCredentials()
                .createCallbackHandler(), ZanataConfiguration.INSTANCE);
    }

    public boolean isPreAuthenticated() {
        return preAuthenticated;
    }

    public void setPreAuthenticated(boolean var) {
        this.preAuthenticated = var;
    }

    public String login() {
        // Default to internal authentication
        return this.login(AuthenticationType.INTERNAL);
    }

    public String login(AuthenticationType authType) {
        getCredentials().setAuthType(authType);
        try {
            if (isLoggedIn()) {
                // If authentication has already occurred during this request
                // via a silent login,
                // and login() is explicitly called then we still want to raise
                // the LOGIN_SUCCESSFUL event,
                // and then return.
                if (Contexts.isEventContextActive()
                        && Contexts.getEventContext().isSet(SILENT_LOGIN)) {
                    getLoginSuccessfulEvent().fire(new LoginSuccessfulEvent());
                    this.preAuthenticated = true;
                    return "loggedIn";
                }

                // used by org.zanata.security.FacesSecurityEvents.addAlreadyLoggedInMessage()
                getAlreadyLoggedInEvent().fire(new AlreadyLoggedInEvent());
                this.preAuthenticated = true;
                return "loggedIn";
            }

            authenticate();

            if (!isLoggedIn()) {
                throw new LoginException();
            }

            if (log.isDebugEnabled()) {
                log.debug("Login successful for: "
                        + getCredentials().getUsername());
            }

            // used by org.zanata.security.FacesSecurityEvents.addLoginSuccessfulMessage()
            getLoginSuccessfulEvent().fire(new LoginSuccessfulEvent());
            this.preAuthenticated = true;
            return "loggedIn";
        } catch (LoginException ex) {
            credentials.invalidate();

            if (log.isDebugEnabled()) {
                log.debug(
                        "Login failed for: " + getCredentials().getUsername(),
                        ex);
            }
            // used by org.zanata.security.FacesSecurityEvents.addLoginFailedMessage()
            getLoginFailedEvent().fire(new LoginFailedEvent(ex));
        }

        return null;
    }

    private Event<AlreadyLoggedInEvent> getAlreadyLoggedInEvent() {
        return ServiceLocator.instance().getInstance("event", Event.class);
    }

    private Event<LoginSuccessfulEvent> getLoginSuccessfulEvent() {
        return ServiceLocator.instance().getInstance("event", Event.class);
    }

    private Event<LoginFailedEvent> getLoginFailedEvent() {
        return ServiceLocator.instance().getInstance("event", Event.class);
    }

    /**
     * Utility method to get the authenticated account username. This differs
     * from {@link org.jboss.seam.security.Credentials#getUsername()} in that
     * this returns the actual account's username, not the user provided one
     * (which for some authentication systems is non-existent).
     *
     * @return The currently authenticated account username, or null if the
     *         session is not authenticated.
     */
    @Nullable
    public String getAccountUsername() {
        HAccount authenticatedAccount =
                ServiceLocator.instance().getInstance(
                        ZanataJpaIdentityStore.AUTHENTICATED_USER, HAccount.class);
        if (authenticatedAccount != null) {
            return authenticatedAccount.getUsername();
        }
        return null;
    }

    public Principal getPrincipal() {
        return principal;
    }

    // copied from org.jboss.seam.security.Identity
    @Override
    public synchronized void runAs(RunAsOperation operation) {
        Principal savedPrincipal = getPrincipal();
        Subject savedSubject = getSubject();

        try {
            principal = operation.getPrincipal();
            subject = operation.getSubject();

            if (systemOp == null) {
                systemOp = new ThreadLocal<>();
            }

            systemOp.set(operation.isSystemOperation());

            operation.execute();
        } finally {
            // Since this bean is a session scoped bean and the threadlocal
            // field is a trancient instance variable, we don't need to worry
            // about removing the value from it (in turns of memory leak in
            // multi-threading environment
            systemOp.set(false);
            principal = savedPrincipal;
            subject = savedSubject;
        }
    }

    @VisibleForTesting
    protected void setPermissionResolver(CustomPermissionResolver resolver) {
        this.permissionResolver = resolver;
    }

    static class ZanataConfiguration extends
            javax.security.auth.login.Configuration {
        private static final javax.security.auth.login.Configuration INSTANCE =
                new ZanataConfiguration();

        private AppConfigurationEntry[] aces = {
                new AppConfigurationEntry(
                        InternalLoginModule.class.getName(),
                        AppConfigurationEntry.LoginModuleControlFlag.REQUIRED,
                        new HashMap<String, String>()
                )
        };

        @Override
        public AppConfigurationEntry[] getAppConfigurationEntry(String name) {
            return JAAS_DEFAULT.equals(name) ? aces : null;
        }

        @Override
        public void refresh() {
        }
    }
}
