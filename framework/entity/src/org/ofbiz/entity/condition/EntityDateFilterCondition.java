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

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericModelException;
import org.ofbiz.entity.config.DatasourceInfo;
import org.ofbiz.entity.model.ModelEntity;

public class EntityDateFilterCondition extends EntityCondition {

    protected String fromDateName;
    protected String thruDateName;

    public EntityDateFilterCondition(String fromDateName, String thruDateName) {
        this.fromDateName = fromDateName;
        this.thruDateName = thruDateName;
    }

    public String makeWhereString(ModelEntity modelEntity, List entityConditionParams, DatasourceInfo datasourceInfo) {
        EntityCondition condition = makeCondition();
        return condition.makeWhereString(modelEntity, entityConditionParams, datasourceInfo);
    }

    public void checkCondition(ModelEntity modelEntity) throws GenericModelException {
        EntityCondition condition = makeCondition();
        condition.checkCondition(modelEntity);
    }

    public boolean mapMatches(GenericDelegator delegator, Map map) {    
        EntityCondition condition = makeCondition();
        return condition.mapMatches(delegator, map);
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof EntityDateFilterCondition)) return false;
        EntityDateFilterCondition other = (EntityDateFilterCondition) obj;
        return equals(fromDateName, other.fromDateName) && equals(thruDateName, other.thruDateName);
    }

    public int hashCode() {
        return hashCode(fromDateName) ^ hashCode(thruDateName);
    }

    public void visit(EntityConditionVisitor visitor) {
        visitor.acceptEntityDateFilterCondition(this);
    }

    public void accept(EntityConditionVisitor visitor) {
        visitor.acceptEntityDateFilterCondition(this);
    }

    public EntityCondition freeze() {
        return this;
    }

    public void encryptConditionFields(ModelEntity modelEntity, GenericDelegator delegator) {
        // nothing to do here...
    }

    protected EntityCondition makeCondition() {
        return makeCondition(UtilDateTime.nowTimestamp(), fromDateName, thruDateName);
    }

    public static EntityExpr makeCondition(Timestamp moment, String fromDateName, String thruDateName) {
        return new EntityExpr(
            new EntityExpr(
                new EntityExpr( thruDateName, EntityOperator.EQUALS, null ),
                EntityOperator.OR,
                new EntityExpr( thruDateName, EntityOperator.GREATER_THAN, moment )
            ),
            EntityOperator.AND,
            new EntityExpr(
                new EntityExpr( fromDateName, EntityOperator.EQUALS, null ),
                EntityOperator.OR,
                new EntityExpr( fromDateName, EntityOperator.LESS_THAN_EQUAL_TO, moment )
            )
       );
    }
}
