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

// PRunsProductsByFeature
// ReportE

import org.apache.ofbiz.entity.util.EntityUtil

if (productCategoryIdPar) {
    category = from("ProductCategory").where("productCategoryId", productCategoryIdPar).queryOne()
    context.category = category
}
if (productFeatureTypeIdPar) {
    featureType = from("ProductFeatureType").where("productFeatureTypeId", productFeatureTypeIdPar).queryOne()
    context.featureType = featureType
}

allProductionRuns = from("WorkEffortAndGoods").where("workEffortName", planName).orderBy("productId").queryList()
productionRuns = [:]
features = []
if (!productFeatureTypeIdPar) {
    features.put(null, UtilMisc.toMap("productFeature", null, "productionRuns", productionRuns))
}

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

        // group by standard feature of type productFeatureTypeIdPar
        if (productFeatureTypeIdPar) {
            standardFeature = from("ProductFeatureAndAppl").where("productFeatureTypeId", productFeatureTypeIdPar, "productId", productionRun.productId, "productFeatureApplTypeId", "STANDARD_FEATURE").filterByDate().queryFirst()
            standardFeatureId = null
            if (standardFeature) {
                standardFeatureId = standardFeature.productFeatureId
            }
            if (!features.containsKey(standardFeatureId)) {
                features.put(standardFeatureId, [productFeature : standardFeature, productionRuns : []])
            }
            feature = (Map)features.standardFeatureId
            productionRuns = (List)feature.productionRuns
        }

        // select the production run's task of a given name (i.e. type) if any (based on the report's parameter)
        productionRunTask = from("WorkEffort").where("workEffortParentId", productionRun.workEffortId, "workEffortName", taskNamePar).queryFirst()
        if (!productionRunTask) {
            // the production run doesn't include the given task, skip it
            return
        }

        productionRunMap = [productionRun : productionRun,
                                          product : productionRunProduct,
                                          productionRunTask  : productionRunTask]
        productionRuns.add(productionRunMap)
    }
    context.features = features.values()
}
