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
package org.apache.ofbiz.product.facility.facility

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

import org.apache.ofbiz.base.util.UtilDateTime
import org.apache.ofbiz.entity.GenericEntityException
import org.apache.ofbiz.entity.condition.EntityCondition
import org.apache.ofbiz.entity.condition.EntityOperator
import org.apache.ofbiz.entity.model.DynamicViewEntity
import org.apache.ofbiz.entity.model.ModelKeyMap
import org.apache.ofbiz.entity.model.ModelViewEntity.ComplexAlias
import org.apache.ofbiz.entity.model.ModelViewEntity.ComplexAliasField
import org.apache.ofbiz.entity.transaction.TransactionUtil
import org.apache.ofbiz.entity.util.EntityListIterator
import org.apache.ofbiz.product.inventory.InventoryWorker

action = request.getParameter('action')

searchParameterString = 'action=Y&facilityId=' + facilityId

offsetQOH = -1
offsetATP = -1
hasOffsetQOH = false
hasOffsetATP = false

EntityListIterator prodsEli = null
rows = [] as ArrayList

if (action) {
    // ------------------------------
    prodView = new DynamicViewEntity()
    atpDiffComplexAlias = new ComplexAlias('-')

    conditionMap = [facilityId: facilityId]

    if (offsetQOHQty) {
        try {
            offsetQOH = Integer.parseInt(offsetQOHQty)
            hasOffsetQOH = true
            searchParameterString = searchParameterString + '&offsetQOHQty=' + offsetQOH
        } catch (NumberFormatException nfe) {
            logError(nfe, 'Caught an exception : ' + nfe)
            request.setAttribute('_ERROR_MESSAGE', 'An entered value seems non-numeric')
        }
    }
    if (offsetATPQty) {
        try {
            offsetATP = Integer.parseInt(offsetATPQty)
            hasOffsetATP = true
            searchParameterString = searchParameterString + '&offsetATPQty=' + offsetATP
        } catch (NumberFormatException nfe) {
            logError(nfe, 'Caught an exception : ' + nfe)
            request.setAttribute('_ERROR_MESSAGE', 'An entered value seems non-numeric')
        }
    }

    prodView.with {
        addMemberEntity('PRFA', 'ProductFacility')
        addAlias('PRFA', 'productId', null, null, null, Boolean.TRUE, null)
        addAlias('PRFA', 'minimumStock', null, null, null, Boolean.TRUE, null)
        addAlias('PRFA', 'reorderQuantity', null, null, null, Boolean.TRUE, null)
        addAlias('PRFA', 'daysToShip', null, null, null, Boolean.TRUE, null)
        addAlias('PRFA', 'facilityId', null, null, null, Boolean.TRUE, null)

        addMemberEntity('PROD', 'Product')
        addViewLink('PROD', 'PRFA', Boolean.FALSE, ModelKeyMap.makeKeyMapList('productId'))
        addAlias('PROD', 'internalName', null, null, null, Boolean.TRUE, null)
        addAlias('PROD', 'isVirtual', null, null, null, Boolean.TRUE, null)
        addAlias('PROD', 'salesDiscontinuationDate', null, null, null, Boolean.TRUE, null)
    }
    if (productTypeId) {
        prodView.addAlias('PROD', 'productTypeId', null, null, null, Boolean.TRUE, null)
        conditionMap.productTypeId = productTypeId
        searchParameterString = searchParameterString + '&productTypeId=' + productTypeId
    }

    prodView.addMemberEntity('IITE', 'InventoryItem')
    prodView.addViewLink('PRFA', 'IITE', Boolean.FALSE, ModelKeyMap.makeKeyMapList('productId', 'productId', 'facilityId', 'facilityId'))
    prodView.addAlias('IITE', 'totalQuantityOnHandTotal', 'quantityOnHandTotal', null, null, null, 'sum')
    prodView.addAlias('IITE', 'totalAvailableToPromiseTotal', 'availableToPromiseTotal', null, null, null, 'sum')
    qohDiffComplexAlias = new ComplexAlias('-')
    qohDiffComplexAlias.addComplexAliasMember(new ComplexAliasField('IITE', 'quantityOnHandTotal', null, 'sum'))
    qohDiffComplexAlias.addComplexAliasMember(new ComplexAliasField('PRFA', 'minimumStock', null, null))
    prodView.addAlias(null, 'offsetQOHQtyAvailable', null, null, null, null, null, qohDiffComplexAlias)
    atpDiffComplexAlias = new ComplexAlias('-')
    atpDiffComplexAlias.addComplexAliasMember(new ComplexAliasField('IITE', 'availableToPromiseTotal', null, 'sum'))
    atpDiffComplexAlias.addComplexAliasMember(new ComplexAliasField('PRFA', 'minimumStock', null, null))
    prodView.addAlias(null, 'offsetATPQtyAvailable', null, null, null, null, null, atpDiffComplexAlias)

    if (searchInProductCategoryId) {
        prodView.addMemberEntity('PRCA', 'ProductCategoryMember')
        prodView.addViewLink('PRFA', 'PRCA', Boolean.FALSE, ModelKeyMap.makeKeyMapList('productId'))
        prodView.addAlias('PRCA', 'productCategoryId', null, null, null, Boolean.TRUE, null)
        conditionMap.productCategoryId = searchInProductCategoryId
        searchParameterString = searchParameterString + '&searchInProductCategoryId=' + searchInProductCategoryId
    }

    if (productSupplierId) {
        prodView.addMemberEntity('SPPR', 'SupplierProduct')
        prodView.addViewLink('PRFA', 'SPPR', Boolean.FALSE, ModelKeyMap.makeKeyMapList('productId'))
        prodView.addAlias('SPPR', 'partyId', null, null, null, Boolean.TRUE, null)
        conditionMap.partyId = productSupplierId
        searchParameterString = searchParameterString + '&productSupplierId=' + productSupplierId
    }

    // set distinct on so we only get one row per product
    searchCondition = EntityCondition.makeCondition(conditionMap, EntityOperator.AND)
    notVirtualCondition = EntityCondition.makeCondition(EntityCondition.makeCondition('isVirtual', EntityOperator.EQUALS, null),
                                                        EntityOperator.OR,
                                                        EntityCondition.makeCondition('isVirtual', EntityOperator.NOT_EQUAL, 'Y'))

    whereConditionsList = [searchCondition, notVirtualCondition]
    // add the discontinuation date condition
    if (productsSoldThruTimestamp) {
        discontinuationDateCondition = EntityCondition.makeCondition(
               [
                EntityCondition.makeCondition('salesDiscontinuationDate', EntityOperator.EQUALS, null),
                EntityCondition.makeCondition('salesDiscontinuationDate', EntityOperator.GREATER_THAN, productsSoldThruTimestamp)
               ],
               EntityOperator.OR)
        whereConditionsList.add(discontinuationDateCondition)
        searchParameterString = searchParameterString + '&productsSoldThruTimestamp=' + productsSoldThruTimestamp
    }

    // add search on internal name
    if (internalName) {
        whereConditionsList.add(EntityCondition.makeCondition('internalName', EntityOperator.LIKE, '%' + internalName + '%'))
        searchParameterString = searchParameterString + '&internalName=' + internalName
    }

    // add search on productId
    if (productId) {
        whereConditionsList.add(EntityCondition.makeCondition('productId', EntityOperator.LIKE, productId + '%'))
        searchParameterString = searchParameterString + '&productId=' + productId
    }

    orderBy = []
    if (hasOffsetATP) {
        orderBy.add('offsetATPQtyAvailable')
    }
    if (hasOffsetQOH) {
        orderBy.add('offsetQOHQtyAvailable')
    }
    orderBy.add('productId')

    // If the user has specified a number of months over which to sum usage quantities, define the correct timestamp
    checkTime = null
    monthsInPastLimitStr = request.getParameter('monthsInPastLimit')
    if (monthsInPastLimitStr) {
        try {
            monthsInPastLimit = Integer.parseInt(monthsInPastLimitStr)
            cal = UtilDateTime.toCalendar(null)
            cal.add(Calendar.MONTH, 0 - monthsInPastLimit)
            checkTime = UtilDateTime.toTimestamp(cal.getTime())
            searchParameterString += '&monthsInPastLimit=' + monthsInPastLimitStr
        } catch (Exception e) {
            logError(e, 'Caught an exception : ' + e)
            request.setAttribute('_ERROR_MESSAGE', 'An exception occured please check the log')
        }
    }

    if (checkTime) {
        // Construct a dynamic view entity to search against for sales usage quantities
        salesUsageViewEntity = new DynamicViewEntity().with { dve ->
            addMemberEntity('OI', 'OrderItem')
            addMemberEntity('OH', 'OrderHeader')
            addMemberEntity('ItIss', 'ItemIssuance')
            addMemberEntity('InvIt', 'InventoryItem')
            addViewLink('OI', 'OH', false, ModelKeyMap.makeKeyMapList('orderId'))
            addViewLink('OI', 'ItIss', false, ModelKeyMap.makeKeyMapList('orderId', 'orderId', 'orderItemSeqId', 'orderItemSeqId'))
            addViewLink('ItIss', 'InvIt', false, ModelKeyMap.makeKeyMapList('inventoryItemId'))
            addAlias('OI', 'productId')
            addAlias('OH', 'statusId')
            addAlias('OH', 'orderTypeId')
            addAlias('OH', 'orderDate')
            addAlias('ItIss', 'inventoryItemId')
            addAlias('ItIss', 'quantity')
            addAlias('InvIt', 'facilityId')
            return dve
        }

        // Construct a dynamic view entity to search against for production usage quantities
        productionUsageViewEntity = new DynamicViewEntity().with { dve ->
            addMemberEntity('WEIA', 'WorkEffortInventoryAssign')
            addMemberEntity('WE', 'WorkEffort')
            addMemberEntity('II', 'InventoryItem')
            addViewLink('WEIA', 'WE', false, ModelKeyMap.makeKeyMapList('workEffortId'))
            addViewLink('WEIA', 'II', false, ModelKeyMap.makeKeyMapList('inventoryItemId'))
            addAlias('WEIA', 'quantity')
            addAlias('WE', 'actualCompletionDate')
            addAlias('WE', 'workEffortTypeId')
            addAlias('II', 'facilityId')
            addAlias('II', 'productId')
            return dve
        }
    }

    whereCondition = EntityCondition.makeCondition(whereConditionsList, EntityOperator.AND)

    beganTransaction = false
    List prods = null
    try {
        beganTransaction = TransactionUtil.begin()

        // get the indexes for the partial list
        lowIndex = ((viewIndex.intValue() * viewSize.intValue()) + 1)
        highIndex = (viewIndex.intValue() + 1) * viewSize.intValue()
        prodsEli = from(prodView).where(whereCondition).orderBy(orderBy).cursorScrollInsensitive().maxRows(highIndex).distinct().queryIterator()

        // get the partial list for this page
        prods = prodsEli.getPartialList(lowIndex, highIndex)
        prodsIt = prods.iterator()
        while (prodsIt) {
            oneProd = prodsIt.next()
            offsetQOHQtyAvailable = oneProd.getBigDecimal('offsetQOHQtyAvailable')
            offsetATPQtyAvailable = oneProd.getBigDecimal('offsetATPQtyAvailable')
            if (hasOffsetATP) {
                if (offsetATPQtyAvailable && offsetATPQtyAvailable.doubleValue() > offsetATP) {
                    break
                }
            }
            if (hasOffsetQOH) {
                if (offsetQOHQtyAvailable && offsetQOHQtyAvailable.doubleValue() > offsetQOH) {
                    break
                }
            }

            oneInventory = [
                    productId: oneProd.productId,
                    minimumStock: oneProd.getBigDecimal('minimumStock'),
                    reorderQuantity: oneProd.getBigDecimal('reorderQuantity'),
                    daysToShip: oneProd.getString('daysToShip'),
                    totalQuantityOnHand: oneProd.totalQuantityOnHandTotal,
                    totalAvailableToPromise: oneProd.totalAvailableToPromiseTotal,
                    offsetQOHQtyAvailable: offsetQOHQtyAvailable,
                    offsetATPQtyAvailable: offsetATPQtyAvailable,
                    quantityOnOrder: InventoryWorker.getOutstandingPurchasedQuantity(oneProd.productId, delegator)
            ]

            if (checkTime) {
                // Make a query against the sales usage view entity
                salesUsageIt = from(salesUsageViewEntity)
                                    .where(EntityCondition.makeCondition('facilityId', EntityOperator.EQUALS, facilityId),
                                        EntityCondition.makeCondition('productId', EntityOperator.EQUALS, oneProd.productId),
                                        EntityCondition.makeCondition('statusId', EntityOperator.IN,
                                                ['ORDER_COMPLETED', 'ORDER_APPROVED', 'ORDER_HELD']),
                                        EntityCondition.makeCondition('orderTypeId', EntityOperator.EQUALS, 'SALES_ORDER'),
                                        EntityCondition.makeCondition('orderDate', EntityOperator.GREATER_THAN_EQUAL_TO, checkTime))
                                    .queryIterator()

                // Sum the sales usage quantities found
                salesUsageQuantity = 0
                salesUsageIt.each { salesUsageItem ->
                    if (salesUsageItem.quantity) {
                        try {
                            salesUsageQuantity += salesUsageItem.getDouble('quantity').doubleValue()
                        } catch (Exception e) {
                            logError(e, 'Caught an exception : ' + e)
                            request.setAttribute('_ERROR_MESSAGE', 'An exception occured please check the log')
                        }
                    }
                }
                salesUsageIt.close()

                // Make a query against the production usage view entity
                productionUsageIt = from(productionUsageViewEntity)
                                    .where(EntityCondition.makeCondition('facilityId', EntityOperator.EQUALS, facilityId),
                                         EntityCondition.makeCondition('productId', EntityOperator.EQUALS, oneProd.productId),
                                         EntityCondition.makeCondition('workEffortTypeId', EntityOperator.EQUALS, 'PROD_ORDER_TASK'),
                                         EntityCondition.makeCondition('actualCompletionDate', EntityOperator.GREATER_THAN_EQUAL_TO, checkTime))
                                    .queryIterator()

                // Sum the production usage quantities found
                productionUsageQuantity = 0
                productionUsageIt.each { productionUsageItem ->
                    if (productionUsageItem.quantity) {
                        try {
                            productionUsageQuantity += productionUsageItem.getDouble('quantity').doubleValue()
                        } catch (Exception e) {
                            logError(e, 'Caught an exception : ' + e)
                            request.setAttribute('_ERROR_MESSAGE', 'An exception occured please check the log')
                        }
                    }
                }
                productionUsageIt.close()
                oneInventory.usageQuantity = salesUsageQuantity + productionUsageQuantity
            }
            rows.add(oneInventory)
        }
        if (rows.size() < viewSize.intValue()) {
            productListSize = lowIndex + rows.size() - 1
        } else {
            // attempt to get the full size
            if (hasOffsetQOH || hasOffsetATP) {
                rowProcessed = 0
                while (prodsEli.hasNext()) {
                    nextValue = prodsEli.next()
                    offsetQOHQtyAvailable = nextValue.getDouble('offsetQOHQtyAvailable')
                    offsetATPQtyAvailable = nextValue.getDouble('offsetATPQtyAvailable')
                    if (hasOffsetATP) {
                        if (offsetATPQtyAvailable && offsetATPQtyAvailable.doubleValue() > offsetATP) {
                            break
                        }
                    }
                    if (hasOffsetQOH) {
                        if (offsetQOHQtyAvailable && offsetQOHQtyAvailable.doubleValue() > offsetQOH) {
                            break
                        }
                    }
                    rowProcessed++
                }
                productListSize = lowIndex + rows.size() + rowProcessed - 1
            } else {
                productListSize = prodsEli.getResultsSizeAfterPartialList()
            }
        }
        prodsEli.close()
        if (highIndex > productListSize) {
            highIndex = productListSize
        }
        context.overrideListSize = productListSize
        context.highIndex = highIndex
        context.lowIndex = lowIndex
    } catch (GenericEntityException e) {
        errMsg = 'Failure in operation, rolling back transaction'
        logError(e, errMsg)
        try {
            // only rollback the transaction if we started one...
            TransactionUtil.rollback(beganTransaction, errMsg, e)
        } catch (GenericEntityException e2) {
            logError(e2, 'Could not rollback transaction: ' + e2)
        }
        // after rolling back, rethrow the exception
        throw e
    } finally {
        if (prodsEli != null) {
            try {
                prodsEli.close()
            } catch (Exception exc) {
                logError(exc, module)
            }
        }
        // only commit the transaction if we started one... this will throw an exception if it fails
        TransactionUtil.commit(beganTransaction)
    }
}
context.inventoryByProduct = rows
context.searchParameterString = searchParameterString
