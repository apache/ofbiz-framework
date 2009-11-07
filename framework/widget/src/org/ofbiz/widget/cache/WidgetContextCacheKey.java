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
package org.ofbiz.widget.cache;

import java.util.Map;
import java.util.Set;

import javolution.util.FastMap;
import javolution.util.FastSet;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilGenerics;
import org.ofbiz.base.util.UtilMisc;

public class WidgetContextCacheKey {

    public static final String module = WidgetContextCacheKey.class.getName();

    private static Set<String> fieldNamesToSkip;

    static {
        fieldNamesToSkip = FastSet.newInstance();
        fieldNamesToSkip.add("globalContext");
        fieldNamesToSkip.add("delegator");
        fieldNamesToSkip.add("dispatcher");
        fieldNamesToSkip.add("authz");
        fieldNamesToSkip.add("security");
        fieldNamesToSkip.add("webSiteId");
        fieldNamesToSkip.add("userLogin");
        fieldNamesToSkip.add("screens");
        fieldNamesToSkip.add("nullField");
        fieldNamesToSkip.add("autoUserLogin");
        fieldNamesToSkip.add("person");
        fieldNamesToSkip.add("partyGroup");
        fieldNamesToSkip.add("timeZone");
        fieldNamesToSkip.add("sessionAttributes");
        fieldNamesToSkip.add("requestAttributes");
        fieldNamesToSkip.add("JspTaglibs");
        fieldNamesToSkip.add("requestParameters");
        fieldNamesToSkip.add("page");
        fieldNamesToSkip.add("controlPath");
        fieldNamesToSkip.add("contextRoot");
        fieldNamesToSkip.add("serverRoot");
        fieldNamesToSkip.add("checkLoginUrl");
        fieldNamesToSkip.add("externalLoginKey");
        fieldNamesToSkip.add("externalKeyParam");
        fieldNamesToSkip.add("nowTimestamp");
        fieldNamesToSkip.add("session");
        fieldNamesToSkip.add("request");
        fieldNamesToSkip.add("response");
        fieldNamesToSkip.add("application");
        fieldNamesToSkip.add("formStringRenderer");
        fieldNamesToSkip.add("null");
        fieldNamesToSkip.add("sections");
        fieldNamesToSkip.add("uiLabelMap");
        // remove
        fieldNamesToSkip.add("layoutSettings");
        fieldNamesToSkip.add("activeApp");
        fieldNamesToSkip.add("appheaderTemplate");
        fieldNamesToSkip.add("servletContext");
        // parameters
        fieldNamesToSkip.add("visit");
        fieldNamesToSkip.add("visitor");
    }

    protected Map<String, Object> context;

    public WidgetContextCacheKey(Map<String, ? extends Object> context) {
        this.context = FastMap.newInstance();
        this.context.putAll(context);
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public boolean equals(Object obj) {
        WidgetContextCacheKey key = null;
        if (obj instanceof WidgetContextCacheKey) {
            key = (WidgetContextCacheKey)obj;
        }
        if (key == null || key.context == null) {
            return this.context == null;
        }
        if (this.context == null) {
            return false;
        }

        Set<String> unifiedContext = FastSet.newInstance();
        unifiedContext.addAll(this.context.keySet());
        unifiedContext.addAll(key.context.keySet());
        for (String fieldName: unifiedContext) {
            if (fieldNamesToSkip.contains(fieldName)) {
                continue;
            }
            Object field1 = this.context.get(fieldName);
            Object field2 = key.context.get(fieldName);
            if (field1 == null && field2 == null) {
                continue;
            }
            if ((field1 == null || field2 == null) && field1 != field2) {
                Debug.logWarning("Screen Key doesn't match for :" + fieldName + "; value1 = " + field1 + "; value2 = " + field2, module);
                return false;
            }
            if ("parameters".equals(fieldName)) {
                if (!parametersAreEqual(UtilGenerics.<String, Object>checkMap(field1), UtilGenerics.<String, Object>checkMap(field2))) {
                    return false;
                }
                continue;
            }
            if (!field1.equals(field2)) {
                Debug.logWarning("Screen Key doesn't match for :" + fieldName + "; value1 = " + field1 + "; value2 = " + field2, module);
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        Map<String, Object> printableMap = FastMap.newInstance();
        for (String fieldName: this.context.keySet()) {
            if (!fieldNamesToSkip.contains(fieldName) && !"parameters".equals(fieldName)) {
                printableMap.put(fieldName, this.context.get(fieldName));
            }
        }
        Map<String, Object> parameters = UtilGenerics.checkMap(this.context.get("parameters"));
        return printMap(printableMap) + "\n" + printMap(parameters);
    }

    public static String printMap(Map<String, ? extends Object> map) {
        Map<String, Object> printableMap = FastMap.newInstance();
        for (String fieldName: map.keySet()) {
            if (!fieldNamesToSkip.contains(fieldName) &&
                    !fieldName.startsWith("javax.servlet") &&
                    !fieldName.startsWith("org.apache") &&
                    !fieldName.startsWith("_CLIENT_")) {
                printableMap.put(fieldName, map.get(fieldName));
            }
        }
        return UtilMisc.printMap(printableMap);
    }

    public static boolean parametersAreEqual(Map<String, ? extends Object> map1, Map<String, ? extends Object> map2) {
        Set<String> unifiedContext = FastSet.newInstance();
        unifiedContext.addAll(map1.keySet());
        unifiedContext.addAll(map2.keySet());
        for (String fieldName: unifiedContext) {
            if (fieldNamesToSkip.contains(fieldName)) {
                continue;
            }
            if (fieldName.startsWith("javax.servlet") ||
                    fieldName.startsWith("org.apache") ||
                    fieldName.startsWith("_CLIENT_")) {
                continue;
            }
            Object field1 = map1.get(fieldName);
            Object field2 = map2.get(fieldName);
            if (field1 == null && field2 == null) {
                continue;
            }
            if ((field1 == null || field2 == null) && field1 != field2) {
                Debug.logWarning("Screen Key doesn't match for :" + fieldName + "; value1 = " + field1 + "; value2 = " + field2, module);
                return false;
            }
            if (!field1.equals(field2)) {
                Debug.logWarning("Screen Key doesn't match for :" + fieldName + "; value1 = " + field1 + "; value2 = " + field2, module);
                return false;
            }
        }
        return true;
    }
}
