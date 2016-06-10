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

import org.ofbiz.base.util.UtilHttp

paramMap = UtilHttp.getParameterMap(request);

contentId = context.contentId;
dataResourceId = context.dataResourceId;

productFeatureList = from("ProductFeature").cache(true).queryList();
featureList = [] as ArrayList;
if (dataResourceId) {
    productFeatureList.each { productFeature ->
        productFeatureId = productFeature.productFeatureId;
        description = productFeature.description;
        productFeatureDataResource = from("ProductFeatureDataResource").where("productFeatureId", productFeatureId, "dataResourceId", dataResourceId).cache(true).queryOne();
        if (productFeatureDataResource) {
            feature = [];
            feature.productFeatureId = productFeatureId;
            feature.dataResourceId = dataResourceId;
            feature.action = "Y";
            feature.description = description;
            featureList.add(feature);
        }
    }
    context.featureList = featureList;
}
