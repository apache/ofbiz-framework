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

import java.math.BigDecimal;
import java.sql.Timestamp;
import com.ibm.icu.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javolution.util.FastMap;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ModelService;
import org.ofbiz.service.ServiceUtil;
import org.ofbiz.common.uom.UomWorker;

/**
 * Subscription Services
 */
public class SubscriptionServices {

    public static final String module = SubscriptionServices.class.getName();
    public static final String resource = "ProductUiLabels";
    public static final String resourceOrderError = "OrderErrorUiLabels";
    
    public static Map<String, Object> processExtendSubscription(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Timestamp nowTimestamp = UtilDateTime.nowTimestamp();

        String partyId = (String) context.get("partyId");
        String subscriptionResourceId = (String) context.get("subscriptionResourceId");
        String inventoryItemId = (String) context.get("inventoryItemId");
        String roleTypeId = (String) context.get("useRoleTypeId");
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Integer useTime = (Integer) context.get("useTime");
        String useTimeUomId = (String) context.get("useTimeUomId");
        String alwaysCreateNewRecordStr = (String) context.get("alwaysCreateNewRecord");
        Locale locale = (Locale) context.get("locale");
        boolean alwaysCreateNewRecord = !"N".equals(alwaysCreateNewRecordStr);

        GenericValue lastSubscription = null;
        try {
            Map<String, String> subscriptionFindMap = UtilMisc.toMap("partyId", partyId, "subscriptionResourceId", subscriptionResourceId);
            // if this subscription is attached to something the customer owns, filter by that too
            if (UtilValidate.isNotEmpty(inventoryItemId)) subscriptionFindMap.put("inventoryItemId", inventoryItemId);
            List<GenericValue> subscriptionList = delegator.findByAnd("Subscription", subscriptionFindMap, null, false);
            // DEJ20070718 DON'T filter by date, we want to consider all subscriptions: List listFiltered = EntityUtil.filterByDate(subscriptionList, true);
            List<GenericValue> listOrdered = EntityUtil.orderBy(subscriptionList, UtilMisc.toList("-fromDate"));
            if (listOrdered.size() > 0) {
                lastSubscription = listOrdered.get(0);
            }
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(e.toString());
        }

        GenericValue newSubscription = null;
        if (lastSubscription == null || alwaysCreateNewRecord) {
            newSubscription = delegator.makeValue("Subscription");
            newSubscription.set("subscriptionResourceId", subscriptionResourceId);
            newSubscription.set("partyId", partyId);
            newSubscription.set("roleTypeId", roleTypeId);
            newSubscription.set("productId", context.get("productId"));
            newSubscription.set("orderId", context.get("orderId"));
            newSubscription.set("orderItemSeqId", context.get("orderItemSeqId"));
            newSubscription.set("automaticExtend", context.get("automaticExtend"));
            newSubscription.set("canclAutmExtTimeUomId", context.get("canclAutmExtTimeUomId"));
            newSubscription.set("canclAutmExtTime", context.get("canclAutmExtTime"));
        } else {
            newSubscription = lastSubscription;
        }
        newSubscription.set("inventoryItemId", inventoryItemId);

        Timestamp thruDate = lastSubscription != null ? (Timestamp) lastSubscription.get("thruDate") : null;

        // set the fromDate, one way or another
        if (thruDate == null) {
            // no thruDate? start with NOW
            thruDate = nowTimestamp;
            newSubscription.set("fromDate", nowTimestamp);
        } else {
            // there is a thru date... if it is in the past, bring it up to NOW before adding on the time period
            // don't want to penalize for skipping time, in other words if they had a subscription last year for a
            // month and buy another month, we want that second month to start now and not last year
            if (thruDate.before(nowTimestamp)) {
                thruDate = nowTimestamp;
            }
            newSubscription.set("fromDate", thruDate);
        }

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(thruDate);
        int[] times = UomWorker.uomTimeToCalTime(useTimeUomId);
        if (times != null) {
            calendar.add(times[0], (useTime.intValue() * times[1]));
        } else {
            Debug.logWarning("Don't know anything about useTimeUomId [" + useTimeUomId + "], defaulting to month", module);
            calendar.add(Calendar.MONTH, useTime);
        }

        thruDate = new Timestamp(calendar.getTimeInMillis());
        newSubscription.set("thruDate", thruDate);

        Map<String, Object> result = ServiceUtil.returnSuccess();
        try {
            if (lastSubscription != null && !alwaysCreateNewRecord) {
                Map<String, Object> updateSubscriptionMap = dctx.getModelService("updateSubscription").makeValid(newSubscription, ModelService.IN_PARAM);
                updateSubscriptionMap.put("userLogin", delegator.findOne("UserLogin", UtilMisc.toMap("userLoginId", "system"), false));

                Map<String, Object> updateSubscriptionResult = dispatcher.runSync("updateSubscription", updateSubscriptionMap);
                result.put("subscriptionId", updateSubscriptionMap.get("subscriptionId"));
                if (ServiceUtil.isError(updateSubscriptionResult)) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                            "ProductSubscriptionUpdateError", 
                            UtilMisc.toMap("subscriptionId", updateSubscriptionMap.get("subscriptionId")), locale),
                            null, null, updateSubscriptionResult);
                }
            } else {
                Map<String, Object> createPartyRoleMap = FastMap.newInstance();
                if (UtilValidate.isNotEmpty(roleTypeId)) {
                    createPartyRoleMap.put("partyId", partyId);
                    createPartyRoleMap.put("roleTypeId", roleTypeId);
                    createPartyRoleMap.put("userLogin", userLogin);
                    Map<String, Object> createPartyRoleResult = dispatcher.runSync("createPartyRole", createPartyRoleMap);
                    if (ServiceUtil.isError(createPartyRoleResult)) {
                        return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                                "ProductSubscriptionPartyRoleCreationError", 
                                UtilMisc.toMap("subscriptionResourceId", subscriptionResourceId), locale),
                                null, null, createPartyRoleResult);
                    }
                }
                Map<String, Object> createSubscriptionMap = dctx.getModelService("createSubscription").makeValid(newSubscription, ModelService.IN_PARAM);
                createSubscriptionMap.put("userLogin", delegator.findOne("UserLogin", UtilMisc.toMap("userLoginId", "system"), false));

                Map<String, Object> createSubscriptionResult = dispatcher.runSync("createSubscription", createSubscriptionMap);
                if (ServiceUtil.isError(createSubscriptionResult)) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                            "ProductSubscriptionCreateError", 
                            UtilMisc.toMap("subscriptionResourceId", subscriptionResourceId), locale),
                            null, null, createSubscriptionResult);
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

    public static Map<String, Object> processExtendSubscriptionByProduct(DispatchContext dctx, Map<String, ? extends Object> context) throws GenericServiceException{
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        String productId = (String) context.get("productId");
        Integer qty = (Integer) context.get("quantity");
        Locale locale = (Locale) context.get("locale");
        if (qty == null) {
            qty = Integer.valueOf(1);
        }

        Timestamp orderCreatedDate = (Timestamp) context.get("orderCreatedDate");
        if (orderCreatedDate == null) {
            orderCreatedDate = UtilDateTime.nowTimestamp();
        }
        try {
            List<GenericValue> productSubscriptionResourceList = delegator.findByAnd("ProductSubscriptionResource", UtilMisc.toMap("productId", productId), null, true);
            productSubscriptionResourceList = EntityUtil.filterByDate(productSubscriptionResourceList, orderCreatedDate, null, null, true);
            productSubscriptionResourceList = EntityUtil.filterByDate(productSubscriptionResourceList, orderCreatedDate, "purchaseFromDate", "purchaseThruDate", true);

            if (productSubscriptionResourceList.size() == 0) {
                Debug.logError("No ProductSubscriptionResource found for productId: " + productId, module);
                return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                        "ProductSubscriptionResourceNotFound", 
                        UtilMisc.toMap("productId", productId), locale));
            }

            for (GenericValue productSubscriptionResource: productSubscriptionResourceList) {
                Long useTime = productSubscriptionResource.getLong("useTime");
                Integer newUseTime = Integer.valueOf(0);
                if (useTime != null) {
                    newUseTime = Integer.valueOf(useTime.intValue() * qty.intValue());
                }
                Map<String, Object> subContext = UtilMisc.makeMapWritable(context);
                subContext.put("useTime", newUseTime);
                subContext.put("useTimeUomId", productSubscriptionResource.get("useTimeUomId"));
                subContext.put("useRoleTypeId", productSubscriptionResource.get("useRoleTypeId"));
                subContext.put("subscriptionResourceId", productSubscriptionResource.get("subscriptionResourceId"));
                subContext.put("automaticExtend", productSubscriptionResource.get("automaticExtend"));
                subContext.put("canclAutmExtTime", productSubscriptionResource.get("canclAutmExtTime"));
                subContext.put("canclAutmExtTimeUomId", productSubscriptionResource.get("canclAutmExtTimeUomId"));

                Map<String, Object> ctx = dctx.getModelService("processExtendSubscription").makeValid(subContext, ModelService.IN_PARAM);
                Map<String, Object> processExtendSubscriptionResult = dispatcher.runSync("processExtendSubscription", ctx);
                if (ServiceUtil.isError(processExtendSubscriptionResult)) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                            "ProductSubscriptionByProductError", 
                            UtilMisc.toMap("productId", productId), locale),
                            null, null, processExtendSubscriptionResult);
                }
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, e.toString(), module);
            return ServiceUtil.returnError(e.toString());
        }

        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> processExtendSubscriptionByOrder(DispatchContext dctx, Map<String, ? extends Object> context) throws GenericServiceException{
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Map<String, Object> subContext = UtilMisc.makeMapWritable(context);
        String orderId = (String) context.get("orderId");
        Locale locale = (Locale) context.get("locale");

        Debug.logInfo("In processExtendSubscriptionByOrder service with orderId: " + orderId, module);

        GenericValue orderHeader = null;
        try {
            List<GenericValue> orderRoleList = delegator.findByAnd("OrderRole", UtilMisc.toMap("orderId", orderId, "roleTypeId", "END_USER_CUSTOMER"), null, false);
            if (orderRoleList.size() > 0) {
                GenericValue orderRole = orderRoleList.get(0);
                String partyId = (String) orderRole.get("partyId");
                subContext.put("partyId", partyId);
            } else {
                return ServiceUtil.returnFailure(UtilProperties.getMessage(resourceOrderError, 
                        "OrderErrorCannotGetOrderRoleEntity", 
                        UtilMisc.toMap("itemMsgInfo", orderId), locale));
            }
            orderHeader = delegator.findOne("OrderHeader", UtilMisc.toMap("orderId", orderId), false);
            if (orderHeader == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resourceOrderError, 
                        "OrderErrorNoValidOrderHeaderFoundForOrderId", 
                        UtilMisc.toMap("orderId", orderId), locale));
            }
            Timestamp orderCreatedDate = (Timestamp) orderHeader.get("orderDate");
            subContext.put("orderCreatedDate", orderCreatedDate);
            List<GenericValue> orderItemList = orderHeader.getRelated("OrderItem", null, null, false);
            for (GenericValue orderItem: orderItemList) {
                BigDecimal qty = orderItem.getBigDecimal("quantity");
                String productId = orderItem.getString("productId");
                if (UtilValidate.isEmpty(productId)) {
                    continue;
                }
                List<GenericValue> productSubscriptionResourceList = delegator.findByAnd("ProductSubscriptionResource", UtilMisc.toMap("productId", productId), null, true);
                List<GenericValue> productSubscriptionResourceListFiltered = EntityUtil.filterByDate(productSubscriptionResourceList, true);
                if (productSubscriptionResourceListFiltered.size() > 0) {
                    subContext.put("subscriptionTypeId", "PRODUCT_SUBSCR");
                    subContext.put("productId", productId);
                    subContext.put("orderId", orderId);
                    subContext.put("orderItemSeqId", orderItem.get("orderItemSeqId"));
                    subContext.put("inventoryItemId", orderItem.get("fromInventoryItemId"));
                    subContext.put("quantity", Integer.valueOf(qty.intValue()));
                    Map<String, Object> ctx = dctx.getModelService("processExtendSubscriptionByProduct").makeValid(subContext, ModelService.IN_PARAM);
                    Map<String, Object> thisResult = dispatcher.runSync("processExtendSubscriptionByProduct", ctx);
                    if (ServiceUtil.isError(thisResult)) {
                        return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                                "ProductSubscriptionByOrderError", 
                                UtilMisc.toMap("orderId", orderId), locale), null, null, thisResult);
                    }
                }
            }
        } catch (GenericEntityException e) {
            Debug.logError(e.toString(), module);
            return ServiceUtil.returnError(e.toString());
        }

        return ServiceUtil.returnSuccess();
    }
}
