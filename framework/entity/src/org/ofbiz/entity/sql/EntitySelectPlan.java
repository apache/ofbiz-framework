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
import java.util.Map;
import java.util.concurrent.Callable;

import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.model.DynamicViewEntity;
import org.ofbiz.entity.transaction.TransactionUtil;
import org.ofbiz.entity.util.EntityListIterator;
import org.ofbiz.sql.ConditionPlan;
import org.ofbiz.sql.ParameterizedConditionException;
import org.ofbiz.sql.SelectPlan;

public final class EntitySelectPlan extends SelectPlan<EntitySelectPlan, EntityCondition> {
    private final DynamicViewEntity dve;
    private final List<String> orderBy;
    private final int offset = -1;
    private final int limit = -1;

    public EntitySelectPlan(DynamicViewEntity dve, ConditionPlan<EntityCondition> wherePlan, ConditionPlan<EntityCondition> havingPlan, List<String> orderBy) {
        super(wherePlan, havingPlan);
        this.dve = dve;
        this.orderBy = orderBy;
        //this.offset = offset;
        //this.limit = limit;
    }

    public EntityListIterator getEntityListIterator(Delegator delegator, Map<String, ? extends Object> params) throws GenericEntityException {
        EntityCondition whereCondition;
        EntityCondition havingCondition;
        try {
            whereCondition = getWherePlan().getCondition(params);
            havingCondition = getHavingPlan().getCondition(params);
        } catch (ParameterizedConditionException e) {
            throw (GenericEntityException) new GenericEntityException(e.getMessage()).initCause(e);
        }
        return delegator.findListIteratorByCondition(dve, whereCondition, havingCondition, null, orderBy, null);
    }

    public List<GenericValue> getAll(final Delegator delegator, final Map<String, ? extends Object> params) throws GenericEntityException {
        return TransactionUtil.doTransaction(new Callable<List<GenericValue>>() {
            public List<GenericValue> call() throws Exception {
                EntityListIterator it = null;
                try {
                    it = getEntityListIterator(delegator, params);
                    return it.getCompleteList();
                } finally {
                    if (it != null) it.close();
                }
            }
        }, "sql select", 0, true);
    }

    public DynamicViewEntity getDynamicViewEntity() {
        return dve;
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

    @Override
    public StringBuilder appendTo(StringBuilder sb) {
        sb.append("dve=").append(dve);
        if (getWherePlan() != null) {
            if (sb.length() > 0) sb.append(", ");
            sb.append("where=(");
            getWherePlan().appendTo(sb);
            sb.append(")");
        }
        if (getHavingPlan() != null) {
            if (sb.length() > 0) sb.append(", ");
            sb.append("having=(");
            getHavingPlan().appendTo(sb);
            sb.append(")");
        }
        /*TODO death code can be removed ?
        if (offset != -1) {
            if (sb.length() > 0) sb.append(", ");
            sb.append("offset=").append(offset);
        }
        if (limit != -1) {
            if (sb.length() > 0) sb.append(", ");
            sb.append("limit=").append(limit);
        }
        */
        sb.append("]");
        sb.insert(0, "[");
        return sb;
    }
}
