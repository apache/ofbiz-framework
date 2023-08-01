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

// The only required parameter is "productionRunId".
// The "actionForm" parameter triggers actions (see "ProductionRunSimpleEvents.xml").

import org.apache.ofbiz.entity.util.EntityUtil
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.manufacturing.jobshopmgt.ProductionRun

productionRunId = parameters.productionRunId ?: parameters.workEffortId
if (productionRunId) {
    ProductionRun productionRun = new ProductionRun(productionRunId, delegator, dispatcher)
    if (productionRun.exist()) {
        productionRunId = productionRun.getGenericValue().workEffortId
        context.productionRunId = productionRunId
        context.productionRun = productionRun.getGenericValue()

        // Find all the order items to which this production run is linked.
        orderItems = from('WorkOrderItemFulfillment').where('workEffortId', productionRunId).queryList()
        if (orderItems) {
            context.orderItems = orderItems
        }

        quantityToProduce = productionRun.getGenericValue().get('quantityToProduce') ?: 0.0

        // Find the inventory items produced
        inventoryItems = from('WorkEffortInventoryProduced').where('workEffortId', productionRunId).queryList()
        context.inventoryItems = inventoryItems
        if (inventoryItems) {
            lastWorkEffortInventoryProduced = (GenericValue)inventoryItems.last()
            lastInventoryItem = lastWorkEffortInventoryProduced.getRelatedOne('InventoryItem', false)
            context.lastLotId = lastInventoryItem.lotId
        }

        // Find if the production run can produce inventory.
        quantityProduced = productionRun.getGenericValue().quantityProduced ?: 0.0
        quantityRejected = productionRun.getGenericValue().quantityRejected ?: 0.0

        lastTask = productionRun.getLastProductionRunRoutingTask()
        quantityDeclared = lastTask ? (lastTask.quantityProduced ?: 0.0) : 0.0

        context.canDeclareAndProduce = 'N'

        if (lastTask && (lastTask.currentStatusId == 'PRUN_RUNNING' || lastTask.currentStatusId == 'PRUN_COMPLETED')) {
            context.canDeclareAndProduce = 'Y'
        }
        maxQuantity = quantityDeclared - quantityProduced

        productionRunData = [
                workEffortId: productionRunId,
                productId: productionRun.getProductProduced().productId,
                product: productionRun.getProductProduced(),
                quantityToProduce: quantityToProduce,
                quantityProduced: quantityProduced,
                quantityRejected: quantityRejected,
                quantityRemaining: quantityToProduce - quantityProduced,
                estimatedCompletionDate: productionRun.getEstimatedCompletionDate(),
                productionRunName: productionRun.getProductionRunName(),
                description: productionRun.getDescription(),
                estimatedStartDate: productionRun.getEstimatedStartDate(),
                actualStartDate: productionRun.getGenericValue().getTimestamp('actualStartDate'),
                actualCompletionDate: productionRun.getGenericValue().getTimestamp('actualCompletionDate'),
                currentStatusId: productionRun.getGenericValue().currentStatusId,
                facilityId: productionRun.getGenericValue().facilityId
        ]
        if (maxQuantity > 0 && 'WIP' != productionRun.getProductProduced().productTypeId) {
            productionRunData.quantity = maxQuantity
            context.canProduce = 'Y'
        }

        manufacturer = from('WorkEffortPartyAssignment')
                .where('workEffortId', productionRunId, 'roleTypeId', 'MANUFACTURER').filterByDate().queryFirst()
        if (manufacturer) {
            productionRunData.manufacturerId = manufacturer.partyId
        }
        context.productionRunData = productionRunData

        actionForm = parameters.actionForm ?: 'beforeActionProductionRun'
        context.actionForm = actionForm
        //---------------
        // Routing tasks
        //---------------
        // routingTask update sub-screen
        routingTaskId = parameters.routingTaskId
        if (routingTaskId && (actionForm == 'UpdateRoutingTask' || actionForm == 'EditRoutingTask')) {
            routingTask = from('WorkEffort').where('workEffortId', routingTaskId).queryOne()
            Map routingTaskData = routingTask.getAllFields()
            routingTaskData.estimatedSetupMillis = routingTask.getDouble('estimatedSetupMillis')
            routingTaskData.estimatedMilliSeconds = routingTask.getDouble('estimatedMilliSeconds')
            context.routingTaskData = routingTaskData
            routingTaskData.partyId = userLogin.partyId
            context.routingTaskId = routingTaskId
            // Get the list of deliverable products, i.e. the WorkEffortGoodStandard entries
            // with workEffortGoodStdTypeId = "PRUNT_PROD_DELIV":
            // first of all we get the template task (the routing task)
            templateTaskAssoc = from('WorkEffortAssoc')
                    .where('workEffortIdTo', routingTask.workEffortId, 'workEffortAssocTypeId', 'WORK_EFF_TEMPLATE').filterByDate().queryFirst()
            templateTask = [:]
            if (templateTaskAssoc) {
                templateTask = templateTaskAssoc.getRelatedOne('FromWorkEffort', false)
            }
            delivProducts = []
            if (templateTask) {
                delivProducts = EntityUtil.filterByDate(templateTask.getRelated('WorkEffortGoodStandard',
                        [workEffortGoodStdTypeId: 'PRUNT_PROD_DELIV'], null, false))
            }
            context.delivProducts = delivProducts
            // Get the list of delivered products, i.e. inventory items
            prunInventoryProduced = from('WorkEffortAndInventoryProduced').where('workEffortId', routingTaskId).queryList()
            context.prunInventoryProduced = prunInventoryProduced
        }

        //  RoutingTasks list
        List productionRunRoutingTasks = productionRun.getProductionRunRoutingTasks()
        String startTaskId = null // which production run task is ready to start and has the [Start] buton next to it
        String issueTaskId = null // which production run task is ready to have products issued with [Issue Components] button
        String completeTaskId = null // which task has the [Complete] button next to it
        productionRunRoutingTasks.each { task ->
            // only PRUN_RUNNING tasks can have items issued or production run completed
            if (task.currentStatusId == 'PRUN_RUNNING') {
                // Use WorkEffortGoodStandard to figure out if there are products which are needed for this task (PRUNT_PRODNEEDED)
                // and which have not been issued (ie, WEGS_CREATED).
                // If so this task should have products issued
                components = from('WorkEffortGoodStandard')
                        .where('workEffortId', task.workEffortId, 'workEffortGoodStdTypeId', 'PRUNT_PROD_NEEDED', 'statusId', 'WEGS_CREATED')
                        .queryList()
                if (components) {
                    issueTaskId = task.workEffortId
                }
                if (!issueTaskId) {
                    completeTaskId = task.workEffortId
                }
            }

            // the first CREATED and SCHEDULED task will be the startTaskId.
            // As the issue and complete tasks are filled out this condition will no longer be true
            if (!startTaskId &&
                  !issueTaskId &&
                  !completeTaskId &&
                  (task.currentStatusId == 'PRUN_CREATED' ||
                          task.currentStatusId == 'PRUN_SCHEDULED' ||
                          task.currentStatusId == 'PRUN_DOC_PRINTED')) {
                startTaskId = task.workEffortId
            }
        }
        context.productionRunRoutingTasks = productionRunRoutingTasks
        context.startTaskId = startTaskId ?: 'null'
        context.issueTaskId = issueTaskId ?: 'null'
        context.completeTaskId = completeTaskId ?: 'null'

        //  Product components list
        productionRunComponents = productionRun.getProductionRunComponents()
        productionRunComponentsData = []
        productionRunComponentsDataReadyForIssuance = []
        productionRunComponentsAlreadyIssued = []
        if (productionRunComponents) {
            productionRunComponents.each { component ->
                product = component.getRelatedOne('Product', false)
                componentName = product.getString('internalName')
                productionRunTask = component.getRelatedOne('WorkEffort', false)
                workEffortName = productionRunTask.getString('workEffortName')
                Map componentData = component.getAllFields()
                componentData.internalName = componentName
                componentData.workEffortName = workEffortName
                componentData.facilityId = productionRunTask.facilityId
                issuances = from('WorkEffortAndInventoryAssign')
                        .where('workEffortId', component.workEffortId, 'productId', product.productId).queryList()
                totalIssued = 0.0
                issuances.each { issuance ->
                    issued = issuance.quantity
                    if (issued) {
                        totalIssued += issued
                    }
                }
                returns = from('WorkEffortAndInventoryProduced')
                        .where('workEffortId', component.workEffortId , 'productId', product.productId).queryList()
                totalReturned = 0.0
                returns.each { returned ->
                    returnDetail = from('InventoryItemDetail')
                            .where('inventoryItemId', returned.inventoryItemId).orderBy('inventoryItemDetailSeqId').queryFirst()
                    if (returnDetail) {
                        qtyReturned = returnDetail.quantityOnHandDiff
                        if (qtyReturned) {
                            totalReturned += qtyReturned
                        }
                    }
                }
                componentData.issuedQuantity = totalIssued
                componentData.returnedQuantity = totalReturned
                componentData.currentStatusId = productionRunTask.currentStatusId
                if (productionRunTask.currentStatusId == 'PRUN_RUNNING') {
                    componentData.isRunning = 'Y'
                } else {
                    componentData.isRunning = 'null'
                }
                productionRunComponentsData.add(componentData)
                if (productionRunTask.currentStatusId == 'PRUN_RUNNING' && component.getString('statusId') == 'WEGS_CREATED') {
                    productionRunComponentsDataReadyForIssuance.add(componentData)
                } else if (totalIssued > 0.0) {
                    productionRunComponentsAlreadyIssued.add(componentData)
                }
            }
        }
        // Content
        productionRunContents = from('WorkEffortContentAndInfo')
                .where('workEffortId', productionRunId).orderBy('-fromDate').filterByDate().queryList()
        context.productionRunContents = productionRunContents
        context.productionRunComponents = productionRunComponents
        context.productionRunComponentsData = productionRunComponentsData
        context.productionRunComponentsDataReadyForIssuance = productionRunComponentsDataReadyForIssuance
        context.productionRunComponentsAlreadyIssued = productionRunComponentsAlreadyIssued
    }
}
