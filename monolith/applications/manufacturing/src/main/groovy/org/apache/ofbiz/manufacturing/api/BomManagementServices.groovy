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
import org.apache.ofbiz.base.util.UtilMisc
import org.apache.ofbiz.base.util.UtilProperties
import org.apache.ofbiz.base.util.UtilValidate
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.entity.condition.EntityCondition
import org.apache.ofbiz.entity.condition.EntityOperator
import org.apache.ofbiz.entity.util.EntityQuery
import org.apache.ofbiz.entity.util.EntityUtil
import org.apache.ofbiz.manufacturing.bom.BOMNode
import org.apache.ofbiz.manufacturing.bom.BOMTree
import org.apache.ofbiz.service.ServiceUtil
import org.apache.ofbiz.ws.rs.util.RestApiUtil
import org.apache.ofbiz.ws.rs.util.RestQueryOptions

Map findBomProducts() {
    RestQueryOptions queryOptions
    try {
        queryOptions = RestQueryOptions.fromParameters(parameters)
    } catch (IllegalArgumentException e) {
        return ServiceUtil.returnError(e.message)
    }
    List orderBy
    try {
        orderBy = bomProductOrderBy(queryOptions.sort)
    } catch (IllegalArgumentException e) {
        return ServiceUtil.returnError(e.message)
    }
    List bomTypeIds = bomAssocTypeIds()
    if (!bomTypeIds) {
        return success(RestApiUtil.getPagedResult('products', [], queryOptions, 0L,
                RestApiUtil.getRelativeRequestPath(binding)))
    }
    List selectedBomTypeIds = UtilValidate.isNotEmpty(parameters.productAssocTypeId)
            ? [parameters.productAssocTypeId]
            : bomTypeIds
    List conditions = [EntityCondition.makeCondition('productAssocTypeId', EntityOperator.IN, selectedBomTypeIds)]
    if (UtilValidate.isNotEmpty(parameters.productId)) {
        conditions.add(EntityCondition.makeCondition('productId', parameters.productId))
    }
    if (UtilValidate.isNotEmpty(parameters.componentProductId ?: parameters.productIdTo)) {
        conditions.add(EntityCondition.makeCondition('productIdTo', parameters.componentProductId ?: parameters.productIdTo))
    }
    if (UtilValidate.isNotEmpty(parameters.query)) {
        Set matchingProductIds = EntityUtil.searchIds(delegator, 'Product', 'productId',
                ['productId', 'productName', 'internalName'], parameters.query, 500)
        if (!matchingProductIds) {
            return success(RestApiUtil.getPagedResult('products', [], queryOptions, 0L,
                    RestApiUtil.getRelativeRequestPath(binding)))
        }
        conditions.add(EntityCondition.makeCondition('productId', EntityOperator.IN, matchingProductIds as List))
    }

    EntityCondition whereCondition = EntityCondition.makeCondition(conditions, EntityOperator.AND)
    EntityQuery query = select('productId', 'productAssocTypeId').from('ProductAssoc')
            .where(whereCondition).filterByDate().distinct()
    long totalCount = query.queryCount()
    List rows = query.orderBy(orderBy)
            .queryPagedList(queryOptions.pageIndex, queryOptions.pageSize).getData()
    Set productIds = rows*.productId.findAll { it } as Set
    Map productsById = EntityUtil.lookupById(delegator, 'Product', 'productId', productIds, false)
    Map componentSummaries = componentSummaryByProduct(productIds, selectedBomTypeIds)
    Map usedInCounts = EntityUtil.countByField(delegator, 'ProductAssoc', 'productIdTo', productIds,
            [EntityCondition.makeCondition('productAssocTypeId', EntityOperator.IN, selectedBomTypeIds)], true)

    List products = rows.collect { GenericValue row ->
        GenericValue product = productsById[row.productId]
        Map componentSummary = componentSummaries[row.productId] ?: [:]
        [
                productId: row.productId,
                productName: ManufacturingServiceUtil.displayProductName(product),
                internalName: product?.internalName,
                productAssocTypeId: row.productAssocTypeId,
                bomType: row.productAssocTypeId,
                componentCount: componentSummary.count ?: 0,
                usedInCount: usedInCounts[row.productId] ?: 0,
                activeFromDate: componentSummary.activeFromDate
        ]
    }
    return success(RestApiUtil.getPagedResult('products', products, queryOptions, totalCount,
            RestApiUtil.getRelativeRequestPath(binding)))
}

Map getBomSnapshot() {
    String productId = parameters.productId
    if (!productId) {
        return ServiceUtil.returnError(manufacturingMessage('ManufacturingProductIdRequired'))
    }
    GenericValue product = from('Product').where(productId: productId).queryOne()
    if (!product) {
        return ServiceUtil.returnError(manufacturingMessage('ManufacturingProductNotFound',
                [productId: productId]))
    }

    String productAssocTypeId = parameters.productAssocTypeId ?: 'MANUF_COMPONENT'
    Timestamp fromDate = parameters.fromDate
            ?: parameters.asOfDate
            ?: UtilDateTime.nowTimestamp()
    BigDecimal quantity = parameters.quantity ?: BigDecimal.ONE
    BigDecimal amount = parameters.amount ?: BigDecimal.ZERO
    Integer type = UtilMisc.toIntegerObject(parameters.type) ?: 0

    List componentRows = datedProductAssocRows([productId: productId, productAssocTypeId: productAssocTypeId],
            fromDate, 'sequenceNum', 'productIdTo')
    List components = snapshotComponentRows(componentRows, quantity)
    List bomLevels = bomLevelRows(productId, productAssocTypeId, fromDate, quantity, type)
    List parentRows = datedProductAssocRows([productIdTo: productId, productAssocTypeId: productAssocTypeId],
            fromDate, 'sequenceNum', 'productId')
    List usedInParents = snapshotParentRows(parentRows, product.quantityUomId as String)

    return success(productId: productId, productName: ManufacturingServiceUtil.displayProductName(product), internalName: product.internalName,
            productAssocTypeId: productAssocTypeId, bomType: productAssocTypeId, fromDate: fromDate, quantity: quantity,
            amount: amount, type: type, facilityId: parameters.facilityId, currencyUomId: parameters.currencyUomId,
            componentCount: components.size(), usedInCount: usedInParents.size(), usedInParents: usedInParents,
            bomLevels: bomLevels, components: components)
}

List bomLevelRows(String productId, String productAssocTypeId, Timestamp fromDate, BigDecimal quantity, Integer type) {
    Map treeResult = runService('getBOMTree', [
            productId: productId,
            bomType: productAssocTypeId,
            fromDate: fromDate,
            quantity: quantity,
            type: type,
            userLogin: userLogin
    ].findAll { it.value != null })
    if (ServiceUtil.isError(treeResult)) {
        return []
    }

    BOMTree tree = treeResult.tree as BOMTree
    List nodes = []
    tree?.print(nodes)
    return nodes.collect { BOMNode node ->
        GenericValue nodeProduct = node.getProduct()
        GenericValue productAssoc = node.getProductAssoc()
        [
                level: node.getDepth(),
                productId: nodeProduct?.productId,
                productName: ManufacturingServiceUtil.displayProductName(nodeProduct),
                parentProductId: productAssoc?.productId,
                componentProductId: node.getDepth() == 0 ? null : nodeProduct?.productId,
                componentName: node.getDepth() == 0 ? null : ManufacturingServiceUtil.displayProductName(nodeProduct),
                quantity: node.getQuantity(),
                quantityUomId: nodeProduct?.quantityUomId,
                fromDate: productAssoc?.fromDate,
                thruDate: productAssoc?.thruDate,
                productAssocTypeId: productAssoc?.productAssocTypeId ?: productAssocTypeId
        ]
    }
}

List datedProductAssocRows(Map fields, Timestamp fromDate, String... orderBy) {
    return from('ProductAssoc')
            .where(fields)
            .filterByDate(fromDate)
            .orderBy(orderBy as List)
            .queryList()
}

List snapshotComponentRows(List componentRows, BigDecimal bomQuantity) {
    Set componentProductIds = componentRows*.productIdTo.findAll { it } as Set
    Set routingIds = componentRows*.routingWorkEffortId.findAll { it } as Set
    Map componentProducts = EntityUtil.lookupById(delegator, 'Product', 'productId',
            componentProductIds, false)
    Map routings = EntityUtil.lookupById(delegator, 'WorkEffort', 'workEffortId', routingIds)
    return componentRows.collect { GenericValue component ->
        BigDecimal componentQuantity = component.getBigDecimal('quantity') ?: BigDecimal.ZERO
        GenericValue componentProduct = componentProducts[component.productIdTo]
        GenericValue routing = routings[component.routingWorkEffortId]
        [
                level: 1,
                productId: component.productId,
                componentProductId: component.productIdTo,
                componentName: ManufacturingServiceUtil.displayProductName(componentProduct),
                componentProductName: ManufacturingServiceUtil.displayProductName(componentProduct),
                quantityUomId: componentProduct?.quantityUomId,
                productAssocTypeId: component.productAssocTypeId,
                fromDate: component.fromDate,
                thruDate: component.thruDate,
                sequenceNum: component.sequenceNum,
                quantity: componentQuantity,
                extendedQuantity: componentQuantity * bomQuantity,
                scrapFactor: component.scrapFactor,
                instruction: component.instruction,
                routingWorkEffortId: component.routingWorkEffortId,
                routingName: routing?.workEffortName,
                estimateCalcMethod: component.estimateCalcMethod,
                recurrenceInfoId: component.recurrenceInfoId
        ]
    }
}

List snapshotParentRows(List parentRows, String quantityUomId) {
    Set parentProductIds = parentRows*.productId.findAll { it } as Set
    Map parentProducts = EntityUtil.lookupById(delegator, 'Product', 'productId', parentProductIds,
            false)
    return parentRows.collect { GenericValue association ->
        GenericValue parentProduct = parentProducts[association.productId]
        [
                parentProductId: association.productId,
                parentProductName: ManufacturingServiceUtil.displayProductName(parentProduct),
                componentProductId: association.productIdTo,
                quantity: association.getBigDecimal('quantity'),
                quantityUomId: quantityUomId,
                sequenceNum: association.sequenceNum,
                fromDate: association.fromDate,
                thruDate: association.thruDate,
                productAssocTypeId: association.productAssocTypeId
        ]
    }
}

Map removeBomAssociation() {
    return expireBomAssociation(parameters.productId, parameters.productIdTo, parameters.productAssocTypeId,
            parameters.fromDate)
}

Map removeBomParentAssociation() {
    return expireBomAssociation(parameters.productId, parameters.componentProductId, parameters.productAssocTypeId,
            parameters.fromDate)
}

Map expireBomAssociation(String productId, String productIdTo, String assocTypeId, Timestamp fromDate) {
    String productAssocTypeId = assocTypeId ?: 'MANUF_COMPONENT'
    GenericValue association = from('ProductAssoc')
            .where(productId: productId, productIdTo: productIdTo,
                    productAssocTypeId: productAssocTypeId, fromDate: fromDate)
            .queryOne()
    if (!association) {
        return ServiceUtil.returnError(manufacturingMessage('ManufacturingBomAssociationNotFound'))
    }
    Timestamp thruDate = UtilDateTime.nowTimestamp()
    Map updateResult = runService('updateProductAssoc', [
            productId: association.productId,
            productIdTo: association.productIdTo,
            productAssocTypeId: association.productAssocTypeId,
            fromDate: association.fromDate,
            thruDate: thruDate,
            userLogin: userLogin
    ])
    if (ServiceUtil.isError(updateResult)) {
        return updateResult
    }
    if (UtilValidate.isNotEmpty(updateResult.errorMessage)) {
        return ServiceUtil.returnError(updateResult.errorMessage)
    }
    return success([
            removed: true,
            productId: association.productId,
            parentProductId: association.productId,
            productIdTo: association.productIdTo,
            componentProductId: association.productIdTo,
            productAssocTypeId: association.productAssocTypeId,
            fromDate: association.fromDate,
            thruDate: thruDate,
            quantity: association.quantity,
            sequenceNum: association.sequenceNum
    ])
}

Map runBomSimulation() {
    String productId = parameters.productId
    if (!productId) {
        return ServiceUtil.returnError(manufacturingMessage('ManufacturingProductIdRequired'))
    }
    String productAssocTypeId = parameters.productAssocTypeId ?: 'MANUF_COMPONENT'
    Timestamp effectiveDate = parameters.effectiveDate
            ?: parameters.fromDate
            ?: parameters.asOfDate
    BigDecimal quantity = parameters.quantity ?: BigDecimal.ONE
    String simulationMode = parameters.simulationMode ?: 'EXPLOSION'
    String currencyUomId = parameters.currencyUomId
    String facilityId = parameters.facilityId
    List warnings = []

    Integer type = UtilMisc.toIntegerObject(parameters.type)
    if (type == null) {
        type = simulationMode == 'IMPLOSION' ? BOMTree.IMPLOSION : BOMTree.EXPLOSION
    }
    Map treeResult = runService('getBOMTree', [
            productId: productId,
            bomType: productAssocTypeId,
            fromDate: effectiveDate,
            quantity: quantity,
            type: type,
            userLogin: userLogin
    ].findAll { it.value != null })
    if (ServiceUtil.isError(treeResult)) {
        return treeResult
    }

    GenericValue rootProduct = from('Product').where(productId: productId).cache().queryOne()
    BOMTree tree = treeResult.tree as BOMTree
    List nodes = []
    tree?.print(nodes)
    List componentNodes = nodes.findAll { BOMNode node -> node && node.getDepth() > 0 }
    Set productsWithCostSources = costSourceProductIds(componentNodes.collect { BOMNode node -> node.getProduct()?.productId },
            currencyUomId)
    List availabilityRows = componentNodes.collect { BOMNode node ->
        availabilityRow(node, facilityId, warnings)
    }
    List costRows = componentNodes.collect { BOMNode node ->
        costRow(node, currencyUomId, warnings, productsWithCostSources)
    }
    BigDecimal knownMaterialCost = costRows.inject(BigDecimal.ZERO) { BigDecimal total, Map row ->
        row.totalCost != null ? total.add(row.totalCost as BigDecimal) : total
    }
    int missingCostCount = costRows.count { Map row -> row.costStatus == 'MISSING_COST' }
    if (!facilityId) {
        warnings.add(manufacturingMessage('ManufacturingBomSimulationAvailabilityNotChecked'))
    }
    if (!currencyUomId) {
        warnings.add(manufacturingMessage('ManufacturingBomSimulationCostNotChecked'))
    }

    Map requestParameters = [
            productId: productId,
            productName: ManufacturingServiceUtil.displayProductName(rootProduct),
            quantity: quantity,
            productAssocTypeId: productAssocTypeId,
            facilityId: facilityId,
            currencyUomId: currencyUomId,
            effectiveDate: effectiveDate,
            simulationMode: simulationMode
    ]
    Map summary = [
            requiredComponentCount: componentNodes.size(),
            knownMaterialCost: knownMaterialCost,
            missingCostCount: missingCostCount,
            availabilityStatus: facilityId ? 'CHECKED' : 'NOT_CHECKED'
    ]
    return success(parameters: requestParameters, summary: summary, availabilityRows: availabilityRows,
            costRows: costRows, warnings: warnings.unique())
}

Map findBomTypes() {
    RestQueryOptions queryOptions
    try {
        queryOptions = RestQueryOptions.fromParameters(parameters)
    } catch (IllegalArgumentException e) {
        return ServiceUtil.returnError(e.message)
    }
    List orderBy
    try {
        orderBy = bomTypeOrderBy(queryOptions.sort)
    } catch (IllegalArgumentException e) {
        return ServiceUtil.returnError(e.message)
    }
    EntityQuery query = from('ProductAssocType')
            .select('productAssocTypeId', 'description')
            .where(parentTypeId: 'PRODUCT_COMPONENT')
            .cache(true)
    long totalCount = query.queryCount()
    List bomTypes = query.orderBy(orderBy)
            .queryPagedList(queryOptions.pageIndex, queryOptions.pageSize).getData()
            .collect { GenericValue productAssocType ->
                String productAssocTypeId = productAssocType.productAssocTypeId
                String description = productAssocType.description ?: productAssocTypeId
                [
                        productAssocTypeId: productAssocTypeId,
                        description: description,
                        label: description
                ]
            }
    return success(RestApiUtil.getPagedResult('bomTypes', bomTypes, queryOptions, totalCount,
            RestApiUtil.getRelativeRequestPath(binding)))
}

List bomProductOrderBy(String sortExpression) {
    return RestApiUtil.resolveOrderBy(sortExpression, [
            productId: 'productId',
            productAssocTypeId: 'productAssocTypeId',
            bomType: 'productAssocTypeId'
    ], ['productId', 'productAssocTypeId'])
}

List bomTypeOrderBy(String sortExpression) {
    return RestApiUtil.resolveOrderBy(sortExpression, [
            productAssocTypeId: 'productAssocTypeId',
            description: 'description'
    ], ['description', 'productAssocTypeId'])
}

List bomAssocTypeIds() {
    return from('ProductAssocType').where(parentTypeId: 'PRODUCT_COMPONENT')
            .cache(true).getFieldList('productAssocTypeId') ?: []
}

Map componentSummaryByProduct(Collection productIds, List bomTypeIds) {
    if (!productIds || !bomTypeIds) {
        return [:]
    }
    List components = from('ProductAssoc')
            .select('productId', 'fromDate')
            .where(EntityCondition.makeCondition([
                    EntityCondition.makeCondition('productId', EntityOperator.IN, productIds as List),
                    EntityCondition.makeCondition('productAssocTypeId', EntityOperator.IN, bomTypeIds)
            ], EntityOperator.AND))
            .filterByDate()
            .queryList()
    return components.groupBy { it.productId }.collectEntries { String id, List rows ->
        [(id): [count: rows.size(), activeFromDate: rows*.fromDate.min()]]
    }
}

Map availabilityRow(BOMNode node, String facilityId, List warnings) {
    GenericValue product = node.getProduct()
    BigDecimal requiredQuantity = node.getQuantity()
    BigDecimal quantityOnHand = null
    BigDecimal shortageQuantity = null
    if (facilityId) {
        Map inventoryResult = runService('getInventoryAvailableByFacility', [
                productId: product.productId,
                facilityId: facilityId,
                userLogin: userLogin
        ])
        if (ServiceUtil.isError(inventoryResult)) {
            warnings.add(manufacturingMessage('ManufacturingBomSimulationAvailabilityCheckFailed',
                    [productId: product.productId, errorString: ServiceUtil.getErrorMessage(inventoryResult)]))
        } else {
            quantityOnHand = inventoryResult.quantityOnHandTotal as BigDecimal
            if (quantityOnHand != null && requiredQuantity != null) {
                shortageQuantity = requiredQuantity.subtract(quantityOnHand).max(BigDecimal.ZERO)
            }
        }
    }
    return [
            level: node.getDepth(),
            productId: product.productId,
            productName: ManufacturingServiceUtil.displayProductName(product),
            requiredQuantity: requiredQuantity,
            quantityUomId: product.quantityUomId,
            quantityOnHand: quantityOnHand,
            shortageQuantity: shortageQuantity,
            availabilityStatus: facilityId ? 'CHECKED' : 'NOT_CHECKED'
    ]
}

Map costRow(BOMNode node, String currencyUomId, List warnings, Set costSourceProductIds) {
    GenericValue product = node.getProduct()
    BigDecimal requiredQuantity = node.getQuantity()
    BigDecimal unitCost = null
    BigDecimal totalCost = null
    String costStatus = 'MISSING_COST'
    if (currencyUomId) {
        if (costSourceProductIds.contains(product.productId)) {
            Map costResult = runService('getProductCost', [
                    productId: product.productId,
                    currencyUomId: currencyUomId,
                    costComponentTypePrefix: 'EST_STD',
                    userLogin: userLogin
            ])
            if (ServiceUtil.isError(costResult)) {
                warnings.add(manufacturingMessage('ManufacturingBomSimulationCostCheckFailed',
                        [productId: product.productId, errorString: ServiceUtil.getErrorMessage(costResult)]))
            } else {
                unitCost = costResult.productCost as BigDecimal
                totalCost = unitCost != null && requiredQuantity != null ? unitCost * requiredQuantity : null
                costStatus = 'KNOWN'
            }
        }
    }
    return [
            level: node.getDepth(),
            productId: product.productId,
            productName: ManufacturingServiceUtil.displayProductName(product),
            requiredQuantity: requiredQuantity,
            unitCost: unitCost,
            totalCost: totalCost,
            costStatus: costStatus
    ]
}

Set costSourceProductIds(Collection productIds, String currencyUomId) {
    Set selectedProductIds = productIds.findAll { it } as Set
    if (!currencyUomId || !selectedProductIds) {
        return [] as Set
    }
    EntityCondition costCondition = EntityCondition.makeCondition([
            EntityCondition.makeCondition('productId', EntityOperator.IN, selectedProductIds as List),
            EntityCondition.makeCondition('costUomId', currencyUomId),
            EntityCondition.makeCondition('costComponentTypeId', EntityOperator.LIKE, 'EST_STD_%')
    ], EntityOperator.AND)
    Set productIdsWithCosts = from('CostComponent')
            .select('productId')
            .where(costCondition)
            .filterByDate()
            .distinct()
            .queryList()*.productId.findAll { it } as Set
    Set supplierProductIds = from('SupplierProduct')
            .select('productId')
            .where(EntityCondition.makeCondition([
                    EntityCondition.makeCondition('productId', EntityOperator.IN, selectedProductIds as List),
                    EntityCondition.makeCondition('currencyUomId', currencyUomId)
            ], EntityOperator.AND))
            .filterByDate('availableFromDate', 'availableThruDate')
            .distinct()
            .queryList()*.productId.findAll { it } as Set
    return productIdsWithCosts + supplierProductIds
}

String manufacturingMessage(String property, Map context = [:]) {
    return UtilProperties.getMessage('ManufacturingUiLabels', property, context, locale)
}
