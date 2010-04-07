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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javolution.util.FastList;
import javolution.util.FastMap;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.StringUtil;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.common.DataModelConstants;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.transaction.TransactionUtil;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ModelService;
import org.ofbiz.service.ServiceUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.ebay.sdk.ApiContext;
import com.ebay.sdk.ApiException;
import com.ebay.sdk.SdkException;
import com.ebay.sdk.SdkSoapException;
import com.ebay.sdk.call.*;
import com.ebay.sdk.util.eBayUtil;
import com.ebay.soap.eBLBaseComponents.AmountType;
import com.ebay.soap.eBLBaseComponents.CheckoutStatusCodeType;
import com.ebay.soap.eBLBaseComponents.CurrencyCodeType;
import com.ebay.soap.eBLBaseComponents.DeleteSellingManagerTemplateRequestType;
import com.ebay.soap.eBLBaseComponents.DeleteSellingManagerTemplateResponseType;
import com.ebay.soap.eBLBaseComponents.DisputeExplanationCodeType;
import com.ebay.soap.eBLBaseComponents.DisputeReasonCodeType;
import com.ebay.soap.eBLBaseComponents.GetAllBiddersModeCodeType;
import com.ebay.soap.eBLBaseComponents.GetSellingManagerInventoryRequestType;
import com.ebay.soap.eBLBaseComponents.GetSellingManagerInventoryResponseType;
import com.ebay.soap.eBLBaseComponents.GetStoreOptionsRequestType;
import com.ebay.soap.eBLBaseComponents.GetStoreOptionsResponseType;
import com.ebay.soap.eBLBaseComponents.GetStoreRequestType;
import com.ebay.soap.eBLBaseComponents.GetStoreResponseType;
import com.ebay.soap.eBLBaseComponents.ItemSortTypeCodeType;
import com.ebay.soap.eBLBaseComponents.ListingTypeCodeType;
import com.ebay.soap.eBLBaseComponents.MerchDisplayCodeType;
import com.ebay.soap.eBLBaseComponents.OfferType;
import com.ebay.soap.eBLBaseComponents.OrderTransactionArrayType;
import com.ebay.soap.eBLBaseComponents.OrderTransactionType;
import com.ebay.soap.eBLBaseComponents.OrderType;
import com.ebay.soap.eBLBaseComponents.PaginationType;
import com.ebay.soap.eBLBaseComponents.SecondChanceOfferDurationCodeType;
import com.ebay.soap.eBLBaseComponents.SellingManagerProductDetailsType;
import com.ebay.soap.eBLBaseComponents.SellingManagerProductInventoryStatusType;
import com.ebay.soap.eBLBaseComponents.SellingManagerProductType;
import com.ebay.soap.eBLBaseComponents.SellingManagerSearchType;
import com.ebay.soap.eBLBaseComponents.SellingManagerSearchTypeCodeType;
import com.ebay.soap.eBLBaseComponents.SellingManagerSoldListingsPropertyTypeCodeType;
import com.ebay.soap.eBLBaseComponents.SellingManagerTemplateDetailsArrayType;
import com.ebay.soap.eBLBaseComponents.SellingManagerTemplateDetailsType;
import com.ebay.soap.eBLBaseComponents.SellingStatusType;
import com.ebay.soap.eBLBaseComponents.SetStoreCategoriesRequestType;
import com.ebay.soap.eBLBaseComponents.SetStoreCategoriesResponseType;
import com.ebay.soap.eBLBaseComponents.SetStoreRequestType;
import com.ebay.soap.eBLBaseComponents.SetStoreResponseType;
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
import com.ebay.soap.eBLBaseComponents.TaskStatusCodeType;
import com.ebay.soap.eBLBaseComponents.TransactionArrayType;
import com.ebay.soap.eBLBaseComponents.TransactionType;
import com.ebay.soap.eBLBaseComponents.UserType;
import com.ebay.soap.eBLBaseComponents.VerifyAddSecondChanceItemResponseType;

import java.sql.Timestamp;

import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
 
import com.ebay.soap.eBLBaseComponents.DetailLevelCodeType;
import com.ebay.soap.eBLBaseComponents.ItemArrayType;
import com.ebay.soap.eBLBaseComponents.ItemListCustomizationType;
import com.ebay.soap.eBLBaseComponents.ItemType;
import com.ebay.soap.eBLBaseComponents.PaginatedItemArrayType;
import com.ebay.soap.eBLBaseComponents.SellingManagerSoldOrderType;
import com.ebay.soap.eBLBaseComponents.SellingManagerSoldTransactionType;

import org.ofbiz.ebay.ProductsExportToEbay;
import org.ofbiz.ebay.EbayHelper;

public class EbayStore {
    private static final String resource = "EbayStoreUiLabels";
    private static final String module = ProductsExportToEbay.class.getName();
    public static ProductsExportToEbay productExportEbay = new ProductsExportToEbay();

    private static void appendRequesterCredentials(Element elem, Document doc, String token) {
        Element requesterCredentialsElem = UtilXml.addChildElement(elem, "RequesterCredentials", doc);
        UtilXml.addChildElementValue(requesterCredentialsElem, "eBayAuthToken", token, doc);
    }

    private static Map postItem(String postItemsUrl, StringBuffer dataItems, String devID, String appID, String certID,
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
        Map result = FastMap.newInstance();
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
        Map<String, Object> result = FastMap.newInstance();
        SetStoreCategoriesRequestType req = null;
        StoreCustomCategoryArrayType categoryArrayType = null;

        List<GenericValue> catalogCategories = null;

        if (UtilValidate.isEmpty(context.get("prodCatalogId")) || UtilValidate.isEmpty(context.get("productStoreId")) || UtilValidate.isEmpty(context.get("partyId"))) {
            return ServiceUtil.returnError("Please set catalogId and  productStoreId.");
        }
        if (!EbayStoreHelper.validatePartyAndRoleType(delegator,context.get("partyId").toString())) {
            return ServiceUtil.returnError("Party ".concat(context.get("partyId").toString()).concat(" no roleTypeId EBAY_ACCOUNT for export categories to ebay store."));
        }
        try {
            SetStoreCategoriesCall  call = new SetStoreCategoriesCall(EbayStoreHelper.getApiContext((String)context.get("productStoreId"), locale, delegator));

            catalogCategories = delegator.findByAnd("ProdCatalogCategory", UtilMisc.toMap("prodCatalogId", context.get("prodCatalogId").toString(),"prodCatalogCategoryTypeId","PCCT_EBAY_ROOT"), UtilMisc.toList("sequenceNum ASC"));
            if (catalogCategories != null && catalogCategories.size() > 0) {
                List<StoreCustomCategoryType> listAdd = FastList.newInstance();
                List<StoreCustomCategoryType> listEdit = FastList.newInstance();
                //start at level 0 of categories
                for (GenericValue catalogCategory : catalogCategories) {
                    GenericValue productCategory = catalogCategory.getRelatedOne("ProductCategory");
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
                    result = excuteExportCategoryToEbayStore(call, req, StoreCategoryUpdateActionCodeType.ADD, delegator,context.get("partyId").toString(), catalogCategories);
                }
                if (listEdit.size() > 0) {
                    req = new SetStoreCategoriesRequestType();
                    categoryArrayType = new StoreCustomCategoryArrayType();
                    categoryArrayType.setCustomCategory(toStoreCustomCategoryTypeArray(listEdit));
                    req.setStoreCategories(categoryArrayType);
                    result = excuteExportCategoryToEbayStore(call, req, StoreCategoryUpdateActionCodeType.RENAME, delegator,context.get("partyId").toString(), catalogCategories);
                }

                //start at level 1 of categories
                listAdd = FastList.newInstance();
                listEdit = FastList.newInstance();
                for (GenericValue catalogCategory : catalogCategories) {
                    GenericValue productCategory = catalogCategory.getRelatedOne("ProductCategory");
                    if (productCategory != null) {
                        String ebayParentCategoryId = EbayStoreHelper.retriveEbayCategoryIdByPartyId(delegator, productCategory.getString("productCategoryId"), context.get("partyId").toString());
                        if (ebayParentCategoryId != null) {
                            List<GenericValue> productCategoryRollupList = delegator.findByAnd("ProductCategoryRollup",  UtilMisc.toMap("parentProductCategoryId", productCategory.getString("productCategoryId")), UtilMisc.toList("sequenceNum ASC"));
                            for (GenericValue productCategoryRollup : productCategoryRollupList) {
                                productCategory = delegator.findByPrimaryKey("ProductCategory", UtilMisc.toMap("productCategoryId", productCategoryRollup.getString("productCategoryId")));
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
                            result = excuteExportCategoryToEbayStore(call, req, StoreCategoryUpdateActionCodeType.ADD, delegator,context.get("partyId").toString(), catalogCategories);
                        }
                        if (listEdit.size() > 0) {
                            req = new SetStoreCategoriesRequestType();
                            categoryArrayType = new StoreCustomCategoryArrayType();
                            categoryArrayType.setCustomCategory(toStoreCustomCategoryTypeArray(listEdit));
                            req.setStoreCategories(categoryArrayType);
                            req.setDestinationParentCategoryID(new Long(ebayParentCategoryId));
                            result = excuteExportCategoryToEbayStore(call, req, StoreCategoryUpdateActionCodeType.RENAME, delegator,context.get("partyId").toString(), catalogCategories);
                        }
                    }
                }
                //start at level 2 of categories
                listAdd = FastList.newInstance();
                listEdit = FastList.newInstance();
                for (GenericValue catalogCategory : catalogCategories) {
                    GenericValue productCategory = catalogCategory.getRelatedOne("ProductCategory");
                    if (productCategory != null) {
                        List<GenericValue> productParentCategoryRollupList = delegator.findByAnd("ProductCategoryRollup",  UtilMisc.toMap("parentProductCategoryId",productCategory.getString("productCategoryId")),UtilMisc.toList("sequenceNum ASC"));
                        for (GenericValue productParentCategoryRollup : productParentCategoryRollupList) {
                            String ebayParentCategoryId = EbayStoreHelper.retriveEbayCategoryIdByPartyId(delegator,productParentCategoryRollup.getString("productCategoryId"),context.get("partyId").toString());
                            if (ebayParentCategoryId != null) {
                                List<GenericValue> productChildCategoryRollupList = delegator.findByAnd("ProductCategoryRollup",  UtilMisc.toMap("parentProductCategoryId",productParentCategoryRollup.getString("productCategoryId")),UtilMisc.toList("sequenceNum ASC"));
                                for (GenericValue productChildCategoryRollup : productChildCategoryRollupList) {
                                    productCategory = delegator.findByPrimaryKey("ProductCategory", UtilMisc.toMap("productCategoryId", productChildCategoryRollup.getString("productCategoryId")));
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
                                    result = excuteExportCategoryToEbayStore(call, req, StoreCategoryUpdateActionCodeType.ADD, delegator, context.get("partyId").toString(), catalogCategories);
                                }
                                if (listEdit.size() > 0) {
                                    req = new SetStoreCategoriesRequestType();
                                    categoryArrayType = new StoreCustomCategoryArrayType();
                                    categoryArrayType.setCustomCategory(toStoreCustomCategoryTypeArray(listEdit));
                                    req.setStoreCategories(categoryArrayType);
                                    req.setDestinationParentCategoryID(new Long(ebayParentCategoryId));
                                    result = excuteExportCategoryToEbayStore(call, req, StoreCategoryUpdateActionCodeType.RENAME, delegator, context.get("partyId").toString(), catalogCategories);
                                }
                            }
                        }
                    }
                }
            } else {
                return ServiceUtil.returnError("Not found product Category type EBAY_ROOT in catalog "+context.get("prodCatalogId"));
            }
        } catch (GenericEntityException e) {
            result = ServiceUtil.returnFailure(e.getMessage());
        }
        if (result.get("responseMessage") != null && result.get("responseMessage").equals("fail")) result = ServiceUtil.returnError(result.get("errorMessage").toString());
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

    public static Map<String, Object> excuteExportCategoryToEbayStore(SetStoreCategoriesCall  call, SetStoreCategoriesRequestType req, StoreCategoryUpdateActionCodeType actionCode,Delegator delegator, String partyId,List<GenericValue> catalogCategories) {
        Map<String, Object> result = FastMap.newInstance();
        SetStoreCategoriesResponseType resp = null;
        try {
            if (req != null && actionCode != null) {
                req.setAction(actionCode);
                resp = (SetStoreCategoriesResponseType) call.execute(req);
                if (resp != null && "SUCCESS".equals(resp.getAck().toString())) {
                    long returnTaskId = resp.getTaskID() == null? 0: resp.getTaskID().longValue();
                    TaskStatusCodeType returnedStatus = resp.getStatus();
                    StoreCustomCategoryArrayType returnedCustomCategory = resp.getCustomCategory();
                    if (actionCode.equals(StoreCategoryUpdateActionCodeType.ADD) && returnedCustomCategory != null) {
                        StoreCustomCategoryType[] returnCategoryTypeList = returnedCustomCategory.getCustomCategory();
                        for (StoreCustomCategoryType returnCategoryType : returnCategoryTypeList) {
                            List<GenericValue> productCategoryList = delegator.findByAnd("ProductCategory", UtilMisc.toMap("categoryName",returnCategoryType.getName(),"productCategoryTypeId","EBAY_CATEGORY"));
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
                    result = ServiceUtil.returnSuccess("Export categories to ebay store".concat(" success."));
                } else {
                    result = ServiceUtil.returnError("Fail to export categories to an ebay store ".concat(resp.getMessage()));
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

    public static Map buildSetStoreXml(DispatchContext dctx, Map context, StringBuffer dataStoreXml, String token, String siteID) {
        Locale locale = (Locale)context.get("locale");
        try {
            Delegator delegator = dctx.getDelegator();
            String webSiteUrl = (String)context.get("webSiteUrl");
            List selectResult = (List)context.get("selectResult");

            StringUtil.SimpleEncoder encoder = StringUtil.getEncoder("xml");

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
                    productStore = delegator.findByPrimaryKey("ProductStore",UtilMisc.toMap("productStoreId", context.get("productStoreId").toString()));
                }
                Element itemElem = UtilXml.addChildElement(storeRequestElem, "Store", storeDocument);
                UtilXml.addChildElementValue(itemElem, "Name", (String) productStore.getString("storeName"), storeDocument);
                UtilXml.addChildElementValue(itemElem, "SubscriptionLevel", "Basic", storeDocument);
                UtilXml.addChildElementValue(itemElem, "Description", (String) productStore.getString("title"), storeDocument);
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
        String result ="success";
        try {
            Document docResponse = UtilXml.readXmlDocument(msg, true);
            Element elemResponse = docResponse.getDocumentElement();
            String ack = UtilXml.childElementValue(elemResponse, "Ack", "Failure");
            if (ack != null && "Failure".equals(ack)) {
                String errorMessage = "";
                List errorList = UtilXml.childElementList(elemResponse, "Errors");
                Iterator errorElemIter = errorList.iterator();
                while (errorElemIter.hasNext()) {
                    Element errorElement = (Element) errorElemIter.next();
                    errorMessage = UtilXml.childElementValue(errorElement, "LongMessage");
                }
                result = errorMessage;
            } else {
                String productSuccessfullyExportedMsg = "Successfully exported with ID (" + productStoreId + ").";
                result = "success";
            }
        } catch (Exception e) {
            Debug.logError("Error in processing xml string" + e.getMessage(), module);
            result =  "Failure";
        }
        return result;
    }

    public static Map buildGetStoreXml(Map context, StringBuffer dataStoreXml, String token, String siteID) {
        Locale locale = (Locale)context.get("locale");
        try {
            StringUtil.SimpleEncoder encoder = StringUtil.getEncoder("xml");

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
        } catch (Exception e) {
            Debug.logError("Exception during building data to eBay: " + e.getMessage(), module);
            return ServiceUtil.returnFailure(UtilProperties.getMessage(resource, "productsExportToEbay.exceptionDuringBuildingDataItemsToEbay", locale));
        }
        return ServiceUtil.returnSuccess();
    }

    public static Map buildSetStoreCategoriesXml(DispatchContext dctx, Map context, StringBuffer dataStoreXml, String token, String siteID, String productCategoryId) {
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale)context.get("locale");
        try {
            StringUtil.SimpleEncoder encoder = StringUtil.getEncoder("xml");

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
                    category = delegator.findByPrimaryKeyCache("ProductCategory", UtilMisc.toMap("productCategoryId", productCategoryId));
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
        } catch (Exception e) {
            Debug.logError("Exception during building data to eBay: " + e.getMessage(), module);
            return ServiceUtil.returnFailure(UtilProperties.getMessage(resource, "productsExportToEbay.exceptionDuringBuildingDataItemsToEbay", locale));
        }
        return ServiceUtil.returnSuccess();
    }

    public static Map readEbayGetStoreCategoriesResponse(String msg, Locale locale) {
        Map results = null;
        List categories = FastList.newInstance();
        try {
            Document docResponse = UtilXml.readXmlDocument(msg, true);
            Element elemResponse = docResponse.getDocumentElement();
            String ack = UtilXml.childElementValue(elemResponse, "Ack", "Failure");
            if (ack != null && "Failure".equals(ack)) {
                String errorMessage = "";
                List errorList = UtilXml.childElementList(elemResponse, "Errors");
                Iterator errorElemIter = errorList.iterator();
                while (errorElemIter.hasNext()) {
                    Element errorElement = (Element) errorElemIter.next();
                    errorMessage = UtilXml.childElementValue(errorElement, "ShortMessage", "");
                }
                return ServiceUtil.returnFailure(errorMessage);
            } else {
                // retrieve Store
                List Store = UtilXml.childElementList(elemResponse, "Store");
                Iterator StoreElemIter = Store.iterator();
                while (StoreElemIter.hasNext()) {
                    Element StoreElemIterElemIterElement = (Element) StoreElemIter.next();
                    // retrieve Custom Category Array

                    List customCategories = UtilXml.childElementList(StoreElemIterElemIterElement, "CustomCategories");
                    Iterator customCategoriesElemIter = customCategories.iterator();
                    while (customCategoriesElemIter.hasNext()) {
                        Element customCategoriesElemIterElement = (Element)customCategoriesElemIter.next();

                        // retrieve CustomCategory
                        List customCategory = UtilXml.childElementList(customCategoriesElemIterElement, "CustomCategory");
                        Iterator customCategoryElemIter = customCategory.iterator();
                        while (customCategoryElemIter.hasNext()) {
                            Map categ = FastMap.newInstance();
                            Element categoryElement = (Element)customCategoryElemIter.next();
                            categ.put("CategoryID", UtilXml.childElementValue(categoryElement, "CategoryID"));
                            categ.put("CategoryName", UtilXml.childElementValue(categoryElement, "Name"));
                            categ.put("CategorySeq", UtilXml.childElementValue(categoryElement, "Order"));
                            categories.add(categ);
                        }
                    }
                }
                categories = UtilMisc.sortMaps(categories, UtilMisc.toList("CategoryName"));
                results = UtilMisc.toMap("categories", categories);
            }
        } catch (Exception e) {
            return ServiceUtil.returnFailure();
        }
        return results;
    }

    public static Map<String, Object> getEbayStoreUser(DispatchContext dctx, Map<String, ? extends Object> context) {
        Map<String, Object> result = FastMap.newInstance();
        String errorMsg = null;
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get("locale");
        String productStoreId = (String) context.get("productStoreId");
        List itemsResult = FastList.newInstance();
        try {
            List productStores = delegator.findByAnd("ProductStoreRole", UtilMisc.toMap("productStoreId", productStoreId, "roleTypeId", "EBAY_ACCOUNT")); 
            if (productStores.size() != 0) {
                String partyId = ((GenericValue) productStores.get(0)).getString("partyId");
                List userLoginStore = delegator.findByAnd("UserLogin", UtilMisc.toMap("partyId", partyId));
                if (userLoginStore.size() != 0) {
                String    userLoginId = ((GenericValue) userLoginStore.get(0)).getString("userLoginId");
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
        Map<String,Object> result = FastMap.newInstance();
        StoreType returnedStoreType = null;
        GetStoreRequestType req = new GetStoreRequestType();
        GetStoreResponseType resp =  null;

        String userLoginId = null;
        String password = null;
        if (context.get("productStoreId") != null) {
            String partyId = null;
            try {
                List<GenericValue> productStoreRoles = delegator.findByAnd("ProductStoreRole", UtilMisc.toMap("productStoreId", context.get("productStoreId").toString(),"roleTypeId","EBAY_ACCOUNT"));
                if (productStoreRoles.size() != 0) {
                    partyId=  (String)productStoreRoles.get(0).get("partyId");
                    List<GenericValue> userLogins = delegator.findByAnd("UserLogin", UtilMisc.toMap("partyId", partyId));
                    if (userLogins.size() != 0) {
                        userLoginId = (String)userLogins.get(0).get("userLoginId");
                        password = (String)userLogins.get(0).get("currentPassword");
                    }
                    
                }
            } catch (GenericEntityException e1) {
                e1.printStackTrace();
            }
            Debug.log("userLoginId is "+userLoginId+" and productStoreId is "+ context.get("productStoreId"));
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
                    Map<String,Object> ebayResp = FastMap.newInstance();
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
                            Map<String,Object> storeFontScheme = (Map<String,Object>) results.get("storeFontScheme");
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
                    StoreHeaderStyleCodeType[] storeHeaderStyleCodeList =  storeHeaderStyleCodeType.values();
                    if (storeHeaderStyleCodeList != null) {
                        List<Map<String,Object>> storeHeaderStyleList  = FastList.newInstance();
                        int i=0;
                        while (i<storeHeaderStyleCodeList.length) {
                            Map<String,Object> storeHeaderStyleMap = FastMap.newInstance();
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
                    StoreItemListLayoutCodeType[] storeItemListLayoutCodeTypeList = storeItemListLayoutCodeType.values();
                    if (storeItemListLayoutCodeTypeList != null) {
                        List<Map<String,Object>> storeItemListLayoutCodeList  = FastList.newInstance();
                        int i = 0;
                        while (i < storeItemListLayoutCodeTypeList.length) {
                            Map<String,Object> storeItemListLayoutCodeMap = FastMap.newInstance();
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
                    StoreItemListSortOrderCodeType[] storeItemListSortOrderCodeTypeList = storeItemListSortOrderCodeType.values();
                    if (storeItemListSortOrderCodeTypeList != null) {
                        List<Map<String,Object>> storeItemSortOrderCodeList  = FastList.newInstance();
                        int i = 0;
                        while (i < storeItemListSortOrderCodeTypeList.length) {
                            Map<String,Object> storeItemSortOrderCodeMap = FastMap.newInstance();
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
                    StoreCustomHeaderLayoutCodeType[] storeCustomHeaderLayoutCodeTypeList = storeCustomHeaderLayoutCodeType.values();
                    if (storeCustomHeaderLayoutCodeTypeList != null) {
                        List<Map<String,Object>> storeCustomHeaderLayoutList  = FastList.newInstance();
                        int i = 0;
                        while (i < storeCustomHeaderLayoutCodeTypeList.length) {
                            Map<String,Object> storeCustomHeaderLayoutMap = FastMap.newInstance();
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
                        StoreCustomListingHeaderDisplayCodeType[] storeCustomListingHeaderDisplayCodeTypeList = storeCustomListingHeaderDisplayCodeType.values();
                        if (storeCustomListingHeaderDisplayCodeTypeList != null) {
                            List<Map<String,Object>> storeCustomListingHeaderDisplayCodeList  = FastList.newInstance();
                            int i = 0;
                            while (i < storeCustomListingHeaderDisplayCodeTypeList.length) {
                                Map<String,Object> storeCustomListingHeaderDisplayCodeMap = FastMap.newInstance();
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
                    MerchDisplayCodeType[] merchDisplayCodeTypeList = merchDisplayCodeType.values();
                    if (merchDisplayCodeTypeList != null) {
                        List<Map<String,Object>> merchDisplayCodeList = FastList.newInstance();
                        int i = 0;
                        while (i < merchDisplayCodeTypeList.length) {
                            Map<String,Object> merchDisplayCodeMap = FastMap.newInstance();
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
                    StoreSubscriptionLevelCodeType[] storeSubscriptionlevelList = returnedStoreType.getSubscriptionLevel().values();
                    if (storeSubscriptionlevelList != null) {
                        List<Map<String,Object>> storeSubscriptionLevelCodeList = FastList.newInstance();
                        int i = 0;
                        while (i < storeSubscriptionlevelList.length) {
                            Map<String,Object> storeSubscriptionLevelCodeMap = FastMap.newInstance();
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
            Map<String,Object> inMap = FastMap.newInstance();
            if (returnStoreType != null) {
                inMap.put("productStoreId", productStoreId);
                inMap.put("storeName", returnStoreType.getName());
                inMap.put("subtitle", returnStoreType.getDescription());
                inMap.put("title", returnStoreType.getName());
                inMap.put("userLogin", context.get("userLogin"));
                dispatcher.runSync("updateProductStore", inMap);
            }
        } catch (Exception e) {
            Debug.log("error message"+e);
        }
    }

    public static Map<String,Object> retrieveThemeColorSchemeByThemeId(DispatchContext dctx, Map<String,Object> context) {
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");
        Delegator delegator = dctx.getDelegator();
        Map<String,Object> result = FastMap.newInstance();
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
                        StoreThemeType storeThemeType = (StoreThemeType) storeBasicTheme[i];
                        if (themeId.equals(storeThemeType.getThemeID().toString())) {
                            StoreColorSchemeType colorSchemeType = storeThemeType.getColorScheme();
                            if (colorSchemeType != null) {
                                if (colorSchemeId.equals(colorSchemeType.getColorSchemeID().toString())) {
                                    // get font,size and color 
                                    storeColorSchemeMap = FastMap.newInstance();
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
        Map<String,Object> result = FastMap.newInstance();
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
                    List<Map<String,Object>> logoList = FastList.newInstance();
                    while (i < returnedLogoArray.getLogoLength()) {
                        Map<String,Object> logo  = FastMap.newInstance();
                        StoreLogoType storeLogoType = (StoreLogoType) returnedLogoArray.getLogo(i);
                        logo.put("storeLogoId", storeLogoType.getLogoID());
                        logo.put("storeLogoName", storeLogoType.getName());
                        logo.put("storeLogoURL", storeLogoType.getURL());
                        logoList.add(logo);
                        i++;
                    }
                    result = ServiceUtil.returnSuccess("load store logo data success..");
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
        Map<String,Object> result = FastMap.newInstance();
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
                    List<Map<String,Object>> themeList = FastList.newInstance();
                    while (i < returnedBasicThemeArray.getThemeLength()) {
                        Map<String,Object> basictheme  = FastMap.newInstance();
                        StoreThemeType storeBasicThemeType = (StoreThemeType) returnedBasicThemeArray.getTheme(i);
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
                    result = ServiceUtil.returnSuccess("load store Basic Theme option data success..");
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
        Map<String,Object> result = FastMap.newInstance();
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
                    result = ServiceUtil.returnSuccess("load store advanced Theme option data success..");

                    returnedAdvancedThemeArray = resp.getAdvancedThemeArray();

                    int i = 0;
                    List<Map<String,Object>> themeList = FastList.newInstance();
                    while (i < returnedAdvancedThemeArray.getThemeLength()) {
                        Map<String,Object> advanceTheme = FastMap.newInstance();
                        StoreThemeType storeThemeType = returnedAdvancedThemeArray.getTheme(i);
                        advanceTheme.put("storeThemeId",storeThemeType.getThemeID());
                        advanceTheme.put("storeThemeName",storeThemeType.getName());
                        themeList.add(advanceTheme);
                        i++;
                    }
                    result.put("storeThemeList", themeList);
                    int j = 0;
                    StoreColorSchemeType[] storeColorSchemeTypes = returnedAdvancedThemeArray.getGenericColorSchemeArray().getColorScheme();
                    List<Map<String,Object>> themeColorList = FastList.newInstance();
                    while (j < storeColorSchemeTypes.length) {
                        Map<String,Object> advanceColorTheme = FastMap.newInstance();
                        StoreColorSchemeType storeColorSchemeType = (StoreColorSchemeType) storeColorSchemeTypes[j];
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
        Map<String,Object> result = FastMap.newInstance();
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

                StoreColorSchemeType storeFontColorSchemeType = null;
                Map<String,Object> advanceFontTheme = FastMap.newInstance();
                if (resp != null && "SUCCESS".equals(resp.getAck().toString())) {
                    returnedThemeArray = resp.getAdvancedThemeArray();
                    int i = 0;
                    List<Map<String,Object>> themeList = FastList.newInstance();

                    StoreColorSchemeType[] storeColorSchemeTypes = returnedThemeArray.getGenericColorSchemeArray().getColorScheme();
                    while (i < storeColorSchemeTypes.length) {

                        StoreColorSchemeType storeColorSchemeType = (StoreColorSchemeType) storeColorSchemeTypes[i];
                        StoreFontType storeFontType =  storeColorSchemeType.getFont();
                        advanceFontTheme.put("storeFontTypeNameFaceColor",storeFontType.getNameColor());
                        int j = 0;
                        StoreFontFaceCodeType[] storeFontNameFaceCodeTypes = storeFontType.getNameFace().values();
                        List<Map<String,Object>> nameFaces = FastList.newInstance();
                        while (j < storeFontNameFaceCodeTypes.length) {
                            Map<String,Object> storeFontNameFaceCodeTypeMap = FastMap.newInstance();
                            StoreFontFaceCodeType storeFontNameFaceCodeType = (StoreFontFaceCodeType) storeFontNameFaceCodeTypes[j];
                            storeFontNameFaceCodeTypeMap.put("storeFontValue", storeFontNameFaceCodeType.value());
                            storeFontNameFaceCodeTypeMap.put("storeFontName", storeFontNameFaceCodeType.name());
                            nameFaces.add(storeFontNameFaceCodeTypeMap);
                            j++;
                        }
                        advanceFontTheme.put("storeFontTypeFontFaceList",nameFaces);
                        j = 0;
                        StoreFontSizeCodeType[] storeFontSizeCodeTypes =  storeFontType.getNameSize().values();
                        List<Map<String,Object>> sizeFaces = FastList.newInstance();
                        while (j < storeFontSizeCodeTypes.length) {
                            Map<String,Object> storeFontSizeCodeTypeMap = FastMap.newInstance();
                            StoreFontSizeCodeType storeFontSizeCodeType = (StoreFontSizeCodeType) storeFontSizeCodeTypes[j];
                            storeFontSizeCodeTypeMap.put("storeFontSizeValue", storeFontSizeCodeType.value());
                            storeFontSizeCodeTypeMap.put("storeFontSizeName", storeFontSizeCodeType.name());
                            sizeFaces.add(storeFontSizeCodeTypeMap);
                            j++;
                        }
                        advanceFontTheme.put("storeFontTypeSizeFaceList", sizeFaces);

                        advanceFontTheme.put("storeFontTypeTitleColor", storeFontType.getTitleColor());
                        j = 0;
                        StoreFontFaceCodeType[] storeFontTypeTitleFaces = storeFontType.getTitleFace().values();
                        List<Map<String,Object>> titleFaces = FastList.newInstance();
                        while (j < storeFontTypeTitleFaces.length) {
                            Map<String,Object> storeFontTypeTitleFaceMap = FastMap.newInstance();
                            StoreFontFaceCodeType storeFontTypeTitleFace = (StoreFontFaceCodeType) storeFontTypeTitleFaces[j];
                            storeFontTypeTitleFaceMap.put("storeFontValue", storeFontTypeTitleFace.value());
                            storeFontTypeTitleFaceMap.put("storeFontName", storeFontTypeTitleFace.name());
                            titleFaces.add(storeFontTypeTitleFaceMap);
                            j++;
                        }
                        advanceFontTheme.put("storeFontTypeFontTitleList",titleFaces);

                        j = 0;
                        StoreFontSizeCodeType[] storeTitleSizeCodeTypes =  storeFontType.getTitleSize().values();
                        List<Map<String,Object>> titleSizes = FastList.newInstance();
                        while (j < storeTitleSizeCodeTypes.length) {
                            Map<String,Object> storeFontSizeCodeTypeMap = FastMap.newInstance();
                            StoreFontSizeCodeType storeFontSizeCodeType = (StoreFontSizeCodeType) storeTitleSizeCodeTypes[j];
                            storeFontSizeCodeTypeMap.put("storeFontSizeValue", storeFontSizeCodeType.value());
                            storeFontSizeCodeTypeMap.put("storeFontSizeName", storeFontSizeCodeType.name());
                            titleSizes.add(storeFontSizeCodeTypeMap);
                            j++;
                        }
                        advanceFontTheme.put("storeFontSizeTitleList",titleSizes);


                        advanceFontTheme.put("storeFontTypeDescColor", storeFontType.getDescColor());
                        j = 0;
                        StoreFontFaceCodeType[] storeFontTypeDescFaces = storeFontType.getDescFace().values();
                        List<Map<String,Object>> descFaces = FastList.newInstance();
                        while (j < storeFontTypeDescFaces.length) {
                            Map<String,Object> storeFontTypeDescFaceMap = FastMap.newInstance();
                            StoreFontFaceCodeType storeFontTypeDescFace = (StoreFontFaceCodeType) storeFontTypeDescFaces[j];
                            storeFontTypeDescFaceMap.put("storeFontValue", storeFontTypeDescFace.value());
                            storeFontTypeDescFaceMap.put("storeFontName", storeFontTypeDescFace.name());
                            descFaces.add(storeFontTypeDescFaceMap);
                            j++;
                        }
                        advanceFontTheme.put("storeFontTypeFontDescList",descFaces);

                        j = 0;
                        StoreFontSizeCodeType[] storeDescSizeCodeTypes =   storeFontType.getDescSize().values();
                        List<Map<String,Object>> descSizes = FastList.newInstance();
                        while (j < storeDescSizeCodeTypes.length) {
                            Map<String,Object> storeFontSizeCodeTypeMap = FastMap.newInstance();
                            StoreFontSizeCodeType storeFontSizeCodeType = (StoreFontSizeCodeType) storeDescSizeCodeTypes[j];
                            storeFontSizeCodeTypeMap.put("storeFontSizeValue", storeFontSizeCodeType.value());
                            storeFontSizeCodeTypeMap.put("storeFontSizeName", storeFontSizeCodeType.name());
                            descSizes.add(storeFontSizeCodeTypeMap);
                            j++;
                        }
                        advanceFontTheme.put("storeDescSizeList",descSizes);
                        i++;
                    }
                    result = ServiceUtil.returnSuccess("load store Basic Theme option data success..");
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
        Map<String,Object> result = FastMap.newInstance();
        Locale locale = (Locale) context.get("locale");
        Delegator delegator = dctx.getDelegator();
        SetStoreRequestType req = null;
        StoreThemeArrayType returnedThemeArray = null;
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

                StoreCustomListingHeaderType storeCustomListingHeader = new StoreCustomListingHeaderType();

                if ( storeType == null )
                      throw new SdkException("StoreType property is not set.");

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
        Map<String, Object> result = FastMap.newInstance();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get("locale");
        String productStoreId = (String) context.get("productStoreId");
        List activeItems = FastList.newInstance();
        try {
            Map<String, Object> inMap = FastMap.newInstance();
            inMap.put("productStoreId", productStoreId);
            inMap.put("userLogin", userLogin);
            Map<String, Object> resultUser = dispatcher.runSync("getEbayStoreUser", inMap);
            String userID = (String) resultUser.get("userLoginId");
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
                    Map<String, Object> entry = FastMap.newInstance();
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
        Map<String, Object> result = FastMap.newInstance();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get("locale");
        String productStoreId = (String) context.get("productStoreId");
        String filter = (String) context.get("filter");
        String itemId = (String) context.get("itemId");
        String buyerId = (String) context.get("buyerId");
        String listingType = (String) context.get("listingType");
        List soldItems = FastList.newInstance();
        try {
            Map<String, Object> inMap = FastMap.newInstance();
            inMap.put("productStoreId", productStoreId);
            inMap.put("userLogin", userLogin);
            Map<String, Object> resultUser = dispatcher.runSync("getEbayStoreUser", inMap);
            String userID = (String) resultUser.get("userLoginId");
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
                            Map<String, Object> entry = FastMap.newInstance();
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

    public static Map<String, Object> exportProductsFromEbayStore(DispatchContext dctx, Map context) {
        Map<String,Object> result = FastMap.newInstance();
        Locale locale = (Locale) context.get("locale");
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Map<String, Object> eBayConfigResult = EbayHelper.buildEbayConfig(context, delegator);
        Map response = null;
        try {
            GenericValue product = delegator.findByPrimaryKey("Product", UtilMisc.toMap("productId", context.get("productId").toString()));
            int intAtp = 1;
            String facilityId = "";
            if (UtilValidate.isNotEmpty(context.get("requireEbayInventory")) && "on".equals(context.get("requireEbayInventory").toString())) {
                GenericValue ebayProductStore = EntityUtil.getFirst(EntityUtil.filterByDate(delegator.findByAnd("EbayProductStoreInventory", UtilMisc.toMap("productStoreId", context.get("productStoreId").toString(), "productId", context.get("productId")))));
                if (UtilValidate.isNotEmpty(ebayProductStore)) {
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
                GenericValue prodCategoryMember = EntityUtil.getFirst(EntityUtil.filterByDate(delegator.findByAnd("ProductCategoryMember", UtilMisc.toMap("productCategoryId", context.get("productCategoryId"),"productId", context.get("productId")))));
                if (UtilValidate.isNotEmpty(prodCategoryMember)) {
                    GenericValue prodCategoryRole = EntityUtil.getFirst(EntityUtil.filterByDate(delegator.findByAnd("ProductCategoryRole", UtilMisc.toMap("productCategoryId", prodCategoryMember.get("productCategoryId").toString(), "partyId", userLogin.get("partyId"),"roleTypeId", "EBAY_ACCOUNT"))));
                    if (UtilValidate.isNotEmpty(prodCategoryRole)) {
                        context.put("ebayCategory", prodCategoryRole.get("comments"));
                    } else {
                        result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_FAIL);
                        result.put(ModelService.ERROR_MESSAGE, "Category not found for this product on ebay.");
                    }
                }
            } else {
                List<GenericValue> prodCategoryMember = EntityUtil.filterByDate(delegator.findByAnd("ProductCategoryMember", UtilMisc.toMap("productId", context.get("productId"))));
                Iterator prodCategoryMemberIter = prodCategoryMember.iterator();
                while (prodCategoryMemberIter.hasNext()) {
                    GenericValue prodCategory = (GenericValue) prodCategoryMemberIter.next();
                    GenericValue prodCatalogCategory = EntityUtil.getFirst(EntityUtil.filterByDate(delegator.findByAnd("ProdCatalogCategory", UtilMisc.toMap("prodCatalogId", context.get("prodCatalogId"), "productCategoryId", prodCategory.get("productCategoryId").toString()))));
                    if (UtilValidate.isNotEmpty(prodCatalogCategory)) {
                        GenericValue prodCategoryRole = EntityUtil.getFirst(EntityUtil.filterByDate(delegator.findByAnd("ProductCategoryRole", UtilMisc.toMap("productCategoryId", prodCatalogCategory.get("productCategoryId").toString(), "partyId", userLogin.get("partyId"),"roleTypeId", "EBAY_ACCOUNT"))));
                        if (UtilValidate.isNotEmpty(prodCategoryRole)) {
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
                    Map resultMap = ProductsExportToEbay.buildDataItemsXml(dctx, context, dataItemsXml, eBayConfigResult.get("token").toString(), product);
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
                    Map resultMap = ProductsExportToEbay.buildDataItemsXml(dctx, context, dataItemsXml, eBayConfigResult.get("token").toString(), product);
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

            if (UtilValidate.isNotEmpty(productExportEbay.getProductExportSuccessMessageList())) {
                if ((facilityId != "")  && (intAtp != 0)) {
                    int newAtp = intAtp - 1;
                    Map<String,Object> inMap = FastMap.newInstance();
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

            if (UtilValidate.isNotEmpty(productExportEbay.getproductExportFailureMessageList())) {
                result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_FAIL);
                result.put(ModelService.ERROR_MESSAGE_LIST, productExportEbay.getproductExportFailureMessageList());
            }
        }catch (Exception e) {
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
        Map<String, Object> result = FastMap.newInstance();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get("locale");
        String productStoreId = (String) context.get("productStoreId");
        List soldItems = FastList.newInstance();
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
        Map<String, Object> result = FastMap.newInstance();
        boolean checkVerify = false;
        LocalDispatcher dispatcher = dctx.getDispatcher();
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
            Map<String, Object> serviceMap = FastMap.newInstance();
            serviceMap.put("itemId", itemID);
            serviceMap.put("productStoreId", productStoreId);
            serviceMap.put("locale", locale);
            serviceMap.put("userLogin", userLogin);
            Map<String, Object> bidderTest = (Map) getEbayAllBidders(dctx, serviceMap);
            List<Map> test = (List) bidderTest.get("allBidders");
            if (test.size() != 0) {
                verify.setRecipientBidderUserID((String) test.get(0).get("userId"));
            }
            VerifyAddSecondChanceItemResponseType verifyResult = verify.verifyAddSecondChanceItem();
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
        Map<String, Object> result = FastMap.newInstance();
        List<Map> allBidders = FastList.newInstance();
        LocalDispatcher dispatcher = dctx.getDispatcher();
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
            GetAllBiddersCall api = new GetAllBiddersCall(apiContext);
            api.setDetailLevel(detailLevels);
            api.setItemID(itemID);
            api.setCallMode(GetAllBiddersModeCodeType.VIEW_ALL);
            OfferType[] bidders = api.getAllBidders();

            for (int count = 0; count < bidders.length; count++) {
                Map<String, Object> entry = FastMap.newInstance();
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
        Map<String, Object> result = FastMap.newInstance();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
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
        return ServiceUtil.returnSuccess("Add Second Chance Offer Successful.");
    }

    public Map<String, Object> getMyeBaySelling(DispatchContext dctx, Map<String, ? extends Object> context) {
        Map<String, Object>result = FastMap.newInstance();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get("locale");
        String productStoreId = (String) context.get("productStoreId");
        List unsoldItems = FastList.newInstance();
        try {
            Map<String, Object> inMap = FastMap.newInstance();
            inMap.put("productStoreId", productStoreId);
            inMap.put("userLogin", userLogin);
            Map<String, Object> resultUser = dispatcher.runSync("getEbayStoreUser", inMap);
            String userID = (String) resultUser.get("userLoginId");
            ApiContext apiContext = EbayStoreHelper.getApiContext(productStoreId, locale, delegator);
            GetMyeBaySellingCall api = new GetMyeBaySellingCall(apiContext);
            ItemListCustomizationType itemListType = new ItemListCustomizationType();
            itemListType.setInclude(Boolean.TRUE);

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
                public int getColumnCount() { return 0; }
                public int getRowCount() { return activeItems == null ? 0 : activeItems.length;}
                public Map<String, Object> getValueAt(int row, int col) {
                    ItemType item = activeItems[row];
                    return itemToColumns(item);
                }
            };
            //add To List
            List<Map> activeList = getDataModelToList(dataModel);
            int activeSize = dataModel.getRowCount();
            ItemType[] tempItems = null;
            if (api.getReturnedScheduledList() != null) tempItems = (api.getReturnedScheduledList().getItemArray()).getItem();
            final ItemType[] scheItems = tempItems;
            // Display Scheduled items in table.
            dataModel = new AbstractTableModel() {
                public int getColumnCount() { return 0; }
                public int getRowCount() { return scheItems == null ? 0 : scheItems.length;}
                public Map<String, Object> getValueAt(int row, int col) {
                    ItemType item = scheItems[row];
                    return schItemToColumns(item);
                }
            };
            // set data
            List<Map> scheduledList = getDataModelToList(dataModel);
            int scheduledSize = dataModel.getRowCount();
            OrderTransactionType[] tempSoldItems = null;
            if (UtilValidate.isNotEmpty(api.getReturnedSoldList())) tempSoldItems = (api.getReturnedSoldList().getOrderTransactionArray()).getOrderTransaction();
            // add to list
            List<Map> soldList =  EbayStore.getOrderTransactions(tempSoldItems);
            int soldSize = tempSoldItems == null ? 0 : tempSoldItems.length;
            ItemType[] tempUnSoldItems = null;
            if (UtilValidate.isNotEmpty(api.getReturnedUnsoldList())) tempUnSoldItems = (api.getReturnedUnsoldList().getItemArray()).getItem();
            final ItemType[] unSoldItems = tempUnSoldItems;
            // Display unsold items in table.
            dataModel = new AbstractTableModel() {
                public int getColumnCount() { return 0; }
                public int getRowCount() { return unSoldItems == null ? 0 : unSoldItems.length;}
                public Map<String, Object> getValueAt(int row, int col) {
                    ItemType item = unSoldItems[row];
                    return unsoldItemToColumns(item);
                }
            };
            // add to list
            List<Map> unsoldList = getDataModelToList(dataModel);
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
    public List<Map> getDataModelToList(TableModel dataModel) {
        List<Map> list = FastList.newInstance();
        for (int rowIndex = 0; rowIndex < dataModel.getRowCount(); rowIndex++) {
            list.add((Map<String, Object>) dataModel.getValueAt(rowIndex, 0));
        }
        return list;
    }
    static Map<String, Object> itemToColumns(ItemType item) {
        Map<String, Object> cols = FastMap.newInstance();
        int i = 0;
        cols.put("itemId", item.getItemID() != null ? item.getItemID() : "");
        cols.put("title", item.getTitle() != null ? item.getTitle() : "");
        
        SellingStatusType sst = item.getSellingStatus();
        double currentPrice = 0;
        int bidCount = 0;
        double reservPrice = 0;
        int hitCount = 0;
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
        Map<String, Object> cols = FastMap.newInstance();
        int i = 0;
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
        return cols;
    }

    static Map<String, Object> unsoldItemToColumns(ItemType item) {
        Map<String, Object> cols = FastMap.newInstance();
        int i = 0;
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
        return cols;
    }

    public static List<Map> getOrderTransactions(OrderTransactionType[] orderTrans) {
        List<Map> colsList = FastList.newInstance();
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
                    colsList.add(EbayStore.getTransaction(trans[rowIndex1]));
                }
            } else {
                colsList.add(EbayStore.getTransaction(transaction));
            }
        }
        return colsList;
    }

    public static Map<String, Object> getTransaction(TransactionType transaction){
        Map<String, Object> cols = FastMap.newInstance();
        ItemType item = transaction.getItem();
        String itemId = null; 
        String title = null;
        if (UtilValidate.isNotEmpty(item)) {
            itemId = item.getItemID();
            title = item.getTitle();
        }
        cols.put("itemId", itemId);
        cols.put("title", title);
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
        String unpaidItemStatus = null;
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
}