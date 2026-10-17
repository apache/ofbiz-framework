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

import org.apache.ofbiz.entity.condition.EntityConditionBuilder
import org.apache.ofbiz.entity.util.EntityUtil

exprBldr =  new EntityConditionBuilder()
invoice = context.invoice
if (!invoice) {
    return
}
glAccountOrganizationAndClassList = null
switch (invoice.invoiceTypeId) {
    case 'SALES_INVOICE':
        itemTypesCond = exprBldr.OR {
            EQUALS(invoiceItemTypeId: 'SINVOICE_ADJ')
            EQUALS(parentTypeId: 'SINVOICE_ADJ')
            EQUALS(invoiceItemTypeId: 'SINVOICE_ITM_ADJ')
            EQUALS(parentTypeId: 'SINVOICE_ITM_ADJ')
            EQUALS(invoiceItemTypeId: 'INV_PROD_ITEM')
            EQUALS(parentTypeId: 'INV_PROD_ITEM')
        }
        invoiceItemTypes = from('InvoiceItemType').where(itemTypesCond).orderBy(['parentTypeId', 'invoiceItemTypeId']).queryList()
        glAccountOrganizationAndClassList = from('GlAccountOrganizationAndClass').where('organizationPartyId', invoice.partyIdFrom).queryList()
        break
    case 'PURCHASE_INVOICE':
        itemTypesCond = exprBldr.OR {
            EQUALS(invoiceItemTypeId: 'PINVOICE_ADJ')
            EQUALS(parentTypeId: 'PINVOICE_ADJ')
            EQUALS(invoiceItemTypeId: 'PINVOICE_ITM_ADJ')
            EQUALS(parentTypeId: 'PINVOICE_ITM_ADJ')
            EQUALS(invoiceItemTypeId: 'PINV_PROD_ITEM')
            EQUALS(parentTypeId: 'PINV_PROD_ITEM')
        }
        invoiceItemTypes = from('InvoiceItemType').where(itemTypesCond).orderBy(['parentTypeId', 'invoiceItemTypeId']).queryList()
        glAccountOrganizationAndClassList = from('GlAccountOrganizationAndClass').where('organizationPartyId', invoice.partyId).queryList()
        break
    case 'PAYROL_INVOICE':
        itemTypesCond = exprBldr.OR {
            EQUALS(invoiceItemTypeId: 'PAYROL_EARN_HOURS')
            EQUALS(parentTypeId: 'PAYROL_EARN_HOURS')
            EQUALS(invoiceItemTypeId: 'PAYROL_DD_FROM_GROSS')
            EQUALS(parentTypeId: 'PAYROL_DD_FROM_GROSS')
            EQUALS(invoiceItemTypeId: 'PAYROL_TAXES')
            EQUALS(parentTypeId: 'PAYROL_TAXES')
        }
        invoiceItemTypes = from('InvoiceItemType').where(itemTypesCond).orderBy(['parentTypeId', 'invoiceItemTypeId']).queryList()
        glAccountOrganizationAndClassList = from('GlAccountOrganizationAndClass').where('organizationPartyId', invoice.partyId).queryList()
        break
    case 'COMMISSION_INVOICE':
        itemTypesCond = exprBldr.OR {
            EQUALS(invoiceItemTypeId: 'COMM_INV_ITEM')
            EQUALS(parentTypeId: 'COMM_INV_ITEM')
            EQUALS(invoiceItemTypeId: 'COMM_INV_ADJ')
            EQUALS(parentTypeId: 'COMM_INV_ADJ')
        }
        invoiceItemTypes = from('InvoiceItemType').where(itemTypesCond).orderBy(['parentTypeId', 'invoiceItemTypeId']).queryList()
        glAccountOrganizationAndClassList = from('GlAccountOrganizationAndClass').where('organizationPartyId', invoice.partyId).queryList()
        break
    default:
        map = from('InvoiceItemTypeMap').where('invoiceTypeId', invoice.invoiceTypeId).cache(true).queryList()
        invoiceItemTypes = EntityUtil.getRelated('InvoiceItemType', null, map, false)
        break
}
context.invoiceItemTypes = invoiceItemTypes

context.glAccountOrganizationAndClassList = glAccountOrganizationAndClassList
