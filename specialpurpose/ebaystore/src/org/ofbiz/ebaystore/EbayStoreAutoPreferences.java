/*
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
 */
package org.ofbiz.ebaystore;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilGenerics;
import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityQuery;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ModelService;
import org.ofbiz.service.ServiceUtil;

import com.ebay.sdk.ApiContext;
import com.ebay.sdk.ApiException;
import com.ebay.sdk.SdkException;
import com.ebay.sdk.SdkSoapException;
import com.ebay.sdk.call.AddDisputeCall;
import com.ebay.sdk.call.AddItemCall;
import com.ebay.sdk.call.AddOrderCall;
import com.ebay.sdk.call.DeleteSellingManagerTemplateCall;
import com.ebay.sdk.call.GetBestOffersCall;
import com.ebay.sdk.call.GetItemCall;
import com.ebay.sdk.call.GetMyeBaySellingCall;
import com.ebay.sdk.call.GetSellingManagerInventoryCall;
import com.ebay.sdk.call.GetSellingManagerSoldListingsCall;
import com.ebay.sdk.call.GetUserCall;
import com.ebay.sdk.call.LeaveFeedbackCall;
import com.ebay.sdk.call.RelistItemCall;
import com.ebay.sdk.call.RespondToBestOfferCall;
import com.ebay.sdk.call.VerifyAddSecondChanceItemCall;
import com.ebay.soap.eBLBaseComponents.AddOrderRequestType;
import com.ebay.soap.eBLBaseComponents.AddOrderResponseType;
import com.ebay.soap.eBLBaseComponents.AmountType;
import com.ebay.soap.eBLBaseComponents.AutomatedLeaveFeedbackEventCodeType;
import com.ebay.soap.eBLBaseComponents.BestOfferActionCodeType;
import com.ebay.soap.eBLBaseComponents.BestOfferDetailsType;
import com.ebay.soap.eBLBaseComponents.BestOfferStatusCodeType;
import com.ebay.soap.eBLBaseComponents.BestOfferType;
import com.ebay.soap.eBLBaseComponents.BuyerPaymentMethodCodeType;
import com.ebay.soap.eBLBaseComponents.CommentTypeCodeType;
import com.ebay.soap.eBLBaseComponents.CurrencyCodeType;
import com.ebay.soap.eBLBaseComponents.DeleteSellingManagerTemplateRequestType;
import com.ebay.soap.eBLBaseComponents.DeleteSellingManagerTemplateResponseType;
import com.ebay.soap.eBLBaseComponents.DetailLevelCodeType;
import com.ebay.soap.eBLBaseComponents.DisputeExplanationCodeType;
import com.ebay.soap.eBLBaseComponents.DisputeReasonCodeType;
import com.ebay.soap.eBLBaseComponents.FeedbackDetailType;
import com.ebay.soap.eBLBaseComponents.GetSellingManagerInventoryRequestType;
import com.ebay.soap.eBLBaseComponents.GetSellingManagerInventoryResponseType;
import com.ebay.soap.eBLBaseComponents.ItemArrayType;
import com.ebay.soap.eBLBaseComponents.ItemListCustomizationType;
import com.ebay.soap.eBLBaseComponents.ItemSortTypeCodeType;
import com.ebay.soap.eBLBaseComponents.ItemType;
import com.ebay.soap.eBLBaseComponents.ListingTypeCodeType;
import com.ebay.soap.eBLBaseComponents.OrderType;
import com.ebay.soap.eBLBaseComponents.PaginatedItemArrayType;
import com.ebay.soap.eBLBaseComponents.PaginationType;
import com.ebay.soap.eBLBaseComponents.SellingManagerOrderStatusType;
import com.ebay.soap.eBLBaseComponents.SellingManagerPaidStatusCodeType;
import com.ebay.soap.eBLBaseComponents.SellingManagerProductDetailsType;
import com.ebay.soap.eBLBaseComponents.SellingManagerProductType;
import com.ebay.soap.eBLBaseComponents.SellingManagerShippedStatusCodeType;
import com.ebay.soap.eBLBaseComponents.SellingManagerSoldOrderType;
import com.ebay.soap.eBLBaseComponents.SellingManagerSoldTransactionType;
import com.ebay.soap.eBLBaseComponents.SellingManagerTemplateDetailsArrayType;
import com.ebay.soap.eBLBaseComponents.SellingManagerTemplateDetailsType;
import com.ebay.soap.eBLBaseComponents.TradingRoleCodeType;
import com.ebay.soap.eBLBaseComponents.TransactionArrayType;
import com.ebay.soap.eBLBaseComponents.TransactionType;
import com.ebay.soap.eBLBaseComponents.UserType;

public class EbayStoreAutoPreferences {
    public static String module = EbayStoreAutoPreferences.class.getName();
    private static final String resource = "EbayStoreUiLabels";

    public EbayStoreAutoPreferences() {

    }

    /*  It may take several minutes to process your automated feedback.  to connect to ebay site*/
    public static Map<String, Object> autoPrefLeaveFeedbackOption(DispatchContext dctx, Map<String, ? extends Object> context) throws ApiException, SdkException, Exception {

        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get("locale");

        if (UtilValidate.isEmpty(context.get("productStoreId")) && UtilValidate.isEmpty(context.get("jobId"))) {
            return ServiceUtil.returnFailure(UtilProperties.getMessage(resource, "EbayStoreRequiredProductStoreId", locale));
        }
        String jobId = (String) context.get("jobId");
        String productStoreId = (String) context.get("productStoreId");
        String isAutoPositiveFeedback = "N";
        String feedbackEventCode = null;
        GenericValue ebayProductStorePref = null;
        List<String> list = new LinkedList<String>();

        try {
            ApiContext apiContext = EbayStoreHelper.getApiContext(productStoreId, locale, delegator);
            ebayProductStorePref = EntityQuery.use(delegator).from("EbayProductStorePref").where("productStoreId", productStoreId, "autoPrefEnumId", "EBAY_AUTO_PIT_FB").queryOne();
            if (UtilValidate.isNotEmpty(ebayProductStorePref) && UtilValidate.isNotEmpty(ebayProductStorePref.getString("autoPrefJobId"))) {
                isAutoPositiveFeedback = ebayProductStorePref.getString("enabled");
                // if isAutoPositiveFeedback is N that means not start this job run service
                if ("Y".equals(isAutoPositiveFeedback) && jobId.equals(ebayProductStorePref.getString("autoPrefJobId"))) {
                    feedbackEventCode = ebayProductStorePref.getString("condition1");
                    String storeComments = ebayProductStorePref.getString("condition2");
                    String comment = null;
                    if (UtilValidate.isNotEmpty(storeComments)) {
                        if (storeComments.indexOf("\\[,\\]") != -1) {
                            String[] strs = storeComments.split("\\[,\\]");
                            for (String str : strs) {
                                list.add(str);
                            }
                        }
                    }
                    // start getting sold item list from ebay follow your site
                    GetSellingManagerSoldListingsCall sellingManagerSoldListings = new GetSellingManagerSoldListingsCall(apiContext);

                    List<SellingManagerSoldOrderType> items = new LinkedList<SellingManagerSoldOrderType>();
                    SellingManagerSoldOrderType[] sellingManagerSoldOrders = sellingManagerSoldListings.getSellingManagerSoldListings();
                    if (UtilValidate.isNotEmpty(sellingManagerSoldOrders)) {
                        for (SellingManagerSoldOrderType solditem : sellingManagerSoldOrders) {
                            SellingManagerOrderStatusType orderStatus = solditem.getOrderStatus();
                            if (orderStatus != null && !orderStatus.isFeedbackSent()) {
                                SellingManagerPaidStatusCodeType paidStatus = orderStatus.getPaidStatus();
                                CommentTypeCodeType commentType = orderStatus.getFeedbackReceived();
                                //Buyer has paid for this item.
                                if ("PAYMENT_RECEIVED".equals(feedbackEventCode) && SellingManagerPaidStatusCodeType.PAID.equals(paidStatus)) {
                                    items.add(solditem);
                                }
                                //Buyer has paid for this item and left me positive feedback.
                                if ("POSITIVE_FEEDBACK_RECEIVED".equals(feedbackEventCode) && CommentTypeCodeType.POSITIVE.equals(commentType) && SellingManagerPaidStatusCodeType.PAID.equals(paidStatus)) {
                                    items.add(solditem);
                                }
                            }
                        }
                        GetUserCall getUserCall = new GetUserCall(apiContext);
                        String commentingUser = getUserCall.getUser().getUserID();
                        for (SellingManagerSoldOrderType item : items) {
                            // start leave feedbacks
                            SellingManagerSoldTransactionType[] soldTrans = item.getSellingManagerSoldTransaction();
                            if (UtilValidate.isNotEmpty(soldTrans)) {
                                for (SellingManagerSoldTransactionType soldTran : soldTrans) {
                                    LeaveFeedbackCall leaveFeedbackCall = new LeaveFeedbackCall(apiContext);
                                    FeedbackDetailType detail = new FeedbackDetailType();
                                    // ramdom comments
                                    if (list.size() > 0) {
                                        Collections.shuffle(list, new Random());
                                        comment = list.get(0);
                                    }
                                    detail.setCommentText(comment);
                                    detail.setCommentingUser(commentingUser);
                                    //detail.setCommentingUserScore(value);
                                    detail.setCommentType(CommentTypeCodeType.POSITIVE);
                                    detail.setItemID(soldTran.getItemID());
                                    detail.setItemPrice(soldTran.getItemPrice());
                                    detail.setItemTitle(soldTran.getItemTitle());
                                    leaveFeedbackCall.setFeedbackDetail(detail);
                                    leaveFeedbackCall.setTargetUser(item.getBuyerID());
                                    leaveFeedbackCall.setTransactionID(String.valueOf(soldTran.getTransactionID()));
                                    leaveFeedbackCall.leaveFeedback();
                                    Debug.logInfo("Auto leave feedback with site ".concat(apiContext.getSite().value()).concat("itemId ".concat(soldTran.getItemID())).concat(" comment is ".concat(comment)), module);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            return ServiceUtil.returnFailure(UtilProperties.getMessage(resource, "EbayStoreProblemConnectingToEbaySite", locale) + e);
        }

        return ServiceUtil.returnSuccess();
    }

    public static String autoPrefLeaveFeedbackOptions(HttpServletRequest request, HttpServletResponse response) {
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        Locale locale = UtilHttp.getLocale(request);
        Map<String, Object> paramMap = UtilHttp.getCombinedMap(request);

        if (UtilValidate.isEmpty(paramMap.get("productStoreId"))) {
            request.setAttribute("_ERROR_MESSAGE_", "Required productStoreId for get api context to connect with ebay site.");
            return "error";
        }

        String productStoreId = (String) paramMap.get("productStoreId");
        String isAutoPositiveFeedback = "N";
        String condition = null;
        if (UtilValidate.isNotEmpty(paramMap.get("isAutoPositiveFeedback"))) {
            isAutoPositiveFeedback = (String) paramMap.get("isAutoPositiveFeedback");
        }
        String feedbackEventCode = (String) paramMap.get("feedbackEventCode");
        ApiContext apiContext = EbayStoreHelper.getApiContext(productStoreId, locale, delegator);

        try {
            GenericValue ebayProductStorePref = null;
            String comments = null;
            String autoPrefJobId = null;
            GenericValue userLogin = EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "system").queryOne();
            if ("Y".equals(isAutoPositiveFeedback)) {
                if ("PAYMENT_RECEIVED".equals(feedbackEventCode)) {
                    condition = AutomatedLeaveFeedbackEventCodeType.PAYMENT_RECEIVED.toString();
                } else if ("POSITIVE_FEEDBACK_RECEIVED".equals(feedbackEventCode)) {
                    condition = AutomatedLeaveFeedbackEventCodeType.POSITIVE_FEEDBACK_RECEIVED.toString();
                }
                // allow only 10 comment can be store / set new comments to condition2 separate by [,]
            }
            for (int i = 1; i <= 5; i++) {
                String comment = (String) paramMap.get("comment_".concat(String.valueOf(i)));
                if (comment != null && comment.length() > 0) {
                    if (comments == null) {
                        comments = comment;
                    } else {
                        comments = comments.concat("[").concat(",").concat(("]").concat(comment));
                    }
                }
            }
            if (UtilValidate.isEmpty(comments)) {
                request.setAttribute("_ERROR_MESSAGE_", "Required least one at comment for your store feedback send with ebay site.");
                return "error";
            }

            Map<String, Object> context = UtilMisc.<String, Object>toMap("userLogin", userLogin, "serviceName", "autoPrefLeaveFeedbackOption");
            ebayProductStorePref = EntityQuery.use(delegator).from("EbayProductStorePref").where("productStoreId", productStoreId, "autoPrefEnumId", "EBAY_AUTO_PIT_FB").queryOne();
            context.put("productStoreId", productStoreId);
            context.put("autoPrefEnumId", "EBAY_AUTO_PIT_FB");
            if (UtilValidate.isNotEmpty(ebayProductStorePref) && UtilValidate.isNotEmpty(ebayProductStorePref.getString("autoPrefJobId"))) {
                autoPrefJobId = ebayProductStorePref.getString("autoPrefJobId");
            }
            context.put("autoPrefJobId", autoPrefJobId);
            context.put("enabled", isAutoPositiveFeedback);
            context.put("condition1", condition);
            context.put("condition2", comments);
            context.put("condition3", null);
            if (UtilValidate.isEmpty(ebayProductStorePref)) {
                dispatcher.runSync("createEbayProductStorePref", context);
            } else {
                dispatcher.runSync("updateEbayProductStorePref", context);
            }
            request.setAttribute("_EVENT_MESSAGE_", "Setting Automated Positive Feedback for Buyers Success with site " + apiContext.getSite().value());

        } catch (GenericEntityException e) {
            request.setAttribute("_ERROR_MESSAGE_", e.getMessage());
            return "error";
        } catch (GenericServiceException e) {
            request.setAttribute("_ERROR_MESSAGE_", e.getMessage());
            return "error";
        }

        return "success";
    }

    /* start automatically service send a Feedback Reminder email if feedback has not been received. and check how many days after shipping you want this email sent? */
    public static Map<String, Object> autoSendFeedbackReminderEmail(DispatchContext dctx, Map<String, ? extends Object> context) throws ApiException, SdkException, Exception {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Locale locale = (Locale) context.get("locale");
        GenericValue userLogin = EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "system").queryOne();
        if (UtilValidate.isEmpty(context.get("productStoreId")) && UtilValidate.isEmpty(context.get("jobId"))) {
            return ServiceUtil.returnFailure(UtilProperties.getMessage(resource, "EbayStoreRequiredProductStoreId", locale));
        }
        String jobId = (String) context.get("jobId");
        String productStoreId = (String) context.get("productStoreId");
        String isAutoFeedbackReminder = "N";
        int afterDays = 0;
        GenericValue ebayProductStorePref = null;
        String dateTimeFormat = UtilDateTime.DATE_TIME_FORMAT;
        SimpleDateFormat formatter = new SimpleDateFormat(dateTimeFormat);

        try {
            ApiContext apiContext = EbayStoreHelper.getApiContext(productStoreId, locale, delegator);
            ebayProductStorePref = EntityQuery.use(delegator).from("EbayProductStorePref").where("productStoreId", productStoreId, "autoPrefEnumId", "EBAY_AUTO_FB_RMD").queryOne();
            if (UtilValidate.isNotEmpty(ebayProductStorePref) && UtilValidate.isNotEmpty(ebayProductStorePref.getString("autoPrefJobId"))) {
                isAutoFeedbackReminder = ebayProductStorePref.getString("enabled");
                // if isAutoPositiveFeedback is N that means not start this job run service
                if ("Y".equals(isAutoFeedbackReminder) && jobId.equals(ebayProductStorePref.getString("autoPrefJobId"))) {
                    afterDays = Integer.parseInt(ebayProductStorePref.getString("condition1"));
                    
                    // start getting sold item list from ebay follow your site
                    GetSellingManagerSoldListingsCall sellingManagerSoldListings = new GetSellingManagerSoldListingsCall(apiContext);
                    List<SellingManagerSoldOrderType> items = new LinkedList<SellingManagerSoldOrderType>();
                    SellingManagerSoldOrderType[] sellingManagerSoldOrders = sellingManagerSoldListings.getSellingManagerSoldListings();
                    if (UtilValidate.isNotEmpty(sellingManagerSoldOrders)) {
                        for (SellingManagerSoldOrderType solditem : sellingManagerSoldOrders) {
                            SellingManagerOrderStatusType orderStatus = solditem.getOrderStatus();
                            if (orderStatus != null) {
                                SellingManagerPaidStatusCodeType paidStatus = orderStatus.getPaidStatus();
                                SellingManagerShippedStatusCodeType shippedStatus = orderStatus.getShippedStatus();

                                //Buyer has paid for this item.  && Seller shipped items but feedback has not been received from buyer more than days condition
                                if (SellingManagerPaidStatusCodeType.PAID.equals(paidStatus) && SellingManagerShippedStatusCodeType.SHIPPED.equals(shippedStatus)) {
                                    Calendar right_now = Calendar.getInstance();
                                    Calendar shippedTime = orderStatus.getShippedTime();
                                    Calendar afterShippedTime = orderStatus.getShippedTime();
                                    afterShippedTime.add(Calendar.DAY_OF_MONTH, afterDays);
                                    Debug.logInfo("Verify date for send reminder feedback eamil by auto service: buyer " + solditem.getBuyerID() + " seller shippedTime " +
                                            "" + formatter.format(shippedTime) + " codition days " + afterDays + " after shippedTime :" + formatter.format(afterShippedTime) + " now date" + formatter.format(right_now), module);
                                    // if now date is after shipped time follow after days condition would be send reminder email to buyer
                                    if (right_now.after(afterShippedTime)) {
                                        items.add(solditem);
                                    }
                                }
                            }
                        }

                        // call service send email (get template follow productStoreId)
                        for (SellingManagerSoldOrderType item : items) {
                            // call send
                            Map<String, Object> sendMap = new HashMap<String, Object>();
                            GenericValue productStoreEmail = EntityQuery.use(delegator).from("ProductStoreEmailSetting").where("productStoreId", productStoreId, "emailType", "EBAY_FEEBACK_REMIN").queryOne();
                            String bodyScreenLocation = productStoreEmail.getString("bodyScreenLocation");
                            sendMap.put("bodyScreenUri", bodyScreenLocation);
                            String subjectString = productStoreEmail.getString("subject");
                            sendMap.put("userLogin", userLogin);
                            sendMap.put("subject", subjectString);
                            sendMap.put("contentType", productStoreEmail.get("contentType"));
                            sendMap.put("sendFrom", productStoreEmail.get("fromAddress"));
                            sendMap.put("sendCc", productStoreEmail.get("ccAddress"));
                            sendMap.put("sendBcc", productStoreEmail.get("bccAddress"));
                            sendMap.put("sendTo", item.getBuyerEmail());

                            Map<String, Object> bodyParameters = new HashMap<String, Object>();
                            bodyParameters.put("buyerUserId", item.getBuyerID());
                            sendMap.put("bodyParameters", bodyParameters);

                            try {
                                dispatcher.runAsync("sendMailFromScreen", sendMap);
                            } catch (Exception e) {
                                Debug.logError(e, module);
                                return ServiceUtil.returnError(e.getMessage());
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            return ServiceUtil.returnFailure(UtilProperties.getMessage(resource, "EbayStoreProblemConnectingToEbaySite", locale) + e);
        }

        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> automaticEbayRelistSoldItems(DispatchContext dctx, Map<String, ? extends Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get("locale");
        String jobId = (String) context.get("jobId");
        try {
            GenericValue userLogin = EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "system").queryOne();
            Map<String, Object> serviceMap = new HashMap<String, Object>();
            serviceMap.put("userLogin", userLogin);
            //ProductStore
            List<GenericValue> productStores = EntityQuery.use(delegator).from("EbayProductStorePref").where("autoPrefJobId", jobId).queryList();
            if (productStores.size() != 0) {
                // get auto preference setting
                String productStoreId = productStores.get(0).getString("productStoreId");
                String condition1 = productStores.get(0).getString("condition1");
                String condition2 = productStores.get(0).getString("condition2");
                // convert preference setting
                Timestamp fromDate = UtilDateTime.toTimestamp(condition1);
                Timestamp thruDate = UtilDateTime.toTimestamp(condition2);
                Timestamp nowTime = UtilDateTime.nowTimestamp();
                if (nowTime.after(fromDate) && nowTime.before(thruDate)) {
                    serviceMap.put("productStoreId", productStoreId);
                    Map<String, Object> eBayUserLogin = dispatcher.runSync("getEbayStoreUser", serviceMap);
                    String eBayUserLoginId = (String) eBayUserLogin.get("userLoginId");
                    GenericValue party = EntityQuery.use(delegator).from("UserLogin").where("userLoginId", eBayUserLoginId).queryOne();
                    String partyId = party.getString("partyId");
                    //save sold items to OFbBiz product entity
                    Map<String, Object> resultService = dispatcher.runSync("getEbaySoldItems", serviceMap);
                    List<Map<String, Object>> soldItems = UtilGenerics.checkList(resultService.get("soldItems"));
                    if (soldItems.size() != 0) {
                        for (int itemCount = 0; itemCount < soldItems.size(); itemCount++) {
                            Map<String, Object> soldItemMap = soldItems.get(itemCount);
                            if (UtilValidate.isNotEmpty(soldItemMap.get("itemId"))) {
                                GenericValue productCheck = EntityQuery.use(delegator).from("Product").where("productId", soldItemMap.get("itemId")).queryOne();
                                if (productCheck == null) {
                                    Map<String, Object> inMap = new HashMap<String, Object>();
                                    inMap.put("productId", soldItemMap.get("itemId"));
                                    inMap.put("productTypeId", "EBAY_ITEM");
                                    inMap.put("internalName", "eBay Item " + soldItemMap.get("title"));
                                    inMap.put("userLogin", userLogin);
                                    dispatcher.runSync("createProduct", inMap);
                                    // ProductRole (VENDOR)
                                    List<GenericValue> productRole = EntityQuery.use(delegator).from("ProductRole").where("partyId", partyId, "productId", soldItemMap.get("itemId"), "roleTypeId", "VENDOR").queryList();
                                    if (productRole.size() == 0) {
                                        Map<String, Object> addRole = new HashMap<String, Object>();
                                        addRole.put("productId", soldItemMap.get("itemId"));
                                        addRole.put("roleTypeId", "VENDOR");
                                        addRole.put("partyId", partyId);
                                        addRole.put("fromDate", UtilDateTime.nowTimestamp());
                                        addRole.put("userLogin", userLogin);
                                        dispatcher.runSync("addPartyToProduct", addRole);
                                    }
                                }
                            }
                        }
                    }
                    //check active items
                    serviceMap = new HashMap<String, Object>();
                    serviceMap.put("userLogin", userLogin);
                    serviceMap.put("productStoreId", productStoreId);
                    resultService = dispatcher.runSync("getEbayActiveItems", serviceMap);
                    List<Map<String, Object>> activeItems = UtilGenerics.checkList(resultService.get("activeItems"));
                    List<String> activeItemMaps = new LinkedList<String>();
                    if (activeItems.size() != 0) {
                        for (int itemCount = 0; itemCount < activeItems.size(); itemCount++) {
                            Map<String, Object> activeItemMap = UtilGenerics.checkMap(activeItems.get(itemCount));
                            if (UtilValidate.isNotEmpty(activeItemMap.get("itemId"))) {
                                activeItemMaps.add((String) activeItemMap.get("itemId"));
                            }
                        }
                    }
                    //check product role
                    List<GenericValue> productRoles = EntityQuery.use(delegator).from("ProductRole").where("partyId", partyId, "roleTypeId", "VENDOR").queryList();
                    List<String> productRoleIds = new LinkedList<String>();
                    if (productRoles.size() != 0) {
                        for (int itemCount = 0; itemCount < productRoles.size(); itemCount++) {
                            String productId = productRoles.get(itemCount).getString("productId");
                            productRoleIds.add(productId);
                        }
                    }
                    List<EntityCondition> andExpr = new LinkedList<EntityCondition>();
                    EntityCondition activeItemCond = EntityCondition.makeCondition("productId", EntityOperator.NOT_IN, activeItemMaps);
                    andExpr.add(activeItemCond);
                    EntityCondition productTypeCond = EntityCondition.makeCondition("productTypeId", EntityOperator.EQUALS, "EBAY_ITEM");
                    andExpr.add(productTypeCond);
                    EntityCondition isVirtualCond = EntityCondition.makeCondition("isVirtual", EntityOperator.NOT_EQUAL, "Y");
                    andExpr.add(isVirtualCond);
                    EntityCondition productRole = EntityCondition.makeCondition("productId", EntityOperator.IN, productRoleIds);
                    andExpr.add(productRole);
                    List<GenericValue> itemsToRelist = EntityQuery.use(delegator).from("Product").where(andExpr).queryList();
                    if (itemsToRelist.size() != 0) {
                        //re-list sold items and not active
                        ApiContext apiContext = EbayStoreHelper.getApiContext(productStoreId, locale, delegator);
                        for (int itemRelist = 0; itemRelist < itemsToRelist.size(); itemRelist++) {
                            RelistItemCall relistItemCall = new RelistItemCall(apiContext);
                            ItemType itemToBeRelisted = new ItemType();
                            GenericValue product = itemsToRelist.get(itemRelist);
                            itemToBeRelisted.setItemID(product.getString("productId"));
                            relistItemCall.setItemToBeRelisted(itemToBeRelisted);
                            relistItemCall.relistItem();
                            GenericValue productStore = EntityQuery.use(delegator).from("Product").where("productId", product.getString("productId")).queryOne();
                            productStore.set("isVirtual", "Y");
                            productStore.store();
                            Debug.logInfo("Relisted Item - " + product.getString("productId"), module);
                        }
                    }
                }
            }
        } catch (Exception e) {
            return ServiceUtil.returnError(e.getMessage());
        }
        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> automaticEbayDisputeNotComplete(DispatchContext dctx, Map<String, ? extends Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get("locale");
        String jobId = (String) context.get("jobId");
        try {
            GenericValue userLogin = EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "system").queryOne();
            List<GenericValue> productStores = EntityQuery.use(delegator).from("EbayProductStorePref").where("autoPrefJobId", jobId).queryList();
            if (productStores.size() != 0) {
                // get automatic setting
                String productStoreId = productStores.get(0).getString("productStoreId");
                String condition1 = productStores.get(0).getString("condition1");
                String condition2 = productStores.get(0).getString("condition2");
                String condition3 = productStores.get(0).getString("condition3");
                // convert automatic setting for usage
                int afterDays = 0;
                if (UtilValidate.isInteger(condition1)) {
                    afterDays = Integer.parseInt(condition1);
                }
                DisputeReasonCodeType disputeReason = null;
                if (UtilValidate.isNotEmpty(condition2)) {
                    disputeReason = DisputeReasonCodeType.valueOf(condition2);
                }
                DisputeExplanationCodeType disputeExplanation = null;
                if (UtilValidate.isNotEmpty(condition3)) {
                    disputeExplanation = DisputeExplanationCodeType.valueOf(condition3);
                }
                // get sold items
                Map<String, Object> serviceMap = new HashMap<String, Object>();
                serviceMap.put("productStoreId", productStoreId);
                serviceMap.put("userLogin", userLogin);
                Map<String, Object> resultService = dispatcher.runSync("getEbaySoldItems", serviceMap);
                List<Map<String, Object>> soldItems = UtilGenerics.checkList(resultService.get("soldItems"));
                // check items to dispute
                List<Map<String, Object>> itemsToDispute = new LinkedList<Map<String,Object>>();
                for (int itemCount = 0; itemCount < soldItems.size(); itemCount++) {
                    Map<String, Object> item = UtilGenerics.checkMap(soldItems.get(itemCount));
                    String checkoutStatus = (String) item.get("checkoutStatus");
                    Date creationTime = (Date) item.get("creationTime");
                    Date paidTime = (Date) item.get("paidTime");
                    String unpaidItemStatus = (String) item.get("unpaidItemStatus");
                    int checkDays = UtilDateTime.getIntervalInDays(UtilDateTime.toTimestamp(creationTime), UtilDateTime.nowTimestamp());
                    if (checkDays > afterDays && "CheckoutIncomplete".equals(checkoutStatus) && unpaidItemStatus == null && paidTime == null && checkoutStatus != "CheckoutComplete") {
                        itemsToDispute.add(item);
                    }
                }
                // Dispute items
                if (disputeReason != null && disputeExplanation != null && itemsToDispute.size() != 0) {
                    ApiContext apiContext = EbayStoreHelper.getApiContext(productStoreId, locale, delegator);
                    DetailLevelCodeType[] detailLevels = new DetailLevelCodeType[]{
                            DetailLevelCodeType.RETURN_ALL,
                            DetailLevelCodeType.ITEM_RETURN_ATTRIBUTES,
                            DetailLevelCodeType.ITEM_RETURN_DESCRIPTION
                    };
                    for (int count = 0; count < itemsToDispute.size(); count++) {
                        Map<String, Object> item = itemsToDispute.get(count);
                        AddDisputeCall api = new AddDisputeCall(apiContext);
                        api.setDetailLevel(detailLevels);
                        api.setItemID((String) item.get("itemId"));
                        api.setTransactionID((String) item.get("transactionId"));
                        api.setDisputeExplanation(disputeExplanation);
                        api.setDisputeReason(disputeReason);
                    }
                }
            }
        } catch (Exception e) {
            return ServiceUtil.returnError(e.getMessage());
        }
        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> automaticEbayDisputeNotPay(DispatchContext dctx, Map<String, ? extends Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get("locale");
        String jobId = (String) context.get("jobId");
        try {
            GenericValue userLogin = EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "system").queryOne();
            List<GenericValue> productStores = EntityQuery.use(delegator).from("EbayProductStorePref").where("autoPrefJobId", jobId).queryList();
            if (productStores.size() != 0) {
                // get automatic setting
                String productStoreId = productStores.get(0).getString("productStoreId");
                String condition1 = productStores.get(0).getString("condition1");
                String condition2 = productStores.get(0).getString("condition2");
                String condition3 = productStores.get(0).getString("condition3");
                // convert automatic setting for usage
                int afterDays = 0;
                if (UtilValidate.isInteger(condition1)) {
                    afterDays = Integer.parseInt(condition1);
                }
                DisputeReasonCodeType disputeReason = null;
                if (UtilValidate.isNotEmpty(condition2)) {
                    disputeReason = DisputeReasonCodeType.valueOf(condition2);
                }
                DisputeExplanationCodeType disputeExplanation = null;
                if (UtilValidate.isNotEmpty(condition3)) {
                    disputeExplanation = DisputeExplanationCodeType.valueOf(condition3);
                }
                // get sold items
                Map<String, Object> serviceMap = new HashMap<String, Object>();
                serviceMap.put("productStoreId", productStoreId);
                serviceMap.put("userLogin", userLogin);
                Map<String, Object> resultService = dispatcher.runSync("getEbaySoldItems", serviceMap);
                List<Map<String, Object>> soldItems = UtilGenerics.checkList(resultService.get("soldItems"));
                // check items to dispute
                List<Map<String, Object>> itemsToDispute = new LinkedList<Map<String,Object>>();
                for (int itemCount = 0; itemCount < soldItems.size(); itemCount++) {
                    Map<String, Object> item = UtilGenerics.checkMap(soldItems.get(itemCount));
                    String checkoutStatus = (String) item.get("checkoutStatus");
                    Date creationTime = (Date) item.get("creationTime");
                    Date paidTime = (Date) item.get("paidTime");
                    String unpaidItemStatus = (String) item.get("unpaidItemStatus");
                    int checkDays = UtilDateTime.getIntervalInDays(UtilDateTime.toTimestamp(creationTime), UtilDateTime.nowTimestamp());
                    if (checkDays > afterDays && unpaidItemStatus == null && paidTime == null && checkoutStatus == "CheckoutComplete") {
                        itemsToDispute.add(item);
                    }
                }
                // Dispute items
                if (disputeReason != null && disputeExplanation != null && itemsToDispute.size() != 0) {
                    ApiContext apiContext = EbayStoreHelper.getApiContext(productStoreId, locale, delegator);
                    DetailLevelCodeType[] detailLevels = new DetailLevelCodeType[]{
                            DetailLevelCodeType.RETURN_ALL,
                            DetailLevelCodeType.ITEM_RETURN_ATTRIBUTES,
                            DetailLevelCodeType.ITEM_RETURN_DESCRIPTION
                    };
                    for (int count = 0; count < itemsToDispute.size(); count++) {
                        Map<String, Object> item = itemsToDispute.get(count);
                        AddDisputeCall api = new AddDisputeCall(apiContext);
                        api.setDetailLevel(detailLevels);
                        api.setItemID((String) item.get("itemId"));
                        api.setTransactionID((String) item.get("transactionId"));
                        api.setDisputeExplanation(disputeExplanation);
                        api.setDisputeReason(disputeReason);
                    }
                }
            }
        } catch (Exception e) {
            return ServiceUtil.returnError(e.getMessage());
        }
        return ServiceUtil.returnSuccess();
    }

    /* start automatically service send an email when ebay seller has been received payment from ebay buyer */
    public static Map<String, Object> autoSendPaymentReceivedEmail(DispatchContext dctx, Map<String, ? extends Object> context) throws ApiException, SdkException, Exception {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Locale locale = (Locale) context.get("locale");
        GenericValue userLogin = EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "system").queryOne();

        if (UtilValidate.isEmpty(context.get("productStoreId")) && UtilValidate.isEmpty(context.get("jobId"))) {
            return ServiceUtil.returnFailure(UtilProperties.getMessage(resource, "EbayStoreRequiredProductStoreId", locale));
        }

        String jobId = (String) context.get("jobId");
        String productStoreId = (String) context.get("productStoreId");

        String isAutoSendEmail = "N";
        GenericValue ebayProductStorePref = null;

        try {
            ApiContext apiContext = EbayStoreHelper.getApiContext(productStoreId, locale, delegator);
            ebayProductStorePref = EntityQuery.use(delegator).from("EbayProductStorePref").where("productStoreId", productStoreId, "autoPrefEnumId", "EBAY_AUTO_FB_RMD").queryOne();
            if (UtilValidate.isNotEmpty(ebayProductStorePref) && UtilValidate.isNotEmpty(ebayProductStorePref.getString("autoPrefJobId"))) {
                isAutoSendEmail = ebayProductStorePref.getString("enabled");
                // if isAutoPositiveFeedback is N that means not start this job run service
                if ("Y".equals(isAutoSendEmail) && jobId.equals(ebayProductStorePref.getString("autoPrefJobId"))) {
                    // start getting sold item list from ebay follow your site
                    GetSellingManagerSoldListingsCall sellingManagerSoldListings = new GetSellingManagerSoldListingsCall(apiContext);
                    List<SellingManagerSoldOrderType> items = new LinkedList<SellingManagerSoldOrderType>();
                    SellingManagerSoldOrderType[] sellingManagerSoldOrders = sellingManagerSoldListings.getSellingManagerSoldListings();
                    if (UtilValidate.isNotEmpty(sellingManagerSoldOrders)) {
                        for (SellingManagerSoldOrderType solditem : sellingManagerSoldOrders) {
                            SellingManagerOrderStatusType orderStatus = solditem.getOrderStatus();
                            if (orderStatus != null) {
                                SellingManagerPaidStatusCodeType paidStatus = orderStatus.getPaidStatus();
                                //Buyer has paid for this item. and seller received
                                if (SellingManagerPaidStatusCodeType.PAID.equals(paidStatus)) {
                                    items.add(solditem);
                                }
                            }
                        }

                        // call service send email (get template follow productStoreId)
                        for (SellingManagerSoldOrderType item : items) {
                            // call send
                            Map<String, Object> sendMap = new HashMap<String, Object>();
                            GenericValue productStoreEmail = EntityQuery.use(delegator).from("ProductStoreEmailSetting").where("productStoreId", productStoreId, "emailType", "EBAY_PAY_RECIEVED").queryOne();
                            String bodyScreenLocation = productStoreEmail.getString("bodyScreenLocation");
                            sendMap.put("bodyScreenUri", bodyScreenLocation);
                            String subjectString = productStoreEmail.getString("subject");
                            sendMap.put("userLogin", userLogin);
                            sendMap.put("subject", subjectString);
                            sendMap.put("contentType", productStoreEmail.get("contentType"));
                            sendMap.put("sendFrom", productStoreEmail.get("fromAddress"));
                            sendMap.put("sendCc", productStoreEmail.get("ccAddress"));
                            sendMap.put("sendBcc", productStoreEmail.get("bccAddress"));
                            sendMap.put("sendTo", item.getBuyerEmail());

                            Map<String, Object> bodyParameters = new HashMap<String, Object>();
                            bodyParameters.put("buyerUserId", item.getBuyerID());
                            sendMap.put("bodyParameters", bodyParameters);

                            try {
                                dispatcher.runAsync("sendMailFromScreen", sendMap);
                            } catch (Exception e) {
                                Debug.logError(e, module);
                                return ServiceUtil.returnError(e.getMessage());
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            return ServiceUtil.returnFailure(UtilProperties.getMessage(resource, "EbayStoreProblemConnectingToEbaySite", locale) + e);
        }

        return ServiceUtil.returnSuccess();
    }


    public static Map<String, Object> runCombineOrders(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> result = new HashMap<String, Object>();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get("locale");
        String productStoreId = (String) context.get("productStoreId");
        try {
            GenericValue userLogin = EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "system").queryOne();
            ApiContext apiContext = EbayStoreHelper.getApiContext(productStoreId, locale, delegator);
            GetSellingManagerSoldListingsCall sellingManagerSoldListings = new GetSellingManagerSoldListingsCall(apiContext);
            SellingManagerSoldOrderType[] sellingManagerSoldOrders = sellingManagerSoldListings.getSellingManagerSoldListings();
            if (sellingManagerSoldOrders != null) {
                int soldOrderLength = sellingManagerSoldOrders.length;
                GenericValue ebayPref = EntityQuery.use(delegator).from("EbayProductStorePref").where("productStoreId", productStoreId, "autoPrefEnumId", "ENA_COMB_ORD").queryOne();
                if (UtilValidate.isNotEmpty(ebayPref)) {

                    Timestamp beginDate = UtilDateTime.toTimestamp("01/01/2001 00:00:00");
                    Long daysCount = Long.parseLong(ebayPref.get("condition1").toString());
                    Hashtable<String, List<Map<String, Object>>> h = new Hashtable<String, List<Map<String, Object>>>();

                    for (int i = 0; i < soldOrderLength; i++) {
                        SellingManagerSoldOrderType sellingManagerSoldOrder = sellingManagerSoldOrders[i];
                        String buyerId = sellingManagerSoldOrder.getBuyerID().toString();
                        List<Map<String, Object>> soldGroupList = new LinkedList<Map<String,Object>>();
                        Map<String, Object> mymap = new HashMap<String, Object>();
                        mymap.put("group", "");
                        mymap.put("soldorder", sellingManagerSoldOrder);
                        if (h.size() > 0) {
                            Enumeration<String> enums = h.keys();
                            String key = "";
                            while (enums.hasMoreElements()) {
                                key = enums.nextElement();
                                List<Map<String, Object>> tempList = h.get(key);
                                if (key.equals(buyerId)) {
                                    key = buyerId;
                                    tempList.add(mymap);
                                    h.put(buyerId, tempList);
                                }
                            }
                            if (!key.equals(buyerId)) {
                                soldGroupList.clear();
                                soldGroupList.add(mymap);
                                h.put(buyerId, soldGroupList);
                            }
                        } else {
                            soldGroupList.add(mymap);
                            h.put(buyerId, soldGroupList);
                        }
                    }

                    Enumeration<String> enums = h.keys();
                    while (enums.hasMoreElements()) {
                        int groupRunning = 0;
                        String key = enums.nextElement();
                        List<Map<String, Object>> soldGroupList = h.get(key);
                        int maxItems = Integer.parseInt(ebayPref.get("condition2").toString());

                        if (soldGroupList.size() > 1) {
                            for (int j = 0; j < soldGroupList.size(); j++) {
                                Map<String, Object> myMap = soldGroupList.get(j);
                                SellingManagerSoldOrderType soldorder = (SellingManagerSoldOrderType) myMap.get("soldorder");
                                Timestamp createdate = UtilDateTime.toTimestamp(soldorder.getCreationTime().getTime());
                                if (myMap.get("group").toString().length() == 0) {
                                    beginDate = createdate;
                                }
                                beginDate = findStartDate(beginDate, soldGroupList);
                                runCheckAndGroup(groupRunning, beginDate, daysCount, soldGroupList);
                                groupRunning++;
                            }

                            int x = 0;
                            while (x < groupRunning) {
                                OrderType order = new OrderType();
                                order.setCreatingUserRole(TradingRoleCodeType.SELLER);
                                BuyerPaymentMethodCodeType[] buyerPayment = new BuyerPaymentMethodCodeType[1];
                                buyerPayment[0] = BuyerPaymentMethodCodeType.CASH_ON_PICKUP;
                                order.setPaymentMethods(buyerPayment);
                                TransactionArrayType transactionArr = new TransactionArrayType();
                                List<TransactionType> translist = new LinkedList<TransactionType>();

                                AmountType total = new AmountType();
                                double totalAmt = 0.0;
                                CurrencyCodeType currencyId = null;
                                int totalQty = 0;

                                for (int j = 0; j < soldGroupList.size(); j++) {
                                    Map<String, Object> myMap = soldGroupList.get(j);
                                    if (UtilValidate.isNotEmpty(myMap.get("group"))) {
                                        if (x == Integer.parseInt(myMap.get("group").toString())) {
                                            SellingManagerSoldOrderType sellingManagerSoldOrder = (SellingManagerSoldOrderType) myMap.get("soldorder");
                                            String buyerId = sellingManagerSoldOrder.getBuyerID().toString();
                                            int qty = sellingManagerSoldOrder.getTotalQuantity();
                                            totalQty = totalQty + qty;
                                            if (key.equals(buyerId) && (UtilValidate.isEmpty(sellingManagerSoldOrder.getOrderStatus().getPaidTime()))) {
                                                double totalAmount = 0.0;
                                                if (UtilValidate.isNotEmpty(sellingManagerSoldOrder.getTotalAmount())) {
                                                    totalAmount = sellingManagerSoldOrder.getTotalAmount().getValue();
                                                    currencyId = sellingManagerSoldOrder.getTotalAmount().getCurrencyID();
                                                } else {
                                                    totalAmount = sellingManagerSoldOrder.getSalePrice().getValue();
                                                    currencyId = sellingManagerSoldOrder.getSalePrice().getCurrencyID();
                                                }
                                                //Combine
                                                totalAmt = totalAmt + totalAmount;
                                                SellingManagerSoldTransactionType[] sellingManagerSoldTransactions = sellingManagerSoldOrder.getSellingManagerSoldTransaction();
                                                //set transaction
                                                for (int count = 0; count < sellingManagerSoldTransactions.length; count++) {
                                                    SellingManagerSoldTransactionType sellingManagerSoldTransaction = sellingManagerSoldTransactions[count];
                                                    TransactionType transtype = new TransactionType();
                                                    ItemType itemtype = new ItemType();
                                                    if (UtilValidate.isNotEmpty(sellingManagerSoldTransaction.getItemID())) {
                                                        itemtype.setItemID(sellingManagerSoldTransaction.getItemID());
                                                        transtype.setItem(itemtype);
                                                        transtype.setTransactionID(sellingManagerSoldTransaction.getTransactionID().toString());
                                                        translist.add(transtype);
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                if (totalQty < maxItems) {
                                    total.setValue(totalAmt);
                                    total.setCurrencyID(currencyId);
                                    TransactionType[] transArr = new TransactionType[translist.size()];
                                    for (int counter = 0; counter < translist.size(); counter++) {
                                        transArr[counter] = translist.get(counter);
                                    }
                                    transactionArr.setTransaction(transArr);
                                    if (transactionArr.getTransactionLength() > 1) {
                                        order.setTotal(total);
                                        order.setTransactionArray(transactionArr);
                                        AddOrderCall call = new AddOrderCall(apiContext);
                                        AddOrderRequestType req = new AddOrderRequestType();
                                        AddOrderResponseType resp = null;
                                        req.setOrder(order);
                                        resp = (AddOrderResponseType) call.execute(req);
                                        if (resp != null && "SUCCESS".equals(resp.getAck().toString())) {
                                            String orderId = resp.getOrderID();
                                            Debug.logInfo(":: new order id is = " + orderId, module);
                                        } else {
                                            EbayStoreHelper.createErrorLogMessage(userLogin, dispatcher, productStoreId, resp.getAck().toString(), "Add order : runCombineOrders", resp.getErrors(0).getLongMessage());
                                        }
                                    }
                                }
                                x++;
                            }
                        }
                    }
                }
            }
            result = ServiceUtil.returnSuccess();
        } catch (GenericServiceException e) {
            result = ServiceUtil.returnError(e.getMessage());
        } catch (Exception e) {
            return ServiceUtil.returnError(e.getMessage());
        }
        return result;
    }

    public static Timestamp findStartDate(Timestamp lessStartTimestamp, List<Map<String, Object>> inList) {
        for (Map<String, Object> inMap : inList) {
            SellingManagerSoldOrderType soldorder = (SellingManagerSoldOrderType) inMap.get("soldorder");
            Timestamp createTimestamp = UtilDateTime.toTimestamp(soldorder.getCreationTime().getTime());
            String group = (String) inMap.get("group");
            if (createTimestamp.before(lessStartTimestamp) && group.length() == 0) {
                lessStartTimestamp = createTimestamp;
            }
        }
        return lessStartTimestamp;
    }

    public static void runCheckAndGroup(int groupRunning, Timestamp startTimestamp, long countDays, List<Map<String, Object>> inList) {
        Timestamp endDate = UtilDateTime.getDayEnd(UtilDateTime.toTimestamp(startTimestamp), countDays);
        for (Map<String, Object> inMap : inList) {
            String group = inMap.get("group").toString();
            SellingManagerSoldOrderType soldorder = (SellingManagerSoldOrderType) inMap.get("soldorder");
            if (group.length() == 0) {
                Timestamp createtimestamp = UtilDateTime.toTimestamp(soldorder.getCreationTime().getTime());
                if (((createtimestamp.equals(startTimestamp)) || (createtimestamp.after(startTimestamp))) && (createtimestamp.before(endDate))) {
                    inMap.put("group", "" + groupRunning);
                }
            }
        }
    }

    public static Map<String, Object> autoSendWinningBuyerNotification(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> result = new HashMap<String, Object>();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get("locale");
        String productStoreId = (String) context.get("productStoreId");
        try {
            GenericValue userLogin = EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "system").queryOne();
            Map<String, Object> resultSold =  dispatcher.runSync("getEbaySoldItems", UtilMisc.toMap("productStoreId", productStoreId, "userLogin", userLogin));
            List<Map<String, Object>> soldItems = UtilGenerics.checkList(resultSold.get("soldItems"));
            if (soldItems.size() != 0) {
                for (int i = 0; i < soldItems.size(); i++) {
                    Map<String, Object> item = soldItems.get(i);
                    Timestamp lastestTime = UtilDateTime.getDayStart(UtilDateTime.nowTimestamp(), 1);
                    Date creationDate = (Date) item.get("creationTime");
                    Timestamp creationTime = UtilDateTime.toTimestamp(creationDate);

                    if (creationTime.equals(lastestTime) && (item.get("listingType").toString().equals("Chinese"))) {
                        Map<String, Object> serviceMap = new HashMap<String, Object>();
                        serviceMap.put("userLogin", userLogin);
                        serviceMap.put("locale", locale);
                        serviceMap.put("productStoreId", productStoreId);
                        serviceMap.put("itemId", item.get("itemId").toString());
                        Map<String, Object> resultBid =  dispatcher.runSync("getEbayAllBidders", serviceMap);
                        List<Map<String, Object>> allBidders =  UtilGenerics.checkList(resultBid.get("allBidders"));

                        if (allBidders.size() != 0) {
                            // call to send email to bidder
                            for (int j = 0; j < allBidders.size(); j++) {
                                Map<String, Object> bidder = allBidders.get(j);
                                UserType user = (UserType) bidder.get("bidder");
                                String buyerUserId = bidder.get("userId").toString();

                                Map<String, Object> sendMap = new HashMap<String, Object>();
                                GenericValue productStoreEmail = EntityQuery.use(delegator).from("ProductStoreEmailSetting").where("productStoreId", productStoreId, "emailType", "EBAY_WIN_BUYER_NOTI").queryOne();
                                String bodyScreenLocation = productStoreEmail.getString("bodyScreenLocation");
                                sendMap.put("bodyScreenUri", bodyScreenLocation);
                                String subjectString = productStoreEmail.getString("subject");
                                sendMap.put("userLogin", userLogin);
                                sendMap.put("subject", subjectString);
                                sendMap.put("contentType", productStoreEmail.get("contentType"));
                                sendMap.put("sendFrom", productStoreEmail.get("fromAddress"));
                                sendMap.put("sendCc", productStoreEmail.get("ccAddress"));
                                sendMap.put("sendBcc", productStoreEmail.get("bccAddress"));
                                sendMap.put("sendTo", user.getEmail());

                                Map<String, Object> bodyParameters = new HashMap<String, Object>();
                                bodyParameters.put("buyerUserId", buyerUserId);
                                sendMap.put("bodyParameters", bodyParameters);

                                try {
                                    dispatcher.runAsync("sendMailFromScreen", sendMap);
                                } catch (Exception e) {
                                    Debug.logError(e, module);
                                    return ServiceUtil.returnError(e.getMessage());
                                }
                            }
                        }
                    }
                }
            }
            result = ServiceUtil.returnSuccess();
        } catch (GenericServiceException e) {
            result = ServiceUtil.returnError(e.getMessage());
        } catch (Exception e) {
            return ServiceUtil.returnError(e.getMessage());
        }
        return result;
    }

    public static Map<String, Object> autoSendItemDispatchedNotification(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> result = new HashMap<String, Object>();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        String productStoreId = (String) context.get("productStoreId");

        try {
            GenericValue userLogin = EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "system").queryOne();
            context.put("userLogin", userLogin);
            Map<String, Object> resultSold =  dispatcher.runSync("getEbaySoldItems", context);
            List<Map<String, Object>> soldItems = UtilGenerics.checkList(resultSold.get("soldItems"));
            if (soldItems.size() != 0) {
                for (int i = 0; i < soldItems.size(); i++) {
                    Map<String, Object> item = soldItems.get(i);
                    String shippedStatus = item.get("shippedStatus").toString();
                    Timestamp lastestTime = UtilDateTime.getDayStart(UtilDateTime.nowTimestamp(), 1);
                    Date shippedTime = (Date) item.get("shippedTime");
                    Timestamp shippedTimestamp = UtilDateTime.toTimestamp(shippedTime);

                    if ("SHIPPED".equals(shippedStatus) && shippedTimestamp.equals(lastestTime)) {
                        String buyerUserId = item.get("buyerUserId").toString();
                        String buyerEmail = item.get("buyerEmail").toString();

                         Map<String, Object> sendMap = new HashMap<String, Object>();
                         GenericValue productStoreEmail = EntityQuery.use(delegator).from("ProductStoreEmailSetting").where("productStoreId", productStoreId, "emailType", "EBAY_ITEM_DISPATCH").queryOne();
                         String bodyScreenLocation = productStoreEmail.getString("bodyScreenLocation");
                         sendMap.put("bodyScreenUri", bodyScreenLocation);
                         String subjectString = productStoreEmail.getString("subject");
                         sendMap.put("userLogin", userLogin);
                         sendMap.put("subject", subjectString);
                         sendMap.put("contentType", productStoreEmail.get("contentType"));
                         sendMap.put("sendFrom", productStoreEmail.get("fromAddress"));
                         sendMap.put("sendCc", productStoreEmail.get("ccAddress"));
                         sendMap.put("sendBcc", productStoreEmail.get("bccAddress"));
                         sendMap.put("sendTo", buyerEmail);

                         Map<String, Object> bodyParameters = new HashMap<String, Object>();
                         bodyParameters.put("buyerUserId", buyerUserId);
                         sendMap.put("bodyParameters", bodyParameters);

                         try {
                             dispatcher.runAsync("sendMailFromScreen", sendMap);
                         } catch (Exception e) {
                             Debug.logError(e, module);
                             return ServiceUtil.returnError(e.getMessage());
                         }
                        
                    }
                }
            }
            result = ServiceUtil.returnSuccess();
        } catch (GenericServiceException e) {
            result = ServiceUtil.returnError(e.getMessage());
        } catch (Exception e) {
            return ServiceUtil.returnError(e.getMessage());
        }
        return result;
    }

    public static Map<String,Object> autoBlockItemsOutOfStock(DispatchContext dctx, Map<String,Object> context) {
        Locale locale = (Locale) context.get("locale");
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Delegator delegator = dctx.getDelegator();
        Map<String,Object> result = new HashMap<String, Object>();
        GetSellingManagerInventoryRequestType req = new GetSellingManagerInventoryRequestType();
        GetSellingManagerInventoryResponseType resp =  null;

        if (context.get("productStoreId") != null) {
            GetSellingManagerInventoryCall call = new GetSellingManagerInventoryCall(EbayStoreHelper.getApiContext((String)context.get("productStoreId"), locale, delegator));

            try {
                SellingManagerProductType[] returnedSellingManagerProductType = null;
                resp = (GetSellingManagerInventoryResponseType)call.execute(req);
                if (resp != null && "SUCCESS".equals(resp.getAck().toString())) {
                    returnedSellingManagerProductType  = resp.getSellingManagerProduct();
                    for (int i = 0; i < returnedSellingManagerProductType.length; i++) {
                       SellingManagerProductDetailsType prodDetailType = returnedSellingManagerProductType[i].getSellingManagerProductDetails();
                       int qty = prodDetailType.getQuantityAvailable();

                       if (qty == 0) {
                           SellingManagerTemplateDetailsArrayType sellingTempArr =  returnedSellingManagerProductType[i].getSellingManagerTemplateDetailsArray();
                           SellingManagerTemplateDetailsType[] selllingTempType = null;
                           if (UtilValidate.isNotEmpty(sellingTempArr)) {
                               selllingTempType = sellingTempArr.getSellingManagerTemplateDetails();
                           }

                           if (selllingTempType.length > 0) {
                               for (int j = 0; j < selllingTempType.length; j++) {
                                   Long longTemplete = Long.parseLong(selllingTempType[j].getSaleTemplateID());
                                   DeleteSellingManagerTemplateCall tcall = new DeleteSellingManagerTemplateCall(EbayStoreHelper.getApiContext((String)context.get("productStoreId"), locale, delegator));
                                   DeleteSellingManagerTemplateRequestType treq = new DeleteSellingManagerTemplateRequestType();
                                   DeleteSellingManagerTemplateResponseType tresp =  null;
                                   treq.setSaleTemplateID(longTemplete);

                                   tresp = (DeleteSellingManagerTemplateResponseType) tcall.execute(treq);
                                   if (tresp != null && "SUCCESS".equals(tresp.getAck().toString())) {
                                      result = ServiceUtil.returnSuccess();
                                   } else {
                                       EbayStoreHelper.createErrorLogMessage(userLogin, dctx.getDispatcher(), context.get("productStoreId").toString(), tresp.getAck().toString(), "Delete selling manager template : autoBlockItemsOutOfStock", tresp.getErrors(0).getLongMessage());
                                   }
                               }
                           }
                       }
                    }
                    result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
                } else {
                    EbayStoreHelper.createErrorLogMessage(userLogin, dctx.getDispatcher(), context.get("productStoreId").toString(), resp.getAck().toString(), "Get selling manager inventory : autoBlockItemsOutOfStock", resp.getErrors(0).getLongMessage());
                }
                result = ServiceUtil.returnSuccess();
            } catch (ApiException e) {
                e.printStackTrace();
            } catch (SdkSoapException e) {
                e.printStackTrace();
            } catch (SdkException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    public static Map<String, Object> autoRelistingItems(DispatchContext dctx, Map<String, ? extends Object> context) {
        Map<String, Object> itemObject = new HashMap<String, Object>();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get("locale");
        try {
            GenericValue userLogin = EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "system").queryOne();
            EntityCondition expression1 = EntityCondition.makeCondition("autoRelisting", EntityOperator.EQUALS, "Y");
            EntityCondition expression2 = EntityCondition.makeCondition("endDateTime", EntityOperator.LESS_THAN, UtilDateTime.nowTimestamp());
            EntityCondition expression3 = EntityCondition.makeCondition("itemId", EntityOperator.NOT_EQUAL, null);
            List<EntityCondition> expressions = new LinkedList<EntityCondition>();
            expressions.add(expression1);
            expressions.add(expression2);
            expressions.add(expression3);
            EntityCondition cond = EntityCondition.makeCondition(expressions, EntityOperator.AND);
            List<GenericValue> ebayProductListings = EntityQuery.use(delegator).from("EbayProductListing").where(expressions).queryList();
            for (int index = 0; index < ebayProductListings.size(); index++) {
                Map<String, Object> inMap = new HashMap<String, Object>();
                AddItemCall addItemCall = new AddItemCall(EbayStoreHelper.getApiContext((String)context.get("productStoreId"), locale, delegator));
                GenericValue ebayProductListing = ebayProductListings.get(index);
                ItemType item = EbayStoreHelper.prepareAddItem(delegator, ebayProductListing);
                addItemCall.setItem(item);
                itemObject.put("addItemCall", addItemCall);
                itemObject.put("productListingId", ebayProductListing.getString("productListingId"));
                inMap.put("itemObject", itemObject);
                inMap.put("userLogin", userLogin);
                Map<String, Object>result = dispatcher.runSync("exportProductEachItem", inMap);
                String success = (String) result.get("responseMessage");
                if ("success".equals(success)) {
                    String duration = item.getListingDuration();
                    if (duration.length() > 4) {
                        Timestamp startDateTime = UtilDateTime.nowTimestamp();
                        int durationInt = Integer.parseInt(duration.replace("DAYS_", ""));
                        Timestamp endDateTime = UtilDateTime.addDaysToTimestamp(startDateTime, durationInt);
                        ebayProductListing.set("startDateTime", startDateTime);
                        ebayProductListing.set("endDateTime", endDateTime);
                        ebayProductListing.store();
                    }
                }
            }
        } catch (Exception e) {
            return ServiceUtil.returnError(e.getMessage());
        }
        return ServiceUtil.returnSuccess();
    }
    public static Map<String, Object> autoBestOffer(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get("locale");
        try {
            String productStoreId = (String) context.get("productStoreId");
            GenericValue ebayProductStorePref = EntityQuery.use(delegator).from("EbayProductStorePref").where("productStoreId", productStoreId, "autoPrefEnumId", "EBAY_AUTO_BEST_OFFER").queryOne();
            String parentPrefCondId = ebayProductStorePref.getString("parentPrefCondId");
            List<GenericValue> ebayProductStorePrefCond = EntityQuery.use(delegator).from("EbayProductStorePrefCond").where("parentPrefCondId", parentPrefCondId).queryList();
            //Parameters
            String priceType = ebayProductStorePrefCond.get(0).getString("acceptanceCondition");
            String acceptBestOfferValue = ebayProductStorePrefCond.get(1).getString("acceptanceCondition");
            String rejectOffer = ebayProductStorePrefCond.get(2).getString("acceptanceCondition");
            String ignoreOfferMessage = ebayProductStorePrefCond.get(3).getString("acceptanceCondition");
            String rejectGreaterEnable = ebayProductStorePrefCond.get(4).getString("acceptanceCondition");
            String greaterValue = ebayProductStorePrefCond.get(5).getString("acceptanceCondition");
            String lessValue = ebayProductStorePrefCond.get(6).getString("acceptanceCondition");
            String rejectGreaterMsg = ebayProductStorePrefCond.get(7).getString("acceptanceCondition");
            String rejectLessEnable = ebayProductStorePrefCond.get(8).getString("acceptanceCondition");
            String lessThanValue = ebayProductStorePrefCond.get(9).getString("acceptanceCondition");
            String rejectLessMsg = ebayProductStorePrefCond.get(10).getString("acceptanceCondition");
            //case parameter to double type
            BigDecimal acceptPercentValue = new BigDecimal(acceptBestOfferValue);
            BigDecimal greaterPercentValue = new BigDecimal(greaterValue);
            BigDecimal lessThanPercentValue = new BigDecimal(lessValue);
            BigDecimal rejectPercentValue = new BigDecimal(lessThanValue);

            ApiContext apiContext = EbayStoreHelper.getApiContext(productStoreId, locale, delegator);
            //GetMysbaySellingCall for get total page
            GetMyeBaySellingCall getTotalPage = new GetMyeBaySellingCall(apiContext);
            ItemListCustomizationType itemListType = new ItemListCustomizationType();
            itemListType.setInclude(Boolean.TRUE);
            itemListType.setSort(ItemSortTypeCodeType.ITEM_ID_DESCENDING);
            itemListType.setListingType(ListingTypeCodeType.FIXED_PRICE_ITEM);
            DetailLevelCodeType[] detailLevels = new DetailLevelCodeType[] {
                DetailLevelCodeType.RETURN_ALL,
                DetailLevelCodeType.ITEM_RETURN_ATTRIBUTES,
                DetailLevelCodeType.ITEM_RETURN_DESCRIPTION
            };
            getTotalPage.setDetailLevel(detailLevels);
            getTotalPage.setActiveList(itemListType);
            getTotalPage.getMyeBaySelling();
            int totalPage = getTotalPage.getReturnedActiveList().getPaginationResult().getTotalNumberOfPages();
            for (int t = 1; t <= totalPage; t++) {
                //GetMyebaySellingCall for get item that is sold on store
                GetMyeBaySellingCall ebaySelling = new GetMyeBaySellingCall(apiContext);
                //Set type of item
                ItemListCustomizationType itemList = new ItemListCustomizationType();
                itemList.setInclude(Boolean.TRUE);
                itemListType.setSort(ItemSortTypeCodeType.ITEM_ID_DESCENDING);
                itemListType.setListingType(ListingTypeCodeType.FIXED_PRICE_ITEM);

                PaginationType page = new PaginationType();
                page.setPageNumber(t);
                itemList.setPagination(page);
                itemList.setListingType(ListingTypeCodeType.FIXED_PRICE_ITEM);

                DetailLevelCodeType[] detailLevel = new DetailLevelCodeType[] {
                        DetailLevelCodeType.RETURN_ALL,
                        DetailLevelCodeType.ITEM_RETURN_ATTRIBUTES,
                        DetailLevelCodeType.ITEM_RETURN_DESCRIPTION,
                        DetailLevelCodeType.RETURN_HEADERS,
                        DetailLevelCodeType.RETURN_MESSAGES
                };
                ebaySelling.setDetailLevel(detailLevel);
                ebaySelling.setActiveList(itemList);
                ebaySelling.getMyeBaySelling();
                PaginatedItemArrayType itemListCustomizationType = ebaySelling.getReturnedActiveList();
                ItemArrayType itemArrayType = itemListCustomizationType.getItemArray();
                int itemArrayTypeSize = itemArrayType.getItemLength();

                //Loop for get item
                for (int itemCount = 0; itemCount < itemArrayTypeSize; itemCount++) {
                    ItemType item = itemArrayType.getItem(itemCount);
                    String itemID = item.getItemID();
                    Double buyItNowPrice = item.getBuyItNowPrice().getValue();
                    GetItemCall getItem = new GetItemCall(apiContext);
                    getItem.setDetailLevel(detailLevel);
                    getItem.getItem(itemID);
                    String SKUItem = getItem.getSKU();
                    ItemType itemBestOffer = getItem.getReturnedItem();
                    BestOfferDetailsType bestOfferDetailsType = itemBestOffer.getBestOfferDetails();
                    int inventoryQuantityItem = item.getQuantityAvailable();  //Quantity of the item
                    int bestOfferCount = itemBestOffer.getBestOfferDetails().getBestOfferCount();
                    Boolean bestOfferIsEnabled = itemBestOffer.getBestOfferDetails().isBestOfferEnabled();
                    //Check value of Best offer Detail not null
                    if ((bestOfferDetailsType != null) && (bestOfferCount > 0) && bestOfferIsEnabled.equals(true)) {
                        //Get base price from kindOfPrice parameter
                        Double doBasePrice = null;
                        if (priceType.equals("BUY_IT_NOW_PRICE")) {
                            doBasePrice = buyItNowPrice;
                        } else if (priceType.equals("START_PRICE")) {
                            doBasePrice = itemBestOffer.getStartPrice().getValue();
                        } else if (priceType.equals("RESERVE_PRICE")) {
                            doBasePrice = itemBestOffer.getReservePrice().getValue();
                        } else if (priceType.equals("RETAIL_PRICE")) {
                            //ignore
                        } else if (priceType.equals("SELLER_COST")) {
                            List<GenericValue> supplierProduct = EntityQuery.use(delegator).from("SupplierProduct").where("productId", SKUItem).orderBy("availableFromDate DESC").queryList();
                            String lastPrice = supplierProduct.get(0).getString("lastPrice");
                            doBasePrice = Double.parseDouble(lastPrice);
                        } else if (priceType.equals("SECOND_CHANCE_PRICE")) {
                            VerifyAddSecondChanceItemCall verifyAddSecondChanceItemCall = new VerifyAddSecondChanceItemCall(apiContext);
                            doBasePrice = verifyAddSecondChanceItemCall.getBuyItNowPrice().getValue();
                        } else if (priceType.equals("STORE_PRICE")) {
                            //ignore
                        }
                        BigDecimal basePrice = new BigDecimal(doBasePrice);
                        BigDecimal percent = new BigDecimal(100);
                        //Calculate price with base price and percent from parameter
                        BigDecimal acceptPrice = (basePrice.multiply(acceptPercentValue)).divide(percent);
                        BigDecimal greaterPrice = (basePrice.multiply(greaterPercentValue)).divide(percent);
                        BigDecimal lessThanPrice = (basePrice.multiply(lessThanPercentValue)).divide(percent);
                        BigDecimal rejectPrice = (basePrice.multiply(rejectPercentValue)).divide(percent);

                        //GetBestOfferCall for get best offer detail
                        GetBestOffersCall getBestOfferCall = new GetBestOffersCall(apiContext);
                        getBestOfferCall.setItemID(itemID);
                        getBestOfferCall.setDetailLevel(detailLevel);
                        getBestOfferCall.setBestOfferStatus(BestOfferStatusCodeType.ALL);
                        getBestOfferCall.getBestOffers();
                        BestOfferType[] bestOffers = getBestOfferCall.getReturnedBestOffers();
                        List<String> acceptBestOfferIndexId = new LinkedList<String>();
                        SortedMap<String, Object> acceptBestOfferIDs = new TreeMap<String, Object>();
                        //Loop for get data best offer from buyer
                        RespondToBestOfferCall respondToBestOfferCall = new RespondToBestOfferCall(apiContext);
                        respondToBestOfferCall.setItemID(itemID);
                        for (int offerCount = 0; offerCount < bestOffers.length; offerCount++) {
                            BestOfferType bestOfferType = bestOffers[offerCount];
                            BestOfferStatusCodeType bestOfferStatusCodeType = bestOfferType.getStatus();
                            //Check status of best offer
                            if (bestOfferStatusCodeType == BestOfferStatusCodeType.PENDING) {
                                String bestOfferID = bestOfferType.getBestOfferID();
                                UserType buyer = bestOfferType.getBuyer();
                                String buyerUserID = buyer.getUserID();
                                AmountType price = bestOfferType.getPrice();
                                String offerPrice = new Double(price.getValue()).toString();
                                Double doCerrentPrice = Double.parseDouble(offerPrice);
                                int offerQuantity = bestOfferType.getQuantity();
                                String[] bestOfferIDs = { bestOfferID };
                                respondToBestOfferCall.setBestOfferIDs(bestOfferIDs);

                                if (rejectOffer.equals("Y")) {
                                    if (offerQuantity > inventoryQuantityItem) {
                                        respondToBestOfferCall.setSellerResponse("Your order is more than inventory item's Buy-It-Now price.");
                                        respondToBestOfferCall.setBestOfferAction(BestOfferActionCodeType.DECLINE);
                                        respondToBestOfferCall.respondToBestOffer();
                                        continue;
                                    }
                                }

                                String buyerMessage = bestOfferType.getBuyerMessage();
                                if (ignoreOfferMessage.equals("Y") && UtilValidate.isNotEmpty(buyerMessage)) {
                                    GenericValue userOfferCheck = EntityQuery.use(delegator).from("EbayUserBestOffer").where("itemId", itemID, "userId", buyerUserID).queryOne();
                                    if (UtilValidate.isEmpty(userOfferCheck)) {
                                        GenericValue ebayUserBestOffer = delegator.makeValue("EbayUserBestOffer");
                                        ebayUserBestOffer.put("productStoreId", productStoreId);
                                        ebayUserBestOffer.put("itemId", itemID);
                                        ebayUserBestOffer.put("userId", buyerUserID);
                                        ebayUserBestOffer.put("bestOfferId", bestOfferID);
                                        ebayUserBestOffer.put("contactStatus", "NOT_CONTACT");
                                        ebayUserBestOffer.create();
                                    }
                                    continue;
                                }
                                BigDecimal cerrentPrice = new BigDecimal(doCerrentPrice);
                                if (cerrentPrice.compareTo(acceptPrice) >= 0) {
                                    acceptBestOfferIndexId.add(bestOfferID);
                                    String Quantity = String.valueOf(offerQuantity);
                                    acceptBestOfferIDs.put(bestOfferID, Quantity);
                                } else if ((cerrentPrice.compareTo(greaterPrice) >= 0) && (cerrentPrice.compareTo(lessThanPrice) <= 0 ) && rejectGreaterEnable.equals("Y")) {
                                    respondToBestOfferCall.setBestOfferAction(BestOfferActionCodeType.DECLINE);
                                    respondToBestOfferCall.setSellerResponse(rejectGreaterMsg);
                                    respondToBestOfferCall.respondToBestOffer();
                                } else if ((cerrentPrice.compareTo(rejectPrice) <= 0 && rejectLessEnable.equals("Y"))) {
                                    respondToBestOfferCall.setBestOfferAction(BestOfferActionCodeType.DECLINE);
                                    respondToBestOfferCall.setSellerResponse(rejectLessMsg);
                                    respondToBestOfferCall.respondToBestOffer();
                                } else {
                                    respondToBestOfferCall.setBestOfferAction(BestOfferActionCodeType.DECLINE);
                                    respondToBestOfferCall.respondToBestOffer();
                                }
                            }
                        }

                        if (acceptBestOfferIndexId.size() > 0) {
                            int quantityAvailable = inventoryQuantityItem;
                            Collections.sort(acceptBestOfferIndexId);
                            RespondToBestOfferCall respondAcceptBestOfferCall = new RespondToBestOfferCall(apiContext);
                            respondAcceptBestOfferCall.setItemID(itemID);
                            for (String bestOfferIdIndex : acceptBestOfferIndexId) {
                                if (quantityAvailable <= 0) break;
                                Integer offerQuantity = Integer.parseInt(acceptBestOfferIDs.get(bestOfferIdIndex).toString());
                                String[] bestOfferID = { bestOfferIdIndex };
                                respondAcceptBestOfferCall.setBestOfferIDs(bestOfferID);
                                //respondAcceptBestOfferCall.setBestOfferIDs(bestOfferID);
                                if (offerQuantity <= quantityAvailable) {
                                    respondAcceptBestOfferCall.setBestOfferAction(BestOfferActionCodeType.ACCEPT);
                                    quantityAvailable = quantityAvailable - offerQuantity;
                                } else {
                                    respondAcceptBestOfferCall.setBestOfferAction(BestOfferActionCodeType.DECLINE);
                                }
                                respondAcceptBestOfferCall.respondToBestOffer();
                            }
                        }
                    }
                }
            }
        } catch (ApiException e){
            return ServiceUtil.returnError(e.getMessage());
        }catch(Exception e){
            return ServiceUtil.returnError(e.getMessage());
        }
        return ServiceUtil.returnSuccess();
    }
}
