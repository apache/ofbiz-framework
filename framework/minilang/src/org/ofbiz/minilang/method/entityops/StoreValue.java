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
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.minilang.SimpleMethod;
import org.ofbiz.minilang.method.ContextAccessor;
import org.ofbiz.minilang.method.MethodContext;
import org.ofbiz.minilang.method.MethodOperation;
import org.w3c.dom.Element;

/**
 * Uses the delegator to store the specified value object entity in the datasource
 */
public class StoreValue extends MethodOperation {
    public static final class StoreValueFactory implements Factory<StoreValue> {
        public StoreValue createMethodOperation(Element element, SimpleMethod simpleMethod) {
            return new StoreValue(element, simpleMethod);
        }

        public String getName() {
            return "store-value";
        }
    }

    public static final String module = StoreValue.class.getName();

    ContextAccessor<GenericValue> valueAcsr;
    String doCacheClearStr;

    public StoreValue(Element element, SimpleMethod simpleMethod) {
        super(element, simpleMethod);
        valueAcsr = new ContextAccessor<GenericValue>(element.getAttribute("value-field"), element.getAttribute("value-name"));
        doCacheClearStr = element.getAttribute("do-cache-clear");
    }

    @Override
    public boolean exec(MethodContext methodContext) {
        boolean doCacheClear = !"false".equals(methodContext.expandString(doCacheClearStr));

        GenericValue value = null;
        try {
            value = valueAcsr.get(methodContext);
        } catch (ClassCastException e) {
            String errMsg = "In store-value the value specified by valueAcsr [" + valueAcsr + "] was not an instance of GenericValue, not storing";
            Debug.logError(errMsg, module);
            methodContext.setErrorReturn(errMsg, simpleMethod);
            return false;
        }
        if (value == null) {
            String errMsg = "In store-value a value was not found with the specified valueAcsr: " + valueAcsr + ", not storing";
            Debug.logWarning(errMsg, module);
            methodContext.setErrorReturn(errMsg, simpleMethod);
            return false;
        }

        try {
            methodContext.getDelegator().store(value, doCacheClear);
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            String errMsg = "ERROR: Could not complete the " + simpleMethod.getShortDescription() + " process [problem storing the " + valueAcsr + " value: " + e.getMessage() + "]";
            methodContext.setErrorReturn(errMsg, simpleMethod);
            return false;
        }
        return true;
    }

    @Override
    public String rawString() {
        // TODO: something more than the empty tag
        return "<store-value/>";
    }
    @Override
    public String expandedString(MethodContext methodContext) {
        // TODO: something more than a stub/dummy
        return this.rawString();
    }
}
