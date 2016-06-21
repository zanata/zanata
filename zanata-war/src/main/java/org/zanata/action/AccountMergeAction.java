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
package org.zanata.action;

import static javax.faces.application.FacesMessage.SEVERITY_ERROR;

import java.io.Serializable;

import lombok.Getter;
import lombok.Setter;

import javax.annotation.Nullable;
import javax.enterprise.context.SessionScoped;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import org.zanata.dao.AccountDAO;
import org.zanata.model.HAccount;
import org.zanata.security.AuthenticationManager;
import org.zanata.security.ZanataIdentity;
import org.zanata.security.annotations.Authenticated;
import org.zanata.security.openid.OpenIdAuthCallback;
import org.zanata.security.openid.OpenIdAuthenticationResult;
import org.zanata.security.openid.OpenIdProviderType;
import org.zanata.service.RegisterService;
import org.zanata.ui.faces.FacesMessages;
import org.zanata.util.Synchronized;

/**
 * @author Carlos Munoz <a
 *         href="mailto:camunoz@redhat.com">camunoz@redhat.com</a>
 */
@Named("accountMergeAction")
@javax.faces.bean.ViewScoped
public class AccountMergeAction implements Serializable {

    @SessionScoped
    @Synchronized
    static class ObsoleteHolder implements Serializable {
        private static final long serialVersionUID = 1L;
        @Nullable HAccount account;
    }

    private static final long serialVersionUID = 1L;

    @Inject
    @Authenticated
    private HAccount authenticatedAccount;

    @Inject
    private FacesMessages facesMessages;

    @Inject
    private AuthenticationManager authenticationManager;

    @Inject
    private RegisterService registerServiceImpl;

    @Getter
    @Setter
    private String openId = "http://";

    @Inject
    private ObsoleteHolder obsolete;

    private boolean accountsValid;

    public @Nullable HAccount getObsoleteAccount() {
        return obsolete.account;
    }

    private void setObsoleteAccount(@Nullable HAccount obsoleteAccount) {
        obsolete.account = obsoleteAccount;
    }

    @PostConstruct
    public void onCreate() {
        ZanataIdentity.instance().checkLoggedIn();
    }

    public boolean getAccountsValid() {
        return accountsValid;
    }

    public void loginToMergingAccount(String provider) {
        if (provider.equalsIgnoreCase("Internal")) {
            // no implementation for internal account merging yet
        } else {
            OpenIdProviderType providerType;
            try {
                providerType = OpenIdProviderType.valueOf(provider);
            } catch (IllegalArgumentException e) {
                providerType = OpenIdProviderType.Generic;
            }

            if (providerType == OpenIdProviderType.Generic) {
                authenticationManager.openIdAuthenticate(openId, providerType,
                        new AccountMergeAuthCallback());
            } else {
                authenticationManager.openIdAuthenticate(providerType,
                        new AccountMergeAuthCallback());
            }
        }
    }

    public boolean isAccountSelected() {
        return getObsoleteAccount() != null;
    }

    public void validateAccounts() {
        boolean valid = true;

        // The account to merge in has been authenticated
        HAccount obsoleteAccount = getObsoleteAccount();
        if (obsoleteAccount != null) {
            if (obsoleteAccount.getId() == null) {
                facesMessages.addGlobal(SEVERITY_ERROR,
                        "Could not find an account for that user.");
                valid = false;
            } else if (authenticatedAccount.getId().equals(
                    obsoleteAccount.getId())) {
                facesMessages.addGlobal(SEVERITY_ERROR,
                        "You are attempting to merge the same account.");
                valid = false;
            }
        }

        this.accountsValid = valid;
    }

    public void mergeAccounts() {
        registerServiceImpl
                .mergeAccounts(authenticatedAccount, getObsoleteAccount());
        setObsoleteAccount(null); // reset the obsolete account
        facesMessages.addGlobal("Your accounts have been merged.");
    }

    public String cancel() {
        // see faces-config.xml
        setObsoleteAccount(null);
        return "cancel";
    }

    private static class AccountMergeAuthCallback implements
            OpenIdAuthCallback, Serializable {
        private static final long serialVersionUID = 1L;

        @Inject
        private AccountDAO accountDAO;

        @Inject
        private ObsoleteHolder obsoleteHolder;

        @Override
        public void afterOpenIdAuth(OpenIdAuthenticationResult result) {
            if (result.isAuthenticated()) {
                HAccount account =
                        accountDAO.getByCredentialsId(result
                                .getAuthenticatedId());
                if (account == null) {
                    account = new HAccount(); // In case an account is not found
                }
                obsoleteHolder.account = account;
            }
        }

        @Override
        public String getRedirectToUrl() {
            return "/profile/merge_account.xhtml";
        }
    }
}
