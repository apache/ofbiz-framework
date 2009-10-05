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

import javolution.context.ObjectFactory;

import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericModelException;
import org.ofbiz.entity.config.DatasourceInfo;
import org.ofbiz.entity.model.ModelEntity;

public class EntityDateFilterCondition extends EntityCondition {

    protected static final ObjectFactory<EntityDateFilterCondition> entityDateFilterConditionFactory = new ObjectFactory<EntityDateFilterCondition>() {
        @Override
        protected EntityDateFilterCondition create() {
            return new EntityDateFilterCondition();
        }
    };

    protected String fromDateName = null;
    protected String thruDateName = null;

    protected EntityDateFilterCondition() {}

    /** @deprecated Use EntityCondition.makeConditionDate() instead */
    @Deprecated
    public EntityDateFilterCondition(String fromDateName, String thruDateName) {
        init(fromDateName, thruDateName);
    }

    public void init(String fromDateName, String thruDateName) {
        this.fromDateName = fromDateName;
        this.thruDateName = thruDateName;
    }

    public void reset() {
        this.fromDateName = null;
        this.thruDateName = null;
    }

    @Override
    public String makeWhereString(ModelEntity modelEntity, List<EntityConditionParam> entityConditionParams, DatasourceInfo datasourceInfo) {
        EntityCondition condition = makeCondition();
        return condition.makeWhereString(modelEntity, entityConditionParams, datasourceInfo);
    }

    @Override
    public void checkCondition(ModelEntity modelEntity) throws GenericModelException {
        EntityCondition condition = makeCondition();
        condition.checkCondition(modelEntity);
    }

    @Override
    public boolean mapMatches(Delegator delegator, Map<String, ? extends Object> map) {
        EntityCondition condition = makeCondition();
        return condition.mapMatches(delegator, map);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof EntityDateFilterCondition)) return false;
        EntityDateFilterCondition other = (EntityDateFilterCondition) obj;
        return equals(fromDateName, other.fromDateName) && equals(thruDateName, other.thruDateName);
    }

    @Override
    public int hashCode() {
        return hashCode(fromDateName) ^ hashCode(thruDateName);
    }

    @Override
    public void visit(EntityConditionVisitor visitor) {
        visitor.acceptEntityDateFilterCondition(this);
    }

    @Override
    public void accept(EntityConditionVisitor visitor) {
        visitor.acceptEntityDateFilterCondition(this);
    }

    @Override
    public EntityCondition freeze() {
        return this;
    }

    @Override
    public void encryptConditionFields(ModelEntity modelEntity, Delegator delegator) {
        // nothing to do here...
    }

    protected EntityCondition makeCondition() {
        return makeCondition(UtilDateTime.nowTimestamp(), fromDateName, thruDateName);
    }

    public static EntityExpr makeCondition(Timestamp moment, String fromDateName, String thruDateName) {
        return EntityCondition.makeCondition(
            EntityCondition.makeCondition(
                EntityCondition.makeCondition(thruDateName, EntityOperator.EQUALS, null),
                EntityOperator.OR,
                EntityCondition.makeCondition(thruDateName, EntityOperator.GREATER_THAN, moment)
           ),
            EntityOperator.AND,
            EntityCondition.makeCondition(
                EntityCondition.makeCondition(fromDateName, EntityOperator.EQUALS, null),
                EntityOperator.OR,
                EntityCondition.makeCondition(fromDateName, EntityOperator.LESS_THAN_EQUAL_TO, moment)
           )
      );
    }
}
