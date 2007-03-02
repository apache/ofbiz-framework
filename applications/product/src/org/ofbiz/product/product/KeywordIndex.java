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

import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.common.KeywordSearchUtil;
import org.ofbiz.content.data.DataResourceWorker;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.util.EntityUtil;

/**
 *  Does indexing in preparation for a keyword search.
 */
public class KeywordIndex {
    
    public static final String module = KeywordIndex.class.getName();

    public static void indexKeywords(GenericValue product, boolean doAll) throws GenericEntityException {
        if (product == null) return;
        Timestamp nowTimestamp = UtilDateTime.nowTimestamp();
        
        if (!doAll) {
            if ("N".equals(product.getString("autoCreateKeywords"))) {
                return;
            }
            if ("Y".equals(product.getString("isVariant")) && "true".equals(UtilProperties.getPropertyValue("prodsearch", "index.ignore.variants"))) {
                return;
            }
            Timestamp salesDiscontinuationDate = product.getTimestamp("salesDiscontinuationDate");
            if (salesDiscontinuationDate != null && salesDiscontinuationDate.before(nowTimestamp) && 
                    "true".equals(UtilProperties.getPropertyValue("prodsearch", "index.ignore.discontinued.sales"))) {
                return;
            }
        }
        
        GenericDelegator delegator = product.getDelegator();
        if (delegator == null) return;
        String productId = product.getString("productId");

        // get these in advance just once since they will be used many times for the multiple strings to index
        String separators = KeywordSearchUtil.getSeparators();
        String stopWordBagOr = KeywordSearchUtil.getStopWordBagOr();
        String stopWordBagAnd = KeywordSearchUtil.getStopWordBagAnd();
        boolean removeStems = KeywordSearchUtil.getRemoveStems();
        Set stemSet = KeywordSearchUtil.getStemSet();
        
        Map keywords = new TreeMap();
        List strings = new ArrayList(50);

        int pidWeight = 1;
        try {
            pidWeight = Integer.parseInt(UtilProperties.getPropertyValue("prodsearch", "index.weight.Product.productId", "1"));
        } catch (Exception e) {
            Debug.logWarning("Could not parse weight number: " + e.toString(), module);
        }
        keywords.put(product.getString("productId").toLowerCase(), new Long(pidWeight));

        addWeightedKeywordSourceString(product, "productName", strings);
        addWeightedKeywordSourceString(product, "internalName", strings);
        addWeightedKeywordSourceString(product, "brandName", strings);
        addWeightedKeywordSourceString(product, "description", strings);
        addWeightedKeywordSourceString(product, "longDescription", strings);

        if (!"0".equals(UtilProperties.getPropertyValue("prodsearch", "index.weight.ProductFeatureAndAppl.description", "1")) ||
            !"0".equals(UtilProperties.getPropertyValue("prodsearch", "index.weight.ProductFeatureAndAppl.abbrev", "1")) ||
            !"0".equals(UtilProperties.getPropertyValue("prodsearch", "index.weight.ProductFeatureAndAppl.idCode", "1"))) {
            // get strings from attributes and features
            Iterator productFeatureAndAppls = UtilMisc.toIterator(delegator.findByAnd("ProductFeatureAndAppl", UtilMisc.toMap("productId", productId)));
            while (productFeatureAndAppls != null && productFeatureAndAppls.hasNext()) {
                GenericValue productFeatureAndAppl = (GenericValue) productFeatureAndAppls.next();
                addWeightedKeywordSourceString(productFeatureAndAppl, "description", strings);
                addWeightedKeywordSourceString(productFeatureAndAppl, "abbrev", strings);
                addWeightedKeywordSourceString(productFeatureAndAppl, "idCode", strings);
            }
        }

        // ProductAttribute
        if (!"0".equals(UtilProperties.getPropertyValue("prodsearch", "index.weight.ProductAttribute.attrName", "1")) ||
                !"0".equals(UtilProperties.getPropertyValue("prodsearch", "index.weight.ProductAttribute.attrValue", "1"))) {
            Iterator productAttributes = UtilMisc.toIterator(delegator.findByAnd("ProductAttribute", UtilMisc.toMap("productId", productId)));
            while (productAttributes != null && productAttributes.hasNext()) {
                GenericValue productAttribute = (GenericValue) productAttributes.next();
                addWeightedKeywordSourceString(productAttribute, "attrName", strings);
                addWeightedKeywordSourceString(productAttribute, "attrValue", strings);
            }
        }

        // GoodIdentification
        if (!"0".equals(UtilProperties.getPropertyValue("prodsearch", "index.weight.GoodIdentification.idValue", "1"))) {
            Iterator goodIdentifications = UtilMisc.toIterator(delegator.findByAnd("GoodIdentification", UtilMisc.toMap("productId", productId)));
            while (goodIdentifications != null && goodIdentifications.hasNext()) {
                GenericValue goodIdentification = (GenericValue) goodIdentifications.next();
                addWeightedKeywordSourceString(goodIdentification, "idValue", strings);
            }
        }
        
        // Variant Product IDs
        if ("Y".equals(product.getString("isVirtual"))) {
            if (!"0".equals(UtilProperties.getPropertyValue("prodsearch", "index.weight.Variant.Product.productId", "1"))) {
                Iterator variantProductAssocs = UtilMisc.toIterator(delegator.findByAnd("ProductAssoc", UtilMisc.toMap("productId", productId, "productAssocTypeId", "PRODUCT_VARIANT")));
                while (variantProductAssocs != null && variantProductAssocs.hasNext()) {
                    GenericValue variantProductAssoc = (GenericValue) variantProductAssocs.next();
                    int weight = 1;
                    try {
                        weight = Integer.parseInt(UtilProperties.getPropertyValue("prodsearch", "index.weight.Variant.Product.productId", "1"));
                    } catch (Exception e) {
                        Debug.logWarning("Could not parse weight number: " + e.toString(), module);
                    }
                    for (int i = 0; i < weight; i++) {
                        strings.add(variantProductAssoc.getString("productIdTo"));
                    }
                }
            }
        }
        
        String productContentTypes = UtilProperties.getPropertyValue("prodsearch", "index.include.ProductContentTypes");
        List productContentTypeList = Arrays.asList(productContentTypes.split(","));
        Iterator productContentTypeIter = productContentTypeList.iterator();
        while (productContentTypeIter.hasNext()) {
            String productContentTypeId = (String) productContentTypeIter.next();

            int weight = 1;
            try {
                weight = Integer.parseInt(UtilProperties.getPropertyValue("prodsearch", "index.weight.ProductContent." + productContentTypeId, "1"));
            } catch (Exception e) {
                Debug.logWarning("Could not parse weight number: " + e.toString(), module);
            }
            
            List productContentAndInfos = delegator.findByAnd("ProductContentAndInfo", UtilMisc.toMap("productId", productId, "productContentTypeId", productContentTypeId), null);
            Iterator productContentAndInfoIter = productContentAndInfos.iterator();
            while (productContentAndInfoIter.hasNext()) {
                GenericValue productContentAndInfo = (GenericValue) productContentAndInfoIter.next();
                addWeightedDataResourceString(productContentAndInfo, weight, strings, delegator, product);
                
                List alternateViews = productContentAndInfo.getRelated("ContentAssocDataResourceViewTo", UtilMisc.toMap("caContentAssocTypeId", "ALTERNATE_LOCALE"), UtilMisc.toList("-caFromDate"));
                alternateViews = EntityUtil.filterByDate(alternateViews, UtilDateTime.nowTimestamp(), "caFromDate", "caThruDate", true);
                Iterator alternateViewIter = alternateViews.iterator();
                while (alternateViewIter.hasNext()) {
                    GenericValue thisView = (GenericValue) alternateViewIter.next();
                    addWeightedDataResourceString(thisView, weight, strings, delegator, product);
                }
            }
        }
        
        Iterator strIter = strings.iterator();
        while (strIter.hasNext()) {
            String str = (String) strIter.next();
            // call process keywords method here
            KeywordSearchUtil.processKeywordsForIndex(str, keywords, separators, stopWordBagAnd, stopWordBagOr, removeStems, stemSet);
        }

        List toBeStored = new LinkedList();
        Iterator kiter = keywords.entrySet().iterator();
        while (kiter.hasNext()) {
            Map.Entry entry = (Map.Entry) kiter.next();
            GenericValue productKeyword = delegator.makeValue("ProductKeyword", UtilMisc.toMap("productId", product.getString("productId"), "keyword", entry.getKey(), "relevancyWeight", entry.getValue()));
            toBeStored.add(productKeyword);
        }
        if (toBeStored.size() > 0) {
            if (Debug.verboseOn()) Debug.logVerbose("[KeywordSearch.induceKeywords] Storing " + toBeStored.size() + " keywords for productId " + product.getString("productId"), module);
            delegator.storeAll(toBeStored);
        }
    }
    
    public static void addWeightedDataResourceString(GenericValue drView, int weight, List strings, GenericDelegator delegator, GenericValue product) {
        Map drContext = UtilMisc.toMap("product", product);
        try {
            String contentText = DataResourceWorker.renderDataResourceAsText(delegator, drView.getString("dataResourceId"), drContext, null, null, false);
            for (int i = 0; i < weight; i++) {
                strings.add(contentText);
            }
        } catch (IOException e1) {
            Debug.logError(e1, "Error getting content text to index", module);
        } catch (GeneralException e1) {
            Debug.logError(e1, "Error getting content text to index", module);
        }
    }

    public static void addWeightedKeywordSourceString(GenericValue value, String fieldName, List strings) {
        if (value.getString(fieldName) != null) {
            int weight = 1;

            try {
                weight = Integer.parseInt(UtilProperties.getPropertyValue("prodsearch", "index.weight." + value.getEntityName() + "." + fieldName, "1"));
            } catch (Exception e) {
                Debug.logWarning("Could not parse weight number: " + e.toString(), module);
            }

            for (int i = 0; i < weight; i++) {
                strings.add(value.getString(fieldName));
            }
        }
    }
}
