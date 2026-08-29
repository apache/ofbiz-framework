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
package org.apache.ofbiz.ws.rs.util;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.model.ModelEntity;
import org.apache.ofbiz.entity.model.ModelField;
import org.apache.ofbiz.service.ModelParam;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.webapp.WebAppUtil;
import org.apache.ofbiz.ws.rs.common.AuthenticationScheme;
import org.apache.ofbiz.ws.rs.core.ResponseStatus;
import org.apache.ofbiz.ws.rs.listener.ApiContextListener;
import org.apache.ofbiz.ws.rs.model.ModelOperation;

import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.BooleanSchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.DateSchema;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MapSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.NumberSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;

public final class OpenApiUtil {

    private static final String MODULE = OpenApiUtil.class.getName();

    private OpenApiUtil() {

    }

    private static final Map<String, String> LIST_TYPE_MAP = new HashMap<>();
    private static final Map<String, String> CLASS_ALIAS = new HashMap<>();
    private static final Map<String, Class<?>> JAVA_OPEN_API_MAP = new HashMap<>();
    private static final Map<String, String> FIELD_TYPE_MAP = new HashMap<>();
    private static final Map<String, ApiResponse> RESPONSES = new HashMap<>();
    private static final Map<String, Schema<?>> SCHEMAS = new HashMap<>();
    private static final Map<String, ApiResponse> CUSTOM_RESPONSES = new HashMap<>();
    private static final Schema<?> GENERIC_ERROR_SCHEMA = new MapSchema()
            .addProperty("statusCode", new IntegerSchema().description("HTTP Status Code"))
            .addProperty("statusDescription", new StringSchema().description("HTTP Status Code Description"))
            .addProperty("errorType", new StringSchema().description("Error Type for the error"))
            .addProperty("errorMessage", new StringSchema().description("Error Message"));


    static {
        CLASS_ALIAS.put("String", "String");
        CLASS_ALIAS.put("java.lang.String", "String");
        CLASS_ALIAS.put("CharSequence", "String");
        CLASS_ALIAS.put("java.lang.CharSequence", "String");
        CLASS_ALIAS.put("Date", "String");
        CLASS_ALIAS.put("java.sql.Date", "String");
        CLASS_ALIAS.put("Time", "String");
        CLASS_ALIAS.put("java.sql.Time", "String");
        CLASS_ALIAS.put("Timestamp", "Timestamp");
        CLASS_ALIAS.put("java.sql.Timestamp", "Timestamp");
        CLASS_ALIAS.put("Integer", "Integer");
        CLASS_ALIAS.put("java.lang.Integer", "Integer");
        CLASS_ALIAS.put("Long", "Long");
        CLASS_ALIAS.put("java.lang.Long", "Long");
        CLASS_ALIAS.put("BigInteger", "BigInteger");
        CLASS_ALIAS.put("java.math.BigInteger", "BigInteger");
        CLASS_ALIAS.put("Float", "Float");
        CLASS_ALIAS.put("java.lang.Float", "Float");
        CLASS_ALIAS.put("Double", "Float");
        CLASS_ALIAS.put("java.lang.Double", "Float");
        CLASS_ALIAS.put("BigDecimal", "BigDecimal");
        CLASS_ALIAS.put("java.math.BigDecimal", "BigDecimal");
        CLASS_ALIAS.put("Boolean", "Boolean");
        CLASS_ALIAS.put("java.lang.Boolean", "Boolean");

        CLASS_ALIAS.put("org.apache.ofbiz.entity.GenericValue", "GenericValue");
        CLASS_ALIAS.put("GenericValue", "GenericValue");
        CLASS_ALIAS.put("GenericPK", "GenericPK");
        CLASS_ALIAS.put("org.apache.ofbiz.entity.GenericPK", "GenericPK");
        CLASS_ALIAS.put("org.apache.ofbiz.entity.GenericEntity", "GenericEntity");
        CLASS_ALIAS.put("GenericEntity", "GenericEntity");

        CLASS_ALIAS.put("java.util.List", "List");
        CLASS_ALIAS.put("List", "List");
        CLASS_ALIAS.put("java.util.Set", "Set");
        CLASS_ALIAS.put("Set", "Set");
        CLASS_ALIAS.put("java.util.Map", "Map");
        CLASS_ALIAS.put("Map", "Map");
        CLASS_ALIAS.put("java.util.HashMap", "HashMap");
        CLASS_ALIAS.put("HashMap", "HashMap");

        JAVA_OPEN_API_MAP.put("String", StringSchema.class);
        JAVA_OPEN_API_MAP.put("Integer", IntegerSchema.class);
        JAVA_OPEN_API_MAP.put("Long", IntegerSchema.class);
        JAVA_OPEN_API_MAP.put("Boolean", BooleanSchema.class);
        JAVA_OPEN_API_MAP.put("Map", MapSchema.class);
        JAVA_OPEN_API_MAP.put("GenericEntity", MapSchema.class);
        JAVA_OPEN_API_MAP.put("GenericPK", MapSchema.class);
        JAVA_OPEN_API_MAP.put("GenericValue", MapSchema.class);
        JAVA_OPEN_API_MAP.put("HashMap", MapSchema.class);
        JAVA_OPEN_API_MAP.put("List", ArraySchema.class);
        JAVA_OPEN_API_MAP.put("Set", ArraySchema.class);
        JAVA_OPEN_API_MAP.put("Collection", ArraySchema.class);
        JAVA_OPEN_API_MAP.put("Float", NumberSchema.class);
        JAVA_OPEN_API_MAP.put("Double", NumberSchema.class);
        JAVA_OPEN_API_MAP.put("BigDecimal", NumberSchema.class);
        JAVA_OPEN_API_MAP.put("BigInteger", IntegerSchema.class);
        JAVA_OPEN_API_MAP.put("Timestamp", DateSchema.class);

        FIELD_TYPE_MAP.put("id", "String");
        FIELD_TYPE_MAP.put("indicator", "String");
        FIELD_TYPE_MAP.put("date", "String");
        FIELD_TYPE_MAP.put("id-vlong", "String");
        FIELD_TYPE_MAP.put("description", "String");
        FIELD_TYPE_MAP.put("numeric", "Int"); //
        FIELD_TYPE_MAP.put("long-varchar", "String");
        FIELD_TYPE_MAP.put("id-long", "String");
        FIELD_TYPE_MAP.put("currency-amount", "BigDecimal");
        FIELD_TYPE_MAP.put("value", "value");
        FIELD_TYPE_MAP.put("email", "String");
        FIELD_TYPE_MAP.put("currency-precise", "BigDecimal");
        FIELD_TYPE_MAP.put("very-short", "String");
        FIELD_TYPE_MAP.put("date-time", "Timestamp");
        FIELD_TYPE_MAP.put("credit-card-date", "String");
        FIELD_TYPE_MAP.put("url", "String");
        FIELD_TYPE_MAP.put("credit-card-number", "String");
        FIELD_TYPE_MAP.put("fixed-point", "BigDecimal");
        FIELD_TYPE_MAP.put("name", "String");
        FIELD_TYPE_MAP.put("short-varchar", "String");
        FIELD_TYPE_MAP.put("comment", "String");
        FIELD_TYPE_MAP.put("time", "String");
        FIELD_TYPE_MAP.put("very-long", "String");
        FIELD_TYPE_MAP.put("floating-point", "Float");
        FIELD_TYPE_MAP.put("object", "Byte");
        FIELD_TYPE_MAP.put("byte-array", "Byte");
        FIELD_TYPE_MAP.put("blob", "Byte");

        buildApiResponseSchemas();
        buildApiResponses();
        builCustomApiResponses();
    }

    private static void buildApiResponseSchemas() {
        SCHEMAS.put("api.response.unauthorized.noheader", GENERIC_ERROR_SCHEMA);
        SCHEMAS.put("api.response.unauthorized.invalidtoken", GENERIC_ERROR_SCHEMA);
        SCHEMAS.put("api.response.forbidden", GENERIC_ERROR_SCHEMA);
        SCHEMAS.put("api.response.service.badrequest", GENERIC_ERROR_SCHEMA);
        SCHEMAS.put("api.response.service.unprocessableentity", GENERIC_ERROR_SCHEMA);
        SCHEMAS.put("api.response.service.methodnotallowed", GENERIC_ERROR_SCHEMA);
    }

    /*
     * Define your custom ApiResponses here
     */
    private static void builCustomApiResponses() {
        /**
         * Map<String, Object> customResponseExample = UtilMisc.toMap(
         *      "statusCode", ResponseStatus.Custom.MY_CUSTOM_STATUS_CODE.getStatusCode(),
         *      "statusDescription", ResponseStatus.Custom.MY_CUSTOM_STATUS_CODE.getReasonPhrase(),
         *      "errorType", "An Error Type",
         *      "errorCode", "1234",
         *      "errorMessage", "Something went Wrong.",
         *      "errorDescription", "A description of what went wrong.");
         *
         * final ApiResponse customResponse = new ApiResponse()
         * .description("Custom Error: A description of said error.")
         *       .content(new Content()
         *               .addMediaType(jakarta.ws.rs.core.MediaType.APPLICATION_JSON, new MediaType()
         *                       .schema(new Schema<>()
         *                               .$ref("#/components/schemas/" + "api.response.custom.response.example"))
         *                       .example(customResponseExample)));
         *
         * CUSTOM_RESPONSES.put(String.valueOf(ResponseStatus.Custom.MY_CUSTOM_STATUS_CODE.getStatusCode()), customResponse);
        **/
    }

    public static ApiResponse getCustomApiResponseByStatusCode(String statusCode) {
        return CUSTOM_RESPONSES.get(statusCode);
    }

    /**
     * Returns the standard API responses shared across all OFBiz REST operations.
     *
     * @return the map of standard {@link ApiResponse} instances keyed by HTTP status code
     */
    public static Map<String, ApiResponse> getStandardApiResponses() {
        return RESPONSES;
    }

    /**
     * Returns the standard API response schemas shared across all OFBiz REST operations.
     *
     * @return the map of standard {@link Schema} instances keyed by schema name
     */
    public static Map<String, Schema<?>> getStandardApiResponseSchemas() {
        return SCHEMAS;
    }

    private static void buildApiResponses() {
        Map<String, Object> unauthorizedNoHeaderExample = UtilMisc.toMap("statusCode", Response.Status.UNAUTHORIZED.getStatusCode(),
                "statusDescription", Response.Status.UNAUTHORIZED.getReasonPhrase(),
                "errorMessage", "Unauthorized: Access is denied due to invalid or absent Authorization header.");
        Map<String, Object> unauthorizedInvalidTokenExample = UtilMisc.toMap("statusCode", Response.Status.UNAUTHORIZED.getStatusCode(),
                "statusDescription", Response.Status.UNAUTHORIZED.getReasonPhrase(),
                "errorMessage", "Unauthorized: Access is denied due to invalid or absent Authorization header.");
        Map<String, Object> forbiddenExample = UtilMisc.toMap("statusCode", Response.Status.FORBIDDEN.getStatusCode(),
                "statusDescription", Response.Status.FORBIDDEN.getReasonPhrase(),
                "errorMessage", "Forbidden: Insufficient rights to perform this API call.");
        Map<String, Object> badRequestExample = UtilMisc.toMap("statusCode", Response.Status.BAD_REQUEST.getStatusCode(),
                "statusDescription", Response.Status.BAD_REQUEST.getReasonPhrase(),
                "errorType", "ServiceValidationException", "errorCode", "1000",
                "errorMessage", "createProduct validation failed. The request contained invalid information and could not be processed.",
                "errorDescription", "The following required parameter is missing: [IN] [createProduct.internalName]");
        Map<String, Object> unprocessableEntExample = UtilMisc.toMap("statusCode", ResponseStatus.Custom.UNPROCESSABLE_ENTITY.getStatusCode(),
                "statusDescription", ResponseStatus.Custom.UNPROCESSABLE_ENTITY.getReasonPhrase(),
                "errorType", "GenericEntityException", "errorCode", "2000",
                "errorMessage", "createProduct execution failed. The request contained invalid information and could not be processed.",
                "errorDescription", "StandardException: A truncation error was encountered trying to shrink CHAR 'string' to length 1.");
        Map<String, Object> methodNotAllowedExample = UtilMisc.toMap("statusCode", Response.Status.METHOD_NOT_ALLOWED.getStatusCode(),
                "statusDescription", Response.Status.METHOD_NOT_ALLOWED.getReasonPhrase(),
                "errorMessage", "HTTP POST is not allowed on service 'demoDoGetService'.");

        final ApiResponse unauthorizedNoHeader = new ApiResponse().addHeaderObject(HttpHeaders.WWW_AUTHENTICATE, new Header()
                .schema(new Schema<>().type("string").format("string")).example(HttpHeaders.WWW_AUTHENTICATE + ": "
                 + AuthenticationScheme.BEARER.getScheme() + " realm=\"" + AuthenticationScheme.REALM + "\""))
                .description("Unauthorized: Access is denied due to invalid or absent Authorization header.")
                .content(new Content()
                        .addMediaType(jakarta.ws.rs.core.MediaType.APPLICATION_JSON, new MediaType()
                                .schema(new Schema<>()
                                        .$ref("#/components/schemas/" + "api.response.unauthorized.noheader"))
                                .example(unauthorizedNoHeaderExample)));

        final ApiResponse unauthorizedInvalidToken = new ApiResponse()
                .description("Unauthorized: Access is denied due to invalid or absent Authorization header.")
                .content(new Content()
                        .addMediaType(jakarta.ws.rs.core.MediaType.APPLICATION_JSON, new MediaType()
                                .schema(new Schema<>()
                                        .$ref("#/components/schemas/" + "api.response.unauthorized.invalidtoken"))
                                .example(unauthorizedInvalidTokenExample)));

        final ApiResponse forbidden = new ApiResponse().addHeaderObject(HttpHeaders.WWW_AUTHENTICATE, new Header()
                .schema(new Schema<>().type("string"))
                .example(HttpHeaders.WWW_AUTHENTICATE + ": "
                + AuthenticationScheme.BEARER.getScheme() + " realm=\"" + AuthenticationScheme.REALM + "\""))
                .description("Forbidden: Insufficient rights to perform this API call.")
                .content(new Content()
                        .addMediaType(jakarta.ws.rs.core.MediaType.APPLICATION_JSON, new MediaType()
                                .schema(new Schema<>().$ref("#/components/schemas/" + "api.response.forbidden"))
                                .example(forbiddenExample)));

        final ApiResponse badRequest = new ApiResponse()
                .description("Bad Request: Due to malformed request syntax or invalid request message framing or incorrect request parameters.")
                .content(new Content()
                        .addMediaType(jakarta.ws.rs.core.MediaType.APPLICATION_JSON, new MediaType()
                                .schema(new Schema<>()
                                        .$ref("#/components/schemas/" + "api.response.service.badrequest"))
                                .example(badRequestExample)));

        final ApiResponse unprocessableEntity = new ApiResponse()
                .description("Unprocessable Entity: Error indicating semantical errors. Request is syntactically correct though.")
                .content(new Content()
                        .addMediaType(jakarta.ws.rs.core.MediaType.APPLICATION_JSON, new MediaType()
                                .schema(new Schema<>()
                                        .$ref("#/components/schemas/" + "api.response.service.unprocessableentity"))
                                .example(unprocessableEntExample)));

        final ApiResponse methodNotAllowed = new ApiResponse()
                .description("Method Not Allowed: Service called with HTTP method other than the declared one.")
                .content(new Content()
                        .addMediaType(jakarta.ws.rs.core.MediaType.APPLICATION_JSON, new MediaType()
                                .schema(new Schema<>()
                                        .$ref("#/components/schemas/" + "api.response.service.methodnotallowed"))
                                .example(methodNotAllowedExample)));

        RESPONSES.put(String.valueOf(Response.Status.UNAUTHORIZED.getStatusCode()), unauthorizedNoHeader);
        RESPONSES.put(String.valueOf(Response.Status.UNAUTHORIZED.getStatusCode()), unauthorizedInvalidToken);
        RESPONSES.put(String.valueOf(Response.Status.FORBIDDEN.getStatusCode()), forbidden);
        RESPONSES.put(String.valueOf(Response.Status.BAD_REQUEST.getStatusCode()), badRequest);
        RESPONSES.put(String.valueOf(ResponseStatus.Custom.UNPROCESSABLE_ENTITY.getStatusCode()), unprocessableEntity);
        RESPONSES.put(String.valueOf(Response.Status.METHOD_NOT_ALLOWED.getStatusCode()), methodNotAllowed);
    }

    /**
     * Returns the OpenAPI {@link Class} type mapped to the given OFBiz service
     * attribute type.
     *
     * @param attributeType the OFBiz attribute type (e.g. {@code "String"}, {@code "Long"})
     * @return the corresponding OpenAPI schema class, or {@code null} if no mapping exists
     */
    public static Class<?> getOpenApiTypeForAttributeType(String attributeType) {
        if (isTypeDomainModel(attributeType)) {
            return MapSchema.class;
        }
        return JAVA_OPEN_API_MAP.get(CLASS_ALIAS.get(attributeType));
    }

    /**
     * Returns the OpenAPI {@link Class} type mapped to the given OFBiz entity
     * field type.
     *
     * @param fieldType the OFBiz entity field type (e.g. {@code "date-time"}, {@code "numeric"})
     * @return the corresponding OpenAPI schema class, or {@code null} if no mapping exists
     */
    public static Class<?> getOpenApiTypeForFieldType(String fieldType) {
        return JAVA_OPEN_API_MAP.get(FIELD_TYPE_MAP.get(fieldType));
    }

    /**
     * Builds the OpenAPI request body schema for the input parameters of the
     * given OFBiz service.
     *
     * <p>Each input parameter is mapped to an OpenAPI property. Parameters
     * that are not optional are added to the required list. Parameters without
     * a known OpenAPI type mapping are silently skipped.</p>
     *
     * @param service the {@link ModelService} for which to build the input schema
     * @return an OpenAPI {@link Schema} representing the service request body
     */
    public static Schema<Object> getInSchema(ModelService service, ModelOperation op) {
        Schema<Object> parentSchema = new Schema<Object>();
        parentSchema.setDescription("In Schema for service: " + service.getName() + " request");
        parentSchema.setType("object");
        List<String> required = UtilMisc.toList();
        service.getInParamNamesMap().forEach((name, type) -> {
            if (!RestApiUtil.SECURITY_PARAMS.contains(name) && (op == null || (op != null && !op.getCustomHeadersList().contains(name)))) {
                ModelParam param = service.getParam(name);
                if (!param.isOptional()) {
                    required.add(name);
                }
                Schema<?> attrSchema = getAttributeSchema(service, param);
                if (attrSchema != null) {
                    parentSchema.addProperty(name, attrSchema);
                }
            }
        });
        parentSchema.setRequired(required);
        return parentSchema;
    }

    /**
     * Builds the OpenAPI schema for a single OFBiz service parameter.
     *
     * <p>The schema type is determined by mapping the parameter's OFBiz type
     * to an OpenAPI type. Returns {@code null} and logs a warning if no mapping
     * is found, or if a {@code Map} type parameter has no entity name or child
     * attributes defined.</p>
     *
     * @param service the {@link ModelService} owning the parameter
     * @param param   the {@link ModelParam} to build a schema for
     * @return the OpenAPI {@link Schema} for the parameter, or {@code null} if
     *         the parameter cannot be mapped
     */
    @SuppressWarnings("deprecation")
    public static Schema<?> getAttributeSchema(ModelService service, ModelParam param) {
        Schema<?> schema = null;
        if (param == null) {
            Debug.logWarning("ModelParam is null - ignored.", MODULE);
            return null;
        }
        Class<?> schemaClass = getOpenApiTypeForAttributeType(param.getType());
        if (schemaClass == null) {
            Debug.logWarning("Attribute '" + param.getName() + "' ignored as it is declared as '" + param.getType()
                    + "' and corresponding OpenApi Type Mapping not found.", MODULE);
            return null;
        }
        try {
            schema = (Schema<?>) schemaClass.getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException
                | SecurityException e) {
            Debug.logError(e, MODULE);
        }

        List<ModelParam> children = param.getChildren();
        Delegator delegator = WebAppUtil.getDelegator(ApiContextListener.getApplicationCntx());
        if (schema instanceof ArraySchema) {
            ArraySchema arrSch = (ArraySchema) schema;
            if (LIST_TYPE_MAP.containsKey(param.getName())) {
                arrSch.setItems(getSchemaForModel(LIST_TYPE_MAP.get(param.getName())));
            } else {
                arrSch.setItems(children.size() > 0 ? getAttributeSchema(service, children.get(0)) : new StringSchema());
            }
        } else if (schema instanceof MapSchema) {
            if (isTypeGenericEntityOrGenericValue(param.getType())) {
                if (UtilValidate.isEmpty(param.getEntityName())) {
                    Debug.logWarning(
                            "Attribute '" + param.getName() + "' ignored as it is declared as '" + param.getType() + "' but does not have "
                            + "entity-name defined.",
                            MODULE);
                    return null;
                }
                schema = getSchemaForEntity(delegator.getModelEntity(param.getEntityName()));
            } else if (isTypeDomainModel(param.getType())) {
                schema = getSchemaForModel(param.getType());
            } else if (UtilValidate.isEmpty(param.getChildren())) {
                Debug.logWarning(
                        "Attribute '" + param.getName() + "' ignored as it is declared as '" + param.getType() + "' but does not have "
                        + "any child attributes.",
                        MODULE);
                return null;
            } else {
                List<String> required = UtilMisc.toList();
                for (ModelParam childParam : children) {
                    if (!param.isOptional()) {
                        required.add(childParam.getName());
                    }
                    schema.addProperty(childParam.getName(), getAttributeSchema(service, childParam));
                }
                schema.setRequired(required);
            }

        }
        return schema;
    }

    /**
     * Builds the OpenAPI response body schema for the output parameters of the
     * given OFBiz service.
     *
     * <p>The response schema always includes {@code statusCode},
     * {@code statusDescription}, and {@code successMessage} properties.
     * Service output parameters are nested under a {@code data} property.</p>
     *
     * @param service the {@link ModelService} for which to build the output schema
     * @return an OpenAPI {@link Schema} representing the service response body
     */
    public static Schema<Object> getOutSchema(ModelService service) {
        Schema<Object> parentSchema = new Schema<Object>();
        parentSchema.setDescription("Out Schema for service: " + service.getName() + " response");
        parentSchema.setType("object");
        parentSchema.addProperty("statusCode", new IntegerSchema().description("HTTP Status Code"));
        parentSchema.addProperty("statusDescription", new StringSchema().description("HTTP Status Code Description"));
        parentSchema.addProperty("successMessage", new StringSchema().description("Success Message"));
        Schema<Object> dataSchema = new Schema<Object>();
        parentSchema.addProperty("data", dataSchema);
        service.getOutParamNamesMap().forEach((name, type) -> {
            if (!name.equals(RestApiUtil.RESPONSE_STATUS_KEY)) {
                Schema<?> attrSchema = getAttributeSchema(service, service.getParam(name));
                if (attrSchema != null) {
                    dataSchema.addProperty(name, getAttributeSchema(service, service.getParam(name)));
                }
            }
        });
        return parentSchema;
    }

    public static Schema<?> getGenericErrorSchema(ModelService service) {
        return GENERIC_ERROR_SCHEMA;
    }

    private static boolean isTypeGenericEntityOrGenericValue(String type) {
        if (type == null) {
            return false;
        }
        return type.matches("org.apache.ofbiz.entity.GenericValue|GenericValue|org.apache.ofbiz.entity.GenericEntity|GenericEntity");
    }

    private static boolean isTypeDomainModel(String className) {
        if (className == null || CLASS_ALIAS.containsKey(className)) {
            return false;
        }
        try {
            Class<?> modelClass = Class.forName(className);
            Class<?> domainModelClass = Class.forName("org.apache.ofbiz.base.model.DomainModel");
            return domainModelClass.isAssignableFrom(modelClass);
        } catch (ClassNotFoundException e) {
            Debug.logInfo("Class not found while checking DomainModel type: " + className, MODULE);
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private static Schema<?> getSchemaForModel(String className) {
        Schema<?> dataSchema = new Schema<>();
        dataSchema.setType("object");
        try {
            Class<?> modelClass = Class.forName(className);
            List<Field> fields = getFields(modelClass);
            for (Field field : fields) {
                String fieldNm = field.getName();
                Class<?> fieldType = field.getType();
                Schema<?> schema = null;
                Class<?> schemaClass = getOpenApiTypeForAttributeType(fieldType.getName());
                if (schemaClass == null) {
                    continue;
                }
                try {
                    schema = (Schema<?>) schemaClass.getDeclaredConstructor().newInstance();
                    //@Schema(minLength = 1, maxLength = 20, nullable = false, example="10001", description = "Interne Party ID")
                    io.swagger.v3.oas.annotations.media.Schema fieldAnnotation =
                            field.getAnnotation(io.swagger.v3.oas.annotations.media.Schema.class);
                    if (fieldAnnotation != null) {
                        if (UtilValidate.isNotEmpty(fieldAnnotation.name())) {
                            schema.setName(fieldAnnotation.name());
                        }
                        schema.setDescription(fieldAnnotation.description() != null ? fieldAnnotation.description() : fieldNm);
                        if (UtilValidate.isNotEmpty(fieldAnnotation.example())) {
                            schema.setExample(fieldAnnotation.example());
                        }
                        if (UtilValidate.isNotEmpty(fieldAnnotation.minLength())) {
                            schema.setMinLength(fieldAnnotation.minLength());
                        }
                        if (UtilValidate.isNotEmpty(fieldAnnotation.maxLength())) {
                            schema.setMaxLength(fieldAnnotation.maxLength());
                        }
                        if (UtilValidate.isNotEmpty(fieldAnnotation.defaultValue())) {
                            schema.setDefault(fieldAnnotation.defaultValue());
                        }
                        if (UtilValidate.isNotEmpty(fieldAnnotation.format())) {
                            schema.setFormat(fieldAnnotation.format());
                        }
                        if (UtilValidate.isNotEmpty(fieldAnnotation.nullable())) {
                            schema.setNullable(fieldAnnotation.nullable());
                        }
                    }

                    if (schema instanceof ArraySchema) {
                        ParameterizedType genericType = (ParameterizedType) field.getGenericType();
                        Class<? extends Type> genericClass = (Class<? extends Type>) genericType.getActualTypeArguments()[0];
                        ArraySchema arrSch = (ArraySchema) schema;
                        Class<?> listSchemaClass = getOpenApiTypeForAttributeType(genericClass.getName());
                        Schema<?> listSchema = null;
                        if (listSchemaClass == null || isTypeDomainModel(genericClass.getName())) {
                            arrSch.setItems(getSchemaForModel(genericClass.getName()));
                        } else {
                            listSchema = (Schema<?>) listSchemaClass.getDeclaredConstructor().newInstance();
                            arrSch.setItems(listSchema);
                        }
                        dataSchema.addProperty(fieldNm, arrSch);
                    } else if (schema instanceof MapSchema) {
                        if (isTypeDomainModel(fieldType.getName())) {
                            schema = getSchemaForModel(fieldType.getName());
                            dataSchema.addProperty(fieldNm, schema);
                        }
                    } else {
                        dataSchema.addProperty(fieldNm, schema.description(fieldNm));
                    }
                } catch (InstantiationException | IllegalAccessException | NoSuchMethodException
                        | InvocationTargetException e) {
                    Debug.logError(e, "Error evaluating class [%s].", MODULE, className);
                }
            }
        } catch (ClassNotFoundException e1) {
            Debug.logError(e1, "Error evaluating class [%s].", MODULE, className);
        }
        return dataSchema;
    }

    private static <T> List<Field> getFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        while (clazz != Object.class) {
            fields.addAll(0, Arrays.asList(clazz.getDeclaredFields()));
            clazz = clazz.getSuperclass();
        }
        return fields;
    }

    public static Map<String, String> getListTypes() {
        return LIST_TYPE_MAP;
    }

    private static Schema<?> getSchemaForEntity(ModelEntity entity) {
        Schema<?> dataSchema = new Schema<>();
        dataSchema.setType("object");
        List<String> fields = entity.getAllFieldNames();
        for (String fieldNm : fields) {
            ModelField field = entity.getField(fieldNm);
            Schema<?> schema = null;
            Class<?> schemaClass = getOpenApiTypeForFieldType(field.getType());
            if (schemaClass == null) {
                continue;
            }
            try {
                schema = (Schema<?>) schemaClass.getDeclaredConstructor().newInstance();
            } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException
                    | SecurityException e) {
                Debug.logError(e, MODULE);
            }
            if (schemaClass != null) {
                dataSchema.addProperty(fieldNm, schema.description(fieldNm));
            }
        }
        return dataSchema;
    }

    /**
     * Builds a standard HTTP 200 success {@link ApiResponse} referencing the
     * OpenAPI response schema for the given service.
     *
     * @param service the {@link ModelService} for which to build the success response
     * @param op {@link ModelOperation}
     * @return an {@link ApiResponse} with a JSON media type and a schema reference
     *         to {@code #/components/schemas/api.response.<serviceName>.success}
     */
    public static ApiResponse buildSuccessResponse(ModelService service, ModelOperation op) {
        final MediaType mediaType = new MediaType()
                .schema(new Schema<>().$ref("#/components/schemas/" + "api.response." + service.getName() + ".success"));

        if (op != null) {
            mediaType.setExample(op.getExampleObject("response", "200"));
        }

        return new ApiResponse()
                .description("Success response for the API call.")
                .content(new Content()
                        .addMediaType(javax.ws.rs.core.MediaType.APPLICATION_JSON, mediaType));
    }
}
