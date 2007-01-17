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

import org.ofbiz.base.util.Debug;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.minilang.SimpleMethod;
import org.ofbiz.minilang.method.ContextAccessor;
import org.ofbiz.minilang.method.MethodContext;
import org.ofbiz.minilang.method.MethodOperation;
import org.w3c.dom.Element;

/**
 * Uses the delegator to find entity values by anding the map fields
 */
public class CloneValue extends MethodOperation {
    
    public static final String module = CloneValue.class.getName();        
    
    ContextAccessor valueAcsr;
    ContextAccessor newValueAcsr;

    public CloneValue(Element element, SimpleMethod simpleMethod) {
        super(element, simpleMethod);
        valueAcsr = new ContextAccessor(element.getAttribute("value-name"));
        newValueAcsr = new ContextAccessor(element.getAttribute("new-value-name"));
    }

    public boolean exec(MethodContext methodContext) {
        GenericValue value = (GenericValue) valueAcsr.get(methodContext);
        if (value == null) {
            Debug.logWarning("In clone-value a value was not found with the specified valueAcsr: " + valueAcsr + ", not copying", module);
            return true;
        }

        newValueAcsr.put(methodContext, GenericValue.create(value));
        return true;
    }

    public String rawString() {
        // TODO: something more than the empty tag
        return "<clone-value/>";
    }
    public String expandedString(MethodContext methodContext) {
        // TODO: something more than a stub/dummy
        return this.rawString();
    }
}
