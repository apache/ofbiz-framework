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

import org.apache.ofbiz.base.util.UtilMisc
import org.apache.ofbiz.base.util.UtilValidate
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.entity.condition.EntityCondition
import org.apache.ofbiz.entity.condition.EntityOperator
import org.apache.ofbiz.entity.util.EntityQuery
import org.apache.ofbiz.entity.util.EntityListIterator
import org.apache.ofbiz.party.party.PartyHelper
import org.apache.ofbiz.service.ServiceUtil

Map findProductionRuns() {
    // Keep paging defensive so API callers cannot request pathological windows.
    Integer pageIndexValue = UtilMisc.toIntegerObject(parameters.pageIndex)
    Integer pageSizeValue = UtilMisc.toIntegerObject(parameters.pageSize)
    int pageIndex = pageIndexValue != null ? pageIndexValue : 0
    int pageSize = pageSizeValue != null ? pageSizeValue : 20
    if (pageIndex < 0) {
        pageIndex = 0
    }
    if (pageSize < 1 || pageSize > 100) {
        pageSize = 20
    }

    String productionRunId = parameters.productionRunId ?: parameters.workEffortId
    List conditions = [
            EntityCondition.makeCondition('workEffortTypeId', 'PROD_ORDER_HEADER'),
            EntityCondition.makeCondition('workEffortGoodStdTypeId', 'PRUN_PROD_DELIV')
    ]
    if (UtilValidate.isNotEmpty(productionRunId)) {
        conditions.add(EntityCondition.makeCondition('workEffortId', productionRunId))
    }
    if (UtilValidate.isNotEmpty(parameters.productId)) {
        conditions.add(EntityCondition.makeCondition('productId', parameters.productId))
    }
    if (UtilValidate.isNotEmpty(parameters.facilityId)) {
        conditions.add(EntityCondition.makeCondition('facilityId', parameters.facilityId))
    }
    String currentStatusId = parameters.statusId ?: parameters.currentStatusId
    if (UtilValidate.isNotEmpty(currentStatusId)) {
        conditions.add(EntityCondition.makeCondition('currentStatusId', currentStatusId))
    }

    if (UtilValidate.isNotEmpty(parameters.workEffortName)) {
        conditions.add(EntityCondition.makeCondition('workEffortName', EntityOperator.LIKE, '%' + parameters.workEffortName + '%'))
    }

    String queryText = parameters.query
    if (UtilValidate.isNotEmpty(queryText)) {
        String likeQuery = '%' + queryText.trim() + '%'
        List queryConditions = [
                EntityCondition.makeCondition('workEffortId', EntityOperator.LIKE, likeQuery),
                EntityCondition.makeCondition('workEffortName', EntityOperator.LIKE, likeQuery)
        ]
        conditions.add(EntityCondition.makeCondition(queryConditions, EntityOperator.OR))
    }

    if (UtilValidate.isNotEmpty(parameters.productName)) {
        // Product name search is resolved through Product so we can match both display and internal names.
        List productConditions = [
                EntityCondition.makeCondition('productName', EntityOperator.LIKE, '%' + parameters.productName + '%'),
                EntityCondition.makeCondition('internalName', EntityOperator.LIKE, '%' + parameters.productName + '%')
        ]
        List productIds = from('Product')
                .where(EntityCondition.makeCondition(productConditions, EntityOperator.OR))
                .getFieldList('productId')
                .unique()
        if (!productIds) {
            return success(pageIndex: pageIndex, pageSize: pageSize, totalCount: 0L, hasNext: false, productionRuns: [])
        }
        conditions.add(EntityCondition.makeCondition('productId', EntityOperator.IN, productIds))
    }

    EntityCondition whereCondition = EntityCondition.makeCondition(conditions, EntityOperator.AND)
    List orderBy = resolveProductionRunSort(parameters.sort)
    if (!orderBy) {
        return ServiceUtil.returnError('Unsupported production run sort field: ' + parameters.sort)
    }

    EntityQuery productionRunQuery = from('WorkEffortAndGoods').where(whereCondition)
    long totalCount = productionRunQuery.queryCount()
    int lowIndex = (pageIndex * pageSize) + 1
    int highIndex = (pageIndex + 1) * pageSize
    List productionRunValues = []
    EntityListIterator iterator = null
    try {
        iterator = productionRunQuery
                .orderBy(orderBy)
                .cursorScrollInsensitive()
                .maxRows(highIndex)
                .queryIterator()
        productionRunValues = iterator.getPartialList(lowIndex, pageSize) ?: []
    } finally {
        iterator?.close()
    }

    // Batch-load display data once, then map each row in memory to avoid N+1 lookups.
    Map lookups = buildProductionRunLookups([], productionRunValues, [], [], [], [])
    List productionRuns = productionRunValues.collect { GenericValue productionRun ->
        productionRunSummaryMap(productionRun, lookups)
    }

    return success(pageIndex: pageIndex, pageSize: pageSize, totalCount: totalCount,
            hasNext: ((pageIndex + 1) * pageSize) < totalCount, productionRuns: productionRuns)
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

List resolveProductionRunSort(Object sortValue) {
    Map sortFieldMap = [
            estimatedStartDate: 'estimatedStartDate',
            actualStartDate: 'actualStartDate',
            status: 'currentStatusId',
            productId: 'productId',
            workEffortId: 'workEffortId'
    ]
    if (!sortValue) {
        return ['-estimatedStartDate', 'workEffortId']
    }
    String sort = sortValue
    boolean descending = sort.startsWith('-')
    String sortKey = descending ? sort.substring(1) : sort
    String entityField = sortFieldMap[sortKey]
    return entityField ? [(descending ? '-' : '') + entityField, 'workEffortId'] : []
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
    Map productMap = lookupById('Product', 'productId', productIds)
    productMap.values().each { GenericValue product ->
        if (UtilValidate.isNotEmpty(product.quantityUomId)) {
            uomIds.add(product.quantityUomId)
        }
    }

    return [
            products: productMap,
            facilities: lookupById('Facility', 'facilityId', facilityIds),
            statuses: lookupById('StatusItem', 'statusId', statusIds),
            uoms: lookupById('Uom', 'uomId', uomIds),
            roles: lookupById('RoleType', 'roleTypeId', roleTypeIds)
    ]
}

Map lookupById(String entityName, String fieldName, Collection ids) {
    List idList = new ArrayList(ids.findAll { UtilValidate.isNotEmpty(it) }.unique())
    if (!idList) {
        return [:]
    }
    return from(entityName)
            .where(EntityCondition.makeCondition(fieldName, EntityOperator.IN, idList))
            .cache(true)
            .queryList()
            .collectEntries { GenericValue value -> [(value.getString(fieldName)): value] }
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
            productName: productDisplayName(product),
            facilityId: productionRun.facilityId,
            facilityName: facility?.facilityName,
            statusId: productionRun.currentStatusId,
            statusDescription: status?.description,
            quantity: productionRun.estimatedQuantity,
            quantityUomId: product?.quantityUomId,
            quantityUomDescription: uom?.description,
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
            productName: productDisplayName(product),
            facilityId: productionRun.facilityId,
            facilityName: facility?.facilityName,
            statusId: productionRun.currentStatusId,
            statusDescription: status?.description,
            workEffortName: productionRun.workEffortName,
            description: productionRun.description,
            quantity: productionRunQuantity ?: productionRun.quantityToProduce,
            quantityProduced: productionRun.quantityProduced ?: BigDecimal.ZERO,
            quantityRejected: productionRun.quantityRejected ?: BigDecimal.ZERO,
            quantityUomId: product?.quantityUomId,
            quantityUomDescription: uom?.description,
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
            statusDescription: status?.description,
            facilityId: task.facilityId,
            facilityName: facility?.facilityName,
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
            componentProductName: productDisplayName(product),
            requiredQuantity: requiredQuantity,
            issuedQuantity: issuedQuantity,
            remainingQuantity: requiredQuantity - issuedQuantity,
            quantityUomId: product?.quantityUomId,
            quantityUomDescription: uom?.description,
            statusId: component.statusId,
            statusDescription: status?.description,
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
            roleTypeDescription: role?.description,
            statusId: party.assignmentStatusId,
            statusDescription: status?.description,
            facilityId: party.facilityId,
            facilityName: facility?.facilityName,
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
            fixedAssetName: fixedAsset.fixedAssetName,
            statusId: fixedAsset.statusId,
            statusDescription: status?.description,
            availabilityStatusId: fixedAsset.availabilityStatusId,
            availabilityStatusDescription: availabilityStatus?.description,
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

String productDisplayName(GenericValue product) {
    if (!product) {
        return null
    }
    // Prefer the product display name, then fall back to internal name and finally the id.
    return product.productName ?: product.internalName ?: product.productId
}
