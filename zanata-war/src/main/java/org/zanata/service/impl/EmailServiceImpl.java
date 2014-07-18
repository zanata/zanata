/*
 * Copyright 2012, Red Hat, Inc. and individual contributors as indicated by the
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
package org.zanata.service.impl;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.security.RunAsOperation;
import org.jboss.seam.security.management.IdentityManager;
import org.zanata.ApplicationConfiguration;
import org.zanata.action.VersionGroupJoinAction;
import org.zanata.dao.PersonDAO;
import org.zanata.email.ActivationEmailStrategy;
import org.zanata.email.Addresses;
import org.zanata.email.EmailBuilder;
import org.zanata.email.EmailStrategy;
import org.zanata.email.EmailValidationEmailStrategy;
import org.zanata.email.PasswordResetEmailStrategy;
import org.zanata.email.UsernameChangedEmailStrategy;
import org.zanata.i18n.Messages;
import org.zanata.model.HLocale;
import org.zanata.model.HLocaleMember;
import org.zanata.model.HPerson;
import org.zanata.service.EmailService;

import javax.mail.internet.InternetAddress;

import static org.zanata.email.Addresses.getAddresses;

/**
 * @author Alex Eng <a href="mailto:aeng@redhat.com">aeng@redhat.com</a>
 */
@AutoCreate
@Name("emailServiceImpl")
@Scope(ScopeType.STATELESS)
@Slf4j
public class EmailServiceImpl implements EmailService {

    @In
    private EmailBuilder emailBuilder;

    @In
    private IdentityManager identityManager;

    @In
    private PersonDAO personDAO;

    @In
    private ApplicationConfiguration applicationConfiguration;

    @In
    private VersionGroupJoinAction versionGroupJoinAction;

    @In
    private Messages msgs;

    /**
     *
     * @return the list of users with the admin role
     */
    private List<HPerson> getAdmins() {
        // required to read admin users for a non-admin session
        final List<HPerson> admins = new ArrayList<HPerson>();
        new RunAsOperation() {
            @Override
            public void execute() {
                for (Principal admin : identityManager.listMembers("admin")) {
                    admins.add(personDAO.findByUsername(admin.getName()));
                }
            }
        }.addRole("admin").run();

        return admins;
    }

    private List<HPerson> getCoordinators(HLocale locale) {
        List<HPerson> coordinators = new ArrayList<HPerson>();

        for (HLocaleMember member : locale.getMembers()) {
            if (member.isCoordinator()) {
                coordinators.add(member.getPerson());
            }
        }
        return coordinators;
    }

    @Override
    public String sendActivationEmail(String toName,
            String toEmailAddr, String activationKey) {
        InternetAddress to = Addresses.getAddress(toEmailAddr, toName);
        emailBuilder.sendMessage(new ActivationEmailStrategy(activationKey),
                null, to);
        return msgs.get("jsf.Account.ActivationMessage");
    }

    @Override
    public String sendEmailValidationEmail(String toName,
            String toEmailAddr, String activationKey) {
        InternetAddress to = Addresses.getAddress(toEmailAddr, toName);
        emailBuilder.sendMessage(new EmailValidationEmailStrategy(activationKey),
                null, to);
        return msgs.get("jsf.email.accountchange.SentNotification");
    }

    @Override
    public String sendPasswordResetEmail(HPerson person, String key) {
        InternetAddress to = Addresses.getAddress(person);
        emailBuilder.sendMessage(new PasswordResetEmailStrategy(key), null, to);
        return msgs.get("jsf.email.passwordreset.SentNotification");
    }

    @Override
    public String sendToAdmins(EmailStrategy strategy) {
        List<String> adminEmails = applicationConfiguration.getAdminEmail();
        if (!adminEmails.isEmpty()) {
            String receivedReason = msgs.get("jsf.email.admin.ReceivedReason");
            String toName = msgs.get("jsf.ZanataAdministrator");
            emailBuilder.sendMessage(strategy, receivedReason,
                    getAddresses(adminEmails, toName));
            return msgs.get("jsf.email.admin.SentNotification");
        } else {
            return sendToAdminUsers(strategy);
        }
    }

    /**
     * Emails admin users with given template
     *
     */
    private String sendToAdminUsers(EmailStrategy strategy) {
        String receivedReason = msgs.get(
                "jsf.email.admin.user.ReceivedReason");
        emailBuilder.sendMessage(strategy, receivedReason,
                getAddresses(getAdmins()));
        return msgs.get("jsf.email.admin.SentNotification");
    }

    @Override
    public String sendToLanguageCoordinators(HLocale locale,
            EmailStrategy strategy) {
        List<HPerson> coordinators = getCoordinators(locale);
        if (!coordinators.isEmpty()) {
            String receivedReason = msgs.format(
                    "jsf.email.coordinator.ReceivedReason",
                    locale.retrieveNativeName());

            emailBuilder.sendMessage(strategy, receivedReason,
                    getAddresses(coordinators));
            return msgs.format("jsf.email.coordinator.SentNotification",
                    locale.retrieveNativeName());
        } else {
            return sendToAdmins(strategy);
        }
    }

    @Override
    public String sendToVersionGroupMaintainers(List<HPerson> maintainers,
            EmailStrategy strategy) {
        if (!maintainers.isEmpty()) {
            String receivedReason = msgs.format(
                    "jsf.email.group.maintainer.ReceivedReason",
                    versionGroupJoinAction.getGroupName());
            emailBuilder.sendMessage(strategy, receivedReason,
                    getAddresses(maintainers));
            return msgs.format("jsf.email.group.maintainer.SentNotification",
                    versionGroupJoinAction.getGroupName());
        } else {
            return sendToAdmins(strategy);
        }
    }

    @Override
    public String sendUsernameChangedEmail(String email, String newUsername) {
        InternetAddress to = Addresses.getAddress(email, newUsername);
        boolean resetPassword = applicationConfiguration.isInternalAuth();
        emailBuilder.sendMessage(new UsernameChangedEmailStrategy(
                newUsername, resetPassword), null, to);
        return msgs.get("jsf.email.usernamechange.SentNotification");
    }

}
