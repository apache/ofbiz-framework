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
    String queryText = parameters.query?.trim()
    if (UtilValidate.isEmpty(queryText)) {
        return success(RestApiUtil.getPagedResult('products', [], queryOptions, 0L, null))
    }

    EntityCondition searchCondition = EntityUtil.upperLikeAny(['productId', 'productName', 'internalName'], queryText)
    EntityQuery productQuery = from('Product')
            .select('productId', 'productName', 'internalName', 'productTypeId', 'quantityUomId')
            .where(searchCondition)
    long totalCount = productQuery.queryCount()
    List productRows = productQuery.orderBy('productId')
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
    return success(RestApiUtil.getPagedResult('products', products, queryOptions, totalCount, null))
}

Map findWarehouses() {
    return findFacilitiesByTypes(['WAREHOUSE', 'PLANT'])
}

Map findFacilitiesByTypes(List facilityTypeIds) {
    RestQueryOptions queryOptions
    try {
        queryOptions = RestQueryOptions.fromParameters(parameters)
    } catch (IllegalArgumentException e) {
        return ServiceUtil.returnError(e.message)
    }
    EntityQuery query = from('Facility')
            .select('facilityId', 'facilityName', 'facilityTypeId')
            .where(EntityCondition.makeCondition('facilityTypeId', EntityOperator.IN, facilityTypeIds))
    long totalCount = query.queryCount()
    List facilities = query.orderBy('facilityName', 'facilityId')
            .queryPagedList(queryOptions.pageIndex, queryOptions.pageSize).getData()
            .collect { GenericValue facility ->
                String facilityId = facility.facilityId
                String facilityName = facility.facilityName ?: facilityId
                String label = facilityName == facilityId ? facilityId : "${facilityName} [${facilityId}]".toString()
                [
                        facilityId: facilityId,
                        facilityName: facilityName,
                        facilityTypeId: facility.facilityTypeId,
                        label: label
                ]
    }
    return success(RestApiUtil.getPagedResult('facilities', facilities, queryOptions, totalCount, null))
}
