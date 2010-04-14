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
package org.ofbiz.base.util.test;

import java.util.Map;
import org.ofbiz.base.util.Assert;
import junit.framework.TestCase;

/**
 * Assert tests {@link org.ofbiz.base.util.Assert}.
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
            Assert.argumentNotNull("foo", testObject);
        } catch (Exception e) {
            fail("argumentNotNull threw an exception - " + e);
        }
        try {
            Assert.argumentNotNull("foo", null);
            fail("argumentNotNull - IllegalArgumentException not thrown");
        } catch (IllegalArgumentException e) {}

        //-----------------------------------------------------------------------
        try {
            Assert.argumentsNotNull("foo", testObject, "bar", testObject);
        } catch (Exception e) {
            fail("argumentsNotNull threw an exception - " + e);
        }
        try {
            Assert.argumentsNotNull("foo", testObject, "bar", null);
            fail("argumentsNotNull - IllegalArgumentException not thrown");
        } catch (IllegalArgumentException e) {}

        //-----------------------------------------------------------------------
        try {
            Assert.argumentIsClass("foo", testObject, Object.class);
        } catch (Exception e) {
            fail("argumentIsClass threw an exception - " + e);
        }
        try {
            Assert.argumentIsClass("foo", testObject, AssertTests.class);
            fail("argumentIsClass - IllegalArgumentException not thrown");
        } catch (IllegalArgumentException e) {}

        //-----------------------------------------------------------------------
        try {
            Assert.argumentEqualsObject("foo", testObject, testObject);
        } catch (Exception e) {
            fail("argumentEqualsObject threw an exception - " + e);
        }
        try {
            Assert.argumentEqualsObject("foo", testObject, this);
            fail("argumentEqualsObject - IllegalArgumentException not thrown");
        } catch (IllegalArgumentException e) {}

        //-----------------------------------------------------------------------
        try {
            Assert.argumentEqualsObject("foo", testObject, testObject);
        } catch (Exception e) {
            fail("argumentEqualsObject threw an exception - " + e);
        }
        try {
            Assert.argumentEqualsObject("foo", testObject, this);
            fail("argumentEqualsObject - IllegalArgumentException not thrown");
        } catch (IllegalArgumentException e) {}

        //-----------------------------------------------------------------------
        try {
            Assert.argumentCanBeCastTo("foo", testObject, Object.class);
        } catch (Exception e) {
            fail("argumentCanBeCastTo threw an exception - " + e);
        }
        try {
            Assert.argumentCanBeCastTo("foo", this, Map.class);
            fail("argumentCanBeCastTo - IllegalArgumentException not thrown");
        } catch (IllegalArgumentException e) {}
    }
}
