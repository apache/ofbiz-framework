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
package org.ofbiz.minilang.method.envops;

import java.text.MessageFormat;
import java.util.List;
import java.util.Map;

import javolution.util.FastMap;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.minilang.MiniLangException;
import org.ofbiz.minilang.SimpleMethod;
import org.ofbiz.minilang.method.ContextAccessor;
import org.ofbiz.minilang.method.MethodContext;
import org.ofbiz.minilang.method.MethodOperation;
import org.w3c.dom.Element;

/**
 * Appends the specified String to a field
 */
public class StringAppend extends MethodOperation {

    public static final String module = StringAppend.class.getName();

    ContextAccessor<List<? extends Object>> argListAcsr;
    ContextAccessor<String> fieldAcsr;
    ContextAccessor<Map<String, Object>> mapAcsr;
    String prefix;
    String string;
    String suffix;

    public StringAppend(Element element, SimpleMethod simpleMethod) throws MiniLangException {
        super(element, simpleMethod);
        string = element.getAttribute("string");
        prefix = element.getAttribute("prefix");
        suffix = element.getAttribute("suffix");
        // the schema for this element now just has the "field" attribute, though the old "field-name" and "map-name" pair is still supported
        fieldAcsr = new ContextAccessor<String>(element.getAttribute("field"), element.getAttribute("field-name"));
        mapAcsr = new ContextAccessor<Map<String, Object>>(element.getAttribute("map-name"));
        argListAcsr = new ContextAccessor<List<? extends Object>>(element.getAttribute("arg-list"), element.getAttribute("arg-list-name"));
    }

    public String appendString(String oldValue, MethodContext methodContext) {
        String value = methodContext.expandString(string);
        String prefixValue = methodContext.expandString(prefix);
        String suffixValue = methodContext.expandString(suffix);
        if (!argListAcsr.isEmpty()) {
            List<? extends Object> argList = argListAcsr.get(methodContext);
            if (UtilValidate.isNotEmpty(argList)) {
                value = MessageFormat.format(value, argList.toArray());
            }
        }
        StringBuilder newValue = new StringBuilder();
        if (UtilValidate.isNotEmpty(value)) {
            if (UtilValidate.isEmpty(oldValue)) {
                newValue.append(value);
            } else {
                newValue.append(oldValue);
                if (prefixValue != null)
                    newValue.append(prefixValue);
                newValue.append(value);
                if (suffixValue != null)
                    newValue.append(suffixValue);
            }
        } else {
            if (UtilValidate.isEmpty(oldValue)) {
                newValue.append(oldValue);
            }
        }
        return newValue.toString();
    }

    @Override
    public boolean exec(MethodContext methodContext) throws MiniLangException {
        if (!mapAcsr.isEmpty()) {
            Map<String, Object> toMap = mapAcsr.get(methodContext);
            if (toMap == null) {
                if (Debug.verboseOn())
                    Debug.logVerbose("Map not found with name " + mapAcsr + ", creating new map", module);
                toMap = FastMap.newInstance();
                mapAcsr.put(methodContext, toMap);
            }
            String oldValue = fieldAcsr.get(toMap, methodContext);
            fieldAcsr.put(toMap, this.appendString(oldValue, methodContext), methodContext);
        } else {
            String oldValue = fieldAcsr.get(methodContext);
            fieldAcsr.put(methodContext, this.appendString(oldValue, methodContext));
        }
        return true;
    }

    @Override
    public String expandedString(MethodContext methodContext) {
        // TODO: something more than a stub/dummy
        return this.rawString();
    }

    @Override
    public String rawString() {
        // TODO: something more than the empty tag
        return "<string-append string=\"" + this.string + "\" prefix=\"" + this.prefix + "\" suffix=\"" + this.suffix + "\" field-name=\"" + this.fieldAcsr + "\" map-name=\"" + this.mapAcsr + "\"/>";
    }

    public static final class StringAppendFactory implements Factory<StringAppend> {
        public StringAppend createMethodOperation(Element element, SimpleMethod simpleMethod) throws MiniLangException {
            return new StringAppend(element, simpleMethod);
        }

        public String getName() {
            return "string-append";
        }
    }
}
