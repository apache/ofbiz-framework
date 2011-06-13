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

import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.product.catalog.CatalogWorker;

prodCatalogId = CatalogWorker.getCurrentCatalogId(request);
webSiteId = CatalogWorker.getWebSiteId(request);

currencyUomId = parameters.currencyUomId ?: UtilHttp.getCurrencyUom(request);
context.currencyUomId = currencyUomId;

partyId = parameters.partyId ?:request.getAttribute("partyId");

party = delegator.findByPrimaryKey("Party", [partyId : partyId]);
context.party = party;
if (party) {
    context.lookupPerson = party.getRelatedOne("Person");
    context.lookupGroup = party.getRelatedOne("PartyGroup");
}

shoppingListId = parameters.shoppingListId ?: request.getAttribute("shoppingListId");

//get the party for listid if it exists
if (!partyId && shoppingListId) {
    partyId = delegator.findByPrimaryKey("ShoppingList", [shoppingListId : shoppingListId]).partyId;
}
context.partyId = partyId;

// get the top level shopping lists for the party
allShoppingLists = delegator.findByAnd("ShoppingList", [partyId : partyId], ["listName"]);
shoppingLists = EntityUtil.filterByAnd(allShoppingLists, [parentShoppingListId : null]);
context.allShoppingLists = allShoppingLists;
context.shoppingLists = shoppingLists;

// get all shoppingListTypes
shoppingListTypes = delegator.findList("ShoppingListType", null, null, ["description"], null, true);
context.shoppingListTypes = shoppingListTypes;

// no passed shopping list id default to first list
if (!shoppingListId) {
    firstList = EntityUtil.getFirst(shoppingLists);
    if (firstList) {
        shoppingListId = firstList.shoppingListId;
    }
}

// if we passed a shoppingListId get the shopping list info
if (shoppingListId) {
    shoppingList = delegator.findByPrimaryKey("ShoppingList", [shoppingListId : shoppingListId]);
    context.shoppingList = shoppingList;

    if (shoppingList) {
        shoppingListItemTotal = 0.0;
        shoppingListChildTotal = 0.0;

        shoppingListItems = shoppingList.getRelatedCache("ShoppingListItem");
        if (shoppingListItems) {
            shoppingListItemDatas = new ArrayList(shoppingListItems.size());
            shoppingListItems.each { shoppingListItem ->
                shoppingListItemData = [:];
                product = shoppingListItem.getRelatedOneCache("Product");

                // DEJ20050704 not sure about calculating price here, will have some bogus data when not in a store webapp
                calcPriceInMap = [product : product, quantity : shoppingListItem.quantity , currencyUomId : currencyUomId, userLogin : userLogin, productStoreId : shoppingList.productStoreId];
                calcPriceOutMap = dispatcher.runSync("calculateProductPrice", calcPriceInMap);
                price = calcPriceOutMap.price;
                totalPrice = price * shoppingListItem.getDouble("quantity");
                shoppingListItemTotal += totalPrice;

                productVariantAssocs = null;
                if ("Y".equals(product.isVirtual)) {
                    productVariantAssocs = product.getRelatedCache("MainProductAssoc", [productAssocTypeId : "PRODUCT_VARIANT"], ["sequenceNum"]);
                    productVariantAssocs = EntityUtil.filterByDate(productVariantAssocs);
                }

                shoppingListItemData.shoppingListItem = shoppingListItem;
                shoppingListItemData.product = product;
                shoppingListItemData.unitPrice = price;
                shoppingListItemData.totalPrice = totalPrice;
                shoppingListItemData.productVariantAssocs = productVariantAssocs;
                shoppingListItemDatas.add(shoppingListItemData);
            }
            context.shoppingListItemDatas = shoppingListItemDatas;
        }

        shoppingListType = shoppingList.getRelatedOne("ShoppingListType");
        context.shoppingListType = shoppingListType;

        // get the child shopping lists of the current list for the logged in user
        childShoppingLists = delegator.findByAndCache("ShoppingList", [partyId : partyId, parentShoppingListId : shoppingListId], ["listName"]);
        // now get prices for each child shopping list...
        if (childShoppingLists) {
            childShoppingListDatas = new ArrayList(childShoppingLists.size());
            childShoppingListDatas.each { childShoppingList ->
                childShoppingListData = [:];
                calcListPriceInMap = [shoppingListId : childShoppingList.shoppingListId , prodCatalogId : prodCatalogId , webSiteId : webSiteId, userLogin : userLogin];
                childShoppingListData.childShoppingList = childShoppingList;
                childShoppingListDatas.add(childShoppingListData);
            }
            context.childShoppingListDatas = childShoppingListDatas;
        }

        // get the parent shopping list if there is one
        parentShoppingList = shoppingList.getRelatedOne("ParentShoppingList");
        context.parentShoppingList = parentShoppingList;
    }
}

