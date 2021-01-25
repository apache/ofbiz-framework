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
package org.apache.ofbiz.product.product;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilHttp;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.transaction.GenericTransactionException;
import org.apache.ofbiz.entity.transaction.TransactionUtil;
import org.apache.ofbiz.entity.util.EntityListIterator;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.product.product.ProductSearch.ProductSearchContext;
import org.apache.ofbiz.product.product.ProductSearch.ResultSortOrder;
import org.apache.ofbiz.webapp.stats.VisitHandler;

/**
 * Product Search Related Events
 */
public class ProductSearchEvents {

    public static final int DEFAULT_TX_TIMEOUT = 600;
    private static final String MODULE = ProductSearchEvents.class.getName();
    private static final String RESOURCE = "ProductErrorUiLabels";

    /**
     * Removes the results of a search from the specified category
     * @param request  The HTTPRequest object for the current request
     * @param response The HTTPResponse object for the current request
     * @return String specifying the exit status of this event
     */
    public static String searchRemoveFromCategory(HttpServletRequest request, HttpServletResponse response) {
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        String productCategoryId = request.getParameter("SE_SEARCH_CATEGORY_ID");
        String errMsg = null;

        try {
            boolean beganTransaction = TransactionUtil.begin(DEFAULT_TX_TIMEOUT);
            try (EntityListIterator eli = getProductSearchResults(request)) {
                if (eli == null) {
                    errMsg = UtilProperties.getMessage(RESOURCE, "productsearchevents.no_results_found_probably_error_constraints",
                            UtilHttp.getLocale(request));
                    request.setAttribute("_ERROR_MESSAGE_", errMsg);
                    return "error";
                }

                int numRemoved = 0;
                GenericValue searchResultView = null;
                while ((searchResultView = eli.next()) != null) {
                    String productId = searchResultView.getString("mainProductId");
                    numRemoved += delegator.removeByAnd("ProductCategoryMember", UtilMisc.toMap("productCategoryId", productCategoryId,
                            "productId", productId));
                }
                Map<String, String> messageMap = UtilMisc.toMap("numRemoved", Integer.toString(numRemoved));
                errMsg = UtilProperties.getMessage(RESOURCE, "productsearchevents.removed_x_items", messageMap, UtilHttp.getLocale(request));
                request.setAttribute("_EVENT_MESSAGE_", errMsg);
            } catch (GenericEntityException e) {
                Map<String, String> messageMap = UtilMisc.toMap("errSearchResult", e.toString());
                errMsg = UtilProperties.getMessage(RESOURCE, "productsearchevents.error_getting_search_results", messageMap,
                        UtilHttp.getLocale(request));
                Debug.logError(e, errMsg, MODULE);
                request.setAttribute("_ERROR_MESSAGE_", errMsg);
                TransactionUtil.rollback(beganTransaction, errMsg, e);
                return "error";
            } finally {
                TransactionUtil.commit(beganTransaction);
            }
        } catch (GenericTransactionException e) {
            Map<String, String> messageMap = UtilMisc.toMap("errSearchResult", e.toString());
            errMsg = UtilProperties.getMessage(RESOURCE, "productsearchevents.error_getting_search_results", messageMap, UtilHttp.getLocale(request));
            Debug.logError(e, errMsg, MODULE);
            request.setAttribute("_ERROR_MESSAGE_", errMsg);
            return "error";
        }
        return "success";
    }

    /**
     * Sets the thru date of the results of a search to the specified date for the specified catogory
     * @param request  The HTTPRequest object for the current request
     * @param response The HTTPResponse object for the current request
     * @return String specifying the exit status of this event
     */
    public static String searchExpireFromCategory(HttpServletRequest request, HttpServletResponse response) {
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        String productCategoryId = request.getParameter("SE_SEARCH_CATEGORY_ID");
        String thruDateStr = request.getParameter("thruDate");
        String errMsg = null;

        Timestamp thruDate;
        try {
            thruDate = Timestamp.valueOf(thruDateStr);
        } catch (RuntimeException e) {
            Map<String, String> messageMap = UtilMisc.toMap("errDateFormat", e.toString());
            errMsg = UtilProperties.getMessage(RESOURCE, "productsearchevents.thruDate_not_formatted_properly", messageMap,
                    UtilHttp.getLocale(request));
            Debug.logError(e, errMsg, MODULE);
            request.setAttribute("_ERROR_MESSAGE_", errMsg);
            return "error";
        }

        try {
            boolean beganTransaction = TransactionUtil.begin(DEFAULT_TX_TIMEOUT);
            try (EntityListIterator eli = getProductSearchResults(request)) {
                if (eli == null) {
                    errMsg = UtilProperties.getMessage(RESOURCE, "productsearchevents.no_results_found_probably_error_constraints",
                            UtilHttp.getLocale(request));
                    request.setAttribute("_ERROR_MESSAGE_", errMsg);
                    return "error";
                }

                GenericValue searchResultView = null;
                int numExpired = 0;
                while ((searchResultView = eli.next()) != null) {
                    String productId = searchResultView.getString("mainProductId");
                    //get all tuples that match product and category
                    List<GenericValue> pcmList = EntityQuery.use(delegator).from("ProductCategoryMember").where("productCategoryId",
                            productCategoryId, "productId", productId).queryList();

                    //set those thrudate to that specificed maybe remove then add new one
                    for (GenericValue pcm : pcmList) {
                        if (pcm.get("thruDate") == null) {
                            pcm.set("thruDate", thruDate);
                            pcm.store();
                            numExpired++;
                        }
                    }
                }
                Map<String, String> messageMap = UtilMisc.toMap("numExpired", Integer.toString(numExpired));
                errMsg = UtilProperties.getMessage(RESOURCE, "productsearchevents.expired_x_items", messageMap, UtilHttp.getLocale(request));
                request.setAttribute("_EVENT_MESSAGE_", errMsg);
            } catch (GenericEntityException e) {
                Map<String, String> messageMap = UtilMisc.toMap("errSearchResult", e.toString());
                errMsg = UtilProperties.getMessage(RESOURCE, "productsearchevents.error_getting_search_results", messageMap,
                        UtilHttp.getLocale(request));
                Debug.logError(e, errMsg, MODULE);
                request.setAttribute("_ERROR_MESSAGE_", errMsg);
                TransactionUtil.rollback(beganTransaction, errMsg, e);
                return "error";
            } finally {
                TransactionUtil.commit(beganTransaction);
            }
        } catch (GenericTransactionException e) {
            Map<String, String> messageMap = UtilMisc.toMap("errSearchResult", e.toString());
            errMsg = UtilProperties.getMessage(RESOURCE, "productsearchevents.error_getting_search_results", messageMap, UtilHttp.getLocale(request));
            Debug.logError(e, errMsg, MODULE);
            request.setAttribute("_ERROR_MESSAGE_", errMsg);
            return "error";
        }
        return "success";
    }

    /**
     * Adds the results of a search to the specified catogory
     * @param request  The HTTPRequest object for the current request
     * @param response The HTTPResponse object for the current request
     * @return String specifying the exit status of this event
     */
    public static String searchAddToCategory(HttpServletRequest request, HttpServletResponse response) {
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        String productCategoryId = request.getParameter("SE_SEARCH_CATEGORY_ID");
        String fromDateStr = request.getParameter("fromDate");
        Timestamp fromDate = null;
        String errMsg = null;

        try {
            fromDate = Timestamp.valueOf(fromDateStr);
        } catch (RuntimeException e) {
            Map<String, String> messageMap = UtilMisc.toMap("errDateFormat", e.toString());
            errMsg = UtilProperties.getMessage(RESOURCE, "productsearchevents.fromDate_not_formatted_properly", messageMap,
                    UtilHttp.getLocale(request));
            request.setAttribute("_ERROR_MESSAGE_", errMsg);
            return "error";
        }

        try {
            boolean beganTransaction = TransactionUtil.begin(DEFAULT_TX_TIMEOUT);
            try (EntityListIterator eli = getProductSearchResults(request)) {
                if (eli == null) {
                    errMsg = UtilProperties.getMessage(RESOURCE, "productsearchevents.no_results_found_probably_error_constraints",
                            UtilHttp.getLocale(request));
                    request.setAttribute("_ERROR_MESSAGE_", errMsg);
                    return "error";
                }

                GenericValue searchResultView = null;
                int numAdded = 0;
                while ((searchResultView = eli.next()) != null) {
                    String productId = searchResultView.getString("mainProductId");

                    GenericValue pcm = delegator.makeValue("ProductCategoryMember");
                    pcm.set("productCategoryId", productCategoryId);
                    pcm.set("productId", productId);
                    pcm.set("fromDate", fromDate);
                    pcm.create();

                    numAdded++;
                }
                Map<String, String> messageMap = UtilMisc.toMap("numAdded", Integer.toString(numAdded));
                errMsg = UtilProperties.getMessage(RESOURCE, "productsearchevents.added_x_product_category_members", messageMap,
                        UtilHttp.getLocale(request));
                request.setAttribute("_EVENT_MESSAGE_", errMsg);
            } catch (GenericEntityException e) {
                Map<String, String> messageMap = UtilMisc.toMap("errSearchResult", e.toString());
                errMsg = UtilProperties.getMessage(RESOURCE, "productsearchevents.error_getting_search_results", messageMap,
                        UtilHttp.getLocale(request));
                Debug.logError(e, errMsg, MODULE);
                request.setAttribute("_ERROR_MESSAGE_", errMsg);
                TransactionUtil.rollback(beganTransaction, errMsg, e);
                return "error";
            } finally {
                TransactionUtil.commit(beganTransaction);
            }
        } catch (GenericTransactionException e) {
            Map<String, String> messageMap = UtilMisc.toMap("errSearchResult", e.toString());
            errMsg = UtilProperties.getMessage(RESOURCE, "productsearchevents.error_getting_search_results", messageMap, UtilHttp.getLocale(request));
            Debug.logError(e, errMsg, MODULE);
            request.setAttribute("_ERROR_MESSAGE_", errMsg);
            return "error";
        }

        return "success";
    }

    /**
     * Adds a feature to search results
     * @param request  The HTTPRequest object for the current request
     * @param response The HTTPResponse object for the current request
     * @return String specifying the exit status of this event
     */
    public static String searchAddFeature(HttpServletRequest request, HttpServletResponse response) {
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        Locale locale = UtilHttp.getLocale(request);

        String productFeatureId = request.getParameter("productFeatureId");
        String fromDateStr = request.getParameter("fromDate");
        String thruDateStr = request.getParameter("thruDate");
        String amountStr = request.getParameter("amount");
        String sequenceNumStr = request.getParameter("sequenceNum");
        String productFeatureApplTypeId = request.getParameter("productFeatureApplTypeId");

        Timestamp thruDate = null;
        Timestamp fromDate = null;
        BigDecimal amount = null;
        Long sequenceNum = null;

        try {
            if (UtilValidate.isNotEmpty(fromDateStr)) {
                fromDate = Timestamp.valueOf(fromDateStr);
            }
            if (UtilValidate.isNotEmpty(thruDateStr)) {
                thruDate = Timestamp.valueOf(thruDateStr);
            }
            if (UtilValidate.isNotEmpty(amountStr)) {
                amount = new BigDecimal(amountStr);
            }
            if (UtilValidate.isNotEmpty(sequenceNumStr)) {
                sequenceNum = Long.valueOf(sequenceNumStr);
            }
        } catch (RuntimeException e) {
            String errorMsg = UtilProperties.getMessage(RESOURCE, "productSearchEvents.error_casting_types", locale) + " : " + e.toString();
            request.setAttribute("_ERROR_MESSAGE_", errorMsg);
            Debug.logError(e, errorMsg, MODULE);
            return "error";
        }

        try {
            boolean beganTransaction = TransactionUtil.begin(DEFAULT_TX_TIMEOUT);
            try (EntityListIterator eli = getProductSearchResults(request)) {
                if (eli == null) {
                    String errMsg = UtilProperties.getMessage(RESOURCE, "productsearchevents.no_results_found_probably_error_constraints",
                            UtilHttp.getLocale(request));
                    request.setAttribute("_ERROR_MESSAGE_", errMsg);
                    return "error";
                }

                GenericValue searchResultView = null;
                int numAdded = 0;
                while ((searchResultView = eli.next()) != null) {
                    String productId = searchResultView.getString("mainProductId");
                    GenericValue pfa = delegator.makeValue("ProductFeatureAppl");
                    pfa.set("productId", productId);
                    pfa.set("productFeatureId", productFeatureId);
                    pfa.set("fromDate", fromDate);
                    pfa.set("thruDate", thruDate);
                    pfa.set("productFeatureApplTypeId", productFeatureApplTypeId);
                    pfa.set("amount", amount);
                    pfa.set("sequenceNum", sequenceNum);
                    pfa.create();
                    numAdded++;
                }
                Map<String, Object> messageMap = UtilMisc.toMap("numAdded", numAdded, "productFeatureId", productFeatureId);
                String eventMsg = UtilProperties.getMessage(RESOURCE, "productSearchEvents.added_param_features", messageMap, locale) + ".";
                request.setAttribute("_EVENT_MESSAGE_", eventMsg);
            } catch (GenericEntityException e) {
                String errorMsg = UtilProperties.getMessage(RESOURCE, "productSearchEvents.error_getting_results", locale) + " : " + e.toString();
                request.setAttribute("_ERROR_MESSAGE_", errorMsg);
                Debug.logError(e, errorMsg, MODULE);
                TransactionUtil.rollback(beganTransaction, errorMsg, e);
                return "error";
            } finally {
                TransactionUtil.commit(beganTransaction);
            }
        } catch (GenericTransactionException e) {
            String errorMsg = UtilProperties.getMessage(RESOURCE, "productSearchEvents.error_getting_results", locale) + " : " + e.toString();
            request.setAttribute("_ERROR_MESSAGE_", errorMsg);
            Debug.logError(e, errorMsg, MODULE);
            return "error";
        }

        return "success";
    }

    /**
     * Removes a feature from search results
     * @param request  The HTTPRequest object for the current request
     * @param response The HTTPResponse object for the current request
     * @return String specifying the exit status of this event
     */
    public static String searchRemoveFeature(HttpServletRequest request, HttpServletResponse response) {
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        Locale locale = UtilHttp.getLocale(request);

        String productFeatureId = request.getParameter("productFeatureId");

        try {
            boolean beganTransaction = TransactionUtil.begin(DEFAULT_TX_TIMEOUT);
            try (EntityListIterator eli = getProductSearchResults(request)) {
                if (eli == null) {
                    String errMsg = UtilProperties.getMessage(RESOURCE, "productsearchevents.no_results_found_probably_error_constraints",
                            UtilHttp.getLocale(request));
                    request.setAttribute("_ERROR_MESSAGE_", errMsg);
                    return "error";
                }

                GenericValue searchResultView = null;
                int numRemoved = 0;
                while ((searchResultView = eli.next()) != null) {
                    String productId = searchResultView.getString("mainProductId");
                    numRemoved += delegator.removeByAnd("ProductFeatureAppl", UtilMisc.toMap("productId", productId, "productFeatureId",
                            productFeatureId));
                }
                Map<String, Object> messageMap = UtilMisc.toMap("numRemoved", numRemoved, "productFeatureId", productFeatureId);
                String eventMsg = UtilProperties.getMessage(RESOURCE, "productSearchEvents.removed_param_features", messageMap, locale) + ".";
                request.setAttribute("_EVENT_MESSAGE_", eventMsg);
            } catch (GenericEntityException e) {
                String errorMsg = UtilProperties.getMessage(RESOURCE, "productSearchEvents.error_getting_results", locale) + " : " + e.toString();
                request.setAttribute("_ERROR_MESSAGE_", errorMsg);
                Debug.logError(e, errorMsg, MODULE);
                TransactionUtil.rollback(beganTransaction, errorMsg, e);
                return "error";
            } finally {
                TransactionUtil.commit(beganTransaction);
            }
        } catch (GenericTransactionException e) {
            String errorMsg = UtilProperties.getMessage(RESOURCE, "productSearchEvents.error_getting_results", locale) + " : " + e.toString();
            request.setAttribute("_ERROR_MESSAGE_", errorMsg);
            Debug.logError(e, errorMsg, MODULE);
            return "error";
        }

        return "success";
    }

    /**
     * Formats the results of a search to the screen as a tab-delimited output
     * @param request  The HTTPRequest object for the current request
     * @param response The HTTPResponse object for the current request
     * @return String specifying the exit status of this event
     */
    public static String searchExportProductList(HttpServletRequest request, HttpServletResponse response) {
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        String errMsg = null;
        List<Map<String, Object>> productExportList = new LinkedList<>();

        try {
            boolean beganTransaction = TransactionUtil.begin(DEFAULT_TX_TIMEOUT);
            try (EntityListIterator eli = getProductSearchResults(request)) {
                if (eli == null) {
                    errMsg = UtilProperties.getMessage(RESOURCE, "productsearchevents.no_results_found_probably_error_constraints",
                            UtilHttp.getLocale(request));
                    request.setAttribute("_ERROR_MESSAGE_", errMsg);
                    return "error";
                }

                GenericValue searchResultView = null;
                while ((searchResultView = eli.next()) != null) {
                    Map<String, Object> productMap = new HashMap<>();
                    String productId = searchResultView.getString("mainProductId");
                    productMap.put("productId", productId);

                    productMap.put("productFeatureCustom", EntityQuery.use(delegator).from("ProductFeatureAndAppl").where("productId", productId,
                            "productFeatureTypeId", "HAZMAT").filterByDate().queryFirst());

                    List<GenericValue> productCategories = EntityQuery.use(delegator).from("ProductCategoryAndMember").where("productId",
                            productId).filterByDate().queryList();
                    productMap.put("productCategories", productCategories);
                    List<GenericValue> productFeatures =
                            EntityQuery.use(delegator).from("ProductFeatureAndAppl").where("productId", productId).filterByDate().queryList();
                    productMap.put("productFeatures", productFeatures);
                    productExportList.add(productMap);
                }
            } catch (GenericEntityException e) {
                Map<String, String> messageMap = UtilMisc.toMap("errSearchResult", e.toString());
                errMsg = UtilProperties.getMessage(RESOURCE, "productsearchevents.error_getting_search_results", messageMap,
                        UtilHttp.getLocale(request));
                Debug.logError(e, errMsg, MODULE);
                request.setAttribute("_ERROR_MESSAGE_", errMsg);
                TransactionUtil.rollback(beganTransaction, errMsg, e);
                return "error";
            } finally {
                TransactionUtil.commit(beganTransaction);
            }
        } catch (GenericTransactionException e) {
            Map<String, String> messageMap = UtilMisc.toMap("errSearchResult", e.toString());
            errMsg = UtilProperties.getMessage(RESOURCE, "productsearchevents.error_getting_search_results", messageMap, UtilHttp.getLocale(request));
            Debug.logError(e, errMsg, MODULE);
            request.setAttribute("_ERROR_MESSAGE_", errMsg);
            return "error";
        }

        request.setAttribute("productExportList", productExportList);
        return "success";
    }

    private static EntityListIterator getProductSearchResults(HttpServletRequest request) {
        HttpSession session = request.getSession();
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        String visitId = VisitHandler.getVisitId(session);

        List<ProductSearch.ProductSearchConstraint> productSearchConstraintList =
                ProductSearchSession.ProductSearchOptions.getConstraintList(session);
        // if no constraints, don't do a search...
        if (UtilValidate.isNotEmpty(productSearchConstraintList)) {
            ResultSortOrder resultSortOrder = ProductSearchSession.ProductSearchOptions.getResultSortOrder(request);
            ProductSearchSession.checkSaveSearchOptionsHistory(session);
            ProductSearchContext productSearchContext = new ProductSearchContext(delegator, visitId);

            productSearchContext.addProductSearchConstraints(productSearchConstraintList);
            productSearchContext.setResultSortOrder(resultSortOrder);

            return productSearchContext.doQuery(delegator);
        }
        return null;
    }
}
