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

import java.util.Comparator;

import org.ofbiz.entity.GenericEntity;
import org.ofbiz.entity.GenericModelException;
import org.ofbiz.entity.config.DatasourceInfo;
import org.ofbiz.entity.model.ModelEntity;

public class OrderByItem implements Comparator {
    public static final int DEFAULT = 0;
    public static final int UPPER   = 1;
    public static final int LOWER   = 2;

    protected boolean descending;
    protected EntityConditionValue value;

    public OrderByItem(EntityConditionValue value) {
        this.value = value;
    }

    public OrderByItem(EntityConditionValue value, boolean descending) {
        this(value);
        this.descending = descending;
    }

    public EntityConditionValue getValue() {
        return value;
    }

    public boolean getDescending() {
        return descending;
    }

    public static final OrderByItem parse(Object obj) {
        if (obj instanceof String) {
            return parse((String) obj);
        } else if (obj instanceof EntityConditionValue) {
            return new OrderByItem((EntityConditionValue) obj, false);
        } else if (obj instanceof OrderByItem) {
            return (OrderByItem) obj;
        } else {
            throw new IllegalArgumentException("unknown orderBy item: " + obj);
        }
    }

    public static final OrderByItem parse(String text) {
        text = text.trim();
        int startIndex = 0, endIndex = text.length();
        boolean descending;
        int caseSensitivity;
        if (text.endsWith(" DESC")) {
            descending = true;
            endIndex -= 5;
        } else if (text.endsWith(" ASC")) {
            descending = false;
            endIndex -= 4;
        } else if (text.startsWith("-")) {
            descending = true;
            startIndex++;
        } else if (text.startsWith("+")) {
            descending = false;
            startIndex++;
        } else {
            descending = false;
        }

        if (startIndex != 0 || endIndex != text.length()) {
            text = text.substring(startIndex, endIndex);
            startIndex = 0;
            endIndex = text.length();
        }

        if (text.endsWith(")")) {
            String upperText = text.toUpperCase();
            endIndex--;
            if (upperText.startsWith("UPPER(")) {
                caseSensitivity = UPPER;
                startIndex = 6;
            } else if (upperText.startsWith("LOWER(")) {
                caseSensitivity = LOWER;
                startIndex = 6;
            } else {
                caseSensitivity = DEFAULT;
            }
        } else {
            caseSensitivity = DEFAULT;
        }

        if (startIndex != 0 || endIndex != text.length()) {
            text = text.substring(startIndex, endIndex);
            startIndex = 0;
            endIndex = text.length();
        }
        EntityConditionValue value = new EntityFieldValue(text);
        switch (caseSensitivity) {
            case UPPER:
                value = new EntityFunction.UPPER(value);
                break;
            case LOWER:
                value = new EntityFunction.LOWER(value);
                break;
        }
        return new OrderByItem(value, descending);
    }

    public int compare(java.lang.Object obj1, java.lang.Object obj2) {
        return compare((GenericEntity) obj1, (GenericEntity) obj2);
    }
        
    public void checkOrderBy(ModelEntity modelEntity) throws GenericModelException {
        value.validateSql(modelEntity);
    }

    public int compare(GenericEntity obj1, GenericEntity obj2) {
        Object value1 = value.getValue(obj1);
        Object value2 = value.getValue(obj2);

        int result;
        // null is defined as the largest possible value
        if (value1 == null) {
            result = value2 == null ? 0 : 1;
        } else if (value2 == null) {
            result = value1 == null ? 0 : -1;
        } else {
            result = ((Comparable) value1).compareTo(value2);
        }
        // if (Debug.infoOn()) Debug.logInfo("[OrderByComparator.compareAsc] Result is " + result + " for [" + value + "] and [" + value2 + "]", module);
        return descending ? -result : result;
    }

    public String makeOrderByString(ModelEntity modelEntity, boolean includeTablenamePrefix, DatasourceInfo datasourceInfo) {
        StringBuffer sb = new StringBuffer();
        makeOrderByString(sb, modelEntity, includeTablenamePrefix, datasourceInfo);
        return sb.toString();
    }

    public void makeOrderByString(StringBuffer sb, ModelEntity modelEntity, boolean includeTablenamePrefix, DatasourceInfo datasourceInfo) {
        getValue().addSqlValue(sb, modelEntity, null, includeTablenamePrefix, datasourceInfo);
        sb.append(descending ? " DESC" : " ASC");
    }

    public boolean equals(java.lang.Object obj) {
        if (!(obj instanceof OrderByItem)) return false;
        OrderByItem that = (OrderByItem) obj;

        return getValue().equals(that.getValue()) && getDescending() == that.getDescending();
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(getValue());
        sb.append(descending ? " DESC" : " ASC");
        return sb.toString();
    }
}
