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
package org.ofbiz.product.feature;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntity;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ModelService;
import org.ofbiz.service.ServiceUtil;


/**
 * Services for product features
 */

public class ProductFeatureServices {

    public static final String module = ProductFeatureServices.class.getName();
    public static final String resource = "ProductUiLabels";
    
    /*
     * Parameters: productFeatureCategoryId, productFeatureGroupId, productId, productFeatureApplTypeId
     * Result: productFeaturesByType, a Map of all product features from productFeatureCategoryId, group by productFeatureType -> List of productFeatures
     * If the parameter were productFeatureCategoryId, the results are from ProductFeatures.  If productFeatureCategoryId were null and there were a productFeatureGroupId,
     * the results are from ProductFeatureGroupAndAppl.  Otherwise, if there is a productId, the results are from ProductFeatureAndAppl.
     * The optional productFeatureApplTypeId causes results to be filtered by this parameter--only used in conjunction with productId.
     */
    public static Map getProductFeaturesByType(DispatchContext dctx, Map context) {
        Map results = new HashMap();
        GenericDelegator delegator = dctx.getDelegator();

        /* because we might need to search either for product features or for product features of a product, the search code has to be generic.
         * we will determine which entity and field to search on based on what the user has supplied us with.
         */
        String valueToSearch = (String) context.get("productFeatureCategoryId");
        String productFeatureApplTypeId = (String) context.get("productFeatureApplTypeId");
        
        String entityToSearch = "ProductFeature";
        String fieldToSearch = "productFeatureCategoryId";
        List orderBy = UtilMisc.toList("productFeatureTypeId", "description");
        
        if (valueToSearch == null && context.get("productFeatureGroupId") != null) {
            entityToSearch = "ProductFeatureGroupAndAppl";
            fieldToSearch = "productFeatureGroupId";
            valueToSearch = (String) context.get("productFeatureGroupId");
            // use same orderBy as with a productFeatureCategoryId search
        } else if (valueToSearch == null && context.get("productId") != null){
            entityToSearch = "ProductFeatureAndAppl";
            fieldToSearch = "productId";
            valueToSearch = (String) context.get("productId");
            orderBy = UtilMisc.toList("sequenceNum", "productFeatureApplTypeId", "productFeatureTypeId", "description");
        }
        
        if (valueToSearch == null) {
            return ServiceUtil.returnError("This service requires a productId, a productFeatureGroupId, or a productFeatureCategoryId to run.");
        }
        
        try {
            // get all product features in this feature category
            List allFeatures = delegator.findByAnd(entityToSearch, UtilMisc.toMap(fieldToSearch, valueToSearch), orderBy);
        
            if (entityToSearch.equals("ProductFeatureAndAppl") && productFeatureApplTypeId != null)
                allFeatures = EntityUtil.filterByAnd(allFeatures, UtilMisc.toMap("productFeatureApplTypeId", productFeatureApplTypeId));
                
            List featureTypes = new ArrayList();  // or LinkedList?
            Map featuresByType = new LinkedHashMap();
            GenericValue feature = null;
            for (Iterator featuresIter = allFeatures.iterator(); featuresIter.hasNext(); ) {
                feature = (GenericValue) featuresIter.next();
                String featureType = feature.getString("productFeatureTypeId");
                if (!featureTypes.contains(featureType)) {
                    featureTypes.add(featureType);
                }
                if (!featuresByType.containsKey(featureType)) {
                    featuresByType.put(featureType, new ArrayList());
                }
                List features = (List)featuresByType.get(featureType);
                features.add(feature);
            }

            results = ServiceUtil.returnSuccess();
            results.put("productFeatureTypes", featureTypes);
            results.put("productFeaturesByType", featuresByType);
        } catch (GenericEntityException ex) {
            Debug.logError(ex, ex.getMessage(), module);
            return ServiceUtil.returnError(ex.getMessage());
        }
        return results;
    }
    
    /*
     * Parameter: productId, productFeatureAppls (a List of ProductFeatureAndAppl entities of features applied to productId)
     * Result: variantProductIds: a List of productIds of variants with those features
     */
    public static Map getAllExistingVariants(DispatchContext dctx, Map context) {
        Map results = new HashMap();
        GenericDelegator delegator = dctx.getDelegator();

        String productId = (String) context.get("productId");
        List curProductFeatureAndAppls = (List) context.get("productFeatureAppls");
        List existingVariantProductIds = new ArrayList();
        
        try {
            /*
             * get a list of all products which are associated with the current one as PRODUCT_VARIANT and for each one, 
             * see if it has every single feature in the list of productFeatureAppls as a STANDARD_FEATURE.  If so, then 
             * it qualifies and add it to the list of existingVariantProductIds.
             */
            List productAssocs = EntityUtil.filterByDate(delegator.findByAnd("ProductAssoc", UtilMisc.toMap("productId", productId, "productAssocTypeId", "PRODUCT_VARIANT")), true);
            if (productAssocs != null && productAssocs.size() > 0) {
                Iterator productAssocIter = productAssocs.iterator();
                while (productAssocIter.hasNext()) {
                    GenericEntity productAssoc = (GenericEntity) productAssocIter.next();
                    
                    //for each associated product, if it has all standard features, display it's productId
                    boolean hasAllFeatures = true;
                    Iterator curProductFeatureAndApplIter = curProductFeatureAndAppls.iterator();
                    while (curProductFeatureAndApplIter.hasNext()) {
                        String productFeatureAndAppl = (String) curProductFeatureAndApplIter.next();
                        Map findByMap = UtilMisc.toMap("productId", productAssoc.getString("productIdTo"), 
                                "productFeatureId", productFeatureAndAppl,
                                "productFeatureApplTypeId", "STANDARD_FEATURE");

                        //Debug.log("Using findByMap: " + findByMap);

                        List standardProductFeatureAndAppls = EntityUtil.filterByDate(delegator.findByAnd("ProductFeatureAppl", findByMap), true);
                        if (standardProductFeatureAndAppls == null || standardProductFeatureAndAppls.size() == 0) {
                            // Debug.log("Does NOT have this standard feature");
                            hasAllFeatures = false;
                            break;
                        } else {
                            // Debug.log("DOES have this standard feature");
                        }
                    }

                    if (hasAllFeatures) {
                        // add to list of existing variants: productId=productAssoc.productIdTo
                        existingVariantProductIds.add(productAssoc.get("productIdTo"));
                    }
                }
            }
            results = ServiceUtil.returnSuccess();
            results.put("variantProductIds", existingVariantProductIds);
        } catch (GenericEntityException ex) {
            Debug.logError(ex, ex.getMessage(), module);
            return ServiceUtil.returnError(ex.getMessage());
        }
    return results;
    }

    /*
     * Parameter: productId (of the parent product which has SELECTABLE features)
     * Result: featureCombinations, a List of Maps containing, for each possible variant of the productid: 
     * {defaultVariantProductId: id of this variant; curProductFeatureAndAppls: features applied to this variant; existingVariantProductIds: List of productIds which are already variants with these features }
     */
    public static Map getVariantCombinations(DispatchContext dctx, Map context) {
        Map results = new HashMap();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        
        String productId = (String) context.get("productId");
        
        try {
            Map featuresResults = dispatcher.runSync("getProductFeaturesByType", UtilMisc.toMap("productId", productId));
            Map features = new HashMap();
            
            if (featuresResults.get(ModelService.RESPONSE_MESSAGE).equals(ModelService.RESPOND_SUCCESS)) {
                features = (Map) featuresResults.get("productFeaturesByType");
            } else {
                return ServiceUtil.returnError((String) featuresResults.get(ModelService.ERROR_MESSAGE_LIST));
            }
            
            // need to keep 2 lists, oldCombinations and newCombinations, and keep swapping them after each looping.  Otherwise, you'll get a
            // concurrent modification exception
            List oldCombinations = new LinkedList();
            
            // loop through each feature type
            for (Iterator fi = features.keySet().iterator(); fi.hasNext(); ) {
                String currentFeatureType = (String) fi.next();
                List currentFeatures = (List) features.get(currentFeatureType);
                
                List newCombinations = new LinkedList();
                List combinations;
                
                // start with either existing combinations or from scratch
                if (oldCombinations.size() > 0) {
                    combinations = oldCombinations;
                } else {
                    combinations = new LinkedList();
                }
                
                // in both cases, use each feature of current feature type's idCode and
                // product feature and add it to the id code and product feature applications
                // of the next variant.  just a matter of whether we're starting with an
                // existing list of features and id code or from scratch.
                if (combinations.size()==0) {
                    for (Iterator cFi = currentFeatures.iterator(); cFi.hasNext(); ) {
                        GenericEntity currentFeature = (GenericEntity) cFi.next();
                        if (currentFeature.getString("productFeatureApplTypeId").equals("SELECTABLE_FEATURE")) {
                            Map newCombination = new HashMap();
                            List newFeatures = new LinkedList();
                            List newFeatureIds = new LinkedList();
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
                    for (Iterator comboIt = combinations.iterator(); comboIt.hasNext(); ) {
                        Map combination = (Map) comboIt.next();
                        for (Iterator cFi = currentFeatures.iterator(); cFi.hasNext(); ) {
                            GenericEntity currentFeature = (GenericEntity) cFi.next();
                            if (currentFeature.getString("productFeatureApplTypeId").equals("SELECTABLE_FEATURE")) {
                                Map newCombination = new HashMap();
                                // .clone() is important, or you'll keep adding to the same List for all the variants
                                // have to cast twice: once from get() and once from clone()
                                List newFeatures = ((List) ((LinkedList) combination.get("curProductFeatureAndAppls")).clone());
                                List newFeatureIds = ((List) ((LinkedList) combination.get("curProductFeatureIds")).clone());
                                if (currentFeature.getString("idCode") != null) {
                                    newCombination.put("defaultVariantProductId", combination.get("defaultVariantProductId") + currentFeature.getString("idCode"));
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
            HashMap defaultVariantProductIds = new HashMap(); // this map will contain the codes already used (as keys)
            defaultVariantProductIds.put(productId, null);
            
            // now figure out which of these combinations already have productIds associated with them
            for (Iterator fCi = oldCombinations.iterator(); fCi.hasNext(); ) {
                Map combination = (Map) fCi.next();
                // Verify if the default code is already used, if so add a numeric suffix
                if (defaultVariantProductIds.containsKey(combination.get("defaultVariantProductId"))) {
                    combination.put("defaultVariantProductId", combination.get("defaultVariantProductId") + (defaultCodeCounter < 10? "0" + defaultCodeCounter: "" + defaultCodeCounter));
                    defaultCodeCounter++;
                }
                defaultVariantProductIds.put(combination.get("defaultVariantProductId"), null);
                results = dispatcher.runSync("getAllExistingVariants", UtilMisc.toMap("productId", productId,
                                             "productFeatureAppls", combination.get("curProductFeatureIds")));
                combination.put("existingVariantProductIds", results.get("variantProductIds"));
            }
            results = ServiceUtil.returnSuccess();
            results.put("featureCombinations", oldCombinations);
        } catch (GenericServiceException ex) {
            Debug.logError(ex, ex.getMessage(), module);
            return ServiceUtil.returnError(ex.getMessage());
        }
        
        return results;
    }

    /* 
     * Parameters: productCategoryId (String) and productFeatures (a List of ProductFeature GenericValues)
     * Result: products (a List of Product GenericValues)
     */
    public static Map getCategoryVariantProducts(DispatchContext dctx, Map context) {
        Map results = new HashMap();
        LocalDispatcher dispatcher = dctx.getDispatcher();

        List productFeatures = (List) context.get("productFeatures");
        String productCategoryId = (String) context.get("productCategoryId");

        // get all the product members of the product category
        Map result = new HashMap();
        try {
            result = dispatcher.runSync("getProductCategoryMembers", UtilMisc.toMap("categoryId", productCategoryId));
        } catch (GenericServiceException ex) {
            Debug.logError("Cannot get category memebers for " + productCategoryId + " due to error: " + ex.getMessage(), module);
            return ServiceUtil.returnError(ex.getMessage());
        }

        List memberProducts = (List) result.get("categoryMembers");
        if ((memberProducts != null) && (memberProducts.size() > 0)) {
            // construct a Map of productFeatureTypeId -> productFeatureId from the productFeatures List
            Map featuresByType = new HashMap();
            for (Iterator pFi = productFeatures.iterator(); pFi.hasNext(); ) {
                GenericValue nextFeature = (GenericValue) pFi.next();
                featuresByType.put(nextFeature.getString("productFeatureTypeId"), nextFeature.getString("productFeatureId"));
            }

            List products = new ArrayList();  // final list of variant products  
            for (Iterator mPi = memberProducts.iterator(); mPi.hasNext(); ) {
                // find variants for each member product of the category
                GenericValue memberProduct = (GenericValue) mPi.next();

                try {
                    result = dispatcher.runSync("getProductVariant", UtilMisc.toMap("productId", memberProduct.getString("productId"), "selectedFeatures", featuresByType));
                } catch (GenericServiceException ex) {
                    Debug.logError("Cannot get product variants for " + memberProduct.getString("productId") + " due to error: " + ex.getMessage(), module);
                    return ServiceUtil.returnError(ex.getMessage());
                }

                List variantProducts = (List) result.get("products");
                if ((variantProducts != null) && (variantProducts.size() > 0)) {
                    products.addAll(variantProducts);
                } else {
                    Debug.logWarning("Product " + memberProduct.getString("productId") + " did not have any variants for the given features", module);
                }
            }

            if (products.size() == 0) {
                return ServiceUtil.returnError("No products which fit your requirements were found.");
            } else {
                results = ServiceUtil.returnSuccess();
                results.put("products", products);    
            }

        } else {
            Debug.logWarning("No products found in " + productCategoryId, module);
        }

        return results;
    }
}
