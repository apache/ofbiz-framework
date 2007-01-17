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
package org.ofbiz.accounting.thirdparty.gosoftware;

import java.util.Map;
import java.util.Properties;
import java.util.List;
import java.io.IOException;
import java.text.DecimalFormat;
import java.sql.Timestamp;

import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.ServiceUtil;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.StringUtil;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.accounting.payment.PaymentGatewayServices;


public class RitaServices {

    public static final String module = RitaServices.class.getName();

    public static Map ccAuth(DispatchContext dctx, Map context) {
        Properties props = buildPccProperties(context);
        RitaApi api = getApi(props, "CREDIT");
        if (api == null) {
            return ServiceUtil.returnError("RiTA is not configured properly");
        }

        try {
            RitaServices.setCreditCardInfo(api, dctx.getDelegator(), context);
        } catch (GeneralException e) {
            return ServiceUtil.returnError(e.getMessage());
        }

        // basic tx info
        api.set(RitaApi.TRANS_AMOUNT, getAmountString(context, "processAmount"));
        api.set(RitaApi.INVOICE, context.get("orderId"));

        // command setting
        if ("1".equals(props.getProperty("autoBill"))) {
            // sale
            api.set(RitaApi.COMMAND, "SALE");
        } else {
            // pre-auth
            api.set(RitaApi.COMMAND, "PRE_AUTH");
        }

        // send the transaction
        RitaApi out = null;
        try {
            Debug.log("Sending request to RiTA", module);
            out = api.send();
        } catch (IOException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        } catch (GeneralException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        if (out != null) {
            Map result = ServiceUtil.returnSuccess();
            String resultCode = out.get(RitaApi.RESULT);
            boolean passed = false;
            if ("CAPTURED".equals(resultCode)) {
                result.put("authResult", Boolean.TRUE);
                result.put("captureResult", Boolean.TRUE);
                passed = true;
            } else if ("APPROVED".equals(resultCode)) {
                result.put("authCode", out.get(RitaApi.AUTH_CODE));
                result.put("authResult", Boolean.TRUE);
                passed = true;
            } else if ("PROCESSED".equals(resultCode)) {
                result.put("authResult", Boolean.TRUE);
            } else {
                result.put("authResult", Boolean.FALSE);
            }

            result.put("authRefNum", out.get(RitaApi.INTRN_SEQ_NUM) != null ? out.get(RitaApi.INTRN_SEQ_NUM) : "");
            result.put("processAmount", context.get("processAmount"));
            result.put("authCode", out.get(RitaApi.AUTH_CODE));
            result.put("authFlag", out.get(RitaApi.REFERENCE));
            result.put("authMessage", out.get(RitaApi.RESULT));
            result.put("cvCode", out.get(RitaApi.CVV2_CODE));
            result.put("avsCode", out.get(RitaApi.AVS_CODE));

            if (!passed) {
                String respMsg = out.get(RitaApi.RESULT) + " / " + out.get(RitaApi.INTRN_SEQ_NUM);                
                result.put("customerRespMsgs", UtilMisc.toList(respMsg));
            }

            if (result.get("captureResult") != null) {
                result.put("captureCode", out.get(RitaApi.AUTH_CODE));
                result.put("captureFlag", out.get(RitaApi.REFERENCE));
                result.put("captureRefNum", out.get(RitaApi.INTRN_SEQ_NUM));
                result.put("captureMessage", out.get(RitaApi.RESULT));
            }

            return result;

        } else {
            return ServiceUtil.returnError("Receive a null result from RiTA");
        }
    }

    public static Map ccCapture(DispatchContext dctx, Map context) {
        GenericValue orderPaymentPreference = (GenericValue) context.get("orderPaymentPreference");

        //lets see if there is a auth transaction already in context
        GenericValue authTransaction = (GenericValue) context.get("authTrans");

        if (authTransaction == null){
            authTransaction = PaymentGatewayServices.getAuthTransaction(orderPaymentPreference);
        }

        if (authTransaction == null) {
            return ServiceUtil.returnError("No authorization transaction found for the OrderPaymentPreference; cannot capture");
        }

        // setup the RiTA Interface
        Properties props = buildPccProperties(context);
        RitaApi api = getApi(props, "CREDIT");
        if (api == null) {
            return ServiceUtil.returnError("RiTA is not configured properly");
        }

        api.set(RitaApi.ORIG_SEQ_NUM, authTransaction.getString("referenceNum"));
        api.set(RitaApi.COMMAND, "COMPLETION");

        // send the transaction
        RitaApi out = null;
        try {
            out = api.send();
        } catch (IOException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        } catch (GeneralException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        if (out != null) {
            Map result = ServiceUtil.returnSuccess();
            String resultCode = out.get(RitaApi.RESULT);
            if ("CAPTURED".equals(resultCode)) {
                result.put("captureResult", Boolean.TRUE);
            } else {
                result.put("captureResult", Boolean.FALSE);
            }
            result.put("captureAmount", context.get("captureAmount"));
            result.put("captureRefNum", out.get(RitaApi.INTRN_SEQ_NUM) != null ? out.get(RitaApi.INTRN_SEQ_NUM) : "");
            result.put("captureCode", out.get(RitaApi.AUTH_CODE));
            result.put("captureFlag", out.get(RitaApi.REFERENCE));
            result.put("captureMessage", out.get(RitaApi.RESULT));

            return result;
        } else {
            return ServiceUtil.returnError("Receive a null result from RiTA");
        }
    }

    public static Map ccVoidRelease(DispatchContext dctx, Map context) {
        return ccVoid(dctx, context, false);
    }

    public static Map ccVoidRefund(DispatchContext dctx, Map context) {
        return ccVoid(dctx, context, true);
    }

    private static Map ccVoid(DispatchContext dctx, Map context, boolean isRefund) {
        GenericValue orderPaymentPreference = (GenericValue) context.get("orderPaymentPreference");

        //lets see if there is a auth transaction already in context
        GenericValue authTransaction = (GenericValue) context.get("authTrans");

        if (authTransaction == null){
            authTransaction = PaymentGatewayServices.getAuthTransaction(orderPaymentPreference);
        }

        if (authTransaction == null) {
            return ServiceUtil.returnError("No authorization transaction found for the OrderPaymentPreference; cannot release");
        }

        // setup the RiTA Interface
        Properties props = buildPccProperties(context);
        RitaApi api = getApi(props, "CREDIT");
        if (api == null) {
            return ServiceUtil.returnError("RiTA is not configured properly");
        }

        api.set(RitaApi.TRANS_AMOUNT, getAmountString(context, isRefund ? "refundAmount" : "releaseAmount"));
        api.set(RitaApi.ORIG_SEQ_NUM, authTransaction.getString("referenceNum"));
        api.set(RitaApi.COMMAND, "VOID");

        // check to make sure we are configured for SALE mode
        if (!"1".equals(props.getProperty("autoBill"))) {
            return ServiceUtil.returnError("RiTA does not support releasing pre-auths.");
        }

        // send the transaction
        RitaApi out = null;
        try {
            out = api.send();
        } catch (IOException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        } catch (GeneralException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        if (out != null) {
            Map result = ServiceUtil.returnSuccess();
            String resultCode = out.get(RitaApi.RESULT);
            if ("VOIDED".equals(resultCode)) {
                result.put(isRefund ? "refundResult" : "releaseResult", Boolean.TRUE);
            } else {
                result.put(isRefund ? "refundResult" : "releaseResult", Boolean.FALSE);
            }
            result.put(isRefund ? "refundAmount" : "releaseAmount", context.get(isRefund ? "refundAmount" : "releaseAmount"));
            result.put(isRefund ? "refundRefNum" : "releaseRefNum", out.get(RitaApi.INTRN_SEQ_NUM) != null ? out.get(RitaApi.INTRN_SEQ_NUM) : "");
            result.put(isRefund ? "refundCode" : "releaseCode", out.get(RitaApi.AUTH_CODE));
            result.put(isRefund ? "refundFlag" : "releaseFlag", out.get(RitaApi.REFERENCE));
            result.put(isRefund ? "refundMessage" : "releaseMessage", out.get(RitaApi.RESULT));

            return result;
        } else {
            return ServiceUtil.returnError("Receive a null result from RiTA");
        }
    }

    public static Map ccCreditRefund(DispatchContext dctx, Map context) {
        GenericValue orderPaymentPreference = (GenericValue) context.get("orderPaymentPreference");

        //lets see if there is a auth transaction already in context
        GenericValue authTransaction = (GenericValue) context.get("authTrans");

        if (authTransaction == null){
            authTransaction = PaymentGatewayServices.getAuthTransaction(orderPaymentPreference);
        }

        if (authTransaction == null) {
            return ServiceUtil.returnError("No authorization transaction found for the OrderPaymentPreference; cannot refund");
        }

        // setup the RiTA Interface
        Properties props = buildPccProperties(context);
        RitaApi api = getApi(props, "CREDIT");
        if (api == null) {
            return ServiceUtil.returnError("RiTA is not configured properly");
        }

        // set the required cc info
        try {
            RitaServices.setCreditCardInfo(api, dctx.getDelegator(), context);
        } catch (GeneralException e) {
            return ServiceUtil.returnError(e.getMessage());
        }

        api.set(RitaApi.TRANS_AMOUNT, getAmountString(context, "refundAmount"));
        api.set(RitaApi.ORIG_SEQ_NUM, authTransaction.getString("referenceNum"));
        api.set(RitaApi.COMMAND, "CREDIT");

        // send the transaction
        RitaApi out = null;
        try {
            out = api.send();
        } catch (IOException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        } catch (GeneralException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        if (out != null) {
            Map result = ServiceUtil.returnSuccess();
            String resultCode = out.get(RitaApi.RESULT);
            if ("CAPTURED".equals(resultCode)) {
                result.put("refundResult", Boolean.TRUE);
            } else {
                result.put("refundResult", Boolean.FALSE);
            }
            result.put("refundAmount", context.get("refundAmount"));
            result.put("refundRefNum", out.get(RitaApi.INTRN_SEQ_NUM) != null ? out.get(RitaApi.INTRN_SEQ_NUM) : "");
            result.put("refundCode", out.get(RitaApi.AUTH_CODE));
            result.put("refundFlag", out.get(RitaApi.REFERENCE));
            result.put("refundMessage", out.get(RitaApi.RESULT));

            return result;
        } else {
            return ServiceUtil.returnError("Receive a null result from RiTA");
        }
    }

    public static Map ccRefund(DispatchContext dctx, Map context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericDelegator delegator = dctx.getDelegator();
        GenericValue orderPaymentPreference = (GenericValue) context.get("orderPaymentPreference");
        GenericValue orderHeader = null;
        try {
            orderHeader = orderPaymentPreference.getRelatedOne("OrderHeader");
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError("Unable to obtain order header information from payment preference");
        }

        if (orderHeader != null) {
            String terminalId = orderHeader.getString("terminalId");
            boolean isVoid = false;
            if (terminalId != null) {
                Timestamp orderDate = orderHeader.getTimestamp("orderDate");
                GenericValue terminalState = null;
                try {
                    List states = delegator.findByAnd("PosTerminalState", UtilMisc.toMap("posTerminalId", terminalId));
                    states = EntityUtil.filterByDate(states, UtilDateTime.nowTimestamp(), "openedDate", "closedDate", true);
                    terminalState = EntityUtil.getFirst(states);
                } catch (GenericEntityException e) {
                    Debug.logError(e, module);
                }

                // this is the current opened terminal
                if (terminalState != null) {
                    Timestamp openDate = terminalState.getTimestamp("openedDate");
                    // if the order date is after the open date of the current state
                    // the order happend within the current open/close of the terminal
                    if (orderDate.after(openDate)) {
                        isVoid = true;
                    }
                }
            }

            Map refundResp = null;
            try {
                if (isVoid) {
                    refundResp = dispatcher.runSync("ritaCCVoidRefund", context);
                } else {
                    refundResp = dispatcher.runSync("ritaCCCreditRefund", context);
                }
            } catch (GenericServiceException e) {
                Debug.logError(e, module);
                return ServiceUtil.returnError("Service invocation exception");
            }
            return refundResp;
        } else {
            return ServiceUtil.returnError("Unable to find order information; cannot complete the refund");
        }
    }

    private static void setCreditCardInfo(RitaApi api, GenericDelegator delegator, Map context) throws GeneralException {
        GenericValue orderPaymentPreference = (GenericValue) context.get("orderPaymentPreference");
        GenericValue creditCard = (GenericValue) context.get("creditCard");
        if (creditCard == null) {
            creditCard = delegator.findByPrimaryKey("CreditCard", UtilMisc.toMap("paymentMethodId", orderPaymentPreference.getString("paymentMethodId")));
        }
        if (creditCard != null) {
            List expDateList = StringUtil.split(creditCard.getString("expireDate"), "/");
            String month = (String) expDateList.get(0);
            String year = (String) expDateList.get(1);
            String y2d = year.substring(2);

            String title = creditCard.getString("titleOnCard");
            String fname = creditCard.getString("firstNameOnCard");
            String mname = creditCard.getString("middleNameOnCard");
            String lname = creditCard.getString("lastNameOnCard");
            String sufix = creditCard.getString("suffixOnCard");
            StringBuffer name = new StringBuffer();
            if (UtilValidate.isNotEmpty(title)) {
                name.append(title + " ");
            }
            if (UtilValidate.isNotEmpty(fname)) {
                name.append(fname + " ");
            }
            if (UtilValidate.isNotEmpty(mname)) {
                name.append(mname + " ");
            }
            if (UtilValidate.isNotEmpty(lname)) {
                name.append(lname + " ");
            }
            if (UtilValidate.isNotEmpty(sufix)) {
                name.append(sufix);
            }
            String nameOnCard = name.toString().trim();
            String acctNumber = creditCard.getString("cardNumber");
            String cvNum = (String) context.get("cardSecurityCode");

            api.set(RitaApi.ACCT_NUM, acctNumber);
            api.set(RitaApi.EXP_MONTH, month);
            api.set(RitaApi.EXP_YEAR, y2d);
            api.set(RitaApi.CARDHOLDER, nameOnCard);
            if (UtilValidate.isNotEmpty(cvNum)) {
                api.set(RitaApi.CVV2, cvNum);
            }

            // billing address information
            GenericValue billingAddress = (GenericValue) context.get("billingAddress");
            if (billingAddress != null) {
                api.set(RitaApi.CUSTOMER_STREET, billingAddress.getString("address1"));
                api.set(RitaApi.CUSTOMER_ZIP, billingAddress.getString("postalCode"));
            } else {
                String zipCode = orderPaymentPreference.getString("billingPostalCode");
                if (UtilValidate.isNotEmpty(zipCode)) {
                    api.set(RitaApi.CUSTOMER_ZIP, zipCode);
                }
            }

            // set the present flag
            String presentFlag = orderPaymentPreference.getString("presentFlag");
            if (presentFlag == null) {
                presentFlag = "N";
            }
            api.set(RitaApi.PRESENT_FLAG, presentFlag.equals("Y") ? "3" : "1"); // 1, no present, 2 present, 3 swiped
        } else {
            throw new GeneralException("No CreditCard object found");
        }
    }

    private static RitaApi getApi(Properties props) {
        if (props == null) {
            Debug.logError("Cannot load API w/ null properties", module);
            return null;
        }
        String host = props.getProperty("host");
        int port = 0;
        try {
            port = Integer.parseInt(props.getProperty("port"));
        } catch (Exception e) {
            Debug.logError(e, module);
        }
        boolean ssl = props.getProperty("ssl", "N").equals("Y") ? true : false;

        RitaApi api = null;
        if (port > 0 && host != null) {
            api = new RitaApi(host, port, ssl);
        } else {
            api = new RitaApi();
        }

        api.set(RitaApi.CLIENT_ID, props.getProperty("clientID"));
        api.set(RitaApi.USER_ID, props.getProperty("userID"));
        api.set(RitaApi.USER_PW, props.getProperty("userPW"));
        api.set(RitaApi.FORCE_FLAG, props.getProperty("forceTx"));

        return api;
    }

    private static RitaApi getApi(Properties props, String paymentType) {
        RitaApi api = getApi(props);
        api.set(RitaApi.FUNCTION_TYPE, "PAYMENT");
        api.set(RitaApi.PAYMENT_TYPE, paymentType);
        return api;
    }

    private static Properties buildPccProperties(Map context) {
        String configString = (String) context.get("paymentConfig");
        if (configString == null) {
            configString = "payment.properties";
        }

        String clientId = UtilProperties.getPropertyValue(configString, "payment.rita.clientID");
        String userId = UtilProperties.getPropertyValue(configString, "payment.rita.userID");
        String userPw = UtilProperties.getPropertyValue(configString, "payment.rita.userPW");
        String host = UtilProperties.getPropertyValue(configString, "payment.rita.host");
        String port = UtilProperties.getPropertyValue(configString, "payment.rita.port");
        String ssl = UtilProperties.getPropertyValue(configString, "payment.rita.ssl", "N");
        String autoBill = UtilProperties.getPropertyValue(configString, "payment.rita.autoBill", "0");
        String forceTx = UtilProperties.getPropertyValue(configString, "payment.rita.forceTx", "0");

        // some property checking
        if (UtilValidate.isEmpty(clientId)) {
            Debug.logWarning("The clientID property in [" + configString + "] is not configured", module);
            return null;
        }
        if (UtilValidate.isEmpty(userId)) {
            Debug.logWarning("The userID property in [" + configString + "] is not configured", module);
            return null;
        }
        if (UtilValidate.isEmpty(userPw)) {
            Debug.logWarning("The userPW property in [" + configString + "] is not configured", module);
            return null;
        }

        // create some properties for CS Client
        Properties props = new Properties();
        props.put("clientID", clientId);
        props.put("userID", userId);
        props.put("userPW", userPw);
        props.put("host", host);
        props.put("port", port);
        props.put("ssl", ssl);
        props.put("autoBill", autoBill);
        props.put("forceTx", forceTx);
        Debug.log("Returning properties - " + props, module);

        return props;
    }

    private static String getAmountString(Map context, String amountField) {
        String currencyFormat = UtilProperties.getPropertyValue("general.properties", "currency.decimal.format", "##0.00");
        DecimalFormat formatter = new DecimalFormat(currencyFormat);
        Double processAmount = (Double) context.get(amountField);
        return formatter.format(processAmount);
    }
}
