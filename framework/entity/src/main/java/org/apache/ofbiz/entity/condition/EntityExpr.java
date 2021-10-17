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
import java.util.Objects;

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
 * Represents an infix condition expression.
 */
@SuppressWarnings("serial")
public final class EntityExpr implements EntityCondition {
    private static final String MODULE = EntityExpr.class.getName();
    /** The left hand side of the expression.  */
    private final Object lhs;
    /** The operator used to combine the two sides of the expression.  */
    private final EntityOperator<Object, Object> operator;
    /** The right hand side of the expression.  */
    private final Object rhs;

    /**
     * Constructs an infix comparison expression.
     * @param lhs the left hand side of the expression
     * @param operator the comparison operator used to compare the two sides of the expression
     * @param rhs the right hand side of the expression
     * @throws IllegalArgumentException if {@code lhs} or {@code operator} are {@code null},
     *         or if {@code rhs} is null when the operator is not an equality check.
     */
    public <L, R, LL, RR> EntityExpr(L lhs, EntityComparisonOperator<LL, RR> operator, R rhs) {
        if (lhs == null) {
            throw new IllegalArgumentException("The field name/value cannot be null");
        }
        if (operator == null) {
            throw new IllegalArgumentException("The operator argument cannot be null");
        }

        if (EntityExpr.isNullField(rhs)
                && !(EntityOperator.NOT_EQUAL.equals(operator) || EntityOperator.EQUALS.equals(operator))) {
            throw new IllegalArgumentException("Operator must be EQUALS or NOT_EQUAL when right/rhs argument is NULL ");
        }

        if (EntityOperator.BETWEEN.equals(operator)
                && (!(rhs instanceof Collection<?>) || (((Collection<?>) rhs).size() != 2))) {
            String msg = "BETWEEN Operator requires a Collection with 2 elements for the right/rhs argument";
            throw new IllegalArgumentException(msg);
        }

        this.lhs = (lhs instanceof String) ? EntityFieldValue.makeFieldValue((String) lhs) : lhs;
        this.operator = UtilGenerics.cast(operator);
        this.rhs = rhs;
    }

    /**
     * Constructs an simple combination of expression.
     * @param lhs the expression of the left hand side
     * @param operator the operator used to join the {@code lhs} and {@code rhs} expressions
     * @param rhs the expression of the right hand side
     * @throws IllegalArgumentException if any parameter is {@code null}.
     */
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

    /**
     * Gets the left hand side of the condition expression.
     * @return the left hand side of the condition expression
     */
    public Object getLhs() {
        return lhs;
    }

    /**
     * Gets the operator used to combine the two sides of the condition expression.
     * @return the operator used to combine the two sides of the condition expression.
     */
    public <L, R> EntityOperator<L, R> getOperator() {
        return UtilGenerics.cast(operator);
    }

    /**
     * Gets the right hand side of the condition expression.
     * @return the right hand side of the condition expression
     */
    public Object getRhs() {
        return rhs;
    }

    @Override
    public boolean isEmpty() {
        return operator.isEmpty(lhs, rhs);
    }

    @Override
    public String makeWhereString(ModelEntity modelEntity, List<EntityConditionParam> entityConditionParams,
            Datasource datasourceInfo) {
        checkRhsType(modelEntity, null);
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
        if (lhs instanceof EntityCondition) {
            // CHECKSTYLE_OFF: ALMOST_ALL
            ((EntityCondition) lhs).checkCondition(modelEntity);
            ((EntityCondition) rhs).checkCondition(modelEntity);
            // CHECKSTYLE_ON: ALMOST_ALL
        }
    }

    @Override
    public EntityCondition freeze() {
        return operator.freeze(lhs, rhs);
    }

    @Override
    public void accept(EntityConditionVisitor visitor) {
        visitor.visit(this);
    }

    // TODO: Expand the documentation to explain what is exactly checked.
    /**
     * Ensures that the right hand side of the condition expression is valid.
     * @param modelEntity the entity model used to check the condition expression
     * @param delegator the delegator used to check the condition expression
     */
    public void checkRhsType(ModelEntity modelEntity, Delegator delegator) {
        if (EntityExpr.isNullField(rhs) || modelEntity == null) {
            return;
        }

        Object value;
        if (this.rhs instanceof EntityFunction<?>) {
            value = UtilGenerics.<EntityFunction<?>>cast(rhs).getOriginalValue();
        } else {
            value = rhs;
        }

        if (value instanceof Collection<?>) {
            Collection<?> valueCol = UtilGenerics.cast(value);
            if (!valueCol.isEmpty()) {
                value = valueCol.iterator().next();
            } else {
                value = null;
            }
        }

        // This will be the common case for now as the delegator isn't available where we want to do this
        // we'll cheat a little here and assume the default delegator.
        Delegator deleg = (delegator == null) ? DelegatorFactory.getDelegator("default") : delegator;

        String fieldName = null;
        ModelField curField;
        if (lhs instanceof EntityFieldValue) {
            EntityFieldValue efv = (EntityFieldValue) lhs;
            fieldName = efv.getFieldName();
            curField = efv.getModelField(modelEntity);
        } else {
            // nothing to check
            return;
        }

        if (curField == null) {
            String msg = "FieldName " + fieldName + " not found for entity: " + modelEntity.getEntityName();
            throw new IllegalArgumentException(msg);
        }
        ModelFieldType type = null;
        try {
            type = deleg.getEntityFieldType(modelEntity, curField.getType());
        } catch (GenericEntityException e) {
            Debug.logWarning(e, MODULE);
        }
        if (type == null) {
            String ftype = curField.getType();
            String entityName = modelEntity.getEntityName();
            throw new IllegalArgumentException("Type " + ftype + " not found for entity [" + entityName + "];"
                    + " probably because there is no datasource (helper) setup for the entity group"
                    + " that this entity is in: [" + deleg.getEntityGroupName(entityName) + "]");
        }
        if (value instanceof EntityConditionSubSelect) {
            ModelFieldType valueType = null;
            try {
                ModelEntity valueModelEntity = ((EntityConditionSubSelect) value).getModelEntity();
                valueType = deleg.getEntityFieldType(valueModelEntity,
                        valueModelEntity.getField(((EntityConditionSubSelect) value).getKeyFieldName()).getType());
            } catch (GenericEntityException e) {
                Debug.logWarning(e, MODULE);
            }
            if (valueType == null) {
                String ftype = curField.getType();
                String entityName = modelEntity.getEntityName();
                throw new IllegalArgumentException("Type " + ftype + " not found for entity [" + entityName + "];"
                        + " probably because there is no datasource (helper) setup for the entity group"
                        + " that this entity is in: [" + deleg.getEntityGroupName(entityName) + "]");

            }
            // Make sure the type of keyFieldName of EntityConditionSubSelect  matches the field Java type.
            try {
                if (!ObjectType.instanceOf(ObjectType.loadClass(valueType.getJavaType()), type.getJavaType())) {
                    String msg = "Warning using [" + value.getClass().getName() + "]"
                            + " and entity field [" + modelEntity.getEntityName() + "." + curField.getName() + "]."
                            + " The Java type of keyFieldName : [" + valueType.getJavaType() + "]"
                            + " is not compatible with the Java type of the field [" + type.getJavaType() + "]";
                    // Eventually we should do this, but for now we'll do a "soft" failure:
                    // throw new IllegalArgumentException(msg);
                    Debug.logWarning(new Exception("Location of database type warning"),
                            "=-=-=-=-=-=-=-=-= Database type warning in EntityExpr =-=-=-=-=-=-=-=-= " + msg, MODULE);
                }
            } catch (ClassNotFoundException e) {
                String msg = "Warning using [" + value.getClass().getName() + "]"
                        + " and entity field [" + modelEntity.getEntityName() + "." + curField.getName() + "]."
                        + " The Java type of keyFieldName : [" + valueType.getJavaType() + "] could not be found]";
                // Eventually we should do this, but for now we'll do a "soft" failure:
                // throw new IllegalArgumentException(msg);
                Debug.logWarning(e, "=-=-=-=-=-=-=-=-= Database type warning in EntityExpr =-=-=-=-=-=-=-=-= " + msg,
                        MODULE);
            }
        } else if (value instanceof EntityFieldValue) {
            EntityFieldValue efv = (EntityFieldValue) lhs;
            String rhsFieldName = efv.getFieldName();
            ModelField rhsField = efv.getModelField(modelEntity);
            if (rhsField == null) {
                String msg = "FieldName " + rhsFieldName + " not found for entity: " + modelEntity.getEntityName();
                throw new IllegalArgumentException(msg);
            }
            ModelFieldType rhsType = null;
            try {
                rhsType = deleg.getEntityFieldType(modelEntity, rhsField.getType());
            } catch (GenericEntityException e) {
                Debug.logWarning(e, MODULE);
            }
            try {
                if (!ObjectType.instanceOf(ObjectType.loadClass(rhsType.getJavaType()), type.getJavaType())) {
                    String msg = "Warning using [" + value.getClass().getName() + "]"
                            + " and entity field [" + modelEntity.getEntityName() + "." + curField.getName() + "]."
                            + " The Java type [" + rhsType.getJavaType() + "] of rhsFieldName : [" + rhsFieldName + "]"
                            + " is not compatible with the Java type of the field [" + type.getJavaType() + "]";
                    // Eventually we should do this, but for now we'll do a "soft" failure:
                    // throw new IllegalArgumentException(msg);
                    Debug.logWarning(new Exception("Location of database type warning"),
                            "=-=-=-=-=-=-=-=-= Database type warning in EntityExpr =-=-=-=-=-=-=-=- " + msg, MODULE);
                }
            } catch (ClassNotFoundException e) {
                String msg = "Warning using [" + value.getClass().getName() + "]"
                        + " and entity field [" + modelEntity.getEntityName() + "." + curField.getName() + "]."
                        + " The Java type [" + rhsType.getJavaType() + "]"
                        + " of rhsFieldName : [" + rhsFieldName + "] could not be found]";
                // Eventually we should do this, but for now we'll do a "soft" failure:
                // throw new IllegalArgumentException(msg);
                Debug.logWarning(e, "=-=-=-=-=-=-=-=-= Database type warning in EntityExpr =-=-=-=-=-=-=-=-= " + msg,
                        MODULE);
            }
        } else {
            // Make sure the type matches the field Java type.
            if (!ObjectType.instanceOf(value, type.getJavaType())) {
                String msg = "In entity field [" + modelEntity.getEntityName() + "." + curField.getName() + "]"
                        + " set the value passed in [" + value.getClass().getName() + "]"
                        + " is not compatible with the Java type of the field [" + type.getJavaType() + "]";
                // Eventually we should do this, but for now we'll do a "soft" failure:
                // throw new IllegalArgumentException(msg);
                Debug.logWarning(new Exception("Location of database type warning"),
                        "=-=-=-=-=-=-=-=-= Database type warning in EntityExpr =-=-=-=-=-=-=-=-= " + msg, MODULE);
            }
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof EntityExpr)) {
            return false;
        }
        EntityExpr ee = (EntityExpr) obj;
        return Objects.equals(lhs, ee.lhs) && Objects.equals(operator, ee.operator) && Objects.equals(rhs, ee.rhs);

    }

    @Override
    public int hashCode() {
        return Objects.hashCode(lhs) + Objects.hashCode(operator) + Objects.hashCode(rhs);
    }

    @Override
    public String toString() {
        return makeWhereString();
    }

    private static boolean isNullField(Object o) {
        return o == null || o == GenericEntity.NULL_FIELD;
    }
}
