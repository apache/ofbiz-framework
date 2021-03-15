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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.ws.rs.response.Error;
import org.apache.ofbiz.ws.rs.response.Success;

public final class RestApiUtil {

    private RestApiUtil() {

    }

    public static Response success(String message, Object data) {
        Success success = new Success(Response.Status.OK.getStatusCode(), Response.Status.OK.getReasonPhrase(), message, data);
        return Response.status(Response.Status.OK.getStatusCode()).type(MediaType.APPLICATION_JSON).entity(success).build();
    }

    public static Response error(int statusCode, String reasonPhrase, String message) {
        Error error = new Error(statusCode, reasonPhrase, message);
        return Response.status(statusCode).type(MediaType.APPLICATION_JSON).entity(error).build();
    }

    /**
     * @param message
     * @return
     */
    public static ResponseBuilder errorBuilder(int statusCode, String reasonPhrase, String message) {
        Error error = new Error(statusCode, reasonPhrase, message);
        return Response.status(statusCode).type(MediaType.APPLICATION_JSON).entity(error);
    }

    /**
     * @param multivaluedMap
     * @return
     */
    public static Map<String, Object> extractParams(MultivaluedMap<String, String> multivaluedMap) {
        Map<String, Object> result = new HashMap<>();
        multivaluedMap.forEach((name, values) -> {
            if (UtilValidate.isNotEmpty(values)) {
                result.put(name, (values.size() != 1) ? values : values.get(0));
            }
        });
        return result;
    }

    /**
     * Extracts path parameters from resource pathInfo
     * @param pathInfo
     * @return
     */
    public static List<String> getPathParameters(String pathInfo) {
        List<String> pathParams = new ArrayList<>();
        if (pathInfo == null) {
            return pathParams;
        }
        String[] pathParts = pathInfo.split("/");
        for (String pathSegement : pathParts) {
            if (pathSegement.startsWith("{") && pathSegement.endsWith("}")) {
                pathParams.add(pathSegement.substring(1, pathSegement.length() - 1));
            }
        }
        return pathParams;
    }
}
