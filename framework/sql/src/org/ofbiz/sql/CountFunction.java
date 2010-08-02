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
public final class CountFunction extends StaticValue {
    private final boolean isDistinct;
    private final FieldValue field;

    public CountFunction(boolean isDistinct, FieldValue field) {
        this.isDistinct = isDistinct;
        this.field = field;
    }

    public void accept(Visitor visitor) {
        visitor.visit(this);
    }

    public String getDefaultName() {
        return "COUNT";
    }

    public boolean isDistinct() {
        return isDistinct;
    }

    public FieldValue getField() {
        return field;
    }

    public boolean equals(Object o) {
        if (o instanceof CountFunction) {
            CountFunction other = (CountFunction) o;
            return isDistinct == other.isDistinct && field.equals(other.field);
        } else {
            return false;
        }
    }

    public StringBuilder appendTo(StringBuilder sb) {
        sb.append("COUNT(");
        if (isDistinct) sb.append("DISTINCT ");
        field.appendTo(sb);
        sb.append(')');
        return sb;
    }
}
