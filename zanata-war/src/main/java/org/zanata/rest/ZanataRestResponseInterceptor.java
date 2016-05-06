/*
 * Copyright 2015, Red Hat, Inc. and individual contributors as indicated by the
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

package org.zanata.rest;

import java.io.IOException;
import java.util.List;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;

import org.jboss.resteasy.annotations.interception.HeaderDecoratorPrecedence;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

/**
 * This Interceptor is a POST process for Zanata REST API request.
 * It support Cross-Origin resource sharing (CORS) by enabling Access-Control header in
 * response header. CORS is needed for request from different domain.
 *
 * @author Alex Eng <a href="mailto:aeng@redhat.com">aeng@redhat.com</a>
 */
@Provider
@PreMatching
@HeaderDecoratorPrecedence
public class ZanataRestResponseInterceptor implements ContainerResponseFilter {
    private static final String ALLOW_METHODS = "PUT, POST, DELETE, GET, OPTIONS";

    public void filter(ContainerRequestContext requestContext,
            ContainerResponseContext responseContext) throws IOException {
        MultivaluedMap<String, String> requestHeaders =
            requestContext.getHeaders();
        MultivaluedMap<String, Object> responseHeaders =
            responseContext.getHeaders();

        List<String> allowHeaders = requestHeaders
                .get("Access-Control-Request-Headers");
        if(allowHeaders == null) {
            allowHeaders = Lists.newArrayList();
        }
        List<String> origins = requestHeaders.get("origin");
        if (origins != null && !origins.isEmpty()) {
            responseHeaders.add("Access-Control-Allow-Origin",
                Joiner.on(" ").skipNulls().join(origins));
        } else {
            responseHeaders.add("Access-Control-Allow-Origin", "*");
        }
        if(requestContext.getMethod().equals("OPTIONS")) {
            responseHeaders.add("Access-Control-Allow-Methods", ALLOW_METHODS);
        } else {
            responseHeaders.add("Access-Control-Allow-Methods",
                    requestContext.getMethod());
        }
        responseHeaders.add("Access-Control-Allow-Credentials", true);
        responseHeaders.add("Access-Control-Allow-Headers",
            "X-Requested-With, Content-Type, Accept, " + Joiner.on(",").join(
                allowHeaders));
    }
}
