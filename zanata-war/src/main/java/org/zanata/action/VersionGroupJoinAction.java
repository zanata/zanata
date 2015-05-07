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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang.StringUtils;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.security.Restrict;
import org.jboss.seam.international.LocaleSelector;
import org.jboss.seam.security.management.JpaIdentityStore;
import org.zanata.common.EntityStatus;
import org.zanata.common.ProjectType;
import org.zanata.dao.ProjectDAO;
import org.zanata.dao.ProjectIterationDAO;
import org.zanata.dao.VersionGroupDAO;
import org.zanata.email.EmailStrategy;
import org.zanata.email.RequestToJoinVersionGroupEmailStrategy;
import org.zanata.model.HAccount;
import org.zanata.model.HPerson;
import org.zanata.model.HProject;
import org.zanata.model.HProjectIteration;
import org.zanata.service.EmailService;
import org.zanata.service.VersionGroupService;

import org.zanata.ui.AbstractAutocomplete;
import org.zanata.ui.faces.FacesMessages;
import org.zanata.webtrans.shared.model.ProjectIterationId;

import com.google.common.collect.Lists;

@AutoCreate
@Name("versionGroupJoinAction")
@Scope(ScopeType.PAGE)
@Slf4j
public class VersionGroupJoinAction extends AbstractAutocomplete<HProject>
        implements Serializable {
    private static final long serialVersionUID = 1L;

    @In
    private VersionGroupService versionGroupServiceImpl;

    @In
    private ProjectDAO projectDAO;

    @In
    private ProjectIterationDAO projectIterationDAO;

    @In
    private VersionGroupDAO versionGroupDAO;

    @In(required = false, value = JpaIdentityStore.AUTHENTICATED_USER)
    private HAccount authenticatedAccount;

    @Getter
    @Setter
    private String slug;

    @Getter
    private String projectSlug;

    @Getter
    private List<SelectableVersion> projectVersions = Lists.newArrayList();

    @In
    private EmailService emailServiceImpl;

    @Getter
    @Setter
    private String message;

    @In("jsfMessages")
    private FacesMessages facesMessages;

    public boolean hasSelectedVersion() {
        if(projectVersions.isEmpty()) {
            return false;
        }
        for (SelectableVersion projectVersion : projectVersions) {
            if (projectVersion.isSelected()) {
                return true;
            }
        }
        return false;
    }

    public String getGroupName() {
        return versionGroupDAO.getBySlug(slug).getName();
    }

    public void bindSelectedVersion(String versionSlug, boolean selected) {
        for (SelectableVersion projectVersion : projectVersions) {
            if (projectVersion.getIterationSlug().equals(versionSlug)) {
                projectVersion.setSelected(selected);
            }
        }
    }

    public List<SelectableVersion> getVersions() {
        if (projectVersions.isEmpty() && StringUtils.isNotEmpty(projectSlug)) {
            List<HProjectIteration> versions =
                    projectIterationDAO.getByProjectSlug(projectSlug,
                        EntityStatus.ACTIVE, EntityStatus.READONLY);
            for (HProjectIteration version : versions) {
                if(!isVersionInGroup(version.getId())) {
                    projectVersions
                            .add(new SelectableVersion(projectSlug, version
                                    .getSlug(), version.getProjectType(), false));
                }
            }
        }
        return projectVersions;
    }

    public boolean isVersionInGroup(Long versionId) {
        return versionGroupServiceImpl.isVersionInGroup(slug, versionId);
    }

    public List<HPerson> getGroupMaintainers() {
        List<HPerson> maintainers = Lists.newArrayList();
        for (HPerson maintainer : versionGroupServiceImpl
            .getMaintainersBySlug(slug)) {
            maintainers.add(maintainer);
        }
        return maintainers;
    }

    @Restrict("#{identity.loggedIn}")
    public void send() {
        if (hasSelectedVersion()) {
            String fromName = authenticatedAccount.getPerson().getName();
            String fromLoginName = authenticatedAccount.getUsername();
            String replyEmail = authenticatedAccount.getPerson().getEmail();

            List<HPerson> maintainers = Lists.newArrayList();
            for (HPerson maintainer : versionGroupServiceImpl
                .getMaintainersBySlug(slug)) {
                maintainers.add(maintainer);
            }

            Collection<ProjectIterationId> projectVersionIds =
                Lists.newArrayList();

            for (VersionGroupJoinAction.SelectableVersion selectedVersion : projectVersions) {
                if (selectedVersion.isSelected()) {
                    projectVersionIds.add(new ProjectIterationId(
                        selectedVersion.getProjectSlug(),
                        selectedVersion.getIterationSlug(),
                        selectedVersion.getProjectType()));
                }
            }

            EmailStrategy strategy =
                new RequestToJoinVersionGroupEmailStrategy(
                    fromLoginName, fromName, replyEmail,
                    getGroupName(), getSlug(),
                    projectVersionIds, message);

            try {
                String msg =
                    emailServiceImpl.sendToVersionGroupMaintainers(
                        getGroupMaintainers(), strategy);
                facesMessages.addGlobal(msg);
                clearFormFields();

            } catch (Exception e) {
                String subject = strategy.getSubject(msgs);

                StringBuilder sb =
                    new StringBuilder()
                        .append("Failed to send email with subject '")
                        .append(subject)
                        .append("' , message '").append(message)
                        .append("'");
                log.error(
                        "Failed to send email: fromName '{}', fromLoginName '{}', replyEmail '{}', subject '{}', message '{}'",
                        e, fromName, fromLoginName, replyEmail,
                        subject, message);
                facesMessages.addGlobal(sb.toString());
            }
        } else {
            facesMessages.addGlobal(msgs.get("jsf.NoProjectVersionSelected"));
        }
    }

    /**
     * This is to reset data when user closes dialog or after sending email.
     * See version-group/request_join_modal.xhtml#cancelJoinGroupEmail
     */
    public void clearFormFields() {
        projectSlug = "";
        projectVersions.clear();
        setQuery("");
    }

    @Override
    public List<HProject> suggest() {
        if(authenticatedAccount == null || StringUtils.isEmpty(getQuery())) {
            return Collections.emptyList();
        }
        return projectDAO.getProjectsForMaintainer(
                authenticatedAccount.getPerson(), getQuery(), 0,
                Integer.MAX_VALUE);
    }

    @Override
    public void onSelectItemAction() {
        projectSlug = getSelectedItem();
        // Need to clear the all the versions displayed in dialog from previous
        // selected project when user select a new project from search
        projectVersions.clear();
    }

    public final class SelectableVersion extends ProjectIterationId {
        @Getter
        @Setter
        private boolean selected;

        public SelectableVersion(String projectSlug, String versionSlug,
                ProjectType projectType, boolean selected) {
            super(projectSlug, versionSlug, projectType);
            this.selected = selected;
        }
    }
}
