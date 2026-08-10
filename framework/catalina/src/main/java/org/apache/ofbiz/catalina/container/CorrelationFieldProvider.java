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
package org.apache.ofbiz.catalina.container;

import java.util.Map;

import org.apache.catalina.connector.Request;
import org.apache.ofbiz.entity.GenericValue;

/**
 * Extension point for {@link CorrelationValve}: lets a component contribute additional
 * key/value pairs to the per-request logging correlation context (Log4j2 {@code ThreadContext}),
 * beyond the core {@code requestId}/{@code visitId}/{@code userLoginId} fields the Valve always
 * sets.
 *
 * <p>Implementations are discovered via {@link java.util.ServiceLoader} - a component adds one by
 * providing an implementation class plus a
 * {@code META-INF/services/org.apache.ofbiz.catalina.container.CorrelationFieldProvider} file
 * naming it, with no framework code changes required. All discovered providers are called for
 * every request and their fields merged together.
 */
public interface CorrelationFieldProvider {

    /**
     * Returns additional correlation fields for the current request, or {@code null}/empty if
     * this provider has nothing to add (e.g. an anonymous request for a provider that only
     * contributes fields for logged-in users).
     * @param request the current request
     * @param userLogin the logged-in user, or {@code null} if the request is anonymous
     */
    Map<String, String> getFields(Request request, GenericValue userLogin);
}
