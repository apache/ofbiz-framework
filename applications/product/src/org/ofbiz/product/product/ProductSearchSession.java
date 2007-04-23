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
package org.ofbiz.product.product;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.sql.Timestamp;


import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import javolution.util.FastList;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.webapp.stats.VisitHandler;
import org.ofbiz.webapp.control.RequestHandler;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityConditionList;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityFindOptions;
import org.ofbiz.entity.util.EntityListIterator;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.product.catalog.CatalogWorker;
import org.ofbiz.product.category.CategoryWorker;
import org.ofbiz.product.feature.ParametricSearch;
import org.ofbiz.product.product.ProductSearch.CategoryConstraint;
import org.ofbiz.product.product.ProductSearch.FeatureConstraint;
import org.ofbiz.product.product.ProductSearch.KeywordConstraint;
import org.ofbiz.product.product.ProductSearch.ProductSearchConstraint;
import org.ofbiz.product.product.ProductSearch.ProductSearchContext;
import org.ofbiz.product.product.ProductSearch.ResultSortOrder;
import org.ofbiz.product.product.ProductSearch.SortKeywordRelevancy;
import org.ofbiz.product.store.ProductStoreWorker;

/**
 *  Utility class with methods to prepare and perform ProductSearch operations in the content of an HttpSession
 */
public class ProductSearchSession {

    public static final String module = ProductSearchSession.class.getName();

    public static class ProductSearchOptions implements java.io.Serializable {
        protected List constraintList = null;
        protected String topProductCategoryId = null;
        protected ResultSortOrder resultSortOrder = null;
        protected Integer viewIndex = null;
        protected Integer viewSize = null;
        protected boolean changed = false;

        public ProductSearchOptions() { }

        /** Basic copy constructor */
        public ProductSearchOptions(ProductSearchOptions productSearchOptions) {
            this.constraintList = FastList.newInstance();
            this.constraintList.addAll(productSearchOptions.constraintList);
            this.topProductCategoryId = productSearchOptions.topProductCategoryId;
            this.resultSortOrder = productSearchOptions.resultSortOrder;
            this.viewIndex = productSearchOptions.viewIndex;
            this.viewSize = productSearchOptions.viewSize;
            this.changed = productSearchOptions.changed;
        }

        public List getConstraintList() {
            return this.constraintList;
        }
        public static List getConstraintList(HttpSession session) {
            return getProductSearchOptions(session).constraintList;
        }
        public static void addConstraint(ProductSearchConstraint productSearchConstraint, HttpSession session) {
            ProductSearchOptions productSearchOptions = getProductSearchOptions(session);
            if (productSearchOptions.constraintList == null) {
                productSearchOptions.constraintList = FastList.newInstance();
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
         * @param viewIndex The viewIndex to set.
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
                    this.setViewIndex(new Integer(20));
                }
            }
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
        /**
         * @param viewSize The viewSize to set.
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
                    this.setViewSize(new Integer(20));
                }
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

        public List searchGetConstraintStrings(boolean detailed, GenericDelegator delegator, Locale locale) {
            List productSearchConstraintList = this.getConstraintList();
            List constraintStrings = FastList.newInstance();
            if (productSearchConstraintList == null) {
                return constraintStrings;
            }
            Iterator productSearchConstraintIter = productSearchConstraintList.iterator();
            while (productSearchConstraintIter.hasNext()) {
                ProductSearchConstraint productSearchConstraint = (ProductSearchConstraint) productSearchConstraintIter.next();
                if (productSearchConstraint == null) continue;
                String constraintString = productSearchConstraint.prettyPrintConstraint(delegator, detailed, locale);
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
            List optionsHistoryList = getSearchOptionsHistoryList(session); 
            optionsHistoryList.add(0, new ProductSearchOptions(productSearchOptions));
            productSearchOptions.changed = false;
        }
    }
    public static List getSearchOptionsHistoryList(HttpSession session) {
        List optionsHistoryList = (List) session.getAttribute("_PRODUCT_SEARCH_OPTIONS_HISTORY_"); 
        if (optionsHistoryList == null) {
            optionsHistoryList = FastList.newInstance();
            session.setAttribute("_PRODUCT_SEARCH_OPTIONS_HISTORY_", optionsHistoryList);
        }
        return optionsHistoryList;
    }
    public static void clearSearchOptionsHistoryList(HttpSession session) {
        session.removeAttribute("_PRODUCT_SEARCH_OPTIONS_HISTORY_");
    }
    
    public static void setCurrentSearchFromHistory(int index, boolean removeOld, HttpSession session) {
        List searchOptionsHistoryList = getSearchOptionsHistoryList(session);
        if (index < searchOptionsHistoryList.size()) {
            ProductSearchOptions productSearchOptions = (ProductSearchOptions) searchOptionsHistoryList.get(index);
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
        GenericDelegator delegator = (GenericDelegator) request.getAttribute("delegator");
        Map requestParams = UtilHttp.getParameterMap(request);
        ProductSearchSession.processSearchParameters(requestParams, request);

        // get the current productStoreId
        String productStoreId = ProductStoreWorker.getProductStoreId(request);
        if (productStoreId != null) {
            // get a Set of all keywords in the search, if there are any...
            Set keywords = new HashSet();
            List constraintList = ProductSearchOptions.getConstraintList(session);
            if (constraintList != null) {
                Iterator constraintIter = constraintList.iterator();
                while (constraintIter.hasNext()) {
                    Object constraint = constraintIter.next();
                    if (constraint instanceof KeywordConstraint) {
                        KeywordConstraint keywordConstraint = (KeywordConstraint) constraint;
                        Set keywordSet = keywordConstraint.makeFullKeywordSet(delegator);
                        if (keywordSet != null) keywords.addAll(keywordSet);
                    }
                }
            }

            if (keywords.size() > 0) {
                List productStoreKeywordOvrdList = null;
                try {
                    productStoreKeywordOvrdList = delegator.findByAndCache("ProductStoreKeywordOvrd", UtilMisc.toMap("productStoreId", productStoreId), UtilMisc.toList("-fromDate"));
                    productStoreKeywordOvrdList = EntityUtil.filterByDate(productStoreKeywordOvrdList, true);
                } catch (GenericEntityException e) {
                    Debug.logError(e, "Error reading ProductStoreKeywordOvrd list, not doing keyword override", module);
                }

                if (productStoreKeywordOvrdList != null && productStoreKeywordOvrdList.size() > 0) {
                    Iterator productStoreKeywordOvrdIter = productStoreKeywordOvrdList.iterator();
                    while (productStoreKeywordOvrdIter.hasNext()) {
                        GenericValue productStoreKeywordOvrd = (GenericValue) productStoreKeywordOvrdIter.next();
                        String ovrdKeyword = productStoreKeywordOvrd.getString("keyword");
                        if (keywords.contains(ovrdKeyword)) {
                            String targetTypeEnumId = productStoreKeywordOvrd.getString("targetTypeEnumId");
                            String target = productStoreKeywordOvrd.getString("target");
                            ServletContext ctx = (ServletContext) request.getAttribute("servletContext");
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

    public static ArrayList searchDo(HttpSession session, GenericDelegator delegator, String prodCatalogId) {
        String visitId = VisitHandler.getVisitId(session);
        ProductSearchOptions productSearchOptions = getProductSearchOptions(session);
        List productSearchConstraintList = productSearchOptions.getConstraintList();
        if (productSearchConstraintList == null || productSearchConstraintList.size() == 0) {
            // no constraints, don't do a search...
            return new ArrayList();
        }

        ResultSortOrder resultSortOrder = productSearchOptions.getResultSortOrder();

        // if the search options have changed since the last search, put at the beginning of the options history list
        checkSaveSearchOptionsHistory(session);
        
        return ProductSearch.searchProducts(productSearchConstraintList, resultSortOrder, delegator, visitId);
    }

    public static void searchClear(HttpSession session) {
        ProductSearchOptions.clearSearchOptions(session);
    }
    
    public static List searchGetConstraintStrings(boolean detailed, HttpSession session, GenericDelegator delegator) {
        Locale locale = UtilHttp.getLocale(session);
        ProductSearchOptions productSearchOptions = getProductSearchOptions(session);
        return productSearchOptions.searchGetConstraintStrings(detailed, delegator, locale);
    }

    public static String searchGetSortOrderString(boolean detailed, HttpServletRequest request) {
        Locale locale = UtilHttp.getLocale(request);
        ResultSortOrder resultSortOrder = ProductSearchOptions.getResultSortOrder(request);
        if (resultSortOrder == null) return "";
        return resultSortOrder.prettyPrintSortOrder(detailed, locale);
    }

    public static void searchSetSortOrder(ResultSortOrder resultSortOrder, HttpSession session) {
        ProductSearchOptions.setResultSortOrder(resultSortOrder, session);
    }

    public static void searchAddFeatureIdConstraints(Collection featureIds, Boolean exclude, HttpServletRequest request) {
        HttpSession session = request.getSession();
        if (featureIds == null || featureIds.size() == 0) {
            return;
        }
        Iterator featureIdIter = featureIds.iterator();
        while (featureIdIter.hasNext()) {
            String productFeatureId = (String) featureIdIter.next();
            searchAddConstraint(new FeatureConstraint(productFeatureId, exclude), session);
        }
    }

    public static void searchAddConstraint(ProductSearchConstraint productSearchConstraint, HttpSession session) {
        ProductSearchOptions.addConstraint(productSearchConstraint, session);
    }

    public static void searchRemoveConstraint(int index, HttpSession session) {
        List productSearchConstraintList = ProductSearchOptions.getConstraintList(session);
        if (productSearchConstraintList == null) {
            return;
        } else if (index >= productSearchConstraintList.size()) {
            return;
        } else {
            productSearchConstraintList.remove(index);
        }
    }

    public static void processSearchParameters(Map parameters, HttpServletRequest request) {
        GenericDelegator delegator = (GenericDelegator) request.getAttribute("delegator");
        Boolean alreadyRun = (Boolean) request.getAttribute("processSearchParametersAlreadyRun"); 
        if (Boolean.TRUE.equals(alreadyRun)) {
            // even if already run, check the VIEW_SIZE and VIEW_INDEX again, just for kicks
            ProductSearchOptions productSearchOptions = getProductSearchOptions(request.getSession());
            productSearchOptions.setViewIndex((String) parameters.get("VIEW_INDEX"));
            productSearchOptions.setViewSize((String) parameters.get("VIEW_SIZE"));
            
            return;
        } else {
            request.setAttribute("processSearchParametersAlreadyRun", Boolean.TRUE);
        }
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
        if (UtilValidate.isNotEmpty((String) parameters.get("PRIORITIZE_CATEGORY_ID"))) {
            prioritizeCategoryId = (String) parameters.get("PRIORITIZE_CATEGORY_ID");
        } else if (UtilValidate.isNotEmpty((String) parameters.get("S_TPC"))) {
            prioritizeCategoryId = (String) parameters.get("S_TPC");
        }
        if (UtilValidate.isNotEmpty(prioritizeCategoryId)) {
            ProductSearchOptions.setTopProductCategoryId(prioritizeCategoryId, session);
            constraintsChanged = true;
        }

        // if there is another category, add a constraint for it
        if (UtilValidate.isNotEmpty((String) parameters.get("SEARCH_CATEGORY_ID"))) {
            String searchCategoryId = (String) parameters.get("SEARCH_CATEGORY_ID");
            String searchSubCategories = (String) parameters.get("SEARCH_SUB_CATEGORIES");
            String searchCategoryExc = (String) parameters.get("SEARCH_CATEGORY_EXC");
            Boolean exclude = UtilValidate.isEmpty(searchCategoryExc) ? null : new Boolean(!"N".equals(searchCategoryExc));
            searchAddConstraint(new ProductSearch.CategoryConstraint(searchCategoryId, !"N".equals(searchSubCategories), exclude), session);
            constraintsChanged = true;
        }
        
        for (int catNum = 1; catNum < 10; catNum++) {
            if (UtilValidate.isNotEmpty((String) parameters.get("SEARCH_CATEGORY_ID" + catNum))) {
                String searchCategoryId = (String) parameters.get("SEARCH_CATEGORY_ID" + catNum);
                String searchSubCategories = (String) parameters.get("SEARCH_SUB_CATEGORIES" + catNum);
                String searchCategoryExc = (String) parameters.get("SEARCH_CATEGORY_EXC" + catNum);
                Boolean exclude = UtilValidate.isEmpty(searchCategoryExc) ? null : new Boolean(!"N".equals(searchCategoryExc));
                searchAddConstraint(new ProductSearch.CategoryConstraint(searchCategoryId, !"N".equals(searchSubCategories), exclude), session);
                constraintsChanged = true;
            }
        }

        // a shorter variation for categories
        for (int catNum = 1; catNum < 10; catNum++) {
            if (UtilValidate.isNotEmpty((String) parameters.get("S_CAT" + catNum))) {
                String searchCategoryId = (String) parameters.get("S_CAT" + catNum);
                String searchSubCategories = (String) parameters.get("S_CSB" + catNum);
                String searchCategoryExc = (String) parameters.get("S_CEX" + catNum);
                Boolean exclude = UtilValidate.isEmpty(searchCategoryExc) ? null : new Boolean(!"N".equals(searchCategoryExc));
                searchAddConstraint(new ProductSearch.CategoryConstraint(searchCategoryId, !"N".equals(searchSubCategories), exclude), session);
                constraintsChanged = true;
            }
        }

        // if there is any category selected try to use catalog and add a constraint for it
        if (UtilValidate.isNotEmpty((String) parameters.get("SEARCH_CATALOG_ID"))) {    
            String searchCatalogId = (String) parameters.get("SEARCH_CATALOG_ID");
            if (searchCatalogId != null && !searchCatalogId.equalsIgnoreCase("")) {
                List categories = CategoryWorker.getRelatedCategoriesRet(request, "topLevelList", CatalogWorker.getCatalogTopCategoryId(request, searchCatalogId), true);
                searchAddConstraint(new ProductSearch.CatalogConstraint(searchCatalogId, categories), session);
                constraintsChanged = true;              
            }
        } 
        
        // if keywords were specified, add a constraint for them
        if (UtilValidate.isNotEmpty((String) parameters.get("SEARCH_STRING"))) {
            String keywordString = (String) parameters.get("SEARCH_STRING");
            String searchOperator = (String) parameters.get("SEARCH_OPERATOR");
            // defaults to true/Y, ie anything but N is true/Y
            boolean anyPrefixSuffix = !"N".equals((String) parameters.get("SEARCH_ANYPRESUF"));
            searchAddConstraint(new ProductSearch.KeywordConstraint(keywordString, anyPrefixSuffix, anyPrefixSuffix, null, "AND".equals(searchOperator)), session);
            constraintsChanged = true;
        }

        for (int kwNum = 1; kwNum < 10; kwNum++) {
            if (UtilValidate.isNotEmpty((String) parameters.get("SEARCH_STRING" + kwNum))) {
                String keywordString = (String) parameters.get("SEARCH_STRING" + kwNum);
                String searchOperator = (String) parameters.get("SEARCH_OPERATOR" + kwNum);
                // defaults to true/Y, ie anything but N is true/Y
                boolean anyPrefixSuffix = !"N".equals((String) parameters.get("SEARCH_ANYPRESUF" + kwNum));
                searchAddConstraint(new ProductSearch.KeywordConstraint(keywordString, anyPrefixSuffix, anyPrefixSuffix, null, "AND".equals(searchOperator)), session);
                constraintsChanged = true;
            }
        }

        Iterator parameterNameIter = parameters.keySet().iterator();
        while (parameterNameIter.hasNext()) {
            String parameterName = (String) parameterNameIter.next();
            if (parameterName.startsWith("SEARCH_FEAT") && !parameterName.startsWith("SEARCH_FEAT_EXC")) {
                String productFeatureId = (String) parameters.get(parameterName);
                if (productFeatureId != null && productFeatureId.length() > 0) {
                    String paramNameExt = parameterName.substring("SEARCH_FEAT".length());
                    String searchCategoryExc = (String) parameters.get("SEARCH_FEAT_EXC" + paramNameExt);
                    Boolean exclude = UtilValidate.isEmpty(searchCategoryExc) ? null : new Boolean(!"N".equals(searchCategoryExc));
                    //Debug.logInfo("parameterName=" + parameterName + ", paramNameExt=" + paramNameExt + ", searchCategoryExc=" + searchCategoryExc + ", exclude=" + exclude, module);
                    searchAddConstraint(new ProductSearch.FeatureConstraint(productFeatureId, exclude), session);
                    constraintsChanged = true;
                }
            }
            // a shorter feature variation
            if (parameterName.startsWith("S_PFI")) {
                String productFeatureId = (String) parameters.get(parameterName);
                if (productFeatureId != null && productFeatureId.length() > 0) {
                    String paramNameExt = parameterName.substring("S_PFI".length());
                    String searchCategoryExc = (String) parameters.get("S_PFX" + paramNameExt);
                    Boolean exclude = UtilValidate.isEmpty(searchCategoryExc) ? null : new Boolean(!"N".equals(searchCategoryExc));
                    //Debug.logInfo("parameterName=" + parameterName + ", paramNameExt=" + paramNameExt + ", searchCategoryExc=" + searchCategoryExc + ", exclude=" + exclude, module);
                    searchAddConstraint(new ProductSearch.FeatureConstraint(productFeatureId, exclude), session);
                    constraintsChanged = true;
                }
            }

            //if product features category were selected add a constraint for each
            if (parameterName.startsWith("SEARCH_PROD_FEAT_CAT") && !parameterName.startsWith("SEARCH_PROD_FEAT_CAT_EXC")) {
                String productFeatureCategoryId = (String) parameters.get(parameterName);
                if (productFeatureCategoryId != null && productFeatureCategoryId.length() > 0) {
                    String paramNameExt = parameterName.substring("SEARCH_PROD_FEAT_CAT".length());
                    String searchProdFeatureCategoryExc = (String) parameters.get("SEARCH_PROD_FEAT_CAT_EXC" + paramNameExt);
                    Boolean exclude = UtilValidate.isEmpty(searchProdFeatureCategoryExc) ? null : new Boolean(!"N".equals(searchProdFeatureCategoryExc));
                    searchAddConstraint(new ProductSearch.FeatureCategoryConstraint(productFeatureCategoryId, exclude), session);
                    constraintsChanged = true;
                }
            }
            // a shorter variation for feature category
            if (parameterName.startsWith("S_FCI")) {
                String productFeatureCategoryId = (String) parameters.get(parameterName);
                if (productFeatureCategoryId != null && productFeatureCategoryId.length() > 0) {
                    String paramNameExt = parameterName.substring("S_FCI".length());
                    String searchProdFeatureCategoryExc = (String) parameters.get("S_FCX" + paramNameExt);
                    Boolean exclude = UtilValidate.isEmpty(searchProdFeatureCategoryExc) ? null : new Boolean(!"N".equals(searchProdFeatureCategoryExc));
                    searchAddConstraint(new ProductSearch.FeatureCategoryConstraint(productFeatureCategoryId, exclude), session);
                    constraintsChanged = true;
                }
            }

            //if product features group were selected add a constraint for each
            if (parameterName.startsWith("SEARCH_PROD_FEAT_GRP") && !parameterName.startsWith("SEARCH_PROD_FEAT_GRP_EXC")) {
                String productFeatureGroupId = (String) parameters.get(parameterName);
                if (productFeatureGroupId != null && productFeatureGroupId.length() > 0) {
                    String paramNameExt = parameterName.substring("SEARCH_PROD_FEAT_GRP".length());
                    String searchProdFeatureGroupExc = (String) parameters.get("SEARCH_PROD_FEAT_GRP_EXC" + paramNameExt);
                    Boolean exclude = UtilValidate.isEmpty(searchProdFeatureGroupExc) ? null : new Boolean(!"N".equals(searchProdFeatureGroupExc));
                    searchAddConstraint(new ProductSearch.FeatureGroupConstraint(productFeatureGroupId, exclude), session);
                    constraintsChanged = true;
                }
            }
            // a shorter variation for feature group
            if (parameterName.startsWith("S_FGI")) {
                String productFeatureGroupId = (String) parameters.get(parameterName);
                if (productFeatureGroupId != null && productFeatureGroupId.length() > 0) {
                    String paramNameExt = parameterName.substring("S_FGI".length());
                    String searchProdFeatureGroupExc = (String) parameters.get("S_FGX" + paramNameExt);
                    Boolean exclude = UtilValidate.isEmpty(searchProdFeatureGroupExc) ? null : new Boolean(!"N".equals(searchProdFeatureGroupExc));
                    searchAddConstraint(new ProductSearch.FeatureGroupConstraint(productFeatureGroupId, exclude), session);
                    constraintsChanged = true;
                }
            }
        }

        // if features were selected add a constraint for each
        Map featureIdByType = ParametricSearch.makeFeatureIdByTypeMap(parameters);
        if (featureIdByType.size() > 0) {
            constraintsChanged = true;
            searchAddFeatureIdConstraints(featureIdByType.values(), null, request);
        }

        // add a supplier to the search
        if (UtilValidate.isNotEmpty((String) parameters.get("SEARCH_SUPPLIER_ID")) || UtilValidate.isNotEmpty((String) parameters.get("S_SUP"))) {
            String supplierPartyId = (String) parameters.get("SEARCH_SUPPLIER_ID");
            if (UtilValidate.isEmpty(supplierPartyId)) supplierPartyId = (String) parameters.get("S_SUP");
            searchAddConstraint(new ProductSearch.SupplierConstraint(supplierPartyId), session);
            constraintsChanged = true;
        }
        
        // add a list price range to the search
        if (UtilValidate.isNotEmpty((String) parameters.get("LIST_PRICE_LOW")) || UtilValidate.isNotEmpty((String) parameters.get("LIST_PRICE_HIGH"))) {
            Double listPriceLow = null;
            Double listPriceHigh = null;
            String listPriceCurrency = UtilHttp.getCurrencyUom(request);
            if (UtilValidate.isNotEmpty((String) parameters.get("LIST_PRICE_LOW"))) {
                try {
                    listPriceLow = Double.valueOf((String) parameters.get("LIST_PRICE_LOW"));
                } catch (NumberFormatException e) {
                    Debug.logError("Error parsing LIST_PRICE_LOW parameter [" + (String) parameters.get("LIST_PRICE_LOW") + "]: " + e.toString(), module);
                }
            }
            if (UtilValidate.isNotEmpty((String) parameters.get("LIST_PRICE_HIGH"))) {
                try {
                    listPriceHigh = Double.valueOf((String) parameters.get("LIST_PRICE_HIGH"));
                } catch (NumberFormatException e) {
                    Debug.logError("Error parsing LIST_PRICE_HIGH parameter [" + (String) parameters.get("LIST_PRICE_HIGH") + "]: " + e.toString(), module);
                }
            }
            searchAddConstraint(new ProductSearch.ListPriceRangeConstraint(listPriceLow, listPriceHigh, listPriceCurrency), session);
            constraintsChanged = true;
        }
        if (UtilValidate.isNotEmpty((String) parameters.get("LIST_PRICE_RANGE")) || UtilValidate.isNotEmpty((String) parameters.get("S_LPR"))) {
            String listPriceRangeStr = (String) parameters.get("LIST_PRICE_RANGE");
            if (UtilValidate.isEmpty(listPriceRangeStr)) listPriceRangeStr = (String) parameters.get("S_LPR");
            String listPriceLowStr = listPriceRangeStr.substring(0, listPriceRangeStr.indexOf("_")); 
            String listPriceHighStr = listPriceRangeStr.substring(listPriceRangeStr.indexOf("_") + 1); 

            Double listPriceLow = null;
            Double listPriceHigh = null;
            String listPriceCurrency = UtilHttp.getCurrencyUom(request);
            if (UtilValidate.isNotEmpty(listPriceLowStr)) {
                try {
                    listPriceLow = Double.valueOf(listPriceLowStr);
                } catch (NumberFormatException e) {
                    Debug.logError("Error parsing low part of LIST_PRICE_RANGE parameter [" + listPriceLowStr + "]: " + e.toString(), module);
                }
            }
            if (UtilValidate.isNotEmpty(listPriceHighStr)) {
                try {
                    listPriceHigh = Double.valueOf(listPriceHighStr);
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

        String prodCatalogId = CatalogWorker.getCurrentCatalogId(request);
        String viewProductCategoryId = CatalogWorker.getCatalogViewAllowCategoryId(delegator, prodCatalogId);
        if (UtilValidate.isNotEmpty(viewProductCategoryId)) {
            ProductSearchConstraint viewAllowConstraint = new CategoryConstraint(viewProductCategoryId, true, null);
            searchAddConstraint(viewAllowConstraint, session);
            // not consider this a change for now, shouldn't change often: constraintsChanged = true;
        }
        
        // set the sort order
        String sortOrder = (String) parameters.get("sortOrder");
        if (UtilValidate.isEmpty(sortOrder)) sortOrder = (String) parameters.get("S_O");
        String sortAscending = (String) parameters.get("sortAscending");
        if (UtilValidate.isEmpty(sortAscending)) sortAscending = (String) parameters.get("S_A");
        boolean ascending = !"N".equals(sortAscending);
        if (sortOrder != null) {
            if (sortOrder.equals("SortKeywordRelevancy") || sortOrder.equals("SKR")) {
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
            }
        }
        
        ProductSearchOptions productSearchOptions = getProductSearchOptions(session);
        if (constraintsChanged) {
            // query changed, clear out the VIEW_INDEX & VIEW_SIZE
            productSearchOptions.clearViewInfo();
        }

        productSearchOptions.setViewIndex((String) parameters.get("VIEW_INDEX"));
        productSearchOptions.setViewSize((String) parameters.get("VIEW_SIZE"));
    }

    public static Map getProductSearchResult(HttpServletRequest request, GenericDelegator delegator, String prodCatalogId) {

        // ========== Create View Indexes
        int viewIndex = 0;
        int viewSize = 20;
        int highIndex = 0;
        int lowIndex = 0;
        int listSize = 0;

        HttpSession session = request.getSession();
        ProductSearchOptions productSearchOptions = getProductSearchOptions(session);
        
        String addOnTopProdCategoryId = productSearchOptions.getTopProductCategoryId();
        Integer viewIndexInteger = productSearchOptions.getViewIndex();
        if (viewIndexInteger != null) viewIndex = viewIndexInteger.intValue();
        Integer viewSizeInteger = productSearchOptions.getViewSize();
        if (viewSizeInteger != null) viewSize = viewSizeInteger.intValue();

        lowIndex = viewIndex * viewSize;
        highIndex = (viewIndex + 1) * viewSize;

        // ========== Do the actual search
        List productIds = FastList.newInstance();
        String visitId = VisitHandler.getVisitId(session);
        List productSearchConstraintList = ProductSearchOptions.getConstraintList(session);
        // if no constraints, don't do a search...
        if (productSearchConstraintList != null && productSearchConstraintList.size() > 0) {
            // if the search options have changed since the last search, put at the beginning of the options history list
            checkSaveSearchOptionsHistory(session);

            int addOnTopTotalListSize = 0;
            int addOnTopListSize = 0;
            List addOnTopProductCategoryMembers = FastList.newInstance();
            if (UtilValidate.isNotEmpty(addOnTopProdCategoryId)) {
                // always include the members of the addOnTopProdCategoryId
                Timestamp now = UtilDateTime.nowTimestamp();
                List addOnTopProdCondList = FastList.newInstance();
                addOnTopProdCondList.add(new EntityExpr(new EntityExpr("thruDate", EntityOperator.EQUALS, null), EntityOperator.OR, new EntityExpr("thruDate", EntityOperator.GREATER_THAN, now)));
                addOnTopProdCondList.add(new EntityExpr("fromDate", EntityOperator.LESS_THAN, now));
                addOnTopProdCondList.add(new EntityExpr("productCategoryId", EntityOperator.EQUALS, addOnTopProdCategoryId));
                EntityFindOptions findOpts = new EntityFindOptions(true, EntityFindOptions.TYPE_SCROLL_INSENSITIVE, EntityFindOptions.CONCUR_READ_ONLY, true);
                EntityListIterator pli = null;
                try {
                    pli = delegator.findListIteratorByCondition("ProductCategoryMember", new EntityConditionList(addOnTopProdCondList, EntityOperator.AND), null, UtilMisc.toList("productId"), UtilMisc.toList("sequenceNum"), findOpts);
                    addOnTopProductCategoryMembers = pli.getPartialList(lowIndex, viewSize);
                    addOnTopListSize = addOnTopProductCategoryMembers.size();
                    for (int i = 0; i < addOnTopProductCategoryMembers.size(); i++) {
                        GenericValue alwaysAddProductCategoryMember = (GenericValue)addOnTopProductCategoryMembers.get(i);
                        productIds.add(alwaysAddProductCategoryMember.getString("productId"));
                    }
                    // attempt to get the full size
                    pli.last();
                    addOnTopTotalListSize = pli.currentIndex();
                    listSize = listSize + addOnTopTotalListSize;
                } catch (GenericEntityException e) {
                    Debug.logError(e, module);
                } finally {
                    if (pli != null) {
                        try {
                            pli.close();
                        } catch (GenericEntityException e) {
                            Debug.logError(e, module);
                        }
                    }
                }
            }

            // setup resultOffset and maxResults, noting that resultOffset is 1 based, not zero based as these numbers
            int resultOffsetInt = lowIndex - addOnTopTotalListSize + 1;
            if (resultOffsetInt < 1) {
                resultOffsetInt = 1;
            }
            int maxResultsInt = viewSize - addOnTopListSize;
            Integer resultOffset = new Integer(resultOffsetInt);
            Integer maxResults = new Integer(maxResultsInt);

            ResultSortOrder resultSortOrder = ProductSearchOptions.getResultSortOrder(request);

            ProductSearchContext productSearchContext = new ProductSearchContext(delegator, visitId);
            productSearchContext.addProductSearchConstraints(productSearchConstraintList);
            productSearchContext.setResultSortOrder(resultSortOrder);
            productSearchContext.setResultOffset(resultOffset);
            productSearchContext.setMaxResults(maxResults);

            List foundProductIds = productSearchContext.doSearch();
            if (maxResultsInt > 0) {
                productIds.addAll(foundProductIds);
            }

            Integer totalResults = productSearchContext.getTotalResults();
            if (totalResults != null) {
                listSize = listSize + totalResults.intValue();
            }
        }

        if (listSize < highIndex) {
            highIndex = listSize;
        }

        // ========== Setup other display info
        List searchConstraintStrings = searchGetConstraintStrings(false, session, delegator);
        String searchSortOrderString = searchGetSortOrderString(false, request);

        // ========== populate the result Map
        Map result = new HashMap();

        result.put("productIds", productIds);
        result.put("viewIndex", new Integer(viewIndex));
        result.put("viewSize", new Integer(viewSize));
        result.put("listSize", new Integer(listSize));
        result.put("lowIndex", new Integer(lowIndex));
        result.put("highIndex", new Integer(highIndex));
        result.put("searchConstraintStrings", searchConstraintStrings);
        result.put("searchSortOrderString", searchSortOrderString);

        return result;
    }
    
    public static String makeSearchParametersString(HttpSession session) {
        return makeSearchParametersString(getProductSearchOptions(session));
    }
    public static String makeSearchParametersString(ProductSearchOptions productSearchOptions) {
        StringBuffer searchParamString = new StringBuffer();

        List constraintList = productSearchOptions.getConstraintList();
        Iterator constraintIter = constraintList.iterator();
        int categoriesCount = 0;
        int featuresCount = 0;
        int featureCategoriesCount = 0;
        int featureGroupsCount = 0;
        int keywordsCount = 0;
        boolean isNotFirst = false;
        while (constraintIter.hasNext()) {
            ProductSearchConstraint psc = (ProductSearchConstraint) constraintIter.next();
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
                    searchParamString.append(cc.exclude.booleanValue() ? "Y" : "N");
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
                    searchParamString.append(fc.exclude.booleanValue() ? "Y" : "N");
                }
            /* No way to specify parameters for these right now, so table until later
            } else if (psc instanceof ProductSearch.FeatureSetConstraint) {
                ProductSearch.FeatureSetConstraint fsc = (ProductSearch.FeatureSetConstraint) psc;
             */   
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
                    searchParamString.append(pfcc.exclude.booleanValue() ? "Y" : "N");
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
                    searchParamString.append(pfgc.exclude.booleanValue() ? "Y" : "N");
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
                    if (lprc.lowPrice != null) searchParamString.append(lprc.lowPrice);
                    searchParamString.append("_");
                    if (lprc.highPrice != null) searchParamString.append(lprc.highPrice);
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
        if (resultSortOrder != null) {
            if (resultSortOrder instanceof ProductSearch.SortKeywordRelevancy) {
                //ProductSearch.SortKeywordRelevancy skr = (ProductSearch.SortKeywordRelevancy) resultSortOrder;
                searchParamString.append("&amp;S_O=SKR");
            } else if (resultSortOrder instanceof ProductSearch.SortProductField) {
                ProductSearch.SortProductField spf = (ProductSearch.SortProductField) resultSortOrder;
                searchParamString.append("&amp;S_O=SPF:");
                searchParamString.append(spf.fieldName);
            } else if (resultSortOrder instanceof ProductSearch.SortProductPrice) {
                ProductSearch.SortProductPrice spp = (ProductSearch.SortProductPrice) resultSortOrder;
                searchParamString.append("&amp;S_O=SPP:");
                searchParamString.append(spp.productPriceTypeId);
            }
            searchParamString.append("&amp;S_A=");
            searchParamString.append(resultSortOrder.isAscending() ? "Y" : "N");
        }

        return searchParamString.toString();
    }
}
