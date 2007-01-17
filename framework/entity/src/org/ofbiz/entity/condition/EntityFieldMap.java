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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Encapsulates simple expressions used for specifying queries
 *
 */
public class EntityFieldMap extends EntityConditionListBase {

    protected Map fieldMap;

    protected EntityFieldMap() {
        super();
    }

    public static List makeConditionList(Map fieldMap, EntityComparisonOperator op) {
        if (fieldMap == null) return new ArrayList();
        List list = new ArrayList(fieldMap.size());
        Iterator it = fieldMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            String field = (String)entry.getKey();
            Object value = entry.getValue();
            list.add(new EntityExpr(field, op, value));
        }
        return list;
    }

    public EntityFieldMap(Map fieldMap, EntityComparisonOperator compOp, EntityJoinOperator joinOp) {
        super(makeConditionList(fieldMap, compOp), joinOp);
        this.fieldMap = fieldMap;
        if (this.fieldMap == null) this.fieldMap = new LinkedHashMap();
        this.operator = joinOp;
    }

    public EntityFieldMap(Map fieldMap, EntityJoinOperator operator) {
        this(fieldMap, EntityOperator.EQUALS, operator);
    }

    public Object getField(String name) {
        return this.fieldMap.get(name);
    }
    
    public boolean containsField(String name) {
        return this.fieldMap.containsKey(name);
    }
    
    public Iterator getFieldKeyIterator() {
        return Collections.unmodifiableSet(this.fieldMap.keySet()).iterator();
    }
    
    public Iterator getFieldEntryIterator() {
        return Collections.unmodifiableSet(this.fieldMap.entrySet()).iterator();
    }
    
    public void accept(EntityConditionVisitor visitor) {
        visitor.acceptEntityFieldMap(this);
    }
}
