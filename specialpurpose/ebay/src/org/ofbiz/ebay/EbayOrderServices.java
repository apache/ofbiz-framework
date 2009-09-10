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

import java.util.Locale;
import java.util.Map;

import javolution.util.FastMap;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.entity.GenericDelegator;
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
                String success = (String) result.get(ModelService.SUCCESS_MESSAGE);
                if (success != null) {
                    result = checkOrders(delegator, dispatcher, locale, context, success);
                }
            }
        } catch (Exception e) {
            String errMsg = UtilProperties.getMessage(resource, "buildEbayConfig.exceptionInGetOrdersFromEbay" + e.getMessage(), locale);
            return ServiceUtil.returnError(errMsg);
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

    private static Map<String, Object> checkOrders(GenericDelegator delegator, LocalDispatcher dispatcher, Locale locale, Map<String, Object> context, String response) {
        Document docResponse;
        try {
            docResponse = UtilXml.readXmlDocument(response, true);
            //Debug.logInfo("The generated string is ======= " + UtilXml.writeXmlDocument(docResponse), module);            
        } catch (Exception e) {
            Debug.logError("Exception during read response from Ebay", module);
        }
        return ServiceUtil.returnSuccess();
    }
}
