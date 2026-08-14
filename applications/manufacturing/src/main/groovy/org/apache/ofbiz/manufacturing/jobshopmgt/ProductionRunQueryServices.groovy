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

import org.apache.ofbiz.base.util.UtilValidate
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.entity.condition.EntityCondition
import org.apache.ofbiz.entity.condition.EntityOperator
import org.apache.ofbiz.entity.util.EntityQuery
import org.apache.ofbiz.entity.util.EntityUtil
import org.apache.ofbiz.manufacturing.api.ManufacturingServiceUtil
import org.apache.ofbiz.party.party.PartyHelper
import org.apache.ofbiz.service.ServiceUtil
import org.apache.ofbiz.ws.rs.util.RestApiUtil
import org.apache.ofbiz.ws.rs.util.RestQueryOptions

Map findProductionRuns() {
    RestQueryOptions queryOptions
    try {
        queryOptions = RestQueryOptions.fromParameters(parameters)
    } catch (IllegalArgumentException e) {
        return ServiceUtil.returnError(e.message)
    }
    Map filters = queryOptions.filters
    List orderBy
    try {
        orderBy = productionRunOrderBy(queryOptions.sort)
    } catch (IllegalArgumentException e) {
        return ServiceUtil.returnError(e.message)
    }

    String productionRunId = filters.productionRunId ?: filters.workEffortId
    List conditions = [
            EntityCondition.makeCondition('workEffortTypeId', 'PROD_ORDER_HEADER'),
            EntityCondition.makeCondition('workEffortGoodStdTypeId', 'PRUN_PROD_DELIV')
    ]
    if (UtilValidate.isNotEmpty(productionRunId)) {
        conditions.add(EntityCondition.makeCondition('workEffortId', productionRunId))
    }
    if (UtilValidate.isNotEmpty(filters.productId)) {
        conditions.add(EntityCondition.makeCondition('productId', filters.productId))
    }
    if (UtilValidate.isNotEmpty(filters.facilityId)) {
        conditions.add(EntityCondition.makeCondition('facilityId', filters.facilityId))
    }
    String currentStatusId = filters.statusId ?: filters.currentStatusId
    if (UtilValidate.isNotEmpty(currentStatusId)) {
        conditions.add(EntityCondition.makeCondition('currentStatusId', currentStatusId))
    }
    if (filters.estimatedStartDateFrom != null) {
        conditions.add(EntityCondition.makeCondition('estimatedStartDate', EntityOperator.GREATER_THAN_EQUAL_TO,
                filters.estimatedStartDateFrom))
    }
    if (filters.estimatedStartDateThru != null) {
        conditions.add(EntityCondition.makeCondition('estimatedStartDate', EntityOperator.LESS_THAN_EQUAL_TO,
                filters.estimatedStartDateThru))
    }

    if (UtilValidate.isNotEmpty(filters.workEffortName)) {
        conditions.add(EntityUtil.upperLikeAny(['workEffortName'], filters.workEffortName))
    }

    String queryText = filters.query
    if (UtilValidate.isNotEmpty(queryText)) {
        List queryConditions = [EntityUtil.upperLikeAny(['workEffortId', 'workEffortName'], queryText)]
        Set matchingProductIds = EntityUtil.searchIds(delegator, 'Product', 'productId',
                ['productId', 'productName', 'internalName'], queryText, 500)
        if (matchingProductIds) {
            queryConditions.add(EntityCondition.makeCondition('productId', EntityOperator.IN, matchingProductIds as List))
        }
        conditions.add(EntityCondition.makeCondition(queryConditions, EntityOperator.OR))
    }

    if (UtilValidate.isNotEmpty(filters.productName)) {
        Set productIds = EntityUtil.searchIds(delegator, 'Product', 'productId',
                ['productId', 'productName', 'internalName'], filters.productName, 500)
        if (!productIds) {
            return success(RestApiUtil.getPagedResult('productionRuns', [], queryOptions, 0L,
                    RestApiUtil.getRelativeRequestPath(binding)))
        }
        conditions.add(EntityCondition.makeCondition('productId', EntityOperator.IN, productIds as List))
    }

    EntityCondition whereCondition = EntityCondition.makeCondition(conditions, EntityOperator.AND)

    EntityQuery productionRunQuery = from('WorkEffortAndGoods').where(whereCondition)
    long totalCount = productionRunQuery.queryCount()
    List productionRunValues = productionRunQuery.orderBy(orderBy)
            .queryPagedList(queryOptions.pageIndex, queryOptions.pageSize).getData()

    // Batch-load display data once, then map each row in memory to avoid N+1 lookups.
    Map lookups = buildProductionRunLookups([], productionRunValues, [], [], [], [])
    List productionRuns = productionRunValues.collect { GenericValue productionRun ->
        productionRunSummaryMap(productionRun, lookups)
    }

    return success(RestApiUtil.getPagedResult('productionRuns', productionRuns, queryOptions, totalCount,
            RestApiUtil.getRelativeRequestPath(binding)))
}

Map getProductionRunDetails() {
    String productionRunId = parameters.productionRunId ?: parameters.workEffortId
    if (!productionRunId) {
        return ServiceUtil.returnError('productionRunId is required.')
    }

    ProductionRun productionRunHelper = new ProductionRun(productionRunId, delegator, dispatcher)
    if (!productionRunHelper.exist()) {
        return ServiceUtil.returnError('Production run not found: ' + productionRunId)
    }

    GenericValue productionRun = productionRunHelper.getGenericValue()
    productionRunId = productionRun.workEffortId
    GenericValue producedProduct = productionRunHelper.getProductProduced()
    BigDecimal productionRunQuantity = productionRunHelper.getQuantity()
    List tasks = productionRunHelper.getProductionRunRoutingTasks() ?: []
    List taskIds = tasks*.workEffortId
    List workEffortIds = ([productionRunId] + taskIds).unique()

    List components = productionRunHelper.getProductionRunComponents() ?: []
    // These related records are queried in batches and then grouped in-memory by parent work effort.
    List parties = workEffortIds ? from('WorkEffortPartyAssignView')
            .where(EntityCondition.makeCondition('workEffortId', EntityOperator.IN, workEffortIds))
            .filterByDate()
            .queryList() : []
    List fixedAssets = taskIds ? from('WorkEffortAndFixedAssetAssign')
            .where(EntityCondition.makeCondition('workEffortId', EntityOperator.IN, taskIds))
            .filterByDate()
            .queryList() : []
    List notes = from('WorkEffortNoteAndData')
            .where('workEffortId', productionRunId)
            .orderBy('-noteDateTime')
            .queryList()
    List issuedQuantities = taskIds ? from('WorkEffortAndInventoryAssign')
            .where(EntityCondition.makeCondition('workEffortId', EntityOperator.IN, taskIds))
            .queryList() : []

    // The response uses cached lookups for names/descriptions instead of repeated entity queries.
    // Example shape:
    // [
    //     products: ['PROD_1001': GenericValue(Product)],
    //     facilities: ['FAC_01': GenericValue(Facility)],
    //     statuses: ['PRUN_CREATED': GenericValue(StatusItem)],
    //     uoms: ['EA': GenericValue(Uom)],
    //     roles: ['WORKER': GenericValue(RoleType)]
    // ]
    Map lookups = buildProductionRunLookups([productionRun], [], tasks, components, parties, fixedAssets, [producedProduct].findAll { it })
    // Sum issued inventory per task/product pair so component rows can calculate remaining quantity cheaply.
    // Example shape:
    // [
    //     'TASK_10::COMP_A': 5,
    //     'TASK_10::COMP_B': 2,
    //     'TASK_20::COMP_A': 1
    // ]
    Map issuedQuantityByTaskProduct = issuedQuantities.groupBy { it.workEffortId + '::' + it.productId }
            .collectEntries { String key, List values -> [(key): values.sum { it.getBigDecimal('quantity') ?: BigDecimal.ZERO }] }
    // These maps let the detail payload attach parties, fixed assets, and component task metadata in one pass.
    Map partiesByWorkEffortId = parties.groupBy { it.workEffortId }
    Map fixedAssetsByWorkEffortId = fixedAssets.groupBy { it.workEffortId }
    Map taskById = tasks.collectEntries { GenericValue task -> [(task.workEffortId): task] }

    Map detail = productionRunHeaderMap(productionRun, producedProduct, productionRunQuantity, lookups)
    detail.parties = parties.collect { GenericValue party -> partyMap(party, lookups) }
    detail.notes = notes.collect { GenericValue note -> noteMap(note) }
    detail.tasks = tasks.collect { GenericValue task ->
        taskMap(task, productionRunId, partiesByWorkEffortId[task.workEffortId] ?: [], fixedAssetsByWorkEffortId[task.workEffortId] ?: [], lookups)
    }
    detail.components = components.collect { GenericValue component ->
        componentMap(component, taskById[component.workEffortId], issuedQuantityByTaskProduct, lookups)
    }

    return success(detail)
}

Map buildProductionRunLookups(List productionRuns, List productionRunGoods, List tasks, List components,
                              List parties, List fixedAssets, List products = []) {
    // Centralize related lookups so list/detail mapping can reuse one batch of reference data.
    // This keeps the service out of N+1 query patterns while still resolving display fields in memory.
    Set productIds = [] as Set
    Set facilityIds = [] as Set
    Set statusIds = [] as Set
    Set uomIds = [] as Set
    Set roleTypeIds = [] as Set

    // Production-run header rows only contribute facility and status data.
    productionRuns.each { GenericValue value ->
        if (UtilValidate.isNotEmpty(value.facilityId)) {
            facilityIds.add(value.facilityId)
        }
        if (UtilValidate.isNotEmpty(value.currentStatusId)) {
            statusIds.add(value.currentStatusId)
        }
    }
    // WorkEffortAndGoods rows carry the produced product id for the list view.
    productionRunGoods.each { GenericValue value ->
        if (UtilValidate.isNotEmpty(value.productId)) {
            productIds.add(value.productId)
        }
        if (UtilValidate.isNotEmpty(value.facilityId)) {
            facilityIds.add(value.facilityId)
        }
        if (UtilValidate.isNotEmpty(value.currentStatusId)) {
            statusIds.add(value.currentStatusId)
        }
    }
    tasks.each { GenericValue task ->
        if (UtilValidate.isNotEmpty(task.facilityId)) {
            facilityIds.add(task.facilityId)
        }
        if (UtilValidate.isNotEmpty(task.currentStatusId)) {
            statusIds.add(task.currentStatusId)
        }
    }
    components.each { GenericValue component ->
        if (UtilValidate.isNotEmpty(component.productId)) {
            productIds.add(component.productId)
        }
        if (UtilValidate.isNotEmpty(component.statusId)) {
            statusIds.add(component.statusId)
        }
    }
    parties.each { GenericValue party ->
        if (UtilValidate.isNotEmpty(party.roleTypeId)) {
            roleTypeIds.add(party.roleTypeId)
        }
        if (UtilValidate.isNotEmpty(party.assignmentStatusId)) {
            statusIds.add(party.assignmentStatusId)
        }
        if (UtilValidate.isNotEmpty(party.facilityId)) {
            facilityIds.add(party.facilityId)
        }
    }
    fixedAssets.each { GenericValue fixedAsset ->
        if (UtilValidate.isNotEmpty(fixedAsset.statusId)) {
            statusIds.add(fixedAsset.statusId)
        }
        if (UtilValidate.isNotEmpty(fixedAsset.availabilityStatusId)) {
            statusIds.add(fixedAsset.availabilityStatusId)
        }
    }
    products.each { GenericValue product ->
        if (UtilValidate.isNotEmpty(product.productId)) {
            productIds.add(product.productId)
        }
    }

    // Once the ids are collected, load each reference entity once and reuse it everywhere below.
    Map productMap = EntityUtil.lookupById(delegator, 'Product', 'productId', productIds)
    productMap.values().each { GenericValue product ->
        if (UtilValidate.isNotEmpty(product.quantityUomId)) {
            uomIds.add(product.quantityUomId)
        }
    }

    return [
            products: productMap,
            facilities: EntityUtil.lookupById(delegator, 'Facility', 'facilityId', facilityIds),
            statuses: EntityUtil.lookupById(delegator, 'StatusItem', 'statusId', statusIds),
            uoms: EntityUtil.lookupById(delegator, 'Uom', 'uomId', uomIds),
            roles: EntityUtil.lookupById(delegator, 'RoleType', 'roleTypeId', roleTypeIds)
    ]
}

List productionRunOrderBy(String sortExpression) {
    return RestApiUtil.resolveOrderBy(sortExpression, [
            estimatedStartDate: 'estimatedStartDate',
            actualStartDate: 'actualStartDate',
            status: 'currentStatusId',
            currentStatusId: 'currentStatusId',
            productId: 'productId',
            facilityId: 'facilityId',
            productionRunId: 'workEffortId',
            workEffortId: 'workEffortId',
            workEffortName: 'workEffortName'
    ], ['-estimatedStartDate', 'workEffortId'])
}

Map productionRunSummaryMap(GenericValue productionRun, Map lookups) {
    // List rows are projected into API-friendly values with names and descriptions resolved server-side.
    GenericValue product = lookups.products[productionRun.productId]
    GenericValue facility = lookups.facilities[productionRun.facilityId]
    GenericValue status = lookups.statuses[productionRun.currentStatusId]
    GenericValue uom = lookups.uoms[product?.quantityUomId]
    return [
            productionRunId: productionRun.workEffortId,
            workEffortId: productionRun.workEffortId,
            productId: productionRun.productId,
            productName: ManufacturingServiceUtil.displayProductName(product),
            facilityId: productionRun.facilityId,
            facilityName: ManufacturingServiceUtil.displayFacilityName(facility),
            statusId: productionRun.currentStatusId,
            statusDescription: ManufacturingServiceUtil.displayStatusDescription(status),
            quantity: productionRun.estimatedQuantity,
            quantityUomId: product?.quantityUomId,
            quantityUomDescription: ManufacturingServiceUtil.displayUomDescription(uom),
            estimatedStartDate: productionRun.estimatedStartDate,
            actualStartDate: productionRun.actualStartDate,
            estimatedCompletionDate: productionRun.estimatedCompletionDate,
            workEffortName: productionRun.workEffortName
    ]
}

Map productionRunHeaderMap(GenericValue productionRun, GenericValue producedProduct, BigDecimal productionRunQuantity, Map lookups) {
    // Detail response starts with the production run header, then appends tasks and components below.
    GenericValue product = producedProduct ?: lookups.products[productionRun.productId]
    GenericValue facility = lookups.facilities[productionRun.facilityId]
    GenericValue status = lookups.statuses[productionRun.currentStatusId]
    GenericValue uom = lookups.uoms[product?.quantityUomId]
    return [
            productionRunId: productionRun.workEffortId,
            workEffortId: productionRun.workEffortId,
            productId: product?.productId,
            productName: ManufacturingServiceUtil.displayProductName(product),
            facilityId: productionRun.facilityId,
            facilityName: ManufacturingServiceUtil.displayFacilityName(facility),
            statusId: productionRun.currentStatusId,
            statusDescription: ManufacturingServiceUtil.displayStatusDescription(status),
            workEffortName: productionRun.workEffortName,
            description: productionRun.description,
            quantity: productionRunQuantity ?: productionRun.quantityToProduce,
            quantityProduced: productionRun.quantityProduced ?: BigDecimal.ZERO,
            quantityRejected: productionRun.quantityRejected ?: BigDecimal.ZERO,
            quantityUomId: product?.quantityUomId,
            quantityUomDescription: ManufacturingServiceUtil.displayUomDescription(uom),
            estimatedStartDate: productionRun.estimatedStartDate,
            estimatedCompletionDate: productionRun.estimatedCompletionDate,
            actualStartDate: productionRun.actualStartDate,
            actualCompletionDate: productionRun.actualCompletionDate
    ]
}

Map taskMap(GenericValue task, String productionRunId, List parties, List fixedAssets, Map lookups) {
    // Task rows carry only task-local data plus the already-grouped assignments.
    GenericValue status = lookups.statuses[task.currentStatusId]
    GenericValue facility = lookups.facilities[task.facilityId]
    return [
            workEffortId: task.workEffortId,
            productionRunId: productionRunId,
            priority: task.priority,
            workEffortName: task.workEffortName,
            description: task.description,
            statusId: task.currentStatusId,
            statusDescription: ManufacturingServiceUtil.displayStatusDescription(status),
            facilityId: task.facilityId,
            facilityName: ManufacturingServiceUtil.displayFacilityName(facility),
            estimatedStartDate: task.estimatedStartDate,
            estimatedCompletionDate: task.estimatedCompletionDate,
            actualStartDate: task.actualStartDate,
            actualCompletionDate: task.actualCompletionDate,
            estimatedSetupMillis: task.estimatedSetupMillis,
            estimatedMilliSeconds: task.estimatedMilliSeconds,
            quantityToProduce: task.quantityToProduce,
            parties: parties.collect { GenericValue party -> partyMap(party, lookups) },
            fixedAssets: fixedAssets.collect { GenericValue fixedAsset -> fixedAssetMap(fixedAsset, lookups) }
    ]
}

Map componentMap(GenericValue component, GenericValue task, Map issuedQuantityByTaskProduct, Map lookups) {
    // Components reuse the grouped issued totals to avoid querying per component line.
    GenericValue product = lookups.products[component.productId]
    GenericValue status = lookups.statuses[component.statusId]
    GenericValue uom = lookups.uoms[product?.quantityUomId]
    BigDecimal requiredQuantity = component.getBigDecimal('estimatedQuantity') ?: BigDecimal.ZERO
    BigDecimal issuedQuantity = issuedQuantityByTaskProduct[component.workEffortId + '::' + component.productId] ?: BigDecimal.ZERO
    return [
            componentProductId: component.productId,
            componentProductName: ManufacturingServiceUtil.displayProductName(product),
            requiredQuantity: requiredQuantity,
            issuedQuantity: issuedQuantity,
            remainingQuantity: requiredQuantity - issuedQuantity,
            quantityUomId: product?.quantityUomId,
            quantityUomDescription: ManufacturingServiceUtil.displayUomDescription(uom),
            statusId: component.statusId,
            statusDescription: ManufacturingServiceUtil.displayStatusDescription(status),
            workEffortId: component.workEffortId,
            taskSequence: task?.priority,
            taskName: task?.workEffortName,
            fromDate: component.fromDate
    ]
}

Map partyMap(GenericValue party, Map lookups) {
    // Party names come from OFBiz's shared party helper so name formatting stays consistent.
    GenericValue role = lookups.roles[party.roleTypeId]
    GenericValue status = lookups.statuses[party.assignmentStatusId]
    GenericValue facility = lookups.facilities[party.facilityId]
    return [
            workEffortId: party.workEffortId,
            partyId: party.partyId,
            partyName: PartyHelper.getPartyName(party),
            roleTypeId: party.roleTypeId,
            roleTypeDescription: ManufacturingServiceUtil.displayRoleTypeDescription(role),
            statusId: party.assignmentStatusId,
            statusDescription: ManufacturingServiceUtil.displayStatusDescription(status),
            facilityId: party.facilityId,
            facilityName: ManufacturingServiceUtil.displayFacilityName(facility),
            fromDate: party.fromDate,
            thruDate: party.thruDate
    ]
}

Map fixedAssetMap(GenericValue fixedAsset, Map lookups) {
    // Fixed asset responses expose both the assignment status and the asset's own availability status.
    GenericValue status = lookups.statuses[fixedAsset.statusId]
    GenericValue availabilityStatus = lookups.statuses[fixedAsset.availabilityStatusId]
    return [
            workEffortId: fixedAsset.workEffortId,
            fixedAssetId: fixedAsset.fixedAssetId,
            fixedAssetName: ManufacturingServiceUtil.displayFixedAssetName(fixedAsset),
            statusId: fixedAsset.statusId,
            statusDescription: ManufacturingServiceUtil.displayStatusDescription(status),
            availabilityStatusId: fixedAsset.availabilityStatusId,
            availabilityStatusDescription: ManufacturingServiceUtil.displayStatusDescription(availabilityStatus),
            fromDate: fixedAsset.fromDate,
            thruDate: fixedAsset.thruDate
    ]
}

Map noteMap(GenericValue note) {
    // Notes are passed through as-is; the detail payload just relays the note record fields.
    return [
            noteId: note.noteId,
            noteName: note.noteName,
            noteInfo: note.noteInfo,
            noteParty: note.noteParty,
            noteDateTime: note.noteDateTime,
            internalNote: note.internalNote
    ]
}
