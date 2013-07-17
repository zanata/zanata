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

import org.hibernate.validator.constraints.Email;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.LocaleSelector;
import org.jboss.seam.log.Log;
import org.jboss.seam.security.management.JpaIdentityStore;
import org.zanata.common.LocaleId;
import org.zanata.model.HAccount;
import org.zanata.model.HLocale;
import org.zanata.model.HLocaleMember;
import org.zanata.model.HPerson;
import org.zanata.service.EmailService;
import org.zanata.service.LocaleService;

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
public class SendEmailAction implements Serializable
{
   private static final long serialVersionUID = 1L;

   private static final String EMAIL_TYPE_CONTACT_ADMIN = "contact_admin";
   private static final String EMAIL_TYPE_CONTACT_COORDINATOR = "contact_coordinator";
   private static final String EMAIL_TYPE_REQUEST_JOIN = "request_join_language";
   private static final String EMAIL_TYPE_REQUEST_ROLE = "request_role_language";
   private static final String EMAIL_TYPE_REQUEST_TO_JOIN_GROUP = "request_to_join_group";

   @In
   private EmailService emailServiceImpl;

   @In(required = true, value = JpaIdentityStore.AUTHENTICATED_USER)
   private HAccount authenticatedAccount;

   @In
   private LocaleService localeServiceImpl;

   @In
   private LocaleSelector localeSelector;

   @Logger
   private Log log;

   private String fromName;
   private String fromLoginName;
   private String replyEmail;
   private String subject;
   private String message;
   private String emailType;

   private String language;
   private HLocale locale;

   private List<HPerson> groupMaintainers;

   @Create
   public void onCreate()
   {
      if (authenticatedAccount == null)
      {
         log.error("SendEmailAction failed to load authenticated account");
         return;
      }
      fromName = authenticatedAccount.getPerson().getName();
      fromLoginName = authenticatedAccount.getUsername();
      replyEmail = authenticatedAccount.getPerson().getEmail();

      subject = "";
      message = "";
   }

   public String getFromName()
   {
      return fromName;
   }

   public void setFromName(String name)
   {
      fromName = name;
   }

   public String getFromLoginName()
   {
      return fromLoginName;
   }

   public void setFromLoginName(String fromLoginName)
   {
      this.fromLoginName = fromLoginName;
   }

   @Email
   public String getReplyEmail()
   {
      return replyEmail;
   }

   @Email
   public void setReplyEmail(String replyEmail)
   {
      this.replyEmail = replyEmail;
   }

   public String getSubject()
   {
      return subject;
   }

   public void setSubject(String subject)
   {
      this.subject = subject;
   }

   public String getHtmlMessage()
   {
      return message;
   }

   public void setHtmlMessage(String encodedMessage)
   {
      this.message = encodedMessage;
   }

   public String getEmailType()
   {
      return emailType;
   }

   public void setEmailType(String emailType)
   {
      this.emailType = emailType;
   }

   public String getLanguage()
   {
      return language;
   }

   public void setLanguage(String language)
   {
      this.language = language;
      locale = localeServiceImpl.getByLocaleId(new LocaleId(language));
   }

   public HLocale getLocale()
   {
      return locale;
   }

   private List<HPerson> getCoordinators()
   {
      List<HPerson> coordinators = new ArrayList<HPerson>();
      
      for (HLocaleMember member : locale.getMembers())
      {
         if (member.isCoordinator())
         {
            coordinators.add(member.getPerson());
         }
      }
      return coordinators;
   }

   /**
    * Sends the email by rendering an appropriate email template with the values
    * in this bean.
    * 
    * @return a view to redirect to. This should be replaced with configuration
    *         in pages.xml
    */
   public String send()
   {
      Locale pervLocale = localeSelector.getLocale();
      localeSelector.setLocale(new Locale("en"));

      try
      {
         if (emailType.equals(EMAIL_TYPE_CONTACT_ADMIN))
         {
            String msg = emailServiceImpl.sendToAdminEmails(EmailService.ADMIN_EMAIL_TEMPLATE, fromName, fromLoginName, replyEmail, subject, message);
            FacesMessages.instance().add(msg);
            return "success";
         }
         else if (emailType.equals(EMAIL_TYPE_CONTACT_COORDINATOR))
         {
            String msg = emailServiceImpl.sendToLanguageCoordinators(EmailService.COORDINATOR_EMAIL_TEMPLATE, getCoordinators(), fromName, fromLoginName, replyEmail, subject, message, language);
            FacesMessages.instance().add(msg);
            return "success";
         }
         else if (emailType.equals(EMAIL_TYPE_REQUEST_JOIN))
         {
            String msg = emailServiceImpl.sendToLanguageCoordinators(EmailService.REQUEST_TO_JOIN_EMAIL_TEMPLATE, getCoordinators(), fromName, fromLoginName, replyEmail, subject, message, language);
            FacesMessages.instance().add(msg);
            return "success";
         }
         else if (emailType.equals(EMAIL_TYPE_REQUEST_ROLE))
         {
            String msg = emailServiceImpl.sendToLanguageCoordinators(EmailService.REQUEST_ROLE_EMAIL_TEMPLATE, getCoordinators(), fromName, fromLoginName, replyEmail, subject, message, language);
            FacesMessages.instance().add(msg);
            return "success";
         }
         else if (emailType.equals(EMAIL_TYPE_REQUEST_TO_JOIN_GROUP))
         {
            String msg = emailServiceImpl.sendToVersionGroupMaintainer(EmailService.REQUEST_TO_JOIN_GROUP_EMAIL_TEMPLATE, groupMaintainers, fromName, fromLoginName, replyEmail, subject, message);
            FacesMessages.instance().add(msg);
            return "success";
         }
         else
         {
            throw new Exception("Invalid email type: " + emailType);
         }
      }
      catch (Exception e)
      {
         FacesMessages.instance().add("There was a problem sending the message: " + e.getMessage());
         log.error("Failed to send email: fromName '{0}', fromLoginName '{1}', replyEmail '{2}', subject '{3}', message '{4}'", e, fromName, fromLoginName, replyEmail, subject, message);
         return "failure";
      }
      finally
      {
         localeSelector.setLocale(pervLocale);
      }
   }

   /**
    * @return string 'canceled'
    */
   public String cancel()
   {
      log.info("Canceled sending email: fromName '{0}', fromLoginName '{1}', replyEmail '{2}', subject '{3}', message '{4}'", fromName, fromLoginName, replyEmail, subject, message);
      FacesMessages.instance().add("Sending message canceled");
      return "canceled";
   }

   public String sendToVersionGroupMaintainer(List<HPerson> maintainers)
   {
      groupMaintainers = maintainers;
      return send();
   }
}
