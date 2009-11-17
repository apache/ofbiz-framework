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

public final class TableName extends Atom {
    private final String tableName;
    private final String alias;

    public TableName(String tableName, String alias) {
        this.tableName = tableName;
        this.alias = alias != null ? alias : tableName;
    }

    public final String getTableName() {
        return tableName;
    }

    public final String getAlias() {
        return alias;
    }

    public StringBuilder appendTo(StringBuilder sb) {
        sb.append(tableName);
        if (!alias.equals(tableName)) {
            sb.append(' ').append(alias);
        }
        return sb;
    }
}
