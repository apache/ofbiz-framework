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
package org.apache.ofbiz.ws.rs.openapi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.servlet.ServletContext;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ModelParam;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.webapp.WebAppUtil;
import org.apache.ofbiz.ws.rs.core.OFBizApiConfig;
import org.apache.ofbiz.ws.rs.listener.ApiContextListener;
import org.apache.ofbiz.ws.rs.model.ModelApi;
import org.apache.ofbiz.ws.rs.model.ModelOperation;
import org.apache.ofbiz.ws.rs.model.ModelResource;
import org.apache.ofbiz.ws.rs.util.OpenApiUtil;
import org.apache.ofbiz.ws.rs.util.RestApiUtil;

import io.swagger.v3.jaxrs2.Reader;
import io.swagger.v3.oas.integration.api.OpenAPIConfiguration;
import io.swagger.v3.oas.integration.api.OpenApiReader;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.HeaderParameter;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.PathParameter;
import io.swagger.v3.oas.models.parameters.QueryParameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.tags.Tag;

public final class OFBizOpenApiReader extends Reader implements OpenApiReader {
    private static final String MODULE = OFBizOpenApiReader.class.getName();
    private Components components;
    private Paths paths;
    @SuppressWarnings("rawtypes")
    private Map<String, Schema> schemas;
    private OpenAPI openApi;
    private DispatchContext context;
    private static final Parameter HEADER_CONTENT_TYPE_JSON = new HeaderParameter().name(HttpHeaders.CONTENT_TYPE)
            .schema(new StringSchema()).example(jakarta.ws.rs.core.MediaType.APPLICATION_JSON).required(true);
    private static final Parameter HEADER_ACCEPT_JSON = new HeaderParameter().name(HttpHeaders.ACCEPT)
            .schema(new StringSchema()).example(jakarta.ws.rs.core.MediaType.APPLICATION_JSON).required(true);

    @Override
    public void setConfiguration(OpenAPIConfiguration openApiConfiguration) {
        super.setConfiguration(openApiConfiguration);
    }

    @Override
    public OpenAPI read(Set<Class<?>> classes, Map<String, Object> resources) {
        openApi = super.read(classes, resources);
        ServletContext servletContext = ApiContextListener.getApplicationCntx();
        LocalDispatcher dispatcher = WebAppUtil.getDispatcher(servletContext);
        context = dispatcher.getDispatchContext();
        initializeStdOpenApiComponents();
        addPredefinedSchemas();
        addExportableServices();
        addApiResources();
        openApi.setPaths(paths);
        openApi.setComponents(components);
        return openApi;
    }

    private void addApiResources() {
        Map<String, ModelApi> apis = OFBizApiConfig.getModelApis();
        SecurityRequirement security = new SecurityRequirement();
        security.addList("jwtToken");

        apis.forEach((k, api) -> {
            if (!api.isPublish()) return;

            List<String> baseSegments = new ArrayList<>();
            baseSegments.add(api.getPath());

            for (ModelResource resource : api.getResources()) {
                processResourceRecursive(resource, baseSegments, security);
            }
        });
    }
    private void processResourceRecursive(ModelResource resource, List<String> parentSegments, SecurityRequirement security) {
        List<String> currentSegments = new ArrayList<>(parentSegments);
        currentSegments.add(resource.getPath());

        Tag resourceTag = new Tag().name(resource.getDisplayName()).description(resource.getDescription());
        openApi.addTagsItem(resourceTag);

        for (ModelOperation op : resource.getOperations()) {
            List<String> fullPathSegments = new ArrayList<>(currentSegments);
            fullPathSegments.add(op.getPath());
            String uri = buildNestedUrl(fullPathSegments);

            PathItem pathItemObject = paths.get(uri);
            boolean pathExists = pathItemObject != null;
            if (!pathExists) {
                pathItemObject = new PathItem();
            }

            String serviceName = op.getService();
            ModelService service;
            try {
                service = context.getModelService(serviceName);
            } catch (GenericServiceException e) {
                Debug.logError("Service '" + serviceName + "' not found while trying to map REST resource " + uri + "; ignoring. ", MODULE);
                continue;
            }

            Operation operation = new Operation()
                    .summary(op.getDescription())
                    .description(op.getDescription())
                    .addTagsItem(resource.getDisplayName())
                    .operationId(serviceName)
                    .deprecated(false)
                    .addSecurityItem(security);

            String verb = op.getVerb().toUpperCase();
            if (verb.equalsIgnoreCase(HttpMethod.GET)) {
                QueryParameter serviceInParam = (QueryParameter) new QueryParameter().required(true)
                        .description("Operation Input Parameters in JSON").name("input");

                Schema<?> refSchema = new Schema<>().$ref("#/components/schemas/api.request." + service.getName());
                serviceInParam.content(new Content().addMediaType(jakarta.ws.rs.core.MediaType.APPLICATION_JSON,
                        new MediaType().schema(refSchema)));
                operation.addParametersItem(serviceInParam);
            } else if (verb.matches(HttpMethod.POST + "|" + HttpMethod.PUT + "|" + HttpMethod.PATCH)) {
                RequestBody request = new RequestBody()
                        .description("Request Body for operation " + op.getDescription())
                        .content(new Content().addMediaType(jakarta.ws.rs.core.MediaType.APPLICATION_JSON,
                                new MediaType().schema(new Schema<>().$ref("#/components/schemas/api.request." + service.getName()))));
                operation.setRequestBody(request);
                operation.addParametersItem(HEADER_CONTENT_TYPE_JSON);
            }

            List<String> pathParams = RestApiUtil.getPathParameters(uri);
            for (String pathParam : pathParams) {
                ModelParam mdParam = service.getInModelParamList().stream()
                        .filter(param -> (!param.getInternal() && pathParam.equals(param.getName())))
                        .findFirst().orElse(null);
                PathParameter pathParameter = new PathParameter();
                pathParameter.setRequired(true);
                pathParameter.setName(pathParam);
                pathParameter.setDescription(mdParam != null ? mdParam.getShortDisplayDescription() : "");
                pathParameter.setSchema(OpenApiUtil.getAttributeSchema(service, mdParam));
                operation.addParametersItem(pathParameter);
            }

            addServiceOutSchema(service);
            addServiceInSchema(service);
            addServiceOperationApiResponses(service, operation);
            setPathItemOperation(pathItemObject, verb.toUpperCase(), operation);

            if (!pathExists) {
                paths.addPathItem(uri, pathItemObject);
            }
        }

        //Recursively process nested resources
        if (resource.getSubResources() != null) {
            for (ModelResource sub : resource.getSubResources()) {
                processResourceRecursive(sub, currentSegments, security);
            }
        }
    }

    public static String buildNestedUrl(List<String> segments) {
        StringBuilder pathBuilder = new StringBuilder();
        for (String segment : segments) {
            if (segment == null || segment.trim().isEmpty()) {
                continue;
            }

            // Trim leading/trailing slashes
            segment = segment.replaceAll("^/+", "").replaceAll("/+$", "");
            if (!segment.isEmpty()) {
                pathBuilder.append("/").append(segment);
            }
        }
        return pathBuilder.toString();
    }
    private void addExportableServices() {
        Set<String> serviceNames = context.getAllServiceNames();
        for (String serviceName : serviceNames) {
            ModelService service = null;
            try {
                service = context.getModelService(serviceName);
            } catch (GenericServiceException e) {
                e.printStackTrace();
            }
            if (service != null && service.isExport() && UtilValidate.isNotEmpty(service.getAction())) {
                String action = service.getAction().toUpperCase();
                SecurityRequirement security = new SecurityRequirement();
                security.addList("jwtToken");
                final Operation operation = new Operation().summary(service.getDescription())
                        .description(service.getDescription()).addTagsItem("Exported Services")
                        .operationId(service.getName()).deprecated(false).addSecurityItem(security);
                PathItem pathItemObject = new PathItem();
                if (service.getAction().equalsIgnoreCase(HttpMethod.GET)) {
                    boolean inParamsEmpty = UtilValidate.isEmpty(service.getInParamNamesMap());
                    if (!inParamsEmpty) {
                        QueryParameter serviceInParam = new QueryParameter();
                        serviceInParam.setRequired(true);
                        serviceInParam.setDescription("Operation Input Parameters in JSON");
                        serviceInParam.setName("input");

                        Schema<?> refSchema = new Schema<>();
                        refSchema.$ref("#/components/schemas/" + "api.request." + service.getName());
                        serviceInParam.content(new Content().addMediaType(jakarta.ws.rs.core.MediaType.APPLICATION_JSON,
                                new MediaType().schema(refSchema)));
                        operation.addParametersItem(serviceInParam);
                    }
                    operation.addParametersItem(HEADER_ACCEPT_JSON);
                } else if (action.matches(HttpMethod.POST + "|" + HttpMethod.PUT + "|" + HttpMethod.PATCH)) {
                    RequestBody request = new RequestBody().description("Request Body for service " + service.getName())
                            .content(new Content().addMediaType(jakarta.ws.rs.core.MediaType.APPLICATION_JSON,
                                    new MediaType().schema(new Schema<>().$ref("#/components/schemas/" + "api.request." + service.getName()))));
                    operation.setRequestBody(request);
                    operation.addParametersItem(HEADER_CONTENT_TYPE_JSON);
                }
                addServiceOutSchema(service);
                addServiceInSchema(service);
                addServiceOperationApiResponses(service, operation);
                setPathItemOperation(pathItemObject, service.getAction().toUpperCase(), operation);
                paths.addPathItem("/services/" + service.getName(), pathItemObject);
            }
        }
    }

    private void initializeStdOpenApiComponents() {
        Tag serviceResourceTag = new Tag().name("Exported Services")
                .description("OFBiz services that are exposed via REST interface with export attribute set to true");
        openApi.addTagsItem(serviceResourceTag);
        components = openApi.getComponents();
        if (components == null) {
            components = new Components();
        }
        schemas = components.getSchemas();
        if (schemas == null) {
            schemas = new HashMap<>();
            components.schemas(schemas);
        }
        paths = openApi.getPaths();
        if (paths == null) {
            paths = new Paths();
        }
    }

    private void setPathItemOperation(PathItem pathItemObject, String method, Operation operation) {
        switch (method) {
        case HttpMethod.POST:
            pathItemObject.post(operation);
            break;
        case HttpMethod.GET:
            pathItemObject.get(operation);
            break;
        case HttpMethod.DELETE:
            pathItemObject.delete(operation);
            break;
        case HttpMethod.PUT:
            pathItemObject.put(operation);
            break;
        case HttpMethod.PATCH:
            pathItemObject.patch(operation);
            break;
        case HttpMethod.HEAD:
            pathItemObject.head(operation);
            break;
        case HttpMethod.OPTIONS:
            pathItemObject.options(operation);
            break;
        default:
            // Do nothing here
            break;
        }
    }

    private void addServiceOutSchema(ModelService service) {
        schemas.put("api.response." + service.getName() + ".success", OpenApiUtil.getOutSchema(service));
    }

    private void addServiceInSchema(ModelService service) {
        schemas.put("api.request." + service.getName(), OpenApiUtil.getInSchema(service));
    }

    private void addPredefinedSchemas() {
        OpenApiUtil.getStandardApiResponseSchemas().forEach((name, schema) -> {
            schemas.put(name, schema);
        });
    }

    private void addServiceOperationApiResponses(ModelService service, Operation operation) {
        ApiResponses apiResponsesObject = new ApiResponses();
        ApiResponse successResponse = OpenApiUtil.buildSuccessResponse(service);
        apiResponsesObject.addApiResponse(String.valueOf(Response.Status.OK.getStatusCode()), successResponse);
        OpenApiUtil.getStandardApiResponses().forEach((code, response) -> {
            apiResponsesObject.addApiResponse(code, response);
        });
        operation.setResponses(apiResponsesObject);
    }

}
