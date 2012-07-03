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
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.base.util.collections.FlexibleMapAccessor;
import org.ofbiz.base.util.string.FlexibleStringExpander;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.DelegatorFactory;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.finder.EntityFinderUtil.Condition;
import org.ofbiz.entity.finder.EntityFinderUtil.ConditionExpr;
import org.ofbiz.entity.finder.EntityFinderUtil.ConditionList;
import org.ofbiz.entity.finder.EntityFinderUtil.ConditionObject;
import org.ofbiz.entity.model.ModelEntity;
import org.ofbiz.minilang.MiniLangException;
import org.ofbiz.minilang.MiniLangValidate;
import org.ofbiz.minilang.SimpleMethod;
import org.ofbiz.minilang.artifact.ArtifactInfoContext;
import org.ofbiz.minilang.method.MethodContext;
import org.ofbiz.minilang.method.MethodOperation;
import org.w3c.dom.Element;

/**
 * Implements the &lt;entity-count&gt; element.
 * 
 * @see <a href="https://cwiki.apache.org/OFBADMIN/mini-language-reference.html#Mini-languageReference-{{%3Centitycount%3E}}">Mini-language Reference</a>
 */
public final class EntityCount extends MethodOperation {

    public static final String module = EntityCount.class.getName();

    private final FlexibleMapAccessor<Long> countFma;
    private final FlexibleStringExpander delegatorNameFse;
    private final FlexibleStringExpander entityNameFse;
    private final Condition havingCondition;
    private final Condition whereCondition;

    public EntityCount(Element element, SimpleMethod simpleMethod) throws MiniLangException {
        super(element, simpleMethod);
        if (MiniLangValidate.validationOn()) {
            MiniLangValidate.attributeNames(simpleMethod, element, "entity-name", "count-field", "delegator-name");
            MiniLangValidate.requiredAttributes(simpleMethod, element, "entity-name", "count-field");
            MiniLangValidate.expressionAttributes(simpleMethod, element, "count-field");
            MiniLangValidate.childElements(simpleMethod, element, "condition-expr", "condition-list", "condition-object", "having-condition-list");
            MiniLangValidate.requireAnyChildElement(simpleMethod, element, "condition-expr", "condition-list", "condition-object");
        }
        this.entityNameFse = FlexibleStringExpander.getInstance(element.getAttribute("entity-name"));
        this.delegatorNameFse = FlexibleStringExpander.getInstance(element.getAttribute("delegator-name"));
        this.countFma = FlexibleMapAccessor.getInstance(element.getAttribute("count-field"));
        int conditionElementCount = 0;
        Element conditionExprElement = UtilXml.firstChildElement(element, "condition-expr");
        conditionElementCount = conditionExprElement == null ? conditionElementCount : conditionElementCount++;
        Element conditionListElement = UtilXml.firstChildElement(element, "condition-list");
        conditionElementCount = conditionListElement == null ? conditionElementCount : conditionElementCount++;
        Element conditionObjectElement = UtilXml.firstChildElement(element, "condition-object");
        conditionElementCount = conditionObjectElement == null ? conditionElementCount : conditionElementCount++;
        if (conditionElementCount > 1) {
            MiniLangValidate.handleError("Element must include only one condition child element", simpleMethod, conditionObjectElement);
        }
        if (conditionExprElement != null) {
            this.whereCondition = new ConditionExpr(conditionExprElement);
        } else if (conditionListElement != null) {
            this.whereCondition = new ConditionList(conditionListElement);
        } else if (conditionObjectElement != null) {
            this.whereCondition = new ConditionObject(conditionObjectElement);
        } else {
            this.whereCondition = null;
        }
        Element havingConditionListElement = UtilXml.firstChildElement(element, "having-condition-list");
        if (havingConditionListElement != null) {
            this.havingCondition = new ConditionList(havingConditionListElement);
        } else {
            this.havingCondition = null;
        }
    }

    @Override
    public boolean exec(MethodContext methodContext) throws MiniLangException {
        try {
            String delegatorName = this.delegatorNameFse.expandString(methodContext.getEnvMap());
            Delegator delegator = methodContext.getDelegator();
            if (UtilValidate.isNotEmpty(delegatorName)) {
                delegator = DelegatorFactory.getDelegator(delegatorName);
            }
            String entityName = this.entityNameFse.expandString(methodContext.getEnvMap());
            ModelEntity modelEntity = delegator.getModelEntity(entityName);
            EntityCondition whereEntityCondition = null;
            if (this.whereCondition != null) {
                whereEntityCondition = this.whereCondition.createCondition(methodContext.getEnvMap(), modelEntity, delegator.getModelFieldTypeReader(modelEntity));
            }
            EntityCondition havingEntityCondition = null;
            if (this.havingCondition != null) {
                havingEntityCondition = this.havingCondition.createCondition(methodContext.getEnvMap(), modelEntity, delegator.getModelFieldTypeReader(modelEntity));
            }
            long count = delegator.findCountByCondition(entityName, whereEntityCondition, havingEntityCondition, null);
            this.countFma.put(methodContext.getEnvMap(), count);
        } catch (GeneralException e) {
            String errMsg = "Exception thrown while performing entity count: " + e.getMessage();
            Debug.logWarning(e, errMsg, module);
            simpleMethod.addErrorMessage(methodContext, errMsg);
            return false;
        }
        return true;
    }

    @Override
    public void gatherArtifactInfo(ArtifactInfoContext aic) {
        aic.addEntityName(entityNameFse.toString());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("<entity-count ");
        sb.append("entity-name=\"").append(this.entityNameFse).append("\" ");
        sb.append("count-field=\"").append(this.countFma).append("\" ");
        if (!this.delegatorNameFse.isEmpty()) {
            sb.append("delegator-name=\"").append(this.delegatorNameFse).append("\" ");
        }
        sb.append("/>");
        return sb.toString();
    }

    /**
     * A factory for the &lt;entity-count&gt; element.
     */
    public static final class EntityCountFactory implements Factory<EntityCount> {
        @Override
        public EntityCount createMethodOperation(Element element, SimpleMethod simpleMethod) throws MiniLangException {
            return new EntityCount(element, simpleMethod);
        }

        @Override
        public String getName() {
            return "entity-count";
        }
    }
}
