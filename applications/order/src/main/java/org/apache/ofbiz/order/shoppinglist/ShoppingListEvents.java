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
package org.apache.ofbiz.order.shoppinglist;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilHttp;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.order.shoppingcart.CartItemModifyException;
import org.apache.ofbiz.order.shoppingcart.ItemNotFoundException;
import org.apache.ofbiz.order.shoppingcart.ShoppingCart;
import org.apache.ofbiz.order.shoppingcart.ShoppingCartEvents;
import org.apache.ofbiz.order.shoppingcart.ShoppingCartItem;
import org.apache.ofbiz.product.catalog.CatalogWorker;
import org.apache.ofbiz.product.config.ProductConfigWorker;
import org.apache.ofbiz.product.config.ProductConfigWrapper;
import org.apache.ofbiz.product.store.ProductStoreWorker;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.service.ServiceUtil;
import org.apache.ofbiz.webapp.website.WebSiteWorker;

/**
 * Shopping cart events.
 */
public class ShoppingListEvents {

    public static final String module = ShoppingListEvents.class.getName();
    public static final String resource_error = "OrderErrorUiLabels";
    public static final String PERSISTANT_LIST_NAME = "auto-save";

    public static String addBulkFromCart(HttpServletRequest request, HttpServletResponse response) {
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        ShoppingCart cart = ShoppingCartEvents.getCartObject(request);
        GenericValue userLogin = (GenericValue) request.getSession().getAttribute("userLogin");

        String shoppingListId = request.getParameter("shoppingListId");
        String shoppingListTypeId = request.getParameter("shoppingListTypeId");
        String selectedCartItems[] = request.getParameterValues("selectedItem");
        if (UtilValidate.isEmpty(selectedCartItems)) {
            selectedCartItems = makeCartItemsArray(cart);
        }

        try {
            shoppingListId = addBulkFromCart(delegator, dispatcher, cart, userLogin, shoppingListId, shoppingListTypeId, selectedCartItems, true, true);
        } catch (IllegalArgumentException e) {
            request.setAttribute("_ERROR_MESSAGE_", e.getMessage());
            return "error";
        }

        request.setAttribute("shoppingListId", shoppingListId);
        return "success";
    }

    public static String addBulkFromCart(Delegator delegator, LocalDispatcher dispatcher, ShoppingCart cart, GenericValue userLogin, String shoppingListId, String shoppingListTypeId, String[] items, boolean allowPromo, boolean append) throws IllegalArgumentException {
        String errMsg = null;

        if (items == null || items.length == 0) {
            errMsg = UtilProperties.getMessage(resource_error, "shoppinglistevents.select_items_to_add_to_list", cart.getLocale());
            throw new IllegalArgumentException(errMsg);
        }

        if (UtilValidate.isEmpty(shoppingListId)) {
            // create a new shopping list
            Map<String, Object> newListResult = null;
            try {
                newListResult = dispatcher.runSync("createShoppingList", UtilMisc.<String, Object>toMap("userLogin", userLogin, 
                        "productStoreId", cart.getProductStoreId(), "partyId", cart.getOrderPartyId(), 
                        "shoppingListTypeId", shoppingListTypeId, "currencyUom", cart.getCurrency()),
                        90, true);
            } catch (GenericServiceException e) {
                Debug.logError(e, "Problems creating new ShoppingList", module);
                errMsg = UtilProperties.getMessage(resource_error,"shoppinglistevents.cannot_create_new_shopping_list", cart.getLocale());
                throw new IllegalArgumentException(errMsg);
            }

            // check for errors
            if (ServiceUtil.isError(newListResult)) {
                throw new IllegalArgumentException(ServiceUtil.getErrorMessage(newListResult));
            }

            // get the new list id
            if (newListResult != null) {
                shoppingListId = (String) newListResult.get("shoppingListId");
            }

            // if no list was created throw an error
            if (shoppingListId == null || shoppingListId.equals("")) {
                errMsg = UtilProperties.getMessage(resource_error,"shoppinglistevents.shoppingListId_is_required_parameter", cart.getLocale());
                throw new IllegalArgumentException(errMsg);
            }
        } else if (!append) {
            try {
                clearListInfo(delegator, shoppingListId);
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
                throw new IllegalArgumentException("Could not clear current shopping list: " + e.toString());
            }
        }

        for (String item2 : items) {
            Integer cartIdInt = null;
            try {
                cartIdInt = Integer.valueOf(item2);
            } catch (Exception e) {
                Debug.logWarning(e, UtilProperties.getMessage(resource_error,"OrderIllegalCharacterInSelectedItemField", cart.getLocale()), module);
            }
            if (cartIdInt != null) {
                ShoppingCartItem item = cart.findCartItem(cartIdInt.intValue());
                if (allowPromo || !item.getIsPromo()) {
                    Debug.logInfo("Adding cart item to shopping list [" + shoppingListId + "], allowPromo=" + allowPromo + ", item.getIsPromo()=" + item.getIsPromo() + ", item.getProductId()=" + item.getProductId() + ", item.getQuantity()=" + item.getQuantity(), module);
                    Map<String, Object> serviceResult = null;
                    try {
                        Map<String, Object> ctx = UtilMisc.<String, Object>toMap("userLogin", userLogin, "shoppingListId", shoppingListId, "productId", item.getProductId(), "quantity", item.getQuantity());
                        ctx.put("reservStart", item.getReservStart());
                        ctx.put("reservLength", item.getReservLength());
                        ctx.put("reservPersons", item.getReservPersons());
                        if (item.getConfigWrapper() != null) {
                            ctx.put("configId", item.getConfigWrapper().getConfigId());
                        }
                        serviceResult = dispatcher.runSync("createShoppingListItem", ctx);
                    } catch (GenericServiceException e) {
                        Debug.logError(e, "Problems creating ShoppingList item entity", module);
                        errMsg = UtilProperties.getMessage(resource_error,"shoppinglistevents.error_adding_item_to_shopping_list", cart.getLocale());
                        throw new IllegalArgumentException(errMsg);
                    }

                    // check for errors
                    if (ServiceUtil.isError(serviceResult)) {
                        throw new IllegalArgumentException(ServiceUtil.getErrorMessage(serviceResult));
                    }
                }
            }
        }

        // return the shoppinglist id
        return shoppingListId;
    }

    public static String addListToCart(HttpServletRequest request, HttpServletResponse response) {
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        ShoppingCart cart = ShoppingCartEvents.getCartObject(request);

        String shoppingListId = request.getParameter("shoppingListId");
        String includeChild = request.getParameter("includeChild");
        String prodCatalogId =  CatalogWorker.getCurrentCatalogId(request);

        try {
            addListToCart(delegator, dispatcher, cart, prodCatalogId, shoppingListId, (includeChild != null), true, true);
        } catch (IllegalArgumentException e) {
            request.setAttribute("_ERROR_MESSAGE_", e.getMessage());
            return "error";
        }


        return "success";
    }

    public static String addListToCart(Delegator delegator, LocalDispatcher dispatcher, ShoppingCart cart, String prodCatalogId, String shoppingListId, boolean includeChild, boolean setAsListItem, boolean append) throws java.lang.IllegalArgumentException {
        String errMsg = null;

        // no list; no add
        if (shoppingListId == null) {
            errMsg = UtilProperties.getMessage(resource_error,"shoppinglistevents.choose_shopping_list", cart.getLocale());
            throw new IllegalArgumentException(errMsg);
        }

        // get the shopping list
        GenericValue shoppingList = null;
        List<GenericValue> shoppingListItems = null;
        try {
            shoppingList = EntityQuery.use(delegator).from("ShoppingList").where("shoppingListId", shoppingListId).queryOne();
            if (shoppingList == null) {
                errMsg = UtilProperties.getMessage(resource_error,"shoppinglistevents.error_getting_shopping_list_and_items", cart.getLocale());
                throw new IllegalArgumentException(errMsg);
            }

            shoppingListItems = shoppingList.getRelated("ShoppingListItem", null, null, false);
            if (shoppingListItems == null) {
                shoppingListItems = new LinkedList<>();
            }

            // include all items of child lists if flagged to do so
            if (includeChild) {
                List<GenericValue> childShoppingLists = shoppingList.getRelated("ChildShoppingList", null, null, false);
                for (GenericValue v : childShoppingLists) {
                    List<GenericValue> items = v.getRelated("ShoppingListItem", null, null, false);
                    shoppingListItems.addAll(items);
                }
            }

        } catch (GenericEntityException e) {
            Debug.logError(e, "Problems getting ShoppingList and ShoppingListItem records", module);
            errMsg = UtilProperties.getMessage(resource_error,"shoppinglistevents.error_getting_shopping_list_and_items", cart.getLocale());
            throw new IllegalArgumentException(errMsg);
        }

        // no items; not an error; just mention that nothing was added
        if (UtilValidate.isEmpty(shoppingListItems)) {
            errMsg = UtilProperties.getMessage(resource_error,"shoppinglistevents.no_items_added", cart.getLocale());
            return errMsg;
        }

        // check if we are to clear the cart first
        if (!append) {
            cart.clear();
            // Prevent the system from creating a new shopping list every time the cart is restored for anonymous user.
            cart.setAutoSaveListId(shoppingListId);
        }

        // get the survey info for all the items
        Map<String, List<String>> shoppingListSurveyInfo = getItemSurveyInfos(shoppingListItems);

        // add the items
        StringBuilder eventMessage = new StringBuilder();
        for (GenericValue shoppingListItem : shoppingListItems) {
            String productId = shoppingListItem.getString("productId");
            BigDecimal quantity = shoppingListItem.getBigDecimal("quantity");
            Timestamp reservStart = shoppingListItem.getTimestamp("reservStart");
            BigDecimal reservLength = shoppingListItem.getBigDecimal("reservLength");
            BigDecimal reservPersons = shoppingListItem.getBigDecimal("reservPersons");
            String configId = shoppingListItem.getString("configId");
            try {
                String listId = shoppingListItem.getString("shoppingListId");
                String itemId = shoppingListItem.getString("shoppingListItemSeqId");

                Map<String, Object> attributes = new HashMap<>();
                // list items are noted in the shopping cart
                if (setAsListItem) {
                    attributes.put("shoppingListId", listId);
                    attributes.put("shoppingListItemSeqId", itemId);
                }

                // check if we have existing survey responses to append
                if (shoppingListSurveyInfo.containsKey(listId + "." + itemId) && UtilValidate.isNotEmpty(shoppingListSurveyInfo.get(listId + "." + itemId))) {
                    attributes.put("surveyResponses", shoppingListSurveyInfo.get(listId + "." + itemId));
                }

                ProductConfigWrapper configWrapper = null;
                if (UtilValidate.isNotEmpty(configId)) {
                    configWrapper = ProductConfigWorker.loadProductConfigWrapper(delegator, dispatcher, configId, productId, cart.getProductStoreId(), prodCatalogId, cart.getWebSiteId(), cart.getCurrency(), cart.getLocale(), cart.getAutoUserLogin());
                }
                // TODO: add code to check for survey response requirement

                // i cannot get the addOrDecrease function to accept a null reservStart field: i get a null pointer exception a null constant works....
                if (reservStart == null) {
                       cart.addOrIncreaseItem(productId, null, quantity, null, null, null, null, null, null, attributes, prodCatalogId, configWrapper, null, null, null, dispatcher);
                } else {
                    cart.addOrIncreaseItem(productId, null, quantity, reservStart, reservLength, reservPersons, null, null, null, null, null, attributes, prodCatalogId, configWrapper, null, null, null, dispatcher);
                }
                Map<String, Object> messageMap = UtilMisc.<String, Object>toMap("productId", productId);
                errMsg = UtilProperties.getMessage(resource_error,"shoppinglistevents.added_product_to_cart", messageMap, cart.getLocale());
                eventMessage.append(errMsg).append("\n");
            } catch (CartItemModifyException e) {
                Debug.logWarning(e, UtilProperties.getMessage(resource_error,"OrderProblemsAddingItemFromListToCart", cart.getLocale()));
                Map<String, Object> messageMap = UtilMisc.<String, Object>toMap("productId", productId);
                errMsg = UtilProperties.getMessage(resource_error,"shoppinglistevents.problem_adding_product_to_cart", messageMap, cart.getLocale());
                eventMessage.append(errMsg).append("\n");
            } catch (ItemNotFoundException e) {
                Debug.logWarning(e, UtilProperties.getMessage(resource_error,"OrderProductNotFound", cart.getLocale()));
                Map<String, Object> messageMap = UtilMisc.<String, Object>toMap("productId", productId);
                errMsg = UtilProperties.getMessage(resource_error,"shoppinglistevents.problem_adding_product_to_cart", messageMap, cart.getLocale());
                eventMessage.append(errMsg).append("\n");
            }
        }

        if (eventMessage.length() > 0) {
            return eventMessage.toString();
        }

        // all done
        return ""; // no message to return; will simply reply as success
    }

    public static String replaceShoppingListItem(HttpServletRequest request, HttpServletResponse response) {
        String quantityStr = request.getParameter("quantity");

        // just call the updateShoppingListItem service
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        GenericValue userLogin = (GenericValue) request.getSession().getAttribute("userLogin");
        Locale locale = UtilHttp.getLocale(request);

        BigDecimal quantity = null;
        try {
            quantity = new BigDecimal(quantityStr);
        } catch (NumberFormatException e) {
            // do nothing, just won't pass to service if it is null
            Debug.logError(e, module);
        }

        Map<String, Object> serviceInMap = new HashMap<String, Object>();
        serviceInMap.put("shoppingListId", request.getParameter("shoppingListId"));
        serviceInMap.put("shoppingListItemSeqId", request.getParameter("shoppingListItemSeqId"));
        serviceInMap.put("productId", request.getParameter("add_product_id"));
        serviceInMap.put("userLogin", userLogin);
        if (quantity != null) serviceInMap.put("quantity", quantity);
        Map<String, Object> result = null;
        try {
            result = dispatcher.runSync("updateShoppingListItem", serviceInMap);
        } catch (GenericServiceException e) {
            String errMsg = UtilProperties.getMessage(resource_error,"shoppingListEvents.error_calling_update", locale) + ": "  + e.toString();
            request.setAttribute("_ERROR_MESSAGE_", errMsg);
            String errorMsg = "Error calling the updateShoppingListItem in handleShoppingListItemVariant: " + e.toString();
            Debug.logError(e, errorMsg, module);
            return "error";
        }

        ServiceUtil.getMessages(request, result, "", "", "", "", "", "", "");
        if ("error".equals(result.get(ModelService.RESPONSE_MESSAGE))) {
            return "error";
        }
        return "success";
    }

    /**
     * Finds or creates a specialized (auto-save) shopping list used to record shopping bag contents between user visits.
     */
    public static String getAutoSaveListId(Delegator delegator, LocalDispatcher dispatcher, String partyId, GenericValue userLogin, String productStoreId) throws GenericEntityException, GenericServiceException {
        if (partyId == null && userLogin != null) {
            partyId = userLogin.getString("partyId");
        }

        String autoSaveListId = null;
        GenericValue list = null;
        // TODO: add sorting, just in case there are multiple...
        if (partyId != null) {
            Map<String, Object> findMap = UtilMisc.<String, Object>toMap("partyId", partyId, "productStoreId", productStoreId, "shoppingListTypeId", "SLT_SPEC_PURP", "listName", PERSISTANT_LIST_NAME);
            List<GenericValue> existingLists = EntityQuery.use(delegator).from("ShoppingList").where(findMap).queryList();
            Debug.logInfo("Finding existing auto-save shopping list with:  \nfindMap: " + findMap + "\nlists: " + existingLists, module);

            if (UtilValidate.isNotEmpty(existingLists)) {
                list = EntityUtil.getFirst(existingLists);
                autoSaveListId = list.getString("shoppingListId");
            }
        }
        if (list == null && dispatcher != null) {
            Map<String, Object> listFields = UtilMisc.<String, Object>toMap("userLogin", userLogin, "productStoreId", productStoreId, "shoppingListTypeId", "SLT_SPEC_PURP", "listName", PERSISTANT_LIST_NAME);
            Map<String, Object> newListResult = dispatcher.runSync("createShoppingList", listFields, 90, true);

            if (newListResult != null) {
                autoSaveListId = (String) newListResult.get("shoppingListId");
            }
        }

        return autoSaveListId;
    }

    /**
     * Fills the specialized shopping list with the current shopping cart if one exists (if not leaves it alone)
     */
    public static void fillAutoSaveList(ShoppingCart cart, LocalDispatcher dispatcher) throws GeneralException {
        if (cart != null && dispatcher != null) {
            GenericValue userLogin = ShoppingListEvents.getCartUserLogin(cart);
            Delegator delegator = cart.getDelegator();
            String autoSaveListId = cart.getAutoSaveListId();
            if (autoSaveListId == null) {
                autoSaveListId = getAutoSaveListId(delegator, dispatcher, null, userLogin, cart.getProductStoreId());
                cart.setAutoSaveListId(autoSaveListId);
            }
            GenericValue shoppingList = EntityQuery.use(delegator).from("ShoppingList").where("shoppingListId", autoSaveListId).queryOne();
            Integer currentListSize = 0;
            if (UtilValidate.isNotEmpty(shoppingList)) {
                List<GenericValue> shoppingListItems = shoppingList.getRelated("ShoppingListItem", null, null, false);
                if (UtilValidate.isNotEmpty(shoppingListItems)) {
                    currentListSize = shoppingListItems.size();
                }
            }

            try {
                String[] itemsArray = makeCartItemsArray(cart);
                if (itemsArray.length != 0) {
                    addBulkFromCart(delegator, dispatcher, cart, userLogin, autoSaveListId, null, itemsArray, false, false);
                } else if (currentListSize != 0) {
                    clearListInfo(delegator, autoSaveListId);
                }
            } catch (IllegalArgumentException e) {
                throw new GeneralException(e.getMessage(), e);
            }
        }
    }

    /**
     * Saves the shopping cart to the specialized (auto-save) shopping list
     */
    public static String saveCartToAutoSaveList(HttpServletRequest request, HttpServletResponse response) {
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        ShoppingCart cart = ShoppingCartEvents.getCartObject(request);

        try {
            fillAutoSaveList(cart, dispatcher);
        } catch (GeneralException e) {
            Debug.logError(e, "Error saving the cart to the auto-save list: " + e.toString(), module);
        }

        return "success";
    }

    /**
     * Restores the specialized (auto-save) shopping list back into the shopping cart
     */
    public static String restoreAutoSaveList(HttpServletRequest request, HttpServletResponse response) {
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        GenericValue productStore = ProductStoreWorker.getProductStore(request);
        HttpSession session = request.getSession();
        ShoppingCart cart = ShoppingCartEvents.getCartObject(request);

        // locate the user's identity
        GenericValue userLogin = (GenericValue) session.getAttribute("userLogin");
        if (userLogin == null) {
            userLogin = (GenericValue) session.getAttribute("autoUserLogin");
        }
        
        if (!ProductStoreWorker.autoSaveCart(productStore) || userLogin == null) {
            // if auto-save is disabled or there is still no userLogin just return here
            return "success";
        }

        // safety check for missing required parameter.
        if (cart.getWebSiteId() == null) {
            cart.setWebSiteId(WebSiteWorker.getWebSiteId(request));
        }

        // find the list ID
        String autoSaveListId = cart.getAutoSaveListId();
        if (autoSaveListId == null) {
            try {
                autoSaveListId = getAutoSaveListId(delegator, dispatcher, null, userLogin, cart.getProductStoreId());
            } catch (GeneralException e) {
                Debug.logError(e, module);
            }
            cart.setAutoSaveListId(autoSaveListId);
        } else if (userLogin != null) {
            String existingAutoSaveListId = null;
            try {
                existingAutoSaveListId = getAutoSaveListId(delegator, dispatcher, null, userLogin, cart.getProductStoreId());
            } catch (GeneralException e) {
                Debug.logError(e, module);
            }
            if (existingAutoSaveListId != null) {
                if (!existingAutoSaveListId.equals(autoSaveListId)) {
                    // Replace with existing shopping list
                    cart.setAutoSaveListId(existingAutoSaveListId);
                    autoSaveListId = existingAutoSaveListId;
                    cart.setLastListRestore(null);
                } else {
                    // CASE: User first login and logout and then re-login again. This condition does not require a restore at all
                    // because at this point items in the cart and the items in the shopping list are same so just return.
                    return "success";
                }
            }
        }

        // check to see if we are okay to load this list
        java.sql.Timestamp lastLoad = cart.getLastListRestore();
        boolean okayToLoad = autoSaveListId == null ? false : (lastLoad == null ? true : false);
        if (!okayToLoad && lastLoad != null) {
            GenericValue shoppingList = null;
            try {
                shoppingList = EntityQuery.use(delegator).from("ShoppingList").where("shoppingListId", autoSaveListId).queryOne();
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
            }
            if (shoppingList != null) {
                java.sql.Timestamp lastModified = shoppingList.getTimestamp("lastAdminModified");
                if (lastModified != null) {
                    if (lastModified.after(lastLoad)) {
                        okayToLoad = true;
                    }
                    if (cart.size() == 0 && lastModified.after(cart.getCartCreatedTime())) {
                        okayToLoad = true;
                    }
                }
            }
        }

        // load (restore) the list of we have determined it is okay to load
        if (okayToLoad) {
            String prodCatalogId = CatalogWorker.getCurrentCatalogId(request);
            try {
                addListToCart(delegator, dispatcher, cart, prodCatalogId, autoSaveListId, false, false, userLogin != null ? true : false);
                cart.setLastListRestore(UtilDateTime.nowTimestamp());
            } catch (IllegalArgumentException e) {
                Debug.logError(e, module);
            }
        }

        return "success";
    }

    /**
     * Remove all items from the given list.
     */
    public static int clearListInfo(Delegator delegator, String shoppingListId) throws GenericEntityException {
        // remove the survey responses first
        delegator.removeByAnd("ShoppingListItemSurvey", UtilMisc.toMap("shoppingListId", shoppingListId));

        // next remove the items
        return delegator.removeByAnd("ShoppingListItem", UtilMisc.toMap("shoppingListId", shoppingListId));
    }

    /**
     * Creates records for survey responses on survey items
     */
    public static int makeListItemSurveyResp(Delegator delegator, GenericValue item, List<String> surveyResps) throws GenericEntityException {
        if (UtilValidate.isNotEmpty(surveyResps)) {
            int count = 0;
            for (String responseId : surveyResps) {
                GenericValue listResp = delegator.makeValue("ShoppingListItemSurvey");
                listResp.set("shoppingListId", item.getString("shoppingListId"));
                listResp.set("shoppingListItemSeqId", item.getString("shoppingListItemSeqId"));
                listResp.set("surveyResponseId", responseId);
                delegator.create(listResp);
                count++;
            }
            return count;
        }
        return -1;
    }

    /**
     * Returns Map keyed on item sequence ID containing a list of survey response IDs
     */
    public static Map<String, List<String>> getItemSurveyInfos(List<GenericValue> items) {
        Map<String, List<String>> surveyInfos = new HashMap<>();
        if (UtilValidate.isNotEmpty(items)) {
            for (GenericValue item : items) {
                String listId = item.getString("shoppingListId");
                String itemId = item.getString("shoppingListItemSeqId");
                surveyInfos.put(listId + "." + itemId, getItemSurveyInfo(item));
            }
        }

        return surveyInfos;
    }

    /**
     * Returns a list of survey response IDs for a shopping list item
     */
    public static List<String> getItemSurveyInfo(GenericValue item) {
        List<String> responseIds = new LinkedList<>();
        List<GenericValue> surveyResp = null;
        try {
            surveyResp = item.getRelated("ShoppingListItemSurvey", null, null, false);
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }

        if (UtilValidate.isNotEmpty(surveyResp)) {
            for (GenericValue resp : surveyResp) {
                responseIds.add(resp.getString("surveyResponseId"));
            }
        }

        return responseIds;
    }

    private static GenericValue getCartUserLogin(ShoppingCart cart) {
        GenericValue ul = cart.getUserLogin();
        if (ul == null) {
            ul = cart.getAutoUserLogin();
        }
        return ul;
    }

    private static String[] makeCartItemsArray(ShoppingCart cart) {
        int len = cart.size();
        String[] arr = new String[len];
        for (int i = 0; i < len; i++) {
            arr[i] = Integer.toString(i);
        }
        return arr;
    }

    /**
     * Create the guest cookies for a shopping list
     */
    public static String createGuestShoppingListCookies (HttpServletRequest request, HttpServletResponse response){
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        HttpSession session = request.getSession(true);
        ShoppingCart cart = (ShoppingCart) session.getAttribute("shoppingCart");
        GenericValue userLogin = (GenericValue) session.getAttribute("userLogin");
        Properties systemProps = System.getProperties();
        String guestShoppingUserName = "GuestShoppingListId_" + systemProps.getProperty("user.name").replace(" ", "_");
        String productStoreId = ProductStoreWorker.getProductStoreId(request);
        int cookieAge = (60 * 60 * 24 * 30);
        String autoSaveListId = null;
        Cookie[] cookies = request.getCookies();

        // check userLogin
        if (userLogin != null) {
            String partyId = userLogin.getString("partyId");
            if (UtilValidate.isEmpty(partyId)) {
                return "success";
            }
        }

        // find shopping list ID
        if (cookies != null) {
            for (Cookie cookie: cookies) {
                if (cookie.getName().equals(guestShoppingUserName)) {
                    autoSaveListId = cookie.getValue();
                    break;
                }
            }
        }

        // clear the auto-save info
        if (userLogin!= null && ProductStoreWorker.autoSaveCart(delegator, productStoreId)) {
            if (UtilValidate.isEmpty(autoSaveListId)) {
                try {
                    Map<String, Object> listFields = UtilMisc.<String, Object>toMap("userLogin", userLogin, "productStoreId", productStoreId, "shoppingListTypeId", "SLT_SPEC_PURP", "listName", PERSISTANT_LIST_NAME);
                    Map<String, Object> newListResult = dispatcher.runSync("createShoppingList", listFields, 90, true);
                    if (newListResult != null) {
                        autoSaveListId = (String) newListResult.get("shoppingListId");
                    }
                } catch (GeneralException e) {
                    Debug.logError(e, module);
                }
                Cookie guestShoppingListCookie = new Cookie(guestShoppingUserName, autoSaveListId);
                guestShoppingListCookie.setMaxAge(cookieAge);
                guestShoppingListCookie.setPath("/");
                guestShoppingListCookie.setSecure(true);
                guestShoppingListCookie.setHttpOnly(true);
                response.addCookie(guestShoppingListCookie);
            }
        }
        if (UtilValidate.isNotEmpty(autoSaveListId)) {
            if (UtilValidate.isNotEmpty(cart)) {
                cart.setAutoSaveListId(autoSaveListId);
            } else {
                cart = ShoppingCartEvents.getCartObject(request);
                cart.setAutoSaveListId(autoSaveListId);
            }
        }
        return "success";
    }

    /**
     * Clear the guest cookies for a shopping list
     */
    public static String clearGuestShoppingListCookies (HttpServletRequest request, HttpServletResponse response){
        Properties systemProps = System.getProperties();
        String guestShoppingUserName = "GuestShoppingListId_" + systemProps.getProperty("user.name").replace(" ", "_");
        Cookie guestShoppingListCookie = new Cookie(guestShoppingUserName, null);
        guestShoppingListCookie.setMaxAge(0);
        guestShoppingListCookie.setPath("/");
        response.addCookie(guestShoppingListCookie);
        return "success";
    }
}
