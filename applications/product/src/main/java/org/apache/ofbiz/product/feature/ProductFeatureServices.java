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
package org.apache.ofbiz.product.feature;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.service.ServiceUtil;

/**
 * Services for product features
 */

public class ProductFeatureServices {

    private static final String MODULE = ProductFeatureServices.class.getName();
    private static final String RESOURCE = "ProductUiLabels";

    /*
     * Parameters: productFeatureCategoryId, productFeatureGroupId, productId, productFeatureApplTypeId
     * Result: productFeaturesByType, a Map of all product features from productFeatureCategoryId, group by productFeatureType ->
     * List of productFeatures
     * If the parameter were productFeatureCategoryId, the results are from ProductFeatures.  If productFeatureCategoryId were null
     * and there were a productFeatureGroupId,
     * the results are from ProductFeatureGroupAndAppl.  Otherwise, if there is a productId, the results are from ProductFeatureAndAppl.
     * The optional productFeatureApplTypeId causes results to be filtered by this parameter--only used in conjunction with productId.
     */
    public static Map<String, Object> getProductFeaturesByType(DispatchContext dctx, Map<String, ? extends Object> context) {
        Map<String, Object> results;
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get("locale");

        /* because we might need to search either for product features or for product features of a product, the search code has to be generic.
         * we will determine which entity and field to search on based on what the user has supplied us with.
         */
        String valueToSearch = (String) context.get("productFeatureCategoryId");
        String productFeatureApplTypeId = (String) context.get("productFeatureApplTypeId");

        String entityToSearch = "ProductFeature";
        String fieldToSearch = "productFeatureCategoryId";
        List<String> orderBy = UtilMisc.toList("productFeatureTypeId", "description");

        if (valueToSearch == null && context.get("productFeatureGroupId") != null) {
            entityToSearch = "ProductFeatureGroupAndAppl";
            fieldToSearch = "productFeatureGroupId";
            valueToSearch = (String) context.get("productFeatureGroupId");
            // use same orderBy as with a productFeatureCategoryId search
        } else if (valueToSearch == null && context.get("productId") != null) {
            entityToSearch = "ProductFeatureAndAppl";
            fieldToSearch = "productId";
            valueToSearch = (String) context.get("productId");
            orderBy = UtilMisc.toList("sequenceNum", "productFeatureApplTypeId", "productFeatureTypeId", "description");
        }

        if (valueToSearch == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ProductFeatureByType", locale));
        }

        try {
            // get all product features in this feature category
            List<GenericValue> allFeatures = EntityQuery.use(delegator).from(entityToSearch).where(fieldToSearch, valueToSearch)
                    .orderBy(orderBy).queryList();

            if ("ProductFeatureAndAppl".equals(entityToSearch) && productFeatureApplTypeId != null) {
                allFeatures = EntityUtil.filterByAnd(allFeatures, UtilMisc.toMap("productFeatureApplTypeId", productFeatureApplTypeId));
            }

            List<String> featureTypes = new LinkedList<>();
            Map<String, List<GenericValue>> featuresByType = new LinkedHashMap<>();
            for (GenericValue feature: allFeatures) {
                String featureType = feature.getString("productFeatureTypeId");
                if (!featureTypes.contains(featureType)) {
                    featureTypes.add(featureType);
                }
                List<GenericValue> features = featuresByType.get(featureType);
                if (features == null) {
                    features = new LinkedList<>();
                    featuresByType.put(featureType, features);
                }
                features.add(feature);
            }

            results = ServiceUtil.returnSuccess();
            results.put("productFeatureTypes", featureTypes);
            results.put("productFeaturesByType", featuresByType);
        } catch (GenericEntityException ex) {
            Debug.logError(ex, ex.getMessage(), MODULE);
            return ServiceUtil.returnError(ex.getMessage());
        }
        return results;
    }

    /*
     * Parameter: productId, productFeatureAppls (a List of ProductFeatureAndAppl entities of features applied to productId)
     * Result: variantProductIds: a List of productIds of variants with those features
     */
    public static Map<String, Object> getAllExistingVariants(DispatchContext dctx, Map<String, ? extends Object> context) {
        Map<String, Object> results;
        Delegator delegator = dctx.getDelegator();

        String productId = (String) context.get("productId");
        List<String> curProductFeatureAndAppls = UtilGenerics.cast(context.get("productFeatureAppls"));
        List<String> existingVariantProductIds = new LinkedList<>();

        try {
            /*
             * get a list of all products which are associated with the current one as PRODUCT_VARIANT and for each one,
             * see if it has every single feature in the list of productFeatureAppls as a STANDARD_FEATURE.  If so, then
             * it qualifies and add it to the list of existingVariantProductIds.
             */
            List<GenericValue> productAssocs = EntityQuery.use(delegator).from("ProductAssoc").where("productId", productId, "productAssocTypeId",
                    "PRODUCT_VARIANT").filterByDate().queryList();
            for (GenericValue productAssoc: productAssocs) {

                //for each associated product, if it has all standard features, display it's productId
                boolean hasAllFeatures = true;
                for (String productFeatureAndAppl: curProductFeatureAndAppls) {
                    Map<String, String> findByMap = UtilMisc.toMap("productId", productAssoc.getString("productIdTo"),
                            "productFeatureId", productFeatureAndAppl,
                            "productFeatureApplTypeId", "STANDARD_FEATURE");

                    List<GenericValue> standardProductFeatureAndAppls = EntityQuery.use(delegator).from("ProductFeatureAppl").where(findByMap)
                            .filterByDate().queryList();
                    if (UtilValidate.isEmpty(standardProductFeatureAndAppls)) {
                        hasAllFeatures = false;
                        break;
                    }
                }
                if (hasAllFeatures) {
                    // add to list of existing variants: productId=productAssoc.productIdTo
                    existingVariantProductIds.add(productAssoc.getString("productIdTo"));
                }
            }
            results = ServiceUtil.returnSuccess();
            results.put("variantProductIds", existingVariantProductIds);
        } catch (GenericEntityException ex) {
            Debug.logError(ex, ex.getMessage(), MODULE);
            return ServiceUtil.returnError(ex.getMessage());
        }
        return results;
    }

    /*
     * Parameter: productId (of the parent product which has SELECTABLE features)
     * Result: featureCombinations, a List of Maps containing, for each possible variant of the productid:
     * {defaultVariantProductId: id of this variant; curProductFeatureAndAppls: features applied to this variant;
     * existingVariantProductIds: List of productIds which are already variants with these features }
     */
    public static Map<String, Object> getVariantCombinations(DispatchContext dctx, Map<String, ? extends Object> context) {
        Map<String, Object> results;
        LocalDispatcher dispatcher = dctx.getDispatcher();

        String productId = (String) context.get("productId");

        try {
            Map<String, Object> featuresResults = dispatcher.runSync("getProductFeaturesByType", UtilMisc.toMap("productId", productId));
            Map<String, List<GenericValue>> features;

            if (featuresResults.get(ModelService.RESPONSE_MESSAGE).equals(ModelService.RESPOND_SUCCESS)) {
                features = UtilGenerics.cast(featuresResults.get("productFeaturesByType"));
            } else {
                return ServiceUtil.returnError((String) featuresResults.get(ModelService.ERROR_MESSAGE_LIST));
            }

            // need to keep 2 lists, oldCombinations and newCombinations, and keep swapping them after each looping.  Otherwise, you'll get a
            // concurrent modification exception
            List<Map<String, Object>> oldCombinations = new LinkedList<>();

            // loop through each feature type
            for (Map.Entry<String, List<GenericValue>> entry: features.entrySet()) {
                List<GenericValue> currentFeatures = entry.getValue();

                List<Map<String, Object>> newCombinations = new LinkedList<>();
                List<Map<String, Object>> combinations;

                // start with either existing combinations or from scratch
                if (!oldCombinations.isEmpty()) {
                    combinations = oldCombinations;
                } else {
                    combinations = new LinkedList<>();
                }

                // in both cases, use each feature of current feature type's idCode and
                // product feature and add it to the id code and product feature applications
                // of the next variant.  just a matter of whether we're starting with an
                // existing list of features and id code or from scratch.
                if (combinations.isEmpty()) {
                    for (GenericValue currentFeature: currentFeatures) {
                        if ("SELECTABLE_FEATURE".equals(currentFeature.getString("productFeatureApplTypeId"))) {
                            Map<String, Object> newCombination = new HashMap<>();
                            List<GenericValue> newFeatures = new LinkedList<>();
                            List<String> newFeatureIds = new LinkedList<>();
                            if (currentFeature.getString("idCode") != null) {
                                newCombination.put("defaultVariantProductId", productId + currentFeature.getString("idCode"));
                            } else {
                                newCombination.put("defaultVariantProductId", productId);
                            }
                            newFeatures.add(currentFeature);
                            newFeatureIds.add(currentFeature.getString("productFeatureId"));
                            newCombination.put("curProductFeatureAndAppls", newFeatures);
                            newCombination.put("curProductFeatureIds", newFeatureIds);
                            newCombinations.add(newCombination);
                        }
                    }
                } else {
                    for (Map<String, Object> combination: combinations) {
                        for (GenericValue currentFeature: currentFeatures) {
                            if ("SELECTABLE_FEATURE".equals(currentFeature.getString("productFeatureApplTypeId"))) {
                                Map<String, Object> newCombination = new HashMap<>();
                                // .clone() is important, or you'll keep adding to the same List for all the variants
                                // have to cast twice: once from get() and once from clone()
                                List<GenericValue> newFeatures = UtilMisc.makeListWritable(UtilGenerics.cast(combination
                                        .get("curProductFeatureAndAppls")));
                                List<String> newFeatureIds = UtilMisc.makeListWritable(UtilGenerics.cast(combination.get("curProductFeatureIds")));
                                if (currentFeature.getString("idCode") != null) {
                                    newCombination.put("defaultVariantProductId", combination.get("defaultVariantProductId")
                                            + currentFeature.getString("idCode"));
                                } else {
                                    newCombination.put("defaultVariantProductId", combination.get("defaultVariantProductId"));
                                }
                                newFeatures.add(currentFeature);
                                newFeatureIds.add(currentFeature.getString("productFeatureId"));
                                newCombination.put("curProductFeatureAndAppls", newFeatures);
                                newCombination.put("curProductFeatureIds", newFeatureIds);
                                newCombinations.add(newCombination);
                            }
                        }
                    }
                }
                if (newCombinations.size() >= oldCombinations.size()) {
                    oldCombinations = newCombinations; // save the newly expanded list as oldCombinations
                }
            }

            int defaultCodeCounter = 1;
            Set<String> defaultVariantProductIds = new HashSet<>(); // this map will contain the codes already used (as keys)
            defaultVariantProductIds.add(productId);

            // now figure out which of these combinations already have productIds associated with them
            for (Map<String, Object> combination: oldCombinations) {
                // Verify if the default code is already used, if so add a numeric suffix
                if (defaultVariantProductIds.contains(combination.get("defaultVariantProductId"))) {
                    combination.put("defaultVariantProductId", combination.get("defaultVariantProductId") + "-" + (defaultCodeCounter < 10 ? "0"
                            + defaultCodeCounter : "" + defaultCodeCounter));
                    defaultCodeCounter++;
                }
                defaultVariantProductIds.add((String) combination.get("defaultVariantProductId"));
                results = dispatcher.runSync("getAllExistingVariants", UtilMisc.toMap("productId", productId,
                                             "productFeatureAppls", combination.get("curProductFeatureIds")));
                combination.put("existingVariantProductIds", results.get("variantProductIds"));
            }
            results = ServiceUtil.returnSuccess();
            results.put("featureCombinations", oldCombinations);
        } catch (GenericServiceException ex) {
            Debug.logError(ex, ex.getMessage(), MODULE);
            return ServiceUtil.returnError(ex.getMessage());
        }

        return results;
    }

    /*
     * Parameters: productCategoryId (String) and productFeatures (a List of ProductFeature GenericValues)
     * Result: products (a List of Product GenericValues)
     */
    public static Map<String, Object> getCategoryVariantProducts(DispatchContext dctx, Map<String, ? extends Object> context) {
        Map<String, Object> results = new HashMap<>();
        LocalDispatcher dispatcher = dctx.getDispatcher();

        List<GenericValue> productFeatures = UtilGenerics.cast(context.get("productFeatures"));
        String productCategoryId = (String) context.get("productCategoryId");
        Locale locale = (Locale) context.get("locale");

        // get all the product members of the product category
        Map<String, Object> result;
        try {
            result = dispatcher.runSync("getProductCategoryMembers", UtilMisc.toMap("categoryId", productCategoryId));
        } catch (GenericServiceException ex) {
            Debug.logError("Cannot get category memebers for " + productCategoryId + " due to error: " + ex.getMessage(), MODULE);
            return ServiceUtil.returnError(ex.getMessage());
        }

        List<GenericValue> memberProducts = UtilGenerics.cast(result.get("categoryMembers"));
        if ((memberProducts != null) && (!memberProducts.isEmpty())) {
            // construct a Map of productFeatureTypeId -> productFeatureId from the productFeatures List
            Map<String, String> featuresByType = new HashMap<>();
            for (GenericValue nextFeature: productFeatures) {
                featuresByType.put(nextFeature.getString("productFeatureTypeId"), nextFeature.getString("productFeatureId"));
            }

            List<GenericValue> products = new LinkedList<>(); // final list of variant products
            for (GenericValue memberProduct: memberProducts) {
                // find variants for each member product of the category

                try {
                    result = dispatcher.runSync("getProductVariant", UtilMisc.toMap("productId", memberProduct.getString("productId"),
                            "selectedFeatures", featuresByType));
                } catch (GenericServiceException ex) {
                    Debug.logError("Cannot get product variants for " + memberProduct.getString("productId") + " due to error: "
                            + ex.getMessage(), MODULE);
                    return ServiceUtil.returnError(ex.getMessage());
                }

                List<GenericValue> variantProducts = UtilGenerics.cast(result.get("products"));
                if ((variantProducts != null) && (!variantProducts.isEmpty())) {
                    products.addAll(variantProducts);
                } else {
                    Debug.logWarning("Product " + memberProduct.getString("productId") + " did not have any variants for the given features", MODULE);
                }
            }

            if (products.isEmpty()) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ProductCategoryNoVariants", locale));
            } else {
                results = ServiceUtil.returnSuccess();
                results.put("products", products);
            }

        } else {
            Debug.logWarning("No products found in " + productCategoryId, MODULE);
        }

        return results;
    }
}
