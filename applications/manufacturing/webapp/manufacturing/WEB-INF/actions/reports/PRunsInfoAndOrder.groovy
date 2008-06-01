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

import java.util.*;
import org.ofbiz.entity.*;
import org.ofbiz.base.util.*;
import org.ofbiz.entity.util.*;
import org.ofbiz.manufacturing.jobshopmgt.ProductionRunHelper;
import org.ofbiz.order.order.OrderReadHelper;
import org.ofbiz.product.category.CategoryWorker;

if (!UtilValidate.isEmpty(productCategoryIdPar)) {
    category = delegator.findByPrimaryKey("ProductCategory", UtilMisc.toMap("productCategoryId", productCategoryIdPar));
    context.put("category", category);
}

allProductionRuns = delegator.findByAnd("WorkEffortAndGoods", UtilMisc.toMap("workEffortName", planName, "statusId", "WEGS_CREATED", "workEffortGoodStdTypeId", "PRUN_PROD_DELIV"), UtilMisc.toList("productId"));
productionRuns = new ArrayList();

if (allProductionRuns != null) {
    allProductionRunsIt = allProductionRuns.iterator();
    while (allProductionRunsIt.hasNext()) {
        productionRun = allProductionRunsIt.next();
        // verify if the product is a member of the given category (based on the report's parameter)
        if (!UtilValidate.isEmpty(productCategoryIdPar)) {
            if (!isProductInCategory(delegator, productionRun.getString("productId"), productCategoryIdPar)) {
                // the production run's product is not a member of the given category, skip it
                continue;
            }
        }
        productionRunProduct = delegator.findByPrimaryKey("Product", UtilMisc.toMap("productId", productionRun.getString("productId")));
        String rootProductionRunId = ProductionRunHelper.getRootProductionRun(delegator, productionRun.getString("workEffortId"));

        productionRunOrders = delegator.findByAnd("WorkOrderItemFulfillment", UtilMisc.toMap("workEffortId", rootProductionRunId));
        productionRunOrder = EntityUtil.getFirst(productionRunOrders);
        OrderReadHelper orh = new OrderReadHelper(delegator, productionRunOrder.getString("orderId"));

        // select the production run's task of a given name (i.e. type) if any (based on the report's parameter)
        productionRunTasks = delegator.findByAnd("WorkEffort", UtilMisc.toMap("workEffortParentId", productionRun.getString("workEffortId"), "workEffortName", taskNamePar));
        productionRunTask = EntityUtil.getFirst(productionRunTasks);
        if (productionRunTask == null) {
            // the production run doesn't include the given task, skip it
            continue;
        }

        productionRunMap = UtilMisc.toMap("productionRun", productionRun,
                                          "product", productionRunProduct,
                                          "productionRunTask", productionRunTask,
                                          "productionRunOrder", productionRunOrder,
                                          "customer", orh.getPlacingParty(),
                                          "address", orh.getShippingAddress());
        allProductionComponents = delegator.findByAnd("WorkEffortAndGoods", UtilMisc.toMap("workEffortId", productionRunTask.getString("workEffortId"), "statusId", "WEGS_CREATED", "workEffortGoodStdTypeId", "PRUNT_PROD_NEEDED"), UtilMisc.toList("productId"));
        componentList = new ArrayList();

        if (allProductionComponents != null) {
            allProductionComponentsIt = allProductionComponents.iterator();
            while (allProductionComponentsIt.hasNext()) {
                productionComponent = allProductionComponentsIt.next();

                productionRunProductComp = delegator.findByPrimaryKey("Product", UtilMisc.toMap("productId", productionComponent.getString("productId")));
                productionRunProductMap = UtilMisc.toMap("component", productionComponent,
                                                         "componentProduct", productionRunProductComp);
                componentList.add(productionRunProductMap);
            }
        }
        productionRunMap.put("componentList", componentList);
        productionRuns.add(productionRunMap);
    }
}
context.put("productionRuns", productionRuns);
