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
 * Uses the delegator to refresh the specified value object entity from the datasource
 */
public class RefreshValue extends MethodOperation {
    public static final class RefreshValueFactory implements Factory<RefreshValue> {
        public RefreshValue createMethodOperation(Element element, SimpleMethod simpleMethod) {
            return new RefreshValue(element, simpleMethod);
        }

        public String getName() {
            return "refresh-value";
        }
    }

    public static final String module = RemoveValue.class.getName();

    ContextAccessor<GenericValue> valueAcsr;
    String doCacheClearStr;

    public RefreshValue(Element element, SimpleMethod simpleMethod) {
        super(element, simpleMethod);
        valueAcsr = new ContextAccessor<GenericValue>(element.getAttribute("value-field"), element.getAttribute("value-name"));
        doCacheClearStr = element.getAttribute("do-cache-clear");
    }

    @Override
    public boolean exec(MethodContext methodContext) {
        boolean doCacheClear = !"false".equals(methodContext.expandString(doCacheClearStr));

        GenericValue value = valueAcsr.get(methodContext);
        if (value == null) {
            String errMsg = "In remove-value a value was not found with the specified valueAcsr: " + valueAcsr + ", not removing";
            Debug.logWarning(errMsg, module);
            methodContext.setErrorReturn(errMsg, simpleMethod);
            return false;
        }

        try {
            methodContext.getDelegator().refresh(value, doCacheClear);
        } catch (GenericEntityException e) {
            String errMsg = "ERROR: Could not complete the " + simpleMethod.getShortDescription() + " process [problem removing the " + valueAcsr + " value: " + e.getMessage() + "]";
            Debug.logError(e, errMsg, module);
            methodContext.setErrorReturn(errMsg, simpleMethod);
            return false;
        }
        return true;
    }

    @Override
    public String rawString() {
        // TODO: something more than the empty tag
        return "<refresh-value/>";
    }
    @Override
    public String expandedString(MethodContext methodContext) {
        // TODO: something more than a stub/dummy
        return this.rawString();
    }
}
