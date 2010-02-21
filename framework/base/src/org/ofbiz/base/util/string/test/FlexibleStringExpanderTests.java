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
package org.ofbiz.base.util.string.test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.ofbiz.base.util.string.FlexibleStringExpander;

public class FlexibleStringExpanderTests extends TestCase {

    public FlexibleStringExpanderTests(String name) {
        super(name);
    }

    public void testFlexibleStringExpander() {
        FlexibleStringExpander fse = FlexibleStringExpander.getInstance(null);
        assertTrue("null FlexibleStringExpander", fse.isEmpty());
        String compare = "Hello World!";
        fse = FlexibleStringExpander.getInstance(compare);
        assertEquals("null context", compare, fse.expandString(null));
        Map<String, Object> testMap = new HashMap<String, Object>();
        testMap.put("var", "World");
        fse = FlexibleStringExpander.getInstance("Hello ${var}!");
        assertTrue("simple replacement", compare.equals(fse.expandString(testMap)));
        testMap.put("nested", "Hello ${var}");
        fse = FlexibleStringExpander.getInstance("${nested}!");
        assertTrue("hidden (runtime) nested replacement", compare.equals(fse.expandString(testMap)));
        fse = FlexibleStringExpander.getInstance("${'Hello ${var}'}!");
        assertTrue("visible nested replacement", compare.equals(fse.expandString(testMap)));
        fse = FlexibleStringExpander.getInstance("${bsh:return \"Hello \" + var + \"!\";}");
        assertTrue("bsh: script", compare.equals(fse.expandString(testMap)));
        fse = FlexibleStringExpander.getInstance("${groovy:return \"Hello \" + var + \"!\";}");
        assertTrue("groovy: script", compare.equals(fse.expandString(testMap)));
        testMap.put("testMap", testMap);
        fse = FlexibleStringExpander.getInstance("Hello ${testMap.var}!");
        assertTrue("UEL integration: Map", compare.equals(fse.expandString(testMap)));
        List<String> testList = new ArrayList<String>();
        testList.add("World");
        testMap.put("testList", testList);
        fse = FlexibleStringExpander.getInstance("Hello ${testList[0]}!");
        assertTrue("UEL integration: List", compare.equals(fse.expandString(testMap)));
        fse = FlexibleStringExpander.getInstance("This is an \\${escaped} expression");
        assertTrue("Escaped expression", "This is an ${escaped} expression".equals(fse.expandString(testMap)));
    }
}
