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
package org.ofbiz.sql;

import java.util.List;
import java.util.Map;

import org.ofbiz.base.util.StringUtil;

public final class SQLSelect extends SQLStatement<SQLSelect> implements InsertSource {
    private final SelectGroup selectGroup;
    private final Unioned unioned;
    private final Map<String, Relation> relations;
    private final int offset;
    private final int limit;
    private final List<OrderByItem> orderBy;

    public SQLSelect(boolean isDistinct, List<FieldAll> fieldAlls, Map<String, FieldDef> fieldDefs, Table table, Map<String, Relation> relations, Condition whereCondition, Condition havingCondition, List<String> groupBy, List<OrderByItem> orderBy, int offset, int limit) {
        this(new SelectGroup(isDistinct, fieldAlls, fieldDefs, table, whereCondition, havingCondition, groupBy), null, relations, orderBy, offset, limit);
    }

    public SQLSelect(SelectGroup selectGroup, Unioned unioned, Map<String, Relation> relations, List<OrderByItem> orderBy, int offset, int limit) {
        this.selectGroup = selectGroup;
        this.unioned = unioned;
        this.relations = checkEmpty(relations);
        this.orderBy = orderBy;
        this.offset = offset;
        this.limit = limit;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }

    public SelectGroup getSelectGroup() {
        return selectGroup;
    }

    public Unioned getUnioned() {
        return unioned;
    }

    public Map<String, Relation> getRelations() {
        return relations;
    }

    public List<OrderByItem> getOrderBy() {
        return orderBy;
    }

    public int getOffset() {
        return offset;
    }

    public int getLimit() {
        return limit;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof SQLSelect)) {
            return false;
        }

        SQLSelect other = (SQLSelect) o;
        return selectGroup.equals(other.selectGroup)
            && equalsHelper(unioned, other.unioned)
            && equalsHelper(relations, other.relations)
            && offset == other.offset
            && limit == other.limit
            && equalsHelper(orderBy, other.orderBy)
        ;
    }

    public StringBuilder appendTo(StringBuilder sb) {
        selectGroup.appendTo(sb);
        if (unioned != null) {
            unioned.appendTo(sb);
        }
        if (relations != null) {
            StringUtil.appendTo(sb, relations.values(), " ", null, null);
        }
        if (orderBy != null) {
            sb.append(" ORDER BY ");
            StringUtil.append(sb, orderBy, null, null, ", ");
        }
        if (offset != -1) {
            sb.append(" OFFSET ").append(offset);
        }
        if (limit != -1) {
            sb.append(" LIMIT ").append(limit);
        }
        sb.append(';');
        return sb;
    }
}
