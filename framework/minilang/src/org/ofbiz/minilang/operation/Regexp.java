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
package org.ofbiz.minilang.operation;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.oro.text.regex.MalformedPatternException;
import org.ofbiz.base.util.CompilerMatcher;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.ObjectType;
import org.w3c.dom.Element;

/**
 * Validates the current field using a regular expression
 */
public class Regexp extends SimpleMapOperation {

    public static final String module = Regexp.class.getName();
    private transient static ThreadLocal<CompilerMatcher> compilerMatcher = CompilerMatcher.getThreadLocal();

    String expr;

    public Regexp(Element element, SimpleMapProcess simpleMapProcess) {
        super(element, simpleMapProcess);
        expr = element.getAttribute("expr");
    }

    @Override
    public void exec(Map<String, Object> inMap, Map<String, Object> results, List<Object> messages, Locale locale, ClassLoader loader) {
        Object obj = inMap.get(fieldName);
        String fieldValue = null;
        try {
            fieldValue = (String) ObjectType.simpleTypeConvert(obj, "String", null, locale);
        } catch (GeneralException e) {
            messages.add("Could not convert field value for comparison: " + e.getMessage());
            return;
        }
        boolean matches = false;
        try {
            matches = compilerMatcher.get().matches(fieldValue, expr);
        } catch (MalformedPatternException e) {
            Debug.logError(e, "Regular Expression [" + this.expr + "] is mal-formed: " + e.toString(), module);
            return;
        }
        if (!matches) {
            addMessage(messages, loader, locale);
        }
    }
}
