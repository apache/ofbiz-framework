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
package org.ofbiz.product.product;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.sql.Timestamp;
import java.util.*;

import javolution.util.FastList;
import javolution.util.FastMap;
import javolution.util.FastSet;

import org.jdom.JDOMException;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilGenerics;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.string.FlexibleStringExpander;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityJoinOperator;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.product.image.ScaleImage;
import org.ofbiz.product.catalog.CatalogWorker;
import org.ofbiz.product.category.CategoryWorker;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ModelService;
import org.ofbiz.service.ServiceUtil;

/**
 * Product Services
 */
public class ProductServices {

    public static final String module = ProductServices.class.getName();
    public static final String resource = "ProductErrorUiLabels";

    /**
     * Creates a Collection of product entities which are variant products from the specified product ID.
     */
    public static Map<String, Object> prodFindAllVariants(DispatchContext dctx, Map<String, ? extends Object> context) {
        // * String productId      -- Parent (virtual) product ID
        Map<String, Object> subContext = UtilMisc.makeMapWritable(context);
        subContext.put("type", "PRODUCT_VARIANT");
        return prodFindAssociatedByType(dctx, subContext);
    }

    /**
     * Finds a specific product or products which contain the selected features.
     */
    public static Map<String, Object> prodFindSelectedVariant(DispatchContext dctx, Map<String, ? extends Object> context) {
        // * String productId      -- Parent (virtual) product ID
        // * Map selectedFeatures  -- Selected features
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get("locale");
        Map selectedFeatures = UtilGenerics.checkMap(context.get("selectedFeatures"));
        List<GenericValue> products = FastList.newInstance();
        // All the variants for this products are retrieved
        Map<String, Object> resVariants = prodFindAllVariants(dctx, context);
        List<GenericValue> variants = UtilGenerics.checkList(resVariants.get("assocProducts"));
        for (GenericValue oneVariant: variants) {
            // For every variant, all the standard features are retrieved
            Map<String, String> feaContext = FastMap.newInstance();
            feaContext.put("productId", oneVariant.getString("productIdTo"));
            feaContext.put("type", "STANDARD_FEATURE");
            Map<String, Object> resFeatures = prodGetFeatures(dctx, feaContext);
            List<GenericValue> features = UtilGenerics.checkList(resFeatures.get("productFeatures"));
            boolean variantFound = true;
            // The variant is discarded if at least one of its standard features
            // has the same type of one of the selected features but a different feature id.
            // Example:
            // Input: (COLOR, Black), (SIZE, Small)
            // Variant1: (COLOR, Black), (SIZE, Large) --> nok
            // Variant2: (COLOR, Black), (SIZE, Small) --> ok
            // Variant3: (COLOR, Black), (SIZE, Small), (IMAGE, SkyLine) --> ok
            // Variant4: (COLOR, Black), (IMAGE, SkyLine) --> ok
            for (GenericValue oneFeature: features) {
                if (selectedFeatures.containsKey(oneFeature.getString("productFeatureTypeId"))) {
                    if (!selectedFeatures.containsValue(oneFeature.getString("productFeatureId"))) {
                        variantFound = false;
                        break;
                    }
                }
            }
            if (variantFound) {
                try {
                    products.add(delegator.findByPrimaryKey("Product", UtilMisc.toMap("productId", oneVariant.getString("productIdTo"))));
                } catch (GenericEntityException e) {
                    Map<String, String> messageMap = UtilMisc.toMap("errProductFeatures", e.toString());
                    String errMsg = UtilProperties.getMessage(resource,"productservices.problem_reading_product_features_errors", messageMap, locale);
                    Debug.logError(e, errMsg, module);
                    return ServiceUtil.returnError(errMsg);
                }
            }
        }

        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put("products", products);
        return result;
    }

    /**
     * Finds product variants based on a product ID and a distinct feature.
     */
    public static Map<String, Object> prodFindDistinctVariants(DispatchContext dctx, Map<String, ? extends Object> context) {
        // * String productId      -- Parent (virtual) product ID
        // * String feature        -- Distinct feature name
        Delegator delegator = dctx.getDelegator();
        String productId = (String) context.get("productId");
        String feature = (String) context.get("feature");

        return ServiceUtil.returnError("This service has not yet been implemented.");
    }

    /**
     * Finds a Set of feature types in sequence.
     */
    public static Map<String, Object> prodFindFeatureTypes(DispatchContext dctx, Map<String, ? extends Object> context) {
        // * String productId      -- Product ID to look up feature types
        Delegator delegator = dctx.getDelegator();
        String productId = (String) context.get("productId");
        String productFeatureApplTypeId = (String) context.get("productFeatureApplTypeId");
        if (UtilValidate.isEmpty(productFeatureApplTypeId)) {
            productFeatureApplTypeId = "SELECTABLE_FEATURE";
        }
        Locale locale = (Locale) context.get("locale");
        String errMsg=null;
        Set<String> featureSet = new LinkedHashSet<String>();

        try {
            Map<String, String> fields = UtilMisc.toMap("productId", productId, "productFeatureApplTypeId", productFeatureApplTypeId);
            List<String> order = UtilMisc.toList("sequenceNum", "productFeatureTypeId");
            List<GenericValue> features = delegator.findByAndCache("ProductFeatureAndAppl", fields, order);
            for (GenericValue v: features) {
                featureSet.add(v.getString("productFeatureTypeId"));
            }
            //if (Debug.infoOn()) Debug.logInfo("" + featureSet, module);
        } catch (GenericEntityException e) {
            Map<String, String> messageMap = UtilMisc.toMap("errProductFeatures", e.toString());
            errMsg = UtilProperties.getMessage(resource,"productservices.problem_reading_product_features_errors", messageMap, locale);
            Debug.logError(e, errMsg, module);
            return ServiceUtil.returnError(errMsg);
        }

        if (featureSet.size() == 0) {
            errMsg = UtilProperties.getMessage(resource,"productservices.problem_reading_product_features", locale);
            // ToDo DO 2004-02-23 Where should the errMsg go?
            Debug.logWarning(errMsg + " for product " + productId, module);
            //return ServiceUtil.returnError(errMsg);
        }
        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put("featureSet", featureSet);
        return result;
    }

    /**
     * Builds a variant feature tree.
     */
    public static Map<String, Object> prodMakeFeatureTree(DispatchContext dctx, Map<String, ? extends Object> context) {
        // * String productId      -- Parent (virtual) product ID
        // * List featureOrder     -- Order of features
        // * Boolean checkInventory-- To calculate available inventory.
        // * String productStoreId -- Product Store ID for Inventory
        String productStoreId = (String) context.get("productStoreId");
        Locale locale = (Locale) context.get("locale");

        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Map<String, Object> result = FastMap.newInstance();
        List<String> featureOrder = UtilMisc.makeListWritable(UtilGenerics.<String>checkCollection(context.get("featureOrder")));

        if (UtilValidate.isEmpty(featureOrder)) {
            return ServiceUtil.returnError("Empty list of features passed");
        }

        List<GenericValue> variants = UtilGenerics.checkList(prodFindAllVariants(dctx, context).get("assocProducts"));
        List<String> virtualVariant = FastList.newInstance();

        if (UtilValidate.isEmpty(variants)) {
            return ServiceUtil.returnSuccess();
        }
        List<String> items = FastList.newInstance();
        List<GenericValue> outOfStockItems = FastList.newInstance();

        for (GenericValue variant: variants) {
            String productIdTo = variant.getString("productIdTo");

            // first check to see if intro and discontinue dates are within range
            GenericValue productTo = null;

            try {
                productTo = delegator.findByPrimaryKeyCache("Product", UtilMisc.toMap("productId", productIdTo));
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
                Map<String, String> messageMap = UtilMisc.toMap("productIdTo", productIdTo, "errMessage", e.toString());
                return ServiceUtil.returnError(UtilProperties.getMessage(resource, "productservices.error_finding_associated_variant_with_ID_error", messageMap, locale));
            }
            if (productTo == null) {
                Debug.logWarning("Could not find associated variant with ID " + productIdTo + ", not showing in list", module);
                continue;
            }

            java.sql.Timestamp nowTimestamp = UtilDateTime.nowTimestamp();

            // check to see if introductionDate hasn't passed yet
            if (productTo.get("introductionDate") != null && nowTimestamp.before(productTo.getTimestamp("introductionDate"))) {
                if (Debug.verboseOn()) {
                    String excMsg = "Tried to view the Product " + productTo.getString("productName") +
                        " (productId: " + productTo.getString("productId") + ") as a variant. This product has not yet been made available for sale, so not adding for view.";

                    Debug.logVerbose(excMsg, module);
                }
                continue;
            }

            // check to see if salesDiscontinuationDate has passed
            if (productTo.get("salesDiscontinuationDate") != null && nowTimestamp.after(productTo.getTimestamp("salesDiscontinuationDate"))) {
                if (Debug.verboseOn()) {
                    String excMsg = "Tried to view the Product " + productTo.getString("productName") +
                        " (productId: " + productTo.getString("productId") + ") as a variant. This product is no longer available for sale, so not adding for view.";

                    Debug.logVerbose(excMsg, module);
                }
                continue;
            }

            // next check inventory for each item: if inventory is not required or is available
            Boolean checkInventory = (Boolean) context.get("checkInventory");
            try {
                if (checkInventory) {
                    Map<String, Object> invReqResult = dispatcher.runSync("isStoreInventoryAvailableOrNotRequired", UtilMisc.<String, Object>toMap("productStoreId", productStoreId, "productId", productIdTo, "quantity", BigDecimal.ONE));
                    if (ServiceUtil.isError(invReqResult)) {
                        return ServiceUtil.returnError("Error calling the isStoreInventoryRequired when building the variant product tree.", null, null, invReqResult);
                    } else if ("Y".equals((String) invReqResult.get("availableOrNotRequired"))) {
                        items.add(productIdTo);
                        if (productTo.getString("isVirtual") != null && productTo.getString("isVirtual").equals("Y")) {
                            virtualVariant.add(productIdTo);
                        }
                    } else {
                        outOfStockItems.add(productTo);
                    }
                } else {
                    items.add(productIdTo);
                    if (productTo.getString("isVirtual") != null && productTo.getString("isVirtual").equals("Y")) {
                        virtualVariant.add(productIdTo);
                    }
                }
            } catch (GenericServiceException e) {
                String errMsg = "Error calling the isStoreInventoryRequired when building the variant product tree: " + e.toString();
                Debug.logError(e, errMsg, module);
                return ServiceUtil.returnError(errMsg);
            }
        }

        String productId = (String) context.get("productId");

        // Make the selectable feature list
        List<GenericValue> selectableFeatures = null;
        try {
            Map<String, String> fields = UtilMisc.toMap("productId", productId, "productFeatureApplTypeId", "SELECTABLE_FEATURE");
            List<String> sort = UtilMisc.toList("sequenceNum");

            selectableFeatures = delegator.findByAndCache("ProductFeatureAndAppl", fields, sort);
            selectableFeatures = EntityUtil.filterByDate(selectableFeatures, true);
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource,"productservices.empty_list_of_selectable_features_found", locale));
        }
        Map<String, List<String>> features = FastMap.newInstance();
        for (GenericValue v: selectableFeatures) {
            String featureType = v.getString("productFeatureTypeId");
            String feature = v.getString("description");

            if (!features.containsKey(featureType)) {
                List<String> featureList = FastList.newInstance();
                featureList.add(feature);
                features.put(featureType, featureList);
            } else {
                List<String> featureList = features.get(featureType);
                featureList.add(feature);
                features.put(featureType, featureList);
            }
        }

        Map<String, Object> tree = null;
        try {
            tree = makeGroup(delegator, features, items, featureOrder, 0);
        } catch (Exception e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }
        if (UtilValidate.isEmpty(tree)) {
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_ERROR);
            result.put(ModelService.ERROR_MESSAGE, UtilProperties.getMessage(resource,"productservices.feature_grouping_came_back_empty", locale));
        } else {
            result.put("variantTree", tree);
            result.put("virtualVariant", virtualVariant);
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
        }

        Map<String, GenericValue> sample = null;
        try {
            sample = makeVariantSample(dctx.getDelegator(), features, items, featureOrder.get(0));
        } catch (Exception e) {
            return ServiceUtil.returnError(e.getMessage());
        }

        if (outOfStockItems.size() > 0) {
            result.put("unavailableVariants", outOfStockItems);
        }
        result.put("variantSample", sample);
        result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);

        return result;
    }

    /**
     * Gets the product features of a product.
     */
    public static Map<String, Object> prodGetFeatures(DispatchContext dctx, Map<String, ? extends Object> context) {
        // * String productId      -- Product ID to find
        // * String type           -- Type of feature (STANDARD_FEATURE, SELECTABLE_FEATURE)
        // * String distinct       -- Distinct feature (SIZE, COLOR)
        Delegator delegator = dctx.getDelegator();
        Map<String, Object> result = FastMap.newInstance();
        String productId = (String) context.get("productId");
        String distinct = (String) context.get("distinct");
        String type = (String) context.get("type");
        Locale locale = (Locale) context.get("locale");
        String errMsg=null;
        List<GenericValue> features = null;

        try {
            Map<String, String> fields = UtilMisc.toMap("productId", productId);
            List<String> order = UtilMisc.toList("sequenceNum", "productFeatureTypeId");

            if (distinct != null) fields.put("productFeatureTypeId", distinct);
            if (type != null) fields.put("productFeatureApplTypeId", type);
            features = delegator.findByAndCache("ProductFeatureAndAppl", fields, order);
            result.put("productFeatures", features);
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
        } catch (GenericEntityException e) {
            Map<String, String> messageMap = UtilMisc.toMap("errMessage", e.toString());
            errMsg = UtilProperties.getMessage(resource,"productservices.problem_reading_product_feature_entity", messageMap, locale);
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_ERROR);
            result.put(ModelService.ERROR_MESSAGE, errMsg);
        }
        return result;
    }

    /**
     * Finds a product by product ID.
     */
    public static Map<String, Object> prodFindProduct(DispatchContext dctx, Map<String, ? extends Object> context) {
        // * String productId      -- Product ID to find
        Delegator delegator = dctx.getDelegator();
        Map<String, Object> result = FastMap.newInstance();
        String productId = (String) context.get("productId");
        Locale locale = (Locale) context.get("locale");
        String errMsg = null;

        if (UtilValidate.isEmpty(productId)) {
            errMsg = UtilProperties.getMessage(resource,"productservices.invalid_productId_passed", locale);
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_ERROR);
            result.put(ModelService.ERROR_MESSAGE, errMsg);
            return result;
        }

        try {
            GenericValue product = delegator.findByPrimaryKeyCache("Product", UtilMisc.toMap("productId", productId));
            GenericValue mainProduct = product;

            if (product.get("isVariant") != null && product.getString("isVariant").equalsIgnoreCase("Y")) {
                List<GenericValue> c = product.getRelatedByAndCache("AssocProductAssoc",
                        UtilMisc.toMap("productAssocTypeId", "PRODUCT_VARIANT"));
                //if (Debug.infoOn()) Debug.logInfo("Found related: " + c, module);
                c = EntityUtil.filterByDate(c);
                //if (Debug.infoOn()) Debug.logInfo("Found Filtered related: " + c, module);
                if (c.size() > 0) {
                    GenericValue asV = c.iterator().next();

                    //if (Debug.infoOn()) Debug.logInfo("ASV: " + asV, module);
                    mainProduct = asV.getRelatedOneCache("MainProduct");
                    //if (Debug.infoOn()) Debug.logInfo("Main product = " + mainProduct, module);
                }
            }
            result.put("product", mainProduct);
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
        } catch (GenericEntityException e) {
            e.printStackTrace();
            Map<String, String> messageMap = UtilMisc.toMap("errMessage", e.getMessage());
            errMsg = UtilProperties.getMessage(resource,"productservices.problems_reading_product_entity", messageMap, locale);
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_ERROR);
            result.put(ModelService.ERROR_MESSAGE, errMsg);
        }

        return result;
    }

    /**
     * Finds associated products by product ID and association ID.
     */
    public static Map<String, Object> prodFindAssociatedByType(DispatchContext dctx, Map<String, ? extends Object> context) {
        // * String productId      -- Current Product ID
        // * String type           -- Type of association (ie PRODUCT_UPGRADE, PRODUCT_COMPLEMENT, PRODUCT_VARIANT)
        Delegator delegator = dctx.getDelegator();
        Map<String, Object> result = FastMap.newInstance();
        String productId = (String) context.get("productId");
        String productIdTo = (String) context.get("productIdTo");
        String type = (String) context.get("type");
        Locale locale = (Locale) context.get("locale");
        String errMsg = null;

        Boolean cvaBool = (Boolean) context.get("checkViewAllow");
        boolean checkViewAllow = (cvaBool == null ? false : cvaBool);
        String prodCatalogId = (String) context.get("prodCatalogId");
        Boolean bidirectional = (Boolean) context.get("bidirectional");
        bidirectional = bidirectional == null ? false : bidirectional;
        Boolean sortDescending = (Boolean) context.get("sortDescending");
        sortDescending = sortDescending == null ? false : sortDescending;

        if (productId == null && productIdTo == null) {
            errMsg = UtilProperties.getMessage(resource,"productservices.both_productId_and_productIdTo_cannot_be_null", locale);
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_ERROR);
            result.put(ModelService.ERROR_MESSAGE, errMsg);
            return result;
        }

        if (productId != null && productIdTo != null) {
            errMsg = UtilProperties.getMessage(resource,"productservices.both_productId_and_productIdTo_cannot_be_defined", locale);
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_ERROR);
            result.put(ModelService.ERROR_MESSAGE, errMsg);
            return result;
        }

        productId = productId == null ? productIdTo : productId;
        GenericValue product = null;

        try {
            product = delegator.findByPrimaryKeyCache("Product", UtilMisc.toMap("productId", productId));
        } catch (GenericEntityException e) {
            Map<String, String> messageMap = UtilMisc.toMap("errMessage", e.getMessage());
            errMsg = UtilProperties.getMessage(resource,"productservices.problems_reading_product_entity", messageMap, locale);
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_ERROR);
            result.put(ModelService.ERROR_MESSAGE, errMsg);
            return result;
        }

        if (product == null) {
            errMsg = UtilProperties.getMessage(resource,"productservices.problems_getting_product_entity", locale);
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_ERROR);
            result.put(ModelService.ERROR_MESSAGE, errMsg);
            return result;
        }

        try {
            List<GenericValue> productAssocs = null;

            List<String> orderBy = FastList.newInstance();
            if (sortDescending) {
                orderBy.add("sequenceNum DESC");
            } else {
                orderBy.add("sequenceNum");
            }

            if (bidirectional) {
                EntityCondition cond = EntityCondition.makeCondition(
                        UtilMisc.toList(
                                EntityCondition.makeCondition("productId", productId),
                                EntityCondition.makeCondition("productIdTo", productId)
                       ), EntityJoinOperator.OR);
                cond = EntityCondition.makeCondition(cond, EntityCondition.makeCondition("productAssocTypeId", type));
                productAssocs = delegator.findList("ProductAssoc", cond, null, orderBy, null, true);
            } else {
                if (productIdTo == null) {
                    productAssocs = product.getRelatedCache("MainProductAssoc", UtilMisc.toMap("productAssocTypeId", type), orderBy);
                } else {
                    productAssocs = product.getRelatedCache("AssocProductAssoc", UtilMisc.toMap("productAssocTypeId", type), orderBy);
                }
            }
            // filter the list by date
            productAssocs = EntityUtil.filterByDate(productAssocs);
            // first check to see if there is a view allow category and if these products are in it...
            if (checkViewAllow && prodCatalogId != null && UtilValidate.isNotEmpty(productAssocs)) {
                String viewProductCategoryId = CatalogWorker.getCatalogViewAllowCategoryId(delegator, prodCatalogId);
                if (viewProductCategoryId != null) {
                    if (productIdTo == null) {
                        productAssocs = CategoryWorker.filterProductsInCategory(delegator, productAssocs, viewProductCategoryId, "productIdTo");
                    } else {
                        productAssocs = CategoryWorker.filterProductsInCategory(delegator, productAssocs, viewProductCategoryId, "productId");
                    }
                }
            }


            result.put("assocProducts", productAssocs);
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
        } catch (GenericEntityException e) {
            Map<String, String> messageMap = UtilMisc.toMap("errMessage", e.getMessage());
            errMsg = UtilProperties.getMessage(resource,"productservices.problems_product_association_relation_error", messageMap, locale);
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_ERROR);
            result.put(ModelService.ERROR_MESSAGE, errMsg);
            return result;
        }

        return result;
    }

    // Builds a product feature tree
    private static Map<String, Object> makeGroup(Delegator delegator, Map<String, List<String>> featureList, List<String> items, List<String> order, int index)
        throws IllegalArgumentException, IllegalStateException {
        //List featureKey = FastList.newInstance();
        Map<String, List<String>> tempGroup = FastMap.newInstance();
        Map<String, Object> group = new LinkedHashMap<String, Object>();
        String orderKey = (String) order.get(index);

        if (featureList == null) {
            throw new IllegalArgumentException("Cannot build feature tree: featureList is null");
        }

        if (index < 0) {
            throw new IllegalArgumentException("Invalid index '" + index + "' min index '0'");
        }
        if (index + 1 > order.size()) {
            throw new IllegalArgumentException("Invalid index '" + index + "' max index '" + (order.size() - 1) + "'");
        }

        // loop through items and make the lists
        for (String thisItem: items) {
            // -------------------------------
            // Gather the necessary data
            // -------------------------------

            if (Debug.verboseOn()) Debug.logVerbose("ThisItem: " + thisItem, module);
            List<GenericValue> features = null;

            try {
                Map<String, String> fields = UtilMisc.toMap("productId", thisItem, "productFeatureTypeId", orderKey,
                        "productFeatureApplTypeId", "STANDARD_FEATURE");
                List<String> sort = UtilMisc.toList("sequenceNum");

                // get the features and filter out expired dates
                features = delegator.findByAndCache("ProductFeatureAndAppl", fields, sort);
                features = EntityUtil.filterByDate(features, true);
            } catch (GenericEntityException e) {
                throw new IllegalStateException("Problem reading relation: " + e.getMessage());
            }
            if (Debug.verboseOn()) Debug.logVerbose("Features: " + features, module);

            // -------------------------------
            for (GenericValue item: features) {
                String itemKey = item.getString("description");

                if (tempGroup.containsKey(itemKey)) {
                    List<String> itemList = tempGroup.get(itemKey);

                    if (!itemList.contains(thisItem))
                        itemList.add(thisItem);
                } else {
                    List<String> itemList = UtilMisc.toList(thisItem);

                    tempGroup.put(itemKey, itemList);
                }
            }
        }
        if (Debug.verboseOn()) Debug.logVerbose("TempGroup: " + tempGroup, module);

        // Loop through the feature list and order the keys in the tempGroup
        List<String> orderFeatureList = featureList.get(orderKey);

        if (orderFeatureList == null) {
            throw new IllegalArgumentException("Cannot build feature tree: orderFeatureList is null for orderKey=" + orderKey);
        }

        for (String featureStr: orderFeatureList) {
            if (tempGroup.containsKey(featureStr))
                group.put(featureStr, tempGroup.get(featureStr));
        }

        if (Debug.verboseOn()) Debug.logVerbose("Group: " + group, module);

        // no groups; no tree
        if (group.size() == 0) {
            return group;
            //throw new IllegalStateException("Cannot create tree from group list; error on '" + orderKey + "'");
        }

        if (index + 1 == order.size()) {
            return group;
        }

        // loop through the keysets and get the sub-groups
        for (String key: group.keySet()) {
            List<String> itemList = UtilGenerics.checkList(group.get(key));

            if (UtilValidate.isNotEmpty(itemList)) {
                Map<String, Object> subGroup = makeGroup(delegator, featureList, itemList, order, index + 1);
                group.put(key, subGroup);
            } else {
                // do nothing, ie put nothing in the Map
                //throw new IllegalStateException("Cannot create tree from an empty list; error on '" + key + "'");
            }
        }
        return group;
    }

    // builds a variant sample (a single sku for a featureType)
    private static Map<String, GenericValue> makeVariantSample(Delegator delegator, Map<String, List<String>> featureList, List<String> items, String feature) {
        Map<String, GenericValue> tempSample = FastMap.newInstance();
        Map<String, GenericValue> sample = new LinkedHashMap<String, GenericValue>();
        for (String productId: items) {
            List<GenericValue> features = null;

            try {
                Map<String, String> fields = UtilMisc.toMap("productId", productId, "productFeatureTypeId", feature,
                        "productFeatureApplTypeId", "STANDARD_FEATURE");
                List<String> sort = UtilMisc.toList("sequenceNum", "description");

                // get the features and filter out expired dates
                features = delegator.findByAndCache("ProductFeatureAndAppl", fields, sort);
                features = EntityUtil.filterByDate(features, true);
            } catch (GenericEntityException e) {
                throw new IllegalStateException("Problem reading relation: " + e.getMessage());
            }
            for (GenericValue featureAppl: features) {
                try {
                    GenericValue product = delegator.findByPrimaryKeyCache("Product",
                            UtilMisc.toMap("productId", productId));

                    tempSample.put(featureAppl.getString("description"), product);
                } catch (GenericEntityException e) {
                    throw new RuntimeException("Cannot get product entity: " + e.getMessage());
                }
            }
        }

        // Sort the sample based on the feature list.
        List<String> features = featureList.get(feature);
        for (String f: features) {
            if (tempSample.containsKey(f))
                sample.put(f, tempSample.get(f));
        }

        return sample;
    }

    public static Map<String, Object> quickAddVariant(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        Map<String, Object> result = FastMap.newInstance();
        Locale locale = (Locale) context.get("locale");
        String errMsg=null;
        String productId = (String) context.get("productId");
        String variantProductId = (String) context.get("productVariantId");
        String productFeatureIds = (String) context.get("productFeatureIds");
        Long prodAssocSeqNum = (Long) context.get("sequenceNum");

        try {
            // read the product, duplicate it with the given id
            GenericValue product = delegator.findByPrimaryKey("Product", UtilMisc.toMap("productId", productId));
            if (product == null) {
                Map<String, String> messageMap = UtilMisc.toMap("productId", productId);
                errMsg = UtilProperties.getMessage(resource,"productservices.product_not_found_with_ID", messageMap, locale);
                result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_ERROR);
                result.put(ModelService.ERROR_MESSAGE, errMsg);
                return result;
            }
            // check if product exists
            GenericValue variantProduct = delegator.findByPrimaryKey("Product",UtilMisc.toMap("productId", variantProductId));
            boolean variantProductExists = (variantProduct != null);
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
            if (variantProductExists) {
                // Since the variant product is already a variant, first of all we remove the old features
                // and the associations of type PRODUCT_VARIANT: a given product can be a variant of only one product.
                delegator.removeByAnd("ProductAssoc", UtilMisc.toMap("productIdTo", variantProductId,
                                                                     "productAssocTypeId", "PRODUCT_VARIANT"));
                delegator.removeByAnd("ProductFeatureAppl", UtilMisc.toMap("productId", variantProductId,
                                                                           "productFeatureApplTypeId", "STANDARD_FEATURE"));
            }
            // add an association from productId to variantProductId of the PRODUCT_VARIANT
            Map<String, Object> productAssocMap = UtilMisc.toMap("productId", productId, "productIdTo", variantProductId,
                                                 "productAssocTypeId", "PRODUCT_VARIANT",
                                                 "fromDate", UtilDateTime.nowTimestamp());
            if (prodAssocSeqNum != null) {
                productAssocMap.put("sequenceNum", prodAssocSeqNum);
            }
            GenericValue productAssoc = delegator.makeValue("ProductAssoc", productAssocMap);
            productAssoc.create();

            // add the selected standard features to the new product given the productFeatureIds
            java.util.StringTokenizer st = new java.util.StringTokenizer(productFeatureIds, "|");
            while (st.hasMoreTokens()) {
                String productFeatureId = st.nextToken();

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

        } catch (GenericEntityException e) {
            Debug.logError(e, "Entity error creating quick add variant data", module);
            Map<String, String> messageMap = UtilMisc.toMap("errMessage", e.toString());
            errMsg = UtilProperties.getMessage(resource,"productservices.entity_error_quick_add_variant_data", messageMap, locale);
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_ERROR);
            result.put(ModelService.ERROR_MESSAGE, errMsg);
            return result;
        }
        result.put("productVariantId", variantProductId);
        return result;
    }

    /**
     * This will create a virtual product and return its ID, and associate all of the variants with it.
     * It will not put the selectable features on the virtual or standard features on the variant.
     */
    public static Map<String, Object> quickCreateVirtualWithVariants(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        Timestamp nowTimestamp = UtilDateTime.nowTimestamp();

        // get the various IN attributes
        String variantProductIdsBag = (String) context.get("variantProductIdsBag");
        String productFeatureIdOne = (String) context.get("productFeatureIdOne");
        String productFeatureIdTwo = (String) context.get("productFeatureIdTwo");
        String productFeatureIdThree = (String) context.get("productFeatureIdThree");

        Map<String, Object> successResult = ServiceUtil.returnSuccess();

        try {
            // Generate new virtual productId, prefix with "VP", put in successResult
            String productId = (String) context.get("productId");

            if (UtilValidate.isEmpty(productId)) {
                productId = "VP" + delegator.getNextSeqId("Product");
                // Create new virtual product...
                GenericValue product = delegator.makeValue("Product");
                product.set("productId", productId);
                // set: isVirtual=Y, isVariant=N, productTypeId=FINISHED_GOOD, introductionDate=now
                product.set("isVirtual", "Y");
                product.set("isVariant", "N");
                product.set("productTypeId", "FINISHED_GOOD");
                product.set("introductionDate", nowTimestamp);
                // set all to Y: returnable, taxable, chargeShipping, autoCreateKeywords, includeInPromotions
                product.set("returnable", "Y");
                product.set("taxable", "Y");
                product.set("chargeShipping", "Y");
                product.set("autoCreateKeywords", "Y");
                product.set("includeInPromotions", "Y");
                // in it goes!
                product.create();
            }
            successResult.put("productId", productId);

            // separate variantProductIdsBag into a Set of variantProductIds
            //note: can be comma, tab, or white-space delimited
            Set<String> prelimVariantProductIds = FastSet.newInstance();
            List<String> splitIds = Arrays.asList(variantProductIdsBag.split("[,\\p{Space}]"));
            Debug.logInfo("Variants: bag=" + variantProductIdsBag, module);
            Debug.logInfo("Variants: split=" + splitIds, module);
            prelimVariantProductIds.addAll(splitIds);
            //note: should support both direct productIds and GoodIdentification entries (what to do if more than one GoodID? Add all?

            Map<String, GenericValue> variantProductsById = FastMap.newInstance();
            for (String variantProductId: prelimVariantProductIds) {
                if (UtilValidate.isEmpty(variantProductId)) {
                    // not sure why this happens, but seems to from time to time with the split method
                    continue;
                }
                // is a Product.productId?
                GenericValue variantProduct = delegator.findByPrimaryKey("Product", UtilMisc.toMap("productId", variantProductId));
                if (variantProduct != null) {
                    variantProductsById.put(variantProductId, variantProduct);
                } else {
                    // is a GoodIdentification.idValue?
                    List<GenericValue> goodIdentificationList = delegator.findByAnd("GoodIdentification", UtilMisc.toMap("idValue", variantProductId));
                    if (UtilValidate.isEmpty(goodIdentificationList)) {
                        // whoops, nothing found... return error
                        return ServiceUtil.returnError("Error creating a virtual with variants: the ID [" + variantProductId + "] is not a valid Product.productId or a GoodIdentification.idValue");
                    }

                    if (goodIdentificationList.size() > 1) {
                        // what to do here? for now just log a warning and add all of them as variants; they can always be dissociated later
                        Debug.logWarning("Warning creating a virtual with variants: the ID [" + variantProductId + "] was not a productId and resulted in [" + goodIdentificationList.size() + "] GoodIdentification records: " + goodIdentificationList, module);
                    }

                    for (GenericValue goodIdentification: goodIdentificationList) {
                        GenericValue giProduct = goodIdentification.getRelatedOne("Product");
                        if (giProduct != null) {
                            variantProductsById.put(giProduct.getString("productId"), giProduct);
                        }
                    }
                }
            }

            // Attach productFeatureIdOne, Two, Three to the new virtual and all variant products as a standard feature
            Set<String> featureProductIds = FastSet.newInstance();
            featureProductIds.add(productId);
            featureProductIds.addAll(variantProductsById.keySet());
            Set<String> productFeatureIds = FastSet.newInstance();
            productFeatureIds.add(productFeatureIdOne);
            productFeatureIds.add(productFeatureIdTwo);
            productFeatureIds.add(productFeatureIdThree);

            for (String featureProductId: featureProductIds) {
                for (String productFeatureId: productFeatureIds) {
                    if (UtilValidate.isNotEmpty(productFeatureId)) {
                        GenericValue productFeatureAppl = delegator.makeValue("ProductFeatureAppl",
                                UtilMisc.toMap("productId", featureProductId, "productFeatureId", productFeatureId,
                                        "productFeatureApplTypeId", "STANDARD_FEATURE", "fromDate", nowTimestamp));
                        productFeatureAppl.create();
                    }
                }
            }

            for (GenericValue variantProduct: variantProductsById.values()) {
                // for each variant product set: isVirtual=N, isVariant=Y, introductionDate=now
                variantProduct.set("isVirtual", "N");
                variantProduct.set("isVariant", "Y");
                variantProduct.set("introductionDate", nowTimestamp);
                variantProduct.store();

                // for each variant product create associate with the new virtual as a PRODUCT_VARIANT
                GenericValue productAssoc = delegator.makeValue("ProductAssoc",
                        UtilMisc.toMap("productId", productId, "productIdTo", variantProduct.get("productId"),
                                "productAssocTypeId", "PRODUCT_VARIANT", "fromDate", nowTimestamp));
                productAssoc.create();
            }
        } catch (GenericEntityException e) {
            String errMsg = "Error creating new virtual product from variant products: " + e.toString();
            Debug.logError(e, errMsg, module);
            return ServiceUtil.returnError(errMsg);
        }
        return successResult;
    }

    public static Map<String, Object> updateProductIfAvailableFromShipment(DispatchContext dctx, Map<String, ? extends Object> context) {
        if ("Y".equals(UtilProperties.getPropertyValue("catalog.properties", "reactivate.product.from.receipt", "N"))) {
            LocalDispatcher dispatcher = dctx.getDispatcher();
            Delegator delegator = dctx.getDelegator();
            GenericValue userLogin = (GenericValue) context.get("userLogin");
            String inventoryItemId = (String) context.get("inventoryItemId");

            GenericValue inventoryItem = null;
            try {
                inventoryItem = delegator.findByPrimaryKeyCache("InventoryItem", UtilMisc.toMap("inventoryItemId", inventoryItemId));
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
                return ServiceUtil.returnError(e.getMessage());
            }

            if (inventoryItem != null) {
                String productId = inventoryItem.getString("productId");
                GenericValue product = null;
                try {
                    product = delegator.findByPrimaryKeyCache("Product", UtilMisc.toMap("productId", productId));
                } catch (GenericEntityException e) {
                    Debug.logError(e, module);
                    return ServiceUtil.returnError(e.getMessage());
                }

                if (product != null) {
                    Timestamp salesDiscontinuationDate = product.getTimestamp("salesDiscontinuationDate");
                    if (salesDiscontinuationDate != null && salesDiscontinuationDate.before(UtilDateTime.nowTimestamp())) {
                        Map<String, Object> invRes = null;
                        try {
                            invRes = dispatcher.runSync("getProductInventoryAvailable", UtilMisc.<String, Object>toMap("productId", productId, "userLogin", userLogin));
                        } catch (GenericServiceException e) {
                            Debug.logError(e, module);
                            return ServiceUtil.returnError(e.getMessage());
                        }

                        BigDecimal availableToPromiseTotal = (BigDecimal) invRes.get("availableToPromiseTotal");
                        if (availableToPromiseTotal != null && availableToPromiseTotal.compareTo(BigDecimal.ZERO) > 0) {
                            // refresh the product so we can update it
                            GenericValue productToUpdate = null;
                            try {
                                productToUpdate = delegator.findByPrimaryKey("Product", product.getPrimaryKey());
                            } catch (GenericEntityException e) {
                                Debug.logError(e, module);
                                return ServiceUtil.returnError(e.getMessage());
                            }

                            // set and save
                            productToUpdate.set("salesDiscontinuationDate", null);
                            try {
                                delegator.store(productToUpdate);
                            } catch (GenericEntityException e) {
                                Debug.logError(e, module);
                                return ServiceUtil.returnError(e.getMessage());
                            }
                        }
                    }
                }
            }
        }

        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> addAdditionalViewForProduct(DispatchContext dctx, Map<String, ? extends Object> context)
        throws IOException, JDOMException {

        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String productId = (String) context.get("productId");
        String productContentTypeId = (String) context.get("productContentTypeId");
        ByteBuffer imageData = (ByteBuffer) context.get("uploadedFile");

        if (UtilValidate.isNotEmpty(context.get("_uploadedFile_fileName"))) {
            String imageFilenameFormat = UtilProperties.getPropertyValue("catalog", "image.filename.format");
            String imageServerPath = FlexibleStringExpander.expandString(UtilProperties.getPropertyValue("catalog", "image.server.path"), context);
            String imageUrlPrefix = UtilProperties.getPropertyValue("catalog", "image.url.prefix");

            FlexibleStringExpander filenameExpander = FlexibleStringExpander.getInstance(imageFilenameFormat);
            String id = productId + "_View_" + productContentTypeId.charAt(productContentTypeId.length() - 1);
            String fileLocation = filenameExpander.expandString(UtilMisc.toMap("location", "products", "type", "additional", "id", id));
            String filePathPrefix = "";
            String filenameToUse = fileLocation;
            if (fileLocation.lastIndexOf("/") != -1) {
                filePathPrefix = fileLocation.substring(0, fileLocation.lastIndexOf("/") + 1); // adding 1 to include the trailing slash
                filenameToUse = fileLocation.substring(fileLocation.lastIndexOf("/") + 1);
            }

            List<GenericValue> fileExtension = FastList.newInstance();
            try {
                fileExtension = delegator.findByAnd("FileExtension", UtilMisc.toMap("mimeTypeId", (String) context.get("_uploadedFile_contentType")));
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
                ServiceUtil.returnError(e.getMessage());
            }

            GenericValue extension = EntityUtil.getFirst(fileExtension);
            if (extension != null) {
                filenameToUse += "." + extension.getString("fileExtensionId");
            }

            File file = new File(imageServerPath + "/" + filePathPrefix + filenameToUse);

            try {
                RandomAccessFile out = new RandomAccessFile(file, "rw");
                out.write(imageData.array());
                out.close();
            } catch (FileNotFoundException e) {
                Debug.logError(e, module);
                return ServiceUtil.returnError("Unable to open file for writing: " + file.getAbsolutePath());
            } catch (IOException e) {
                Debug.logError(e, module);
                return ServiceUtil.returnError("Unable to write binary data to: " + file.getAbsolutePath());
            }

            /* scale Image in different sizes */
            String viewNumber = String.valueOf(productContentTypeId.charAt(productContentTypeId.length() - 1));
            Map<String, Object> resultResize = FastMap.newInstance();
            try {
                resultResize.putAll(ScaleImage.scaleImageInAllSize(context, filenameToUse, "additional", viewNumber));
            } catch (IOException e) {
                String errMsg = "Scale additional image in all different sizes is impossible : " + e.toString();
                Debug.logError(e, errMsg, module);
                return ServiceUtil.returnError(errMsg);
            } catch (JDOMException e) {
                String errMsg = "Errors occur in parsing ImageProperties.xml : " + e.toString();
                Debug.logError(e, errMsg, module);
                return ServiceUtil.returnError(errMsg);
            }

            String imageUrl = imageUrlPrefix + "/" + filePathPrefix + filenameToUse;

            if (UtilValidate.isNotEmpty(imageUrl) && imageUrl.length() > 0) {
                String contentId = (String) context.get("contentId");

                Map<String, Object> dataResourceCtx = FastMap.newInstance();
                dataResourceCtx.put("objectInfo", imageUrl);
                dataResourceCtx.put("dataResourceName", (String) context.get("_uploadedFile_fileName"));
                dataResourceCtx.put("userLogin", userLogin);

                Map<String, Object> productContentCtx = FastMap.newInstance();
                productContentCtx.put("productId", productId);
                productContentCtx.put("productContentTypeId", productContentTypeId);
                productContentCtx.put("fromDate", (Timestamp) context.get("fromDate"));
                productContentCtx.put("thruDate", (Timestamp) context.get("thruDate"));
                productContentCtx.put("userLogin", userLogin);

                if (UtilValidate.isNotEmpty(contentId)) {
                    GenericValue content = null;
                    try {
                        content = delegator.findOne("Content", UtilMisc.toMap("contentId", contentId), false);
                    } catch (GenericEntityException e) {
                        Debug.logError(e, module);
                        ServiceUtil.returnError(e.getMessage());
                    }

                    if (content != null) {
                        GenericValue dataResource = null;
                        try {
                            dataResource = content.getRelatedOne("DataResource");
                        } catch (GenericEntityException e) {
                            Debug.logError(e, module);
                            ServiceUtil.returnError(e.getMessage());
                        }

                        if (dataResource != null) {
                            dataResourceCtx.put("dataResourceId", dataResource.getString("dataResourceId"));
                            try {
                                dispatcher.runSync("updateDataResource", dataResourceCtx);
                            } catch (GenericServiceException e) {
                                Debug.logError(e, module);
                                ServiceUtil.returnError(e.getMessage());
                            }
                        } else {
                            dataResourceCtx.put("dataResourceTypeId", "SHORT_TEXT");
                            dataResourceCtx.put("mimeTypeId", "text/html");
                            Map<String, Object> dataResourceResult = FastMap.newInstance();
                            try {
                                dataResourceResult = dispatcher.runSync("createDataResource", dataResourceCtx);
                            } catch (GenericServiceException e) {
                                Debug.logError(e, module);
                                ServiceUtil.returnError(e.getMessage());
                            }

                            Map<String, Object> contentCtx = FastMap.newInstance();
                            contentCtx.put("contentId", contentId);
                            contentCtx.put("dataResourceId", dataResourceResult.get("dataResourceId"));
                            contentCtx.put("userLogin", userLogin);
                            try {
                                dispatcher.runSync("updateContent", contentCtx);
                            } catch (GenericServiceException e) {
                                Debug.logError(e, module);
                                ServiceUtil.returnError(e.getMessage());
                            }
                        }

                        productContentCtx.put("contentId", contentId);
                        try {
                            dispatcher.runSync("updateProductContent", productContentCtx);
                        } catch (GenericServiceException e) {
                            Debug.logError(e, module);
                            ServiceUtil.returnError(e.getMessage());
                        }
                    }
                } else {
                    dataResourceCtx.put("dataResourceTypeId", "SHORT_TEXT");
                    dataResourceCtx.put("mimeTypeId", "text/html");
                    Map<String, Object> dataResourceResult = FastMap.newInstance();
                    try {
                        dataResourceResult = dispatcher.runSync("createDataResource", dataResourceCtx);
                    } catch (GenericServiceException e) {
                        Debug.logError(e, module);
                        ServiceUtil.returnError(e.getMessage());
                    }

                    Map<String, Object> contentCtx = FastMap.newInstance();
                    contentCtx.put("contentTypeId", "DOCUMENT");
                    contentCtx.put("dataResourceId", dataResourceResult.get("dataResourceId"));
                    contentCtx.put("userLogin", userLogin);
                    Map<String, Object> contentResult = FastMap.newInstance();
                    try {
                        contentResult = dispatcher.runSync("createContent", contentCtx);
                    } catch (GenericServiceException e) {
                        Debug.logError(e, module);
                        ServiceUtil.returnError(e.getMessage());
                    }

                    productContentCtx.put("contentId", contentResult.get("contentId"));
                    try {
                        dispatcher.runSync("createProductContent", productContentCtx);
                    } catch (GenericServiceException e) {
                        Debug.logError(e, module);
                        ServiceUtil.returnError(e.getMessage());
                    }
                }
            }
        }
           return ServiceUtil.returnSuccess();
    }

    /**
     * Finds productId(s) corresponding to a product reference, productId or a GoodIdentification idValue
     * @param dctx
     * @param context
     * @param context.productId use to search with productId or goodIdentification.idValue
     * @return a GenericValue with a productId and a List of complementary productId found
     */
    public static Map<String, Object> findProductById(DispatchContext ctx, Map<String, Object> context) {
        Delegator delegator = ctx.getDelegator();
        String idToFind = (String) context.get("idToFind");
        String goodIdentificationTypeId = (String) context.get("goodIdentificationTypeId");
        String searchProductFirstContext = (String) context.get("searchProductFirst");
        String searchAllIdContext = (String) context.get("searchAllId");

        boolean searchProductFirst = UtilValidate.isNotEmpty(searchProductFirstContext) && "N".equals(searchProductFirstContext) ? false : true;
        boolean searchAllId = UtilValidate.isNotEmpty(searchAllIdContext)&& "Y".equals(searchAllIdContext) ? true : false;

        GenericValue product = null;
        List<GenericValue> productsFound = null;
        try {
            productsFound = ProductWorker.findProductsById(delegator, idToFind, goodIdentificationTypeId, searchProductFirst, searchAllId);
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            ServiceUtil.returnError(e.getMessage());
        }

        if (UtilValidate.isNotEmpty(productsFound)) {
            // gets the first productId of the List
            product = EntityUtil.getFirst(productsFound);
            // remove this productId
            productsFound.remove(0);
        }

        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put("product", product);
        result.put("productsList", productsFound);

        return result;
    }

    public static Map<String, Object> addImageForProductPromo(DispatchContext dctx, Map<String, ? extends Object> context)
            throws IOException, JDOMException {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String productPromoId = (String) context.get("productPromoId");
        String productPromoContentTypeId = (String) context.get("productPromoContentTypeId");
        ByteBuffer imageData = (ByteBuffer) context.get("uploadedFile");
        String contentId = (String) context.get("contentId");

        if (UtilValidate.isNotEmpty(context.get("_uploadedFile_fileName"))) {
            String imageFilenameFormat = UtilProperties.getPropertyValue("catalog", "image.filename.format");
            String imageServerPath = FlexibleStringExpander.expandString(UtilProperties.getPropertyValue("catalog", "image.server.path"), context);
            String imageUrlPrefix = UtilProperties.getPropertyValue("catalog", "image.url.prefix");

            FlexibleStringExpander filenameExpander = FlexibleStringExpander.getInstance(imageFilenameFormat);
            String id = productPromoId + "_Image_" + productPromoContentTypeId.charAt(productPromoContentTypeId.length() - 1);
            String fileLocation = filenameExpander.expandString(UtilMisc.toMap("location", "products", "type", "promo", "id", id));
            String filePathPrefix = "";
            String filenameToUse = fileLocation;
            if (fileLocation.lastIndexOf("/") != -1) {
                filePathPrefix = fileLocation.substring(0, fileLocation.lastIndexOf("/") + 1); // adding 1 to include the trailing slash
                filenameToUse = fileLocation.substring(fileLocation.lastIndexOf("/") + 1);
            }

            List<GenericValue> fileExtension = FastList.newInstance();
            try {
                fileExtension = delegator.findList("FileExtension", EntityCondition.makeCondition("mimeTypeId", EntityOperator.EQUALS, (String) context.get("_uploadedFile_contentType")), null, null, null, false);
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
                ServiceUtil.returnError(e.getMessage());
            }

            GenericValue extension = EntityUtil.getFirst(fileExtension);
            if (extension != null) {
                filenameToUse += "." + extension.getString("fileExtensionId");
            }

            File makeResourceDirectory  = new File(imageServerPath + "/" + filePathPrefix);
            if (!makeResourceDirectory.exists()) {
                makeResourceDirectory.mkdirs();
            }

            File file = new File(imageServerPath + "/" + filePathPrefix + filenameToUse);

            try {
                RandomAccessFile out = new RandomAccessFile(file, "rw");
                out.write(imageData.array());
                out.close();
            } catch (FileNotFoundException e) {
                Debug.logError(e, module);
                return ServiceUtil.returnError("Unable to open file for writing: " + file.getAbsolutePath());
            } catch (IOException e) {
                Debug.logError(e, module);
                return ServiceUtil.returnError("Unable to write binary data to: " + file.getAbsolutePath());
            }

            String imageUrl = imageUrlPrefix + "/" + filePathPrefix + filenameToUse;

            if (UtilValidate.isNotEmpty(imageUrl) && imageUrl.length() > 0) {
                Map<String, Object> dataResourceCtx = FastMap.newInstance();
                dataResourceCtx.put("objectInfo", imageUrl);
                dataResourceCtx.put("dataResourceName", (String) context.get("_uploadedFile_fileName"));
                dataResourceCtx.put("userLogin", userLogin);

                Map<String, Object> productPromoContentCtx = FastMap.newInstance();
                productPromoContentCtx.put("productPromoId", productPromoId);
                productPromoContentCtx.put("productPromoContentTypeId", productPromoContentTypeId);
                productPromoContentCtx.put("fromDate", (Timestamp) context.get("fromDate"));
                productPromoContentCtx.put("thruDate", (Timestamp) context.get("thruDate"));
                productPromoContentCtx.put("userLogin", userLogin);

                if (UtilValidate.isNotEmpty(contentId)) {
                    GenericValue content = null;
                    try {
                        content = delegator.findOne("Content", UtilMisc.toMap("contentId", contentId), false);
                    } catch (GenericEntityException e) {
                        Debug.logError(e, module);
                        ServiceUtil.returnError(e.getMessage());
                    }

                    if (UtilValidate.isNotEmpty(content)) {
                        GenericValue dataResource = null;
                        try {
                            dataResource = content.getRelatedOne("DataResource");
                        } catch (GenericEntityException e) {
                            Debug.logError(e, module);
                            ServiceUtil.returnError(e.getMessage());
                        }

                        if (UtilValidate.isNotEmpty(dataResource)) {
                            dataResourceCtx.put("dataResourceId", dataResource.getString("dataResourceId"));
                            try {
                                dispatcher.runSync("updateDataResource", dataResourceCtx);
                            } catch (GenericServiceException e) {
                                Debug.logError(e, module);
                                ServiceUtil.returnError(e.getMessage());
                            }
                        } else {
                            dataResourceCtx.put("dataResourceTypeId", "SHORT_TEXT");
                            dataResourceCtx.put("mimeTypeId", "text/html");
                            Map<String, Object> dataResourceResult = FastMap.newInstance();
                            try {
                                dataResourceResult = dispatcher.runSync("createDataResource", dataResourceCtx);
                            } catch (GenericServiceException e) {
                                Debug.logError(e, module);
                                ServiceUtil.returnError(e.getMessage());
                            }

                            Map<String, Object> contentCtx = FastMap.newInstance();
                            contentCtx.put("contentId", contentId);
                            contentCtx.put("dataResourceId", dataResourceResult.get("dataResourceId"));
                            contentCtx.put("userLogin", userLogin);
                            try {
                                dispatcher.runSync("updateContent", contentCtx);
                            } catch (GenericServiceException e) {
                                Debug.logError(e, module);
                                ServiceUtil.returnError(e.getMessage());
                            }
                        }

                        productPromoContentCtx.put("contentId", contentId);
                        try {
                            dispatcher.runSync("updateProductPromoContent", productPromoContentCtx);
                        } catch (GenericServiceException e) {
                            Debug.logError(e, module);
                            ServiceUtil.returnError(e.getMessage());
                        }
                    }
                } else {
                    dataResourceCtx.put("dataResourceTypeId", "SHORT_TEXT");
                    dataResourceCtx.put("mimeTypeId", "text/html");
                    Map<String, Object> dataResourceResult = FastMap.newInstance();
                    try {
                        dataResourceResult = dispatcher.runSync("createDataResource", dataResourceCtx);
                    } catch (GenericServiceException e) {
                        Debug.logError(e, module);
                        ServiceUtil.returnError(e.getMessage());
                    }

                    Map<String, Object> contentCtx = FastMap.newInstance();
                    contentCtx.put("contentTypeId", "DOCUMENT");
                    contentCtx.put("dataResourceId", dataResourceResult.get("dataResourceId"));
                    contentCtx.put("userLogin", userLogin);
                    Map<String, Object> contentResult = FastMap.newInstance();
                    try {
                        contentResult = dispatcher.runSync("createContent", contentCtx);
                    } catch (GenericServiceException e) {
                        Debug.logError(e, module);
                        ServiceUtil.returnError(e.getMessage());
                    }

                    productPromoContentCtx.put("contentId", contentResult.get("contentId"));
                    try {
                        dispatcher.runSync("createProductPromoContent", productPromoContentCtx);
                    } catch (GenericServiceException e) {
                        Debug.logError(e, module);
                        ServiceUtil.returnError(e.getMessage());
                    }
                }
            }
        } else {
            Map<String, Object> productPromoContentCtx = FastMap.newInstance();
            productPromoContentCtx.put("productPromoId", productPromoId);
            productPromoContentCtx.put("productPromoContentTypeId", productPromoContentTypeId);
            productPromoContentCtx.put("contentId", contentId);
            productPromoContentCtx.put("fromDate", (Timestamp) context.get("fromDate"));
            productPromoContentCtx.put("thruDate", (Timestamp) context.get("thruDate"));
            productPromoContentCtx.put("userLogin", userLogin);
            try {
                dispatcher.runSync("updateProductPromoContent", productPromoContentCtx);
            } catch (GenericServiceException e) {
                Debug.logError(e, module);
                ServiceUtil.returnError(e.getMessage());
            }
        }
        return ServiceUtil.returnSuccess();
    }
}
