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

import org.ofbiz.base.lang.SourceMonitored;

@SourceMonitored
public final class FieldValue extends StaticValue {
    private final String fieldName;
    private final String tableName;

    public FieldValue(String fieldName) {
        this(null, fieldName);
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }

    public FieldValue(String tableName, String fieldName) {
        this.tableName = tableName;
        this.fieldName = fieldName;
    }

    public final String getTableName() {
        return tableName;
    }

    public final String getFieldName() {
        return fieldName;
    }

    @Override
    public String getDefaultName() {
        return fieldName;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof FieldValue) {
            FieldValue other = (FieldValue) o;
            return fieldName.equals(other.fieldName) && equalsHelper(tableName, other.tableName);
        } else {
            return false;
        }
    }

    public StringBuilder appendTo(StringBuilder sb) {
        if (tableName != null) {
            sb.append(tableName).append('.');
        }
        sb.append(fieldName);
        return sb;
    }
}
