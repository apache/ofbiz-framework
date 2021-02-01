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


import org.apache.ofbiz.entity.GenericValue

/*
 * Create OrderRequirementCommitment and Requirement for items with automatic requirement upon ordering
 */
def checkCreateOrderRequirement() {
    def reqMap = getProductRequirementMethod()
    GenericValue order = reqMap.order
    if (order.orderTypeId == 'SALES_ORDER' && reqMap.requirementMethodId == 'PRODRQM_AUTO') {
        createRequirementAndCommitment()
    }
    success()
}

def getProductRequirementMethod() {
    GenericValue order = from('OrderHeader').where(parameters).queryOne()
    GenericValue product = from('Product').where(parameters).queryOne()
    String requirementMethodId = product ? product.requirementMethodId : ''
    if (!requirementMethodId && product) {
        boolean isMarketingPkg = EntityTypeUtil.hasParentType(delegator, 'ProductType', 'productTypeId',
                                                              product.productTypeId, 'parentTypeId', 'MARKETING_PKG')
        if (!isMarketingPkg && product.productTypeId != 'DIGITAL_GOOD' && order) {
            productStore = from('ProductStore').where(productStoreId: order.productStoreId).queryOne()
            requirementMethodId = productStore ? productStore.requirementMethodEnumId : ''
        }
    }
    return [order: order, requirementMethodId: requirementMethodId]

}

/*
 * create a requirement and commitment for it
 */
def createRequirementAndCommitment() {
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

