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

import org.ofbiz.base.util.StringUtil;

public final class SQLIndex extends SQLStatement<SQLIndex> {
    private final boolean isUnique;
    private final String name;
    private final String table;
    private final String using;
    private final List<ConstantValue> values;

    public SQLIndex(boolean isUnique, String name, String table, String using, List<ConstantValue> values) {
        this.isUnique = isUnique;
        this.name = name;
        this.table = table;
        this.using = using;
        this.values = values;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }

    public boolean getIsUnique() {
        return isUnique;
    }

    public String getName() {
        return name;
    }

    public String getTable() {
        return table;
    }

    public String getUsing() {
        return using;
    }

    public List<ConstantValue> getValues() {
        return values;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof SQLIndex) {
            SQLIndex other = (SQLIndex) o;
            return isUnique == other.isUnique && name.equals(other.name) && table.equals(other.table) && equalsHelper(using, other.using) && values.equals(other.values);
        } else {
            return false;
        }
    }

    public StringBuilder appendTo(StringBuilder sb) {
        sb.append("CREATE");
        if (isUnique) {
            sb.append(" UNIQUE");
        }
        sb.append(" INDEX ");
        sb.append(name);
        sb.append(" ON ");
        sb.append(table);
        if (using != null) {
            sb.append(" USING ").append(using);
        }
        sb.append(" (");
        StringUtil.append(sb, values, null, null, ", ");
        sb.append(')');
        sb.append(';');
        return sb;
    }
}
