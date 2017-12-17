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
    protected List<OrderByItem> orderByList = new ArrayList<>();

    public OrderByList() {
    }

    public OrderByList(String... orderByList) {
        addOrderBy(orderByList);
    }

    public OrderByList(Collection<String> orderByList) {
        addOrderBy(orderByList);
    }

    public void addOrderBy(String... orderByList) {
        for (String orderByItem: orderByList) {
            addOrderBy(orderByItem);
        }
    }

    public void addOrderBy(Collection<String> orderByList) {
        for (String orderByItem: orderByList) {
            addOrderBy(orderByItem);
        }
    }

    public void addOrderBy(String text) {
        addOrderBy(OrderByItem.parse(text));
    }

    public void addOrderBy(EntityConditionValue value) {
        addOrderBy(value, false);
    }

    public void addOrderBy(EntityConditionValue value, boolean descending) {
        addOrderBy(new OrderByItem(value, descending));
    }

    public void addOrderBy(OrderByItem orderByItem) {
        orderByList.add(orderByItem);
    }

    public void checkOrderBy(ModelEntity modelEntity) throws GenericModelException {
        for (OrderByItem orderByItem: orderByList) {
            orderByItem.checkOrderBy(modelEntity);
        }
    }

    public String makeOrderByString(ModelEntity modelEntity, boolean includeTablenamePrefix, Datasource datasourceInfo) {
        StringBuilder sb = new StringBuilder();
        makeOrderByString(sb, modelEntity, includeTablenamePrefix, datasourceInfo);
        return sb.toString();
    }

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
