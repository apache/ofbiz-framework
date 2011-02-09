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
public final class AggregateFunction extends StaticValue {
    public static final String module = AggregateFunction.class.getName();

    private final String name;
    private final boolean isDistinct;
    private final StaticValue value;

    public AggregateFunction(String name, boolean isDistinct, StaticValue value) {
        this.name = name;
        this.isDistinct = isDistinct;
        this.value = value;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }

    public String getName() {
        return name;
    }

    public boolean isDistinct() {
        return isDistinct;
    }

    @Override
    public String getDefaultName() {
        return name;
    }

    public StaticValue getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof AggregateFunction) {
            AggregateFunction other = (AggregateFunction) o;
            return name.equals(other.name) && isDistinct == other.isDistinct && value.equals(other.value);
        } else {
            return false;
        }
    }

    public StringBuilder appendTo(StringBuilder sb) {
        sb.append(name).append('(');
        if (isDistinct) {
            sb.append("DISTINCT ");
        }
        value.appendTo(sb);
        sb.append(')');
        return sb;
    }
}
