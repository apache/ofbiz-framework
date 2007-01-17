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

import org.ofbiz.base.util.*;
import org.ofbiz.minilang.*;
import org.ofbiz.minilang.method.*;

/**
 * Appends the specified String to a field
 */
public class StringAppend extends MethodOperation {
    
    public static final String module = StringAppend.class.getName();
    
    String string;
    String prefix;
    String suffix;
    ContextAccessor mapAcsr;
    ContextAccessor fieldAcsr;
    ContextAccessor argListAcsr;

    public StringAppend(Element element, SimpleMethod simpleMethod) {
        super(element, simpleMethod);
        string = element.getAttribute("string");
        prefix = element.getAttribute("prefix");
        suffix = element.getAttribute("suffix");
        mapAcsr = new ContextAccessor(element.getAttribute("map-name"));
        fieldAcsr = new ContextAccessor(element.getAttribute("field-name"));
        argListAcsr = new ContextAccessor(element.getAttribute("arg-list-name"));
    }

    public boolean exec(MethodContext methodContext) {
        if (!mapAcsr.isEmpty()) {
            Map toMap = (Map) mapAcsr.get(methodContext);

            if (toMap == null) {
                if (Debug.verboseOn()) Debug.logVerbose("Map not found with name " + mapAcsr + ", creating new map", module);
                toMap = new HashMap();
                mapAcsr.put(methodContext, toMap);
            }
            
            String oldValue = (String) fieldAcsr.get(toMap, methodContext);
            fieldAcsr.put(toMap, this.appendString(oldValue, methodContext), methodContext);
        } else {
            String oldValue = (String) fieldAcsr.get(methodContext);
            fieldAcsr.put(methodContext, this.appendString(oldValue, methodContext));
        }

        return true;
    }
    
    public String appendString(String oldValue, MethodContext methodContext) {
        String value = methodContext.expandString(string);
        String prefixValue = methodContext.expandString(prefix);
        String suffixValue = methodContext.expandString(suffix);
        
        if (!argListAcsr.isEmpty()) {
            List argList = (List) argListAcsr.get(methodContext);
            if (argList != null && argList.size() > 0) {
                value = MessageFormat.format(value, argList.toArray());
            }
        }

        StringBuffer newValue = new StringBuffer();
        if (value != null && value.length() > 0) {
            if (oldValue == null || oldValue.length() == 0) {
                newValue.append(value);
            } else {
                newValue.append(oldValue);
                if (prefixValue != null) newValue.append(prefixValue);
                newValue.append(value);
                if (suffixValue != null) newValue.append(suffixValue);
            }
        } else {
            if (oldValue == null || oldValue.length() == 0) {
                newValue.append(oldValue);
            }
        }
        
        return newValue.toString();
    }

    public String rawString() {
        // TODO: something more than the empty tag
        return "<string-append string=\"" + this.string + "\" prefix=\"" + this.prefix + "\" suffix=\"" + this.suffix + "\" field-name=\"" + this.fieldAcsr + "\" map-name=\"" + this.mapAcsr + "\"/>";
    }
    public String expandedString(MethodContext methodContext) {
        // TODO: something more than a stub/dummy
        return this.rawString();
    }
}
