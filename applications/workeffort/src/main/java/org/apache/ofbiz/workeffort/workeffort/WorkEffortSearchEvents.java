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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.webapp.stats.VisitHandler;
import org.apache.ofbiz.workeffort.workeffort.WorkEffortSearch.ResultSortOrder;
import org.apache.ofbiz.workeffort.workeffort.WorkEffortSearch.WorkEffortSearchConstraint;
import org.apache.ofbiz.workeffort.workeffort.WorkEffortSearch.WorkEffortSearchContext;
import org.apache.ofbiz.workeffort.workeffort.WorkEffortSearchSession.WorkEffortSearchOptions;


public class WorkEffortSearchEvents {

    public static final String module = WorkEffortSearchEvents.class.getName();

    public static Map<String, Object> getWorkEffortSearchResult(HttpServletRequest request, Delegator delegator) {

        // ========== Create View Indexes
        int viewIndex = 0;
        int viewSize = 20;
        int highIndex = 0;
        int lowIndex = 0;
        int listSize = 0;

        HttpSession session = request.getSession();
        WorkEffortSearchOptions workEffortSearchOptions = WorkEffortSearchSession.getWorkEffortSearchOptions(session);

        Integer viewIndexInteger = workEffortSearchOptions.getViewIndex();
        if (viewIndexInteger != null) {
            viewIndex = viewIndexInteger;
        }
        Integer viewSizeInteger = workEffortSearchOptions.getViewSize();
        if (viewSizeInteger != null) {
            viewSize = viewSizeInteger;
        }

        lowIndex = viewIndex * viewSize;
        highIndex = (viewIndex + 1) * viewSize;

        // setup resultOffset and maxResults, noting that resultOffset is 1 based, not zero based as these numbers
        Integer resultOffset = lowIndex + 1;
        Integer maxResults = viewSize;

        // ========== Do the actual search
        ArrayList<String> workEffortIds = null;
        String visitId = VisitHandler.getVisitId(session);
        List<WorkEffortSearchConstraint> workEffortSearchConstraintList = WorkEffortSearchOptions.getConstraintList(session);
        // if no constraints, don't do a search...
        if (UtilValidate.isNotEmpty(workEffortSearchConstraintList)) {
            // if the search options have changed since the last search, put at the beginning of the options history list
            WorkEffortSearchSession.checkSaveSearchOptionsHistory(session);

            ResultSortOrder resultSortOrder = WorkEffortSearchOptions.getResultSortOrder(request);

            WorkEffortSearchContext workEffortSearchContext = new WorkEffortSearchContext(delegator, visitId);
            workEffortSearchContext.addWorkEffortSearchConstraints(workEffortSearchConstraintList);

            workEffortSearchContext.setResultSortOrder(resultSortOrder);
            workEffortSearchContext.setResultOffset(resultOffset);
            workEffortSearchContext.setMaxResults(maxResults);

            workEffortIds = workEffortSearchContext.doSearch();

            Integer totalResults = workEffortSearchContext.getTotalResults();
            if (totalResults != null) {
                listSize = totalResults;
            }
        }

        if (listSize < highIndex) {
            highIndex = listSize;
        }

        // ========== Setup other display info
        List<String> searchConstraintStrings = WorkEffortSearchSession.searchGetConstraintStrings(false, session, delegator);
        String searchSortOrderString = WorkEffortSearchSession.searchGetSortOrderString(false, request);

        // ========== populate the result Map
        Map<String, Object> result = new HashMap<>();

        result.put("workEffortIds", workEffortIds);
        result.put("viewIndex", viewIndex);
        result.put("viewSize", viewSize);
        result.put("listSize", listSize);
        result.put("lowIndex", lowIndex);
        result.put("highIndex", highIndex);
        result.put("searchConstraintStrings", searchConstraintStrings);
        result.put("searchSortOrderString", searchSortOrderString);

        return result;
    }


}
