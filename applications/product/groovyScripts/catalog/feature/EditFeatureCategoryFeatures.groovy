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

import org.ofbiz.entity.*
import org.ofbiz.entity.condition.*
import org.ofbiz.entity.util.EntityFindOptions
import org.ofbiz.base.util.*
import org.ofbiz.entity.transaction.*

module = "EditFeatureCategoryFeatures.groovy";

context.hasPermission = security.hasEntityPermission("CATALOG", "_VIEW", session);

context.nowTimestampString = UtilDateTime.nowTimestamp().toString();

productId = request.getParameter("productId");
context.productId = productId;

productFeatureCategoryId = parameters.get("productFeatureCategoryId");
context.productFeatureCategoryId = productFeatureCategoryId;

context.curProductFeatureCategory = from("ProductFeatureCategory").where("productFeatureCategoryId", productFeatureCategoryId).queryOne();

context.productFeatureTypes = from("ProductFeatureType").orderBy("description").queryList();

context.productFeatureCategories = from("ProductFeatureCategory").orderBy("description").queryList();

//we only need these if we will be showing the apply feature to category forms
if (productId) {
    context.productFeatureApplTypes = from("ProductFeatureApplType").orderBy("description").queryList();
}

productFeaturesSize = from("ProductFeature").where("productFeatureCategoryId", productFeatureCategoryId).queryCount();

highIndex = 0;
lowIndex = 0;
listSize = (int) productFeaturesSize;

viewIndex = (viewIndex) ?: 0;

lowIndex = viewIndex * viewSize;
highIndex = (viewIndex + 1) * viewSize;
if (listSize < highIndex) {
    highIndex = listSize;
}

context.viewIndex = viewIndex;
context.viewSize = viewSize;
context.listSize = listSize;
context.lowIndex = lowIndex;
context.highIndex = highIndex;

boolean beganTransaction = false;
try {
    beganTransaction = TransactionUtil.begin();

    productFeaturesEli = from("ProductFeature")
                            .where("productFeatureCategoryId", productFeatureCategoryId)
                            .orderBy("productFeatureTypeId", "defaultSequenceNum", "description")
                            .distinct()
                            .cursorScrollInsensitive()
                            .maxRows(highIndex)
                            .queryIterator();
    productFeatures = productFeaturesEli.getPartialList(lowIndex + 1, highIndex - lowIndex);
    productFeaturesEli.close();
} catch (GenericEntityException e) {
    String errMsg = "Failure in operation, rolling back transaction";
    Debug.logError(e, errMsg, module);
    try {
        // only rollback the transaction if we started one...
        TransactionUtil.rollback(beganTransaction, errMsg, e);
    } catch (GenericEntityException e2) {
        Debug.logError(e2, "Could not rollback transaction: " + e2.toString(), module);
    }
    // after rolling back, rethrow the exception
    throw e;
} finally {
    // only commit the transaction if we started one... this will throw an exception if it fails
    TransactionUtil.commit(beganTransaction);
}

context.productFeatures = productFeatures;

productFeatureApplMap = [:];
productFeatureAppls = null;
productFeatureIter = productFeatures.iterator();
productFeatureApplIter = null;
while (productFeatureIter) {
    productFeature = productFeatureIter.next();
    productFeatureAppls = from("ProductFeatureAppl").where("productId", productId, "productFeatureId", productFeature.productFeatureId).queryList();
    productFeatureApplIter = productFeatureAppls.iterator();
    while (productFeatureApplIter) {
        productFeatureAppl = productFeatureApplIter.next();
        productFeatureApplMap.put(productFeatureAppl.productFeatureId, productFeatureAppl.productFeatureApplTypeId);
    }
}
context.productFeatureApplMap = productFeatureApplMap;
