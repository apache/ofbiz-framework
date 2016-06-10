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

// This script can be used for testing right now but it should not be used for production because
// it does not work for marketing packages and more importantly, if there is a new product without any
// inventory items, it will not show up on the inventory report even if it had a ProductFacility record.
// These problems need to be addressed before this can be used in production.

// This script counts the inventory in the facility using a complex and
// pretty efficient dynamic view entity.
// However, since the quantities are not computed using the inventory
// services (getInventoryAvailableByFacility and getMktgPackagesAvailable)
// there are some limitations: the virtual inventory of marketing packages
// is not computed; you can use the ViewFacilityInventoryByProduct.groovy if you
// need it (but it is slower than this one).

import org.ofbiz.base.util.Debug
import org.ofbiz.entity.*
import org.ofbiz.entity.condition.*
import org.ofbiz.entity.transaction.*
import org.ofbiz.entity.util.*
import org.ofbiz.entity.model.DynamicViewEntity
import org.ofbiz.entity.model.ModelKeyMap
import org.ofbiz.entity.model.ModelViewEntity.ComplexAlias
import org.ofbiz.entity.model.ModelViewEntity.ComplexAliasField
import org.ofbiz.product.inventory.*

action = request.getParameter("action");

searchParameterString = "action=Y&facilityId=" + facilityId;

offsetQOH = -1;
offsetATP = -1;
hasOffsetQOH = false;
hasOffsetATP = false;

EntityListIterator prodsEli = null;
rows = [] as ArrayList;

if (action) {
    // ------------------------------
    prodView = new DynamicViewEntity();
    atpDiffComplexAlias = new ComplexAlias("-");

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
    prodView.addAlias("PRFA", "productId", null, null, null, Boolean.TRUE, null);
    prodView.addAlias("PRFA", "minimumStock", null, null, null, Boolean.TRUE, null);
    prodView.addAlias("PRFA", "reorderQuantity", null, null, null, Boolean.TRUE, null);
    prodView.addAlias("PRFA", "daysToShip", null, null, null, Boolean.TRUE, null);
    prodView.addAlias("PRFA", "facilityId", null, null, null, Boolean.TRUE, null);

    prodView.addMemberEntity("PROD", "Product");
    prodView.addViewLink("PROD", "PRFA", Boolean.FALSE, ModelKeyMap.makeKeyMapList("productId"));
    prodView.addAlias("PROD", "internalName", null, null, null, Boolean.TRUE, null);
    prodView.addAlias("PROD", "isVirtual", null, null, null, Boolean.TRUE, null);
    prodView.addAlias("PROD", "salesDiscontinuationDate", null, null, null, Boolean.TRUE, null);
    if (productTypeId) {
        prodView.addAlias("PROD", "productTypeId", null, null, null, Boolean.TRUE, null);
        conditionMap.productTypeId = productTypeId;
        searchParameterString = searchParameterString + "&productTypeId=" + productTypeId;
    }

    prodView.addMemberEntity("IITE", "InventoryItem");
    prodView.addViewLink("PRFA", "IITE", Boolean.FALSE, ModelKeyMap.makeKeyMapList("productId", "productId", "facilityId", "facilityId"));
    prodView.addAlias("IITE", "totalQuantityOnHandTotal", "quantityOnHandTotal", null, null, null, "sum");
    prodView.addAlias("IITE", "totalAvailableToPromiseTotal", "availableToPromiseTotal", null, null, null, "sum");
    qohDiffComplexAlias = new ComplexAlias("-");
    qohDiffComplexAlias.addComplexAliasMember(new ComplexAliasField("IITE", "quantityOnHandTotal", null, "sum"));
    qohDiffComplexAlias.addComplexAliasMember(new ComplexAliasField("PRFA", "minimumStock", null, null));
    prodView.addAlias(null, "offsetQOHQtyAvailable", null, null, null, null, null, qohDiffComplexAlias);
    atpDiffComplexAlias = new ComplexAlias("-");
    atpDiffComplexAlias.addComplexAliasMember(new ComplexAliasField("IITE", "availableToPromiseTotal", null, "sum"));
    atpDiffComplexAlias.addComplexAliasMember(new ComplexAliasField("PRFA", "minimumStock", null, null));
    prodView.addAlias(null, "offsetATPQtyAvailable", null, null, null, null, null, atpDiffComplexAlias);

    if (searchInProductCategoryId) {
        prodView.addMemberEntity("PRCA", "ProductCategoryMember");
        prodView.addViewLink("PRFA", "PRCA", Boolean.FALSE, ModelKeyMap.makeKeyMapList("productId"));
        prodView.addAlias("PRCA", "productCategoryId", null, null, null, Boolean.TRUE, null);
        conditionMap.productCategoryId = searchInProductCategoryId;
        searchParameterString = searchParameterString + "&searchInProductCategoryId=" + searchInProductCategoryId;
    }

    if (productSupplierId) {
        prodView.addMemberEntity("SPPR", "SupplierProduct");
        prodView.addViewLink("PRFA", "SPPR", Boolean.FALSE, ModelKeyMap.makeKeyMapList("productId"));
        prodView.addAlias("SPPR", "partyId", null, null, null, Boolean.TRUE, null);
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
                EntityCondition.makeCondition("salesDiscontinuationDate", EntityOperator.GREATER_THAN,productsSoldThruTimestamp)
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

    orderBy = [];
    if (hasOffsetATP) {
        orderBy.add("offsetATPQtyAvailable");
    }
    if (hasOffsetQOH) {
        orderBy.add("offsetQOHQtyAvailable");
    }
    orderBy.add("productId");

    // If the user has specified a number of months over which to sum usage quantities, define the correct timestamp
    checkTime = null;
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

    if (checkTime) {

        // Construct a dynamic view entity to search against for sales usage quantities
        salesUsageViewEntity = new DynamicViewEntity();
        salesUsageViewEntity.addMemberEntity("OI", "OrderItem");
        salesUsageViewEntity.addMemberEntity("OH", "OrderHeader");
        salesUsageViewEntity.addMemberEntity("ItIss", "ItemIssuance");
        salesUsageViewEntity.addMemberEntity("InvIt", "InventoryItem");
        salesUsageViewEntity.addViewLink("OI", "OH", false, ModelKeyMap.makeKeyMapList("orderId"));
        salesUsageViewEntity.addViewLink("OI", "ItIss", false, ModelKeyMap.makeKeyMapList("orderId", "orderId", "orderItemSeqId", "orderItemSeqId"));
        salesUsageViewEntity.addViewLink("ItIss", "InvIt", false, ModelKeyMap.makeKeyMapList("inventoryItemId"));
        salesUsageViewEntity.addAlias("OI", "productId");
        salesUsageViewEntity.addAlias("OH", "statusId");
        salesUsageViewEntity.addAlias("OH", "orderTypeId");
        salesUsageViewEntity.addAlias("OH", "orderDate");
        salesUsageViewEntity.addAlias("ItIss", "inventoryItemId");
        salesUsageViewEntity.addAlias("ItIss", "quantity");
        salesUsageViewEntity.addAlias("InvIt", "facilityId");

        // Construct a dynamic view entity to search against for production usage quantities
        productionUsageViewEntity = new DynamicViewEntity();
        productionUsageViewEntity.addMemberEntity("WEIA", "WorkEffortInventoryAssign");
        productionUsageViewEntity.addMemberEntity("WE", "WorkEffort");
        productionUsageViewEntity.addMemberEntity("II", "InventoryItem");
        productionUsageViewEntity.addViewLink("WEIA", "WE", false, ModelKeyMap.makeKeyMapList("workEffortId"));
        productionUsageViewEntity.addViewLink("WEIA", "II", false, ModelKeyMap.makeKeyMapList("inventoryItemId"));
        productionUsageViewEntity.addAlias("WEIA", "quantity");
        productionUsageViewEntity.addAlias("WE", "actualCompletionDate");
        productionUsageViewEntity.addAlias("WE", "workEffortTypeId");
        productionUsageViewEntity.addAlias("II", "facilityId");
        productionUsageViewEntity.addAlias("II", "productId");
    }

    whereCondition = EntityCondition.makeCondition(whereConditionsList, EntityOperator.AND);

    beganTransaction = false;
    List prods = null;
    try {
        beganTransaction = TransactionUtil.begin();

        // get the indexes for the partial list
        lowIndex = ((viewIndex.intValue() * viewSize.intValue()) + 1);
        highIndex = (viewIndex.intValue() + 1) * viewSize.intValue();
        prodsEli = from(prodView).where(whereCondition).orderBy(orderBy).cursorScrollInsensitive().maxRows(highIndex).distinct().queryIterator();

        // get the partial list for this page
        prods = prodsEli.getPartialList(lowIndex, highIndex);
        prodsIt = prods.iterator();
        while (prodsIt) {
            oneProd = prodsIt.next();
            offsetQOHQtyAvailable = oneProd.getBigDecimal("offsetQOHQtyAvailable");
            offsetATPQtyAvailable = oneProd.getBigDecimal("offsetATPQtyAvailable");
            if (hasOffsetATP) {
                if (offsetATPQtyAvailable && offsetATPQtyAvailable.doubleValue() > offsetATP) {
                    break;
                }
            }
            if (hasOffsetQOH) {
                if (offsetQOHQtyAvailable && offsetQOHQtyAvailable.doubleValue() > offsetQOH) {
                    break;
                }
            }

            oneInventory = [:];
            oneInventory.productId = oneProd.productId;
            oneInventory.minimumStock = oneProd.getBigDecimal("minimumStock");
            oneInventory.reorderQuantity = oneProd.getBigDecimal("reorderQuantity");
            oneInventory.daysToShip = oneProd.getString("daysToShip");
            oneInventory.totalQuantityOnHand = oneProd.totalQuantityOnHandTotal;
            oneInventory.totalAvailableToPromise = oneProd.totalAvailableToPromiseTotal;
            oneInventory.offsetQOHQtyAvailable = offsetQOHQtyAvailable;
            oneInventory.offsetATPQtyAvailable = offsetATPQtyAvailable;
            oneInventory.quantityOnOrder = InventoryWorker.getOutstandingPurchasedQuantity(oneProd.productId, delegator);


            if (checkTime) {

                // Make a query against the sales usage view entity
                salesUsageIt = from(salesUsageViewEntity)
                                    .where(EntityCondition.makeCondition("facilityId", EntityOperator.EQUALS, facilityId),
                                        EntityCondition.makeCondition("productId", EntityOperator.EQUALS, oneProd.productId),
                                        EntityCondition.makeCondition("statusId", EntityOperator.IN, ['ORDER_COMPLETED', 'ORDER_APPROVED', 'ORDER_HELD']),
                                        EntityCondition.makeCondition("orderTypeId", EntityOperator.EQUALS, "SALES_ORDER"),
                                        EntityCondition.makeCondition("orderDate", EntityOperator.GREATER_THAN_EQUAL_TO, checkTime))
                                    .queryIterator();

                // Sum the sales usage quantities found
                salesUsageQuantity = 0;
                salesUsageIt.each { salesUsageItem ->
                    if (salesUsageItem.quantity) {
                        try {
                            salesUsageQuantity += salesUsageItem.getDouble("quantity").doubleValue();
                        } catch (Exception e) {
                            // Ignore
                        }
                    }
                }
                salesUsageIt.close();

                // Make a query against the production usage view entity
                productionUsageIt = from(productionUsageViewEntity)
                                    .where(EntityCondition.makeCondition("facilityId", EntityOperator.EQUALS, facilityId),
                                         EntityCondition.makeCondition("productId", EntityOperator.EQUALS, oneProd.productId),
                                         EntityCondition.makeCondition("workEffortTypeId", EntityOperator.EQUALS, "PROD_ORDER_TASK"),
                                         EntityCondition.makeCondition("actualCompletionDate", EntityOperator.GREATER_THAN_EQUAL_TO, checkTime))
                                    .queryIterator();

                // Sum the production usage quantities found
                productionUsageQuantity = 0;
                productionUsageIt.each { productionUsageItem ->
                    if (productionUsageItem.quantity) {
                        try {
                            productionUsageQuantity += productionUsageItem.getDouble("quantity").doubleValue();
                        } catch (Exception e) {
                            // Ignore
                        }
                    }
                }
                productionUsageIt.close();
                oneInventory.usageQuantity = salesUsageQuantity + productionUsageQuantity;
            }
            rows.add(oneInventory);
        }
        if (rows.size() < viewSize.intValue()) {
            productListSize = lowIndex + rows.size() - 1;
        } else {
            // attempt to get the full size
            if (hasOffsetQOH || hasOffsetATP) {
                rowProcessed = 0;
                while (nextValue = prodsEli.next()) {
                    offsetQOHQtyAvailable = nextValue.getDouble("offsetQOHQtyAvailable");
                    offsetATPQtyAvailable = nextValue.getDouble("offsetATPQtyAvailable");
                    if (hasOffsetATP) {
                        if (offsetATPQtyAvailable && offsetATPQtyAvailable.doubleValue() > offsetATP) {
                            break;
                        }
                    }
                    if (hasOffsetQOH) {
                        if (offsetQOHQtyAvailable && offsetQOHQtyAvailable.doubleValue() > offsetQOH) {
                            break;
                        }
                    }
                    rowProcessed++;
                }
                productListSize = lowIndex + rows.size() + rowProcessed - 1;
            } else {
                productListSize = prodsEli.getResultsSizeAfterPartialList();
            }
        }
        prodsEli.close();
        if (highIndex > productListSize) {
            highIndex = productListSize;
        }
        context.overrideListSize = productListSize;
        context.highIndex = highIndex;
        context.lowIndex = lowIndex;

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
        if (prodsEli != null) {
            try {
                prodsEli.close();
            } catch (Exception exc) {}
        }
        // only commit the transaction if we started one... this will throw an exception if it fails
        TransactionUtil.commit(beganTransaction);
    }
}
context.inventoryByProduct = rows;
context.searchParameterString = searchParameterString;
