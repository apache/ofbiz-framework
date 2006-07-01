/*
 * $Id: EntityFieldMap.java 5462 2005-08-05 18:35:48Z jonesde $
 *
 * Copyright (c) 2001-2005 The Open For Business Project - www.ofbiz.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 * CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT
 * OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 * THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */
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
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @version    $Rev$
 * @since      2.0
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
