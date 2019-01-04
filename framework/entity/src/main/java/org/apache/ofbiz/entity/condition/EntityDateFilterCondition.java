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

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericModelException;
import org.apache.ofbiz.entity.config.model.Datasource;
import org.apache.ofbiz.entity.model.ModelEntity;

/**
 * Represents Date-range condition expression.
 * <p>
 * This is used to filter rows that are valid in a particular range.
 */
@SuppressWarnings("serial")
public final class EntityDateFilterCondition implements EntityCondition {
    /** The column containing dates before which a row is considered invalid.  */
    private final String fromDateName;
    /** The column containing dates after which a row is considered invalid.  */
    private final String thruDateName;

    /**
     * Constructs a condition expression to filter rows that are currently valid.
     *
     * This means that we remove rows whose from/thru date range does not match the current date.
     * The <i>current date</i> is the one computed when the SQL query is generated.
     *
     * @param fromDateName the name of the field corresponding to the from date
     * @param thruDateName the name of the field corresponding to the thru date
     */
    public EntityDateFilterCondition(String fromDateName, String thruDateName) {
        this.fromDateName = fromDateName;
        this.thruDateName = thruDateName;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public String makeWhereString(ModelEntity modelEntity, List<EntityConditionParam> entityConditionParams, Datasource datasourceInfo) {
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
        if (!(obj instanceof EntityDateFilterCondition)) {
            return false;
        }
        EntityDateFilterCondition other = (EntityDateFilterCondition) obj;
        return Objects.equals(fromDateName, other.fromDateName) && Objects.equals(thruDateName, other.thruDateName);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(fromDateName) ^ Objects.hashCode(thruDateName);
    }

    @Override
    public void accept(EntityConditionVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public EntityCondition freeze() {
        return this;
    }

    @Override
    public String toString() {
        return makeWhereString();
    }

    /**
     * Constructs a condition expression to filter rows that are valid now.
     *
     * @return a condition expression filtering rows that are currently valid
     */
    private EntityCondition makeCondition() {
        return makeCondition(UtilDateTime.nowTimestamp(), fromDateName, thruDateName);
    }

    /**
     * Constructs a condition expression to filter rows that are valid at a given time stamp.
     *
     * This means that we remove rows whose from/thru date range does not match the time stamp.
     *
     * @param moment the time stamp used to check validity
     * @param fromDateName the name of the field corresponding to the from date
     * @param thruDateName the name of the field corresponding to the thru date
     * @return a condition expression filtering rows that are currently valid
     */
    public static EntityCondition makeCondition(Timestamp moment, String fromDateName, String thruDateName) {
        return EntityCondition.makeCondition(
                EntityCondition.makeCondition(
                        EntityCondition.makeCondition(thruDateName, null),
                        EntityOperator.OR,
                        EntityCondition.makeCondition(thruDateName, EntityOperator.GREATER_THAN, moment)),
                EntityCondition.makeCondition(
                        EntityCondition.makeCondition(fromDateName, null),
                        EntityOperator.OR,
                        EntityCondition.makeCondition(fromDateName, EntityOperator.LESS_THAN_EQUAL_TO, moment)));
    }

    /**
     * Creates an EntityCondition representing a date range filter query to be used against
     * entities that themselves represent a date range.  When used the resulting entities
     * will meet at least one of the following criteria:
     * - fromDate is equal to or after rangeStart but before rangeEnd
     * - thruDate is equal to or after rangeStart but before rangeEnd
     * - fromDate is null and thruDate is equal to or after rangeStart
     * - thruDate is null and fromDate is before rangeEnd
     * - fromDate is null and thruDate is null
     *
     * @param rangeStart    The start of the range to filter against
     * @param rangeEnd      The end of the range to filter against
     * @param fromDateName  The name of the field containing the entity's "fromDate"
     * @param thruDateName  The name of the field containing the entity's "thruDate"
     * @return EntityCondition representing the date range filter
     */
    public static EntityCondition makeRangeCondition(Timestamp rangeStart, Timestamp rangeEnd, String fromDateName,
            String thruDateName) {
        return EntityCondition.makeCondition(EntityOperator.OR,
                EntityCondition.makeConditionMap(thruDateName, null, fromDateName, null),
                EntityCondition.makeCondition(
                        EntityCondition.makeCondition(fromDateName, EntityOperator.GREATER_THAN_EQUAL_TO, rangeStart),
                        EntityCondition.makeCondition(fromDateName, EntityOperator.LESS_THAN, rangeEnd)),
                EntityCondition.makeCondition(
                        EntityCondition.makeCondition(thruDateName, EntityOperator.GREATER_THAN_EQUAL_TO, rangeStart),
                        EntityCondition.makeCondition(thruDateName, EntityOperator.LESS_THAN, rangeEnd)),
                EntityCondition.makeCondition(
                        EntityCondition.makeCondition(fromDateName, null),
                        EntityCondition.makeCondition(thruDateName, EntityOperator.GREATER_THAN_EQUAL_TO, rangeStart)),
                EntityCondition.makeCondition(
                        EntityCondition.makeCondition(thruDateName, null),
                        EntityCondition.makeCondition(fromDateName, EntityOperator.LESS_THAN, rangeEnd)));
    }
}
