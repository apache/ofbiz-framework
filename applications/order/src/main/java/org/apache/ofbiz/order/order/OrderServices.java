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
package org.apache.ofbiz.order.order;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import javax.transaction.Transaction;

import org.apache.commons.lang.StringUtils;
import org.apache.fop.apps.MimeConstants;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.GeneralRuntimeException;
import org.apache.ofbiz.base.util.ObjectType;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilNumber;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.common.DataModelConstants;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntity;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityExpr;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.transaction.GenericTransactionException;
import org.apache.ofbiz.entity.transaction.TransactionUtil;
import org.apache.ofbiz.entity.util.EntityFindOptions;
import org.apache.ofbiz.entity.util.EntityListIterator;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityTypeUtil;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.entity.util.EntityUtilProperties;
import org.apache.ofbiz.order.shoppingcart.CartItemModifyException;
import org.apache.ofbiz.order.shoppingcart.CheckOutHelper;
import org.apache.ofbiz.order.shoppingcart.ItemNotFoundException;
import org.apache.ofbiz.order.shoppingcart.ShoppingCart;
import org.apache.ofbiz.order.shoppingcart.ShoppingCartItem;
import org.apache.ofbiz.order.shoppingcart.product.ProductPromoWorker;
import org.apache.ofbiz.order.shoppingcart.shipping.ShippingEvents;
import org.apache.ofbiz.party.contact.ContactHelper;
import org.apache.ofbiz.party.contact.ContactMechWorker;
import org.apache.ofbiz.party.party.PartyWorker;
import org.apache.ofbiz.product.product.ProductWorker;
import org.apache.ofbiz.product.store.ProductStoreWorker;
import org.apache.ofbiz.security.Security;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.service.ServiceUtil;

import com.ibm.icu.util.Calendar;

/**
 * Order Processing Services
 */

public class OrderServices {

    public static final String module = OrderServices.class.getName();
    public static final String resource = "OrderUiLabels";
    public static final String resource_error = "OrderErrorUiLabels";
    public static final String resourceProduct = "ProductUiLabels";

    private static Map<String, String> salesAttributeRoleMap = new HashMap<>();
    private static Map<String, String> purchaseAttributeRoleMap = new HashMap<>();
    static {
        salesAttributeRoleMap.put("placingCustomerPartyId", "PLACING_CUSTOMER");
        salesAttributeRoleMap.put("billToCustomerPartyId", "BILL_TO_CUSTOMER");
        salesAttributeRoleMap.put("billFromVendorPartyId", "BILL_FROM_VENDOR");
        salesAttributeRoleMap.put("shipToCustomerPartyId", "SHIP_TO_CUSTOMER");
        salesAttributeRoleMap.put("endUserCustomerPartyId", "END_USER_CUSTOMER");

        purchaseAttributeRoleMap.put("billToCustomerPartyId", "BILL_TO_CUSTOMER");
        purchaseAttributeRoleMap.put("billFromVendorPartyId", "BILL_FROM_VENDOR");
        purchaseAttributeRoleMap.put("shipFromVendorPartyId", "SHIP_FROM_VENDOR");
        purchaseAttributeRoleMap.put("supplierAgentPartyId", "SUPPLIER_AGENT");
    }
    public static final int taxDecimals = UtilNumber.getBigDecimalScale("salestax.calc.decimals");
    public static final RoundingMode taxRounding = UtilNumber.getRoundingMode("salestax.rounding");
    public static final int orderDecimals = UtilNumber.getBigDecimalScale("order.decimals");
    public static final RoundingMode orderRounding = UtilNumber.getRoundingMode("order.rounding");
    public static final BigDecimal ZERO = BigDecimal.ZERO.setScale(taxDecimals, taxRounding);


    private static boolean hasPermission(String orderId, GenericValue userLogin, String action, Security security, Delegator delegator) {
        OrderReadHelper orh = new OrderReadHelper(delegator, orderId);
        String orderTypeId = orh.getOrderTypeId();
        String partyId = null;
        GenericValue orderParty = orh.getEndUserParty();
        if (UtilValidate.isEmpty(orderParty)) {
            orderParty = orh.getPlacingParty();
        }
        if (orderParty != null) {
            partyId = orderParty.getString("partyId");
        }
        boolean hasPermission = hasPermission(orderTypeId, partyId, userLogin, action, security);
        if (!hasPermission) {
            GenericValue placingCustomer = null;
            try {
                placingCustomer = EntityQuery.use(delegator).from("OrderRole").where("orderId", orderId, "partyId", userLogin.getString("partyId"), "roleTypeId", "PLACING_CUSTOMER").queryOne();
            } catch (GenericEntityException e) {
                Debug.logError("Could not select OrderRoles for order " + orderId + " due to " + e.getMessage(), module);
            }
            hasPermission = (placingCustomer != null);
        }
        return hasPermission;
    }

    private static boolean hasPermission(String orderTypeId, String partyId, GenericValue userLogin, String action, Security security) {
        boolean hasPermission = security.hasEntityPermission("ORDERMGR", "_" + action, userLogin);
        if (!hasPermission) {
            if ("SALES_ORDER".equals(orderTypeId)) {
                if (security.hasEntityPermission("ORDERMGR", "_SALES_" + action, userLogin)) {
                    hasPermission = true;
                } else {
                    // check sales agent/customer relationship
                    List<GenericValue> repsCustomers = new LinkedList<>();
                    try {
                        repsCustomers = EntityUtil.filterByDate(userLogin.getRelatedOne("Party", false).getRelated("FromPartyRelationship", UtilMisc.toMap("roleTypeIdFrom", "AGENT", "roleTypeIdTo", "CUSTOMER", "partyIdTo", partyId), null, false));
                    } catch (GenericEntityException ex) {
                        Debug.logError("Could not determine if " + partyId + " is a customer of user " + userLogin.getString("userLoginId") + " due to " + ex.getMessage(), module);
                    }
                    if ((repsCustomers != null) && (repsCustomers.size() > 0) && (security.hasEntityPermission("ORDERMGR", "_ROLE_" + action, userLogin))) {
                        hasPermission = true;
                    }
                    if (!hasPermission) {
                        // check sales sales rep/customer relationship
                        try {
                            repsCustomers = EntityUtil.filterByDate(userLogin.getRelatedOne("Party", false).getRelated("FromPartyRelationship", UtilMisc.toMap("roleTypeIdFrom", "SALES_REP", "roleTypeIdTo", "CUSTOMER", "partyIdTo", partyId), null, false));
                        } catch (GenericEntityException ex) {
                            Debug.logError("Could not determine if " + partyId + " is a customer of user " + userLogin.getString("userLoginId") + " due to " + ex.getMessage(), module);
                        }
                        if ((repsCustomers != null) && (repsCustomers.size() > 0) && (security.hasEntityPermission("ORDERMGR", "_ROLE_" + action, userLogin))) {
                            hasPermission = true;
                        }
                    }
                }
            } else if (("PURCHASE_ORDER".equals(orderTypeId) && (security.hasEntityPermission("ORDERMGR", "_PURCHASE_" + action, userLogin)))) {
                hasPermission = true;
            }
        }
        return hasPermission;
    }
    /** Service for creating a new order */
    public static Map<String, Object> createOrder(DispatchContext ctx, Map<String, ? extends Object> context) {
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        Security security = ctx.getSecurity();
        List<GenericValue> toBeStored = new LinkedList<>();
        Locale locale = (Locale) context.get("locale");
        Map<String, Object> successResult = ServiceUtil.returnSuccess();

        GenericValue userLogin = (GenericValue) context.get("userLogin");
        // get the order type
        String orderTypeId = (String) context.get("orderTypeId");
        String partyId = (String) context.get("partyId");
        String billFromVendorPartyId = (String) context.get("billFromVendorPartyId");

        // check security permissions for order:
        //  SALES ORDERS - if userLogin has ORDERMGR_SALES_CREATE or ORDERMGR_CREATE permission, or if it is same party as the partyId, or
        //                 if it is an AGENT (sales rep) creating an order for his customer
        //  PURCHASE ORDERS - if there is a PURCHASE_ORDER permission
        Map<String, Object> resultSecurity = new HashMap<>();
        boolean hasPermission = OrderServices.hasPermission(orderTypeId, partyId, userLogin, "CREATE", security);
        // final check - will pass if userLogin's partyId = partyId for order or if userLogin has ORDERMGR_CREATE permission
        // jacopoc: what is the meaning of this code block? FIXME
        if (!hasPermission) {
            partyId = ServiceUtil.getPartyIdCheckSecurity(userLogin, security, context, resultSecurity, "ORDERMGR", "_CREATE");
            if (resultSecurity.size() > 0) {
                return resultSecurity;
            }
        }

        // get the product store for the order, but it is required only for sales orders
        String productStoreId = (String) context.get("productStoreId");
        GenericValue productStore = null;
        if (("SALES_ORDER".equals(orderTypeId)) && (UtilValidate.isNotEmpty(productStoreId))) {
            try {
                productStore = EntityQuery.use(delegator).from("ProductStore").where("productStoreId", productStoreId).cache().queryOne();
            } catch (GenericEntityException e) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                        "OrderErrorCouldNotFindProductStoreWithID",UtilMisc.toMap("productStoreId",productStoreId),locale)  + e.toString());
            }
        }

        // figure out if the order is immediately fulfilled based on product store settings
        boolean isImmediatelyFulfilled = false;
        if (productStore != null) {
            isImmediatelyFulfilled = "Y".equals(productStore.getString("isImmediatelyFulfilled"));
        }

        successResult.put("orderTypeId", orderTypeId);

        // lookup the order type entity
        GenericValue orderType = null;
        try {
            orderType = EntityQuery.use(delegator).from("OrderType").where("orderTypeId", orderTypeId).cache().queryOne();
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                    "OrderErrorOrderTypeLookupFailed",locale) + e.toString());
        }

        // make sure we have a valid order type
        if (orderType == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                    "OrderErrorInvalidOrderTypeWithID", UtilMisc.toMap("orderTypeId",orderTypeId), locale));
        }

        // check to make sure we have something to order
        List<GenericValue> orderItems = UtilGenerics.checkList(context.get("orderItems"));
        if (orderItems.size() < 1) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error, "items.none", locale));
        }

        // all this marketing pkg auto stuff is deprecated in favor of MARKETING_PKG_AUTO productTypeId and a BOM of MANUF_COMPONENT assocs
        // these need to be retrieved now because they might be needed for exploding MARKETING_PKG_AUTO
        List<GenericValue> orderAdjustments = UtilGenerics.checkList(context.get("orderAdjustments"));
        List<GenericValue> orderItemShipGroupInfo = UtilGenerics.checkList(context.get("orderItemShipGroupInfo"));
        List<GenericValue> orderItemPriceInfo = UtilGenerics.checkList(context.get("orderItemPriceInfos"));

        // check inventory and other things for each item
        List<String> errorMessages = new LinkedList<>();
        Map<String, BigDecimal> normalizedItemQuantities = new HashMap<>();
        Map<String, String> normalizedItemNames = new HashMap<>();
        Map<String, GenericValue> itemValuesBySeqId = new HashMap<>();
        Timestamp nowTimestamp = UtilDateTime.nowTimestamp();

        // need to run through the items combining any cases where multiple lines refer to the
        // same product so the inventory check will work correctly
        // also count quantities ordered while going through the loop
        for (GenericValue orderItem : orderItems) {
            // start by putting it in the itemValuesById Map
            itemValuesBySeqId.put(orderItem.getString("orderItemSeqId"), orderItem);

            String currentProductId = orderItem.getString("productId");
            if (currentProductId != null) {
                // only normalize items with a product associated (ignore non-product items)
                if (normalizedItemQuantities.get(currentProductId) == null) {
                    normalizedItemQuantities.put(currentProductId, orderItem.getBigDecimal("quantity"));
                    normalizedItemNames.put(currentProductId, orderItem.getString("itemDescription"));
                } else {
                    BigDecimal currentQuantity = normalizedItemQuantities.get(currentProductId);
                    normalizedItemQuantities.put(currentProductId, currentQuantity.add(orderItem.getBigDecimal("quantity")));
                }

                try {
                    // count product ordered quantities
                    // run this synchronously so it will run in the same transaction
                    Map<String, Object> result = dispatcher.runSync("countProductQuantityOrdered", UtilMisc.<String, Object>toMap("productId", currentProductId, "quantity", orderItem.getBigDecimal("quantity"), "userLogin", userLogin));
                    if (ServiceUtil.isError(result)) {
                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(result));
                    }
                } catch (GenericServiceException e1) {
                    Debug.logError(e1, "Error calling countProductQuantityOrdered service", module);
                    return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                            "OrderErrorCallingCountProductQuantityOrderedService",locale) + e1.toString());
                }
            }
        }

        if (!"PURCHASE_ORDER".equals(orderTypeId) && productStoreId == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                    "OrderErrorTheProductStoreIdCanOnlyBeNullForPurchaseOrders",locale));
        }

        Timestamp orderDate = (Timestamp) context.get("orderDate");

        for (String currentProductId : normalizedItemQuantities.keySet()) {
            // lookup the product entity for each normalized item; error on products not found
            BigDecimal currentQuantity = normalizedItemQuantities.get(currentProductId);
            String itemName = normalizedItemNames.get(currentProductId);
            GenericValue product = null;

            try {
                product = EntityQuery.use(delegator).from("Product").where("productId", currentProductId).cache().queryOne();
            } catch (GenericEntityException e) {
                String errMsg = UtilProperties.getMessage(resource_error, "product.not_found", new Object[] { currentProductId }, locale);
                Debug.logError(e, errMsg, module);
                errorMessages.add(errMsg);
                continue;
            }

            if (product == null) {
                String errMsg = UtilProperties.getMessage(resource_error, "product.not_found", new Object[] { currentProductId }, locale);
                Debug.logError(errMsg, module);
                errorMessages.add(errMsg);
                continue;
            }

            if ("SALES_ORDER".equals(orderTypeId)) {
                // check to see if introductionDate hasn't passed yet
                if (product.get("introductionDate") != null && nowTimestamp.before(product.getTimestamp("introductionDate"))) {
                    String excMsg = UtilProperties.getMessage(resource_error, "product.not_yet_for_sale",
                            new Object[] { getProductName(product, itemName), product.getString("productId") }, locale);
                    Debug.logWarning(excMsg, module);
                    errorMessages.add(excMsg);
                    continue;
                }
            }

            if ("SALES_ORDER".equals(orderTypeId)) {
                boolean salesDiscontinuationFlag = false;
                // When past orders are imported, they should be imported even if sales discontinuation date is in the past but if the order date was before it
                if (orderDate != null && product.get("salesDiscontinuationDate") != null) {
                    salesDiscontinuationFlag = orderDate.after(product.getTimestamp("salesDiscontinuationDate")) && nowTimestamp.after(product.getTimestamp("salesDiscontinuationDate"));
                } else if (product.get("salesDiscontinuationDate") != null) {
                    salesDiscontinuationFlag = nowTimestamp.after(product.getTimestamp("salesDiscontinuationDate"));
                }
                // check to see if salesDiscontinuationDate has passed
                if (salesDiscontinuationFlag) {
                    String excMsg = UtilProperties.getMessage(resource_error, "product.no_longer_for_sale",
                            new Object[] { getProductName(product, itemName), product.getString("productId") }, locale);
                    Debug.logWarning(excMsg, module);
                    errorMessages.add(excMsg);
                    continue;
                }
            }

            if ("SALES_ORDER".equals(orderTypeId)) {
                // check to see if we have inventory available
                try {
                    Map<String, Object> invReqResult = dispatcher.runSync("isStoreInventoryAvailableOrNotRequired", UtilMisc.toMap("productStoreId", productStoreId, "productId", product.get("productId"), "product", product, "quantity", currentQuantity));
                    if (ServiceUtil.isError(invReqResult)) {
                        errorMessages.add(ServiceUtil.getErrorMessage(invReqResult));
                        List<String> errMsgList = UtilGenerics.checkList(invReqResult.get(ModelService.ERROR_MESSAGE_LIST));
                        errorMessages.addAll(errMsgList);
                    } else if (!"Y".equals(invReqResult.get("availableOrNotRequired"))) {
                        String invErrMsg = UtilProperties.getMessage(resource_error, "product.out_of_stock",
                                new Object[] { getProductName(product, itemName), currentProductId }, locale);
                        Debug.logWarning(invErrMsg, module);
                        errorMessages.add(invErrMsg);
                        continue;
                    }
                } catch (GenericServiceException e) {
                    String errMsg = "Fatal error calling inventory checking services: " + e.toString();
                    Debug.logError(e, errMsg, module);
                    errorMessages.add(errMsg);
                }
            }
        }

        // add the fixedAsset id to the workefforts map by obtaining the fixed Asset number from the FixedAssetProduct table
        List<GenericValue> workEfforts = UtilGenerics.checkList(context.get("workEfforts")); // is an optional parameter from this service but mandatory for rental items
        for (GenericValue orderItem : orderItems) {
            if ("RENTAL_ORDER_ITEM".equals(orderItem.getString("orderItemTypeId"))) {
                // check to see if workefforts are available for this order type.
                if (UtilValidate.isEmpty(workEfforts))    {
                    String errMsg = "Work Efforts missing for ordertype RENTAL_ORDER_ITEM " + "Product: "  + orderItem.getString("productId");
                    Debug.logError(errMsg, module);
                    errorMessages.add(errMsg);
                    return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                            "OrderRentalOrderItems",locale));
                }
                for (GenericValue workEffort : workEfforts) {
                    // find the related workEffortItem (workEffortId = orderSeqId)
                    // create the entity maps required.
                    if (workEffort.getString("workEffortId").equals(orderItem.getString("orderItemSeqId")))    {
                        List<GenericValue> selFixedAssetProduct = null;
                        try {
                            selFixedAssetProduct = EntityQuery.use(delegator).from("FixedAssetProduct").where("productId",orderItem.getString("productId"),"fixedAssetProductTypeId", "FAPT_USE").filterByDate(nowTimestamp, "fromDate", "thruDate").queryList();
                        } catch (GenericEntityException e) {
                            String excMsg = "Could not find related Fixed Asset for the product: " + orderItem.getString("productId");
                            Debug.logError(excMsg, module);
                            errorMessages.add(excMsg);
                            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                                    "OrderCouldNotFindRelatedFixedAssetForTheProduct",UtilMisc.toMap("productId",orderItem.getString("productId")), locale));
                        }

                        if (UtilValidate.isNotEmpty(selFixedAssetProduct)) {
                            Iterator<GenericValue> firstOne = selFixedAssetProduct.iterator();
                            if (firstOne.hasNext()) {
                                GenericValue fixedAssetProduct = delegator.makeValue("FixedAssetProduct");
                                fixedAssetProduct = firstOne.next();
                                workEffort.set("fixedAssetId",fixedAssetProduct.get("fixedAssetId"));
                                workEffort.set("quantityToProduce",orderItem.get("quantity")); // have quantity easy available later...
                                workEffort.set("createdByUserLogin", userLogin.get("userLoginId"));
                            }
                        }
                        break;  // item found, so go to next orderitem.
                    }
                }
            }
        }

        if (errorMessages.size() > 0) {
            return ServiceUtil.returnError(errorMessages);
        }

        // the inital status for ALL order types
        String initialStatus = "ORDER_CREATED";
        successResult.put("statusId", initialStatus);

        // create the order object
        String orderId = (String) context.get("orderId");
        String orgPartyId = null;
        if (productStore != null) {
            orgPartyId = productStore.getString("payToPartyId");
        } else if (billFromVendorPartyId != null) {
            orgPartyId = billFromVendorPartyId;
        }

        if (UtilValidate.isNotEmpty(orgPartyId)) {
            Map<String, Object> getNextOrderIdContext = new HashMap<>();
            getNextOrderIdContext.putAll(context);
            getNextOrderIdContext.put("partyId", orgPartyId);
            getNextOrderIdContext.put("userLogin", userLogin);

            if (("SALES_ORDER".equals(orderTypeId)) || (productStoreId != null)) {
                getNextOrderIdContext.put("productStoreId", productStoreId);
            }
            if (UtilValidate.isEmpty(orderId)) {
                try {
                    getNextOrderIdContext = ctx.makeValidContext("getNextOrderId", ModelService.IN_PARAM, getNextOrderIdContext);
                    Map<String, Object> getNextOrderIdResult = dispatcher.runSync("getNextOrderId", getNextOrderIdContext);
                    if (ServiceUtil.isError(getNextOrderIdResult)) {
                        String errMsg = UtilProperties.getMessage(resource_error,
                                "OrderErrorGettingNextOrderIdWhileCreatingOrder", locale);
                        return ServiceUtil.returnError(errMsg);
                    }
                    orderId = (String) getNextOrderIdResult.get("orderId");
                } catch (GenericServiceException e) {
                    String errMsg = UtilProperties.getMessage(resource_error,
                            "OrderCaughtGenericServiceExceptionWhileGettingOrderId", locale);
                    Debug.logError(e, errMsg, module);
                    return ServiceUtil.returnError(errMsg);
                }
            }
        }

        if (UtilValidate.isEmpty(orderId)) {
            // for purchase orders or when other orderId generation fails, a product store id should not be required to make an order
            orderId = delegator.getNextSeqId("OrderHeader");
        }

        String billingAccountId = (String) context.get("billingAccountId");
        if (orderDate == null) {
            orderDate = nowTimestamp;
        }

        Map<String, Object> orderHeaderMap = UtilMisc.<String, Object>toMap("orderId", orderId, "orderTypeId", orderTypeId,
                "orderDate", orderDate, "entryDate", nowTimestamp,
                "statusId", initialStatus, "billingAccountId", billingAccountId);
        orderHeaderMap.put("orderName", context.get("orderName"));
        orderHeaderMap.put("agreementId", context.get("agreementId"));
        if (isImmediatelyFulfilled) {
            // also flag this order as needing inventory issuance so that when it is set to complete it will be issued immediately (needsInventoryIssuance = Y)
            orderHeaderMap.put("needsInventoryIssuance", "Y");
        }
        GenericValue orderHeader = delegator.makeValue("OrderHeader", orderHeaderMap);

        // determine the sales channel
        String salesChannelEnumId = (String) context.get("salesChannelEnumId");
        if ((salesChannelEnumId == null) || "UNKNWN_SALES_CHANNEL".equals(salesChannelEnumId)) {
            // try the default store sales channel
            if ("SALES_ORDER".equals(orderTypeId) && (productStore != null)) {
                salesChannelEnumId = productStore.getString("defaultSalesChannelEnumId");
            }
            // if there's still no channel, set to unknown channel
            if (salesChannelEnumId == null) {
                salesChannelEnumId = "UNKNWN_SALES_CHANNEL";
            }
        }
        orderHeader.set("salesChannelEnumId", salesChannelEnumId);

        if (context.get("currencyUom") != null) {
            orderHeader.set("currencyUom", context.get("currencyUom"));
        }

        if (context.get("firstAttemptOrderId") != null) {
            orderHeader.set("firstAttemptOrderId", context.get("firstAttemptOrderId"));
        }

        if (context.get("grandTotal") != null) {
            orderHeader.set("grandTotal", context.get("grandTotal"));
        }

        if (UtilValidate.isNotEmpty(context.get("visitId"))) {
            orderHeader.set("visitId", context.get("visitId"));
        }

        if (UtilValidate.isNotEmpty(context.get("internalCode"))) {
            orderHeader.set("internalCode", context.get("internalCode"));
        }

        if (UtilValidate.isNotEmpty(context.get("externalId"))) {
            orderHeader.set("externalId", context.get("externalId"));
        }

        if (UtilValidate.isNotEmpty(context.get("originFacilityId"))) {
            orderHeader.set("originFacilityId", context.get("originFacilityId"));
        }

        if (UtilValidate.isNotEmpty(context.get("productStoreId"))) {
            orderHeader.set("productStoreId", context.get("productStoreId"));
        }

        if (UtilValidate.isNotEmpty(context.get("transactionId"))) {
            orderHeader.set("transactionId", context.get("transactionId"));
        }

        if (UtilValidate.isNotEmpty(context.get("terminalId"))) {
            orderHeader.set("terminalId", context.get("terminalId"));
        }

        if (UtilValidate.isNotEmpty(context.get("autoOrderShoppingListId"))) {
            orderHeader.set("autoOrderShoppingListId", context.get("autoOrderShoppingListId"));
        }

        if (UtilValidate.isNotEmpty(context.get("webSiteId"))) {
            orderHeader.set("webSiteId", context.get("webSiteId"));
        }

        if (userLogin != null && userLogin.get("userLoginId") != null) {
            orderHeader.set("createdBy", userLogin.getString("userLoginId"));
        }
        
        if (UtilValidate.isNotEmpty(context.get("isRushOrder"))) {
            orderHeader.set("isRushOrder", context.get("isRushOrder"));
        }
        
        if (UtilValidate.isNotEmpty(context.get("priority"))) {
            orderHeader.set("priority", context.get("priority"));
        }

        String invoicePerShipment = EntityUtilProperties.getPropertyValue("accounting","create.invoice.per.shipment", delegator);
        if (UtilValidate.isNotEmpty(invoicePerShipment)) {
            orderHeader.set("invoicePerShipment", invoicePerShipment);
        }

        // first try to create the OrderHeader; if this does not fail, continue.
        try {
            delegator.create(orderHeader);
        } catch (GenericEntityException e) {
            Debug.logError(e, "Cannot create OrderHeader entity; problems with insert", module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                    "OrderOrderCreationFailedPleaseNotifyCustomerService",locale));
        }

        // create the order status record
        String orderStatusSeqId = delegator.getNextSeqId("OrderStatus");
        GenericValue orderStatus = delegator.makeValue("OrderStatus", UtilMisc.toMap("orderStatusId", orderStatusSeqId));
        orderStatus.set("orderId", orderId);
        orderStatus.set("statusId", orderHeader.getString("statusId"));
        orderStatus.set("statusDatetime", nowTimestamp);
        orderStatus.set("statusUserLogin", userLogin.getString("userLoginId"));
        toBeStored.add(orderStatus);

        // before processing orderItems process orderItemGroups so that they'll be in place for the foreign keys and what not
        List<GenericValue> orderItemGroups = UtilGenerics.checkList(context.get("orderItemGroups"));
        if (UtilValidate.isNotEmpty(orderItemGroups)) {
            for (GenericValue orderItemGroup : orderItemGroups) {
                orderItemGroup.set("orderId", orderId);
                toBeStored.add(orderItemGroup);
            }
        }

        // set the order items
        for (GenericValue orderItem : orderItems) {
            orderItem.set("orderId", orderId);
            toBeStored.add(orderItem);

            // create the item status record
            String itemStatusId = delegator.getNextSeqId("OrderStatus");
            GenericValue itemStatus = delegator.makeValue("OrderStatus", UtilMisc.toMap("orderStatusId", itemStatusId));
            itemStatus.put("statusId", orderItem.get("statusId"));
            itemStatus.put("orderId", orderId);
            itemStatus.put("orderItemSeqId", orderItem.get("orderItemSeqId"));
            itemStatus.put("statusDatetime", nowTimestamp);
            itemStatus.set("statusUserLogin", userLogin.getString("userLoginId"));
            toBeStored.add(itemStatus);
        }

        // set the order attributes
        List<GenericValue> orderAttributes = UtilGenerics.checkList(context.get("orderAttributes"));
        if (UtilValidate.isNotEmpty(orderAttributes)) {
            for (GenericValue oatt : orderAttributes) {
                oatt.set("orderId", orderId);
                toBeStored.add(oatt);
            }
        }

        // set the order item attributes
        List<GenericValue> orderItemAttributes = UtilGenerics.checkList(context.get("orderItemAttributes"));
        if (UtilValidate.isNotEmpty(orderItemAttributes)) {
            for (GenericValue oiatt : orderItemAttributes) {
                oiatt.set("orderId", orderId);
                toBeStored.add(oiatt);
            }
        }

        // create the order internal notes
        List<String> orderInternalNotes = UtilGenerics.checkList(context.get("orderInternalNotes"));
        if (UtilValidate.isNotEmpty(orderInternalNotes)) {
            for (String orderInternalNote : orderInternalNotes) {
                try {
                    Map<String, Object> noteOutputMap = dispatcher.runSync("createOrderNote", UtilMisc.<String, Object>toMap("orderId", orderId,
                                                                                             "internalNote", "Y",
                                                                                             "note", orderInternalNote,
                                                                                             "userLogin", userLogin));
                    if (ServiceUtil.isError(noteOutputMap)) {
                        return ServiceUtil.returnError(UtilProperties.getMessage(resource,
                                "OrderOrderNoteCannotBeCreated", UtilMisc.toMap("errorString", ""), locale),
                                null, null, noteOutputMap);
                    }
                } catch (GenericServiceException e) {
                    Debug.logError(e, "Error creating internal notes while creating order: " + e.toString(), module);
                    return ServiceUtil.returnError(UtilProperties.getMessage(resource,
                            "OrderOrderNoteCannotBeCreated", UtilMisc.toMap("errorString", e.toString()), locale));
                }
            }
        }

        // create the order public notes
        List<String> orderNotes = UtilGenerics.checkList(context.get("orderNotes"));
        if (UtilValidate.isNotEmpty(orderNotes)) {
            for (String orderNote : orderNotes) {
                try {
                    Map<String, Object> noteOutputMap = dispatcher.runSync("createOrderNote", UtilMisc.<String, Object>toMap("orderId", orderId,
                                                                                             "internalNote", "N",
                                                                                             "note", orderNote,
                                                                                             "userLogin", userLogin));
                    if (ServiceUtil.isError(noteOutputMap)) {
                        return ServiceUtil.returnError(UtilProperties.getMessage(resource,
                            "OrderOrderNoteCannotBeCreated", UtilMisc.toMap("errorString", ""), locale),
                            null, null, noteOutputMap);
                    }
                } catch (GenericServiceException e) {
                    Debug.logError(e, "Error creating notes while creating order: " + e.toString(), module);
                    return ServiceUtil.returnError(UtilProperties.getMessage(resource,
                            "OrderOrderNoteCannotBeCreated", UtilMisc.toMap("errorString", e.toString()), locale));
                }
            }
        }

        // create the workeffort records
        // and connect them with the orderitem over the WorkOrderItemFulfillment
        // create also the techData calendars to keep track of availability of the fixed asset.
        if (UtilValidate.isNotEmpty(workEfforts)) {
            List<GenericValue> tempList = new LinkedList<>();
            for (GenericValue workEffort : workEfforts) {
                // create the entity maps required.
                GenericValue workOrderItemFulfillment = delegator.makeValue("WorkOrderItemFulfillment");
                // find fixed asset supplied on the workeffort map
                GenericValue fixedAsset = null;
                Debug.logInfo("find the fixedAsset",module);
                try {
                    fixedAsset = EntityQuery.use(delegator).from("FixedAsset").where("fixedAssetId", workEffort.get("fixedAssetId")).queryOne();
                }
                catch (GenericEntityException e) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                            "OrderFixedAssetNotFoundFixedAssetId",
                            UtilMisc.toMap("fixedAssetId",workEffort.get("fixedAssetId")), locale));
                }
                if (fixedAsset == null) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                            "OrderFixedAssetNotFoundFixedAssetId",
                            UtilMisc.toMap("fixedAssetId",workEffort.get("fixedAssetId")), locale));
                }
                // see if this fixed asset has a calendar, when no create one and attach to fixed asset
                Debug.logInfo("find the techdatacalendar",module);
                GenericValue techDataCalendar = null;
                try { techDataCalendar = fixedAsset.getRelatedOne("TechDataCalendar", false);
                }
                catch (GenericEntityException e) {
                    Debug.logInfo("TechData calendar does not exist yet so create for fixedAsset: " + fixedAsset.get("fixedAssetId") ,module);
                }
                if (techDataCalendar == null) {
                    for (GenericValue currentValue : tempList) {
                        if ("FixedAsset".equals(currentValue.getEntityName()) && currentValue.getString("fixedAssetId").equals(workEffort.getString("fixedAssetId"))) {
                            fixedAsset = currentValue;
                            break;
                        }
                    }
                    for (GenericValue currentValue : tempList) {
                        if ("TechDataCalendar".equals(currentValue.getEntityName()) && currentValue.getString("calendarId").equals(fixedAsset.getString("calendarId"))) {
                            techDataCalendar = currentValue;
                            break;
                        }
                    }
                }
                if (techDataCalendar == null) {
                    techDataCalendar = delegator.makeValue("TechDataCalendar");
                    Debug.logInfo("create techdata calendar because it does not exist",module);
                    String calendarId = delegator.getNextSeqId("TechDataCalendar");
                    techDataCalendar.set("calendarId", calendarId);
                    tempList.add(techDataCalendar);
                    Debug.logInfo("update fixed Asset",module);
                    fixedAsset.set("calendarId",calendarId);
                    tempList.add(fixedAsset);
                }
                // then create the workEffort and the workOrderItemFulfillment to connect to the order and orderItem
                workOrderItemFulfillment.set("orderItemSeqId", workEffort.get("workEffortId").toString()); // orderItemSeqNo is stored here so save first
                // workeffort
                String workEffortId = delegator.getNextSeqId("WorkEffort"); // find next available workEffortId
                workEffort.set("workEffortId", workEffortId);
                workEffort.set("workEffortTypeId", "ASSET_USAGE");
                workEffort.set("currentStatusId", "_NA_"); // a lot of workefforts selection services expect a value here....
                toBeStored.add(workEffort);  // store workeffort before workOrderItemFulfillment because of workEffortId key constraint
                // workOrderItemFulfillment
                workOrderItemFulfillment.set("workEffortId", workEffortId);
                workOrderItemFulfillment.set("orderId", orderId);
                toBeStored.add(workOrderItemFulfillment);

                // now create the TechDataExcDay, when they do not exist, create otherwise update the capacity values
                // please note that calendarId is the same for (TechData)Calendar, CalendarExcDay and CalendarExWeek
                Timestamp estimatedStartDate = workEffort.getTimestamp("estimatedStartDate");
                Timestamp estimatedCompletionDate = workEffort.getTimestamp("estimatedCompletionDate");
                long dayCount = (estimatedCompletionDate.getTime() - estimatedStartDate.getTime())/86400000;
                while (--dayCount >= 0)    {
                    GenericValue techDataCalendarExcDay = null;
                    // find an existing Day exception record
                    Timestamp exceptionDateStartTime = UtilDateTime.getDayStart(new Timestamp(estimatedStartDate.getTime()),(int)dayCount);
                    try {
                        techDataCalendarExcDay = EntityQuery.use(delegator).from("TechDataCalendarExcDay").where("calendarId", fixedAsset.get("calendarId"), "exceptionDateStartTime", exceptionDateStartTime).queryOne();
                    }
                    catch (GenericEntityException e) {
                        Debug.logInfo(" techData excday record not found so creating........", module);
                    }
                    if (techDataCalendarExcDay == null) {
                        for (GenericValue currentValue : tempList) {
                            if ("TechDataCalendarExcDay".equals(currentValue.getEntityName()) && currentValue.getString("calendarId").equals(fixedAsset.getString("calendarId"))
                                    && currentValue.getTimestamp("exceptionDateStartTime").equals(exceptionDateStartTime)) {
                                techDataCalendarExcDay = currentValue;
                                break;
                            }
                        }
                    }
                    if (techDataCalendarExcDay == null)    {
                        techDataCalendarExcDay = delegator.makeValue("TechDataCalendarExcDay");
                        techDataCalendarExcDay.set("calendarId", fixedAsset.get("calendarId"));
                        techDataCalendarExcDay.set("exceptionDateStartTime", exceptionDateStartTime);
                        techDataCalendarExcDay.set("usedCapacity", BigDecimal.ZERO);  // initialise to zero
                        techDataCalendarExcDay.set("exceptionCapacity", fixedAsset.getBigDecimal("productionCapacity"));
                    }
                    // add the quantity to the quantity on the date record
                    BigDecimal newUsedCapacity = techDataCalendarExcDay.getBigDecimal("usedCapacity").add(workEffort.getBigDecimal("quantityToProduce"));
                    // check to see if the requested quantity is available on the requested day but only when the maximum capacity is set on the fixed asset
                    if (fixedAsset.get("productionCapacity") != null)    {
                       if (newUsedCapacity.compareTo(techDataCalendarExcDay.getBigDecimal("exceptionCapacity")) > 0)    {
                            String errMsg = UtilProperties.getMessage(resource_error, "OrderFixedAssetSoldOut", UtilMisc.toMap("fixedAssetId", workEffort.get("fixedAssetId"), "exceptionDateStartTime", techDataCalendarExcDay.getString("exceptionDateStartTime")), locale);
                            Debug.logError(errMsg, module);
                            errorMessages.add(errMsg);
                            continue;
                        }
                    }
                    techDataCalendarExcDay.set("usedCapacity", newUsedCapacity);
                    tempList.add(techDataCalendarExcDay);
                }
            }
            if (tempList.size() > 0) {
                toBeStored.addAll(tempList);
            }
        }
        if (errorMessages.size() > 0) {
            return ServiceUtil.returnError(errorMessages);
        }

        // set the orderId on all adjustments; this list will include order and
        // item adjustments...
        if (UtilValidate.isNotEmpty(orderAdjustments)) {
            for (GenericValue orderAdjustment : orderAdjustments) {
                try {
                    orderAdjustment.set("orderAdjustmentId", delegator.getNextSeqId("OrderAdjustment"));
                } catch (IllegalArgumentException e) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                            "OrderErrorCouldNotGetNextSequenceIdForOrderAdjustmentCannotCreateOrder",locale));
                }

                orderAdjustment.set("orderId", orderId);
                orderAdjustment.set("createdDate", UtilDateTime.nowTimestamp());
                orderAdjustment.set("createdByUserLogin", userLogin.getString("userLoginId"));

                if (UtilValidate.isEmpty(orderAdjustment.get("orderItemSeqId"))) {
                    orderAdjustment.set("orderItemSeqId", DataModelConstants.SEQ_ID_NA);
                }
                if (UtilValidate.isEmpty(orderAdjustment.get("shipGroupSeqId"))) {
                    orderAdjustment.set("shipGroupSeqId", DataModelConstants.SEQ_ID_NA);
                }
                toBeStored.add(orderAdjustment);
            }
        }

        // set the order contact mechs
        List<GenericValue> orderContactMechs = UtilGenerics.checkList(context.get("orderContactMechs"));
        if (UtilValidate.isNotEmpty(orderContactMechs)) {
            for (GenericValue ocm : orderContactMechs) {
                ocm.set("orderId", orderId);
                toBeStored.add(ocm);
            }
        }

        // set the order item contact mechs
        List<GenericValue> orderItemContactMechs = UtilGenerics.checkList(context.get("orderItemContactMechs"));
        if (UtilValidate.isNotEmpty(orderItemContactMechs)) {
            for (GenericValue oicm : orderItemContactMechs) {
                oicm.set("orderId", orderId);
                toBeStored.add(oicm);
            }
        }

        // set the order item ship groups
        List<String> dropShipGroupIds = new LinkedList<>(); // this list will contain the ids of all the ship groups for drop shipments (no reservations)
        if (UtilValidate.isNotEmpty(orderItemShipGroupInfo)) {
            for (GenericValue valueObj : orderItemShipGroupInfo) {
                valueObj.set("orderId", orderId);
                if ("OrderItemShipGroup".equals(valueObj.getEntityName())) {
                    // ship group
                    if (valueObj.get("carrierRoleTypeId") == null) {
                        valueObj.set("carrierRoleTypeId", "CARRIER");
                    }
                    if (UtilValidate.isNotEmpty(valueObj.getString("supplierPartyId"))) {
                        dropShipGroupIds.add(valueObj.getString("shipGroupSeqId"));
                    }
                } else if ("OrderAdjustment".equals(valueObj.getEntityName())) {
                    // shipping / tax adjustment(s)
                    if (UtilValidate.isEmpty(valueObj.get("orderItemSeqId"))) {
                        valueObj.set("orderItemSeqId", DataModelConstants.SEQ_ID_NA);
                    }
                    valueObj.set("orderAdjustmentId", delegator.getNextSeqId("OrderAdjustment"));
                    valueObj.set("createdDate", UtilDateTime.nowTimestamp());
                    valueObj.set("createdByUserLogin", userLogin.getString("userLoginId"));
                }
                toBeStored.add(valueObj);
            }
        }

        // set the additional party roles
        Map<String, List<String>> additionalPartyRole = UtilGenerics.cast(context.get("orderAdditionalPartyRoleMap"));
        if (additionalPartyRole != null) {
            for (Map.Entry<String, List<String>> entry : additionalPartyRole.entrySet()) {
                String additionalRoleTypeId = entry.getKey();
                List<String> parties = entry.getValue();
                if (parties != null) {
                    for (String additionalPartyId : parties) {
                        toBeStored.add(delegator.makeValue("PartyRole", UtilMisc.toMap("partyId", additionalPartyId, "roleTypeId", additionalRoleTypeId)));
                        toBeStored.add(delegator.makeValue("OrderRole", UtilMisc.toMap("orderId", orderId, "partyId", additionalPartyId, "roleTypeId", additionalRoleTypeId)));
                    }
                }
            }
        }

        // set the item survey responses
        List<GenericValue> surveyResponses = UtilGenerics.checkList(context.get("orderItemSurveyResponses"));
        if (UtilValidate.isNotEmpty(surveyResponses)) {
            for (GenericValue surveyResponse : surveyResponses) {
                surveyResponse.set("orderId", orderId);
                toBeStored.add(surveyResponse);
            }
        }

        // set the item price info; NOTE: this must be after the orderItems are stored for referential integrity
        if (UtilValidate.isNotEmpty(orderItemPriceInfo)) {
            for (GenericValue oipi : orderItemPriceInfo) {
                try {
                    oipi.set("orderItemPriceInfoId", delegator.getNextSeqId("OrderItemPriceInfo"));
                } catch (IllegalArgumentException e) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                            "OrderErrorCouldNotGetNextSequenceIdForOrderItemPriceInfoCannotCreateOrder",locale));
                }

                oipi.set("orderId", orderId);
                toBeStored.add(oipi);
            }
        }

        // set the item associations
        List<GenericValue> orderItemAssociations = UtilGenerics.checkList(context.get("orderItemAssociations"));
        if (UtilValidate.isNotEmpty(orderItemAssociations)) {
            for (GenericValue orderItemAssociation : orderItemAssociations) {
                if (orderItemAssociation.get("toOrderId") == null) {
                    orderItemAssociation.set("toOrderId", orderId);
                } else if (orderItemAssociation.get("orderId") == null) {
                    orderItemAssociation.set("orderId", orderId);
                }
                toBeStored.add(orderItemAssociation);
            }
        }

        // store the orderProductPromoUseInfos
        List<GenericValue> orderProductPromoUses = UtilGenerics.checkList(context.get("orderProductPromoUses"));
        if (UtilValidate.isNotEmpty(orderProductPromoUses)) {
            for (GenericValue productPromoUse  : orderProductPromoUses) {
                productPromoUse.set("orderId", orderId);
                toBeStored.add(productPromoUse);
            }
        }

        // store the orderProductPromoCodes
        Set<String> orderProductPromoCodes = UtilGenerics.cast(context.get("orderProductPromoCodes"));
        if (UtilValidate.isNotEmpty(orderProductPromoCodes)) {
            for (String productPromoCodeId : orderProductPromoCodes) {
                GenericValue orderProductPromoCode = delegator.makeValue("OrderProductPromoCode");
                orderProductPromoCode.set("orderId", orderId);
                orderProductPromoCode.set("productPromoCodeId", productPromoCodeId);
                toBeStored.add(orderProductPromoCode);
            }
        }

        // see the attributeRoleMap definition near the top of this file for attribute-role mappings
        Map<String, String> attributeRoleMap = salesAttributeRoleMap;
        if ("PURCHASE_ORDER".equals(orderTypeId)) {
            attributeRoleMap = purchaseAttributeRoleMap;
        }
        for (Map.Entry<String, String> attributeRoleEntry : attributeRoleMap.entrySet()) {
            if (UtilValidate.isNotEmpty(context.get(attributeRoleEntry.getKey()))) {
                // make sure the party is in the role before adding
                toBeStored.add(delegator.makeValue("PartyRole", UtilMisc.toMap("partyId", context.get(attributeRoleEntry.getKey()), "roleTypeId", attributeRoleEntry.getValue())));
                toBeStored.add(delegator.makeValue("OrderRole", UtilMisc.toMap("orderId", orderId, "partyId", context.get(attributeRoleEntry.getKey()), "roleTypeId", attributeRoleEntry.getValue())));
            }
        }


        // set the affiliate -- This is going to be removed...
        String affiliateId = (String) context.get("affiliateId");
        if (UtilValidate.isNotEmpty(affiliateId)) {
            toBeStored.add(delegator.makeValue("OrderRole",
                    UtilMisc.toMap("orderId", orderId, "partyId", affiliateId, "roleTypeId", "AFFILIATE")));
        }

        // set the distributor
        String distributorId = (String) context.get("distributorId");
        if (UtilValidate.isNotEmpty(distributorId)) {
            toBeStored.add(delegator.makeValue("OrderRole",
                    UtilMisc.toMap("orderId", orderId, "partyId", distributorId, "roleTypeId", "DISTRIBUTOR")));
        }

        // find all parties in role VENDOR associated with WebSite OR ProductStore (where WebSite overrides, if specified), associated first valid with the Order
        if (UtilValidate.isNotEmpty(context.get("productStoreId"))) {
            try {
                GenericValue productStoreRole = EntityQuery.use(delegator).from("ProductStoreRole")
                        .where("roleTypeId", "VENDOR", "productStoreId", context.get("productStoreId"))
                        .orderBy("-fromDate")
                        .filterByDate()
                        .queryFirst();
                if (productStoreRole != null) {
                    toBeStored.add(delegator.makeValue("OrderRole",
                            UtilMisc.toMap("orderId", orderId, "partyId", productStoreRole.get("partyId"), "roleTypeId", "VENDOR")));
                }
            } catch (GenericEntityException e) {
                Debug.logError(e, "Error looking up Vendor for the current Product Store", module);
            }

        }
        if (UtilValidate.isNotEmpty(context.get("webSiteId"))) {
            try {
                GenericValue webSiteRole = EntityQuery.use(delegator).from("WebSiteRole").where("roleTypeId", "VENDOR", "webSiteId", context.get("webSiteId")).orderBy("-fromDate").filterByDate().queryFirst();
                if (webSiteRole != null) {
                    toBeStored.add(delegator.makeValue("OrderRole",
                            UtilMisc.toMap("orderId", orderId, "partyId", webSiteRole.get("partyId"), "roleTypeId", "VENDOR")));
                }
            } catch (GenericEntityException e) {
                Debug.logError(e, "Error looking up Vendor for the current Web Site", module);
            }

        }

        // set the order payment info
        List<GenericValue> orderPaymentInfos = UtilGenerics.checkList(context.get("orderPaymentInfo"));
        if (UtilValidate.isNotEmpty(orderPaymentInfos)) {
            for (GenericValue valueObj : orderPaymentInfos) {
                valueObj.set("orderId", orderId);
                if ("OrderPaymentPreference".equals(valueObj.getEntityName())) {
                    if (valueObj.get("orderPaymentPreferenceId") == null) {
                        valueObj.set("orderPaymentPreferenceId", delegator.getNextSeqId("OrderPaymentPreference"));
                        valueObj.set("createdDate", UtilDateTime.nowTimestamp());
                        valueObj.set("createdByUserLogin", userLogin.getString("userLoginId"));
                    }
                    if (valueObj.get("statusId") == null) {
                        valueObj.set("statusId", "PAYMENT_NOT_RECEIVED");
                    }
                }
                toBeStored.add(valueObj);
            }
        }

        // store the trackingCodeOrder entities
        List<GenericValue> trackingCodeOrders = UtilGenerics.checkList(context.get("trackingCodeOrders"));
        if (UtilValidate.isNotEmpty(trackingCodeOrders)) {
            for (GenericValue trackingCodeOrder : trackingCodeOrders) {
                trackingCodeOrder.set("orderId", orderId);
                toBeStored.add(trackingCodeOrder);
            }
        }

       // store the OrderTerm entities

       List<GenericValue> orderTerms = UtilGenerics.checkList(context.get("orderTerms"));
       if (UtilValidate.isNotEmpty(orderTerms)) {
           for (GenericValue orderTerm : orderTerms) {
               orderTerm.set("orderId", orderId);
               if (orderTerm.get("orderItemSeqId") == null) {
                   orderTerm.set("orderItemSeqId", "_NA_");
               }
               toBeStored.add(orderTerm);
           }
       }

       // if a workEffortId is passed, then prepare a OrderHeaderWorkEffort value
       String workEffortId = (String) context.get("workEffortId");
       if (UtilValidate.isNotEmpty(workEffortId)) {
           GenericValue orderHeaderWorkEffort = delegator.makeValue("OrderHeaderWorkEffort");
           orderHeaderWorkEffort.set("orderId", orderId);
           orderHeaderWorkEffort.set("workEffortId", workEffortId);
           toBeStored.add(orderHeaderWorkEffort);
       }

        try {
            // store line items, etc so that they will be there for the foreign key checks
            delegator.storeAll(toBeStored);

            List<String> resErrorMessages = new LinkedList<>();

            // add a product service to inventory
            if (UtilValidate.isNotEmpty(orderItems)) {
                for (GenericValue orderItem: orderItems) {
                    String productId = (String) orderItem.get("productId");
                    GenericValue product = delegator.getRelatedOne("Product", orderItem, false);

                    if (product != null && ("SERVICE_PRODUCT".equals(product.get("productTypeId")) || "AGGREGATEDSERV_CONF".equals(product.get("productTypeId")))) {
                        String inventoryFacilityId = null;
                        if ("Y".equals(productStore.getString("oneInventoryFacility"))) {
                            inventoryFacilityId = productStore.getString("inventoryFacilityId");

                            if (UtilValidate.isEmpty(inventoryFacilityId)) {
                                Debug.logWarning("ProductStore with id " + productStoreId + " has Y for oneInventoryFacility but inventoryFacilityId is empty, returning false for inventory check", module);
                            }
                        } else {
                            List<GenericValue> productFacilities = null;
                            GenericValue productFacility = null;

                            try {
                                productFacilities = product.getRelated("ProductFacility", null, null, true);
                            } catch (GenericEntityException e) {
                                Debug.logWarning(e, "Error invoking getRelated in isCatalogInventoryAvailable", module);
                            }

                            if (UtilValidate.isNotEmpty(productFacilities)) {
                                productFacility = EntityUtil.getFirst(productFacilities);
                                inventoryFacilityId = (String) productFacility.get("facilityId");
                            }
                        }

                        Map<String, Object> ripCtx = new HashMap<>();
                        if (UtilValidate.isNotEmpty(inventoryFacilityId) && UtilValidate.isNotEmpty(productId) && orderItem.getBigDecimal("quantity").compareTo(BigDecimal.ZERO) > 0) {
                            // do something tricky here: run as the "system" user
                            GenericValue permUserLogin = EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "system").cache().queryOne();
                            ripCtx.put("productId", productId);
                            ripCtx.put("facilityId", inventoryFacilityId);
                            ripCtx.put("inventoryItemTypeId", "SERIALIZED_INV_ITEM");
                            ripCtx.put("statusId","INV_AVAILABLE");
                            ripCtx.put("quantityAccepted", orderItem.getBigDecimal("quantity"));
                            ripCtx.put("quantityRejected", 0.0);
                            ripCtx.put("userLogin", permUserLogin);
                            try {
                                Map<String, Object> ripResult = dispatcher.runSync("receiveInventoryProduct", ripCtx);
                                if (ServiceUtil.isError(ripResult)) {
                                    String errMsg = ServiceUtil.getErrorMessage(ripResult);
                                    @SuppressWarnings("unchecked")
                                    Collection<? extends String> map = (Collection<? extends String>) UtilMisc.<String, String>toMap("reasonCode", "ReceiveInventoryServiceError", "description", errMsg);
                                    resErrorMessages.addAll(map);
                                }
                            } catch (GenericServiceException e) {
                                Debug.logWarning(e, "Error invoking receiveInventoryProduct service in createOrder", module);
                            }
                        }
                    }
                }
            }

            // START inventory reservation
            try {
                reserveInventory(delegator, dispatcher, userLogin, locale, orderItemShipGroupInfo, dropShipGroupIds, itemValuesBySeqId,
                        orderTypeId, productStoreId, resErrorMessages);
            } catch (GeneralException e) {
                return ServiceUtil.returnError(e.getMessage());
            }

            if (resErrorMessages.size() > 0) {
                return ServiceUtil.returnError(resErrorMessages);
            }
            // END inventory reservation

            successResult.put("orderId", orderId);
        } catch (GenericEntityException e) {
            Debug.logError(e, "Problem with order storage or reservations", module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                    "OrderErrorCouldNotCreateOrderWriteError",locale) + e.getMessage() + ").");
        }

        return successResult;
    }

    public static Map<String, Object> countProductQuantityOrdered(DispatchContext ctx, Map<String, Object> context) {
        Delegator delegator = ctx.getDelegator();
        Locale locale = (Locale) context.get("locale");
        List<GenericValue> productCalculatedInfoList = null;
        GenericValue productCalculatedInfo = null;
        String productId = (String) context.get("productId");
        BigDecimal quantity = (BigDecimal) context.get("quantity");
        try {
            productCalculatedInfoList = EntityQuery.use(delegator).from("ProductCalculatedInfo").where("productId", productId).queryList();
            if (UtilValidate.isEmpty(productCalculatedInfoList)) {
                productCalculatedInfo = delegator.makeValue("ProductCalculatedInfo");
                productCalculatedInfo.set("productId", productId);
                productCalculatedInfo.set("totalQuantityOrdered", quantity);
                productCalculatedInfo.create();
            } else {
                productCalculatedInfo = productCalculatedInfoList.get(0);
                BigDecimal totalQuantityOrdered = productCalculatedInfo.getBigDecimal("totalQuantityOrdered");
                if (totalQuantityOrdered == null) {
                    productCalculatedInfo.set("totalQuantityOrdered", quantity);
                } else {
                    productCalculatedInfo.set("totalQuantityOrdered", totalQuantityOrdered.add(quantity));
                }
            }
            productCalculatedInfo.store();
        } catch (GenericEntityException e) {
            Debug.logError(e, "Error calling countProductQuantityOrdered service", module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                    "OrderErrorCallingCountProductQuantityOrderedService",locale) + e.toString());

        }

        String virtualProductId = null;
        try {
            GenericValue product = EntityQuery.use(delegator).from("Product").where("productId", productId).cache(true).queryOne();
            virtualProductId = ProductWorker.getVariantVirtualId(product);
        } catch (GenericEntityException e) {
            Debug.logError(e, "Error calling countProductQuantityOrdered service", module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                    "OrderErrorCallingCountProductQuantityOrderedService",locale) + e.toString());
        }

        if (UtilValidate.isNotEmpty(virtualProductId)) {
            context.put("productId", virtualProductId);
            countProductQuantityOrdered(ctx, context);
        }
        return ServiceUtil.returnSuccess();
    }

    public static void reserveInventory(Delegator delegator, LocalDispatcher dispatcher, GenericValue userLogin, Locale locale, List<GenericValue> orderItemShipGroupInfo, List<String> dropShipGroupIds, Map<String, GenericValue> itemValuesBySeqId, String orderTypeId, String productStoreId, List<String> resErrorMessages) throws GeneralException {
        boolean isImmediatelyFulfilled = false;
        GenericValue productStore = null;
        if (UtilValidate.isNotEmpty(productStoreId)) {
            try {
                productStore = EntityQuery.use(delegator).from("ProductStore").where("productStoreId", productStoreId).cache().queryOne();
            } catch (GenericEntityException e) {
                throw new GeneralException(UtilProperties.getMessage(resource_error,
                        "OrderErrorCouldNotFindProductStoreWithID",
                        UtilMisc.toMap("productStoreId", productStoreId), locale) + e.toString());
            }
        }
        if (productStore != null) {
            isImmediatelyFulfilled = "Y".equals(productStore.getString("isImmediatelyFulfilled"));
        }

        boolean reserveInventory = ("SALES_ORDER".equals(orderTypeId));
        if (reserveInventory && isImmediatelyFulfilled) {
            // don't reserve inventory if the product store has isImmediatelyFulfilled set, ie don't if in this store things are immediately fulfilled
            reserveInventory = false;
        }

        // START inventory reservation
        // decrement inventory available for each OrderItemShipGroupAssoc, within the same transaction
        if (UtilValidate.isNotEmpty(orderItemShipGroupInfo)) {
            for (GenericValue orderItemShipGroupAssoc : orderItemShipGroupInfo) {
                if ("OrderItemShipGroupAssoc".equals(orderItemShipGroupAssoc.getEntityName())) {
                    if (dropShipGroupIds != null && dropShipGroupIds.contains(orderItemShipGroupAssoc.getString("shipGroupSeqId"))) {
                        // the items in the drop ship groups are not reserved
                        continue;
                    }
                    GenericValue orderItem = itemValuesBySeqId.get(orderItemShipGroupAssoc.get("orderItemSeqId"));
                    if ("SALES_ORDER".equals(orderTypeId) && orderItem != null && productStore != null && "Y".equals(productStore.getString("allocateInventory"))) {
                        //If the 'autoReserve' flag is not set for the order item, don't reserve the inventory
                        String autoReserve = OrderReadHelper.getOrderItemAttribute(orderItem, "autoReserve");
                        if (autoReserve == null || !"true".equals(autoReserve)) {
                             continue;
                        }
                    }
                    if ("SALES_ORDER".equals(orderTypeId) && orderItem != null) {
                        //If the 'reserveAfterDate' is not yet come don't reserve the inventory
                        Timestamp reserveAfterDate = orderItem.getTimestamp("reserveAfterDate");
                        if (UtilValidate.isNotEmpty(reserveAfterDate) && reserveAfterDate.after(UtilDateTime.nowTimestamp())) {
                            continue;
                        }
                    }
                    GenericValue orderItemShipGroup = orderItemShipGroupAssoc.getRelatedOne("OrderItemShipGroup", false);
                    String shipGroupFacilityId = orderItemShipGroup.getString("facilityId");
                    String itemStatus = orderItem.getString("statusId");
                    if ("ITEM_REJECTED".equals(itemStatus) || "ITEM_CANCELLED".equals(itemStatus) || "ITEM_COMPLETED".equals(itemStatus)) {
                        Debug.logInfo("Order item [" + orderItem.getString("orderId") + " / " + orderItem.getString("orderItemSeqId") + "] is not in a proper status for reservation", module);
                        continue;
                    }
                    if (UtilValidate.isNotEmpty(orderItem.getString("productId")) &&   // only reserve product items, ignore non-product items
                            !"RENTAL_ORDER_ITEM".equals(orderItem.getString("orderItemTypeId"))) {  // ignore for rental
                        try {
                            // get the product of the order item
                            GenericValue product = orderItem.getRelatedOne("Product", false);
                            if (product == null) {
                                Debug.logError("Error when looking up product in reserveInventory service", module);
                                resErrorMessages.add("Error when looking up product in reserveInventory service");
                                continue;
                            }
                            if (reserveInventory) {
                                // for MARKETING_PKG_PICK reserve the components
                                if (EntityTypeUtil.hasParentType(delegator, "ProductType", "productTypeId", product.getString("productTypeId"), "parentTypeId", "MARKETING_PKG_PICK")) {
                                    Map<String, Object> componentsRes = dispatcher.runSync("getAssociatedProducts", UtilMisc.toMap("productId", orderItem.getString("productId"), "type", "PRODUCT_COMPONENT"));
                                    if (ServiceUtil.isError(componentsRes)) {
                                        resErrorMessages.add(ServiceUtil.getErrorMessage(componentsRes));
                                        continue;
                                    }
                                    List<GenericValue> assocProducts = UtilGenerics.checkList(componentsRes.get("assocProducts"));
                                    for (GenericValue productAssoc : assocProducts) {
                                        BigDecimal quantityOrd = productAssoc.getBigDecimal("quantity");
                                        BigDecimal quantityKit = orderItemShipGroupAssoc.getBigDecimal("quantity");
                                        BigDecimal quantity = quantityOrd.multiply(quantityKit);
                                        Map<String, Object> reserveInput = new HashMap<>();
                                        reserveInput.put("productStoreId", productStoreId);
                                        reserveInput.put("productId", productAssoc.getString("productIdTo"));
                                        reserveInput.put("orderId", orderItem.getString("orderId"));
                                        reserveInput.put("orderItemSeqId", orderItem.getString("orderItemSeqId"));
                                        reserveInput.put("shipGroupSeqId", orderItemShipGroupAssoc.getString("shipGroupSeqId"));
                                        reserveInput.put("quantity", quantity);
                                        reserveInput.put("userLogin", userLogin);
                                        reserveInput.put("facilityId", shipGroupFacilityId);
                                        Map<String, Object> reserveResult = dispatcher.runSync("reserveStoreInventory", reserveInput);
                                        if (ServiceUtil.isError(reserveResult)) {
                                            String invErrMsg = "The product ";
                                            invErrMsg += getProductName(product, orderItem);
                                            invErrMsg += " with ID " + orderItem.getString("productId") + " is no longer in stock. Please try reducing the quantity or removing the product from this order.";
                                            resErrorMessages.add(invErrMsg);
                                        }
                                    }
                                } else {
                                    // reserve the product
                                    Map<String, Object> reserveInput = new HashMap<>();
                                    reserveInput.put("productStoreId", productStoreId);
                                    reserveInput.put("productId", orderItem.getString("productId"));
                                    reserveInput.put("orderId", orderItem.getString("orderId"));
                                    reserveInput.put("orderItemSeqId", orderItem.getString("orderItemSeqId"));
                                    reserveInput.put("shipGroupSeqId", orderItemShipGroupAssoc.getString("shipGroupSeqId"));
                                    reserveInput.put("facilityId", shipGroupFacilityId);
                                    // use the quantity from the orderItemShipGroupAssoc, NOT the orderItem, these are reserved by item-group assoc
                                    reserveInput.put("quantity", orderItemShipGroupAssoc.getBigDecimal("quantity"));
                                    reserveInput.put("userLogin", userLogin);
                                    Map<String, Object> reserveResult = dispatcher.runSync("reserveStoreInventory", reserveInput);

                                    if (ServiceUtil.isError(reserveResult)) {
                                        String invErrMsg = "The product ";
                                        invErrMsg += getProductName(product, orderItem);
                                        invErrMsg += " with ID " + orderItem.getString("productId") + " is no longer in stock. Please try reducing the quantity or removing the product from this order.";
                                        resErrorMessages.add(invErrMsg);
                                    }
                                }
                            }
                            // Reserving inventory or not we still need to create a marketing package
                            // If the product is a marketing package auto, attempt to create enough packages to bring ATP back to 0, won't necessarily create enough to cover this order.
                            if (EntityTypeUtil.hasParentType(delegator, "ProductType", "productTypeId", product.getString("productTypeId"), "parentTypeId", "MARKETING_PKG_AUTO")) {
                                // do something tricky here: run as the "system" user
                                // that can actually create and run a production run
                                GenericValue permUserLogin = EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "system").cache().queryOne();
                                Map<String, Object> inputMap = new HashMap<>();
                                if (UtilValidate.isNotEmpty(shipGroupFacilityId)) {
                                    inputMap.put("facilityId", shipGroupFacilityId);
                                } else {
                                    inputMap.put("facilityId", productStore.getString("inventoryFacilityId"));
                                }
                                inputMap.put("orderId", orderItem.getString("orderId"));
                                inputMap.put("orderItemSeqId", orderItem.getString("orderItemSeqId"));
                                inputMap.put("userLogin", permUserLogin);
                                Map<String, Object> prunResult = dispatcher.runSync("createProductionRunForMktgPkg", inputMap);
                                if (ServiceUtil.isError(prunResult)) {
                                    Debug.logError(ServiceUtil.getErrorMessage(prunResult) + " for input:" + inputMap, module);
                                }
                            }
                        } catch (GenericServiceException e) {
                            String errMsg = "Fatal error calling reserveStoreInventory service: " + e.toString();
                            Debug.logError(e, errMsg, module);
                            resErrorMessages.add(errMsg);
                        }
                    }

                    // rent item
                    if (UtilValidate.isNotEmpty(orderItem.getString("productId")) && "RENTAL_ORDER_ITEM".equals(orderItem.getString("orderItemTypeId"))) {
                        try {
                            // get the product of the order item
                            GenericValue product = orderItem.getRelatedOne("Product", false);
                            if (product == null) {
                                Debug.logError("Error when looking up product in reserveInventory service", module);
                                resErrorMessages.add("Error when looking up product in reserveInventory service");
                                continue;
                            }

                            // check product type for rent
                            String productType = (String) product.get("productTypeId");
                            if ("ASSET_USAGE_OUT_IN".equals(productType)) {
                                if (reserveInventory) {
                                    // for MARKETING_PKG_PICK reserve the components
                                    if (EntityTypeUtil.hasParentType(delegator, "ProductType", "productTypeId", product.getString("productTypeId"), "parentTypeId", "MARKETING_PKG_PICK")) {
                                        Map<String, Object> componentsRes = dispatcher.runSync("getAssociatedProducts", UtilMisc.toMap("productId", orderItem.getString("productId"), "type", "PRODUCT_COMPONENT"));
                                        if (ServiceUtil.isError(componentsRes)) {
                                            resErrorMessages.add((String)componentsRes.get(ModelService.ERROR_MESSAGE));
                                            continue;
                                        }
                                        List<GenericValue> assocProducts = UtilGenerics.checkList(componentsRes.get("assocProducts"));
                                        for (GenericValue productAssoc : assocProducts) {
                                            BigDecimal quantityOrd = productAssoc.getBigDecimal("quantity");
                                            BigDecimal quantityKit = orderItemShipGroupAssoc.getBigDecimal("quantity");
                                            BigDecimal quantity = quantityOrd.multiply(quantityKit);
                                            Map<String, Object> reserveInput = new HashMap<>();
                                            reserveInput.put("productStoreId", productStoreId);
                                            reserveInput.put("productId", productAssoc.getString("productIdTo"));
                                            reserveInput.put("orderId", orderItem.getString("orderId"));
                                            reserveInput.put("orderItemSeqId", orderItem.getString("orderItemSeqId"));
                                            reserveInput.put("shipGroupSeqId", orderItemShipGroupAssoc.getString("shipGroupSeqId"));
                                            reserveInput.put("quantity", quantity);
                                            reserveInput.put("userLogin", userLogin);
                                            reserveInput.put("facilityId", shipGroupFacilityId);
                                            Map<String, Object> reserveResult = dispatcher.runSync("reserveStoreInventory", reserveInput);

                                            if (ServiceUtil.isError(reserveResult)) {
                                                String invErrMsg = "The product ";
                                                invErrMsg += getProductName(product, orderItem);
                                                invErrMsg += " with ID " + orderItem.getString("productId") + " is no longer in stock. Please try reducing the quantity or removing the product from this order.";
                                                resErrorMessages.add(invErrMsg);
                                            }
                                        }
                                    } else {
                                        // reserve the product
                                        Map<String, Object> reserveInput = new HashMap<>();
                                        reserveInput.put("productStoreId", productStoreId);
                                        reserveInput.put("productId", orderItem.getString("productId"));
                                        reserveInput.put("orderId", orderItem.getString("orderId"));
                                        reserveInput.put("orderItemSeqId", orderItem.getString("orderItemSeqId"));
                                        reserveInput.put("shipGroupSeqId", orderItemShipGroupAssoc.getString("shipGroupSeqId"));
                                        reserveInput.put("facilityId", shipGroupFacilityId);
                                        // use the quantity from the orderItemShipGroupAssoc, NOT the orderItem, these are reserved by item-group assoc
                                        reserveInput.put("quantity", orderItemShipGroupAssoc.getBigDecimal("quantity"));
                                        reserveInput.put("userLogin", userLogin);
                                        Map<String, Object> reserveResult = dispatcher.runSync("reserveStoreInventory", reserveInput);

                                        if (ServiceUtil.isError(reserveResult)) {
                                            String invErrMsg = "The product ";
                                            invErrMsg += getProductName(product, orderItem);
                                            invErrMsg += " with ID " + orderItem.getString("productId") + " is no longer in stock. Please try reducing the quantity or removing the product from this order.";
                                            resErrorMessages.add(invErrMsg);
                                        }
                                    }
                                }

                                if (EntityTypeUtil.hasParentType(delegator, "ProductType", "productTypeId", product.getString("productTypeId"), "parentTypeId", "MARKETING_PKG_AUTO")) {
                                    GenericValue permUserLogin = EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "system").cache().queryOne();
                                    Map<String, Object> inputMap = new HashMap<>();
                                    if (UtilValidate.isNotEmpty(shipGroupFacilityId)) {
                                        inputMap.put("facilityId", shipGroupFacilityId);
                                    } else {
                                        inputMap.put("facilityId", productStore.getString("inventoryFacilityId"));
                                    }
                                    inputMap.put("orderId", orderItem.getString("orderId"));
                                    inputMap.put("orderItemSeqId", orderItem.getString("orderItemSeqId"));
                                    inputMap.put("userLogin", permUserLogin);
                                    Map<String, Object> prunResult = dispatcher.runSync("createProductionRunForMktgPkg", inputMap);
                                    if (ServiceUtil.isError(prunResult)) {
                                        Debug.logError(ServiceUtil.getErrorMessage(prunResult) + " for input:" + inputMap, module);
                                    }
                                }
                            }
                        } catch (GenericServiceException e) {
                            String errMsg = "Fatal error calling reserveStoreInventory service: " + e.toString();
                            Debug.logError(e, errMsg, module);
                            resErrorMessages.add(errMsg);
                        }
                    }
                }
            }
        }
    }

    public static String getProductName(GenericValue product, GenericValue orderItem) {
        if (UtilValidate.isNotEmpty(product.getString("productName"))) {
            return product.getString("productName");
        }
        return orderItem.getString("itemDescription");
    }

    public static String getProductName(GenericValue product, String orderItemName) {
        if (UtilValidate.isNotEmpty(product.getString("productName"))) {
            return product.getString("productName");
        }
        return orderItemName;
    }

    public static String determineSingleFacilityFromOrder(GenericValue orderHeader) {
        if (orderHeader != null) {
            String productStoreId = orderHeader.getString("productStoreId");
            if (productStoreId != null) {
                return ProductStoreWorker.determineSingleFacilityForStore(orderHeader.getDelegator(), productStoreId);
            }
        }
        return null;
    }

    /** Service for resetting the OrderHeader grandTotal */
    public static Map<String, Object> resetGrandTotal(DispatchContext ctx, Map<String, ? extends Object> context) {
        Delegator delegator = ctx.getDelegator();
        Locale locale = (Locale) context.get("locale");
        String orderId = (String) context.get("orderId");

        GenericValue orderHeader = null;
        try {
            orderHeader = EntityQuery.use(delegator).from("OrderHeader").where("orderId", orderId).queryOne();
        } catch (GenericEntityException e) {
            String errMsg = UtilProperties.getMessage(resource_error, "OrderCouldNotSetGrantTotalOnOrderHeader", UtilMisc.toMap("errorString", e.toString()), locale);
            Debug.logError(e, errMsg, module);
            return ServiceUtil.returnError(errMsg);
        }

        if (orderHeader != null) {
            OrderReadHelper orh = new OrderReadHelper(orderHeader);
            BigDecimal currentTotal = orderHeader.getBigDecimal("grandTotal");
            BigDecimal currentSubTotal = orderHeader.getBigDecimal("remainingSubTotal");

            // get the new grand total
            BigDecimal updatedTotal = orh.getOrderGrandTotal();

            String productStoreId = orderHeader.getString("productStoreId");
            String showPricesWithVatTax = null;
            if (UtilValidate.isNotEmpty(productStoreId)) {
                GenericValue productStore = null;
                try {
                    productStore = EntityQuery.use(delegator).from("ProductStore").where("productStoreId", productStoreId).cache().queryOne();
                } catch (GenericEntityException e) {
                    String errorMessage = UtilProperties.getMessage(resource_error,
                            "OrderErrorCouldNotFindProductStoreWithID",
                            UtilMisc.toMap("productStoreId", productStoreId), (Locale) context.get("locale")) + e.toString();
                    Debug.logError(e, errorMessage, module);
                    return ServiceUtil.returnError(errorMessage + e.getMessage() + ").");
                }
                showPricesWithVatTax  = productStore.getString("showPricesWithVatTax");
            }
            BigDecimal remainingSubTotal = ZERO;
            if (UtilValidate.isNotEmpty(productStoreId) && "Y".equalsIgnoreCase(showPricesWithVatTax)) {
                // calculate subTotal as grandTotal + taxes - (returnsTotal + shipping of all items)
                remainingSubTotal = updatedTotal.subtract(orh.getOrderReturnedTotal()).subtract(orh.getShippingTotal());
            } else {
                // calculate subTotal as grandTotal - returnsTotal - (tax + shipping of items not returned)
                remainingSubTotal = updatedTotal.subtract(orh.getOrderReturnedTotal()).subtract(orh.getOrderNonReturnedTaxAndShipping());
            }

            if (currentTotal == null || currentSubTotal == null || updatedTotal.compareTo(currentTotal) != 0 ||
                    remainingSubTotal.compareTo(currentSubTotal) != 0) {
                orderHeader.set("grandTotal", updatedTotal);
                orderHeader.set("remainingSubTotal", remainingSubTotal);
                try {
                    orderHeader.store();
                } catch (GenericEntityException e) {
                    String errMsg = UtilProperties.getMessage(resource_error, "OrderCouldNotSetGrantTotalOnOrderHeader", UtilMisc.toMap("errorString", e.toString()), locale);
                    Debug.logError(e, errMsg, module);
                    return ServiceUtil.returnError(errMsg);
                }
            }
        }

        return ServiceUtil.returnSuccess();
    }

    /** Service for setting the OrderHeader grandTotal for all OrderHeaders with no grandTotal */
    public static Map<String, Object> setEmptyGrandTotals(DispatchContext ctx, Map<String, ? extends Object> context) {
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Boolean forceAll = (Boolean) context.get("forceAll");
        Locale locale = (Locale) context.get("locale");
        if (forceAll == null) {
            forceAll = Boolean.FALSE;
        }

        EntityCondition cond = null;
        if (!forceAll) {
            List<EntityExpr> exprs = UtilMisc.toList(EntityCondition.makeCondition("grandTotal", EntityOperator.EQUALS, null),
                    EntityCondition.makeCondition("remainingSubTotal", EntityOperator.EQUALS, null));
            cond = EntityCondition.makeCondition(exprs, EntityOperator.OR);
        }

        try (EntityListIterator eli = EntityQuery.use(delegator).select("orderId").from("OrderHeader").where(cond).queryIterator()) {
            if (eli != null) {
                // reset each order
                GenericValue orderHeader = null;
                while ((orderHeader = eli.next()) != null) {
                    String orderId = orderHeader.getString("orderId");
                    Map<String, Object> resetResult = null;
                    try {
                        resetResult = dispatcher.runSync("resetGrandTotal", UtilMisc.<String, Object>toMap("orderId", orderId, "userLogin", userLogin));
                        if (ServiceUtil.isError(resetResult)) {
                            return ServiceUtil.returnError(ServiceUtil.getErrorMessage(resetResult));
                        }
                    } catch (GenericServiceException e) {
                        Debug.logError(e, "ERROR: Cannot reset order totals - " + orderId, module);
                    }

                    if (resetResult != null && ServiceUtil.isError(resetResult)) {
                        Debug.logWarning(UtilProperties.getMessage(resource_error,
                                "OrderErrorCannotResetOrderTotals",
                                UtilMisc.toMap("orderId",orderId,"resetResult",ServiceUtil.getErrorMessage(resetResult)), locale), module);
                    } else {
                        Debug.logInfo("No orders found for reset processing", module);
                    }
                }
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
        return ServiceUtil.returnSuccess();
    }

    /** Service for checking and re-calc the tax amount */
    public static Map<String, Object> recalcOrderTax(DispatchContext ctx, Map<String, ? extends Object> context) {
        LocalDispatcher dispatcher = ctx.getDispatcher();
        Delegator delegator = ctx.getDelegator();
        String orderId = (String) context.get("orderId");
        String orderItemSeqId = (String) context.get("orderItemSeqId");
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");

        // check and make sure we have permission to change the order
        Security security = ctx.getSecurity();
        boolean hasPermission = OrderServices.hasPermission(orderId, userLogin, "UPDATE", security, delegator);
        if (!hasPermission) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                    "OrderYouDoNotHavePermissionToChangeThisOrdersStatus",locale));
        }

        // get the order header
        GenericValue orderHeader = null;
        try {
            orderHeader = EntityQuery.use(delegator).from("OrderHeader").where("orderId", orderId).queryOne();
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                    "OrderErrorCannotGetOrderHeaderEntity",locale) + e.getMessage());
        }

        if (orderHeader == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                    "OrderErrorNoValidOrderHeaderFoundForOrderId", UtilMisc.toMap("orderId",orderId), locale));
        }

        // Retrieve the order tax adjustments
        List<GenericValue> orderTaxAdjustments = null;
        try {
            orderTaxAdjustments = EntityQuery.use(delegator).from("OrderAdjustment").where("orderId", orderId, "orderAdjustmentTypeId", "SALES_TAX").queryList();
        } catch (GenericEntityException e) {
            Debug.logError(e, "Unable to retrieve SALES_TAX adjustments for order : " + orderId, module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                    "OrderUnableToRetrieveSalesTaxAdjustments",locale));
        }

        // Accumulate the total existing tax adjustment
        BigDecimal totalExistingOrderTax = ZERO;
        for (GenericValue orderTaxAdjustment : orderTaxAdjustments) {
            if (orderTaxAdjustment.get("amount") != null) {
                totalExistingOrderTax = totalExistingOrderTax.add(orderTaxAdjustment.getBigDecimal("amount").setScale(taxDecimals, taxRounding));
            }
        }

        // Accumulate the total manually added tax adjustment
        BigDecimal totalManuallyAddedOrderTax = ZERO;
        for (GenericValue orderTaxAdjustment : orderTaxAdjustments) {
            if (orderTaxAdjustment.get("amount") != null && "Y".equals(orderTaxAdjustment.getString("isManual"))) {
                totalManuallyAddedOrderTax = totalManuallyAddedOrderTax.add(orderTaxAdjustment.getBigDecimal("amount").setScale(taxDecimals, taxRounding));
            }
        }

        // Recalculate the taxes for the order
        BigDecimal totalNewOrderTax = ZERO;
        OrderReadHelper orh = new OrderReadHelper(orderHeader);
        List<GenericValue> shipGroups = orh.getOrderItemShipGroups();
        if (shipGroups != null) {
            for (GenericValue shipGroup : shipGroups) {
                String shipGroupSeqId = shipGroup.getString("shipGroupSeqId");

                List<GenericValue> validOrderItems = orh.getValidOrderItems(shipGroupSeqId);
                if (validOrderItems != null) {
                    // prepare the inital lists
                    List<GenericValue> products = new ArrayList<>(validOrderItems.size());
                    List<BigDecimal> amounts = new ArrayList<>(validOrderItems.size());
                    List<BigDecimal> shipAmts = new ArrayList<>(validOrderItems.size());
                    List<BigDecimal> itPrices = new ArrayList<>(validOrderItems.size());
                    List<BigDecimal> itQuantities = new ArrayList<>(validOrderItems.size());

                    // adjustments and total
                    List<GenericValue> allAdjustments = orh.getAdjustments();
                    List<GenericValue> orderHeaderAdjustments = OrderReadHelper.getOrderHeaderAdjustments(allAdjustments, shipGroupSeqId);
                    BigDecimal orderSubTotal = OrderReadHelper.getOrderItemsSubTotal(validOrderItems, allAdjustments);

                    // shipping amount
                    BigDecimal orderShipping = OrderReadHelper.calcOrderAdjustments(orderHeaderAdjustments, orderSubTotal, false, false, true);

                    //promotions amount
                    BigDecimal orderPromotions = OrderReadHelper.calcOrderPromoAdjustmentsBd(allAdjustments);

                    // build up the list of tax calc service parameters
                    for (int i = 0; i < validOrderItems.size(); i++) {
                        GenericValue orderItem = validOrderItems.get(i);
                        String productId = orderItem.getString("productId");
                        try {
                            products.add(i, EntityQuery.use(delegator).from("Product").where("productId", productId).queryOne());  // get the product entity
                            amounts.add(i, OrderReadHelper.getOrderItemSubTotal(orderItem, allAdjustments, true, false)); // get the item amount
                            shipAmts.add(i, OrderReadHelper.getOrderItemAdjustmentsTotal(orderItem, allAdjustments, false, false, true)); // get the shipping amount
                            itPrices.add(i, orderItem.getBigDecimal("unitPrice"));
                            itQuantities.add(i, orderItem.getBigDecimal("quantity"));
                        } catch (GenericEntityException e) {
                            Debug.logError(e, "Cannot read order item entity : " + orderItem, module);
                            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                                    "OrderCannotReadTheOrderItemEntity",locale));
                        }
                    }

                    GenericValue shippingAddress = orh.getShippingAddress(shipGroupSeqId);
                    // no shipping address, try the billing address
                    if (shippingAddress == null) {
                        List<GenericValue> billingAddressList = orh.getBillingLocations();
                        if (billingAddressList.size() > 0) {
                            shippingAddress = billingAddressList.get(0);
                        }
                    }

                    // TODO and NOTE DEJ20070816: this is NOT a good way to determine if this is a face-to-face or immediatelyFulfilled order
                    //this should be made consistent with the CheckOutHelper.makeTaxContext(int shipGroup, GenericValue shipAddress) method
                    if (shippingAddress == null) {
                        // face-to-face order; use the facility address
                        String facilityId = orderHeader.getString("originFacilityId");
                        if (facilityId != null) {
                            GenericValue facilityContactMech = ContactMechWorker.getFacilityContactMechByPurpose(delegator, facilityId, UtilMisc.toList("SHIP_ORIG_LOCATION", "PRIMARY_LOCATION"));
                            if (facilityContactMech != null) {
                                try {
                                    shippingAddress = EntityQuery.use(delegator).from("PostalAddress").where("contactMechId", facilityContactMech.getString("contactMechId")).queryOne();
                                } catch (GenericEntityException e) {
                                    Debug.logError(e, module);
                                }
                            }
                        }
                    }

                    // if shippingAddress is still null then don't calculate tax; it may be an situation where no tax is applicable, or the data is bad and we don't have a way to find an address to check tax for
                    if (shippingAddress == null) {
                        Debug.logWarning("Not calculating tax for Order [" + orderId + "] because there is no shippingAddress, and no address on the origin facility [" +  orderHeader.getString("originFacilityId") + "]", module);
                        continue;
                    }

                    // prepare the service context
                    Map<String, Object> serviceContext = UtilMisc.<String, Object>toMap("productStoreId", orh.getProductStoreId(), "itemProductList", products, "itemAmountList", amounts,
                        "itemShippingList", shipAmts, "itemPriceList", itPrices, "itemQuantityList", itQuantities, "orderShippingAmount", orderShipping);
                    serviceContext.put("shippingAddress", shippingAddress);
                    serviceContext.put("orderPromotionsAmount", orderPromotions);
                    if (orh.getBillToParty() != null) {
                        serviceContext.put("billToPartyId", orh.getBillToParty().getString("partyId"));
                    }
                    if (orh.getBillFromParty() != null) {
                        serviceContext.put("payToPartyId", orh.getBillFromParty().getString("partyId"));
                    }

                    // invoke the calcTax service
                    Map<String, Object> serviceResult = null;
                    try {
                        serviceResult = dispatcher.runSync("calcTax", serviceContext);
                    } catch (GenericServiceException e) {
                        Debug.logError(e, module);
                        return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                                "OrderProblemOccurredInTaxService",locale));
                    }

                    if (ServiceUtil.isError(serviceResult)) {
                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                    }

                    // the adjustments (returned in order) from the tax service
                    List<GenericValue> orderAdj = UtilGenerics.checkList(serviceResult.get("orderAdjustments"));
                    List<List<GenericValue>> itemAdj = UtilGenerics.checkList(serviceResult.get("itemAdjustments"));

                    // Accumulate the new tax total from the recalculated header adjustments
                    if (UtilValidate.isNotEmpty(orderAdj)) {
                        for (GenericValue oa : orderAdj) {
                            if (oa.get("amount") != null) {
                                totalNewOrderTax = totalNewOrderTax.add(oa.getBigDecimal("amount").setScale(taxDecimals, taxRounding));
                            }
                        }
                    }

                    // Accumulate the new tax total from the recalculated item adjustments
                    if (UtilValidate.isNotEmpty(itemAdj)) {
                        for (int i = 0; i < itemAdj.size(); i++) {
                            List<GenericValue> itemAdjustments = itemAdj.get(i);
                            for (GenericValue ia : itemAdjustments) {
                                if (ia.get("amount") != null) {
                                    totalNewOrderTax = totalNewOrderTax.add(ia.getBigDecimal("amount").setScale(taxDecimals, taxRounding));
                                }
                            }
                        }
                    }
                }
            }

            // If there is any manually added tax then add it into new system generated tax.
            if (totalManuallyAddedOrderTax.compareTo(BigDecimal.ZERO) > 0) {
                totalNewOrderTax = totalNewOrderTax.add(totalManuallyAddedOrderTax).setScale(taxDecimals, taxRounding);
            }

            // Determine the difference between existing and new tax adjustment totals, if any
            BigDecimal orderTaxDifference = totalNewOrderTax.subtract(totalExistingOrderTax).setScale(taxDecimals, taxRounding);

            // If the total has changed, create an OrderAdjustment to reflect the fact
            if (orderTaxDifference.signum() != 0) {
                Map<String, Object> createOrderAdjContext = new HashMap<>();
                createOrderAdjContext.put("orderAdjustmentTypeId", "SALES_TAX");
                createOrderAdjContext.put("orderId", orderId);
                if (UtilValidate.isNotEmpty(orderItemSeqId)) {
                    createOrderAdjContext.put("orderItemSeqId", orderItemSeqId);
                } else {
                    createOrderAdjContext.put("orderItemSeqId", "_NA_");
                }
                createOrderAdjContext.put("shipGroupSeqId", "_NA_");
                createOrderAdjContext.put("description", "Tax adjustment due to order change");
                createOrderAdjContext.put("amount", orderTaxDifference);
                createOrderAdjContext.put("userLogin", userLogin);
                Map<String, Object> createOrderAdjResponse = null;
                try {
                    createOrderAdjResponse = dispatcher.runSync("createOrderAdjustment", createOrderAdjContext);
                } catch (GenericServiceException e) {
                    String createOrderAdjErrMsg = UtilProperties.getMessage(resource_error,
                            "OrderErrorCallingCreateOrderAdjustmentService", locale);
                    Debug.logError(createOrderAdjErrMsg, module);
                    return ServiceUtil.returnError(createOrderAdjErrMsg);
                }
                if (ServiceUtil.isError(createOrderAdjResponse)) {
                    Debug.logError(ServiceUtil.getErrorMessage(createOrderAdjResponse), module);
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(createOrderAdjResponse));
                }
            }
        }

        return ServiceUtil.returnSuccess();
    }

    /** Service for checking and re-calc the shipping amount */
    public static Map<String, Object> recalcOrderShipping(DispatchContext ctx, Map<String, ? extends Object> context) {
        LocalDispatcher dispatcher = ctx.getDispatcher();
        Delegator delegator = ctx.getDelegator();
        String orderId = (String) context.get("orderId");
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");

        // check and make sure we have permission to change the order
        Security security = ctx.getSecurity();
        boolean hasPermission = OrderServices.hasPermission(orderId, userLogin, "UPDATE", security, delegator);
        if (!hasPermission) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                    "OrderYouDoNotHavePermissionToChangeThisOrdersStatus",locale));
        }

        // get the order header
        GenericValue orderHeader = null;
        try {
            orderHeader = EntityQuery.use(delegator).from("OrderHeader").where("orderId", orderId).queryOne();
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                    "OrderErrorCannotGetOrderHeaderEntity",locale) + e.getMessage());
        }

        if (orderHeader == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                    "OrderErrorNoValidOrderHeaderFoundForOrderId", UtilMisc.toMap("orderId",orderId), locale));
        }

        OrderReadHelper orh = new OrderReadHelper(orderHeader);
        List<GenericValue> shipGroups = orh.getOrderItemShipGroups();
        if (shipGroups != null) {
            for (GenericValue shipGroup : shipGroups) {
                String shipGroupSeqId = shipGroup.getString("shipGroupSeqId");

                if (shipGroup.get("contactMechId") == null || shipGroup.get("shipmentMethodTypeId") == null) {
                    // not shipped (face-to-face order)
                    continue;
                }

                Map<String, Object> shippingEstMap = ShippingEvents.getShipEstimate(dispatcher, delegator, orh, shipGroupSeqId);
                BigDecimal shippingTotal = null;
                if (UtilValidate.isEmpty(orh.getValidOrderItems(shipGroupSeqId))) {
                    shippingTotal = ZERO;
                    Debug.logInfo("No valid order items found - " + shippingTotal, module);
                } else {
                    shippingTotal = UtilValidate.isEmpty(shippingEstMap.get("shippingTotal")) ? ZERO : (BigDecimal)shippingEstMap.get("shippingTotal");
                    shippingTotal = shippingTotal.setScale(orderDecimals, orderRounding);
                    Debug.logInfo("Got new shipping estimate - " + shippingTotal, module);
                }
                if (Debug.infoOn()) {
                    Debug.logInfo("New Shipping Total [" + orderId + " / " + shipGroupSeqId + "] : " + shippingTotal, module);
                }

                BigDecimal currentShipping = OrderReadHelper.getAllOrderItemsAdjustmentsTotal(orh.getOrderItemAndShipGroupAssoc(shipGroupSeqId), orh.getAdjustments(), false, false, true);
                currentShipping = currentShipping.add(OrderReadHelper.calcOrderAdjustments(orh.getOrderHeaderAdjustments(shipGroupSeqId), orh.getOrderItemsSubTotal(), false, false, true));

                if (Debug.infoOn()) {
                    Debug.logInfo("Old Shipping Total [" + orderId + " / " + shipGroupSeqId + "] : " + currentShipping, module);
                }

                List<String> errorMessageList = UtilGenerics.checkList(shippingEstMap.get(ModelService.ERROR_MESSAGE_LIST));
                if (errorMessageList != null) {
                    Debug.logWarning("Problem finding shipping estimates for [" + orderId + "/ " + shipGroupSeqId + "] = " + errorMessageList, module);
                    continue;
                }

                if ((shippingTotal != null) && (shippingTotal.compareTo(currentShipping) != 0)) {
                    // place the difference as a new shipping adjustment
                    BigDecimal adjustmentAmount = shippingTotal.subtract(currentShipping);
                    String adjSeqId = delegator.getNextSeqId("OrderAdjustment");
                    GenericValue orderAdjustment = delegator.makeValue("OrderAdjustment", UtilMisc.toMap("orderAdjustmentId", adjSeqId));
                    orderAdjustment.set("orderAdjustmentTypeId", "SHIPPING_CHARGES");
                    orderAdjustment.set("amount", adjustmentAmount);
                    orderAdjustment.set("orderId", orh.getOrderId());
                    orderAdjustment.set("shipGroupSeqId", shipGroupSeqId);
                    orderAdjustment.set("orderItemSeqId", DataModelConstants.SEQ_ID_NA);
                    orderAdjustment.set("createdDate", UtilDateTime.nowTimestamp());
                    orderAdjustment.set("createdByUserLogin", userLogin.getString("userLoginId"));
                    try {
                        orderAdjustment.create();
                    } catch (GenericEntityException e) {
                        Debug.logError(e, "Problem creating shipping re-calc adjustment : " + orderAdjustment, module);
                        return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                                "OrderErrorCannotCreateAdjustment",locale));
                    }
                }

                // TODO: re-balance free shipping adjustment
            }
        }

        return ServiceUtil.returnSuccess();

    }

    /** Service for checking to see if an order is fully completed or canceled */
    public static Map<String, Object> checkItemStatus(DispatchContext ctx, Map<String, ? extends Object> context) {
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        Locale locale = (Locale) context.get("locale");

        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String orderId = (String) context.get("orderId");

        // check and make sure we have permission to change the order
        Security security = ctx.getSecurity();
        boolean hasPermission = OrderServices.hasPermission(orderId, userLogin, "UPDATE", security, delegator);
        if (!hasPermission) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                    "OrderYouDoNotHavePermissionToChangeThisOrdersStatus",locale));
        }

        // get the order header
        GenericValue orderHeader = null;
        try {
            orderHeader = EntityQuery.use(delegator).from("OrderHeader").where("orderId", orderId).queryOne();
        } catch (GenericEntityException e) {
            Debug.logError(e, "Cannot get OrderHeader record", module);
        }
        if (orderHeader == null) {
            Debug.logError("OrderHeader came back as null", module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                    "OrderCannotUpdateNullOrderHeader",UtilMisc.toMap("orderId",orderId),locale));
        }

        // get the order items
        List<GenericValue> orderItems = null;
        try {
            orderItems = EntityQuery.use(delegator).from("OrderItem").where("orderId", orderId).queryList();
        } catch (GenericEntityException e) {
            Debug.logError(e, "Cannot get OrderItem records", module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                    "OrderProblemGettingOrderItemRecords", locale));
        }

        String orderHeaderStatusId = orderHeader.getString("statusId");
        String orderTypeId = orderHeader.getString("orderTypeId");

        boolean allCanceled = true;
        boolean allComplete = true;
        boolean allApproved = true;
        if (orderItems != null) {
            for (GenericValue item : orderItems) {
                String statusId = item.getString("statusId");
                if (!"ITEM_CANCELLED".equals(statusId)) {
                    allCanceled = false;
                    if (!"ITEM_COMPLETED".equals(statusId)) {
                        allComplete = false;
                        if (!"ITEM_APPROVED".equals(statusId)) {
                            allApproved = false;
                            break;
                        }
                    }
                }
            }

            // find the next status to set to (if any)
            String newStatus = null;
            if (allCanceled) {
                newStatus = "ORDER_CANCELLED";
            } else if (allComplete) {
                newStatus = "ORDER_COMPLETED";
            } else if (allApproved) {
                boolean changeToApprove = true;

                // NOTE DEJ20070805 I'm not sure why we would want to auto-approve the header... adding at least this one exeption so that we don't have to add processing, held, etc statuses to the item status list
                // NOTE2 related to the above: appears this was a weird way to set the order header status by setting all order item statuses... changing that to be less weird and more direct
                // this is a bit of a pain: if the current statusId = ProductStore.headerApprovedStatus and we don't have that status in the history then we don't want to change it on approving the items
                if (UtilValidate.isNotEmpty(orderHeader.getString("productStoreId"))) {
                    try {
                        GenericValue productStore = EntityQuery.use(delegator).from("ProductStore").where("productStoreId", orderHeader.getString("productStoreId")).queryOne();
                        if (productStore != null) {
                            String headerApprovedStatus = productStore.getString("headerApprovedStatus");
                            if (UtilValidate.isNotEmpty(headerApprovedStatus)) {
                                if (headerApprovedStatus.equals(orderHeaderStatusId)) {
                                    List<GenericValue> orderStatusList = EntityQuery.use(delegator).from("OrderStatus").where("orderId", orderId, "statusId", headerApprovedStatus, "orderItemSeqId", null).queryList();
                                    // should be 1 in the history, but just in case accept 0 too
                                    if (orderStatusList.size() <= 1) {
                                        changeToApprove = false;
                                    }
                                }
                            }
                        }
                    } catch (GenericEntityException e) {
                         String errMsg = UtilProperties.getMessage(resource_error, "OrderDatabaseErrorCheckingIfWeShouldChangeOrderHeaderStatusToApproved", UtilMisc.toMap("errorString", e.toString()), locale);
                        Debug.logError(e, errMsg, module);
                        return ServiceUtil.returnError(errMsg);
                    }
                }

                if ("ORDER_SENT".equals(orderHeaderStatusId)) {
                    changeToApprove = false;
                }
                if ("ORDER_COMPLETED".equals(orderHeaderStatusId)) {
                    if ("SALES_ORDER".equals(orderTypeId)) {
                        changeToApprove = false;
                    }
                }
                if ("ORDER_CANCELLED".equals(orderHeaderStatusId)) {
                    changeToApprove = false;
                }

                if (changeToApprove) {
                    newStatus = "ORDER_APPROVED";
                    if ("ORDER_HOLD".equals(orderHeaderStatusId)) {
                        // Don't let the system to auto approve order if the order was put on hold.
                        return ServiceUtil.returnSuccess();
                    }
                }
            }

            // now set the new order status
            if (newStatus != null && !newStatus.equals(orderHeaderStatusId)) {
                Map<String, Object> serviceContext = UtilMisc.<String, Object>toMap("orderId", orderId, "statusId", newStatus, "userLogin", userLogin);
                Map<String, Object> newSttsResult = null;
                try {
                    newSttsResult = dispatcher.runSync("changeOrderStatus", serviceContext);
                } catch (GenericServiceException e) {
                    Debug.logError(e, "Problem calling the changeOrderStatus service", module);
                }
                if (ServiceUtil.isError(newSttsResult)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(newSttsResult));
                }
            }
        } else {
            Debug.logWarning(UtilProperties.getMessage(resource_error,
                    "OrderReceivedNullForOrderItemRecordsOrderId", UtilMisc.toMap("orderId",orderId),locale), module);
        }

        return ServiceUtil.returnSuccess();
    }

    /** Service to cancel an order item quantity */
    public static Map<String, Object> cancelOrderItem(DispatchContext ctx, Map<String, ? extends Object> context) {
        LocalDispatcher dispatcher = ctx.getDispatcher();
        Delegator delegator = ctx.getDelegator();
        Locale locale = (Locale) context.get("locale");
        Map<String, Object> resp = new HashMap<>();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        BigDecimal cancelQuantity = (BigDecimal) context.get("cancelQuantity");
        String orderId = (String) context.get("orderId");
        String orderItemSeqId = (String) context.get("orderItemSeqId");
        String shipGroupSeqId = (String) context.get("shipGroupSeqId");
        Map<String, String> itemReasonMap = UtilGenerics.cast(context.get("itemReasonMap"));
        Map<String, String> itemCommentMap = UtilGenerics.cast(context.get("itemCommentMap"));
        Map<String, String> itemQuantityMap = UtilGenerics.cast(context.get("itemQtyMap"));
        if ((cancelQuantity == null) && UtilValidate.isNotEmpty(itemQuantityMap)) {
            String key = orderItemSeqId+":"+shipGroupSeqId;
            if (UtilValidate.isNotEmpty(itemQuantityMap.get(key))) {
                cancelQuantity = new BigDecimal(itemQuantityMap.get(key));
            }

        }
        // debugging message info
        String itemMsgInfo = orderId + " / " + orderItemSeqId + " / " + shipGroupSeqId;

        // check and make sure we have permission to change the order
        Security security = ctx.getSecurity();

        boolean hasPermission = OrderServices.hasPermission(orderId, userLogin, "UPDATE", security, delegator);
        if (!hasPermission) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                    "OrderYouDoNotHavePermissionToChangeThisOrdersStatus",locale));
        }

        Map<String, String> fields = UtilMisc.<String, String>toMap("orderId", orderId);
        if (orderItemSeqId != null) {
            fields.put("orderItemSeqId", orderItemSeqId);
        }
        if (shipGroupSeqId != null) {
            fields.put("shipGroupSeqId", shipGroupSeqId);
        }

        OrderReadHelper orh = new OrderReadHelper(delegator, orderId);
        List<GenericValue> orderItemShipGroupAssocs = null;
        try {
            orderItemShipGroupAssocs = EntityQuery.use(delegator).from("OrderItemShipGroupAssoc").where(fields).queryList();
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                    "OrderErrorCannotGetOrderItemAssocEntity", UtilMisc.toMap("itemMsgInfo",itemMsgInfo), locale));
        }

        if (orderItemShipGroupAssocs != null) {
            for (GenericValue orderItemShipGroupAssoc : orderItemShipGroupAssocs) {
                GenericValue orderItem = null;
                String itemStatus = "ITEM_CANCELLED";
                try {
                    orderItem = orderItemShipGroupAssoc.getRelatedOne("OrderItem", false);
                } catch (GenericEntityException e) {
                    Debug.logError(e, module);
                }

                if (orderItem == null) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                            "OrderErrorCannotCancelItemItemNotFound", UtilMisc.toMap("itemMsgInfo",itemMsgInfo), locale));
                }

                BigDecimal aisgaCancelQuantity =  orderItemShipGroupAssoc.getBigDecimal("cancelQuantity");
                if (aisgaCancelQuantity == null) {
                    aisgaCancelQuantity = BigDecimal.ZERO;
                }
                BigDecimal availableQuantity = orderItemShipGroupAssoc.getBigDecimal("quantity").subtract(aisgaCancelQuantity);

                BigDecimal itemCancelQuantity = orderItem.getBigDecimal("cancelQuantity");
                if (itemCancelQuantity == null) {
                    itemCancelQuantity = BigDecimal.ZERO;
                }
                BigDecimal itemQuantity = orderItem.getBigDecimal("quantity").subtract(itemCancelQuantity);
                if (availableQuantity == null) {
                    availableQuantity = BigDecimal.ZERO;
                }
                if (itemQuantity == null) {
                    itemQuantity = BigDecimal.ZERO;
                }

                if ("PURCHASE_ORDER".equals(orh.getOrderTypeId())) {
                    BigDecimal receivedQty = orh.getItemReceivedQuantity(orderItem);
                    if (receivedQty.compareTo(BigDecimal.ZERO) > 0) {
                        itemStatus = "ITEM_COMPLETED";
                    }
                    itemQuantity = itemQuantity.subtract(receivedQty);
                } else {
                    BigDecimal shippedQty = orh.getItemShippedQuantity(orderItem);
                    if (shippedQty.compareTo(BigDecimal.ZERO) > 0 ) {
                        itemStatus = "ITEM_COMPLETED";
                    }
                    itemQuantity = itemQuantity.subtract(shippedQty);
                }

                BigDecimal thisCancelQty = null;
                if (cancelQuantity != null) {
                    thisCancelQty = cancelQuantity;
                } else {
                    thisCancelQty = itemQuantity;
                }

                if (availableQuantity.compareTo(thisCancelQty) >= 0) {
                    if (availableQuantity.compareTo(BigDecimal.ZERO) == 0) {
                        continue;  //OrderItemShipGroupAssoc already cancelled
                    }
                    orderItem.set("cancelQuantity", itemCancelQuantity.add(thisCancelQty));
                    orderItemShipGroupAssoc.set("cancelQuantity", aisgaCancelQuantity.add(thisCancelQty));

                    try {
                        List<GenericValue> toStore = UtilMisc.toList(orderItem, orderItemShipGroupAssoc);
                        delegator.storeAll(toStore);
                    } catch (GenericEntityException e) {
                        Debug.logError(e, module);
                        return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                                "OrderUnableToSetCancelQuantity", UtilMisc.toMap("itemMsgInfo",itemMsgInfo), locale));
                    }

                    Map<String, Object> localCtx = UtilMisc.toMap("userLogin", userLogin,
                            "orderId", orderItem.getString("orderId"),
                            "orderItemSeqId", orderItem.getString("orderItemSeqId"),
                            "shipGroupSeqId", orderItemShipGroupAssoc.getString("shipGroupSeqId"));
                    if (availableQuantity.compareTo(thisCancelQty) == 0) {
                        try {
                            resp= dispatcher.runSync("deleteOrderItemShipGroupAssoc", localCtx);
                            if (ServiceUtil.isError(resp)) {
                                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(resp));
                            }
                        } catch (GenericServiceException e) {
                            Debug.logError(e, module);
                            return ServiceUtil.returnError(e.getMessage());
                        }
                    }


                    //  create order item change record
                    if (!"Y".equals(orderItem.getString("isPromo"))) {
                        String reasonEnumId = null;
                        String changeComments = null;
                        if (UtilValidate.isNotEmpty(itemReasonMap)) {
                            reasonEnumId = itemReasonMap.get(orderItem.getString("orderItemSeqId"));
                        }
                        if (UtilValidate.isNotEmpty(itemCommentMap)) {
                            changeComments = itemCommentMap.get(orderItem.getString("orderItemSeqId"));
                        }

                        Map<String, Object> serviceCtx = new HashMap<>();
                        serviceCtx.put("orderId", orderItem.getString("orderId"));
                        serviceCtx.put("orderItemSeqId", orderItem.getString("orderItemSeqId"));
                        serviceCtx.put("cancelQuantity", thisCancelQty);
                        serviceCtx.put("changeTypeEnumId", "ODR_ITM_CANCEL");
                        serviceCtx.put("reasonEnumId", reasonEnumId);
                        serviceCtx.put("changeComments", changeComments);
                        serviceCtx.put("userLogin", userLogin);
                        try {
                            resp = dispatcher.runSync("createOrderItemChange", serviceCtx);
                            if (ServiceUtil.isError(resp)) {
                                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(resp));
                            }
                        } catch (GenericServiceException e) {
                            Debug.logError(e, module);
                            return ServiceUtil.returnError(e.getMessage());
                        }
                    }

                    // log an order note
                    try {
                        BigDecimal quantity = thisCancelQty.setScale(1, orderRounding);
                        String cancelledItemToOrder = UtilProperties.getMessage(resource, "OrderCancelledItemToOrder", locale);
                        resp = dispatcher.runSync("createOrderNote", UtilMisc.<String, Object>toMap("orderId", orderId, "note", cancelledItemToOrder +
                                orderItem.getString("productId") + " (" + quantity + ")", "internalNote", "Y", "userLogin", userLogin));
                        if (ServiceUtil.isError(resp)) {
                            return ServiceUtil.returnError(ServiceUtil.getErrorMessage(resp));
                        }
                    } catch (GenericServiceException e) {
                        Debug.logError(e, module);
                    }

                    if (thisCancelQty.compareTo(itemQuantity) >= 0) {
                        if ("ITEM_COMPLETED".equals(itemStatus) && "SALES_ORDER".equals(orh.getOrderTypeId())) {
                            //If partial item shipped then release remaining inventory of SO item and marked SO item as completed.
                            Map<String, Object> cancelOrderItemInvResCtx = UtilMisc.toMap("orderId", orderId, "orderItemSeqId", orderItem.getString("orderItemSeqId"), "shipGroupSeqId",
                                    shipGroupSeqId, "cancelQuantity", thisCancelQty, "userLogin", userLogin);
                            try {
                                dispatcher.runSyncIgnore("cancelOrderItemInvResQty", cancelOrderItemInvResCtx);
                            } catch (GenericServiceException e) {
                                Debug.logError(e, module);
                                return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderUnableToUpdateInventoryReservations", UtilMisc.toMap("itemMsgInfo",itemMsgInfo), locale));
                            }
                        }
                        // all items are cancelled -- mark the item as cancelled
                        Map<String, Object> statusCtx = UtilMisc.<String, Object>toMap("orderId", orderId, "orderItemSeqId", orderItem.getString("orderItemSeqId"), "statusId", itemStatus, "userLogin", userLogin);
                        try {
                            dispatcher.runSyncIgnore("changeOrderItemStatus", statusCtx);
                        } catch (GenericServiceException e) {
                            Debug.logError(e, module);
                            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                                    "OrderUnableToCancelOrderLine", UtilMisc.toMap("itemMsgInfo",itemMsgInfo), locale));
                        }
                    } else {
                        // reverse the inventory reservation
                        Map<String, Object> invCtx = UtilMisc.<String, Object>toMap("orderId", orderId, "orderItemSeqId", orderItem.getString("orderItemSeqId"), "shipGroupSeqId",
                                shipGroupSeqId, "cancelQuantity", thisCancelQty, "userLogin", userLogin);
                        try {
                            dispatcher.runSyncIgnore("cancelOrderItemInvResQty", invCtx);
                        } catch (GenericServiceException e) {
                            Debug.logError(e, module);
                            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                                    "OrderUnableToUpdateInventoryReservations", UtilMisc.toMap("itemMsgInfo",itemMsgInfo), locale));
                        }
                    }
                } else {
                    return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                            "OrderInvalidCancelQuantityCannotCancel", UtilMisc.toMap("thisCancelQty",thisCancelQty), locale));
                }
            }
        } else {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                    "OrderErrorCannotCancelItemItemNotFound", UtilMisc.toMap("itemMsgInfo",itemMsgInfo), locale));
        }

        return ServiceUtil.returnSuccess();
    }

    /** Service for changing the status on order item(s) */
    public static Map<String, Object> setItemStatus(DispatchContext ctx, Map<String, ? extends Object> context) {
        Delegator delegator = ctx.getDelegator();

        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String orderId = (String) context.get("orderId");
        String orderItemSeqId = (String) context.get("orderItemSeqId");
        String fromStatusId = (String) context.get("fromStatusId");
        String statusId = (String) context.get("statusId");
        Timestamp statusDateTime = (Timestamp) context.get("statusDateTime");
        Locale locale = (Locale) context.get("locale");

        // check and make sure we have permission to change the order
        Security security = ctx.getSecurity();
        boolean hasPermission = OrderServices.hasPermission(orderId, userLogin, "UPDATE", security, delegator);
        if (!hasPermission) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                    "OrderYouDoNotHavePermissionToChangeThisOrdersStatus",locale));
        }

        List<EntityExpr> exprs = new ArrayList<>();
        exprs.add(EntityCondition.makeCondition("orderId", orderId));
        if (orderItemSeqId != null) {
            exprs.add(EntityCondition.makeCondition("orderItemSeqId", orderItemSeqId));
        } if (fromStatusId != null) {
            exprs.add(EntityCondition.makeCondition("statusId", fromStatusId));
        } else {
            exprs.add(EntityCondition.makeCondition("statusId", EntityOperator.NOT_IN, UtilMisc.toList("ITEM_COMPLETED", "ITEM_CANCELLED")));
        }

        List<GenericValue> orderItems = null;
        try {
            orderItems = EntityQuery.use(delegator).from("OrderItem").where(exprs).queryList();
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                    "OrderErrorCannotGetOrderItemEntity",locale) + e.getMessage());
        }

        if (UtilValidate.isNotEmpty(orderItems)) {
            List<GenericValue> toBeStored = new ArrayList<>();
            for (GenericValue orderItem : orderItems) {
                if (orderItem == null) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                            "OrderErrorCannotChangeItemStatusItemNotFound", locale));
                }
                if (Debug.verboseOn()) {
                    Debug.logVerbose("[OrderServices.setItemStatus] : Status Change: [" + orderId + "] (" + orderItem.getString("orderItemSeqId"), module);
                }
                if (Debug.verboseOn()) {
                    Debug.logVerbose("[OrderServices.setItemStatus] : From Status : " + orderItem.getString("statusId"), module);
                }
                if (Debug.verboseOn()) {
                    Debug.logVerbose("[OrderServices.setOrderStatus] : To Status : " + statusId, module);
                }

                if (orderItem.getString("statusId").equals(statusId)) {
                    continue;
                }

                try {
                    GenericValue statusChange = EntityQuery.use(delegator).from("StatusValidChange").where("statusId", orderItem.getString("statusId"), "statusIdTo", statusId).queryOne();

                    if (statusChange == null) {
                        Debug.logWarning(UtilProperties.getMessage(resource_error,
                                "OrderItemStatusNotChangedIsNotAValidChange", UtilMisc.toMap("orderStatusId",orderItem.getString("statusId"),"statusId",statusId), locale), module);
                        continue;
                    }
                } catch (GenericEntityException e) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                            "OrderErrorCouldNotChangeItemStatus",locale) + e.getMessage());
                }

                orderItem.set("statusId", statusId);
                toBeStored.add(orderItem);
                if (statusDateTime == null) {
                    statusDateTime = UtilDateTime.nowTimestamp();
                }
                // now create a status change
                Map<String, Object> changeFields = new HashMap<>();
                changeFields.put("orderStatusId", delegator.getNextSeqId("OrderStatus"));
                changeFields.put("statusId", statusId);
                changeFields.put("orderId", orderId);
                changeFields.put("orderItemSeqId", orderItem.getString("orderItemSeqId"));
                changeFields.put("statusDatetime", statusDateTime);
                changeFields.put("statusUserLogin", userLogin.getString("userLoginId"));
                GenericValue orderStatus = delegator.makeValue("OrderStatus", changeFields);
                toBeStored.add(orderStatus);
            }

            // store the changes
            if (toBeStored.size() > 0) {
                try {
                    delegator.storeAll(toBeStored);
                } catch (GenericEntityException e) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                            "OrderErrorCannotStoreStatusChanges", locale) + e.getMessage());
                }
            }

        }

        return ServiceUtil.returnSuccess();
    }

    /** Service for changing the status on an order header */
    public static Map<String, Object> setOrderStatus(DispatchContext ctx, Map<String, ? extends Object> context) {
        LocalDispatcher dispatcher = ctx.getDispatcher();
        Delegator delegator = ctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String orderId = (String) context.get("orderId");
        String statusId = (String) context.get("statusId");
        String changeReason = (String) context.get("changeReason");
        Map<String, Object> successResult = ServiceUtil.returnSuccess();
        Locale locale = (Locale) context.get("locale");

        // check and make sure we have permission to change the order
        Security security = ctx.getSecurity();
        boolean hasPermission = OrderServices.hasPermission(orderId, userLogin, "UPDATE", security, delegator);
        if (!hasPermission) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                    "OrderYouDoNotHavePermissionToChangeThisOrdersStatus",locale));
        }

        try {
            GenericValue orderHeader = EntityQuery.use(delegator).from("OrderHeader").where("orderId", orderId).queryOne();

            if (orderHeader == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                        "OrderErrorCouldNotChangeOrderStatusOrderCannotBeFound", locale));
            }
            // first save off the old status
            successResult.put("oldStatusId", orderHeader.get("statusId"));
            successResult.put("orderTypeId", orderHeader.get("orderTypeId"));

            if (Debug.verboseOn()) {
                Debug.logVerbose("[OrderServices.setOrderStatus] : From Status : " + orderHeader.getString("statusId"), module);
            }
            if (Debug.verboseOn()) {
                Debug.logVerbose("[OrderServices.setOrderStatus] : To Status : " + statusId, module);
            }

            if (orderHeader.getString("statusId").equals(statusId)) {
                Debug.logWarning(UtilProperties.getMessage(resource_error,
                        "OrderTriedToSetOrderStatusWithTheSameStatusIdforOrderWithId", UtilMisc.toMap("statusId",statusId,"orderId",orderId),locale),module);
                return successResult;
            }
            try {
                GenericValue statusChange = EntityQuery.use(delegator).from("StatusValidChange").where("statusId", orderHeader.getString("statusId"), "statusIdTo", statusId).cache(true).queryOne();
                if (statusChange == null) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                            "OrderErrorCouldNotChangeOrderStatusStatusIsNotAValidChange", locale) + ": [" + orderHeader.getString("statusId") + "] -> [" + statusId + "]");
                }
            } catch (GenericEntityException e) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                        "OrderErrorCouldNotChangeOrderStatus",locale) + e.getMessage() + ").");
            }

            // update the current status
            orderHeader.set("statusId", statusId);

            // now create a status change
            GenericValue orderStatus = delegator.makeValue("OrderStatus");
            orderStatus.put("orderStatusId", delegator.getNextSeqId("OrderStatus"));
            orderStatus.put("statusId", statusId);
            orderStatus.put("orderId", orderId);
            orderStatus.put("statusDatetime", UtilDateTime.nowTimestamp());
            orderStatus.put("statusUserLogin", userLogin.getString("userLoginId"));
            orderStatus.put("changeReason", changeReason);

            orderHeader.store();
            orderStatus.create();

            successResult.put("needsInventoryIssuance", orderHeader.get("needsInventoryIssuance"));
            successResult.put("grandTotal", orderHeader.get("grandTotal"));
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                    "OrderErrorCouldNotChangeOrderStatus",locale) + e.getMessage() + ").");
        }

        if ("Y".equals(context.get("setItemStatus"))) {
            String newItemStatusId = null;
            if ("ORDER_APPROVED".equals(statusId)) {
                newItemStatusId = "ITEM_APPROVED";
            } else if ("ORDER_COMPLETED".equals(statusId)) {
                newItemStatusId = "ITEM_COMPLETED";
            } else if ("ORDER_CANCELLED".equals(statusId)) {
                newItemStatusId = "ITEM_CANCELLED";
            }

            if (newItemStatusId != null) {
                try {
                    Map<String, Object> resp = dispatcher.runSync("changeOrderItemStatus", UtilMisc.<String, Object>toMap("orderId", orderId, "statusId", newItemStatusId, "userLogin", userLogin));
                    if (ServiceUtil.isError(resp)) {
                        return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                                "OrderErrorCouldNotChangeItemStatus", locale) + newItemStatusId, null, null, resp);
                    }
                } catch (GenericServiceException e) {
                    Debug.logError(e, "Error changing item status to " + newItemStatusId + ": " + e.toString(), module);
                    return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                            "OrderErrorCouldNotChangeItemStatus", locale) + newItemStatusId + ": " + e.toString());
                }
            }
        }

        successResult.put("orderStatusId", statusId);
        return successResult;
    }

    /** Service to update the order tracking number */
    public static Map<String, Object> updateTrackingNumber(DispatchContext dctx, Map<String, ? extends Object> context) {
        Map<String, Object> result = new HashMap<>();
        Delegator delegator = dctx.getDelegator();
        String orderId = (String) context.get("orderId");
        String shipGroupSeqId = (String) context.get("shipGroupSeqId");
        String trackingNumber = (String) context.get("trackingNumber");

        try {
            GenericValue shipGroup = EntityQuery.use(delegator).from("OrderItemShipGroup").where("orderId", orderId, "shipGroupSeqId", shipGroupSeqId).queryOne();

            if (shipGroup == null) {
                result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_ERROR);
                result.put(ModelService.ERROR_MESSAGE, "ERROR: No order shipment preference found!");
            } else {
                shipGroup.set("trackingNumber", trackingNumber);
                shipGroup.store();
                result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_ERROR);
            result.put(ModelService.ERROR_MESSAGE, "ERROR: Could not set tracking number (" + e.getMessage() + ").");
        }
        return result;
    }

    /** Service to add a role type to an order */
    public static Map<String, Object> addRoleType(DispatchContext ctx, Map<String, ? extends Object> context) {
        Map<String, Object> result = new HashMap<>();
        Delegator delegator = ctx.getDelegator();
        String orderId = (String) context.get("orderId");
        String partyId = (String) context.get("partyId");
        String roleTypeId = (String) context.get("roleTypeId");
        Boolean removeOld = (Boolean) context.get("removeOld");

        if (removeOld != null && removeOld) {
            try {
                delegator.removeByAnd("OrderRole", UtilMisc.toMap("orderId", orderId, "roleTypeId", roleTypeId));
            } catch (GenericEntityException e) {
                result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_ERROR);
                result.put(ModelService.ERROR_MESSAGE, "ERROR: Could not remove old roles (" + e.getMessage() + ").");
                return result;
            }
        }

        Map<String, String> fields = UtilMisc.<String, String>toMap("orderId", orderId, "partyId", partyId, "roleTypeId", roleTypeId);

        try {
            // first check and see if we are already there; if so, just return success
            GenericValue testValue = EntityQuery.use(delegator).from("OrderRole").where(fields).queryOne();
            if (testValue != null) {
                ServiceUtil.returnSuccess();
            } else {
                GenericValue value = delegator.makeValue("OrderRole", fields);
                delegator.create(value);
            }
        } catch (GenericEntityException e) {
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_ERROR);
            result.put(ModelService.ERROR_MESSAGE, "ERROR: Could not add role to order (" + e.getMessage() + ").");
            return result;
        }
        result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
        return result;
    }

    /** Service to remove a role type from an order */
    public static Map<String, Object> removeRoleType(DispatchContext ctx, Map<String, ? extends Object> context) {
        Map<String, Object> result = new HashMap<>();
        Delegator delegator = ctx.getDelegator();
        String orderId = (String) context.get("orderId");
        String partyId = (String) context.get("partyId");
        String roleTypeId = (String) context.get("roleTypeId");
        GenericValue testValue = null;

        try {
            testValue = EntityQuery.use(delegator).from("OrderRole").where("orderId", orderId, "partyId", partyId, "roleTypeId", roleTypeId).queryOne();
        } catch (GenericEntityException e) {
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_ERROR);
            result.put(ModelService.ERROR_MESSAGE, "ERROR: Could not add role to order (" + e.getMessage() + ").");
            return result;
        }

        if (testValue == null) {
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
            return result;
        }

        try {
            GenericValue value = EntityQuery.use(delegator).from("OrderRole").where("orderId", orderId, "partyId", partyId, "roleTypeId", roleTypeId).queryOne();

            value.remove();
        } catch (GenericEntityException e) {
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_ERROR);
            result.put(ModelService.ERROR_MESSAGE, "ERROR: Could not remove role from order (" + e.getMessage() + ").");
            return result;
        }
        result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
        return result;
    }

    /** Service to email a customer with initial order confirmation */
    public static Map<String, Object> sendOrderConfirmNotification(DispatchContext ctx, Map<String, ? extends Object> context) {
        return sendOrderNotificationScreen(ctx, context, "PRDS_ODR_CONFIRM");
    }

    /** Service to email a customer with order changes */
    public static Map<String, Object> sendOrderCompleteNotification(DispatchContext ctx, Map<String, ? extends Object> context) {
        return sendOrderNotificationScreen(ctx, context, "PRDS_ODR_COMPLETE");
    }

    /** Service to email a customer with order changes */
    public static Map<String, Object> sendOrderBackorderNotification(DispatchContext ctx, Map<String, ? extends Object> context) {
        return sendOrderNotificationScreen(ctx, context, "PRDS_ODR_BACKORDER");
    }

    /** Service to email a customer with order changes */
    public static Map<String, Object> sendOrderChangeNotification(DispatchContext ctx, Map<String, ? extends Object> context) {
        return sendOrderNotificationScreen(ctx, context, "PRDS_ODR_CHANGE");
    }

    /** Service to email a customer with order payment retry results */
    public static Map<String, Object> sendOrderPayRetryNotification(DispatchContext ctx, Map<String, ? extends Object> context) {
        return sendOrderNotificationScreen(ctx, context, "PRDS_ODR_PAYRETRY");
    }

    protected static Map<String, Object> sendOrderNotificationScreen(DispatchContext dctx, Map<String, ? extends Object> context, String emailType) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String orderId = (String) context.get("orderId");
        String orderItemSeqId = (String) context.get("orderItemSeqId");
        String shipGroupSeqId = (String) context.get("shipGroupSeqId");
        String sendTo = (String) context.get("sendTo");
        String sendCc = (String) context.get("sendCc");
        String sendBcc = (String) context.get("sendBcc");
        String note = (String) context.get("note");
        String screenUri = (String) context.get("screenUri");
        GenericValue temporaryAnonymousUserLogin = (GenericValue) context.get("temporaryAnonymousUserLogin");
        Locale localePar = (Locale) context.get("locale");
        if (userLogin == null) {
            // this may happen during anonymous checkout, try to the special case user
            userLogin = temporaryAnonymousUserLogin;
        }

        // prepare the order information
        Map<String, Object> sendMap = new HashMap<>();

        // get the order header and store
        GenericValue orderHeader = null;
        try {
            orderHeader = EntityQuery.use(delegator).from("OrderHeader").where("orderId", orderId).queryOne();
        } catch (GenericEntityException e) {
            Debug.logError(e, "Problem getting OrderHeader", module);
        }

        if (orderHeader == null) {
            return ServiceUtil.returnFailure(UtilProperties.getMessage(resource,
                    "OrderOrderNotFound", UtilMisc.toMap("orderId", orderId), localePar));
        }

        if (orderHeader.get("webSiteId") == null) {
            return ServiceUtil.returnFailure(UtilProperties.getMessage(resource,
                    "OrderOrderWithoutWebSite", UtilMisc.toMap("orderId", orderId), localePar));
        }

        GenericValue productStoreEmail = null;
        try {
            productStoreEmail = EntityQuery.use(delegator).from("ProductStoreEmailSetting").where("productStoreId", orderHeader.get("productStoreId"), "emailType", emailType).queryOne();
        } catch (GenericEntityException e) {
            Debug.logError(e, "Problem getting the ProductStoreEmailSetting for productStoreId=" + orderHeader.get("productStoreId") + " and emailType=" + emailType, module);
        }
        if (productStoreEmail == null) {
            return ServiceUtil.returnFailure(UtilProperties.getMessage(resourceProduct,
                    "ProductProductStoreEmailSettingsNotValid",
                    UtilMisc.toMap("productStoreId", orderHeader.get("productStoreId"),
                            "emailType", emailType), localePar));
        }

        // the override screenUri
        if (UtilValidate.isEmpty(screenUri)) {
            String bodyScreenLocation = productStoreEmail.getString("bodyScreenLocation");
            if (UtilValidate.isEmpty(bodyScreenLocation)) {
                bodyScreenLocation = ProductStoreWorker.getDefaultProductStoreEmailScreenLocation(emailType);
            }
            sendMap.put("bodyScreenUri", bodyScreenLocation);

            String xslfoAttachScreenLocation = productStoreEmail.getString("xslfoAttachScreenLocation");
            sendMap.put("xslfoAttachScreenLocation", xslfoAttachScreenLocation);
            // add attachmentName param to get an attachment namend "[oderId].pdf" instead of default "Details.pdf"
            sendMap.put("attachmentName", (UtilValidate.isNotEmpty(shipGroupSeqId) ? orderId + "-" + StringUtils.stripStart(shipGroupSeqId, "0") : orderId) + ".pdf");
            sendMap.put("attachmentType", MimeConstants.MIME_PDF);
        } else {
            sendMap.put("bodyScreenUri", screenUri);
        }

        if (context.containsKey("xslfoAttachScreenLocationList")) {
            sendMap.put("xslfoAttachScreenLocationList", context.get("xslfoAttachScreenLocationList"));
            sendMap.put("attachmentNameList", context.get("attachmentNameList"));
            sendMap.put("attachmentTypeList", context.get("attachmentTypeList"));
        }

        // website
        sendMap.put("webSiteId", orderHeader.get("webSiteId"));

        OrderReadHelper orh = new OrderReadHelper(orderHeader);
        String emailString = orh.getOrderEmailString();
        if (UtilValidate.isEmpty(emailString)) {
            Debug.logInfo("Customer is not setup to receive emails; no address(s) found [" + orderId + "]", module);
            return ServiceUtil.returnFailure(UtilProperties.getMessage(resource,
                    "OrderOrderWithoutEmailAddress", UtilMisc.toMap("orderId", orderId), localePar));
        }

        // where to get the locale... from PLACING_CUSTOMER's UserLogin.lastLocale,
        // or if not available then from ProductStore.defaultLocaleString
        // or if not available then the system Locale
        Locale locale = null;
        GenericValue placingParty = orh.getPlacingParty();
        GenericValue placingUserLogin = placingParty == null ? null : PartyWorker.findPartyLatestUserLogin(placingParty.getString("partyId"), delegator);
        if (placingParty != null) {
            locale = PartyWorker.findPartyLastLocale(placingParty.getString("partyId"), delegator);
        }

        // for anonymous orders, use the temporaryAnonymousUserLogin as the placingUserLogin will be null
        if (placingUserLogin == null) {
            placingUserLogin = temporaryAnonymousUserLogin;
        }

        GenericValue productStore = OrderReadHelper.getProductStoreFromOrder(orderHeader);
        if (locale == null && productStore != null) {
            String localeString = productStore.getString("defaultLocaleString");
            if (UtilValidate.isNotEmpty(localeString)) {
                locale = UtilMisc.parseLocale(localeString);
            }
        }
        if (locale == null) {
            locale = Locale.getDefault();
        }

        Map<String, Object> bodyParameters = UtilMisc.<String, Object>toMap("orderId", orderId, "orderItemSeqId", orderItemSeqId, "userLogin", placingUserLogin, "locale", locale);
        if (placingParty!= null) {
            bodyParameters.put("partyId", placingParty.get("partyId"));
        }
        bodyParameters.put("note", note);
        bodyParameters.put("shipGroupSeqId", shipGroupSeqId);
        sendMap.put("bodyParameters", bodyParameters);
        sendMap.put("userLogin",userLogin);

        String subjectString = productStoreEmail.getString("subject");
        sendMap.put("subject", subjectString);

        sendMap.put("contentType", productStoreEmail.get("contentType"));
        sendMap.put("sendFrom", productStoreEmail.get("fromAddress"));
        sendMap.put("sendCc", productStoreEmail.get("ccAddress"));
        sendMap.put("sendBcc", productStoreEmail.get("bccAddress"));
        if ((sendTo != null) && UtilValidate.isEmail(sendTo)) {
            sendMap.put("sendTo", sendTo);
        } else {
            sendMap.put("sendTo", emailString);
        }
        if ((sendCc != null) && UtilValidate.isEmail(sendCc)) {
            sendMap.put("sendCc", sendCc);
        } else {
            sendMap.put("sendCc", productStoreEmail.get("ccAddress"));
        }

        if ((sendBcc != null) && UtilValidate.isEmailList(sendBcc)) {
            sendMap.put("sendBcc", sendBcc);
        }

        // send the notification
        Map<String, Object> sendResp = null;
        try {
            sendResp = dispatcher.runSync("sendMailFromScreen", sendMap);
            if (ServiceUtil.isError(sendResp)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(sendResp));
            }
        } catch (GenericServiceException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                    "OrderServiceExceptionSeeLogs",locale));
        }

        // check for errors
        if (sendResp != null && ServiceUtil.isSuccess(sendResp)) {
            sendResp.put("emailType", emailType);
        }
        if (UtilValidate.isNotEmpty(orderId)) {
            sendResp.put("orderId", orderId);
        }
        return sendResp;
    }

    /** Service to email order notifications for pending actions */
    public static Map<String, Object> sendProcessNotification(DispatchContext ctx, Map<String, ? extends Object> context) {
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        String adminEmailList = (String) context.get("adminEmailList");
        String assignedToUser = (String) context.get("assignedPartyId");
        String workEffortId = (String) context.get("workEffortId");
        Locale locale = (Locale) context.get("locale");

        GenericValue workEffort = null;
        GenericValue orderHeader = null;

        // get the order/workflow info
        try {
            workEffort = EntityQuery.use(delegator).from("WorkEffort").where("workEffortId", workEffortId).queryOne();
            String sourceReferenceId = workEffort.getString("sourceReferenceId");
            if (sourceReferenceId != null) {
                orderHeader = EntityQuery.use(delegator).from("OrderHeader").where("orderId", sourceReferenceId)
                        .queryOne();
            }
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                    "OrderProblemWithEntityLookup", locale));
        }

        // find the assigned user's email address(s)
        GenericValue party = null;
        Collection<GenericValue> assignedToEmails = null;
        try {
            party = EntityQuery.use(delegator).from("Party").where("partyId", assignedToUser).queryOne();
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                    "OrderProblemWithEntityLookup", locale));
        }
        if (party != null) {
            assignedToEmails = ContactHelper.getContactMechByPurpose(party, "PRIMARY_EMAIL", false);
        }

        Map<String, Object> templateData = new HashMap<>(context);
        if (orderHeader != null) {
            templateData.putAll(orderHeader);
        }
        templateData.putAll(workEffort);

        templateData.put("omgStatusId", workEffort.getString("currentStatusId"));

        // get the assignments
        List<GenericValue> assignments = null;
        try {
            assignments = workEffort.getRelated("WorkEffortPartyAssignment", null, null, false);
        } catch (GenericEntityException e1) {
            Debug.logError(e1, "Problems getting assignements", module);
        }
        templateData.put("assignments", assignments);

        StringBuilder emailList = new StringBuilder();
        if (assignedToEmails != null) {
            for (GenericValue ct : assignedToEmails) {
                if (ct != null && ct.get("infoString") != null) {
                    if (emailList.length() > 1) {
                        emailList.append(",");
                    }
                    emailList.append(ct.getString("infoString"));
                }
            }
        }
        if (adminEmailList != null) {
            if (emailList.length() > 1) {
                emailList.append(",");
            }
            emailList.append(adminEmailList);
        }

        // prepare the mail info
        String templateName = "component://order/template/email/EmailProcessNotify.ftl";

        Map<String, Object> sendMailContext = new HashMap<>();
        sendMailContext.put("sendTo", emailList.toString());
        sendMailContext.put("sendFrom", "workflow@ofbiz.org"); // fixme
        sendMailContext.put("subject", "Workflow Notification");
        sendMailContext.put("templateName", templateName);
        sendMailContext.put("templateData", templateData);

        try {
            dispatcher.runAsync("sendGenericNotificationEmail", sendMailContext);
        } catch (GenericServiceException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                    "OrderSendMailServiceFailed", locale) + e.getMessage());
        }
        return ServiceUtil.returnSuccess();
    }

    /** Service to get order header information as standard results. */
    public static Map<String, Object> getOrderHeaderInformation(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        String orderId = (String) context.get("orderId");
        Locale locale = (Locale) context.get("locale");

        GenericValue orderHeader = null;
        try {
            orderHeader = EntityQuery.use(delegator).from("OrderHeader").where("orderId", orderId).queryOne();
        } catch (GenericEntityException e) {
            Debug.logError(e, "Problem getting order header detial", module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                    "OrderCannotGetOrderHeader", locale) + e.getMessage());
        }
        if (orderHeader != null) {
            Map<String, Object> result = ServiceUtil.returnSuccess();
            result.putAll(orderHeader);
            return result;
        }
        return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                "OrderErrorGettingOrderHeaderInformationNull", locale));
    }

    /** Service to get the total shipping for an order. */
    public static Map<String, Object> getOrderShippingAmount(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        String orderId = (String) context.get("orderId");
        Locale locale = (Locale) context.get("locale");

        GenericValue orderHeader = null;
        try {
            orderHeader = EntityQuery.use(delegator).from("OrderHeader").where("orderId", orderId).queryOne();
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                    "OrderErrorCouldNotGetOrderInformation", locale) + e.getMessage() + ").");
        }

        Map<String, Object> result = null;
        if (orderHeader != null) {
            OrderReadHelper orh = new OrderReadHelper(orderHeader);
            List<GenericValue> orderItems = orh.getValidOrderItems();
            List<GenericValue> orderAdjustments = orh.getAdjustments();
            List<GenericValue> orderHeaderAdjustments = orh.getOrderHeaderAdjustments();
            BigDecimal orderSubTotal = orh.getOrderItemsSubTotal();

            BigDecimal shippingAmount = OrderReadHelper.getAllOrderItemsAdjustmentsTotal(orderItems, orderAdjustments, false, false, true);
            shippingAmount = shippingAmount.add(OrderReadHelper.calcOrderAdjustments(orderHeaderAdjustments, orderSubTotal, false, false, true));

            result = ServiceUtil.returnSuccess();
            result.put("shippingAmount", shippingAmount);
        } else {
            result = ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                      "OrderUnableToFindOrderHeaderCannotGetShippingAmount", locale));
        }
        return result;
    }

    /** Service to get an order contact mech. */
    public static Map<String, Object> getOrderAddress(DispatchContext dctx, Map<String, ? extends Object> context) {
        Map<String, Object> result = new HashMap<>();
        Delegator delegator = dctx.getDelegator();
        String orderId = (String) context.get("orderId");
        Locale locale = (Locale) context.get("locale");
        //appears to not be used: GenericValue v = null;
        String purpose[] = { "BILLING_LOCATION", "SHIPPING_LOCATION" };
        String outKey[] = { "billingAddress", "shippingAddress" };
        GenericValue orderHeader = null;

        try {
            orderHeader = EntityQuery.use(delegator).from("OrderHeader").where("orderId", orderId).queryOne();
            if (orderHeader != null) {
                result.put("orderHeader", orderHeader);
            }
        } catch (GenericEntityException e) {
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_ERROR);
            result.put(ModelService.ERROR_MESSAGE, UtilProperties.getMessage(resource,
                    "OrderOrderNotFound", UtilMisc.toMap("orderId", orderId), locale));
            return result;
        }
        if (orderHeader == null) {
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_ERROR);
            result.put(ModelService.ERROR_MESSAGE, UtilProperties.getMessage(resource,
                    "OrderOrderNotFound", UtilMisc.toMap("orderId", orderId), locale));
            return result;
        }
        for (int i = 0; i < purpose.length; i++) {
            try {
                GenericValue orderContactMech = EntityUtil.getFirst(orderHeader.getRelated("OrderContactMech", UtilMisc.toMap("contactMechPurposeTypeId", purpose[i]), null, false));
                GenericValue contactMech = orderContactMech.getRelatedOne("ContactMech", false);

                if (contactMech != null) {
                    result.put(outKey[i], contactMech.getRelatedOne("PostalAddress", false));
                }
            } catch (GenericEntityException e) {
                result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_ERROR);
                result.put(ModelService.ERROR_MESSAGE, UtilProperties.getMessage(resource,
                        "OrderOrderContachMechNotFound", UtilMisc.toMap("errorString", e.getMessage()), locale));
                return result;
            }
        }

        result.put("orderId", orderId);
        return result;
    }

    /** Service to create a order header note. */
    public static Map<String, Object> createOrderNote(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String noteString = (String) context.get("note");
        String noteName = (String) context.get("noteName");
        String orderId = (String) context.get("orderId");
        String internalNote = (String) context.get("internalNote");
        Map<String, Object> noteCtx = UtilMisc.<String, Object>toMap("note", noteString, "userLogin", userLogin, "noteName", noteName);
        Locale locale = (Locale) context.get("locale");

        try {
            // Store the note.
            Map<String, Object> noteRes = dispatcher.runSync("createNote", noteCtx);
            if (ServiceUtil.isError(noteRes)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(noteRes));
            }

            String noteId = (String) noteRes.get("noteId");

            if (UtilValidate.isEmpty(noteId)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                        "OrderProblemCreatingTheNoteNoNoteIdReturned", locale));
            }

            // Set the order info
            Map<String, String> fields = UtilMisc.<String, String>toMap("orderId", orderId, "noteId", noteId, "internalNote", internalNote);
            GenericValue v = delegator.makeValue("OrderHeaderNote", fields);

            delegator.create(v);
        } catch (GenericEntityException | GenericServiceException ee) {
            Debug.logError(ee, module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource,
                    "OrderOrderNoteCannotBeCreated", UtilMisc.toMap("errorString", ee.getMessage()), locale));
        }

        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> allowOrderSplit(DispatchContext ctx, Map<String, ? extends Object> context) {
        Delegator delegator = ctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String orderId = (String) context.get("orderId");
        String shipGroupSeqId = (String) context.get("shipGroupSeqId");
        Locale locale = (Locale) context.get("locale");

        // check and make sure we have permission to change the order
        Security security = ctx.getSecurity();
        if (!security.hasEntityPermission("ORDERMGR", "_UPDATE", userLogin)) {
            GenericValue placingCustomer = null;
            try {
                placingCustomer = EntityQuery.use(delegator).from("OrderRole").where("orderId", orderId, "partyId", userLogin.getString("partyId"), "roleTypeId", "PLACING_CUSTOMER").queryOne();
            } catch (GenericEntityException e) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                        "OrderErrorCannotGetOrderRoleEntity", locale) + e.getMessage());
            }
            if (placingCustomer == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                        "OrderYouDoNotHavePermissionToChangeThisOrdersStatus", locale));
            }
        }

        GenericValue shipGroup = null;
        try {
            shipGroup = EntityQuery.use(delegator).from("OrderItemShipGroup").where("orderId", orderId, "shipGroupSeqId", shipGroupSeqId).queryOne();
        } catch (GenericEntityException e) {
            Debug.logError(e, "Problems getting OrderItemShipGroup for : " + orderId + " / " + shipGroupSeqId, module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                    "OrderCannotUpdateProblemGettingOrderShipmentPreference", locale));
        }

        if (shipGroup != null) {
            shipGroup.set("maySplit", "Y");
            try {
                shipGroup.store();
            } catch (GenericEntityException e) {
                Debug.logError("Problem saving OrderItemShipGroup for : " + orderId + " / " + shipGroupSeqId, module);
                return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                        "OrderCannotUpdateProblemSettingOrderShipmentPreference", locale));
            }
        } else {
            Debug.logError("ERROR: Got a NULL OrderItemShipGroup", module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                    "OrderCannotUpdateNoAvailableGroupsToChange", locale));
        }
        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> cancelFlaggedSalesOrders(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        List<GenericValue> ordersToCheck = null;

        // create the query expressions
        List<EntityCondition> exprs = UtilMisc.<EntityCondition>toList(
                EntityCondition.makeCondition("orderTypeId", EntityOperator.EQUALS, "SALES_ORDER"),
                EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "ORDER_COMPLETED"),
                EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "ORDER_CANCELLED"),
                EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "ORDER_REJECTED")
       );

        // get the orders
        try {
            ordersToCheck = EntityQuery.use(delegator).from("OrderHeader").where(exprs).orderBy("orderDate").queryList();
        } catch (GenericEntityException e) {
            Debug.logError(e, "Problem getting order headers", module);
        }

        if (UtilValidate.isEmpty(ordersToCheck)) {
            Debug.logInfo("No orders to check, finished", module);
            return ServiceUtil.returnSuccess();
        }

        Timestamp nowTimestamp = UtilDateTime.nowTimestamp();
        for (GenericValue orderHeader : ordersToCheck) {
            String orderId = orderHeader.getString("orderId");
            String orderStatus = orderHeader.getString("statusId");

            if ("ORDER_CREATED".equals(orderStatus)) {
                // first check for un-paid orders
                Timestamp orderDate = orderHeader.getTimestamp("entryDate");

                // need the store for the order
                GenericValue productStore = null;
                try {
                    productStore = orderHeader.getRelatedOne("ProductStore", false);
                } catch (GenericEntityException e) {
                    Debug.logError(e, "Unable to get ProductStore from OrderHeader", module);
                }

                // default days to cancel
                int daysTillCancel = 30;

                // get the value from the store
                if (productStore != null && productStore.get("daysToCancelNonPay") != null) {
                    daysTillCancel = productStore.getLong("daysToCancelNonPay").intValue();
                }

                if (daysTillCancel > 0) {
                    // 0 days means do not auto-cancel
                    Calendar cal = Calendar.getInstance();
                    cal.setTimeInMillis(orderDate.getTime());
                    cal.add(Calendar.DAY_OF_YEAR, daysTillCancel);
                    Date cancelDate = cal.getTime();
                    Date nowDate = new Date();
                    if (cancelDate.equals(nowDate) || nowDate.after(cancelDate)) {
                        // cancel the order item(s)
                        Map<String, Object> svcCtx = UtilMisc.<String, Object>toMap("orderId", orderId, "statusId", "ITEM_CANCELLED", "userLogin", userLogin);
                        try {
                            // TODO: looks like result is ignored here, but we should be looking for errors
                            Map<String, Object> serviceResult = dispatcher.runSync("changeOrderItemStatus", svcCtx);
                            if (ServiceUtil.isError(serviceResult)) {
                                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                            }
                        } catch (GenericServiceException e) {
                            Debug.logError(e, "Problem calling change item status service : " + svcCtx, module);
                        }
                    }
                }
            } else {
                // check for auto-cancel items
                List<EntityCondition> itemsExprs = new ArrayList<>();

                // create the query expressions
                itemsExprs.add(EntityCondition.makeCondition("orderId", EntityOperator.EQUALS, orderId));
                itemsExprs.add(EntityCondition.makeCondition(UtilMisc.toList(EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, "ITEM_CREATED"),
                        EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, "ITEM_APPROVED")), EntityOperator.OR));
                itemsExprs.add(EntityCondition.makeCondition("dontCancelSetUserLogin", EntityOperator.EQUALS, GenericEntity.NULL_FIELD));
                itemsExprs.add(EntityCondition.makeCondition("dontCancelSetDate", EntityOperator.EQUALS, GenericEntity.NULL_FIELD));
                itemsExprs.add(EntityCondition.makeCondition("autoCancelDate", EntityOperator.NOT_EQUAL, GenericEntity.NULL_FIELD));

                List<GenericValue> orderItems = null;
                try {
                    orderItems = EntityQuery.use(delegator).from("OrderItem").where(itemsExprs).queryList();
                } catch (GenericEntityException e) {
                    Debug.logError(e, "Problem getting order item records", module);
                }
                if (UtilValidate.isNotEmpty(orderItems)) {
                    for (GenericValue orderItem : orderItems) {
                        String orderItemSeqId = orderItem.getString("orderItemSeqId");
                        Timestamp autoCancelDate = orderItem.getTimestamp("autoCancelDate");

                        if (autoCancelDate != null) {
                            if (nowTimestamp.equals(autoCancelDate) || nowTimestamp.after(autoCancelDate)) {
                                // cancel the order item
                                Map<String, Object> svcCtx = UtilMisc.<String, Object>toMap("orderId", orderId, "orderItemSeqId", orderItemSeqId, "statusId", "ITEM_CANCELLED", "userLogin", userLogin);
                                try {
                                    // TODO: check service result for an error return
                                    Map<String, Object> serviceResult = dispatcher.runSync("changeOrderItemStatus", svcCtx);
                                    if (ServiceUtil.isError(serviceResult)) {
                                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                                    }
                                } catch (GenericServiceException e) {
                                    Debug.logError(e, "Problem calling change item status service : " + svcCtx, module);
                                }
                            }
                        }
                    }
                }
            }
        }
        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> checkDigitalItemFulfillment(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String orderId = (String) context.get("orderId");
        Locale locale = (Locale) context.get("locale");

        // need the order header
        GenericValue orderHeader = null;
        try {
            orderHeader = EntityQuery.use(delegator).from("OrderHeader").where("orderId", orderId).queryOne();
        } catch (GenericEntityException e) {
            Debug.logError(e, "ERROR: Unable to get OrderHeader for orderId : " + orderId, module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                    "OrderErrorUnableToGetOrderHeaderForOrderId", UtilMisc.toMap("orderId",orderId), locale));
        }

        // get all the items for the order
        List<GenericValue> orderItems = null;
        if (orderHeader != null) {
            try {
                orderItems = orderHeader.getRelated("OrderItem", null, null, false);
            } catch (GenericEntityException e) {
                Debug.logError(e, "ERROR: Unable to get OrderItem list for orderId : " + orderId, module);
                return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                        "OrderErrorUnableToGetOrderItemListForOrderId", UtilMisc.toMap("orderId",orderId), locale));
            }
        }

        // find any digital or non-product items
        List<GenericValue> nonProductItems = new ArrayList<>();
        List<GenericValue> digitalItems = new ArrayList<>();
        Map<GenericValue, GenericValue> digitalProducts = new HashMap<>();

        if (UtilValidate.isNotEmpty(orderItems)) {
            for (GenericValue item : orderItems) {
                GenericValue product = null;
                try {
                    product = item.getRelatedOne("Product", false);
                } catch (GenericEntityException e) {
                    Debug.logError(e, "ERROR: Unable to get Product from OrderItem", module);
                }
                if (product != null) {
                    GenericValue productType = null;
                    try {
                        productType = product.getRelatedOne("ProductType", false);
                    } catch (GenericEntityException e) {
                        Debug.logError(e, "ERROR: Unable to get ProductType from Product", module);
                    }

                    if (productType != null) {
                        String isPhysical = productType.getString("isPhysical");
                        String isDigital = productType.getString("isDigital");

                        // check for digital and finished/digital goods
                        if (isDigital != null && "Y".equalsIgnoreCase(isDigital)) {
                            // we only invoice APPROVED items
                            if ("ITEM_APPROVED".equals(item.getString("statusId"))) {
                                digitalItems.add(item);
                            }
                            if (isPhysical == null || !"Y".equalsIgnoreCase(isPhysical)) {
                                // 100% digital goods need status change
                                digitalProducts.put(item, product);
                            }
                        }
                    }
                } else {
                    String itemType = item.getString("orderItemTypeId");
                    if (!"PRODUCT_ORDER_ITEM".equals(itemType)) {
                        nonProductItems.add(item);
                    }
                }
            }
        }

        // now process the digital items
        if (digitalItems.size() > 0 || nonProductItems.size() > 0) {
            GenericValue productStore = OrderReadHelper.getProductStoreFromOrder(dispatcher.getDelegator(), orderId);
            boolean invoiceItems = true;
            if (productStore != null && productStore.get("autoInvoiceDigitalItems") != null) {
                invoiceItems = "Y".equalsIgnoreCase(productStore.getString("autoInvoiceDigitalItems"));
            }

            // single list with all invoice items
            List<GenericValue> itemsToInvoice = new LinkedList<>();
            itemsToInvoice.addAll(nonProductItems);
            itemsToInvoice.addAll(digitalItems);

            if (invoiceItems) {
                // invoice all APPROVED digital/non-product goods

                // do something tricky here: run as a different user that can actually create an invoice, post transaction, etc
                Map<String, Object> invoiceResult = null;
                try {
                    GenericValue permUserLogin = EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "system").queryOne();
                    Map<String, Object> invoiceContext = UtilMisc.<String, Object>toMap("orderId", orderId, "billItems", itemsToInvoice, "userLogin", permUserLogin);
                    invoiceResult = dispatcher.runSync("createInvoiceForOrder", invoiceContext);
                    if (ServiceUtil.isError(invoiceResult)) {
                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(invoiceResult));
                    }
                } catch (GenericEntityException | GenericServiceException e) {
                    Debug.logError(e, "ERROR: Unable to invoice digital items", module);
                    return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                            "OrderProblemWithInvoiceCreationDigitalItemsNotFulfilled", locale));
                }
                // update the status of digital goods to COMPLETED; leave physical/digital as APPROVED for pick/ship
                for (GenericValue item : itemsToInvoice) {
                    GenericValue productType = null;
                    GenericValue product = digitalProducts.get(item);
                    boolean markComplete = false;

                    if (product != null) {
                        try {
                            productType = product.getRelatedOne("ProductType", false);
                        } catch (GenericEntityException e) {
                            Debug.logError(e, "ERROR: Unable to get ProductType from Product", module);
                        }
                    } else {
                        String itemType = item.getString("orderItemTypeId");
                        if (!"PRODUCT_ORDER_ITEM".equals(itemType)) {
                            markComplete = true;
                        }
                    }

                    if (product != null && productType != null) {
                        String isPhysical = productType.getString("isPhysical");
                        String isDigital = productType.getString("isDigital");

                        // we were set as a digital good; one more check and change status
                        if ((isDigital != null && "Y".equalsIgnoreCase(isDigital)) &&
                                (isPhysical == null || !"Y".equalsIgnoreCase(isPhysical))) {
                            markComplete = true;
                        }
                    }

                    if (markComplete) {
                        Map<String, Object> statusCtx = new HashMap<>();
                        statusCtx.put("orderId", item.getString("orderId"));
                        statusCtx.put("orderItemSeqId", item.getString("orderItemSeqId"));
                        statusCtx.put("statusId", "ITEM_COMPLETED");
                        statusCtx.put("userLogin", userLogin);
                        try {
                            dispatcher.runSyncIgnore("changeOrderItemStatus", statusCtx);
                        } catch (GenericServiceException e) {
                            Debug.logError(e, "ERROR: Problem setting the status to COMPLETED : " + item, module);
                        }
                    }
                }
            }

            // fulfill the digital goods
            Map<String, Object> fulfillContext = UtilMisc.<String, Object>toMap("orderId", orderId, "orderItems", digitalItems, "userLogin", userLogin);
            Map<String, Object> fulfillResult = null;
            try {
                // will be running in an isolated transaction to prevent rollbacks
                fulfillResult = dispatcher.runSync("fulfillDigitalItems", fulfillContext, 300, true);
            } catch (GenericServiceException e) {
                Debug.logError(e, "ERROR: Unable to fulfill digital items", module);
            }
            if (ModelService.RESPOND_ERROR.equals(fulfillResult.get(ModelService.RESPONSE_MESSAGE))) {
                // this service cannot return error at this point or we will roll back the invoice
                // since payments are already captured; errors should have been logged already.
                // the response message here will be passed as an error to the user.
                return ServiceUtil.returnSuccess((String)fulfillResult.get(ModelService.ERROR_MESSAGE));
            }
        }

        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> fulfillDigitalItems(DispatchContext ctx, Map<String, ? extends Object> context) {
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        //appears to not be used: String orderId = (String) context.get("orderId");
        List<GenericValue> orderItems = UtilGenerics.checkList(context.get("orderItems"));
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");
        if (UtilValidate.isNotEmpty(orderItems)) {
            // loop through the digital items to fulfill
            for (GenericValue orderItem : orderItems) {
                // make sure we have a valid item
                if (orderItem == null) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                            "OrderErrorCannotCheckForFulfillmentItemNotFound", locale));
                }

                // locate the Product & ProductContent records
                GenericValue product = null;
                List<GenericValue> productContent = null;
                try {
                    product = orderItem.getRelatedOne("Product", false);
                    if (product == null) {
                        return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                                "OrderErrorCannotCheckForFulfillmentProductNotFound", locale));
                    }
                    List<EntityExpr> exprs = new ArrayList<>();

                    exprs.add(EntityCondition.makeCondition("productContentTypeId", EntityOperator.IN, UtilMisc.toList("FULFILLMENT_EXTASYNC", "FULFILLMENT_EXTSYNC", "FULFILLMENT_EMAIL", "DIGITAL_DOWNLOAD")));
                    exprs.add(EntityCondition.makeCondition("productId", EntityOperator.EQUALS, product.getString("productId")));

                    // try looking up the parent product if the product has no content and is a variant
                    List<GenericValue> allProductContent = EntityQuery.use(delegator).from("ProductContent").where(exprs).queryList();
                    if (UtilValidate.isEmpty(allProductContent) && ("Y".equals(product.getString("isVariant")))) {
                        GenericValue parentProduct = ProductWorker.getParentProduct(product.getString("productId"), delegator);
                        if (allProductContent == null) {
                            allProductContent = new LinkedList<>();
                        }
                        if (parentProduct != null) {
                            allProductContent.addAll(parentProduct.getRelated("ProductContent", null, null, false));
                        }
                    }

                    if (UtilValidate.isNotEmpty(allProductContent)) {
                        // only keep ones with valid dates
                        productContent = EntityUtil.filterByDate(allProductContent, UtilDateTime.nowTimestamp(), "fromDate", "thruDate", true);
                        Debug.logInfo("Product has " + allProductContent.size() + " associations, " +
                                (productContent == null ? "0" : "" + productContent.size()) + " has valid from/thru dates", module);
                    }
                } catch (GenericEntityException e) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                            "OrderErrorCannotGetProductEntity", locale) + e.getMessage());
                }
                // now use the ProductContent to fulfill the item
                if (UtilValidate.isNotEmpty(productContent)) {
                    for (GenericValue productContentItem : productContent) {
                        GenericValue content = null;
                        try {
                            content = productContentItem.getRelatedOne("Content", false);
                        } catch (GenericEntityException e) {
                            Debug.logError(e,"ERROR: Cannot get Content entity: " + e.getMessage(),module);
                            continue;
                        }

                        String fulfillmentType = productContentItem.getString("productContentTypeId");
                        if ("FULFILLMENT_EXTASYNC".equals(fulfillmentType) || "FULFILLMENT_EXTSYNC".equals(fulfillmentType)) {
                            // external service fulfillment
                            String fulfillmentService = (String) content.get("serviceName"); // Kept for backward compatibility
                            GenericValue custMethod = null;
                            if (UtilValidate.isNotEmpty(content.getString("customMethodId"))) {
                                try {
                                    custMethod = EntityQuery.use(delegator).from("CustomMethod").where("customMethodId", content.get("customMethodId")).cache().queryOne();
                                } catch (GenericEntityException e) {
                                    Debug.logError(e,"ERROR: Cannot get CustomMethod associate to Content entity: " + e.getMessage(),module);
                                    continue;
                                }
                            }
                            if (custMethod != null) {
                                fulfillmentService = custMethod.getString("customMethodName");
                            }
                            if (fulfillmentService == null) {
                                Debug.logError("ProductContent of type FULFILLMENT_EXTERNAL had Content with empty serviceName, can not run fulfillment", module);
                            }
                            Map<String, Object> serviceCtx = UtilMisc.<String, Object>toMap("userLogin", userLogin, "orderItem", orderItem);
                            serviceCtx.putAll(productContentItem.getPrimaryKey());
                            try {
                                Debug.logInfo("Running external fulfillment '" + fulfillmentService + "'", module);
                                if ("FULFILLMENT_EXTASYNC".equals(fulfillmentType)) {
                                    dispatcher.runAsync(fulfillmentService, serviceCtx, true);
                                } else if ("FULFILLMENT_EXTSYNC".equals(fulfillmentType)) {
                                    Map<String, Object> resp = dispatcher.runSync(fulfillmentService, serviceCtx);
                                    if (ServiceUtil.isError(resp)) {
                                        return ServiceUtil.returnError(UtilProperties.getMessage(resource,
                                                "OrderOrderExternalFulfillmentError", locale), null, null, resp);
                                    }
                                }
                            } catch (GenericServiceException e) {
                                Debug.logError(e, "ERROR: Could not run external fulfillment service '" + fulfillmentService + "'; " + e.getMessage(), module);
                            }
                        } else if ("FULFILLMENT_EMAIL".equals(fulfillmentType)) {
                            // digital email fulfillment
                            // TODO: Add support for fulfillment email
                            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                                    "OrderEmailFulfillmentTypeNotYetImplemented", locale));
                        } else if ("DIGITAL_DOWNLOAD".equals(fulfillmentType)) {
                            // digital download fulfillment

                            // Nothing to do for here. Downloads are made available to the user
                            // though a query of OrderItems with related ProductContent.
                        } else {
                            Debug.logError("Invalid fulfillment type : " + fulfillmentType + " not supported.", module);
                        }
                    }
                }
            }
        }
        return ServiceUtil.returnSuccess();
    }

    /** Service to invoice service items from order*/
    public static Map<String, Object> invoiceServiceItems(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String orderId = (String) context.get("orderId");
        Locale locale = (Locale) context.get("locale");

        OrderReadHelper orh = null;
        try {
            orh = new OrderReadHelper(delegator, orderId);
        } catch (IllegalArgumentException e) {
            Debug.logError(e, "ERROR: Unable to get OrderHeader for orderId : " + orderId, module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                    "OrderErrorUnableToGetOrderHeaderForOrderId", UtilMisc.toMap("orderId",orderId), locale));
        }

        // get all the approved items for the order
        List<GenericValue> orderItems = null;
        orderItems = orh.getOrderItemsByCondition(EntityCondition.makeCondition("statusId", "ITEM_APPROVED"));

        // find any service items
        List<GenericValue> serviceItems = new LinkedList<>();
        if (UtilValidate.isNotEmpty(orderItems)) {
            for (GenericValue item : orderItems) {
                GenericValue product = null;
                try {
                    product = item.getRelatedOne("Product", false);
                } catch (GenericEntityException e) {
                    Debug.logError(e, "ERROR: Unable to get Product from OrderItem", module);
                }
                if (product != null) {
                    // check for service goods
                    if ("SERVICE".equals(product.get("productTypeId"))) {
                        serviceItems.add(item);
                    }
                }
            }
        }

        // now process the service items
        if (UtilValidate.isNotEmpty(serviceItems)) {
            // Make sure there is actually something needing invoicing because createInvoiceForOrder doesn't check
            List<GenericValue> billItems = new LinkedList<>();
            for (GenericValue item : serviceItems) {
                BigDecimal orderQuantity = OrderReadHelper.getOrderItemQuantity(item);
                BigDecimal invoiceQuantity = OrderReadHelper.getOrderItemInvoicedQuantity(item);
                BigDecimal outstandingQuantity = orderQuantity.subtract(invoiceQuantity);
                if (outstandingQuantity.compareTo(ZERO) > 0) {
                    billItems.add(item);
                }
            }
            // do something tricky here: run as a different user that can actually create an invoice, post transaction, etc
            Map<String, Object> invoiceResult = null;
            try {
                GenericValue permUserLogin = ServiceUtil.getUserLogin(dctx, context, "system");
                Map<String, Object> invoiceContext = UtilMisc.toMap("orderId", orderId, "billItems", billItems, "userLogin", permUserLogin);
                invoiceResult = dispatcher.runSync("createInvoiceForOrder", invoiceContext);
                if (ServiceUtil.isError(invoiceResult)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(invoiceResult));
                }
            } catch (GenericServiceException e) {
                Debug.logError(e, "ERROR: Unable to invoice service items", module);
                return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                        "OrderProblemWithInvoiceCreationServiceItems", locale));
            }
            // update the status of service goods to COMPLETED;
            for (GenericValue item : serviceItems) {
                Map<String, Object> statusCtx = new HashMap<>();
                statusCtx.put("orderId", item.getString("orderId"));
                statusCtx.put("orderItemSeqId", item.getString("orderItemSeqId"));
                statusCtx.put("statusId", "ITEM_COMPLETED");
                statusCtx.put("userLogin", userLogin);
                try {
                    dispatcher.runSyncIgnore("changeOrderItemStatus", statusCtx);
                } catch (GenericServiceException e) {
                    Debug.logError(e, "ERROR: Problem setting the status to COMPLETED : " + item, module);
                }
            }
        }

        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> addItemToApprovedOrder(DispatchContext dctx, Map<String, ? extends Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");
        String shipGroupSeqId = (String) context.get("shipGroupSeqId");
        String orderId = (String) context.get("orderId");
        String productId = (String) context.get("productId");
        String prodCatalogId = (String) context.get("prodCatalogId");
        BigDecimal basePrice = (BigDecimal) context.get("basePrice");
        BigDecimal quantity = (BigDecimal) context.get("quantity");
        Timestamp itemDesiredDeliveryDate = (Timestamp) context.get("itemDesiredDeliveryDate");
        String overridePrice = (String) context.get("overridePrice");
        String reasonEnumId = (String) context.get("reasonEnumId");
        String orderItemTypeId = (String) context.get("orderItemTypeId");
        String changeComments = (String) context.get("changeComments");
        Boolean calcTax = (Boolean) context.get("calcTax");
        Map<String, String> itemAttributesMap = UtilGenerics.cast(context.get("itemAttributesMap"));

        if (calcTax == null) {
            calcTax = Boolean.TRUE;
        }


        int shipGroupIdx = -1;
        try {
            shipGroupIdx = Integer.parseInt(shipGroupSeqId);
            shipGroupIdx--;
        } catch (NumberFormatException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }
        if (shipGroupIdx < 0) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource,
                    "OrderShipGroupSeqIdInvalid", UtilMisc.toMap("shipGroupSeqId", shipGroupSeqId), locale));
        }
        if (quantity.compareTo(BigDecimal.ONE) < 0) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "OrderItemQtyMustBePositive", locale));
        }

        // obtain a shopping cart object for updating
        ShoppingCart cart = null;
        try {
            cart = loadCartForUpdate(dispatcher, delegator, userLogin, orderId);
        } catch (GeneralException e) {
            return ServiceUtil.returnError(e.getMessage());
        }

        try {
            //For quantity we should test if we allow to add decimal quantity for this product an productStore :
            // if not and if quantity is in decimal format then return error.
            if(! ProductWorker.isDecimalQuantityOrderAllowed(delegator, productId, cart.getProductStoreId())){
                BigDecimal remainder = quantity.remainder(BigDecimal.ONE);
                if (remainder.compareTo(BigDecimal.ZERO) != 0) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(resource_error, "cart.addToCart.quantityInDecimalNotAllowed", locale));
                }
                quantity = quantity.setScale(0, UtilNumber.getRoundingMode("order.rounding"));
            } else {
                quantity = quantity.setScale(UtilNumber.getBigDecimalScale("order.decimals"), UtilNumber.getRoundingMode("order.rounding"));
            }
        } catch(GenericEntityException e) {
            Debug.logError(e.getMessage(), module);
            quantity = BigDecimal.ONE;
        }

        shipGroupIdx = cart.getShipInfoIndex(shipGroupSeqId);

        // add in the new product
        try {
            ShoppingCartItem item = null;
            if ("PURCHASE_ORDER".equals(cart.getOrderType())) {
                GenericValue supplierProduct = cart.getSupplierProduct(productId, quantity, dispatcher);
                if (supplierProduct != null) {
                    item = ShoppingCartItem.makePurchaseOrderItem(null, productId, null, quantity, null, null, prodCatalogId, null, orderItemTypeId, null, dispatcher, cart, supplierProduct, itemDesiredDeliveryDate, itemDesiredDeliveryDate, null);
                    cart.addItem(0, item);
                } else {
                    throw new CartItemModifyException("No supplier information found for product [" + productId + "] and quantity quantity [" + quantity + "], cannot add to cart.");
                }

                if (basePrice != null) {
                    item.setBasePrice(basePrice);
                    item.setIsModifiedPrice(true);
                }

                item.setItemComment(changeComments);
                item.setDesiredDeliveryDate(itemDesiredDeliveryDate);
                cart.clearItemShipInfo(item);
                cart.setItemShipGroupQty(item, item.getQuantity(), shipGroupIdx);
            } else {
                item = ShoppingCartItem.makeItem(null, productId, null, quantity, null, null, null, null, null, null, null, null, prodCatalogId, null, null, null, dispatcher, cart, null, null, null, Boolean.FALSE, Boolean.FALSE);
                if (basePrice != null && overridePrice != null) {
                    item.setBasePrice(basePrice);
                    // special hack to make sure we re-calc the promos after a price change
                    item.setQuantity(quantity.add(BigDecimal.ONE), dispatcher, cart, false);
                    item.setQuantity(quantity, dispatcher, cart, false);
                    item.setBasePrice(basePrice);
                    item.setIsModifiedPrice(true);
                }

                // set the item in the selected ship group
                item.setDesiredDeliveryDate(itemDesiredDeliveryDate);
                shipGroupIdx = cart.getShipInfoIndex(shipGroupSeqId);
                int itemId = cart.getItemIndex(item);
                cart.positionItemToGroup(itemId, quantity, cart.getItemShipGroupIndex(itemId), shipGroupIdx, false);
                cart.clearItemShipInfo(item);
                cart.setItemShipGroupQty(item, item.getQuantity(), shipGroupIdx);
            }
            // set the order item attributes
            if (itemAttributesMap != null) {
                // go through the item attributes map once to get a list of key names
                Set<String> attributeNames = new HashSet<>();
                Set<String> keys  = itemAttributesMap.keySet();
                for (String key : keys) {
                    attributeNames.add(key);
                }
                String attrValue = null;
                for (String attrName : attributeNames) {
                    attrValue = itemAttributesMap.get(attrName);
                    if (UtilValidate.isNotEmpty(attrName)) {
                        item.setOrderItemAttribute(attrName, attrValue);
                        Debug.logInfo("Set item attribute Name: " + attrName + " , Value:" + attrValue, module);
                    }
                }
            }
        } catch (CartItemModifyException | ItemNotFoundException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        Map<String, Object> changeMap = UtilMisc.<String, Object>toMap("itemReasonMap", UtilMisc.<String, Object>toMap("reasonEnumId", reasonEnumId),
                                        "itemCommentMap", UtilMisc.<String, Object>toMap("changeComments", changeComments));
        // save all the updated information
        try {
            saveUpdatedCartToOrder(dispatcher, delegator, cart, locale, userLogin, orderId, changeMap, calcTax, false);
        } catch (GeneralException e) {
            return ServiceUtil.returnError(e.getMessage());
        }

        // log an order note
        try {
            String addedItemToOrder = UtilProperties.getMessage(resource, "OrderAddedItemToOrder", locale);
            Map<String, Object> result = dispatcher.runSync("createOrderNote", UtilMisc.<String, Object>toMap("orderId", orderId, "note", addedItemToOrder +
                    productId + " (" + quantity + ")", "internalNote", "Y", "userLogin", userLogin));
            if (ServiceUtil.isError(result)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(result));
            }
        } catch (GenericServiceException e) {
            Debug.logError(e, module);
        }

        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put("shoppingCart", cart);
        result.put("orderId", orderId);
        return result;
    }

    public static Map<String, Object> updateApprovedOrderItems(DispatchContext dctx, Map<String, ? extends Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");
        String orderId = (String) context.get("orderId");
        Map<String, String> overridePriceMap = UtilGenerics.cast(context.get("overridePriceMap"));
        Map<String, String> itemDescriptionMap = UtilGenerics.cast(context.get("itemDescriptionMap"));
        Map<String, String> itemPriceMap = UtilGenerics.cast(context.get("itemPriceMap"));
        Map<String, String> itemQtyMap = UtilGenerics.cast(context.get("itemQtyMap"));
        Map<String, String> itemReasonMap = UtilGenerics.cast(context.get("itemReasonMap"));
        Map<String, String> itemCommentMap = UtilGenerics.cast(context.get("itemCommentMap"));
        Map<String, String> itemAttributesMap = UtilGenerics.cast(context.get("itemAttributesMap"));
        Map<String, String> itemEstimatedShipDateMap = UtilGenerics.cast(context.get("itemShipDateMap"));
        Map<String, String> itemEstimatedDeliveryDateMap = UtilGenerics.cast(context.get("itemDeliveryDateMap"));
        Map<String, String> itemReserveAfterDateMap = UtilGenerics.cast(context.get("itemReserveAfterDateMap"));
        Boolean calcTax = (Boolean) context.get("calcTax");
        if (calcTax == null) {
            calcTax = Boolean.TRUE;
        }

        // obtain a shopping cart object for updating
        ShoppingCart cart = null;
        try {
            cart = loadCartForUpdate(dispatcher, delegator, userLogin, orderId);
        } catch (GeneralException e) {
            return ServiceUtil.returnError(e.getMessage());
        }

        // go through the item map and obtain the totals per item
        Map<String, BigDecimal> itemTotals = new HashMap<>();
        for (String key : itemQtyMap.keySet()) {
            String quantityStr = itemQtyMap.get(key);
            BigDecimal groupQty = BigDecimal.ZERO;
            try {
                groupQty = (BigDecimal) ObjectType.simpleTypeOrObjectConvert(quantityStr, "BigDecimal", null, locale);
            } catch (GeneralException e) {
                Debug.logError(e, module);
                return ServiceUtil.returnError(e.getMessage());
            }

            if (groupQty.compareTo(BigDecimal.ZERO) < 0) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resource,
                        "OrderItemQtyMustBePositive", locale));
            }

            String[] itemInfo = key.split(":");
            BigDecimal tally = itemTotals.get(itemInfo[0]);
            if (tally == null) {
                tally = groupQty;
            } else {
                tally = tally.add(groupQty);
            }
            itemTotals.put(itemInfo[0], tally);
        }

        // set the items amount/price
        for (String itemSeqId : itemTotals.keySet()) {
            ShoppingCartItem cartItem = cart.findCartItem(itemSeqId);

            if (cartItem != null) {
                BigDecimal qty = itemTotals.get(itemSeqId);
                BigDecimal priceSave = cartItem.getBasePrice();

                try {
                    //For quantity we should test if we allow to add decimal quantity for this product an productStore :
                    // if not and if quantity is in decimal format then return error.
                    if(! ProductWorker.isDecimalQuantityOrderAllowed(delegator, cartItem.getProductId(), cart.getProductStoreId())){
                        BigDecimal remainder = qty.remainder(BigDecimal.ONE);
                        if (remainder.compareTo(BigDecimal.ZERO) != 0) {
                            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error, "cart.addToCart.quantityInDecimalNotAllowed", locale));
                        }
                        qty = qty.setScale(0, UtilNumber.getRoundingMode("order.rounding"));
                    } else {
                        qty = qty.setScale(UtilNumber.getBigDecimalScale("order.decimals"), UtilNumber.getRoundingMode("order.rounding"));
                    }
                } catch(GenericEntityException e) {
                    Debug.logError(e.getMessage(), module);
                    qty = BigDecimal.ONE;
                }

                // set quantity
                try {
                    cartItem.setQuantity(qty, dispatcher, cart, false, false); // trigger external ops, don't reset ship groups (and update prices for both PO and SO items)
                } catch (CartItemModifyException e) {
                    Debug.logError(e, module);
                    return ServiceUtil.returnError(e.getMessage());
                }
                Debug.logInfo("Set item quantity: [" + itemSeqId + "] " + qty, module);

                if (cartItem.getIsModifiedPrice()) {
                    cartItem.setBasePrice(priceSave);
                }

                if (overridePriceMap.containsKey(itemSeqId)) {
                    String priceStr = itemPriceMap.get(itemSeqId);
                    if (UtilValidate.isNotEmpty(priceStr)) {
                        BigDecimal price = null;
                        try {
                            price = (BigDecimal) ObjectType.simpleTypeOrObjectConvert(priceStr, "BigDecimal", null, locale);
                        } catch (GeneralException e) {
                            Debug.logError(e, module);
                            return ServiceUtil.returnError(e.getMessage());
                        }
                        cartItem.setBasePrice(price);
                        cartItem.setIsModifiedPrice(true);
                        Debug.logInfo("Set item price: [" + itemSeqId + "] " + price, module);
                    }

                }

                // Update the item description
                if (itemDescriptionMap != null && itemDescriptionMap.containsKey(itemSeqId)) {
                    String description = itemDescriptionMap.get(itemSeqId);
                    if (UtilValidate.isNotEmpty(description)) {
                        cartItem.setName(description);
                        Debug.logInfo("Set item description: [" + itemSeqId + "] " + description, module);
                    } else {
                        return ServiceUtil.returnError(UtilProperties.getMessage(resource,
                                "OrderItemDescriptionCannotBeEmpty", locale));
                    }
                }

                // Update the item comment
                if (itemCommentMap != null && itemCommentMap.containsKey(itemSeqId)) {
                    String comments = itemCommentMap.get(itemSeqId);
                    if (UtilValidate.isNotEmpty(comments)) {
                        cartItem.setItemComment(comments);
                        Debug.logInfo("Set item comment: [" + itemSeqId + "] " + comments, module);
                    }
                }

                // update the order item attributes
                if (itemAttributesMap != null) {
                    // go through the item attributes map once to get a list of key names
                    Set<String> attributeNames = new HashSet<>();
                    Set<String> keys  = itemAttributesMap.keySet();
                    for (String key : keys) {
                        String[] attributeInfo = key.split(":");
                        attributeNames.add(attributeInfo[0]);
                    }

                    String attrValue = null;
                    for (String attrName : attributeNames) {
                        attrValue = itemAttributesMap.get(attrName + ":" + itemSeqId);
                        if (UtilValidate.isNotEmpty(attrName)) {
                            cartItem.setOrderItemAttribute(attrName, attrValue);
                            Debug.logInfo("Set item attribute Name: [" + itemSeqId + "] " + attrName + " , Value:" + attrValue, module);
                        }
                    }
                }

            } else {
                Debug.logInfo("Unable to locate shopping cart item for seqId #" + itemSeqId, module);
            }
        }
        // Create Estimated Delivery dates
        if (null != itemEstimatedDeliveryDateMap) {
            for (Map.Entry<String, String> entry : itemEstimatedDeliveryDateMap.entrySet()) {
                String itemSeqId =  entry.getKey();

                // ignore internationalised variant of dates
                if (!itemSeqId.endsWith("_i18n")) {
                    String estimatedDeliveryDate = entry.getValue();
                    if (UtilValidate.isNotEmpty(estimatedDeliveryDate)) {
                        Timestamp deliveryDate = Timestamp.valueOf(estimatedDeliveryDate);
                        ShoppingCartItem cartItem = cart.findCartItem(itemSeqId);
                        cartItem.setDesiredDeliveryDate(deliveryDate);
                    }
                }
            }
        }

        // Create Estimated ship dates
        if (null != itemEstimatedShipDateMap) {
            for (Map.Entry<String, String> entry : itemEstimatedShipDateMap.entrySet()) {
                String itemSeqId =  entry.getKey();

                // ignore internationalised variant of dates
                if (!itemSeqId.endsWith("_i18n")) {
                    String estimatedShipDate = entry.getValue();
                    if (UtilValidate.isNotEmpty(estimatedShipDate)) {
                        Timestamp shipDate = Timestamp.valueOf(estimatedShipDate);
                        ShoppingCartItem cartItem = cart.findCartItem(itemSeqId);
                        cartItem.setEstimatedShipDate(shipDate);
                    }
                }
            }
        }
        //Update Reserve After Date
        if (null != itemReserveAfterDateMap) {
            for (Map.Entry<String, String> entry : itemReserveAfterDateMap.entrySet()) {
                String itemSeqId =  entry.getKey();
                // ignore internationalised variant of dates
                if (!itemSeqId.endsWith("_i18n")) {
                    String reserveAfterDateStr = entry.getValue();
                    if (UtilValidate.isNotEmpty(reserveAfterDateStr)) {
                        Timestamp reserveAfterDate = Timestamp.valueOf(reserveAfterDateStr);
                        ShoppingCartItem cartItem = cart.findCartItem(itemSeqId);
                        cartItem.setReserveAfterDate(reserveAfterDate);
                    }
                }
            }
        }

        // update the group amounts
        for (String key : itemQtyMap.keySet()) {
            String quantityStr = itemQtyMap.get(key);
            BigDecimal groupQty = BigDecimal.ZERO;
            try {
                groupQty = (BigDecimal) ObjectType.simpleTypeOrObjectConvert(quantityStr, "BigDecimal", null, locale);
            } catch (GeneralException e) {
                Debug.logError(e, module);
                return ServiceUtil.returnError(e.getMessage());
            }

            String[] itemInfo = key.split(":");
            try {
                Integer.parseInt(itemInfo[1]);
            } catch (NumberFormatException e) {
                Debug.logError(e, module);
                return ServiceUtil.returnError(e.getMessage());
            }

            // set the group qty
            ShoppingCartItem cartItem = cart.findCartItem(itemInfo[0]);
            if (cartItem != null) {
                try {
                    //For quantity we should test if we allow to add decimal quantity for this product an productStore :
                    // if not and if quantity is in decimal format then return error.
                    if(! ProductWorker.isDecimalQuantityOrderAllowed(delegator, cartItem.getProductId(), cart.getProductStoreId())){
                        BigDecimal remainder = groupQty.remainder(BigDecimal.ONE);
                        if (remainder.compareTo(BigDecimal.ZERO) != 0) {
                            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error, "cart.addToCart.quantityInDecimalNotAllowed", locale));
                        }
                        groupQty = groupQty.setScale(0, UtilNumber.getRoundingMode("order.rounding"));
                    } else {
                        groupQty = groupQty.setScale(UtilNumber.getBigDecimalScale("order.decimals"), UtilNumber.getRoundingMode("order.rounding"));
                    }
                } catch(GenericEntityException e) {
                    Debug.logError(e.getMessage(), module);
                    groupQty = BigDecimal.ONE;
                }
                int shipGroupIndex = cart.getShipInfoIndex(itemInfo[1]);
                if (Debug.infoOn()) {
                    Debug.logInfo("Shipping info (before) for group #" + (shipGroupIndex) + " [" + cart.getShipmentMethodTypeId(shipGroupIndex) + " / " + cart.getCarrierPartyId(shipGroupIndex) + "]", module);
                }
                cart.setItemShipGroupQty(cartItem, groupQty, shipGroupIndex);
                if (Debug.infoOn()) {
                    Debug.logInfo("Set ship group qty: [" + itemInfo[0] + " / " + itemInfo[1] + " (" + (shipGroupIndex) + ")] " + groupQty, module);
                    Debug.logInfo("Shipping info (after) for group #" + (shipGroupIndex) + " [" + cart.getShipmentMethodTypeId(shipGroupIndex) + " / " + cart.getCarrierPartyId(shipGroupIndex) + "]", module);
                }
            }
        }

        // save all the updated information
        try {
            saveUpdatedCartToOrder(dispatcher, delegator, cart, locale, userLogin, orderId, UtilMisc.<String, Object>toMap("itemReasonMap", itemReasonMap, "itemCommentMap", itemCommentMap), calcTax, false);
        } catch (GeneralException e) {
            return ServiceUtil.returnError(e.getMessage());
        }

        // run promotions to handle all changes in the cart
        ProductPromoWorker.doPromotions(cart, dispatcher);

        // log an order note
        try {
            Map<String, Object> result = dispatcher.runSync("createOrderNote", UtilMisc.<String, Object>toMap("orderId", orderId, "note", "Updated order.", "internalNote", "Y", "userLogin", userLogin));
            if (ServiceUtil.isError(result)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(result));
            }
        } catch (GenericServiceException e) {
            Debug.logError(e, module);
        }

        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put("shoppingCart", cart);
        result.put("orderId", orderId);
        return result;
    }

    public static Map<String, Object> loadCartForUpdate(DispatchContext dctx, Map<String, ? extends Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();

        String orderId = (String) context.get("orderId");
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        ShoppingCart cart = null;
        Map<String, Object> result = null;
        try {
            cart = loadCartForUpdate(dispatcher, delegator, userLogin, orderId);
            result = ServiceUtil.returnSuccess();
            result.put("shoppingCart", cart);
        } catch (GeneralException e) {
            Debug.logError(e, module);
            result = ServiceUtil.returnError(e.getMessage());
        }

        result.put("orderId", orderId);
        return result;
    }

    /*
     *  Warning: loadCartForUpdate(...) and saveUpdatedCartToOrder(...) must always
     *           be used together in this sequence.
     *           In fact loadCartForUpdate(...) will remove or cancel data associated to the order,
     *           before returning the ShoppingCart object; for this reason, the cart
     *           must be stored back using the method saveUpdatedCartToOrder(...),
     *           because that method will recreate the data.
     */
    private static ShoppingCart loadCartForUpdate(LocalDispatcher dispatcher, Delegator delegator, GenericValue userLogin, String orderId) throws GeneralException {
        // load the order into a shopping cart
        Map<String, Object> loadCartResp = null;
        try {
            loadCartResp = dispatcher.runSync("loadCartFromOrder", UtilMisc.<String, Object>toMap("orderId", orderId,
                                                                                  "skipInventoryChecks", Boolean.TRUE, // the items are already reserved, no need to check again
                                                                                  "skipProductChecks", Boolean.TRUE, // the products are already in the order, no need to check their validity now
                                                                                  "userLogin", userLogin));
        } catch (GenericServiceException e) {
            Debug.logError(e, module);
            throw new GeneralException(e.getMessage());
        }
        if (ServiceUtil.isError(loadCartResp)) {
            throw new GeneralException(ServiceUtil.getErrorMessage(loadCartResp));
        }

        ShoppingCart cart = (ShoppingCart) loadCartResp.get("shoppingCart");
        if (cart == null) {
            throw new GeneralException("Error loading shopping cart from order [" + orderId + "]");
        }
        cart.setOrderId(orderId);

        // Now that the cart is loaded, all the data that will be re-created
        // when the method saveUpdatedCartToOrder(...) will be called, are
        // removed and cancelled:
        // - inventory reservations are cancelled
        // - promotional items are cancelled
        // - order payments are released (cancelled)
        // - offline non received payments are cancelled
        // - promotional, shipping and tax adjustments are removed

        // Inventory reservations
        // find ship group associations
        List<GenericValue> shipGroupAssocs = null;
        try {
            shipGroupAssocs = EntityQuery.use(delegator).from("OrderItemShipGroupAssoc").where("orderId", orderId).queryList();
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            throw new GeneralException(e.getMessage());
        }
        // cancel existing inventory reservations
        if (shipGroupAssocs != null) {
            for (GenericValue shipGroupAssoc : shipGroupAssocs) {
                String orderItemSeqId = shipGroupAssoc.getString("orderItemSeqId");
                String shipGroupSeqId = shipGroupAssoc.getString("shipGroupSeqId");

                Map<String, Object> cancelCtx = UtilMisc.<String, Object>toMap("userLogin", userLogin, "orderId", orderId);
                cancelCtx.put("orderItemSeqId", orderItemSeqId);
                cancelCtx.put("shipGroupSeqId", shipGroupSeqId);

                Map<String, Object> cancelResp = null;
                try {
                    cancelResp = dispatcher.runSync("cancelOrderInventoryReservation", cancelCtx);
                } catch (GenericServiceException e) {
                    Debug.logError(e, module);
                    throw new GeneralException(e.getMessage());
                }
                if (ServiceUtil.isError(cancelResp)) {
                    throw new GeneralException(ServiceUtil.getErrorMessage(cancelResp));
                }
            }
        }

        // cancel promo items -- if the promo still qualifies it will be added by the cart
        List<GenericValue> promoItems = null;
        try {
            promoItems = EntityQuery.use(delegator).from("OrderItem").where("orderId", orderId, "isPromo", "Y").queryList();
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            throw new GeneralException(e.getMessage());
        }
        if (promoItems != null) {
            for (GenericValue promoItem : promoItems) {
                // Skip if the promo is already cancelled
                if ("ITEM_CANCELLED".equals(promoItem.get("statusId"))) {
                    continue;
                }
                Map<String, Object> cancelPromoCtx = UtilMisc.<String, Object>toMap("orderId", orderId);
                cancelPromoCtx.put("orderItemSeqId", promoItem.getString("orderItemSeqId"));
                cancelPromoCtx.put("userLogin", userLogin);
                Map<String, Object> cancelResp = null;
                try {
                    cancelResp = dispatcher.runSync("cancelOrderItemNoActions", cancelPromoCtx);
                } catch (GenericServiceException e) {
                    Debug.logError(e, module);
                    throw new GeneralException(e.getMessage());
                }
                if (ServiceUtil.isError(cancelResp)) {
                    throw new GeneralException(ServiceUtil.getErrorMessage(cancelResp));
                }
            }
        }

        // cancel exiting authorizations
        Map<String, Object> releaseResp = null;
        try {
            releaseResp = dispatcher.runSync("releaseOrderPayments", UtilMisc.<String, Object>toMap("orderId", orderId, "userLogin", userLogin));
        } catch (GenericServiceException e) {
            Debug.logError(e, module);
            throw new GeneralException(e.getMessage());
        }
        if (ServiceUtil.isError(releaseResp)) {
            throw new GeneralException(ServiceUtil.getErrorMessage(releaseResp));
        }

        // cancel other (non-completed and non-cancelled) payments
        List<GenericValue> paymentPrefsToCancel = null;
        try {
            List<EntityExpr> exprs = UtilMisc.toList(EntityCondition.makeCondition("orderId", EntityOperator.EQUALS, orderId));
            exprs.add(EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "PAYMENT_RECEIVED"));
            exprs.add(EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "PAYMENT_CANCELLED"));
            exprs.add(EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "PAYMENT_DECLINED"));
            exprs.add(EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "PAYMENT_SETTLED"));
            exprs.add(EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "PAYMENT_REFUNDED"));
            paymentPrefsToCancel = EntityQuery.use(delegator).from("OrderPaymentPreference").where(exprs).queryList();
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            throw new GeneralException(e.getMessage());
        }
        if (paymentPrefsToCancel != null) {
            for (GenericValue opp : paymentPrefsToCancel) {
                try {
                    opp.set("statusId", "PAYMENT_CANCELLED");
                    opp.store();
                } catch (GenericEntityException e) {
                    Debug.logError(e, module);
                    throw new GeneralException(e.getMessage());
                }
            }
        }

        // remove the adjustments
        try {
            List<EntityCondition> adjExprs = new LinkedList<>();
            adjExprs.add(EntityCondition.makeCondition("orderId", EntityOperator.EQUALS, orderId));
            List<EntityCondition> exprs = new LinkedList<>();
            exprs.add(EntityCondition.makeCondition("orderAdjustmentTypeId", EntityOperator.EQUALS, "PROMOTION_ADJUSTMENT"));
            exprs.add(EntityCondition.makeCondition("orderAdjustmentTypeId", EntityOperator.EQUALS, "SHIPPING_CHARGES"));
            exprs.add(EntityCondition.makeCondition("orderAdjustmentTypeId", EntityOperator.EQUALS, "SALES_TAX"));
            exprs.add(EntityCondition.makeCondition("orderAdjustmentTypeId", EntityOperator.EQUALS, "VAT_TAX"));
            exprs.add(EntityCondition.makeCondition("orderAdjustmentTypeId", EntityOperator.EQUALS, "VAT_PRICE_CORRECT"));
            adjExprs.add(EntityCondition.makeCondition(exprs, EntityOperator.OR));
            EntityCondition cond = EntityCondition.makeCondition(adjExprs, EntityOperator.AND);
            List<GenericValue> orderAdjustmentsToDelete = EntityQuery.use(delegator).from("OrderAdjustment").where(cond).queryList();
            List<GenericValue> orderAdjustmentsToStore = new LinkedList<>();
            List<GenericValue> orderAdjustmentsToRemove = new LinkedList<>();
            if (UtilValidate.isNotEmpty(orderAdjustmentsToDelete)) {
                for (GenericValue orderAdjustment : orderAdjustmentsToDelete) {
                    //check if the adjustment has a related entry in entity OrderAdjustmentBilling
                    List<GenericValue> oaBilling = orderAdjustment.getRelated("OrderAdjustmentBilling", null, null, false);
                    if (UtilValidate.isNotEmpty(oaBilling)) {
                        orderAdjustmentsToRemove.add(orderAdjustment);
                        if ("SALES_TAX".equals(orderAdjustment.get("orderAdjustmentTypeId"))) {
                            //if the orderAdjustment is  a sale tax, set the amount to 0 to avoid amount addition
                            orderAdjustmentsToStore.add(orderAdjustment);
                        }
                    }
                }
            }
            //then remove order Adjustment of the list
            if (UtilValidate.isNotEmpty(orderAdjustmentsToDelete)) {
                orderAdjustmentsToDelete.removeAll(orderAdjustmentsToRemove);
                delegator.removeAll(orderAdjustmentsToDelete);
            }
            if (UtilValidate.isNotEmpty(orderAdjustmentsToStore)) {
                for (GenericValue orderAdjustment : orderAdjustmentsToStore) {
                    orderAdjustment.set("amount", BigDecimal.ZERO);
                }
                delegator.storeAll(orderAdjustmentsToStore);
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            throw new GeneralException(e.getMessage());
        }

        return cart;
    }

    public static Map<String, Object> saveUpdatedCartToOrder(DispatchContext dctx, Map<String, ? extends Object> context) {

        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();

        String orderId = (String) context.get("orderId");
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        ShoppingCart cart = (ShoppingCart) context.get("shoppingCart");
        Map<String, Object> changeMap = UtilGenerics.cast(context.get("changeMap"));
        Locale locale = (Locale) context.get("locale");
        Boolean deleteItems = (Boolean) context.get("deleteItems");
        Boolean calcTax = (Boolean) context.get("calcTax");
        if (calcTax == null) {
            calcTax = Boolean.TRUE;
        }

        Map<String, Object> result = null;
        try {
            saveUpdatedCartToOrder(dispatcher, delegator, cart, locale, userLogin, orderId, changeMap, calcTax, deleteItems);
            result = ServiceUtil.returnSuccess();
        } catch (GeneralException e) {
            Debug.logError(e, module);
            result = ServiceUtil.returnError(e.getMessage());
        }

        result.put("orderId", orderId);
        return result;
    }

    private static void saveUpdatedCartToOrder(LocalDispatcher dispatcher, Delegator delegator, ShoppingCart cart,
            Locale locale, GenericValue userLogin, String orderId, Map<String, Object> changeMap, boolean calcTax,
            boolean deleteItems) throws GeneralException {
        // get/set the shipping estimates. If it's a SALES ORDER, then return an error if there are no ship estimates
        int shipGroupsSize = cart.getShipGroupSize();
        int realShipGroupsSize = (new OrderReadHelper(delegator, orderId)).getOrderItemShipGroups().size();
        // If an empty csi has initially been added to cart.shipInfo by ShoppingCart.setItemShipGroupQty() (called indirectly by ShoppingCart.setUserLogin() and then ProductPromoWorker.doPromotions(), etc.)
        //  shipGroupsSize > realShipGroupsSize are different (+1 for shipGroupsSize), then simply bypass the 1st empty csi!
        int origin = realShipGroupsSize == shipGroupsSize ? 0 : 1;
        for (int gi = origin; gi < shipGroupsSize; gi++) {
            String shipmentMethodTypeId = cart.getShipmentMethodTypeId(gi);
            String carrierPartyId = cart.getCarrierPartyId(gi);
            Debug.logInfo("Getting ship estimate for group #" + gi + " [" + shipmentMethodTypeId + " / " + carrierPartyId + "]", module);
            Map<String, Object> result = ShippingEvents.getShipGroupEstimate(dispatcher, delegator, cart, gi);
            if (("SALES_ORDER".equals(cart.getOrderType())) && (ServiceUtil.isError(result))) {
                Debug.logError(ServiceUtil.getErrorMessage(result), module);
                throw new GeneralException(ServiceUtil.getErrorMessage(result));
            }

            BigDecimal shippingTotal = (BigDecimal) result.get("shippingTotal");
            if (shippingTotal == null) {
                shippingTotal = BigDecimal.ZERO;
            }
            cart.setItemShipGroupEstimate(shippingTotal, gi);
        }

        // calc the sales tax
        CheckOutHelper coh = new CheckOutHelper(dispatcher, delegator, cart);
        if (calcTax) {
            try {
                coh.calcAndAddTax();
            } catch (GeneralException e) {
                Debug.logError(e, module);
                throw new GeneralException(e.getMessage());
            }
        }

        // get the new orderItems, adjustments, shipping info, payments and order item attributes from the cart
        List<Map<String, Object>> modifiedItems = new LinkedList<>();
        List<Map<String, Object>> newItems = new LinkedList<>();
        List<GenericValue> toStore = new LinkedList<>();
        List<GenericValue> toAddList = new ArrayList<>();
        toAddList.addAll(cart.makeAllAdjustments());
        cart.clearAllPromotionAdjustments();
        ProductPromoWorker.doPromotions(cart, dispatcher);

        // validate the payment methods
        Map<String, Object> validateResp = coh.validatePaymentMethods();
        if (ServiceUtil.isError(validateResp)) {
            throw new GeneralException(ServiceUtil.getErrorMessage(validateResp));
        }

        // handle OrderHeader fields
        String billingAccountId = cart.getBillingAccountId();
        if (UtilValidate.isNotEmpty(billingAccountId)) {
            try {
                GenericValue orderHeader = EntityQuery.use(delegator).from("OrderHeader").where("orderId", orderId).queryOne();
                orderHeader.set("billingAccountId", billingAccountId);
                toStore.add(orderHeader);
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
                throw new GeneralException(e.getMessage());
            }
        }

        toStore.addAll(cart.makeOrderItems(false, true, dispatcher));
        toStore.addAll(cart.makeAllAdjustments());

        long groupIndex = cart.getShipInfoSize();
        if (!deleteItems) {
            for (long itr = 1; itr <= groupIndex; itr++) {
                List<GenericValue> removeList = new ArrayList<>();
                for (GenericValue stored: toStore) {
                    if ("OrderAdjustment".equals(stored.getEntityName())) {
                        if (("SHIPPING_CHARGES".equals(stored.get("orderAdjustmentTypeId")) ||
                               "SALES_TAX".equals(stored.get("orderAdjustmentTypeId"))) &&
                                stored.get("orderId").equals(orderId)) {
                            // Removing objects from toStore list for old Shipping and Handling Charges Adjustment and Sales Tax Adjustment.
                            removeList.add(stored);
                        }
                        if ("Y".equals(stored.getString("isManual"))) {
                            // Removing objects from toStore list for Manually added Adjustment.
                            removeList.add(stored);
                        }
                    }
                }
                toStore.removeAll(removeList);
            }
            for (GenericValue toAdd: toAddList) {
                if ("OrderAdjustment".equals(toAdd.getEntityName())) {
                    if ("Y".equals(toAdd.getString("isManual")) && (("PROMOTION_ADJUSTMENT".equals(toAdd.get("orderAdjustmentTypeId"))) ||
                            ("SHIPPING_CHARGES".equals(toAdd.get("orderAdjustmentTypeId"))) || ("SALES_TAX".equals(toAdd.get("orderAdjustmentTypeId"))))) {
                        toStore.add(toAdd);
                    }
                }
            }
        } else {
            // add all the cart adjustments
            toStore.addAll(toAddList);
        }

        // Creating objects for New Shipping and Handling Charges Adjustment and Sales Tax Adjustment
        toStore.addAll(cart.makeAllShipGroupInfos(dispatcher));
        toStore.addAll(cart.makeAllOrderPaymentInfos(dispatcher));
        toStore.addAll(cart.makeAllOrderItemAttributes(orderId, ShoppingCart.FILLED_ONLY));
        List<GenericValue> toRemove = new LinkedList<>();
        if (deleteItems) {
            // flag to delete existing order items and adjustments
            try {
                toRemove.addAll(EntityQuery.use(delegator).from("OrderItemShipGroupAssoc").where("orderId", orderId).queryList());
                toRemove.addAll(EntityQuery.use(delegator).from("OrderItemContactMech").where("orderId", orderId).queryList());
                toRemove.addAll(EntityQuery.use(delegator).from("OrderItemPriceInfo").where("orderId", orderId).queryList());
                toRemove.addAll(EntityQuery.use(delegator).from("OrderItemAttribute").where("orderId", orderId).queryList());
                toRemove.addAll(EntityQuery.use(delegator).from("OrderItemBilling").where("orderId", orderId).queryList());
                toRemove.addAll(EntityQuery.use(delegator).from("OrderItemRole").where("orderId", orderId).queryList());
                toRemove.addAll(EntityQuery.use(delegator).from("OrderItemChange").where("orderId", orderId).queryList());
                toRemove.addAll(EntityQuery.use(delegator).from("OrderAdjustment").where("orderId", orderId).queryList());
                toRemove.addAll(EntityQuery.use(delegator).from("OrderItem").where("orderId", orderId).queryList());
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
            }
        } else {
            // get the empty order item atrributes from the cart and remove them
            toRemove.addAll(cart.makeAllOrderItemAttributes(orderId, ShoppingCart.EMPTY_ONLY));
        }

        // get the promo uses and codes
        for (String promoCodeEntered : cart.getProductPromoCodesEntered()) {
            GenericValue orderProductPromoCode = delegator.makeValue("OrderProductPromoCode");
            orderProductPromoCode.set("orderId", orderId);
            orderProductPromoCode.set("productPromoCodeId", promoCodeEntered);
            toStore.add(orderProductPromoCode);
        }
        for (GenericValue promoUse : cart.makeProductPromoUses()) {
            promoUse.set("orderId", orderId);
            toStore.add(promoUse);
        }

        List<GenericValue> existingPromoCodes = null;
        List<GenericValue> existingPromoUses = null;
        try {
            existingPromoCodes = EntityQuery.use(delegator).from("OrderProductPromoCode").where("orderId", orderId).queryList();
            existingPromoUses = EntityQuery.use(delegator).from("ProductPromoUse").where("orderId", orderId).queryList();
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
        toRemove.addAll(existingPromoCodes);
        toRemove.addAll(existingPromoUses);

        // set the orderId & other information on all new value objects
        List<String> dropShipGroupIds = new LinkedList<>(); // this list will contain the ids of all the ship groups for drop shipments (no reservations)
        for (GenericValue valueObj : toStore) {
            valueObj.set("orderId", orderId);
            if ("OrderItemShipGroup".equals(valueObj.getEntityName())) {
                // ship group
                if (valueObj.get("carrierRoleTypeId") == null) {
                    valueObj.set("carrierRoleTypeId", "CARRIER");
                }
                if (UtilValidate.isNotEmpty(valueObj.get("supplierPartyId"))) {
                    dropShipGroupIds.add(valueObj.getString("shipGroupSeqId"));
                }
            } else if ("OrderAdjustment".equals(valueObj.getEntityName())) {
                // shipping / tax adjustment(s)
                if (UtilValidate.isEmpty(valueObj.get("orderItemSeqId"))) {
                    valueObj.set("orderItemSeqId", DataModelConstants.SEQ_ID_NA);
                }
                // in order to avoid duplicate adjustments don't set orderAdjustmentId (which is the pk) if there is already one
                if (UtilValidate.isEmpty(valueObj.getString("orderAdjustmentId"))) {
                    valueObj.set("orderAdjustmentId", delegator.getNextSeqId("OrderAdjustment"));
                }
                valueObj.set("createdDate", UtilDateTime.nowTimestamp());
                valueObj.set("createdByUserLogin", userLogin.getString("userLoginId"));
            } else if ("OrderPaymentPreference".equals(valueObj.getEntityName())) {
                if (valueObj.get("orderPaymentPreferenceId") == null) {
                    valueObj.set("orderPaymentPreferenceId", delegator.getNextSeqId("OrderPaymentPreference"));
                    valueObj.set("createdDate", UtilDateTime.nowTimestamp());
                    valueObj.set("createdByUserLogin", userLogin.getString("userLoginId"));
                }
                if (valueObj.get("statusId") == null) {
                    valueObj.set("statusId", "PAYMENT_NOT_RECEIVED");
                }
            } else if ("OrderItem".equals(valueObj.getEntityName()) && !deleteItems) {

                //  ignore promotion items. They are added/canceled automatically
                if ("Y".equals(valueObj.getString("isPromo"))) {
                    //Fetching the new promo items and adding it to list so that we can create OrderStatus record for that items.
                    Map<String, Object> promoItem = new HashMap<>();
                    promoItem.put("orderId", valueObj.getString("orderId"));
                    promoItem.put("orderItemSeqId", valueObj.getString("orderItemSeqId"));
                    promoItem.put("quantity", valueObj.getBigDecimal("quantity"));
                    newItems.add(promoItem);
                    continue;
                }
                GenericValue oldOrderItem = null;
                try {
                    oldOrderItem = EntityQuery.use(delegator).from("OrderItem").where("orderId", valueObj.getString("orderId"), "orderItemSeqId", valueObj.getString("orderItemSeqId")).queryOne();
                } catch (GenericEntityException e) {
                    Debug.logError(e, module);
                    throw new GeneralException(e.getMessage());
                }
                if (oldOrderItem != null) {

                    //  Existing order item found. Check for modifications and store if any
                    String oldItemDescription = oldOrderItem.getString("itemDescription") != null ? oldOrderItem.getString("itemDescription") : "";
                    BigDecimal oldQuantity = oldOrderItem.getBigDecimal("quantity") != null ? oldOrderItem.getBigDecimal("quantity") : BigDecimal.ZERO;
                    BigDecimal oldUnitPrice = oldOrderItem.getBigDecimal("unitPrice") != null ? oldOrderItem.getBigDecimal("unitPrice") : BigDecimal.ZERO;
                    String oldItemComment = oldOrderItem.getString("comments") != null ? oldOrderItem.getString("comments") : "";

                    boolean changeFound = false;
                    Map<String, Object> modifiedItem = new HashMap<>();
                    if (!oldItemDescription.equals(valueObj.getString("itemDescription"))) {
                        modifiedItem.put("itemDescription", oldItemDescription);
                        changeFound = true;
                    }

                    if (!oldItemComment.equals(valueObj.getString("comments"))) {
                        modifiedItem.put("changeComments", valueObj.getString("comments"));
                        changeFound = true;
                    }

                    BigDecimal quantityDif = valueObj.getBigDecimal("quantity").subtract(oldQuantity);
                    BigDecimal unitPriceDif = valueObj.getBigDecimal("unitPrice").subtract(oldUnitPrice);
                    if (quantityDif.compareTo(BigDecimal.ZERO) != 0) {
                        modifiedItem.put("quantity", quantityDif);
                        changeFound = true;
                    }
                    if (unitPriceDif.compareTo(BigDecimal.ZERO) != 0) {
                        modifiedItem.put("unitPrice", unitPriceDif);
                        changeFound = true;
                    }
                    if (changeFound) {

                        //  found changes to store
                        Map<String, String> itemReasonMap = UtilGenerics.cast(changeMap.get("itemReasonMap"));
                        if (UtilValidate.isNotEmpty(itemReasonMap)) {
                            String changeReasonId = itemReasonMap.get(valueObj.getString("orderItemSeqId"));
                            modifiedItem.put("reasonEnumId", changeReasonId);
                        }

                        modifiedItem.put("orderId", valueObj.getString("orderId"));
                        modifiedItem.put("orderItemSeqId", valueObj.getString("orderItemSeqId"));
                        modifiedItem.put("changeTypeEnumId", "ODR_ITM_UPDATE");
                        modifiedItems.add(modifiedItem);
                    }
                } else {

                    //  this is a new item appended to the order
                    Map<String, String> itemReasonMap = UtilGenerics.cast(changeMap.get("itemReasonMap"));
                    Map<String, String> itemCommentMap = UtilGenerics.cast(changeMap.get("itemCommentMap"));
                    Map<String, Object> appendedItem = new HashMap<>();
                    if (UtilValidate.isNotEmpty(itemReasonMap)) {
                        String changeReasonId = itemReasonMap.get("reasonEnumId");
                        appendedItem.put("reasonEnumId", changeReasonId);
                    }
                    if (UtilValidate.isNotEmpty(itemCommentMap)) {
                        String changeComments = itemCommentMap.get("changeComments");
                        appendedItem.put("changeComments", changeComments);
                    }

                    appendedItem.put("orderId", valueObj.getString("orderId"));
                    appendedItem.put("orderItemSeqId", valueObj.getString("orderItemSeqId"));
                    appendedItem.put("quantity", valueObj.getBigDecimal("quantity"));
                    appendedItem.put("changeTypeEnumId", "ODR_ITM_APPEND");
                    modifiedItems.add(appendedItem);
                    newItems.add(appendedItem);
                }
            }
        }

        if (Debug.verboseOn()) {
             Debug.logVerbose("To Store Contains: " + toStore, module);
        }

        // remove any order item attributes that were set to empty
        try {
            delegator.removeAll(toRemove);
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            throw new GeneralException(e.getMessage());
        }

        // store the new items/adjustments/order item attributes
        try {
            delegator.storeAll(toStore);
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            throw new GeneralException(e.getMessage());
        }

        //  store the OrderItemChange
        if (UtilValidate.isNotEmpty(modifiedItems)) {
            for (Map<String, Object> modifiendItem: modifiedItems) {
                Map<String, Object> serviceCtx = new HashMap<>();
                serviceCtx.put("orderId", modifiendItem.get("orderId"));
                serviceCtx.put("orderItemSeqId", modifiendItem.get("orderItemSeqId"));
                serviceCtx.put("itemDescription", modifiendItem.get("itemDescription"));
                serviceCtx.put("quantity", modifiendItem.get("quantity"));
                serviceCtx.put("unitPrice", modifiendItem.get("unitPrice"));
                serviceCtx.put("changeTypeEnumId", modifiendItem.get("changeTypeEnumId"));
                serviceCtx.put("reasonEnumId", modifiendItem.get("reasonEnumId"));
                serviceCtx.put("changeComments", modifiendItem.get("changeComments"));
                serviceCtx.put("userLogin", userLogin);
                Map<String, Object> resp = null;
                try {
                    resp = dispatcher.runSync("createOrderItemChange", serviceCtx);
                } catch (GenericServiceException e) {
                    Debug.logError(e, module);
                    throw new GeneralException(e.getMessage());
                }
                if (ServiceUtil.isError(resp)) {
                    throw new GeneralException(ServiceUtil.getErrorMessage(resp));
                }
            }
        }

      //To create record of OrderStatus entity
        if (UtilValidate.isNotEmpty(newItems)) {
            for (Map<String, Object> newItem : newItems) {
                String itemStatusId = delegator.getNextSeqId("OrderStatus");
                GenericValue itemStatus = delegator.makeValue("OrderStatus", UtilMisc.toMap("orderStatusId", itemStatusId));
                itemStatus.put("statusId", "ITEM_CREATED");
                itemStatus.put("orderId", newItem.get("orderId"));
                itemStatus.put("orderItemSeqId", newItem.get("orderItemSeqId"));
                itemStatus.put("statusDatetime", UtilDateTime.nowTimestamp());
                itemStatus.set("statusUserLogin", userLogin.get("userLoginId"));
                delegator.create(itemStatus);
            }
        }

        // make the order item object map & the ship group assoc list
        List<GenericValue> orderItemShipGroupAssoc = new LinkedList<>();
        Map<String, GenericValue> itemValuesBySeqId = new HashMap<>();
        for (GenericValue v : toStore) {
            if ("OrderItem".equals(v.getEntityName())) {
                itemValuesBySeqId.put(v.getString("orderItemSeqId"), v);
            } else if ("OrderItemShipGroupAssoc".equals(v.getEntityName())) {
                orderItemShipGroupAssoc.add(v);
            }
        }

        // reserve the inventory
        String productStoreId = cart.getProductStoreId();
        String orderTypeId = cart.getOrderType();
        List<String> resErrorMessages = new LinkedList<>();
        try {
            Debug.logInfo("Calling reserve inventory...", module);
            reserveInventory(delegator, dispatcher, userLogin, locale, orderItemShipGroupAssoc, dropShipGroupIds, itemValuesBySeqId,
                    orderTypeId, productStoreId, resErrorMessages);
        } catch (GeneralException e) {
            Debug.logError(e, module);
            throw new GeneralException(e.getMessage());
        }

        if (resErrorMessages.size() > 0) {
            throw new GeneralException(ServiceUtil.getErrorMessage(ServiceUtil.returnError(resErrorMessages)));
        }
    }

    public static Map<String, Object> processOrderPayments(DispatchContext dctx, Map<String, ? extends Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String orderId = (String) context.get("orderId");
        Locale locale = (Locale) context.get("locale");

        OrderReadHelper orh = new OrderReadHelper(delegator, orderId);
        String productStoreId = orh.getProductStoreId();

        // check if order was already cancelled / rejected
        GenericValue orderHeader = orh.getOrderHeader();
        String orderStatus = orderHeader.getString("statusId");
        if ("ORDER_CANCELLED".equals(orderStatus) || "ORDER_REJECTED".equals(orderStatus)) {
            return ServiceUtil.returnFailure(UtilProperties.getMessage(resource,
                    "OrderProcessOrderPaymentsStatusInvalid", locale) + orderStatus);
        }

        // process the payments
        if (!"PURCHASE_ORDER".equals(orh.getOrderTypeId())) {
            GenericValue productStore = ProductStoreWorker.getProductStore(productStoreId, delegator);
            Map<String, Object> paymentResp = null;
            try {
                Debug.logInfo("Calling process payments...", module);
                paymentResp = CheckOutHelper.processPayment(orderId, orh.getOrderGrandTotal(), orh.getCurrency(), productStore, userLogin, false, false, dispatcher, delegator);
            } catch (GeneralException | GeneralRuntimeException e) {
                Debug.logError(e, module);
                return ServiceUtil.returnError(e.getMessage());
            }

            if (ServiceUtil.isError(paymentResp)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resource,
                        "OrderProcessOrderPayments", locale), null, null, paymentResp);
            }
        }
        return ServiceUtil.returnSuccess();
    }

    // sample test services
    public static Map<String, Object> shoppingCartTest(DispatchContext dctx, Map<String, ? extends Object> context) {
        Locale locale = (Locale) context.get("locale");
        ShoppingCart cart = new ShoppingCart(dctx.getDelegator(), "9000", "webStore", locale, "USD");
        try {
            cart.addOrIncreaseItem("GZ-1005", null, BigDecimal.ONE, null, null, null, null, null, null, null, "DemoCatalog", null, null, null, null, dctx.getDispatcher());
        } catch (CartItemModifyException | ItemNotFoundException e) {
            Debug.logError(e, module);
        }

        try {
            dctx.getDispatcher().runAsync("shoppingCartRemoteTest", UtilMisc.toMap("cart", cart), true);
        } catch (GenericServiceException e) {
            Debug.logError(e, module);
        }

        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> shoppingCartRemoteTest(DispatchContext dctx, Map<String, ? extends Object> context) {
        ShoppingCart cart = (ShoppingCart) context.get("cart");
        Debug.logInfo("Product ID : " + cart.findCartItem(0).getProductId(), module);
        return ServiceUtil.returnSuccess();
    }

    /**
     * Service to create a payment using an order payment preference.
     * @return Map
     */
    public static Map<String, Object> createPaymentFromPreference(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String orderPaymentPreferenceId = (String) context.get("orderPaymentPreferenceId");
        String paymentRefNum = (String) context.get("paymentRefNum");
        String paymentFromId = (String) context.get("paymentFromId");
        String comments = (String) context.get("comments");
        Timestamp eventDate = (Timestamp) context.get("eventDate");
        Locale locale = (Locale) context.get("locale");
        if (UtilValidate.isEmpty(eventDate)) {
            eventDate = UtilDateTime.nowTimestamp();
        }
        try {
            // get the order payment preference
            GenericValue orderPaymentPreference = EntityQuery.use(delegator).from("OrderPaymentPreference").where("orderPaymentPreferenceId", orderPaymentPreferenceId).queryOne();
            if (orderPaymentPreference == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resource,
                        "OrderOrderPaymentCannotBeCreated",
                        UtilMisc.toMap("orderPaymentPreferenceId", "orderPaymentPreferenceId"), locale));
            }

            // get the order header
            GenericValue orderHeader = orderPaymentPreference.getRelatedOne("OrderHeader", false);
            if (orderHeader == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resource,
                        "OrderOrderPaymentCannotBeCreatedWithRelatedOrderHeader", locale));
            }

            // get the store for the order.  It will be used to set the currency
            GenericValue productStore = orderHeader.getRelatedOne("ProductStore", false);
            if (productStore == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resource,
                        "OrderOrderPaymentCannotBeCreatedWithRelatedProductStore", locale));
            }

            // get the partyId billed to
            if (paymentFromId == null) {
                OrderReadHelper orh = new OrderReadHelper(orderHeader);
                GenericValue billToParty = orh.getBillToParty();
                if (billToParty != null) {
                    paymentFromId = billToParty.getString("partyId");
                } else {
                    paymentFromId = "_NA_";
                }
            }

            // set the payToPartyId
            String payToPartyId = productStore.getString("payToPartyId");
            if (payToPartyId == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resource,
                        "OrderOrderPaymentCannotBeCreatedPayToPartyIdNotSet", locale));
            }

            // create the payment
            Map<String, Object> paymentParams = new HashMap<>();
            BigDecimal maxAmount = orderPaymentPreference.getBigDecimal("maxAmount");
                paymentParams.put("paymentTypeId", "CUSTOMER_PAYMENT");
                paymentParams.put("paymentMethodTypeId", orderPaymentPreference.getString("paymentMethodTypeId"));
                paymentParams.put("paymentPreferenceId", orderPaymentPreference.getString("orderPaymentPreferenceId"));
                paymentParams.put("amount", maxAmount);
                paymentParams.put("statusId", "PMNT_RECEIVED");
                paymentParams.put("effectiveDate", eventDate);
                paymentParams.put("partyIdFrom", paymentFromId);
                paymentParams.put("currencyUomId", productStore.getString("defaultCurrencyUomId"));
                paymentParams.put("partyIdTo", payToPartyId);
            if (paymentRefNum != null) {
                paymentParams.put("paymentRefNum", paymentRefNum);
            }
            if (comments != null) {
                paymentParams.put("comments", comments);
            }
            paymentParams.put("userLogin", userLogin);

            Map<String, Object> result = dispatcher.runSync("createPayment", paymentParams);
            if (ServiceUtil.isError(result)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(result));
            }
            return result;

        } catch (GenericEntityException | GenericServiceException ex) {
            Debug.logError(ex, "Unable to create payment using payment preference.", module);
            return(ServiceUtil.returnError(ex.getMessage()));
        }
    }

    public static Map<String, Object> massChangeApproved(DispatchContext dctx, Map<String, ? extends Object> context) {
        return massChangeOrderStatus(dctx, context, "ORDER_APPROVED");
    }

    public static Map<String, Object> massCancelOrders(DispatchContext dctx, Map<String, ? extends Object> context) {
        return massChangeItemStatus(dctx, context, "ITEM_CANCELLED");
    }

    public static Map<String, Object> massRejectOrders(DispatchContext dctx, Map<String, ? extends Object> context) {
        return massChangeItemStatus(dctx, context, "ITEM_REJECTED");
    }

    public static Map<String, Object> massHoldOrders(DispatchContext dctx, Map<String, ? extends Object> context) {
        return massChangeOrderStatus(dctx, context, "ORDER_HOLD");
    }

    public static Map<String, Object> massProcessOrders(DispatchContext dctx, Map<String, ? extends Object> context) {
        return massChangeOrderStatus(dctx, context, "ORDER_PROCESSING");
    }

    public static Map<String, Object> massChangeOrderStatus(DispatchContext dctx, Map<String, ? extends Object> context, String statusId) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        List<String> orderIds = UtilGenerics.checkList(context.get("orderIdList"));
        Locale locale = (Locale) context.get("locale");
        for (String orderId : orderIds) {
            if (UtilValidate.isEmpty(orderId)) {
                continue;
            }
            GenericValue orderHeader = null;
            try {
                orderHeader = EntityQuery.use(delegator).from("OrderHeader").where("orderId", orderId).queryOne();
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
                return ServiceUtil.returnError(e.getMessage());
            }
            if (orderHeader == null) {
                return ServiceUtil.returnFailure(UtilProperties.getMessage(resource,
                        "OrderOrderNotFound", UtilMisc.toMap("orderId", orderId), locale));
            }

            Map<String, Object> ctx = new HashMap<>();
            ctx.put("statusId", statusId);
            ctx.put("orderId", orderId);
            ctx.put("setItemStatus", "Y");
            ctx.put("userLogin", userLogin);
            Map<String, Object> resp = null;
            try {
                resp = dispatcher.runSync("changeOrderStatus", ctx);
            } catch (GenericServiceException e) {
                Debug.logError(e, module);
                return ServiceUtil.returnError(e.getMessage());
            }
            if (ServiceUtil.isError(resp)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                        "OrderErrorCouldNotChangeOrderStatus", locale), null, null, resp);
            }
        }
        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> massChangeItemStatus(DispatchContext dctx, Map<String, ? extends Object> context, String statusId) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        List<String> orderIds = UtilGenerics.checkList(context.get("orderIdList"));
        Locale locale = (Locale) context.get("locale");
        for (String orderId : orderIds) {
            if (UtilValidate.isEmpty(orderId)) {
                continue;
            }
            GenericValue orderHeader = null;
            try {
                orderHeader = EntityQuery.use(delegator).from("OrderHeader").where("orderId", orderId).queryOne();
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
                return ServiceUtil.returnError(e.getMessage());
            }
            if (orderHeader == null) {
                return ServiceUtil.returnFailure(UtilProperties.getMessage(resource,
                        "OrderOrderNotFound", UtilMisc.toMap("orderId", orderId), locale));
            }

            Map<String, Object> ctx = new HashMap<>();
            ctx.put("statusId", statusId);
            ctx.put("orderId", orderId);
            ctx.put("userLogin", userLogin);
            Map<String, Object> resp = null;
            try {
                resp = dispatcher.runSync("changeOrderItemStatus", ctx);
            } catch (GenericServiceException e) {
                Debug.logError(e, module);
                return ServiceUtil.returnError(e.getMessage());
            }
            if (ServiceUtil.isError(resp)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                        "OrderErrorCouldNotChangeItemStatus", locale), null, null, resp);
            }
        }
        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> massQuickShipOrders(DispatchContext dctx, Map<String, ? extends Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        List<String> orderIds = UtilGenerics.checkList(context.get("orderIdList"));
        Locale locale = (Locale) context.get("locale");
        for (Object orderId : orderIds) {
            if (UtilValidate.isEmpty(orderId)) {
                continue;
            }
            Map<String, Object> ctx = new HashMap<>();
            ctx.put("userLogin", userLogin);
            ctx.put("orderId", orderId);

            Map<String, Object> resp = null;
            try {
                resp = dispatcher.runSync("quickShipEntireOrder", ctx);
            } catch (GenericServiceException e) {
                Debug.logError(e, module);
                return ServiceUtil.returnError(e.getMessage());
            }
            if (ServiceUtil.isError(resp)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resource,
                        "OrderOrderQuickShipEntireOrderError", locale), null, null, resp);
            }
        }
        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> massPickOrders(DispatchContext dctx, Map<String, ? extends Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");
        // grouped by facility
        Map<String, List<String>> facilityOrdersMap = new HashMap<>();

        // make the list per facility
        List<String> orderIds = UtilGenerics.checkList(context.get("orderIdList"));
        for (String orderId : orderIds) {
            if (UtilValidate.isEmpty(orderId)) {
                continue;
            }
            List<GenericValue> invInfo = null;
            try {
                invInfo = EntityQuery.use(delegator).from("OrderItemAndShipGrpInvResAndItem").where("orderId", orderId, "statusId", "ITEM_APPROVED").queryList();
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
                return ServiceUtil.returnError(e.getMessage());
            }
            if (invInfo != null) {
                for (GenericValue inv : invInfo) {
                    String facilityId = inv.getString("facilityId");
                    List<String> orderIdsByFacility = facilityOrdersMap.get(facilityId);
                    if (orderIdsByFacility == null) {
                        orderIdsByFacility = new ArrayList<>();
                    }
                    orderIdsByFacility.add(orderId);
                    facilityOrdersMap.put(facilityId, orderIdsByFacility);
                }
            }
        }

        // now create the pick lists for each facility
        for (String facilityId : facilityOrdersMap.keySet()) {
            List<String> orderIdList = facilityOrdersMap.get(facilityId);

            Map<String, Object> ctx = new HashMap<>();
            ctx.put("userLogin", userLogin);
            ctx.put("orderIdList", orderIdList);
            ctx.put("facilityId", facilityId);

            Map<String, Object> resp = null;
            try {
                resp = dispatcher.runSync("createPicklistFromOrders", ctx);
            } catch (GenericServiceException e) {
                Debug.logError(e, module);
                return ServiceUtil.returnError(e.getMessage());
            }
            if (ServiceUtil.isError(resp)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resource,
                        "OrderOrderPickingListCreationError", locale), null, null, resp);
            }
        }

        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> massPrintOrders(DispatchContext dctx, Map<String, ? extends Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String screenLocation = (String) context.get("screenLocation");
        String printerName = (String) context.get("printerName");

        // make the list per facility
        List<String> orderIds = UtilGenerics.checkList(context.get("orderIdList"));
        for (String orderId : orderIds) {
            if (UtilValidate.isEmpty(orderId)) {
                continue;
            }
            Map<String, Object> ctx = new HashMap<>();
            ctx.put("userLogin", userLogin);
            ctx.put("screenLocation", screenLocation);
            if (UtilValidate.isNotEmpty(printerName)) {
                ctx.put("printerName", printerName);
            }
            ctx.put("screenContext", UtilMisc.toMap("orderId", orderId));

            try {
                dispatcher.runAsync("sendPrintFromScreen", ctx);
            } catch (GenericServiceException e) {
                Debug.logError(e, module);
            }
        }
        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> massCreateFileForOrders(DispatchContext dctx, Map<String, ? extends Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String screenLocation = (String) context.get("screenLocation");

        // make the list per facility
        List<String> orderIds = UtilGenerics.checkList(context.get("orderIdList"));
        for (String orderId : orderIds) {
            if (UtilValidate.isEmpty(orderId)) {
                continue;
            }
            Map<String, Object> ctx = new HashMap<>();
            ctx.put("userLogin", userLogin);
            ctx.put("screenLocation", screenLocation);
            ctx.put("fileName", "order_" + orderId + "_");
            ctx.put("screenContext", UtilMisc.toMap("orderId", orderId));

            try {
                dispatcher.runAsync("createFileFromScreen", ctx);
            } catch (GenericServiceException e) {
                Debug.logError(e, module);
            }
        }
        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> massCancelRemainingPurchaseOrderItems(DispatchContext dctx, Map<String, ? extends Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        List<String> orderIds = UtilGenerics.checkList(context.get("orderIdList"));
        Locale locale = (Locale) context.get("locale");

        for (Object orderId : orderIds) {
            if (UtilValidate.isEmpty(orderId)) {
                continue;
            }
            Map<String, Object> ctx = new HashMap<>();
            ctx.put("orderId", orderId);
            ctx.put("userLogin", userLogin);

            Map<String, Object> resp = null;
            try {
                resp = dispatcher.runSync("cancelRemainingPurchaseOrderItems", ctx);
            } catch (GenericServiceException e) {
                Debug.logError(e, module);
                return ServiceUtil.returnError(e.getMessage());
            }
            if (ServiceUtil.isError(resp)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resource,
                        "OrderOrderCancelRemainingPurchaseOrderItemsError", locale), null, null, resp);
            }
            try {
                resp = dispatcher.runSync("checkOrderItemStatus", ctx);
            } catch (GenericServiceException e) {
                Debug.logError(e, module);
                return ServiceUtil.returnError(e.getMessage());
            }
            if (ServiceUtil.isError(resp)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resource,
                        "OrderOrderCheckOrderItemStatusError", locale), null, null, resp);
            }
        }
        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> checkCreateDropShipPurchaseOrders(DispatchContext ctx, Map<String, ? extends Object> context) {
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        // TODO (use the "system" user)
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String orderId = (String) context.get("orderId");
        Locale locale = (Locale) context.get("locale");
        OrderReadHelper orh = new OrderReadHelper(delegator, orderId);
        // TODO: skip this if there is already a purchase order associated with the sales order (ship group)

        try {
            // if sales order
            if ("SALES_ORDER".equals(orh.getOrderTypeId())) {
                // get the order's ship groups
                for (GenericValue shipGroup : orh.getOrderItemShipGroups()) {
                    if (UtilValidate.isNotEmpty(shipGroup.getString("supplierPartyId"))) {
                        // This ship group is a drop shipment: we create a purchase order for it
                        String supplierPartyId = shipGroup.getString("supplierPartyId");
                        // Set supplier preferred currency for drop-ship (PO) order to support multi currency
                        GenericValue supplierParty = EntityQuery.use(delegator).from("Party").where("partyId", supplierPartyId).queryOne();
                        String currencyUomId = supplierParty.getString("preferredCurrencyUomId");
                        // If supplier currency not found then set currency of sales order
                        if (UtilValidate.isEmpty(currencyUomId)) {
                            currencyUomId = orh.getCurrency();
                        }
                        // create the cart
                        ShoppingCart cart = new ShoppingCart(delegator, orh.getProductStoreId(), null, currencyUomId);
                        cart.setOrderType("PURCHASE_ORDER");
                        cart.setBillToCustomerPartyId(cart.getBillFromVendorPartyId()); //Company
                        cart.setBillFromVendorPartyId(supplierPartyId);
                        cart.setOrderPartyId(supplierPartyId);
                        cart.setAgreementId(shipGroup.getString("supplierAgreementId"));
                        // Get the items associated to it and create po
                        List<GenericValue> items = orh.getValidOrderItems(shipGroup.getString("shipGroupSeqId"));
                        if (UtilValidate.isNotEmpty(items)) {
                            for (GenericValue item : items) {
                                try {
                                    int itemIndex = cart.addOrIncreaseItem(item.getString("productId"),
                                                                           null, // amount
                                                                           item.getBigDecimal("quantity"),
                                                                           null, null, null, // reserv
                                                                           item.getTimestamp("shipBeforeDate"),
                                                                           item.getTimestamp("shipAfterDate"),
                                                                           null, null, null,
                                                                           null, null, null,
                                                                           null, dispatcher);
                                    ShoppingCartItem sci = cart.findCartItem(itemIndex);
                                    sci.setAssociatedOrderId(orderId);
                                    sci.setAssociatedOrderItemSeqId(item.getString("orderItemSeqId"));
                                    sci.setOrderItemAssocTypeId("DROP_SHIPMENT");
                                } catch (CartItemModifyException | ItemNotFoundException e) {
                                    return ServiceUtil.returnError(UtilProperties.getMessage(resource,
                                            "OrderOrderCreatingDropShipmentsError",
                                            UtilMisc.toMap("orderId", orderId, "errorString", e.getMessage()),
                                            locale));
                                }
                            }
                        }

                        // If there are indeed items to drop ship, then create the purchase order
                        if (UtilValidate.isNotEmpty(cart.items())) {
                            //resolve supplier promotion
                            ProductPromoWorker.doPromotions(cart, dispatcher);
                            // set checkout options
                            cart.setDefaultCheckoutOptions(dispatcher);
                            // the shipping address is the one of the customer
                            cart.setAllShippingContactMechId(shipGroup.getString("contactMechId"));
                            // associate ship groups of sales and purchase orders
                            ShoppingCart.CartShipInfo cartShipInfo = cart.getShipGroups().get(0);
                            cartShipInfo.setAssociatedShipGroupSeqId(shipGroup.getString("shipGroupSeqId"));
                            // create the order
                            CheckOutHelper coh = new CheckOutHelper(dispatcher, delegator, cart);
                            coh.createOrder(userLogin);
                        } else {
                            // if there are no items to drop ship, then clear out the supplier partyId
                            Debug.logWarning("No drop ship items found for order [" + shipGroup.getString("orderId") + "] and ship group [" + shipGroup.getString("shipGroupSeqId") + "] and supplier party [" + shipGroup.getString("supplierPartyId") + "].  Supplier party information will be cleared for this ship group", module);
                            shipGroup.set("supplierPartyId", null);
                            shipGroup.store();

                        }
                    }
                }
            }
        } catch (GenericEntityException exc) {
            // TODO: imporve error handling
            return ServiceUtil.returnError(UtilProperties.getMessage(resource,
                    "OrderOrderCreatingDropShipmentsError",
                    UtilMisc.toMap("orderId", orderId, "errorString", exc.getMessage()),
                    locale));
        }

        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> updateOrderPaymentPreference(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        String orderPaymentPreferenceId = (String) context.get("orderPaymentPreferenceId");
        String checkOutPaymentId = (String) context.get("checkOutPaymentId");
        String statusId = (String) context.get("statusId");

        try {
            GenericValue opp = EntityQuery.use(delegator).from("OrderPaymentPreference").where("orderPaymentPreferenceId", orderPaymentPreferenceId).queryOne();
            String paymentMethodId = null;
            String paymentMethodTypeId = null;

            // The checkOutPaymentId is either a paymentMethodId or paymentMethodTypeId
            // the original method did a "\d+" regexp to decide which is the case, this version is more explicit with its lookup of PaymentMethodType
            if (checkOutPaymentId != null) {
                List<GenericValue> paymentMethodTypes = EntityQuery.use(delegator).from("PaymentMethodType").cache(true).queryList();
                for (GenericValue type : paymentMethodTypes) {
                    if (type.get("paymentMethodTypeId").equals(checkOutPaymentId)) {
                        paymentMethodTypeId = (String) type.get("paymentMethodTypeId");
                        break;
                    }
                }
                if (paymentMethodTypeId == null) {
                    GenericValue method = EntityQuery.use(delegator).from("PaymentMethod").where("paymentMethodTypeId", paymentMethodTypeId).queryOne();
                    paymentMethodId = checkOutPaymentId;
                    paymentMethodTypeId = (String) method.get("paymentMethodTypeId");
                }
            }

            Map<String, Object> results = ServiceUtil.returnSuccess();
            if (UtilValidate.isNotEmpty(statusId) && "PAYMENT_CANCELLED".equalsIgnoreCase(statusId)) {
                opp.set("statusId", "PAYMENT_CANCELLED");
                opp.store();
                results.put("orderPaymentPreferenceId", opp.get("orderPaymentPreferenceId"));
            } else {
                GenericValue newOpp = (GenericValue) opp.clone();
                opp.set("statusId", "PAYMENT_CANCELLED");
                opp.store();

                newOpp.set("orderPaymentPreferenceId", delegator.getNextSeqId("OrderPaymentPreference"));
                newOpp.set("paymentMethodId", paymentMethodId);
                newOpp.set("paymentMethodTypeId", paymentMethodTypeId);
                newOpp.setNonPKFields(context);
                newOpp.create();
                results.put("orderPaymentPreferenceId", newOpp.get("orderPaymentPreferenceId"));
            }

            return results;
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }
    }

    /**
     * Generates a product requirement for the total cancelled quantity over all order items for each product
     * @param dctx the dispatch context
     * @param context the context
     * @return the result of the service execution
     */
    public static Map<String, Object> generateReqsFromCancelledPOItems(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");

        String orderId = (String) context.get("orderId");
        String facilityId = (String) context.get("facilityId");

        try {

            GenericValue orderHeader = EntityQuery.use(delegator).from("OrderHeader").where("orderId", orderId).queryOne();

            if (UtilValidate.isEmpty(orderHeader)) {
                String errorMessage = UtilProperties.getMessage(resource_error,
                        "OrderErrorOrderIdNotFound", UtilMisc.toMap("orderId", orderId), locale);
                Debug.logError(errorMessage, module);
                return ServiceUtil.returnError(errorMessage);
            }

            if (! "PURCHASE_ORDER".equals(orderHeader.getString("orderTypeId"))) {
                String errorMessage = UtilProperties.getMessage(resource_error,
                        "ProductErrorOrderNotPurchaseOrder", UtilMisc.toMap("orderId", orderId), locale);
                Debug.logError(errorMessage, module);
                return ServiceUtil.returnError(errorMessage);
            }

            // Build a map of productId -> quantity cancelled over all order items
            Map<String, Object> productRequirementQuantities = new HashMap<>();
            List<GenericValue> orderItems = orderHeader.getRelated("OrderItem", null, null, false);
            for (GenericValue orderItem : orderItems) {
                if (! "PRODUCT_ORDER_ITEM".equals(orderItem.getString("orderItemTypeId"))) {
                    continue;
                }

                // Get the cancelled quantity for the item
                BigDecimal orderItemCancelQuantity = BigDecimal.ZERO;
                if (! UtilValidate.isEmpty(orderItem.get("cancelQuantity"))) {
                    orderItemCancelQuantity = orderItem.getBigDecimal("cancelQuantity");
                }

                if (orderItemCancelQuantity.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }

                String productId = orderItem.getString("productId");
                if (productRequirementQuantities.containsKey(productId)) {
                    orderItemCancelQuantity = orderItemCancelQuantity.add((BigDecimal) productRequirementQuantities.get(productId));
                }
                productRequirementQuantities.put(productId, orderItemCancelQuantity);

            }

            // Generate requirements for each of the product quantities
            for (String productId : productRequirementQuantities.keySet()) {
                BigDecimal requiredQuantity = (BigDecimal) productRequirementQuantities.get(productId);
                Map<String, Object> createRequirementResult = dispatcher.runSync("createRequirement", UtilMisc.<String, Object>toMap("requirementTypeId", "PRODUCT_REQUIREMENT", "facilityId", facilityId, "productId", productId, "quantity", requiredQuantity, "userLogin", userLogin));
                if (ServiceUtil.isError(createRequirementResult)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(createRequirementResult));
                }
            }

        } catch (GenericEntityException | GenericServiceException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        return ServiceUtil.returnSuccess();
    }

    /**
     * Cancels remaining (unreceived) quantities for items of an order. Does not consider received-but-rejected quantities.
     * @param dctx the dispatch context
     * @param context the context
     * @return cancels remaining (unreceived) quantities for items of an order
     */
    public static Map<String, Object> cancelRemainingPurchaseOrderItems(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");

        String orderId = (String) context.get("orderId");

        try {

            GenericValue orderHeader = EntityQuery.use(delegator).from("OrderHeader").where("orderId", orderId).queryOne();

            if (UtilValidate.isEmpty(orderHeader)) {
                String errorMessage = UtilProperties.getMessage(resource_error,
                        "OrderErrorOrderIdNotFound", UtilMisc.toMap("orderId", orderId), locale);
                Debug.logError(errorMessage, module);
                return ServiceUtil.returnError(errorMessage);
            }

            if (! "PURCHASE_ORDER".equals(orderHeader.getString("orderTypeId"))) {
                String errorMessage = UtilProperties.getMessage(resource_error,
                        "OrderErrorOrderNotPurchaseOrder", UtilMisc.toMap("orderId", orderId), locale);
                Debug.logError(errorMessage, module);
                return ServiceUtil.returnError(errorMessage);
            }

            List<GenericValue> orderItems = orderHeader.getRelated("OrderItem", null, null, false);
            for (GenericValue orderItem : orderItems) {
                if (! "PRODUCT_ORDER_ITEM".equals(orderItem.getString("orderItemTypeId"))) {
                    continue;
                }

                // Get the ordered quantity for the item
                BigDecimal orderItemQuantity = BigDecimal.ZERO;
                if (! UtilValidate.isEmpty(orderItem.get("quantity"))) {
                    orderItemQuantity = orderItem.getBigDecimal("quantity");
                }
                BigDecimal orderItemCancelQuantity = BigDecimal.ZERO;
                if (! UtilValidate.isEmpty(orderItem.get("cancelQuantity"))) {
                    orderItemCancelQuantity = orderItem.getBigDecimal("cancelQuantity");
                }

                // Get the received quantity for the order item - ignore the quantityRejected, since rejected items should be reordered
                List<GenericValue> shipmentReceipts = orderItem.getRelated("ShipmentReceipt", null, null, false);
                BigDecimal receivedQuantity = BigDecimal.ZERO;
                for (GenericValue shipmentReceipt : shipmentReceipts) {
                    if (! UtilValidate.isEmpty(shipmentReceipt.get("quantityAccepted"))) {
                        receivedQuantity = receivedQuantity.add(shipmentReceipt.getBigDecimal("quantityAccepted"));
                    }
                }

                BigDecimal quantityToCancel = orderItemQuantity.subtract(orderItemCancelQuantity).subtract(receivedQuantity);
                if (quantityToCancel.compareTo(BigDecimal.ZERO) > 0) {
                Map<String, Object> cancelOrderItemResult = dispatcher.runSync("cancelOrderItem", UtilMisc.toMap("orderId", orderId, "orderItemSeqId", orderItem.get("orderItemSeqId"), "cancelQuantity", quantityToCancel, "userLogin", userLogin));
                if (ServiceUtil.isError(cancelOrderItemResult)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(cancelOrderItemResult));
                }
                }

                // If there's nothing to cancel, the item should be set to completed, if it isn't already
                orderItem.refresh();
                if ("ITEM_APPROVED".equals(orderItem.getString("statusId"))) {
                    Map<String, Object> changeOrderItemStatusResult = dispatcher.runSync("changeOrderItemStatus", UtilMisc.toMap("orderId", orderId, "orderItemSeqId", orderItem.get("orderItemSeqId"), "statusId", "ITEM_COMPLETED", "userLogin", userLogin));
                    if (ServiceUtil.isError(changeOrderItemStatusResult)) {
                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(changeOrderItemStatusResult));
                    }
                }
            }

        } catch (GenericEntityException | GenericServiceException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        return ServiceUtil.returnSuccess();
    }

    // create simple non-product order
    public static Map<String, Object> createSimpleNonProductSalesOrder(DispatchContext dctx, Map<String, ? extends Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();

        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");

        String paymentMethodId = (String) context.get("paymentMethodId");
        String productStoreId = (String) context.get("productStoreId");
        String currency = (String) context.get("currency");
        String partyId = (String) context.get("partyId");
        Map<String, BigDecimal> itemMap = UtilGenerics.cast(context.get("itemMap"));

        ShoppingCart cart = new ShoppingCart(delegator, productStoreId, null, locale, currency);
        try {
            cart.setUserLogin(userLogin, dispatcher);
        } catch (CartItemModifyException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }
        cart.setOrderType("SALES_ORDER");
        cart.setOrderPartyId(partyId);

        for (String item : itemMap.keySet()) {
            BigDecimal price = itemMap.get(item);
            try {
                cart.addNonProductItem("BULK_ORDER_ITEM", item, null, price, BigDecimal.ONE, null, null, null, dispatcher);
            } catch (CartItemModifyException e) {
                Debug.logError(e, module);
                return ServiceUtil.returnError(e.getMessage());
            }
        }

        // set the payment method
        try {
            cart.addPayment(paymentMethodId);
        } catch (IllegalArgumentException e) {
            return ServiceUtil.returnError(e.getMessage());
        }

        // save the order (new tx)
        Map<String, Object> createResp;
        try {
            createResp = dispatcher.runSync("createOrderFromShoppingCart", UtilMisc.toMap("shoppingCart", cart), 90, true);
        } catch (GenericServiceException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }
        if (ServiceUtil.isError(createResp)) {
            return ServiceUtil.returnError(ServiceUtil.getErrorMessage(createResp));
        }

        // auth the order (new tx)
        Map<String, Object> authResp;
        try {
            authResp = dispatcher.runSync("callProcessOrderPayments", UtilMisc.toMap("shoppingCart", cart), 180, true);
        } catch (GenericServiceException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }
        if (ServiceUtil.isError(authResp)) {
            return ServiceUtil.returnError(ServiceUtil.getErrorMessage(authResp));
        }

        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put("orderId", createResp.get("orderId"));
        return result;
    }

    // generic method for creating an order from a shopping cart
    public static Map<String, Object> createOrderFromShoppingCart(DispatchContext dctx, Map<String, ? extends Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();

        ShoppingCart cart = (ShoppingCart) context.get("shoppingCart");
        GenericValue userLogin = cart.getUserLogin();

        CheckOutHelper coh = new CheckOutHelper(dispatcher, delegator, cart);
        Map<String, Object> createOrder = coh.createOrder(userLogin);
        if (ServiceUtil.isError(createOrder)) {
            return createOrder;
        }
        String orderId = (String) createOrder.get("orderId");

        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put("shoppingCart", cart);
        result.put("orderId", orderId);
        return result;
    }

    // generic method for processing an order's payment(s)
    public static Map<String, Object> callProcessOrderPayments(DispatchContext dctx, Map<String, ? extends Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get("locale");

        Transaction trans = null;
        try {
            // disable transaction processing
            trans = TransactionUtil.suspend();

            // get the cart
            ShoppingCart cart = (ShoppingCart) context.get("shoppingCart");
            GenericValue userLogin = cart.getUserLogin();
            Boolean manualHold = (Boolean) context.get("manualHold");
            if (manualHold == null) {
                manualHold = Boolean.FALSE;
            }

            if (!"PURCHASE_ORDER".equals(cart.getOrderType())) {
                String productStoreId = cart.getProductStoreId();
                GenericValue productStore = ProductStoreWorker.getProductStore(productStoreId, delegator);
                CheckOutHelper coh = new CheckOutHelper(dispatcher, delegator, cart);

                // process payment
                Map<String, Object> payResp;
                try {
                    payResp = coh.processPayment(productStore, userLogin, false, manualHold);
                } catch (GeneralException e) {
                    Debug.logError(e, module);
                    return ServiceUtil.returnError(e.getMessage());
                }
                if (ServiceUtil.isError(payResp)) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(resource,
                            "OrderProcessOrderPayments", locale), null, null, payResp);
                }
            }

            return ServiceUtil.returnSuccess();
        } catch (GenericTransactionException e) {
            return ServiceUtil.returnError(e.getMessage());
        } finally {
            // resume transaction
            try {
                TransactionUtil.resume(trans);
            } catch (GenericTransactionException e) {
                Debug.logWarning(e, e.getMessage(), module);
            }
        }
    }

    /**
     * Determines the total amount invoiced for a given order item over all invoices by totalling the item subtotal (via OrderItemBilling),
     *  any adjustments for that item (via OrderAdjustmentBilling), and the item's share of any order-level adjustments (that calculated
     *  by applying the percentage of the items total that the item represents to the order-level adjustments total (also via
     *  OrderAdjustmentBilling). Also returns the quantity invoiced for the item over all invoices, to aid in prorating.
     * @param dctx DispatchContext
     * @param context Map
     * @return Map
     */
    public static Map<String, Object> getOrderItemInvoicedAmountAndQuantity(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get("locale");

        String orderId = (String) context.get("orderId");
        String orderItemSeqId = (String) context.get("orderItemSeqId");

        GenericValue orderHeader = null;
        GenericValue orderItemToCheck = null;
        BigDecimal orderItemTotalValue;
        BigDecimal invoicedQuantity = ZERO; // Quantity invoiced for the target order item
        try {

            orderHeader = EntityQuery.use(delegator).from("OrderHeader").where("orderId", orderId).queryOne();
            if (UtilValidate.isEmpty(orderHeader)) {
                String errorMessage = UtilProperties.getMessage(resource_error,
                        "OrderErrorOrderIdNotFound", context, locale);
                Debug.logError(errorMessage, module);
                return ServiceUtil.returnError(errorMessage);
            }
            orderItemToCheck = EntityQuery.use(delegator).from("OrderItem").where("orderId", orderId, "orderItemSeqId", orderItemSeqId).queryOne();
            if (UtilValidate.isEmpty(orderItemToCheck)) {
                String errorMessage = UtilProperties.getMessage(resource_error,
                        "OrderErrorOrderItemNotFound", context, locale);
                Debug.logError(errorMessage, module);
                return ServiceUtil.returnError(errorMessage);
            }

            BigDecimal orderItemsSubtotal = ZERO; // Aggregated value of order items, non-tax and non-shipping item-level adjustments
            BigDecimal invoicedTotal = ZERO; // Amount invoiced for the target order item
            BigDecimal itemAdjustments = ZERO; // Item-level tax- and shipping-adjustments

            // Aggregate the order items subtotal
            List<GenericValue> orderItems = orderHeader.getRelated("OrderItem", null, UtilMisc.toList("orderItemSeqId"), false);
            for (GenericValue orderItem : orderItems) {
                // Look at the orderItemBillings to discover the amount and quantity ever invoiced for this order item
                List<GenericValue> orderItemBillings = EntityQuery.use(delegator).from("OrderItemBilling").where("orderId", orderId, "orderItemSeqId", orderItem.get("orderItemSeqId")).queryList();
                for (GenericValue orderItemBilling : orderItemBillings) {
                    BigDecimal quantity = orderItemBilling.getBigDecimal("quantity");
                    BigDecimal amount = orderItemBilling.getBigDecimal("amount").setScale(orderDecimals, orderRounding);
                    if (UtilValidate.isEmpty(invoicedQuantity) || UtilValidate.isEmpty(amount)) {
                        continue;
                    }

                    // Add the item base amount to the subtotal
                    orderItemsSubtotal = orderItemsSubtotal.add(quantity.multiply(amount));

                    // If the item is the target order item, add the invoiced quantity and amount to their respective totals
                    if (orderItemSeqId.equals(orderItem.get("orderItemSeqId"))) {
                        invoicedQuantity = invoicedQuantity.add(quantity);
                        invoicedTotal = invoicedTotal.add(quantity.multiply(amount));
                    }
                }

                // Retrieve the adjustments for this item
                List<GenericValue> orderAdjustments = EntityQuery.use(delegator).from("OrderAdjustment").where("orderId", orderId, "orderItemSeqId", orderItem.get("orderItemSeqId")).queryList();
                for (GenericValue orderAdjustment : orderAdjustments) {
                    String orderAdjustmentTypeId = orderAdjustment.getString("orderAdjustmentTypeId");

                    // Look at the orderAdjustmentBillings to discove the amount ever invoiced for this order adjustment
                    List<GenericValue> orderAdjustmentBillings = EntityQuery.use(delegator).from("OrderAdjustmentBilling").where("orderAdjustmentId", orderAdjustment.get("orderAdjustmentId")).queryList();
                    for (GenericValue orderAjustmentBilling : orderAdjustmentBillings) {
                        BigDecimal amount = orderAjustmentBilling.getBigDecimal("amount").setScale(orderDecimals, orderRounding);
                        if (UtilValidate.isEmpty(amount)) {
                            continue;
                        }

                        if ("SALES_TAX".equals(orderAdjustmentTypeId) || "SHIPPING_CHARGES".equals(orderAdjustmentTypeId)) {
                            if (orderItemSeqId.equals(orderItem.get("orderItemSeqId"))) {

                                // Add tax- and shipping-adjustment amounts to the total adjustments for the target order item
                                itemAdjustments = itemAdjustments.add(amount);
                            }
                        } else {

                            // Add non-tax and non-shipping adjustment amounts to the order items subtotal
                            orderItemsSubtotal = orderItemsSubtotal.add(amount);
                            if (orderItemSeqId.equals(orderItem.get("orderItemSeqId"))) {

                                // If the item is the target order item, add non-tax and non-shipping adjustment amounts to the invoiced total
                                invoicedTotal = invoicedTotal.add(amount);
                            }
                        }
                    }
                }
            }

            // Total the order-header-level adjustments for the order
            BigDecimal orderHeaderAdjustmentsTotalValue = ZERO;
            List<GenericValue> orderHeaderAdjustments = EntityQuery.use(delegator).from("OrderAdjustment").where("orderId", orderId, "orderItemSeqId", "_NA_").queryList();
            for (GenericValue orderHeaderAdjustment : orderHeaderAdjustments) {
                List<GenericValue> orderHeaderAdjustmentBillings = EntityQuery.use(delegator).from("OrderAdjustmentBilling").where("orderAdjustmentId", orderHeaderAdjustment.get("orderAdjustmentId")).queryList();
                for (GenericValue orderHeaderAdjustmentBilling : orderHeaderAdjustmentBillings) {
                    BigDecimal amount = orderHeaderAdjustmentBilling.getBigDecimal("amount").setScale(orderDecimals, orderRounding);
                    if (UtilValidate.isEmpty(amount)) {
                        continue;
                    }
                    orderHeaderAdjustmentsTotalValue = orderHeaderAdjustmentsTotalValue.add(amount);
                }
            }

            // How much of the order-level adjustments total does the target order item represent? The assumption is: the same
            //  proportion of the adjustments as of the invoiced total for the item to the invoiced total for all items. These
            //  figures don't take tax- and shipping- adjustments into account, so as to be in accordance with the code in InvoiceServices
            BigDecimal invoicedAmountProportion = ZERO;
            if (orderItemsSubtotal.signum() != 0) {
                invoicedAmountProportion = invoicedTotal.divide(orderItemsSubtotal, 5, orderRounding);
            }
            BigDecimal orderItemHeaderAjustmentAmount = orderHeaderAdjustmentsTotalValue.multiply(invoicedAmountProportion);
            orderItemTotalValue = invoicedTotal.add(orderItemHeaderAjustmentAmount);

            // Add back the tax- and shipping- item-level adjustments for the order item
            orderItemTotalValue = orderItemTotalValue.add(itemAdjustments);

        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put("invoicedAmount", orderItemTotalValue.setScale(orderDecimals, orderRounding));
        result.put("invoicedQuantity", invoicedQuantity.setScale(orderDecimals, orderRounding));
        return result;
    }

    public static Map<String, Object> setOrderPaymentStatus(DispatchContext ctx, Map<String, ? extends Object> context) {
        Delegator delegator = ctx.getDelegator();
        String orderPaymentPreferenceId = (String) context.get("orderPaymentPreferenceId");
        String changeReason = (String) context.get("changeReason");
        Locale locale = (Locale) context.get("locale");
        try {
            GenericValue orderPaymentPreference = EntityQuery.use(delegator).from("OrderPaymentPreference").where("orderPaymentPreferenceId", orderPaymentPreferenceId).queryOne();
            String orderId = orderPaymentPreference.getString("orderId");
            String statusUserLogin = orderPaymentPreference.getString("createdByUserLogin");
            GenericValue orderHeader = EntityQuery.use(delegator).from("OrderHeader").where("orderId", orderId).queryOne();
            if (orderHeader == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                        "OrderErrorCouldNotChangeOrderStatusOrderCannotBeFound", locale));
            }
            String statusId = orderPaymentPreference.getString("statusId");
            if (Debug.verboseOn()) {
                Debug.logVerbose("[OrderServices.setOrderPaymentStatus] : Setting Order Payment Status to : " + statusId, module);
            }
            // create a order payment status
            GenericValue orderStatus = delegator.makeValue("OrderStatus");
            orderStatus.put("statusId", statusId);
            orderStatus.put("orderId", orderId);
            orderStatus.put("orderPaymentPreferenceId", orderPaymentPreferenceId);
            orderStatus.put("statusUserLogin", statusUserLogin);
            orderStatus.put("changeReason", changeReason);

            // Check that the status has actually changed before creating a new record
            GenericValue previousStatus = EntityQuery.use(delegator).from("OrderStatus").where("orderId", orderId, "orderPaymentPreferenceId", orderPaymentPreferenceId).orderBy("-statusDatetime").queryFirst();
            if (previousStatus != null) {
                // Temporarily set some values on the new status so that we can do an equals() check
                orderStatus.put("orderStatusId", previousStatus.get("orderStatusId"));
                orderStatus.put("statusDatetime", previousStatus.get("statusDatetime"));
                if (orderStatus.equals(previousStatus)) {
                    // Status is the same, return without creating
                    return ServiceUtil.returnSuccess();
                }
            }
            orderStatus.put("orderStatusId", delegator.getNextSeqId("OrderStatus"));
            orderStatus.put("statusDatetime", UtilDateTime.nowTimestamp());
            orderStatus.create();

        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                    "OrderErrorCouldNotChangeOrderStatus", locale) + e.getMessage() + ").");
        }

        return ServiceUtil.returnSuccess();
    }
    public static Map<String, Object> runSubscriptionAutoReorders(DispatchContext dctx, Map<String, ? extends Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();

        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");
        int count = 0;
        Map<String, Object> result = null;

        boolean beganTransaction = false;
        List<EntityExpr> exprs = UtilMisc.toList(EntityCondition.makeCondition("automaticExtend", EntityOperator.EQUALS, "Y"),
                EntityCondition.makeCondition("orderId", EntityOperator.NOT_EQUAL, null),
                EntityCondition.makeCondition("productId", EntityOperator.NOT_EQUAL, null));
        try {
            beganTransaction = TransactionUtil.begin();
        } catch (GenericTransactionException e1) {
            Debug.logError(e1, "[Delegator] Could not begin transaction: " + e1.toString(), module);
        }

        try (EntityListIterator eli = EntityQuery.use(delegator).from("Subscription").where(exprs).queryIterator()) {

            if (eli != null) {
                GenericValue subscription;
                while (((subscription = eli.next()) != null)) {

                    Calendar endDate = Calendar.getInstance();
                    endDate.setTime(UtilDateTime.nowTimestamp());
                    // Check if today date + cancel period (if provided) is earlier than the thrudate
                    int field = Calendar.MONTH;
                    if (subscription.get("canclAutmExtTime") != null && subscription.get("canclAutmExtTimeUomId") != null) {
                        if ("TF_day".equals(subscription.getString("canclAutmExtTimeUomId"))) {
                            field = Calendar.DAY_OF_YEAR;
                        } else if ("TF_wk".equals(subscription.getString("canclAutmExtTimeUomId"))) {
                            field = Calendar.WEEK_OF_YEAR;
                        } else if ("TF_mon".equals(subscription.getString("canclAutmExtTimeUomId"))) {
                            field = Calendar.MONTH;
                        } else if ("TF_yr".equals(subscription.getString("canclAutmExtTimeUomId"))) {
                            field = Calendar.YEAR;
                        } else {
                            Debug.logWarning("Don't know anything about canclAutmExtTimeUomId [" + subscription.getString("canclAutmExtTimeUomId") + "], defaulting to month", module);
                        }

                        endDate.add(field, Integer.parseInt(subscription.getString("canclAutmExtTime")));
                    }

                    Calendar endDateSubscription = Calendar.getInstance();
                    endDateSubscription.setTime(subscription.getTimestamp("thruDate"));

                    if (endDate.before(endDateSubscription)) {
                        // nor expired yet.....
                        continue;
                    }

                    result = dispatcher.runSync("loadCartFromOrder", UtilMisc.toMap("orderId", subscription.get("orderId"), "userLogin", userLogin));
                    if (ServiceUtil.isError(result)) {
                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(result));
                    }
                    ShoppingCart cart = (ShoppingCart) result.get("shoppingCart");

                    // remove former orderId from cart (would cause duplicate entry).
                    // orderId is set by order-creation services (including store-specific prefixes, e.g.)
                    cart.setOrderId(null);

                    // only keep the orderitem with the related product.
                    List<ShoppingCartItem> cartItems = cart.items();
                    for (ShoppingCartItem shoppingCartItem : cartItems) {
                        if (!subscription.get("productId").equals(shoppingCartItem.getProductId())) {
                            cart.removeCartItem(shoppingCartItem, dispatcher);
                        }
                    }

                    CheckOutHelper helper = new CheckOutHelper(dispatcher, delegator, cart);

                    // store the order
                    Map<String, Object> createResp = helper.createOrder(userLogin);
                    if (createResp != null && ServiceUtil.isError(createResp)) {
                        Debug.logError("Cannot create order for shopping list - " + subscription, module);
                    } else {
                        String orderId = (String) createResp.get("orderId");

                        // authorize the payments
                        Map<String, Object> payRes = null;
                        try {
                            payRes = helper.processPayment(ProductStoreWorker.getProductStore(cart.getProductStoreId(), delegator), userLogin);
                        } catch (GeneralException e) {
                            Debug.logError(e, module);
                        }

                        if (payRes != null && ServiceUtil.isError(payRes)) {
                            Debug.logError("Payment processing problems with shopping list - " + subscription, module);
                        }

                        // remove the automatic extension flag
                        subscription.put("automaticExtend", "N");
                        subscription.store();

                        // send notification
                        if (orderId != null) {
                            dispatcher.runAsync("sendOrderPayRetryNotification", UtilMisc.toMap("orderId", orderId));
                        }
                        count++;
                    }
                }
            }

        } catch (GenericServiceException e) {
            Debug.logError("Could call service to create cart", module);
            return ServiceUtil.returnError(e.toString());
        } catch (CartItemModifyException e) {
            Debug.logError("Could not modify cart: " + e.toString(), module);
            return ServiceUtil.returnError(e.toString());
        } catch (GenericEntityException e) {
            try {
                // only rollback the transaction if we started one...
                TransactionUtil.rollback(beganTransaction, "Error creating subscription auto-reorders", e);
            } catch (GenericEntityException e2) {
                Debug.logError(e2, "[Delegator] Could not rollback transaction: " + e2.toString(), module);
            }
            Debug.logError(e, "Error while creating new shopping list based automatic reorder" + e.toString(), module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource,
                    "OrderShoppingListCreationError", UtilMisc.toMap("errorString", e.toString()), locale));
        } finally {
            try {
                // only commit the transaction if we started one... this will throw an exception if it fails
                TransactionUtil.commit(beganTransaction);
            } catch (GenericEntityException e) {
                Debug.logError(e, "Could not commit transaction for creating new shopping list based automatic reorder", module);
            }
        }
        return ServiceUtil.returnSuccess(UtilProperties.getMessage(resource,
                "OrderRunSubscriptionAutoReorders", UtilMisc.toMap("count", count), locale));
    }

    /**
     * Create an OrderItemShipGroup record
     * @param dctx
     * @param context
     * @return
     */
    public static Map<String, Object> addOrderItemShipGroup(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> result;
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get("locale" );
        String orderId = (String) context.get("orderId");

        //main message error
        String mainErrorMessage = UtilProperties.getMessage(resource_error, "OrderUnableToAddOISGToOrder", locale);
        Map<String, Object> createOrderItemShipGroupMap = null;
        try {
            createOrderItemShipGroupMap = dctx.makeValidContext("createOrderItemShipGroup", ModelService.IN_PARAM, context);
        } catch (GenericServiceException gse) {
            String errMsg = mainErrorMessage + gse.toString();
            return ServiceUtil.returnError(errMsg);
        }

        try {
            //test if party is a valid carrier
            String carrierPartyId = (String) context.get("carrierPartyId");
            GenericValue carrierRole = EntityQuery.use(delegator).from("PartyRole").where("partyId", carrierPartyId, "roleTypeId", "CARRIER").cache().queryOne();
            if (UtilValidate.isNotEmpty(carrierPartyId) && UtilValidate.isEmpty(carrierRole)) {
                String errMsg = mainErrorMessage + UtilProperties.getMessage(resource_error, "OrderCartShipGroupPartyCarrierNotFound", UtilMisc.toMap("partyId", carrierPartyId), locale);
                return ServiceUtil.returnError(errMsg);
            }

            //test if shipmentMethodTypeId is available for carrier party
            String shipmentMethodTypeId = (String) context.get("shipmentMethodTypeId");
            if (UtilValidate.isNotEmpty(shipmentMethodTypeId)) {
                // carrierPartyId is not in shipmentMethodTypeId
                if (shipmentMethodTypeId.indexOf("_o_" ) == -1) {
                    GenericValue shipmentMethod = EntityQuery.use(delegator).from("CarrierShipmentMethod").where("partyId", carrierPartyId, "roleTypeId", "CARRIER", "shipmentMethodTypeId", shipmentMethodTypeId).cache().queryOne();
                    if (UtilValidate.isEmpty(shipmentMethod)) {
                        String errMsg = mainErrorMessage + UtilProperties.getMessage(resource_error, "OrderCartShipGroupShipmentMethodNotFound", UtilMisc.toMap("shipmentMethodTypeId", shipmentMethodTypeId), locale);
                        return ServiceUtil.returnError(errMsg);
                    }
                } else {
                    // carrierPartyId is in shipmentMethodTypeId
                    String[] carrierShipmentMethod = shipmentMethodTypeId.split("_o_");
                    if (carrierShipmentMethod.length == 2) {
                        shipmentMethodTypeId = carrierShipmentMethod[0];
                        carrierPartyId = carrierShipmentMethod[1];
                    }
                    context.put("carrierPartyId", carrierPartyId);
                    context.put("shipmentMethodTypeId", shipmentMethodTypeId);
                }
            }

            List<GenericValue> oisgs = EntityQuery.use(delegator).from("OrderItemShipGroup").where("orderId", orderId).orderBy("shipGroupSeqId DESC").queryList();
            if (UtilValidate.isNotEmpty(oisgs)) {
                GenericValue oisg = EntityUtil.getFirst(oisgs);
                // set shipmentMethodTypeId, carrierPartyId, carrierRoleTypeId, contactMechId when shipmentMethodTypeId and carrierPartyId are empty
                if (UtilValidate.isEmpty(carrierPartyId) && UtilValidate.isEmpty(shipmentMethodTypeId)) {
                    createOrderItemShipGroupMap.put("shipmentMethodTypeId", oisg.get("shipmentMethodTypeId"));
                    createOrderItemShipGroupMap.put("carrierPartyId", oisg.get("carrierPartyId"));
                    createOrderItemShipGroupMap.put("carrierRoleTypeId", oisg.get("carrierRoleTypeId"));
                    createOrderItemShipGroupMap.put("contactMechId", oisg.get("contactMechId"));
                }
            }
        } catch (GenericEntityException gee) {
            String errMsg = mainErrorMessage + gee.toString();
            return ServiceUtil.returnError(errMsg);
        }

        // set maySplit and isGift for the new oisg to No if they are not present
        if (UtilValidate.isEmpty(createOrderItemShipGroupMap.get("maySplit"))) {
            createOrderItemShipGroupMap.put("maySplit", "N");
        }
        if (UtilValidate.isEmpty(createOrderItemShipGroupMap.get("isGift"))) {
            createOrderItemShipGroupMap.put("isGift", "N");
        }

        //create new oisg
        try {
            result = dctx.getDispatcher().runSync("createOrderItemShipGroup", createOrderItemShipGroupMap);
            if (ServiceUtil.isError(result)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(result));
            }
        } catch (GenericServiceException gse) {
            String errMsg = mainErrorMessage + gse.toString();
            return ServiceUtil.returnError(errMsg);
        }
        return result;
    }

    /**
     * Remove an OrderItemShipGroup record
     * @param ctx
     * @param context a map containing in paramaters
     * @return result: a map containing out parameters
     * @throws GenericEntityException
     */
    public static Map<String, Object> deleteOrderItemShipGroup(DispatchContext ctx, Map<?, ?> context) throws GenericEntityException {
        Delegator delegator = ctx.getDelegator();
        Locale locale = (Locale) context.get("locale" );
        Map<String, Object> result = new HashMap<>();

        GenericValue orderItemShipGroup = (GenericValue) context.get("orderItemShipGroup");
        if (UtilValidate.isEmpty(orderItemShipGroup)) {
            String orderId= (String) context.get("orderId");
            GenericValue orderHeader = EntityQuery.use(delegator).from("OrderHeader").where("orderId", orderId).queryOne();
            String shipGroupSeqId= (String) context.get("shipGroupSeqId");
            if (orderHeader != null && UtilValidate.isNotEmpty(shipGroupSeqId)) {
                orderItemShipGroup = EntityQuery.use(delegator).from("OrderItemShipGroup").where("orderId", orderId, "shipGroupSeqId", shipGroupSeqId).queryOne();
                if (UtilValidate.isEmpty(orderItemShipGroup)) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(resource_error, "OrderItemShipGroupDoesNotExist", locale));
                }
            }
        }
        if (orderItemShipGroup != null) {
            orderItemShipGroup.remove();
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
        }
        return result;
    }

    /**
     * Create orderItem and shipGroup association
     * @param dctx
     * @param context
     * @return
     * @throws GenericEntityException
     */
    public static Map<String, Object> addOrderItemShipGroupAssoc(DispatchContext dctx, Map<String, Object> context)
            throws GenericEntityException {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Locale locale = (Locale) context.get("locale" );
        String orderId = (String) context.get("orderId");
        String orderItemSeqId = (String) context.get("orderItemSeqId");
        String shipGroupSeqId = (String) context.get("shipGroupSeqId");
        BigDecimal quantity = (BigDecimal) context.get("quantity");

        //main message error
        String mainErrorMessage = UtilProperties.getMessage(resource_error, "OrderUnableToAddItemToOISG", locale);
        //test orderItem and check status
        GenericValue orderItem = EntityQuery.use(delegator).from("OrderItem").where("orderId", orderId, "orderItemSeqId", orderItemSeqId).queryOne();
        if (orderItem == null) {
            String errMsg = mainErrorMessage + UtilProperties.getMessage(resource_error, "OrderErrorOrderItemNotFound", UtilMisc.toMap("orderId", orderId, "orderItemSeqId", orderItemSeqId), locale);
            return ServiceUtil.returnError(errMsg);
        }
        String statusId = orderItem.getString("statusId");
        // add OISG only if orderItem is not already prepared
        if ("ITEM_CREATED".equals(statusId) || "ITEM_APPROVED".equals(statusId)) {
            //find OISG
            //by default create a new orderItemShipGroup if null with default carrier and contact from the first OISG
            if ("new".equals(shipGroupSeqId)) {
                try {
                    Map<String, Object> addOrderItemShipGroupMap = dctx.makeValidContext("addOrderItemShipGroup", ModelService.IN_PARAM, context);
                    addOrderItemShipGroupMap.remove("shipGroupSeqId");
                    //get default OrderItemShipGroup value for carrier and contact data
                    List<GenericValue> oisgas = orderItem.getRelated("OrderItemShipGroupAssoc", null, null, false);
                    if (UtilValidate.isNotEmpty(oisgas)) {
                        GenericValue oisga = EntityUtil.getFirst(oisgas);
                        GenericValue oisg = oisga.getRelatedOne("OrderItemShipGroup", false);
                        if (oisg != null) {
                            addOrderItemShipGroupMap.put("shipmentMethodTypeId", oisg.get("shipmentMethodTypeId"));
                            addOrderItemShipGroupMap.put("carrierPartyId", oisg.get("carrierPartyId"));
                            addOrderItemShipGroupMap.put("carrierRoleTypeId", oisg.get("carrierRoleTypeId"));
                            addOrderItemShipGroupMap.put("contactMechId", oisg.get("contactMechId"));
                        }
                    }
                    //call  service to create new oisg
                    Map<String, Object> result = null;
                    result = dispatcher.runSync("addOrderItemShipGroup", addOrderItemShipGroupMap);
                    if (ServiceUtil.isError(result)) {
                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(result));
                    }
                    if (result.containsKey("shipGroupSeqId")) {
                        shipGroupSeqId = (String) result.get("shipGroupSeqId");
                    }
                } catch (GenericServiceException e) {
                    String errMsg = UtilProperties.getMessage(resource, mainErrorMessage, locale);
                    return ServiceUtil.returnError(errMsg);
                }
            }
            GenericValue orderItemShipGroup = EntityQuery.use(delegator).from("OrderItemShipGroup").where("orderId", orderId, "shipGroupSeqId", shipGroupSeqId).queryOne();
            if (UtilValidate.isEmpty(orderItemShipGroup)) {
                String errMsg = mainErrorMessage + UtilProperties.getMessage(resource_error, "OrderCartShipGroupNotFound", UtilMisc.toMap("groupIndex", shipGroupSeqId), locale);
                return ServiceUtil.returnError(errMsg);
            }
            //now test quantity parameter
            //if quantity is null or negative then display error
            if (quantity == null || quantity.compareTo(BigDecimal.ZERO) == -1) {
                String errMsg = mainErrorMessage + UtilProperties.getMessage(resource_error, "OrderQuantityAssociatedCannotBeNullOrNegative", locale);
                return ServiceUtil.returnError(errMsg);
            }
            //test if this association already exist if yes display error
            GenericValue oisgAssoc = EntityQuery.use(delegator).from("OrderItemShipGroupAssoc").where("orderId", orderId, "orderItemSeqId", orderItem.get("orderItemSeqId"), "shipGroupSeqId", shipGroupSeqId).queryOne();
            if (oisgAssoc != null) {
                String errMsg = mainErrorMessage + UtilProperties.getMessage(resource_error, "OrderErrorOrderItemAlreadyRelatedToShipGroup", locale);
                return ServiceUtil.returnError(errMsg);
            }
            //no error, create OISGA
            oisgAssoc = delegator.makeValue("OrderItemShipGroupAssoc", UtilMisc.toMap("orderId", orderId, "orderItemSeqId", orderItem.get("orderItemSeqId"), "shipGroupSeqId", shipGroupSeqId));
            oisgAssoc.set("quantity", quantity);
            oisgAssoc.create();
            return ServiceUtil.returnSuccess();
        }
        String errMsg = UtilProperties.getMessage(resource, mainErrorMessage + orderItem, locale);
        return ServiceUtil.returnError(errMsg);
    }

    /**
     * Update orderItem and shipgroup association
     * @param dctx
     * @param context
     * @return
     * @throws GeneralException
     */
    public static Map<String, Object> updateOrderItemShipGroupAssoc(DispatchContext dctx, Map<String, Object> context)
            throws GeneralException{
        Map<String, Object> result = ServiceUtil.returnSuccess();
        String message = null;
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Locale locale = (Locale) context.get("locale" );
        GenericValue userLogin = (GenericValue) context.get("userLogin" );

        String orderId = (String) context.get("orderId");
        String orderItemSeqId = (String) context.get("orderItemSeqId");
        String shipGroupSeqId = (String) context.get("shipGroupSeqId");
        BigDecimal quantity = (BigDecimal) context.get("quantity");
        if (UtilValidate.isEmpty(quantity)) {
            quantity = BigDecimal.ZERO;
        }
        BigDecimal totalQuantity = (BigDecimal) context.get("totalQuantity");
        if (UtilValidate.isEmpty(totalQuantity)) {
            totalQuantity = BigDecimal.ZERO;
        }

        //main message error
        String mainErrorMessage = UtilProperties.getMessage(resource_error, "OrderUnableToUpdateOrderItemFromOISG", locale);
        Integer rowCount = (Integer) context.get("rowCount");
        Integer rowNumber = (Integer) context.get("rowNumber"); //total row number

        if (rowNumber == null) {
            Long count = EntityQuery.use(delegator).from("OrderItemShipGroupAssoc").where("orderId", orderId, "orderItemSeqId", orderItemSeqId).queryCount();
            rowNumber = count.intValue();
            result.put("rowNumber", rowNumber);
        }

        //find OISG Assoc
        GenericValue oisga = EntityQuery.use(delegator).from("OrderItemShipGroupAssoc").where("orderId", orderId, "orderItemSeqId", orderItemSeqId, "shipGroupSeqId", shipGroupSeqId).queryOne();
        if (UtilValidate.isEmpty(oisga)) {
            String errMsg = mainErrorMessage + " : Order Item Ship Group Assoc Does Not Exist";
            Debug.logError(errMsg, module);
            return ServiceUtil.returnError(errMsg);
        }

        // find OISG associated with oisga
        GenericValue oisg = EntityQuery.use(delegator).from("OrderItemShipGroup").where("orderId", orderId, "shipGroupSeqId", shipGroupSeqId).queryOne();
        //find OrderItem
        GenericValue orderItem = EntityQuery.use(delegator).from("OrderItem").where("orderId", orderId, "orderItemSeqId", orderItemSeqId).queryOne();
        if (UtilValidate.isEmpty(orderItem)) {
            String errMsg = mainErrorMessage + UtilProperties.getMessage(resource_error, "OrderErrorOrderItemNotFound", UtilMisc.toMap("orderId", orderId, "orderItemSeqId", orderItemSeqId), locale);
            return ServiceUtil.returnError(errMsg);
        }

        // update OISGA
        if (oisg != null) {
            //if quantity is 0, delete this association only if there is several oisgaoc
            if (ZERO.compareTo(quantity) == 0) {
                // test if  there is only one oisgaoc then display errror
                if (rowNumber == 1) {
                    String errMsg = mainErrorMessage + UtilProperties.getMessage(resource_error, "OrderQuantityAssociatedCannotBeNullOrNegative", locale);
                    Debug.logError(errMsg, module);
                    return ServiceUtil.returnError(errMsg);
                }
                try {
                    Map<String, Object> cancelOrderInventoryReservationMap = dctx.makeValidContext("cancelOrderInventoryReservation", ModelService.IN_PARAM, context);
                    Map<String, Object> localResult = dispatcher.runSync("cancelOrderInventoryReservation", cancelOrderInventoryReservationMap);
                    if (ServiceUtil.isError(localResult)) {
                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(localResult));
                    }
                    Map<String, Object> deleteOrderItemShipGroupAssocMap = dctx.makeValidContext("deleteOrderItemShipGroupAssoc", ModelService.IN_PARAM, context);
                    localResult = dispatcher.runSync("deleteOrderItemShipGroupAssoc", deleteOrderItemShipGroupAssocMap);
                    if (ServiceUtil.isError(localResult)) {
                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(localResult));
                    }
                } catch (GenericServiceException e) {
                    return ServiceUtil.returnError(e.toString());
                }
                //Only for multi service calling and the last row : test if orderItem quantity equals OrderItemShipGroupAssocs quantitys
                if (rowCount != null) {
                    int rowCountInt = rowCount;
                    int rowNumberInt = rowNumber;
                    if (rowCountInt == rowNumberInt - 1) {
                        try {
                            message = validateOrderItemShipGroupAssoc(delegator, dispatcher, orderItem, totalQuantity, oisga, userLogin, locale);
                        }
                        catch (GeneralException e) {
                            String errMsg = mainErrorMessage + UtilProperties.getMessage(resource_error, "OrderQuantityAssociatedIsLessThanOrderItemQuantity", locale);
                            Debug.logError(errMsg, module);
                            return ServiceUtil.returnError(errMsg);
                        }
                    }
                }
                result.put("totalQuantity", totalQuantity);
                if (UtilValidate.isNotEmpty(message)) {
                    result.put(ModelService.SUCCESS_MESSAGE, message);
                }
                return result;
           }

           BigDecimal actualQuantity = totalQuantity.add(quantity);
           BigDecimal qty = (BigDecimal) orderItem.get("quantity");
           if (UtilValidate.isEmpty(qty)) {
               qty = BigDecimal.ZERO;
           }
           BigDecimal cancelQty = (BigDecimal) orderItem.get("cancelQuantity");
           if (UtilValidate.isEmpty(cancelQty)) {
               cancelQty = BigDecimal.ZERO;
           }
           BigDecimal orderItemQuantity = qty.subtract(cancelQty);
            if (actualQuantity.compareTo(orderItemQuantity ) > 0) {
                String errMsg = mainErrorMessage + UtilProperties.getMessage(resource_error, "OrderQuantityAssociatedIsBiggerThanOrderItemQuantity", locale);
                Debug.logError(errMsg, module);
                return ServiceUtil.returnError(errMsg);
            }

            //if quantity is bigger than OI then display error
            if (quantity.compareTo(orderItemQuantity) > 0) {
                String errMsg = mainErrorMessage + UtilProperties.getMessage(resource_error, "OrderQuantityAssociatedIsBiggerThanOrderItemQuantity", locale);
                Debug.logError(errMsg, module);
                return ServiceUtil.returnError(errMsg);
            }
            oisga.set("quantity", quantity);
            //store new values
            oisga.store();
            // reserve the inventory
            GenericValue orderHeader = EntityQuery.use(delegator).from("OrderHeader").where("orderId", orderId).queryOne();
            if (orderHeader != null) {
                Map<String, Object> cancelResp = dispatcher.runSync("cancelOrderInventoryReservation", UtilMisc.toMap("userLogin", userLogin, "orderId", orderId, "orderItemSeqId", orderItemSeqId, "shipGroupSeqId", shipGroupSeqId ));
                if (ServiceUtil.isError(cancelResp)) {
                    throw new GeneralException(ServiceUtil.getErrorMessage(cancelResp));
                }
                String productStoreId = orderHeader.getString("productStoreId");
                String orderTypeId = orderHeader.getString("orderTypeId");
                List<String> resErrorMessages = new LinkedList<>();
                if (Debug.infoOn()) {
                    Debug.logInfo("Calling reserve inventory...", module);
                }
                reserveInventory(delegator, dispatcher, userLogin, locale, UtilMisc.toList(oisga), null, UtilMisc.<String, GenericValue>toMap(orderItemSeqId, orderItem), orderTypeId, productStoreId, resErrorMessages);
            }

            //update totalQuantity
            totalQuantity = totalQuantity.add(quantity);
            result.put("totalQuantity", totalQuantity);

            //Only for multi service calling and the last row : test if orderItem quantity equals OrderItemShipGroupAssocs quantitys
            if (rowCount != null) {
                int rowCountInt = rowCount;
                int rowNumberInt = rowNumber;
                if (rowCountInt == rowNumberInt - 1) {
                    try {
                        message = validateOrderItemShipGroupAssoc(delegator, dispatcher, orderItem, totalQuantity,  oisga, userLogin, locale);
                    }
                    catch (GeneralException e) {
                        String errMsg = mainErrorMessage + UtilProperties.getMessage(resource_error, "OrderQuantityAssociatedIsLessThanOrderItemQuantity", locale);
                        Debug.logError(errMsg, module);
                        return ServiceUtil.returnError(errMsg);
                    }
                }
                if (UtilValidate.isNotEmpty(message)) {
                    result.put(ModelService.SUCCESS_MESSAGE, message);
                }
            }
        } else {
            //update totalQuantity
            totalQuantity = totalQuantity.add(quantity);
            result.put("totalQuantity", totalQuantity);
        }
        return result;
    }

    /**
     * Validate OrderItemShipGroupAssoc quantity
     * This service should be called after updateOrderItemShipGroupAssoc
     * test if orderItem quantity equals OrderItemShipGroupAssocs quantities
     * if not then get the last orderItemShipgroupAssoc estimated shipDate and add quantity to this OrderItemShipGroupAssoc
     * @param ctx
     * @param context
     * @return
     * @throws GeneralException
     */
    private static String validateOrderItemShipGroupAssoc(Delegator delegator, LocalDispatcher dispatcher, GenericValue orderItem, BigDecimal totalQuantity, GenericValue lastOISGAssoc, GenericValue userLogin, Locale locale)
           throws GeneralException {
        String result = null;
        BigDecimal qty = (BigDecimal) orderItem.get("quantity");
        if (UtilValidate.isEmpty(qty)) {
            qty = BigDecimal.ZERO;
        }
        BigDecimal cancelQty = (BigDecimal) orderItem.get("cancelQuantity");
        if (UtilValidate.isEmpty(cancelQty)) {
            cancelQty = BigDecimal.ZERO;
        }

        BigDecimal orderItemQuantity = qty.subtract(cancelQty);
        if (totalQuantity.compareTo(orderItemQuantity) < 0) {
            //if quantity in orderItem is bigger than in totalQUantity then added missing quantity in ShipGroupAssoc
            BigDecimal adjustementQuantity = orderItemQuantity.subtract( totalQuantity);
            BigDecimal lastOISGAssocQuantity = (BigDecimal) lastOISGAssoc.get("quantity");
            if (UtilValidate.isEmpty(lastOISGAssocQuantity)) {
                lastOISGAssocQuantity = BigDecimal.ZERO;
            }
            BigDecimal oisgaQty = lastOISGAssocQuantity.add(adjustementQuantity);
            lastOISGAssoc.set("quantity", oisgaQty);
            lastOISGAssoc.store();

            // reserve the inventory
            GenericValue orderHeader = EntityQuery.use(delegator).from("OrderHeader").where("orderId", lastOISGAssoc.get("orderId")).queryOne();
            if (UtilValidate.isNotEmpty(orderHeader)) {
                Map<String, Object> cancelOrderInventoryReservationMap = UtilMisc.toMap("userLogin", userLogin, "locale", locale);
                cancelOrderInventoryReservationMap.put("orderId", lastOISGAssoc.get("orderId"));
                cancelOrderInventoryReservationMap.put("orderItemSeqId", lastOISGAssoc.get("orderItemSeqId"));
                cancelOrderInventoryReservationMap.put("shipGroupSeqId", lastOISGAssoc.get("shipGroupSeqId"));
                Map<String, Object> cancelResp = dispatcher.runSync("cancelOrderInventoryReservation", cancelOrderInventoryReservationMap);
                if (ServiceUtil.isError(cancelResp)) {
                    throw new GeneralException(ServiceUtil.getErrorMessage(cancelResp));
                }
                String productStoreId = orderHeader.getString("productStoreId");
                String orderTypeId = orderHeader.getString("orderTypeId");
                List<String> resErrorMessages = new LinkedList<>();
                if (Debug.infoOn()) {
                    Debug.logInfo("Calling reserve inventory...", module);
                }
                reserveInventory(delegator, dispatcher, userLogin, locale, UtilMisc.toList(lastOISGAssoc), null, UtilMisc.<String, GenericValue>toMap(lastOISGAssoc.getString("orderItemSeqId"), orderItem), orderTypeId, productStoreId, resErrorMessages);
            }

            //return warning message
            return "Order OISG Assoc Quantity Auto Completed";
        }
        return result;
    }

    public static Map<String, Object> setShippingInstructions(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        String orderId = (String) context.get("orderId");
        String shipGroupSeqId = (String) context.get("shipGroupSeqId");
        String shippingInstructions = (String) context.get("shippingInstructions");
        try {
            GenericValue orderItemShipGroup = EntityQuery.use(delegator).from("OrderItemShipGroup").where("orderId", orderId,"shipGroupSeqId",shipGroupSeqId).queryFirst();
            orderItemShipGroup.set("shippingInstructions", shippingInstructions);
            orderItemShipGroup.store();
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> setGiftMessage(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        String orderId = (String) context.get("orderId");
        String shipGroupSeqId = (String) context.get("shipGroupSeqId");
        String giftMessage = (String) context.get("giftMessage");
        try {
            GenericValue orderItemShipGroup = EntityQuery.use(delegator).from("OrderItemShipGroup").where("orderId", orderId,"shipGroupSeqId",shipGroupSeqId).queryFirst();
            orderItemShipGroup.set("giftMessage", giftMessage);
            orderItemShipGroup.set("isGift", "Y");
            orderItemShipGroup.store();
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> createAlsoBoughtProductAssocs(DispatchContext dctx, Map<String, ? extends Object> context) {
        final Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        // All orders with an entryDate > orderEntryFromDateTime will be processed
        Timestamp orderEntryFromDateTime = (Timestamp) context.get("orderEntryFromDateTime");
        // If true all orders ever created will be processed and any pre-existing ALSO_BOUGHT ProductAssocs will be expired
        boolean processAllOrders = context.get("processAllOrders") == null ? false : (Boolean) context.get("processAllOrders");
        if (orderEntryFromDateTime == null && !processAllOrders) {
            // No from date supplied, check to see when this service last ran and use the startDateTime
            // FIXME: This code is unreliable - the JobSandbox value might have been purged. Use another mechanism to persist orderEntryFromDateTime.
            EntityCondition cond = EntityCondition.makeCondition(UtilMisc.toMap("statusId", "SERVICE_FINISHED", "serviceName", "createAlsoBoughtProductAssocs"));
            EntityFindOptions efo = new EntityFindOptions();
            efo.setMaxRows(1);
            try {
                GenericValue lastRunJobSandbox = EntityUtil.getFirst(delegator.findList("JobSandbox", cond, null, UtilMisc.toList("startDateTime DESC"), efo, false));
                if (lastRunJobSandbox != null) {
                    orderEntryFromDateTime = lastRunJobSandbox.getTimestamp("startDateTime");
                }
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
            }
            if (orderEntryFromDateTime == null) {
                // Still null, process all orders
                processAllOrders = true;
            }
        }
        if (processAllOrders) {
            // Expire any pre-existing ALSO_BOUGHT ProductAssocs in preparation for reprocessing
            EntityCondition cond = EntityCondition.makeCondition(UtilMisc.toList(
                    EntityCondition.makeCondition("productAssocTypeId", "ALSO_BOUGHT"),
                    EntityCondition.makeConditionDate("fromDate", "thruDate")
           ));
            try {
                delegator.storeByCondition("ProductAssoc", UtilMisc.toMap("thruDate", UtilDateTime.nowTimestamp()), cond);
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
            }
        }
        List<EntityExpr> orderCondList = UtilMisc.toList(EntityCondition.makeCondition("orderTypeId", "SALES_ORDER"));
        if (!processAllOrders && orderEntryFromDateTime != null) {
            orderCondList.add(EntityCondition.makeCondition("entryDate", EntityOperator.GREATER_THAN, orderEntryFromDateTime));
        }
        final EntityCondition cond = EntityCondition.makeCondition(orderCondList);
        List<String> orderIds;
        try {
            orderIds = TransactionUtil.doNewTransaction(() -> {
                List<String> oids = new LinkedList<>();

                EntityQuery eq = EntityQuery.use(delegator)
                        .select("orderId")
                        .from("OrderHeader")
                        .where(cond)
                        .orderBy("entryDate ASC");

                try (EntityListIterator eli = eq.queryIterator()) {
                    GenericValue orderHeader;
                    while ((orderHeader = eli.next()) != null) {
                        oids.add(orderHeader.getString("orderId"));
                    }
                }
                return oids;
            }, "getSalesOrderIds", 0, true);
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        for (String orderId: orderIds) {
            Map<String, Object> svcIn = new HashMap<>();
            svcIn.put("userLogin", context.get("userLogin"));
            svcIn.put("orderId", orderId);
            try {
                Map<String, Object> serviceResult = dispatcher.runSync("createAlsoBoughtProductAssocsForOrder", svcIn);
                if (ServiceUtil.isError(serviceResult)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                }
            } catch (GenericServiceException e) {
                Debug.logError(e, module);
            }
        }
        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> createAlsoBoughtProductAssocsForOrder(DispatchContext dctx, Map<String, ? extends Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        String orderId = (String) context.get("orderId");
        OrderReadHelper orh = new OrderReadHelper(delegator, orderId);
        List<GenericValue> orderItems = orh.getOrderItems();
        Map<String, Object> serviceResult = new HashMap<>();
        // In order to improve efficiency a little bit, we will always create the ProductAssoc records
        // with productId < productIdTo when the two are compared.  This way when checking for an existing
        // record we don't have to check both possible combinations of productIds
        Set<String> productIdSet = new TreeSet<>();
        if (orderItems != null) {
            for (GenericValue orderItem : orderItems) {
                String productId = orderItem.getString("productId");
                if (productId != null) {
                    GenericValue parentProduct = ProductWorker.getParentProduct(productId, delegator);
                    if (parentProduct != null) {
                        productId = parentProduct.getString("productId");
                    }
                    productIdSet.add(productId);
                }
            }
        }
        Set<String> productIdToSet = new TreeSet<>(productIdSet);
        for (String productId : productIdSet) {
            productIdToSet.remove(productId);
            for (String productIdTo : productIdToSet) {
                EntityCondition cond = EntityCondition.makeCondition(
                        UtilMisc.toList(
                                EntityCondition.makeCondition("productId", productId),
                                EntityCondition.makeCondition("productIdTo", productIdTo),
                                EntityCondition.makeCondition("productAssocTypeId", "ALSO_BOUGHT"),
                                EntityCondition.makeCondition("fromDate", EntityOperator.LESS_THAN_EQUAL_TO, UtilDateTime.nowTimestamp()),
                                EntityCondition.makeCondition("thruDate", null)
                       )
               );
                GenericValue existingProductAssoc = null;
                try {
                    // No point in using the cache because of the filterByDateExpr
                    existingProductAssoc = EntityQuery.use(delegator).from("ProductAssoc").where(cond).orderBy("fromDate DESC").queryFirst();
                } catch (GenericEntityException e) {
                    Debug.logError(e, module);
                }
                try {
                    if (existingProductAssoc != null) {
                        BigDecimal newQuantity = existingProductAssoc.getBigDecimal("quantity");
                        if (newQuantity == null || newQuantity.compareTo(BigDecimal.ZERO) < 0) {
                            newQuantity = BigDecimal.ZERO;
                        }
                        newQuantity = newQuantity.add(BigDecimal.ONE);
                        ModelService updateProductAssoc = dctx.getModelService("updateProductAssoc");
                        Map<String, Object> updateCtx = updateProductAssoc.makeValid(context, ModelService.IN_PARAM, true, null);
                        updateCtx.putAll(updateProductAssoc.makeValid(existingProductAssoc, ModelService.IN_PARAM));
                        updateCtx.put("quantity", newQuantity);
                        serviceResult = dispatcher.runSync("updateProductAssoc", updateCtx);
                        if (ServiceUtil.isError(serviceResult)) {
                            return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                        }
                    } else {
                        Map<String, Object> createCtx = new HashMap<>();
                        createCtx.put("userLogin", context.get("userLogin"));
                        createCtx.put("productId", productId);
                        createCtx.put("productIdTo", productIdTo);
                        createCtx.put("productAssocTypeId", "ALSO_BOUGHT");
                        createCtx.put("fromDate", UtilDateTime.nowTimestamp());
                        createCtx.put("quantity", BigDecimal.ONE);
                        serviceResult = dispatcher.runSync("createProductAssoc", createCtx);
                        if (ServiceUtil.isError(serviceResult)) {
                            return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                        }
                    }
                } catch (GenericServiceException e) {
                    Debug.logError(e, module);
                }
            }
        }

        return ServiceUtil.returnSuccess();
    }

    /**
     * This service runs when you update shipping method of Order from order view page.
     */
    public static Map<String, Object> updateShipGroupShipInfo(DispatchContext dctx, Map<String, ? extends Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get("locale");
        GenericValue userLogin  = (GenericValue)context.get("userLogin");
        String orderId = (String)context.get("orderId");
        String shipGroupSeqId = (String)context.get("shipGroupSeqId");
        String contactMechId = (String)context.get("contactMechId");
        String oldContactMechId = (String)context.get("oldContactMechId");
        String shipmentMethod = (String)context.get("shipmentMethod");

        //load cart from order to update new shipping method or address
        ShoppingCart shoppingCart = null;
        try {
            shoppingCart = loadCartForUpdate(dispatcher, delegator, userLogin, orderId);
        } catch(GeneralException e) {
            Debug.logError(e, module);
        }

        String message = null;
        if (UtilValidate.isNotEmpty(shipGroupSeqId)) {
            OrderReadHelper orh = new OrderReadHelper(delegator, orderId);
            List<GenericValue> shippingMethods = null;
            String shipmentMethodTypeId = null;
            String carrierPartyId = null;

            // get shipment method from OrderItemShipGroup, if not available in parameters
            if (UtilValidate.isNotEmpty(shipmentMethod)) {
                String[] arr = shipmentMethod.split( "@" );
                shipmentMethodTypeId = arr[0];
                carrierPartyId = arr[1];
            } else {
                GenericValue orderItemshipGroup = orh.getOrderItemShipGroup(shipGroupSeqId);
                shipmentMethodTypeId = orderItemshipGroup.getString("shipmentMethodTypeId");
                carrierPartyId = orderItemshipGroup.getString("carrierPartyId");
            }
            int groupIdx =Integer.parseInt(shipGroupSeqId);

            /* check whether new selected contact address is same as old contact.
               If contact address is different, get applicable ship methods for changed contact */
            if (UtilValidate.isNotEmpty(oldContactMechId) && oldContactMechId.equals(contactMechId)) {
                shoppingCart.setShipmentMethodTypeId(groupIdx - 1, shipmentMethodTypeId);
                shoppingCart.setCarrierPartyId(groupIdx - 1, carrierPartyId);
            } else {
                Map<String, BigDecimal> shippableItemFeatures = orh.getFeatureIdQtyMap(shipGroupSeqId);
                BigDecimal shippableTotal = orh.getShippableTotal(shipGroupSeqId);
                BigDecimal shippableWeight = orh.getShippableWeight(shipGroupSeqId);
                List<BigDecimal> shippableItemSizes = orh.getShippableSizes(shipGroupSeqId);

                GenericValue shippingAddress = orh.getShippingAddress(shipGroupSeqId);

                shippingMethods = ProductStoreWorker.getAvailableStoreShippingMethods(delegator, orh.getProductStoreId(),
                        shippingAddress, shippableItemSizes, shippableItemFeatures, shippableWeight, shippableTotal);

                boolean isShippingMethodAvailable = false;
                // search shipping method for ship group is applicable to new address or not.
                for (GenericValue shippingMethod : shippingMethods) {
                    isShippingMethodAvailable = shippingMethod.getString("partyId").equals(carrierPartyId) && shippingMethod.getString("shipmentMethodTypeId").equals(shipmentMethodTypeId);
                    if (isShippingMethodAvailable) {
                        shoppingCart.setShipmentMethodTypeId(groupIdx - 1, shipmentMethodTypeId);
                        shoppingCart.setCarrierPartyId(groupIdx - 1, carrierPartyId);
                        break;
                    }
                }

                // set first shipping method from list, if shipping method for ship group is not applicable to new ship address.
                if(!isShippingMethodAvailable) {
                    shoppingCart.setShipmentMethodTypeId(groupIdx - 1, shippingMethods.get(0).getString("shipmentMethodTypeId"));
                    shoppingCart.setCarrierPartyId(groupIdx - 1, shippingMethods.get(0).getString("carrierPartyId"));

                    String newShipMethTypeDesc =null;
                    String shipMethTypeDesc=null;
                    try {
                        shipMethTypeDesc = EntityQuery.use(delegator).from("ShipmentMethodType").where("shipmentMethodTypeId", shipmentMethodTypeId).queryOne().getString("description");
                        newShipMethTypeDesc = EntityQuery.use(delegator).from("ShipmentMethodType").where("shipmentMethodTypeId", shippingMethods.get(0).getString("shipmentMethodTypeId")).queryOne().getString("description");
                    } catch(GenericEntityException e) {
                        Debug.logError(e, module);
                    }
                    // message to notify user for not applicability of shipping method
                    message= "Shipping Method "+carrierPartyId+" "+shipMethTypeDesc+" is not applicable to shipping address. "+shippingMethods.get(0).getString("carrierPartyId")+" "+newShipMethTypeDesc+" has been set for shipping address.";
                }
                shoppingCart.setShippingContactMechId(groupIdx-1, contactMechId);
            }
        }

        // save cart after updating shipping method and shipping address.
        Map<String, Object> changeMap = new HashMap<>();
        try {
            saveUpdatedCartToOrder(dispatcher, delegator, shoppingCart, locale, userLogin, orderId, changeMap, true, false);
        } catch(GeneralException e) {
            Debug.logError(e, module);
        }

        if (UtilValidate.isNotEmpty(message)) {
            return ServiceUtil.returnSuccess(message);
        }
        return ServiceUtil.returnSuccess();
    }
    
    public static Map<String, Object> associateOrderWithAllocationPlans(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin  = (GenericValue)context.get("userLogin");
        String orderId = (String) context.get("orderId");
        Map<String, String> productPlanMap = new HashMap<>();
        Map<String, Object> serviceResult = new HashMap<>();
        try {
            String userLoginId = null;
            if (userLogin != null) {
                userLoginId = userLogin.getString("userLoginId");
            }
            //Get the list of associated products
            OrderReadHelper orderReadHelper = new OrderReadHelper(delegator, orderId);
            List<GenericValue> orderItems =  orderReadHelper.getOrderItems();
            Map<String, Object> serviceCtx = new HashMap<>();
            for (GenericValue orderItem : orderItems) {
                String orderItemSeqId = orderItem.getString("orderItemSeqId");
                String productId = orderItem.getString("productId");
                String planMethodEnumId = null;
                GenericValue orderItemAttribute = EntityQuery.use(delegator).from("OrderItemAttribute").where("orderId", orderId, "orderItemSeqId", orderItemSeqId, "attrName", "autoReserve").queryOne();
                if (orderItemAttribute != null && "true".equals(orderItemAttribute.getString("attrValue"))) {
                    planMethodEnumId = "AUTO";
                } else {
                    planMethodEnumId = "MANUAL";
                }
                //Look for any existing open allocation plan, if not available create a new one
                String planId = null;
                List<EntityCondition> headerConditions = new ArrayList<>();
                headerConditions.add(EntityCondition.makeCondition("productId", productId));
                headerConditions.add(EntityCondition.makeCondition("planTypeId", "SALES_ORD_ALLOCATION"));
                headerConditions.add(EntityCondition.makeCondition("statusId", EntityOperator.IN, UtilMisc.toList("ALLOC_PLAN_CREATED", "ALLOC_PLAN_APPROVED")));
                GenericValue allocationPlanHeader = EntityQuery.use(delegator).from("AllocationPlanHeader").where(headerConditions).queryFirst();
                if (allocationPlanHeader == null) {
                    planId = delegator.getNextSeqId("AllocationPlanHeader");
                    serviceCtx.put("planId", planId);
                    serviceCtx.put("productId", productId);
                    serviceCtx.put("planTypeId", "SALES_ORD_ALLOCATION");
                    serviceCtx.put("planName", "Allocation Plan For Product #"+productId);
                    serviceCtx.put("statusId", "ALLOC_PLAN_CREATED");
                    serviceCtx.put("createdByUserLogin", userLoginId);
                    serviceCtx.put("userLogin", userLogin);
                    serviceResult = dispatcher.runSync("createAllocationPlanHeader", serviceCtx);
                    if (ServiceUtil.isError(serviceResult)) {
                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                    }
                } else {
                    planId = allocationPlanHeader.getString("planId");
                    serviceCtx.put("planId", planId);
                    serviceCtx.put("productId", productId);
                    //If an item added to already approved plan, it should be moved to created status
                    serviceCtx.put("statusId", "ALLOC_PLAN_CREATED");
                    serviceCtx.put("lastModifiedByUserLogin", userLoginId);
                    serviceCtx.put("userLogin", userLogin);
                    serviceResult = dispatcher.runSync("updateAllocationPlanHeader", serviceCtx);
                    if (ServiceUtil.isError(serviceResult)) {
                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                    }
                }
                serviceCtx.clear();
                List<EntityCondition> itemConditions = new ArrayList<>();
                itemConditions.add(EntityCondition.makeCondition("planId", planId));
                itemConditions.add(EntityCondition.makeCondition("orderId", orderId));
                itemConditions.add(EntityCondition.makeCondition("orderItemSeqId", EntityOperator.EQUALS, orderItemSeqId));
                itemConditions.add(EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, "ALLOC_PLAN_ITEM_CRTD"));
                GenericValue allocationPlanItem = EntityQuery.use(delegator).from("AllocationPlanItem").where(itemConditions).queryFirst();
                String planItemSeqId = null;
                if (allocationPlanItem == null) {
                    //Add the orderItem to the allocation plan
                    serviceCtx.put("planId", planId);
                    serviceCtx.put("planMethodEnumId", planMethodEnumId);
                    if ("AUTO".equals(planMethodEnumId)) {
                        serviceCtx.put("allocatedQuantity", orderItem.getBigDecimal("quantity"));
                        serviceCtx.put("statusId", "ALLOC_PLAN_ITEM_APRV");
                    } else {
                        serviceCtx.put("statusId", "ALLOC_PLAN_ITEM_CRTD");
                    }
                    serviceCtx.put("orderId", orderId);
                    serviceCtx.put("orderItemSeqId", orderItemSeqId);
                    serviceCtx.put("productId", productId);
                    serviceCtx.put("createdByUserLogin", userLoginId);
                    serviceCtx.put("userLogin", userLogin);
                    serviceResult = dispatcher.runSync("createAllocationPlanItem", serviceCtx);
                    if (ServiceUtil.isError(serviceResult)) {
                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                    }
                    planItemSeqId = (String) serviceResult.get("planItemSeqId");
                } else {
                    planItemSeqId = allocationPlanItem.getString("planItemSeqId");
                    serviceCtx.put("planId", planId);
                    serviceCtx.put("planItemSeqId", planItemSeqId);
                    serviceCtx.put("productId", productId);
                    serviceCtx.put("planMethodEnumId", planMethodEnumId);
                    if ("AUTO".equals(planMethodEnumId)) {
                        serviceCtx.put("allocatedQuantity", orderItem.getBigDecimal("quantity"));
                        serviceCtx.put("statusId", "ALLOC_PLAN_ITEM_APRV");
                    } else {
                        serviceCtx.put("statusId", "ALLOC_PLAN_ITEM_CRTD");
                    }
                    serviceCtx.put("lastModifiedByUserLogin", userLoginId);
                    serviceCtx.put("userLogin", userLogin);
                    serviceResult = dispatcher.runSync("updateAllocationPlanItem", serviceCtx);
                    if (ServiceUtil.isError(serviceResult)) {
                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                    }
                }
                serviceCtx.clear();

                //Enter the values in productPlanMap
                productPlanMap.put(productId, planId);
            }
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(e.getMessage());
        } catch (GenericServiceException e) {
            return ServiceUtil.returnError(e.getMessage());
        }
        serviceResult.clear();
        serviceResult.put("productPlanMap", productPlanMap);
        return serviceResult;
    }

    public static Map<String, Object> approveAllocationPlanItems(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin  = (GenericValue)context.get("userLogin");
        String planId = (String) context.get("planId");
        Map<String, Object> serviceCtx = new HashMap<>();
        Map<String, Object> serviceResult = new HashMap<>();
        try {
            String userLoginId = null;
            if (userLogin != null) {
                userLoginId = userLogin.getString("userLoginId");
            }
            //Get the list of plan items in created status
            List<EntityCondition> itemConditions = new ArrayList<>();
            itemConditions.add(EntityCondition.makeCondition("planId", planId));
            itemConditions.add(EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, "ALLOC_PLAN_ITEM_CRTD"));
            List<GenericValue> allocationPlanItems = EntityQuery.use(delegator).from("AllocationPlanItem").where(itemConditions).queryList();

            String facilityId = null;
            String productId = null;
            for (GenericValue allocationPlanItem : allocationPlanItems) {
                productId = allocationPlanItem.getString("productId");
                serviceCtx.put("planId", planId);
                serviceCtx.put("planItemSeqId", allocationPlanItem.getString("planItemSeqId"));
                serviceCtx.put("productId", allocationPlanItem.getString("productId"));
                serviceCtx.put("statusId", "ALLOC_PLAN_ITEM_APRV");
                serviceCtx.put("lastModifiedByUserLogin", userLoginId);
                serviceCtx.put("userLogin", userLogin);
                serviceResult = dispatcher.runSync("updateAllocationPlanItem", serviceCtx);
                if (ServiceUtil.isError(serviceResult)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                }
                serviceCtx.clear();
                GenericValue orderHeader = EntityQuery.use(delegator).from("OrderHeader").where("orderId", allocationPlanItem.getString("orderId")).queryOne();
                if (orderHeader != null) {
                    //For now considering single facility case only
                    facilityId = orderHeader.getString("originFacilityId");
                }
            }

            //Get the list of plan items in approved status
            itemConditions.clear();
            itemConditions.add(EntityCondition.makeCondition("planId", planId));
            itemConditions.add(EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, "ALLOC_PLAN_ITEM_APRV"));
            allocationPlanItems = EntityQuery.use(delegator).from("AllocationPlanItem").where(itemConditions).queryList();

            if (allocationPlanItems.size() > 0) {
                //Rereserve the inventory
                serviceCtx.put("productId", productId);
                serviceCtx.put("facilityId", facilityId);
                serviceCtx.put("allocationPlanAndItemList", allocationPlanItems);
                serviceCtx.put("userLogin", userLogin);
                serviceResult = dispatcher.runSync("reassignInventoryReservationsByAllocationPlan", serviceCtx);
                if (ServiceUtil.isError(serviceResult)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                }
            }

        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(e.getMessage());
        } catch (GenericServiceException e) {
            return ServiceUtil.returnError(e.getMessage());
        }
        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> cancelAllocationPlanItems(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin  = (GenericValue)context.get("userLogin");
        String planId = (String) context.get("planId");
        Map<String, Object> serviceCtx = new HashMap<>();
        Map<String, Object> serviceResult = new HashMap<>();
        try {
            String userLoginId = null;
            if (userLogin != null) {
                userLoginId = userLogin.getString("userLoginId");
            }
            //Get the list of plan items
            List<EntityCondition> itemConditions = new ArrayList<>();
            itemConditions.add(EntityCondition.makeCondition("planId", planId));
            itemConditions.add(EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "ALLOC_PLAN_ITEM_CNCL"));
            List<GenericValue> allocationPlanItems = EntityQuery.use(delegator).from("AllocationPlanItem").where(itemConditions).queryList();

            for (GenericValue allocationPlanItem : allocationPlanItems) {
                serviceCtx.put("planId", planId);
                serviceCtx.put("planItemSeqId", allocationPlanItem.getString("planItemSeqId"));
                serviceCtx.put("productId", allocationPlanItem.getString("productId"));
                serviceCtx.put("statusId", "ALLOC_PLAN_ITEM_CNCL");
                serviceCtx.put("lastModifiedByUserLogin", userLoginId);
                serviceCtx.put("userLogin", userLogin);
                serviceResult = dispatcher.runSync("updateAllocationPlanItem", serviceCtx);
                if (ServiceUtil.isError(serviceResult)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                }
                serviceCtx.clear();
            }
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(e.getMessage());
        } catch (GenericServiceException e) {
            return ServiceUtil.returnError(e.getMessage());
        }
        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> changeAllocationPlanStatus(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin  = (GenericValue)context.get("userLogin");
        Locale locale = (Locale) context.get("locale");
        String planId = (String) context.get("planId");
        String statusId = (String) context.get("statusId");
        String oldStatusId = null;
        Map<String, Object> serviceResult = new HashMap<>();
        try {
            List<GenericValue> allocationPlanHeaders = EntityQuery.use(delegator).from("AllocationPlanHeader").where("planId", planId).queryList();
            if (allocationPlanHeaders == null || UtilValidate.isEmpty(allocationPlanHeaders)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                        "OrderErrorAllocationPlanIsNotAvailable", locale) + ": [" + planId + "]");
            }
            for (GenericValue allocationPlanHeader : allocationPlanHeaders) {
                oldStatusId = allocationPlanHeader.getString("statusId");
                try {
                    GenericValue statusChange = EntityQuery.use(delegator).from("StatusValidChange").where("statusId", oldStatusId, "statusIdTo", statusId).queryOne();
                    if (statusChange == null) {
                        return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                                "OrderErrorCouldNotChangeAllocationPlanStatusStatusIsNotAValidChange", locale) + ": [" + oldStatusId + "] -> [" + statusId + "]");
                    }
                } catch (GenericEntityException e) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                            "OrderErrorCouldNotChangeAllocationPlanStatus",locale) + e.getMessage() + ").");
                }
                String userLoginId = null;
                if (userLogin != null) {
                    userLoginId = userLogin.getString("userLoginId");
                }
                Map<String, Object> serviceCtx = new HashMap<>();
                serviceCtx.put("planId", planId);
                serviceCtx.put("productId", allocationPlanHeader.getString("productId"));
                serviceCtx.put("statusId", statusId);
                serviceCtx.put("lastModifiedByUserLogin", userLoginId);
                serviceCtx.put("userLogin", userLogin);
                serviceResult = dispatcher.runSync("updateAllocationPlanHeader", serviceCtx);
                if (ServiceUtil.isError(serviceResult)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                }
            }
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(e.getMessage());
        } catch (GenericServiceException e) {
            return ServiceUtil.returnError(e.getMessage());
        }
        serviceResult.clear();
        serviceResult.put("planId", planId);
        serviceResult.put("oldStatusId", oldStatusId);
        return serviceResult;
    }

    public static Map<String, Object> changeAllocationPlanItemStatus(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin  = (GenericValue)context.get("userLogin");
        Locale locale = (Locale) context.get("locale");
        String planId = (String) context.get("planId");
        String planItemSeqId = (String) context.get("planItemSeqId");
        String statusId = (String) context.get("statusId");
        String oldStatusId = null;
        Map<String, Object> serviceResult = new HashMap<>();
        try {
            GenericValue allocationPlanItem = EntityQuery.use(delegator).from("AllocationPlanItem").where("planId", planId, "planItemSeqId", planItemSeqId).queryOne();
            if (allocationPlanItem == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                        "OrderErrorAllocationPlanIsNotAvailable", locale) + ": [" + planId + ":"+ planItemSeqId + "]");
            }
            oldStatusId = allocationPlanItem.getString("statusId");
            try {
                GenericValue statusChange = EntityQuery.use(delegator).from("StatusValidChange").where("statusId", oldStatusId, "statusIdTo", statusId).queryOne();
                if (statusChange == null) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                            "OrderErrorCouldNotChangeAllocationPlanItemStatusStatusIsNotAValidChange", locale) + ": [" + oldStatusId + "] -> [" + statusId + "]");
                }
            } catch (GenericEntityException e) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                        "OrderErrorCouldNotChangeAllocationPlanItemStatus",locale) + e.getMessage() + ").");
            }
            String userLoginId = null;
            if (userLogin != null) {
                userLoginId = userLogin.getString("userLoginId");
            }
            Map<String, Object> serviceCtx = new HashMap<>();
            serviceCtx.put("planId", planId);
            serviceCtx.put("planItemSeqId", planItemSeqId);
            serviceCtx.put("productId", allocationPlanItem.getString("productId"));
            serviceCtx.put("statusId", statusId);
            serviceCtx.put("lastModifiedByUserLogin", userLoginId);
            serviceCtx.put("userLogin", userLogin);
            serviceResult = dispatcher.runSync("updateAllocationPlanItem", serviceCtx);
            if (ServiceUtil.isError(serviceResult)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
            }
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(e.getMessage());
        } catch (GenericServiceException e) {
            return ServiceUtil.returnError(e.getMessage());
        }
        serviceResult.clear();
        serviceResult.put("oldStatusId", oldStatusId);
        return serviceResult;
    }

    public static Map<String, Object> completeAllocationPlanItemByOrderItem(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin  = (GenericValue)context.get("userLogin");
        String orderId = (String) context.get("orderId");
        String orderItemSeqId = (String) context.get("orderItemSeqId");
        try {
            List<EntityExpr> exprs = new ArrayList<>();
            exprs.add(EntityCondition.makeCondition("orderId", orderId));
            if (orderItemSeqId != null) {
                exprs.add(EntityCondition.makeCondition("orderItemSeqId", orderItemSeqId));
            } else {
                exprs.add(EntityCondition.makeCondition("statusId", EntityOperator.NOT_IN, UtilMisc.toList("ALLOC_PLAN_ITEM_CMPL", "ALLOC_PLAN_ITEM_CNCL")));
            }

            List<GenericValue> allocationPlanItems = EntityQuery.use(delegator).from("AllocationPlanItem").where(exprs).queryList();
            if (allocationPlanItems == null || UtilValidate.isEmpty(allocationPlanItems)) {
                return ServiceUtil.returnSuccess();
            }
            List<String> planIds = new ArrayList<>();
            Map<String, Object> serviceCtx = new HashMap<>();
            Map<String, Object> serviceResult = new HashMap<>();
            for (GenericValue allocationPlanItem : allocationPlanItems) {
                String planId = allocationPlanItem.getString("planId");
                serviceCtx.put("planId", planId);
                serviceCtx.put("planItemSeqId", allocationPlanItem.getString("planItemSeqId"));
                serviceCtx.put("statusId", "ALLOC_PLAN_ITEM_CMPL");
                serviceCtx.put("userLogin", userLogin);
                serviceResult = dispatcher.runSync("changeAllocationPlanItemStatus", serviceCtx);
                if (ServiceUtil.isError(serviceResult)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                }
                if (!planIds.contains(planId)) {
                    planIds.add(planId);
                }
                serviceCtx.clear();
                serviceResult.clear();
            }
            //If all the allocation plan items are completed then complete the allocation plan header(s) as well
            for (String planId : planIds) {
                allocationPlanItems = EntityQuery.use(delegator).from("AllocationPlanItem").where("planId", planId).queryList();
                Boolean completeAllocationPlan = true;
                for (GenericValue allocationPlanItem : allocationPlanItems) {
                    if (!"ALLOC_PLAN_ITEM_CMPL".equals(allocationPlanItem.getString("statusId"))) {
                        completeAllocationPlan = false;
                        break;
                    }
                }
                if (completeAllocationPlan) {
                    serviceCtx.put("planId", planId);
                    serviceCtx.put("statusId", "ALLOC_PLAN_COMPLETED");
                    serviceCtx.put("userLogin", userLogin);
                    serviceResult = dispatcher.runSync("changeAllocationPlanStatus", serviceCtx);
                    if (ServiceUtil.isError(serviceResult)) {
                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                    }
                    serviceCtx.clear();
                    serviceResult.clear();
                }
            }
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(e.getMessage());
        } catch (GenericServiceException e) {
            return ServiceUtil.returnError(e.getMessage());
        }
        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> cancelAllocationPlanItemByOrderItem(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin  = (GenericValue)context.get("userLogin");
        String orderId = (String) context.get("orderId");
        String orderItemSeqId = (String) context.get("orderItemSeqId");
        try {
            List<EntityExpr> exprs = new ArrayList<>();
            exprs.add(EntityCondition.makeCondition("orderId", orderId));
            if (orderItemSeqId != null) {
                exprs.add(EntityCondition.makeCondition("orderItemSeqId", orderItemSeqId));
            } else {
                exprs.add(EntityCondition.makeCondition("statusId", EntityOperator.NOT_IN, UtilMisc.toList("ALLOC_PLAN_ITEM_CMPL", "ALLOC_PLAN_ITEM_CNCL")));
            }

            List<GenericValue> allocationPlanItems = EntityQuery.use(delegator).from("AllocationPlanItem").where(exprs).queryList();
            if (allocationPlanItems == null || UtilValidate.isEmpty(allocationPlanItems)) {
                return ServiceUtil.returnSuccess();
            }
            List<String> planIds = new ArrayList<>();
            Map<String, Object> serviceCtx = new HashMap<>();
            Map<String, Object> serviceResult = new HashMap<>();
            for (GenericValue allocationPlanItem : allocationPlanItems) {
                String planId = allocationPlanItem.getString("planId");
                serviceCtx.put("planId", planId);
                serviceCtx.put("planItemSeqId", allocationPlanItem.getString("planItemSeqId"));
                serviceCtx.put("statusId", "ALLOC_PLAN_ITEM_CNCL");
                serviceCtx.put("userLogin", userLogin);
                serviceResult = dispatcher.runSync("changeAllocationPlanItemStatus", serviceCtx);
                if (ServiceUtil.isError(serviceResult)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                }
                if (!planIds.contains(planId)) {
                    planIds.add(planId);
                }
                serviceCtx.clear();
                serviceResult.clear();
            }
            //If all the allocation plan items are cancelled then cancel the allocation plan header(s) as well.
            for (String planId : planIds) {
                allocationPlanItems = EntityQuery.use(delegator).from("AllocationPlanItem").where("planId", planId).queryList();
                Boolean cancelAllocationPlan = true;
                for (GenericValue allocationPlanItem : allocationPlanItems) {
                    if (!"ALLOC_PLAN_ITEM_CNCL".equals(allocationPlanItem.getString("statusId"))) {
                        cancelAllocationPlan = false;
                        break;
                    }
                }
                if (cancelAllocationPlan) {
                    serviceCtx.put("planId", planId);
                    serviceCtx.put("statusId", "ALLOC_PLAN_CANCELLED");
                    serviceCtx.put("userLogin", userLogin);
                    serviceResult = dispatcher.runSync("changeAllocationPlanStatus", serviceCtx);
                    if (ServiceUtil.isError(serviceResult)) {
                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                    }
                    serviceCtx.clear();
                    serviceResult.clear();
                }
            }
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(e.getMessage());
        } catch (GenericServiceException e) {
            return ServiceUtil.returnError(e.getMessage());
        }
        return ServiceUtil.returnSuccess();
    }
    public static Map<String, Object> updateAllocatedQuantityOnOrderItemChange(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin  = (GenericValue)context.get("userLogin");
        String orderId = (String) context.get("orderId");
        try {
            OrderReadHelper orderReadHelper = new OrderReadHelper(delegator, orderId);
            List<GenericValue> orderItems = orderReadHelper.getOrderItems();
            for (GenericValue orderItem : orderItems) {
                String orderItemSeqId = orderItem.getString("orderItemSeqId");
                List<EntityExpr> exprs = new ArrayList<>();
                exprs.add(EntityCondition.makeCondition("orderId", orderId));
                exprs.add(EntityCondition.makeCondition("orderItemSeqId", orderItemSeqId));
                exprs.add(EntityCondition.makeCondition("changeTypeEnumId", "ODR_ITM_UPDATE"));
                exprs.add(EntityCondition.makeCondition("quantity", EntityOperator.NOT_EQUAL, null));
                GenericValue orderItemChange = EntityQuery.use(delegator).from("OrderItemChange").where(exprs).orderBy("-changeDatetime").queryFirst();
                if (orderItemChange != null) {
                    BigDecimal quantityChanged = orderItemChange.getBigDecimal("quantity");
                    if (quantityChanged.compareTo(BigDecimal.ZERO) < 0) {
                        GenericValue allocationPlanItem = EntityQuery.use(delegator).from("AllocationPlanItem").where("orderId", orderId, "orderItemSeqId", orderItemSeqId, "statusId", "ALLOC_PLAN_ITEM_CRTD", "productId", orderItem.getString("productId")).queryFirst();
                        if (allocationPlanItem != null) {
                            BigDecimal revisedQuantity = orderItem.getBigDecimal("quantity");
                            BigDecimal allocatedQuantity = allocationPlanItem.getBigDecimal("allocatedQuantity");
                            if (allocatedQuantity != null && allocatedQuantity.compareTo(revisedQuantity) > 0) {
                                //Allocated Quantity is more than the revisedQuantity quantity, reduce it and release the excess reservations
                                Map<String, Object> serviceCtx = new HashMap<>();
                                serviceCtx.put("planId", allocationPlanItem.getString("planId"));
                                serviceCtx.put("planItemSeqId", allocationPlanItem.getString("planItemSeqId"));
                                serviceCtx.put("productId", allocationPlanItem.getString("productId"));
                                serviceCtx.put("allocatedQuantity", revisedQuantity);
                                serviceCtx.put("lastModifiedByUserLogin", userLogin.getString("userLoginId"));
                                serviceCtx.put("userLogin", userLogin);
                                Map<String, Object> serviceResult = dispatcher.runSync("updateAllocationPlanItem", serviceCtx);
                                if (ServiceUtil.isError(serviceResult)) {
                                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                                }
                            }
                        }
                    }
                }
            }
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(e.getMessage());
        } catch (GenericServiceException e) {
            return ServiceUtil.returnError(e.getMessage());
        }
        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> createAllocationPlanAndItems(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin  = (GenericValue)context.get("userLogin");
        Locale locale = (Locale) context.get("locale");
        String productId = (String) context.get("productId");
        Integer itemListSize = (Integer) context.get("itemListSize");
        Map<String, String> itemOrderIdMap = UtilGenerics.cast(context.get("itemOrderIdMap"));
        Map<String, String> itemOrderItemSeqIdMap = UtilGenerics.cast(context.get("itemOrderItemSeqIdMap"));
        Map<String, String> itemAllocatedQuantityMap = UtilGenerics.cast(context.get("itemAllocatedQuantityMap"));
        Map<String, Object> serviceResult = new HashMap<>();
        Map<String, Object> serviceCtx = new HashMap<>();
        String planId = null;

        try {
            if (itemListSize > 0) {
                String userLoginId = userLogin.getString("userLoginId");
                //Create Allocation Plan Header
                serviceCtx = new HashMap<>();
                planId = delegator.getNextSeqId("AllocationPlanHeader");
                serviceCtx.put("planId", planId);
                serviceCtx.put("productId", productId);
                serviceCtx.put("planTypeId", "SALES_ORD_ALLOCATION");
                serviceCtx.put("statusId", "ALLOC_PLAN_CREATED");
                serviceCtx.put("createdByUserLogin", userLoginId);
                serviceCtx.put("userLogin", userLogin);
                serviceResult = dispatcher.runSync("createAllocationPlanHeader", serviceCtx);
                if (ServiceUtil.isError(serviceResult)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                }
                serviceCtx.clear();

                //Create Allocation Plan Items
                for (int i=0; i<itemListSize; i++) {
                    String orderId = itemOrderIdMap.get(String.valueOf(i));
                    String orderItemSeqId = itemOrderItemSeqIdMap.get(String.valueOf(i));
                    String allocatedQuantityStr = itemAllocatedQuantityMap.get(String.valueOf(i));
                    BigDecimal allocatedQuantity = null;
                    if (allocatedQuantityStr != null) {
                        try {
                            allocatedQuantity = (BigDecimal) ObjectType.simpleTypeOrObjectConvert(allocatedQuantityStr, "BigDecimal", null, locale);
                        } catch (GeneralException e) {
                            return ServiceUtil.returnError(e.getMessage());
                        }
                    }
                    serviceCtx.put("planId", planId);
                    serviceCtx.put("statusId", "ALLOC_PLAN_ITEM_CRTD");
                    serviceCtx.put("planMethodEnumId", "MANUAL");
                    serviceCtx.put("orderId", orderId);
                    serviceCtx.put("orderItemSeqId", orderItemSeqId);
                    serviceCtx.put("productId", productId);
                    serviceCtx.put("allocatedQuantity", allocatedQuantity);
                    serviceCtx.put("prioritySeqId", String.valueOf(i+1));
                    serviceCtx.put("createdByUserLogin", userLoginId);
                    serviceCtx.put("userLogin", userLogin);
                    serviceResult = dispatcher.runSync("createAllocationPlanItem", serviceCtx);
                    if (ServiceUtil.isError(serviceResult)) {
                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                    }
                    serviceCtx.clear();
                }
            } else {
                return ServiceUtil.returnError("Item list is empty.");
            }
        } catch (GenericServiceException e) {
            return ServiceUtil.returnError(e.getMessage());
        }
        serviceResult.clear();
        serviceResult.put("planId", planId);
        return serviceResult;
    }

    public static Map<String, Object> isInventoryAllocationRequired(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        Boolean allocateInventory = false;
        Map<String, Object> serviceContext = UtilGenerics.cast(context.get("serviceContext"));
        String orderId = (String) serviceContext.get("orderId");

        OrderReadHelper orderReadHelper = new OrderReadHelper(delegator, orderId);
        GenericValue productStore = orderReadHelper.getProductStore();
        if (productStore != null && "Y".equals(productStore.getString("allocateInventory"))) {
            allocateInventory = true;
        }
        Map<String, Object> serviceResult = new HashMap<>();
        serviceResult.put("conditionReply", allocateInventory);
        return serviceResult;
    }

    public static Map<String, Object> updateAllocationPlanItems(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");
        String planId = (String) context.get("planId");
        Map<String, String> orderIdMap = UtilGenerics.cast(context.get("orderIdMap"));
        Map<String, String> productIdMap = UtilGenerics.cast(context.get("productIdMap"));
        Map<String, String> allocatedQuantityMap = UtilGenerics.cast(context.get("allocatedQuantityMap"));
        Map<String, String> prioritySeqIdMap = UtilGenerics.cast(context.get("prioritySeqIdMap"));
        Map<String, String> rowSubmitMap = UtilGenerics.cast(context.get("rowSubmitMap"));
        Map<String, Object> serviceCtx = new HashMap<>();
        Map<String, Object> serviceResult = new HashMap<>();
        Boolean changeHeaderStatus = false;
        String productId = null;
        String facilityId = null;
        List<String> reReservePlanItemSeqIdList = new ArrayList<>();

        try {
            for (String planItemSeqId : productIdMap.keySet()) {
                String rowSubmit = rowSubmitMap.get(planItemSeqId);
                productId = productIdMap.get(planItemSeqId);
                String prioritySeqId = prioritySeqIdMap.get(planItemSeqId);
                String allocatedQuantityStr = allocatedQuantityMap.get(planItemSeqId);
                BigDecimal allocatedQuantity = null;
                if (allocatedQuantityStr != null) {
                    try {
                        allocatedQuantity = (BigDecimal) ObjectType.simpleTypeOrObjectConvert(allocatedQuantityStr, "BigDecimal", null, locale);
                    } catch (GeneralException e) {
                        return ServiceUtil.returnError(e.getMessage());
                    }
                }

                GenericValue allocationPlanItem = EntityQuery.use(delegator).from("AllocationPlanItem").where("planId", planId, "planItemSeqId", planItemSeqId, "productId", productId).queryOne();
                if (allocationPlanItem != null) {
                    BigDecimal oldAllocatedQuantity = allocationPlanItem.getBigDecimal("allocatedQuantity");
                    if (oldAllocatedQuantity != null && oldAllocatedQuantity.compareTo(allocatedQuantity) > 0) {
                        reReservePlanItemSeqIdList.add(planItemSeqId);
                    }

                    serviceCtx.put("planId", planId);
                    serviceCtx.put("prioritySeqId", prioritySeqId);
                    serviceCtx.put("planItemSeqId", planItemSeqId);
                    serviceCtx.put("productId", productId);
                    serviceCtx.put("lastModifiedByUserLogin", userLogin.getString("userLoginId"));
                    serviceCtx.put("userLogin", userLogin);
                    if (rowSubmit != null && "Y".equals(rowSubmit)) {
                        serviceCtx.put("allocatedQuantity", allocatedQuantity);
                        serviceCtx.put("planMethodEnumId", "MANUAL");
                        serviceCtx.put("statusId", "ALLOC_PLAN_ITEM_CRTD");
                        changeHeaderStatus = true;
                    }
                    serviceResult = dispatcher.runSync("updateAllocationPlanItem", serviceCtx);
                    if (ServiceUtil.isError(serviceResult)) {
                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                    }
                    serviceCtx.clear();
                }

                //Get the facility id from order id
                String orderId = orderIdMap.get(planItemSeqId);
                GenericValue orderHeader = EntityQuery.use(delegator).from("OrderHeader").where("orderId", orderId).queryOne();
                if (orderHeader != null && facilityId == null) {
                    facilityId = orderHeader.getString("originFacilityId");
                }
            }

            //If any item got updated, change the header status to created
            if (changeHeaderStatus) {
                serviceCtx.put("planId", planId);
                serviceCtx.put("productId", productId);
                serviceCtx.put("statusId", "ALLOC_PLAN_CREATED");
                serviceCtx.put("lastModifiedByUserLogin", userLogin.getString("userLoginId"));
                serviceCtx.put("userLogin", userLogin);
                serviceResult = dispatcher.runSync("updateAllocationPlanHeader", serviceCtx);
                if (ServiceUtil.isError(serviceResult)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                }
                serviceCtx.clear();
            }

            //Get the list of plan items in the created status
            List<EntityCondition> itemConditions = new ArrayList<>();
            itemConditions.add(EntityCondition.makeCondition("planId", planId));
            itemConditions.add(EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, "ALLOC_PLAN_ITEM_CRTD"));
            itemConditions.add(EntityCondition.makeCondition("planItemSeqId", EntityOperator.IN, reReservePlanItemSeqIdList));
            List<GenericValue> allocationPlanItems = EntityQuery.use(delegator).from("AllocationPlanItem").where(itemConditions).queryList();

            if (allocationPlanItems.size() > 0) {
                //Rereserve the inventory
                serviceCtx.put("productId", productId);
                serviceCtx.put("facilityId", facilityId);
                serviceCtx.put("allocationPlanAndItemList", allocationPlanItems);
                serviceCtx.put("userLogin", userLogin);
                serviceResult = dispatcher.runSync("reassignInventoryReservationsByAllocationPlan", serviceCtx);
                if (ServiceUtil.isError(serviceResult)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                }
            }
        } catch (GenericServiceException gse) {
            return ServiceUtil.returnError(gse.getMessage());
        } catch (GenericEntityException gee) {
            return ServiceUtil.returnError(gee.getMessage());
        }
        return ServiceUtil.returnSuccess();
    }
}
