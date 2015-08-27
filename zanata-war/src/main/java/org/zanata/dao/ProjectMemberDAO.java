/*
 * Copyright 2015, Red Hat, Inc. and individual contributors
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
package org.zanata.dao;

import org.hibernate.Query;
import org.hibernate.Session;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.zanata.model.HPerson;
import org.zanata.model.HProject;
import org.zanata.model.HProjectMember;
import org.zanata.model.ProjectRole;

import java.util.HashSet;
import java.util.Set;


/**
 * Provides methods to access data related to membership in a project.
 */
@Name("projectMemberDAO")
@AutoCreate
@Scope(ScopeType.STATELESS)
public class ProjectMemberDAO
        extends AbstractDAOImpl<HProjectMember, HProjectMember.HProjectMemberPK> {

    public ProjectMemberDAO() {
        super(HProjectMember.class);
    }

    public ProjectMemberDAO(Session session) {
        super(HProjectMember.class, session);
    }

    /**
     * Retrieve all of a person's roles in a project.
     */
    public Set<ProjectRole> getRolesInProject(HPerson person, HProject project) {
        Query query = getSession().createQuery(
                "from HProjectMember as m where m.person = :person " +
                        "and m.project = :project")
                .setParameter("person", person)
                .setParameter("project", project)
                .setComment("ProjectMemberDAO.getRolesInProject");
        return new HashSet<>(query.list());
    }

    /**
     * Check whether a person has a specified role in a project.
     *
     * @return true if the given person has the given role in the given project.
     */
    public boolean hasProjectRole(HPerson person, HProject project, ProjectRole role) {
        Query query = getSession().createQuery(
                "select count(m) from HProjectMember as m " +
                        "where m.person = :person " +
                        "and m.project = :project " +
                        "and m.role = :role")
                .setParameter("person", person)
                .setParameter("project", project)
                .setParameter("role", role)
                .setComment("ProjectMemberDAO.hasProjectRole");
        return ((Long) query.uniqueResult()) > 0;
    }

}
