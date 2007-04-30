/*
 Licensed to the Apache Software Foundation (ASF) under one
 or more contributor license agreements.  See the NOTICE file
 distributed with this work for additional information
 regarding copyright ownership.  The ASF licenses this file
 to you under the Apache License, Version 2.0 (the
 "License"); you may not use this file except in compliance
 with the License.  You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing,
 software distributed under the License is distributed on an
 "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 KIND, either express or implied.  See the License for the
 specific language governing permissions and limitations
 under the License.
 */

package org.ofbiz.order.order;

import javolution.util.FastList;
import org.ofbiz.base.util.*;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.*;
import org.ofbiz.entity.model.DynamicViewEntity;
import org.ofbiz.entity.model.ModelKeyMap;
import org.ofbiz.entity.util.EntityFindOptions;
import org.ofbiz.entity.util.EntityListIterator;
import org.ofbiz.security.Security;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;

import java.math.BigDecimal;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * OrderLookupServices
 */
public class OrderLookupServices {

    public static final String module = OrderLookupServices.class.getName();

    public static Map findOrders(DispatchContext dctx, Map context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericDelegator delegator = dctx.getDelegator();
        Security security = dctx.getSecurity();

        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Integer viewIndex = (Integer) context.get("viewIndex");
        Integer viewSize = (Integer) context.get("viewSize");
        String showAll = (String) context.get("showAll");
        String useEntryDate = (String) context.get("useEntryDate");
        if (showAll == null) {
            showAll = "N";
        }

        // list of fields to select (initial list)
        List fieldsToSelect = FastList.newInstance();
        fieldsToSelect.add("orderId");
        fieldsToSelect.add("statusId");
        fieldsToSelect.add("orderTypeId");
        fieldsToSelect.add("orderDate");
        fieldsToSelect.add("currencyUom");
        fieldsToSelect.add("grandTotal");
        fieldsToSelect.add("remainingSubTotal");

        // sorting by order date newest first
        List orderBy = UtilMisc.toList("-orderDate", "-orderId");

        // list to hold the parameters
        List paramList = FastList.newInstance();

        // list of conditions
        List conditions = FastList.newInstance();

        // check security flag for purchase orders
        boolean canViewPo = security.hasEntityPermission("ORDERMGR", "_PURCHASE_VIEW", userLogin);
        if (!canViewPo) {
            conditions.add(new EntityExpr("orderTypeId", EntityOperator.NOT_EQUAL, "PURCHASE_ORDER"));
        }

        // dynamic view entity
        DynamicViewEntity dve = new DynamicViewEntity();
        dve.addMemberEntity("OH", "OrderHeader");
        dve.addAliasAll("OH", ""); // no prefix
        dve.addRelation("one-nofk", "", "OrderType", UtilMisc.toList(new ModelKeyMap("orderTypeId", "orderTypeId")));
        dve.addRelation("one-nofk", "", "StatusItem", UtilMisc.toList(new ModelKeyMap("statusId", "statusId")));

        // start the lookup
        String orderId = (String) context.get("orderId");
        if (UtilValidate.isNotEmpty(orderId)) {
            paramList.add("orderId=" + orderId);
            conditions.add(makeExpr("orderId", orderId));
        }

        // the base order header fields
        List orderTypeList = (List) context.get("orderTypeId");
        if (orderTypeList != null) {
            Iterator i = orderTypeList.iterator();
            List orExprs = FastList.newInstance();
            while (i.hasNext()) {
                String orderTypeId = (String) i.next();
                paramList.add("orderTypeId=" + orderTypeId);

                if (!"PURCHASE_ORDER".equals(orderTypeId) || ("PURCHASE_ORDER".equals(orderTypeId) && canViewPo)) {
                    orExprs.add(new EntityExpr("orderTypeId", EntityOperator.EQUALS, orderTypeId));
                }
            }
            conditions.add(new EntityConditionList(orExprs, EntityOperator.OR));
        }

        String orderName = (String) context.get("orderName");
        if (UtilValidate.isNotEmpty(orderName)) {
            paramList.add("orderName=" + orderName);
            conditions.add(makeExpr("orderName", orderName, true));
        }

        List orderStatusList = (List) context.get("orderStatusId");
        if (orderStatusList != null) {
            Iterator i = orderStatusList.iterator();
            List orExprs = FastList.newInstance();
            while (i.hasNext()) {
                String orderStatusId = (String) i.next();
                paramList.add("orderStatusId=" + orderStatusId);
                if ("PENDING".equals(orderStatusId)) {
                    List pendExprs = FastList.newInstance();
                    pendExprs.add(new EntityExpr("statusId", EntityOperator.EQUALS, "ORDER_CREATED"));
                    pendExprs.add(new EntityExpr("statusId", EntityOperator.EQUALS, "ORDER_PROCESSING"));
                    pendExprs.add(new EntityExpr("statusId", EntityOperator.EQUALS, "ORDER_APPROVED"));
                    orExprs.add(new EntityConditionList(pendExprs, EntityOperator.OR));
                } else {
                    orExprs.add(new EntityExpr("statusId", EntityOperator.EQUALS, orderStatusId));
                }
            }
            conditions.add(new EntityConditionList(orExprs, EntityOperator.OR));
        }

        List productStoreList = (List) context.get("productStoreId");
        if (productStoreList != null) {
            Iterator i = productStoreList.iterator();
            List orExprs = FastList.newInstance();
            while (i.hasNext()) {
                String productStoreId = (String) i.next();
                paramList.add("productStoreId=" + productStoreId);
                orExprs.add(new EntityExpr("productStoreId", EntityOperator.EQUALS, productStoreId));
            }
            conditions.add(new EntityConditionList(orExprs, EntityOperator.OR));
        }

        List webSiteList = (List) context.get("orderWebSiteId");
        if (webSiteList != null) {
            Iterator i = webSiteList.iterator();
            List orExprs = FastList.newInstance();
            while (i.hasNext()) {
                String webSiteId = (String) i.next();
                paramList.add("webSiteId=" + webSiteId);
                orExprs.add(new EntityExpr("webSiteId", EntityOperator.EQUALS, webSiteId));
            }
            conditions.add(new EntityConditionList(orExprs, EntityOperator.OR));
        }

        List saleChannelList = (List) context.get("salesChannelEnumId");
        if (saleChannelList != null) {
            Iterator i = saleChannelList.iterator();
            List orExprs = FastList.newInstance();
            while (i.hasNext()) {
                String salesChannelEnumId = (String) i.next();
                paramList.add("salesChannelEnumId=" + salesChannelEnumId);
                orExprs.add(new EntityExpr("salesChannelEnumId", EntityOperator.EQUALS, salesChannelEnumId));
            }
            conditions.add(new EntityConditionList(orExprs, EntityOperator.OR));
        }

        String createdBy = (String) context.get("createdBy");
        if (UtilValidate.isNotEmpty(createdBy)) {
            paramList.add("createdBy=" + createdBy);
            conditions.add(makeExpr("createdBy", createdBy));
        }

        String terminalId = (String) context.get("terminalId");
        if (UtilValidate.isNotEmpty(terminalId)) {
            paramList.add("terminalId=" + terminalId);
            conditions.add(makeExpr("terminalId", terminalId));
        }

        String transactionId = (String) context.get("transactionId");
        if (UtilValidate.isNotEmpty(transactionId)) {
            paramList.add("transactionId=" + transactionId);
            conditions.add(makeExpr("transactionId", transactionId));
        }

        String externalId = (String) context.get("externalId");
        if (UtilValidate.isNotEmpty(externalId)) {
            paramList.add("externalId=" + externalId);
            conditions.add(makeExpr("externalId", externalId));
        }

        String internalCode = (String) context.get("internalCode");
        if (UtilValidate.isNotEmpty(internalCode)) {
            paramList.add("internalCode=" + internalCode);
            conditions.add(makeExpr("internalCode", internalCode));
        }

        String dateField = "Y".equals(useEntryDate) ? "entryDate" : "orderDate";
        String minDate = (String) context.get("minDate");
        if (UtilValidate.isNotEmpty(minDate) && minDate.length() > 8) {
            minDate = minDate.trim();
            if (minDate.length() < 14) minDate = minDate + " " + "00:00:00.000";
            paramList.add("minDate=" + minDate);

            try {
                Object converted = ObjectType.simpleTypeConvert(minDate, "Timestamp", null, null);
                if (converted != null) {
                    conditions.add(new EntityExpr(dateField, EntityOperator.GREATER_THAN_EQUAL_TO, converted));
                }
            } catch (GeneralException e) {
                Debug.logWarning(e.getMessage(), module);
            }
        }

        String maxDate = (String) context.get("maxDate");
        if (UtilValidate.isNotEmpty(maxDate) && maxDate.length() > 8) {
            maxDate = maxDate.trim();
            if (maxDate.length() < 14) maxDate = maxDate + " " + "23:59:59.999";
            paramList.add("maxDate=" + maxDate);

            try {
                Object converted = ObjectType.simpleTypeConvert(maxDate, "Timestamp", null, null);
                if (converted != null) {
                    conditions.add(new EntityExpr("orderDate", EntityOperator.LESS_THAN_EQUAL_TO, converted));
                }
            } catch (GeneralException e) {
                Debug.logWarning(e.getMessage(), module);
            }
        }

        // party (role) fields
        String userLoginId = (String) context.get("userLoginId");
        String partyId = (String) context.get("partyId");
        List roleTypeList = (List) context.get("roleTypeId");

        if (UtilValidate.isNotEmpty(userLoginId) && UtilValidate.isEmpty(partyId)) {
            GenericValue ul = null;
            try {
                ul = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", userLoginId));
            } catch (GenericEntityException e) {
                Debug.logWarning(e.getMessage(), module);
            }
            if (ul != null) {
                partyId = ul.getString("partyId");
            }
        }

        // add the role data to the view
        if (roleTypeList != null || partyId != null) {
            dve.addMemberEntity("OT", "OrderRole");
            dve.addAlias("OT", "partyId");
            dve.addAlias("OT", "roleTypeId");
            dve.addViewLink("OH", "OT", Boolean.FALSE, UtilMisc.toList(new ModelKeyMap("orderId", "orderId")));
        }

        if (UtilValidate.isNotEmpty(partyId)) {
            paramList.add("partyId=" + partyId);
            fieldsToSelect.add("partyId");
            conditions.add(makeExpr("partyId", partyId));
        }

        if (roleTypeList != null) {
            fieldsToSelect.add("roleTypeId");
            Iterator i = roleTypeList.iterator();
            List orExprs = FastList.newInstance();
            while (i.hasNext()) {
                String roleTypeId = (String) i.next();
                paramList.add("roleTypeId=" + roleTypeId);
                orExprs.add(makeExpr("roleTypeId", roleTypeId));
            }
            conditions.add(new EntityConditionList(orExprs, EntityOperator.OR));
        }

        // order item fields
        String correspondingPoId = (String) context.get("correspondingPoId");
        String subscriptionId = (String) context.get("subscriptionId");
        String productId = (String) context.get("productId");
        String budgetId = (String) context.get("budgetId");
        String quoteId = (String) context.get("quoteId");

        if (correspondingPoId != null || subscriptionId != null || productId != null || budgetId != null || quoteId != null) {
            dve.addMemberEntity("OI", "OrderItem");
            dve.addAlias("OI", "correspondingPoId");
            dve.addAlias("OI", "subscriptionId");
            dve.addAlias("OI", "productId");
            dve.addAlias("OI", "budgetId");
            dve.addAlias("OI", "quoteId");
            dve.addViewLink("OH", "OI", Boolean.FALSE, UtilMisc.toList(new ModelKeyMap("orderId", "orderId")));
        }

        if (UtilValidate.isNotEmpty(correspondingPoId)) {
            paramList.add("correspondingPoId=" + correspondingPoId);
            conditions.add(makeExpr("correspondingPoId", correspondingPoId));
        }

        if (UtilValidate.isNotEmpty(subscriptionId)) {
            paramList.add("subscriptionId=" + subscriptionId);
            conditions.add(makeExpr("subscriptionId", subscriptionId));
        }

        if (UtilValidate.isNotEmpty(productId)) {
            paramList.add("productId=" + productId);
            if (productId.startsWith("%") || productId.startsWith("*") || productId.endsWith("%") || productId.endsWith("*")) {
                conditions.add(makeExpr("productId", productId));
            } else {
                GenericValue product = null;
                try {
                    product = delegator.findByPrimaryKey("Product", UtilMisc.toMap("productId", productId));
                } catch (GenericEntityException e) {
                    Debug.logWarning(e.getMessage(), module);
                }
                if (product != null) {
                    String isVirtual = product.getString("isVirtual");
                    if (isVirtual != null && "Y".equals(isVirtual)) {
                        List orExprs = FastList.newInstance();
                        orExprs.add(new EntityExpr("productId", EntityOperator.EQUALS, productId));

                        Map varLookup = null;
                        try {
                            varLookup = dispatcher.runSync("getAllProductVariants", UtilMisc.toMap("productId", productId));
                        } catch (GenericServiceException e) {
                            Debug.logWarning(e.getMessage(), module);
                        }
                        List variants = (List) varLookup.get("assocProducts");
                        if (variants != null) {
                            Iterator i = variants.iterator();
                            while (i.hasNext()) {
                                GenericValue v = (GenericValue) i.next();
                                orExprs.add(new EntityExpr("productId", EntityOperator.EQUALS, v.getString("productIdTo")));
                            }
                        }
                        conditions.add(new EntityConditionList(orExprs, EntityOperator.OR));
                    } else {
                        conditions.add(new EntityExpr("productId", EntityOperator.EQUALS, productId));
                    }
                }
            }
        }

        if (UtilValidate.isNotEmpty(budgetId)) {
            paramList.add("budgetId=" + budgetId);
            conditions.add(makeExpr("budgetId", budgetId));
        }

        if (UtilValidate.isNotEmpty(quoteId)) {
            paramList.add("quoteId=" + quoteId);
            conditions.add(makeExpr("quoteId", quoteId));
        }

        // payment preference fields
        String billingAccountId = (String) context.get("billingAccountId");
        String finAccountId = (String) context.get("finAccountId");
        String cardNumber = (String) context.get("cardNumber");
        String accountNumber = (String) context.get("accountNumber");

        if (billingAccountId != null || finAccountId != null || cardNumber != null || accountNumber != null) {
            dve.addMemberEntity("OP", "OrderPaymentPreference");
            dve.addAlias("OP", "billingAccountId");
            dve.addAlias("OP", "finAccountId");
            dve.addAlias("OP", "paymentMethodId");
            dve.addViewLink("OH", "OP", Boolean.FALSE, UtilMisc.toList(new ModelKeyMap("orderId", "orderId")));
        }

        // search by billing account ID
        if (UtilValidate.isNotEmpty(billingAccountId)) {
            paramList.add("billingAccountId=" + billingAccountId);
            conditions.add(makeExpr("billingAccountId", billingAccountId));
        }

        // search by fin account ID
        if (UtilValidate.isNotEmpty(finAccountId)) {
            paramList.add("finAccountId=" + finAccountId);
            conditions.add(makeExpr("finAccountId", finAccountId));
        }

        // search by card number
        if (UtilValidate.isNotEmpty(cardNumber)) {
            dve.addMemberEntity("CC", "CreditCard");
            dve.addAlias("CC", "cardNumber");
            dve.addViewLink("OP", "CC", Boolean.FALSE, UtilMisc.toList(new ModelKeyMap("paymentMethodId", "paymentMethodId")));

            paramList.add("cardNumber=" + cardNumber);
            conditions.add(makeExpr("cardNumber", cardNumber));
        }

        // search by eft account number
        if (UtilValidate.isNotEmpty(accountNumber)) {
            dve.addMemberEntity("EF", "EftAccount");
            dve.addAlias("EF", "accountNumber");
            dve.addViewLink("OP", "EF", Boolean.FALSE, UtilMisc.toList(new ModelKeyMap("paymentMethodId", "paymentMethodId")));

            paramList.add("accountNumber=" + accountNumber);
            conditions.add(makeExpr("accountNumber", accountNumber));
        }

        // shipment/inventory item
        String inventoryItemId = (String) context.get("inventoryItemId");
        String softIdentifier = (String) context.get("softIdentifier");
        String serialNumber = (String) context.get("serialNumber");
        String shipmentId = (String) context.get("shipmentId");

        if (shipmentId != null || inventoryItemId != null || softIdentifier != null || serialNumber != null) {
            dve.addMemberEntity("II", "ItemIssuance");
            dve.addAlias("II", "shipmentId");
            dve.addAlias("II", "inventoryItemId");
            dve.addViewLink("OH", "II", Boolean.FALSE, UtilMisc.toList(new ModelKeyMap("orderId", "orderId")));

            if (softIdentifier != null || serialNumber != null) {
                dve.addMemberEntity("IV", "InventoryItem");
                dve.addAlias("IV", "softIdentifier");
                dve.addAlias("IV", "serialNumber");
                dve.addViewLink("II", "IV", Boolean.FALSE, UtilMisc.toList(new ModelKeyMap("inventoryItemId", "inventoryItemId")));
            }
        }

        if (UtilValidate.isNotEmpty(inventoryItemId)) {
            paramList.add("inventoryItemId=" + inventoryItemId);
            conditions.add(makeExpr("inventoryItemId", inventoryItemId));
        }

        if (UtilValidate.isNotEmpty(softIdentifier)) {
            paramList.add("softIdentifier=" + softIdentifier);
            conditions.add(makeExpr("softIdentifier", softIdentifier, true));
        }

        if (UtilValidate.isNotEmpty(serialNumber)) {
            paramList.add("serialNumber=" + serialNumber);
            conditions.add(makeExpr("serialNumber", serialNumber, true));
        }

        if (UtilValidate.isNotEmpty(shipmentId)) {
            paramList.add("shipmentId=" + shipmentId);
            conditions.add(makeExpr("shipmentId", shipmentId));
        }

        // back order checking
        String hasBackOrders = (String) context.get("hasBackOrders");
        if (UtilValidate.isNotEmpty(hasBackOrders)) {
            dve.addMemberEntity("IR", "OrderItemShipGrpInvRes");
            dve.addAlias("IR", "quantityNotAvailable");
            dve.addViewLink("OH", "IR", Boolean.FALSE, UtilMisc.toList(new ModelKeyMap("orderId", "orderId")));

            paramList.add("hasBackOrders=" + hasBackOrders);
            if ("Y".equals(hasBackOrders)) {
                conditions.add(new EntityExpr("quantityNotAvailable", EntityOperator.NOT_EQUAL, null));
                conditions.add(new EntityExpr("quantityNotAvailable", EntityOperator.GREATER_THAN, new Double(0)));
            } else if ("N".equals(hasBackOrders)) {
                List orExpr = FastList.newInstance();
                orExpr.add(new EntityExpr("quantityNotAvailable", EntityOperator.EQUALS, null));
                orExpr.add(new EntityExpr("quantityNotAvailable", EntityOperator.EQUALS, new Double(0)));
                conditions.add(new EntityConditionList(orExpr, EntityOperator.OR));
            }
        }

        // set distinct on so we only get one row per order
        EntityFindOptions findOpts = new EntityFindOptions(true, EntityFindOptions.TYPE_SCROLL_INSENSITIVE, EntityFindOptions.CONCUR_READ_ONLY, true);

        // create the main condition
        EntityCondition cond = null;
        if (conditions.size() > 0 || showAll.equalsIgnoreCase("Y")) {
            cond = new EntityConditionList(conditions, EntityOperator.AND);
        }

        if (Debug.verboseOn()) {
            Debug.log("Find order query: " + cond.toString());
        }

        List orderList = FastList.newInstance();
        int orderCount = 0;

        // get the index for the partial list
        int lowIndex = (((viewIndex.intValue() - 1) * viewSize.intValue()) + 1);
        int highIndex = viewIndex.intValue() * viewSize.intValue();

        if (cond != null) {
            EntityListIterator eli = null;
            try {
                // do the lookup
                eli = delegator.findListIteratorByCondition(dve, cond, null, fieldsToSelect, orderBy, findOpts);

                // attempt to get the full size
                eli.last();
                orderCount = eli.currentIndex();

                // get the partial list for this page
                eli.beforeFirst();
                if (orderCount > viewSize.intValue()) {
                    orderList = eli.getPartialList(lowIndex, viewSize.intValue());
                } else if (orderCount > 0) {
                    orderList = eli.getCompleteList();
                }

                if (highIndex > orderCount) {
                    highIndex = orderCount;
                }
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
                return ServiceUtil.returnError(e.getMessage());
            } finally {
                if (eli != null) {
                    try {
                        eli.close();
                    } catch (GenericEntityException e) {
                        Debug.logWarning(e, e.getMessage(), module);
                    }
                }
            }
        }

        // create the result map
        Map result = ServiceUtil.returnSuccess();

        // filter out requested inventory problems
        filterInventoryProblems(context, result, orderList, paramList);

        // format the param list
        String paramString = StringUtil.join(paramList, "&amp;");

        result.put("highIndex", new Integer(highIndex));
        result.put("lowIndex", new Integer(lowIndex));
        result.put("viewIndex", viewIndex);
        result.put("viewSize", viewSize);
        result.put("showAll", showAll);

        result.put("paramList", (paramString != null? paramString: ""));
        result.put("orderList", orderList);
        result.put("orderListSize", new Integer(orderCount));

        return result;
    }

    public static void filterInventoryProblems(Map context, Map result, List orderList, List paramList) {
        List filterInventoryProblems = FastList.newInstance();

        String doFilter = (String) context.get("filterInventoryProblems");
        if (doFilter == null) {
            doFilter = "N";
        }

        if ("Y".equals(doFilter) && orderList.size() > 0) {
            paramList.add("filterInventoryProblems=Y");
            Iterator i = orderList.iterator();
            while (i.hasNext()) {
                GenericValue orderHeader = (GenericValue) i.next();
                OrderReadHelper orh = new OrderReadHelper(orderHeader);
                BigDecimal backorderQty = orh.getOrderBackorderQuantityBd();
                if (backorderQty.compareTo(new BigDecimal("0")) == 1) {
                    filterInventoryProblems.add(orh.getOrderId());
                }
            }
        }

        List filterPOsOpenPastTheirETA = FastList.newInstance();
        List filterPOsWithRejectedItems = FastList.newInstance();
        List filterPartiallyReceivedPOs = FastList.newInstance();

        String filterPOReject = (String) context.get("filterPOsWithRejectedItems");
        String filterPOPast = (String) context.get("filterPOsOpenPastTheirETA");
        String filterPartRec = (String) context.get("filterPartiallyReceivedPOs");
        if (filterPOReject == null) {
            filterPOReject = "N";
        }
        if (filterPOPast == null) {
            filterPOPast = "N";
        }
        if (filterPartRec == null) {
            filterPartRec = "N";
        }

        boolean doPoFilter = false;
        if ("Y".equals(filterPOReject)) {
            paramList.add("filterPOsWithRejectedItems=Y");
            doPoFilter = true;
        }
        if ("Y".equals(filterPOPast)) {
            paramList.add("filterPOsOpenPastTheirETA=Y");
            doPoFilter = true;
        }
        if ("Y".equals(filterPartRec)) {
            paramList.add("filterPartiallyReceivedPOs=Y");
            doPoFilter = true;
        }

        if (doPoFilter && orderList.size() > 0) {
            Iterator i = orderList.iterator();
            while (i.hasNext()) {
                GenericValue orderHeader = (GenericValue) i.next();
                OrderReadHelper orh = new OrderReadHelper(orderHeader);
                String orderType = orh.getOrderTypeId();
                String orderId = orh.getOrderId();

                if ("PURCHASE_ORDER".equals(orderType)) {
                    if ("Y".equals(filterPOReject) && orh.getRejectedOrderItems()) {
                        filterPOsWithRejectedItems.add(orderId);
                    }
                    else if ("Y".equals(filterPOPast) && orh.getPastEtaOrderItems(orderId)) {
                        filterPOsOpenPastTheirETA.add(orderId);
                    }
                    else if ("Y".equals(filterPartRec) && orh.getPartiallyReceivedItems()) {
                        filterPartiallyReceivedPOs.add(orderId);
                    }
                }
            }
        }

        result.put("filterInventoryProblemsList", filterInventoryProblems);
        result.put("filterPOsWithRejectedItemsList", filterPOsWithRejectedItems);
        result.put("filterPOsOpenPastTheirETAList", filterPOsOpenPastTheirETA);
        result.put("filterPartiallyReceivedPOsList", filterPartiallyReceivedPOs);
    }

    protected static EntityExpr makeExpr(String fieldName, String value) {
        return makeExpr(fieldName, value, false);
    }

    protected static EntityExpr makeExpr(String fieldName, String value, boolean forceLike) {
        EntityComparisonOperator op = forceLike ? EntityOperator.LIKE : EntityOperator.EQUALS;

        if (value.startsWith("*")) {
            op = EntityOperator.LIKE;
            value = "%" + value.substring(1);
        }
        else if (value.startsWith("%")) {
            op = EntityOperator.LIKE;
        }

        if (value.endsWith("*")) {
            op = EntityOperator.LIKE;
            value = value.substring(0, value.length() - 1) + "%";
        }
        else if (value.endsWith("%")) {
            op = EntityOperator.LIKE;
        }

        if (forceLike) {
            if (!value.startsWith("%")) {
                value = "%" + value;
            }
            if (!value.endsWith("%")) {
                value = value + "%";
            }
        }

        return new EntityExpr(fieldName, op, value);
    }
}
