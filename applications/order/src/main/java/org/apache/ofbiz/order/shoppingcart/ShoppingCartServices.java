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
package org.apache.ofbiz.order.shoppingcart;

import java.math.BigDecimal;
import java.math.MathContext;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilFormatOut;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityExpr;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityTypeUtil;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.entity.util.EntityUtilProperties;
import org.apache.ofbiz.order.order.OrderReadHelper;
import org.apache.ofbiz.order.shoppingcart.ShoppingCart.CartShipInfo;
import org.apache.ofbiz.order.shoppingcart.ShoppingCart.CartShipInfo.CartShipItemInfo;
import org.apache.ofbiz.product.config.ProductConfigWorker;
import org.apache.ofbiz.product.config.ProductConfigWrapper;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;

/**
 * Shopping Cart Services
 */
public class ShoppingCartServices {

    public static final String module = ShoppingCartServices.class.getName();
    public static final String resource_error = "OrderErrorUiLabels";

    public static final MathContext generalRounding = new MathContext(10);
    public static Map<String, Object> assignItemShipGroup(DispatchContext dctx, Map<String, Object> context) {
        ShoppingCart cart = (ShoppingCart) context.get("shoppingCart");
        Integer fromGroupIndex = (Integer) context.get("fromGroupIndex");
        Integer toGroupIndex = (Integer) context.get("toGroupIndex");
        Integer itemIndex = (Integer) context.get("itemIndex");
        BigDecimal quantity = (BigDecimal) context.get("quantity");
        Boolean clearEmptyGroups = (Boolean) context.get("clearEmptyGroups");

        if (clearEmptyGroups == null) {
            clearEmptyGroups = Boolean.TRUE;
        }

        Debug.logInfo("From Group - " + fromGroupIndex + " To Group - " + toGroupIndex + "Item - " + itemIndex + "(" + quantity + ")", module);
        if (fromGroupIndex.equals(toGroupIndex)) {
            // nothing to do
            return ServiceUtil.returnSuccess();
        }

        cart.positionItemToGroup(itemIndex.intValue(), quantity,
                fromGroupIndex.intValue(), toGroupIndex.intValue(), clearEmptyGroups.booleanValue());
        Debug.logInfo("Called cart.positionItemToGroup()", module);

        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object>setShippingOptions(DispatchContext dctx, Map<String, Object> context) {
        ShoppingCart cart = (ShoppingCart) context.get("shoppingCart");
        Integer groupIndex = (Integer) context.get("groupIndex");
        String shippingContactMechId = (String) context.get("shippingContactMechId");
        String shipmentMethodString = (String) context.get("shipmentMethodString");
        String shippingInstructions = (String) context.get("shippingInstructions");
        String giftMessage = (String) context.get("giftMessage");
        Boolean maySplit = (Boolean) context.get("maySplit");
        Boolean isGift = (Boolean) context.get("isGift");
        Locale locale = (Locale) context.get("locale");

        ShoppingCart.CartShipInfo csi = cart.getShipInfo(groupIndex.intValue());
        if (csi != null) {
            int idx = groupIndex.intValue();

            if (UtilValidate.isNotEmpty(shipmentMethodString)) {
                int delimiterPos = shipmentMethodString.indexOf('@');
                String shipmentMethodTypeId = null;
                String carrierPartyId = null;

                if (delimiterPos > 0) {
                    shipmentMethodTypeId = shipmentMethodString.substring(0, delimiterPos);
                    carrierPartyId = shipmentMethodString.substring(delimiterPos + 1);
                 }

                cart.setShipmentMethodTypeId(idx, shipmentMethodTypeId);
                cart.setCarrierPartyId(idx, carrierPartyId);
            }

            cart.setShippingInstructions(idx, shippingInstructions);
            cart.setShippingContactMechId(idx, shippingContactMechId);
            cart.setGiftMessage(idx, giftMessage);

            if (maySplit != null) {
                cart.setMaySplit(idx, maySplit);
            }
            if (isGift != null) {
                cart.setIsGift(idx, isGift);
            }
        } else {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderCartShipGroupNotFound", UtilMisc.toMap("groupIndex",groupIndex), locale));
        }
        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object>setPaymentOptions(DispatchContext dctx, Map<String, Object> context) {
        Locale locale = (Locale) context.get("locale");

        return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderServiceNotYetImplemented",locale));
    }

    public static Map<String, Object>setOtherOptions(DispatchContext dctx, Map<String, Object> context) {
        ShoppingCart cart = (ShoppingCart) context.get("shoppingCart");
        String orderAdditionalEmails = (String) context.get("orderAdditionalEmails");
        String correspondingPoId = (String) context.get("correspondingPoId");

        cart.setOrderAdditionalEmails(orderAdditionalEmails);
        if (UtilValidate.isNotEmpty(correspondingPoId)) {
            cart.setPoNumber(correspondingPoId);
        } else {
            cart.setPoNumber(null);
        }

        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object>loadCartFromOrder(DispatchContext dctx, Map<String, Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();

        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String orderId = (String) context.get("orderId");
        Boolean skipInventoryChecks = (Boolean) context.get("skipInventoryChecks");
        Boolean skipProductChecks = (Boolean) context.get("skipProductChecks");
        boolean includePromoItems = Boolean.TRUE.equals(context.get("includePromoItems"));
        Locale locale = (Locale) context.get("locale");
        //FIXME: deepak:Personally I don't like the idea of passing flag but for orderItem quantity calculation we need this flag.
        String createAsNewOrder = (String) context.get("createAsNewOrder");
        List<GenericValue>orderTerms = null;

        if (UtilValidate.isEmpty(skipInventoryChecks)) {
            skipInventoryChecks = Boolean.FALSE;
        }
        if (UtilValidate.isEmpty(skipProductChecks)) {
            skipProductChecks = Boolean.FALSE;
        }

        // get the order header
        GenericValue orderHeader = null;
        try {
            orderHeader = EntityQuery.use(delegator).from("OrderHeader").where("orderId", orderId).queryOne();
            orderTerms = orderHeader.getRelated("OrderTerm", null, null, false);
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        // initial require cart info
        OrderReadHelper orh = new OrderReadHelper(orderHeader);
        String productStoreId = orh.getProductStoreId();
        String orderTypeId = orh.getOrderTypeId();
        String currency = orh.getCurrency();
        String website = orh.getWebSiteId();
        String currentStatusString = orh.getCurrentStatusString();

        // create the cart
        ShoppingCart cart = new ShoppingCart(delegator, productStoreId, website, locale, currency);
        cart.setDoPromotions(!includePromoItems);
        cart.setOrderType(orderTypeId);
        cart.setChannelType(orderHeader.getString("salesChannelEnumId"));
        cart.setInternalCode(orderHeader.getString("internalCode"));
        cart.setOrderDate(UtilDateTime.nowTimestamp());
        cart.setOrderId(orderHeader.getString("orderId"));
        cart.setOrderName(orderHeader.getString("orderName"));
        cart.setOrderStatusId(orderHeader.getString("statusId"));
        cart.setOrderStatusString(currentStatusString);
        cart.setFacilityId(orderHeader.getString("originFacilityId"));

        try {
            cart.setUserLogin(userLogin, dispatcher);
        } catch (CartItemModifyException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        // set the order name
        String orderName = orh.getOrderName();
        if (orderName != null) {
            cart.setOrderName(orderName);
        }

        // set the role information
        GenericValue placingParty = orh.getPlacingParty();
        if (placingParty != null) {
            cart.setPlacingCustomerPartyId(placingParty.getString("partyId"));
        }

        GenericValue billFromParty = orh.getBillFromParty();
        if (billFromParty != null) {
            cart.setBillFromVendorPartyId(billFromParty.getString("partyId"));
        }

        GenericValue billToParty = orh.getBillToParty();
        if (billToParty != null) {
            cart.setBillToCustomerPartyId(billToParty.getString("partyId"));
        }

        GenericValue shipToParty = orh.getShipToParty();
        if (shipToParty != null) {
            cart.setShipToCustomerPartyId(shipToParty.getString("partyId"));
        }

        GenericValue endUserParty = orh.getEndUserParty();
        if (endUserParty != null) {
            cart.setEndUserCustomerPartyId(endUserParty.getString("partyId"));
            cart.setOrderPartyId(endUserParty.getString("partyId"));
        }

        // load order attributes
        List<GenericValue> orderAttributesList = null;
        try {
            orderAttributesList = EntityQuery.use(delegator).from("OrderAttribute").where("orderId", orderId).queryList();
            if (UtilValidate.isNotEmpty(orderAttributesList)) {
                for (GenericValue orderAttr : orderAttributesList) {
                    String name = orderAttr.getString("attrName");
                    String value = orderAttr.getString("attrValue");
                    cart.setOrderAttribute(name, value);
                }
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        // load the payment infos
        List<GenericValue> orderPaymentPrefs = null;
        try {
            List<EntityExpr> exprs = UtilMisc.toList(EntityCondition.makeCondition("orderId", EntityOperator.EQUALS, orderId));
            exprs.add(EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "PAYMENT_RECEIVED"));
            exprs.add(EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "PAYMENT_CANCELLED"));
            exprs.add(EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "PAYMENT_DECLINED"));
            exprs.add(EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "PAYMENT_SETTLED"));
            orderPaymentPrefs = EntityQuery.use(delegator).from("OrderPaymentPreference").where(exprs).queryList();
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }
        if (UtilValidate.isNotEmpty(orderPaymentPrefs)) {
            Iterator<GenericValue> oppi = orderPaymentPrefs.iterator();
            while (oppi.hasNext()) {
                GenericValue opp = oppi.next();
                String paymentId = opp.getString("paymentMethodId");
                if (paymentId == null) {
                    paymentId = opp.getString("paymentMethodTypeId");
                }
                BigDecimal maxAmount = opp.getBigDecimal("maxAmount");
                String overflow = opp.getString("overflowFlag");

                ShoppingCart.CartPaymentInfo cpi = null;

                if ((overflow == null || !"Y".equals(overflow)) && oppi.hasNext()) {
                    cpi = cart.addPaymentAmount(paymentId, maxAmount);
                    Debug.logInfo("Added Payment: " + paymentId + " / " + maxAmount, module);
                } else {
                    cpi = cart.addPayment(paymentId);
                    Debug.logInfo("Added Payment: " + paymentId + " / [no max]", module);
                }
                // for finance account the finAccountId needs to be set
                if ("FIN_ACCOUNT".equals(paymentId)) {
                    cpi.finAccountId = opp.getString("finAccountId");
                }
                // set the billing account and amount
                cart.setBillingAccount(orderHeader.getString("billingAccountId"), orh.getBillingAccountMaxAmount());
            }
        } else {
            Debug.logInfo("No payment preferences found for order #" + orderId, module);
        }
        // set the order term
        if (UtilValidate.isNotEmpty(orderTerms)) {
            for (GenericValue orderTerm : orderTerms) {
                BigDecimal termValue = BigDecimal.ZERO;
                if (UtilValidate.isNotEmpty(orderTerm.getString("termValue"))){
                    termValue = new BigDecimal(orderTerm.getString("termValue"));
                }
                long termDays = 0;
                if (UtilValidate.isNotEmpty(orderTerm.getString("termDays"))) {
                    termDays = Long.parseLong(orderTerm.getString("termDays").trim());
                }
                String orderItemSeqId = orderTerm.getString("orderItemSeqId");
                cart.addOrderTerm(orderTerm.getString("termTypeId"), orderItemSeqId, termValue, termDays, orderTerm.getString("textValue"), orderTerm.getString("description"));
            }
        }

        List<GenericValue> orderItemShipGroupList = orh.getOrderItemShipGroups();
        for (GenericValue orderItemShipGroup: orderItemShipGroupList) {
            // should be sorted by shipGroupSeqId
            int newShipInfoIndex = cart.addShipInfo();
            CartShipInfo cartShipInfo = cart.getShipInfo(newShipInfoIndex);
            cartShipInfo.shipAfterDate = orderItemShipGroup.getTimestamp("shipAfterDate");
            cartShipInfo.shipBeforeDate = orderItemShipGroup.getTimestamp("shipByDate");
            cartShipInfo.shipmentMethodTypeId = orderItemShipGroup.getString("shipmentMethodTypeId");
            cartShipInfo.carrierPartyId = orderItemShipGroup.getString("carrierPartyId");
            cartShipInfo.supplierPartyId = orderItemShipGroup.getString("supplierPartyId");
            cartShipInfo.setMaySplit(orderItemShipGroup.getBoolean("maySplit"));
            cartShipInfo.giftMessage = orderItemShipGroup.getString("giftMessage");
            cartShipInfo.setContactMechId(orderItemShipGroup.getString("contactMechId"));
            cartShipInfo.shippingInstructions = orderItemShipGroup.getString("shippingInstructions");
            cartShipInfo.setFacilityId(orderItemShipGroup.getString("facilityId"));
            cartShipInfo.setVendorPartyId(orderItemShipGroup.getString("vendorPartyId"));
            cartShipInfo.setShipGroupSeqId(orderItemShipGroup.getString("shipGroupSeqId"));
            cartShipInfo.shipTaxAdj.addAll(orh.getOrderHeaderAdjustmentsTax(orderItemShipGroup.getString("shipGroupSeqId")));
        }

        List<GenericValue> orderItems = orh.getOrderItems();
        long nextItemSeq = 0;
        if (UtilValidate.isNotEmpty(orderItems)) {
            for (GenericValue item : orderItems) {
                // get the next item sequence id
                String orderItemSeqId = item.getString("orderItemSeqId");
                orderItemSeqId = orderItemSeqId.replaceAll("\\P{Digit}", "");
                // get product Id
                String productId = item.getString("productId");
                GenericValue product = null;
                // creates survey responses for Gift cards same as last Order created
                Map<String, Object> surveyResponseResult = null;
                try {
                    long seq = Long.parseLong(orderItemSeqId);
                    if (seq > nextItemSeq) {
                        nextItemSeq = seq;
                    }
                } catch (NumberFormatException e) {
                    Debug.logError(e, module);
                    return ServiceUtil.returnError(e.getMessage());
                }
                if ("ITEM_REJECTED".equals(item.getString("statusId")) || "ITEM_CANCELLED".equals(item.getString("statusId"))) continue;
                try {
                    product = EntityQuery.use(delegator).from("Product").where("productId", productId).queryOne();
                    if ("DIGITAL_GOOD".equals(product.getString("productTypeId"))) {
                        Map<String, Object> surveyResponseMap = new HashMap<String, Object>();
                        Map<String, Object> answers = new HashMap<String, Object>();
                        List<GenericValue> surveyResponseAndAnswers = EntityQuery.use(delegator).from("SurveyResponseAndAnswer").where("orderId", orderId, "orderItemSeqId", orderItemSeqId).queryList();
                        if (UtilValidate.isNotEmpty(surveyResponseAndAnswers)) {
                            String surveyId = EntityUtil.getFirst(surveyResponseAndAnswers).getString("surveyId");
                            for (GenericValue surveyResponseAndAnswer : surveyResponseAndAnswers) {
                                answers.put((surveyResponseAndAnswer.get("surveyQuestionId").toString()), surveyResponseAndAnswer.get("textResponse"));
                            }
                            surveyResponseMap.put("answers", answers);
                            surveyResponseMap.put("surveyId", surveyId);
                            surveyResponseResult = dispatcher.runSync("createSurveyResponse", surveyResponseMap);
                        }
                    }
                } catch (GenericEntityException e) {
                    Debug.logError(e, module);
                    return ServiceUtil.returnError(e.getMessage());
                } catch (GenericServiceException e) {
                    Debug.logError(e.toString(), module);
                    return ServiceUtil.returnError(e.toString());
                }

                // do not include PROMO items
                if (!includePromoItems && item.get("isPromo") != null && "Y".equals(item.getString("isPromo"))) {
                    continue;
                }

                // not a promo item; go ahead and add it in
                BigDecimal amount = item.getBigDecimal("selectedAmount");
                if (amount == null) {
                    amount = BigDecimal.ZERO;
                }
                //BigDecimal quantity = item.getBigDecimal("quantity");
                BigDecimal quantity = BigDecimal.ZERO;
                if("ITEM_COMPLETED".equals(item.getString("statusId")) && "N".equals(createAsNewOrder)) {
                    quantity = item.getBigDecimal("quantity");
                } else {
                    quantity = OrderReadHelper.getOrderItemQuantity(item);
                }
                if (quantity == null) {
                    quantity = BigDecimal.ZERO;
                }

                BigDecimal unitPrice = null;
                if ("Y".equals(item.getString("isModifiedPrice"))) {
                    unitPrice = item.getBigDecimal("unitPrice");
                }

                int itemIndex = -1;
                if (item.get("productId") == null) {
                    // non-product item
                    String itemType = item.getString("orderItemTypeId");
                    String desc = item.getString("itemDescription");
                    try {
                        // TODO: passing in null now for itemGroupNumber, but should reproduce from OrderItemGroup records
                        itemIndex = cart.addNonProductItem(itemType, desc, null, unitPrice, quantity, null, null, null, dispatcher);
                    } catch (CartItemModifyException e) {
                        Debug.logError(e, module);
                        return ServiceUtil.returnError(e.getMessage());
                    }
                } else {
                    // product item
                    String prodCatalogId = item.getString("prodCatalogId");

                    //prepare the rental data
                    Timestamp reservStart = null;
                    BigDecimal reservLength = null;
                    BigDecimal reservPersons = null;
                    String accommodationMapId = null;
                    String accommodationSpotId = null;

                    GenericValue workEffort = null;
                    String workEffortId = orh.getCurrentOrderItemWorkEffort(item);
                    if (workEffortId != null) {
                        try {
                            workEffort = EntityQuery.use(delegator).from("WorkEffort").where("workEffortId", workEffortId).queryOne();
                        } catch (GenericEntityException e) {
                            Debug.logError(e, module);
                        }
                    }
                    if (workEffort != null && "ASSET_USAGE".equals(workEffort.getString("workEffortTypeId"))) {
                        reservStart = workEffort.getTimestamp("estimatedStartDate");
                        reservLength = OrderReadHelper.getWorkEffortRentalLength(workEffort);
                        reservPersons = workEffort.getBigDecimal("reservPersons");
                        accommodationMapId = workEffort.getString("accommodationMapId");
                        accommodationSpotId = workEffort.getString("accommodationSpotId");

                    }    //end of rental data

                    //check for AGGREGATED products
                    ProductConfigWrapper configWrapper = null;
                    String configId = null;
                    try {
                        product = EntityQuery.use(delegator).from("Product").where("productId", productId).queryOne();
                        if (EntityTypeUtil.hasParentType(delegator, "ProductType", "productTypeId", product.getString("productTypeId"), "parentTypeId", "AGGREGATED")) {
                            GenericValue productAssoc = EntityQuery.use(delegator).from("ProductAssoc")
                                                                  .where("productAssocTypeId", "PRODUCT_CONF", "productIdTo", product.getString("productId"))
                                                                  .filterByDate()
                                                                  .queryFirst();
                            if (productAssoc != null) {
                                productId = productAssoc.getString("productId");
                                configId = product.getString("configId");
                            }
                        }
                    } catch (GenericEntityException e) {
                        Debug.logError(e, module);
                    }

                    if (UtilValidate.isNotEmpty(configId)) {
                        configWrapper = ProductConfigWorker.loadProductConfigWrapper(delegator, dispatcher, configId, productId, productStoreId, prodCatalogId, website, currency, locale, userLogin);
                    }
                    try {
                        itemIndex = cart.addItemToEnd(productId, amount, quantity, unitPrice, reservStart, reservLength, reservPersons,accommodationMapId,accommodationSpotId, null, null, prodCatalogId, configWrapper, item.getString("orderItemTypeId"), dispatcher, null, unitPrice == null ? null : false, skipInventoryChecks, skipProductChecks);
                    } catch (ItemNotFoundException e) {
                        Debug.logError(e, module);
                        return ServiceUtil.returnError(e.getMessage());
                    } catch (CartItemModifyException e) {
                        Debug.logError(e, module);
                        return ServiceUtil.returnError(e.getMessage());
                    }
                }

                // flag the item w/ the orderItemSeqId so we can reference it
                ShoppingCartItem cartItem = cart.findCartItem(itemIndex);
                cartItem.setIsPromo(item.get("isPromo") != null && "Y".equals(item.getString("isPromo")));
                cartItem.setOrderItemSeqId(item.getString("orderItemSeqId"));

                try {
                    cartItem.setItemGroup(cart.addItemGroup(item.getRelatedOne("OrderItemGroup", true)));
                } catch (GenericEntityException e) {
                    Debug.logError(e, module);
                    return ServiceUtil.returnError(e.getMessage());
                }
                // attach surveyResponseId for each item
                if (UtilValidate.isNotEmpty(surveyResponseResult)){
                    cartItem.setAttribute("surveyResponseId",surveyResponseResult.get("surveyResponseId"));
                }
                // attach addition item information
                cartItem.setStatusId(item.getString("statusId"));
                cartItem.setItemType(item.getString("orderItemTypeId"));
                cartItem.setItemComment(item.getString("comments"));
                cartItem.setQuoteId(item.getString("quoteId"));
                cartItem.setQuoteItemSeqId(item.getString("quoteItemSeqId"));
                cartItem.setProductCategoryId(item.getString("productCategoryId"));
                cartItem.setDesiredDeliveryDate(item.getTimestamp("estimatedDeliveryDate"));
                cartItem.setShipBeforeDate(item.getTimestamp("shipBeforeDate"));
                cartItem.setShipAfterDate(item.getTimestamp("shipAfterDate"));
                cartItem.setShoppingList(item.getString("shoppingListId"), item.getString("shoppingListItemSeqId"));
                cartItem.setIsModifiedPrice("Y".equals(item.getString("isModifiedPrice")));
                cartItem.setName(item.getString("itemDescription"));
                cartItem.setExternalId(item.getString("externalId"));
                cartItem.setListPrice(item.getBigDecimal("unitListPrice"));

                // load order item attributes
                List<GenericValue> orderItemAttributesList = null;
                try {
                    orderItemAttributesList = EntityQuery.use(delegator).from("OrderItemAttribute").where("orderId", orderId, "orderItemSeqId", orderItemSeqId).queryList();
                    if (UtilValidate.isNotEmpty(orderItemAttributesList)) {
                        for (GenericValue orderItemAttr : orderItemAttributesList) {
                            String name = orderItemAttr.getString("attrName");
                            String value = orderItemAttr.getString("attrValue");
                            cartItem.setOrderItemAttribute(name, value);
                        }
                    }
                } catch (GenericEntityException e) {
                    Debug.logError(e, module);
                    return ServiceUtil.returnError(e.getMessage());
                }

                // load order item contact mechs
                List<GenericValue> orderItemContactMechList = null;
                try {
                    orderItemContactMechList = EntityQuery.use(delegator).from("OrderItemContactMech").where("orderId", orderId, "orderItemSeqId", orderItemSeqId).queryList();
                    if (UtilValidate.isNotEmpty(orderItemContactMechList)) {
                        for (GenericValue orderItemContactMech : orderItemContactMechList) {
                            String contactMechPurposeTypeId = orderItemContactMech.getString("contactMechPurposeTypeId");
                            String contactMechId = orderItemContactMech.getString("contactMechId");
                            cartItem.addContactMech(contactMechPurposeTypeId, contactMechId);
                        }
                    }
                } catch (GenericEntityException e) {
                    Debug.logError(e, module);
                    return ServiceUtil.returnError(e.getMessage());
                }

                // set the PO number on the cart
                cart.setPoNumber(item.getString("correspondingPoId"));

                // get all item adjustments EXCEPT tax adjustments
                List<GenericValue> itemAdjustments = orh.getOrderItemAdjustments(item);
                if (itemAdjustments != null) {
                    for (GenericValue itemAdjustment : itemAdjustments) {
                        if (!isTaxAdjustment(itemAdjustment)) cartItem.addAdjustment(itemAdjustment);
                    }
                }
            }

            // setup the OrderItemShipGroupAssoc records
            if (UtilValidate.isNotEmpty(orderItems)) {
                int itemIndex = 0;
                for (GenericValue item : orderItems) {
                    // if rejected or cancelled ignore, just like above otherwise all indexes will be off by one!
                    if ("ITEM_REJECTED".equals(item.getString("statusId")) || "ITEM_CANCELLED".equals(item.getString("statusId"))) continue;

                    List<GenericValue> orderItemAdjustments = orh.getOrderItemAdjustments(item);
                    // set the item's ship group info
                    List<GenericValue> shipGroupAssocs = orh.getOrderItemShipGroupAssocs(item);
                    if (UtilValidate.isNotEmpty(shipGroupAssocs)) {
                        shipGroupAssocs = EntityUtil.orderBy(shipGroupAssocs, UtilMisc.toList("-shipGroupSeqId"));
                    }
                    for (int g = 0; g < shipGroupAssocs.size(); g++) {
                        GenericValue sgAssoc = shipGroupAssocs.get(g);
                        BigDecimal shipGroupQty = OrderReadHelper.getOrderItemShipGroupQuantity(sgAssoc);
                        if (shipGroupQty == null) {
                            shipGroupQty = BigDecimal.ZERO;
                        }

                        String cartShipGroupIndexStr = sgAssoc.getString("shipGroupSeqId");
                        int cartShipGroupIndex = cart.getShipInfoIndex(cartShipGroupIndexStr);
                        if (cartShipGroupIndex > 0) {
                            cart.positionItemToGroup(itemIndex, shipGroupQty, 0, cartShipGroupIndex, false);
                        }

                        // because the ship groups are setup before loading items, and the ShoppingCart.addItemToEnd
                        // method is called when loading items above and it calls ShoppingCart.setItemShipGroupQty,
                        // this may not be necessary here, so check it first as calling it here with 0 quantity and
                        // such ends up removing cart items from the group, which causes problems later with inventory
                        // reservation, tax calculation, etc.
                        ShoppingCart.CartShipInfo csi = cart.getShipInfo(cartShipGroupIndex);
                        ShoppingCartItem cartItem = cart.findCartItem(itemIndex);
                        if (cartItem == null || cartItem.getQuantity() == null ||
                                BigDecimal.ZERO.equals(cartItem.getQuantity()) ||
                                shipGroupQty.equals(cartItem.getQuantity())) {
                            Debug.logInfo("In loadCartFromOrder not adding item [" + item.getString("orderItemSeqId") +
                                    "] to ship group with index [" + itemIndex + "]; group quantity is [" + shipGroupQty +
                                    "] item quantity is [" + (cartItem != null ? cartItem.getQuantity() : "no cart item") +
                                    "] cartShipGroupIndex is [" + cartShipGroupIndex + "], csi.shipItemInfo.size(): " +
                                    (cartShipGroupIndex < 0 ? 0 : csi.shipItemInfo.size()), module);
                        } else {
                            cart.setItemShipGroupQty(itemIndex, shipGroupQty, cartShipGroupIndex);
                        }

                        List<GenericValue> shipGroupItemAdjustments = EntityUtil.filterByAnd(orderItemAdjustments, UtilMisc.toMap("shipGroupSeqId", cartShipGroupIndexStr));
                        if (cartItem == null || cartShipGroupIndex < 0) {
                            Debug.logWarning("In loadCartFromOrder could not find cart item for itemIndex=" + itemIndex + ", for orderId=" + orderId, module);
                        } else {
                            CartShipItemInfo cartShipItemInfo = csi.getShipItemInfo(cartItem);
                            if (cartShipItemInfo == null) {
                                Debug.logWarning("In loadCartFromOrder could not find CartShipItemInfo for itemIndex=" + itemIndex + ", for orderId=" + orderId, module);
                            } else {
                                List<GenericValue> itemTaxAdj = cartShipItemInfo.itemTaxAdj;
                                for (GenericValue shipGroupItemAdjustment : shipGroupItemAdjustments) {
                                    if (isTaxAdjustment(shipGroupItemAdjustment)) itemTaxAdj.add(shipGroupItemAdjustment);
                                }
                            }
                        }
                    }
                    itemIndex ++;
                }
            }

            // set the item seq in the cart
            if (nextItemSeq > 0) {
                try {
                    cart.setNextItemSeq(nextItemSeq+1);
                } catch (GeneralException e) {
                    Debug.logError(e, module);
                    return ServiceUtil.returnError(e.getMessage());
                }
            }
        }

        if (includePromoItems) {
            for (String productPromoCode: orh.getProductPromoCodesEntered()) {
                cart.addProductPromoCode(productPromoCode, dispatcher);
            }
            for (GenericValue productPromoUse: orh.getProductPromoUse()) {
                cart.addProductPromoUse(productPromoUse.getString("productPromoId"), productPromoUse.getString("productPromoCodeId"), productPromoUse.getBigDecimal("totalDiscountAmount"), productPromoUse.getBigDecimal("quantityLeftInActions"), new HashMap<ShoppingCartItem, BigDecimal>());
            }
        }

        List<GenericValue> adjustments = orh.getOrderHeaderAdjustments();
        // If applyQuoteAdjustments is set to false then standard cart adjustments are used.
        if (!adjustments.isEmpty()) {
            // The cart adjustments are added to the cart
            cart.getAdjustments().addAll(adjustments);
        }

        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put("shoppingCart", cart);
        return result;
    }

    public static Map<String, Object> loadCartFromQuote(DispatchContext dctx, Map<String, Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();

        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String quoteId = (String) context.get("quoteId");
        String applyQuoteAdjustmentsString = (String) context.get("applyQuoteAdjustments");
        Locale locale = (Locale) context.get("locale");

        boolean applyQuoteAdjustments = applyQuoteAdjustmentsString == null || "true".equals(applyQuoteAdjustmentsString);

        // get the quote header
        GenericValue quote = null;
        try {
            quote = EntityQuery.use(delegator).from("Quote").where("quoteId", quoteId).queryOne();
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        // initial require cart info
        String productStoreId = quote.getString("productStoreId");
        String currency = quote.getString("currencyUomId");

        // create the cart
        ShoppingCart cart = new ShoppingCart(delegator, productStoreId, locale, currency);
        // set shopping cart type
        if ("PURCHASE_QUOTE".equals(quote.getString("quoteTypeId"))) {
            cart.setOrderType("PURCHASE_ORDER");
            cart.setBillFromVendorPartyId(quote.getString("partyId"));
        }
        try {
            cart.setUserLogin(userLogin, dispatcher);
        } catch (CartItemModifyException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        cart.setQuoteId(quoteId);
        cart.setOrderName(quote.getString("quoteName"));
        cart.setChannelType(quote.getString("salesChannelEnumId"));

        List<GenericValue>quoteItems = null;
        List<GenericValue>quoteAdjs = null;
        List<GenericValue>quoteRoles = null;
        List<GenericValue>quoteAttributes = null;
        List<GenericValue>quoteTerms = null;
        try {
            quoteItems = quote.getRelated("QuoteItem", null, UtilMisc.toList("quoteItemSeqId"), false);
            quoteAdjs = quote.getRelated("QuoteAdjustment", null, null, false);
            quoteRoles = quote.getRelated("QuoteRole", null, null, false);
            quoteAttributes = quote.getRelated("QuoteAttribute", null, null, false);
            quoteTerms = quote.getRelated("QuoteTerm", null, null, false);
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }
        // set the role information
        cart.setOrderPartyId(quote.getString("partyId"));
        if (UtilValidate.isNotEmpty(quoteRoles)) {
            for (GenericValue quoteRole : quoteRoles) {
                String quoteRoleTypeId = quoteRole.getString("roleTypeId");
                String quoteRolePartyId = quoteRole.getString("partyId");
                if ("PLACING_CUSTOMER".equals(quoteRoleTypeId)) {
                    cart.setPlacingCustomerPartyId(quoteRolePartyId);
                } else if ("BILL_TO_CUSTOMER".equals(quoteRoleTypeId)) {
                    cart.setBillToCustomerPartyId(quoteRolePartyId);
                } else if ("SHIP_TO_CUSTOMER".equals(quoteRoleTypeId)) {
                    cart.setShipToCustomerPartyId(quoteRolePartyId);
                } else if ("END_USER_CUSTOMER".equals(quoteRoleTypeId)) {
                    cart.setEndUserCustomerPartyId(quoteRolePartyId);
                } else if ("BILL_FROM_VENDOR".equals(quoteRoleTypeId)) {
                    cart.setBillFromVendorPartyId(quoteRolePartyId);
                } else {
                    cart.addAdditionalPartyRole(quoteRolePartyId, quoteRoleTypeId);
                }
            }
        }

        // set the order term
        if (UtilValidate.isNotEmpty(quoteTerms)) {
            // create order term from quote term
            for (GenericValue quoteTerm : quoteTerms) {
                BigDecimal termValue = BigDecimal.ZERO;
                if (UtilValidate.isNotEmpty(quoteTerm.getString("termValue"))){
                    termValue = new BigDecimal(quoteTerm.getString("termValue"));
                }
                long termDays = 0;
                if (UtilValidate.isNotEmpty(quoteTerm.getString("termDays"))) {
                    termDays = Long.parseLong(quoteTerm.getString("termDays").trim());
                }
                String orderItemSeqId = quoteTerm.getString("quoteItemSeqId");
                cart.addOrderTerm(quoteTerm.getString("termTypeId"), orderItemSeqId,termValue, termDays, quoteTerm.getString("textValue"),quoteTerm.getString("description"));
            }
        }

        // set the attribute information
        if (UtilValidate.isNotEmpty(quoteAttributes)) {
            for (GenericValue quoteAttribute : quoteAttributes) {
                cart.setOrderAttribute(quoteAttribute.getString("attrName"), quoteAttribute.getString("attrValue"));
            }
        }

        // Convert the quote adjustment to order header adjustments and
        // put them in a map: the key/values pairs are quoteItemSeqId/List of adjs
        Map<String, List<GenericValue>> orderAdjsMap = new HashMap<String, List<GenericValue>>() ;
        for (GenericValue quoteAdj : quoteAdjs) {
            List<GenericValue> orderAdjs = orderAdjsMap.get(UtilValidate.isNotEmpty(quoteAdj.getString("quoteItemSeqId")) ? quoteAdj.getString("quoteItemSeqId") : quoteId);
            if (orderAdjs == null) {
                orderAdjs = new LinkedList<GenericValue>();
                orderAdjsMap.put(UtilValidate.isNotEmpty(quoteAdj.getString("quoteItemSeqId")) ? quoteAdj.getString("quoteItemSeqId") : quoteId, orderAdjs);
            }
            // convert quote adjustments to order adjustments
            GenericValue orderAdj = delegator.makeValue("OrderAdjustment");
            orderAdj.put("orderAdjustmentId", quoteAdj.get("quoteAdjustmentId"));
            orderAdj.put("orderAdjustmentTypeId", quoteAdj.get("quoteAdjustmentTypeId"));
            orderAdj.put("orderItemSeqId", quoteAdj.get("quoteItemSeqId"));
            orderAdj.put("comments", quoteAdj.get("comments"));
            orderAdj.put("description", quoteAdj.get("description"));
            orderAdj.put("amount", quoteAdj.get("amount"));
            orderAdj.put("productPromoId", quoteAdj.get("productPromoId"));
            orderAdj.put("productPromoRuleId", quoteAdj.get("productPromoRuleId"));
            orderAdj.put("productPromoActionSeqId", quoteAdj.get("productPromoActionSeqId"));
            orderAdj.put("productFeatureId", quoteAdj.get("productFeatureId"));
            orderAdj.put("correspondingProductId", quoteAdj.get("correspondingProductId"));
            orderAdj.put("sourceReferenceId", quoteAdj.get("sourceReferenceId"));
            orderAdj.put("sourcePercentage", quoteAdj.get("sourcePercentage"));
            orderAdj.put("customerReferenceId", quoteAdj.get("customerReferenceId"));
            orderAdj.put("primaryGeoId", quoteAdj.get("primaryGeoId"));
            orderAdj.put("secondaryGeoId", quoteAdj.get("secondaryGeoId"));
            orderAdj.put("exemptAmount", quoteAdj.get("exemptAmount"));
            orderAdj.put("taxAuthGeoId", quoteAdj.get("taxAuthGeoId"));
            orderAdj.put("taxAuthPartyId", quoteAdj.get("taxAuthPartyId"));
            orderAdj.put("overrideGlAccountId", quoteAdj.get("overrideGlAccountId"));
            orderAdj.put("includeInTax", quoteAdj.get("includeInTax"));
            orderAdj.put("includeInShipping", quoteAdj.get("includeInShipping"));
            orderAdj.put("createdDate", quoteAdj.get("createdDate"));
            orderAdj.put("createdByUserLogin", quoteAdj.get("createdByUserLogin"));
            orderAdjs.add(orderAdj);
        }

        long nextItemSeq = 0;
        if (UtilValidate.isNotEmpty(quoteItems)) {
            for (GenericValue quoteItem : quoteItems) {
                // get the next item sequence id
                String orderItemSeqId = quoteItem.getString("quoteItemSeqId");
                orderItemSeqId = orderItemSeqId.replaceAll("\\P{Digit}", "");
                try {
                    long seq = Long.parseLong(orderItemSeqId);
                    if (seq > nextItemSeq) {
                        nextItemSeq = seq;
                    }
                } catch (NumberFormatException e) {
                    Debug.logError(e, module);
                    return ServiceUtil.returnError(e.getMessage());
                }

                boolean isPromo = quoteItem.get("isPromo") != null && "Y".equals(quoteItem.getString("isPromo"));
                if (isPromo && !applyQuoteAdjustments) {
                    // do not include PROMO items
                    continue;
                }

                // not a promo item; go ahead and add it in
                BigDecimal amount = quoteItem.getBigDecimal("selectedAmount");
                if (amount == null) {
                    amount = BigDecimal.ZERO;
                }
                BigDecimal quantity = quoteItem.getBigDecimal("quantity");
                if (quantity == null) {
                    quantity = BigDecimal.ZERO;
                }
                BigDecimal quoteUnitPrice = quoteItem.getBigDecimal("quoteUnitPrice");
                if (quoteUnitPrice == null) {
                    quoteUnitPrice = BigDecimal.ZERO;
                }
                if (amount.compareTo(BigDecimal.ZERO) > 0) {
                    // If, in the quote, an amount is set, we need to
                    // pass to the cart the quoteUnitPrice/amount value.
                    quoteUnitPrice = quoteUnitPrice.divide(amount, generalRounding);
                }

                //rental product data
                Timestamp reservStart = quoteItem.getTimestamp("reservStart");
                BigDecimal reservLength = quoteItem.getBigDecimal("reservLength");
                BigDecimal reservPersons = quoteItem.getBigDecimal("reservPersons");
                int itemIndex = -1;
                if (quoteItem.get("productId") == null) {
                    // non-product item
                    String desc = quoteItem.getString("comments");
                    try {
                        // note that passing in null for itemGroupNumber as there is no real grouping concept in the quotes right now
                        itemIndex = cart.addNonProductItem(null, desc, null, null, quantity, null, null, null, dispatcher);
                    } catch (CartItemModifyException e) {
                        Debug.logError(e, module);
                        return ServiceUtil.returnError(e.getMessage());
                    }
                } else {
                    // product item
                    String productId = quoteItem.getString("productId");
                    ProductConfigWrapper configWrapper = null;
                    if (UtilValidate.isNotEmpty(quoteItem.getString("configId"))) {
                        configWrapper = ProductConfigWorker.loadProductConfigWrapper(delegator, dispatcher, quoteItem.getString("configId"), productId, productStoreId, null, null, currency, locale, userLogin);
                    }
                    try {
                            itemIndex = cart.addItemToEnd(productId, amount, quantity, quoteUnitPrice, reservStart, reservLength, reservPersons,null,null, null, null, null, configWrapper, null, dispatcher, new Boolean(!applyQuoteAdjustments), new Boolean(quoteUnitPrice.compareTo(BigDecimal.ZERO) == 0), Boolean.FALSE, Boolean.FALSE);

                    } catch (ItemNotFoundException e) {
                        Debug.logError(e, module);
                        return ServiceUtil.returnError(e.getMessage());
                    } catch (CartItemModifyException e) {
                        Debug.logError(e, module);
                        return ServiceUtil.returnError(e.getMessage());
                    }
                }

                // flag the item w/ the orderItemSeqId so we can reference it
                ShoppingCartItem cartItem = cart.findCartItem(itemIndex);
                cartItem.setOrderItemSeqId(orderItemSeqId);
                // attach additional item information
                cartItem.setItemComment(quoteItem.getString("comments"));
                cartItem.setQuoteId(quoteItem.getString("quoteId"));
                cartItem.setQuoteItemSeqId(quoteItem.getString("quoteItemSeqId"));
                cartItem.setIsPromo(isPromo);
            }

        }

        // If applyQuoteAdjustments is set to false then standard cart adjustments are used.
        if (applyQuoteAdjustments) {
            // The cart adjustments, derived from quote adjustments, are added to the cart

            // Tax adjustments should be added to the shipping group and shipping group item info
            // Other adjustments like promotional price should be added to the cart independent of
            // the ship group.
            // We're creating the cart right now using data from the quote, so there cannot yet be more than one ship group.

            List<GenericValue> cartAdjs = cart.getAdjustments();
            CartShipInfo shipInfo = cart.getShipInfo(0);

            List<GenericValue> adjs = orderAdjsMap.get(quoteId);

            if (adjs != null) {
                for (GenericValue adj : adjs) {
                    if (isTaxAdjustment( adj )) shipInfo.shipTaxAdj.add(adj);
                    else cartAdjs.add(adj);
                }
            }

            // The cart item adjustments, derived from quote item adjustments, are added to the cart
            if (quoteItems != null) {
                for (ShoppingCartItem item : cart) {
                    String orderItemSeqId = item.getOrderItemSeqId();
                    if (orderItemSeqId != null) {
                        adjs = orderAdjsMap.get(orderItemSeqId);
                    } else {
                        adjs = null;
                    }
                    if (adjs != null) {
                        for (GenericValue adj : adjs) {
                            if (isTaxAdjustment( adj )) {
                                CartShipItemInfo csii = shipInfo.getShipItemInfo(item);

                                if (csii.itemTaxAdj == null) shipInfo.setItemInfo(item, UtilMisc.toList(adj));
                                else csii.itemTaxAdj.add(adj);
                            }
                            else item.addAdjustment(adj);
                        }
                    }
                }
            }
        }

        // set the item seq in the cart
        if (nextItemSeq > 0) {
            try {
                cart.setNextItemSeq(nextItemSeq+1);
            } catch (GeneralException e) {
                Debug.logError(e, module);
                return ServiceUtil.returnError(e.getMessage());
            }
        }

        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put("shoppingCart", cart);
        return result;
    }

    private static boolean isTaxAdjustment(GenericValue cartAdj) {
        String adjType = cartAdj.getString("orderAdjustmentTypeId");

        return "SALES_TAX".equals(adjType) || "VAT_TAX".equals(adjType) || "VAT_PRICE_CORRECT".equals(adjType);
    }

    public static Map<String, Object>loadCartFromShoppingList(DispatchContext dctx, Map<String, Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();

        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String shoppingListId = (String) context.get("shoppingListId");
        String orderPartyId = (String) context.get("orderPartyId");
        Locale locale = (Locale) context.get("locale");

        // get the shopping list header
        GenericValue shoppingList = null;
        try {
            shoppingList = EntityQuery.use(delegator).from("ShoppingList").where("shoppingListId", shoppingListId).queryOne();
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        // initial required cart info
        String productStoreId = shoppingList.getString("productStoreId");
        String currency = shoppingList.getString("currencyUom");
        // If no currency has been set in the ShoppingList, use the ProductStore default currency
        if (currency == null) {
            try {
                GenericValue productStore = shoppingList.getRelatedOne("ProductStore", false);
                if (productStore != null) {
                    currency = productStore.getString("defaultCurrencyUomId");
                }
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
                return ServiceUtil.returnError(e.getMessage());
            }
        }
        // If we still have no currency, use the default from general.properties.  Failing that, use USD
        if (currency == null) {
                currency = EntityUtilProperties.getPropertyValue("general", "currency.uom.id.default", "USD", delegator);
        }

        // create the cart
        ShoppingCart cart = new ShoppingCart(delegator, productStoreId, locale, currency);

        try {
            cart.setUserLogin(userLogin, dispatcher);
        } catch (CartItemModifyException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        // set the role information
        if (UtilValidate.isNotEmpty(orderPartyId)) {
            cart.setOrderPartyId(orderPartyId);
        } else {
            cart.setOrderPartyId(shoppingList.getString("partyId"));
        }

        List<GenericValue>shoppingListItems = null;
        try {
            shoppingListItems = shoppingList.getRelated("ShoppingListItem", null, null, false);
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        long nextItemSeq = 0;
        if (UtilValidate.isNotEmpty(shoppingListItems)) {
            for (GenericValue shoppingListItem : shoppingListItems) {
                // get the next item sequence id
                String orderItemSeqId = shoppingListItem.getString("shoppingListItemSeqId");
                orderItemSeqId = orderItemSeqId.replaceAll("\\P{Digit}", "");
                try {
                    long seq = Long.parseLong(orderItemSeqId);
                    if (seq > nextItemSeq) {
                        nextItemSeq = seq;
                    }
                } catch (NumberFormatException e) {
                    Debug.logError(e, module);
                    return ServiceUtil.returnError(e.getMessage());
                }
                BigDecimal modifiedPrice = shoppingListItem.getBigDecimal("modifiedPrice");
                BigDecimal quantity = shoppingListItem.getBigDecimal("quantity");
                if (quantity == null) {
                    quantity = BigDecimal.ZERO;
                }
                int itemIndex = -1;
                if (shoppingListItem.get("productId") != null) {
                    // product item
                    String productId = shoppingListItem.getString("productId");
                    ProductConfigWrapper configWrapper = null;
                    if (UtilValidate.isNotEmpty(shoppingListItem.getString("configId"))) {
                        configWrapper = ProductConfigWorker.loadProductConfigWrapper(delegator, dispatcher, shoppingListItem.getString("configId"), productId, productStoreId, null, null, currency, locale, userLogin);
                    }
                    try {
                        itemIndex = cart.addItemToEnd(productId, null, quantity, null, null, null, null, null, configWrapper, dispatcher, Boolean.TRUE, Boolean.TRUE);
                    } catch (ItemNotFoundException e) {
                        Debug.logError(e, module);
                        return ServiceUtil.returnError(e.getMessage());
                    } catch (CartItemModifyException e) {
                        Debug.logError(e, module);
                        return ServiceUtil.returnError(e.getMessage());
                    }

                    // set the modified price
                    if (modifiedPrice != null && modifiedPrice.doubleValue() != 0) {
                        ShoppingCartItem item = cart.findCartItem(itemIndex);
                        if (item != null) {
                            item.setIsModifiedPrice(true);
                            item.setBasePrice(modifiedPrice);
                        }
                    }
                }

                // flag the item w/ the orderItemSeqId so we can reference it
                ShoppingCartItem cartItem = cart.findCartItem(itemIndex);
                cartItem.setOrderItemSeqId(orderItemSeqId);
                // attach additional item information
                cartItem.setShoppingList(shoppingListItem.getString("shoppingListId"), shoppingListItem.getString("shoppingListItemSeqId"));
            }

        }

        // set the item seq in the cart
        if (nextItemSeq > 0) {
            try {
                cart.setNextItemSeq(nextItemSeq+1);
            } catch (GeneralException e) {
                Debug.logError(e, module);
                return ServiceUtil.returnError(e.getMessage());
            }
        }

        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put("shoppingCart", cart);
        return result;
    }

    public static Map<String, Object>getShoppingCartData(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> result = ServiceUtil.returnSuccess();
        Locale locale = (Locale) context.get("locale");
        ShoppingCart shoppingCart = (ShoppingCart) context.get("shoppingCart");
        if (shoppingCart != null) {
            String isoCode = shoppingCart.getCurrency();
            result.put("totalQuantity", shoppingCart.getTotalQuantity());
            result.put("currencyIsoCode",isoCode);
            result.put("subTotal", shoppingCart.getSubTotal());
            result.put("subTotalCurrencyFormatted",org.apache.ofbiz.base.util.UtilFormatOut.formatCurrency(shoppingCart.getSubTotal(), isoCode, locale));
            result.put("totalShipping", shoppingCart.getTotalShipping());
            result.put("totalShippingCurrencyFormatted",org.apache.ofbiz.base.util.UtilFormatOut.formatCurrency(shoppingCart.getTotalShipping(), isoCode, locale));
            result.put("totalSalesTax",shoppingCart.getTotalSalesTax());
            result.put("totalSalesTaxCurrencyFormatted",org.apache.ofbiz.base.util.UtilFormatOut.formatCurrency(shoppingCart.getTotalSalesTax(), isoCode, locale));
            result.put("displayGrandTotal", shoppingCart.getDisplayGrandTotal());
            result.put("displayGrandTotalCurrencyFormatted",org.apache.ofbiz.base.util.UtilFormatOut.formatCurrency(shoppingCart.getDisplayGrandTotal(), isoCode, locale));
            BigDecimal orderAdjustmentsTotal = OrderReadHelper.calcOrderAdjustments(OrderReadHelper.getOrderHeaderAdjustments(shoppingCart.getAdjustments(), null), shoppingCart.getSubTotal(), true, true, true);
            result.put("displayOrderAdjustmentsTotalCurrencyFormatted", org.apache.ofbiz.base.util.UtilFormatOut.formatCurrency(orderAdjustmentsTotal, isoCode, locale));
            Map<String, Object> cartItemData = new HashMap<String, Object>();
            for (ShoppingCartItem cartLine : shoppingCart) {
                int cartLineIndex = shoppingCart.getItemIndex(cartLine);
                cartItemData.put("displayItemQty_" + cartLineIndex, cartLine.getQuantity());
                cartItemData.put("displayItemPrice_" + cartLineIndex, org.apache.ofbiz.base.util.UtilFormatOut.formatCurrency(cartLine.getDisplayPrice(), isoCode, locale));
                cartItemData.put("displayItemSubTotal_" + cartLineIndex, cartLine.getDisplayItemSubTotal());
                cartItemData.put("displayItemSubTotalCurrencyFormatted_" + cartLineIndex ,org.apache.ofbiz.base.util.UtilFormatOut.formatCurrency(cartLine.getDisplayItemSubTotal(), isoCode, locale));
                cartItemData.put("displayItemAdjustment_" + cartLineIndex ,org.apache.ofbiz.base.util.UtilFormatOut.formatCurrency(cartLine.getOtherAdjustments(), isoCode, locale));
            }
            result.put("cartItemData",cartItemData);
        }
        return result;
    }

    public static Map<String, Object>getShoppingCartItemIndex(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> result = ServiceUtil.returnSuccess();
        ShoppingCart shoppingCart = (ShoppingCart) context.get("shoppingCart");
        String productId = (String) context.get("productId");
        if (shoppingCart != null && UtilValidate.isNotEmpty(shoppingCart.items())) {
            List<ShoppingCartItem> items = shoppingCart.findAllCartItems(productId);
            if (items.size() > 0) {
                ShoppingCartItem item = items.get(0);
                int itemIndex = shoppingCart.getItemIndex(item);
                result.put("itemIndex", String.valueOf(itemIndex));
            }
        }
        return result;
    }

    public static Map<String, Object>resetShipGroupItems(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> result = ServiceUtil.returnSuccess();
        ShoppingCart cart = (ShoppingCart) context.get("shoppingCart");
        for (ShoppingCartItem item : cart) {
            cart.clearItemShipInfo(item);
            cart.setItemShipGroupQty(item, item.getQuantity(), 0);
        }
        return result;
    }

    public static Map<String, Object>prepareVendorShipGroups(DispatchContext dctx, Map<String, Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        ShoppingCart cart = (ShoppingCart) context.get("shoppingCart");
        Map<String, Object> result = ServiceUtil.returnSuccess();
        try {
            Map<String, Object> resp = dispatcher.runSync("resetShipGroupItems", context);
            if (ServiceUtil.isError(resp)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(resp));
            }
        } catch (GenericServiceException e) {
            Debug.logError(e.toString(), module);
            return ServiceUtil.returnError(e.toString());
        }
        Map<String, Object> vendorMap = new HashMap<String, Object>();
        for (ShoppingCartItem item : cart) {
            GenericValue vendorProduct = null;
            String productId = item.getParentProductId();
            if (productId == null) {
                productId = item.getProductId();
            }
            int index = 0;
            try {
                vendorProduct = EntityQuery.use(delegator).from("VendorProduct").where("productId", productId, "productStoreGroupId", "_NA_").queryFirst();
            } catch (GenericEntityException e) {
                Debug.logError(e.toString(), module);
            }

            if (UtilValidate.isEmpty(vendorProduct)) {
                if (vendorMap.containsKey("_NA_")) {
                    index = ((Integer) vendorMap.get("_NA_")).intValue();
                    cart.positionItemToGroup(item, item.getQuantity(), 0, index, true);
                } else {
                    index = cart.addShipInfo();
                    vendorMap.put("_NA_", index);

                    ShoppingCart.CartShipInfo info = cart.getShipInfo(index);
                    info.setVendorPartyId("_NA_");
                    info.setShipGroupSeqId(UtilFormatOut.formatPaddedNumber(index, 5));
                    cart.positionItemToGroup(item, item.getQuantity(), 0, index, true);
                }
            }
            if (vendorProduct != null) {
                String vendorPartyId = vendorProduct.getString("vendorPartyId");
                if (vendorMap.containsKey(vendorPartyId)) {
                    index = ((Integer) vendorMap.get(vendorPartyId)).intValue();
                    cart.positionItemToGroup(item, item.getQuantity(), 0, index, true);
                } else {
                    index = cart.addShipInfo();
                    vendorMap.put(vendorPartyId, index);

                    ShoppingCart.CartShipInfo info = cart.getShipInfo(index);
                    info.setVendorPartyId(vendorPartyId);
                    info.setShipGroupSeqId(UtilFormatOut.formatPaddedNumber(index, 5));
                    cart.positionItemToGroup(item, item.getQuantity(), 0, index, true);
                }
            }
        }
        return result;
    }
}
