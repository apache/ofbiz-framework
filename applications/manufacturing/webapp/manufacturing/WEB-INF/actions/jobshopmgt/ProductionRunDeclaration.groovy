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

// The only required parameter is "productionRunId".
// The "actionForm" parameter triggers actions (see "ProductionRunSimpleEvents.xml").

import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.widget.html.HtmlFormWrapper;
import org.ofbiz.manufacturing.jobshopmgt.ProductionRun;

import javolution.util.FastList;

productionRunId = parameters.productionRunId ?: parameters.workEffortId;
if (productionRunId) {
    ProductionRun productionRun = new ProductionRun(productionRunId, delegator, dispatcher);
    if (productionRun.exist()) {
        productionRunId = productionRun.getGenericValue().workEffortId;
        context.productionRunId = productionRunId;
        context.productionRun = productionRun.getGenericValue();

        // Find all the order items to which this production run is linked.
        orderItems = delegator.findByAnd("WorkOrderItemFulfillment", [workEffortId : productionRunId]);
        if (orderItems) {
            context.orderItems = orderItems;
        }

        quantityToProduce = productionRun.getGenericValue().get("quantityToProduce") ?: 0.0;

        // Find the inventory items produced
        inventoryItems = delegator.findByAnd("WorkEffortInventoryProduced", [workEffortId : productionRunId]);
        context.inventoryItems = inventoryItems;
        if (inventoryItems) {
            lastWorkEffortInventoryProduced = (GenericValue)inventoryItems.get(inventoryItems.size() - 1);
            lastInventoryItem = lastWorkEffortInventoryProduced.getRelatedOne("InventoryItem");
            context.lastLotId = lastInventoryItem.lotId;
        }

        // Find if the production run can produce inventory.
        quantityProduced = productionRun.getGenericValue().quantityProduced ?: 0.0;
        quantityRejected = productionRun.getGenericValue().quantityRejected ?: 0.0;

        lastTask = productionRun.getLastProductionRunRoutingTask();
        quantityDeclared = lastTask ? (lastTask.quantityProduced ?: 0.0) : 0.0 ;

        if (lastTask && ("PRUN_RUNNING".equals(lastTask.currentStatusId) || "PRUN_COMPLETED".equals(lastTask.currentStatusId))) {
            context.canDeclareAndProduce = "Y";
        }
        maxQuantity = quantityDeclared - quantityProduced;

        productionRunData = [:];
        productionRunData.workEffortId = productionRunId;
        productionRunData.productId = productionRun.getProductProduced().productId;
        productionRunData.product = productionRun.getProductProduced();
        if (maxQuantity > 0 && !"WIP".equals(productionRun.getProductProduced().productTypeId)) {
            productionRunData.quantity = maxQuantity;
            context.canProduce = "Y";
        }
        productionRunData.quantityToProduce = quantityToProduce;
        productionRunData.quantityProduced = quantityProduced;
        productionRunData.quantityRejected = quantityRejected;
        productionRunData.quantityRemaining = quantityToProduce - quantityProduced;
        productionRunData.estimatedCompletionDate = productionRun.getEstimatedCompletionDate();
        productionRunData.productionRunName = productionRun.getProductionRunName();
        productionRunData.description = productionRun.getDescription();
        productionRunData.estimatedStartDate = productionRun.getEstimatedStartDate();
        productionRunData.actualStartDate = productionRun.getGenericValue().getTimestamp("actualStartDate");
        productionRunData.actualCompletionDate = productionRun.getGenericValue().getTimestamp("actualCompletionDate");
        productionRunData.currentStatusId = productionRun.getGenericValue().currentStatusId;

        context.productionRunData = productionRunData;

        actionForm = parameters.actionForm ?: "beforeActionProductionRun";
        context.actionForm = actionForm;
        //---------------
        // Routing tasks
        //---------------
        // routingTask update sub-screen
        routingTaskId = parameters.routingTaskId;
        if (routingTaskId && (actionForm.equals("UpdateRoutingTask") || actionForm.equals("EditRoutingTask"))) {
            routingTask = delegator.findByPrimaryKey("WorkEffort", [workEffortId : routingTaskId]);
            Map routingTaskData = routingTask.getAllFields();
            routingTaskData.estimatedSetupMillis = routingTask.getDouble("estimatedSetupMillis");
            routingTaskData.estimatedMilliSeconds = routingTask.getDouble("estimatedMilliSeconds");
            HtmlFormWrapper editPrRoutingTaskWrapper = new HtmlFormWrapper("component://manufacturing/webapp/manufacturing/jobshopmgt/ProductionRunForms.xml", "EditProductionRunDeclRoutingTask", request, response);
            editPrRoutingTaskWrapper.putInContext("routingTaskData", routingTaskData);
            editPrRoutingTaskWrapper.putInContext("actionForm", "UpdateRoutingTask");
            routingTaskData.partyId = userLogin.partyId;
            context.editPrRoutingTaskWrapper = editPrRoutingTaskWrapper;
            context.routingTaskId = routingTaskId;
            // Get the list of deliverable products, i.e. the WorkEffortGoodStandard entries
            // with workEffortGoodStdTypeId = "PRUNT_PROD_DELIV":
            // first of all we get the template task (the routing task)
            templateTaskAssoc = EntityUtil.getFirst(EntityUtil.filterByDate(delegator.findByAnd("WorkEffortAssoc", [workEffortIdTo : routingTask.workEffortId, workEffortAssocTypeId : "WORK_EFF_TEMPLATE"])));
            templateTask = [:];
            if (templateTaskAssoc) {
                templateTask = templateTaskAssoc.getRelatedOne("FromWorkEffort");
            }
            delivProducts = [];
            if (templateTask) {
                delivProducts = EntityUtil.filterByDate(templateTask.getRelatedByAnd("WorkEffortGoodStandard", [workEffortGoodStdTypeId : "PRUNT_PROD_DELIV"]));
            }
            HtmlFormWrapper createRoutingTaskDelivProductForm = new HtmlFormWrapper("component://manufacturing/webapp/manufacturing/jobshopmgt/ProductionRunForms.xml", "CreateRoutingTaskDelivProduct", request, response);
            createRoutingTaskDelivProductForm.putInContext("formData", [productionRunId : productionRunId, workEffortId : routingTaskId]);
            context.createRoutingTaskDelivProductForm = createRoutingTaskDelivProductForm;
            context.delivProducts = delivProducts;
            // Get the list of delivered products, i.e. inventory items
            prunInventoryProduced = delegator.findByAnd("WorkEffortAndInventoryProduced", [workEffortId : routingTaskId]);
            context.prunInventoryProduced = prunInventoryProduced;
            HtmlFormWrapper prunInventoryProducedForm = new HtmlFormWrapper("component://manufacturing/webapp/manufacturing/jobshopmgt/ProductionRunForms.xml", "ProductionRunTaskInventoryProducedList", request, response);
            prunInventoryProducedForm.putInContext("prunInventoryProduced", prunInventoryProduced);
            context.prunInventoryProducedForm = prunInventoryProducedForm;
        }

        //  RoutingTasks list
        List productionRunRoutingTasks = productionRun.getProductionRunRoutingTasks();
        String startTaskId = null;   // which production run task is ready to start and has the [Start] buton next to it
        String issueTaskId = null;   // which production run task is ready to have products issued with [Issue Components] button
        String completeTaskId = null;   // which task has the [Complete] button next to it
        productionRunRoutingTasks.each { task ->
            // only PRUN_RUNNING tasks can have items issued or production run completed
            if ("PRUN_RUNNING".equals(task.currentStatusId)) {
                // Use WorkEffortGoodStandard to figure out if there are products which are needed for this task (PRUNT_PRODNEEDED) and which have not been issued (ie, WEGS_CREATED).
                // If so this task should have products issued
                components = delegator.findByAnd("WorkEffortGoodStandard", [workEffortId : task.workEffortId, workEffortGoodStdTypeId : "PRUNT_PROD_NEEDED"]);
                List notIssued = EntityUtil.filterByAnd(components, [statusId : "WEGS_CREATED"]);
                if (components && notIssued) {
                    issueTaskId = task.workEffortId;
                }
                if (!issueTaskId) {
                    completeTaskId = task.workEffortId;
                }
            }

            // the first CREATED and SCHEDULED task will be the startTaskId.  As the issue and complete tasks are filled out this condition will no longer be true
            if (!startTaskId &&
                  !issueTaskId &&
                  !completeTaskId &&
                  ("PRUN_CREATED".equals(task.currentStatusId) ||
                   "PRUN_SCHEDULED".equals(task.currentStatusId) ||
                   "PRUN_DOC_PRINTED".equals(task.currentStatusId))) {
                startTaskId = task.workEffortId;
            }
        }
        context.productionRunRoutingTasks = productionRunRoutingTasks;
        context.startTaskId = (startTaskId ? startTaskId: "null");
        context.issueTaskId = (issueTaskId? issueTaskId: "null");
        context.completeTaskId = (completeTaskId != null? completeTaskId: "null");

        //  Product components list
        productionRunComponents = productionRun.getProductionRunComponents();
        productionRunComponentsData = FastList.newInstance();
        productionRunComponentsDataReadyForIssuance = FastList.newInstance();
        productionRunComponentsAlreadyIssued = FastList.newInstance();
        if (productionRunComponents) {
            productionRunComponents.each { component ->
                product = component.getRelatedOne("Product");
                componentName = product.getString("internalName");
                productionRunTask = component.getRelatedOne("WorkEffort");
                workEffortName = productionRunTask.getString("workEffortName");
                Map componentData = component.getAllFields();
                componentData.internalName = componentName;
                componentData.workEffortName = workEffortName;
                componentData.facilityId = productionRunTask.facilityId;
                issuances = delegator.findByAnd("WorkEffortAndInventoryAssign", [workEffortId : component.workEffortId, productId : product.productId]);
                totalIssued = 0.0;
                issuances.each { issuance ->
                    issued = issuance.quantity;
                    if (issued) {
                        totalIssued += issued;
                    }
                }
                returns = delegator.findByAnd("WorkEffortAndInventoryProduced", [workEffortId : component.workEffortId , productId : product.productId]);
                totalReturned = 0.0;
                returns.each { returned ->
                    returnDetail = EntityUtil.getFirst(delegator.findByAnd("InventoryItemDetail", [inventoryItemId : returned.inventoryItemId], ["inventoryItemDetailSeqId"]));
                    if (returnDetail) {
                        qtyReturned = returnDetail.quantityOnHandDiff;
                        if (qtyReturned) {
                            totalReturned += qtyReturned;
                        }
                    }
                }
                componentData.issuedQuantity = totalIssued;
                componentData.returnedQuantity = totalReturned;
                componentData.currentStatusId = productionRunTask.currentStatusId;
                if ("PRUN_RUNNING".equals(productionRunTask.currentStatusId)) {
                    componentData.isRunning = "Y";
                } else {
                    componentData.isRunning = "null";
                }
                productionRunComponentsData.add(componentData);
                if ("PRUN_RUNNING".equals(productionRunTask.currentStatusId) && "WEGS_CREATED".equals(component.getString("statusId"))) {
                    productionRunComponentsDataReadyForIssuance.add(componentData);
                } else if (totalIssued > 0.0) {
                    productionRunComponentsAlreadyIssued.add(componentData);
                }
            }
        }
        // Content
        productionRunContents = EntityUtil.filterByDate(delegator.findByAnd("WorkEffortContentAndInfo", [workEffortId : productionRunId], ["-fromDate"]));
        context.productionRunContents = productionRunContents;
        context.productionRunComponentsData = productionRunComponentsData;
        context.productionRunComponentsDataReadyForIssuance = productionRunComponentsDataReadyForIssuance;
        context.productionRunComponentsAlreadyIssued = productionRunComponentsAlreadyIssued;
    }
}
