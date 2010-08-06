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
import org.ofbiz.entity.transaction.*
import org.ofbiz.base.util.*

module = "ApplyFeaturesFromCategory.groovy";

context.nowTimestampString = UtilDateTime.nowTimestamp().toString();

productFeatureCategoryId = request.getParameter("productFeatureCategoryId");
context.productFeatureCategoryId = productFeatureCategoryId;

context.selFeatureApplTypeId = request.getParameter("productFeatureApplTypeId");

context.curProductFeatureCategory = delegator.findOne("ProductFeatureCategory", [productFeatureCategoryId : productFeatureCategoryId], false);

context.productFeatureTypes = delegator.findList("ProductFeatureType", null, null, ['description'], null, false);

context.productFeatureCategories = delegator.findList("ProductFeatureCategory", null, null, ['description'], null, false);

//we only need these if we will be showing the apply feature to category forms
if (productId) {
    context.productFeatureApplTypes = delegator.findList("ProductFeatureApplType", null, null, ['description'], null, false);
}

productFeaturesSize = delegator.findCountByCondition("ProductFeature", EntityCondition.makeCondition("productFeatureCategoryId", EntityOperator.EQUALS, productFeatureCategoryId), null, null);

highIndex = 0;
lowIndex = 0;
listSize = (int) productFeaturesSize;

if (viewIndex == null) {
    viewIndex = 0;
}
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

whereCondition = EntityCondition.makeCondition([productFeatureCategoryId : productFeatureCategoryId], EntityOperator.AND);
EntityFindOptions efo = new EntityFindOptions();
efo.setDistinct(true);
efo.setResultSetType(EntityFindOptions.TYPE_SCROLL_INSENSITIVE);
efo.setMaxRows(highIndex);

boolean beganTransaction = false;
try {
    beganTransaction = TransactionUtil.begin();

    productFeaturesEli = delegator.find("ProductFeature", whereCondition, null, null, ['productFeatureTypeId', 'defaultSequenceNum', 'description'], efo);
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
    productFeatureAppls = delegator.findList("ProductFeatureAppl", EntityCondition.makeCondition([productId : productId, productFeatureId : productFeature.productFeatureId]), null, null, null, false);
    productFeatureApplIter = productFeatureAppls.iterator();
    while (productFeatureApplIter) {
        productFeatureAppl = productFeatureApplIter.next();
        productFeatureApplMap.put(productFeatureAppl.productFeatureId, productFeatureAppl.productFeatureApplTypeId);
    }
}
context.productFeatureApplMap = productFeatureApplMap;
