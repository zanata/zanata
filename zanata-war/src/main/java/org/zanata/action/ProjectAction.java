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

import javax.faces.model.DataModel;

import javax.inject.Inject;
import javax.inject.Named;
import org.zanata.dao.ProjectDAO;

@Named("projectAction")
@javax.faces.bean.ViewScoped
public class ProjectAction implements Serializable {
    private static final long serialVersionUID = 1L;

    private boolean showActive = true;
    private boolean showReadOnly = true;
    private final boolean showObsolete = false;

    private ProjectPagedListDataModel projectPagedListDataModel =
            new ProjectPagedListDataModel(!showActive, !showReadOnly,
                    !showObsolete);

    @Inject
    private ProjectDAO projectDAO;

    public boolean getEmpty() {
        return projectDAO.getFilterProjectSize(false, false, false) == 0;
    }

    public DataModel getProjectPagedListDataModel() {
        return projectPagedListDataModel;
    }

    public boolean isShowActive() {
        return showActive;
    }

    public void setShowActive(boolean showActive) {
        projectPagedListDataModel.setFilterActive(!showActive);
        this.showActive = showActive;
    }

    public boolean isShowReadOnly() {
        return showReadOnly;
    }

    public void setShowReadOnly(boolean showReadOnly) {
        projectPagedListDataModel.setFilterReadOnly(!showReadOnly);
        this.showReadOnly = showReadOnly;
    }

}
