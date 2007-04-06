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

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

import javolution.util.FastList;
import javolution.util.FastMap;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.GenericDelegator;
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
    //public static UtilCache featureAllCache = new UtilCache("custom.FeaturePerTypeAll", 0, 600000, true);
    //public static UtilCache featureByCategoryCache = new UtilCache("custom.FeaturePerTypeByCategory", 0, 600000, true);
    
    /** Gets all features associated with the specified category through: 
     * ProductCategory -> ProductFeatureCategoryAppl -> ProductFeatureCategory -> ProductFeature.
     * Returns a Map of Lists of ProductFeature GenericValue objects organized by productFeatureTypeId. 
     */
    public static Map makeCategoryFeatureLists(String productCategoryId, GenericDelegator delegator) {
        return makeCategoryFeatureLists(productCategoryId, delegator, DEFAULT_PER_TYPE_MAX_SIZE);
    }
    
    public static Map makeCategoryFeatureLists(String productCategoryId, GenericDelegator delegator, int perTypeMaxSize) {
        Map productFeaturesByTypeMap = FastMap.newInstance();
        try {
            List productFeatureCategoryAppls = delegator.findByAndCache("ProductFeatureCategoryAppl", UtilMisc.toMap("productCategoryId", productCategoryId));
            productFeatureCategoryAppls = EntityUtil.filterByDate(productFeatureCategoryAppls, true);
            if (productFeatureCategoryAppls != null) { 
                Iterator pfcasIter = productFeatureCategoryAppls.iterator();
                while (pfcasIter.hasNext()) {
                    GenericValue productFeatureCategoryAppl = (GenericValue) pfcasIter.next();
                    List productFeatures = delegator.findByAndCache("ProductFeature", UtilMisc.toMap("productFeatureCategoryId", productFeatureCategoryAppl.get("productFeatureCategoryId")));
                    Iterator pfsIter = productFeatures.iterator();
                    while (pfsIter.hasNext()) {
                        GenericValue productFeature = (GenericValue) pfsIter.next();
                        String productFeatureTypeId = productFeature.getString("productFeatureTypeId");
                        Map featuresByType = (Map) productFeaturesByTypeMap.get(productFeatureTypeId);
                        if (featuresByType == null) {
                            featuresByType = FastMap.newInstance();
                            productFeaturesByTypeMap.put(productFeatureTypeId, featuresByType);
                        }
                        if (featuresByType.size() < perTypeMaxSize) {
                            featuresByType.put(productFeature.get("productFeatureId"), productFeature);
                        }
                    }
                }
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, "Error getting feature categories associated with the category with ID: " + productCategoryId, module);
        }
           
        try {
            List productFeatureCatGrpAppls = delegator.findByAndCache("ProductFeatureCatGrpAppl", UtilMisc.toMap("productCategoryId", productCategoryId));
            productFeatureCatGrpAppls = EntityUtil.filterByDate(productFeatureCatGrpAppls, true);
            if (productFeatureCatGrpAppls != null) { 
                Iterator pfcgasIter = productFeatureCatGrpAppls.iterator();
                while (pfcgasIter.hasNext()) {
                    GenericValue productFeatureCatGrpAppl = (GenericValue) pfcgasIter.next();
                    List productFeatureGroupAppls = delegator.findByAndCache("ProductFeatureGroupAppl", UtilMisc.toMap("productFeatureGroupId", productFeatureCatGrpAppl.get("productFeatureGroupId")));
                    Iterator pfgaasIter = productFeatureGroupAppls.iterator();
                    while (pfgaasIter.hasNext()) {
                        GenericValue productFeatureGroupAppl = (GenericValue) pfgaasIter.next();
                        GenericValue productFeature = delegator.findByPrimaryKeyCache("ProductFeature", UtilMisc.toMap("productFeatureId", productFeatureGroupAppl.get("productFeatureId")));
                        
                        String productFeatureTypeId = productFeature.getString("productFeatureTypeId");
                        Map featuresByType = (Map) productFeaturesByTypeMap.get(productFeatureTypeId);
                        if (featuresByType == null) {
                            featuresByType = FastMap.newInstance();
                            productFeaturesByTypeMap.put(productFeatureTypeId, featuresByType);
                        }
                        if (featuresByType.size() < perTypeMaxSize) {
                            featuresByType.put(productFeature.get("productFeatureId"), productFeature);
                        }
                    }
                }
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, "Error getting feature groups associated with the category with ID: " + productCategoryId, module);
        }
           
        // now before returning, order the features in each list by description
        Iterator productFeatureTypeEntries = productFeaturesByTypeMap.entrySet().iterator();
        while (productFeatureTypeEntries.hasNext()) {
            Map.Entry entry = (Map.Entry) productFeatureTypeEntries.next();
            List sortedFeatures = EntityUtil.orderBy(((Map) entry.getValue()).values(), UtilMisc.toList("description"));
            productFeaturesByTypeMap.put(entry.getKey(), sortedFeatures);
        }
        
        return productFeaturesByTypeMap;
    }
    
    public static Map getAllFeaturesByType(GenericDelegator delegator) {
        return getAllFeaturesByType(delegator, DEFAULT_PER_TYPE_MAX_SIZE);
    }
    public static Map getAllFeaturesByType(GenericDelegator delegator, int perTypeMaxSize) {
        Map productFeaturesByTypeMap = FastMap.newInstance();
        try {
            Set typesWithOverflowMessages = new HashSet();
            EntityListIterator productFeatureEli = delegator.findListIteratorByCondition("ProductFeature", null, null, UtilMisc.toList("description"));
            GenericValue productFeature = null;
            while ((productFeature = (GenericValue) productFeatureEli.next()) != null) {
                String productFeatureTypeId = productFeature.getString("productFeatureTypeId");
                List featuresByType = (List) productFeaturesByTypeMap.get(productFeatureTypeId);
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
    
    public static Map makeFeatureIdByTypeMap(ServletRequest request) {
        Map parameters = UtilHttp.getParameterMap((HttpServletRequest) request);
        return makeFeatureIdByTypeMap(parameters);
    }
    
    /** Handles parameters coming in prefixed with "pft_" where the text in the key following the prefix is a productFeatureTypeId and the value is a productFeatureId; meant to be used with drop-downs and such */
    public static Map makeFeatureIdByTypeMap(Map parameters) {
        Map featureIdByType = FastMap.newInstance();
        if (parameters == null) return featureIdByType;
        
        Iterator parameterNameIter = parameters.keySet().iterator();
        while (parameterNameIter.hasNext()) {
            String parameterName = (String) parameterNameIter.next();
            if (parameterName.startsWith("pft_")) {
                String productFeatureTypeId = parameterName.substring(4);
                String productFeatureId = (String) parameters.get(parameterName);
                if (productFeatureId != null && productFeatureId.length() > 0) {
                    featureIdByType.put(productFeatureTypeId, productFeatureId);
                }
            }
        }
        
        return featureIdByType;
    }
    
    /** Handles parameters coming in prefixed with "SEARCH_FEAT" where the parameter value is a productFeatureId; meant to be used with text entry boxes or check-boxes and such */
    public static List makeFeatureIdListFromPrefixed(Map parameters) {
        List featureIdList = FastList.newInstance();
        if (parameters == null) return featureIdList;
        
        Iterator parameterNameIter = parameters.keySet().iterator();
        while (parameterNameIter.hasNext()) {
            String parameterName = (String) parameterNameIter.next();
            if (parameterName.startsWith("SEARCH_FEAT")) {
                String productFeatureId = (String) parameters.get(parameterName);
                if (productFeatureId != null && productFeatureId.length() > 0) {
                    featureIdList.add(productFeatureId);
                }
            }
        }
        
        return featureIdList;
    }
    
    public static String makeFeatureIdByTypeString(Map featureIdByType) {
        if (featureIdByType == null || featureIdByType.size() == 0) {
            return "";
        }
        
        StringBuffer outSb = new StringBuffer();
        Iterator fbtIter = featureIdByType.entrySet().iterator();
        while (fbtIter.hasNext()) {
            Map.Entry entry = (Map.Entry) fbtIter.next();
            String productFeatureTypeId = (String) entry.getKey();
            String productFeatureId = (String) entry.getValue();
            outSb.append(productFeatureTypeId);
            outSb.append('=');
            outSb.append(productFeatureId);
            if (fbtIter.hasNext()) {
                outSb.append('&');
            }
        }
        
        return outSb.toString();
    }
    
    /**
     *  Handles parameters coming in prefixed with "SEARCH_PROD_FEAT_CAT" 
     *  where the parameter value is a productFeatureCategoryId; 
     *  meant to be used with text entry boxes or check-boxes and such 
     **/
    public static List makeProductFeatureCategoryIdListFromPrefixed(Map parameters) {
        List prodFeatureCategoryIdList = FastList.newInstance();
        if (parameters == null) return prodFeatureCategoryIdList;
        
        Iterator parameterNameIter = parameters.keySet().iterator();
        while (parameterNameIter.hasNext()) {
            String parameterName = (String) parameterNameIter.next();
            if (parameterName.startsWith("SEARCH_PROD_FEAT_CAT")) {
                String productFeatureCategoryId = (String) parameters.get(parameterName);
                if (productFeatureCategoryId != null && productFeatureCategoryId.length() > 0) {
                   prodFeatureCategoryIdList.add(productFeatureCategoryId);
                }
            }
        }
        return prodFeatureCategoryIdList;
    }
}
