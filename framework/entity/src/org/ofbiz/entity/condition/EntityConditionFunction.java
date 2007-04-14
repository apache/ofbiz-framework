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

import java.util.List;
import java.util.Map;

import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericModelException;
import org.ofbiz.entity.config.DatasourceInfo;
import org.ofbiz.entity.model.ModelEntity;

/**
 * Encapsulates operations between entities and entity fields. This is a immutable class.
 *
 */
public abstract class EntityConditionFunction extends EntityCondition {

    public static final int ID_NOT = 1;

    public static class NOT extends EntityConditionFunction {
        public NOT(EntityCondition nested) { super(ID_NOT, "NOT", nested); }
        public boolean mapMatches(GenericDelegator delegator, Map map) {
            return !condition.mapMatches(delegator, map);
        }
        public EntityCondition freeze() {
            return new NOT(condition.freeze());
        }
        public void encryptConditionFields(ModelEntity modelEntity, GenericDelegator delegator) {
            // nothing to do here...
        }
    };

    protected int idInt;
    protected String codeString;
    protected EntityCondition condition;

    protected EntityConditionFunction(int id, String code, EntityCondition condition) {
        idInt = id;
        codeString = code;
        this.condition = condition;
    }

    public String getCode() {
        if (codeString == null)
            return "null";
        else
            return codeString;
    }

    public int getId() {
        return idInt;
    }

    public void visit(EntityConditionVisitor visitor) {
        visitor.acceptEntityConditionFunction(this, condition);
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof EntityConditionFunction)) return false;
        EntityConditionFunction otherFunc = (EntityConditionFunction) obj;
        return
            this.idInt == otherFunc.idInt
            && ( this.condition != null ? condition.equals( otherFunc.condition ) : otherFunc.condition != null );
    }

    public int hashCode() {
        return idInt ^ condition.hashCode();
    }

    public String makeWhereString(ModelEntity modelEntity, List entityConditionParams, DatasourceInfo datasourceInfo) {
        StringBuffer sb = new StringBuffer();
        sb.append(codeString).append('(');
        sb.append(condition.makeWhereString(modelEntity, entityConditionParams, datasourceInfo));
        sb.append(')');
        return sb.toString();
    }

    public void checkCondition(ModelEntity modelEntity) throws GenericModelException {
        condition.checkCondition(modelEntity);
    }
}
