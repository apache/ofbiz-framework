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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.string.FlexibleStringExpander;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityJoinOperator;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.entity.util.EntityUtilProperties;
import org.apache.ofbiz.product.catalog.CatalogWorker;
import org.apache.ofbiz.product.category.CategoryWorker;
import org.apache.ofbiz.product.image.ScaleImage;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.service.ServiceUtil;
import org.jdom.JDOMException;

/**
 * Product Services
 */
public class ProductServices {

    public static final String module = ProductServices.class.getName();
    public static final String resource = "ProductUiLabels";
    public static final String resourceError = "ProductErrorUiLabels";

    /**
     * Creates a Collection of product entities which are variant products from the specified product ID.
     */
    public static Map<String, Object> prodFindAllVariants(DispatchContext dctx, Map<String, ? extends Object> context) {
        Map<String, Object> subContext = UtilMisc.makeMapWritable(context);
        subContext.put("type", "PRODUCT_VARIANT");
        return prodFindAssociatedByType(dctx, subContext);
    }

    /**
     * Finds a specific product or products which contain the selected features.
     */
    public static Map<String, Object> prodFindSelectedVariant(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get("locale");
        Map<String, String> selectedFeatures = UtilGenerics.cast(context.get("selectedFeatures"));
        List<GenericValue> products = new LinkedList<>();
        // All the variants for this products are retrieved
        Map<String, Object> resVariants = prodFindAllVariants(dctx, context);
        List<GenericValue> variants = UtilGenerics.checkList(resVariants.get("assocProducts"));
        for (GenericValue oneVariant: variants) {
            // For every variant, all the standard features are retrieved
            Map<String, String> feaContext = new HashMap<>();
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
                    products.add(EntityQuery.use(delegator).from("Product").where("productId", oneVariant.getString("productIdTo")).queryOne());
                } catch (GenericEntityException e) {
                    Map<String, String> messageMap = UtilMisc.toMap("errProductFeatures", e.toString());
                    String errMsg = UtilProperties.getMessage(resourceError,"productservices.problem_reading_product_features_errors", messageMap, locale);
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
     * Finds a Set of feature types in sequence.
     */
    public static Map<String, Object> prodFindFeatureTypes(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        String productId = (String) context.get("productId");
        String productFeatureApplTypeId = (String) context.get("productFeatureApplTypeId");
        if (UtilValidate.isEmpty(productFeatureApplTypeId)) {
            productFeatureApplTypeId = "SELECTABLE_FEATURE";
        }
        Locale locale = (Locale) context.get("locale");
        String errMsg=null;
        Set<String> featureSet = new LinkedHashSet<>();

        try {
            List<GenericValue> features = EntityQuery.use(delegator).from("ProductFeatureAndAppl").where("productId", productId, "productFeatureApplTypeId", productFeatureApplTypeId).orderBy("sequenceNum", "productFeatureTypeId").cache(true).filterByDate().queryList();
            for (GenericValue v: features) {
                featureSet.add(v.getString("productFeatureTypeId"));
            }
        } catch (GenericEntityException e) {
            Map<String, String> messageMap = UtilMisc.toMap("errProductFeatures", e.toString());
            errMsg = UtilProperties.getMessage(resourceError,"productservices.problem_reading_product_features_errors", messageMap, locale);
            Debug.logError(e, errMsg, module);
            return ServiceUtil.returnError(errMsg);
        }

        if (featureSet.size() == 0) {
            errMsg = UtilProperties.getMessage(resourceError,"productservices.problem_reading_product_features", locale);
            // ToDo DO 2004-02-23 Where should the errMsg go?
            Debug.logWarning(errMsg + " for product " + productId, module);
        }
        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put("featureSet", featureSet);
        return result;
    }

    /**
     * Builds a variant feature tree.
     */
    public static Map<String, Object> prodMakeFeatureTree(DispatchContext dctx, Map<String, ? extends Object> context) {
        String productStoreId = (String) context.get("productStoreId");
        Locale locale = (Locale) context.get("locale");

        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Map<String, Object> result = new HashMap<>();
        List<String> featureOrder = UtilMisc.makeListWritable(UtilGenerics.<String>checkCollection(context.get("featureOrder")));

        if (UtilValidate.isEmpty(featureOrder)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource,
                    "ProductFeatureTreeCannotFindFeaturesList", locale));
        }

        List<GenericValue> variants = UtilGenerics.checkList(prodFindAllVariants(dctx, context).get("assocProducts"));
        List<String> virtualVariant = new LinkedList<>();

        if (UtilValidate.isEmpty(variants)) {
            return ServiceUtil.returnSuccess();
        }
        List<String> items = new LinkedList<>();
        List<GenericValue> outOfStockItems = new LinkedList<>();

        for (GenericValue variant: variants) {
            String productIdTo = variant.getString("productIdTo");

            // first check to see if intro and discontinue dates are within range
            GenericValue productTo = null;

            try {
                productTo = EntityQuery.use(delegator).from("Product").where("productId", productIdTo).cache().queryOne();
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
                Map<String, String> messageMap = UtilMisc.toMap("productIdTo", productIdTo, "errMessage", e.toString());
                return ServiceUtil.returnError(UtilProperties.getMessage(resourceError,
                        "productservices.error_finding_associated_variant_with_ID_error", messageMap, locale));
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
                        return ServiceUtil.returnError(UtilProperties.getMessage(resource,
                                "ProductFeatureTreeCannotCallIsStoreInventoryRequired", locale), null, null, invReqResult);
                    } else if ("Y".equals(invReqResult.get("availableOrNotRequired"))) {
                        items.add(productIdTo);
                        if (productTo.getString("isVirtual") != null && "Y".equals(productTo.getString("isVirtual"))) {
                            virtualVariant.add(productIdTo);
                        }
                    } else {
                        outOfStockItems.add(productTo);
                    }
                } else {
                    items.add(productIdTo);
                    if (productTo.getString("isVirtual") != null && "Y".equals(productTo.getString("isVirtual"))) {
                        virtualVariant.add(productIdTo);
                    }
                }
            } catch (GenericServiceException e) {
                Debug.logError(e, "Error calling the isStoreInventoryRequired when building the variant product tree: " + e.toString(), module);
                return ServiceUtil.returnError(UtilProperties.getMessage(resource,
                        "ProductFeatureTreeCannotCallIsStoreInventoryRequired", locale));
            }
        }

        String productId = (String) context.get("productId");

        // Make the selectable feature list
        List<GenericValue> selectableFeatures = null;
        try {
            selectableFeatures = EntityQuery.use(delegator).from("ProductFeatureAndAppl").where("productId", productId, "productFeatureApplTypeId", "SELECTABLE_FEATURE").orderBy("sequenceNum").cache(true).filterByDate().queryList();
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError,
                    "productservices.empty_list_of_selectable_features_found", locale));
        }
        Map<String, List<String>> features = new HashMap<>();
        for (GenericValue v: selectableFeatures) {
            String featureType = v.getString("productFeatureTypeId");
            String feature = v.getString("description");

            if (!features.containsKey(featureType)) {
                List<String> featureList = new LinkedList<>();
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
            result.put(ModelService.ERROR_MESSAGE, UtilProperties.getMessage(resourceError,
                    "productservices.feature_grouping_came_back_empty", locale));
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
        // String type          -- Type of feature (STANDARD_FEATURE, SELECTABLE_FEATURE)
        // String distinct      -- Distinct feature (SIZE, COLOR)
        Delegator delegator = dctx.getDelegator();
        Map<String, Object> result = new HashMap<>();
        String productId = (String) context.get("productId");
        String distinct = (String) context.get("distinct");
        String type = (String) context.get("type");
        Locale locale = (Locale) context.get("locale");
        String errMsg=null;
        List<GenericValue> features = null;

        try {
            Map<String, String> fields = UtilMisc.toMap("productId", productId);

            if (distinct != null) {
                fields.put("productFeatureTypeId", distinct);
            }
            if (type != null) {
                fields.put("productFeatureApplTypeId", type);
            }
            features = EntityQuery.use(delegator).from("ProductFeatureAndAppl").where(fields).orderBy("sequenceNum", "productFeatureTypeId").cache(true).queryList();
            result.put("productFeatures", features);
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
        } catch (GenericEntityException e) {
            Map<String, String> messageMap = UtilMisc.toMap("errMessage", e.toString());
            errMsg = UtilProperties.getMessage(resourceError,
                    "productservices.problem_reading_product_feature_entity", messageMap, locale);
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_ERROR);
            result.put(ModelService.ERROR_MESSAGE, errMsg);
        }
        return result;
    }

    /**
     * Finds a product by product ID.
     */
    public static Map<String, Object> prodFindProduct(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        Map<String, Object> result = new HashMap<>();
        String productId = (String) context.get("productId");
        Locale locale = (Locale) context.get("locale");
        String errMsg = null;

        if (UtilValidate.isEmpty(productId)) {
            errMsg = UtilProperties.getMessage(resourceError,
                    "productservices.invalid_productId_passed", locale);
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_ERROR);
            result.put(ModelService.ERROR_MESSAGE, errMsg);
            return result;
        }

        try {
            GenericValue product = EntityQuery.use(delegator).from("Product").where("productId", productId).cache().queryOne();
            GenericValue mainProduct = product;

            if (product.get("isVariant") != null && "Y".equalsIgnoreCase(product.getString("isVariant"))) {
                List<GenericValue> c = product.getRelated("AssocProductAssoc", UtilMisc.toMap("productAssocTypeId", "PRODUCT_VARIANT"), null, true);
                c = EntityUtil.filterByDate(c);
                if (c.size() > 0) {
                    GenericValue asV = c.iterator().next();
                    mainProduct = asV.getRelatedOne("MainProduct", true);
                }
            }
            result.put("product", mainProduct);
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
        } catch (GenericEntityException e) {
            Map<String, String> messageMap = UtilMisc.toMap("errMessage", e.getMessage());
            errMsg = UtilProperties.getMessage(resourceError,
                    "productservices.problems_reading_product_entity", messageMap, locale);
            Debug.logError(e, errMsg, module);
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_ERROR);
            result.put(ModelService.ERROR_MESSAGE, errMsg);
        }

        return result;
    }

    /**
     * Finds associated products by product ID and association ID.
     */
    public static Map<String, Object> prodFindAssociatedByType(DispatchContext dctx, Map<String, ? extends Object> context) {
        // String type -- Type of association (ie PRODUCT_UPGRADE, PRODUCT_COMPLEMENT, PRODUCT_VARIANT)
        Delegator delegator = dctx.getDelegator();
        Map<String, Object> result = new HashMap<>();
        String productId = (String) context.get("productId");
        String productIdTo = (String) context.get("productIdTo");
        String type = (String) context.get("type");
        Locale locale = (Locale) context.get("locale");
        String errMsg = null;

        Boolean cvaBool = (Boolean) context.get("checkViewAllow");
        boolean checkViewAllow = (cvaBool == null ? false : cvaBool);
        String prodCatalogId = (String) context.get("prodCatalogId");
        Boolean bidirectional = (Boolean) context.get("bidirectional");
        bidirectional = bidirectional == null ? Boolean.FALSE : bidirectional;
        Boolean sortDescending = (Boolean) context.get("sortDescending");
        sortDescending = sortDescending == null ? Boolean.FALSE : sortDescending;

        if (productId == null && productIdTo == null) {
            errMsg = UtilProperties.getMessage(resourceError,
                    "productservices.both_productId_and_productIdTo_cannot_be_null", locale);
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_ERROR);
            result.put(ModelService.ERROR_MESSAGE, errMsg);
            return result;
        }

        if (productId != null && productIdTo != null) {
            errMsg = UtilProperties.getMessage(resourceError,
                    "productservices.both_productId_and_productIdTo_cannot_be_defined", locale);
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_ERROR);
            result.put(ModelService.ERROR_MESSAGE, errMsg);
            return result;
        }

        productId = productId == null ? productIdTo : productId;
        GenericValue product = null;

        try {
            product = EntityQuery.use(delegator).from("Product").where("productId", productId).cache().queryOne();
        } catch (GenericEntityException e) {
            Map<String, String> messageMap = UtilMisc.toMap("errMessage", e.getMessage());
            errMsg = UtilProperties.getMessage(resourceError,
                    "productservices.problems_reading_product_entity", messageMap, locale);
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_ERROR);
            result.put(ModelService.ERROR_MESSAGE, errMsg);
            return result;
        }

        if (product == null) {
            errMsg = UtilProperties.getMessage(resourceError,
                    "productservices.problems_getting_product_entity", locale);
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_ERROR);
            result.put(ModelService.ERROR_MESSAGE, errMsg);
            return result;
        }

        try {
            List<GenericValue> productAssocs = null;

            List<String> orderBy = new LinkedList<>();
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
                productAssocs = EntityQuery.use(delegator).from("ProductAssoc").where(EntityCondition.makeCondition(cond, EntityCondition.makeCondition("productAssocTypeId", type))).orderBy(orderBy).cache(true).queryList();
            } else {
                if (productIdTo == null) {
                    productAssocs = product.getRelated("MainProductAssoc", UtilMisc.toMap("productAssocTypeId", type), orderBy, true);
                } else {
                    productAssocs = product.getRelated("AssocProductAssoc", UtilMisc.toMap("productAssocTypeId", type), orderBy, true);
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
            errMsg = UtilProperties.getMessage(resourceError,
                    "productservices.problems_product_association_relation_error", messageMap, locale);
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_ERROR);
            result.put(ModelService.ERROR_MESSAGE, errMsg);
            return result;
        }

        return result;
    }

    // Builds a product feature tree
    private static Map<String, Object> makeGroup(Delegator delegator, Map<String, List<String>> featureList, List<String> items, List<String> order, int index)
        throws IllegalArgumentException, IllegalStateException {
        Map<String, List<String>> tempGroup = new HashMap<>();
        Map<String, Object> group = new LinkedHashMap<>();
        String orderKey = order.get(index);

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

            if (Debug.verboseOn()) {
                Debug.logVerbose("ThisItem: " + thisItem, module);
            }
            List<GenericValue> features = null;

            try {
                // get the features and filter out expired dates
                features = EntityQuery.use(delegator).from("ProductFeatureAndAppl")
                        .where("productId", thisItem, "productFeatureTypeId", orderKey, "productFeatureApplTypeId", "STANDARD_FEATURE")
                        .orderBy("sequenceNum")
                        .cache(true)
                        .filterByDate()
                        .queryList();
            } catch (GenericEntityException e) {
                throw new IllegalStateException("Problem reading relation: " + e.getMessage());
            }
            if (Debug.verboseOn()) {
                Debug.logVerbose("Features: " + features, module);
            }

            // -------------------------------
            for (GenericValue item: features) {
                String itemKey = item.getString("description");

                if (tempGroup.containsKey(itemKey)) {
                    List<String> itemList = tempGroup.get(itemKey);

                    if (!itemList.contains(thisItem)) {
                        itemList.add(thisItem);
                    }
                } else {
                    List<String> itemList = UtilMisc.toList(thisItem);

                    tempGroup.put(itemKey, itemList);
                }
            }
        }
        if (Debug.verboseOn()) {
            Debug.logVerbose("TempGroup: " + tempGroup, module);
        }

        // Loop through the feature list and order the keys in the tempGroup
        List<String> orderFeatureList = featureList.get(orderKey);

        if (orderFeatureList == null) {
            throw new IllegalArgumentException("Cannot build feature tree: orderFeatureList is null for orderKey=" + orderKey);
        }

        for (String featureStr: orderFeatureList) {
            if (tempGroup.containsKey(featureStr)) {
                group.put(featureStr, tempGroup.get(featureStr));
            }
        }

        if (Debug.verboseOn()) {
            Debug.logVerbose("Group: " + group, module);
        }

        // no groups; no tree
        if (group.size() == 0) {
            return group;
        }

        if (index + 1 == order.size()) {
            return group;
        }

        // loop through the keysets and get the sub-groups
        for (Entry<String, Object> entry : group.entrySet()) {
            String key = entry.getKey();
            List<String> itemList = UtilGenerics.checkList(group.get(key));

            if (UtilValidate.isNotEmpty(itemList)) {
                Map<String, Object> subGroup = makeGroup(delegator, featureList, itemList, order, index + 1);
                group.put(key, subGroup);
            }
        }
        return group;
    }

    // builds a variant sample (a single sku for a featureType)
    private static Map<String, GenericValue> makeVariantSample(Delegator delegator, Map<String, List<String>> featureList, List<String> items, String feature) {
        Map<String, GenericValue> tempSample = new HashMap<>();
        Map<String, GenericValue> sample = new LinkedHashMap<>();
        for (String productId: items) {
            List<GenericValue> features = null;

            try {
                // get the features and filter out expired dates
                features = EntityQuery.use(delegator).from("ProductFeatureAndAppl")
                               .where("productId", productId, "productFeatureTypeId", feature, "productFeatureApplTypeId", "STANDARD_FEATURE")
                               .orderBy("sequenceNum", "description")
                               .cache(true)
                               .filterByDate()
                               .queryList();
            } catch (GenericEntityException e) {
                throw new IllegalStateException("Problem reading relation: " + e.getMessage());
            }
            for (GenericValue featureAppl: features) {
                try {
                    GenericValue product = EntityQuery.use(delegator).from("Product").where("productId", productId).cache(true).queryOne();

                    tempSample.put(featureAppl.getString("description"), product);
                } catch (GenericEntityException e) {
                    throw new RuntimeException("Cannot get product entity: " + e.getMessage());
                }
            }
        }

        // Sort the sample based on the feature list.
        List<String> features = featureList.get(feature);
        for (String f: features) {
            if (tempSample.containsKey(f)) {
                sample.put(f, tempSample.get(f));
            }
        }

        return sample;
    }

    public static Map<String, Object> quickAddVariant(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        Map<String, Object> result = new HashMap<>();
        Locale locale = (Locale) context.get("locale");
        String errMsg=null;
        String productId = (String) context.get("productId");
        String variantProductId = (String) context.get("productVariantId");
        String productFeatureIds = (String) context.get("productFeatureIds");
        Long prodAssocSeqNum = (Long) context.get("sequenceNum");

        try {
            // read the product, duplicate it with the given id
            GenericValue product = EntityQuery.use(delegator).from("Product").where("productId", productId).queryOne();
            if (product == null) {
                Map<String, String> messageMap = UtilMisc.toMap("productId", productId);
                errMsg = UtilProperties.getMessage(resourceError,
                        "productservices.product_not_found_with_ID", messageMap, locale);
                result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_ERROR);
                result.put(ModelService.ERROR_MESSAGE, errMsg);
                return result;
            }
            // check if product exists
            GenericValue variantProduct = EntityQuery.use(delegator).from("Product").where("productId", variantProductId).queryOne();
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

                GenericValue productFeature = EntityQuery.use(delegator).from("ProductFeature").where("productFeatureId", productFeatureId).queryOne();

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
            errMsg = UtilProperties.getMessage(resourceError,
                    "productservices.entity_error_quick_add_variant_data", messageMap, locale);
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
        Locale locale = (Locale) context.get("locale");

        Map<String, Object> successResult = ServiceUtil.returnSuccess();

        try {
            // Generate new virtual productId, put in successResult
            String productId = (String) context.get("productId");

            if (UtilValidate.isEmpty(productId)) {
                productId = delegator.getNextSeqId("Product");
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
            Set<String> prelimVariantProductIds = new HashSet<>();
            List<String> splitIds = Arrays.asList(variantProductIdsBag.split("[,\\p{Space}]"));
            Debug.logInfo("Variants: bag=" + variantProductIdsBag, module);
            Debug.logInfo("Variants: split=" + splitIds, module);
            prelimVariantProductIds.addAll(splitIds);
            //note: should support both direct productIds and GoodIdentification entries (what to do if more than one GoodID? Add all?

            Map<String, GenericValue> variantProductsById = new HashMap<>();
            for (String variantProductId: prelimVariantProductIds) {
                if (UtilValidate.isEmpty(variantProductId)) {
                    // not sure why this happens, but seems to from time to time with the split method
                    continue;
                }
                // is a Product.productId?
                GenericValue variantProduct = EntityQuery.use(delegator).from("Product").where("productId", variantProductId).queryOne();
                if (variantProduct != null) {
                    variantProductsById.put(variantProductId, variantProduct);
                } else {
                    // is a GoodIdentification.idValue?
                    List<GenericValue> goodIdentificationList = EntityQuery.use(delegator).from("GoodIdentification").where("idValue", variantProductId).queryList();
                    if (UtilValidate.isEmpty(goodIdentificationList)) {
                        // whoops, nothing found... return error
                        return ServiceUtil.returnError(UtilProperties.getMessage(resource,
                                "ProductVirtualVariantCreation", UtilMisc.toMap("variantProductId", variantProductId), locale));
                    }

                    if (goodIdentificationList.size() > 1) {
                        // what to do here? for now just log a warning and add all of them as variants; they can always be dissociated later
                        Debug.logWarning("Warning creating a virtual with variants: the ID [" + variantProductId + "] was not a productId and resulted in [" + goodIdentificationList.size() + "] GoodIdentification records: " + goodIdentificationList, module);
                    }

                    for (GenericValue goodIdentification: goodIdentificationList) {
                        GenericValue giProduct = goodIdentification.getRelatedOne("Product", false);
                        if (giProduct != null) {
                            variantProductsById.put(giProduct.getString("productId"), giProduct);
                        }
                    }
                }
            }

            // Attach productFeatureIdOne, Two, Three to the new virtual and all variant products as a standard feature
            Set<String> featureProductIds = new HashSet<>();
            featureProductIds.add(productId);
            featureProductIds.addAll(variantProductsById.keySet());
            Set<String> productFeatureIds = new HashSet<>();
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
            String errMsg = UtilProperties.getMessage(resourceError, "ProductErrorCreatingNewVirtualProductFromVariantProducts", UtilMisc.toMap("errorString", e.toString()), locale);
            Debug.logError(e, errMsg, module);
            return ServiceUtil.returnError(errMsg);
        }
        return successResult;
    }

    public static Map<String, Object> updateProductIfAvailableFromShipment(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        if ("Y".equals(EntityUtilProperties.getPropertyValue("catalog", "reactivate.product.from.receipt", "N", delegator))) {
            LocalDispatcher dispatcher = dctx.getDispatcher();
            GenericValue userLogin = (GenericValue) context.get("userLogin");
            String inventoryItemId = (String) context.get("inventoryItemId");

            GenericValue inventoryItem = null;
            try {
                inventoryItem = EntityQuery.use(delegator).from("InventoryItem").where("inventoryItemId", inventoryItemId).cache().queryOne();
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
                return ServiceUtil.returnError(e.getMessage());
            }

            if (inventoryItem != null) {
                String productId = inventoryItem.getString("productId");
                GenericValue product = null;
                try {
                    product = EntityQuery.use(delegator).from("Product").where("productId", productId).cache().queryOne();
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
                            if (ServiceUtil.isError(invRes)) {
                                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(invRes));
                            }
                        } catch (GenericServiceException e) {
                            Debug.logError(e, module);
                            return ServiceUtil.returnError(e.getMessage());
                        }

                        BigDecimal availableToPromiseTotal = (BigDecimal) invRes.get("availableToPromiseTotal");
                        if (availableToPromiseTotal != null && availableToPromiseTotal.compareTo(BigDecimal.ZERO) > 0) {
                            // refresh the product so we can update it
                            GenericValue productToUpdate = null;
                            try {
                                productToUpdate = EntityQuery.use(delegator).from("Product").where(product.getPrimaryKey()).queryOne();
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

    public static Map<String, Object> addAdditionalViewForProduct(DispatchContext dctx,
            Map<String, ? extends Object> context) {

        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        String productId = (String) context.get("productId");
        String productContentTypeId = (String) context.get("productContentTypeId");
        ByteBuffer imageData = (ByteBuffer) context.get("uploadedFile");
        Locale locale = (Locale) context.get("locale");

        if (UtilValidate.isNotEmpty(context.get("_uploadedFile_fileName"))) {
            Map<String, Object> imageContext = new HashMap<>();
            imageContext.putAll(context);
            imageContext.put("delegator", delegator);
            imageContext.put("tenantId",delegator.getDelegatorTenantId());
            String imageFilenameFormat = EntityUtilProperties.getPropertyValue("catalog", "image.filename.additionalviewsize.format", delegator);

            String imageServerPath = FlexibleStringExpander.expandString(EntityUtilProperties.getPropertyValue("catalog", "image.server.path", delegator), imageContext);
            String imageUrlPrefix = FlexibleStringExpander.expandString(EntityUtilProperties.getPropertyValue("catalog", "image.url.prefix", delegator), imageContext);
            imageServerPath = imageServerPath.endsWith("/") ? imageServerPath.substring(0, imageServerPath.length()-1) : imageServerPath;
            imageUrlPrefix = imageUrlPrefix.endsWith("/") ? imageUrlPrefix.substring(0, imageUrlPrefix.length()-1) : imageUrlPrefix;
            FlexibleStringExpander filenameExpander = FlexibleStringExpander.getInstance(imageFilenameFormat);
            String viewNumber = String.valueOf(productContentTypeId.charAt(productContentTypeId.length() - 1));
            String viewType = "additional" + viewNumber;
            String id = productId;
            if (imageFilenameFormat.endsWith("${id}")) {
                id = productId + "_View_" + viewNumber;
                viewType = "additional";
            }
            String fileLocation = filenameExpander.expandString(UtilMisc.toMap("location", "products", "id", id, "viewtype", viewType, "sizetype", "original"));
            String filePathPrefix = "";
            String filenameToUse = fileLocation;
            if (fileLocation.lastIndexOf('/') != -1) {
                filePathPrefix = fileLocation.substring(0, fileLocation.lastIndexOf('/') + 1); // adding 1 to include the trailing slash
                filenameToUse = fileLocation.substring(fileLocation.lastIndexOf('/') + 1);
            }

            List<GenericValue> fileExtension;
            try {
                fileExtension = EntityQuery.use(delegator)
                        .from("FileExtension")
                        .where("mimeTypeId", context.get("_uploadedFile_contentType"))
                        .queryList();
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
                return ServiceUtil.returnError(e.getMessage());
            }

            GenericValue extension = EntityUtil.getFirst(fileExtension);
            if (extension != null) {
                filenameToUse += "." + extension.getString("fileExtensionId");
            }

            /* Write the new image file */
            String targetDirectory = imageServerPath + "/" + filePathPrefix;
            try {
                File targetDir = new File(targetDirectory);
                // Create the new directory
                if (!targetDir.exists()) {
                    boolean created = targetDir.mkdirs();
                    if (!created) {
                        String errMsg = UtilProperties.getMessage(resource, "ScaleImage.unable_to_create_target_directory", locale) + " - " + targetDirectory;
                        Debug.logFatal(errMsg, module);
                        return ServiceUtil.returnError(errMsg);
                    }
                // Delete existing image files
                // Images are ordered by productId (${location}/${id}/${viewtype}/${sizetype})
                } else if (!filenameToUse.contains(productId)) {
                    try {
                        File[] files = targetDir.listFiles();
                        for (File file : files) {
                            if (file.isFile()) {
                                if (!file.delete()) {
                                    Debug.logError("File : " + file.getName() + ", couldn't be deleted", module);
                                }
                            }

                        }
                    } catch (SecurityException e) {
                        Debug.logError(e,module);
                    }
                // Images aren't ordered by productId (${location}/${viewtype}/${sizetype}/${id})
                } else {
                    try {
                        File[] files = targetDir.listFiles();
                        for (File file : files) {
                            if (file.isFile() && file.getName().startsWith(productId + "_View_" + viewNumber)) {
                                if (!file.delete()) {
                                    Debug.logError("File : " + file.getName() + ", couldn't be deleted", module);
                                }
                            }
                        }
                    } catch (SecurityException e) {
                        Debug.logError(e,module);
                    }
                }
            } catch (NullPointerException e) {
                Debug.logError(e,module);
            }
            // Write
            try {
            File file = new File(imageServerPath + "/" + fileLocation + "." +  extension.getString("fileExtensionId"));
                try {
                    RandomAccessFile out = new RandomAccessFile(file, "rw");
                    out.write(imageData.array());
                    out.close();
                } catch (FileNotFoundException e) {
                    Debug.logError(e, module);
                    return ServiceUtil.returnError(UtilProperties.getMessage(resource,
                            "ProductImageViewUnableWriteFile", UtilMisc.toMap("fileName", file.getAbsolutePath()), locale));
                } catch (IOException e) {
                    Debug.logError(e, module);
                    return ServiceUtil.returnError(UtilProperties.getMessage(resource,
                            "ProductImageViewUnableWriteBinaryData", UtilMisc.toMap("fileName", file.getAbsolutePath()), locale));
                }
            } catch (NullPointerException e) {
                Debug.logError(e,module);
            }

            /* scale Image in different sizes */
            Map<String, Object> resultResize = new HashMap<>();
            try {
                resultResize.putAll(ScaleImage.scaleImageInAllSize(imageContext, filenameToUse, "additional", viewNumber));
            } catch (IOException e) {
                Debug.logError(e, "Scale additional image in all different sizes is impossible : " + e.toString(), module);
                return ServiceUtil.returnError(UtilProperties.getMessage(resource,
                        "ProductImageViewScaleImpossible", UtilMisc.toMap("errorString", e.toString()), locale));
            } catch (JDOMException e) {
                Debug.logError(e, "Errors occur in parsing ImageProperties.xml : " + e.toString(), module);
                return ServiceUtil.returnError(UtilProperties.getMessage(resource,
                        "ProductImageViewParsingError", UtilMisc.toMap("errorString", e.toString()), locale));
            }

            String imageUrl = imageUrlPrefix + "/" + fileLocation + "." +  extension.getString("fileExtensionId");
            /* store the imageUrl version of the image, for backwards compatibility with code that does not use scaled versions */
            Map<String, Object> result = addImageResource(dispatcher, delegator, context, imageUrl, productContentTypeId);

            if( ServiceUtil.isError(result)) {
                return result;
            }

            /* now store the image versions created by ScaleImage.scaleImageInAllSize */
            /* have to shrink length of productContentTypeId, as otherwise value is too long for database field */
            Map<String,String> imageUrlMap = UtilGenerics.cast(resultResize.get("imageUrlMap"));
            for ( String sizeType : ScaleImage.sizeTypeList ) {
                imageUrl = imageUrlMap.get(sizeType);
                if( UtilValidate.isNotEmpty(imageUrl)) {
                    try {
                        GenericValue productContentType = EntityQuery.use(delegator)
                                .from("ProductContentType")
                                .where("productContentTypeId", "XTRA_IMG_" + viewNumber + "_" + sizeType.toUpperCase(Locale.getDefault()))
                                .cache()
                                .queryOne();
                        if (UtilValidate.isNotEmpty(productContentType)) {
                            result = addImageResource(dispatcher, delegator, context, imageUrl, "XTRA_IMG_" + viewNumber + "_" + sizeType.toUpperCase(Locale.getDefault()));
                            if( ServiceUtil.isError(result)) {
                                Debug.logError(ServiceUtil.getErrorMessage(result), module);
                                return result;
                            }
                        }
                    } catch(GenericEntityException e) {
                        Debug.logError(e,module);
                        return ServiceUtil.returnError(e.getMessage());
                    }
                }
            }
        }
        return ServiceUtil.returnSuccess();
    }

    private static Map<String,Object> addImageResource( LocalDispatcher dispatcher, Delegator delegator, Map<String, ? extends Object> context,
            String imageUrl, String productContentTypeId ) {
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String productId = (String) context.get("productId");

        if (UtilValidate.isNotEmpty(imageUrl) && imageUrl.length() > 0) {
            String contentId = (String) context.get("contentId");

            Map<String, Object> dataResourceCtx = new HashMap<>();
            dataResourceCtx.put("objectInfo", imageUrl);
            dataResourceCtx.put("dataResourceName", context.get("_uploadedFile_fileName"));
            dataResourceCtx.put("userLogin", userLogin);

            Map<String, Object> productContentCtx = new HashMap<>();
            productContentCtx.put("productId", productId);
            productContentCtx.put("productContentTypeId", productContentTypeId);
            productContentCtx.put("fromDate", context.get("fromDate"));
            productContentCtx.put("thruDate", context.get("thruDate"));
            productContentCtx.put("userLogin", userLogin);

            if (UtilValidate.isNotEmpty(contentId)) {
                GenericValue content = null;
                try {
                    content = EntityQuery.use(delegator).from("Content").where("contentId", contentId).queryOne();
                } catch (GenericEntityException e) {
                    Debug.logError(e, module);
                    return ServiceUtil.returnError(e.getMessage());
                }

                if (content != null) {
                    GenericValue dataResource = null;
                    try {
                        dataResource = content.getRelatedOne("DataResource", false);
                    } catch (GenericEntityException e) {
                        Debug.logError(e, module);
                        return ServiceUtil.returnError(e.getMessage());
                    }

                    if (dataResource != null) {
                        dataResourceCtx.put("dataResourceId", dataResource.getString("dataResourceId"));
                        try {
                            Map<String, Object> serviceResult = dispatcher.runSync("updateDataResource", dataResourceCtx);
                            if (ServiceUtil.isError(serviceResult)) {
                                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                            }
                        } catch (GenericServiceException e) {
                            Debug.logError(e, module);
                            return ServiceUtil.returnError(e.getMessage());
                        }
                    } else {
                        dataResourceCtx.put("dataResourceTypeId", "SHORT_TEXT");
                        dataResourceCtx.put("mimeTypeId", "text/html");
                        Map<String, Object> dataResourceResult;
                        try {
                            dataResourceResult = dispatcher.runSync("createDataResource", dataResourceCtx);
                            if (ServiceUtil.isError(dataResourceResult)) {
                                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(dataResourceResult));
                            }
                        } catch (GenericServiceException e) {
                            Debug.logError(e, module);
                            return ServiceUtil.returnError(e.getMessage());
                        }

                        Map<String, Object> contentCtx = new HashMap<>();
                        contentCtx.put("contentId", contentId);
                        contentCtx.put("dataResourceId", dataResourceResult.get("dataResourceId"));
                        contentCtx.put("userLogin", userLogin);
                        try {
                            Map<String, Object> serviceResult = dispatcher.runSync("updateContent", contentCtx);
                            if (ServiceUtil.isError(serviceResult)) {
                                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                            }
                        } catch (GenericServiceException e) {
                            Debug.logError(e, module);
                            return ServiceUtil.returnError(e.getMessage());
                        }
                    }

                    productContentCtx.put("contentId", contentId);
                    try {
                        Map<String, Object> serviceResult = dispatcher.runSync("updateProductContent", productContentCtx);
                        if (ServiceUtil.isError(serviceResult)) {
                            return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                        }
                    } catch (GenericServiceException e) {
                        Debug.logError(e, module);
                        return ServiceUtil.returnError(e.getMessage());
                    }
                }
            } else {
                dataResourceCtx.put("dataResourceTypeId", "SHORT_TEXT");
                dataResourceCtx.put("mimeTypeId", "text/html");
                Map<String, Object> dataResourceResult;
                try {
                    dataResourceResult = dispatcher.runSync("createDataResource", dataResourceCtx);
                    if (ServiceUtil.isError(dataResourceResult)) {
                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(dataResourceResult));
                    }
                } catch (GenericServiceException e) {
                    Debug.logError(e, module);
                    return ServiceUtil.returnError(e.getMessage());
                }

                Map<String, Object> contentCtx = new HashMap<>();
                contentCtx.put("contentTypeId", "DOCUMENT");
                contentCtx.put("dataResourceId", dataResourceResult.get("dataResourceId"));
                contentCtx.put("userLogin", userLogin);
                Map<String, Object> contentResult;
                try {
                    contentResult = dispatcher.runSync("createContent", contentCtx);
                    if (ServiceUtil.isError(contentResult)) {
                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(contentResult));
                    }
                } catch (GenericServiceException e) {
                    Debug.logError(e, module);
                    return ServiceUtil.returnError(e.getMessage());
                }

                productContentCtx.put("contentId", contentResult.get("contentId"));
                try {
                    Map<String, Object> serviceResult = dispatcher.runSync("createProductContent", productContentCtx);
                    if (ServiceUtil.isError(serviceResult)) {
                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                    }
                } catch (GenericServiceException e) {
                    Debug.logError(e, module);
                    return ServiceUtil.returnError(e.getMessage());
                }
            }
        }
       return ServiceUtil.returnSuccess();
    }

    /**
     * Finds productId(s) corresponding to a product reference, productId or a GoodIdentification idValue
     * @param ctx the dispatch context
     * @param context productId use to search with productId or goodIdentification.idValue
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
            return ServiceUtil.returnError(e.getMessage());
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

    public static Map<String, Object> addImageForProductPromo(DispatchContext dctx,
            Map<String, ? extends Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String productPromoId = (String) context.get("productPromoId");
        String productPromoContentTypeId = (String) context.get("productPromoContentTypeId");
        ByteBuffer imageData = (ByteBuffer) context.get("uploadedFile");
        String contentId = (String) context.get("contentId");
        Locale locale = (Locale) context.get("locale");

        if (UtilValidate.isNotEmpty(context.get("_uploadedFile_fileName"))) {
            Map<String, Object> imageContext = new HashMap<>();
            imageContext.putAll(context);
            imageContext.put("tenantId",delegator.getDelegatorTenantId());
            String imageFilenameFormat = EntityUtilProperties.getPropertyValue("catalog", "image.filename.format", delegator);

            String imageServerPath = FlexibleStringExpander.expandString(EntityUtilProperties.getPropertyValue("catalog", "image.server.path",delegator), imageContext);
            String imageUrlPrefix = FlexibleStringExpander.expandString(EntityUtilProperties.getPropertyValue("catalog", "image.url.prefix", delegator), imageContext);
            imageServerPath = imageServerPath.endsWith("/") ? imageServerPath.substring(0, imageServerPath.length()-1) : imageServerPath;
            imageUrlPrefix = imageUrlPrefix.endsWith("/") ? imageUrlPrefix.substring(0, imageUrlPrefix.length()-1) : imageUrlPrefix;
            FlexibleStringExpander filenameExpander = FlexibleStringExpander.getInstance(imageFilenameFormat);
            String id = productPromoId + "_Image_" + productPromoContentTypeId.charAt(productPromoContentTypeId.length() - 1);
            String fileLocation = filenameExpander.expandString(UtilMisc.toMap("location", "products", "type", "promo", "id", id));
            String filePathPrefix = "";
            String filenameToUse = fileLocation;
            if (fileLocation.lastIndexOf('/') != -1) {
                filePathPrefix = fileLocation.substring(0, fileLocation.lastIndexOf('/') + 1); // adding 1 to include
                                                                                               // the trailing slash
                filenameToUse = fileLocation.substring(fileLocation.lastIndexOf('/') + 1);
            }

            List<GenericValue> fileExtension;
            try {
                fileExtension = EntityQuery.use(delegator)
                        .from("FileExtension")
                        .where("mimeTypeId", EntityOperator.EQUALS, context.get("_uploadedFile_contentType"))
                        .queryList();
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
                return ServiceUtil.returnError(e.getMessage());
            }

            GenericValue extension = EntityUtil.getFirst(fileExtension);
            if (extension != null) {
                filenameToUse += "." + extension.getString("fileExtensionId");
            }

            File makeResourceDirectory  = new File(imageServerPath + "/" + filePathPrefix);
            if (!makeResourceDirectory.exists()) {
                if (!makeResourceDirectory.mkdirs()) {
                    Debug.logError("Directory :" + makeResourceDirectory.getPath() + ", couldn't be created", module);
                }
            }

            File file = new File(imageServerPath + "/" + filePathPrefix + filenameToUse);

            try {
                RandomAccessFile out = new RandomAccessFile(file, "rw");
                out.write(imageData.array());
                out.close();
            } catch (FileNotFoundException e) {
                Debug.logError(e, module);
                return ServiceUtil.returnError(UtilProperties.getMessage(resource,
                        "ProductImageViewUnableWriteFile", UtilMisc.toMap("fileName", file.getAbsolutePath()), locale));
            } catch (IOException e) {
                Debug.logError(e, module);
                return ServiceUtil.returnError(UtilProperties.getMessage(resource,
                        "ProductImageViewUnableWriteBinaryData", UtilMisc.toMap("fileName", file.getAbsolutePath()), locale));
            }

            String imageUrl = imageUrlPrefix + "/" + filePathPrefix + filenameToUse;

            if (UtilValidate.isNotEmpty(imageUrl) && imageUrl.length() > 0) {
                Map<String, Object> dataResourceCtx = new HashMap<>();
                dataResourceCtx.put("objectInfo", imageUrl);
                dataResourceCtx.put("dataResourceName", context.get("_uploadedFile_fileName"));
                dataResourceCtx.put("userLogin", userLogin);

                Map<String, Object> productPromoContentCtx = new HashMap<>();
                productPromoContentCtx.put("productPromoId", productPromoId);
                productPromoContentCtx.put("productPromoContentTypeId", productPromoContentTypeId);
                productPromoContentCtx.put("fromDate", context.get("fromDate"));
                productPromoContentCtx.put("thruDate", context.get("thruDate"));
                productPromoContentCtx.put("userLogin", userLogin);

                if (UtilValidate.isNotEmpty(contentId)) {
                    GenericValue content = null;
                    try {
                        content = EntityQuery.use(delegator).from("Content").where("contentId", contentId).queryOne();
                    } catch (GenericEntityException e) {
                        Debug.logError(e, module);
                        return ServiceUtil.returnError(e.getMessage());
                    }

                    if (content != null) {
                        GenericValue dataResource = null;
                        try {
                            dataResource = content.getRelatedOne("DataResource", false);
                        } catch (GenericEntityException e) {
                            Debug.logError(e, module);
                            return ServiceUtil.returnError(e.getMessage());
                        }

                        if (dataResource != null) {
                            dataResourceCtx.put("dataResourceId", dataResource.getString("dataResourceId"));
                            try {
                                Map<String, Object> serviceResult = dispatcher.runSync("updateDataResource", dataResourceCtx);
                                if (ServiceUtil.isError(serviceResult)) {
                                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                                }
                            } catch (GenericServiceException e) {
                                Debug.logError(e, module);
                                return ServiceUtil.returnError(e.getMessage());
                            }
                        } else {
                            dataResourceCtx.put("dataResourceTypeId", "SHORT_TEXT");
                            dataResourceCtx.put("mimeTypeId", "text/html");
                            Map<String, Object> dataResourceResult;
                            try {
                                dataResourceResult = dispatcher.runSync("createDataResource", dataResourceCtx);
                                if (ServiceUtil.isError(dataResourceResult)) {
                                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(dataResourceResult));
                                }
                            } catch (GenericServiceException e) {
                                Debug.logError(e, module);
                                return ServiceUtil.returnError(e.getMessage());
                            }

                            Map<String, Object> contentCtx = new HashMap<>();
                            contentCtx.put("contentId", contentId);
                            contentCtx.put("dataResourceId", dataResourceResult.get("dataResourceId"));
                            contentCtx.put("userLogin", userLogin);
                            try {
                                Map<String, Object> serviceResult = dispatcher.runSync("updateContent", contentCtx);
                                if (ServiceUtil.isError(serviceResult)) {
                                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                                }
                            } catch (GenericServiceException e) {
                                Debug.logError(e, module);
                                return ServiceUtil.returnError(e.getMessage());
                            }
                        }

                        productPromoContentCtx.put("contentId", contentId);
                        try {
                            Map<String, Object> serviceResult = dispatcher.runSync("updateProductPromoContent", productPromoContentCtx);
                            if (ServiceUtil.isError(serviceResult)) {
                                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                            }
                        } catch (GenericServiceException e) {
                            Debug.logError(e, module);
                            return ServiceUtil.returnError(e.getMessage());
                        }
                    }
                } else {
                    dataResourceCtx.put("dataResourceTypeId", "SHORT_TEXT");
                    dataResourceCtx.put("mimeTypeId", "text/html");
                    Map<String, Object> dataResourceResult;
                    try {
                        dataResourceResult = dispatcher.runSync("createDataResource", dataResourceCtx);
                        if (ServiceUtil.isError(dataResourceResult)) {
                            return ServiceUtil.returnError(ServiceUtil.getErrorMessage(dataResourceResult));
                        }
                    } catch (GenericServiceException e) {
                        Debug.logError(e, module);
                        return ServiceUtil.returnError(e.getMessage());
                    }

                    Map<String, Object> contentCtx = new HashMap<>();
                    contentCtx.put("contentTypeId", "DOCUMENT");
                    contentCtx.put("dataResourceId", dataResourceResult.get("dataResourceId"));
                    contentCtx.put("userLogin", userLogin);
                    Map<String, Object> contentResult;
                    try {
                        contentResult = dispatcher.runSync("createContent", contentCtx);
                        if (ServiceUtil.isError(contentResult)) {
                            return ServiceUtil.returnError(ServiceUtil.getErrorMessage(contentResult));
                        }
                    } catch (GenericServiceException e) {
                        Debug.logError(e, module);
                        return ServiceUtil.returnError(e.getMessage());
                    }

                    productPromoContentCtx.put("contentId", contentResult.get("contentId"));
                    try {
                        Map<String, Object> serviceResult = dispatcher.runSync("createProductPromoContent", productPromoContentCtx);
                        if (ServiceUtil.isError(serviceResult)) {
                            return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                        }
                    } catch (GenericServiceException e) {
                        Debug.logError(e, module);
                        return ServiceUtil.returnError(e.getMessage());
                    }
                }
            }
        } else {
            Map<String, Object> productPromoContentCtx = new HashMap<>();
            productPromoContentCtx.put("productPromoId", productPromoId);
            productPromoContentCtx.put("productPromoContentTypeId", productPromoContentTypeId);
            productPromoContentCtx.put("contentId", contentId);
            productPromoContentCtx.put("fromDate", context.get("fromDate"));
            productPromoContentCtx.put("thruDate", context.get("thruDate"));
            productPromoContentCtx.put("userLogin", userLogin);
            try {
                Map<String, Object> serviceResult = dispatcher.runSync("updateProductPromoContent", productPromoContentCtx);
                if (ServiceUtil.isError(serviceResult)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                }
            } catch (GenericServiceException e) {
                Debug.logError(e, module);
                return ServiceUtil.returnError(e.getMessage());
            }
        }
        return ServiceUtil.returnSuccess();
    }
}
