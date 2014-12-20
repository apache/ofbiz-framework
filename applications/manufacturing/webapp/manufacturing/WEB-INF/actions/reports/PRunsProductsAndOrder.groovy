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

// PRunsProductsAndOrder
// ReportD

import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.manufacturing.jobshopmgt.ProductionRunHelper;
import org.ofbiz.order.order.OrderReadHelper;

if (productCategoryIdPar) {
    category = from("ProductCategory").where("productCategoryId", productCategoryIdPar).queryOne();
    context.category = category;
}

allProductionRuns = from("WorkEffortAndGoods").where("workEffortName", planName, "statusId", "WEGS_CREATED", "workEffortGoodStdTypeId", "PRUN_PROD_DELIV").orderBy("productId").queryList();
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
        productionRunProduct = from("Product").where("productId", productionRun.productId).queryOne();
        String rootProductionRunId = ProductionRunHelper.getRootProductionRun(delegator, productionRun.workEffortId);

        productionRunOrder = from("WorkOrderItemFulfillment").where("workEffortId", rootProductionRunId).queryFirst();
        OrderReadHelper orh = new OrderReadHelper(delegator, productionRunOrder.orderId);
        location = from("ProductFacilityLocation").where("productId", productionRun.productId, "facilityId", productionRun.facilityId).queryFirst();

        productionRunMap = [productionRun : productionRun,
                                          product : productionRunProduct,
                                          productionRunOrder : productionRunOrder,
                                          customer : orh.getPlacingParty(),
                                          address : orh.getShippingAddress(),
                                          location : location];

        productionRunMap.plan = planName;
        quantity = productionRun.estimatedQuantity;
        for (int i = 0; i < quantity; i++) {
            productionRuns.add(productionRunMap);
        }
    }
}
context.productionRuns = productionRuns;
