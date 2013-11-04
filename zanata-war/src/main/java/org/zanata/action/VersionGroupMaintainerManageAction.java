package org.zanata.action;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.datamodel.DataModel;
import org.jboss.seam.annotations.datamodel.DataModelSelection;
import org.jboss.seam.annotations.security.Restrict;
import org.jboss.seam.faces.FacesMessages;
import org.zanata.dao.AccountDAO;
import org.zanata.model.HAccount;
import org.zanata.model.HIterationGroup;
import org.zanata.model.HPerson;
import org.zanata.service.VersionGroupService;

@Name("versionGroupMaintainerManageAction")
@Scope(ScopeType.PAGE)
@Slf4j
public class VersionGroupMaintainerManageAction implements Serializable {
    private static final long serialVersionUID = 1L;

    @DataModel
    List<HPerson> allList;

    @DataModelSelection
    HPerson selectedPerson;

    private String slug;
    private HIterationGroup group;

    @In
    private VersionGroupService versionGroupServiceImpl;

    @In
    AccountDAO accountDAO;

    public void init() {
        allList = versionGroupServiceImpl.getMaintainerBySlug(slug);
    }

    public HPerson getSelectedPerson() {
        return this.selectedPerson;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getSlug() {
        return slug;
    }

    @Restrict("#{s:hasPermission(versionGroupMaintainerManageAction.group,'update')}")
    public
            void deleteMaintainer(HPerson person) {
        log.debug("try to delete maintainer {} from slug {}",
                person.getName(), this.slug);
        HIterationGroup iterationGroup =
                versionGroupServiceImpl.getBySlug(this.slug);
        Set<HPerson> personList = iterationGroup.getMaintainers();
        for (HPerson l : personList) {
            if (l.getEmail().equals(person.getEmail())) {
                log.debug("remove the person");
                iterationGroup.getMaintainers().remove(l);
                break;
            }
        }

        versionGroupServiceImpl.makePersistent(iterationGroup);
        versionGroupServiceImpl.flush();
    }

    @Restrict("#{s:hasPermission(versionGroupMaintainerManageAction.group,'update')}")
    public
            String addMaintainers(String account) {
        HAccount a = accountDAO.getByUsername(account);
        if (a == null) {
            FacesMessages.instance().add("This account does not exist.");
            return "failure";
        } else if (a.isEnabled()) {
            HIterationGroup iterationGroup =
                    versionGroupServiceImpl.getBySlug(this.slug);
            Set<HPerson> personList = iterationGroup.getMaintainers();
            personList.add(a.getPerson());
            versionGroupServiceImpl.makePersistent(iterationGroup);
            versionGroupServiceImpl.flush();
            log.debug("add {} into maintainers", account);
            return "success";
        } else {
            FacesMessages.instance().add("This account is disabled.");
            return "failure";
        }
    }

    public HIterationGroup getGroup() {
        if (group == null) {
            group = versionGroupServiceImpl.getBySlug(slug);
        }

        return group;
    }
}
