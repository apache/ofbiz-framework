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
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ofbiz.base.conversion.Converter;
import org.apache.ofbiz.base.conversion.ConverterLoader;
import org.apache.ofbiz.base.conversion.Converters;
import org.apache.ofbiz.base.lang.SourceMonitored;
import org.apache.ofbiz.base.test.GenericTestCaseBase;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilMisc;

@SourceMonitored
public class MiscTests extends GenericTestCaseBase {

    public MiscTests(String name) {
        super(name);
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
        List<String> arrayList = new ArrayList<String>();
        arrayList.addAll(baseList);
        Map<String, String> baseMap = UtilMisc.toMap("a", "1", "b", "2", "c", "3");
        Map<String, String> hashMap = new HashMap<String, String>();
        hashMap.putAll(baseMap);
        Object[] testObjects = new Object[] {
            string,
            bigDecimal,
            url,
            arrayList,
            hashMap
        };
        for (Object testObject: testObjects) {
            assertPassThru(testObject, testObject.getClass());
        }
        assertPassThru(arrayList, arrayList.getClass(), List.class);
        assertPassThru(hashMap, hashMap.getClass(), Map.class);
    }
}
