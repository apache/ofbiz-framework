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

import org.apache.ofbiz.manufacturing.jobshopmgt.ProductionRunHelper
import org.apache.ofbiz.order.order.OrderReadHelper

if (productCategoryIdPar) {
    category = from("ProductCategory").where("productCategoryId", productCategoryIdPar).queryOne()
    context.category = category
}

allProductionRuns = from("WorkEffortAndGoods").where("workEffortName", planName, "statusId", "WEGS_CREATED", "workEffortGoodStdTypeId", "PRUN_PROD_DELIV").orderBy("productId").queryList()
productionRuns = []

if (allProductionRuns) {
    allProductionRuns.each { productionRun ->
        // verify if the product is a member of the given category (based on the report's parameter)
        if (productCategoryIdPar) {
            if (!isProductInCategory(delegator, productionRun.productId, productCategoryIdPar)) {
                // the production run's product is not a member of the given category, skip it
                return
            }
        }
        productionRunProduct = from("Product").where("productId", productionRun.productId).queryOne()
        String rootProductionRunId = ProductionRunHelper.getRootProductionRun(delegator, productionRun.workEffortId)

        productionRunOrder = from("WorkOrderItemFulfillment").where("workEffortId", rootProductionRunId).queryFirst()
        OrderReadHelper orh = new OrderReadHelper(delegator, productionRunOrder.orderId)

        // select the production run's task of a given name (i.e. type) if any (based on the report's parameter)
        productionRunTask = from("WorkEffort").where("workEffortParentId", productionRun.workEffortId, "workEffortName", taskNamePar).queryFirst()
        if (!productionRunTask) {
            // the production run doesn't include the given task, skip it
            return
        }

        productionRunMap = [productionRun : productionRun,
                                          product : productionRunProduct,
                                          productionRunTask : productionRunTask,
                                          productionRunOrder : productionRunOrder,
                                          customer : orh.getPlacingParty(),
                                          address : orh.getShippingAddress()]
        allProductionComponents = from("WorkEffortAndGoods").where("workEffortId", productionRunTask.workEffortId, "statusId", "WEGS_CREATED", "workEffortGoodStdTypeId", "PRUNT_PROD_NEEDED").orderBy("productId").queryList()
        
        componentList = []

        if (allProductionComponents) {
            allProductionComponents.each { productionComponent ->
                productionRunProductComp = from("Product").where("productId", productionComponent.productId).queryOne()
                productionRunProductMap = [component : productionComponent,componentProduct : productionRunProductComp]
                componentList.add(productionRunProductMap)
            }
        }
        productionRunMap.componentList = componentList
        productionRuns.add(productionRunMap)
    }
}
context.productionRuns = productionRuns
