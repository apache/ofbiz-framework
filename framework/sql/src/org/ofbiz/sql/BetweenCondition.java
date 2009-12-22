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

public final class BetweenCondition extends Condition {
    private final Value left;
    private final Value r1;
    private final Value r2;

    public BetweenCondition(Value left, Value r1, Value r2) {
        this.left = left;
        this.r1 = r1;
        this.r2 = r2;
    }

    public void accept(Visitor visitor) {
        visitor.visit(this);
    }

    public Value getLeft() {
        return left;
    }

    public Value getR1() {
        return r1;
    }

    public Value getR2() {
        return r2;
    }

    public StringBuilder appendTo(StringBuilder sb) {
        left.appendTo(sb);
        sb.append(" BETWEEN ");
        r1.appendTo(sb);
        sb.append(" AND ");
        r2.appendTo(sb);
        return sb;
    }
}
