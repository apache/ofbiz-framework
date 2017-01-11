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
package org.apache.ofbiz.ebaystore;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.UtilXml;
import org.apache.ofbiz.ebay.EbayHelper;
import org.apache.ofbiz.ebay.ProductsExportToEbay;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.service.ServiceUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.ebay.sdk.ApiContext;
import com.ebay.sdk.ApiException;
import com.ebay.sdk.SdkException;
import com.ebay.sdk.SdkSoapException;
import com.ebay.sdk.TimeFilter;
import com.ebay.sdk.call.AddDisputeCall;
import com.ebay.sdk.call.AddSecondChanceItemCall;
import com.ebay.sdk.call.GetAllBiddersCall;
import com.ebay.sdk.call.GetItemCall;
import com.ebay.sdk.call.GetMyeBaySellingCall;
import com.ebay.sdk.call.GetOrdersCall;
import com.ebay.sdk.call.GetSellerTransactionsCall;
import com.ebay.sdk.call.GetSellingManagerSoldListingsCall;
import com.ebay.sdk.call.GetStoreCall;
import com.ebay.sdk.call.GetStoreOptionsCall;
import com.ebay.sdk.call.ReviseItemCall;
import com.ebay.sdk.call.SetStoreCall;
import com.ebay.sdk.call.SetStoreCategoriesCall;
import com.ebay.sdk.call.VerifyAddSecondChanceItemCall;
import com.ebay.sdk.util.eBayUtil;
import com.ebay.soap.eBLBaseComponents.AddressType;
import com.ebay.soap.eBLBaseComponents.AmountType;
import com.ebay.soap.eBLBaseComponents.CheckoutStatusType;
import com.ebay.soap.eBLBaseComponents.CurrencyCodeType;
import com.ebay.soap.eBLBaseComponents.DetailLevelCodeType;
import com.ebay.soap.eBLBaseComponents.DisputeExplanationCodeType;
import com.ebay.soap.eBLBaseComponents.DisputeReasonCodeType;
import com.ebay.soap.eBLBaseComponents.ExternalTransactionType;
import com.ebay.soap.eBLBaseComponents.GalleryTypeCodeType;
import com.ebay.soap.eBLBaseComponents.GetAllBiddersModeCodeType;
import com.ebay.soap.eBLBaseComponents.GetStoreOptionsRequestType;
import com.ebay.soap.eBLBaseComponents.GetStoreOptionsResponseType;
import com.ebay.soap.eBLBaseComponents.GetStoreRequestType;
import com.ebay.soap.eBLBaseComponents.GetStoreResponseType;
import com.ebay.soap.eBLBaseComponents.ItemArrayType;
import com.ebay.soap.eBLBaseComponents.ItemListCustomizationType;
import com.ebay.soap.eBLBaseComponents.ItemSortTypeCodeType;
import com.ebay.soap.eBLBaseComponents.ItemType;
import com.ebay.soap.eBLBaseComponents.ListingTypeCodeType;
import com.ebay.soap.eBLBaseComponents.MerchDisplayCodeType;
import com.ebay.soap.eBLBaseComponents.OfferType;
import com.ebay.soap.eBLBaseComponents.OrderStatusCodeType;
import com.ebay.soap.eBLBaseComponents.OrderTransactionType;
import com.ebay.soap.eBLBaseComponents.OrderType;
import com.ebay.soap.eBLBaseComponents.PaginatedItemArrayType;
import com.ebay.soap.eBLBaseComponents.PaginationType;
import com.ebay.soap.eBLBaseComponents.PhotoDisplayCodeType;
import com.ebay.soap.eBLBaseComponents.PictureDetailsType;
import com.ebay.soap.eBLBaseComponents.PictureSourceCodeType;
import com.ebay.soap.eBLBaseComponents.SalesTaxType;
import com.ebay.soap.eBLBaseComponents.SecondChanceOfferDurationCodeType;
import com.ebay.soap.eBLBaseComponents.SellingManagerSearchType;
import com.ebay.soap.eBLBaseComponents.SellingManagerSearchTypeCodeType;
import com.ebay.soap.eBLBaseComponents.SellingManagerSoldListingsPropertyTypeCodeType;
import com.ebay.soap.eBLBaseComponents.SellingManagerSoldOrderType;
import com.ebay.soap.eBLBaseComponents.SellingManagerSoldTransactionType;
import com.ebay.soap.eBLBaseComponents.SellingStatusType;
import com.ebay.soap.eBLBaseComponents.SetStoreCategoriesRequestType;
import com.ebay.soap.eBLBaseComponents.SetStoreCategoriesResponseType;
import com.ebay.soap.eBLBaseComponents.SetStoreRequestType;
import com.ebay.soap.eBLBaseComponents.SetStoreResponseType;
import com.ebay.soap.eBLBaseComponents.ShippingDetailsType;
import com.ebay.soap.eBLBaseComponents.ShippingServiceOptionsType;
import com.ebay.soap.eBLBaseComponents.StoreCategoryUpdateActionCodeType;
import com.ebay.soap.eBLBaseComponents.StoreColorSchemeType;
import com.ebay.soap.eBLBaseComponents.StoreColorType;
import com.ebay.soap.eBLBaseComponents.StoreCustomCategoryArrayType;
import com.ebay.soap.eBLBaseComponents.StoreCustomCategoryType;
import com.ebay.soap.eBLBaseComponents.StoreCustomHeaderLayoutCodeType;
import com.ebay.soap.eBLBaseComponents.StoreCustomListingHeaderDisplayCodeType;
import com.ebay.soap.eBLBaseComponents.StoreCustomListingHeaderType;
import com.ebay.soap.eBLBaseComponents.StoreFontFaceCodeType;
import com.ebay.soap.eBLBaseComponents.StoreFontSizeCodeType;
import com.ebay.soap.eBLBaseComponents.StoreFontType;
import com.ebay.soap.eBLBaseComponents.StoreHeaderStyleCodeType;
import com.ebay.soap.eBLBaseComponents.StoreItemListLayoutCodeType;
import com.ebay.soap.eBLBaseComponents.StoreItemListSortOrderCodeType;
import com.ebay.soap.eBLBaseComponents.StoreLogoArrayType;
import com.ebay.soap.eBLBaseComponents.StoreLogoType;
import com.ebay.soap.eBLBaseComponents.StoreSubscriptionLevelCodeType;
import com.ebay.soap.eBLBaseComponents.StoreThemeArrayType;
import com.ebay.soap.eBLBaseComponents.StoreThemeType;
import com.ebay.soap.eBLBaseComponents.StoreType;
import com.ebay.soap.eBLBaseComponents.TradingRoleCodeType;
import com.ebay.soap.eBLBaseComponents.TransactionType;
import com.ebay.soap.eBLBaseComponents.UserType;
import com.ibm.icu.text.SimpleDateFormat;

public class EbayStore {
    private static final String resource = "EbayStoreUiLabels";
    private static final String module = ProductsExportToEbay.class.getName();
    public static ProductsExportToEbay productExportEbay = new ProductsExportToEbay();

    private static void appendRequesterCredentials(Element elem, Document doc, String token) {
        Element requesterCredentialsElem = UtilXml.addChildElement(elem, "RequesterCredentials", doc);
        UtilXml.addChildElementValue(requesterCredentialsElem, "eBayAuthToken", token, doc);
    }

    private static Map<String, Object> postItem(String postItemsUrl, StringBuffer dataItems, String devID, String appID, String certID,
            String callName, String compatibilityLevel, String siteID) throws IOException {
        if (Debug.verboseOn()) {
            Debug.logVerbose("Request of " + callName + " To eBay:\n" + dataItems.toString(), module);
        }
        HttpURLConnection connection = (HttpURLConnection)(new URL(postItemsUrl)).openConnection();
        connection.setDoInput(true);
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("X-EBAY-API-COMPATIBILITY-LEVEL", compatibilityLevel);
        connection.setRequestProperty("X-EBAY-API-DEV-NAME", devID);
        connection.setRequestProperty("X-EBAY-API-APP-NAME", appID);
        connection.setRequestProperty("X-EBAY-API-CERT-NAME", certID);
        connection.setRequestProperty("X-EBAY-API-CALL-NAME", callName);
        connection.setRequestProperty("X-EBAY-API-SITEID", siteID);
        connection.setRequestProperty("Content-Type", "text/xml");

        OutputStream outputStream = connection.getOutputStream();
        outputStream.write(dataItems.toString().getBytes());
        outputStream.close();
        int responseCode = connection.getResponseCode();
        InputStream inputStream;
        Map<String, Object> result = new HashMap<String, Object>();
        String response = null;

        if (responseCode == HttpURLConnection.HTTP_CREATED ||
                responseCode == HttpURLConnection.HTTP_OK) {
            inputStream = connection.getInputStream();
            response = toString(inputStream);
            result = ServiceUtil.returnSuccess(response);
        } else {
            inputStream = connection.getErrorStream();
            response = toString(inputStream);
            result = ServiceUtil.returnFailure(response);
        }

        if (Debug.verboseOn()) {
            Debug.logVerbose("Response of " + callName + " From eBay:\n" + response, module);
        }

        return result;
    }

    private static String toString(InputStream inputStream) throws IOException {
        String string;
        StringBuilder outputBuilder = new StringBuilder();
        if (inputStream != null) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            while (null != (string = reader.readLine())) {
                outputBuilder.append(string).append('\n');
            }
        }
        return outputBuilder.toString();
    }

    /* add/update/delete  categories and child into your ebay store category */
    public static Map<String,Object> exportCategoriesSelectedToEbayStore(DispatchContext dctx, Map<String,? extends Object>  context) {
        Locale locale = (Locale) context.get("locale");
        Delegator delegator = dctx.getDelegator();
        Map<String, Object> result = new HashMap<String, Object>();
        SetStoreCategoriesRequestType req = null;
        StoreCustomCategoryArrayType categoryArrayType = null;

        List<GenericValue> catalogCategories = null;

        if (UtilValidate.isEmpty(context.get("prodCatalogId")) || UtilValidate.isEmpty(context.get("productStoreId")) || UtilValidate.isEmpty(context.get("partyId"))) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "EbayStoreSetCatalogIdAndProductStoreId", locale));
        }
        if (!EbayStoreHelper.validatePartyAndRoleType(delegator,context.get("partyId").toString())) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "EbayStorePartyWithoutRoleEbayAccount", UtilMisc.toMap("partyId", context.get("partyId").toString()), locale));
        }
        try {
            SetStoreCategoriesCall  call = new SetStoreCategoriesCall(EbayStoreHelper.getApiContext((String)context.get("productStoreId"), locale, delegator));

            catalogCategories = EntityQuery.use(delegator).from("ProdCatalogCategory").where("prodCatalogId", context.get("prodCatalogId").toString(),"prodCatalogCategoryTypeId","PCCT_EBAY_ROOT").orderBy("sequenceNum ASC").queryList();
            if (catalogCategories != null && catalogCategories.size() > 0) {
                List<StoreCustomCategoryType> listAdd = new LinkedList<StoreCustomCategoryType>();
                List<StoreCustomCategoryType> listEdit = new LinkedList<StoreCustomCategoryType>();
                //start at level 0 of categories
                for (GenericValue catalogCategory : catalogCategories) {
                    GenericValue productCategory = catalogCategory.getRelatedOne("ProductCategory", false);
                    if (productCategory != null) {
                        String ebayCategoryId = EbayStoreHelper.retriveEbayCategoryIdByPartyId(delegator,productCategory.getString("productCategoryId"),context.get("partyId").toString());
                        StoreCustomCategoryType categoryType = new StoreCustomCategoryType();
                        if (ebayCategoryId == null) {
                            categoryType.setName(productCategory.getString("categoryName"));
                            listAdd.add(categoryType);
                        } else {
                            categoryType.setCategoryID(new Long(ebayCategoryId));
                            categoryType.setName(productCategory.getString("categoryName"));
                            listEdit.add(categoryType);
                        }
                    }
                }
                if (listAdd.size() > 0) {
                    req = new SetStoreCategoriesRequestType();
                    categoryArrayType = new StoreCustomCategoryArrayType();
                    categoryArrayType.setCustomCategory(toStoreCustomCategoryTypeArray(listAdd));
                    req.setStoreCategories(categoryArrayType);
                    result = excuteExportCategoryToEbayStore(call, req, StoreCategoryUpdateActionCodeType.ADD, delegator,context.get("partyId").toString(), catalogCategories, locale);
                }
                if (listEdit.size() > 0) {
                    req = new SetStoreCategoriesRequestType();
                    categoryArrayType = new StoreCustomCategoryArrayType();
                    categoryArrayType.setCustomCategory(toStoreCustomCategoryTypeArray(listEdit));
                    req.setStoreCategories(categoryArrayType);
                    result = excuteExportCategoryToEbayStore(call, req, StoreCategoryUpdateActionCodeType.RENAME, delegator,context.get("partyId").toString(), catalogCategories, locale);
                }

                //start at level 1 of categories
                listAdd = new LinkedList<StoreCustomCategoryType>();
                listEdit = new LinkedList<StoreCustomCategoryType>();
                for (GenericValue catalogCategory : catalogCategories) {
                    GenericValue productCategory = catalogCategory.getRelatedOne("ProductCategory", false);
                    if (productCategory != null) {
                        String ebayParentCategoryId = EbayStoreHelper.retriveEbayCategoryIdByPartyId(delegator, productCategory.getString("productCategoryId"), context.get("partyId").toString());
                        if (ebayParentCategoryId != null) {
                            List<GenericValue> productCategoryRollupList = EntityQuery.use(delegator).from("ProductCategoryRollup").where("parentProductCategoryId", productCategory.getString("productCategoryId")).orderBy("sequenceNum ASC").queryList();
                            for (GenericValue productCategoryRollup : productCategoryRollupList) {
                                productCategory = EntityQuery.use(delegator).from("ProductCategory").where("productCategoryId", productCategoryRollup.getString("productCategoryId")).queryOne();
                                StoreCustomCategoryType childCategoryType = new StoreCustomCategoryType();
                                String ebayChildCategoryId = EbayStoreHelper.retriveEbayCategoryIdByPartyId(delegator, productCategory.getString("productCategoryId"), context.get("partyId").toString());
                                if (ebayChildCategoryId == null) {
                                    childCategoryType.setName(productCategory.getString("categoryName"));
                                    listAdd.add(childCategoryType);
                                } else {
                                    childCategoryType.setCategoryID(new Long(ebayChildCategoryId));
                                    childCategoryType.setName(productCategory.getString("categoryName"));
                                    listEdit.add(childCategoryType);
                                }
                            }
                        }
                        if (listAdd.size() > 0) {
                            req = new SetStoreCategoriesRequestType();
                            categoryArrayType = new StoreCustomCategoryArrayType();
                            categoryArrayType.setCustomCategory(toStoreCustomCategoryTypeArray(listAdd));
                            req.setStoreCategories(categoryArrayType);
                            req.setDestinationParentCategoryID(new Long(ebayParentCategoryId));
                            result = excuteExportCategoryToEbayStore(call, req, StoreCategoryUpdateActionCodeType.ADD, delegator,context.get("partyId").toString(), catalogCategories, locale);
                        }
                        if (listEdit.size() > 0) {
                            req = new SetStoreCategoriesRequestType();
                            categoryArrayType = new StoreCustomCategoryArrayType();
                            categoryArrayType.setCustomCategory(toStoreCustomCategoryTypeArray(listEdit));
                            req.setStoreCategories(categoryArrayType);
                            req.setDestinationParentCategoryID(new Long(ebayParentCategoryId));
                            result = excuteExportCategoryToEbayStore(call, req, StoreCategoryUpdateActionCodeType.RENAME, delegator,context.get("partyId").toString(), catalogCategories, locale);
                        }
                    }
                }
                //start at level 2 of categories
                listAdd = new LinkedList<StoreCustomCategoryType>();
                listEdit = new LinkedList<StoreCustomCategoryType>();
                for (GenericValue catalogCategory : catalogCategories) {
                    GenericValue productCategory = catalogCategory.getRelatedOne("ProductCategory", false);
                    if (productCategory != null) {
                        List<GenericValue> productParentCategoryRollupList = EntityQuery.use(delegator).from("ProductCategoryRollup").where("parentProductCategoryId",productCategory.getString("productCategoryId")).orderBy("sequenceNum ASC").queryList();
                        for (GenericValue productParentCategoryRollup : productParentCategoryRollupList) {
                            String ebayParentCategoryId = EbayStoreHelper.retriveEbayCategoryIdByPartyId(delegator,productParentCategoryRollup.getString("productCategoryId"),context.get("partyId").toString());
                            if (ebayParentCategoryId != null) {
                                List<GenericValue> productChildCategoryRollupList = EntityQuery.use(delegator).from("ProductCategoryRollup").where("parentProductCategoryId",productParentCategoryRollup.getString("productCategoryId")).orderBy("sequenceNum ASC").queryList();
                                for (GenericValue productChildCategoryRollup : productChildCategoryRollupList) {
                                    productCategory = EntityQuery.use(delegator).from("ProductCategory").where("productCategoryId", productChildCategoryRollup.getString("productCategoryId")).queryOne();
                                    StoreCustomCategoryType childCategoryType = new StoreCustomCategoryType();
                                    String ebayChildCategoryId = EbayStoreHelper.retriveEbayCategoryIdByPartyId(delegator,productCategory.getString("productCategoryId"),context.get("partyId").toString());
                                    if (ebayChildCategoryId == null) {
                                        childCategoryType.setName(productCategory.getString("categoryName"));
                                        listAdd.add(childCategoryType);
                                    } else {
                                        childCategoryType.setCategoryID(new Long(ebayChildCategoryId));
                                        childCategoryType.setName(productCategory.getString("categoryName"));
                                        listEdit.add(childCategoryType);
                                    }
                                }
                                if (listAdd.size() > 0) {
                                    req = new SetStoreCategoriesRequestType();
                                    categoryArrayType = new StoreCustomCategoryArrayType();
                                    categoryArrayType.setCustomCategory(toStoreCustomCategoryTypeArray(listAdd));
                                    req.setStoreCategories(categoryArrayType);
                                    req.setDestinationParentCategoryID(new Long(ebayParentCategoryId));
                                    result = excuteExportCategoryToEbayStore(call, req, StoreCategoryUpdateActionCodeType.ADD, delegator, context.get("partyId").toString(), catalogCategories, locale);
                                }
                                if (listEdit.size() > 0) {
                                    req = new SetStoreCategoriesRequestType();
                                    categoryArrayType = new StoreCustomCategoryArrayType();
                                    categoryArrayType.setCustomCategory(toStoreCustomCategoryTypeArray(listEdit));
                                    req.setStoreCategories(categoryArrayType);
                                    req.setDestinationParentCategoryID(new Long(ebayParentCategoryId));
                                    result = excuteExportCategoryToEbayStore(call, req, StoreCategoryUpdateActionCodeType.RENAME, delegator, context.get("partyId").toString(), catalogCategories, locale);
                                }
                            }
                        }
                    }
                }
            } else {
                return ServiceUtil.returnError(UtilProperties.getMessage(resource, "EbayStoreRootCategoryNotFound", UtilMisc.toMap("prodCatalogId", context.get("prodCatalogId")), locale));
            }
        } catch (GenericEntityException e) {
            result = ServiceUtil.returnFailure(e.getMessage());
        }
        if (result.get("responseMessage") != null && result.get("responseMessage").equals("fail")) {
            result = ServiceUtil.returnError(result.get("errorMessage").toString());
        }
        return result;
    }

    public static StoreCustomCategoryType[] toStoreCustomCategoryTypeArray(List<StoreCustomCategoryType> list) {
        StoreCustomCategoryType[] storeCustomCategoryTypeArry = null;
        try {
            if (list != null && list.size() > 0) {
                storeCustomCategoryTypeArry = new StoreCustomCategoryType[list.size()];
                int i=0;
                for (StoreCustomCategoryType val : list) {
                    storeCustomCategoryTypeArry[i] = val;
                }
            }
        } catch (Exception e) {
            Debug.logError(e.getMessage(), module);
        }
        return storeCustomCategoryTypeArry;
    }

    public static Map<String, Object> excuteExportCategoryToEbayStore(SetStoreCategoriesCall  call, SetStoreCategoriesRequestType req, StoreCategoryUpdateActionCodeType actionCode,Delegator delegator, String partyId,List<GenericValue> catalogCategories, Locale locale) {
        Map<String, Object> result = new HashMap<String, Object>();
        SetStoreCategoriesResponseType resp = null;
        try {
            if (req != null && actionCode != null) {
                req.setAction(actionCode);
                resp = (SetStoreCategoriesResponseType) call.execute(req);
                if (resp != null && "SUCCESS".equals(resp.getAck().toString())) {
                    StoreCustomCategoryArrayType returnedCustomCategory = resp.getCustomCategory();
                    if (actionCode.equals(StoreCategoryUpdateActionCodeType.ADD) && returnedCustomCategory != null) {
                        StoreCustomCategoryType[] returnCategoryTypeList = returnedCustomCategory.getCustomCategory();
                        for (StoreCustomCategoryType returnCategoryType : returnCategoryTypeList) {
                            List<GenericValue> productCategoryList = EntityQuery.use(delegator).from("ProductCategory").where("categoryName",returnCategoryType.getName(),"productCategoryTypeId","EBAY_CATEGORY").queryList();
                            for (GenericValue productCategory : productCategoryList) {
                                if (EbayStoreHelper.veriflyCategoryInCatalog(delegator,catalogCategories,productCategory.getString("productCategoryId"))) {
                                    if (EbayStoreHelper.createEbayCategoryIdByPartyId(delegator, productCategory.getString("productCategoryId"), partyId, String.valueOf(returnCategoryType.getCategoryID()))) {
                                        Debug.logInfo("Create new ProductCategoryRollup with partyId "+partyId+" categoryId "+productCategory.getString("productCategoryId")+ " and ebayCategoryId "+String.valueOf(returnCategoryType.getCategoryID()), module);
                                    }
                                    break;
                                }
                            }
                        }
                    }
                    result = ServiceUtil.returnSuccess(UtilProperties.getMessage(resource, "EbayExportToEbayStoreSuccess", locale));
                } else {
                    return ServiceUtil.returnError(UtilProperties.getMessage(resource, "EbayExportToEbayStoreFailed", UtilMisc.toMap("errorString", resp.getMessage()), locale));
                }
            }
        } catch (ApiException e) {
            result = ServiceUtil.returnFailure(e.getMessage());
        } catch (SdkSoapException e) {
            result = ServiceUtil.returnFailure(e.getMessage());
        } catch (SdkException e) {
            result = ServiceUtil.returnFailure(e.getMessage());
        } catch (GenericEntityException e) {
            result = ServiceUtil.returnFailure(e.getMessage());
        }
        return result;
    }

    public static Map<String, Object> buildSetStoreXml(DispatchContext dctx, Map<String, ? extends Object> context, StringBuffer dataStoreXml, String token, String siteID) {
        Locale locale = (Locale)context.get("locale");
        try {
            Delegator delegator = dctx.getDelegator();
            
            // Get the list of products to be exported to eBay
            try {
                Document storeDocument = UtilXml.makeEmptyXmlDocument("SetStoreRequest");
                Element storeRequestElem = storeDocument.getDocumentElement();
                storeRequestElem.setAttribute("xmlns", "urn:ebay:apis:eBLBaseComponents");

                appendRequesterCredentials(storeRequestElem, storeDocument, token);

                /*UtilXml.addChildElementValue(storeRequestElem, "SiteId", siteID, storeDocument);
                UtilXml.addChildElementValue(storeRequestElem, "DetailLevel", "ReturnAll", storeDocument);
                UtilXml.addChildElementValue(storeRequestElem, "LevelLimit", "1", storeDocument);*/
                // Prepare data for set to XML
                GenericValue productStore = null;
                if (UtilValidate.isNotEmpty(context.get("productStoreId").toString())) {
                    productStore = EntityQuery.use(delegator).from("ProductStore").where("productStoreId", context.get("productStoreId").toString()).queryOne();
                }
                Element itemElem = UtilXml.addChildElement(storeRequestElem, "Store", storeDocument);
                UtilXml.addChildElementValue(itemElem, "Name", productStore.getString("storeName"), storeDocument);
                UtilXml.addChildElementValue(itemElem, "SubscriptionLevel", "Basic", storeDocument);
                UtilXml.addChildElementValue(itemElem, "Description", productStore.getString("title"), storeDocument);
                dataStoreXml.append(UtilXml.writeXmlDocument(storeDocument));

            } catch (Exception e) {
                Debug.logError("Exception during building data items to eBay: " + e.getMessage(), module);
                return ServiceUtil.returnFailure(UtilProperties.getMessage(resource, "productsExportToEbay.exceptionDuringBuildingDataItemsToEbay", locale));
            }
        } catch (Exception e) {
            Debug.logError("Exception during building data items to eBay: " + e.getMessage(), module);
            return ServiceUtil.returnFailure(UtilProperties.getMessage(resource, "productsExportToEbay.exceptionDuringBuildingDataItemsToEbay", locale));
        }
        return ServiceUtil.returnSuccess();
    }

    public static String readEbayResponse(String msg, String productStoreId) {
        String result = "success";
        try {
            Document docResponse = UtilXml.readXmlDocument(msg, true);
            Element elemResponse = docResponse.getDocumentElement();
            String ack = UtilXml.childElementValue(elemResponse, "Ack", "Failure");
            if (ack != null && "Failure".equals(ack)) {
                String errorMessage = "";
                List<Element> errorList = UtilGenerics.checkList(UtilXml.childElementList(elemResponse, "Errors"));
                Iterator<Element> errorElemIter = errorList.iterator();
                while (errorElemIter.hasNext()) {
                    Element errorElement = errorElemIter.next();
                    errorMessage = UtilXml.childElementValue(errorElement, "LongMessage");
                }
                result = errorMessage;
            } else {
                result = "Successfully exported with ID (" + productStoreId + ").";
            }
        } catch (Exception e) {
            Debug.logError("Error in processing xml string" + e.getMessage(), module);
            result =  "Failure";
        }
        return result;
    }

    public static Map<String, Object> buildGetStoreXml(Map<String, ? extends Object> context, StringBuffer dataStoreXml, String token, String siteID) {
        Locale locale = (Locale)context.get("locale");
        // Get the list of products to be exported to eBay
        try {
            Document storeDocument = UtilXml.makeEmptyXmlDocument("GetStoreRequest");
            Element storeRequestElem = storeDocument.getDocumentElement();
            storeRequestElem.setAttribute("xmlns", "urn:ebay:apis:eBLBaseComponents");
            appendRequesterCredentials(storeRequestElem, storeDocument, token);
            //UtilXml.addChildElementValue(storeRequestElem, "CategorySiteID", siteID, storeDocument);
            UtilXml.addChildElementValue(storeRequestElem, "DetailLevel", "ReturnAll", storeDocument);
            UtilXml.addChildElementValue(storeRequestElem, "LevelLimit", "1", storeDocument);
            dataStoreXml.append(UtilXml.writeXmlDocument(storeDocument));
        } catch (Exception e) {
            Debug.logError("Exception during building data to eBay: " + e.getMessage(), module);
            return ServiceUtil.returnFailure(UtilProperties.getMessage(resource, "productsExportToEbay.exceptionDuringBuildingDataItemsToEbay", locale));
        }
        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> buildSetStoreCategoriesXml(DispatchContext dctx, Map<String, ? extends Object> context, StringBuffer dataStoreXml, String token, String siteID, String productCategoryId) {
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale)context.get("locale");
        // Get the list of products to be exported to eBay
        try {
            Document storeDocument = UtilXml.makeEmptyXmlDocument("SetStoreCategoriesRequest");
            Element storeRequestElem = storeDocument.getDocumentElement();
            storeRequestElem.setAttribute("xmlns", "urn:ebay:apis:eBLBaseComponents");
            appendRequesterCredentials(storeRequestElem, storeDocument, token);
            UtilXml.addChildElementValue(storeRequestElem, "DetailLevel", "ReturnAll", storeDocument);
            UtilXml.addChildElementValue(storeRequestElem, "Version", "643", storeDocument);
            UtilXml.addChildElementValue(storeRequestElem, "Action", "Add", storeDocument);

            Element StoreCategoriesElem = UtilXml.addChildElement(storeRequestElem, "StoreCategories", storeDocument);
            //UtilXml.addChildElementValue(StoreCategoriesElem, "Country", (String)context.get("country"), storeDocument);
            GenericValue category = null;
            if (UtilValidate.isNotEmpty(context.get("prodCatalogId"))) {
                category = EntityQuery.use(delegator).from("ProductCategory").where("productCategoryId", productCategoryId).cache().queryOne();
            }
            String categoryName = category.getString("productCategoryId").toString();
            if (category.getString("categoryName").toString() != null) {
                categoryName = category.getString("categoryName").toString();
            }
            Element customCategoryElem = UtilXml.addChildElement(StoreCategoriesElem, "CustomCategory", storeDocument);
            //UtilXml.addChildElementValue(customCategoryElem, "CategoryID", "", storeDocument);
            UtilXml.addChildElementValue(customCategoryElem, "Name", categoryName, storeDocument);

            dataStoreXml.append(UtilXml.writeXmlDocument(storeDocument));

        } catch (Exception e) {
            Debug.logError("Exception during building data to eBay: " + e.getMessage(), module);
            return ServiceUtil.returnFailure(UtilProperties.getMessage(resource, "productsExportToEbay.exceptionDuringBuildingDataItemsToEbay", locale));
        }
        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> readEbayGetStoreCategoriesResponse(String msg, Locale locale) {
        Map<String, Object> results = null;
        List<Map<Object, Object>> categories = new LinkedList<Map<Object, Object>>();
        try {
            Document docResponse = UtilXml.readXmlDocument(msg, true);
            Element elemResponse = docResponse.getDocumentElement();
            String ack = UtilXml.childElementValue(elemResponse, "Ack", "Failure");
            if (ack != null && "Failure".equals(ack)) {
                String errorMessage = "";
                List<Element> errorList = UtilGenerics.checkList(UtilXml.childElementList(elemResponse, "Errors"));
                Iterator<Element> errorElemIter = errorList.iterator();
                while (errorElemIter.hasNext()) {
                    Element errorElement = errorElemIter.next();
                    errorMessage = UtilXml.childElementValue(errorElement, "ShortMessage", "");
                }
                return ServiceUtil.returnFailure(errorMessage);
            } else {
                // retrieve Store
                List<Element> Store = UtilGenerics.checkList(UtilXml.childElementList(elemResponse, "Store"));
                Iterator<Element> StoreElemIter = Store.iterator();
                while (StoreElemIter.hasNext()) {
                    Element StoreElemIterElemIterElement = StoreElemIter.next();
                    // retrieve Custom Category Array

                    List<Element> customCategories = UtilGenerics.checkList(UtilXml.childElementList(StoreElemIterElemIterElement, "CustomCategories"));
                    Iterator<Element> customCategoriesElemIter = customCategories.iterator();
                    while (customCategoriesElemIter.hasNext()) {
                        Element customCategoriesElemIterElement = customCategoriesElemIter.next();

                        // retrieve CustomCategory
                        List<Element> customCategory = UtilGenerics.checkList(UtilXml.childElementList(customCategoriesElemIterElement, "CustomCategory"));
                        Iterator<Element> customCategoryElemIter = customCategory.iterator();
                        while (customCategoryElemIter.hasNext()) {
                            Map<Object, Object> categ = new HashMap<Object, Object>();
                            Element categoryElement = customCategoryElemIter.next();
                            categ.put("CategoryID", UtilXml.childElementValue(categoryElement, "CategoryID"));
                            categ.put("CategoryName", UtilXml.childElementValue(categoryElement, "Name"));
                            categ.put("CategorySeq", UtilXml.childElementValue(categoryElement, "Order"));
                            categories.add(categ);
                        }
                    }
                }
                categories = UtilMisc.sortMaps(categories, UtilMisc.toList("CategoryName"));
                results = UtilMisc.<String, Object>toMap("categories", categories);
            }
        } catch (Exception e) {
            return ServiceUtil.returnFailure();
        }
        return results;
    }

    public static Map<String, Object> getEbayStoreUser(DispatchContext dctx, Map<String, ? extends Object> context) {
        Map<String, Object> result = new HashMap<String, Object>();
        Delegator delegator = dctx.getDelegator();
        String productStoreId = (String) context.get("productStoreId");
        try {
            List<GenericValue> productStores = EntityQuery.use(delegator).from("ProductStoreRole").where("productStoreId", productStoreId, "roleTypeId", "EBAY_ACCOUNT").queryList();
            if (productStores.size() != 0) {
                String partyId = (productStores.get(0)).getString("partyId");
                List<GenericValue> userLoginStore = EntityQuery.use(delegator).from("UserLogin").where("partyId", partyId).queryList();
                if (userLoginStore.size() != 0) {
                String    userLoginId = (userLoginStore.get(0)).getString("userLoginId");
                result.put("userLoginId", userLoginId);
                }
            }
        } catch (Exception e) {

        }
        return result;
    }

    /*Editing the Store Settings */
    /* Get store output */
    public static Map<String,Object> getEbayStoreOutput(DispatchContext dctx, Map<String,Object> context) {
        Locale locale = (Locale) context.get("locale");
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Delegator delegator = dctx.getDelegator();
        Map<String,Object> result = new HashMap<String, Object>();
        StoreType returnedStoreType = null;
        GetStoreRequestType req = new GetStoreRequestType();
        GetStoreResponseType resp =  null;

        String userLoginId = null;
        if (context.get("productStoreId") != null) {
            String partyId = null;
            try {
                List<GenericValue> productStoreRoles = EntityQuery.use(delegator).from("ProductStoreRole").where("productStoreId", context.get("productStoreId").toString(),"roleTypeId","EBAY_ACCOUNT").queryList();
                if (productStoreRoles.size() != 0) {
                    partyId=  (String)productStoreRoles.get(0).get("partyId");
                    List<GenericValue> userLogins = EntityQuery.use(delegator).from("UserLogin").where("partyId", partyId).queryList();
                    if (userLogins.size() != 0) {
                        userLoginId = (String)userLogins.get(0).get("userLoginId");
                    }

                }
            } catch (GenericEntityException e1) {
                e1.printStackTrace();
            }
            Debug.logInfo("userLoginId is "+userLoginId+" and productStoreId is "+ context.get("productStoreId"), module);
            GetStoreCall call = new GetStoreCall(EbayStoreHelper.getApiContext((String)context.get("productStoreId"), locale, delegator));
            //call.setSite(EbayHelper.getSiteCodeType((String)context.get("productStoreId"), locale, delegator));
            call.setCategoryStructureOnly(false);
            call.setUserID(userLoginId);

            try {
                resp = (GetStoreResponseType)call.execute(req);
                if (resp != null && "SUCCESS".equals(resp.getAck().toString())) {
                    returnedStoreType  = resp.getStore();
                    result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
                    //result.put(ModelService.SUCCESS_MESSAGE, UtilProperties.getMessage(resource, "EbayStoreLoadSuccess", locale));
                    // update product store in ofbiz
                    updateProductStore(dctx, context, returnedStoreType,(String) context.get("productStoreId"));
                    Map<String,Object> ebayResp = new HashMap<String, Object>();
                    ebayResp.put("storeName", returnedStoreType.getName());
                    ebayResp.put("storeUrl", returnedStoreType.getURL());
                    ebayResp.put("storeUrlPath", returnedStoreType.getURLPath());
                    String desc = returnedStoreType.getDescription();
                    if (desc != null) desc  =  desc.trim();
                    ebayResp.put("storeDesc", desc);

                    StoreLogoType logoType = returnedStoreType.getLogo();
                    ebayResp.put("storeLogoId", logoType.getLogoID());
                    ebayResp.put("storeLogoName", logoType.getName());
                    ebayResp.put("storeLogoURL", logoType.getURL());

                    StoreThemeType themeType = returnedStoreType.getTheme();
                    ebayResp.put("storeThemeId", themeType.getThemeID());
                    ebayResp.put("storeThemeName", themeType.getName());

                    StoreColorSchemeType colorSchemeType = themeType.getColorScheme();
                    ebayResp.put("storeColorSchemeId", colorSchemeType.getColorSchemeID());

                    StoreColorType colorType = colorSchemeType.getColor();
                    ebayResp.put("storeColorPrimary", colorType.getPrimary());
                    ebayResp.put("storeColorAccent", colorType.getAccent());
                    ebayResp.put("storeColorSecondary", colorType.getSecondary());

                    StoreFontType fontType = colorSchemeType.getFont();
                    ebayResp.put("storeDescColor", fontType.getDescColor());
                    ebayResp.put("storeNameColor", fontType.getNameColor());
                    ebayResp.put("storeTitleColor", fontType.getTitleColor());

                    if (fontType != null) {// basic & advance theme
                        String themeId = themeType.getThemeID().toString().concat("-").concat(colorSchemeType.getColorSchemeID().toString());
                        context.put("themeId", themeId);
                        Map<String,Object> results = retrieveThemeColorSchemeByThemeId(dctx, context);
                        if (results != null) {
                            Map<String,Object> storeFontScheme = UtilGenerics.checkMap(results.get("storeFontScheme"));
                            if (storeFontScheme != null) {
                                ebayResp.put("storeDescFontFace", storeFontScheme.get("storeFontTypeFontDescValue"));
                                ebayResp.put("storeDescSizeCode", storeFontScheme.get("storeDescSizeValue"));

                                ebayResp.put("storeNameFontFace", storeFontScheme.get("storeFontTypeFontFaceValue"));
                                ebayResp.put("storeNameFontFaceSize", storeFontScheme.get("storeFontTypeSizeFaceValue"));

                                ebayResp.put("storeTitleFontFace", storeFontScheme.get("storeFontTypeFontTitleValue"));
                                ebayResp.put("storeTitleFontFaceSize", storeFontScheme.get("storeFontSizeTitleValue"));
                            }
                        }
                    }

                    StoreHeaderStyleCodeType storeHeaderStyleCodeType = returnedStoreType.getHeaderStyle();
                    ebayResp.put("storeHeaderStyle", storeHeaderStyleCodeType.value());
                    StoreHeaderStyleCodeType[] storeHeaderStyleCodeList =  StoreHeaderStyleCodeType.values();
                    if (storeHeaderStyleCodeList != null) {
                        List<Map<String,Object>> storeHeaderStyleList = new LinkedList<Map<String, Object>>();
                        int i=0;
                        while (i<storeHeaderStyleCodeList.length) {
                            Map<String,Object> storeHeaderStyleMap = new HashMap<String, Object>();
                            StoreHeaderStyleCodeType storeHeaderStyleCode = storeHeaderStyleCodeList[i];
                            storeHeaderStyleMap.put("storeHeaderStyleName", storeHeaderStyleCode.name());
                            storeHeaderStyleMap.put("storeHeaderStyleValue", storeHeaderStyleCode.value());
                            storeHeaderStyleList.add(storeHeaderStyleMap);
                            i++;
                        }
                        ebayResp.put("storeHeaderStyleList", storeHeaderStyleList);
                    }

                    ebayResp.put("storeHomePage", returnedStoreType.getHomePage().toString());

                    StoreItemListLayoutCodeType storeItemListLayoutCodeType = returnedStoreType.getItemListLayout();
                    ebayResp.put("storeItemLayoutSelected", storeItemListLayoutCodeType.value());
                    StoreItemListLayoutCodeType[] storeItemListLayoutCodeTypeList = StoreItemListLayoutCodeType.values();
                    if (storeItemListLayoutCodeTypeList != null) {
                        List<Map<String,Object>> storeItemListLayoutCodeList  = new LinkedList<Map<String, Object>>();
                        int i = 0;
                        while (i < storeItemListLayoutCodeTypeList.length) {
                            Map<String,Object> storeItemListLayoutCodeMap = new HashMap<String, Object>();
                            StoreItemListLayoutCodeType storeItemListLayoutCode = storeItemListLayoutCodeTypeList[i];
                            storeItemListLayoutCodeMap.put("storeItemLayoutName", storeItemListLayoutCode.name());
                            storeItemListLayoutCodeMap.put("storeItemLayoutValue", storeItemListLayoutCode.value());
                            storeItemListLayoutCodeList.add(storeItemListLayoutCodeMap);
                            i++;
                        }
                        ebayResp.put("storeItemLayoutList", storeItemListLayoutCodeList);
                    }
                    StoreItemListSortOrderCodeType storeItemListSortOrderCodeType = returnedStoreType.getItemListSortOrder();
                    ebayResp.put("storeItemSortOrderSelected", storeItemListSortOrderCodeType.value());
                    StoreItemListSortOrderCodeType[] storeItemListSortOrderCodeTypeList = StoreItemListSortOrderCodeType.values();
                    if (storeItemListSortOrderCodeTypeList != null) {
                        List<Map<String,Object>> storeItemSortOrderCodeList  = new LinkedList<Map<String, Object>>();
                        int i = 0;
                        while (i < storeItemListSortOrderCodeTypeList.length) {
                            Map<String,Object> storeItemSortOrderCodeMap = new HashMap<String, Object>();
                            StoreItemListSortOrderCodeType storeItemListLayoutCode = storeItemListSortOrderCodeTypeList[i];
                            storeItemSortOrderCodeMap.put("storeItemSortLayoutName", storeItemListLayoutCode.name());
                            storeItemSortOrderCodeMap.put("storeItemSortLayoutValue", storeItemListLayoutCode.value());
                            storeItemSortOrderCodeList.add(storeItemSortOrderCodeMap);
                            i++;
                        }
                        ebayResp.put("storeItemSortOrderList", storeItemSortOrderCodeList);
                    }

                    ebayResp.put("storeCustomHeader", returnedStoreType.getCustomHeader());
                    StoreCustomHeaderLayoutCodeType storeCustomHeaderLayoutCodeType = returnedStoreType.getCustomHeaderLayout();
                    ebayResp.put("storeCustomHeaderLayout", storeCustomHeaderLayoutCodeType.value());
                    StoreCustomHeaderLayoutCodeType[] storeCustomHeaderLayoutCodeTypeList = StoreCustomHeaderLayoutCodeType.values();
                    if (storeCustomHeaderLayoutCodeTypeList != null) {
                        List<Map<String,Object>> storeCustomHeaderLayoutList  = new LinkedList<Map<String, Object>>();
                        int i = 0;
                        while (i < storeCustomHeaderLayoutCodeTypeList.length) {
                            Map<String,Object> storeCustomHeaderLayoutMap = new HashMap<String, Object>();
                            StoreCustomHeaderLayoutCodeType StoreCustomHeaderLayoutCode = storeCustomHeaderLayoutCodeTypeList[i];
                            storeCustomHeaderLayoutMap.put("storeCustomHeaderLayoutName", StoreCustomHeaderLayoutCode.name());
                            storeCustomHeaderLayoutMap.put("storeCustomHeaderLayoutValue", StoreCustomHeaderLayoutCode.value());
                            storeCustomHeaderLayoutList.add(storeCustomHeaderLayoutMap);
                            i++;
                        }
                        ebayResp.put("storeCustomHeaderLayoutList", storeCustomHeaderLayoutList);
                    }

                    StoreCustomListingHeaderType storeCustomListingHeaderType = returnedStoreType.getCustomListingHeader();
                    if (storeCustomListingHeaderType != null) {
                        StoreCustomListingHeaderDisplayCodeType storeCustomListingHeaderDisplayCodeType = storeCustomListingHeaderType.getDisplayType();
                        ebayResp.put("isLogo", storeCustomListingHeaderType.isLogo());
                        ebayResp.put("isSearchBox", storeCustomListingHeaderType.isSearchBox());
                        ebayResp.put("isAddToFavoriteStores", storeCustomListingHeaderType.isAddToFavoriteStores());
                        ebayResp.put("isSignUpForStoreNewsletter", storeCustomListingHeaderType.isSignUpForStoreNewsletter());

                        ebayResp.put("storeCustomListingHeaderDisplayName", storeCustomListingHeaderDisplayCodeType.name());
                        ebayResp.put("storeCustomListingHeaderDisplayValue", storeCustomListingHeaderDisplayCodeType.value());
                        StoreCustomListingHeaderDisplayCodeType[] storeCustomListingHeaderDisplayCodeTypeList = StoreCustomListingHeaderDisplayCodeType.values();
                        if (storeCustomListingHeaderDisplayCodeTypeList != null) {
                            List<Map<String,Object>> storeCustomListingHeaderDisplayCodeList  = new LinkedList<Map<String, Object>>();
                            int i = 0;
                            while (i < storeCustomListingHeaderDisplayCodeTypeList.length) {
                                Map<String,Object> storeCustomListingHeaderDisplayCodeMap = new HashMap<String, Object>();
                                StoreCustomListingHeaderDisplayCodeType storeCustomListingHeaderDisplayCode = storeCustomListingHeaderDisplayCodeTypeList[i];
                                storeCustomListingHeaderDisplayCodeMap.put("storeCustomHeaderLayoutName", storeCustomListingHeaderDisplayCode.name());
                                storeCustomListingHeaderDisplayCodeMap.put("storeCustomHeaderLayoutValue", storeCustomListingHeaderDisplayCode.value());
                                storeCustomListingHeaderDisplayCodeList.add(storeCustomListingHeaderDisplayCodeMap);
                                i++;
                            }
                            ebayResp.put("storeCustomListingHeaderDisplayList", storeCustomListingHeaderDisplayCodeList);
                        }
                    }

                    //CustomListingHeader
                    MerchDisplayCodeType merchDisplayCodeType = returnedStoreType.getMerchDisplay();
                    ebayResp.put("storeMerchDisplay",merchDisplayCodeType.value());
                    MerchDisplayCodeType[] merchDisplayCodeTypeList = MerchDisplayCodeType.values();
                    if (merchDisplayCodeTypeList != null) {
                        List<Map<String,Object>> merchDisplayCodeList = new LinkedList<Map<String, Object>>();
                        int i = 0;
                        while (i < merchDisplayCodeTypeList.length) {
                            Map<String,Object> merchDisplayCodeMap = new HashMap<String, Object>();
                            MerchDisplayCodeType merchDisplayCode = merchDisplayCodeTypeList[i];
                            merchDisplayCodeMap.put("merchDisplayCodeName", merchDisplayCode.name());
                            merchDisplayCodeMap.put("merchDisplayCodeValue", merchDisplayCode.value());
                            merchDisplayCodeList.add(merchDisplayCodeMap);
                            i++;
                        }
                        ebayResp.put("storeMerchDisplayList",merchDisplayCodeList);
                    }

                    Calendar calendar = returnedStoreType.getLastOpenedTime();
                    ebayResp.put("storeLastOpenedTime", calendar.getTime().toString());
                    ebayResp.put("storeSubscriptionLevel", returnedStoreType.getSubscriptionLevel().value());
                    returnedStoreType.getSubscriptionLevel();
                    StoreSubscriptionLevelCodeType[] storeSubscriptionlevelList = StoreSubscriptionLevelCodeType.values();
                    if (storeSubscriptionlevelList != null) {
                        List<Map<String,Object>> storeSubscriptionLevelCodeList = new LinkedList<Map<String, Object>>();
                        int i = 0;
                        while (i < storeSubscriptionlevelList.length) {
                            Map<String,Object> storeSubscriptionLevelCodeMap = new HashMap<String, Object>();
                            StoreSubscriptionLevelCodeType storeSubscriptionLevelCode= storeSubscriptionlevelList[i];
                            storeSubscriptionLevelCodeMap.put("storeSubscriptionLevelCodeName", storeSubscriptionLevelCode.name());
                            storeSubscriptionLevelCodeMap.put("storeSubscriptionLevelCodeValue", storeSubscriptionLevelCode.value());
                            storeSubscriptionLevelCodeList.add(storeSubscriptionLevelCodeMap);
                            i++;
                        }
                        ebayResp.put("storeSubscriptionLevelList", storeSubscriptionLevelCodeList);
                    }

                    result.put("ebayStore", ebayResp);
                } else {
                    EbayStoreHelper.createErrorLogMessage(userLogin, dctx.getDispatcher(), context.get("productStoreId").toString(), resp.getAck().toString(), "Get store : getEbayStoreOutput", resp.getErrors(0).getLongMessage());
                    result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_ERROR);
                    result.put(ModelService.ERROR_MESSAGE, resp.getAck().toString() +":"+ resp.getMessage());
                }
            } catch (ApiException e) {
                result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_ERROR);
                result.put(ModelService.ERROR_MESSAGE, e.getMessage());
            } catch (SdkSoapException e) {
                result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_ERROR);
                result.put(ModelService.ERROR_MESSAGE, e.getMessage());
            } catch (SdkException e) {
                result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_ERROR);
                result.put(ModelService.ERROR_MESSAGE, e.getMessage());
            }
        }
        return result;
    }

    public static void updateProductStore(DispatchContext dctx, Map<String,Object> context, StoreType returnStoreType, String productStoreId) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        try {
            Map<String,Object> inMap = new HashMap<String, Object>();
            if (returnStoreType != null) {
                inMap.put("productStoreId", productStoreId);
                inMap.put("storeName", returnStoreType.getName());
                inMap.put("subtitle", returnStoreType.getDescription());
                inMap.put("title", returnStoreType.getName());
                inMap.put("userLogin", context.get("userLogin"));
                dispatcher.runSync("updateProductStore", inMap);
            }
        } catch (GenericServiceException e) {
            Debug.logError("error message"+e, module);
        }
    }

    public static Map<String,Object> retrieveThemeColorSchemeByThemeId(DispatchContext dctx, Map<String,Object> context) {
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");
        Delegator delegator = dctx.getDelegator();
        Map<String,Object> result = new HashMap<String, Object>();
        GetStoreOptionsRequestType req = null;
        GetStoreOptionsResponseType resp  = null;
        StoreThemeArrayType returnedBasicThemeArray = null;

        try {
            if (context.get("productStoreId") != null) {
                String themeId = (String)context.get("themeId");

                GetStoreOptionsCall  call = new GetStoreOptionsCall(EbayStoreHelper.getApiContext((String)context.get("productStoreId"), locale, delegator));
                req = new GetStoreOptionsRequestType();

                resp = (GetStoreOptionsResponseType) call.execute(req);
                if (resp != null && "SUCCESS".equals(resp.getAck().toString())) {

                    returnedBasicThemeArray = resp.getBasicThemeArray();
                    StoreThemeType[] storeBasicTheme = returnedBasicThemeArray.getTheme();

                    int i = 0;
                    String colorSchemeId = themeId.substring(themeId.indexOf("-") + 1);
                    themeId = themeId.substring(0,themeId.indexOf("-"));

                    Map<String,Object> storeColorSchemeMap = null;
                    while (i < storeBasicTheme.length) {
                        StoreThemeType storeThemeType = storeBasicTheme[i];
                        if (themeId.equals(storeThemeType.getThemeID().toString())) {
                            StoreColorSchemeType colorSchemeType = storeThemeType.getColorScheme();
                            if (colorSchemeType != null) {
                                if (colorSchemeId.equals(colorSchemeType.getColorSchemeID().toString())) {
                                    // get font,size and color
                                    storeColorSchemeMap = new HashMap<String, Object>();
                                    StoreFontType storeFontType = colorSchemeType.getFont();
                                    storeColorSchemeMap.put("storeFontTypeFontFaceValue", storeFontType.getNameFace().value());
                                    storeColorSchemeMap.put("storeFontTypeSizeFaceValue", storeFontType.getNameSize().value());

                                    storeColorSchemeMap.put("storeFontTypeFontTitleValue", storeFontType.getTitleFace().value());
                                    storeColorSchemeMap.put("storeFontSizeTitleValue", storeFontType.getTitleSize().value());

                                    storeColorSchemeMap.put("storeFontTypeFontDescValue", storeFontType.getDescFace().value());
                                    storeColorSchemeMap.put("storeDescSizeValue", storeFontType.getDescSize().value());
                                    break;
                                }
                            }
                        }
                        i++;
                    }
                    result.put("storeFontScheme", storeColorSchemeMap);
                } else {
                    EbayStoreHelper.createErrorLogMessage(userLogin, dctx.getDispatcher(), context.get("productStoreId").toString(), resp.getAck().toString(), "Get store option : retrieveThemeColorSchemeByThemeId", resp.getErrors(0).getLongMessage());
                }
            }
        } catch (ApiException e) {
            e.printStackTrace();
        } catch (SdkSoapException e) {
            e.printStackTrace();
        } catch (SdkException e) {
            e.printStackTrace();
        }
        return result;
    }

    public static Map<String,Object> retrievePredesignedLogoOption(DispatchContext dctx, Map<String,Object> context) {
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Map<String,Object> result = new HashMap<String, Object>();
        Locale locale = (Locale) context.get("locale");
        Delegator delegator = dctx.getDelegator();
        GetStoreOptionsRequestType req = null;
        StoreLogoArrayType returnedLogoArray = null;
        GetStoreOptionsResponseType resp  = null;
        try {
            if (context.get("productStoreId") != null) {
                GetStoreOptionsCall  call = new GetStoreOptionsCall(EbayStoreHelper.getApiContext((String)context.get("productStoreId"), locale, delegator));
                req = new GetStoreOptionsRequestType();

                resp = (GetStoreOptionsResponseType) call.execute(req);

                if (resp != null && "SUCCESS".equals(resp.getAck().toString())) {
                    returnedLogoArray = resp.getLogoArray();

                    int i = 0;
                    List<Map<String,Object>> logoList = new LinkedList<Map<String, Object>>();
                    while (i < returnedLogoArray.getLogoLength()) {
                        Map<String,Object> logo  = new HashMap<String, Object>();
                        StoreLogoType storeLogoType = returnedLogoArray.getLogo(i);
                        logo.put("storeLogoId", storeLogoType.getLogoID());
                        logo.put("storeLogoName", storeLogoType.getName());
                        logo.put("storeLogoURL", storeLogoType.getURL());
                        logoList.add(logo);
                        i++;
                    }
                    result = ServiceUtil.returnSuccess(UtilProperties.getMessage(resource, "EbayStoreLoadLogoSuccess", locale));
                    result.put("storeLogoOptList", logoList);
                } else {
                    EbayStoreHelper.createErrorLogMessage(userLogin, dctx.getDispatcher(), context.get("productStoreId").toString(), resp.getAck().toString(), "Get store option : retrievePredesignedLogoOption", resp.getErrors(0).getLongMessage());
                }
            }
        } catch (ApiException e) {
            e.printStackTrace();
        } catch (SdkSoapException e) {
            e.printStackTrace();
        } catch (SdkException e) {
            e.printStackTrace();
        }
        return result;
    }

    public static Map<String,Object> retrieveBasicThemeArray(DispatchContext dctx, Map<String,Object> context) {
        Map<String,Object> result = new HashMap<String, Object>();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");
        Delegator delegator = dctx.getDelegator();
        GetStoreOptionsRequestType req = null;
        StoreThemeArrayType returnedBasicThemeArray = null;
        GetStoreOptionsResponseType resp  = null;
        try {
            if (context.get("productStoreId") != null) {
                GetStoreOptionsCall  call = new GetStoreOptionsCall(EbayStoreHelper.getApiContext((String)context.get("productStoreId"), locale, delegator));
                req = new GetStoreOptionsRequestType();

                resp = (GetStoreOptionsResponseType) call.execute(req);

                StoreColorSchemeType storeFontColorSchemeType = null;
                if (resp != null && "SUCCESS".equals(resp.getAck().toString())) {
                    returnedBasicThemeArray = resp.getBasicThemeArray();
                    int i = 0;
                    List<Map<String,Object>> themeList = new LinkedList<Map<String, Object>>();
                    while (i < returnedBasicThemeArray.getThemeLength()) {
                        Map<String,Object> basictheme  = new HashMap<String, Object>();
                        StoreThemeType storeBasicThemeType = returnedBasicThemeArray.getTheme(i);
                        basictheme.put("storeThemeId", storeBasicThemeType.getThemeID());
                        basictheme.put("storeThemeName", storeBasicThemeType.getName());

                        StoreColorSchemeType storeColorSchemeType = storeBasicThemeType.getColorScheme();
                        basictheme.put("storeColorSchemeId",storeColorSchemeType.getColorSchemeID());
                        basictheme.put("storeColorSchemeName",storeColorSchemeType.getName());

                        if (storeFontColorSchemeType == null) {
                            storeFontColorSchemeType = storeBasicThemeType.getColorScheme();
                        }
                        themeList.add(basictheme);
                        i++;
                    }
                    result = ServiceUtil.returnSuccess(UtilProperties.getMessage(resource, "EbayStoreLoadBasicThemeSuccess", locale));
                    result.put("storeThemeList", themeList);
                } else {
                    EbayStoreHelper.createErrorLogMessage(userLogin, dctx.getDispatcher(), context.get("productStoreId").toString(), resp.getAck().toString(), "Get store option : retrieveBasicThemeArray", resp.getErrors(0).getLongMessage());
                }
            }
        } catch (ApiException e) {
            e.printStackTrace();
        } catch (SdkSoapException e) {
            e.printStackTrace();
        } catch (SdkException e) {
            e.printStackTrace();
        }
        return result;
    }

    public static Map<String,Object> retrieveAdvancedThemeArray(DispatchContext dctx, Map<String,Object> context) {
        Map<String,Object> result = new HashMap<String, Object>();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");
        Delegator delegator = dctx.getDelegator();
        GetStoreOptionsRequestType req = null;
        StoreThemeArrayType returnedAdvancedThemeArray = null;
        GetStoreOptionsResponseType resp  = null;
        try {
            if (context.get("productStoreId") != null) {
                GetStoreOptionsCall  call = new GetStoreOptionsCall(EbayStoreHelper.getApiContext((String)context.get("productStoreId"), locale, delegator));
                req = new GetStoreOptionsRequestType();

                resp = (GetStoreOptionsResponseType) call.execute(req);

                if (resp != null && "SUCCESS".equals(resp.getAck().toString())) {
                    result = ServiceUtil.returnSuccess(UtilProperties.getMessage(resource, "EbayStoreLoadAdvancedThemeSuccess", locale));

                    returnedAdvancedThemeArray = resp.getAdvancedThemeArray();

                    int i = 0;
                    List<Map<String,Object>> themeList = new LinkedList<Map<String, Object>>();
                    while (i < returnedAdvancedThemeArray.getThemeLength()) {
                        Map<String,Object> advanceTheme = new HashMap<String, Object>();
                        StoreThemeType storeThemeType = returnedAdvancedThemeArray.getTheme(i);
                        advanceTheme.put("storeThemeId",storeThemeType.getThemeID());
                        advanceTheme.put("storeThemeName",storeThemeType.getName());
                        themeList.add(advanceTheme);
                        i++;
                    }
                    result.put("storeThemeList", themeList);
                    int j = 0;
                    StoreColorSchemeType[] storeColorSchemeTypes = returnedAdvancedThemeArray.getGenericColorSchemeArray().getColorScheme();
                    List<Map<String,Object>> themeColorList = new LinkedList<Map<String, Object>>();
                    while (j < storeColorSchemeTypes.length) {
                        Map<String,Object> advanceColorTheme = new HashMap<String, Object>();
                        StoreColorSchemeType storeColorSchemeType = storeColorSchemeTypes[j];
                        advanceColorTheme.put("storeColorSchemeId", storeColorSchemeType.getColorSchemeID());
                        advanceColorTheme.put("storeColorName", storeColorSchemeType.getName());
                        themeColorList.add(advanceColorTheme);
                        j++;
                    }
                    result.put("storeAdvancedThemeColorOptList", themeColorList);
                } else {
                    EbayStoreHelper.createErrorLogMessage(userLogin, dctx.getDispatcher(), context.get("productStoreId").toString(), resp.getAck().toString(), "Get store option : retrieveAdvancedThemeArray", resp.getErrors(0).getLongMessage());
                }
                //this.returnedSubscriptionArray = resp.getSubscriptionArray();
            }
        } catch (ApiException e) {
            e.printStackTrace();
        } catch (SdkSoapException e) {
            e.printStackTrace();
        } catch (SdkException e) {
            e.printStackTrace();
        }
        return result;
    }

    public static Map<String,Object> retrieveStoreFontTheme(DispatchContext dctx, Map<String,Object> context) {
        Map<String,Object> result = new HashMap<String, Object>();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");
        Delegator delegator = dctx.getDelegator();
        GetStoreOptionsRequestType req = null;
        StoreThemeArrayType returnedThemeArray = null;
        GetStoreOptionsResponseType resp  = null;
        try {
            if (context.get("productStoreId") != null) {
                GetStoreOptionsCall  call = new GetStoreOptionsCall(EbayStoreHelper.getApiContext((String)context.get("productStoreId"), locale, delegator));
                req = new GetStoreOptionsRequestType();

                resp = (GetStoreOptionsResponseType) call.execute(req);

                Map<String,Object> advanceFontTheme = new HashMap<String, Object>();
                if (resp != null && "SUCCESS".equals(resp.getAck().toString())) {
                    returnedThemeArray = resp.getAdvancedThemeArray();
                    int i = 0;
                    StoreColorSchemeType[] storeColorSchemeTypes = returnedThemeArray.getGenericColorSchemeArray().getColorScheme();
                    while (i < storeColorSchemeTypes.length) {

                        StoreColorSchemeType storeColorSchemeType = storeColorSchemeTypes[i];
                        StoreFontType storeFontType =  storeColorSchemeType.getFont();
                        advanceFontTheme.put("storeFontTypeNameFaceColor",storeFontType.getNameColor());
                        int j = 0;
                        storeFontType.getNameFace();
                        StoreFontFaceCodeType[] storeFontNameFaceCodeTypes = StoreFontFaceCodeType.values();
                        List<Map<String,Object>> nameFaces = new LinkedList<Map<String, Object>>();
                        while (j < storeFontNameFaceCodeTypes.length) {
                            Map<String,Object> storeFontNameFaceCodeTypeMap = new HashMap<String, Object>();
                            StoreFontFaceCodeType storeFontNameFaceCodeType = storeFontNameFaceCodeTypes[j];
                            storeFontNameFaceCodeTypeMap.put("storeFontValue", storeFontNameFaceCodeType.value());
                            storeFontNameFaceCodeTypeMap.put("storeFontName", storeFontNameFaceCodeType.name());
                            nameFaces.add(storeFontNameFaceCodeTypeMap);
                            j++;
                        }
                        advanceFontTheme.put("storeFontTypeFontFaceList",nameFaces);
                        j = 0;
                        storeFontType.getNameSize();
                        StoreFontSizeCodeType[] storeFontSizeCodeTypes =  StoreFontSizeCodeType.values();
                        List<Map<String,Object>> sizeFaces = new LinkedList<Map<String, Object>>();
                        while (j < storeFontSizeCodeTypes.length) {
                            Map<String,Object> storeFontSizeCodeTypeMap = new HashMap<String, Object>();
                            StoreFontSizeCodeType storeFontSizeCodeType = storeFontSizeCodeTypes[j];
                            storeFontSizeCodeTypeMap.put("storeFontSizeValue", storeFontSizeCodeType.value());
                            storeFontSizeCodeTypeMap.put("storeFontSizeName", storeFontSizeCodeType.name());
                            sizeFaces.add(storeFontSizeCodeTypeMap);
                            j++;
                        }
                        advanceFontTheme.put("storeFontTypeSizeFaceList", sizeFaces);

                        advanceFontTheme.put("storeFontTypeTitleColor", storeFontType.getTitleColor());
                        j = 0;
                        storeFontType.getTitleFace();
                        StoreFontFaceCodeType[] storeFontTypeTitleFaces = StoreFontFaceCodeType.values();
                        List<Map<String,Object>> titleFaces = new LinkedList<Map<String, Object>>();
                        while (j < storeFontTypeTitleFaces.length) {
                            Map<String,Object> storeFontTypeTitleFaceMap = new HashMap<String, Object>();
                            StoreFontFaceCodeType storeFontTypeTitleFace = storeFontTypeTitleFaces[j];
                            storeFontTypeTitleFaceMap.put("storeFontValue", storeFontTypeTitleFace.value());
                            storeFontTypeTitleFaceMap.put("storeFontName", storeFontTypeTitleFace.name());
                            titleFaces.add(storeFontTypeTitleFaceMap);
                            j++;
                        }
                        advanceFontTheme.put("storeFontTypeFontTitleList",titleFaces);

                        j = 0;
                        storeFontType.getTitleSize();
                        StoreFontSizeCodeType[] storeTitleSizeCodeTypes =  StoreFontSizeCodeType.values();
                        List<Map<String,Object>> titleSizes = new LinkedList<Map<String, Object>>();
                        while (j < storeTitleSizeCodeTypes.length) {
                            Map<String,Object> storeFontSizeCodeTypeMap = new HashMap<String, Object>();
                            StoreFontSizeCodeType storeFontSizeCodeType = storeTitleSizeCodeTypes[j];
                            storeFontSizeCodeTypeMap.put("storeFontSizeValue", storeFontSizeCodeType.value());
                            storeFontSizeCodeTypeMap.put("storeFontSizeName", storeFontSizeCodeType.name());
                            titleSizes.add(storeFontSizeCodeTypeMap);
                            j++;
                        }
                        advanceFontTheme.put("storeFontSizeTitleList",titleSizes);


                        advanceFontTheme.put("storeFontTypeDescColor", storeFontType.getDescColor());
                        j = 0;
                        storeFontType.getDescFace();
                        StoreFontFaceCodeType[] storeFontTypeDescFaces = StoreFontFaceCodeType.values();
                        List<Map<String,Object>> descFaces = new LinkedList<Map<String, Object>>();
                        while (j < storeFontTypeDescFaces.length) {
                            Map<String,Object> storeFontTypeDescFaceMap = new HashMap<String, Object>();
                            StoreFontFaceCodeType storeFontTypeDescFace = storeFontTypeDescFaces[j];
                            storeFontTypeDescFaceMap.put("storeFontValue", storeFontTypeDescFace.value());
                            storeFontTypeDescFaceMap.put("storeFontName", storeFontTypeDescFace.name());
                            descFaces.add(storeFontTypeDescFaceMap);
                            j++;
                        }
                        advanceFontTheme.put("storeFontTypeFontDescList",descFaces);

                        j = 0;
                        storeFontType.getDescSize();
                        StoreFontSizeCodeType[] storeDescSizeCodeTypes =   StoreFontSizeCodeType.values();
                        List<Map<String,Object>> descSizes = new LinkedList<Map<String, Object>>();
                        while (j < storeDescSizeCodeTypes.length) {
                            Map<String,Object> storeFontSizeCodeTypeMap = new HashMap<String, Object>();
                            StoreFontSizeCodeType storeFontSizeCodeType = storeDescSizeCodeTypes[j];
                            storeFontSizeCodeTypeMap.put("storeFontSizeValue", storeFontSizeCodeType.value());
                            storeFontSizeCodeTypeMap.put("storeFontSizeName", storeFontSizeCodeType.name());
                            descSizes.add(storeFontSizeCodeTypeMap);
                            j++;
                        }
                        advanceFontTheme.put("storeDescSizeList",descSizes);
                        i++;
                    }
                    result = ServiceUtil.returnSuccess(UtilProperties.getMessage(resource, "EbayStoreLoadBasicThemeSuccess", locale));
                    result.put("advanceFontTheme", advanceFontTheme);
                } else {
                    EbayStoreHelper.createErrorLogMessage(userLogin, dctx.getDispatcher(), context.get("productStoreId").toString(), resp.getAck().toString(), "Get store option : retrieveStoreFontTheme", resp.getErrors(0).getLongMessage());
                }
            }
        } catch (ApiException e) {
            e.printStackTrace();
        } catch (SdkSoapException e) {
            e.printStackTrace();
        } catch (SdkException e) {
            e.printStackTrace();
        }
        return result;
    }

    public static Map<String,Object>  setEbayStoreInput(DispatchContext dctx, Map<String,Object> context) {
        Map<String,Object> result = new HashMap<String, Object>();
        Locale locale = (Locale) context.get("locale");
        Delegator delegator = dctx.getDelegator();
        SetStoreRequestType req = null;
        SetStoreResponseType resp  = null;
        StoreType storeType = null;
        try {
            if (context.get("productStoreId") != null) {
                SetStoreCall  call = new SetStoreCall(EbayStoreHelper.getApiContext((String)context.get("productStoreId"), locale, delegator));
                req = new SetStoreRequestType();

                storeType = new StoreType();
                storeType.setName((String)context.get("storeName"));
                storeType.setDescription((String)context.get("storeDesc"));
                storeType.setURL((String)context.get("storeUrl"));
                storeType.setURLPath("");
                StoreLogoType storeLogo = new StoreLogoType();
                if (context.get("storeLogoURL") == null) {
                    if (context.get("storeLogoId") != null) storeLogo.setLogoID(Integer.parseInt((String)context.get("storeLogoId")));
                    storeLogo.setName((String)context.get("storeLogoName"));
                } else {
                    storeLogo.setURL((String)context.get("storeLogoURL"));
                }
                storeType.setLogo(storeLogo);

                StoreThemeType storeTheme = new StoreThemeType();
                StoreColorSchemeType storeColorScheme = null;
                StoreColorType storecolor = null;
                StoreFontType storeFont = null;
                if (context.get("themeType").equals("Advanced")) {
                    storeColorScheme = new StoreColorSchemeType();
                    if (context.get("storeAdvancedThemeColor") != null) storeColorScheme.setColorSchemeID(Integer.parseInt((String)context.get("storeAdvancedThemeColor")));

                    storecolor = new StoreColorType();
                    storecolor.setPrimary((String)context.get("storePrimaryColor"));
                    storecolor.setSecondary((String)context.get("storeSecondaryColor"));
                    storecolor.setAccent((String)context.get("storeAccentColor"));
                    storeColorScheme.setColor(storecolor);
                    storeTheme.setColorScheme(storeColorScheme);
                    storeTheme.setName(null);
                    storeTheme.setThemeID(Integer.parseInt((String)context.get("storeAdvancedTheme")));
                } else if (context.get("themeType").equals("Basic")) {
                    storeColorScheme = new StoreColorSchemeType();
                    if (context.get("storeBasicTheme")!=null) {
                        String storeBasicTheme = (String)context.get("storeBasicTheme");
                        String storeThemeId = null;
                        String storeColorSchemeId = null;
                        if (storeBasicTheme.indexOf("-") != -1) {
                            storeThemeId = storeBasicTheme.substring(0, storeBasicTheme.indexOf("-"));
                            storeColorSchemeId = storeBasicTheme.substring(storeBasicTheme.indexOf("-")+1);
                        }
                        if (storeColorSchemeId != null) storeColorScheme.setColorSchemeID(Integer.parseInt(storeColorSchemeId));

                        storecolor = new StoreColorType();
                        storecolor.setPrimary((String)context.get("storePrimaryColor"));
                        storecolor.setSecondary((String)context.get("storeSecondaryColor"));
                        storecolor.setAccent((String)context.get("storeAccentColor"));
                        storeColorScheme.setColor(storecolor);

                        storeFont = new StoreFontType();
                        storeFont.setNameColor((String)context.get("storeNameFontColor"));
                        storeFont.setNameFace(StoreFontFaceCodeType.valueOf((String)context.get("storeNameFont")));
                        storeFont.setNameSize(StoreFontSizeCodeType.valueOf((String)context.get("storeNameFontSize")));

                        storeFont.setTitleColor((String)context.get("storeTitleFontColor"));
                        storeFont.setTitleFace(StoreFontFaceCodeType.valueOf((String)context.get("storeTitleFont")));
                        storeFont.setTitleSize(StoreFontSizeCodeType.valueOf((String)context.get("storeTitleFontSize")));

                        storeFont.setDescColor((String)context.get("storeDescFontColor"));
                        storeFont.setDescFace(StoreFontFaceCodeType.valueOf((String)context.get("storeDescFont")));
                        storeFont.setDescSize(StoreFontSizeCodeType.valueOf((String)context.get("storeDescFontSize")));

                        storeColorScheme.setFont(storeFont);

                        storeTheme.setColorScheme(storeColorScheme);
                        storeTheme.setName(null);
                        storeTheme.setThemeID(Integer.parseInt(storeThemeId));
                    }
                }
                storeType.setTheme(storeTheme);
                storeType.setHeaderStyle(StoreHeaderStyleCodeType.valueOf((String)context.get("storeHeaderStyle")));
                storeType.setItemListLayout(StoreItemListLayoutCodeType.valueOf((String)context.get("storeItemLayout")));
                storeType.setItemListSortOrder(StoreItemListSortOrderCodeType.valueOf((String)context.get("storeItemSortOrder")));
                storeType.setMerchDisplay(MerchDisplayCodeType.valueOf((String)context.get("storeMerchDisplay")));
                storeType.setSubscriptionLevel(StoreSubscriptionLevelCodeType.valueOf((String)context.get("storeSubscriptionDisplay")));

                storeType.setCustomHeader((String)context.get("storeCustomHeader"));
                storeType.setCustomHeaderLayout(StoreCustomHeaderLayoutCodeType.valueOf((String)context.get("storeCustomHeaderLayout")));

                req.setStore(storeType);
                resp = (SetStoreResponseType) call.execute(req);

                if (resp != null && "SUCCESS".equals(resp.getAck().toString())) {
                    result = ServiceUtil.returnSuccess(UtilProperties.getMessage(resource, "EbayStoreSaveSuccess",locale));
                } else {
                    result = ServiceUtil.returnError(resp.getMessage());
                }
                LocalDispatcher dispatcher = dctx.getDispatcher();
                Map<String,Object> results = dispatcher.runSync("getEbayStoreOutput",UtilMisc.toMap("productStoreId",(String) context.get("productStoreId"),"userLogin",context.get("userLogin")));
                if (results != null) {
                    result.put("ebayStore", results.get("ebayStore"));
                }
            }
        } catch (ApiException e) {
            result = ServiceUtil.returnError(e.getMessage());
        } catch (SdkSoapException e) {
            result = ServiceUtil.returnError(e.getMessage());
        } catch (SdkException e) {
            result = ServiceUtil.returnError(e.getMessage());
        } catch (GenericServiceException e) {
            result = ServiceUtil.returnError(e.getMessage());
        }
        return result;
    }

    public static Map<String, Object> getEbayActiveItems(DispatchContext dctx, Map<String, ? extends Object> context) {
        Map<String, Object> result = new HashMap<String, Object>();
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get("locale");
        String productStoreId = (String) context.get("productStoreId");
        List<Map<String, Object>> activeItems = new LinkedList<Map<String, Object>>();
        try {
            ApiContext apiContext = EbayStoreHelper.getApiContext(productStoreId, locale, delegator);
            GetMyeBaySellingCall getMyeBaySellingCall = new GetMyeBaySellingCall(apiContext);
            ItemListCustomizationType activeList = new ItemListCustomizationType();
            getMyeBaySellingCall.setActiveList(activeList );
            DetailLevelCodeType[] level = {DetailLevelCodeType.RETURN_ALL};
            getMyeBaySellingCall.setDetailLevel(level);
            getMyeBaySellingCall.getMyeBaySelling();
            PaginatedItemArrayType itemListCustomizationType = getMyeBaySellingCall.getReturnedActiveList();

            if (itemListCustomizationType != null) {
                ItemArrayType itemArrayType = itemListCustomizationType.getItemArray();
                int itemArrayTypeSize = itemArrayType.getItemLength();
                for (int i = 0; i < itemArrayTypeSize; i++) {
                    Map<String, Object> entry = new HashMap<String, Object>();
                    ItemType item = itemArrayType.getItem(i);
                    entry.put("itemId", item.getItemID());
                    entry.put("title", item.getTitle());
                    if (item.getPictureDetails() != null) {
                        String url[] = item.getPictureDetails().getPictureURL();
                        if (url.length != 0) {
                            entry.put("pictureURL", url[0]);
                        } else {
                            entry.put("pictureURL", null);
                        }
                    } else {
                        entry.put("pictureURL", null);
                    }
                    entry.put("timeLeft",item.getTimeLeft());
                    if (item.getBuyItNowPrice() != null) {
                        entry.put("buyItNowPrice", item.getBuyItNowPrice().getValue());
                    } else {
                        entry.put("buyItNowPrice", null);
                    }
                    if (item.getStartPrice() != null) {
                        entry.put("startPrice", item.getStartPrice().getValue());
                    } else {
                        entry.put("startPrice", null);
                    }
                    if (item.getListingDetails() != null) {
                        entry.put("relistedItemId", item.getListingDetails().getRelistedItemID());
                    } else {
                        entry.put("relistedItemId", null);
                    }
                    if (item.getListingType() != null) {
                    entry.put("listingType", item.getListingType().value());
                    } else {
                        entry.put("listingType", null);
                    }
                    activeItems.add(entry);
                }
            }
            result.put("activeItems", activeItems);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            return ServiceUtil.returnError(e.getMessage());
        }
        return result;
    }

    public static Map<String, Object> getEbaySoldItems(DispatchContext dctx, Map<String, ? extends Object> context) {
        Map<String, Object> result = new HashMap<String, Object>();
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get("locale");
        String productStoreId = (String) context.get("productStoreId");
        String filter = (String) context.get("filter");
        String itemId = (String) context.get("itemId");
        String buyerId = (String) context.get("buyerId");
        List<Map<String, Object>> soldItems = new LinkedList<Map<String, Object>>();
        double reservPrice = 0;
        try {
            ApiContext apiContext = EbayStoreHelper.getApiContext(productStoreId, locale, delegator);
            GetSellingManagerSoldListingsCall sellingManagerSoldListings = new GetSellingManagerSoldListingsCall(apiContext);
            if (UtilValidate.isNotEmpty(filter)) {
                SellingManagerSoldListingsPropertyTypeCodeType[] filterObject = {SellingManagerSoldListingsPropertyTypeCodeType.valueOf(filter)};
                sellingManagerSoldListings.setFilter(filterObject );
            }
            if (UtilValidate.isNotEmpty(itemId)) {
                SellingManagerSearchType search = new SellingManagerSearchType();
                search.setSearchType(SellingManagerSearchTypeCodeType.ITEM_ID);
                search.setSearchValue(itemId);
                sellingManagerSoldListings.setSearch(search);
            }
            if (UtilValidate.isNotEmpty(buyerId)) {
                SellingManagerSearchType search = new SellingManagerSearchType();
                search.setSearchType(SellingManagerSearchTypeCodeType.BUYER_USER_ID);
                search.setSearchValue(buyerId);
                sellingManagerSoldListings.setSearch(search);
            }
            sellingManagerSoldListings.getSellingManagerSoldListings();
            SellingManagerSoldOrderType[] sellingManagerSoldOrders = sellingManagerSoldListings.getReturnedSaleRecord();

            if (UtilValidate.isNotEmpty(sellingManagerSoldOrders)) {
                int soldOrderLength = sellingManagerSoldOrders.length;
                for (int i = 0; i < soldOrderLength; i++) {
                    SellingManagerSoldOrderType sellingManagerSoldOrder = sellingManagerSoldOrders[i];
                    if (sellingManagerSoldOrder != null) {
                        SellingManagerSoldTransactionType[] sellingManagerSoldTransactions = sellingManagerSoldOrder.getSellingManagerSoldTransaction();
                        int sellingManagerSoldTransactionLength = sellingManagerSoldTransactions.length;
                        for (int j = 0; j < sellingManagerSoldTransactionLength; j++) {
                            Map<String, Object> entry = new HashMap<String, Object>();
                            SellingManagerSoldTransactionType sellingManagerSoldTransaction = sellingManagerSoldTransactions[j];
                            entry.put("itemId", sellingManagerSoldTransaction.getItemID());
                            entry.put("title", sellingManagerSoldTransaction.getItemTitle());
                            entry.put("transactionId", sellingManagerSoldTransaction.getTransactionID().toString());
                            entry.put("quantity", sellingManagerSoldTransaction.getQuantitySold());
                            entry.put("listingType", sellingManagerSoldTransaction.getListingType().value());

                            String buyer = null;
                            if (sellingManagerSoldOrder.getBuyerID() != null) {
                                buyer  = sellingManagerSoldOrder.getBuyerID();
                            }
                            entry.put("buyer", buyer);
                            String buyerEmail = null;
                            if (sellingManagerSoldOrder.getBuyerID() != null) {
                                buyerEmail  = sellingManagerSoldOrder.getBuyerEmail();
                            }
                            entry.put("buyerEmail", buyerEmail);
                            GetItemCall api = new GetItemCall(apiContext);
                            api.setItemID(sellingManagerSoldTransaction.getItemID());
                            DetailLevelCodeType[] detailLevels = new DetailLevelCodeType[] {
                                      DetailLevelCodeType.RETURN_ALL,
                                      DetailLevelCodeType.ITEM_RETURN_ATTRIBUTES,
                                      DetailLevelCodeType.ITEM_RETURN_DESCRIPTION
                                  };
                            api.setDetailLevel(detailLevels);
                            ItemType itemType = api.getItem();
                            String itemUrl = null;

                            entry.put("SKU", itemType.getSKU());
                            if (UtilValidate.isNotEmpty(itemType.getReservePrice())) reservPrice = itemType.getReservePrice().getValue();
                            entry.put("reservePrice", reservPrice);
                            entry.put("hitCount", itemType.getHitCount() != null ? itemType.getHitCount() : 0);

                            if (itemType.getListingDetails() != null) {
                                itemUrl  = itemType.getListingDetails().getViewItemURL();
                            }
                            entry.put("itemUrl", itemUrl);
                            String itemUrlNatural = null;
                            if (itemType.getListingDetails() != null) {
                                itemUrlNatural  = itemType.getListingDetails().getViewItemURLForNaturalSearch();
                            }
                            entry.put("itemUrlNatural", itemUrlNatural);
                            String unpaidItemStatus = null;
                            if (sellingManagerSoldOrder.getUnpaidItemStatus() != null) {
                                unpaidItemStatus  = sellingManagerSoldOrder.getUnpaidItemStatus().value();
                            }
                            entry.put("unpaidItemStatus", unpaidItemStatus);
                            Date creationTime = null;
                            if (sellingManagerSoldOrder.getCreationTime() != null) {
                                creationTime = sellingManagerSoldOrder.getCreationTime().getTime();
                            }
                            entry.put("creationTime", creationTime);
                            double totalAmount = 0;
                            if (sellingManagerSoldOrder.getTotalAmount() != null) {
                                totalAmount  = sellingManagerSoldOrder.getTotalAmount().getValue();
                            }
                            entry.put("totalAmount", totalAmount);
                            if (sellingManagerSoldOrder.getSalePrice() != null) {
                                entry.put("salePrice", sellingManagerSoldOrder.getSalePrice().getValue());
                            }
                            Date paidTime = null;
                            String checkoutStatus = null;
                            String shippedStatus = null;
                            Date shippedTime = null;
                            if (sellingManagerSoldOrder.getOrderStatus() != null) {
                                if (sellingManagerSoldOrder.getOrderStatus().getPaidTime() != null) {
                                    paidTime  = sellingManagerSoldOrder.getOrderStatus().getPaidTime().getTime();
                                }
                                if (sellingManagerSoldOrder.getOrderStatus().getCheckoutStatus() != null) {
                                    checkoutStatus  = sellingManagerSoldOrder.getOrderStatus().getCheckoutStatus().value();
                                }
                                if (sellingManagerSoldOrder.getOrderStatus().getShippedStatus() != null) {
                                    shippedStatus = sellingManagerSoldOrder.getOrderStatus().getShippedStatus().value();
                                }
                                if (sellingManagerSoldOrder.getOrderStatus().getShippedTime() != null) {
                                    shippedTime = sellingManagerSoldOrder.getOrderStatus().getShippedTime().getTime();
                                }
                            }
                            entry.put("paidTime", paidTime);
                            entry.put("checkoutStatus", checkoutStatus);
                            entry.put("shippedStatus", shippedStatus);
                            entry.put("shippedTime", shippedTime);
                            soldItems.add(entry);
                        }
                    }
                }
            }
            result.put("soldItems", soldItems);
        } catch (Exception e) {
            return ServiceUtil.returnError(e.getMessage());
        }
        return result;
    }

    public static Map<String, Object> exportProductsFromEbayStore(DispatchContext dctx, Map<String, Object> context) {
        Map<String,Object> result = new HashMap<String, Object>();
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Map<String, Object> eBayConfigResult = EbayHelper.buildEbayConfig(context, delegator);
        Map<String, Object> response = null;
        try {
            GenericValue product = EntityQuery.use(delegator).from("Product").where("productId", context.get("productId").toString()).queryOne();
            int intAtp = 1;
            String facilityId = "";
            if (UtilValidate.isNotEmpty(context.get("requireEbayInventory")) && "on".equals(context.get("requireEbayInventory").toString())) {
                GenericValue ebayProductStore = EntityQuery.use(delegator).from("EbayProductStoreInventory").where("productStoreId", context.get("productStoreId").toString(), "productId", context.get("productId")).filterByDate().queryFirst();
                if (ebayProductStore != null) {
                    facilityId = ebayProductStore.getString("facilityId");
                    BigDecimal atp = ebayProductStore.getBigDecimal("availableToPromiseListing");
                    intAtp = atp.intValue();
                    if (intAtp == 0) {
                        result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_FAIL);
                        result.put(ModelService.ERROR_MESSAGE, "ATP is not enough, can not create listing.");
                    }
                }
            }
            GenericValue userLogin = (GenericValue) context.get("userLogin");
            if (UtilValidate.isNotEmpty(context.get("productCategoryId"))) {
                GenericValue prodCategoryMember = EntityQuery.use(delegator).from("ProductCategoryMember").where("productCategoryId", context.get("productCategoryId"),"productId", context.get("productId")).filterByDate().queryFirst();
                if (prodCategoryMember != null) {
                    GenericValue prodCategoryRole = EntityQuery.use(delegator).from("ProductCategoryRole").where("productCategoryId", prodCategoryMember.get("productCategoryId").toString(), "partyId", userLogin.get("partyId"),"roleTypeId", "EBAY_ACCOUNT").filterByDate().queryFirst();
                    if (prodCategoryRole != null) {
                        context.put("ebayCategory", prodCategoryRole.get("comments"));
                    } else {
                        result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_FAIL);
                        result.put(ModelService.ERROR_MESSAGE, "Category not found for this product on ebay.");
                    }
                }
            } else {
                List<GenericValue> prodCategoryMember = EntityQuery.use(delegator).from("ProductCategoryMember").where("productId", context.get("productId")).filterByDate().queryList();
                Iterator<GenericValue> prodCategoryMemberIter = prodCategoryMember.iterator();
                while (prodCategoryMemberIter.hasNext()) {
                    GenericValue prodCategory = prodCategoryMemberIter.next();
                    GenericValue prodCatalogCategory = EntityQuery.use(delegator).from("ProdCatalogCategory").where("prodCatalogId", context.get("prodCatalogId"), "productCategoryId", prodCategory.get("productCategoryId").toString()).filterByDate().queryFirst();
                    if (prodCatalogCategory != null) {
                        GenericValue prodCategoryRole = EntityQuery.use(delegator).from("ProductCategoryRole").where("productCategoryId", prodCatalogCategory.get("productCategoryId").toString(), "partyId", userLogin.get("partyId"),"roleTypeId", "EBAY_ACCOUNT").filterByDate().queryFirst();
                        if (prodCategoryRole != null) {
                            context.put("ebayCategory", prodCategoryRole.get("comments"));
                        } else {
                            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_FAIL);
                            result.put(ModelService.ERROR_MESSAGE, "Category not found for this product on ebay.");
                        }
                    }
                }
            }

            if (intAtp != 0) {
                if (UtilValidate.isNotEmpty(context.get("listingTypeAuc")) && "on".equals(context.get("listingTypeAuc").toString())) {
                    context.put("listingFormat", "Chinese");
                    context.put("listingDuration",  context.get("listingDurationAuc").toString());

                    StringBuffer dataItemsXml = new StringBuffer();
                    Map<String, Object> resultMap = ProductsExportToEbay.buildDataItemsXml(dctx, context, dataItemsXml, eBayConfigResult.get("token").toString(), product);
                    if (!ServiceUtil.isFailure(resultMap)) {
                        response = postItem(eBayConfigResult.get("xmlGatewayUri").toString(), dataItemsXml, eBayConfigResult.get("devID").toString(), eBayConfigResult.get("appID").toString(), eBayConfigResult.get("certID").toString(), "AddItem", eBayConfigResult.get("compatibilityLevel").toString(), eBayConfigResult.get("siteID").toString());
                        if (ServiceUtil.isFailure(response)) {
                            return ServiceUtil.returnFailure(ServiceUtil.getErrorMessage(response));
                        }
                        if (UtilValidate.isNotEmpty(response)) {
                            ProductsExportToEbay.exportToEbayResponse((String) response.get("successMessage"), product);
                        }
                    } else {
                        return ServiceUtil.returnFailure(ServiceUtil.getErrorMessage(resultMap));
                    }
                }

                if (UtilValidate.isNotEmpty(context.get("listingTypeFixed")) && "on".equals(context.get("listingTypeFixed").toString())) {
                    context.put("listingFormat", "FixedPriceItem");
                    context.put("listingDuration", context.get("listingDurationFixed").toString());

                    StringBuffer dataItemsXml = new StringBuffer();
                    Map<String, Object> resultMap = ProductsExportToEbay.buildDataItemsXml(dctx, context, dataItemsXml, eBayConfigResult.get("token").toString(), product);
                    if (!ServiceUtil.isFailure(resultMap)) {
                        response = postItem(eBayConfigResult.get("xmlGatewayUri").toString(), dataItemsXml, eBayConfigResult.get("devID").toString(), eBayConfigResult.get("appID").toString(), eBayConfigResult.get("certID").toString(), "AddItem", eBayConfigResult.get("compatibilityLevel").toString(), eBayConfigResult.get("siteID").toString());
                        if (ServiceUtil.isFailure(response)) {
                            return ServiceUtil.returnFailure(ServiceUtil.getErrorMessage(response));
                        }
                        if (UtilValidate.isNotEmpty(response)) {
                            ProductsExportToEbay.exportToEbayResponse((String) response.get("successMessage"), product);
                        }
                    } else {
                        return ServiceUtil.returnFailure(ServiceUtil.getErrorMessage(resultMap));
                    }
                }
            }

            if (UtilValidate.isNotEmpty(ProductsExportToEbay.getProductExportSuccessMessageList())) {
                if ((facilityId != "")  && (intAtp != 0)) {
                    int newAtp = intAtp - 1;
                    Map<String,Object> inMap = new HashMap<String, Object>();
                    inMap.put("productStoreId", context.get("productStoreId").toString());
                    inMap.put("facilityId", facilityId);
                    inMap.put("productId", context.get("productId"));
                    inMap.put("availableToPromiseListing", new BigDecimal(newAtp));
                    inMap.put("userLogin", context.get("userLogin"));
                    dispatcher.runSync("updateEbayProductStoreInventory", inMap);
                }
                result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
                result.put(ModelService.SUCCESS_MESSAGE, "Export products listing success..");
            }

            if (UtilValidate.isNotEmpty(ProductsExportToEbay.getproductExportFailureMessageList())) {
                result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_FAIL);
                result.put(ModelService.ERROR_MESSAGE_LIST, ProductsExportToEbay.getproductExportFailureMessageList());
            }
        } catch (GenericEntityException|GenericServiceException ge) {
            return ServiceUtil.returnError(ge.getMessage());
        } catch (Exception e) {
            return ServiceUtil.returnError(e.getMessage());
        }
        return result;
    }

    public static DisputeExplanationCodeType getEbayDisputeExplanationCodeType(String disputeExplanationCode) {
        DisputeExplanationCodeType disputeExplanationCodeType = null;
        if (disputeExplanationCode != null) {
            if (disputeExplanationCode.equals("BUYER_HAS_NOT_RESPONDED")) {
                disputeExplanationCodeType = DisputeExplanationCodeType.BUYER_HAS_NOT_RESPONDED;
            } else if (disputeExplanationCode.equals("BUYER_REFUSED_TO_PAY")) {
                disputeExplanationCodeType = DisputeExplanationCodeType.BUYER_REFUSED_TO_PAY;
            } else if (disputeExplanationCode.equals("BUYER_RETURNED_ITEM_FOR_REFUND")) {
                disputeExplanationCodeType = DisputeExplanationCodeType.BUYER_RETURNED_ITEM_FOR_REFUND;
            } else if (disputeExplanationCode.equals("UNABLE_TO_RESOLVE_TERMS")) {
                disputeExplanationCodeType = DisputeExplanationCodeType.UNABLE_TO_RESOLVE_TERMS;
            } else if (disputeExplanationCode.equals("BUYER_PURCHASING_MISTAKE")) {
                disputeExplanationCodeType = DisputeExplanationCodeType.BUYER_PURCHASING_MISTAKE;
            } else if (disputeExplanationCode.equals("SHIP_COUNTRY_NOT_SUPPORTED")) {
                disputeExplanationCodeType = DisputeExplanationCodeType.SHIP_COUNTRY_NOT_SUPPORTED;
            } else if (disputeExplanationCode.equals("SHIPPING_ADDRESS_NOT_CONFIRMED")) {
                disputeExplanationCodeType = DisputeExplanationCodeType.SHIPPING_ADDRESS_NOT_CONFIRMED;
            } else if (disputeExplanationCode.equals("PAYMENT_METHOD_NOT_SUPPORTED")) {
                disputeExplanationCodeType = DisputeExplanationCodeType.PAYMENT_METHOD_NOT_SUPPORTED;
            } else if (disputeExplanationCode.equals("BUYER_NO_LONGER_REGISTERED")) {
                disputeExplanationCodeType = DisputeExplanationCodeType.BUYER_NO_LONGER_REGISTERED;
            } else if (disputeExplanationCode.equals("BUYER_NO_LONGER_REGISTERED")) {
                disputeExplanationCodeType = DisputeExplanationCodeType.BUYER_NO_LONGER_REGISTERED;
            } else {
                disputeExplanationCodeType = DisputeExplanationCodeType.OTHER_EXPLANATION;
            }
        } else {
            disputeExplanationCodeType = DisputeExplanationCodeType.OTHER_EXPLANATION;
        }
        return disputeExplanationCodeType;
    }

    public static DisputeReasonCodeType getEbayDisputeReasonCodeType(String disputeReasonCode) {
        DisputeReasonCodeType disputeReasonCodeType = null;
        if (disputeReasonCode != null) {
            if (disputeReasonCode.equals("TRANSACTION_MUTUALLY_CANCELED")) {
                disputeReasonCodeType = DisputeReasonCodeType.TRANSACTION_MUTUALLY_CANCELED;
            } else if (disputeReasonCode.equals("BUYER_HAS_NOT_PAID")) {
                disputeReasonCodeType = DisputeReasonCodeType.BUYER_HAS_NOT_PAID;
            }
        }
        return disputeReasonCodeType;
    }

    public static Map<String, Object> addEbayDispute(DispatchContext dctx, Map<String, ? extends Object> context) {
        Map<String, Object> result = new HashMap<String, Object>();
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get("locale");
        String productStoreId = (String) context.get("productStoreId");
        try {
            String itemId = (String) context.get("itemId");
            String transactionId = (String) context.get("transactionId");
            DisputeReasonCodeType drct = EbayStore.getEbayDisputeReasonCodeType((String)context.get("disputeReasonCodeType"));
            DisputeExplanationCodeType dect = EbayStore.getEbayDisputeExplanationCodeType((String) context.get("disputeExplanationCodeType"));
            DetailLevelCodeType[] detailLevels = new DetailLevelCodeType[] {
                    DetailLevelCodeType.RETURN_ALL,
                    DetailLevelCodeType.ITEM_RETURN_ATTRIBUTES,
                    DetailLevelCodeType.ITEM_RETURN_DESCRIPTION
                };
            ApiContext apiContext = EbayStoreHelper.getApiContext(productStoreId, locale, delegator);
            AddDisputeCall api = new AddDisputeCall(apiContext);
            api.setDetailLevel(detailLevels);
            api.setItemID(itemId);
            api.setTransactionID(transactionId);
            api.setDisputeExplanation(dect);
            api.setDisputeReason(drct);

            String disputeId = api.addDispute();
            result.put("disputeId", disputeId);
        } catch (Exception e) {
            return ServiceUtil.returnError(e.getMessage());
        }
        return result;
    }

    public static Map<String, Object> verifyEbayAddSecondChanceItem(DispatchContext dctx, Map<String, ? extends Object> context) {
        Map<String, Object> result = new HashMap<String, Object>();
        boolean checkVerify = false;
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get("locale");
        String productStoreId = (String) context.get("productStoreId");
        String itemID = (String) context.get("itemId");
        ApiContext apiContext = EbayStoreHelper.getApiContext(productStoreId, locale, delegator);
        try {
            DetailLevelCodeType[] detailLevels = new DetailLevelCodeType[] {
                    DetailLevelCodeType.RETURN_ALL,
                    DetailLevelCodeType.ITEM_RETURN_ATTRIBUTES,
                    DetailLevelCodeType.ITEM_RETURN_DESCRIPTION
                };
            VerifyAddSecondChanceItemCall verify = new VerifyAddSecondChanceItemCall(apiContext);
            verify.setItemID(itemID);
            verify.setDetailLevel(detailLevels);

            verify.setDuration(SecondChanceOfferDurationCodeType.DAYS_1);
            Map<String, Object> serviceMap = new HashMap<String, Object>();
            serviceMap.put("itemId", itemID);
            serviceMap.put("productStoreId", productStoreId);
            serviceMap.put("locale", locale);
            serviceMap.put("userLogin", userLogin);
            Map<String, Object> bidderTest = UtilGenerics.checkMap(getEbayAllBidders(dctx, serviceMap));
            List<Map<String, String>> test = UtilGenerics.checkList(bidderTest.get("allBidders"));
            if (test.size() != 0) {
                verify.setRecipientBidderUserID(test.get(0).get("userId"));
            }
            result.put("checkVerify", true);
        } catch (Exception e) {
            result.put("checkVerify", checkVerify);
            result.put("errorMessage", "This item ( " + itemID + " ) can not add second chance offer.");
            result.put("responseMessage", "error");
            return result;
        }
        return result;
    }

    public static Map<String, Object> getEbayAllBidders(DispatchContext dctx, Map<String, ? extends Object> context) {
        Map<String, Object> result = new HashMap<String, Object>();
        List<Map<String, Object>> allBidders = new LinkedList<Map<String, Object>>();
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get("locale");
        String productStoreId = (String) context.get("productStoreId");
        String itemID = (String) context.get("itemId");
        ApiContext apiContext = EbayStoreHelper.getApiContext(productStoreId, locale, delegator);
        try {
            DetailLevelCodeType[] detailLevels = new DetailLevelCodeType[] {
                    DetailLevelCodeType.RETURN_ALL,
                    DetailLevelCodeType.ITEM_RETURN_ATTRIBUTES,
                    DetailLevelCodeType.ITEM_RETURN_DESCRIPTION
                };
            GetAllBiddersCall api = new GetAllBiddersCall(apiContext);
            api.setDetailLevel(detailLevels);
            api.setItemID(itemID);
            api.setCallMode(GetAllBiddersModeCodeType.VIEW_ALL);
            OfferType[] bidders = api.getAllBidders();

            for (int count = 0; count < bidders.length; count++) {
                Map<String, Object> entry = new HashMap<String, Object>();
                OfferType offer = bidders[count];
                entry.put("userId", offer.getUser().getUserID());
                entry.put("bidder", offer.getUser());
                allBidders.add(entry);
              }
            result.put("allBidders", allBidders);
        } catch (Exception e) {
            Debug.logError(e.getMessage(), module);
            result.put("allBidders", allBidders);
            return result;
        }
        return result;
    }

    public static Map<String, Object> addEbaySecondChanceOffer(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get("locale");
        String productStoreId = (String) context.get("productStoreId");
        String durationString = (String) context.get("duration");
        String itemID = (String) context.get("itemId");
        String sellerMessage = (String) context.get("sellerMessage");
        String recipientBidderUserID = (String) context.get("recipientBidderUserId");
        try {
            ApiContext apiContext = EbayStoreHelper.getApiContext(productStoreId, locale, delegator);
            AddSecondChanceItemCall api = new AddSecondChanceItemCall(apiContext);
            SecondChanceOfferDurationCodeType duration = SecondChanceOfferDurationCodeType.valueOf(durationString);
            api.setDuration(duration);
            AmountType buyItNowPrice = new AmountType();
            if (UtilValidate.isNotEmpty((String) context.get("buyItNowPrice"))) {
                buyItNowPrice.setValue(Double.parseDouble((String) context.get("buyItNowPrice")));
                buyItNowPrice.setCurrencyID(CurrencyCodeType.USD);
                api.setBuyItNowPrice(buyItNowPrice);
            }
            api.setRecipientBidderUserID(recipientBidderUserID);
            api.setItemID(itemID);
            api.setSellerMessage(sellerMessage);
            api.addSecondChanceItem();
        } catch (Exception e) {
            return ServiceUtil.returnError(e.getMessage());
        }
        return ServiceUtil.returnSuccess(UtilProperties.getMessage(resource, "EbayStoreAddSecondChanceOfferSuccessful", locale));
    }

    @SuppressWarnings("serial")
    public Map<String, Object> getMyeBaySelling(DispatchContext dctx, Map<String, ? extends Object> context) {
        Map<String, Object>result = new HashMap<String, Object>();
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get("locale");
        String productStoreId = (String) context.get("productStoreId");
        try {
            ApiContext apiContext = EbayStoreHelper.getApiContext(productStoreId, locale, delegator);
            GetMyeBaySellingCall api = new GetMyeBaySellingCall(apiContext);
            ItemListCustomizationType itemListType = new ItemListCustomizationType();
            itemListType.setInclude(Boolean.TRUE);
            itemListType.setIncludeNotes(Boolean.TRUE);
            itemListType.setSort(ItemSortTypeCodeType.ITEM_ID_DESCENDING);

            String entriesPerPage = (String) context.get("entriesPerPage");
            String pageNumber = (String) context.get("pageNumber");
            String listingType = (String) context.get("listingType");

            PaginationType page = new PaginationType();
            if (UtilValidate.isNotEmpty(entriesPerPage)) {
                page.setEntriesPerPage(Integer.valueOf(entriesPerPage));
            }
            if (UtilValidate.isNotEmpty(pageNumber)) {
                page.setPageNumber(Integer.valueOf(pageNumber));
            }
            itemListType.setPagination(page);
            if (UtilValidate.isNotEmpty(listingType)) {
                itemListType.setListingType(ListingTypeCodeType.valueOf(listingType));
            } else {
                itemListType.setListingType(ListingTypeCodeType.FIXED_PRICE_ITEM);
            }
            DetailLevelCodeType[] detailLevels = new DetailLevelCodeType[] {
                    DetailLevelCodeType.RETURN_ALL,
                    DetailLevelCodeType.ITEM_RETURN_ATTRIBUTES,
                    DetailLevelCodeType.ITEM_RETURN_DESCRIPTION
            };
            api.setDetailLevel(detailLevels);
            api.setActiveList(itemListType);
            api.setScheduledList(itemListType);
            api.setSoldList(itemListType);
            api.setUnsoldList(itemListType);
            api.getMyeBaySelling();
            ItemType[] tempActiveItems = null;
            if (api.getReturnedActiveList() != null) tempActiveItems = (api.getReturnedActiveList().getItemArray()).getItem();
            final ItemType[] activeItems = tempActiveItems;
            // Display active items in table.
            AbstractTableModel dataModel = new AbstractTableModel() {
                @Override
                public int getColumnCount() { return 0; }
                @Override
                public int getRowCount() { return activeItems == null ? 0 : activeItems.length;}
                @Override
                public Map<String, Object> getValueAt(int row, int col) {
                    ItemType item = activeItems[row];
                    return itemToColumns(item);
                }
            };
            //add To List
            List<Map<Object, Object>> activeList = getDataModelToList(dataModel);
            int activeSize = dataModel.getRowCount();
            ItemType[] tempItems = null;
            if (api.getReturnedScheduledList() != null) tempItems = (api.getReturnedScheduledList().getItemArray()).getItem();
            final ItemType[] scheItems = tempItems;
            // Display Scheduled items in table.
            dataModel = new AbstractTableModel() {
                @Override
                public int getColumnCount() { return 0; }
                @Override
                public int getRowCount() { return scheItems == null ? 0 : scheItems.length;}
                @Override
                public Map<String, Object> getValueAt(int row, int col) {
                    ItemType item = scheItems[row];
                    return schItemToColumns(item);
                }
            };
            // set data
            List<Map<Object, Object>> scheduledList = getDataModelToList(dataModel);
            int scheduledSize = dataModel.getRowCount();
            OrderTransactionType[] tempSoldItems = null;
            if (UtilValidate.isNotEmpty(api.getReturnedSoldList())) tempSoldItems = (api.getReturnedSoldList().getOrderTransactionArray()).getOrderTransaction();
            // add to list
            List<Map<String, Object>> soldList = new LinkedList<Map<String, Object>>();
            if (UtilValidate.isNotEmpty(tempSoldItems)) {
                soldList =  EbayStore.getOrderTransactions(tempSoldItems);
            }
            int soldSize = tempSoldItems == null ? 0 : tempSoldItems.length;
            ItemType[] tempUnSoldItems = null;
            if (UtilValidate.isNotEmpty(api.getReturnedUnsoldList())) tempUnSoldItems = (api.getReturnedUnsoldList().getItemArray()).getItem();
            final ItemType[] unSoldItems = tempUnSoldItems;
            // Display unsold items in table.
            dataModel = new AbstractTableModel() {
                @Override
                public int getColumnCount() { return 0; }
                @Override
                public int getRowCount() { return unSoldItems == null ? 0 : unSoldItems.length;}
                @Override
                public Map<String, Object> getValueAt(int row, int col) {
                    ItemType item = unSoldItems[row];
                    return unsoldItemToColumns(item);
                }
            };
            // add to list
            List<Map<Object, Object>> unsoldList = getDataModelToList(dataModel);
            int unsoldSize = dataModel.getRowCount();
            //list to result
            result.put("activeItems", activeList);
            result.put("soldItems", soldList);
            result.put("unsoldItems", unsoldList);
            result.put("scheduledItems", scheduledList);
            //page control to result;
            result.put("activeSize", activeSize);
            result.put("soldSize", soldSize);
            result.put("unsoldeSize", unsoldSize);
            result.put("scheduledSize", scheduledSize);
        } catch (Exception e) {
            return ServiceUtil.returnError(e.getMessage());
        }
        return result;
    }
    // set output data list (MyeBaySelling)
    public List<Map<Object, Object>> getDataModelToList(TableModel dataModel) {
        List<Map<Object, Object>> list = new LinkedList<Map<Object, Object>>();
        for (int rowIndex = 0; rowIndex < dataModel.getRowCount(); rowIndex++) {
            list.add(UtilGenerics.checkMap(dataModel.getValueAt(rowIndex, 0)));
        }
        return list;
    }
    static Map<String, Object> itemToColumns(ItemType item) {
        Map<String, Object> cols = new HashMap<String, Object>();
        cols.put("itemId", item.getItemID() != null ? item.getItemID() : "");
        cols.put("title", item.getTitle() != null ? item.getTitle() : "");

        SellingStatusType sst = item.getSellingStatus();
        double currentPrice = 0;
        int bidCount = 0;
        double reservPrice = 0;
        if (UtilValidate.isNotEmpty(sst)) {
            AmountType amt = sst.getCurrentPrice();
            currentPrice = amt != null ? (new Double(amt.getValue())) : 0;
            bidCount = sst.getBidCount() != null ? sst.getBidCount() : 0;
        }
        cols.put("buyItNowPrice", item.getBuyItNowPrice().getValue());
        cols.put("currentPrice", currentPrice);
        cols.put("bidCount", bidCount);

        java.util.Calendar startTime = item.getListingDetails() == null ? null : item.getListingDetails().getStartTime();
        cols.put("startTime", startTime != null ? eBayUtil.toAPITimeString(startTime.getTime()) : "");

        Integer quantity = item.getQuantity();
        String quantityStr = null;
        if (UtilValidate.isNotEmpty(quantity)) quantityStr = quantity.toString();
        cols.put("quantity", quantityStr);
        cols.put("listingType", item.getListingType().value());
        cols.put("viewItemURL", item.getListingDetails().getViewItemURL());
        cols.put("SKU", item.getSKU());
        if (UtilValidate.isNotEmpty(item.getReservePrice())) reservPrice = item.getReservePrice().getValue();
        cols.put("reservePrice", reservPrice);
        cols.put("hitCount", item.getHitCount() != null ? item.getHitCount() : 0);
        return cols;
    }

    static Map<String, Object> schItemToColumns(ItemType item) {
        Map<String, Object> cols = new HashMap<String, Object>();
        double reservPrice = 0;
        cols.put("itemId", item.getItemID() != null ? item.getItemID() : "");
        cols.put("title", item.getTitle() != null ? item.getTitle() : "");

        java.util.Calendar startTime = item.getListingDetails() == null ? null : item.getListingDetails().getStartTime();
        cols.put("startTime", startTime != null ? eBayUtil.toAPITimeString(startTime.getTime()) : "");
        AmountType amt = item.getStartPrice();
        cols.put("StartPrice", amt != null ? (new Double(amt.getValue()).toString()) : "");

        Integer quantity = item.getQuantity();
        String quantityStr = null;
        if (UtilValidate.isNotEmpty(quantity)) {
            quantityStr = quantity.toString();
        }
        cols.put("quantity", quantityStr);
        cols.put("listingType", item.getListingType().value());
        cols.put("SKU", item.getSKU());
        if (UtilValidate.isNotEmpty(item.getReservePrice())) reservPrice = item.getReservePrice().getValue();
        cols.put("reservePrice", reservPrice);
        cols.put("hitCount", item.getHitCount() != null ? item.getHitCount() : 0);
        return cols;
    }

    static Map<String, Object> unsoldItemToColumns(ItemType item) {
        Map<String, Object> cols = new HashMap<String, Object>();
        double reservPrice = 0;
        cols.put("itemId", item.getItemID() != null ? item.getItemID() : "");
        cols.put("title", item.getTitle() != null ? item.getTitle() : "");

        AmountType amt = item.getStartPrice();
        cols.put("price", amt != null ? (new Double(amt.getValue()).toString()) : "");

        java.util.Calendar startTime = item.getListingDetails() == null ? null : item.getListingDetails().getStartTime();
        cols.put("startTime", startTime != null ? eBayUtil.toAPITimeString(startTime.getTime()) : "");

        java.util.Calendar endTime = item.getListingDetails() == null ? null : item.getListingDetails().getEndTime();
        cols.put("endTime", endTime != null ? eBayUtil.toAPITimeString(endTime.getTime()) : "");

        Integer quantity = item.getQuantity();
        String quantityStr = null;
        if (UtilValidate.isNotEmpty(quantity)) {
            quantityStr = quantity.toString();
        }
        cols.put("quantity", quantityStr);
        cols.put("listingType", item.getListingType().value());
        cols.put("SKU", item.getSKU());
        if (UtilValidate.isNotEmpty(item.getReservePrice())) reservPrice = item.getReservePrice().getValue();
        cols.put("reservePrice", reservPrice);
        cols.put("hitCount", item.getHitCount() != null ? item.getHitCount() : 0);
        return cols;
    }

    public static List<Map<String, Object>> getOrderTransactions(OrderTransactionType[] orderTrans) {
        List<Map<String, Object>> colsList = new LinkedList<Map<String, Object>>();
        OrderTransactionType orderTran = null;
        OrderType order = null;
        TransactionType transaction= null;
        for (int rowIndex = 0; rowIndex < orderTrans.length; rowIndex++) {
            orderTran = orderTrans[rowIndex];
            order = orderTran.getOrder();
            transaction = orderTran.getTransaction();
            if (UtilValidate.isNotEmpty(order)) {
                TransactionType[] trans = order.getTransactionArray().getTransaction();
                String orderId = order.getOrderID();
                for (int rowIndex1 = 0; rowIndex1 < trans.length; rowIndex1++) {
                    Map<String, Object> transactionMap = EbayStore.getTransaction(trans[rowIndex1]);
                    transactionMap.put("orderId", orderId);
                    colsList.add(transactionMap);
                }
            } else {
                colsList.add(EbayStore.getTransaction(transaction));
            }
        }
        return colsList;
    }

    public static Map<String, Object> getTransaction(TransactionType transaction){
        Map<String, Object> cols = new HashMap<String, Object>();
        ItemType item = transaction.getItem();
        String itemId = null;
        String title = null;
        String SKU = null;
        if (UtilValidate.isNotEmpty(item)) {
            itemId = item.getItemID();
            title = item.getTitle();
            SKU = item.getSKU();
        }
        cols.put("itemId", itemId);
        cols.put("title", title);
        cols.put("SKU", SKU);
        UserType buyer = transaction.getBuyer();
        String user = null;
        if (UtilValidate.isNotEmpty(buyer)) user = buyer.getUserID();
        cols.put("buyer", user);
        cols.put("listingType", item.getListingType().value());
        Date paidTime = null;
        String checkoutStatus = null;
        String eBayPaymentStatus = null;
        String completeStatus = null;
        String buyerPaidStatus = null;
        Date shippedTime = null;
        String transactionId = null;
        double totalPrice = 0;
        double transactionPrice = 0;
        Date createdDate = null;
        String sellerPaidStatus = null;
        String orderId = null;
        double adjustmentAmount = 0;
        double amountPaid = 0;
        if (UtilValidate.isNotEmpty(transaction.getStatus())) {
            if (UtilValidate.isNotEmpty(transaction.getStatus().getCheckoutStatus())) {
                checkoutStatus = transaction.getStatus().getCheckoutStatus().value();
            }
            if (UtilValidate.isNotEmpty(transaction.getStatus().getEBayPaymentStatus())) {
                eBayPaymentStatus = transaction.getStatus().getEBayPaymentStatus().value();
            }
            if (UtilValidate.isNotEmpty(transaction.getStatus().getCompleteStatus())) {
                completeStatus = transaction.getStatus().getCompleteStatus().value();
            }
        }
        if (UtilValidate.isNotEmpty(transaction.getBuyerPaidStatus())) {
            buyerPaidStatus = transaction.getBuyerPaidStatus().value();
        }
        if (UtilValidate.isNotEmpty(transaction.getPaidTime())) {
            paidTime = transaction.getPaidTime().getTime();
        }
        if (UtilValidate.isNotEmpty(transaction.getShippedTime())) {
            shippedTime = transaction.getShippedTime().getTime();
        }
        if (UtilValidate.isNotEmpty(transaction.getTransactionID())) {
            transactionId = transaction.getTransactionID().toString();
        }
        if (UtilValidate.isNotEmpty(transaction.getTotalPrice())) {
            totalPrice = transaction.getTotalPrice().getValue();
        }
        if (UtilValidate.isNotEmpty(transaction.getTransactionPrice())) {
            transactionPrice = transaction.getTransactionPrice().getValue();
        }
        if (UtilValidate.isNotEmpty(transaction.getCreatedDate())) {
            createdDate = transaction.getCreatedDate().getTime();
        }
        if (UtilValidate.isNotEmpty(transaction.getSellerPaidStatus())) {
            sellerPaidStatus = transaction.getSellerPaidStatus().value();
        }
        if (UtilValidate.isNotEmpty(transaction.getContainingOrder())) {
            if (UtilValidate.isNotEmpty(transaction.getContainingOrder().getCheckoutStatus())) {
                checkoutStatus = transaction.getContainingOrder().getCheckoutStatus().getStatus().value();
            }
            orderId = transaction.getContainingOrder().getOrderID();
        }
        if (UtilValidate.isNotEmpty(transaction.getAdjustmentAmount())) {
            adjustmentAmount = transaction.getAdjustmentAmount().getValue();
        }
        if (UtilValidate.isNotEmpty(transaction.getAmountPaid())) {
            amountPaid = transaction.getAmountPaid().getValue();
        }
        cols.put("amountPaid", amountPaid);
        cols.put("adjustmentAmount", adjustmentAmount);
        cols.put("orderId", orderId);
        cols.put("checkoutStatus", checkoutStatus);
        cols.put("eBayPaymentStatus", eBayPaymentStatus);
        cols.put("completeStatus", completeStatus);
        cols.put("buyerPaidStatus", buyerPaidStatus);
        cols.put("paidTime", paidTime);
        cols.put("shippedTime", shippedTime);
        cols.put("quantity", transaction.getQuantityPurchased());
        cols.put("transactionId", transactionId);
        cols.put("transactionPrice", transactionPrice);
        cols.put("totalPrice", totalPrice);
        cols.put("createdDate", createdDate);
        cols.put("sellerPaidStatus", sellerPaidStatus);
        return cols;
    }

    public Map<String, Object> getEbayStoreProductItem(DispatchContext dctx, Map<String, ? extends Object> context) {
        Map<String, Object>result = new HashMap<String, Object>();
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get("locale");
        String productStoreId = (String) context.get("productStoreId");
        String itemID = (String) context.get("itemId");

        try {
            ApiContext apiContext = EbayStoreHelper.getApiContext(productStoreId, locale, delegator);
            GetItemCall api = new GetItemCall(apiContext);

            DetailLevelCodeType[] detailLevels = new DetailLevelCodeType[] {
                    DetailLevelCodeType.RETURN_ALL,
                    DetailLevelCodeType.ITEM_RETURN_ATTRIBUTES,
                    DetailLevelCodeType.ITEM_RETURN_DESCRIPTION
            };
            api.setDetailLevel(detailLevels);
            api.getItem(itemID);

            // Set item type.
            ItemType item = api.getReturnedItem();
            String title = item.getTitle();
            String description = item.getDescription();
            String listingType = item.getListingType().value();

            if (item.getPictureDetails() != null) {
                String url[] = item.getPictureDetails().getPictureURL();
                if (url.length != 0) {
                    result.put("pictureURL", url[0]);
                } else {
                    result.put("pictureURL", null);
                }
            } else {
                result.put("pictureURL", null);
            }

            result.put("title", title);
            result.put("description", description);
            AmountType amt = item.getStartPrice();
            result.put("price", amt != null ? (new Double(amt.getValue()).toString()) : "");
            result.put("currencyId", amt.getCurrencyID().toString());
            result.put("listingType", listingType);
        } catch (Exception e) {
            return ServiceUtil.returnError(e.getMessage());
        }
        return result;
    }

    public Map<String, Object> reviseEbayStoreProductItem(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get("locale");
        String productStoreId = (String) context.get("productStoreId");
        String itemID = (String) context.get("itemId");
        String title = (String) context.get("title");
        String description = (String) context.get("description");
        String price = (String) context.get("price");
        String imageFileName = (String) context.get("_imageData_fileName");
        String currencyId = (String) context.get("currencyId");

        try {
            ApiContext apiContext = EbayStoreHelper.getApiContext(productStoreId, locale, delegator);
            String sandboxEPSURL = "https://api.sandbox.ebay.com/ws/api.dll";
            apiContext.setEpsServerUrl(sandboxEPSURL);
            ReviseItemCall api = new ReviseItemCall(apiContext);

            // Set item type.
            ItemType itemToBeRevised = new ItemType();
            itemToBeRevised.setItemID(itemID);

            if (UtilValidate.isNotEmpty(title)) {
                itemToBeRevised.setTitle(title);
            }

            if (UtilValidate.isNotEmpty(description)) {
                itemToBeRevised.setDescription(description);
            }

            // Set startPrice value.
            AmountType startPrice = new AmountType();
            if (UtilValidate.isNotEmpty(price)) {
                startPrice.setValue(Double.parseDouble(price));
                startPrice.setCurrencyID(CurrencyCodeType.valueOf(currencyId));
                itemToBeRevised.setStartPrice(startPrice);
            }

            // Check upload image file.
            if (UtilValidate.isNotEmpty(imageFileName)) {

                // Upload image to ofbiz path /runtime/tmp .
                ByteBuffer byteWrap = (ByteBuffer) context.get("imageData");
                File file = new File(System.getProperty("ofbiz.home"), "runtime" + File.separator + "tmp" + File.separator + imageFileName);
                FileOutputStream fileOutputStream = new FileOutputStream(file, false);
                FileChannel wChannel = fileOutputStream.getChannel();
                wChannel.write(byteWrap);
                wChannel.close();
                fileOutputStream.close();

                // Set path file picture to api and set picture details.
                String [] pictureFiles = {System.getProperty("ofbiz.home") + File.separator + "runtime" + File.separator + "tmp" + File.separator + imageFileName};
                PictureDetailsType pictureDetails = new PictureDetailsType();
                pictureDetails.setGalleryType(GalleryTypeCodeType.GALLERY);
                pictureDetails.setPhotoDisplay(PhotoDisplayCodeType.NONE);
                pictureDetails.setPictureSource(PictureSourceCodeType.EPS);
                itemToBeRevised.setPictureDetails(pictureDetails);

                api.setItemToBeRevised(itemToBeRevised);
                api.uploadPictures(pictureFiles, pictureDetails);
            } else {
                api.setItemToBeRevised(itemToBeRevised);
            }
        } catch (Exception e) {
            return ServiceUtil.returnError(e.getMessage());
        }
        return ServiceUtil.returnSuccess(UtilProperties.getMessage(resource, "EbayStoreUpdateItemSuccessfully", locale));
    }
    public Map<String, Object> geteBayClosedItem(DispatchContext dctx, Map<String, ? extends Object> context) {
        Map <String, Object> result = new HashMap<String, Object>();
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get("locale");
        String productStoreId = (String) context.get("productStoreId");
        List <Map<String, Object>> closedItems = new LinkedList<Map<String, Object>>();
        try {
            ApiContext apiContext = EbayStoreHelper.getApiContext(productStoreId, locale, delegator);
            ItemListCustomizationType itemListType = new ItemListCustomizationType();
            itemListType.setInclude(Boolean.TRUE);
            itemListType.setSort(ItemSortTypeCodeType.ITEM_ID_DESCENDING);

            String entriesPerPage = (String) context.get("entriesPerPage");
            String pageNumber = (String) context.get("pageNumber");
            String listingType = (String) context.get("listingType");

            PaginationType page = new PaginationType();
            if (UtilValidate.isNotEmpty(entriesPerPage)) {
                page.setEntriesPerPage(Integer.valueOf(entriesPerPage));
            }
            if (UtilValidate.isNotEmpty(pageNumber)) {
                page.setPageNumber(Integer.valueOf(pageNumber));
            }
            itemListType.setPagination(page);
            if (UtilValidate.isNotEmpty(listingType)) {
                itemListType.setListingType(ListingTypeCodeType.valueOf(listingType));
            } else {
                itemListType.setListingType(ListingTypeCodeType.FIXED_PRICE_ITEM);
            }
            DetailLevelCodeType[] detailLevels = new DetailLevelCodeType[] {
                    DetailLevelCodeType.RETURN_ALL,
                    DetailLevelCodeType.ITEM_RETURN_ATTRIBUTES,
                    DetailLevelCodeType.ITEM_RETURN_DESCRIPTION
            };
            GetMyeBaySellingCall getMyeBaySelling = new GetMyeBaySellingCall(apiContext);
            getMyeBaySelling.setDetailLevel(detailLevels);
            getMyeBaySelling.setSoldList(itemListType);
            getMyeBaySelling.setUnsoldList(itemListType);
            getMyeBaySelling.getMyeBaySelling();
            ItemType[] tempUnSoldItems = null;
            if (UtilValidate.isNotEmpty(getMyeBaySelling.getReturnedUnsoldList())) tempUnSoldItems = (getMyeBaySelling.getReturnedUnsoldList().getItemArray()).getItem();

            if (UtilValidate.isNotEmpty(tempUnSoldItems)) {
                for (int i = 0; i < tempUnSoldItems.length; i++) {
                    Map <String, Object> unsoldItemMap = getClosedItem(tempUnSoldItems[i]);
                    unsoldItemMap.put("sellingStatus", "unsold");
                    closedItems.add(unsoldItemMap);
                }
            }
            OrderTransactionType[] tempSoldItems = null;
            if (UtilValidate.isNotEmpty(getMyeBaySelling.getReturnedSoldList())) tempSoldItems  = (getMyeBaySelling.getReturnedSoldList().getOrderTransactionArray()).getOrderTransaction();

            if (UtilValidate.isNotEmpty(tempSoldItems)) {
                for (int i = 0; i < tempSoldItems.length; i++) {
                    ItemType soldItem = tempSoldItems[i].getTransaction().getItem();
                    Map <String, Object> soldItemMap = getClosedItem(soldItem);
                    soldItemMap.put("sellingStatus", "sold");
                    closedItems.add(soldItemMap);
                }
            }
            result.put("closedItemList", closedItems);
        } catch (Exception e) {
            return ServiceUtil.returnError(e.getMessage());
        }
        return result;
    }
    
    public static Map<String ,Object> getClosedItem(ItemType tempItems) {
        Map <String, Object> result = new HashMap<String, Object>();
        if(UtilValidate.isNotEmpty(tempItems)) {
            double hitCount = 0;
            int quantity = 0;
            int bidCount = 0;
            double reservePrice = 0;
            double buyItNowPrice = 0;
            String listingType = null;
            String endTime = null;
            String viewItemURL= null;
            String itemId = tempItems.getItemID();
            String SKU = tempItems.getSKU();
            String title = tempItems.getTitle();
            result.put("itemId", itemId);
            result.put("SKU", SKU);
            result.put("title", title);
            if(UtilValidate.isNotEmpty(tempItems.getBuyItNowPrice())) {
                buyItNowPrice = tempItems.getBuyItNowPrice().getValue();
            }
            if(UtilValidate.isNotEmpty(tempItems.getHitCount())) {
                hitCount = tempItems.getHitCount();
            }
            if(UtilValidate.isNotEmpty(tempItems.getReservePrice())) {
                reservePrice = tempItems.getReservePrice().getValue();
            }
            if(UtilValidate.isNotEmpty(tempItems.getSellingStatus().getBidCount())) {
                bidCount= tempItems.getSellingStatus().getBidCount();
            }
            if(UtilValidate.isNotEmpty(tempItems.getListingDetails().getEndTime())) {
                Calendar endTimeItem = tempItems.getListingDetails().getEndTime();
                endTime = eBayUtil.toAPITimeString(endTimeItem.getTime());
            }
            if(UtilValidate.isNotEmpty(tempItems.getListingDetails().getViewItemURL())) {
                viewItemURL = tempItems.getListingDetails().getViewItemURL();
            }
            if(UtilValidate.isNotEmpty(tempItems.getListingType().value())) {
                listingType = tempItems.getListingType().value();
            }

            result.put("buyItNowPrice", buyItNowPrice);
            result.put("hitCount", hitCount);
            result.put("quantity", quantity);
            result.put("reservePrice", reservePrice);
            result.put("bidCount", bidCount);
            result.put("endTime", endTime);
            result.put("listingType", listingType);
            result.put("viewItemURL", viewItemURL);
        }
        return result;
    }

    public static Map<String, Object> getShippingDetail(AddressType shippingAddress, Locale locale) {
        if(UtilValidate.isEmpty(shippingAddress)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "EbayStoreRequiredShippingAddressParameter", locale));
        }
        Map<String, Object> result = new HashMap<String, Object>();
        String buyerName = null;
        String street = null;
        String street1 = null;
        String street2 = null;
        String cityName = null;
        String stateOrProvince = null;
        String county = null;
        String countryName = null;
        String phone = null;
        String postalCode = null;
        if(UtilValidate.isNotEmpty(shippingAddress.getName())) {
            buyerName = shippingAddress.getName();
        }
        if(UtilValidate.isNotEmpty(shippingAddress.getStreet())) {
            street = shippingAddress.getStreet();
        }
        if(UtilValidate.isNotEmpty(shippingAddress.getStreet1())) {
            street = shippingAddress.getStreet1();
        }
        if(UtilValidate.isNotEmpty(shippingAddress.getStreet2())) {
            street = shippingAddress.getStreet2();
        }
        if(UtilValidate.isNotEmpty(shippingAddress.getCityName())) {
            cityName = shippingAddress.getCityName();
        }
        if(UtilValidate.isNotEmpty(shippingAddress.getStateOrProvince())) {
            stateOrProvince = shippingAddress.getStateOrProvince();
        }
        if(UtilValidate.isNotEmpty(shippingAddress.getCountry())) {
            county = shippingAddress.getCountry().value();
        }
        if(UtilValidate.isNotEmpty(shippingAddress.getCountryName())) {
            countryName = shippingAddress.getCountryName();
        }
        if(UtilValidate.isNotEmpty(shippingAddress.getPhone())) {
            phone = shippingAddress.getPhone();
        }
        if(UtilValidate.isNotEmpty(shippingAddress.getPostalCode())) {
            postalCode = shippingAddress.getPostalCode();
        }
        result.put("buyerName", buyerName);
        result.put("shippingAddressStreet", street);
        result.put("shippingAddressStreet1", street1);
        result.put("shippingAddressStreet2", street2);
        result.put("shippingAddressCityName", cityName);
        result.put("shippingAddressStateOrProvince", stateOrProvince);
        result.put("shippingAddressCountry", county);
        result.put("countryName", countryName);
        result.put("shippingAddressPhone", phone);
        result.put("shippingAddressPostalCode", postalCode);
        return result;
    }
    public static boolean checkExistProduct(Delegator delegator, String productId) {
        boolean checkResult = false;
        try {
            GenericValue product = EntityQuery.use(delegator).from("Product").where("productId", productId).queryOne();
            if(product != null) {
                checkResult = true;
            }
        } catch(GenericEntityException e) {
            return false;
        }
        return checkResult;
    }
    public static Map<String, Object> getTransactionHelper(TransactionType transaction, Locale locale) {
        Map<String, Object> orderMap = new HashMap<String, Object>();
        if(UtilValidate.isNotEmpty(transaction)) {
            String orderId = null;
            String externalId = null;
            String createdDate = null;
            String userId = null;
            String itemId = null;
            String title = null;
            String SKU = null;
            int quantityPurchased = 0;
            double transactionPrice = 0;
            String buyer = null;
            String eiasTokenBuyer = null;
            String emailBuyer = null;
            String checkoutStatus = null;
            String paymentMethod = null;
            String viewItemURL = null;
            String currency = null;
            double buyItNowPrice = 0;
            double amountPaid = 0.0;
            String shippingService = null;
            double shippingServiceCost = 0.0;
            double shippingTotalAdditionalCost = 0.0;
            String shippedTime = null;
            String paidTime = null;
            double salesTaxAmount = 0.0;
            float salesTaxPercent = 0;
            Map<String, Object> itemSold = new HashMap<String, Object>();
            Map<String, Object> address = new HashMap<String, Object>();

            if(UtilValidate.isNotEmpty(transaction.getItem())) {
                ItemType item = transaction.getItem();
                itemId = item.getItemID();
                title = item.getTitle();
                SKU = item.getSKU();
                buyItNowPrice = item.getBuyItNowPrice().getValue();
                currency = item.getCurrency().value();

                if(UtilValidate.isNotEmpty(item.getListingDetails())) {
                    viewItemURL = item.getListingDetails().getViewItemURL();
                }
            }

            externalId = transaction.getTransactionID();
            if ("0".equals(externalId)) {
                // this is a Chinese Auction: ItemID is used to uniquely identify the transaction
                externalId = "EBS_"+itemId;
            } else {
                externalId = "EBS_"+externalId;
            }

            if (UtilValidate.isNotEmpty(transaction.getCreatedDate())) {
                createdDate = EbayStoreHelper.convertDate(transaction.getCreatedDate().getTime(), locale);
            }
            if (UtilValidate.isNotEmpty(transaction.getShippedTime())) {
                shippedTime = EbayStoreHelper.convertDate(transaction.getShippedTime().getTime(), locale);
            }
            if (UtilValidate.isNotEmpty(transaction.getPaidTime())) {
                paidTime = EbayStoreHelper.convertDate(transaction.getPaidTime().getTime(), locale);
            }
            if (UtilValidate.isNotEmpty(transaction.getQuantityPurchased())) {
                quantityPurchased = transaction.getQuantityPurchased();
            }
            if (UtilValidate.isNotEmpty(transaction.getTransactionPrice())) {
                transactionPrice = transaction.getTransactionPrice().getValue();
            }
            if (UtilValidate.isNotEmpty(transaction.getAmountPaid())) {
                amountPaid = transaction.getAmountPaid().getValue();
            }
            if(UtilValidate.isNotEmpty(transaction.getBuyer())) {
                UserType getBuyer = transaction.getBuyer();
                buyer = transaction.getBuyer().getUserID();
                if (UtilValidate.isNotEmpty(getBuyer.getEmail())) {
                    emailBuyer = getBuyer.getEmail();
                }
                if (UtilValidate.isNotEmpty(getBuyer.getEIASToken())) {
                    eiasTokenBuyer = getBuyer.getEIASToken();
                }
                if (UtilValidate.isNotEmpty(getBuyer.getBuyerInfo().getShippingAddress())) {
                    userId = getBuyer.getUserID();
                    AddressType shipping = getBuyer.getBuyerInfo().getShippingAddress();
                    address = getShippingDetail(shipping, locale);
                }
            }
            if(UtilValidate.isNotEmpty(transaction.getStatus())) {
                if(UtilValidate.isNotEmpty(transaction.getStatus().getPaymentMethodUsed()))
                    paymentMethod = transaction.getStatus().getPaymentMethodUsed().value();
                if(UtilValidate.isNotEmpty(transaction.getStatus().getCheckoutStatus()))
                    checkoutStatus = transaction.getStatus().getCheckoutStatus().value();
            }
            if (UtilValidate.isNotEmpty(transaction.getShippingServiceSelected())) {
                ShippingServiceOptionsType shippingServiceSelect = transaction.getShippingServiceSelected();
                if (UtilValidate.isNotEmpty(shippingServiceSelect.getShippingService())) {
                    shippingService = shippingServiceSelect.getShippingService();
                }
                if (UtilValidate.isNotEmpty(shippingServiceSelect.getShippingServiceCost())) {
                    shippingServiceCost = shippingServiceSelect.getShippingServiceCost().getValue();
                }
                if (UtilValidate.isNotEmpty(shippingServiceSelect.getShippingServiceAdditionalCost())) {
                    shippingTotalAdditionalCost  = shippingServiceSelect.getShippingServiceAdditionalCost().getValue();
                }
            }
            if (UtilValidate.isNotEmpty(transaction.getShippingDetails().getSalesTax().getSalesTaxAmount())) {
                salesTaxAmount = transaction.getShippingDetails().getSalesTax().getSalesTaxAmount().getValue();
            }
            if (UtilValidate.isNotEmpty(transaction.getShippingDetails().getSalesTax().getSalesTaxPercent())) {
                salesTaxPercent = transaction.getShippingDetails().getSalesTax().getSalesTaxPercent();
            }

            orderMap.put("externalId", externalId);
            orderMap.put("itemId", itemId);
            orderMap.put("title", title);
            orderMap.put("ebayUserIdBuyer", userId);
            orderMap.put("eiasTokenBuyer", eiasTokenBuyer);
            orderMap.put("productId", SKU);
            orderMap.put("buyItNowPrice", buyItNowPrice);
            orderMap.put("currency", currency);
            orderMap.put("viewItemURL", viewItemURL);
            orderMap.put("orderId", orderId);
            orderMap.put("createdDate", createdDate);
            orderMap.put("paidTime", paidTime);
            orderMap.put("transactionPrice", transactionPrice);
            orderMap.put("buyer", buyer);
            orderMap.put("emailBuyer", emailBuyer);
            orderMap.put("checkoutStatus", checkoutStatus.substring(8));
            orderMap.put("amountPaid", amountPaid);
            orderMap.put("quantityPurchased", quantityPurchased);
            orderMap.put("itemSold", itemSold);
            orderMap.put("paymentMethod", paymentMethod);
            orderMap.put("buyerName", address.get("buyerName").toString());
            orderMap.put("shippingAddressCityName", address.get("shippingAddressCityName").toString());
            orderMap.put("shippingAddressCountry", address.get("shippingAddressCountry").toString());
            orderMap.put("countryName", address.get("countryName").toString());
            orderMap.put("shippingAddressPhone", address.get("shippingAddressPhone").toString());
            orderMap.put("shippingAddressPostalCode", address.get("shippingAddressPostalCode").toString());
            orderMap.put("shippingAddressStateOrProvince", address.get("shippingAddressStateOrProvince").toString());
            orderMap.put("shippingAddressStreet", address.get("shippingAddressStreet").toString());
            if (UtilValidate.isNotEmpty(address.get("shippingAddressStreet1"))) {
                orderMap.put("shippingAddressStreet1", address.get("shippingAddressStreet1").toString());
            }
            if (UtilValidate.isNotEmpty(address.get("shippingAddressStreet2"))) {
                orderMap.put("shippingAddressStreet2", address.get("shippingAddressStreet2").toString());
            }
            orderMap.put("shippingService", shippingService);
            orderMap.put("shippingServiceCost", shippingServiceCost);
            orderMap.put("shippingTotalAdditionalCost", shippingTotalAdditionalCost);
            orderMap.put("shippedTime", shippedTime);
            orderMap.put("salesTaxAmount", salesTaxAmount);
            orderMap.put("salesTaxPercent", salesTaxPercent);
        }
        return orderMap;
    }
    public Map<String, Object> getEbayStoreTransaction(DispatchContext dctx, Map<String, ? extends Object> context) {
        Map<String, Object> result = new HashMap<String, Object>();
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get("locale");
        List<Map<String, Object>> transactionList = new LinkedList<Map<String, Object>>();
        List<String> orderIdList = new LinkedList<String>();
        String productStoreId = (String) context.get("productStoreId");
        try {
            Calendar fromDate = Calendar.getInstance();
            Calendar toDate = Calendar.getInstance();
            if (UtilValidate.isNotEmpty(context.get("thruDate"))) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
                dateFormat.parse(context.get("fromDate").toString());
                fromDate.setTime(dateFormat.parse(context.get("fromDate").toString()));

                SimpleDateFormat dateFormat2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
                dateFormat2.parse(context.get("thruDate").toString());
                toDate.setTime(dateFormat.parse(context.get("thruDate").toString()));
            } else {
                toDate.setTime(UtilDateTime.nowDate());
                fromDate = null;
            }

            ApiContext apiContext = EbayStoreHelper.getApiContext(productStoreId, locale, delegator);
            DetailLevelCodeType[] detailLevels = new DetailLevelCodeType[] {
                    DetailLevelCodeType.RETURN_ALL,
                    DetailLevelCodeType.ITEM_RETURN_ATTRIBUTES,
                    DetailLevelCodeType.ITEM_RETURN_DESCRIPTION
            };
            GetSellerTransactionsCall getSellerTransaction = new GetSellerTransactionsCall(apiContext);
            getSellerTransaction.setIncludeContainingOrder(Boolean.TRUE);
            getSellerTransaction.setDetailLevel(detailLevels);
            if (UtilValidate.isEmpty(fromDate)) {
                getSellerTransaction.setNumberOfDays(30);
            }
            TimeFilter modifiedTimeFilter = new TimeFilter(fromDate, toDate);
            getSellerTransaction.setModifiedTimeFilter(modifiedTimeFilter);
            TransactionType[] transactions = getSellerTransaction.getSellerTransactions();
            for (int tranCount = 0; tranCount < transactions.length; tranCount++) {
                TransactionType transaction = transactions[tranCount];
                if (UtilValidate.isNotEmpty(transaction.getContainingOrder())) {
                    String orderId = transaction.getContainingOrder().getOrderID();
                    if (!orderIdList.contains(orderId)) {
                        orderIdList.add(orderId);
                    }
                    continue;
                }
                Map<String, Object> transactionMap = EbayStore.getTransactionHelper(transaction, locale);
                transactionList.add(transactionMap);
            }
        } catch (Exception e) {
            return ServiceUtil.returnError(e.getMessage());
        }
        result.put("productStoreId", productStoreId);
        result.put("formSelect", "transaction");
        result.put("orderIdList", orderIdList);
        result.put("transactionsList", transactionList);
        return result;
    }

    public Map<String, Object> getEbayStoreOrder(DispatchContext dctx, Map<String, ? extends Object> context) {
        Map<String, Object> result = new HashMap<String, Object>();
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get("locale");
        List<Map<String, Object>> orderList = new LinkedList<Map<String,Object>>();
        String productStoreId = (String) context.get("productStoreId");
        ApiContext apiContext = EbayStoreHelper.getApiContext(productStoreId, locale, delegator);
        try {
            Calendar fromDate = Calendar.getInstance();
            Calendar toDate = Calendar.getInstance();
            if (UtilValidate.isNotEmpty(context.get("thruDate"))) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
                dateFormat.parse(context.get("fromDate").toString());
                fromDate.setTime(dateFormat.parse(context.get("fromDate").toString()));

                SimpleDateFormat dateFormat2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
                dateFormat2.parse(context.get("thruDate").toString());
                toDate.setTime(dateFormat.parse(context.get("thruDate").toString()));
            } else {
                toDate.setTime(UtilDateTime.nowDate());
                fromDate = null;
            }

            GetOrdersCall getOrder = new GetOrdersCall(apiContext);
            DetailLevelCodeType[] detailLevels = new DetailLevelCodeType[] {
                    DetailLevelCodeType.RETURN_ALL,
                    DetailLevelCodeType.ITEM_RETURN_ATTRIBUTES,
                    DetailLevelCodeType.ITEM_RETURN_DESCRIPTION
            };
            getOrder.setDetailLevel(detailLevels);
            getOrder.setCreateTimeFrom(fromDate);
            getOrder.setCreateTimeTo(toDate);
            getOrder.setOrderRole(TradingRoleCodeType.SELLER);
            getOrder.setOrderStatus(OrderStatusCodeType.COMPLETED);

            OrderType[] orders = getOrder.getOrders();
            for (int orderCount = 0; orderCount < orders.length; orderCount++) {
                OrderType order = orders[orderCount];
                Map<String, Object> orderMap = EbayStore.getOrderHelper(order, locale);
                orderList.add(orderMap);
            }

        } catch (Exception e) {
            result = ServiceUtil.returnFailure(e.getMessage());
        }
        System.out.println(orderList);
        result.put("productStoreId", productStoreId);
        result.put("formSelect", "order");
        result.put("orderList", orderList);
        return result;
    }

    private static Map<String, Object> getOrderHelper(OrderType order, Locale locale) {
        LinkedHashMap<String, Object> orderCtx = new LinkedHashMap<String, Object>();
        String externalOrderId = "EBS_"+order.getOrderID();
        double amountPaid = 0.0;
        String emailBuyer = null;
        String createdTime = null;
        String paidTime = null;
        String paymentMethod = null;
        String shippedTime = null;
        String shippingService = null;
        String ebayUserIdBuyer = null;
        String eBayPaymentStatus = null;
        String status = null;
        double shippingServiceCost = 0.0;
        double salesTaxAmount = 0.0;
        double salesTaxPercent = 0.0;
        double insuranceCost = 0.0;
        double insuranceFee = 0.0;
        String insuranceOption = null;
        boolean insuranceWanted = false;
        String salesTaxState = null;
        boolean shippingIncludedInTax = false;
        String externalTransactionId = null;
        String externalTransactionTime = null;
        double feeOrCreditAmount = 0.0;
        double paymentOrRefundAmount = 0.0;
        Map<String, Object> shippingServiceSelectedCtx = new HashMap<String, Object>();
        Map<String, Object> shippingDetailsCtx = new HashMap<String, Object>();
        Map<String, Object> shippingAddressMap = new HashMap<String, Object>();
        Map<String, Object> checkoutStatusCtx = new HashMap<String, Object>();
        Map<String, Object> externalTransactionCtx = new HashMap<String, Object>();
        if (UtilValidate.isNotEmpty(order.getTotal())) {
            amountPaid = order.getTotal().getValue();
        }
        if (UtilValidate.isNotEmpty(order.getCreatedTime())) {
            createdTime = EbayStoreHelper.convertDate(order.getCreatedTime().getTime(), locale);
        }
        if (UtilValidate.isNotEmpty(order.getPaidTime())) {
            paidTime = EbayStoreHelper.convertDate(order.getPaidTime().getTime(), locale);
        }
        if (UtilValidate.isNotEmpty(order.getShippedTime())) {
            shippedTime = EbayStoreHelper.convertDate(order.getShippedTime().getTime(), locale);
        }
        if (UtilValidate.isNotEmpty(order.getBuyerUserID())) {
            ebayUserIdBuyer = order.getBuyerUserID();
        }
        if (UtilValidate.isNotEmpty(order.getShippingAddress())) {
            AddressType shippingAddress = order.getShippingAddress();
            shippingAddressMap = EbayStore.getShippingDetail(shippingAddress, locale);
        }
        if (UtilValidate.isNotEmpty(order.getShippingServiceSelected())) {
            ShippingServiceOptionsType shippingServiceSelected = order.getShippingServiceSelected();
            if (UtilValidate.isNotEmpty(shippingServiceSelected.getShippingService())) {
                shippingService = shippingServiceSelected.getShippingService();
            }
            if (UtilValidate.isNotEmpty(shippingServiceSelected.getShippingServiceCost())) {
                shippingServiceCost = shippingServiceSelected.getShippingServiceCost().getValue();
            }
            if (UtilValidate.isNotEmpty(shippingServiceSelected.getShippingInsuranceCost())) {
                insuranceCost = shippingServiceSelected.getShippingInsuranceCost().getValue();
            }
        }
        shippingServiceSelectedCtx.put("shippingService", shippingService);
        shippingServiceSelectedCtx.put("shippingServiceCost", shippingServiceCost);
        shippingServiceSelectedCtx.put("shippingTotalAdditionalCost", insuranceCost);

        if (UtilValidate.isNotEmpty(order.getShippingDetails())) {
            ShippingDetailsType shippingDetail = order.getShippingDetails();
            if (UtilValidate.isNotEmpty(shippingDetail.getInsuranceFee())) {
                insuranceFee = shippingDetail.getInsuranceFee().getValue();
            }
            if (UtilValidate.isNotEmpty(shippingDetail.getInsuranceOption())) {
                insuranceOption = shippingDetail.getInsuranceOption().value();
            }
            if (UtilValidate.isNotEmpty(shippingDetail.isInsuranceWanted())) {
                insuranceWanted = shippingDetail.isInsuranceWanted();
            }
            if (UtilValidate.isNotEmpty(shippingDetail.getSalesTax())) {
                SalesTaxType salesTax = shippingDetail.getSalesTax();
                if (UtilValidate.isNotEmpty(salesTax.getSalesTaxAmount())) {
                    salesTaxAmount = salesTax.getSalesTaxAmount().getValue();
                }
                if (UtilValidate.isNotEmpty(salesTax.getSalesTaxPercent())) {
                    salesTaxPercent = salesTax.getSalesTaxPercent().doubleValue();
                }
                if (UtilValidate.isNotEmpty(salesTax.getSalesTaxState())) {
                    salesTaxState = salesTax.getSalesTaxState();
                }
                if (UtilValidate.isNotEmpty(salesTax.isShippingIncludedInTax())) {
                    shippingIncludedInTax = salesTax.isShippingIncludedInTax();
                }
            }
        }
        shippingDetailsCtx.put("insuranceFee", insuranceFee);
        shippingDetailsCtx.put("insuranceOption", insuranceOption);
        shippingDetailsCtx.put("insuranceWanted", insuranceWanted);
        shippingDetailsCtx.put("salesTaxAmount", salesTaxAmount);
        shippingDetailsCtx.put("salesTaxPercent", salesTaxPercent);
        shippingDetailsCtx.put("salesTaxState", salesTaxState);
        shippingDetailsCtx.put("shippingIncludedInTax", shippingIncludedInTax);

        if (UtilValidate.isNotEmpty(order.getCheckoutStatus())) {
            CheckoutStatusType checkoutStatus = order.getCheckoutStatus();
            if (UtilValidate.isNotEmpty(checkoutStatus.getEBayPaymentStatus())) {
                eBayPaymentStatus = checkoutStatus.getEBayPaymentStatus().value();
            }
            if (UtilValidate.isNotEmpty(checkoutStatus.getStatus())) {
                status = checkoutStatus.getStatus().value();
            }
            if (UtilValidate.isNotEmpty(checkoutStatus.getPaymentMethod())) {
                paymentMethod = checkoutStatus.getPaymentMethod().value();
            }
        }
        checkoutStatusCtx.put("eBayPaymentStatus", eBayPaymentStatus);
        checkoutStatusCtx.put("paymentMethodUsed", paymentMethod);
        checkoutStatusCtx.put("completeStatus", status);

        if (UtilValidate.isNotEmpty(order.getExternalTransaction())) {
            ExternalTransactionType[] externalTransactions = order.getExternalTransaction();
            for (int count = 0; count < externalTransactions.length; count++) {
                ExternalTransactionType externalTransaction = externalTransactions[count];
                if (UtilValidate.isNotEmpty(externalTransaction.getExternalTransactionID())) {
                    externalTransactionId = externalTransaction.getExternalTransactionID();
                }
                if (UtilValidate.isNotEmpty(externalTransaction.getExternalTransactionTime())) {
                    externalTransactionTime = EbayStoreHelper.convertDate(externalTransaction.getExternalTransactionTime().getTime(), locale);
                }
                if (UtilValidate.isNotEmpty(externalTransaction.getFeeOrCreditAmount())) {
                    feeOrCreditAmount = externalTransaction.getFeeOrCreditAmount().getValue();
                }
                if (UtilValidate.isNotEmpty(externalTransaction.getPaymentOrRefundAmount())) {
                    paymentOrRefundAmount = externalTransaction.getPaymentOrRefundAmount().getValue();
                }
            }
        }
        externalTransactionCtx.put("externalTransactionID", externalTransactionId);
        externalTransactionCtx.put("externalTransactionTime", externalTransactionTime);
        externalTransactionCtx.put("feeOrCreditAmount", feeOrCreditAmount);
        externalTransactionCtx.put("paymentOrRefundAmount", paymentOrRefundAmount);

        List<Map<String, Object>> orderItemList = new LinkedList<Map<String,Object>>();
        if (UtilValidate.isNotEmpty(order.getTransactionArray().getTransaction())) {
            TransactionType[] transactions = order.getTransactionArray().getTransaction();
            for (int tranCount = 0; tranCount < transactions.length; tranCount++) {
                Map<String, Object> orderItemCtx = new HashMap<String, Object>();
                TransactionType transaction = transactions[tranCount];
                int quantityPurchased = 0;
                double transactionPrice = 0.0;
                String createdDate = null;
                if (UtilValidate.isNotEmpty(transaction.getQuantityPurchased())) {
                    quantityPurchased = transaction.getQuantityPurchased();
                }
                if (UtilValidate.isNotEmpty(transaction.getTransactionPrice())) {
                    transactionPrice = transaction.getTransactionPrice().getValue();
                }
                if (UtilValidate.isNotEmpty(transaction.getCreatedDate())) {
                    createdDate = EbayStoreHelper.convertDate(transaction.getCreatedDate().getTime(), locale);
                }
                if (UtilValidate.isNotEmpty(transaction.getBuyer().getEmail())) {
                    emailBuyer = transaction.getBuyer().getEmail();
                }
                String itemId = null;
                String productId = null;
                double startPrice = 0.0;
                String title = null;
                if (UtilValidate.isNotEmpty(transaction.getItem())) {
                    ItemType item = transaction.getItem();
                    if (UtilValidate.isNotEmpty(item.getSKU())) {
                        productId = item.getSKU();
                    }
                    if (UtilValidate.isNotEmpty(item.getItemID())) {
                        itemId = item.getItemID();
                    }
                    if (UtilValidate.isNotEmpty(item.getStartPrice())) {
                        startPrice = item.getStartPrice().getValue();
                    }
                    if (UtilValidate.isNotEmpty(item.getTitle())) {
                        title = item.getTitle();
                    }
                }
                orderItemCtx.put("orderId", externalOrderId);
                orderItemCtx.put("closedDate", createdDate);
                orderItemCtx.put("goodIdentificationIdValue", itemId);
                orderItemCtx.put("quantity", quantityPurchased);
                orderItemCtx.put("startPrice", startPrice);
                orderItemCtx.put("title", title);
                orderItemCtx.put("productId", productId);
                orderItemCtx.put("transactionPrice", transactionPrice);
                orderItemList.add(orderItemCtx);
            }
        }
        orderCtx.put("externalId", externalOrderId);
        orderCtx.put("emailBuyer", emailBuyer);
        orderCtx.put("amountPaid", amountPaid);
        orderCtx.put("createdDate", createdTime);
        orderCtx.put("paidTime", paidTime);
        orderCtx.put("shippedTime", shippedTime);
        orderCtx.put("ebayUserIdBuyer", ebayUserIdBuyer);
        orderCtx.put("shippingAddressCtx", shippingAddressMap);
        orderCtx.put("shippingServiceSelectedCtx", shippingServiceSelectedCtx);
        orderCtx.put("shippingDetailsCtx", shippingDetailsCtx);
        orderCtx.put("checkoutStatusCtx", checkoutStatusCtx);
        orderCtx.put("externalTransactionCtx", externalTransactionCtx);
        orderCtx.put("orderItemList", orderItemList);
        return orderCtx;
    }
}
