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

import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.model.DynamicViewEntity;
import org.ofbiz.entity.util.EntityListIterator;

public class SQLSelect extends SimpleNode {
    private DynamicViewEntity dve;
    private EntityCondition whereCondition;
    private EntityCondition havingCondition;
    private int offset = -1;
    private int limit = -1;

    public SQLSelect(int id) {
        super(id);
    }

    public SQLSelect(Parser p, int id) {
        super(p, id);
    }


    /** Accept the visitor. **/
    public Object jjtAccept(ParserVisitor visitor, Object data) {
        return visitor.visit(this, data);
    }

    public EntityListIterator getEntityListIterator(Delegator delegator) throws GenericEntityException {
        return delegator.findListIteratorByCondition(dve, whereCondition, havingCondition, null, null, null);
    }

    void setDynamicViewEntity(DynamicViewEntity dve) {
        this.dve = dve;
    }

    public DynamicViewEntity getDynamicViewEntity() {
        return dve;
    }

    void setWhereCondition(EntityCondition whereCondition) {
        this.whereCondition = whereCondition;
    }

    public EntityCondition getWhereCondition() {
        return whereCondition;
    }

    void setHavingCondition(EntityCondition havingCondition) {
        this.havingCondition = havingCondition;
    }

    public EntityCondition getHavingCondition() {
        return havingCondition;
    }

    void setOffset(int offset) {
        this.offset = offset;
    }

    public int getOffset() {
        return offset;
    }

    void setLimit(int limit) {
        this.limit = limit;
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
/* JavaCC - OriginalChecksum=49309c1a721b16d029f160d2568a03bc (do not edit this line) */
