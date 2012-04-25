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
package org.ofbiz.minilang;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.ofbiz.base.util.ScriptUtil;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.string.FlexibleStringExpander;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;

/**
 * Mini-language utilities.
 */
public final class MiniLangUtil {

    public static final String module = MiniLangUtil.class.getName();

    public static final Set<String> SCRIPT_PREFIXES;

    static {
        Set<String> scriptPrefixes = new HashSet<String>();
        for (String scriptName : ScriptUtil.SCRIPT_NAMES) {
            scriptPrefixes.add(scriptName.concat(":"));
        }
        SCRIPT_PREFIXES = Collections.unmodifiableSet(scriptPrefixes);
    }

    public static boolean containsScript(String str) {
        if (str.length() > 0) {
            for (String scriptPrefix : SCRIPT_PREFIXES) {
                if (str.contains(scriptPrefix)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean autoCorrectOn() {
        return "true".equals(UtilProperties.getPropertyValue("minilang.properties", "autocorrect"));
    }

    public static void flagDocumentAsCorrected(Element element) {
        Document doc = element.getOwnerDocument();
        if (doc != null) {
            doc.setUserData("autoCorrected", "true", null);
        }
    }

    public static boolean isConstantAttribute(String attributeValue) {
        if (attributeValue.length() > 0) {
            return !FlexibleStringExpander.containsExpression(FlexibleStringExpander.getInstance(attributeValue));
        }
        return true;
    }

    public static boolean isConstantPlusExpressionAttribute(String attributeValue) {
        if (attributeValue.length() > 0) {
            if (attributeValue.startsWith("${") && attributeValue.endsWith("}")) {
                // A lot of existing code uses concatenated expressions, and they can be difficult
                // to convert to a single expression, so we will allow them for now.
                String expression = attributeValue.substring(2, attributeValue.length() - 1);
                if (!expression.contains("${")) {
                    return true;
                }
            }
            FlexibleStringExpander fse = FlexibleStringExpander.getInstance(attributeValue);
            return FlexibleStringExpander.containsConstant(fse);
        }
        return true;
    }

    public static boolean isDocumentAutoCorrected(Document document) {
        String autoCorrected = (String) document.getUserData("autoCorrected");
        return "true".equals(autoCorrected);
    }

    public static void removeInvalidAttributes(Element element, String... validAttributeNames) {
        Set<String> validNames = new HashSet<String>();
        for (String name : validAttributeNames) {
            validNames.add(name);
        }
        boolean elementModified = false;
        NamedNodeMap nnm = element.getAttributes();
        for (int i = 0; i < nnm.getLength(); i++) {
            String attributeName = nnm.item(i).getNodeName();
            if (!validNames.contains(attributeName)) {
                element.removeAttribute(attributeName);
                elementModified = true;
            }
        }
        if (elementModified) {
            flagDocumentAsCorrected(element);
        }
    }

    private MiniLangUtil() {}
}
