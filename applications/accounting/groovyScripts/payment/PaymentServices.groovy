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
import org.apache.ofbiz.accounting.payment.PaymentWorker
import org.apache.ofbiz.base.util.Debug
import org.apache.ofbiz.base.util.UtilDateTime
import org.apache.ofbiz.base.util.UtilFormatOut
import org.apache.ofbiz.base.util.UtilProperties
import org.apache.ofbiz.entity.condition.EntityCondition
import org.apache.ofbiz.entity.condition.EntityOperator
import org.apache.ofbiz.entity.GenericValue
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

def createPaymentApplication() {
    // Create a Payment Application
    if (!parameters.invoiceId && !parameters.billingAccountId && !parameters.taxAuthGeoId && !parameters.toPaymentId) {
        return error(UtilProperties.getResourceBundleMap("AccountingUiLabels", locale)?.AccountingPaymentApplicationParameterMissing)
    }
    GenericValue paymentAppl = delegator.makeValue("PaymentApplication")
    paymentAppl.setNonPKFields(parameters)

    GenericValue payment = from("Payment").where("paymentId", parameters.paymentId).queryOne()
    if (!payment) {
        return error(UtilProperties.getResourceBundleMap("AccountingUiLabels", locale)?.AccountingPaymentApplicationParameterMissing)
    }

    BigDecimal notAppliedPayment = PaymentWorker.getPaymentNotApplied(payment)

    if (parameters.invoiceId) {
        // get the invoice and do some further validation against it
        GenericValue invoice = from("Invoice").where("invoiceId", parameters.invoiceId).queryOne()
        // check the currencies if they are compatible
        if (invoice.currencyUomId != payment.currencyUomId && invoice.currencyUomId != payment.actualCurrencyUomId) {
            return error(UtilProperties.getResourceBundleMap("AccountingUiLabels", locale)?.AccountingCurrenciesOfInvoiceAndPaymentNotCompatible)
        }
        if (invoice.currencyUomId != payment.currencyUomId && invoice.currencyUomId == payment.actualCurrencyUomId) {
            // if required get the payment amount in foreign currency (local we already have)
            Boolean actual = true
            notAppliedPayment = PaymentWorker.getPaymentNotApplied(payment, actual)
        }
        // get the amount that has not been applied yet for the invoice (outstanding amount)
        BigDecimal notAppliedInvoice = InvoiceWorker.getInvoiceNotApplied(invoice)
        if (notAppliedInvoice <= notAppliedPayment) {
            paymentAppl.amountApplied = notAppliedInvoice
        } else {
            paymentAppl.amountApplied = notAppliedPayment
        }

        if (invoice.billingAccountId) {
            paymentAppl.billingAccountId = invoice.billingAccountId
        }
    }

    if (parameters.toPaymentId) {
        // get the to payment and check the parent types are compatible
        GenericValue toPayment = from("Payment").where("paymentId", parameters.toPaymentId).queryOne()
        if (toPayment) {
            toPaymentType = from("PaymentType").where("paymentTypeId", toPayment.paymentTypeId).queryOne()
        }
        paymentType = from("PaymentType").where("paymentTypeId", payment.paymentTypeId).queryOne()

        //  when amount not provided use the the lowest value available
        if (!parameters.amountApplied) {
            notAppliedPayment = PaymentWorker.getPaymentNotApplied(payment)
            BigDecimal notAppliedToPayment = PaymentWorker.getPaymentNotApplied(toPayment)
            if (notAppliedPayment < notAppliedToPayment) {
                paymentAppl.amountApplied = notAppliedPayment
            } else {
                paymentAppl.amountApplied = notAppliedToPayment
            }
        }
    }

    if (!paymentAppl.amountApplied) {
        if (parameters.billingAccountId || parameters.taxAuthGeoId) {
            paymentAppl.amountApplied = notAppliedPayment
        }
    }
    paymentAppl.paymentApplicationId = delegator.getNextSeqId("PaymentApplication")

    serviceResult = success()
    serviceResult.amountApplied = paymentAppl.amountApplied
    serviceResult.paymentApplicationId = paymentAppl.paymentApplicationId
    serviceResult.paymentTypeId = payment.paymentTypeId
    paymentAppl.create()
    return serviceResult
}