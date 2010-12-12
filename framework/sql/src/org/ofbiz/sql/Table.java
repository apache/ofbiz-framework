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

public final class Table extends Atom {
    private final TableName tableName;
    private final Joined joined;

    public Table(TableName tableName) {
        this(tableName, null);
    }

    public Table(TableName tableName, Joined joined) {
        this.tableName = tableName;
        this.joined = joined;
    }

    public TableName getTableName() {
        return tableName;
    }

    public Joined getJoined() {
        return joined;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Table) {
            Table other = (Table) o;
            return tableName.equals(other.tableName) && equalsHelper(joined, other.joined);
        } else {
            return false;
        }
    }

    public StringBuilder appendTo(StringBuilder sb) {
        tableName.appendTo(sb);
        if (joined != null) {
            joined.appendTo(tableName.getAlias(), sb);
        }
        return sb;
    }
}
