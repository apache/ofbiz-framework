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
package org.ofbiz.minilang.method.conditional;

import org.w3c.dom.*;
import org.ofbiz.base.util.*;
import org.ofbiz.minilang.*;

/**
 * Creates Conditional objects according to the element that is passed.
 */
public class ConditionalFactory {
    
    public static final String module = ConditionalFactory.class.getName();
    
    public static Conditional makeConditional(Element element, SimpleMethod simpleMethod) {
        String tagName = element.getTagName();
        
        if ("or".equals(tagName)) {
            return new CombinedCondition(element, CombinedCondition.OR, simpleMethod);
        } else if ("xor".equals(tagName)) {
            return new CombinedCondition(element, CombinedCondition.XOR, simpleMethod);
        } else if ("and".equals(tagName)) {
            return new CombinedCondition(element, CombinedCondition.AND, simpleMethod);
        } else if ("not".equals(tagName)) {
            return new CombinedCondition(element, CombinedCondition.NOT, simpleMethod);
        } else if ("if-validate-method".equals(tagName)) {
            return new ValidateMethodCondition(element);
        } else if ("if-compare".equals(tagName)) {
            return new CompareCondition(element, simpleMethod);
        } else if ("if-compare-field".equals(tagName)) {
            return new CompareFieldCondition(element, simpleMethod);
        } else if ("if-empty".equals(tagName)) {
            return new EmptyCondition(element, simpleMethod);
        } else if ("if-regexp".equals(tagName)) {
            return new RegexpCondition(element, simpleMethod);
        } else if ("if-has-permission".equals(tagName)) {
            return new HasPermissionCondition(element, simpleMethod);
        } else {
            Debug.logWarning("Found an unknown if condition: " + tagName, module);
            return null;
        }
    }
}
