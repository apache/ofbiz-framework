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

import java.util.Collection;
import java.util.Map;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.DelegatorFactory;
import org.ofbiz.entity.GenericEntity;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.minilang.SimpleMethod;
import org.ofbiz.minilang.method.ContextAccessor;
import org.ofbiz.minilang.method.MethodContext;
import org.ofbiz.minilang.method.MethodOperation;
import org.w3c.dom.Element;

/**
 * Uses the delegator to find an entity value by its primary key
 */
public class FindByPrimaryKey extends MethodOperation {
    public static final class FindByPrimaryKeyFactory implements Factory<FindByPrimaryKey> {
        public FindByPrimaryKey createMethodOperation(Element element, SimpleMethod simpleMethod) {
            return new FindByPrimaryKey(element, simpleMethod);
        }

        public String getName() {
            return "find-by-primary-key";
        }
    }

    public static final String module = FindByPrimaryKey.class.getName();

    ContextAccessor<GenericValue> valueAcsr;
    String entityName;
    ContextAccessor<Map<String, ? extends Object>> mapAcsr;
    String delegatorName;
    String useCacheStr;
    ContextAccessor<Collection<String>> fieldsToSelectListAcsr;

    public FindByPrimaryKey(Element element, SimpleMethod simpleMethod) {
        super(element, simpleMethod);
        valueAcsr = new ContextAccessor<GenericValue>(element.getAttribute("value-field"), element.getAttribute("value-name"));
        entityName = element.getAttribute("entity-name");
        mapAcsr = new ContextAccessor<Map<String, ? extends Object>>(element.getAttribute("map"), element.getAttribute("map-name"));
        fieldsToSelectListAcsr = new ContextAccessor<Collection<String>>(element.getAttribute("fields-to-select-list"));
        delegatorName = element.getAttribute("delegator-name");
        useCacheStr = element.getAttribute("use-cache");
    }

    @Override
    public boolean exec(MethodContext methodContext) {
        String entityName = methodContext.expandString(this.entityName);
        String delegatorName = methodContext.expandString(this.delegatorName);
        String useCacheStr = methodContext.expandString(this.useCacheStr);

        boolean useCache = "true".equals(useCacheStr);

        Delegator delegator = methodContext.getDelegator();
        if (UtilValidate.isNotEmpty(delegatorName)) {
            delegator = DelegatorFactory.getDelegator(delegatorName);
        }

        Map<String, ? extends Object> inMap = mapAcsr.get(methodContext);
        if (UtilValidate.isEmpty(entityName) && inMap instanceof GenericEntity) {
            GenericEntity inEntity = (GenericEntity) inMap;
            entityName = inEntity.getEntityName();
        }

        Collection<String> fieldsToSelectList = null;
        if (!fieldsToSelectListAcsr.isEmpty()) {
            fieldsToSelectList = fieldsToSelectListAcsr.get(methodContext);
        }

        try {
            if (fieldsToSelectList != null) {
                valueAcsr.put(methodContext, delegator.findByPrimaryKeyPartial(delegator.makePK(entityName, inMap), UtilMisc.makeSetWritable(fieldsToSelectList)));
            } else {
                valueAcsr.put(methodContext, delegator.findOne(entityName, inMap, useCache));
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            String errMsg = "ERROR: Could not complete the " + simpleMethod.getShortDescription() + " process [problem finding the " + entityName + " entity: " + e.getMessage() + "]";
            methodContext.setErrorReturn(errMsg, simpleMethod);
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
        return "<find-by-primary-key/>";
    }
    @Override
    public String expandedString(MethodContext methodContext) {
        // TODO: something more than a stub/dummy
        return this.rawString();
    }
}
