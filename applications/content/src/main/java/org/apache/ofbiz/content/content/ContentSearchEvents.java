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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.content.content.ContentSearch.ContentSearchConstraint;
import org.apache.ofbiz.content.content.ContentSearch.ContentSearchContext;
import org.apache.ofbiz.content.content.ContentSearch.ResultSortOrder;
import org.apache.ofbiz.content.content.ContentSearchSession.ContentSearchOptions;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.webapp.stats.VisitHandler;


public class ContentSearchEvents {

    private static final String MODULE = ContentSearchEvents.class.getName();

    public static Map<String, Object> getContentSearchResult(HttpServletRequest request, Delegator delegator) {

        // ========== Create View Indexes
        int viewIndex = 0;
        int viewSize = 20;
        int highIndex = 0;
        int lowIndex = 0;
        int listSize = 0;

        HttpSession session = request.getSession();
        ContentSearchOptions contentSearchOptions = ContentSearchSession.getContentSearchOptions(session);

        Integer viewIndexInteger = contentSearchOptions.getViewIndex();
        if (viewIndexInteger != null) viewIndex = viewIndexInteger;
        Integer viewSizeInteger = contentSearchOptions.getViewSize();
        if (viewSizeInteger != null) viewSize = viewSizeInteger;

        lowIndex = viewIndex * viewSize;
        highIndex = (viewIndex + 1) * viewSize;

        // setup resultOffset and maxResults, noting that resultOffset is 1 based, not zero based as these numbers
        Integer resultOffset = lowIndex + 1;
        Integer maxResults = viewSize;

        // ========== Do the actual search
        ArrayList<String> contentIds = null;
        String visitId = VisitHandler.getVisitId(session);
        List<ContentSearchConstraint> contentSearchConstraintList = ContentSearchOptions.getConstraintList(session);
        // if no constraints, don't do a search...
        if (UtilValidate.isNotEmpty(contentSearchConstraintList)) {
            // if the search options have changed since the last search, put at the beginning of the options history list
            ContentSearchSession.checkSaveSearchOptionsHistory(session);

            ResultSortOrder resultSortOrder = ContentSearchOptions.getResultSortOrder(request);

            ContentSearchContext contentSearchContext = new ContentSearchContext(delegator, visitId);
            contentSearchContext.addContentSearchConstraints(contentSearchConstraintList);

            contentSearchContext.setResultSortOrder(resultSortOrder);
            contentSearchContext.setResultOffset(resultOffset);
            contentSearchContext.setMaxResults(maxResults);

            contentIds = contentSearchContext.doSearch();

            Integer totalResults = contentSearchContext.getTotalResults();
            if (totalResults != null) {
                listSize = totalResults;
            }
        }

        if (listSize < highIndex) {
            highIndex = listSize;
        }

        // ========== Setup other display info
        List<String> searchConstraintStrings = ContentSearchSession.searchGetConstraintStrings(false, session, delegator);
        String searchSortOrderString = ContentSearchSession.searchGetSortOrderString(false, request);

        // ========== populate the result Map
        Map<String, Object> result = new HashMap<>();

        result.put("contentIds", contentIds);
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
