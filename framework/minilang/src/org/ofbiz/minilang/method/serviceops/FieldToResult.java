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
package org.ofbiz.minilang.method.serviceops;

import java.util.*;

import org.w3c.dom.*;
import org.ofbiz.base.util.*;
import org.ofbiz.minilang.*;
import org.ofbiz.minilang.method.*;

/**
 * Copies a map field to a Service result entry
 */
public class FieldToResult extends MethodOperation {
    public static final class FieldToResultFactory implements Factory<FieldToResult> {
        public FieldToResult createMethodOperation(Element element, SimpleMethod simpleMethod) {
            return new FieldToResult(element, simpleMethod);
        }

        public String getName() {
            return "field-to-result";
        }
    }

    public static final String module = FieldToResult.class.getName();

    ContextAccessor<Map<String, ? extends Object>> mapAcsr;
    ContextAccessor<Object> fieldAcsr;
    ContextAccessor<Object> resultAcsr;

    public FieldToResult(Element element, SimpleMethod simpleMethod) {
        super(element, simpleMethod);
        // the schema for this element now just has the "field" attribute, though the old "field-name" and "map-name" pair is still supported
        mapAcsr = new ContextAccessor<Map<String, ? extends Object>>(element.getAttribute("map-name"));
        fieldAcsr = new ContextAccessor<Object>(element.getAttribute("field"), element.getAttribute("field-name"));
        resultAcsr = new ContextAccessor<Object>(element.getAttribute("result-name"), fieldAcsr.toString());
    }

    @Override
    public boolean exec(MethodContext methodContext) {
        // only run this if it is in an SERVICE context
        if (methodContext.getMethodType() == MethodContext.SERVICE) {
            Object fieldVal = null;

            if (!mapAcsr.isEmpty()) {
                Map<String, ? extends Object> fromMap = mapAcsr.get(methodContext);

                if (fromMap == null) {
                    Debug.logWarning("Map not found with name " + mapAcsr, module);
                    return true;
                }

                fieldVal = fieldAcsr.get(fromMap, methodContext);
            } else {
                // no map name, try the env
                fieldVal = fieldAcsr.get(methodContext);
            }

            if (fieldVal == null) {
                Debug.logWarning("Field value not found with name " + fieldAcsr + " in Map with name " + mapAcsr, module);
                return true;
            }

            resultAcsr.put(methodContext.getResults(), fieldVal, methodContext);
        }
        return true;
    }

    @Override
    public String rawString() {
        // TODO: add all attributes and other info
        return "<field-to-result field-name=\"" + this.fieldAcsr + "\" map-name=\"" + this.mapAcsr + "\"/>";
    }
    @Override
    public String expandedString(MethodContext methodContext) {
        // TODO: something more than a stub/dummy
        return this.rawString();
    }
}
