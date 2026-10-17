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
package org.apache.ofbiz.accounting.invoice

import org.apache.ofbiz.entity.GenericValue

/**
 * Sample Calculate Affiliate Commission
 * @return Success response
 */
Map sampleCalculateAffiliateCommission() {
    List invoiceIds = []
    GenericValue payment = from('Payment').where(parameters).queryOne()

    // find affiliate or commission partner for payment.partyIdFrom; will be relationship from CUSTOMER to AFFILIATE, type SALES_AFFILIATE
    from('PartyRelationship')
            .where(partyIdFrom: payment.partyIdFrom,
                    roleTypeIdFrom: 'CUSTOMER',
                    partyRelationshipTypeId: 'SALES_AFFILIATE')
            .filterByDate()
            .queryList()
            .each {
                // Calculate a commission for each commission partner, identified by affiliatePartyRelationship.partyIdTo
                if (it.roleTypeIdTo == 'AFFILIATE') {
                    BigDecimal commissionAmount = 10.0 + payment.amount * 0.15
                    invoiceIds << createCommissionInvoiceInline(payment, it.partyIdTo, commissionAmount).invoiceId
                } else if (it.roleTypeIdTo == 'TIERED_COMMISSION') {
                    // NOTE: this is just an example of another type of commission partner associated with a customer, doesn't really exist
                    invoiceIds << 'something to do'
                }
            }
    return success([invoiceIds: invoiceIds])
}

Map createCommissionInvoiceInline(GenericValue payment, String commissionPartyId, BigDecimal commissionAmount) {
    Map serviceResult = run service: 'createInvoice', with: [invoiceTypeId: 'COMMISSION_INVOICE',
                                                             statusId: 'INVOICE_RECEIVED',
                                                             partyIdFrom: commissionPartyId,
                                                             partyId: payment.partyIdTo]
    run service: 'createInvoiceItem', with: [invoiceId: serviceResult.invoiceId,
                                             invoiceItemTypeId: 'COMM_INV_ITEM',
                                             amount: commissionAmount,
                                             quantity: 1,
                                             description: "Commission for Received Customer Payment [${payment.paymentId}]"]
    return serviceResult
}
