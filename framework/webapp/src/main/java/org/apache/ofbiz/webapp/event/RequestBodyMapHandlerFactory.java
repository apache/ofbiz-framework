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
package org.apache.ofbiz.webapp.event;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletRequest;

/** Factory class that provides the proper <code>RequestBodyMapHandler</code> based on the content type of the <code>ServletRequest</code> */
public class RequestBodyMapHandlerFactory {
    private static final Map<String, RequestBodyMapHandler> REQ_BODY_MAP_HANDLERS = new HashMap<>();
    static {
        REQ_BODY_MAP_HANDLERS.put("application/json", new JSONRequestBodyMapHandler());
    }

    public static RequestBodyMapHandler getRequestBodyMapHandler(ServletRequest request) {
        String contentType = request.getContentType();
        if (contentType != null && contentType.indexOf(";") != -1) {
            contentType = contentType.substring(0, contentType.indexOf(";"));
        }
        return REQ_BODY_MAP_HANDLERS.get(contentType);
    }

    public static Map<String, Object> extractMapFromRequestBody(ServletRequest request) throws IOException {
        Map<String, Object> outputMap = null;
        RequestBodyMapHandler handler = getRequestBodyMapHandler(request);
        if (handler != null) {
            outputMap = handler.extractMapFromRequestBody(request);
        }
        return outputMap;
    }
}
