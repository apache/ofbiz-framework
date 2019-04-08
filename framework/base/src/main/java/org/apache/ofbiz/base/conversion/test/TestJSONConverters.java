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
package org.apache.ofbiz.base.conversion.test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.apache.ofbiz.base.conversion.Converter;
import org.apache.ofbiz.base.conversion.ConverterLoader;
import org.apache.ofbiz.base.conversion.Converters;
import org.apache.ofbiz.base.conversion.JSONConverters;
import org.apache.ofbiz.base.lang.JSON;
import org.apache.ofbiz.base.util.UtilGenerics;

public class TestJSONConverters  extends TestCase {
    public TestJSONConverters(String name) {
        super(name);
        ConverterLoader loader = new JSONConverters();
        loader.loadConverters();
    }

    public void testJSONToMap() throws Exception {
        Converter<JSON, Map<String,String>> converter = UtilGenerics.cast(Converters.getConverter(JSON.class, Map.class));
        Map<String,String> map, convertedMap;
        map = new HashMap<String,String>();
        map.put("field1", "value1");
        JSON json = JSON.from(map);
        convertedMap = UtilGenerics.toMap(converter.convert(json));
        assertEquals("JSON to Map", map, convertedMap);
    }

    public void testJSONToList() throws Exception {
        Converter<JSON, List<Object>> converter = UtilGenerics.cast(Converters.getConverter(JSON.class, List.class));
        List<Object> list, convertedList;
        list = new ArrayList<Object>();
        list.add("field1");
        list.add("field2");
        JSON json = JSON.from(list);
        convertedList = UtilGenerics.toList(converter.convert(json));
        assertEquals("JSON to List", list, convertedList);
    }

    public void testMapToJSON() throws Exception {
        Converter<Map<String,Object>, JSON> converter = UtilGenerics.cast(Converters.getConverter(Map.class, JSON.class));
        JSON json;
        Map<String,Object> map = new LinkedHashMap<String,Object>();
        map.put("field1", "value1");
        map.put("field2", new BigDecimal("3.7"));
        json = converter.convert(map);
        assertEquals("Map to JSON", "{\"field1\":\"value1\",\"field2\":3.7}", json.toString());
    }

    public void testListToJSON() throws Exception {
        Converter<List<String>, JSON> converter = UtilGenerics.cast(Converters.getConverter(List.class, JSON.class));
        JSON json;
        List<String> list = new ArrayList<String>();
        list.add("field1");
        list.add("field2");
        json = converter.convert(list);
        assertEquals("List to JSON", "[\"field1\",\"field2\"]", json.toString());
    }
}

