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
package org.apache.ofbiz.manufacturing.reports

import org.apache.ofbiz.entity.GenericValue

shipmentId = parameters.shipmentId
shipment = from('Shipment').where('shipmentId', shipmentId).queryOne()

context.shipmentIdPar = shipment.shipmentId
BigDecimal fixedAssetTime = BigDecimal.ZERO
records = []
if (shipment) {
    shipmentPlans = from('OrderShipment').where('shipmentId', shipmentId).queryList()
    shipmentPlans.each { shipmentPlan ->
        productionRuns = from('WorkOrderItemFulfillment')
                .where('orderId', shipmentPlan.orderId, 'orderItemSeqId', shipmentPlan.orderItemSeqId).orderBy('workEffortId').queryList()
        if (productionRuns) {
            productionRuns.each { productionRun ->
                productionRunProduct = [:]
                productionRunProducts = from('WorkEffortGoodStandard')
                        .where('workEffortId', productionRun.workEffortId , 'workEffortGoodStdTypeId', 'PRUN_PROD_DELIV', 'statusId', 'WEGS_CREATED')
                        .queryList()
                if (productionRunProducts) {
                    productionRunProduct = ((GenericValue)productionRunProducts.get(0)).getRelatedOne('Product', false)
                }
                tasks = from('WorkEffort').where('workEffortParentId', productionRun.workEffortId, 'workEffortTypeId', 'PROD_ORDER_TASK').queryList()
                tasks.each { task ->
                    record = [
                            productId: productionRunProduct.productId,
                            productName: productionRunProduct.internalName,
                            fixedAssetId: task.fixedAssetId,
                            priority: task.getLong('priority'),
                            workEffortId: productionRun.workEffortId,
                            taskId: task.workEffortId,
                            taskName: task.workEffortName,
                            taskDescription: task.description,
                            taskEstimatedTime: task.getDouble('estimatedMilliSeconds'),
                            taskEstimatedSetup: task.getDouble('estimatedSetupMillis'),
                    ]
                    records.add(record)
                    if (task.getDouble('estimatedMilliSeconds') != null) {
                        fixedAssetTime = fixedAssetTime + task.getDouble('estimatedMilliSeconds')
                    }
                }
            }
        }
    }
    context.fixedAssetTime = fixedAssetTime
    context.records = records

    // check permission
    hasPermission = false
    if (security.hasEntityPermission('MANUFACTURING', '_VIEW', session)) {
        hasPermission = true
    }
    context.hasPermission = hasPermission
}

return 'success'
