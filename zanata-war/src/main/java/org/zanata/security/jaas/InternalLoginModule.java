package org.zanata.security.jaas;

import java.security.acl.Group;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zanata.security.SimpleGroup;
import org.zanata.security.SimplePrincipal;
import org.zanata.security.ZanataIdentity;
import org.zanata.seam.security.IdentityManager;

import static org.zanata.security.ZanataIdentity.*;

/**
 * Login module for internal authentication.
 *
 * @author Patrick Huang <a
 *         href="mailto:pahuang@redhat.com">pahuang@redhat.com</a>
 */
public class InternalLoginModule implements LoginModule {
    private static final Logger log =
            LoggerFactory.getLogger(InternalLoginModule.class);

    protected Set<String> roles = new HashSet<>();

    protected Subject subject;
    protected Map<String, ?> options;
    protected CallbackHandler callbackHandler;

    protected String username;

    public boolean abort() throws LoginException {
        return true;
    }

    public boolean commit() throws LoginException {
        subject.getPrincipals().add(new SimplePrincipal(username));

        Group roleGroup = null;

        for (Group g : subject.getPrincipals(Group.class)) {
            if (ROLES_GROUP.equalsIgnoreCase(g.getName())) {
                roleGroup = g;
                break;
            }
        }

        if (roleGroup == null) {
            roleGroup = new SimpleGroup(ROLES_GROUP);
        }

        for (String role : roles) {
            roleGroup.addMember(new SimplePrincipal(role));
        }

        subject.getPrincipals().add(roleGroup);

        return true;
    }

    public void initialize(Subject subject, CallbackHandler callbackHandler,
            Map<String, ?> sharedState, Map<String, ?> options) {
        this.subject = subject;
        this.options = options;
        this.callbackHandler = callbackHandler;
    }

    public boolean login() throws LoginException {
        try {
            NameCallback cbName = new NameCallback("Enter username");
            PasswordCallback cbPassword =
                    new PasswordCallback("Enter password", false);

            // Get the username and password from the callback handler
            callbackHandler.handle(new Callback[] { cbName, cbPassword });
            username = cbName.getName();
        } catch (Exception ex) {
            log.warn("Error logging in", ex);
            LoginException le = new LoginException(ex.getMessage());
            le.initCause(ex);
            throw le;
        }

        IdentityManager identityManager = IdentityManager.instance();
        if (identityManager != null && identityManager.isEnabled()) {
            ZanataIdentity identity = ZanataIdentity.instance();

            try {
                boolean success =
                        identityManager.authenticate(username, identity
                                .getCredentials().getPassword());

                if (success) {
                    List<String> impliedRoles =
                            identityManager.getImpliedRoles(username);
                    for (String role : impliedRoles) {
                        identity.addRole(role);
                    }
                }

                return success;
            } catch (Exception ex) {
                log.warn("Error invoking login method");
                LoginException le = new LoginException(ex.getMessage());
                le.initCause(ex);
                throw le;
            }
        } else {
            log.error("No authentication method defined - "
                    +
                    "please define authenticate-method for <security:identity/> in components.xml");
            throw new LoginException("No authentication method defined");
        }

    }

    public boolean logout() throws LoginException {
        return true;
    }
}
