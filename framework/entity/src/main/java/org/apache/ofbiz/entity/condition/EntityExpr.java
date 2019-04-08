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

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.ObjectType;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.DelegatorFactory;
import org.apache.ofbiz.entity.GenericEntity;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericModelException;
import org.apache.ofbiz.entity.config.model.Datasource;
import org.apache.ofbiz.entity.model.ModelEntity;
import org.apache.ofbiz.entity.model.ModelField;
import org.apache.ofbiz.entity.model.ModelFieldType;

/**
 * Encapsulates simple expressions used for specifying queries
 *
 */
@SuppressWarnings("serial")
public final class EntityExpr extends EntityCondition {
    public static final String module = EntityExpr.class.getName();

    private final Object lhs;
    private final EntityOperator<Object, Object, ?> operator;
    private final Object rhs;

    public <L,R,LL,RR> EntityExpr(L lhs, EntityComparisonOperator<LL,RR> operator, R rhs) {
        if (lhs == null) {
            throw new IllegalArgumentException("The field name/value cannot be null");
        }
        if (operator == null) {
            throw new IllegalArgumentException("The operator argument cannot be null");
        }

        if (rhs == null || rhs == GenericEntity.NULL_FIELD) {
            if (!EntityOperator.NOT_EQUAL.equals(operator) && !EntityOperator.EQUALS.equals(operator)) {
                throw new IllegalArgumentException("Operator must be EQUALS or NOT_EQUAL when right/rhs argument is NULL ");
            }
        }

        if (EntityOperator.BETWEEN.equals(operator)) {
            if (!(rhs instanceof Collection<?>) || (((Collection<?>) rhs).size() != 2)) {
                throw new IllegalArgumentException("BETWEEN Operator requires a Collection with 2 elements for the right/rhs argument");
            }
        }

        if (lhs instanceof String) {
            this.lhs = EntityFieldValue.makeFieldValue((String) lhs);
        } else {
            this.lhs = lhs;
        }
        this.operator = UtilGenerics.cast(operator);
        this.rhs = rhs;

        //Debug.logInfo("new EntityExpr internal field=" + lhs + ", value=" + rhs + ", value type=" + (rhs == null ? "null object" : rhs.getClass().getName()), module);
    }

    public EntityExpr(EntityCondition lhs, EntityJoinOperator operator, EntityCondition rhs) {
        if (lhs == null) {
            throw new IllegalArgumentException("The left EntityCondition argument cannot be null");
        }
        if (rhs == null) {
            throw new IllegalArgumentException("The right EntityCondition argument cannot be null");
        }
        if (operator == null) {
            throw new IllegalArgumentException("The operator argument cannot be null");
        }

        this.lhs = lhs;
        this.operator = UtilGenerics.cast(operator);
        this.rhs = rhs;
    }

    public Object getLhs() {
        return lhs;
    }

    public <L,R,T> EntityOperator<L,R,T> getOperator() {
        return UtilGenerics.cast(operator);
    }

    public Object getRhs() {
        return rhs;
    }

    @Override
    public boolean isEmpty() {
        return operator.isEmpty(lhs, rhs);
    }

    @Override
    public String makeWhereString(ModelEntity modelEntity, List<EntityConditionParam> entityConditionParams, Datasource datasourceInfo) {
        // if (Debug.verboseOn()) Debug.logVerbose("makeWhereString for entity " + modelEntity.getEntityName(), module);

        this.checkRhsType(modelEntity, null);

        StringBuilder sql = new StringBuilder();
        operator.addSqlValue(sql, modelEntity, entityConditionParams, true, lhs, rhs, datasourceInfo);
        return sql.toString();
    }

    @Override
    public boolean mapMatches(Delegator delegator, Map<String, ? extends Object> map) {
        return operator.mapMatches(delegator, map, lhs, rhs);
    }

    @Override
    public void checkCondition(ModelEntity modelEntity) throws GenericModelException {
        // if (Debug.verboseOn()) Debug.logVerbose("checkCondition for entity " + modelEntity.getEntityName(), module);
        if (lhs instanceof EntityCondition) {
            ((EntityCondition) lhs).checkCondition(modelEntity);
            ((EntityCondition) rhs).checkCondition(modelEntity);
        }
    }

    @Override
    protected void addValue(StringBuilder buffer, ModelField field, Object value, List<EntityConditionParam> params) {
        if (rhs instanceof EntityFunction.UPPER) {
            if (value instanceof String) {
                value = ((String) value).toUpperCase();
            }
        }
        super.addValue(buffer, field, value, params);
    }

    @Override
    public EntityCondition freeze() {
        return operator.freeze(lhs, rhs);
    }

    @Override
    public void visit(EntityConditionVisitor visitor) {
        visitor.acceptEntityOperator(operator, lhs, rhs);
    }

    @Override
    public void accept(EntityConditionVisitor visitor) {
        visitor.acceptEntityExpr(this);
    }

    public void checkRhsType(ModelEntity modelEntity, Delegator delegator) {
        if (this.rhs == null || this.rhs == GenericEntity.NULL_FIELD || modelEntity == null) return;

        Object value = this.rhs;
        if (this.rhs instanceof EntityFunction<?>) {
            value = UtilGenerics.<EntityFunction<?>>cast(this.rhs).getOriginalValue();
        }

        if (value instanceof Collection<?>) {
            Collection<?> valueCol = UtilGenerics.cast(value);
            if (valueCol.size() > 0) {
                value = valueCol.iterator().next();
            } else {
                value = null;
            }
        }

        if (delegator == null) {
            // this will be the common case for now as the delegator isn't available where we want to do this
            // we'll cheat a little here and assume the default delegator
            delegator = DelegatorFactory.getDelegator("default");
        }

        String fieldName = null;
        ModelField curField;
        if (this.lhs instanceof EntityFieldValue) {
            EntityFieldValue efv = (EntityFieldValue) this.lhs;
            fieldName = efv.getFieldName();
            curField = efv.getModelField(modelEntity);
        } else {
            // nothing to check
            return;
        }

        if (curField == null) {
            throw new IllegalArgumentException("FieldName " + fieldName + " not found for entity: " + modelEntity.getEntityName());
        }
        ModelFieldType type = null;
        try {
            type = delegator.getEntityFieldType(modelEntity, curField.getType());
        } catch (GenericEntityException e) {
            Debug.logWarning(e, module);
        }
        if (type == null) {
            throw new IllegalArgumentException("Type " + curField.getType() + " not found for entity [" + modelEntity.getEntityName() + "]; probably because there is no datasource (helper) setup for the entity group that this entity is in: [" + delegator.getEntityGroupName(modelEntity.getEntityName()) + "]");
        }
        if (value instanceof EntityConditionSubSelect){
            ModelFieldType valueType = null;
            try {
                ModelEntity valueModelEntity= ((EntityConditionSubSelect) value).getModelEntity();
                valueType = delegator.getEntityFieldType(valueModelEntity,  valueModelEntity.getField(((EntityConditionSubSelect) value).getKeyFieldName()).getType());
            } catch (GenericEntityException e) {
                Debug.logWarning(e, module);
            }
          // make sure the type of keyFieldName of EntityConditionSubSelect  matches the field Java type
            try {
                if (!ObjectType.instanceOf(ObjectType.loadClass(valueType.getJavaType()), type.getJavaType())) {
                    String errMsg = "Warning using ["+ value.getClass().getName() + "] and entity field [" + modelEntity.getEntityName() + "." + curField.getName() + "]. The Java type of keyFieldName : [" + valueType.getJavaType()+ "] is not compatible with the Java type of the field [" + type.getJavaType() + "]";
                    // eventually we should do this, but for now we'll do a "soft" failure: throw new IllegalArgumentException(errMsg);
                    Debug.logWarning(new Exception("Location of database type warning"), "=-=-=-=-=-=-=-=-= Database type warning in EntityExpr =-=-=-=-=-=-=-=-= " + errMsg, module);
                }
            } catch (ClassNotFoundException e) {
                String errMsg = "Warning using ["+ value.getClass().getName() + "] and entity field [" + modelEntity.getEntityName() + "." + curField.getName() + "]. The Java type of keyFieldName : [" + valueType.getJavaType()+ "] could not be found]";
                // eventually we should do this, but for now we'll do a "soft" failure: throw new IllegalArgumentException(errMsg);
                Debug.logWarning(e, "=-=-=-=-=-=-=-=-= Database type warning in EntityExpr =-=-=-=-=-=-=-=-= " + errMsg, module);
             }
        } else if (value instanceof EntityFieldValue) {
            EntityFieldValue efv = (EntityFieldValue) this.lhs;
            String rhsFieldName = efv.getFieldName();
            ModelField rhsField = efv.getModelField(modelEntity);
            if (rhsField == null) {
                throw new IllegalArgumentException("FieldName " + rhsFieldName + " not found for entity: " + modelEntity.getEntityName());
            }
            ModelFieldType rhsType = null;
            try {
                rhsType = delegator.getEntityFieldType(modelEntity, rhsField.getType());
            } catch (GenericEntityException e) {
                Debug.logWarning(e, module);
            }
            try {
                if (!ObjectType.instanceOf(ObjectType.loadClass(rhsType.getJavaType()), type.getJavaType())) {
                    String errMsg = "Warning using ["+ value.getClass().getName() + "] and entity field [" + modelEntity.getEntityName() + "." + curField.getName() + "]. The Java type [" + rhsType.getJavaType() + "] of rhsFieldName : [" + rhsFieldName + "] is not compatible with the Java type of the field [" + type.getJavaType() + "]";
                    // eventually we should do this, but for now we'll do a "soft" failure: throw new IllegalArgumentException(errMsg);
                    Debug.logWarning(new Exception("Location of database type warning"), "=-=-=-=-=-=-=-=-= Database type warning in EntityExpr =-=-=-=-=-=-=-=- " + errMsg, module);
                }
            } catch (ClassNotFoundException e) {
                String errMsg = "Warning using ["+ value.getClass().getName() + "] and entity field [" + modelEntity.getEntityName() + "." + curField.getName() + "]. The Java type [" + rhsType.getJavaType() + "] of rhsFieldName : [" + rhsFieldName + "] could not be found]";
                // eventually we should do this, but for now we'll do a "soft" failure: throw new IllegalArgumentException(errMsg);
                Debug.logWarning(e, "=-=-=-=-=-=-=-=-= Database type warning in EntityExpr =-=-=-=-=-=-=-=-= " + errMsg, module);
            }
        } else {
        // make sure the type matches the field Java type
            if (!ObjectType.instanceOf(value, type.getJavaType())) {
                String errMsg = "In entity field [" + modelEntity.getEntityName() + "." + curField.getName() + "] set the value passed in [" + value.getClass().getName() + "] is not compatible with the Java type of the field [" + type.getJavaType() + "]";
                // eventually we should do this, but for now we'll do a "soft" failure: throw new IllegalArgumentException(errMsg);
                Debug.logWarning(new Exception("Location of database type warning"), "=-=-=-=-=-=-=-=-= Database type warning in EntityExpr =-=-=-=-=-=-=-=-= " + errMsg, module);
            }
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof EntityExpr)) return false;
        EntityExpr other = (EntityExpr) obj;
        boolean isEqual = equals(lhs, other.lhs) &&
               equals(operator, other.operator) &&
               equals(rhs, other.rhs);
        //if (!isEqual) {
        //    Debug.logWarning("EntityExpr.equals is false for: \n-this.lhs=" + this.lhs + "; other.lhs=" + other.lhs +
        //            "\nthis.operator=" + this.operator + "; other.operator=" + other.operator +
        //            "\nthis.rhs=" + this.rhs + "other.rhs=" + other.rhs, module);
        //}
        return isEqual;
    }

    @Override
    public int hashCode() {
        return hashCode(lhs) +
               hashCode(operator) +
               hashCode(rhs);
    }
}
