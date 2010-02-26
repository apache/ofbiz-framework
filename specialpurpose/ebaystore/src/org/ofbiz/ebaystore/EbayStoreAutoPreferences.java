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

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import javolution.util.FastList;
import javolution.util.FastMap;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;

import com.ebay.sdk.ApiContext;
import com.ebay.sdk.ApiException;
import com.ebay.sdk.SdkException;
import com.ebay.sdk.call.AddOrderCall;
import com.ebay.sdk.call.AddDisputeCall;
import com.ebay.sdk.call.GetSellingManagerSoldListingsCall;
import com.ebay.sdk.call.GetUserCall;
import com.ebay.sdk.call.LeaveFeedbackCall;
import com.ebay.soap.eBLBaseComponents.AddOrderRequestType;
import com.ebay.soap.eBLBaseComponents.AddOrderResponseType;
import com.ebay.soap.eBLBaseComponents.AmountType;
import com.ebay.sdk.call.RelistItemCall;
import com.ebay.soap.eBLBaseComponents.AutomatedLeaveFeedbackEventCodeType;
import com.ebay.soap.eBLBaseComponents.BuyerPaymentMethodCodeType;
import com.ebay.soap.eBLBaseComponents.CommentTypeCodeType;
import com.ebay.soap.eBLBaseComponents.CurrencyCodeType;
import com.ebay.soap.eBLBaseComponents.DetailLevelCodeType;
import com.ebay.soap.eBLBaseComponents.DisputeExplanationCodeType;
import com.ebay.soap.eBLBaseComponents.DisputeReasonCodeType;
import com.ebay.soap.eBLBaseComponents.FeedbackDetailType;
import com.ebay.soap.eBLBaseComponents.ItemType;
import com.ebay.soap.eBLBaseComponents.OrderType;
import com.ebay.soap.eBLBaseComponents.ItemType;
import com.ebay.soap.eBLBaseComponents.SellingManagerOrderStatusType;
import com.ebay.soap.eBLBaseComponents.SellingManagerPaidStatusCodeType;
import com.ebay.soap.eBLBaseComponents.SellingManagerShippedStatusCodeType;
import com.ebay.soap.eBLBaseComponents.SellingManagerSoldListingsSortTypeCodeType;
import com.ebay.soap.eBLBaseComponents.SellingManagerSoldOrderType;
import com.ebay.soap.eBLBaseComponents.SellingManagerSoldTransactionType;
import com.ebay.soap.eBLBaseComponents.TradingRoleCodeType;
import com.ebay.soap.eBLBaseComponents.TransactionArrayType;
import com.ebay.soap.eBLBaseComponents.TransactionType;
import com.ebay.soap.eBLBaseComponents.UserType;

public class EbayStoreAutoPreferences {
	public static String module = EbayStoreAutoPreferences.class.getName();

	public EbayStoreAutoPreferences(){

	}
	/*  It may take several minutes to process your automated feedback.  to connect to ebay site*/
	public static Map<String, Object> autoPrefLeaveFeedbackOption(DispatchContext dctx, Map<String, ? extends Object> context) throws ApiException, SdkException, Exception{

		Delegator delegator = dctx.getDelegator();
		Locale locale = (Locale) context.get("locale");

		if (UtilValidate.isEmpty(context.get("productStoreId"))&& UtilValidate.isEmpty(context.get("jobId"))){
			return ServiceUtil.returnFailure("Required productStoreId for get api context to connect with ebay site.");
		}
		String jobId = (String) context.get("jobId");
		String productStoreId = (String) context.get("productStoreId");
		String isAutoPositiveFeedback = "N";
		String feedbackEventCode = null;
		GenericValue ebayProductStorePref = null;
		List<String> list = FastList.newInstance();

		try {
			ApiContext apiContext = EbayStoreHelper.getApiContext(productStoreId, locale, delegator);
			ebayProductStorePref = delegator.findByPrimaryKey("EbayProductStorePref", UtilMisc.toMap("productStoreId", productStoreId,"autoPrefEnumId","EBAY_AUTO_PIT_FB"));
			if (UtilValidate.isNotEmpty(ebayProductStorePref) && UtilValidate.isNotEmpty(ebayProductStorePref.getString("autoPrefJobId"))) {
				isAutoPositiveFeedback = ebayProductStorePref.getString("enabled");
				// if isAutoPositiveFeedback is N that means not start this job run service
				if ("Y".equals(isAutoPositiveFeedback)&& jobId.equals(ebayProductStorePref.getString("autoPrefJobId"))) {
					feedbackEventCode = ebayProductStorePref.getString("condition1");
					String storeComments = ebayProductStorePref.getString("condition2");
					String comment = null;
					if (UtilValidate.isNotEmpty(storeComments)){
						if (storeComments.indexOf("\\[,\\]") != -1) {
							String[] strs = storeComments.split("\\[,\\]");
							for (String str :strs) {
								list.add(str);
							}
						}
					}
					// start getting sold item list from ebay follow your site
					GetSellingManagerSoldListingsCall sellingManagerSoldListings = new GetSellingManagerSoldListingsCall(apiContext);

					List<SellingManagerSoldOrderType> items = FastList.newInstance();
					SellingManagerSoldOrderType[] sellingManagerSoldOrders = sellingManagerSoldListings.getSellingManagerSoldListings();
					if (UtilValidate.isNotEmpty(sellingManagerSoldOrders)) {
						for(SellingManagerSoldOrderType solditem :sellingManagerSoldOrders){
							SellingManagerOrderStatusType orderStatus = solditem.getOrderStatus();
							if (orderStatus != null && !orderStatus.isFeedbackSent()) {
								SellingManagerPaidStatusCodeType  paidStatus = orderStatus.getPaidStatus();
								CommentTypeCodeType commentType  = orderStatus.getFeedbackReceived();
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
						for(SellingManagerSoldOrderType item :items){
							// start leave feedbacks
							SellingManagerSoldTransactionType[] soldTrans = item.getSellingManagerSoldTransaction();
							if (UtilValidate.isNotEmpty(soldTrans)) {
								for(SellingManagerSoldTransactionType soldTran : soldTrans){
									LeaveFeedbackCall leaveFeedbackCall = new LeaveFeedbackCall(apiContext);
									FeedbackDetailType detail = new FeedbackDetailType();
									// ramdom comments
									if (list.size()>0) {
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
		}catch (Exception e) {
			return ServiceUtil.returnFailure("Problems to connect with ebay site message:"+e);
		}

		return ServiceUtil.returnSuccess();
	}

	public static String autoPrefLeaveFeedbackOptions(HttpServletRequest request, HttpServletResponse response) {
		HttpSession session = request.getSession();
		LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
		GenericValue userLogin = (GenericValue) session.getAttribute("userLogin");
		Delegator delegator = (Delegator) request.getAttribute("delegator");
		Locale locale = UtilHttp.getLocale(request);
		Map paramMap = UtilHttp.getCombinedMap(request);

		if (UtilValidate.isEmpty(paramMap.get("productStoreId"))){
			request.setAttribute("_ERROR_MESSAGE_","Required productStoreId for get api context to connect with ebay site.");
			return "error";
		}

		String productStoreId = (String) paramMap.get("productStoreId");
		String isAutoPositiveFeedback = "N";
		String condition = null;
		if (UtilValidate.isNotEmpty(paramMap.get("isAutoPositiveFeedback"))) isAutoPositiveFeedback = (String) paramMap.get("isAutoPositiveFeedback");
		String feedbackEventCode = (String) paramMap.get("feedbackEventCode");
		ApiContext apiContext = EbayStoreHelper.getApiContext(productStoreId, locale, delegator);

		try {
			GenericValue ebayProductStorePref = null;
			String comments = null;
			String autoPrefJobId = null;

			if ("Y".equals(isAutoPositiveFeedback)) {
				if ("PAYMENT_RECEIVED".equals(feedbackEventCode)) {
					condition = AutomatedLeaveFeedbackEventCodeType.PAYMENT_RECEIVED.toString();
				} else if ("POSITIVE_FEEDBACK_RECEIVED".equals(feedbackEventCode)) {
					condition = AutomatedLeaveFeedbackEventCodeType.POSITIVE_FEEDBACK_RECEIVED.toString();
				}
				// allow only 10 comment can be store / set new comments to condition2 separate by [,]
			}
			for(int i=1;i<=5;i++){
				String comment = (String)paramMap.get("comment_".concat(String.valueOf(i)));
				if (comment!=null && comment.length()>0) {
					if (comments==null) comments = comment;
					else comments = comments.concat("[").concat(",").concat(("]").concat(comment));
				}
			}
			if (UtilValidate.isEmpty(comments)){
				request.setAttribute("_ERROR_MESSAGE_","Required least one at comment for your store feedback send with ebay site.");
				return "error";
			}

			Map context  = UtilMisc.toMap("userLogin", userLogin,"serviceName","autoPrefLeaveFeedbackOption");
			ebayProductStorePref = delegator.findByPrimaryKey("EbayProductStorePref", UtilMisc.toMap("productStoreId", productStoreId,"autoPrefEnumId","EBAY_AUTO_PIT_FB"));
			context.put("productStoreId", productStoreId);
			context.put("autoPrefEnumId", "EBAY_AUTO_PIT_FB");
			if (UtilValidate.isNotEmpty(ebayProductStorePref) && UtilValidate.isNotEmpty(ebayProductStorePref.getString("autoPrefJobId"))) autoPrefJobId = ebayProductStorePref.getString("autoPrefJobId");
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
			request.setAttribute("_EVENT_MESSAGE_","Setting Automated Positive Feedback for Buyers Success with site "+apiContext.getSite().value());

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
	public static Map<String, Object> autoSendFeedbackReminderEmail(DispatchContext dctx, Map<String, ? extends Object> context) throws ApiException, SdkException, Exception{
		Delegator delegator = dctx.getDelegator();
		Locale locale = (Locale) context.get("locale");

		if (UtilValidate.isEmpty(context.get("productStoreId")) && UtilValidate.isEmpty(context.get("jobId"))){
			return ServiceUtil.returnFailure("Required productStoreId for get api context to connect with ebay site.");
		}
		String jobId = (String) context.get("jobId");
		String productStoreId = (String) context.get("productStoreId");
		String isAutoFeedbackReminder = "N";
		int afterDays = 0;
		String isAlsoSendCopyToSeller = "N";
		GenericValue ebayProductStorePref = null;
		List<String> list = FastList.newInstance();
		String dateTimeFormat = UtilDateTime.DATE_TIME_FORMAT;
		SimpleDateFormat formatter = new SimpleDateFormat(dateTimeFormat);
		
		try {
			ApiContext apiContext = EbayStoreHelper.getApiContext(productStoreId, locale, delegator);
			ebayProductStorePref = delegator.findByPrimaryKey("EbayProductStorePref", UtilMisc.toMap("productStoreId", productStoreId,"autoPrefEnumId","EBAY_AUTO_FB_RMD"));
			if (UtilValidate.isNotEmpty(ebayProductStorePref) && UtilValidate.isNotEmpty(ebayProductStorePref.getString("autoPrefJobId"))) {
				isAutoFeedbackReminder = ebayProductStorePref.getString("enabled");
				// if isAutoPositiveFeedback is N that means not start this job run service
				if ("Y".equals(isAutoFeedbackReminder)&& jobId.equals(ebayProductStorePref.getString("autoPrefJobId"))) {
					afterDays = Integer.parseInt(ebayProductStorePref.getString("condition1"));
					isAlsoSendCopyToSeller = ebayProductStorePref.getString("condition2");

					// start getting sold item list from ebay follow your site
					GetSellingManagerSoldListingsCall sellingManagerSoldListings = new GetSellingManagerSoldListingsCall(apiContext);
					List<SellingManagerSoldOrderType> items = FastList.newInstance();
					SellingManagerSoldOrderType[] sellingManagerSoldOrders = sellingManagerSoldListings.getSellingManagerSoldListings();
					if (UtilValidate.isNotEmpty(sellingManagerSoldOrders)) {
						for(SellingManagerSoldOrderType solditem :sellingManagerSoldOrders){
							SellingManagerOrderStatusType orderStatus = solditem.getOrderStatus();
							if (orderStatus != null) {
								SellingManagerPaidStatusCodeType  paidStatus = orderStatus.getPaidStatus();
								SellingManagerShippedStatusCodeType  shippedStatus = orderStatus.getShippedStatus();
								
								//Buyer has paid for this item.  && Seller shipped items but feedback has not been received from buyer more than days condition 
								if (SellingManagerPaidStatusCodeType.PAID.equals(paidStatus) && SellingManagerShippedStatusCodeType.SHIPPED.equals(shippedStatus)) {
									Calendar right_now =  Calendar.getInstance();
									Calendar shippedTime = orderStatus.getShippedTime();
									Calendar afterShippedTime = orderStatus.getShippedTime();
									afterShippedTime.add(afterShippedTime.DAY_OF_MONTH, afterDays);
									Debug.logInfo("Verify date for send reminder feedback eamil by auto service: buyer "+solditem.getBuyerID()+" seller shippedTime " +
											""+formatter.format(shippedTime)+" codition days "+afterDays+" after shippedTime :"+formatter.format(afterShippedTime)+" now date"+formatter.format(right_now), module);
									// if now date is after shipped time follow after days condition would be send reminder email to buyer
									if (right_now.after(afterShippedTime)) items.add(solditem);
								}
							}
						}
						
						// call service send email (get template follow productStoreId)
						GetUserCall getUserCall = new GetUserCall(apiContext);
						String sellerUser = getUserCall.getUser().getUserID();
						for(SellingManagerSoldOrderType item :items){
							// start leave feedbacks
							SellingManagerSoldTransactionType[] soldTrans = item.getSellingManagerSoldTransaction();
							if (UtilValidate.isNotEmpty(soldTrans)) {
								for(SellingManagerSoldTransactionType soldTran : soldTrans){
									// call send 
								}
							}
						}
					}
				}
			} 
		}catch (Exception e) {
			return ServiceUtil.returnFailure("Problems to connect with ebay site message:"+e);
		}
		
		return ServiceUtil.returnSuccess();
	}

	public static Map<String, Object> automaticEbayRelistSoldItems(DispatchContext dctx, Map<String, ? extends Object> context) {
		Map<String, Object>result = FastMap.newInstance();
		LocalDispatcher dispatcher = dctx.getDispatcher();
		Delegator delegator = dctx.getDelegator();
		Locale locale = (Locale) context.get("locale");
		String jobId = (String) context.get("jobId");
		try {
			GenericValue userLogin = delegator.findOne("UserLogin", false, "userLoginId", "system");
			Map<String, Object>serviceMap = FastMap.newInstance();
			serviceMap.put("userLogin", userLogin);
			List<GenericValue>stores = delegator.findByAnd("ProductStore", UtilMisc.toMap());
			//ProductStore
			List<GenericValue> productStores = delegator.findByAnd("EbayProductStorePref", UtilMisc.toMap("autoPrefJobId", jobId));
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
					Map eBayUserLogin = dispatcher.runSync("getEbayStoreUser", serviceMap);
					String eBayUserLoginId = (String)eBayUserLogin.get("userLoginId");
					GenericValue party =  delegator.findByPrimaryKey("UserLogin", UtilMisc.toMap("userLoginId", eBayUserLoginId));
					String partyId = party.getString("partyId");
					//save sold items to OFbBiz product entity
					Map resultService = dispatcher.runSync("getEbaySoldItems", serviceMap);
					List soldItems = (List) resultService.get("soldItems");
					if (soldItems.size()!=0) {
						for (int itemCount = 0; itemCount < soldItems.size(); itemCount++) {
							Map soldItemMap = (Map)soldItems.get(itemCount);
							if (UtilValidate.isNotEmpty(soldItemMap.get("itemId"))) {
								GenericValue productCheck = delegator.findByPrimaryKey("Product", UtilMisc.toMap("productId", soldItemMap.get("itemId")));
								if (productCheck == null) {
									Map<String, Object>inMap = FastMap.newInstance();
									inMap.put("productId", soldItemMap.get("itemId"));
									inMap.put("productTypeId", "EBAY_ITEM");
									inMap.put("internalName", "eBay Item " + soldItemMap.get("title"));
									inMap.put("userLogin", userLogin);
									dispatcher.runSync("createProduct", inMap);
									// ProductRole (VENDOR)
									List productRole = delegator.findByAnd("ProductRole", UtilMisc.toMap("partyId", partyId, "productId", soldItemMap.get("itemId"), "roleTypeId", "VENDOR"));
									if (productRole.size() == 0) {
										Map<String, Object>addRole = FastMap.newInstance();
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
					serviceMap = FastMap.newInstance();
					serviceMap.put("userLogin", userLogin);
					serviceMap.put("productStoreId", productStoreId);
					resultService = dispatcher.runSync("getEbayActiveItems", serviceMap);
					List activeItems = (List) resultService.get("activeItems");
					List<String> activeItemMaps = FastList.newInstance();
					if (activeItems.size() != 0) {
						for (int itemCount = 0; itemCount < activeItems.size(); itemCount++) {
							Map activeItemMap = (Map)activeItems.get(itemCount);
							if (UtilValidate.isNotEmpty(activeItemMap.get("itemId"))) {
								activeItemMaps.add((String)activeItemMap.get("itemId"));
							}
						}
					}
					//check product role
					List<GenericValue>productRoles = delegator.findByAnd("ProductRole", UtilMisc.toMap("partyId", partyId, "roleTypeId", "VENDOR"));
					List<String>productRoleIds = FastList.newInstance();
					if (productRoles.size() != 0) {
						for (int itemCount = 0; itemCount < productRoles.size(); itemCount++) {
							String productId = productRoles.get(itemCount).getString("productId");
							productRoleIds.add(productId);
						}
					}
					List andExpr = FastList.newInstance();
					EntityCondition activeItemCond = EntityCondition.makeCondition("productId", EntityOperator.NOT_IN, activeItemMaps);
					andExpr.add(activeItemCond);
					EntityCondition productTypeCond = EntityCondition.makeCondition("productTypeId", EntityOperator.EQUALS, "EBAY_ITEM");
					andExpr.add(productTypeCond);
					EntityCondition isVirtualCond = EntityCondition.makeCondition("isVirtual", EntityOperator.NOT_EQUAL, "Y");
					andExpr.add(isVirtualCond);
					EntityCondition productRole = EntityCondition.makeCondition("productId", EntityOperator.IN, productRoleIds);
					andExpr.add(productRole);
					EntityCondition andCond =  EntityCondition.makeCondition(andExpr, EntityOperator.AND);
					List itemsToRelist = delegator.findList("Product", andCond, null, null, null, false);
					if (itemsToRelist.size() != 0) {
						//re-list sold items and not active
						Map<String, Object> inMap = FastMap.newInstance();
						inMap.put("productStoreId", productStoreId);
						inMap.put("userLogin", userLogin);
						Map<String, Object> resultUser = dispatcher.runSync("getEbayStoreUser", inMap);
						String userID = (String) resultUser.get("userLoginId");
						ApiContext apiContext = EbayStoreHelper.getApiContext(productStoreId, locale, delegator);
						for (int itemRelist = 0; itemRelist < itemsToRelist.size(); itemRelist++) {
							RelistItemCall relistItemCall = new RelistItemCall(apiContext);
							ItemType itemToBeRelisted = new ItemType();
							GenericValue product = (GenericValue)itemsToRelist.get(itemRelist);
							itemToBeRelisted.setItemID(product.getString("productId"));
							relistItemCall.setItemToBeRelisted(itemToBeRelisted);
							relistItemCall.relistItem();
							GenericValue productStore = delegator.findByPrimaryKey("Product", UtilMisc.toMap("productId", product.getString("productId")));
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
		Map<String, Object>result = FastMap.newInstance();
		LocalDispatcher dispatcher = dctx.getDispatcher();
		Delegator delegator = dctx.getDelegator();
		Locale locale = (Locale) context.get("locale");
		String jobId = (String) context.get("jobId");
		try {
			GenericValue userLogin = delegator.findOne("UserLogin", false, "userLoginId", "system");
			List<GenericValue> productStores = delegator.findByAnd("EbayProductStorePref", UtilMisc.toMap("autoPrefJobId", jobId));
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
				Map<String, Object>serviceMap = FastMap.newInstance();
				serviceMap.put("productStoreId", productStoreId);
				serviceMap.put("userLogin", userLogin);
				Map resultService = dispatcher.runSync("getEbaySoldItems", serviceMap);
				List soldItems = (List) resultService.get("soldItems");
				// check items to dispute
				List<Map>itemsToDispute = FastList.newInstance();
				for (int itemCount = 0; itemCount < soldItems.size(); itemCount++) {
					Map item = (Map) soldItems.get(itemCount);
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
					DetailLevelCodeType[] detailLevels = new DetailLevelCodeType[] {
					          DetailLevelCodeType.RETURN_ALL,
					          DetailLevelCodeType.ITEM_RETURN_ATTRIBUTES,
					          DetailLevelCodeType.ITEM_RETURN_DESCRIPTION
					      };
					for (int count = 0; count < itemsToDispute.size(); count++) {
						Map<String, Object>item = itemsToDispute.get(count);
						AddDisputeCall api = new AddDisputeCall(apiContext);
					    api.setDetailLevel(detailLevels);
					    api.setItemID((String)item.get("itemId"));
					    api.setTransactionID((String)item.get("transactionId"));
					    api.setDisputeExplanation(disputeExplanation);
					    api.setDisputeReason(disputeReason);
					    String id = api.addDispute();
					}
				}
			}
		} catch (Exception e) {
			return ServiceUtil.returnError(e.getMessage());
		}
		return ServiceUtil.returnSuccess();
	}
	public static Map<String, Object> automaticEbayDisputeNotPay(DispatchContext dctx, Map<String, ? extends Object> context) {
		Map<String, Object>result = FastMap.newInstance();
		LocalDispatcher dispatcher = dctx.getDispatcher();
		Delegator delegator = dctx.getDelegator();
		Locale locale = (Locale) context.get("locale");
		String jobId = (String) context.get("jobId");
		try {
			GenericValue userLogin = delegator.findOne("UserLogin", false, "userLoginId", "system");
			List<GenericValue> productStores = delegator.findByAnd("EbayProductStorePref", UtilMisc.toMap("autoPrefJobId", jobId));
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
				Map<String, Object>serviceMap = FastMap.newInstance();
				serviceMap.put("productStoreId", productStoreId);
				serviceMap.put("userLogin", userLogin);
				Map resultService = dispatcher.runSync("getEbaySoldItems", serviceMap);
				List soldItems = (List) resultService.get("soldItems");
				// check items to dispute
				List<Map>itemsToDispute = FastList.newInstance();
				for (int itemCount = 0; itemCount < soldItems.size(); itemCount++) {
					Map item = (Map) soldItems.get(itemCount);
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
					DetailLevelCodeType[] detailLevels = new DetailLevelCodeType[] {
					          DetailLevelCodeType.RETURN_ALL,
					          DetailLevelCodeType.ITEM_RETURN_ATTRIBUTES,
					          DetailLevelCodeType.ITEM_RETURN_DESCRIPTION
					      };
					for (int count = 0; count < itemsToDispute.size(); count++) {
						Map<String, Object>item = itemsToDispute.get(count);
						AddDisputeCall api = new AddDisputeCall(apiContext);
					    api.setDetailLevel(detailLevels);
					    api.setItemID((String)item.get("itemId"));
					    api.setTransactionID((String)item.get("transactionId"));
					    api.setDisputeExplanation(disputeExplanation);
					    api.setDisputeReason(disputeReason);
					    String id = api.addDispute();
					}
				}
			}
		} catch (Exception e) {
			return ServiceUtil.returnError(e.getMessage());
		}
		return ServiceUtil.returnSuccess();
	}
	/* start automatically service send an email when ebay seller has been received payment from ebay buyer */
	public static Map<String, Object> autoSendPaymentReceivedEmail(DispatchContext dctx, Map<String, ? extends Object> context) throws ApiException, SdkException, Exception{
		Delegator delegator = dctx.getDelegator();
		Locale locale = (Locale) context.get("locale");
		GenericValue userLogin = delegator.findOne("UserLogin", false, "userLoginId", "system");
		
		if (UtilValidate.isEmpty(context.get("productStoreId")) && UtilValidate.isEmpty(context.get("jobId"))){
			return ServiceUtil.returnFailure("Required productStoreId for get api context to connect with ebay site.");
		}
		
		String jobId = (String) context.get("jobId");
		String productStoreId = (String) context.get("productStoreId");
		
		String isAutoSendEmail = "N";
		String isAlsoSendCopyToSeller = "N";
		GenericValue ebayProductStorePref = null;
		List<String> list = FastList.newInstance();
		
		try {
			ApiContext apiContext = EbayStoreHelper.getApiContext(productStoreId, locale, delegator);
			ebayProductStorePref = delegator.findByPrimaryKey("EbayProductStorePref", UtilMisc.toMap("productStoreId", productStoreId,"autoPrefEnumId","EBAY_AUTO_FB_RMD"));
			if (UtilValidate.isNotEmpty(ebayProductStorePref) && UtilValidate.isNotEmpty(ebayProductStorePref.getString("autoPrefJobId"))) {
				isAutoSendEmail = ebayProductStorePref.getString("enabled");
				// if isAutoPositiveFeedback is N that means not start this job run service
				if ("Y".equals(isAutoSendEmail) && jobId.equals(ebayProductStorePref.getString("autoPrefJobId"))) {
					isAlsoSendCopyToSeller = ebayProductStorePref.getString("condition1");

					// start getting sold item list from ebay follow your site
					GetSellingManagerSoldListingsCall sellingManagerSoldListings = new GetSellingManagerSoldListingsCall(apiContext);
					List<SellingManagerSoldOrderType> items = FastList.newInstance();
					SellingManagerSoldOrderType[] sellingManagerSoldOrders = sellingManagerSoldListings.getSellingManagerSoldListings();
					if (UtilValidate.isNotEmpty(sellingManagerSoldOrders)) {
						for(SellingManagerSoldOrderType solditem :sellingManagerSoldOrders){
							SellingManagerOrderStatusType orderStatus = solditem.getOrderStatus();
							if (orderStatus != null) {
								SellingManagerPaidStatusCodeType  paidStatus = orderStatus.getPaidStatus();
								//Buyer has paid for this item. and seller received 
								if (SellingManagerPaidStatusCodeType.PAID.equals(paidStatus)) {
									items.add(solditem);
								}
							}
						}
						
						// call service send email (get template follow productStoreId)
						GetUserCall getUserCall = new GetUserCall(apiContext);
						String sellerUser = getUserCall.getUser().getUserID();
						for(SellingManagerSoldOrderType item :items){
							// start leave feedbacks
							SellingManagerSoldTransactionType[] soldTrans = item.getSellingManagerSoldTransaction();
							if (UtilValidate.isNotEmpty(soldTrans)) {
								for(SellingManagerSoldTransactionType soldTran : soldTrans){
									// call send 
								}
							}
						}
					}
				}
			} 
		}catch (Exception e) {
			return ServiceUtil.returnFailure("Problems to connect with ebay site message:"+e);
		}
		
		return ServiceUtil.returnSuccess();
	}


    public static Map<String, Object> runCombineOrders(DispatchContext dctx, Map<String,Object> context){
        Map<String, Object>result = FastMap.newInstance();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get("locale");
        String productStoreId = (String) context.get("productStoreId");
        try {
            ApiContext apiContext = EbayStoreHelper.getApiContext(productStoreId, locale, delegator);
            GetSellingManagerSoldListingsCall sellingManagerSoldListings = new GetSellingManagerSoldListingsCall(apiContext);
            SellingManagerSoldOrderType[] sellingManagerSoldOrders = sellingManagerSoldListings.getSellingManagerSoldListings();
            if(sellingManagerSoldOrders != null) {
              int soldOrderLength = sellingManagerSoldOrders.length;
              GenericValue ebayPref = delegator.findByPrimaryKey("EbayProductStorePref", UtilMisc.toMap("productStoreId", productStoreId, "autoPrefEnumId", "ENA_COMB_ORD"));
              if (UtilValidate.isNotEmpty(ebayPref)) {

                Timestamp beginDate = UtilDateTime.toTimestamp("01/01/2001 00:00:00");
                Long daysCount = Long.parseLong(ebayPref.get("condition1").toString());
                Hashtable h = new Hashtable();

                for(int i=0; i<soldOrderLength; i++){
                    SellingManagerSoldOrderType sellingManagerSoldOrder = sellingManagerSoldOrders[i];
                    String buyerId = sellingManagerSoldOrder.getBuyerID().toString();
                    List<Map<String, Object>> soldGroupList = FastList.newInstance();
                    Map<String, Object> mymap = FastMap.newInstance();
                    mymap.put("group", "");
                    mymap.put("soldorder", sellingManagerSoldOrder);
                    if (h.size() > 0) {
                        Enumeration enums = h.keys();
                        String key = "";
                        while (enums.hasMoreElements()) {
                            key = (String)enums.nextElement();
                            List<Map<String, Object>> tempList = (List<Map<String, Object>>) h.get(key);
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

                Enumeration enums = h.keys();
                while (enums.hasMoreElements()) {
                    int groupRunning = 0;
                    String key = (String)enums.nextElement();
                    List<Map<String, Object>> soldGroupList = (List<Map<String, Object>>) h.get(key);
                    int maxItems = Integer.parseInt(ebayPref.get("condition2").toString());

                    if (soldGroupList.size() > 1) {
                        for (int j=0;j<soldGroupList.size();j++) {
                            Map<String, Object> myMap = (Map<String, Object>) soldGroupList.get(j);
                            SellingManagerSoldOrderType soldorder = (SellingManagerSoldOrderType) myMap.get("soldorder");
                            Timestamp createdate = UtilDateTime.toTimestamp(soldorder.getCreationTime().getTime());
                            if(myMap.get("group").toString().length()==0) beginDate = createdate;
                            beginDate = findStartDate(beginDate, soldGroupList);
                             runCheckAndGroup(groupRunning, beginDate, daysCount, soldGroupList);
                             groupRunning++;
                        }

                        int x = 0;
                        while (x<groupRunning) {
                            OrderType order = new OrderType();
                            order.setCreatingUserRole(TradingRoleCodeType.SELLER);
                            BuyerPaymentMethodCodeType[] buyerPayment = new BuyerPaymentMethodCodeType[1];
                            buyerPayment[0] = BuyerPaymentMethodCodeType.CASH_ON_PICKUP;
                            order.setPaymentMethods(buyerPayment);
                            TransactionArrayType transactionArr = new TransactionArrayType();
                            List translist = FastList.newInstance();

                            AmountType total = new AmountType();
                            double totalAmt = 0.0;
                            CurrencyCodeType currencyId = null;
                            int totalQty = 0;

                            for (int j=0;j<soldGroupList.size();j++) {
                                Map<String, Object> myMap = (Map<String, Object>) soldGroupList.get(j);
                                if (UtilValidate.isNotEmpty(myMap.get("group"))) {
                                    if(x == Integer.parseInt(myMap.get("group").toString())){
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
                                            for (int count=0; count<sellingManagerSoldTransactions.length; count++) {
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
                                for (int counter=0; counter<translist.size(); counter++){
                                    transArr[counter] = (TransactionType)translist.get(counter);
                                }
                                transactionArr.setTransaction(transArr);
                                if (transactionArr.getTransactionLength() > 1) {
                                    order.setTotal(total);
                                    order.setTransactionArray(transactionArr);
                                    AddOrderCall call = new AddOrderCall(apiContext);
                                    AddOrderRequestType req = new AddOrderRequestType();
                                    AddOrderResponseType resp = null;
                                    req.setOrder(order);
                                    resp = (AddOrderResponseType)call.execute(req);
                                    if(resp != null && "SUCCESS".equals(resp.getAck().toString())){
                                       String orderId = resp.getOrderID();
                                       Debug.log(":: new order id is = "+orderId);
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
        for (Map<String,Object> inMap : inList){
            SellingManagerSoldOrderType soldorder = (SellingManagerSoldOrderType) inMap.get("soldorder");
            Timestamp createTimestamp = UtilDateTime.toTimestamp(soldorder.getCreationTime().getTime());
            String group = (String) inMap.get("group");
            if (createTimestamp.before(lessStartTimestamp) && group.length() == 0) {
                lessStartTimestamp = createTimestamp;
            }
        }
        return lessStartTimestamp;
    }

    public static void runCheckAndGroup(int groupRunning,Timestamp startTimestamp, long countDays, List<Map<String, Object>> inList) {
        Timestamp endDate = UtilDateTime.getDayEnd(UtilDateTime.toTimestamp(startTimestamp), countDays);
        for (Map<String,Object> inMap : inList) {
            String group = (String) inMap.get("group").toString();
            SellingManagerSoldOrderType soldorder = (SellingManagerSoldOrderType) inMap.get("soldorder");
            if (group.length() == 0) {
                Timestamp createtimestamp = UtilDateTime.toTimestamp(soldorder.getCreationTime().getTime());
                if (((createtimestamp.equals(startTimestamp)) || (createtimestamp.after(startTimestamp))) && (createtimestamp.before(endDate))) {
                   inMap.put("group", ""+groupRunning);
                }
            }
        }
    }
}
