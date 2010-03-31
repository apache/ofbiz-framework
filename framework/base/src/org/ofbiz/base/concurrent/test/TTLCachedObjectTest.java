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
package org.ofbiz.base.concurrent.test;

import org.ofbiz.base.concurrent.GeneratedResult;
import org.ofbiz.base.concurrent.TTLCachedObject;
import org.ofbiz.base.concurrent.TTLObject;
import org.ofbiz.base.lang.SourceMonitored;

@SourceMonitored
public class TTLCachedObjectTest extends TTLObjectTest {
    static {
        TTLObject.setDefaultTTLForClass(TTLCachedObjectTestTTLObject.class, 100);
    }

    public TTLCachedObjectTest(String name) {
        super(name, true);
    }

    protected final class TTLCachedObjectTestTTLObject extends TTLCachedObject<String> {
        protected long dataTimestamp = NOT_EXISTANT_TIMESTAMP;
        protected String data = "first";

        protected long getTimestamp(String old) throws Exception {
            return dataTimestamp;
        }

        protected GeneratedResult<String> generate(String old, int serial) throws Exception {
            return new GeneratedResult<String>(dataTimestamp, data);
        }
    }

    public void testTTLCachedObject() throws Exception {
        TTLCachedObjectTestTTLObject object = new TTLCachedObjectTestTTLObject();
        assertNull("initial non-existant value", object.getObject());
        assertEquals("initial non-existant timestamp", TTLCachedObject.NOT_EXISTANT_TIMESTAMP, object.getTimestamp());
        object.dataTimestamp = 1;
        assertNull("initial no-refresh value", object.getObject());
        assertEquals("initial no-refresh timestamp", TTLCachedObject.NOT_EXISTANT_TIMESTAMP, object.getTimestamp());
        object.refresh();
        assertEquals("first value", "first", object.getObject());
        assertEquals("first timestamp", 1, object.getTimestamp());
        object.data = "second";
        object.refresh();
        assertEquals("not-modified value", "first", object.getObject());
        assertEquals("not-modified timestamp", 1, object.getTimestamp());
        object.dataTimestamp = 2;
        assertEquals("cached modified value", "first", object.getObject());
        assertEquals("cached modified timestamp", 1, object.getTimestamp());
        object.refresh();
        assertEquals("refresh second value", "second", object.getObject());
        assertEquals("refresh second timestamp", 2, object.getTimestamp());
        object.dataTimestamp = TTLCachedObject.NOT_EXISTANT_TIMESTAMP;
        object.refresh();
        assertNull("refresh non-existant value", object.getObject());
        assertEquals("refresh non-existant timestamp", TTLCachedObject.NOT_EXISTANT_TIMESTAMP, object.getTimestamp());
    }
}
