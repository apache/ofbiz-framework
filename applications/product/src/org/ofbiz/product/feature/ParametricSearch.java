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

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

import javolution.util.FastList;
import javolution.util.FastMap;
import javolution.util.FastSet;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.util.EntityListIterator;
import org.ofbiz.entity.util.EntityUtil;

/**
 *  Utilities for parametric search based on features.
 */
public class ParametricSearch {

    public static final String module = ParametricSearch.class.getName();

    public static final int DEFAULT_PER_TYPE_MAX_SIZE = 1000;

    // DEJ20060427 not used right now, could be removed if that circumstance persists
    //public static UtilCache featureAllCache = UtilCache.createUtilCache("custom.FeaturePerTypeAll", 0, 600000, true);
    //public static UtilCache featureByCategoryCache = UtilCache.createUtilCache("custom.FeaturePerTypeByCategory", 0, 600000, true);

    /** Gets all features associated with the specified category through:
     * ProductCategory -> ProductFeatureCategoryAppl -> ProductFeatureCategory -> ProductFeature.
     * Returns a Map of Lists of ProductFeature GenericValue objects organized by productFeatureTypeId.
     */
    public static Map<String, List<GenericValue>> makeCategoryFeatureLists(String productCategoryId, Delegator delegator) {
        return makeCategoryFeatureLists(productCategoryId, delegator, DEFAULT_PER_TYPE_MAX_SIZE);
    }

    public static Map<String, List<GenericValue>> makeCategoryFeatureLists(String productCategoryId, Delegator delegator, int perTypeMaxSize) {
        Map<String, Map<String, GenericValue>> productFeaturesByTypeMap = FastMap.newInstance();
        try {
            List<GenericValue> productFeatureCategoryAppls = delegator.findByAndCache("ProductFeatureCategoryAppl", UtilMisc.toMap("productCategoryId", productCategoryId));
            productFeatureCategoryAppls = EntityUtil.filterByDate(productFeatureCategoryAppls, true);
            if (productFeatureCategoryAppls != null) {
                for (GenericValue productFeatureCategoryAppl: productFeatureCategoryAppls) {
                    List<GenericValue> productFeatures = delegator.findByAndCache("ProductFeature", UtilMisc.toMap("productFeatureCategoryId", productFeatureCategoryAppl.get("productFeatureCategoryId")));
                    for (GenericValue productFeature: productFeatures) {
                        String productFeatureTypeId = productFeature.getString("productFeatureTypeId");
                        Map<String, GenericValue> featuresByType = productFeaturesByTypeMap.get(productFeatureTypeId);
                        if (featuresByType == null) {
                            featuresByType = FastMap.newInstance();
                            productFeaturesByTypeMap.put(productFeatureTypeId, featuresByType);
                        }
                        if (featuresByType.size() < perTypeMaxSize) {
                            featuresByType.put(productFeature.getString("productFeatureId"), productFeature);
                        }
                    }
                }
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, "Error getting feature categories associated with the category with ID: " + productCategoryId, module);
        }

        try {
            List<GenericValue> productFeatureCatGrpAppls = delegator.findByAndCache("ProductFeatureCatGrpAppl", UtilMisc.toMap("productCategoryId", productCategoryId));
            productFeatureCatGrpAppls = EntityUtil.filterByDate(productFeatureCatGrpAppls, true);
            if (productFeatureCatGrpAppls != null) {
                for (GenericValue productFeatureCatGrpAppl: productFeatureCatGrpAppls) {
                    List<GenericValue> productFeatureGroupAppls = delegator.findByAndCache("ProductFeatureGroupAppl", UtilMisc.toMap("productFeatureGroupId", productFeatureCatGrpAppl.get("productFeatureGroupId")));
                    for (GenericValue productFeatureGroupAppl: productFeatureGroupAppls) {
                        GenericValue productFeature = delegator.findByPrimaryKeyCache("ProductFeature", UtilMisc.toMap("productFeatureId", productFeatureGroupAppl.get("productFeatureId")));

                        String productFeatureTypeId = productFeature.getString("productFeatureTypeId");
                        Map<String, GenericValue> featuresByType = productFeaturesByTypeMap.get(productFeatureTypeId);
                        if (featuresByType == null) {
                            featuresByType = FastMap.newInstance();
                            productFeaturesByTypeMap.put(productFeatureTypeId, featuresByType);
                        }
                        if (featuresByType.size() < perTypeMaxSize) {
                            featuresByType.put(productFeature.getString("productFeatureId"), productFeature);
                        }
                    }
                }
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, "Error getting feature groups associated with the category with ID: " + productCategoryId, module);
        }

        // now before returning, order the features in each list by description
        Map<String, List<GenericValue>> productFeaturesByTypeMapSorted = FastMap.newInstance();
        for (Map.Entry<String, Map<String, GenericValue>> entry: productFeaturesByTypeMap.entrySet()) {
            List<GenericValue> sortedFeatures = EntityUtil.orderBy(entry.getValue().values(), UtilMisc.toList("description"));
            productFeaturesByTypeMapSorted.put(entry.getKey(), sortedFeatures);
        }

        return productFeaturesByTypeMapSorted;
    }

    public static Map<String, List<GenericValue>> getAllFeaturesByType(Delegator delegator) {
        return getAllFeaturesByType(delegator, DEFAULT_PER_TYPE_MAX_SIZE);
    }
    public static Map<String, List<GenericValue>> getAllFeaturesByType(Delegator delegator, int perTypeMaxSize) {
        Map<String, List<GenericValue>> productFeaturesByTypeMap = FastMap.newInstance();
        try {
            Set<String> typesWithOverflowMessages = FastSet.newInstance();
            EntityListIterator productFeatureEli = delegator.find("ProductFeature", null, null, null, UtilMisc.toList("description"), null);
            GenericValue productFeature = null;
            while ((productFeature = productFeatureEli.next()) != null) {
                String productFeatureTypeId = productFeature.getString("productFeatureTypeId");
                List<GenericValue> featuresByType = productFeaturesByTypeMap.get(productFeatureTypeId);
                if (featuresByType == null) {
                    featuresByType = FastList.newInstance();
                    productFeaturesByTypeMap.put(productFeatureTypeId, featuresByType);
                }
                if (featuresByType.size() > perTypeMaxSize) {
                    if (!typesWithOverflowMessages.contains(productFeatureTypeId)) {
                        typesWithOverflowMessages.add(productFeatureTypeId);
                        // TODO: uh oh, how do we pass this message back? no biggie for now
                    }
                } else {
                    featuresByType.add(productFeature);
                }
            }
            productFeatureEli.close();
        } catch (GenericEntityException e) {
            Debug.logError(e, "Error getting all features", module);
        }
        return productFeaturesByTypeMap;
    }

    public static Map<String, String> makeFeatureIdByTypeMap(ServletRequest request) {
        Map<String, Object> parameters = UtilHttp.getParameterMap((HttpServletRequest) request);
        return makeFeatureIdByTypeMap(parameters);
    }

    /** Handles parameters coming in prefixed with "pft_" where the text in the key following the prefix is a productFeatureTypeId and the value is a productFeatureId; meant to be used with drop-downs and such */
    public static Map<String, String> makeFeatureIdByTypeMap(Map<String, Object> parameters) {
        Map<String, String> featureIdByType = FastMap.newInstance();
        if (parameters == null) return featureIdByType;


        for (Map.Entry<String, Object> entry: parameters.entrySet()) {
            String parameterName = entry.getKey();
            if (parameterName.startsWith("pft_")) {
                String productFeatureTypeId = parameterName.substring(4);
                String productFeatureId = (String) entry.getValue();
                if (UtilValidate.isNotEmpty(productFeatureId)) {
                    featureIdByType.put(productFeatureTypeId, productFeatureId);
                }
            }
        }

        return featureIdByType;
    }

    /** Handles parameters coming in prefixed with "SEARCH_FEAT" where the parameter value is a productFeatureId; meant to be used with text entry boxes or check-boxes and such */
    public static List<String> makeFeatureIdListFromPrefixed(Map<String, Object> parameters) {
        List<String> featureIdList = FastList.newInstance();
        if (parameters == null) return featureIdList;

        for (Map.Entry<String, Object> entry: parameters.entrySet()) {
            String parameterName = entry.getKey();
            if (parameterName.startsWith("SEARCH_FEAT")) {
                String productFeatureId = (String) entry.getValue();
                if (UtilValidate.isNotEmpty(productFeatureId)) {
                    featureIdList.add(productFeatureId);
                }
            }
        }

        return featureIdList;
    }

    public static String makeFeatureIdByTypeString(Map<String, String> featureIdByType) {
        if (UtilValidate.isEmpty(featureIdByType)) {
            return "";
        }

        StringBuilder outSb = new StringBuilder();
        for (Map.Entry<String, String> entry: featureIdByType.entrySet()) {
            if (outSb.length() > 0) {
                outSb.append('&');
            }
            String productFeatureTypeId = entry.getKey();
            String productFeatureId = entry.getValue();
            outSb.append(productFeatureTypeId);
            outSb.append('=');
            outSb.append(productFeatureId);
        }

        return outSb.toString();
    }

    /**
     *  Handles parameters coming in prefixed with "SEARCH_PROD_FEAT_CAT"
     *  where the parameter value is a productFeatureCategoryId;
     *  meant to be used with text entry boxes or check-boxes and such
     **/
    public static List<String> makeProductFeatureCategoryIdListFromPrefixed(Map<String, Object> parameters) {
        List<String> prodFeatureCategoryIdList = FastList.newInstance();
        if (parameters == null) return prodFeatureCategoryIdList;

        for (Map.Entry<String, Object> entry: parameters.entrySet()) {
            String parameterName = entry.getKey();
            if (parameterName.startsWith("SEARCH_PROD_FEAT_CAT")) {
                String productFeatureCategoryId = (String) entry.getValue();
                if (UtilValidate.isNotEmpty(productFeatureCategoryId)) {
                   prodFeatureCategoryIdList.add(productFeatureCategoryId);
                }
            }
        }
        return prodFeatureCategoryIdList;
    }
}
