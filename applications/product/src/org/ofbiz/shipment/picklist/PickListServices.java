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
package org.ofbiz.shipment.picklist;

import java.util.Map;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.ServiceUtil;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.condition.EntityConditionList;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilValidate;

public class PickListServices {

    public static final String module = PickListServices.class.getName();

    public static Map convertOrderIdListToHeaders(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();

        List orderHeaderList = (List) context.get("orderHeaderList");
        List orderIdList = (List) context.get("orderIdList");

        // we don't want to process if there is already a header list
        if (orderHeaderList == null) {
            // convert the ID list to headers
            if (orderIdList != null) {
                List conditionList1 = new ArrayList();
                List conditionList2 = new ArrayList();

                // we are only concerned about approved sales orders
                conditionList2.add(new EntityExpr("statusId", EntityOperator.EQUALS, "ORDER_APPROVED"));
                conditionList2.add(new EntityExpr("orderTypeId", EntityOperator.EQUALS, "SALES_ORDER"));

                // build the expression list from the IDs
                Iterator i = orderIdList.iterator();
                while (i.hasNext()) {
                    String orderId = (String) i.next();
                    conditionList1.add(new EntityExpr("orderId", EntityOperator.EQUALS, orderId));
                }

                // create the conditions
                EntityCondition idCond = new EntityConditionList(conditionList1, EntityOperator.OR);
                conditionList2.add(idCond);

                EntityCondition cond = new EntityConditionList(conditionList2, EntityOperator.AND);

                // run the query
                try {
                    orderHeaderList = delegator.findByCondition("OrderHeader", cond, null, UtilMisc.toList("+orderDate"));
                } catch (GenericEntityException e) {
                    Debug.logError(e, module);
                    return ServiceUtil.returnError(e.getMessage());
                }
                Debug.log("Recieved orderIdList  - " + orderIdList, module);
                Debug.log("Found orderHeaderList - " + orderHeaderList, module);
            }
        }

        Map result = ServiceUtil.returnSuccess();
        result.put("orderHeaderList", orderHeaderList);
        return result;
    }

    public static boolean isBinComplete(GenericDelegator delegator, String picklistBinId) throws GeneralException {
        // lookup the items in the bin
        List items;
        try {
            items = delegator.findByAnd("PicklistItem", UtilMisc.toMap("picklistBinId", picklistBinId));
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            throw e;
        }

        if (!UtilValidate.isEmpty(items)) {
            Iterator i = items.iterator();
            while (i.hasNext()) {
                GenericValue v = (GenericValue) i.next();
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
