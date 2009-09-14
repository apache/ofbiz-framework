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

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javolution.util.FastMap;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.order.order.OrderChangeHelper;
import org.ofbiz.order.shoppingcart.CheckOutHelper;
import org.ofbiz.order.shoppingcart.ShoppingCart;
import org.ofbiz.order.shoppingcart.ShoppingCartItem;
import org.ofbiz.party.party.PartyWorker;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ModelService;
import org.ofbiz.service.ServiceUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class EbayOrderServices {
    
    private static final String resource = "EbayUiLabels";
    private static final String module = EbayOrderServices.class.getName();
    
    public static Map<String, Object> getEbayOrders(DispatchContext dctx, Map<String, Object> context) {
        GenericDelegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Locale locale = (Locale) context.get("locale");
        Map<String, Object> result = FastMap.newInstance();
        try {
            Map<String, Object> eBayConfigResult = EbayHelper.buildEbayConfig(context, delegator);
            StringBuffer getOrdersXml = new StringBuffer();

            if (!ServiceUtil.isFailure(buildGetOrdersRequest(context, getOrdersXml, eBayConfigResult.get("token").toString()))) {
                result = EbayHelper.postItem(eBayConfigResult.get("xmlGatewayUri").toString(), getOrdersXml, eBayConfigResult.get("devID").toString(), eBayConfigResult.get("appID").toString(), eBayConfigResult.get("certID").toString(), "GetOrders", eBayConfigResult.get("compatibilityLevel").toString(), eBayConfigResult.get("siteID").toString());
                String responseMsg = (String) result.get(ModelService.SUCCESS_MESSAGE);
                if (responseMsg != null) {
                    result = checkOrders(delegator, dispatcher, locale, context, responseMsg);
                }
            }
        } catch (Exception e) {
            String errMsg = UtilProperties.getMessage(resource, "buildEbayConfig.exceptionInGetOrdersFromEbay" + e.getMessage(), locale);
            return ServiceUtil.returnError(errMsg);
        }
        return result;
    }

    public static Map<String, Object> importEbayOrders(DispatchContext dctx, Map<String, Object> context) {
        Debug.logInfo("The value of context map is " + context, module);
        GenericDelegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Locale locale = (Locale) context.get("locale");
        Map<String, Object> result = FastMap.newInstance();
        try {
            result = createShoppingCart(delegator, dispatcher, locale, context, true);
        } catch (Exception e) {
            Debug.logError("Exception in importOrderFromEbay " + e, module);
            return ServiceUtil.returnFailure(UtilProperties.getMessage(resource, "ordersImportFromEbay.exceptionInImportOrderFromEbay", locale));
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

             EbayHelper.appendRequesterCredentials(transElem, transDoc, token);
             UtilXml.addChildElementValue(transElem, "DetailLevel", "ReturnAll", transDoc);
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
             //Debug.logInfo("The value of generated string is ======= " + UtilXml.writeXmlDocument(transDoc), module);
             dataItemsXml.append(UtilXml.writeXmlDocument(transDoc));
         } catch (Exception e) {
             Debug.logError("Exception during building get seller transactions request", module);
             return ServiceUtil.returnFailure(UtilProperties.getMessage(resource, "ordersImportFromEbay.exceptionDuringBuildingGetSellerTransactionRequest", locale));
         }
         return ServiceUtil.returnSuccess();
    }

    private static Map<String, Object> checkOrders(GenericDelegator delegator, LocalDispatcher dispatcher, Locale locale, Map<String, Object> context, String responseMsg) {
        StringBuffer errorMessage = new StringBuffer();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        List<GenericValue> orders = readGetOrdersResponse(responseMsg, locale, (String) context.get("productStoreId"), delegator, dispatcher, errorMessage, userLogin);
        if (orders == null || orders.size() == 0) {
            Debug.logError("No orders found", module);
            return ServiceUtil.returnFailure(UtilProperties.getMessage(resource, "ordersImportFromEbay.noOrdersFound", locale));
        }
        return ServiceUtil.returnSuccess();
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
    
    
    private static List readGetOrdersResponse(String responseMsg, Locale locale, String productStoreId, GenericDelegator delegator, LocalDispatcher dispatcher, StringBuffer errorMessage, GenericValue userLogin) {
        List fetchedOrders = new ArrayList();
        try {
            Document docResponse = UtilXml.readXmlDocument(responseMsg, true);
            //Debug.logInfo("The generated string is ======= " + UtilXml.writeXmlDocument(docResponse), module);
            Element elemResponse = docResponse.getDocumentElement();
            String ack = UtilXml.childElementValue(elemResponse, "Ack", "Failure");

            int totalOrders = 0;
            
            if (ack != null && "Success".equals(ack)) {
                List orderArrays = UtilXml.childElementList(elemResponse, "OrderArray");
                if (orderArrays != null && orderArrays.size() > 0) {
                    totalOrders = orderArrays.size();
                }
                if (totalOrders > 0) {
                    // retrieve transaction array
                    Iterator orderArraysElemIter = orderArrays.iterator();
                    while (orderArraysElemIter.hasNext()) {
                        Element orderArraysElement = (Element) orderArraysElemIter.next();

                        // retrieve transaction
                        List orders = UtilXml.childElementList(orderArraysElement, "Order");
                        Iterator ordersElemIter = orders.iterator();
                        
                        while (ordersElemIter.hasNext()) {
                            Map<String, Object> orderCtx = FastMap.newInstance();    
                            Element ordersElement = (Element) ordersElemIter.next();
                            String externalOrderId = UtilXml.childElementValue(ordersElement, "OrderID");
                            if (externalOrderExists(delegator, externalOrderId) != null) {
                                continue;
                            }
                            orderCtx.put("externalId", externalOrderId);
                            orderCtx.put("amountPaid", UtilXml.childElementValue(ordersElement, "Total", "0"));
                            orderCtx.put("createdDate", UtilXml.childElementValue(ordersElement, "CreatedTime"));
                            orderCtx.put("paidTime", UtilXml.childElementValue(ordersElement, "PaidTime"));
                            orderCtx.put("shippedTime", UtilXml.childElementValue(ordersElement, "ShippedTime"));
                            orderCtx.put("ebayUserIdBuyer", UtilXml.childElementValue(ordersElement, "BuyerUserID"));
                            orderCtx.put("productStoreId", productStoreId);
                            
                            // Retrieve shipping address 
                            Map<String, Object> shippingAddressCtx = FastMap.newInstance();
                            List shippingAddressList = UtilXml.childElementList(ordersElement, "ShippingAddress");
                            Iterator shippingAddressElemIter = shippingAddressList.iterator();
                            while (shippingAddressElemIter.hasNext()) {
                                Element shippingAddressElement = (Element)shippingAddressElemIter.next();
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
                            
                            // Retrieve shipping service selected
                            Map<String, Object> shippingServiceSelectedCtx = FastMap.newInstance();
                            List shippingServiceSelectedList = UtilXml.childElementList(ordersElement, "ShippingServiceSelected");
                            Iterator shippingServiceSelectedElemIter = shippingServiceSelectedList.iterator();
                            while (shippingServiceSelectedElemIter.hasNext()) {
                                Element shippingServiceSelectedElement = (Element)shippingServiceSelectedElemIter.next();
                                shippingServiceSelectedCtx.put("shippingService", UtilXml.childElementValue(shippingServiceSelectedElement, "ShippingService"));
                                shippingServiceSelectedCtx.put("shippingServiceCost", UtilXml.childElementValue(shippingServiceSelectedElement, "ShippingServiceCost", "0"));
                                String insuranceCost = UtilXml.childElementValue(shippingServiceSelectedElement, "ShippingInsuranceCost", "0");
                                if (UtilValidate.isNotEmpty(insuranceCost)) {
                                    shippingServiceSelectedCtx.put("shippingTotalAdditionalCost", insuranceCost);
                                }
                            }
                            orderCtx.put("shippingServiceSelectedCtx", shippingServiceSelectedCtx);
                            
                            // Retrieve shipping details
                            Map<String, Object> shippingDetailsCtx = FastMap.newInstance();
                            List shippingDetailsList = UtilXml.childElementList(ordersElement, "ShippingDetails");
                            Iterator shippingDetailsElemIter = shippingDetailsList.iterator();
                            while (shippingDetailsElemIter.hasNext()) {
                                Element shippingDetailsElement = (Element) shippingDetailsElemIter.next();
                                shippingDetailsCtx.put("insuranceFee", UtilXml.childElementValue(shippingDetailsElement, "InsuranceFee", "0"));
                                shippingDetailsCtx.put("insuranceOption", UtilXml.childElementValue(shippingDetailsElement, "InsuranceOption"));
                                shippingDetailsCtx.put("insuranceWanted", UtilXml.childElementValue(shippingDetailsElement, "InsuranceWanted", "false"));

                                // Retrieve sales Tax
                                List salesTaxList = UtilXml.childElementList(shippingDetailsElement, "SalesTax");
                                Iterator salesTaxElemIter = salesTaxList.iterator();
                                while (salesTaxElemIter.hasNext()) {
                                    Element salesTaxElement = (Element) salesTaxElemIter.next();
                                    shippingDetailsCtx.put("salesTaxAmount", UtilXml.childElementValue(salesTaxElement, "SalesTaxAmount", "0"));
                                    shippingDetailsCtx.put("salesTaxPercent", UtilXml.childElementValue(salesTaxElement, "SalesTaxPercent", "0"));
                                    shippingDetailsCtx.put("salesTaxState", UtilXml.childElementValue(salesTaxElement, "SalesTaxState", "0"));
                                    shippingDetailsCtx.put("shippingIncludedInTax", UtilXml.childElementValue(salesTaxElement, "ShippingIncludedInTax", "false"));
                                    }
                                }
                            orderCtx.put("shippingDetailsCtx", shippingDetailsCtx);
                            
                            // Retrieve checkout status
                            Map<String, Object> checkoutStatusCtx = FastMap.newInstance();
                            List checkoutStatusList = UtilXml.childElementList(ordersElement, "CheckoutStatus");
                            Iterator checkoutStatusElemIter = checkoutStatusList.iterator();
                            while (checkoutStatusElemIter.hasNext()) {
                                Element statusElement = (Element) checkoutStatusElemIter.next();
                                checkoutStatusCtx.put("eBayPaymentStatus", UtilXml.childElementValue(statusElement, "eBayPaymentStatus"));
                                checkoutStatusCtx.put("paymentMethodUsed", UtilXml.childElementValue(statusElement, "PaymentMethod"));
                                checkoutStatusCtx.put("completeStatus", UtilXml.childElementValue(statusElement, "Status"));
                            }
                            orderCtx.put("checkoutStatusCtx", checkoutStatusCtx);
                            
                            // Retrieve external transaction
                            Map<String, Object> externalTransactionCtx = FastMap.newInstance();
                            List externalTransactionList = UtilXml.childElementList(ordersElement, "ExternalTransaction");
                            Iterator externalTransactionElemIter = externalTransactionList.iterator();
                            while (externalTransactionElemIter.hasNext()) {
                                Element externalTransactionElement = (Element) externalTransactionElemIter.next();
                                externalTransactionCtx.put("externalTransactionID", UtilXml.childElementValue(externalTransactionElement, "ExternalTransactionID"));
                                externalTransactionCtx.put("externalTransactionTime", UtilXml.childElementValue(externalTransactionElement, "ExternalTransactionTime"));
                                externalTransactionCtx.put("feeOrCreditAmount", UtilXml.childElementValue(externalTransactionElement, "FeeOrCreditAmount", "0"));
                                externalTransactionCtx.put("paymentOrRefundAmount", UtilXml.childElementValue(externalTransactionElement, "PaymentOrRefundAmount", "0"));
                            }
                            orderCtx.put("externalTransactionCtx", externalTransactionCtx);
                            
                            // Retrieve Transactions Array --> Transactions | Order Items
                            List orderItemList = new ArrayList();
                            String buyersEmailId = null;
                            List transactionArrayList = UtilXml.childElementList(ordersElement, "TransactionArray");
                            Iterator transactionArrayElemIter = transactionArrayList.iterator();
                            while (transactionArrayElemIter.hasNext()) { 
                                Element transactionArrayElement = (Element) transactionArrayElemIter.next();
                                
                                boolean buyerEmailExists = false;
                                List transactionList = UtilXml.childElementList(transactionArrayElement, "Transaction");
                                Iterator transactionElemIter = transactionList.iterator();
                                while (transactionElemIter.hasNext()) { 
                                    Map<String, Object> transactionCtx = FastMap.newInstance();
                                    Element transactionElement = (Element) transactionElemIter.next();
                                    
                                    // Retrieve Buyer email
                                    if (!buyerEmailExists) {
                                        List buyerList = UtilXml.childElementList(transactionElement, "Buyer");
                                        Iterator buyerElemIter = buyerList.iterator();
                                        while (buyerElemIter.hasNext()) {
                                            Element buyerElement = (Element) buyerElemIter.next();
                                            buyersEmailId = UtilXml.childElementValue(buyerElement, "Email");
                                            buyerEmailExists = true;
                                        }
                                    }
                                    
                                    // Retrieve Order Item info
                                    List itemList = UtilXml.childElementList(transactionElement, "Item");
                                    Iterator itemElemIter = itemList.iterator();
                                    while (itemElemIter.hasNext()) {
                                        Element itemElement = (Element) itemElemIter.next();
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
                            Map<String, Object> result = dispatcher.runSync("importEbayOrders", orderCtx);
                            fetchedOrders.add(orderCtx);
                        }
                    }
                }
               //Debug.logInfo("The generated string is ======= " + fetchedOrders.toString(), module);
            } else {
                List errorList = UtilXml.childElementList(elemResponse, "Errors");
                Iterator errorElemIter = errorList.iterator();
                while (errorElemIter.hasNext()) {
                    Element errorElement = (Element) errorElemIter.next();
                    errorMessage.append(UtilXml.childElementValue(errorElement, "ShortMessage", ""));
                }
            }
        } catch (Exception e) {
            Debug.logError("Exception during read response from Ebay", module);
        }
        return fetchedOrders;
    }
    
    private static Map createShoppingCart(GenericDelegator delegator, LocalDispatcher dispatcher, Locale locale, Map context, boolean create) {
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
                GenericValue productStore = delegator.findByPrimaryKey("ProductStore", UtilMisc.toMap("productStoreId", productStoreId));
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

            if (UtilValidate.isNotEmpty(externalId)) {
                if (externalOrderExists(delegator, externalId) != null && create) {
                    return ServiceUtil.returnFailure(UtilProperties.getMessage(resource, "ordersImportFromEbay.externalIdAlreadyExist", locale));
                }
                cart.setExternalId(externalId);
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
            if (UtilValidate.isNotEmpty((String) context.get("createdDate"))) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
                Date createdDate = sdf.parse((String) context.get("createdDate"));
                orderDate = new Timestamp(createdDate.getTime());
            }
            cart.setOrderDate(orderDate);
            // Before import the order from eBay to OFBiz is mandatory that the payment has be received
            String paidTime = (String) context.get("paidTime");
            if (UtilValidate.isEmpty(paidTime)) {
                return ServiceUtil.returnFailure(UtilProperties.getMessage(resource, "ordersImportFromEbay.paymentIsStillNotReceived", locale));
            }
 
            //List orderItemList = (List) context.get("orderItemList");
            List orderItemList = (List) context.get("orderItemList");
            Iterator orderItemIter = orderItemList.iterator();
            while (orderItemIter.hasNext()) {
                Map orderItem = (Map) orderItemIter.next();
                addItem(cart, orderItem, dispatcher, 0);
            }
            
            // set partyId from
            if (UtilValidate.isNotEmpty(payToPartyId)) {
                cart.setBillFromVendorPartyId(payToPartyId);
            }
            // Apply shipping costs as order adjustment
            Map<String, Object> shippingServiceSelectedCtx =  (Map) context.get("shippingServiceSelectedCtx");
            
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
            Map<String, Object> shippingDetailsCtx = (Map) context.get("shippingDetailsCtx");
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
                String emailContactMechId = null;
                String phoneContactMechId = null;
                GenericValue partyAttribute = null;
                Map<String, Object> shippingAddressCtx =  (Map) context.get("shippingAddressCtx");
                if (UtilValidate.isNotEmpty(shippingAddressCtx)) {
                    String buyerName = (String) shippingAddressCtx.get("buyerName");
                    String firstName = (String) buyerName.substring(0, buyerName.indexOf(" "));
                    String lastName = (String) buyerName.substring(buyerName.indexOf(" ")+1);
                    
                    String country = (String) shippingAddressCtx.get("shippingAddressCountry");
                    String state = (String) shippingAddressCtx.get("shippingAddressStateOrProvince");
                    String city = (String) shippingAddressCtx.get("shippingAddressCityName");
                    EbayHelper.correctCityStateCountry(dispatcher, shippingAddressCtx, city, state, country);
                    
                    List<GenericValue> shipInfo = PartyWorker.findMatchingPartyAndPostalAddress(delegator, shippingAddressCtx.get("shippingAddressStreet1").toString(), 
                            (UtilValidate.isEmpty(shippingAddressCtx.get("shippingAddressStreet2")) ? null : shippingAddressCtx.get("shippingAddressStreet2").toString()), city, state, 
                            shippingAddressCtx.get("shippingAddressPostalCode").toString(), null, country, firstName, null, lastName);
                    if (shipInfo != null && shipInfo.size() > 0) {
                        GenericValue first = EntityUtil.getFirst(shipInfo);
                        partyId = first.getString("partyId");
                        Debug.logInfo("Existing shipping address found for : (party: " + partyId + ")", module);
                    }
                }
                // if we get a party, check its contact information.
                if (UtilValidate.isNotEmpty(partyId)) {
                    Debug.logInfo("Found existing party associated to the eBay buyer: " + partyId, module);
                    GenericValue party = delegator.findByPrimaryKey("Party", UtilMisc.toMap("partyId", partyId));

                    contactMechId = EbayHelper.setShippingAddressContactMech(dispatcher, delegator, party, userLogin, shippingAddressCtx);
                    String emailBuyer = (String) context.get("emailBuyer");
                    if (!(emailBuyer.equals("") || emailBuyer.equalsIgnoreCase("Invalid Request"))) {
                        String emailContactMech = EbayHelper.setEmailContactMech(dispatcher, delegator, party, userLogin, context);
                    }
                    String phoneContactMech = EbayHelper.setPhoneContactMech(dispatcher, delegator, party, userLogin, shippingAddressCtx);
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
                    Debug.logInfo("Creating new postal address for party: " + partyId, module);
                    contactMechId = EbayHelper.createAddress(dispatcher, partyId, userLogin, "SHIPPING_LOCATION", shippingAddressCtx);
                    if (UtilValidate.isEmpty(contactMechId)) {
                        return ServiceUtil.returnFailure("Unable to create postalAddress with input map: " + shippingAddressCtx);
                    }
                    Debug.logInfo("Created postal address: " + contactMechId, module);
                    Debug.logInfo("Creating new phone number for party: " + partyId, module);
                    EbayHelper.createPartyPhone(dispatcher, partyId, (String) shippingAddressCtx.get("shippingAddressPhone"), userLogin);
                    Debug.logInfo("Creating association to eBay buyer for party: " + partyId, module);
                    EbayHelper.createEbayCustomer(dispatcher, partyId, (String) context.get("ebayUserIdBuyer"), (String) context.get("eiasTokenBuyer"), userLogin);
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
                cart.setShippingContactMechId(contactMechId);
                cart.setMaySplit(Boolean.FALSE);

                Debug.logInfo("Setting shipment method: " + (String) shippingServiceSelectedCtx.get("shippingService"), module);
                EbayHelper.setShipmentMethodType(cart, (String) shippingServiceSelectedCtx.get("shippingService"));
                cart.makeAllShipGroupInfos();

                // create the order
                Debug.logInfo("Creating CheckOutHelper.", module);
                CheckOutHelper checkout = new CheckOutHelper(dispatcher, delegator, cart);
                Debug.logInfo("Creating order.", module);
                Map orderCreate = checkout.createOrder(userLogin);

                String orderId = (String)orderCreate.get("orderId");
                Debug.logInfo("Created order with id: " + orderId, module);

                // approve the order
                if (UtilValidate.isNotEmpty(orderId)) {
                    Debug.logInfo("Approving order with id: " + orderId, module);
                    boolean approved = OrderChangeHelper.approveOrder(dispatcher, userLogin, orderId);
                    Debug.logInfo("Order approved with result: " + approved, module);

                    // create the payment from the preference
                    if (approved) {
                        Debug.logInfo("Creating payment for approved order.", module);
                        EbayHelper.createPaymentFromPaymentPreferences(delegator, dispatcher, userLogin, orderId, externalId, cart.getOrderDate(), partyId);
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
    private static GenericValue externalOrderExists(GenericDelegator delegator, String externalId) throws GenericEntityException {
        Debug.logInfo("Checking for existing externalId: " + externalId, module);
        GenericValue orderHeader = null;
        List orderHeaderList = delegator.findByAnd("OrderHeader", UtilMisc.toMap("externalId", externalId));
        if (orderHeaderList != null && orderHeaderList.size() > 0) {
            orderHeader = EntityUtil.getFirst(orderHeaderList);
        }
        return orderHeader;
    }
    
    private static void addItem(ShoppingCart cart, Map orderItem, LocalDispatcher dispatcher, int groupIdx) throws GeneralException {
        String productId = (String) orderItem.get("productId");
        BigDecimal qty = new BigDecimal(orderItem.get("quantity").toString());
        BigDecimal price = new BigDecimal(orderItem.get("transactionPrice").toString());
        price = price.setScale(ShoppingCart.scale, ShoppingCart.rounding);
        
        HashMap<Object, Object> attrs = new HashMap<Object, Object>();
        attrs.put("shipGroup", groupIdx);

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
