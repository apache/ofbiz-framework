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

public final class Unioned extends Atom {
    public enum Operator { UNION, UNION_ALL, INTERSECT, INTERSECT_ALL, EXCEPT, EXCEPT_ALL }

    private final Operator operator;
    private final SelectGroup group;
    private final Unioned next;

    public Unioned(Operator operator, SelectGroup group, Unioned next) {
        this.operator = operator;
        this.group = group;
        this.next = next;
    }

    public Operator getOperator() {
        return operator;
    }

    public SelectGroup getGroup() {
        return group;
    }

    public Unioned getNext() {
        return next;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Unioned) {
            Unioned other = (Unioned) o;
            return operator.equals(other.operator) && group.equals(other.group) && equalsHelper(next, other.next);
        } else {
            return false;
        }
    }

    public StringBuilder appendTo(StringBuilder sb) {
        sb.append(' ').append(operator.toString().replace('_', ' ')).append(' ');
        group.appendTo(sb);
        if (next != null) {
            next.appendTo(sb);
        }
        return sb;
    }
}
