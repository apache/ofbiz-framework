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

import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntity;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.minilang.SimpleMethod;
import org.ofbiz.minilang.method.ContextAccessor;
import org.ofbiz.minilang.method.MethodContext;
import org.ofbiz.minilang.method.MethodOperation;
import org.w3c.dom.Element;

/**
 * Uses the delegator to find an entity value by its primary key
 */
public class FindByPrimaryKey extends MethodOperation {
    
    public static final String module = FindByPrimaryKey.class.getName();
    
    ContextAccessor valueAcsr;
    String entityName;
    ContextAccessor mapAcsr;
    String delegatorName;
    String useCacheStr;
    ContextAccessor fieldsToSelectListAcsr;

    public FindByPrimaryKey(Element element, SimpleMethod simpleMethod) {
        super(element, simpleMethod);
        valueAcsr = new ContextAccessor(element.getAttribute("value-name"));
        entityName = element.getAttribute("entity-name");
        mapAcsr = new ContextAccessor(element.getAttribute("map-name"));
        fieldsToSelectListAcsr = new ContextAccessor(element.getAttribute("fields-to-select-list"));
        delegatorName = element.getAttribute("delegator-name");
        useCacheStr = element.getAttribute("use-cache");
    }

    public boolean exec(MethodContext methodContext) {
        String entityName = methodContext.expandString(this.entityName);
        String delegatorName = methodContext.expandString(this.delegatorName);
        String useCacheStr = methodContext.expandString(this.useCacheStr);
        
        boolean useCache = "true".equals(useCacheStr);

        GenericDelegator delegator = methodContext.getDelegator();
        if (delegatorName != null && delegatorName.length() > 0) {
            delegator = GenericDelegator.getGenericDelegator(delegatorName);
        }

        Map inMap = (Map) mapAcsr.get(methodContext);
        if (UtilValidate.isEmpty(entityName) && inMap instanceof GenericEntity) {
            GenericEntity inEntity = (GenericEntity) inMap;
            entityName = inEntity.getEntityName();
        }
        
        List fieldsToSelectList = null;
        if (!fieldsToSelectListAcsr.isEmpty()) {
            fieldsToSelectList = (List) fieldsToSelectListAcsr.get(methodContext);
        }
        
        try {
            if (fieldsToSelectList != null) {
                valueAcsr.put(methodContext, delegator.findByPrimaryKeyPartial(delegator.makePK("Product", inMap), new HashSet(fieldsToSelectList)));
            } else {
                if (useCache) {
                    valueAcsr.put(methodContext, delegator.findByPrimaryKeyCache(entityName, inMap));
                } else {
                    valueAcsr.put(methodContext, delegator.findByPrimaryKey(entityName, inMap));
                }
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            String errMsg = "ERROR: Could not complete the " + simpleMethod.getShortDescription() + " process [problem finding the " + entityName + " entity: " + e.getMessage() + "]";
            methodContext.setErrorReturn(errMsg, simpleMethod);
            return false;
        }
        return true;
    }

    public String rawString() {
        // TODO: something more than the empty tag
        return "<find-by-primary-key/>";
    }
    public String expandedString(MethodContext methodContext) {
        // TODO: something more than a stub/dummy
        return this.rawString();
    }
}
