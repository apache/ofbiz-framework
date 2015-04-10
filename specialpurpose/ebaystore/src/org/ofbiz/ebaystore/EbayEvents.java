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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilGenerics;
import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.util.EntityQuery;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;

import com.ebay.sdk.ApiContext;
import com.ebay.sdk.ApiException;
import com.ebay.sdk.SdkException;
import com.ebay.sdk.call.AddItemCall;
import com.ebay.sdk.call.GetCategorySpecificsCall;
import com.ebay.sdk.call.GetSellingManagerInventoryCall;
import com.ebay.sdk.call.ReviseSellingManagerProductCall;
import com.ebay.sdk.call.VerifyAddItemCall;
import com.ebay.soap.eBLBaseComponents.AmountType;
import com.ebay.soap.eBLBaseComponents.CategoryType;
import com.ebay.soap.eBLBaseComponents.CountryCodeType;
import com.ebay.soap.eBLBaseComponents.CurrencyCodeType;
import com.ebay.soap.eBLBaseComponents.DetailLevelCodeType;
import com.ebay.soap.eBLBaseComponents.FeeType;
import com.ebay.soap.eBLBaseComponents.FeesType;
import com.ebay.soap.eBLBaseComponents.GetSellingManagerInventoryRequestType;
import com.ebay.soap.eBLBaseComponents.GetSellingManagerInventoryResponseType;
import com.ebay.soap.eBLBaseComponents.ItemType;
import com.ebay.soap.eBLBaseComponents.ListingDesignerType;
import com.ebay.soap.eBLBaseComponents.ListingTypeCodeType;
import com.ebay.soap.eBLBaseComponents.NameRecommendationType;
import com.ebay.soap.eBLBaseComponents.NameValueListArrayType;
import com.ebay.soap.eBLBaseComponents.NameValueListType;
import com.ebay.soap.eBLBaseComponents.PictureDetailsType;
import com.ebay.soap.eBLBaseComponents.RecommendationsType;
import com.ebay.soap.eBLBaseComponents.ReturnPolicyType;
import com.ebay.soap.eBLBaseComponents.ReviseSellingManagerProductRequestType;
import com.ebay.soap.eBLBaseComponents.ReviseSellingManagerProductResponseType;
import com.ebay.soap.eBLBaseComponents.SellingManagerProductDetailsType;
import com.ebay.soap.eBLBaseComponents.SellingManagerProductType;
import com.ebay.soap.eBLBaseComponents.ShippingDetailsType;
import com.ebay.soap.eBLBaseComponents.ShippingServiceDetailsType;
import com.ebay.soap.eBLBaseComponents.ShippingServiceOptionsType;
import com.ebay.soap.eBLBaseComponents.ShippingTypeCodeType;
import com.ebay.soap.eBLBaseComponents.SiteCodeType;
import com.ebay.soap.eBLBaseComponents.StoreCustomCategoryType;
import com.ebay.soap.eBLBaseComponents.StorefrontType;
import com.ebay.soap.eBLBaseComponents.VATDetailsType;
import com.ebay.soap.eBLBaseComponents.ValueRecommendationType;
import com.ebay.soap.eBLBaseComponents.VerifyAddItemRequestType;
import com.ebay.soap.eBLBaseComponents.VerifyAddItemResponseType;
import com.ebay.soap.eBLBaseComponents.WarningLevelCodeType;
import com.ebay.soap.eBLBaseComponents.AddItemRequestType;
import com.ebay.soap.eBLBaseComponents.AddItemResponseType;

public class EbayEvents {

    private static final int SHIPPING_SERVICE_ID_LIMIT = 50000;
    public static final String module = EbayEvents.class.getName();

    public static String sendLeaveFeedback(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession(true);
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        Map<String, Object> requestParams = UtilHttp.getParameterMap(request);
        GenericValue userLogin = (GenericValue) session.getAttribute("userLogin");
        int feedbackSize = Integer.parseInt((String)requestParams.get("feedbackSize"));
        String productStoreId = (String)requestParams.get("productStoreId");
        for (int i = 1; i <= feedbackSize; i++) {
            String commentType = (String)requestParams.get("commentType"+i);
            String commentText = (String)requestParams.get("commentText"+i);
            if (!commentType.equals("none") && commentText != null) {
                String itemId = (String)requestParams.get("itemId"+i);
                String transactionId = (String)requestParams.get("transactionId"+i);
                String targetUser = (String)requestParams.get("targetUser"+i);
                String commentingUser = (String)requestParams.get("commentingUser"+i);
                String role = (String)requestParams.get("role"+i);
                String ratingItem = (String)requestParams.get("ratingItem"+i);
                String ratingComm = (String)requestParams.get("ratingComm"+i);
                String ratingShip = (String)requestParams.get("ratingShip"+i);
                String ratingShipHand = (String)requestParams.get("ratingShipHand"+i);
                String AqItemAsDescribedId = (String)requestParams.get("AqItemAsDescribedId"+i);

                Map<String, Object> leavefeedback =  new HashMap<String, Object>();
                leavefeedback.put("productStoreId", productStoreId);
                leavefeedback.put("userLogin", userLogin);
                leavefeedback.put("itemId", itemId);
                leavefeedback.put("transactionId", transactionId);
                leavefeedback.put("targetUser", targetUser);
                leavefeedback.put("commentingUser", commentingUser);
                leavefeedback.put("role", role);
                leavefeedback.put("commentText", commentText);
                leavefeedback.put("commentType", commentType);
                leavefeedback.put("ratingItem", ratingItem);
                leavefeedback.put("ratingComm", ratingComm);
                leavefeedback.put("ratingShip", ratingShip);
                leavefeedback.put("ratingShipHand", ratingShipHand);
                leavefeedback.put("AqItemAsDescribedId", AqItemAsDescribedId);
                // Call service
                try {
                    dispatcher.runSync("leaveFeedback", leavefeedback);
                } catch (GenericServiceException e) {
                    Debug.logError(e, module);
                    request.setAttribute("_ERROR_MESSAGE_","Exception: ".concat(e.getMessage()));
                    return "error";
                }
            }
        }
        return "success";
    }
    /* event to add products to prepare create & export listing */
    public static String addProductListing(HttpServletRequest request, HttpServletResponse response) {
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        Map<String,Object> requestParams = UtilHttp.getParameterMap(request);
        Locale locale = UtilHttp.getLocale(request);

        if (UtilValidate.isEmpty(requestParams.get("productStoreId"))) {
            request.setAttribute("_ERROR_MESSAGE_","Required productStoreId and selected products.");
            return "error";
        }
        List<String> productIds = UtilGenerics.checkList(requestParams.get("productIds"));
        if (UtilValidate.isNotEmpty(requestParams.get("productIds"))) {
            productIds = UtilGenerics.checkList(requestParams.get("productIds"));
        } else if (UtilValidate.isNotEmpty(requestParams.get("selectResult"))) {
            try {
                productIds = UtilGenerics.checkList(requestParams.get("selectResult"));
            } catch (ClassCastException e) {
                if (UtilValidate.isEmpty(productIds)) productIds = new LinkedList<String>();
                productIds.add((String) requestParams.get("selectResult"));
            }
        } else {
            request.setAttribute("_ERROR_MESSAGE_","Required productStoreId and selected products.");
            return "error";
        }
        String productStoreId = (String) requestParams.get("productStoreId");

        ApiContext apiContext = EbayStoreHelper.getApiContext(productStoreId, locale, delegator);
        //String webSiteUrl = (String) requestParams.get("webSiteUrl");
        String webSiteUrl = "http://demo-trunk.ofbiz.apache.org";
        Map<String,Object> addItemObject = getAddItemListingObject(request, apiContext);
        List<Map<String,Object>> itemObjs = null;
        if (UtilValidate.isNotEmpty(addItemObject.get("itemListings"))) {
            itemObjs = UtilGenerics.checkList(addItemObject.get("itemListings"));
        } else {
            itemObjs = new LinkedList<Map<String,Object>>();
        }

        if (UtilValidate.isNotEmpty(productIds)) {
            try {
                // check  add new product obj ? to export 
                for (String productId : productIds) {
                    for (Map<String,Object> itObj : itemObjs) {
                        if (UtilValidate.isNotEmpty(itObj.get(productId.concat("_Obj")))) {
                            productIds.remove(productId);
                        }
                    }
                }
                Debug.logInfo("run in with productIds "+productIds, module);
                for (String productId : productIds) {
                    AddItemCall addItemCall = new AddItemCall(apiContext);
                    ItemType item = new ItemType();
                    GenericValue product = EntityQuery.use(delegator).from("Product").where("productId", productId).queryOne();
                    item.setTitle(product.getString("internalName"));
                    item.setCurrency(CurrencyCodeType.USD);
                    String productDescription = "";
                    String description = product.getString("description");
                    String longDescription = product.getString("longDescription");
                    if (UtilValidate.isNotEmpty(description)) {
                        productDescription = description;
                    } else if (UtilValidate.isNotEmpty(longDescription)) {
                        productDescription = longDescription;
                    } else if (UtilValidate.isNotEmpty(product.getString("productName"))) {
                        productDescription = product.getString("productName");
                    }
                    item.setDescription(productDescription);
                    item.setSKU(product.getString("productId"));
                    item.setApplicationData(product.getString("productId"));
                    item.setCountry(CountryCodeType.US);
                    item.setQuantity(1);
                    String smallImage = product.getString("smallImageUrl");
                    String mediumImage = product.getString("mediumImageUrl");
                    String largeImage = product.getString("largeImageUrl");
                    String ebayImage = null;
                    if (UtilValidate.isNotEmpty(largeImage)) {
                        ebayImage = largeImage;
                    } else if (UtilValidate.isNotEmpty(mediumImage)) {
                        ebayImage = mediumImage;
                    } else if (UtilValidate.isNotEmpty(smallImage)) {
                        ebayImage = smallImage;
                    }
                    if (UtilValidate.isNotEmpty(ebayImage)) {
                        PictureDetailsType pic = new PictureDetailsType();
                        String pictureUrl = webSiteUrl + ebayImage;
                        String[] picURL = new String[1];
                        picURL[0] = pictureUrl;
                        //String[] picURL = {webSiteUrl + ebayImage};
                        //pic.setPictureURL(picURL);
                        pic.setPictureURL(picURL);
                        item.setPictureDetails(pic);
                    }
                    item.setCategoryMappingAllowed(true);
                    item.setSite(apiContext.getSite());
                    addItemCall.setSite(apiContext.getSite());
                    addItemCall.setItem(item);
                    addItemCall.setWarningLevel(WarningLevelCodeType.HIGH);

                    Map<String,Object> itemListing = null;
                    for (Map<String,Object> itemObj : itemObjs) {
                        if (UtilValidate.isNotEmpty(itemObj.get(productId.concat("_Obj")))) {
                            itemListing = UtilGenerics.checkMap(itemObj.get(productId.concat("_Obj")));
                            itemListing.put("addItemCall", addItemCall);
                            itemListing.put("productId", productId);
                            break;
                        }
                    }
                    if (UtilValidate.isEmpty(itemListing)) {
                        itemListing = new HashMap<String, Object>();
                        itemListing.put("addItemCall", addItemCall);
                        itemListing.put("productId", productId);
                    }
                    itemObjs.add(itemListing);
                }
                addItemObject.put("itemListing", itemObjs);
            } catch (Exception e) {
                Debug.logError(e.getMessage(), module);
                request.setAttribute("_ERROR_MESSAGE_","Exception: ".concat(e.getMessage()));
                return "error";
            }
        }
        return "success";
    }

    public static String prepareEbaySiteFacadeObject(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession(true);
        if (request.getParameter("productStoreId") == null) {
            return "error";
        }
        ApiContext apiContext = getApiContext(request);
        try {
            if (UtilValidate.isNotEmpty(apiContext)) {
                String siteCode = apiContext.getSite().name();
                if (UtilValidate.isNotEmpty(session.getAttribute("itemListings_".concat(siteCode)))) {
                    request.setAttribute("productStoreId", request.getParameter("productStoreId"));
                    return "prepare";
                }
                getSiteFacade(apiContext,request);
            } else {
                request.setAttribute("_ERROR_MESSAGE_","No apiContext for this account or this site please register on ebay or check you user account.");
                return "error";
            }
        } catch (ApiException e) {
            request.setAttribute("_ERROR_MESSAGE_","ApiException ".concat(e.getMessage()));
            return "error";
        } catch (SdkException e) {
            request.setAttribute("_ERROR_MESSAGE_","SdkException ".concat(e.getMessage()));
            return "error";
        } catch (Exception e) {
            request.setAttribute("_ERROR_MESSAGE_","Exception ".concat(e.getMessage()));
            return "error";
        }
        return "success";
    }

    public static EbayStoreSiteFacade getSiteFacade(ApiContext apiContext, HttpServletRequest request) throws ApiException, SdkException, Exception{
        String siteFacadeName = null;
        EbayStoreSiteFacade siteFacade = null;

        if (request.getParameter("productStoreId") == null) {
            Debug.logError("Required productStoreId for get ebay information.", module);
            return siteFacade;
        }

        HttpSession session = request.getSession(true);
        if (UtilValidate.isNotEmpty(apiContext)) {
            siteFacadeName = "siteFacade".concat("_".concat(apiContext.getSite().name()));
        }

        if (UtilValidate.isEmpty(session.getAttribute(siteFacadeName))) {
            session.setAttribute(siteFacadeName,new EbayStoreSiteFacade(apiContext));
            if (UtilValidate.isNotEmpty(session.getAttribute(siteFacadeName))) {
                siteFacade = (EbayStoreSiteFacade)session.getAttribute(siteFacadeName);
            }
        } else {
            siteFacade = (EbayStoreSiteFacade)session.getAttribute(siteFacadeName);
        }
        Debug.logInfo("loaded session for ebay site Facade is ".concat(siteFacadeName).concat(session.getAttribute(siteFacadeName).toString()),module);
        return siteFacade;
    }

    public static ApiContext getApiContext(HttpServletRequest request) {
        Locale locale = UtilHttp.getLocale(request);
        Delegator delegator = (Delegator) request.getAttribute("delegator");

        if (request.getParameter("productStoreId") == null && request.getAttribute("productStoreId") == null) {
            Debug.logError("Required productStoreId for get ebay API config data.", module);
            return null;
        }
        ApiContext apiContext = EbayStoreHelper.getApiContext(request.getParameter("productStoreId") != null ? request.getParameter("productStoreId"):(String)request.getAttribute("productStoreId"), locale, delegator);
        return apiContext;
    }

    public static String clearProductListing(HttpServletRequest request, HttpServletResponse response) {
        removeItemListingObject(request, getApiContext(request));
        return "success";
    }

    public static void removeItemListingObject(HttpServletRequest request, ApiContext apiContext) {
        HttpSession session = request.getSession(true);
        String siteCode = apiContext.getSite().name();
        Map<String,Object> addItemObject = UtilGenerics.checkMap(session.getAttribute("itemListings_".concat(siteCode)));
        if (UtilValidate.isNotEmpty(addItemObject)) {
            session.removeAttribute("itemListings_".concat(siteCode));
        }
    }

    public static Map<String,Object> getAddItemListingObject(HttpServletRequest request, ApiContext apiContext) {
        String siteCode = apiContext.getSite().name();
        Map<String,Object> addItemObject = UtilGenerics.checkMap(request.getAttribute("itemListings_".concat(siteCode)));
        HttpSession session = request.getSession(true);
        if (addItemObject == null) {
            addItemObject = UtilGenerics.checkMap(session.getAttribute("itemListings_".concat(siteCode)));
        } else {
            session.setAttribute("itemListings_".concat(siteCode), addItemObject);
        }
        if (addItemObject == null) {
            addItemObject = new HashMap<String, Object>();
            session.setAttribute("itemListings_".concat(siteCode), addItemObject);
        }
        return addItemObject;
    }

    // make ebay category list
    public static List<CategoryType> getChildCategories(HttpServletRequest request) throws ApiException, SdkException, Exception{
        List<CategoryType> categories = new LinkedList<CategoryType>();
        EbayStoreSiteFacade sf = null;
        String categoryId = null;

        if (request.getParameter("productStoreId") == null && request.getAttribute("productStoreId") == null) {
            Debug.logError("Required productStoreId for get ebay LeafCategories.", module);
            return categories;
        }
        if (request.getParameter("categoryId") != null || request.getAttribute("categoryId") != null) {
            categoryId = request.getParameter("categoryId") != null ? request.getParameter("categoryId") : (String) request.getAttribute("categoryId");
            Debug.logInfo("Load child categories from session following site id and categoryId is ".concat(categoryId), module);
        } else {
            Debug.logWarning("No categoryId to get child categories.", module);
        }

        ApiContext apiContext = getApiContext(request);
        sf = getSiteFacade(apiContext,request);
        if (UtilValidate.isNotEmpty(sf)) {
            Map<SiteCodeType, List<CategoryType>> csCateMaps = sf.getSiteCategoriesCSMap();
            List<CategoryType> csCateList = csCateMaps.get(apiContext.getSite());
            if (UtilValidate.isNotEmpty(csCateList)) {
                if (UtilValidate.isNotEmpty(categoryId)) {
                    // find child of selected ebay categories 
                    for (CategoryType csCate : csCateList) {
                        String[] categoryParentIds = csCate.getCategoryParentID();
                        for (String categoryParentId : categoryParentIds) {
                            if (categoryId.equals(categoryParentId)) {
                                categories.add(csCate);
                            }
                        }
                    }
                } else {
                    // find first level of ebay categories
                    for (CategoryType csCate : csCateList) {
                        String[] categoryParentIds = csCate.getCategoryParentID();
                        for (String categoryParentId : categoryParentIds) {
                            if (csCate.getCategoryID().equals(categoryParentId)) {
                                categories.add(csCate);
                            }
                        }
                    }
                }
                //sort the cats list
                Collections.sort(categories, new Comparator<Object>() {
                    public int compare(Object a, Object b) {
                        CategoryType cat1 = (CategoryType)a;
                        CategoryType cat2 = (CategoryType)b;
                        int catId1 = Integer.parseInt(cat1.getCategoryID());
                        int catId2 = Integer.parseInt(cat2.getCategoryID());
                        return catId1 - catId2;
                    }
                });
            }
        }
        return categories;
    }

    public static List<StoreCustomCategoryType> getStoreChildCategories(HttpServletRequest request) throws ApiException, SdkException, Exception {
        List<StoreCustomCategoryType> categories = new LinkedList<StoreCustomCategoryType>();
        List<StoreCustomCategoryType> csCateList = new LinkedList<StoreCustomCategoryType>();
        EbayStoreSiteFacade sf = null;
        String categoryId = null;

        if (UtilValidate.isEmpty(request.getParameter("productStoreId")) && UtilValidate.isEmpty(request.getAttribute("productStoreId"))) {
            Debug.logError("Required productStoreId for get ebay LeafCategories.", module);
            return categories;
        }
        if (UtilValidate.isNotEmpty(request.getParameter("categoryId")) || UtilValidate.isNotEmpty(request.getAttribute("categoryId"))) {
            categoryId = request.getParameter("categoryId") != null ? request.getParameter("categoryId") : (String) request.getAttribute("categoryId");
            Debug.logInfo("Load child categories from session following site id and categoryId is ".concat(categoryId), module);
        } else {
            Debug.logWarning("No categoryId to get child categories.", module);
        }

        ApiContext apiContext = getApiContext(request);
        sf = getSiteFacade(apiContext,request);
        if (UtilValidate.isNotEmpty(sf)) {
            Map<SiteCodeType, List<StoreCustomCategoryType>> csCateMaps = sf.getSiteStoreCategoriesMap();
            csCateList = csCateMaps.get(apiContext.getSite());
            if (UtilValidate.isNotEmpty(csCateList)) {
                if (UtilValidate.isNotEmpty(categoryId)) {
                    // find child of selected ebay categories 
                    for (StoreCustomCategoryType csCate : csCateList) {
                        if (categoryId.equals(String.valueOf(csCate.getCategoryID()))) {
                            StoreCustomCategoryType [] childCategories = csCate.getChildCategory();
                            for (StoreCustomCategoryType childCategoryId : childCategories) {
                                categories.add(childCategoryId);
                            }
                        }
                    }
                } else {
                    // find first level of ebay categories
                    for (StoreCustomCategoryType csCate : csCateList) {
                        categories.add(csCate);
                    }
                }
                //sort the cats list
                Collections.sort(categories, new Comparator<Object>() {
                    public int compare(Object a, Object b) {
                        StoreCustomCategoryType cat1 = (StoreCustomCategoryType) a;
                        StoreCustomCategoryType cat2 = (StoreCustomCategoryType) b;
                        int catId1 = Integer.parseInt(Long.toString(cat1.getCategoryID()));
                        int catId2 = Integer.parseInt(Long.toString(cat2.getCategoryID()));
                        return catId1 - catId2;
                    }
                });
            }
        }
        return categories;
    }

    public static CategoryType getCsCategoriesMapped(HttpServletRequest request) throws ApiException, SdkException, Exception{
        EbayStoreSiteFacade sf = null;
        String categoryId = null;
        CategoryType cate = null;

        if (request.getParameter("productStoreId") == null && request.getAttribute("productStoreId") == null) {
            Debug.logError("Required productStoreId for get ebay LeafCategories.", module);
            return null;
        }
        if (request.getParameter("categoryId") != null || request.getAttribute("categoryId") != null) {
            categoryId = request.getParameter("categoryId") != null ? request.getParameter("categoryId") : (String)request.getAttribute("categoryId");
            Debug.logInfo("Load child categories from session following site id and categoryId is ".concat(categoryId), module);
        } else {
            Debug.logWarning("No categoryId to get child categories.", module);
            return null;
        }

        ApiContext apiContext = getApiContext(request);
        sf = getSiteFacade(apiContext,request);
        if (UtilValidate.isNotEmpty(sf)) {
            Map<SiteCodeType, List<CategoryType>> csCateMaps = sf.getSiteCategoriesCSMap();
            List<CategoryType> csCateList = csCateMaps.get(apiContext.getSite());
            if (UtilValidate.isNotEmpty(csCateList)) {
                if (UtilValidate.isNotEmpty(categoryId)) {
                    // find child of selected ebay categories 
                    for (CategoryType csCate : csCateList) {
                        if (csCate.getCategoryID().equals(categoryId)) {
                            cate = csCate;
                            break;
                        }
                    }
                } 
            }
        }
        return cate;
    }

    public static String setSelectedEbayCategory(HttpServletRequest request, HttpServletResponse response) {
        Map<String,Object> requestParams = UtilHttp.getParameterMap(request);
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        if (UtilValidate.isEmpty(requestParams.get("productStoreId"))) {
            request.setAttribute("_ERROR_MESSAGE_","Required productStoreId.");
            return "error";
        }
        if (UtilValidate.isEmpty(requestParams.get("isProductId"))) {
            request.setAttribute("_ERROR_MESSAGE_","Required can not find form Id.");
            return "error";
        }
        String isProductId = (String) requestParams.get("isProductId");
        if (UtilValidate.isEmpty(requestParams.get("productId")) || UtilValidate.isEmpty(requestParams.get("ebayCategory"))) {
            request.setAttribute("_ERROR_MESSAGE_","No ebay category or productId selected with form id ".concat(isProductId));
            return "error";
        }
        String categoryId = (String)requestParams.get("ebayCategory");
        if (categoryId.contains("false")) {
            request.setAttribute("_ERROR_MESSAGE_","Please select ebay category with low level of categories.");
            return "error";
        } else {
            if (categoryId.contains("true")) categoryId = categoryId.substring(0,categoryId.indexOf(":"));
        }
        String productId = (String) requestParams.get("isProductId");
        EbayStoreCategoryFacade cf = null;
        EbayStoreSiteFacade sf = null;
        // find is exiting product and set category into item in additem call
        try {
            if (UtilValidate.isNotEmpty(EntityQuery.use(delegator).from("Product").where("productId", productId).queryOne())) {
                ApiContext apiContext = getApiContext(request);
                Map<String,Object> addItemObject = getAddItemListingObject(request, apiContext);
                List<Map<String,Object>> addItemlist = UtilGenerics.checkList(addItemObject.get("itemListing"));

                if (UtilValidate.isNotEmpty(addItemlist)) {
                    for (Map<String,Object> addItemCall : addItemlist) {
                        AddItemCall itemCall = (AddItemCall) addItemCall.get("addItemCall");
                        ItemType item = itemCall.getItem();
                        if (productId.equals(item.getSKU())) {
                            request.setAttribute("categoryId", categoryId);
                            CategoryType csCate = getCsCategoriesMapped(request);
                            if (UtilValidate.isNotEmpty(csCate)) {
                                Debug.logInfo("Set selected ebay category ".concat(csCate.getCategoryID().toString().concat(csCate.getCategoryName()).concat(String.valueOf((csCate.isLeafCategory())))), module);
                                item.setPrimaryCategory(csCate);
                                // get category feature and attributes
                                sf = getSiteFacade(apiContext, request);
                                String refName = "itemCateFacade_".concat(csCate.getCategoryID());
                                if (UtilValidate.isEmpty(addItemObject.get(refName))) {
                                    cf = new EbayStoreCategoryFacade(csCate.getCategoryID(), apiContext, sf.getAttrMaster(), sf);
                                    addItemObject.put(refName, cf);
                                }
                                request.setAttribute("_EVENT_MESSAGE_","Set selected ebay category ".concat(csCate.getCategoryID().toString()).concat(" with product ".concat(productId).concat(" successed."))); 
                            } else {
                                Debug.logWarning(categoryId.concat(" This category is not leaf category or ?"), module);
                            }
                            break;
                        }
                    }
                }
            }
        } catch (GenericEntityException e) {
            Debug.logError(e.getMessage(), module);
        } catch (ApiException e) {
            Debug.logError(e.getMessage(), module);
        } catch (SdkException e) {
            Debug.logError(e.getMessage(), module);
        } catch (Exception e) {
            Debug.logError(e.getMessage(), module);
        }
        request.setAttribute("productStoreId", requestParams.get("productStoreId"));
        request.setAttribute("isProductId", productId);
        request.removeAttribute("categoryId");
        return "success";
    }

    /* for shipping service detail filter */
    private static boolean isFlat(ShippingTypeCodeType[] st) {
        for (int i = 0; i < st.length; i++) {
            if (st[i].compareTo(ShippingTypeCodeType.FLAT) == 0) {
                return true;
            }
        }
        return false;
    }

    public static ShippingServiceDetailsType[] filterShippingService(ShippingServiceDetailsType[] array) {
        ArrayList<ShippingServiceDetailsType> list = new ArrayList<ShippingServiceDetailsType>();
        for (int i = 0; i < array.length; i++) {
            if (isFlat(array[i].getServiceType()) && array[i].getShippingServiceID() < SHIPPING_SERVICE_ID_LIMIT) {
                list.add(array[i]);
            }
        }
        return list.toArray(new ShippingServiceDetailsType[0]);
    }

    public static String updateProductExportDetail(HttpServletRequest request, HttpServletResponse response) {
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        HttpSession session = request.getSession(true);
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        Map<String,Object> requestParams = UtilHttp.getParameterMap(request);
        Locale locale = UtilHttp.getLocale(request);
        GenericValue userLogin = (GenericValue) session.getAttribute("userLogin");
        HashMap<String, Object> attributeMapList = new HashMap<String, Object>();
        String id = "";
        if (UtilValidate.isNotEmpty(requestParams.get("listype"))) {
            if ("auction".equals(requestParams.get("listype"))) {
                id = "_1";
            } else {
                id = "_2";
            }
        }
        String startPrice = "";
        if (UtilValidate.isNotEmpty(requestParams.get("startPrice".concat(id)))) {
            startPrice = (String) requestParams.get("startPrice".concat(id));
        }
        String buyItNowPrice = "";
        if (UtilValidate.isNotEmpty(requestParams.get("buyItNowPrice".concat(id)))) {
            buyItNowPrice = (String) requestParams.get("buyItNowPrice".concat(id));
        }
        String productId = null;
        if (UtilValidate.isNotEmpty(requestParams.get("productId"))) {
            productId = requestParams.get("productId").toString();
        }

        String itemPkCateId = (String) requestParams.get("primaryCateId");
        String shippingService = (String) requestParams.get("ShippingService");
        String productStoreId = (String) requestParams.get("productStoreId");
        
        // initialize request parameter.
        Map<String, Object> paramMap = UtilHttp.getParameterMap(request);
        List<String> nameSpecificList = new LinkedList<String>();
        List<String> valueSpecificList = new LinkedList<String>();
        String nameValueListType = null;
        String valueListType = null;
        int rowCount = UtilHttp.getMultiFormRowCount(paramMap);
        if (rowCount > 1) {
            for (int i = 0; i < rowCount; i++) {
                String thisSuffix = UtilHttp.MULTI_ROW_DELIMITER + i;        // current suffix after each field id
                // get the NameSpecifics
                if (paramMap.containsKey("nameValueListType" + thisSuffix)) {
                    nameValueListType = (String) paramMap.remove("nameValueListType" + thisSuffix);
                }
                // get the ValueSpecifics
                if (paramMap.containsKey("categorySpecifics" + thisSuffix)) {
                    valueListType = (String) paramMap.remove("categorySpecifics" + thisSuffix);
                }
                
                if ((UtilValidate.isNotEmpty(nameValueListType)) && (UtilValidate.isNotEmpty(valueListType))){
                   nameSpecificList.add(nameValueListType);
                   valueSpecificList.add(valueListType);
                }
            }
        }
            
        try {
            ApiContext apiContext = EbayStoreHelper.getApiContext(productStoreId, locale, delegator);
            Map<String,Object> addItemObject = getAddItemListingObject(request, apiContext);
            List<Map<String,Object>> listAddItem = null;
            if (UtilValidate.isNotEmpty(addItemObject.get("itemListing"))) {
                listAddItem = UtilGenerics.checkList(addItemObject.get("itemListing"));
            } else {
                listAddItem = new LinkedList<Map<String,Object>>();
            }

            for (Map<String,Object> itemObj : listAddItem) {
                AddItemCall addItemCall = (AddItemCall) itemObj.get("addItemCall");
                ItemType item = addItemCall.getItem();
                String SKU = item.getSKU();
                if (UtilValidate.isNotEmpty(productId)) {
                    if (productId.equals(SKU)) {

                        attributeMapList.put("Title", item.getTitle());
                        attributeMapList.put("SKU", SKU);
                        attributeMapList.put("Currency", item.getCurrency().value());
                        attributeMapList.put("Description", item.getDescription());
                        attributeMapList.put("ApplicationData", item.getApplicationData());
                        attributeMapList.put("Country", item.getCountry().value());
                        attributeMapList.put("PictureURL", item.getPictureDetails().getPictureURL(0));
                        attributeMapList.put("Site", item.getSite().value());
                        attributeMapList.put("UseTaxTable", "false");
                        attributeMapList.put("BestOfferEnabled", "true");
                        attributeMapList.put("AutoPayEnabled", "true");
                        attributeMapList.put("CategoryID", item.getPrimaryCategory().getCategoryID());
                        attributeMapList.put("CategoryLevel", item.getPrimaryCategory().getCategoryLevel());
                        attributeMapList.put("CategoryName", item.getPrimaryCategory().getCategoryName());
                        attributeMapList.put("CategoryParentID", item.getPrimaryCategory().getCategoryParentID(0).toString());
                        attributeMapList.put("LeafCategory", "true");
                        attributeMapList.put("LSD", "true");

                        // set Item Specifics.
                        int itemSpecificsSize = nameSpecificList.size();
                        int valueSpecificsSize = valueSpecificList.size();
                        if ((itemSpecificsSize > 0) && (valueSpecificsSize > 0)) {
                            NameValueListArrayType nameValueListArray = new NameValueListArrayType();
                            NameValueListType[] nameValueListTypes = new NameValueListType[nameSpecificList.size()];
                            for (int i = 0; i < itemSpecificsSize; i++) {
                                String name = nameSpecificList.get(i);
                                String value = valueSpecificList.get(i);
                                String[] valueArray = new String[] { value };
                                
                                // set Name value list type.
                                NameValueListType listType = new NameValueListType();
                                listType.setName(name);
                                listType.setValue(valueArray);
                                nameValueListTypes[i] = listType;
                            }
                            nameValueListArray.setNameValueList(nameValueListTypes);
                            item.setItemSpecifics(nameValueListArray);
                        }

                        item.setUseTaxTable(false);
                        item.setDispatchTimeMax(3);
                        ReturnPolicyType policy = new ReturnPolicyType();
                        policy.setReturnsAcceptedOption("ReturnsNotAccepted");
                        item.setReturnPolicy(policy);
                        attributeMapList.put("ReturnsAcceptedOption", "ReturnsNotAccepted");

                        String currencyId = "";
                        if (UtilValidate.isNotEmpty(requestParams.get("currencyId".concat(id)))) {
                            currencyId = (String) requestParams.get("currencyId".concat(id));
                        }
                        if (UtilValidate.isNotEmpty(requestParams.get("enabledTheme")) && "Y".equals(requestParams.get("enabledTheme"))) {
                            ListingDesignerType designer = new ListingDesignerType();
                            String layoutId = (String) requestParams.get("themeGroup");
                            String themeIdImage = (String) requestParams.get("theme");
                            String themeId = themeIdImage.substring(0, themeIdImage.indexOf(":"));
                            designer.setLayoutID(Integer.parseInt(layoutId));
                            designer.setThemeID(Integer.parseInt(themeId));
                            item.setListingDesigner(designer);
                            attributeMapList.put("LayoutID", item.getListingDesigner().getLayoutID());
                            attributeMapList.put("ThemeID", item.getListingDesigner().getThemeID());
                        }
                        if ("_1".equals(id)) {
                            item.setListingType(ListingTypeCodeType.CHINESE);
                            AmountType amtStart = new AmountType();
                            amtStart.setCurrencyID(CurrencyCodeType.valueOf(currencyId));
                            amtStart.setValue(Double.parseDouble(startPrice));
                            item.setStartPrice(amtStart);

                            AmountType amtBIN = new AmountType();
                            amtBIN.setCurrencyID(CurrencyCodeType.valueOf(currencyId));
                            amtBIN.setValue(Double.parseDouble(buyItNowPrice));
                            item.setBuyItNowPrice(amtBIN);
                            attributeMapList.put("BuyItNowPrice", item.getBuyItNowPrice().getValue());

                            if (UtilValidate.isNotEmpty(requestParams.get("reservePrice".concat(id)))) {
                                AmountType amtResv = new AmountType();
                                amtResv.setCurrencyID(CurrencyCodeType.valueOf(currencyId));
                                amtResv.setValue(Double.parseDouble(requestParams.get("reservePrice".concat(id)).toString()));
                                item.setReservePrice(amtResv);
                                attributeMapList.put("ReservePrice", item.getReservePrice().getValue());
                            }
                        } else if ("_2".equals(id)) {
                            item.setListingType(ListingTypeCodeType.FIXED_PRICE_ITEM);
                            AmountType amtBIN = new AmountType();
                            amtBIN.setCurrencyID(CurrencyCodeType.valueOf(currencyId));
                            amtBIN.setValue(Double.parseDouble(startPrice));
                            item.setStartPrice(amtBIN);
                            if (UtilValidate.isNotEmpty(requestParams.get("enableBestOffer".concat(id)))) {
                                item.setBestOfferEnabled(Boolean.valueOf(requestParams.get("enableBestOffer".concat(id)).toString()));
                            }
                        }
                        attributeMapList.put("ListingType", item.getListingType().value());
                        attributeMapList.put("StartPrice", item.getStartPrice().getValue());

                        EbayStoreHelper.mappedPaymentMethods(requestParams, itemPkCateId, addItemObject, item, attributeMapList);

                        ShippingDetailsType shippingDetail = new ShippingDetailsType();
                        ShippingServiceOptionsType[] shippingOptions = new ShippingServiceOptionsType[1];
                        ShippingServiceOptionsType shippingOption = new ShippingServiceOptionsType();
                        shippingOption.setShippingServicePriority(1);
                        shippingOption.setShippingService(shippingService);
                        AmountType amtServiceCost = new AmountType();
                        amtServiceCost.setValue(5.0);
                        amtServiceCost.setCurrencyID(CurrencyCodeType.USD);
                        shippingOption.setShippingServiceCost(amtServiceCost);
                        shippingOptions[0] = shippingOption;
                        shippingDetail.setShippingType(ShippingTypeCodeType.FLAT);
                        shippingDetail.setShippingServiceOptions(shippingOptions);
                        item.setShippingDetails(shippingDetail);
                        attributeMapList.put("ShippingService", shippingService);
                        attributeMapList.put("ShippingServiceCost", ""+5.0);
                        attributeMapList.put("ShippingServiceCostCurrency", "USD");
                        attributeMapList.put("ShippingServicePriority", "1");
                        attributeMapList.put("ShippingType", "Flat");
                        attributeMapList.put("ShippingServiceAdditionalCost", amtServiceCost.getValue());

                        EbayStoreHelper.mappedShippingLocations(requestParams, item, apiContext, request, attributeMapList);

                        if (UtilValidate.isNotEmpty(requestParams.get("vatPercent".concat(id)))) {
                            VATDetailsType vat = new VATDetailsType();
                            vat.setVATPercent(new Float(requestParams.get("vatPercent".concat(id)).toString()));
                            item.setVATDetails(vat);
                            attributeMapList.put("VATPercent", vat);
                        }
                        if (UtilValidate.isNotEmpty(requestParams.get("location"))) {
                            item.setLocation(requestParams.get("location").toString());
                            attributeMapList.put("Location", requestParams.get("location").toString());
                        }
                        if (UtilValidate.isNotEmpty(requestParams.get("quantity".concat(id)))) {
                            item.setQuantity(Integer.parseInt(requestParams.get("quantity".concat(id)).toString()));
                            attributeMapList.put("Quantity", requestParams.get("quantity".concat(id)).toString());
                        }
                        if (UtilValidate.isNotEmpty(requestParams.get("duration".concat(id)))) {
                             item.setListingDuration(requestParams.get("duration".concat(id)).toString());
                             attributeMapList.put("ListingDuration", requestParams.get("duration".concat(id)).toString());
                        }
                        if (UtilValidate.isNotEmpty(requestParams.get("lotsize".concat(id)))) {
                            item.setLotSize(Integer.parseInt(requestParams.get("lotsize".concat(id)).toString()));
                            attributeMapList.put("LotSize", requestParams.get("lotsize".concat(id)).toString());
                        }
                        if (UtilValidate.isNotEmpty(requestParams.get("postalCode".concat(id)))) {
                            item.setPostalCode(requestParams.get("postalCode".concat(id)).toString());
                            attributeMapList.put("PostalCode", requestParams.get("postalCode".concat(id)).toString());
                        }
                        StorefrontType storeFront = new StorefrontType();
                        if (UtilValidate.isNotEmpty(requestParams.get("ebayStore1Category"))) {
                            String ebayStore1Category = (String)requestParams.get("ebayStore1Category");
                            if (ebayStore1Category.contains("false")) {
                                request.setAttribute("_ERROR_MESSAGE_","Please select ebay store category with low level of categories.");
                                return "error";
                            } else {
                                if (ebayStore1Category.contains("true")) ebayStore1Category = ebayStore1Category.substring(0,ebayStore1Category.indexOf(":"));
                            }
                            storeFront.setStoreCategoryID(new Long(ebayStore1Category));
                            attributeMapList.put("StoreCategoryID", ebayStore1Category);

                        }
                        if (UtilValidate.isNotEmpty(requestParams.get("ebayStore2Category"))) {
                            String ebayStore2Category = (String)requestParams.get("ebayStore2Category");
                            if (ebayStore2Category.contains("false")) {
                                request.setAttribute("_ERROR_MESSAGE_","Please select ebay store category with low level of categories.");
                                return "error";
                            } else {
                                if (ebayStore2Category.contains("true")) ebayStore2Category = ebayStore2Category.substring(0,ebayStore2Category.indexOf(":"));
                            }
                            storeFront.setStoreCategory2ID(new Long(ebayStore2Category));
                            attributeMapList.put("StoreCategory2ID", ebayStore2Category);
                        }
                        if (UtilValidate.isNotEmpty(requestParams.get("ebayStore1Category")) || UtilValidate.isNotEmpty(requestParams.get("ebayStore2Category"))) {
                            item.setStorefront(storeFront);
                        }
                        //TODO: set value of country and currency on the basis of request param values
                        item.setCountry(CountryCodeType.US);
                        attributeMapList.put("Country", "US");
                        item.setCurrency(CurrencyCodeType.USD);
                        attributeMapList.put("Currency", "USD");

                        if (UtilValidate.isNotEmpty(requestParams.get("requireEbayInventory")) && "Y".equals(requestParams.get("requireEbayInventory").toString())) {
                            GenericValue ebayProductStore = EntityQuery.use(delegator).from("EbayProductStoreInventory").where("productStoreId", productStoreId, "productId", productId).filterByDate().queryFirst();
                            if (UtilValidate.isNotEmpty(ebayProductStore)) {
                                String facilityId = ebayProductStore.getString("facilityId");
                                BigDecimal atp = ebayProductStore.getBigDecimal("availableToPromiseListing");
                                int intAtp = atp.intValue();
                                if ((facilityId != "")  && (intAtp != 0)) {
                                    int newAtp = intAtp - 1;
                                    Map<String,Object> inMap = new HashMap<String, Object>();
                                    inMap.put("productStoreId", productStoreId);
                                    inMap.put("facilityId", facilityId);
                                    inMap.put("productId", productId);
                                    inMap.put("availableToPromiseListing", new BigDecimal(newAtp));
                                    inMap.put("userLogin", userLogin);
                                    try {
                                        dispatcher.runSync("updateEbayProductStoreInventory", inMap);
                                    } catch (GenericServiceException ex) {
                                        Debug.logError(ex.getMessage(), module);
                                        request.setAttribute("_ERROR_MESSAGE_","Exception: ".concat(ex.getMessage()));
                                        return "error";
                                    }
                                    itemObj.put("requireEbayInventory", "Y");
                                }
                            }
                        }
                        addItemCall.setItem(item);

                        // create/update EbayProductListing entity
                        Map<String, Object> prodMap = new HashMap<String, Object>();
                        prodMap.put("productStoreId", productStoreId);
                        prodMap.put("productId", productId);
                        prodMap.put("userLogin", userLogin);
                        if (UtilValidate.isNotEmpty(requestParams.get("isAutoRelist"))) {
                            prodMap.put("autoRelisting", "Y");
                            itemObj.put("isAutoRelist", "Y");
                        }
                        try {
                            GenericValue storeRole = EntityQuery.use(delegator).from("ProductStoreRole").where("productStoreId", productStoreId, "roleTypeId", "EBAY_ACCOUNT").queryFirst();
                            if (UtilValidate.isNotEmpty(storeRole)) {
                                GenericValue eBayUserLogin = EntityQuery.use(delegator).from("UserLogin")
                                                                           .where("partyId", storeRole.get("partyId"))
                                                                           .queryFirst();
                                if (UtilValidate.isNotEmpty(eBayUserLogin)) {
                                    prodMap.put("userLoginId", eBayUserLogin.get("userLoginId").toString());
                                }
                            }
                        } catch (GenericEntityException ex) {
                            Debug.logError(ex.getMessage(), module);
                            request.setAttribute("_ERROR_MESSAGE_","Exception: ".concat(ex.getMessage()));
                            return "error";
                        }
                        String productListingId = null;
                        if (UtilValidate.isEmpty(itemObj.get("productListingId"))) {
                            try {
                                prodMap.put("statusId", "ITEM_CREATED");
                                Map<String, Object> result = dispatcher.runSync("createEbayProductListing", prodMap);
                                productListingId = result.get("productListingId").toString();
                                itemObj.put("productListingId", productListingId);
                                itemObj.put("isSaved", "Y");
                            } catch (GenericServiceException ex) {
                                Debug.logError(ex.getMessage(), module);
                                request.setAttribute("_ERROR_MESSAGE_","Exception: ".concat(ex.getMessage()));
                                return "error";
                            }
                        } else {
                            productListingId = itemObj.get("productListingId").toString();
                            prodMap.put("productListingId", productListingId);
                            try {
                                dispatcher.runSync("updateEbayProductListing", prodMap);
                            } catch (GenericServiceException ex) {
                                Debug.logError(ex.getMessage(), module);
                                request.setAttribute("_ERROR_MESSAGE_","Exception: ".concat(ex.getMessage()));
                                return "error";
                            }
                        }

                        // create/update EbayProductListingAttribute
                        if (UtilValidate.isNotEmpty(productListingId)) {
                            attributeMapList.put("productListingId", productListingId);
                            Map<String, Object> ebayProdAttrMap = new HashMap<String, Object>();
                            ebayProdAttrMap.put("productListingId", productListingId);
                            ebayProdAttrMap.put("userLogin", userLogin);
                            ebayProdAttrMap.put("attributeMapList", attributeMapList);
                            try {
                                dispatcher.runSync("setEbayProductListingAttribute", ebayProdAttrMap);
                            } catch (GenericServiceException ex) {
                                Debug.logError(ex.getMessage(), module);
                                request.setAttribute("_ERROR_MESSAGE_","Exception: ".concat(ex.getMessage()));
                                return "error";
                            }
                        }
                    }
                }
            }
            request.setAttribute("productStoreId", requestParams.get("productStoreId"));
        } catch(Exception e) {
            Debug.logError(e.getMessage(), module);
            request.setAttribute("_ERROR_MESSAGE_","Exception: ".concat(e.getMessage()));
            return "error";
        }
        return "success";
    }

    public static String verifyItemBeforeAddAndExportToEbay(HttpServletRequest request, HttpServletResponse response) {
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        Map<String,Object> requestParams = UtilHttp.getParameterMap(request);
        Locale locale = UtilHttp.getLocale(request);
        String productStoreId = (String) requestParams.get("productStoreId");
        HttpSession session = request.getSession(true);
        GenericValue userLogin = (GenericValue) session.getAttribute("userLogin");

        try {
            ApiContext apiContext = EbayStoreHelper.getApiContext(productStoreId, locale, delegator);
            VerifyAddItemRequestType req = new VerifyAddItemRequestType();
            VerifyAddItemResponseType resp = null;
            AddItemResponseType addItemResp = null;


            VerifyAddItemCall verifyCall = new VerifyAddItemCall(apiContext);
            Map<String,Object> addItemObject = getAddItemListingObject(request, apiContext);
            List<Map<String,Object>> listAddItem = null;
            if (UtilValidate.isNotEmpty(addItemObject.get("itemListing"))) {
                listAddItem = UtilGenerics.checkList(addItemObject.get("itemListing"));
            } else {
                listAddItem = new LinkedList<Map<String,Object>>();
            }
            double feesummary = 0.0;
            for (Map<String,Object> itemObj : listAddItem) {
                AddItemCall addItemCall = (AddItemCall) itemObj.get("addItemCall");
                ItemType item = addItemCall.getItem();
                String SKU = item.getSKU();
                if (UtilValidate.isNotEmpty(requestParams.get("productId"))) {
                    String productId = requestParams.get("productId").toString();
                    if (productId.equals(SKU)) {
                        req.setItem(item);
                        resp = (VerifyAddItemResponseType) verifyCall.execute(req);
                        if (resp != null && "SUCCESS".equals(resp.getAck().toString())) {
                            itemObj.put("isVerify", "Y");
                            FeesType feest = resp.getFees();
                            FeeType[] fees = feest.getFee();
                            for (FeeType fee : fees) {
                                double dfee = fee.getFee().getValue();
                                feesummary = feesummary + dfee;
                            }
                            //if item is verified then export it to ebay
                            AddItemRequestType addItemReq = new AddItemRequestType();
                            addItemReq.setItem(item);
                            addItemResp = (AddItemResponseType) addItemCall.execute(addItemReq);
                            if (addItemResp != null && "SUCCESS".equals(addItemResp.getAck().toString())) {
                                removeProductFromListing(request, response);
                            }
                        } else {
                            EbayStoreHelper.createErrorLogMessage(userLogin, dispatcher, productStoreId, resp.getAck().toString(), "Verify Item : verifyItemBeforeAdd", resp.getErrors(0).getLongMessage());
                        }
                    }
                }
            }
            //request.setAttribute("itemFee", feesummary);
            request.setAttribute("productStoreId", requestParams.get("productStoreId"));
        } catch (Exception e) {
            Debug.logError(e.getMessage(), module);
            request.setAttribute("_ERROR_MESSAGE_","Exception: ".concat(e.getMessage()));
            return "error";
        }
        return "success";
    }

    public static String removeProductFromListing(HttpServletRequest request, HttpServletResponse response) {
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        Map<String,Object> requestParams = UtilHttp.getParameterMap(request);
        Locale locale = UtilHttp.getLocale(request);
        String productStoreId = (String) requestParams.get("productStoreId");
        try {
            ApiContext apiContext = EbayStoreHelper.getApiContext(productStoreId, locale, delegator);
            Map<String,Object> addItemObject = getAddItemListingObject(request, apiContext);
            List<Map<String,Object>> listAddItem = null;
            if (UtilValidate.isNotEmpty(addItemObject.get("itemListing"))) {
                listAddItem = UtilGenerics.checkList(addItemObject.get("itemListing"));
            } else {
                listAddItem = new LinkedList<Map<String,Object>>();
            }
            int i = 0;
            for (Map<String,Object> itemObj : listAddItem) {
                AddItemCall addItemCall = (AddItemCall) itemObj.get("addItemCall");
                ItemType item = addItemCall.getItem();
                String SKU = item.getSKU();
                if (UtilValidate.isNotEmpty(requestParams.get("productId"))) {
                    String productId = requestParams.get("productId").toString();
                    if (productId.equals(SKU)) {
                        listAddItem.remove(i);
                    }
                }
                i++;
            }
            if (listAddItem.size() <=0) {
                removeItemListingObject(request, apiContext);
            }
            request.setAttribute("productStoreId", requestParams.get("productStoreId"));
        } catch (Exception e) {
            Debug.logError(e.getMessage(), module);
            request.setAttribute("_ERROR_MESSAGE_","Exception: ".concat(e.getMessage()));
            return "error";
        }
        return "success";
    }

    public static String exportListingToEbay(HttpServletRequest request, HttpServletResponse response) {
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        Map<String,Object> requestParams = UtilHttp.getParameterMap(request);
        Locale locale = UtilHttp.getLocale(request);
        String productStoreId = (String) requestParams.get("productStoreId");
        HttpSession session = request.getSession(true);
        GenericValue userLogin = (GenericValue) session.getAttribute("userLogin");
        try {
            ApiContext apiContext = EbayStoreHelper.getApiContext(productStoreId, locale, delegator);
            Map<String,Object> addItemObject = getAddItemListingObject(request, apiContext);
            List<Map<String,Object>> listAddItem = null;
            if (UtilValidate.isNotEmpty(addItemObject.get("itemListing"))) {
                listAddItem = UtilGenerics.checkList(addItemObject.get("itemListing"));
            } else {
                listAddItem = new LinkedList<Map<String,Object>>();
            }
            for (Map<String,Object> itemObj : listAddItem) {
                updateQuantityInventoryProduct(itemObj, productStoreId, locale, delegator, dispatcher, userLogin);
                dispatcher.runSync("exportProductEachItem", UtilMisc.toMap("itemObject", itemObj));
            }
        } catch (Exception e) {
            Debug.logError(e.getMessage(), module);
            request.setAttribute("_ERROR_MESSAGE_","Exception: ".concat(e.getMessage()));
            return "error";
        }
        return "success";
    }

    private static void updateQuantityInventoryProduct(Map<String, Object> itemObj, String productStoreId, Locale locale, Delegator delegator, LocalDispatcher dispatcher, GenericValue userLogin) {
        try {
            AddItemCall addItemCall = (AddItemCall) itemObj.get("addItemCall");
            ItemType item = addItemCall.getItem();
            String productId = item.getSKU();
            if (UtilValidate.isNotEmpty(itemObj.get("requireEbayInventory")) && (itemObj.get("requireEbayInventory").equals("Y"))) {
                GetSellingManagerInventoryRequestType req = new GetSellingManagerInventoryRequestType();
                GetSellingManagerInventoryResponseType resp =  null;
                SellingManagerProductType[] returnedSellingManagerProductType = null;
                GetSellingManagerInventoryCall call = new GetSellingManagerInventoryCall(EbayStoreHelper.getApiContext(productStoreId, locale, delegator));
                resp = (GetSellingManagerInventoryResponseType)call.execute(req);
                if (resp != null && "SUCCESS".equals(resp.getAck().toString())) {
                    returnedSellingManagerProductType  = resp.getSellingManagerProduct();
                    for (int i = 0; i < returnedSellingManagerProductType.length; i++) {
                        SellingManagerProductDetailsType prodDetailType = returnedSellingManagerProductType[i].getSellingManagerProductDetails();
                        String productIdInv = Long.toString(prodDetailType.getProductID());
                        if (productId.equals(productIdInv)) {
                            int qty = prodDetailType.getQuantityAvailable();
                            if (qty > 0) {
                                int newQty = qty - 1;
                                prodDetailType.setQuantityAvailable(newQty);
                                ReviseSellingManagerProductCall revProdCall = new ReviseSellingManagerProductCall(EbayStoreHelper.getApiContext(productStoreId, locale, delegator));
                                ReviseSellingManagerProductRequestType revReq = new ReviseSellingManagerProductRequestType();
                                ReviseSellingManagerProductResponseType revResp = null;
                                revReq.setSellingManagerProductDetails(prodDetailType);
                                revResp = (ReviseSellingManagerProductResponseType) revProdCall.execute(revReq);
                                if (revResp != null && "SUCCESS".equals(revResp.getAck().toString())) {
                                    Debug.logInfo("  Already update quantity on eBay inventory with product id ::"+revResp.getSellingManagerProductDetails().getProductID(), module);
                                } else {
                                    EbayStoreHelper.createErrorLogMessage(userLogin, dispatcher, productStoreId, revResp.getAck().toString(), "ReviseSellingManagerProductCall : updateQuantityInventoryProduct", revResp.getErrors(0).getLongMessage());
                                }
                            }
                        }
                    }
                } else {
                    EbayStoreHelper.createErrorLogMessage(userLogin, dispatcher, productStoreId, resp.getAck().toString(), "GetSellingManagerInventoryCall : updateQuantityInventoryProduct", resp.getErrors(0).getLongMessage());
                }
            }
        } catch (Exception e) {
            Debug.logError(e.getMessage(), module);
        }
    }
    
    public static Map<String, Map<String, List<String>>> categorySpecifics(String categoryId, HttpServletRequest request) {
        Map<String, Map<String, List<String>>> recommendationMap = new HashMap<String, Map<String,List<String>>>();
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        Map<String,Object> requestParams = UtilHttp.getParameterMap(request);
        Locale locale = UtilHttp.getLocale(request);
        String productStoreId = (String) requestParams.get("productStoreId");
        
        try {
            ApiContext apiContext = EbayStoreHelper.getApiContext(productStoreId, locale, delegator);
            GetCategorySpecificsCall categorySpecifics = new GetCategorySpecificsCall(apiContext);
            String[] categoryIds = {categoryId};
            categorySpecifics.setCategoryID(categoryIds);
            DetailLevelCodeType[] detailLevel = new DetailLevelCodeType[] {
                    DetailLevelCodeType.RETURN_ALL,
                    DetailLevelCodeType.ITEM_RETURN_ATTRIBUTES,
                    DetailLevelCodeType.ITEM_RETURN_DESCRIPTION
            };
            categorySpecifics.setDetailLevel(detailLevel);
            RecommendationsType[] recommend =  categorySpecifics.getCategorySpecifics();
            
            for (int i = 0; i < recommend.length; i++) {
                NameRecommendationType[] nameRecommend = recommend[i].getNameRecommendation();
                Map<String, List<String>> nameRecommendationMap = new HashMap<String, List<String>>();
                for (int j = 0; j < nameRecommend.length; j++) {
                    String name = nameRecommend[j].getName();
                    List<String> valueList = new LinkedList<String>();
                    ValueRecommendationType[] valueRecommend = nameRecommend[j].getValueRecommendation();
                    for (int k = 0; k < valueRecommend.length; k++) {
                        String value = valueRecommend[k].getValue();
                        valueList.add(value);
                    }
                    nameRecommendationMap.put(name, valueList);
                }
                recommendationMap.put("categorySpecifics", nameRecommendationMap);
            }
        } catch (Exception e) {
            Debug.logError(e.getMessage(), module);
            return null;
        }
        return recommendationMap;
    }
}
