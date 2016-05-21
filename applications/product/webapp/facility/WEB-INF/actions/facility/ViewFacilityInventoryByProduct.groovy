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

import java.util.*
import java.sql.Timestamp
import org.ofbiz.base.util.*
import org.ofbiz.entity.*
import org.ofbiz.entity.condition.*
import org.ofbiz.entity.transaction.*
import org.ofbiz.entity.model.DynamicViewEntity
import org.ofbiz.entity.model.ModelKeyMap
import org.ofbiz.entity.util.EntityFindOptions
import org.ofbiz.product.inventory.*

action = request.getParameter("action");
statusId = request.getParameter("statusId");
searchParameterString = "";
searchParameterString = "action=Y&facilityId=" + facilityId;

offsetQOH = -1;
offsetATP = -1;
hasOffsetQOH = false;
hasOffsetATP = false;

rows = [] as ArrayList;

if (action) {
    // ------------------------------
    prodView = new DynamicViewEntity();
    conditionMap = [facilityId : facilityId];

    if (offsetQOHQty) {
        try {
            offsetQOH = Integer.parseInt(offsetQOHQty);
            hasOffsetQOH = true;
            searchParameterString = searchParameterString + "&offsetQOHQty=" + offsetQOH;
        } catch (NumberFormatException nfe) {
        }
    }
    if (offsetATPQty) {
        try {
            offsetATP = Integer.parseInt(offsetATPQty);
            hasOffsetATP = true;
            searchParameterString = searchParameterString + "&offsetATPQty=" + offsetATP;
        } catch (NumberFormatException nfe) {
        }
    }

    prodView.addMemberEntity("PRFA", "ProductFacility");
    prodView.addAliasAll("PRFA", null, null);

    prodView.addMemberEntity("PROD", "Product");
    prodView.addViewLink("PROD", "PRFA", Boolean.FALSE, ModelKeyMap.makeKeyMapList("productId"));
    prodView.addAlias("PROD", "internalName");
    prodView.addAlias("PROD", "isVirtual");
    prodView.addAlias("PROD", "salesDiscontinuationDate");
    if (productTypeId) {
        prodView.addAlias("PROD", "productTypeId");
        conditionMap.productTypeId = productTypeId;
        searchParameterString = searchParameterString + "&productTypeId=" + productTypeId;
    }
    if (searchInProductCategoryId) {
        prodView.addMemberEntity("PRCA", "ProductCategoryMember");
        prodView.addViewLink("PRFA", "PRCA", Boolean.FALSE, ModelKeyMap.makeKeyMapList("productId"));
        prodView.addAlias("PRCA", "productCategoryId");
        conditionMap.productCategoryId = searchInProductCategoryId;
        searchParameterString = searchParameterString + "&searchInProductCategoryId=" + searchInProductCategoryId;
    }

    if (productSupplierId) {
        prodView.addMemberEntity("SPPR", "SupplierProduct");
        prodView.addViewLink("PRFA", "SPPR", Boolean.FALSE, ModelKeyMap.makeKeyMapList("productId"));
        prodView.addAlias("SPPR", "partyId");
        conditionMap.partyId = productSupplierId;
        searchParameterString = searchParameterString + "&productSupplierId=" + productSupplierId;
    }

    // set distinct on so we only get one row per product
    searchCondition = EntityCondition.makeCondition(conditionMap, EntityOperator.AND);
    notVirtualCondition = EntityCondition.makeCondition(EntityCondition.makeCondition("isVirtual", EntityOperator.EQUALS, null),
                                                         EntityOperator.OR,
                                                         EntityCondition.makeCondition("isVirtual", EntityOperator.NOT_EQUAL, "Y"));

    whereConditionsList = [searchCondition, notVirtualCondition];
    // add the discontinuation date condition
    if (productsSoldThruTimestamp) {
        discontinuationDateCondition = EntityCondition.makeCondition(
               [
                EntityCondition.makeCondition("salesDiscontinuationDate", EntityOperator.EQUALS, null),
                EntityCondition.makeCondition("salesDiscontinuationDate", EntityOperator.GREATER_THAN, productsSoldThruTimestamp)
               ],
               EntityOperator.OR);
        whereConditionsList.add(discontinuationDateCondition);
        searchParameterString = searchParameterString + "&productsSoldThruTimestamp=" + productsSoldThruTimestamp;
    }

    // add search on internal name
    if (internalName) {
        whereConditionsList.add(EntityCondition.makeCondition("internalName", EntityOperator.LIKE, "%" + internalName + "%"));
        searchParameterString = searchParameterString + "&internalName=" + internalName;
    }

    // add search on productId
    if (productId) {
        whereConditionsList.add(EntityCondition.makeCondition("productId", EntityOperator.LIKE, productId + "%"));
        searchParameterString = searchParameterString + "&productId=" + productId;
    }
    whereCondition = EntityCondition.makeCondition(whereConditionsList, EntityOperator.AND);

    beganTransaction = false;
    List prods = null;
    try {
        beganTransaction = TransactionUtil.begin();
        prodsEli = from(prodView).where(whereCondition).orderBy("productId").cursorScrollInsensitive().distinct().queryIterator();
        prods = prodsEli.getCompleteList();
        prodsEli.close();
    } catch (GenericEntityException e) {
        errMsg = "Failure in operation, rolling back transaction";
        Debug.logError(e, errMsg, "ViewFacilityInventoryByProduct");
        try {
            // only rollback the transaction if we started one...
            TransactionUtil.rollback(beganTransaction, errMsg, e);
        } catch (GenericEntityException e2) {
            Debug.logError(e2, "Could not rollback transaction: " + e2.toString(), "ViewFacilityInventoryByProduct");
        }
        // after rolling back, rethrow the exception
        throw e;
    } finally {
        // only commit the transaction if we started one... this will throw an exception if it fails
        TransactionUtil.commit(beganTransaction);
    }

    // If the user has specified a number of months over which to sum usage quantities, define the correct timestamp
    Timestamp checkTime = null;
    monthsInPastLimitStr = request.getParameter("monthsInPastLimit");
    if (monthsInPastLimitStr) {
        try {
            monthsInPastLimit = Integer.parseInt(monthsInPastLimitStr);
            cal = UtilDateTime.toCalendar(null);
            cal.add(Calendar.MONTH, 0 - monthsInPastLimit);
            checkTime = UtilDateTime.toTimestamp(cal.getTime());
            searchParameterString += "&monthsInPastLimit=" + monthsInPastLimitStr;
        } catch (Exception e) {
            // Ignore
        }
    }

    prods.each { oneProd ->
        oneInventory = [:];
        resultMap = [:];
        oneInventory.checkTime = checkTime;
        oneInventory.facilityId = facilityId;
        oneInventory.productId = oneProd.productId;
        minimumStock = oneProd.minimumStock;
        oneInventory.minimumStock = minimumStock;
        oneInventory.reorderQuantity = oneProd.reorderQuantity;
        oneInventory.daysToShip = oneProd.daysToShip;
        
        resultMap =runService('getProductInventoryAndFacilitySummary', [productId : oneProd.productId, minimumStock : minimumStock, facilityId : oneProd.facilityId, checkTime : checkTime, statusId : statusId]);
        if (resultMap) {
            oneInventory.totalAvailableToPromise = resultMap.totalAvailableToPromise;
            oneInventory.totalQuantityOnHand = resultMap.totalQuantityOnHand;
            oneInventory.quantityOnOrder = resultMap.quantityOnOrder;
            oneInventory.offsetQOHQtyAvailable = resultMap.offsetQOHQtyAvailable;
            oneInventory.offsetATPQtyAvailable = resultMap.offsetATPQtyAvailable;
            oneInventory.quantityUom = resultMap.quantityUomId;
            oneInventory.usageQuantity = resultMap.usageQuantity;
            oneInventory.defaultPrice = resultMap.defaultPrice;
            oneInventory.listPrice = resultMap.listPrice;
            oneInventory.wholeSalePrice = resultMap.wholeSalePrice;
            if (offsetQOHQty && offsetATPQty) {
                if ((offsetQOHQty && resultMap.offsetQOHQtyAvailable < offsetQOH) && (offsetATPQty && resultMap.offsetATPQtyAvailable < offsetATP)) {
                    rows.add(oneInventory);
                }
            }else if (offsetQOHQty || offsetATPQty) {
                if ((offsetQOHQty && resultMap.offsetQOHQtyAvailable < offsetQOH) || (offsetATPQty && resultMap.offsetATPQtyAvailable < offsetATP)) {
                    rows.add(oneInventory);
                }
            } else {
                rows.add(oneInventory);
            }
        }
    }
}
context.overrideListSize = rows.size();
context.inventoryByProduct = rows;
context.searchParameterString = searchParameterString;
