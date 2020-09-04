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

import java.io.IOException;
import java.sql.Timestamp;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.common.KeywordSearchUtil;
import org.apache.ofbiz.content.data.DataResourceWorker;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.entity.util.EntityUtilProperties;

/**
 *  Does indexing in preparation for a keyword search.
 */
public class KeywordIndex {

    private static final String MODULE = KeywordIndex.class.getName();

    public static void forceIndexKeywords(GenericValue product) throws GenericEntityException {
        KeywordIndex.indexKeywords(product, true);
    }

    public static void indexKeywords(GenericValue product) throws GenericEntityException {
        KeywordIndex.indexKeywords(product, false);
    }

    public static void indexKeywords(GenericValue product, boolean doAll) throws GenericEntityException {
        if (product == null) {
            return;
        }
        Timestamp nowTimestamp = UtilDateTime.nowTimestamp();
        Delegator delegator = product.getDelegator();
        if (!doAll) {
            if ("N".equals(product.getString("autoCreateKeywords"))) {
                return;
            }
            if ("Y".equals(product.getString("isVariant")) && "true".equals(EntityUtilProperties.getPropertyValue("prodsearch",
                    "index.ignore.variants", delegator))) {
                return;
            }
            Timestamp salesDiscontinuationDate = product.getTimestamp("salesDiscontinuationDate");
            if (salesDiscontinuationDate != null && salesDiscontinuationDate.before(nowTimestamp)
                    && "true".equals(EntityUtilProperties.getPropertyValue("prodsearch", "index.ignore.discontinued.sales", delegator))) {
                return;
            }
        }

        if (delegator == null) {
            return;
        }
        String productId = product.getString("productId");

        // get these in advance just once since they will be used many times for the multiple strings to index
        String separators = KeywordSearchUtil.getSeparators();
        String stopWordBagOr = KeywordSearchUtil.getStopWordBagOr();
        String stopWordBagAnd = KeywordSearchUtil.getStopWordBagAnd();
        boolean removeStems = KeywordSearchUtil.getRemoveStems();
        Set<String> stemSet = KeywordSearchUtil.getStemSet();

        Map<String, Long> keywords = new TreeMap<>();
        List<String> strings = new LinkedList<>();

        int pidWeight = 1;
        try {
            pidWeight = EntityUtilProperties.getPropertyAsInteger("prodsearch", "index.weight.Product.productId", 0);
        } catch (Exception e) {
            Debug.logWarning("Could not parse weight number: " + e.toString(), MODULE);
        }
        keywords.put(product.getString("productId").toLowerCase(Locale.getDefault()), (long) pidWeight);

        // Product fields - default is 0 if not found in the properties file
        if (!"0".equals(EntityUtilProperties.getPropertyValue("prodsearch", "index.weight.Product.productName", "0", delegator))) {
            addWeightedKeywordSourceString(product, "productName", strings);
        }
        if (!"0".equals(EntityUtilProperties.getPropertyValue("prodsearch", "index.weight.Product.internalName", "0", delegator))) {
            addWeightedKeywordSourceString(product, "internalName", strings);
        }
        if (!"0".equals(EntityUtilProperties.getPropertyValue("prodsearch", "index.weight.Product.brandName", "0", delegator))) {
            addWeightedKeywordSourceString(product, "brandName", strings);
        }
        if (!"0".equals(EntityUtilProperties.getPropertyValue("prodsearch", "index.weight.Product.description", "0", delegator))) {
            addWeightedKeywordSourceString(product, "description", strings);
        }
        if (!"0".equals(EntityUtilProperties.getPropertyValue("prodsearch", "index.weight.Product.longDescription", "0", delegator))) {
            addWeightedKeywordSourceString(product, "longDescription", strings);
        }

        // ProductFeatureAppl
        if (!"0".equals(EntityUtilProperties.getPropertyValue("prodsearch", "index.weight.ProductFeatureAndAppl.description", "0", delegator))
                || !"0".equals(EntityUtilProperties.getPropertyValue("prodsearch", "index.weight.ProductFeatureAndAppl.abbrev", "0", delegator))
                || !"0".equals(EntityUtilProperties.getPropertyValue("prodsearch", "index.weight.ProductFeatureAndAppl.idCode", "0", delegator))) {
            // get strings from attributes and features
            List<GenericValue> productFeatureAndAppls = EntityQuery.use(delegator).from("ProductFeatureAndAppl")
                    .where("productId", productId).queryList();
            for (GenericValue productFeatureAndAppl: productFeatureAndAppls) {
                addWeightedKeywordSourceString(productFeatureAndAppl, "description", strings);
                addWeightedKeywordSourceString(productFeatureAndAppl, "abbrev", strings);
                addWeightedKeywordSourceString(productFeatureAndAppl, "idCode", strings);
            }
        }

        // ProductAttribute
        if (!"0".equals(EntityUtilProperties.getPropertyValue("prodsearch", "index.weight.ProductAttribute.attrName", "0", delegator))
                || !"0".equals(EntityUtilProperties.getPropertyValue("prodsearch", "index.weight.ProductAttribute.attrValue", "0", delegator))) {
            List<GenericValue> productAttributes = EntityQuery.use(delegator).from("ProductAttribute").where("productId", productId).queryList();
            for (GenericValue productAttribute: productAttributes) {
                addWeightedKeywordSourceString(productAttribute, "attrName", strings);
                addWeightedKeywordSourceString(productAttribute, "attrValue", strings);
            }
        }

        // GoodIdentification
        if (!"0".equals(EntityUtilProperties.getPropertyValue("prodsearch", "index.weight.GoodIdentification.idValue", "0", delegator))) {
            List<GenericValue> goodIdentifications = EntityQuery.use(delegator).from("GoodIdentification").where("productId", productId).queryList();
            for (GenericValue goodIdentification: goodIdentifications) {
                addWeightedKeywordSourceString(goodIdentification, "idValue", strings);
            }
        }

        // Variant Product IDs
        if ("Y".equals(product.getString("isVirtual"))) {
            if (!"0".equals(EntityUtilProperties.getPropertyValue("prodsearch", "index.weight.Variant.Product.productId", "0", delegator))) {
                List<GenericValue> variantProductAssocs = EntityQuery.use(delegator).from("ProductAssoc").where("productId", productId,
                        "productAssocTypeId", "PRODUCT_VARIANT").filterByDate().queryList();
                for (GenericValue variantProductAssoc: variantProductAssocs) {
                    int weight = 1;
                    try {
                        weight = EntityUtilProperties.getPropertyAsInteger("prodsearch", "index.weight.Variant.Product.productId", 0);
                    } catch (Exception e) {
                        Debug.logWarning("Could not parse weight number: " + e.toString(), MODULE);
                    }
                    for (int i = 0; i < weight; i++) {
                        strings.add(variantProductAssoc.getString("productIdTo"));
                    }
                }
            }
        }

        String productContentTypes = EntityUtilProperties.getPropertyValue("prodsearch", "index.include.ProductContentTypes", delegator);
        for (String productContentTypeId: productContentTypes.split(",")) {
            int weight = 1;
            try {
                // this is defaulting to a weight of 1 because you specified you wanted to index this type
                weight = EntityUtilProperties.getPropertyAsInteger("prodsearch", "index.weight.ProductContent." + productContentTypeId, 1);
            } catch (Exception e) {
                Debug.logWarning("Could not parse weight number: " + e.toString(), MODULE);
            }

            List<GenericValue> productContentAndInfos = EntityQuery.use(delegator).from("ProductContentAndInfo").where("productId", productId,
                    "productContentTypeId", productContentTypeId).queryList();
            for (GenericValue productContentAndInfo: productContentAndInfos) {
                addWeightedDataResourceString(productContentAndInfo, weight, strings, delegator, product);

                List<GenericValue> alternateViews = productContentAndInfo.getRelated("ContentAssocDataResourceViewTo",
                        UtilMisc.toMap("caContentAssocTypeId", "ALTERNATE_LOCALE"), UtilMisc.toList("-caFromDate"), false);
                alternateViews = EntityUtil.filterByDate(alternateViews, UtilDateTime.nowTimestamp(), "caFromDate", "caThruDate", true);
                for (GenericValue thisView: alternateViews) {
                    addWeightedDataResourceString(thisView, weight, strings, delegator, product);
                }
            }
        }
        if (UtilValidate.isNotEmpty(strings)) {
            for (String str: strings) {
                // call process keywords method here
                KeywordSearchUtil.processKeywordsForIndex(str, keywords, separators, stopWordBagAnd, stopWordBagOr, removeStems, stemSet);
            }
        }

        List<GenericValue> toBeStored = new LinkedList<>();
        int keywordMaxLength = EntityUtilProperties.getPropertyAsInteger("prodsearch", "product.keyword.max.length", 0);
        for (Map.Entry<String, Long> entry: keywords.entrySet()) {
            if (entry.getKey().length() <= keywordMaxLength) {
                GenericValue productKeyword = delegator.makeValue("ProductKeyword", UtilMisc.toMap("productId", product.getString("productId"),
                        "keyword", entry.getKey(), "keywordTypeId", "KWT_KEYWORD", "relevancyWeight", entry.getValue()));
                toBeStored.add(productKeyword);
            }
        }
        if (!toBeStored.isEmpty()) {
            if (Debug.verboseOn()) {
                Debug.logVerbose("[KeywordIndex.indexKeywords] Storing " + toBeStored.size() + " keywords for productId "
                        + product.getString("productId"), MODULE);
            }

            if ("true".equals(EntityUtilProperties.getPropertyValue("prodsearch", "index.delete.on_index", "false", delegator))) {
                // delete all keywords if the properties file says to
                delegator.removeByAnd("ProductKeyword", UtilMisc.toMap("productId", product.getString("productId")));
            }

            delegator.storeAll(toBeStored);
        }
    }

    public static void addWeightedDataResourceString(GenericValue drView, int weight, List<String> strings, Delegator delegator,
                                                     GenericValue product) {
        Map<String, Object> drContext = UtilMisc.<String, Object>toMap("product", product);
        try {
            String contentText = DataResourceWorker.renderDataResourceAsText(null, delegator, drView.getString("dataResourceId"), drContext,
                    null, null, false);
            for (int i = 0; i < weight; i++) {
                strings.add(contentText);
            }
        } catch (GeneralException | IOException e1) {
            Debug.logError(e1, "Error getting content text to index", MODULE);
        }
    }

    public static void addWeightedKeywordSourceString(GenericValue value, String fieldName, List<String> strings) {
        if (value.getString(fieldName) != null) {
            int weight = 1;

            try {
                weight = EntityUtilProperties.getPropertyAsInteger("prodsearch", "index.weight." + value.getEntityName() + "." + fieldName, 1);
            } catch (Exception e) {
                Debug.logWarning("Could not parse weight number: " + e.toString(), MODULE);
            }

            for (int i = 0; i < weight; i++) {
                strings.add(value.getString(fieldName));
            }
        }
    }
}
