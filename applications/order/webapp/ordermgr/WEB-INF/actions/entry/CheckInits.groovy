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

productStore = ProductStoreWorker.getProductStore(request);
if (productStore) {
    context.defaultProductStore = productStore;
    if (productStore.defaultSalesChannelEnumId)
        context.defaultSalesChannel = delegator.findByPrimaryKeyCache("Enumeration", [enumId : productStore.defaultSalesChannelEnumId]);
}
// Get the Cart
shoppingCart = session.getAttribute("shoppingCart");
context.shoppingCart = shoppingCart;

salesChannels = delegator.findByAndCache("Enumeration", [enumTypeId : "ORDER_SALES_CHANNEL"], ["sequenceId"]);
context.salesChannels = salesChannels;

productStores = delegator.findList("ProductStore", null, null, ["productStoreId", "storeName"], null, true);
context.productStores = productStores;

suppliers = delegator.findByAnd("PartyRoleAndPartyDetail", [roleTypeId : "SUPPLIER"], ["groupName", "partyId"]);
context.suppliers = suppliers;

organizations = delegator.findByAnd("PartyRole", [roleTypeId : "INTERNAL_ORGANIZATIO"]);
context.organizations = organizations;
