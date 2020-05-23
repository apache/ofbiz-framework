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

def setPaymentStatus() {
    if (parameters.paymentId) {
        Map resultMap = new HashMap()
        payment = from("Payment").where("paymentId", parameters.paymentId).queryOne()
        if (payment) {
            oldStatusId = payment.statusId
            statusItem = from("StatusItem").where("statusId", parameters.statusId).queryOne()
            if (statusItem) {
                if (!oldStatusId.equals(parameters.statusId)) {
                    statusChange = from("StatusValidChange").where("statusId", oldStatusId, "statusIdTo", parameters.statusId).queryOne()
                    if (statusChange) {
                        // payment method is mandatory when set to sent or received
                        if (("PMNT_RECEIVED".equals(parameters.paymentId) || "PMNT_SENT".equals(parameters.paymentId)) && !payment.paymentMethodId) {
                            resultMap.errorMessage = UtilProperties.getMessage("AccountingUiLabels","AccountingMissingPaymentMethod", [statusItem.description], locale)
                            logError("Cannot set status to " + parameters.statusId + " on payment " + payment.paymentId + ": payment method is missing")
                            return failure(resultMap.errorMessage)
                        }
                        // check if the payment fully applied when set to confirmed
                        if ("PMNT_CONFIRMED".equals(parameters.statusId)) {
                            notYetApplied = org.apache.ofbiz.accounting.payment.PaymentWorker.getPaymentNotApplied(payment)
                            if (BigDecimal.ZERO.compareTo(notYetApplied)) {
                                resultMap.errorMessage = UtilProperties.getMessage("AccountingUiLabels","AccountingPSNotConfirmedNotFullyApplied", locale)
                                logError("Cannot change from " + payment.statusId + " to " + parameters.statusId + ", payment not fully applied:" + notYetapplied)
                                return failure(resultMap.errorMessage)
                            }
                        }
                        // if new status is cancelled delete existing payment applications
                        if ("PMNT_CANCELLED".equals(parameters.statusId)) {
                            paymentApplications = payment.getRelated("PaymentApplication", null, null, false);
                            if (paymentApplications) {
                                paymentApplications.each { paymentApplication ->
                                    removePaymentApplicationMap.paymentApplicationId = paymentApplication.paymentApplicationId
                                    paymentAppResult = runService('removePaymentApplication', removePaymentApplicationMap)
                                }
                            }
                            // if new status is cancelled and the payment is associated to an OrderPaymentPreference, update the status of that record too
                            orderPaymentPreference = payment.getRelatedOne("OrderPaymentPreference", false)
                            if (orderPaymentPreference) {
                                updateOrderPaymentPreferenceMap.orderPaymentPreferenceId = orderPaymentPreference.orderPaymentPreferenceId
                                updateOrderPaymentPreferenceMap.statusId = "PAYMENT_CANCELLED"
                                runService('updateOrderPaymentPreference', updateOrderPaymentPreferenceMap)
                            }
                        }

                        // everything ok, so now change the status field
                        payment.statusId = parameters.statusId
                        payment.store()
                    } else {
                        resultMap.errorMessage = "Cannot change from " + oldStatusId + " to " + parameters.statusId
                        logError(resultMap.errorMessage)
                    }
                }
            } else {
                resultMap.errorMessage = "No status found with status ID" + parameters.statusId
                logError(resultMap.errorMessage)
            }
        } else {
            resultMap.errorMessage = "No payment found with payment ID " + parameters.paymentId
            logError(resultMap.errorMessage)
        }
    }
}