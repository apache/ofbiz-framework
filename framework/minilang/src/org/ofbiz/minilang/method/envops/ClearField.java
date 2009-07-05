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
 * Clears the specified field
 */
public class ClearField extends MethodOperation {
    public static final class ClearFieldFactory implements Factory<ClearField> {
        public ClearField createMethodOperation(Element element, SimpleMethod simpleMethod) {
            return new ClearField(element, simpleMethod);
        }

        public String getName() {
            return "clear-field";
        }
    }

    public static final String module = ClearField.class.getName();

    ContextAccessor<Map<String, Object>> mapAcsr;
    ContextAccessor<Object> fieldAcsr;

    public ClearField(Element element, SimpleMethod simpleMethod) {
        super(element, simpleMethod);

        // the schema for this element now just has the "field" attribute, though the old "field-name" and "map-name" pair is still supported
        fieldAcsr = new ContextAccessor<Object>(element.getAttribute("field"), element.getAttribute("field-name"));
        mapAcsr = new ContextAccessor<Map<String, Object>>(element.getAttribute("map-name"));
    }

    @Override
    public boolean exec(MethodContext methodContext) {
        if (!mapAcsr.isEmpty()) {
            Map<String, Object> toMap = mapAcsr.get(methodContext);

            if (toMap == null) {
                // it seems silly to create a new map, but necessary since whenever
                // an env field like a Map or List is referenced it should be created, even if empty
                if (Debug.verboseOn()) Debug.logVerbose("Map not found with name " + mapAcsr + ", creating new map", module);
                toMap = FastMap.newInstance();
                mapAcsr.put(methodContext, toMap);
            }

            fieldAcsr.put(toMap, null, methodContext);
        } else {
            fieldAcsr.put(methodContext, null);
        }

        return true;
    }

    @Override
    public String rawString() {
        return "<clear-field field-name=\"" + this.fieldAcsr + "\" map-name=\"" + this.mapAcsr + "\"/>";
    }
    @Override
    public String expandedString(MethodContext methodContext) {
        // TODO: something more than a stub/dummy
        return this.rawString();
    }
}
