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

public final class Relation extends Atom implements Iterable<KeyMap> {
    private final String name;
    private final String type;
    private final String title;
    private final String entityName;
    private final List<KeyMap> keyMaps;

    public Relation(String type, String title, String entityName, List<KeyMap> keyMaps) {
        this.type = type;
        this.title = title;
        this.entityName = entityName;
        this.keyMaps = keyMaps;
        this.name = title == null ? entityName : title + entityName;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getEntityName() {
        return entityName;
    }

    public Iterator<KeyMap> iterator() {
        return keyMaps.iterator();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Relation) {
            Relation other = (Relation) o;
            return equalsHelper(type, other.type) && equalsHelper(title, other.title) && entityName.equals(other.entityName) && keyMaps.equals(other.keyMaps);
        } else {
            return false;
        }
    }

    public StringBuilder appendTo(StringBuilder sb) {
        sb.append("RELATION");
        if (type != null) {
            sb.append(" TYPE ").append(type);
        }
        if (title != null) {
            sb.append(" TITLE ").append(title);
        }
        sb.append(' ').append(entityName);
        sb.append(" MAP");
        for (int i = 0; i < keyMaps.size(); i++) {
            KeyMap keyMap = keyMaps.get(i);
            if (i != 0) sb.append(" AND ");
            sb.append(' ').append(keyMap.getLeftFieldName());
            sb.append(" = ").append(keyMap.getRightFieldName());
        }
        return sb;
    }
}
