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

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Transactional;
import org.jboss.seam.annotations.security.Restrict;
import org.jboss.seam.core.Events;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage.Severity;
import org.jboss.seam.log.Log;
import org.jboss.seam.security.management.JpaIdentityStore;
import org.zanata.common.LocaleId;
import org.zanata.dao.LocaleDAO;
import org.zanata.dao.LocaleMemberDAO;
import org.zanata.dao.PersonDAO;
import org.zanata.model.HAccount;
import org.zanata.model.HLocale;
import org.zanata.model.HLocaleMember;
import org.zanata.model.HPerson;
import org.zanata.service.LanguageTeamService;
import org.zanata.service.LocaleService;

@Name("languageTeamAction")
@Scope(ScopeType.PAGE)
public class LanguageTeamAction implements Serializable
{
   private static final long serialVersionUID = 1L;

   @In
   private LanguageTeamService languageTeamServiceImpl;

   @In
   private LocaleDAO localeDAO;

   @In
   private LocaleMemberDAO localeMemberDAO;

   @In
   private PersonDAO personDAO;

   @In
   private LocaleService localeServiceImpl;

   @In(required = false, value = JpaIdentityStore.AUTHENTICATED_USER)
   HAccount authenticatedAccount;

   @Logger
   Log log;

   private String language;
   private String searchTerm;
   private List<SelectablePerson> searchResults;
   private boolean selectAll = false;

   public String getLanguage()
   {
      return language;
   }

   public void setLanguage(String language)
   {
      this.language = language;
   }

   public String getSearchTerm()
   {
      return searchTerm;
   }

   public void setSearchTerm(String searchTerm)
   {
      this.searchTerm = searchTerm;
   }

   public List<SelectablePerson> getSearchResults()
   {
      if(searchResults == null)
      {
         searchResults = new ArrayList<SelectablePerson>();
      }

      return searchResults;
   }

   public boolean isUserInTeam()
   {
      return authenticatedAccount != null && this.isPersonInTeam( this.authenticatedAccount.getId() );
   }

   public void selectAll()
   {
      for (SelectablePerson selectablePerson : getSearchResults())
      {
         if (!isPersonInTeam(selectablePerson.getPerson().getId()))
         {
            selectablePerson.setSelected(selectAll);
         }
      }
   }

   @Restrict("#{s:hasPermission(languageTeamAction.locale, 'manage-language-team')}")
   public void addSelected()
   {
      for (SelectablePerson selectablePerson : getSearchResults())
      {
         if (selectablePerson.isSelected())
         {
            addTeamMember(selectablePerson.getPerson().getId(), selectablePerson.isTranslator, selectablePerson.isReviewer, selectablePerson.isCoordinator);
         }
      }
   }

   public HLocale getLocale()
   {
      /*
       * Preload the HLocaleMember objects.
       * This line is needed as Hibernate has problems when invoking lazily loaded collections
       * from postLoad entity listener methods. In this case, the drools engine will attempt to
       * access the 'members' collection from inside the security listener's postLoad method to
       * evaluate rules.
       */
      HLocale locale = localeServiceImpl.getByLocaleId(new LocaleId(language));
      if( locale != null )
      {
         locale.getMembers();
      }
      return locale;
   }

   public List<HLocaleMember> getLocaleMembers()
   {
      return localeMemberDAO.findAllByLocale( new LocaleId(language) );
   }

   @Transactional
   @Restrict("#{s:hasRole('admin')}")
   public void joinTribe()
   {
      log.debug("starting join tribe");
      if (authenticatedAccount == null)
      {
         log.error("failed to load auth person");
         return;
      }
      try
      {
         languageTeamServiceImpl.joinOrUpdateRoleInLanguageTeam(this.language, authenticatedAccount.getPerson().getId(), true, true, true);
         Events.instance().raiseEvent("personJoinedTribe");
         log.info("{0} joined tribe {1}", authenticatedAccount.getUsername(), this.language);
         // FIXME use localizable string
         FacesMessages.instance().add("You are now a member of the {0} language team", getLocale().retrieveNativeName());
      }
      catch (Exception e)
      {
         FacesMessages.instance().add(Severity.ERROR, e.getMessage());
      }
   }

   @Transactional
   public void leaveTribe()
   {
      log.debug("starting leave tribe");
      if (authenticatedAccount == null)
      {
         log.error("failed to load auth person");
         return;
      }
      languageTeamServiceImpl.leaveLanguageTeam(this.language, authenticatedAccount.getPerson().getId());
      Events.instance().raiseEvent("personLeftTribe");
      log.info("{0} left tribe {1}", authenticatedAccount.getUsername(), this.language);
      FacesMessages.instance().add("You have left the {0} language team", getLocale().retrieveNativeName());
   }

   @Restrict("#{s:hasPermission(languageTeamAction.locale, 'manage-language-team')}")
   public void saveTeamCoordinator( HLocaleMember member )
   {
      this.localeDAO.makePersistent(getLocale());
      this.localeDAO.flush();
      if( member.isCoordinator() )
      {
         FacesMessages.instance().add("{0} has been made a Team Coordinator", member.getPerson().getAccount().getUsername());
      }
      else
      {
         FacesMessages.instance().add("{0} has been removed from as Team Coordinators", member.getPerson().getAccount().getUsername());
      }
   }

   @Restrict("#{s:hasPermission(languageTeamAction.locale, 'manage-language-team')}")
   public void saveTeamReviewer( HLocaleMember member )
   {
      this.localeDAO.makePersistent(getLocale());
      this.localeDAO.flush();
      if( member.isReviewer() )
      {
         FacesMessages.instance().add("{0} has been made a Team Reviewer", member.getPerson().getAccount().getUsername());
      }
      else
      {
         FacesMessages.instance().add("{0} has been removed from as Team Reviewer", member.getPerson().getAccount().getUsername());
      }
   }

   @Restrict("#{s:hasPermission(languageTeamAction.locale, 'manage-language-team')}")
   public void saveTeamTranslator( HLocaleMember member )
   {
      this.localeDAO.makePersistent(getLocale());
      this.localeDAO.flush();
      if( member.isReviewer() )
      {
         FacesMessages.instance().add("{0} has been made a Team Translator", member.getPerson().getAccount().getUsername());
      }
      else
      {
         FacesMessages.instance().add("{0} has been removed from as Team Translator", member.getPerson().getAccount().getUsername());
      }
   }

   private void addTeamMember( final Long personId, boolean isTranslator, boolean isReviewer, boolean isCoordinator )
   {
      this.languageTeamServiceImpl.joinOrUpdateRoleInLanguageTeam(this.language, personId, isTranslator, isReviewer, isCoordinator);
   }

   @Restrict("#{s:hasPermission(languageTeamAction.locale, 'manage-language-team')}")
   public void removeMembership( HLocaleMember member )
   {
      this.languageTeamServiceImpl.leaveLanguageTeam(this.language, member.getPerson().getId());
   }

   public boolean isPersonInTeam( final Long personId )
   {
      for( HLocaleMember lm : getLocale().getMembers() )
      {
         if( lm.getPerson().getId().equals( personId ) )
         {
            return true;
         }
      }
      return false;
   }

   private HLocaleMember getLocaleMember( final Long personId )
   {
      for( HLocaleMember lm : getLocale().getMembers() )
      {
         if( lm.getPerson().getId().equals( personId ) )
         {
            return lm;
         }
      }
      return null;
   }

   public void searchForTeamMembers()
   {
      getSearchResults().clear();
      List<HPerson> results = this.personDAO.findAllContainingName( this.searchTerm );
      for(HPerson person: results)
      {
         HLocaleMember localeMember = getLocaleMember(person.getId());
         boolean isMember = localeMember == null ? false : true;
         boolean isReviewer = false;
         boolean isTranslator = false;
         boolean isCoordinator = false;
         
         if(isMember)
         {
            isTranslator = localeMember.isTranslator();
            isReviewer = localeMember.isReviewer();
            isCoordinator = localeMember.isCoordinator();
         }
         getSearchResults().add(new SelectablePerson(person, isMember, isTranslator, isReviewer, isCoordinator));
      }
   }

   public boolean isSelectAll()
   {
      return selectAll;
   }

   public void setSelectAll(boolean selectAll)
   {
      this.selectAll = selectAll;
   }

   public final class SelectablePerson
   {
      private HPerson person;
      private boolean selected;

      private boolean isReviewer;
      private boolean isCoordinator;
      private boolean isTranslator;

      public SelectablePerson(HPerson person, boolean selected, boolean isTranslator, boolean isReviewer, boolean isCoordinator)
      {
         this.person = person;
         this.selected = selected;
         this.isReviewer = isReviewer;
         this.isCoordinator = isCoordinator;
         this.isTranslator = isTranslator;
      }

      public boolean isReviewer()
      {
         return isReviewer;
      }

      public void setReviewer(boolean isReviewer)
      {
         this.isReviewer = isReviewer;
      }

      public boolean isCoordinator()
      {
         return isCoordinator;
      }

      public void setCoordinator(boolean isCoordinator)
      {
         this.isCoordinator = isCoordinator;
      }

      public boolean isTranslator()
      {
         return isTranslator;
      }

      public void setTranslator(boolean isTranslator)
      {
         this.isTranslator = isTranslator;
      }

      public HPerson getPerson()
      {
         return person;
      }
      public void setPerson(HPerson person)
      {
         this.person = person;
      }
      public boolean isSelected()
      {
         return selected;
      }
      public void setSelected(boolean selected)
      {
         this.selected = selected;
      }
   }

}
