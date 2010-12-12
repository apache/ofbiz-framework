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

public final class Joined extends Atom implements Iterable<KeyMap> {
    private final boolean isOptional;
    private final TableName tableName;
    private final List<KeyMap> keyMaps;
    private final Joined joined;

    public Joined(boolean isOptional, TableName tableName, List<KeyMap> keyMaps) {
        this(isOptional, tableName, keyMaps, null);
    }

    public Joined(boolean isOptional, TableName tableName, List<KeyMap> keyMaps, Joined joined) {
        this.isOptional = isOptional;
        this.tableName = tableName;
        this.keyMaps = keyMaps;
        this.joined = joined;
    }

    public boolean isOptional() {
        return isOptional;
    }

    public TableName getTableName() {
        return tableName;
    }

    public Iterator<KeyMap> iterator() {
        return keyMaps.iterator();
    }

    public Joined getJoined() {
        return joined;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Joined) {
            Joined other = (Joined) o;
            return tableName.equals(other.tableName) && keyMaps.equals(other.keyMaps) && equalsHelper(joined, other.joined);
        } else {
            return false;
        }
    }

    public StringBuilder appendTo(StringBuilder sb) {
        return appendTo("left", sb);
    }

    public StringBuilder appendTo(String leftAlias, StringBuilder sb) {
        sb.append(isOptional ? " LEFT JOIN " : " JOIN ");
        return appendToRest(leftAlias, sb);
    }

    public StringBuilder appendToRest(String leftAlias, StringBuilder sb) {
        tableName.appendTo(sb);
        sb.append(" ON ");
        for (int i = 0; i < keyMaps.size(); i++) {
            KeyMap keyMap = keyMaps.get(i);
            if (i != 0) sb.append(" AND ");
            sb.append(' ').append(leftAlias).append('.').append(keyMap.getLeftFieldName());
            sb.append(" = ").append(tableName.getAlias()).append('.').append(keyMap.getRightFieldName());
        }
        if (joined != null) {
            joined.appendTo(tableName.getAlias(), sb);
        }
        return sb;
    }
}
