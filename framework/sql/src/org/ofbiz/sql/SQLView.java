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

public final class SQLView extends SQLStatement<SQLView> {
    private final String name;
    private final SQLSelect sqlSelect;

    public SQLView(String name, SQLSelect sqlSelect) {
        this.name = name;
        this.sqlSelect = sqlSelect;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }

    public String getName() {
        return name;
    }

    public SQLSelect getSelect() {
        return sqlSelect;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof SQLView) {
            SQLView other = (SQLView) o;
            return name.equals(other.name) && sqlSelect.equals(other.sqlSelect);
        } else {
            return false;
        }
    }

    public StringBuilder appendTo(StringBuilder sb) {
        sb.append("CREATE VIEW ").append(name).append(" AS ");
        sqlSelect.appendTo(sb);
        return sb;
    }
}
