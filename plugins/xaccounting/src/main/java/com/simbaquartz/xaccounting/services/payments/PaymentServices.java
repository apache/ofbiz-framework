package com.simbaquartz.xaccounting.services.payments;

import com.simbaquartz.xaccounting.services.AxAccountingHelper;
import com.simbaquartz.xaccounting.services.AxInvoiceWorker;
import com.fidelissd.zcp.xcommon.collections.FastMap;
import org.apache.ofbiz.accounting.payment.PaymentWorker;
import org.apache.ofbiz.base.util.*;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.transaction.TransactionUtil;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.security.Security;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.*;

public class PaymentServices {

    private static String module = "AccountingActivityServices";

    /**
     * Register a create new payment.
     *
     * @param dctx
     * @param context
     * @return
     */
    public static Map<String, Object> fsdCreatePaymentAndApplication(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String taxAuthGeoId = (String) context.get("taxAuthGeoId");
        String statusId = (String) context.get("statusId");
        String roleTypeIdTo = (String) context.get("roleTypeIdTo");
        String quoteId = (String) context.get("quoteId");
        String paymentTypeId = (String) context.get("paymentTypeId");
        String paymentRefNum = (String) context.get("paymentRefNum");
        String paymentPreferenceId = (String) context.get("paymentPreferenceId");
        String paymentMethodTypeId = (String) context.get("paymentMethodTypeId");
        String paymentMethodId = (String) context.get("paymentMethodId");
        String paymentId = (String) context.get("paymentId");
        String paymentGatewayResponseId = (String) context.get("paymentGatewayResponseId");
        String partyIdTo = (String) context.get("partyIdTo");
        String partyIdFrom = (String) context.get("partyIdFrom");
        String overrideGlAccountId = (String) context.get("overrideGlAccountId");
        String invoiceItemSeqId = (String) context.get("invoiceItemSeqId");
        String invoiceId = (String) context.get("invoiceId");
        String finAccountTransId = (String) context.get("finAccountTransId");
        String currencyUomId  = (String) context.get("currencyUomId");
        String comments = (String) context.get("comments");
        String billingAccountId = (String) context.get("billingAccountId");
        String actualCurrencyUomId = (String) context.get("actualCurrencyUomId");
        Timestamp effectiveDate = (Timestamp) context.get("effectiveDate");
        BigDecimal amount = (BigDecimal) context.get("amount");
        BigDecimal actualCurrencyAmount = (BigDecimal) context.get("actualCurrencyAmount");
        String paymentApplicationId = "";
        // create payment
        try {
            Map<String, Object> createPaymentCtx = FastMap.newInstance();
            createPaymentCtx.put("userLogin", userLogin);
            createPaymentCtx.put("actualCurrencyAmount", actualCurrencyAmount);
            createPaymentCtx.put("actualCurrencyUomId", actualCurrencyUomId);
            createPaymentCtx.put("amount", amount);
            createPaymentCtx.put("comments", comments);
            createPaymentCtx.put("currencyUomId", currencyUomId);
            createPaymentCtx.put("effectiveDate", effectiveDate);
            createPaymentCtx.put("finAccountTransId", finAccountTransId);
            createPaymentCtx.put("overrideGlAccountId", overrideGlAccountId);
            createPaymentCtx.put("partyIdFrom", partyIdFrom);
            createPaymentCtx.put("partyIdTo", partyIdTo);
            createPaymentCtx.put("paymentGatewayResponseId", paymentGatewayResponseId);
            createPaymentCtx.put("paymentId", paymentId);
            createPaymentCtx.put("paymentMethodId", paymentMethodId);
            createPaymentCtx.put("paymentMethodTypeId", paymentMethodTypeId);
            createPaymentCtx.put("paymentPreferenceId", paymentPreferenceId);
            createPaymentCtx.put("paymentRefNum", paymentRefNum);
            createPaymentCtx.put("paymentTypeId", paymentTypeId);
            createPaymentCtx.put("roleTypeIdTo", roleTypeIdTo);
            createPaymentCtx.put("statusId", statusId);

            Map<String, Object> createPaymentCtxResponse = dispatcher.runSync("fsdCreatePayment", createPaymentCtx);
            if (!ServiceUtil.isSuccess(createPaymentCtxResponse)) {
                return createPaymentCtxResponse;
            }

            paymentId = (String) createPaymentCtxResponse.get("paymentId");
        } catch (Exception e) {
            e.printStackTrace();
            Debug.logError(e, module);
            return  ServiceUtil.returnError(e.getMessage());
        }

        //create payment application
        try {
            Map<String, Object> createPaymentAppCtx = FastMap.newInstance();
            createPaymentAppCtx.put("userLogin", userLogin);
            createPaymentAppCtx.put("amountApplied", amount);
            createPaymentAppCtx.put("billingAccountId", billingAccountId);
            createPaymentAppCtx.put("paymentId", paymentId);
            createPaymentAppCtx.put("invoiceId", invoiceId);
            createPaymentAppCtx.put("quoteId", quoteId);
            createPaymentAppCtx.put("taxAuthGeoId", taxAuthGeoId);
            Map<String, Object> createPaymentAppCtxResponse = dispatcher.runSync("fsdCreatePaymentApplication", createPaymentAppCtx);
            if (!ServiceUtil.isSuccess(createPaymentAppCtxResponse)) {
                return createPaymentAppCtxResponse;
            }

            paymentApplicationId = (String) createPaymentAppCtxResponse.get("paymentApplicationId");
        } catch (Exception e) {
            e.printStackTrace();
            Debug.logError(e, module);
            return  ServiceUtil.returnError(e.getMessage());
        }

        serviceResult.put("paymentId", paymentId);
        serviceResult.put("paymentApplicationId", paymentApplicationId);
        return serviceResult;
    }

    /**
     * Register a create new payment application.
     *
     * @param dctx
     * @param context
     * @return
     */
    public static Map<String, Object> fsdCreatePaymentApplication(DispatchContext dctx, Map<String, Object> context) throws GenericEntityException, GenericServiceException {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String taxAuthGeoId = (String) context.get("taxAuthGeoId");
        String billingAccountId = (String) context.get("billingAccountId");
        String invoiceId = (String) context.get("invoiceId");
        String toPaymentId = (String) context.get("toPaymentId");
        String paymentId = (String) context.get("paymentId");
        String quoteId = (String) context.get("quoteId");
        BigDecimal amountApplied = (BigDecimal) context.get("amountApplied");

        if(UtilValidate.isEmpty(invoiceId) && UtilValidate.isEmpty(taxAuthGeoId) && UtilValidate.isEmpty(billingAccountId) && UtilValidate.isEmpty(toPaymentId) && UtilValidate.isEmpty(quoteId)){
            return ServiceUtil.returnError(UtilProperties.getMessage("AccountingUiLabels", "AccountingPaymentApplicationParameterMissing", Locale.getDefault()));
        }

        GenericValue paymentAppl = delegator.makeValue("PaymentApplication");
        paymentAppl.set("paymentId", paymentId);
        paymentAppl.set("invoiceId", invoiceId);
        paymentAppl.put("quoteId", quoteId);
        paymentAppl.set("taxAuthGeoId", taxAuthGeoId);
        paymentAppl.set("toPaymentId", toPaymentId);

        GenericValue payment = EntityQuery.use(delegator).from("Payment").where("paymentId", paymentId).queryOne();
        if(UtilValidate.isEmpty(payment)){
            return ServiceUtil.returnError(UtilProperties.getMessage("AccountingUiLabels", "AccountingPaymentApplicationParameterMissing", Locale.getDefault()));
        }

        BigDecimal notAppliedPayment = PaymentWorker.getPaymentNotApplied(payment);

        if(UtilValidate.isNotEmpty(invoiceId)){
            GenericValue invoice = EntityQuery.use(delegator).from("Invoice").where("invoiceId", invoiceId).queryOne();
            String paymentCurrencyUomId = payment.getString("currencyUomId");
            String invoiceCurrencyUomId = invoice.getString("currencyUomId");
            String paymentActualCurrencyUomId = payment.getString("actualCurrencyUomId");
            if(!paymentCurrencyUomId.equals(invoiceCurrencyUomId) && !paymentActualCurrencyUomId.equals(invoiceCurrencyUomId)){
                return ServiceUtil.returnError(UtilProperties.getMessage("AccountingUiLabels", "AccountingCurrenciesOfInvoiceAndPaymentNotCompatible", Locale.getDefault()));
            }

            if(!invoiceCurrencyUomId.equals(paymentCurrencyUomId) && invoiceCurrencyUomId.equals(paymentActualCurrencyUomId)){
                Boolean actual = true;
               //if required get the payment amount in foreign currency (local we already have)
                 notAppliedPayment = PaymentWorker.getPaymentNotApplied(payment, actual);
            }

            //get the amount that has not been applied yet for the invoice (outstanding amount)
           BigDecimal notAppliedInvoice = AxInvoiceWorker.getInvoiceNotApplied(delegator, invoice);
            if (notAppliedInvoice.compareTo(notAppliedPayment) <= 0){
                paymentAppl.put("amountApplied", notAppliedInvoice);
            }else{
                paymentAppl.put("amountApplied", notAppliedPayment);
            }

            if(UtilValidate.isNotEmpty(invoice.getBigDecimal("billingAccountId"))){
                paymentAppl.put("billingAccountId", invoice.getBigDecimal("billingAccountId"));
            }
        }

        if(UtilValidate.isNotEmpty(toPaymentId)) {
            //get the to payment and check the parent types are compatible
            GenericValue toPayment = EntityQuery.use(delegator).from("Payment").where("paymentId", toPaymentId).queryOne();
            GenericValue toPaymentType = EntityQuery.use(delegator).from("PaymentType").where("paymentTypeId", toPayment.getString("paymentTypeId")).queryOne();
            GenericValue paymentType = EntityQuery.use(delegator).from("PaymentType").where("paymentTypeId", payment.getString("paymentTypeId")).queryOne();

            //when amount not provided use the the lowest value available
            if (UtilValidate.isEmpty(amountApplied)) {
                notAppliedPayment = PaymentWorker.getPaymentNotApplied(payment);
                BigDecimal notAppliedToPayment = PaymentWorker.getPaymentNotApplied(toPayment);
                if (notAppliedPayment.compareTo(notAppliedToPayment) < 0) {
                    paymentAppl.put("amountApplied", notAppliedPayment);
                } else {
                    paymentAppl.put("amountApplied", notAppliedToPayment);
                }
            }
        }

        if(UtilValidate.isNotEmpty(billingAccountId)){
            if(UtilValidate.isEmpty(paymentAppl.getBigDecimal("amountApplied"))){
                paymentAppl.put("amountApplied", notAppliedPayment);
            }
        }

        if(UtilValidate.isNotEmpty(taxAuthGeoId)){
            if(UtilValidate.isEmpty(paymentAppl.getBigDecimal("amountApplied"))){
                paymentAppl.put("amountApplied", notAppliedPayment);
            }
        }

        if(UtilValidate.isNotEmpty(quoteId)){
            paymentAppl.put("quoteId", quoteId);
            if(UtilValidate.isEmpty(paymentAppl.getBigDecimal("amountApplied"))){
                paymentAppl.put("amountApplied", notAppliedPayment);
            }
        }

        String paymentApplicationId = delegator.getNextSeqId("PaymentApplication");
        paymentAppl.put("paymentApplicationId", paymentApplicationId);

        serviceResult.put("amountApplied", paymentAppl.getBigDecimal("amountApplied"));
        serviceResult.put("paymentApplicationId", paymentAppl.getString("paymentApplicationId"));
        delegator.create(paymentAppl);
        serviceResult.put("paymentTypeId", payment.getString("paymentTypeId"));

        return serviceResult;
    }


    /**
     * Register a create new payment.
     *
     * @param dctx
     * @param context
     * @return
     */
    public static Map<String, Object> fsdCreatePayment(DispatchContext dctx, Map<String, Object> context) throws GenericEntityException, GenericServiceException {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        Security security = dctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String userLoginPartyId = (String) userLogin.get("partyId");
        String partyIdTo = (String) context.get("partyIdTo");
        String partyIdFrom = (String) context.get("partyIdFrom");
        String paymentId = (String) context.get("paymentId");
        String statusId = (String) context.get("statusId");
        String paymentMethodId = (String) context.get("paymentMethodId");
        String paymentMethodTypeId = (String) context.get("paymentMethodTypeId");
        String paymentPreferenceId = (String) context.get("paymentPreferenceId");
        String paymentTypeId = (String) context.get("paymentTypeId");
        String paymentGatewayResponseId = (String) context.get("paymentGatewayResponseId");
        String roleTypeIdTo = (String) context.get("roleTypeIdTo");
        Timestamp effectiveDate = (Timestamp) context.get("effectiveDate");
        String paymentRefNum = (String) context.get("paymentRefNum");
        String currencyUomId = (String) context.get("currencyUomId");
        String comments = (String) context.get("comments");
        String finAccountTransId = (String) context.get("finAccountTransId");
        String overrideGlAccountId = (String) context.get("overrideGlAccountId");
        String actualCurrencyUomId = (String) context.get("actualCurrencyUomId");
        BigDecimal amount = (BigDecimal) context.get("amount");
        BigDecimal actualCurrencyAmount = (BigDecimal) context.get("actualCurrencyAmount");
        if (!security.hasEntityPermission("PAY_INFO", "_CREATE", userLogin) && !security.hasEntityPermission("ACCOUNTING", "_CREATE", userLogin) && !userLoginPartyId.equals(partyIdFrom) && !userLoginPartyId.equals(partyIdTo)) {
            return ServiceUtil.returnError(UtilProperties.getMessage("AccountingUiLabels", "AccountingCreatePaymentPermissionError", Locale.getDefault()));
        }

        GenericValue payment = delegator.makeValue("Payment");
        if(UtilValidate.isEmpty(paymentId)){
            paymentId = delegator.getNextSeqId("Payment");
            payment.set("paymentId", paymentId);
        }else{
            payment.set("paymentId", paymentId);
        }
        serviceResult.put("paymentId", paymentId);

        if(UtilValidate.isEmpty(statusId)){
            payment.set("statusId", "PMNT_NOT_PAID");
        }else{
            payment.set("statusId", statusId);
        }

        if(UtilValidate.isNotEmpty(paymentMethodId)){
            GenericValue paymentMethod = EntityQuery.use(delegator).from("PaymentMethod").where("paymentMethodId", paymentMethodId).queryOne();
            if(UtilValidate.isEmpty(paymentMethodTypeId) || (UtilValidate.isNotEmpty(paymentMethodTypeId) && !paymentMethodTypeId.equals(paymentMethod.getString("paymentMethodTypeId")))){
                Debug.logInfo("Replacing passed payment method type [" + paymentMethodTypeId +" with payment method type ["+ paymentMethod.getString("paymentMethodTypeId") + "for payment method ["+paymentMethodId, module);
                paymentMethodTypeId = paymentMethod.getString("paymentMethodTypeId");
            }
        }

        if(UtilValidate.isNotEmpty(paymentPreferenceId)){
            GenericValue orderPaymentPreference = EntityQuery.use(delegator).from("OrderPaymentPreference").where("orderPaymentPreferenceId", paymentPreferenceId).queryOne();
            if(UtilValidate.isEmpty(paymentMethodId)){
                paymentMethodId = orderPaymentPreference.getString("paymentMethodId");
            }if(UtilValidate.isEmpty(paymentMethodTypeId)){
                paymentMethodTypeId = orderPaymentPreference.getString("paymentMethodTypeId");
            }
        }

        if(UtilValidate.isEmpty(paymentMethodTypeId)){
            return ServiceUtil.returnError(UtilProperties.getMessage("AccountingUiLabels", "AccountingPaymentMethodIdPaymentMethodTypeIdNullError", Locale.getDefault()));
        }

        payment.set("partyIdTo", partyIdTo);
        payment.set("partyIdFrom", partyIdFrom);
        payment.set("paymentTypeId", paymentTypeId);
        payment.set("paymentMethodTypeId", paymentMethodTypeId);
        payment.set("paymentMethodId", paymentMethodId);
        payment.set("paymentGatewayResponseId", paymentGatewayResponseId);
        payment.set("paymentPreferenceId", paymentPreferenceId);
        payment.set("roleTypeIdTo", roleTypeIdTo);
        if(UtilValidate.isNotEmpty(effectiveDate)) {
            payment.set("effectiveDate", effectiveDate);
        }else{
            payment.set("effectiveDate", UtilDateTime.nowTimestamp());
        }
        payment.set("paymentRefNum", paymentRefNum);
        payment.set("amount", amount);
        payment.set("currencyUomId", currencyUomId);
        payment.set("comments", comments);
        payment.set("finAccountTransId", finAccountTransId);
        payment.set("overrideGlAccountId", overrideGlAccountId);
        payment.set("actualCurrencyAmount", actualCurrencyAmount);
        payment.set("actualCurrencyUomId", actualCurrencyUomId);
        payment.set("createdByUserLogin", userLogin.getString("partyId"));
        delegator.create(payment);
        return serviceResult;
    }

    /**
     * Set payment status.
     *
     * @param dctx
     * @param context
     * @return
     */
    public static Map<String, Object> setFsdPaymentStatus(DispatchContext dctx, Map<String, Object> context) throws GenericEntityException, GenericServiceException {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        Security security = dctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String paymentId = (String) context.get("paymentId");
        String statusId = (String) context.get("statusId");
        if (!security.hasEntityPermission("ACCOUNTING_ROLE", "_UPDATE", userLogin) && !security.hasEntityPermission("ACCOUNTING", "_UPDATE", userLogin)) {
            return ServiceUtil.returnError(UtilProperties.getMessage("AccountingUiLabels", "AccountingPermissionError", Locale.getDefault()));
        }

        GenericValue payment = EntityQuery.use(delegator).from("Payment").where("paymentId", paymentId).queryOne();
        GenericValue statusItem = EntityQuery.use(delegator).from("StatusItem").where("statusId", statusId).queryOne();
        serviceResult.put("oldStatusId", payment.getString("statusId"));
        String paymentStatusId = (String) payment.getString("statusId");

        if(!paymentStatusId.equals(statusId)) {
            GenericValue statusChange = EntityQuery.use(delegator).from("StatusValidChange").where("statusId", paymentStatusId, "statusIdTo", statusId).queryOne();
            if (UtilValidate.isEmpty(statusChange)) {
                Debug.logError("Cannot change from " + paymentStatusId + " to " + statusId, module);
                return ServiceUtil.returnError(UtilProperties.getMessage("AccountingUiLabels", "AccountingPSInvalidStatusChange", Locale.getDefault()));
            } else {
                //payment method is mandatory when set to sent or received.
                if (statusId.equals("PMNT_RECEIVED") || statusId.equals("PMNT_SENT")) {
                    if (UtilValidate.isEmpty(payment.getString("paymentMethodId"))) {
                        Debug.logError("Cannot set status to " + statusId + " on payment " + paymentId + ": payment method is missing", module);
                        return ServiceUtil.returnError(UtilProperties.getMessage("AccountingUiLabels", "AccountingMissingPaymentMethod", Locale.getDefault()));
                    }
                }
                //check if the payment fully applied when set to confirmed
                if (statusId.equals("PMNT_CONFIRMED")) {
                    BigDecimal notYetApplied = PaymentWorker.getPaymentNotApplied(payment);
                    if (notYetApplied.compareTo(BigDecimal.ZERO) >= 0) {
                        Debug.logError("Cannot change from " + paymentStatusId + " to " + statusId + ", payment not fully applied: " + notYetApplied, module);
                        return ServiceUtil.returnError(UtilProperties.getMessage("AccountingUiLabels", "AccountingPSNotConfirmedNotFullyApplied", Locale.getDefault()));
                    }
                }

                if (statusId.equals("PMNT_CANCELLED")) {
                    //if new status is cancelled delete existing payment applications.
                    List<GenericValue> paymentApplications = payment.getRelated("PaymentApplication", null, null, false);
                    for (GenericValue paymentApplication : paymentApplications) {
                        Map<String, Object> removePaymentApplicationMap = new HashMap<String, Object>();
                        removePaymentApplicationMap.put("userLogin", userLogin);
                        removePaymentApplicationMap.put("paymentApplicationId", paymentApplication.getString("paymentApplicationId"));
                        dispatcher.runSync("removePaymentApplication", removePaymentApplicationMap);
                    }
                    //if new status is cancelled and the payment is associated to an OrderPaymentPreference, update the status of that record too.
                    GenericValue orderPaymentPreference = payment.getRelatedOne("OrderPaymentPreference", false);
                    if (UtilValidate.isNotEmpty(orderPaymentPreference)) {
                        Map<String, Object> updateOrderPaymentPreferenceMap = new HashMap<String, Object>();
                        updateOrderPaymentPreferenceMap.put("userLogin", userLogin);
                        updateOrderPaymentPreferenceMap.put("orderPaymentPreferenceId", orderPaymentPreference.getString("orderPaymentPreferenceId"));
                        updateOrderPaymentPreferenceMap.put("statusId", "PAYMENT_CANCELLED");
                        dispatcher.runSync("updateOrderPaymentPreference", updateOrderPaymentPreferenceMap);
                    }
                }

                if (statusId.equals("PMNT_RECEIVED")) {
                    BigDecimal totalAmountApplied = BigDecimal.ZERO;

                    GenericValue paymentAppl = EntityQuery.use(delegator).from("PaymentApplication").where("paymentId", paymentId).queryOne();
                    String invoiceId = (String) paymentAppl.getString("invoiceId");
                    List<GenericValue> paymentApplications = EntityQuery.use(delegator).from("PaymentApplication").where("invoiceId", invoiceId).queryList();
                    Boolean isInvalidEffectiveDate = false;
                    Timestamp currentDate = UtilDateTime.nowTimestamp();
                    for (GenericValue paymentApplication : paymentApplications) {
                        totalAmountApplied = totalAmountApplied.add(paymentApplication.getBigDecimal("amountApplied"));
                        String paymentAppId = (String) paymentApplication.getString("paymentId");
                        GenericValue paymentRec = EntityQuery.use(delegator).from("Payment").where("paymentId", paymentAppId).queryOne();
                        Timestamp effectiveDate = (Timestamp) paymentRec.get("effectiveDate");
                        long t1 = currentDate.getTime();
                        long t2 = effectiveDate.getTime();
                        if(t2>t1) {
                            isInvalidEffectiveDate = true;
                        }
                    }
                    // get invoice total
                    GenericValue invoiceRecord = EntityQuery.use(delegator).from("Invoice").where("invoiceId", invoiceId).queryFirst();
                    BigDecimal invoiceTotal = AxInvoiceWorker.getInvoiceTotal(invoiceRecord);
                    if(!isInvalidEffectiveDate) {
                        if (invoiceTotal.equals(totalAmountApplied)) {
                            // mark invoice as paid
                            invoiceRecord.set("statusId", "INVOICE_PAID");
                            delegator.store(invoiceRecord);
                        }
                    }
                }

                //everything ok so now change the status field
                payment.set("statusId", statusId);
                delegator.store(payment);
            }
        }
        return serviceResult;
    }

    /**
     * Check payment status.
     *
     * @param dctx
     * @param context
     * @return
     */
    public static Map <String, Object> checkPaymentPendingStatus(DispatchContext dctx, Map <String, Object> context) throws GenericEntityException, GenericServiceException {
        Map <String, Object> serviceResult = ServiceUtil.returnSuccess();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Timestamp currentDate = UtilDateTime.nowTimestamp();
       List <GenericValue> invoiceRecords = EntityQuery.use(delegator).from("Invoice").where(EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "INVOICE_PAID")).orderBy("createdStamp DESC").queryList();

        for (GenericValue invoiceRecord : invoiceRecords) {
            TransactionUtil.begin();
            Boolean isValidEffectiveDate = false;
            BigDecimal totalAmountApplied = BigDecimal.ZERO;
            Timestamp effectivePaymentDate = null;
            String invoiceId = (String) invoiceRecord.getString("invoiceId");
            List <GenericValue> paymentApplications = EntityQuery.use(delegator).from("PaymentApplication").where("invoiceId", invoiceRecord.get("invoiceId")).queryList();
            for (GenericValue paymentApplication : paymentApplications) {
                totalAmountApplied = totalAmountApplied.add(paymentApplication.getBigDecimal("amountApplied"));
                String paymentAppId = (String) paymentApplication.getString("paymentId");
                GenericValue paymentRec = EntityQuery.use(delegator).from("Payment").where("paymentId", paymentAppId).queryOne();
                effectivePaymentDate = (Timestamp) paymentRec.get("effectiveDate");
                if (currentDate.after(effectivePaymentDate)) {
                    isValidEffectiveDate = true;
                }

                GenericValue payment = EntityQuery.use(delegator).from("Payment").where("paymentId", paymentAppId).queryFirst();
                Timestamp paymentEffectivePaymentDate = (Timestamp) payment.get("effectiveDate");
                if (currentDate.after(paymentEffectivePaymentDate)) {
                    payment.set("statusId", "PMNT_RECEIVED");
                    payment.store();
                }
            }

            if(totalAmountApplied.intValue() >0)
            {
                invoiceRecord.set("statusId", "INVOICE_READY");
            }

            // get invoice total
            BigDecimal invoiceTotal = BigDecimal.ZERO;
            BigDecimal zeroBigDecimal = BigDecimal.ZERO;
            if (UtilValidate.isNotEmpty(invoiceRecord)) {
                invoiceTotal = AxInvoiceWorker.getInvoiceTotal(invoiceRecord);
            }
            BigDecimal invoiceOutstanding = AxInvoiceWorker.getInvoiceNotApplied(delegator, invoiceRecord);
            if (isValidEffectiveDate) {
                if (invoiceOutstanding.compareTo(zeroBigDecimal) == 0) {

                    // mark invoice as paid
                    if (null != invoiceRecord && !"INVOICE_PAID".equals(invoiceRecord.get("statusId"))) {
                        invoiceRecord.set("statusId", "INVOICE_PAID");

                        String invoiceType = "";
                        if ("PURCHASE_INVOICE".equals(invoiceRecord.get("invoiceTypeId"))) {
                            invoiceType = "Purchase Invoice";
                        } else {
                            invoiceType = "Sales Invoice";
                        }

                        Map <String, Object> awardDetails = AxAccountingHelper.getAwardDetailsFromInvoiceId(dispatcher, invoiceId, invoiceType);
                        Timestamp awardDate = null;
                        Date quoteOrderedDate = (Date) awardDetails.get("quoteOrderedDate");
                        if (UtilValidate.isNotEmpty(quoteOrderedDate)) {
                            awardDate = UtilDateTime.toTimestamp(quoteOrderedDate);
                        }

                        Map <String, Object> notifyInvoicePaymentCtx = UtilMisc.toMap("userLogin", userLogin);
                        notifyInvoicePaymentCtx.put("invoiceId", invoiceId);
                        notifyInvoicePaymentCtx.put("quoteId", awardDetails.get("quoteId"));
                        notifyInvoicePaymentCtx.put("quotePurchaseOrderNumber", awardDetails.get("quotePurchaseOrderNumber"));
                        notifyInvoicePaymentCtx.put("contractSupplierName", awardDetails.get("quoteSupplierPartyName"));
                        notifyInvoicePaymentCtx.put("contractCustomerName", awardDetails.get("quoteCustomerName"));
                        notifyInvoicePaymentCtx.put("awardedDate", awardDate);
                        notifyInvoicePaymentCtx.put("contractAmount", awardDetails.get("quoteSubTotal"));
                        notifyInvoicePaymentCtx.put("billedAmount", totalAmountApplied);
                        notifyInvoicePaymentCtx.put("effectivePaymentDate", effectivePaymentDate);
                        notifyInvoicePaymentCtx.put("invoiceType", invoiceType);
                        Map <String, Object> notifyInvoicePaymentResp = dispatcher.runSync("notifyInvoicePayment", notifyInvoicePaymentCtx);
                        if (ServiceUtil.isFailure(notifyInvoicePaymentResp)) {
                            return notifyInvoicePaymentResp;
                        }
                        // index quote in solr
                        String quoteId = (String) awardDetails.get("quoteId");
                        if(UtilValidate.isNotEmpty(quoteId)){
                            Map<String, Object> indexQuoteMap = UtilMisc.toMap(
                                    "quoteId",quoteId,
                                    "userLogin", userLogin
                            );
                            Map<String, Object> indexQuoteMapResp = dispatcher.runSync("indexQuoteInSolr", indexQuoteMap);
                            if (ServiceUtil.isFailure(indexQuoteMapResp)) {
                                return indexQuoteMapResp;
                            }
                        }
                    }
                }
            }
            invoiceRecord.store();
            TransactionUtil.commit();

        }
        return serviceResult;
    }
}