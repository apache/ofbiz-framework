/*
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
 */
package org.ofbiz.entity.sql;

import java.util.List;

import javolution.util.FastList;

import org.ofbiz.entity.condition.EntityFieldValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityConditionValue;
import org.ofbiz.entity.condition.EntityOperator;

import org.ofbiz.sql.BooleanCondition;
import org.ofbiz.sql.Condition;
import org.ofbiz.sql.ConditionList;
import org.ofbiz.sql.FieldValue;
import org.ofbiz.sql.Joiner;
import org.ofbiz.sql.NumberValue;
import org.ofbiz.sql.StringValue;
import org.ofbiz.sql.Value;

public final class EntityConditionPlanner {
    public EntityCondition buildCondition(Condition condition) {
        if (condition == null) return null;
        if (condition instanceof BooleanCondition) {
            BooleanCondition bc = (BooleanCondition) condition;
            return EntityCondition.makeCondition(buildFieldValue(bc.getLeft()), EntityOperator.lookupComparison(bc.getOp()), buildValue(bc.getRight()));
        } else if (condition instanceof ConditionList) {
            ConditionList cl = (ConditionList) condition;
            List<EntityCondition> conditions = FastList.newInstance();
            for (Condition subCondition: cl) {
                conditions.add(buildCondition(subCondition));
            }
            return EntityCondition.makeCondition(conditions, cl.getJoiner() == Joiner.AND ? EntityOperator.AND : EntityOperator.OR);
        } else {
            throw new UnsupportedOperationException(condition.toString());
        }
    }

    public EntityFieldValue buildFieldValue(Value value) {
        if (value instanceof FieldValue) {
            FieldValue fv = (FieldValue) value;
            return EntityFieldValue.makeFieldValue(fv.getFieldName(), fv.getTableName(), null, null);
        }
        throw new UnsupportedOperationException(value.toString());
    }

    public Object buildValue(Object value) {
        if (value instanceof NumberValue) {
            return ((NumberValue) value).getNumber();
        } else if (value instanceof StringValue) {
            return ((StringValue) value).getString();
        } else if (value instanceof List) {
            List<Object> values = FastList.newInstance();
            for (Object sqlValue: (List) value) {
                values.add(buildValue(sqlValue));
            }
            return values;
        }
        throw new UnsupportedOperationException(value.toString());
    }
}
