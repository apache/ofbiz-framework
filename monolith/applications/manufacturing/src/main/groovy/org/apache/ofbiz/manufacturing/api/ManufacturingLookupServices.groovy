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

Map findProductLookupOptions() {
    RestQueryOptions queryOptions
    try {
        queryOptions = RestQueryOptions.fromParameters(parameters)
    } catch (IllegalArgumentException e) {
        return ServiceUtil.returnError(e.message)
    }
    List orderBy
    try {
        orderBy = productLookupOrderBy(queryOptions.sort)
    } catch (IllegalArgumentException e) {
        return ServiceUtil.returnError(e.message)
    }
    String queryText = parameters.query?.trim()
    if (UtilValidate.isEmpty(queryText)) {
        return success(RestApiUtil.getPagedResult('products', [], queryOptions, 0L,
                RestApiUtil.getRelativeRequestPath(binding)))
    }

    EntityCondition searchCondition = EntityUtil.upperLikeAny(['productId', 'productName', 'internalName'], queryText)
    EntityQuery productQuery = from('Product')
            .select('productId', 'productName', 'internalName', 'productTypeId', 'quantityUomId')
            .where(searchCondition)
    long totalCount = productQuery.queryCount()
    List productRows = productQuery.orderBy(orderBy)
            .queryPagedList(queryOptions.pageIndex, queryOptions.pageSize).getData()
    List products = productRows.collect { GenericValue product ->
        String productName = ManufacturingServiceUtil.displayProductName(product)
        [
                productId: product.productId,
                productName: productName,
                internalName: product.internalName,
                productTypeId: product.productTypeId,
                quantityUomId: product.quantityUomId,
                label: productName
        ]
    }
    return success(RestApiUtil.getPagedResult('products', products, queryOptions, totalCount,
            RestApiUtil.getRelativeRequestPath(binding)))
}

Map searchProducts() {
    return findProductLookupOptions()
}

Map searchParties() {
    RestQueryOptions queryOptions
    try {
        queryOptions = RestQueryOptions.fromParameters(parameters)
    } catch (IllegalArgumentException e) {
        return ServiceUtil.returnError(e.message)
    }
    Map filters = queryOptions.filters
    List orderBy
    try {
        orderBy = partyLookupOrderBy(queryOptions.sort)
    } catch (IllegalArgumentException e) {
        return ServiceUtil.returnError(e.message)
    }
    String queryText = filters.query?.trim()
    if (UtilValidate.isEmpty(queryText)) {
        return success(RestApiUtil.getPagedResult('parties', [], queryOptions, 0L,
                RestApiUtil.getRelativeRequestPath(binding)))
    }

    EntityCondition searchCondition = EntityUtil.upperLikeAny(['partyId', 'firstName', 'lastName', 'groupName'], queryText)
    EntityQuery partyQuery = from('PartyNameView')
            .select('partyId', 'partyTypeId', 'firstName', 'middleName', 'lastName', 'groupName')
            .where(searchCondition)
    long totalCount = partyQuery.queryCount()
    List partyRows = partyQuery.orderBy(orderBy)
            .queryPagedList(queryOptions.pageIndex, queryOptions.pageSize).getData()
    List parties = partyRows.collect { GenericValue party ->
        String partyName = PartyHelper.getPartyName(party, false) ?: party.partyId
        [
                partyId: party.partyId,
                partyTypeId: party.partyTypeId,
                partyName: partyName,
                label: partyName
        ]
    }
    return success(RestApiUtil.getPagedResult('parties', parties, queryOptions, totalCount,
            RestApiUtil.getRelativeRequestPath(binding)))
}

Map findFixedAssets() {
    RestQueryOptions queryOptions
    try {
        queryOptions = RestQueryOptions.fromParameters(parameters)
    } catch (IllegalArgumentException e) {
        return ServiceUtil.returnError(e.message)
    }
    Map filters = queryOptions.filters
    List orderBy
    try {
        orderBy = fixedAssetLookupOrderBy(queryOptions.sort)
    } catch (IllegalArgumentException e) {
        return ServiceUtil.returnError(e.message)
    }
    List conditions = []
    if (UtilValidate.isNotEmpty(filters.fixedAssetTypeId)) {
        conditions.add(EntityCondition.makeCondition('fixedAssetTypeId', filters.fixedAssetTypeId))
    }
    EntityQuery query = from('FixedAsset')
            .select('fixedAssetId', 'fixedAssetName', 'fixedAssetTypeId')
    if (conditions) {
        query.where(EntityCondition.makeCondition(conditions, EntityOperator.AND))
    }
    long totalCount = query.queryCount()
    List fixedAssets = query.orderBy(orderBy)
            .queryPagedList(queryOptions.pageIndex, queryOptions.pageSize).getData()
            .collect { GenericValue fixedAsset ->
                String fixedAssetName = ManufacturingServiceUtil.displayFixedAssetName(fixedAsset)
                [
                        fixedAssetId: fixedAsset.fixedAssetId,
                        fixedAssetName: fixedAssetName,
                        fixedAssetTypeId: fixedAsset.fixedAssetTypeId,
                        label: fixedAssetName
                ]
            }
    return success(RestApiUtil.getPagedResult('fixedAssets', fixedAssets, queryOptions, totalCount,
            RestApiUtil.getRelativeRequestPath(binding)))
}

Map findCostComponentOptions() {
    List costComponentTypes = from('CostComponentType')
            .select('costComponentTypeId', 'description')
            .orderBy('description', 'costComponentTypeId')
            .cache(true)
            .queryList()
            .collect { GenericValue type ->
                [
                        costComponentTypeId: type.costComponentTypeId,
                        description: type.description,
                        label: type.description ?: type.costComponentTypeId
                ]
            }
    List costComponentCalcs = from('CostComponentCalc')
            .select('costComponentCalcId', 'description', 'costCustomMethodId', 'fixedCost', 'variableCost', 'currencyUomId')
            .orderBy('description', 'costComponentCalcId')
            .queryList()
            .collect { GenericValue calc ->
                [
                        costComponentCalcId: calc.costComponentCalcId,
                        description: calc.description,
                        costCustomMethodId: calc.costCustomMethodId,
                        fixedCost: calc.fixedCost,
                        variableCost: calc.variableCost,
                        currencyUomId: calc.currencyUomId,
                        label: calc.description ?: calc.costComponentCalcId
                ]
            }
    return success(costComponentTypes: costComponentTypes, costComponentCalcs: costComponentCalcs)
}

Map findWarehouses() {
    return findFacilitiesByTypes(['WAREHOUSE'])
}

Map findManufacturingPlants() {
    return findFacilitiesByTypes(['PLANT'])
}

Map findManufacturingFacilities() {
    return findFacilitiesByTypes(['WAREHOUSE', 'PLANT'])
}

Map findFacilitiesByTypes(List facilityTypeIds) {
    RestQueryOptions queryOptions
    try {
        queryOptions = RestQueryOptions.fromParameters(parameters)
    } catch (IllegalArgumentException e) {
        return ServiceUtil.returnError(e.message)
    }
    List orderBy
    try {
        orderBy = facilityLookupOrderBy(queryOptions.sort)
    } catch (IllegalArgumentException e) {
        return ServiceUtil.returnError(e.message)
    }
    EntityQuery query = from('Facility')
            .select('facilityId', 'facilityName', 'facilityTypeId')
            .where(EntityCondition.makeCondition('facilityTypeId', EntityOperator.IN, facilityTypeIds))
    long totalCount = query.queryCount()
    List facilities = query.orderBy(orderBy)
            .queryPagedList(queryOptions.pageIndex, queryOptions.pageSize).getData()
            .collect { GenericValue facility ->
                String facilityId = facility.facilityId
                String facilityName = ManufacturingServiceUtil.displayFacilityName(facility)
                String label = facilityName == facilityId ? facilityId : "${facilityName} [${facilityId}]".toString()
                [
                        facilityId: facilityId,
                        facilityName: facilityName,
                        facilityTypeId: facility.facilityTypeId,
                        label: label
                ]
    }
    return success(RestApiUtil.getPagedResult('facilities', facilities, queryOptions, totalCount,
            RestApiUtil.getRelativeRequestPath(binding)))
}

List productLookupOrderBy(String sortExpression) {
    return RestApiUtil.resolveOrderBy(sortExpression, [
            productId: 'productId',
            productName: 'productName',
            internalName: 'internalName',
            productTypeId: 'productTypeId',
            quantityUomId: 'quantityUomId'
    ], ['productId'])
}

List partyLookupOrderBy(String sortExpression) {
    return RestApiUtil.resolveOrderBy(sortExpression, [
            partyId: 'partyId',
            partyName: 'groupName',
            groupName: 'groupName',
            firstName: 'firstName',
            lastName: 'lastName'
    ], ['partyId'])
}

List fixedAssetLookupOrderBy(String sortExpression) {
    return RestApiUtil.resolveOrderBy(sortExpression, [
            fixedAssetId: 'fixedAssetId',
            fixedAssetName: 'fixedAssetName',
            fixedAssetTypeId: 'fixedAssetTypeId'
    ], ['fixedAssetName', 'fixedAssetId'])
}

List facilityLookupOrderBy(String sortExpression) {
    return RestApiUtil.resolveOrderBy(sortExpression, [
            facilityId: 'facilityId',
            facilityName: 'facilityName',
            facilityTypeId: 'facilityTypeId'
    ], ['facilityName', 'facilityId'])
}
