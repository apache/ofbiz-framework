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
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.ofbiz.entity.GenericEntity;
import org.ofbiz.entity.GenericModelException;
import org.ofbiz.entity.config.DatasourceInfo;
import org.ofbiz.entity.model.ModelEntity;

public class OrderByList implements Comparator {
    protected List orderByList = new ArrayList();

    public OrderByList() {
    }

    public OrderByList(Collection orderByList) {
        addOrderBy(orderByList);
    }
    
    public void addOrderBy(Collection orderByList) {
        Iterator it = orderByList.iterator();
        while (it.hasNext()) {
            addOrderBy(OrderByItem.parse(it.next()));
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
        for (int i = 0; i < orderByList.size(); i++) {
            OrderByItem orderByItem = (OrderByItem) orderByList.get(i);
            orderByItem.checkOrderBy(modelEntity);
        }
    }

    public String makeOrderByString(ModelEntity modelEntity, boolean includeTablenamePrefix, DatasourceInfo datasourceInfo) {
        StringBuffer sb = new StringBuffer();
        makeOrderByString(sb, modelEntity, includeTablenamePrefix, datasourceInfo);
        return sb.toString();
    }

    public void makeOrderByString(StringBuffer sb, ModelEntity modelEntity, boolean includeTablenamePrefix, DatasourceInfo datasourceInfo) {
        if (!orderByList.isEmpty()) {
            sb.append(" ORDER BY ");
        }
        for (int i = 0; i < orderByList.size(); i++) {
            if (i != 0) sb.append(", ");
            OrderByItem orderByItem = (OrderByItem) orderByList.get(i);
            orderByItem.makeOrderByString(sb, modelEntity, includeTablenamePrefix, datasourceInfo);
        }
    }

    public int compare(Object obj1, Object obj2) {
        return compare((GenericEntity) obj1, (GenericEntity) obj2);
    }

    public int compare(GenericEntity entity1, GenericEntity entity2) {
        int result = 0;
        for (int i = 0; i < orderByList.size() && result == 0; i++) {
            OrderByItem orderByItem = (OrderByItem) orderByList.get(i);
            result = orderByItem.compare(entity1, entity2);
        }
        return result;
    }

    public boolean equals(java.lang.Object obj) {
        if (!(obj instanceof OrderByList)) return false;
        OrderByList that = (OrderByList) obj;
        return orderByList.equals(that.orderByList);
    }

    public String toString() {
        return makeOrderByString(null, false, null);
    }
}
