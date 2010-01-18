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
package org.ofbiz.ebay;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import javolution.util.FastList;
import javolution.util.FastMap;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.StringUtil;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ModelService;
import org.ofbiz.service.ServiceUtil;
import org.ofbiz.webapp.event.EventHandlerException;
import org.ofbiz.product.product.ProductContentWrapper;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.ebay.sdk.ApiAccount;
import com.ebay.sdk.ApiContext;
import com.ebay.sdk.ApiCredential;
import com.ebay.sdk.ApiException;
import com.ebay.sdk.ApiLogging;
import com.ebay.sdk.SdkException;
import com.ebay.sdk.SdkSoapException;
import com.ebay.sdk.call.*;
import com.ebay.soap.eBLBaseComponents.AbstractRequestType;
import com.ebay.soap.eBLBaseComponents.GetStoreOptionsRequestType;
import com.ebay.soap.eBLBaseComponents.GetStoreOptionsResponseType;
import com.ebay.soap.eBLBaseComponents.GetStoreRequestType;
import com.ebay.soap.eBLBaseComponents.GetStoreResponseType;
import com.ebay.soap.eBLBaseComponents.MerchDisplayCodeType;
import com.ebay.soap.eBLBaseComponents.StoreColorSchemeType;
import com.ebay.soap.eBLBaseComponents.StoreColorType;
import com.ebay.soap.eBLBaseComponents.StoreCustomHeaderLayoutCodeType;
import com.ebay.soap.eBLBaseComponents.StoreCustomListingHeaderDisplayCodeType;
import com.ebay.soap.eBLBaseComponents.StoreCustomListingHeaderLinkCodeType;
import com.ebay.soap.eBLBaseComponents.StoreCustomListingHeaderLinkType;
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

public class EbayStore {
	private static final String resource = "EbayUiLabels";
	private static final String configFileName = "ebayExport.properties";
	private static final String module = ProductsExportToEbay.class.getName();
	private static List<String> productExportSuccessMessageList = FastList.newInstance();
	private static List<String> productExportFailureMessageList = FastList.newInstance();
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

	public static Map exportCategoriesSelectedToEbayStore(DispatchContext dctx, Map context) {
		Locale locale = (Locale) context.get("locale");
		Delegator delegator = dctx.getDelegator();
		productExportSuccessMessageList.clear();
		productExportFailureMessageList.clear();
		Map<String, Object> result = FastMap.newInstance();
		Map response = null;
		Map<String, Object> eBayConfigResult = EbayHelper.buildEbayConfig(context, delegator);
		StringBuffer dataStoreXml = new StringBuffer();
		try {
			// GetStore //
			Map resultMap = buildGetStoreXml(context, dataStoreXml, eBayConfigResult.get("token").toString(), eBayConfigResult.get("siteID").toString());
			if (!ServiceUtil.isFailure(resultMap)) {
				response = postItem(eBayConfigResult.get("xmlGatewayUri").toString(), dataStoreXml, eBayConfigResult.get("devID").toString(), eBayConfigResult.get("appID").toString(), eBayConfigResult.get("certID").toString(), "GetStore", eBayConfigResult.get("compatibilityLevel").toString(), eBayConfigResult.get("siteID").toString());
				String successMessage = (String)response.get("successMessage");
				if (successMessage != null) {
					//returnResult = readEbayGetStoreCategoriesResponse(successMessage, locale);
					String isSuccess = readEbayResponse(successMessage, context.get("productStoreId").toString());
					if (isSuccess == "success"){
						GenericValue catalogStore = null;
						if(UtilValidate.isNotEmpty(context.get("productStoreId"))){
							catalogStore = EntityUtil.getFirst(delegator.findByAnd("ProductStoreCatalog",UtilMisc.toMap("productStoreId", context.get("productStoreId").toString(), "prodCatalogId", context.get("prodCatalogId").toString())));
						}
						List<GenericValue> catalogCategories = null;
						if(UtilValidate.isNotEmpty(context.get("prodCatalogId"))){
							catalogCategories = delegator.findByAnd("ProdCatalogCategory", UtilMisc.toMap("prodCatalogId", context.get("prodCatalogId").toString()));
						}
						if (catalogCategories != null){
							String isSuccess2 = "";
							Iterator categoriesListIter = catalogCategories.iterator();
							while (categoriesListIter.hasNext()) {
								GenericValue catalogCategory = (GenericValue) categoriesListIter.next();
								String prodCategoryId = catalogCategory.getString("productCategoryId").toString();

								StringBuffer dataCategoriesXml = new StringBuffer();
								Map resultCategoriesMap = buildSetStoreCategoriesXml(dctx, context, dataCategoriesXml, eBayConfigResult.get("token").toString(), eBayConfigResult.get("siteID").toString(), prodCategoryId);
								if (!ServiceUtil.isFailure(resultCategoriesMap)) {
									response = postItem(eBayConfigResult.get("xmlGatewayUri").toString(), dataCategoriesXml, eBayConfigResult.get("devID").toString(), eBayConfigResult.get("appID").toString(), eBayConfigResult.get("certID").toString(), "SetStoreCategories", eBayConfigResult.get("compatibilityLevel").toString(), eBayConfigResult.get("siteID").toString());
									String successMessage2 = (String)response.get("successMessage");
									if (successMessage2 != null) {
										isSuccess2 = readEbayResponse(successMessage2, context.get("productStoreId").toString());
									}
								}
							}
							// Get categories list from GetStore
							Map<String, Object> returnResult = FastMap.newInstance();
							StringBuffer dataGetStoreXml = new StringBuffer();
							Map resultStoreMap = buildGetStoreXml(context, dataGetStoreXml, eBayConfigResult.get("token").toString(), eBayConfigResult.get("siteID").toString());
							if (!ServiceUtil.isFailure(resultStoreMap)) {
								response = postItem(eBayConfigResult.get("xmlGatewayUri").toString(), dataGetStoreXml, eBayConfigResult.get("devID").toString(), eBayConfigResult.get("appID").toString(), eBayConfigResult.get("certID").toString(), "GetStore", eBayConfigResult.get("compatibilityLevel").toString(), eBayConfigResult.get("siteID").toString());
								String successStoreMessage = (String)response.get("successMessage");
								if (successStoreMessage != null) {
									returnResult = readEbayGetStoreCategoriesResponse(successStoreMessage, locale);
								}
							}
							if (returnResult.size() > 0){
								List catetoriesList = FastList.newInstance();
								catetoriesList = (List) returnResult.get("categories");
								Iterator categoriesIter = catetoriesList.iterator();
								while (categoriesIter.hasNext()) {
									Map category = (Map) categoriesIter.next();
									String CategoryID = category.get("CategoryID").toString();
									String CategoryName = category.get("CategoryName").toString();
									String CategorySeq = category.get("CategorySeq").toString();

									context.put("webSiteUrl", "http://demo.ofbiz.org");
									context.put("primaryCategoryId", CategoryID);
									//context.put("listingDuration", "Days_1");
									context.put("country", "US");
									context.put("location",context.get("location").toString());
									String paymentMethods = context.get("paymentMethods").toString();
									context.put(""+paymentMethods, "on");
									context.put("Currency", "USD");
									context.put("UseTaxTable", "false");
									context.put("DispatchTimeMax", "3");

									GenericValue categoryFound = EntityUtil.getFirst(delegator.findByAnd("ProductCategory", UtilMisc.toMap("categoryName", CategoryName)));

									if (UtilValidate.isNotEmpty(categoryFound)){
										GenericValue catalogCategory = EntityUtil.getFirst(delegator.findByAnd("ProdCatalogCategory", UtilMisc.toMap("prodCatalogId", context.get("prodCatalogId").toString(), "productCategoryId", categoryFound.get("productCategoryId").toString())));
										List<GenericValue> categoryMemberList = null;

										if (UtilValidate.isNotEmpty(catalogCategory)){
											categoryMemberList = delegator.findByAnd("ProductCategoryMember",UtilMisc.toMap("productCategoryId", catalogCategory.get("productCategoryId").toString()));
											if ((categoryMemberList == null) || (categoryMemberList.size() < 1)){

												GenericValue getChildCategory = EntityUtil.getFirst(delegator.findByAnd("ProductCategoryRollup",UtilMisc.toMap("parentProductCategoryId", catalogCategory.get("productCategoryId").toString())));
												if (UtilValidate.isNotEmpty(getChildCategory)){
													categoryMemberList = delegator.findByAnd("ProductCategoryMember",UtilMisc.toMap("productCategoryId", getChildCategory.get("productCategoryId").toString()));
												}
											}
										}

										if (categoryMemberList != null){
											Iterator categoryMemberListIter = categoryMemberList.iterator();
											while (categoryMemberListIter.hasNext()) {
												// build AddItem request ***
												StringBuffer dataItemsXml = new StringBuffer();
												GenericValue productCategoryMem = (GenericValue) categoryMemberListIter.next();
												GenericValue product = productCategoryMem.getRelatedOne("Product");
												GenericValue startPriceValue = EntityUtil.getFirst(EntityUtil.filterByDate(product.getRelatedByAnd("ProductPrice", UtilMisc.toMap("productPricePurposeId", "EBAY", "productPriceTypeId", "MINIMUM_PRICE"))));
												if (UtilValidate.isEmpty(startPriceValue)) {
													String startPriceMissingMsg = "Unable to find a starting price for auction of product with id (" + product.getString("productId") + "), So Ignoring the export of this product to eBay.";
													return ServiceUtil.returnFailure(startPriceMissingMsg);
													// Ignore the processing of product having no start price value
												}else{
													context.put("startPrice", startPriceValue.get("price").toString());
												}


												Map resultItem = productExportEbay.buildDataItemsXml(dctx, context, dataItemsXml, eBayConfigResult.get("token").toString(), product);
												if (!ServiceUtil.isFailure(resultItem)) {
													response = postItem(eBayConfigResult.get("xmlGatewayUri").toString(), dataItemsXml, eBayConfigResult.get("devID").toString(), eBayConfigResult.get("appID").toString(), eBayConfigResult.get("certID").toString(), "AddItem", eBayConfigResult.get("compatibilityLevel").toString(), eBayConfigResult.get("siteID").toString());
													String successMessage4 = (String)response.get("successMessage");
													if (successMessage4 != null) {
														isSuccess2 = readEbayResponse(successMessage4, context.get("productStoreId").toString());
													}
													if (UtilValidate.isNotEmpty(response)) {
														// build ReviseItem request ***
														String itemID = exportItemIDResponse((String) response.get("successMessage"));
														if (itemID != ""){
															StringBuffer dataRevItemsXml = new StringBuffer();
															Map resultRevItem = buildReviseItemXml(dctx, context, dataRevItemsXml, eBayConfigResult.get("token").toString(), eBayConfigResult.get("siteID").toString(), itemID, CategoryID);
															if (!ServiceUtil.isFailure(resultRevItem)) {
																response = postItem(eBayConfigResult.get("xmlGatewayUri").toString(), dataRevItemsXml, eBayConfigResult.get("devID").toString(), eBayConfigResult.get("appID").toString(), eBayConfigResult.get("certID").toString(), "ReviseItem", eBayConfigResult.get("compatibilityLevel").toString(), eBayConfigResult.get("siteID").toString());
																String successMessage3 = (String)response.get("successMessage");
																if (successMessage3 != null) {
																	isSuccess2 = readEbayResponse(successMessage3, context.get("productStoreId").toString());
																}
																if (ServiceUtil.isFailure(response)) {
																	return ServiceUtil.returnFailure(ServiceUtil.getErrorMessage(response));
																}
															}
															if (UtilValidate.isNotEmpty(productExportEbay.getProductExportSuccessMessageList())) {
																result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
																result.put(ModelService.SUCCESS_MESSAGE_LIST, productExportEbay.getProductExportSuccessMessageList());
															}
															if (UtilValidate.isNotEmpty(productExportEbay.getproductExportFailureMessageList())) {
																result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_FAIL);
																result.put(ModelService.ERROR_MESSAGE_LIST, productExportEbay.getproductExportFailureMessageList());
															}
														}
													}
												} else {
													return ServiceUtil.returnFailure(ServiceUtil.getErrorMessage(resultItem));
												}
											}
										}
									}
									//buildDataItemsXml(DispatchContext dctx, Map context, StringBuffer dataItemsXml, String token, GenericValue prod) {
								}
							}
						}
					}
				} else {
					ServiceUtil.returnFailure(ServiceUtil.getErrorMessage(response));
				}
			} else {
				return ServiceUtil.returnFailure(ServiceUtil.getErrorMessage(resultMap));
			}
			//result =  ServiceUtil.returnSuccess();
		} catch (Exception e) {
			Debug.logError("Exception in exportCategoriesSelectedToEbayStore : " + e, module);
			return ServiceUtil.returnFailure(UtilProperties.getMessage(resource, "productsExportToEbay.exceptionInGetEbayCategories", locale));
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
				if (UtilValidate.isNotEmpty(context.get("productStoreId").toString())){
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
//	--- GetStoreOptions request method -------------- //
	public static Map buildGetStoreOptionsXml(Map context, StringBuffer dataStoreXml, String token) {
		Locale locale = (Locale)context.get("locale");
		try {
			StringUtil.SimpleEncoder encoder = StringUtil.getEncoder("xml");

			// Get the list of products to be exported to eBay
			try {
				Document storeDocument = UtilXml.makeEmptyXmlDocument("GetStoreOptionsRequest");
				Element storeRequestElem = storeDocument.getDocumentElement();
				storeRequestElem.setAttribute("xmlns", "urn:ebay:apis:eBLBaseComponents");
				appendRequesterCredentials(storeRequestElem, storeDocument, token);
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

	// --- GetStore request method -------------- //
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

	// --- SetStoreCategoriesRequest method -------------- //
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
				if(UtilValidate.isNotEmpty(context.get("prodCatalogId"))){
					category = delegator.findByPrimaryKeyCache("ProductCategory", UtilMisc.toMap("productCategoryId", productCategoryId));
				}
				String categoryName = category.getString("productCategoryId").toString();
				if(category.getString("categoryName").toString() != null){
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

	public static Map buildReviseItemXml(DispatchContext dctx, Map context, StringBuffer dataStoreXml, String token, String siteID, String productId, String primaryCategoryId) {
		Delegator delegator = dctx.getDelegator();
		Locale locale = (Locale)context.get("locale");
		try {
			StringUtil.SimpleEncoder encoder = StringUtil.getEncoder("xml");

			// Get the list of products to be exported to eBay
			try {
				Document itemDocument = UtilXml.makeEmptyXmlDocument("ReviseItemRequest");
				Element itemRequestElem = itemDocument.getDocumentElement();
				itemRequestElem.setAttribute("xmlns", "urn:ebay:apis:eBLBaseComponents");
				appendRequesterCredentials(itemRequestElem, itemDocument, token);

				UtilXml.addChildElementValue(itemRequestElem, "Version", "643", itemDocument);
				UtilXml.addChildElementValue(itemRequestElem, "ErrorLanguage", "en_US", itemDocument);
				UtilXml.addChildElementValue(itemRequestElem, "WarningLevel", "High", itemDocument);
				Element ItemCategoriesElem = UtilXml.addChildElement(itemRequestElem, "Item", itemDocument);
				UtilXml.addChildElementValue(ItemCategoriesElem, "ItemID", productId, itemDocument);
				Element customCategoryElem = UtilXml.addChildElement(ItemCategoriesElem, "PrimaryCategory", itemDocument);
				UtilXml.addChildElementValue(customCategoryElem, "CategoryID", primaryCategoryId, itemDocument);

				dataStoreXml.append(UtilXml.writeXmlDocument(itemDocument));

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
					Element StoreElemIterElemIterElement = (Element)StoreElemIter.next();
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

	public static String exportItemIDResponse(String msg) {
		String itemID = "";
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
				return "";
			} else {
				itemID = UtilXml.childElementValue(elemResponse, "ItemID");
				String productSuccessfullyExportedMsg = "Product successfully exported with ID (" + itemID + ").";
				return itemID;
			}
		} catch (Exception e) {
			Debug.logError("Error in processing xml string" + e.getMessage(), module);
			return "";
		}
	}

	public static Map<String, Object> getEbayStoreUser(DispatchContext dctx, Map<String, ? extends Object> context){
		Map<String, Object>result = FastMap.newInstance();
		String errorMsg = null;
		LocalDispatcher dispatcher = dctx.getDispatcher();
		GenericValue userLogin = (GenericValue) context.get("userLogin");
		Delegator delegator = dctx.getDelegator();
		Locale locale = (Locale) context.get("locale");
		String productStoreId = (String) context.get("productStoreId");
		List itemsResult = FastList.newInstance();
		try{
			List productStores = delegator.findByAnd("ProductStoreRole", UtilMisc.toMap("productStoreId", productStoreId, "roleTypeId", "EBAY_ORGANIZATION")); 
			if(productStores.size() != 0){
				String partyId = ((GenericValue) productStores.get(0)).getString("partyId");
				List userLoginStore = delegator.findByAnd("UserLogin", UtilMisc.toMap("partyId", partyId));
				if(userLoginStore.size() != 0){
				String	userLoginId = ((GenericValue) userLoginStore.get(0)).getString("userLoginId");
				result.put("userLoginId", userLoginId);
				}
			}
		}catch(Exception e){
			
		}
		return result;
	}

	/*Editing the Store Settings */
	/* Get store output */
	public static Map<String,Object> getEbayStoreOutput(DispatchContext dctx, Map<String,Object> context){
		Locale locale = (Locale) context.get("locale");
		Delegator delegator = dctx.getDelegator();
		Map<String,Object> result = FastMap.newInstance();
		StoreType returnedStoreType = null;
		GetStoreRequestType req = new GetStoreRequestType();
		GetStoreResponseType resp =  null;
		
		String userLoginId = null;
		String password = null;
		if(context.get("productStoreId") != null){
			String partyId = null;
			try {
				List<GenericValue> productStoreRoles = delegator.findByAnd("ProductStoreRole", UtilMisc.toMap("productStoreId", context.get("productStoreId").toString(),"roleTypeId","EBAY_ACCOUNT"));
				if(productStoreRoles!=null){
					partyId=  (String)productStoreRoles.get(0).get("partyId");
					List<GenericValue> userLogin = delegator.findByAnd("UserLogin", UtilMisc.toMap("partyId", partyId));
					if(userLogin!=null){
						userLoginId = (String)userLogin.get(0).get("userLoginId");
						password = (String)userLogin.get(0).get("currentPassword");
					}
					
				}
				
			} catch (GenericEntityException e1) {
				e1.printStackTrace();
			}
			Debug.log("userLoginId is "+userLoginId+" and productStoreId is "+context.get("productStoreId"));
			GetStoreCall call = new GetStoreCall(EbayHelper.getApiContext((String)context.get("productStoreId"), locale, delegator));
			//call.setSite(EbayHelper.getSiteCodeType((String)context.get("productStoreId"), locale, delegator));
			call.setCategoryStructureOnly(false);
			call.setUserID(userLoginId);
			
			try {
				resp = (GetStoreResponseType)call.execute(req);
				if(resp != null && "SUCCESS".equals(resp.getAck().toString())){
					returnedStoreType  = resp.getStore();
					result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
					result.put(ModelService.SUCCESS_MESSAGE, "load store data success..");
					//result = ServiceUtil.returnSuccess("load store data success..");
					Map<String,Object> ebayResp = FastMap.newInstance();
					ebayResp.put("storeName", returnedStoreType.getName());
					ebayResp.put("storeUrl", returnedStoreType.getURL());
					ebayResp.put("storeUrlPath", returnedStoreType.getURLPath());
					ebayResp.put("storeDesc", returnedStoreType.getDescription());

					StoreLogoType logoType = returnedStoreType.getLogo();
					ebayResp.put("storeLogoId", logoType.getLogoID());
					ebayResp.put("storeLogoName", logoType.getName());
					ebayResp.put("storeLogoURL", logoType.getURL());

					StoreThemeType themeType = returnedStoreType.getTheme();
					ebayResp.put("storeThemeId",themeType.getThemeID());
					ebayResp.put("storeThemeName",themeType.getName());

					StoreColorSchemeType colorSchemeType = themeType.getColorScheme();
					ebayResp.put("storeColorSchemeId",colorSchemeType.getColorSchemeID());

					StoreColorType colorType = colorSchemeType.getColor();
					ebayResp.put("storeColorPrimary",colorType.getPrimary());
					ebayResp.put("storeColorAccent",colorType.getAccent());
					ebayResp.put("storeColorSecondary",colorType.getSecondary());

					StoreFontType fontType = colorSchemeType.getFont();
					ebayResp.put("storeDescColor",fontType.getDescColor());
					ebayResp.put("storeNameColor",fontType.getNameColor());
					ebayResp.put("storeTitleColor",fontType.getTitleColor());

					if(fontType!=null){// basic & advance theme
						String themeId = themeType.getThemeID().toString().concat("-").concat(colorSchemeType.getColorSchemeID().toString());
						context.put("themeId", themeId);
						Map<String,Object> results = retrieveThemeColorSchemeByThemeId(dctx,context);
						if(results!=null){
							Map<String,Object> storeFontScheme = (Map<String,Object>)results.get("storeFontScheme");
							if(storeFontScheme!=null){
								ebayResp.put("storeDescFontFace",storeFontScheme.get("storeFontTypeFontDescValue"));
								ebayResp.put("storeDescSizeCode", storeFontScheme.get("storeDescSizeValue"));

								ebayResp.put("storeNameFontFace",storeFontScheme.get("storeFontTypeFontFaceValue"));
								ebayResp.put("storeNameFontFaceSize",storeFontScheme.get("storeFontTypeSizeFaceValue"));

								ebayResp.put("storeTitleFontFace",storeFontScheme.get("storeFontTypeFontTitleValue"));
								ebayResp.put("storeTitleFontFaceSize",storeFontScheme.get("storeFontSizeTitleValue"));
							}
						}
					}

					StoreHeaderStyleCodeType storeHeaderStyleCodeType = returnedStoreType.getHeaderStyle();
					ebayResp.put("storeHeaderStyle", storeHeaderStyleCodeType.value());
					StoreHeaderStyleCodeType[] storeHeaderStyleCodeList =  storeHeaderStyleCodeType.values();
					if(storeHeaderStyleCodeList != null){
						List<Map<String,Object>> storeHeaderStyleList  = FastList.newInstance();
						int i=0;
						while(i<storeHeaderStyleCodeList.length){
							Map<String,Object> storeHeaderStyleMap = FastMap.newInstance();
							StoreHeaderStyleCodeType storeHeaderStyleCode = storeHeaderStyleCodeList[i];
							storeHeaderStyleMap.put("storeHeaderStyleName",storeHeaderStyleCode.name());
							storeHeaderStyleMap.put("storeHeaderStyleValue",storeHeaderStyleCode.value());
							storeHeaderStyleList.add(storeHeaderStyleMap);
							i++;
						}
						ebayResp.put("storeHeaderStyleList", storeHeaderStyleList);
					}

					ebayResp.put("storeHomePage", returnedStoreType.getHomePage().toString());

					StoreItemListLayoutCodeType storeItemListLayoutCodeType = returnedStoreType.getItemListLayout();
					ebayResp.put("storeItemLayoutSelected", storeItemListLayoutCodeType.value());
					StoreItemListLayoutCodeType[] storeItemListLayoutCodeTypeList = storeItemListLayoutCodeType.values();
					if(storeItemListLayoutCodeTypeList!=null){
						List<Map<String,Object>> storeItemListLayoutCodeList  = FastList.newInstance();
						int i=0;
						while(i<storeItemListLayoutCodeTypeList.length){
							Map<String,Object> storeItemListLayoutCodeMap = FastMap.newInstance();
							StoreItemListLayoutCodeType storeItemListLayoutCode = storeItemListLayoutCodeTypeList[i];
							storeItemListLayoutCodeMap.put("storeItemLayoutName",storeItemListLayoutCode.name());
							storeItemListLayoutCodeMap.put("storeItemLayoutValue",storeItemListLayoutCode.value());
							storeItemListLayoutCodeList.add(storeItemListLayoutCodeMap);
							i++;
						}
						ebayResp.put("storeItemLayoutList", storeItemListLayoutCodeList);
					}
					StoreItemListSortOrderCodeType storeItemListSortOrderCodeType = returnedStoreType.getItemListSortOrder();
					ebayResp.put("storeItemSortOrderSelected", storeItemListSortOrderCodeType.value());
					StoreItemListSortOrderCodeType[] storeItemListSortOrderCodeTypeList = storeItemListSortOrderCodeType.values();
					if(storeItemListSortOrderCodeTypeList!=null){
						List<Map<String,Object>> storeItemSortOrderCodeList  = FastList.newInstance();
						int i=0;
						while(i<storeItemListSortOrderCodeTypeList.length){
							Map<String,Object> storeItemSortOrderCodeMap = FastMap.newInstance();
							StoreItemListSortOrderCodeType storeItemListLayoutCode = storeItemListSortOrderCodeTypeList[i];
							storeItemSortOrderCodeMap.put("storeItemSortLayoutName",storeItemListLayoutCode.name());
							storeItemSortOrderCodeMap.put("storeItemSortLayoutValue",storeItemListLayoutCode.value());
							storeItemSortOrderCodeList.add(storeItemSortOrderCodeMap);
							i++;
						}
						ebayResp.put("storeItemSortOrderList", storeItemSortOrderCodeList);
					}

					ebayResp.put("storeCustomHeader", returnedStoreType.getCustomHeader());
					StoreCustomHeaderLayoutCodeType storeCustomHeaderLayoutCodeType = returnedStoreType.getCustomHeaderLayout();
					ebayResp.put("storeCustomHeaderLayout",storeCustomHeaderLayoutCodeType.value());
					StoreCustomHeaderLayoutCodeType[] storeCustomHeaderLayoutCodeTypeList = storeCustomHeaderLayoutCodeType.values();
					if(storeCustomHeaderLayoutCodeTypeList!=null){
						List<Map<String,Object>> storeCustomHeaderLayoutList  = FastList.newInstance();
						int i=0;
						while(i<storeCustomHeaderLayoutCodeTypeList.length){
							Map<String,Object> storeCustomHeaderLayoutMap = FastMap.newInstance();
							StoreCustomHeaderLayoutCodeType StoreCustomHeaderLayoutCode = storeCustomHeaderLayoutCodeTypeList[i];
							storeCustomHeaderLayoutMap.put("storeCustomHeaderLayoutName",StoreCustomHeaderLayoutCode.name());
							storeCustomHeaderLayoutMap.put("storeCustomHeaderLayoutValue",StoreCustomHeaderLayoutCode.value());
							storeCustomHeaderLayoutList.add(storeCustomHeaderLayoutMap);
							i++;
						}
						ebayResp.put("storeCustomHeaderLayoutList", storeCustomHeaderLayoutList);
					}

					StoreCustomListingHeaderType storeCustomListingHeaderType = returnedStoreType.getCustomListingHeader();
					if(storeCustomListingHeaderType!=null){
						StoreCustomListingHeaderDisplayCodeType storeCustomListingHeaderDisplayCodeType = storeCustomListingHeaderType.getDisplayType();
						ebayResp.put("isLogo",storeCustomListingHeaderType.isLogo());
						ebayResp.put("isSearchBox",storeCustomListingHeaderType.isSearchBox());
						ebayResp.put("isAddToFavoriteStores",storeCustomListingHeaderType.isAddToFavoriteStores());
						ebayResp.put("isSignUpForStoreNewsletter",storeCustomListingHeaderType.isSignUpForStoreNewsletter());

						ebayResp.put("storeCustomListingHeaderDisplayName",storeCustomListingHeaderDisplayCodeType.name());
						ebayResp.put("storeCustomListingHeaderDisplayValue",storeCustomListingHeaderDisplayCodeType.value());
						StoreCustomListingHeaderDisplayCodeType[] storeCustomListingHeaderDisplayCodeTypeList = storeCustomListingHeaderDisplayCodeType.values();
						if(storeCustomListingHeaderDisplayCodeTypeList != null){
							List<Map<String,Object>> storeCustomListingHeaderDisplayCodeList  = FastList.newInstance();
							int i=0;
							while(i<storeCustomListingHeaderDisplayCodeTypeList.length){
								Map<String,Object> storeCustomListingHeaderDisplayCodeMap = FastMap.newInstance();
								StoreCustomListingHeaderDisplayCodeType storeCustomListingHeaderDisplayCode = storeCustomListingHeaderDisplayCodeTypeList[i];
								storeCustomListingHeaderDisplayCodeMap.put("storeCustomHeaderLayoutName",storeCustomListingHeaderDisplayCode.name());
								storeCustomListingHeaderDisplayCodeMap.put("storeCustomHeaderLayoutValue",storeCustomListingHeaderDisplayCode.value());
								storeCustomListingHeaderDisplayCodeList.add(storeCustomListingHeaderDisplayCodeMap);
								i++;
							}
							ebayResp.put("storeCustomListingHeaderDisplayList", storeCustomListingHeaderDisplayCodeList);
						}

						/* StoreCustomListingHeaderLinkType[] storeCustomLinkToIncludeList = storeCustomListingHeaderType.getLinkToInclude();
            		   if(storeCustomLinkToIncludeList != null){
            			   List<Map<String,Object>> storeCustomListingHeaderLinkList  = FastList.newInstance();
                		   int i=0;
                		   while(i<storeCustomLinkToIncludeList.length){
                			   Map<String,Object> storeCustomListingHeaderLinkMap = FastMap.newInstance();
                			   StoreCustomListingHeaderLinkType storeCustomListingHeaderLink = storeCustomLinkToIncludeList[i];
                			   storeCustomListingHeaderLinkMap.put("storeCustomListingHeaderLinkId",storeCustomListingHeaderLink.getLinkID());
                			   storeCustomListingHeaderLinkMap.put("storeCustomListingHeaderLinkOrder",storeCustomListingHeaderLink.getOrder());

                			   StoreCustomListingHeaderLinkCodeType storeCustomListingHeaderLinkCodeType = storeCustomListingHeaderLink.getLinkType();
                			   storeCustomListingHeaderLinkMap.put("storeCustomListingHeaderLinkValue",storeCustomListingHeaderLinkCodeType.value());
                			   StoreCustomListingHeaderLinkCodeType[] storeCustomListingHeaderLinkCodeTypeList = storeCustomListingHeaderLinkCodeType.values();

                			   if(storeCustomListingHeaderLinkCodeTypeList!=null){
                				   List<Map<String,Object>> storeCustomListingHeaderLinkCodeList = FastList.newInstance();
                				   int j=0;
                				   while(j<storeCustomListingHeaderLinkCodeTypeList.length){
                					   Map<String,Object> storeCustomListingHeaderLinkCodeMap = FastMap.newInstance();
                					   StoreCustomListingHeaderLinkCodeType storeCustomListingHeaderLinkCode = storeCustomListingHeaderLinkCodeTypeList[i];
                					   storeCustomListingHeaderLinkCodeMap.put("storeCustomListingHeaderLinkName",storeCustomListingHeaderLinkCode.value());
                					   storeCustomListingHeaderLinkCodeMap.put("storeCustomListingHeaderLinkValue",storeCustomListingHeaderLinkCode.name());
                					   storeCustomListingHeaderLinkCodeList.add(storeCustomListingHeaderLinkCodeMap);
                					   j++;
                				   }
                				   storeCustomListingHeaderLinkMap.put("storeCustomListingHeaderLinkCodeList", storeCustomListingHeaderLinkCodeList);
                			   }

                			   storeCustomListingHeaderLinkList.add(storeCustomListingHeaderLinkMap);
                			   i++;
                		   }
                		   ebayResp.put("storeCustomLinktoIncludeList", storeCustomListingHeaderLinkList);
            		   }*/

					}

					//CustomListingHeader
					MerchDisplayCodeType merchDisplayCodeType = returnedStoreType.getMerchDisplay();
					ebayResp.put("storeMerchDisplay",merchDisplayCodeType.value());
					MerchDisplayCodeType[] merchDisplayCodeTypeList = merchDisplayCodeType.values();
					if(merchDisplayCodeTypeList!=null){
						List<Map<String,Object>> merchDisplayCodeList = FastList.newInstance();
						int i=0;
						while(i<merchDisplayCodeTypeList.length){
							Map<String,Object> merchDisplayCodeMap = FastMap.newInstance();
							MerchDisplayCodeType merchDisplayCode = merchDisplayCodeTypeList[i];
							merchDisplayCodeMap.put("merchDisplayCodeName",merchDisplayCode.name());
							merchDisplayCodeMap.put("merchDisplayCodeValue",merchDisplayCode.value());
							merchDisplayCodeList.add(merchDisplayCodeMap);
							i++;
						}
						ebayResp.put("storeMerchDisplayList",merchDisplayCodeList);
					}

					Calendar calendar = returnedStoreType.getLastOpenedTime();
					ebayResp.put("storeLastOpenedTime", calendar.getTime().toString());
					ebayResp.put("storeSubscriptionLevel",returnedStoreType.getSubscriptionLevel().value());
					StoreSubscriptionLevelCodeType[] storeSubscriptionlevelList = returnedStoreType.getSubscriptionLevel().values();
					if(storeSubscriptionlevelList!=null){
						List<Map<String,Object>> storeSubscriptionLevelCodeList = FastList.newInstance();
						int i=0;
						while(i<storeSubscriptionlevelList.length){
							Map<String,Object> storeSubscriptionLevelCodeMap = FastMap.newInstance();
							StoreSubscriptionLevelCodeType storeSubscriptionLevelCode= storeSubscriptionlevelList[i];
							storeSubscriptionLevelCodeMap.put("storeSubscriptionLevelCodeName", storeSubscriptionLevelCode.name());
							storeSubscriptionLevelCodeMap.put("storeSubscriptionLevelCodeValue", storeSubscriptionLevelCode.value());
							storeSubscriptionLevelCodeList.add(storeSubscriptionLevelCodeMap);
							i++;
						}
						ebayResp.put("storeSubscriptionLevelList", storeSubscriptionLevelCodeList);
					}
					/*ebayResp.put("storeDesc", returnedStoreType.getDescription());
            	   ebayResp.put("storeDesc", returnedStoreType.getDescription());*/

					result.put("ebayStore", ebayResp);
				}else{
					result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_ERROR);
					result.put(ModelService.ERROR_MESSAGE,resp.getAck().toString());
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

	public static Map<String,Object>  retrieveThemeColorSchemeByThemeId(DispatchContext dctx, Map<String,Object> context){

		Locale locale = (Locale) context.get("locale");
		Delegator delegator = dctx.getDelegator();
		Map<String,Object> result = FastMap.newInstance();
		GetStoreOptionsRequestType req = null;
		GetStoreOptionsResponseType resp  = null;
		StoreThemeArrayType returnedBasicThemeArray = null;
		StoreThemeArrayType returnedAdvanceThemeArray = null;

		try {
			if(context.get("productStoreId") != null){
				String themeId = (String)context.get("themeId");

				GetStoreOptionsCall  call = new GetStoreOptionsCall(EbayHelper.getApiContext((String)context.get("productStoreId"), locale, delegator));
				req = new GetStoreOptionsRequestType();

				resp = (GetStoreOptionsResponseType) call.execute(req);
				if(resp != null && "SUCCESS".equals(resp.getAck().toString())){

					returnedBasicThemeArray = resp.getBasicThemeArray();
					StoreThemeType[] storeBasicTheme = returnedBasicThemeArray.getTheme();

					int i=0;
					String colorSchemeId = themeId.substring(themeId.indexOf("-")+1);
					themeId = themeId.substring(0,themeId.indexOf("-"));

					Map<String,Object> storeColorSchemeMap = null;
					boolean flag = false;
					while(i<storeBasicTheme.length){

						StoreThemeType storeThemeType = (StoreThemeType)storeBasicTheme[i];
						if(themeId.equals(storeThemeType.getThemeID().toString())){
							StoreColorSchemeType colorSchemeType = storeThemeType.getColorScheme();
							if(colorSchemeType!=null){
								if(colorSchemeId.equals(colorSchemeType.getColorSchemeID().toString())){
									// get font,size and color 
									storeColorSchemeMap = FastMap.newInstance();
									StoreFontType storeFontType = colorSchemeType.getFont();
									storeColorSchemeMap.put("storeFontTypeFontFaceValue",storeFontType.getNameFace().value());
									storeColorSchemeMap.put("storeFontTypeSizeFaceValue",storeFontType.getNameSize().value());

									storeColorSchemeMap.put("storeFontTypeFontTitleValue",storeFontType.getTitleFace().value());
									storeColorSchemeMap.put("storeFontSizeTitleValue",storeFontType.getTitleSize().value());

									storeColorSchemeMap.put("storeFontTypeFontDescValue",storeFontType.getDescFace().value());
									storeColorSchemeMap.put("storeDescSizeValue",storeFontType.getDescSize().value());
									flag=true;
									break;
								}
							}
						}
						i++;
					}
					result.put("storeFontScheme",storeColorSchemeMap);
				}
			}
		}catch (ApiException e) {
			e.printStackTrace();
		} catch (SdkSoapException e) {
			e.printStackTrace();
		} catch (SdkException e) {
			e.printStackTrace();
		}

		return result;
	}

	public static Map<String,Object>  retrievePredesignedLogoOption(DispatchContext dctx, Map<String,Object> context){
		Map<String,Object> result = FastMap.newInstance();
		Locale locale = (Locale) context.get("locale");
		Delegator delegator = dctx.getDelegator();
		GetStoreOptionsRequestType req = null;
		StoreLogoArrayType returnedLogoArray = null;
		GetStoreOptionsResponseType resp  = null;
		try {
			if(context.get("productStoreId") != null){
				GetStoreOptionsCall  call = new GetStoreOptionsCall(EbayHelper.getApiContext((String)context.get("productStoreId"), locale, delegator));
				req = new GetStoreOptionsRequestType();

				resp = (GetStoreOptionsResponseType) call.execute(req);

				if(resp != null && "SUCCESS".equals(resp.getAck().toString())){
					returnedLogoArray = resp.getLogoArray();

					int i=0;
					List<Map<String,Object>> logoList = FastList.newInstance();
					while(i<returnedLogoArray.getLogoLength()){
						Map<String,Object> logo  = FastMap.newInstance();
						StoreLogoType storeLogoType = (StoreLogoType)returnedLogoArray.getLogo(i);
						logo.put("storeLogoId", storeLogoType.getLogoID());
						logo.put("storeLogoName", storeLogoType.getName());
						logo.put("storeLogoURL", storeLogoType.getURL());
						logoList.add(logo);
						i++;
					}
					result = ServiceUtil.returnSuccess("load store logo data success..");
					result.put("storeLogoOptList", logoList);
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
	public static Map<String,Object>  retrieveBasicThemeArray(DispatchContext dctx, Map<String,Object> context){
		Map<String,Object> result = FastMap.newInstance();
		Locale locale = (Locale) context.get("locale");
		Delegator delegator = dctx.getDelegator();
		GetStoreOptionsRequestType req = null;
		StoreThemeArrayType returnedBasicThemeArray = null;
		GetStoreOptionsResponseType resp  = null;
		try {
			if(context.get("productStoreId") != null){
				GetStoreOptionsCall  call = new GetStoreOptionsCall(EbayHelper.getApiContext((String)context.get("productStoreId"), locale, delegator));
				req = new GetStoreOptionsRequestType();

				resp = (GetStoreOptionsResponseType) call.execute(req);

				StoreColorSchemeType storeFontColorSchemeType = null;
				if(resp != null && "SUCCESS".equals(resp.getAck().toString())){
					returnedBasicThemeArray = resp.getBasicThemeArray();
					int i=0;
					List<Map<String,Object>> themeList = FastList.newInstance();
					while(i<returnedBasicThemeArray.getThemeLength()){
						Map<String,Object> basictheme  = FastMap.newInstance();
						StoreThemeType storeBasicThemeType = (StoreThemeType)returnedBasicThemeArray.getTheme(i);
						basictheme.put("storeThemeId", storeBasicThemeType.getThemeID());
						basictheme.put("storeThemeName", storeBasicThemeType.getName());

						StoreColorSchemeType storeColorSchemeType = storeBasicThemeType.getColorScheme();
						basictheme.put("storeColorSchemeId",storeColorSchemeType.getColorSchemeID());
						basictheme.put("storeColorSchemeName",storeColorSchemeType.getName());

						if(storeFontColorSchemeType == null){
							storeFontColorSchemeType = storeBasicThemeType.getColorScheme();
						}

						themeList.add(basictheme);
						i++;
					}
					result = ServiceUtil.returnSuccess("load store Basic Theme option data success..");
					result.put("storeThemeList", themeList);
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

	public static Map<String,Object>  retrieveAdvancedThemeArray(DispatchContext dctx, Map<String,Object> context){
		Map<String,Object> result = FastMap.newInstance();
		Locale locale = (Locale) context.get("locale");
		Delegator delegator = dctx.getDelegator();
		GetStoreOptionsRequestType req = null;
		StoreThemeArrayType returnedAdvancedThemeArray = null;
		GetStoreOptionsResponseType resp  = null;
		try {
			if(context.get("productStoreId") != null){
				GetStoreOptionsCall  call = new GetStoreOptionsCall(EbayHelper.getApiContext((String)context.get("productStoreId"), locale, delegator));
				req = new GetStoreOptionsRequestType();

				resp = (GetStoreOptionsResponseType) call.execute(req);

				if(resp != null && "SUCCESS".equals(resp.getAck().toString())){
					result = ServiceUtil.returnSuccess("load store advanced Theme option data success..");

					returnedAdvancedThemeArray = resp.getAdvancedThemeArray();

					int i=0;
					List<Map<String,Object>> themeList = FastList.newInstance();
					while(i<returnedAdvancedThemeArray.getThemeLength()){
						Map<String,Object> advanceTheme = FastMap.newInstance();
						StoreThemeType storeThemeType = returnedAdvancedThemeArray.getTheme(i);
						advanceTheme.put("storeThemeId",storeThemeType.getThemeID());
						advanceTheme.put("storeThemeName",storeThemeType.getName());
						themeList.add(advanceTheme);
						i++;
					}
					result.put("storeThemeList", themeList);
					int j=0;
					StoreColorSchemeType[] storeColorSchemeTypes = returnedAdvancedThemeArray.getGenericColorSchemeArray().getColorScheme();
					List<Map<String,Object>> themeColorList = FastList.newInstance();
					while(j<storeColorSchemeTypes.length){
						Map<String,Object> advanceColorTheme = FastMap.newInstance();
						StoreColorSchemeType storeColorSchemeType = (StoreColorSchemeType)storeColorSchemeTypes[j];
						advanceColorTheme.put("storeColorSchemeId", storeColorSchemeType.getColorSchemeID());
						advanceColorTheme.put("storeColorName", storeColorSchemeType.getName());
						themeColorList.add(advanceColorTheme);
						j++;
					}

					result.put("storeAdvancedThemeColorOptList", themeColorList);
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

	public static Map<String,Object>  retrieveStoreFontTheme(DispatchContext dctx, Map<String,Object> context){
		Map<String,Object> result = FastMap.newInstance();
		Locale locale = (Locale) context.get("locale");
		Delegator delegator = dctx.getDelegator();
		GetStoreOptionsRequestType req = null;
		StoreThemeArrayType returnedThemeArray = null;
		GetStoreOptionsResponseType resp  = null;
		try {
			if(context.get("productStoreId") != null){
				GetStoreOptionsCall  call = new GetStoreOptionsCall(EbayHelper.getApiContext((String)context.get("productStoreId"), locale, delegator));
				req = new GetStoreOptionsRequestType();

				resp = (GetStoreOptionsResponseType) call.execute(req);

				StoreColorSchemeType storeFontColorSchemeType = null;
				Map<String,Object> advanceFontTheme = FastMap.newInstance();
				if(resp != null && "SUCCESS".equals(resp.getAck().toString())){
					returnedThemeArray = resp.getAdvancedThemeArray();
					int i=0;
					List<Map<String,Object>> themeList = FastList.newInstance();

					StoreColorSchemeType[] storeColorSchemeTypes = returnedThemeArray.getGenericColorSchemeArray().getColorScheme();
					while(i<storeColorSchemeTypes.length){

						StoreColorSchemeType storeColorSchemeType = (StoreColorSchemeType)storeColorSchemeTypes[i];
						StoreFontType storeFontType =  storeColorSchemeType.getFont();
						advanceFontTheme.put("storeFontTypeNameFaceColor",storeFontType.getNameColor());
						int j=0;
						StoreFontFaceCodeType[] storeFontNameFaceCodeTypes = storeFontType.getNameFace().values();
						List<Map<String,Object>> nameFaces = FastList.newInstance();
						while(j<storeFontNameFaceCodeTypes.length){
							Map<String,Object> storeFontNameFaceCodeTypeMap = FastMap.newInstance();
							StoreFontFaceCodeType storeFontNameFaceCodeType = (StoreFontFaceCodeType)storeFontNameFaceCodeTypes[j];
							storeFontNameFaceCodeTypeMap.put("storeFontValue",storeFontNameFaceCodeType.value());
							storeFontNameFaceCodeTypeMap.put("storeFontName",storeFontNameFaceCodeType.name());
							nameFaces.add(storeFontNameFaceCodeTypeMap);
							j++;
						}
						advanceFontTheme.put("storeFontTypeFontFaceList",nameFaces);
						j=0;
						StoreFontSizeCodeType[] storeFontSizeCodeTypes =  storeFontType.getNameSize().values();
						List<Map<String,Object>> sizeFaces = FastList.newInstance();
						while(j<storeFontSizeCodeTypes.length){
							Map<String,Object> storeFontSizeCodeTypeMap = FastMap.newInstance();
							StoreFontSizeCodeType storeFontSizeCodeType = (StoreFontSizeCodeType)storeFontSizeCodeTypes[j];
							storeFontSizeCodeTypeMap.put("storeFontSizeValue",storeFontSizeCodeType.value());
							storeFontSizeCodeTypeMap.put("storeFontSizeName",storeFontSizeCodeType.name());
							sizeFaces.add(storeFontSizeCodeTypeMap);
							j++;
						}
						advanceFontTheme.put("storeFontTypeSizeFaceList",sizeFaces);

						advanceFontTheme.put("storeFontTypeTitleColor",storeFontType.getTitleColor());
						j=0;
						StoreFontFaceCodeType[] storeFontTypeTitleFaces = storeFontType.getTitleFace().values();
						List<Map<String,Object>> titleFaces = FastList.newInstance();
						while(j<storeFontTypeTitleFaces.length){
							Map<String,Object> storeFontTypeTitleFaceMap = FastMap.newInstance();
							StoreFontFaceCodeType storeFontTypeTitleFace = (StoreFontFaceCodeType)storeFontTypeTitleFaces[j];
							storeFontTypeTitleFaceMap.put("storeFontValue",storeFontTypeTitleFace.value());
							storeFontTypeTitleFaceMap.put("storeFontName",storeFontTypeTitleFace.name());
							titleFaces.add(storeFontTypeTitleFaceMap);
							j++;
						}
						advanceFontTheme.put("storeFontTypeFontTitleList",titleFaces);

						j=0;
						StoreFontSizeCodeType[] storeTitleSizeCodeTypes =  storeFontType.getTitleSize().values();
						List<Map<String,Object>> titleSizes = FastList.newInstance();
						while(j<storeTitleSizeCodeTypes.length){
							Map<String,Object> storeFontSizeCodeTypeMap = FastMap.newInstance();
							StoreFontSizeCodeType storeFontSizeCodeType = (StoreFontSizeCodeType)storeTitleSizeCodeTypes[j];
							storeFontSizeCodeTypeMap.put("storeFontSizeValue",storeFontSizeCodeType.value());
							storeFontSizeCodeTypeMap.put("storeFontSizeName",storeFontSizeCodeType.name());
							titleSizes.add(storeFontSizeCodeTypeMap);
							j++;
						}
						advanceFontTheme.put("storeFontSizeTitleList",titleSizes);


						advanceFontTheme.put("storeFontTypeDescColor",storeFontType.getDescColor());
						j=0;
						StoreFontFaceCodeType[] storeFontTypeDescFaces = storeFontType.getDescFace().values();
						List<Map<String,Object>> descFaces = FastList.newInstance();
						while(j<storeFontTypeDescFaces.length){
							Map<String,Object> storeFontTypeDescFaceMap = FastMap.newInstance();
							StoreFontFaceCodeType storeFontTypeDescFace = (StoreFontFaceCodeType)storeFontTypeDescFaces[j];
							storeFontTypeDescFaceMap.put("storeFontValue",storeFontTypeDescFace.value());
							storeFontTypeDescFaceMap.put("storeFontName",storeFontTypeDescFace.name());
							descFaces.add(storeFontTypeDescFaceMap);
							j++;
						}
						advanceFontTheme.put("storeFontTypeFontDescList",descFaces);

						j=0;
						StoreFontSizeCodeType[] storeDescSizeCodeTypes =   storeFontType.getDescSize().values();
						List<Map<String,Object>> descSizes = FastList.newInstance();
						while(j<storeDescSizeCodeTypes.length){
							Map<String,Object> storeFontSizeCodeTypeMap = FastMap.newInstance();
							StoreFontSizeCodeType storeFontSizeCodeType = (StoreFontSizeCodeType)storeDescSizeCodeTypes[j];
							storeFontSizeCodeTypeMap.put("storeFontSizeValue",storeFontSizeCodeType.value());
							storeFontSizeCodeTypeMap.put("storeFontSizeName",storeFontSizeCodeType.name());
							descSizes.add(storeFontSizeCodeTypeMap);
							j++;
						}
						advanceFontTheme.put("storeDescSizeList",descSizes);
						i++;
					}
					result = ServiceUtil.returnSuccess("load store Basic Theme option data success..");
					result.put("advanceFontTheme", advanceFontTheme);
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
}
