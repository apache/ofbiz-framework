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
package org.apache.ofbiz.ws.rs.resources;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.ws.rs.ApiServiceRequest;
import org.apache.ofbiz.ws.rs.ServiceRequestProcessor;
import org.apache.ofbiz.ws.rs.annotation.Secured;
import org.apache.ofbiz.ws.rs.annotation.ServiceRequestValidator;
import org.apache.ofbiz.ws.rs.response.Success;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Link;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

@Secured
@Path(OFBizServiceResource.BASE_PATH)
@ServiceRequestValidator
public class OFBizServiceResource {

    public static final String BASE_PATH = "/services";

    @Context
    private ServletContext servletContext;

    @Context
    private UriInfo uriInfo;

    @Context
    private HttpServletRequest httpRequest;

    /**
     * Returns a list of all OFBiz services that are marked as exported and have a valid action.
     *
     * <p>Each service entry contains:
     * <ul>
     *     <li>Service name</li>
     *     <li>Description</li>
     *     <li>Self link for invocation using the configured HTTP method</li>
     * </ul>
     *
     * @return HTTP 200 response containing a JSON payload with available services
     * @throws GenericServiceException if service model lookup fails
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response serviceList() throws GenericServiceException {
        LocalDispatcher dispatcher = (LocalDispatcher) servletContext.getAttribute("dispatcher");
        DispatchContext context = dispatcher.getDispatchContext();
        Set<String> serviceNames = context.getAllServiceNames();
        List<Map<String, Object>> serviceList = new ArrayList<>();
        for (String serviceName : serviceNames) {
            ModelService service = context.getModelService(serviceName);
            if (service != null && service.isExport() && UtilValidate.isNotEmpty(service.getAction())) {
                Map<String, Object> serviceMap = new LinkedHashMap<String, Object>();
                serviceMap.put("name", service.getName());
                serviceMap.put("description", service.getDescription());
                Link selfLink = Link.fromUriBuilder(uriInfo.getAbsolutePathBuilder().path(service.getName()))
                        .type(service.getAction()).rel("self").build();
                serviceMap.put("link", selfLink);
                serviceList.add(serviceMap);
            }
        }
        Success success = new Success(Response.Status.OK.getStatusCode(), Response.Status.OK.getReasonPhrase(),
                Response.Status.OK.getReasonPhrase(), serviceList);
        return Response.status(Response.Status.OK).type(MediaType.APPLICATION_JSON).entity(success).build();
    }

    /**
     * Invokes an OFBiz service using HTTP GET semantics.
     *
     * <p>Request parameters are provided via query parameters and mapped to service input fields.</p>
     *
     * @param serviceRequest wrapper containing input parameters from query string
     * @param serviceName the name of the OFBiz service to execute
     * @return response containing the service execution result
     * @throws IOException if request processing fails
     * @throws GenericServiceException if service execution fails
     */
    @GET
    @Path("/{serviceName}")
    @Produces(MediaType.APPLICATION_JSON)
    @Secured
    public Response doGet(@QueryParam(value = "inParams") ApiServiceRequest serviceRequest,
            @PathParam(value = "serviceName") String serviceName) throws IOException, GenericServiceException {
        ServiceRequestProcessor processor = new ServiceRequestProcessor();
        return processor.process(UtilMisc.toMap("serviceName", serviceName, "httpVerb", HttpMethod.GET, "requestMap",
                serviceRequest.getInParams(), "dispatcher", servletContext.getAttribute("dispatcher"), "request", httpRequest));
    }

    /**
     * Invokes an OFBiz service using HTTP POST semantics.
     *
     * <p>The request body must contain a JSON object representing service input parameters.</p>
     *
     * @param serviceInParams JSON payload mapped to service input parameters
     * @param serviceName the name of the OFBiz service to execute
     * @return response containing the service execution result
     * @throws IOException if request processing fails
     * @throws GenericEntityException if entity processing fails
     * @throws GenericServiceException if service execution fails
     */
    @POST
    @Path("/{serviceName}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response doPost(HashMap<String, Object> serviceInParams,
            @PathParam(value = "serviceName") String serviceName)
            throws IOException, GenericEntityException, GenericServiceException {
        return processBodyRequest(serviceName, HttpMethod.POST, serviceInParams);
    }

    /**
     * Invokes an OFBiz service using HTTP PUT semantics.
     *
     * <p>The request body replaces the full input set for the service invocation.</p>
     *
     * @param serviceInParams JSON payload mapped to service input parameters
     * @param serviceName the name of the OFBiz service to execute
     * @return response containing the service execution result
     * @throws IOException if request processing fails
     * @throws GenericEntityException if entity processing fails
     * @throws GenericServiceException if service execution fails
     */
    @PUT
    @Path("/{serviceName}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response doPut(HashMap<String, Object> serviceInParams, @PathParam(value = "serviceName") String serviceName)
            throws IOException, GenericEntityException, GenericServiceException {
        return processBodyRequest(serviceName, HttpMethod.PUT, serviceInParams);
    }

    /**
     * Invokes an OFBiz service using HTTP PATCH semantics.
     *
     * <p>The request body contains partial updates to be applied as service input parameters.</p>
     *
     * @param serviceInParams JSON payload containing partial service input parameters
     * @param serviceName the name of the OFBiz service to execute
     * @return response containing the service execution result
     * @throws IOException if request processing fails
     * @throws GenericEntityException if entity processing fails
     * @throws GenericServiceException if service execution fails
     */
    @PATCH
    @Path("/{serviceName}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response doPatch(HashMap<String, Object> serviceInParams,
            @PathParam(value = "serviceName") String serviceName)
            throws IOException, GenericEntityException, GenericServiceException {
        return processBodyRequest(serviceName, HttpMethod.PATCH, serviceInParams);
    }

    /**
     * Invokes an OFBiz service using HTTP DELETE semantics.
     *
     * <p>The request body contains parameters required to identify or remove the target resource.</p>
     *
     * @param serviceInParams JSON payload mapped to service input parameters
     * @param serviceName the name of the OFBiz service to execute
     * @return response containing the service execution result
     * @throws IOException if request processing fails
     * @throws GenericEntityException if entity processing fails
     * @throws GenericServiceException if service execution fails
     */
    @DELETE
    @Path("/{serviceName}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response doDelete(HashMap<String, Object> serviceInParams,
            @PathParam(value = "serviceName") String serviceName)
            throws IOException, GenericEntityException, GenericServiceException {
        return processBodyRequest(serviceName, HttpMethod.DELETE, serviceInParams);
    }

    private Response processBodyRequest(String serviceName, String httpVerb, HashMap<String, Object> serviceInParams)
            throws IOException, GenericEntityException, GenericServiceException {
        if (UtilValidate.isEmpty(serviceInParams)) {
            throw new BadRequestException("The request body is missing.");
        }
        ServiceRequestProcessor processor = new ServiceRequestProcessor();
        return processor.process(UtilMisc.toMap(
                "serviceName", serviceName,
                "httpVerb", httpVerb,
                "requestMap", serviceInParams,
                "dispatcher", servletContext.getAttribute("dispatcher"),
                "request", httpRequest));
    }
}
