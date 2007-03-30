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

import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import javolution.util.FastMap;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilParse;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericPK;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityConditionList;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.transaction.GenericTransactionException;
import org.ofbiz.entity.transaction.TransactionUtil;
import org.ofbiz.entity.util.EntityListIterator;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.product.store.ProductStoreWorker;
import org.ofbiz.security.Security;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;

/**
 * Product Information Related Events
 */
public class ProductEvents {

    public static final String module = ProductEvents.class.getName();
    public static final String resource = "ProductUiLabels";

    /**
     * Updates ProductKeyword information according to UPDATE_MODE parameter, only support CREATE and DELETE, no modify becuse all fields are PKs
     *
     * @param request
     *                The HTTPRequest object for the current request
     * @param response
     *                The HTTPResponse object for the current request
     * @return String specifying the exit status of this event
     */
    public static String updateProductKeyword(HttpServletRequest request, HttpServletResponse response) {
        GenericDelegator delegator = (GenericDelegator) request.getAttribute("delegator");
        Security security = (Security) request.getAttribute("security");
        List errMsgList = new LinkedList();

        String updateMode = request.getParameter("UPDATE_MODE");

        if (updateMode == null || updateMode.length() <= 0) {
            String errMsg = UtilProperties.getMessage(resource,"productevents.updatemode_not_specified", UtilHttp.getLocale(request));
            request.setAttribute("_ERROR_MESSAGE_", errMsg);
            Debug.logWarning("[ProductEvents.updateProductKeyword] Update Mode was not specified, but is required", module);
            return "error";
        }

        // check permissions before moving on...
        if (!security.hasEntityPermission("CATALOG", "_" + updateMode, request.getSession())) {
            Map messageMap = UtilMisc.toMap("updateMode", updateMode);
            String errMsg = UtilProperties.getMessage(resource,"productevents.not_sufficient_permissions", messageMap, UtilHttp.getLocale(request));
            request.setAttribute("_ERROR_MESSAGE_", errMsg);
            return "error";
        }

        String productId = request.getParameter("PRODUCT_ID");
        String keyword = request.getParameter("KEYWORD");
        String relevancyWeightString = request.getParameter("relevancyWeight");
        Long relevancyWeight = null;
        if (UtilValidate.isNotEmpty(relevancyWeightString)) {
            try {
                relevancyWeight = Long.valueOf(relevancyWeightString);
            } catch (NumberFormatException e) {
                String errMsg = "Bad format for relevancyWeight [" + relevancyWeightString + "]: " + e.toString();
                request.setAttribute("_ERROR_MESSAGE_", errMsg);
                return "error";
            }
        }

        String errMsgTemp = "";
        if (!UtilValidate.isNotEmpty(productId)) {
            errMsgList.add(UtilProperties.getMessage(resource,"productevents.product_ID_missing", UtilHttp.getLocale(request)));
        }
        if (!UtilValidate.isNotEmpty(keyword)) {
            errMsgList.add(UtilProperties.getMessage(resource,"productevents.keyword_missing", UtilHttp.getLocale(request)));
        }
        if (errMsgList.size() > 0) {
            request.setAttribute("_ERROR_MESSAGE_LIST_", errMsgList);
            return "error";
        }

        if (updateMode.equals("CREATE")) {
            keyword = keyword.toLowerCase();

            GenericValue productKeyword = delegator.makeValue("ProductKeyword", UtilMisc.toMap("productId", productId, "keyword", keyword, "relevancyWeight", relevancyWeight));
            GenericValue newValue = null;

            try {
                newValue = delegator.findByPrimaryKey(productKeyword.getPrimaryKey());
            } catch (GenericEntityException e) {
                Debug.logWarning(e.getMessage(), module);
                newValue = null;
            }

            if (newValue != null) {
                String errMsg = UtilProperties.getMessage(resource,"productevents.could_not_create_productkeyword_entry_exists", UtilHttp.getLocale(request));
                request.setAttribute("_ERROR_MESSAGE_", errMsg);
                return "error";
            }

            try {
                productKeyword = productKeyword.create();
            } catch (GenericEntityException e) {
                Debug.logWarning(e.getMessage(), module);
                productKeyword = null;
            }
            if (productKeyword == null) {
                String errMsg = UtilProperties.getMessage(resource,"productevents.could_not_create_productkeyword_entry_write", UtilHttp.getLocale(request));
                request.setAttribute("_ERROR_MESSAGE_", errMsg);
                return "error";
            }
        } else if (updateMode.equals("DELETE")) {
            GenericValue productKeyword = null;

            try {
                productKeyword = delegator.findByPrimaryKey("ProductKeyword", UtilMisc.toMap("productId", productId, "keyword", keyword));
            } catch (GenericEntityException e) {
                Debug.logWarning(e.getMessage(), module);
                productKeyword = null;
            }
            if (productKeyword == null) {
                String errMsg = UtilProperties.getMessage(resource,"productevents.could_not_remove_productkeyword_entry_notexists", UtilHttp.getLocale(request));
                request.setAttribute("_ERROR_MESSAGE_", errMsg);
                return "error";
            }
            try {
                productKeyword.remove();
            } catch (GenericEntityException e) {
                String errMsg = UtilProperties.getMessage(resource,"productevents.could_not_remove_productkeyword_entry_writeerror", UtilHttp.getLocale(request));
                request.setAttribute("_ERROR_MESSAGE_", errMsg);
                Debug.logWarning("[ProductEvents.updateProductKeyword] Could not remove product-keyword (write error); message: " + e.getMessage(), module);
                return "error";
            }
        } else {
            Map messageMap = UtilMisc.toMap("updateMode", updateMode);
            String errMsg = UtilProperties.getMessage(resource,"productevents.specified_update_mode_not_supported", messageMap, UtilHttp.getLocale(request));
            request.setAttribute("_ERROR_MESSAGE_", errMsg);
            return "error";
        }

        return "success";
    }

    /**
     * Update (create/induce or delete) all keywords for a given Product
     *
     * @param request
     *                The HTTPRequest object for the current request
     * @param response
     *                The HTTPResponse object for the current request
     * @return String specifying the exit status of this event
     */
    public static String updateProductKeywords(HttpServletRequest request, HttpServletResponse response) {
        String errMsg = "";
        GenericDelegator delegator = (GenericDelegator) request.getAttribute("delegator");
        Security security = (Security) request.getAttribute("security");

        String updateMode = request.getParameter("UPDATE_MODE");

        if (updateMode == null || updateMode.length() <= 0) {
            errMsg = UtilProperties.getMessage(resource,"productevents.updatemode_not_specified", UtilHttp.getLocale(request));
            request.setAttribute("_ERROR_MESSAGE_", errMsg);
            Debug.logWarning("[ProductEvents.updateProductKeywords] Update Mode was not specified, but is required", module);
            return "error";
        }

        // check permissions before moving on...
        if (!security.hasEntityPermission("CATALOG", "_" + updateMode, request.getSession())) {
            errMsg = UtilProperties.getMessage(resource,"productevents.not_sufficient_permissions", UtilHttp.getLocale(request));
            request.setAttribute( "_ERROR_MESSAGE_", errMsg);
            return "error";
        }

        String productId = request.getParameter("PRODUCT_ID");

        if (!UtilValidate.isNotEmpty(productId)) {
            errMsg = UtilProperties.getMessage(resource,"productevents.no_product_ID_specified_keywords", UtilHttp.getLocale(request));
            request.setAttribute("_ERROR_MESSAGE_", errMsg);
            return "error";
        }

        GenericValue product = null;

        try {
            product = delegator.findByPrimaryKey("Product", UtilMisc.toMap("productId", productId));
        } catch (GenericEntityException e) {
            Debug.logWarning(e.getMessage(), module);
            product = null;
        }
        if (product == null) {
            Map messageMap = UtilMisc.toMap("productId", productId);
            errMsg = UtilProperties.getMessage(resource,"productevents.product_with_productId_not_found_keywords", messageMap, UtilHttp.getLocale(request));
            request.setAttribute("_ERROR_MESSAGE_", errMsg);
            return "error";
        }

        if (updateMode.equals("CREATE")) {
            try {
                KeywordSearch.induceKeywords(product, true);
            } catch (GenericEntityException e) {
                errMsg = UtilProperties.getMessage(resource,"productevents.could_not_create_keywords_write", UtilHttp.getLocale(request));
                request.setAttribute("_ERROR_MESSAGE_", errMsg);
                return "error";
            }
        } else if (updateMode.equals("DELETE")) {
            try {
                product.removeRelated("ProductKeyword");
            } catch (GenericEntityException e) {
                errMsg = UtilProperties.getMessage(resource,"productevents.could_not_remove_keywords_write", UtilHttp.getLocale(request));
                request.setAttribute("_ERROR_MESSAGE_", errMsg);
                Debug.logWarning("[ProductEvents.updateProductKeywords] Could not remove product-keywords (write error); message: " + e.getMessage(), module);
                return "error";
            }
        } else {
            Map messageMap = UtilMisc.toMap("updateMode", updateMode);
            errMsg = UtilProperties.getMessage(resource,"productevents.specified_update_mode_not_supported", messageMap, UtilHttp.getLocale(request));
            request.setAttribute("_ERROR_MESSAGE_", errMsg);
            return "error";
        }

        return "success";
    }

    /**
     * Updates/adds keywords for all products
     *
     * @param request HTTPRequest object for the current request
     * @param response HTTPResponse object for the current request
     * @return String specifying the exit status of this event
     */
    public static String updateAllKeywords(HttpServletRequest request, HttpServletResponse response) {
        //String errMsg = "";
        GenericDelegator delegator = (GenericDelegator) request.getAttribute("delegator");
        Security security = (Security) request.getAttribute("security");
        Timestamp nowTimestamp = UtilDateTime.nowTimestamp();

        String updateMode = "CREATE";
        String errMsg=null;

        String doAll = request.getParameter("doAll");

        // check permissions before moving on...
        if (!security.hasEntityPermission("CATALOG", "_" + updateMode, request.getSession())) {
            Map messageMap = UtilMisc.toMap("updateMode", updateMode);
            errMsg = UtilProperties.getMessage(resource,"productevents.not_sufficient_permissions", messageMap, UtilHttp.getLocale(request));
            request.setAttribute("_ERROR_MESSAGE_", errMsg);
            return "error";
        }

        EntityCondition condition = null;
        if (!"Y".equals(doAll)) {
            List condList = new LinkedList();
            condList.add(new EntityExpr(new EntityExpr("autoCreateKeywords", EntityOperator.EQUALS, null), EntityOperator.OR, new EntityExpr("autoCreateKeywords", EntityOperator.NOT_EQUAL, "N")));
            if ("true".equals(UtilProperties.getPropertyValue("prodsearch", "index.ignore.variants"))) {
                condList.add(new EntityExpr(new EntityExpr("isVariant", EntityOperator.EQUALS, null), EntityOperator.OR, new EntityExpr("isVariant", EntityOperator.NOT_EQUAL, "Y")));
            }
            if ("true".equals(UtilProperties.getPropertyValue("prodsearch", "index.ignore.discontinued.sales"))) {
                condList.add(new EntityExpr(new EntityExpr("salesDiscontinuationDate", EntityOperator.EQUALS, null), EntityOperator.OR, new EntityExpr("salesDiscontinuationDate", EntityOperator.GREATER_THAN_EQUAL_TO, nowTimestamp)));
            }
            condition = new EntityConditionList(condList, EntityOperator.AND);
        } else {
            condition = new EntityExpr(new EntityExpr("autoCreateKeywords", EntityOperator.EQUALS, null), EntityOperator.OR, new EntityExpr("autoCreateKeywords", EntityOperator.NOT_EQUAL, "N"));
        }


        EntityListIterator entityListIterator = null;
        try {
            if (Debug.infoOn()) {
                long count = delegator.findCountByCondition("Product", condition, null);
                Debug.logInfo("========== Found " + count + " products to index ==========", module);
            }
            entityListIterator = delegator.findListIteratorByCondition("Product", condition, null, null);
        } catch (GenericEntityException gee) {
            Debug.logWarning(gee, gee.getMessage(), module);
            Map messageMap = UtilMisc.toMap("gee", gee.toString());
            errMsg = UtilProperties.getMessage(resource,"productevents.error_getting_product_list", messageMap, UtilHttp.getLocale(request));
            request.setAttribute("_ERROR_MESSAGE_", errMsg);
            return "error";
        }

        int numProds = 0;
        int errProds = 0;

        GenericValue product = null;
        while ((product = (GenericValue) entityListIterator.next()) != null) {
            try {
                KeywordSearch.induceKeywords(product, "Y".equals(doAll));
            } catch (GenericEntityException e) {
                errMsg = UtilProperties.getMessage(resource,"productevents.could_not_create_keywords_write", UtilHttp.getLocale(request));
                request.setAttribute("_ERROR_MESSAGE_", errMsg);
                Debug.logWarning("[ProductEvents.updateAllKeywords] Could not create product-keyword (write error); message: " + e.getMessage(), module);
                try {
                    entityListIterator.close();
                } catch (GenericEntityException gee) {
                    Debug.logError(gee, "Error closing EntityListIterator when indexing product keywords.", module);
                }
                errProds++;
            }
            numProds++;
            if (numProds % 500 == 0) {
                Debug.logInfo("Keywords indexed for " + numProds + " so far", module);
            }
        }

        if (entityListIterator != null) {
            try {
                entityListIterator.close();
            } catch (GenericEntityException gee) {
                Debug.logError(gee, "Error closing EntityListIterator when indexing product keywords.", module);
            }
        }

        if (errProds == 0) {
            Map messageMap = UtilMisc.toMap("numProds", Integer.toString(numProds));
            errMsg = UtilProperties.getMessage(resource,"productevents.keyword_creation_complete_for_products", messageMap, UtilHttp.getLocale(request));
            request.setAttribute("_EVENT_MESSAGE_", errMsg);
            return "success";
        } else {
            Map messageMap = UtilMisc.toMap("numProds", Integer.toString(numProds));
            messageMap.put("errProds", Integer.toString(errProds));
            errMsg = UtilProperties.getMessage(resource,"productevents.keyword_creation_complete_for_products_with_errors", messageMap, UtilHttp.getLocale(request));
            request.setAttribute( "_ERROR_MESSAGE_", errMsg);
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
        List errMsgList = new LinkedList();
        GenericDelegator delegator = (GenericDelegator) request.getAttribute("delegator");
        Security security = (Security) request.getAttribute("security");

        String updateMode = request.getParameter("UPDATE_MODE");

        if (updateMode == null || updateMode.length() <= 0) {
            errMsg = UtilProperties.getMessage(resource,"productevents.updatemode_not_specified", UtilHttp.getLocale(request));
            request.setAttribute("_ERROR_MESSAGE_", errMsg);
            Debug.logWarning("[ProductEvents.updateProductAssoc] Update Mode was not specified, but is required", module);
            return "error";
        }

        // check permissions before moving on...
        if (!security.hasEntityPermission("CATALOG", "_" + updateMode, request.getSession())) {
            Map messageMap = UtilMisc.toMap("updateMode", updateMode);
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
            if (delegator.findByPrimaryKey("Product", UtilMisc.toMap("productId", productId)) == null) {
                Map messageMap = UtilMisc.toMap("productId", productId);
                errMsgList.add(UtilProperties.getMessage(resource,"productevents.product_with_id_not_found", messageMap, UtilHttp.getLocale(request)));
            }
            if (delegator.findByPrimaryKey("Product", UtilMisc.toMap("productId", productIdTo)) == null) {
                Map messageMap = UtilMisc.toMap("productIdTo", productIdTo);
                errMsgList.add(UtilProperties.getMessage(resource,"productevents.product_To_with_id_not_found", messageMap, UtilHttp.getLocale(request)));
            }
        } catch (GenericEntityException e) {
            // if there is an exception for either, the other probably wont work
            Debug.logWarning(e, module);
        }

        if (UtilValidate.isNotEmpty(fromDateStr)) {
            try {
                fromDate = Timestamp.valueOf(fromDateStr);
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
                productAssoc = delegator.findByPrimaryKey(tempProductAssoc.getPrimaryKey());
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
        Double quantity = null;
        Long sequenceNum = null;

        if (UtilValidate.isNotEmpty(thruDateStr)) {
            try {
                thruDate = Timestamp.valueOf(thruDateStr);
            } catch (Exception e) {
                errMsgList.add(UtilProperties.getMessage(resource,"productevents.thru_date_not_formatted_correctly", UtilHttp.getLocale(request)));
            }
        }
        if (UtilValidate.isNotEmpty(quantityStr)) {
            try {
                quantity = Double.valueOf(quantityStr);
            } catch (Exception e) {
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
                productAssoc = delegator.findByPrimaryKey(tempProductAssoc.getPrimaryKey());
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
            Map messageMap = UtilMisc.toMap("updateMode", updateMode);
            errMsg = UtilProperties.getMessage(resource,"productevents.specified_update_mode_not_supported", messageMap, UtilHttp.getLocale(request));
            request.setAttribute("_ERROR_MESSAGE_", errMsg);
            return "error";
        }

        return "success";
    }

    public static String updateAttribute(HttpServletRequest request, HttpServletResponse response) {
        String errMsg = "";
        List errMsgList = new LinkedList();
        GenericDelegator delegator = (GenericDelegator) request.getAttribute("delegator");
        Security security = (Security) request.getAttribute("security");

        String updateMode = request.getParameter("UPDATE_MODE");

        if (updateMode == null || updateMode.length() <= 0) {
            errMsg = UtilProperties.getMessage(resource,"productevents.updatemode_not_specified", UtilHttp.getLocale(request));
            request.setAttribute("_ERROR_MESSAGE_", errMsg);
            Debug.logWarning("[CategoryEvents.updateCategory] Update Mode was not specified, but is required", module);
            return "error";
        }

        // check permissions before moving on...
        if (!security.hasEntityPermission("CATALOG", "_" + updateMode, request.getSession())) {
            Map messageMap = UtilMisc.toMap("updateMode", updateMode);
            errMsg = UtilProperties.getMessage(resource,"productevents.not_sufficient_permissions", messageMap, UtilHttp.getLocale(request));
            request.setAttribute("_ERROR_MESSAGE_", errMsg);
            return "error";
        }

        String productId = request.getParameter("PRODUCT_ID");
        String attrName = request.getParameter("ATTRIBUTE_NAME");
        String attrValue = request.getParameter("ATTRIBUTE_VALUE");
        String attrType = request.getParameter("ATTRIBUTE_TYPE");

        if (!UtilValidate.isNotEmpty(productId))
            errMsgList.add(UtilProperties.getMessage(resource,"productevents.product_ID_missing", UtilHttp.getLocale(request)));
        if (!UtilValidate.isNotEmpty(attrName))
            errMsgList.add(UtilProperties.getMessage(resource,"productevents.attribute_name_missing", UtilHttp.getLocale(request)));
        if (errMsgList.size() > 0) {
            request.setAttribute("_ERROR_MESSAGE_LIST_", errMsgList);
            return "error";
        }

        List toBeStored = new LinkedList();
        GenericValue attribute = delegator.makeValue("ProductAttribute", null);

        toBeStored.add(attribute);
        attribute.set("productId", productId);
        attribute.set("attrName", attrName);
        attribute.set("attrValue", attrValue);
        attribute.set("attrType", attrType);

        if (updateMode.equals("CREATE")) {
            try {
                delegator.storeAll(toBeStored);
            } catch (GenericEntityException e) {
                errMsg = UtilProperties.getMessage(resource,"productevents.could_not_create_attribute_write", UtilHttp.getLocale(request));
                request.setAttribute("_ERROR_MESSAGE_", errMsg);
                return "error";
            }
        } else if (updateMode.equals("UPDATE")) {
            try {
                delegator.storeAll(toBeStored);
            } catch (GenericEntityException e) {
                errMsg = UtilProperties.getMessage(resource,"productevents.could_not_update_attribute_write", UtilHttp.getLocale(request));
                request.setAttribute("_ERROR_MESSAGE_", errMsg);
                Debug.logWarning("[ProductEvents.updateAttribute] Could not update attribute (write error); message: " + e.getMessage(), module);
                return "error";
            }
        } else if (updateMode.equals("DELETE")) {
            try {
                delegator.removeByAnd("ProductAttribute", UtilMisc.toMap("productId", productId, "attrName", attrName));
            } catch (GenericEntityException e) {
                errMsg = UtilProperties.getMessage(resource,"productevents.could_not_delete_attribute_write", UtilHttp.getLocale(request));
                request.setAttribute("_ERROR_MESSAGE_", errMsg);
                Debug.logWarning("[ProductEvents.updateAttribute] Could not delete attribute (write error); message: " + e.getMessage(), module);
                return "error";
            }
        } else {
            Map messageMap = UtilMisc.toMap("updateMode", updateMode);
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
        GenericDelegator delegator = (GenericDelegator) request.getAttribute("delegator");
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
                    GenericValue product = delegator.findByPrimaryKey("Product", UtilMisc.toMap("productId", productId));
                    product.set("lastModifiedDate", nowTimestamp);
                    product.setString("lastModifiedByUserLogin", userLogin.getString("userLoginId"));
                    try {
                        product.set("productHeight", UtilParse.parseDoubleForEntity(request.getParameter("productHeight")));
                        product.set("productWidth", UtilParse.parseDoubleForEntity(request.getParameter("productWidth")));
                        product.set("productDepth", UtilParse.parseDoubleForEntity(request.getParameter("productDepth")));
                        product.set("weight", UtilParse.parseDoubleForEntity(request.getParameter("weight")));
    
                        // default unit settings for shipping parameters
                        product.set("heightUomId", "LEN_in");
                        product.set("widthUomId", "LEN_in");
                        product.set("depthUomId", "LEN_in");
                        product.set("weightUomId", "WT_oz");
    
                        Double floz = UtilParse.parseDoubleForEntity(request.getParameter("~floz"));
                        Double ml = UtilParse.parseDoubleForEntity(request.getParameter("~ml"));
                        Double ntwt = UtilParse.parseDoubleForEntity(request.getParameter("~ntwt"));
                        Double grams = UtilParse.parseDoubleForEntity(request.getParameter("~grams"));
    
                        List currentProductFeatureAndAppls = EntityUtil.filterByDate(delegator.findByAnd("ProductFeatureAndAppl", UtilMisc.toMap("productId", productId, "productFeatureApplTypeId", "STANDARD_FEATURE")), true);
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
                        GenericValue product = delegator.findByPrimaryKey("Product", UtilMisc.toMap("productId", productId));
                        try {
                            product.set("productHeight", UtilParse.parseDoubleForEntity(request.getParameter("productHeight" + attribIdx)));
                            product.set("productWidth", UtilParse.parseDoubleForEntity(request.getParameter("productWidth" + attribIdx)));
                            product.set("productDepth", UtilParse.parseDoubleForEntity(request.getParameter("productDepth" + attribIdx)));
                            product.set("weight", UtilParse.parseDoubleForEntity(request.getParameter("weight" + attribIdx)));
                            Double floz = UtilParse.parseDoubleForEntity(request.getParameter("~floz" + attribIdx));
                            Double ml = UtilParse.parseDoubleForEntity(request.getParameter("~ml" + attribIdx));
                            Double ntwt = UtilParse.parseDoubleForEntity(request.getParameter("~ntwt" + attribIdx));
                            Double grams = UtilParse.parseDoubleForEntity(request.getParameter("~grams" + attribIdx));
    
                                List currentProductFeatureAndAppls = EntityUtil.filterByDate(delegator.findByAnd("ProductFeatureAndAppl", UtilMisc.toMap("productId", productId, "productFeatureApplTypeId", "STANDARD_FEATURE")), true);
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
    private static void setOrCreateProdFeature(GenericDelegator delegator, String productId, List currentProductFeatureAndAppls,
                                          String uomId, String productFeatureTypeId, Double numberSpecified) throws GenericEntityException {
        
        GenericValue productFeatureType = delegator.findByPrimaryKey("ProductFeatureType", UtilMisc.toMap("productFeatureTypeId", productFeatureTypeId));
        GenericValue uom = delegator.findByPrimaryKey("Uom", UtilMisc.toMap("uomId", uomId));
        
        Timestamp nowTimestamp = UtilDateTime.nowTimestamp();

        // filter list of features to the one we'll be editing
        List typeUomProductFeatureAndApplList = EntityUtil.filterByAnd(currentProductFeatureAndAppls, UtilMisc.toMap("productFeatureTypeId", productFeatureTypeId, "uomId", uomId));

        // go through each; need to remove? do it now
        Iterator typeUomProductFeatureAndApplIter = typeUomProductFeatureAndApplList.iterator();
        boolean foundOneEqual = false;
        while (typeUomProductFeatureAndApplIter.hasNext()) {
            GenericValue typeUomProductFeatureAndAppl = (GenericValue) typeUomProductFeatureAndApplIter.next();
            if ((numberSpecified != null) && (numberSpecified.equals(typeUomProductFeatureAndAppl.getDouble("numberSpecified")))) {
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
            List existingProductFeatureList = delegator.findByAnd("ProductFeature", UtilMisc.toMap("productFeatureTypeId", productFeatureTypeId, "numberSpecified", numberSpecified, "uomId", uomId));
            if (existingProductFeatureList.size() > 0) {
                GenericValue existingProductFeature = (GenericValue) existingProductFeatureList.get(0);
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
                if (delegator.findByPrimaryKey("ProductFeatureCategory", UtilMisc.toMap("productFeatureCategoryId", productFeatureTypeId)) == null) {
                    GenericValue productFeatureCategory = delegator.makeValue("ProductFeatureCategory", null);
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
        GenericDelegator delegator = (GenericDelegator) request.getAttribute("delegator");
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
                GenericValue productFeatureType = delegator.findByPrimaryKey("ProductFeatureType", UtilMisc.toMap("productFeatureTypeId", productFeatureTypeId));
                if (productFeatureType == null) {
                    String errMsg = "Error: the ProductFeature Type specified was not valid and one is require to add or update variant features.";
                    request.setAttribute("_ERROR_MESSAGE_", errMsg);
                    return "error";
                }
                
                // check for variantProductId - this will mean that we have multiple variants to update
                if (variantProductId != null) {
                    // multiple products, so use a numeric suffix to get them all
                    int attribIdx = 0;
                    GenericValue product = delegator.findByPrimaryKey("Product", UtilMisc.toMap("productId", productId));
                    do {
                        GenericValue variantProduct = delegator.findByPrimaryKey("Product", UtilMisc.toMap("productId", variantProductId));
                        String description = request.getParameter("description" + attribIdx);
                        // blank means null, which means delete the feature application
                        if ((description != null) && (description.trim().length() < 1)) {
                            description = null;
                        }
    
                        Set variantDescRemoveToRemoveOnVirtual = new HashSet();
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
            Timestamp nowTimestamp, GenericDelegator delegator, Set descriptionsToRemove, Set descriptionsRemoved) throws GenericEntityException {
        if (productFeatureType == null) {
            return;
        }

        GenericValue productFeatureAndAppl = null;

        Set descriptionsForThisType = new HashSet();
        List productFeatureAndApplList = EntityUtil.filterByDate(delegator.findByAnd("ProductFeatureAndAppl", UtilMisc.toMap("productId", productId, 
                "productFeatureApplTypeId", productFeatureApplTypeId, "productFeatureTypeId", productFeatureTypeId)), true);
        if (productFeatureAndApplList.size() > 0) {
            Iterator productFeatureAndApplIter = productFeatureAndApplList.iterator();
            while (productFeatureAndApplIter.hasNext()) {
                productFeatureAndAppl = (GenericValue) productFeatureAndApplIter.next();
                GenericValue productFeatureAppl = delegator.makeValidValue("ProductFeatureAppl", productFeatureAndAppl);

                // remove productFeatureAppl IFF: productFeatureAppl != null && (description is empty/null || description is different than existing)
                if (productFeatureAppl != null && (description == null || !description.equals(productFeatureAndAppl.getString("description")))) {
                    // if descriptionsToRemove is not null, only remove if description is in that set
                    if (descriptionsToRemove == null || (descriptionsToRemove != null && descriptionsToRemove.contains(productFeatureAndAppl.get("description")))) {
                        // okay, almost there: before removing it if this is a virtual product check to make SURE this feature's description doesn't exist on any of the variants; wouldn't want to remove something we should have kept around...
                        if ("Y".equals(product.getString("isVirtual"))) {
                            boolean foundFeatureOnVariant = false;
                            // get/check all the variants
                            List variantAssocs = product.getRelatedByAnd("MainProductAssoc", UtilMisc.toMap("productAssocTypeId", "PRODUCT_VARIANT"));
                            List variants = EntityUtil.getRelated("AssocProduct", variantAssocs);
                            Iterator variantIter = variants.iterator();
                            while (!foundFeatureOnVariant && variantIter.hasNext()) {
                                GenericValue variant = (GenericValue) variantIter.next();
                                // get the selectable features for the variant
                                List variantProductFeatureAndAppls = variant.getRelated("ProductFeatureAndAppl", 
                                        UtilMisc.toMap("productFeatureTypeId", productFeatureTypeId, 
                                                "productFeatureApplTypeId", "STANDARD_FEATURE", "description", description), null);
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
                            descriptionsRemoved.add(productFeatureAndAppl.get("description"));
                        }
                        productFeatureAppl.remove();
                        continue;
                    }
                }
                
                // we got here, is still a valid description associated with this product
                descriptionsForThisType.add(productFeatureAndAppl.get("description"));
            }
        }
        
        if (description != null && (productFeatureAndAppl == null || (productFeatureAndAppl != null && !descriptionsForThisType.contains(description)))) {
            // need to add an appl, and possibly the feature
            
            // see if a feature exists with the type and description specified (if doesn't exist will create later)
            String productFeatureId = null;
            List existingProductFeatureList = delegator.findByAnd("ProductFeature", UtilMisc.toMap("productFeatureTypeId", productFeatureTypeId, "description", description));
            if (existingProductFeatureList.size() > 0) {
                GenericValue existingProductFeature = (GenericValue) existingProductFeatureList.get(0);
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
                if (delegator.findByPrimaryKey("ProductFeatureCategory", UtilMisc.toMap("productFeatureCategoryId", productFeatureTypeId)) == null) {
                    GenericValue productFeatureCategory = delegator.makeValue("ProductFeatureCategory", null);
                    productFeatureCategory.set("productFeatureCategoryId", productFeatureTypeId);
                    productFeatureCategory.set("description", productFeatureType.get("description"));
                    productFeatureCategory.create();
                }
                newProductFeature.set("productFeatureCategoryId", productFeatureTypeId);
                newProductFeature.create();
            }
    
            // check to see if the productFeatureId is already attached to the virtual or variant, if not attach them...
            List specificProductFeatureApplList = EntityUtil.filterByDate(delegator.findByAnd("ProductFeatureAppl", UtilMisc.toMap("productId", productId, 
                    "productFeatureApplTypeId", productFeatureApplTypeId, "productFeatureId", productFeatureId)), true);
            
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
        GenericDelegator delegator = (GenericDelegator) request.getAttribute("delegator");
        //Timestamp nowTimestamp = UtilDateTime.nowTimestamp();
        //GenericValue userLogin = (GenericValue) request.getSession().getAttribute("userLogin");
        String productId = request.getParameter("productId");
        String productFeatureTypeId = request.getParameter("productFeatureTypeId");

        try {
            GenericValue product = delegator.findByPrimaryKey("Product", UtilMisc.toMap("productId", productId));
            // get all the variants
            List variantAssocs = product.getRelatedByAnd("MainProductAssoc", UtilMisc.toMap("productAssocTypeId", "PRODUCT_VARIANT"));
            List variants = EntityUtil.getRelated("AssocProduct", variantAssocs);
            Iterator variantIter = variants.iterator();
            while (variantIter.hasNext()) {
                GenericValue variant = (GenericValue) variantIter.next();
                // get the selectable features for the variant
                List productFeatureAndAppls = variant.getRelated("ProductFeatureAndAppl", UtilMisc.toMap("productFeatureTypeId", productFeatureTypeId, "productFeatureApplTypeId", "STANDARD_FEATURE"), null);
                Iterator productFeatureAndApplIter = productFeatureAndAppls.iterator();
                while (productFeatureAndApplIter.hasNext()) {
                    GenericValue productFeatureAndAppl = (GenericValue) productFeatureAndApplIter.next();
                    GenericPK productFeatureApplPK = delegator.makePK("ProductFeatureAppl", null);
                    productFeatureApplPK.setPKFields(productFeatureAndAppl);
                    delegator.removeByPrimaryKey(productFeatureApplPK);
                }
            }
            List productFeatureAndAppls = product.getRelated("ProductFeatureAndAppl", UtilMisc.toMap("productFeatureTypeId", productFeatureTypeId, "productFeatureApplTypeId", "SELECTABLE_FEATURE"), null);
            Iterator productFeatureAndApplIter = productFeatureAndAppls.iterator();
            while (productFeatureAndApplIter.hasNext()) {
                GenericValue productFeatureAndAppl = (GenericValue) productFeatureAndApplIter.next();
                GenericPK productFeatureApplPK = delegator.makePK("ProductFeatureAppl", null);
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
        GenericDelegator delegator = (GenericDelegator) request.getAttribute("delegator");
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
        GenericDelegator delegator = (GenericDelegator) request.getAttribute("delegator");
        String productId = request.getParameter("productId");
        String fromDate = request.getParameter("fromDate");
        if ((fromDate == null) || (fromDate.trim().length() == 0)) {
            fromDate = UtilDateTime.nowTimestamp().toString();
        }
        String[] categoryId = request.getParameterValues("categoryId");
        if (categoryId != null) {
            for (int i = 0; i < categoryId.length; i++) {
                try {
                    List catMembs = delegator.findByAnd("ProductCategoryMember", UtilMisc.toMap(
                            "productCategoryId", categoryId[i],
                            "productId", productId));
                    catMembs = EntityUtil.filterByDate(catMembs);
                    if (catMembs.size() == 0) {
                        delegator.create("ProductCategoryMember",
                                UtilMisc.toMap("productCategoryId", categoryId[i], "productId", productId, "fromDate", fromDate));
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
        GenericDelegator delegator = (GenericDelegator) request.getAttribute("delegator");
        String productId = request.getParameter("productId");
        String productCategoryId = request.getParameter("productCategoryId");
        String thruDate = request.getParameter("thruDate");
        if ((thruDate == null) || (thruDate.trim().length() == 0)) {
            thruDate = UtilDateTime.nowTimestamp().toString();
        }
        try {
            List prodCatMembs = delegator.findByAnd("ProductCategoryMember",
                    UtilMisc.toMap("productCategoryId", productCategoryId, "productId", productId));
            prodCatMembs = EntityUtil.filterByDate(prodCatMembs);
            if (prodCatMembs.size() > 0) {
                // there is one to modify
                GenericValue prodCatMemb = (GenericValue)prodCatMembs.get(0);
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
        GenericDelegator delegator = (GenericDelegator) request.getAttribute("delegator");
        String productId = request.getParameter("productId");
        String productFeatureApplTypeId = request.getParameter("productFeatureApplTypeId");
        String fromDate = request.getParameter("fromDate");
        if ((fromDate == null) || (fromDate.trim().length() == 0)) {
            fromDate = UtilDateTime.nowTimestamp().toString();
        }
        String[] productFeatureIdArray = request.getParameterValues("productFeatureId");
        if (productFeatureIdArray != null && productFeatureIdArray.length > 0) {
            try {
                for (int i = 0; i < productFeatureIdArray.length; i++) {
                    if (!productFeatureIdArray[i].equals("~~any~~")) {
                        List featureAppls = delegator.findByAnd("ProductFeatureAppl",
                                UtilMisc.toMap("productId", productId,
                                        "productFeatureId", productFeatureIdArray[i],
                                        "productFeatureApplTypeId", productFeatureApplTypeId));
                        if (featureAppls.size() == 0) {
                            // no existing application for this
                            delegator.create("ProductFeatureAppl",
                                    UtilMisc.toMap("productId", productId,
                                        "productFeatureId", productFeatureIdArray[i],
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

            HttpSession session = request.getSession();
            GenericValue userLogin = (GenericValue) session.getAttribute("userLogin");
            if (userLogin != null) {
                // user login currency
                currencyStr = userLogin.getString("lastCurrencyUom");
                // user login locale
                localeStr = userLogin.getString("lastLocale");
            }

            // if currency is not set, the store's default currency is used
            if (currencyStr == null && productStore.get("defaultCurrencyUomId") != null) {
                currencyStr = productStore.getString("defaultCurrencyUomId");
            }

            // if locale is not set, the store's default locale is used
            if (localeStr == null && productStore.get("defaultLocaleString") != null) {
                localeStr = productStore.getString("defaultLocaleString");
            }
            
            UtilHttp.setCurrencyUom(session, currencyStr);
            UtilHttp.setLocale(request, localeStr);

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
        GenericDelegator delegator = (GenericDelegator) request.getAttribute("delegator");
        GenericValue userLogin = (GenericValue) session.getAttribute("userLogin");
        GenericValue productStore = ProductStoreWorker.getProductStore(request);
        if (productStore != null && userLogin != null) {
            if ("Y".equals(productStore.getString("requireCustomerRole"))) {
                List productStoreRoleList = null;
                try {
                    productStoreRoleList = delegator.findByAnd("ProductStoreRole", UtilMisc.toMap("productStoreId", productStore.get("productStoreId"), 
                            "partyId", userLogin.get("partyId"), "roleTypeId", "CUSTOMER"));
                    productStoreRoleList = EntityUtil.filterByDate(productStoreRoleList, true);
                } catch (GenericEntityException e) {
                    Debug.logError(e, "Database error finding CUSTOMER ProductStoreRole records, required by the ProductStore with ID [" + productStore.getString("productStoreId") + "]", module);
                }
                if (productStoreRoleList == null || productStoreRoleList.size() == 0) {
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
        GenericDelegator delegator = (GenericDelegator) request.getAttribute("delegator");
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
            productStoreEmail = delegator.findByPrimaryKey("ProductStoreEmailSetting",
                    UtilMisc.toMap("productStoreId", productStoreId, "emailType", emailType));
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
        
        Map paramMap = UtilHttp.getParameterMap(request);
        paramMap.put("locale", UtilHttp.getLocale(request));
        paramMap.put("userLogin", session.getAttribute("userLogin"));

        Map context = FastMap.newInstance();
        context.put("bodyScreenUri", bodyScreenLocation);
        context.put("bodyParameters", paramMap);
        context.put("sendTo", paramMap.get("sendTo"));
        context.put("contentType", productStoreEmail.get("contentType"));
        context.put("sendFrom", productStoreEmail.get("fromAddress"));
        context.put("sendCc", productStoreEmail.get("ccAddress"));
        context.put("sendBcc", productStoreEmail.get("bccAddress"));
        context.put("subject", productStoreEmail.getString("subject"));

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
}
