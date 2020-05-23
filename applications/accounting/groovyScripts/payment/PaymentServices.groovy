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
import org.apache.ofbiz.base.util.Debug
import org.apache.ofbiz.base.util.UtilDateTime
import org.apache.ofbiz.base.util.UtilFormatOut
import org.apache.ofbiz.base.util.UtilProperties
import org.apache.ofbiz.entity.condition.EntityCondition
import org.apache.ofbiz.entity.condition.EntityOperator
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.service.ModelService
import org.apache.ofbiz.service.ServiceUtil
import java.sql.Timestamp

MODULE = "PaymentServices.groovy"
def createPayment() {
    if (!security.hasEntityPermission("ACCOUNTING", "_CREATE", parameters.userLogin) && (!security.hasEntityPermission("PAY_INFO", "_CREATE", parameters.userLogin) && userLogin.partyId != parameters.partyIdFrom && userLogin.partyId != parameters.partyIdTo)) {
        return error(UtilProperties.getResourceBundleMap("AccountingUiLabels", locale)?.AccountingCreatePaymentPermissionError)
    }

    GenericValue payment = delegator.makeValue("Payment")
    payment.paymentId = parameters.paymentId ?: delegator.getNextSeqId("Payment")
    paymentId = payment.paymentId
    parameters.statusId = parameters.statusId ?: "PMNT_NOT_PAID"

    if (parameters.paymentMethodId) {
        GenericValue paymentMethod = from("PaymentMethod").where("paymentMethodId", parameters.paymentMethodId).queryOne()
        if (parameters.paymentMethodTypeId != paymentMethod.paymentMethodTypeId) {
            Debug.logInfo("Replacing passed payment method type [" + parameters.paymentMethodTypeId + "] with payment method type [" + paymentMethod.paymentMethodTypeId + "] for payment method [" + parameters.paymentMethodId +"]", MODULE)
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
    GenericValue newEntity = delegator.makeValue("PaymentContent")
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
    GenericValue lookupPKMap = delegator.makeValue("PaymentContent")
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

