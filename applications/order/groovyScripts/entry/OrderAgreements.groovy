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

import org.apache.ofbiz.entity.condition.EntityCondition
import org.apache.ofbiz.entity.condition.EntityOperator
import org.apache.ofbiz.order.shoppingcart.ShoppingCartEvents
import org.apache.ofbiz.product.catalog.CatalogWorker


shoppingCart = ShoppingCartEvents.getCartObject(request)
context.cart = shoppingCart

// get applicable agreements for order entry
if ('PURCHASE_ORDER'.equals(shoppingCart.getOrderType())) {

    // for a purchase order, orderPartyId = billFromVendor (the supplier)
    supplierPartyId = shoppingCart.getOrderPartyId()
    customerPartyId = shoppingCart.getBillToCustomerPartyId()

    // the agreement for a purchse order is from us to the supplier
    agreementCondition = EntityCondition.makeCondition([
            EntityCondition.makeCondition('partyIdTo', EntityOperator.EQUALS, supplierPartyId),
            EntityCondition.makeCondition('partyIdFrom', EntityOperator.EQUALS, customerPartyId)
    ], EntityOperator.AND)

    agreementRoleCondition = EntityCondition.makeCondition([
            EntityCondition.makeCondition('partyId', EntityOperator.EQUALS, supplierPartyId),
            EntityCondition.makeCondition('roleTypeId', EntityOperator.EQUALS, 'SUPPLIER')
    ], EntityOperator.AND)

} else {

    // for a sales order, orderPartyId = billToCustomer (the customer)
    customerPartyId = shoppingCart.getOrderPartyId()
    companyPartyId = shoppingCart.getBillFromVendorPartyId()

    // the agreement for a sales order is from the customer to us
    agreementCondition = EntityCondition.makeCondition([
            EntityCondition.makeCondition('partyIdTo', EntityOperator.EQUALS, companyPartyId),
            EntityCondition.makeCondition('partyIdFrom', EntityOperator.EQUALS, customerPartyId)
    ], EntityOperator.AND)

    agreementRoleCondition = EntityCondition.makeCondition([
            EntityCondition.makeCondition('partyId', EntityOperator.EQUALS, customerPartyId),
            EntityCondition.makeCondition('roleTypeId', EntityOperator.EQUALS, 'CUSTOMER')
    ], EntityOperator.AND)

}

agreements = from("Agreement").where(agreementCondition).filterByDate().cache(true).queryList()
if (agreements) {
    context.agreements = agreements
}

agreementRoles = from("AgreementRole").where(agreementRoleCondition).cache(true).queryList()
if (agreementRoles) {
    context.agreementRoles = agreementRoles
}

// catalog id collection, current catalog id and name
productStoreId = shoppingCart.getProductStoreId()
if ('SALES_ORDER' == shoppingCart.getOrderType() && productStoreId) {
    catalogCol = CatalogWorker.getCatalogIdsAvailable(delegator, productStoreId, shoppingCart.getOrderPartyId())
} else {
    catalogCol = CatalogWorker.getAllCatalogIds(request)
}

if (catalogCol) {
    context.catalogCol = catalogCol

    currentCatalogId = catalogCol.get(0)
    context.currentCatalogId = currentCatalogId
    context.currentCatalogName = CatalogWorker.getCatalogName(request, currentCatalogId)
}

// currencies and shopping cart currency
context.currencies = from("Uom").where("uomTypeId", "CURRENCY_MEASURE").cache(true).queryList()
context.currencyUomId = shoppingCart.getCurrency()
