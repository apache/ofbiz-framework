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

import javolution.util.FastList;
import javolution.util.FastSet;
import javolution.util.FastMap;
import org.ofbiz.base.util.*;
import org.ofbiz.entity.*;
import org.ofbiz.entity.condition.*;
import org.ofbiz.entity.util.*;

//NOTE: this code ignores from/thru dates on the products and categories under the rootProductCategoryId!

//TODO_MAYBE: variant product support for products in category, and product shown directly (should we really do this?)
//TODO:

// get products and categories under the root category
productMemberList = delegator.findByAnd("ProductCategoryMember", [productCategoryId : rootProductCategoryId], ["sequenceNum"]);
categoryRollupList = delegator.findByAnd("ProductCategoryRollup", [parentProductCategoryId : rootProductCategoryId], ["sequenceNum"]);

// for use in the queries
productIdSet = FastSet.newInstance();
productCategoryIdSet = FastSet.newInstance();

// for use in the templates
productList = FastList.newInstance();
productCategoryList = FastList.newInstance();

productMemberList.each { productMember ->
    if (!productIdSet.contains(productMember.productId)) {
        productList.add(productMember.getRelatedOneCache("Product"));
    }
    productIdSet.add(productMember.productId);
}
categoryRollupList.each { categoryRollup ->
    if (!productCategoryIdSet.contains(categoryRollup.productCategoryId)) {
        productCategoryList.add(categoryRollup.getRelatedOneCache("CurrentProductCategory"));
    }
    productCategoryIdSet.add(categoryRollup.productCategoryId);
}

productFieldsToSelect = UtilMisc.toSet("productId", "quantityTotal", "amountTotal");

//NOTE: tax, etc also have productId on them, so restrict by type INV_PROD_ITEM, INV_FPROD_ITEM, INV_DPROD_ITEM, others?
baseProductAndExprs = FastList.newInstance();
baseProductAndExprs.add(EntityCondition.makeCondition("invoiceTypeId", EntityOperator.EQUALS, "SALES_INVOICE"));
baseProductAndExprs.add(EntityCondition.makeCondition("invoiceItemTypeId", EntityOperator.IN, ["INV_PROD_ITEM", "INV_FPROD_ITEM", "INV_DPROD_ITEM"]));
baseProductAndExprs.add(EntityCondition.makeCondition("statusId", EntityOperator.IN, ["INVOICE_READY", "INVOICE_PAID"]));
if (organizationPartyId) baseProductAndExprs.add(EntityCondition.makeCondition("partyIdFrom", EntityOperator.EQUALS, organizationPartyId));
if (currencyUomId) baseProductAndExprs.add(EntityCondition.makeCondition("currencyUomId", EntityOperator.EQUALS, currencyUomId));

categoryFieldsToSelect = UtilMisc.toSet("productCategoryId", "quantityTotal", "amountTotal");

baseCategoryAndExprs = FastList.newInstance();
baseCategoryAndExprs.add(EntityCondition.makeCondition("invoiceTypeId", EntityOperator.EQUALS, "SALES_INVOICE"));
baseCategoryAndExprs.add(EntityCondition.makeCondition("invoiceItemTypeId", EntityOperator.IN, ["INV_PROD_ITEM", "INV_FPROD_ITEM", "INV_DPROD_ITEM"]));
baseCategoryAndExprs.add(EntityCondition.makeCondition("statusId", EntityOperator.IN, ["INVOICE_READY", "INVOICE_PAID"]));
if (productCategoryIdSet) baseCategoryAndExprs.add(EntityCondition.makeCondition("productCategoryId", EntityOperator.IN, productCategoryIdSet));
if (productIdSet) baseCategoryAndExprs.add(EntityCondition.makeCondition("productId", EntityOperator.NOT_IN, productIdSet));
if (organizationPartyId) baseCategoryAndExprs.add(EntityCondition.makeCondition("partyIdFrom", EntityOperator.EQUALS, organizationPartyId));
if (currencyUomId) baseCategoryAndExprs.add(EntityCondition.makeCondition("currencyUomId", EntityOperator.EQUALS, currencyUomId));


// get the Calendar object for the current month (specifed by month, year Integer values in the context)
monthCal = Calendar.getInstance();
monthCal.set(Calendar.YEAR, year);
monthCal.set(Calendar.MONTH, (month - 1));

nextMonthCal = Calendar.getInstance();
nextMonthCal.setTimeInMillis(monthCal.getTimeInMillis());
nextMonthCal.add(Calendar.MONTH, 1);

// iterate through the days and do the queries
productResultMapByDayList = FastList.newInstance();
productNullResultByDayList = FastList.newInstance();
categoryResultMapByDayList = FastList.newInstance();

monthProductResultMap = FastMap.newInstance();
monthCategoryResultMap = FastMap.newInstance();
monthProductNullResult = FastMap.newInstance();

daysInMonth = monthCal.getActualMaximum(Calendar.DAY_OF_MONTH);
for (int currentDay = 0; currentDay <= daysInMonth; currentDay++) {
    currentDayCal = Calendar.getInstance();
    currentDayCal.setTimeInMillis(monthCal.getTimeInMillis());
    currentDayCal.set(Calendar.DAY_OF_MONTH, currentDay);
    currentDayBegin = new java.sql.Timestamp(currentDayCal.getTimeInMillis());
    currentDayCal.add(Calendar.DAY_OF_MONTH, 1);
    nextDayBegin = new java.sql.Timestamp(currentDayCal.getTimeInMillis());

    findOpts = new EntityFindOptions(true, EntityFindOptions.TYPE_SCROLL_INSENSITIVE, EntityFindOptions.CONCUR_READ_ONLY, true);

    // do the product find
    productAndExprs = FastList.newInstance();
    productAndExprs.addAll(baseProductAndExprs);
    if (productIdSet) productAndExprs.add(EntityCondition.makeCondition("productId", EntityOperator.IN, productIdSet));
    productAndExprs.add(EntityCondition.makeCondition("invoiceDate", EntityOperator.GREATER_THAN_EQUAL_TO, currentDayBegin));
    productAndExprs.add(EntityCondition.makeCondition("invoiceDate", EntityOperator.LESS_THAN, nextDayBegin));

    productResultListIterator = delegator.find("InvoiceItemProductSummary", EntityCondition.makeCondition(productAndExprs, EntityOperator.AND), null, productFieldsToSelect, null, findOpts);
    productResultMap = FastMap.newInstance();
    while ((productResult = productResultListIterator.next())) {
        productResultMap[productResult.productId] = productResult;
        monthProductResult = UtilMisc.getMapFromMap(monthProductResultMap, productResult.productId);
        UtilMisc.addToBigDecimalInMap(monthProductResult, "quantityTotal", productResult.getBigDecimal("quantityTotal"));
        UtilMisc.addToBigDecimalInMap(monthProductResult, "amountTotal", productResult.getBigDecimal("amountTotal"));
    }
    productResultListIterator.close();
    productResultMapByDayList.add(productResultMap);

    // do the category find
    categoryAndExprs = FastList.newInstance();
    categoryAndExprs.addAll(baseCategoryAndExprs);
    categoryAndExprs.add(EntityCondition.makeCondition("invoiceDate", EntityOperator.GREATER_THAN_EQUAL_TO, currentDayBegin));
    categoryAndExprs.add(EntityCondition.makeCondition("invoiceDate", EntityOperator.LESS_THAN, nextDayBegin));

    categoryResultListIterator = delegator.find("InvoiceItemCategorySummary", EntityCondition.makeCondition(categoryAndExprs, EntityOperator.AND), null, categoryFieldsToSelect, null, findOpts);
    categoryResultMap = FastMap.newInstance();
    while ((categoryResult = categoryResultListIterator.next())) {
        categoryResultMap[categoryResult.productCategoryId] = categoryResult;
        monthCategoryResult = UtilMisc.getMapFromMap(monthCategoryResultMap, categoryResult.productCategoryId);
        UtilMisc.addToBigDecimalInMap(monthCategoryResult, "quantityTotal", categoryResult.getBigDecimal("quantityTotal"));
        UtilMisc.addToBigDecimalInMap(monthCategoryResult, "amountTotal", categoryResult.getBigDecimal("amountTotal"));
    }
    categoryResultListIterator.close();
    categoryResultMapByDayList.add(categoryResultMap);

    // do a find for InvoiceItem with a null productId
    productNullAndExprs = FastList.newInstance();
    productNullAndExprs.addAll(baseProductAndExprs);
    productNullAndExprs.add(EntityCondition.makeCondition("productId", EntityOperator.EQUALS, null));
    productNullAndExprs.add(EntityCondition.makeCondition("invoiceDate", EntityOperator.GREATER_THAN_EQUAL_TO, currentDayBegin));
    productNullAndExprs.add(EntityCondition.makeCondition("invoiceDate", EntityOperator.LESS_THAN, nextDayBegin));
    productNullResultListIterator = delegator.find("InvoiceItemProductSummary", EntityCondition.makeCondition(productNullAndExprs, EntityOperator.AND), null, productFieldsToSelect, null, findOpts);
    // should just be 1 result
    productNullResult = productNullResultListIterator.next();
    productNullResultListIterator.close();
    if (productNullResult) {
        productNullResultByDayList.add(productNullResult);
        UtilMisc.addToBigDecimalInMap(monthProductNullResult, "quantityTotal", productNullResult.getBigDecimal("quantityTotal"));
        UtilMisc.addToBigDecimalInMap(monthProductNullResult, "amountTotal", productNullResult.getBigDecimal("amountTotal"));
    } else {
        // no result, add an empty Map place holder
        productNullResultByDayList.add(FastMap.newInstance());
    }
}

context.productResultMapByDayList = productResultMapByDayList;
context.productNullResultByDayList = productNullResultMapByDayList;
context.categoryResultMapByDayList = categoryResultMapByDayList;

context.monthProductResultMap = monthProductResultMap;
context.monthCategoryResultMap = monthCategoryResultMap;
context.monthProductNullResult = monthProductNullResult;

context.productCategoryList = productCategoryList;
context.productList = productList;
