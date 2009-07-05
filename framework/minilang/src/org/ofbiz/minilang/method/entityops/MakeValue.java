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
package org.ofbiz.minilang.method.entityops;

import java.util.*;

import org.w3c.dom.*;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.minilang.*;
import org.ofbiz.minilang.method.*;

/**
 * Uses the delegator to find entity values by anding the map fields
 */
public class MakeValue extends MethodOperation {
    public static final class MakeValueFactory implements Factory<MakeValue> {
        public MakeValue createMethodOperation(Element element, SimpleMethod simpleMethod) {
            return new MakeValue(element, simpleMethod);
        }

        public String getName() {
            return "make-value";
        }
    }

    ContextAccessor<GenericValue> valueAcsr;
    String entityName;
    ContextAccessor<Map<String, ? extends Object>> mapAcsr;

    public MakeValue(Element element, SimpleMethod simpleMethod) {
        super(element, simpleMethod);
        valueAcsr = new ContextAccessor<GenericValue>(element.getAttribute("value-field"), element.getAttribute("value-name"));
        entityName = element.getAttribute("entity-name");
        mapAcsr = new ContextAccessor<Map<String, ? extends Object>>(element.getAttribute("map"), element.getAttribute("map-name"));
    }

    @Override
    public boolean exec(MethodContext methodContext) {
        String entityName = methodContext.expandString(this.entityName);
        Map<String, ? extends Object> ctxMap = (mapAcsr.isEmpty() ? null : mapAcsr.get(methodContext));
        valueAcsr.put(methodContext, methodContext.getDelegator().makeValidValue(entityName, ctxMap));
        return true;
    }

    public String getEntityName() {
        return this.entityName;
    }

    @Override
    public String rawString() {
        // TODO: something more than the empty tag
        return "<make-value/>";
    }
    @Override
    public String expandedString(MethodContext methodContext) {
        // TODO: something more than a stub/dummy
        return this.rawString();
    }
}
