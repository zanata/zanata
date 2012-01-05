package org.zanata.dao;

import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.zanata.model.HPerson;
import org.zanata.model.HProject;
import org.zanata.model.type.StatusType;

@Name("projectDAO")
@AutoCreate
@Scope(ScopeType.STATELESS)
public class ProjectDAO extends AbstractDAOImpl<HProject, Long>
{
   private static final String ORDERBY_TIMESTAMP = "creationDate";

   public ProjectDAO()
   {
      super(HProject.class);
   }

   public ProjectDAO(Session session)
   {
      super(HProject.class, session);
   }

   public HProject getBySlug(String slug)
   {
      return (HProject) getSession().createCriteria(HProject.class).add(Restrictions.naturalId().set("slug", slug)).setCacheable(true).setComment("ProjectDAO.getBySlug").uniqueResult();
   }

   @SuppressWarnings("unchecked")
   public List<HPerson> getProjectMaintainerBySlug(String slug)
   {
      Query query = getSession().createQuery("select p.maintainers from HProject as p where p.slug = :slug").setParameter("slug", slug);
      return query.list();
   }

   @SuppressWarnings("unchecked")
   public List<HProject> getOffsetListByCreateDate(int offset, int count)
   {
      return getSession().createCriteria(HProject.class).addOrder(Order.desc(ORDERBY_TIMESTAMP)).setMaxResults(count).setFirstResult(offset).setComment("ProjectDAO.getAllProjectOffsetListByCreateDate").list();
   }

   @SuppressWarnings("unchecked")
   public List<HProject> getFilteredOffsetListByCreateDate(int offset, int count)
   {
      return getSession().createCriteria(HProject.class).addOrder(Order.desc(ORDERBY_TIMESTAMP)).add(Restrictions.ne("status", StatusType.Obsolete)).setMaxResults(count).setFirstResult(offset).setComment("ProjectDAO.getOffsetListByCreateDate").list();
   }

   public int getFilteredProjectSize()
   {
      Long totalCount = (Long) getSession().createQuery("select count(*) from HProject as p where p.status <> :status").setParameter("status", StatusType.Obsolete).uniqueResult();
      if (totalCount == null)
         return 0;
      return totalCount.intValue();
   }

   public int getProjectSize()
   {
      Long totalCount = (Long) getSession().createQuery("select count(*) from HProject").uniqueResult();
      if (totalCount == null)
         return 0;
      return totalCount.intValue();
   }
}
