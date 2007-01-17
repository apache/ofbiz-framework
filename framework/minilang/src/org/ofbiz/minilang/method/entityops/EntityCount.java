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

import java.util.Map;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.base.util.collections.FlexibleMapAccessor;
import org.ofbiz.base.util.string.FlexibleStringExpander;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.finder.EntityFinderUtil.Condition;
import org.ofbiz.entity.finder.EntityFinderUtil.ConditionExpr;
import org.ofbiz.entity.finder.EntityFinderUtil.ConditionList;
import org.ofbiz.entity.finder.EntityFinderUtil.ConditionObject;
import org.ofbiz.minilang.SimpleMethod;
import org.ofbiz.minilang.method.MethodContext;
import org.ofbiz.minilang.method.MethodOperation;
import org.w3c.dom.Element;

/**
 * Uses the delegator to find entity values by a condition
 */
public class EntityCount extends MethodOperation {
    
    public static final String module = EntityCount.class.getName();
    
    protected FlexibleStringExpander entityNameExdr;
    protected FlexibleStringExpander delegatorNameExdr;
    protected Condition whereCondition;
    protected Condition havingCondition;
    protected FlexibleMapAccessor countAcsr;

    public EntityCount(Element element, SimpleMethod simpleMethod) {
        super(element, simpleMethod);
        this.entityNameExdr = new FlexibleStringExpander(element.getAttribute("entity-name"));
        this.delegatorNameExdr = new FlexibleStringExpander(element.getAttribute("delegator-name"));
        this.countAcsr = new FlexibleMapAccessor(element.getAttribute("count-name"));
        
        // process condition-expr | condition-list
        Element conditionExprElement = UtilXml.firstChildElement(element, "condition-expr");
        Element conditionListElement = UtilXml.firstChildElement(element, "condition-list");
        Element conditionObjectElement = UtilXml.firstChildElement(element, "condition-object");
        if (conditionExprElement != null && conditionListElement != null) {
            throw new IllegalArgumentException("In entity find by condition element, cannot have condition-expr and condition-list sub-elements");
        }
        if (conditionExprElement != null) {
            this.whereCondition = new ConditionExpr(conditionExprElement);
        } else if (conditionListElement != null) {
            this.whereCondition = new ConditionList(conditionListElement);
        } else if (conditionObjectElement != null) {
            this.whereCondition = new ConditionObject(conditionObjectElement);
        }
        
        // process having-condition-list
        Element havingConditionListElement = UtilXml.firstChildElement(element, "having-condition-list");
        if (havingConditionListElement != null) {
            this.havingCondition = new ConditionList(havingConditionListElement);
        }
    }

    public boolean exec(MethodContext methodContext) {
        try {
            Map context = methodContext.getEnvMap();
            GenericDelegator delegator = methodContext.getDelegator();
            String entityName = this.entityNameExdr.expandString(context);
            String delegatorName = this.delegatorNameExdr.expandString(context);
            
            if (delegatorName != null && delegatorName.length() > 0) {
                delegator = GenericDelegator.getGenericDelegator(delegatorName);
            }

            // create whereEntityCondition from whereCondition
            EntityCondition whereEntityCondition = null;
            if (this.whereCondition != null) {
                whereEntityCondition = this.whereCondition.createCondition(context, entityName, delegator);
            }

            // create havingEntityCondition from havingCondition
            EntityCondition havingEntityCondition = null;
            if (this.havingCondition != null) {
                havingEntityCondition = this.havingCondition.createCondition(context, entityName, delegator);
            }
            
            long count = delegator.findCountByCondition(entityName, whereEntityCondition, havingEntityCondition);
            
            this.countAcsr.put(context, new Long(count));
        } catch (GeneralException e) {
            Debug.logError(e, module);
            String errMsg = "ERROR: Could not complete the " + simpleMethod.getShortDescription() + " process: " + e.getMessage();

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

    public String rawString() {
        // TODO: something more than the empty tag
        return "<entity-count/>";
    }
    public String expandedString(MethodContext methodContext) {
        // TODO: something more than a stub/dummy
        return this.rawString();
    }
}

