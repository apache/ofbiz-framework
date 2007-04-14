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
package org.ofbiz.entity.condition;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericModelException;
import org.ofbiz.entity.config.DatasourceInfo;
import org.ofbiz.entity.model.ModelEntity;

/**
 * Encapsulates a list of EntityConditions to be used as a single EntityCondition combined as specified
 *
 */
public abstract class EntityConditionListBase extends EntityCondition {
    public static final String module = EntityConditionListBase.class.getName();

    protected List conditionList;
    protected EntityJoinOperator operator;

    protected EntityConditionListBase() {}

    public EntityConditionListBase(List conditionList, EntityJoinOperator operator) {
        this.conditionList = conditionList;
        this.operator = operator;
    }

    public EntityOperator getOperator() {
        return this.operator;
    }

    public EntityCondition getCondition(int index) {
        return (EntityCondition) this.conditionList.get(index);
    }
    
    protected int getConditionListSize() {
        return this.conditionList.size();
    }
    
    protected Iterator getConditionIterator() {
        return this.conditionList.iterator();
    }
    
    public void visit(EntityConditionVisitor visitor) {
        visitor.acceptEntityJoinOperator(operator, conditionList);
    }

    public String makeWhereString(ModelEntity modelEntity, List entityConditionParams, DatasourceInfo datasourceInfo) {
        // if (Debug.verboseOn()) Debug.logVerbose("makeWhereString for entity " + modelEntity.getEntityName(), module);
        StringBuffer sql = new StringBuffer();
        operator.addSqlValue(sql, modelEntity, entityConditionParams, conditionList, datasourceInfo);
        return sql.toString();
    }

    public void checkCondition(ModelEntity modelEntity) throws GenericModelException {
        // if (Debug.verboseOn()) Debug.logVerbose("checkCondition for entity " + modelEntity.getEntityName(), module);
        operator.validateSql(modelEntity, conditionList);
    }

    public boolean mapMatches(GenericDelegator delegator, Map map) {
        return operator.mapMatches(delegator, map, conditionList);
    }

    public EntityCondition freeze() {
        return operator.freeze(conditionList);
    }

    public void encryptConditionFields(ModelEntity modelEntity, GenericDelegator delegator) {
        Iterator conditionIter = this.conditionList.iterator();
        while (conditionIter.hasNext()) {
            EntityCondition cond = (EntityCondition) conditionIter.next();
            cond.encryptConditionFields(modelEntity, delegator);
        }
    }
    
    public boolean equals(Object obj) {
        if (!(obj instanceof EntityConditionListBase)) return false;
        EntityConditionListBase other = (EntityConditionListBase) obj;
        
        boolean isEqual = conditionList.equals(other.conditionList) && operator.equals(other.operator);
        //if (!isEqual) {
        //    Debug.logWarning("EntityConditionListBase.equals is false:\n this.operator=" + this.operator + "; other.operator=" + other.operator + 
        //            "\nthis.conditionList=" + this.conditionList +
        //            "\nother.conditionList=" + other.conditionList, module);
        //}
        return isEqual;
    }

    public int hashCode() {
        return conditionList.hashCode() + operator.hashCode();
    }
}
