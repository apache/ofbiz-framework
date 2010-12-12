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
public final class ConditionList extends Condition implements Iterable<Condition> {
    private final Joiner joiner;
    private final List<Condition> conditions;

    public ConditionList(Joiner joiner, List<Condition> conditions) {
        this.joiner = joiner;
        this.conditions = conditions;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }

    public void add(Condition condition) {
        conditions.add(condition);
    }

    public Joiner getJoiner() {
        return joiner;
    }

    public Iterator<Condition> iterator() {
        return conditions.iterator();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof ConditionList) {
            ConditionList other = (ConditionList) o;
            return joiner.equals(other.joiner) && conditions.equals(other.conditions);
        } else {
            return false;
        }
    }

    public StringBuilder appendTo(StringBuilder sb) {
        sb.append('(');
        StringUtil.appendTo(sb, conditions, " ", " ", joiner.toString());
        sb.append(')');
        return sb;
    }
}
