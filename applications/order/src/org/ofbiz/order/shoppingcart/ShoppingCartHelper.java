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

import java.sql.Timestamp;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.*;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.order.shoppingcart.product.ProductPromoWorker;
import org.ofbiz.product.config.ProductConfigWrapper;
import org.ofbiz.security.Security;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ModelService;
import org.ofbiz.service.ServiceUtil;

/**
 * A facade over the 
 * {@link org.ofbiz.order.shoppingcart.ShoppingCart ShoppingCart}
 * providing catalog and product services to simplify the interaction
 * with the cart directly. 
 */
public class ShoppingCartHelper {

    public static final String resource = "OrderUiLabels";
    public static String module = ShoppingCartHelper.class.getName();
    public static final String resource_error = "OrderErrorUiLabels";

    // The shopping cart to manipulate
    private ShoppingCart cart = null;

    // The entity engine delegator
    private GenericDelegator delegator = null;

    // The service invoker
    private LocalDispatcher dispatcher = null;

    /**
     * Changes will be made to the cart directly, as opposed
     * to a copy of the cart provided.
     * 
     * @param cart The cart to manipulate
     */
    public ShoppingCartHelper(GenericDelegator delegator, LocalDispatcher dispatcher, ShoppingCart cart) {
        this.dispatcher = dispatcher;
        this.delegator = delegator;
        this.cart = cart;

        if (delegator == null) {
            this.delegator = dispatcher.getDelegator();
        }
        if (dispatcher == null) {
            throw new IllegalArgumentException("Dispatcher argument is null");
        }
        if (cart == null) {
            throw new IllegalArgumentException("ShoppingCart argument is null");
        }
    }

    /** Event to add an item to the shopping cart. */
    public Map addToCart(String catalogId, String shoppingListId, String shoppingListItemSeqId, String productId,
            String productCategoryId, String itemType, String itemDescription, 
            Double price, Double amount, double quantity, 
            java.sql.Timestamp reservStart, Double reservLength, Double reservPersons, 
            java.sql.Timestamp shipBeforeDate, java.sql.Timestamp shipAfterDate,
            ProductConfigWrapper configWrapper, String itemGroupNumber, Map context, String parentProductId) {
        Map result = null;
        Map attributes = null;
        String pProductId = null;
        pProductId = parentProductId;
        // price sanity check
        if (productId == null && price != null && price.doubleValue() < 0) {
            String errMsg = UtilProperties.getMessage(resource, "cart.price_not_positive_number", this.cart.getLocale());
            result = ServiceUtil.returnError(errMsg);
            return result;
        }

        // quantity sanity check
        if (quantity < 1) {
            String errMsg = UtilProperties.getMessage(resource, "cart.quantity_not_positive_number", this.cart.getLocale());
            result = ServiceUtil.returnError(errMsg);
            return result;
        }

        // amount sanity check
        if (amount != null && amount.doubleValue() < 0) {
            amount = null;
        }

        // check desiredDeliveryDate syntax and remove if empty
        String ddDate = (String) context.get("itemDesiredDeliveryDate");
        if (!UtilValidate.isEmpty(ddDate)) {
            try {
                java.sql.Timestamp.valueOf((String) context.get("itemDesiredDeliveryDate"));
            } catch (IllegalArgumentException e) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderInvalidDesiredDeliveryDateSyntaxError",this.cart.getLocale()));
            }
        } else {
            context.remove("itemDesiredDeliveryDate");
        }

        // remove an empty comment
        String comment = (String) context.get("itemComment");
        if (UtilValidate.isEmpty(comment)) {
            context.remove("itemComment");
        }

        // stores the default desired delivery date in the cart if need
        if (!UtilValidate.isEmpty((String) context.get("useAsDefaultDesiredDeliveryDate"))) {
            cart.setDefaultItemDeliveryDate((String) context.get("itemDesiredDeliveryDate"));
        } else {
            // do we really want to clear this if it isn't checked?
            cart.setDefaultItemDeliveryDate(null);
        }

        // stores the default comment in session if need
        if (!UtilValidate.isEmpty((String) context.get("useAsDefaultComment"))) {
            cart.setDefaultItemComment((String) context.get("itemComment"));
        } else {
            // do we really want to clear this if it isn't checked?
            cart.setDefaultItemComment(null);
        }

        // Create a HashMap of product attributes - From ShoppingCartItem.attributeNames[]
        for (int namesIdx = 0; namesIdx < ShoppingCartItem.attributeNames.length; namesIdx++) {
            if (attributes == null)
                attributes = new HashMap();
            if (context.containsKey(ShoppingCartItem.attributeNames[namesIdx])) {
                attributes.put(ShoppingCartItem.attributeNames[namesIdx], context.get(ShoppingCartItem.attributeNames[namesIdx]));
            }
        }

        // check for required amount flag; if amount and no flag set to 0
        GenericValue product = null;
        if (productId != null) {
            try {
                product = delegator.findByPrimaryKeyCache("Product", UtilMisc.toMap("productId", productId));
            } catch (GenericEntityException e) {
                Debug.logError(e, "Unable to lookup product : " + productId, module);
            }
            if (product == null || product.get("requireAmount") == null || "N".equals(product.getString("requireAmount"))) {
                amount = null;
            }
            Debug.logInfo("carthelper productid " + productId,module);
            Debug.logInfo("parent productid " + pProductId,module);
            //if (product != null && !"Y".equals(product.getString("isVariant")))
            //    pProductId = null;
            
        }

        // add or increase the item to the cart        
        try {
            int itemId = -1;
            if (productId != null) {
                itemId = cart.addOrIncreaseItem(productId, amount, quantity, reservStart, reservLength, 
                                                reservPersons, shipBeforeDate, shipAfterDate, null, attributes, 
                                                catalogId, configWrapper, itemType, itemGroupNumber, pProductId, dispatcher);
            } else {
                itemId = cart.addNonProductItem(itemType, itemDescription, productCategoryId, price, quantity, attributes, catalogId, itemGroupNumber, dispatcher);
            }

            // set the shopping list info
            if (itemId > -1 && shoppingListId != null && shoppingListItemSeqId != null) {
                ShoppingCartItem item = cart.findCartItem(itemId);
                item.setShoppingList(shoppingListId, shoppingListItemSeqId);
            }
        } catch (CartItemModifyException e) {
            if (cart.getOrderType().equals("PURCHASE_ORDER")) {
                String errMsg = UtilProperties.getMessage(resource, "cart.product_not_valid_for_supplier", this.cart.getLocale());
                errMsg = errMsg + " (" + e.getMessage() + ")";
                result = ServiceUtil.returnError(errMsg);
            } else {
                result = ServiceUtil.returnError(e.getMessage());
            }
            return result;
        } catch (ItemNotFoundException e) {
            result = ServiceUtil.returnError(e.getMessage());
            return result;
        }
        
        // Indicate there were no critical errors
        result = ServiceUtil.returnSuccess();
        return result;
    }

    public Map addToCartFromOrder(String catalogId, String orderId, String[] itemIds, boolean addAll, String itemGroupNumber) {
        ArrayList errorMsgs = new ArrayList();
        Map result;
        String errMsg = null;

        if (orderId == null || orderId.length() <= 0) {
            errMsg = UtilProperties.getMessage(resource,"cart.order_not_specified_to_add_from", this.cart.getLocale());
            result = ServiceUtil.returnError(errMsg);
            return result;
        }

        boolean noItems = true;

        if (addAll) {
            Iterator itemIter = null;

            try {
                itemIter = UtilMisc.toIterator(delegator.findByAnd("OrderItem", UtilMisc.toMap("orderId", orderId), null));
            } catch (GenericEntityException e) {
                Debug.logWarning(e.getMessage(), module);
                itemIter = null;
            }

            String orderItemTypeId = null;
            if (itemIter != null && itemIter.hasNext()) {
                while (itemIter.hasNext()) {
                    GenericValue orderItem = (GenericValue) itemIter.next();
                    orderItemTypeId = orderItem.getString("orderItemTypeId");
                    // do not store rental items
                    if (orderItemTypeId.equals("RENTAL_ORDER_ITEM")) 
                        continue;
                    // never read: int itemId = -1;
                    if (orderItem.get("productId") != null && orderItem.get("quantity") != null) {
                        Double amount = orderItem.getDouble("selectedAmount");
                        try {
                            this.cart.addOrIncreaseItem(orderItem.getString("productId"), amount, orderItem.getDouble("quantity").doubleValue(),
                                    null, null, null, null, null, null, null, catalogId, null, orderItemTypeId, itemGroupNumber, null, dispatcher);
                            noItems = false;
                        } catch (CartItemModifyException e) {
                            errorMsgs.add(e.getMessage());
                        } catch (ItemNotFoundException e) {
                            errorMsgs.add(e.getMessage());
                        }
                    }
                }
                if (errorMsgs.size() > 0) {
                    result = ServiceUtil.returnError(errorMsgs);
                    result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
                    return result; // don't return error because this is a non-critical error and should go back to the same page
                }
            } else {
                noItems = true;
            }
        } else {
            noItems = true;
            if (itemIds != null) {

                for (int i = 0; i < itemIds.length; i++) {
                    String orderItemSeqId = itemIds[i];
                    GenericValue orderItem = null;

                    try {
                        orderItem = delegator.findByPrimaryKey("OrderItem", UtilMisc.toMap("orderId", orderId, "orderItemSeqId", orderItemSeqId));
                    } catch (GenericEntityException e) {
                        Debug.logWarning(e.getMessage(), module);
                        errorMsgs.add("Order line \"" + orderItemSeqId + "\" not found, so not added.");
                        continue;
                    }
                    if (orderItem != null) {
                        if (orderItem.get("productId") != null && orderItem.get("quantity") != null) {
                            Double amount = orderItem.getDouble("selectedAmount");
                            try {
                                this.cart.addOrIncreaseItem(orderItem.getString("productId"), amount,
                                        orderItem.getDouble("quantity").doubleValue(), null, null, null, null, null, null, null, 
                                        catalogId, null, orderItem.getString("orderItemTypeId"), itemGroupNumber, null, dispatcher);
                                noItems = false;
                            } catch (CartItemModifyException e) {
                                errorMsgs.add(e.getMessage());
                            } catch (ItemNotFoundException e) {
                                errorMsgs.add(e.getMessage());
                            }
                        }
                    }
                }
                if (errorMsgs.size() > 0) {
                    result = ServiceUtil.returnError(errorMsgs);
                    result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
                    return result; // don't return error because this is a non-critical error and should go back to the same page
                }
            } // else no items
        }

        if (noItems) {
            result = ServiceUtil.returnSuccess();
            result.put("_ERROR_MESSAGE_", UtilProperties.getMessage(resource_error,"OrderNoItemsFoundToAdd", this.cart.getLocale()));
            return result; // don't return error because this is a non-critical error and should go back to the same page
        }

        result = ServiceUtil.returnSuccess();
        return result;
    }

    /** 
     * Adds all products in a category according to quantity request parameter
     * for each; if no parameter for a certain product in the category, or if
     * quantity is 0, do not add. 
     * If a _ign_${itemGroupNumber} is appended to the name it will be put in that group instead of the default in the request parameter in itemGroupNumber
     * 
     * There are 2 options for the syntax:
     *  - name="quantity_${productId}" value="${quantity}
     *  - name="product_${whatever}" value="${productId}" (note: quantity is always 1)
     */
    public Map addToCartBulk(String catalogId, String categoryId, Map context) {
        String itemGroupNumber = (String) context.get("itemGroupNumber");
        // use this prefix for the main structure such as a checkbox or a text input where name="quantity_${productId}" value="${quantity}" 
        String keyPrefix = "quantity_";
        // use this prefix for a different structure, useful for radio buttons; can have any suffix, name="product_${whatever}" value="${productId}" and quantity is always 1 
        String productQuantityKeyPrefix = "product_";
        
        // If a _ign_${itemGroupNumber} is appended to the name it will be put in that group instead of the default in the request parameter in itemGroupNumber
        String ignSeparator = "_ign_";
        
        // iterate through the context and find all keys that start with "quantity_"
        Iterator entryIter = context.entrySet().iterator();
        while (entryIter.hasNext()) {
            Map.Entry entry = (Map.Entry) entryIter.next();
            String productId = null;
            String quantStr = null;
            String itemGroupNumberToUse = itemGroupNumber;
            if (entry.getKey() instanceof String) {
                String key = (String) entry.getKey();
                //Debug.logInfo("Bulk Key: " + key, module);

                int ignIndex = key.indexOf(ignSeparator);
                if (ignIndex > 0) {
                    itemGroupNumberToUse = key.substring(ignIndex + ignSeparator.length());
                    key = key.substring(0, ignIndex);
                }
                
                if (key.startsWith(keyPrefix)) {
                    productId = key.substring(keyPrefix.length());
                    quantStr = (String) entry.getValue();
                } else if (key.startsWith(productQuantityKeyPrefix)) {
                    productId = (String) entry.getValue();
                    quantStr = "1";
                } else {
                    continue;
                }
            } else {
                continue;
            }

            if (quantStr != null && quantStr.length() > 0) {
                double quantity = 0;

                try {
                    quantity = Double.parseDouble(quantStr);
                } catch (NumberFormatException nfe) {
                    quantity = 0;
                }
                if (quantity > 0.0) {
                    try {
                        if (Debug.verboseOn()) Debug.logVerbose("Bulk Adding to cart [" + quantity + "] of [" + productId + "] in Item Group [" + itemGroupNumber + "]", module);
                        this.cart.addOrIncreaseItem(productId, null, quantity, null, null, null, null, null, null, null, catalogId, null, null, itemGroupNumberToUse, null, dispatcher);
                    } catch (CartItemModifyException e) {
                        return ServiceUtil.returnError(e.getMessage());
                    } catch (ItemNotFoundException e) {
                        return ServiceUtil.returnError(e.getMessage());
                    }
                }
            }
        }

        //Indicate there were no non critical errors
        return ServiceUtil.returnSuccess();
    }

    /** 
     * Adds a set of requirements to the cart.
     */
    public Map addToCartBulkRequirements(String catalogId, Map context) {
        NumberFormat nf = NumberFormat.getNumberInstance(this.cart.getLocale());
        String itemGroupNumber = (String) context.get("itemGroupNumber");
        // check if we are using per row submit
        boolean useRowSubmit = (!context.containsKey("_useRowSubmit"))? false : 
                "Y".equalsIgnoreCase((String)context.get("_useRowSubmit"));
        
        // check if we are to also look in a global scope (no delimiter)        
        //boolean checkGlobalScope = (!context.containsKey("_checkGlobalScope"))? false :
        //        "Y".equalsIgnoreCase((String)context.get("_checkGlobalScope"));
        
        int rowCount = 0; // parsed int value
        try {
            if (context.containsKey("_rowCount")) {
                rowCount = Integer.parseInt((String)context.get("_rowCount"));
            }
        } catch (NumberFormatException e) {
            //throw new EventHandlerException("Invalid value for _rowCount");
        }
        
        // now loop throw the rows and prepare/invoke the service for each
        for (int i = 0; i < rowCount; i++) {
            String productId = null;
            String quantStr = null;
            String requirementId = null;
            String thisSuffix = UtilHttp.MULTI_ROW_DELIMITER + i;
            boolean rowSelected = (!context.containsKey("_rowSubmit" + thisSuffix))? false :
                    "Y".equalsIgnoreCase((String)context.get("_rowSubmit" + thisSuffix));
            
            // make sure we are to process this row
            if (useRowSubmit && !rowSelected) {            
                continue;
            }
            
            // build the context
            if (context.containsKey("productId" + thisSuffix)) {
                productId = (String) context.get("productId" + thisSuffix);
                quantStr = (String) context.get("quantity" + thisSuffix);
                requirementId = (String) context.get("requirementId" + thisSuffix);
                GenericValue requirement = null;
                try {
                    requirement = delegator.findByPrimaryKey("Requirement", UtilMisc.toMap("requirementId", requirementId));
                } catch(GenericEntityException gee) {
                }
                if (requirement == null) {
                    return ServiceUtil.returnError("Requirement with id [" + requirementId + "] doesn't exist.");
                }

                if (quantStr != null && quantStr.length() > 0) {
                    double quantity = 0;
                    try {
                        quantity = nf.parse(quantStr).doubleValue();
                    } catch (ParseException nfe) {
                        quantity = 0;
                    }
                    if (quantity > 0.0) {
                        Iterator items = this.cart.iterator();
                        boolean requirementAlreadyInCart = false;
                        while (items.hasNext() && !requirementAlreadyInCart) {
                            ShoppingCartItem sci = (ShoppingCartItem)items.next();
                            if (sci.getRequirementId() != null && sci.getRequirementId().equals(requirementId)) {
                                requirementAlreadyInCart = true;
                                continue;
                            }
                        }
                        if (requirementAlreadyInCart) {
                            if (Debug.warningOn()) Debug.logWarning(UtilProperties.getMessage(resource_error, "OrderTheRequirementIsAlreadyInTheCartNotAdding", UtilMisc.toMap("requirementId",requirementId), cart.getLocale()), module);
                            continue;
                        }
                        try {
                            if (Debug.verboseOn()) Debug.logVerbose("Bulk Adding to cart requirement [" + quantity + "] of [" + productId + "]", module);
                            int index = this.cart.addOrIncreaseItem(productId, null, quantity, null, null, null, requirement.getTimestamp("requiredByDate"), null, null, null, catalogId, null, null, itemGroupNumber, null, dispatcher);
                            ShoppingCartItem sci = (ShoppingCartItem)this.cart.items().get(index);
                            sci.setRequirementId(requirementId);
                        } catch (CartItemModifyException e) {
                            return ServiceUtil.returnError(e.getMessage());
                        } catch (ItemNotFoundException e) {
                            return ServiceUtil.returnError(e.getMessage());
                        }
                    }
                }
            }
        }
        //Indicate there were no non critical errors
        return ServiceUtil.returnSuccess();
    }

    /**
     * Adds all products in a category according to default quantity on ProductCategoryMember
     * for each; if no default for a certain product in the category, or if
     * quantity is 0, do not add
     */
    public Map addCategoryDefaults(String catalogId, String categoryId, String itemGroupNumber) {
        ArrayList errorMsgs = new ArrayList();
        Map result = null;
        String errMsg = null;

        if (categoryId == null || categoryId.length() <= 0) {
            errMsg = UtilProperties.getMessage(resource,"cart.category_not_specified_to_add_from", this.cart.getLocale());
            result = ServiceUtil.returnError(errMsg);
//          result = ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderNoCategorySpecifiedToAddFrom.",this.cart.getLocale()));
            return result;
        }

        Collection prodCatMemberCol = null;

        try {
            prodCatMemberCol = delegator.findByAndCache("ProductCategoryMember", UtilMisc.toMap("productCategoryId", categoryId));
        } catch (GenericEntityException e) {
            Debug.logWarning(e.toString(), module);
            Map messageMap = UtilMisc.toMap("categoryId", categoryId);
            messageMap.put("message", e.getMessage());
            errMsg = UtilProperties.getMessage(resource,"cart.could_not_get_products_in_category_cart", messageMap, this.cart.getLocale());
            result = ServiceUtil.returnError(errMsg);
            return result;
        }

        if (prodCatMemberCol == null) {
            Map messageMap = UtilMisc.toMap("categoryId", categoryId);
            errMsg = UtilProperties.getMessage(resource,"cart.could_not_get_products_in_category", messageMap, this.cart.getLocale());
            result = ServiceUtil.returnError(errMsg);
            return result;
        }

        double totalQuantity = 0;
        Iterator pcmIter = prodCatMemberCol.iterator();

        while (pcmIter.hasNext()) {
            GenericValue productCategoryMember = (GenericValue) pcmIter.next();
            Double quantity = productCategoryMember.getDouble("quantity");

            if (quantity != null && quantity.doubleValue() > 0.0) {
                try {
                    this.cart.addOrIncreaseItem(productCategoryMember.getString("productId"), 
                            null, quantity.doubleValue(), null, null, null, null, null, null, null, 
                            catalogId, null, null, itemGroupNumber, null, dispatcher);
                    totalQuantity += quantity.doubleValue();
                } catch (CartItemModifyException e) {
                    errorMsgs.add(e.getMessage());
                } catch (ItemNotFoundException e) {
                    errorMsgs.add(e.getMessage());
                }
            }
        }
        if (errorMsgs.size() > 0) {
            result = ServiceUtil.returnError(errorMsgs);
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
            return result; // don't return error because this is a non-critical error and should go back to the same page
        }

        result = ServiceUtil.returnSuccess();
        result.put("totalQuantity", new Double(totalQuantity));
        return result;
    }

    /** Delete an item from the shopping cart. */
    public Map deleteFromCart(Map context) {
        Map result = null;
        Set names = context.keySet();
        Iterator i = names.iterator();
        ArrayList errorMsgs = new ArrayList();

        while (i.hasNext()) {
            String o = (String) i.next();

            if (o.toUpperCase().startsWith("DELETE")) {
                try {
                    String indexStr = o.substring(o.lastIndexOf('_') + 1);
                    int index = Integer.parseInt(indexStr);

                    try {
                        this.cart.removeCartItem(index, dispatcher);
                    } catch (CartItemModifyException e) {
                        errorMsgs.add(e.getMessage());
                    }
                } catch (NumberFormatException nfe) {}
            }
        }

        if (errorMsgs.size() > 0) {
            result = ServiceUtil.returnError(errorMsgs);
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
            return result; // don't return error because this is a non-critical error and should go back to the same page
        }

        result = ServiceUtil.returnSuccess();
        return result;
    }

    /** Update the items in the shopping cart. */
    public Map modifyCart(Security security, GenericValue userLogin, Map context, boolean removeSelected, String[] selectedItems, Locale locale) {
        Map result = null;
        if (locale == null) {
            locale = this.cart.getLocale();
        }
        NumberFormat nf = NumberFormat.getNumberInstance(locale);

        ArrayList deleteList = new ArrayList();
        ArrayList errorMsgs = new ArrayList();

        Set parameterNames = context.keySet();
        Iterator parameterNameIter = parameterNames.iterator();

        double oldQuantity = -1;
        String oldDescription = "";
        double oldPrice = -1;

        if (this.cart.isReadOnlyCart()) {
            String errMsg = UtilProperties.getMessage(resource, "cart.cart_is_in_read_only_mode", this.cart.getLocale());
            errorMsgs.add(errMsg);
            result = ServiceUtil.returnError(errorMsgs);
            return result;
        }

        // TODO: This should be refactored to use UtilHttp.parseMultiFormData(parameters)
        while (parameterNameIter.hasNext()) {
            String parameterName = (String) parameterNameIter.next();
            int underscorePos = parameterName.lastIndexOf('_');

            if (underscorePos >= 0) {
                try {
                    String indexStr = parameterName.substring(underscorePos + 1);
                    int index = Integer.parseInt(indexStr);
                    String quantString = (String) context.get(parameterName);
                    double quantity = -1;
                    String itemDescription = "";
                    if (quantString != null) quantString = quantString.trim();

                    // get the cart item
                    ShoppingCartItem item = this.cart.findCartItem(index);
                    if (parameterName.toUpperCase().startsWith("OPTION")) {
                        if (quantString.toUpperCase().startsWith("NO^")) {
                            if (quantString.length() > 2) { // the length of the prefix
                                String featureTypeId = this.getRemoveFeatureTypeId(parameterName);
                                if (featureTypeId != null) {
                                    item.removeAdditionalProductFeatureAndAppl(featureTypeId);
                                }
                            }
                        } else {
                            GenericValue featureAppl = this.getFeatureAppl(item.getProductId(), parameterName, quantString);
                            if (featureAppl != null) {
                                item.putAdditionalProductFeatureAndAppl(featureAppl);
                            }
                        }
                    } else if (parameterName.toUpperCase().startsWith("DESCRIPTION")) {
                        itemDescription = quantString;  // the quantString is actually the description if the field name starts with DESCRIPTION
                    } else if (parameterName.startsWith("reservStart")) {
                        // should have format: yyyy-mm-dd hh:mm:ss.fffffffff
                        quantString += " 00:00:00.000000000";
                        if (item != null) {
                            Timestamp reservStart = Timestamp.valueOf(quantString);
                            item.setReservStart(reservStart);
                        }
                    } else if (parameterName.startsWith("reservLength")) {
                        if (item != null) {
                            double reservLength = nf.parse(quantString).doubleValue();
                            item.setReservLength(reservLength);
                        }
                    } else if (parameterName.startsWith("reservPersons")) {
                        if (item != null) {
                            double reservPersons = nf.parse(quantString).doubleValue();
                            item.setReservPersons(reservPersons);
                        }
                    } else if (parameterName.startsWith("shipBeforeDate")) {
                        if (item != null && quantString.length() > 0) {
                            // input is either yyyy-mm-dd or a full timestamp
                            if (quantString.length() == 10)
                                quantString += " 00:00:00.000";
                            item.setShipBeforeDate(Timestamp.valueOf(quantString));
                        }
                    } else if (parameterName.startsWith("shipAfterDate")) {
                        if (item != null && quantString.length() > 0) {
                            // input is either yyyy-mm-dd or a full timestamp
                            if (quantString.length() == 10)
                                quantString += " 00:00:00.000";
                            item.setShipAfterDate(Timestamp.valueOf(quantString));
                        }
                    } else if (parameterName.startsWith("itemType")) {
                        if (item != null && quantString.length() > 0) {
                            item.setItemType(quantString);
                        }
                    } else {
                        quantity = nf.parse(quantString).doubleValue();
                        if (quantity < 0) {
                            throw new CartItemModifyException("Quantity must be a positive number.");
                        }
                    }

                    // perhaps we need to reset the ship groups' before and after dates based on new dates for the items
                    if (parameterName.startsWith("shipAfterDate") || parameterName.startsWith("shipBeforeDate")) {
                        this.cart.setShipGroupShipDatesFromItem(item);
                    }

                    if (parameterName.toUpperCase().startsWith("UPDATE")) {
                        if (quantity == 0.0) {
                            deleteList.add(item);
                        } else {
                            if (item != null) {
                                try {
                                    // if, on a purchase order, the quantity has changed, get the new SupplierProduct entity for this quantity level.
                                    if (cart.getOrderType().equals("PURCHASE_ORDER")) {
                                        oldQuantity = item.getQuantity();
                                        if (oldQuantity != quantity) {
                                            // save the old description and price, in case the user wants to change those as well
                                            oldDescription = item.getName();
                                            oldPrice = item.getBasePrice();


                                            GenericValue productSupplier = this.getProductSupplier(item.getProductId(), new Double(quantity), cart.getCurrency());

                                            if (productSupplier == null) {
                                                if ("_NA_".equals(cart.getPartyId())) {
                                                    // no supplier does not require the supplier product
                                                    item.setQuantity(quantity, dispatcher, this.cart);
                                                    item.setName(item.getProduct().getString("internalName"));
                                                } else {
                                                    // in this case, the user wanted to purchase a quantity which is not available (probably below minimum)
                                                    String errMsg = UtilProperties.getMessage(resource, "cart.product_not_valid_for_supplier", this.cart.getLocale());
                                                    errMsg = errMsg + " (" + item.getProductId() + ", " + quantity + ", " + cart.getCurrency() + ")";
                                                    errorMsgs.add(errMsg);
                                                }
                                            } else {
                                                item.setQuantity(quantity, dispatcher, this.cart);
                                                item.setBasePrice(productSupplier.getDouble("lastPrice").doubleValue());
                                                item.setName(ShoppingCartItem.getPurchaseOrderItemDescription(item.getProduct(), productSupplier, cart.getLocale()));
                                            }
                                        }
                                    } else {
                                        item.setQuantity(quantity, dispatcher, this.cart);
                                    }
                                } catch (CartItemModifyException e) {
                                    errorMsgs.add(e.getMessage());
                                }
                            }
                        }
                    }

                    if (parameterName.toUpperCase().startsWith("DESCRIPTION")) {
                        if (!oldDescription.equals(itemDescription)) {
                            if (security.hasEntityPermission("ORDERMGR", "_CREATE", userLogin)) {
                                if (item != null) {
                                    item.setName(itemDescription);
                                }
                            }
                        }
                    }

                    if (parameterName.toUpperCase().startsWith("PRICE")) {
                        NumberFormat pf = NumberFormat.getCurrencyInstance(locale);
                        String tmpQuantity = pf.format(quantity);
                        String tmpOldPrice = pf.format(oldPrice);
                        if (!tmpOldPrice.equals(tmpQuantity)) {
                            if (security.hasEntityPermission("ORDERMGR", "_CREATE", userLogin)) {
                                if (item != null) {
                                    item.setBasePrice(quantity); // this is quantity because the parsed number variable is the same as quantity
                                    item.setDisplayPrice(quantity); // or the amount shown the cart items page won't be right 
                                    item.setIsModifiedPrice(true); // flag as a modified price
                                }
                            }
                        }
                    }

                    if (parameterName.toUpperCase().startsWith("DELETE")) {
                        deleteList.add(this.cart.findCartItem(index));
                    }
                } catch (NumberFormatException nfe) {
                    Debug.logWarning(nfe, UtilProperties.getMessage(resource_error, "OrderCaughtNumberFormatExceptionOnCartUpdate", cart.getLocale()));
                } catch (ParseException pe) {
                    Debug.logWarning(pe, UtilProperties.getMessage(resource_error, "OrderCaughtParseExceptionOnCartUpdate", cart.getLocale()));
                } catch (Exception e) {
                    Debug.logWarning(e, UtilProperties.getMessage(resource_error, "OrderCaughtExceptionOnCartUpdate", cart.getLocale()));
                }
            } // else not a parameter we need
        }

        // get a list of the items to delete
        if (removeSelected) {
            for (int si = 0; si < selectedItems.length; si++) {
                String indexStr = selectedItems[si];
                ShoppingCartItem item = null;
                try {
                    int index = Integer.parseInt(indexStr);
                    item = this.cart.findCartItem(index);
                } catch (Exception e) {
                    Debug.logWarning(e, UtilProperties.getMessage(resource_error, "OrderProblemsGettingTheCartItemByIndex", cart.getLocale()));
                }
                if (item != null) {
                    deleteList.add(item);
                }
            }
        }

        Iterator di = deleteList.iterator();

        while (di.hasNext()) {
            ShoppingCartItem item = (ShoppingCartItem) di.next();
            int itemIndex = this.cart.getItemIndex(item);

            if (Debug.infoOn())
                Debug.logInfo("Removing item index: " + itemIndex, module);
            try {
                this.cart.removeCartItem(itemIndex, dispatcher);
            } catch (CartItemModifyException e) {
                result = ServiceUtil.returnError(new Vector());
                errorMsgs.add(e.getMessage());
            }
        }

        if (context.containsKey("alwaysShowcart")) {
            this.cart.setViewCartOnAdd(true);
        } else {
            this.cart.setViewCartOnAdd(false);
        }

        // Promotions are run again.
        ProductPromoWorker.doPromotions(this.cart, dispatcher);

        if (errorMsgs.size() > 0) {
            result = ServiceUtil.returnError(errorMsgs);
            return result;
        }

        result = ServiceUtil.returnSuccess();
        return result;
    }

    /** Empty the shopping cart. */
    public boolean clearCart() {
        this.cart.clear();
        return true;
    }

    /** Returns the shopping cart this helper is wrapping. */
    public ShoppingCart getCartObject() {
        return this.cart;
    }

    public GenericValue getFeatureAppl(String productId, String optionField, String featureId) {
        if (delegator == null) {
            throw new IllegalArgumentException("No delegator available to lookup ProductFeature");
        }

        Map fields = UtilMisc.toMap("productId", productId, "productFeatureId", featureId);
        if (optionField != null) {
            int featureTypeStartIndex = optionField.indexOf('^') + 1;
            int featureTypeEndIndex = optionField.lastIndexOf('_');
            if (featureTypeStartIndex > 0 && featureTypeEndIndex > 0) {
                fields.put("productFeatureTypeId", optionField.substring(featureTypeStartIndex, featureTypeEndIndex));
            }
        }

        GenericValue productFeatureAppl = null;
        List features = null;
        try {
            features = delegator.findByAnd("ProductFeatureAndAppl", fields, UtilMisc.toList("-fromDate"));
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return null;
        }

        if (features != null) {
            if (features.size() > 1) {
                features = EntityUtil.filterByDate(features);
            }
            productFeatureAppl = EntityUtil.getFirst(features);
        }

        return productFeatureAppl;
    }

    public String getRemoveFeatureTypeId(String optionField) {
        if (optionField != null) {
            int featureTypeStartIndex = optionField.indexOf('^') + 1;
            int featureTypeEndIndex = optionField.lastIndexOf('_');
            if (featureTypeStartIndex > 0 && featureTypeEndIndex > 0) {
                return optionField.substring(featureTypeStartIndex, featureTypeEndIndex);
            }
        }
        return null;
    }
    /**
     * Select an agreement
     *
     * @param agreementId
     */
    public Map selectAgreement(String agreementId) {
        Map result = null;
        GenericValue agreement = null;

        if ((this.delegator == null) || (this.dispatcher == null) || (this.cart == null)) {
            result = ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderDispatcherOrDelegatorOrCartArgumentIsNull",this.cart.getLocale()));
            return result;
        }

        if ((agreementId == null) || (agreementId.length() <= 0)) {
            result = ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderNoAgreementSpecified",this.cart.getLocale()));
            return result;
        }

        try {
            agreement = this.delegator.findByPrimaryKeyCache("Agreement",UtilMisc.toMap("agreementId", agreementId));
        } catch (GenericEntityException e) {
            Debug.logWarning(e.toString(), module);
            result = ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderCouldNotGetAgreement",UtilMisc.toMap("agreementId",agreementId),this.cart.getLocale()) + UtilProperties.getMessage(resource_error,"OrderError",this.cart.getLocale()) + e.getMessage());
            return result;
        }

        if (agreement == null) {
            result = ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderCouldNotGetAgreement",UtilMisc.toMap("agreementId",agreementId),this.cart.getLocale()));
        } else {
            // set the agreement id in the cart
            cart.setAgreementId(agreementId);
            try {
                 // set the currency based on the pricing agreement
                 List agreementItems = agreement.getRelated("AgreementItem", UtilMisc.toMap("agreementItemTypeId", "AGREEMENT_PRICING_PR"), null);
                 if (agreementItems.size() > 0) {
                       GenericValue agreementItem = (GenericValue) agreementItems.get(0);
                       String currencyUomId = (String) agreementItem.get("currencyUomId");
                       try {
                            cart.setCurrency(dispatcher,currencyUomId);
                       } catch (CartItemModifyException ex) {
                           result = ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderSetCurrencyError",this.cart.getLocale()) + ex.getMessage());
                            return result;
                       }
                 }
            } catch (GenericEntityException e) {
                Debug.logWarning(e.toString(), module);
                result = ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderCouldNotGetAgreementItemsThrough",UtilMisc.toMap("agreementId",agreementId),this.cart.getLocale()) + UtilProperties.getMessage(resource_error,"OrderError",this.cart.getLocale()) + e.getMessage());
                return result;
            }

            try {
                 // clear the existing order terms
                 cart.removeOrderTerms();
                 // set order terms based on agreement terms
                 List agreementTerms = agreement.getRelated("AgreementTerm");
                 if (agreementTerms.size() > 0) {
                      for (int i = 0; agreementTerms.size() > i;i++) {
                           GenericValue agreementTerm = (GenericValue) agreementTerms.get(i);
                           String termTypeId = (String) agreementTerm.get("termTypeId");
                           Double termValue = (Double) agreementTerm.get("termValue");
                           Long termDays = (Long) agreementTerm.get("termDays");
                           String description = agreementTerm.getString("description");
                           cart.addOrderTerm(termTypeId, termValue, termDays, description);
                      }
                  }
            } catch (GenericEntityException e) {
                  Debug.logWarning(e.toString(), module);
                  result = ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderCouldNotGetAgreementTermsThrough",UtilMisc.toMap("agreementId",agreementId),this.cart.getLocale())  + UtilProperties.getMessage(resource_error,"OrderError",this.cart.getLocale()) + e.getMessage());
                  return result;
            }
        }
        return result;
    }

    public Map setCurrency(String currencyUomId) {
        Map result = null;

        try {
            this.cart.setCurrency(this.dispatcher,currencyUomId);
            result = ServiceUtil.returnSuccess();
         } catch (CartItemModifyException ex) {
             result = ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"Set currency error",this.cart.getLocale()) + ex.getMessage());
             return result;
         }
        return result;
    }

    public Map addOrderTerm(String termTypeId,Double termValue,Long termDays) {
        return addOrderTerm(termTypeId, termValue, termDays, null);
    }

    public Map addOrderTerm(String termTypeId,Double termValue,Long termDays, String description) {
        Map result = null;
        this.cart.addOrderTerm(termTypeId,termValue,termDays,description);
        result = ServiceUtil.returnSuccess();
        return result;
    }

    public Map removeOrderTerm(int index) {
        Map result = null;
        this.cart.removeOrderTerm(index);
        result = ServiceUtil.returnSuccess();
        return result;
    }

    /** Get the first SupplierProduct record for productId with matching quantity and currency */
    public GenericValue getProductSupplier(String productId, Double quantity, String currencyUomId) {
        GenericValue productSupplier = null;
        Map params = UtilMisc.toMap("productId", productId, "partyId", cart.getPartyId(), "currencyUomId", currencyUomId, "quantity", quantity);
        try {
            Map result = dispatcher.runSync("getSuppliersForProduct", params);
            List productSuppliers = (List)result.get("supplierProducts");
            if ((productSuppliers != null) && (productSuppliers.size() > 0)) {
                productSupplier=(GenericValue) productSuppliers.get(0);
            }
        } catch (GenericServiceException e) {
            Debug.logWarning(UtilProperties.getMessage(resource_error,"OrderRunServiceGetSuppliersForProductError", cart.getLocale()) + e.getMessage(), module);
        }
        return productSupplier;
    }

}
