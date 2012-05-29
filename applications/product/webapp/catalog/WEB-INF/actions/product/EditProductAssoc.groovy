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

context.nowDate = UtilDateTime.nowDate();
context.nowTimestampString = UtilDateTime.nowTimestamp().toString();

useValues = true;
if (request.getAttribute("_ERROR_MESSAGE_")) {
    useValues = false;
}

productId = parameters.productId;
if (!productId) {
    productId = parameters.PRODUCT_ID;
} else {
    context.productId = productId;
}

productIdTo = parameters.PRODUCT_ID_TO;
if (productIdTo) {
    context.productIdTo = productIdTo;
}

productAssocTypeId = parameters.PRODUCT_ASSOC_TYPE_ID;
if (productAssocTypeId != null) {
    context.productAssocTypeId = productAssocTypeId;
}

fromDateStr = parameters.FROM_DATE;

fromDate = null;
if (UtilValidate.isNotEmpty(fromDateStr)) {
    fromDate = ObjectType.simpleTypeConvert(fromDateStr, "Timestamp", null, timeZone, locale, false);
}
if (!fromDate) {
    fromDate = request.getAttribute("ProductAssocCreateFromDate");
} else {
    context.fromDate = fromDate;
}

product = delegator.findOne("Product", [productId : productId], false);
if (product) {
    context.product = product;
}

productAssoc = delegator.findOne("ProductAssoc", [productId : productId, productIdTo : productIdTo, productAssocTypeId : productAssocTypeId, fromDate : fromDate], false);
if (productAssoc) {
    context.productAssoc = productAssoc;
}

if ("true".equalsIgnoreCase(parameters.useValues)) {
    useValues = true;
}

if (!productAssoc) {
    useValues = false;
}

context.useValues = useValues;
context.isCreate = true;

assocTypes = delegator.findList("ProductAssocType", null, null, ['description'], null, false);
context.assocTypes = assocTypes;

if (product) {
    context.assocFromProducts = product.getRelated("MainProductAssoc", null, ['sequenceNum'], false);

    context.assocToProducts = product.getRelated("AssocProductAssoc", null, null, false);
}
