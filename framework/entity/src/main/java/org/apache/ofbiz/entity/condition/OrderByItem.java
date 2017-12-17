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

package org.apache.ofbiz.entity.condition;

import java.util.Comparator;
import java.util.Locale;

import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.entity.GenericEntity;
import org.apache.ofbiz.entity.GenericModelException;
import org.apache.ofbiz.entity.config.model.Datasource;
import org.apache.ofbiz.entity.model.ModelEntity;

public class OrderByItem implements Comparator<GenericEntity> {
    public static final int DEFAULT = 0;
    public static final int UPPER   = 1;
    public static final int LOWER   = 2;

    public static final String NULLS_FIRST = "NULLS FIRST";
    public static final String NULLS_LAST = "NULLS LAST";
    public static final String module = OrderByItem.class.getName();

    protected boolean descending;
    protected Boolean nullsFirst;
    protected EntityConditionValue value;

    public OrderByItem(EntityConditionValue value) {
        this.value = value;
    }

    public OrderByItem(EntityConditionValue value, boolean descending) {
        this(value);
        this.descending = descending;
    }

    public OrderByItem(EntityConditionValue value, boolean descending, Boolean nullsFirst) {
        this(value, descending);
        this.nullsFirst = nullsFirst;
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

        // handle nulls first/last
        Boolean nullsFirst = null;
        if (text.toUpperCase(Locale.getDefault()).endsWith(NULLS_FIRST)) {
            nullsFirst = true;
            text = text.substring(0, text.length() - NULLS_FIRST.length()).trim();
        }

        if (text.toUpperCase(Locale.getDefault()).endsWith(NULLS_LAST)) {
            nullsFirst = false;
            text = text.substring(0, text.length() - NULLS_LAST.length()).trim();
        }

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
            String upperText = text.toUpperCase(Locale.getDefault());
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
        }
        EntityConditionValue value = EntityFieldValue.makeFieldValue(text);
        switch (caseSensitivity) {
            case UPPER:
                value = EntityFunction.UPPER(value);
                break;
            case LOWER:
                value = EntityFunction.LOWER(value);
                break;
            default:
                break;
        }
        return new OrderByItem(value, descending, nullsFirst);
    }

    public void checkOrderBy(ModelEntity modelEntity) throws GenericModelException {
        value.validateSql(modelEntity);
    }

    public int compare(GenericEntity obj1, GenericEntity obj2) {
        Comparable<Object> value1 = UtilGenerics.cast(value.getValue(obj1));
        Object value2 = value.getValue(obj2);

        int result;
        // null is defined as the largest possible value
        if (value1 == null) {
            result = value2 == null ? 0 : 1;
        } else if (value2 == null) {
            result = -1;
        } else {
            result = value1.compareTo(value2);
        }
        return descending ? -result : result;
    }

    public String makeOrderByString(ModelEntity modelEntity, boolean includeTablenamePrefix, Datasource datasourceInfo) {
        StringBuilder sb = new StringBuilder();
        makeOrderByString(sb, modelEntity, includeTablenamePrefix, datasourceInfo);
        return sb.toString();
    }

    public void makeOrderByString(StringBuilder sb, ModelEntity modelEntity, boolean includeTablenamePrefix, Datasource datasourceInfo) {
        if ((nullsFirst != null) && (!datasourceInfo.getUseOrderByNulls())) {
            sb.append("CASE WHEN ");
            getValue().addSqlValue(sb, modelEntity, null, includeTablenamePrefix, datasourceInfo);
            sb.append(" IS NULL THEN ");
            sb.append(nullsFirst ? "0" : "1");
            sb.append(" ELSE ");
            sb.append(nullsFirst ? "1" : "0");
            sb.append(" END, ");
        }

        getValue().addSqlValue(sb, modelEntity, null, includeTablenamePrefix, datasourceInfo);
        sb.append(descending ? " DESC" : " ASC");

        if ((nullsFirst != null) && (datasourceInfo.getUseOrderByNulls())) {
            sb.append(nullsFirst ? " NULLS FIRST" : " NULLS LAST");
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (descending ? 1231 : 1237);
        result = prime * result + ((nullsFirst == null) ? 0 : nullsFirst.hashCode());
        result = prime * result + ((value == null) ? 0 : value.hashCode());
        return result;
    }

    @Override
    public boolean equals(java.lang.Object obj) {
        if (!(obj instanceof OrderByItem)) {
            return false;
        }
        OrderByItem that = (OrderByItem) obj;

        return getValue().equals(that.getValue()) && getDescending() == that.getDescending();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getValue());
        sb.append(descending ? " DESC" : " ASC");
        if (nullsFirst != null) {
            sb.append(nullsFirst ? " NULLS FIRST" : " NULLS LAST");
        }
        return sb.toString();
    }
}
