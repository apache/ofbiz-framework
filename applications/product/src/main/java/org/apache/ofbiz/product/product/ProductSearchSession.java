/*
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
 */
package org.apache.ofbiz.product.product;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilHttp;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.model.DynamicViewEntity;
import org.apache.ofbiz.entity.model.ModelKeyMap;
import org.apache.ofbiz.entity.util.EntityListIterator;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtilProperties;
import org.apache.ofbiz.product.catalog.CatalogWorker;
import org.apache.ofbiz.product.category.CategoryWorker;
import org.apache.ofbiz.product.feature.ParametricSearch;
import org.apache.ofbiz.product.product.ProductSearch.CategoryConstraint;
import org.apache.ofbiz.product.product.ProductSearch.FeatureConstraint;
import org.apache.ofbiz.product.product.ProductSearch.KeywordConstraint;
import org.apache.ofbiz.product.product.ProductSearch.ProductSearchConstraint;
import org.apache.ofbiz.product.product.ProductSearch.ProductSearchContext;
import org.apache.ofbiz.product.product.ProductSearch.ResultSortOrder;
import org.apache.ofbiz.product.product.ProductSearch.SortKeywordRelevancy;
import org.apache.ofbiz.product.store.ProductStoreWorker;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.webapp.control.RequestHandler;
import org.apache.ofbiz.webapp.stats.VisitHandler;

/**
 *  Utility class with methods to prepare and perform ProductSearch operations in the content of an HttpSession
 */
public class ProductSearchSession {

    public static final String module = ProductSearchSession.class.getName();

    @SuppressWarnings("serial")
    public static class ProductSearchOptions implements java.io.Serializable {
        protected List<ProductSearchConstraint> constraintList = null;
        protected String topProductCategoryId = null;
        protected ResultSortOrder resultSortOrder = null;
        protected Integer viewIndex = null;
        protected Integer viewSize = null;
        protected boolean changed = false;
        protected String paging = "Y";
        protected Integer previousViewSize = null;

        public ProductSearchOptions() { }

        /** Basic copy constructor */
        public ProductSearchOptions(ProductSearchOptions productSearchOptions) {
            this.constraintList = new LinkedList<>();
            if (UtilValidate.isNotEmpty(productSearchOptions.constraintList)) {
                this.constraintList.addAll(productSearchOptions.constraintList);
            }
            this.topProductCategoryId = productSearchOptions.topProductCategoryId;
            this.resultSortOrder = productSearchOptions.resultSortOrder;
            this.viewIndex = productSearchOptions.viewIndex;
            this.viewSize = productSearchOptions.viewSize;
            this.changed = productSearchOptions.changed;
            this.paging = productSearchOptions.paging;
            this.previousViewSize = productSearchOptions.previousViewSize;
        }

        public List<ProductSearchConstraint> getConstraintList() {
            return this.constraintList;
        }
        public static List<ProductSearchConstraint> getConstraintList(HttpSession session) {
            return getProductSearchOptions(session).constraintList;
        }
        public static void addConstraint(ProductSearchConstraint productSearchConstraint, HttpSession session) {
            ProductSearchOptions productSearchOptions = getProductSearchOptions(session);
            if (productSearchOptions.constraintList == null) {
                productSearchOptions.constraintList = new LinkedList<>();
            }
            if (!productSearchOptions.constraintList.contains(productSearchConstraint)) {
                productSearchOptions.constraintList.add(productSearchConstraint);
                productSearchOptions.changed = true;
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
            ProductSearchOptions productSearchOptions = getProductSearchOptions(request.getSession());
            return productSearchOptions.getResultSortOrder();
        }
        public static void setResultSortOrder(ResultSortOrder resultSortOrder, HttpSession session) {
            ProductSearchOptions productSearchOptions = getProductSearchOptions(session);
            productSearchOptions.resultSortOrder = resultSortOrder;
            productSearchOptions.changed = true;
        }

        public static void clearSearchOptions(HttpSession session) {
            ProductSearchOptions productSearchOptions = getProductSearchOptions(session);
            productSearchOptions.constraintList = null;
            productSearchOptions.topProductCategoryId = null;
            productSearchOptions.resultSortOrder = null;
        }

        public void clearViewInfo() {
            this.viewIndex = null;
            this.viewSize = null;
            this.paging = "Y";
            this.previousViewSize = null;
        }

        /**
         * Get the view size
         * @return returns the viewIndex.
         */
        public Integer getViewIndex() {
            return viewIndex;
        }
        /**
         * Set the view index
         * @param viewIndex the viewIndex to set.
         */
        public void setViewIndex(Integer viewIndex) {
            this.viewIndex = viewIndex;
        }
        /**
         * Set the view index
         * @param viewIndexStr the viewIndex to set.
         */
        public void setViewIndex(String viewIndexStr) {
            if (UtilValidate.isEmpty(viewIndexStr)) {
                return;
            }
            try {
                this.setViewIndex(Integer.valueOf(viewIndexStr));
            } catch (Exception e) {
                Debug.logError(e, "Error in formatting of VIEW_INDEX [" + viewIndexStr + "], setting to 20", module);
                if (this.viewIndex == null) {
                    this.setViewIndex(20);
                }
            }
        }

        /**
         * Get the view size
         * @return returns the view size.
         */
        public Integer getViewSize() {
            return viewSize;
        }

        /**
         * Set the view size
         * @param viewSize the view size to set.
         */
        public void setViewSize(Integer viewSize) {
            setPreviousViewSize(getViewSize());
            this.viewSize = viewSize;
        }

        /**
         * Set the view size
         * @param viewSizeStr the view size to set.
         */
        public void setViewSize(String viewSizeStr) {
            if (UtilValidate.isEmpty(viewSizeStr)) {
                return;
            }
            try {
                this.setViewSize(Integer.valueOf(viewSizeStr));
            } catch (Exception e) {
                Debug.logError(e, "Error in formatting of VIEW_SIZE [" + viewSizeStr + "], setting to 20", module);
                if (this.viewSize == null) {
                    this.setViewSize(20);
                }
            }
        }

        /**
         * Get the paging
         * @return Returns the paging
         */
        public String getPaging() {
            return paging;
        }

        /**
         * Set the paging
         * @param paging the paging to set
         */
        public void setPaging(String paging) {
            if (paging == null) {
                paging = "Y";
            }
            this.paging = paging;
        }

        /**
         * Get the previous view size
         * @return returns the previous view size
         */
        public Integer getPreviousViewSize() {
            return previousViewSize;
        }
        /**
         * Set the previous view size
         * @param previousViewSize the previousViewSize to set.
         */
        public void setPreviousViewSize(Integer previousViewSize) {
            if (previousViewSize == null) {
                this.previousViewSize = 20;
            } else {
                this.previousViewSize = previousViewSize;
            }
        }

        public String getTopProductCategoryId() {
            return topProductCategoryId;
        }

        public static void setTopProductCategoryId(String topProductCategoryId, HttpSession session) {
            ProductSearchOptions productSearchOptions = getProductSearchOptions(session);
            productSearchOptions.setTopProductCategoryId(topProductCategoryId);
        }

        public void setTopProductCategoryId(String topProductCategoryId) {
            if (this.topProductCategoryId != null && topProductCategoryId != null) {
                if (!this.topProductCategoryId.equals(topProductCategoryId)) {
                    this.topProductCategoryId = topProductCategoryId;
                    this.changed = true;
                }
            } else {
                if (this.topProductCategoryId != null || topProductCategoryId != null) {
                    this.topProductCategoryId = topProductCategoryId;
                    this.changed = true;
                }
            }
        }

        public List<String> searchGetConstraintStrings(boolean detailed, Delegator delegator, Locale locale) {
            List<ProductSearchConstraint> productSearchConstraintList = this.getConstraintList();
            List<String> constraintStrings = new LinkedList<>();
            if (productSearchConstraintList == null) {
                return constraintStrings;
            }
            for (ProductSearchConstraint productSearchConstraint: productSearchConstraintList) {
                if (productSearchConstraint == null) {
                    continue;
                }
                String constraintString = productSearchConstraint.prettyPrintConstraint(delegator, detailed, locale);
                if (UtilValidate.isNotEmpty(constraintString)) {
                    constraintStrings.add(constraintString);
                } else {
                    constraintStrings.add("Description not available");
                }
            }
            return constraintStrings;
        }
        public List<String> searchGetConstraintStrings(boolean detailed, LocalDispatcher dispatcher, Locale locale) {
            List<ProductSearchConstraint> productSearchConstraintList = this.getConstraintList();
            List<String> constraintStrings = new LinkedList<>();
            if (productSearchConstraintList == null) {
                return constraintStrings;
            }
            for (ProductSearchConstraint productSearchConstraint: productSearchConstraintList) {
                if (productSearchConstraint == null) {
                    continue;
                }                
                String constraintString = productSearchConstraint.prettyPrintConstraint(dispatcher, detailed, locale);
                if (UtilValidate.isNotEmpty(constraintString)) {
                    constraintStrings.add(constraintString);
                } else {
                    constraintStrings.add("Description not available");
                }
            }
            return constraintStrings;
        }
    }

    public static ProductSearchOptions getProductSearchOptions(HttpSession session) {
        ProductSearchOptions productSearchOptions = (ProductSearchOptions) session.getAttribute("_PRODUCT_SEARCH_OPTIONS_CURRENT_");
        if (productSearchOptions == null) {
            productSearchOptions = new ProductSearchOptions();
            session.setAttribute("_PRODUCT_SEARCH_OPTIONS_CURRENT_", productSearchOptions);
        }
        return productSearchOptions;
    }

    public static void checkSaveSearchOptionsHistory(HttpSession session) {
        ProductSearchOptions productSearchOptions = getProductSearchOptions(session);
        // if the options have changed since the last search, add it to the beginning of the search options history
        if (productSearchOptions.changed) {
            List<ProductSearchOptions> optionsHistoryList = getSearchOptionsHistoryList(session);
            optionsHistoryList.add(0, new ProductSearchOptions(productSearchOptions));
            productSearchOptions.changed = false;
        }
    }
    public static List<ProductSearchOptions> getSearchOptionsHistoryList(HttpSession session) {
        List<ProductSearchOptions> optionsHistoryList = UtilGenerics.checkList(session.getAttribute("_PRODUCT_SEARCH_OPTIONS_HISTORY_"));
        if (optionsHistoryList == null) {
            optionsHistoryList = new LinkedList<>();
            session.setAttribute("_PRODUCT_SEARCH_OPTIONS_HISTORY_", optionsHistoryList);
        }
        return optionsHistoryList;
    }
    public static void clearSearchOptionsHistoryList(HttpSession session) {
        session.removeAttribute("_PRODUCT_SEARCH_OPTIONS_HISTORY_");
    }

    public static void setCurrentSearchFromHistory(int index, boolean removeOld, HttpSession session) {
        List<ProductSearchOptions> searchOptionsHistoryList = getSearchOptionsHistoryList(session);
        if (index < searchOptionsHistoryList.size()) {
            ProductSearchOptions productSearchOptions = searchOptionsHistoryList.get(index);
            if (removeOld) {
                searchOptionsHistoryList.remove(index);
            }
            if (productSearchOptions != null) {
                session.setAttribute("_PRODUCT_SEARCH_OPTIONS_CURRENT_", new ProductSearchOptions(productSearchOptions));
            }
        } else {
            throw new IllegalArgumentException("Could not set current search options to history index [" + index + "], only [" + searchOptionsHistoryList.size() + "] entries in the history list.");
        }
    }

    public static String clearSearchOptionsHistoryList(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession();
        clearSearchOptionsHistoryList(session);
        return "success";
    }

    public static String setCurrentSearchFromHistory(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession();
        String searchHistoryIndexStr = request.getParameter("searchHistoryIndex");
        String removeOldStr = request.getParameter("removeOld");

        if (UtilValidate.isEmpty(searchHistoryIndexStr)) {
            request.setAttribute("_ERROR_MESSAGE_", "No search history index passed, cannot set current search to previous.");
            return "error";
        }

        try {
            int searchHistoryIndex = Integer.parseInt(searchHistoryIndexStr);
            boolean removeOld = true;
            if (UtilValidate.isNotEmpty(removeOldStr)) {
                removeOld = !"false".equals(removeOldStr);
            }
            setCurrentSearchFromHistory(searchHistoryIndex, removeOld, session);
        } catch (Exception e) {
            request.setAttribute("_ERROR_MESSAGE_", e.toString());
            return "error";
        }

        return "success";
    }

    /** A ControlServlet event method used to check to see if there is an override for any of the current keywords in the search */
    public static final String checkDoKeywordOverride(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession();
        Delegator delegator = (Delegator) request.getAttribute("delegator");

        // get the current productStoreId
        String productStoreId = ProductStoreWorker.getProductStoreId(request);
        if (productStoreId != null) {
            // get a Set of all keywords in the search, if there are any...
            Set<String> keywords = new HashSet<>();
            List<ProductSearchConstraint> constraintList = ProductSearchOptions.getConstraintList(session);
            if (constraintList != null) {
                for (ProductSearchConstraint constraint: constraintList) {
                    if (constraint instanceof KeywordConstraint) {
                        KeywordConstraint keywordConstraint = (KeywordConstraint) constraint;
                        Set<String> keywordSet = keywordConstraint.makeFullKeywordSet(delegator);
                        if (keywordSet != null) {
                            keywords.addAll(keywordSet);
                        }
                    }
                }
            }

            if (keywords.size() > 0) {
                List<GenericValue> productStoreKeywordOvrdList = null;
                try {
                    productStoreKeywordOvrdList = EntityQuery.use(delegator).from("ProductStoreKeywordOvrd").where("productStoreId", productStoreId).orderBy("-fromDate").cache(true).filterByDate().queryList();
                } catch (GenericEntityException e) {
                    Debug.logError(e, "Error reading ProductStoreKeywordOvrd list, not doing keyword override", module);
                }

                if (UtilValidate.isNotEmpty(productStoreKeywordOvrdList)) {
                    for (GenericValue productStoreKeywordOvrd: productStoreKeywordOvrdList) {
                        String ovrdKeyword = productStoreKeywordOvrd.getString("keyword");
                        if (keywords.contains(ovrdKeyword)) {
                            String targetTypeEnumId = productStoreKeywordOvrd.getString("targetTypeEnumId");
                            String target = productStoreKeywordOvrd.getString("target");
                            ServletContext ctx = request.getServletContext();
                            RequestHandler rh = (RequestHandler) ctx.getAttribute("_REQUEST_HANDLER_");
                            if ("KOTT_PRODCAT".equals(targetTypeEnumId)) {
                                String requestName = "/category/~category_id=" + target;
                                target = rh.makeLink(request, response, requestName, false, false, false);
                            } else if ("KOTT_PRODUCT".equals(targetTypeEnumId)) {
                                String requestName = "/product/~product_id=" + target;
                                target = rh.makeLink(request, response, requestName, false, false, false);
                            } else if ("KOTT_OFBURL".equals(targetTypeEnumId)) {
                                target = rh.makeLink(request, response, target, false, false, false);
                            } else if ("KOTT_AURL".equals(targetTypeEnumId)) {
                                // do nothing, is absolute URL
                            } else {
                                Debug.logError("The targetTypeEnumId [] is not recognized, not doing keyword override", module);
                                // might as well see if there are any others...
                                continue;
                            }
                            try {
                                response.sendRedirect(target);
                                return "none";
                            } catch (IOException e) {
                                Debug.logError(e, "Could not send redirect to: " + target, module);
                                continue;
                            }
                        }
                    }
                }
            }
        }

        return "success";
    }

    public static ArrayList<String> searchDo(HttpSession session, Delegator delegator, String prodCatalogId) {
        String visitId = VisitHandler.getVisitId(session);
        ProductSearchOptions productSearchOptions = getProductSearchOptions(session);
        List<ProductSearchConstraint> productSearchConstraintList = productSearchOptions.getConstraintList();
        if (UtilValidate.isEmpty(productSearchConstraintList)) {
            // no constraints, don't do a search...
            return new ArrayList<>();
        }

        ResultSortOrder resultSortOrder = productSearchOptions.getResultSortOrder();

        // if the search options have changed since the last search, put at the beginning of the options history list
        checkSaveSearchOptionsHistory(session);

        return ProductSearch.searchProducts(productSearchConstraintList, resultSortOrder, delegator, visitId);
    }

    public static void searchClear(HttpSession session) {
        ProductSearchOptions.clearSearchOptions(session);
    }

    public static List<String> searchGetConstraintStrings(boolean detailed, HttpSession session, Delegator delegator) {
        Locale locale = UtilHttp.getLocale(session);
        ProductSearchOptions productSearchOptions = getProductSearchOptions(session);
        LocalDispatcher dispatcher = (LocalDispatcher) session.getAttribute("dispatcher");
        return productSearchOptions.searchGetConstraintStrings(detailed, dispatcher, locale);
    }

    public static String searchGetSortOrderString(boolean detailed, HttpServletRequest request) {
        Locale locale = UtilHttp.getLocale(request);
        ResultSortOrder resultSortOrder = ProductSearchOptions.getResultSortOrder(request);
        return resultSortOrder.prettyPrintSortOrder(detailed, locale);
    }

    public static void searchSetSortOrder(ResultSortOrder resultSortOrder, HttpSession session) {
        ProductSearchOptions.setResultSortOrder(resultSortOrder, session);
    }

    public static void searchAddFeatureIdConstraints(Collection<String> featureIds, Boolean exclude, HttpServletRequest request) {
        HttpSession session = request.getSession();
        if (UtilValidate.isEmpty(featureIds)) {
            return;
        }
        for (String productFeatureId: featureIds) {
            searchAddConstraint(new FeatureConstraint(productFeatureId, exclude), session);
        }
    }

    public static void searchAddConstraint(ProductSearchConstraint productSearchConstraint, HttpSession session) {
        ProductSearchOptions.addConstraint(productSearchConstraint, session);
    }

    public static void searchRemoveConstraint(int index, HttpSession session) {
        List<ProductSearchConstraint> productSearchConstraintList = ProductSearchOptions.getConstraintList(session);
        if (productSearchConstraintList == null) {
            return;
        } else if (index >= productSearchConstraintList.size()) {
            return;
        } else {
            productSearchConstraintList.remove(index);
        }
    }

    public static void processSearchParameters(Map<String, Object> parameters, HttpServletRequest request) {
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        Boolean alreadyRun = (Boolean) request.getAttribute("processSearchParametersAlreadyRun");
        if (Boolean.TRUE.equals(alreadyRun)) {
            return;
        }
        request.setAttribute("processSearchParametersAlreadyRun", Boolean.TRUE);

        HttpSession session = request.getSession();
        boolean constraintsChanged = false;
        GenericValue productStore = ProductStoreWorker.getProductStore(request);

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

        String prioritizeCategoryId = null;
        if (UtilValidate.isNotEmpty(parameters.get("PRIORITIZE_CATEGORY_ID"))) {
            prioritizeCategoryId = (String) parameters.get("PRIORITIZE_CATEGORY_ID");
        } else if (UtilValidate.isNotEmpty(parameters.get("S_TPC"))) {
            prioritizeCategoryId = (String) parameters.get("S_TPC");
        }
        if (UtilValidate.isNotEmpty(prioritizeCategoryId)) {
            ProductSearchOptions.setTopProductCategoryId(prioritizeCategoryId, session);
            constraintsChanged = true;
        }

        // if there is another category, add a constraint for it
        if (UtilValidate.isNotEmpty(parameters.get("SEARCH_CATEGORY_ID"))) {
            String searchCategoryId = (String) parameters.get("SEARCH_CATEGORY_ID");
            String searchSubCategories = (String) parameters.get("SEARCH_SUB_CATEGORIES");
            String searchCategoryExc = (String) parameters.get("SEARCH_CATEGORY_EXC");
            Boolean exclude = UtilValidate.isEmpty(searchCategoryExc) ? null : !"N".equals(searchCategoryExc);
            searchAddConstraint(new ProductSearch.CategoryConstraint(searchCategoryId, !"N".equals(searchSubCategories), exclude), session);
            constraintsChanged = true;
        }

        for (int catNum = 1; catNum < 10; catNum++) {
            if (UtilValidate.isNotEmpty(parameters.get("SEARCH_CATEGORY_ID" + catNum))) {
                String searchCategoryId = (String) parameters.get("SEARCH_CATEGORY_ID" + catNum);
                String searchSubCategories = (String) parameters.get("SEARCH_SUB_CATEGORIES" + catNum);
                String searchCategoryExc = (String) parameters.get("SEARCH_CATEGORY_EXC" + catNum);
                Boolean exclude = UtilValidate.isEmpty(searchCategoryExc) ? null : !"N".equals(searchCategoryExc);
                searchAddConstraint(new ProductSearch.CategoryConstraint(searchCategoryId, !"N".equals(searchSubCategories), exclude), session);
                constraintsChanged = true;
            }
        }

        // a shorter variation for categories
        for (int catNum = 1; catNum < 10; catNum++) {
            if (UtilValidate.isNotEmpty(parameters.get("S_CAT" + catNum))) {
                String searchCategoryId = (String) parameters.get("S_CAT" + catNum);
                String searchSubCategories = (String) parameters.get("S_CSB" + catNum);
                String searchCategoryExc = (String) parameters.get("S_CEX" + catNum);
                Boolean exclude = UtilValidate.isEmpty(searchCategoryExc) ? null : !"N".equals(searchCategoryExc);
                searchAddConstraint(new ProductSearch.CategoryConstraint(searchCategoryId, !"N".equals(searchSubCategories), exclude), session);
                constraintsChanged = true;
            }
        }

        // if there is any category selected try to use catalog and add a constraint for it
        if (UtilValidate.isNotEmpty(parameters.get("SEARCH_CATALOG_ID"))) {
            String searchCatalogId = (String) parameters.get("SEARCH_CATALOG_ID");
            if (searchCatalogId != null && !searchCatalogId.equalsIgnoreCase("")) {
                String topCategory = CatalogWorker.getCatalogTopCategoryId(request, searchCatalogId);
                if (UtilValidate.isEmpty(topCategory)) {
                    topCategory = CatalogWorker.getCatalogTopEbayCategoryId(request, searchCatalogId);
                }
                List<GenericValue> categories = CategoryWorker.getRelatedCategoriesRet(request, "topLevelList", topCategory, true, false, true);
                searchAddConstraint(new ProductSearch.CatalogConstraint(searchCatalogId, categories), session);
                constraintsChanged = true;
            }
        }

        // if keywords were specified, add a constraint for them
        if (UtilValidate.isNotEmpty(parameters.get("SEARCH_STRING"))) {
            String keywordString = (String) parameters.get("SEARCH_STRING");
            String searchOperator = (String) parameters.get("SEARCH_OPERATOR");
            // defaults to true/Y, ie anything but N is true/Y
            boolean anyPrefixSuffix = !"N".equals(parameters.get("SEARCH_ANYPRESUF"));
            searchAddConstraint(new ProductSearch.KeywordConstraint(keywordString, anyPrefixSuffix, anyPrefixSuffix, null, "AND".equals(searchOperator)), session);
            constraintsChanged = true;
        }

        // if productName were specified, add a constraint for them
        if (UtilValidate.isNotEmpty(parameters.get("SEARCH_PRODUCT_NAME"))) {
            String productName = (String) parameters.get("SEARCH_PRODUCT_NAME");
            searchAddConstraint(new ProductSearch.ProductFieldConstraint(productName, "productName"), session);
            constraintsChanged = true;
        }

        // if internalName were specified, add a constraint for them
        if (UtilValidate.isNotEmpty(parameters.get("SEARCH_INTERNAL_PROD_NAME"))) {
            String internalName = (String) parameters.get("SEARCH_INTERNAL_PROD_NAME");
            searchAddConstraint(new ProductSearch.ProductFieldConstraint(internalName, "internalName"), session);
            constraintsChanged = true;
        }

        for (int kwNum = 1; kwNum < 10; kwNum++) {
            if (UtilValidate.isNotEmpty(parameters.get("SEARCH_STRING" + kwNum))) {
                String keywordString = (String) parameters.get("SEARCH_STRING" + kwNum);
                String searchOperator = (String) parameters.get("SEARCH_OPERATOR" + kwNum);
                // defaults to true/Y, ie anything but N is true/Y
                boolean anyPrefixSuffix = !"N".equals(parameters.get("SEARCH_ANYPRESUF" + kwNum));
                searchAddConstraint(new ProductSearch.KeywordConstraint(keywordString, anyPrefixSuffix, anyPrefixSuffix, null, "AND".equals(searchOperator)), session);
                constraintsChanged = true;
            }
        }

        for (Entry<String, Object> entry : parameters.entrySet()) {
            String parameterName = entry.getKey();
            if (parameterName.startsWith("SEARCH_FEAT") && !parameterName.startsWith("SEARCH_FEAT_EXC")) {
                String productFeatureId = (String) parameters.get(parameterName);
                if (UtilValidate.isNotEmpty(productFeatureId)) {
                    String paramNameExt = parameterName.substring("SEARCH_FEAT".length());
                    String searchCategoryExc = (String) parameters.get("SEARCH_FEAT_EXC" + paramNameExt);
                    Boolean exclude = UtilValidate.isEmpty(searchCategoryExc) ? null : !"N".equals(searchCategoryExc);
                    //Debug.logInfo("parameterName=" + parameterName + ", paramNameExt=" + paramNameExt + ", searchCategoryExc=" + searchCategoryExc + ", exclude=" + exclude, module);
                    searchAddConstraint(new ProductSearch.FeatureConstraint(productFeatureId, exclude), session);
                    constraintsChanged = true;
                }
            }
            // a shorter feature variation
            if (parameterName.startsWith("S_PFI")) {
                String productFeatureId = (String) parameters.get(parameterName);
                if (UtilValidate.isNotEmpty(productFeatureId)) {
                    String paramNameExt = parameterName.substring("S_PFI".length());
                    String searchCategoryExc = (String) parameters.get("S_PFX" + paramNameExt);
                    Boolean exclude = UtilValidate.isEmpty(searchCategoryExc) ? null : !"N".equals(searchCategoryExc);
                    searchAddConstraint(new ProductSearch.FeatureConstraint(productFeatureId, exclude), session);
                    constraintsChanged = true;
                }
            }

            //if product features category were selected add a constraint for each
            if (parameterName.startsWith("SEARCH_PROD_FEAT_CAT") && !parameterName.startsWith("SEARCH_PROD_FEAT_CAT_EXC")) {
                String productFeatureCategoryId = (String) parameters.get(parameterName);
                if (UtilValidate.isNotEmpty(productFeatureCategoryId)) {
                    String paramNameExt = parameterName.substring("SEARCH_PROD_FEAT_CAT".length());
                    String searchProdFeatureCategoryExc = (String) parameters.get("SEARCH_PROD_FEAT_CAT_EXC" + paramNameExt);
                    Boolean exclude = UtilValidate.isEmpty(searchProdFeatureCategoryExc) ? null : !"N".equals(searchProdFeatureCategoryExc);
                    searchAddConstraint(new ProductSearch.FeatureCategoryConstraint(productFeatureCategoryId, exclude), session);
                    constraintsChanged = true;
                }
            }
            // a shorter variation for feature category
            if (parameterName.startsWith("S_FCI")) {
                String productFeatureCategoryId = (String) parameters.get(parameterName);
                if (UtilValidate.isNotEmpty(productFeatureCategoryId)) {
                    String paramNameExt = parameterName.substring("S_FCI".length());
                    String searchProdFeatureCategoryExc = (String) parameters.get("S_FCX" + paramNameExt);
                    Boolean exclude = UtilValidate.isEmpty(searchProdFeatureCategoryExc) ? null : !"N".equals(searchProdFeatureCategoryExc);
                    searchAddConstraint(new ProductSearch.FeatureCategoryConstraint(productFeatureCategoryId, exclude), session);
                    constraintsChanged = true;
                }
            }

            //if product features group were selected add a constraint for each
            if (parameterName.startsWith("SEARCH_PROD_FEAT_GRP") && !parameterName.startsWith("SEARCH_PROD_FEAT_GRP_EXC")) {
                String productFeatureGroupId = (String) parameters.get(parameterName);
                if (UtilValidate.isNotEmpty(productFeatureGroupId)) {
                    String paramNameExt = parameterName.substring("SEARCH_PROD_FEAT_GRP".length());
                    String searchProdFeatureGroupExc = (String) parameters.get("SEARCH_PROD_FEAT_GRP_EXC" + paramNameExt);
                    Boolean exclude = UtilValidate.isEmpty(searchProdFeatureGroupExc) ? null : !"N".equals(searchProdFeatureGroupExc);
                    searchAddConstraint(new ProductSearch.FeatureGroupConstraint(productFeatureGroupId, exclude), session);
                    constraintsChanged = true;
                }
            }
            // a shorter variation for feature group
            if (parameterName.startsWith("S_FGI")) {
                String productFeatureGroupId = (String) parameters.get(parameterName);
                if (UtilValidate.isNotEmpty(productFeatureGroupId)) {
                    String paramNameExt = parameterName.substring("S_FGI".length());
                    String searchProdFeatureGroupExc = (String) parameters.get("S_FGX" + paramNameExt);
                    Boolean exclude = UtilValidate.isEmpty(searchProdFeatureGroupExc) ? null : !"N".equals(searchProdFeatureGroupExc);
                    searchAddConstraint(new ProductSearch.FeatureGroupConstraint(productFeatureGroupId, exclude), session);
                    constraintsChanged = true;
                }
            }
        }

        // if features were selected add a constraint for each
        Map<String, String> featureIdByType = ParametricSearch.makeFeatureIdByTypeMap(parameters);
        if (featureIdByType.size() > 0) {
            constraintsChanged = true;
            searchAddFeatureIdConstraints(featureIdByType.values(), null, request);
        }

        // add a supplier to the search
        if (UtilValidate.isNotEmpty(parameters.get("SEARCH_SUPPLIER_ID")) || UtilValidate.isNotEmpty(parameters.get("S_SUP"))) {
            String supplierPartyId = (String) parameters.get("SEARCH_SUPPLIER_ID");
            if (UtilValidate.isEmpty(supplierPartyId)) {
                supplierPartyId = (String) parameters.get("S_SUP");
            }
            searchAddConstraint(new ProductSearch.SupplierConstraint(supplierPartyId), session);
            constraintsChanged = true;
        }

        // add a list price range to the search
        if (UtilValidate.isNotEmpty(parameters.get("LIST_PRICE_LOW")) || UtilValidate.isNotEmpty(parameters.get("LIST_PRICE_HIGH"))) {
            BigDecimal listPriceLow = null;
            BigDecimal listPriceHigh = null;
            String listPriceCurrency = UtilHttp.getCurrencyUom(request);
            if (UtilValidate.isNotEmpty(parameters.get("LIST_PRICE_LOW"))) {
                try {
                    listPriceLow = new BigDecimal((String) parameters.get("LIST_PRICE_LOW"));
                } catch (NumberFormatException e) {
                    Debug.logError("Error parsing LIST_PRICE_LOW parameter [" + (String) parameters.get("LIST_PRICE_LOW") + "]: " + e.toString(), module);
                }
            }
            if (UtilValidate.isNotEmpty(parameters.get("LIST_PRICE_HIGH"))) {
                try {
                    listPriceHigh = new BigDecimal((String) parameters.get("LIST_PRICE_HIGH"));
                } catch (NumberFormatException e) {
                    Debug.logError("Error parsing LIST_PRICE_HIGH parameter [" + (String) parameters.get("LIST_PRICE_HIGH") + "]: " + e.toString(), module);
                }
            }
            searchAddConstraint(new ProductSearch.ListPriceRangeConstraint(listPriceLow, listPriceHigh, listPriceCurrency), session);
            constraintsChanged = true;
        }
        if (UtilValidate.isNotEmpty(parameters.get("LIST_PRICE_RANGE")) || UtilValidate.isNotEmpty(parameters.get("S_LPR"))) {
            String listPriceRangeStr = (String) parameters.get("LIST_PRICE_RANGE");
            if (UtilValidate.isEmpty(listPriceRangeStr)) {
                listPriceRangeStr = (String) parameters.get("S_LPR");
            }
            int underscoreIndex = listPriceRangeStr.indexOf('_');
            String listPriceLowStr;
            String listPriceHighStr;
            if (underscoreIndex >= 0) {
                listPriceLowStr = listPriceRangeStr.substring(0, listPriceRangeStr.indexOf('_'));
                listPriceHighStr = listPriceRangeStr.substring(listPriceRangeStr.indexOf('_') + 1);
            } else {
                // no underscore: assume it is a low range with no high range, ie the ending underscore was left off
                listPriceLowStr = listPriceRangeStr;
                listPriceHighStr = null;
            }

            BigDecimal listPriceLow = null;
            BigDecimal listPriceHigh = null;
            String listPriceCurrency = UtilHttp.getCurrencyUom(request);
            if (UtilValidate.isNotEmpty(listPriceLowStr)) {
                try {
                    listPriceLow = new BigDecimal(listPriceLowStr);
                } catch (NumberFormatException e) {
                    Debug.logError("Error parsing low part of LIST_PRICE_RANGE parameter [" + listPriceLowStr + "]: " + e.toString(), module);
                }
            }
            if (UtilValidate.isNotEmpty(listPriceHighStr)) {
                try {
                    listPriceHigh = new BigDecimal(listPriceHighStr);
                } catch (NumberFormatException e) {
                    Debug.logError("Error parsing high part of LIST_PRICE_RANGE parameter [" + listPriceHighStr + "]: " + e.toString(), module);
                }
            }
            searchAddConstraint(new ProductSearch.ListPriceRangeConstraint(listPriceLow, listPriceHigh, listPriceCurrency), session);
            constraintsChanged = true;
        }

        // check the ProductStore to see if we should add the ExcludeVariantsConstraint
        if (productStore != null && !"N".equals(productStore.getString("prodSearchExcludeVariants"))) {
            searchAddConstraint(new ProductSearch.ExcludeVariantsConstraint(), session);
            // not consider this a change for now, shouldn't change often: constraintsChanged = true;
        }

        if ("true".equalsIgnoreCase((String) parameters.get("AVAILABILITY_FILTER"))) {
            searchAddConstraint(new ProductSearch.AvailabilityDateConstraint(), session);
            constraintsChanged = true;
        }

        if (UtilValidate.isNotEmpty(parameters.get("SEARCH_GOOD_IDENTIFICATION_TYPE")) ||
            UtilValidate.isNotEmpty(parameters.get("SEARCH_GOOD_IDENTIFICATION_VALUE"))) {
            String include = (String) parameters.get("SEARCH_GOOD_IDENTIFICATION_INCL");
            if (UtilValidate.isEmpty(include)) {
                include = "Y";
            }
            Boolean inc =  Boolean.TRUE;
            if ("N".equalsIgnoreCase(include)) {
                inc =  Boolean.FALSE;
            }

            searchAddConstraint(new ProductSearch.GoodIdentificationConstraint((String)parameters.get("SEARCH_GOOD_IDENTIFICATION_TYPE"),
                                (String) parameters.get("SEARCH_GOOD_IDENTIFICATION_VALUE"), inc), session);
            constraintsChanged = true;
        }

        String prodCatalogId = CatalogWorker.getCurrentCatalogId(request);
        String viewProductCategoryId = CatalogWorker.getCatalogViewAllowCategoryId(delegator, prodCatalogId);
        if (UtilValidate.isNotEmpty(viewProductCategoryId)) {
            ProductSearchConstraint viewAllowConstraint = new CategoryConstraint(viewProductCategoryId, true, null);
            searchAddConstraint(viewAllowConstraint, session);
            // not consider this a change for now, shouldn't change often: constraintsChanged = true;
        }

        // set the sort order
        String sortOrder = (String) parameters.get("sortOrder");
        if (UtilValidate.isEmpty(sortOrder)) {
            sortOrder = (String) parameters.get("S_O");
        }
        String sortAscending = (String) parameters.get("sortAscending");
        if (UtilValidate.isEmpty(sortAscending)) {
            sortAscending = (String) parameters.get("S_A");
        }
        boolean ascending = !"N".equals(sortAscending);
        if (sortOrder != null) {
            if ("SortKeywordRelevancy".equals(sortOrder) || "SKR".equals(sortOrder)) {
                searchSetSortOrder(new ProductSearch.SortKeywordRelevancy(), session);
            } else if (sortOrder.startsWith("SortProductField:")) {
                String fieldName = sortOrder.substring("SortProductField:".length());
                searchSetSortOrder(new ProductSearch.SortProductField(fieldName, ascending), session);
            } else if (sortOrder.startsWith("SPF:")) {
                String fieldName = sortOrder.substring("SPF:".length());
                searchSetSortOrder(new ProductSearch.SortProductField(fieldName, ascending), session);
            } else if (sortOrder.startsWith("SortProductPrice:")) {
                String priceTypeId = sortOrder.substring("SortProductPrice:".length());
                searchSetSortOrder(new ProductSearch.SortProductPrice(priceTypeId, ascending), session);
            } else if (sortOrder.startsWith("SPP:")) {
                String priceTypeId = sortOrder.substring("SPP:".length());
                searchSetSortOrder(new ProductSearch.SortProductPrice(priceTypeId, ascending), session);
            } else if (sortOrder.startsWith("SortProductFeature:")) {
                String featureId = sortOrder.substring("SortProductFeature:".length());
                searchSetSortOrder(new ProductSearch.SortProductFeature(featureId, ascending), session);
            } else if (sortOrder.startsWith("SPFT:")) {
                String priceTypeId = sortOrder.substring("SPFT:".length());
                searchSetSortOrder(new ProductSearch.SortProductPrice(priceTypeId, ascending), session);
            }
        }

        ProductSearchOptions productSearchOptions = getProductSearchOptions(session);
        if (constraintsChanged) {
            // query changed, clear out the VIEW_INDEX & VIEW_SIZE
            productSearchOptions.clearViewInfo();
        }

        productSearchOptions.setViewIndex((String) parameters.get("VIEW_INDEX"));
        productSearchOptions.setViewSize((String) parameters.get("VIEW_SIZE"));
        productSearchOptions.setPaging((String) parameters.get("PAGING"));
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> getProductSearchResult(HttpServletRequest request, Delegator delegator, String prodCatalogId) {

        // ========== Create View Indexes
        int viewIndex = 0;
        int viewSize = 20;
        int highIndex = 0;
        int lowIndex = 0;
        int listSize = 0;
        String paging = "Y";
        int previousViewSize = 20;
        Map<String, Object> requestParams = UtilHttp.getCombinedMap(request);
        List<String> keywordTypeIds = new LinkedList<>();
        if (requestParams.get("keywordTypeId") instanceof String) {
            keywordTypeIds.add((String) requestParams.get("keywordTypeId"));
        } else if (requestParams.get("keywordTypeId") instanceof List){
            keywordTypeIds = (List<String>) requestParams.get("keywordTypeId");
        }
        String statusId = (String) requestParams.get("statusId");

        HttpSession session = request.getSession();
        ProductSearchOptions productSearchOptions = getProductSearchOptions(session);

        String addOnTopProdCategoryId = productSearchOptions.getTopProductCategoryId();

        Integer viewIndexInteger = productSearchOptions.getViewIndex();
        if (viewIndexInteger != null) {
            viewIndex = viewIndexInteger;
        }

        Integer viewSizeInteger = productSearchOptions.getViewSize();
        if (viewSizeInteger != null) {
            viewSize = viewSizeInteger;
        }

        Integer previousViewSizeInteger = productSearchOptions.getPreviousViewSize();
        if (previousViewSizeInteger != null) {
            previousViewSize = previousViewSizeInteger;
        }

        String pag = productSearchOptions.getPaging();
        paging = pag;

        lowIndex = viewIndex * viewSize;
        highIndex = (viewIndex + 1) * viewSize;

        // ========== Do the actual search
        List<String> productIds = new LinkedList<>();
        String visitId = VisitHandler.getVisitId(session);
        List<ProductSearchConstraint> productSearchConstraintList = ProductSearchOptions.getConstraintList(session);
        String noConditionFind = (String) requestParams.get("noConditionFind");
        if (UtilValidate.isEmpty(noConditionFind)) {
            noConditionFind = EntityUtilProperties.getPropertyValue("widget", "widget.defaultNoConditionFind", delegator);
        }
        // if noConditionFind to Y then find without conditions otherwise search according to constraints.
        if ("Y".equals(noConditionFind) || UtilValidate.isNotEmpty(productSearchConstraintList)) {
            // if the search options have changed since the last search, put at the beginning of the options history list
            checkSaveSearchOptionsHistory(session);

            int addOnTopTotalListSize = 0;
            int addOnTopListSize = 0;
            List<GenericValue> addOnTopProductCategoryMembers;
            if (UtilValidate.isNotEmpty(addOnTopProdCategoryId)) {
                // always include the members of the addOnTopProdCategoryId
                Timestamp now = UtilDateTime.nowTimestamp();
                List<EntityCondition> addOnTopProdCondList = new LinkedList<>();
                addOnTopProdCondList.add(EntityCondition.makeCondition(EntityCondition.makeCondition("thruDate", EntityOperator.EQUALS, null), EntityOperator.OR, EntityCondition.makeCondition("thruDate", EntityOperator.GREATER_THAN, now)));
                addOnTopProdCondList.add(EntityCondition.makeCondition("fromDate", EntityOperator.LESS_THAN, now));
                addOnTopProdCondList.add(EntityCondition.makeCondition("productCategoryId", EntityOperator.EQUALS, addOnTopProdCategoryId));
                EntityQuery eq = EntityQuery.use(delegator)
                        .select(UtilMisc.toSet("productId", "sequenceNum"))
                        .from("ProductCategoryMember")
                        .where(addOnTopProdCondList)
                        .orderBy("sequenceNum")
                        .cursorScrollInsensitive()
                        .distinct()
                        .maxRows(highIndex);

                try (EntityListIterator pli = eq.queryIterator()) {
                    addOnTopProductCategoryMembers = pli.getPartialList(lowIndex, viewSize);
                    addOnTopListSize = addOnTopProductCategoryMembers.size();
                    for (GenericValue alwaysAddProductCategoryMember: addOnTopProductCategoryMembers) {
                        productIds.add(alwaysAddProductCategoryMember.getString("productId"));
                    }
                    addOnTopTotalListSize = pli.getResultsSizeAfterPartialList();
                    listSize = listSize + addOnTopTotalListSize;
                } catch (GenericEntityException e) {
                    Debug.logError(e, module);
                }
            }

            // setup resultOffset and maxResults, noting that resultOffset is 1 based, not zero based as these numbers
            int resultOffsetInt = lowIndex - addOnTopTotalListSize + 1;
            if (resultOffsetInt < 1) {
                resultOffsetInt = 1;
            }
            int maxResultsInt = viewSize - addOnTopListSize;
            Integer resultOffset = resultOffsetInt;
            Integer maxResults = maxResultsInt;

            ResultSortOrder resultSortOrder = ProductSearchOptions.getResultSortOrder(request);

            ProductSearchContext productSearchContext = new ProductSearchContext(delegator, visitId);
            if (UtilValidate.isNotEmpty(productSearchConstraintList)) {
                productSearchContext.addProductSearchConstraints(productSearchConstraintList);
            }
            productSearchContext.setResultSortOrder(resultSortOrder);
            productSearchContext.setResultOffset(resultOffset);
            productSearchContext.setMaxResults(maxResults);

            if (UtilValidate.isNotEmpty(keywordTypeIds)) {
                productSearchContext.keywordTypeIds = keywordTypeIds;
            } else {
                 productSearchContext.keywordTypeIds = UtilMisc.toList("KWT_KEYWORD");
            }

            if (UtilValidate.isNotEmpty(statusId)) {
                productSearchContext.statusId = statusId;
            }

            List<String> foundProductIds = productSearchContext.doSearch();
            if (maxResultsInt > 0) {
                productIds.addAll(foundProductIds);
            }

            Integer totalResults = productSearchContext.getTotalResults();
            if (totalResults != null) {
                listSize = listSize + totalResults;
            }
        }

        if (listSize < highIndex) {
            highIndex = listSize;
        }

        // ========== Setup other display info
        List<String> searchConstraintStrings = searchGetConstraintStrings(false, session, delegator);
        String searchSortOrderString = searchGetSortOrderString(false, request);

        // ========== populate the result Map
        Map<String, Object> result = new HashMap<>();

        result.put("productIds", productIds);
        result.put("viewIndex", viewIndex);
        result.put("viewSize", viewSize);
        result.put("listSize", listSize);
        result.put("lowIndex", lowIndex);
        result.put("highIndex", highIndex);
        result.put("paging", paging);
        result.put("previousViewSize", previousViewSize);
        result.put("searchConstraintStrings", searchConstraintStrings);
        result.put("searchSortOrderString", searchSortOrderString);
        result.put("noConditionFind", noConditionFind);

        return result;
    }

    public static String makeSearchParametersString(HttpSession session) {
        return makeSearchParametersString(getProductSearchOptions(session));
    }
    public static String makeSearchParametersString(ProductSearchOptions productSearchOptions) {
        StringBuilder searchParamString = new StringBuilder();

        List<ProductSearchConstraint> constraintList = productSearchOptions.getConstraintList();
        if (UtilValidate.isEmpty(constraintList)) {
            constraintList = new ArrayList<>();
        }
        int categoriesCount = 0;
        int featuresCount = 0;
        int featureCategoriesCount = 0;
        int featureGroupsCount = 0;
        int keywordsCount = 0;
        boolean isNotFirst = false;
        for (ProductSearchConstraint psc: constraintList) {
            if (psc instanceof ProductSearch.CategoryConstraint) {
                ProductSearch.CategoryConstraint cc = (ProductSearch.CategoryConstraint) psc;
                categoriesCount++;
                if (isNotFirst) {
                    searchParamString.append("&amp;");
                } else {
                    isNotFirst = true;
                }
                searchParamString.append("S_CAT");
                searchParamString.append(categoriesCount);
                searchParamString.append("=");
                searchParamString.append(cc.productCategoryId);
                searchParamString.append("&amp;S_CSB");
                searchParamString.append(categoriesCount);
                searchParamString.append("=");
                searchParamString.append(cc.includeSubCategories ? "Y" : "N");
                if (cc.exclude != null) {
                    searchParamString.append("&amp;S_CEX");
                    searchParamString.append(categoriesCount);
                    searchParamString.append("=");
                    searchParamString.append(cc.exclude ? "Y" : "N");
                }
            } else if (psc instanceof ProductSearch.FeatureConstraint) {
                ProductSearch.FeatureConstraint fc = (ProductSearch.FeatureConstraint) psc;
                featuresCount++;
                if (isNotFirst) {
                    searchParamString.append("&amp;");
                } else {
                    isNotFirst = true;
                }
                searchParamString.append("S_PFI");
                searchParamString.append(featuresCount);
                searchParamString.append("=");
                searchParamString.append(fc.productFeatureId);
                if (fc.exclude != null) {
                    searchParamString.append("&amp;S_PFX");
                    searchParamString.append(featuresCount);
                    searchParamString.append("=");
                    searchParamString.append(fc.exclude ? "Y" : "N");
                }
            } else if (psc instanceof ProductSearch.FeatureCategoryConstraint) {
                ProductSearch.FeatureCategoryConstraint pfcc = (ProductSearch.FeatureCategoryConstraint) psc;
                featureCategoriesCount++;
                if (isNotFirst) {
                    searchParamString.append("&amp;");
                } else {
                    isNotFirst = true;
                }
                searchParamString.append("S_FCI");
                searchParamString.append(featureCategoriesCount);
                searchParamString.append("=");
                searchParamString.append(pfcc.productFeatureCategoryId);
                if (pfcc.exclude != null) {
                    searchParamString.append("&amp;S_FCX");
                    searchParamString.append(featureCategoriesCount);
                    searchParamString.append("=");
                    searchParamString.append(pfcc.exclude ? "Y" : "N");
                }
            } else if (psc instanceof ProductSearch.FeatureGroupConstraint) {
                ProductSearch.FeatureGroupConstraint pfgc = (ProductSearch.FeatureGroupConstraint) psc;
                featureGroupsCount++;
                if (isNotFirst) {
                    searchParamString.append("&amp;");
                } else {
                    isNotFirst = true;
                }
                searchParamString.append("S_FGI");
                searchParamString.append(featureGroupsCount);
                searchParamString.append("=");
                searchParamString.append(pfgc.productFeatureGroupId);
                if (pfgc.exclude != null) {
                    searchParamString.append("&amp;S_FGX");
                    searchParamString.append(featureGroupsCount);
                    searchParamString.append("=");
                    searchParamString.append(pfgc.exclude ? "Y" : "N");
                }
            } else if (psc instanceof ProductSearch.KeywordConstraint) {
                ProductSearch.KeywordConstraint kc = (ProductSearch.KeywordConstraint) psc;
                keywordsCount++;
                if (isNotFirst) {
                    searchParamString.append("&amp;");
                } else {
                    isNotFirst = true;
                }
                searchParamString.append("SEARCH_STRING");
                searchParamString.append(keywordsCount);
                searchParamString.append("=");
                searchParamString.append(UtilHttp.encodeBlanks(kc.keywordsString));
                searchParamString.append("&amp;SEARCH_OPERATOR");
                searchParamString.append(keywordsCount);
                searchParamString.append("=");
                searchParamString.append(kc.isAnd ? "AND" : "OR");
                searchParamString.append("&amp;SEARCH_ANYPRESUF");
                searchParamString.append(keywordsCount);
                searchParamString.append("=");
                searchParamString.append(kc.anyPrefix | kc.anySuffix ? "Y" : "N");
            } else if (psc instanceof ProductSearch.ListPriceRangeConstraint) {
                ProductSearch.ListPriceRangeConstraint lprc = (ProductSearch.ListPriceRangeConstraint) psc;
                if (lprc.lowPrice != null || lprc.highPrice != null) {
                    if (isNotFirst) {
                        searchParamString.append("&amp;");
                    } else {
                        isNotFirst = true;
                    }
                    searchParamString.append("S_LPR");
                    searchParamString.append("=");
                    if (lprc.lowPrice != null) {
                        searchParamString.append(lprc.lowPrice);
                    }
                    searchParamString.append("_");
                    if (lprc.highPrice != null) {
                        searchParamString.append(lprc.highPrice);
                    }
                }
            } else if (psc instanceof ProductSearch.SupplierConstraint) {
                ProductSearch.SupplierConstraint suppc = (ProductSearch.SupplierConstraint) psc;
                if (suppc.supplierPartyId != null) {
                    if (isNotFirst) {
                        searchParamString.append("&amp;");
                    } else {
                        isNotFirst = true;
                    }
                    searchParamString.append("S_SUP");
                    searchParamString.append("=");
                    searchParamString.append(suppc.supplierPartyId);
                }
            }
        }

        String topProductCategoryId = productSearchOptions.getTopProductCategoryId();
        if (topProductCategoryId != null) {
            searchParamString.append("&amp;S_TPC");
            searchParamString.append("=");
            searchParamString.append(topProductCategoryId);
        }
        ResultSortOrder resultSortOrder = productSearchOptions.getResultSortOrder();
        if (resultSortOrder instanceof ProductSearch.SortKeywordRelevancy) {
            searchParamString.append("&amp;S_O=SKR");
        } else if (resultSortOrder instanceof ProductSearch.SortProductField) {
            ProductSearch.SortProductField spf = (ProductSearch.SortProductField) resultSortOrder;
            searchParamString.append("&amp;S_O=SPF:");
            searchParamString.append(spf.fieldName);
        } else if (resultSortOrder instanceof ProductSearch.SortProductPrice) {
            ProductSearch.SortProductPrice spp = (ProductSearch.SortProductPrice) resultSortOrder;
            searchParamString.append("&amp;S_O=SPP:");
            searchParamString.append(spp.productPriceTypeId);
        } else if (resultSortOrder instanceof ProductSearch.SortProductFeature) {
            ProductSearch.SortProductFeature spf = (ProductSearch.SortProductFeature) resultSortOrder;
            searchParamString.append("&amp;S_O=SPFT:");
            searchParamString.append(spf.productFeatureTypeId);
        }
        searchParamString.append("&amp;S_A=");
        searchParamString.append(resultSortOrder.isAscending() ? "Y" : "N");

        return searchParamString.toString();
    }

    /**
     * This method returns a list of productId counts grouped by productFeatureId's of input productFeatureTypeId,
     * the constraint being applied on current ProductSearchConstraint list in session.
     * @param productFeatureTypeId The productFeatureTypeId, productFeatureId's of which should be considered.
     * @param session Current session.
     * @param delegator The delegator object.
     * @return List of Maps containing productFeatureId, productFeatureTypeId, description, featureCount.
     */
    public static List<Map<String, String>> listCountByFeatureForType(String productFeatureTypeId, HttpSession session, Delegator delegator) {
        String visitId = VisitHandler.getVisitId(session);

        ProductSearchContext productSearchContext = new ProductSearchContext(delegator, visitId);
        List<ProductSearchConstraint> productSearchConstraintList = ProductSearchOptions.getConstraintList(session);
        if (UtilValidate.isNotEmpty(productSearchConstraintList)) {
            productSearchContext.addProductSearchConstraints(productSearchConstraintList);
        }
        productSearchContext.finishKeywordConstraints();
        productSearchContext.finishCategoryAndFeatureConstraints();

        DynamicViewEntity dynamicViewEntity = productSearchContext.dynamicViewEntity;
        List<EntityCondition> entityConditionList = productSearchContext.entityConditionList;

        dynamicViewEntity.addMemberEntity("PFAC", "ProductFeatureAppl");
        dynamicViewEntity.addAlias("PFAC", "pfacProductFeatureId", "productFeatureId", null, null, Boolean.TRUE, null);
        dynamicViewEntity.addAlias("PFAC", "pfacFromDate", "fromDate", null, null, null, null);
        dynamicViewEntity.addAlias("PFAC", "pfacThruDate", "thruDate", null, null, null, null);
        dynamicViewEntity.addAlias("PFAC", "featureCount", "productId", null, null, null, "count-distinct");
        dynamicViewEntity.addViewLink("PROD", "PFAC", Boolean.FALSE, ModelKeyMap.makeKeyMapList("productId"));
        entityConditionList.add(EntityCondition.makeCondition(EntityCondition.makeCondition("pfacThruDate", EntityOperator.EQUALS, null), EntityOperator.OR, EntityCondition.makeCondition("pfacThruDate", EntityOperator.GREATER_THAN, UtilDateTime.nowTimestamp())));
        entityConditionList.add(EntityCondition.makeCondition("pfacFromDate", EntityOperator.LESS_THAN, UtilDateTime.nowTimestamp()));

        dynamicViewEntity.addMemberEntity("PFC", "ProductFeature");
        dynamicViewEntity.addAlias("PFC", "pfcProductFeatureTypeId", "productFeatureTypeId", null, null, Boolean.TRUE, null);
        dynamicViewEntity.addAlias("PFC", "pfcDescription", "description", null, null, Boolean.TRUE, null);
        dynamicViewEntity.addViewLink("PFAC", "PFC", Boolean.FALSE, ModelKeyMap.makeKeyMapList("productFeatureId"));
        entityConditionList.add(EntityCondition.makeCondition("pfcProductFeatureTypeId", EntityOperator.EQUALS, productFeatureTypeId));

        List<Map<String, String>> featureCountList = null;
        EntityQuery eq = EntityQuery.use(delegator)
                .select(UtilMisc.toSet("pfacProductFeatureId", "featureCount", "pfcDescription", "pfcProductFeatureTypeId"))
                .from(dynamicViewEntity)
                .where(entityConditionList)
                .orderBy(productSearchContext.orderByList)
                .cursorScrollInsensitive();

        try (EntityListIterator eli = eq.queryIterator()) {
            featureCountList = new LinkedList<>();
            GenericValue searchResult = null;
            while ((searchResult = eli.next()) != null) {
                featureCountList.add(UtilMisc.<String, String>toMap("productFeatureId", (String) searchResult.get("pfacProductFeatureId"), "productFeatureTypeId", (String) searchResult.get("pfcProductFeatureTypeId"), "description", (String) searchResult.get("pfcDescription"), "featureCount", Long.toString((Long) searchResult.get("featureCount"))));
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, "Error in product search", module);
            return null;
        }

        return featureCountList;
    }

    public static int getCategoryCostraintIndex(HttpSession session) {
        int index = 0;
        List<ProductSearchConstraint> productSearchConstraintList = ProductSearchOptions.getConstraintList(session);
        for (ProductSearchConstraint constraint: productSearchConstraintList) {
            if (constraint instanceof CategoryConstraint) {
                index++;
            }
        }
        return index;
    }

    /**
     * This method returns count of products within a given price range, the constraint being
     * applied on current ProductSearchConstraint list in session.
     * @param priceLow The low price.
     * @param priceHigh The high price.
     * @param session Current session.
     * @param delegator The delegator object.
     * @return The long value of count of products.
     */
    public static long getCountForListPriceRange(BigDecimal priceLow, BigDecimal priceHigh, HttpSession session, Delegator delegator) {
        String visitId = VisitHandler.getVisitId(session);

        ProductSearchContext productSearchContext = new ProductSearchContext(delegator, visitId);
        List<ProductSearchConstraint> productSearchConstraintList = ProductSearchOptions.getConstraintList(session);
        if (UtilValidate.isNotEmpty(productSearchConstraintList)) {
            productSearchContext.addProductSearchConstraints(productSearchConstraintList);
        }
        productSearchContext.finishKeywordConstraints();
        productSearchContext.finishCategoryAndFeatureConstraints();

        DynamicViewEntity dynamicViewEntity = productSearchContext.dynamicViewEntity;
        List<EntityCondition> entityConditionList = productSearchContext.entityConditionList;
        List<String> fieldsToSelect = new LinkedList<>();

        dynamicViewEntity.addMemberEntity("PPC", "ProductPrice");
        dynamicViewEntity.addAlias("PPC", "ppcProductPriceTypeId", "productPriceTypeId", null, null, null, null);
        dynamicViewEntity.addAlias("PPC", "ppcFromDate", "fromDate", null, null, null, null);
        dynamicViewEntity.addAlias("PPC", "ppcThruDate", "thruDate", null, null, null, null);
        dynamicViewEntity.addAlias("PPC", "ppcPrice", "price", null, null, null, null);
        dynamicViewEntity.addAlias("PPC", "priceRangeCount", "productId", null, null, null, "count-distinct");
        dynamicViewEntity.addViewLink("PROD", "PPC", Boolean.FALSE, ModelKeyMap.makeKeyMapList("productId"));
        fieldsToSelect.add("priceRangeCount");
        entityConditionList.add(EntityCondition.makeCondition(EntityCondition.makeCondition("ppcThruDate", EntityOperator.EQUALS, null), EntityOperator.OR, EntityCondition.makeCondition("ppcThruDate", EntityOperator.GREATER_THAN, UtilDateTime.nowTimestamp())));
        entityConditionList.add(EntityCondition.makeCondition("ppcFromDate", EntityOperator.LESS_THAN, UtilDateTime.nowTimestamp()));
        entityConditionList.add(EntityCondition.makeCondition("ppcPrice", EntityOperator.GREATER_THAN_EQUAL_TO, priceLow));
        entityConditionList.add(EntityCondition.makeCondition("ppcPrice", EntityOperator.LESS_THAN_EQUAL_TO, priceHigh));
        entityConditionList.add(EntityCondition.makeCondition("ppcProductPriceTypeId", EntityOperator.EQUALS, "LIST_PRICE"));

        Long priceRangeCount = 0L;
        EntityQuery eq = EntityQuery.use(delegator)
                .select(UtilMisc.toSet(fieldsToSelect))
                .from(dynamicViewEntity)
                .where(entityConditionList)
                .orderBy(productSearchContext.orderByList)
                .cursorScrollInsensitive();

        try (EntityListIterator eli = eq.queryIterator()) {
            GenericValue searchResult = null;
            while ((searchResult = eli.next()) != null) {
                priceRangeCount = searchResult.getLong("priceRangeCount");
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, "Error in product search", module);
        }
        return priceRangeCount;
    }

    /**
     * This method returns count of products in a given category (including all sub categories), the constraint being
     * applied on current ProductSearchConstraint list in session.
     * @param productCategoryId productCategoryId for which the count should be returned.
     * @param session Current session.
     * @param delegator The delegator object.
     * @return The long value of count of products.
     */
    public static long getCountForProductCategory(String productCategoryId, HttpSession session, Delegator delegator) {
        String visitId = VisitHandler.getVisitId(session);

        ProductSearchContext productSearchContext = new ProductSearchContext(delegator, visitId);
        List<ProductSearchConstraint> productSearchConstraintList = ProductSearchOptions.getConstraintList(session);
        if (UtilValidate.isNotEmpty(productSearchConstraintList)) {
            productSearchContext.addProductSearchConstraints(productSearchConstraintList);
        }
        productSearchContext.finishKeywordConstraints();
        productSearchContext.finishCategoryAndFeatureConstraints();

        DynamicViewEntity dynamicViewEntity = productSearchContext.dynamicViewEntity;
        List<EntityCondition> entityConditionList = productSearchContext.entityConditionList;
        List<String> fieldsToSelect = new LinkedList<>();

        dynamicViewEntity.addMemberEntity("PCMC", "ProductCategoryMember");
        dynamicViewEntity.addAlias("PCMC", "pcmcProductCategoryId", "productCategoryId", null, null, null, null);
        dynamicViewEntity.addAlias("PCMC", "pcmcFromDate", "fromDate", null, null, null, null);
        dynamicViewEntity.addAlias("PCMC", "pcmcThruDate", "thruDate", null, null, null, null);
        dynamicViewEntity.addAlias("PCMC", "categoryCount", "productId", null, null, null, "count-distinct");
        dynamicViewEntity.addViewLink("PROD", "PCMC", Boolean.FALSE, ModelKeyMap.makeKeyMapList("productId"));
        fieldsToSelect.add("categoryCount");
        entityConditionList.add(EntityCondition.makeCondition(EntityCondition.makeCondition("pcmcThruDate", EntityOperator.EQUALS, null), EntityOperator.OR, EntityCondition.makeCondition("pcmcThruDate", EntityOperator.GREATER_THAN, productSearchContext.nowTimestamp)));
        entityConditionList.add(EntityCondition.makeCondition("pcmcFromDate", EntityOperator.LESS_THAN, productSearchContext.nowTimestamp));

        Set<String> productCategoryIdSet = new HashSet<>();
        ProductSearch.getAllSubCategoryIds(productCategoryId, productCategoryIdSet, delegator, productSearchContext.nowTimestamp);
        entityConditionList.add(EntityCondition.makeCondition("pcmcProductCategoryId", EntityOperator.IN, productCategoryIdSet));

        Long categoryCount = 0L;
        EntityQuery eq = EntityQuery.use(delegator)
                .select(UtilMisc.toSet(fieldsToSelect))
                .from(dynamicViewEntity)
                .where(entityConditionList)
                .orderBy(productSearchContext.orderByList)
                .cursorScrollInsensitive();

        try (EntityListIterator eli = eq.queryIterator()) {
            GenericValue searchResult = null;
            while ((searchResult = eli.next()) != null) {
                categoryCount = searchResult.getLong("categoryCount");
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, "Error in product search", module);
        }
        return categoryCount;
    }
}
