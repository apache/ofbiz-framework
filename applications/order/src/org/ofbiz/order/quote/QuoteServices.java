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

import java.util.Locale;
import java.util.Map;

import javolution.util.FastMap;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;


public class QuoteServices {

    public static final String module = QuoteServices.class.getName();
    public static final String resource = "OrderUiLabels";
    public static final String resource_error = "OrderErrorUiLabels";

    public static Map sendQuoteReportMail(DispatchContext dctx, Map context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericDelegator delegator = dctx.getDelegator();
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

}
