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
import io.swagger.v3.oas.models.media.ObjectSchema;

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
//        addEntityResources();
        openApi.setPaths(paths);
        openApi.setComponents(components);
        return openApi;
    }

    private void addApiResources() {
        Map<String, ModelApi> apis = OFBizApiConfig.getModelApis();
        SecurityRequirement security = new SecurityRequirement();
        security.addList("jwtToken");
        apis.forEach((k, v) -> {
            if (!v.isPublish()) {
                return;
            }
            List<ModelResource> resources = v.getResources();
            resources.forEach(modelResource -> {
                Tag resourceTab = new Tag().name(modelResource.getDisplayName()).description(modelResource.getDescription());
                openApi.addTagsItem(resourceTab);
                String basePath = modelResource.getPath();
                for (ModelOperation op : modelResource.getOperations()) {
                    String uri = basePath + op.getPath();
                    boolean pathExists = false;
                    PathItem pathItemObject = paths.get(uri);
                    if (UtilValidate.isEmpty(pathItemObject)) {
                        pathItemObject = new PathItem();
                    } else {
                        pathExists = true;
                    }
                    String serviceName = op.getService();
                    final Operation operation = new Operation().summary(op.getDescription())
                            .description(op.getDescription()).addTagsItem(modelResource.getDisplayName())
                            .operationId(serviceName).deprecated(false).addSecurityItem(security);
                    String verb = op.getVerb().toUpperCase();
                    ModelService service = null;
                    try {
                        service = context.getModelService(serviceName);
                    } catch (GenericServiceException e) {
                        Debug.logError("Service '" + serviceName + "' not found while trying to map REST resource " + uri + "; ignoring. ", MODULE);
                        continue;
                    }
                    if (verb.equalsIgnoreCase(HttpMethod.GET)) {
                        final QueryParameter serviceInParam = (QueryParameter) new QueryParameter().required(true)
                                .description("Operation Input Parameters in JSON").name("input");
                        Schema<?> refSchema = new Schema<>();
                        refSchema.$ref("#/components/schemas/" + "api.request." + service.getName());
                        serviceInParam.content(new Content().addMediaType(jakarta.ws.rs.core.MediaType.APPLICATION_JSON,
                                new MediaType().schema(refSchema)));
                        operation.addParametersItem(serviceInParam);
                    } else if (verb.matches(HttpMethod.POST + "|" + HttpMethod.PUT + "|" + HttpMethod.PATCH)) {
                        RequestBody request = new RequestBody()
                                .description("Request Body for operation " + op.getDescription())
                                .content(new Content().addMediaType(jakarta.ws.rs.core.MediaType.APPLICATION_JSON,
                                        new MediaType().schema(new Schema<>()
                                                .$ref("#/components/schemas/" + "api.request." + service.getName()))));
                        operation.setRequestBody(request);
                        operation.addParametersItem(HEADER_CONTENT_TYPE_JSON);
                    }
                    List<String> pathParams = RestApiUtil.getPathParameters(uri);
                    for (String pathParam : pathParams) {
                        ModelParam mdParam = service.getInModelParamList().stream()
                                .filter(param -> (!param.getInternal() && pathParam.equals(param.getName())))
                                .findFirst().orElse(null);
                        final PathParameter pathParameter = (PathParameter) new PathParameter().required(true)
                                .description(mdParam != null ? mdParam.getShortDisplayDescription() : "")
                                .name(pathParam)
                                .schema(OpenApiUtil.getAttributeSchema(service, mdParam));
                        operation.addParametersItem(pathParameter);
                    }
                    addServiceOutSchema(service);
                    addServiceInSchema(service);
                    addServiceOperationApiResponses(service, operation);
                    setPathItemOperation(pathItemObject, verb.toUpperCase(), operation);
                    if (!pathExists) {
                        paths.addPathItem(basePath + op.getPath(), pathItemObject);
                    }
                }
            });
        });
    }

    private void addEntityResources() {
        SecurityRequirement security = new SecurityRequirement();
        security.addList("jwtToken");

        String basePath = "/entity";
        Tag entityTag = new Tag().name("Entity API").description("OFBiz Generic Entity REST API");
        openApi.addTagsItem(entityTag);

        // Path parameter for entity name
        PathParameter entityNameParam = new PathParameter();
        entityNameParam.setName("entityName");
        entityNameParam.setDescription("Name of the OFBiz entity (e.g., GlAccount, Person, OrderHeader)");
        entityNameParam.setRequired(true);
        entityNameParam.setSchema(new StringSchema());

        // Pagination parameters
        QueryParameter viewSizeParam = new QueryParameter();
        viewSizeParam.setName("pageSize");
        viewSizeParam.setDescription("Number of records per page (pagination)");
        viewSizeParam.setRequired(false);
        viewSizeParam.setSchema(new StringSchema());

        QueryParameter viewIndexParam = new QueryParameter();
        viewIndexParam.setName("pageIndex");
        viewIndexParam.setDescription("Page offset (0-based index)");
        viewIndexParam.setRequired(false);
        viewIndexParam.setSchema(new StringSchema());

        // Standard GET operation for entity list
        PathItem getEntityPath = new PathItem();
        Operation getOperation = new Operation();
        getOperation.setSummary("Query entities");
        getOperation.setDescription("Retrieve entities filtered by field-value pairs in query parameters");
        getOperation.addTagsItem("Entity API");
        getOperation.setOperationId("queryEntities");
        getOperation.addSecurityItem(security);
        getOperation.addParametersItem(entityNameParam);
        getOperation.addParametersItem(viewSizeParam);
        getOperation.addParametersItem(viewIndexParam);
        getOperation.addParametersItem(HEADER_ACCEPT_JSON);

        // Dynamic field filtering note
        getOperation.setDescription(getOperation.getDescription()
                + "\n\nFilter using any entity field as query parameter (e.g., ?glAccountTypeId=CURRENT_ASSET)");

        ApiResponses getResponses = new ApiResponses();
        getResponses.addApiResponse("200", new ApiResponse().description("Successfully retrieved entities"));
        getResponses.addApiResponse("404", new ApiResponse().description("Entity not found"));
        getOperation.setResponses(getResponses);
        getEntityPath.setGet(getOperation);

        paths.addPathItem(basePath + "/{entityName}", getEntityPath);

        // GET operation for single entity by primary key
        PathItem getEntityByIdPath = new PathItem();
        PathParameter pkValueParam = new PathParameter();
        pkValueParam.setName("pkValue");
        pkValueParam.setDescription("Primary key value (e.g., CURRENT_ASSET for GlAccountType)");
        pkValueParam.setRequired(true);
        pkValueParam.setSchema(new StringSchema());

        Operation getByIdOperation = new Operation();
        getByIdOperation.setSummary("Get entity by primary key");
        getByIdOperation.setDescription("Retrieve a single entity by its primary key");
        getByIdOperation.addTagsItem("Entity API");
        getByIdOperation.setOperationId("getEntityById");
        getByIdOperation.addSecurityItem(security);
        getByIdOperation.addParametersItem(entityNameParam);
        getByIdOperation.addParametersItem(pkValueParam);
        getByIdOperation.addParametersItem(HEADER_ACCEPT_JSON);

        ApiResponses getByIdResponses = new ApiResponses();
        getByIdResponses.addApiResponse("200", new ApiResponse().description("Successfully retrieved entity"));
        getByIdResponses.addApiResponse("404", new ApiResponse().description("Entity not found"));
        getByIdOperation.setResponses(getByIdResponses);
        getEntityByIdPath.setGet(getByIdOperation);

        paths.addPathItem(basePath + "/{entityName}/{pkValue}", getEntityByIdPath);

        // --- POST: Create Entity ---
        Operation postOperation = new Operation()
                .summary("Create a new entity")
                .description("Create a new entity by posting its field values")
                .operationId("createEntity")
                .addTagsItem("Entity API")
                .addSecurityItem(security)
                .addParametersItem(entityNameParam)
                .requestBody(new RequestBody()
                        .required(true)
                        .content(new Content().addMediaType("application/json", new MediaType()
                                .schema(new ObjectSchema().additionalProperties(new StringSchema())))))
                .responses(new ApiResponses()
                        .addApiResponse("201", new ApiResponse().description("Entity created"))
                        .addApiResponse("400", new ApiResponse().description("Error creating entity")));

        PathItem postPath = paths.get(basePath + "/{entityName}");
        if (postPath == null) postPath = new PathItem();
        postPath.post(postOperation);
        paths.addPathItem(basePath + "/{entityName}", postPath);

// --- PUT: Update Entity ---
        Operation putOperation = new Operation()
                .summary("Update an existing entity")
                .description("Update an entity using its primary key and new field values")
                .operationId("updateEntity")
                .addTagsItem("Entity API")
                .addSecurityItem(security)
                .addParametersItem(entityNameParam)
                .addParametersItem(pkValueParam)
                .requestBody(new RequestBody()
                        .required(true)
                        .content(new Content().addMediaType("application/json", new MediaType()
                                .schema(new ObjectSchema().additionalProperties(new StringSchema())))))
                .responses(new ApiResponses()
                        .addApiResponse("200", new ApiResponse().description("Entity updated"))
                        .addApiResponse("404", new ApiResponse().description("Entity not found"))
                        .addApiResponse("400", new ApiResponse().description("Error updating entity")));

        PathItem putPath = paths.get(basePath + "/{entityName}/{pk}");
        if (putPath == null) putPath = new PathItem();
        putPath.put(putOperation);
        paths.addPathItem(basePath + "/{entityName}/{pk}", putPath);

        // --- DELETE: Delete Entity ---
        Operation deleteOperation = new Operation()
                .summary("Delete an entity")
                .description("Delete an entity using its primary key")
                .operationId("deleteEntity")
                .addTagsItem("Entity API")
                .addSecurityItem(security)
                .addParametersItem(entityNameParam)
                .addParametersItem(pkValueParam)
                .responses(new ApiResponses()
                        .addApiResponse("204", new ApiResponse().description("Entity deleted"))
                        .addApiResponse("404", new ApiResponse().description("Entity not found"))
                        .addApiResponse("400", new ApiResponse().description("Error deleting entity")));

        PathItem deletePath = paths.get(basePath + "/{entityName}/{pk}");
        if (deletePath == null) deletePath = new PathItem();
        deletePath.delete(deleteOperation);
        paths.addPathItem(basePath + "/{entityName}/{pk}", deletePath);

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
                        final QueryParameter serviceInParam = (QueryParameter) new QueryParameter()
                                .required(!inParamsEmpty)
                                .description("Service In Parameters in JSON").name("inParams");
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
