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
package org.ofbiz.order.order;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.transaction.Transaction;

import javolution.util.FastList;
import javolution.util.FastMap;
import javolution.util.FastSet;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.GeneralRuntimeException;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilFormatOut;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilNumber;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.collections.ResourceBundleMapWrapper;
import org.ofbiz.common.CommonWorkers;
import org.ofbiz.common.DataModelConstants;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntity;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityConditionList;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.transaction.GenericTransactionException;
import org.ofbiz.entity.transaction.TransactionUtil;
import org.ofbiz.entity.util.EntityFindOptions;
import org.ofbiz.entity.util.EntityListIterator;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.order.shoppingcart.CartItemModifyException;
import org.ofbiz.order.shoppingcart.CheckOutHelper;
import org.ofbiz.order.shoppingcart.ItemNotFoundException;
import org.ofbiz.order.shoppingcart.ShoppingCart;
import org.ofbiz.order.shoppingcart.ShoppingCartItem;
import org.ofbiz.order.shoppingcart.product.ProductPromoWorker;
import org.ofbiz.order.shoppingcart.shipping.ShippingEvents;
import org.ofbiz.party.contact.ContactHelper;
import org.ofbiz.party.contact.ContactMechWorker;
import org.ofbiz.party.party.PartyWorker;
import org.ofbiz.product.product.ProductWorker;
import org.ofbiz.product.store.ProductStoreWorker;
import org.ofbiz.security.Security;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ModelService;
import org.ofbiz.service.ServiceUtil;

import com.ibm.icu.util.Calendar;

/**
 * Order Processing Services
 */

public class OrderServices {

    public static final String module = OrderServices.class.getName();
    public static final String resource = "OrderUiLabels";
    public static final String resource_error = "OrderErrorUiLabels";

    public static Map<String, String> salesAttributeRoleMap = FastMap.newInstance();
    public static Map<String, String> purchaseAttributeRoleMap = FastMap.newInstance();
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
    public static final int taxRounding = UtilNumber.getBigDecimalRoundingMode("salestax.rounding");
    public static final int orderDecimals = UtilNumber.getBigDecimalScale("order.decimals");
    public static final int orderRounding = UtilNumber.getBigDecimalRoundingMode("order.rounding");
    public static final BigDecimal ZERO = BigDecimal.ZERO.setScale(taxDecimals, taxRounding);


    private static boolean hasPermission(String orderId, GenericValue userLogin, String action, Security security, Delegator delegator) {
        OrderReadHelper orh = new OrderReadHelper(delegator, orderId);
        String orderTypeId = orh.getOrderTypeId();
        String partyId = null;
        GenericValue orderParty = orh.getEndUserParty();
        if (UtilValidate.isEmpty(orderParty)) {
            orderParty = orh.getPlacingParty();
        }
        if (UtilValidate.isNotEmpty(orderParty)) {
            partyId = orderParty.getString("partyId");
        }
        boolean hasPermission = hasPermission(orderTypeId, partyId, userLogin, action, security);
        if (!hasPermission) {
            GenericValue placingCustomer = null;
            try {
                Map placingCustomerFields = UtilMisc.toMap("orderId", orderId, "partyId", userLogin.getString("partyId"), "roleTypeId", "PLACING_CUSTOMER");
                placingCustomer = delegator.findByPrimaryKey("OrderRole", placingCustomerFields);
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
            if (orderTypeId.equals("SALES_ORDER")) {
                if (security.hasEntityPermission("ORDERMGR", "_SALES_" + action, userLogin)) {
                    hasPermission = true;
                } else {
                    // check sales agent/customer relationship
                    List<GenericValue> repsCustomers = new LinkedList<GenericValue>();
                    try {
                        repsCustomers = EntityUtil.filterByDate(userLogin.getRelatedOne("Party").getRelatedByAnd("FromPartyRelationship",
                                UtilMisc.toMap("roleTypeIdFrom", "AGENT", "roleTypeIdTo", "CUSTOMER", "partyIdTo", partyId)));
                    } catch (GenericEntityException ex) {
                        Debug.logError("Could not determine if " + partyId + " is a customer of user " + userLogin.getString("userLoginId") + " due to " + ex.getMessage(), module);
                    }
                    if ((repsCustomers != null) && (repsCustomers.size() > 0) && (security.hasEntityPermission("ORDERMGR", "_ROLE_" + action, userLogin))) {
                        hasPermission = true;
                    }
                    if (!hasPermission) {
                        // check sales sales rep/customer relationship
                        try {
                            repsCustomers = EntityUtil.filterByDate(userLogin.getRelatedOne("Party").getRelatedByAnd("FromPartyRelationship",
                                    UtilMisc.toMap("roleTypeIdFrom", "SALES_REP", "roleTypeIdTo", "CUSTOMER", "partyIdTo", partyId)));
                        } catch (GenericEntityException ex) {
                            Debug.logError("Could not determine if " + partyId + " is a customer of user " + userLogin.getString("userLoginId") + " due to " + ex.getMessage(), module);
                        }
                        if ((repsCustomers != null) && (repsCustomers.size() > 0) && (security.hasEntityPermission("ORDERMGR", "_ROLE_" + action, userLogin))) {
                            hasPermission = true;
                        }
                    }
                }
            } else if ((orderTypeId.equals("PURCHASE_ORDER") && (security.hasEntityPermission("ORDERMGR", "_PURCHASE_" + action, userLogin)))) {
                hasPermission = true;
            }
        }
        return hasPermission;
    }
    /** Service for creating a new order */
    public static Map createOrder(DispatchContext ctx, Map context) {
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        Security security = ctx.getSecurity();
        List toBeStored = new LinkedList();
        Locale locale = (Locale) context.get("locale");
        Map successResult = ServiceUtil.returnSuccess();

        GenericValue userLogin = (GenericValue) context.get("userLogin");
        // get the order type
        String orderTypeId = (String) context.get("orderTypeId");
        String partyId = (String) context.get("partyId");
        String billFromVendorPartyId = (String) context.get("billFromVendorPartyId");

        // check security permissions for order:
        //  SALES ORDERS - if userLogin has ORDERMGR_SALES_CREATE or ORDERMGR_CREATE permission, or if it is same party as the partyId, or
        //                 if it is an AGENT (sales rep) creating an order for his customer
        //  PURCHASE ORDERS - if there is a PURCHASE_ORDER permission
        Map resultSecurity = new HashMap();
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
        if ((orderTypeId.equals("SALES_ORDER")) && (UtilValidate.isNotEmpty(productStoreId))) {
            try {
                productStore = delegator.findByPrimaryKeyCache("ProductStore", UtilMisc.toMap("productStoreId", productStoreId));
            } catch (GenericEntityException e) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderErrorCouldNotFindProductStoreWithID",UtilMisc.toMap("productStoreId",productStoreId),locale)  + e.toString());
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
            orderType = delegator.findByPrimaryKeyCache("OrderType", UtilMisc.toMap("orderTypeId", orderTypeId));
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderErrorOrderTypeLookupFailed",locale) + e.toString());
        }

        // make sure we have a valid order type
        if (orderType == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderErrorInvalidOrderTypeWithID", UtilMisc.toMap("orderTypeId",orderTypeId), locale));
        }

        // check to make sure we have something to order
        List orderItems = (List) context.get("orderItems");
        if (orderItems.size() < 1) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error, "items.none", locale));
        }

        // all this marketing pkg auto stuff is deprecated in favor of MARKETING_PKG_AUTO productTypeId and a BOM of MANUF_COMPONENT assocs
        // these need to be retrieved now because they might be needed for exploding MARKETING_PKG_AUTO
        List orderAdjustments = (List) context.get("orderAdjustments");
        List orderItemShipGroupInfo = (List) context.get("orderItemShipGroupInfo");
        List orderItemPriceInfo = (List) context.get("orderItemPriceInfos");

        // check inventory and other things for each item
        List errorMessages = FastList.newInstance();
        Map normalizedItemQuantities = FastMap.newInstance();
        Map normalizedItemNames = FastMap.newInstance();
        Map itemValuesBySeqId = FastMap.newInstance();
        Iterator itemIter = orderItems.iterator();
        java.sql.Timestamp nowTimestamp = UtilDateTime.nowTimestamp();

        //
        // need to run through the items combining any cases where multiple lines refer to the
        // same product so the inventory check will work correctly
        // also count quantities ordered while going through the loop
        while (itemIter.hasNext()) {
            GenericValue orderItem = (GenericValue) itemIter.next();

            // start by putting it in the itemValuesById Map
            itemValuesBySeqId.put(orderItem.getString("orderItemSeqId"), orderItem);

            String currentProductId = (String) orderItem.get("productId");
            if (currentProductId != null) {
                // only normalize items with a product associated (ignore non-product items)
                if (normalizedItemQuantities.get(currentProductId) == null) {
                    normalizedItemQuantities.put(currentProductId, orderItem.getBigDecimal("quantity"));
                    normalizedItemNames.put(currentProductId, orderItem.getString("itemDescription"));
                } else {
                    BigDecimal currentQuantity = (BigDecimal) normalizedItemQuantities.get(currentProductId);
                    normalizedItemQuantities.put(currentProductId, currentQuantity.add(orderItem.getBigDecimal("quantity")));
                }

                try {
                    // count product ordered quantities
                    // run this synchronously so it will run in the same transaction
                    dispatcher.runSync("countProductQuantityOrdered", UtilMisc.<String, Object>toMap("productId", currentProductId, "quantity", orderItem.getBigDecimal("quantity"), "userLogin", userLogin));
                } catch (GenericServiceException e1) {
                    Debug.logError(e1, "Error calling countProductQuantityOrdered service", module);
                    return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderErrorCallingCountProductQuantityOrderedService",locale) + e1.toString());
                }
            }
        }

        if (!"PURCHASE_ORDER".equals(orderTypeId) && productStoreId == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderErrorTheProductStoreIdCanOnlyBeNullForPurchaseOrders",locale));
        }

        Iterator normalizedIter = normalizedItemQuantities.keySet().iterator();
        while (normalizedIter.hasNext()) {
            // lookup the product entity for each normalized item; error on products not found
            String currentProductId = (String) normalizedIter.next();
            BigDecimal currentQuantity = (BigDecimal) normalizedItemQuantities.get(currentProductId);
            String itemName = (String) normalizedItemNames.get(currentProductId);
            GenericValue product = null;

            try {
                product = delegator.findByPrimaryKeyCache("Product", UtilMisc.toMap("productId", currentProductId));
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
                // check to see if salesDiscontinuationDate has passed
                if (product.get("salesDiscontinuationDate") != null && nowTimestamp.after(product.getTimestamp("salesDiscontinuationDate"))) {
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
                    Map invReqResult = dispatcher.runSync("isStoreInventoryAvailableOrNotRequired", UtilMisc.toMap("productStoreId", productStoreId, "productId", product.get("productId"), "product", product, "quantity", currentQuantity));
                    if (ServiceUtil.isError(invReqResult)) {
                        errorMessages.add(invReqResult.get(ModelService.ERROR_MESSAGE));
                        errorMessages.addAll((List) invReqResult.get(ModelService.ERROR_MESSAGE_LIST));
                    } else if (!"Y".equals((String) invReqResult.get("availableOrNotRequired"))) {
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
        List workEfforts = (List) context.get("workEfforts"); // is an optional parameter from this service but mandatory for rental items
        Iterator orderItemIter = orderItems.iterator();
        while (orderItemIter.hasNext()) {
            GenericValue orderItem = (GenericValue) orderItemIter.next();
            if ("RENTAL_ORDER_ITEM".equals(orderItem.getString("orderItemTypeId"))) {
                // check to see if workefforts are available for this order type.
                if (UtilValidate.isEmpty(workEfforts))    {
                    String errMsg = "Work Efforts missing for ordertype RENTAL_ORDER_ITEM " + "Product: "  + orderItem.getString("productId");
                    Debug.logError(errMsg, module);
                    errorMessages.add(errMsg);
                    return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderRentalOrderItems",locale));
                }
                Iterator we = workEfforts.iterator();  // find the related workEffortItem (workEffortId = orderSeqId)
                while (we.hasNext()) {
                    // create the entity maps required.
                    GenericValue workEffort = (GenericValue) we.next();
                    if (workEffort.getString("workEffortId").equals(orderItem.getString("orderItemSeqId")))    {
                        List selFixedAssetProduct = null;
                        try {
                            List allFixedAssetProduct = delegator.findByAnd("FixedAssetProduct",UtilMisc.toMap("productId",orderItem.getString("productId"),"fixedAssetProductTypeId", "FAPT_USE"));
                            selFixedAssetProduct = EntityUtil.filterByDate(allFixedAssetProduct, nowTimestamp, "fromDate", "thruDate", true);
                        } catch (GenericEntityException e) {
                            String excMsg = "Could not find related Fixed Asset for the product: " + orderItem.getString("productId");
                            Debug.logError(excMsg, module);
                            errorMessages.add(excMsg);
                            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderCouldNotFindRelatedFixedAssetForTheProduct",UtilMisc.toMap("productId",orderItem.getString("productId")), locale));
                        }

                        if (UtilValidate.isNotEmpty(selFixedAssetProduct)) {
                            Iterator firstOne = selFixedAssetProduct.iterator();
                            if (firstOne.hasNext())        {
                                GenericValue fixedAssetProduct = delegator.makeValue("FixedAssetProduct");
                                fixedAssetProduct = (GenericValue) firstOne.next();
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
            Map getNextOrderIdContext = UtilMisc.toMap("partyId", orgPartyId, "userLogin", userLogin);

            if ((orderTypeId.equals("SALES_ORDER")) || (productStoreId != null)) {
                getNextOrderIdContext.put("productStoreId", productStoreId);
            }
            if (UtilValidate.isEmpty(orderId)) {
                try {
                    Map getNextOrderIdResult = dispatcher.runSync("getNextOrderId", getNextOrderIdContext);
                    if (ServiceUtil.isError(getNextOrderIdResult)) {
                        String errMsg = UtilProperties.getMessage(resource_error, "OrderErrorGettingNextOrderIdWhileCreatingOrder", locale);
                        return ServiceUtil.returnError(errMsg, null, null, getNextOrderIdResult);
                    }
                    orderId = (String) getNextOrderIdResult.get("orderId");
                } catch (GenericServiceException e) {
                    String errMsg = UtilProperties.getMessage(resource_error, "OrderCaughtGenericServiceExceptionWhileGettingOrderId", locale);
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
        Timestamp orderDate = (Timestamp) context.get("orderDate");
        if (orderDate == null) {
            orderDate = nowTimestamp;
        }

        Map orderHeaderMap = UtilMisc.toMap("orderId", orderId, "orderTypeId", orderTypeId,
                "orderDate", orderDate, "entryDate", nowTimestamp,
                "statusId", initialStatus, "billingAccountId", billingAccountId);
        orderHeaderMap.put("orderName", context.get("orderName"));
        if (isImmediatelyFulfilled) {
            // also flag this order as needing inventory issuance so that when it is set to complete it will be issued immediately (needsInventoryIssuance = Y)
            orderHeaderMap.put("needsInventoryIssuance", "Y");
        }
        GenericValue orderHeader = delegator.makeValue("OrderHeader", orderHeaderMap);

        // determine the sales channel
        String salesChannelEnumId = (String) context.get("salesChannelEnumId");
        if ((salesChannelEnumId == null) || salesChannelEnumId.equals("UNKNWN_SALES_CHANNEL")) {
            // try the default store sales channel
            if (orderTypeId.equals("SALES_ORDER") && (productStore != null)) {
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

        // first try to create the OrderHeader; if this does not fail, continue.
        try {
            delegator.create(orderHeader);
        } catch (GenericEntityException e) {
            Debug.logError(e, "Cannot create OrderHeader entity; problems with insert", module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderOrderCreationFailedPleaseNotifyCustomerService",locale));
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
        List orderItemGroups = (List) context.get("orderItemGroups");
        if (UtilValidate.isNotEmpty(orderItemGroups)) {
            Iterator orderItemGroupIter = orderItemGroups.iterator();
            while (orderItemGroupIter.hasNext()) {
                GenericValue orderItemGroup = (GenericValue) orderItemGroupIter.next();
                orderItemGroup.set("orderId", orderId);
                toBeStored.add(orderItemGroup);
            }
        }

        // set the order items
        Iterator oi = orderItems.iterator();
        while (oi.hasNext()) {
            GenericValue orderItem = (GenericValue) oi.next();
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
        List orderAttributes = (List) context.get("orderAttributes");
        if (UtilValidate.isNotEmpty(orderAttributes)) {
            Iterator oattr = orderAttributes.iterator();
            while (oattr.hasNext()) {
                GenericValue oatt = (GenericValue) oattr.next();
                oatt.set("orderId", orderId);
                toBeStored.add(oatt);
            }
        }

        // set the order item attributes
        List orderItemAttributes = (List) context.get("orderItemAttributes");
        if (UtilValidate.isNotEmpty(orderItemAttributes)) {
            Iterator oiattr = orderItemAttributes.iterator();
            while (oiattr.hasNext()) {
                GenericValue oiatt = (GenericValue) oiattr.next();
                oiatt.set("orderId", orderId);
                toBeStored.add(oiatt);
            }
        }

        // create the order internal notes
        List orderInternalNotes = (List) context.get("orderInternalNotes");
        if (UtilValidate.isNotEmpty(orderInternalNotes)) {
            Iterator orderInternalNotesIt = orderInternalNotes.iterator();
            while (orderInternalNotesIt.hasNext()) {
                String orderInternalNote = (String) orderInternalNotesIt.next();
                try {
                    Map noteOutputMap = dispatcher.runSync("createOrderNote", UtilMisc.<String, Object>toMap("orderId", orderId,
                                                                                             "internalNote", "Y",
                                                                                             "note", orderInternalNote,
                                                                                             "userLogin", userLogin));
                    if (ServiceUtil.isError(noteOutputMap)) {
                        return ServiceUtil.returnError("Error creating internal notes while creating order", null, null, noteOutputMap);
                    }
                } catch (GenericServiceException e) {
                    String errMsg = "Error creating internal notes while creating order: " + e.toString();
                    Debug.logError(e, errMsg, module);
                    return ServiceUtil.returnError(errMsg);
                }
            }
        }

        // create the order public notes
        List orderNotes = (List) context.get("orderNotes");
        if (UtilValidate.isNotEmpty(orderNotes)) {
            Iterator orderNotesIt = orderNotes.iterator();
            while (orderNotesIt.hasNext()) {
                String orderNote = (String) orderNotesIt.next();
                try {
                    Map noteOutputMap = dispatcher.runSync("createOrderNote", UtilMisc.<String, Object>toMap("orderId", orderId,
                                                                                             "internalNote", "N",
                                                                                             "note", orderNote,
                                                                                             "userLogin", userLogin));
                    if (ServiceUtil.isError(noteOutputMap)) {
                        return ServiceUtil.returnError("Error creating notes while creating order", null, null, noteOutputMap);
                    }
                } catch (GenericServiceException e) {
                    String errMsg = "Error creating notes while creating order: " + e.toString();
                    Debug.logError(e, errMsg, module);
                    return ServiceUtil.returnError(errMsg);
                }
            }
        }

        // create the workeffort records
        // and connect them with the orderitem over the WorkOrderItemFulfillment
        // create also the techData calendars to keep track of availability of the fixed asset.
        if (UtilValidate.isNotEmpty(workEfforts)) {
            List tempList = new LinkedList();
            Iterator we = workEfforts.iterator();
            while (we.hasNext()) {
                // create the entity maps required.
                GenericValue workEffort = (GenericValue) we.next();
                GenericValue workOrderItemFulfillment = delegator.makeValue("WorkOrderItemFulfillment");
                // find fixed asset supplied on the workeffort map
                GenericValue fixedAsset = null;
                Debug.logInfo("find the fixedAsset",module);
                try { fixedAsset = delegator.findByPrimaryKey("FixedAsset",
                        UtilMisc.toMap("fixedAssetId", workEffort.get("fixedAssetId")));
                }
                catch (GenericEntityException e) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderFixedAssetNotFoundFixedAssetId", UtilMisc.toMap("fixedAssetId",workEffort.get("fixedAssetId")), locale));
                }
                if (fixedAsset == null) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderFixedAssetNotFoundFixedAssetId", UtilMisc.toMap("fixedAssetId",workEffort.get("fixedAssetId")), locale));
                }
                // see if this fixed asset has a calendar, when no create one and attach to fixed asset
                Debug.logInfo("find the techdatacalendar",module);
                GenericValue techDataCalendar = null;
                try { techDataCalendar = fixedAsset.getRelatedOne("TechDataCalendar");
                }
                catch (GenericEntityException e) {
                    Debug.logInfo("TechData calendar does not exist yet so create for fixedAsset: " + fixedAsset.get("fixedAssetId") ,module);
                }
                if (techDataCalendar == null) {
                    Iterator fai = tempList.iterator();
                    while (fai.hasNext()) {
                        GenericValue currentValue = (GenericValue) fai.next();
                        if ("FixedAsset".equals(currentValue.getEntityName()) && currentValue.getString("fixedAssetId").equals(workEffort.getString("fixedAssetId"))) {
                            fixedAsset = currentValue;
                            break;
                        }
                    }
                    Iterator tdci = tempList.iterator();
                    while (tdci.hasNext()) {
                        GenericValue currentValue = (GenericValue) tdci.next();
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
                toBeStored.add(workEffort);  // store workeffort before workOrderItemFulfillment because of workEffortId key constraint
                // workOrderItemFulfillment
                workOrderItemFulfillment.set("workEffortId", workEffortId);
                workOrderItemFulfillment.set("orderId", orderId);
                toBeStored.add(workOrderItemFulfillment);
//                Debug.logInfo("Workeffort "+ workEffortId + " created for asset " + workEffort.get("fixedAssetId") + " and order "+ workOrderItemFulfillment.get("orderId") + "/" + workOrderItemFulfillment.get("orderItemSeqId") + " created", module);
//
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
                        techDataCalendarExcDay = delegator.findByPrimaryKey("TechDataCalendarExcDay",
                            UtilMisc.toMap("calendarId", fixedAsset.get("calendarId"), "exceptionDateStartTime", exceptionDateStartTime));
                    }
                    catch (GenericEntityException e) {
                        Debug.logInfo(" techData excday record not found so creating........", module);
                    }
                    if (techDataCalendarExcDay == null) {
                        Iterator tdcedi = tempList.iterator();
                        while (tdcedi.hasNext()) {
                            GenericValue currentValue = (GenericValue) tdcedi.next();
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
//                       Debug.logInfo(" techData excday record not found creating for calendarId: " + techDataCalendarExcDay.getString("calendarId") +
//                               " and date: " + exceptionDateStartTime.toString(), module);
                    }
                    // add the quantity to the quantity on the date record
                    BigDecimal newUsedCapacity = techDataCalendarExcDay.getBigDecimal("usedCapacity").add(workEffort.getBigDecimal("quantityToProduce"));
                    // check to see if the requested quantity is available on the requested day but only when the maximum capacity is set on the fixed asset
                    if (fixedAsset.get("productionCapacity") != null)    {
//                       Debug.logInfo("see if maximum not reached, available:  " + techDataCalendarExcDay.getString("exceptionCapacity") +
//                               " already allocated: " + techDataCalendarExcDay.getString("usedCapacity") +
//                                " Requested: " + workEffort.getString("quantityToProduce"), module);
                       if (newUsedCapacity.compareTo(techDataCalendarExcDay.getBigDecimal("exceptionCapacity")) > 0)    {
                            String errMsg = "ERROR: fixed_Asset_sold_out AssetId: " + workEffort.get("fixedAssetId") + " on date: " + techDataCalendarExcDay.getString("exceptionDateStartTime");
                            Debug.logError(errMsg, module);
                            errorMessages.add(errMsg);
                            continue;
                        }
                    }
                    techDataCalendarExcDay.set("usedCapacity", newUsedCapacity);
                    tempList.add(techDataCalendarExcDay);
//                  Debug.logInfo("Update success CalendarID: " + techDataCalendarExcDay.get("calendarId").toString() +
//                            " and for date: " + techDataCalendarExcDay.get("exceptionDateStartTime").toString() +
//                            " and for quantity: " + techDataCalendarExcDay.getDouble("usedCapacity").toString(), module);
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
            Iterator iter = orderAdjustments.iterator();

            while (iter.hasNext()) {
                GenericValue orderAdjustment = (GenericValue) iter.next();
                try {
                    orderAdjustment.set("orderAdjustmentId", delegator.getNextSeqId("OrderAdjustment"));
                } catch (IllegalArgumentException e) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderErrorCouldNotGetNextSequenceIdForOrderAdjustmentCannotCreateOrder",locale));
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
        List orderContactMechs = (List) context.get("orderContactMechs");
        if (UtilValidate.isNotEmpty(orderContactMechs)) {
            Iterator ocmi = orderContactMechs.iterator();

            while (ocmi.hasNext()) {
                GenericValue ocm = (GenericValue) ocmi.next();
                ocm.set("orderId", orderId);
                toBeStored.add(ocm);
            }
        }

        // set the order item contact mechs
        List orderItemContactMechs = (List) context.get("orderItemContactMechs");
        if (UtilValidate.isNotEmpty(orderItemContactMechs)) {
            Iterator oicmi = orderItemContactMechs.iterator();

            while (oicmi.hasNext()) {
                GenericValue oicm = (GenericValue) oicmi.next();
                oicm.set("orderId", orderId);
                toBeStored.add(oicm);
            }
        }

        // set the order item ship groups
        List dropShipGroupIds = FastList.newInstance(); // this list will contain the ids of all the ship groups for drop shipments (no reservations)
        if (UtilValidate.isNotEmpty(orderItemShipGroupInfo)) {
            Iterator osiInfos = orderItemShipGroupInfo.iterator();
            while (osiInfos.hasNext()) {
                GenericValue valueObj = (GenericValue) osiInfos.next();
                valueObj.set("orderId", orderId);
                if ("OrderItemShipGroup".equals(valueObj.getEntityName())) {
                    // ship group
                    if (valueObj.get("carrierRoleTypeId") == null) {
                        valueObj.set("carrierRoleTypeId", "CARRIER");
                    }
                    if (!UtilValidate.isEmpty(valueObj.getString("supplierPartyId"))) {
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
        Map additionalPartyRole = (Map) context.get("orderAdditionalPartyRoleMap");
        if (additionalPartyRole != null) {
            Iterator aprIt = additionalPartyRole.entrySet().iterator();
            while (aprIt.hasNext()) {
                Map.Entry entry = (Map.Entry) aprIt.next();
                String additionalRoleTypeId = (String) entry.getKey();
                List parties = (List) entry.getValue();
                if (parties != null) {
                    Iterator apIt = parties.iterator();
                    while (apIt.hasNext()) {
                        String additionalPartyId = (String) apIt.next();
                        toBeStored.add(delegator.makeValue("PartyRole", UtilMisc.toMap("partyId", additionalPartyId, "roleTypeId", additionalRoleTypeId)));
                        toBeStored.add(delegator.makeValue("OrderRole", UtilMisc.toMap("orderId", orderId, "partyId", additionalPartyId, "roleTypeId", additionalRoleTypeId)));
                    }
                }
            }
        }

        // set the item survey responses
        List surveyResponses = (List) context.get("orderItemSurveyResponses");
        if (UtilValidate.isNotEmpty(surveyResponses)) {
            Iterator oisr = surveyResponses.iterator();
            while (oisr.hasNext()) {
                GenericValue surveyResponse = (GenericValue) oisr.next();
                surveyResponse.set("orderId", orderId);
                toBeStored.add(surveyResponse);
            }
        }

        // set the item price info; NOTE: this must be after the orderItems are stored for referential integrity
        if (UtilValidate.isNotEmpty(orderItemPriceInfo)) {
            Iterator oipii = orderItemPriceInfo.iterator();

            while (oipii.hasNext()) {
                GenericValue oipi = (GenericValue) oipii.next();
                try {
                    oipi.set("orderItemPriceInfoId", delegator.getNextSeqId("OrderItemPriceInfo"));
                } catch (IllegalArgumentException e) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderErrorCouldNotGetNextSequenceIdForOrderItemPriceInfoCannotCreateOrder",locale));
                }

                oipi.set("orderId", orderId);
                toBeStored.add(oipi);
            }
        }

        // set the item associations
        List orderItemAssociations = (List) context.get("orderItemAssociations");
        if (UtilValidate.isNotEmpty(orderItemAssociations)) {
            Iterator oia = orderItemAssociations.iterator();
            while (oia.hasNext()) {
                GenericValue orderItemAssociation = (GenericValue) oia.next();
                if (orderItemAssociation.get("toOrderId") == null) {
                    orderItemAssociation.set("toOrderId", orderId);
                } else if (orderItemAssociation.get("orderId") == null) {
                    orderItemAssociation.set("orderId", orderId);
                }
                toBeStored.add(orderItemAssociation);
            }
        }

        // store the orderProductPromoUseInfos
        List orderProductPromoUses = (List) context.get("orderProductPromoUses");
        if (UtilValidate.isNotEmpty(orderProductPromoUses)) {
            Iterator orderProductPromoUseIter = orderProductPromoUses.iterator();
            while (orderProductPromoUseIter.hasNext()) {
                GenericValue productPromoUse = (GenericValue) orderProductPromoUseIter.next();
                productPromoUse.set("orderId", orderId);
                toBeStored.add(productPromoUse);
            }
        }

        // store the orderProductPromoCodes
        Set orderProductPromoCodes = (Set) context.get("orderProductPromoCodes");
        if (UtilValidate.isNotEmpty(orderProductPromoCodes)) {
            GenericValue orderProductPromoCode = delegator.makeValue("OrderProductPromoCode");
            Iterator orderProductPromoCodeIter = orderProductPromoCodes.iterator();
            while (orderProductPromoCodeIter.hasNext()) {
                orderProductPromoCode.clear();
                orderProductPromoCode.set("orderId", orderId);
                orderProductPromoCode.set("productPromoCodeId", orderProductPromoCodeIter.next());
                toBeStored.add(orderProductPromoCode);
            }
        }

        /* DEJ20050529 the OLD way, where a single party had all roles... no longer doing things this way...
        // define the roles for the order
        List userOrderRoleTypes = null;
        if ("SALES_ORDER".equals(orderTypeId)) {
            userOrderRoleTypes = UtilMisc.toList("END_USER_CUSTOMER", "SHIP_TO_CUSTOMER", "BILL_TO_CUSTOMER", "PLACING_CUSTOMER");
        } else if ("PURCHASE_ORDER".equals(orderTypeId)) {
            userOrderRoleTypes = UtilMisc.toList("SHIP_FROM_VENDOR", "BILL_FROM_VENDOR", "SUPPLIER_AGENT");
        } else {
            // TODO: some default behavior
        }

        // now add the roles
        if (userOrderRoleTypes != null) {
            Iterator i = userOrderRoleTypes.iterator();
            while (i.hasNext()) {
                String roleType = (String) i.next();
                String thisParty = partyId;
                if (thisParty == null) {
                    thisParty = "_NA_";  // will always set these roles so we can query
                }
                // make sure the party is in the role before adding
                toBeStored.add(delegator.makeValue("PartyRole", UtilMisc.toMap("partyId", partyId, "roleTypeId", roleType)));
                toBeStored.add(delegator.makeValue("OrderRole", UtilMisc.toMap("orderId", orderId, "partyId", partyId, "roleTypeId", roleType)));
            }
        }
        */

        // see the attributeRoleMap definition near the top of this file for attribute-role mappings
        Map attributeRoleMap = salesAttributeRoleMap;
        if ("PURCHASE_ORDER".equals(orderTypeId)) {
            attributeRoleMap = purchaseAttributeRoleMap;
        }
        Iterator attributeRoleEntryIter = attributeRoleMap.entrySet().iterator();
        while (attributeRoleEntryIter.hasNext()) {
            Map.Entry attributeRoleEntry = (Map.Entry) attributeRoleEntryIter.next();

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
                List productStoreRoles = delegator.findByAnd("ProductStoreRole", UtilMisc.toMap("roleTypeId", "VENDOR", "productStoreId", context.get("productStoreId")), UtilMisc.toList("-fromDate"));
                productStoreRoles = EntityUtil.filterByDate(productStoreRoles, true);
                GenericValue productStoreRole = EntityUtil.getFirst(productStoreRoles);
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
                List webSiteRoles = delegator.findByAnd("WebSiteRole", UtilMisc.toMap("roleTypeId", "VENDOR", "webSiteId", context.get("webSiteId")), UtilMisc.toList("-fromDate"));
                webSiteRoles = EntityUtil.filterByDate(webSiteRoles, true);
                GenericValue webSiteRole = EntityUtil.getFirst(webSiteRoles);
                if (webSiteRole != null) {
                    toBeStored.add(delegator.makeValue("OrderRole",
                            UtilMisc.toMap("orderId", orderId, "partyId", webSiteRole.get("partyId"), "roleTypeId", "VENDOR")));
                }
            } catch (GenericEntityException e) {
                Debug.logError(e, "Error looking up Vendor for the current Web Site", module);
            }

        }

        // set the order payment info
        List orderPaymentInfos = (List) context.get("orderPaymentInfo");
        if (UtilValidate.isNotEmpty(orderPaymentInfos)) {
            Iterator oppIter = orderPaymentInfos.iterator();
            while (oppIter.hasNext()) {
                GenericValue valueObj = (GenericValue) oppIter.next();
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
        List trackingCodeOrders = (List) context.get("trackingCodeOrders");
        if (UtilValidate.isNotEmpty(trackingCodeOrders)) {
            Iterator tkcdordIter = trackingCodeOrders.iterator();
            while (tkcdordIter.hasNext()) {
                GenericValue trackingCodeOrder = (GenericValue) tkcdordIter.next();
                trackingCodeOrder.set("orderId", orderId);
                toBeStored.add(trackingCodeOrder);
            }
        }

       // store the OrderTerm entities

       List orderTerms = (List) context.get("orderTerms");
       if (UtilValidate.isNotEmpty(orderTerms)) {
           Iterator orderTermIter = orderTerms.iterator();
           while (orderTermIter.hasNext()) {
               GenericValue orderTerm = (GenericValue) orderTermIter.next();
               orderTerm.set("orderId", orderId);
               orderTerm.set("orderItemSeqId","_NA_");
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

            // START inventory reservation
            List resErrorMessages = new LinkedList();
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
            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderErrorCouldNotCreateOrderWriteError",locale) + e.getMessage() + ").");
        }

        return successResult;
    }

    public static void reserveInventory(Delegator delegator, LocalDispatcher dispatcher, GenericValue userLogin, Locale locale, List orderItemShipGroupInfo, List dropShipGroupIds, Map itemValuesBySeqId, String orderTypeId, String productStoreId, List resErrorMessages) throws GeneralException {
        boolean isImmediatelyFulfilled = false;
        GenericValue productStore = null;
        if (UtilValidate.isNotEmpty(productStoreId)) {
            try {
                productStore = delegator.findByPrimaryKeyCache("ProductStore", UtilMisc.toMap("productStoreId", productStoreId));
            } catch (GenericEntityException e) {
                throw new GeneralException(UtilProperties.getMessage(resource_error, "OrderErrorCouldNotFindProductStoreWithID", UtilMisc.toMap("productStoreId", productStoreId), locale) + e.toString());
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
            Iterator osiInfos = orderItemShipGroupInfo.iterator();
            while (osiInfos.hasNext()) {
                GenericValue orderItemShipGroupAssoc = (GenericValue) osiInfos.next();
                if ("OrderItemShipGroupAssoc".equals(orderItemShipGroupAssoc.getEntityName())) {
                    if (dropShipGroupIds != null && dropShipGroupIds.contains(orderItemShipGroupAssoc.getString("shipGroupSeqId"))) {
                        // the items in the drop ship groups are not reserved
                        continue;
                    }
                    GenericValue orderItem = (GenericValue) itemValuesBySeqId.get(orderItemShipGroupAssoc.get("orderItemSeqId"));
                    GenericValue orderItemShipGroup = orderItemShipGroupAssoc.getRelatedOne("OrderItemShipGroup");
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
                            GenericValue product = orderItem.getRelatedOne("Product");
                            if (product == null) {
                                Debug.logError("Error when looking up product in reserveInventory service", module);
                                resErrorMessages.add("Error when looking up product in reserveInventory service");
                                continue;
                            }
                            if (reserveInventory) {
                                // for MARKETING_PKG_PICK reserve the components
                                if (CommonWorkers.hasParentType(delegator, "ProductType", "productTypeId", product.getString("productTypeId"), "parentTypeId", "MARKETING_PKG_PICK")) {
                                    Map componentsRes = dispatcher.runSync("getAssociatedProducts", UtilMisc.toMap("productId", orderItem.getString("productId"), "type", "PRODUCT_COMPONENT"));
                                    if (ServiceUtil.isError(componentsRes)) {
                                        resErrorMessages.add(componentsRes.get(ModelService.ERROR_MESSAGE));
                                        continue;
                                    } else {
                                        List assocProducts = (List) componentsRes.get("assocProducts");
                                        Iterator assocProductsIter = assocProducts.iterator();
                                        while (assocProductsIter.hasNext()) {
                                            GenericValue productAssoc = (GenericValue) assocProductsIter.next();
                                            BigDecimal quantityOrd = productAssoc.getBigDecimal("quantity");
                                            BigDecimal quantityKit = orderItemShipGroupAssoc.getBigDecimal("quantity");
                                            BigDecimal quantity = quantityOrd.multiply(quantityKit);
                                            Map reserveInput = new HashMap();
                                            reserveInput.put("productStoreId", productStoreId);
                                            reserveInput.put("productId", productAssoc.getString("productIdTo"));
                                            reserveInput.put("orderId", orderItem.getString("orderId"));
                                            reserveInput.put("orderItemSeqId", orderItem.getString("orderItemSeqId"));
                                            reserveInput.put("shipGroupSeqId", orderItemShipGroupAssoc.getString("shipGroupSeqId"));
                                            reserveInput.put("quantity", quantity);
                                            reserveInput.put("userLogin", userLogin);
                                            reserveInput.put("facilityId", shipGroupFacilityId);
                                            Map reserveResult = dispatcher.runSync("reserveStoreInventory", reserveInput);

                                            if (ServiceUtil.isError(reserveResult)) {
                                                String invErrMsg = "The product ";
                                                if (product != null) {
                                                    invErrMsg += getProductName(product, orderItem);
                                                }
                                                invErrMsg += " with ID " + orderItem.getString("productId") + " is no longer in stock. Please try reducing the quantity or removing the product from this order.";
                                                resErrorMessages.add(invErrMsg);
                                            }
                                        }
                                    }
                                } else {
                                    // reserve the product
                                    Map reserveInput = new HashMap();
                                    reserveInput.put("productStoreId", productStoreId);
                                    reserveInput.put("productId", orderItem.getString("productId"));
                                    reserveInput.put("orderId", orderItem.getString("orderId"));
                                    reserveInput.put("orderItemSeqId", orderItem.getString("orderItemSeqId"));
                                    reserveInput.put("shipGroupSeqId", orderItemShipGroupAssoc.getString("shipGroupSeqId"));
                                    reserveInput.put("facilityId", shipGroupFacilityId);
                                    // use the quantity from the orderItemShipGroupAssoc, NOT the orderItem, these are reserved by item-group assoc
                                    reserveInput.put("quantity", orderItemShipGroupAssoc.getBigDecimal("quantity"));
                                    reserveInput.put("userLogin", userLogin);
                                    Map reserveResult = dispatcher.runSync("reserveStoreInventory", reserveInput);

                                    if (ServiceUtil.isError(reserveResult)) {
                                        String invErrMsg = "The product ";
                                        if (product != null) {
                                            invErrMsg += getProductName(product, orderItem);
                                        }
                                        invErrMsg += " with ID " + orderItem.getString("productId") + " is no longer in stock. Please try reducing the quantity or removing the product from this order.";
                                        resErrorMessages.add(invErrMsg);
                                    }
                                }
                            }
                            // Reserving inventory or not we still need to create a marketing package
                            // If the product is a marketing package auto, attempt to create enough packages to bring ATP back to 0, won't necessarily create enough to cover this order.
                            if (CommonWorkers.hasParentType(delegator, "ProductType", "productTypeId", product.getString("productTypeId"), "parentTypeId", "MARKETING_PKG_AUTO")) {
                                // do something tricky here: run as the "system" user
                                // that can actually create and run a production run
                                GenericValue permUserLogin = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "system"));
                                Map inputMap = new HashMap();
                                if (UtilValidate.isNotEmpty(shipGroupFacilityId)) {
                                    inputMap.put("facilityId", shipGroupFacilityId);
                                } else {
                                    inputMap.put("facilityId", productStore.getString("inventoryFacilityId"));
                                }
                                inputMap.put("orderId", orderItem.getString("orderId"));
                                inputMap.put("orderItemSeqId", orderItem.getString("orderItemSeqId"));
                                inputMap.put("userLogin", permUserLogin);
                                Map prunResult = dispatcher.runSync("createProductionRunForMktgPkg", inputMap);
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
                }
            }
        }
    }

    public static String getProductName(GenericValue product, GenericValue orderItem) {
        if (UtilValidate.isNotEmpty(product.getString("productName"))) {
            return product.getString("productName");
        } else {
            return orderItem.getString("itemDescription");
        }
    }

    public static String getProductName(GenericValue product, String orderItemName) {
        if (UtilValidate.isNotEmpty(product.getString("productName"))) {
            return product.getString("productName");
        } else {
            return orderItemName;
        }
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
    public static Map resetGrandTotal(DispatchContext ctx, Map context) {
        Delegator delegator = ctx.getDelegator();
        //appears to not be used: GenericValue userLogin = (GenericValue) context.get("userLogin");
        String orderId = (String) context.get("orderId");

        GenericValue orderHeader = null;
        try {
            orderHeader = delegator.findByPrimaryKey("OrderHeader", UtilMisc.toMap("orderId", orderId));
        } catch (GenericEntityException e) {
            String errMsg = "ERROR: Could not set grantTotal on OrderHeader entity: " + e.toString();
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
                    productStore = delegator.findByPrimaryKeyCache("ProductStore", UtilMisc.toMap("productStoreId", productStoreId));
                } catch (GenericEntityException e) {
                    String errorMessage = UtilProperties.getMessage(resource_error, "OrderErrorCouldNotFindProductStoreWithID", UtilMisc.toMap("productStoreId", productStoreId), (Locale) context.get("locale")) + e.toString();
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
                    String errMsg = "ERROR: Could not set grandTotal on OrderHeader entity: " + e.toString();
                    Debug.logError(e, errMsg, module);
                    return ServiceUtil.returnError(errMsg);
                }
            }
        }

        return ServiceUtil.returnSuccess();
    }

    /** Service for setting the OrderHeader grandTotal for all OrderHeaders with no grandTotal */
    public static Map setEmptyGrandTotals(DispatchContext ctx, Map context) {
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Boolean forceAll = (Boolean) context.get("forceAll");
        Locale locale = (Locale) context.get("locale");
        if (forceAll == null) {
            forceAll = Boolean.FALSE;
        }

        EntityCondition cond = null;
        if (!forceAll.booleanValue()) {
            List exprs = UtilMisc.toList(EntityCondition.makeCondition("grandTotal", EntityOperator.EQUALS, null),
                    EntityCondition.makeCondition("remainingSubTotal", EntityOperator.EQUALS, null));
            cond = EntityCondition.makeCondition(exprs, EntityOperator.OR);
        }
        Set fields = UtilMisc.toSet("orderId");

        EntityListIterator eli = null;
        try {
            eli = delegator.find("OrderHeader", cond, null, fields, null, null);
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        if (eli != null) {
            // reset each order
            GenericValue orderHeader = null;
            while ((orderHeader = (GenericValue) eli.next()) != null) {
                String orderId = orderHeader.getString("orderId");
                Map resetResult = null;
                try {
                    resetResult = dispatcher.runSync("resetGrandTotal", UtilMisc.<String, Object>toMap("orderId", orderId, "userLogin", userLogin));
                } catch (GenericServiceException e) {
                    Debug.logError(e, "ERROR: Cannot reset order totals - " + orderId, module);
                }

                if (resetResult != null && ServiceUtil.isError(resetResult)) {
                    Debug.logWarning(UtilProperties.getMessage(resource_error,"OrderErrorCannotResetOrderTotals", UtilMisc.toMap("orderId",orderId,"resetResult",ServiceUtil.getErrorMessage(resetResult)), locale), module);
                }
            }

            // close the ELI
            try {
                eli.close();
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
            }
        } else {
            Debug.logInfo("No orders found for reset processing", module);
        }

        return ServiceUtil.returnSuccess();
    }

    /** Service for checking and re-calc the tax amount */
    public static Map recalcOrderTax(DispatchContext ctx, Map context) {
        LocalDispatcher dispatcher = ctx.getDispatcher();
        Delegator delegator = ctx.getDelegator();
        String orderId = (String) context.get("orderId");
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");

        // check and make sure we have permission to change the order
        Security security = ctx.getSecurity();
        boolean hasPermission = OrderServices.hasPermission(orderId, userLogin, "UPDATE", security, delegator);
        if (!hasPermission) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderYouDoNotHavePermissionToChangeThisOrdersStatus",locale));
        }

        // get the order header
        GenericValue orderHeader = null;
        try {
            orderHeader = delegator.findByPrimaryKey("OrderHeader", UtilMisc.toMap("orderId", orderId));
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderErrorCannotGetOrderHeaderEntity",locale) + e.getMessage());
        }

        if (orderHeader == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderErrorNoValidOrderHeaderFoundForOrderId", UtilMisc.toMap("orderId",orderId), locale));
        }

        // don't charge tax on purchase orders, better we still do.....
//        if ("PURCHASE_ORDER".equals(orderHeader.getString("orderTypeId"))) {
//            return ServiceUtil.returnSuccess();
//        }

        // Retrieve the order tax adjustments
        List orderTaxAdjustments = null;
        try {
            orderTaxAdjustments = delegator.findByAnd("OrderAdjustment", UtilMisc.toMap("orderId", orderId, "orderAdjustmentTypeId", "SALES_TAX"));
        } catch (GenericEntityException e) {
            Debug.logError(e, "Unable to retrieve SALES_TAX adjustments for order : " + orderId, module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderUnableToRetrieveSalesTaxAdjustments",locale));
        }

        // Accumulate the total existing tax adjustment
        BigDecimal totalExistingOrderTax = ZERO;
        Iterator otait = UtilMisc.toIterator(orderTaxAdjustments);
        while (otait != null && otait.hasNext()) {
            GenericValue orderTaxAdjustment = (GenericValue) otait.next();
            if (orderTaxAdjustment.get("amount") != null) {
                totalExistingOrderTax = totalExistingOrderTax.add(orderTaxAdjustment.getBigDecimal("amount").setScale(taxDecimals, taxRounding));
            }
        }

        // Recalculate the taxes for the order
        BigDecimal totalNewOrderTax = ZERO;
        OrderReadHelper orh = new OrderReadHelper(orderHeader);
        List shipGroups = orh.getOrderItemShipGroups();
        if (shipGroups != null) {
            Iterator itr = shipGroups.iterator();
            while (itr.hasNext()) {
                GenericValue shipGroup = (GenericValue) itr.next();
                String shipGroupSeqId = shipGroup.getString("shipGroupSeqId");

                List validOrderItems = orh.getValidOrderItems(shipGroupSeqId);
                if (validOrderItems != null) {
                    // prepare the inital lists
                    List products = new ArrayList(validOrderItems.size());
                    List amounts = new ArrayList(validOrderItems.size());
                    List shipAmts = new ArrayList(validOrderItems.size());
                    List itPrices = new ArrayList(validOrderItems.size());

                    // adjustments and total
                    List allAdjustments = orh.getAdjustments();
                    List orderHeaderAdjustments = OrderReadHelper.getOrderHeaderAdjustments(allAdjustments, shipGroupSeqId);
                    BigDecimal orderSubTotal = OrderReadHelper.getOrderItemsSubTotal(validOrderItems, allAdjustments);

                    // shipping amount
                    BigDecimal orderShipping = OrderReadHelper.calcOrderAdjustments(orderHeaderAdjustments, orderSubTotal, false, false, true);

                    //promotions amount
                    BigDecimal orderPromotions = OrderReadHelper.calcOrderPromoAdjustmentsBd(allAdjustments);

                    // build up the list of tax calc service parameters
                    for (int i = 0; i < validOrderItems.size(); i++) {
                        GenericValue orderItem = (GenericValue) validOrderItems.get(i);
                        String productId = orderItem.getString("productId");
                        try {
                            products.add(i, delegator.findByPrimaryKey("Product", UtilMisc.toMap("productId", productId)));  // get the product entity
                            amounts.add(i, OrderReadHelper.getOrderItemSubTotal(orderItem, allAdjustments, true, false)); // get the item amount
                            shipAmts.add(i, OrderReadHelper.getOrderItemAdjustmentsTotal(orderItem, allAdjustments, false, false, true)); // get the shipping amount
                            itPrices.add(i, orderItem.getBigDecimal("unitPrice"));
                        } catch (GenericEntityException e) {
                            Debug.logError(e, "Cannot read order item entity : " + orderItem, module);
                            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderCannotReadTheOrderItemEntity",locale));
                        }
                    }

                    GenericValue shippingAddress = orh.getShippingAddress(shipGroupSeqId);
                    // no shipping address, try the billing address
                    if (shippingAddress == null) {
                        List billingAddressList = orh.getBillingLocations();
                        if (billingAddressList.size() > 0) {
                            shippingAddress = (GenericValue) billingAddressList.get(0);
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
                                    shippingAddress = delegator.findByPrimaryKey("PostalAddress",
                                            UtilMisc.toMap("contactMechId", facilityContactMech.getString("contactMechId")));
                                } catch (GenericEntityException e) {
                                    Debug.logError(e, module);
                                }
                            }
                        }
                    }

                    // if shippingAddress is still null then don't calculate tax; it may be an situation where no tax is applicable, or the data is bad and we don't have a way to find an address to check tax for
                    if (shippingAddress == null) {
                        continue;
                    }

                    // prepare the service context
                    Map serviceContext = UtilMisc.toMap("productStoreId", orh.getProductStoreId(), "itemProductList", products, "itemAmountList", amounts,
                        "itemShippingList", shipAmts, "itemPriceList", itPrices, "orderShippingAmount", orderShipping);
                    serviceContext.put("shippingAddress", shippingAddress);
                    serviceContext.put("orderPromotionsAmount", orderPromotions);
                    if (orh.getBillToParty() != null) serviceContext.put("billToPartyId", orh.getBillToParty().getString("partyId"));
                    if (orh.getBillFromParty() != null) serviceContext.put("payToPartyId", orh.getBillFromParty().getString("partyId"));

                    // invoke the calcTax service
                    Map serviceResult = null;
                    try {
                        serviceResult = dispatcher.runSync("calcTax", serviceContext);
                    } catch (GenericServiceException e) {
                        Debug.logError(e, module);
                        return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderProblemOccurredInTaxService",locale));
                    }

                    if (ServiceUtil.isError(serviceResult)) {
                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                    }

                    // the adjustments (returned in order) from the tax service
                    List orderAdj = (List) serviceResult.get("orderAdjustments");
                    List itemAdj = (List) serviceResult.get("itemAdjustments");

                    // Accumulate the new tax total from the recalculated header adjustments
                    if (UtilValidate.isNotEmpty(orderAdj)) {
                        Iterator oai = orderAdj.iterator();
                        while (oai.hasNext()) {
                            GenericValue oa = (GenericValue) oai.next();
                            if (oa.get("amount") != null) {
                                totalNewOrderTax = totalNewOrderTax.add(oa.getBigDecimal("amount").setScale(taxDecimals, taxRounding));
                            }


                        }
                    }

                    // Accumulate the new tax total from the recalculated item adjustments
                    if (UtilValidate.isNotEmpty(itemAdj)) {
                        for (int i = 0; i < itemAdj.size(); i++) {
                            List itemAdjustments = (List) itemAdj.get(i);
                            Iterator ida = itemAdjustments.iterator();
                            while (ida.hasNext()) {
                                GenericValue ia = (GenericValue) ida.next();
                                if (ia.get("amount") != null) {
                                    totalNewOrderTax = totalNewOrderTax.add(ia.getBigDecimal("amount").setScale(taxDecimals, taxRounding));
                                }
                            }
                        }
                    }
                }
            }

            // Determine the difference between existing and new tax adjustment totals, if any
            BigDecimal orderTaxDifference = totalNewOrderTax.subtract(totalExistingOrderTax).setScale(taxDecimals, taxRounding);

            // If the total has changed, create an OrderAdjustment to reflect the fact
            if (orderTaxDifference.signum() != 0) {
                Map createOrderAdjContext = new HashMap();
                createOrderAdjContext.put("orderAdjustmentTypeId", "SALES_TAX");
                createOrderAdjContext.put("orderId", orderId);
                createOrderAdjContext.put("orderItemSeqId", "_NA_");
                createOrderAdjContext.put("shipGroupSeqId", "_NA_");
                createOrderAdjContext.put("description", "Tax adjustment due to order change");
                createOrderAdjContext.put("amount", orderTaxDifference);
                createOrderAdjContext.put("userLogin", userLogin);
                Map createOrderAdjResponse = null;
                try {
                    createOrderAdjResponse = dispatcher.runSync("createOrderAdjustment", createOrderAdjContext);
                } catch (GenericServiceException e) {
                    String createOrderAdjErrMsg = UtilProperties.getMessage(resource_error, "OrderErrorCallingCreateOrderAdjustmentService", locale);
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
    public static Map recalcOrderShipping(DispatchContext ctx, Map context) {
        LocalDispatcher dispatcher = ctx.getDispatcher();
        Delegator delegator = ctx.getDelegator();
        String orderId = (String) context.get("orderId");
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");

        // check and make sure we have permission to change the order
        Security security = ctx.getSecurity();
        boolean hasPermission = OrderServices.hasPermission(orderId, userLogin, "UPDATE", security, delegator);
        if (!hasPermission) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderYouDoNotHavePermissionToChangeThisOrdersStatus",locale));
        }

        // get the order header
        GenericValue orderHeader = null;
        try {
            orderHeader = delegator.findByPrimaryKey("OrderHeader", UtilMisc.toMap("orderId", orderId));
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderErrorCannotGetOrderHeaderEntity",locale) + e.getMessage());
        }

        if (orderHeader == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderErrorNoValidOrderHeaderFoundForOrderId", UtilMisc.toMap("orderId",orderId), locale));
        }

        OrderReadHelper orh = new OrderReadHelper(orderHeader);
        List shipGroups = orh.getOrderItemShipGroups();
        if (shipGroups != null) {
            Iterator i = shipGroups.iterator();
            while (i.hasNext()) {
                GenericValue shipGroup = (GenericValue) i.next();
                String shipGroupSeqId = shipGroup.getString("shipGroupSeqId");

                if (shipGroup.get("contactMechId") == null || shipGroup.get("shipmentMethodTypeId") == null) {
                    // not shipped (face-to-face order)
                    continue;
                }

                Map shippingEstMap = ShippingEvents.getShipEstimate(dispatcher, delegator, orh, shipGroupSeqId);
                BigDecimal shippingTotal = null;
                if (UtilValidate.isEmpty(orh.getValidOrderItems(shipGroupSeqId))) {
                    shippingTotal = ZERO;
                    Debug.log("No valid order items found - " + shippingTotal, module);
                } else {
                    shippingTotal = UtilValidate.isEmpty(shippingEstMap.get("shippingTotal")) ? ZERO : (BigDecimal)shippingEstMap.get("shippingTotal");
                    shippingTotal = shippingTotal.setScale(orderDecimals, orderRounding);
                    Debug.log("Got new shipping estimate - " + shippingTotal, module);
                }
                if (Debug.infoOn()) {
                    Debug.log("New Shipping Total [" + orderId + " / " + shipGroupSeqId + "] : " + shippingTotal, module);
                }

                BigDecimal currentShipping = OrderReadHelper.getAllOrderItemsAdjustmentsTotal(orh.getOrderItemAndShipGroupAssoc(shipGroupSeqId), orh.getAdjustments(), false, false, true);
                currentShipping = currentShipping.add(OrderReadHelper.calcOrderAdjustments(orh.getOrderHeaderAdjustments(shipGroupSeqId), orh.getOrderItemsSubTotal(), false, false, true));

                if (Debug.infoOn()) {
                    Debug.log("Old Shipping Total [" + orderId + " / " + shipGroupSeqId + "] : " + currentShipping, module);
                }

                List errorMessageList = (List) shippingEstMap.get(ModelService.ERROR_MESSAGE_LIST);
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
                    //orderAdjustment.set("comments", "Shipping Re-Calc Adjustment");
                    try {
                        orderAdjustment.create();
                    } catch (GenericEntityException e) {
                        Debug.logError(e, "Problem creating shipping re-calc adjustment : " + orderAdjustment, module);
                        return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderErrorCannotCreateAdjustment",locale));
                    }
                }

                // TODO: re-balance free shipping adjustment
            }
        }

        return ServiceUtil.returnSuccess();

    }

    /** Service for checking to see if an order is fully completed or canceled */
    public static Map checkItemStatus(DispatchContext ctx, Map context) {
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        Locale locale = (Locale) context.get("locale");

        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String orderId = (String) context.get("orderId");

        // check and make sure we have permission to change the order
        Security security = ctx.getSecurity();
        boolean hasPermission = OrderServices.hasPermission(orderId, userLogin, "UPDATE", security, delegator);
        if (!hasPermission) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderYouDoNotHavePermissionToChangeThisOrdersStatus",locale));
        }

        // get the order header
        GenericValue orderHeader = null;
        try {
            orderHeader = delegator.findByPrimaryKey("OrderHeader", UtilMisc.toMap("orderId", orderId));
        } catch (GenericEntityException e) {
            Debug.logError(e, "Cannot get OrderHeader record", module);
        }
        if (orderHeader == null) {
            Debug.logError("OrderHeader came back as null", module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderCannotUpdateNullOrderHeader",UtilMisc.toMap("orderId",orderId),locale));
        }

        // get the order items
        List orderItems = null;
        try {
            orderItems = delegator.findByAnd("OrderItem", UtilMisc.toMap("orderId", orderId));
        } catch (GenericEntityException e) {
            Debug.logError(e, "Cannot get OrderItem records", module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderProblemGettingOrderItemRecords", locale));
        }

        String orderHeaderStatusId = orderHeader.getString("statusId");
        String orderTypeId = orderHeader.getString("orderTypeId");

        boolean allCanceled = true;
        boolean allComplete = true;
        boolean allApproved = true;
        if (orderItems != null) {
            Iterator itemIter = orderItems.iterator();
            while (itemIter.hasNext()) {
                GenericValue item = (GenericValue) itemIter.next();
                String statusId = item.getString("statusId");
                //Debug.log("Item Status: " + statusId, module);
                if (!"ITEM_CANCELLED".equals(statusId)) {
                    //Debug.log("Not set to cancel", module);
                    allCanceled = false;
                    if (!"ITEM_COMPLETED".equals(statusId)) {
                        //Debug.log("Not set to complete", module);
                        allComplete = false;
                        if (!"ITEM_APPROVED".equals(statusId)) {
                            //Debug.log("Not set to approve", module);
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
                        GenericValue productStore = delegator.findByPrimaryKey("ProductStore", UtilMisc.toMap("productStoreId", orderHeader.getString("productStoreId")));
                        if (productStore != null) {
                            String headerApprovedStatus = productStore.getString("headerApprovedStatus");
                            if (UtilValidate.isNotEmpty(headerApprovedStatus)) {
                                if (headerApprovedStatus.equals(orderHeaderStatusId)) {
                                    Map orderStatusCheckMap = UtilMisc.toMap("orderId", orderId, "statusId", headerApprovedStatus, "orderItemSeqId", null);
                                    List orderStatusList = delegator.findByAnd("OrderStatus", orderStatusCheckMap);
                                    // should be 1 in the history, but just in case accept 0 too
                                    if (orderStatusList.size() <= 1) {
                                        changeToApprove = false;
                                    }
                                }
                            }
                        }
                    } catch (GenericEntityException e) {
                        String errMsg = "Database error checking if we should change order header status to approved: " + e.toString();
                        Debug.logError(e, errMsg, module);
                        return ServiceUtil.returnError(errMsg);
                    }
                }

                if ("ORDER_SENT".equals(orderHeaderStatusId)) changeToApprove = false;
                if ("ORDER_COMPLETED".equals(orderHeaderStatusId)) {
                    if ("SALES_ORDER".equals(orderTypeId)) {
                        changeToApprove = false;
                    }
                }
                if ("ORDER_CANCELLED".equals(orderHeaderStatusId)) changeToApprove = false;

                if (changeToApprove) {
                    newStatus = "ORDER_APPROVED";
                }
            }

            // now set the new order status
            if (newStatus != null && !newStatus.equals(orderHeaderStatusId)) {
                Map serviceContext = UtilMisc.toMap("orderId", orderId, "statusId", newStatus, "userLogin", userLogin);
                Map newSttsResult = null;
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
            Debug.logWarning(UtilProperties.getMessage(resource_error,"OrderReceivedNullForOrderItemRecordsOrderId", UtilMisc.toMap("orderId",orderId),locale), module);
        }

        return ServiceUtil.returnSuccess();
    }

    /** Service to cancel an order item quantity */
    public static Map cancelOrderItem(DispatchContext ctx, Map context) {
        LocalDispatcher dispatcher = ctx.getDispatcher();
        Delegator delegator = ctx.getDelegator();
        Locale locale = (Locale) context.get("locale");

        GenericValue userLogin = (GenericValue) context.get("userLogin");
        BigDecimal cancelQuantity = (BigDecimal) context.get("cancelQuantity");
        String orderId = (String) context.get("orderId");
        String orderItemSeqId = (String) context.get("orderItemSeqId");
        String shipGroupSeqId = (String) context.get("shipGroupSeqId");
        Map itemReasonMap = (Map) context.get("itemReasonMap");
        Map itemCommentMap = (Map) context.get("itemCommentMap");

        // debugging message info
        String itemMsgInfo = orderId + " / " + orderItemSeqId + " / " + shipGroupSeqId;

        // check and make sure we have permission to change the order
        Security security = ctx.getSecurity();

        boolean hasPermission = OrderServices.hasPermission(orderId, userLogin, "UPDATE", security, delegator);
        if (!hasPermission) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderYouDoNotHavePermissionToChangeThisOrdersStatus",locale));
        }

        Map fields = UtilMisc.toMap("orderId", orderId);
        if (orderItemSeqId != null) {
            fields.put("orderItemSeqId", orderItemSeqId);
        }
        if (shipGroupSeqId != null) {
            fields.put("shipGroupSeqId", shipGroupSeqId);
        }

        List orderItemShipGroupAssocs = null;
        try {
            orderItemShipGroupAssocs = delegator.findByAnd("OrderItemShipGroupAssoc", fields);
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderErrorCannotGetOrderItemAssocEntity", UtilMisc.toMap("itemMsgInfo",itemMsgInfo), locale));
        }

        if (orderItemShipGroupAssocs != null) {
            Iterator i = orderItemShipGroupAssocs.iterator();
            while (i.hasNext()) {
                GenericValue orderItemShipGroupAssoc = (GenericValue) i.next();
                GenericValue orderItem = null;
                try {
                    orderItem = orderItemShipGroupAssoc.getRelatedOne("OrderItem");
                } catch (GenericEntityException e) {
                    Debug.logError(e, module);
                }

                if (orderItem == null) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderErrorCannotCancelItemItemNotFound", UtilMisc.toMap("itemMsgInfo",itemMsgInfo), locale));
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
                if (availableQuantity == null) availableQuantity = BigDecimal.ZERO;
                if (itemQuantity == null) itemQuantity = BigDecimal.ZERO;

                BigDecimal thisCancelQty = null;
                if (cancelQuantity != null) {
                    thisCancelQty = cancelQuantity;
                } else {
                    thisCancelQty = availableQuantity;
                }

                if (availableQuantity.compareTo(thisCancelQty) >= 0) {
                    if (availableQuantity.compareTo(BigDecimal.ZERO) == 0) {
                        continue;  //OrderItemShipGroupAssoc already cancelled
                    }
                    orderItem.set("cancelQuantity", itemCancelQuantity.add(thisCancelQty));
                    orderItemShipGroupAssoc.set("cancelQuantity", aisgaCancelQuantity.add(thisCancelQty));

                    try {
                        List toStore = UtilMisc.toList(orderItem, orderItemShipGroupAssoc);
                        delegator.storeAll(toStore);
                    } catch (GenericEntityException e) {
                        Debug.logError(e, module);
                        return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderUnableToSetCancelQuantity", UtilMisc.toMap("itemMsgInfo",itemMsgInfo), locale));
                    }

                    //  create order item change record
                    if (!"Y".equals(orderItem.getString("isPromo"))) {
                        String reasonEnumId = null;
                        String changeComments = null;
                        if (UtilValidate.isNotEmpty(itemReasonMap)) {
                            reasonEnumId = (String) itemReasonMap.get(orderItem.getString("orderItemSeqId"));
                        }
                        if (UtilValidate.isNotEmpty(itemCommentMap)) {
                            changeComments = (String) itemCommentMap.get(orderItem.getString("orderItemSeqId"));
                        }

                        Map serviceCtx = FastMap.newInstance();
                        serviceCtx.put("orderId", orderItem.getString("orderId"));
                        serviceCtx.put("orderItemSeqId", orderItem.getString("orderItemSeqId"));
                        serviceCtx.put("cancelQuantity", thisCancelQty);
                        serviceCtx.put("changeTypeEnumId", "ODR_ITM_CANCEL");
                        serviceCtx.put("reasonEnumId", reasonEnumId);
                        serviceCtx.put("changeComments", changeComments);
                        serviceCtx.put("userLogin", userLogin);
                        Map resp = null;
                        try {
                            resp = dispatcher.runSync("createOrderItemChange", serviceCtx);
                        } catch (GenericServiceException e) {
                            Debug.logError(e, module);
                            return ServiceUtil.returnError(e.getMessage());
                        }
                        if (ServiceUtil.isError(resp)) {
                            return ServiceUtil.returnError((String)resp.get(ModelService.ERROR_MESSAGE));
                        }
                    }
                    
                    // log an order note
                    try {
                        BigDecimal quantity = thisCancelQty.setScale(1, orderRounding);
                        String cancelledItemToOrder = UtilProperties.getMessage(resource, "OrderCancelledItemToOrder", locale);
                        dispatcher.runSync("createOrderNote", UtilMisc.<String, Object>toMap("orderId", orderId, "note", cancelledItemToOrder +
                                orderItem.getString("productId") + " (" + quantity + ")", "internalNote", "Y", "userLogin", userLogin));
                    } catch (GenericServiceException e) {
                        Debug.logError(e, module);
                    }

                    if (thisCancelQty.compareTo(itemQuantity) >= 0) {
                        // all items are cancelled -- mark the item as cancelled
                        Map statusCtx = UtilMisc.toMap("orderId", orderId, "orderItemSeqId", orderItem.getString("orderItemSeqId"), "statusId", "ITEM_CANCELLED", "userLogin", userLogin);
                        try {
                            dispatcher.runSyncIgnore("changeOrderItemStatus", statusCtx);
                        } catch (GenericServiceException e) {
                            Debug.logError(e, module);
                            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderUnableToCancelOrderLine", UtilMisc.toMap("itemMsgInfo",itemMsgInfo), locale));
                        }
                    } else {
                        // reverse the inventory reservation
                        Map invCtx = UtilMisc.toMap("orderId", orderId, "orderItemSeqId", orderItem.getString("orderItemSeqId"), "shipGroupSeqId",
                                shipGroupSeqId, "cancelQuantity", thisCancelQty, "userLogin", userLogin);
                        try {
                            dispatcher.runSyncIgnore("cancelOrderItemInvResQty", invCtx);
                        } catch (GenericServiceException e) {
                            Debug.logError(e, module);
                            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderUnableToUpdateInventoryReservations", UtilMisc.toMap("itemMsgInfo",itemMsgInfo), locale));
                        }
                    }
                } else {
                    return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderInvalidCancelQuantityCannotCancel", UtilMisc.toMap("thisCancelQty",thisCancelQty), locale));
                }
            }
        } else {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderErrorCannotCancelItemItemNotFound", UtilMisc.toMap("itemMsgInfo",itemMsgInfo), locale));
        }

        return ServiceUtil.returnSuccess();
    }

    /** Service for changing the status on order item(s) */
    public static Map setItemStatus(DispatchContext ctx, Map context) {
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
            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderYouDoNotHavePermissionToChangeThisOrdersStatus",locale));
        }

        Map fields = UtilMisc.toMap("orderId", orderId);
        if (orderItemSeqId != null)
            fields.put("orderItemSeqId", orderItemSeqId);
        if (fromStatusId != null)
            fields.put("statusId", fromStatusId);

        List orderItems = null;
        try {
            orderItems = delegator.findByAnd("OrderItem", fields);
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderErrorCannotGetOrderItemEntity",locale) + e.getMessage());
        }

        if (UtilValidate.isNotEmpty(orderItems)) {
            List toBeStored = new ArrayList();
            Iterator itemsIterator = orderItems.iterator();
            while (itemsIterator.hasNext()) {
                GenericValue orderItem = (GenericValue) itemsIterator.next();
                if (orderItem == null) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderErrorCannotChangeItemStatusItemNotFound", locale));
                }
                if (Debug.verboseOn()) Debug.logVerbose("[OrderServices.setItemStatus] : Status Change: [" + orderId + "] (" + orderItem.getString("orderItemSeqId"), module);
                if (Debug.verboseOn()) Debug.logVerbose("[OrderServices.setItemStatus] : From Status : " + orderItem.getString("statusId"), module);
                if (Debug.verboseOn()) Debug.logVerbose("[OrderServices.setOrderStatus] : To Status : " + statusId, module);

                if (orderItem.getString("statusId").equals(statusId)) {
                    continue;
                }

                try {
                    Map statusFields = UtilMisc.toMap("statusId", orderItem.getString("statusId"), "statusIdTo", statusId);
                    GenericValue statusChange = delegator.findByPrimaryKeyCache("StatusValidChange", statusFields);

                    if (statusChange == null) {
                        Debug.logWarning(UtilProperties.getMessage(resource_error,"OrderItemStatusNotChangedIsNotAValidChange", UtilMisc.toMap("orderStatusId",orderItem.getString("statusId"),"statusId",statusId), locale), module);
                        continue;
                    }
                } catch (GenericEntityException e) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderErrorCouldNotChangeItemStatus",locale) + e.getMessage());
                }

                orderItem.set("statusId", statusId);
                toBeStored.add(orderItem);
                if (statusDateTime == null) {
                    statusDateTime = UtilDateTime.nowTimestamp();
                }
                // now create a status change
                Map changeFields = new HashMap();
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
                    return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderErrorCannotStoreStatusChanges", locale) + e.getMessage());
                }
            }

        }

        return ServiceUtil.returnSuccess();
    }

    /** Service for changing the status on an order header */
    public static Map setOrderStatus(DispatchContext ctx, Map context) {
        LocalDispatcher dispatcher = ctx.getDispatcher();
        Delegator delegator = ctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String orderId = (String) context.get("orderId");
        String statusId = (String) context.get("statusId");
        Map successResult = ServiceUtil.returnSuccess();
        Locale locale = (Locale) context.get("locale");

        // check and make sure we have permission to change the order
        Security security = ctx.getSecurity();
        boolean hasPermission = OrderServices.hasPermission(orderId, userLogin, "UPDATE", security, delegator);
        if (!hasPermission) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderYouDoNotHavePermissionToChangeThisOrdersStatus",locale));
        }

        if ("Y".equals(context.get("setItemStatus"))) {
            String newItemStatusId = null;
            if ("ORDER_APPROVED".equals(statusId)) {
                newItemStatusId = "ITEM_APPROVED";
            } else if ("ORDER_COMPLETE".equals(statusId)) {
                newItemStatusId = "ITEM_COMPLETE";
            } else if ("ORDER_CANCELLED".equals(statusId)) {
                newItemStatusId = "ITEM_CANCELLED";
            }

            if (newItemStatusId != null) {
                try {
                    Map resp = dispatcher.runSync("changeOrderItemStatus", UtilMisc.<String, Object>toMap("orderId", orderId, "statusId", newItemStatusId, "userLogin", userLogin));
                    if (ServiceUtil.isError(resp)) {
                        return ServiceUtil.returnError("Error changing item status to " + newItemStatusId, null, null, resp);
                    }
                } catch (GenericServiceException e) {
                    String errMsg = "Error changing item status to " + newItemStatusId + ": " + e.toString();
                    Debug.logError(e, errMsg, module);
                    return ServiceUtil.returnError(errMsg);
                }
            }
        }

        try {
            GenericValue orderHeader = delegator.findByPrimaryKey("OrderHeader", UtilMisc.toMap("orderId", orderId));

            if (orderHeader == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderErrorCouldNotChangeOrderStatusOrderCannotBeFound",locale));
            }
            // first save off the old status
            successResult.put("oldStatusId", orderHeader.get("statusId"));

            if (Debug.verboseOn()) Debug.logVerbose("[OrderServices.setOrderStatus] : From Status : " + orderHeader.getString("statusId"), module);
            if (Debug.verboseOn()) Debug.logVerbose("[OrderServices.setOrderStatus] : To Status : " + statusId, module);

            if (orderHeader.getString("statusId").equals(statusId)) {
                Debug.logWarning(UtilProperties.getMessage(resource_error,"OrderTriedToSetOrderStatusWithTheSameStatusIdforOrderWithId", UtilMisc.toMap("statusId",statusId,"orderId",orderId),locale),module);
                return successResult;
            }
            try {
                Map statusFields = UtilMisc.toMap("statusId", orderHeader.getString("statusId"), "statusIdTo", statusId);
                GenericValue statusChange = delegator.findByPrimaryKeyCache("StatusValidChange", statusFields);
                if (statusChange == null) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(resource_error, "OrderErrorCouldNotChangeOrderStatusStatusIsNotAValidChange", locale) + ": [" + statusFields.get("statusId") + "] -> [" + statusFields.get("statusIdTo") + "]");
                }
            } catch (GenericEntityException e) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderErrorCouldNotChangeOrderStatus",locale) + e.getMessage() + ").");
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

            orderHeader.store();
            orderStatus.create();

            successResult.put("needsInventoryIssuance", orderHeader.get("needsInventoryIssuance"));
            successResult.put("grandTotal", orderHeader.get("grandTotal"));
            successResult.put("orderTypeId", orderHeader.get("orderTypeId"));
            //Debug.logInfo("For setOrderStatus orderHeader is " + orderHeader, module);
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderErrorCouldNotChangeOrderStatus",locale) + e.getMessage() + ").");
        }

        // release the inital hold if we are cancelled or approved
        if ("ORDER_CANCELLED".equals(statusId) || "ORDER_APPROVED".equals(statusId)) {
            OrderChangeHelper.releaseInitialOrderHold(ctx.getDispatcher(), orderId);

            // cancel any order processing if we are cancelled
            if ("ORDER_CANCELLED".equals(statusId)) {
                OrderChangeHelper.abortOrderProcessing(ctx.getDispatcher(), orderId);
            }
        }

        successResult.put("orderStatusId", statusId);
        //Debug.logInfo("For setOrderStatus successResult is " + successResult, module);
        return successResult;
    }

    /** Service to update the order tracking number */
    public static Map updateTrackingNumber(DispatchContext dctx, Map context) {
        Map result = new HashMap();
        Delegator delegator = dctx.getDelegator();
        String orderId = (String) context.get("orderId");
        String shipGroupSeqId = (String) context.get("shipGroupSeqId");
        String trackingNumber = (String) context.get("trackingNumber");
        //Locale locale = (Locale) context.get("locale");

        try {
            GenericValue shipGroup = delegator.findByPrimaryKey("OrderItemShipGroup", UtilMisc.toMap("orderId", orderId, "shipGroupSeqId", shipGroupSeqId));

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
    public static Map addRoleType(DispatchContext ctx, Map context) {
        Map result = new HashMap();
        Delegator delegator = ctx.getDelegator();
        String orderId = (String) context.get("orderId");
        String partyId = (String) context.get("partyId");
        String roleTypeId = (String) context.get("roleTypeId");
        Boolean removeOld = (Boolean) context.get("removeOld");
        //Locale locale = (Locale) context.get("locale");

        if (removeOld != null && removeOld.booleanValue()) {
            try {
                delegator.removeByAnd("OrderRole", UtilMisc.toMap("orderId", orderId, "roleTypeId", roleTypeId));
            } catch (GenericEntityException e) {
                result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_ERROR);
                result.put(ModelService.ERROR_MESSAGE, "ERROR: Could not remove old roles (" + e.getMessage() + ").");
                return result;
            }
        }

        Map fields = UtilMisc.toMap("orderId", orderId, "partyId", partyId, "roleTypeId", roleTypeId);

        try {
            // first check and see if we are already there; if so, just return success
            GenericValue testValue = delegator.findByPrimaryKey("OrderRole", fields);
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
    public static Map removeRoleType(DispatchContext ctx, Map context) {
        Map result = new HashMap();
        Delegator delegator = ctx.getDelegator();
        String orderId = (String) context.get("orderId");
        String partyId = (String) context.get("partyId");
        String roleTypeId = (String) context.get("roleTypeId");
        Map fields = UtilMisc.toMap("orderId", orderId, "partyId", partyId, "roleTypeId", roleTypeId);
        //Locale locale = (Locale) context.get("locale");

        GenericValue testValue = null;

        try {
            testValue = delegator.findByPrimaryKey("OrderRole", fields);
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
            GenericValue value = delegator.findByPrimaryKey("OrderRole", fields);

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
    public static Map sendOrderConfirmNotification(DispatchContext ctx, Map context) {
        return sendOrderNotificationScreen(ctx, context, "PRDS_ODR_CONFIRM");
    }

    /** Service to email a customer with order changes */
    public static Map sendOrderCompleteNotification(DispatchContext ctx, Map context) {
        return sendOrderNotificationScreen(ctx, context, "PRDS_ODR_COMPLETE");
    }

    /** Service to email a customer with order changes */
    public static Map sendOrderBackorderNotification(DispatchContext ctx, Map context) {
        return sendOrderNotificationScreen(ctx, context, "PRDS_ODR_BACKORDER");
    }

    /** Service to email a customer with order changes */
    public static Map sendOrderChangeNotification(DispatchContext ctx, Map context) {
        return sendOrderNotificationScreen(ctx, context, "PRDS_ODR_CHANGE");
    }

    /** Service to email a customer with order payment retry results */
    public static Map sendOrderPayRetryNotification(DispatchContext ctx, Map context) {
        return sendOrderNotificationScreen(ctx, context, "PRDS_ODR_PAYRETRY");
    }

    protected static Map sendOrderNotificationScreen(DispatchContext dctx, Map context, String emailType) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String orderId = (String) context.get("orderId");
        String orderItemSeqId = (String) context.get("orderItemSeqId");
        String sendTo = (String) context.get("sendTo");
        String sendCc = (String) context.get("sendCc");
        String note = (String) context.get("note");
        String screenUri = (String) context.get("screenUri");
        GenericValue temporaryAnonymousUserLogin = (GenericValue) context.get("temporaryAnonymousUserLogin");
        if (userLogin == null) {
            // this may happen during anonymous checkout, try to the special case user
            userLogin = temporaryAnonymousUserLogin;
        }

        // prepare the order information
        Map sendMap = FastMap.newInstance();

        // get the order header and store
        GenericValue orderHeader = null;
        try {
            orderHeader = delegator.findByPrimaryKey("OrderHeader", UtilMisc.toMap("orderId", orderId));
        } catch (GenericEntityException e) {
            Debug.logError(e, "Problem getting OrderHeader", module);
        }

        if (orderHeader == null) {
            return ServiceUtil.returnFailure("Could not find OrderHeader with ID [" + orderId + "]");
        }

        if (orderHeader.get("webSiteId") == null) {
            return ServiceUtil.returnFailure("No website attached to order; cannot generate notification [" + orderId + "]");
        }

        GenericValue productStoreEmail = null;
        try {
            productStoreEmail = delegator.findByPrimaryKey("ProductStoreEmailSetting", UtilMisc.toMap("productStoreId", orderHeader.get("productStoreId"), "emailType", emailType));
        } catch (GenericEntityException e) {
            Debug.logError(e, "Problem getting the ProductStoreEmailSetting for productStoreId=" + orderHeader.get("productStoreId") + " and emailType=" + emailType, module);
        }
        if (productStoreEmail == null) {
            return ServiceUtil.returnFailure("No valid email setting for store with productStoreId=" + orderHeader.get("productStoreId") + " and emailType=" + emailType);
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
        } else {
            sendMap.put("bodyScreenUri", screenUri);
        }

        // website
        sendMap.put("webSiteId", orderHeader.get("webSiteId"));

        OrderReadHelper orh = new OrderReadHelper(orderHeader);
        String emailString = orh.getOrderEmailString();
        if (UtilValidate.isEmpty(emailString)) {
            Debug.logInfo("Customer is not setup to receive emails; no address(s) found [" + orderId + "]", module);
            return ServiceUtil.returnError("No sendTo email address found");
        }

        // where to get the locale... from PLACING_CUSTOMER's UserLogin.lastLocale,
        // or if not available then from ProductStore.defaultLocaleString
        // or if not available then the system Locale
        Locale locale = null;
        GenericValue placingParty = orh.getPlacingParty();
        GenericValue placingUserLogin = placingParty == null ? null : PartyWorker.findPartyLatestUserLogin(placingParty.getString("partyId"), delegator);
        if (locale == null && placingParty != null) {
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

        ResourceBundleMapWrapper uiLabelMap = (ResourceBundleMapWrapper) UtilProperties.getResourceBundleMap("EcommerceUiLabels", locale);
        uiLabelMap.addBottomResourceBundle("OrderUiLabels");
        uiLabelMap.addBottomResourceBundle("CommonUiLabels");

        Map bodyParameters = UtilMisc.toMap("orderId", orderId, "orderItemSeqId", orderItemSeqId, "userLogin", placingUserLogin, "uiLabelMap", uiLabelMap, "locale", locale);
        if (placingParty!= null) {
            bodyParameters.put("partyId", placingParty.get("partyId"));
        }
        bodyParameters.put("note", note);
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

        // send the notification
        Map sendResp = null;
        try {
            sendResp = dispatcher.runSync("sendMailFromScreen", sendMap);
        } catch (Exception e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error, "OrderServiceExceptionSeeLogs",locale));
        }

        // check for errors
        if (sendResp != null && !ServiceUtil.isError(sendResp)) {
            sendResp.put("emailType", emailType);
        }
        if (UtilValidate.isNotEmpty(orderId)) {
            sendResp.put("orderId", orderId);
        }
        return sendResp;
    }

    /** Service to email order notifications for pending actions */
    public static Map sendProcessNotification(DispatchContext ctx, Map context) {
        //appears to not be used: Map result = new HashMap();
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        String adminEmailList = (String) context.get("adminEmailList");
        String assignedToUser = (String) context.get("assignedPartyId");
        //appears to not be used: String assignedToRole = (String) context.get("assignedRoleTypeId");
        String workEffortId = (String) context.get("workEffortId");
        Locale locale = (Locale) context.get("locale");

        GenericValue workEffort = null;
        GenericValue orderHeader = null;
        //appears to not be used: String assignedEmail = null;

        // get the order/workflow info
        try {
            workEffort = delegator.findByPrimaryKey("WorkEffort", UtilMisc.toMap("workEffortId", workEffortId));
            String sourceReferenceId = workEffort.getString("sourceReferenceId");
            if (sourceReferenceId != null)
                orderHeader = delegator.findByPrimaryKey("OrderHeader", UtilMisc.toMap("orderId", sourceReferenceId));
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderProblemWithEntityLookup", locale));
        }

        // find the assigned user's email address(s)
        GenericValue party = null;
        Collection assignedToEmails = null;
        try {
            party = delegator.findByPrimaryKey("Party", UtilMisc.toMap("partyId", assignedToUser));
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderProblemWithEntityLookup", locale));
        }
        if (party != null) {
            assignedToEmails = ContactHelper.getContactMechByPurpose(party, "PRIMARY_EMAIL", false);
        }

        Map templateData = new HashMap(context);
        templateData.putAll(orderHeader);
        templateData.putAll(workEffort);

        /* NOTE DEJ20080609 commenting out this code because the old OFBiz Workflow Engine is being deprecated and this was only for that
        String omgStatusId = WfUtil.getOMGStatus(workEffort.getString("currentStatusId"));
        templateData.put("omgStatusId", omgStatusId);
        */
        templateData.put("omgStatusId", workEffort.getString("currentStatusId"));

        // get the assignments
        List assignments = null;
        if (workEffort != null) {
            try {
                assignments = workEffort.getRelated("WorkEffortPartyAssignment");
            } catch (GenericEntityException e1) {
                Debug.logError(e1, "Problems getting assignements", module);
            }
        }
        templateData.put("assignments", assignments);

        StringBuilder emailList = new StringBuilder();
        if (assignedToEmails != null) {
            Iterator aei = assignedToEmails.iterator();
            while (aei.hasNext()) {
                GenericValue ct = (GenericValue) aei.next();
                if (ct != null && ct.get("infoString") != null) {
                    if (emailList.length() > 1)
                        emailList.append(",");
                    emailList.append(ct.getString("infoString"));
                }
            }
        }
        if (adminEmailList != null) {
            if (emailList.length() > 1)
                emailList.append(",");
            emailList.append(adminEmailList);
        }

        // prepare the mail info
        String ofbizHome = System.getProperty("ofbiz.home");
        String templateName = ofbizHome + "/applications/order/email/default/emailprocessnotify.ftl";

        Map sendMailContext = new HashMap();
        sendMailContext.put("sendTo", emailList.toString());
        sendMailContext.put("sendFrom", "workflow@ofbiz.org"); // fixme
        sendMailContext.put("subject", "Workflow Notification");
        sendMailContext.put("templateName", templateName);
        sendMailContext.put("templateData", templateData);

        try {
            dispatcher.runAsync("sendGenericNotificationEmail", sendMailContext);
        } catch (GenericServiceException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderSendMailServiceFailed", locale) + e.getMessage());
        }
        return ServiceUtil.returnSuccess();
    }

    /** Service to create an order payment preference */
    public static Map createPaymentPreference(DispatchContext ctx, Map context) {
        Map result = new HashMap();
        Delegator delegator = ctx.getDelegator();
        String orderId = (String) context.get("orderId");
        String statusId = (String) context.get("statusId");
        String paymentMethodTypeId = (String) context.get("paymentMethodTypeId");
        String paymentMethodId = (String) context.get("paymentMethodId");
        BigDecimal maxAmount = (BigDecimal) context.get("maxAmount");
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");

        String prefId = null;

        try {
            prefId = delegator.getNextSeqId("OrderPaymentPreference");
        } catch (IllegalArgumentException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderErrorCouldNotCreateOrderPaymentPreferenceIdGenerationFailure", locale));
        }

        Map fields = UtilMisc.toMap("orderPaymentPreferenceId", prefId, "orderId", orderId, "paymentMethodTypeId",
                paymentMethodTypeId, "paymentMethodId", paymentMethodId, "maxAmount", maxAmount);

        if (statusId != null) {
            fields.put("statusId", statusId);
        }

        try {
            GenericValue v = delegator.makeValue("OrderPaymentPreference", fields);
            v.set("createdDate", UtilDateTime.nowTimestamp());
            if (userLogin != null) {
                v.set("createdByUserLogin", userLogin.getString("userLoginId"));
            }
            delegator.create(v);
        } catch (GenericEntityException e) {
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_ERROR);
            result.put(ModelService.ERROR_MESSAGE, "ERROR: Could not create OrderPaymentPreference (" + e.getMessage() + ").");
            return result;
        }
        result.put("orderPaymentPreferenceId", prefId);
        result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
        return result;
    }

    /** Service to get order header information as standard results. */
    public static Map getOrderHeaderInformation(DispatchContext dctx, Map context) {
        Delegator delegator = dctx.getDelegator();
        String orderId = (String) context.get("orderId");
        Locale locale = (Locale) context.get("locale");

        GenericValue orderHeader = null;
        try {
            orderHeader = delegator.findByPrimaryKey("OrderHeader", UtilMisc.toMap("orderId", orderId));
        } catch (GenericEntityException e) {
            Debug.logError(e, "Problem getting order header detial", module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderCannotGetOrderHeader", locale) + e.getMessage());
        }
        if (orderHeader != null) {
            Map result = ServiceUtil.returnSuccess();
            result.putAll(orderHeader);
            return result;
        }
        return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderErrorGettingOrderHeaderInformationNull", locale));
    }

    /** Service to get the total shipping for an order. */
    public static Map getOrderShippingAmount(DispatchContext dctx, Map context) {
        Delegator delegator = dctx.getDelegator();
        String orderId = (String) context.get("orderId");
        Locale locale = (Locale) context.get("locale");

        GenericValue orderHeader = null;
        try {
            orderHeader = delegator.findByPrimaryKey("OrderHeader", UtilMisc.toMap("orderId", orderId));
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderErrorCouldNotGetOrderInformation", locale) + e.getMessage() + ").");
        }

        Map result = null;
        if (orderHeader != null) {
            OrderReadHelper orh = new OrderReadHelper(orderHeader);
            List orderItems = orh.getValidOrderItems();
            List orderAdjustments = orh.getAdjustments();
            List orderHeaderAdjustments = orh.getOrderHeaderAdjustments();
            BigDecimal orderSubTotal = orh.getOrderItemsSubTotal();

            BigDecimal shippingAmount = OrderReadHelper.getAllOrderItemsAdjustmentsTotal(orderItems, orderAdjustments, false, false, true);
            shippingAmount = shippingAmount.add(OrderReadHelper.calcOrderAdjustments(orderHeaderAdjustments, orderSubTotal, false, false, true));

            result = ServiceUtil.returnSuccess();
            result.put("shippingAmount", shippingAmount);
        } else {
            result = ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderUnableToFindOrderHeaderCannotGetShippingAmount", locale));
        }
        return result;
    }

    /** Service to get an order contact mech. */
    public static Map getOrderAddress(DispatchContext dctx, Map context) {
        Map result = new HashMap();
        Delegator delegator = dctx.getDelegator();

        String orderId = (String) context.get("orderId");
        //appears to not be used: GenericValue v = null;
        String purpose[] = { "BILLING_LOCATION", "SHIPPING_LOCATION" };
        String outKey[] = { "billingAddress", "shippingAddress" };
        GenericValue orderHeader = null;
        //Locale locale = (Locale) context.get("locale");

        try {
            orderHeader = delegator.findByPrimaryKey("OrderHeader", UtilMisc.toMap("orderId", orderId));
            if (orderHeader != null)
                result.put("orderHeader", orderHeader);
        } catch (GenericEntityException e) {
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_ERROR);
            result.put(ModelService.ERROR_MESSAGE, "ERROR: Could not get OrderHeader (" + e.getMessage() + ").");
            return result;
        }
        if (orderHeader == null) {
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_ERROR);
            result.put(ModelService.ERROR_MESSAGE, "ERROR: Could get the OrderHeader.");
            return result;
        }
        for (int i = 0; i < purpose.length; i++) {
            try {
                GenericValue orderContactMech = EntityUtil.getFirst(orderHeader.getRelatedByAnd("OrderContactMech",
                            UtilMisc.toMap("contactMechPurposeTypeId", purpose[i])));
                GenericValue contactMech = orderContactMech.getRelatedOne("ContactMech");

                if (contactMech != null) {
                    result.put(outKey[i], contactMech.getRelatedOne("PostalAddress"));
                }
            } catch (GenericEntityException e) {
                result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_ERROR);
                result.put(ModelService.ERROR_MESSAGE, "ERROR: Problems getting contact mech (" + e.getMessage() + ").");
                return result;
            }
        }

        result.put("orderId", orderId);
        return result;
    }

    /** Service to create a order header note. */
    public static Map createOrderNote(DispatchContext dctx, Map context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String noteString = (String) context.get("note");
        String noteName = (String) context.get("noteName");
        String orderId = (String) context.get("orderId");
        String internalNote = (String) context.get("internalNote");
        Map noteCtx = UtilMisc.toMap("note", noteString, "userLogin", userLogin, "noteName", noteName);
        Locale locale = (Locale) context.get("locale");

        try {
            // Store the note.
            Map noteRes = dispatcher.runSync("createNote", noteCtx);

            if (ServiceUtil.isError(noteRes))
                return noteRes;

            String noteId = (String) noteRes.get("noteId");

            if (UtilValidate.isEmpty(noteId)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderProblemCreatingTheNoteNoNoteIdReturned", locale));
            }

            // Set the order info
            Map fields = UtilMisc.toMap("orderId", orderId, "noteId", noteId, "internalNote", internalNote);
            GenericValue v = delegator.makeValue("OrderHeaderNote", fields);

            delegator.create(v);
        } catch (GenericEntityException ee) {
            Debug.logError(ee, module);
            return ServiceUtil.returnError("Problem associating note with order (" + ee.getMessage() + ")");
        } catch (GenericServiceException se) {
            Debug.logError(se, module);
            return ServiceUtil.returnError("Problem associating note with order (" + se.getMessage() + ")");
        }

        return ServiceUtil.returnSuccess();
    }

    public static Map allowOrderSplit(DispatchContext ctx, Map context) {
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
                Map placingCustomerFields = UtilMisc.toMap("orderId", orderId, "partyId", userLogin.getString("partyId"), "roleTypeId", "PLACING_CUSTOMER");
                placingCustomer = delegator.findByPrimaryKey("OrderRole", placingCustomerFields);
            } catch (GenericEntityException e) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderErrorCannotGetOrderRoleEntity", locale) + e.getMessage());
            }
            if (placingCustomer == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderYouDoNotHavePermissionToChangeThisOrdersStatus", locale));
            }
        }

        GenericValue shipGroup = null;
        try {
            Map fields = UtilMisc.toMap("orderId", orderId, "shipGroupSeqId", shipGroupSeqId);
            shipGroup = delegator.findByPrimaryKey("OrderItemShipGroup", fields);
        } catch (GenericEntityException e) {
            Debug.logError(e, "Problems getting OrderItemShipGroup for : " + orderId + " / " + shipGroupSeqId, module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderCannotUpdateProblemGettingOrderShipmentPreference", locale));
        }

        if (shipGroup != null) {
            shipGroup.set("maySplit", "Y");
            try {
                shipGroup.store();
            } catch (GenericEntityException e) {
                Debug.logError("Problem saving OrderItemShipGroup for : " + orderId + " / " + shipGroupSeqId, module);
                return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderCannotUpdateProblemSettingOrderShipmentPreference", locale));
            }
        } else {
            Debug.logError("ERROR: Got a NULL OrderItemShipGroup", module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderCannotUpdateNoAvailableGroupsToChange", locale));
        }
        return ServiceUtil.returnSuccess();
    }

    public static Map cancelFlaggedSalesOrders(DispatchContext dctx, Map context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        //Locale locale = (Locale) context.get("locale");

        List ordersToCheck = null;

        // create the query expressions
        List exprs = UtilMisc.toList(
                EntityCondition.makeCondition("orderTypeId", EntityOperator.EQUALS, "SALES_ORDER"),
                EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "ORDER_COMPLETED"),
                EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "ORDER_CANCELLED"),
                EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "ORDER_REJECTED")
       );
        EntityConditionList<EntityExpr> ecl = EntityCondition.makeCondition(exprs, EntityOperator.AND);

        // get the orders
        try {
            ordersToCheck = delegator.findList("OrderHeader", ecl, null, UtilMisc.toList("orderDate"), null, false);
        } catch (GenericEntityException e) {
            Debug.logError(e, "Problem getting order headers", module);
        }

        if (UtilValidate.isEmpty(ordersToCheck)) {
            Debug.logInfo("No orders to check, finished", module);
            return ServiceUtil.returnSuccess();
        }

        Timestamp nowTimestamp = UtilDateTime.nowTimestamp();
        Iterator i = ordersToCheck.iterator();
        while (i.hasNext()) {
            GenericValue orderHeader = (GenericValue) i.next();
            String orderId = orderHeader.getString("orderId");
            String orderStatus = orderHeader.getString("statusId");

            if (orderStatus.equals("ORDER_CREATED")) {
                // first check for un-paid orders
                Timestamp orderDate = orderHeader.getTimestamp("entryDate");

                // need the store for the order
                GenericValue productStore = null;
                try {
                    productStore = orderHeader.getRelatedOne("ProductStore");
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
                    //Debug.log("Cancel Date : " + cancelDate, module);
                    //Debug.log("Current Date : " + nowDate, module);
                    if (cancelDate.equals(nowDate) || nowDate.after(cancelDate)) {
                        // cancel the order item(s)
                        Map svcCtx = UtilMisc.toMap("orderId", orderId, "statusId", "ITEM_CANCELLED", "userLogin", userLogin);
                        try {
                            // TODO: looks like result is ignored here, but we should be looking for errors
                            Map ores = dispatcher.runSync("changeOrderItemStatus", svcCtx);
                        } catch (GenericServiceException e) {
                            Debug.logError(e, "Problem calling change item status service : " + svcCtx, module);
                        }
                    }
                }
            } else {
                // check for auto-cancel items
                List itemsExprs = new ArrayList();

                // create the query expressions
                itemsExprs.add(EntityCondition.makeCondition("orderId", EntityOperator.EQUALS, orderId));
                itemsExprs.add(EntityCondition.makeCondition(UtilMisc.toList(EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, "ITEM_CREATED"),
                        EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, "ITEM_APPROVED")), EntityOperator.OR));
                itemsExprs.add(EntityCondition.makeCondition("dontCancelSetUserLogin", EntityOperator.EQUALS, GenericEntity.NULL_FIELD));
                itemsExprs.add(EntityCondition.makeCondition("dontCancelSetDate", EntityOperator.EQUALS, GenericEntity.NULL_FIELD));
                itemsExprs.add(EntityCondition.makeCondition("autoCancelDate", EntityOperator.NOT_EQUAL, GenericEntity.NULL_FIELD));

                ecl = EntityCondition.makeCondition(itemsExprs);

                List orderItems = null;
                try {
                    orderItems = delegator.findList("OrderItem", ecl, null, null, null, false);
                } catch (GenericEntityException e) {
                    Debug.logError(e, "Problem getting order item records", module);
                }
                if (UtilValidate.isNotEmpty(orderItems)) {
                    Iterator oii = orderItems.iterator();
                    while (oii.hasNext()) {
                        GenericValue orderItem = (GenericValue) oii.next();
                        String orderItemSeqId = orderItem.getString("orderItemSeqId");
                        Timestamp autoCancelDate = orderItem.getTimestamp("autoCancelDate");

                        if (autoCancelDate != null) {
                            if (nowTimestamp.equals(autoCancelDate) || nowTimestamp.after(autoCancelDate)) {
                                // cancel the order item
                                Map svcCtx = UtilMisc.toMap("orderId", orderId, "orderItemSeqId", orderItemSeqId, "statusId", "ITEM_CANCELLED", "userLogin", userLogin);
                                try {
                                    // TODO: check service result for an error return
                                    Map res = dispatcher.runSync("changeOrderItemStatus", svcCtx);
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

    public static Map checkDigitalItemFulfillment(DispatchContext dctx, Map context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String orderId = (String) context.get("orderId");
        Locale locale = (Locale) context.get("locale");

        // need the order header
        GenericValue orderHeader = null;
        try {
            orderHeader = delegator.findByPrimaryKey("OrderHeader", UtilMisc.toMap("orderId", orderId));
        } catch (GenericEntityException e) {
            Debug.logError(e, "ERROR: Unable to get OrderHeader for orderId : " + orderId, module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderErrorUnableToGetOrderHeaderForOrderId", UtilMisc.toMap("orderId",orderId), locale));
        }

        // get all the items for the order
        List orderItems = null;
        if (orderHeader != null) {
            try {
                orderItems = orderHeader.getRelated("OrderItem");
            } catch (GenericEntityException e) {
                Debug.logError(e, "ERROR: Unable to get OrderItem list for orderId : " + orderId, module);
                return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderErrorUnableToGetOrderItemListForOrderId", UtilMisc.toMap("orderId",orderId), locale));
            }
        }

        // find any digital or non-product items
        List nonProductItems = new ArrayList();
        List digitalItems = new ArrayList();
        Map digitalProducts = new HashMap();

        if (UtilValidate.isNotEmpty(orderItems)) {
            Iterator i = orderItems.iterator();
            while (i.hasNext()) {
                GenericValue item = (GenericValue) i.next();
                GenericValue product = null;
                try {
                    product = item.getRelatedOne("Product");
                } catch (GenericEntityException e) {
                    Debug.logError(e, "ERROR: Unable to get Product from OrderItem", module);
                }
                if (product != null) {
                    GenericValue productType = null;
                    try {
                        productType = product.getRelatedOne("ProductType");
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
            List itemsToInvoice = FastList.newInstance();
            itemsToInvoice.addAll(nonProductItems);
            itemsToInvoice.addAll(digitalItems);

            if (invoiceItems) {
                // invoice all APPROVED digital/non-product goods

                // do something tricky here: run as a different user that can actually create an invoice, post transaction, etc
                Map invoiceResult = null;
                try {
                    GenericValue permUserLogin = delegator.findByPrimaryKey("UserLogin", UtilMisc.toMap("userLoginId", "system"));
                    Map invoiceContext = UtilMisc.toMap("orderId", orderId, "billItems", itemsToInvoice, "userLogin", permUserLogin);
                    invoiceResult = dispatcher.runSync("createInvoiceForOrder", invoiceContext);
                } catch (GenericEntityException e) {
                    Debug.logError(e, "ERROR: Unable to invoice digital items", module);
                    return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderProblemWithInvoiceCreationDigitalItemsNotFulfilled", locale));
                } catch (GenericServiceException e) {
                    Debug.logError(e, "ERROR: Unable to invoice digital items", module);
                    return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderProblemWithInvoiceCreationDigitalItemsNotFulfilled", locale));
                }
                if (ModelService.RESPOND_ERROR.equals(invoiceResult.get(ModelService.RESPONSE_MESSAGE))) {
                    return ServiceUtil.returnError((String) invoiceResult.get(ModelService.ERROR_MESSAGE));
                }

                // update the status of digital goods to COMPLETED; leave physical/digital as APPROVED for pick/ship
                Iterator dii = itemsToInvoice.iterator();
                while (dii.hasNext()) {
                    GenericValue productType = null;
                    GenericValue item = (GenericValue) dii.next();
                    GenericValue product = (GenericValue) digitalProducts.get(item);
                    boolean markComplete = false;

                    if (product != null) {
                        try {
                            productType = product.getRelatedOne("ProductType");
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
                        Map statusCtx = new HashMap();
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
            Map fulfillContext = UtilMisc.toMap("orderId", orderId, "orderItems", digitalItems, "userLogin", userLogin);
            Map fulfillResult = null;
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

    public static Map fulfillDigitalItems(DispatchContext ctx, Map context) {
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        //appears to not be used: String orderId = (String) context.get("orderId");
        List orderItems = (List) context.get("orderItems");
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");

        if (UtilValidate.isNotEmpty(orderItems)) {
            // loop through the digital items to fulfill
            Iterator itemsIterator = orderItems.iterator();
            while (itemsIterator.hasNext()) {
                GenericValue orderItem = (GenericValue) itemsIterator.next();

                // make sure we have a valid item
                if (orderItem == null) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderErrorCannotCheckForFulfillmentItemNotFound", locale));
                }

                // locate the Product & ProductContent records
                GenericValue product = null;
                List productContent = null;
                try {
                    product = orderItem.getRelatedOne("Product");
                    if (product == null) {
                        return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderErrorCannotCheckForFulfillmentProductNotFound", locale));
                    }

                    List allProductContent = product.getRelated("ProductContent");

                    // try looking up the parent product if the product has no content and is a variant
                    if (UtilValidate.isEmpty(allProductContent) && ("Y".equals(product.getString("isVariant")))) {
                        GenericValue parentProduct = ProductWorker.getParentProduct(product.getString("productId"), delegator);
                        if (allProductContent == null) {
                            allProductContent = FastList.newInstance();
                        }
                        if (parentProduct != null) {
                            allProductContent.addAll(parentProduct.getRelated("ProductContent"));
                        }
                    }

                    if (UtilValidate.isNotEmpty(allProductContent)) {
                        // only keep ones with valid dates
                        productContent = EntityUtil.filterByDate(allProductContent, UtilDateTime.nowTimestamp(), "fromDate", "thruDate", true);
                        Debug.logInfo("Product has " + allProductContent.size() + " associations, " +
                                (productContent == null ? "0" : "" + productContent.size()) + " has valid from/thru dates", module);
                    }
                } catch (GenericEntityException e) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderErrorCannotGetProductEntity", locale) + e.getMessage());
                }

                // now use the ProductContent to fulfill the item
                if (UtilValidate.isNotEmpty(productContent)) {
                    Iterator prodcontentIterator = productContent.iterator();
                    while (prodcontentIterator.hasNext()) {
                        GenericValue productContentItem = (GenericValue) prodcontentIterator.next();
                        GenericValue content = null;
                        try {
                            content = productContentItem.getRelatedOne("Content");
                        } catch (GenericEntityException e) {
                            Debug.logError(e,"ERROR: Cannot get Content entity: " + e.getMessage(),module);
                            continue;
                        }

                        String fulfillmentType = productContentItem.getString("productContentTypeId");
                        if ("FULFILLMENT_EXTASYNC".equals(fulfillmentType) || "FULFILLMENT_EXTSYNC".equals(fulfillmentType)) {
                            // enternal service fulfillment
                            String fulfillmentService = (String) content.get("serviceName");
                            if (fulfillmentService == null) {
                                Debug.logError("ProductContent of type FULFILLMENT_EXTERNAL had Content with empty serviceName, can not run fulfillment", module);
                            }
                            Map serviceCtx = UtilMisc.toMap("userLogin", userLogin, "orderItem", orderItem);
                            serviceCtx.putAll(productContentItem.getPrimaryKey());
                            try {
                                Debug.logInfo("Running external fulfillment '" + fulfillmentService + "'", module);
                                if ("FULFILLMENT_EXTASYNC".equals(fulfillmentType)) {
                                    dispatcher.runAsync(fulfillmentService, serviceCtx, true);
                                } else if ("FULFILLMENT_EXTSYNC".equals(fulfillmentType)) {
                                    Map resp = dispatcher.runSync(fulfillmentService, serviceCtx);
                                    if (ServiceUtil.isError(resp)) {
                                        return ServiceUtil.returnError("Error running external fulfillment service", null, null, resp);
                                    }
                                }
                            } catch (GenericServiceException e) {
                                Debug.logError(e, "ERROR: Could not run external fulfillment service '" + fulfillmentService + "'; " + e.getMessage(), module);
                            }
                        } else if ("FULFILLMENT_EMAIL".equals(fulfillmentType)) {
                            // digital email fulfillment
                            // TODO: Add support for fulfillment email
                            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderEmailFulfillmentTypeNotYetImplemented", locale));
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
    public static Map invoiceServiceItems(DispatchContext dctx, Map context) {
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
            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderErrorUnableToGetOrderHeaderForOrderId", UtilMisc.toMap("orderId",orderId), locale));
        }

        // get all the approved items for the order
        List<GenericValue> orderItems = null;
        orderItems = orh.getOrderItemsByCondition(EntityCondition.makeCondition("statusId", "ITEM_APPROVED"));

        // find any service items
        List<GenericValue> serviceItems = FastList.newInstance();
        if (UtilValidate.isNotEmpty(orderItems)) {
            for(GenericValue item : orderItems) {
                GenericValue product = null;
                try {
                    product = item.getRelatedOne("Product");
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
            List<GenericValue> billItems = FastList.newInstance();
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
            } catch (GenericServiceException e) {
                Debug.logError(e, "ERROR: Unable to invoice service items", module);
                return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderProblemWithInvoiceCreationServiceItems", locale));
            }
            if (ModelService.RESPOND_ERROR.equals(invoiceResult.get(ModelService.RESPONSE_MESSAGE))) {
                return ServiceUtil.returnError((String) invoiceResult.get(ModelService.ERROR_MESSAGE));
            }

            // update the status of service goods to COMPLETED;
            for(GenericValue item : serviceItems) {
                Map<String, Object> statusCtx = FastMap.newInstance();
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

    public static Map addItemToApprovedOrder(DispatchContext dctx, Map context) {
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
        BigDecimal amount = (BigDecimal) context.get("amount");
        Timestamp itemDesiredDeliveryDate = (Timestamp) context.get("itemDesiredDeliveryDate");
        String overridePrice = (String) context.get("overridePrice");
        Map itemAttributesMap = (Map) context.get("itemAttributesMap");
        String reasonEnumId = (String) context.get("reasonEnumId");
        String changeComments = (String) context.get("changeComments");

        if (amount == null) {
            amount = BigDecimal.ZERO;
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
            return ServiceUtil.returnError("Invalid shipGroupSeqId [" + shipGroupSeqId + "]");
        }

        // obtain a shopping cart object for updating
        ShoppingCart cart = null;
        try {
            cart = loadCartForUpdate(dispatcher, delegator, userLogin, orderId);
        } catch (GeneralException e) {
            return ServiceUtil.returnError(e.getMessage());
        }
        if (cart == null) {
            return ServiceUtil.returnError("ERROR: Null shopping cart object returned!");
        }

        // add in the new product
        try {
            if ("PURCHASE_ORDER".equals(cart.getOrderType())) {
                GenericValue supplierProduct = cart.getSupplierProduct(productId, quantity, dispatcher);
                ShoppingCartItem item = null;
                if (supplierProduct != null) {
                    item = ShoppingCartItem.makePurchaseOrderItem(null, productId, null, quantity, null, null, prodCatalogId, null, null, null, dispatcher, cart, supplierProduct, itemDesiredDeliveryDate, itemDesiredDeliveryDate, null);
                    cart.addItem(0, item);
                } else {
                    throw new CartItemModifyException("No supplier information found for product [" + productId + "] and quantity quantity [" + quantity + "], cannot add to cart.");
                }

                if (basePrice != null) {
                    item.setBasePrice(basePrice);
                    item.setIsModifiedPrice(true);
                }

                cart.setItemShipGroupQty(item, item.getQuantity(), shipGroupIdx);
            } else {
                ShoppingCartItem item = ShoppingCartItem.makeItem(null, productId, null, quantity, null, null, null, null, null, null, null, null, prodCatalogId, null, null, null, dispatcher, cart, null, null, null, Boolean.FALSE, Boolean.FALSE);
                if (basePrice != null && overridePrice != null) {
                    item.setBasePrice(basePrice);
                    // special hack to make sure we re-calc the promos after a price change
                    item.setQuantity(quantity.add(BigDecimal.ONE), dispatcher, cart, false);
                    item.setQuantity(quantity, dispatcher, cart, false);
                    item.setBasePrice(basePrice);
                    item.setIsModifiedPrice(true);
                }

                // set the item in the selected ship group
                item.setShipBeforeDate(itemDesiredDeliveryDate);
                cart.setItemShipGroupQty(item, item.getQuantity(), shipGroupIdx);
            }
        } catch (CartItemModifyException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        } catch (ItemNotFoundException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        Map changeMap = UtilMisc.toMap("itemReasonMap", UtilMisc.toMap("reasonEnumId", reasonEnumId),
                                        "itemCommentMap", UtilMisc.toMap("changeComments", changeComments));
        // save all the updated information
        try {
            saveUpdatedCartToOrder(dispatcher, delegator, cart, locale, userLogin, orderId, changeMap);
        } catch (GeneralException e) {
            return ServiceUtil.returnError(e.getMessage());
        }

        // log an order note
        try {
            String addedItemToOrder = UtilProperties.getMessage(resource, "OrderAddedItemToOrder", locale);
            dispatcher.runSync("createOrderNote", UtilMisc.<String, Object>toMap("orderId", orderId, "note", addedItemToOrder +
                    productId + " (" + quantity + ")", "internalNote", "Y", "userLogin", userLogin));
        } catch (GenericServiceException e) {
            Debug.logError(e, module);
        }

        Map result = ServiceUtil.returnSuccess();
        result.put("shoppingCart", cart);
        result.put("orderId", orderId);
        return result;
    }

    public static Map updateApprovedOrderItems(DispatchContext dctx, Map context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");
        String orderId = (String) context.get("orderId");
        Map overridePriceMap = (Map) context.get("overridePriceMap");
        Map itemDescriptionMap = (Map) context.get("itemDescriptionMap");
        Map itemPriceMap = (Map) context.get("itemPriceMap");
        Map itemQtyMap = (Map) context.get("itemQtyMap");
        Map itemReasonMap = (Map) context.get("itemReasonMap");
        Map itemCommentMap = (Map) context.get("itemCommentMap");
        Map itemAttributesMap = (Map) context.get("itemAttributesMap");
        Map<String,String> itemEstimatedShipDateMap  = (Map) context.get("itemShipDateMap");
        Map<String, String> itemEstimatedDeliveryDateMap = (Map) context.get("itemDeliveryDateMap");

        // obtain a shopping cart object for updating
        ShoppingCart cart = null;
        try {
            cart = loadCartForUpdate(dispatcher, delegator, userLogin, orderId);
        } catch (GeneralException e) {
            return ServiceUtil.returnError(e.getMessage());
        }
        if (cart == null) {
            return ServiceUtil.returnError("ERROR: Null shopping cart object returned!");
        }

        // go through the item attributes map once to get a list of key names
        Set<String> attributeNames =FastSet.newInstance();
        Set<String> keys  = itemAttributesMap.keySet();
        for (String key : keys) {
            String[] attributeInfo = key.split(":");
            attributeNames.add(attributeInfo[0]);
        }

        // go through the item map and obtain the totals per item
        Map itemTotals = new HashMap();
        Iterator i = itemQtyMap.keySet().iterator();
        while (i.hasNext()) {
            String key = (String) i.next();
            String quantityStr = (String) itemQtyMap.get(key);
            BigDecimal groupQty = BigDecimal.ZERO;
            try {
                groupQty = new BigDecimal(quantityStr);
            } catch (NumberFormatException e) {
                Debug.logError(e, module);
                return ServiceUtil.returnError(e.getMessage());
            }

            if (groupQty.compareTo(BigDecimal.ZERO) == 0) {
                return ServiceUtil.returnError("Quantity must be >0, use cancel item to cancel completely!");
            }

            String[] itemInfo = key.split(":");
            BigDecimal tally = (BigDecimal) itemTotals.get(itemInfo[0]);
            if (tally == null) {
                tally = groupQty;
            } else {
                tally = tally.add(groupQty);
            }
            itemTotals.put(itemInfo[0], tally);
        }

        // set the items amount/price
        Iterator iai = itemTotals.keySet().iterator();
        while (iai.hasNext()) {
            String itemSeqId = (String) iai.next();
            ShoppingCartItem cartItem = cart.findCartItem(itemSeqId);

            if (cartItem != null) {
                BigDecimal qty = (BigDecimal) itemTotals.get(itemSeqId);
                BigDecimal priceSave = cartItem.getBasePrice();

                // set quantity
                try {
                    cartItem.setQuantity(qty, dispatcher, cart, false, false); // trigger external ops, don't reset ship groups (and update prices for both PO and SO items)
                } catch (CartItemModifyException e) {
                    Debug.logError(e, module);
                    return ServiceUtil.returnError(e.getMessage());
                }
                Debug.log("Set item quantity: [" + itemSeqId + "] " + qty, module);

                if (cartItem.getIsModifiedPrice()) // set price
                    cartItem.setBasePrice(priceSave);

                if (overridePriceMap.containsKey(itemSeqId)) {
                    String priceStr = (String) itemPriceMap.get(itemSeqId);
                    if (UtilValidate.isNotEmpty(priceStr)) {
                        BigDecimal price = new BigDecimal("-1");
                        price = new BigDecimal(priceStr).setScale(orderDecimals, orderRounding);
                        cartItem.setBasePrice(price);
                        cartItem.setIsModifiedPrice(true);
                        Debug.log("Set item price: [" + itemSeqId + "] " + price, module);
                    }

                }

                // Update the item description
                if (itemDescriptionMap != null && itemDescriptionMap.containsKey(itemSeqId)) {
                    String description = (String) itemDescriptionMap.get(itemSeqId);
                    if (UtilValidate.isNotEmpty(description)) {
                        cartItem.setName(description);
                        Debug.log("Set item description: [" + itemSeqId + "] " + description, module);
                    } else {
                        return ServiceUtil.returnError("Item description must not be empty");
                    }
                }

                // update the order item attributes
                if (itemAttributesMap != null) {
                    String attrValue = null;
                    for (String attrName : attributeNames) {
                        attrValue = (String) itemAttributesMap.get(attrName + ":" + itemSeqId);
                        if (UtilValidate.isNotEmpty(attrName)) {
                            cartItem.setOrderItemAttribute(attrName, attrValue);
                            Debug.log("Set item attribute Name: [" + itemSeqId + "] " + attrName + " , Value:" + attrValue, module);
                        }
                    }
                }

            } else {
                Debug.logInfo("Unable to locate shopping cart item for seqId #" + itemSeqId, module);
            }
        }
        // Create Estimated Delivery dates
        for (Map.Entry<String, String> entry : itemEstimatedDeliveryDateMap.entrySet()) {
            String itemSeqId =  entry.getKey();
            String estimatedDeliveryDate = entry.getValue();
            if (UtilValidate.isNotEmpty(estimatedDeliveryDate)) {
                Timestamp deliveryDate = Timestamp.valueOf(estimatedDeliveryDate);
                ShoppingCartItem cartItem = cart.findCartItem(itemSeqId);
                cartItem.setDesiredDeliveryDate(deliveryDate);
            }
        }

        // Create Estimated ship dates
        for (Map.Entry<String, String> entry : itemEstimatedShipDateMap.entrySet()) {
            String itemSeqId =  entry.getKey();
            String estimatedShipDate = entry.getValue();
            if (UtilValidate.isNotEmpty(estimatedShipDate)) {
                Timestamp shipDate = Timestamp.valueOf(estimatedShipDate);
                ShoppingCartItem cartItem = cart.findCartItem(itemSeqId);
                cartItem.setEstimatedShipDate(shipDate);
            }

        }

        // update the group amounts
        Iterator gai = itemQtyMap.keySet().iterator();
        while (gai.hasNext()) {
            String key = (String) gai.next();
            String quantityStr = (String) itemQtyMap.get(key);
            BigDecimal groupQty = BigDecimal.ZERO;
            try {
                groupQty = new BigDecimal(quantityStr);
            } catch (NumberFormatException e) {
                Debug.logError(e, module);
                return ServiceUtil.returnError(e.getMessage());
            }

            String[] itemInfo = key.split(":");
            int groupIdx = -1;
            try {
                groupIdx = Integer.parseInt(itemInfo[1]);
            } catch (NumberFormatException e) {
                Debug.logError(e, module);
                return ServiceUtil.returnError(e.getMessage());
            }

            // set the group qty
            ShoppingCartItem cartItem = cart.findCartItem(itemInfo[0]);
            if (cartItem != null) {
                Debug.log("Shipping info (before) for group #" + (groupIdx-1) + " [" + cart.getShipmentMethodTypeId(groupIdx-1) + " / " + cart.getCarrierPartyId(groupIdx-1) + "]", module);
                cart.setItemShipGroupQty(cartItem, groupQty, groupIdx - 1);
                Debug.log("Set ship group qty: [" + itemInfo[0] + " / " + itemInfo[1] + " (" + (groupIdx-1) + ")] " + groupQty, module);
                Debug.log("Shipping info (after) for group #" + (groupIdx-1) + " [" + cart.getShipmentMethodTypeId(groupIdx-1) + " / " + cart.getCarrierPartyId(groupIdx-1) + "]", module);
            }
        }

        // save all the updated information
        try {
            saveUpdatedCartToOrder(dispatcher, delegator, cart, locale, userLogin, orderId, UtilMisc.toMap("itemReasonMap", itemReasonMap, "itemCommentMap", itemCommentMap));
        } catch (GeneralException e) {
            return ServiceUtil.returnError(e.getMessage());
        }

        // run promotions to handle all changes in the cart
        ProductPromoWorker.doPromotions(cart, dispatcher);

        // log an order note
        try {
            dispatcher.runSync("createOrderNote", UtilMisc.<String, Object>toMap("orderId", orderId, "note", "Updated order.", "internalNote", "Y", "userLogin", userLogin));
        } catch (GenericServiceException e) {
            Debug.logError(e, module);
        }

        Map result = ServiceUtil.returnSuccess();
        result.put("shoppingCart", cart);
        result.put("orderId", orderId);
        return result;
    }

    public static Map loadCartForUpdate(DispatchContext dctx, Map context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();

        String orderId = (String) context.get("orderId");
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        ShoppingCart cart = null;
        Map result = null;
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
        Map loadCartResp = null;
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
        } else {
            cart.setOrderId(orderId);
        }

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
        List shipGroupAssocs = null;
        try {
            shipGroupAssocs = delegator.findByAnd("OrderItemShipGroupAssoc", UtilMisc.toMap("orderId", orderId));
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            throw new GeneralException(e.getMessage());
        }
        // cancel existing inventory reservations
        if (shipGroupAssocs != null) {
            Iterator iri = shipGroupAssocs.iterator();
            while (iri.hasNext()) {
                GenericValue shipGroupAssoc = (GenericValue) iri.next();
                String orderItemSeqId = shipGroupAssoc.getString("orderItemSeqId");
                String shipGroupSeqId = shipGroupAssoc.getString("shipGroupSeqId");

                Map cancelCtx = UtilMisc.toMap("userLogin", userLogin, "orderId", orderId);
                cancelCtx.put("orderItemSeqId", orderItemSeqId);
                cancelCtx.put("shipGroupSeqId", shipGroupSeqId);

                Map cancelResp = null;
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
        List promoItems = null;
        try {
            promoItems = delegator.findByAnd("OrderItem", UtilMisc.toMap("orderId", orderId, "isPromo", "Y"));
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            throw new GeneralException(e.getMessage());
        }
        if (promoItems != null) {
            Iterator pii = promoItems.iterator();
            while (pii.hasNext()) {
                GenericValue promoItem = (GenericValue) pii.next();
                // Skip if the promo is already cancelled
                if ("ITEM_CANCELLED".equals(promoItem.get("statusId"))) {
                    continue;
                }
                Map cancelPromoCtx = UtilMisc.toMap("orderId", orderId);
                cancelPromoCtx.put("orderItemSeqId", promoItem.getString("orderItemSeqId"));
                cancelPromoCtx.put("userLogin", userLogin);
                Map cancelResp = null;
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
        Map releaseResp = null;
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
        List paymentPrefsToCancel = null;
        try {
            List exprs = UtilMisc.toList(EntityCondition.makeCondition("orderId", EntityOperator.EQUALS, orderId));
            exprs.add(EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "PAYMENT_RECEIVED"));
            exprs.add(EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "PAYMENT_CANCELLED"));
            exprs.add(EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "PAYMENT_DECLINED"));
            exprs.add(EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "PAYMENT_SETTLED"));
            exprs.add(EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "PAYMENT_REFUNDED"));
            EntityCondition cond = EntityCondition.makeCondition(exprs, EntityOperator.AND);
            paymentPrefsToCancel = delegator.findList("OrderPaymentPreference", cond, null, null, null, false);
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            throw new GeneralException(e.getMessage());
        }
        if (paymentPrefsToCancel != null) {
            Iterator oppi = paymentPrefsToCancel.iterator();
            while (oppi.hasNext()) {
                GenericValue opp = (GenericValue) oppi.next();
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
            List adjExprs = new LinkedList();
            adjExprs.add(EntityCondition.makeCondition("orderId", EntityOperator.EQUALS, orderId));
            List exprs = new LinkedList();
            exprs.add(EntityCondition.makeCondition("orderAdjustmentTypeId", EntityOperator.EQUALS, "PROMOTION_ADJUSTMENT"));
            exprs.add(EntityCondition.makeCondition("orderAdjustmentTypeId", EntityOperator.EQUALS, "SHIPPING_CHARGES"));
            exprs.add(EntityCondition.makeCondition("orderAdjustmentTypeId", EntityOperator.EQUALS, "SALES_TAX"));
            adjExprs.add(EntityCondition.makeCondition(exprs, EntityOperator.OR));
            EntityCondition cond = EntityCondition.makeCondition(adjExprs, EntityOperator.AND);
            delegator.removeByCondition("OrderAdjustment", cond);
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            throw new GeneralException(e.getMessage());
        }

        return cart;
    }

    public static Map saveUpdatedCartToOrder(DispatchContext dctx, Map context) throws GeneralException {

        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();

        String orderId = (String) context.get("orderId");
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        ShoppingCart cart = (ShoppingCart) context.get("shoppingCart");
        Map changeMap = (Map) context.get("changeMap");
        Locale locale = (Locale) context.get("locale");

        Map result = null;
        try {
            saveUpdatedCartToOrder(dispatcher, delegator, cart, locale, userLogin, orderId, changeMap);
            result = ServiceUtil.returnSuccess();
            //result.put("shoppingCart", cart);
        } catch (GeneralException e) {
            Debug.logError(e, module);
            result = ServiceUtil.returnError(e.getMessage());
        }

        result.put("orderId", orderId);
        return result;
    }

    private static void saveUpdatedCartToOrder(LocalDispatcher dispatcher, Delegator delegator, ShoppingCart cart, Locale locale, GenericValue userLogin, String orderId, Map changeMap) throws GeneralException {
        // get/set the shipping estimates.  if it's a SALES ORDER, then return an error if there are no ship estimates
        int shipGroups = cart.getShipGroupSize();
        for (int gi = 0; gi < shipGroups; gi++) {
            String shipmentMethodTypeId = cart.getShipmentMethodTypeId(gi);
            String carrierPartyId = cart.getCarrierPartyId(gi);
            Debug.log("Getting ship estimate for group #" + gi + " [" + shipmentMethodTypeId + " / " + carrierPartyId + "]", module);
            Map result = ShippingEvents.getShipGroupEstimate(dispatcher, delegator, cart, gi);
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
        try {
            coh.calcAndAddTax();
        } catch (GeneralException e) {
            Debug.logError(e, module);
            throw new GeneralException(e.getMessage());
        }

        // get the new orderItems, adjustments, shipping info, payments and order item atrributes from the cart
        List<Map> modifiedItems = FastList.newInstance();
        List toStore = new LinkedList();
        List<GenericValue> toAddList = new ArrayList<GenericValue>();
        toAddList.addAll(cart.makeAllAdjustments());
        cart.clearAllPromotionAdjustments();
        ProductPromoWorker.doPromotions(cart, dispatcher);

        // validate the payment methods
        Map validateResp = coh.validatePaymentMethods();
        if (ServiceUtil.isError(validateResp)) {
            throw new GeneralException(ServiceUtil.getErrorMessage(validateResp));
        }

        toStore.addAll(cart.makeOrderItems());
        toStore.addAll(cart.makeAllAdjustments());

        String shipGroupSeqId = null;
        long groupIndex = cart.getShipInfoSize();
        List orderAdjustments = new ArrayList();
        for (long itr = 1; itr <= groupIndex; itr++) {
            shipGroupSeqId = UtilFormatOut.formatPaddedNumber(1, 5);
            List<GenericValue> removeList = new ArrayList<GenericValue>();
            for (GenericValue stored: (List<GenericValue>)toStore) {
                if ("OrderAdjustment".equals(stored.getEntityName())) {
                    if (("SHIPPING_CHARGES".equals(stored.get("orderAdjustmentTypeId")) ||
                            "SALES_TAX".equals(stored.get("orderAdjustmentTypeId"))) &&
                            stored.get("orderId").equals(orderId) &&
                            stored.get("shipGroupSeqId").equals(shipGroupSeqId)) {
                        // Removing objects from toStore list for old Shipping and Handling Charges Adjustment and Sales Tax Adjustment.
                        removeList.add(stored);
                    }
                    if (stored.get("comments") != null && ((String)stored.get("comments")).startsWith("Added manually by")) {
                        // Removing objects from toStore list for Manually added Adjustment.
                        removeList.add(stored);
                    }
                }
            }
            toStore.removeAll(removeList);
        }
        for (GenericValue toAdd: (List<GenericValue>)toAddList) {
            if ("OrderAdjustment".equals(toAdd.getEntityName())) {
                if (toAdd.get("comments") != null && ((String)toAdd.get("comments")).startsWith("Added manually by") && (("PROMOTION_ADJUSTMENT".equals(toAdd.get("orderAdjustmentTypeId"))) ||
                        ("SHIPPING_CHARGES".equals(toAdd.get("orderAdjustmentTypeId"))) || ("SALES_TAX".equals(toAdd.get("orderAdjustmentTypeId"))))) {
                    toStore.add(toAdd);
                }
            }
        }
        // Creating objects for New Shipping and Handling Charges Adjustment and Sales Tax Adjustment
        toStore.addAll(cart.makeAllShipGroupInfos());
        toStore.addAll(cart.makeAllOrderPaymentInfos(dispatcher));
        toStore.addAll(cart.makeAllOrderItemAttributes(orderId, ShoppingCart.FILLED_ONLY));

        // get the empty order item atrributes from the cart and remove them
        List toRemove = FastList.newInstance();
        toRemove.addAll(cart.makeAllOrderItemAttributes(orderId, ShoppingCart.EMPTY_ONLY));

        // set the orderId & other information on all new value objects
        List dropShipGroupIds = FastList.newInstance(); // this list will contain the ids of all the ship groups for drop shipments (no reservations)
        Iterator tsi = toStore.iterator();
        while (tsi.hasNext()) {
            GenericValue valueObj = (GenericValue) tsi.next();
            valueObj.set("orderId", orderId);
            if ("OrderItemShipGroup".equals(valueObj.getEntityName())) {
                // ship group
                if (valueObj.get("carrierRoleTypeId") == null) {
                    valueObj.set("carrierRoleTypeId", "CARRIER");
                }
                if (!UtilValidate.isEmpty(valueObj.get("supplierPartyId"))) {
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
            } else if ("OrderPaymentPreference".equals(valueObj.getEntityName())) {
                if (valueObj.get("orderPaymentPreferenceId") == null) {
                    valueObj.set("orderPaymentPreferenceId", delegator.getNextSeqId("OrderPaymentPreference"));
                    valueObj.set("createdDate", UtilDateTime.nowTimestamp());
                    valueObj.set("createdByUserLogin", userLogin.getString("userLoginId"));
                }
                if (valueObj.get("statusId") == null) {
                    valueObj.set("statusId", "PAYMENT_NOT_RECEIVED");
                }
            } else if ("OrderItem".equals(valueObj.getEntityName())) {

                //  ignore promotion items. They are added/canceled automatically
                if ("Y".equals(valueObj.getString("isPromo"))) {
                    continue;
                }
                GenericValue oldOrderItem = null;
                try {
                    oldOrderItem = delegator.findByPrimaryKey("OrderItem", UtilMisc.toMap("orderId", valueObj.getString("orderId"), "orderItemSeqId", valueObj.getString("orderItemSeqId")));
                } catch (GenericEntityException e) {
                    Debug.logError(e, module);
                    throw new GeneralException(e.getMessage());
                }
                if (UtilValidate.isNotEmpty(oldOrderItem)) {

                    //  Existing order item found. Check for modifications and store if any
                    String oldItemDescription = oldOrderItem.getString("itemDescription") != null ? oldOrderItem.getString("itemDescription") : "";
                    BigDecimal oldQuantity = oldOrderItem.getBigDecimal("quantity") != null ? oldOrderItem.getBigDecimal("quantity") : BigDecimal.ZERO;
                    BigDecimal oldUnitPrice = oldOrderItem.getBigDecimal("unitPrice") != null ? oldOrderItem.getBigDecimal("unitPrice") : BigDecimal.ZERO;

                    boolean changeFound = false;
                    Map modifiedItem = FastMap.newInstance();
                    if (!oldItemDescription.equals(valueObj.getString("itemDescription"))) {
                        modifiedItem.put("itemDescription", oldItemDescription);
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
                        Map itemReasonMap = (Map) changeMap.get("itemReasonMap");
                        Map itemCommentMap = (Map) changeMap.get("itemCommentMap");
                        if (UtilValidate.isNotEmpty(itemReasonMap)) {
                            String changeReasonId = (String) itemReasonMap.get(valueObj.getString("orderItemSeqId"));
                            modifiedItem.put("reasonEnumId", changeReasonId);
                        }
                        if (UtilValidate.isNotEmpty(itemCommentMap)) {
                            String changeComments = (String) itemCommentMap.get(valueObj.getString("orderItemSeqId"));
                            modifiedItem.put("changeComments", changeComments);
                        }

                        modifiedItem.put("orderId", valueObj.getString("orderId"));
                        modifiedItem.put("orderItemSeqId", valueObj.getString("orderItemSeqId"));
                        modifiedItem.put("changeTypeEnumId", "ODR_ITM_UPDATE");
                        modifiedItems.add(modifiedItem);
                    }
                } else {

                    //  this is a new item appended to the order
                    Map itemReasonMap = (Map) changeMap.get("itemReasonMap");
                    Map itemCommentMap = (Map) changeMap.get("itemCommentMap");
                    Map appendedItem = FastMap.newInstance();
                    if (UtilValidate.isNotEmpty(itemReasonMap)) {
                        String changeReasonId = (String) itemReasonMap.get("reasonEnumId");
                        appendedItem.put("reasonEnumId", changeReasonId);
                    }
                    if (UtilValidate.isNotEmpty(itemCommentMap)) {
                        String changeComments = (String) itemCommentMap.get("changeComments");
                        appendedItem.put("changeComments", changeComments);
                    }

                    appendedItem.put("orderId", valueObj.getString("orderId"));
                    appendedItem.put("orderItemSeqId", valueObj.getString("orderItemSeqId"));
                    appendedItem.put("quantity", valueObj.getBigDecimal("quantity"));
                    appendedItem.put("changeTypeEnumId", "ODR_ITM_APPEND");
                    modifiedItems.add(appendedItem);
                }
            }
        }
        Debug.log("To Store Contains: " + toStore, module);

        // remove any order item attributes that were set to empty
        try {
            delegator.removeAll(toRemove,true);
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
            for (Map modifiendItem: modifiedItems) {
                Map serviceCtx = FastMap.newInstance();
                serviceCtx.put("orderId", modifiendItem.get("orderId"));
                serviceCtx.put("orderItemSeqId", modifiendItem.get("orderItemSeqId"));
                serviceCtx.put("itemDescription", modifiendItem.get("itemDescription"));
                serviceCtx.put("quantity", modifiendItem.get("quantity"));
                serviceCtx.put("unitPrice", modifiendItem.get("unitPrice"));
                serviceCtx.put("changeTypeEnumId", modifiendItem.get("changeTypeEnumId"));
                serviceCtx.put("reasonEnumId", modifiendItem.get("reasonEnumId"));
                serviceCtx.put("changeComments", modifiendItem.get("changeComments"));
                serviceCtx.put("userLogin", userLogin);
                Map resp = null;
                try {
                    resp = dispatcher.runSync("createOrderItemChange", serviceCtx);
                } catch (GenericServiceException e) {
                    Debug.logError(e, module);
                    throw new GeneralException(e.getMessage());
                }
                if (ServiceUtil.isError(resp)) {
                    throw new GeneralException((String) resp.get(ModelService.ERROR_MESSAGE));
                }
            }
        }

        // make the order item object map & the ship group assoc list
        List orderItemShipGroupAssoc = new LinkedList();
        Map itemValuesBySeqId = new HashMap();
        Iterator oii = toStore.iterator();
        while (oii.hasNext()) {
            GenericValue v = (GenericValue) oii.next();
            if ("OrderItem".equals(v.getEntityName())) {
                itemValuesBySeqId.put(v.getString("orderItemSeqId"), v);
            } else if ("OrderItemShipGroupAssoc".equals(v.getEntityName())) {
                orderItemShipGroupAssoc.add(v);
            }
        }

        // reserve the inventory
        String productStoreId = cart.getProductStoreId();
        String orderTypeId = cart.getOrderType();
        List resErrorMessages = new LinkedList();
        try {
            Debug.log("Calling reserve inventory...", module);
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

    public static Map processOrderPayments(DispatchContext dctx, Map context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String orderId = (String) context.get("orderId");

        OrderReadHelper orh = new OrderReadHelper(delegator, orderId);
        String productStoreId = orh.getProductStoreId();

        // check if order was already cancelled / rejected
        GenericValue orderHeader = orh.getOrderHeader();
        String orderStatus = orderHeader.getString("statusId");
        if ("ORDER_CANCELLED".equals(orderStatus) || "ORDER_REJECTED".equals(orderStatus)) {
            return ServiceUtil.returnFailure("ERROR: the Order status is "+orderStatus);
        }

        // process the payments
        if (!"PURCHASE_ORDER".equals(orh.getOrderTypeId())) {
            GenericValue productStore = ProductStoreWorker.getProductStore(productStoreId, delegator);
            Map paymentResp = null;
            try {
                Debug.log("Calling process payments...", module);
                //Debug.set(Debug.VERBOSE, true);
                paymentResp = CheckOutHelper.processPayment(orderId, orh.getOrderGrandTotal(), orh.getCurrency(), productStore, userLogin, false, false, dispatcher, delegator);
                //Debug.set(Debug.VERBOSE, false);
            } catch (GeneralException e) {
                Debug.logError(e, module);
                return ServiceUtil.returnError(e.getMessage());
            } catch (GeneralRuntimeException e) {
                Debug.logError(e, module);
                return ServiceUtil.returnError(e.getMessage());
            }

            if (ServiceUtil.isError(paymentResp)) {
                return ServiceUtil.returnError("Error processing payments: ", null, null, paymentResp);
            }
        }
        return ServiceUtil.returnSuccess();
    }

    // sample test services
    public static Map shoppingCartTest(DispatchContext dctx, Map context) {
        Locale locale = (Locale) context.get("locale");
        ShoppingCart cart = new ShoppingCart(dctx.getDelegator(), "9000", "webStore", locale, "USD");
        try {
            cart.addOrIncreaseItem("GZ-1005", null, BigDecimal.ONE, null, null, null, null, null, null, null, "DemoCatalog", null, null, null, null, dctx.getDispatcher());
        } catch (CartItemModifyException e) {
            Debug.logError(e, module);
        } catch (ItemNotFoundException e) {
            Debug.logError(e, module);
        }

        try {
            dctx.getDispatcher().runAsync("shoppingCartRemoteTest", UtilMisc.toMap("cart", cart), true);
        } catch (GenericServiceException e) {
            Debug.logError(e, module);
        }

        return ServiceUtil.returnSuccess();
    }

    public static Map shoppingCartRemoteTest(DispatchContext dctx, Map context) {
        ShoppingCart cart = (ShoppingCart) context.get("cart");
        Debug.log("Product ID : " + cart.findCartItem(0).getProductId(), module);
        return ServiceUtil.returnSuccess();
    }

    /**
     * Service to create a payment using an order payment preference.
     * @return Map
     */
    public static Map createPaymentFromPreference(DispatchContext dctx, Map context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        String orderPaymentPreferenceId = (String) context.get("orderPaymentPreferenceId");
        String paymentRefNum = (String) context.get("paymentRefNum");
        String paymentFromId = (String) context.get("paymentFromId");
        String comments = (String) context.get("comments");
        Timestamp eventDate = (Timestamp) context.get("eventDate");
        if (UtilValidate.isEmpty(eventDate)) {
            eventDate = UtilDateTime.nowTimestamp();
        }
        try {
            // get the order payment preference
            GenericValue orderPaymentPreference = delegator.findByPrimaryKey("OrderPaymentPreference", UtilMisc.toMap("orderPaymentPreferenceId", orderPaymentPreferenceId));
            if (orderPaymentPreference == null) {
                return ServiceUtil.returnError("Failed to create Payment: Cannot find OrderPaymentPreference with orderPaymentPreferenceId " + orderPaymentPreferenceId);
            }

            // get the order header
            GenericValue orderHeader = orderPaymentPreference.getRelatedOne("OrderHeader");
            if (orderHeader == null) {
                return ServiceUtil.returnError("Failed to create Payment: Cannot get related OrderHeader from payment preference");
            }

            // get the store for the order.  It will be used to set the currency
            GenericValue productStore = orderHeader.getRelatedOne("ProductStore");
            if (productStore == null) {
                return ServiceUtil.returnError("Failed to create Payment: Cannot get the ProductStore for the order header");
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
                return ServiceUtil.returnError("Failed to create Payment: payToPartyId not set in ProductStore");
            }

            // create the payment
            Map paymentParams = new HashMap();
            BigDecimal maxAmount = orderPaymentPreference.getBigDecimal("maxAmount");
            //if (maxAmount > 0.0) {
                paymentParams.put("paymentTypeId", "CUSTOMER_PAYMENT");
                paymentParams.put("paymentMethodTypeId", orderPaymentPreference.getString("paymentMethodTypeId"));
                paymentParams.put("paymentPreferenceId", orderPaymentPreference.getString("orderPaymentPreferenceId"));
                paymentParams.put("amount", maxAmount);
                paymentParams.put("statusId", "PMNT_RECEIVED");
                paymentParams.put("effectiveDate", eventDate);
                paymentParams.put("partyIdFrom", paymentFromId);
                paymentParams.put("currencyUomId", productStore.getString("defaultCurrencyUomId"));
                paymentParams.put("partyIdTo", payToPartyId);
            /*}
            else {
                paymentParams.put("paymentTypeId", "CUSTOMER_REFUND"); // JLR 17/7/4 from a suggestion of Si cf. https://issues.apache.org/jira/browse/OFBIZ-828#action_12483045
                paymentParams.put("paymentMethodTypeId", orderPaymentPreference.getString("paymentMethodTypeId")); // JLR 20/7/4 Finally reverted for now, I prefer to see an amount in payment, even negative
                paymentParams.put("paymentPreferenceId", orderPaymentPreference.getString("orderPaymentPreferenceId"));
                paymentParams.put("amount", Double.valueOf(Math.abs(maxAmount)));
                paymentParams.put("statusId", "PMNT_RECEIVED");
                paymentParams.put("effectiveDate", UtilDateTime.nowTimestamp());
                paymentParams.put("partyIdFrom", payToPartyId);
                paymentParams.put("currencyUomId", productStore.getString("defaultCurrencyUomId"));
                paymentParams.put("partyIdTo", billToParty.getString("partyId"));
            }*/
            if (paymentRefNum != null) {
                paymentParams.put("paymentRefNum", paymentRefNum);
            }
            if (comments != null) {
                paymentParams.put("comments", comments);
            }
            paymentParams.put("userLogin", userLogin);

            return dispatcher.runSync("createPayment", paymentParams);

        } catch (GenericEntityException ex) {
            Debug.logError(ex, "Unable to create payment using payment preference.", module);
            return(ServiceUtil.returnError(ex.getMessage()));
        } catch (GenericServiceException ex) {
            Debug.logError(ex, "Unable to create payment using payment preference.", module);
            return(ServiceUtil.returnError(ex.getMessage()));
        }
    }

    public static Map massChangeApproved(DispatchContext dctx, Map context) {
        return massChangeOrderStatus(dctx, context, "ORDER_APPROVED");
    }

    public static Map massCancelOrders(DispatchContext dctx, Map context) {
        return massChangeItemStatus(dctx, context, "ITEM_CANCELLED");
    }

    public static Map massRejectOrders(DispatchContext dctx, Map context) {
        return massChangeItemStatus(dctx, context, "ITEM_REJECTED");
    }

    public static Map massHoldOrders(DispatchContext dctx, Map context) {
        return massChangeOrderStatus(dctx, context, "ORDER_HOLD");
    }

    public static Map massProcessOrders(DispatchContext dctx, Map context) {
        return massChangeOrderStatus(dctx, context, "ORDER_PROCESSING");
    }

    public static Map massChangeOrderStatus(DispatchContext dctx, Map context, String statusId) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        List orderIds = (List) context.get("orderIdList");
        Iterator i = orderIds.iterator();
        while (i.hasNext()) {
            String orderId = (String) i.next();
            if (UtilValidate.isEmpty(orderId)) {
                continue;
            }
            GenericValue orderHeader = null;
            try {
                orderHeader = delegator.findByPrimaryKey("OrderHeader", UtilMisc.toMap("orderId", orderId));
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
                return ServiceUtil.returnError(e.getMessage());
            }
            if (orderHeader == null) {
                return ServiceUtil.returnError("Order #" + orderId + " was not found.");
            }

            Map ctx = FastMap.newInstance();
            ctx.put("statusId", statusId);
            ctx.put("orderId", orderId);
            ctx.put("setItemStatus", "Y");
            ctx.put("userLogin", userLogin);
            Map resp = null;
            try {
                resp = dispatcher.runSync("changeOrderStatus", ctx);
            } catch (GenericServiceException e) {
                Debug.logError(e, module);
                return ServiceUtil.returnError(e.getMessage());
            }
            if (ServiceUtil.isError(resp)) {
                return ServiceUtil.returnError("Error changing order status: ", null, null, resp);
            }
        }
        return ServiceUtil.returnSuccess();
    }

    public static Map massChangeItemStatus(DispatchContext dctx, Map context, String statusId) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        List orderIds = (List) context.get("orderIdList");
        Iterator i = orderIds.iterator();
        while (i.hasNext()) {
            String orderId = (String) i.next();
            if (UtilValidate.isEmpty(orderId)) {
                continue;
            }
            GenericValue orderHeader = null;
            try {
                orderHeader = delegator.findByPrimaryKey("OrderHeader", UtilMisc.toMap("orderId", orderId));
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
                return ServiceUtil.returnError(e.getMessage());
            }
            if (orderHeader == null) {
                return ServiceUtil.returnError("Order #" + orderId + " was not found.");
            }

            Map ctx = FastMap.newInstance();
            ctx.put("statusId", statusId);
            ctx.put("orderId", orderId);
            ctx.put("userLogin", userLogin);
            Map resp = null;
            try {
                resp = dispatcher.runSync("changeOrderItemStatus", ctx);
            } catch (GenericServiceException e) {
                Debug.logError(e, module);
                return ServiceUtil.returnError(e.getMessage());
            }
            if (ServiceUtil.isError(resp)) {
                return ServiceUtil.returnError("Error changing order item status: ", null, null, resp);
            }
        }
        return ServiceUtil.returnSuccess();
    }

    public static Map massQuickShipOrders(DispatchContext dctx, Map context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        List orderIds = (List) context.get("orderIdList");

        for (Object orderId : orderIds) {
            if (UtilValidate.isEmpty(orderId)) {
                continue;
            }
            Map ctx = FastMap.newInstance();
            ctx.put("userLogin", userLogin);
            ctx.put("orderId", orderId);

            Map resp = null;
            try {
                resp = dispatcher.runSync("quickShipEntireOrder", ctx);
            } catch (GenericServiceException e) {
                Debug.logError(e, module);
                return ServiceUtil.returnError(e.getMessage());
            }
            if (ServiceUtil.isError(resp)) {
                return ServiceUtil.returnError("Error quickShipEntireOrder for order: ", null, null, resp);
            }
        }
        return ServiceUtil.returnSuccess();
    }

    public static Map massPickOrders(DispatchContext dctx, Map context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        // grouped by facility
        Map facilityOrdersMap = FastMap.newInstance();

        // make the list per facility
        List orderIds = (List) context.get("orderIdList");
        Iterator i = orderIds.iterator();
        while (i.hasNext()) {
            String orderId = (String) i.next();
            if (UtilValidate.isEmpty(orderId)) {
                continue;
            }
            List invInfo = null;
            try {
                invInfo = delegator.findByAnd("OrderItemAndShipGrpInvResAndItem",
                        UtilMisc.toMap("orderId", orderId, "statusId", "ITEM_APPROVED"));
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
                return ServiceUtil.returnError(e.getMessage());
            }
            if (invInfo != null) {
                Iterator ii = invInfo.iterator();
                while (ii.hasNext()) {
                    GenericValue inv = (GenericValue) ii.next();
                    String facilityId = inv.getString("facilityId");
                    List orderIdsByFacility = (List) facilityOrdersMap.get(facilityId);
                    if (orderIdsByFacility == null) {
                        orderIdsByFacility = new ArrayList();
                    }
                    orderIdsByFacility.add(orderId);
                    facilityOrdersMap.put(facilityId, orderIdsByFacility);
                }
            }
        }

        // now create the pick lists for each facility
        Iterator fi = facilityOrdersMap.keySet().iterator();
        while (fi.hasNext()) {
            String facilityId = (String) fi.next();
            List orderIdList = (List) facilityOrdersMap.get(facilityId);

            Map ctx = FastMap.newInstance();
            ctx.put("userLogin", userLogin);
            ctx.put("orderIdList", orderIdList);
            ctx.put("facilityId", facilityId);

            Map resp = null;
            try {
                resp = dispatcher.runSync("createPicklistFromOrders", ctx);
            } catch (GenericServiceException e) {
                Debug.logError(e, module);
                return ServiceUtil.returnError(e.getMessage());
            }
            if (ServiceUtil.isError(resp)) {
                return ServiceUtil.returnError("Error creating picklist from orders: ", null, null, resp);
            }
        }

        return ServiceUtil.returnSuccess();
    }

    public static Map massPrintOrders(DispatchContext dctx, Map context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String screenLocation = (String) context.get("screenLocation");
        String printerName = (String) context.get("printerName");

        // make the list per facility
        List orderIds = (List) context.get("orderIdList");
        Iterator i = orderIds.iterator();
        while (i.hasNext()) {
            String orderId = (String) i.next();
            if (UtilValidate.isEmpty(orderId)) {
                continue;
            }
            Map ctx = FastMap.newInstance();
            ctx.put("userLogin", userLogin);
            ctx.put("screenLocation", screenLocation);
            //ctx.put("contentType", "application/postscript");
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

    public static Map massCreateFileForOrders(DispatchContext dctx, Map context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String screenLocation = (String) context.get("screenLocation");

        // make the list per facility
        List orderIds = (List) context.get("orderIdList");
        Iterator i = orderIds.iterator();
        while (i.hasNext()) {
            String orderId = (String) i.next();
            if (UtilValidate.isEmpty(orderId)) {
                continue;
            }
            Map ctx = FastMap.newInstance();
            ctx.put("userLogin", userLogin);
            ctx.put("screenLocation", screenLocation);
            //ctx.put("contentType", "application/postscript");
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

    public static Map massCancelRemainingPurchaseOrderItems(DispatchContext dctx, Map context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        List orderIds = (List) context.get("orderIdList");

        for (Object orderId : orderIds) {
            if (UtilValidate.isEmpty(orderId)) {
                continue;
            }
            Map ctx = FastMap.newInstance();
            ctx.put("orderId", orderId);
            ctx.put("userLogin", userLogin);

            Map resp = null;
            try {
                resp = dispatcher.runSync("cancelRemainingPurchaseOrderItems", ctx);
            } catch (GenericServiceException e) {
                Debug.logError(e, module);
                return ServiceUtil.returnError(e.getMessage());
            }
            if (ServiceUtil.isError(resp)) {
                return ServiceUtil.returnError("Error cancelRemainingPurchaseOrderItems for order: ", null, null, resp);
            }
            try {
                resp = dispatcher.runSync("checkOrderItemStatus", ctx);
            } catch (GenericServiceException e) {
                Debug.logError(e, module);
                return ServiceUtil.returnError(e.getMessage());
            }
            if (ServiceUtil.isError(resp)) {
                return ServiceUtil.returnError("Error checkOrderItemStatus for order: ", null, null, resp);
            }
        }
        return ServiceUtil.returnSuccess();
    }

    public static Map checkCreateDropShipPurchaseOrders(DispatchContext ctx, Map context) {
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        // TODO (use the "system" user)
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        String orderId = (String) context.get("orderId");
        OrderReadHelper orh = new OrderReadHelper(delegator, orderId);
        // TODO: skip this if there is already a purchase order associated with the sales order (ship group)

        try {
            // if sales order
            if ("SALES_ORDER".equals(orh.getOrderTypeId())) {
                // get the order's ship groups
                Iterator shipGroups = orh.getOrderItemShipGroups().iterator();
                while (shipGroups.hasNext()) {
                    GenericValue shipGroup = (GenericValue)shipGroups.next();
                    if (!UtilValidate.isEmpty(shipGroup.getString("supplierPartyId"))) {
                        // This ship group is a drop shipment: we create a purchase order for it
                        String supplierPartyId = shipGroup.getString("supplierPartyId");
                        // create the cart
                        ShoppingCart cart = new ShoppingCart(delegator, orh.getProductStoreId(), null, orh.getCurrency());
                        cart.setOrderType("PURCHASE_ORDER");
                        cart.setBillToCustomerPartyId(cart.getBillFromVendorPartyId()); //Company
                        cart.setBillFromVendorPartyId(supplierPartyId);
                        cart.setOrderPartyId(supplierPartyId);
                        // Get the items associated to it and create po
                        List items = orh.getValidOrderItems(shipGroup.getString("shipGroupSeqId"));
                        if (!UtilValidate.isEmpty(items)) {
                            Iterator itemsIt = items.iterator();
                            while (itemsIt.hasNext()) {
                                GenericValue item = (GenericValue)itemsIt.next();
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
                                    // TODO: we should consider also the ship group in the association between sales and purchase orders
                                } catch (Exception e) {
                                    return ServiceUtil.returnError("The following error occurred creating drop shipments for order [" + orderId + "]: " + e.getMessage());
                                }
                            }
                        }

                        // If there are indeed items to drop ship, then create the purchase order
                        if (!UtilValidate.isEmpty(cart.items())) {
                            // set checkout options
                            cart.setDefaultCheckoutOptions(dispatcher);
                            // the shipping address is the one of the customer
                            cart.setShippingContactMechId(shipGroup.getString("contactMechId"));
                            // create the order
                            CheckOutHelper coh = new CheckOutHelper(dispatcher, delegator, cart);
                            Map resultOrderMap = coh.createOrder(userLogin);
                            String purchaseOrderId = (String)resultOrderMap.get("orderId");

                            // TODO: associate the new purchase order with the sales order (ship group)
                        } else {
                            // if there are no items to drop ship, then clear out the supplier partyId
                            Debug.logWarning("No drop ship items found for order [" + shipGroup.getString("orderId") + "] and ship group [" + shipGroup.getString("shipGroupSeqId") + "] and supplier party [" + shipGroup.getString("supplierPartyId") + "].  Supplier party information will be cleared for this ship group", module);
                            shipGroup.set("supplierPartyId", null);
                            shipGroup.store();

                        }
                    }
                }
            }
        } catch (Exception exc) {
            // TODO: imporve error handling
            return ServiceUtil.returnError("The following error occurred creating drop shipments for order [" + orderId + "]: " + exc.getMessage());
        }

        return ServiceUtil.returnSuccess();
    }

    public static Map updateOrderPaymentPreference(DispatchContext dctx, Map context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        String orderPaymentPreferenceId = (String) context.get("orderPaymentPreferenceId");
        String checkOutPaymentId = (String) context.get("checkOutPaymentId");
        String statusId = (String) context.get("statusId");
        try {
            GenericValue opp = delegator.findByPrimaryKey("OrderPaymentPreference", UtilMisc.toMap("orderPaymentPreferenceId", orderPaymentPreferenceId));
            String paymentMethodId = null;
            String paymentMethodTypeId = null;

            // The checkOutPaymentId is either a paymentMethodId or paymentMethodTypeId
            // the original method did a "\d+" regexp to decide which is the case, this version is more explicit with its lookup of PaymentMethodType
            if (checkOutPaymentId != null) {
                List paymentMethodTypes = delegator.findList("PaymentMethodType", null, null, null, null, true);
                for (Iterator iter = paymentMethodTypes.iterator(); iter.hasNext();) {
                    GenericValue type = (GenericValue) iter.next();
                    if (type.get("paymentMethodTypeId").equals(checkOutPaymentId)) {
                        paymentMethodTypeId = (String) type.get("paymentMethodTypeId");
                        break;
                    }
                }
                if (paymentMethodTypeId == null) {
                    GenericValue method = delegator.findByPrimaryKey("PaymentMethod", UtilMisc.toMap("paymentMethodTypeId", paymentMethodTypeId));
                    paymentMethodId = checkOutPaymentId;
                    paymentMethodTypeId = (String) method.get("paymentMethodTypeId");
                }
            }

            Map results = ServiceUtil.returnSuccess();
            if (UtilValidate.isNotEmpty(statusId) && statusId.equalsIgnoreCase("PAYMENT_CANCELLED")) {
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
     * @param dctx
     * @param context
     * @return
     */
    public static Map generateReqsFromCancelledPOItems(DispatchContext dctx, Map context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");

        String orderId = (String) context.get("orderId");
        String facilityId = (String) context.get("facilityId");

        try {

            GenericValue orderHeader = delegator.findByPrimaryKey("OrderHeader", UtilMisc.toMap("orderId", orderId));

            if (UtilValidate.isEmpty(orderHeader)) {
                String errorMessage = UtilProperties.getMessage(resource_error, "OrderErrorOrderIdNotFound", UtilMisc.toMap("orderId", orderId), locale);
                Debug.logError(errorMessage, module);
                return ServiceUtil.returnError(errorMessage);
            }

            if (! "PURCHASE_ORDER".equals(orderHeader.getString("orderTypeId"))) {
                String errorMessage = UtilProperties.getMessage(resource_error, "ProductErrorOrderNotPurchaseOrder", UtilMisc.toMap("orderId", orderId), locale);
                Debug.logError(errorMessage, module);
                return ServiceUtil.returnError(errorMessage);
            }

            // Build a map of productId -> quantity cancelled over all order items
            Map productRequirementQuantities = new HashMap();
            List orderItems = orderHeader.getRelated("OrderItem");
            Iterator oiit = orderItems.iterator();
            while (oiit.hasNext()) {
                GenericValue orderItem = (GenericValue) oiit.next();
                if (! "PRODUCT_ORDER_ITEM".equals(orderItem.getString("orderItemTypeId"))) continue;

                // Get the cancelled quantity for the item
                BigDecimal orderItemCancelQuantity = BigDecimal.ZERO;
                if (! UtilValidate.isEmpty(orderItem.get("cancelQuantity"))) {
                    orderItemCancelQuantity = orderItem.getBigDecimal("cancelQuantity");
                }

                if (orderItemCancelQuantity.compareTo(BigDecimal.ZERO) <= 0) continue;

                String productId = orderItem.getString("productId");
                if (productRequirementQuantities.containsKey(productId)) {
                    orderItemCancelQuantity = orderItemCancelQuantity.add((BigDecimal) productRequirementQuantities.get(productId));
                }
                productRequirementQuantities.put(productId, orderItemCancelQuantity);

            }

            // Generate requirements for each of the product quantities
            Iterator cqit = productRequirementQuantities.keySet().iterator();
            while (cqit.hasNext()) {
                String productId = (String) cqit.next();
                BigDecimal requiredQuantity = (BigDecimal) productRequirementQuantities.get(productId);
                Map createRequirementResult = dispatcher.runSync("createRequirement", UtilMisc.<String, Object>toMap("requirementTypeId", "PRODUCT_REQUIREMENT", "facilityId", facilityId, "productId", productId, "quantity", requiredQuantity, "userLogin", userLogin));
                if (ServiceUtil.isError(createRequirementResult)) return createRequirementResult;
            }

        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        } catch (GenericServiceException se) {
            Debug.logError(se, module);
            return ServiceUtil.returnError(se.getMessage());
        }

        return ServiceUtil.returnSuccess();
    }

    /**
     * Cancels remaining (unreceived) quantities for items of an order. Does not consider received-but-rejected quantities.
     * @param dctx
     * @param context
     * @return
     */
    public static Map cancelRemainingPurchaseOrderItems(DispatchContext dctx, Map context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");

        String orderId = (String) context.get("orderId");

        try {

            GenericValue orderHeader = delegator.findByPrimaryKey("OrderHeader", UtilMisc.toMap("orderId", orderId));

            if (UtilValidate.isEmpty(orderHeader)) {
                String errorMessage = UtilProperties.getMessage(resource_error, "OrderErrorOrderIdNotFound", UtilMisc.toMap("orderId", orderId), locale);
                Debug.logError(errorMessage, module);
                return ServiceUtil.returnError(errorMessage);
            }

            if (! "PURCHASE_ORDER".equals(orderHeader.getString("orderTypeId"))) {
                String errorMessage = UtilProperties.getMessage(resource_error, "OrderErrorOrderNotPurchaseOrder", UtilMisc.toMap("orderId", orderId), locale);
                Debug.logError(errorMessage, module);
                return ServiceUtil.returnError(errorMessage);
            }

            List orderItems = orderHeader.getRelated("OrderItem");
            Iterator oiit = orderItems.iterator();
            while (oiit.hasNext()) {
                GenericValue orderItem = (GenericValue) oiit.next();
                if (! "PRODUCT_ORDER_ITEM".equals(orderItem.getString("orderItemTypeId"))) continue;

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
                List shipmentReceipts = orderItem.getRelated("ShipmentReceipt");
                BigDecimal receivedQuantity = BigDecimal.ZERO;
                Iterator srit = shipmentReceipts.iterator();
                while (srit.hasNext()) {
                    GenericValue shipmentReceipt = (GenericValue) srit.next();
                    if (! UtilValidate.isEmpty(shipmentReceipt.get("quantityAccepted"))) {
                        receivedQuantity = receivedQuantity.add(shipmentReceipt.getBigDecimal("quantityAccepted"));
                    }
                }

                BigDecimal quantityToCancel = orderItemQuantity.subtract(orderItemCancelQuantity).subtract(receivedQuantity);
                if (quantityToCancel.compareTo(BigDecimal.ZERO) > 0) {
                Map cancelOrderItemResult = dispatcher.runSync("cancelOrderItem", UtilMisc.toMap("orderId", orderId, "orderItemSeqId", orderItem.get("orderItemSeqId"), "cancelQuantity", quantityToCancel, "userLogin", userLogin));
                if (ServiceUtil.isError(cancelOrderItemResult)) return cancelOrderItemResult;
                }

                // If there's nothing to cancel, the item should be set to completed, if it isn't already
                orderItem.refresh();
                if ("ITEM_APPROVED".equals(orderItem.getString("statusId"))) {
                    Map changeOrderItemStatusResult = dispatcher.runSync("changeOrderItemStatus", UtilMisc.toMap("orderId", orderId, "orderItemSeqId", orderItem.get("orderItemSeqId"), "statusId", "ITEM_COMPLETED", "userLogin", userLogin));
                    if (ServiceUtil.isError(changeOrderItemStatusResult)) return changeOrderItemStatusResult;
                }
            }

        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        } catch (GenericServiceException se) {
            Debug.logError(se, module);
            return ServiceUtil.returnError(se.getMessage());
        }

        return ServiceUtil.returnSuccess();
    }

    // create simple non-product order
    public static Map createSimpleNonProductSalesOrder(DispatchContext dctx, Map context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();

        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");

        String paymentMethodId = (String) context.get("paymentMethodId");
        String productStoreId = (String) context.get("productStoreId");
        String currency = (String) context.get("currency");
        String partyId = (String) context.get("partyId");
        Map itemMap = (Map) context.get("itemMap");

        ShoppingCart cart = new ShoppingCart(delegator, productStoreId, null, locale, currency);
        try {
            cart.setUserLogin(userLogin, dispatcher);
        } catch (CartItemModifyException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }
        cart.setOrderType("SALES_ORDER");
        cart.setOrderPartyId(partyId);

        Iterator i = itemMap.keySet().iterator();
        while (i.hasNext()) {
            String item = (String) i.next();
            BigDecimal price = (BigDecimal) itemMap.get(item);
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
        Map createResp;
        try {
            createResp = dispatcher.runSync("createOrderFromShoppingCart", UtilMisc.toMap("shoppingCart", cart), 90, true);
        } catch (GenericServiceException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }
        if (ServiceUtil.isError(createResp)) {
            return createResp;
        }

        // auth the order (new tx)
        Map authResp;
        try {
            authResp = dispatcher.runSync("callProcessOrderPayments", UtilMisc.toMap("shoppingCart", cart), 180, true);
        } catch (GenericServiceException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }
        if (ServiceUtil.isError(authResp)) {
            return authResp;
        }

        Map result = ServiceUtil.returnSuccess();
        result.put("orderId", createResp.get("orderId"));
        return result;
    }

    // generic method for creating an order from a shopping cart
    public static Map createOrderFromShoppingCart(DispatchContext dctx, Map context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();

        ShoppingCart cart = (ShoppingCart) context.get("shoppingCart");
        GenericValue userLogin = cart.getUserLogin();

        CheckOutHelper coh = new CheckOutHelper(dispatcher, delegator, cart);
        Map createOrder = coh.createOrder(userLogin);
        if (ServiceUtil.isError(createOrder)) {
            return createOrder;
        }
        String orderId = (String) createOrder.get("orderId");

        Map result = ServiceUtil.returnSuccess();
        result.put("shoppingCart", cart);
        result.put("orderId", orderId);
        return result;
    }

    // generic method for processing an order's payment(s)
    public static Map callProcessOrderPayments(DispatchContext dctx, Map context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();

        Transaction trans = null;
        try {
            // disable transaction procesing
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
                Map payResp;
                try {
                    payResp = coh.processPayment(productStore, userLogin, false, manualHold.booleanValue());
                } catch (GeneralException e) {
                    Debug.logError(e, module);
                    return ServiceUtil.returnError(e.getMessage());
                }
                if (ServiceUtil.isError(payResp)) {
                    return ServiceUtil.returnError("Error processing order payments: ", null, null, payResp);
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
    public static Map getOrderItemInvoicedAmountAndQuantity(DispatchContext dctx, Map context) {
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get("locale");

        String orderId = (String) context.get("orderId");
        String orderItemSeqId = (String) context.get("orderItemSeqId");

        GenericValue orderHeader = null;
        GenericValue orderItemToCheck = null;
        BigDecimal orderItemTotalValue = ZERO;
        BigDecimal invoicedQuantity = ZERO; // Quantity invoiced for the target order item
        try {

            orderHeader = delegator.findByPrimaryKey("OrderHeader", UtilMisc.toMap("orderId", orderId));
            if (UtilValidate.isEmpty(orderHeader)) {
                String errorMessage = UtilProperties.getMessage(resource_error, "OrderErrorOrderIdNotFound", context, locale);
                Debug.logError(errorMessage, module);
                return ServiceUtil.returnError(errorMessage);
            }
            orderItemToCheck = delegator.findByPrimaryKey("OrderItem", UtilMisc.toMap("orderId", orderId, "orderItemSeqId", orderItemSeqId));
            if (UtilValidate.isEmpty(orderItemToCheck)) {
                String errorMessage = UtilProperties.getMessage(resource_error, "OrderErrorOrderItemNotFound", context, locale);
                Debug.logError(errorMessage, module);
                return ServiceUtil.returnError(errorMessage);
            }

            BigDecimal orderItemsSubtotal = ZERO; // Aggregated value of order items, non-tax and non-shipping item-level adjustments
            BigDecimal invoicedTotal = ZERO; // Amount invoiced for the target order item
            BigDecimal itemAdjustments = ZERO; // Item-level tax- and shipping-adjustments

            // Aggregate the order items subtotal
            List orderItems = orderHeader.getRelated("OrderItem", UtilMisc.toList("orderItemSeqId"));
            Iterator oit = orderItems.iterator();
            while (oit.hasNext()) {
                GenericValue orderItem = (GenericValue) oit.next();

                // Look at the orderItemBillings to discover the amount and quantity ever invoiced for this order item
                List orderItemBillings = delegator.findByAnd("OrderItemBilling", UtilMisc.toMap("orderId", orderId, "orderItemSeqId", orderItem.get("orderItemSeqId")));
                Iterator oibit = orderItemBillings.iterator();
                while (oibit.hasNext()) {
                    GenericValue orderItemBilling = (GenericValue) oibit.next();
                    BigDecimal quantity = orderItemBilling.getBigDecimal("quantity");
                    BigDecimal amount = orderItemBilling.getBigDecimal("amount").setScale(orderDecimals, orderRounding);
                    if (UtilValidate.isEmpty(invoicedQuantity) || UtilValidate.isEmpty(amount)) continue;

                    // Add the item base amount to the subtotal
                    orderItemsSubtotal = orderItemsSubtotal.add(quantity.multiply(amount));

                    // If the item is the target order item, add the invoiced quantity and amount to their respective totals
                    if (orderItemSeqId.equals(orderItem.get("orderItemSeqId"))) {
                        invoicedQuantity = invoicedQuantity.add(quantity);
                        invoicedTotal = invoicedTotal.add(quantity.multiply(amount));
                    }
                }

                // Retrieve the adjustments for this item
                List orderAdjustments = delegator.findByAnd("OrderAdjustment", UtilMisc.toMap("orderId", orderId, "orderItemSeqId", orderItem.get("orderItemSeqId")));
                Iterator oait = orderAdjustments.iterator();
                while (oait.hasNext()) {
                    GenericValue orderAdjustment = (GenericValue) oait.next();
                    String orderAdjustmentTypeId = orderAdjustment.getString("orderAdjustmentTypeId");

                    // Look at the orderAdjustmentBillings to discove the amount ever invoiced for this order adjustment
                    List orderAdjustmentBillings = delegator.findByAnd("OrderAdjustmentBilling", UtilMisc.toMap("orderAdjustmentId", orderAdjustment.get("orderAdjustmentId")));
                    Iterator oabit = orderAdjustmentBillings.iterator();
                    while (oabit.hasNext()) {
                        GenericValue orderAjustmentBilling = (GenericValue) oabit.next();
                        BigDecimal amount = orderAjustmentBilling.getBigDecimal("amount").setScale(orderDecimals, orderRounding);
                        if (UtilValidate.isEmpty(amount)) continue;

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
            List orderHeaderAdjustments = delegator.findByAnd("OrderAdjustment", UtilMisc.toMap("orderId", orderId, "orderItemSeqId", "_NA_"));
            Iterator ohait = orderHeaderAdjustments.iterator();
            while (ohait.hasNext()) {
                GenericValue orderHeaderAdjustment = (GenericValue) ohait.next();
                List orderHeaderAdjustmentBillings = delegator.findByAnd("OrderAdjustmentBilling", UtilMisc.toMap("orderAdjustmentId", orderHeaderAdjustment.get("orderAdjustmentId")));
                Iterator ohabit = orderHeaderAdjustmentBillings.iterator();
                while (ohabit.hasNext()) {
                    GenericValue orderHeaderAdjustmentBilling = (GenericValue) ohabit.next();
                    BigDecimal amount = orderHeaderAdjustmentBilling.getBigDecimal("amount").setScale(orderDecimals, orderRounding);
                    if (UtilValidate.isEmpty(amount)) continue;
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

        Map result = ServiceUtil.returnSuccess();
        result.put("invoicedAmount", orderItemTotalValue.setScale(orderDecimals, orderRounding));
        result.put("invoicedQuantity", invoicedQuantity.setScale(orderDecimals, orderRounding));
        return result;
    }

    public static Map setOrderPaymentStatus(DispatchContext ctx, Map context) {
        LocalDispatcher dispatcher = ctx.getDispatcher();
        Delegator delegator = ctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String orderPaymentPreferenceId = (String) context.get("orderPaymentPreferenceId");
        String changeReason = (String) context.get("changeReason");
        Map successResult = ServiceUtil.returnSuccess();
        Locale locale = (Locale) context.get("locale");
        try {
            GenericValue orderPaymentPreference = delegator.findByPrimaryKey("OrderPaymentPreference", UtilMisc.toMap("orderPaymentPreferenceId", orderPaymentPreferenceId));
            String orderId = orderPaymentPreference.getString("orderId");
            String statusUserLogin = orderPaymentPreference.getString("createdByUserLogin");
            GenericValue orderHeader = delegator.findByPrimaryKey("OrderHeader", UtilMisc.toMap("orderId", orderId));
            if (orderHeader == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderErrorCouldNotChangeOrderStatusOrderCannotBeFound", locale));
            }
            String statusId = orderPaymentPreference.getString("statusId");
            if (Debug.verboseOn()) Debug.logVerbose("[OrderServices.setOrderPaymentStatus] : Setting Order Payment Status to : " + statusId, module);
            // create a order payment status
            GenericValue orderStatus = delegator.makeValue("OrderStatus");
            orderStatus.put("statusId", statusId);
            orderStatus.put("orderId", orderId);
            orderStatus.put("orderPaymentPreferenceId", orderPaymentPreferenceId);
            orderStatus.put("statusUserLogin", statusUserLogin);
            orderStatus.put("changeReason", changeReason);

            // Check that the status has actually changed before creating a new record
            List<GenericValue> previousStatusList = delegator.findByAnd("OrderStatus", UtilMisc.toMap("orderId", orderId, "orderPaymentPreferenceId", orderPaymentPreferenceId), UtilMisc.toList("-statusDatetime"));
            GenericValue previousStatus = EntityUtil.getFirst(previousStatusList);
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
            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderErrorCouldNotChangeOrderStatus", locale) + e.getMessage() + ").");
        }

        return ServiceUtil.returnSuccess();
    }
    public static Map runSubscriptionAutoReorders(DispatchContext dctx, Map context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();

        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");
        int count = 0;
        Map result = null;

        boolean beganTransaction = false;
        try {
            beganTransaction = TransactionUtil.begin();

            List exprs = UtilMisc.toList(EntityCondition.makeCondition("automaticExtend", EntityOperator.EQUALS, "Y"),
                    EntityCondition.makeCondition("orderId", EntityOperator.NOT_EQUAL, null),
                    EntityCondition.makeCondition("productId", EntityOperator.NOT_EQUAL, null));
            EntityCondition cond = EntityCondition.makeCondition(exprs, EntityOperator.AND);
            EntityListIterator eli = null;
            eli = delegator.find("Subscription", cond, null, null, null, null);

            if (eli != null) {
                GenericValue subscription;
                while (((subscription = (GenericValue) eli.next()) != null)) {

                    Calendar endDate = Calendar.getInstance();
                    endDate.setTime(UtilDateTime.nowTimestamp());
                    //check if the thruedate - cancel period (if provided) is earlier than todays date
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
                            Debug.logWarning("Don't know anything about useTimeUomId [" + subscription.getString("canclAutmExtTimeUomId") + "], defaulting to month", module);
                        }

                        endDate.add(field, Integer.valueOf(subscription.getString("canclAutmExtTime")).intValue());
                    }

                    Calendar endDateSubscription = Calendar.getInstance();
                    endDateSubscription.setTime(subscription.getTimestamp("thruDate"));

                    if (endDate.before(endDateSubscription)) {
                        // nor expired yet.....
                        continue;
                    }

                    result = dispatcher.runSync("loadCartFromOrder", UtilMisc.toMap("orderId", subscription.get("orderId"), "userLogin", userLogin));
                    ShoppingCart cart = (ShoppingCart) result.get("shoppingCart");

                    // only keep the orderitem with the related product.
                    List cartItems = cart.items();
                    Iterator ci = cartItems.iterator();
                    while (ci.hasNext()) {
                        ShoppingCartItem shoppingCartItem = (ShoppingCartItem) ci.next();
                        if (!subscription.get("productId").equals(shoppingCartItem.getProductId())) {
                            cart.removeCartItem(shoppingCartItem, dispatcher);
                        }
                    }

                    CheckOutHelper helper = new CheckOutHelper(dispatcher, delegator, cart);

                    // store the order
                    Map createResp = helper.createOrder(userLogin);
                    if (createResp != null && ServiceUtil.isError(createResp)) {
                        Debug.logError("Cannot create order for shopping list - " + subscription, module);
                    } else {
                        String orderId = (String) createResp.get("orderId");

                        // authorize the payments
                        Map payRes = null;
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
                        dispatcher.runAsync("sendOrderPayRetryNotification", UtilMisc.toMap("orderId", orderId));
                        count++;
                    }
                }
                eli.close();
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

            String errMsg = "Error while creating new shopping list based automatic reorder" + e.toString();
            Debug.logError(e, errMsg, module);
            return ServiceUtil.returnError(errMsg);
        } finally {
            try {
                // only commit the transaction if we started one... this will throw an exception if it fails
                TransactionUtil.commit(beganTransaction);
            } catch (GenericEntityException e) {
                Debug.logError(e, "Could not commit transaction for creating new shopping list based automatic reorder", module);
            }
        }
        return ServiceUtil.returnSuccess("runSubscriptionAutoReorders finished, " + count + " subscription extended.");
    }

    public static Map setShippingInstructions(DispatchContext dctx, Map context) {
        Delegator delegator = dctx.getDelegator();
        String orderId = (String) context.get("orderId");
        String shipGroupSeqId = (String) context.get("shipGroupSeqId");
        String shippingInstructions = (String) context.get("shippingInstructions");
        try {
            GenericValue orderItemShipGroup = EntityUtil.getFirst(delegator.findByAnd("OrderItemShipGroup", UtilMisc.toMap("orderId", orderId,"shipGroupSeqId",shipGroupSeqId)));
            orderItemShipGroup.set("shippingInstructions", shippingInstructions);
            orderItemShipGroup.store();
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
        return ServiceUtil.returnSuccess();
    }

    public static Map setGiftMessage(DispatchContext dctx, Map context) {
        Delegator delegator = dctx.getDelegator();
        String orderId = (String) context.get("orderId");
        String shipGroupSeqId = (String) context.get("shipGroupSeqId");
        String giftMessage = (String) context.get("giftMessage");
        try {
            GenericValue orderItemShipGroup = EntityUtil.getFirst(delegator.findByAnd("OrderItemShipGroup", UtilMisc.toMap("orderId", orderId,"shipGroupSeqId",shipGroupSeqId)));
            orderItemShipGroup.set("giftMessage", giftMessage);
            orderItemShipGroup.set("isGift", "Y");
            orderItemShipGroup.store();
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> createAlsoBoughtProductAssocs(DispatchContext dctx, Map context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        // All orders with an entryDate > orderEntryFromDateTime will be processed
        Timestamp orderEntryFromDateTime = (Timestamp) context.get("orderEntryFromDateTime");
        // If true all orders ever created will be processed and any pre-existing ALSO_BOUGHT ProductAssocs will be expired
        boolean processAllOrders = context.get("processAllOrders") == null ? false : (Boolean) context.get("processAllOrders");
        if (orderEntryFromDateTime == null && !processAllOrders) {
            // No from date supplied, check to see when this service last ran and use the startDateTime
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
        EntityListIterator eli = null;
        try {
            List<EntityExpr> orderCondList = UtilMisc.toList(EntityCondition.makeCondition("orderTypeId", "SALES_ORDER"));
            if (!processAllOrders && orderEntryFromDateTime != null) {
                orderCondList.add(EntityCondition.makeCondition("entryDate", EntityOperator.GREATER_THAN, orderEntryFromDateTime));
            }
            EntityCondition cond = EntityCondition.makeCondition(orderCondList);
            eli = delegator.find("OrderHeader", cond, null, null, UtilMisc.toList("entryDate ASC"), null);
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }
        if (eli != null) {
            GenericValue orderHeader = null;
            while ((orderHeader = eli.next()) != null) {
                Map svcIn = FastMap.newInstance();
                svcIn.put("userLogin", context.get("userLogin"));
                svcIn.put("orderId", orderHeader.get("orderId"));
                try {
                    dispatcher.runSync("createAlsoBoughtProductAssocsForOrder", svcIn);
                } catch (GenericServiceException e) {
                    Debug.logError(e, module);
                }
            }
            try {
                eli.close();
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
            }
        }
        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> createAlsoBoughtProductAssocsForOrder(DispatchContext dctx, Map context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        String orderId = (String) context.get("orderId");
        OrderReadHelper orh = new OrderReadHelper(delegator, orderId);
        List<GenericValue> orderItems = orh.getOrderItems();
        // In order to improve efficiency a little bit, we will always create the ProductAssoc records
        // with productId < productIdTo when the two are compared.  This way when checking for an existing
        // record we don't have to check both possible combinations of productIds
        TreeSet<String> productIdSet = new TreeSet<String>();
        if (orderItems != null) {
            for (GenericValue orderItem : orderItems) {
                String productId = orderItem.getString("productId");
                if (productId != null) {
                    GenericValue parentProduct = ProductWorker.getParentProduct(productId, delegator);
                    if (parentProduct != null) productId = parentProduct.getString("productId");
                    productIdSet.add(productId);
                }
            }
        }
        TreeSet<String> productIdToSet = new TreeSet<String>(productIdSet);
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
                    existingProductAssoc = EntityUtil.getFirst(delegator.findList("ProductAssoc", cond, null, UtilMisc.toList("fromDate DESC"), null, false));
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
                        dispatcher.runSync("updateProductAssoc", updateCtx);
                    } else {
                        Map<String, Object> createCtx = FastMap.newInstance();
                        createCtx.put("userLogin", context.get("userLogin"));
                        createCtx.put("productId", productId);
                        createCtx.put("productIdTo", productIdTo);
                        createCtx.put("productAssocTypeId", "ALSO_BOUGHT");
                        createCtx.put("fromDate", UtilDateTime.nowTimestamp());
                        createCtx.put("quantity", BigDecimal.ONE);
                        dispatcher.runSync("createProductAssoc", createCtx);
                    }
                } catch (GenericServiceException e) {
                    Debug.logError(e, module);
                }
            }
        }

        return ServiceUtil.returnSuccess();
    }
}
