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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javolution.util.FastList;

import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericModelException;
import org.ofbiz.entity.GenericEntity;
import org.ofbiz.entity.config.DatasourceInfo;
import org.ofbiz.entity.model.ModelEntity;

/**
 * Represents the conditions to be used to constrain a query
 * <br/>An EntityCondition can represent various type of constraints, including:
 * <ul>
 *  <li>EntityConditionList: a list of EntityConditions, combined with the operator specified
 *  <li>EntityExpr: for simple expressions or expressions that combine EntityConditions
 *  <li>EntityFieldMap: a map of fields where the field (key) equals the value, combined with the operator specified
 * </ul>
 * These can be used in various combinations using the EntityConditionList and EntityExpr objects.
 *
 */
public abstract class EntityCondition extends EntityConditionBase {

    public String toString() {
        return makeWhereString(null, FastList.newInstance(), null);
    }

    public void accept(EntityConditionVisitor visitor) {
        throw new IllegalArgumentException(getClass().getName() + ".accept not implemented");
    }

    abstract public String makeWhereString(ModelEntity modelEntity, List entityConditionParams, DatasourceInfo datasourceInfo);

    abstract public void checkCondition(ModelEntity modelEntity) throws GenericModelException;

    public boolean entityMatches(GenericEntity entity) {
        return mapMatches(entity.getDelegator(), entity);
    }    

    public Object eval(GenericEntity entity) {
        return eval(entity.getDelegator(), entity);
    }

    public Object eval(GenericDelegator delegator, Map map) {
        return mapMatches(delegator, map) ? Boolean.TRUE : Boolean.FALSE;
    }

    abstract public boolean mapMatches(GenericDelegator delegator, Map map);

    abstract public EntityCondition freeze();

    abstract public void encryptConditionFields(ModelEntity modelEntity, GenericDelegator delegator);
    
    public void visit(EntityConditionVisitor visitor) {
        throw new IllegalArgumentException(getClass().getName() + ".visit not implemented");
    }
}
