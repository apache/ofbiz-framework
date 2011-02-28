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

import java.util.List;
import java.util.Map;

import javolution.util.FastList;

import org.apache.oro.text.regex.MalformedPatternException;
import org.ofbiz.base.util.CompilerMatcher;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.ObjectType;
import org.ofbiz.base.util.string.FlexibleStringExpander;
import org.ofbiz.minilang.SimpleMethod;
import org.ofbiz.minilang.method.ContextAccessor;
import org.ofbiz.minilang.method.MethodContext;
import org.w3c.dom.Element;

/**
 * Implements compare to a constant condition.
 */
public class RegexpCondition implements Conditional {
    public static final class RegexpConditionFactory extends ConditionalFactory<RegexpCondition> {
        @Override
        public RegexpCondition createCondition(Element element, SimpleMethod simpleMethod) {
            return new RegexpCondition(element, simpleMethod);
        }

        @Override
        public String getName() {
            return "if-regexp";
        }
    }


    public static final String module = RegexpCondition.class.getName();

    SimpleMethod simpleMethod;

    private transient static ThreadLocal<CompilerMatcher> compilerMatcher = CompilerMatcher.getThreadLocal();

    List<?> subOps = FastList.newInstance();
    List<?> elseSubOps = null;

    ContextAccessor<Map<String, ? extends Object>> mapAcsr;
    ContextAccessor<Object> fieldAcsr;

    FlexibleStringExpander exprExdr;

    public RegexpCondition(Element element, SimpleMethod simpleMethod) {
        this.simpleMethod = simpleMethod;

        // NOTE: this is still supported, but is deprecated
        this.mapAcsr = new ContextAccessor<Map<String, ? extends Object>>(element.getAttribute("map-name"));
        this.fieldAcsr = new ContextAccessor<Object>(element.getAttribute("field"));
        if (this.fieldAcsr.isEmpty()) {
            // NOTE: this is still supported, but is deprecated
            this.fieldAcsr = new ContextAccessor<Object>(element.getAttribute("field-name"));
        }

        this.exprExdr = FlexibleStringExpander.getInstance(element.getAttribute("expr"));
    }

    public boolean checkCondition(MethodContext methodContext) {
        String fieldString = getFieldString(methodContext);

        boolean matches = false;
        try {
            matches = compilerMatcher.get().matches(fieldString, methodContext.expandString(this.exprExdr));
        } catch (MalformedPatternException e) {
            Debug.logError(e, "Regular Expression [" + this.exprExdr + "] is mal-formed: " + e.toString(), module);
        }

        if (matches) {
            //Debug.logInfo("The string [" + fieldString + "] matched the pattern expr [" + pattern.getPattern() + "]", module);
            return true;
        } else {
            //Debug.logInfo("The string [" + fieldString + "] did NOT match the pattern expr [" + pattern.getPattern() + "]", module);
            return false;
        }
    }

    protected String getFieldString(MethodContext methodContext) {
        String fieldString = null;
        Object fieldVal = null;

        if (!mapAcsr.isEmpty()) {
            Map<String, ? extends Object> fromMap = mapAcsr.get(methodContext);
            if (fromMap == null) {
                if (Debug.infoOn()) Debug.logInfo("Map not found with name " + mapAcsr + ", using empty string for comparison", module);
            } else {
                fieldVal = fieldAcsr.get(fromMap, methodContext);
            }
        } else {
            // no map name, try the env
            fieldVal = fieldAcsr.get(methodContext);
        }

        if (fieldVal != null) {
            try {
                fieldString = (String) ObjectType.simpleTypeConvert(fieldVal, "String", null, methodContext.getTimeZone(), methodContext.getLocale(), true);
            } catch (GeneralException e) {
                Debug.logError(e, "Could not convert object to String, using empty String", module);
            }
        }
        // always use an empty string by default
        if (fieldString == null) fieldString = "";

        return fieldString;
    }

    public void prettyPrint(StringBuilder messageBuffer, MethodContext methodContext) {
        messageBuffer.append("regexp[");
        messageBuffer.append("[");
        if (!this.mapAcsr.isEmpty()) {
            messageBuffer.append(this.mapAcsr);
            messageBuffer.append(".");
        }
        messageBuffer.append(this.fieldAcsr);
        messageBuffer.append("=");
        messageBuffer.append(getFieldString(methodContext));
        messageBuffer.append("] matches ");
        messageBuffer.append(methodContext.expandString(this.exprExdr));
        messageBuffer.append("]");
    }
}
