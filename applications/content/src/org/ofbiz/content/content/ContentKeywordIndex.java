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
package org.ofbiz.content.content;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javolution.util.FastList;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.common.KeywordSearchUtil;
import org.ofbiz.content.data.DataResourceWorker;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;

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
        if (delegator == null) return;
        String contentId = content.getString("contentId");

        // get these in advance just once since they will be used many times for the multiple strings to index
        String separators = KeywordSearchUtil.getSeparators();
        String stopWordBagOr = KeywordSearchUtil.getStopWordBagOr();
        String stopWordBagAnd = KeywordSearchUtil.getStopWordBagAnd();
        boolean removeStems = KeywordSearchUtil.getRemoveStems();
        Set<String> stemSet = KeywordSearchUtil.getStemSet();

        Map<String, Long> keywords = new TreeMap<String, Long>();
        List<String> strings = FastList.newInstance();

        int pidWeight = 1;
        keywords.put(content.getString("contentId").toLowerCase(), Long.valueOf(pidWeight));

        addWeightedKeywordSourceString(content, "dataResourceId", strings);
        addWeightedKeywordSourceString(content, "contentName", strings);
        addWeightedKeywordSourceString(content, "description", strings);

        // ContentAttribute
        List<GenericValue> contentAttributes = delegator.findByAnd("ContentAttribute", UtilMisc.toMap("contentId", contentId), null, false);
        for (GenericValue contentAttribute: contentAttributes) {
            addWeightedKeywordSourceString(contentAttribute, "attrName", strings);
            addWeightedKeywordSourceString(contentAttribute, "attrValue", strings);
        }

        // ContentMetaData
        List<GenericValue> contentMetaDatas = delegator.findByAnd("ContentMetaData", UtilMisc.toMap("contentId", contentId), null, false);
        for (GenericValue contentMetaData: contentMetaDatas) {
            addWeightedKeywordSourceString(contentMetaData, "metaDataValue", strings);
        }

        // ContentRole
        List<GenericValue> contentRoles = delegator.findByAnd("ContentRole", UtilMisc.toMap("contentId", contentId), null, false);
        for (GenericValue contentRole: contentRoles) {
            GenericValue party = delegator.findOne("PartyNameView", UtilMisc.toMap("partyId", contentRole.getString("partyId")), false);
            if (party != null) {
                addWeightedKeywordSourceString(party, "description", strings);
                addWeightedKeywordSourceString(party, "firstName", strings);
                addWeightedKeywordSourceString(party, "middleName", strings);
                addWeightedKeywordSourceString(party, "lastName", strings);
                addWeightedKeywordSourceString(party, "groupName", strings);
            }
        }

        // DataResourceRole
        List<GenericValue> dataResourceRoles = delegator.findByAnd("DataResourceRole", UtilMisc.toMap("dataResourceId", content.getString("dataResourceId")), null, false);
        for (GenericValue dataResourceRole: dataResourceRoles) {
            GenericValue party = delegator.findOne("PartyNameView", UtilMisc.toMap("partyId", dataResourceRole.getString("partyId")), false);
            if (party != null) {
                addWeightedKeywordSourceString(party, "description", strings);
                addWeightedKeywordSourceString(party, "firstName", strings);
                addWeightedKeywordSourceString(party, "middleName", strings);
                addWeightedKeywordSourceString(party, "lastName", strings);
                addWeightedKeywordSourceString(party, "groupName", strings);
            }
        }

        // Product
        List<GenericValue> productContentList = delegator.findByAnd("ProductContent", UtilMisc.toMap("contentId", contentId), null, false);
        for (GenericValue productContent: productContentList) {
            GenericValue product = delegator.findOne("Product", UtilMisc.toMap("productId", productContent.getString("productId")), false);
            if (product != null) {
                addWeightedKeywordSourceString(product, "productName", strings);
                addWeightedKeywordSourceString(product, "internalName", strings);
                addWeightedKeywordSourceString(product, "brandName", strings);
                addWeightedKeywordSourceString(product, "description", strings);
                addWeightedKeywordSourceString(product, "longDescription", strings);
            }
        }

        // ProductCategory
        List<GenericValue> productCategoryContentList = delegator.findByAnd("ProductCategoryContent", UtilMisc.toMap("contentId", contentId), null, false);
        for (GenericValue productCategoryContent: productCategoryContentList) {
            GenericValue productCategory = delegator.findOne("ProductCategory", UtilMisc.toMap("productCategoryId", productCategoryContent.getString("productCategoryId")), false);
            if (productCategory != null) {
                addWeightedKeywordSourceString(productCategory, "categoryName", strings);
                addWeightedKeywordSourceString(productCategory, "description", strings);
                addWeightedKeywordSourceString(productCategory, "longDescription", strings);
            }
        }

        // PartyContent
        List<GenericValue> partyContents = delegator.findByAnd("PartyContent", UtilMisc.toMap("contentId", contentId), null, false);
        for (GenericValue partyContent: partyContents) {
            GenericValue party = delegator.findOne("PartyNameView", UtilMisc.toMap("partyId", partyContent.getString("partyId")), false);
            if (party != null) {
                addWeightedKeywordSourceString(party, "description", strings);
                addWeightedKeywordSourceString(party, "firstName", strings);
                addWeightedKeywordSourceString(party, "middleName", strings);
                addWeightedKeywordSourceString(party, "lastName", strings);
                addWeightedKeywordSourceString(party, "groupName", strings);
            }
        }

        // WebSiteContent
        List<GenericValue> webSiteContents = delegator.findByAnd("WebSiteContent", UtilMisc.toMap("contentId", contentId), null, false);
        for (GenericValue webSiteContent: webSiteContents) {
            GenericValue webSite = delegator.findOne("WebSite", UtilMisc.toMap("webSiteId", webSiteContent.getString("webSiteId")), false);
            if (webSite != null) {
                addWeightedKeywordSourceString(webSite, "siteName", strings);
                addWeightedKeywordSourceString(webSite, "httpHost", strings);
                addWeightedKeywordSourceString(webSite, "httpsHost", strings);
            }
        }

        // WorkEffortContent
        List<GenericValue> workEffortContents = delegator.findByAnd("WorkEffortContent", UtilMisc.toMap("contentId", contentId), null, false);
        for (GenericValue workEffortContent: workEffortContents) {
            GenericValue workEffort = delegator.findOne("WorkEffort", UtilMisc.toMap("workEffortId", workEffortContent.getString("workEffortId")), false);
            if (workEffort != null) {
                addWeightedKeywordSourceString(workEffort, "workEffortName", strings);
            }
        }

        // DataResource
        GenericValue dataResource = delegator.findOne("DataResource", UtilMisc.toMap("dataResourceId", content.getString("dataResourceId")), false);
        if (dataResource != null) {
            addWeightedKeywordSourceString(dataResource, "dataResourceName", strings);
            addWeightedKeywordSourceString(dataResource, "objectInfo", strings);
        }
        /*List<GenericValue> contentDataResourceViews = delegator.findByAnd("ContentDataResourceView", UtilMisc.toMap("contentId", contentId), null, false);
        for (GenericValue contentDataResourceView: contentDataResourceViews) {
            int weight = 1;
            addWeightedDataResourceString(contentDataResourceView, weight, strings, delegator, content);

            List<GenericValue> alternateViews = contentDataResourceView.getRelated("ContentAssocDataResourceViewTo", UtilMisc.toMap("caContentAssocTypeId", "ALTERNATE_LOCALE"), UtilMisc.toList("-caFromDate"), false);
            alternateViews = EntityUtil.filterByDate(alternateViews, UtilDateTime.nowTimestamp(), "caFromDate", "caThruDate", true);
            for (GenericValue thisView: alternateViews) {
                addWeightedDataResourceString(thisView, weight, strings, delegator, content);
            }
        }*/

        if (UtilValidate.isNotEmpty(strings)) {
            for (String str: strings) {
                // call process keywords method here
                KeywordSearchUtil.processKeywordsForIndex(str, keywords, separators, stopWordBagAnd, stopWordBagOr, removeStems, stemSet);
            }
        }

        List<GenericValue> toBeStored = FastList.newInstance();
        int keywordMaxLength = Integer.parseInt(UtilProperties.getPropertyValue("contentsearch", "content.keyword.max.length"));
        for (Map.Entry<String, Long> entry: keywords.entrySet()) {
            if (entry.getKey().length() <= keywordMaxLength) {
                GenericValue contentKeyword = delegator.makeValue("ContentKeyword", UtilMisc.toMap("contentId", content.getString("contentId"), "keyword", entry.getKey(), "relevancyWeight", entry.getValue()));
                toBeStored.add(contentKeyword);
            }
        }
        if (toBeStored.size() > 0) {
            if (Debug.verboseOn()) Debug.logVerbose("[ContentKeywordIndex.indexKeywords] Storing " + toBeStored.size() + " keywords for contentId " + content.getString("contentId"), module);

            if ("true".equals(UtilProperties.getPropertyValue("contentsearch", "index.delete.on_index", "false"))) {
                // delete all keywords if the properties file says to
                delegator.removeByAnd("ContentKeyword", UtilMisc.toMap("contentId", content.getString("contentId")));
            }

            delegator.storeAll(toBeStored);
        }
    }

    public static void addWeightedDataResourceString(GenericValue drView, int weight, List<String> strings, Delegator delegator, GenericValue content) {
        Map<String, Object> drContext = UtilMisc.<String, Object>toMap("content", content);
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
            for (int i = 0; i < weight; i++) {
                strings.add(value.getString(fieldName));
            }
        }
    }
}
