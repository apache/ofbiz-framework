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

import org.ofbiz.service.*;
import org.ofbiz.entity.*;
import org.ofbiz.entity.condition.*;
import org.ofbiz.entity.util.*;
import org.ofbiz.base.util.*;
import org.ofbiz.order.shoppingcart.*;
import org.ofbiz.party.party.PartyWorker;
import org.ofbiz.product.catalog.CatalogWorker;
import org.ofbiz.product.store.ProductStoreWorker;
import org.ofbiz.order.shoppingcart.product.ProductDisplayWorker;
import org.ofbiz.order.shoppingcart.product.ProductPromoWorker;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityOperator;
import javolution.util.FastList;


// Get the Cart and Prepare Size
shoppingCart = ShoppingCartEvents.getCartObject(request);
context.cart = shoppingCart;

// check the selected product store
productStoreId = shoppingCart.getProductStoreId();
productStore = null;
if (productStoreId) {
    productStore = ProductStoreWorker.getProductStore(productStoreId, delegator);
    if (productStore) {
        // put in the default currency, to help selecting a currency for a purchase order
        context.defaultCurrencyUomId = productStore.defaultCurrencyUomId;
        payToPartyId = productStore.payToPartyId;
        partyId = shoppingCart.getOrderPartyId();

        exprsAgreements = FastList.newInstance();
        exprsAgreementRoles = FastList.newInstance();
        // get applicable agreements for order entry
        if ("PURCHASE_ORDER".equals(shoppingCart.getOrderType())) {
            // the agreement for a PO is from customer to payToParty (ie, us)
            exprsAgreements.add(EntityCondition.makeCondition("partyIdTo", EntityOperator.EQUALS, partyId));
            exprsAgreements.add(EntityCondition.makeCondition("partyIdFrom", EntityOperator.EQUALS, payToPartyId));
            agreements = delegator.findList("Agreement", EntityCondition.makeCondition(exprsAgreements, EntityOperator.AND), null, null, null, true);
            exprsAgreementRoles.add(EntityCondition.makeCondition("partyId", EntityOperator.EQUALS, partyId));
            exprsAgreementRoles.add(EntityCondition.makeCondition("roleTypeId", EntityOperator.EQUALS, "SUPPLIER"));
            agreementRoles = delegator.findList("AgreementRole", EntityCondition.makeCondition(exprsAgreementRoles, EntityOperator.AND), null, null, null, true);
            catalogCol = CatalogWorker.getAllCatalogIds(request);
        } else {
            // the agreement for a sales order is from us to the customer
            exprsAgreements.add(EntityCondition.makeCondition("partyIdTo", EntityOperator.EQUALS, payToPartyId));
            exprsAgreements.add(EntityCondition.makeCondition("partyIdFrom", EntityOperator.EQUALS, partyId));
            agreements = delegator.findList("Agreement", EntityCondition.makeCondition(exprsAgreements, EntityOperator.AND), null, null, null, true);
            exprsAgreementRoles.add(EntityCondition.makeCondition("partyId", EntityOperator.EQUALS, partyId));
            exprsAgreementRoles.add(EntityCondition.makeCondition("roleTypeId", EntityOperator.EQUALS, "CUSTOMER"));
            agreementRoles = delegator.findList("AgreementRole", EntityCondition.makeCondition(exprsAgreementRoles, EntityOperator.AND), null, null, null, true);
            catalogCol = CatalogWorker.getCatalogIdsAvailable(delegator, productStoreId, partyId);
        }

        agreements = EntityUtil.filterByDate(agreements);
        if (agreements) {
            context.agreements = agreements;
        }
        if (agreementRoles) {
            context.agreementRoles =agreementRoles;
        }


        if (catalogCol) {
            currentCatalogId = catalogCol.get(0);
            currentCatalogName = CatalogWorker.getCatalogName(request, currentCatalogId);
            context.catalogCol = catalogCol;
            context.currentCatalogId = currentCatalogId;
            context.currentCatalogName = currentCatalogName;
        }
    }
}

partyId = shoppingCart.getPartyId();
if ("_NA_".equals(partyId)) partyId = null;
context.partyId = partyId;

// currencies and shopping cart currency
currencies = delegator.findByAndCache("Uom", [uomTypeId : "CURRENCY_MEASURE"]);
context.currencies = currencies;
context.currencyUomId = shoppingCart.getCurrency();
