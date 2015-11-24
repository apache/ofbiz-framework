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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.StringUtil;
import org.ofbiz.base.util.UtilCodec;
import org.ofbiz.base.util.UtilGenerics;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityQuery;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.entity.util.EntityUtilProperties;
import org.ofbiz.product.product.ProductContentWrapper;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.ModelService;
import org.ofbiz.service.ServiceUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class ProductsExportToEbay {

    private static final String resource = "EbayUiLabels";
    private static final String configFileName = "ebayExport.properties";
    private static final String module = ProductsExportToEbay.class.getName();
    private static List<String> productExportSuccessMessageList = new LinkedList<String>();
    private static List<String> productExportFailureMessageList = new LinkedList<String>();


    public static Map<String, Object> exportToEbay(DispatchContext dctx, Map<String, Object> context) {
        Locale locale = (Locale) context.get("locale");
        Delegator delegator = dctx.getDelegator();
        productExportSuccessMessageList.clear();
        productExportFailureMessageList.clear();
        Map<String, Object> result = new HashMap<String, Object>();
        Map<String, Object> response = null;
        try {
            List<String> selectResult = UtilGenerics.checkList(context.get("selectResult"), String.class);
            List<GenericValue> productsList  = EntityQuery.use(delegator).from("Product").where(EntityCondition.makeCondition("productId", EntityOperator.IN, selectResult)).queryList();
            if (UtilValidate.isNotEmpty(productsList)) {
                for (GenericValue product : productsList) {
                    GenericValue startPriceValue = EntityUtil.getFirst(EntityUtil.filterByDate(product.getRelated("ProductPrice", UtilMisc.toMap("productPricePurposeId", "EBAY", "productPriceTypeId", "MINIMUM_PRICE"), null, false)));
                    if (UtilValidate.isEmpty(startPriceValue)) {
                        String startPriceMissingMsg = "Unable to find a starting price for auction of product with id (" + product.getString("productId") + "), So Ignoring the export of this product to eBay.";
                        productExportFailureMessageList.add(startPriceMissingMsg);
                        // Ignore the processing of product having no start price value
                        continue;
                    }
                    Map<String, Object> eBayConfigResult = EbayHelper.buildEbayConfig(context, delegator);
                    StringBuffer dataItemsXml = new StringBuffer();
                    Map<String, Object> resultMap = buildDataItemsXml(dctx, context, dataItemsXml, eBayConfigResult.get("token").toString(), product);
                    if (!ServiceUtil.isFailure(resultMap)) {
                        response = postItem(eBayConfigResult.get("xmlGatewayUri").toString(), dataItemsXml, eBayConfigResult.get("devID").toString(), eBayConfigResult.get("appID").toString(), eBayConfigResult.get("certID").toString(), "AddItem", eBayConfigResult.get("compatibilityLevel").toString(), eBayConfigResult.get("siteID").toString());
                        if (ServiceUtil.isFailure(response)) {
                            return ServiceUtil.returnFailure(ServiceUtil.getErrorMessage(response));
                        }
                        if (UtilValidate.isNotEmpty(response)) {
                            exportToEbayResponse((String) response.get("successMessage"), product);
                        }
                    } else {
                        return ServiceUtil.returnFailure(ServiceUtil.getErrorMessage(resultMap));
                    }
                }
            }
        } catch (Exception e) {
            Debug.logError("Exception in exportToEbay " + e, module);
            return ServiceUtil.returnFailure(UtilProperties.getMessage(resource, "productsExportToEbay.exceptionInExportToEbay", locale));
        }
        if (UtilValidate.isNotEmpty(productExportSuccessMessageList)) {
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
            result.put(ModelService.SUCCESS_MESSAGE_LIST, productExportSuccessMessageList);
        }
        if (UtilValidate.isNotEmpty(productExportFailureMessageList)) {
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_FAIL);
            result.put(ModelService.ERROR_MESSAGE_LIST, productExportFailureMessageList);
        }
        return result;
    }

    private static void appendRequesterCredentials(Element elem, Document doc, String token) {
        Element requesterCredentialsElem = UtilXml.addChildElement(elem, "RequesterCredentials", doc);
        UtilXml.addChildElementValue(requesterCredentialsElem, "eBayAuthToken", token, doc);
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

    public static Map<String, Object> buildDataItemsXml(DispatchContext dctx, Map<String, Object> context, StringBuffer dataItemsXml, String token, GenericValue prod) {
        Locale locale = (Locale)context.get("locale");
        try {
            Delegator delegator = dctx.getDelegator();
            String webSiteUrl = (String)context.get("webSiteUrl");

            UtilCodec.SimpleEncoder encoder = UtilCodec.getEncoder("xml");

            // Get the list of products to be exported to eBay
            try {
                Document itemDocument = UtilXml.makeEmptyXmlDocument("AddItemRequest");
                Element itemRequestElem = itemDocument.getDocumentElement();
                itemRequestElem.setAttribute("xmlns", "urn:ebay:apis:eBLBaseComponents");

                appendRequesterCredentials(itemRequestElem, itemDocument, token);

                String title = prod.getString("internalName");
                String qnt = (String)context.get("quantity");
                if (UtilValidate.isEmpty(qnt)) {
                    qnt = "1";
                }
                String productDescription = "";
                String description = prod.getString("description");
                String longDescription = prod.getString("longDescription");
                if (UtilValidate.isNotEmpty(description)) {
                    productDescription = description;
                } else if (UtilValidate.isNotEmpty(longDescription)) {
                    productDescription = longDescription;
                } else if (UtilValidate.isNotEmpty(prod.getString("productName"))) {
                    productDescription = prod.getString("productName");
                }
                String startPrice = (String)context.get("startPrice");
                String startPriceCurrencyUomId = null;
                if (UtilValidate.isEmpty(startPrice)) {
                    GenericValue startPriceValue = EntityUtil.getFirst(EntityUtil.filterByDate(prod.getRelated("ProductPrice", UtilMisc.toMap("productPricePurposeId", "EBAY", "productPriceTypeId", "MINIMUM_PRICE"), null, false)));
                    if (UtilValidate.isNotEmpty(startPriceValue)) {
                        startPrice = startPriceValue.getString("price");
                        startPriceCurrencyUomId = startPriceValue.getString("currencyUomId");
                    }
                }

                // Buy it now is the optional value for a product that you send to eBay. Once this value is entered by user - this option allow user to win auction immediately.
                String buyItNowPrice = (String)context.get("buyItNowPrice");
                String buyItNowCurrencyUomId = null;
                if (UtilValidate.isEmpty(buyItNowPrice)) {
                    GenericValue buyItNowPriceValue = EntityUtil.getFirst(EntityUtil.filterByDate(prod.getRelated("ProductPrice", UtilMisc.toMap("productPricePurposeId", "EBAY", "productPriceTypeId", "MAXIMUM_PRICE"), null, false)));
                    if (UtilValidate.isNotEmpty(buyItNowPriceValue)) {
                        buyItNowPrice = buyItNowPriceValue.getString("price");
                        buyItNowCurrencyUomId = buyItNowPriceValue.getString("currencyUomId");
                    }
                }
                

                Element itemElem = UtilXml.addChildElement(itemRequestElem, "Item", itemDocument);
                UtilXml.addChildElementValue(itemElem, "Country", (String)context.get("country"), itemDocument);
                String location = (String)context.get("location");
                if (UtilValidate.isNotEmpty(location)) {
                    UtilXml.addChildElementValue(itemElem, "Location", location, itemDocument);
                }
                UtilXml.addChildElementValue(itemElem, "ApplicationData", prod.getString("productId"), itemDocument);
                UtilXml.addChildElementValue(itemElem, "SKU", prod.getString("productId"), itemDocument);
                UtilXml.addChildElementValue(itemElem, "Title", title, itemDocument);
                UtilXml.addChildElementValue(itemElem, "ListingDuration", (String)context.get("listingDuration"), itemDocument);
                UtilXml.addChildElementValue(itemElem, "Quantity", qnt, itemDocument);

                String listingFormat = "";
                if (UtilValidate.isNotEmpty(context.get("listingFormat"))) {
                    listingFormat = (String) context.get("listingFormat");
                    UtilXml.addChildElementValue(itemElem, "ListingType", listingFormat, itemDocument);
                }
                if (listingFormat.equals("FixedPriceItem")) {
                    Element startPriceElem = UtilXml.addChildElementValue(itemElem, "StartPrice", startPrice, itemDocument);
                    if (UtilValidate.isEmpty(startPriceCurrencyUomId)) {
                        startPriceCurrencyUomId = EntityUtilProperties.getPropertyValue("general", "currency.uom.id.default", "USD", delegator);
                    }
                    startPriceElem.setAttribute("currencyID", startPriceCurrencyUomId);
                } else {
                    Element startPriceElem = UtilXml.addChildElementValue(itemElem, "StartPrice", startPrice, itemDocument);
                    if (UtilValidate.isEmpty(startPriceCurrencyUomId)) {
                        startPriceCurrencyUomId = EntityUtilProperties.getPropertyValue("general", "currency.uom.id.default", "USD", delegator);
                    }
                    startPriceElem.setAttribute("currencyID", startPriceCurrencyUomId);
                    if (UtilValidate.isNotEmpty(buyItNowPrice)) {
                        Element buyNowPriceElem = UtilXml.addChildElementValue(itemElem, "BuyItNowPrice", buyItNowPrice, itemDocument);
                    if (UtilValidate.isEmpty(buyItNowCurrencyUomId)) {
                        buyItNowCurrencyUomId = EntityUtilProperties.getPropertyValue("general", "currency.uom.id.default", "USD", delegator);
                    }
                    buyNowPriceElem.setAttribute("currencyID", buyItNowCurrencyUomId);
                    }
                }

                ProductContentWrapper pcw = new ProductContentWrapper(dctx.getDispatcher(), prod, locale, EntityUtilProperties.getPropertyValue("content", "defaultMimeType", "text/html; charset=utf-8", delegator));
                StringUtil.StringWrapper ebayDescription = pcw.get("EBAY_DESCRIPTION", "html");
                if (UtilValidate.isNotEmpty(ebayDescription.toString())) {
                    UtilXml.addChildElementCDATAValue(itemElem, "Description", ebayDescription.toString(), itemDocument);
                } else {
                    UtilXml.addChildElementValue(itemElem, "Description", encoder.encode(productDescription), itemDocument);
                }
                String smallImage = prod.getString("smallImageUrl");
                String mediumImage = prod.getString("mediumImageUrl");
                String largeImage = prod.getString("largeImageUrl");
                String ebayImage = null;
                if (UtilValidate.isNotEmpty(largeImage)) {
                    ebayImage = largeImage;
                } else if (UtilValidate.isNotEmpty(mediumImage)) {
                    ebayImage = mediumImage;
                } else if (UtilValidate.isNotEmpty(smallImage)) {
                    ebayImage = smallImage;
                }
                if (UtilValidate.isNotEmpty(ebayImage)) {
                    Element pictureDetails = UtilXml.addChildElement(itemElem, "PictureDetails", itemDocument);
                    UtilXml.addChildElementValue(pictureDetails, "PictureURL", webSiteUrl + ebayImage, itemDocument);
                }

                setPaymentMethodAccepted(itemDocument, itemElem, context);
                setMiscDetails(itemDocument, itemElem, context, delegator);
                String categoryCode = (String)context.get("ebayCategory");
                String primaryCategoryId = "";
                if (categoryCode != null) {
                    if (categoryCode.indexOf("_") != -1) {
                        String[] params = categoryCode.split("_");
                        if (UtilValidate.isEmpty(params) || params[1].length() == 0) {
                            ServiceUtil.returnFailure(UtilProperties.getMessage(resource, "productsExportToEbay.parametersNotCorrectInGetEbayCategories", locale));
                        } else {
                            primaryCategoryId = params[1];
                        }
                    }else{
                        primaryCategoryId = categoryCode;
                    }
                } else {
                    GenericValue productCategoryValue = EntityQuery.use(delegator).from("ProductCategoryAndMember")
                            .where("productCategoryTypeId", "EBAY_CATEGORY", "productId", prod.getString("productId"))
                            .filterByDate()
                            .queryFirst();
                    if (UtilValidate.isNotEmpty(productCategoryValue)) {
                        primaryCategoryId = productCategoryValue.getString("categoryName");
                    }
                }

                Element primaryCatElem = UtilXml.addChildElement(itemElem, "PrimaryCategory", itemDocument);
                UtilXml.addChildElementValue(primaryCatElem, "CategoryID", primaryCategoryId, itemDocument);
                UtilXml.addChildElementValue(itemElem, "ConditionID", "1000", itemDocument);
                Element itemSpecificsElem = UtilXml.addChildElement(itemElem, "ItemSpecifics", itemDocument);
                Element nameValueListElem = UtilXml.addChildElement(itemSpecificsElem, "NameValueList", itemDocument);
                UtilXml.addChildElementValue(nameValueListElem, "Name", "Condition", itemDocument);
                UtilXml.addChildElementValue(nameValueListElem, "Value", "New: With Tags", itemDocument);

                //Debug.logInfo("The generated string is ======= " + UtilXml.writeXmlDocument(itemDocument), module);
                dataItemsXml.append(UtilXml.writeXmlDocument(itemDocument));
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

    private static Map<String, Object> buildCategoriesXml(Map<String, Object> context, StringBuffer dataItemsXml, String token, String siteID, String categoryParent, String levelLimit) {
        Locale locale = (Locale)context.get("locale");
        try {
            Document itemRequest = UtilXml.makeEmptyXmlDocument("GetCategoriesRequest");
            Element itemRequestElem = itemRequest.getDocumentElement();
            itemRequestElem.setAttribute("xmlns", "urn:ebay:apis:eBLBaseComponents");

            appendRequesterCredentials(itemRequestElem, itemRequest, token);

            UtilXml.addChildElementValue(itemRequestElem, "DetailLevel", "ReturnAll", itemRequest);
            UtilXml.addChildElementValue(itemRequestElem, "CategorySiteID", siteID, itemRequest);

            if (UtilValidate.isNotEmpty(categoryParent)) {
                UtilXml.addChildElementValue(itemRequestElem, "CategoryParent", categoryParent, itemRequest);
            }

            if (UtilValidate.isEmpty(levelLimit)) {
                levelLimit = "1";
            }

            UtilXml.addChildElementValue(itemRequestElem, "LevelLimit", levelLimit, itemRequest);
            UtilXml.addChildElementValue(itemRequestElem, "ViewAllNodes", "true", itemRequest);

            dataItemsXml.append(UtilXml.writeXmlDocument(itemRequest));
        } catch (Exception e) {
            Debug.logError("Exception during building data items to eBay", module);
            return ServiceUtil.returnFailure(UtilProperties.getMessage(resource, "productsExportToEbay.exceptionDuringBuildingGetCategoriesRequest", locale));
        }
        return ServiceUtil.returnSuccess();
    }

    /*
    private static Map<String, Object> buildSetTaxTableRequestXml(DispatchContext dctx, Map<String, Object> context, StringBuffer setTaxTableRequestXml, String token) {
        Locale locale = (Locale)context.get("locale");
        try {
            Document taxRequestDocument = UtilXml.makeEmptyXmlDocument("SetTaxTableRequest");
            Element taxRequestElem = taxRequestDocument.getDocumentElement();
            taxRequestElem.setAttribute("xmlns", "urn:ebay:apis:eBLBaseComponents");

            appendRequesterCredentials(taxRequestElem, taxRequestDocument, token);

            Element taxTableElem = UtilXml.addChildElement(taxRequestElem, "TaxTable", taxRequestDocument);
            Element taxJurisdictionElem = UtilXml.addChildElement(taxTableElem, "TaxJurisdiction", taxRequestDocument);

            UtilXml.addChildElementValue(taxJurisdictionElem, "JurisdictionID", "NY", taxRequestDocument);
            UtilXml.addChildElementValue(taxJurisdictionElem, "SalesTaxPercent", "4.25", taxRequestDocument);
            UtilXml.addChildElementValue(taxJurisdictionElem, "ShippingIncludedInTax", "false", taxRequestDocument);

            setTaxTableRequestXml.append(UtilXml.writeXmlDocument(taxRequestDocument));
        } catch (Exception e) {
            Debug.logError("Exception during building request set tax table to eBay", module);
            return ServiceUtil.returnFailure(UtilProperties.getMessage(resource, "productsExportToEbay.exceptionDuringBuildingRequestSetTaxTableToEbay", locale));
        }
        return ServiceUtil.returnSuccess();
    }
    */

    /*
    private static Map<String, Object> buildAddTransactionConfirmationItemRequest(Map<String, Object> context, StringBuffer dataItemsXml, String token, String itemId) {
        Locale locale = (Locale)context.get("locale");
        try {
            Document transDoc = UtilXml.makeEmptyXmlDocument("AddTransactionConfirmationItemRequest");
            Element transElem = transDoc.getDocumentElement();
            transElem.setAttribute("xmlns", "urn:ebay:apis:eBLBaseComponents");

            appendRequesterCredentials(transElem, transDoc, token);

            UtilXml.addChildElementValue(transElem, "ItemID", itemId, transDoc);
            UtilXml.addChildElementValue(transElem, "ListingDuration", "Days_1", transDoc);
            Element negotiatePriceElem = UtilXml.addChildElementValue(transElem, "NegotiatedPrice", "50.00", transDoc);
            negotiatePriceElem.setAttribute("currencyID", "USD");
            UtilXml.addChildElementValue(transElem, "RecipientRelationType", "1", transDoc);
            UtilXml.addChildElementValue(transElem, "RecipientUserID", "buyer_anytime", transDoc);

            dataItemsXml.append(UtilXml.writeXmlDocument(transDoc));
            Debug.logInfo(dataItemsXml.toString(), module);
        } catch (Exception e) {
            Debug.logError("Exception during building AddTransactionConfirmationItemRequest eBay", module);
            return ServiceUtil.returnFailure(UtilProperties.getMessage(resource, "productsExportToEbay.exceptionDuringBuildingAddTransactionConfirmationItemRequestToEbay", locale));
        }
        return ServiceUtil.returnSuccess();
    }
    */

    private static void setPaymentMethodAccepted(Document itemDocument, Element itemElem, Map<String, Object> context) {
        String payPal = (String)context.get("paymentPayPal");
        String payPalEmail = (String)context.get("payPalEmail");
        String visaMC = (String)context.get("paymentVisaMC");
        String amEx = (String)context.get("paymentAmEx");
        String discover = (String)context.get("paymentDiscover");
        String ccAccepted = (String)context.get("paymentCCAccepted");
        String cashInPerson = (String)context.get("paymentCashInPerson");
        String cashOnPickup = (String)context.get("paymentCashOnPickup");
        String cod = (String)context.get("paymentCOD");
        String codPrePayDelivery = (String)context.get("paymentCODPrePayDelivery");
        String mocc = (String)context.get("paymentMOCC");
        String moneyXferAccepted = (String)context.get("paymentMoneyXferAccepted");
        String personalCheck = (String)context.get("paymentPersonalCheck");

        // PayPal
        if (UtilValidate.isNotEmpty(payPal) && "on".equals(payPal)) {
            UtilXml.addChildElementValue(itemElem, "PaymentMethods", "PayPal", itemDocument);
            // PayPal email
            if (UtilValidate.isNotEmpty(payPalEmail)) {
                UtilXml.addChildElementValue(itemElem, "PayPalEmailAddress", payPalEmail, itemDocument);
            }
        }
        // Visa/Master Card
        if (UtilValidate.isNotEmpty(visaMC) && "on".equals(visaMC)) {
            UtilXml.addChildElementValue(itemElem, "PaymentMethods", "VisaMC", itemDocument);
        }
        // American Express
        if (UtilValidate.isNotEmpty(amEx) && "on".equals(amEx)) {
            UtilXml.addChildElementValue(itemElem, "PaymentMethods", "AmEx", itemDocument);
        }
        // Discover
        if (UtilValidate.isNotEmpty(discover) && "on".equals(discover)) {
            UtilXml.addChildElementValue(itemElem, "PaymentMethods", "Discover", itemDocument);
        }
        // Credit Card Accepted
        if (UtilValidate.isNotEmpty(ccAccepted) && "on".equals(ccAccepted)) {
            UtilXml.addChildElementValue(itemElem, "PaymentMethods", "CCAccepted", itemDocument);
        }
        // Cash In Person
        if (UtilValidate.isNotEmpty(cashInPerson) && "on".equals(cashInPerson)) {
            UtilXml.addChildElementValue(itemElem, "PaymentMethods", "CashInPerson", itemDocument);
        }
        // Cash on Pickup
        if (UtilValidate.isNotEmpty(cashOnPickup) && "on".equals(cashOnPickup)) {
            UtilXml.addChildElementValue(itemElem, "PaymentMethods", "CashOnPickup", itemDocument);
        }
        // Cash on Delivery
        if (UtilValidate.isNotEmpty(cod) && "on".equals(cod)) {
            UtilXml.addChildElementValue(itemElem, "PaymentMethods", "COD", itemDocument);
        }
        // Cash On Delivery After Paid
        if (UtilValidate.isNotEmpty(codPrePayDelivery) && "on".equals(codPrePayDelivery)) {
            UtilXml.addChildElementValue(itemElem, "PaymentMethods", "CODPrePayDelivery", itemDocument);
        }
        // Money order/cashiers check
        if (UtilValidate.isNotEmpty(mocc) && "on".equals(mocc)) {
            UtilXml.addChildElementValue(itemElem, "PaymentMethods", "MOCC", itemDocument);
        }
        // Direct transfer of money
        if (UtilValidate.isNotEmpty(moneyXferAccepted) && "on".equals(moneyXferAccepted)) {
            UtilXml.addChildElementValue(itemElem, "PaymentMethods", "MoneyXferAccepted", itemDocument);
        }
        // Personal Check
        if (UtilValidate.isNotEmpty(personalCheck) && "on".equals(personalCheck)) {
            UtilXml.addChildElementValue(itemElem, "PaymentMethods", "PersonalCheck", itemDocument);
        }
    }

    private static void setMiscDetails(Document itemDocument, Element itemElem, Map<String, Object> context, Delegator delegator) throws Exception {
        String customXmlFromUI = (String) context.get("customXml");
        String customXml = "";
        if (UtilValidate.isNotEmpty(customXmlFromUI)) {
            customXml = customXmlFromUI;
        } else {
            customXml = EntityUtilProperties.getPropertyValue(configFileName, "eBayExport.customXml", delegator);
        }
        if (UtilValidate.isNotEmpty(customXml)) {
            Document customXmlDoc = UtilXml.readXmlDocument(customXml);
            if (UtilValidate.isNotEmpty(customXmlDoc)) {
                Element customXmlElement = customXmlDoc.getDocumentElement();
                for (Element eBayElement: UtilXml.childElementList(customXmlElement)) {
                    Node importedElement = itemElem.getOwnerDocument().importNode(eBayElement, true);
                    itemElem.appendChild(importedElement);
                }
            }
        }
    }

    public static Map<String, Object> getEbayCategories(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get("locale");
        String categoryCode = (String)context.get("categoryCode");
        Map<String, Object> result = null;

        try {
            Map<String, Object> eBayConfigResult = EbayHelper.buildEbayConfig(context, delegator);
            String categoryParent = "";
            String levelLimit = "";
            if (categoryCode != null) {
                String[] params = categoryCode.split("_");

                if (params == null || params.length != 3) {
                    ServiceUtil.returnFailure(UtilProperties.getMessage(resource, "productsExportToEbay.parametersNotCorrectInGetEbayCategories", locale));
                } else {
                    categoryParent = params[1];
                    levelLimit = params[2];
                    Integer level = Integer.valueOf(levelLimit);
                    levelLimit = (level.intValue() + 1) + "";
                }
            }

            StringBuffer dataItemsXml = new StringBuffer();
            if (!ServiceUtil.isFailure(buildCategoriesXml(context, dataItemsXml, eBayConfigResult.get("token").toString(), eBayConfigResult.get("siteID").toString(), categoryParent, levelLimit))) {
                Map<String, Object> resultCat = postItem(eBayConfigResult.get("xmlGatewayUri").toString(), dataItemsXml, eBayConfigResult.get("devID").toString(), eBayConfigResult.get("appID").toString(), eBayConfigResult.get("certID").toString(), "GetCategories", eBayConfigResult.get("compatibilityLevel").toString(), eBayConfigResult.get("siteID").toString());
                String successMessage = (String)resultCat.get("successMessage");
                if (successMessage != null) {
                    result = readEbayCategoriesResponse(successMessage, locale);
                } else {
                    ServiceUtil.returnFailure(ServiceUtil.getErrorMessage(resultCat));
                }
            }
        } catch (Exception e) {
            Debug.logError("Exception in GetEbayCategories " + e, module);
            return ServiceUtil.returnFailure(UtilProperties.getMessage(resource, "productsExportToEbay.exceptionInGetEbayCategories", locale));
        }
        return result;
    }

    private static Map<String, Object> readEbayCategoriesResponse(String msg, Locale locale) {
        Map<String, Object> results = null;
        List<Map<String, Object>> categories = new LinkedList<Map<String,Object>>();
        try {
            Document docResponse = UtilXml.readXmlDocument(msg, true);
            Element elemResponse = docResponse.getDocumentElement();
            String ack = UtilXml.childElementValue(elemResponse, "Ack", "Failure");
            if (ack != null && "Failure".equals(ack)) {
                String errorMessage = "";
                for (Element errorElement : UtilXml.childElementList(elemResponse, "Errors")) {
                    errorMessage = UtilXml.childElementValue(errorElement, "ShortMessage", "");
                }
                return ServiceUtil.returnFailure(errorMessage);
            } else {
                // retrieve Category Array
                for (Element categoryArrayElement : UtilXml.childElementList(elemResponse, "CategoryArray")) {
                    // retrieve Category
                    for (Element categoryElement : UtilXml.childElementList(categoryArrayElement, "Category")) {
                        Map<String, Object> categ = new HashMap<String, Object>();

                        String categoryCode = ("true".equalsIgnoreCase((UtilXml.childElementValue(categoryElement, "LeafCategory", "").trim())) ? "Y" : "N") + "_" +
                                              UtilXml.childElementValue(categoryElement, "CategoryID", "").trim() + "_" +
                                              UtilXml.childElementValue(categoryElement, "CategoryLevel", "").trim();
                        categ.put("CategoryCode", categoryCode);
                        categ.put("CategoryName", UtilXml.childElementValue(categoryElement, "CategoryName"));
                        categories.add(categ);
                    }
                }
                categories = UtilGenerics.cast(UtilMisc.sortMaps(UtilGenerics.<List<Map<Object, Object>>>cast(categories), UtilMisc.toList("CategoryName")));
                results = UtilMisc.toMap("categories", (Object) categories);
            }
        } catch (Exception e) {
            return ServiceUtil.returnFailure();
        }
        return results;
    }

    public static Map<String, Object> exportToEbayResponse(String msg, GenericValue product) {
        Map<String, Object> result = ServiceUtil.returnSuccess();
        try {
            Document docResponse = UtilXml.readXmlDocument(msg, true);
            Element elemResponse = docResponse.getDocumentElement();
            String ack = UtilXml.childElementValue(elemResponse, "Ack", "Failure");
            if (ack != null && "Failure".equals(ack)) {
                String errorMessage = "";
                for (Element errorElement : UtilXml.childElementList(elemResponse, "Errors")) {
                    errorMessage = UtilXml.childElementValue(errorElement, "LongMessage");
                }
                productExportFailureMessageList.add(errorMessage);
            } else {
                String productSuccessfullyExportedMsg = "Product successfully exported with ID (" + product.getString("productId") + ").";
                productExportSuccessMessageList.add(productSuccessfullyExportedMsg);
            }
        } catch (Exception e) {
            Debug.logError("Error in processing xml string" + e.getMessage(), module);
            return ServiceUtil.returnFailure();
        }
        return result;
    }
    public static List<String> getProductExportSuccessMessageList(){
        return productExportSuccessMessageList;
    }

    public static List<String> getproductExportFailureMessageList(){
        return productExportFailureMessageList;
    }
}
