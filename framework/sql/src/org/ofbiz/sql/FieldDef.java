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
public final class FieldDef extends Atom {
    private final String alias;
    private final StaticValue value;

    public FieldDef(StaticValue value, String alias) {
        this.alias = alias;
        this.value = value;
    }

    public final String getAlias() {
        return alias;
    }

    public String getDefaultName() {
        return alias == null ? value.getDefaultName() : alias;
    }

    public StaticValue getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof FieldDef) {
            FieldDef other = (FieldDef) o;
            return equalsHelper(alias, other.alias) && value.equals(other.value);
        } else {
            return false;
        }
    }

    public StringBuilder appendTo(StringBuilder sb) {
        value.appendTo(sb);
        if (alias != null) {
            sb.append(" AS ").append(alias);
        }
        return sb;
    }
}
