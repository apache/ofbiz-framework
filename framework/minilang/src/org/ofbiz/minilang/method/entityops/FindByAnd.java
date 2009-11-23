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
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.DelegatorFactory;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.minilang.SimpleMethod;
import org.ofbiz.minilang.method.ContextAccessor;
import org.ofbiz.minilang.method.MethodContext;
import org.ofbiz.minilang.method.MethodOperation;
import org.w3c.dom.Element;

/**
 * Uses the delegator to find entity values by anding the map fields
 */
public class FindByAnd extends MethodOperation {
    public static final class FindByAndFactory implements Factory<FindByAnd> {
        public FindByAnd createMethodOperation(Element element, SimpleMethod simpleMethod) {
            return new FindByAnd(element, simpleMethod);
        }

        public String getName() {
            return "find-by-and";
        }
    }

    public static final String module = FindByAnd.class.getName();

    ContextAccessor<Object> listAcsr;
    String entityName;
    ContextAccessor<Map<String, ? extends Object>> mapAcsr;
    ContextAccessor<List<String>> orderByListAcsr;
    String delegatorName;
    String useCacheStr;
    String useIteratorStr;

    public FindByAnd(Element element, SimpleMethod simpleMethod) {
        super(element, simpleMethod);
        listAcsr = new ContextAccessor<Object>(element.getAttribute("list"), element.getAttribute("list-name"));
        entityName = element.getAttribute("entity-name");
        mapAcsr = new ContextAccessor<Map<String, ? extends Object>>(element.getAttribute("map"), element.getAttribute("map-name"));
        orderByListAcsr = new ContextAccessor<List<String>>(element.getAttribute("order-by-list"), element.getAttribute("order-by-list-name"));
        delegatorName = element.getAttribute("delegator-name");

        useCacheStr = element.getAttribute("use-cache");
        useIteratorStr = element.getAttribute("use-iterator");
    }

    @Override
    public boolean exec(MethodContext methodContext) {
        String entityName = methodContext.expandString(this.entityName);
        String delegatorName = methodContext.expandString(this.delegatorName);
        String useCacheStr = methodContext.expandString(this.useCacheStr);
        String useIteratorStr = methodContext.expandString(this.useIteratorStr);

        boolean useCache = "true".equals(useCacheStr);
        boolean useIterator = "true".equals(useIteratorStr);

        List<String> orderByNames = null;
        if (!orderByListAcsr.isEmpty()) {
            orderByNames = orderByListAcsr.get(methodContext);
        }

        Delegator delegator = methodContext.getDelegator();
        if (UtilValidate.isNotEmpty(delegatorName)) {
            delegator = DelegatorFactory.getDelegator(delegatorName);
        }

        try {
            if (useIterator) {
                EntityCondition whereCond = null;
                if (!mapAcsr.isEmpty()) {
                    whereCond = EntityCondition.makeCondition(mapAcsr.get(methodContext));
                }
                listAcsr.put(methodContext, delegator.find(entityName, whereCond, null, null, orderByNames, null));
            } else {
                if (useCache) {
                    listAcsr.put(methodContext, delegator.findByAndCache(entityName, mapAcsr.get(methodContext), orderByNames));
                } else {
                    listAcsr.put(methodContext, delegator.findByAnd(entityName, mapAcsr.get(methodContext), orderByNames));
                }
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            String errMsg = "ERROR: Could not complete the " + simpleMethod.getShortDescription() + " process [problem finding the " + entityName + " entity: " + e.getMessage() + "]";

            if (methodContext.getMethodType() == MethodContext.EVENT) {
                methodContext.putEnv(simpleMethod.getEventErrorMessageName(), errMsg);
                methodContext.putEnv(simpleMethod.getEventResponseCodeName(), simpleMethod.getDefaultErrorCode());
            } else if (methodContext.getMethodType() == MethodContext.SERVICE) {
                methodContext.putEnv(simpleMethod.getServiceErrorMessageName(), errMsg);
                methodContext.putEnv(simpleMethod.getServiceResponseMessageName(), simpleMethod.getDefaultErrorCode());
            }
            return false;
        }
        return true;
    }

    public String getEntityName() {
        return this.entityName;
    }

    @Override
    public String rawString() {
        // TODO: something more than the empty tag
        return "<find-by-and/>";
    }
    @Override
    public String expandedString(MethodContext methodContext) {
        // TODO: something more than a stub/dummy
        return this.rawString();
    }
}
