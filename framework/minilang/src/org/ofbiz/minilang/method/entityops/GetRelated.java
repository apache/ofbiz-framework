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

import java.util.List;
import java.util.Map;

import org.ofbiz.base.util.Debug;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.minilang.SimpleMethod;
import org.ofbiz.minilang.method.ContextAccessor;
import org.ofbiz.minilang.method.MethodContext;
import org.ofbiz.minilang.method.MethodOperation;
import org.w3c.dom.Element;

/**
 * Gets a list of related entity instance according to the specified relation-name
 */
public class GetRelated extends MethodOperation {
    public static final class GetRelatedFactory implements Factory<GetRelated> {
        public GetRelated createMethodOperation(Element element, SimpleMethod simpleMethod) {
            return new GetRelated(element, simpleMethod);
        }

        public String getName() {
            return "get-related";
        }
    }

    public static final String module = GetRelated.class.getName();

    ContextAccessor<GenericValue> valueAcsr;
    ContextAccessor<Map<String, ? extends Object>> mapAcsr;
    ContextAccessor<List<String>> orderByListAcsr;
    String relationName;
    String useCacheStr;
    ContextAccessor<List<GenericValue>> listAcsr;

    public GetRelated(Element element, SimpleMethod simpleMethod) {
        super(element, simpleMethod);
        valueAcsr = new ContextAccessor<GenericValue>(element.getAttribute("value-field"), element.getAttribute("value-name"));
        relationName = element.getAttribute("relation-name");
        listAcsr = new ContextAccessor<List<GenericValue>>(element.getAttribute("list"), element.getAttribute("list-name"));
        mapAcsr = new ContextAccessor<Map<String, ? extends Object>>(element.getAttribute("map"), element.getAttribute("map-name"));
        orderByListAcsr = new ContextAccessor<List<String>>(element.getAttribute("order-by-list"), element.getAttribute("order-by-list-name"));

        useCacheStr = element.getAttribute("use-cache");
    }

    @Override
    public boolean exec(MethodContext methodContext) {
        String relationName = methodContext.expandString(this.relationName);
        String useCacheStr = methodContext.expandString(this.useCacheStr);
        boolean useCache = "true".equals(useCacheStr);

        List<String> orderByNames = null;
        if (!orderByListAcsr.isEmpty()) {
            orderByNames = orderByListAcsr.get(methodContext);
        }
        Map<String, ? extends Object> constraintMap = null;
        if (!mapAcsr.isEmpty()) {
            constraintMap = mapAcsr.get(methodContext);
        }

        GenericValue value = valueAcsr.get(methodContext);
        if (value == null) {
            Debug.logWarning("Value not found with name: " + valueAcsr + ", not getting related...", module);
            return true;
        }
        try {
            if (useCache) {
                listAcsr.put(methodContext, value.getRelatedCache(relationName, constraintMap, orderByNames));
            } else {
                listAcsr.put(methodContext, value.getRelated(relationName, constraintMap, orderByNames));
            }
        } catch (GenericEntityException e) {
            String errMsg = "ERROR: Could not complete the " + simpleMethod.getShortDescription() + " process [problem getting related from entity with name " + value.getEntityName() + " for the relation-name: " + relationName + ": " + e.getMessage() + "]";
            Debug.logError(e, errMsg, module);
            methodContext.setErrorReturn(errMsg, simpleMethod);
            return false;
        }
        return true;
    }

    public String getRelationName() {
        return this.relationName;
    }
    @Override
    public String rawString() {
        // TODO: something more than the empty tag
        return "<get-related/>";
    }
    @Override
    public String expandedString(MethodContext methodContext) {
        // TODO: something more than a stub/dummy
        return this.rawString();
    }
}
