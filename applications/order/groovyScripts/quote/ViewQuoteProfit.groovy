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

import org.apache.ofbiz.base.util.Debug

costMult = 0.0
quoteCoefficients.each { quoteCoefficient ->
    value = quoteCoefficient.coeffValue
    if (value) {
        costMult += value
    }
}
totalProfit = 0.0
costToPriceMult = 1.0
if (costMult != 100) {
    costToPriceMult = 100 / (100 - costMult)
}

issueDate = quote.issueDate ?: nowTimestamp
totalCost = 0.0
totalPrice = 0.0
totalCostMult = 0.0
currency = quote.currencyUomId
quoteItemAndCostInfos = []
quoteItems.each { quoteItem ->
    defaultQuoteUnitPrice = 0.0
    averageCost = 0.0
    unitPrice = quoteItem.quoteUnitPrice ?: 0.0
    quantity = quoteItem.quantity ?: 1.0
    selectedAmount = quoteItem.selectedAmount ?: 1.0
    profit = 0.0
    percProfit = 0.0

    try {
        if (currency && quoteItem.productId) {
            productPrice = from("ProductPrice")
                              .where(productId : quoteItem.productId, currencyUomId : currency, productPriceTypeId : "AVERAGE_COST")
                              .filterByDate(issueDate)
                              .queryFirst()
            if (productPrice?.price != null) {
                averageCost = productPrice.price * selectedAmount
            }
        }
        totalCost += (averageCost * quantity)
        totalPrice += (unitPrice * quantity * selectedAmount)
    } catch (Exception exc) {
        Debug.logError("Problems getting the averageCost for quoteItem: " + quoteItem)
    }
    profit = unitPrice - averageCost
    percProfit = averageCost != 0 ? (profit / unitPrice) * 100.00 : 0.00
    quoteItemAndCostInfo = new java.util.HashMap(quoteItem)
    quoteItemAndCostInfo.averageCost = averageCost
    quoteItemAndCostInfo.profit = profit
    quoteItemAndCostInfo.percProfit = percProfit
    quoteItemAndCostInfos.add(quoteItemAndCostInfo)
}
totalProfit = totalPrice - totalCost

context.costMult = costMult
context.costToPriceMult = costToPriceMult
context.quoteItemAndCostInfos = quoteItemAndCostInfos

context.totalCost = totalCost
context.totalPrice = totalPrice
context.totalProfit = totalProfit
context.totalPercProfit = totalCost != 0 ? (totalProfit / totalPrice) * 100.00: 0.00
