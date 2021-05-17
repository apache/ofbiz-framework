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
package org.apache.ofbiz.order.quote;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;


public class QuoteServices {

    private static final String MODULE = QuoteServices.class.getName();
    private static final String RESOURCE = "OrderUiLabels";
    private static final String RES_ERROR = "OrderErrorUiLabels";
    private static final String RES_PRODUCT = "ProductUiLabels";

    public static Map<String, Object> sendQuoteReportMail(DispatchContext dctx, Map<String, ? extends Object> context) {
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
        Map<String, Object> sendMap = new HashMap<>();

        // get the quote and store
        GenericValue quote = null;
        try {
            quote = EntityQuery.use(delegator).from("Quote").where("quoteId", quoteId).queryOne();
        } catch (GenericEntityException e) {
            Debug.logError(e, "Problem getting Quote", MODULE);
        }

        if (quote == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    "OrderOrderQuoteCannotBeFound",
                    UtilMisc.toMap("quoteId", quoteId), locale));
        }

        GenericValue productStoreEmail = null;
        try {
            productStoreEmail = EntityQuery.use(delegator).from("ProductStoreEmailSetting").where("productStoreId",
                    quote.get("productStoreId"), "emailType", emailType).queryOne();
        } catch (GenericEntityException e) {
            Debug.logError(e, "Problem getting the ProductStoreEmailSetting for productStoreId=" + quote.get("productStoreId")
                    + " and emailType=" + emailType, MODULE);
        }
        if (productStoreEmail == null) {
            return ServiceUtil.returnFailure(UtilProperties.getMessage(RES_PRODUCT,
                    "ProductProductStoreEmailSettingsNotValid",
                    UtilMisc.toMap("productStoreId", quote.get("productStoreId"),
                            "emailType", emailType), locale));
        }
        String bodyScreenLocation = productStoreEmail.getString("bodyScreenLocation");
        if (UtilValidate.isEmpty(bodyScreenLocation)) {
            return ServiceUtil.returnFailure(UtilProperties.getMessage(RES_PRODUCT,
                    "ProductProductStoreEmailSettingsNotValidBodyScreenLocation",
                    UtilMisc.toMap("productStoreId", quote.get("productStoreId"),
                            "emailType", emailType), locale));
        }
        sendMap.put("bodyScreenUri", bodyScreenLocation);
        String xslfoAttachScreenLocation = productStoreEmail.getString("xslfoAttachScreenLocation");
        sendMap.put("xslfoAttachScreenLocation", xslfoAttachScreenLocation);

        if ((sendTo == null) || !UtilValidate.isEmail(sendTo)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_PRODUCT,
                    "ProductProductStoreEmailSettingsNoSendToFound", locale));
        }

        Map<String, Object> bodyParameters = UtilMisc.<String, Object>toMap("quoteId", quoteId, "userLogin", userLogin, "locale", locale);
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
        Map<String, Object> sendResp = null;
        try {
            sendResp = dispatcher.runSync("sendMailFromScreen", sendMap);
            if (ServiceUtil.isError(sendResp)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(sendResp));
            }
        } catch (GenericServiceException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, "OrderServiceExceptionSeeLogs", locale));
        }

        // check for errors
        if (sendResp != null && ServiceUtil.isSuccess(sendResp)) {
            sendResp.put("emailType", emailType);
        }
        return sendResp;
    }

    public static Map<String, Object> storeQuote(DispatchContext dctx, Map<String, ? extends Object> context) {
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
        List<GenericValue> quoteItems = UtilGenerics.cast(context.get("quoteItems"));
        List<GenericValue> quoteAttributes = UtilGenerics.cast(context.get("quoteAttributes"));
        List<GenericValue> quoteCoefficients = UtilGenerics.cast(context.get("quoteCoefficients"));
        List<GenericValue> quoteRoles = UtilGenerics.cast(context.get("quoteRoles"));
        List<GenericValue> quoteWorkEfforts = UtilGenerics.cast(context.get("quoteWorkEfforts"));
        List<GenericValue> quoteAdjustments = UtilGenerics.cast(context.get("quoteAdjustments"));
        Locale locale = (Locale) context.get("locale");
        Map<String, Object> serviceResult = new HashMap<>();

        //TODO create Quote Terms still to be implemented
        //TODO create Quote Term Attributes still to be implemented
        Map<String, Object> result = new HashMap<>();

        try {
            Map<String, Object> quoteIn = UtilMisc.toMap("quoteTypeId", quoteTypeId, "partyId", partyId, "issueDate", issueDate,
                    "statusId", statusId, "currencyUomId", currencyUomId);
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
            Map<String, Object> quoteOut = dispatcher.runSync("createQuote", quoteIn);
            if (ServiceUtil.isError(quoteOut)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(quoteOut));
            }
            if (UtilValidate.isNotEmpty(quoteOut) && UtilValidate.isNotEmpty(quoteOut.get("quoteId"))) {
                String quoteId = (String) quoteOut.get("quoteId");
                result.put("quoteId", quoteId);

                // create Quote Items
                if (UtilValidate.isNotEmpty(quoteItems)) {
                    for (GenericValue quoteItem : quoteItems) {
                        quoteItem.set("quoteId", quoteId);
                        Map<String, Object> quoteItemIn = quoteItem.getAllFields();
                        quoteItemIn.put("userLogin", userLogin);

                        serviceResult = dispatcher.runSync("createQuoteItem", quoteItemIn);
                        if (ServiceUtil.isError(serviceResult)) {
                            return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                        }
                    }
                }

                // create Quote Attributes
                if (UtilValidate.isNotEmpty(quoteAttributes)) {
                    for (GenericValue quoteAttr : quoteAttributes) {
                        quoteAttr.set("quoteId", quoteId);
                        Map<String, Object> quoteAttrIn = quoteAttr.getAllFields();
                        quoteAttrIn.put("userLogin", userLogin);

                        serviceResult = dispatcher.runSync("createQuoteAttribute", quoteAttrIn);
                        if (ServiceUtil.isError(serviceResult)) {
                            return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                        }
                    }
                }

                // create Quote Coefficients
                if (UtilValidate.isNotEmpty(quoteCoefficients)) {
                    for (GenericValue quoteCoefficient : quoteCoefficients) {
                        quoteCoefficient.set("quoteId", quoteId);
                        Map<String, Object> quoteCoefficientIn = quoteCoefficient.getAllFields();
                        quoteCoefficientIn.put("userLogin", userLogin);

                        serviceResult = dispatcher.runSync("createQuoteCoefficient", quoteCoefficientIn);
                        if (ServiceUtil.isError(serviceResult)) {
                            return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                        }
                    }
                }

                // create Quote Roles
                if (UtilValidate.isNotEmpty(quoteRoles)) {
                    for (GenericValue quoteRole : quoteRoles) {
                        quoteRole.set("quoteId", quoteId);
                        Map<String, Object> quoteRoleIn = quoteRole.getAllFields();
                        quoteRoleIn.put("userLogin", userLogin);
                        serviceResult = dispatcher.runSync("createQuoteRole", quoteRoleIn);
                        if (ServiceUtil.isError(serviceResult)) {
                            return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                        }
                    }
                }

                // create Quote WorkEfforts
                if (UtilValidate.isNotEmpty(quoteWorkEfforts)) {
                    for (GenericValue quoteWorkEffort : quoteWorkEfforts) {
                        quoteWorkEffort.set("quoteId", quoteId);
                        Map<String, Object> quoteWorkEffortIn = quoteWorkEffort.getAllFields();
                        quoteWorkEffortIn.put("userLogin", userLogin);
                        serviceResult = dispatcher.runSync("createQuoteWorkEffort", quoteWorkEffortIn);
                        if (ServiceUtil.isError(serviceResult)) {
                            return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                        }
                    }
                }

                // create Quote Adjustments
                if (UtilValidate.isNotEmpty(quoteAdjustments)) {
                    for (GenericValue quoteAdjustment : quoteAdjustments) {
                        quoteAdjustment.set("quoteId", quoteId);
                        Map<String, Object> quoteAdjustmentIn = quoteAdjustment.getAllFields();
                        quoteAdjustmentIn.put("userLogin", userLogin);
                        serviceResult = dispatcher.runSync("createQuoteAdjustment", quoteAdjustmentIn);
                        if (ServiceUtil.isError(serviceResult)) {
                            return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                        }
                    }
                }

                //TODO create Quote Terms still to be implemented the base service createQuoteTerm
                //TODO create Quote Term Attributes still to be implemented the base service createQuoteTermAttribute
            } else {
                return ServiceUtil.returnFailure(UtilProperties.getMessage(RESOURCE,
                        "OrderOrderQuoteCannotBeStored", locale));
            }
        } catch (GenericServiceException e) {
            Debug.logError(e, "Problem storing Quote", MODULE);
        }

        return result;
    }
}
