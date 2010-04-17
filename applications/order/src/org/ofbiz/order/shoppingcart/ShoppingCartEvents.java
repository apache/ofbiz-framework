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
import java.math.MathContext;
import java.sql.Timestamp;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import javolution.util.FastList;
import javolution.util.FastMap;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.ObjectType;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilFormatOut;
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
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.order.shoppingcart.product.ProductPromoWorker;
import org.ofbiz.product.catalog.CatalogWorker;
import org.ofbiz.product.config.ProductConfigWorker;
import org.ofbiz.product.config.ProductConfigWrapper;
import org.ofbiz.product.product.ProductWorker;
import org.ofbiz.product.store.ProductStoreSurveyWrapper;
import org.ofbiz.product.store.ProductStoreWorker;
import org.ofbiz.security.Security;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ModelService;
import org.ofbiz.service.ServiceUtil;
import org.ofbiz.webapp.control.RequestHandler;

/**
 * Shopping cart events.
 */
public class ShoppingCartEvents {

    public static String module = ShoppingCartEvents.class.getName();
    public static final String resource = "OrderUiLabels";
    public static final String resource_error = "OrderErrorUiLabels";

    private static final String NO_ERROR = "noerror";
    private static final String NON_CRITICAL_ERROR = "noncritical";
    private static final String ERROR = "error";

    public static final MathContext generalRounding = new MathContext(10);

    public static String addProductPromoCode(HttpServletRequest request, HttpServletResponse response) {
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        ShoppingCart cart = getCartObject(request);
        String productPromoCodeId = request.getParameter("productPromoCodeId");
        if (UtilValidate.isNotEmpty(productPromoCodeId)) {
            String checkResult = cart.addProductPromoCode(productPromoCodeId, dispatcher);
            if (UtilValidate.isNotEmpty(checkResult)) {
                request.setAttribute("_ERROR_MESSAGE_", checkResult);
                return "error";
            }
        }
        return "success";
    }

    public static String addItemGroup(HttpServletRequest request, HttpServletResponse response) {
        ShoppingCart cart = getCartObject(request);
        Map parameters = UtilHttp.getParameterMap(request);
        String groupName = (String) parameters.get("groupName");
        String parentGroupNumber = (String) parameters.get("parentGroupNumber");
        String groupNumber = cart.addItemGroup(groupName, parentGroupNumber);
        request.setAttribute("itemGroupNumber", groupNumber);
        return "success";
    }

    public static String addCartItemToGroup(HttpServletRequest request, HttpServletResponse response) {
        ShoppingCart cart = getCartObject(request);
        Map parameters = UtilHttp.getParameterMap(request);
        String itemGroupNumber = (String) parameters.get("itemGroupNumber");
        String indexStr = (String) parameters.get("lineIndex");
        int index = Integer.parseInt(indexStr);
        ShoppingCartItem cartItem = cart.findCartItem(index);
        cartItem.setItemGroup(itemGroupNumber, cart);
        return "success";
    }

    /** Event to add an item to the shopping cart. */
    public static String addToCart(HttpServletRequest request, HttpServletResponse response) {
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        ShoppingCart cart = getCartObject(request);
        ShoppingCartHelper cartHelper = new ShoppingCartHelper(delegator, dispatcher, cart);
        String controlDirective = null;
        Map result = null;
        String productId = null;
        String parentProductId = null;
        String itemType = null;
        String itemDescription = null;
        String productCategoryId = null;
        String priceStr = null;
        BigDecimal price = null;
        String quantityStr = null;
        BigDecimal quantity = BigDecimal.ZERO;
        String reservStartStr = null;
        String reservEndStr = null;
        java.sql.Timestamp reservStart = null;
        java.sql.Timestamp reservEnd = null;
        String reservLengthStr = null;
        BigDecimal reservLength = null;
        String reservPersonsStr = null;
        BigDecimal reservPersons = null;
        String accommodationMapId = null;
        String accommodationSpotId = null;
        String shipBeforeDateStr = null;
        String shipAfterDateStr = null;
        java.sql.Timestamp shipBeforeDate = null;
        java.sql.Timestamp shipAfterDate = null;

        // not used right now: Map attributes = null;
        String catalogId = CatalogWorker.getCurrentCatalogId(request);
        Locale locale = UtilHttp.getLocale(request);
        NumberFormat nf = NumberFormat.getNumberInstance(locale);

        // Get the parameters as a MAP, remove the productId and quantity params.
        Map paramMap = UtilHttp.getCombinedMap(request);

        String itemGroupNumber = (String) paramMap.get("itemGroupNumber");

        // Get shoppingList info if passed
        String shoppingListId = (String) paramMap.get("shoppingListId");
        String shoppingListItemSeqId = (String) paramMap.get("shoppingListItemSeqId");
        if (paramMap.containsKey("ADD_PRODUCT_ID")) {
            productId = (String) paramMap.remove("ADD_PRODUCT_ID");
        } else if (paramMap.containsKey("add_product_id")) {
            Object object = paramMap.remove("add_product_id");
            try {
                productId = (String) object;
            } catch (ClassCastException e) {
                productId = (String) ((List) object).get(0);
            }
        }
        if (paramMap.containsKey("PRODUCT_ID")) {
            parentProductId = (String) paramMap.remove("PRODUCT_ID");
        } else if (paramMap.containsKey("product_id")) {
            parentProductId = (String) paramMap.remove("product_id");
        }

        Debug.logInfo("adding item product " + productId, module);
        Debug.logInfo("adding item parent product " + parentProductId, module);

        if (paramMap.containsKey("ADD_CATEGORY_ID")) {
            productCategoryId = (String) paramMap.remove("ADD_CATEGORY_ID");
        } else if (paramMap.containsKey("add_category_id")) {
            productCategoryId = (String) paramMap.remove("add_category_id");
        }
        if (productCategoryId != null && productCategoryId.length() == 0) {
            productCategoryId = null;
        }

        if (paramMap.containsKey("ADD_ITEM_TYPE")) {
            itemType = (String) paramMap.remove("ADD_ITEM_TYPE");
        } else if (paramMap.containsKey("add_item_type")) {
            itemType = (String) paramMap.remove("add_item_type");
        }

        if (UtilValidate.isEmpty(productId)) {
            // before returning error; check make sure we aren't adding a special item type
            if (UtilValidate.isEmpty(itemType)) {
                request.setAttribute("_ERROR_MESSAGE_", UtilProperties.getMessage(resource_error, "cart.addToCart.noProductInfoPassed", locale));
                return "success"; // not critical return to same page
            }
        } else {
            try {
                String pId = ProductWorker.findProductId(delegator, productId);
                if (pId != null) {
                    productId = pId;
                }
            } catch (Throwable e) {
                Debug.logWarning(e, module);
            }
        }

        // check for an itemDescription
        if (paramMap.containsKey("ADD_ITEM_DESCRIPTION")) {
            itemDescription = (String) paramMap.remove("ADD_ITEM_DESCRIPTION");
        } else if (paramMap.containsKey("add_item_description")) {
            itemDescription = (String) paramMap.remove("add_item_description");
        }
        if (itemDescription != null && itemDescription.length() == 0) {
            itemDescription = null;
        }

        // Get the ProductConfigWrapper (it's not null only for configurable items)
        ProductConfigWrapper configWrapper = null;
        configWrapper = ProductConfigWorker.getProductConfigWrapper(productId, cart.getCurrency(), request);

        if (configWrapper != null) {
            if (paramMap.containsKey("configId")) {
                try {
                    configWrapper.loadConfig(delegator, (String) paramMap.remove("configId"));
                } catch (Exception e) {
                    Debug.logWarning(e, "Could not load configuration", module);
                }
            } else {
                // The choices selected by the user are taken from request and set in the wrapper
                ProductConfigWorker.fillProductConfigWrapper(configWrapper, request);
            }
            if (!configWrapper.isCompleted()) {
                // The configuration is not valid
                request.setAttribute("product_id", productId);
                request.setAttribute("_EVENT_MESSAGE_", UtilProperties.getMessage(resource_error, "cart.addToCart.configureProductBeforeAddingToCart", locale));
                return "product";
            } else {
                // load the Config Id
                ProductConfigWorker.storeProductConfigWrapper(configWrapper, delegator);
            }
        }


        //Check for virtual products
        if (ProductWorker.isVirtual(delegator, productId)) {

            if ("VV_FEATURETREE".equals(ProductWorker.getProductVirtualVariantMethod(delegator, productId))) {
                // get the selected features.
                List<String> selectedFeatures = new LinkedList<String>();
                java.util.Enumeration paramNames = request.getParameterNames();
                while (paramNames.hasMoreElements()) {
                    String paramName = (String) paramNames.nextElement();
                    if (paramName.startsWith("FT")) {
                        selectedFeatures.add(request.getParameterValues(paramName)[0]);
                    }
                }

                // check if features are selected
                if (UtilValidate.isEmpty(selectedFeatures)) {
                    request.setAttribute("product_id", productId);
                    request.setAttribute("_EVENT_MESSAGE_", UtilProperties.getMessage(resource_error, "cart.addToCart.chooseVariationBeforeAddingToCart", locale));
                    return "product";
                }

                String variantProductId = ProductWorker.getVariantFromFeatureTree(productId, selectedFeatures, delegator);
                if (UtilValidate.isNotEmpty(variantProductId)) {
                    productId = variantProductId;
                } else {
                    request.setAttribute("product_id", productId);
                    request.setAttribute("_EVENT_MESSAGE_", UtilProperties.getMessage(resource_error, "cart.addToCart.incompatibilityVariantFeature", locale));
                    return "product";
                }

            } else {
                request.setAttribute("product_id", productId);
                request.setAttribute("_EVENT_MESSAGE_", UtilProperties.getMessage(resource_error, "cart.addToCart.chooseVariationBeforeAddingToCart", locale));
                return "product";
            }
        }

        // get the override price
        if (paramMap.containsKey("PRICE")) {
            priceStr = (String) paramMap.remove("PRICE");
        } else if (paramMap.containsKey("price")) {
            priceStr = (String) paramMap.remove("price");
        }
        if (priceStr == null) {
            priceStr = "0";  // default price is 0
        }

        // get the renting data
        if ("ASSET_USAGE".equals(ProductWorker.getProductTypeId(delegator, productId))) {
            if (paramMap.containsKey("reservStart")) {
                reservStartStr = (String) paramMap.remove("reservStart");
                if (reservStartStr.length() == 10) // only date provided, no time string?
                    reservStartStr += " 00:00:00.000000000"; // should have format: yyyy-mm-dd hh:mm:ss.fffffffff
                if (reservStartStr.length() > 0) {
                    try {
                        reservStart = java.sql.Timestamp.valueOf(reservStartStr);
                    } catch (Exception e) {
                        Debug.logWarning(e, "Problems parsing Reservation start string: "
                                + reservStartStr, module);
                        reservStart = null;
                        request.setAttribute("_ERROR_MESSAGE_", UtilProperties.getMessage(resource_error, "cart.addToCart.rental.startDate", locale));
                        return "error";
                    }
                } else reservStart = null;
            }

            if (paramMap.containsKey("reservEnd")) {
                reservEndStr = (String) paramMap.remove("reservEnd");
                if (reservEndStr.length() == 10) // only date provided, no time string?
                    reservEndStr += " 00:00:00.000000000"; // should have format: yyyy-mm-dd hh:mm:ss.fffffffff
                if (reservEndStr.length() > 0) {
                    try {
                        reservEnd = java.sql.Timestamp.valueOf(reservEndStr);
                    } catch (Exception e) {
                        Debug.logWarning(e, "Problems parsing Reservation end string: " + reservEndStr, module);
                        reservEnd = null;
                        request.setAttribute("_ERROR_MESSAGE_", UtilProperties.getMessage(resource_error, "cart.addToCart.rental.endDate", locale));
                        return "error";
                    }
                } else reservEnd = null;
            }

            if (reservStart != null && reservEnd != null) {
                reservLength = new BigDecimal(UtilDateTime.getInterval(reservStart, reservEnd)).divide(new BigDecimal("86400000"), generalRounding);
            }

            if (reservStart != null && paramMap.containsKey("reservLength")) {
                reservLengthStr = (String) paramMap.remove("reservLength");
                // parse the reservation Length
                try {
                    reservLength = (BigDecimal) ObjectType.simpleTypeConvert(reservLengthStr, "BigDecimal", null, locale);
                } catch (Exception e) {
                    Debug.logWarning(e, "Problems parsing reservation length string: "
                            + reservLengthStr, module);
                    reservLength = BigDecimal.ONE;
                    request.setAttribute("_ERROR_MESSAGE_", UtilProperties.getMessage(resource_error, "OrderReservationLengthShouldBeAPositiveNumber", locale));
                    return "error";
                }
            }

            if (reservStart != null && paramMap.containsKey("reservPersons")) {
                reservPersonsStr = (String) paramMap.remove("reservPersons");
                // parse the number of persons
                try {
                    reservPersons = (BigDecimal) ObjectType.simpleTypeConvert(reservPersonsStr, "BigDecimal", null, locale);
                } catch (Exception e) {
                    Debug.logWarning(e, "Problems parsing reservation number of persons string: " + reservPersonsStr, module);
                    reservPersons = BigDecimal.ONE;
                    request.setAttribute("_ERROR_MESSAGE_", UtilProperties.getMessage(resource_error, "OrderNumberOfPersonsShouldBeOneOrLarger", locale));
                    return "error";
                }
            }

            //check for valid rental parameters
            if (UtilValidate.isEmpty(reservStart) && UtilValidate.isEmpty(reservLength) && UtilValidate.isEmpty(reservPersons)) {
                request.setAttribute("product_id", productId);
                request.setAttribute("_EVENT_MESSAGE_", UtilProperties.getMessage(resource_error, "cart.addToCart.enterBookingInforamtionBeforeAddingToCart", locale));
                return "product";
            }

            //check accommodation for reservations
            if ((paramMap.containsKey("accommodationMapId")) && (paramMap.containsKey("accommodationSpotId"))) {
                accommodationMapId = (String) paramMap.remove("accommodationMapId");
                accommodationSpotId = (String) paramMap.remove("accommodationSpotId");
            }
        }

        // get the quantity
        if (paramMap.containsKey("QUANTITY")) {
            quantityStr = (String) paramMap.remove("QUANTITY");
        } else if (paramMap.containsKey("quantity")) {
            quantityStr = (String) paramMap.remove("quantity");
        }
        if (UtilValidate.isEmpty(quantityStr)) {
            quantityStr = "1";  // default quantity is 1
        }

        // parse the price
        try {
            price = (BigDecimal) ObjectType.simpleTypeConvert(priceStr, "BigDecimal", null, locale);
        } catch (Exception e) {
            Debug.logWarning(e, "Problems parsing price string: " + priceStr, module);
            price = null;
        }

        // parse the quantity
        try {
            quantity = (BigDecimal) ObjectType.simpleTypeConvert(quantityStr, "BigDecimal", null, locale);
        } catch (Exception e) {
            Debug.logWarning(e, "Problems parsing quantity string: " + quantityStr, module);
            quantity = BigDecimal.ONE;
        }

        // get the selected amount
        String selectedAmountStr = null;
        if (paramMap.containsKey("ADD_AMOUNT")) {
            selectedAmountStr = (String) paramMap.remove("ADD_AMOUNT");
        } else if (paramMap.containsKey("add_amount")) {
            selectedAmountStr = (String) paramMap.remove("add_amount");
        }

        // parse the amount
        BigDecimal amount = null;
        if (UtilValidate.isNotEmpty(selectedAmountStr)) {
            try {
                amount = (BigDecimal) ObjectType.simpleTypeConvert(selectedAmountStr, "BigDecimal", null, locale);
            } catch (Exception e) {
                Debug.logWarning(e, "Problem parsing amount string: " + selectedAmountStr, module);
                amount = null;
            }
        } else {
            amount = BigDecimal.ZERO;
        }

        // check for required amount
        if ((ProductWorker.isAmountRequired(delegator, productId)) && (amount == null || amount.doubleValue() == 0.0)) {
            request.setAttribute("product_id", productId);
            request.setAttribute("_EVENT_MESSAGE_", UtilProperties.getMessage(resource_error, "cart.addToCart.enterAmountBeforeAddingToCart", locale));
            return "product";
        }

        // get the ship before date (handles both yyyy-mm-dd input and full timestamp)
        shipBeforeDateStr = (String) paramMap.remove("shipBeforeDate");
        if (UtilValidate.isNotEmpty(shipBeforeDateStr)) {
            if (shipBeforeDateStr.length() == 10) shipBeforeDateStr += " 00:00:00.000";
            try {
                shipBeforeDate = java.sql.Timestamp.valueOf(shipBeforeDateStr);
            } catch (IllegalArgumentException e) {
                Debug.logWarning(e, "Bad shipBeforeDate input: " + e.getMessage(), module);
                shipBeforeDate = null;
            }
        }

        // get the ship after date (handles both yyyy-mm-dd input and full timestamp)
        shipAfterDateStr = (String) paramMap.remove("shipAfterDate");
        if (UtilValidate.isNotEmpty(shipAfterDateStr)) {
            if (shipAfterDateStr.length() == 10) shipAfterDateStr += " 00:00:00.000";
            try {
                shipAfterDate = java.sql.Timestamp.valueOf(shipAfterDateStr);
            } catch (IllegalArgumentException e) {
                Debug.logWarning(e, "Bad shipAfterDate input: " + e.getMessage(), module);
                shipAfterDate = null;
            }
        }

        // check for an add-to cart survey
        List surveyResponses = null;
        if (productId != null) {
            String productStoreId = ProductStoreWorker.getProductStoreId(request);
            List productSurvey = ProductStoreWorker.getProductSurveys(delegator, productStoreId, productId, "CART_ADD", parentProductId);
            if (UtilValidate.isNotEmpty(productSurvey)) {
                // TODO: implement multiple survey per product
                GenericValue survey = EntityUtil.getFirst(productSurvey);
                String surveyResponseId = (String) request.getAttribute("surveyResponseId");
                if (surveyResponseId != null) {
                    surveyResponses = UtilMisc.toList(surveyResponseId);
                } else {
                    String origParamMapId = UtilHttp.stashParameterMap(request);
                    Map<String, Object> surveyContext = UtilMisc.<String, Object>toMap("_ORIG_PARAM_MAP_ID_", origParamMapId);
                    GenericValue userLogin = cart.getUserLogin();
                    String partyId = null;
                    if (userLogin != null) {
                        partyId = userLogin.getString("partyId");
                    }
                    String formAction = "/additemsurvey";
                    String nextPage = RequestHandler.getOverrideViewUri(request.getPathInfo());
                    if (nextPage != null) {
                        formAction = formAction + "/" + nextPage;
                    }
                    ProductStoreSurveyWrapper wrapper = new ProductStoreSurveyWrapper(survey, partyId, surveyContext);
                    request.setAttribute("surveyWrapper", wrapper);
                    request.setAttribute("surveyAction", formAction); // will be used as the form action of the survey
                    return "survey";
                }
            }
        }
        if (surveyResponses != null) {
            paramMap.put("surveyResponses", surveyResponses);
        }

        GenericValue productStore = ProductStoreWorker.getProductStore(request);
        if (productStore != null) {
            String addToCartRemoveIncompat = productStore.getString("addToCartRemoveIncompat");
            String addToCartReplaceUpsell = productStore.getString("addToCartReplaceUpsell");
            try {
                if ("Y".equals(addToCartRemoveIncompat)) {
                    List productAssocs = null;
                    EntityCondition cond = EntityCondition.makeCondition(UtilMisc.toList(
                            EntityCondition.makeCondition(EntityCondition.makeCondition("productId", EntityOperator.EQUALS, productId), EntityOperator.OR, EntityCondition.makeCondition("productIdTo", EntityOperator.EQUALS, productId)),
                            EntityCondition.makeCondition("productAssocTypeId", EntityOperator.EQUALS, "PRODUCT_INCOMPATABLE")), EntityOperator.AND);
                    productAssocs = delegator.findList("ProductAssoc", cond, null, null, null, false);
                    productAssocs = EntityUtil.filterByDate(productAssocs);
                    List productList = FastList.newInstance();
                    Iterator iter = productAssocs.iterator();
                    while (iter.hasNext()) {
                        GenericValue productAssoc = (GenericValue) iter.next();
                        if (productId.equals(productAssoc.getString("productId"))) {
                            productList.add(productAssoc.getString("productIdTo"));
                            continue;
                        }
                        if (productId.equals(productAssoc.getString("productIdTo"))) {
                            productList.add(productAssoc.getString("productId"));
                            continue;
                        }
                    }
                    Iterator sciIter = cart.iterator();
                    while (sciIter.hasNext()) {
                        ShoppingCartItem sci = (ShoppingCartItem) sciIter.next();
                        if (productList.contains(sci.getProductId())) {
                            try {
                                cart.removeCartItem(sci, dispatcher);
                            } catch (CartItemModifyException e) {
                                Debug.logError(e.getMessage(), module);
                            }
                        }
                    }
                }
                if ("Y".equals(addToCartReplaceUpsell)) {
                    List productList = null;
                    EntityCondition cond = EntityCondition.makeCondition(UtilMisc.toList(
                            EntityCondition.makeCondition("productIdTo", EntityOperator.EQUALS, productId),
                            EntityCondition.makeCondition("productAssocTypeId", EntityOperator.EQUALS, "PRODUCT_UPGRADE")), EntityOperator.AND);
                    productList = delegator.findList("ProductAssoc", cond, UtilMisc.toSet("productId"), null, null, false);
                    if (productList != null) {
                        Iterator sciIter = cart.iterator();
                        while (sciIter.hasNext()) {
                            ShoppingCartItem sci = (ShoppingCartItem) sciIter.next();
                            if (productList.contains(sci.getProductId())) {
                                try {
                                    cart.removeCartItem(sci, dispatcher);
                                } catch (CartItemModifyException e) {
                                    Debug.logError(e.getMessage(), module);
                                }
                            }
                        }
                    }
                }
            } catch (GenericEntityException e) {
                Debug.logError(e.getMessage(), module);
            }
        }

        // Translate the parameters and add to the cart
        result = cartHelper.addToCart(catalogId, shoppingListId, shoppingListItemSeqId, productId, productCategoryId,
                itemType, itemDescription, price, amount, quantity, reservStart, reservLength, reservPersons,
                accommodationMapId, accommodationSpotId,
                shipBeforeDate, shipAfterDate, configWrapper, itemGroupNumber, paramMap, parentProductId);
        controlDirective = processResult(result, request);

        // Determine where to send the browser
        if (controlDirective.equals(ERROR)) {
            return "error";
        } else {
            if (cart.viewCartOnAdd()) {
                return "viewcart";
            } else {
                return "success";
            }
        }
    }

    public static String addToCartFromOrder(HttpServletRequest request, HttpServletResponse response) {
        String orderId = request.getParameter("orderId");
        String itemGroupNumber = request.getParameter("itemGroupNumber");
        String[] itemIds = request.getParameterValues("item_id");
        // not used yet: Locale locale = UtilHttp.getLocale(request);

        ShoppingCart cart = getCartObject(request);
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        ShoppingCartHelper cartHelper = new ShoppingCartHelper(delegator, dispatcher, cart);
        String catalogId = CatalogWorker.getCurrentCatalogId(request);
        Map result;
        String controlDirective;

        boolean addAll = ("true".equals(request.getParameter("add_all")));
        result = cartHelper.addToCartFromOrder(catalogId, orderId, itemIds, addAll, itemGroupNumber);
        controlDirective = processResult(result, request);

        //Determine where to send the browser
        if (controlDirective.equals(ERROR)) {
            return "error";
        } else {
            return "success";
        }
    }

    /** Adds all products in a category according to quantity request parameter
     * for each; if no parameter for a certain product in the category, or if
     * quantity is 0, do not add
     */
    public static String addToCartBulk(HttpServletRequest request, HttpServletResponse response) {
        String categoryId = request.getParameter("category_id");
        ShoppingCart cart = getCartObject(request);
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        ShoppingCartHelper cartHelper = new ShoppingCartHelper(delegator, dispatcher, cart);
        String controlDirective;
        Map result;
        // not used yet: Locale locale = UtilHttp.getLocale(request);

        //Convert the params to a map to pass in
        Map paramMap = UtilHttp.getParameterMap(request);
        String catalogId = CatalogWorker.getCurrentCatalogId(request);
        result = cartHelper.addToCartBulk(catalogId, categoryId, paramMap);
        controlDirective = processResult(result, request);

        //Determine where to send the browser
        if (controlDirective.equals(ERROR)) {
            return "error";
        } else {
            return "success";
        }
    }

    public static String quickInitPurchaseOrder(HttpServletRequest request, HttpServletResponse response) {
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        HttpSession session = request.getSession();
        Locale locale = UtilHttp.getLocale(request);

        ShoppingCart cart = new WebShoppingCart(request);
        // TODO: the code below here needs some cleanups
        String billToCustomerPartyId = request.getParameter("billToCustomerPartyId_o_0");
        String supplierPartyId = request.getParameter("supplierPartyId_o_0");
        if (UtilValidate.isEmpty(billToCustomerPartyId) && UtilValidate.isEmpty(supplierPartyId)) {
            request.setAttribute("_ERROR_MESSAGE_", UtilProperties.getMessage(resource_error, "OrderCouldNotInitPurchaseOrder", locale));
            return "error";
        }
        String orderId = request.getParameter("orderId_o_0");
        // set the order id if supplied
        if (UtilValidate.isNotEmpty(orderId)) {
            GenericValue thisOrder = null;
            try {
                thisOrder = delegator.findOne("OrderHeader", UtilMisc.toMap("orderId", orderId), false);
            } catch (GenericEntityException e) {
                Debug.logError(e.getMessage(), module);
            }
            if (thisOrder == null) {
                cart.setOrderId(orderId);
            } else {
                request.setAttribute("_ERROR_MESSAGE_", UtilProperties.getMessage(resource_error,"OrderIdAlreadyExistsPleaseChooseAnother", locale));
                return "error";
            }
        }
        cart.setBillToCustomerPartyId(billToCustomerPartyId);
        cart.setBillFromVendorPartyId(supplierPartyId);
        cart.setOrderPartyId(supplierPartyId);
        cart.setOrderId(orderId);
        String agreementId = request.getParameter("agreementId_o_0");
        if (UtilValidate.isNotEmpty(agreementId)) {
            ShoppingCartHelper sch = new ShoppingCartHelper(delegator, dispatcher, cart);
            sch.selectAgreement(agreementId);
        }

        cart.setOrderType("PURCHASE_ORDER");

        session.setAttribute("shoppingCart", cart);
        session.setAttribute("productStoreId", cart.getProductStoreId());
        session.setAttribute("orderMode", cart.getOrderType());
        session.setAttribute("orderPartyId", cart.getOrderPartyId());

        return "success";
    }

    public static String quickCheckoutOrderWithDefaultOptions(HttpServletRequest request, HttpServletResponse response) {
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        ShoppingCart cart = getCartObject(request);

        // Set the cart's default checkout options for a quick checkout
        cart.setDefaultCheckoutOptions(dispatcher);

        return "success";
    }

    /** Adds a set of requirements to the cart
     */
    public static String addToCartBulkRequirements(HttpServletRequest request, HttpServletResponse response) {
        ShoppingCart cart = getCartObject(request);
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        ShoppingCartHelper cartHelper = new ShoppingCartHelper(delegator, dispatcher, cart);
        String controlDirective;
        Map result;
        // not used yet: Locale locale = UtilHttp.getLocale(request);

        //Convert the params to a map to pass in
        Map paramMap = UtilHttp.getParameterMap(request);
        String catalogId = CatalogWorker.getCurrentCatalogId(request);
        result = cartHelper.addToCartBulkRequirements(catalogId, paramMap);
        controlDirective = processResult(result, request);

        //Determine where to send the browser
        if (controlDirective.equals(ERROR)) {
            return "error";
        } else {
            return "success";
        }
    }

    /** Adds all products in a category according to default quantity on ProductCategoryMember
     * for each; if no default for a certain product in the category, or if
     * quantity is 0, do not add
     */
    public static String addCategoryDefaults(HttpServletRequest request, HttpServletResponse response) {
        String itemGroupNumber = request.getParameter("itemGroupNumber");
        String categoryId = request.getParameter("category_id");
        String catalogId = CatalogWorker.getCurrentCatalogId(request);
        ShoppingCart cart = getCartObject(request);
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        ShoppingCartHelper cartHelper = new ShoppingCartHelper(delegator, dispatcher, cart);
        String controlDirective;
        Map result;
        BigDecimal totalQuantity;
        Locale locale = UtilHttp.getLocale(request);

        result = cartHelper.addCategoryDefaults(catalogId, categoryId, itemGroupNumber);
        controlDirective = processResult(result, request);

        //Determine where to send the browser
        if (controlDirective.equals(ERROR)) {
            return "error";
        } else {
            totalQuantity = (BigDecimal)result.get("totalQuantity");
            Map messageMap = UtilMisc.toMap("totalQuantity", UtilFormatOut.formatQuantity(totalQuantity.doubleValue()));

            request.setAttribute("_EVENT_MESSAGE_",
                                  UtilProperties.getMessage(resource_error, "cart.add_category_defaults",
                                          messageMap, locale));

            return "success";
        }
    }

    /** Delete an item from the shopping cart. */
    public static String deleteFromCart(HttpServletRequest request, HttpServletResponse response) {
        ShoppingCart cart = getCartObject(request);
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        ShoppingCartHelper cartHelper = new ShoppingCartHelper(null, dispatcher, cart);
        String controlDirective;
        Map result;
        Map paramMap = UtilHttp.getParameterMap(request);
        // not used yet: Locale locale = UtilHttp.getLocale(request);

        //Delegate the cart helper
        result = cartHelper.deleteFromCart(paramMap);
        controlDirective = processResult(result, request);

        //Determine where to send the browser
        if (controlDirective.equals(ERROR)) {
            return "error";
        } else {
            return "success";
        }
    }

    /** Update the items in the shopping cart. */
    public static String modifyCart(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession();
        ShoppingCart cart = getCartObject(request);
        Locale locale = UtilHttp.getLocale(request);
        GenericValue userLogin = (GenericValue) session.getAttribute("userLogin");
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        Security security = (Security) request.getAttribute("security");
        ShoppingCartHelper cartHelper = new ShoppingCartHelper(null, dispatcher, cart);
        String controlDirective;
        Map result;
        // not used yet: Locale locale = UtilHttp.getLocale(request);

        Map paramMap = UtilHttp.getParameterMap(request);

        String removeSelectedFlag = request.getParameter("removeSelected");
        String selectedItems[] = request.getParameterValues("selectedItem");
        boolean removeSelected = ("true".equals(removeSelectedFlag) && selectedItems != null && selectedItems.length > 0);
        result = cartHelper.modifyCart(security, userLogin, paramMap, removeSelected, selectedItems, locale);
        controlDirective = processResult(result, request);

        //Determine where to send the browser
        if (controlDirective.equals(ERROR)) {
            return "error";
        } else {
            return "success";
        }
    }

    /** Empty the shopping cart. */
    public static String clearCart(HttpServletRequest request, HttpServletResponse response) {
        ShoppingCart cart = getCartObject(request);
        cart.clear();

        // if this was an anonymous checkout process, go ahead and clear the session and such now that the order is placed; we don't want this to mess up additional orders and such
        HttpSession session = request.getSession();
        GenericValue userLogin = (GenericValue) session.getAttribute("userLogin");
        if (userLogin != null && "anonymous".equals(userLogin.get("userLoginId"))) {
            // here we want to do a full logout, but not using the normal logout stuff because it saves things in the UserLogin record that we don't want changed for the anonymous user
            session.invalidate();
            session = request.getSession(true);

            // to allow the display of the order confirmation page put the userLogin in the request, but leave it out of the session
            request.setAttribute("temporaryAnonymousUserLogin", userLogin);

            Debug.logInfo("Doing clearCart for anonymous user, so logging out but put anonymous userLogin in temporaryAnonymousUserLogin request attribute", module);
        }

        return "success";
    }

    /** Totally wipe out the cart, removes all stored info. */
    public static String destroyCart(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession();
        clearCart(request, response);
        session.removeAttribute("shoppingCart");
        session.removeAttribute("orderPartyId");
        session.removeAttribute("orderMode");
        session.removeAttribute("productStoreId");
        session.removeAttribute("CURRENT_CATALOG_ID");
        return "success";
    }

    /** Gets or creates the shopping cart object */
    public static ShoppingCart getCartObject(HttpServletRequest request, Locale locale, String currencyUom) {
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        ShoppingCart cart = (ShoppingCart) request.getAttribute("shoppingCart");
        HttpSession session = request.getSession(true);
        if (cart == null) {
            cart = (ShoppingCart) session.getAttribute("shoppingCart");
        } else {
            session.setAttribute("shoppingCart", cart);
        }

        if (cart == null) {
            cart = new WebShoppingCart(request, locale, currencyUom);
            session.setAttribute("shoppingCart", cart);
        } else {
            if (locale != null && !locale.equals(cart.getLocale())) {
                cart.setLocale(locale);
            }
            if (currencyUom != null && !currencyUom.equals(cart.getCurrency())) {
                try {
                    cart.setCurrency(dispatcher, currencyUom);
                } catch (CartItemModifyException e) {
                    Debug.logError(e, "Unable to modify currency in cart", module);
                }
            }
        }
        return cart;
    }

    /** Main get cart method; uses the locale & currency from the session */
    public static ShoppingCart getCartObject(HttpServletRequest request) {
        return getCartObject(request, null, null);
    }

    public static String switchCurrentCartObject(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession(true);
        String cartIndexStr = request.getParameter("cartIndex");
        int cartIndex = -1;
        if (UtilValidate.isNotEmpty(cartIndexStr) && UtilValidate.isInteger(cartIndexStr)) {
            try {
                cartIndex = Integer.parseInt(cartIndexStr);
            } catch (NumberFormatException nfe) {
                Debug.logWarning("Invalid value for cart index =" + cartIndexStr, module);
            }
        }
        List cartList = (List) session.getAttribute("shoppingCartList");
        if (UtilValidate.isEmpty(cartList)) {
            cartList = FastList.newInstance();
            session.setAttribute("shoppingCartList", cartList);
        }
        ShoppingCart currentCart = (ShoppingCart) session.getAttribute("shoppingCart");
        if (currentCart != null) {
            cartList.add(currentCart);
            session.setAttribute("shoppingCartList", cartList);
            session.removeAttribute("shoppingCart");
            //destroyCart(request, response);
        }
        ShoppingCart newCart = null;
        if (cartIndex >= 0 && cartIndex < cartList.size()) {
            newCart = (ShoppingCart) cartList.remove(cartIndex);
        } else {
            String productStoreId = request.getParameter("productStoreId");
            if (UtilValidate.isNotEmpty(productStoreId)) {
                session.setAttribute("productStoreId", productStoreId);
            }
            newCart = getCartObject(request);
        }
        session.setAttribute("shoppingCart", newCart);
        return "success";
    }

    public static String clearCartFromList(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession(true);
        String cartIndexStr = request.getParameter("cartIndex");
        int cartIndex = -1;
        if (UtilValidate.isNotEmpty(cartIndexStr) && UtilValidate.isInteger(cartIndexStr)) {
            try {
                cartIndex = Integer.parseInt(cartIndexStr);
            } catch (NumberFormatException nfe) {
                Debug.logWarning("Invalid value for cart index =" + cartIndexStr, module);
            }
        }
        List cartList = (List) session.getAttribute("shoppingCartList");
        if (UtilValidate.isNotEmpty(cartList) && cartIndex >= 0 && cartIndex < cartList.size()) {
            cartList.remove(cartIndex);
        }
        return "success";
    }

    /** Update the cart's UserLogin object if it isn't already set. */
    public static String keepCartUpdated(HttpServletRequest request, HttpServletResponse response) {
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        HttpSession session = request.getSession();
        ShoppingCart cart = getCartObject(request);

        // if we just logged in set the UL
        if (cart.getUserLogin() == null) {
            GenericValue userLogin = (GenericValue) session.getAttribute("userLogin");
            if (userLogin != null) {
                try {
                    cart.setUserLogin(userLogin, dispatcher);
                } catch (CartItemModifyException e) {
                    Debug.logWarning(e, module);
                }
            }
        }

        // same for autoUserLogin
        if (cart.getAutoUserLogin() == null) {
            GenericValue autoUserLogin = (GenericValue) session.getAttribute("autoUserLogin");
            if (autoUserLogin != null) {
                if (cart.getUserLogin() == null) {
                    try {
                        cart.setAutoUserLogin(autoUserLogin, dispatcher);
                    } catch (CartItemModifyException e) {
                        Debug.logWarning(e, module);
                    }
                } else {
                    cart.setAutoUserLogin(autoUserLogin);
                }
            }
        }

        // update the locale
        Locale locale = UtilHttp.getLocale(request);
        if (cart.getLocale() == null || !locale.equals(cart.getLocale())) {
            cart.setLocale(locale);
        }

        return "success";
    }

    /** For GWP Promotions with multiple alternatives, selects an alternative to the current GWP */
    public static String setDesiredAlternateGwpProductId(HttpServletRequest request, HttpServletResponse response) {
        ShoppingCart cart = getCartObject(request);
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        String alternateGwpProductId = request.getParameter("alternateGwpProductId");
        String alternateGwpLineStr = request.getParameter("alternateGwpLine");
        Locale locale = UtilHttp.getLocale(request);

        if (UtilValidate.isEmpty(alternateGwpProductId)) {
            request.setAttribute("_ERROR_MESSAGE_", UtilProperties.getMessage(resource_error,"OrderCouldNotSelectAlternateGiftNoAlternateGwpProductIdPassed", locale));
            return "error";
        }
        if (UtilValidate.isEmpty(alternateGwpLineStr)) {
            request.setAttribute("_ERROR_MESSAGE_", UtilProperties.getMessage(resource_error,"OrderCouldNotSelectAlternateGiftNoAlternateGwpLinePassed", locale));
            return "error";
        }

        int alternateGwpLine = 0;
        try {
            alternateGwpLine = Integer.parseInt(alternateGwpLineStr);
        } catch (Exception e) {
            request.setAttribute("_ERROR_MESSAGE_", UtilProperties.getMessage(resource_error,"OrderCouldNotSelectAlternateGiftAlternateGwpLineIsNotAValidNumber", locale));
            return "error";
        }

        ShoppingCartItem cartLine = cart.findCartItem(alternateGwpLine);
        if (cartLine == null) {
            request.setAttribute("_ERROR_MESSAGE_", "Could not select alternate gift, no cart line item found for #" + alternateGwpLine + ".");
            return "error";
        }

        if (cartLine.getIsPromo()) {
            // note that there should just be one promo adjustment, the reversal of the GWP, so use that to get the promo action key
            Iterator checkOrderAdjustments = UtilMisc.toIterator(cartLine.getAdjustments());
            while (checkOrderAdjustments != null && checkOrderAdjustments.hasNext()) {
                GenericValue checkOrderAdjustment = (GenericValue) checkOrderAdjustments.next();
                if (UtilValidate.isNotEmpty(checkOrderAdjustment.getString("productPromoId")) &&
                        UtilValidate.isNotEmpty(checkOrderAdjustment.getString("productPromoRuleId")) &&
                        UtilValidate.isNotEmpty(checkOrderAdjustment.getString("productPromoActionSeqId"))) {
                    GenericPK productPromoActionPk = delegator.makeValidValue("ProductPromoAction", checkOrderAdjustment).getPrimaryKey();
                    cart.setDesiredAlternateGiftByAction(productPromoActionPk, alternateGwpProductId);
                    if (cart.getOrderType().equals("SALES_ORDER")) {
                        org.ofbiz.order.shoppingcart.product.ProductPromoWorker.doPromotions(cart, dispatcher);
                    }
                    return "success";
                }
            }
        }

        request.setAttribute("_ERROR_MESSAGE_", "Could not select alternate gift, cart line item found for #" + alternateGwpLine + " does not appear to be a valid promotional gift.");
        return "error";
    }

    /** Associates a party to order */
    public static String addAdditionalParty(HttpServletRequest request, HttpServletResponse response) {
        ShoppingCart cart = getCartObject(request);
        String partyId = request.getParameter("additionalPartyId");
        String roleTypeId[] = request.getParameterValues("additionalRoleTypeId");
        List eventList = new LinkedList();
        Locale locale = UtilHttp.getLocale(request);
        int i;

        if (UtilValidate.isEmpty(partyId) || UtilValidate.isEmpty(roleTypeId) || roleTypeId.length < 1) {
            request.setAttribute("_ERROR_MESSAGE_", UtilProperties.getMessage(resource_error,"OrderPartyIdAndOrRoleTypeIdNotDefined", locale));
            return "error";
        }

        if (request.getAttribute("_EVENT_MESSAGE_LIST_") != null) {
            eventList.addAll((List) request.getAttribute("_EVENT_MESSAGE_LIST_"));
        }

        for (i = 0; i < roleTypeId.length; i++) {
            try {
                cart.addAdditionalPartyRole(partyId, roleTypeId[i]);
            } catch (Exception e) {
                eventList.add(e.getLocalizedMessage());
            }
        }

        request.removeAttribute("_EVENT_MESSAGE_LIST_");
        request.setAttribute("_EVENT_MESSAGE_LIST_", eventList);
        return "success";
    }

    /** Removes a previously associated party to order */
    public static String removeAdditionalParty(HttpServletRequest request, HttpServletResponse response) {
        ShoppingCart cart = getCartObject(request);
        String partyId = request.getParameter("additionalPartyId");
        String roleTypeId[] = request.getParameterValues("additionalRoleTypeId");
        List eventList = new LinkedList();
        Locale locale = UtilHttp.getLocale(request);
        int i;

        if (UtilValidate.isEmpty(partyId) || roleTypeId.length < 1) {
            request.setAttribute("_ERROR_MESSAGE_", UtilProperties.getMessage(resource_error,"OrderPartyIdAndOrRoleTypeIdNotDefined", locale));
            return "error";
        }

        if (request.getAttribute("_EVENT_MESSAGE_LIST_") != null) {
            eventList.addAll((List) request.getAttribute("_EVENT_MESSAGE_LIST_"));
        }

        for (i = 0; i < roleTypeId.length; i++) {
            try {
                cart.removeAdditionalPartyRole(partyId, roleTypeId[i]);
            } catch (Exception e) {
                Debug.logInfo(e.getLocalizedMessage(), module);
                eventList.add(e.getLocalizedMessage());
            }
        }

        request.removeAttribute("_EVENT_MESSAGE_LIST_");
        request.setAttribute("_EVENT_MESSAGE_LIST_", eventList);
        return "success";
    }

    /**
     * This should be called to translate the error messages of the
     * <code>ShoppingCartHelper</code> to an appropriately formatted
     * <code>String</code> in the request object and indicate whether
     * the result was an error or not and whether the errors were
     * critical or not
     *
     * @param result    The result returned from the
     * <code>ShoppingCartHelper</code>
     * @param request The servlet request instance to set the error messages
     * in
     * @return one of NON_CRITICAL_ERROR, ERROR or NO_ERROR.
     */
    private static String processResult(Map result, HttpServletRequest request) {
        //Check for errors
        StringBuilder errMsg = new StringBuilder();
        if (result.containsKey(ModelService.ERROR_MESSAGE_LIST)) {
            List errorMsgs = (List)result.get(ModelService.ERROR_MESSAGE_LIST);
            Iterator iterator = errorMsgs.iterator();
            errMsg.append("<ul>");
            while (iterator.hasNext()) {
                errMsg.append("<li>");
                errMsg.append(iterator.next());
                errMsg.append("</li>");
            }
            errMsg.append("</ul>");
        } else if (result.containsKey(ModelService.ERROR_MESSAGE)) {
            errMsg.append(result.get(ModelService.ERROR_MESSAGE));
            request.setAttribute("_ERROR_MESSAGE_", errMsg.toString());
        }

        //See whether there was an error
        if (errMsg.length() > 0) {
            request.setAttribute("_ERROR_MESSAGE_", errMsg.toString());
            if (result.get(ModelService.RESPONSE_MESSAGE).equals(ModelService.RESPOND_SUCCESS)) {
                return NON_CRITICAL_ERROR;
            } else {
                return ERROR;
            }
        } else {
            return NO_ERROR;
        }
    }

    /** Assign agreement **/
    public static String selectAgreement(HttpServletRequest request, HttpServletResponse response) {
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        ShoppingCart cart = getCartObject(request);
        ShoppingCartHelper cartHelper = new ShoppingCartHelper(delegator, dispatcher, cart);
        String agreementId = request.getParameter("agreementId");
        Map result = cartHelper.selectAgreement(agreementId);
        if (ServiceUtil.isError(result)) {
           request.setAttribute("_ERROR_MESSAGE_", ServiceUtil.getErrorMessage(result));
           return "error";
        }
        return "success";
    }

    /** Assign currency **/
    public static String setCurrency(HttpServletRequest request, HttpServletResponse response) {
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        ShoppingCart cart = getCartObject(request);
        ShoppingCartHelper cartHelper = new ShoppingCartHelper(delegator, dispatcher, cart);
        String currencyUomId = request.getParameter("currencyUomId");
        Map result = cartHelper.setCurrency(currencyUomId);
        if (ServiceUtil.isError(result)) {
           request.setAttribute("_ERROR_MESSAGE_", ServiceUtil.getErrorMessage(result));
           return "error";
        }
        return "success";
    }

    /**
     * set the order name of the cart based on request.  right now will always return "success"
     *
     */
    public static String setOrderName(HttpServletRequest request, HttpServletResponse response) {
        ShoppingCart cart = getCartObject(request);
        String orderName = request.getParameter("orderName");
        cart.setOrderName(orderName);
        return "success";
    }

    /**
     * set the PO number of the cart based on request.  right now will always return "success"
     *
     */
    public static String setPoNumber(HttpServletRequest request, HttpServletResponse response) {
        ShoppingCart cart = getCartObject(request);
        String correspondingPoId = request.getParameter("correspondingPoId");
        cart.setPoNumber(correspondingPoId);
        return "success";
    }

    /**
     * Add an order term *
     */
    public static String addOrderTerm(HttpServletRequest request, HttpServletResponse response) {

        ShoppingCart cart = getCartObject(request);
        Locale locale = UtilHttp.getLocale(request);

        String termTypeId = request.getParameter("termTypeId");
        String termValueStr = request.getParameter("termValue");
        String termDaysStr = request.getParameter("termDays");
        String textValue = request.getParameter("textValue");

        GenericValue termType = null;
        Delegator delegator = (Delegator) request.getAttribute("delegator");

        BigDecimal termValue = null;
        Long termDays = null;

        if (UtilValidate.isEmpty(termTypeId)) {
            request.setAttribute("_ERROR_MESSAGE_", UtilProperties.getMessage(resource_error, "OrderOrderTermTypeIsRequired", locale));
            return "error";
        }

        try {
            termType = delegator.findOne("TermType", UtilMisc.toMap("termTypeId", termTypeId), false);
        } catch (GenericEntityException gee) {
            request.setAttribute("_ERROR_MESSAGE_", gee.getMessage());
            return "error";
        }

        if (("FIN_PAYMENT_TERM".equals(termTypeId) && UtilValidate.isEmpty(termDaysStr)) || (UtilValidate.isNotEmpty(termType) && "FIN_PAYMENT_TERM".equals(termType.get("parentTypeId")) && UtilValidate.isEmpty(termDaysStr))) {
            request.setAttribute("_ERROR_MESSAGE_", UtilProperties.getMessage(resource_error, "OrderOrderTermDaysIsRequired", locale));
            return "error";
        }

        if (UtilValidate.isNotEmpty(termValueStr)) {
            try {
                termValue = new BigDecimal(termValueStr);
            } catch (NumberFormatException e) {
                request.setAttribute("_ERROR_MESSAGE_", UtilProperties.getMessage(resource_error, "OrderOrderTermValueError", UtilMisc.toMap("orderTermValue", termValueStr), locale));
                return "error";
            }
        }

        if (UtilValidate.isNotEmpty(termDaysStr)) {
            try {
                termDays = Long.valueOf(termDaysStr);
            } catch (NumberFormatException e) {
                request.setAttribute("_ERROR_MESSAGE_", UtilProperties.getMessage(resource_error, "OrderOrderTermDaysError", UtilMisc.toMap("orderTermDays", termDaysStr), locale));
                return "error";
            }
        }

        removeOrderTerm(request, response);

        cart.addOrderTerm(termTypeId, termValue, termDays, textValue);

        return "success";
    }

    /**
     * Remove an order term *
     */
    public static String removeOrderTerm(HttpServletRequest request, HttpServletResponse response) {

        ShoppingCart cart = getCartObject(request);

        String termIndexStr = request.getParameter("termIndex");
        if (UtilValidate.isNotEmpty(termIndexStr)) {
            try {
                Integer termIndex = Integer.parseInt(termIndexStr);
                if (termIndex >= 0) {
                    List orderTerms = cart.getOrderTerms();
                    if (orderTerms != null && orderTerms.size() > termIndex) {
                        cart.removeOrderTerm(termIndex);
                    }
                }
            } catch (NumberFormatException e) {
                Debug.logWarning(e, "Error parsing termIndex: " + termIndexStr, module);
            }
        }

        return "success";
    }

    /** Initialize order entry from a shopping list **/
    public static String loadCartFromShoppingList(HttpServletRequest request, HttpServletResponse response) {
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        HttpSession session = request.getSession();
        GenericValue userLogin = (GenericValue)session.getAttribute("userLogin");

        String shoppingListId = request.getParameter("shoppingListId");

        ShoppingCart cart = null;
        try {
            Map outMap = dispatcher.runSync("loadCartFromShoppingList",
                    UtilMisc.<String, Object>toMap("shoppingListId", shoppingListId,
                    "userLogin", userLogin));
            cart = (ShoppingCart)outMap.get("shoppingCart");
        } catch (GenericServiceException exc) {
            request.setAttribute("_ERROR_MESSAGE_", exc.getMessage());
            return "error";
        }

        session.setAttribute("shoppingCart", cart);
        session.setAttribute("productStoreId", cart.getProductStoreId());
        session.setAttribute("orderMode", cart.getOrderType());
        session.setAttribute("orderPartyId", cart.getOrderPartyId());

        return "success";
    }

    /** Initialize order entry from a quote **/
    public static String loadCartFromQuote(HttpServletRequest request, HttpServletResponse response) {
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        HttpSession session = request.getSession();
        GenericValue userLogin = (GenericValue)session.getAttribute("userLogin");

        String quoteId = request.getParameter("quoteId");

        ShoppingCart cart = null;
        try {
            Map outMap = dispatcher.runSync("loadCartFromQuote",
                    UtilMisc.<String, Object>toMap("quoteId", quoteId,
                            "applyQuoteAdjustments", "true",
                            "userLogin", userLogin));
            cart = (ShoppingCart) outMap.get("shoppingCart");
        } catch (GenericServiceException exc) {
            request.setAttribute("_ERROR_MESSAGE_", exc.getMessage());
            return "error";
        }

        // Set the cart's default checkout options for a quick checkout
        cart.setDefaultCheckoutOptions(dispatcher);
        // Make the cart read-only
        cart.setReadOnlyCart(true);

        session.setAttribute("shoppingCart", cart);
        session.setAttribute("productStoreId", cart.getProductStoreId());
        session.setAttribute("orderMode", cart.getOrderType());
        session.setAttribute("orderPartyId", cart.getOrderPartyId());

        return "success";
    }

    /** Initialize order entry from an existing order **/
    public static String loadCartFromOrder(HttpServletRequest request, HttpServletResponse response) {
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        HttpSession session = request.getSession();
        GenericValue userLogin = (GenericValue)session.getAttribute("userLogin");
        Delegator delegator = (Delegator) request.getAttribute("delegator");

        String orderId = request.getParameter("orderId");

        ShoppingCart cart = null;
        try {
            Map<String, Object> outMap = dispatcher.runSync("loadCartFromOrder",
                                                UtilMisc.<String, Object>toMap("orderId", orderId,
                                                        "skipProductChecks", Boolean.TRUE, // the products have already been checked in the order, no need to check their validity again
                                                        "userLogin", userLogin));
            if (!ServiceUtil.isSuccess(outMap)) {
                request.setAttribute("_ERROR_MESSAGE_", ServiceUtil.getErrorMessage(outMap));
                return "error";
             }

            cart = (ShoppingCart) outMap.get("shoppingCart");

            cart.removeAdjustmentByType("SALES_TAX");
            cart.removeAdjustmentByType("PROMOTION_ADJUSTMENT");
            String shipGroupSeqId = null;
            long groupIndex = cart.getShipInfoSize();
            List orderAdjustmentList = new ArrayList();
            List orderAdjustments = new ArrayList();
            orderAdjustments = cart.getAdjustments();
            try {
                orderAdjustmentList = delegator.findList("OrderAdjustment", EntityCondition.makeCondition("orderId", EntityOperator.EQUALS, orderId), null, null, null, false);
            } catch (Exception e) {
                Debug.logError(e, module);
            }
            for (long itr = 1; itr <= groupIndex; itr++) {
                shipGroupSeqId = UtilFormatOut.formatPaddedNumber(1, 5);
                List<GenericValue> duplicateAdjustmentList = new ArrayList<GenericValue>();
                for (GenericValue adjustment: (List<GenericValue>)orderAdjustmentList) {
                    if ("PROMOTION_ADJUSTMENT".equals(adjustment.get("orderAdjustmentTypeId"))) {
                        cart.addAdjustment(adjustment);
                    }
                    if ("SALES_TAX".equals(adjustment.get("orderAdjustmentTypeId"))) {
                        if (adjustment.get("description") != null
                                    && ((String)adjustment.get("description")).startsWith("Tax adjustment due")) {
                                cart.addAdjustment(adjustment);
                            }
                        if ( adjustment.get("comments") != null
                                && ((String)adjustment.get("comments")).startsWith("Added manually by")) {
                            cart.addAdjustment(adjustment);
                        }
                    }
                }
                for (GenericValue orderAdjustment: (List<GenericValue>)orderAdjustments) {
                    if ("OrderAdjustment".equals(orderAdjustment.getEntityName())) {
                        if (("SHIPPING_CHARGES".equals(orderAdjustment.get("orderAdjustmentTypeId"))) &&
                                orderAdjustment.get("orderId").equals(orderId) &&
                                orderAdjustment.get("shipGroupSeqId").equals(shipGroupSeqId) && orderAdjustment.get("comments") == null) {
                            // Removing objects from list for old Shipping and Handling Charges Adjustment and Sales Tax Adjustment.
                            duplicateAdjustmentList.add(orderAdjustment);
                        }
                    }
                }
                orderAdjustments.removeAll(duplicateAdjustmentList);
            }
        } catch (GenericServiceException exc) {
            request.setAttribute("_ERROR_MESSAGE_", exc.getMessage());
            return "error";
        }

        cart.setAttribute("addpty", "Y");
        session.setAttribute("shoppingCart", cart);
        session.setAttribute("productStoreId", cart.getProductStoreId());
        session.setAttribute("orderMode", cart.getOrderType());
        session.setAttribute("orderPartyId", cart.getOrderPartyId());

        // Since we only need the cart items, so set the order id as null
        cart.setOrderId(null);
        return "success";
    }

    public static String createQuoteFromCart(HttpServletRequest request, HttpServletResponse response) {
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        HttpSession session = request.getSession();
        GenericValue userLogin = (GenericValue)session.getAttribute("userLogin");
        String destroyCart = request.getParameter("destroyCart");

        ShoppingCart cart = getCartObject(request);
        Map result = null;
        String quoteId = null;
        try {
            result = dispatcher.runSync("createQuoteFromCart",
                    UtilMisc.toMap("cart", cart,
                            "userLogin", userLogin));
            quoteId = (String) result.get("quoteId");
        } catch (GenericServiceException exc) {
            request.setAttribute("_ERROR_MESSAGE_", exc.getMessage());
            return "error";
        }
        if (ServiceUtil.isError(result)) {
           request.setAttribute("_ERROR_MESSAGE_", ServiceUtil.getErrorMessage(result));
           return "error";
        }
        request.setAttribute("quoteId", quoteId);
        if (destroyCart != null && destroyCart.equals("Y")) {
            ShoppingCartEvents.destroyCart(request, response);
        }

        return "success";
    }

    public static String createCustRequestFromCart(HttpServletRequest request, HttpServletResponse response) {
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        HttpSession session = request.getSession();
        GenericValue userLogin = (GenericValue)session.getAttribute("userLogin");
        String destroyCart = request.getParameter("destroyCart");

        ShoppingCart cart = getCartObject(request);
        Map result = null;
        String custRequestId = null;
        try {
            result = dispatcher.runSync("createCustRequestFromCart",
                    UtilMisc.toMap("cart", cart,
                            "userLogin", userLogin));
            custRequestId = (String) result.get("custRequestId");
        } catch (GenericServiceException exc) {
            request.setAttribute("_ERROR_MESSAGE_", exc.getMessage());
            return "error";
        }
        if (ServiceUtil.isError(result)) {
           request.setAttribute("_ERROR_MESSAGE_", ServiceUtil.getErrorMessage(result));
           return "error";
        }
        request.setAttribute("custRequestId", custRequestId);
        if (destroyCart != null && destroyCart.equals("Y")) {
            ShoppingCartEvents.destroyCart(request, response);
        }

        return "success";
    }

    /** Initialize order entry **/
    public static String initializeOrderEntry(HttpServletRequest request, HttpServletResponse response) {
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        HttpSession session = request.getSession();
        Security security = (Security) request.getAttribute("security");
        GenericValue userLogin = (GenericValue)session.getAttribute("userLogin");
        Locale locale = UtilHttp.getLocale(request);

        String productStoreId = request.getParameter("productStoreId");

        if (UtilValidate.isNotEmpty(productStoreId)) {
            session.setAttribute("productStoreId", productStoreId);
        }
        ShoppingCart cart = getCartObject(request);

        // TODO: re-factor and move this inside the ShoppingCart constructor
        String orderMode = request.getParameter("orderMode");
        if (orderMode != null) {
            cart.setOrderType(orderMode);
            session.setAttribute("orderMode", orderMode);
        } else {
            request.setAttribute("_ERROR_MESSAGE_", UtilProperties.getMessage(resource_error,"OrderPleaseSelectEitherSaleOrPurchaseOrder", locale));
            return "error";
        }

        // check the selected product store
        GenericValue productStore = null;
        if (UtilValidate.isNotEmpty(productStoreId)) {
            productStore = ProductStoreWorker.getProductStore(productStoreId, delegator);
            if (productStore != null) {

                // check permission for taking the order
                boolean hasPermission = false;
                if ((cart.getOrderType().equals("PURCHASE_ORDER")) && (security.hasEntityPermission("ORDERMGR", "_PURCHASE_CREATE", session))) {
                    hasPermission = true;
                } else if (cart.getOrderType().equals("SALES_ORDER")) {
                    if (security.hasEntityPermission("ORDERMGR", "_SALES_CREATE", session)) {
                        hasPermission = true;
                    } else {
                        // if the user is a rep of the store, then he also has permission
                        List storeReps = null;
                        try {
                            storeReps = delegator.findByAnd("ProductStoreRole", UtilMisc.toMap("productStoreId", productStore.getString("productStoreId"),
                                                            "partyId", userLogin.getString("partyId"), "roleTypeId", "SALES_REP"));
                        } catch (GenericEntityException gee) {
                            //
                        }
                        storeReps = EntityUtil.filterByDate(storeReps);
                        if (UtilValidate.isNotEmpty(storeReps)) {
                            hasPermission = true;
                        }
                    }
                }

                if (hasPermission) {
                    cart = ShoppingCartEvents.getCartObject(request, null, productStore.getString("defaultCurrencyUomId"));
                } else {
                    request.setAttribute("_ERROR_MESSAGE_", UtilProperties.getMessage(resource_error,"OrderYouDoNotHavePermissionToTakeOrdersForThisStore", locale));
                    cart.clear();
                    session.removeAttribute("orderMode");
                    return "error";
                }
                cart.setProductStoreId(productStoreId);
            } else {
                cart.setProductStoreId(null);
            }
        }

        if ("SALES_ORDER".equals(cart.getOrderType()) && UtilValidate.isEmpty(cart.getProductStoreId())) {
            request.setAttribute("_ERROR_MESSAGE_", UtilProperties.getMessage(resource_error,"OrderAProductStoreMustBeSelectedForASalesOrder", locale));
            cart.clear();
            session.removeAttribute("orderMode");
            return "error";
        }

        String salesChannelEnumId = request.getParameter("salesChannelEnumId");
        if (UtilValidate.isNotEmpty(salesChannelEnumId)) {
            cart.setChannelType(salesChannelEnumId);
        }

        // set party info
        String partyId = request.getParameter("supplierPartyId");
        cart.setAttribute("supplierPartyId", partyId);
        String originOrderId = request.getParameter("originOrderId");
        cart.setAttribute("originOrderId", originOrderId);

        if (!UtilValidate.isEmpty(request.getParameter("partyId"))) {
            partyId = request.getParameter("partyId");
        }
        String userLoginId = request.getParameter("userLoginId");
        if (partyId != null || userLoginId != null) {
            if (UtilValidate.isEmpty(partyId) && UtilValidate.isNotEmpty(userLoginId)) {
                GenericValue thisUserLogin = null;
                try {
                    thisUserLogin = delegator.findByPrimaryKey("UserLogin", UtilMisc.toMap("userLoginId", userLoginId));
                } catch (GenericEntityException gee) {
                    //
                }
                if (thisUserLogin != null) {
                    partyId = thisUserLogin.getString("partyId");
                } else {
                    partyId = userLoginId;
                }
            }
            if (UtilValidate.isNotEmpty(partyId)) {
                GenericValue thisParty = null;
                try {
                    thisParty = delegator.findByPrimaryKey("Party", UtilMisc.toMap("partyId", partyId));
                } catch (GenericEntityException gee) {
                    //
                }
                if (thisParty == null) {
                    request.setAttribute("_ERROR_MESSAGE_", UtilProperties.getMessage(resource_error,"OrderCouldNotLocateTheSelectedParty", locale));
                    return "error";
                } else {
                    cart.setOrderPartyId(partyId);
                    if ("PURCHASE_ORDER".equals(cart.getOrderType())) {
                        cart.setBillFromVendorPartyId(partyId);
                    }
                }
            } else if (partyId != null && partyId.length() == 0) {
                cart.setOrderPartyId("_NA_");
                partyId = null;
            }
        } else {
            partyId = cart.getPartyId();
            if (partyId != null && partyId.equals("_NA_")) partyId = null;
        }

        return "success";
    }

    /** Route order entry **/
    public static String routeOrderEntry(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession();

        // if the order mode is not set in the attributes, then order entry has not been initialized
        if (session.getAttribute("orderMode") == null) {
            return "init";
        }

        // if the request is coming from the init page, then orderMode will be in the request parameters
        if (request.getParameter("orderMode") != null) {
            return "agreements"; // next page after init is always agreements
        }

        // orderMode is set and there is an order in progress, so go straight to the cart
        return "cart";
    }

    public static String doManualPromotions(HttpServletRequest request, HttpServletResponse response) {
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        ShoppingCart cart = getCartObject(request);
        List manualPromotions = new LinkedList();

        // iterate through the context and find all keys that start with "productPromoId_"
        Map context = UtilHttp.getParameterMap(request);
        String keyPrefix = "productPromoId_";
        for (int i = 1; i <= 50; i++) {
            String productPromoId = (String)context.get(keyPrefix + i);
            if (UtilValidate.isNotEmpty(productPromoId)) {
                try {
                    GenericValue promo = delegator.findByPrimaryKey("ProductPromo", UtilMisc.toMap("productPromoId", productPromoId));
                    if (promo != null) {
                        manualPromotions.add(promo);
                    }
                } catch (GenericEntityException gee) {
                    request.setAttribute("_ERROR_MESSAGE_", gee.getMessage());
                    return "error";
                }
            } else {
                break;
            }
        }
        ProductPromoWorker.doPromotions(cart, manualPromotions, dispatcher);
        return "success";
    }


    public static String bulkAddProducts(HttpServletRequest request, HttpServletResponse response) {
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        ShoppingCart cart = ShoppingCartEvents.getCartObject(request);
        ShoppingCartHelper cartHelper = new ShoppingCartHelper(delegator, dispatcher, cart);
        String controlDirective = null;
        Map result = null;
        String productId = null;
        String productCategoryId = null;
        String quantityStr = null;
        String itemDesiredDeliveryDateStr = null;
        BigDecimal quantity = BigDecimal.ZERO;
        String catalogId = CatalogWorker.getCurrentCatalogId(request);
        String itemType = null;
        String itemDescription = "";

        // Get the parameters as a MAP, remove the productId and quantity params.
        Map paramMap = UtilHttp.getParameterMap(request);

        String itemGroupNumber = request.getParameter("itemGroupNumber");

        // Get shoppingList info if passed.  I think there can only be one shoppingList per request
        String shoppingListId = request.getParameter("shoppingListId");
        String shoppingListItemSeqId = request.getParameter("shoppingListItemSeqId");

        // The number of multi form rows is retrieved
        int rowCount = UtilHttp.getMultiFormRowCount(paramMap);
        if (rowCount < 1) {
            Debug.logWarning("No rows to process, as rowCount = " + rowCount, module);
        } else {
            for (int i = 0; i < rowCount; i++) {
                controlDirective = null;                // re-initialize each time
                String thisSuffix = UtilHttp.MULTI_ROW_DELIMITER + i;        // current suffix after each field id

                // get the productId
                if (paramMap.containsKey("productId" + thisSuffix)) {
                    productId = (String) paramMap.remove("productId" + thisSuffix);
                }

                if (paramMap.containsKey("quantity" + thisSuffix)) {
                    quantityStr = (String) paramMap.remove("quantity" + thisSuffix);
                }
                if ((quantityStr == null) || (quantityStr.equals(""))) {    // otherwise, every empty value causes an exception and makes the log ugly
                    quantityStr = "0";  // default quantity is 0, so without a quantity input, this field will not be added
                }

                // parse the quantity
                try {
                    quantity = new BigDecimal(quantityStr);
                } catch (Exception e) {
                    Debug.logWarning(e, "Problems parsing quantity string: " + quantityStr, module);
                    quantity = BigDecimal.ZERO;
                }

                // get the selected amount
                String selectedAmountStr = null;
                if (paramMap.containsKey("amount" + thisSuffix)) {
                    selectedAmountStr = (String) paramMap.remove("amount" + thisSuffix);
                }

                // parse the amount
                BigDecimal amount = null;
                if (UtilValidate.isNotEmpty(selectedAmountStr)) {
                    try {
                        amount = new BigDecimal(selectedAmountStr);
                    } catch (Exception e) {
                        Debug.logWarning(e, "Problem parsing amount string: " + selectedAmountStr, module);
                        amount = null;
                    }
                } else {
                    amount = BigDecimal.ZERO;
                }

                if (paramMap.containsKey("itemDesiredDeliveryDate" + thisSuffix)) {
                    itemDesiredDeliveryDateStr = (String) paramMap.remove("itemDesiredDeliveryDate" + thisSuffix);
                }
                // get the item type
                if (paramMap.containsKey("itemType" + thisSuffix)) {
                    itemType = (String) paramMap.remove("itemType" + thisSuffix);
                }

                if (paramMap.containsKey("itemDescription" + thisSuffix)) {
                    itemDescription = (String) paramMap.remove("itemDescription" + thisSuffix);
                }

                Map itemAttributes = UtilMisc.toMap("itemDesiredDeliveryDate", itemDesiredDeliveryDateStr);

                if (quantity.compareTo(BigDecimal.ZERO) > 0) {
                    Debug.logInfo("Attempting to add to cart with productId = " + productId + ", categoryId = " + productCategoryId +
                            ", quantity = " + quantity + ", itemType = " + itemType + " and itemDescription = " + itemDescription, module);
                    result = cartHelper.addToCart(catalogId, shoppingListId, shoppingListItemSeqId, productId,
                                                  productCategoryId, itemType, itemDescription, null,
                                                  amount, quantity, null, null, null, null, null, null,
                                                  itemGroupNumber, itemAttributes,null);
                    // no values for price and paramMap (a context for adding attributes)
                    controlDirective = processResult(result, request);
                    if (controlDirective.equals(ERROR)) {    // if the add to cart failed, then get out of this loop right away
                        return "error";
                    }
                }
            }
        }

        // Determine where to send the browser
        return cart.viewCartOnAdd() ? "viewcart" : "success";
    }

    // request method for setting the currency, agreement, OrderId and shipment dates at once
    public static String setOrderCurrencyAgreementShipDates(HttpServletRequest request, HttpServletResponse response) {
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        ShoppingCart cart = getCartObject(request);
        ShoppingCartHelper cartHelper = new ShoppingCartHelper(delegator, dispatcher, cart);

        String agreementId = request.getParameter("agreementId");
        String currencyUomId = request.getParameter("currencyUomId");
        String workEffortId = request.getParameter("workEffortId");
        String shipBeforeDateStr = request.getParameter("shipBeforeDate");
        String shipAfterDateStr = request.getParameter("shipAfterDate");
        String cancelBackOrderDateStr = request.getParameter("cancelBackOrderDate");
        String orderId = request.getParameter("orderId");
        String orderName = request.getParameter("orderName");
        String correspondingPoId = request.getParameter("correspondingPoId");
        Locale locale = UtilHttp.getLocale(request);
        Map result = null;

        // set the agreement if specified otherwise set the currency
        if (UtilValidate.isNotEmpty(agreementId)) {
            result = cartHelper.selectAgreement(agreementId);
        } else if (UtilValidate.isNotEmpty(currencyUomId)) {
            result = cartHelper.setCurrency(currencyUomId);
        }
        if (ServiceUtil.isError(result)) {
            request.setAttribute("_ERROR_MESSAGE_", ServiceUtil.getErrorMessage(result));
            return "error";
        }

        // set the work effort id
        cart.setWorkEffortId(workEffortId);

        // set the order id if given
        if (UtilValidate.isNotEmpty(orderId)) {
            GenericValue thisOrder = null;
            try {
                thisOrder = delegator.findOne("OrderHeader", UtilMisc.toMap("orderId", orderId), false);
            } catch (GenericEntityException e) {
                Debug.logError(e.getMessage(), module);
            }
            if (thisOrder == null) {
                cart.setOrderId(orderId);
            } else {
                request.setAttribute("_ERROR_MESSAGE_", UtilProperties.getMessage(resource_error,"OrderIdAlreadyExistsPleaseChooseAnother", locale));
                return "error";
            }
        }

        // set the order name
        cart.setOrderName(orderName);

        // set the corresponding purchase order id
        cart.setPoNumber(correspondingPoId);

        // set the default ship before and after dates if supplied
        try {
            if (UtilValidate.isNotEmpty(shipBeforeDateStr)) {
                if (shipBeforeDateStr.length() == 10) shipBeforeDateStr += " 00:00:00.000";
                cart.setDefaultShipBeforeDate(java.sql.Timestamp.valueOf(shipBeforeDateStr));
            }
            if (UtilValidate.isNotEmpty(shipAfterDateStr)) {
                if (shipAfterDateStr.length() == 10) shipAfterDateStr += " 00:00:00.000";
                cart.setDefaultShipAfterDate(java.sql.Timestamp.valueOf(shipAfterDateStr));
            }
            if (UtilValidate.isNotEmpty(cancelBackOrderDateStr)) {
                if (cancelBackOrderDateStr.length() == 10) cancelBackOrderDateStr += " 00:00:00.000";
                cart.setCancelBackOrderDate(java.sql.Timestamp.valueOf(cancelBackOrderDateStr));
            }
        } catch (IllegalArgumentException e) {
            request.setAttribute("_ERROR_MESSAGE_", e.getMessage());
            return "error";
        }
        return "success";
    }

    public static String getConfigDetailsEvent(HttpServletRequest request, HttpServletResponse response) {

        Delegator delegator = (Delegator) request.getAttribute("delegator");
        String productId = request.getParameter("product_id");
        String currencyUomId = ShoppingCartEvents.getCartObject(request).getCurrency();
        ProductConfigWrapper configWrapper = ProductConfigWorker.getProductConfigWrapper(productId, currencyUomId, request);
        if (configWrapper == null) {
            Debug.logWarning("configWrapper is null", module);
            request.setAttribute("_ERROR_MESSAGE_", "configWrapper is null");
            return "error";
        }
        ProductConfigWorker.fillProductConfigWrapper(configWrapper, request);
        if (configWrapper.isCompleted()) {
            ProductConfigWorker.storeProductConfigWrapper(configWrapper, delegator);
            request.setAttribute("configId", configWrapper.getConfigId());
        }

        request.setAttribute("totalPrice", org.ofbiz.base.util.UtilFormatOut.formatCurrency(configWrapper.getTotalPrice(), currencyUomId, UtilHttp.getLocale(request)));
        return "success";
    }

    public static String bulkAddProductsInApprovedOrder(HttpServletRequest request, HttpServletResponse response) {
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        Locale locale = UtilHttp.getLocale(request);
        String productId = null;
        String productCategoryId = null;
        String quantityStr = null;
        String itemDesiredDeliveryDateStr = null;
        BigDecimal quantity = BigDecimal.ZERO;
        String itemType = null;
        String itemDescription = "";
        String orderId = null;
        String shipGroupSeqId = null;

        Map paramMap = UtilHttp.getParameterMap(request);
        String itemGroupNumber = request.getParameter("itemGroupNumber");
        int rowCount = UtilHttp.getMultiFormRowCount(paramMap);
        if (rowCount < 1) {
            Debug.logWarning("No rows to process, as rowCount = " + rowCount, module);
        } else {
            for (int i = 0; i < rowCount; i++) {
                String thisSuffix = UtilHttp.MULTI_ROW_DELIMITER + i;
                if (paramMap.containsKey("productId" + thisSuffix)) {
                    productId = (String) paramMap.remove("productId" + thisSuffix);
                }
                if (paramMap.containsKey("quantity" + thisSuffix)) {
                    quantityStr = (String) paramMap.remove("quantity" + thisSuffix);
                }
                if ((quantityStr == null) || (quantityStr.equals(""))) {
                    quantityStr = "0";
                }
                try {
                    quantity = new BigDecimal(quantityStr);
                } catch (Exception e) {
                    Debug.logWarning(e, "Problems parsing quantity string: " + quantityStr, module);
                    quantity = BigDecimal.ZERO;
                }
                String selectedAmountStr = null;
                if (paramMap.containsKey("amount" + thisSuffix)) {
                    selectedAmountStr = (String) paramMap.remove("amount" + thisSuffix);
                }
                BigDecimal amount = null;
                if (UtilValidate.isNotEmpty(selectedAmountStr)) {
                    try {
                        amount = new BigDecimal(selectedAmountStr);
                    } catch (Exception e) {
                        Debug.logWarning(e, "Problem parsing amount string: " + selectedAmountStr, module);
                        amount = null;
                    }
                } else {
                    amount = BigDecimal.ZERO;
                }
                if (paramMap.containsKey("itemDesiredDeliveryDate" + thisSuffix)) {
                    itemDesiredDeliveryDateStr = (String) paramMap.remove("itemDesiredDeliveryDate" + thisSuffix);
                }
                Timestamp itemDesiredDeliveryDate = null;
                if (UtilValidate.isNotEmpty(itemDesiredDeliveryDateStr)) {
                    try {
                        itemDesiredDeliveryDate = Timestamp.valueOf(itemDesiredDeliveryDateStr);
                    } catch (Exception e) {
                        Debug.logWarning(e,"Problems parsing Reservation start string: " + itemDesiredDeliveryDateStr, module);
                        itemDesiredDeliveryDate = null;
                        request.setAttribute("_ERROR_MESSAGE_", UtilProperties.getMessage(resource_error,"shoppingCartEvents.problem_parsing_item_desiredDeliveryDate_string", locale));
                    }
                }
                if (paramMap.containsKey("itemType" + thisSuffix)) {
                    itemType = (String) paramMap.remove("itemType" + thisSuffix);
                }
                if (paramMap.containsKey("itemDescription" + thisSuffix)) {
                    itemDescription = (String) paramMap.remove("itemDescription" + thisSuffix);
                }
                if (paramMap.containsKey("orderId" + thisSuffix)) {
                    orderId = (String) paramMap.remove("orderId" + thisSuffix);
                }
                if (paramMap.containsKey("shipGroupSeqId" + thisSuffix)) {
                    shipGroupSeqId = (String) paramMap.remove("shipGroupSeqId" + thisSuffix);
                }
                if (quantity.compareTo(BigDecimal.ZERO) > 0) {
                    Debug.logInfo("Attempting to add to cart with productId = " + productId + ", categoryId = " + productCategoryId +
                            ", quantity = " + quantity + ", itemType = " + itemType + " and itemDescription = " + itemDescription, module);
                    HttpSession session = request.getSession();
                    GenericValue userLogin = (GenericValue) session.getAttribute("userLogin");
                    Map appendOrderItemMap = FastMap.newInstance();
                    appendOrderItemMap.put("productId", productId);
                    appendOrderItemMap.put("quantity", quantity);
                    appendOrderItemMap.put("orderId", orderId);
                    appendOrderItemMap.put("userLogin", userLogin);
                    appendOrderItemMap.put("amount", amount);
                    appendOrderItemMap.put("itemDesiredDeliveryDate", itemDesiredDeliveryDate);
                    appendOrderItemMap.put("shipGroupSeqId", shipGroupSeqId);
                    try {
                        Map result = dispatcher.runSync("appendOrderItem", appendOrderItemMap);
                        request.setAttribute("shoppingCart", (ShoppingCart) result.get("shoppingCart"));
                        ShoppingCartEvents.destroyCart(request, response);
                    } catch (GenericServiceException e) {
                        Debug.logError(e, "Failed to execute service appendOrderItem", module);
                        request.setAttribute("_ERROR_MESSAGE_", e.getMessage());
                        return "error";
                    }
                }
            }
        }
        request.setAttribute("orderId", orderId);
        return  "success";
    }
}
