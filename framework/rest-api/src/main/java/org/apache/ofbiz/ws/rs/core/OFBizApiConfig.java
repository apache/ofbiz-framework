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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.ofbiz.base.component.ComponentConfig;
import org.apache.ofbiz.base.component.ComponentException;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.ws.rs.annotation.Secured;
import org.apache.ofbiz.ws.rs.filters.ServiceContextCleanupFilter;
import org.apache.ofbiz.ws.rs.model.ModelApi;
import org.apache.ofbiz.ws.rs.model.ModelApiReader;
import org.apache.ofbiz.ws.rs.model.ModelOperation;
import org.apache.ofbiz.ws.rs.model.ModelResource;
import org.apache.ofbiz.ws.rs.process.ServiceRequestHandler;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.message.DeflateEncoder;
import org.glassfish.jersey.message.GZipEncoder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.filter.EncodingFilter;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.server.model.ResourceMethod;

import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.core.MediaType;

public class OFBizApiConfig extends ResourceConfig {
    private static final String MODULE = OFBizApiConfig.class.getName();
    private static final Map<String, List<ModelApi>> MICRO_APIS = new HashMap<>();

    /**
     * Configures the OFBiz JAX-RS application by registering built-in resource
     * packages, features, filters, and all DSL-defined REST API resources loaded
     * from {@code *.rest.xml} files found in each OFBiz component's {@code api}
     * directory.
     */
    public OFBizApiConfig() {
        packages("org.apache.ofbiz.ws.rs.resources");
        packages("org.apache.ofbiz.ws.rs.security.auth");
        packages("org.apache.ofbiz.ws.rs.spi.impl");
        // packages("io.swagger.v3.jaxrs2.integration.resources"); //commenting it out
        // to generate customized OpenApi Spec
        register(JacksonFeature.class);
        register(MultiPartFeature.class);
        register(ServiceContextCleanupFilter.class);
        //property(ServerProperties.TRACING, "ALL");
        if (Debug.verboseOn()) {
            register(new LoggingFeature(Logger.getLogger(LoggingFeature.DEFAULT_LOGGER_NAME), Level.INFO,
                    LoggingFeature.Verbosity.PAYLOAD_ANY, 10000));
        }
        registerDSLResources();
        EncodingFilter.enableFor(this, GZipEncoder.class);
        EncodingFilter.enableFor(this, DeflateEncoder.class);
    }

    /**
     * Returns all loaded REST API definitions keyed by their root path.
     *
     * <p>Each entry maps the API path (e.g. {@code "party"}) to the
     * corresponding {@link ModelApi} loaded from a {@code *.rest.xml}
     * definition file.</p>
     *
     * @return registered API definitions
     */
    public static Map<String, List<ModelApi>> getModelApis() {
        return MICRO_APIS;
    }

    private void registerDSLResources() {
        loadApiDefinitions();
        traverseAndRegisterApiDefinitions();
    }

    private void loadApiDefinitions() {
        Collection<ComponentConfig> components = ComponentConfig.getAllComponents();
        for (ComponentConfig component : components) {
            String componentName = component.getComponentName();
            try {
                String apiDirPath = ComponentConfig.getRootLocation(componentName) + "/api";
                File apiDir = new File(apiDirPath);
                if (!apiDir.exists() || !apiDir.isDirectory()) {
                    continue;
                }
                File[] restXmlFiles = apiDir.listFiles((dir, name) -> name.endsWith(".rest.xml"));
                if (restXmlFiles == null) {
                    continue;
                }
                for (File restXmlFile : restXmlFiles) {
                    ModelApi api = ModelApiReader.getModelApi(restXmlFile);
                    if (!api.isPublish()) {
                        Debug.logInfo("API %s[%s] is declared non-publish, ignoring...", api.getName(), api.getApiGroupPath(), MODULE);
                        continue;
                    }
                    String path = api.getApiGroupPath();
                    Debug.logInfo("Adding API %s to group: %s from component: %s", api.getName(), path, componentName, MODULE);
                    MICRO_APIS.computeIfAbsent(path, k -> new ArrayList<>()).add(api);
                }
            } catch (ComponentException | RuntimeException e) {
                Debug.logError(e, MODULE);
            }
        }
    }

    private void traverseAndRegisterApiDefinitions() {
        if (UtilValidate.isEmpty(MICRO_APIS)) {
            Debug.logInfo("No API definitions to process", MODULE);
            return;
        }

        MICRO_APIS.forEach((groupPath, apis) -> {
            Set<String> registeredSignatures = new HashSet<>();
            for (ModelApi modelApi : apis) {
                Debug.logInfo("Registering Resource Definitions from API - " + modelApi.getName()
                        + " in group: " + groupPath, MODULE);
                for (ModelResource resource : modelApi.getResources()) {
                    String resourcePath = buildCleanPath(groupPath, resource.getPath());
                    registerModelResource(resource, resourcePath, registeredSignatures, modelApi.getName());
                }
            }
        });
    }
    private void registerModelResource(ModelResource modelResource, String basePath,
            Set<String> registeredSignatures, String owningApiName) {
        if (!modelResource.isPublish()) return;

        Resource.Builder resourceBuilder = Resource.builder("/" + basePath)
                .name(modelResource.getName());

        for (ModelOperation op : modelResource.getOperations()) {
            String serviceName = op.getService();
            String verb = op.getVerb().toUpperCase();
            String opPath = op.getPath();
            String fullPath = UtilValidate.isEmpty(opPath) ? basePath : buildCleanPath(basePath, opPath);
            String signature = verb + " " + fullPath;

            // Fail fast if api groups define identical endpoints
            if (!registeredSignatures.add(signature)) {
                throw new IllegalStateException("REST path collision in group '" + basePath
                        + "': " + signature + " is already registered by another API"
                        + " (conflict while processing " + owningApiName + ")");
            }

            ServiceRequestHandler requestHandler = new ServiceRequestHandler(serviceName, op.getPrimaryPermission(), op.getMainAction());
            boolean isOtherThanGet = verb.matches(HttpMethod.POST + "|" + HttpMethod.PUT + "|" + HttpMethod.PATCH);

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
            methodBuilder.handledBy(requestHandler);
        }

        // Register the current resource
        registerResources(resourceBuilder.build());

        // Recursively process nested sub-resources
        if (UtilValidate.isNotEmpty(modelResource.getSubResources())) {
            for (ModelResource sub : modelResource.getSubResources()) {
                String subPath = buildCleanPath(basePath, sub.getPath());
                registerModelResource(sub, subPath, registeredSignatures, owningApiName);
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
