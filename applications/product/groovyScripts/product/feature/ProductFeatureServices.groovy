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

import java.util.regex.Pattern
import org.apache.ofbiz.base.util.UtilProperties
import org.apache.ofbiz.entity.GenericValue


/**
 * Apply Feature to Product using Feature Type and ID Code
 * @return
 */
def applyFeatureToProductFromTypeAndCode() {
    // find the ProductFeatures by type and id code
    List productFeatures = from("ProductFeature")
            .where(productFeatureTypeId: parameters.productFeatureTypeId,
                    idCode: parameters.idCode)
            .queryList()
    for (GenericValue productFeature : productFeatures) {
        Map applyFeatureContext = parameters
        applyFeatureContext.productFeatureId = productFeature.productFeatureId
        if (! applyFeatureContext.sequenceNum) {
            applyFeatureContext.sequenceNum = productFeature.defaultSequenceNum
        }
        run service: "applyFeatureToProduct", with: applyFeatureContext
    }
    return success()
}

/**
 * Create a Product Feature Type
 * @return
 */
def createProductFeatureType() {
    Map result = success()
    if (!security.hasEntityPermission("CATALOG", "_CREATE", parameters.userLogin)) {
        return error(UtilProperties.getMessage("ProductUiLabels",
                "ProductCatalogCreatePermissionError", parameters.locale))
    }
    if (! parameters.productFeatureTypeId) {
        parameters.productFeatureTypeId = delegator.getNextSeqId("ProductFeatureType")
    } else if (!Pattern.matches('^[a-zA-Z_0-9]+$', parameters.productFeatureTypeId)) {
        return error(UtilProperties.getMessage("ProductErrorUiLabels",
                "ProductFeatureTypeIdMustContainsLettersAndDigits", parameters.locale))
    }
    GenericValue newEntity = makeValue("ProductFeatureType", parameters)
    newEntity.create()
    result.productFeatureTypeId = newEntity.productFeatureTypeId
    return result
}

/**
 * Create a ProductFeatureApplAttr
 * @return
 */
def createProductFeatureApplAttr() {
    if (!security.hasEntityPermission("CATALOG", "_CREATE", parameters.userLogin)) {
        return error(UtilProperties.getMessage("ProductUiLabels",
                "ProductCatalogCreatePermissionError", parameters.locale))
    }
    GenericValue newEntity = makeValue("ProductFeatureApplAttr", parameters)
    if (! newEntity.fromDate) {
        GenericValue productFeatureAppl = from("ProductFeatureAppl")
                .where(productId: newEntity.productId, productFeatureId: newEntity.productFeatureId)
                .filterByDate().orderBy("-fromDate").queryFirst()
        if (productFeatureAppl) {
            newEntity.fromDate = productFeatureAppl.fromDate
        }
    }
    newEntity.create()
    return success()
}