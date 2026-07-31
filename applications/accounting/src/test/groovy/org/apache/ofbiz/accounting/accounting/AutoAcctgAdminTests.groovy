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
        Map serviceCtx = [
                uomId: 'EUR',
                uomIdTo: 'USD',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('getFXConversion', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
    }

    @Test
    @Order(2)
    void testAddPaymentMethodTypeGlAssignment() {
        Map serviceCtx = [
            paymentMethodTypeId: 'GIFT_CARD',
            organizationPartyId: 'DEMO_COMPANY1',
            glAccountId: '999999',
            userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('addPaymentMethodTypeGlAssignment', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue paymentMethodTypeGlAccount = from('PaymentMethodTypeGlAccount')
                .where('paymentMethodTypeId', 'GIFT_CARD',
                        'organizationPartyId', 'DEMO_COMPANY1')
                .queryOne()
        assert paymentMethodTypeGlAccount
        assert paymentMethodTypeGlAccount.glAccountId == '999999'
    }

    @Test
    @Order(3)
    void testRemovePaymentTypeGlAssignment() {
        Map serviceCtx = [
                paymentTypeId: 'COMMISSION_PAYMENT',
                organizationPartyId: 'DEMO_COMPANY1',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('removePaymentTypeGlAssignment', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue paymentMethodTypeGlAccount = from('PaymentGlAccountTypeMap')
                .where('paymentTypeId', 'COMMISSION_PAYMENT',
                        'organizationPartyId', 'DEMO_COMPANY1')
                .queryOne()
        assert !paymentMethodTypeGlAccount
    }

    @Test
    @Order(4)
    void testCreatePartyAcctgPreference() {
        Map serviceCtx = [
                partyId: 'DEMO_COMPANY',
                refundPaymentMethodId: '9020',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createPartyAcctgPreference', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue partyAcctgPreference = from('PartyAcctgPreference')
                .where('partyId', 'DEMO_COMPANY')
                .queryOne()
        assert partyAcctgPreference
        assert partyAcctgPreference.partyId == 'DEMO_COMPANY'
        assert partyAcctgPreference.refundPaymentMethodId == '9020'
    }

    @Test
    @Order(5)
    void testUpdatePartyAcctgPreference() {
        Map serviceCtx = [
                partyId: 'DEMO_COMPANY1',
                refundPaymentMethodId: '9020',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('updatePartyAcctgPreference', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue partyAcctgPreference = from('PartyAcctgPreference')
                .where('partyId', 'DEMO_COMPANY1')
                .queryOne()
        assert partyAcctgPreference
        assert partyAcctgPreference.refundPaymentMethodId == '9020'
    }

    @Test
    @Order(6)
    void testGetPartyAccountingPreferences() {
        Map serviceCtx = [
                organizationPartyId: 'DEMO_COMPANY1',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('getPartyAccountingPreferences', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult.partyAccountingPreference
    }

    @Test
    @Order(7)
    void testSetAcctgCompany() {
        Map serviceCtx = [
                organizationPartyId: 'DEMO_COMPANY1',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('setAcctgCompany', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue userPreference = from('UserPreference')
                .where('userPrefValue', 'DEMO_COMPANY1')
                .queryFirst()
        assert userPreference
        assert userPreference.userPrefGroupTypeId == 'GLOBAL_PREFERENCES'
        assert userPreference.userPrefTypeId == 'ORGANIZATION_PARTY'
    }

    @Test
    @Order(8)
    void testUpdateFXConversion() {
        Map serviceCtx = [
                uomId: 'INR',
                uomIdTo: 'USD',
                conversionFactor: 2.0,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('updateFXConversion', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue uomConversionDated = from('UomConversionDated')
                .where('uomId', 'INR', 'uomIdTo', 'USD')
                .queryFirst()
        assert uomConversionDated
        assert uomConversionDated.conversionFactor == 2.0
    }

    @Test
    @Order(9)
    void testCreateGlAccountTypeDefault() {
        Map serviceCtx = [
                glAccountTypeId: 'BALANCE_ACCOUNT',
                organizationPartyId: 'DEMO_COMPANY1',
                glAccountId: '999999',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createGlAccountTypeDefault', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue glAccountTypeDefault = from('GlAccountTypeDefault')
                .where('glAccountTypeId', 'BALANCE_ACCOUNT', 'organizationPartyId', 'DEMO_COMPANY1')
                .queryOne()
        assert glAccountTypeDefault
        assert glAccountTypeDefault.glAccountId == '999999'
    }

    @Test
    @Order(10)
    void testRemoveGlAccountTypeDefault() {
        Map serviceCtx = [
                glAccountTypeId: 'ACCOUNTS_PAYABLE',
                organizationPartyId: 'DEMO_COMPANY1',
                glAccountId: '999999',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('removeGlAccountTypeDefault', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue glAccountTypeDefault = from('GlAccountTypeDefault')
                .where('glAccountTypeId', 'ACCOUNTS_PAYABLE',
                        'organizationPartyId', 'DEMO_COMPANY1')
                .queryOne()
        assert !glAccountTypeDefault
    }

    @Test
    @Order(11)
    void testAddInvoiceItemTypeGlAssignment() {
        Map serviceCtx = [
                invoiceItemTypeId: 'PINV_FPROD_ITEM',
                organizationPartyId: 'DEMO_COMPANY1',
                glAccountId: '999999',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('addInvoiceItemTypeGlAssignment', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue invoiceItemTypeGlAccount = from('InvoiceItemTypeGlAccount')
                .where('invoiceItemTypeId', 'PINV_FPROD_ITEM',
                        'organizationPartyId', 'DEMO_COMPANY1')
                .queryOne()
        assert invoiceItemTypeGlAccount
        assert invoiceItemTypeGlAccount.glAccountId == '999999'
    }

    @Test
    @Order(12)
    void testRemoveInvoiceItemTypeGlAssignment() {
        Map serviceCtx = [
                invoiceItemTypeId: 'PINV_SALES_TAX',
                organizationPartyId: 'DEMO_COMPANY1',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('removeInvoiceItemTypeGlAssignment', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue invoiceItemTypeGlAccount = from('InvoiceItemTypeGlAccount')
                .where('invoiceItemTypeId', 'PINV_SALES_TAX',
                        'organizationPartyId', 'DEMO_COMPANY1')
                .queryOne()
        assert !invoiceItemTypeGlAccount
    }

    @Test
    @Order(13)
    void testAddPaymentTypeGlAssignment() {
        Map serviceCtx = [
                paymentTypeId: 'TAX_PAYMENT',
                organizationPartyId: 'DEMO_COMPANY1',
                glAccountTypeId: 'TAX_ACCOUNT',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('addPaymentTypeGlAssignment', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue paymentGlAccountTypeMap = from('PaymentGlAccountTypeMap')
                .where('paymentTypeId', 'TAX_PAYMENT',
                        'organizationPartyId', 'DEMO_COMPANY1')
                .queryOne()
        assert paymentGlAccountTypeMap
        assert paymentGlAccountTypeMap.glAccountTypeId == 'TAX_ACCOUNT'
    }

    @Test
    @Order(14)
    void testRemovePaymentMethodTypeGlAssignment() {
        Map serviceCtx = [
                paymentMethodTypeId: 'CASH',
                organizationPartyId: 'DEMO_COMPANY1',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('removePaymentMethodTypeGlAssignment', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue paymentMethodTypeGlAccount = from('PaymentMethodTypeGlAccount')
                .where('paymentMethodTypeId', 'CASH',
                        'organizationPartyId', 'DEMO_COMPANY1')
                .queryOne()
        assert !paymentMethodTypeGlAccount
    }

}
