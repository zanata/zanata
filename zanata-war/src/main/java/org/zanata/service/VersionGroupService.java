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
package org.zanata.service;

import java.util.List;

import org.zanata.model.HIterationGroup;
import org.zanata.model.HPerson;
import org.zanata.model.HProjectIteration;

/**
 * @author Alex Eng <a href="mailto:aeng@redhat.com">aeng@redhat.com</a>
 */
public interface VersionGroupService
{
   List<HIterationGroup> getAllActiveVersionGroupsOrIsMaintainer();

   HIterationGroup getBySlug(String slug);

   List<HProjectIteration> searchLikeSlugOrProjectSlug(String searchTerm);

   List<HIterationGroup> searchLikeSlugAndName(String searchTerm);

   List<HPerson> getMaintainerBySlug(String slug);

   void makePersistent(HIterationGroup iterationGroup);

   void flush();

   boolean joinVersionGroup(String slug, Long projectIterationId);

   boolean leaveVersionGroup(String slug, Long projectIterationId);

   HProjectIteration getProjectIterationBySlug(String projectSlug, String iterationSlug);

   boolean isVersionInGroup(String groupSlug, Long projectIterationId);

   boolean isGroupInVersion(String groupSlug, Long id);

   public final class SelectableHProject
   {
      private HProjectIteration projectIteration;
      private boolean selected;

      public SelectableHProject(HProjectIteration projectIteration, boolean selected)
      {
         this.projectIteration = projectIteration;
         this.selected = selected;
      }

      public HProjectIteration getProjectIteration()
      {
         return projectIteration;
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
