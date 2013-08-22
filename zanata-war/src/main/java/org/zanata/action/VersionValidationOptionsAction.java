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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.Getter;
import lombok.Setter;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Out;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.log.Log;
import org.zanata.service.ValidationService;
import org.zanata.webtrans.shared.model.ValidationAction;
import org.zanata.webtrans.shared.model.ValidationAction.State;

@Name("versionValidationOptionsAction")
@Scope(ScopeType.PAGE)
public class VersionValidationOptionsAction implements Serializable
{
   private static final long serialVersionUID = 1L;

   @Logger
   private Log log;

   @In
   private ValidationService validationServiceImpl;

   @In(required = false)
   private ProjectIterationHome projectIterationHome;

   private Map<String, Boolean> selectedValidations;

   @Getter
   @Setter
   private String versionSlug;

   @Getter
   @Setter
   private String projectSlug;

   public List<ValidationAction> getValidationList()
   {
      Collection<ValidationAction> result = validationServiceImpl.getValidationAction(projectSlug, versionSlug);
      return new ArrayList<ValidationAction>(result);
   }
   
   public void checkExclusive(ValidationAction valAction)
   {
      for (ValidationAction exclusiveValAction : valAction.getExclusiveValidations())
      {
         if (selectedValidations.containsKey(exclusiveValAction.getId().name()))
         {
            selectedValidations.put(exclusiveValAction.getId().name(), false);
         }
      }
   }
   
   public List<State> getValidationStates()
   {
      return Arrays.asList(ValidationAction.State.values());
   }

   @Out(required = false)
   public Set<String> getVersionCustomizedValidations()
   {
      Set<String> customizedValidationSet = new HashSet<String>();
      for (Map.Entry<String, Boolean> entry : getSelectedValidations().entrySet())
      {
         if (entry.getValue() == Boolean.TRUE)
         {
            customizedValidationSet.add(entry.getKey());
         }
      }
      return customizedValidationSet;
   }

   public Map<String, Boolean> getSelectedValidations()
   {
      if (selectedValidations == null)
      {
         selectedValidations = new HashMap<String, Boolean>();
         if (!projectIterationHome.getInstance().getCustomizedValidations().isEmpty())
         {
            for (String val : projectIterationHome.getInstance().getCustomizedValidations())
            {
               selectedValidations.put(val, true);
            }
         }
      }
      return selectedValidations;
   }

   public void setSelectedValidations(Map<String, Boolean> selectedValidations, State state)
   {
      log.info("======================" + state);
//      this.selectedValidations = selectedValidations;
   }
}
