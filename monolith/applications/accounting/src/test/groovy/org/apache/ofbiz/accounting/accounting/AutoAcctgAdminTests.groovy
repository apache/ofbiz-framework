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
package org.apache.ofbiz.accounting.accounting

import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.service.ServiceUtil
import org.apache.ofbiz.testtools.JunitJupiterTest
import org.apache.ofbiz.testtools.JupiterTestHelper
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test

@JunitJupiterTest
class AutoAcctgAdminTests implements JupiterTestHelper {

    @Test
    @Order(1)
    void testGetFXConversion() {
        String uomId = testParams.uomId ?: 'EUR'
        String uomIdTo = testParams.uomIdTo ?: 'USD'
        Map serviceCtx = [
                uomId: uomId,
                uomIdTo: uomIdTo,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('getFXConversion', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
    }

    @Test
    @Order(2)
    void testAddPaymentMethodTypeGlAssignment() {
        String paymentMethodTypeId = testParams.paymentMethodTypeId ?: 'GIFT_CARD'
        String organizationPartyId = testParams.organizationPartyId ?: 'DEMO_COMPANY1'
        String glAccountId = testParams.glAccountId ?: '999999'
        Map serviceCtx = [
            paymentMethodTypeId: paymentMethodTypeId,
            organizationPartyId: organizationPartyId,
            glAccountId: glAccountId,
            userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('addPaymentMethodTypeGlAssignment', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue paymentMethodTypeGlAccount = from('PaymentMethodTypeGlAccount')
                .where('paymentMethodTypeId', paymentMethodTypeId,
                        'organizationPartyId', organizationPartyId)
                .queryOne()
        assert paymentMethodTypeGlAccount
        assert paymentMethodTypeGlAccount.glAccountId == glAccountId
    }

    @Test
    @Order(3)
    void testRemovePaymentTypeGlAssignment() {
        String paymentTypeId = testParams.paymentTypeId ?: 'COMMISSION_PAYMENT'
        String organizationPartyId = testParams.organizationPartyId ?: 'DEMO_COMPANY1'
        Map serviceCtx = [
                paymentTypeId: paymentTypeId,
                organizationPartyId: organizationPartyId,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('removePaymentTypeGlAssignment', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue paymentMethodTypeGlAccount = from('PaymentGlAccountTypeMap')
                .where('paymentTypeId', paymentTypeId,
                        'organizationPartyId', organizationPartyId)
                .queryOne()
        assert !paymentMethodTypeGlAccount
    }

    @Test
    @Order(4)
    void testCreatePartyAcctgPreference() {
        String partyId = testParams.partyId ?: 'DEMO_COMPANY'
        String refundPaymentMethodId = testParams.refundPaymentMethodId ?: '9020'
        Map serviceCtx = [
                partyId: partyId,
                refundPaymentMethodId: refundPaymentMethodId,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createPartyAcctgPreference', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue partyAcctgPreference = from('PartyAcctgPreference')
                .where('partyId', partyId)
                .queryOne()
        assert partyAcctgPreference
        assert partyAcctgPreference.partyId == partyId
        assert partyAcctgPreference.refundPaymentMethodId == refundPaymentMethodId
    }

    @Test
    @Order(5)
    void testUpdatePartyAcctgPreference() {
        String partyId = testParams.partyId ?: 'DEMO_COMPANY1'
        String refundPaymentMethodId = testParams.refundPaymentMethodId ?: '9020'
        Map serviceCtx = [
                partyId: partyId,
                refundPaymentMethodId: refundPaymentMethodId,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('updatePartyAcctgPreference', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue partyAcctgPreference = from('PartyAcctgPreference')
                .where('partyId', partyId)
                .queryOne()
        assert partyAcctgPreference
        assert partyAcctgPreference.refundPaymentMethodId == refundPaymentMethodId
    }

    @Test
    @Order(6)
    void testGetPartyAccountingPreferences() {
        String organizationPartyId = testParams.organizationPartyId ?: 'DEMO_COMPANY1'
        Map serviceCtx = [
                organizationPartyId: organizationPartyId,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('getPartyAccountingPreferences', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult.partyAccountingPreference
    }

    @Test
    @Order(7)
    void testSetAcctgCompany() {
        String organizationPartyId = testParams.organizationPartyId ?: 'DEMO_COMPANY1'
        Map serviceCtx = [
                organizationPartyId: organizationPartyId,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('setAcctgCompany', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue userPreference = from('UserPreference')
                .where('userPrefValue', organizationPartyId)
                .queryFirst()
        assert userPreference
        assert userPreference.userPrefGroupTypeId == 'GLOBAL_PREFERENCES'
        assert userPreference.userPrefTypeId == 'ORGANIZATION_PARTY'
    }

    @Test
    @Order(8)
    void testUpdateFXConversion() {
        String uomId = testParams.uomId ?: 'INR'
        String uomIdTo = testParams.uomIdTo ?: 'USD'
        Map serviceCtx = [
                uomId: uomId,
                uomIdTo: uomIdTo,
                conversionFactor: 2.0,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('updateFXConversion', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue uomConversionDated = from('UomConversionDated')
                .where('uomId', uomId, 'uomIdTo', uomIdTo)
                .queryFirst()
        assert uomConversionDated
        assert uomConversionDated.conversionFactor == 2.0
    }

    @Test
    @Order(9)
    void testCreateGlAccountTypeDefault() {
        String glAccountTypeId = testParams.glAccountTypeId ?: 'BALANCE_ACCOUNT'
        String organizationPartyId = testParams.organizationPartyId ?: 'DEMO_COMPANY1'
        String glAccountId = testParams.glAccountId ?: '999999'
        Map serviceCtx = [
                glAccountTypeId: glAccountTypeId,
                organizationPartyId: organizationPartyId,
                glAccountId: glAccountId,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createGlAccountTypeDefault', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue glAccountTypeDefault = from('GlAccountTypeDefault')
                .where('glAccountTypeId', glAccountTypeId, 'organizationPartyId', organizationPartyId)
                .queryOne()
        assert glAccountTypeDefault
        assert glAccountTypeDefault.glAccountId == glAccountId
    }

    @Test
    @Order(10)
    void testRemoveGlAccountTypeDefault() {
        String glAccountTypeId = testParams.glAccountTypeId ?: 'ACCOUNTS_PAYABLE'
        String organizationPartyId = testParams.organizationPartyId ?: 'DEMO_COMPANY1'
        String glAccountId = testParams.glAccountId ?: '999999'
        Map serviceCtx = [
                glAccountTypeId: glAccountTypeId,
                organizationPartyId: organizationPartyId,
                glAccountId: glAccountId,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('removeGlAccountTypeDefault', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue glAccountTypeDefault = from('GlAccountTypeDefault')
                .where('glAccountTypeId', glAccountTypeId,
                        'organizationPartyId', organizationPartyId)
                .queryOne()
        assert !glAccountTypeDefault
    }

    @Test
    @Order(11)
    void testAddInvoiceItemTypeGlAssignment() {
        String invoiceItemTypeId = testParams.invoiceItemTypeId ?: 'PINV_FPROD_ITEM'
        String organizationPartyId = testParams.organizationPartyId ?: 'DEMO_COMPANY1'
        String glAccountId = testParams.glAccountId ?: '999999'
        Map serviceCtx = [
                invoiceItemTypeId: invoiceItemTypeId,
                organizationPartyId: organizationPartyId,
                glAccountId: glAccountId,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('addInvoiceItemTypeGlAssignment', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue invoiceItemTypeGlAccount = from('InvoiceItemTypeGlAccount')
                .where('invoiceItemTypeId', invoiceItemTypeId,
                        'organizationPartyId', organizationPartyId)
                .queryOne()
        assert invoiceItemTypeGlAccount
        assert invoiceItemTypeGlAccount.glAccountId == glAccountId
    }

    @Test
    @Order(12)
    void testRemoveInvoiceItemTypeGlAssignment() {
        String invoiceItemTypeId = testParams.invoiceItemTypeId ?: 'PINV_SALES_TAX'
        String organizationPartyId = testParams.organizationPartyId ?: 'DEMO_COMPANY1'
        Map serviceCtx = [
                invoiceItemTypeId: invoiceItemTypeId,
                organizationPartyId: organizationPartyId,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('removeInvoiceItemTypeGlAssignment', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue invoiceItemTypeGlAccount = from('InvoiceItemTypeGlAccount')
                .where('invoiceItemTypeId', invoiceItemTypeId,
                        'organizationPartyId', organizationPartyId)
                .queryOne()
        assert !invoiceItemTypeGlAccount
    }

    @Test
    @Order(13)
    void testAddPaymentTypeGlAssignment() {
        String paymentTypeId = testParams.paymentTypeId ?: 'TAX_PAYMENT'
        String organizationPartyId = testParams.organizationPartyId ?: 'DEMO_COMPANY1'
        String glAccountTypeId = testParams.glAccountTypeId ?: 'TAX_ACCOUNT'
        Map serviceCtx = [
                paymentTypeId: paymentTypeId,
                organizationPartyId: organizationPartyId,
                glAccountTypeId: glAccountTypeId,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('addPaymentTypeGlAssignment', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue paymentGlAccountTypeMap = from('PaymentGlAccountTypeMap')
                .where('paymentTypeId', paymentTypeId,
                        'organizationPartyId', organizationPartyId)
                .queryOne()
        assert paymentGlAccountTypeMap
        assert paymentGlAccountTypeMap.glAccountTypeId == glAccountTypeId
    }

    @Test
    @Order(14)
    void testRemovePaymentMethodTypeGlAssignment() {
        String paymentMethodTypeId = testParams.paymentMethodTypeId ?: 'CASH'
        String organizationPartyId = testParams.organizationPartyId ?: 'DEMO_COMPANY1'
        Map serviceCtx = [
                paymentMethodTypeId: paymentMethodTypeId,
                organizationPartyId: organizationPartyId,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('removePaymentMethodTypeGlAssignment', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue paymentMethodTypeGlAccount = from('PaymentMethodTypeGlAccount')
                .where('paymentMethodTypeId', paymentMethodTypeId,
                        'organizationPartyId', organizationPartyId)
                .queryOne()
        assert !paymentMethodTypeGlAccount
    }

}
