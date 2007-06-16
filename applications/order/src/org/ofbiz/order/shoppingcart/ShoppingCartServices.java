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
package org.ofbiz.order.shoppingcart;

import java.math.BigDecimal;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedList;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityConditionList;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.order.order.OrderReadHelper;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;

/**
 * Shopping Cart Services
 */
public class ShoppingCartServices {

    public static final String module = ShoppingCartServices.class.getName();
    public static final String resource = "OrderUiLabels";
    public static final String resource_error = "OrderErrorUiLabels";

    public static Map assignItemShipGroup(DispatchContext dctx, Map context) {
        ShoppingCart cart = (ShoppingCart) context.get("shoppingCart");
        Integer fromGroupIndex = (Integer) context.get("fromGroupIndex");
        Integer toGroupIndex = (Integer) context.get("toGroupIndex");
        Integer itemIndex = (Integer) context.get("itemIndex");
        Double quantity = (Double) context.get("quantity");
        Boolean clearEmptyGroups = (Boolean) context.get("clearEmptyGroups");

        if (clearEmptyGroups == null) {
            clearEmptyGroups = Boolean.TRUE;
        }

        Debug.log("From Group - " + fromGroupIndex + " To Group - " + toGroupIndex + "Item - " + itemIndex + "(" + quantity + ")", module);
        if (fromGroupIndex.equals(toGroupIndex)) {
            // nothing to do
            return ServiceUtil.returnSuccess();
        }

        cart.positionItemToGroup(itemIndex.intValue(), quantity.doubleValue(),
                fromGroupIndex.intValue(), toGroupIndex.intValue(), clearEmptyGroups.booleanValue());
        Debug.log("Called cart.positionItemToGroup()", module);

        return ServiceUtil.returnSuccess();
    }

    public static Map setShippingOptions(DispatchContext dctx, Map context) {
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

    public static Map setPaymentOptions(DispatchContext dctx, Map context) {
        Locale locale = (Locale) context.get("locale");

        return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderServiceNotYetImplemented",locale));
    }

    public static Map setOtherOptions(DispatchContext dctx, Map context) {
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

    public static Map loadCartFromOrder(DispatchContext dctx, Map context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericDelegator delegator = dctx.getDelegator();

        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String orderId = (String) context.get("orderId");
        Boolean skipInventoryChecks = (Boolean) context.get("skipInventoryChecks");
        Boolean skipProductChecks = (Boolean) context.get("skipProductChecks");
        Locale locale = (Locale) context.get("locale");

        if (UtilValidate.isEmpty(skipInventoryChecks)) {
            skipInventoryChecks = Boolean.FALSE;
        }
        if (UtilValidate.isEmpty(skipProductChecks)) {
            skipProductChecks = Boolean.FALSE;
        }

        // get the order header
        GenericValue orderHeader = null;
        try {
            orderHeader = delegator.findByPrimaryKey("OrderHeader", UtilMisc.toMap("orderId", orderId));
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

        // create the cart
        ShoppingCart cart = new ShoppingCart(delegator, productStoreId, website, locale, currency);
        cart.setOrderType(orderTypeId);
        cart.setChannelType(orderHeader.getString("salesChannelEnumId"));
        cart.setInternalCode(orderHeader.getString("internalCode"));

        try {
            cart.setUserLogin(userLogin, dispatcher);
        } catch (CartItemModifyException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
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

        // load the payment infos
        List orderPaymentPrefs = null;
        try {
            List exprs = UtilMisc.toList(new EntityExpr("orderId", EntityOperator.EQUALS, orderId));
            exprs.add(new EntityExpr("statusId", EntityOperator.NOT_EQUAL, "PAYMENT_RECEIVED"));
            exprs.add(new EntityExpr("statusId", EntityOperator.NOT_EQUAL, "PAYMENT_CANCELLED"));
            exprs.add(new EntityExpr("statusId", EntityOperator.NOT_EQUAL, "PAYMENT_DECLINED"));
            exprs.add(new EntityExpr("statusId", EntityOperator.NOT_EQUAL, "PAYMENT_SETTLED"));
            EntityCondition cond = new EntityConditionList(exprs, EntityOperator.AND);
            orderPaymentPrefs = delegator.findByCondition("OrderPaymentPreference", cond, null, null);
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }
        if (orderPaymentPrefs != null && orderPaymentPrefs.size() > 0) {
            Iterator oppi = orderPaymentPrefs.iterator();
            while (oppi.hasNext()) {
                GenericValue opp = (GenericValue) oppi.next();
                String paymentId = opp.getString("paymentMethodId");
                if (paymentId == null) {
                    paymentId = opp.getString("paymentMethodTypeId");
                }
                Double maxAmount = opp.getDouble("maxAmount");
                String overflow = opp.getString("overflowFlag");

                ShoppingCart.CartPaymentInfo cpi = null;

                if ((overflow == null || !"Y".equals(overflow)) && oppi.hasNext()) {
                    cpi = cart.addPaymentAmount(paymentId, maxAmount);
                    Debug.log("Added Payment: " + paymentId + " / " + maxAmount, module);
                } else {
                    cpi = cart.addPayment(paymentId);
                    Debug.log("Added Payment: " + paymentId + " / [no max]", module);
                }
                // for finance account the finAccountId needs to be set
                if ("FIN_ACCOUNT".equals(paymentId)) {
                    cpi.finAccountId = opp.getString("finAccountId");
                }
            }
        } else {
            Debug.log("No payment preferences found for order #" + orderId, module);
        }

        List orderItems = orh.getValidOrderItems();
        long nextItemSeq = 0;
        if (orderItems != null) {
            Iterator i = orderItems.iterator();
            while (i.hasNext()) {
                GenericValue item = (GenericValue) i.next();

                // get the next item sequence id
                String orderItemSeqId = item.getString("orderItemSeqId");
                try {
                    long seq = Long.parseLong(orderItemSeqId);
                    if (seq > nextItemSeq) {
                        nextItemSeq = seq;
                    }
                } catch (NumberFormatException e) {
                    Debug.logError(e, module);
                    return ServiceUtil.returnError(e.getMessage());
                }

                // do not include PROMO items
                if (item.get("isPromo") != null && "Y".equals(item.getString("isPromo"))) {
                    continue;
                }

                // not a promo item; go ahead and add it in
                Double amount = item.getDouble("selectedAmount");
                if (amount == null) {
                    amount = new Double(0);
                }
                double quantityDbl = 0;
                BigDecimal quantity = item.getBigDecimal("quantity");
                if (quantity != null) {
                    quantityDbl = quantity.doubleValue();
                }
                int itemIndex = -1;
                if (item.get("productId") == null) {
                    // non-product item
                    String itemType = item.getString("orderItemTypeId");
                    String desc = item.getString("itemDescription");
                    try {
                        // TODO: passing in null now for itemGroupNumber, but should reproduce from OrderItemGroup records
                        itemIndex = cart.addNonProductItem(itemType, desc, null, null, quantity.doubleValue(), null, null, null, dispatcher);
                    } catch (CartItemModifyException e) {
                        Debug.logError(e, module);
                        return ServiceUtil.returnError(e.getMessage());
                    }
                } else {
                    // product item
                    String prodCatalogId = item.getString("prodCatalogId");
                    String productId = item.getString("productId");
                    try {
                        itemIndex = cart.addItemToEnd(productId, amount, quantityDbl, null, null, null, prodCatalogId, item.getString("orderItemTypeId"), dispatcher, null, null, skipInventoryChecks, skipProductChecks);
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
                cartItem.setOrderItemSeqId(item.getString("orderItemSeqId"));

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
                if(cartItem.getIsModifiedPrice())
                    cartItem.setBasePrice(item.getDouble("unitPrice").doubleValue());
                
                // set the PO number on the cart
                cart.setPoNumber(item.getString("correspondingPoId"));

                // set the item's ship group info
                List shipGroups = orh.getOrderItemShipGroupAssocs(item);
                for (int g = 0; g < shipGroups.size(); g++) {
                    GenericValue sgAssoc = (GenericValue) shipGroups.get(g);
                    Double shipGroupQty = OrderReadHelper.getOrderItemShipGroupQuantity(sgAssoc);
                    if (shipGroupQty == null) {
                        shipGroupQty = new Double(0);
                    }

                    GenericValue sg = null;
                    try {
                        sg = sgAssoc.getRelatedOne("OrderItemShipGroup");
                    } catch (GenericEntityException e) {
                        Debug.logError(e, module);
                        return ServiceUtil.returnError(e.getMessage());
                    }
                    cart.setShipAfterDate(g, sg.getTimestamp("shipAfterDate"));
                    cart.setShipBeforeDate(g, sg.getTimestamp("shipByDate"));
                    cart.setShipmentMethodTypeId(g, sg.getString("shipmentMethodTypeId"));
                    cart.setCarrierPartyId(g, sg.getString("carrierPartyId"));
                    cart.setSupplierPartyId(g, sg.getString("supplierPartyId"));
                    cart.setMaySplit(g, sg.getBoolean("maySplit"));
                    cart.setGiftMessage(g, sg.getString("giftMessage"));
                    cart.setShippingContactMechId(g, sg.getString("contactMechId"));
                    cart.setShippingInstructions(g, sg.getString("shippingInstructions"));
                    cart.setItemShipGroupQty(itemIndex, shipGroupQty.doubleValue(), g);
                }
            }

            // set the item seq in the cart
            if (nextItemSeq > 0) {
                try {
                    cart.setNextItemSeq(nextItemSeq);
                } catch (GeneralException e) {
                    Debug.logError(e, module);
                    return ServiceUtil.returnError(e.getMessage());
                }
            }
        }

        Map result = ServiceUtil.returnSuccess();
        result.put("shoppingCart", cart);
        return result;
    }
    
    public static Map loadCartFromQuote(DispatchContext dctx, Map context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericDelegator delegator = dctx.getDelegator();

        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String quoteId = (String) context.get("quoteId");
        String applyQuoteAdjustmentsString = (String) context.get("applyQuoteAdjustments");
        Locale locale = (Locale) context.get("locale");

        boolean applyQuoteAdjustments = applyQuoteAdjustmentsString == null || "true".equals(applyQuoteAdjustmentsString);
        
        // get the quote header
        GenericValue quote = null;
        try {
            quote = delegator.findByPrimaryKey("Quote", UtilMisc.toMap("quoteId", quoteId));
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
        if (quote.getString("quoteTypeId").equals("PURCHASE_QUOTE")){
            cart.setOrderType("PURCHASE_ORDER");
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
        
        List quoteItems = null;
        List quoteAdjs = null;
        List quoteRoles = null;
        List quoteAttributes = null;
        try {
            quoteItems = quote.getRelated("QuoteItem");
            quoteAdjs = quote.getRelated("QuoteAdjustment");
            quoteRoles = quote.getRelated("QuoteRole");
            quoteAttributes = quote.getRelated("QuoteAttribute");
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }
        // set the role information
        cart.setOrderPartyId(quote.getString("partyId"));
        if (quoteRoles != null) {
            Iterator quoteRolesIt = quoteRoles.iterator();
            while (quoteRolesIt.hasNext()) {
                GenericValue quoteRole = (GenericValue)quoteRolesIt.next();
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
                } else {
                    cart.addAdditionalPartyRole(quoteRolePartyId, quoteRoleTypeId);
                }
            }
        }

        // set the attribute information
        if (quoteAttributes != null) {
            Iterator quoteAttributesIt = quoteAttributes.iterator();
            while (quoteAttributesIt.hasNext()) {
                GenericValue quoteAttribute = (GenericValue)quoteAttributesIt.next();
                cart.setOrderAttribute(quoteAttribute.getString("attrName"), quoteAttribute.getString("attrValue"));
            }
        }

        // Convert the quote adjustment to order header adjustments and
        // put them in a map: the key/values pairs are quoteItemSeqId/List of adjs
        Map orderAdjsMap = new HashMap();
        Iterator quoteAdjsIter = quoteAdjs.iterator();
        while (quoteAdjsIter.hasNext()) {
            GenericValue quoteAdj = (GenericValue)quoteAdjsIter.next();
            List orderAdjs = (List)orderAdjsMap.get(quoteAdj.get("quoteItemSeqId"));
            if (orderAdjs == null) {
                orderAdjs = new LinkedList();
                orderAdjsMap.put(quoteAdj.get("quoteItemSeqId"), orderAdjs);
            }
            // convert quote adjustments to order adjustments
            GenericValue orderAdj = delegator.makeValue("OrderAdjustment", null);
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
        if (quoteItems != null) {
            Iterator i = quoteItems.iterator();
            while (i.hasNext()) {
                GenericValue item = (GenericValue) i.next();

                // get the next item sequence id
                String orderItemSeqId = item.getString("quoteItemSeqId");
                try {
                    long seq = Long.parseLong(orderItemSeqId);
                    if (seq > nextItemSeq) {
                        nextItemSeq = seq;
                    }
                } catch (NumberFormatException e) {
                    Debug.logError(e, module);
                    return ServiceUtil.returnError(e.getMessage());
                }

                boolean isPromo = item.get("isPromo") != null && "Y".equals(item.getString("isPromo"));
                if (isPromo && !applyQuoteAdjustments) {
                    // do not include PROMO items
                    continue;
                }

                // not a promo item; go ahead and add it in
                Double amount = item.getDouble("selectedAmount");
                if (amount == null) {
                    amount = new Double(0);
                }
                Double quantity = item.getDouble("quantity");
                if (quantity == null) {
                    quantity = new Double(0);
                }
                Double quoteUnitPrice = item.getDouble("quoteUnitPrice");
                if (quoteUnitPrice == null) {
                    quoteUnitPrice = new Double(0);
                }
                if (amount.doubleValue() > 0) {
                    // If, in the quote, an amount is set, we need to
                    // pass to the cart the quoteUnitPrice/amount value.
                    quoteUnitPrice = new Double(quoteUnitPrice.doubleValue() / amount.doubleValue());
                }
                int itemIndex = -1;
                if (item.get("productId") == null) {
                    // non-product item
                    String desc = item.getString("comments");
                    try {
                        // note that passing in null for itemGroupNumber as there is no real grouping concept in the quotes right now
                        itemIndex = cart.addNonProductItem(null, desc, null, null, quantity.doubleValue(), null, null, null, dispatcher);
                    } catch (CartItemModifyException e) {
                        Debug.logError(e, module);
                        return ServiceUtil.returnError(e.getMessage());
                    }
                } else {
                    // product item
                    String productId = item.getString("productId");
                    try {
                        itemIndex = cart.addItemToEnd(productId, amount, quantity.doubleValue(), quoteUnitPrice, null, null, null, null, dispatcher, new Boolean(!applyQuoteAdjustments), new Boolean(quoteUnitPrice.doubleValue() == 0));
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
                cartItem.setItemComment(item.getString("comments"));
                cartItem.setQuoteId(item.getString("quoteId"));
                cartItem.setQuoteItemSeqId(item.getString("quoteItemSeqId"));
                cartItem.setIsPromo(isPromo);
                //cartItem.setDesiredDeliveryDate(item.getTimestamp("estimatedDeliveryDate"));
                //cartItem.setStatusId(item.getString("statusId"));
                //cartItem.setItemType(item.getString("orderItemTypeId"));
                //cartItem.setProductCategoryId(item.getString("productCategoryId"));
                //cartItem.setShoppingList(item.getString("shoppingListId"), item.getString("shoppingListItemSeqId"));
            }

        }

        // If applyQuoteAdjustments is set to false then standard cart adjustments are used.
        if (applyQuoteAdjustments) {
            // The cart adjustments, derived from quote adjustments, are added to the cart
            List adjs = (List)orderAdjsMap.get(null);
            if (adjs != null) {
                cart.getAdjustments().addAll(adjs);
            }
            // The cart item adjustments, derived from quote item adjustments, are added to the cart
            if (quoteItems != null) {
                Iterator i = cart.iterator();
                while (i.hasNext()) {
                    ShoppingCartItem item = (ShoppingCartItem) i.next();
                    adjs = (List)orderAdjsMap.get(item.getOrderItemSeqId());
                    if (adjs != null) {
                        item.getAdjustments().addAll(adjs);
                    }
                }
            }
        }
        // set the item seq in the cart
        if (nextItemSeq > 0) {
            try {
                cart.setNextItemSeq(nextItemSeq);
            } catch (GeneralException e) {
                Debug.logError(e, module);
                return ServiceUtil.returnError(e.getMessage());
            }
        }

        Map result = ServiceUtil.returnSuccess();
        result.put("shoppingCart", cart);
        return result;
    }

    public static Map loadCartFromShoppingList(DispatchContext dctx, Map context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericDelegator delegator = dctx.getDelegator();

        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String shoppingListId = (String) context.get("shoppingListId");
        Locale locale = (Locale) context.get("locale");

        // get the shopping list header
        GenericValue shoppingList = null;
        try {
            shoppingList = delegator.findByPrimaryKey("ShoppingList", UtilMisc.toMap("shoppingListId", shoppingListId));
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
                GenericValue productStore = shoppingList.getRelatedOne("ProductStore");
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
                currency = UtilProperties.getPropertyValue("general", "currency.uom.id.default", "USD");
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
        cart.setOrderPartyId(shoppingList.getString("partyId"));

        List shoppingListItems = null;
        try {
            shoppingListItems = shoppingList.getRelated("ShoppingListItem");
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        long nextItemSeq = 0;
        if (shoppingListItems != null) {
            Iterator i = shoppingListItems.iterator();
            while (i.hasNext()) {
                GenericValue item = (GenericValue) i.next();

                // get the next item sequence id
                String orderItemSeqId = item.getString("shoppingListItemSeqId");
                try {
                    long seq = Long.parseLong(orderItemSeqId);
                    if (seq > nextItemSeq) {
                        nextItemSeq = seq;
                    }
                } catch (NumberFormatException e) {
                    Debug.logError(e, module);
                    return ServiceUtil.returnError(e.getMessage());
                }
                /*
                Double amount = item.getDouble("selectedAmount");
                if (amount == null) {
                    amount = new Double(0);
                }
                 */
                Double quantity = item.getDouble("quantity");
                if (quantity == null) {
                    quantity = new Double(0);
                }
                int itemIndex = -1;
                if (item.get("productId") != null) {
                    // product item
                    String productId = item.getString("productId");
                    try {
                        itemIndex = cart.addItemToEnd(productId, null, quantity.doubleValue(), null, null, null, null, null, dispatcher, Boolean.TRUE, Boolean.TRUE);
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
                cartItem.setShoppingList(item.getString("shoppingListId"), item.getString("shoppingListItemSeqId"));
            }

        }

        // set the item seq in the cart
        if (nextItemSeq > 0) {
            try {
                cart.setNextItemSeq(nextItemSeq);
            } catch (GeneralException e) {
                Debug.logError(e, module);
                return ServiceUtil.returnError(e.getMessage());
            }
        }

        Map result = ServiceUtil.returnSuccess();
        result.put("shoppingCart", cart);
        return result;
    }
}
