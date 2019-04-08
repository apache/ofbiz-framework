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

import java.sql.Timestamp;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilHttp;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.content.content.ContentSearch.ResultSortOrder;
import org.apache.ofbiz.content.content.ContentSearch.SortKeywordRelevancy;
import org.apache.ofbiz.content.content.ContentSearch.ContentSearchConstraint;

public class ContentSearchSession {

    public static final String module = ContentSearchSession.class.getName();
    
    @SuppressWarnings("serial")
    public static class ContentSearchOptions implements java.io.Serializable {
        protected List<ContentSearchConstraint> constraintList = null;
        protected ResultSortOrder resultSortOrder = null;
        protected Integer viewIndex = null;
        protected Integer viewSize = null;
        protected boolean changed = false;
        public ContentSearchOptions() { }

        /** Basic copy constructor */
        public ContentSearchOptions(ContentSearchOptions contentSearchOptions) {
            this.constraintList = UtilMisc.makeListWritable(contentSearchOptions.constraintList);
            this.resultSortOrder = contentSearchOptions.resultSortOrder;
            this.viewIndex = contentSearchOptions.viewIndex;
            this.viewSize = contentSearchOptions.viewSize;
            this.changed = contentSearchOptions.changed;
        }

        public List<ContentSearchConstraint> getConstraintList() {
            return this.constraintList;
        }
        public static List<ContentSearchConstraint> getConstraintList(HttpSession session) {
            return getContentSearchOptions(session).constraintList;
        }
        public static void addConstraint(ContentSearchConstraint contentSearchConstraint, HttpSession session) {
            ContentSearchOptions contentSearchOptions = getContentSearchOptions(session);
            if (contentSearchOptions.constraintList == null) {
                contentSearchOptions.constraintList = new LinkedList<ContentSearch.ContentSearchConstraint>();
            }
            if (!contentSearchOptions.constraintList.contains(contentSearchConstraint)) {
                contentSearchOptions.constraintList.add(contentSearchConstraint);
                contentSearchOptions.changed = true;
            }
        }

        public ResultSortOrder getResultSortOrder() {
            if (this.resultSortOrder == null) {
                this.resultSortOrder = new SortKeywordRelevancy();
                this.changed = true;
            }
            return this.resultSortOrder;
        }
        public static ResultSortOrder getResultSortOrder(HttpServletRequest request) {
            ContentSearchOptions contentSearchOptions = getContentSearchOptions(request.getSession());
            return contentSearchOptions.getResultSortOrder();
        }
        public static void setResultSortOrder(ResultSortOrder resultSortOrder, HttpSession session) {
            ContentSearchOptions contentSearchOptions = getContentSearchOptions(session);
            contentSearchOptions.resultSortOrder = resultSortOrder;
            contentSearchOptions.changed = true;
        }

        public static void clearSearchOptions(HttpSession session) {
            ContentSearchOptions contentSearchOptions = getContentSearchOptions(session);
            contentSearchOptions.constraintList = null;
            contentSearchOptions.resultSortOrder = null;
        }

        public void clearViewInfo() {
            this.viewIndex = null;
            this.viewSize = null;
        }

        /**
         * @return Returns the viewIndex.
         */
        public Integer getViewIndex() {
            return viewIndex;
        }
        /**
         * @param viewIndex The viewIndex to set.
         */
        public void setViewIndex(Integer viewIndex) {
            this.viewIndex = viewIndex;
        }
        /**
         * @return Returns the viewSize.
         */
        public Integer getViewSize() {
            return viewSize;
        }
        /**
         * @param viewSize The viewSize to set.
         */
        public void setViewSize(Integer viewSize) {
            this.viewSize = viewSize;
        }

        public List<String> searchGetConstraintStrings(boolean detailed, Delegator delegator, Locale locale) {
            List<ContentSearchConstraint> contentSearchConstraintList = this.getConstraintList();
            List<String> constraintStrings = new LinkedList<String>();
            if (contentSearchConstraintList == null) {
                return constraintStrings;
            }
            for (ContentSearchConstraint contentSearchConstraint: contentSearchConstraintList) {
                if (contentSearchConstraint == null) continue;
                String constraintString = contentSearchConstraint.prettyPrintConstraint(delegator, detailed, locale);
                if (UtilValidate.isNotEmpty(constraintString)) {
                    constraintStrings.add(constraintString);
                } else {
                    constraintStrings.add("Description not available");
                }
            }
            return constraintStrings;
        }
    }

    public static ContentSearchOptions getContentSearchOptions(HttpSession session) {
        ContentSearchOptions contentSearchOptions = (ContentSearchOptions) session.getAttribute("_CONTENT_SEARCH_OPTIONS_CURRENT_");
        if (contentSearchOptions == null) {
            contentSearchOptions = new ContentSearchOptions();
            session.setAttribute("_CONTENT_SEARCH_OPTIONS_CURRENT_", contentSearchOptions);
        }
        return contentSearchOptions;
    }

    public static void processSearchParameters(Map<String, Object> parameters, HttpServletRequest request) {
        Boolean alreadyRun = (Boolean) request.getAttribute("processSearchParametersAlreadyRun");
        if (Boolean.TRUE.equals(alreadyRun)) {
            return;
        } else {
            request.setAttribute("processSearchParametersAlreadyRun", Boolean.TRUE);
        }
        HttpSession session = request.getSession();
        boolean constraintsChanged = false;

        // clear search? by default yes, but if the clearSearch parameter is N then don't
        String clearSearchString = (String) parameters.get("clearSearch");
        if (!"N".equals(clearSearchString)) {
            searchClear(session);
            constraintsChanged = true;
        } else {
            String removeConstraint = (String) parameters.get("removeConstraint");
            if (UtilValidate.isNotEmpty(removeConstraint)) {
                try {
                    searchRemoveConstraint(Integer.parseInt(removeConstraint), session);
                    constraintsChanged = true;
                } catch (Exception e) {
                    Debug.logError(e, "Error removing constraint [" + removeConstraint + "]", module);
                }
            }
        }

        // add a Content Assoc Type to the search
        if (UtilValidate.isNotEmpty(parameters.get("SEARCH_CONTENT_ID"))) {
            String contentId=(String) parameters.get("SEARCH_CONTENT_ID");
            String contentAssocTypeId=(String) parameters.get("contentAssocTypeId");
            boolean includeAllSubContents =!"N".equalsIgnoreCase((String) parameters.get("SEARCH_SUB_CONTENTS"));
            searchAddConstraint(new ContentSearch.ContentAssocConstraint(contentId,contentAssocTypeId,includeAllSubContents), session);
            constraintsChanged = true;
        }

        // add a Content fromDate thruDate to the search
        if (UtilValidate.isNotEmpty(parameters.get("fromDate")) || UtilValidate.isNotEmpty(parameters.get("thruDate"))) {
            Timestamp fromDate =null;
            if (UtilValidate.isNotEmpty(parameters.get("fromDate"))) {
                fromDate=Timestamp.valueOf((String) parameters.get("fromDate"));
            }

            Timestamp thruDate = null;
            if (UtilValidate.isNotEmpty(parameters.get("thruDate"))) {
                thruDate = Timestamp.valueOf((String) parameters.get("thruDate"));
            }
            searchAddConstraint(new ContentSearch.LastUpdatedRangeConstraint(fromDate,thruDate), session);
            constraintsChanged = true;
        }

        // if keywords were specified, add a constraint for them
        if (UtilValidate.isNotEmpty(parameters.get("SEARCH_STRING"))) {
            String keywordString = (String) parameters.get("SEARCH_STRING");
            String searchOperator = (String) parameters.get("SEARCH_OPERATOR");
            // defaults to true/Y, ie anything but N is true/Y
            boolean anyPrefixSuffix = !"N".equals(parameters.get("SEARCH_ANYPRESUF"));
            searchAddConstraint(new ContentSearch.KeywordConstraint(keywordString, anyPrefixSuffix, anyPrefixSuffix, null, "AND".equals(searchOperator)), session);
            constraintsChanged = true;
        }
        // set the sort order
        String sortOrder = (String) parameters.get("sortOrder");
        String sortAscending = (String) parameters.get("sortAscending");
        boolean ascending = !"N".equals(sortAscending);
        if (sortOrder != null) {
            if (sortOrder.equals("SortKeywordRelevancy")) {
                searchSetSortOrder(new ContentSearch.SortKeywordRelevancy(), session);
            } else if (sortOrder.startsWith("SortContentField:")) {
                String fieldName = sortOrder.substring("SortContentField:".length());
                searchSetSortOrder(new ContentSearch.SortContentField(fieldName, ascending), session);
            }
        }

        ContentSearchOptions contentSearchOptions = getContentSearchOptions(session);
        if (constraintsChanged) {
            // query changed, clear out the VIEW_INDEX & VIEW_SIZE
            contentSearchOptions.clearViewInfo();
        }

        String viewIndexStr = (String) parameters.get("VIEW_INDEX");
        if (UtilValidate.isNotEmpty(viewIndexStr)) {
            try {
                contentSearchOptions.setViewIndex(Integer.valueOf(viewIndexStr));
            } catch (Exception e) {
                Debug.logError(e, "Error formatting VIEW_INDEX, setting to 0", module);
                // we could just do nothing here, but we know something was specified so we don't want to use the previous value from the session
                contentSearchOptions.setViewIndex(Integer.valueOf(0));
            }
        }

        String viewSizeStr = (String) parameters.get("VIEW_SIZE");
        if (UtilValidate.isNotEmpty(viewSizeStr)) {
            try {
                contentSearchOptions.setViewSize(Integer.valueOf(viewSizeStr));
            } catch (Exception e) {
                Debug.logError(e, "Error formatting VIEW_SIZE, setting to 20", module);
                contentSearchOptions.setViewSize(Integer.valueOf(20));
            }
        }
    }

    public static void searchAddConstraint(ContentSearchConstraint contentSearchConstraint, HttpSession session) {
        ContentSearchOptions.addConstraint(contentSearchConstraint, session);
    }
    public static void searchSetSortOrder(ResultSortOrder resultSortOrder, HttpSession session) {
        ContentSearchOptions.setResultSortOrder(resultSortOrder, session);
    }
    public static List<ContentSearchOptions> getSearchOptionsHistoryList(HttpSession session) {
        List<ContentSearchOptions> optionsHistoryList = UtilGenerics.checkList(session.getAttribute("_CONTENT_SEARCH_OPTIONS_HISTORY_"));
        if (optionsHistoryList == null) {
            optionsHistoryList = new LinkedList<ContentSearchSession.ContentSearchOptions>();
            session.setAttribute("_CONTENT_SEARCH_OPTIONS_HISTORY_", optionsHistoryList);
        }
        return optionsHistoryList;
    }

    public static List<String> searchGetConstraintStrings(boolean detailed, HttpSession session, Delegator delegator) {
        Locale locale = UtilHttp.getLocale(session);
        ContentSearchOptions contentSearchOptions = getContentSearchOptions(session);
        return contentSearchOptions.searchGetConstraintStrings(detailed, delegator, locale);
    }
    public static String searchGetSortOrderString(boolean detailed, HttpServletRequest request) {
        Locale locale = UtilHttp.getLocale(request);
        ResultSortOrder resultSortOrder = ContentSearchOptions.getResultSortOrder(request);
        if (resultSortOrder == null) return "";
        return resultSortOrder.prettyPrintSortOrder(detailed, locale);
    }
    public static void checkSaveSearchOptionsHistory(HttpSession session) {
        ContentSearchOptions contentSearchOptions = ContentSearchSession.getContentSearchOptions(session);
        // if the options have changed since the last search, add it to the beginning of the search options history
        if (contentSearchOptions.changed) {
            List<ContentSearchOptions> optionsHistoryList = ContentSearchSession.getSearchOptionsHistoryList(session);
            optionsHistoryList.add(0, new ContentSearchOptions(contentSearchOptions));
            contentSearchOptions.changed = false;
        }
    }
    public static void searchRemoveConstraint(int index, HttpSession session) {
        List<ContentSearchConstraint> contentSearchConstraintList = ContentSearchOptions.getConstraintList(session);
        if (contentSearchConstraintList == null) {
            return;
        } else if (index >= contentSearchConstraintList.size()) {
            return;
        } else {
            contentSearchConstraintList.remove(index);
        }
    }
    public static void searchClear(HttpSession session) {
        ContentSearchOptions.clearSearchOptions(session);
    }
}
