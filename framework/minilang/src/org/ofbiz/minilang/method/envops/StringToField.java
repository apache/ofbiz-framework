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

import java.text.*;
import java.util.*;

import org.w3c.dom.*;

import javolution.util.FastMap;

import org.ofbiz.base.util.*;
import org.ofbiz.minilang.*;
import org.ofbiz.minilang.method.*;

/**
 * Copies the specified String to a field
 */
@Deprecated
@MethodOperation.DeprecatedOperation("set")
public class StringToField extends MethodOperation {
    public static final class StringToFieldFactory implements Factory<StringToField> {
        public StringToField createMethodOperation(Element element, SimpleMethod simpleMethod) {
            return new StringToField(element, simpleMethod);
        }

        public String getName() {
            return "string-to-field";
        }
    }

    public static final String module = StringToField.class.getName();

    String string;
    ContextAccessor<Map<String, Object>> mapAcsr;
    ContextAccessor<Object> fieldAcsr;
    ContextAccessor<List<? extends Object>> argListAcsr;
    String messageFieldName;

    public StringToField(Element element, SimpleMethod simpleMethod) {
        super(element, simpleMethod);
        string = element.getAttribute("string");
        mapAcsr = new ContextAccessor<Map<String, Object>>(element.getAttribute("map-name"));
        fieldAcsr = new ContextAccessor<Object>(element.getAttribute("field-name"));
        argListAcsr = new ContextAccessor<List<? extends Object>>(element.getAttribute("arg-list-name"));
        messageFieldName = element.getAttribute("message-field-name");
    }

    @Override
    public boolean exec(MethodContext methodContext) {
        String valueStr = methodContext.expandString(string);

        if (!argListAcsr.isEmpty()) {
            List<? extends Object> argList = argListAcsr.get(methodContext);
            if (UtilValidate.isNotEmpty(argList)) {
                valueStr = MessageFormat.format(valueStr, argList.toArray());
            }
        }

        Object value;
        if (UtilValidate.isNotEmpty(this.messageFieldName)) {
            value = new MessageString(valueStr, this.messageFieldName, true);
        } else {
            value = valueStr;
        }

        if (!mapAcsr.isEmpty()) {
            Map<String, Object> toMap = mapAcsr.get(methodContext);

            if (toMap == null) {
                if (Debug.verboseOn()) Debug.logVerbose("Map not found with name " + mapAcsr + ", creating new map", module);
                toMap = FastMap.newInstance();
                mapAcsr.put(methodContext, toMap);
            }
            fieldAcsr.put(toMap, value, methodContext);
        } else {
            fieldAcsr.put(methodContext, value);
        }

        return true;
    }

    @Override
    public String rawString() {
        // TODO: something more than the empty tag
        return "<string-to-field string=\"" + this.string + "\" field-name=\"" + this.fieldAcsr + "\" map-name=\"" + this.mapAcsr + "\"/>";
    }
    @Override
    public String expandedString(MethodContext methodContext) {
        // TODO: something more than a stub/dummy
        return this.rawString();
    }
}
