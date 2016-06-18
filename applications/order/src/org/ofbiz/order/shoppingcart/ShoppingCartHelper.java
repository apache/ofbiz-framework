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
import java.sql.Timestamp;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.ObjectType;
import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilNumber;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.util.EntityQuery;
import org.ofbiz.entity.util.EntityTypeUtil;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.entity.util.EntityUtilProperties;
import org.ofbiz.order.order.OrderReadHelper;
import org.ofbiz.order.shoppingcart.product.ProductPromoWorker;
import org.ofbiz.product.config.ProductConfigWorker;
import org.ofbiz.product.config.ProductConfigWrapper;
import org.ofbiz.product.product.ProductWorker;
import org.ofbiz.security.Security;
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
    private Delegator delegator = null;

    // The service invoker
    private LocalDispatcher dispatcher = null;

    /**
     * Changes will be made to the cart directly, as opposed
     * to a copy of the cart provided.
     *
     * @param cart The cart to manipulate
     */
    public ShoppingCartHelper(Delegator delegator, LocalDispatcher dispatcher, ShoppingCart cart) {
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
    public Map<String, Object> addToCart(String catalogId, String shoppingListId, String shoppingListItemSeqId, String productId,
            String productCategoryId, String itemType, String itemDescription,
            BigDecimal price, BigDecimal amount, BigDecimal quantity,
            java.sql.Timestamp reservStart, BigDecimal reservLength, BigDecimal reservPersons,
            java.sql.Timestamp shipBeforeDate, java.sql.Timestamp shipAfterDate,
            ProductConfigWrapper configWrapper, String itemGroupNumber, Map<String, ? extends Object> context, String parentProductId) {

        return addToCart(catalogId,shoppingListId,shoppingListItemSeqId,productId,
                productCategoryId,itemType,itemDescription,price,amount,quantity,
                reservStart,reservLength,reservPersons,null,null,shipBeforeDate,shipAfterDate,
                configWrapper,itemGroupNumber,context,parentProductId);
    }

    /** Event to add an item to the shopping cart with accommodation. */
    public Map<String, Object> addToCart(String catalogId, String shoppingListId, String shoppingListItemSeqId, String productId,
            String productCategoryId, String itemType, String itemDescription,
            BigDecimal price, BigDecimal amount, BigDecimal quantity,
            java.sql.Timestamp reservStart, BigDecimal reservLength, BigDecimal reservPersons, String accommodationMapId,String accommodationSpotId,
            java.sql.Timestamp shipBeforeDate, java.sql.Timestamp shipAfterDate,
            ProductConfigWrapper configWrapper, String itemGroupNumber, Map<String, ? extends Object> context, String parentProductId) {
        Map<String, Object> result = null;
        Map<String, Object> attributes = null;
        String pProductId = null;
        pProductId = parentProductId;
        // price sanity check
        if (productId == null && price != null && price.compareTo(BigDecimal.ZERO) < 0) {
            String errMsg = UtilProperties.getMessage(resource_error, "cart.price_not_positive_number", this.cart.getLocale());
            result = ServiceUtil.returnError(errMsg);
            return result;
        }

        // quantity sanity check
        if (quantity.compareTo(BigDecimal.ZERO) < 0) {
            String errMsg = UtilProperties.getMessage(resource_error, "cart.quantity_not_positive_number", this.cart.getLocale());
            result = ServiceUtil.returnError(errMsg);
            return result;
        }

        // amount sanity check
        if (amount != null && amount.compareTo(BigDecimal.ZERO) < 0) {
            String errMsg = UtilProperties.getMessage(resource_error, "cart.amount_not_positive_number", this.cart.getLocale());
            result = ServiceUtil.returnError(errMsg);
            return result;
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
        if (!UtilValidate.isEmpty(context.get("useAsDefaultDesiredDeliveryDate"))) {
            cart.setDefaultItemDeliveryDate((String) context.get("itemDesiredDeliveryDate"));
        } else {
            // do we really want to clear this if it isn't checked?
            cart.setDefaultItemDeliveryDate(null);
        }

        // stores the default comment in session if need
        if (!UtilValidate.isEmpty(context.get("useAsDefaultComment"))) {
            cart.setDefaultItemComment((String) context.get("itemComment"));
        } else {
            // do we really want to clear this if it isn't checked?
            cart.setDefaultItemComment(null);
        }

        // Create a HashMap of product attributes - From ShoppingCartItem.attributeNames[]
        for (int namesIdx = 0; namesIdx < ShoppingCartItem.attributeNames.length; namesIdx++) {
            if (attributes == null)
                attributes = new HashMap<String, Object>();
            if (context.containsKey(ShoppingCartItem.attributeNames[namesIdx])) {
                attributes.put(ShoppingCartItem.attributeNames[namesIdx], context.get(ShoppingCartItem.attributeNames[namesIdx]));
            }
        }

        // check for required amount flag; if amount and no flag set to 0
        GenericValue product = null;
        if (productId != null) {
            try {
                product = EntityQuery.use(delegator).from("Product").where("productId", productId).cache().queryOne();
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

        // Get the additional features selected for the product (if any)
        Map<String, Object> selectedFeatures = UtilHttp.makeParamMapWithPrefix(context, null, "FT", null);
        Map<String, GenericValue> additionalFeaturesMap = new HashMap<String, GenericValue>();
        for (String selectedFeatureType : selectedFeatures.keySet()) {
            String selectedFeatureValue = (String)selectedFeatures.get(selectedFeatureType);
            if (UtilValidate.isNotEmpty(selectedFeatureValue)) {
                GenericValue productFeatureAndAppl = null;
                try {
                    productFeatureAndAppl = EntityQuery.use(delegator).from("ProductFeatureAndAppl")
                            .where("productId", productId, "productFeatureTypeId", selectedFeatureType, "productFeatureId", selectedFeatureValue)
                            .filterByDate()
                            .queryFirst();
                } catch (GenericEntityException gee) {
                    Debug.logError(gee, module);
                }
                if (UtilValidate.isNotEmpty(productFeatureAndAppl)) {
                    productFeatureAndAppl.set("productFeatureApplTypeId", "STANDARD_FEATURE");
                }
                additionalFeaturesMap.put(selectedFeatureType, productFeatureAndAppl);
            }
        }

        // get order item attributes
        Map<String, String> orderItemAttributes = new HashMap<String, String>();
        String orderItemAttributePrefix = EntityUtilProperties.getPropertyValue("order", "order.item.attr.prefix", delegator);
        for (Entry<String, ? extends Object> entry : context.entrySet()) {
            if (entry.getKey().toString().contains(orderItemAttributePrefix) && UtilValidate.isNotEmpty(entry.getValue())) {
                orderItemAttributes.put(entry.getKey().replaceAll(orderItemAttributePrefix, ""), entry.getValue().toString());
            }
        }

        // add or increase the item to the cart
        int itemId = -1;
        try {
            if (productId != null) {

                       itemId = cart.addOrIncreaseItem(productId, amount, quantity, reservStart, reservLength,
                                                reservPersons, accommodationMapId, accommodationSpotId, shipBeforeDate, shipAfterDate, additionalFeaturesMap, attributes,
                                                orderItemAttributes, catalogId, configWrapper, itemType, itemGroupNumber, pProductId, dispatcher);

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
                String errMsg = UtilProperties.getMessage(resource_error, "cart.product_not_valid_for_supplier", this.cart.getLocale());
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
        if (itemId != -1) {
            result.put("itemId", new Integer(itemId));
        }
        return result;
    }

    public Map<String, Object> addToCartFromOrder(String catalogId, String orderId, String[] itemIds, boolean addAll, String itemGroupNumber) {
        ArrayList<String> errorMsgs = new ArrayList<String>();
        Map<String, Object> result;
        String errMsg = null;

        if (UtilValidate.isEmpty(orderId)) {
            errMsg = UtilProperties.getMessage(resource_error,"cart.order_not_specified_to_add_from", this.cart.getLocale());
            result = ServiceUtil.returnError(errMsg);
            return result;
        }

        boolean noItems = true;
        List<? extends Object> itemIdList = null;
        Iterator<? extends Object> itemIter = null;
        OrderReadHelper orderHelper = new OrderReadHelper(delegator, orderId);
        if (addAll) {
            itemIdList = orderHelper.getOrderItems();
        } else {
            if (itemIds != null) {
                itemIdList = Arrays.asList(itemIds);
            }
        }
        if (UtilValidate.isNotEmpty(itemIdList)) {
            itemIter = itemIdList.iterator();
        }

        String orderItemTypeId = null;
        String productId = null;
        if (itemIter != null && itemIter.hasNext()) {
            while (itemIter.hasNext()) {
                GenericValue orderItem = null;
                Object value = itemIter.next();
                if (value instanceof GenericValue) {
                    orderItem = (GenericValue) value;
                } else {
                    String orderItemSeqId = (String) value;
                    orderItem = orderHelper.getOrderItem(orderItemSeqId);
                }
                // do not include PROMO items
                if (orderItem.get("isPromo") != null && "Y".equals(orderItem.getString("isPromo"))) {
                    continue;
                }
                orderItemTypeId = orderItem.getString("orderItemTypeId");
                productId = orderItem.getString("productId");
                // do not store rental items
                if (orderItemTypeId.equals("RENTAL_ORDER_ITEM"))
                    continue;
                // never read: int itemId = -1;
                if (UtilValidate.isNotEmpty(productId) && orderItem.get("quantity") != null) {
                    BigDecimal amount = orderItem.getBigDecimal("selectedAmount");
                    ProductConfigWrapper configWrapper = null;
                    String aggregatedProdId = null;
                    if (EntityTypeUtil.hasParentType(delegator, "ProductType", "productTypeId", ProductWorker.getProductTypeId(delegator, productId), "parentTypeId", "AGGREGATED")) {
                        try {
                            GenericValue instanceProduct = EntityQuery.use(delegator).from("Product").where("productId", productId).queryOne();
                            String configId = instanceProduct.getString("configId");
                            aggregatedProdId = ProductWorker.getInstanceAggregatedId(delegator, productId);
                            configWrapper = ProductConfigWorker.loadProductConfigWrapper(delegator, dispatcher, configId, aggregatedProdId, cart.getProductStoreId(), catalogId, cart.getWebSiteId(), cart.getCurrency(), cart.getLocale(), cart.getAutoUserLogin());
                        } catch (GenericEntityException e) {
                            errorMsgs.add(e.getMessage());
                        }

                    }
                    try {
                        this.cart.addOrIncreaseItem(UtilValidate.isNotEmpty(aggregatedProdId) ? aggregatedProdId :  productId, amount, orderItem.getBigDecimal("quantity"),
                                null, null, null, null, null, null, null, catalogId, configWrapper, orderItemTypeId, itemGroupNumber, null, dispatcher);
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
    public Map<String, Object> addToCartBulk(String catalogId, String categoryId, Map<String, ? extends Object> context) {
        String itemGroupNumber = (String) context.get("itemGroupNumber");
        // use this prefix for the main structure such as a checkbox or a text input where name="quantity_${productId}" value="${quantity}"
        String keyPrefix = "quantity_";
        // use this prefix for a different structure, useful for radio buttons; can have any suffix, name="product_${whatever}" value="${productId}" and quantity is always 1
        String productQuantityKeyPrefix = "product_";

        // If a _ign_${itemGroupNumber} is appended to the name it will be put in that group instead of the default in the request parameter in itemGroupNumber
        String ignSeparator = "_ign_";

        // iterate through the context and find all keys that start with "quantity_"
        for (Map.Entry<String, ? extends Object> entry : context.entrySet()) {
            String productId = null;
            String quantStr = null;
            String itemGroupNumberToUse = itemGroupNumber;
            String originalProductId = null;
            if (entry.getKey() instanceof String) {
                String key = entry.getKey();
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

            if (UtilValidate.isNotEmpty(quantStr)) {
                BigDecimal quantity = BigDecimal.ZERO;

                try {
                    quantity = new BigDecimal(quantStr);
                } catch (NumberFormatException nfe) {
                    quantity = BigDecimal.ZERO;
                }
                if (quantity.compareTo(BigDecimal.ZERO) > 0) {
                    // check for alternative packing
                    if(ProductWorker.isAlternativePacking(delegator, null , productId)){
                        GenericValue originalProduct = null;
                        originalProductId = productId;
                        productId = ProductWorker.getOriginalProductId(delegator, productId);
                        try {
                            originalProduct = EntityQuery.use(delegator).from("Product").where("productId", originalProductId).queryOne();
                        } catch (GenericEntityException e) {
                            Debug.logError(e, "Error getting parent product", module);
                        }
                        BigDecimal piecesIncluded = BigDecimal.ZERO;
                        if(originalProduct != null){
                            piecesIncluded = new BigDecimal(originalProduct.getLong("piecesIncluded"));
                            quantity = quantity.multiply(piecesIncluded);
                        }
                    }

                    try {
                        //For quantity we should test if we allow to add decimal quantity for this product an productStore : 
                        // if not and if quantity is in decimal format then return error.
                        if(! ProductWorker.isDecimalQuantityOrderAllowed(delegator, productId, cart.getProductStoreId())){
                            BigDecimal remainder = quantity.remainder(BigDecimal.ONE);
                            if (remainder.compareTo(BigDecimal.ZERO) != 0) {
                                return ServiceUtil.returnError(UtilProperties.getMessage(resource_error, "cart.addToCart.quantityInDecimalNotAllowed", this.cart.getLocale()));
                            }
                            quantity = quantity.setScale(0, UtilNumber.getBigDecimalRoundingMode("order.rounding"));
                        } else {
                            quantity = quantity.setScale(UtilNumber.getBigDecimalScale("order.decimals"), UtilNumber.getBigDecimalRoundingMode("order.rounding"));
                        }
                    } catch(GenericEntityException e) {
                        Debug.logError(e.getMessage(), module);
                        quantity = BigDecimal.ONE;
                    }
                    if (quantity.compareTo(BigDecimal.ZERO) < 0) {
                        return ServiceUtil.returnError(UtilProperties.getMessage(resource_error, "cart.quantity_not_positive_number", this.cart.getLocale()));
                    }

                    try {
                        if (Debug.verboseOn()) Debug.logVerbose("Bulk Adding to cart [" + quantity + "] of [" + productId + "] in Item Group [" + itemGroupNumber + "]", module);
                        this.cart.addOrIncreaseItem(productId, null, quantity, null, null, null, null, null, null, null, catalogId, null, null, itemGroupNumberToUse, originalProductId, dispatcher);
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
    public Map<String, Object> addToCartBulkRequirements(String catalogId, Map<String, ? extends Object> context) {
        String itemGroupNumber = (String) context.get("itemGroupNumber");
        // check if we are using per row submit
        boolean useRowSubmit = (!context.containsKey("_useRowSubmit"))? false :
                "Y".equalsIgnoreCase((String)context.get("_useRowSubmit"));

        // check if we are to also look in a global scope (no delimiter)
        //boolean checkGlobalScope = (!context.containsKey("_checkGlobalScope"))? false :
        //        "Y".equalsIgnoreCase((String)context.get("_checkGlobalScope"));

        // The number of multi form rows is retrieved
        int rowCount = UtilHttp.getMultiFormRowCount(context);

        // assume that the facility is the same for all requirements
        String facilityId = (String) context.get("facilityId_o_0");
        if (UtilValidate.isNotEmpty(facilityId)) {
            cart.setFacilityId(facilityId);
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
                    requirement = EntityQuery.use(delegator).from("Requirement").where("requirementId", requirementId).queryOne();
                } catch (GenericEntityException gee) {
                }
                if (requirement == null) {
                    return ServiceUtil.returnFailure(UtilProperties.getMessage(resource, 
                            "OrderRequirementDoesNotExists", 
                            UtilMisc.toMap("requirementId", requirementId), cart.getLocale()));
                }

                if (UtilValidate.isNotEmpty(quantStr)) {
                    BigDecimal quantity = BigDecimal.ZERO;
                    try {
                        quantity = (BigDecimal) ObjectType.simpleTypeConvert(quantStr, "BigDecimal", null, cart.getLocale());
                    } catch (GeneralException ge) {
                        quantity = BigDecimal.ZERO;
                    }
                    if (quantity.compareTo(BigDecimal.ZERO) > 0) {
                        Iterator<ShoppingCartItem> items = this.cart.iterator();
                        boolean requirementAlreadyInCart = false;
                        while (items.hasNext() && !requirementAlreadyInCart) {
                            ShoppingCartItem sci = items.next();
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
                            ShoppingCartItem sci = this.cart.items().get(index);
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
    public Map<String, Object> addCategoryDefaults(String catalogId, String categoryId, String itemGroupNumber) {
        ArrayList<String> errorMsgs = new ArrayList<String>();
        Map<String, Object> result = null;
        String errMsg = null;

        if (UtilValidate.isEmpty(categoryId)) {
            errMsg = UtilProperties.getMessage(resource_error,"cart.category_not_specified_to_add_from", this.cart.getLocale());
            result = ServiceUtil.returnError(errMsg);
            return result;
        }

        Collection<GenericValue> prodCatMemberCol = null;

        try {
            prodCatMemberCol = EntityQuery.use(delegator).from("ProductCategoryMember").where("productCategoryId", categoryId).cache(true).queryList();
        } catch (GenericEntityException e) {
            Debug.logWarning(e.toString(), module);
            Map<String, Object> messageMap = UtilMisc.<String, Object>toMap("categoryId", categoryId);
            messageMap.put("message", e.getMessage());
            errMsg = UtilProperties.getMessage(resource_error,"cart.could_not_get_products_in_category_cart", messageMap, this.cart.getLocale());
            result = ServiceUtil.returnError(errMsg);
            return result;
        }

        if (prodCatMemberCol == null) {
            Map<String, Object> messageMap = UtilMisc.<String, Object>toMap("categoryId", categoryId);
            errMsg = UtilProperties.getMessage(resource_error,"cart.could_not_get_products_in_category", messageMap, this.cart.getLocale());
            result = ServiceUtil.returnError(errMsg);
            return result;
        }

        BigDecimal totalQuantity = BigDecimal.ZERO;
        for (GenericValue productCategoryMember : prodCatMemberCol) {
            BigDecimal quantity = productCategoryMember.getBigDecimal("quantity");

            if (quantity != null && quantity.compareTo(BigDecimal.ZERO) > 0) {
                try {
                    this.cart.addOrIncreaseItem(productCategoryMember.getString("productId"),
                            null, quantity, null, null, null, null, null, null, null,
                            catalogId, null, null, itemGroupNumber, null, dispatcher);
                    totalQuantity = totalQuantity.add(quantity);
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
        result.put("totalQuantity", totalQuantity);
        return result;
    }

    /** Delete an item from the shopping cart. */
    public Map<String, Object> deleteFromCart(Map<String, ? extends Object> context) {
        Map<String, Object> result = null;
        ArrayList<String> errorMsgs = new ArrayList<String>();
        for (String o : context.keySet()) {
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
    public Map<String, Object> modifyCart(Security security, GenericValue userLogin, Map<String, ? extends Object> context, boolean removeSelected, String[] selectedItems, Locale locale) {
        Map<String, Object> result = null;
        if (locale == null) {
            locale = this.cart.getLocale();
        }

        ArrayList<ShoppingCartItem> deleteList = new ArrayList<ShoppingCartItem>();
        ArrayList<String> errorMsgs = new ArrayList<String>();

        BigDecimal oldQuantity = BigDecimal.ONE.negate();
        String oldDescription = "";
        String oldItemComment = "";
        BigDecimal oldPrice = BigDecimal.ONE.negate();

        if (this.cart.isReadOnlyCart()) {
            String errMsg = UtilProperties.getMessage(resource_error, "cart.cart_is_in_read_only_mode", this.cart.getLocale());
            errorMsgs.add(errMsg);
            result = ServiceUtil.returnError(errorMsgs);
            return result;
        }

        // TODO: This should be refactored to use UtilHttp.parseMultiFormData(parameters)
        for (String parameterName : context.keySet()) {
            int underscorePos = parameterName.lastIndexOf('_');

            // ignore localized date input elements, just use their counterpart without the _i18n suffix
            if (underscorePos >= 0 && (!parameterName.endsWith("_i18n"))) {
                try {
                    String indexStr = parameterName.substring(underscorePos + 1);
                    int index = Integer.parseInt(indexStr);
                    String quantString = (String) context.get(parameterName);
                    BigDecimal quantity = BigDecimal.ONE.negate();
                    String itemDescription = "";
                    String itemComment = "";
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
                    } else if (parameterName.toUpperCase().startsWith("COMMENT")) {
                         itemComment= quantString;  // the quantString is actually the comment if the field name starts with COMMENT
                    } else if (parameterName.startsWith("reservStart")) {
                        if (quantString.length() ==0) {
                            // should have format: yyyy-mm-dd hh:mm:ss.fffffffff
                            quantString += " 00:00:00.000000000";
                        }
                        if (item != null) {
                            Timestamp reservStart = Timestamp.valueOf(quantString);
                            item.setReservStart(reservStart);
                        }
                    } else if (parameterName.startsWith("reservLength")) {
                        if (item != null) {
                            BigDecimal reservLength = (BigDecimal) ObjectType.simpleTypeConvert(quantString, "BigDecimal", null, locale);
                            item.setReservLength(reservLength);
                        }
                    } else if (parameterName.startsWith("reservPersons")) {
                        if (item != null) {
                            BigDecimal reservPersons = (BigDecimal) ObjectType.simpleTypeConvert(quantString, "BigDecimal", null, locale);
                            item.setReservPersons(reservPersons);
                        }
                    } else if (parameterName.startsWith("shipBeforeDate")) {
                        if (UtilValidate.isNotEmpty(quantString)) {
                            // input is either yyyy-mm-dd or a full timestamp
                            if (quantString.length() == 10)
                                quantString += " 00:00:00.000";
                            item.setShipBeforeDate(Timestamp.valueOf(quantString));
                        }
                    } else if (parameterName.startsWith("shipAfterDate")) {
                        if (UtilValidate.isNotEmpty(quantString)) {
                            // input is either yyyy-mm-dd or a full timestamp
                            if (quantString.length() == 10)
                                quantString += " 00:00:00.000";
                            item.setShipAfterDate(Timestamp.valueOf(quantString));
                        }
                    } else if (parameterName.startsWith("amount")) {
                        if (UtilValidate.isNotEmpty(quantString)) {
                            BigDecimal amount = new BigDecimal(quantString);
                            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                                String errMsg = UtilProperties.getMessage(resource_error, "cart.amount_not_positive_number", this.cart.getLocale());
                                errorMsgs.add(errMsg);
                                result = ServiceUtil.returnError(errorMsgs);
                                return result;
                            }
                            item.setSelectedAmount(amount);
                        }
                    } else if (parameterName.startsWith("itemType")) {
                        if (UtilValidate.isNotEmpty(quantString)) {
                            item.setItemType(quantString);
                        }
                    } else {
                        quantity = (BigDecimal) ObjectType.simpleTypeConvert(quantString, "BigDecimal", null, locale);
                        //For quantity we should test if we allow to add decimal quantity for this product an productStore : 
                        // if not and if quantity is in decimal format then return error.
                        if (!ProductWorker.isDecimalQuantityOrderAllowed(delegator, item.getProductId(), cart.getProductStoreId()) && parameterName.startsWith("update")) {
                            BigDecimal remainder = quantity.remainder(BigDecimal.ONE);
                            if (remainder.compareTo(BigDecimal.ZERO) != 0) {
                                String errMsg = UtilProperties.getMessage(resource_error, "cart.addToCart.quantityInDecimalNotAllowed", this.cart.getLocale());
                                errorMsgs.add(errMsg);
                                result = ServiceUtil.returnError(errorMsgs);
                                return result;
                            }
                            quantity = quantity.setScale(0, UtilNumber.getBigDecimalRoundingMode("order.rounding"));
                        }                
                        else {
                            quantity = quantity.setScale(UtilNumber.getBigDecimalScale("order.decimals"), UtilNumber.getBigDecimalRoundingMode("order.rounding"));
                        }
                        if (quantity.compareTo(BigDecimal.ZERO) < 0) {
                            String errMsg = UtilProperties.getMessage(resource_error, "cart.quantity_not_positive_number", this.cart.getLocale());
                            errorMsgs.add(errMsg);
                            result = ServiceUtil.returnError(errorMsgs);
                            return result;
                        }
                    }

                    // perhaps we need to reset the ship groups' before and after dates based on new dates for the items
                    if (parameterName.startsWith("shipAfterDate") || parameterName.startsWith("shipBeforeDate")) {
                        this.cart.setShipGroupShipDatesFromItem(item);
                    }

                    if (parameterName.toUpperCase().startsWith("UPDATE")) {
                        if (quantity.compareTo(BigDecimal.ZERO) == 0) {
                            deleteList.add(item);
                        } else {
                            if (item != null) {
                                try {
                                    oldItemComment = item.getItemComment();
                                    // if, on a purchase order, the quantity has changed, get the new SupplierProduct entity for this quantity level.
                                    if (cart.getOrderType().equals("PURCHASE_ORDER")) {
                                        oldQuantity = item.getQuantity();
                                        if (oldQuantity.compareTo(quantity) != 0) {
                                            // save the old description and price, in case the user wants to change those as well
                                            oldDescription = item.getName();
                                            oldPrice = item.getBasePrice();

                                            if (UtilValidate.isNotEmpty(item.getProductId())) {
                                                GenericValue supplierProduct = this.cart.getSupplierProduct(item.getProductId(), quantity, this.dispatcher);

                                                if (supplierProduct == null) {
                                                    if ("_NA_".equals(cart.getPartyId())) {
                                                        // no supplier does not require the supplier product
                                                        item.setQuantity(quantity, dispatcher, this.cart);
                                                        item.setName(item.getProduct().getString("internalName"));
                                                    } else {
                                                        // in this case, the user wanted to purchase a quantity which is not available (probably below minimum)
                                                        String errMsg = UtilProperties.getMessage(resource_error, "cart.product_not_valid_for_supplier", this.cart.getLocale());
                                                        errMsg = errMsg + " (" + item.getProductId() + ", " + quantity + ", " + cart.getCurrency() + ")";
                                                        errorMsgs.add(errMsg);
                                                    }
                                                } else {
                                                    item.setSupplierProductId(supplierProduct.getString("supplierProductId"));
                                                    item.setQuantity(quantity, dispatcher, this.cart);
                                                    item.setBasePrice(supplierProduct.getBigDecimal("lastPrice"));
                                                    item.setName(ShoppingCartItem.getPurchaseOrderItemDescription(item.getProduct(), supplierProduct, cart.getLocale()));
                                                }
                                            } else {
                                                item.setQuantity(quantity, dispatcher, this.cart);
                                            }
                                        }
                                    } else {
                                        BigDecimal minQuantity = ShoppingCart.getMinimumOrderQuantity(delegator, item.getBasePrice(), item.getProductId());
                                        oldQuantity = item.getQuantity();
                                        if (oldQuantity.compareTo(quantity) != 0) {
                                            cart.setShipmentMethodTypeId(index, null);
                                        }
                                        if (quantity.compareTo(minQuantity) < 0) {
                                            quantity = minQuantity;
                                        }
                                        item.setQuantity(quantity, dispatcher, this.cart, true, false);
                                        cart.setItemShipGroupQty(item, quantity, 0);
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
                    
                    if (parameterName.toUpperCase().startsWith("COMMENT")) {
                      if (!oldItemComment.equals(itemComment)) {
                          if (security.hasEntityPermission("ORDERMGR", "_CREATE", userLogin)) {
                              if (item != null) {
                                  item.setItemComment(itemComment);
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

        for (ShoppingCartItem item : deleteList) {
            int itemIndex = this.cart.getItemIndex(item);

            if (Debug.infoOn())
                Debug.logInfo("Removing item index: " + itemIndex, module);
            try {
                this.cart.removeCartItem(itemIndex, dispatcher);
                cart.setShipmentMethodTypeId(itemIndex, null);
            } catch (CartItemModifyException e) {
                result = ServiceUtil.returnError(new ArrayList<String>());
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

        Map<String, String> fields = UtilMisc.<String, String>toMap("productId", productId, "productFeatureId", featureId);
        if (optionField != null) {
            int featureTypeStartIndex = optionField.indexOf('^') + 1;
            int featureTypeEndIndex = optionField.lastIndexOf('_');
            if (featureTypeStartIndex > 0 && featureTypeEndIndex > 0) {
                fields.put("productFeatureTypeId", optionField.substring(featureTypeStartIndex, featureTypeEndIndex));
            }
        }

        GenericValue productFeatureAppl = null;
        List<GenericValue> features = null;
        try {
            features = EntityQuery.use(delegator).from("ProductFeatureAndAppl").where(fields).orderBy("-fromDate").queryList();
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
    public Map<String, Object> selectAgreement(String agreementId) {
        Map<String, Object> result = null;
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
            agreement = EntityQuery.use(this.delegator).from("Agreement").where("agreementId", agreementId).cache(true).queryOne();
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
                List<GenericValue> agreementItems = agreement.getRelated("AgreementItem", UtilMisc.toMap("agreementItemTypeId", "AGREEMENT_PRICING_PR"), null, false);
                if (agreementItems.size() > 0) {
                    GenericValue agreementItem = agreementItems.get(0);
                    String currencyUomId = (String) agreementItem.get("currencyUomId");
                    if (UtilValidate.isNotEmpty(currencyUomId)) {
                        try {
                            cart.setCurrency(dispatcher,currencyUomId);
                        } catch (CartItemModifyException ex) {
                            result = ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderSetCurrencyError",this.cart.getLocale()) + ex.getMessage());
                            return result;
                        }
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
                 List<GenericValue> agreementTerms = EntityUtil.filterByDate(agreement.getRelated("AgreementTerm", null, null, false));
                 if (agreementTerms.size() > 0) {
                      for (int i = 0; agreementTerms.size() > i;i++) {
                           GenericValue agreementTerm = agreementTerms.get(i);
                           String termTypeId = (String) agreementTerm.get("termTypeId");
                           BigDecimal termValue = agreementTerm.getBigDecimal("termValue");
                           Long termDays = (Long) agreementTerm.get("termDays");
                           String textValue = agreementTerm.getString("textValue");
                           cart.addOrderTerm(termTypeId, termValue, termDays, textValue);
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

    public Map<String, Object> setCurrency(String currencyUomId) {
        Map<String, Object> result = null;

        try {
            this.cart.setCurrency(this.dispatcher,currencyUomId);
            result = ServiceUtil.returnSuccess();
         } catch (CartItemModifyException ex) {
             result = ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderSetCurrencyError",this.cart.getLocale()) + ex.getMessage());
             return result;
         }
        return result;
    }

    public Map<String, Object> addOrderTerm(String termTypeId, BigDecimal termValue, Long termDays) {
        return addOrderTerm(termTypeId, termValue, termDays, null);
    }

    public Map<String, Object> addOrderTerm(String termTypeId, BigDecimal termValue,Long termDays, String textValue) {
        Map<String, Object> result = null;
        this.cart.addOrderTerm(termTypeId,termValue,termDays,textValue);
        result = ServiceUtil.returnSuccess();
        return result;
    }

    public Map<String, Object> removeOrderTerm(int index) {
        Map<String, Object> result = null;
        this.cart.removeOrderTerm(index);
        result = ServiceUtil.returnSuccess();
        return result;
    }

}
