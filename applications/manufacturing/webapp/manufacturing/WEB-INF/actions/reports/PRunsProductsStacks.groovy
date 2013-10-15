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

// PRunsProductsStacks
// ReportC

import org.ofbiz.entity.util.EntityUtil;

if (productCategoryIdPar) {
    category = delegator.findOne("ProductCategory", [productCategoryId : productCategoryIdPar], false);
    context.category = category;
}
if (productFeatureTypeIdPar) {
    featureType = delegator.findOne("ProductFeatureType", [productFeatureTypeId : productFeatureTypeIdPar], false);
    context.featureType = featureType;
}

allProductionRuns = delegator.findByAnd("WorkEffortAndGoods", [workEffortName : planName], ["productId"], false);
productionRuns = [];
features = [:];
products = [:];

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
        location = [:];
        if (productionRunProduct) {
            locations = delegator.findByAnd("ProductFacilityLocation", [facilityId : productionRun.facilityId, productId : productionRun.productId], null, false);
            location = EntityUtil.getFirst(locations);
        }
        if (taskNamePar) {
            // select the production run's task of a given name (i.e. type) if any (based on the report's parameter)
            productionRunTasks = delegator.findByAnd("WorkEffort", [workEffortParentId : productionRun.workEffortId , workEffortName : taskNamePar], null, false);
            productionRunTask = EntityUtil.getFirst(productionRunTasks);
            if (!productionRunTask) {
                // the production run doesn't include the given task, skip it
                return;
            }
        }

        // Stack information
        stackInfos = [];
        productionRunQty = productionRun.quantityToProduce;
        //numOfStacks = (int)productionRunQty / stackQty; // number of stacks
        numOfStacks = productionRunQty / stackQty; // number of stacks
        numOfStacks = numOfStacks.intValue();
        qtyInLastStack = productionRunQty % stackQty; // qty in the last stack
        if (qtyInLastStack > 0) {
            numOfStacks++;
        } else {
            qtyInLastStack = stackQty;
        }
        for (int i = 1; i < numOfStacks; i++) {
            stackInfos.add([stackNum : "" + i, numOfStacks : "" + numOfStacks, qty : stackQty]);
        }
        stackInfos.add([stackNum : "" + numOfStacks, numOfStacks : "" + numOfStacks, qty : qtyInLastStack]);

        productionRunMap = [productionRun : productionRun,
                                          product : productionRunProduct,
                                          location : location,
                                          stackInfos : stackInfos];
        productionRuns.add(productionRunMap);
    }
    context.productionRuns = productionRuns;
}
