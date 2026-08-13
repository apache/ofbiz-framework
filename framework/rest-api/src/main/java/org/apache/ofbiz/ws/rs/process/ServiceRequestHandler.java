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
package org.apache.ofbiz.ws.rs.process;

import java.util.Locale;
import java.util.Map;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.service.ServiceUtil;
import org.apache.ofbiz.ws.rs.ServiceNameContextHolder;
import org.apache.ofbiz.ws.rs.util.RestApiUtil;
import org.apache.ofbiz.ws.rs.util.ServiceRequestWorker;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;

public final class ServiceRequestHandler extends RestRequestHandler {

    private static final String MODULE = ServiceRequestHandler.class.getName();
    private String service;
    private String primaryPermission;
    private String mainAction;

    public ServiceRequestHandler(String service) {
        this.service = service;
    }

    public ServiceRequestHandler(String service, String primaryPermission) {
        this.service = service;
        this.primaryPermission = primaryPermission;
    }

    public ServiceRequestHandler(String service, String primaryPermission, String mainAction) {
        this.service = service;
        this.primaryPermission = primaryPermission;
        this.mainAction = mainAction;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Executes the mapped OFBiz service synchronously and returns a JSON
     * response. The execution proceeds as follows:</p>
     * <ul>
     *   <li>A valid service context is built from the request arguments using
     *       {@link DispatchContext#makeValidContext}</li>
     *   <li>The authenticated {@code userLogin} is added to the service context</li>
     *   <li>The service is executed synchronously via {@link LocalDispatcher#runSync}</li>
     *   <li>On success, non-internal output parameters are collected and returned
     *       as a JSON success response</li>
     *   <li>On service result failure, an HTTP 422 error response is returned</li>
     *   <li>On {@link GenericServiceException} from either context building or
     *       execution, the registered {@link ExceptionMapper} is used to map the
     *       exception to an appropriate error response</li>
     * </ul>
     */
    @Override
    protected Response execute(ContainerRequestContext ctx, Map<String, Object> arguments) {
        ServiceNameContextHolder.set(service);
        addSecurityParameters(arguments);
        LocalDispatcher dispatcher = (LocalDispatcher) getServletContext().getAttribute("dispatcher");
        Map<String, Object> serviceContext = null;
        try {
            serviceContext = dispatcher.getDispatchContext().makeValidContext(service, ModelService.IN_PARAM, arguments);
        } catch (GenericServiceException e) {
            Debug.logError(e, MODULE);
            final ExceptionMapper<GenericServiceException> mapper = getMappers().get().findMapping(e);
            return mapper.toResponse(e);
        }
        ModelService svc = getModelService(dispatcher.getDispatchContext());
        GenericValue userLogin = (GenericValue) getHttpRequest().getAttribute("userLogin");
        Locale locale = getHttpRequest().getLocale();
        serviceContext.put("userLogin", userLogin);
        serviceContext.put("locale", locale);
        Map<String, Object> result = null;
        try {
            result = dispatcher.runSync(service, serviceContext);
        } catch (GenericServiceException e) {
            Debug.logError(e, MODULE);
            final ExceptionMapper<GenericServiceException> mapper = getMappers().get().findMapping(e);
            return mapper.toResponse(e);
        }
        if (ServiceUtil.isSuccess(result)) {
            Map<String, Object> responseData = ServiceRequestWorker.extractResponseData(svc, result);
            return RestApiUtil.success((String) result.get(ModelService.SUCCESS_MESSAGE), responseData);
        }
        return RestApiUtil.buildErrorFromServiceResult(service, result, locale);
    }

    private ModelService getModelService(DispatchContext dispatchContext) {
        ModelService svc = null;
        try {
            svc = dispatchContext.getModelService(service);
        } catch (GenericServiceException gse) {
            throw new NotFoundException(gse.getMessage());
        }
        return svc;
    }

    /**
     * Adds Security parameters to the the service parameters.
     * @param arguments
     */
    private void addSecurityParameters(Map<String, Object> arguments) {
        if (arguments != null) {
            if (primaryPermission != null) {
                arguments.put("primaryPermission", primaryPermission);
            }
            if (mainAction != null) {
                arguments.put("mainAction", mainAction);
            }
        }
    }

}
