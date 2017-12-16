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
import org.apache.ofbiz.workeffort.workeffort.WorkEffortSearch.ResultSortOrder;
import org.apache.ofbiz.workeffort.workeffort.WorkEffortSearch.SortKeywordRelevancy;
import org.apache.ofbiz.workeffort.workeffort.WorkEffortSearch.WorkEffortSearchConstraint;

public class WorkEffortSearchSession {

    public static final String module = WorkEffortSearchSession.class.getName();

    @SuppressWarnings("serial")
    public static class WorkEffortSearchOptions implements java.io.Serializable {
        protected List<WorkEffortSearchConstraint> constraintList = null;
        protected ResultSortOrder resultSortOrder = null;
        protected Integer viewIndex = null;
        protected Integer viewSize = null;
        protected boolean changed = false;
        public WorkEffortSearchOptions() { }

        /** Basic copy constructor */
        public WorkEffortSearchOptions(WorkEffortSearchOptions workEffortSearchOptions) {
            this.constraintList = UtilMisc.makeListWritable(workEffortSearchOptions.constraintList);
            this.resultSortOrder = workEffortSearchOptions.resultSortOrder;
            this.viewIndex = workEffortSearchOptions.viewIndex;
            this.viewSize = workEffortSearchOptions.viewSize;
            this.changed = workEffortSearchOptions.changed;
        }

        public List<WorkEffortSearchConstraint> getConstraintList() {
            return this.constraintList;
        }
        public static List<WorkEffortSearchConstraint> getConstraintList(HttpSession session) {
            return getWorkEffortSearchOptions(session).constraintList;
        }
        public static void addConstraint(WorkEffortSearchConstraint workEffortSearchConstraint, HttpSession session) {
            WorkEffortSearchOptions workEffortSearchOptions = getWorkEffortSearchOptions(session);
            if (workEffortSearchOptions.constraintList == null) {
                workEffortSearchOptions.constraintList = new LinkedList<>();
            }
            if (!workEffortSearchOptions.constraintList.contains(workEffortSearchConstraint)) {
                workEffortSearchOptions.constraintList.add(workEffortSearchConstraint);
                workEffortSearchOptions.changed = true;
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
            WorkEffortSearchOptions workEffortSearchOptions = getWorkEffortSearchOptions(request.getSession());
            return workEffortSearchOptions.getResultSortOrder();
        }
        public static void setResultSortOrder(ResultSortOrder resultSortOrder, HttpSession session) {
            WorkEffortSearchOptions workEffortSearchOptions = getWorkEffortSearchOptions(session);
            workEffortSearchOptions.resultSortOrder = resultSortOrder;
            workEffortSearchOptions.changed = true;
        }

        public static void clearSearchOptions(HttpSession session) {
            WorkEffortSearchOptions workEffortSearchOptions = getWorkEffortSearchOptions(session);
            workEffortSearchOptions.constraintList = null;
            workEffortSearchOptions.resultSortOrder = null;
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
            List<WorkEffortSearchConstraint> workEffortSearchConstraintList = this.getConstraintList();
            List<String> constraintStrings = new LinkedList<>();
            if (workEffortSearchConstraintList == null) {
                return constraintStrings;
            }
            for (WorkEffortSearchConstraint workEffortSearchConstraint: workEffortSearchConstraintList) {
                if (workEffortSearchConstraint == null) {
                    continue;
                }
                String constraintString = workEffortSearchConstraint.prettyPrintConstraint(delegator, detailed, locale);
                if (UtilValidate.isNotEmpty(constraintString)) {
                    constraintStrings.add(constraintString);
                } else {
                    constraintStrings.add("Description not available");
                }
            }
            return constraintStrings;
        }
    }

    public static WorkEffortSearchOptions getWorkEffortSearchOptions(HttpSession session) {
        WorkEffortSearchOptions workEffortSearchOptions = (WorkEffortSearchOptions) session.getAttribute("_WORK_EFFORT_SEARCH_OPTIONS_CURRENT_");
        if (workEffortSearchOptions == null) {
            workEffortSearchOptions = new WorkEffortSearchOptions();
            session.setAttribute("_WORK_EFFORT_SEARCH_OPTIONS_CURRENT_", workEffortSearchOptions);
        }
        return workEffortSearchOptions;
    }

    public static void processSearchParameters(Map<String, Object> parameters, HttpServletRequest request) {
        Boolean alreadyRun = (Boolean) request.getAttribute("processSearchParametersAlreadyRun");
        if (Boolean.TRUE.equals(alreadyRun)) {
            return;
        }
        request.setAttribute("processSearchParametersAlreadyRun", Boolean.TRUE);
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

//      add a Work Effort Review to the search
        if (UtilValidate.isNotEmpty(parameters.get("SEARCH_STRING_REVIEW_TEXT"))) {
            String reviewText = (String) parameters.get("SEARCH_STRING_REVIEW_TEXT");
            searchAddConstraint(new WorkEffortSearch.WorkEffortReviewConstraint(reviewText), session);
            constraintsChanged = true;
        }
//      add a Work Effort Assoc Type to the search
        if (UtilValidate.isNotEmpty(parameters.get("SEARCH_WORK_EFFORT_ID"))) {
            String workEffortId=(String) parameters.get("SEARCH_WORK_EFFORT_ID");
            String workEffortAssocTypeId=(String) parameters.get("workEffortAssocTypeId");
            boolean includeAllSubWorkEfforts =!"N".equalsIgnoreCase((String) parameters.get("SEARCH_SUB_WORK_EFFORTS"));
            searchAddConstraint(new WorkEffortSearch.WorkEffortAssocConstraint(workEffortId,workEffortAssocTypeId,includeAllSubWorkEfforts), session);
            constraintsChanged = true;
        }
//      add a Work Effort Party Assignment to the search
        if (UtilValidate.isNotEmpty(parameters.get("partyId"))) {
            String partyId=(String) parameters.get("partyId");
            String roleTypeId=(String) parameters.get("roleTypeId");
            searchAddConstraint(new WorkEffortSearch.PartyAssignmentConstraint(partyId,roleTypeId), session);
            constraintsChanged = true;
        }

//      add a Product Set to the search
        if (UtilValidate.isNotEmpty(parameters.get("productId_1"))) {
            List<String> productSet = new LinkedList<>();
            productSet.add((String) parameters.get("productId_1"));
            if (UtilValidate.isNotEmpty(parameters.get("productId_2"))) {
                productSet.add((String) parameters.get("productId_2"));
            }
            searchAddConstraint(new WorkEffortSearch.ProductSetConstraint(productSet), session);
            constraintsChanged = true;
        }

//      add a WorkEfort fromDate thruDate  to the search
        if (UtilValidate.isNotEmpty(parameters.get("fromDate")) || UtilValidate.isNotEmpty(parameters.get("thruDate"))) {
            Timestamp fromDate =null;
            if (UtilValidate.isNotEmpty(parameters.get("fromDate"))) {
                fromDate=Timestamp.valueOf((String) parameters.get("fromDate"));
            }

            Timestamp thruDate = null;
            if (UtilValidate.isNotEmpty(parameters.get("thruDate"))) {
                thruDate = Timestamp.valueOf((String) parameters.get("thruDate"));
            }
            searchAddConstraint(new WorkEffortSearch.LastUpdatedRangeConstraint(fromDate,thruDate), session);
            constraintsChanged = true;
        }

        // if keywords were specified, add a constraint for them
        if (UtilValidate.isNotEmpty(parameters.get("SEARCH_STRING"))) {
            String keywordString = (String) parameters.get("SEARCH_STRING");
            String searchOperator = (String) parameters.get("SEARCH_OPERATOR");
            // defaults to true/Y, ie anything but N is true/Y
            boolean anyPrefixSuffix = !"N".equals(parameters.get("SEARCH_ANYPRESUF"));
            searchAddConstraint(new WorkEffortSearch.KeywordConstraint(keywordString, anyPrefixSuffix, anyPrefixSuffix, null, "AND".equals(searchOperator)), session);
            constraintsChanged = true;
        }
        // set the sort order
        String sortOrder = (String) parameters.get("sortOrder");
        String sortAscending = (String) parameters.get("sortAscending");
        boolean ascending = !"N".equals(sortAscending);
        if (sortOrder != null) {
            if ("SortKeywordRelevancy".equals(sortOrder)) {
                searchSetSortOrder(new WorkEffortSearch.SortKeywordRelevancy(), session);
            } else if (sortOrder.startsWith("SortWorkEffortField:")) {
                String fieldName = sortOrder.substring("SortWorkEffortField:".length());
                searchSetSortOrder(new WorkEffortSearch.SortWorkEffortField(fieldName, ascending), session);
            }
        }

        WorkEffortSearchOptions workEffortSearchOptions = getWorkEffortSearchOptions(session);
        if (constraintsChanged) {
            // query changed, clear out the VIEW_INDEX & VIEW_SIZE
            workEffortSearchOptions.clearViewInfo();
        }

        String viewIndexStr = (String) parameters.get("VIEW_INDEX");
        if (UtilValidate.isNotEmpty(viewIndexStr)) {
            try {
                workEffortSearchOptions.setViewIndex(Integer.valueOf(viewIndexStr));
            } catch (Exception e) {
                Debug.logError(e, "Error formatting VIEW_INDEX, setting to 0", module);
                // we could just do nothing here, but we know something was specified so we don't want to use the previous value from the session
                workEffortSearchOptions.setViewIndex(Integer.valueOf(0));
            }
        }

        String viewSizeStr = (String) parameters.get("VIEW_SIZE");
        if (UtilValidate.isNotEmpty(viewSizeStr)) {
            try {
                workEffortSearchOptions.setViewSize(Integer.valueOf(viewSizeStr));
            } catch (Exception e) {
                Debug.logError(e, "Error formatting VIEW_SIZE, setting to 20", module);
                workEffortSearchOptions.setViewSize(Integer.valueOf(20));
            }
        }
    }

    public static void searchAddConstraint(WorkEffortSearchConstraint workEffortSearchConstraint, HttpSession session) {
        WorkEffortSearchOptions.addConstraint(workEffortSearchConstraint, session);
    }
    public static void searchSetSortOrder(ResultSortOrder resultSortOrder, HttpSession session) {
        WorkEffortSearchOptions.setResultSortOrder(resultSortOrder, session);
    }
    public static List<WorkEffortSearchOptions> getSearchOptionsHistoryList(HttpSession session) {
        List<WorkEffortSearchOptions> optionsHistoryList = UtilGenerics.checkList(session.getAttribute("_WORK_EFFORT_SEARCH_OPTIONS_HISTORY_"));
        if (optionsHistoryList == null) {
            optionsHistoryList = new LinkedList<>();
            session.setAttribute("_WORK_EFFORT_SEARCH_OPTIONS_HISTORY_", optionsHistoryList);
        }
        return optionsHistoryList;
    }

    public static List<String> searchGetConstraintStrings(boolean detailed, HttpSession session, Delegator delegator) {
        Locale locale = UtilHttp.getLocale(session);
        WorkEffortSearchOptions workEffortSearchOptions = getWorkEffortSearchOptions(session);
        return workEffortSearchOptions.searchGetConstraintStrings(detailed, delegator, locale);
    }
    public static String searchGetSortOrderString(boolean detailed, HttpServletRequest request) {
        Locale locale = UtilHttp.getLocale(request);
        ResultSortOrder resultSortOrder = WorkEffortSearchOptions.getResultSortOrder(request);
        return resultSortOrder.prettyPrintSortOrder(detailed, locale);
    }
    public static void checkSaveSearchOptionsHistory(HttpSession session) {
        WorkEffortSearchOptions workEffortSearchOptions = WorkEffortSearchSession.getWorkEffortSearchOptions(session);
        // if the options have changed since the last search, add it to the beginning of the search options history
        if (workEffortSearchOptions.changed) {
            List<WorkEffortSearchOptions> optionsHistoryList = WorkEffortSearchSession.getSearchOptionsHistoryList(session);
            optionsHistoryList.add(0, new WorkEffortSearchOptions(workEffortSearchOptions));
            workEffortSearchOptions.changed = false;
        }
    }
    public static void searchRemoveConstraint(int index, HttpSession session) {
        List<WorkEffortSearchConstraint> workEffortSearchConstraintList = WorkEffortSearchOptions.getConstraintList(session);
        if (workEffortSearchConstraintList == null) {
            return;
        } else if (index >= workEffortSearchConstraintList.size()) {
            return;
        } else {
            workEffortSearchConstraintList.remove(index);
        }
    }
    public static void searchClear(HttpSession session) {
        WorkEffortSearchOptions.clearSearchOptions(session);
    }
}
