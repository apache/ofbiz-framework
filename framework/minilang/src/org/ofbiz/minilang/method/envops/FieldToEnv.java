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

import org.w3c.dom.*;
import org.ofbiz.base.util.*;
import org.ofbiz.minilang.*;
import org.ofbiz.minilang.method.*;

/**
 * Copies a map field to an environment field
 */
@Deprecated
@MethodOperation.DeprecatedOperation("set")
public class FieldToEnv extends MethodOperation {
    public static final class FieldToEnvFactory implements Factory<FieldToEnv> {
        public FieldToEnv createMethodOperation(Element element, SimpleMethod simpleMethod) {
            return new FieldToEnv(element, simpleMethod);
        }

        public String getName() {
            return "field-to-env";
        }
    }

    public static final String module = FieldToEnv.class.getName();

    ContextAccessor<Object> envAcsr;
    ContextAccessor<Map<String, ? extends Object>> mapAcsr;
    ContextAccessor<Object> fieldAcsr;

    public FieldToEnv(Element element, SimpleMethod simpleMethod) {
        super(element, simpleMethod);
        envAcsr = new ContextAccessor<Object>(element.getAttribute("env-name"));
        mapAcsr = new ContextAccessor<Map<String, ? extends Object>>(element.getAttribute("map-name"));
        fieldAcsr = new ContextAccessor<Object>(element.getAttribute("field-name"));

        // set fieldAcsr to their defualt value of envAcsr if empty - this is the way it USED to work, so still supporting it, but a parsing error will result
        if (fieldAcsr.isEmpty()) {
            fieldAcsr = envAcsr;
        }
        // this is the new way that makes more sense, ie the destination should default to the source
        if (envAcsr.isEmpty()) {
            envAcsr = fieldAcsr;
        }
    }

    @Override
    public boolean exec(MethodContext methodContext) {
        Object fieldVal = null;

        if (!mapAcsr.isEmpty()) {
            Map<String, ? extends Object> fromMap = mapAcsr.get(methodContext);

            if (fromMap == null) {
                Debug.logWarning("Map not found with name " + mapAcsr + ", not copying field", module);
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

        envAcsr.put(methodContext, fieldVal);
        return true;
    }

    @Override
    public String rawString() {
        return "<field-to-env env-name=\"" + this.envAcsr + "\" field-name=\"" + this.fieldAcsr + "\" map-name=\"" + this.mapAcsr + "\"/>";
    }
    @Override
    public String expandedString(MethodContext methodContext) {
        // TODO: something more than a stub/dummy
        return this.rawString();
    }
}
