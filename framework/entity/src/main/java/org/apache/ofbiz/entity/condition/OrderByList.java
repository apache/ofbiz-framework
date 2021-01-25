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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import org.apache.ofbiz.entity.GenericEntity;
import org.apache.ofbiz.entity.GenericModelException;
import org.apache.ofbiz.entity.config.model.Datasource;
import org.apache.ofbiz.entity.model.ModelEntity;

public class OrderByList implements Comparator<GenericEntity> {
    private List<OrderByItem> orderByList = new ArrayList<>();

    public OrderByList() {
    }

    public OrderByList(String... orderByList) {
        addOrderBy(orderByList);
    }

    public OrderByList(Collection<String> orderByList) {
        addOrderBy(orderByList);
    }

    /**
     * Add order by.
     * @param orderByList the order by list
     */
    public void addOrderBy(String... orderByList) {
        for (String orderByItem: orderByList) {
            addOrderBy(orderByItem);
        }
    }

    /**
     * Add order by.
     * @param orderByList the order by list
     */
    public void addOrderBy(Collection<String> orderByList) {
        for (String orderByItem: orderByList) {
            addOrderBy(orderByItem);
        }
    }

    /**
     * Add order by.
     * @param text the text
     */
    public void addOrderBy(String text) {
        addOrderBy(OrderByItem.parse(text));
    }

    /**
     * Add order by.
     * @param value the value
     */
    public void addOrderBy(EntityConditionValue value) {
        addOrderBy(value, false);
    }

    /**
     * Add order by.
     * @param value the value
     * @param descending the descending
     */
    public void addOrderBy(EntityConditionValue value, boolean descending) {
        addOrderBy(new OrderByItem(value, descending));
    }

    /**
     * Add order by.
     * @param orderByItem the order by item
     */
    public void addOrderBy(OrderByItem orderByItem) {
        orderByList.add(orderByItem);
    }

    /**
     * Check order by.
     * @param modelEntity the model entity
     * @throws GenericModelException the generic model exception
     */
    public void checkOrderBy(ModelEntity modelEntity) throws GenericModelException {
        for (OrderByItem orderByItem: orderByList) {
            orderByItem.checkOrderBy(modelEntity);
        }
    }

    /**
     * Make order by string string.
     * @param modelEntity the model entity
     * @param includeTablenamePrefix the include tablename prefix
     * @param datasourceInfo the datasource info
     * @return the string
     */
    public String makeOrderByString(ModelEntity modelEntity, boolean includeTablenamePrefix, Datasource datasourceInfo) {
        StringBuilder sb = new StringBuilder();
        makeOrderByString(sb, modelEntity, includeTablenamePrefix, datasourceInfo);
        return sb.toString();
    }

    /**
     * Make order by string.
     * @param sb the sb
     * @param modelEntity the model entity
     * @param includeTablenamePrefix the include tablename prefix
     * @param datasourceInfo the datasource info
     */
    public void makeOrderByString(StringBuilder sb, ModelEntity modelEntity, boolean includeTablenamePrefix, Datasource datasourceInfo) {
        if (!orderByList.isEmpty()) {
            sb.append(" ORDER BY ");
        }
        for (int i = 0; i < orderByList.size(); i++) {
            if (i != 0) {
                sb.append(", ");
            }
            OrderByItem orderByItem = orderByList.get(i);
            orderByItem.makeOrderByString(sb, modelEntity, includeTablenamePrefix, datasourceInfo);
        }
    }

    @Override
    public int compare(GenericEntity entity1, GenericEntity entity2) {
        int result = 0;
        for (OrderByItem orderByItem: orderByList) {
            result = orderByItem.compare(entity1, entity2);
            if (result != 0) {
                break;
            }
        }
        return result;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((orderByList == null) ? 0 : orderByList.hashCode());
        return result;
    }

    @Override
    public boolean equals(java.lang.Object obj) {
        if (!(obj instanceof OrderByList)) {
            return false;
        }
        OrderByList that = (OrderByList) obj;
        return orderByList.equals(that.orderByList);
    }

    @Override
    public String toString() {
        return makeOrderByString(null, false, null);
    }
}
