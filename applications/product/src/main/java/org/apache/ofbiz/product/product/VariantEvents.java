/*******************************************************************************
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
 *******************************************************************************/
package org.apache.ofbiz.product.product;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilHttp;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.transaction.GenericTransactionException;
import org.apache.ofbiz.entity.transaction.TransactionUtil;
import org.apache.ofbiz.entity.util.EntityQuery;

/**
 * Product Variant Related Events
 */
public class VariantEvents {

    private static final String MODULE = VariantEvents.class.getName();
    private static final String RESOURCE = "ProductErrorUiLabels";

    /** Creates variant products from a virtual product and a combination of selectable features
     *@param request The HTTPRequest object for the current request
     *@param response The HTTPResponse object for the current request
     *@return String specifying the exit status of this event
     */
    public static String quickAddChosenVariant(HttpServletRequest request, HttpServletResponse response) {
        String errMsg = "";
        Delegator delegator = (Delegator) request.getAttribute("delegator");

        String productId = request.getParameter("productId");
        String variantProductId = request.getParameter("variantProductId");
        String featureTypeSizeStr = request.getParameter("featureTypeSize");

        if (UtilValidate.isEmpty(productId)) {
            errMsg = UtilProperties.getMessage(RESOURCE, "variantevents.productId_required_but_missing", UtilHttp.getLocale(request));
            request.setAttribute("_ERROR_MESSAGE_", errMsg);
            return "error";
        }

        if (UtilValidate.isEmpty(variantProductId)) {
            errMsg = UtilProperties.getMessage(RESOURCE, "variantevents.variantProductId_required_but_missing_enter_an_id",
                    UtilHttp.getLocale(request));
            request.setAttribute("_ERROR_MESSAGE_", errMsg);
            return "error";
        }

        int featureTypeSize = 0;

        try {
            featureTypeSize = Integer.parseInt(featureTypeSizeStr);
        } catch (NumberFormatException e) {
            Map<String, String> messageMap = UtilMisc.toMap("featureTypeSizeStr", featureTypeSizeStr);
            errMsg = UtilProperties.getMessage(RESOURCE, "variantevents.featureTypeSize_not_number", messageMap, UtilHttp.getLocale(request));
            request.setAttribute("_ERROR_MESSAGE_", errMsg);
            return "error";
        }

        try {
            boolean beganTransacton = TransactionUtil.begin();

            try {
                // read the product, duplicate it with the given id
                GenericValue product = EntityQuery.use(delegator).from("Product").where("productId", productId).queryOne();
                if (product == null) {
                    Map<String, String> messageMap = UtilMisc.toMap("productId", productId);
                    errMsg = UtilProperties.getMessage(RESOURCE, "variantevents.product_not_found_with_ID", messageMap, UtilHttp.getLocale(request));
                    TransactionUtil.rollback(beganTransacton, errMsg, null);
                    request.setAttribute("_ERROR_MESSAGE_", errMsg);
                    return "error";
                }

                // check if product exists
                GenericValue variantProduct = EntityQuery.use(delegator).from("Product").where("productId", variantProductId).queryOne();
                if (variantProduct == null) {
                    //if product does not exist
                    variantProduct = GenericValue.create(product);
                    variantProduct.set("productId", variantProductId);
                    variantProduct.set("isVirtual", "N");
                    variantProduct.set("isVariant", "Y");
                    variantProduct.set("primaryProductCategoryId", null);
                    //create new
                    variantProduct.create();
                } else {
                    //if product does exist
                    variantProduct.set("isVirtual", "N");
                    variantProduct.set("isVariant", "Y");
                    variantProduct.set("primaryProductCategoryId", null);
                    //update entry
                    variantProduct.store();
                }

                // add an association from productId to variantProductId of the PRODUCT_VARIANT
                GenericValue productAssoc = delegator.makeValue("ProductAssoc",
                        UtilMisc.toMap("productId", productId, "productIdTo", variantProductId,
                            "productAssocTypeId", "PRODUCT_VARIANT", "fromDate", UtilDateTime.nowTimestamp()));
                productAssoc.create();

                // add the selected standard features to the new product given the productFeatureIds
                for (int i = 0; i < featureTypeSize; i++) {
                    String productFeatureId = request.getParameter("feature_" + i);
                    if (productFeatureId == null) {
                        Map<String, String> messageMap = UtilMisc.toMap("i", Integer.toString(i));
                        errMsg = UtilProperties.getMessage(RESOURCE, "variantevents.productFeatureId_for_feature_type_number_not_found",
                                messageMap, UtilHttp.getLocale(request));
                        TransactionUtil.rollback(beganTransacton, errMsg, null);
                        request.setAttribute("_ERROR_MESSAGE_", errMsg);
                        return "error";
                    }

                    GenericValue productFeature = EntityQuery.use(delegator).from("ProductFeature").where("productFeatureId",
                            productFeatureId).queryOne();

                    GenericValue productFeatureAppl = delegator.makeValue("ProductFeatureAppl",
                            UtilMisc.toMap("productId", variantProductId, "productFeatureId", productFeatureId,
                                "productFeatureApplTypeId", "STANDARD_FEATURE", "fromDate", UtilDateTime.nowTimestamp()));

                    // set the default seq num if it's there...
                    if (productFeature != null) {
                        productFeatureAppl.set("sequenceNum", productFeature.get("defaultSequenceNum"));
                    }

                    productFeatureAppl.create();
                }

                TransactionUtil.commit(beganTransacton);
            } catch (GenericEntityException e) {
                Map<String, String> messageMap = UtilMisc.toMap("errMessage", e.toString());
                errMsg = UtilProperties.getMessage(RESOURCE, "variantevents.entity_error_quick_add_variant_data", messageMap,
                        UtilHttp.getLocale(request));
                TransactionUtil.rollback(beganTransacton, errMsg, null);
                Debug.logError(e, "Entity error creating quick add variant data", MODULE);
                request.setAttribute("_ERROR_MESSAGE_", errMsg);
                return "error";
            }
        } catch (GenericTransactionException e) {
            Debug.logError(e, "Transaction error creating quick add variant data", MODULE);
            Map<String, String> messageMap = UtilMisc.toMap("errMessage", e.toString());
            errMsg = UtilProperties.getMessage(RESOURCE, "variantevents.transaction_error_quick_add_variant_data", messageMap,
                    UtilHttp.getLocale(request));
            request.setAttribute("_ERROR_MESSAGE_", errMsg);
            return "error";
        }

        Map<String, String> messageMap = UtilMisc.toMap("variantProductId", variantProductId);
        String sucMsg = UtilProperties.getMessage(RESOURCE, "variantevents.successfully_created_variant_product_with_id", messageMap,
                UtilHttp.getLocale(request));
        request.setAttribute("_EVENT_MESSAGE_", sucMsg);
        return "success";
    }
}
