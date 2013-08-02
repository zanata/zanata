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
package org.zanata.tmx;

import java.io.InputStream;

import javax.persistence.EntityExistsException;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import lombok.Cleanup;
import nu.xom.Element;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Transactional;
import org.jboss.seam.transaction.Transaction;
import org.zanata.common.util.ElementBuilder;
import org.zanata.model.tm.TransMemory;
import org.zanata.process.MessagesProcessHandle;

import com.google.common.collect.Lists;

/**
 * Parses TMX input.
 *
 * @author Carlos Munoz <a href="mailto:camunoz@redhat.com">camunoz@redhat.com</a>
 */
@Name("tmxParser")
@AutoCreate
public class TMXParser
{
   // Batch size to commit in a new transaction for long files
   private static final int BATCH_SIZE = 100;

   @In(required = false)
   private MessagesProcessHandle asynchronousProcessHandle;
   @In
   private Session session;
   @In
   private TransMemoryAdapter transMemoryAdapter;

   @Transactional
   public void parseAndSaveTMX(InputStream input, TransMemory transMemory) throws XMLStreamException, SecurityException, IllegalStateException, RollbackException, HeuristicMixedException, HeuristicRollbackException, SystemException, NotSupportedException
   {
      try
      {
         session.setFlushMode(FlushMode.MANUAL);
         session.setCacheMode(CacheMode.IGNORE);
         XMLInputFactory factory = XMLInputFactory.newInstance();
         factory.setProperty(XMLInputFactory.IS_VALIDATING, false);
         factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
         @Cleanup
         XMLEventReader reader = factory.createXMLEventReader(input);

         QName tu = new QName("tu");
         QName header = new QName("header");
         int handledTUs = 0;

         while (reader.hasNext())
         {
            if( handledTUs > 0 && handledTUs % BATCH_SIZE == 0 )
            {
               commitBatch(handledTUs, true);
            }
            XMLEvent event = reader.nextEvent();
            if (event.isStartElement())
            {
               StartElement elem = event.asStartElement();
               if (elem.getName().equals(tu))
               {
                  Element tuElem = ElementBuilder.buildElement(elem, reader);
                  transMemoryAdapter.processTransUnit(transMemory, tuElem);
                  handledTUs++;
               }
               else if (elem.getName().equals(header))
               {
                  Element headerElem = ElementBuilder.buildElement(elem, reader);
                  transMemoryAdapter.processHeader(transMemory, headerElem);
               }
            }
         }
         commitBatch(handledTUs, false);
      }
      catch (EntityExistsException e)
      {
         String msg = "Possible duplicate TU (duplicate tuid or duplicate" +
               "src content without tuid)";
         throw new EntityExistsException(msg, e);
      }
   }

   private void commitBatch(int numProcessed, boolean beginTransaction) throws NotSupportedException, SystemException, SecurityException, IllegalStateException, RollbackException, HeuristicMixedException, HeuristicRollbackException
   {
      session.flush();
      session.clear();
      Transaction.instance().commit();
      updateProgress(numProcessed);
      if (beginTransaction)
      {
         Transaction.instance().begin();
      }
   }

   private void updateProgress(int numProcessed)
   {
      if( asynchronousProcessHandle != null )
      {
         // TODO Piggybacking on the messages field. We should move to {@link java.util.concurrent.Future}
         // or implement a specific TMX Import handler.
         asynchronousProcessHandle.setMessages(Lists.newArrayList("Processed Entries: " + numProcessed));
      }
   }

}
