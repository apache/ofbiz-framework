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
import org.ofbiz.manufacturing.jobshopmgt.ProductionRun;

productionRunId = parameters.productionRunId;
if (productionRunId) {
    ProductionRun productionRun = new ProductionRun(productionRunId, delegator, dispatcher);
    if (productionRun.exist()) {
        productionRunId = productionRun.getGenericValue().workEffortId;
        context.productionRunId = productionRunId;
        context.productionRun = productionRun.getGenericValue();
        // Prepare production run header data
        productionRunData = [:];
        productionRunData.productionRunId = productionRunId;
        productionRunData.productId = productionRun.getProductProduced().productId;
        productionRunData.currentStatusId = productionRun.getGenericValue().currentStatusId;
        productionRunData.facilityId = productionRun.getGenericValue().facilityId;
        productionRunData.workEffortName = productionRun.getProductionRunName();
        productionRunData.description = productionRun.getDescription();
        productionRunData.quantity = productionRun.getQuantity();
        productionRunData.estimatedStartDate = productionRun.getEstimatedStartDate();
        productionRunData.estimatedCompletionDate = productionRun.getEstimatedCompletionDate();
        context.productionRunData = productionRunData;

        // Find all the order items to which this production run is linked.
        orderItems = delegator.findByAnd("WorkOrderItemFulfillment", [workEffortId : productionRunId]);
        if (orderItems) {
            context.orderItems = orderItems;
        }
        // Find all the work efforts that must be completed before this one.
        mandatoryWorkEfforts = EntityUtil.filterByDate(delegator.findByAnd("WorkEffortAssoc", [workEffortIdTo : productionRunId, workEffortAssocTypeId : "WORK_EFF_PRECEDENCY"]));
        if (mandatoryWorkEfforts) {
            context.mandatoryWorkEfforts = mandatoryWorkEfforts;
        }
        // Find all the work efforts that can start after this one.
        dependentWorkEfforts = EntityUtil.filterByDate(delegator.findByAnd("WorkEffortAssoc", [workEffortIdFrom : productionRunId, workEffortAssocTypeId : "WORK_EFF_PRECEDENCY"]));
        if (dependentWorkEfforts) {
            context.dependentWorkEfforts = dependentWorkEfforts;
        }
        //  RoutingTasks list
        context.productionRunRoutingTasks = productionRun.getProductionRunRoutingTasks();
        context.quantity = productionRun.getQuantity(); // this is useful to compute the total estimates runtime in the form
        //  Product component/parts list
        context.productionRunComponents = productionRun.getProductionRunComponents();;

        // Find all the notes linked to this production run.
        productionRunNoteData = delegator.findByAnd("WorkEffortNoteAndData", [workEffortId : productionRunId]);
        if (productionRunNoteData) {
            context.productionRunNoteData = productionRunNoteData;
        }
    }
}
