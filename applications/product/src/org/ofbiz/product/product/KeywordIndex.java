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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javolution.util.FastList;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.common.KeywordSearchUtil;
import org.ofbiz.content.data.DataResourceWorker;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.util.EntityUtil;

/**
 *  Does indexing in preparation for a keyword search.
 */
public class KeywordIndex {

    public static final String module = KeywordIndex.class.getName();

    public static void forceIndexKeywords(GenericValue product) throws GenericEntityException {
        KeywordIndex.indexKeywords(product, true);
    }

    public static void indexKeywords(GenericValue product) throws GenericEntityException {
        KeywordIndex.indexKeywords(product, false);
    }

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

        Delegator delegator = product.getDelegator();
        if (delegator == null) return;
        String productId = product.getString("productId");

        // get these in advance just once since they will be used many times for the multiple strings to index
        String separators = KeywordSearchUtil.getSeparators();
        String stopWordBagOr = KeywordSearchUtil.getStopWordBagOr();
        String stopWordBagAnd = KeywordSearchUtil.getStopWordBagAnd();
        boolean removeStems = KeywordSearchUtil.getRemoveStems();
        Set<String> stemSet = KeywordSearchUtil.getStemSet();

        Map<String, Long> keywords = new TreeMap<String, Long>();
        List<String> strings = FastList.newInstance();

        int pidWeight = 1;
        try {
            pidWeight = Integer.parseInt(UtilProperties.getPropertyValue("prodsearch", "index.weight.Product.productId", "0"));
        } catch (Exception e) {
            Debug.logWarning("Could not parse weight number: " + e.toString(), module);
        }
        keywords.put(product.getString("productId").toLowerCase(), Long.valueOf(pidWeight));

        // Product fields - default is 0 if not found in the properties file
        if (!"0".equals(UtilProperties.getPropertyValue("prodsearch", "index.weight.Product.productName", "0"))) {
            addWeightedKeywordSourceString(product, "productName", strings);
        }
        if (!"0".equals(UtilProperties.getPropertyValue("prodsearch", "index.weight.Product.internalName", "0"))) {
            addWeightedKeywordSourceString(product, "internalName", strings);
        }
        if (!"0".equals(UtilProperties.getPropertyValue("prodsearch", "index.weight.Product.brandName", "0"))) {
            addWeightedKeywordSourceString(product, "brandName", strings);
        }
        if (!"0".equals(UtilProperties.getPropertyValue("prodsearch", "index.weight.Product.description", "0"))) {
            addWeightedKeywordSourceString(product, "description", strings);
        }
        if (!"0".equals(UtilProperties.getPropertyValue("prodsearch", "index.weight.Product.longDescription", "0"))) {
            addWeightedKeywordSourceString(product, "longDescription", strings);
        }

        // ProductFeatureAppl
        if (!"0".equals(UtilProperties.getPropertyValue("prodsearch", "index.weight.ProductFeatureAndAppl.description", "0")) ||
            !"0".equals(UtilProperties.getPropertyValue("prodsearch", "index.weight.ProductFeatureAndAppl.abbrev", "0")) ||
            !"0".equals(UtilProperties.getPropertyValue("prodsearch", "index.weight.ProductFeatureAndAppl.idCode", "0"))) {
            // get strings from attributes and features
            List<GenericValue> productFeatureAndAppls = delegator.findByAnd("ProductFeatureAndAppl", UtilMisc.toMap("productId", productId));
            for (GenericValue productFeatureAndAppl: productFeatureAndAppls) {
                addWeightedKeywordSourceString(productFeatureAndAppl, "description", strings);
                addWeightedKeywordSourceString(productFeatureAndAppl, "abbrev", strings);
                addWeightedKeywordSourceString(productFeatureAndAppl, "idCode", strings);
            }
        }

        // ProductAttribute
        if (!"0".equals(UtilProperties.getPropertyValue("prodsearch", "index.weight.ProductAttribute.attrName", "0")) ||
                !"0".equals(UtilProperties.getPropertyValue("prodsearch", "index.weight.ProductAttribute.attrValue", "0"))) {
            List<GenericValue> productAttributes = delegator.findByAnd("ProductAttribute", UtilMisc.toMap("productId", productId));
            for (GenericValue productAttribute: productAttributes) {
                addWeightedKeywordSourceString(productAttribute, "attrName", strings);
                addWeightedKeywordSourceString(productAttribute, "attrValue", strings);
            }
        }

        // GoodIdentification
        if (!"0".equals(UtilProperties.getPropertyValue("prodsearch", "index.weight.GoodIdentification.idValue", "0"))) {
            List<GenericValue> goodIdentifications = delegator.findByAnd("GoodIdentification", UtilMisc.toMap("productId", productId));
            for (GenericValue goodIdentification: goodIdentifications) {
                addWeightedKeywordSourceString(goodIdentification, "idValue", strings);
            }
        }

        // Variant Product IDs
        if ("Y".equals(product.getString("isVirtual"))) {
            if (!"0".equals(UtilProperties.getPropertyValue("prodsearch", "index.weight.Variant.Product.productId", "0"))) {
                List<GenericValue> variantProductAssocs = delegator.findByAnd("ProductAssoc", UtilMisc.toMap("productId", productId, "productAssocTypeId", "PRODUCT_VARIANT"));
                variantProductAssocs = EntityUtil.filterByDate(variantProductAssocs);
                for (GenericValue variantProductAssoc: variantProductAssocs) {
                    int weight = 1;
                    try {
                        weight = Integer.parseInt(UtilProperties.getPropertyValue("prodsearch", "index.weight.Variant.Product.productId", "0"));
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
        for (String productContentTypeId: productContentTypes.split(",")) {
            int weight = 1;
            try {
                // this is defaulting to a weight of 1 because you specified you wanted to index this type
                weight = Integer.parseInt(UtilProperties.getPropertyValue("prodsearch", "index.weight.ProductContent." + productContentTypeId, "1"));
            } catch (Exception e) {
                Debug.logWarning("Could not parse weight number: " + e.toString(), module);
            }

            List<GenericValue> productContentAndInfos = delegator.findByAnd("ProductContentAndInfo", UtilMisc.toMap("productId", productId, "productContentTypeId", productContentTypeId), null);
            for (GenericValue productContentAndInfo: productContentAndInfos) {
                addWeightedDataResourceString(productContentAndInfo, weight, strings, delegator, product);

                List<GenericValue> alternateViews = productContentAndInfo.getRelated("ContentAssocDataResourceViewTo", UtilMisc.toMap("caContentAssocTypeId", "ALTERNATE_LOCALE"), UtilMisc.toList("-caFromDate"));
                alternateViews = EntityUtil.filterByDate(alternateViews, UtilDateTime.nowTimestamp(), "caFromDate", "caThruDate", true);
                for (GenericValue thisView: alternateViews) {
                    addWeightedDataResourceString(thisView, weight, strings, delegator, product);
                }
            }
        }

        for (String str: strings) {
            // call process keywords method here
            KeywordSearchUtil.processKeywordsForIndex(str, keywords, separators, stopWordBagAnd, stopWordBagOr, removeStems, stemSet);
        }

        List<GenericValue> toBeStored = FastList.newInstance();
        for (Map.Entry<String, Long> entry: keywords.entrySet()) {
            GenericValue productKeyword = delegator.makeValue("ProductKeyword", UtilMisc.toMap("productId", product.getString("productId"), "keyword", entry.getKey(), "relevancyWeight", entry.getValue()));
            toBeStored.add(productKeyword);
        }
        if (toBeStored.size() > 0) {
            if (Debug.verboseOn()) Debug.logVerbose("[KeywordIndex.indexKeywords] Storing " + toBeStored.size() + " keywords for productId " + product.getString("productId"), module);

            if ("true".equals(UtilProperties.getPropertyValue("prodsearch", "index.delete.on_index", "false"))) {
                // delete all keywords if the properties file says to
                delegator.removeByAnd("ProductKeyword", UtilMisc.toMap("productId", product.getString("productId")));
            }

            delegator.storeAll(toBeStored);
        }
    }

    public static void addWeightedDataResourceString(GenericValue drView, int weight, List<String> strings, Delegator delegator, GenericValue product) {
        Map<String, Object> drContext = UtilMisc.<String, Object>toMap("product", product);
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

    public static void addWeightedKeywordSourceString(GenericValue value, String fieldName, List<String> strings) {
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
