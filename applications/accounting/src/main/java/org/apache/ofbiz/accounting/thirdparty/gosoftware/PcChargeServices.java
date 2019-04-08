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
package org.apache.ofbiz.accounting.thirdparty.gosoftware;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import org.apache.ofbiz.accounting.payment.PaymentGatewayServices;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.StringUtil;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilNumber;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityUtilProperties;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.ServiceUtil;


public class PcChargeServices {

    public static final String module = PcChargeServices.class.getName();
    private static int decimals = UtilNumber.getBigDecimalScale("invoice.decimals");
    private static int rounding = UtilNumber.getBigDecimalRoundingMode("invoice.rounding");
    public final static String resource = "AccountingUiLabels";

    public static Map<String, Object> ccAuth(DispatchContext dctx, Map<String, ? extends Object> context) {
        Locale locale = (Locale) context.get("locale");
        Delegator delegator = dctx.getDelegator();
        // setup the PCCharge Interface
        Properties props = buildPccProperties(context, delegator);
        PcChargeApi api = getApi(props);
        if (api == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                    "AccountingPcChargeErrorGettingPaymentGatewayConfig", locale));
        }

        try {
            PcChargeServices.setCreditCardInfo(api, context);
        } catch (GeneralException e) {
            return ServiceUtil.returnError(e.getMessage());
        }

        // basic tx info
        api.set(PcChargeApi.TRANS_AMOUNT, getAmountString(context, "processAmount"));
        api.set(PcChargeApi.TICKET_NUM, context.get("orderId"));
        api.set(PcChargeApi.MANUAL_FLAG, "0");
        api.set(PcChargeApi.PRESENT_FLAG, "1");

        // command setting
        if ("1".equals(props.getProperty("autoBill"))) {
            // sale
            api.set(PcChargeApi.COMMAND, "1");
        } else {
            // pre-auth
            api.set(PcChargeApi.COMMAND, "4");
        }

        // send the transaction
        PcChargeApi out = null;
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
            Map<String, Object> result = ServiceUtil.returnSuccess();
            String resultCode = out.get(PcChargeApi.RESULT);
            boolean passed = false;
            if ("CAPTURED".equals(resultCode)) {
                result.put("authResult", Boolean.TRUE);
                result.put("captureResult", Boolean.TRUE);
                passed = true;
            } else if ("APPROVED".equals(resultCode)) {
                result.put("authCode", out.get(PcChargeApi.AUTH_CODE));
                result.put("authResult", Boolean.TRUE);
                passed = true;
            } else if ("PROCESSED".equals(resultCode)) {
                result.put("authResult", Boolean.TRUE);
            } else {
                result.put("authResult", Boolean.FALSE);
            }

            result.put("authRefNum", out.get(PcChargeApi.TROUTD) != null ? out.get(PcChargeApi.TROUTD) : "");
            result.put("processAmount", context.get("processAmount"));
            result.put("authCode", out.get(PcChargeApi.AUTH_CODE));
            result.put("authFlag", out.get(PcChargeApi.REFERENCE));
            result.put("authMessage", out.get(PcChargeApi.RESULT));
            result.put("cvCode", out.get(PcChargeApi.CVV2_CODE));
            result.put("avsCode", out.get(PcChargeApi.AVS_CODE));

            if (!passed) {
                String respMsg = out.get(PcChargeApi.RESULT) + " / " + out.get(PcChargeApi.AUTH_CODE);
                String refNum = out.get(PcChargeApi.TROUTD);
                result.put("customerRespMsgs", UtilMisc.toList(respMsg, refNum));
            }

            if (result.get("captureResult") != null) {
                result.put("captureCode", out.get(PcChargeApi.AUTH_CODE));
                result.put("captureFlag", out.get(PcChargeApi.REFERENCE));
                result.put("captureRefNum", out.get(PcChargeApi.TROUTD));
                result.put("captureMessage", out.get(PcChargeApi.RESULT));
            }

            return result;

        } else {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                    "AccountingPcChargeResultIsNull", locale));
        }
    }

    public static Map<String, Object> ccCapture(DispatchContext dctx, Map<String, ? extends Object> context) {
        GenericValue orderPaymentPreference = (GenericValue) context.get("orderPaymentPreference");
        Locale locale = (Locale) context.get("locale");
        Delegator delegator = dctx.getDelegator();
        //lets see if there is a auth transaction already in context
        GenericValue authTransaction = (GenericValue) context.get("authTrans");

        if (authTransaction == null) {
            authTransaction = PaymentGatewayServices.getAuthTransaction(orderPaymentPreference);
        }

        if (authTransaction == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                    "AccountingPaymentTransactionAuthorizationNotFoundCannotCapture", locale));
        }

        // setup the PCCharge Interface
        Properties props = buildPccProperties(context, delegator);
        PcChargeApi api = getApi(props);
        if (api == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                    "AccountingPcChargeErrorGettingPaymentGatewayConfig", locale));
        }

        api.set(PcChargeApi.TROUTD, authTransaction.getString("referenceNum"));
        api.set(PcChargeApi.COMMAND, "5");

        // send the transaction
        PcChargeApi out = null;
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
            Map<String, Object> result = ServiceUtil.returnSuccess();
            String resultCode = out.get(PcChargeApi.RESULT);
            if ("CAPTURED".equals(resultCode)) {
                result.put("captureResult", Boolean.TRUE);
            } else {
                result.put("captureResult", Boolean.FALSE);
            }
            result.put("captureAmount", context.get("captureAmount"));
            result.put("captureRefNum", out.get(PcChargeApi.TROUTD) != null ? out.get(PcChargeApi.TROUTD) : "");
            result.put("captureCode", out.get(PcChargeApi.AUTH_CODE));
            result.put("captureFlag", out.get(PcChargeApi.REFERENCE));
            result.put("captureMessage", out.get(PcChargeApi.RESULT));

            return result;
        } else {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                    "AccountingPcChargeResultIsNull", locale));
        }
    }

    public static Map<String, Object> ccRelease(DispatchContext dctx, Map<String, ? extends Object> context) {
        GenericValue orderPaymentPreference = (GenericValue) context.get("orderPaymentPreference");
        Delegator delegator = dctx.getDelegator();
        //lets see if there is a auth transaction already in context
        GenericValue authTransaction = (GenericValue) context.get("authTrans");
        Locale locale = (Locale) context.get("locale");

        if (authTransaction == null) {
            authTransaction = PaymentGatewayServices.getAuthTransaction(orderPaymentPreference);
        }

        if (authTransaction == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                    "AccountingPaymentTransactionAuthorizationNotFoundCannotRelease", locale));
        }

        // setup the PCCharge Interface
        Properties props = buildPccProperties(context, delegator);
        PcChargeApi api = getApi(props);
        if (api == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                    "AccountingPcChargeErrorGettingPaymentGatewayConfig", locale));
        }

        api.set(PcChargeApi.TROUTD, authTransaction.getString("referenceNum"));
        api.set(PcChargeApi.COMMAND, "3");

        // check to make sure we are configured for SALE mode
        if (!"true".equalsIgnoreCase(props.getProperty("autoBill"))) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                    "AccountingPcChargeCannotSupportReleasingPreAuth", locale));
        }

        // send the transaction
        PcChargeApi out = null;
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
            Map<String, Object> result = ServiceUtil.returnSuccess();
            String resultCode = out.get(PcChargeApi.RESULT);
            if ("VOIDED".equals(resultCode)) {
                result.put("releaseResult", Boolean.TRUE);
            } else {
                result.put("releaseResult", Boolean.FALSE);
            }
            result.put("releaseAmount", context.get("releaseAmount"));
            result.put("releaseRefNum", out.get(PcChargeApi.TROUTD) != null ? out.get(PcChargeApi.TROUTD) : "");
            result.put("releaseCode", out.get(PcChargeApi.AUTH_CODE));
            result.put("releaseFlag", out.get(PcChargeApi.REFERENCE));
            result.put("releaseMessage", out.get(PcChargeApi.RESULT));

            return result;
        } else {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                "AccountingPcChargeResultIsNull", locale));
        }
    }

    public static Map<String, Object> ccRefund(DispatchContext dctx, Map<String, ? extends Object> context) {
        GenericValue orderPaymentPreference = (GenericValue) context.get("orderPaymentPreference");
        Delegator delegator = dctx.getDelegator();
        //lets see if there is a auth transaction already in context
        GenericValue authTransaction = (GenericValue) context.get("authTrans");
        Locale locale = (Locale) context.get("locale");

        if (authTransaction == null) {
            authTransaction = PaymentGatewayServices.getAuthTransaction(orderPaymentPreference);
        }

        if (authTransaction == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                    "AccountingPaymentTransactionAuthorizationNotFoundCannotRefund", locale));
        }

        // setup the PCCharge Interface
        Properties props = buildPccProperties(context, delegator);
        PcChargeApi api = getApi(props);
        if (api == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                    "AccountingPcChargeErrorGettingPaymentGatewayConfig", locale));
        }

        api.set(PcChargeApi.TROUTD, authTransaction.getString("referenceNum"));
        api.set(PcChargeApi.COMMAND, "2");

        // send the transaction
        PcChargeApi out = null;
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
            Map<String, Object> result = ServiceUtil.returnSuccess();
            String resultCode = out.get(PcChargeApi.RESULT);
            if ("CAPTURED".equals(resultCode)) {
                result.put("refundResult", Boolean.TRUE);
            } else {
                result.put("refundResult", Boolean.FALSE);
            }
            result.put("refundAmount", context.get("releaseAmount"));
            result.put("refundRefNum", out.get(PcChargeApi.TROUTD) != null ? out.get(PcChargeApi.TROUTD) : "");
            result.put("refundCode", out.get(PcChargeApi.AUTH_CODE));
            result.put("refundFlag", out.get(PcChargeApi.REFERENCE));
            result.put("refundMessage", out.get(PcChargeApi.RESULT));

            return result;
        } else {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                    "AccountingPcChargeResultIsNull", locale));
        }
    }

    private static void setCreditCardInfo(PcChargeApi api, Map<String, ? extends Object> context) throws GeneralException {
        GenericValue orderPaymentPreference = (GenericValue) context.get("orderPaymentPreference");
        GenericValue creditCard = (GenericValue) context.get("creditCard");
        if (creditCard != null) {
            List<String> expDateList = StringUtil.split(creditCard.getString("expireDate"), "/");
            String month = expDateList.get(0);
            String year = expDateList.get(1);
            String y2d = year.substring(2);
            String expDate = month + y2d;

            String title = creditCard.getString("titleOnCard");
            String fname = creditCard.getString("firstNameOnCard");
            String mname = creditCard.getString("middleNameOnCard");
            String lname = creditCard.getString("lastNameOnCard");
            String sufix = creditCard.getString("suffixOnCard");
            StringBuilder name = new StringBuilder();
            if (UtilValidate.isNotEmpty(title)) {
                name.append(title).append(" ");
            }
            if (UtilValidate.isNotEmpty(fname)) {
                name.append(fname).append(" ");
            }
            if (UtilValidate.isNotEmpty(mname)) {
                name.append(mname).append(" ");
            }
            if (UtilValidate.isNotEmpty(lname)) {
                name.append(lname).append(" ");
            }
            if (UtilValidate.isNotEmpty(sufix)) {
                name.append(sufix);
            }
            String nameOnCard = name.toString().trim();
            String acctNumber = "F" + creditCard.getString("cardNumber");
            String cvNum = (String) context.get("cardSecurityCode");

            api.set(PcChargeApi.ACCT_NUM, acctNumber);
            api.set(PcChargeApi.EXP_DATE, expDate);
            api.set(PcChargeApi.CARDHOLDER, nameOnCard);
            if (UtilValidate.isNotEmpty(cvNum)) {
                api.set(PcChargeApi.CVV2, cvNum);
            }

            // billing address information
            GenericValue billingAddress = (GenericValue) context.get("billingAddress");
            if (billingAddress != null) {
                api.set(PcChargeApi.STREET, billingAddress.getString("address1"));
                api.set(PcChargeApi.ZIP_CODE, billingAddress.getString("postalCode"));
            } else {
                String zipCode = orderPaymentPreference.getString("billingPostalCode");
                if (UtilValidate.isNotEmpty(zipCode)) {
                    api.set(PcChargeApi.ZIP_CODE, zipCode);
                }
            }
        } else {
            throw new GeneralException("No CreditCard object found");
        }
    }

    private static PcChargeApi getApi(Properties props) {
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
        PcChargeApi api = null;
        if (port > 0 && host != null) {
            api = new PcChargeApi(host, port);
        } else {
            api = new PcChargeApi();
        }

        api.set(PcChargeApi.PROCESSOR_ID, props.getProperty("processorID"));
        api.set(PcChargeApi.MERCH_NUM, props.getProperty("merchantID"));
        api.set(PcChargeApi.USER_ID, props.getProperty("userID"));
        return api;
    }

    private static Properties buildPccProperties(Map<String, ? extends Object> context, Delegator delegator) {
        String configString = (String) context.get("paymentConfig");
        if (configString == null) {
            configString = "payment.properties";
        }

        String processorId = EntityUtilProperties.getPropertyValue(configString, "payment.pccharge.processorID", delegator);
        String merchantId = EntityUtilProperties.getPropertyValue(configString, "payment.pccharge.merchantID", delegator);
        String userId = EntityUtilProperties.getPropertyValue(configString, "payment.pccharge.userID", delegator);
        String host = EntityUtilProperties.getPropertyValue(configString, "payment.pccharge.host", delegator);
        String port = EntityUtilProperties.getPropertyValue(configString, "payment.pccharge.port", delegator);
        String autoBill = EntityUtilProperties.getPropertyValue(configString, "payment.pccharge.autoBill", "true", delegator);

        // some property checking
        if (UtilValidate.isEmpty(processorId)) {
            Debug.logWarning("The processorID property in [" + configString + "] is not configured", module);
            return null;
        }
        if (UtilValidate.isEmpty(merchantId)) {
            Debug.logWarning("The merchantID property in [" + configString + "] is not configured", module);
            return null;
        }
        if (UtilValidate.isEmpty(userId)) {
            Debug.logWarning("The userID property in [" + configString + "] is not configured", module);
            return null;
        }

        // create some properties for CS Client
        Properties props = new Properties();
        props.put("processorID", processorId);
        props.put("merchantID", merchantId);
        props.put("userID", userId);
        props.put("host", host);
        props.put("port", port);
        props.put("autoBill", autoBill);
        Debug.logInfo("Returning properties - " + props, module);

        return props;
    }

    private static String getAmountString(Map<String, ? extends Object> context, String amountField) {
        BigDecimal processAmount = (BigDecimal) context.get(amountField);
        return processAmount.setScale(decimals, rounding).toPlainString();
    }

}
