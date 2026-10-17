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
package org.apache.ofbiz.base.conversion;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.ofbiz.base.lang.JSON;
import org.apache.ofbiz.base.util.UtilGenerics;

/** JSON Converter classes. */
public class JSONConverters implements ConverterLoader {

    public static class JSONToList extends AbstractConverter<JSON, List<Object>> {
        public JSONToList() {
            super(JSON.class, List.class);
        }

        @Override
        public List<Object> convert(JSON obj) throws ConversionException {
            try {
                return UtilGenerics.<List<Object>>cast(obj.toObject(List.class));
            } catch (IOException e) {
                throw new ConversionException(e);
            }
        }
    }

    public static class JSONToMap extends AbstractConverter<JSON, Map<String, Object>> {
        public JSONToMap() {
            super(JSON.class, Map.class);
        }

        @Override
        public Map<String, Object> convert(JSON obj) throws ConversionException {
            try {
                return UtilGenerics.<Map<String, Object>>cast(obj.toObject(Map.class));
            } catch (IOException e) {
                throw new ConversionException(e);
            }
        }
    }

    public static class ListToJSON extends AbstractConverter<List<Object>, JSON> {
        public ListToJSON() {
            super(List.class, JSON.class);
        }

        @Override
        public JSON convert(List<Object> obj) throws ConversionException {
            try {
                return JSON.from(obj);
            } catch (IOException e) {
                throw new ConversionException(e);
            }
        }
    }

    public static class MapToJSON extends AbstractConverter<Map<String, Object>, JSON> {
        public MapToJSON() {
            super(Map.class, JSON.class);
        }

        @Override
        public JSON convert(Map<String, Object> obj) throws ConversionException {
            try {
                return JSON.from(obj);
            } catch (IOException e) {
                throw new ConversionException(e);
            }
        }
    }

    @Override
    public void loadConverters() {
        Converters.loadContainedConverters(JSONConverters.class);
    }
}
