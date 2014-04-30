/*
 * Copyright 2010, Red Hat, Inc. and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.zanata.action;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.faces.application.FacesMessage;

import org.hibernate.validator.constraints.Email;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.LocaleSelector;
import org.jboss.seam.security.management.JpaIdentityStore;
import org.zanata.common.LocaleId;
import org.zanata.model.HAccount;
import org.zanata.model.HLocale;
import org.zanata.model.HLocaleMember;
import org.zanata.model.HPerson;
import org.zanata.seam.scope.ConversationScopeMessages;
import org.zanata.service.EmailService;
import org.zanata.service.LocaleService;
import org.zanata.util.ZanataMessages;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Sends an email to a specified role.
 * 
 * Currently just sends an email to admin.
 * 
 * @author damason@redhat.com
 * 
 */
@Name("sendEmail")
@Scope(ScopeType.PAGE)
@Slf4j
public class SendEmailAction implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final String EMAIL_TYPE_CONTACT_ADMIN = "contact_admin";
    private static final String EMAIL_TYPE_CONTACT_COORDINATOR =
            "contact_coordinator";
    private static final String EMAIL_TYPE_REQUEST_JOIN =
            "request_join_language";
    private static final String EMAIL_TYPE_REQUEST_ROLE =
            "request_role_language";
    private static final String EMAIL_TYPE_REQUEST_TO_JOIN_GROUP =
            "request_to_join_group";

    @In
    private EmailService emailServiceImpl;

    @In(required = true, value = JpaIdentityStore.AUTHENTICATED_USER)
    private HAccount authenticatedAccount;

    @In
    private LocaleService localeServiceImpl;

    @In
    private LocaleSelector localeSelector;

    @Getter
    @Setter
    private String fromName;

    @Getter
    @Setter
    private String fromLoginName;

    @Email
    @Getter
    @Setter
    private String replyEmail;

    @Getter
    @Setter
    private String subject;

    @Getter
    @Setter
    private String htmlMessage;

    @Getter
    @Setter
    private String emailType;

    @Getter
    private String language;

    @Getter
    private HLocale locale;

    @In
    private ConversationScopeMessages conversationScopeMessages;

    private List<HPerson> groupMaintainers;

    @In
    private ZanataMessages zanataMessages;

    public static final String SUCCESS = "success";
    public static final String FAILED = "failure";

    @Create
    public void onCreate() {
        if (authenticatedAccount == null) {
            log.error("SendEmailAction failed to load authenticated account");
            return;
        }
        fromName = authenticatedAccount.getPerson().getName();
        fromLoginName = authenticatedAccount.getUsername();
        replyEmail = authenticatedAccount.getPerson().getEmail();

        subject = "";
        htmlMessage = "";
    }

    public void setLanguage(String language) {
        this.language = language;
        locale = localeServiceImpl.getByLocaleId(new LocaleId(language));
    }

    private List<HPerson> getCoordinators() {
        List<HPerson> coordinators = new ArrayList<HPerson>();

        for (HLocaleMember member : locale.getMembers()) {
            if (member.isCoordinator()) {
                coordinators.add(member.getPerson());
            }
        }
        return coordinators;
    }

    /**
     * Sends the email by rendering an appropriate email template with the
     * values in this bean.
     * 
     * @return a view to redirect to. This should be replaced with configuration
     *         in pages.xml
     */
    public String send() {
        Locale pervLocale = localeSelector.getLocale();
        localeSelector.setLocale(new Locale("en"));

        try {
            if (emailType.equals(EMAIL_TYPE_CONTACT_ADMIN)) {
                String msg =
                        emailServiceImpl
                                .sendToAdminEmails(
                                        EmailService.ADMIN_EMAIL_TEMPLATE,
                                        fromName, fromLoginName, replyEmail,
                                        subject, htmlMessage);
                FacesMessages.instance().add(msg);
                conversationScopeMessages.putMessage(
                        FacesMessage.SEVERITY_INFO, msg);
                return SUCCESS;
            } else if (emailType.equals(EMAIL_TYPE_CONTACT_COORDINATOR)) {
                String msg =
                        emailServiceImpl.sendToLanguageCoordinators(
                                EmailService.COORDINATOR_EMAIL_TEMPLATE,
                                getCoordinators(), fromName, fromLoginName,
                                replyEmail, subject, htmlMessage, language);
                FacesMessages.instance().add(msg);
                conversationScopeMessages.putMessage(
                        FacesMessage.SEVERITY_INFO, msg);
                return SUCCESS;
            } else if (emailType.equals(EMAIL_TYPE_REQUEST_JOIN)) {
                String msg =
                        emailServiceImpl.sendToLanguageCoordinators(
                                EmailService.REQUEST_TO_JOIN_EMAIL_TEMPLATE,
                                getCoordinators(), fromName, fromLoginName,
                                replyEmail, subject, htmlMessage, language);
                FacesMessages.instance().add(msg);
                conversationScopeMessages.putMessage(
                        FacesMessage.SEVERITY_INFO, msg);
                return SUCCESS;
            } else if (emailType.equals(EMAIL_TYPE_REQUEST_ROLE)) {
                String msg =
                        emailServiceImpl.sendToLanguageCoordinators(
                                EmailService.REQUEST_ROLE_EMAIL_TEMPLATE,
                                getCoordinators(), fromName, fromLoginName,
                                replyEmail, subject, htmlMessage, language);
                FacesMessages.instance().add(msg);
                conversationScopeMessages.putMessage(
                        FacesMessage.SEVERITY_INFO, msg);
                return SUCCESS;
            } else if (emailType.equals(EMAIL_TYPE_REQUEST_TO_JOIN_GROUP)) {
                String msg =
                        emailServiceImpl
                                .sendToVersionGroupMaintainer(
                                        EmailService.REQUEST_TO_JOIN_GROUP_EMAIL_TEMPLATE,
                                        groupMaintainers, fromName,
                                        fromLoginName, replyEmail, subject,
                                        htmlMessage);
                conversationScopeMessages.putMessage(
                        FacesMessage.SEVERITY_INFO, msg);
                return SUCCESS;
            } else {
                throw new Exception("Invalid email type: " + emailType);
            }
        } catch (Exception e) {
            FacesMessages.instance().add(
                    "There was a problem sending the message: "
                            + e.getMessage());
            log.error(
                    "Failed to send email: fromName '{}', fromLoginName '{}', replyEmail '{}', subject '{}', message '{}'",
                    e, fromName, fromLoginName, replyEmail, subject,
                    htmlMessage);
            return FAILED;
        } finally {
            localeSelector.setLocale(pervLocale);
        }
    }

    /**
     * @return string 'canceled'
     */
    public void cancel() {
        log.info(
                "Canceled sending email: fromName '{}', fromLoginName '{}', replyEmail '{}', subject '{}', message '{}'",
                fromName, fromLoginName, replyEmail, subject, htmlMessage);
        FacesMessages.instance().add("Sending message canceled");
        conversationScopeMessages.putMessage(FacesMessage.SEVERITY_INFO,
                "Sending message canceled");
    }

    public String sendToVersionGroupMaintainer(List<HPerson> maintainers) {
        groupMaintainers = maintainers;
        return send();
    }
}
