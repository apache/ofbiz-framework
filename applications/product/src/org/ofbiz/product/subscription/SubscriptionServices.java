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
package org.ofbiz.product.subscription;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ModelService;
import org.ofbiz.service.ServiceUtil;

/**
 * Subscription Services
 */
public class SubscriptionServices {

    public static final String module = SubscriptionServices.class.getName();

    public static Map processExtendSubscription(DispatchContext dctx, Map context) throws GenericServiceException{
        GenericDelegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Timestamp nowTimestamp = UtilDateTime.nowTimestamp();
        
        String partyId = (String) context.get("partyId");
        String subscriptionResourceId = (String) context.get("subscriptionResourceId");
        String roleTypeId = (String) context.get("useRoleTypeId");
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Integer useTime = (Integer) context.get("useTime");
        String useTimeUomId = (String) context.get("useTimeUomId");
        String alwaysCreateNewRecordStr = (String) context.get("alwaysCreateNewRecord");
        boolean alwaysCreateNewRecord = !"N".equals(alwaysCreateNewRecordStr);
        
        GenericValue lastSubscription = null;
        try {
            List subscriptionList = delegator.findByAndCache("Subscription", UtilMisc.toMap("partyId", partyId, "subscriptionResourceId", subscriptionResourceId));
            List listFiltered = EntityUtil.filterByDate(subscriptionList, true);
            List listOrdered = EntityUtil.orderBy(listFiltered, UtilMisc.toList("-fromDate"));
            if (listOrdered.size() > 0) {
                lastSubscription = (GenericValue) listOrdered.get(0);
            }
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(e.toString());
        }

        GenericValue newSubscription = null;
        if (lastSubscription == null || alwaysCreateNewRecord) {
            newSubscription = delegator.makeValue("Subscription", null);
            newSubscription.set("subscriptionResourceId", subscriptionResourceId);
            newSubscription.set("partyId", partyId);
            newSubscription.set("roleTypeId", roleTypeId);
            newSubscription.set("productId", context.get("productId"));
            newSubscription.set("orderId", context.get("orderId"));
            newSubscription.set("orderItemSeqId", context.get("orderItemSeqId"));
        } else {
            newSubscription = lastSubscription;
        }
        
        Timestamp thruDate = lastSubscription != null ? (Timestamp) lastSubscription.get("thruDate") : null;
        if (thruDate == null) {
            // no thruDate? start with NOW
            thruDate = nowTimestamp;
            newSubscription.set("fromDate", nowTimestamp);
        } else {
            // there is a thru date... if it is in the past, bring it up to NOW before adding on the time period
            //don't want to penalize for skipping time, in other words if they had a subscription last year for a month and buy another month, we want that second month to start now and not last year
            if (thruDate.before(nowTimestamp)) {
                thruDate = nowTimestamp;
            } else {
                newSubscription.set("fromDate", thruDate);
            }
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(thruDate);
        int field = Calendar.MONTH;
        if ("TF_day".equals(useTimeUomId)) {
            field = Calendar.DAY_OF_YEAR;   
        } else if ("TF_wk".equals(useTimeUomId)) {
            field = Calendar.WEEK_OF_YEAR;   
        } else if ("TF_mon".equals(useTimeUomId)) {
            field = Calendar.MONTH;   
        } else if ("TF_yr".equals(useTimeUomId)) {
            field = Calendar.YEAR;   
        } else {
            Debug.logWarning("Don't know anything about useTimeUomId [" + useTimeUomId + "], defaulting to month", module);
        }
        calendar.add(field, useTime.intValue());
        thruDate = new Timestamp(calendar.getTimeInMillis());
        newSubscription.set("thruDate", thruDate);
        
        Map result = ServiceUtil.returnSuccess();
        try {
            if (lastSubscription != null && !alwaysCreateNewRecord) {
                Map updateSubscriptionMap = dctx.getModelService("updateSubscription").makeValid(newSubscription, ModelService.IN_PARAM);
                updateSubscriptionMap.put("userLogin", delegator.findByPrimaryKey("UserLogin", UtilMisc.toMap("userLoginId", "system")));

                Map updateSubscriptionResult = dispatcher.runSync("updateSubscription", updateSubscriptionMap);
                result.put("subscriptionId", updateSubscriptionMap.get("subscriptionId"));
                if (ServiceUtil.isError(updateSubscriptionResult)) {
                    return ServiceUtil.returnError("Error processing subscription update with ID [" + updateSubscriptionMap.get("subscriptionId") + "]", null, null, updateSubscriptionResult);
                }
            } else {
                Map createPartyRoleMap = new HashMap();
                if (UtilValidate.isNotEmpty(roleTypeId)) {
                    createPartyRoleMap.put("partyId", partyId);
                    createPartyRoleMap.put("roleTypeId", roleTypeId);
                    createPartyRoleMap.put("userLogin", userLogin);
                    Map createPartyRoleResult = dispatcher.runSync("createPartyRole", createPartyRoleMap);
                    if (ServiceUtil.isError(createPartyRoleResult)) {
                        return ServiceUtil.returnError("Error creating new PartyRole while processing subscription update with resource ID [" + subscriptionResourceId + "]", null, null, createPartyRoleResult);
                    }
                }
                Map createSubscriptionMap = dctx.getModelService("createSubscription").makeValid(newSubscription, ModelService.IN_PARAM);
                createSubscriptionMap.put("userLogin", delegator.findByPrimaryKey("UserLogin", UtilMisc.toMap("userLoginId", "system")));

                Map createSubscriptionResult = dispatcher.runSync("createSubscription", createSubscriptionMap);
                if (ServiceUtil.isError(createSubscriptionResult)) {
                    return ServiceUtil.returnError("Error creating subscription while processing with resource ID [" + subscriptionResourceId + "]", null, null, createSubscriptionResult);
                }
                result.put("subscriptionId", createSubscriptionResult.get("subscriptionId"));
            }
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(e.toString());
        } catch (GenericServiceException e) {
            return ServiceUtil.returnError(e.toString());
        }
        return result;
    }
    
    public static Map processExtendSubscriptionByProduct(DispatchContext dctx, Map context) throws GenericServiceException{
        GenericDelegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        String productId = (String) context.get("productId");
        Integer qty = (Integer) context.get("quantity");
        if (qty == null) {
            qty = new Integer(1);
        }
        
        Timestamp orderCreatedDate = (Timestamp) context.get("orderCreatedDate");
        if (orderCreatedDate == null) {
            orderCreatedDate = UtilDateTime.nowTimestamp();   
        }
        try {
            List productSubscriptionResourceList = delegator.findByAndCache("ProductSubscriptionResource", UtilMisc.toMap("productId", productId));
            productSubscriptionResourceList = EntityUtil.filterByDate(productSubscriptionResourceList, orderCreatedDate, null, null, true);
            productSubscriptionResourceList = EntityUtil.filterByDate(productSubscriptionResourceList, orderCreatedDate, "purchaseFromDate", "purchaseThruDate", true);

            if (productSubscriptionResourceList.size() == 0) {
                String msg = "No ProductSubscriptionResource found for productId: " + productId;
                Debug.logError(msg, module);
                return ServiceUtil.returnError(msg); 
            }

            Iterator productSubscriptionResourceIter = productSubscriptionResourceList.iterator();
            while (productSubscriptionResourceIter.hasNext()) {
                GenericValue productSubscriptionResource = (GenericValue) productSubscriptionResourceIter.next();

                Long useTime = (Long) productSubscriptionResource.get("useTime");
                Integer newUseTime = new Integer(0);
                if (useTime != null) {
                    newUseTime = new Integer(useTime.intValue() * qty.intValue());
                }
                context.put("useTime", newUseTime);
                context.put("useTimeUomId", productSubscriptionResource.get("useTimeUomId"));
                context.put("useRoleTypeId", productSubscriptionResource.get("useRoleTypeId"));
                context.put("subscriptionResourceId", productSubscriptionResource.get("subscriptionResourceId"));
                context.put("productId", productId);
                context.put("orderId", context.get("orderId"));
                
                Map ctx = dctx.getModelService("processExtendSubscription").makeValid(context, ModelService.IN_PARAM);
                Map processExtendSubscriptionResult = dispatcher.runSync("processExtendSubscription", ctx);
                if (ServiceUtil.isError(processExtendSubscriptionResult)) {
                    return ServiceUtil.returnError("Error processing subscriptions for Product with ID [" + productId + "]", null, null, processExtendSubscriptionResult);
                }
            }
        } catch(GenericEntityException e) {
            Debug.logError(e, e.toString(), module);
            return ServiceUtil.returnError(e.toString());
        }
        Map result = ServiceUtil.returnSuccess();
        return result;
    }
    
    public static Map processExtendSubscriptionByOrder(DispatchContext dctx, Map context) throws GenericServiceException{
        GenericDelegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        String orderId = (String) context.get("orderId");
        
        Debug.logInfo("In processExtendSubscriptionByOrder service with orderId: " + orderId, module);
        
        GenericValue orderHeader = null;
        try {
            List orderRoleList = delegator.findByAnd("OrderRole", UtilMisc.toMap("orderId", orderId, "roleTypeId", "END_USER_CUSTOMER"));
            if (orderRoleList.size() > 0 ) {
                GenericValue orderRole = (GenericValue)orderRoleList.get(0);
                String partyId = (String) orderRole.get("partyId");
                context.put("partyId", partyId);
            } else {
                String msg = "No OrderRole found for orderId:" + orderId;
                Debug.logError(msg, module);
                return ServiceUtil.returnError(msg); 
            }
            orderHeader = delegator.findByPrimaryKeyCache("OrderHeader", UtilMisc.toMap("orderId", orderId));
            if (orderHeader == null) {
                String msg = "No OrderHeader found for orderId:" + orderId;
                Debug.logError(msg, module);
                return ServiceUtil.returnError(msg); 
            }
            Timestamp orderCreatedDate = (Timestamp) orderHeader.get("orderDate");
            context.put("orderCreatedDate", orderCreatedDate);
            List orderItemList = orderHeader.getRelated("OrderItem");
            Iterator orderItemIter = orderItemList.iterator();
            while (orderItemIter.hasNext()) {
                GenericValue orderItem = (GenericValue)orderItemIter.next();   
                Double qty = (Double) orderItem.get("quantity");
                String productId = (String) orderItem.get("productId");
                if (UtilValidate.isEmpty(productId)) {
                    continue;
                }
                List productSubscriptionResourceList = delegator.findByAndCache("ProductSubscriptionResource", UtilMisc.toMap("productId", productId));
                List productSubscriptionResourceListFiltered = EntityUtil.filterByDate(productSubscriptionResourceList, true);
                if (productSubscriptionResourceListFiltered.size() > 0) {
                    context.put("productId", productId);
                    context.put("orderId", orderId);
                    context.put("orderItemSeqId", orderItem.get("orderItemSeqId"));
                    context.put("quantity", new Integer(qty.intValue()));
                    Map ctx = dctx.getModelService("processExtendSubscriptionByProduct").makeValid(context, ModelService.IN_PARAM);
                    Map thisResult = dispatcher.runSync("processExtendSubscriptionByProduct", ctx);
                    if (ServiceUtil.isError(thisResult)) {
                        return ServiceUtil.returnError("Error processing subscriptions for Order with ID [" + orderId + "]", null, null, thisResult);
                    }
                }
            }
        } catch(GenericEntityException e) {
            Debug.logError(e.toString(), module);
            return ServiceUtil.returnError(e.toString());
        }
        Map result = ServiceUtil.returnSuccess();
        return result;
    }
}
