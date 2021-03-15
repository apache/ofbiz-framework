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

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.apache.ofbiz.ws.rs.util.RestApiUtil;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.server.model.ResourceMethod;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/")
@SuppressWarnings({"unchecked", "rawtypes"})
public class ApiRootResource {

    /**
     *
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response listAllResources(@Context Application application, @Context HttpServletRequest request) {
        String basePath = request.getRequestURL().toString();

        ObjectNode root = JsonNodeFactory.instance.objectNode();
        ArrayNode resources = JsonNodeFactory.instance.arrayNode();
        root.set("resources", resources);
        for (Class<?> aClass : application.getClasses()) {
            if (isAnnotatedResourceClass(aClass)) {
                Resource resource = Resource.from(aClass);
                ObjectNode resourceNode = JsonNodeFactory.instance.objectNode();
                String uriPrefix = resource.getPath();
                for (ResourceMethod srm : resource.getResourceMethods()) {
                    addTo(resourceNode, uriPrefix, srm, joinUri(basePath, uriPrefix));
                }
                resources.add(resourceNode);
            }
        }
        return RestApiUtil.success("OFBiz Resources.", root);
    }

    private void addTo(ObjectNode resourceNode, String uriPrefix, ResourceMethod srm, String path) {
        if (resourceNode.get(uriPrefix) == null) {
            ObjectNode inner = JsonNodeFactory.instance.objectNode();
            inner.put("path", path);
            inner.set("verbs", JsonNodeFactory.instance.arrayNode());
            resourceNode.set(uriPrefix, inner);
        }
        ArrayNode arrayNode = (ArrayNode) resourceNode.get(uriPrefix).get("verbs");
        arrayNode.add(srm.getHttpMethod());
    }

    private boolean isAnnotatedResourceClass(Class rc) {
        if (rc.isAnnotationPresent(Path.class)) {
            return true;
        }
        for (Class i : rc.getInterfaces()) {
            if (i.isAnnotationPresent(Path.class)) {
                return true;
            }
        }
        return false;
    }

    public static String joinUri(String... parts) {
        StringBuilder result = new StringBuilder();
        for (String part : parts) {
            if (result.length() > 0 && result.charAt(result.length() - 1) == '/') {
                result.setLength(result.length() - 1);
            }
            if (result.length() > 0 && !part.startsWith("/")) {
                result.append('/');
            }
            result.append(part);
        }
        return result.toString();
    }
}
