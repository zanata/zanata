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
package org.zanata.service.impl;

import java.util.Set;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Transactional;
import org.jboss.seam.core.Events;
import org.zanata.ApplicationConfiguration;
import org.zanata.dao.DocumentDAO;
import org.zanata.dao.ProjectIterationDAO;
import org.zanata.events.DocumentUploadedEvent;
import org.zanata.lock.Lock;
import org.zanata.model.HDocument;
import org.zanata.model.HLocale;
import org.zanata.model.HProjectIteration;
import org.zanata.rest.dto.resource.Resource;
import org.zanata.rest.service.ResourceUtils;
import org.zanata.security.ZanataIdentity;
import org.zanata.service.CopyTransService;
import org.zanata.service.DocumentService;
import org.zanata.service.LocaleService;

/**
 * Default implementation of the {@link DocumentService} business service interface.
 *
 * @author Carlos Munoz <a href="mailto:camunoz@redhat.com">camunoz@redhat.com</a>
 */
@Name("documentServiceImpl")
@Scope(ScopeType.STATELESS)
public class DocumentServiceImpl implements DocumentService
{
   @In
   private ZanataIdentity identity;

   @In
   private ProjectIterationDAO projectIterationDAO;

   @In
   private DocumentDAO documentDAO;

   @In
   private LocaleService localeServiceImpl;

   @In
   private CopyTransService copyTransServiceImpl;

   @In
   private LockManagerServiceImpl lockManagerServiceImpl;

   @In
   private ResourceUtils resourceUtils;

   @In
   private ApplicationConfiguration applicationConfiguration;


   @Override
   @Transactional
   public HDocument saveDocument( String projectSlug, String iterationSlug, Resource sourceDoc,
                                  Set<String> extensions, boolean copyTrans, boolean lock)
   {
      Lock docLock = null;
      if( lock )
      {
         // Lock this document for push
         docLock = new Lock(projectSlug, iterationSlug, sourceDoc.getName(), "push");
         lockManagerServiceImpl.attain(docLock);
      }

      try
      {
         return this.saveDocument(projectSlug, iterationSlug, sourceDoc, extensions, copyTrans);
      }
      finally
      {
         if(lock)
         {
            lockManagerServiceImpl.release(docLock);
         }
      }
   }

   @Override
   @Transactional
   public HDocument saveDocument( String projectSlug, String iterationSlug, Resource sourceDoc,
                                  Set<String> extensions, boolean copyTrans)
   {
      // Only active iterations allow the addition of a document
      HProjectIteration hProjectIteration = projectIterationDAO.getBySlug(projectSlug, iterationSlug);

      // Check permission
      identity.checkPermission(hProjectIteration, "import-template");

      String docId = sourceDoc.getName();

      HDocument document = documentDAO.getByDocIdAndIteration(hProjectIteration, docId);
      HLocale hLocale = this.localeServiceImpl.validateSourceLocale( sourceDoc.getLang() );

      boolean changed = false;
      int nextDocRev;
      if (document == null)
      { // must be a create operation
         nextDocRev = 1;
         changed = true;
         // TODO check that entity name matches id parameter
         document = new HDocument(sourceDoc.getName(), sourceDoc.getContentType(), hLocale);
         document.setProjectIteration(hProjectIteration);
         hProjectIteration.getDocuments().put(docId, document);
         document = documentDAO.makePersistent(document);
      }
      else if (document.isObsolete())
      { // must also be a create operation
         nextDocRev = document.getRevision() + 1;
         changed = true;
         document.setObsolete(false);
         // not sure if this is needed
         hProjectIteration.getDocuments().put(docId, document);
      }
      else
      { // must be an update operation
         nextDocRev = document.getRevision() + 1;
      }

      changed |= resourceUtils.transferFromResource(sourceDoc, document, extensions, hLocale, nextDocRev);
      documentDAO.flush();
      
      if(changed && Events.exists())
      {
         Events.instance().raiseTransactionSuccessEvent(
               DocumentUploadedEvent.EVENT_NAME,
               new DocumentUploadedEvent(document.getId(), true, hLocale.getLocaleId()
         ));
      }

      if (copyTrans && nextDocRev == 1)
      {
         copyTranslations(document);
      }

      return document;
   }

   @Override
   @Transactional
   public void makeObsolete(HDocument document)
   {
      // Simply make it obsolete. This method is here in case this logic is expanded.
      document.setObsolete(true);
      documentDAO.makePersistent(document);
      documentDAO.flush();
   }

   /**
    * Invoke the copy trans function for a document.
    *
    * @param document The document to copy translations into.
    */
   private void copyTranslations(HDocument document)
   {
      if (applicationConfiguration.getEnableCopyTrans())
      {
         copyTransServiceImpl.copyTransForDocument(document);
      }
   }
}
