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

import java.sql.Timestamp;
import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.base.util.UtilDateTime;

context.nowDate = UtilDateTime.nowDate();
context.nowTimestampString = UtilHttp.encodeBlanks(UtilDateTime.nowTimestamp().toString());

boolean useValues = true;
if (request.getAttribute("_ERROR_MESSAGE_")) useValues = false;

productId = parameters.productId;
if (productId) context.productId = productId;

productIdTo = parameters.productIdTo;
updateMode = parameters.UPDATE_MODE;

if (productIdTo) context.productIdTo = productIdTo;

productAssocTypeId = parameters.productAssocTypeId;
if (productAssocTypeId) context.productAssocTypeId = productAssocTypeId;

fromDateStr = parameters.fromDate;

Timestamp fromDate = null;
if (fromDateStr) fromDate = Timestamp.valueOf(fromDateStr) ?: (Timestamp)request.getAttribute("ProductAssocCreateFromDate");;
context.fromDate = fromDate;

productAssoc = delegator.findOne("ProductAssoc", [productId : productId, productIdTo : productIdTo, productAssocTypeId : productAssocTypeId, fromDate : fromDate], false);
if (updateMode) {
    productAssoc = [:];
    context.remove("productIdTo");
}
if (productAssoc) {
    context.productAssoc = productAssoc;
}

if ("true".equalsIgnoreCase((String)request.getParameter("useValues"))) useValues = true;
if (!productAssoc) useValues = false;

context.useValues = useValues;

Collection assocTypes = delegator.findByAnd("ProductAssocType", [parentTypeId : "PRODUCT_COMPONENT"], ["productAssocTypeId", "description"], false);
context.assocTypes = assocTypes;

Collection formulae = delegator.findByAnd("CustomMethod", [customMethodTypeId : "BOM_FORMULA"], ["customMethodId", "description"], false);
context.formulae = formulae;

if (product) {
    assocFromProducts = product.getRelated("MainProductAssoc", (productAssocTypeId ? [productAssocTypeId : productAssocTypeId]: [:]), ["sequenceNum","productId"], false);
    if (assocFromProducts) context.assocFromProducts = assocFromProducts;

    assocToProducts = product.getRelated("AssocProductAssoc", (productAssocTypeId ? [productAssocTypeId : productAssocTypeId]: [:]), ["sequenceNum","productId"], false);
    if (assocToProducts) context.assocToProducts = assocToProducts;
}

