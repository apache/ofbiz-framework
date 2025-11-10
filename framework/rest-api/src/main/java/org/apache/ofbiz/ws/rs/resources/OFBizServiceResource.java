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
     * @return
     * @throws GenericServiceException
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
     * @param serviceRequest
     * @param serviceName
     * @return
     * @throws IOException
     * @throws GenericServiceException
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
     * @param serviceInParams
     * @param serviceName
     * @return
     * @throws IOException
     * @throws GenericEntityException
     * @throws GenericServiceException
     */
    @POST
    @Path("/{serviceName}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response doPost(HashMap<String, Object> serviceInParams,
            @PathParam(value = "serviceName") String serviceName)
            throws IOException, GenericEntityException, GenericServiceException {
        if (UtilValidate.isEmpty(serviceInParams)) {
            throw new BadRequestException("The request body is missing.");
        }
        ServiceRequestProcessor processor = new ServiceRequestProcessor();
        return processor.process(UtilMisc.toMap("serviceName", serviceName, "httpVerb", HttpMethod.POST, "requestMap",
                serviceInParams, "dispatcher", servletContext.getAttribute("dispatcher"), "request", httpRequest));
    }

    /**
     * @param serviceInParams
     * @param serviceName
     * @return
     * @throws IOException
     * @throws GenericEntityException
     * @throws GenericServiceException
     */
    @PUT
    @Path("/{serviceName}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response doPut(HashMap<String, Object> serviceInParams, @PathParam(value = "serviceName") String serviceName)
            throws IOException, GenericEntityException, GenericServiceException {
        if (UtilValidate.isEmpty(serviceInParams)) {
            throw new BadRequestException("The request body is missing.");
        }
        ServiceRequestProcessor processor = new ServiceRequestProcessor();
        return processor.process(UtilMisc.toMap("serviceName", serviceName, "httpVerb", HttpMethod.PUT, "requestMap",
                serviceInParams, "dispatcher", servletContext.getAttribute("dispatcher"), "request", httpRequest));
    }

    /**
     * @param serviceInParams
     * @param serviceName
     * @return
     * @throws IOException
     * @throws GenericEntityException
     * @throws GenericServiceException
     */
    @PATCH
    @Path("/{serviceName}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response doPatch(HashMap<String, Object> serviceInParams,
            @PathParam(value = "serviceName") String serviceName)
            throws IOException, GenericEntityException, GenericServiceException {
        if (UtilValidate.isEmpty(serviceInParams)) {
            throw new BadRequestException("The request body is missing.");
        }
        ServiceRequestProcessor processor = new ServiceRequestProcessor();
        return processor.process(UtilMisc.toMap("serviceName", serviceName, "httpVerb", HttpMethod.PATCH, "requestMap",
                serviceInParams, "dispatcher", servletContext.getAttribute("dispatcher"), "request", httpRequest));
    }

    /**
     * @param serviceInParams
     * @param serviceName
     * @return
     * @throws IOException
     * @throws GenericEntityException
     * @throws GenericServiceException
     */
    @DELETE
    @Path("/{serviceName}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response doDelete(HashMap<String, Object> serviceInParams,
            @PathParam(value = "serviceName") String serviceName)
            throws IOException, GenericEntityException, GenericServiceException {
        if (UtilValidate.isEmpty(serviceInParams)) {
            throw new BadRequestException("The request body is missing.");
        }
        ServiceRequestProcessor processor = new ServiceRequestProcessor();
        return processor.process(UtilMisc.toMap("serviceName", serviceName, "httpVerb", HttpMethod.DELETE, "requestMap",
                serviceInParams, "dispatcher", servletContext.getAttribute("dispatcher"), "request", httpRequest));
    }
}
