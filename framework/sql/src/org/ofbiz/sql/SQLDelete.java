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

public final class SQLDelete extends SQLStatement<SQLDelete> {
    private final Table table;
    private final Condition whereCondition;

    public SQLDelete(Table table, Condition whereCondition) {
        this.table = table;
        this.whereCondition = whereCondition;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }

    public Table getTable() {
        return table;
    }

    public Condition getWhereCondition() {
        return whereCondition;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof SQLDelete) {
            SQLDelete other = (SQLDelete) o;
            return table.equals(other.table) && equalsHelper(whereCondition, other.whereCondition);
        } else {
            return false;
        }
    }

    public StringBuilder appendTo(StringBuilder sb) {
        sb.append("DELETE FROM ");
        table.getTableName().appendTo(sb);
        if (table.getJoined() != null) {
            sb.append(" USING");
            table.getJoined().appendToRest(table.getTableName().getAlias(), sb);
        }
        if (whereCondition != null) {
            sb.append(" WHERE ");
            whereCondition.appendTo(sb);
        }
        sb.append(';');
        return sb;
    }
}
