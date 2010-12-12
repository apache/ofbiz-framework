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

import org.ofbiz.base.lang.SourceMonitored;
import org.ofbiz.base.util.StringUtil;

@SourceMonitored
public final class MathValue extends StaticValue implements Iterable<ConstantValue> {
    private final String op;
    private final List<ConstantValue> values;

    public MathValue(String op, List<ConstantValue> values) {
        this.op = op.toLowerCase();
        this.values = values;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }

    public String getOp() {
        return op;
    }

    @Override
    public String getDefaultName() {
        return null;
    }

    public Iterator<ConstantValue> iterator() {
        return values.iterator();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof MathValue) {
            MathValue other = (MathValue) o;
            return op.equals(other.op) && values.equals(other.values);
        } else {
            return false;
        }
    }

    public StringBuilder appendTo(StringBuilder sb) {
        sb.append('(');
        StringUtil.appendTo(sb, values, null, null, " ", op, " ");
        sb.append(')');
        return sb;
    }
}
