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
package org.zanata.rest.service.editor;

import java.util.List;

import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Transactional;
import org.zanata.common.LocaleId;
import org.zanata.dao.TextFlowTargetDAO;
import org.zanata.model.HTextFlowTarget;
import org.zanata.rest.dto.resource.TransUnit;
import org.zanata.rest.dto.resource.TransUnits;

/**
 * @author Alex Eng <a href="mailto:aeng@redhat.com">aeng@redhat.com</a>
 */
@Name("transUnitService")
@Path(TransUnitResource.SERVICE_PATH)
@Transactional
public class TransUnitService implements TransUnitResource {
    @In
    private TextFlowTargetDAO textFlowTargetDAO;

    @In
    private TransUnitUtils transUnitUtils;

    @Override
    public Response get(String localeId, String ids) {
        TransUnits transUnits = new TransUnits();
        if (StringUtils.isEmpty(ids)) {
            return Response.ok(transUnits).build();
        }
        List<Long> idList = TransUnitUtils.filterAndConvertIdsToList(ids);
        if (idList.size() > TransUnitUtils.MAX_SIZE) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        LocaleId locale = new LocaleId(localeId);

        for (Long id : idList) {
            HTextFlowTarget hTarget =
                    textFlowTargetDAO.getTextFlowTarget(id, locale);
            if (hTarget != null) {
                TransUnit tu =
                        transUnitUtils.buildTransUnit(hTarget, locale, true,
                                true);
                transUnits.addTransUnit(hTarget.getTextFlow().getId()
                        .toString(), tu);
            }
        }
        return Response.ok(transUnits).build();
    }
}
