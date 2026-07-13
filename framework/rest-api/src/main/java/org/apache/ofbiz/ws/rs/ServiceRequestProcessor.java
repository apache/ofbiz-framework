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

import java.util.Map;

import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.service.ServiceUtil;
import org.apache.ofbiz.ws.rs.util.RestApiUtil;
import org.apache.ofbiz.ws.rs.util.ServiceRequestWorker;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;

public class ServiceRequestProcessor {

    /**
     * Processes an incoming REST API request by executing the mapped OFBiz service
     * and returning a JSON response.
     *
     * <p>The following keys are expected in {@code requestContext}:</p>
     * <ul>
     *   <li>{@code serviceName} — the OFBiz service to invoke</li>
     *   <li>{@code httpVerb} — the HTTP method of the request (e.g. {@code GET}, {@code POST})</li>
     *   <li>{@code requestMap} — the request parameters</li>
     *   <li>{@code dispatcher} — the {@link LocalDispatcher} to use for service invocation</li>
     *   <li>{@code request} — the {@link HttpServletRequest} providing the authenticated user</li>
     * </ul>
     *
     * <p>If the service result indicates success, the non-internal output parameters
     * are returned in the response data. If the service fails, an HTTP 422
     * Unprocessable Entity error response is returned.</p>
     *
     * @param requestContext a map containing the request execution context
     * @return a JSON {@link Response} with the service output on success, or an
     *         error response on failure
     * @throws GenericServiceException if the service invocation fails unexpectedly
     * @throws NotFoundException       if the requested service does not exist
     * @throws MethodNotAllowedException if the HTTP verb is not permitted for the service
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
        Map<String, Object> serviceContext = dispatchContext.makeValidContext(serviceName, ModelService.IN_PARAM, requestMap);
        serviceContext.put("userLogin", userLogin);
        Map<String, Object> result = dispatcher.runSync(serviceName, serviceContext);
        if (ServiceUtil.isSuccess(result)) {
            Map<String, Object> responseData = ServiceRequestWorker.extractResponseData(service, result);
            return RestApiUtil.success((String) result.get(ModelService.SUCCESS_MESSAGE), responseData);
        }
        return RestApiUtil.buildErrorFromServiceResult(serviceName, result, request.getLocale());
    }
}
