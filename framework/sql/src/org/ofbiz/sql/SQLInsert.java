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

public final class SQLInsert extends SQLStatement<SQLInsert> {
    private final TableName tableName;
    private final InsertSource source;
    private final List<String> columns;

    public SQLInsert(TableName tableName, InsertSource source, List<String> columns) {
        this.tableName = tableName;
        this.source = source;
        this.columns = columns;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }

    public TableName getTableName() {
        return tableName;
    }

    public InsertSource getSource() {
        return source;
    }

    public Iterator<String> iterator() {
        return columns.iterator();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof SQLInsert) {
            SQLInsert other = (SQLInsert) o;
            return tableName.equals(other.tableName) && equalsHelper(columns, other.columns) && source.equals(other.source);
        } else {
            return false;
        }
    }

    public StringBuilder appendTo(StringBuilder sb) {
        sb.append("INSERT INTO ");
        tableName.appendTo(sb);
        if (columns != null && !columns.isEmpty()) {
            sb.append(" (");
            StringUtil.append(sb, columns, null, null, ", ");
            sb.append(')');
        }
        sb.append(' ');
        source.appendTo(sb);
        return sb;
    }
}
