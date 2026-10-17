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
package org.apache.ofbiz.ws.rs.spi.impl;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilProperties;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import jakarta.ws.rs.core.Link;
import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.Provider;

@Provider
public class JacksonConfig implements ContextResolver<ObjectMapper> {

    private static final String MODULE = JacksonConfig.class.getName();
    private static final String INCLUDE_VALUE = UtilProperties.getPropertyValue(
            "rest-api", "rest-api.jackson.serialization.inclusion.value", "NON_NULL");
    private static final String INCLUDE_CONTENT = UtilProperties.getPropertyValue(
            "rest-api", "rest-api.jackson.serialization.inclusion.content", "NON_NULL");
    private final ObjectMapper objectMapper;

    /**
     * Configures the Jackson {@link ObjectMapper} used for JSON serialization
     * and deserialization in REST responses.
     *
     * <p>This configuration applies consistent serialization rules across OFBiz REST APIs,
     * including:
     * <ul>
     *     <li>ISO-style date handling (no timestamp serialization)</li>
     *     <li>Ignoring unknown JSON properties during deserialization</li>
     *     <li>Excluding null values from output</li>
     *     <li>Full field visibility for serialization/deserialization</li>
     *     <li>Pretty-printed JSON output</li>
     * </ul>
     *
     * <p>Additionally, custom serializers are registered for REST link representations</p>
     */
    public JacksonConfig() {
        JsonInclude.Include jsonIncludeValue = resolveInclude(INCLUDE_VALUE);
        JsonInclude.Include jsonIncludeContent = resolveInclude(INCLUDE_CONTENT);

        objectMapper = JsonMapper.builder()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .defaultPropertyInclusion(JsonInclude.Value.construct(jsonIncludeValue, jsonIncludeContent))
                .visibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY)
                .enable(SerializationFeature.INDENT_OUTPUT)
                .build();
        SimpleModule simpleModule = new SimpleModule();
        simpleModule.addSerializer(Link.class, new LinkSerializer());
        objectMapper.registerModule(simpleModule);
    }

    @Override
    public ObjectMapper getContext(Class<?> type) {
        return objectMapper;
    }

    /**
     * Small Helper to map from Include-Strings in .properties to valid JsonInclude
     * @param String value equal to JsonInclude.Include enum
     * @return JsonInclude.Include
     */
    private static JsonInclude.Include resolveInclude(String value) {
        try {
            return JsonInclude.Include.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            Debug.logWarning("Invalid jackson inclusion value [%s], falling back to NON_NULL", MODULE, value);
            return JsonInclude.Include.NON_NULL;
        }
    }
}
