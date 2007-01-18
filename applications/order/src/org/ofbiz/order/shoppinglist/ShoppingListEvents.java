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
package org.ofbiz.order.shoppinglist;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.order.shoppingcart.CartItemModifyException;
import org.ofbiz.order.shoppingcart.ItemNotFoundException;
import org.ofbiz.order.shoppingcart.ShoppingCart;
import org.ofbiz.order.shoppingcart.ShoppingCartEvents;
import org.ofbiz.order.shoppingcart.ShoppingCartItem;
import org.ofbiz.product.catalog.CatalogWorker;
import org.ofbiz.product.store.ProductStoreWorker;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ModelService;
import org.ofbiz.service.ServiceUtil;

/**
 * Shopping cart events.
 */
public class ShoppingListEvents {
    
    public static final String module = ShoppingListEvents.class.getName();
    public static final String resource = "OrderUiLabels";
    public static final String err_resource = "OrderErrorUiLabel";
    public static final String resource_error = "OrderErrorUiLabels";
    public static final String PERSISTANT_LIST_NAME = "auto-save";

    public static String addBulkFromCart(HttpServletRequest request, HttpServletResponse response) {
        GenericDelegator delegator = (GenericDelegator) request.getAttribute("delegator");
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        ShoppingCart cart = ShoppingCartEvents.getCartObject(request);
        GenericValue userLogin = (GenericValue) request.getSession().getAttribute("userLogin");

        String shoppingListId = request.getParameter("shoppingListId");
        String selectedCartItems[] = request.getParameterValues("selectedItem");

        try {
            shoppingListId = addBulkFromCart(delegator, dispatcher, cart, userLogin, shoppingListId, selectedCartItems, true, true);
        } catch (IllegalArgumentException e) {
            request.setAttribute("_ERROR_MESSAGE_", e.getMessage());
            return "error";
        }

        request.setAttribute("shoppingListId", shoppingListId);
        return "success";
    }

    public static String addBulkFromCart(GenericDelegator delegator, LocalDispatcher dispatcher, ShoppingCart cart, GenericValue userLogin, String shoppingListId, String[] items, boolean allowPromo, boolean append) throws IllegalArgumentException {
        String errMsg = null;

        if (items == null || items.length == 0) {
            errMsg = UtilProperties.getMessage(resource, "shoppinglistevents.select_items_to_add_to_list", cart.getLocale());
            throw new IllegalArgumentException(errMsg);
        }
                
        if (UtilValidate.isEmpty(shoppingListId)) {
            // create a new shopping list
            Map newListResult = null;
            try {
                newListResult = dispatcher.runSync("createShoppingList", UtilMisc.toMap("userLogin", userLogin, "productStoreId", cart.getProductStoreId(), "partyId", cart.getOrderPartyId(), "currencyUom", cart.getCurrency()));
            } catch (GenericServiceException e) {
                Debug.logError(e, "Problems creating new ShoppingList", module);
                errMsg = UtilProperties.getMessage(resource,"shoppinglistevents.cannot_create_new_shopping_list", cart.getLocale());
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
                errMsg = UtilProperties.getMessage(resource,"shoppinglistevents.shoppingListId_is_required_parameter", cart.getLocale());
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
                
        for (int i = 0; i < items.length; i++) {
            Integer cartIdInt = null;
            try {            
                cartIdInt = new Integer(items[i]);
            } catch (Exception e) {
            	Debug.logWarning(e, UtilProperties.getMessage(resource_error,"OrderIllegalCharacterInSelectedItemField", cart.getLocale()), module);
            }
            if (cartIdInt != null) {            
                ShoppingCartItem item = cart.findCartItem(cartIdInt.intValue());
                if (allowPromo || !item.getIsPromo()) {
                    Debug.logInfo("Adding cart item to shopping list [" + shoppingListId + "], allowPromo=" + allowPromo + ", item.getIsPromo()=" + item.getIsPromo() + ", item.getProductId()=" + item.getProductId() + ", item.getQuantity()=" + item.getQuantity(), module);
                    Map serviceResult = null;
                    try {
                        Map ctx = UtilMisc.toMap("userLogin", userLogin, "shoppingListId", shoppingListId, "productId", item.getProductId(), "quantity", new Double(item.getQuantity()));
                        ctx.put("reservStart", item.getReservStart());
                        ctx.put("reservLength", new Double(item.getReservLength()));
                        ctx.put("reservPersons", new Double(item.getReservPersons()));
                        serviceResult = dispatcher.runSync("createShoppingListItem", ctx);
                    } catch (GenericServiceException e) {
                        Debug.logError(e, "Problems creating ShoppingList item entity", module);
                        errMsg = UtilProperties.getMessage(resource,"shoppinglistevents.error_adding_item_to_shopping_list", cart.getLocale());
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
        GenericDelegator delegator = (GenericDelegator) request.getAttribute("delegator");
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        ShoppingCart cart = ShoppingCartEvents.getCartObject(request);

        String shoppingListId = request.getParameter("shoppingListId");
        String includeChild = request.getParameter("includeChild");
        String prodCatalogId =  CatalogWorker.getCurrentCatalogId(request);

        String eventMessage = null;
        try {
            addListToCart(delegator, dispatcher, cart, prodCatalogId, shoppingListId, (includeChild != null), true, true);
        } catch (IllegalArgumentException e) {
            request.setAttribute("_ERROR_MESSAGE_", e.getMessage());
            return "error";
        }

        if (eventMessage != null && eventMessage.length() > 0) {
            request.setAttribute("_EVENT_MESSAGE_", eventMessage);
        }

        return "success";
    }

    public static String addListToCart(GenericDelegator delegator, LocalDispatcher dispatcher, ShoppingCart cart, String prodCatalogId, String shoppingListId, boolean includeChild, boolean setAsListItem, boolean append) throws java.lang.IllegalArgumentException {
        String errMsg = null;

        // no list; no add
        if (shoppingListId == null) {
            errMsg = UtilProperties.getMessage(resource,"shoppinglistevents.choose_shopping_list", cart.getLocale());
            throw new IllegalArgumentException(errMsg);
        }
        
        // get the shopping list 
        GenericValue shoppingList = null;
        List shoppingListItems = null;
        try {
            shoppingList = delegator.findByPrimaryKey("ShoppingList", UtilMisc.toMap("shoppingListId", shoppingListId));
            if (shoppingList == null) {
                errMsg = UtilProperties.getMessage(resource,"shoppinglistevents.error_getting_shopping_list_and_items", cart.getLocale());
                throw new IllegalArgumentException(errMsg);
            }

            shoppingListItems = shoppingList.getRelated("ShoppingListItem");
            if (shoppingListItems == null) {
                shoppingListItems = new LinkedList();
            }

            // include all items of child lists if flagged to do so
            if (includeChild) {
                List childShoppingLists = shoppingList.getRelated("ChildShoppingList");
                Iterator ci = childShoppingLists.iterator();
                while (ci.hasNext()) {
                    GenericValue v = (GenericValue) ci.next();
                    List items = v.getRelated("ShoppingListItem");
                    shoppingListItems.addAll(items);
                }
            }

        } catch (GenericEntityException e) {
            Debug.logError(e, "Problems getting ShoppingList and ShoppingListItem records", module);
            errMsg = UtilProperties.getMessage(resource,"shoppinglistevents.error_getting_shopping_list_and_items", cart.getLocale());
            throw new IllegalArgumentException(errMsg);
        }
        
        // no items; not an error; just mention that nothing was added
        if (shoppingListItems == null || shoppingListItems.size() == 0) {
            errMsg = UtilProperties.getMessage(resource,"shoppinglistevents.no_items_added", cart.getLocale());
            return errMsg;
        }

        // check if we are to clear the cart first
        if (!append) {
            cart.clear();
        }

        // get the survey info for all the items
        Map shoppingListSurveyInfo = getItemSurveyInfos(shoppingListItems);

        // add the items
        StringBuffer eventMessage = new StringBuffer();                       
        Iterator i = shoppingListItems.iterator();
        while (i.hasNext()) {
            GenericValue shoppingListItem = (GenericValue) i.next();
            String productId = shoppingListItem.getString("productId");
            Double quantity = shoppingListItem.getDouble("quantity");
            Timestamp reservStart = shoppingListItem.getTimestamp("reservStart");
            Double reservLength = shoppingListItem.getDouble("reservLength");
            Double reservPersons = shoppingListItem.getDouble("reservPersons");
            try {
                String listId = shoppingListItem.getString("shoppingListId");
                String itemId = shoppingListItem.getString("shoppingListItemSeqId");

                Map attributes = new HashMap();
                // list items are noted in the shopping cart
                if (setAsListItem) {
                    attributes.put("shoppingListId", listId);
                    attributes.put("shoppingListItemSeqId", itemId);
                }

                // check if we have existing survey responses to append
                if (shoppingListSurveyInfo.containsKey(listId + "." + itemId)) {
                    attributes.put("surveyResponses", shoppingListSurveyInfo.get(listId + "." + itemId));
                }

                // TODO: add code to check for survey response requirement
                
                // i cannot get the addOrDecrease function to accept a null reservStart field: i get a null pointer exception a null constant works....
                if (reservStart == null) {
                    cart.addOrIncreaseItem(productId, null, quantity.doubleValue(), null, null, null, null, null, null, attributes, prodCatalogId, null, null, null, null, dispatcher);
                } else {
                    cart.addOrIncreaseItem(productId, null, quantity.doubleValue(), reservStart, reservLength, reservPersons, null, null, null, attributes, prodCatalogId, null, null, null, null, dispatcher);
                }
                Map messageMap = UtilMisc.toMap("productId", productId);
                errMsg = UtilProperties.getMessage(resource,"shoppinglistevents.added_product_to_cart", messageMap, cart.getLocale());
                eventMessage.append(errMsg + "\n");
            } catch (CartItemModifyException e) {
            	Debug.logWarning(e, UtilProperties.getMessage(resource_error,"OrderProblemsAddingItemFromListToCart", cart.getLocale()));
                Map messageMap = UtilMisc.toMap("productId", productId);
                errMsg = UtilProperties.getMessage(resource,"shoppinglistevents.problem_adding_product_to_cart", messageMap, cart.getLocale());
                eventMessage.append(errMsg + "\n");
            } catch (ItemNotFoundException e) {
            	Debug.logWarning(e, UtilProperties.getMessage(resource_error,"OrderProductNotFound", cart.getLocale()));
                Map messageMap = UtilMisc.toMap("productId", productId);
                errMsg = UtilProperties.getMessage(resource,"shoppinglistevents.problem_adding_product_to_cart", messageMap, cart.getLocale());
                eventMessage.append(errMsg + "\n");
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
                
        Double quantity = null;
        try {
            quantity = Double.valueOf(quantityStr);
        } catch (Exception e) {
            // do nothing, just won't pass to service if it is null
        }
        
        Map serviceInMap = new HashMap();
        serviceInMap.put("shoppingListId", request.getParameter("shoppingListId"));
        serviceInMap.put("shoppingListItemSeqId", request.getParameter("shoppingListItemSeqId"));
        serviceInMap.put("productId", request.getParameter("add_product_id"));
        serviceInMap.put("userLogin", userLogin);
        if (quantity != null) serviceInMap.put("quantity", quantity);
        Map result = null;
        try {
            result = dispatcher.runSync("updateShoppingListItem", serviceInMap);
        } catch (GenericServiceException e) {
        	String errMsg = UtilProperties.getMessage(ShoppingListEvents.err_resource,"shoppingListEvents.error_calling_update", locale) + ": "  + e.toString();            
            request.setAttribute("_ERROR_MESSAGE_", errMsg);            
            String errorMsg = "Error calling the updateShoppingListItem in handleShoppingListItemVariant: " + e.toString();
            Debug.logError(e, errorMsg, module);
            return "error";
        }
        
        ServiceUtil.getMessages(request, result, "", "", "", "", "", "", "");
        if ("error".equals(result.get(ModelService.RESPONSE_MESSAGE))) {
            return "error";
        } else {
            return "success";
        }
    }
    
    /**
     * Finds or creates a specialized (auto-save) shopping list used to record shopping bag contents between user visits.
     */
    public static String getAutoSaveListId(GenericDelegator delegator, LocalDispatcher dispatcher, String partyId, GenericValue userLogin, String productStoreId) throws GenericEntityException, GenericServiceException {
        if (partyId == null && userLogin != null) {
            partyId = userLogin.getString("partyId");
        }

        String autoSaveListId = null;
        // TODO: add sorting, just in case there are multiple...
        Map findMap = UtilMisc.toMap("partyId", partyId, "productStoreId", productStoreId, "shoppingListTypeId", "SLT_SPEC_PURP", "listName", PERSISTANT_LIST_NAME);
        List existingLists = delegator.findByAnd("ShoppingList", findMap);
        Debug.logInfo("Finding existing auto-save shopping list with:  \nfindMap: " + findMap + "\nlists: " + existingLists, module);

        GenericValue list = null;
        if (existingLists != null && !existingLists.isEmpty()) {
            list = EntityUtil.getFirst(existingLists);
            autoSaveListId = list.getString("shoppingListId");
        }

        if (list == null && dispatcher != null && userLogin != null) {
            Map listFields = UtilMisc.toMap("userLogin", userLogin, "productStoreId", productStoreId, "shoppingListTypeId", "SLT_SPEC_PURP", "listName", PERSISTANT_LIST_NAME);
            Map newListResult = dispatcher.runSync("createShoppingList", listFields);

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
            if (userLogin == null) return; //only save carts when a user is logged in....
            GenericDelegator delegator = cart.getDelegator();
            String autoSaveListId = getAutoSaveListId(delegator, dispatcher, null, userLogin, cart.getProductStoreId());

            try {
                String[] itemsArray = makeCartItemsArray(cart);
                if (itemsArray != null && itemsArray.length != 0) {
                    addBulkFromCart(delegator, dispatcher, cart, userLogin, autoSaveListId, itemsArray, false, false);
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
        GenericDelegator delegator = (GenericDelegator) request.getAttribute("delegator");
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        GenericValue productStore = ProductStoreWorker.getProductStore(request);

        if (!ProductStoreWorker.autoSaveCart(productStore)) {
            // if auto-save is disabled just return here
            return "success";
        }

        HttpSession session = request.getSession();
        ShoppingCart cart = ShoppingCartEvents.getCartObject(request);

        // safety check for missing required parameter.
        if (cart.getWebSiteId() == null) {
            cart.setWebSiteId(CatalogWorker.getWebSiteId(request));
        }

        // locate the user's identity
        GenericValue userLogin = (GenericValue) session.getAttribute("userLogin");
        if (userLogin == null) {
            userLogin = (GenericValue) session.getAttribute("autoUserLogin");
        }

        if (userLogin == null) {
            // not logged in; cannot identify the user
            return "success";
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
        }

        // check to see if we are okay to load this list
        java.sql.Timestamp lastLoad = cart.getLastListRestore();
        boolean okayToLoad = autoSaveListId == null ? false : (lastLoad == null ? true : false);
        if (!okayToLoad && lastLoad != null) {
            GenericValue shoppingList = null;
            try {
                shoppingList = delegator.findByPrimaryKey("ShoppingList", UtilMisc.toMap("shoppingListId", autoSaveListId));
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
                addListToCart(delegator, dispatcher, cart, prodCatalogId, autoSaveListId, false, false, false);
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
    public static int clearListInfo(GenericDelegator delegator, String shoppingListId) throws GenericEntityException {
        // remove the survey responses first
        delegator.removeByAnd("ShoppingListItemSurvey", UtilMisc.toMap("shoppingListId", shoppingListId));

        // next remove the items
        return delegator.removeByAnd("ShoppingListItem", UtilMisc.toMap("shoppingListId", shoppingListId));
    }

    /**
     * Creates records for survey responses on survey items
     */
    public static int makeListItemSurveyResp(GenericDelegator delegator, GenericValue item, List surveyResps) throws GenericEntityException {
        if (surveyResps != null && surveyResps.size() > 0) {
            Iterator i = surveyResps.iterator();
            int count = 0;
            while (i.hasNext()) {
                String responseId = (String) i.next();
                GenericValue listResp = delegator.makeValue("ShoppingListItemSurvey", null);
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
    public static Map getItemSurveyInfos(List items) {
        Map surveyInfos = new HashMap();
        if (items != null && items.size() > 0) {
            Iterator itemIt = items.iterator();
            while (itemIt.hasNext()) {
                GenericValue item = (GenericValue) itemIt.next();
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
    public static List getItemSurveyInfo(GenericValue item) {
        List responseIds = new ArrayList();
        List surveyResp = null;
        try {
            surveyResp = item.getRelated("ShoppingListItemSurvey");
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }

        if (surveyResp != null || surveyResp.size() > 0) {
            Iterator respIt = surveyResp.iterator();
            while (respIt.hasNext()) {
                GenericValue resp = (GenericValue) respIt.next();
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
}
