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
public final class BooleanCondition extends Condition {
    private final Value left;
    private final String op;
    private final Value right;

    public BooleanCondition(Value left, String op, Value right) {
        this.left = left;
        this.op = op.toLowerCase();
        this.right = right;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }

    public Value getLeft() {
        return left;
    }

    public String getOp() {
        return op;
    }

    public Value getRight() {
        return right;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof BooleanCondition) {
            BooleanCondition other = (BooleanCondition) o;
            return left.equals(other.left) & op.equals(other.op) && right.equals(other.right);
        } else {
            return false;
        }
    }

    public StringBuilder appendTo(StringBuilder sb) {
        left.appendTo(sb);
        sb.append(' ').append(op).append(' ');
        right.appendTo(sb);
        return sb;
    }
}
