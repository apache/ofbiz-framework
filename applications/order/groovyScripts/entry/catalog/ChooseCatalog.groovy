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

import org.apache.ofbiz.product.catalog.CatalogWorker
import org.apache.ofbiz.order.shoppingcart.ShoppingCartEvents

catalogCol = null
currentCatalogId = null
currentCatalogName = null

cart = ShoppingCartEvents.getCartObject(request)
productStoreId = cart.getProductStoreId()
partyId = cart.getOrderPartyId()

currentCatalogId = CatalogWorker.getCurrentCatalogId(request)

if ("SALES_ORDER".equals(cart.getOrderType())) {
    catalogCol = CatalogWorker.getCatalogIdsAvailable(delegator, productStoreId, partyId)
} else {
    catalogCol = CatalogWorker.getAllCatalogIds(request)
    if (!currentCatalogId && catalogCol) {
        currentCatalogId = catalogCol.get(0)
    }
    session.setAttribute("CURRENT_CATALOG_ID", currentCatalogId)
}
currentCatalogName = CatalogWorker.getCatalogName(request, currentCatalogId)

context.catalogCol = catalogCol
context.currentCatalogId = currentCatalogId
context.currentCatalogName = currentCatalogName
