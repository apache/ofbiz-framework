/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *******************************************************************************/
package org.apache.ofbiz.ws.rs;

import java.io.IOException;

import javax.annotation.Priority;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.webapp.WebAppUtil;
import org.apache.ofbiz.ws.rs.annotation.ServiceRequestValidator;
import org.apache.ofbiz.ws.rs.util.RestApiUtil;

@Provider
@ServiceRequestValidator
@Priority(Priorities.USER)
public class ServiceRequestFilter implements ContainerRequestFilter {

    private static final String MODULE = ServiceRequestFilter.class.getName();

    private static final String SVC_IN_PARAMS = "inParams";

    @Context
    private UriInfo uriInfo;

    @Context
    private ResourceInfo resourceInfo;

    @Context
    private HttpServletRequest httpRequest;

    @Context
    private ServletContext servletContext;

    /**
     * @param requestContext
     * @throws IOException
     */
    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        Debug.logInfo("Service request is going to get validated!", MODULE);
        String service = (String) RestApiUtil.extractParams(uriInfo.getPathParameters()).get("serviceName");
        String method = requestContext.getMethod();
        String action = null;
        if (UtilValidate.isNotEmpty(service)) {
            ModelService mdService = null;
            try {
                mdService = WebAppUtil.getDispatcher(servletContext).getDispatchContext().getModelService(service);
            } catch (GenericServiceException e) {
                Debug.logError(e.getMessage(), MODULE);
            }

            if (mdService == null) {
                throw new ServiceNotFoundException(service);
            }

            if (mdService != null && !mdService.isExport()) {
                throw new NotFoundException("Service '" + service + "' is not exportable.");
            }

            action = mdService.getAction();
            if (mdService != null && UtilValidate.isEmpty(action)) {
                throw new NotFoundException("Service '" + service + "' does not have HTTP action defined.");
            }

            if (!action.equalsIgnoreCase(method)) {
                throw new MethodNotAllowedException("HTTP " + method + " is not allowed on service '" + service + "'");
            }

            if (action.equalsIgnoreCase(HttpMethod.GET) && UtilValidate.isNotEmpty(mdService.getInParamNamesMap())
                    && UtilValidate.isEmpty(httpRequest.getParameter(SVC_IN_PARAMS))) {
                throw new BadRequestException("Missing Parameter: 'inParams'");
            }
            // If everything looks good, set the 'requestForService' property in the
            // context. Indicates which service this request is for.
            requestContext.setProperty("requestForService", service);
        }
    }

}
