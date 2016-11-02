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

import org.apache.ofbiz.base.util.UtilMisc
import org.apache.ofbiz.entity.util.EntityUtil

costMult = 0.0
quoteCoefficients.each { quoteCoefficient ->
    value = quoteCoefficient.coeffValue
    if (value) {
        costMult += value
    }
}
costToPriceMult = 1.0
if (costMult != 100) {
    costToPriceMult = 100 / (100 - costMult)
}

totalCost = 0.0
totalPrice = 0.0
totalCostMult = 0.0
currency = quote.currencyUomId
quoteItemAndCostInfos = []
quoteItems.each { quoteItem ->
    defaultQuoteUnitPrice = 0.0
    averageCost = 0.0
    unitPrice = 0.0
    quantity = 1.0
    selectedAmount = quoteItem.selectedAmount ?: 1.0
    if (quoteItem.quantity != null) {
        quantity = quoteItem.quantity
    }
    if (quoteItem.quoteUnitPrice != null) {
        unitPrice = quoteItem.quoteUnitPrice
    }

    try {
        if (currency && quoteItem.productId) {
            productPrice = from("ProductPrice").where("productId", quoteItem.productId, "currencyUomId", currency, "productPriceTypeId", "AVERAGE_COST").filterByDate().queryFirst()
            if (productPrice?.price != null) {
                averageCost = productPrice.price
            }
        }
        defaultQuoteUnitPrice = averageCost * costToPriceMult * selectedAmount
        totalCost += (averageCost * quantity)
        totalPrice += (unitPrice * quantity * selectedAmount)
    } catch (Exception exc) {
        Debug.logError("Problems getting the averageCost for quoteItem: " + quoteItem)
    }

    quoteItemAndCostInfo = new java.util.HashMap(quoteItem)
    quoteItemAndCostInfo.averageCost = averageCost
    quoteItemAndCostInfo.costToPriceMult = costToPriceMult
    quoteItemAndCostInfo.defaultQuoteUnitPrice = defaultQuoteUnitPrice
    quoteItemAndCostInfos.add(quoteItemAndCostInfo)
}

context.costMult = costMult
context.costToPriceMult = costToPriceMult
context.quoteItemAndCostInfos = quoteItemAndCostInfos

context.totalCost = totalCost
context.totalPrice = totalPrice
context.totalCostMult = (totalCost != 0 ? totalPrice / totalCost : 0)

