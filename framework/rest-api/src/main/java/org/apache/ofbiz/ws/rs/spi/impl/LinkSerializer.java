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

import java.io.IOException;
import java.util.Map.Entry;

import javax.ws.rs.core.Link;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public class LinkSerializer extends JsonSerializer<javax.ws.rs.core.Link> {
    static final String HREF_PROPERTY = "href";


    /**
     * Serialize.
     * @param link               the link
     * @param jsonGenerator      the json generator
     * @param serializerProvider the serializer provider
     * @throws IOException the io exception
     */
    public void serialize(Link link, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField(HREF_PROPERTY, link.getUri().toString());
        for (Entry<String, String> entry : link.getParams().entrySet()) {
            jsonGenerator.writeStringField(entry.getKey(), entry.getValue());
        }
        jsonGenerator.writeEndObject();
    }
}
