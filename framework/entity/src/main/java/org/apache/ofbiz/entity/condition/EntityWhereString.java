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

import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntity;
import org.apache.ofbiz.entity.GenericModelException;
import org.apache.ofbiz.entity.config.model.Datasource;
import org.apache.ofbiz.entity.model.ModelEntity;

/**
 * <p>Encapsulates SQL expressions used for where clause snippets.
 *  NOTE: This is UNSAFE and BREAKS the idea behind the Entity Engine where
 *  you avoid directly specifying SQL. So, KEEP IT MINIMAL and preferably replace
 *  it when the feature you are getting at is implemented in a more automatic way for you.</p>
 *
 * <p>By minimal I mean use this in conjunction with other EntityConditions like the
 *  EntityExpr, EntityConditionList and EntityFieldMap objects which more cleanly
 *  encapsulate where conditions and don't require you to directly write SQL.</p>
 *
 */
@SuppressWarnings("serial")
public final class EntityWhereString extends EntityCondition {

    protected final String sqlString;

    public EntityWhereString(String sqlString) {
        this.sqlString = sqlString;
    }

    @Override
    public boolean isEmpty() {
        return UtilValidate.isEmpty(sqlString);
    }

    @Override
    public String makeWhereString(ModelEntity modelEntity, List<EntityConditionParam> entityConditionParams, Datasource datasourceInfo) {
        return sqlString;
    }

    @Override
    public void checkCondition(ModelEntity modelEntity) throws GenericModelException {// no nothing, this is always assumed to be fine... could do funky SQL syntax checking, but hey this is a HACK anyway
    }

    @Override
    public boolean entityMatches(GenericEntity entity) {
        throw new UnsupportedOperationException("Cannot do entityMatches on a WhereString, ie no SQL evaluation in EE; Where String is: " + sqlString);
    }

    @Override
    public boolean mapMatches(Delegator delegator, Map<String, ? extends Object> map) {
        throw new UnsupportedOperationException("Cannot do mapMatches on a WhereString, ie no SQL evaluation in EE; Where String is: " + sqlString);
    }

    public String getWhereString() {
        return sqlString;
    }

    @Override
    public EntityCondition freeze() {
        return this;
    }

    @Override
    public void visit(EntityConditionVisitor visitor) {
        visitor.acceptEntityWhereString(this);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof EntityWhereString)) return false;
        EntityWhereString other = (EntityWhereString) obj;
        return equals(sqlString, other.sqlString);
    }

    @Override
    public int hashCode() {
        return hashCode(sqlString);
    }
}
