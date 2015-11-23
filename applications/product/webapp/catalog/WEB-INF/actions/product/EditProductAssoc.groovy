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
import org.ofbiz.base.util.*

uiLabelMap = UtilProperties.getResourceBundleMap("ProductUiLabels", locale);

product = from("Product").where("productId", parameters.productId).queryOne();

fromDate = UtilDateTime.nowTimestamp();
if (UtilValidate.isNotEmpty(parameters.fromDate)) {
    fromDate = ObjectType.simpleTypeConvert(parameters.fromDate, "Timestamp", null, timeZone, locale, false);
}

productAssoc = from("ProductAssoc").where("productId", parameters.productId, "productIdTo", parameters.productIdTo, "productAssocTypeId", parameters.productAssocTypeId, "fromDate", fromDate).queryOne();
context.productAssoc = productAssoc;

if (product) {
    assocFromProducts = product.getRelated("MainProductAssoc", null, ['sequenceNum'], false);
    assocToProducts = product.getRelated("AssocProductAssoc", null, null, false);
    assocFromMap = ["assocProducts" : assocFromProducts, "sectionTitle" : uiLabelMap.ProductAssociationsFromProduct];
    assocToMap = ["assocProducts" : assocToProducts, "sectionTitle" : uiLabelMap.ProductAssociationsToProduct];
    context.assocSections = [assocFromMap, assocToMap];
}