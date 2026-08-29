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

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ModelParam;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.webapp.WebAppUtil;
import org.apache.ofbiz.ws.rs.core.OFBizApiConfig;
import org.apache.ofbiz.ws.rs.core.ResponseStatus;
import org.apache.ofbiz.ws.rs.listener.ApiContextListener;
import org.apache.ofbiz.ws.rs.model.ModelApi;
import org.apache.ofbiz.ws.rs.model.ModelMapping;
import org.apache.ofbiz.ws.rs.model.ModelOperation;
import org.apache.ofbiz.ws.rs.model.ModelQueryParam;
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
import jakarta.servlet.ServletContext;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.StatusType;

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
    @SuppressWarnings("deprecation")
    public OpenAPI read(Set<Class<?>> classes, Map<String, Object> resources) {
        openApi = super.read(classes, resources);
        ServletContext servletContext = ApiContextListener.getApplicationCntx();
        LocalDispatcher dispatcher = WebAppUtil.getDispatcher(servletContext);
        context = dispatcher.getDispatchContext();
        initializeStdOpenApiComponents();
        addPredefinedSchemas();
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

            List<ModelMapping> mappings = api.getMappings();
            mappings.forEach(modelMapping -> {
                OpenApiUtil.getListTypes().put(modelMapping.getName(), modelMapping.getClassName());
            });
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
            String verb = op.getVerb().toUpperCase();
            if (verb.equalsIgnoreCase(HttpMethod.GET)) {
                List<ModelQueryParam> queryParams = op.getQueryParams();

                for (ModelQueryParam queryParam : queryParams) {
                    if (pathParams.contains(queryParam.getName())) {
                        Debug.logWarning("Query parameter '%s' for Service '%s' is already defined as path parameter, ignoring.", MODULE,
                                queryParam.getName(), service.getName());
                    } else {
                        ModelParam mdParam = service.getInModelParamList().stream().filter(param -> (
                                        !param.getInternal() && queryParam.getName().equals(param.getName()))).findFirst().orElse(null);
                        if (mdParam != null) {
                            final QueryParameter serviceInParam = (QueryParameter) new QueryParameter()
                                    .required(!mdParam.isOptional())
                                    .description(UtilValidate.isNotEmpty(queryParam.getDescription())
                                            ? queryParam.getDescription() : mdParam.getDescription())
                                    .name(queryParam.getName())
                                    .schema(new Schema<>().type(queryParam.getType()));
                            operation.addParametersItem(serviceInParam);
                        } else {
                            Debug.logWarning("Query parameter '%s' for Service '%s' not found in service definition, ignoring.", MODULE,
                                    queryParam.getName(), service.getName());
                        }
                    }
                }
            } else if (verb.matches(HttpMethod.POST + "|" + HttpMethod.PUT + "|" + HttpMethod.PATCH)) {
                RequestBody request = new RequestBody()
                        .description("Request Body for operation " + op.getDescription())
                        .content(new Content().addMediaType(jakarta.ws.rs.core.MediaType.APPLICATION_JSON,
                                new MediaType().schema(new Schema<>().$ref("#/components/schemas/api.request." + service.getName()))));
                operation.setRequestBody(request);
                operation.addParametersItem(HEADER_CONTENT_TYPE_JSON);
            }


            addServiceOutSchema(service);
            addServiceInSchema(service, op);
            addServiceOperationApiResponses(service, op, operation);
            addAdditionalOperationApiResponses(service, op, operation);
            addCustomHeaders(op, operation);
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

    private void initializeStdOpenApiComponents() {
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

    private void addServiceInSchema(ModelService service, ModelOperation op) {
        schemas.put("api.request." + service.getName(), OpenApiUtil.getInSchema(service, op));
    }

    private void addServiceInSchema(ModelService service) {
        schemas.put("api.request." + service.getName(), OpenApiUtil.getInSchema(service, null));
    }

    private void addPredefinedSchemas() {
        OpenApiUtil.getStandardApiResponseSchemas().forEach((name, schema) -> {
            schemas.put(name, schema);
        });
    }

    private void addServiceOperationApiResponses(ModelService service, ModelOperation op, Operation operation) {
        ApiResponses apiResponsesObject = new ApiResponses();
        ApiResponse successResponse = OpenApiUtil.buildSuccessResponse(service, op);
        apiResponsesObject.addApiResponse(String.valueOf(Response.Status.OK.getStatusCode()), successResponse);
        OpenApiUtil.getStandardApiResponses().forEach((code, response) -> {
            apiResponsesObject.addApiResponse(code, response);
        });
        operation.setResponses(apiResponsesObject);
    }

    private void addAdditionalOperationApiResponses(ModelService service, ModelOperation op, Operation operation) {
        ApiResponses apiResponsesObject = operation.getResponses();

        if (apiResponsesObject == null) {
            apiResponsesObject = new ApiResponses();
        }

        final ApiResponses apiResponsesObjectCopy = apiResponsesObject;
        op.getAddApiResponsesList().forEach((statusCode) -> {

            StatusType statusType = Response.Status.fromStatusCode(Integer.valueOf(statusCode));
            if (statusType == null) {
                statusType = ResponseStatus.Custom.fromStatusCode(Integer.valueOf(statusCode));
            }

            if (statusType != null) {
                String schemaName = "";
                ApiResponse customResponse = OpenApiUtil.getCustomApiResponseByStatusCode(statusCode);
                if (customResponse != null) {
                    apiResponsesObjectCopy.addApiResponse(statusCode, customResponse);
                    Schema<?> schema = customResponse.getContent()
                            .get(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
                            .getSchema();

                    String ref = schema.get$ref();
                    schemaName = ref.substring(ref.lastIndexOf('/') + 1);
                } else {
                    schemaName = "api.response.service.".concat(service.getName()).concat(".").concat(String.valueOf(statusType.getStatusCode()));

                    ApiResponse response = new ApiResponse()
                            .description(statusType.getReasonPhrase())
                            .content(new Content()
                                    .addMediaType(javax.ws.rs.core.MediaType.APPLICATION_JSON, new MediaType()
                                            .schema(new Schema<>().$ref("#/components/schemas/" + schemaName))
                                            .example(op.getExampleObject("response", String.valueOf(statusType.getStatusCode())))));
                    apiResponsesObjectCopy.addApiResponse(statusCode, response);
                }

                if (statusType.getStatusCode() > 399) {
                    schemas.put(schemaName, OpenApiUtil.getGenericErrorSchema(null));
                } else {
                    schemas.put(schemaName, OpenApiUtil.getOutSchema(service));
                }

            }
        });
    }

    private void addCustomHeaders(ModelOperation op, Operation operation) {
        op.getCustomHeadersList().forEach((headerName) -> {
            operation.addParametersItem(new HeaderParameter().name(headerName).schema(new StringSchema()).required(true));
        });
    }
}
