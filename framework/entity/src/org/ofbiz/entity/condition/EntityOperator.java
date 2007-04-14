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

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntity;
import org.ofbiz.entity.GenericModelException;
import org.ofbiz.entity.config.DatasourceInfo;
import org.ofbiz.entity.model.ModelEntity;
import org.ofbiz.entity.model.ModelField;

/**
 * Encapsulates operations between entities and entity fields. This is a immutable class.
 *
 */
public abstract class EntityOperator extends EntityConditionBase {

    public static final int ID_EQUALS = 1;
    public static final int ID_NOT_EQUAL = 2;
    public static final int ID_LESS_THAN = 3;
    public static final int ID_GREATER_THAN = 4;
    public static final int ID_LESS_THAN_EQUAL_TO = 5;
    public static final int ID_GREATER_THAN_EQUAL_TO = 6;
    public static final int ID_IN = 7;
    public static final int ID_BETWEEN = 8;
    public static final int ID_NOT = 9;
    public static final int ID_AND = 10;
    public static final int ID_OR = 11;
    public static final int ID_LIKE = 12;
    public static final int ID_NOT_IN = 13;
    public static final int ID_NOT_LIKE = 14;

    private static HashMap registry = new HashMap();

    private static void register(String name, EntityOperator operator) {
        registry.put(name, operator);
    }

    public static EntityOperator lookup(String name) {
        return (EntityOperator) registry.get(name);
    }

    public static EntityComparisonOperator lookupComparison(String name) {
        EntityOperator operator = lookup(name);
        if ( !(operator instanceof EntityComparisonOperator ) )
            throw new IllegalArgumentException(name + " is not a comparison operator");
        return (EntityComparisonOperator)operator;
    }

    public static EntityJoinOperator lookupJoin(String name) {
        EntityOperator operator = lookup(name);
        if ( !(operator instanceof EntityJoinOperator ) )
            throw new IllegalArgumentException(name + " is not a join operator");
        return (EntityJoinOperator)operator;
    }

    public static final EntityComparisonOperator EQUALS = new EntityComparisonOperator(ID_EQUALS, "=") {
        public boolean compare(Object lhs, Object rhs) { return EntityComparisonOperator.compareEqual(lhs, rhs); }
        protected void makeRHSWhereString(ModelEntity entity, List entityConditionParams, StringBuffer sb, ModelField field, Object rhs, DatasourceInfo datasourceInfo) {
            if (rhs == null || rhs == GenericEntity.NULL_FIELD) {
                //Debug.logInfo("makeRHSWhereString: field IS NULL: " + field.getName(), module);
                sb.append(" IS NULL");
            } else {
                //Debug.logInfo("makeRHSWhereString: field not null, doing super: " + field.getName() + ", type: " + rhs.getClass().getName() + ", value: " + rhs, module);
                super.makeRHSWhereString(entity, entityConditionParams, sb, field, rhs, datasourceInfo);
            }
        }
    };
    static { register( "equals", EQUALS ); }
    public static final EntityComparisonOperator NOT_EQUAL = new EntityComparisonOperator(ID_NOT_EQUAL, "<>") {
        public boolean compare(Object lhs, Object rhs) { return EntityComparisonOperator.compareNotEqual(lhs, rhs); }
        protected void makeRHSWhereString(ModelEntity entity, List entityConditionParams, StringBuffer sb, ModelField field, Object rhs, DatasourceInfo datasourceInfo) {
            if (rhs == null || rhs == GenericEntity.NULL_FIELD) {
                sb.append(" IS NOT NULL");
            } else {
                super.makeRHSWhereString(entity, entityConditionParams, sb, field, rhs, datasourceInfo);
            }
        }
    };
    static { register( "not-equal", NOT_EQUAL ); }
    static { register( "not-equals", NOT_EQUAL ); }
    static { register( "notEqual", NOT_EQUAL ); }
    public static final EntityComparisonOperator LESS_THAN = new EntityComparisonOperator(ID_LESS_THAN, "<") {
        public boolean compare(Object lhs, Object rhs) { return EntityComparisonOperator.compareLessThan(lhs, rhs); }
    };
    static { register( "less", LESS_THAN ); }
    static { register( "less-than", LESS_THAN ); }
    static { register( "lessThan", LESS_THAN ); }
    public static final EntityComparisonOperator GREATER_THAN = new EntityComparisonOperator(ID_GREATER_THAN, ">") {
        public boolean compare(Object lhs, Object rhs) { return EntityComparisonOperator.compareGreaterThan(lhs, rhs); }
    };
    static { register( "greater", GREATER_THAN ); }
    static { register( "greater-than", GREATER_THAN ); }
    static { register( "greaterThan", GREATER_THAN ); }
    public static final EntityComparisonOperator LESS_THAN_EQUAL_TO = new EntityComparisonOperator(ID_LESS_THAN_EQUAL_TO, "<=") {
        public boolean compare(Object lhs, Object rhs) { return EntityComparisonOperator.compareLessThanEqualTo(lhs, rhs); }
    };
    static { register( "less-equals", LESS_THAN_EQUAL_TO ); }
    static { register( "less-than-equal-to", LESS_THAN_EQUAL_TO ); }
    static { register( "lessThanEqualTo", LESS_THAN_EQUAL_TO ); }
    public static final EntityComparisonOperator GREATER_THAN_EQUAL_TO = new EntityComparisonOperator(ID_GREATER_THAN_EQUAL_TO, ">=") {
        public boolean compare(Object lhs, Object rhs) { return EntityComparisonOperator.compareGreaterThanEqualTo(lhs, rhs); }
    };
    static { register( "greater-equals", GREATER_THAN_EQUAL_TO ); }
    static { register( "greater-than-equal-to", GREATER_THAN_EQUAL_TO ); }
    static { register( "greaterThanEqualTo", GREATER_THAN_EQUAL_TO ); }
    public static final EntityComparisonOperator IN = new EntityComparisonOperator(ID_IN, "IN") {
        public boolean compare(Object lhs, Object rhs) { return EntityComparisonOperator.compareIn(lhs, rhs); }
        protected void makeRHSWhereStringValue(ModelEntity entity, List entityConditionParams, StringBuffer sb, ModelField field, Object rhs) { appendRHSList(entityConditionParams, sb, field, rhs); }
    };
    static { register( "in", IN ); }
    public static final EntityComparisonOperator BETWEEN = new EntityComparisonOperator(ID_BETWEEN, "BETWEEN");
    static { register( "between", BETWEEN ); }
    public static final EntityComparisonOperator NOT = new EntityComparisonOperator(ID_NOT, "NOT");
    static { register( "not", NOT ); }
    public static final EntityJoinOperator AND = new EntityJoinOperator(ID_AND, "AND", false);
    static { register( "and", AND ); }
    public static final EntityJoinOperator OR = new EntityJoinOperator(ID_OR, "OR", true);
    static { register( "or", OR ); }
    public static final EntityComparisonOperator LIKE = new EntityComparisonOperator(ID_LIKE, "LIKE") {
        public boolean compare(Object lhs, Object rhs) { return EntityComparisonOperator.compareLike(lhs, rhs); }
    };
    static { register( "like", LIKE ); }
    public static final EntityComparisonOperator NOT_LIKE = new EntityComparisonOperator(ID_NOT_LIKE, "NOT LIKE") {
        public boolean compare(Object lhs, Object rhs) { return !EntityComparisonOperator.compareLike(lhs, rhs); }
    };
    static { register( "not-like", NOT_LIKE); }
    public static final EntityComparisonOperator NOT_IN = new EntityComparisonOperator(ID_NOT_IN, "NOT IN") {
        public boolean compare(Object lhs, Object rhs) { return !EntityComparisonOperator.compareIn(lhs, rhs); }
        protected void makeRHSWhereStringValue(ModelEntity entity, List entityConditionParams, StringBuffer sb, ModelField field, Object rhs) { appendRHSList(entityConditionParams, sb, field, rhs); }
    };
    static { register( "not-in", NOT_IN ); }

    protected int idInt;
    protected String codeString;

    public EntityOperator(int id, String code) {
        idInt = id;
        codeString = code;
    }

    public String getCode() {
        if (codeString == null) {
            return "null";
        } else {
            return codeString;
        }
    }

    public int getId() {
        return idInt;
    }

    public String toString() {
        return codeString;
    }

    public int hashCode() {
        return this.codeString.hashCode();
    }

    public boolean equals(Object obj) {
        EntityOperator otherOper = (EntityOperator) obj;
        return this.idInt == otherOper.idInt;
    }

    public boolean entityMatches(GenericEntity entity, Object lhs, Object rhs) {
        return mapMatches(entity.getDelegator(), entity, lhs, rhs);
    }

    protected void appendRHSList(List entityConditionParams, StringBuffer whereStringBuffer, ModelField field, Object rhs) {
        whereStringBuffer.append('(');

        if (rhs instanceof Collection) {
            Iterator rhsIter = ((Collection) rhs).iterator();

            while (rhsIter.hasNext()) {
                Object inObj = rhsIter.next();

                addValue(whereStringBuffer, field, inObj, entityConditionParams);
                if (rhsIter.hasNext()) {
                    whereStringBuffer.append(", ");
                }
            }
        } else {
            addValue(whereStringBuffer, field, rhs, entityConditionParams);
        }
        whereStringBuffer.append(')');
    }

    public Object eval(GenericDelegator delegator, Map map, Object lhs, Object rhs) {
        return castBoolean(mapMatches(delegator, map, lhs, rhs));
    }

    public abstract boolean mapMatches(GenericDelegator delegator, Map map, Object lhs, Object rhs);
    public abstract void validateSql(ModelEntity entity, Object lhs, Object rhs) throws GenericModelException;
    public void addSqlValue(StringBuffer sql, ModelEntity entity, List entityConditionParams, Object lhs, Object rhs, DatasourceInfo datasourceInfo) {
        addSqlValue(sql, entity, entityConditionParams, true, lhs, rhs, datasourceInfo);
    }

    public abstract void addSqlValue(StringBuffer sql, ModelEntity entity, List entityConditionParams, boolean compat, Object rhs, Object lhs, DatasourceInfo datasourceInfo);
    public abstract EntityCondition freeze(Object lhs, Object rhs);
    public abstract void visit(EntityConditionVisitor visitor, Object lhs, Object rhs);

    public static final Object WILDCARD = new Object() {
        public String toString() {
            return "(WILDCARD)";
        }
    };
}
