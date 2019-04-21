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
package org.apache.ofbiz.content.content;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.common.KeywordSearchUtil;
import org.apache.ofbiz.content.data.DataResourceWorker;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtilProperties;

/**
 *  Does indexing in preparation for a keyword search.
 */
public class ContentKeywordIndex {

    public static final String module = ContentKeywordIndex.class.getName();

    public static void forceIndexKeywords(GenericValue content) throws GenericEntityException {
        ContentKeywordIndex.indexKeywords(content, true);
    }

    public static void indexKeywords(GenericValue content) throws GenericEntityException {
        ContentKeywordIndex.indexKeywords(content, false);
    }

    public static void indexKeywords(GenericValue content, boolean doAll) throws GenericEntityException {
        if (content == null) return;
        
        Delegator delegator = content.getDelegator();
        String contentId = content.getString("contentId");

        // get these in advance just once since they will be used many times for the multiple strings to index
        String separators = KeywordSearchUtil.getSeparators();
        String stopWordBagOr = KeywordSearchUtil.getStopWordBagOr();
        String stopWordBagAnd = KeywordSearchUtil.getStopWordBagAnd();
        boolean removeStems = KeywordSearchUtil.getRemoveStems();
        Set<String> stemSet = KeywordSearchUtil.getStemSet();

        Map<String, Long> keywords = new TreeMap<>();
        List<String> strings = new LinkedList<>();

        int pidWeight = 1;
        keywords.put(content.getString("contentId").toLowerCase(Locale.getDefault()), (long) pidWeight);

        addWeightedKeywordSourceString(content, "dataResourceId", strings);
        addWeightedKeywordSourceString(content, "contentName", strings);
        addWeightedKeywordSourceString(content, "description", strings);

        // ContentAttribute
        List<GenericValue> contentAttributes = EntityQuery.use(delegator).from("ContentAttribute").where("contentId", contentId).queryList();
        for (GenericValue contentAttribute: contentAttributes) {
            addWeightedKeywordSourceString(contentAttribute, "attrName", strings);
            addWeightedKeywordSourceString(contentAttribute, "attrValue", strings);
        }

        // ContentMetaData
        List<GenericValue> contentMetaDatas = EntityQuery.use(delegator).from("ContentMetaData").where("contentId", contentId).queryList();
        for (GenericValue contentMetaData: contentMetaDatas) {
            addWeightedKeywordSourceString(contentMetaData, "metaDataValue", strings);
        }

        // ContentRole
        List<GenericValue> contentRoles = EntityQuery.use(delegator).from("ContentRole").where("contentId", contentId).queryList();
        for (GenericValue contentRole: contentRoles) {
            GenericValue party = EntityQuery.use(delegator).from("PartyNameView").where("partyId", contentRole.get("partyId")).queryOne();
            if (party != null) {
                addWeightedKeywordSourceString(party, "description", strings);
                addWeightedKeywordSourceString(party, "firstName", strings);
                addWeightedKeywordSourceString(party, "middleName", strings);
                addWeightedKeywordSourceString(party, "lastName", strings);
                addWeightedKeywordSourceString(party, "groupName", strings);
            }
        }

        // DataResourceRole
        List<GenericValue> dataResourceRoles = EntityQuery.use(delegator).from("DataResourceRole").where("dataResourceId", content.get("dataResourceId")).queryList();
        for (GenericValue dataResourceRole: dataResourceRoles) {
            GenericValue party = EntityQuery.use(delegator).from("PartyNameView").where("partyId", dataResourceRole.get("partyId")).queryOne();
            if (party != null) {
                addWeightedKeywordSourceString(party, "description", strings);
                addWeightedKeywordSourceString(party, "firstName", strings);
                addWeightedKeywordSourceString(party, "middleName", strings);
                addWeightedKeywordSourceString(party, "lastName", strings);
                addWeightedKeywordSourceString(party, "groupName", strings);
            }
        }

        // Product
        List<GenericValue> productContentList = EntityQuery.use(delegator).from("ProductContent").where("contentId", contentId).queryList();
        for (GenericValue productContent: productContentList) {
            GenericValue product = EntityQuery.use(delegator).from("Product").where("productId", productContent.get("productId")).queryOne();
            if (product != null) {
                addWeightedKeywordSourceString(product, "productName", strings);
                addWeightedKeywordSourceString(product, "internalName", strings);
                addWeightedKeywordSourceString(product, "brandName", strings);
                addWeightedKeywordSourceString(product, "description", strings);
                addWeightedKeywordSourceString(product, "longDescription", strings);
            }
        }

        // ProductCategory
        List<GenericValue> productCategoryContentList = EntityQuery.use(delegator).from("ProductCategoryContent").where("contentId", contentId).queryList();
        for (GenericValue productCategoryContent: productCategoryContentList) {
            GenericValue productCategory = EntityQuery.use(delegator).from("ProductCategory").where("productCategoryId", productCategoryContent.getString("productCategoryId")).queryOne();
            if (productCategory != null) {
                addWeightedKeywordSourceString(productCategory, "categoryName", strings);
                addWeightedKeywordSourceString(productCategory, "description", strings);
                addWeightedKeywordSourceString(productCategory, "longDescription", strings);
            }
        }

        // PartyContent
        List<GenericValue> partyContents = EntityQuery.use(delegator).from("PartyContent").where("contentId", contentId).queryList();
        for (GenericValue partyContent: partyContents) {
            GenericValue party = EntityQuery.use(delegator).from("PartyNameView").where("partyId", partyContent.get("partyId")).queryOne();
            if (party != null) {
                addWeightedKeywordSourceString(party, "description", strings);
                addWeightedKeywordSourceString(party, "firstName", strings);
                addWeightedKeywordSourceString(party, "middleName", strings);
                addWeightedKeywordSourceString(party, "lastName", strings);
                addWeightedKeywordSourceString(party, "groupName", strings);
            }
        }

        // WebSiteContent
        List<GenericValue> webSiteContents = EntityQuery.use(delegator).from("WebSiteContent").where("contentId", contentId).queryList();
        for (GenericValue webSiteContent: webSiteContents) {
            GenericValue webSite = EntityQuery.use(delegator).from("WebSite").where("webSiteId", webSiteContent.get("webSiteId")).queryOne();
            if (webSite != null) {
                addWeightedKeywordSourceString(webSite, "siteName", strings);
                addWeightedKeywordSourceString(webSite, "httpHost", strings);
                addWeightedKeywordSourceString(webSite, "httpsHost", strings);
            }
        }

        // WorkEffortContent
        List<GenericValue> workEffortContents = EntityQuery.use(delegator).from("WorkEffortContent").where("contentId", contentId).queryList();
        for (GenericValue workEffortContent: workEffortContents) {
            GenericValue workEffort = EntityQuery.use(delegator).from("WorkEffort").where("workEffortId", workEffortContent.get("workEffortId")).queryOne();
            if (workEffort != null) {
                addWeightedKeywordSourceString(workEffort, "workEffortName", strings);
            }
        }

        // DataResource
        GenericValue dataResource = EntityQuery.use(delegator).from("DataResource").where("dataResourceId", content.get("dataResourceId")).queryOne();
        if (dataResource != null) {
            addWeightedKeywordSourceString(dataResource, "dataResourceName", strings);
            addWeightedKeywordSourceString(dataResource, "objectInfo", strings);
        }

        if (UtilValidate.isNotEmpty(strings)) {
            for (String str: strings) {
                // call process keywords method here
                KeywordSearchUtil.processKeywordsForIndex(str, keywords, separators, stopWordBagAnd, stopWordBagOr, removeStems, stemSet);
            }
        }

        List<GenericValue> toBeStored = new LinkedList<>();
        int keywordMaxLength = EntityUtilProperties.getPropertyAsInteger("contentsearch", "content.keyword.max.length", 0);
        for (Map.Entry<String, Long> entry: keywords.entrySet()) {
            if (entry.getKey().length() <= keywordMaxLength) {
                GenericValue contentKeyword = delegator.makeValue("ContentKeyword", UtilMisc.toMap("contentId", content.getString("contentId"), "keyword", entry.getKey(), "relevancyWeight", entry.getValue()));
                toBeStored.add(contentKeyword);
            }
        }
        if (toBeStored.size() > 0) {
            if (Debug.verboseOn()) Debug.logVerbose("[ContentKeywordIndex.indexKeywords] Storing " + toBeStored.size() + " keywords for contentId " + content.getString("contentId"), module);

            if ("true".equals(EntityUtilProperties.getPropertyValue("contentsearch", "index.delete.on_index", "false", delegator))) {
                // delete all keywords if the properties file says to
                delegator.removeByAnd("ContentKeyword", UtilMisc.toMap("contentId", content.getString("contentId")));
            }

            delegator.storeAll(toBeStored);
        }
    }

    public static void addWeightedDataResourceString(GenericValue drView, int weight, List<String> strings, Delegator delegator, GenericValue content) {
        Map<String, Object> drContext = UtilMisc.<String, Object>toMap("content", content);
        try {
            String contentText = DataResourceWorker.renderDataResourceAsText(null, delegator, drView.getString("dataResourceId"), drContext, null, null, false);
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
            for (int i = 0; i < weight; i++) {
                strings.add(value.getString(fieldName));
            }
        }
    }
}
