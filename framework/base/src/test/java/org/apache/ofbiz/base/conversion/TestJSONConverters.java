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
package org.apache.ofbiz.base.conversion;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.ofbiz.base.lang.JSON;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.junit.Test;

public class TestJSONConverters {
    public TestJSONConverters() {
        ConverterLoader loader = new JSONConverters();
        loader.loadConverters();
    }

    @Test
    public void testJSONToMap() throws Exception {
        Converter<JSON, Map<String, String>> converter =
                UtilGenerics.cast(Converters.getConverter(JSON.class, Map.class));
        Map<String, String> map;
        Map<String, String> convertedMap;
        map = new HashMap<>();
        map.put("field1", "value1");
        JSON json = JSON.from(map);
        Object obj = converter.convert(json);
        convertedMap = (obj instanceof Map) ? UtilGenerics.cast(obj) : null;
        assertEquals("JSON to Map", map, convertedMap);
    }

    @Test
    public void testJSONToList() throws Exception {
        Converter<JSON, List<Object>> converter = UtilGenerics.cast(Converters.getConverter(JSON.class, List.class));
        List<Object> list = new ArrayList<>();
        list.add("field1");
        list.add("field2");
        JSON json = JSON.from(list);
        Object obj = converter.convert(json);
        List<Object> convertedList = (obj instanceof List) ? UtilGenerics.cast(obj) : null;
        assertEquals("JSON to List", list, convertedList);
    }

    @Test
    public void testMapToJSON() throws Exception {
        Converter<Map<String, Object>, JSON> converter =
                UtilGenerics.cast(Converters.getConverter(Map.class, JSON.class));
        JSON json;
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("field1", "value1");
        map.put("field2", new BigDecimal("3.7"));
        json = converter.convert(map);
        assertEquals("Map to JSON", "{\"field1\":\"value1\",\"field2\":3.7}", json.toString());
    }

    @Test
    public void testListToJSON() throws Exception {
        Converter<List<String>, JSON> converter = UtilGenerics.cast(Converters.getConverter(List.class, JSON.class));
        JSON json;
        List<String> list = new ArrayList<>();
        list.add("field1");
        list.add("field2");
        json = converter.convert(list);
        assertEquals("List to JSON", "[\"field1\",\"field2\"]", json.toString());
    }
}

