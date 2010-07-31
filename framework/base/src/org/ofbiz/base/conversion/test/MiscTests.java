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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javolution.util.FastList;
import javolution.util.FastMap;

import org.ofbiz.base.conversion.Converter;
import org.ofbiz.base.conversion.ConverterLoader;
import org.ofbiz.base.conversion.Converters;
import org.ofbiz.base.conversion.JSONResult;
import org.ofbiz.base.lang.SourceMonitored;
import org.ofbiz.base.test.GenericTestCaseBase;
import org.ofbiz.base.util.UtilGenerics;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.collections.LRUMap;

@SourceMonitored
public class MiscTests extends GenericTestCaseBase {

    public MiscTests(String name) {
        super(name);
    }

    private static <S, T extends JSONResult.Indenting> void assertConversion(String label, String wanted, Class<T> targetClass, Object source, Class<S> sourceClass) throws Exception {
        Converter<S, T> converter = Converters.getConverter(sourceClass, targetClass);
        assertTrue(label + " can convert", converter.canConvert(sourceClass, targetClass));
        assertEquals(label, wanted, converter.convert(UtilGenerics.<S>cast(source)).getResult());
    }

    public void testStaticHelperClass() throws Exception {
        assertStaticHelperClass(Converters.class);
    }

    public static class ConverterLoaderImpl implements ConverterLoader {
        public void loadConverters() {
            throw new RuntimeException();
        }
    }

    public void testLoadContainedConvertersIgnoresException() {
        Converters.loadContainedConverters(MiscTests.class);
    }

    public void testExtendsImplements() throws Exception {
        List<String> arraysList = Arrays.asList("a", "b", "c");
        assertConversion("", "[\n \"a\",\n \"b\",\n \"c\"\n]", JSONResult.Indenting.class, arraysList, arraysList.getClass());
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
        assertConversion("", "{\n \"a\": \"1\"\n}", JSONResult.Indenting.class, map, LRUMap.class);
    }

    public static <S> void assertPassThru(Object wanted, Class<S> sourceClass) throws Exception {
        assertPassThru(wanted, sourceClass, sourceClass);
    }

    public static <S> void assertPassThru(Object wanted, Class<S> sourceClass, Class<? super S> targetClass) throws Exception {
        Converter<S, ? super S> converter = Converters.getConverter(sourceClass, targetClass);
        Object result = converter.convert(UtilGenerics.<S>cast(wanted));
        assertEquals("pass thru convert", wanted, result);
        assertSame("pass thru exact equals", wanted, result);
        assertTrue("pass thru can convert wanted", converter.canConvert(wanted.getClass(), targetClass));
        assertTrue("pass thru can convert source", converter.canConvert(sourceClass, targetClass));
        assertEquals("pass thru source class", wanted.getClass(), converter.getSourceClass());
        assertEquals("pass thru target class", targetClass, converter.getTargetClass());
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
            assertPassThru(testObject, testObject.getClass());
        }
        assertPassThru(fastList, fastList.getClass(), List.class);
        assertPassThru(fastMap, fastMap.getClass(), Map.class);
        assertPassThru(hashMap, hashMap.getClass(), Map.class);
    }
}
