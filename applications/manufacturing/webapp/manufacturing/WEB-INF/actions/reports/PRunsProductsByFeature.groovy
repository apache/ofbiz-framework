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

import java.util.*;
import org.ofbiz.entity.*;
import org.ofbiz.base.util.*;
import org.ofbiz.entity.util.*;
import org.ofbiz.product.category.CategoryWorker;

if (!UtilValidate.isEmpty(productCategoryIdPar)) {
    category = delegator.findByPrimaryKey("ProductCategory", UtilMisc.toMap("productCategoryId", productCategoryIdPar));
    context.put("category", category);
}
if (!UtilValidate.isEmpty(productFeatureTypeIdPar)) {
    featureType = delegator.findByPrimaryKey("ProductFeatureType", UtilMisc.toMap("productFeatureTypeId", productFeatureTypeIdPar));
    context.put("featureType", featureType);
}

allProductionRuns = delegator.findByAnd("WorkEffortAndGoods", UtilMisc.toMap("workEffortName", planName), UtilMisc.toList("productId"));
productionRuns = new ArrayList();
features = new HashMap();
if (UtilValidate.isEmpty(productFeatureTypeIdPar)) {
    features.put(null, UtilMisc.toMap("productFeature", null, "productionRuns", productionRuns));
}

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

        // group by standard feature of type productFeatureTypeIdPar
        if (productFeatureTypeIdPar != null) {
            standardFeatures = delegator.findByAnd("ProductFeatureAndAppl", UtilMisc.toMap("productFeatureTypeId", productFeatureTypeIdPar, "productId", productionRun.getString("productId"), "productFeatureApplTypeId", "STANDARD_FEATURE"));
            standardFeatures = EntityUtil.filterByDate(standardFeatures);
            standardFeature = EntityUtil.getFirst(standardFeatures);
            standardFeatureId = null;
            if (standardFeature != null) {
                standardFeatureId = standardFeature.getString("productFeatureId");
            }
            if (!features.containsKey(standardFeatureId)) {
                features.put(standardFeatureId, UtilMisc.toMap("productFeature", standardFeature, "productionRuns", new ArrayList()));
            }
            feature = (Map)features.get(standardFeatureId);
            productionRuns = (List)feature.get("productionRuns");
        }

        // select the production run's task of a given name (i.e. type) if any (based on the report's parameter)
        productionRunTasks = delegator.findByAnd("WorkEffort", UtilMisc.toMap("workEffortParentId", productionRun.getString("workEffortId"), "workEffortName", taskNamePar));
        productionRunTask = EntityUtil.getFirst(productionRunTasks);
        if (productionRunTask == null) {
            // the production run doesn't include the given task, skip it
            continue;
        }

        productionRunMap = UtilMisc.toMap("productionRun", productionRun,
                                          "product", productionRunProduct,
                                          "productionRunTask", productionRunTask);
        productionRuns.add(productionRunMap);
    }
    context.put("features", features.values());
}
