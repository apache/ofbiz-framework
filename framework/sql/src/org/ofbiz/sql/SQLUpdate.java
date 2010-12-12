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

import java.util.Iterator;
import java.util.List;

import org.ofbiz.base.util.StringUtil;

public final class SQLUpdate extends SQLStatement<SQLUpdate> implements Iterable<SetField> {
    private final Table table;
    private final List<SetField> setFields;
    private final Condition whereCondition;

    public SQLUpdate(Table table, List<SetField> setFields, Condition whereCondition) {
        this.table = table;
        this.setFields = setFields;
        this.whereCondition = whereCondition;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }

    public Table getTable() {
        return table;
    }

    public Iterator<SetField> iterator() {
        return setFields.iterator();
    }

    public Condition getWhereCondition() {
        return whereCondition;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof SQLUpdate) {
            SQLUpdate other = (SQLUpdate) o;
            return table.equals(other.table) && setFields.equals(other.setFields) && equalsHelper(whereCondition, other.whereCondition);
        } else {
            return false;
        }
    }

    public StringBuilder appendTo(StringBuilder sb) {
        sb.append("UPDATE ");
        table.getTableName().appendTo(sb);
        // n=4;1+(n-1)*2+3+(n-1)*2+1;3*n+(n-1)*2
        // 17
        // 18
        sb.append(" SET ");
        if (setFields.size() <= 3) {
            StringUtil.appendTo(sb, setFields, null, null, ", ");
        } else {
            Iterator<SetField> it = setFields.iterator();
            sb.append('(');
            while (it.hasNext()) {
                SetField setField = it.next();
                sb.append(setField.getName());
                if (it.hasNext()) {
                    sb.append(", ");
                }
            }
            sb.append(") = (");
            it = setFields.iterator();
            while (it.hasNext()) {
                SetField setField = it.next();
                setField.getValue().appendTo(sb);
                if (it.hasNext()) {
                    sb.append(", ");
                }
            }
            sb.append(')');
            it = setFields.iterator();
        }
        if (table.getJoined() != null) {
            sb.append(" FROM ");
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
