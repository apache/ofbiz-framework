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
package org.ofbiz.base.conversion.test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.ofbiz.base.conversion.Converter;
import org.ofbiz.base.conversion.ConverterLoader;
import org.ofbiz.base.conversion.Converters;
import org.ofbiz.base.conversion.JSONConverters;
import org.ofbiz.base.lang.JSON;

public class TestJSONConverters  extends TestCase {
    public TestJSONConverters(String name) {
        super(name);
        ConverterLoader loader = new JSONConverters();
        loader.loadConverters();
    }

    public void testJSONToMap() throws Exception {
        Converter<JSON, Map> converter = Converters.getConverter(JSON.class, Map.class);
        Map map, convertedMap;
        map = new HashMap();
        map.put("field1", "value1");
        JSON json = JSON.from(map);
        convertedMap = converter.convert(json);
        assertEquals("JSON to Map", map, convertedMap);
    }

    public void testJSONToList() throws Exception {
        Converter<JSON, List> converter = Converters.getConverter(JSON.class, List.class);
        List list, convertedList;
        list = new ArrayList();
        list.add("field1");
        list.add("field2");
        JSON json = JSON.from(list);
        convertedList = converter.convert(json);
        assertEquals("JSON to List", list, convertedList);
    }

    public void testMapToJSON() throws Exception {
        Converter<Map, JSON> converter = Converters.getConverter(Map.class, JSON.class);
        JSON json;
        Map map = new LinkedHashMap();
        map.put("field1", "value1");
        map.put("field2", new BigDecimal("3.7"));
        json = converter.convert(map);
        assertEquals("Map to JSON", "{\"field1\":\"value1\",\"field2\":3.7}", json.toString());
    }

    public void testListToJSON() throws Exception {
        Converter<List, JSON> converter = Converters.getConverter(List.class, JSON.class);
        JSON json;
        List list = new ArrayList();
        list.add("field1");
        list.add("field2");
        json = converter.convert(list);
        assertEquals("List to JSON", "[\"field1\",\"field2\"]", json.toString());
    }
}

