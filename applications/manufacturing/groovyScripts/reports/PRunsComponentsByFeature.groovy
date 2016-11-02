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

import org.apache.ofbiz.entity.util.EntityUtil
import org.apache.ofbiz.product.category.CategoryWorker

if (productCategoryIdPar) {
    category = from("ProductCategory").where("productCategoryId", productCategoryIdPar).queryOne()
    context.category = category
}
if (productFeatureTypeIdPar) {
    featureType = from("ProductFeatureType").where("productFeatureTypeId", productFeatureTypeIdPar).queryOne()
    context.featureType = featureType
}

allProductionRuns = from("WorkEffortAndGoods").where("workEffortName", planName).orderBy("productId").queryList()
productionRuns = []
features = [:] // each entry is a productFeatureId|{productFeature,products}
products = [:] // each entry is a productId|{product,quantity}
if (!productFeatureTypeIdPar) {
    features.put(null, UtilMisc.toMap("productFeature", null, "products", products))
}

if (allProductionRuns) {
    allProductionRuns.each { productionRun ->
        // select the production run's task of a given name (i.e. type) if any (based on the report's parameter)
        productionRunTask = from("WorkEffort").where("workEffortParentId", productionRun.workEffortId, "workEffortName", taskNamePar).queryFirst()
        if (!productionRunTask) {
            // the production run doesn't include the given task, skip it
            return
        }

        // select the task's components, if any
        allProductionRunComponents = from("WorkEffortGoodStandard").where("workEffortId", productionRunTask.workEffortId, "workEffortGoodStdTypeId", "PRUNT_PROD_NEEDED").queryList()
        allProductionRunComponents.each { productionRunComponent ->
            // verify if the product is a member of the given category (based on the report's parameter)
            if (productCategoryIdPar) {
                if (!isProductInCategory(delegator, productionRunComponent.productId, productCategoryIdPar)) {
                    // the production run's product is not a member of the given category, skip it
                    return
                }
            }
            productionRunProduct = from("Product").where("productId", productionRunComponent.productId).queryOne()

            location = null
            if (productionRunProduct) {
                location = from("ProductFacilityLocation").where("facilityId", productionRun.facilityId, "productId", productionRunProduct.productId).queryFirst()
            }

            // group by standard feature of type productFeatureTypeIdPar
            if (productFeatureTypeIdPar) {
                standardFeature = from("ProductFeatureAndAppl").where("productFeatureTypeId", productFeatureTypeIdPar, "productId", productionRunComponent.productId, "productFeatureApplTypeId", "STANDARD_FEATURE").filterByDate().queryFirst()
                standardFeatureId = null
                if (standardFeature) {
                    standardFeatureId = standardFeature.productFeatureId
                }
                if (!features.containsKey(standardFeatureId)) {
                    features.put(standardFeatureId, [productFeature : standardFeature, products : [:]])
                }
                feature = (Map)features.get(standardFeatureId)
                products = (Map)feature.products
            }

            //
            // populate the products map and sum the quantities
            //
            if (!products.containsKey(productionRunComponent.getString("productId"))) {
                products.put(productionRunComponent.productId, [product : productionRunProduct, quantity : new Double(0), location : location])
            }
            Map productMap = (Map)products.get(productionRunComponent.productId)
            productMapQty = productMap.quantity
            currentProductQty = productionRunComponent.estimatedQuantity
            productMap.quantity = productMapQty + currentProductQty
        }
    }
    // now create lists of products for each feature group
    featuresIt = features.values().iterator()
    while (featuresIt) {
        feature = featuresIt.next()
        productsMap = feature.products
        productsMapIt = productsMap.values().iterator()
        while (productsMapIt.hasNext()) {
            productMap = productsMapIt.next()
            if (productMap.product.productWidth && productMap.product.productHeight) {
                productMapQty = productMap.quantity
                productHeight = productMap.product.productHeight
                productWidth = productMap.product.productWidth
                productArea = (productHeight * productWidth) / (1000 * 1000)
                panelQty = 0.0
                int panelQtyInt = 0
                if (productArea > 0) panelQty = productMapQty / productArea
                panelQtyInt = panelQty
                if (panelQtyInt < panelQty) panelQtyInt++
                productMap.panelQuantity = panelQtyInt
            }
        }
        feature.productList = productsMap.values()
    }
    context.features = features.values()
}
