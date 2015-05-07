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

package org.zanata.action;

import java.io.Serializable;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.security.Restrict;
import org.jboss.seam.security.management.JpaIdentityStore;
import org.zanata.email.ContactAdminEmailStrategy;
import org.zanata.email.EmailStrategy;
import org.zanata.i18n.Messages;
import org.zanata.model.HAccount;
import org.zanata.service.EmailService;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.zanata.ui.faces.FacesMessages;

/**
 * Handles send email to admin.
 * Need to separate from SendEmailAction as contact admin now pages which has footer.xhtml
 *
 * @see org.zanata.action.SendEmailAction
 *
 * @author Alex Eng <a href="mailto:aeng@redhat.com">aeng@redhat.com</a>
 */
@Name("contactAdminAction")
@Scope(ScopeType.PAGE)
@Restrict("#{identity.loggedIn}")
@Slf4j
public class ContactAdminAction implements Serializable {

    @In(value = JpaIdentityStore.AUTHENTICATED_USER, required = false)
    private HAccount authenticatedAccount;

    @In
    private EmailService emailServiceImpl;

    @In
    private Messages msgs;

    @In("jsfMessages")
    private FacesMessages facesMessages;

    @Getter
    private String replyEmail;

    @Getter
    private String subject;

    @Getter
    @Setter
    private String message;

    private String fromName;

    private String fromLoginName;

    @Create
    public void onCreate() {
        fromName = authenticatedAccount.getPerson().getName();
        fromLoginName = authenticatedAccount.getUsername();
        replyEmail = authenticatedAccount.getPerson().getEmail();

        subject = msgs.get("jsf.message.admin.inquiry.subject");
        message = "";
    }

    public void send() {
        try {
            EmailStrategy strategy = new ContactAdminEmailStrategy(
                    fromLoginName, fromName, replyEmail, subject, message);

            facesMessages.addGlobal(emailServiceImpl.sendToAdmins(strategy,
                    null));
        } catch (Exception e) {
            StringBuilder sb =
                    new StringBuilder()
                            .append("Failed to send email with subject '")
                            .append(subject)
                            .append("' , message '").append(message)
                            .append("'");
            log.error(
                    "Failed to send email: fromName '{}', fromLoginName '{}', replyEmail '{}', subject '{}', message '{}'",
                    e, fromName, fromLoginName, replyEmail, subject, message);
            facesMessages.addGlobal(sb.toString());
        }
    }
}
