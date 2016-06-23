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
package org.ofbiz.product.product;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.ObjectType;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilGenerics;
import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericPK;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.transaction.GenericTransactionException;
import org.ofbiz.entity.transaction.TransactionUtil;
import org.ofbiz.entity.util.EntityListIterator;
import org.ofbiz.entity.util.EntityQuery;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.entity.util.EntityUtilProperties;
import org.ofbiz.product.store.ProductStoreWorker;
import org.ofbiz.security.Security;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.webapp.website.WebSiteWorker;

/**
 * Product Information Related Events
 */
public class ProductEvents {

    public static final String module = ProductEvents.class.getName();
    public static final String resource = "ProductErrorUiLabels";

    /**
     * Updates/adds keywords for all products
     *
     * @param request HTTPRequest object for the current request
     * @param response HTTPResponse object for the current request
     * @return String specifying the exit status of this event
     */
    public static String updateAllKeywords(HttpServletRequest request, HttpServletResponse response) {
        //String errMsg = "";
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        Security security = (Security) request.getAttribute("security");
        Timestamp nowTimestamp = UtilDateTime.nowTimestamp();

        String updateMode = "CREATE";
        String errMsg=null;

        String doAll = request.getParameter("doAll");

        // check permissions before moving on...
        if (!security.hasEntityPermission("CATALOG", "_" + updateMode, request.getSession())) {
            Map<String, String> messageMap = UtilMisc.toMap("updateMode", updateMode);
            errMsg = UtilProperties.getMessage(resource,"productevents.not_sufficient_permissions", messageMap, UtilHttp.getLocale(request));
            request.setAttribute("_ERROR_MESSAGE_", errMsg);
            return "error";
        }

        EntityCondition condition = null;
        if (!"Y".equals(doAll)) {
            List<EntityCondition> condList = new LinkedList<EntityCondition>();
            condList.add(EntityCondition.makeCondition(EntityCondition.makeCondition("autoCreateKeywords", EntityOperator.EQUALS, null), EntityOperator.OR, EntityCondition.makeCondition("autoCreateKeywords", EntityOperator.NOT_EQUAL, "N")));
            if ("true".equals(EntityUtilProperties.getPropertyValue("prodsearch", "index.ignore.variants", delegator))) {
                condList.add(EntityCondition.makeCondition(EntityCondition.makeCondition("isVariant", EntityOperator.EQUALS, null), EntityOperator.OR, EntityCondition.makeCondition("isVariant", EntityOperator.NOT_EQUAL, "Y")));
            }
            if ("true".equals(EntityUtilProperties.getPropertyValue("prodsearch", "index.ignore.discontinued.sales", delegator))) {
                condList.add(EntityCondition.makeCondition(EntityCondition.makeCondition("salesDiscontinuationDate", EntityOperator.EQUALS, null), EntityOperator.OR, EntityCondition.makeCondition("salesDiscontinuationDate", EntityOperator.GREATER_THAN_EQUAL_TO, nowTimestamp)));
            }
            condition = EntityCondition.makeCondition(condList, EntityOperator.AND);
        } else {
            condition = EntityCondition.makeCondition(EntityCondition.makeCondition("autoCreateKeywords", EntityOperator.EQUALS, null), EntityOperator.OR, EntityCondition.makeCondition("autoCreateKeywords", EntityOperator.NOT_EQUAL, "N"));
        }


        EntityListIterator entityListIterator = null;
        int numProds = 0;
        int errProds = 0;

        boolean beganTx = false;
        try {
            // begin the transaction
            beganTx = TransactionUtil.begin(7200);
            try {
                if (Debug.infoOn()) {
                    long count = EntityQuery.use(delegator).from("Product").where(condition).queryCount();
                    Debug.logInfo("========== Found " + count + " products to index ==========", module);
                }
                entityListIterator = EntityQuery.use(delegator).from("Product").where(condition).queryIterator();
            } catch (GenericEntityException gee) {
                Debug.logWarning(gee, gee.getMessage(), module);
                Map<String, String> messageMap = UtilMisc.toMap("gee", gee.toString());
                errMsg = UtilProperties.getMessage(resource,"productevents.error_getting_product_list", messageMap, UtilHttp.getLocale(request));
                request.setAttribute("_ERROR_MESSAGE_", errMsg);
                throw gee;
            }

            GenericValue product;
            while ((product = entityListIterator.next()) != null) {
                try {
                    KeywordIndex.indexKeywords(product, "Y".equals(doAll));
                } catch (GenericEntityException e) {
                    //errMsg = UtilProperties.getMessage(resource,"productevents.could_not_create_keywords_write", UtilHttp.getLocale(request));
                    //request.setAttribute("_ERROR_MESSAGE_", errMsg);
                    Debug.logWarning("[ProductEvents.updateAllKeywords] Could not create product-keyword (write error); message: " + e.getMessage(), module);
                    errProds++;
                }
                numProds++;
                if (numProds % 500 == 0) {
                    Debug.logInfo("Keywords indexed for " + numProds + " so far", module);
                }
            }
        } catch (GenericEntityException e) {
            try {
                TransactionUtil.rollback(beganTx, e.getMessage(), e);
            } catch (Exception e1) {
                Debug.logError(e1, module);
            }
            return "error";
        } catch (Throwable t) {
            Debug.logError(t, module);
            request.setAttribute("_ERROR_MESSAGE_", t.getMessage());
            try {
                TransactionUtil.rollback(beganTx, t.getMessage(), t);
            } catch (Exception e2) {
                Debug.logError(e2, module);
            }
            return "error";
        } finally {
            if (entityListIterator != null) {
                try {
                    entityListIterator.close();
                } catch (GenericEntityException gee) {
                    Debug.logError(gee, "Error closing EntityListIterator when indexing product keywords.", module);
                }
            }

            // commit the transaction
            try {
                TransactionUtil.commit(beganTx);
            } catch (Exception e) {
                Debug.logError(e, module);
            }
        }

        if (errProds == 0) {
            Map<String, String> messageMap = UtilMisc.toMap("numProds", Integer.toString(numProds));
            errMsg = UtilProperties.getMessage(resource,"productevents.keyword_creation_complete_for_products", messageMap, UtilHttp.getLocale(request));
            request.setAttribute("_EVENT_MESSAGE_", errMsg);
            return "success";
        } else {
            Map<String, String> messageMap = UtilMisc.toMap("numProds", Integer.toString(numProds));
            messageMap.put("errProds", Integer.toString(errProds));
            errMsg = UtilProperties.getMessage(resource,"productevents.keyword_creation_complete_for_products_with_errors", messageMap, UtilHttp.getLocale(request));
            request.setAttribute("_ERROR_MESSAGE_", errMsg);
            return "error";
        }
    }

    /**
     * Updates ProductAssoc information according to UPDATE_MODE parameter
     *
     * @param request The HTTPRequest object for the current request
     * @param response The HTTPResponse object for the current request
     * @return String specifying the exit status of this event
     */
    public static String updateProductAssoc(HttpServletRequest request, HttpServletResponse response) {
        String errMsg = "";
        List<Object> errMsgList = new LinkedList<Object>();
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        Security security = (Security) request.getAttribute("security");

        String updateMode = request.getParameter("UPDATE_MODE");

        if (UtilValidate.isEmpty(updateMode)) {
            errMsg = UtilProperties.getMessage(resource,"productevents.updatemode_not_specified", UtilHttp.getLocale(request));
            request.setAttribute("_ERROR_MESSAGE_", errMsg);
            Debug.logWarning("[ProductEvents.updateProductAssoc] Update Mode was not specified, but is required", module);
            return "error";
        }

        // check permissions before moving on...
        if (!security.hasEntityPermission("CATALOG", "_" + updateMode, request.getSession())) {
            Map<String, String> messageMap = UtilMisc.toMap("updateMode", updateMode);
            errMsg = UtilProperties.getMessage(resource,"productevents.not_sufficient_permissions", messageMap, UtilHttp.getLocale(request));
            request.setAttribute("_ERROR_MESSAGE_", errMsg);
            return "error";
        }

        String productId = request.getParameter("PRODUCT_ID");
        String productIdTo = request.getParameter("PRODUCT_ID_TO");
        String productAssocTypeId = request.getParameter("PRODUCT_ASSOC_TYPE_ID");
        String fromDateStr = request.getParameter("FROM_DATE");
        Timestamp fromDate = null;

        try {
            if (EntityQuery.use(delegator).from("Product").where("productId", productId).queryOne() == null) {
                Map<String, String> messageMap = UtilMisc.toMap("productId", productId);
                errMsgList.add(UtilProperties.getMessage(resource,"productevents.product_with_id_not_found", messageMap, UtilHttp.getLocale(request)));
            }
            if (EntityQuery.use(delegator).from("Product").where("productId", productIdTo).queryOne() == null) {
                Map<String, String> messageMap = UtilMisc.toMap("productIdTo", productIdTo);
                errMsgList.add(UtilProperties.getMessage(resource,"productevents.product_To_with_id_not_found", messageMap, UtilHttp.getLocale(request)));
            }
        } catch (GenericEntityException e) {
            // if there is an exception for either, the other probably wont work
            Debug.logWarning(e, module);
        }

        if (UtilValidate.isNotEmpty(fromDateStr)) {
            try {
                fromDate = (Timestamp) ObjectType.simpleTypeConvert(fromDateStr, "Timestamp", null, UtilHttp.getTimeZone(request), UtilHttp.getLocale(request), false);
            } catch (Exception e) {
                errMsgList.add("From Date not formatted correctly.");
            }
        }
        if (!UtilValidate.isNotEmpty(productId))
            errMsgList.add(UtilProperties.getMessage(resource,"productevents.product_ID_missing", UtilHttp.getLocale(request)));
        if (!UtilValidate.isNotEmpty(productIdTo))
            errMsgList.add(UtilProperties.getMessage(resource,"productevents.product_ID_To_missing", UtilHttp.getLocale(request)));
        if (!UtilValidate.isNotEmpty(productAssocTypeId))
            errMsgList.add(UtilProperties.getMessage(resource,"productevents.association_type_ID_missing", UtilHttp.getLocale(request)));
        // from date is only required if update mode is not CREATE
        if (!updateMode.equals("CREATE") && !UtilValidate.isNotEmpty(fromDateStr))
            errMsgList.add(UtilProperties.getMessage(resource,"productevents.from_date_missing", UtilHttp.getLocale(request)));
        if (errMsgList.size() > 0) {
            request.setAttribute("_ERROR_MESSAGE_LIST_", errMsgList);
            return "error";
        }

        // clear some cache entries
        delegator.clearCacheLine("ProductAssoc", UtilMisc.toMap("productId", productId));
        delegator.clearCacheLine("ProductAssoc", UtilMisc.toMap("productId", productId, "productAssocTypeId", productAssocTypeId));

        delegator.clearCacheLine("ProductAssoc", UtilMisc.toMap("productIdTo", productIdTo));
        delegator.clearCacheLine("ProductAssoc", UtilMisc.toMap("productIdTo", productIdTo, "productAssocTypeId", productAssocTypeId));

        delegator.clearCacheLine("ProductAssoc", UtilMisc.toMap("productAssocTypeId", productAssocTypeId));
        delegator.clearCacheLine("ProductAssoc", UtilMisc.toMap("productId", productId, "productIdTo", productIdTo, "productAssocTypeId", productAssocTypeId, "fromDate", fromDate));

        GenericValue tempProductAssoc = delegator.makeValue("ProductAssoc", UtilMisc.toMap("productId", productId, "productIdTo", productIdTo, "productAssocTypeId", productAssocTypeId, "fromDate", fromDate));

        if (updateMode.equals("DELETE")) {
            GenericValue productAssoc = null;

            try {
                productAssoc = EntityQuery.use(delegator).from(tempProductAssoc.getEntityName()).where(tempProductAssoc.getPrimaryKey()).queryOne();
            } catch (GenericEntityException e) {
                Debug.logWarning(e.getMessage(), module);
                productAssoc = null;
            }
            if (productAssoc == null) {
                errMsg = UtilProperties.getMessage(resource,"productevents.could_not_remove_product_association_exist", UtilHttp.getLocale(request));
                request.setAttribute("_ERROR_MESSAGE_", errMsg);
                return "error";
            }
            try {
                productAssoc.remove();
            } catch (GenericEntityException e) {
                errMsg = UtilProperties.getMessage(resource,"productevents.could_not_remove_product_association_write", UtilHttp.getLocale(request));
                request.setAttribute("_ERROR_MESSAGE_", errMsg);
                Debug.logWarning("[ProductEvents.updateProductAssoc] Could not remove product association (write error); message: " + e.getMessage(), module);
                return "error";
            }
            return "success";
        }

        String thruDateStr = request.getParameter("THRU_DATE");
        String reason = request.getParameter("REASON");
        String instruction = request.getParameter("INSTRUCTION");
        String quantityStr = request.getParameter("QUANTITY");
        String sequenceNumStr = request.getParameter("SEQUENCE_NUM");
        Timestamp thruDate = null;
        BigDecimal quantity = null;
        Long sequenceNum = null;

        if (UtilValidate.isNotEmpty(thruDateStr)) {
            try {
                thruDate = (Timestamp) ObjectType.simpleTypeConvert(thruDateStr, "Timestamp", null, UtilHttp.getTimeZone(request), UtilHttp.getLocale(request), false);
            } catch (Exception e) {
                errMsgList.add(UtilProperties.getMessage(resource,"productevents.thru_date_not_formatted_correctly", UtilHttp.getLocale(request)));
            }
        }
        if (UtilValidate.isNotEmpty(quantityStr)) {
            try {
                quantity = new BigDecimal(quantityStr);
            } catch (NumberFormatException e) {
                errMsgList.add(UtilProperties.getMessage(resource,"productevents.quantity_not_formatted_correctly", UtilHttp.getLocale(request)));
            }
        }
        if (UtilValidate.isNotEmpty(sequenceNumStr)) {
            try {
                sequenceNum = Long.valueOf(sequenceNumStr);
            } catch (Exception e) {
                errMsgList.add(UtilProperties.getMessage(resource,"productevents.sequenceNum_not_formatted_correctly", UtilHttp.getLocale(request)));
            }
        }
        if (errMsgList.size() > 0) {
            request.setAttribute("_ERROR_MESSAGE_LIST_", errMsgList);
            return "error";
        }

        tempProductAssoc.set("thruDate", thruDate);
        tempProductAssoc.set("reason", reason);
        tempProductAssoc.set("instruction", instruction);
        tempProductAssoc.set("quantity", quantity);
        tempProductAssoc.set("sequenceNum", sequenceNum);

        if (updateMode.equals("CREATE")) {
            // if no from date specified, set to now
            if (fromDate == null) {
                fromDate = new Timestamp(new java.util.Date().getTime());
                tempProductAssoc.set("fromDate", fromDate);
                request.setAttribute("ProductAssocCreateFromDate", fromDate);
            }

            GenericValue productAssoc = null;

            try {
                productAssoc = EntityQuery.use(delegator).from(tempProductAssoc.getEntityName()).where(tempProductAssoc.getPrimaryKey()).queryOne();
            } catch (GenericEntityException e) {
                Debug.logWarning(e.getMessage(), module);
                productAssoc = null;
            }
            if (productAssoc != null) {
                errMsg = UtilProperties.getMessage(resource,"productevents.could_not_create_product_association_exists", UtilHttp.getLocale(request));
                request.setAttribute("_ERROR_MESSAGE_", errMsg);
                return "error";
            }
            try {
                productAssoc = tempProductAssoc.create();
            } catch (GenericEntityException e) {
                errMsg = UtilProperties.getMessage(resource,"productevents.could_not_create_product_association_write", UtilHttp.getLocale(request));
                request.setAttribute("_ERROR_MESSAGE_", errMsg);
                Debug.logWarning("[ProductEvents.updateProductAssoc] Could not create product association (write error); message: " + e.getMessage(), module);
                return "error";
            }
        } else if (updateMode.equals("UPDATE")) {
            try {
                tempProductAssoc.store();
            } catch (GenericEntityException e) {
                errMsg = UtilProperties.getMessage(resource,"productevents.could_not_update_product_association_write", UtilHttp.getLocale(request));
                request.setAttribute("_ERROR_MESSAGE_", errMsg);
                Debug.logWarning("[ProductEvents.updateProductAssoc] Could not update product association (write error); message: " + e.getMessage(), module);
                return "error";
            }
        } else {
            Map<String, String> messageMap = UtilMisc.toMap("updateMode", updateMode);
            errMsg = UtilProperties.getMessage(resource,"productevents.specified_update_mode_not_supported", messageMap, UtilHttp.getLocale(request));
            request.setAttribute("_ERROR_MESSAGE_", errMsg);
            return "error";
        }

        return "success";
    }

    /** Event to clear the last viewed categories */
    public static String clearLastViewedCategories(HttpServletRequest request, HttpServletResponse response) {
        // just store a new empty list in the session
        HttpSession session = request.getSession();
        if (session != null) {
            session.setAttribute("lastViewedCategories", new LinkedList());
        }
        return "success";
    }

    /** Event to clear the last vieweed products */
    public static String clearLastViewedProducts(HttpServletRequest request, HttpServletResponse response) {
        // just store a new empty list in the session
        HttpSession session = request.getSession();
        if (session != null) {
            session.setAttribute("lastViewedProducts", new LinkedList());
        }
        return "success";
    }

    /** Event to clear the last viewed history (products/categories/searchs) */
    public static String clearAllLastViewed(HttpServletRequest request, HttpServletResponse response) {
        ProductEvents.clearLastViewedCategories(request, response);
        ProductEvents.clearLastViewedProducts(request, response);
        ProductSearchSession.clearSearchOptionsHistoryList(request, response);
        return "success";
    }

    public static String updateProductQuickAdminShipping(HttpServletRequest request, HttpServletResponse response) {
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        Timestamp nowTimestamp = UtilDateTime.nowTimestamp();
        GenericValue userLogin = (GenericValue) request.getSession().getAttribute("userLogin");
        String variantProductId = request.getParameter("productId0");

        boolean applyToAll = (request.getParameter("applyToAll") != null);

        try {
            boolean beganTransaction = TransactionUtil.begin();
            try {
                // check for variantProductId - this will mean that we have multiple ship info to update
                if (variantProductId == null) {
                    // only single product to update
                    String productId = request.getParameter("productId");
                    GenericValue product = EntityQuery.use(delegator).from("Product").where("productId", productId).queryOne();
                    product.set("lastModifiedDate", nowTimestamp);
                    product.setString("lastModifiedByUserLogin", userLogin.getString("userLoginId"));
                    try {
                        product.set("productHeight", parseBigDecimalForEntity(request.getParameter("productHeight")));
                        product.set("productWidth", parseBigDecimalForEntity(request.getParameter("productWidth")));
                        product.set("productDepth", parseBigDecimalForEntity(request.getParameter("productDepth")));
                        product.set("productWeight", parseBigDecimalForEntity(request.getParameter("weight")));

                        // default unit settings for shipping parameters
                        product.set("heightUomId", "LEN_in");
                        product.set("widthUomId", "LEN_in");
                        product.set("depthUomId", "LEN_in");
                        product.set("weightUomId", "WT_oz");

                        BigDecimal floz = parseBigDecimalForEntity(request.getParameter("~floz"));
                        BigDecimal ml = parseBigDecimalForEntity(request.getParameter("~ml"));
                        BigDecimal ntwt = parseBigDecimalForEntity(request.getParameter("~ntwt"));
                        BigDecimal grams = parseBigDecimalForEntity(request.getParameter("~grams"));

                        List<GenericValue> currentProductFeatureAndAppls = EntityQuery.use(delegator).from("ProductFeatureAndAppl").where("productId", productId, "productFeatureApplTypeId", "STANDARD_FEATURE").filterByDate().queryList();
                        setOrCreateProdFeature(delegator, productId, currentProductFeatureAndAppls, "VLIQ_ozUS", "AMOUNT", floz);
                        setOrCreateProdFeature(delegator, productId, currentProductFeatureAndAppls, "VLIQ_ml", "AMOUNT", ml);
                        setOrCreateProdFeature(delegator, productId, currentProductFeatureAndAppls, "WT_g", "AMOUNT", grams);
                        setOrCreateProdFeature(delegator, productId, currentProductFeatureAndAppls, "WT_oz", "AMOUNT", ntwt);
                        product.store();

                    } catch (NumberFormatException nfe) {
                        String errMsg = "Shipping Dimensions and Weights must be numbers.";
                        request.setAttribute("_ERROR_MESSAGE_", errMsg);
                        Debug.logError(nfe, errMsg, module);
                        return "error";
                    }
                } else {
                    // multiple products, so use a numeric suffix to get them all
                    int prodIdx = 0;
                    int attribIdx = 0;
                    String productId = variantProductId;
                    do {
                        GenericValue product = EntityQuery.use(delegator).from("Product").where("productId", productId).queryOne();
                        try {
                            product.set("productHeight", parseBigDecimalForEntity(request.getParameter("productHeight" + attribIdx)));
                            product.set("productWidth", parseBigDecimalForEntity(request.getParameter("productWidth" + attribIdx)));
                            product.set("productDepth", parseBigDecimalForEntity(request.getParameter("productDepth" + attribIdx)));
                            product.set("productWeight", parseBigDecimalForEntity(request.getParameter("weight" + attribIdx)));
                            BigDecimal floz = parseBigDecimalForEntity(request.getParameter("~floz" + attribIdx));
                            BigDecimal ml = parseBigDecimalForEntity(request.getParameter("~ml" + attribIdx));
                            BigDecimal ntwt = parseBigDecimalForEntity(request.getParameter("~ntwt" + attribIdx));
                            BigDecimal grams = parseBigDecimalForEntity(request.getParameter("~grams" + attribIdx));

                            List<GenericValue> currentProductFeatureAndAppls = EntityQuery.use(delegator).from("ProductFeatureAndAppl").where("productId", productId, "productFeatureApplTypeId", "STANDARD_FEATURE").filterByDate().queryList();
                            setOrCreateProdFeature(delegator, productId, currentProductFeatureAndAppls, "VLIQ_ozUS", "AMOUNT", floz);
                            setOrCreateProdFeature(delegator, productId, currentProductFeatureAndAppls, "VLIQ_ml", "AMOUNT", ml);
                            setOrCreateProdFeature(delegator, productId, currentProductFeatureAndAppls, "WT_g", "AMOUNT", grams);
                            setOrCreateProdFeature(delegator, productId, currentProductFeatureAndAppls, "WT_oz", "AMOUNT", ntwt);
                            product.store();
                        } catch (NumberFormatException nfe) {
                            String errMsg = "Shipping Dimensions and Weights must be numbers.";
                            request.setAttribute("_ERROR_MESSAGE_", errMsg);
                            Debug.logError(nfe, errMsg, module);
                            return "error";
                        }
                        prodIdx++;
                        if (!applyToAll) {
                            attribIdx = prodIdx;
                        }
                        productId = request.getParameter("productId" + prodIdx);
                    } while (productId != null);
                }
                TransactionUtil.commit(beganTransaction);
            } catch (GenericEntityException e) {
                String errMsg = "Error updating quick admin shipping settings: " + e.toString();
                Debug.logError(e, errMsg, module);
                request.setAttribute("_ERROR_MESSAGE_", errMsg);
                TransactionUtil.rollback(beganTransaction, errMsg, e);
                return "error";
            }
        } catch (GenericTransactionException gte) {
            String errMsg = "Error updating quick admin shipping settings: " + gte.toString();
            Debug.logError(gte, errMsg, module);
            request.setAttribute("_ERROR_MESSAGE_", errMsg);
            return "error";
        }
        return "success";
    }

    /**
     * find a specific feature in a given list, then update it or create it if it doesn't exist.
     * @param delegator
     * @param productId
     * @param existingFeatures
     * @param uomId
     * @param productFeatureTypeId
     * @param numberSpecified
     * @return
     * @throws GenericEntityException
     */
    private static void setOrCreateProdFeature(Delegator delegator, String productId, List<GenericValue> currentProductFeatureAndAppls,
                                          String uomId, String productFeatureTypeId, BigDecimal numberSpecified) throws GenericEntityException {

        GenericValue productFeatureType = EntityQuery.use(delegator).from("ProductFeatureType").where("productFeatureTypeId", productFeatureTypeId).queryOne();
        GenericValue uom = EntityQuery.use(delegator).from("Uom").where("uomId", uomId).queryOne();

        Timestamp nowTimestamp = UtilDateTime.nowTimestamp();

        // filter list of features to the one we'll be editing
        List<GenericValue> typeUomProductFeatureAndApplList = EntityUtil.filterByAnd(currentProductFeatureAndAppls, UtilMisc.toMap("productFeatureTypeId", productFeatureTypeId, "uomId", uomId));

        // go through each; need to remove? do it now
        boolean foundOneEqual = false;
        for (GenericValue typeUomProductFeatureAndAppl: typeUomProductFeatureAndApplList) {
            if ((numberSpecified != null) && (numberSpecified.compareTo(typeUomProductFeatureAndAppl.getBigDecimal("numberSpecified")) == 0)) {
                foundOneEqual = true;
            } else {
                // remove the PFA...
                GenericValue productFeatureAppl = delegator.makeValidValue("ProductFeatureAppl", typeUomProductFeatureAndAppl);
                productFeatureAppl.remove();
            }
        }

        // NOTE: if numberSpecified is null then foundOneEqual will always be false, so need to check both
        if (numberSpecified != null && !foundOneEqual) {
            String productFeatureId = null;
            List<GenericValue> existingProductFeatureList = EntityQuery.use(delegator).from("ProductFeature").where("productFeatureTypeId", productFeatureTypeId, "numberSpecified", numberSpecified, "uomId", uomId).queryList();
            if (existingProductFeatureList.size() > 0) {
                GenericValue existingProductFeature = existingProductFeatureList.get(0);
                productFeatureId = existingProductFeature.getString("productFeatureId");
            } else {
                // doesn't exist, so create it
                productFeatureId = delegator.getNextSeqId("ProductFeature");
                GenericValue prodFeature = delegator.makeValue("ProductFeature", UtilMisc.toMap("productFeatureId", productFeatureId, "productFeatureTypeId", productFeatureTypeId));
                if (uomId != null) {
                    prodFeature.set("uomId", uomId);
                }
                prodFeature.set("numberSpecified", numberSpecified);
                prodFeature.set("description", numberSpecified.toString() + (uom == null ? "" : (" " + uom.getString("description"))));

                // if there is a productFeatureCategory with the same id as the productFeatureType, use that category.
                // otherwise, use a default category from the configuration
                if (EntityQuery.use(delegator).from("ProductFeatureCategory").where("productFeatureCategoryId", productFeatureTypeId).queryOne() == null) {
                    GenericValue productFeatureCategory = delegator.makeValue("ProductFeatureCategory");
                    productFeatureCategory.set("productFeatureCategoryId", productFeatureTypeId);
                    productFeatureCategory.set("description", productFeatureType.get("description"));
                    productFeatureCategory.create();
                }
                prodFeature.set("productFeatureCategoryId", productFeatureTypeId);
                prodFeature.create();
            }

            delegator.create("ProductFeatureAppl", UtilMisc.toMap("productId", productId, "productFeatureId", productFeatureId,
                    "productFeatureApplTypeId", "STANDARD_FEATURE", "fromDate", nowTimestamp));
        }
    }

    public static String updateProductQuickAdminSelFeat(HttpServletRequest request, HttpServletResponse response) {
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        Timestamp nowTimestamp = UtilDateTime.nowTimestamp();
        //GenericValue userLogin = (GenericValue) request.getSession().getAttribute("userLogin");
        String productId = request.getParameter("productId");
        String variantProductId = request.getParameter("productId0");
        String useImagesProdId = request.getParameter("useImages");
        String productFeatureTypeId = request.getParameter("productFeatureTypeId");

        if (UtilValidate.isEmpty(productFeatureTypeId)) {
            String errMsg = "Error: please select a ProductFeature Type to add or update variant features.";
            request.setAttribute("_ERROR_MESSAGE_", errMsg);
            return "error";
        }

        try {
            boolean beganTransaction = TransactionUtil.begin();
            try {
                GenericValue productFeatureType = EntityQuery.use(delegator).from("ProductFeatureType").where("productFeatureTypeId", productFeatureTypeId).queryOne();
                if (productFeatureType == null) {
                    String errMsg = "Error: the ProductFeature Type specified was not valid and one is require to add or update variant features.";
                    request.setAttribute("_ERROR_MESSAGE_", errMsg);
                    return "error";
                }

                // check for variantProductId - this will mean that we have multiple variants to update
                if (variantProductId != null) {
                    // multiple products, so use a numeric suffix to get them all
                    int attribIdx = 0;
                    GenericValue product = EntityQuery.use(delegator).from("Product").where("productId", productId).queryOne();
                    do {
                        GenericValue variantProduct = EntityQuery.use(delegator).from("Product").where("productId", variantProductId).queryOne();
                        String description = request.getParameter("description" + attribIdx);
                        // blank means null, which means delete the feature application
                        if ((description != null) && (description.trim().length() < 1)) {
                            description = null;
                        }

                        Set<String> variantDescRemoveToRemoveOnVirtual = new HashSet<String>();
                        checkUpdateFeatureApplByDescription(variantProductId, variantProduct, description, productFeatureTypeId, productFeatureType, "STANDARD_FEATURE", nowTimestamp, delegator, null, variantDescRemoveToRemoveOnVirtual);
                        checkUpdateFeatureApplByDescription(productId, product, description, productFeatureTypeId, productFeatureType, "SELECTABLE_FEATURE", nowTimestamp, delegator, variantDescRemoveToRemoveOnVirtual, null);

                        // update image urls
                        if ((useImagesProdId != null) && (useImagesProdId.equals(variantProductId))) {
                            product.set("smallImageUrl", variantProduct.getString("smallImageUrl"));
                            product.set("mediumImageUrl", variantProduct.getString("mediumImageUrl"));
                            product.set("largeImageUrl", null);
                            product.set("detailImageUrl", null);
                            product.store();
                        }
                        attribIdx++;
                        variantProductId = request.getParameter("productId" + attribIdx);
                    } while (variantProductId != null);
                }

                TransactionUtil.commit(beganTransaction);
            } catch (GenericEntityException e) {
                String errMsg = "Error updating quick admin selectable feature settings: " + e.toString();
                Debug.logError(e, errMsg, module);
                request.setAttribute("_ERROR_MESSAGE_", errMsg);
                TransactionUtil.rollback(beganTransaction, errMsg, e);
                return "error";
            }
        } catch (GenericTransactionException gte) {
            String errMsg = "Error updating quick admin selectable feature settings: " + gte.toString();
            Debug.logError(gte, errMsg, module);
            request.setAttribute("_ERROR_MESSAGE_", errMsg);
            return "error";
        }
        return "success";
    }

    protected static void checkUpdateFeatureApplByDescription(String productId, GenericValue product, String description,
            String productFeatureTypeId, GenericValue productFeatureType, String productFeatureApplTypeId,
            Timestamp nowTimestamp, Delegator delegator, Set<String> descriptionsToRemove, Set<String> descriptionsRemoved) throws GenericEntityException {
        if (productFeatureType == null) {
            return;
        }

        GenericValue productFeatureAndAppl = null;

        Set<String> descriptionsForThisType = new HashSet<String>();
        List<GenericValue> productFeatureAndApplList = EntityQuery.use(delegator).from("ProductFeatureAndAppl").where("productId", productId, "productFeatureApplTypeId", productFeatureApplTypeId, "productFeatureTypeId", productFeatureTypeId).filterByDate().queryList();
        if (productFeatureAndApplList.size() > 0) {
            Iterator<GenericValue> productFeatureAndApplIter = productFeatureAndApplList.iterator();
            while (productFeatureAndApplIter.hasNext()) {
                productFeatureAndAppl = productFeatureAndApplIter.next();
                GenericValue productFeatureAppl = delegator.makeValidValue("ProductFeatureAppl", productFeatureAndAppl);

                // remove productFeatureAppl IFF: productFeatureAppl != null && (description is empty/null || description is different than existing)
                if (productFeatureAppl != null && (description == null || !description.equals(productFeatureAndAppl.getString("description")))) {
                    // if descriptionsToRemove is not null, only remove if description is in that set
                    if (descriptionsToRemove == null || (descriptionsToRemove != null && descriptionsToRemove.contains(productFeatureAndAppl.getString("description")))) {
                        // okay, almost there: before removing it if this is a virtual product check to make SURE this feature's description doesn't exist on any of the variants; wouldn't want to remove something we should have kept around...
                        if ("Y".equals(product.getString("isVirtual"))) {
                            boolean foundFeatureOnVariant = false;
                            // get/check all the variants
                            List<GenericValue> variantAssocs = product.getRelated("MainProductAssoc", UtilMisc.toMap("productAssocTypeId", "PRODUCT_VARIANT"), null, false);
                            variantAssocs = EntityUtil.filterByDate(variantAssocs);
                            List<GenericValue> variants = EntityUtil.getRelated("AssocProduct", null, variantAssocs, false);
                            Iterator<GenericValue> variantIter = variants.iterator();
                            while (!foundFeatureOnVariant && variantIter.hasNext()) {
                                GenericValue variant = variantIter.next();
                                // get the selectable features for the variant
                                List<GenericValue> variantProductFeatureAndAppls = variant.getRelated("ProductFeatureAndAppl", UtilMisc.toMap("productFeatureTypeId", productFeatureTypeId, "productFeatureApplTypeId", "STANDARD_FEATURE", "description", description), null, false);
                                if (variantProductFeatureAndAppls.size() > 0) {
                                    foundFeatureOnVariant = true;
                                }
                            }

                            if (foundFeatureOnVariant) {
                                // don't remove this one!
                                continue;
                            }
                        }

                        if (descriptionsRemoved != null) {
                            descriptionsRemoved.add(productFeatureAndAppl.getString("description"));
                        }
                        productFeatureAppl.remove();
                        continue;
                    }
                }

                // we got here, is still a valid description associated with this product
                descriptionsForThisType.add(productFeatureAndAppl.getString("description"));
            }
        }

        if (description != null && (productFeatureAndAppl == null || (productFeatureAndAppl != null && !descriptionsForThisType.contains(description)))) {
            // need to add an appl, and possibly the feature

            // see if a feature exists with the type and description specified (if doesn't exist will create later)
            String productFeatureId = null;
            List<GenericValue> existingProductFeatureList = EntityQuery.use(delegator).from("ProductFeature").where("productFeatureTypeId", productFeatureTypeId, "description", description).queryList();
            if (existingProductFeatureList.size() > 0) {
                GenericValue existingProductFeature = existingProductFeatureList.get(0);
                productFeatureId = existingProductFeature.getString("productFeatureId");
            } else {
                // doesn't exist, so create it
                productFeatureId = delegator.getNextSeqId("ProductFeature");
                GenericValue newProductFeature = delegator.makeValue("ProductFeature",
                        UtilMisc.toMap("productFeatureId", productFeatureId,
                                "productFeatureTypeId", productFeatureTypeId,
                                "description", description));

                // if there is a productFeatureCategory with the same id as the productFeatureType, use that category.
                // otherwise, create a category for the feature type
                if (EntityQuery.use(delegator).from("ProductFeatureCategory").where("productFeatureCategoryId", productFeatureTypeId).queryOne() == null) {
                    GenericValue productFeatureCategory = delegator.makeValue("ProductFeatureCategory");
                    productFeatureCategory.set("productFeatureCategoryId", productFeatureTypeId);
                    productFeatureCategory.set("description", productFeatureType.get("description"));
                    productFeatureCategory.create();
                }
                newProductFeature.set("productFeatureCategoryId", productFeatureTypeId);
                newProductFeature.create();
            }

            // check to see if the productFeatureId is already attached to the virtual or variant, if not attach them...
            List<GenericValue> specificProductFeatureApplList = EntityQuery.use(delegator).from("ProductFeatureAppl").where("productId", productId, "productFeatureApplTypeId", productFeatureApplTypeId, "productFeatureId", productFeatureId).filterByDate().queryList();

            if (specificProductFeatureApplList.size() == 0) {
                delegator.create("ProductFeatureAppl",
                        UtilMisc.toMap("productId", productId,
                                "productFeatureId", productFeatureId,
                                "productFeatureApplTypeId", productFeatureApplTypeId,
                                "fromDate", nowTimestamp));
            }
        }
    }

    public static String removeFeatureApplsByFeatureTypeId(HttpServletRequest request, HttpServletResponse response) {
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        //Timestamp nowTimestamp = UtilDateTime.nowTimestamp();
        //GenericValue userLogin = (GenericValue) request.getSession().getAttribute("userLogin");
        String productId = request.getParameter("productId");
        String productFeatureTypeId = request.getParameter("productFeatureTypeId");

        try {
            GenericValue product = EntityQuery.use(delegator).from("Product").where("productId", productId).queryOne();
            // get all the variants
            List<GenericValue> variantAssocs = product.getRelated("MainProductAssoc", UtilMisc.toMap("productAssocTypeId", "PRODUCT_VARIANT"), null, false);
            variantAssocs = EntityUtil.filterByDate(variantAssocs);
            List<GenericValue> variants = EntityUtil.getRelated("AssocProduct", null, variantAssocs, false);
            for (GenericValue variant: variants) {
                // get the selectable features for the variant
                List<GenericValue> productFeatureAndAppls = variant.getRelated("ProductFeatureAndAppl", UtilMisc.toMap("productFeatureTypeId", productFeatureTypeId, "productFeatureApplTypeId", "STANDARD_FEATURE"), null, false);
                for (GenericValue productFeatureAndAppl: productFeatureAndAppls) {
                    GenericPK productFeatureApplPK = delegator.makePK("ProductFeatureAppl");
                    productFeatureApplPK.setPKFields(productFeatureAndAppl);
                    delegator.removeByPrimaryKey(productFeatureApplPK);
                }
            }
            List<GenericValue> productFeatureAndAppls = product.getRelated("ProductFeatureAndAppl", UtilMisc.toMap("productFeatureTypeId", productFeatureTypeId, "productFeatureApplTypeId", "SELECTABLE_FEATURE"), null, false);
            for (GenericValue productFeatureAndAppl: productFeatureAndAppls) {
                GenericPK productFeatureApplPK = delegator.makePK("ProductFeatureAppl");
                productFeatureApplPK.setPKFields(productFeatureAndAppl);
                delegator.removeByPrimaryKey(productFeatureApplPK);
            }
        } catch (GenericEntityException e) {
            String errMsg = "Error creating new virtual product from variant products: " + e.toString();
            request.setAttribute("_ERROR_MESSAGE_", errMsg);
            return "error";
        }
        return "success";
    }

    public static String removeProductFeatureAppl(HttpServletRequest request, HttpServletResponse response) {
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        //Timestamp nowTimestamp = UtilDateTime.nowTimestamp();
        //GenericValue userLogin = (GenericValue) request.getSession().getAttribute("userLogin");
        String productId = request.getParameter("productId");
        String productFeatureId = request.getParameter("productFeatureId");

        if (UtilValidate.isEmpty(productId) || UtilValidate.isEmpty(productFeatureId)) {
            String errMsg = "Must specify both a productId [was:" + productId + "] and a productFeatureId [was:" + productFeatureId + "] to remove the feature from the product.";
            request.setAttribute("_ERROR_MESSAGE_", errMsg);
            return "error";
        }

        try {
            delegator.removeByAnd("ProductFeatureAppl", UtilMisc.toMap("productFeatureId", productFeatureId, "productId", productId));
        } catch (GenericEntityException e) {
            String errMsg = "Error removing product feature: " + e.toString();
            request.setAttribute("_ERROR_MESSAGE_", errMsg);
            return "error";
        }
        return "success";
    }

    public static String addProductToCategories(HttpServletRequest request, HttpServletResponse response) {
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        String productId = request.getParameter("productId");
        String fromDateStr = request.getParameter("fromDate");
        Timestamp fromDate = UtilDateTime.nowTimestamp();
        if (UtilValidate.isNotEmpty(fromDateStr)) {
            fromDate = Timestamp.valueOf(fromDateStr);
        }
        String[] categoryIds = request.getParameterValues("categoryId");
        if (categoryIds != null) {
            for (String categoryId: categoryIds) {
                try {
                    List<GenericValue> catMembs = EntityQuery.use(delegator).from("ProductCategoryMember").where("productCategoryId", categoryId, "productId", productId).filterByDate().queryList();
                    if (catMembs.size() == 0) {
                        delegator.create("ProductCategoryMember",
                                UtilMisc.toMap("productCategoryId", categoryId, "productId", productId, "fromDate", fromDate));
                    }
                } catch (GenericEntityException e) {
                    String errMsg = "Error adding to category: " + e.toString();
                    request.setAttribute("_ERROR_MESSAGE_", errMsg);
                    return "error";
                }

            }
        }
        return "success";
    }

    public static String updateProductCategoryMember(HttpServletRequest request, HttpServletResponse response) {
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        String productId = request.getParameter("productId");
        String productCategoryId = request.getParameter("productCategoryId");
        String thruDate = request.getParameter("thruDate");
        if ((thruDate == null) || (thruDate.trim().length() == 0)) {
            thruDate = UtilDateTime.nowTimestamp().toString();
        }
        try {
            List<GenericValue> prodCatMembs = EntityQuery.use(delegator).from("ProductCategoryMember").where("productCategoryId", productCategoryId, "productId", productId).filterByDate().queryList();
            if (prodCatMembs.size() > 0) {
                // there is one to modify
                GenericValue prodCatMemb = prodCatMembs.get(0);
                prodCatMemb.setString("thruDate", thruDate);
                prodCatMemb.store();
            }

        } catch (GenericEntityException e) {
            String errMsg = "Error adding to category: " + e.toString();
            request.setAttribute("_ERROR_MESSAGE_", errMsg);
            return "error";
        }
        return "success";
    }

    public static String addProductFeatures(HttpServletRequest request, HttpServletResponse response) {
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        String productId = request.getParameter("productId");
        String productFeatureApplTypeId = request.getParameter("productFeatureApplTypeId");
        String fromDateStr = request.getParameter("fromDate");
        Timestamp fromDate = UtilDateTime.nowTimestamp();
        if (UtilValidate.isNotEmpty(fromDateStr)) {
            fromDate = Timestamp.valueOf(fromDateStr);
        }
        String[] productFeatureIdArray = request.getParameterValues("productFeatureId");
        if (productFeatureIdArray != null && productFeatureIdArray.length > 0) {
            try {
                for (String productFeatureId: productFeatureIdArray) {
                    if (!productFeatureId.equals("~~any~~")) {
                        List<GenericValue> featureAppls = EntityQuery.use(delegator).from("ProductFeatureAppl").where("productId", productId, "productFeatureId", productFeatureId, "productFeatureApplTypeId", productFeatureApplTypeId).queryList();
                        if (featureAppls.size() == 0) {
                            // no existing application for this
                            delegator.create("ProductFeatureAppl",
                                    UtilMisc.toMap("productId", productId,
                                        "productFeatureId", productFeatureId,
                                        "productFeatureApplTypeId", productFeatureApplTypeId,
                                        "fromDate", fromDate));
                        }
                    }
                }
            } catch (GenericEntityException e) {
                String errMsg = "Error adding feature: " + e.toString();
                request.setAttribute("_ERROR_MESSAGE_", errMsg);
                return "error";
            }
        }
        return "success";
    }

    /** Simple event to set the users initial locale and currency Uom based on website product store */
    public static String setDefaultStoreSettings(HttpServletRequest request, HttpServletResponse response) {
        GenericValue productStore = ProductStoreWorker.getProductStore(request);
        if (productStore != null) {
            String currencyStr = null;
            String localeStr = null;
            String timeZoneStr = null;

            HttpSession session = request.getSession();
            GenericValue userLogin = (GenericValue) session.getAttribute("userLogin");
            if (userLogin != null) {
                // user login currency
                currencyStr = userLogin.getString("lastCurrencyUom");
                // user login locale
                localeStr = userLogin.getString("lastLocale");
                // user login timezone
                timeZoneStr = userLogin.getString("lastTimeZone");
            }

            // if currency is not set, the store's default currency is used
            if (currencyStr == null && productStore.get("defaultCurrencyUomId") != null) {
                currencyStr = productStore.getString("defaultCurrencyUomId");
            }

            // if locale is not set, the store's default locale is used
            if (localeStr == null && productStore.get("defaultLocaleString") != null) {
                localeStr = productStore.getString("defaultLocaleString");
            }
            
            // if timezone is not set, the store's default timezone is used
            if (timeZoneStr == null && productStore.get("defaultTimeZoneString") != null) {
                timeZoneStr = productStore.getString("defaultTimeZoneString");
            }

            UtilHttp.setCurrencyUom(session, currencyStr);
            UtilHttp.setLocale(request, localeStr);
            UtilHttp.setTimeZone(request, timeZoneStr);
        }
        return "success";
    }

    /**
     * If ProductStore.requireCustomerRole == Y then the loggedin user must be associated with the store in the customer role.
     * This event method is called from the ProductEvents.storeCheckLogin and ProductEvents.storeLogin
     *
     * @param request
     * @param response
     * @return String with response, maybe "success" or "error" if logged in user is not associated with the ProductStore in the CUSTOMER role.
     */
    public static String checkStoreCustomerRole(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession();
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        GenericValue userLogin = (GenericValue) session.getAttribute("userLogin");
        GenericValue productStore = ProductStoreWorker.getProductStore(request);
        if (productStore != null && userLogin != null) {
            if ("Y".equals(productStore.getString("requireCustomerRole"))) {
                List<GenericValue> productStoreRoleList = null;
                try {
                    productStoreRoleList = EntityQuery.use(delegator).from("ProductStoreRole").where("productStoreId", productStore.get("productStoreId"), "partyId", userLogin.get("partyId"), "roleTypeId", "CUSTOMER").filterByDate().queryList();
                } catch (GenericEntityException e) {
                    Debug.logError(e, "Database error finding CUSTOMER ProductStoreRole records, required by the ProductStore with ID [" + productStore.getString("productStoreId") + "]", module);
                }
                if (UtilValidate.isEmpty(productStoreRoleList)) {
                    // uh-oh, this user isn't associated...
                    String errorMsg = "The " + productStore.getString("storeName") + " [" + productStore.getString("productStoreId") + "] ProductStore requires that customers be associated with it, and the logged in user is NOT associated with it in the CUSTOMER role; userLoginId=[" + userLogin.getString("userLoginId") + "], partyId=[" + userLogin.getString("partyId") + "]";
                    Debug.logWarning(errorMsg, module);
                    request.setAttribute("_ERROR_MESSAGE_", errorMsg);
                    session.removeAttribute("userLogin");
                    session.removeAttribute("autoUserLogin");
                    return "error";
                }
            }
        }
        return "success";
    }

    public static String tellAFriend(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession();
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        String emailType = "PRDS_TELL_FRIEND";
        String defaultScreenLocation = "component://ecommerce/widget/EmailProductScreens.xml#TellFriend";

        GenericValue productStore = ProductStoreWorker.getProductStore(request);
        if (productStore == null) {
            String errMsg = "Could not send tell a friend email, no ProductStore found";
            request.setAttribute("_ERROR_MESSAGE_", errMsg);
            return "error";
        }
        String productStoreId = productStore.getString("productStoreId");

        GenericValue productStoreEmail = null;
        try {
            productStoreEmail = EntityQuery.use(delegator).from("ProductStoreEmailSetting").where("productStoreId", productStoreId, "emailType", emailType).queryOne();
        } catch (GenericEntityException e) {
            String errMsg = "Unable to get product store email setting for tell-a-friend: " + e.toString();
            Debug.logError(e, errMsg, module);
            request.setAttribute("_ERROR_MESSAGE_", errMsg);
            return "error";
        }
        if (productStoreEmail == null) {
            String errMsg = "Could not find tell a friend [" + emailType + "] email settings for the store [" + productStoreId + "]";
            request.setAttribute("_ERROR_MESSAGE_", errMsg);
            return "error";
        }

        String bodyScreenLocation = productStoreEmail.getString("bodyScreenLocation");
        if (UtilValidate.isEmpty(bodyScreenLocation)) {
            bodyScreenLocation = defaultScreenLocation;
        }

        Map<String, Object> paramMap = UtilHttp.getParameterMap(request);
        String websiteId = (String) paramMap.get("websiteId");
        if (UtilValidate.isEmpty(websiteId)) {
            websiteId = WebSiteWorker.getWebSiteId(request);
        }
        paramMap.put("locale", UtilHttp.getLocale(request));
        paramMap.put("userLogin", session.getAttribute("userLogin"));

        Map<String, Object> context = new HashMap<String, Object>();
        context.put("bodyScreenUri", bodyScreenLocation);
        context.put("bodyParameters", paramMap);
        context.put("sendTo", paramMap.get("sendTo"));
        context.put("contentType", productStoreEmail.get("contentType"));
        context.put("sendFrom", productStoreEmail.get("fromAddress"));
        context.put("sendCc", productStoreEmail.get("ccAddress"));
        context.put("sendBcc", productStoreEmail.get("bccAddress"));
        context.put("subject", productStoreEmail.getString("subject"));
        context.put("webSiteId", websiteId);

        try {
            dispatcher.runAsync("sendMailFromScreen", context);
        } catch (GenericServiceException e) {
            String errMsg = "Problem sending mail: " + e.toString();
            Debug.logError(e, errMsg, module);
            request.setAttribute("_ERROR_MESSAGE_", errMsg);
            return "error";
        }
        return "success";
    }

    @Deprecated
    public static Double parseDoubleForEntity(String doubleString) throws NumberFormatException {
        if (doubleString == null) {
            return null;
        }
        doubleString = doubleString.trim();
        doubleString = doubleString.replaceAll(",", "");
        if (doubleString.length() < 1) {
            return null;
        }
        return Double.valueOf(doubleString);
    }

    public static List<GenericValue> getProductCompareList(HttpServletRequest request) {
        HttpSession session = request.getSession();
        Object compareListObj = session.getAttribute("productCompareList");
        List<GenericValue> compareList = null;
        if (compareListObj == null) {
            compareList = new LinkedList<GenericValue>();
        } else if (!(compareListObj instanceof List<?>)) {
            Debug.logWarning("Session attribute productCompareList contains something other than the expected product list, overwriting.", module);
            compareList = new LinkedList<GenericValue>();
        } else {
            compareList = UtilGenerics.cast(compareListObj);
        }
        return compareList;
    }

    public static String addProductToComparisonList(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession();
        Delegator delegator = (Delegator) request.getAttribute("delegator");

        String productId = request.getParameter("productId");
        GenericValue product = null;
        if (UtilValidate.isNotEmpty(productId)) {
            try {
                product = ProductWorker.findProduct(delegator, productId);
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
            }
        }

        if (product == null) {
            String errMsg = UtilProperties.getMessage(resource, "productevents.product_with_id_not_found", UtilMisc.toMap("productId", productId), UtilHttp.getLocale(request));
            request.setAttribute("_ERROR_MESSAGE_", errMsg);
            return "error";
        }

        List<GenericValue> compareList = getProductCompareList(request);
        boolean alreadyInList = false;
        for (GenericValue compProduct : compareList) {
            if (product.getString("productId").equals(compProduct.getString("productId"))) {
                alreadyInList = true;
                break;
            }
        }
        if (!alreadyInList) {
            compareList.add(product);
        }
        session.setAttribute("productCompareList", compareList);
        String productName = ProductContentWrapper.getProductContentAsText(product, "PRODUCT_NAME", request, "html");
        String eventMsg = UtilProperties.getMessage("ProductUiLabels", "ProductAddToCompareListSuccess", UtilMisc.toMap("name", productName), UtilHttp.getLocale(request));
        request.setAttribute("_EVENT_MESSAGE_", eventMsg);
        return "success";
    }

    public static String removeProductFromComparisonList(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession();
        Delegator delegator = (Delegator) request.getAttribute("delegator");

        String productId = request.getParameter("productId");
        GenericValue product = null;
        if (UtilValidate.isNotEmpty(productId)) {
            try {
                product = ProductWorker.findProduct(delegator, productId);
            } catch (GenericEntityException e) {
                productId =  null;
                Debug.logError(e, module);
            }
        }

        if (product == null) {
            String errMsg = UtilProperties.getMessage(resource, "productevents.product_with_id_not_found", UtilMisc.toMap("productId", productId), UtilHttp.getLocale(request));
            request.setAttribute("_ERROR_MESSAGE_", errMsg);
            return "error";
        }

        List<GenericValue> compareList = getProductCompareList(request);
        Iterator<GenericValue> it = compareList.iterator();
        while (it.hasNext()) {
            GenericValue compProduct = it.next();
            if (product.getString("productId").equals(compProduct.getString("productId"))) {
                it.remove();
                break;
            }
        }
        session.setAttribute("productCompareList", compareList);
        String productName = ProductContentWrapper.getProductContentAsText(product, "PRODUCT_NAME", request, "html");
        String eventMsg = UtilProperties.getMessage("ProductUiLabels", "ProductRemoveFromCompareListSuccess", UtilMisc.toMap("name", productName), UtilHttp.getLocale(request));
        request.setAttribute("_EVENT_MESSAGE_", eventMsg);
        return "success";
    }

    public static String clearProductComparisonList(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession();
        session.setAttribute("productCompareList", new LinkedList());
        String eventMsg = UtilProperties.getMessage("ProductUiLabels", "ProductClearCompareListSuccess", UtilHttp.getLocale(request));
        request.setAttribute("_EVENT_MESSAGE_", eventMsg);
        return "success";
    }

    /**
     * Return nulls for empty strings, as the entity engine can deal with nulls. This will provide blanks
     * in fields where BigDecimal display. Blank meaning null, vs. 0 which means 0
     * @param bigDecimalString
     * @return a BigDecimal for the parsed value
     */
    public static BigDecimal parseBigDecimalForEntity(String bigDecimalString) throws NumberFormatException {
        if (bigDecimalString == null) {
            return null;
        }
        bigDecimalString = bigDecimalString.trim();
        bigDecimalString = bigDecimalString.replaceAll(",", "");
        if (bigDecimalString.length() < 1) {
            return null;
        }
        return new BigDecimal(bigDecimalString);
    }
    
    /** Event add product tags */
    public static String addProductTags (HttpServletRequest request, HttpServletResponse response) {
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        String productId = request.getParameter("productId");
        String productTags = request.getParameter("productTags");
        String statusId = request.getParameter("statusId");
        if (UtilValidate.isNotEmpty(productId) && UtilValidate.isNotEmpty(productTags)) {
            List<String> matchList = new LinkedList<String>();
            Pattern regex = Pattern.compile("[^\\s\"']+|\"([^\"]*)\"|'([^']*)'");
            Matcher regexMatcher = regex.matcher(productTags);
            while (regexMatcher.find()) {
                matchList.add(regexMatcher.group().replace("'", ""));
            }
            
            GenericValue userLogin = null;
            try {
                userLogin = EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "system").cache().queryOne();
            } catch (GenericEntityException e) {
                request.setAttribute("_ERROR_MESSAGE_", e.getMessage());
                return "error";
            }
            
            if(UtilValidate.isEmpty(statusId)) {
                statusId = "KW_PENDING";
            }
            
            if(UtilValidate.isNotEmpty(matchList)) {
                for (String keywordStr : matchList) {
                    try {
                        dispatcher.runSync("createProductKeyword", UtilMisc.toMap("productId", productId, "keyword", keywordStr.trim(), "keywordTypeId", "KWT_TAG", "statusId", statusId, "userLogin", userLogin));
                    } catch (GenericServiceException e) {
                        request.setAttribute("_ERROR_MESSAGE_", e.getMessage());
                        return "error";
                    }
                }
            }
        }
        return "success";
    }
}
