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

/*
 * For cases when the ApplyFeaturesFromCategory.ftl is actually supposed to get its list of ProductFeatures from a productFeatureGroupId.
 * Puts productFeatureGroup and productFeatures which are put of this group into the context.  Currently does not break out the features by view size.
 */

import org.apache.ofbiz.entity.*

productFeatureGroupId = parameters.get("productFeatureGroupId")
if (productFeatureGroupId) {
    productFeatureGroup = from("ProductFeatureGroup").where("productFeatureGroupId", productFeatureGroupId).queryOne()
    productFeatures = []
    productFeatureGroupAppls = productFeatureGroup.getRelated("ProductFeatureGroupAppl", null, ['sequenceNum'], false)
    for (pFGAi = productFeatureGroupAppls.iterator(); pFGAi;) {
        productFeatureGroupAppl = (GenericEntity)pFGAi.next()
        productFeature = (GenericEntity)productFeatureGroupAppl.getRelatedOne("ProductFeature", false)
        productFeature.set("defaultSequenceNum", productFeatureGroupAppl.getLong("sequenceNum"))
        productFeatures.add(productFeature)
    }
    context.productFeatureGroup = productFeatureGroup
    context.productFeatures = productFeatures

    // this will not break out the product features by view size
    context.listSize = productFeatures.size()
    context.highIndex = productFeatures.size()
}
