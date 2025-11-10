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
package org.apache.ofbiz.ws.rs.core;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.core.MediaType;

import org.apache.ofbiz.base.component.ComponentConfig;
import org.apache.ofbiz.base.component.ComponentException;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.ws.rs.ServiceRequestFilter;
import org.apache.ofbiz.ws.rs.model.ModelApi;
import org.apache.ofbiz.ws.rs.model.ModelApiReader;
import org.apache.ofbiz.ws.rs.model.ModelOperation;
import org.apache.ofbiz.ws.rs.model.ModelResource;
import org.apache.ofbiz.ws.rs.process.ServiceRequestHandler;
import org.apache.ofbiz.ws.rs.annotation.Secured;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.server.model.ResourceMethod;
import org.apache.ofbiz.ws.rs.filters.ServiceContextCleanupFilter;

public class OFBizApiConfig extends ResourceConfig {
    private static final String MODULE = OFBizApiConfig.class.getName();
    private static final Map<String, ModelApi> MICRO_APIS = new HashMap<>();

    public OFBizApiConfig() {
        packages("org.apache.ofbiz.ws.rs.resources");
        packages("org.apache.ofbiz.ws.rs.security.auth");
        packages("org.apache.ofbiz.ws.rs.spi.impl");
        // packages("io.swagger.v3.jaxrs2.integration.resources"); //commenting it out
        // to generate customized OpenApi Spec
        register(JacksonFeature.class);
        register(ServiceRequestFilter.class);
        register(MultiPartFeature.class);
        register(ServiceContextCleanupFilter.class);
        //property(ServerProperties.TRACING, "ALL");
        if (Debug.verboseOn()) {
            register(new LoggingFeature(Logger.getLogger(LoggingFeature.DEFAULT_LOGGER_NAME), Level.INFO,
                    LoggingFeature.Verbosity.PAYLOAD_ANY, 10000));
        }
        registerDSLResources();
    }

    public static Map<String, ModelApi> getModelApis() {
        return MICRO_APIS;
    }

    private void registerDSLResources() {
        loadApiDefinitions();
        traverseAndRegisterApiDefinitions();
    }

    private void loadApiDefinitions() {
        Collection<ComponentConfig> components = ComponentConfig.getAllComponents();
        components.forEach(component -> {
            String cName = component.getComponentName();
            try {
                String apiDirPath = ComponentConfig.getRootLocation(cName) + "/api";
                File apiDir = new File(apiDirPath);
                if (apiDir.exists() && apiDir.isDirectory()) {
                    File[] restXmlFiles = apiDir.listFiles((dir, name) -> name.endsWith(".rest.xml"));
                    for (File apiSchemaF : restXmlFiles) {
                        ModelApi api = ModelApiReader.getModelApi(apiSchemaF);
                        if (!api.isPublish()) {
                            Debug.logInfo("API {}[{}] is declared to be a non-publish, ignoring...", api.getName(), api.getPath(), MODULE);
                            continue;
                        }
                        String path = api.getPath();
                        if (MICRO_APIS.containsKey(path)) {
                            Debug.logWarning("Duplicate REST API definition detected for path: " + path
                                    + " at location " + apiSchemaF
                                    + ". Overriding existing entry from component: " + cName, MODULE);
                        } else {
                            Debug.logInfo("Processing REST API path: " + path + " from component " + cName, MODULE);
                        }
                        MICRO_APIS.put(path, api);
                    }
                }
            } catch (ComponentException e) {
                Debug.logError(e, MODULE);
            }
        });
    }

    private void traverseAndRegisterApiDefinitions() {
        if (UtilValidate.isEmpty(MICRO_APIS)) {
            Debug.logInfo("No API definitions to process", MODULE);
            return;
        }

        MICRO_APIS.forEach((apiPath, modelApi) -> {
            Debug.logInfo("Registering Resource Definitions from API - " + apiPath, MODULE);
            for (ModelResource resource : modelApi.getResources()) {
                String resourcePath = buildCleanPath(apiPath, resource.getPath());
                registerModelResource(resource, resourcePath);
            }
        });
    }
    private void registerModelResource(ModelResource modelResource, String basePath) {
        if (!modelResource.isPublish()) return;

        Resource.Builder resourceBuilder = Resource.builder("/" + basePath)
                .name(modelResource.getName());

        for (ModelOperation op : modelResource.getOperations()) {
            String verb = op.getVerb().toUpperCase();
            boolean isOtherThanGet = verb.matches(HttpMethod.POST + "|" + HttpMethod.PUT + "|" + HttpMethod.PATCH);
            String opPath = op.getPath();

            ResourceMethod.Builder methodBuilder;
            if (UtilValidate.isEmpty(opPath)) {
                methodBuilder = resourceBuilder.addMethod(verb);
            } else {
                Resource.Builder childBuilder = resourceBuilder.addChildResource(opPath);
                methodBuilder = childBuilder.addMethod(verb);
            }

            methodBuilder.produces(MediaType.APPLICATION_JSON);
            if (isOtherThanGet) {
                methodBuilder.consumes(MediaType.APPLICATION_JSON);
            }
            if (op.isAuth()) {
                methodBuilder.nameBindings(Secured.class);
            }
            methodBuilder.handledBy(new ServiceRequestHandler(op.getService()));
        }

        // Register the current resource
        registerResources(resourceBuilder.build());

        // Recursively process nested sub-resources
        if (UtilValidate.isNotEmpty(modelResource.getSubResources())) {
            for (ModelResource sub : modelResource.getSubResources()) {
                String subPath = buildCleanPath(basePath, sub.getPath());
                registerModelResource(sub, subPath);
            }
        }
    }
    private String buildCleanPath(String... parts) {
        StringBuilder pathBuilder = new StringBuilder();
        for (String part : parts) {
            if (part == null || part.trim().isEmpty()) continue;
            part = part.replaceAll("^/+", "").replaceAll("/+$", ""); // trim slashes
            if (!part.isEmpty()) {
                if (pathBuilder.length() > 0) pathBuilder.append('/');
                pathBuilder.append(part);
            }
        }
        return pathBuilder.toString();
    }
}
