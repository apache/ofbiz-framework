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
import java.net.URL;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import javolution.util.FastList;
import javolution.util.FastMap;

import org.ofbiz.base.concurrent.TTLObject;
import org.ofbiz.base.conversion.Converter;
import org.ofbiz.base.conversion.Converters;
import org.ofbiz.base.test.GenericTestCaseBase;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.collections.LRUMap;

public class MiscTests extends GenericTestCaseBase {

    public MiscTests(String name) {
        super(name);
    }

    public void testExtendsImplements() throws Exception {
        List<String> arraysList = Arrays.asList("a", "b", "c");
        Converter converter = Converters.getConverter(arraysList.getClass(), String.class);
        assertEquals("", "[a, b, c]", converter.convert(arraysList));
        Exception caught = null;
        try {
            Converters.getConverter(MiscTests.class, String.class);
        } catch (ClassNotFoundException e) {
            caught = e;
        } finally {
            assertNotNull("ClassNotFoundException thrown for MiscTests.class", caught);
        }
        LRUMap<String, String> map = new LRUMap<String, String>();
        map.put("a", "1");
        converter = Converters.getConverter(LRUMap.class, String.class);
        assertEquals("", "{a=1}", converter.convert(map));
    }

    public void testPassthru() throws Exception {
        String string = "ofbiz";
        BigDecimal bigDecimal = new BigDecimal("1.234");
        URL url = new URL("http://ofbiz.apache.org");
        List<String> baseList = UtilMisc.toList("a", "1", "b", "2", "c", "3");
        ArrayList<String> arrayList = new ArrayList<String>();
        arrayList.addAll(baseList);
        List<String> fastList = FastList.newInstance();
        fastList.addAll(baseList);
        Map<String, String> baseMap = UtilMisc.toMap("a", "1", "b", "2", "c", "3");
        HashMap<String, String> hashMap = new HashMap<String, String>();
        hashMap.putAll(baseMap);
        Map<String, String> fastMap = FastMap.newInstance();
        fastMap.putAll(baseMap);
        Object[] testObjects = new Object[] {
            string,
            bigDecimal,
            url,
            arrayList,
            fastList,
            hashMap,
            fastMap,
        };
        for (Object testObject: testObjects) {
            Converter converter = Converters.getConverter(testObject.getClass(), testObject.getClass());
            Object result = converter.convert(testObject);
            assertEquals("pass thru convert", testObject, result);
            assertTrue("pass thru exact equals", testObject == result);
            assertTrue("pass thru can convert", converter.canConvert(testObject.getClass(), testObject.getClass()));
            assertFalse("pass thru can't convert to object", converter.canConvert(testObject.getClass(), Object.class));
            assertFalse("pass thru can't convert from object", converter.canConvert(Object.class, testObject.getClass()));
            assertEquals("pass thru source class", testObject.getClass(), converter.getSourceClass());
            assertEquals("pass thru target class", result.getClass(), converter.getTargetClass());
        }
    }
}
