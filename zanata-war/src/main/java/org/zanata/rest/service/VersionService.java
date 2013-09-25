package org.zanata.rest.service;


import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import org.jboss.seam.annotations.Name;
import org.zanata.rest.dto.VersionInfo;
import org.zanata.util.VersionUtility;


@Name("versionService")
@Path(VersionResource.SERVICE_PATH)
public class VersionService implements VersionResource
{
   
   private final VersionInfo version;

   public VersionService()
   {
      this(VersionUtility.getAPIVersionInfo());
   }

   VersionService(VersionInfo ver)
   {
      this.version = ver;
   }

   @Override
   public Response get()
   {
      return Response.ok(version).build();
   }
}
