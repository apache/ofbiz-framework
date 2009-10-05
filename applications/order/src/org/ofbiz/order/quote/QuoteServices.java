/*******************************************************************************
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
 *******************************************************************************/
package org.ofbiz.order.quote;

import java.sql.Timestamp;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javolution.util.FastMap;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;


public class QuoteServices {

    public static final String module = QuoteServices.class.getName();
    public static final String resource = "OrderUiLabels";
    public static final String resource_error = "OrderErrorUiLabels";

    public static Map sendQuoteReportMail(DispatchContext dctx, Map context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");

        String emailType = (String) context.get("emailType");
        String quoteId = (String) context.get("quoteId");
        String sendTo = (String) context.get("sendTo");
        String sendCc = (String) context.get("sendCc");
        String note = (String) context.get("note");

        // prepare the order information
        Map sendMap = FastMap.newInstance();

        // get the quote and store
        GenericValue quote = null;
        try {
            quote = delegator.findByPrimaryKey("Quote", UtilMisc.toMap("quoteId", quoteId));
        } catch (GenericEntityException e) {
            Debug.logError(e, "Problem getting Quote", module);
        }

        if (quote == null) {
            return ServiceUtil.returnFailure("Could not find Quote with ID [" + quoteId + "]");
        }

        GenericValue productStoreEmail = null;
        try {
            productStoreEmail = delegator.findByPrimaryKey("ProductStoreEmailSetting", UtilMisc.toMap("productStoreId", quote.get("productStoreId"), "emailType", emailType));
        } catch (GenericEntityException e) {
            Debug.logError(e, "Problem getting the ProductStoreEmailSetting for productStoreId=" + quote.get("productStoreId") + " and emailType=" + emailType, module);
        }
        if (productStoreEmail == null) {
            return ServiceUtil.returnFailure("No valid email setting for store with productStoreId=" + quote.get("productStoreId") + " and emailType=" + emailType);
        }
        String bodyScreenLocation = productStoreEmail.getString("bodyScreenLocation");
        if (UtilValidate.isEmpty(bodyScreenLocation)) {
            return ServiceUtil.returnFailure("No valid bodyScreenLocation in email setting for store with productStoreId=" + quote.get("productStoreId") + " and emailType=" + emailType);
        }
        sendMap.put("bodyScreenUri", bodyScreenLocation);
        String xslfoAttachScreenLocation = productStoreEmail.getString("xslfoAttachScreenLocation");
        sendMap.put("xslfoAttachScreenLocation", xslfoAttachScreenLocation);

        if ((sendTo == null) || !UtilValidate.isEmail(sendTo)) {
            return ServiceUtil.returnError("No sendTo email address found");
        }

        Map bodyParameters = UtilMisc.toMap("quoteId", quoteId, "userLogin", userLogin, "locale", locale);
        bodyParameters.put("note", note);
        bodyParameters.put("partyId", quote.getString("partyId")); // This is set to trigger the "storeEmailAsCommunication" seca
        sendMap.put("bodyParameters", bodyParameters);
        sendMap.put("userLogin", userLogin);

        String subjectString = productStoreEmail.getString("subject");
        sendMap.put("subject", subjectString);

        sendMap.put("contentType", productStoreEmail.get("contentType"));
        sendMap.put("sendFrom", productStoreEmail.get("fromAddress"));
        sendMap.put("sendCc", productStoreEmail.get("ccAddress"));
        sendMap.put("sendBcc", productStoreEmail.get("bccAddress"));
        sendMap.put("sendTo", sendTo);
        if ((sendCc != null) && UtilValidate.isEmail(sendCc)) {
            sendMap.put("sendCc", sendCc);
        } else {
            sendMap.put("sendCc", productStoreEmail.get("ccAddress"));
        }

        // send the notification
        Map sendResp = null;
        try {
            sendResp = dispatcher.runSync("sendMailFromScreen", sendMap);
        } catch (Exception e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error, "OrderServiceExceptionSeeLogs",locale));
        }

        // check for errors
        if (sendResp != null && !ServiceUtil.isError(sendResp)) {
            sendResp.put("emailType", emailType);
        }
        return sendResp;
    }

    public static Map storeQuote(DispatchContext dctx, Map context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        String quoteTypeId = (String) context.get("quoteTypeId");
        String partyId = (String) context.get("partyId");
        Timestamp issueDate = (Timestamp) context.get("issueDate");
        String statusId = (String) context.get("statusId");
        String currencyUomId = (String) context.get("currencyUomId");
        String productStoreId = (String) context.get("productStoreId");
        String salesChannelEnumId = (String) context.get("salesChannelEnumId");
        Timestamp validFromDate = (Timestamp) context.get("validFromDate");
        Timestamp validThruDate = (Timestamp) context.get("validThruDate");
        String quoteName = (String) context.get("quoteName");
        String description = (String) context.get("description");
        List quoteItems = (List) context.get("quoteItems");
        List quoteAttributes = (List) context.get("quoteAttributes");
        List quoteCoefficients = (List) context.get("quoteCoefficients");
        List quoteRoles = (List) context.get("quoteRoles");
        List quoteTerms = (List) context.get("quoteTerms");
        List quoteTermAttributes = (List) context.get("quoteTermAttributes");
        List quoteWorkEfforts = (List) context.get("quoteWorkEfforts");
        List quoteAdjustments = (List) context.get("quoteAdjustments");

        Map result = FastMap.newInstance();

        try {
            Map quoteIn = UtilMisc.toMap("quoteTypeId", quoteTypeId, "partyId", partyId, "issueDate", issueDate, "statusId", statusId, "currencyUomId", currencyUomId);
            quoteIn.put("productStoreId", productStoreId);
            quoteIn.put("salesChannelEnumId", salesChannelEnumId);
            quoteIn.put("productStoreId", productStoreId);
            quoteIn.put("validFromDate", validFromDate);
            quoteIn.put("validThruDate", validThruDate);
            quoteIn.put("quoteName", quoteName);
            quoteIn.put("description", description);
            if (userLogin != null) {
                quoteIn.put("userLogin", userLogin);
            }


            // create Quote
            Map quoteOut = dispatcher.runSync("createQuote", quoteIn);

            if (UtilValidate.isNotEmpty(quoteOut) && UtilValidate.isNotEmpty(quoteOut.get("quoteId"))) {
                String quoteId = (String)quoteOut.get("quoteId");
                result.put("quoteId", quoteId);

                // create Quote Items
                if (UtilValidate.isNotEmpty(quoteItems)) {
                    Iterator quoteIt = quoteItems.iterator();
                    while (quoteIt.hasNext()) {
                        GenericValue quoteItem = (GenericValue)quoteIt.next();
                        quoteItem.set("quoteId", quoteId);
                        Map quoteItemIn = quoteItem.getAllFields();
                        quoteItemIn.put("userLogin", userLogin);

                        dispatcher.runSync("createQuoteItem", quoteItemIn);
                    }
                }

                // create Quote Attributes
                if (UtilValidate.isNotEmpty(quoteAttributes)) {
                    Iterator quoteAttrIt = quoteAttributes.iterator();
                    while (quoteAttrIt.hasNext()) {
                        GenericValue quoteAttr = (GenericValue)quoteAttrIt.next();
                        quoteAttr.set("quoteId", quoteId);
                        Map quoteAttrIn = quoteAttr.getAllFields();
                        quoteAttrIn.put("userLogin", userLogin);

                        dispatcher.runSync("createQuoteAttribute", quoteAttrIn);
                    }
                }

                // create Quote Coefficients
                if (UtilValidate.isNotEmpty(quoteCoefficients)) {
                    Iterator quoteCoefficientIt = quoteCoefficients.iterator();
                    while (quoteCoefficientIt.hasNext()) {
                        GenericValue quoteCoefficient = (GenericValue)quoteCoefficientIt.next();
                        quoteCoefficient.set("quoteId", quoteId);
                        Map quoteCoefficientIn = quoteCoefficient.getAllFields();
                        quoteCoefficientIn.put("userLogin", userLogin);

                        dispatcher.runSync("createQuoteCoefficient", quoteCoefficientIn);
                    }
                }

                // create Quote Roles
                if (UtilValidate.isNotEmpty(quoteRoles)) {
                    Iterator quoteRoleIt = quoteRoles.iterator();
                    while (quoteRoleIt.hasNext()) {
                        GenericValue quoteRole = (GenericValue)quoteRoleIt.next();
                        quoteRole.set("quoteId", quoteId);
                        Map quoteRoleIn = quoteRole.getAllFields();
                        quoteRoleIn.put("userLogin", userLogin);

                        dispatcher.runSync("createQuoteRole", quoteRoleIn);
                    }
                }

                // create Quote WorkEfforts
                if (UtilValidate.isNotEmpty(quoteWorkEfforts)) {
                    Iterator quoteWorkEffortIt = quoteWorkEfforts.iterator();
                    while (quoteWorkEffortIt.hasNext()) {
                        GenericValue quoteWorkEffort = (GenericValue)quoteWorkEffortIt.next();
                        quoteWorkEffort.set("quoteId", quoteId);
                        Map quoteWorkEffortIn = quoteWorkEffort.getAllFields();
                        quoteWorkEffortIn.put("userLogin", userLogin);

                        dispatcher.runSync("createQuoteWorkEffort", quoteWorkEffortIn);
                    }
                }

                // create Quote Adjustments
                if (UtilValidate.isNotEmpty(quoteAdjustments)) {
                    Iterator quoteAdjustmentIt = quoteAdjustments.iterator();
                    while (quoteAdjustmentIt.hasNext()) {
                        GenericValue quoteAdjustment = (GenericValue)quoteAdjustmentIt.next();
                        quoteAdjustment.set("quoteId", quoteId);
                        Map quoteAdjustmentIn = quoteAdjustment.getAllFields();
                        quoteAdjustmentIn.put("userLogin", userLogin);

                        dispatcher.runSync("createQuoteAdjustment", quoteAdjustmentIn);
                    }
                }

                //TODO create Quote Terms still to be implemented the base service createQuoteTerm
                //TODO create Quote Term Attributes still to be implemented the base service createQuoteTermAttribute
            } else {
                return ServiceUtil.returnFailure("Could not storing Quote");
            }
        } catch (GenericServiceException e) {
            Debug.logError(e, "Problem storing Quote", module);
        }

        return result;
    }
}