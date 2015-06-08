/*******************************************************************************
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
 *******************************************************************************/
package org.ofbiz.entity.condition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.ofbiz.base.util.UtilGenerics;
import org.ofbiz.entity.util.EntityUtil;

/**
 * Encapsulates simple expressions used for specifying queries
 *
 */
@SuppressWarnings("serial")
public final class EntityFieldMap extends EntityConditionListBase<EntityExpr> {

    protected final Map<String, ? extends Object> fieldMap;

    public static <V> List<EntityExpr> makeConditionList(Map<String, V> fieldMap, EntityComparisonOperator<?,V> op) {
        if (fieldMap == null) {
            return Collections.emptyList();
        }
        List<EntityExpr> list = new ArrayList<EntityExpr>(fieldMap.size());
        for (Map.Entry<String, ? extends Object> entry: fieldMap.entrySet()) {
            list.add(EntityCondition.makeCondition(entry.getKey(), op, entry.getValue()));
        }
        return list;
    }

    public <V> EntityFieldMap(EntityComparisonOperator<?,?> compOp, EntityJoinOperator joinOp, V... keysValues) {
        this(EntityUtil.makeFields(keysValues), UtilGenerics.<EntityComparisonOperator<String,V>>cast(compOp), joinOp);
    }

    public <V> EntityFieldMap(Map<String, V> fieldMap, EntityComparisonOperator<?,?> compOp, EntityJoinOperator joinOp) {
        super(makeConditionList(fieldMap, UtilGenerics.<EntityComparisonOperator<String,V>>cast(compOp)), joinOp);
        this.fieldMap = fieldMap == null ? Collections.<String, Object>emptyMap() : fieldMap;
    }

    public Object getField(String name) {
        return this.fieldMap.get(name);
    }

    public boolean containsField(String name) {
        return this.fieldMap.containsKey(name);
    }

    public Iterator<String> getFieldKeyIterator() {
        return Collections.unmodifiableSet(this.fieldMap.keySet()).iterator();
    }

    public Iterator<?> getFieldEntryIterator() {
        return Collections.unmodifiableMap(this.fieldMap).entrySet().iterator();
    }

    @Override
    public void accept(EntityConditionVisitor visitor) {
        visitor.acceptEntityFieldMap(this);
    }
}
