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
package org.apache.ofbiz.order.test;

import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.order.order.OrderChangeHelper;
import org.apache.ofbiz.order.shoppingcart.CartItemModifyException;
import org.apache.ofbiz.order.shoppingcart.CheckOutHelper;
import org.apache.ofbiz.order.shoppingcart.ItemNotFoundException;
import org.apache.ofbiz.order.shoppingcart.ShoppingCart;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.service.ServiceUtil;

/**
 * Order Processing Services
 */

public class OrderTestServices {

    public static final String module = OrderTestServices.class.getName();

    public static Map<String, Object> createTestSalesOrders(DispatchContext dctx, Map<String, ? extends Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Integer numberOfOrders = (Integer) context.get("numberOfOrders");

        int numberOfOrdersInt = numberOfOrders.intValue();
        for (int i = 1; i <= numberOfOrdersInt; i++) {
            try {
                ModelService modelService = dctx.getModelService("createTestSalesOrderSingle");
                Map<String, Object> outputMap = dispatcher.runSync("createTestSalesOrderSingle", modelService.makeValid(context, ModelService.IN_PARAM));
                String orderId = (String)outputMap.get("orderId");
                Debug.logInfo("Test sales order with id [" + orderId + "] has been processed.", module);
            } catch (GenericServiceException e) {
                String errMsg = "Error calling createTestSalesOrderSingle: " + e.toString();
                Debug.logError(e, errMsg, module);
            }
        }
        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> createTestSalesOrderSingle(DispatchContext dctx, Map<String, ? extends Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get("locale");
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String productCategoryId = (String) context.get("productCategoryId");
        String productStoreId = (String) context.get("productStoreId");
        String currencyUomId = (String) context.get("currencyUomId");
        String partyId = (String) context.get("partyId");
        String productId = (String) context.get("productId");
        Integer numberOfProductsPerOrder = (Integer) context.get("numberOfProductsPerOrder");
        String salesChannel = (String) context.get("salesChannel");
        if (UtilValidate.isEmpty(salesChannel)) {
            salesChannel = "WEB_SALES_CHANNEL";
        }

        List<String> productsList = new LinkedList<String>();
        try {
            if (UtilValidate.isNotEmpty(productId)) {
                productsList.add(productId);
                numberOfProductsPerOrder = 1;
            } else {
                Map<String, Object> result = dispatcher.runSync("getProductCategoryMembers", UtilMisc.toMap("categoryId", productCategoryId));
                if (result.get("categoryMembers") != null) {
                    List<GenericValue> productCategoryMembers = UtilGenerics.checkList(result.get("categoryMembers"));
                    if (productCategoryMembers != null) {
                        for (GenericValue prodCatMemb : productCategoryMembers) {
                            if (prodCatMemb != null) {
                                productsList.add(prodCatMemb.getString("productId"));
                            }
                        }
                    }
                }
            }
        } catch (GenericServiceException gse) {
            return ServiceUtil.returnError(gse.getMessage());
        } catch (Exception e) {
            return ServiceUtil.returnError(e.getMessage());
        }
        if (productsList.size() == 0) {
            return ServiceUtil.returnError(UtilProperties.getMessage("OrderUiLabels",
                    "OrderCreateTestSalesOrderSingleError", 
                    UtilMisc.toMap("productCategoryId", productCategoryId), locale));
        }

        Random r = new Random();

        ShoppingCart cart = new ShoppingCart(delegator, productStoreId, locale, currencyUomId);
        cart.setOrderType("SALES_ORDER");
        cart.setChannelType(salesChannel);
        cart.setProductStoreId(productStoreId);

        cart.setBillToCustomerPartyId(partyId);
        cart.setPlacingCustomerPartyId(partyId);
        cart.setShipToCustomerPartyId(partyId);
        cart.setEndUserCustomerPartyId(partyId);
        try {
            cart.setUserLogin(userLogin, dispatcher);
        } catch (Exception exc) {
            Debug.logWarning("Error setting userLogin in the cart: " + exc.getMessage(), module);
        }
        int numberOfProductsPerOrderInt = numberOfProductsPerOrder.intValue();
        for (int j = 1; j <= numberOfProductsPerOrderInt; j++) {
            // get a product
            int k = r.nextInt(productsList.size());
            try {
                cart.addOrIncreaseItem(productsList.get(k), null, BigDecimal.ONE, null, null, null,
                                       null, null, null, null,
                                       null /*catalogId*/, null, null/*itemType*/, null/*itemGroupNumber*/, null, dispatcher);
            } catch (CartItemModifyException | ItemNotFoundException exc) {
                Debug.logWarning("Error adding product with id " + productsList.get(k) + " to the cart: " + exc.getMessage(), module);
            }
        }
        cart.setDefaultCheckoutOptions(dispatcher);
        CheckOutHelper checkout = new CheckOutHelper(dispatcher, delegator, cart);
        Map<String, Object> orderCreateResult = checkout.createOrder(userLogin);
        String orderId = (String) orderCreateResult.get("orderId");

        Map<String, Object> resultMap = ServiceUtil.returnSuccess();
        // approve the order
        if (UtilValidate.isNotEmpty(orderId)) {
            Debug.logInfo("Created test order with id: " + orderId, module);
            boolean approved = OrderChangeHelper.approveOrder(dispatcher, userLogin, orderId);
            Debug.logInfo("Test order with id: " + orderId + " has been approved: " + approved, module);
            resultMap.put("orderId", orderId);
        }
        Boolean shipOrder = (Boolean) context.get("shipOrder");
        if (shipOrder.booleanValue() && UtilValidate.isNotEmpty(orderId)) {
            try {
                dispatcher.runSync("quickShipEntireOrder", UtilMisc.toMap("orderId", orderId, "userLogin", userLogin));
                Debug.logInfo("Test sales order with id [" + orderId + "] has been shipped", module);
            } catch (GenericServiceException gse) {
                Debug.logWarning("Unable to quick ship test sales order with id [" + orderId + "] with error: " + gse.getMessage(), module);
            } catch (Exception exc) {
                Debug.logWarning("Unable to quick ship test sales order with id [" + orderId + "] with error: " + exc.getMessage(), module);
            }
        }

        return resultMap;
    }
}
