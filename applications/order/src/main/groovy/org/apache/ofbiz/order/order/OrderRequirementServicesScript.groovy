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
package org.apache.ofbiz.order.order

import org.apache.ofbiz.base.util.Debug
import org.apache.ofbiz.base.util.UtilProperties
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.entity.util.EntityTypeUtil
import org.apache.ofbiz.service.ServiceUtil

/*
 * Create OrderRequirementCommitment and Requirement for items with automatic requirement upon ordering
 */
Map checkCreateOrderRequirement() {
    Map reqMap = getProductRequirementMethod()
    GenericValue order = reqMap.order
    if (order.orderTypeId == 'SALES_ORDER' && reqMap.requirementMethodId == 'PRODRQM_AUTO') {
        createRequirementAndCommitment()
    }
    return success()
}

Map getProductRequirementMethod(String productId) {
    GenericValue order = from('OrderHeader').where(parameters).queryOne()
    GenericValue product = from('Product').where(productId: productId).queryOne()
    String requirementMethodId = product ? product.requirementMethodEnumId : ''
    if (!requirementMethodId && product) {
        boolean isMarketingPkg = EntityTypeUtil.hasParentType(delegator, 'ProductType', 'productTypeId',
                                                              product.productTypeId, 'parentTypeId', 'MARKETING_PKG')
        if (!isMarketingPkg && product.productTypeId != 'DIGITAL_GOOD' && order) {
            GenericValue productStore = from('ProductStore').where(productStoreId: order.productStoreId).queryOne()
            requirementMethodId = productStore ? productStore.requirementMethodEnumId : ''
        }
    }
    return [order: order, requirementMethodId: requirementMethodId]
}

/*
 * create a requirement and commitment for it
 */
Map createRequirementAndCommitment() {
    Map createRequirement = [requirementTypeId: 'PRODUCT_REQUIREMENT']
    Map returnMap = success()

    GenericValue order = from('OrderHeader').where(orderId: orderId).queryOne()
    if (order) {
        GenericValue productStore = from('ProductStore').where(productStoreId: order.productStoreId).queryOne()
        if (productStore.inventoryFacilityId) {
            createRequirement.facilityId = productStore.inventoryFacilityId
        }
        Map result = run service: 'createRequirement', with: createRequirement
        returnMap.requirementId = result.requirementId
        // create the OrderRequirementCommitment to record the Requirement created for an order item

        run service: 'createOrderRequirementCommitment', with: [*:parameters, requirementId: result.requirementId]
    }
    return returnMap
}

/*
Stock Requirement
*/

Map checkCreateStockRequirementQoh() {
    checkCreateStockRequirement('PRODRQM_STOCK')
}

Map checkCreateStockRequirementAtp() {
    checkCreateStockRequirement('PRODRQM_STOCK_ATP')
}

Map checkCreateStockRequirement(String methodId) {
    if (!(security.hasEntityPermission('ORDERMGR', '_CREATE', parameters.userLogin))) {
        return error(UtilProperties.getMessage('OrderErrorUiLabels', 'OrderSecurityErrorToRunCheckCreateStockRequirement', parameters.locale))
    }
    Map resultMap = success()

    String requirementMethodId
    String productId
    String facilityId
    GenericValue inventoryItem
    BigDecimal quantity = parameters.quantity ?: 0

    // If the service is triggered by the updateItemIssuance service, get the ItemIssuance by the passed itemIssuanceId
    Map inventoryItemAndRequirementMethodId = getStockRequirementMethod()
    requirementMethodId = inventoryItemAndRequirementMethodId.requirementMethodId
    inventoryItem = inventoryItemAndRequirementMethodId.inventoryItem
    if (inventoryItem) {
        productId = inventoryItem.productId
        facilityId = inventoryItem.facilityId
    }

    if (requirementMethodId && methodId == requirementMethodId && inventoryItem) {
        Map result = getProductFacilityAndQuantities(productId, facilityId)
        boolean createRequirement = false
        if (result.productFacility) {
            GenericValue productFacility = result.productFacility
            BigDecimal minimumStock = productFacility.getBigDecimal('minimumStock')
            switch (methodId) {
                case 'PRODRQM_STOCK': //qoh
                    //No requirements are created if we are already under stock
                    BigDecimal quantityOnHandTotal = result.quantityOnHandTotal
                    if (minimumStock && quantityOnHandTotal >= minimumStock) {
                        BigDecimal newQuantityOnHand = quantityOnHandTotal.subtract(quantity)
                        /*If this new issuance will cause the quantityOnHandTotal to go below the minimumStock,
                        create a new requirement */
                        if (newQuantityOnHand < productFacility.minimumStock) {
                            createRequirement = true
                        }
                    }
                    break
                case 'PRODRQM_STOCK_ATP': // atp
                    /* No requirements are created if we are not under stock
                    this service is supposed to be called after inventory is reserved,
                    so inventory should have been updated already */
                    BigDecimal availableToPromiseTotal = result.availableToPromiseTotal
                    if (minimumStock && availableToPromiseTotal < minimumStock) {
                        BigDecimal oldAvailableToPromiseTotal = availableToPromiseTotal.add(quantity)
                        /* If before this reservation the availableToPromiseTotal was over minimumStock,
                        create a new requirement*/
                        if (oldAvailableToPromiseTotal >= minimumStock) {
                            createRequirement = true
                        }
                    }
                    break
            }
            if (createRequirement) {
                BigDecimal reqQuantity = productFacility.reorderQuantity ?: (quantity ?: 0)
                Map inputMap = [
                        productId: productId,
                        facilityId: facilityId,
                        quantity: reqQuantity,
                        requirementTypeId: 'PRODUCT_REQUIREMENT'
                ]
                result = run service: 'createRequirement', with: inputMap
                if (ServiceUtil.isError(result)) {
                    return result
                }
                resultMap.requirementId = result.requirementId
            }
        }
    }
    return resultMap
}

Map checkCreateProductRequirementForFacility() {
    if (!(security.hasEntityPermission('ORDERMGR', '_CREATE', parameters.userLogin))) {
        return error(UtilProperties.getMessage('OrderErrorUiLabels', 'OrderSecurityErrorToRunCheckCreateStockRequirement', parameters.locale))
    }
    Map resultMap = success()

    List<GenericValue> products = from('ProductFacility').where([facilityId: parameters.facilityId]).queryList()
    for (GenericValue productFacility : products) {
        String requirementMethodId = getProductRequirementMethod(productFacility.productId).requirementMethodId
        requirementMethodId = requirementMethodId ?: parameters.defaultRequirementMethodId
        if (requirementMethodId) {
            Map result = getProductFacilityAndQuantities(productFacility.productId, productFacility.facilityId)
            BigDecimal currentQuantity = requirementMethodId == 'PRODRQM_STOCK' ? result.quantityOnHandTotal : result.availableToPromiseTotal
            BigDecimal minimumStock = productFacility.getBigDecimal('minimumStock')
            if (minimumStock && currentQuantity < minimumStock) {
                BigDecimal reqQuantity = productFacility.reorderQuantity ?:0
                BigDecimal quantityShortfall = minimumStock.subtract(currentQuantity)
                if (reqQuantity < quantityShortfall) {
                    reqQuantity = quantityShortfall
                }
                Map inputMap = [productId: productFacility.productId,
                                facilityId: productFacility.facilityId,
                                quantity: reqQuantity,
                                requirementTypeId: 'PRODUCT_REQUIREMENT'
                    ]
                result = run service: 'createRequirement', with: inputMap
                if (ServiceUtil.isError(result)) {
                    return result
                }
                Debug.logInfo("Requirement creted with id [${result.requirementId}] for product with id [${productFacility.productId}]",
                     'OrderRequirementServiceScript')
            }
        }
    }
    return resultMap
}

private Map getStockRequirementMethod() {
    GenericValue inventoryItem = null
    if (parameters.itemIssuanceId) {
        GenericValue itemIssuance = from('ItemIssuance')
            .where([itemIssuanceId: parameters.itemIssuanceId])
            .queryOne()
        inventoryItem = itemIssuance ? itemIssuance.getRelatedOne('InventoryItem', true) : null
    } else {
        inventoryItem = from('InventoryItem').where(parameters).queryOne()
    }
    if (!inventoryItem) {
        return [requirementMethodId: null, inventoryItem: null]
    }
    String requirementMethodId = getProductRequirementMethod(inventoryItem.productId).requirementMethodId
    return [requirementMethodId: requirementMethodId, inventoryItem: inventoryItem]
}

private Map getProductFacilityAndQuantities(String productId, String facilityId) {
    // Get the ProductFacility for the minimum stock level
    GenericValue productFacility = from('ProductFacility')
            .where([productId: productId,
                    facilityId: facilityId])
            .queryOne()
    // Get the product's total quantityOnHand in the facility
    Map resultMap = run service: 'getInventoryAvailableByFacility', with: [
            *: parameters,
            productId: productId,
            facilityId: facilityId
    ]
    if (ServiceUtil.isError(resultMap)) {
        return resultMap
    }
    resultMap.productFacility = productFacility
    return resultMap
}
