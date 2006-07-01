/*
 * $Id: VariantEvents.java 5462 2005-08-05 18:35:48Z jonesde $
 *
 *  Copyright (c) 2001-2005 The Open For Business Project - www.ofbiz.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a
 *  copy of this software and associated documentation files (the "Software"),
 *  to deal in the Software without restriction, including without limitation
 *  the rights to use, copy, modify, merge, publish, distribute, sublicense,
 *  and/or sell copies of the Software, and to permit persons to whom the
 *  Software is furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included
 *  in all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 *  OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 *  IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 *  CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT
 *  OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 *  THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.ofbiz.product.product;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.transaction.GenericTransactionException;
import org.ofbiz.entity.transaction.TransactionUtil;
import org.ofbiz.security.Security;

import java.util.Map;

/**
 * Product Variant Related Events
 *
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @version    $Rev$
 * @since      2.0
 */
public class VariantEvents {

    public static final String module = VariantEvents.class.getName();
    public static final String resource = "ProductUiLabels";

    /** Creates variant products from a virtual product and a combination of selectable features
     *@param request The HTTPRequest object for the current request
     *@param response The HTTPResponse object for the current request
     *@return String specifying the exit status of this event
     */
    public static String quickAddChosenVariant(HttpServletRequest request, HttpServletResponse response) {
        String errMsg = "";
        GenericDelegator delegator = (GenericDelegator) request.getAttribute("delegator");
        Security security = (Security) request.getAttribute("security");

        String productId = request.getParameter("productId");
        String variantProductId = request.getParameter("variantProductId");
        String featureTypeSizeStr = request.getParameter("featureTypeSize");

        if (UtilValidate.isEmpty(productId)) {
            errMsg = UtilProperties.getMessage(resource,"variantevents.productId_required_but_missing", UtilHttp.getLocale(request));
            request.setAttribute("_ERROR_MESSAGE_", errMsg);
            return "error";
        }

        if (UtilValidate.isEmpty(variantProductId)) {
            errMsg = UtilProperties.getMessage(resource,"variantevents.variantProductId_required_but_missing_enter_an_id", UtilHttp.getLocale(request));
            request.setAttribute("_ERROR_MESSAGE_", errMsg);
            return "error";
        }

        int featureTypeSize = 0;

        try {
            featureTypeSize = Integer.parseInt(featureTypeSizeStr);
        } catch (NumberFormatException e) {
            Map messageMap = UtilMisc.toMap("featureTypeSizeStr", featureTypeSizeStr);
            errMsg = UtilProperties.getMessage(resource,"variantevents.featureTypeSize_not_number", messageMap, UtilHttp.getLocale(request));
            request.setAttribute("_ERROR_MESSAGE_", errMsg);
            return "error";
        }

        try {
            boolean beganTransacton = TransactionUtil.begin();

            try {
                // read the product, duplicate it with the given id
                GenericValue product = delegator.findByPrimaryKey("Product", UtilMisc.toMap("productId", productId));
                if (product == null) {
                    Map messageMap = UtilMisc.toMap("productId", productId);
                    errMsg = UtilProperties.getMessage(resource,"variantevents.product_not_found_with_ID", messageMap, UtilHttp.getLocale(request));
                    TransactionUtil.rollback(beganTransacton, errMsg, null);
                    request.setAttribute("_ERROR_MESSAGE_", errMsg);
                    return "error";
                }

                // check if product exists
                GenericValue variantProduct = delegator.findByPrimaryKey("Product",UtilMisc.toMap("productId", variantProductId));
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
                        Map messageMap = UtilMisc.toMap("i", Integer.toString(i));
                        errMsg = UtilProperties.getMessage(resource,"variantevents.productFeatureId_for_feature_type_number_not_found", messageMap, UtilHttp.getLocale(request));
                        TransactionUtil.rollback(beganTransacton, errMsg, null);
                        request.setAttribute("_ERROR_MESSAGE_", errMsg);
                        return "error";
                    }

                    GenericValue productFeature = delegator.findByPrimaryKey("ProductFeature", UtilMisc.toMap("productFeatureId", productFeatureId));

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
                Map messageMap = UtilMisc.toMap("errMessage", e.toString());
                errMsg = UtilProperties.getMessage(resource,"variantevents.entity_error_quick_add_variant_data", messageMap, UtilHttp.getLocale(request));
                TransactionUtil.rollback(beganTransacton, errMsg, null);
                Debug.logError(e, "Entity error creating quick add variant data", module);
                request.setAttribute("_ERROR_MESSAGE_", errMsg);
                return "error";
            }
        } catch (GenericTransactionException e) {
            Debug.logError(e, "Transaction error creating quick add variant data", module);
            Map messageMap = UtilMisc.toMap("errMessage", e.toString());
            errMsg = UtilProperties.getMessage(resource,"variantevents.transaction_error_quick_add_variant_data", messageMap, UtilHttp.getLocale(request));
            request.setAttribute("_ERROR_MESSAGE_", errMsg);
            return "error";
        }

        Map messageMap = UtilMisc.toMap("variantProductId", variantProductId);
        String sucMsg = UtilProperties.getMessage(resource,"variantevents.successfully_created_variant_product_with_id", messageMap, UtilHttp.getLocale(request));
        request.setAttribute("_EVENT_MESSAGE_", sucMsg);
        return "success";
    }
}
