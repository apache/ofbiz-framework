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

/*
 * Script to build the open order item report using
 * the OrderItemQuantityReportGroupByItem view.
 */


import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.condition.*;
import org.ofbiz.entity.util.*;
import org.ofbiz.entity.*;
import org.ofbiz.base.util.*;

productStoreId = parameters.productStoreId;
orderTypeId = parameters.orderTypeId;
orderStatusId = parameters.orderStatusId;

// search by orderTypeId is mandatory
conditions = [EntityCondition.makeCondition("orderTypeId", EntityOperator.EQUALS, orderTypeId)];

if (fromOrderDate) {
    conditions.add(EntityCondition.makeCondition("orderDate", EntityOperator.GREATER_THAN_EQUAL_TO, fromOrderDate));
}
if (thruOrderDate) {
    conditions.add(EntityCondition.makeCondition("orderDate", EntityOperator.LESS_THAN_EQUAL_TO, thruOrderDate));
}

if (productStoreId) {
    conditions.add(EntityCondition.makeCondition("productStoreId", EntityOperator.EQUALS, productStoreId));
    // for generating a title (given product store)
    context.productStore = from("ProductStore").where("productStoreId", productStoreId).cache(true).queryOne();
} else {
    // for generating a title (all stores)  TODO: use UtilProperties to internationalize
    context.productStore = [storeName : "All Stores"];
}
if (orderStatusId) {
    conditions.add(EntityCondition.makeCondition("orderStatusId", EntityOperator.EQUALS, orderStatusId));
} else {
    // search all orders that are not completed, cancelled or rejected
    conditions.add(
            EntityCondition.makeCondition([
                    EntityCondition.makeCondition("orderStatusId", EntityOperator.NOT_EQUAL, "ORDER_COMPLETED"),
                    EntityCondition.makeCondition("orderStatusId", EntityOperator.NOT_EQUAL, "ORDER_CANCELLED"),
                    EntityCondition.makeCondition("orderStatusId", EntityOperator.NOT_EQUAL, "ORDER_REJECTED")
                    ], EntityOperator.AND)
            );
}

// item conditions
conditions.add(EntityCondition.makeCondition("orderItemStatusId", EntityOperator.NOT_EQUAL, "ITEM_COMPLETED"));
conditions.add(EntityCondition.makeCondition("orderItemStatusId", EntityOperator.NOT_EQUAL, "ITEM_CANCELLED"));
conditions.add(EntityCondition.makeCondition("orderItemStatusId", EntityOperator.NOT_EQUAL, "ITEM_REJECTED"));

// get the results as an entity list iterator
listIt = select("orderId", "orderDate", "productId", "quantityOrdered", "quantityIssued", "quantityOpen", "shipBeforeDate", "shipAfterDate", "itemDescription")
            .from("OrderItemQuantityReportGroupByItem")
            .where(conditions)
            .orderBy("orderDate DESC")
            .cursorScrollInsensitive()
            .distinct()
            .queryIterator();
orderItemList = [];
totalCostPrice = 0.0;
totalListPrice = 0.0;
totalMarkup = 0.0;
totalDiscount = 0.0;
totalRetailPrice = 0.0;
totalquantityOrdered = 0.0;
totalquantityOpen = 0.0;

listIt.each { listValue ->
    orderId = listValue.orderId;
    productId = listValue.productId;
    orderDate = listValue.orderDate;
    quantityOrdered = listValue.quantityOrdered;
    quantityOpen = listValue.quantityOpen;
    quantityIssued = listValue.quantityIssued;
    itemDescription = listValue.itemDescription;
    shipAfterDate = listValue.shipAfterDate;
    shipBeforeDate = listValue.shipBeforeDate;
    productIdCondExpr =  [EntityCondition.makeCondition("productId", EntityOperator.EQUALS, productId)];
    productPrices = select("price","productPriceTypeId").from("ProductPrice").where(productIdCondExpr).queryList();
    costPrice = 0.0;
    retailPrice = 0.0;
    listPrice = 0.0;

    productPrices.each { productPriceMap ->
        if (productPriceMap.productPriceTypeId.equals("AVERAGE_COST")) {
            costPrice = productPriceMap.price;
        } else if (productPriceMap.productPriceTypeId.equals("DEFAULT_PRICE")) {
            retailPrice = productPriceMap.price;
        } else if (productPriceMap.productPriceTypeId.equals("LIST_PRICE")) {
            listPrice = productPriceMap.price;
        }
    }

    totalListPrice += listPrice;
    totalRetailPrice += retailPrice;
    totalCostPrice += costPrice;
    totalquantityOrdered += quantityOrdered;
    totalquantityOpen += quantityOpen;
    costPriceDividendValue = costPrice;
    if (costPriceDividendValue) {
        percentMarkup = ((retailPrice - costPrice)/costPrice)*100;
    } else{
        percentMarkup = "";
    }
    orderItemMap = [orderDate : orderDate,
                    orderId : orderId,
                    productId : productId,
                    itemDescription : itemDescription,
                    quantityOrdered : quantityOrdered,
                    quantityIssued : quantityIssued,
                    quantityOpen : quantityOpen,
                    shipAfterDate : shipAfterDate,
                    shipBeforeDate : shipBeforeDate,
                    costPrice : costPrice,
                    retailPrice : retailPrice,
                    listPrice : listPrice,
                    discount : listPrice - retailPrice,
                    calculatedMarkup : retailPrice - costPrice,
                    percentMarkup : percentMarkup];
    orderItemList.add(orderItemMap);
}

listIt.close();
totalAmountList = [];
if (orderItemList) {
    totalCostPriceDividendValue = totalCostPrice;
    if (totalCostPriceDividendValue) {
        totalPercentMarkup = ((totalRetailPrice - totalCostPrice)/totalCostPrice)*100 ;
    } else{
        totalPercentMarkup = "";
    }
    totalAmountMap = [totalCostPrice : totalCostPrice,
                      totalListPrice : totalListPrice,
                      totalRetailPrice : totalRetailPrice,
                      totalquantityOrdered : totalquantityOrdered,
                      quantityOrdered : quantityOrdered,
                      totalquantityOpen : totalquantityOpen,
                      totalDiscount : totalListPrice - totalRetailPrice,
                      totalMarkup : totalRetailPrice - totalCostPrice,
                      totalPercentMarkup : totalPercentMarkup];
    totalAmountList.add(totalAmountMap);
}
context.orderItemList = orderItemList;
context.totalAmountList = totalAmountList;
