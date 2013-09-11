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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.EntityManager;

import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.datamodel.DataModel;
import org.jboss.seam.annotations.datamodel.DataModelSelection;
import org.jboss.seam.core.Conversation;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.security.management.JpaIdentityStore;
import org.zanata.dao.AccountDAO;
import org.zanata.dao.CredentialsDAO;
import org.zanata.model.HAccount;
import org.zanata.model.security.HCredentials;
import org.zanata.model.security.HOpenIdCredentials;
import org.zanata.security.AuthenticationManager;
import org.zanata.security.openid.FedoraOpenIdProvider;
import org.zanata.security.openid.GoogleOpenIdProvider;
import org.zanata.security.openid.MyOpenIdProvider;
import org.zanata.security.openid.OpenIdAuthCallback;
import org.zanata.security.openid.OpenIdAuthenticationResult;
import org.zanata.security.openid.OpenIdProviderType;
import org.zanata.security.openid.YahooOpenIdProvider;

import static org.jboss.seam.international.StatusMessage.Severity.ERROR;
import static org.jboss.seam.international.StatusMessage.Severity.INFO;

/**
 * @author Carlos Munoz <a href="mailto:camunoz@redhat.com">camunoz@redhat.com</a>
 */
@Name("credentialsAction")
@Scope(ScopeType.PAGE)
public class CredentialsAction implements Serializable
{
   private static final long serialVersionUID = 1L;

   @In(value = JpaIdentityStore.AUTHENTICATED_USER)
   private HAccount authenticatedAccount;

   @In
   private AccountDAO accountDAO;

   @In
   private AuthenticationManager authenticationManager;

   @DataModel
   private List<HCredentials> userCredentials;

   @DataModelSelection
   private HCredentials selectedCredentials;


   public void loadUserCredentials()
   {
      // Get the list of credentials from the database
      HAccount account = accountDAO.findById( authenticatedAccount.getId(), false );
      userCredentials = new ArrayList<HCredentials>( account.getCredentials() );
   }

   public List<HCredentials> getUserCredentials()
   {
      return userCredentials;
   }

   public void remove()
   {
      HAccount account = accountDAO.findById( authenticatedAccount.getId(), false );
      account.getCredentials().remove( selectedCredentials );
      userCredentials = new ArrayList<HCredentials>( account.getCredentials() ); // Reload the credentials
      accountDAO.makePersistent( account );
   }

   public void cancel()
   {
      // See pages.xml
   }

   public void verifyCredentials(String providerTypeStr)
   {
      OpenIdProviderType providerType = OpenIdProviderType.valueOf(providerTypeStr);
      HOpenIdCredentials newCreds = new HOpenIdCredentials();
      newCreds.setAccount(authenticatedAccount);

      authenticationManager.openIdAuthenticate(
            providerType, new CredentialsCreationCallback(newCreds) );
   }

   public boolean isGoogleOpenId( String openId )
   {
      return new GoogleOpenIdProvider().accepts( openId );
   }

   public boolean isFedoraOpenId( String openId )
   {
      return new FedoraOpenIdProvider().accepts( openId );
   }

   public boolean isMyOpenId( String openId )
   {
      return new MyOpenIdProvider().accepts(openId);
   }

   public boolean isYahooOpenId( String openId )
   {
      return new YahooOpenIdProvider().accepts(openId);
   }

   public boolean isGenericOpenId( String openId )
   {
      return !this.isFedoraOpenId(openId) && !this.isGoogleOpenId(openId) && !this.isMyOpenId(openId)
            && !this.isYahooOpenId(openId);
   }

   /**
    * Callback for credential creation.
    */
   private static class CredentialsCreationCallback implements OpenIdAuthCallback, Serializable
   {
      private static final long serialVersionUID = 1L;
      private HCredentials newCredentials;

      private CredentialsCreationCallback(HCredentials newCredentials)
      {
         this.newCredentials = newCredentials;
      }

      @Override
      public void afterOpenIdAuth(OpenIdAuthenticationResult result)
      {
         // Save the credentials after a successful authentication
         if( result.isAuthenticated() )
         {
            this.newCredentials.setUser(result.getAuthenticatedId());
            this.newCredentials.setEmail( result.getEmail() );
            // NB: Seam component injection won't work on callbacks
            EntityManager em = (EntityManager)Component.getInstance("entityManager");
            CredentialsDAO credentialsDAO = (CredentialsDAO)Component.getInstance(CredentialsDAO.class);

            Conversation.instance().begin(true, false); // (To retain messages)
            FacesMessages.instance().clear();

            if( credentialsDAO.findByUser( result.getAuthenticatedId() ) != null )
            {
               FacesMessages.instance().add(ERROR, "jsf.identities.invalid.Duplicate", null,
                     "Duplicate identity", "This Identity is already in use.");
            }
            else
            {
               em.persist(this.newCredentials);
               FacesMessages.instance().add(INFO, "jsf.identities.IdentityAdded", null,
                     "Identity Added", "Your new identity has been added to this account.");
            }
         }
      }

      @Override
      public String getRedirectToUrl()
      {
         return "/profile/identities.seam?cid=" + Conversation.instance().getId(); // keep the same conversation
      }
   }
}
