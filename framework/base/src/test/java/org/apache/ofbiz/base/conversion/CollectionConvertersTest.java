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
import static org.junit.Assert.assertNotNull;

import java.util.Map;

import org.apache.ofbiz.base.util.UtilMisc;
import org.junit.Test;

public class CollectionConvertersTest {

    @Test
    public void testToMap() throws ConversionException {
        Converter<String, Map<String, String>> cvt = new CollectionConverters.StringToMap();
        for (String s: new String[] {"", "{", "}", "}{"}) {
            ConversionException caught = null;
            try {
                cvt.convert(s);
            } catch (ConversionException e) {
                caught = e;
            } finally {
                assertNotNull("bad(" + s + ")", caught);
            }
        }
        assertEquals("single", UtilMisc.toMap("1", "one"), cvt.convert("{1=one}"));
        assertEquals("double", UtilMisc.toMap("2", "two", "1", "one"), cvt.convert("{1=one, 2=two}"));
        assertEquals("double-space", UtilMisc.toMap("2", "two ", " 1", "one"), cvt.convert("{ 1=one, 2=two }"));
    }
}
