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

import org.ofbiz.base.util.*;
import org.ofbiz.entity.*;
import org.ofbiz.product.feature.*;
import org.ofbiz.product.product.ProductSearch;
import org.ofbiz.webapp.stats.VisitHandler;

visitId = VisitHandler.getVisitId(session);

featureIdByType = ParametricSearch.makeFeatureIdByTypeMap(request);
featureIdSet = [] as Set;
if (featureIdByType) {
    featureIdSet.addAll(featureIdByType.values());
}

productIds = ProductSearch.parametricKeywordSearch(featureIdSet, null, delegator, productCategoryId, true, visitId, true, true, false);

// get the product for each ID
products = new ArrayList(productIds.size());
productIds.each { productId ->
    product = delegator.findOne("Product", UtilMisc.toMap("productId", productId), true);
    products.add(product);
}

productFeatureAndTypeDatas = new ArrayList(featureIdByType.size());
featureIdByType.each { featureIdByTypeEntry ->
    productFeatureType = delegator.findOne("ProductFeatureType", UtilMisc.toMap("productFeatureTypeId", featureIdByTypeEntry.key), true);
    productFeature = delegator.findOne("ProductFeature", UtilMisc.toMap("productFeatureId", featureIdByTypeEntry.value), true);
    productFeatureAndTypeData = [:];
    productFeatureAndTypeData.productFeatureType = productFeatureType;
    productFeatureAndTypeData.productFeature = productFeature;
    productFeatureAndTypeDatas.add(productFeatureAndTypeData);
}

context.productFeatureAndTypeDatas = productFeatureAndTypeDatas;
context.products = products;
