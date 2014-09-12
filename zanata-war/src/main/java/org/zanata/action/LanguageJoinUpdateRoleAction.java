/*
 * Copyright 2013, Red Hat, Inc. and individual contributors as indicated by the
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

import lombok.Getter;
import lombok.Setter;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.security.management.JpaIdentityStore;
import org.zanata.common.LocaleId;
import org.zanata.dao.LocaleMemberDAO;
import org.zanata.i18n.Messages;
import org.zanata.model.HAccount;
import org.zanata.model.HLocaleMember;

/**
 * @author Alex Eng <a href="mailto:aeng@redhat.com">aeng@redhat.com</a>
 */

@AutoCreate
@Name("languageJoinUpdateRoleAction")
@Scope(ScopeType.PAGE)
public class LanguageJoinUpdateRoleAction implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final String EMAIL_TYPE_REQUEST_JOIN =
            "request_join_language";
    private static final String EMAIL_TYPE_REQUEST_ROLE =
            "request_role_language";

    @In
    private SendEmailAction sendEmail;
    @In
    private Messages msgs;

    @In
    private LocaleMemberDAO localeMemberDAO;

    @In(required = false, value = JpaIdentityStore.AUTHENTICATED_USER)
    private HAccount authenticatedAccount;

    @Getter
    @Setter
    private Boolean requestAsTranslator;

    @Getter
    @Setter
    private Boolean requestAsReviewer;

    @Getter
    @Setter
    private Boolean requestAsCoordinator;

    @Getter
    @Setter
    private String emailType;

    @Setter
    @Getter
    private String language;

    private String title;

    private String subject;

    public boolean hasRoleRequest() {
        return requestAsTranslator || requestAsReviewer || requestAsCoordinator;
    }

    public String getSubject() {
        if (emailType.equals(EMAIL_TYPE_REQUEST_JOIN)) {
            subject =
                    msgs.format("jsf.email.joinrequest.Subject",
                            sendEmail.getFromLoginName(),
                            sendEmail.getLocale().getLocaleId().getId());
        } else {
            subject =
                    msgs.format("jsf.email.rolerequest.Subject",
                            sendEmail.getFromLoginName(),
                            sendEmail.getLocale().getLocaleId().getId());
        }
        return subject;
    }

    public String getTitle() {
        if (emailType.equals(EMAIL_TYPE_REQUEST_JOIN)) {
            title =
                    msgs
                            .format("jsf.RequestToJoinLanguageTeamTitle",
                                    sendEmail.getLocale().getLocaleId().getId());
        } else {
            title =
                    msgs
                            .format("jsf.RequestRoleLanguageTeamTitle",
                                    sendEmail.getLocale().getLocaleId().getId());
        }
        return title;
    }

    public boolean requestingTranslator() {
        return requestAsTranslator != null && requestAsTranslator && !isTranslator();
    }

    public boolean isTranslator() {
        HLocaleMember member = getLocaleMember();
        if (member != null) {
            return getLocaleMember().isTranslator();
        }
        return false;
    }

    public boolean requestingReviewer() {
        return requestAsReviewer != null && requestAsReviewer && !isReviewer();
    }

    public boolean isReviewer() {
        HLocaleMember member = getLocaleMember();
        if (member != null) {
            return getLocaleMember().isReviewer();
        }
        return false;
    }

    public boolean requestingCoordinator() {
        return requestAsCoordinator != null && requestAsCoordinator && !isCoordinator();
    }

    public boolean isCoordinator() {
        HLocaleMember member = getLocaleMember();
        if (member != null) {
            return getLocaleMember().isCoordinator();
        }
        return false;
    }

    private HLocaleMember getLocaleMember() {
        return localeMemberDAO.findByPersonAndLocale(authenticatedAccount
                .getPerson().getId(), new LocaleId(language));
    }
}
