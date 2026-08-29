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
package org.apache.ofbiz.manufacturing.api

import java.sql.Timestamp

import org.apache.ofbiz.base.util.UtilDateTime
import org.apache.ofbiz.base.util.UtilValidate
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.entity.condition.EntityCondition
import org.apache.ofbiz.entity.condition.EntityOperator
import org.apache.ofbiz.entity.util.EntityQuery
import org.apache.ofbiz.entity.util.EntityUtil
import org.apache.ofbiz.party.party.PartyHelper
import org.apache.ofbiz.service.ServiceUtil
import org.apache.ofbiz.ws.rs.util.RestApiUtil
import org.apache.ofbiz.ws.rs.util.RestQueryOptions

Map findProductRoutings() {
    RestQueryOptions queryOptions
    try {
        queryOptions = RestQueryOptions.fromParameters(parameters)
    } catch (IllegalArgumentException e) {
        return ServiceUtil.returnError(e.message)
    }
    Map filters = queryOptions.filters
    List orderBy
    try {
        orderBy = routingOrderBy(queryOptions.sort)
    } catch (IllegalArgumentException e) {
        return ServiceUtil.returnError(e.message)
    }
    List conditions = [EntityCondition.makeCondition('workEffortTypeId', 'ROUTING')]
    if (UtilValidate.isNotEmpty(filters.productId)) {
        Set productRoutingIds = routingIdsForProducts([filters.productId] as Set)
        if (!productRoutingIds) {
            return success(RestApiUtil.getPagedResult('routings', [], queryOptions, 0L,
                    RestApiUtil.getRelativeRequestPath(binding)))
        }
        conditions.add(EntityCondition.makeCondition('workEffortId', EntityOperator.IN, productRoutingIds as List))
    }
    if (UtilValidate.isNotEmpty(filters.routingId ?: filters.workEffortId)) {
        conditions.add(EntityCondition.makeCondition('workEffortId', filters.routingId ?: filters.workEffortId))
    }
    if (UtilValidate.isNotEmpty(filters.query)) {
        Set matchingRoutingIds = EntityUtil.searchIds(delegator, 'WorkEffort', 'workEffortId',
                ['workEffortId', 'workEffortName', 'description'], filters.query,
                [EntityCondition.makeCondition('workEffortTypeId', 'ROUTING')], 500)
        Set matchingProductIds = EntityUtil.searchIds(delegator, 'Product', 'productId',
                ['productId', 'productName', 'internalName'], filters.query, 500)
        if (matchingProductIds) {
            matchingRoutingIds.addAll(routingIdsForProducts(matchingProductIds))
        }
        if (!matchingRoutingIds) {
            return success(RestApiUtil.getPagedResult('routings', [], queryOptions, 0L,
                    RestApiUtil.getRelativeRequestPath(binding)))
        }
        conditions.add(EntityCondition.makeCondition('workEffortId', EntityOperator.IN, matchingRoutingIds as List))
    }
    if (UtilValidate.isNotEmpty(filters.workEffortName)) {
        conditions.add(EntityCondition.makeCondition('workEffortName', EntityOperator.LIKE, '%' + filters.workEffortName + '%'))
    }
    if (UtilValidate.isNotEmpty(filters.currentStatusId)) {
        conditions.add(EntityCondition.makeCondition('currentStatusId', filters.currentStatusId))
    }
    EntityCondition whereCondition = EntityCondition.makeCondition(conditions, EntityOperator.AND)
    EntityQuery query = from('WorkEffort').where(whereCondition)
    long totalCount = query.queryCount()
    List rows = query.orderBy(orderBy).queryPagedList(queryOptions.pageIndex, queryOptions.pageSize).getData()
    Set routingIds = rows*.workEffortId.findAll { it } as Set
    Map taskCounts = routingTaskCountByRouting(routingIds)
    Map productCounts = routingProductCountByRouting(routingIds)
    Map statuses = EntityUtil.lookupById(delegator, 'StatusItem', 'statusId',
            rows*.currentStatusId.findAll { it } as Set)

    List routingList = rows.collect { GenericValue routing ->
        [
                routingId: routing.workEffortId,
                workEffortId: routing.workEffortId,
                workEffortName: routing.workEffortName,
                description: routing.description,
                quantityToProduce: routing.quantityToProduce,
                currentStatusId: routing.currentStatusId,
                currentStatusDescription: statuses[routing.currentStatusId]?.description,
                productCount: productCounts[routing.workEffortId] ?: 0,
                taskCount: taskCounts[routing.workEffortId] ?: 0
        ]
    }
    return success(RestApiUtil.getPagedResult('routings', routingList, queryOptions, totalCount,
            RestApiUtil.getRelativeRequestPath(binding)))
}

Map getRoutingDetails() {
    String routingId = parameters.routingId ?: parameters.workEffortId
    if (!routingId) {
        return ServiceUtil.returnError('routingId is required.')
    }
    GenericValue routing = EntityUtil.lookupById(delegator, 'WorkEffort', 'workEffortId',
            [routingId])[routingId]
    if (!routing || routing.workEffortTypeId != 'ROUTING') {
        return ServiceUtil.returnError('Routing not found: ' + routingId)
    }
    List taskAssocs = routingTaskAssocs(routingId)
    Map tasks = EntityUtil.lookupById(delegator, 'WorkEffort', 'workEffortId',
            taskAssocs*.workEffortIdTo.findAll { it } as Set)
    List products = routingProductLinks(routingId)
    Map productLookups = EntityUtil.lookupById(delegator, 'Product', 'productId',
            products*.productId.findAll { it } as Set, false)
    Map workCenters = EntityUtil.lookupById(delegator, 'FixedAsset', 'fixedAssetId',
            tasks.values()*.fixedAssetId.findAll { it } as Set, false)
    Map purposeTypes = EntityUtil.lookupById(delegator, 'WorkEffortPurposeType',
            'workEffortPurposeTypeId', tasks.values()*.workEffortPurposeTypeId.findAll { it } as Set)
    List operationMaps = taskAssocs.collect { GenericValue assoc ->
        GenericValue task = tasks[assoc.workEffortIdTo]
        [
                routingId: assoc.workEffortIdFrom,
                routingTaskId: assoc.workEffortIdTo,
                workEffortId: assoc.workEffortIdTo,
                workEffortName: task?.workEffortName,
                operationName: task?.workEffortName,
                sequenceNum: assoc.sequenceNum,
                fromDate: assoc.fromDate,
                thruDate: assoc.thruDate,
                workEffortPurposeTypeId: task?.workEffortPurposeTypeId,
                workEffortPurposeTypeDescription: purposeTypes[task?.workEffortPurposeTypeId]?.description,
                fixedAssetId: task?.fixedAssetId,
                fixedAssetName: ManufacturingServiceUtil.displayFixedAssetName(workCenters[task?.fixedAssetId]),
                workCenterName: ManufacturingServiceUtil.displayFixedAssetName(workCenters[task?.fixedAssetId]),
                estimatedSetupMillis: task?.estimatedSetupMillis,
                estimatedMilliSeconds: task?.estimatedMilliSeconds,
                estimateCalcMethod: task?.estimateCalcMethod,
                reservPersons: task?.reservPersons
        ]
    }
    Map response = [
            routingId: routing.workEffortId,
            workEffortId: routing.workEffortId,
            workEffortTypeId: routing.workEffortTypeId,
            workEffortName: routing.workEffortName,
            description: routing.description,
            currentStatusId: routing.currentStatusId,
            quantityToProduce: routing.quantityToProduce,
            products: products.collect { GenericValue link ->
                GenericValue product = productLookups[link.productId]
                [
                        routingId: link.workEffortId,
                        workEffortId: link.workEffortId,
                        productId: link.productId,
                        productName: ManufacturingServiceUtil.displayProductName(product),
                        fromDate: link.fromDate,
                        thruDate: link.thruDate,
                        estimatedQuantity: link.estimatedQuantity,
                        estimatedCost: link.estimatedCost,
                        workEffortGoodStdTypeId: link.workEffortGoodStdTypeId
                ]
            },
            operations: operationMaps,
            tasks: operationMaps
    ]
    response.costs = costRowsByWorkEffortId(tasks.keySet()).values().flatten() as List
    return success(response)
}

Map findRoutingTasks() {
    Map result = ServiceUtil.returnSuccess()
    result.putAll(routingTaskList(false))
    return result
}

Map searchRoutingTasksForRouting() {
    Map result = ServiceUtil.returnSuccess()
    result.putAll(routingTaskList(true))
    return result
}

Map routingTaskList(boolean requireQuery) {
    RestQueryOptions queryOptions
    try {
        queryOptions = RestQueryOptions.fromParameters(parameters)
    } catch (IllegalArgumentException e) {
        return ServiceUtil.returnError(e.message)
    }
    Map filters = queryOptions.filters
    List orderBy
    try {
        orderBy = routingTaskOrderBy(queryOptions.sort)
    } catch (IllegalArgumentException e) {
        return ServiceUtil.returnError(e.message)
    }
    String queryText = filters.query?.toString()?.trim()
    if (requireQuery && UtilValidate.isEmpty(queryText)) {
        return RestApiUtil.getPagedResult('tasks', [], queryOptions, 0L,
                RestApiUtil.getRelativeRequestPath(binding))
    }

    List conditions = [EntityCondition.makeCondition('workEffortTypeId', 'ROU_TASK')]
    if (UtilValidate.isNotEmpty(queryText)) {
        conditions.add(EntityUtil.upperLikeAny(['workEffortId', 'workEffortName', 'description', 'workEffortPurposeTypeId'], queryText))
    }
    if (!requireQuery && UtilValidate.isNotEmpty(filters.workEffortId ?: filters.routingTaskId)) {
        conditions.add(EntityCondition.makeCondition('workEffortId', filters.workEffortId ?: filters.routingTaskId))
    }
    if (!requireQuery && UtilValidate.isNotEmpty(filters.workEffortName)) {
        conditions.add(EntityCondition.makeCondition('workEffortName', EntityOperator.LIKE, '%' + filters.workEffortName + '%'))
    }
    if (!requireQuery && UtilValidate.isNotEmpty(filters.fixedAssetId)) {
        conditions.add(EntityCondition.makeCondition('fixedAssetId', filters.fixedAssetId))
    }

    EntityCondition whereCondition = EntityCondition.makeCondition(conditions, EntityOperator.AND)
    EntityQuery query = from('WorkEffort').where(whereCondition)
    long totalCount = query.queryCount()
    List rows = query.orderBy(orderBy)
            .queryPagedList(queryOptions.pageIndex, queryOptions.pageSize).getData()
    Map workCenters = EntityUtil.lookupById(delegator, 'FixedAsset', 'fixedAssetId',
            rows*.fixedAssetId.findAll { it } as Set, false)
    Map purposeTypes = EntityUtil.lookupById(delegator, 'WorkEffortPurposeType',
            'workEffortPurposeTypeId', rows*.workEffortPurposeTypeId.findAll { it } as Set)
    List tasks = rows.collect { GenericValue task -> taskMap(task, null, workCenters, purposeTypes) }

    return RestApiUtil.getPagedResult('tasks', tasks, queryOptions, totalCount,
            RestApiUtil.getRelativeRequestPath(binding))
}

Map getRoutingTaskDetails() {
    String taskId = parameters.routingTaskId ?: parameters.taskId ?: parameters.workEffortId
    if (!taskId) {
        return ServiceUtil.returnError('routingTaskId is required.')
    }
    GenericValue task = EntityUtil.lookupById(delegator, 'WorkEffort', 'workEffortId', [taskId])[taskId]
    if (!task || task.workEffortTypeId != 'ROU_TASK') {
        return ServiceUtil.returnError('Routing task not found: ' + taskId)
    }
    return success(taskDetailMap(task))
}

Map findRoutingTaskPurposeTypeOptions() {
    RestQueryOptions queryOptions
    try {
        queryOptions = RestQueryOptions.fromParameters(parameters)
    } catch (IllegalArgumentException e) {
        return ServiceUtil.returnError(e.message)
    }
    List orderBy
    try {
        orderBy = routingPurposeTypeOrderBy(queryOptions.sort)
    } catch (IllegalArgumentException e) {
        return ServiceUtil.returnError(e.message)
    }
    EntityQuery query = from('WorkEffortPurposeType')
            .select('workEffortPurposeTypeId', 'description')
            .where(EntityCondition.makeCondition('workEffortPurposeTypeId', EntityOperator.LIKE, 'ROU%'))
            .cache(true)
    long totalCount = query.queryCount()
    List purposeTypes = query.orderBy(orderBy)
            .queryPagedList(queryOptions.pageIndex, queryOptions.pageSize).getData()
            .collect { GenericValue purposeType ->
                String purposeTypeId = purposeType.workEffortPurposeTypeId
                String description = purposeType.description ?: purposeTypeId
                [
                        workEffortPurposeTypeId: purposeTypeId,
                        description: description,
                        label: "${description} [${purposeTypeId}]".toString()
                ]
            }

    return success(RestApiUtil.getPagedResult('purposeTypes', purposeTypes, queryOptions, totalCount,
            RestApiUtil.getRelativeRequestPath(binding)))
}

List routingOrderBy(String sortExpression) {
    return RestApiUtil.resolveOrderBy(sortExpression, [
            routingId: 'workEffortId',
            workEffortId: 'workEffortId',
            workEffortName: 'workEffortName',
            currentStatusId: 'currentStatusId',
            status: 'currentStatusId',
            quantityToProduce: 'quantityToProduce'
    ], ['workEffortId'])
}

List routingTaskOrderBy(String sortExpression) {
    return RestApiUtil.resolveOrderBy(sortExpression, [
            routingTaskId: 'workEffortId',
            workEffortId: 'workEffortId',
            workEffortName: 'workEffortName',
            workEffortPurposeTypeId: 'workEffortPurposeTypeId',
            fixedAssetId: 'fixedAssetId'
    ], ['workEffortName', 'workEffortId'])
}

List routingPurposeTypeOrderBy(String sortExpression) {
    return RestApiUtil.resolveOrderBy(sortExpression, [
            workEffortPurposeTypeId: 'workEffortPurposeTypeId',
            description: 'description'
    ], ['workEffortPurposeTypeId'])
}

Map taskDetailMap(GenericValue task) {
    if (!task) {
        return [:]
    }
    List usage = from('WorkEffortAssoc')
            .where(workEffortIdTo: task.workEffortId, workEffortAssocTypeId: 'ROUTING_COMPONENT')
            .filterByDate()
            .orderBy('workEffortIdFrom', 'sequenceNum')
            .queryList()
    List costs = costRowsByWorkEffortId([task.workEffortId])[task.workEffortId] ?: []
    List outputs = outputRowsByWorkEffortId([task.workEffortId])[task.workEffortId] ?: []
    List tools = toolRowsByWorkEffortId([task.workEffortId])[task.workEffortId] ?: []
    List operators = operatorRowsByWorkEffortId([task.workEffortId])[task.workEffortId] ?: []
    Map workCenters = EntityUtil.lookupById(delegator, 'FixedAsset', 'fixedAssetId',
            [task.fixedAssetId], false)
    Map purposeTypes = EntityUtil.lookupById(delegator, 'WorkEffortPurposeType',
            'workEffortPurposeTypeId', [task.workEffortPurposeTypeId])
    return taskMap(task, usage, workCenters, purposeTypes) + [
            costRates: costs,
            outputs: outputs,
            tools: tools,
            operators: operators,
            costs: costs,
            fixedAssets: fixedAssetAssignmentsByWorkEffortId([task.workEffortId])[task.workEffortId] ?: []
    ]
}

Map expireRoutingGoodStandard() {
    Timestamp thruDate = UtilDateTime.nowTimestamp()
    Map updateResult = runService('updateWorkEffortGoodStandard', [
            workEffortId: parameters.workEffortId,
            productId: parameters.productId,
            workEffortGoodStdTypeId: parameters.workEffortGoodStdTypeId,
            fromDate: parameters.fromDate,
            thruDate: thruDate,
            userLogin: userLogin
    ])
    if (ServiceUtil.isError(updateResult)) {
        return updateResult
    }
    return success([
            workEffortId: parameters.workEffortId,
            productId: parameters.productId,
            workEffortGoodStdTypeId: parameters.workEffortGoodStdTypeId,
            fromDate: parameters.fromDate,
            thruDate: thruDate,
            removed: true
    ])
}

List routingTaskAssocs(String routingId) {
    return from('WorkEffortAssoc')
            .where(workEffortIdFrom: routingId, workEffortAssocTypeId: 'ROUTING_COMPONENT')
            .filterByDate()
            .orderBy('sequenceNum', 'workEffortIdTo')
            .queryList()
}

List routingProductLinks(String routingId) {
    return from('WorkEffortGoodStandard')
            .where(workEffortId: routingId, workEffortGoodStdTypeId: 'ROU_PROD_TEMPLATE')
            .filterByDate()
            .orderBy('productId', 'fromDate')
            .queryList()
}

Map taskMap(GenericValue task, List usage, Map workCenters = [:], Map purposeTypes = [:]) {
    [
            routingTaskId: task?.workEffortId,
            workEffortId: task?.workEffortId,
            workEffortTypeId: task?.workEffortTypeId,
            workEffortName: task?.workEffortName,
            description: task?.description,
            currentStatusId: task?.currentStatusId,
            workEffortPurposeTypeId: task?.workEffortPurposeTypeId,
            workEffortPurposeTypeDescription: purposeTypes[task?.workEffortPurposeTypeId]?.description,
            fixedAssetId: task?.fixedAssetId,
            fixedAssetName: ManufacturingServiceUtil.displayFixedAssetName(workCenters[task?.fixedAssetId]),
            workCenterName: ManufacturingServiceUtil.displayFixedAssetName(workCenters[task?.fixedAssetId]),
            estimatedMilliSeconds: task?.estimatedMilliSeconds,
            estimatedSetupMillis: task?.estimatedSetupMillis,
            estimateCalcMethod: task?.estimateCalcMethod,
            reservPersons: task?.reservPersons,
            routings: (usage ?: []).collect { GenericValue assoc ->
                [routingId: assoc.workEffortIdFrom, workEffortIdFrom: assoc.workEffortIdFrom,
                        sequenceNum: assoc.sequenceNum, fromDate: assoc.fromDate, thruDate: assoc.thruDate]
            }
    ]
}

String costRateStatus(Timestamp fromDate, Timestamp thruDate) {
    Timestamp now = UtilDateTime.nowTimestamp()
    if (thruDate && thruDate.before(now)) {
        return 'EXPIRED'
    }
    return fromDate && fromDate.after(now) ? 'SCHEDULED' : 'ACTIVE'
}

Map routingTaskCountByRouting(Collection routingIds) {
    if (!routingIds) {
        return [:]
    }
    List assocs = from('WorkEffortAssoc')
            .where(EntityCondition.makeCondition([
                    EntityCondition.makeCondition('workEffortIdFrom', EntityOperator.IN, routingIds as List),
                    EntityCondition.makeCondition('workEffortAssocTypeId', 'ROUTING_COMPONENT')
            ], EntityOperator.AND))
            .filterByDate()
            .queryList()
    return assocs.groupBy { it.workEffortIdFrom }.collectEntries { String id, List rows -> [(id): rows.size()] }
}

Map routingProductCountByRouting(Collection routingIds) {
    if (!routingIds) {
        return [:]
    }
    List links = from('WorkEffortGoodStandard')
            .where(EntityCondition.makeCondition([
                    EntityCondition.makeCondition('workEffortId', EntityOperator.IN, routingIds as List),
                    EntityCondition.makeCondition('workEffortGoodStdTypeId', 'ROU_PROD_TEMPLATE')
            ], EntityOperator.AND))
            .filterByDate()
            .queryList()
    return links.groupBy { it.workEffortId }.collectEntries { String id, List rows -> [(id): rows.size()] }
}

Set routingIdsForProducts(Collection productIds) {
    if (!productIds) {
        return [] as Set
    }
    return from('WorkEffortGoodStandard')
            .select('workEffortId')
            .where(EntityCondition.makeCondition([
                    EntityCondition.makeCondition('productId', EntityOperator.IN, productIds as List),
                    EntityCondition.makeCondition('workEffortGoodStdTypeId', 'ROU_PROD_TEMPLATE')
            ], EntityOperator.AND))
            .filterByDate()
            .maxRows(500)
            .getFieldList('workEffortId') as Set
}

Map costRowsByWorkEffortId(Collection workEffortIds) {
    if (!workEffortIds) {
        return [:]
    }
    List costRows = from('WorkEffortCostCalc')
            .where(EntityCondition.makeCondition('workEffortId', EntityOperator.IN, workEffortIds as List))
            .filterByDate()
            .orderBy('workEffortId', 'costComponentTypeId')
            .queryList()
    Map calcs = EntityUtil.lookupById(delegator, 'CostComponentCalc', 'costComponentCalcId',
            costRows*.costComponentCalcId.findAll { it } as Set)
    Map costTypes = EntityUtil.lookupById(delegator, 'CostComponentType', 'costComponentTypeId',
            costRows*.costComponentTypeId.findAll { it } as Set)
    return groupedRowsByWorkEffortId(costRows) { GenericValue row, String ignoredWorkEffortId ->
        GenericValue calc = calcs[row.costComponentCalcId]
        GenericValue costType = costTypes[row.costComponentTypeId]
        [
                routingTaskId: row.workEffortId,
                workEffortId: row.workEffortId,
                costComponentTypeId: row.costComponentTypeId,
                costCategory: costType?.description,
                costComponentCalcId: row.costComponentCalcId,
                calculationRule: calc?.description,
                setupCost: calc?.fixedCost,
                runRate: calc?.variableCost,
                runRateUom: calc?.currencyUomId,
                fromDate: row.fromDate,
                thruDate: row.thruDate,
                status: costRateStatus(row.getTimestamp('fromDate'), row.getTimestamp('thruDate'))
        ]
    }
}

Map fixedAssetAssignmentsByWorkEffortId(Collection workEffortIds) {
    if (!workEffortIds) {
        return [:]
    }
    return groupedRowsByWorkEffortId(from('WorkEffortFixedAssetAssign')
            .where(EntityCondition.makeCondition('workEffortId', EntityOperator.IN, workEffortIds as List))
            .filterByDate()
            .orderBy('workEffortId', 'fixedAssetId')
            .queryList()) { GenericValue row, String ignoredWorkEffortId ->
        [
                fixedAssetId: row.fixedAssetId,
                fromDate: row.fromDate,
                thruDate: row.thruDate,
                statusId: row.statusId,
                availabilityStatusId: row.availabilityStatusId,
                allocatedCost: row.allocatedCost
        ]
    }
}

Map outputRowsByWorkEffortId(Collection workEffortIds) {
    if (!workEffortIds) {
        return [:]
    }
    List outputs = from('WorkEffortGoodStandard')
            .where(EntityCondition.makeCondition([
                    EntityCondition.makeCondition('workEffortId', EntityOperator.IN, workEffortIds as List),
                    EntityCondition.makeCondition('workEffortGoodStdTypeId', 'PRUNT_PROD_DELIV')
            ], EntityOperator.AND))
            .filterByDate()
            .orderBy('workEffortId', 'productId', 'fromDate')
            .queryList()
    Map products = EntityUtil.lookupById(delegator, 'Product', 'productId',
            outputs*.productId.findAll { it } as Set, false)
    return groupedRowsByWorkEffortId(outputs) { GenericValue row, String ignoredWorkEffortId ->
        GenericValue product = products[row.productId]
        [
                routingTaskId: row.workEffortId,
                workEffortId: row.workEffortId,
                productId: row.productId,
                productName: ManufacturingServiceUtil.displayProductName(product),
                estimatedQuantity: row.estimatedQuantity,
                quantityUomId: product?.quantityUomId,
                estimatedCost: row.estimatedCost,
                fromDate: row.fromDate,
                thruDate: row.thruDate,
                workEffortGoodStdTypeId: row.workEffortGoodStdTypeId
        ]
    }
}

Map toolRowsByWorkEffortId(Collection workEffortIds) {
    if (!workEffortIds) {
        return [:]
    }
    List tools = from('WorkEffortFixedAssetStd')
            .where(EntityCondition.makeCondition('workEffortId', EntityOperator.IN, workEffortIds as List))
            .orderBy('workEffortId', 'fixedAssetTypeId')
            .queryList()
    Map fixedAssetTypes = EntityUtil.lookupById(delegator, 'FixedAssetType', 'fixedAssetTypeId',
            tools*.fixedAssetTypeId.findAll { it } as Set)
    Map tasks = EntityUtil.lookupById(delegator, 'WorkEffort', 'workEffortId',
            tools*.workEffortId.findAll { it } as Set)
    return groupedRowsByWorkEffortId(tools) { GenericValue row, String workEffortId ->
        GenericValue fixedAssetType = fixedAssetTypes[row.fixedAssetTypeId]
        GenericValue task = tasks[workEffortId]
        [
                fixedAssetTypeId: row.fixedAssetTypeId,
                fixedAssetId: task?.fixedAssetId,
                toolName: fixedAssetType?.description,
                estimatedQuantity: row.estimatedQuantity,
                estimatedDuration: row.estimatedDuration,
                estimatedCost: row.estimatedCost,
                fromDate: null,
                thruDate: null
        ]
    }
}

Map operatorRowsByWorkEffortId(Collection workEffortIds) {
    if (!workEffortIds) {
        return [:]
    }
    List operators = from('WorkEffortPartyAssignment')
            .where(EntityCondition.makeCondition('workEffortId', EntityOperator.IN, workEffortIds as List))
            .filterByDate()
            .orderBy('workEffortId', 'partyId', 'roleTypeId')
            .queryList()
    Map partyNames = EntityUtil.lookupById(delegator, 'PartyNameView', 'partyId',
            operators*.partyId.findAll { it } as Set, false)
    Map roleTypes = EntityUtil.lookupById(delegator, 'RoleType', 'roleTypeId',
            operators*.roleTypeId.findAll { it } as Set)
    return groupedRowsByWorkEffortId(operators) { GenericValue row, String ignoredWorkEffortId ->
        GenericValue partyName = partyNames[row.partyId]
        GenericValue roleType = roleTypes[row.roleTypeId]
        [
                partyId: row.partyId,
                operatorName: PartyHelper.getPartyName(partyName, false) ?: row.partyId,
                roleTypeId: row.roleTypeId,
                roleTypeDescription: roleType?.description ?: row.roleTypeId,
                statusId: row.statusId,
                comments: row.comments,
                fromDate: row.fromDate,
                thruDate: row.thruDate
        ]
    }
}

Map groupedRowsByWorkEffortId(List rows, Closure rowMapper) {
    return rows.groupBy { it.workEffortId }.collectEntries { String workEffortId, List groupedRows ->
        [(workEffortId): groupedRows.collect { GenericValue row -> rowMapper(row, workEffortId) }]
    }
}
