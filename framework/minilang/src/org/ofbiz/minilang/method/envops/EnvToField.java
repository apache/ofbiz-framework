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
 * Copies an environment field to a map field
 */
@Deprecated
@MethodOperation.DeprecatedOperation("set")
public class EnvToField extends MethodOperation {
    public static final class EnvToFieldFactory implements Factory<EnvToField> {
        public EnvToField createMethodOperation(Element element, SimpleMethod simpleMethod) {
            return new EnvToField(element, simpleMethod);
        }

        public String getName() {
            return "env-to-field";
        }
    }

    public static final String module = EnvToField.class.getName();

    ContextAccessor<Object> envAcsr;
    ContextAccessor<Map<String, Object>> mapAcsr;
    ContextAccessor<Object> fieldAcsr;

    public EnvToField(Element element, SimpleMethod simpleMethod) {
        super(element, simpleMethod);
        envAcsr = new ContextAccessor<Object>(element.getAttribute("env-name"));
        mapAcsr = new ContextAccessor<Map<String, Object>>(element.getAttribute("map-name"));
        fieldAcsr = new ContextAccessor<Object>(element.getAttribute("field-name"));

        // set fieldAcsr to their defualt value of envAcsr if empty
        if (fieldAcsr.isEmpty()) {
            fieldAcsr = envAcsr;
        }
    }

    @Override
    public boolean exec(MethodContext methodContext) {
        Object envVar = envAcsr.get(methodContext);

        if (envVar == null) {
            Debug.logWarning("Environment field not found with name " + envAcsr + ", not copying env field", module);
            return true;
        }

        if (!mapAcsr.isEmpty()) {
            Map<String, Object> toMap = mapAcsr.get(methodContext);

            if (toMap == null) {
                if (Debug.verboseOn()) Debug.logVerbose("Map not found with name " + mapAcsr + ", creating new map", module);
                toMap = FastMap.newInstance();
                mapAcsr.put(methodContext, toMap);
            }
            fieldAcsr.put(toMap, envVar, methodContext);
        } else {
            // no to-map, so put in env
            fieldAcsr.put(methodContext, envVar);
        }
        return true;
    }

    @Override
    public String rawString() {
        return "<env-to-field env-name=\"" + this.envAcsr + "\" field-name=\"" + this.fieldAcsr + "\" map-name=\"" + this.mapAcsr + "\"/>";
    }
    @Override
    public String expandedString(MethodContext methodContext) {
        // TODO: something more than a stub/dummy
        return this.rawString();
    }
}
