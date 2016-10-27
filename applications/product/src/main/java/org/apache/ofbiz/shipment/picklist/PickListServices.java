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
package org.apache.ofbiz.shipment.picklist;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.ServiceUtil;

public class PickListServices {

    public static final String module = PickListServices.class.getName();

    public static Map<String, Object> convertOrderIdListToHeaders(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();

        List<GenericValue> orderHeaderList = UtilGenerics.checkList(context.get("orderHeaderList"));
        List<String> orderIdList = UtilGenerics.checkList(context.get("orderIdList"));

        // we don't want to process if there is already a header list
        if (orderHeaderList == null) {
            // convert the ID list to headers
            if (orderIdList != null) {
                List<EntityCondition> conditionList1 = new LinkedList<EntityCondition>();
                List<EntityCondition> conditionList2 = new LinkedList<EntityCondition>();

                // we are only concerned about approved sales orders
                conditionList2.add(EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, "ORDER_APPROVED"));
                conditionList2.add(EntityCondition.makeCondition("orderTypeId", EntityOperator.EQUALS, "SALES_ORDER"));

                // build the expression list from the IDs
                for (String orderId: orderIdList) {
                    conditionList1.add(EntityCondition.makeCondition("orderId", EntityOperator.EQUALS, orderId));
                }

                // create the conditions
                EntityCondition idCond = EntityCondition.makeCondition(conditionList1, EntityOperator.OR);
                conditionList2.add(idCond);

                // run the query
                try {
                    orderHeaderList = EntityQuery.use(delegator).from("OrderHeader")
                            .where(conditionList2)
                            .orderBy("orderDate")
                            .queryList();
                } catch (GenericEntityException e) {
                    Debug.logError(e, module);
                    return ServiceUtil.returnError(e.getMessage());
                }
                Debug.logInfo("Recieved orderIdList  - " + orderIdList, module);
                Debug.logInfo("Found orderHeaderList - " + orderHeaderList, module);
            }
        }

        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put("orderHeaderList", orderHeaderList);
        return result;
    }

    public static boolean isBinComplete(Delegator delegator, String picklistBinId) throws GeneralException {
        // lookup the items in the bin
        List<GenericValue> items;
        try {
            items = EntityQuery.use(delegator).from("PicklistItem").where("picklistBinId", picklistBinId).queryList();
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            throw e;
        }

        if (UtilValidate.isNotEmpty(items)) {
            for (GenericValue v: items) {
                String itemStatus = v.getString("itemStatusId");
                if (itemStatus != null) {
                    if (!"PICKITEM_COMPLETED".equals(itemStatus)) {
                        return false;
                    }
                }
            }
            return true;
        }

        return false;
    }
}
