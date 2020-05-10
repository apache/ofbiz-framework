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
package org.apache.ofbiz.accounting

import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.service.ServiceUtil
import org.apache.ofbiz.service.testtools.OFBizTestCase

class AutoAcctgAgreementTests extends OFBizTestCase {
    public AutoAcctgAgreementTests(String name) {
        super(name)
    }

    void testAddPaymentMethodTypeGlAssignment() {
        Map serviceCtx = [
                agreementId: '1000',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('expireAgreement', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue agreement = from('Agreement')
                .where('agreementId', '1000')
                .filterByDate()
                .queryOne()
        assert agreement == null
    }

    void testCopyAgreement() {
        Map serviceCtx = [
                agreementId: '1010',
                copyAgreementTerms: 'N',
                copyAgreementProducts: 'Y',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('copyAgreement', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue agreement = from('Agreement')
                .where('agreementId', serviceResult.agreementId)
                .queryOne()
        List<GenericValue> agreementItems = agreement.getRelated("AgreementItem", null, null, false)
        List<GenericValue> agreementTerms = agreement.getRelated("AgreementTerm", null, null, false)
        List<GenericValue> agreementProductAppls = agreement.getRelated("AgreementProductAppl", null, null, false)

        assert agreement
        assert agreementItems
        assert agreementTerms?.isEmpty()
        assert agreementProductAppls
    }

    void testGetCommissionForProduct() {
        Map serviceCtx = [
                productId: 'TestProduct2',
                invoiceItemSeqId: 'COMM_INV_ITEM',
                invoiceItemTypeId: 'COMM_INV_ITEM',
                amount: 100.00,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('getCommissionForProduct', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        List<Map<String, Object>> commissions = serviceResult.commissions
        Map<String, Object> commission = commissions?.first()

        assert commission
        assert commission.commission == 10.00
    }

}