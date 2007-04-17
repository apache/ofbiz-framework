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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javolution.util.FastMap;

import org.apache.commons.collections.set.ListOrderedSet;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilFormatOut;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericPK;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.order.order.OrderReadHelper;
import org.ofbiz.order.shoppingcart.product.ProductPromoWorker;
import org.ofbiz.order.shoppinglist.ShoppingListEvents;
import org.ofbiz.product.catalog.CatalogWorker;
import org.ofbiz.product.category.CategoryWorker;
import org.ofbiz.product.config.ProductConfigWrapper;
import org.ofbiz.product.product.ProductContentWrapper;
import org.ofbiz.product.product.ProductWorker;
import org.ofbiz.product.store.ProductStoreWorker;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ModelService;
import org.ofbiz.service.ServiceUtil;

/**
 * <p><b>Title:</b> ShoppingCartItem.java
 * <p><b>Description:</b> Shopping cart item object.
 */
public class ShoppingCartItem implements java.io.Serializable {

    public static String module = ShoppingCartItem.class.getName();
    public static final String resource = "OrderUiLabels";
    public static final String resource_error = "OrderErrorUiLabels";
    public static String[] attributeNames = { "shoppingListId", "shoppingListItemSeqId", "surveyResponses",
                                              "itemDesiredDeliveryDate", "itemComment"};

    private transient GenericDelegator delegator = null;
    /** the actual or variant product */
    private transient GenericValue _product = null;
    /** the virtual product if _product is a variant */
    private transient GenericValue _parentProduct = null;

    private String delegatorName = null;
    private String prodCatalogId = null;
    private String productId = null;
    private String parentProductId = null;
    private String externalId = null;
    /** ends up in orderItemTypeId */
    private String itemType = null;
    private ShoppingCart.ShoppingCartItemGroup itemGroup = null;
    private String productCategoryId = null;
    private String itemDescription = null;
    /** for reservations: date start*/
    private Timestamp reservStart = null;
    /** for reservations: length */
    private double reservLength = 0;
    /** for reservations: number of persons using */
    private double reservPersons = 0;
    private double quantity = 0.0;
    private double basePrice = 0.0;
    private Double displayPrice = null;
    private Double recurringBasePrice = null;
    private Double recurringDisplayPrice = null;
    /** comes from price calc, used for special promo price promotion action */
    private Double specialPromoPrice = null;
    /** for reservations: extra % 2nd person */
    private double reserv2ndPPPerc = 0.0;
    /** for reservations: extra % Nth person */
    private double reservNthPPPerc = 0.0;
    private double listPrice = 0.0;
    /** flag to know if the price have been modified */
    private boolean isModifiedPrice = false;
    private double selectedAmount = 0.0;
    private String requirementId = null;
    private String quoteId = null;
    private String quoteItemSeqId = null;
    // The following three optional fields are used to collect information for the OrderItemAssoc entity 
    private String associatedOrderId = null; // the order Id, if any, to which the given item is associated (typically a sales order item can be associated to a purchase order item, for example in drop shipments)
    private String associatedOrderItemSeqId = null; // the order item Id, if any, to which the given item is associated
    private String orderItemAssocTypeId = "PURCHASE_ORDER"; // the type of association between this item and an external item; by default, for backward compatibility, a PURCHASE association is used (i.e. the extarnal order is a sales order and this item is a purchase order item created to fulfill the sales order item

    private String statusId = null;
    private Map orderItemAttributes = null;
    private Map attributes = null;
    private String orderItemSeqId = null;
    private Locale locale = null;
    private Timestamp shipBeforeDate = null;
    private Timestamp shipAfterDate = null;

    private Map contactMechIdsMap = FastMap.newInstance();
    private List orderItemPriceInfos = null;
    private List itemAdjustments = new LinkedList();
    private boolean isPromo = false;
    private double promoQuantityUsed = 0;
    private Map quantityUsedPerPromoCandidate = new HashMap();
    private Map quantityUsedPerPromoFailed = new HashMap();
    private Map quantityUsedPerPromoActual = new HashMap();
    private Map additionalProductFeatureAndAppls = new HashMap();
    private List alternativeOptionProductIds = null;
    private ProductConfigWrapper configWrapper = null;
    private List featuresForSupplier = new LinkedList();

    /**
     * Makes a ShoppingCartItem for a purchase order item and adds it to the cart.
     * NOTE: This method will get the product entity and check to make sure it can be purchased.
     *
     * @param cartLocation The location to place this item; null will place at the end
     * @param productId The primary key of the product being added
     * @param quantity The quantity to add
     * @param additionalProductFeatureAndAppls Product feature/appls map
     * @param attributes All unique attributes for this item (NOT features)
     * @param prodCatalogId The catalog this item was added from
     * @param configWrapper The product configuration wrapper (null if the product is not configurable)
     * @param dispatcher LocalDispatcher object for doing promotions, etc
     * @param cart The parent shopping cart object this item will belong to
     * @param supplierProduct GenericValue of SupplierProduct entity, containing product description and prices
     * @param shipBeforeDate Request that the shipment be made before this date
     * @param shipAfterDate Request that the shipment be made after this date
     * @return a new ShoppingCartItem object
     * @throws CartItemModifyException
     */
    public static ShoppingCartItem makePurchaseOrderItem(Integer cartLocation, String productId, Double selectedAmountDbl, double quantity, 
            Map additionalProductFeatureAndAppls, Map attributes, String prodCatalogId, ProductConfigWrapper configWrapper, String itemType, ShoppingCart.ShoppingCartItemGroup itemGroup, 
            LocalDispatcher dispatcher, ShoppingCart cart, GenericValue supplierProduct, Timestamp shipBeforeDate, Timestamp shipAfterDate) 
                throws CartItemModifyException, ItemNotFoundException {
        GenericDelegator delegator = cart.getDelegator();
        GenericValue product = null;

        try {
            product = delegator.findByPrimaryKeyCache("Product", UtilMisc.toMap("productId", productId));
        } catch (GenericEntityException e) {
            Debug.logWarning(e.toString(), module);
            product = null;
        }

        if (product == null) {
            Map messageMap = UtilMisc.toMap("productId", productId );

            String excMsg = UtilProperties.getMessage(resource, "item.product_not_found",
                                          messageMap , cart.getLocale() );

            Debug.logWarning(excMsg, module);
            throw new ItemNotFoundException(excMsg);
        }
        ShoppingCartItem newItem = new ShoppingCartItem(product, additionalProductFeatureAndAppls, attributes, prodCatalogId, configWrapper, cart.getLocale(), itemType, itemGroup, null);

        // check to see if product is virtual
        if ("Y".equals(product.getString("isVirtual"))) {
            Map messageMap = UtilMisc.toMap("productName", product.getString("productName"), 
                                            "productId", product.getString("productId"));            

            String excMsg = UtilProperties.getMessage(resource, "item.cannot_add_product_virtual",
                                          messageMap , cart.getLocale() );
                
            Debug.logWarning(excMsg, module);
            throw new CartItemModifyException(excMsg);
        }

        // Timestamp nowTimestamp = UtilDateTime.nowTimestamp();

        // check to see if the product is fully configured
        if ("AGGREGATED".equals(product.getString("productTypeId"))) {
            if (configWrapper == null || !configWrapper.isCompleted()) {
                Map messageMap = UtilMisc.toMap("productName", product.getString("productName"), 
                                                "productId", product.getString("productId"));            

                String excMsg = UtilProperties.getMessage(resource, "item.cannot_add_product_not_configured_correctly",
                                              messageMap , cart.getLocale() );
                
                Debug.logWarning(excMsg, module);
                throw new CartItemModifyException(excMsg);
            }
        }

        // add to cart before setting quantity so that we can get order total, etc
        if (cartLocation == null) {
            cart.addItemToEnd(newItem);
        } else {
            cart.addItem(cartLocation.intValue(), newItem);
        }

        if (selectedAmountDbl != null) {
            newItem.setSelectedAmount(selectedAmountDbl.doubleValue());
        }
        
        // set the ship before/after dates.  this needs to happen before setQuantity because setQuantity causes the ship group's dates to be
        // checked versus the cart item's
        newItem.setShipBeforeDate(shipBeforeDate != null ? shipBeforeDate : cart.getDefaultShipBeforeDate());
        newItem.setShipAfterDate(shipAfterDate != null ? shipAfterDate : cart.getDefaultShipAfterDate());

        try {
            newItem.setQuantity(quantity, dispatcher, cart, true);
        } catch (CartItemModifyException e) {
            cart.removeCartItem(cart.getItemIndex(newItem), dispatcher);
            cart.clearItemShipInfo(newItem);
            cart.removeEmptyCartItems();
            throw e;
        }

        // specific for purchase orders - description is set to supplierProductId + supplierProductName, price set to lastPrice of SupplierProduct
        // if supplierProduct has no supplierProductName, use the regular supplierProductId
        if (supplierProduct != null) {
            newItem.setName(getPurchaseOrderItemDescription(product, supplierProduct, cart.getLocale()));
            newItem.setBasePrice(supplierProduct.getDouble("lastPrice").doubleValue());
        } else {
            newItem.setName(product.getString("internalName"));
        }
        return newItem;

    }

    /**
     * Makes a ShoppingCartItem and adds it to the cart.
     * NOTE: This method will get the product entity and check to make sure it can be purchased.
     *
     * @param cartLocation The location to place this item; null will place at the end
     * @param productId The primary key of the product being added
     * @param selectedAmountDbl Optional. Defaults to 0.0. If a selectedAmount is needed (complements the quantity value), pass it in here.
     * @param quantity Required. The quantity to add.
     * @param unitPriceDbl Optional. Defaults to 0.0, which causes calculation of price.
     * @param reservStart Optional. The start of the reservation.
     * @param reservLengthDbl Optional. The length of the reservation.
     * @param reservPersonsDbl Optional. The number of persons taking advantage of the reservation.
     * @param shipBeforeDate Optional. The date to ship the order by.
     * @param shipAfterDate Optional. Wait until this date to ship.
     * @param additionalProductFeatureAndAppls Optional. Product feature/appls map.
     * @param attributes Optional. All unique attributes for this item (NOT features).
     * @param prodCatalogId Optional, but strongly recommended. The catalog this item was added from.
     * @param configWrapper Optional. The product configuration wrapper (null if the product is not configurable).
     * @param itemType Optional. Specifies the type of cart item, corresponds to an OrderItemType and should be a valid orderItemTypeId.
     * @param itemGroup Optional. Specifies which item group in the cart this should belong to, if item groups are needed/desired.
     * @param dispatcher Required (for price calculation, promos, etc). LocalDispatcher object for doing promotions, etc.
     * @param cart Required. The parent shopping cart object this item will belong to.
     * @param triggerExternalOpsBool Optional. Defaults to true. Trigger external operations (like promotions and such)?
     * @param triggerPriceRulesBool Optional. Defaults to true. Trigger the price rules to calculate the price for this item?
     * 
     * @return a new ShoppingCartItem object
     * @throws CartItemModifyException
     */
    public static ShoppingCartItem makeItem(Integer cartLocation, String productId, Double selectedAmountDbl, double quantity, Double unitPriceDbl, 
            Timestamp reservStart, Double reservLengthDbl, Double reservPersonsDbl, Timestamp shipBeforeDate, Timestamp shipAfterDate, 
            Map additionalProductFeatureAndAppls, Map attributes, String prodCatalogId, ProductConfigWrapper configWrapper, 
            String itemType, ShoppingCart.ShoppingCartItemGroup itemGroup, LocalDispatcher dispatcher, ShoppingCart cart, Boolean triggerExternalOpsBool, Boolean triggerPriceRulesBool, String parentProductId, Boolean skipInventoryChecks, Boolean skipProductChecks) 
            throws CartItemModifyException, ItemNotFoundException {
        GenericDelegator delegator = cart.getDelegator();
        GenericValue product = null;
        GenericValue parentProduct = null;
        
        try {
            product = delegator.findByPrimaryKeyCache("Product", UtilMisc.toMap("productId", productId));
    
            // first see if there is a purchase allow category and if this product is in it or not
            String purchaseProductCategoryId = CatalogWorker.getCatalogPurchaseAllowCategoryId(delegator, prodCatalogId);
            if (product != null && purchaseProductCategoryId != null) {
                if (!CategoryWorker.isProductInCategory(delegator, product.getString("productId"), purchaseProductCategoryId)) {
                    // a Purchase allow productCategoryId was found, but the product is not in the category, axe it...
                    product = null;
                }
            }
        } catch (GenericEntityException e) {
            Debug.logWarning(e.toString(), module);
            product = null;
        }
    
        if (product == null) {
            Map messageMap = UtilMisc.toMap("productId", productId );
            String excMsg = UtilProperties.getMessage(resource, "item.product_not_found", messageMap , cart.getLocale() );
    
            Debug.logWarning(excMsg, module);
            throw new ItemNotFoundException(excMsg);
        }

        if (parentProductId != null)
        {
            try 
            {
                parentProduct = delegator.findByPrimaryKeyCache("Product", UtilMisc.toMap("productId", parentProductId));
            } catch (GenericEntityException e) {
                Debug.logWarning(e.toString(), module);
                parentProduct = null;
            }
        }
        return makeItem(cartLocation, product, selectedAmountDbl, quantity, unitPriceDbl, 
                reservStart, reservLengthDbl, reservPersonsDbl, shipBeforeDate, shipAfterDate, 
                additionalProductFeatureAndAppls, attributes, prodCatalogId, configWrapper, 
                itemType, itemGroup, dispatcher, cart, triggerExternalOpsBool, triggerPriceRulesBool, parentProduct, skipInventoryChecks, skipProductChecks);
    }

    /**
     * Makes a ShoppingCartItem and adds it to the cart.
     * WARNING: This method does not check if the product is in a purchase category.
     * rental fields were added.
     *
     * @param cartLocation The location to place this item; null will place at the end
     * @param product The product entity relating to the product being added
     * @param selectedAmountDbl Optional. Defaults to 0.0. If a selectedAmount is needed (complements the quantity value), pass it in here.
     * @param quantity Required. The quantity to add.
     * @param unitPriceDbl Optional. Defaults to 0.0, which causes calculation of price.
     * @param reservStart Optional. The start of the reservation.
     * @param reservLengthDbl Optional. The length of the reservation.
     * @param reservPersonsDbl Optional. The number of persons taking advantage of the reservation.
     * @param shipBeforeDate Optional. The date to ship the order by.
     * @param shipAfterDate Optional. Wait until this date to ship.
     * @param additionalProductFeatureAndAppls Optional. Product feature/appls map.
     * @param attributes Optional. All unique attributes for this item (NOT features).
     * @param prodCatalogId Optional, but strongly recommended. The catalog this item was added from.
     * @param configWrapper Optional. The product configuration wrapper (null if the product is not configurable).
     * @param itemType Optional. Specifies the type of cart item, corresponds to an OrderItemType and should be a valid orderItemTypeId.
     * @param itemGroup Optional. Specifies which item group in the cart this should belong to, if item groups are needed/desired.
     * @param dispatcher Required (for price calculation, promos, etc). LocalDispatcher object for doing promotions, etc.
     * @param cart Required. The parent shopping cart object this item will belong to.
     * @param triggerExternalOpsBool Optional. Defaults to true. Trigger external operations (like promotions and such)?
     * @param triggerPriceRulesBool Optional. Defaults to true. Trigger the price rules to calculate the price for this item?
     * 
     * @return a new ShoppingCartItem object
     * @throws CartItemModifyException
     */
    public static ShoppingCartItem makeItem(Integer cartLocation, GenericValue product, Double selectedAmountDbl, 
            double quantity, Double unitPriceDbl, Timestamp reservStart, Double reservLengthDbl, Double reservPersonsDbl, 
            Timestamp shipBeforeDate, Timestamp shipAfterDate, Map additionalProductFeatureAndAppls, Map attributes, 
            String prodCatalogId, ProductConfigWrapper configWrapper, String itemType, ShoppingCart.ShoppingCartItemGroup itemGroup, LocalDispatcher dispatcher, 
            ShoppingCart cart, Boolean triggerExternalOpsBool, Boolean triggerPriceRulesBool, GenericValue parentProduct, Boolean skipInventoryChecks, Boolean skipProductChecks) throws CartItemModifyException {

        ShoppingCartItem newItem = new ShoppingCartItem(product, additionalProductFeatureAndAppls, attributes, prodCatalogId, configWrapper, cart.getLocale(), itemType, itemGroup, parentProduct);

        double selectedAmount = selectedAmountDbl == null ? 0.0 : selectedAmountDbl.doubleValue();
        double unitPrice = unitPriceDbl == null ? 0.0 : unitPriceDbl.doubleValue();
        double reservLength = reservLengthDbl == null ? 0.0 : reservLengthDbl.doubleValue();
        double reservPersons = reservPersonsDbl == null ? 0.0 : reservPersonsDbl.doubleValue();
        boolean triggerPriceRules = triggerPriceRulesBool == null ? true : triggerPriceRulesBool.booleanValue();
        boolean triggerExternalOps = triggerExternalOpsBool == null ? true : triggerExternalOpsBool.booleanValue();
    
        // check to see if product is virtual
        if ("Y".equals(product.getString("isVirtual"))) {            
            Map messageMap = UtilMisc.toMap("productName", product.getString("productName"), 
                                            "productId", product.getString("productId"));            

            String excMsg = UtilProperties.getMessage(resource, "item.cannot_add_product_virtual",
                                          messageMap , cart.getLocale() );

            Debug.logWarning(excMsg, module);
            throw new CartItemModifyException(excMsg);
        }

        java.sql.Timestamp nowTimestamp = UtilDateTime.nowTimestamp();
        
        if (!skipProductChecks.booleanValue()) {
            // check to see if introductionDate hasn't passed yet
            if (product.get("introductionDate") != null && nowTimestamp.before(product.getTimestamp("introductionDate"))) {
                Map messageMap = UtilMisc.toMap("productName", product.getString("productName"), 
                                                "productId", product.getString("productId"));            

                String excMsg = UtilProperties.getMessage(resource, "item.cannot_add_product_not_yet_available",
                                              messageMap , cart.getLocale() );            

                Debug.logWarning(excMsg, module);
                throw new CartItemModifyException(excMsg);
            }

            // check to see if salesDiscontinuationDate has passed
            if (product.get("salesDiscontinuationDate") != null && nowTimestamp.after(product.getTimestamp("salesDiscontinuationDate"))) {
                Map messageMap = UtilMisc.toMap("productName", product.getString("productName"), 
                                                "productId", product.getString("productId"));

                String excMsg = UtilProperties.getMessage(resource, "item.cannot_add_product_no_longer_available",
                                              messageMap , cart.getLocale() );

                Debug.logWarning(excMsg, module);
                throw new CartItemModifyException(excMsg);
            }
            /*
            if (product.get("salesDiscWhenNotAvail") != null && "Y".equals(product.getString("salesDiscWhenNotAvail"))) {
                // check atp and if <= 0 then the product is no more available because
                // all the units in warehouse are reserved by other sales orders and no new purchase orders will be done 
                // for this product.
                if (!newItem.isInventoryAvailableOrNotRequired(quantity, cart.getProductStoreId(), dispatcher)) {
                    Map messageMap = UtilMisc.toMap("productName", product.getString("productName"), 
                                                    "productId", product.getString("productId"));

                    String excMsg = UtilProperties.getMessage(resource, "item.cannot_add_product_no_longer_available",
                                                  messageMap , cart.getLocale() );

                    Debug.logWarning(excMsg, module);
                    throw new CartItemModifyException(excMsg);
                }
            }
             */
        }
        // check to see if the product is a rental item
        if ("ASSET_USAGE".equals(product.getString("productTypeId"))) {
            if (reservStart == null)    {
                String excMsg = UtilProperties.getMessage(resource, "item.missing_reservation_starting_date",
                                              cart.getLocale() );                
                throw new CartItemModifyException(excMsg);
            }

            if (reservStart.before(UtilDateTime.nowTimestamp()))    {
                String excMsg = UtilProperties.getMessage(resource, "item.reservation_from_tomorrow",
                                              cart.getLocale() );
                throw new CartItemModifyException(excMsg);
            }
            newItem.setReservStart(reservStart);

            if (reservLength < 1)    {
                String excMsg = UtilProperties.getMessage(resource, "item.number_of_days",
                                              cart.getLocale() );                
                throw new CartItemModifyException(excMsg);
            }
            newItem.setReservLength(reservLength);
            
            if (product.get("reservMaxPersons") != null) {
                 double reservMaxPersons = product.getDouble("reservMaxPersons").doubleValue();
                 if (reservMaxPersons < reservPersons)    {
                     Map messageMap = UtilMisc.toMap("reservMaxPersons", product.getString("reservMaxPersons"), 
                                                     "reservPersons", (new Double(reservPersons)).toString());
                     String excMsg = UtilProperties.getMessage(resource, "item.maximum_number_of_person_renting",
                                                   messageMap, cart.getLocale() );
                     
                     Debug.logInfo(excMsg,module);
                     throw new CartItemModifyException(excMsg);
                 }
             }
             newItem.setReservPersons(reservPersons);

             if (product.get("reserv2ndPPPerc") != null)
                 newItem.setReserv2ndPPPerc(product.getDouble("reserv2ndPPPerc").doubleValue());

             if (product.get("reservNthPPPerc") != null)
                 newItem.setReservNthPPPerc(product.getDouble("reservNthPPPerc").doubleValue());

            // check to see if the related fixed asset is available for rent
            String isAvailable = checkAvailability(product.getString("productId"), quantity, reservStart, reservLength, cart);
            if(isAvailable.compareTo("OK") != 0) {
                Map messageMap = UtilMisc.toMap("productId", product.getString("productId"), 
                                                "availableMessage", isAvailable);
                String excMsg = UtilProperties.getMessage(resource, "item.product_not_available",
                                              messageMap, cart.getLocale() );                
                Debug.logInfo(excMsg, module);
                throw new CartItemModifyException(isAvailable);
            }
        }
        
        // check to see if the product is fully configured
        if ("AGGREGATED".equals(product.getString("productTypeId"))) {
            if (configWrapper == null || !configWrapper.isCompleted()) {                
                Map messageMap = UtilMisc.toMap("productName", product.getString("productName"), 
                                                "productId", product.getString("productId"));
                String excMsg = UtilProperties.getMessage(resource, "item.cannot_add_product_not_configured_correctly",
                                              messageMap , cart.getLocale() );
                Debug.logWarning(excMsg, module);
                throw new CartItemModifyException(excMsg);
            }
        }
        
        // set the ship before and after dates (defaults to cart ship before/after dates)
        newItem.setShipBeforeDate(shipBeforeDate != null ? shipBeforeDate : cart.getDefaultShipBeforeDate());
        newItem.setShipAfterDate(shipAfterDate != null ? shipAfterDate : cart.getDefaultShipAfterDate());
        
        // set the product unit price as base price
        // if triggerPriceRules is true this price will be overriden
        newItem.setBasePrice(unitPrice);

        // add to cart before setting quantity so that we can get order total, etc
        if (cartLocation == null) {
            cart.addItemToEnd(newItem);
        } else {
            cart.addItem(cartLocation.intValue(), newItem);
        }

        // We have to set the selectedAmount before calling setQuantity because
        // selectedAmount changes the item's base price (used in the updatePrice
        // method called inside the setQuantity method)
        if (selectedAmount > 0) {
            newItem.setSelectedAmount(selectedAmount);
        }

        try {
            newItem.setQuantity((int)quantity, dispatcher, cart, triggerExternalOps, true, triggerPriceRules, skipInventoryChecks.booleanValue());
        } catch (CartItemModifyException e) {
            Debug.logWarning(e.getMessage(), module);
            cart.removeCartItem(cart.getItemIndex(newItem), dispatcher);
            cart.clearItemShipInfo(newItem);
            cart.removeEmptyCartItems();
            throw e;
        }

        return newItem;
    }

    /**
     * Makes a non-product ShoppingCartItem and adds it to the cart.
     * NOTE: This is only for non-product items; items without a product entity (work items, bulk items, etc)
     *
     * @param cartLocation The location to place this item; null will place at the end
     * @param itemType The OrderItemTypeId for the item being added
     * @param itemDescription The optional description of the item
     * @param productCategoryId The optional category the product *will* go in
     * @param basePrice The price for this item
     * @param selectedAmount
     * @param quantity The quantity to add
     * @param attributes All unique attributes for this item (NOT features)
     * @param prodCatalogId The catalog this item was added from
     * @param dispatcher LocalDispatcher object for doing promotions, etc
     * @param cart The parent shopping cart object this item will belong to
     * @param triggerExternalOpsBool Indicates if we should run external operations (promotions, auto-save, etc)
     * @return a new ShoppingCartItem object
     * @throws CartItemModifyException
     */
    public static ShoppingCartItem makeItem(Integer cartLocation, String itemType, String itemDescription, String productCategoryId, 
            Double basePrice, Double selectedAmount, double quantity, Map attributes, String prodCatalogId, ShoppingCart.ShoppingCartItemGroup itemGroup, 
            LocalDispatcher dispatcher, ShoppingCart cart, Boolean triggerExternalOpsBool) throws CartItemModifyException {

        GenericDelegator delegator = cart.getDelegator();
        ShoppingCartItem newItem = new ShoppingCartItem(delegator, itemType, itemDescription, productCategoryId, basePrice, attributes, prodCatalogId, cart.getLocale(), itemGroup);

        // add to cart before setting quantity so that we can get order total, etc
        if (cartLocation == null) {
            cart.addItemToEnd(newItem);
        } else {
            cart.addItem(cartLocation.intValue(), newItem);
        }
        
        boolean triggerExternalOps = triggerExternalOpsBool == null ? true : triggerExternalOpsBool.booleanValue();

        try {
            newItem.setQuantity(quantity, dispatcher, cart, triggerExternalOps);
        } catch (CartItemModifyException e) {
            cart.removeEmptyCartItems();
            throw e;
        }

        if (selectedAmount != null) {
            newItem.setSelectedAmount(selectedAmount.doubleValue());
        }
        return newItem;
    }

    /** Clone an item. */
    public ShoppingCartItem(ShoppingCartItem item) {
        try {
            this._product = item.getProduct();
        } catch (IllegalStateException e) {
            this._product = null;
        }
        this.delegator = item.getDelegator();
        this.delegatorName = item.delegatorName;
        this.prodCatalogId = item.getProdCatalogId();
        this.productId = item.getProductId();
        this.itemType = item.getItemType();
        this.itemGroup = item.getItemGroup();
        this.productCategoryId = item.getProductCategoryId();
        this.quantity = item.getQuantity();
        this.reservStart = item.getReservStart();
        this.reservLength = item.getReservLength();
        this.reservPersons = item.getReservPersons();
        this.selectedAmount = item.getSelectedAmount();
        this.setBasePrice(item.getBasePrice());
        this.setDisplayPrice(item.getDisplayPrice());
        this.setRecurringBasePrice(item.getRecurringBasePrice());
        this.setRecurringDisplayPrice(item.getRecurringDisplayPrice());
        this.listPrice = item.getListPrice();
        this.reserv2ndPPPerc = item.getReserv2ndPPPerc();
        this.reservNthPPPerc = item.getReservNthPPPerc();
        this.requirementId = item.getRequirementId();
        this.quoteId = item.getQuoteId();
        this.quoteItemSeqId = item.getQuoteItemSeqId();
        this.associatedOrderId = item.getAssociatedOrderId();
        this.associatedOrderItemSeqId = item.getAssociatedOrderItemSeqId();
        this.orderItemAssocTypeId = item.getOrderItemAssocTypeId();
        this.isPromo = item.getIsPromo();
        this.promoQuantityUsed = item.promoQuantityUsed;
        this.locale = item.locale;
        this.quantityUsedPerPromoCandidate = new HashMap(item.quantityUsedPerPromoCandidate);
        this.quantityUsedPerPromoFailed = new HashMap(item.quantityUsedPerPromoFailed);
        this.quantityUsedPerPromoActual = new HashMap(item.quantityUsedPerPromoActual);
        this.orderItemSeqId = item.getOrderItemSeqId();
        this.additionalProductFeatureAndAppls = item.getAdditionalProductFeatureAndAppls() == null ?
                null : new HashMap(item.getAdditionalProductFeatureAndAppls());
        this.attributes = item.getAttributes() == null ? new HashMap() : new HashMap(item.getAttributes());
        this.contactMechIdsMap = item.getOrderItemContactMechIds() == null ? null : new HashMap(item.getOrderItemContactMechIds());
        this.orderItemPriceInfos = item.getOrderItemPriceInfos() == null ? null : new LinkedList(item.getOrderItemPriceInfos());
        this.itemAdjustments = item.getAdjustments() == null ? null : new LinkedList(item.getAdjustments());
        if (this._product == null) {
            this.itemDescription = item.getName();
        }
        if (item.configWrapper != null) {
            this.configWrapper = new ProductConfigWrapper(item.configWrapper);
        }
    }

    /** Cannot create shopping cart item with no parameters */
    protected ShoppingCartItem() {}

    /** Creates new ShoppingCartItem object. */
    protected ShoppingCartItem(GenericValue product, Map additionalProductFeatureAndAppls, Map attributes, String prodCatalogId, Locale locale, String itemType, ShoppingCart.ShoppingCartItemGroup itemGroup) {
        this(product, additionalProductFeatureAndAppls, attributes, prodCatalogId, null, locale, itemType, itemGroup, null);
         if (product != null) {
            String productName = ProductContentWrapper.getProductContentAsText(product, "PRODUCT_NAME", this.locale, null);
            // if the productName is null or empty, see if there is an associated virtual product and get the productName of that product
            if (UtilValidate.isEmpty(productName)) {
                GenericValue parentProduct = this.getParentProduct();
                if (parentProduct != null) {
                    productName = ProductContentWrapper.getProductContentAsText(parentProduct, "PRODUCT_NAME", this.locale, null);
                }
            }

            if (productName == null) {
                this.itemDescription= "";
            } else {
                this.itemDescription= productName;
            }
        }
    }

    /** Creates new ShoppingCartItem object. */
    protected ShoppingCartItem(GenericValue product, Map additionalProductFeatureAndAppls, Map attributes, String prodCatalogId, ProductConfigWrapper configWrapper, Locale locale, String itemType, ShoppingCart.ShoppingCartItemGroup itemGroup, GenericValue parentProduct) {
        this._product = product;
        this.productId = _product.getString("productId");
        this._parentProduct = parentProduct;
        if (parentProduct != null)
            this.parentProductId = _parentProduct.getString("productId");
        if (UtilValidate.isEmpty(itemType)) {
            if (_product.getString("productTypeId").equals("ASSET_USAGE")) {
                this.itemType = "RENTAL_ORDER_ITEM";  // will create additional workeffort/asset usage records
            } else {
                this.itemType = "PRODUCT_ORDER_ITEM";
            }
        } else {
            this.itemType = itemType;
        }
        this.itemGroup = itemGroup;
        this.prodCatalogId = prodCatalogId;
        this.attributes = (attributes == null? FastMap.newInstance(): attributes);
        this.delegator = _product.getDelegator();
        this.delegatorName = _product.getDelegator().getDelegatorName();
        this.addAllProductFeatureAndAppls(additionalProductFeatureAndAppls);
        this.locale = locale;
        this.configWrapper = configWrapper;
    }

    /** Creates new ShopingCartItem object. */
    protected ShoppingCartItem(GenericDelegator delegator, String itemTypeId, String description, String categoryId, Double basePrice, Map attributes, String prodCatalogId, Locale locale, ShoppingCart.ShoppingCartItemGroup itemGroup) {
        this.delegator = delegator;
        this.itemType = itemTypeId;
        this.itemGroup = itemGroup;
        this.itemDescription = description;
        this.productCategoryId = categoryId;
        if (basePrice != null) {
            this.setBasePrice(basePrice.doubleValue());
            this.setDisplayPrice(basePrice.doubleValue());
        }
        this.attributes = (attributes == null? FastMap.newInstance(): attributes);
        this.prodCatalogId = prodCatalogId;
        this.delegatorName = delegator.getDelegatorName();
        this.locale = locale;
    }

    public String getProdCatalogId() {
        return this.prodCatalogId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public String getExternalId() {
        return this.externalId;
    }
    
    /** Sets the user selected amount */
    public void setSelectedAmount(double selectedAmount) {
        this.selectedAmount = selectedAmount;
    }

    /** Returns the user selected amount */
    public double getSelectedAmount() {
        return this.selectedAmount;
    }

    /** Sets the base price for the item; use with caution */
    public void setBasePrice(double basePrice) {
        this.basePrice = basePrice;
    }

    /** Sets the display price for the item; use with caution */
    public void setDisplayPrice(double displayPrice) {
        this.displayPrice = new Double(displayPrice);
    }

    /** Sets the base price for the item; use with caution */
    public void setRecurringBasePrice(Double recurringBasePrice) {
        this.recurringBasePrice = recurringBasePrice;
    }

    /** Sets the display price for the item; use with caution */
    public void setRecurringDisplayPrice(Double recurringDisplayPrice) {
        this.recurringDisplayPrice = recurringDisplayPrice;
    }

    public void setSpecialPromoPrice(Double specialPromoPrice) {
        this.specialPromoPrice = specialPromoPrice;
    }

    /** Sets the extra % for second person */
    public void setReserv2ndPPPerc(double reserv2ndPPPerc) {
        this.reserv2ndPPPerc = reserv2ndPPPerc;
    }
    /** Sets the extra % for third and following person */
    public void setReservNthPPPerc(double reservNthPPPerc) {
        this.reservNthPPPerc = reservNthPPPerc;
    }
    /** Sets the reservation start date */
    public void setReservStart(Timestamp reservStart)    {
        this.reservStart = reservStart;
    }
    /** Sets the reservation length */
    public void setReservLength(double reservLength)    {
        this.reservLength = reservLength;
    }
    /** Sets number of persons using the reservation */
    public void setReservPersons(double reservPersons)    {
        this.reservPersons = reservPersons;
    }

    /** Sets the quantity for the item and validates the change in quantity, etc */
    public void setQuantity(double quantity, LocalDispatcher dispatcher, ShoppingCart cart) throws CartItemModifyException {
        this.setQuantity(quantity, dispatcher, cart, true);
    }

    /** Sets the quantity for the item and validates the change in quantity, etc */
    public void setQuantity(double quantity, LocalDispatcher dispatcher, ShoppingCart cart, boolean triggerExternalOps) throws CartItemModifyException {
        this.setQuantity(quantity, dispatcher, cart, triggerExternalOps, true);
    }

    /** Sets the quantity for the item and validates the change in quantity, etc */
    public void setQuantity(double quantity, LocalDispatcher dispatcher, ShoppingCart cart, boolean triggerExternalOps, boolean resetShipGroup) throws CartItemModifyException {
        this.setQuantity((int) quantity, dispatcher, cart, triggerExternalOps, resetShipGroup, true, false);
    }

    /** Sets the quantity for the item and validates the change in quantity, etc */
    public void setQuantity(double quantity, LocalDispatcher dispatcher, ShoppingCart cart, boolean triggerExternalOps, boolean resetShipGroup, boolean updateProductPrice) throws CartItemModifyException {
        this.setQuantity((int) quantity, dispatcher, cart, triggerExternalOps, resetShipGroup, updateProductPrice, false);
    }

    /** returns "OK" when the product can be booked or returns a string with the dates the related fixed Asset is not available */
    public static String checkAvailability(String productId, double quantity, Timestamp reservStart, double reservLength, ShoppingCart cart) {
        GenericDelegator delegator = cart.getDelegator();
        // find related fixedAsset
        List selFixedAssetProduct = null;
        GenericValue fixedAssetProduct = null;
        try {
            List allFixedAssetProduct = delegator.findByAnd("FixedAssetProduct", UtilMisc.toMap("productId", productId, "fixedAssetProductTypeId", "FAPT_USE"));
            selFixedAssetProduct = EntityUtil.filterByDate(allFixedAssetProduct, UtilDateTime.nowTimestamp(), "fromDate", "thruDate", true);
        } catch (GenericEntityException e) {
            Map messageMap = UtilMisc.toMap("productId", productId);
            String msg = UtilProperties.getMessage(resource, "item.cannot_find_Fixed_Asset", messageMap , cart.getLocale());                                       
            return msg;
        }
        if (selFixedAssetProduct != null && selFixedAssetProduct.size() > 0) {
            Iterator firstOne = selFixedAssetProduct.iterator();
            fixedAssetProduct = (GenericValue) firstOne.next();
        } else {
            Map messageMap = UtilMisc.toMap("productId", productId);
            String msg = UtilProperties.getMessage(resource, "item.cannot_find_Fixed_Asset", messageMap , cart.getLocale());
            return msg;
        }

        // find the fixed asset itself
        GenericValue fixedAsset = null;
        try {
            fixedAsset = fixedAssetProduct.getRelatedOne("FixedAsset");
        } catch (GenericEntityException e) {
            Map messageMap = UtilMisc.toMap("fixedAssetId", fixedAssetProduct.getString("fixedAssetId"));
            String msg = UtilProperties.getMessage(resource, "item.fixed_Asset_not_found", messageMap , cart.getLocale());
            return msg;
        }
        if (fixedAsset == null) {
            Map messageMap = UtilMisc.toMap("fixedAssetId", fixedAssetProduct.getString("fixedAssetId"));
            String msg = UtilProperties.getMessage(resource, "item.fixed_Asset_not_found", messageMap , cart.getLocale());
            return msg;            
        }
        //Debug.logInfo("Checking availability for product: " + productId.toString() + " and related FixedAsset: " + fixedAssetProduct.getString("fixedAssetId"),module);

        // see if this fixed asset has a calendar, when no create one and attach to fixed asset
        // DEJ20050725 this isn't being used anywhere, commenting out for now and not assigning from the getRelatedOne: GenericValue techDataCalendar = null;
        try {
            fixedAsset.getRelatedOne("TechDataCalendar");
        } catch (GenericEntityException e) {
            // no calendar ok, when not more that total capacity
            if (fixedAsset.getDouble("productionCapacity").doubleValue() >= quantity) {
                String msg = UtilProperties.getMessage(resource, "item.availableOk", cart.getLocale());
                return msg;
            } else {
                Map messageMap = UtilMisc.toMap("quantityReq", (new Double(quantity)).toString(),
                                                "quantityAvail", fixedAsset.getString("productionCapacity"));
                String msg = UtilProperties.getMessage(resource, "item.availableQnt", messageMap , cart.getLocale());
                return msg;
            }
        }
        // now find all the dates and check the availabilty for each date
        // please note that calendarId is the same for (TechData)Calendar, CalendarExcDay and CalendarExWeek
        long dayCount = 0;
        String resultMessage = "";
        while (dayCount < (long) reservLength) {
            GenericValue techDataCalendarExcDay = null;
            // find an existing Day exception record
            Timestamp exceptionDateStartTime = new Timestamp((long) (reservStart.getTime() + (dayCount++ * 86400000)));
            try {
                techDataCalendarExcDay = delegator.findByPrimaryKey("TechDataCalendarExcDay",
                        UtilMisc.toMap("calendarId", fixedAsset.get("calendarId"), "exceptionDateStartTime", exceptionDateStartTime));
            } catch (GenericEntityException e) {
                if (fixedAsset.get("productionCapacity") != null) {
                    //Debug.logInfo(" No exception day record found, available: " + fixedAsset.getString("productionCapacity") + " Requested now: " + quantity, module);
                    if (fixedAsset.getDouble("productionCapacity").doubleValue() < quantity)
                        resultMessage = resultMessage.concat(exceptionDateStartTime.toString().substring(0, 10) + ", ");
                }
            }
            if (techDataCalendarExcDay != null) {
                // see if we can get the number of assets available
                // first try techDataCalendarExcDay(exceptionCapacity) and then FixedAsset(productionCapacity)
                // if still zero, do not check availability
                double exceptionCapacity = 0.00;
                if (techDataCalendarExcDay.get("exceptionCapacity") != null)
                    exceptionCapacity = techDataCalendarExcDay.getDouble("exceptionCapacity").doubleValue();
                if (exceptionCapacity == 0.00 && fixedAsset.get("productionCapacity") != null)
                    exceptionCapacity = fixedAsset.getDouble("productionCapacity").doubleValue();
                if (exceptionCapacity != 0.00) {
                    double usedCapacity = 0.00;
                    if (techDataCalendarExcDay.get("usedCapacity") != null)
                        usedCapacity = techDataCalendarExcDay.getDouble("usedCapacity").doubleValue();
                    if (exceptionCapacity < (quantity + usedCapacity)) {
                        resultMessage = resultMessage.concat(exceptionDateStartTime.toString().substring(0, 10) + ", ");
                        Debug.logInfo("No rental fixed Asset available: " + exceptionCapacity +
                                " already used: " + usedCapacity +
                                " Requested now: " + quantity, module);
                    }
                }
            }
        }
        if (resultMessage.compareTo("") == 0) {        
            String msg = UtilProperties.getMessage(resource, "item.availableOk", cart.getLocale());
            return msg;
        }
        else {
            Map messageMap = UtilMisc.toMap("resultMessage", resultMessage);
            String msg = UtilProperties.getMessage(resource, "item.notAvailable", messageMap, cart.getLocale());
            return msg;            
        }
    }

    protected boolean isInventoryAvailableOrNotRequired(double quantity, String productStoreId, LocalDispatcher dispatcher) throws CartItemModifyException {
        boolean inventoryAvailable = true;
        try {
            Map invReqResult = dispatcher.runSync("isStoreInventoryAvailableOrNotRequired", UtilMisc.toMap("productStoreId", productStoreId, "productId", productId, "product", this.getProduct(), "quantity", new Double(quantity)));
            if (ServiceUtil.isError(invReqResult)) {
                Debug.logError("Error calling isStoreInventoryAvailableOrNotRequired service, result is: " + invReqResult, module);
                throw new CartItemModifyException((String) invReqResult.get(ModelService.ERROR_MESSAGE));
            }
            inventoryAvailable = "Y".equals((String) invReqResult.get("availableOrNotRequired"));
        } catch (GenericServiceException e) {
            String errMsg = "Fatal error calling inventory checking services: " + e.toString();
            Debug.logError(e, errMsg, module);
            throw new CartItemModifyException(errMsg);
        }
        return inventoryAvailable;
    }

    protected void setQuantity(int quantity, LocalDispatcher dispatcher, ShoppingCart cart, boolean triggerExternalOps, boolean resetShipGroup, boolean updateProductPrice, boolean skipInventoryChecks) throws CartItemModifyException {
        if (this.quantity == quantity) {
            return;
        }

        if (this.isPromo) {
            throw new CartItemModifyException("Sorry, you can't change the quantity on the promotion item " + this.getName() + " (product ID: " + productId + "), not setting quantity.");
        }

        // needed for inventory checking and auto-save
        String productStoreId = cart.getProductStoreId();

        if (!skipInventoryChecks && !"PURCHASE_ORDER".equals(cart.getOrderType())) {
            // check inventory if new quantity is greater than old quantity; don't worry about inventory getting pulled out from under, that will be handled at checkout time
            if (_product != null && quantity > this.quantity) {
                if (!isInventoryAvailableOrNotRequired(quantity, productStoreId, dispatcher)) {
                    String excMsg = "Sorry, we do not have enough (you tried " + UtilFormatOut.formatQuantity(quantity) + ") of the product " + this.getName() + " (product ID: " + productId + ") in stock, not adding to cart. Please try a lower quantity, try again later, or call customer service for more information.";
                    Debug.logWarning(excMsg, module);
                    throw new CartItemModifyException(excMsg);
                }
            }
        }

        // set quantity before promos so order total, etc will be updated
        this.quantity = quantity;

        if (updateProductPrice) {
            this.updatePrice(dispatcher, cart);
        }

        // apply/unapply promotions
        if (triggerExternalOps) {
            ProductPromoWorker.doPromotions(cart, dispatcher);
        }

        if (!"PURCHASE_ORDER".equals(cart.getOrderType())) {
            // store the auto-save cart
            if (triggerExternalOps && ProductStoreWorker.autoSaveCart(delegator, productStoreId)) {
                try {
                    ShoppingListEvents.fillAutoSaveList(cart, dispatcher);
                } catch (GeneralException e) {
                    Debug.logWarning(e, UtilProperties.getMessage(resource_error,"OrderUnableToStoreAutoSaveCart", locale));
                }
            }
        }

        // set the item ship group
        if (resetShipGroup) {
            cart.clearItemShipInfo(this);

            /*

            // Deprecated in favour of ShoppingCart.createDropShipGroups(), called during checkout

            int shipGroupIndex = -1;
            if ("PURCHASE_ORDER".equals(cart.getOrderType())) {
                shipGroupIndex = 0;
            } else {
                if (_product != null && "PRODRQM_DS".equals(_product.getString("requirementMethodEnumId"))) {
                    // this is a drop-ship only product: we need a ship group with supplierPartyId set
                    Map supplierProductsResult = null;
                    try {
                        supplierProductsResult = dispatcher.runSync("getSuppliersForProduct", UtilMisc.toMap("productId", _product.getString("productId"),
                                                                                                                 "quantity", new Double(quantity),
                                                                                                                 "currencyUomId", cart.getCurrency(),
                                                                                                                 "canDropShip", "Y",
                                                                                                                 "userLogin", cart.getUserLogin()));
                        List productSuppliers = (List)supplierProductsResult.get("supplierProducts");
                        GenericValue supplierProduct = EntityUtil.getFirst(productSuppliers);
                        if (supplierProduct != null) {
                            String supplierPartyId = supplierProduct.getString("partyId");
                            List shipGroups = cart.getShipGroups();
                            for(int i = 0; i < shipGroups.size(); i++) {
                                ShoppingCart.CartShipInfo csi = (ShoppingCart.CartShipInfo)shipGroups.get(i);
                                if (supplierPartyId.equals(csi.getSupplierPartyId())) {
                                    shipGroupIndex = i;
                                    break;
                                }
                            }
                            if (shipGroupIndex == -1) {
                                // create a new ship group
                                shipGroupIndex = cart.addShipInfo();
                                cart.setSupplierPartyId(shipGroupIndex, supplierPartyId);
                            }
                        }
                    } catch (Exception e) {
                        Debug.logWarning("Error calling getSuppliersForProduct service, result is: " + supplierProductsResult, module);
                    }
                }

                if (shipGroupIndex == -1) {
                    List shipGroups = cart.getShipGroups();
                    for(int i = 0; i < shipGroups.size(); i++) {
                        ShoppingCart.CartShipInfo csi = (ShoppingCart.CartShipInfo)shipGroups.get(i);
                        if (csi.getSupplierPartyId() == null) {
                            shipGroupIndex = i;
                            break;
                        }
                    }
                    if (shipGroupIndex == -1) {
                        // create a new ship group
                        shipGroupIndex = cart.addShipInfo();
                    }
                }
            }
            cart.setItemShipGroupQty(this, quantity, shipGroupIndex);
            */
            cart.setItemShipGroupQty(this, quantity, 0);
        }
    }

    public void updatePrice(LocalDispatcher dispatcher, ShoppingCart cart) throws CartItemModifyException {
        // set basePrice using the calculateProductPrice service
        if (_product != null && isModifiedPrice == false) {
            try {
                Map priceContext = FastMap.newInstance();
                priceContext.put("currencyUomId", cart.getCurrency());

                String partyId = cart.getPartyId();
                if (partyId != null) {
                    priceContext.put("partyId", partyId);
                }
                priceContext.put("quantity", new Double(this.getQuantity()));
                priceContext.put("product", this.getProduct());
                if (cart.getOrderType().equals("PURCHASE_ORDER")) {
                    Map priceResult = dispatcher.runSync("calculatePurchasePrice", priceContext);
                    if (ServiceUtil.isError(priceResult)) {
                        throw new CartItemModifyException("There was an error while calculating the price: " + ServiceUtil.getErrorMessage(priceResult));
                    }
                    Boolean validPriceFound = (Boolean) priceResult.get("validPriceFound");
                    if (!validPriceFound.booleanValue()) {
                        throw new CartItemModifyException("Could not find a valid price for the product with ID [" + this.getProductId() + "] and supplier with ID [" + partyId + "], not adding to cart.");
                    }

                    this.setBasePrice(((Double) priceResult.get("price")).doubleValue());
                    this.setDisplayPrice(this.basePrice);
                    this.orderItemPriceInfos = (List) priceResult.get("orderItemPriceInfos");
                } else {
                    priceContext.put("prodCatalogId", this.getProdCatalogId());
                    priceContext.put("webSiteId", cart.getWebSiteId());
                    priceContext.put("productStoreId", cart.getProductStoreId());
                    priceContext.put("agreementId", cart.getAgreementId());
                    priceContext.put("productPricePurposeId", "PURCHASE");
                    priceContext.put("checkIncludeVat", "Y"); 
                    Map priceResult = dispatcher.runSync("calculateProductPrice", priceContext);
                    if (ServiceUtil.isError(priceResult)) {
                        throw new CartItemModifyException("There was an error while calculating the price: " + ServiceUtil.getErrorMessage(priceResult));
                    }

                    Boolean validPriceFound = (Boolean) priceResult.get("validPriceFound");
                    if (Boolean.FALSE.equals(validPriceFound)) {
                        throw new CartItemModifyException("Could not find a valid price for the product with ID [" + this.getProductId() + "], not adding to cart.");
                    }

                    if (priceResult.get("listPrice") != null) {
                        this.listPrice = ((Double) priceResult.get("listPrice")).doubleValue();
                    }

                    if (priceResult.get("basePrice") != null) {
                        this.setBasePrice(((Double) priceResult.get("basePrice")).doubleValue());
                    }

                    if (priceResult.get("price") != null) {
                        this.setDisplayPrice(((Double) priceResult.get("price")).doubleValue());
                    }

                    this.setSpecialPromoPrice((Double) priceResult.get("specialPromoPrice"));

                    this.orderItemPriceInfos = (List) priceResult.get("orderItemPriceInfos");

                    // If product is configurable, the price is taken from the configWrapper.
                    if (configWrapper != null) {
                        // TODO: for configurable products need to do something to make them VAT aware... for now base and display prices are the same
                        this.setBasePrice(configWrapper.getTotalPrice());
                        this.setDisplayPrice(configWrapper.getTotalPrice());
                    }
                    
                    // no try to do a recurring price calculation; not all products have recurring prices so may be null
                    Map recurringPriceContext = FastMap.newInstance();
                    recurringPriceContext.putAll(priceContext);
                    recurringPriceContext.put("productPricePurposeId", "RECURRING_CHARGE");
                    Map recurringPriceResult = dispatcher.runSync("calculateProductPrice", recurringPriceContext);
                    if (ServiceUtil.isError(recurringPriceResult)) {
                        throw new CartItemModifyException("There was an error while calculating the price: " + ServiceUtil.getErrorMessage(recurringPriceResult));
                    }

                    // for the recurring price only set the values iff validPriceFound is true
                    Boolean validRecurringPriceFound = (Boolean) recurringPriceResult.get("validPriceFound");
                    if (Boolean.TRUE.equals(validRecurringPriceFound)) {
                        if (recurringPriceResult.get("basePrice") != null) {
                            this.setRecurringBasePrice((Double) recurringPriceResult.get("basePrice"));
                        }
                        if (recurringPriceResult.get("price") != null) {
                            this.setRecurringDisplayPrice((Double) recurringPriceResult.get("price"));
                        }
                    }
                }
            } catch (GenericServiceException e) {
                throw new CartItemModifyException("There was an error while calculating the price", e);
            }
        }
    }

    /** Returns the quantity. */
    public double getQuantity() {
        return this.quantity;
    }

    /** Returns the reservation start date. */
    public Timestamp getReservStart() {
        return this.getReservStart(0);
    }
    /** Returns the reservation start date with a number of days added. */
    public Timestamp getReservStart(double addDays) {
        if (addDays == 0)
                return this.reservStart;
        else    {
            if(this.reservStart != null)
                return new Timestamp((long)(this.reservStart.getTime() + (addDays * 86400000.0)));
            else
                return null;
        }
    }
    /** Returns the reservation length. */
    public double getReservLength() {
        return this.reservLength;
    }
    /** Returns the reservation number of persons. */
    public double getReservPersons() {
        return this.reservPersons;
    }

    public double getPromoQuantityUsed() {
        if (this.getIsPromo()) {
            return this.quantity;
        } else {
            return this.promoQuantityUsed;
        }
    }

    public double getPromoQuantityAvailable() {
        if (this.getIsPromo()) {
            return 0;
        } else {
            return this.quantity - this.promoQuantityUsed;
        }
    }

    public Iterator getQuantityUsedPerPromoActualIter() {
        return this.quantityUsedPerPromoActual.entrySet().iterator();
    }

    public Iterator getQuantityUsedPerPromoCandidateIter() {
        return this.quantityUsedPerPromoCandidate.entrySet().iterator();
    }

    public Iterator getQuantityUsedPerPromoFailedIter() {
        return this.quantityUsedPerPromoFailed.entrySet().iterator();
    }

    public synchronized double addPromoQuantityCandidateUse(double quantityDesired, GenericValue productPromoCondAction, boolean checkAvailableOnly) {
        if (quantityDesired == 0) return 0;
        double promoQuantityAvailable = this.getPromoQuantityAvailable();
        double promoQuantityToUse = quantityDesired;
        if (promoQuantityAvailable > 0) {
            if (promoQuantityToUse > promoQuantityAvailable) {
                promoQuantityToUse = promoQuantityAvailable;
            }

            if (!checkAvailableOnly) {
                // keep track of candidate promo uses on cartItem
                GenericPK productPromoCondActionPK = productPromoCondAction.getPrimaryKey();
                Double existingValue = (Double) this.quantityUsedPerPromoCandidate.get(productPromoCondActionPK);
                if (existingValue == null) {
                    this.quantityUsedPerPromoCandidate.put(productPromoCondActionPK, new Double(promoQuantityToUse));
                } else {
                    this.quantityUsedPerPromoCandidate.put(productPromoCondActionPK, new Double(promoQuantityToUse + existingValue.doubleValue()));
                }

                this.promoQuantityUsed += promoQuantityToUse;
                //Debug.logInfo("promoQuantityToUse=" + promoQuantityToUse + ", quantityDesired=" + quantityDesired + ", for promoCondAction: " + productPromoCondAction, module);
                //Debug.logInfo("promoQuantityUsed now=" + promoQuantityUsed, module);
            }

            return promoQuantityToUse;
        } else {
            return 0;
        }
    }

    public double getPromoQuantityCandidateUse(GenericValue productPromoCondAction) {
        GenericPK productPromoCondActionPK = productPromoCondAction.getPrimaryKey();
        Double existingValue = (Double) this.quantityUsedPerPromoCandidate.get(productPromoCondActionPK);
        if (existingValue == null) {
            return 0;
        } else {
            return existingValue.doubleValue();
        }
    }

    public double getPromoQuantityCandidateUseActionAndAllConds(GenericValue productPromoAction) {
        double totalUse = 0;
        String productPromoId = productPromoAction.getString("productPromoId");
        String productPromoRuleId = productPromoAction.getString("productPromoRuleId");

        GenericPK productPromoActionPK = productPromoAction.getPrimaryKey();
        Double existingValue = (Double) this.quantityUsedPerPromoCandidate.get(productPromoActionPK);
        if (existingValue != null) {
            totalUse = existingValue.doubleValue();
        }

        Iterator entryIter = this.quantityUsedPerPromoCandidate.entrySet().iterator();
        while (entryIter.hasNext()) {
            Map.Entry entry = (Map.Entry) entryIter.next();
            GenericPK productPromoCondActionPK = (GenericPK) entry.getKey();
            Double quantityUsed = (Double) entry.getValue();
            if (quantityUsed != null) {
                // must be in the same rule and be a condition
                if (productPromoId.equals(productPromoCondActionPK.getString("productPromoId")) &&
                        productPromoRuleId.equals(productPromoCondActionPK.getString("productPromoRuleId")) &&
                        productPromoCondActionPK.containsKey("productPromoCondSeqId")) {
                    totalUse += quantityUsed.doubleValue();
                }
            }
        }

        return totalUse;
    }

    public synchronized void resetPromoRuleUse(String productPromoId, String productPromoRuleId) {
        Iterator entryIter = this.quantityUsedPerPromoCandidate.entrySet().iterator();
        while (entryIter.hasNext()) {
            Map.Entry entry = (Map.Entry) entryIter.next();
            GenericPK productPromoCondActionPK = (GenericPK) entry.getKey();
            Double quantityUsed = (Double) entry.getValue();
            if (productPromoId.equals(productPromoCondActionPK.getString("productPromoId")) && productPromoRuleId.equals(productPromoCondActionPK.getString("productPromoRuleId"))) {
                entryIter.remove();
                Double existingValue = (Double) this.quantityUsedPerPromoFailed.get(productPromoCondActionPK);
                if (existingValue == null) {
                    this.quantityUsedPerPromoFailed.put(productPromoCondActionPK, quantityUsed);
                } else {
                    this.quantityUsedPerPromoFailed.put(productPromoCondActionPK, new Double(quantityUsed.doubleValue() + existingValue.doubleValue()));
                }
                this.promoQuantityUsed -= quantityUsed.doubleValue();
            }
        }
    }

    public synchronized void confirmPromoRuleUse(String productPromoId, String productPromoRuleId) {
        Iterator entryIter = this.quantityUsedPerPromoCandidate.entrySet().iterator();
        while (entryIter.hasNext()) {
            Map.Entry entry = (Map.Entry) entryIter.next();
            GenericPK productPromoCondActionPK = (GenericPK) entry.getKey();
            Double quantityUsed = (Double) entry.getValue();
            if (productPromoId.equals(productPromoCondActionPK.getString("productPromoId")) && productPromoRuleId.equals(productPromoCondActionPK.getString("productPromoRuleId"))) {
                entryIter.remove();
                Double existingValue = (Double) this.quantityUsedPerPromoActual.get(productPromoCondActionPK);
                if (existingValue == null) {
                    this.quantityUsedPerPromoActual.put(productPromoCondActionPK, quantityUsed);
                } else {
                    this.quantityUsedPerPromoActual.put(productPromoCondActionPK, new Double(quantityUsed.doubleValue() + existingValue.doubleValue()));
                }
            }
        }
    }

    public synchronized void clearPromoRuleUseInfo() {
        this.quantityUsedPerPromoActual.clear();
        this.quantityUsedPerPromoCandidate.clear();
        this.quantityUsedPerPromoFailed.clear();
        this.promoQuantityUsed = this.getIsPromo() ? this.quantity : 0;
    }

    /** Sets the item comment. */
    public void setItemComment(String itemComment) {
        this.setAttribute("itemComment", itemComment);
    }

    /** Returns the item's comment. */
    public String getItemComment() {
        return (String) this.getAttribute("itemComment");
    }

    /** Sets the item's customer desired delivery date. */
    public void setDesiredDeliveryDate(Timestamp ddDate) {
        if (ddDate != null) {
            this.setAttribute("itemDesiredDeliveryDate", ddDate.toString());
        }
    }

    /** Returns the item's customer desired delivery date. */
    public Timestamp getDesiredDeliveryDate() {
        String ddDate = (String) this.getAttribute("itemDesiredDeliveryDate");

        if (ddDate != null) {
            try {
                return Timestamp.valueOf(ddDate);
            } catch (IllegalArgumentException e) {
                Debug.logWarning(e, UtilProperties.getMessage(resource_error,"OrderProblemGettingItemDesiredDeliveryDateFor", UtilMisc.toMap("productId",this.getProductId()), locale));
                return null;
            }
        }
        return null;
    }

    /** Sets the date to ship before */
    public void setShipBeforeDate(Timestamp date) {
        this.shipBeforeDate = date;
        
    }

    /** Returns the date to ship before */
    public Timestamp getShipBeforeDate() {
        return this.shipBeforeDate;
    }

    /** Sets the date to ship after */
    public void setShipAfterDate(Timestamp date) {
        this.shipAfterDate = date;
    }

    /** Returns the date to ship after */
    public Timestamp getShipAfterDate() {
        return this.shipAfterDate;
    }

    /** Sets the item type. */
    public void setItemType(String itemType) {
        this.itemType = itemType;
    }

    /** Returns the item type. */
    public String getItemType() {
        return this.itemType;
    }

    /** Returns the item type. */
    public GenericValue getItemTypeGenericValue() {
        try {
            return this.getDelegator().findByPrimaryKeyCache("OrderItemType", UtilMisc.toMap("orderItemTypeId", this.itemType));
        } catch (GenericEntityException e) {
            Debug.logError(e, "Error getting ShippingCartItem's OrderItemType", module);
            return null;
        }
    }

    /** Sets the item group. */
    public void setItemGroup(ShoppingCart.ShoppingCartItemGroup itemGroup) {
        this.itemGroup = itemGroup;
    }

    /** Sets the item group. */
    public void setItemGroup(String groupNumber, ShoppingCart cart) {
        this.itemGroup = cart.getItemGroupByNumber(groupNumber);
    }

    /** Returns the item group. */
    public ShoppingCart.ShoppingCartItemGroup getItemGroup() {
        return this.itemGroup;
    }
    
    public boolean isInItemGroup(String groupNumber) {
        if (this.itemGroup == null) return false;
        if (this.itemGroup.getGroupNumber().equals(groupNumber)) return true;
        return false;
    }

    /** Returns the item type description. */
    public String getItemTypeDescription() {
        GenericValue orderItemType = null;
        if (this.getItemType() != null) {
            try {
                orderItemType = this.getDelegator().findByPrimaryKeyCache("OrderItemType", UtilMisc.toMap("orderItemTypeId", this.getItemType()));
            } catch (GenericEntityException e) {
                Debug.logWarning(e, UtilProperties.getMessage(resource_error,"OrderProblemsGettingOrderItemTypeFor", UtilMisc.toMap("orderItemTypeId",this.getItemType()), locale));
            }
        }
        if (orderItemType != null) {
            return orderItemType.getString("description");
        }
        return null;
    }

    /** Returns the productCategoryId for the item or null if none. */
    public String getProductCategoryId() {
        return this.productCategoryId;
    }

    public void setProductCategoryId(String productCategoryId) {
        this.productCategoryId = productCategoryId;
    }

    public void setOrderItemSeqId(String orderItemSeqId) {
        Debug.log("Setting orderItemSeqId - " + orderItemSeqId, module);
        this.orderItemSeqId = orderItemSeqId;
    }

    public String getOrderItemSeqId() {
        return orderItemSeqId;
    }

    public void setShoppingList(String shoppingListId, String itemSeqId) {
        attributes.put("shoppingListId", shoppingListId);
        attributes.put("shoppingListItemSeqId", itemSeqId);
    }

    public String getShoppingListId() {
        return (String) attributes.get("shoppingListId");
    }

    public String getShoppingListItemSeqId() {
        return (String) attributes.get("shoppingListItemSeqId");
    }

    /** Sets the requirementId. */
    public void setRequirementId(String requirementId) {
        this.requirementId = requirementId;
    }

    /** Returns the requirementId. */
    public String getRequirementId() {
        return this.requirementId;
    }

    /** Sets the quoteId. */
    public void setQuoteId(String quoteId) {
        this.quoteId = quoteId;
    }

    /** Returns the quoteId. */
    public String getQuoteId() {
        return this.quoteId;
    }

    /** Sets the quoteItemSeqId. */
    public void setQuoteItemSeqId(String quoteItemSeqId) {
        this.quoteItemSeqId = quoteItemSeqId;
    }

    /** Returns the quoteItemSeqId. */
    public String getQuoteItemSeqId() {
        return this.quoteItemSeqId;
    }

    /** Sets the orderItemAssocTypeId. */
    public void setOrderItemAssocTypeId(String orderItemAssocTypeId) {
        if (orderItemAssocTypeId != null) {
            this.orderItemAssocTypeId = orderItemAssocTypeId;
        }
    }

    /** Returns the OrderItemAssocTypeId. */
    public String getOrderItemAssocTypeId() {
        return this.orderItemAssocTypeId;
    }

    /** Sets the associatedOrderId. */
    public void setAssociatedOrderId(String associatedOrderId) {
        this.associatedOrderId = associatedOrderId;
    }

    /** Returns the associatedId. */
    public String getAssociatedOrderId() {
        return this.associatedOrderId;
    }

    /** Sets the associatedOrderItemSeqId. */
    public void setAssociatedOrderItemSeqId(String associatedOrderItemSeqId) {
        this.associatedOrderItemSeqId = associatedOrderItemSeqId;
    }

    /** Returns the associatedOrderItemSeqId. */
    public String getAssociatedOrderItemSeqId() {
        return this.associatedOrderItemSeqId;
    }

    public String getStatusId() {
        return this.statusId;
    }

    public void setStatusId(String statusId) {
        this.statusId = statusId;
    }

    /** Returns true if shipping charges apply to this item. */
    public boolean shippingApplies() {
        GenericValue product = getProduct();
        if (product != null) {
            return ProductWorker.shippingApplies(product);
        } else {
            // we don't ship non-product items
            return false;
        }
    }

    /** Returns true if tax charges apply to this item. */
    public boolean taxApplies() {
        GenericValue product = getProduct();
        if (product != null) {
            return ProductWorker.taxApplies(product);
        } else {
            // we do tax non-product items
            return true;
        }
    }

    /** Returns the item's productId. */
    public String getProductId() {
        return productId;
    }
    /** Set the item's description. */
    public void setName(String itemName) {
        this.itemDescription = itemName;
    }
    /** Returns the item's description. */
    public String getName() {
       if (itemDescription != null) {
          return itemDescription;
       } else {
        GenericValue product = getProduct();
        if (product != null) {
            String productName = ProductContentWrapper.getProductContentAsText(product, "PRODUCT_NAME", this.locale, null);
            // if the productName is null or empty, see if there is an associated virtual product and get the productName of that product
            if (UtilValidate.isEmpty(productName)) {
                GenericValue parentProduct = this.getParentProduct();
                if (parentProduct != null) {
                    productName = ProductContentWrapper.getProductContentAsText(parentProduct, "PRODUCT_NAME", this.locale, null);
                }
            }
            if (productName == null) {
                return "";
            } else {
                return productName;
            }
        } else {
               return "";
            }
        }
    }

    /** Returns the item's description. */
    public String getDescription() {
        GenericValue product = getProduct();

        if (product != null) {
            String description = ProductContentWrapper.getProductContentAsText(product, "DESCRIPTION", this.locale, null);

            // if the description is null or empty, see if there is an associated virtual product and get the description of that product
            if (UtilValidate.isEmpty(description)) {
                GenericValue parentProduct = this.getParentProduct();
                if (parentProduct != null) {
                    description = ProductContentWrapper.getProductContentAsText(parentProduct, "DESCRIPTION", this.locale, null);
                }
            }

            if (description == null) {
                return "";
            } else {
                return description;
            }
        } else {
            return null;
        }
    }

    public ProductConfigWrapper getConfigWrapper() {
        return configWrapper;
    }

    /** Returns the item's unit weight */
    public double getWeight() {
        GenericValue product = getProduct();
        if (product != null) {
            Double weight = product.getDouble("weight");

            // if the weight is null, see if there is an associated virtual product and get the weight of that product
            if (weight == null) {
                GenericValue parentProduct = this.getParentProduct();
                if (parentProduct != null) weight = parentProduct.getDouble("weight");
            }

            if (weight == null) {
                return 0;
            } else {
                return weight.doubleValue();
            }
        } else {
            // non-product items have 0 weight
            return 0;
        }
    }

    /** Returns the item's pieces included */
    public long getPiecesIncluded() {
        GenericValue product = getProduct();
        if (product != null) {
            Long pieces = product.getLong("piecesIncluded");

            // if the piecesIncluded is null, see if there is an associated virtual product and get the piecesIncluded of that product
            if (pieces == null) {
                GenericValue parentProduct = this.getParentProduct();
                if (parentProduct != null) pieces = parentProduct.getLong("piecesIncluded");
            }

            if (pieces == null) {
                return 1;
            } else {
                return pieces.longValue();
            }
        } else {
            // non-product item assumed 1 piece
            return 1;
        }
    }

    /** Returns a Set of the item's features */
    public Set getFeatureSet() {
        Set featureSet = new ListOrderedSet();
        GenericValue product = this.getProduct();
        if (product != null) {
            List featureAppls = null;
            try {
                featureAppls = product.getRelated("ProductFeatureAppl");
                List filterExprs = UtilMisc.toList(new EntityExpr("productFeatureApplTypeId", EntityOperator.EQUALS, "STANDARD_FEATURE"));
                filterExprs.add(new EntityExpr("productFeatureApplTypeId", EntityOperator.EQUALS, "REQUIRED_FEATURE"));
                featureAppls = EntityUtil.filterByOr(featureAppls, filterExprs);
            } catch (GenericEntityException e) {
                Debug.logError(e, "Unable to get features from product : " + product.get("productId"), module);
            }
            if (featureAppls != null) {
                Iterator fai = featureAppls.iterator();
                while (fai.hasNext()) {
                    GenericValue appl = (GenericValue) fai.next();
                    featureSet.add(appl.getString("productFeatureId"));
                }
            }
        }
        if (this.additionalProductFeatureAndAppls != null) {
            Iterator aapi = this.additionalProductFeatureAndAppls.values().iterator();
            while (aapi.hasNext()) {
                GenericValue appl = (GenericValue) aapi.next();
                featureSet.add(appl.getString("productFeatureId"));
            }
        }
        return featureSet;
    }
    /** Returns a list of the item's standard features */
    public List getStandardFeatureList() {
        List features = null;
        GenericValue product = this.getProduct();
        if (product != null) {
            try {
                List featureAppls = product.getRelated("ProductFeatureAndAppl");
                features=EntityUtil.filterByAnd(featureAppls,UtilMisc.toMap("productFeatureApplTypeId","STANDARD_FEATURE"));
            } catch (GenericEntityException e) {
                Debug.logError(e, "Unable to get features from product : " + product.get("productId"), module);
            }
        }
        return features;
    }

    /** Returns a List of the item's features for supplier*/
   public List getFeaturesForSupplier(LocalDispatcher dispatcher,String partyId) {
       List featureAppls = getStandardFeatureList();
       if (featureAppls != null && featureAppls.size() > 0) {
           try {
              Map result = dispatcher.runSync("convertFeaturesForSupplier", UtilMisc.toMap("partyId", partyId, "productFeatures", featureAppls));
              featuresForSupplier = (List) result.get("convertedProductFeatures");
           } catch (GenericServiceException e) {
               Debug.logError(e, "Unable to get features for supplier from product : " + this.productId, module);
           }
       }
       return featuresForSupplier;
   }

    /** Returns the item's size (length + girth) */
    public double getSize() {
        GenericValue product = getProduct();
        if (product != null) {
            Double height = product.getDouble("shippingHeight");
            Double width = product.getDouble("shippingWidth");
            Double depth = product.getDouble("shippingDepth");

            // if all are null, see if there is an associated virtual product and get the info of that product
            if (height == null & width == null && depth == null) {
                GenericValue parentProduct = this.getParentProduct();
                if (parentProduct != null) {
                    height = parentProduct.getDouble("shippingHeight");
                    width = product.getDouble("shippingWidth");
                    depth = product.getDouble("shippingDepth");
                }
            }

            if (height == null) height = new Double(0);
            if (width == null) width = new Double(0);
            if (depth == null) depth = new Double(0);

            // determine girth (longest field is length)
            double[] sizeInfo = { height.doubleValue(), width.doubleValue(), depth.doubleValue() };
            Arrays.sort(sizeInfo);

            return (sizeInfo[0] * 2) + (sizeInfo[1] * 2) + sizeInfo[2];
        } else {
            // non-product items have 0 size
            return 0;
        }
    }


    public Map getItemProductInfo() {
        Map itemInfo = FastMap.newInstance();
        itemInfo.put("productId", this.getProductId());
        itemInfo.put("weight", new Double(this.getWeight()));
        itemInfo.put("size",  new Double(this.getSize()));
        itemInfo.put("piecesIncluded", new Long(this.getPiecesIncluded()));
        itemInfo.put("featureSet", this.getFeatureSet());
        GenericValue product = getProduct();
        if (product != null) {
            itemInfo.put("inShippingBox", product.getString("inShippingBox"));
            if (product.getString("inShippingBox") != null && product.getString("inShippingBox").equals("Y")){
                itemInfo.put("shippingHeight", product.getDouble("shippingHeight"));
                itemInfo.put("shippingWidth", product.getDouble("shippingWidth"));
                itemInfo.put("shippingDepth", product.getDouble("shippingDepth"));
            }
        }
        return itemInfo;
    }

    /** Returns the base price. */
    public double getBasePrice() {
        double curBasePrice;
        if (selectedAmount > 0) {
            curBasePrice = basePrice * selectedAmount;
        } else {
            curBasePrice = basePrice;
        }
        return curBasePrice;
    }
    
    public double getDisplayPrice() {
        double curDisplayPrice;
        if (this.displayPrice == null) {
            curDisplayPrice = this.getBasePrice();
        } else {
            if (selectedAmount > 0) {
                curDisplayPrice = this.displayPrice.doubleValue() * this.selectedAmount;
            } else {
                curDisplayPrice = this.displayPrice.doubleValue();
            }
        }
        return curDisplayPrice;
    }
    
    public Double getSpecialPromoPrice() {
        return this.specialPromoPrice;
    }

    public Double getRecurringBasePrice() {
        if (this.recurringBasePrice == null) return null;
        
        if (selectedAmount > 0) {
            return new Double(this.recurringBasePrice.doubleValue() * selectedAmount);
        } else {
            return this.recurringBasePrice;
        }
    }
    
    public Double getRecurringDisplayPrice() {
        if (this.recurringDisplayPrice == null) {
            return this.getRecurringBasePrice();
        }

        if (selectedAmount > 0) {
            return new Double(this.recurringDisplayPrice.doubleValue() * this.selectedAmount);
        } else {
            return this.recurringDisplayPrice;
        }
    }
    
    /** Returns the list price. */
    public double getListPrice() {
        return listPrice;
    }

    /** Returns isModifiedPrice */
    public boolean getIsModifiedPrice() {
        return isModifiedPrice;
    }

    /** Set isModifiedPrice */
    public void setIsModifiedPrice(boolean isModifiedPrice) {
        this.isModifiedPrice = isModifiedPrice;
    }

    /** get the percentage for the second person */
    public double getReserv2ndPPPerc() {
        return reserv2ndPPPerc;
    }

    /** get the percentage for the third and following person */
    public double getReservNthPPPerc() {
        return reservNthPPPerc;
    }


    /** Returns the "other" adjustments. */
    public double getOtherAdjustments() {
        return OrderReadHelper.calcItemAdjustmentsBd(new BigDecimal(quantity), new BigDecimal(getBasePrice()), this.getAdjustments(), true, false, false, false, false).doubleValue();
    }

    /** Returns the "other" adjustments. */
    public double getOtherAdjustmentsRecurring() {
        return OrderReadHelper.calcItemAdjustmentsRecurringBd(new BigDecimal(quantity), new BigDecimal(getRecurringBasePrice() == null ? 0.0 : getRecurringBasePrice().doubleValue()), this.getAdjustments(), true, false, false, false, false).doubleValue();
    }

    /** calculates for a reservation the percentage/100 extra for more than 1 person. */
    // similar code at editShoppingList.bsh
    public double getRentalAdjustment() {
        if (!"RENTAL_ORDER_ITEM".equals(this.itemType)) {
            // not a rental item?
            return 1;
        }
        double persons = this.getReservPersons();
        double rentalValue = 0;
        if (persons > 1)    {
            if (persons > 2 ) {
                persons -= 2;
                if(getReservNthPPPerc() > 0) {
                    rentalValue = persons * getReservNthPPPerc();
                } else {
                    rentalValue = persons * getReserv2ndPPPerc();
                }
                persons = 2;
            }
            if (persons == 2) {
                rentalValue += getReserv2ndPPPerc();
            }
        }
        rentalValue += 100;    // add final 100 percent for first person
        //     Debug.log("rental parameters....Nbr of persons:" + getReservPersons() + " extra% 2nd person:" + getReserv2ndPPPerc()+ " extra% Nth person:" + getReservNthPPPerc() + "  total rental adjustment:" + rentalValue/100 * getReservLength() );
        return rentalValue/100 * getReservLength(); // return total rental adjustment
    }

    /** Returns the total line price. */
    public double getItemSubTotal(double quantity) {
//        Debug.logInfo("Price" + getBasePrice() + " quantity" +  quantity + " Rental adj:" + getRentalAdjustment() + " other adj:" + getOtherAdjustments(), module);
          return (getBasePrice() * quantity * getRentalAdjustment()) + getOtherAdjustments();
    }

    public double getItemSubTotal() {
        return this.getItemSubTotal(this.getQuantity());
    }

    public double getDisplayItemSubTotal() {
        return (this.getDisplayPrice() * this.getQuantity() * this.getRentalAdjustment()) + this.getOtherAdjustments();
    }
    
    public double getDisplayItemSubTotalNoAdj() {
        return this.getDisplayPrice() * this.getQuantity();
    }

    public double getDisplayItemRecurringSubTotal() {
        Double curRecurringDisplayPrice = this.getRecurringDisplayPrice();
        
        if (curRecurringDisplayPrice == null) {
            return this.getOtherAdjustmentsRecurring();
        }
        
        return (curRecurringDisplayPrice.doubleValue() * this.getQuantity()) + this.getOtherAdjustmentsRecurring();
    }

    public double getDisplayItemRecurringSubTotalNoAdj() {
        Double curRecurringDisplayPrice = this.getRecurringDisplayPrice();
        if (curRecurringDisplayPrice == null) return 0.0;
        
        return curRecurringDisplayPrice.doubleValue() * this.getQuantity();
    }

    public void addAllProductFeatureAndAppls(Map productFeatureAndApplsToAdd) {
        if (productFeatureAndApplsToAdd == null) return;
        Iterator productFeatureAndApplsToAddIter = productFeatureAndApplsToAdd.values().iterator();
        while (productFeatureAndApplsToAddIter.hasNext()) {
            GenericValue additionalProductFeatureAndAppl = (GenericValue) productFeatureAndApplsToAddIter.next();
            this.putAdditionalProductFeatureAndAppl(additionalProductFeatureAndAppl);
        }
    }

    public void putAdditionalProductFeatureAndAppl(GenericValue additionalProductFeatureAndAppl) {
        if (additionalProductFeatureAndAppl == null) return;

        // if one already exists with the given type, remove it with the corresponding adjustment
        removeAdditionalProductFeatureAndAppl(additionalProductFeatureAndAppl.getString("productFeatureTypeId"));

        // adds to additional map and creates an adjustment with given price
        String featureType = additionalProductFeatureAndAppl.getString("productFeatureTypeId");
        this.additionalProductFeatureAndAppls.put(featureType, additionalProductFeatureAndAppl);

        GenericValue orderAdjustment = this.getDelegator().makeValue("OrderAdjustment", null);
        orderAdjustment.set("orderAdjustmentTypeId", "ADDITIONAL_FEATURE");
        orderAdjustment.set("description", additionalProductFeatureAndAppl.get("description"));
        orderAdjustment.set("productFeatureId", additionalProductFeatureAndAppl.get("productFeatureId"));

        // NOTE: this is a VERY simple pricing scheme for additional features and will likely need to be extended for most real applications
        double amount = 0;
        Double amountDbl = (Double) additionalProductFeatureAndAppl.get("amount");
        if (amountDbl != null) {
            amount = amountDbl.doubleValue() * this.getQuantity();
        }
        orderAdjustment.set("amount", new Double(amount));

        Double recurringAmountDbl = (Double) additionalProductFeatureAndAppl.get("recurringAmount");
        if (recurringAmountDbl != null) {
            double recurringAmount = recurringAmountDbl.doubleValue() * this.getQuantity();
            orderAdjustment.set("recurringAmount", new Double(recurringAmount));
            //Debug.logInfo("Setting recurringAmount " + recurringAmount + " for " + orderAdjustment, module);
        }

        this.addAdjustment(orderAdjustment);
    }

    public GenericValue getAdditionalProductFeatureAndAppl(String productFeatureTypeId) {
        if (this.additionalProductFeatureAndAppls == null) return null;
        return (GenericValue) this.additionalProductFeatureAndAppls.get(productFeatureTypeId);
    }

    public GenericValue removeAdditionalProductFeatureAndAppl(String productFeatureTypeId) {
        if (this.additionalProductFeatureAndAppls == null) return null;

        GenericValue oldAdditionalProductFeatureAndAppl = (GenericValue) this.additionalProductFeatureAndAppls.remove(productFeatureTypeId);

        if (oldAdditionalProductFeatureAndAppl != null) {
            removeFeatureAdjustment(oldAdditionalProductFeatureAndAppl.getString("productFeatureId"));
        }

        //if (this.additionalProductFeatureAndAppls.size() == 0) this.additionalProductFeatureAndAppls = null;

        return oldAdditionalProductFeatureAndAppl;
    }

    public Map getAdditionalProductFeatureAndAppls() {
        return this.additionalProductFeatureAndAppls;
    }

    public Map getFeatureIdQtyMap(double quantity) {
        Map featureMap = FastMap.newInstance();
        GenericValue product = this.getProduct();
        if (product != null) {
            List featureAppls = null;
            try {
                featureAppls = product.getRelated("ProductFeatureAppl");
                List filterExprs = UtilMisc.toList(new EntityExpr("productFeatureApplTypeId", EntityOperator.EQUALS, "STANDARD_FEATURE"));
                filterExprs.add(new EntityExpr("productFeatureApplTypeId", EntityOperator.EQUALS, "REQUIRED_FEATURE"));
                featureAppls = EntityUtil.filterByOr(featureAppls, filterExprs);
            } catch (GenericEntityException e) {
                Debug.logError(e, "Unable to get features from product : " + product.get("productId"), module);
            }
            if (featureAppls != null) {
                Iterator fai = featureAppls.iterator();
                while (fai.hasNext()) {
                    GenericValue appl = (GenericValue) fai.next();
                    Double lastQuantity = (Double) featureMap.get(appl.getString("productFeatureId"));
                    if (lastQuantity == null) {
                        lastQuantity = new Double(0);
                    }
                    Double newQuantity = new Double(lastQuantity.doubleValue() + quantity);
                    featureMap.put(appl.getString("productFeatureId"), newQuantity);
                }
            }
        }
        if (this.additionalProductFeatureAndAppls != null) {
            Iterator aapi = this.additionalProductFeatureAndAppls.values().iterator();
            while (aapi.hasNext()) {
                GenericValue appl = (GenericValue) aapi.next();
                Double lastQuantity = (Double) featureMap.get(appl.getString("productFeatureId"));
                if (lastQuantity == null) {
                    lastQuantity = new Double(0);
                }
                Double newQuantity = new Double(lastQuantity.doubleValue() + quantity);
                featureMap.put(appl.getString("productFeatureId"), newQuantity);
            }
        }
        return featureMap;
    }

    /** Removes an item attribute. */
    public void removeAttribute(String name) {
        attributes.remove(name);
    }

    /** Sets an item attribute. */
    public void setAttribute(String name, Object value) {
        attributes.put(name, value);
    }

    /** Return a specific attribute. */
    public Object getAttribute(String name) {
        return attributes.get(name);
    }

    /** Returns the attributes for the item. */
    public Map getAttributes() {
        return attributes;
    }

    /** Remove an OrderItemAttribute. */
    public void removeOrderItemAttribute(String name) {
        if (orderItemAttributes != null) {
            orderItemAttributes.remove(name);
        }
    }

    /** Creates an OrderItemAttribute entry. */
    public void setOrderItemAttribute(String name, String value) {
        if (orderItemAttributes == null) orderItemAttributes = FastMap.newInstance();
        this.orderItemAttributes.put(name, value);
    }

    /** Return an OrderItemAttribute. */
    public String getOrderItemAttribute(String name) {
        if (orderItemAttributes == null) return null;
        return (String) this.orderItemAttributes.get(name);
    }

    public Map getOrderItemAttributes() {
        Map attrs = FastMap.newInstance();
        if (orderItemAttributes != null) {
            attrs.putAll(orderItemAttributes);
        }
        return attrs;
    }

    /** Add an adjustment to the order item; don't worry about setting the orderId, orderItemSeqId or orderAdjustmentId; they will be set when the order is created */
    public int addAdjustment(GenericValue adjustment) {
        itemAdjustments.add(adjustment);
        return itemAdjustments.indexOf(adjustment);
    }

    public void removeAdjustment(GenericValue adjustment) {
        itemAdjustments.remove(adjustment);
    }

    public void removeAdjustment(int index) {
        itemAdjustments.remove(index);
    }

    public List getAdjustments() {
        return itemAdjustments;
    }

    public void removeFeatureAdjustment(String productFeatureId) {
        if (productFeatureId == null) return;
        Iterator itemAdjustmentsIter = itemAdjustments.iterator();

        while (itemAdjustmentsIter.hasNext()) {
            GenericValue itemAdjustment = (GenericValue) itemAdjustmentsIter.next();

            if (productFeatureId.equals(itemAdjustment.getString("productFeatureId"))) {
                itemAdjustmentsIter.remove();
            }
        }
    }

    public List getOrderItemPriceInfos() {
        return orderItemPriceInfos;
    }

    /** Add a contact mech to this purpose; the contactMechPurposeTypeId is required */
    public void addContactMech(String contactMechPurposeTypeId, String contactMechId) {
        if (contactMechPurposeTypeId == null) throw new IllegalArgumentException("You must specify a contactMechPurposeTypeId to add a ContactMech");
        contactMechIdsMap.put(contactMechPurposeTypeId, contactMechId);
    }

    /** Get the contactMechId for this item given the contactMechPurposeTypeId */
    public String getContactMech(String contactMechPurposeTypeId) {
        return (String) contactMechIdsMap.get(contactMechPurposeTypeId);
    }

    /** Remove the contactMechId from this item given the contactMechPurposeTypeId */
    public String removeContactMech(String contactMechPurposeTypeId) {
        return (String) contactMechIdsMap.remove(contactMechPurposeTypeId);
    }

    public Map getOrderItemContactMechIds() {
        return contactMechIdsMap;
    }

    public void setIsPromo(boolean isPromo) {
        this.isPromo = isPromo;
    }

    public boolean getIsPromo() {
        return this.isPromo;
    }

    public List getAlternativeOptionProductIds() {
        return this.alternativeOptionProductIds;
    }
    public void setAlternativeOptionProductIds(List alternativeOptionProductIds) {
        this.alternativeOptionProductIds = alternativeOptionProductIds;
    }

    /** Compares the specified object with this cart item. */
    public boolean equals(ShoppingCartItem item) {
        if (item == null) return false;
        return this.equals(item.getProductId(), item.additionalProductFeatureAndAppls, item.attributes, item.prodCatalogId, item.selectedAmount, item.getItemType(), item.getItemGroup(), item.getIsPromo());
    }

    /** Compares the specified object with this cart item. Defaults isPromo to false. Default to no itemGroup. */
    public boolean equals(String productId, Map additionalProductFeatureAndAppls, Map attributes, String prodCatalogId, double selectedAmount) {
        return equals(productId, additionalProductFeatureAndAppls, attributes, prodCatalogId, selectedAmount, null, null, false);
    }

    /** Compares the specified object with this cart item. Defaults isPromo to false. */
    public boolean equals(String productId, Map additionalProductFeatureAndAppls, Map attributes, String prodCatalogId, ProductConfigWrapper configWrapper, String itemType, ShoppingCart.ShoppingCartItemGroup itemGroup, double selectedAmount) {
        return equals(productId, null, 0.00, 0.00, additionalProductFeatureAndAppls, attributes, prodCatalogId, selectedAmount, configWrapper, itemType, itemGroup, false);
    }

    /** Compares the specified object with this cart item including rental data. Defaults isPromo to false. */
    public boolean equals(String productId, Timestamp reservStart, double reservLength, double reservPersons, Map additionalProductFeatureAndAppls, Map attributes, String prodCatalogId, ProductConfigWrapper configWrapper, String itemType, ShoppingCart.ShoppingCartItemGroup itemGroup, double selectedAmount) {
        return equals(productId, reservStart, reservLength, reservPersons, additionalProductFeatureAndAppls, attributes, prodCatalogId, selectedAmount, configWrapper, itemType, itemGroup, false);
    }

    /** Compares the specified object with this cart item. Defaults isPromo to false. */
    public boolean equals(String productId, Map additionalProductFeatureAndAppls, Map attributes, String prodCatalogId, double selectedAmount, String itemType, ShoppingCart.ShoppingCartItemGroup itemGroup, boolean isPromo) {
        return equals(productId, null, 0.00, 0.00, additionalProductFeatureAndAppls, attributes, prodCatalogId, selectedAmount, null, itemType, itemGroup, isPromo);
    }

    /** Compares the specified object with this cart item. */
    public boolean equals(String productId, Timestamp reservStart, double reservLength, double reservPersons, 
            Map additionalProductFeatureAndAppls, Map attributes, String prodCatalogId, double selectedAmount, 
            ProductConfigWrapper configWrapper, String itemType, ShoppingCart.ShoppingCartItemGroup itemGroup, boolean isPromo) {
        if (this.productId == null || productId == null) {
            // all non-product items are unique
            return false;
        }
        if (!this.productId.equals(productId)) {
            return false;
        }

        if ((this.prodCatalogId == null && prodCatalogId != null) || (this.prodCatalogId != null && prodCatalogId == null)) {
            return false;
        }
        if (this.prodCatalogId != null && prodCatalogId != null && !this.prodCatalogId.equals(prodCatalogId)) {
            return false;
        }

        if (this.getSelectedAmount() != selectedAmount) {
            return false;
        }

        if ((this.reservStart == null && reservStart != null) || (this.reservStart != null && reservStart == null)) {
            return false;
        }
        if (this.reservStart != null && reservStart != null && !this.reservStart.equals(reservStart)) {
            return false;
        }

        if (this.reservLength != reservLength) {
            return false;
        }

        if (this.reservPersons != reservPersons) {
            return false;
        }

        if (this.isPromo != isPromo) {
            return false;
        }

        if ((this.additionalProductFeatureAndAppls != null && additionalProductFeatureAndAppls != null) &&
                (this.additionalProductFeatureAndAppls.size() != additionalProductFeatureAndAppls.size()) &&
                !(this.additionalProductFeatureAndAppls.equals(additionalProductFeatureAndAppls))) {
            return false;
        }

        if ((this.attributes != null && attributes != null) &&
                ( (this.attributes.size() != attributes.size()) ||
                !(this.attributes.equals(attributes)) )) {
            return false;
        }

        if (configWrapper != null && !configWrapper.equals(this.configWrapper)) {
            return false;
        }
        
        if (itemType != null && !itemType.equals(this.itemType)) {
            return false;
        }

        if (itemGroup != null && !itemGroup.equals(this.itemGroup)) {
            return false;
        }

        if (quoteId != null) {
            // all items linked to a quote are unique
            return false;
        }

        if (requirementId != null) {
            // all items linked to a requirement are unique
            return false;
        }

        return true;
    }

    /** Gets the Product entity. If it is not already retreived gets it from the delegator */
    public GenericValue getProduct() {
        if (this._product != null) {
            return this._product;
        }
        if (this.productId != null) {
            try {
                this._product = this.getDelegator().findByPrimaryKeyCache("Product", UtilMisc.toMap("productId", productId));
            } catch (GenericEntityException e) {
                throw new RuntimeException("Entity Engine error getting Product (" + e.getMessage() + ")");
            }
        }
        return this._product;
    }

    public GenericValue getParentProduct() {
        if (this._parentProduct != null) {
            return this._parentProduct;
        }
        if (this.productId == null) {
            throw new IllegalStateException("Bad product id");
        }

          this._parentProduct = ProductWorker.getParentProduct(productId, this.getDelegator());

        return this._parentProduct;
    }

    public String getParentProductId() {
        GenericValue parentProduct = this.getParentProduct();
        if (parentProduct != null) {
            return parentProduct.getString("productId");
        } else {
            return null;
        }
    }

    public Map getOptionalProductFeatures() {
        if (_product != null) {
            return ProductWorker.getOptionalProductFeatures(getDelegator(), this.productId);
        } else {
            // non-product items do not have features
            return FastMap.newInstance();
        }
    }

    public GenericDelegator getDelegator() {
        if (delegator == null) {
            if (UtilValidate.isEmpty(delegatorName)) {
                throw new IllegalStateException("Bad delegator name");
            }
            delegator = GenericDelegator.getGenericDelegator(delegatorName);
        }
        return delegator;
    }

    public void explodeItem(ShoppingCart cart, LocalDispatcher dispatcher) throws CartItemModifyException {
        double baseQuantity = this.getQuantity();
        int thisIndex = cart.items().indexOf(this);
        List newItems = new ArrayList();

        if (baseQuantity > 1) {
            for (int i = 1; i < baseQuantity; i++) {
                // clone the item
                ShoppingCartItem item = new ShoppingCartItem(this);

                // set the new item's quantity
                item.setQuantity(1, dispatcher, cart, false);

                // now copy/calc the adjustments
                Debug.logInfo("Clone's adj: " + item.getAdjustments(), module);
                if (item.getAdjustments() != null && item.getAdjustments().size() > 0) {
                    List adjustments = new LinkedList(item.getAdjustments());
                    Iterator adjIterator = adjustments.iterator();

                    while (adjIterator.hasNext()) {
                        GenericValue adjustment = (GenericValue) adjIterator.next();

                        if (adjustment != null) {
                            item.removeAdjustment(adjustment);
                            GenericValue newAdjustment = GenericValue.create(adjustment);
                            Double adjAmount = newAdjustment.getDouble("amount");

                            // we use != becuase adjustments can be +/-
                            if (adjAmount != null && adjAmount.doubleValue() != 0.00)
                                newAdjustment.set("amount", new Double(adjAmount.doubleValue() / baseQuantity));
                            Debug.logInfo("Cloned adj: " + newAdjustment, module);
                            item.addAdjustment(newAdjustment);
                        } else {
                            Debug.logInfo("Clone Adjustment is null", module);
                        }
                    }
                }
                newItems.add(item);
            }

            // set this item's quantity
            this.setQuantity(1, dispatcher, cart, false);

            Debug.logInfo("BaseQuantity: " + baseQuantity, module);
            Debug.logInfo("Item's Adj: " + this.getAdjustments(), module);

            // re-calc this item's adjustments
            if (this.getAdjustments() != null && this.getAdjustments().size() > 0) {
                List adjustments = new LinkedList(this.getAdjustments());
                Iterator adjIterator = adjustments.iterator();

                while (adjIterator.hasNext()) {
                    GenericValue adjustment = (GenericValue) adjIterator.next();

                    if (adjustment != null) {
                        this.removeAdjustment(adjustment);
                        GenericValue newAdjustment = GenericValue.create(adjustment);
                        Double adjAmount = newAdjustment.getDouble("amount");

                        // we use != becuase adjustments can be +/-
                        if (adjAmount != null && adjAmount.doubleValue() != 0.00)
                            newAdjustment.set("amount", new Double(adjAmount.doubleValue() / baseQuantity));
                        Debug.logInfo("Updated adj: " + newAdjustment, module);
                        this.addAdjustment(newAdjustment);
                    }
                }
            }

            // add the cloned item(s) to the cart
            Iterator newItemsItr = newItems.iterator();

            while (newItemsItr.hasNext()) {
                cart.addItem(thisIndex, (ShoppingCartItem) newItemsItr.next());
            }
        }
    }
    public static String getPurchaseOrderItemDescription(GenericValue product, GenericValue supplierProduct, Locale locale){
          String itemDescription = "";
          String supplierProductId = supplierProduct.getString("supplierProductId");
          if (supplierProductId == null) {
               supplierProductId = "";
          } else {
               supplierProductId += " ";
          }
          String supplierProductName = supplierProduct.getString("supplierProductName");
          if (supplierProductName == null) {
            if (supplierProductName == null) {
                supplierProductName = ProductContentWrapper.getProductContentAsText(product, "PRODUCT_NAME", locale, null);
             }
           }
          itemDescription = supplierProductId + supplierProductName;
          return itemDescription;
    }
}
