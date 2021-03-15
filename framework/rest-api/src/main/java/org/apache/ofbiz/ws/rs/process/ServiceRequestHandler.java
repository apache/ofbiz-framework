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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ModelParam;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.service.ServiceUtil;
import org.apache.ofbiz.ws.rs.util.ErrorUtil;
import org.apache.ofbiz.ws.rs.util.RestApiUtil;

public final class ServiceRequestHandler extends RestRequestHandler {

    private static final String MODULE = ServiceRequestHandler.class.getName();
    private String service;

    public ServiceRequestHandler(String service) {
        this.service = service;
    }

    /**
     * @param ctx ContainerRequestContext
     * @param arguments Map
     * @return Response
     */
    @Override
    protected Response execute(ContainerRequestContext ctx, Map<String, Object> arguments) {
        ctx.setProperty("requestForService", service);
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
        serviceContext.put("userLogin", userLogin);
        Map<String, Object> result = null;
        try {
            result = dispatcher.runSync(service, serviceContext);
        } catch (GenericServiceException e) {
            Debug.logError(e, MODULE);
            final ExceptionMapper<GenericServiceException> mapper = getMappers().get().findMapping(e);
            return mapper.toResponse(e);
        }
        Map<String, Object> responseData = new LinkedHashMap<>();
        if (ServiceUtil.isSuccess(result)) {
            Set<String> outParams = svc.getOutParamNames();
            for (String outParamName : outParams) {
                ModelParam outParam = svc.getParam(outParamName);
                if (!outParam.isInternal()) {
                    Object value = result.get(outParamName);
                    if (UtilValidate.isNotEmpty(value)) {
                        responseData.put(outParamName, value);
                    }
                }
            }
            return RestApiUtil.success((String) result.get(ModelService.SUCCESS_MESSAGE), responseData);
        } else {
            return ErrorUtil.buildErrorFromServiceResult(service, result, getHttpRequest().getLocale());
        }
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
}
