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

import org.apache.ofbiz.base.util.*

productId = request.getParameter("productId")
productVariantId = productId + "_"
productFeatureIds = ""
product = from("Product").where("productId", productId).queryOne()

result = runService('getProductFeaturesByType', [productId : productId, productFeatureApplTypeId : "SELECTABLE_FEATURE"])
featureTypes = result.productFeatureTypes
featuresByTypes = result.productFeaturesByType
searchFeatures = []
selectedFeatureTypeValues = []
if (featureTypes) {
    featureTypes.each { featureType ->
        featuresByType = featuresByTypes[featureType]
        featureTypeAndValues = [featureType : featureType, features : featuresByType]
        searchFeatures.add(featureTypeAndValues)
        //
        selectedFeatureTypeValue = request.getParameter(featureType)
        if (selectedFeatureTypeValue) {
            featureTypeAndValues.selectedFeatureId = selectedFeatureTypeValue
            selectedFeatureTypeValues.add(selectedFeatureTypeValue)
            feature = from("ProductFeature").where("productFeatureId", selectedFeatureTypeValue).cache(true).queryOne()
            productVariantId += feature.getString("idCode") ?: ""
            productFeatureIds += "|" + selectedFeatureTypeValue
        }
    }
}

variants = []
    result = runService('getAllExistingVariants', [productId : productId, productFeatureAppls : selectedFeatureTypeValues])
    variants = result.variantProductIds

// Quick Add Variant
productFeatureIdsPar = request.getParameter("productFeatureIds")
productVariantIdPar = request.getParameter("productVariantId")
if (productVariantIdPar && productFeatureIdsPar) {
    result = runService('quickAddVariant', [productId : productId, productFeatureIds : productFeatureIdsPar, productVariantId : productVariantIdPar])
}

context.product = product
context.searchFeatures = searchFeatures
context.variants = variants

// also need the variant products themselves
variantProducts = []
variants.each { variantId ->
    variantProducts.add(from("Product").where("productId", variantId).cache(true).queryOne())
}
context.variantProducts = variantProducts

if (security.hasEntityPermission("CATALOG", "_CREATE", session)) {
    if (selectedFeatureTypeValues.size() == featureTypes.size() && variants.size() == 0) {
        context.productFeatureIds = productFeatureIds
        context.productVariantId = productVariantId
    }
}
