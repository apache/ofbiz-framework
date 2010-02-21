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

    private static void fseTest(String label, String input, Map<String, ? extends Object> context, Object compare, boolean isEmpty) {
        FlexibleStringExpander fse = FlexibleStringExpander.getInstance(input);
        assertEquals(label, compare, fse.expandString(context));
        assertEquals("isEmpty:" + label, isEmpty, fse.isEmpty());
        assertEquals("static:" + label, compare, FlexibleStringExpander.expandString(input, context));
    }

    public void testFlexibleStringExpander() {
        Map<String, Object> testMap = new HashMap<String, Object>();
        testMap.put("var", "World");
        testMap.put("nested", "Hello ${var}");
        testMap.put("testMap", testMap);
        List<String> testList = new ArrayList<String>();
        testList.add("World");
        testMap.put("testList", testList);
        fseTest("null FlexibleStringExpander", null, null, "", true);
        fseTest("null context", "Hello World!", null, "Hello World!", false);
        fseTest("simple replacement", "Hello ${var}!", testMap, "Hello World!", false);
        fseTest("hidden (runtime) nested replacement", "${nested}!", testMap, "Hello World!", false);
        fseTest("visible nested replacement", "${'Hello ${var}'}!", testMap, "Hello World!", false);
        fseTest("bsh: script", "${bsh:return \"Hello \" + var + \"!\";}", testMap, "Hello World!", false);
        fseTest("groovy: script", "${groovy:return \"Hello \" + var + \"!\";}", testMap, "Hello World!", false);
        fseTest("UEL integration: Map", "Hello ${testMap.var}!", testMap, "Hello World!", false);
        fseTest("UEL integration: List", "Hello ${testList[0]}!", testMap, "Hello World!", false);
        fseTest("Escaped expression", "This is an \\${escaped} expression", testMap, "This is an ${escaped} expression", false);
    }
}
