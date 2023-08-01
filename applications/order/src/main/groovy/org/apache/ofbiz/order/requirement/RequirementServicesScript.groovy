/*
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
*/
package org.apache.ofbiz.order.requirement

import org.apache.ofbiz.base.util.UtilDateTime
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.entity.condition.EntityCondition
import org.apache.ofbiz.entity.condition.EntityConditionBuilder
import org.apache.ofbiz.entity.util.EntityQuery

import java.sql.Timestamp

/**
 * Delete a requirement after deleting related entity records
 */
Map deleteRequirementAndRelated() {
    GenericValue requirement = from('Requirement').where(parameters).queryOne()
    if (requirement) {
        requirement.removeRelated('RequirementAttribute')
        requirement.removeRelated('RequirementRole')
        requirement.removeRelated('RequirementStatus')
        requirement.removeRelated('RequirementCustRequest')
        requirement.remove()
        return success()
    }
    return error('Entity value not found with name: requirement Method = deleteRequirementAndRelated')
}

/**
 * If the requirement is a product requirement (purchasing) try to assign it to the primary supplier
 */
Map autoAssignRequirementToSupplier() {
    GenericValue requirement = from('Requirement').where(parameters).queryOne()
    if (requirement) {
        if (requirement.requirementTypeId == 'PRODUCT_REQUIREMENT'
                && requirement.productId
                && requirement.quantity) {
            EntityCondition condition = new EntityConditionBuilder().AND {
                EQUALS(productId: requirement.productId)
                LESS_THAN_EQUAL_TO(minimumOrderQuantity: requirement.quantity)
            }
            EntityQuery supplierProductsQuery = from('SupplierProduct').where(condition).orderBy('lastPrice', 'supplierPrefOrderId')
            if (requirement.requiredByDate) {
                supplierProductsQuery.filterByDate((Timestamp) requirement.requiredByDate, 'availableFromDate', 'availableThruDate')
            }
            GenericValue supplierProduct = supplierProductsQuery.queryFirst()
            if (supplierProduct?.partyId) {
                delegator.createOrStore('RequirementRole', [requirementId: requirement.requirementId,
                                                            partyId: supplierProduct.partyId,
                                                            roleTypeId: 'SUPPLIER',
                                                            fromDate: UtilDateTime.nowTimestamp()])
            }
        }
        return success()
    }
    return error('Entity value not found with name: requirement Method = autoAssignRequirementToSupplier')
}

/**
 * Create the inventory transfers required to fulfill the requirement
 */
Map createTransferFromRequirement() {
    GenericValue requirement = from('Requirement').where(parameters).queryOne()
    if (!requirement) {
        return error('Entity value not found with name: requirement Method = createTransferFromRequirement')
    }
    try {
        Map serviceResult = run service: 'createInventoryTransfersForProduct',
                with: [productId: requirement.productId,
                       facilityId: parameters.fromFacilityId,
                       facilityIdTo: requirement.facilityId,
                       quantity: requirement.quantity,
                       sendDate: requirement.requiredByDate]
        BigDecimal quantityNotTransferred = serviceResult.quantityNotTransferred
        if (quantityNotTransferred > 0) {
            // we create a new requirement for the quantity not transferred (because not available)
            run service: 'createRequirement', with: [*: requirement.getAllFields(),
                                                     quantity: quantityNotTransferred]
        }
        run service: 'updateRequirement', with: [requirementId: requirement.requirementId,
                                                 statusId: 'REQ_ORDERED']
    } catch (Exception e) {
        return error('Failed to create the requirement with ' + e)
    }
    return success()
}

