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

// PRunsInfoAndOrder
// ReportG

import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.manufacturing.jobshopmgt.ProductionRunHelper;
import org.ofbiz.order.order.OrderReadHelper;

if (productCategoryIdPar) {
    category = delegator.findOne("ProductCategory", [productCategoryId : productCategoryIdPar], false);
    context.category = category;
}

allProductionRuns = delegator.findByAnd("WorkEffortAndGoods", [workEffortName : planName, statusId : "WEGS_CREATED", workEffortGoodStdTypeId : "PRUN_PROD_DELIV"], ["productId"], false);
productionRuns = [];

if (allProductionRuns) {
    allProductionRuns.each { productionRun ->
        // verify if the product is a member of the given category (based on the report's parameter)
        if (productCategoryIdPar) {
            if (!isProductInCategory(delegator, productionRun.productId, productCategoryIdPar)) {
                // the production run's product is not a member of the given category, skip it
                return;
            }
        }
        productionRunProduct = delegator.findOne("Product", [productId : productionRun.productId], false);
        String rootProductionRunId = ProductionRunHelper.getRootProductionRun(delegator, productionRun.workEffortId);

        productionRunOrders = delegator.findByAnd("WorkOrderItemFulfillment", [workEffortId : rootProductionRunId], null, false);
        productionRunOrder = EntityUtil.getFirst(productionRunOrders);
        OrderReadHelper orh = new OrderReadHelper(delegator, productionRunOrder.orderId);

        // select the production run's task of a given name (i.e. type) if any (based on the report's parameter)
        productionRunTasks = delegator.findByAnd("WorkEffort", [workEffortParentId : productionRun.workEffortId, workEffortName : taskNamePar], null, false);
        productionRunTask = EntityUtil.getFirst(productionRunTasks);
        if (!productionRunTask) {
            // the production run doesn't include the given task, skip it
            return;
        }

        productionRunMap = [productionRun : productionRun,
                                          product : productionRunProduct,
                                          productionRunTask : productionRunTask,
                                          productionRunOrder : productionRunOrder,
                                          customer : orh.getPlacingParty(),
                                          address : orh.getShippingAddress()];
        allProductionComponents = delegator.findByAnd("WorkEffortAndGoods", [workEffortId : productionRunTask.workEffortId, statusId : "WEGS_CREATED", workEffortGoodStdTypeId : "PRUNT_PROD_NEEDED"], ["productId"], false);
        componentList = [];

        if (allProductionComponents) {
            allProductionComponents.each { productionComponent ->
                productionRunProductComp = delegator.findOne("Product", [productId : productionComponent.productId], false);
                productionRunProductMap = [component : productionComponent,componentProduct : productionRunProductComp];
                componentList.add(productionRunProductMap);
            }
        }
        productionRunMap.componentList = componentList;
        productionRuns.add(productionRunMap);
    }
}
context.productionRuns = productionRuns;
