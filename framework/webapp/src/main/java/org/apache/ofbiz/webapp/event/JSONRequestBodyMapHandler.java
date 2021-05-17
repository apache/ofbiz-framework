/*
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
 */
package org.apache.ofbiz.webapp.event;

import java.io.IOException;
import java.util.Map;

import javax.servlet.ServletRequest;

import org.apache.ofbiz.base.lang.JSON;
import org.apache.ofbiz.base.util.UtilGenerics;

/** An implementation of <code>RequestBodyMapHandler</code> that can extract a <code>Map&lt;String, Object&gt;</code>
 * from the JSON data in the request body */
public class JSONRequestBodyMapHandler implements RequestBodyMapHandler {

    @Override
    public Map<String, Object> extractMapFromRequestBody(ServletRequest request) throws IOException {
        return UtilGenerics.<Map<String, Object>>cast(JSON.from(request.getInputStream()).toObject(Map.class));
    }
}
