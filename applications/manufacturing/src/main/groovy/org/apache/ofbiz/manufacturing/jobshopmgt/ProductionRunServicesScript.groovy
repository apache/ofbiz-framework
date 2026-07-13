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
package org.apache.ofbiz.manufacturing.jobshopmgt

import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.entity.condition.EntityCondition
import org.apache.ofbiz.entity.condition.EntityOperator
import org.apache.ofbiz.service.ServiceUtil

/**
 * Assign the selected party to the production run or task.
 */
Map createProductionRunPartyAssign() {
    parameters.statusId = 'PRTYASGN_ASSIGNED'
    parameters.workEffortId = parameters.workEffortId ?: parameters.productionRunId

    Map serviceResult = runService('assignPartyToWorkEffort', parameters)
    if (ServiceUtil.isError(serviceResult)) {
        return serviceResult
    }
    return [successMessage: null, productionRunId: parameters.workEffortId]
}

/**
 *Associate the production run to another production run
 */
Map createProductionRunAssoc() {
    Map serviceContext = [workEffortAssocTypeId: 'WORK_EFF_PRECEDENCY']
    if (parameters.workFlowSequenceTypeId == 'WF_PREDECESSOR') {
        serviceContext.workEffortIdFrom = parameters.productionRunIdTo
        serviceContext.workEffortIdTo = parameters.productionRunId
    } else if (parameters.workFlowSequenceTypeId == 'WF_SUCCESSOR') {
        serviceContext.workEffortIdFrom = parameters.productionRunId
        serviceContext.workEffortIdTo = parameters.productionRunIdTo
    }

    Map serviceResult = runService('createWorkEffortAssoc', serviceContext)
    if (ServiceUtil.isError(serviceResult)) {
        return serviceResult
    }
    return success()
}

/**
 *Issues the Inventory for a Production Run Task
 */
Map issueProductionRunTask() {
    GenericValue workEffort = from('WorkEffort').where(parameters).queryOne()
    parameters.failIfItemsAreNotAvailable = parameters.failIfItemsAreNotAvailable ?: 'Y'
    parameters.failIfItemsAreNotOnHand = parameters.failIfItemsAreNotOnHand ?: 'Y'
    if (workEffort && 'PRUN_CANCELLED' != workEffort.currentStatusId) {
        List components = from('WorkEffortGoodStandard')
            .where(workEffortId: workEffort.workEffortId,
                    statusId: 'WEGS_CREATED',
                    workEffortGoodStdTypeId: 'PRUNT_PROD_NEEDED')
            .filterByDate()
            .queryList()

        // 1. GLOBAL PRE-CHECK: Prevent partial issuance if any component is missing and strict mode is active
        if (parameters.failIfItemsAreNotAvailable == 'Y' || parameters.failIfItemsAreNotOnHand == 'Y') {
            for (GenericValue component : components) {
                if (!component.productId) {
                    continue
                }
                BigDecimal needed = component.estimatedQuantity ?: 0.0
                BigDecimal totalIssuance = from('WorkEffortAndInventoryAssign')
                        .where(workEffortId: workEffort.workEffortId, productId: component.productId)
                        .queryList()
                        .sum { it.getBigDecimal('quantity') ?: 0.0 } ?: 0.0
                BigDecimal quantityToIssue = needed - totalIssuance

                if (quantityToIssue <= 0) {
                    continue
                }

                // Check availability by summing positive item-level contributions plus our existing reservations
                List inventoryItems = from('InventoryItem')
                        .where(EntityCondition.makeCondition([
                            EntityCondition.makeCondition(productId: component.productId, facilityId: workEffort.facilityId),
                            EntityCondition.makeCondition([
                                EntityCondition.makeCondition('availableToPromiseTotal', EntityOperator.GREATER_THAN, BigDecimal.ZERO),
                                EntityCondition.makeCondition('quantityOnHandTotal', EntityOperator.GREATER_THAN, BigDecimal.ZERO)
                            ], EntityOperator.OR)
                        ], EntityOperator.AND))
                        .queryList()

                // UNIVERSAL AVAILABILITY CHECK:
                // We sum up:
                // 1. "Free" ATP from all items (atp > 0).
                // 2. Reservations already held by THIS task (which are "Pre-paid" ATP).
                // This ensures we don't fail-fast if the only reason ATP is 0 is because we already reserved it.
                BigDecimal totalAvailableToUsAtp = inventoryItems.sum {
                    BigDecimal atp = it.getBigDecimal('availableToPromiseTotal') ?: 0.0
                    return atp > 0 ? atp : 0.0
                } ?: 0.0

                BigDecimal totalAvailableToUsQoh = inventoryItems.sum {
                    BigDecimal qoh = it.getBigDecimal('quantityOnHandTotal') ?: 0.0
                    return qoh > 0 ? qoh : 0.0
                } ?: 0.0

                // Account for our own reservations (which are deducted from Item ATP but still available to us)
                from('WorkEffortInvRes')
                    .where(workEffortId: workEffort.workEffortId, productId: component.productId)
                    .queryList()
                    .each { res ->
                        BigDecimal resQty = res.getBigDecimal('quantity') ?: 0.0
                        BigDecimal notAvail = res.getBigDecimal('quantityNotAvailable') ?: 0.0
                        totalAvailableToUsAtp += (resQty - notAvail)
                    }

                // If Fail-Fast is ON, compare current need against our "Universal Pool" (Free + Promised)
                if ((parameters.failIfItemsAreNotAvailable == 'Y' && quantityToIssue > totalAvailableToUsAtp)
                        || (parameters.failIfItemsAreNotOnHand == 'Y' && quantityToIssue > totalAvailableToUsQoh)) {
                    logError("issueProductionRunTask - Pre-check Fail: Product ${component.productId}. Need: "
                            + "${quantityToIssue}, Effective ATP: ${totalAvailableToUsAtp}, Effective QOH: "
                            + "${totalAvailableToUsQoh}")
                    GenericValue product = from('Product').where(productId: component.productId).cache().queryOne()
                    Map paramMap = [productId: component.productId,
                                    internalName: product ? product.internalName : '',
                                    parameters: parameters]
                    return error(label('ManufacturingUiLabels', 'ManufacturingMaterialsNotAvailable', paramMap))
                }
            }
        }

        // 2. ACTUAL ISSUANCE LOOP
        components.each { component ->
                if (component.productId) {
                    Map callSvcMap = component.getAllFields()
                    BigDecimal totalIssuance = from('WorkEffortAndInventoryAssign')
                            .where(workEffortId: workEffort.workEffortId, productId: component.productId)
                            .queryList()
                            .sum { it.getBigDecimal('quantity') ?: 0.0 } ?: 0.0

                    callSvcMap.quantity = (component.getBigDecimal('estimatedQuantity') ?: 0.0) - totalIssuance

                    if ((callSvcMap.quantity ?: 0.0) > 0) {
                        callSvcMap.reserveOrderEnumId = parameters.reserveOrderEnumId
                        callSvcMap.description = 'BOM Part'
                        callSvcMap.failIfItemsAreNotAvailable = parameters.failIfItemsAreNotAvailable
                        callSvcMap.failIfItemsAreNotOnHand = parameters.failIfItemsAreNotOnHand

                        Map serviceResult = run service: 'issueProductionRunTaskComponent', with: callSvcMap
                        if (ServiceUtil.isError(serviceResult)) {
                            return serviceResult
                        }
                    }
                }
        }
    }
    return success(label('ManufacturingUiLabels', 'ManufacturingProductionRunTaskIssuedSuccessfully',
            [workEffortId: parameters.workEffortId]))
}

/**
 *Issues the Inventory for a Production Run Task Component
 */
Map issueProductionRunTaskComponent() {
    GenericValue workEffort = from('WorkEffort').where(workEffortId: parameters.workEffortId).queryOne()

    // POLICY GATE: Early enforcement
    String facilityId = parameters.facilityId ?: workEffort?.facilityId
    GenericValue facility = from('Facility').where(facilityId: facilityId).queryOne()
    if (parameters.failIfItemsAreNotAvailable == 'N' && facility?.allowInventoryReallocation == 'N') {
        return ServiceUtil.returnError('Inventory Policy forbids inventory reallocation for this facility ['
                + facilityId + ']. Reallocation mode (failIfItemsAreNotAvailable=N) cannot be used.')
    }

    GenericValue productionRun = from('WorkEffort').where(workEffortId: workEffort.workEffortParentId).queryOne()
    if (['PRUN_CANCELLED', 'PRUN_CLOSED'].contains(productionRun.currentStatusId)) {
        return error(label('ManufacturingUiLabels', 'ManufacturingAddProdCompInCompCanStatusError'))
    }
    String productId = parameters.productId
    GenericValue workEffortGoodStandard = null

    if (parameters.fromDate) {
        Map wegsPk = [workEffortId: parameters.workEffortId,
                      productId: productId,
                      fromDate: parameters.fromDate,
                      workEffortGoodStdTypeId: 'PRUNT_PROD_NEEDED']
        workEffortGoodStandard = from('WorkEffortGoodStandard').where(wegsPk).queryOne()
    }

    // Fallback: If fromDate is missing, try finding based on product identity
    workEffortGoodStandard = workEffortGoodStandard ?: from('WorkEffortGoodStandard')
        .where(workEffortId: parameters.workEffortId, productId: productId, workEffortGoodStdTypeId: 'PRUNT_PROD_NEEDED')
        .queryFirst()

    if (workEffortGoodStandard) {
        BigDecimal estimatedQuantity = parameters.quantity ?: workEffortGoodStandard.estimatedQuantity ?: 0.0
        // Initialize the counter for total needed to issue
        parameters.quantityNotIssued = estimatedQuantity
        // Ensure strategy and failure flags are defaulted early for all subsequent calls and reconciliation
        parameters.reserveOrderEnumId = parameters.reserveOrderEnumId ?: 'INVRO_FIFO_REC'
        parameters.failIfItemsAreNotAvailable = parameters.failIfItemsAreNotAvailable ?: 'Y'
        parameters.failIfItemsAreNotOnHand = parameters.failIfItemsAreNotOnHand ?: 'Y'

        // Component Issuance Lifecycle:
        // 1. Consume existing reservations for this component on the task first.
        // 2. If additional quantity is needed, find available InventoryItems based on strategy.
        // 3. Create issuance records and decrease inventory for the total consumed quantity.

        // Step 1: Check for Manual Overrides (Execution Override)
        Map overrideResult = handleManualIssuanceOverride(parameters)
        if (overrideResult && !overrideResult.isEmpty()) {
            return overrideResult
        }

        // Step 2: Default Reservation-First Flow (No Override)
        Map resCond = [workEffortId: parameters.workEffortId, productId: productId]
        if (parameters.inventoryItemId) {
            resCond.inventoryItemId = parameters.inventoryItemId
        }
        List resList = from('WorkEffortInvRes').where(resCond).queryList()

        resList.each { reservation ->
            if (parameters.quantityNotIssued > 0) {
                GenericValue inventoryItem = from('InventoryItem').where(inventoryItemId: reservation.inventoryItemId).queryOne()
                if (inventoryItem) {
                    BigDecimal resQty = reservation.getBigDecimal('quantity') ?: 0.0
                    BigDecimal availableQoh = inventoryItem.getBigDecimal('quantityOnHandTotal') ?: 0.0

                    /*
                     * Example:
                     *  - Component Needs: 60 (quantityNotIssued)
                     *  - Item 10197: Reserved=60 (resQty), Physically On Hand=50 (availableQoh)
                     *  - Calculation:
                     *    1. Initial issueQty = 60 (Min of Need and Reserved)
                     *    2. Capped issueQty = 50 (Limited by availableQoh)
                     *    3. Remaining Need = 10 (60 - 50) -> This allows the loop to pick up the next item in the plan.
                     */
                    BigDecimal issueQty = parameters.quantityNotIssued > resQty ? resQty : parameters.quantityNotIssued
                    if (parameters.failIfItemsAreNotOnHand == 'Y' && issueQty > availableQoh) {
                        logInfo('issueProductionRunTaskComponent - Step 2: Capping reservation issuance for Item: '
                                + "${reservation.inventoryItemId} to available QOH: ${availableQoh} "
                                + "(Reserved: ${resQty})")
                        issueQty = availableQoh
                    }

                    if (issueQty > 0) {
                        Map issueParams = [*:parameters, inventoryItemId: reservation.inventoryItemId,
                                           quantityNotIssued: issueQty, useReservedItems: 'Y']
                        issueProductionRunTaskComponentInline(issueParams, inventoryItem, null)

                        parameters.quantityNotIssued -= issueQty
                    } else if (resQty > 0) {
                        // Self-Healing: If we have a reservation but no QOH, it's a "ghost". Clean it up.
                        logInfo('issueProductionRunTaskComponent - Step 2: Ghost Reservation detected for Item: '
                                + "${reservation.inventoryItemId} (Reserved: ${resQty}, QOH: 0). Releasing now.")
                        run service: 'releaseProductionRunTaskComponent', with: [
                            workEffortId: parameters.workEffortId,
                            inventoryItemId: reservation.inventoryItemId,
                            productId: productId,
                            quantity: resQty
                        ]
                    }
                }
            }
        }

        // Step 3: Pool Stock Issuance.
        if (parameters.quantityNotIssued > 0) {
            String orderBy = ''
            switch (parameters.reserveOrderEnumId) {
                case 'INVRO_COST_DESC':
                    orderBy = '-unitCost'
                    break
                case 'INVRO_COST_ASC':
                    orderBy = '+unitCost'
                    break
                case 'INVRO_FIFO_EXP':
                    orderBy = '+expireDate'
                    break
                case 'INVRO_LIFO_EXP':
                    orderBy = '-expireDate'
                    break
                case 'INVRO_LIFO_REC':
                    orderBy = '-datetimeReceived'
                    break
                case 'INVRO_FIFO_REC':
                    orderBy = '+datetimeReceived'
                    break
                default:
                    orderBy = '+datetimeReceived'
                    break
            }

            List poolCond = [
                EntityCondition.makeCondition(productId: productId, facilityId: workEffort.facilityId),
                EntityCondition.makeCondition([
                    EntityCondition.makeCondition('availableToPromiseTotal', EntityOperator.GREATER_THAN,
                            BigDecimal.ZERO),
                    EntityCondition.makeCondition('quantityOnHandTotal', EntityOperator.GREATER_THAN, BigDecimal.ZERO)
                ], EntityOperator.OR)
            ]

            if (parameters.inventoryItemId) {
                poolCond << EntityCondition.makeCondition(inventoryItemId: parameters.inventoryItemId)
            }

            List inventoryItemList = from('InventoryItem')
                    .where(EntityCondition.makeCondition(poolCond, EntityOperator.AND))
                    .orderBy(orderBy)
                    .queryList()

            // PRE-CHECK: Early Fail-Fast Validation
            // We sum up the "Universal Pool" for this specific product across the whole facility:
            // 1. FREE ATP: Any items with positive availability today.
            // 2. OUR PROMISES: Any inventory explicitly reserved for THIS production task.
            // This ensures that 'Strict' fulfillment mode (failIfItemsAreNotAvailable=Y) correctly recognizes its own reserved stock.
            BigDecimal totalAvailableAtp = inventoryItemList.sum {
                BigDecimal itemAtp = it.getBigDecimal('availableToPromiseTotal') ?: 0.0
                // Look up our reservation on this specific item
                BigDecimal ourRes = from('WorkEffortInvRes')
                        .where(workEffortId: parameters.workEffortId, inventoryItemId: it.inventoryItemId,
                                productId: productId)
                        .queryOne()?.getBigDecimal('quantity') ?: 0.0
                // Logic: Use the free pool (if any) PLUS whatever is promised to us.
                BigDecimal usableFromThis = (itemAtp > 0 ? itemAtp : 0.0) + ourRes
                // PHYSICAL INTEGRITY CAPPING: Do not promise more than we physically have in Normal mode.
                BigDecimal qohValue = it.getBigDecimal('quantityOnHandTotal') ?: 0.0
                if (parameters.failIfItemsAreNotOnHand == 'Y' && usableFromThis > qohValue) {
                    usableFromThis = qohValue
                }
                return usableFromThis
            } ?: 0.0

            BigDecimal totalPhysicalQoh = inventoryItemList.sum {
                (it.getBigDecimal('quantityOnHandTotal') && it.getBigDecimal('quantityOnHandTotal') > 0)
                        ? it.getBigDecimal('quantityOnHandTotal') : 0.0
            } ?: 0.0

            if ((parameters.failIfItemsAreNotAvailable == 'Y' && parameters.quantityNotIssued > totalAvailableAtp) ||
                    (parameters.failIfItemsAreNotOnHand == 'Y' && parameters.quantityNotIssued > totalPhysicalQoh)) {
                GenericValue product = from('Product').where(productId: productId).cache().queryOne()
                Map paramMap = [productId: productId,
                                internalName: product ? product.internalName : '',
                                parameters: parameters]
                String errorMsg = label('ManufacturingUiLabels', 'ManufacturingMaterialsNotAvailable', paramMap)
                logError('issueProductionRunTaskComponent - FAILED: '
                        + "${errorMsg}. Shortfall: ${parameters.quantityNotIssued} for Product [${productId}]")
                return error(errorMsg)
            }

            GenericValue lastNonSerInventoryItem = null
            inventoryItemList.each { inventoryItem ->
                if (parameters.quantityNotIssued > 0) {
                    // Flexible mode (consuming QOH) is only allowed if BOTH the task requests it (failIfItemsAreNotAvailable=N)
                    // AND the facility policy permits it (allowInventoryReallocation=Y).
                    boolean isFlexible = (parameters.failIfItemsAreNotAvailable == 'N' && (facility?.allowInventoryReallocation ?: 'N') == 'Y')
                    BigDecimal itemAtp = inventoryItem.getBigDecimal('availableToPromiseTotal') ?: 0.0
                    GenericValue resLookup = from('WorkEffortInvRes')
                            .where(workEffortId: parameters.workEffortId, inventoryItemId: inventoryItem.inventoryItemId, productId: productId)
                            .queryOne()
                    BigDecimal ourRes = resLookup?.getBigDecimal('quantity') ?: 0.0

                    BigDecimal availableToUs = isFlexible ?
                            (inventoryItem.getBigDecimal('quantityOnHandTotal') ?: 0.0) :
                            ((itemAtp > 0 ? itemAtp : 0.0) + ourRes)

                    // STRICT PHYSICAL CAPPING: If we cannot go negative on hand, cap our availability by the physical QOH.
                    BigDecimal qoh = inventoryItem.getBigDecimal('quantityOnHandTotal') ?: 0.0
                    if (parameters.failIfItemsAreNotOnHand == 'Y' && availableToUs > qoh) {
                        availableToUs = qoh
                    }

                    if (availableToUs > 0) {
                        BigDecimal issueQty = parameters.quantityNotIssued > availableToUs ? availableToUs : parameters.quantityNotIssued

                        Map issueParams = [*:parameters, inventoryItemId: inventoryItem.inventoryItemId,
                                           quantityNotIssued: issueQty, useReservedItems: 'N']
                        Map inlineResult = issueProductionRunTaskComponentInline(issueParams, inventoryItem, null)
                        if (ServiceUtil.isError(inlineResult)) {
                            // Propagate policy violation errors from inline call
                            return inlineResult
                        }
                        parameters.quantityNotIssued -= issueQty
                    }

                    if (inventoryItem.inventoryItemTypeId == 'NON_SERIAL_INV_ITEM') {
                        lastNonSerInventoryItem = inventoryItem
                    }
                }
            }

            // Step 4: Final Fallback - Forced Creation (Last Resort for non-strict modes)
            if (parameters.quantityNotIssued > 0) {
                if (parameters.failIfItemsAreNotAvailable == 'Y' || parameters.failIfItemsAreNotOnHand == 'Y') {
                    GenericValue product = from('Product').where(productId: productId).cache().queryOne()
                    Map paramMap = [productId: productId,
                                internalName: product ? product.internalName : '',
                                parameters: parameters]
                    String errorMsg = label('ManufacturingUiLabels', 'ManufacturingMaterialsNotAvailable', paramMap)
                    logError('issueProductionRunTaskComponent - FAILED: '
                            + "${errorMsg}. Shortfall: ${parameters.quantityNotIssued} for Product [${productId}]")
                    return error(errorMsg)
                }

                // If we got here, it means both fail flags were 'N', allowing negative issuance on the last non-serialized item
                if (lastNonSerInventoryItem) {
                    run service: 'assignInventoryToWorkEffort', with: [workEffortId: parameters.workEffortId,
                                                               inventoryItemId: lastNonSerInventoryItem.inventoryItemId,
                                                                       quantity: parameters.quantityNotIssued]

                    // subtract from quantityNotIssued from the availableToPromise and quantityOnHand of existing inventory item
                    // instead of updating InventoryItem, add an InventoryItemDetail
                    run service: 'createInventoryItemDetail', with: [inventoryItemId: lastNonSerInventoryItem.inventoryItemId,
                                                             workEffortId: parameters.workEffortId,
                                                             availableToPromiseDiff: -parameters.quantityNotIssued,
                                                             quantityOnHandDiff: -parameters.quantityNotIssued,
                                                             reasonEnumId: parameters.reasonEnumId,
                                                                     description: parameters.description]
                    run service: 'balanceInventoryItems', with: [inventoryItemId: lastNonSerInventoryItem.inventoryItemId]
                } else {
                    // no non-ser inv item, create a non-ser InventoryItem with availableToPromise = -quantityNotIssued
                    Map serviceResult = run service: 'createInventoryItem', with: [productId: productId,
                                                                           facilityId: workEffort.facilityId,
                                                                                   inventoryItemTypeId: 'NON_SERIAL_INV_ITEM']
                    String inventoryItemId = serviceResult.inventoryItemId
                    run service: 'assignInventoryToWorkEffort', with: [workEffortId: workEffort.workEffortId,
                                                               inventoryItemId: inventoryItemId,
                                                                       quantity: parameters.quantityNotIssued]

                    // also create a detail record with the quantities
                    run service: 'createInventoryItemDetail',
                            with: [workEffortId: workEffort.workEffortId,
                             inventoryItemId: inventoryItemId,
                             availableToPromiseDiff: -parameters.quantityNotIssued,
                             quantityOnHandDiff: -parameters.quantityNotIssued,
                             reasonEnumId: parameters.reasonEnumId,
                             description: parameters.description]
                    parameters.quantityNotIssued = 0.0
                }
            }
        }

        // Final BOM Status Reconciliation
        BigDecimal totalIssuance = BigDecimal.ZERO
        from('WorkEffortAndInventoryAssign')
                .where(workEffortId: workEffortGoodStandard.workEffortId,
                        productId: workEffortGoodStandard.productId)
                .queryList()
                .each { issuance ->
                    totalIssuance = totalIssuance.add(issuance.getBigDecimal('quantity') ?: BigDecimal.ZERO)
                }
        if (workEffortGoodStandard.getBigDecimal('estimatedQuantity') <= totalIssuance) {
            workEffortGoodStandard.statusId = 'WEGS_COMPLETED'
            workEffortGoodStandard.store()
        }
    } else {
        logInfo('issueProductionRunTaskComponent - SKIPPING ISSUANCE: No WorkEffortGoodStandard '
                + "(BOM Component) record found for Task [${parameters.workEffortId}] Product [${productId}]. "
                + 'Force Issue only works for planned BOM items.')
    }
    return success()
}

/**
 *Does a issuance for one InventoryItem, meant to be called in-line
 */
Map issueProductionRunTaskComponentInline(Map parameters,
                                          GenericValue inventoryItem,
                                          GenericValue lastNonSerInventoryItem) {
    if (parameters.quantityNotIssued > 0) {
        if (inventoryItem.inventoryItemTypeId == 'SERIALIZED_INV_ITEM' &&
                inventoryItem.statusId == 'INV_AVAILABLE') {
            inventoryItem.statusId = 'INV_DELIVERED'
            inventoryItem.store()
            Map serviceResult = run service: 'assignInventoryToWorkEffort', with: [workEffortId: parameters.workEffortId,
                                                                   inventoryItemId: parameters.inventoryItemId,
                                                                                   quantity: 1]
            if (ServiceUtil.isError(serviceResult)) {
                return serviceResult
            }
            parameters.quantityNotIssued = ((BigDecimal) parameters.quantityNotIssued).subtract(BigDecimal.ONE)
        }
        if ((!inventoryItem.statusId || inventoryItem.statusId == 'INV_AVAILABLE') &&
                inventoryItem.inventoryItemTypeId == 'NON_SERIAL_INV_ITEM') {
            // PHYSICAL REFRESH: Ensure we have the latest ledger values in this atomic pass
            inventoryItem.refresh()

            // Smart Availability: We can always use what we've already reserved, plus the pool
            GenericValue ourRes = from('WorkEffortInvRes')
                    .where(workEffortId: parameters.workEffortId, inventoryItemId: inventoryItem.inventoryItemId,
                            productId: parameters.productId ?: inventoryItem.productId)
                    .queryOne()
            BigDecimal ourResQty = ourRes?.getBigDecimal('quantity') ?: 0.0
            BigDecimal ourResQna = ourRes?.getBigDecimal('quantityNotAvailable') ?: 0.0
            BigDecimal ourPhysicalResQty = ourResQty.subtract(ourResQna).max(BigDecimal.ZERO)

            // LEDGER AWARENESS: Find any backordered reservation (unlocalized) for the same task/product.
            // When we issue from an item, we must satisfy these backorders to prevent "Logical Debt" double-counting.
            GenericValue ourBackorder = from('WorkEffortInvRes')
                    .where(workEffortId: parameters.workEffortId, inventoryItemId: null,
                            productId: parameters.productId ?: inventoryItem.productId)
                    .queryFirst()

            // Guard Logic: How much can we safely issue right now?
            // Facility Check: Does the facility allow reallocation?
            GenericValue task = from('WorkEffort').where(workEffortId: parameters.workEffortId).queryOne()
            GenericValue facility = task ? from('Facility').where(facilityId: task.facilityId).queryOne() : null
            String allowReallocation = facility?.allowInventoryReallocation ?: 'N'

            BigDecimal effectiveAvailable = (inventoryItem.getBigDecimal('availableToPromiseTotal') ?: BigDecimal.ZERO) + ourPhysicalResQty
            BigDecimal inventoryItemQuantity

            // POLICY GATE: Traditional (Strict) vs Fluid (Reallocation)
            if (allowReallocation == 'N') {
                // TRADITIONAL: Strictly limited to what is FREE (ATP + Our Res).
                // We cannot reallocate from others even if failIfItemsAreNotAvailable is 'N'.
                inventoryItemQuantity = effectiveAvailable
            } else if (parameters.failIfItemsAreNotAvailable == 'N') {
                // FLEXIBLE: Unlimited issuance (allows going into negative QOH)
                inventoryItemQuantity = BigDecimal.valueOf(9999999)
            } else {
                // FLUID (Reallocation Allowed): Can take anything physical (QOH)
                inventoryItemQuantity = inventoryItem.getBigDecimal('quantityOnHandTotal') ?: BigDecimal.ZERO
            }

            // PHYSICAL INTEGRITY GUARD:
            if (inventoryItemQuantity < parameters.quantityNotIssued) {
                // If we are in a Traditional warehouse, this error is a Policy violation.
                // In a Fluid warehouse, this only happens if failIfItemsAreNotAvailable is 'Y' and we ran out of QOH.
                String errorPrefix = (allowReallocation == 'N') ? 'Inventory Policy forbids inventory reallocation'
                        : 'Inventory Shortfall'
                return ServiceUtil.returnError("${errorPrefix}: Item [${inventoryItem.inventoryItemId}] "
                        + 'has insufficient capacity '
                        + "(Issuable: ${inventoryItemQuantity}). ATP: ${inventoryItem.availableToPromiseTotal}, "
                        + "QOH: ${inventoryItem.quantityOnHandTotal}, Our Physical Reservation: ${ourPhysicalResQty}. "
                        + 'Atomic issuance cannot proceed.')
            }

            BigDecimal deductAmount = parameters.quantityNotIssued > inventoryItemQuantity
                    ? inventoryItemQuantity : (BigDecimal) parameters.quantityNotIssued

            if (deductAmount <= 0) {
                return success()
            }

            Map serviceResult = runService('assignInventoryToWorkEffort', [workEffortId: parameters.workEffortId,
                                                                           inventoryItemId: inventoryItem.inventoryItemId,
                                                                               quantity: deductAmount])
            if (ServiceUtil.isError(serviceResult)) {
                return serviceResult
            }

            // SMART ATP AWARENESS: Determining Ledger Impact
            // Scenario A (Fulfillment): If we hold a reservation for THIS task on THIS item,
            // we've already "paid" for the ATP. Deduction should be 0 to avoid double-counting.
            // Scenario B (Reallocation/Pool): If we have no reservation, we are consuming "Fresh" availability.
            // Deduction should be -FullQty. (Any impacted tasks released during reconciliation will then restore
            // the ledger to 0, preventing stale backorder ATP).

            // Only subtract what hasn't already been promised (reserved) to this work effort.
            BigDecimal atpImpact = (deductAmount - ourPhysicalResQty).max(0.0)
            BigDecimal atpDiff = -atpImpact

            serviceResult = runService('createInventoryItemDetail', [inventoryItemId: inventoryItem.inventoryItemId,
                                                                             workEffortId: parameters.workEffortId,
                                                                             availableToPromiseDiff: atpDiff,
                                                                             quantityOnHandDiff: -deductAmount,
                                                                             reasonEnumId: parameters.reasonEnumId,
                                                                             description: parameters.description])
            if (ServiceUtil.isError(serviceResult)) {
                return serviceResult
            }

            // CONSUME OWN RESERVATION: If we had a reservation for this task, we must reduce it
            // because we just fulfilled it physically.
            if (ourResQty > 0) {
                BigDecimal qnaSatisfied = deductAmount.min(ourResQna)
                BigDecimal remainingQna = ourResQna.subtract(qnaSatisfied).max(BigDecimal.ZERO)
                BigDecimal remainingRes = ourResQty.subtract(deductAmount).max(BigDecimal.ZERO)
                remainingQna = remainingQna.min(remainingRes)

                if (remainingRes <= 0) {
                    run service: 'deleteWorkEffortInvRes', with: [
                        workEffortId: parameters.workEffortId,
                        inventoryItemId: inventoryItem.inventoryItemId,
                        productId: parameters.productId ?: inventoryItem.productId
                    ]
                } else {
                    run service: 'updateWorkEffortInvRes', with: [
                        workEffortId: parameters.workEffortId,
                        inventoryItemId: inventoryItem.inventoryItemId,
                        productId: parameters.productId ?: inventoryItem.productId,
                        quantity: remainingRes,
                        quantityNotAvailable: remainingQna
                    ]
                }
            }

            // SATISFY GLOBAL BACKORDER: If we held a global backorder (null item), reduce it now
            // because we've physically fulfilled the requirement.
            if (ourBackorder) {
                BigDecimal boQty = ourBackorder.getBigDecimal('quantity') ?: 0.0
                BigDecimal boQna = ourBackorder.getBigDecimal('quantityNotAvailable') ?: 0.0
                // How much of this issuance satisfies the backorder?
                BigDecimal satisfactionQty = deductAmount.min(boQty)
                BigDecimal remainingBo = boQty - satisfactionQty

                if (remainingBo <= 0) {
                    run service: 'deleteWorkEffortInvRes', with: [
                        workEffortId: parameters.workEffortId,
                        inventoryItemId: null,
                        productId: parameters.productId ?: inventoryItem.productId
                    ]
                } else {
                    // Update: Reduce the logical debt
                    run service: 'updateWorkEffortInvRes', with: [
                        workEffortId: parameters.workEffortId,
                        inventoryItemId: null,
                        productId: parameters.productId ?: inventoryItem.productId,
                        quantity: remainingBo,
                        quantityNotAvailable: (boQna - satisfactionQty).max(0.0)
                    ]
                }
            }

            // RECONCILIATION HOOK: If we issued stock that was promised to others, reconcile those reservations.
            // Find Facility Policy for the current task
            GenericValue taskWe = from('WorkEffort').where(workEffortId: parameters.workEffortId).queryOne()
            GenericValue taskFac = taskWe ? from('Facility').where(facilityId: taskWe.facilityId).queryOne() : null
            String taskAllowReallocation = taskFac?.allowInventoryReallocation ?: 'N'

            // Robustly resolve strategy (Fallback to FIFO if missing)
            String strategy = parameters.reserveOrderEnumId ?: 'INVRO_FIFO_REC'

            reconcileGlobalReservationsInternal(inventoryItem.inventoryItemId, atpImpact,
                    parameters.workEffortId, strategy, parameters.failIfItemsAreNotAvailable, taskAllowReallocation)

            parameters.quantityNotIssued = ((BigDecimal) parameters.quantityNotIssued).subtract(deductAmount)
            serviceResult = runService('balanceInventoryItems', [inventoryItemId: inventoryItem.inventoryItemId])
            if (ServiceUtil.isError(serviceResult)) {
                return serviceResult
            }
            lastNonSerInventoryItem = inventoryItem
        }
    }
    return success()
}

/**
 *Issue one InventoryItem to a WorkEffort
 */
Map issueInventoryItemToWorkEffort() {
    String workEffortId = parameters.workEffortId
    GenericValue inventoryItem = parameters.inventoryItem
    BigDecimal quantityIssued = 0.0

    if (inventoryItem.inventoryItemTypeId == 'SERIALIZED_INV_ITEM' && inventoryItem.statusId) {
        inventoryItem.statusId = 'INV_DELIVERED'
        inventoryItem.store()
        quantityIssued = 1
        Map serviceResult = run service: 'assignInventoryToWorkEffort', with: [workEffortId: workEffortId,
                                                                               inventoryItemId: inventoryItem.inventoryItemId,
                                                                               quantity: quantityIssued]
        if (ServiceUtil.isError(serviceResult)) {
            return serviceResult
        }
    }

    if (inventoryItem.inventoryItemTypeId == 'NON_SERIAL_INV_ITEM') {
        BigDecimal qoh = inventoryItem.getBigDecimal('quantityOnHandTotal') ?: 0.0
        if (qoh > 0) {
            quantityIssued = (parameters.quantity == null || parameters.quantity > qoh) ? qoh : parameters.quantity

            Map serviceResult = run service: 'assignInventoryToWorkEffort', with: [workEffortId: workEffortId,
                                                                                   inventoryItemId: inventoryItem.inventoryItemId,
                                                                                   quantity: quantityIssued]
            if (ServiceUtil.isError(serviceResult)) {
                return serviceResult
            }

            serviceResult = run service: 'createInventoryItemDetail', with: [inventoryItemId: inventoryItem.inventoryItemId,
                                                                             workEffortId: workEffortId,
                                                                             availableToPromiseDiff: -quantityIssued,
                                                                             quantityOnHandDiff: -quantityIssued,
                                                                             reasonEnumId: parameters.reasonEnumId,
                                                                             description: parameters.description]
            if (ServiceUtil.isError(serviceResult)) {
                return serviceResult
            }
        }
    }
    return [successMessage: null, finishedProductId: inventoryItem.productId, quantityIssued: quantityIssued]
}

/**
 * Atomic execution bridge to bypass chronological loop and force deterministic execution.
 */
Map executeProductionRunTaskComponentAtomic() {
    String inventoryItemId = parameters.inventoryItemId
    GenericValue inventoryItem = from('InventoryItem').where(inventoryItemId: inventoryItemId).queryOne()
    if (!inventoryItem) {
        return ServiceUtil.returnError("InventoryItem ${inventoryItemId} not found for atomic execution.")
    }

    Map inlineParams = [*:parameters, quantityNotIssued: parameters.quantity]
    Map issueResult = issueProductionRunTaskComponentInline(inlineParams, inventoryItem, null)
    if (ServiceUtil.isError(issueResult)) {
        return issueResult
    }

    // Evaluate if this specific product's BOM component is now fully satisfied
    GenericValue workEffortGoodStandard = from('WorkEffortGoodStandard')
            .where(workEffortId: parameters.workEffortId, productId: parameters.productId, workEffortGoodStdTypeId: 'PRUNT_PROD_NEEDED')
            .queryFirst()

    if (workEffortGoodStandard) {
        BigDecimal totalIssuance = BigDecimal.ZERO
        from('WorkEffortAndInventoryAssign')
                .where(workEffortId: parameters.workEffortId, productId: parameters.productId)
                .queryList()
                .each { issuance ->
                    totalIssuance = totalIssuance.add(issuance.getBigDecimal('quantity') ?: BigDecimal.ZERO)
                }

        BigDecimal needed = workEffortGoodStandard.getBigDecimal('estimatedQuantity') ?: BigDecimal.ZERO

        if (totalIssuance >= needed && needed > 0) {
            try {
                workEffortGoodStandard.statusId = 'WEGS_COMPLETED'
                workEffortGoodStandard.store()
            } catch (Exception e) {
                logWarning("executeProductionRunTaskComponentAtomic - Failed to update BOM component status: ${e.message}")
            }
        }
    }

    return success("Successfully performed atomic issuance of ${parameters.quantity} for task ${parameters.workEffortId}")
}

/**
 * Handles manual lot/location override for production run's task issuance.
 */
Map handleManualIssuanceOverride(Map parameters) {
    String lotId = parameters.lotId
    String locationSeqId = parameters.locationSeqId
    String inventoryItemId = parameters.inventoryItemId
    String productId = parameters.productId
    if (!(lotId || locationSeqId || inventoryItemId)) {
        return [:]
    }

    // Find matching inventory items for the override
    Map whereMap = [productId: productId]
    if (inventoryItemId) {
        whereMap.inventoryItemId = inventoryItemId
    }
    if (lotId) {
        whereMap.lotId = lotId
    }
    if (locationSeqId) {
        whereMap.locationSeqId = locationSeqId
    }

    GenericValue inventoryItem = from('InventoryItem').where(whereMap)
        .queryFirst()

    if (!inventoryItem) {
        return error('No inventory item found for the selected Lot/Location.')
    }

    BigDecimal qoh = inventoryItem.getBigDecimal('quantityOnHandTotal') ?: 0.0
    List workEffortReservations = from('WorkEffortInvRes')
        .where(inventoryItemId: inventoryItem.inventoryItemId)
        .queryList()
    BigDecimal totalRes = workEffortReservations
        .findAll { it.workEffortId != parameters.workEffortId }
        .sum { it.getBigDecimal('quantity') ?: 0.0 } ?: 0.0
    BigDecimal ownRes = workEffortReservations
        .findAll { it.workEffortId == parameters.workEffortId && (!productId || it.productId == productId) }
        .sum { it.getBigDecimal('quantity') ?: 0.0 } ?: 0.0
    BigDecimal atp = qoh - totalRes + ownRes

    // 1. Physical Stock Check
    if ((parameters.failIfItemsAreNotOnHand ?: 'Y') == 'Y' && qoh < parameters.quantityNotIssued) {
        logWarning("handleManualIssuanceOverride - FAILED Physical Check: QOH ${qoh} < Needed ${parameters.quantityNotIssued}")
        return error('Insufficient physical inventory in selected Lot/Location.')
    }

    // Find Facility Policy
    GenericValue workEffort = from('WorkEffort').where(workEffortId: parameters.workEffortId).queryOne()
    GenericValue facility = workEffort ? from('Facility').where(facilityId: workEffort.facilityId).queryOne() : null
    String allowReallocation = facility?.allowInventoryReallocation ?: 'N'

    // 2. Promise Check (ATP)
    // We check ATP if we are in Strict mode (failIfItemsAreNotAvailable=Y) OR if the Facility strictly forbids reallocation.
    boolean mustCheckAtp = (parameters.failIfItemsAreNotAvailable == 'Y' || allowReallocation == 'N')
    if (mustCheckAtp && atp < parameters.quantityNotIssued) {
        String msg = (allowReallocation == 'N') ?
                'Manual issuance blocked: This item is promised to other tasks and Facility policy forbids inventory reallocation.' :
                'Inventory in selected Lot is already promised to other tasks.'
        return error(msg)
    }

    // Step 3: Issue the item (Reservations will be handled by issueProductionRunTaskComponentInline)

    // 4. Issue the item
    BigDecimal amountToIssue = parameters.quantityNotIssued
    Map issueParams = [*:parameters, inventoryItemId: inventoryItem.inventoryItemId,
                       quantityNotIssued: amountToIssue, failIfItemsAreNotAvailable: 'N']
    issueProductionRunTaskComponentInline(issueParams, inventoryItem, null)

    List impactedTasks = [] // reconcileGlobalReservationsInternal already called via issueProductionRunTaskComponentInline

    String successMsg = "Successfully issued ${amountToIssue} from ${inventoryItem.inventoryItemId}."
    if (impactedTasks) {
        successMsg += " Note: Stock was reallocated from reservations for Task(s): ${impactedTasks.join(', ')}."
    }
    successMsg += ' Previous reservations for this task were reconciled.'
    return success(successMsg)
}

/**
 * Service wrapper for reconcileGlobalReservations.
 */
Map reconcileGlobalReservationsService() {
    List impactedTaskIds = reconcileGlobalReservationsInternal(
        parameters.inventoryItemId,
        parameters.amountToIssue,
        parameters.currentWorkEffortId,
        parameters.strategy ?: 'INVRO_FIFO_REC',
        parameters.failAvailable ?: 'Y',
        parameters.allowInventoryReallocation ?: 'N'
    )
    return success([impactedTaskIds: impactedTaskIds])
}

/**
 * Reconciles reservations system-wide after an inventory issuance.
 * Returns a list of workEffortIds that were impacted.
 */
List<String> reconcileGlobalReservationsInternal(String inventoryItemId, BigDecimal amountToIssue,
                                         String currentWorkEffortId, String strategy = 'INVRO_FIFO_REC',
                                         String failAvailable = 'Y', String allowInventoryReallocation = 'N') {
    GenericValue inventoryItem = from('InventoryItem').where('inventoryItemId', inventoryItemId).queryOne()
    if (!inventoryItem) {
        return []
    }
    String productId = inventoryItem.productId

    // SELF-HEALING: Purge logical debt before reconciliation
    // Use PRODUCT-SPECIFIC estimated and committed quantities to avoid cross-component leakage.
    BigDecimal currentEstimated = from('WorkEffortGoodStandard')
        .where(workEffortId: currentWorkEffortId, productId: productId, workEffortGoodStdTypeId: 'PRUNT_PROD_NEEDED')
        .queryFirst()?.getBigDecimal('estimatedQuantity') ?: BigDecimal.ZERO

    BigDecimal totalCommitted = from('WorkEffortInvRes')
        .where(workEffortId: currentWorkEffortId, productId: productId)
        .queryList()
        .inject(BigDecimal.ZERO) { sum, r -> sum + (r.getBigDecimal('quantity') ?: BigDecimal.ZERO) }

    if (totalCommitted > currentEstimated) {
        BigDecimal excessCommitment = totalCommitted - currentEstimated
        logWarning('reconcileGlobalReservations - SELF-HEALING: Purging '
                + "${excessCommitment} excess commitment for WE ${currentWorkEffortId}")
        Map releaseResult = runService('releaseProductionRunTaskComponent', [
            workEffortId: currentWorkEffortId,
            inventoryItemId: inventoryItemId,
            productId: productId,
            quantity: excessCommitment,
            appendInventoryItemDetail: false
        ])
        if (ServiceUtil.isError(releaseResult)) {
            logError('reconcileGlobalReservations - FAILED to release own reservation: '
                    + ServiceUtil.getErrorMessage(releaseResult))
        }
    }

    List impactedTaskIds = []
    BigDecimal remainingToDeduct = amountToIssue

    // 0. Policy & Context Discovery

    GenericValue facility = from('Facility').where(facilityId: inventoryItem.facilityId).cache().queryOne()
    String allowReallocation = facility?.allowInventoryReallocation ?: allowInventoryReallocation ?: 'N'
    strategy = strategy ?: 'INVRO_FIFO_REC'

    // 1. Discovery: Find all prospective source reservations (WorkEfforts and Sales Orders)
    List prospectiveReservations = []

    from('WorkEffortInvResAndItem')
        .where(inventoryItemId: inventoryItemId)
        .queryList()
        .each { res ->
            prospectiveReservations << [
                type: 'WorkEffort',
                res: res,
                reservedDatetime: res.reservedDatetime,
                datetimeReceived: res.datetimeReceived,
                expireDate: res.expireDate,
                unitCost: res.unitCost ?: 0
            ]
        }

    from('OrderItemShipGrpInvResAndItem')
        .where(inventoryItemId: inventoryItemId)
        .queryList()
        .each { res ->
            prospectiveReservations << [
                type: 'SalesOrder',
                res: res,
                reservedDatetime: res.reservedDatetime,
                datetimeReceived: res.datetimeReceived,
                expireDate: res.expireDate,
                unitCost: res.unitCost ?: 0
            ]
        }

    // 1.5 Cross-Item Shadow Cleanup: Find reservations for this same product/task on OTHER items.
    if (productId && currentWorkEffortId) {
        from('WorkEffortInvResAndItem')
            .where(EntityCondition.makeCondition(
                EntityCondition.makeCondition('workEffortId', currentWorkEffortId),
                EntityCondition.makeCondition('productId', productId),
                EntityCondition.makeCondition('inventoryItemId', EntityOperator.NOT_EQUAL, inventoryItemId)
            ))
            .queryList()
            .each { res ->
                prospectiveReservations << [
                    type: 'WorkEffort',
                    res: res,
                    reservedDatetime: res.reservedDatetime,
                    datetimeReceived: res.datetimeReceived,
                    expireDate: res.expireDate,
                    unitCost: res.unitCost ?: 0
                ]
            }
    }

    // 2. Strategy Sort
    switch (strategy) {
        case 'INVRO_LIFO_REC':
            prospectiveReservations.sort { a, b -> (b.datetimeReceived?.time ?: 0L) <=> (a.datetimeReceived?.time ?: 0L) }
            break
        case 'INVRO_FIFO_EXP':
            prospectiveReservations.sort { a, b -> (a.expireDate?.time ?: 0L) <=> (b.expireDate?.time ?: 0L) }
            break
        case 'INVRO_LIFO_EXP':
            prospectiveReservations.sort { a, b -> (b.expireDate?.time ?: 0L) <=> (a.expireDate?.time ?: 0L) }
            break
        case 'INVRO_COST_DESC':
            prospectiveReservations.sort { a, b -> (b.unitCost ?: 0) <=> (a.unitCost ?: 0) }
            break
        case 'INVRO_COST_ASC':
            prospectiveReservations.sort { a, b -> (a.unitCost ?: 0) <=> (b.unitCost ?: 0) }
            break
        default:
            prospectiveReservations.sort { a, b -> (a.datetimeReceived?.time ?: 0L) <=> (b.datetimeReceived?.time ?: 0L) }
    }

    // 3. Selection: Self-Consumption vs Smart Reallocation
    List reconciliationList = []
    List ourReservations = prospectiveReservations.findAll { it.type == 'WorkEffort' && it.res.workEffortId == currentWorkEffortId }
    List otherReservations = prospectiveReservations.findAll { !(it.type == 'WorkEffort' && it.res.workEffortId == currentWorkEffortId) }

    reconciliationList.addAll(ourReservations)
    // Only reallocate if BOTH the context allows it (Force/Auto mode) AND the facility policy allows reallocation.
    if (failAvailable != 'Y' && allowReallocation == 'Y') {
        reconciliationList.addAll(otherReservations)
    }

    // 4. Consumption Loop
    for (Map source : reconciliationList) {
        if (remainingToDeduct <= 0) {
            break
        }
        GenericValue res = source.res

        boolean isSelf = (source.type == 'WorkEffort' && res.workEffortId == currentWorkEffortId)

        BigDecimal resQty = res.getBigDecimal('quantity') ?: 0.0
        BigDecimal deductNow = remainingToDeduct.min(resQty)

        if (source.type == 'WorkEffort') {
            if (isSelf) {
                // SATISFACTION: We are issuing against our own reservation.
                Map releaseResult = runService('releaseProductionRunTaskComponent', [
                    workEffortId: res.workEffortId,
                    inventoryItemId: res.inventoryItemId,
                    productId: res.productId,
                    quantity: deductNow,
                    // Only restore ATP if we are releasing from a DIFFERENT item than the one we issued from.
                    appendInventoryItemDetail: (res.inventoryItemId != inventoryItemId)
                ])
                if (ServiceUtil.isError(releaseResult)) {
                    logError("reconcileGlobalReservations - FAILED to release reservation for Task [${res.workEffortId}]: "
                            + ServiceUtil.getErrorMessage(releaseResult))
                    continue
                }
            } else {
                // REALLOCATION: Instead of releasing the reservation, we "demote" it to a backorder.
                Map updateResult = runService('updateWorkEffortInvRes', [
                    workEffortId: res.workEffortId,
                    inventoryItemId: res.inventoryItemId,
                    productId: res.productId,
                    quantity: res.getBigDecimal('quantity'),
                    quantityNotAvailable: (res.getBigDecimal('quantityNotAvailable') ?: BigDecimal.ZERO) + deductNow
                ])
                if (ServiceUtil.isError(updateResult)) {
                    logError("reconcileGlobalReservations - FAILED to demote reservation for Task [${res.workEffortId}]: "
                            + ServiceUtil.getErrorMessage(updateResult))
                    continue
                }
                impactedTaskIds << res.workEffortId.toString()

                // Restore ATP: Demoting a physical reservation to a backorder frees up the physical stock in the ledger.
                Map detailResult = runService('createInventoryItemDetail', [
                    inventoryItemId: inventoryItemId,
                    workEffortId: res.workEffortId,
                    availableToPromiseDiff: deductNow,
                    quantityOnHandDiff: BigDecimal.ZERO,
                    description: "Reconciliation: Reservation demoted to Backorder for Task [${res.workEffortId}]"
                ])
                if (ServiceUtil.isError(detailResult)) {
                    logError("reconcileGlobalReservations - FAILED to restore ATP for Task [${res.workEffortId}]: "
                            + ServiceUtil.getErrorMessage(detailResult))
                }
                logInfo("reconcileGlobalReservations - REALLOCATION: Demoted ${deductNow} from Task [${res.workEffortId}] to Backorder.")
            }
        } else if (source.type == 'SalesOrder') {
            impactedTaskIds << "Order: ${res.orderId}".toString()
            runService('cancelOrderItemShipGrpInvRes', [
                orderId: res.orderId,
                orderItemSeqId: res.orderItemSeqId,
                shipGroupSeqId: res.shipGroupSeqId,
                inventoryItemId: res.inventoryItemId,
                cancelQuantity: deductNow
            ])
            logInfo("reconcileGlobalReservations - Released SalesOrder [${res.orderId}] for ${deductNow} units")
        }
        remainingToDeduct = remainingToDeduct.subtract(deductNow)
    }

    // 5. SANITY GUARD: Multi-Item Over-Commitment Cleanup
    // Loop over ALL impacted tasks (including current one) to ensure they aren't holding excess debt
    Set<String> allImpacted = (impactedTaskIds + [currentWorkEffortId]).findAll { it } as Set

    for (String targetWeId : allImpacted) {
        if (productId && targetWeId) {
            GenericValue wegs = from('WorkEffortGoodStandard')
                .where(workEffortId: targetWeId, productId: productId, workEffortGoodStdTypeId: 'PRUNT_PROD_NEEDED')
                .queryFirst()

            if (wegs) {
                BigDecimal estimatedQty = wegs.getBigDecimal('estimatedQuantity') ?: BigDecimal.ZERO

                // Total Issued
                BigDecimal totalIssued = from('WorkEffortAndInventoryAssign')
                    .where(workEffortId: targetWeId, productId: productId)
                    .queryList()
                    .inject(BigDecimal.ZERO) { sum, it -> sum + (it.getBigDecimal('quantity') ?: BigDecimal.ZERO) }

                // Total Commitment (Physical + Backorder)
                List allResList = from('WorkEffortInvRes')
                    .where(workEffortId: targetWeId, productId: productId)
                    .queryList()

                BigDecimal totalCommitment = allResList.inject(BigDecimal.ZERO) { sum, it -> sum + (it.getBigDecimal('quantity') ?: BigDecimal.ZERO) }

                // POLICY: Definition of "Excess" depends on allowInventoryReallocation (Strictness)
                // Aggressive (Y): Ceiling = Estimate - Issued (Hard life cap)
                // Conservative (N): Ceiling = Estimate (Remaining backorders allowed if Issued > Est)
                BigDecimal commitmentCeiling = (allowInventoryReallocation == 'Y') ? (estimatedQty - totalIssued).max(BigDecimal.ZERO) : estimatedQty

                if (totalCommitment > commitmentCeiling) {
                    BigDecimal excessToPurge = totalCommitment.subtract(commitmentCeiling)
                    if (excessToPurge > 0) {
                        logInfo("reconcileGlobalReservations - [SANITY GUARD]: Task [${targetWeId}] Total Commitment "
                                + "(${totalCommitment}) exceeds Policy Ceiling (${commitmentCeiling}) "
                                + "[Policy: ${allowInventoryReallocation}]. Purging ${excessToPurge} units of logical debt...")

                        // Sort by backorder first (we want to purge debt before physical reservations)
                        allResList.sort { a, b -> (b.quantityNotAvailable ?: 0) <=> (a.quantityNotAvailable ?: 0) }

                        allResList.each { resToClean ->
                            if (excessToPurge > 0) {
                                BigDecimal qna = resToClean.getBigDecimal('quantityNotAvailable') ?: BigDecimal.ZERO
                                if (qna > 0) {
                                    BigDecimal purgeFromQna = excessToPurge.min(qna)
                                    Map releaseResResult = runService('releaseProductionRunTaskComponent', [
                                        workEffortId: targetWeId,
                                        inventoryItemId: resToClean.inventoryItemId,
                                        productId: productId,
                                        quantity: purgeFromQna,
                                        appendInventoryItemDetail: true // Logical debt purge MUST restore ATP
                                    ])
                                    if (ServiceUtil.isError(releaseResResult)) {
                                        logError('reconcileGlobalReservations - [SANITY GUARD] FAILED to purge excess '
                                                + "debt for Task [${targetWeId}]: "
                                                + ServiceUtil.getErrorMessage(releaseResResult))
                                        excessToPurge = BigDecimal.ZERO
                                    } else {
                                        excessToPurge = excessToPurge.subtract(purgeFromQna)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    return allImpacted.toList()
}
