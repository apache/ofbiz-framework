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

package org.apache.ofbiz.ebay;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.UtilXml;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityComparisonOperator;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.order.order.OrderChangeHelper;
import org.apache.ofbiz.order.shoppingcart.CheckOutHelper;
import org.apache.ofbiz.order.shoppingcart.ShoppingCart;
import org.apache.ofbiz.order.shoppingcart.ShoppingCartItem;
import org.apache.ofbiz.party.party.PartyWorker;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.service.ServiceUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class EbayOrderServices {

    private static final String resource = "EbayUiLabels";
    private static final String module = EbayOrderServices.class.getName();
    private static boolean isGetSellerTransactionsCall = false;
    private static boolean isGetOrdersCall = false;
    private static boolean isGetMyeBaySellingCall = false;
    private static List<Map<String, Object>> orderList = new LinkedList<Map<String,Object>>();
    private static List<String> getSellerTransactionsContainingOrderList = new LinkedList<String>();
    private static List<String> orderImportSuccessMessageList = new LinkedList<String>();
    private static List<String> orderImportFailureMessageList = new LinkedList<String>();

    public static Map<String, Object> getEbayOrders(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Locale locale = (Locale) context.get("locale");
        orderImportSuccessMessageList.clear();
        orderImportFailureMessageList.clear();
        getSellerTransactionsContainingOrderList.clear();
        orderList.clear();
        Map<String, Object> result = new HashMap<String, Object>();
        try {
            Map<String, Object> eBayConfigResult = EbayHelper.buildEbayConfig(context, delegator);
            if (UtilValidate.isEmpty(eBayConfigResult)) {
                String eBayConfigErrorMsg = UtilProperties.getMessage(resource, "EbayConfigurationSettingsAreMissingForConnectingToEbayServer", locale);
                return ServiceUtil.returnError(eBayConfigErrorMsg);
            }

            StringBuffer sellerTransactionsItemsXml = new StringBuffer();
            if (!ServiceUtil.isFailure(buildGetSellerTransactionsRequest(context, sellerTransactionsItemsXml, eBayConfigResult.get("token").toString()))) {
                result = EbayHelper.postItem(eBayConfigResult.get("xmlGatewayUri").toString(), sellerTransactionsItemsXml, eBayConfigResult.get("devID").toString(), eBayConfigResult.get("appID").toString(), eBayConfigResult.get("certID").toString(), "GetSellerTransactions", eBayConfigResult.get("compatibilityLevel").toString(), eBayConfigResult.get("siteID").toString());
                String  getSellerTransactionSuccessMsg = (String) result.get(ModelService.SUCCESS_MESSAGE);
                if (getSellerTransactionSuccessMsg != null) {
                    isGetSellerTransactionsCall = true;
                    result = checkOrders(delegator, dispatcher, locale, context, getSellerTransactionSuccessMsg);
                }
            }

            StringBuffer getOrdersXml = new StringBuffer();
            if (!ServiceUtil.isFailure(buildGetOrdersRequest(context, getOrdersXml, eBayConfigResult.get("token").toString()))) {
                result = EbayHelper.postItem(eBayConfigResult.get("xmlGatewayUri").toString(), getOrdersXml, eBayConfigResult.get("devID").toString(), eBayConfigResult.get("appID").toString(), eBayConfigResult.get("certID").toString(), "GetOrders", eBayConfigResult.get("compatibilityLevel").toString(), eBayConfigResult.get("siteID").toString());
                String getOrdersSuccessMsg = (String) result.get(ModelService.SUCCESS_MESSAGE);
                if (getOrdersSuccessMsg != null) {
                    isGetOrdersCall = true;
                    result = checkOrders(delegator, dispatcher, locale, context, getOrdersSuccessMsg);
                }
            }

            StringBuffer getMyeBaySellingXml = new StringBuffer();
            if (!ServiceUtil.isFailure(buildGetMyeBaySellingRequest(context, getMyeBaySellingXml, eBayConfigResult.get("token").toString()))) {
                result = EbayHelper.postItem(eBayConfigResult.get("xmlGatewayUri").toString(), getMyeBaySellingXml, eBayConfigResult.get("devID").toString(), eBayConfigResult.get("appID").toString(), eBayConfigResult.get("certID").toString(), "GetMyeBaySelling", eBayConfigResult.get("compatibilityLevel").toString(), eBayConfigResult.get("siteID").toString());
                String getMyeBaySellingSuccessMsg = (String) result.get(ModelService.SUCCESS_MESSAGE);
                if (getMyeBaySellingSuccessMsg != null) {
                    isGetMyeBaySellingCall = true;
                    result = checkOrders(delegator, dispatcher, locale, context, getMyeBaySellingSuccessMsg);
                }
            }
        } catch (Exception e) {
            String errMsg = UtilProperties.getMessage(resource, "buildEbayConfig.exceptionInGetOrdersFromEbay" + e.getMessage(), locale);
            return ServiceUtil.returnError(errMsg);
        }
        return result;
    }

    public static Map<String, Object> importEbayOrders(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Locale locale = (Locale) context.get("locale");
        Map<String, Object> result = new HashMap<String, Object>();
        String externalId = (String) context.get("externalId");
        List<Map<String, Object>> orderList = UtilGenerics.checkList(context.get("orderList"));
        try {
            if (UtilValidate.isNotEmpty(orderList)) {
                Iterator<Map<String, Object>> orderListIter = orderList.iterator();
                while (orderListIter.hasNext()) {
                    Map<String, Object> orderMapCtx = orderListIter.next();
                    if (externalId.equals(orderMapCtx.get("externalId").toString())) {
                        context.clear();
                        context.putAll(orderMapCtx);
                        break;
                    }
                }
            }
            result = createShoppingCart(delegator, dispatcher, locale, context, true);
        } catch (Exception e) {
            Debug.logError("Exception in importOrderFromEbay " + e, module);
            return ServiceUtil.returnFailure(UtilProperties.getMessage(resource, "ordersImportFromEbay.exceptionInImportOrderFromEbay", locale));
        }
        if (UtilValidate.isNotEmpty(orderImportSuccessMessageList)) {
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
            result.put(ModelService.SUCCESS_MESSAGE_LIST, orderImportSuccessMessageList);
        }

        if (UtilValidate.isNotEmpty(orderImportSuccessMessageList)) {
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_FAIL);
            result.put(ModelService.ERROR_MESSAGE_LIST, orderImportFailureMessageList);
        }
        return result;
    }

    private static Map<String, Object> buildGetOrdersRequest(Map<String, Object> context, StringBuffer dataItemsXml, String token) {
        Locale locale = (Locale) context.get("locale");
        String fromDate = (String) context.get("fromDate");
        String thruDate = (String) context.get("thruDate");
        try {
             Document transDoc = UtilXml.makeEmptyXmlDocument("GetOrdersRequest");
             Element transElem = transDoc.getDocumentElement();
             transElem.setAttribute("xmlns", "urn:ebay:apis:eBLBaseComponents");
             UtilXml.addChildElementValue(transElem, "DetailLevel", "ReturnAll", transDoc);

             EbayHelper.appendRequesterCredentials(transElem, transDoc, token);

             if (UtilValidate.isNotEmpty(getSellerTransactionsContainingOrderList)) {
                 Element orderIdArrayElem = UtilXml.addChildElement(transElem, "OrderIDArray", transDoc);
                 Iterator<String> orderIdListIter = getSellerTransactionsContainingOrderList.iterator();
                 while (orderIdListIter.hasNext()) {
                     String orderId = orderIdListIter.next();
                     UtilXml.addChildElementValue(orderIdArrayElem, "OrderID", orderId, transDoc);
                 }
             } else {
                 UtilXml.addChildElementValue(transElem, "OrderRole", "Seller", transDoc);
                 UtilXml.addChildElementValue(transElem, "OrderStatus", "Completed", transDoc);

                 String fromDateOut = EbayHelper.convertDate(fromDate, "yyyy-MM-dd HH:mm:ss.SSS", "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
                 if (fromDateOut != null) {
                     UtilXml.addChildElementValue(transElem, "CreateTimeFrom", fromDateOut, transDoc);
                 } else {
                     Debug.logError("Cannot convert from date from yyyy-MM-dd HH:mm:ss.SSS date format to yyyy-MM-dd'T'HH:mm:ss.SSS'Z' date format", module);
                     return ServiceUtil.returnFailure(UtilProperties.getMessage(resource, "ordersImportFromEbay.cannotConvertFromDate", locale));
                 }
                 fromDateOut = EbayHelper.convertDate(thruDate, "yyyy-MM-dd HH:mm:ss.SSS", "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
                 if (fromDateOut != null) {
                     UtilXml.addChildElementValue(transElem, "CreateTimeTo", fromDateOut, transDoc);
                 } else {
                     Debug.logError("Cannot convert thru date from yyyy-MM-dd HH:mm:ss.SSS date format to yyyy-MM-dd'T'HH:mm:ss.SSS'Z' date format", module);
                     return ServiceUtil.returnFailure(UtilProperties.getMessage(resource, "ordersImportFromEbay.cannotConvertThruDate", locale));
                 }
             }
             //Debug.logInfo("The value of generated string is ======= " + UtilXml.writeXmlDocument(transDoc), module);
             dataItemsXml.append(UtilXml.writeXmlDocument(transDoc));
         } catch (Exception e) {
             Debug.logError("Exception during building get seller transactions request", module);
             return ServiceUtil.returnFailure(UtilProperties.getMessage(resource, "ordersImportFromEbay.exceptionDuringBuildingGetSellerTransactionRequest", locale));
         }
         return ServiceUtil.returnSuccess();
    }

    private static Map<String, Object> buildGetSellerTransactionsRequest(Map<String, Object> context, StringBuffer sellerTransactionsItemsXml, String token) {
        Locale locale = (Locale)context.get("locale");
        String fromDate = (String)context.get("fromDate");
        String thruDate = (String)context.get("thruDate");
        try {
             Document transDoc = UtilXml.makeEmptyXmlDocument("GetSellerTransactionsRequest");
             Element transElem = transDoc.getDocumentElement();
             transElem.setAttribute("xmlns", "urn:ebay:apis:eBLBaseComponents");

             EbayHelper.appendRequesterCredentials(transElem, transDoc, token);
             UtilXml.addChildElementValue(transElem, "DetailLevel", "ReturnAll", transDoc);
             UtilXml.addChildElementValue(transElem, "IncludeContainingOrder", "true", transDoc);

             String fromDateOut = EbayHelper.convertDate(fromDate, "yyyy-MM-dd HH:mm:ss.SSS", "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
             if (fromDateOut != null) {
                 UtilXml.addChildElementValue(transElem, "ModTimeFrom", fromDateOut, transDoc);
             } else {
                 Debug.logError("Cannot convert from date from yyyy-MM-dd HH:mm:ss.SSS date format to yyyy-MM-dd'T'HH:mm:ss.SSS'Z' date format", module);
                 return ServiceUtil.returnFailure(UtilProperties.getMessage(resource, "ordersImportFromEbay.cannotConvertFromDate", locale));
             }

             fromDateOut = EbayHelper.convertDate(thruDate, "yyyy-MM-dd HH:mm:ss.SSS", "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
             if (fromDateOut != null) {
                 UtilXml.addChildElementValue(transElem, "ModTimeTo", fromDateOut, transDoc);
             } else {
                 Debug.logError("Cannot convert thru date from yyyy-MM-dd HH:mm:ss.SSS date format to yyyy-MM-dd'T'HH:mm:ss.SSS'Z' date format", module);
                 return ServiceUtil.returnFailure(UtilProperties.getMessage(resource, "ordersImportFromEbay.cannotConvertThruDate", locale));
             }
             //Debug.logInfo("The value of generated string is ======= " + UtilXml.writeXmlDocument(transDoc), module);
             sellerTransactionsItemsXml.append(UtilXml.writeXmlDocument(transDoc));
         } catch (Exception e) {
             Debug.logError("Exception during building get seller transactions request", module);
             return ServiceUtil.returnFailure(UtilProperties.getMessage(resource, "ordersImportFromEbay.exceptionDuringBuildingGetSellerTransactionRequest", locale));
         }
         return ServiceUtil.returnSuccess();
    }

    private static Map<String, Object> buildGetMyeBaySellingRequest(Map<String, Object> context, StringBuffer getMyeBaySellingXml, String token) {
        Locale locale = (Locale) context.get("locale");
        try {
             Document transDoc = UtilXml.makeEmptyXmlDocument("GetMyeBaySellingRequest");
             Element transElem = transDoc.getDocumentElement();
             transElem.setAttribute("xmlns", "urn:ebay:apis:eBLBaseComponents");

             EbayHelper.appendRequesterCredentials(transElem, transDoc, token);

             Element deletedFromSoldListElem = UtilXml.addChildElement(transElem, "DeletedFromSoldList", transDoc);
             UtilXml.addChildElementValue(deletedFromSoldListElem, "Sort", "ItemID", transDoc);
             //Debug.logInfo("The value of generated string is ======= " + UtilXml.writeXmlDocument(transDoc), module);
             getMyeBaySellingXml.append(UtilXml.writeXmlDocument(transDoc));
         } catch (Exception e) {
             Debug.logError("Exception during building MyeBaySelling request", module);
             return ServiceUtil.returnFailure(UtilProperties.getMessage(resource, "ordersImportFromEbay.exceptionDuringBuildingMyeBaySellingRequest", locale));
         }
         return ServiceUtil.returnSuccess();
    }

    private static Map<String, Object> checkOrders(Delegator delegator, LocalDispatcher dispatcher, Locale locale, Map<String, Object> context, String responseMsg) {
        StringBuffer errorMessage = new StringBuffer();
        Map<String, Object> result = new HashMap<String, Object>();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        if (isGetSellerTransactionsCall) {
            List<Map<String, Object>> getSellerTransactionList = readGetSellerTransactionResponse(responseMsg, locale, (String) context.get("productStoreId"), delegator, dispatcher, errorMessage, userLogin);
            if (UtilValidate.isNotEmpty(getSellerTransactionList)) {
                orderList.addAll(getSellerTransactionList);
            }
            isGetSellerTransactionsCall = false;
            return ServiceUtil.returnSuccess();
        } else if (isGetOrdersCall) {
            List<Map<String, Object>> getOrdersList = readGetOrdersResponse(responseMsg, locale, (String) context.get("productStoreId"), delegator, dispatcher, errorMessage, userLogin);
            if (UtilValidate.isNotEmpty(getOrdersList)) {
                orderList.addAll(getOrdersList);
            }
            isGetOrdersCall = false;
            return ServiceUtil.returnSuccess();
        } else if (isGetMyeBaySellingCall) {
            // for now fetching only deleted transaction & orders value from the sold list.
            List<String> eBayDeletedOrdersAndTransactionList = readGetMyeBaySellingResponse(responseMsg, locale, (String) context.get("productStoreId"), delegator, dispatcher, errorMessage, userLogin);
            if (UtilValidate.isNotEmpty(eBayDeletedOrdersAndTransactionList)) {
                Debug.logInfo("The value of getMyeBaySellingList" + eBayDeletedOrdersAndTransactionList, module);
                Iterator<Map<String, Object>> orderListIter = orderList.iterator();
                while (orderListIter.hasNext()) {
                    Map<String, Object> orderCtx = orderListIter.next();
                    if (eBayDeletedOrdersAndTransactionList.contains(orderCtx.get("externalId"))) {
                         // Now finally exclude orders & transaction that has been deleted from sold list.
                        orderList.remove(orderCtx);
                    }
                }
            }
        }
        if (UtilValidate.isEmpty(orderList)) {
            Debug.logError("No orders found", module);
            return ServiceUtil.returnFailure(UtilProperties.getMessage(resource, "ordersImportFromEbay.noOrdersFound", locale));
        }
        if (UtilValidate.isNotEmpty(orderList)) {
            result.put("orderList", orderList);
        }
        return result;
    }

// Sample xml data that is being generated from GetOrders request
//
//    <?xml version="1.0" encoding="UTF-8"?><GetOrdersResponse xmlns="urn:ebay:apis:eBLBaseComponents">
//    <Timestamp>2009-09-10T11:17:53.529Z</Timestamp>
//    <Ack>Success</Ack>
//    <Version>631</Version>
//    <Build>E631_CORE_BUNDLED_9942930_R1</Build>
//    <OrderArray>
//        <Order>
//            <OrderID>116583010</OrderID>
//            <OrderStatus>Completed</OrderStatus>
//            <AdjustmentAmount currencyID="USD">0.0</AdjustmentAmount>
//            <AmountSaved currencyID="USD">0.0</AmountSaved>
//            <CheckoutStatus>
//                <eBayPaymentStatus>NoPaymentFailure</eBayPaymentStatus>
//                <LastModifiedTime>2009-09-08T14:07:10.000Z</LastModifiedTime>
//                <PaymentMethod>CCAccepted</PaymentMethod>
//                <Status>Complete</Status>
//                <IntegratedMerchantCreditCardEnabled>false</IntegratedMerchantCreditCardEnabled>
//            </CheckoutStatus>
//            <ShippingDetails>
//                <InsuranceFee currencyID="USD">0.0</InsuranceFee>
//                <InsuranceOption>NotOffered</InsuranceOption>
//                <InsuranceWanted>false</InsuranceWanted>
//                <SalesTax>
//                    <SalesTaxPercent>0.0</SalesTaxPercent>
//                    <SalesTaxState/>
//                    <ShippingIncludedInTax>false</ShippingIncludedInTax>
//                    <SalesTaxAmount currencyID="USD">0.0</SalesTaxAmount>
//                </SalesTax>
//                <ShippingServiceOptions>
//                    <ShippingService>UPS2ndDay</ShippingService>
//                    <ShippingServiceCost currencyID="USD">10.0</ShippingServiceCost>
//                    <ShippingServicePriority>1</ShippingServicePriority>
//                    <ExpeditedService>false</ExpeditedService>
//                    <ShippingTimeMin>1</ShippingTimeMin>
//                    <ShippingTimeMax>2</ShippingTimeMax>
//                </ShippingServiceOptions>
//                <SellingManagerSalesRecordNumber>103</SellingManagerSalesRecordNumber>
//                <GetItFast>false</GetItFast>
//            </ShippingDetails>
//            <CreatingUserRole>Buyer</CreatingUserRole>
//            <CreatedTime>2009-09-08T08:53:23.000Z</CreatedTime>
//            <PaymentMethods>AmEx</PaymentMethods>
//            <PaymentMethods>Discover</PaymentMethods>
//            <PaymentMethods>VisaMC</PaymentMethods>
//            <ShippingAddress>
//                <Name>Apache OFBiz</Name>
//                <Street1>Apache Software Foundation</Street1>
//                <Street2/>
//                <CityName>Salt Lake City</CityName>
//                <StateOrProvince>UT</StateOrProvince>
//                <Country>US</Country>
//                <CountryName>United States</CountryName>
//                <Phone>888 887 9876</Phone>
//                <PostalCode>84101</PostalCode>
//                <AddressID>5149770</AddressID>
//                <AddressOwner>eBay</AddressOwner>
//                <ExternalAddressID/>
//            </ShippingAddress>
//            <ShippingServiceSelected>
//                <ShippingInsuranceCost currencyID="USD">0.0</ShippingInsuranceCost>
//                <ShippingService>UPS2ndDay</ShippingService>
//                <ShippingServiceCost currencyID="USD">10.0</ShippingServiceCost>
//            </ShippingServiceSelected>
//            <Subtotal currencyID="USD">132.0</Subtotal>
//            <Total currencyID="USD">142.0</Total>
//            <ExternalTransaction>
//                <ExternalTransactionID>SIS</ExternalTransactionID>
//                <ExternalTransactionTime>2009-09-08T08:53:32.000Z</ExternalTransactionTime>
//                <FeeOrCreditAmount currencyID="USD">0.0</FeeOrCreditAmount>
//                <PaymentOrRefundAmount currencyID="USD">142.0</PaymentOrRefundAmount>
//            </ExternalTransaction>
//            <TransactionArray>
//                <Transaction>
//                    <Buyer>
//                        <Email>apache.ofbiz@gmail.com</Email>
//                    </Buyer>
//                    <ShippingDetails>
//                        <SellingManagerSalesRecordNumber>101</SellingManagerSalesRecordNumber>
//                    </ShippingDetails>
//                    <Item>
//                        <ItemID>110040779968</ItemID>
//                        <SKU>GZ-9290</SKU>
//                    </Item>
//                    <QuantityPurchased>1</QuantityPurchased>
//                    <Status>
//                        <PaymentHoldStatus>None</PaymentHoldStatus>
//                    </Status>
//                    <TransactionID>0</TransactionID>
//                    <TransactionPrice currencyID="USD">102.0</TransactionPrice>
//                </Transaction>
//                <Transaction>
//                    <Buyer>
//                        <Email>apache.ofbiz@gmail.com</Email>
//                    </Buyer>
//                    <ShippingDetails>
//                        <SellingManagerSalesRecordNumber>102</SellingManagerSalesRecordNumber>
//                    </ShippingDetails>
//                    <Item>
//                        <ItemID>110040780249</ItemID>
//                        <SKU>GZ-1001</SKU>
//                    </Item>
//                    <QuantityPurchased>1</QuantityPurchased>
//                    <Status>
//                        <PaymentHoldStatus>None</PaymentHoldStatus>
//                    </Status>
//                    <TransactionID>0</TransactionID>
//                    <TransactionPrice currencyID="USD">30.0</TransactionPrice>
//                </Transaction>
//            </TransactionArray>
//            <BuyerUserID>apacheofbiz</BuyerUserID>
//            <PaidTime>2009-09-08T14:04:17.000Z</PaidTime>
//            <ShippedTime>2009-09-08T14:07:09.000Z</ShippedTime>
//            <IntegratedMerchantCreditCardEnabled>false</IntegratedMerchantCreditCardEnabled>
//        </Order>
//    </OrderArray>
//</GetOrdersResponse>


    private static List<Map<String, Object>> readGetOrdersResponse(String responseMsg, Locale locale, String productStoreId, Delegator delegator, LocalDispatcher dispatcher, StringBuffer errorMessage, GenericValue userLogin) {
        List<Map<String, Object>> fetchedOrders = new LinkedList<Map<String,Object>>();
        try {
            Document docResponse = UtilXml.readXmlDocument(responseMsg, true);
            //Debug.logInfo("The generated string is ======= " + UtilXml.writeXmlDocument(docResponse), module);
            Element elemResponse = docResponse.getDocumentElement();
            String ack = UtilXml.childElementValue(elemResponse, "Ack", "Failure");

            int totalOrders = 0;

            if (ack != null && "Success".equals(ack)) {
                List<? extends Element> orderArrays = UtilXml.childElementList(elemResponse, "OrderArray");
                if (UtilValidate.isNotEmpty(orderArrays)) {
                    totalOrders = orderArrays.size();
                }
                if (totalOrders > 0) {
                    // retrieve transaction array
                    Iterator<? extends Element> orderArraysElemIter = orderArrays.iterator();
                    while (orderArraysElemIter.hasNext()) {
                        Element orderArraysElement = orderArraysElemIter.next();

                        // retrieve transaction
                        List<? extends Element> orders = UtilXml.childElementList(orderArraysElement, "Order");
                        Iterator<? extends Element> ordersElemIter = orders.iterator();

                        while (ordersElemIter.hasNext()) {
                            Map<String, Object> orderCtx = new HashMap<String, Object>();
                            Element ordersElement = ordersElemIter.next();
                            String externalOrderId = UtilXml.childElementValue(ordersElement, "OrderID");
                            orderCtx.put("externalId", "EBO_" + externalOrderId);
                            GenericValue orderExist = externalOrderExists(delegator, (String)orderCtx.get("externalId"));
                            if (orderExist != null) {
                                orderCtx.put("orderId", orderExist.get("orderId"));
                            } else {
                                orderCtx.put("orderId", "");
                            }

                            orderCtx.put("amountPaid", UtilXml.childElementValue(ordersElement, "Total", "0"));
                            String createdDate = UtilXml.childElementValue(ordersElement, "CreatedTime");
                            if (UtilValidate.isNotEmpty(createdDate)) {
                                createdDate = EbayHelper.convertDate(createdDate, "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", "yyyy-MM-dd HH:mm:ss.SSS");
                                orderCtx.put("createdDate", createdDate);
                            }
                            orderCtx.put("paidTime", UtilXml.childElementValue(ordersElement, "PaidTime"));
                            orderCtx.put("shippedTime", UtilXml.childElementValue(ordersElement, "ShippedTime"));
                            orderCtx.put("ebayUserIdBuyer", UtilXml.childElementValue(ordersElement, "BuyerUserID"));
                            orderCtx.put("productStoreId", productStoreId);

                            // Retrieve shipping address
                            Map<String, Object> shippingAddressCtx = new HashMap<String, Object>();
                            List<? extends Element> shippingAddressList = UtilXml.childElementList(ordersElement, "ShippingAddress");
                            Iterator<? extends Element> shippingAddressElemIter = shippingAddressList.iterator();
                            while (shippingAddressElemIter.hasNext()) {
                                Element shippingAddressElement = shippingAddressElemIter.next();
                                shippingAddressCtx.put("buyerName", UtilXml.childElementValue(shippingAddressElement, "Name"));
                                shippingAddressCtx.put("shippingAddressStreet1", UtilXml.childElementValue(shippingAddressElement, "Street1"));
                                shippingAddressCtx.put("shippingAddressStreet2", UtilXml.childElementValue(shippingAddressElement, "Street2"));
                                shippingAddressCtx.put("shippingAddressCityName", UtilXml.childElementValue(shippingAddressElement, "CityName"));
                                shippingAddressCtx.put("shippingAddressStateOrProvince", UtilXml.childElementValue(shippingAddressElement, "StateOrProvince"));
                                shippingAddressCtx.put("shippingAddressCountry", UtilXml.childElementValue(shippingAddressElement, "Country"));
                                shippingAddressCtx.put("shippingAddressCountryName", UtilXml.childElementValue(shippingAddressElement, "CountryName"));
                                shippingAddressCtx.put("shippingAddressPhone", UtilXml.childElementValue(shippingAddressElement, "Phone"));
                                shippingAddressCtx.put("shippingAddressPostalCode", UtilXml.childElementValue(shippingAddressElement, "PostalCode"));
                            }
                            orderCtx.put("shippingAddressCtx", shippingAddressCtx);

                            if (UtilValidate.isEmpty(shippingAddressCtx)) {
                                String shippingAddressMissingMsg = "Shipping Address is missing for eBay Order ID (" + externalOrderId + ")";
                                orderImportFailureMessageList.add(shippingAddressMissingMsg);
                            }

                            // Retrieve shipping service selected
                            Map<String, Object> shippingServiceSelectedCtx = new HashMap<String, Object>();
                            List<? extends Element> shippingServiceSelectedList = UtilXml.childElementList(ordersElement, "ShippingServiceSelected");
                            Iterator<? extends Element> shippingServiceSelectedElemIter = shippingServiceSelectedList.iterator();
                            while (shippingServiceSelectedElemIter.hasNext()) {
                                Element shippingServiceSelectedElement = shippingServiceSelectedElemIter.next();
                                shippingServiceSelectedCtx.put("shippingService", UtilXml.childElementValue(shippingServiceSelectedElement, "ShippingService"));
                                shippingServiceSelectedCtx.put("shippingServiceCost", UtilXml.childElementValue(shippingServiceSelectedElement, "ShippingServiceCost", "0"));
                                String insuranceCost = UtilXml.childElementValue(shippingServiceSelectedElement, "ShippingInsuranceCost", "0");
                                if (UtilValidate.isNotEmpty(insuranceCost)) {
                                    shippingServiceSelectedCtx.put("shippingTotalAdditionalCost", insuranceCost);
                                }
                            }
                            orderCtx.put("shippingServiceSelectedCtx", shippingServiceSelectedCtx);

                            if (UtilValidate.isEmpty(shippingServiceSelectedCtx.get("shippingService").toString())) {
                                String shippingServiceMissingMsg = "Shipping Method is missing for eBay Order ID (" + externalOrderId + ")";
                                orderImportFailureMessageList.add(shippingServiceMissingMsg);
                            }

                            // Retrieve shipping details
                            Map<String, Object> shippingDetailsCtx = new HashMap<String, Object>();
                            List<? extends Element> shippingDetailsList = UtilXml.childElementList(ordersElement, "ShippingDetails");
                            Iterator<? extends Element> shippingDetailsElemIter = shippingDetailsList.iterator();
                            while (shippingDetailsElemIter.hasNext()) {
                                Element shippingDetailsElement = shippingDetailsElemIter.next();
                                shippingDetailsCtx.put("insuranceFee", UtilXml.childElementValue(shippingDetailsElement, "InsuranceFee", "0"));
                                shippingDetailsCtx.put("insuranceOption", UtilXml.childElementValue(shippingDetailsElement, "InsuranceOption"));
                                shippingDetailsCtx.put("insuranceWanted", UtilXml.childElementValue(shippingDetailsElement, "InsuranceWanted", "false"));

                                // Retrieve sales Tax
                                List<? extends Element> salesTaxList = UtilXml.childElementList(shippingDetailsElement, "SalesTax");
                                Iterator<? extends Element> salesTaxElemIter = salesTaxList.iterator();
                                while (salesTaxElemIter.hasNext()) {
                                    Element salesTaxElement = salesTaxElemIter.next();
                                    shippingDetailsCtx.put("salesTaxAmount", UtilXml.childElementValue(salesTaxElement, "SalesTaxAmount", "0"));
                                    shippingDetailsCtx.put("salesTaxPercent", UtilXml.childElementValue(salesTaxElement, "SalesTaxPercent", "0"));
                                    shippingDetailsCtx.put("salesTaxState", UtilXml.childElementValue(salesTaxElement, "SalesTaxState", "0"));
                                    shippingDetailsCtx.put("shippingIncludedInTax", UtilXml.childElementValue(salesTaxElement, "ShippingIncludedInTax", "false"));
                                    }
                                }
                            orderCtx.put("shippingDetailsCtx", shippingDetailsCtx);

                            // Retrieve checkout status
                            Map<String, Object> checkoutStatusCtx = new HashMap<String, Object>();
                            List<? extends Element> checkoutStatusList = UtilXml.childElementList(ordersElement, "CheckoutStatus");
                            Iterator<? extends Element> checkoutStatusElemIter = checkoutStatusList.iterator();
                            while (checkoutStatusElemIter.hasNext()) {
                                Element statusElement = checkoutStatusElemIter.next();
                                checkoutStatusCtx.put("eBayPaymentStatus", UtilXml.childElementValue(statusElement, "eBayPaymentStatus"));
                                checkoutStatusCtx.put("paymentMethodUsed", UtilXml.childElementValue(statusElement, "PaymentMethod"));
                                checkoutStatusCtx.put("completeStatus", UtilXml.childElementValue(statusElement, "Status"));
                            }
                            orderCtx.put("checkoutStatusCtx", checkoutStatusCtx);

                            // Retrieve external transaction
                            Map<String, Object> externalTransactionCtx = new HashMap<String, Object>();
                            List<? extends Element> externalTransactionList = UtilXml.childElementList(ordersElement, "ExternalTransaction");
                            Iterator<? extends Element> externalTransactionElemIter = externalTransactionList.iterator();
                            while (externalTransactionElemIter.hasNext()) {
                                Element externalTransactionElement = externalTransactionElemIter.next();
                                externalTransactionCtx.put("externalTransactionID", UtilXml.childElementValue(externalTransactionElement, "ExternalTransactionID"));
                                externalTransactionCtx.put("externalTransactionTime", UtilXml.childElementValue(externalTransactionElement, "ExternalTransactionTime"));
                                externalTransactionCtx.put("feeOrCreditAmount", UtilXml.childElementValue(externalTransactionElement, "FeeOrCreditAmount", "0"));
                                externalTransactionCtx.put("paymentOrRefundAmount", UtilXml.childElementValue(externalTransactionElement, "PaymentOrRefundAmount", "0"));
                            }
                            orderCtx.put("externalTransactionCtx", externalTransactionCtx);

                            // Retrieve Transactions Array --> Transactions | Order Items
                            List<Map<String, Object>> orderItemList = new LinkedList<Map<String,Object>>();
                            String buyersEmailId = null;
                            List<? extends Element> transactionArrayList = UtilXml.childElementList(ordersElement, "TransactionArray");
                            Iterator<? extends Element> transactionArrayElemIter = transactionArrayList.iterator();
                            while (transactionArrayElemIter.hasNext()) {
                                Element transactionArrayElement = transactionArrayElemIter.next();

                                boolean buyerEmailExists = false;
                                List<? extends Element> transactionList = UtilXml.childElementList(transactionArrayElement, "Transaction");
                                Iterator<? extends Element> transactionElemIter = transactionList.iterator();
                                while (transactionElemIter.hasNext()) {
                                    Map<String, Object> transactionCtx = new HashMap<String, Object>();
                                    Element transactionElement = transactionElemIter.next();

                                    // Retrieve Buyer email
                                    if (!buyerEmailExists) {
                                        List<? extends Element> buyerList = UtilXml.childElementList(transactionElement, "Buyer");
                                        Iterator<? extends Element> buyerElemIter = buyerList.iterator();
                                        while (buyerElemIter.hasNext()) {
                                            Element buyerElement = buyerElemIter.next();
                                            buyersEmailId = UtilXml.childElementValue(buyerElement, "Email");
                                            if (UtilValidate.isNotEmpty(buyersEmailId)) {
                                                buyerEmailExists = true;
                                            }
                                        }
                                    }

                                    // Retrieve Order Item info
                                    List<? extends Element> itemList = UtilXml.childElementList(transactionElement, "Item");
                                    Iterator<? extends Element> itemElemIter = itemList.iterator();
                                    while (itemElemIter.hasNext()) {
                                        Element itemElement = itemElemIter.next();
                                        transactionCtx.put("goodIdentificationIdValue", UtilXml.childElementValue(itemElement, "ItemID"));
                                        transactionCtx.put("productId", UtilXml.childElementValue(itemElement, "SKU"));
                                    }
                                    transactionCtx.put("quantity", UtilXml.childElementValue(transactionElement, "QuantityPurchased"));
                                    transactionCtx.put("transactionId", UtilXml.childElementValue(transactionElement, "TransactionID"));
                                    transactionCtx.put("transactionPrice", UtilXml.childElementValue(transactionElement, "TransactionPrice"));
                                    orderItemList.add(transactionCtx);
                                }
                            }
                            orderCtx.put("orderItemList", orderItemList);
                            if (UtilValidate.isNotEmpty(buyersEmailId)) {
                                orderCtx.put("emailBuyer", buyersEmailId);
                            }
                            orderCtx.put("userLogin", userLogin);
                            orderCtx.put("isEbayOrder", "Y");
                            orderCtx.put("isEbayTransaction", "");

                            //Map<String, Object> result = dispatcher.runSync("importEbayOrders", orderCtx);
                            fetchedOrders.add(orderCtx);
                        }
                    }
                }
               //Debug.logInfo("The generated string is ======= " + fetchedOrders.toString(), module);
            } else {
                List<? extends Element> errorList = UtilXml.childElementList(elemResponse, "Errors");
                Iterator<? extends Element> errorElemIter = errorList.iterator();
                while (errorElemIter.hasNext()) {
                    Element errorElement = errorElemIter.next();
                    errorMessage.append(UtilXml.childElementValue(errorElement, "ShortMessage", ""));
                }
            }
        } catch (Exception e) {
            Debug.logError("Exception during read response from Ebay", module);
        }
        return fetchedOrders;
    }

    private static List<Map<String, Object>> readGetSellerTransactionResponse(String responseMsg, Locale locale, String productStoreId, Delegator delegator, LocalDispatcher dispatcher, StringBuffer errorMessage, GenericValue userLogin) {
        List<Map<String, Object>> fetchedOrders = new LinkedList<Map<String,Object>>();
        try {
            Document docResponse = UtilXml.readXmlDocument(responseMsg, true);
            //Debug.logInfo("The generated string is ======= " + UtilXml.writeXmlDocument(docResponse), module);
            Element elemResponse = docResponse.getDocumentElement();
            String ack = UtilXml.childElementValue(elemResponse, "Ack", "Failure");
            List<? extends Element> paginationList = UtilXml.childElementList(elemResponse, "PaginationResult");

            int totalOrders = 0;
            Iterator<? extends Element> paginationElemIter = paginationList.iterator();
            while (paginationElemIter.hasNext()) {
                Element paginationElement = paginationElemIter.next();
                String totalNumberOfEntries = UtilXml.childElementValue(paginationElement, "TotalNumberOfEntries", "0");
                totalOrders = Integer.valueOf(totalNumberOfEntries);
            }

            if (ack != null && "Success".equals(ack)) {
                if (totalOrders > 0) {
                    // retrieve transaction array
                    List<? extends Element> transactions = UtilXml.childElementList(elemResponse, "TransactionArray");
                    Iterator<? extends Element> transactionsElemIter = transactions.iterator();
                    while (transactionsElemIter.hasNext()) {
                        Element transactionsElement = transactionsElemIter.next();

                        // retrieve transaction
                        List<? extends Element> transaction = UtilXml.childElementList(transactionsElement, "Transaction");
                        Iterator<? extends Element> transactionElemIter = transaction.iterator();
                        while (transactionElemIter.hasNext()) {
                            Element transactionElement = transactionElemIter.next();
                            Map<String, Object> orderCtx = new HashMap<String, Object>();

                            orderCtx.put("amountPaid", UtilXml.childElementValue(transactionElement, "AmountPaid", "0"));
                            String createdDate = UtilXml.childElementValue(transactionElement, "CreatedDate");
                            if (UtilValidate.isNotEmpty(createdDate)) {
                                createdDate = EbayHelper.convertDate(createdDate, "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", "yyyy-MM-dd HH:mm:ss.SSS");
                                orderCtx.put("createdDate", createdDate);
                            }
                            orderCtx.put("paidTime", UtilXml.childElementValue(transactionElement, "PaidTime"));
                            orderCtx.put("shippedTime", UtilXml.childElementValue(transactionElement, "ShippedTime"));
                            orderCtx.put("productStoreId", productStoreId);

                            // if any transaction contains order then add it in the list and give it to GetOrders request.
                            List<? extends Element> containingOrders = UtilXml.childElementList(transactionElement, "ContainingOrder");
                            Iterator<? extends Element> containingOrdersIter = containingOrders.iterator();
                            while (containingOrdersIter.hasNext()) {
                                Element containingOrdersElement = containingOrdersIter.next();
                                String orderId = UtilXml.childElementValue(containingOrdersElement, "OrderID");
                                if (getSellerTransactionsContainingOrderList != null && ! getSellerTransactionsContainingOrderList.contains(orderId)) {
                                    getSellerTransactionsContainingOrderList.add(orderId);
                                }
                            }
                            if (UtilValidate.isNotEmpty(containingOrders)) {
                                continue;
                            }

                            // retrieve buyer
                            Map<String, Object> shippingAddressCtx = new HashMap<String, Object>();
                            Map<String, Object> buyerCtx = new HashMap<String, Object>();
                            List<? extends Element> buyer = UtilXml.childElementList(transactionElement, "Buyer");
                            Iterator<? extends Element> buyerElemIter = buyer.iterator();
                            while (buyerElemIter.hasNext()) {
                                Element buyerElement = buyerElemIter.next();
                                buyerCtx.put("emailBuyer", UtilXml.childElementValue(buyerElement, "Email", ""));
                                buyerCtx.put("eiasTokenBuyer", UtilXml.childElementValue(buyerElement, "EIASToken", ""));
                                buyerCtx.put("ebayUserIdBuyer", UtilXml.childElementValue(buyerElement, "UserID", ""));

                                // retrieve buyer information
                                List<? extends Element> buyerInfo = UtilXml.childElementList(buyerElement, "BuyerInfo");
                                Iterator<? extends Element> buyerInfoElemIter = buyerInfo.iterator();
                                while (buyerInfoElemIter.hasNext()) {
                                    Element buyerInfoElement = buyerInfoElemIter.next();

                                    // retrieve shipping address
                                    List<? extends Element> shippingAddressInfo = UtilXml.childElementList(buyerInfoElement, "ShippingAddress");
                                    Iterator<? extends Element> shippingAddressElemIter = shippingAddressInfo.iterator();
                                    while (shippingAddressElemIter.hasNext()) {
                                        Element shippingAddressElement = shippingAddressElemIter.next();
                                        shippingAddressCtx.put("buyerName", UtilXml.childElementValue(shippingAddressElement, "Name", ""));
                                        shippingAddressCtx.put("shippingAddressStreet", UtilXml.childElementValue(shippingAddressElement, "Street", ""));
                                        shippingAddressCtx.put("shippingAddressStreet1", UtilXml.childElementValue(shippingAddressElement, "Street1", ""));
                                        shippingAddressCtx.put("shippingAddressStreet2", UtilXml.childElementValue(shippingAddressElement, "Street2", ""));
                                        shippingAddressCtx.put("shippingAddressCityName", UtilXml.childElementValue(shippingAddressElement, "CityName", ""));
                                        shippingAddressCtx.put("shippingAddressStateOrProvince", UtilXml.childElementValue(shippingAddressElement, "StateOrProvince", ""));
                                        shippingAddressCtx.put("shippingAddressCountry", UtilXml.childElementValue(shippingAddressElement, "Country", ""));
                                        shippingAddressCtx.put("shippingAddressCountryName", UtilXml.childElementValue(shippingAddressElement, "CountryName", ""));
                                        shippingAddressCtx.put("shippingAddressPhone", UtilXml.childElementValue(shippingAddressElement, "Phone", ""));
                                        shippingAddressCtx.put("shippingAddressPostalCode", UtilXml.childElementValue(shippingAddressElement, "PostalCode", ""));
                                    }
                                }
                            }
                            orderCtx.put("buyerCtx", buyerCtx);
                            orderCtx.put("shippingAddressCtx", shippingAddressCtx);

                            // retrieve shipping service selected
                            Map<String, Object> shippingServiceSelectedCtx = new HashMap<String, Object>();
                            List<? extends Element> shippingServiceSelected = UtilXml.childElementList(transactionElement, "ShippingServiceSelected");
                            Iterator<? extends Element> shippingServiceSelectedElemIter = shippingServiceSelected.iterator();
                            while (shippingServiceSelectedElemIter.hasNext()) {
                                Element shippingServiceSelectedElement = shippingServiceSelectedElemIter.next();
                                shippingServiceSelectedCtx.put("shippingService", UtilXml.childElementValue(shippingServiceSelectedElement, "ShippingService", ""));
                                shippingServiceSelectedCtx.put("shippingServiceCost", UtilXml.childElementValue(shippingServiceSelectedElement, "ShippingServiceCost", "0"));

                                String incuranceCost = UtilXml.childElementValue(shippingServiceSelectedElement, "ShippingInsuranceCost", "0");
                                String additionalCost = UtilXml.childElementValue(shippingServiceSelectedElement, "ShippingServiceAdditionalCost", "0");
                                String surchargeCost = UtilXml.childElementValue(shippingServiceSelectedElement, "ShippingSurcharge", "0");

                                double shippingInsuranceCost = 0;
                                double shippingServiceAdditionalCost = 0;
                                double shippingSurcharge = 0;

                                if (UtilValidate.isNotEmpty(incuranceCost)) {
                                    shippingInsuranceCost = new Double(incuranceCost).doubleValue();
                                }
                                if (UtilValidate.isNotEmpty(additionalCost)) {
                                    shippingServiceAdditionalCost = new Double(additionalCost).doubleValue();
                                }
                                if (UtilValidate.isNotEmpty(surchargeCost)) {
                                    shippingSurcharge = new Double(surchargeCost).doubleValue();
                                }
                                double shippingTotalAdditionalCost = shippingInsuranceCost + shippingServiceAdditionalCost + shippingSurcharge;
                                String totalAdditionalCost = new Double(shippingTotalAdditionalCost).toString();
                                shippingServiceSelectedCtx.put("shippingTotalAdditionalCost", totalAdditionalCost);
                            }
                            orderCtx.put("shippingServiceSelectedCtx", shippingServiceSelectedCtx);

                            // retrieve shipping details
                            Map<String, Object> shippingDetailsCtx = new HashMap<String, Object>();
                            List<? extends Element> shippingDetails = UtilXml.childElementList(transactionElement, "ShippingDetails");
                            Iterator<? extends Element> shippingDetailsElemIter = shippingDetails.iterator();
                            while (shippingDetailsElemIter.hasNext()) {
                                Element shippingDetailsElement = shippingDetailsElemIter.next();
                                shippingDetailsCtx.put("insuranceFee", UtilXml.childElementValue(shippingDetailsElement, "InsuranceFee", "0"));
                                shippingDetailsCtx.put("insuranceOption", UtilXml.childElementValue(shippingDetailsElement, "InsuranceOption", ""));
                                shippingDetailsCtx.put("insuranceWanted", UtilXml.childElementValue(shippingDetailsElement, "InsuranceWanted", "false"));

                                // retrieve sales Tax
                                List<? extends Element> salesTax = UtilXml.childElementList(shippingDetailsElement, "SalesTax");
                                Iterator<? extends Element> salesTaxElemIter = salesTax.iterator();
                                while (salesTaxElemIter.hasNext()) {
                                    Element salesTaxElement = salesTaxElemIter.next();
                                    shippingDetailsCtx.put("salesTaxAmount", UtilXml.childElementValue(salesTaxElement, "SalesTaxAmount", "0"));
                                    shippingDetailsCtx.put("salesTaxPercent", UtilXml.childElementValue(salesTaxElement, "SalesTaxPercent", "0"));
                                    shippingDetailsCtx.put("salesTaxState", UtilXml.childElementValue(salesTaxElement, "SalesTaxState", "0"));
                                    shippingDetailsCtx.put("shippingIncludedInTax", UtilXml.childElementValue(salesTaxElement, "ShippingIncludedInTax", "false"));
                                }

                                // retrieve tax table
                                List<? extends Element> taxTable = UtilXml.childElementList(shippingDetailsElement, "TaxTable");
                                Iterator<? extends Element> taxTableElemIter = taxTable.iterator();
                                while (taxTableElemIter.hasNext()) {
                                    Element taxTableElement = taxTableElemIter.next();

                                    List<? extends Element> taxJurisdiction = UtilXml.childElementList(taxTableElement, "TaxJurisdiction");
                                    Iterator<? extends Element> taxJurisdictionElemIter = taxJurisdiction.iterator();
                                    while (taxJurisdictionElemIter.hasNext()) {
                                        Element taxJurisdictionElement = taxJurisdictionElemIter.next();

                                        shippingDetailsCtx.put("jurisdictionID", UtilXml.childElementValue(taxJurisdictionElement, "JurisdictionID", ""));
                                        shippingDetailsCtx.put("jurisdictionSalesTaxPercent", UtilXml.childElementValue(taxJurisdictionElement, "SalesTaxPercent", "0"));
                                        shippingDetailsCtx.put("jurisdictionShippingIncludedInTax", UtilXml.childElementValue(taxJurisdictionElement, "ShippingIncludedInTax", "0"));
                                    }
                                }
                            }
                            orderCtx.put("shippingDetailsCtx", shippingDetailsCtx);

                            // retrieve status
                            Map<String, Object> checkoutStatusCtx = new HashMap<String, Object>();
                            List<? extends Element> status = UtilXml.childElementList(transactionElement, "Status");
                            Iterator<? extends Element> statusElemIter = status.iterator();
                            while (statusElemIter.hasNext()) {
                                Element statusElement = statusElemIter.next();
                                checkoutStatusCtx.put("eBayPaymentStatus", UtilXml.childElementValue(statusElement, "eBayPaymentStatus", ""));
                                checkoutStatusCtx.put("checkoutStatus", UtilXml.childElementValue(statusElement, "CheckoutStatus", ""));
                                checkoutStatusCtx.put("paymentMethodUsed", UtilXml.childElementValue(statusElement, "PaymentMethodUsed", ""));
                                checkoutStatusCtx.put("completeStatus", UtilXml.childElementValue(statusElement, "CompleteStatus", ""));
                                checkoutStatusCtx.put("buyerSelectedShipping", UtilXml.childElementValue(statusElement, "BuyerSelectedShipping", ""));
                            }
                            orderCtx.put("checkoutStatusCtx", checkoutStatusCtx);

                            // retrieve external transaction
                            Map<String, Object> externalTransactionCtx = new HashMap<String, Object>();
                            List<? extends Element> externalTransaction = UtilXml.childElementList(transactionElement, "ExternalTransaction");
                            Iterator<? extends Element> externalTransactionElemIter = externalTransaction.iterator();
                            while (externalTransactionElemIter.hasNext()) {
                                Element externalTransactionElement = externalTransactionElemIter.next();
                                externalTransactionCtx.put("externalTransactionID", UtilXml.childElementValue(externalTransactionElement, "ExternalTransactionID", ""));
                                externalTransactionCtx.put("externalTransactionTime", UtilXml.childElementValue(externalTransactionElement, "ExternalTransactionTime", ""));
                                externalTransactionCtx.put("feeOrCreditAmount", UtilXml.childElementValue(externalTransactionElement, "FeeOrCreditAmount", "0"));
                                externalTransactionCtx.put("paymentOrRefundAmount", UtilXml.childElementValue(externalTransactionElement, "PaymentOrRefundAmount", "0"));
                            }
                            orderCtx.put("externalTransactionCtx", externalTransactionCtx);

                            String quantityPurchased = UtilXml.childElementValue(transactionElement, "QuantityPurchased", "");
                            // retrieve item
                            List<Map<String, Object>> orderItemList = new LinkedList<Map<String,Object>>();
                            String itemId = "";
                            List<? extends Element> item = UtilXml.childElementList(transactionElement, "Item");
                            Iterator<? extends Element> itemElemIter = item.iterator();
                            while (itemElemIter.hasNext()) {
                                Map<String, Object> orderItemCtx = new HashMap<String, Object>();
                                Element itemElement = itemElemIter.next();
                                itemId = UtilXml.childElementValue(itemElement, "ItemID", "");
                                orderItemCtx.put("paymentMethods", UtilXml.childElementValue(itemElement, "PaymentMethods", ""));
                                orderItemCtx.put("quantity", quantityPurchased);
                                orderItemCtx.put("startPrice", UtilXml.childElementValue(itemElement, "StartPrice", "0"));
                                orderItemCtx.put("title", UtilXml.childElementValue(itemElement, "Title", ""));

                                String productId = UtilXml.childElementValue(itemElement, "SKU", "");
                                if (UtilValidate.isEmpty(productId)) {
                                    productId = UtilXml.childElementValue(itemElement, "ApplicationData", "");
                                    if (UtilValidate.isEmpty(productId)) {
                                         productId = EbayHelper.retrieveProductIdFromTitle(delegator, (String) orderItemCtx.get("title"));
                                    }
                                }
                                orderItemCtx.put("productId", productId);
                                // retrieve selling status
                                List<? extends Element> sellingStatus = UtilXml.childElementList(itemElement, "SellingStatus");
                                Iterator<? extends Element> sellingStatusitemElemIter = sellingStatus.iterator();
                                while (sellingStatusitemElemIter.hasNext()) {
                                    Element sellingStatusElement = sellingStatusitemElemIter.next();
                                    orderItemCtx.put("amount", UtilXml.childElementValue(sellingStatusElement, "CurrentPrice", "0"));
                                    orderItemCtx.put("quantitySold", UtilXml.childElementValue(sellingStatusElement, "QuantitySold", "0"));
                                    orderItemCtx.put("listingStatus", UtilXml.childElementValue(sellingStatusElement, "ListingStatus", ""));
                                }
                                orderItemList.add(orderItemCtx);
                            }
                            orderCtx.put("orderItemList", orderItemList);

                            // retrieve transactionId
                            String transactionId = UtilXml.childElementValue(transactionElement, "TransactionID", "");

                            // set the externalId
                            if ("0".equals(transactionId)) {
                                // this is a Chinese Auction: ItemID is used to uniquely identify the transaction
                                orderCtx.put("externalId", "EBI_" + itemId);
                            } else {
                                orderCtx.put("externalId", "EBT_" + transactionId);
                            }
                            orderCtx.put("transactionId", "EBI_" + itemId);

                            GenericValue orderExist = externalOrderExists(delegator, (String)orderCtx.get("externalId"));
                            if (orderExist != null) {
                                orderCtx.put("orderId", orderExist.get("orderId"));
                            } else {
                                orderCtx.put("orderId", "");
                            }

                            // retrieve transaction price
                            orderCtx.put("transactionPrice", UtilXml.childElementValue(transactionElement, "TransactionPrice", "0"));

                            if (UtilValidate.isEmpty(shippingServiceSelectedCtx.get("shippingService").toString())) {
                                String shippingServiceMissingMsg = "Shipping Method is missing for eBay Order ID (" + orderCtx.get("externalId").toString() + ")";
                                orderImportFailureMessageList.add(shippingServiceMissingMsg);
                            }

                            // Lets put emailAddress & userId on the root similiar to that of GetOrders request
                            orderCtx.put("emailBuyer", buyerCtx.get("emailBuyer").toString());
                            orderCtx.put("ebayUserIdBuyer", buyerCtx.get("ebayUserIdBuyer").toString());

                            orderCtx.put("userLogin", userLogin);
                            orderCtx.put("isEbayTransaction", "Y");
                            orderCtx.put("isEbayOrder", "");

                            // Now finally put the root map in the fetched orders list.
                            fetchedOrders.add(orderCtx);
                        }
                    }
                }
            } else {
                List<? extends Element> errorList = UtilXml.childElementList(elemResponse, "Errors");
                Iterator<? extends Element> errorElemIter = errorList.iterator();
                while (errorElemIter.hasNext()) {
                    Element errorElement = errorElemIter.next();
                    errorMessage.append(UtilXml.childElementValue(errorElement, "ShortMessage", ""));
                }
            }
        } catch (Exception e) {
            Debug.logError("Exception during read response from Ebay", module);
        }
        return fetchedOrders;
    }

    private static List<String> readGetMyeBaySellingResponse(String responseMsg, Locale locale, String productStoreId, Delegator delegator, LocalDispatcher dispatcher, StringBuffer errorMessage, GenericValue userLogin) {
        List<String> fetchDeletedOrdersAndTransactions = new LinkedList<String>();
        try {
            Document docResponse = UtilXml.readXmlDocument(responseMsg, true);
            //Debug.logInfo("The generated string is ======= " + UtilXml.writeXmlDocument(docResponse), module);
            Element elemResponse = docResponse.getDocumentElement();
            String ack = UtilXml.childElementValue(elemResponse, "Ack", "Failure");

            if (ack != null && "Success".equals(ack)) {
                // retrieve transaction array
                List<? extends Element> deletedFromSoldList = UtilXml.childElementList(elemResponse, "DeletedFromSoldList");
                Iterator<? extends Element> deletedFromSoldElemIter = deletedFromSoldList.iterator();
                while (deletedFromSoldElemIter.hasNext()) {
                    Element deletedFromSoldElement = deletedFromSoldElemIter.next();
                    // retrieve transaction
                    List<? extends Element> orderTransactionArrayList = UtilXml.childElementList(deletedFromSoldElement, "OrderTransactionArray");
                    Iterator<? extends Element> orderTransactionArrayElemIter = orderTransactionArrayList.iterator();
                    while (orderTransactionArrayElemIter.hasNext()) {
                        Element orderTransactionArrayElement = orderTransactionArrayElemIter.next();
                        List<? extends Element> orderTransactionList = UtilXml.childElementList(orderTransactionArrayElement, "OrderTransaction");
                        Iterator<? extends Element> orderTransactionElemIter = orderTransactionList.iterator();
                        while (orderTransactionElemIter.hasNext()) {
                            Element orderTransactionElement = orderTransactionElemIter.next();

                            List<? extends Element> sellerOrderList = UtilXml.childElementList(orderTransactionElement, "Order");
                            Iterator<? extends Element> sellerOrderElemIter = sellerOrderList.iterator();
                            while (sellerOrderElemIter.hasNext()) {
                                Element sellerOrderElement = sellerOrderElemIter.next();
                                String orderId = UtilXml.childElementValue(sellerOrderElement, "OrderID");
                                if (UtilValidate.isNotEmpty(orderId)) {
                                    fetchDeletedOrdersAndTransactions.add(orderId);
                                }
                            }
                            List<? extends Element> transactionList = UtilXml.childElementList(orderTransactionElement, "Transaction");
                            Iterator<? extends Element> transactionElemIter = transactionList.iterator();
                            while (transactionElemIter.hasNext()) {
                                Element transactionElement = transactionElemIter.next();

                                List<? extends Element> itemList = UtilXml.childElementList(transactionElement, "Item");
                                Iterator<? extends Element> itemElemIter = itemList.iterator();
                                while (itemElemIter.hasNext()) {
                                    Element itemElement = itemElemIter.next();
                                    String itemId = UtilXml.childElementValue(itemElement, "ItemID");
                                    if (UtilValidate.isNotEmpty(itemId)) {
                                        fetchDeletedOrdersAndTransactions.add(itemId);
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                List<? extends Element> errorList = UtilXml.childElementList(elemResponse, "Errors");
                Iterator<? extends Element> errorElemIter = errorList.iterator();
                while (errorElemIter.hasNext()) {
                    Element errorElement = errorElemIter.next();
                    errorMessage.append(UtilXml.childElementValue(errorElement, "ShortMessage", ""));
                }
            }
        } catch (Exception e) {
            Debug.logError("Exception during read response from Ebay", module);
        }
        return fetchDeletedOrdersAndTransactions;
    }

    private static Map<String, Object> createShoppingCart(Delegator delegator, LocalDispatcher dispatcher, Locale locale, Map<String, Object> context, boolean create) {
        try {
            String productStoreId = (String) context.get("productStoreId");
            GenericValue userLogin = (GenericValue) context.get("userLogin");
            String defaultCurrencyUomId = null;
            String payToPartyId = null;
            String facilityId = null;

            // Product Store is mandatory
            if (productStoreId == null) {
                return ServiceUtil.returnFailure(UtilProperties.getMessage(resource, "ordersImportFromEbay.productStoreIdIsMandatory", locale));
            } else {
                GenericValue productStore = EntityQuery.use(delegator).from("ProductStore").where("productStoreId", productStoreId).queryOne();
                if (productStore != null) {
                    defaultCurrencyUomId = productStore.getString("defaultCurrencyUomId");
                    payToPartyId = productStore.getString("payToPartyId");
                    facilityId = productStore.getString("inventoryFacilityId");
                } else {
                    return ServiceUtil.returnFailure(UtilProperties.getMessage(resource, "ordersImportFromEbay.productStoreIdIsMandatory", locale));
                }
            }

            // create a new shopping cart
            ShoppingCart cart = new ShoppingCart(delegator, productStoreId, locale, defaultCurrencyUomId);

            // set the external id with the eBay Item Id
            String externalId = (String) context.get("externalId");
            String transactionId = (String) context.get("transactionId");

            if (UtilValidate.isNotEmpty(externalId)) {
                if (externalOrderExists(delegator, externalId) != null && create) {
                    return ServiceUtil.returnFailure(UtilProperties.getMessage(resource, "ordersImportFromEbay.externalIdAlreadyExist", locale));
                }
                cart.setExternalId(externalId);
                cart.setTransactionId(transactionId);
            } else {
                return ServiceUtil.returnFailure(UtilProperties.getMessage(resource, "ordersImportFromEbay.externalIdNotAvailable", locale));
            }

            cart.setOrderType("SALES_ORDER");
            cart.setChannelType("EBAY_SALES_CHANNEL");
            cart.setUserLogin(userLogin, dispatcher);
            cart.setProductStoreId(productStoreId);

            if (UtilValidate.isNotEmpty(facilityId)) {
                cart.setFacilityId(facilityId);
            }
            String amountStr = (String) context.get("amountPaid");
            BigDecimal amountPaid = BigDecimal.ZERO;

            if (UtilValidate.isNotEmpty(amountStr)) {
                amountPaid = new BigDecimal(amountStr);
            }
            // add the payment EXT_BAY for the paid amount
            cart.addPaymentAmount("EXT_EBAY", amountPaid, externalId, null, true, false, false);

            // set the order date with the eBay created date
            Timestamp orderDate = UtilDateTime.nowTimestamp();
            if (UtilValidate.isNotEmpty(context.get("createdDate"))) {
                orderDate = UtilDateTime.toTimestamp((String) context.get("createdDate"));
            }
            cart.setOrderDate(orderDate);
            // Before import the order from eBay to OFBiz is mandatory that the payment has be received
            String paidTime = (String) context.get("paidTime");
            if (UtilValidate.isEmpty(paidTime)) {
                return ServiceUtil.returnFailure(UtilProperties.getMessage(resource, "ordersImportFromEbay.paymentIsStillNotReceived", locale));
            }

            List<Map<String, Object>> orderItemList = UtilGenerics.checkList(context.get("orderItemList"));
            Iterator<Map<String, Object>> orderItemIter = orderItemList.iterator();
            while (orderItemIter.hasNext()) {
                Map<String, Object> orderItem = orderItemIter.next();
                addItem(cart, orderItem, dispatcher, delegator, 0);
            }

            // set partyId from
            if (UtilValidate.isNotEmpty(payToPartyId)) {
                cart.setBillFromVendorPartyId(payToPartyId);
            }
            // Apply shipping costs as order adjustment
            Map<String, Object> shippingServiceSelectedCtx =  UtilGenerics.checkMap(context.get("shippingServiceSelectedCtx"));

            String shippingCost = (String) shippingServiceSelectedCtx.get("shippingServiceCost");
            if (UtilValidate.isNotEmpty(shippingCost)) {
                double shippingAmount = new Double(shippingCost).doubleValue();
                if (shippingAmount > 0) {
                    GenericValue shippingAdjustment = EbayHelper.makeOrderAdjustment(delegator, "SHIPPING_CHARGES", cart.getOrderId(), null, null, shippingAmount, 0.0);
                    if (shippingAdjustment != null) {
                        cart.addAdjustment(shippingAdjustment);
                    }
                }
            }
            // Apply additional shipping costs as order adjustment
            String shippingTotalAdditionalCost = (String) shippingServiceSelectedCtx.get("shippingTotalAdditionalCost");
            if (UtilValidate.isNotEmpty(shippingTotalAdditionalCost)) {
                double shippingAdditionalCost = new Double(shippingTotalAdditionalCost).doubleValue();
                if (shippingAdditionalCost > 0) {
                    GenericValue shippingAdjustment = EbayHelper.makeOrderAdjustment(delegator, "MISCELLANEOUS_CHARGE", cart.getOrderId(), null, null, shippingAdditionalCost, 0.0);
                    if (shippingAdjustment != null) {
                        cart.addAdjustment(shippingAdjustment);
                    }
                }
            }
            // Apply sales tax as order adjustment
            Map<String, Object> shippingDetailsCtx = UtilGenerics.checkMap(context.get("shippingDetailsCtx"));
            String salesTaxAmount = (String) shippingDetailsCtx.get("salesTaxAmount");
            String salesTaxPercent = (String) shippingDetailsCtx.get("salesTaxPercent");
            if (UtilValidate.isNotEmpty(salesTaxAmount)) {
                double salesTaxAmountTotal = new Double(salesTaxAmount).doubleValue();
                if (salesTaxAmountTotal > 0) {
                    double salesPercent = 0.0;
                    if (UtilValidate.isNotEmpty(salesTaxPercent)) {
                        salesPercent = new Double(salesTaxPercent).doubleValue();
                    }
                    GenericValue salesTaxAdjustment = EbayHelper.makeOrderAdjustment(delegator, "SALES_TAX", cart.getOrderId(), null, null, salesTaxAmountTotal, salesPercent);
                    if (salesTaxAdjustment != null) {
                        cart.addAdjustment(salesTaxAdjustment);
                    }
                }
            }
            // order has to be created ?
            if (create) {
                Debug.logInfo("Importing new order from eBay", module);
                // set partyId to
                String partyId = null;
                String contactMechId = null;

                Map<String, Object> shippingAddressCtx = UtilGenerics.checkMap(context.get("shippingAddressCtx"));
                if (UtilValidate.isNotEmpty(shippingAddressCtx)) {
                    String buyerName = (String) shippingAddressCtx.get("buyerName");
                    String firstName = buyerName.substring(0, buyerName.indexOf(" "));
                    String lastName = buyerName.substring(buyerName.indexOf(" ")+1);

                    String country = (String) shippingAddressCtx.get("shippingAddressCountry");
                    String state = (String) shippingAddressCtx.get("shippingAddressStateOrProvince");
                    String city = (String) shippingAddressCtx.get("shippingAddressCityName");
                    EbayHelper.correctCityStateCountry(dispatcher, shippingAddressCtx, city, state, country);

                    List<GenericValue> shipInfo = 
                        PartyWorker.findMatchingPersonPostalAddresses
                            (delegator, 
                             shippingAddressCtx.get("shippingAddressStreet1").toString(),
                            (UtilValidate.isEmpty(shippingAddressCtx.get("shippingAddressStreet2")) ? null : shippingAddressCtx.get("shippingAddressStreet2").toString()), 
                             shippingAddressCtx.get("city").toString(), 
                            (UtilValidate.isEmpty(shippingAddressCtx.get("stateProvinceGeoId")) ? null : shippingAddressCtx.get("stateProvinceGeoId").toString()),
                             shippingAddressCtx.get("shippingAddressPostalCode").toString(), 
                             null, 
                             shippingAddressCtx.get("countryGeoId").toString(), 
                             firstName, 
                             null, 
                             lastName);
                    if (UtilValidate.isNotEmpty(shipInfo)) {
                        GenericValue first = EntityUtil.getFirst(shipInfo);
                        partyId = first.getString("partyId");
                        Debug.logInfo("Existing shipping address found for : (party: " + partyId + ")", module);
                    }
                }

                // If matching party not found then try to find partyId from PartyAttribute entity.
                GenericValue partyAttribute = null;
                if (UtilValidate.isNotEmpty(context.get("eiasTokenBuyer"))) {
                    partyAttribute = EntityQuery.use(delegator).from("PartyAttribute").where("attrValue", (String) context.get("eiasTokenBuyer")).queryFirst();
                    if (UtilValidate.isNotEmpty(partyAttribute)) {
                        partyId = (String) partyAttribute.get("partyId");
                    }
                }

                // if we get a party, check its contact information.
                if (UtilValidate.isNotEmpty(partyId)) {
                    Debug.logInfo("Found existing party associated to the eBay buyer: " + partyId, module);
                    GenericValue party = EntityQuery.use(delegator).from("Party").where("partyId", partyId).queryOne();

                    contactMechId = EbayHelper.setShippingAddressContactMech(dispatcher, delegator, party, userLogin, shippingAddressCtx);
                    String emailBuyer = (String) context.get("emailBuyer");
                    if (!(emailBuyer.equals("") || emailBuyer.equalsIgnoreCase("Invalid Request"))) {
                        EbayHelper.setEmailContactMech(dispatcher, delegator, party, userLogin, context);
                    }
                    EbayHelper.setPhoneContactMech(dispatcher, delegator, party, userLogin, shippingAddressCtx);
                }

                // create party if none exists already
                if (UtilValidate.isEmpty(partyId)) {
                    Debug.logInfo("Creating new party for the eBay buyer.", module);
                    partyId = EbayHelper.createCustomerParty(dispatcher, (String) shippingAddressCtx.get("buyerName"), userLogin);
                    if (UtilValidate.isEmpty(partyId)) {
                        Debug.logWarning("Using admin party for the eBay buyer.", module);
                        partyId = "admin";
                    }
                }

                // create new party's contact information
                if (UtilValidate.isEmpty(contactMechId)) {
                    Map<String, Object> buyerCtx = UtilGenerics.checkMap(context.get("buyerCtx"));
                    String eiasTokenBuyer = null;
                    if (UtilValidate.isNotEmpty(buyerCtx)) {
                        eiasTokenBuyer = (String) buyerCtx.get("eiasTokenBuyer");
                    }
                    Debug.logInfo("Creating new postal address for party: " + partyId, module);
                    contactMechId = EbayHelper.createAddress(dispatcher, partyId, userLogin, "SHIPPING_LOCATION", shippingAddressCtx);
                    if (UtilValidate.isEmpty(contactMechId)) {
                        return ServiceUtil.returnFailure(UtilProperties.getMessage(resource, "EbayStoreUnableToCreatePostalAddress", locale) + shippingAddressCtx);
                    }
                    Debug.logInfo("Created postal address: " + contactMechId, module);
                    Debug.logInfo("Creating new phone number for party: " + partyId, module);
                    EbayHelper.createPartyPhone(dispatcher, partyId, (String) shippingAddressCtx.get("shippingAddressPhone"), userLogin);
                    Debug.logInfo("Creating association to eBay buyer for party: " + partyId, module);
                    EbayHelper.createEbayCustomer(dispatcher, partyId, (String) context.get("ebayUserIdBuyer"), eiasTokenBuyer, userLogin);
                    String emailBuyer = (String) context.get("emailBuyer");
                    if (UtilValidate.isNotEmpty(emailBuyer) && !emailBuyer.equalsIgnoreCase("Invalid Request")) {
                        Debug.logInfo("Creating new email for party: " + partyId, module);
                        EbayHelper.createPartyEmail(dispatcher, partyId, emailBuyer, userLogin);
                    }
                }

                Debug.logInfo("Setting cart roles for party: " + partyId, module);
                cart.setBillToCustomerPartyId(partyId);
                cart.setPlacingCustomerPartyId(partyId);
                cart.setShipToCustomerPartyId(partyId);
                cart.setEndUserCustomerPartyId(partyId);

                Debug.logInfo("Setting contact mech in cart: " + contactMechId, module);
                cart.setAllShippingContactMechId(contactMechId);
                cart.setAllMaySplit(Boolean.FALSE);

                Debug.logInfo("Setting shipment method: " + (String) shippingServiceSelectedCtx.get("shippingService"), module);
                EbayHelper.setShipmentMethodType(cart, (String) shippingServiceSelectedCtx.get("shippingService"), productStoreId, delegator);
                cart.makeAllShipGroupInfos();

                // create the order
                Debug.logInfo("Creating CheckOutHelper.", module);
                CheckOutHelper checkout = new CheckOutHelper(dispatcher, delegator, cart);
                Debug.logInfo("Creating order.", module);
                Map<?, ?> orderCreate = checkout.createOrder(userLogin);

                if ("error".equals(orderCreate.get("responseMessage"))) {
                    List<String> errorMessageList = UtilGenerics.checkList(orderCreate.get("errorMessageList"), String.class);
                    return ServiceUtil.returnError(errorMessageList);
                }
                String orderId = (String) orderCreate.get("orderId");
                Debug.logInfo("Created order with id: " + orderId, module);

                if (UtilValidate.isNotEmpty(orderId)) {
                    String orderCreatedMsg = "Order created successfully with ID (" + orderId + ") & eBay Order ID associated with this order is (" + externalId + ").";
                    orderImportSuccessMessageList.add(orderCreatedMsg);
                }

                // approve the order
                if (UtilValidate.isNotEmpty(orderId)) {
                    Debug.logInfo("Approving order with id: " + orderId, module);
                    boolean approved = OrderChangeHelper.approveOrder(dispatcher, userLogin, orderId);
                    Debug.logInfo("Order approved with result: " + approved, module);

                    // create the payment from the preference
                    if (approved) {
                        Debug.logInfo("Creating payment for approved order.", module);
                        EbayHelper.createPaymentFromPaymentPreferences(delegator, dispatcher, userLogin, orderId, externalId, cart.getOrderDate(), amountPaid, partyId);
                        Debug.logInfo("Payment created.", module);
                    }
                }
            }
        } catch (Exception e) {
            Debug.logError("Exception in createShoppingCart: " + e.getMessage(), module);
            return ServiceUtil.returnFailure(UtilProperties.getMessage(resource, "ordersImportFromEbay.exceptionInCreateShoppingCart", locale) + ": " + e.getMessage());
        }
        return ServiceUtil.returnSuccess();
    }

    // Made some changes transactionId removed.
    private static GenericValue externalOrderExists(Delegator delegator, String externalId) throws GenericEntityException {
        Debug.logInfo("Checking for existing externalId: " + externalId, module);
        EntityCondition condition = EntityCondition.makeCondition(UtilMisc.toList(EntityCondition.makeCondition("externalId", EntityComparisonOperator.EQUALS, externalId), EntityCondition.makeCondition("statusId", EntityComparisonOperator.NOT_EQUAL, "ORDER_CANCELLED")), EntityComparisonOperator.AND);
        GenericValue orderHeader = EntityQuery.use(delegator).from("OrderHeader")
                .where(condition)
                .cache(true).queryFirst();
        return orderHeader;
    }

    private static void addItem(ShoppingCart cart, Map<String, Object> orderItem, LocalDispatcher dispatcher, Delegator delegator, int groupIdx) throws GeneralException {
        String productId = (String) orderItem.get("productId");
        GenericValue product = EntityQuery.use(delegator).from("Product").where("productId", productId).queryOne();
        if (UtilValidate.isEmpty(product)) {
            String productMissingMsg = "The product having ID (" + productId + ") is misssing in the system.";
            orderImportFailureMessageList.add(productMissingMsg);
        }
        BigDecimal qty = new BigDecimal(orderItem.get("quantity").toString());
        String itemPrice = (String) orderItem.get("transactionPrice");
        if (UtilValidate.isEmpty(itemPrice)) {
            itemPrice = (String) orderItem.get("amount");
        }
        BigDecimal price = new BigDecimal(itemPrice);
        price = price.setScale(ShoppingCart.scale, ShoppingCart.rounding);

        HashMap<String, Object> attrs = new HashMap<String, Object>();
        attrs.put("shipGroup", groupIdx);

        // Checking if previously added same product exists in the cart
        ShoppingCartItem previouslyAddedItemInCart = null;
        if (cart.size() != 0) {
            Iterator<ShoppingCartItem> cartiter = cart.iterator();
            while (cartiter != null && cartiter.hasNext()) {
                ShoppingCartItem cartItem = cartiter.next();
                if (cartItem.getProductId().equals(productId)) {
                    previouslyAddedItemInCart = cartItem;
                }
            }
        }
        if (previouslyAddedItemInCart != null) {
            BigDecimal newQuantity = previouslyAddedItemInCart.getQuantity().add(qty);
            previouslyAddedItemInCart.setQuantity(newQuantity, dispatcher, cart);
        } else {
            int idx = cart.addItemToEnd(productId, null, qty, null, null, attrs, null, null, dispatcher, Boolean.FALSE, Boolean.TRUE, Boolean.TRUE, Boolean.TRUE);
            ShoppingCartItem cartItem = cart.findCartItem(idx);
            cartItem.setQuantity(qty, dispatcher, cart, true, false);
            // locate the price verify it matches the expected price
            BigDecimal cartPrice = cartItem.getBasePrice();
            cartPrice = cartPrice.setScale(ShoppingCart.scale, ShoppingCart.rounding);
            if (price.doubleValue() != cartPrice.doubleValue()) {
                // does not match; honor the price but hold the order for manual review
                cartItem.setIsModifiedPrice(true);
                cartItem.setBasePrice(price);
                cart.setHoldOrder(true);
                cart.addInternalOrderNote("Price received [" + price + "] (for item # " + productId + ") from eBay Checkout does not match the price in the database [" + cartPrice + "]. Order is held for manual review.");
            }
            // assign the item to its ship group
            cart.setItemShipGroupQty(cartItem, qty, groupIdx);
        }
    } 
}
