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

import org.apache.ofbiz.entity.util.EntityUtil
import org.apache.ofbiz.base.util.UtilHttp
import org.apache.ofbiz.product.catalog.CatalogWorker
import org.apache.ofbiz.webapp.website.WebSiteWorker
import org.apache.ofbiz.entity.util.EntityUtilProperties


prodCatalogId = CatalogWorker.getCurrentCatalogId(request)
webSiteId = WebSiteWorker.getWebSiteId(request)

currencyUomId = parameters.currencyUomId ?: UtilHttp.getCurrencyUom(request)
context.currencyUomId = currencyUomId

partyId = parameters.partyId ?:request.getAttribute("partyId")

party = from("Party").where("partyId", partyId).queryOne()
context.party = party
if (party) {
    context.lookupPerson = party.getRelatedOne("Person", false)
    context.lookupGroup = party.getRelatedOne("PartyGroup", false)
}

shoppingListId = parameters.shoppingListId ?: request.getAttribute("shoppingListId")

//get the party for listid if it exists
if (!partyId && shoppingListId) {
    partyId = from("ShoppingList").where("shoppingListId", shoppingListId).queryOne().partyId
}
context.partyId = partyId

// get the top level shopping lists for the party
allShoppingLists = from("ShoppingList").where("partyId", partyId).queryList()
shoppingLists = EntityUtil.filterByAnd(allShoppingLists, [parentShoppingListId : null])
context.allShoppingLists = allShoppingLists
context.shoppingLists = shoppingLists

// get all shoppingListTypes
shoppingListTypes = from("ShoppingListType").orderBy("description").cache(true).queryList()
context.shoppingListTypes = shoppingListTypes

// no passed shopping list id default to first list
if (!shoppingListId) {
    firstList = EntityUtil.getFirst(shoppingLists)
    if (firstList) {
        shoppingListId = firstList.shoppingListId
    }
}

// if we passed a shoppingListId get the shopping list info
if (shoppingListId) {
    shoppingList = from("ShoppingList").where("shoppingListId", shoppingListId).queryOne()
    context.shoppingList = shoppingList
    context.shoppingListId = shoppingListId

    if (shoppingList) {
        shoppingListItemTotal = 0.0
        shoppingListChildTotal = 0.0

        shoppingListItems = shoppingList.getRelated("ShoppingListItem", null, null, true)
        if (shoppingListItems) {
            shoppingListItemDatas = new ArrayList(shoppingListItems.size())
            shoppingListItems.each { shoppingListItem ->
                shoppingListItemData = [:]
                product = shoppingListItem.getRelatedOne("Product", true)

                // DEJ20050704 not sure about calculating price here, will have some bogus data when not in a store webapp
                calcPriceOutMap = runService('calculateProductPrice', [product : product, quantity : shoppingListItem.quantity , currencyUomId : currencyUomId, userLogin : userLogin, productStoreId : shoppingList.productStoreId])
                price = calcPriceOutMap.price
                totalPrice = price * shoppingListItem.getDouble("quantity")
                shoppingListItemTotal += totalPrice

                productVariantAssocs = null
                if ("Y".equals(product.isVirtual)) {
                    productVariantAssocs = product.getRelated("MainProductAssoc", [productAssocTypeId : "PRODUCT_VARIANT"], ["sequenceNum"], true)
                    productVariantAssocs = EntityUtil.filterByDate(productVariantAssocs)
                }

                shoppingListItemData.shoppingListItem = shoppingListItem
                shoppingListItemData.product = product
                shoppingListItemData.unitPrice = price
                shoppingListItemData.totalPrice = totalPrice
                shoppingListItemData.productVariantAssocs = productVariantAssocs
                shoppingListItemDatas.add(shoppingListItemData)
            }
            context.shoppingListItemDatas = shoppingListItemDatas
            // pagination for the shopping list
            viewIndex = Integer.valueOf(parameters.VIEW_INDEX  ?: 0)
            viewSize = parameters.VIEW_SIZE ?Integer.valueOf(parameters.VIEW_SIZE): modelTheme.getDefaultViewSize()?:20
            listSize = shoppingListItemDatas ? shoppingListItemDatas.size() : 0

            lowIndex = (viewIndex * viewSize) + 1
            highIndex = (viewIndex + 1) * viewSize
            highIndex = highIndex > listSize ? listSize : highIndex
            lowIndex = lowIndex > highIndex ? highIndex : lowIndex

            context.viewIndex = viewIndex
            context.viewSize = viewSize
            context.listSize = listSize
            context.lowIndex = lowIndex
            context.highIndex = highIndex
        }

        shoppingListType = shoppingList.getRelatedOne("ShoppingListType", false)
        context.shoppingListType = shoppingListType

        // get the child shopping lists of the current list for the logged in user
        childShoppingLists = from("ShoppingList").where("partyId", partyId, "parentShoppingListId", shoppingListId).orderBy("listName").cache(true).queryList()
        // now get prices for each child shopping list...
        if (childShoppingLists) {
            childShoppingListDatas = new ArrayList(childShoppingLists.size())
            childShoppingListDatas.each { childShoppingList ->
                childShoppingListData = [:]
                calcListPriceInMap = [shoppingListId : childShoppingList.shoppingListId , prodCatalogId : prodCatalogId , webSiteId : webSiteId, userLogin : userLogin]
                childShoppingListData.childShoppingList = childShoppingList
                childShoppingListDatas.add(childShoppingListData)
            }
            context.childShoppingListDatas = childShoppingListDatas
        }

        // get the parent shopping list if there is one
        parentShoppingList = shoppingList.getRelatedOne("ParentShoppingList", false)
        context.parentShoppingList = parentShoppingList
    }
}

