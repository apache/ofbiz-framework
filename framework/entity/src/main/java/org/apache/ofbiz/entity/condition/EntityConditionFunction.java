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

package org.apache.ofbiz.entity.condition;

import java.util.List;
import java.util.Map;

import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericModelException;
import org.apache.ofbiz.entity.config.model.Datasource;
import org.apache.ofbiz.entity.model.ModelEntity;

/**
 * Base class for entity condition functions.
 *
 */
@SuppressWarnings("serial")
public abstract class EntityConditionFunction extends EntityCondition {

    public static final int ID_NOT = 1;

    public static class NOT extends EntityConditionFunction {
        public NOT(EntityCondition nested) { super(ID_NOT, "NOT", nested); }
        @Override
        public boolean mapMatches(Delegator delegator, Map<String, ? extends Object> map) {
            return !condition.mapMatches(delegator, map);
        }
        @Override
        public EntityCondition freeze() {
            return new NOT(condition.freeze());
        }
    }

    protected Integer idInt = null;
    protected String codeString = null;
    protected EntityCondition condition = null;

    protected EntityConditionFunction(int id, String code, EntityCondition condition) {
        init(id, code, condition);
    }

    public void init(int id, String code, EntityCondition condition) {
        idInt = id;
        codeString = code;
        this.condition = condition;
    }

    public void reset() {
        idInt = null;
        codeString = null;
        this.condition = null;
    }

    public String getCode() {
        if (codeString == null) {
            return "null";
        }
        return codeString;
    }

    public int getId() {
        return idInt;
    }

    @Override
    public void visit(EntityConditionVisitor visitor) {
        visitor.acceptEntityConditionFunction(this, condition);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof EntityConditionFunction)) {
            return false;
        }
        EntityConditionFunction otherFunc = (EntityConditionFunction) obj;
        return this.idInt.equals(otherFunc.idInt) && (this.condition != null ? condition.equals(otherFunc.condition) : otherFunc.condition != null);
    }

    @Override
    public int hashCode() {
        return idInt.hashCode() ^ condition.hashCode();
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public String makeWhereString(ModelEntity modelEntity, List<EntityConditionParam> entityConditionParams, Datasource datasourceInfo) {
        StringBuilder sb = new StringBuilder();
        sb.append(codeString).append('(');
        sb.append(condition.makeWhereString(modelEntity, entityConditionParams, datasourceInfo));
        sb.append(')');
        return sb.toString();
    }

    @Override
    public void checkCondition(ModelEntity modelEntity) throws GenericModelException {
        condition.checkCondition(modelEntity);
    }
}
