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
package org.ofbiz.entity.finder;

import java.util.HashMap;
import java.util.Map;

import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.condition.EntityFieldMap;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.model.ModelEntity;
import org.w3c.dom.Element;

/**
 * Uses the delegator to find entity values by a and
 *
 */
public class ByAndFinder extends ListFinder {
    
    public static final String module = ByAndFinder.class.getName();         
    
    protected Map fieldMap;

    public ByAndFinder(Element element) {
        super(element, "and");
        
        // process field-map
        this.fieldMap = EntityFinderUtil.makeFieldMap(element);
    }

    protected EntityCondition getWhereEntityCondition(Map context, ModelEntity modelEntity, GenericDelegator delegator) {
        // create the by and map
        Map entityContext = new HashMap();
        EntityFinderUtil.expandFieldMapToContext(this.fieldMap, context, entityContext);
        // then convert the types...
        modelEntity.convertFieldMapInPlace(entityContext, delegator);
        return new EntityFieldMap(entityContext, EntityOperator.AND);
    }
}

