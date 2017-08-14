/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ofbiz.base.util.test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ofbiz.base.util.Assert;

import junit.framework.TestCase;

/**
 * Assert tests {@link org.apache.ofbiz.base.util.Assert}.
 *
 */
public class AssertTests extends TestCase {

    public AssertTests(String name) {
        super(name);
    }

    public void testAssert(){
        Object testObject = new Object();

        //-----------------------------------------------------------------------
        try {
            Assert.notNull("foo", testObject);
        } catch (Exception e) {
            fail("notNull threw an exception - " + e);
        }
        try {
            Assert.notNull("foo", null);
            fail("notNull - IllegalArgumentException not thrown");
        } catch (IllegalArgumentException e) {}

        //-----------------------------------------------------------------------
        try {
            Assert.notNull("foo", testObject, "bar", testObject);
        } catch (Exception e) {
            fail("notNull (argument list) threw an exception - " + e);
        }
        try {
            Assert.notNull("foo", testObject, "bar", null);
            fail("notNull (argument list) - IllegalArgumentException not thrown");
        } catch (IllegalArgumentException e) {}

        //-----------------------------------------------------------------------
        try {
            Assert.notEmpty("foo", "foo");
        } catch (Exception e) {
            fail("notEmpty(String) threw an exception - " + e);
        }
        try {
            Assert.notEmpty("foo", "");
            fail("notEmpty(String) - IllegalArgumentException not thrown");
        } catch (IllegalArgumentException e) {}

        //-----------------------------------------------------------------------
        String[] strArray = {"foo", "bar"};
        try {
            Assert.notEmpty("foo", strArray);
        } catch (Exception e) {
            fail("notEmpty(Array) threw an exception - " + e);
        }
        try {
            Assert.notEmpty("foo", new String[0]);
            fail("notEmpty(Array) - IllegalArgumentException not thrown");
        } catch (IllegalArgumentException e) {}

        //-----------------------------------------------------------------------
        List<String> strList = new ArrayList<String>();
        try {
            Assert.notEmpty("foo", strList);
            fail("notEmpty(Collection) - IllegalArgumentException not thrown");
        } catch (IllegalArgumentException e) {}
        strList.add("foo");
        try {
            Assert.notEmpty("foo", strList);
        } catch (Exception e) {
            fail("notEmpty(Collection) threw an exception - " + e);
        }

        //-----------------------------------------------------------------------
        Map<String,String> strMap = new HashMap<String, String>();
        try {
            Assert.notEmpty("foo", strMap);
            fail("notEmpty(Map) - IllegalArgumentException not thrown");
        } catch (IllegalArgumentException e) {}
        strMap.put("foo", "foo");
        try {
            Assert.notEmpty("foo", strMap);
        } catch (Exception e) {
            fail("notEmpty(Map) threw an exception - " + e);
        }

        //-----------------------------------------------------------------------
        try {
            Assert.isInstanceOf("foo", strMap, Map.class);
        } catch (Exception e) {
            fail("isInstanceOf threw an exception - " + e);
        }
        try {
            Assert.isInstanceOf("foo", strMap, AssertTests.class);
            fail("isInstanceOf - IllegalArgumentException not thrown");
        } catch (IllegalArgumentException e) {}

        //-----------------------------------------------------------------------
        try {
            Assert.isInstanceOf("foo", strMap, String.class, Map.class);
        } catch (Exception e) {
            fail("isInstanceOf (argument list) threw an exception - " + e);
        }
        try {
            Assert.isInstanceOf("foo", strMap, String.class, AssertTests.class);
            fail("isInstanceOf (argument list) - IllegalArgumentException not thrown");
        } catch (IllegalArgumentException e) {}

        //-----------------------------------------------------------------------
        try {
            Assert.isNotInstanceOf("foo", strMap, String.class);
        } catch (Exception e) {
            fail("isNotInstanceOf threw an exception - " + e);
        }
        try {
            Assert.isNotInstanceOf("foo", strMap, Map.class);
            fail("isNotInstanceOf - IllegalArgumentException not thrown");
        } catch (IllegalArgumentException e) {}

        //-----------------------------------------------------------------------
        try {
            Assert.isNotInstanceOf("foo", strMap, String.class, AssertTests.class);
        } catch (Exception e) {
            fail("isNotInstanceOf (argument list) threw an exception - " + e);
        }
        try {
            Assert.isNotInstanceOf("foo", strMap, String.class, Map.class);
            fail("isNotInstanceOf (argument list) - IllegalArgumentException not thrown");
        } catch (IllegalArgumentException e) {}

        //-----------------------------------------------------------------------
        try {
            Assert.isAssignableTo("foo", strArray, strArray.getClass());
        } catch (Exception e) {
            fail("isNotInstanceOf (argument list) threw an exception - " + e);
        }
        try {
            Map[] mapArray = {strMap};
            Assert.isAssignableTo("foo", strArray, mapArray.getClass());
            fail("isNotInstanceOf (argument list) - IllegalArgumentException not thrown");
        } catch (IllegalArgumentException e) {}
    }
}
