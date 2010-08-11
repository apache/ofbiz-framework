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

import java.util.Locale;
import java.util.Map;

import javolution.util.FastMap;

import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;

import com.ebay.sdk.ApiContext;
import com.ebay.sdk.call.GetBestOffersCall;
import com.ebay.sdk.call.GetItemCall;
import com.ebay.sdk.call.RespondToBestOfferCall;
import com.ebay.soap.eBLBaseComponents.BestOfferActionCodeType;
import com.ebay.soap.eBLBaseComponents.BestOfferStatusCodeType;
import com.ebay.soap.eBLBaseComponents.BestOfferType;
import com.ebay.soap.eBLBaseComponents.DetailLevelCodeType;
import com.ebay.soap.eBLBaseComponents.ItemType;
import com.ebay.soap.eBLBaseComponents.UserType;

public class EbayStoreCustomerService {

    public static String module = EbayStoreCustomerService.class.getName();

    public static Map<String, Object> listBestOfferIncludeMessage(DispatchContext dctx, Map<String, ? extends Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get("locale");
        if (UtilValidate.isEmpty(context.get("userId")) || UtilValidate.isEmpty(context.get("itemId"))) {
            return ServiceUtil.returnFailure("Required userId and itemId.");
        }
        Map <String, Object> result = FastMap.newInstance();
        try {
            String itemId = (String) context.get("itemId");
            String bestOfferId = (String) context.get("bestOfferId");
            String productStoreId = (String) context.get("productStoreId");
            GenericValue userLogin = delegator.findOne("UserLogin", false, "userLoginId", "system");
            Map<String, Object> inMap = FastMap.newInstance();
            inMap.put("productStoreId", productStoreId);
            inMap.put("userLogin", userLogin);
            Map<String, Object> resultUser = dispatcher.runSync("getEbayStoreUser", inMap);
            String userID = (String) resultUser.get("userLoginId");
            ApiContext apiContext = EbayStoreHelper.getApiContext(productStoreId, locale, delegator);
            DetailLevelCodeType[] detailLevel = new DetailLevelCodeType[] {
                    DetailLevelCodeType.RETURN_ALL,
                    DetailLevelCodeType.ITEM_RETURN_ATTRIBUTES,
                    DetailLevelCodeType.ITEM_RETURN_DESCRIPTION
                    };
            GetItemCall getItemCall = new GetItemCall(apiContext);
            getItemCall.setDetailLevel(detailLevel);
            getItemCall.setItemID(itemId);
            getItemCall.getItem();
            ItemType item = getItemCall.getReturnedItem();
            String itemName = item.getTitle();
            GetBestOffersCall getBestOffersCall = new GetBestOffersCall(apiContext);
            getBestOffersCall.setDetailLevel(detailLevel);
            getBestOffersCall.setItemID(itemId);
            getBestOffersCall.setBestOfferID(bestOfferId);
            getBestOffersCall.getBestOffers();
            BestOfferType[] bestOffers = getBestOffersCall.getReturnedBestOffers();
            BestOfferType bestOffer = bestOffers[0];
            BestOfferStatusCodeType bestOfferStatus = bestOffer.getStatus();
            String offerStatus = bestOfferStatus.toString();
            String buyerMessage = bestOffer.getBuyerMessage();
            Double price = bestOffer.getPrice().getValue();
            String currentPrice = price.toString();
            Integer quantity = bestOffer.getQuantity();
            String orderQuantity = quantity.toString();
            UserType buyer = bestOffer.getBuyer();
            String buyerEmail = buyer.getEmail();
            result.put("email", buyerEmail);
            result.put("message", buyerMessage);
            result.put("price", currentPrice);
            result.put("quantity", orderQuantity);
            result.put("offerStatus", offerStatus);
            result.put("itemName", itemName);
        } catch (Exception e) {
            return ServiceUtil.returnFailure("Error from listBestOfferIncludeMessage service "+ e);
        }
        return result;
    }
    public static Map<String, Object> updateContactStatus(DispatchContext dctx, Map<String, ? extends Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get("locale");
        if (UtilValidate.isEmpty(context.get("productStoreId")) || UtilValidate.isEmpty(context.get("userId")) || UtilValidate.isEmpty(context.get("itemId")) || UtilValidate.isEmpty(context.get("offerId")) || UtilValidate.isEmpty(context.get("contactSetting"))) {
            return ServiceUtil.returnFailure("Required userId, itemId, productStoreId, OfferId and contactStatus.");
        }
        String userId = (String) context.get("userId");
        String itemId = (String) context.get("itemId");
        String itemName = (String) context.get("itemName");
        String productStoreId = (String) context.get("productStoreId");
        String offerId = (String) context.get("offerId");
        String contactStatus = (String) context.get("contactSetting");
        String price = (String) context.get("price");
        String email = (String) context.get("email");
        String quantity = (String) context.get("quantity");
        Map <String, Object> result = FastMap.newInstance();
        try {
            GenericValue userLogin = delegator.findOne("UserLogin", false, "userLoginId", "system");
            Map<String, Object> inMap = FastMap.newInstance();
            inMap.put("productStoreId", productStoreId);
            inMap.put("userLogin", userLogin);
            Map<String, Object> resultUser = dispatcher.runSync("getEbayStoreUser", inMap);
            String userID = (String) resultUser.get("userLoginId");
            ApiContext apiContext = EbayStoreHelper.getApiContext(productStoreId, locale, delegator);
            String[] bestOfferIDs = {offerId};
            RespondToBestOfferCall respondToBestOfferCall = new RespondToBestOfferCall(apiContext);
            respondToBestOfferCall.setItemID(itemId);
            respondToBestOfferCall.setBestOfferIDs(bestOfferIDs);
            if (contactStatus.equals("ACCEPT")) {
                respondToBestOfferCall.setBestOfferAction(BestOfferActionCodeType.ACCEPT);
                respondToBestOfferCall.respondToBestOffer();
                contactStatus = "FINISHED";
            } else if (contactStatus.equals("DECLINE")) {
                respondToBestOfferCall.setBestOfferAction(BestOfferActionCodeType.DECLINE);
                respondToBestOfferCall.respondToBestOffer();
                contactStatus = "FINISHED";
            } else {
                return ServiceUtil.returnFailure("Required contactStatus setting.");
            }
            GenericValue  ebayUserBestOffer = delegator.findByPrimaryKey("EbayUserBestOffer", UtilMisc.toMap("userId", userId, "itemId", itemId));
            ebayUserBestOffer.set("contactStatus", contactStatus);
            ebayUserBestOffer.store();
            
            result.put("userId", userId);
            result.put("itemId", itemId);
            result.put("productStoreId", productStoreId);
            result.put("offerId", offerId);
            result.put("contactStatus", contactStatus);
            result.put("price", price);
            result.put("email", email);
            result.put("itemName", itemName);
            result.put("quantity", quantity);
        } catch (Exception e) {
            return ServiceUtil.returnFailure("Error from updateContactStatus service "+ e);
        }
        return result;
    }
    public static Map<String, Object> deleteContactAlert(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        String productStoreId = (String) context.get("productStoreId");
        Map <String, Object> result = FastMap.newInstance();
        if (UtilValidate.isEmpty(context.get("itemId")) || UtilValidate.isEmpty(context.get("userId"))) {
            return ServiceUtil.returnFailure("Required userId and itemId");
        }
        String itemId = (String) context.get("itemId");
        String userId = (String) context.get("userId");
        try {
            GenericValue ebayUserBestOffer = delegator.findByPrimaryKey("EbayUserBestOffer", UtilMisc.toMap("itemId", itemId, "userId", userId));
            ebayUserBestOffer.remove();
            result.put("productStoreId", productStoreId);
        } catch (Exception e) {
            return ServiceUtil.returnFailure("Error from deleteContactAlert service "+ e);
        }
        return result;
    }
}
