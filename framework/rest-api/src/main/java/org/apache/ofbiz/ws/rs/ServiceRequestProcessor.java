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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;

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

public class ServiceRequestProcessor {

    /**
     * @param requestContext
     * @return
     * @throws GenericServiceException
     */
    @SuppressWarnings("unchecked")
    public Response process(Map<String, Object> requestContext) throws GenericServiceException {
        String serviceName = (String) requestContext.get("serviceName");
        String httpVerb = (String) requestContext.get("httpVerb");
        Map<String, Object> requestMap = (Map<String, Object>) requestContext.get("requestMap");
        LocalDispatcher dispatcher = (LocalDispatcher) requestContext.get("dispatcher");
        HttpServletRequest request = (HttpServletRequest) requestContext.get("request");
        GenericValue userLogin = (GenericValue) request.getAttribute("userLogin");
        DispatchContext dispatchContext = dispatcher.getDispatchContext();
        ModelService service = null;
        try {
            service = dispatchContext.getModelService(serviceName);
        } catch (GenericServiceException gse) {
            throw new NotFoundException(gse.getMessage());
        }
        if (UtilValidate.isNotEmpty(service.getAction()) && !service.getAction().equalsIgnoreCase(httpVerb)) {
            throw new MethodNotAllowedException("HTTP " + httpVerb + " is not allowed on this service.");
        }
        Map<String, Object> serviceContext = dispatchContext.makeValidContext(serviceName, ModelService.IN_PARAM, requestMap);
        serviceContext.put("userLogin", userLogin);
        Map<String, Object> result = dispatcher.runSync(serviceName, serviceContext);
        Map<String, Object> responseData = new LinkedHashMap<>();
        if (ServiceUtil.isSuccess(result)) {
            Set<String> outParams = service.getOutParamNames();
            for (String outParamName : outParams) {
                ModelParam outParam = service.getParam(outParamName);
                if (!outParam.isInternal()) {
                    Object value = result.get(outParamName);
                    if (UtilValidate.isNotEmpty(value)) {
                        responseData.put(outParamName, value);
                    }
                }
            }
            return RestApiUtil.success((String) result.get(ModelService.SUCCESS_MESSAGE), responseData);
        } else {
            return ErrorUtil.buildErrorFromServiceResult(serviceName, result, request.getLocale());
        }
    }
}
