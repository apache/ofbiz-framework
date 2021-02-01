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


import org.apache.ofbiz.base.util.UtilDateTime
import org.apache.ofbiz.base.util.UtilProperties
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.service.ServiceUtil

/**
 * Assign the selected party to the production run or task.
 */
def createProductionRunPartyAssign() {
    parameters.statusId = "PRTYASGN_ASSIGNED"
    if (!parameters.workEffortId) {
        parameters.workEffortId = parameters.productionRunId
    }

    Map serviceResult = run service: "assignPartyToWorkEffort", with: parameters
    if (ServiceUtil.isError(serviceResult)) {
        return serviceResult
    }
    return [successMessage: null, productionRunId: parameters.workEffortId]
}

/**
 *Associate the production run to another production run
 */
def createProductionRunAssoc() {
    Map serviceContext = [workEffortAssocTypeId: "WORK_EFF_PRECEDENCY"]
    if ("WF_PREDECESSOR" == parameters.workFlowSequenceTypeId) {
        serviceContext.workEffortIdFrom = parameters.productionRunIdTo
        serviceContext.workEffortIdTo = parameters.productionRunId
    } else if ("WF_SUCCESSOR" == parameters.workFlowSequenceTypeId) {
        serviceContext.workEffortIdFrom = parameters.productionRunId
        serviceContext.workEffortIdTo = parameters.productionRunIdTo
    }

    Map serviceResult = run service: "createWorkEffortAssoc", with: serviceContext
    if (ServiceUtil.isError(serviceResult)) {
        return serviceResult
    }
    return success()
}

/**
 *Issues the Inventory for a Production Run Task
 */
def issueProductionRunTask() {
    GenericValue workEffort = from("WorkEffort").where(parameters).queryOne()
    parameters.failIfItemsAreNotAvailable = parameters.failIfItemsAreNotAvailable ?: "Y"
    parameters.failIfItemsAreNotOnHand = parameters.failIfItemsAreNotOnHand ?: "Y"
    if (workEffort && "PRUN_CANCELLED" != workEffort.currentStatusId) {
        from("WorkEffortGoodStandard")
                .where(workEffortId: workEffort.workEffortId,
                        statusId: "WEGS_CREATED",
                        workEffortGoodStdTypeId: "PRUNT_PROD_NEEDED")
                .filterByDate()
                .queryList()
                .each { component ->
                    if (component.productId) {
                        Map callSvcMap = component.getAllFields()
                        BigDecimal totalIssuance = 0.0
                        from("WorkEffortAndInventoryAssign")
                                .where(workEffortId: workEffort.workEffortId,
                                        productId: component.productId)
                                .queryList()
                                .each { issuance ->
                                    if (issuance.quantity) {
                                        totalIssuance += issuance.quantity
                                    }
                                }
                        if (totalIssuance != 0) {
                            callSvcMap.quantity = component.estimatedQuantity - totalIssuance
                        }
                        callSvcMap.reserveOrderEnumId = parameters.reserveOrderEnumId
                        callSvcMap.description = "BOM Part"
                        callSvcMap.failIfItemsAreNotAvailable = parameters.failIfItemsAreNotAvailable
                        callSvcMap.failIfItemsAreNotOnHand = parameters.failIfItemsAreNotOnHand

                        Map serviceResult = run service: "issueProductionRunTaskComponent", with: callSvcMap
                        if (ServiceUtil.isError(serviceResult)) {
                            return serviceResult
                        }
                    }
                    logInfo("Issued inventory for workEffortId ${workEffort.workEffortId}")
                }
    }
    return success()
}

/**
 *Issues the Inventory for a Production Run Task Component
 */
def issueProductionRunTaskComponent() {
    GenericValue workEffort = from("WorkEffort").where("workEffortId", parameters.workEffortId).queryOne()
    GenericValue productionRun = from("WorkEffort").where("workEffortId", workEffort.workEffortParentId).queryOne()
    if (['PRUN_CANCELLED', 'PRUN_CLOSED'].contains(productionRun.currentStatusId)) {
        return error(UtilProperties.getMessage('ManufacturingUiLabels',
                'ManufacturingAddProdCompInCompCanStatusError', locale))
    }
    String productId = parameters.productId
    GenericValue workEffortGoodStandard = null
    BigDecimal estimatedQuantity = parameters.quantity ?: 0.0
    if (parameters.fromDate) {
        workEffortGoodStandard = from("WorkEffortGoodStandard")
                .where("workEffortId", parameters.workEffortId,
                        "productId", parameters.productId,
                        "fromDate", parameters.fromDate,
                        "workEffortGoodStdTypeId", "PRUNT_PROD_NEEDED")
                .queryOne()
        productId = workEffortGoodStandard.productId
        if (!parameters.quantity) {
            estimatedQuantity = workEffortGoodStandard.estimatedQuantity
        }

        if (!productId) {
            Map serviceResult = run service: "createWorkEffortGoodStandard", with: [workEffortId: parameters.workEffortId,
                                                                                    productId: parameters.productId,
                                                                                    workEffortGoodStdTypeId: "PRUNT_PROD_NEEDED",
                                                                                    fromDate: parameters.fromDate,
                                                                                    estimatedQuantity: estimatedQuantity,
                                                                                    statusId: "WEGS_CREATED"]
            if (ServiceUtil.isError(serviceResult)) {
                return serviceResult
            }
            // if the task is in completed status we want to make WEIA for the added product as well
            if ("PRUN_COMPLETED" == workEffort.currentStatusId) {
                productId = parameters.productId
            }
        }

    // kind of like the inventory reservation routine, find InventoryItems to issue from,
    // but instead of doing the reservation just create an issuance and an inventory item detail for the change
    if (productId) {
        String orderBy = "+datetimeReceived"
        nowTimestamp = UtilDateTime.nowTimestamp()

            // before we do the find, put together the orderBy list based on which reserveOrderEnumId is specified
            switch (parameters.reserveOrderEnumId) {
                case "INVRO_FIFO_EXP":
                    orderBy = "+expireDate"
                    break
                case "INVRO_LIFO_EXP":
                    orderBy = "-expireDate"
                    break
                case "INVRO_LIFO_REC":
                    orderBy = "-datetimeReceived"
                    break
                default:
                    parameters.reserveOrderEnumId = "INVRO_FIFO_REC"
                    break
            }
            Map lookupFieldMap = [productId: productId,
                                  facilityId: workEffort.facilityId]
            if (parameters.lotId) {
                parameters.failIfItemsAreNotAvailable = "Y"
                lookupFieldMap.lotId = parameters.lotId
            }
            if (parameters.locationSeqId) {
                lookupFieldMap.locationSeqId = parameters.locationSeqId
            }
            List inventoryItemList = from("InventoryItem")
                    .where(lookupFieldMap)
                    .orderBy(orderBy)
                    .queryList()

            if (parameters.locationSeqId && parameters.secondaryLocationSeqId) {
                lookupFieldMap.locationSeqId = parameters.locationSeqId
                inventoryItemList << from("InventoryItem")
                        .where(lookupFieldMap)
                        .orderBy(orderBy)
                        .queryList()
            }

            GenericValue lastNonSerInventoryItem = null
            parameters.quantityNotIssued = estimatedQuantity
            parameters.useReservedItems = "N"

            inventoryItemList.each { inventoryItem ->
                issueProductionRunTaskComponentInline(parameters, inventoryItem, lastNonSerInventoryItem)
            }

            if (parameters.failIfItemsAreNotAvailable != "Y" && parameters.quantityNotIssued != 0) {
                parameters.useReservedItems = "Y"
                inventoryItemList.each { inventoryItem ->
                    if (parameters.quantityNotIssued > 0) {
                        inventoryItem.refresh()
                        issueProductionRunTaskComponentInline(parameters, inventoryItem, lastNonSerInventoryItem)
                    }
                }
            }
            // if quantityNotIssued is not 0, then pull it from the last non-serialized inventory item found, in the quantityNotIssued field
            if (parameters.quantityNotIssued != 0) {
                if (parameters.failIfItemsAreNotAvailable == "Y" || !parameters.failIfItemsAreNotOnHand) {
                    GenericValue product
                    if (productId) {
                        product = from("Product").where(productId: productId).cache().queryOne()
                    }
                    Map paramMap = [productId: productId,
                                    internalName: product ? product.internalName : '',
                                    parameters: parameters]
                    return ServiceUtil.returnError((UtilProperties.getMessage('ManufacturingUiLabels',
                            'ManufacturingMaterialsNotAvailable', paramMap, parameters.locale)))
                }
                if (lastNonSerInventoryItem) {
                    Map serviceResult = run service: "assignInventoryToWorkEffort", with: [workEffortId: parameters.workEffortId,
                                                                                           inventoryItemId: lastNonSerInventoryItem.inventoryItemId,
                                                                                           quantity: parameters.quantityNotIssued]
                    if (ServiceUtil.isError(serviceResult)) {
                        return serviceResult
                    }

                    // subtract from quantityNotIssued from the availableToPromise and quantityOnHand of existing inventory item
                    // instead of updating InventoryItem, add an InventoryItemDetail
                    serviceResult = run service: "createInventoryItemDetail", with: [inventoryItemId: lastNonSerInventoryItem.inventoryItemId,
                                                                                     workEffortId: parameters.workEffortId,
                                                                                     availableToPromiseDiff: -parameters.quantityNotIssued,
                                                                                     quantityOnHandDiff: -parameters.quantityNotIssued,
                                                                                     reasonEnumId: parameters.reasonEnumId,
                                                                                     description: parameters.description]
                    if (ServiceUtil.isError(serviceResult)) {
                        return error(ServiceUtil.getErrorMessage(serviceResult))
                    }
                    serviceResult = run service: "balanceInventoryItems", with: [inventoryItemId: lastNonSerInventoryItem.inventoryItemId]
                    if (ServiceUtil.isError(serviceResult)) {
                        return error(ServiceUtil.getErrorMessage(serviceResult))
                    }
                } else {
                    // no non-ser inv item, create a non-ser InventoryItem with availableToPromise = -quantityNotIssued
                    Map serviceResult = run service: "createInventoryItem", with: [productId: productId,
                                                                                   facilityId: workEffort.facilityId,
                                                                                   inventoryItemTypeId: "NON_SERIAL_INV_ITEM"]
                    if (ServiceUtil.isError(serviceResult)) {
                        return serviceResult
                    }
                    String inventoryItemId = serviceResult.inventoryItemId
                    serviceResult = run service: "assignInventoryToWorkEffort", with: [workEffortId: workEffort.workEffortId,
                                                                                       inventoryItemId: inventoryItemId,
                                                                                       quantity: parameters.quantityNotIssued]
                    if (ServiceUtil.isError(serviceResult)) {
                        return serviceResult
                    }

                    // also create a detail record with the quantities
                    serviceResult = run service: "createInventoryItemDetail",
                            with: [workEffortId: workEffort.workEffortId,
                                   inventoryItemId: inventoryItemId,
                                   availableToPromiseDiff: -parameters.quantityNotIssued,
                                   quantityOnHandDiff: -parameters.quantityNotIssued,
                                   reasonEnumId: parameters.reasonEnumId,
                                   description: parameters.description]
                    if (ServiceUtil.isError(serviceResult)) {
                        return serviceResult
                    }
                    parameters.quantityNotIssued = 0.0
                }
            }
            if (workEffortGoodStandard) {
                BigDecimal totalIssuance = 0.0
                from("WorkEffortAndInventoryAssign")
                        .where("workEffortId", workEffortGoodStandard.workEffortId,
                                "productId", workEffortGoodStandard.productId)
                        .queryList()
                        .each { issuance ->
                            totalIssuance += issuance.quantity
                        }
                if (workEffortGoodStandard.estimatedQuantity <= totalIssuance) {
                    workEffortGoodStandard.statusId = "WEGS_COMPLETED"
                    workEffortGoodStandard.store()
                }
            }
        }
    }
    return success()
}


/**
 *Does a issuance for one InventoryItem, meant to be called in-line
 */
def issueProductionRunTaskComponentInline(Map parameters,
                                          GenericValue inventoryItem,
                                          GenericValue lastNonSerInventoryItem) {

    if (parameters.quantityNotIssued > 0) {
        if ("SERIALIZED_INV_ITEM" == inventoryItem.inventoryItemTypeId &&
                "INV_AVAILABLE" == inventoryItem.statusId) {
            inventoryItem.statusId = "INV_DELIVERED"
            inventoryItem.store()
            Map serviceResult = run service: "assignInventoryToWorkEffort", with: [workEffortId: parameters.workEffortId,
                                                                                   inventoryItemId: parameters.inventoryItemId,
                                                                                   quantity: 1]
            if (ServiceUtil.isError(serviceResult)) {
                return serviceResult
            }
            parameters.quantityNotIssued = parameters.quantityNotIssued - 1
        }
        if ((!inventoryItem.statusId || "INV_AVAILABLE" == inventoryItem.statusId) &&
                "NON_SERIAL_INV_ITEM" == inventoryItem.inventoryItemTypeId) {
            BigDecimal inventoryItemQuantity = "Y" != parameters.useReservedItems ?
                    inventoryItem.availableToPromiseTotal:
                    inventoryItem.quantityOnHandTotal

            // reduce atp on inventoryItem if availableToPromise greater than 0, if not the code at the end of this method will handle it
            if (inventoryItemQuantity && inventoryItemQuantity > 0) {
                parameters.deductAmount = parameters.quantityNotIssued > inventoryItemQuantity ?
                        inventoryItemQuantity:
                        parameters.quantityNotIssued
                Map serviceResult = run service: "assignInventoryToWorkEffort", with: [workEffortId: parameters.workEffortId,
                                                                                   inventoryItemId: inventoryItem.inventoryItemId,
                                                                                   quantity: parameters.deductAmount]
                if (ServiceUtil.isError(serviceResult)) {
                    return serviceResult
                }

                serviceResult = run service: "createInventoryItemDetail", with: [inventoryItemId: inventoryItem.inventoryItemId,
                                                                                 workEffortId: parameters.workEffortId,
                                                                                 availableToPromiseDiff: -parameters.deductAmount,
                                                                                 quantityOnHandDiff: -parameters.deductAmount,
                                                                                 reasonEnumId: parameters.reasonEnumId,
                                                                                 description: parameters.description]
                if (ServiceUtil.isError(serviceResult)) {
                    return serviceResult
                }
                parameters.quantityNotIssued -= parameters.deductAmount
                serviceResult = run service: "balanceInventoryItems", with: [inventoryItemId: inventoryItem.inventoryItemId]
                if (ServiceUtil.isError(serviceResult)) {
                    return serviceResult
                }
                lastNonSerInventoryItem = inventoryItem
            }
        }
    }
    return success()
}

/**
 *Issue one InventoryItem to a WorkEffort
 */
def issueInventoryItemToWorkEffort() {
    GenericValue inventoryItem = parameters.inventoryItem
    BigDecimal quantityIssued = 0.0
    if ("SERIALIZED_INV_ITEM" == inventoryItem.inventoryItemTypeId && inventoryItem.statusId) {
        inventoryItem.statusId = "INV_DELIVERED"
        inventoryItem.store()
        quantityIssued = 1
        Map serviceResult = run service: "assignInventoryToWorkEffort", with: [workEffortId: parameters.workEffortId,
                                                                               inventoryItemId: inventoryItem.inventoryItemId,
                                                                               quantity: quantityIssued]
        if (ServiceUtil.isError(serviceResult)) {
            return serviceResult
        }
    }
    if ("NON_SERIAL_INV_ITEM" == inventoryItem.inventoryItemTypeId &&
            inventoryItem.availableToPromiseTotal &&
            inventoryItem.availableToPromiseTotal > 0) {

        quantityIssued = !parameters.quantity || parameters.quantity > inventoryItem.availableToPromiseTotal ?
                parameters.quantity:
                inventoryItem.availableToPromiseTotal
        Map serviceResult = run service: "assignInventoryToWorkEffort", with: [workEffortId: parameters.workEffortId,
                                                                               inventoryItemId: inventoryItem.inventoryItemId,
                                                                               quantity: quantityIssued]
        if (ServiceUtil.isError(serviceResult)) {
            return serviceResult
        }

        serviceResult = run service: "createInventoryItemDetail", with: [inventoryItemId: inventoryItem.inventoryItemId,
                                                                         workEffortId: parameters.workEffortId,
                                                                         availableToPromiseDiff: - quantityIssued,
                                                                         quantityOnHandDiff: - quantityIssued,
                                                                         reasonEnumId: parameters.reasonEnumId,
                                                                         description: parameters.description]
        if (ServiceUtil.isError(serviceResult)) {
            return serviceResult
        }
        parameters.quantityNotIssued -= parameters.deductAmount
    }
    return [successMessage: null, finishedProductId: inventoryItem.productId, quantityIssued: quantityIssued]
}