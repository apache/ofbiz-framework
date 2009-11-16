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
import java.util.ListIterator;
import java.util.Map;

import javolution.util.FastList;
import javolution.util.FastMap;

import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.model.DynamicViewEntity;

import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.model.DynamicViewEntity;
import org.ofbiz.entity.model.ModelKeyMap;
import org.ofbiz.entity.util.EntityListIterator;

import org.ofbiz.sql.SelectPlan;

public final class EntitySelectPlan extends SelectPlan<EntitySelectPlan> {
    private final DynamicViewEntity dve;
    private final EntityCondition whereCondition;
    private final EntityCondition havingCondition;
    private final List<String> orderBy;
    private final int offset = -1;
    private final int limit = -1;

    public EntitySelectPlan(DynamicViewEntity dve, EntityCondition whereCondition, EntityCondition havingCondition, List<String> orderBy) {
        this.dve = dve;
        this.whereCondition = whereCondition;
        this.havingCondition = havingCondition;
        this.orderBy = orderBy;
        //this.offset = offset;
        //this.limit = limit;
    }

    public EntityListIterator getEntityListIterator(Delegator delegator) throws GenericEntityException {
        return delegator.findListIteratorByCondition(dve, whereCondition, havingCondition, null, orderBy, null);
    }

    public DynamicViewEntity getDynamicViewEntity() {
        return dve;
    }

    public EntityCondition getWhereCondition() {
        return whereCondition;
    }

    public EntityCondition getHavingCondition() {
        return havingCondition;
    }

    public List<String> getOrderBy() {
        return orderBy;
    }

    public int getOffset() {
        return offset;
    }

    public int getLimit() {
        return limit;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("dve=" + dve);
        if (whereCondition != null) {
            if (sb.length() > 0) sb.append(", ");
            sb.append("where=(").append(whereCondition).append(")");
        }
        if (havingCondition != null) {
            if (sb.length() > 0) sb.append(", ");
            sb.append("having=(").append(havingCondition).append(")");
        }
        if (offset != -1) {
            if (sb.length() > 0) sb.append(", ");
            sb.append("offset=").append(offset);
        }
        if (limit != -1) {
            if (sb.length() > 0) sb.append(", ");
            sb.append("limit=").append(limit);
        }
        sb.append("]");
        sb.insert(0, "[").insert(0, super.toString());
        return sb.toString();
    }
}
