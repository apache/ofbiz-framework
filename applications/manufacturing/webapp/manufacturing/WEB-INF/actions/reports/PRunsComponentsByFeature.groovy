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

// PRunsComponentsByFeature
// ReportF

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
features = new HashMap(); // each entry is a productFeatureId|{productFeature,products}
products = new HashMap(); // each entry is a productId|{product,quantity} 
if (UtilValidate.isEmpty(productFeatureTypeIdPar)) {
    features.put(null, UtilMisc.toMap("productFeature", null, "products", products));
}

if (allProductionRuns != null) {
    allProductionRunsIt = allProductionRuns.iterator();
    while (allProductionRunsIt.hasNext()) {
        productionRun = allProductionRunsIt.next();
        // select the production run's task of a given name (i.e. type) if any (based on the report's parameter)
        productionRunTasks = delegator.findByAnd("WorkEffort", UtilMisc.toMap("workEffortParentId", productionRun.getString("workEffortId"), "workEffortName", taskNamePar));
        productionRunTask = EntityUtil.getFirst(productionRunTasks);
        if (productionRunTask == null) {
            // the production run doesn't include the given task, skip it
            continue;
        }

        // select the task's components, if any
        allProductionRunComponents = delegator.findByAnd("WorkEffortGoodStandard", UtilMisc.toMap("workEffortId", productionRunTask.getString("workEffortId"),"workEffortGoodStdTypeId", "PRUNT_PROD_NEEDED"));
        allProductionRunComponentsIt = allProductionRunComponents.iterator();
        while(allProductionRunComponentsIt.hasNext()) {
            productionRunComponent = allProductionRunComponentsIt.next();
            // verify if the product is a member of the given category (based on the report's parameter)
            if (!UtilValidate.isEmpty(productCategoryIdPar)) {
                if (!isProductInCategory(delegator, productionRunComponent.getString("productId"), productCategoryIdPar)) {
                    // the production run's product is not a member of the given category, skip it
                    continue;
                }
            }
            productionRunProduct = delegator.findByPrimaryKey("Product", UtilMisc.toMap("productId", productionRunComponent.getString("productId")));

            location = null;
            if (!UtilValidate.isEmpty(productionRunProduct)) {
                locations = delegator.findByAnd("ProductFacilityLocation", UtilMisc.toMap("facilityId", productionRun.getString("facilityId"), "productId", productionRunProduct.getString("productId")));
                location = EntityUtil.getFirst(locations);
            }

            // group by standard feature of type productFeatureTypeIdPar
            if (productFeatureTypeIdPar != null) {
                standardFeatures = delegator.findByAnd("ProductFeatureAndAppl", UtilMisc.toMap("productFeatureTypeId", productFeatureTypeIdPar, "productId", productionRunComponent.getString("productId"), "productFeatureApplTypeId", "STANDARD_FEATURE"));
                standardFeatures = EntityUtil.filterByDate(standardFeatures);
                standardFeature = EntityUtil.getFirst(standardFeatures);
                standardFeatureId = null;
                if (standardFeature != null) {
                    standardFeatureId = standardFeature.getString("productFeatureId");
                }
                if (!features.containsKey(standardFeatureId)) {
                    features.put(standardFeatureId, UtilMisc.toMap("productFeature", standardFeature, "products", new HashMap()));
                }
                feature = (Map)features.get(standardFeatureId);
                products = (Map)feature.get("products");
            }

            //
            // populate the products map and sum the quantities
            //
            if (!products.containsKey(productionRunComponent.getString("productId"))) {
                products.put(productionRunComponent.getString("productId"), UtilMisc.toMap("product", productionRunProduct, "quantity", new Double(0), "location", location));
            }
            Map productMap = (Map)products.get(productionRunComponent.getString("productId"));
            Double productMapQty = (Double)productMap.get("quantity");
            Double currentProductQty = productionRunComponent.getDouble("estimatedQuantity");
            productMap.put("quantity", new Double(productMapQty.doubleValue() + currentProductQty.doubleValue()));
        }
    }
    // now create lists of products for each feature group
    featuresIt = features.values().iterator();
    while (featuresIt.hasNext()) {
        feature = featuresIt.next();
        productsMap = feature.get("products");
        productsMapIt = productsMap.values().iterator();
        while (productsMapIt.hasNext()) {
            productMap = productsMapIt.next();
            if (productMap.get("product").get("productWidth") != null && productMap.get("product").get("productHeight") != null) {
                Double productMapQty = (Double)productMap.get("quantity");
                Double productHeight = (Double)productMap.get("product").get("productHeight");
                Double productWidth = (Double)productMap.get("product").get("productWidth");
                double productArea = (productHeight.doubleValue() * productWidth.doubleValue()) / (1000 * 1000);
                double panelQty = 0.0;
                int panelQtyInt = 0;
                if (productArea > 0) panelQty = productMapQty.doubleValue() / productArea;
                panelQtyInt = (int)panelQty;
                if (panelQtyInt < panelQty) panelQtyInt++;
                productMap.put("panelQuantity", new Integer(panelQtyInt));
            }
        }
        feature.put("productList", productsMap.values());
    }
    context.put("features", features.values());
}
