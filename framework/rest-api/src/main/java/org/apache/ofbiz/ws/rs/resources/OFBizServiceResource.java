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

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.PATCH;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;

import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.ws.rs.ApiServiceRequest;
import org.apache.ofbiz.ws.rs.ServiceRequestProcessor;
import org.apache.ofbiz.ws.rs.annotation.ServiceRequestValidator;
import org.apache.ofbiz.ws.rs.response.Success;
import org.apache.ofbiz.ws.rs.security.Secured;

@Secured
@Path(OFBizServiceResource.BASE_PATH)
@Provider
@ServiceRequestValidator
public class OFBizServiceResource extends OFBizResource {

    public static final String BASE_PATH = "/services";

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
        LocalDispatcher dispatcher = getDispatcher();
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
                serviceRequest.getInParams(), "dispatcher", getDispatcher(), "request", httpRequest));
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
    @Produces(MediaType.APPLICATION_JSON)
    public Response doPost(HashMap<String, Object> serviceInParams,
            @PathParam(value = "serviceName") String serviceName)
            throws IOException, GenericEntityException, GenericServiceException {
        if (UtilValidate.isEmpty(serviceInParams)) {
            throw new BadRequestException("The request body is missing.");
        }
        ServiceRequestProcessor processor = new ServiceRequestProcessor();
        return processor.process(UtilMisc.toMap("serviceName", serviceName, "httpVerb", HttpMethod.POST, "requestMap",
                serviceInParams, "dispatcher", getDispatcher(), "request", httpRequest));
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
    @Produces(MediaType.APPLICATION_JSON)
    public Response doPut(HashMap<String, Object> serviceInParams, @PathParam(value = "serviceName") String serviceName)
            throws IOException, GenericEntityException, GenericServiceException {
        if (UtilValidate.isEmpty(serviceInParams)) {
            throw new BadRequestException("The request body is missing.");
        }
        ServiceRequestProcessor processor = new ServiceRequestProcessor();
        return processor.process(UtilMisc.toMap("serviceName", serviceName, "httpVerb", HttpMethod.PUT, "requestMap",
                serviceInParams, "dispatcher", getDispatcher(), "request", httpRequest));
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
    @Produces(MediaType.APPLICATION_JSON)
    public Response doPatch(HashMap<String, Object> serviceInParams,
            @PathParam(value = "serviceName") String serviceName)
            throws IOException, GenericEntityException, GenericServiceException {
        if (UtilValidate.isEmpty(serviceInParams)) {
            throw new BadRequestException("The request body is missing.");
        }
        ServiceRequestProcessor processor = new ServiceRequestProcessor();
        return processor.process(UtilMisc.toMap("serviceName", serviceName, "httpVerb", HttpMethod.PATCH, "requestMap",
                serviceInParams, "dispatcher", getDispatcher(), "request", httpRequest));
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
    @Produces(MediaType.APPLICATION_JSON)
    public Response doDelete(HashMap<String, Object> serviceInParams,
            @PathParam(value = "serviceName") String serviceName)
            throws IOException, GenericEntityException, GenericServiceException {
        if (UtilValidate.isEmpty(serviceInParams)) {
            throw new BadRequestException("The request body is missing.");
        }
        ServiceRequestProcessor processor = new ServiceRequestProcessor();
        return processor.process(UtilMisc.toMap("serviceName", serviceName, "httpVerb", HttpMethod.DELETE, "requestMap",
                serviceInParams, "dispatcher", getDispatcher(), "request", httpRequest));
    }
}
