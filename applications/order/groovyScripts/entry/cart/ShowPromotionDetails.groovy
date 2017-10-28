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
import org.apache.ofbiz.order.shoppingcart.product.ProductPromoWorker

productPromoId = request.getParameter("productPromoId")
if (!productPromoId) productPromoId = parameters.productPromoId
productPromo = from("ProductPromo").where("productPromoId", productPromoId).queryOne()

promoAutoDescription = ProductPromoWorker.makeAutoDescription(productPromo, delegator, locale, request.getAttribute("dispatcher"))

productPromoCategoryList = from("ProductPromoCategory").where("productPromoId", productPromoId).cache(true).queryList()
productPromoCategoryIncludeList = EntityUtil.filterByAnd(productPromoCategoryList, [productPromoApplEnumId : "PPPA_INCLUDE"])
productPromoCategoryExcludeList = EntityUtil.filterByAnd(productPromoCategoryList, [productPromoApplEnumId : "PPPA_EXCLUDE"])
productPromoCategoryAlwaysList = EntityUtil.filterByAnd(productPromoCategoryList, [productPromoApplEnumId : "PPPA_ALWAYS"])

productIdsCond = [] as Set
productIdsAction = [] as Set
ProductPromoWorker.makeProductPromoCondActionIdSets(productPromoId, productIdsCond, productIdsAction, delegator, null)

productIds = new TreeSet(productIdsCond)
productIds.addAll(productIdsAction)

context.productPromoId = productPromoId
context.productPromo = productPromo
context.promoAutoDescription = promoAutoDescription

context.productPromoCategoryIncludeList = productPromoCategoryIncludeList
context.productPromoCategoryExcludeList = productPromoCategoryExcludeList
context.productPromoCategoryAlwaysList = productPromoCategoryAlwaysList

context.productIdsCond = productIdsCond
context.productIdsAction = productIdsAction
context.productIds = productIds as List

viewIndex = 0
viewSize = 10
highIndex = 0
lowIndex = 0
listSize = productIds.size()

try {
    viewIndex = Integer.valueOf(request.getParameter("VIEW_INDEX"))
} catch (Exception e) {
    viewIndex = 0
}

try {
    viewSize = Integer.valueOf(request.getParameter("VIEW_SIZE"))
} catch (Exception e) {
    viewSize = 10
}

lowIndex = viewIndex * viewSize
highIndex = (viewIndex + 1) * viewSize
if (listSize < highIndex) {
    highIndex = listSize
}

context.viewIndex = viewIndex
context.viewSize = viewSize
context.listSize = listSize
context.lowIndex = lowIndex
context.highIndex = highIndex
