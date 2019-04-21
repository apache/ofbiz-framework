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

package org.apache.ofbiz.accounting.thirdparty.sagepay;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.ofbiz.accounting.payment.PaymentGatewayServices;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilFormatOut;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;

public class SagePayPaymentServices {

    public static final String module = SagePayPaymentServices.class.getName();
    public final static String resource = "AccountingUiLabels";

    private static Map<String, String> buildCustomerBillingInfo(Map<String, Object> context) {
        Debug.logInfo("SagePay - Entered buildCustomerBillingInfo", module);
        Debug.logInfo("SagePay buildCustomerBillingInfo context : " + context, module);

        Map<String, String> billingInfo = new HashMap<>();

        String orderId = null;
        BigDecimal processAmount = null;
        String currency = null;
        String cardNumber = null;
        String cardType = null;
        String nameOnCard = null;
        String expireDate = null;
        String securityCode = null;
        String postalCode = null;
        String address = null;

        try {

            GenericValue opp = (GenericValue) context.get("orderPaymentPreference");
            if (opp != null) {
                if ("CREDIT_CARD".equals(opp.getString("paymentMethodTypeId"))) {

                    GenericValue creditCard = (GenericValue) context.get("creditCard");
                    if (creditCard == null || !(opp.get("paymentMethodId").equals(creditCard.get("paymentMethodId")))) {
                        creditCard = opp.getRelatedOne("CreditCard", false);
                    }

                    securityCode = opp.getString("securityCode");

                    //getting billing address
                    GenericValue billingAddress = (GenericValue) context.get("billingAddress");
                    postalCode = billingAddress.getString("postalCode");
                    String address2 = billingAddress.getString("address2");
                    if (address2 == null){
                        address2 = "";
                    }
                    address = billingAddress.getString("address1") + " " + address2;

                    //getting card details
                    cardNumber = creditCard.getString("cardNumber");
                    String firstName = creditCard.getString("firstNameOnCard");
                    String middleName = creditCard.getString("middleNameOnCard");
                    String lastName = creditCard.getString("lastNameOnCard");
                    if (middleName == null){
                        middleName = "";
                    }
                    nameOnCard = firstName + " " + middleName + " " + lastName;
                    cardType = creditCard.getString("cardType");
                    if (cardType != null) {
                        if ("CCT_MASTERCARD".equals(cardType)) {
                            cardType = "MC";
                        }
                        if ("CCT_VISAELECTRON".equals(cardType)) {
                            cardType = "UKE";
                        }
                        if ("CCT_DINERSCLUB".equals(cardType)) {
                            cardType = "DC";
                        }
                        if ("CCT_SWITCH".equals(cardType)) {
                            cardType = "MAESTRO";
                        }
                    }
                    expireDate = creditCard.getString("expireDate");
                    String month = expireDate.substring(0,2);
                    String year = expireDate.substring(5);
                    expireDate = month + year;

                    //getting order details
                    orderId = UtilFormatOut.checkNull((String) context.get("orderId"));
                    processAmount =  (BigDecimal) context.get("processAmount");
                    currency = (String) context.get("currency");

                } else {
                    Debug.logWarning("Payment preference " + opp + " is not a credit card", module);
                }
            }
        } catch (GenericEntityException ex) {
            Debug.logError("Cannot build customer information for " + context + " due to error: " + ex.getMessage(), module);
            return null;
        }

        billingInfo.put("orderId", orderId);
        if(processAmount != null){
            billingInfo.put("amount", processAmount.toString());
        } else {
            billingInfo.put("amount", "");
        }
        billingInfo.put("currency", currency);
        billingInfo.put("description", orderId);
        billingInfo.put("cardNumber", cardNumber);
        billingInfo.put("cardHolder",  nameOnCard);
        billingInfo.put("expiryDate", expireDate);
        billingInfo.put("cardType", cardType);
        billingInfo.put("cv2", securityCode);
        billingInfo.put("billingPostCode", postalCode);
        billingInfo.put("billingAddress", address);

        Debug.logInfo("SagePay billingInfo : " + billingInfo, module);
        Debug.logInfo("SagePay - Exiting buildCustomerBillingInfo", module);

        return billingInfo;
    }

    public static Map<String, Object> ccAuth(DispatchContext dctx, Map<String, Object> context) {
        Debug.logInfo("SagePay - Entered ccAuth", module);
        Debug.logInfo("SagePay ccAuth context : " + context, module);
        Map<String, Object> response = null;
        String orderId = (String) context.get("orderId");
        Locale locale = (Locale) context.get("locale");
        GenericValue orderPaymentPreference = (GenericValue) context.get("orderPaymentPreference");
        
        if (orderPaymentPreference == null) {
            response = ServiceUtil.returnError(UtilProperties.getMessage(resource, "AccountingSagePayOrderPaymenPreferenceIsNull", UtilMisc.toMap("orderId", orderId, "orderPaymentPreference", null), locale));
        } else {
            response = processCardAuthorisationPayment(dctx, context);
        }
        Debug.logInfo("SagePay ccAuth response : " + response, module);
        Debug.logInfo("SagePay - Exiting ccAuth", module);
        return response;
    }


    private static Map<String, Object> processCardAuthorisationPayment(DispatchContext ctx, Map<String, Object> context) {
        Map<String, Object> result = ServiceUtil.returnSuccess();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        Locale locale = (Locale) context.get("locale");
        Map<String, String> billingInfo = buildCustomerBillingInfo(context);
        String paymentGatewayConfigId = (String) context.get("paymentGatewayConfigId");

        try {

            Map<String, Object> paymentResult = dispatcher.runSync("SagePayPaymentAuthentication",
                    UtilMisc.toMap(
                            "paymentGatewayConfigId", paymentGatewayConfigId,
                            "vendorTxCode", billingInfo.get("orderId"),
                            "cardHolder", billingInfo.get("cardHolder"),
                            "cardNumber", billingInfo.get("cardNumber"),
                            "expiryDate", billingInfo.get("expiryDate"),
                            "cardType", billingInfo.get("cardType"),
                            "cv2", billingInfo.get("cv2"),
                            "description", billingInfo.get("description"),
                            "amount", billingInfo.get("amount"),
                            "currency", billingInfo.get("currency"),
                            "billingAddress", billingInfo.get("billingAddress"),
                            "billingPostCode", billingInfo.get("billingPostCode")
                        )
                    );

            Debug.logInfo("SagePay - SagePayPaymentAuthentication result : " + paymentResult, module);

            String transactionType = (String) paymentResult.get("transactionType");
            String status = (String) paymentResult.get("status");
            String statusDetail = (String) paymentResult.get("statusDetail");
            String vpsTxId = (String) paymentResult.get("vpsTxId");
            String securityKey = (String) paymentResult.get("securityKey");
            String txAuthNo = (String) paymentResult.get("txAuthNo");
            String vendorTxCode = (String) paymentResult.get("vendorTxCode");
            String amount = (String) paymentResult.get("amount");

            if (status != null && "OK".equals(status)) {
                Debug.logInfo("SagePay - Payment authorized for order : " + vendorTxCode, module);
                result = SagePayUtil.buildCardAuthorisationPaymentResponse(Boolean.TRUE, txAuthNo, securityKey, new BigDecimal(amount), vpsTxId, vendorTxCode, statusDetail);
                if ("PAYMENT".equals(transactionType)) {
                    Map<String,Object> captureResult = SagePayUtil.buildCardCapturePaymentResponse(Boolean.TRUE, txAuthNo, securityKey, new BigDecimal(amount), vpsTxId, vendorTxCode, statusDetail);
                    result.putAll(captureResult);
                }
            } else if (status != null && "INVALID".equals(status)) {
                Debug.logInfo("SagePay - Invalid authorisation request for order : " + vendorTxCode, module);
                result = SagePayUtil.buildCardAuthorisationPaymentResponse(Boolean.FALSE, null, null, BigDecimal.ZERO, "INVALID", vendorTxCode, statusDetail);
            } else if (status != null && "MALFORMED".equals(status)) {
                Debug.logInfo("SagePay - Malformed authorisation request for order : " + vendorTxCode, module);
                result = SagePayUtil.buildCardAuthorisationPaymentResponse(Boolean.FALSE, null, null, BigDecimal.ZERO, "MALFORMED", vendorTxCode, statusDetail);
            } else if (status != null && "NOTAUTHED".equals(status)) {
                Debug.logInfo("SagePay - NotAuthed authorisation request for order : " + vendorTxCode, module);
                result = SagePayUtil.buildCardAuthorisationPaymentResponse(Boolean.FALSE, null, securityKey, BigDecimal.ZERO, vpsTxId, vendorTxCode, statusDetail);
            } else if (status != null && "REJECTED".equals(status)) {
                Debug.logInfo("SagePay - Rejected authorisation request for order : " + vendorTxCode, module);
                result = SagePayUtil.buildCardAuthorisationPaymentResponse(Boolean.FALSE, null, securityKey, new BigDecimal(amount), vpsTxId, vendorTxCode, statusDetail);
            } else {
                Debug.logInfo("SagePay - Invalid status " + status + " received for order : " + vendorTxCode, module);
                result = SagePayUtil.buildCardAuthorisationPaymentResponse(Boolean.FALSE, null, null, BigDecimal.ZERO, "ERROR", vendorTxCode, statusDetail);
            }
        } catch(GenericServiceException e) {
            Debug.logError(e, "Error in calling SagePayPaymentAuthentication", module);
            result = ServiceUtil.returnError(UtilProperties.getMessage(resource, "AccountingSagePayPaymentAuthorisationException", UtilMisc.toMap("errorString", e.getMessage()), locale));
        }
        return result;
    }

    public static Map<String, Object> ccCapture(DispatchContext ctx, Map<String, Object> context) {
        Debug.logInfo("SagePay - Entered ccCapture", module);
        Debug.logInfo("SagePay ccCapture context : " + context, module);
        GenericValue orderPaymentPreference = (GenericValue) context.get("orderPaymentPreference");
        GenericValue authTransaction = PaymentGatewayServices.getAuthTransaction(orderPaymentPreference);
        context.put("authTransaction", authTransaction);
        Map<String, Object> response = processCardCapturePayment(ctx, context);

        Debug.logInfo("SagePay ccCapture response : " + response, module);
        Debug.logInfo("SagePay - Exiting ccCapture", module);

        return response;
    }

    private static Map<String, Object> processCardCapturePayment(DispatchContext ctx, Map<String, Object> context) {
        Map<String, Object> result = ServiceUtil.returnSuccess();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        Locale locale = (Locale) context.get("locale");
        String paymentGatewayConfigId = (String) context.get("paymentGatewayConfigId");
        GenericValue authTransaction = (GenericValue) context.get("authTransaction");
        BigDecimal amount = (BigDecimal) context.get("captureAmount");
        String vendorTxCode = (String) authTransaction.get("altReference");
        String vpsTxId = (String) authTransaction.get("referenceNum");
        String securityKey = (String) authTransaction.get("gatewayFlag");
        String txAuthCode = (String) authTransaction.get("gatewayCode");

        try {

            Map<String, Object> paymentResult = dispatcher.runSync("SagePayPaymentAuthorisation",
                    UtilMisc.toMap(
                            "paymentGatewayConfigId", paymentGatewayConfigId,
                            "vendorTxCode", vendorTxCode,
                            "vpsTxId", vpsTxId,
                            "securityKey", securityKey,
                            "txAuthNo", txAuthCode,
                            "amount", amount.toString()
                        )
                    );
            Debug.logInfo("SagePay - SagePayPaymentAuthorisation result : " + paymentResult, module);
            String status = (String) paymentResult.get("status");
            String statusDetail = (String) paymentResult.get("statusDetail");
            if (status != null && "OK".equals(status)) {
                Debug.logInfo("SagePay Payment Released for Order : " + vendorTxCode, module);
                result = SagePayUtil.buildCardCapturePaymentResponse(Boolean.TRUE, txAuthCode, securityKey, amount, vpsTxId, vendorTxCode, statusDetail);
            } else {
                Debug.logInfo("SagePay - Invalid status " + status + " received for order : " + vendorTxCode, module);
                result = SagePayUtil.buildCardCapturePaymentResponse(Boolean.FALSE, txAuthCode, securityKey, amount, vpsTxId, vendorTxCode, statusDetail);
            }
        } catch(GenericServiceException e) {
            Debug.logError(e, "Error in calling SagePayPaymentAuthorisation", module);
            result = ServiceUtil.returnError(UtilProperties.getMessage(resource, "AccountingSagePayPaymentAuthorisationException", UtilMisc.toMap("errorString", e.getMessage()), locale));
        }
        return result;
    }

    public static Map<String, Object> ccRefund(DispatchContext ctx, Map<String, Object> context) {
        Debug.logInfo("SagePay - Entered ccRefund", module);
        Debug.logInfo("SagePay ccRefund context : " + context, module);
        Locale locale = (Locale) context.get("locale");
        GenericValue orderPaymentPreference = (GenericValue) context.get("orderPaymentPreference");
        GenericValue captureTransaction = PaymentGatewayServices.getCaptureTransaction(orderPaymentPreference);
        if (captureTransaction == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "AccountingPaymentTransactionAuthorizationNotFoundCannotRefund", locale));
        }
        Debug.logInfo("SagePay ccRefund captureTransaction : " + captureTransaction, module);
        GenericValue creditCard = null;
        try {
            creditCard = orderPaymentPreference.getRelatedOne("CreditCard", false);
        } catch (GenericEntityException e) {
            Debug.logError(e, "Error getting CreditCard for OrderPaymentPreference : " + orderPaymentPreference, module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "AccountingPaymentUnableToGetCCInfo", locale) + " " + orderPaymentPreference);
        }
        context.put("creditCard",creditCard);
        context.put("captureTransaction", captureTransaction);

        List<GenericValue> authTransactions = PaymentGatewayServices.getAuthTransactions(orderPaymentPreference);

        EntityCondition authCondition = EntityCondition.makeCondition("paymentServiceTypeEnumId", "PRDS_PAY_AUTH");
        List<GenericValue> authTransactions1 = EntityUtil.filterByCondition(authTransactions, authCondition);

        GenericValue authTransaction = EntityUtil.getFirst(authTransactions1);

        Timestamp authTime = authTransaction.getTimestamp("transactionDate");
        Calendar authCal = Calendar.getInstance();
        authCal.setTimeInMillis(authTime.getTime());

        Timestamp nowTime = UtilDateTime.nowTimestamp();
        Calendar nowCal = Calendar.getInstance();
        nowCal.setTimeInMillis(nowTime.getTime());

        Calendar yesterday = Calendar.getInstance();
        yesterday.set(nowCal.get(Calendar.YEAR), nowCal.get(Calendar.MONTH), nowCal.get(Calendar.DATE), 23, 59, 59);
        yesterday.add(Calendar.DAY_OF_YEAR, -1);

        Map<String, Object> response = null;

        if (authCal.before(yesterday)) {
            Debug.logInfo("SagePay - Calling Refund for Refund", module);
            response = processCardRefundPayment(ctx, context);
        } else {

            Calendar cal = Calendar.getInstance();
            cal.set(nowCal.get(Calendar.YEAR), nowCal.get(Calendar.MONTH), nowCal.get(Calendar.DATE), 23, 49, 59);

            if (authCal.before(cal)) {
                Debug.logInfo("SagePay - Calling Void for Refund", module);
                response = processCardVoidPayment(ctx, context);
            } else {
                Debug.logInfo("SagePay - Calling Refund for Refund", module);
                response = processCardRefundPayment(ctx, context);
            }
        }

        Debug.logInfo("SagePay ccRefund response : " + response, module);
        return response;
    }

    private static Map<String, Object> processCardRefundPayment(DispatchContext ctx, Map<String, Object> context) {
        Map<String, Object> result = ServiceUtil.returnSuccess();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        Locale locale = (Locale) context.get("locale");
        String paymentGatewayConfigId = (String) context.get("paymentGatewayConfigId");
        GenericValue captureTransaction = (GenericValue) context.get("captureTransaction");
        BigDecimal amount = (BigDecimal) context.get("refundAmount");

        String orderId = (String) captureTransaction.get("altReference");
        orderId = "R" + orderId;

        try {

            Map<String, Object> paymentResult = dispatcher.runSync("SagePayPaymentRefund",
                    UtilMisc.toMap(
                            "paymentGatewayConfigId", paymentGatewayConfigId,
                            "vendorTxCode", orderId,
                            "amount", amount.toString(),
                            "currency", "GBP",
                            "description", orderId,
                            "relatedVPSTxId", captureTransaction.get("referenceNum"),
                            "relatedVendorTxCode", captureTransaction.get("altReference"),
                            "relatedSecurityKey", captureTransaction.get("gatewayFlag"),
                            "relatedTxAuthNo", captureTransaction.get("gatewayCode")
                        )
                    );
            Debug.logInfo("SagePay - SagePayPaymentRefund result : " + paymentResult, module);

            String status = (String) paymentResult.get("status");
            String statusDetail = (String) paymentResult.get("statusDetail");
            String vpsTxId = (String) paymentResult.get("vpsTxId");
            String txAuthNo = (String) paymentResult.get("txAuthNo");

            if (status != null && "OK".equals(status)) {
                Debug.logInfo("SagePay Payment Refunded for Order : " + orderId, module);
                result = SagePayUtil.buildCardRefundPaymentResponse(Boolean.TRUE, txAuthNo, amount, vpsTxId, orderId, statusDetail);
            } else {
                Debug.logInfo("SagePay - Invalid status " + status + " received for order : " + orderId, module);
                result = SagePayUtil.buildCardRefundPaymentResponse(Boolean.FALSE, null, BigDecimal.ZERO, status, orderId, statusDetail);
            }

        } catch(GenericServiceException e) {
            Debug.logError(e, "Error in calling SagePayPaymentRefund", module);
            result = ServiceUtil.returnError(UtilProperties.getMessage(resource, "AccountingSagePayPaymentRefundException", UtilMisc.toMap("errorString", e.getMessage()), locale));
        }

        return result;
    }

    private static Map<String, Object> processCardVoidPayment(DispatchContext ctx, Map<String, Object> context) {
        Map<String, Object> result = ServiceUtil.returnSuccess();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        Locale locale = (Locale) context.get("locale");
        String paymentGatewayConfigId = (String) context.get("paymentGatewayConfigId");
        GenericValue captureTransaction = (GenericValue) context.get("captureTransaction");
        BigDecimal amount = (BigDecimal) context.get("refundAmount");
        String orderId = (String) captureTransaction.get("altReference");

        try {
            Map<String, Object> paymentResult = dispatcher.runSync("SagePayPaymentVoid",
                    UtilMisc.toMap(
                            "paymentGatewayConfigId", paymentGatewayConfigId,
                            "vendorTxCode", captureTransaction.get("altReference"),
                            "vpsTxId", captureTransaction.get("referenceNum"),
                            "securityKey", captureTransaction.get("gatewayFlag"),
                            "txAuthNo", captureTransaction.get("gatewayCode")
                        )
                    );

            Debug.logInfo("SagePay - SagePayPaymentVoid result : " + paymentResult, module);

            String status = (String) paymentResult.get("status");
            String statusDetail = (String) paymentResult.get("statusDetail");

            if (status != null && "OK".equals(status)) {
                Debug.logInfo("SagePay Payment Voided for Order : " + orderId, module);
                result = SagePayUtil.buildCardVoidPaymentResponse(Boolean.TRUE, amount, "SUCCESS", orderId, statusDetail);
            } else if (status != null && "MALFORMED".equals(status)) {
                Debug.logInfo("SagePay - Malformed void request for order : " + orderId, module);
                result = SagePayUtil.buildCardVoidPaymentResponse(Boolean.FALSE, BigDecimal.ZERO, "MALFORMED", orderId, statusDetail);
            } else if (status != null && "INVALID".equals(status)){
                Debug.logInfo("SagePay - Invalid void request for order : " + orderId, module);
                result = SagePayUtil.buildCardVoidPaymentResponse(Boolean.FALSE, BigDecimal.ZERO, "INVALID", orderId, statusDetail);
            } else if (status != null && "ERROR".equals(status)){
                Debug.logInfo("SagePay - Error in void request for order : " + orderId, module);
                result = SagePayUtil.buildCardVoidPaymentResponse(Boolean.FALSE, BigDecimal.ZERO, "ERROR", orderId, statusDetail);
            }

        } catch(GenericServiceException e) {
            Debug.logError(e, "Error in calling SagePayPaymentVoid", module);
            result = ServiceUtil.returnError(UtilProperties.getMessage(resource, "AccountingSagePayPaymentVoidException", UtilMisc.toMap("errorString", e.getMessage()), locale));
        }
        return result;
    }

    public static Map<String, Object> ccRelease(DispatchContext ctx, Map<String, Object> context) {
        Debug.logInfo("SagePay - Entered ccRelease", module);
        Debug.logInfo("SagePay ccRelease context : " + context, module);
        Locale locale = (Locale) context.get("locale");
        GenericValue orderPaymentPreference = (GenericValue) context.get("orderPaymentPreference");

        GenericValue authTransaction = PaymentGatewayServices.getAuthTransaction(orderPaymentPreference);
        if (authTransaction == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "AccountingPaymentTransactionAuthorizationNotFoundCannotRelease", locale));
        }
        context.put("authTransaction", authTransaction);

        Map<String, Object> response = processCardReleasePayment(ctx, context);
        Debug.logInfo("SagePay ccRelease response : " + response, module);
        return response;
    }

    private static Map<String, Object> processCardReleasePayment(DispatchContext ctx, Map<String, Object> context) {
        Map<String, Object> result = ServiceUtil.returnSuccess();
        Locale locale = (Locale) context.get("locale");
        LocalDispatcher dispatcher = ctx.getDispatcher();

        String paymentGatewayConfigId = (String) context.get("paymentGatewayConfigId");
        BigDecimal amount = (BigDecimal) context.get("releaseAmount");

        GenericValue authTransaction = (GenericValue) context.get("authTransaction");
        String orderId = (String) authTransaction.get("altReference");
        String refNum = (String) authTransaction.get("referenceNum");

        try {
            Map<String, Object> paymentResult = dispatcher.runSync("SagePayPaymentRelease",
                    UtilMisc.toMap(
                            "paymentGatewayConfigId", paymentGatewayConfigId,
                            "vendorTxCode", orderId,
                            "releaseAmount", amount.toString(),
                            "vpsTxId", refNum,
                            "securityKey", authTransaction.get("gatewayFlag"),
                            "txAuthNo", authTransaction.get("gatewayCode")
                        )
                    );

            Debug.logInfo("SagePay - SagePayPaymentRelease result : " + paymentResult, module);

            String status = (String) paymentResult.get("status");
            String statusDetail = (String) paymentResult.get("statusDetail");

            if (status != null && "OK".equals(status)) {
                Debug.logInfo("SagePay Payment Released for Order : " + orderId, module);
                result = SagePayUtil.buildCardReleasePaymentResponse(Boolean.TRUE, null, amount, refNum, orderId, statusDetail);
            } else {
                Debug.logInfo("SagePay - Invalid status " + status + " received for order : " + orderId, module);
                result = SagePayUtil.buildCardReleasePaymentResponse(Boolean.FALSE, null, amount, refNum, orderId, statusDetail);
            }

        } catch(GenericServiceException e) {
            Debug.logError(e, "Error in calling SagePayPaymentRelease", module);
            result = ServiceUtil.returnError(UtilProperties.getMessage(resource, "AccountingSagePayPaymentReleaseException", UtilMisc.toMap("errorString", e.getMessage()), locale));
        }

        return result;
    }

}
