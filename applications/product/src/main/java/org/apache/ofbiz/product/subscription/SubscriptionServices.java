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
package org.apache.ofbiz.product.subscription;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.common.uom.UomWorker;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.service.ServiceUtil;

import com.ibm.icu.util.Calendar;

/**
 * Subscription Services
 */
public class SubscriptionServices {

    public static final String module = SubscriptionServices.class.getName();
    public static final String resource = "ProductUiLabels";
    public static final String resourceError = "ProductErrorUiLabels";
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
            if (UtilValidate.isNotEmpty(inventoryItemId)) {
                subscriptionFindMap.put("inventoryItemId", inventoryItemId);
            }
            List<GenericValue> subscriptionList = EntityQuery.use(delegator).from("Subscription").where(subscriptionFindMap).queryList();
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
                updateSubscriptionMap.put("userLogin", EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "system").queryOne());

                Map<String, Object> updateSubscriptionResult = dispatcher.runSync("updateSubscription", updateSubscriptionMap);
                result.put("subscriptionId", updateSubscriptionMap.get("subscriptionId"));
                if (ServiceUtil.isError(updateSubscriptionResult)) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(resource,
                            "ProductSubscriptionUpdateError",
                            UtilMisc.toMap("subscriptionId", updateSubscriptionMap.get("subscriptionId")), locale),
                            null, null, updateSubscriptionResult);
                }
            } else {
                Map<String, Object> ensurePartyRoleMap = new HashMap<>();
                if (UtilValidate.isNotEmpty(roleTypeId)) {
                    ensurePartyRoleMap.put("partyId", partyId);
                    ensurePartyRoleMap.put("roleTypeId", roleTypeId);
                    ensurePartyRoleMap.put("userLogin", userLogin);
                    Map<String, Object> createPartyRoleResult = dispatcher.runSync("ensurePartyRole", ensurePartyRoleMap);
                    if (ServiceUtil.isError(createPartyRoleResult)) {
                        return ServiceUtil.returnError(UtilProperties.getMessage(resource,
                                "ProductSubscriptionPartyRoleCreationError",
                                UtilMisc.toMap("subscriptionResourceId", subscriptionResourceId), locale),
                                null, null, createPartyRoleResult);
                    }
                }
                Map<String, Object> createSubscriptionMap = dctx.getModelService("createSubscription").makeValid(newSubscription, ModelService.IN_PARAM);
                createSubscriptionMap.put("userLogin", EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "system").queryOne());

                Map<String, Object> createSubscriptionResult = dispatcher.runSync("createSubscription", createSubscriptionMap);
                if (ServiceUtil.isError(createSubscriptionResult)) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(resource,
                            "ProductSubscriptionCreateError",
                            UtilMisc.toMap("subscriptionResourceId", subscriptionResourceId), locale),
                            null, null, createSubscriptionResult);
                }
                result.put("subscriptionId", createSubscriptionResult.get("subscriptionId"));
            }
        } catch (GenericEntityException | GenericServiceException e) {
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
            List<GenericValue> productSubscriptionResourceList = EntityQuery.use(delegator).from("ProductSubscriptionResource")
                    .where("productId", productId)
                    .cache(true)
                    .filterByDate(orderCreatedDate, "fromDate", "thruDate", "purchaseFromDate", "purchaseThruDate")
                    .queryList();

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
                subContext.put("gracePeriodOnExpiry", productSubscriptionResource.get("gracePeriodOnExpiry"));
                subContext.put("gracePeriodOnExpiryUomId", productSubscriptionResource.get("gracePeriodOnExpiryUomId"));

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
            List<GenericValue> orderRoleList = EntityQuery.use(delegator).from("OrderRole").where("orderId", orderId, "roleTypeId", "END_USER_CUSTOMER").queryList();
            if (orderRoleList.size() > 0) {
                GenericValue orderRole = orderRoleList.get(0);
                String partyId = (String) orderRole.get("partyId");
                subContext.put("partyId", partyId);
            } else {
                return ServiceUtil.returnFailure(UtilProperties.getMessage(resourceOrderError,
                        "OrderErrorCannotGetOrderRoleEntity",
                        UtilMisc.toMap("itemMsgInfo", orderId), locale));
            }
            orderHeader = EntityQuery.use(delegator).from("OrderHeader").where("orderId", orderId).queryOne();
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
                List<GenericValue> productSubscriptionResourceListFiltered = EntityQuery.use(delegator).from("ProductSubscriptionResource").where("productId", productId).cache(true).filterByDate().queryList();
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

    public static Map<String, Object> runServiceOnSubscriptionExpiry( DispatchContext dctx, Map<String, ? extends Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        Locale locale =(Locale)context.get("locale");
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Map<String, Object> result = new HashMap<>();
        Map<String, Object> expiryMap = new HashMap<>();
        String gracePeriodOnExpiry = null;
        String gracePeriodOnExpiryUomId = null;
        String subscriptionId = null;
        Timestamp expirationCompletedDate = null;

        try {
            EntityCondition cond1 = EntityCondition.makeCondition("automaticExtend", EntityOperator.EQUALS, "N");
            EntityCondition cond2 = EntityCondition.makeCondition("automaticExtend", EntityOperator.EQUALS, null);
            EntityCondition cond = EntityCondition.makeCondition(UtilMisc.toList(cond1, cond2), EntityOperator.OR);
            List<GenericValue> subscriptionList = null;
            subscriptionList = EntityQuery.use(delegator).from("Subscription").where(cond).queryList();

            if (subscriptionList != null) {
                for (GenericValue subscription : subscriptionList) {
                	expirationCompletedDate = subscription.getTimestamp("expirationCompletedDate");
                	if (expirationCompletedDate == null) {
                		Calendar currentDate = Calendar.getInstance();
                        currentDate.setTime(UtilDateTime.nowTimestamp());
                        // check if the thruDate + grace period (if provided) is earlier than today's date
                        Calendar endDateSubscription = Calendar.getInstance();
                        int field = Calendar.MONTH;
                        String subscriptionResourceId = subscription.getString("subscriptionResourceId");
                        GenericValue subscriptionResource = null;
                        subscriptionResource = EntityQuery.use(delegator).from("SubscriptionResource").where("subscriptionResourceId", subscriptionResourceId).queryOne();
                        subscriptionId = subscription.getString("subscriptionId");
                        gracePeriodOnExpiry = subscription.getString("gracePeriodOnExpiry");
                        gracePeriodOnExpiryUomId = subscription.getString("gracePeriodOnExpiryUomId");
                        String serviceNameOnExpiry = subscriptionResource.getString("serviceNameOnExpiry");
                        endDateSubscription.setTime(subscription.getTimestamp("thruDate"));

                        if (gracePeriodOnExpiry != null && gracePeriodOnExpiryUomId != null) {
                            if ("TF_day".equals(gracePeriodOnExpiryUomId)) {
                                field = Calendar.DAY_OF_YEAR;
                            } else if ("TF_wk".equals(gracePeriodOnExpiryUomId)) {
                                field = Calendar.WEEK_OF_YEAR;
                            } else if ("TF_mon".equals(gracePeriodOnExpiryUomId)) {
                                field = Calendar.MONTH;
                            } else if ("TF_yr".equals(gracePeriodOnExpiryUomId)) {
                                field = Calendar.YEAR;
                            } else {
                                Debug.logWarning("Don't know anything about gracePeriodOnExpiryUomId [" + gracePeriodOnExpiryUomId + "], defaulting to month", module);
                            }
                            endDateSubscription.add(field, Integer.parseInt(gracePeriodOnExpiry));
                        }
                        if ((currentDate.after(endDateSubscription) || currentDate.equals(endDateSubscription)) && serviceNameOnExpiry != null) {
                            if (userLogin != null) {
                                expiryMap.put("userLogin", userLogin);
                            }
                            if (subscriptionId != null) {
                                expiryMap.put("subscriptionId", subscriptionId);
                            }
                            result = dispatcher.runSync(serviceNameOnExpiry, expiryMap);
                            if (ServiceUtil.isSuccess(result)) {
                                subscription.set("expirationCompletedDate", UtilDateTime.nowTimestamp());
                                delegator.store(subscription);
                                Debug.logInfo("Subscription expired successfully for subscription ID:" + subscriptionId, module);
                            } else if (ServiceUtil.isError(result)) {
                                result = null;
                                Debug.logError("Error expiring subscription while processing with subscriptionId: " + subscriptionId, module);
                            }

                            if (result != null && subscriptionId != null) {
                                Debug.logInfo("Service mentioned in serviceNameOnExpiry called with result: " + ServiceUtil.makeSuccessMessage(result, "", "", "", ""), module);
                            } else if (result == null && subscriptionId != null) {
                                Debug.logError("Subscription couldn't be expired for subscriptionId: " + subscriptionId, module);
                                return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, "ProductSubscriptionCouldntBeExpired", UtilMisc.toMap("subscriptionId", subscriptionId), locale));
                            }
                        }
                	}
                }
            }
        } catch (GenericServiceException e) {
            Debug.logError("Error while calling service specified in serviceNameOnExpiry", module);
            return ServiceUtil.returnError(e.toString());
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }

        return result;
    }

    public static Map<String, Object> runSubscriptionExpired(
            DispatchContext dctx, Map<String, ? extends Object> context) {
    	 Locale locale = (Locale)context.get("locale");
        String subscriptionId = (String) context.get("subscriptionId");
        Map<String, Object> result = new HashMap<>();
        if (subscriptionId != null) {
            return ServiceUtil.returnSuccess(UtilProperties.getMessage(resource, "ProductRunSubscriptionExpiredServiceCalledSuccessfully", locale));
        }
        return result;
    }
}
