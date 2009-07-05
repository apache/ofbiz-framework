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

import java.util.*;

import javolution.util.FastMap;

import org.w3c.dom.*;
import org.ofbiz.base.util.*;
import org.ofbiz.minilang.*;
import org.ofbiz.minilang.method.*;

/**
 * Copies a map field to a map field
 */
@Deprecated
@MethodOperation.DeprecatedOperation("set")
public class FieldToField extends MethodOperation {
    public static final class FieldToFieldFactory implements Factory<FieldToField> {
        public FieldToField createMethodOperation(Element element, SimpleMethod simpleMethod) {
            return new FieldToField(element, simpleMethod);
        }

        public String getName() {
            return "field-to-field";
        }
    }

    public static final String module = FieldToField.class.getName();

    ContextAccessor<Map<String, Object>> mapAcsr;
    ContextAccessor<Object> fieldAcsr;
    ContextAccessor<Map<String, Object>> toMapAcsr;
    ContextAccessor<Object> toFieldAcsr;

    public FieldToField(Element element, SimpleMethod simpleMethod) {
        super(element, simpleMethod);
        mapAcsr = new ContextAccessor<Map<String, Object>>(element.getAttribute("map-name"));
        fieldAcsr = new ContextAccessor<Object>(element.getAttribute("field-name"));
        toMapAcsr = new ContextAccessor<Map<String, Object>>(element.getAttribute("to-map-name"));
        toFieldAcsr = new ContextAccessor<Object>(element.getAttribute("to-field-name"));

        // set toMapAcsr and toFieldAcsr to their defualt values of mapAcsr and fieldAcsr if empty
        if (toMapAcsr.isEmpty()) {
            toMapAcsr = mapAcsr;
        }
        if (toFieldAcsr.isEmpty()) {
            toFieldAcsr = fieldAcsr;
        }
    }

    @Override
    public boolean exec(MethodContext methodContext) {
        Object fieldVal = null;

        if (!mapAcsr.isEmpty()) {
            Map<String, ? extends Object> fromMap = mapAcsr.get(methodContext);

            if (fromMap == null) {
                if (Debug.infoOn()) Debug.logInfo("Map not found with name " + mapAcsr + ", not copying from this map", module);
                return true;
            }

            fieldVal = fieldAcsr.get(fromMap, methodContext);
        } else {
            // no map name, try the env
            fieldVal = fieldAcsr.get(methodContext);
        }

        if (fieldVal == null) {
            if (Debug.verboseOn()) Debug.logVerbose("Field value not found with name " + fieldAcsr + " in Map with name " + mapAcsr + ", not copying field", module);
            return true;
        }

        // note that going to an env field will only work if it came from an env
        // field because if not specified the to-map-name will be set to the map-name
        // to go from a map field to an env field, use the field-to-env operation
        Map<String, Object> toMap = null;

        if (!toMapAcsr.isEmpty()) {
            toMap = toMapAcsr.get(methodContext);
            if (toMap == null) {
                if (Debug.verboseOn()) Debug.logVerbose("Map not found with name " + toMapAcsr + ", creating new map", module);
                toMap = FastMap.newInstance();
                toMapAcsr.put(methodContext, toMap);
            }
            toFieldAcsr.put(toMap, fieldVal, methodContext);
        } else {
            // no to-map, so put in env
            toFieldAcsr.put(methodContext, fieldVal);
        }

        return true;
    }

    @Override
    public String rawString() {
        return "<field-to-field field-name=\"" + this.fieldAcsr + "\" map-name=\"" + this.mapAcsr + "\" to-field-name=\"" + this.toFieldAcsr + "\" to-map-name=\"" + this.toMapAcsr + "\"/>";
    }
    @Override
    public String expandedString(MethodContext methodContext) {
        // TODO: something more than a stub/dummy
        return this.rawString();
    }
}
