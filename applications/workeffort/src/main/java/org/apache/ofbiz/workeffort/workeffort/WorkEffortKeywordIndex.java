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

package org.apache.ofbiz.workeffort.workeffort;

import java.io.IOException;
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
import org.apache.ofbiz.common.KeywordSearchUtil;
import org.apache.ofbiz.content.data.DataResourceWorker;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.entity.util.EntityUtilProperties;

public class WorkEffortKeywordIndex {
    private static final String MODULE = WorkEffortKeywordIndex.class.getName();
    public static void indexKeywords(GenericValue workEffort) throws GenericEntityException {
        if (workEffort == null) {
            return;
        }

        Delegator delegator = workEffort.getDelegator();
        if (delegator == null) {
            return;
        }
        String workEffortId = workEffort.getString("workEffortId");
        String separators = KeywordSearchUtil.getSeparators();
        String stopWordBagOr = KeywordSearchUtil.getStopWordBagOr();
        String stopWordBagAnd = KeywordSearchUtil.getStopWordBagAnd();
        boolean removeStems = KeywordSearchUtil.getRemoveStems();
        Set<String> stemSet = KeywordSearchUtil.getStemSet();

        Map<String, Long> keywords = new TreeMap<>();
        List<String> strings = new LinkedList<>();
        int widWeight = 1;
        try {
            widWeight = EntityUtilProperties.getPropertyAsInteger("workeffort", "index.weight.WorkEffort.workEffortId", 1);
        } catch (Exception e) {
            Debug.logWarning("Could not parse weight number: " + e.toString(), MODULE);
        }
        keywords.put(workEffort.getString("workEffortId").toLowerCase(Locale.getDefault()), (long) widWeight);

        addWeightedKeywordSourceString(workEffort, "workEffortName", strings);
        addWeightedKeywordSourceString(workEffort, "workEffortTypeId", strings);
        addWeightedKeywordSourceString(workEffort, "currentStatusId", strings);

        if (!"0".equals(EntityUtilProperties.getPropertyValue("workeffort", "index.weight.WorkEffortNoteAndData.noteInfo", "1", delegator))) {
            List<GenericValue> workEffortNotes = EntityQuery.use(delegator).from("WorkEffortNoteAndData")
                    .where("workEffortId", workEffortId).queryList();
            for (GenericValue workEffortNote : workEffortNotes) {
                addWeightedKeywordSourceString(workEffortNote, "noteInfo", strings);
            }
        }
        //WorkEffortAttribute
        if (!"0".equals(EntityUtilProperties.getPropertyValue("workeffort", "index.weight.WorkEffortAttribute.attrName", "1", delegator))
                || !"0".equals(EntityUtilProperties.getPropertyValue("workeffort", "index.weight.WorkEffortAttribute.attrValue", "1", delegator))) {
            List<GenericValue> workEffortAttributes = EntityQuery.use(delegator).from("WorkEffortAttribute")
                    .where("workEffortId", workEffortId).queryList();
            for (GenericValue workEffortAttribute : workEffortAttributes) {
                addWeightedKeywordSourceString(workEffortAttribute, "attrName", strings);
                addWeightedKeywordSourceString(workEffortAttribute, "attrValue", strings);
            }
        }

        String workEffortContentTypes = EntityUtilProperties.getPropertyValue("workeffort", "index.include.WorkEffortContentTypes", delegator);
        for (String workEffortContentTypeId: workEffortContentTypes.split(",")) {
            int weight = 1;
            try {
                weight = EntityUtilProperties.getPropertyAsInteger("workeffort", "index.weight.WorkEffortContent." + workEffortContentTypeId, 1);
            } catch (Exception e) {
                Debug.logWarning("Could not parse weight number: " + e.toString(), MODULE);
            }

            List<GenericValue> workEffortContentAndInfos = EntityQuery.use(delegator).from("WorkEffortContentAndInfo").where("workEffortId",
                    workEffortId, "workEffortContentTypeId", workEffortContentTypeId).queryList();
            for (GenericValue workEffortContentAndInfo: workEffortContentAndInfos) {
                addWeightedDataResourceString(workEffortContentAndInfo, weight, strings, delegator, workEffort);
                List<GenericValue> alternateViews = workEffortContentAndInfo.getRelated("ContentAssocDataResourceViewTo",
                        UtilMisc.toMap("caContentAssocTypeId", "ALTERNATE_LOCALE"), UtilMisc.toList("-caFromDate"), false);
                alternateViews = EntityUtil.filterByDate(alternateViews, UtilDateTime.nowTimestamp(), "caFromDate", "caThruDate", true);
                for (GenericValue thisView: alternateViews) {
                    addWeightedDataResourceString(thisView, weight, strings, delegator, workEffort);
                }
            }
        }
        for (String str: strings) {
            // call process keywords method here
            KeywordSearchUtil.processKeywordsForIndex(str, keywords, separators, stopWordBagAnd, stopWordBagOr, removeStems, stemSet);
        }

        List<GenericValue> toBeStored = new LinkedList<>();
        for (Map.Entry<String, Long> entry: keywords.entrySet()) {
            if (entry.getKey().length() < 60) { // ignore very long strings, cannot be stored anyway
                GenericValue workEffortKeyword = delegator.makeValue("WorkEffortKeyword", UtilMisc.toMap("workEffortId",
                        workEffort.getString("workEffortId"), "keyword", entry.getKey(), "relevancyWeight", entry.getValue()));
                toBeStored.add(workEffortKeyword);
            }
        }
        if (!toBeStored.isEmpty()) {
            if (Debug.verboseOn()) {
                Debug.logVerbose("WorkEffortKeywordIndex indexKeywords Storing " + toBeStored.size() + " keywords for workEffortId "
                        + workEffort.getString("workEffortId"), MODULE);
            }
            delegator.storeAll(toBeStored);
        }

    }

    public static void addWeightedDataResourceString(GenericValue dataResource, int weight, List<String> strings, Delegator delegator,
                                                     GenericValue workEffort) {
        Map<String, Object> workEffortCtx = UtilMisc.<String, Object>toMap("workEffort", workEffort);
        try {
            String contentText = DataResourceWorker.renderDataResourceAsText(null, delegator, dataResource.getString("dataResourceId"),
                    workEffortCtx, null, null, false);
            for (int i = 0; i < weight; i++) {
                strings.add(contentText);
            }
        } catch (IOException | GeneralException e) {
            Debug.logError(e, "Error getting content text to index", MODULE);
        }
    }
    public static void addWeightedKeywordSourceString(GenericValue value, String fieldName, List<String> strings) {
        if (value.getString(fieldName) != null) {
            int weight = 1;

            try {
                weight = EntityUtilProperties.getPropertyAsInteger("workeffort", "index.weight." + value.getEntityName() + "." + fieldName, 1);
            } catch (Exception e) {
                Debug.logWarning("Could not parse weight number: " + e.toString(), MODULE);
            }

            for (int i = 0; i < weight; i++) {
                strings.add(value.getString(fieldName));
            }
        }
    }

}
