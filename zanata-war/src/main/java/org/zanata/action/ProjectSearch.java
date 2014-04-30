package org.zanata.action;

import java.io.Serializable;
import java.util.List;

import javax.faces.model.DataModel;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.apache.commons.lang.StringUtils;
import org.apache.lucene.queryParser.ParseException;
import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.zanata.dao.ProjectDAO;
import org.zanata.model.HProject;
import org.zanata.security.ZanataIdentity;

import com.google.common.collect.Lists;
import org.zanata.ui.AbstractAutocomplete;

@Name("projectSearch")
@Scope(ScopeType.CONVERSATION)
@AutoCreate
public class ProjectSearch implements Serializable {

    private static final long serialVersionUID = 1L;

    private final static int DEFAULT_PAGE_SIZE = 30;

    @Getter
    @Setter
    private int scrollerPage = 1;

    @In
    private ZanataIdentity identity;

    @Getter
    private final AbstractAutocomplete<SearchResult> projectAutocomplete =
            new AbstractAutocomplete<SearchResult>() {
                private ProjectDAO projectDAO = (ProjectDAO) Component
                        .getInstance(ProjectDAO.class);

                @Override
                public List<SearchResult> suggest() {
                    List<SearchResult> result = Lists.newArrayList();
                    if (StringUtils.isEmpty(getQuery())) {
                        return result;
                    }
                    try {
                        List<HProject> searchResult =
                                projectDAO.searchProjects(
                                        getQuery(),
                                        INITIAL_RESULT_COUNT,
                                        0,
                                        ZanataIdentity.instance()
                                                .hasPermission("HProject",
                                                        "view-obsolete"));

                        for (HProject project : searchResult) {
                            result.add(new SearchResult(project));
                        }
                        result.add(new SearchResult());
                        return result;
                    } catch (ParseException pe) {
                        return result;
                    }
                }

                @Override
                public void onSelectItemAction() {
                    // nothing here
                }

                @Override
                public void setQuery(String query) {
                    queryProjectPagedListDataModel.setQuery(query);
                    super.setQuery(query);
                }
            };

    private QueryProjectPagedListDataModel queryProjectPagedListDataModel =
            new QueryProjectPagedListDataModel(DEFAULT_PAGE_SIZE);

    // Count of result to be return as part of autocomplete
    private final static int INITIAL_RESULT_COUNT = 5;

    public int getPageSize() {
        return queryProjectPagedListDataModel.getPageSize();
    }

    public DataModel getProjectPagedListDataModel() {
        queryProjectPagedListDataModel.setIncludeObsolete(identity
                .hasPermission("HProject", "view-obsolete"));
        return queryProjectPagedListDataModel;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    public class SearchResult {
        @Getter
        private HProject project;

        public boolean isProjectNull() {
            return project == null;
        }
    }
}
