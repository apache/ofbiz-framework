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

import javax.servlet.ServletRequest;
import java.io.IOException;
import java.util.Map;

/** An handler that can extract a Map (typically used as a service input map) from the data in the body of a <code>ServletRequest</code>. */
public interface RequestBodyMapHandler {
    /** Extracts from the data in the body of the <code>ServletRequest</code> an instance of <code>Map<String, Object></code>.
     *
     * @param request the request with the data in its body
     * @return an instance of <code>Map<String, Object></code> that represents the data in the request body
     */
    public Map<String, Object> extractMapFromRequestBody(ServletRequest request) throws IOException;
}
