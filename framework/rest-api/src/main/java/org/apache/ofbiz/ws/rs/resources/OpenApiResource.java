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

import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.ws.rs.openapi.OFBizOpenApiReader;
import org.apache.ofbiz.ws.rs.openapi.OFBizResourceScanner;

import io.swagger.v3.core.util.Json;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.integration.GenericOpenApiContextBuilder;
import io.swagger.v3.oas.integration.SwaggerConfiguration;
import io.swagger.v3.oas.integration.api.OpenApiContext;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

@Path("/openapi.{type:json|yaml}")
public final class OpenApiResource {
    @Context
    private ServletConfig config;

    @Context
    private ServletContext context;

    @Context
    private HttpServletRequest request;

    @Context
    private Application app;

    /**
     * Generates the OpenAPI specification for OFBiz REST resources.
     *
     * <p>This endpoint dynamically builds the OpenAPI document by scanning all
     * JAX-RS resources in the configured package and applying OFBiz-specific
     * OpenAPI readers and scanners.</p>
     *
     * <p>The generated specification includes configured security schemes for:
     * <ul>
     *     <li>JWT Bearer token authentication</li>
     *     <li>HTTP Basic authentication</li>
     * </ul>
     *
     * <p>The response format can be returned as either JSON or YAML depending on
     * the requested type.</p>
     *
     * <p>This endpoint is marked as hidden in OpenAPI documentation to avoid
     * recursive self-inclusion in the generated spec.</p>
     *
     * @param headers HTTP headers of the request (currently unused but available for extensions)
     * @param uriInfo URI context of the request
     * @param type optional response format selector ("yaml" for YAML output, otherwise JSON)
     * @return the generated OpenAPI specification in JSON or YAML format
     * @throws Exception if OpenAPI generation or serialization fails
     */
    @GET
    @Produces({MediaType.APPLICATION_JSON, "application/yaml"})
    @Operation(hidden = true)
    public Response getOpenApi(@Context HttpHeaders headers, @Context UriInfo uriInfo, @PathParam("type") String type)
            throws Exception {
        OpenAPI openApi = new OpenAPI();
        openApi.addServersItem(buildOpenApiServer());

        SecurityScheme securitySchemeBearer = new SecurityScheme();
        securitySchemeBearer.setName("jwtToken");
        securitySchemeBearer.setType(SecurityScheme.Type.HTTP);
        securitySchemeBearer.setScheme("bearer");
        securitySchemeBearer.bearerFormat("JWT");
        openApi.schemaRequirement(securitySchemeBearer.getName(), securitySchemeBearer);

        SecurityScheme basicAuthScheme = new SecurityScheme();
        basicAuthScheme.setName("basicAuth");
        basicAuthScheme.setType(SecurityScheme.Type.HTTP);
        basicAuthScheme.setScheme("basic");
        openApi.schemaRequirement(basicAuthScheme.getName(), basicAuthScheme);


        SwaggerConfiguration config = new SwaggerConfiguration().openAPI(openApi.info(buildOpenApiInfo()))
                .readerClass(OFBizOpenApiReader.class.getName())
                .resourcePackages(Stream.of("org.apache.ofbiz.ws.rs.resources").collect(Collectors.toSet()))
                .scannerClass(OFBizResourceScanner.class.getName());


        OpenApiContext ctx = new GenericOpenApiContextBuilder<>().openApiConfiguration(config)
                .buildContext(true);

        openApi = ctx.read();

        if (UtilValidate.isNotEmpty(type) && type.trim().equalsIgnoreCase("yaml")) {
            return Response.status(Response.Status.OK)
                    .entity(Yaml.mapper().writeValueAsString(openApi))
                    .type("application/yaml").build();
        }
        return Response.status(Response.Status.OK)
                .entity(Json.mapper().writeValueAsString(openApi))
                .type(MediaType.APPLICATION_JSON_TYPE).build();
    }


    private Info buildOpenApiInfo() {
        Info info = new Info()
                .version(UtilProperties.getPropertyValue("rest-api", "rest-api.info.version"))
                .title(UtilProperties.getPropertyValue("rest-api", "rest-api.info.title"))
                .description(UtilProperties.getPropertyValue("rest-api", "rest-api.info.description"))
                .contact(buildOpenApiContact())
                .termsOfService(UtilProperties.getPropertyValue("rest-api", "rest-api.info.termsOfService"))
                .license(new License()
                        .name(UtilProperties.getPropertyValue("rest-api", "rest-api.info.license.name"))
                        .url(UtilProperties.getPropertyValue("rest-api", "rest-api.info.license.url")));

        return info;
    }

    private Contact buildOpenApiContact() {
        Contact contact = new Contact()
                .name(UtilProperties.getPropertyValue("rest-api", "rest-api.info.contact.name"))
                .email(UtilProperties.getPropertyValue("rest-api", "rest-api.info.contact.email"))
                .url(UtilProperties.getPropertyValue("rest-api", "rest-api.info.contact.url"));
        return contact;
    }

    private Server buildOpenApiServer() {
        Server serverItem =
                new Server().url(request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + request.getContextPath())
                        .description(UtilProperties.getPropertyValue("rest-api", "rest-api.server.description"));
        return serverItem;
    }
}
