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
import org.apache.ofbiz.accounting.invoice.InvoiceWorker
import org.apache.ofbiz.base.util.Debug
import org.apache.ofbiz.base.util.UtilDateTime
import org.apache.ofbiz.base.util.UtilFormatOut
import org.apache.ofbiz.base.util.UtilProperties
import org.apache.ofbiz.entity.condition.EntityCondition
import org.apache.ofbiz.entity.condition.EntityOperator
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.entity.util.EntityUtil
import org.apache.ofbiz.service.ModelService
import org.apache.ofbiz.service.ServiceUtil
import java.sql.Timestamp

def createPayment() {
    if (!security.hasEntityPermission("ACCOUNTING", "_CREATE", parameters.userLogin) && (!security.hasEntityPermission("PAY_INFO", "_CREATE", parameters.userLogin) && userLogin.partyId != parameters.partyIdFrom && userLogin.partyId != parameters.partyIdTo)) {
        return error(UtilProperties.getResourceBundleMap("AccountingUiLabels", locale)?.AccountingCreatePaymentPermissionError)
    }

    GenericValue payment = makeValue("Payment")
    payment.paymentId = parameters.paymentId ?: delegator.getNextSeqId("Payment")
    paymentId = payment.paymentId
    parameters.statusId = parameters.statusId ?: "PMNT_NOT_PAID"

    if (parameters.paymentMethodId) {
        GenericValue paymentMethod = from("PaymentMethod").where("paymentMethodId", parameters.paymentMethodId).queryOne()
        if (parameters.paymentMethodTypeId != paymentMethod.paymentMethodTypeId) {
            logInfo("Replacing passed payment method type [" + parameters.paymentMethodTypeId + "] with payment method type [" + paymentMethod.paymentMethodTypeId + "] for payment method [" + parameters.paymentMethodId +"]")
            parameters.paymentMethodTypeId = paymentMethod.paymentMethodTypeId
        }
    }

    if (parameters.paymentPreferenceId) {
        GenericValue orderPaymentPreference = from("OrderPaymentPreference").where("orderPaymentPreferenceId", parameters.paymentPreferenceId).queryOne()
        parameters.paymentId = parameters.paymentId ?: orderPaymentPreference.paymentMethodId
        parameters.paymentMethodTypeId = parameters.paymentMethodTypeId ?: orderPaymentPreference.paymentMethodTypeId
    }

    if (!parameters.paymentMethodTypeId) {
        return error(UtilProperties.getResourceBundleMap("AccountingUiLabels", locale)?.AccountingPaymentMethodIdPaymentMethodTypeIdNullError)
    }

    payment.setNonPKFields(parameters)
    payment.effectiveDate = payment.effectiveDate ?: UtilDateTime.nowTimestamp()
    delegator.create(payment)
    Map result = success()
    result.paymentId = paymentId
    return result
}
def getInvoicePaymentInfoList() {
    // Create a list with information on payment due dates and amounts for the invoice
    GenericValue invoice;
    List invoicePaymentInfoList = []
    if (!parameters.invoice) {
        invoice = from("Invoice").where("invoiceId", parameters.invoiceId).queryOne()
    } else {
        invoice = parameters.invoice
    }

    BigDecimal invoiceTotalAmount = InvoiceWorker.getInvoiceTotal(invoice)
    BigDecimal invoiceTotalAmountPaid = InvoiceWorker.getInvoiceApplied(invoice)

    List invoiceTerms = from("InvoiceTerm").where("invoiceId", invoice.invoiceId).queryList()

    BigDecimal remainingAppliedAmount = invoiceTotalAmountPaid
    BigDecimal computedTotalAmount = (BigDecimal) 0

    Map invoicePaymentInfo = [:]

    for (invoiceTerm in invoiceTerms) {
        termType = from("TermType").where("termTypeId", invoiceTerm.termTypeId).cache(true).queryOne()
        if ("FIN_PAYMENT_TERM" == termType.parentTypeId) {
            invoicePaymentInfo.clear()
            invoicePaymentInfo.invoiceId = invoice.invoiceId
            invoicePaymentInfo.invoiceTermId = invoiceTerm.invoiceTermId
            invoicePaymentInfo.termTypeId = invoiceTerm.termTypeId
            invoicePaymentInfo.dueDate = UtilDateTime.getDayEnd(invoice.invoiceDate, invoiceTerm.termDays)

            BigDecimal invoiceTermAmount = (invoiceTerm.termValue * invoiceTotalAmount ) / 100
            invoicePaymentInfo.amount = invoiceTermAmount
            computedTotalAmount = computedTotalAmount + (BigDecimal) invoicePaymentInfo.amount

            if (remainingAppliedAmount >= invoiceTermAmount) {
                invoicePaymentInfo.paidAmount = invoiceTermAmount
                remainingAppliedAmount = remainingAppliedAmount - invoiceTermAmount
            } else {
                invoicePaymentInfo.paidAmount = remainingAppliedAmount
                remainingAppliedAmount = (BigDecimal) 0
            }
            invoicePaymentInfo.outstandingAmount = invoicePaymentInfo.amount - invoicePaymentInfo.paidAmount
            invoicePaymentInfoList.add(invoicePaymentInfo)
        }
    }

    if (remainingAppliedAmount > 0.0 || invoiceTotalAmount <= 0.0 || computedTotalAmount < invoiceTotalAmount) {
        invoicePaymentInfo.clear()
        invoiceTerm = from("InvoiceTerm").where("invoiceId", invoice.invoiceId, "termTypeId", "FIN_PAYMENT_TERM").queryFirst()
        if (invoiceTerm) {
            invoicePaymentInfo.termTypeId = invoiceTerm.termTypeId
            invoicePaymentInfo.dueDate = UtilDateTime.getDayEnd(invoice.invoiceDate, invoiceTerm.termDays)
        } else {
            invoicePaymentInfo.dueDate = UtilDateTime.getDayEnd(invoice.invoiceDate)
        }
        invoicePaymentInfo.invoiceId = invoice.invoiceId
        invoicePaymentInfo.amount = invoiceTotalAmount - computedTotalAmount
        invoicePaymentInfo.paidAmount = remainingAppliedAmount
        invoicePaymentInfo.outstandingAmount = invoicePaymentInfo.amount - invoicePaymentInfo.paidAmount
        invoicePaymentInfoList.add(invoicePaymentInfo)
    }
    Map result = success()
    result.invoicePaymentInfoList = invoicePaymentInfoList
    return result
}
def updatePayment() {
    Map lookupPayment = makeValue("Payment")
    lookupPayment.setPKFields(parameters)
    GenericValue payment = from("Payment").where("paymentId", lookupPayment.paymentId).queryOne()
    if (!security.hasEntityPermission("ACCOUNTING", "_UPDATE", parameters.userLogin) &&
        (!security.hasEntityPermission("PAY_INFO", "_UPDATE", parameters.userLogin) &&
        userLogin.partyId != payment.partyIdFrom && userLogin.partyId != payment.partyIdTo)) {
        return error(UtilProperties.getResourceBundleMap("AccountingUiLabels", locale)?.AccountingUpdatePaymentPermissionError)
    }
    if ("PMNT_NOT_PAID" != payment.statusId) {
        // check if only status change
        GenericValue newPayment = makeValue("Payment")
        GenericValue oldPayment = makeValue("Payment")
        newPayment.setNonPKFields(payment)
        oldPayment.setNonPKFields(payment)
        newPayment.setNonPKFields(parameters)

        // fields :- comments, paymentRefNum, finAccountTransId, statusIhStatus does not allow an update of the information are editable for Payment
        oldPayment.statusId = newPayment.statusId
        oldPayment.comments = newPayment.comments
        oldPayment.paymentRefNum = newPayment.paymentRefNum ?: null
        oldPayment.finAccountTransId = newPayment.finAccountTransId ?: null
        if (!oldPayment.equals(newPayment)) {
            return error(UtilProperties.getResourceBundleMap("AccountingUiLabels", locale)?.AccountingPSUpdateNotAllowedBecauseOfStatus)
        }
    }
    statusIdSave = payment.statusId  // do not allow status change here
    payment.setNonPKFields(parameters)
    payment.statusId = statusIdSave  // do not allow status change here
    payment.effectiveDate = payment.effectiveDate ?: UtilDateTime.nowTimestamp()
    if (payment.paymentMethodId) {
        paymentMethod = from("PaymentMethod").where("paymentMethodId", payment.paymentMethodId).queryOne()
        if (payment.paymentMethodTypeId != paymentMethod.paymentMethodTypeId) {
            logInfo("Replacing passed payment method type [" + parameters.paymentMethodTypeId + "] with payment method type [" +
                paymentMethod.paymentMethodTypeId + "] for payment method [" + parameters.paymentMethodId +"]")
        }
        payment.paymentMethodTypeId = paymentMethod.paymentMethodTypeId
    }
    payment.store()
    if (parameters.statusId) {
        if (parameters.statusId != statusIdSave) {
            Map param = dispatcher.getDispatchContext().makeValidContext('setPaymentStatus', ModelService.IN_PARAM, parameters)
            param.paymentId = payment.paymentId
            serviceResult = run service: 'setPaymentStatus', with: param
            if (!ServiceUtil.isSuccess(serviceResult)) return error(serviceResult)
        }
    }
    return success()
}
def createPaymentAndApplicationForParty() {
    paymentAmount = 0
    List invoiceIds = []
    Map result = success()
    parameters.invoices.each { invoice ->
        if ("INVOICE_READY" == invoice.statusId) {
            Map serviceContext = dispatcher.getDispatchContext().makeValidContext('getInvoicePaymentInfoList', ModelService.IN_PARAM, invoice)
            serviceContext.userLogin = userLogin
            serviceResult = run service: 'getInvoicePaymentInfoList', with: serviceContext
            if (ServiceUtil.isError(serviceResult)) return serviceResult
            invoicePaymentInfo = serviceResult.invoicePaymentInfoList[0]
            paymentAmount += invoicePaymentInfo.outstandingAmount
        } else {
            return error(UtilProperties.getMessage("AccountingUiLabels", "AccountingInvoicesRequiredInReadyStatus", parameters.locale))
        }
    }
    if (paymentAmount > 0) {
        serviceResult = run service: 'getPartyAccountingPreferences', with: parameters
        if (ServiceUtil.isError(serviceResult)) return serviceResult
        partyAcctgPreference = serviceResult.partyAccountingPreference
        Map createPaymentMap = [:]
        createPaymentMap.paymentTypeId = "VENDOR_PAYMENT"
        createPaymentMap.partyIdFrom = parameters.organizationPartyId
        createPaymentMap.currencyUomId = partyAcctgPreference.baseCurrencyUomId
        createPaymentMap.partyIdTo = parameters.partyId
        createPaymentMap.statusId = "PMNT_SENT"
        createPaymentMap.amount = paymentAmount
        createPaymentMap.paymentMethodTypeId = parameters.paymentMethodTypeId
        createPaymentMap.paymentMethodId = parameters.paymentMethodId
        createPaymentMap.paymentRefNum = parameters.checkStartNumber
        createPaymentMap.userLogin = userLogin
        serviceResult = run service: 'createPayment', with: createPaymentMap
        if (ServiceUtil.isError(serviceResult)) return serviceResult
        paymentId = serviceResult.paymentId
        result.paymentId = paymentId

        parameters.invoices.each {invoice ->
        if ("INVOICE_READY" == invoice.statusId) {
            Map serviceContext = dispatcher.getDispatchContext().makeValidContext('getInvoicePaymentInfoList', ModelService.IN_PARAM, invoice)
            serviceContext.userLogin = userLogin
            serviceResult = run service: 'getInvoicePaymentInfoList', with: serviceContext
            if (ServiceUtil.isError(serviceResult)) return serviceResult
            invoicePaymentInfo = serviceResult.invoicePaymentInfoList[0]
            if (invoicePaymentInfo.outstandingAmount > 0) {
                Map createPaymentApplicationMap = [:]
                createPaymentApplicationMap.paymentId =  paymentId
                createPaymentApplicationMap.amountApplied = invoicePaymentInfo.outstandingAmount
                createPaymentApplicationMap.invoiceId = invoice.invoiceId
                serviceResult = run service: 'createPaymentApplication', with: createPaymentApplicationMap
                if (ServiceUtil.isError(serviceResult)) return serviceResult
            }
        }
        invoiceIds.add(invoice.invoiceId)
        }
    }
    result.invoiceIds = invoiceIds
    result.amount =  paymentAmount
    return result
}
def getPaymentRunningTotal(){
    paymentIds = parameters.paymentIds;
    runningTotal = 0;
    payments = from("Payment").where(EntityCondition.makeCondition("paymentId", EntityOperator.IN, paymentIds)).queryList()
    if (payments) {
        for (GenericValue payment : payments) {
            runningTotal = runningTotal + payment.amount;
        }
    }

    if (parameters.organizationPartyId) {
        serviceCtx = [
                organizationPartyId: parameters.organizationPartyId,
                userLogin: userLogin
        ]
        serviceResult = dispatcher.runSync('getPartyAccountingPreferences', serviceCtx);
        partyAcctgPreference = serviceResult.partyAccountingPreference;

        if (partyAcctgPreference.baseCurrencyUomId) {
            currencyUomId = partyAcctgPreference.baseCurrencyUomId;
        } else {
            currencyUomId = UtilProperties.getPropertyValue('general.properties', 'currency.uom.id.default');
        }
    } else  {
        currencyUomId = UtilProperties.getPropertyValue('general.properties', 'currency.uom.id.default');
    }

    paymentRunningTotal = UtilFormatOut.formatCurrency(runningTotal, currencyUomId, locale);

    result = success()
    result.paymentRunningTotal = paymentRunningTotal
    return result
}
def createPaymentContent() {
    GenericValue newEntity = makeValue("PaymentContent")
    newEntity.setPKFields(parameters, true)
    newEntity.setNonPKFields(parameters, true)

    if (!newEntity.fromDate) {
        Timestamp nowTimestamp = UtilDateTime.nowTimestamp()
        newEntity.fromDate  = nowTimestamp
    }
    newEntity.create()

    result = run service: 'updateContent', with: parameters
    if (ServiceUtil.isError(result)) return result

    Map result = success()
    result.contentId = newEntity.contentId
    result.paymentId = newEntity.paymentId
    result.paymentContentTypeId = newEntity.paymentContentTypeId
    return result
}
//TODO: This can be converted into entity-auto with a seca rule for updateContent
def updatePaymentContent() {
    serviceResult = success()
    GenericValue lookupPKMap = makeValue("PaymentContent")
    lookupPKMap.setPKFields(parameters, true)

    GenericValue lookedUpValue = findOne("PaymentContent", lookupPKMap, false)
    if (lookedUpValue) {
        lookedUpValue.setNonPKFields(parameters)
        lookedUpValue.store()
        result = run service: 'updateContent', with: parameters
        if (ServiceUtil.isError(result)) return result
        return serviceResult
    } else {
        return ServiceUtil.returnError("Error getting Payment Content")
    }
}
def massChangePaymentStatus() {
    serviceResult = success()
    Map setPaymentStatusMap = [:]
    parameters.paymentIds.each{ paymentId ->
        setPaymentStatusMap.paymentId = paymentId
        setPaymentStatusMap.statusId = parameters.statusId
        setPaymentStatusMap.userLogin = parameters.userLogin
        result = run service: 'setPaymentStatus', with: setPaymentStatusMap
        if (ServiceUtil.isError(result)) return result
        setPaymentStatusMap.clear()
    }
    return serviceResult
}
def getInvoicePaymentInfoListByDueDateOffset(){

    filteredInvoicePaymentInfoList = []

    Timestamp asOfDate = UtilDateTime.getDayEnd(UtilDateTime.nowTimestamp(), (long) parameters.daysOffset);

    exprList = [EntityCondition.makeCondition("invoiceTypeId", EntityOperator.EQUALS, parameters.invoiceTypeId),
                EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "INVOICE_CANCELLED"),
                EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "INVOICE_PAID")
    ]
    if (parameters.partyId) {
        exprList.add(EntityCondition.makeCondition("partyId", EntityOperator.EQUALS, parameters.partyId))
    }
    if (parameters.partyIdFrom) {
        exprList.add(EntityCondition.makeCondition("partyIdFrom", EntityOperator.EQUALS, parameters.partyIdFrom))
    }

    condition = EntityCondition.makeCondition(exprList, EntityOperator.AND);

    invoices = from("Invoice").where(condition).orderBy("invoiceDate").queryList();

    if (invoices) {
        for (GenericValue invoice : invoices) {
            getInvoicePaymentInfoListInMap = [:]
            getInvoicePaymentInfoListInMap.put("invoice", invoice);
            getInvoicePaymentInfoListInMap.put("userLogin", userLogin);
            serviceResult = run service: 'getInvoicePaymentInfoList', with: getInvoicePaymentInfoListInMap
            if (ServiceUtil.isError(serviceResult)) return result
            invoicePaymentInfoList = serviceResult.invoicePaymentInfoList;
            if (invoicePaymentInfoList) {
                invoicePaymentInfoList.each { invoicePaymentInfo ->
                    if (invoicePaymentInfo.outstandingAmount.compareTo(BigDecimal.ZERO) > 0 && invoicePaymentInfo.dueDate.before(asOfDate)) {
                        filteredInvoicePaymentInfoList.add(invoicePaymentInfo);
                    }
                }
            }
        }
    }

    result = success()
    result.invoicePaymentInfoList = filteredInvoicePaymentInfoList
    return result
}

def getPaymentGroupReconciliationId() {
    paymentGroupMember = from("PaymentGroupMember").where("paymentGroupId", parameters.paymentGroupId).queryFirst()
    glReconciliationId = null;
    Map result = success()
    if (paymentGroupMember) {
        payment = paymentGroupMember.getRelatedOne('Payment', false)
        finAccountTrans = payment.getRelatedOne('FinAccountTrans', false)
        if (finAccountTrans) {
            glReconciliationId = finAccountTrans.glReconciliationId
        }
    }
    result.glReconciliationId = glReconciliationId
    return result
}

def createPaymentAndApplication() {
    Map result = success()
    Map createPaymentCtx = dispatcher.getDispatchContext().makeValidContext('createPayment', 'IN', parameters)
    Map createPaymentResp = dispatcher.runSync('createPayment', createPaymentCtx)

    if (ServiceUtil.isError(createPaymentResp)) return createPaymentResp

    Map createPaymentApplicationCtx = dispatcher.getDispatchContext().makeValidContext('createPaymentApplication', 'IN', parameters)
    createPaymentApplicationCtx.paymentId = createPaymentResp.paymentId
    createPaymentApplicationCtx.amountApplied = parameters.amount
    Map createPaymentApplicationResp = dispatcher.runSync('createPaymentApplication', createPaymentApplicationCtx)

    if (ServiceUtil.isError(createPaymentApplicationResp)) return createPaymentApplicationResp

    result.put("paymentId", createPaymentResp.paymentId)
    result.put("paymentApplicationId", createPaymentApplicationResp.paymentApplicationId)
    return result

}

def createFinAccoutnTransFromPayment() {
    serviceResult = success()
    Map createFinAccountTransMap = dispatcher.getDispatchContext().makeValidContext('setPaymentStatus', ModelService.IN_PARAM, parameters)
    createFinAccountTransMap.finAccountTransTypeId = 'WITHDRAWAL'
    createFinAccountTransMap.partyId  = parameters.organizationPartyId
    createFinAccountTransMap.transactionDate = UtilDateTime.nowTimestamp()
    createFinAccountTransMap.entryDate = UtilDateTime.nowTimestamp()
    createFinAccountTransMap.statusId = 'FINACT_TRNS_CREATED'
    createFinAccountTransMap.comments = "Pay to ${parameters.partyId} for invoice Ids - ${parameters.invoiceIds}"
    result = run service: 'createFinAccountTrans', with: createFinAccountTransMap
    if (ServiceUtil.isError(result)) {
        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(result))
    }
    Map updatePaymentMap = [:]
    updatePaymentMap.finAccountTransId = result.finAccountTransId
    updatePaymentMap.paymentId = parameters.paymentId
    result = run service: 'updatePayment', with: updatePaymentMap
    if (ServiceUtil.isError(result)) {
        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(result))
    }
    return serviceResult
}

def quickSendPayment() {
    Map result = success()
    Map updatePaymentCtx = dispatcher.getDispatchContext().makeValidContext('updatePayment', 'IN', parameters)
    Map updatePaymentResp = dispatcher.runSync('updatePayment', updatePaymentCtx)

    if (ServiceUtil.isError(updatePaymentResp)) return updatePaymentResp

    Map setPaymentStatusCtx = dispatcher.getDispatchContext().makeValidContext('setPaymentStatus', 'IN', parameters)
    setPaymentStatusCtx.statusId = "PMNT_SENT"
    Map setPaymentStatusResp = dispatcher.runSync('setPaymentStatus', setPaymentStatusCtx)

    if (ServiceUtil.isError(setPaymentStatusResp)) return setPaymentStatusResp

    return result

}

/**
 * Service to cancel payment batch
 */
def cancelPaymentBatch() {
    List<GenericValue> paymentGroupMemberAndTransList = from("PmtGrpMembrPaymentAndFinAcctTrans").where("paymentGroupId", parameters.paymentGroupId).queryList()

    if (paymentGroupMemberAndTransList) {
        GenericValue paymentGroupMemberAndTrans = EntityUtil.getFirst(paymentGroupMemberAndTransList)
        if ("FINACT_TRNS_APPROVED" == paymentGroupMemberAndTrans.finAccountTransStatusId) {
            return error(UtilProperties.getMessage('AccountingErrorUiLabels', 'AccountingTransactionIsAlreadyReconciled', locale))
        }

        for (GenericValue paymentGroupMember : paymentGroupMemberAndTransList) {
            Map expirePaymentGroupMemberMap = dispatcher.getDispatchContext().makeValidContext("expirePaymentGroupMember", "IN", paymentGroupMember)
            result = runService("expirePaymentGroupMember", expirePaymentGroupMemberMap)
            if (ServiceUtil.isError(result)) return result

            GenericValue finAccountTrans = from("FinAccountTrans").where("finAccountTransId", paymentGroupMember.finAccountTransId).queryOne()
            if (finAccountTrans) {
                Map setFinAccountTransStatusMap = dispatcher.getDispatchContext().makeValidContext("setFinAccountTransStatus", "IN", finAccountTrans)
                setFinAccountTransStatusMap.statusId = "FINACT_TRNS_CANCELED"
                result = runService("setFinAccountTransStatus", setFinAccountTransStatusMap)
                if (ServiceUtil.isError(result)) return result
            }
        }
    }
}

def getPayments() {
    payments = []
    if (parameters.paymentGroupId) {
        paymentGroupMembers = from("PaymentGroupMember").where("paymentGroupId", parameters.paymentGroupId).filterByDate().queryList()
        if (paymentGroupMembers) {
            paymentIds = EntityUtil.getFieldListFromEntityList(paymentGroupMembers, "paymentId", true)
            payments = from("Payment").where(EntityCondition.makeCondition("paymentId", EntityOperator.IN, paymentIds)).queryList()
        }
    }
    if (parameters.finAccountTransId) {
        payments = from("Payment").where("finAccountTransId", parameters.finAccountTransId).queryList()
    }
    result = success()
    result.payments = payments
    return result
}

def cancelCheckRunPayments() {
    paymentGroupMemberAndTransList = from("PmtGrpMembrPaymentAndFinAcctTrans").where("paymentGroupId", parameters.paymentGroupId).queryList()
    if (paymentGroupMemberAndTransList) {
        paymentGroupMemberAndTrans = EntityUtil.getFirst(paymentGroupMemberAndTransList)
        if ("FINACT_TRNS_APPROVED" != paymentGroupMemberAndTrans.finAccountTransStatusId) {
            for (GenericValue paymentGroupMemberAndTrans : paymentGroupMemberAndTransList) {
                payment = from("Payment").where("paymentId", paymentGroupMemberAndTrans.paymentId).queryOne()
                Map voidPaymentMap = dispatcher.getDispatchContext().makeValidContext("voidPayment", "IN", payment)
                result = runService("voidPayment", voidPaymentMap)
                if (ServiceUtil.isError(result)) return result
                Map expirePaymentGroupMemberMap = dispatcher.getDispatchContext().makeValidContext("expirePaymentGroupMember", "IN", paymentGroupMemberAndTrans)
                result = runService("expirePaymentGroupMember", expirePaymentGroupMemberMap)
                if (ServiceUtil.isError(result)) return result
            }
        } else {
            return error(UtilProperties.getMessage("AccountingErrorUiLabels", "AccountingCheckIsAlreadyIssued", locale))
        }
    }
    return success()
}
