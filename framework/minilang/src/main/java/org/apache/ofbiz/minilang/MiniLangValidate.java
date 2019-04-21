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
package org.apache.ofbiz.minilang;

import java.util.HashSet;
import java.util.Set;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.ScriptUtil;
import org.apache.ofbiz.base.util.UtilProperties;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * Mini-language validation.
 */
public final class MiniLangValidate {

    public static final String module = MiniLangValidate.class.getName();

    /**
     * Tests <code>element</code> for invalid attribute names.
     * 
     * @param method The <code>&lt;simple-method&gt;</code> that contains <code>element</code> 
     * @param element The <code>element</code> to test
     * @param validAttributeNames The valid attribute names
     * @throws ValidationException If an invalid attribute name is found and <code>validation.level=strict</code>
     */
    public static void attributeNames(SimpleMethod method, Element element, String... validAttributeNames) throws ValidationException {
        Set<String> validNames = new HashSet<>();
        for (String name : validAttributeNames) {
            validNames.add(name);
        }
        NamedNodeMap nnm = element.getAttributes();
        for (int i = 0; i < nnm.getLength(); i++) {
            String attributeName = nnm.item(i).getNodeName();
            if (!validNames.contains(attributeName)) {
                handleError("Attribute name \"" + attributeName + "\" is not valid.", method, element);
            }
        }
    }

    /**
     * Returns <code>attributeValue</code> if it is not empty, else returns <code>defaultValue</code>.
     * No <code>null</code> checks are performed.
     * 
     * @param attributeValue
     * @param defaultValue
     * @return <code>attributeValue</code> if it is not empty, else returns <code>defaultValue</code>
     */
    public static String checkAttribute(String attributeValue, String defaultValue) {
        return attributeValue.isEmpty() ? defaultValue : attributeValue;
    }

    /**
     * Tests <code>element</code> for invalid child elements.
     * 
     * @param method The <code>&lt;simple-method&gt;</code> that contains <code>element</code> 
     * @param element The <code>element</code> to test
     * @param validChildElementNames The valid child element tag names
     * @throws ValidationException If an invalid child element is found and <code>validation.level=strict</code>
     */
    public static void childElements(SimpleMethod method, Element element, String... validChildElementNames) throws ValidationException {
        Set<String> validNames = new HashSet<>();
        for (String name : validChildElementNames) {
            validNames.add(name);
        }
        Node node = element.getFirstChild();
        while (node != null) {
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element childElement = (Element) node;
                if (!validNames.contains(childElement.getTagName())) {
                    handleError("Child element <" + childElement.getTagName() + "> is not valid.", method, element);
                }
            }
            node = node.getNextSibling();
        }
    }

    /**
     * Tests if element attributes are constant type.
     * 
     * @param method The <code>&lt;simple-method&gt;</code> that contains <code>element</code> 
     * @param element The <code>element</code> to test
     * @param attributeNames The attributes to test
     * @throws ValidationException If an invalid attribute is found and <code>validation.level=strict</code>
     */
    public static void constantAttributes(SimpleMethod method, Element element, String... attributeNames) throws ValidationException {
        for (String name : attributeNames) {
            String attributeValue = element.getAttribute(name);
            if (!MiniLangUtil.isConstantAttribute(attributeValue)) {
                handleError("Constant attribute \"" + name + "\" cannot contain an expression.", method, element);
            }
        }
    }

    /**
     * Tests if element attributes are constant+expr type.
     * 
     * @param method The <code>&lt;simple-method&gt;</code> that contains <code>element</code> 
     * @param element The <code>element</code> to test
     * @param attributeNames The attributes to test
     * @throws ValidationException If an invalid attribute is found and <code>validation.level=strict</code>
     */
    public static void constantPlusExpressionAttributes(SimpleMethod method, Element element, String... attributeNames) throws ValidationException {
        for (String name : attributeNames) {
            String attributeValue = element.getAttribute(name);
            if (!MiniLangUtil.isConstantPlusExpressionAttribute(attributeValue)) {
                handleError("Constant+expr attribute \"" + name + "\" is missing a constant value (expression-only constants are not allowed).", method, element);
            }
            if (MiniLangUtil.containsScript(attributeValue)) {
                handleError("Constant+expr attribute \"" + name + "\" cannot contain a script (remove script).", method, element);
            }
        }
    }

    /**
     * Tests <code>element</code> for a deprecated attribute.
     * 
     * @param method The <code>&lt;simple-method&gt;</code> that contains <code>element</code> 
     * @param element The <code>element</code> to test
     * @param attributeName The name of the deprecated attribute
     * @param fixInstruction Instructions to fix the deprecated attribute
     * @throws ValidationException If the deprecated attribute is found and <code>validation.level=strict</code>
     */
    public static void deprecatedAttribute(SimpleMethod method, Element element, String attributeName, String fixInstruction) throws ValidationException {
        String attributeValue = element.getAttribute(attributeName);
        if (attributeValue.length() > 0) {
            handleError("Attribute \"" + attributeName + "\" is deprecated (" + fixInstruction + ")", method, element);
        }
    }

    /**
     * Tests if element attributes are expression type.
     * 
     * @param method The <code>&lt;simple-method&gt;</code> that contains <code>element</code> 
     * @param element The <code>element</code> to test
     * @param attributeNames The attributes to test
     * @throws ValidationException If an invalid attribute is found and <code>validation.level=strict</code>
     */
    public static void expressionAttributes(SimpleMethod method, Element element, String... attributeNames) throws ValidationException {
        for (String name : attributeNames) {
            String attributeValue = element.getAttribute(name);
            if (attributeValue.length() > 0) {
                if (attributeValue.startsWith("${") && attributeValue.endsWith("}")) {
                    attributeValue = attributeValue.substring(2, attributeValue.length() - 1);
                }
                if (MiniLangUtil.containsScript(attributeValue)) {
                    handleError("Expression attribute \"" + name + "\" cannot contain a script (remove script).", method, element);
                }
            }
        }
    }

    /**
     * Handles a Mini-language validation error.
     * 
     * @param errorMessage The error message
     * @param method The <code>&lt;simple-method&gt;</code> that contains <code>element</code> 
     * @param element The <code>element</code> that contains the error
     * @throws ValidationException If <code>validation.level=strict</code>, otherwise a warning is logged
     */
    public static void handleError(String errorMessage, SimpleMethod method, Element element) throws ValidationException {
        ValidationException e = new ValidationException(errorMessage, method, element);
        if (strictOn()) {
            throw e;
        } else {
            Debug.logWarning(e.getMessage(), module);
        }
    }

    /**
     * Returns <code>true</code> if <code>validation.level=lenient</code>.
     * 
     * @return <code>true</code> if <code>validation.level=lenient</code>
     */
    public static boolean lenientOn() {
        return "lenient".equals(UtilProperties.getPropertyValue("minilang", "validation.level"));
    }

    /**
     * Tests <code>element</code> for child elements.
     * 
     * @param method The <code>&lt;simple-method&gt;</code> that contains <code>element</code> 
     * @param element The <code>element</code> to test
     * @throws ValidationException If a child element is found and <code>validation.level=strict</code>
     */
    public static void noChildElements(SimpleMethod method, Element element) throws ValidationException {
        Node node = element.getFirstChild();
        while (node != null) {
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element childElement = (Element) node;
                handleError("Child element <" + childElement.getTagName() + "> is not valid.", method, element);
            }
            node = node.getNextSibling();
        }
    }

    /**
     * Tests <code>element</code> for any one required attribute from a set of attribute names.
     * 
     * @param method The <code>&lt;simple-method&gt;</code> that contains <code>element</code> 
     * @param element The <code>element</code> to test
     * @param attributeNames The required attribute names
     * @throws ValidationException If none of the required attributes are found and <code>validation.level=strict</code>
     */
    public static void requireAnyAttribute(SimpleMethod method, Element element, String... attributeNames) throws ValidationException {
        StringBuilder sb = new StringBuilder();
        for (String name : attributeNames) {
            String attributeValue = element.getAttribute(name);
            if (attributeValue.length() > 0) {
                return;
            }
            if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append("\"").append(name).append("\"");
        }
        handleError("Element must include one of " + sb + " attributes.", method, element);
    }

    /**
     * Tests <code>element</code> for any one required child element from a set of tag names.
     * 
     * @param method The <code>&lt;simple-method&gt;</code> that contains <code>element</code> 
     * @param element The <code>element</code> to test
     * @param elementNames The required child element tag names
     * @throws ValidationException If none of the required child elements are found and <code>validation.level=strict</code>
     */
    public static void requireAnyChildElement(SimpleMethod method, Element element, String... elementNames) throws ValidationException {
        Set<String> childElementNames = new HashSet<>();
        Node node = element.getFirstChild();
        while (node != null) {
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element childElement = (Element) node;
                childElementNames.add(childElement.getTagName());
            }
            node = node.getNextSibling();
        }
        StringBuilder sb = new StringBuilder();
        for (String name : elementNames) {
            if (childElementNames.contains(name)) {
                return;
            }
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append("<").append(name).append(">");
        }
        handleError("Element must include one of " + sb + " child elements.", method, element);
    }

    /**
     * Tests <code>element</code> for required attributes.
     * 
     * @param method The <code>&lt;simple-method&gt;</code> that contains <code>element</code> 
     * @param element The <code>element</code> to test
     * @param attributeNames The required attribute names
     * @throws ValidationException If any of the required attributes are not found and <code>validation.level=strict</code>
     */
    public static void requiredAttributes(SimpleMethod method, Element element, String... attributeNames) throws ValidationException {
        for (String name : attributeNames) {
            String attributeValue = element.getAttribute(name);
            if (attributeValue.length() == 0) {
                handleError("Required attribute \"" + name + "\" is missing.", method, element);
            }
        }
    }

    /**
     * Tests <code>element</code> for required child elements.
     * 
     * @param method The <code>&lt;simple-method&gt;</code> that contains <code>element</code> 
     * @param element The <code>element</code> to test
     * @param elementNames The required child element tag names
     * @throws ValidationException If any of the required child elements are not found and <code>validation.level=strict</code>
     */
    public static void requiredChildElements(SimpleMethod method, Element element, String... elementNames) throws ValidationException {
        Set<String> childElementNames = new HashSet<>();
        Node node = element.getFirstChild();
        while (node != null) {
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element childElement = (Element) node;
                childElementNames.add(childElement.getTagName());
            }
            node = node.getNextSibling();
        }
        for (String name : elementNames) {
            if (!childElementNames.contains(name)) {
                handleError("Required child element <" + name + "> is missing.", method, element);
            }
        }
    }

    /**
     * Tests if element attributes are script type.
     * 
     * @param method The <code>&lt;simple-method&gt;</code> that contains <code>element</code> 
     * @param element The <code>element</code> to test
     * @param attributeNames The attributes to test
     * @throws ValidationException If an invalid attribute is found and <code>validation.level=strict</code>
     */
    public static void scriptAttributes(SimpleMethod method, Element element, String... attributeNames) throws ValidationException {
        for (String name : attributeNames) {
            String attributeValue = element.getAttribute(name).trim();
            if (attributeValue.length() > 0) {
                if (attributeValue.startsWith("${") && attributeValue.endsWith("}")) {
                    handleError("Script attribute \"" + name + "\" enclosed in \"${}\" (remove enclosing ${}).", method, element);
                }
                boolean scriptFound = false;
                for (String scriptName : ScriptUtil.SCRIPT_NAMES) {
                    String scriptPrefix = scriptName.concat(":");
                    if (attributeValue.contains(scriptPrefix)) {
                        scriptFound = true;
                        break;
                    }
                }
                if (!scriptFound) {
                    handleError("Script attribute \"" + name + "\" does not contain a script.", method, element);
                }
            }
        }
    }

    /**
     * Returns <code>true</code> if <code>validation.level=strict</code>.
     * 
     * @return <code>true</code> if <code>validation.level=strict</code>
     */
    public static boolean strictOn() {
        return "strict".equals(UtilProperties.getPropertyValue("minilang", "validation.level"));
    }

    /**
     * Returns <code>true</code> if <code>validation.level</code> is set to lenient or strict.
     * 
     * @return <code>true</code> if <code>validation.level</code> is set to lenient or strict
     */
    public static boolean validationOn() {
        return !"none".equals(UtilProperties.getPropertyValue("minilang", "validation.level"));
    }

    private MiniLangValidate() {}

}
