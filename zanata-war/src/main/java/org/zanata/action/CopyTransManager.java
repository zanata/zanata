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

import com.google.common.collect.MapMaker;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Startup;
import org.jboss.seam.security.Identity;
import org.zanata.model.HCopyTransOptions;
import org.zanata.model.HDocument;
import org.zanata.model.HProjectIteration;
import org.zanata.process.RunnableProcessListener;
import org.zanata.process.CopyTransProcess;
import org.zanata.process.CopyTransProcessHandle;
import org.zanata.service.ProcessManagerService;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Manager Bean that keeps track of manual copy trans being run
 * in the system, to avoid duplicates and to provide asynchronous feedback.
 *
 * @author Carlos Munoz <a href="mailto:camunoz@redhat.com">camunoz@redhat.com</a>
 */
@Name("copyTransManager")
@Scope(ScopeType.APPLICATION)
@Startup
// TODO This class should be merged with the copy trans service (?)
public class CopyTransManager implements Serializable
{
   private static final long serialVersionUID = 1L;

   // Single instance of the process listener
   private final CopyTransProcessListener listenerInstance = new CopyTransProcessListener();

   // Collection of currently running copy trans processes
   private Map<CopyTransProcessKey, CopyTransProcessHandle> currentlyRunning =
         Collections.synchronizedMap( new HashMap<CopyTransProcessKey, CopyTransProcessHandle>() );

   // Collection of recently cancelled copy trans processes (discards the oldest ones)
   // TODO deprecated, switch to CacheBuilder
   private Map<CopyTransProcessKey, CopyTransProcessHandle> recentlyCancelled =
         new MapMaker()
               .softValues()
               .expiration(1, TimeUnit.HOURS) // keep them for an hour
               .makeMap();
//         CacheBuilder.newBuilder()
//               .softValues()
//               .expireAfterWrite(1, TimeUnit.HOURS) // keep them for an hour
//               .build().asMap();

   // Collection of recently completed copy trans processes (discards the olders ones)
   // TODO deprecated, switch to CacheBuilder
   private Map<CopyTransProcessKey, CopyTransProcessHandle> recentlyFinished =
         new MapMaker()
               .softValues()
               .expiration(1, TimeUnit.HOURS) // keep them for an hour
               .makeMap();

//         CacheBuilder.newBuilder()
//               .softValues()
//               .expireAfterWrite(1, TimeUnit.HOURS) // keep them for an hour
//               .build().asMap();

   @In
   private ProcessManagerService processManagerServiceImpl;

   @In
   private Identity identity;


   public boolean isCopyTransRunning( Object target )
   {
      CopyTransProcessKey key;

      if( target instanceof HProjectIteration )
      {
         key = CopyTransProcessKey.getKey((HProjectIteration)target);
      }
      else if( target instanceof HDocument )
      {
         key = CopyTransProcessKey.getKey((HDocument)target);
      }
      else
      {
         throw new IllegalArgumentException("Copy Trans can only run for HProjectIteration and HDocument");
      }

      if( currentlyRunning.containsKey(key) )
      {
         CopyTransProcessHandle handle = currentlyRunning.get(key);

         if( handle != null )
         {
            if( !handle.isInProgress() )
            {
               currentlyRunning.remove(key);
            }
            return handle.isInProgress();
         }
      }
      return false;
   }

   /**
    * Start a Translation copy with the default options.
    */
   public void startCopyTrans( HProjectIteration iteration )
   {
      this.startCopyTrans( iteration, new HCopyTransOptions() );
   }

   /**
    * Start a Translation copy for a document with the given options.
    *
    * @param document The document for which to start copy trans.
    * @param options The options to run copy trans with.
    */
   public void startCopyTrans( HDocument document, HCopyTransOptions options )
   {
      if( isCopyTransRunning(document) )
      {
         throw new RuntimeException("Copy Trans is already running for document '" + document.getDocId() + "'");
      }

      CopyTransProcessHandle handle = new CopyTransProcessHandle( document, identity.getCredentials().getUsername(), options );
      handle.addListener(listenerInstance);

      processManagerServiceImpl.startProcess(new CopyTransProcess(), handle);
      currentlyRunning.put(CopyTransProcessKey.getKey(document), handle);
   }

   /**
    * Start a Translation copy with the given custom options.
    */
   public void startCopyTrans( HProjectIteration iteration, HCopyTransOptions options )
   {
      // double check
      if( isCopyTransRunning(iteration) )
      {
         throw new RuntimeException("Copy Trans is already running for version '" + iteration.getSlug() + "'");
      }

      CopyTransProcessHandle handle = new CopyTransProcessHandle( iteration, identity.getCredentials().getUsername(), options );
      handle.addListener(listenerInstance);

      processManagerServiceImpl.startProcess(new CopyTransProcess(), handle);
      currentlyRunning.put(CopyTransProcessKey.getKey(iteration), handle);
   }

   public CopyTransProcessHandle getCopyTransProcessHandle(Object target)
   {
      CopyTransProcessKey key;

      if( target instanceof HProjectIteration )
      {
         key = CopyTransProcessKey.getKey((HProjectIteration)target);
      }
      else if( target instanceof HDocument )
      {
         key = CopyTransProcessKey.getKey((HDocument)target);
      }
      else
      {
         throw new IllegalArgumentException("Copy Trans can only run for HProjectIteration and HDocument");
      }
      return currentlyRunning.get( key );
   }

   public void cancelCopyTrans( HProjectIteration iteration )
   {
      if( isCopyTransRunning(iteration) )
      {
         CopyTransProcessKey key = CopyTransProcessKey.getKey(iteration);
         CopyTransProcessHandle handle = this.getCopyTransProcessHandle(iteration);
         handle.stop();
         handle.setCancelledTime(System.currentTimeMillis());
         handle.setCancelledBy(identity.getCredentials().getUsername());
         this.recentlyCancelled.put(key, handle);
      }
   }

   /**
    * Obtains the most recently finished (cancelled or otherwise) process handle for a copy trans on a given target.
    * If a long time has passed since the last cancelled process, or if there has not been a recent cancellation, this
    * method may return null.
    *
    * @param target The target for which to retrieve the most recently finished process handle.
    * @return Most recently finished process handle for the project iteration, or null if there isn't one.
    */
   public CopyTransProcessHandle getMostRecentlyFinished( Object target )
   {
      CopyTransProcessKey key;

      if( target instanceof HProjectIteration )
      {
         key = CopyTransProcessKey.getKey((HProjectIteration)target);
      }
      else if( target instanceof HDocument )
      {
         key = CopyTransProcessKey.getKey((HDocument)target);
      }
      else
      {
         throw new IllegalArgumentException("Copy Trans can only run for HProjectIteration and HDocument");
      }

      // Only if copy trans is not running
      if( !this.isCopyTransRunning(target) )
      {
         CopyTransProcessHandle mostRecent = this.recentlyCancelled.get( key );
         CopyTransProcessHandle recentlyRan = this.recentlyFinished.get( key );

         if( mostRecent == null )
         {
            mostRecent = recentlyRan;
         }
         else if( recentlyRan != null && mostRecent != null
               && recentlyRan.getStartTime() > mostRecent.getStartTime() )
         {
            mostRecent = recentlyRan;
         }

         return mostRecent;
      }
      else
      {
         return null;
      }
   }

   /**
    * Internal class to detect when a copy trans process is complete.
    */
   private final class CopyTransProcessListener implements RunnableProcessListener<CopyTransProcessHandle>, Serializable
   {
      private static final long serialVersionUID = 1L;

      @Override
      public void onComplete(CopyTransProcessHandle handle)
      {
         // move the entry to the recently finished, if not already done (i.e. it was cancelled)
         if( currentlyRunning.containsValue( handle ) )
         {
            CopyTransProcessKey key;
            if( handle.getProjectIteration() != null )
            {
               key = CopyTransProcessKey.getKey(handle.getProjectIteration());
            }
            else
            {
               key = CopyTransProcessKey.getKey(handle.getDocument());
            }
            recentlyFinished.put( key, currentlyRunning.remove( key ) );
         }
      }
   }

   /**
    * Internal class to index Copy Trans processes.
    */
   @EqualsAndHashCode
   @Getter
   @Setter
   @NoArgsConstructor(access = AccessLevel.PRIVATE)
   private static final class CopyTransProcessKey implements Serializable
   {
      private static final long serialVersionUID = -2054359069473618887L;
      private String projectSlug;
      private String iterationSlug;
      private String docId;

      public static CopyTransProcessKey getKey(HProjectIteration iteration)
      {
         CopyTransProcessKey newKey = new CopyTransProcessKey();
         newKey.setProjectSlug(iteration.getProject().getSlug());
         newKey.setIterationSlug(iteration.getSlug());
         return newKey;
      }

      public static CopyTransProcessKey getKey(HDocument document)
      {
         CopyTransProcessKey newKey = new CopyTransProcessKey();
         newKey.setDocId(document.getDocId());
         newKey.setProjectSlug(document.getProjectIteration().getProject().getSlug());
         newKey.setIterationSlug(document.getProjectIteration().getSlug());
         return newKey;
      }
   }

}
