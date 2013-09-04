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
package org.zanata.async.tasks;

import com.google.common.base.Optional;
import org.jboss.seam.Component;
import org.zanata.ApplicationConfiguration;
import org.zanata.adapter.po.PoWriter2;
import org.zanata.async.AsyncTaskHandle;
import org.zanata.async.AsyncTask;
import org.zanata.common.LocaleId;
import org.zanata.dao.DocumentDAO;
import org.zanata.dao.LocaleDAO;
import org.zanata.dao.TextFlowTargetDAO;
import org.zanata.model.HDocument;
import org.zanata.model.HLocale;
import org.zanata.model.HTextFlowTarget;
import org.zanata.rest.dto.resource.Resource;
import org.zanata.rest.dto.resource.TranslationsResource;
import org.zanata.rest.service.ResourceUtils;
import org.zanata.service.ConfigurationService;
import org.zanata.service.FileSystemService;
import org.zanata.service.impl.ConfigurationServiceImpl;
import org.zanata.service.impl.FileSystemServiceImpl;

import java.io.File;
import java.io.FileOutputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * This is an asynchronous task to build a zip file containing all files for a Project
 * Iteration. its expected return value is an input stream with the contents of the Zip file.
 * Currently only supports PO files.
 *
 * @author Carlos Munoz <a href="mailto:camunoz@redhat.com">camunoz@redhat.com</a>
 */
public class ZipFileBuildTask implements AsyncTask<String, AsyncTaskHandle<String>>
{

   private final String projectSlug;
   private final String iterationSlug;
   private final String localeId;
   private final String userName;
   private final boolean isOfflinePo;

   private AsyncTaskHandle<String> handle;

   public ZipFileBuildTask(String projectSlug, String iterationSlug, String localeId, String userName, boolean offlinePo)
   {
      this.projectSlug = projectSlug;
      this.iterationSlug = iterationSlug;
      this.localeId = localeId;
      this.userName = userName;
      isOfflinePo = offlinePo;
   }

   @Override
   public AsyncTaskHandle<String> getHandle()
   {
      if( handle == null )
      {
         handle = buildHandle();
      }
      return handle;
   }

   private AsyncTaskHandle<String> buildHandle()
   {
      AsyncTaskHandle<String> newHandle = new AsyncTaskHandle<String>();

      // Max documents to process
      DocumentDAO documentDAO = (DocumentDAO) Component.getInstance(DocumentDAO.class);
      final List<HDocument> allIterationDocs = documentDAO.getAllByProjectIteration(projectSlug, iterationSlug);
      newHandle.setMaxProgress(allIterationDocs.size() + 1); // all files plus the zanata.xml file

      return newHandle;
   }

   @Override
   public String call() throws Exception
   {
      // Needed Components
      DocumentDAO documentDAO = (DocumentDAO) Component.getInstance(DocumentDAO.class);
      LocaleDAO localeDAO = (LocaleDAO)Component.getInstance(LocaleDAO.class);
      ResourceUtils resourceUtils = (ResourceUtils)Component.getInstance(ResourceUtils.class);
      TextFlowTargetDAO textFlowTargetDAO = (TextFlowTargetDAO)Component.getInstance(TextFlowTargetDAO.class);
      FileSystemService fileSystemService = (FileSystemService) Component.getInstance(FileSystemServiceImpl.class);
      ConfigurationService configurationService = (ConfigurationService)Component.getInstance(ConfigurationServiceImpl.class);
      ApplicationConfiguration applicationConfiguration = (ApplicationConfiguration)Component.getInstance(ApplicationConfiguration.class);

      final String projectDirectory = projectSlug + "-" + iterationSlug + "/";
      final HLocale hLocale = localeDAO.findByLocaleId(new LocaleId(localeId));
      final String mappedLocale = hLocale.getLocaleId().getId();
      final String localeDirectory = projectDirectory + mappedLocale + "/";

      final File downloadFile = fileSystemService.createDownloadStagingFile("zip");
      final FileOutputStream output = new FileOutputStream( downloadFile );
      final ZipOutputStream zipOutput = new ZipOutputStream(output);
      zipOutput.setMethod(ZipOutputStream.DEFLATED);
      final PoWriter2 poWriter = new PoWriter2(false, isOfflinePo);
      final Set<String> extensions = new HashSet<String>();

      extensions.add("gettext");
      extensions.add("comment");

      // Generate the download descriptor file
      String downloadId = fileSystemService.createDownloadDescriptorFile(downloadFile,
            projectSlug + "_" + iterationSlug + "_" + localeId + ".zip",
            userName);

      // Add the config file at the root of the project directory
      String configFilename = projectDirectory + configurationService.getConfigurationFileName();
      zipOutput.putNextEntry(new ZipEntry(configFilename));
      zipOutput.write(configurationService.getConfigurationFileContents(
            projectSlug, iterationSlug, hLocale, isOfflinePo, applicationConfiguration.getServerPath())
            .getBytes());
      zipOutput.closeEntry();
      getHandle().increaseProgress(1);

      final List<HDocument> allIterationDocs = documentDAO.getAllByProjectIteration(projectSlug, iterationSlug);
      for (HDocument document : allIterationDocs)
      {
         // Stop the process if signaled to do so
         if (getHandle().isCancelled())
         {
            zipOutput.close();
            downloadFile.delete();
            fileSystemService.deleteDownloadDescriptorFile(downloadId);
            return null;
         }

         TranslationsResource translationResource = new TranslationsResource();
         List<HTextFlowTarget> hTargets = textFlowTargetDAO.findTranslations(document, hLocale);
         resourceUtils.transferToTranslationsResource(
               translationResource, document, hLocale, extensions, hTargets, Optional.<String>absent());

         Resource res = resourceUtils.buildResource( document );

         String filename = localeDirectory + document.getDocId() + ".po";
         zipOutput.putNextEntry(new ZipEntry(filename));
         poWriter.writePo(zipOutput, "UTF-8", res, translationResource);
         zipOutput.closeEntry();

         getHandle().increaseProgress(1);
      }

      zipOutput.flush();
      zipOutput.close();

      return downloadId;
   }
}
