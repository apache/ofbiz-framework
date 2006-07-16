/*
 *
 * Copyright 2001-2006 The Apache Software Foundation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package org.ofbiz.product.product;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.webapp.stats.VisitHandler;
import org.ofbiz.webapp.control.RequestHandler;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.product.catalog.CatalogWorker;
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
 *
 * @author <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @version    $Rev$
 * @since      3.0
 */
public class ProductSearchSession {

    public static final String module = ProductSearchSession.class.getName();

    public static class ProductSearchOptions implements java.io.Serializable {
        protected List constraintList = null;
        protected ResultSortOrder resultSortOrder = null;
        protected Integer viewIndex = null;
        protected Integer viewSize = null;
        protected boolean changed = false;

        public ProductSearchOptions() { }

        /** Basic copy constructor */
        public ProductSearchOptions(ProductSearchOptions productSearchOptions) {
            this.constraintList = new LinkedList(productSearchOptions.constraintList);
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
                productSearchOptions.constraintList = new LinkedList();
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
        public static ResultSortOrder getResultSortOrder(HttpSession session) {
            ProductSearchOptions productSearchOptions = getProductSearchOptions(session);
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

        public List searchGetConstraintStrings(boolean detailed, GenericDelegator delegator) {
            List productSearchConstraintList = this.getConstraintList();
            List constraintStrings = new ArrayList();
            if (productSearchConstraintList == null) {
                return constraintStrings;
            }
            Iterator productSearchConstraintIter = productSearchConstraintList.iterator();
            while (productSearchConstraintIter.hasNext()) {
                ProductSearchConstraint productSearchConstraint = (ProductSearchConstraint) productSearchConstraintIter.next();
                if (productSearchConstraint == null) continue;
                String constraintString = productSearchConstraint.prettyPrintConstraint(delegator, detailed);
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
            optionsHistoryList = new LinkedList();
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

        // make sure the view allow category is included
        productSearchConstraintList = ensureViewAllowConstraint(productSearchConstraintList, prodCatalogId, delegator);
        ResultSortOrder resultSortOrder = productSearchOptions.getResultSortOrder();

        // if the search options have changed since the last search, put at the beginning of the options history list
        checkSaveSearchOptionsHistory(session);
        
        return ProductSearch.searchProducts(productSearchConstraintList, resultSortOrder, delegator, visitId);
    }

    public static List ensureViewAllowConstraint(List productSearchConstraintList, String prodCatalogId, GenericDelegator delegator) {
        String viewProductCategoryId = CatalogWorker.getCatalogViewAllowCategoryId(delegator, prodCatalogId);
        if (UtilValidate.isNotEmpty(viewProductCategoryId)) {
            ProductSearchConstraint viewAllowConstraint = new CategoryConstraint(viewProductCategoryId, true);
            if (!productSearchConstraintList.contains(viewAllowConstraint)) {
                // don't add to same list, will modify the one in the session, create new list
                productSearchConstraintList = new ArrayList(productSearchConstraintList);
                productSearchConstraintList.add(viewAllowConstraint);
            }
        }
        return productSearchConstraintList;
    }

    public static void searchClear(HttpSession session) {
        ProductSearchOptions.clearSearchOptions(session);
    }
    
    public static List searchGetConstraintStrings(boolean detailed, HttpSession session, GenericDelegator delegator) {
        ProductSearchOptions productSearchOptions = getProductSearchOptions(session);
        return productSearchOptions.searchGetConstraintStrings(detailed, delegator);
    }

    public static String searchGetSortOrderString(boolean detailed, HttpSession session) {
        ResultSortOrder resultSortOrder = ProductSearchOptions.getResultSortOrder(session);
        if (resultSortOrder == null) return "";
        return resultSortOrder.prettyPrintSortOrder(detailed);
    }

    public static void searchSetSortOrder(ResultSortOrder resultSortOrder, HttpSession session) {
        ProductSearchOptions.setResultSortOrder(resultSortOrder, session);
    }

    public static void searchAddFeatureIdConstraints(Collection featureIds, HttpSession session) {
        if (featureIds == null || featureIds.size() == 0) {
            return;
        }
        Iterator featureIdIter = featureIds.iterator();
        while (featureIdIter.hasNext()) {
            String productFeatureId = (String) featureIdIter.next();
            searchAddConstraint(new FeatureConstraint(productFeatureId), session);
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

        // if there is another category, add a constraint for it
        if (UtilValidate.isNotEmpty((String) parameters.get("SEARCH_CATEGORY_ID"))) {
            String searchCategoryId = (String) parameters.get("SEARCH_CATEGORY_ID");
            String searchSubCategories = (String) parameters.get("SEARCH_SUB_CATEGORIES");
            searchAddConstraint(new ProductSearch.CategoryConstraint(searchCategoryId, !"N".equals(searchSubCategories)), session);
            constraintsChanged = true;
        }
        if (UtilValidate.isNotEmpty((String) parameters.get("SEARCH_CATEGORY_ID2"))) {
            String searchCategoryId = (String) parameters.get("SEARCH_CATEGORY_ID2");
            String searchSubCategories = (String) parameters.get("SEARCH_SUB_CATEGORIES2");
            searchAddConstraint(new ProductSearch.CategoryConstraint(searchCategoryId, !"N".equals(searchSubCategories)), session);
            constraintsChanged = true;
        }
        if (UtilValidate.isNotEmpty((String) parameters.get("SEARCH_CATEGORY_ID3"))) {
            String searchCategoryId = (String) parameters.get("SEARCH_CATEGORY_ID3");
            String searchSubCategories = (String) parameters.get("SEARCH_SUB_CATEGORIES3");
            searchAddConstraint(new ProductSearch.CategoryConstraint(searchCategoryId, !"N".equals(searchSubCategories)), session);
            constraintsChanged = true;
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
        if (UtilValidate.isNotEmpty((String) parameters.get("SEARCH_STRING2"))) {
            String keywordString = (String) parameters.get("SEARCH_STRING2");
            String searchOperator = (String) parameters.get("SEARCH_OPERATOR2");
            // defaults to true/Y, ie anything but N is true/Y
            boolean anyPrefixSuffix = !"N".equals((String) parameters.get("SEARCH_ANYPRESUF2"));
            searchAddConstraint(new ProductSearch.KeywordConstraint(keywordString, anyPrefixSuffix, anyPrefixSuffix, null, "AND".equals(searchOperator)), session);
            constraintsChanged = true;
        }
        if (UtilValidate.isNotEmpty((String) parameters.get("SEARCH_STRING3"))) {
            String keywordString = (String) parameters.get("SEARCH_STRING3");
            String searchOperator = (String) parameters.get("SEARCH_OPERATOR3");
            // defaults to true/Y, ie anything but N is true/Y
            boolean anyPrefixSuffix = !"N".equals((String) parameters.get("SEARCH_ANYPRESUF3"));
            searchAddConstraint(new ProductSearch.KeywordConstraint(keywordString, anyPrefixSuffix, anyPrefixSuffix, null, "AND".equals(searchOperator)), session);
            constraintsChanged = true;
        }

        // if features were specified by ID add a constraint for each
        List featureIdList = ParametricSearch.makeFeatureIdListFromPrefixed(parameters);
        if (featureIdList.size() > 0) {
            constraintsChanged = true;
            searchAddFeatureIdConstraints(featureIdList, session);
        }

        // if features were selected add a constraint for each
        Map featureIdByType = ParametricSearch.makeFeatureIdByTypeMap(parameters);
        if (featureIdByType.size() > 0) {
            constraintsChanged = true;
            searchAddFeatureIdConstraints(featureIdByType.values(), session);
        }

        // add a supplier to the search
        if (UtilValidate.isNotEmpty((String) parameters.get("SEARCH_SUPPLIER_ID"))) {
            String supplierPartyId = (String) parameters.get("SEARCH_SUPPLIER_ID");
            searchAddConstraint(new ProductSearch.SupplierConstraint(supplierPartyId), session);
            constraintsChanged = true;
        }

        // set the sort order
        String sortOrder = (String) parameters.get("sortOrder");
        String sortAscending = (String) parameters.get("sortAscending");
        boolean ascending = !"N".equals(sortAscending);
        if (sortOrder != null) {
            if (sortOrder.equals("SortKeywordRelevancy")) {
                searchSetSortOrder(new ProductSearch.SortKeywordRelevancy(), session);
            } else if (sortOrder.startsWith("SortProductField:")) {
                String fieldName = sortOrder.substring("SortProductField:".length());
                searchSetSortOrder(new ProductSearch.SortProductField(fieldName, ascending), session);
            } else if (sortOrder.startsWith("SortProductPrice:")) {
                String priceTypeId = sortOrder.substring("SortProductPrice:".length());
                searchSetSortOrder(new ProductSearch.SortProductPrice(priceTypeId, ascending), session);
            }
        }
        
        ProductSearchOptions productSearchOptions = getProductSearchOptions(session);
        if (constraintsChanged) {
            // query changed, clear out the VIEW_INDEX & VIEW_SIZE
            productSearchOptions.clearViewInfo();
        }

        String viewIndexStr = (String) parameters.get("VIEW_INDEX");
        if (UtilValidate.isNotEmpty(viewIndexStr)) {
            try {
                productSearchOptions.setViewIndex(Integer.valueOf(viewIndexStr));
            } catch (Exception e) {
                Debug.logError(e, "Error formatting VIEW_INDEX, setting to 0", module);
                // we could just do nothing here, but we know something was specified so we don't want to use the previous value from the session
                productSearchOptions.setViewIndex(new Integer(0));
            }
        }

        String viewSizeStr = (String) parameters.get("VIEW_SIZE");
        if (UtilValidate.isNotEmpty(viewSizeStr)) {
            try {
                productSearchOptions.setViewSize(Integer.valueOf(viewSizeStr));
            } catch (Exception e) {
                Debug.logError(e, "Error formatting VIEW_SIZE, setting to 20", module);
                productSearchOptions.setViewSize(new Integer(20));
            }
        }
    }

    public static Map getProductSearchResult(HttpSession session, GenericDelegator delegator, String prodCatalogId) {

        // ========== Create View Indexes
        int viewIndex = 0;
        int viewSize = 20;
        int highIndex = 0;
        int lowIndex = 0;
        int listSize = 0;

        ProductSearchOptions productSearchOptions = getProductSearchOptions(session);
        
        Integer viewIndexInteger = productSearchOptions.getViewIndex();
        if (viewIndexInteger != null) viewIndex = viewIndexInteger.intValue();
        Integer viewSizeInteger = productSearchOptions.getViewSize();
        if (viewSizeInteger != null) viewSize = viewSizeInteger.intValue();

        lowIndex = viewIndex * viewSize;
        highIndex = (viewIndex + 1) * viewSize;

        // setup resultOffset and maxResults, noting that resultOffset is 1 based, not zero based as these numbers
        Integer resultOffset = new Integer(lowIndex + 1);
        Integer maxResults = new Integer(viewSize);

        // ========== Do the actual search
        ArrayList productIds = null;
        String visitId = VisitHandler.getVisitId(session);
        List productSearchConstraintList = ProductSearchOptions.getConstraintList(session);
        // if no constraints, don't do a search...
        if (productSearchConstraintList != null && productSearchConstraintList.size() > 0) {
            // if the search options have changed since the last search, put at the beginning of the options history list
            checkSaveSearchOptionsHistory(session);

            productSearchConstraintList = ensureViewAllowConstraint(productSearchConstraintList, prodCatalogId, delegator);
            ResultSortOrder resultSortOrder = ProductSearchOptions.getResultSortOrder(session);

            ProductSearchContext productSearchContext = new ProductSearchContext(delegator, visitId);
            productSearchContext.addProductSearchConstraints(productSearchConstraintList);
            productSearchContext.setResultSortOrder(resultSortOrder);
            productSearchContext.setResultOffset(resultOffset);
            productSearchContext.setMaxResults(maxResults);

            productIds = productSearchContext.doSearch();

            Integer totalResults = productSearchContext.getTotalResults();
            if (totalResults != null) {
                listSize = totalResults.intValue();
            }
        }

        if (listSize < highIndex) {
            highIndex = listSize;
        }

        // ========== Setup other display info
        List searchConstraintStrings = searchGetConstraintStrings(false, session, delegator);
        String searchSortOrderString = searchGetSortOrderString(false, session);

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
}
