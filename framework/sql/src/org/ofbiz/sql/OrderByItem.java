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

public final class OrderByItem extends Atom {
    enum Order { DEFAULT, ASCENDING, DESCENDING };

    private final Order order;
    private final String functionName;
    private final String fieldName;

    public OrderByItem(Order order, String functionName, String fieldName) {
        this.order = order;
        this.functionName = functionName;
        this.fieldName = fieldName;
    }

    public final Order getOrder() {
        return order;
    }

    public final String getFunctionName() {
        return functionName;
    }

    public final String getFieldName() {
        return fieldName;
    }

    public StringBuilder appendTo(StringBuilder sb) {
        if (functionName != null) sb.append(functionName).append('(');
        sb.append(fieldName);
        if (functionName != null) sb.append(')');
        switch (order) {
            case ASCENDING:
                sb.append(" ASC");
                break;
            case DESCENDING:
                sb.append(" DESC");
                break;
        }
        return sb;
    }
}
