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
import java.util.Collections;
import java.util.List;
import java.util.Map;

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
import org.zanata.webtrans.shared.model.ValidationId;
import org.zanata.webtrans.shared.validation.ValidationFactory;

import com.google.common.collect.Maps;

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

   @Getter
   @Setter
   private String versionSlug;

   @Getter
   @Setter
   private String projectSlug;

   private Map<ValidationId, ValidationAction> availableValidations = Maps.newHashMap();

   public List<ValidationAction> getValidationList()
   {
      if (availableValidations.isEmpty())
      {
         availableValidations.clear();
         Collection<ValidationAction> validationList = validationServiceImpl.getValidationAction(projectSlug,
               versionSlug);
         for (ValidationAction validationAction : validationList)
         {
            availableValidations.put(validationAction.getId(), validationAction);
         }
      }

      List<ValidationAction> sortedList = new ArrayList<ValidationAction>(availableValidations.values());
      Collections.sort(sortedList, ValidationFactory.ValidationActionComparator);
      return sortedList;
   }

   /**
    * If this action is enabled(Warning or Error), then it's exclusive validation will be turn off
    * @param selectedValidationAction
    */
   public void checkExclusive(ValidationAction selectedValidationAction)
   {
      if (selectedValidationAction.getState() != State.Off)
      {
         for (ValidationAction exclusiveValAction : selectedValidationAction.getExclusiveValidations())
         {
            if (availableValidations.containsKey(exclusiveValAction.getId()))
            {
               availableValidations.get(exclusiveValAction.getId()).setState(State.Off);
            }
         }
      }
   }

   public List<State> getValidationStates()
   {
      return Arrays.asList(ValidationAction.State.values());
   }

   @Out(required = false)
   public Collection<ValidationAction> getVersionCustomizedValidations()
   {
      return availableValidations.values();
   }
}
