/*
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
 */
package org.apache.ofbiz.order.shoppingcart;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilFormatOut;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.DelegatorFactory;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericPK;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityExpr;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.entity.util.EntityUtilProperties;
import org.apache.ofbiz.order.order.OrderReadHelper;
import org.apache.ofbiz.order.shoppingcart.product.ProductPromoWorker;
import org.apache.ofbiz.order.shoppinglist.ShoppingListEvents;
import org.apache.ofbiz.product.catalog.CatalogWorker;
import org.apache.ofbiz.product.category.CategoryWorker;
import org.apache.ofbiz.product.config.ProductConfigWorker;
import org.apache.ofbiz.product.config.ProductConfigWrapper;
import org.apache.ofbiz.product.product.ProductContentWrapper;
import org.apache.ofbiz.product.product.ProductWorker;
import org.apache.ofbiz.product.store.ProductStoreWorker;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;

/**
 * <p><b>Title:</b> ShoppingCartItem.java
 * <p><b>Description:</b> Shopping cart item object.
 */
@SuppressWarnings("serial")
public class ShoppingCartItem implements java.io.Serializable {

    public static final MathContext GEN_ROUNDING = new MathContext(10);
    protected static final String[] ATTRIBUTE_NAMES = {"shoppingListId", "shoppingListItemSeqId", "surveyResponses",
            "itemDesiredDeliveryDate", "itemComment", "fromInventoryItemId"};
    private static final String MODULE = ShoppingCartItem.class.getName();
    private static final String RESOURCE = "OrderUiLabels";
    private static final String RES_ERROR = "OrderErrorUiLabels";
    private transient Delegator delegator = null;
    /**
     * the actual or variant product
     */
    private transient GenericValue product = null;
    /**
     * the virtual product if product is a variant
     */
    private transient GenericValue parentProduct = null;

    private String delegatorName = null;
    private String prodCatalogId = null;
    private String productId = null;
    private String supplierProductId = null;
    private String parentProductId = null;
    private String externalId = null;
    /**
     * ends up in orderItemTypeId
     */
    private String itemType = null;
    private ShoppingCart.ShoppingCartItemGroup itemGroup = null;
    private String productCategoryId = null;
    private String itemDescription = null;
    /**
     * for reservations: date start
     */
    private Timestamp reservStart = null;
    /**
     * for reservations: length
     */
    private BigDecimal reservLength = BigDecimal.ZERO;
    /**
     * for reservations: number of persons using
     */
    private BigDecimal reservPersons = BigDecimal.ZERO;
    private String accommodationMapId = null;
    private String accommodationSpotId = null;
    private BigDecimal quantity = BigDecimal.ZERO;
    private BigDecimal basePrice = BigDecimal.ZERO;
    private BigDecimal displayPrice = null;
    private BigDecimal recurringBasePrice = null;
    private BigDecimal recurringDisplayPrice = null;
    /**
     * comes from price calc, used for special promo price promotion action
     */
    private BigDecimal specialPromoPrice = null;
    /**
     * for reservations: extra % 2nd person
     */
    private BigDecimal reserv2ndPPPerc = BigDecimal.ZERO;
    /**
     * for reservations: extra % Nth person
     */
    private BigDecimal reservNthPPPerc = BigDecimal.ZERO;
    private BigDecimal listPrice = BigDecimal.ZERO;
    /**
     * flag to know if the price have been modified
     */
    private boolean isModifiedPrice = false;
    private BigDecimal selectedAmount = BigDecimal.ZERO;
    private String requirementId = null;
    private String quoteId = null;
    private String quoteItemSeqId = null;
    // The following three optional fields are used to collect information for the OrderItemAssoc entity
    private String associatedOrderId = null;
    // the order Id, if any, to which the given item is associated (typically a sales order item can be associated to a purchase order item,
    // for example in drop shipments)
    private String associatedOrderItemSeqId = null; // the order item Id, if any, to which the given item is associated
    private String orderItemAssocTypeId = "PURCHASE_ORDER"; // the type of association between this item and an external item; by default,
    // for backward compatibility, a PURCHASE association is used (i.e. the extarnal order is a sales order and this item is a purchase order item
    // created to fulfill the sales order item
    private String statusId = null;
    private Map<String, String> orderItemAttributes = null;
    private Map<String, Object> attributes = null;
    private String orderItemSeqId = null;
    private Locale locale = null;
    private Timestamp shipBeforeDate = null;
    private Timestamp shipAfterDate = null;
    private Timestamp reserveAfterDate = null;
    private Timestamp estimatedShipDate = null;
    private Timestamp cancelBackOrderDate = null;

    private Map<String, String> contactMechIdsMap = new HashMap<>();
    private List<GenericValue> orderItemPriceInfos = null;
    private List<GenericValue> itemAdjustments = new LinkedList<>();
    private boolean isPromo = false;
    private BigDecimal promoQuantityUsed = BigDecimal.ZERO;
    private Map<GenericPK, BigDecimal> quantityUsedPerPromoCandidate = new HashMap<>();
    private Map<GenericPK, BigDecimal> quantityUsedPerPromoFailed = new HashMap<>();
    private Map<GenericPK, BigDecimal> quantityUsedPerPromoActual = new HashMap<>();
    private Map<String, GenericValue> additionalProductFeatureAndAppls = new HashMap<>();
    private List<String> alternativeOptionProductIds = null;
    private ProductConfigWrapper configWrapper = null;
    private List<GenericValue> featuresForSupplier = new LinkedList<>();

    /**
     * Clone an item.
     */
    public ShoppingCartItem(ShoppingCartItem item) {
        this.delegator = item.getDelegator();
        try {
            this.product = item.getProduct();
        } catch (IllegalStateException e) {
            this.product = null;
        }
        try {
            this.parentProduct = item.getParentProduct();
        } catch (IllegalStateException e) {
            this.parentProduct = null;
        }
        this.delegatorName = item.delegatorName;
        this.prodCatalogId = item.getProdCatalogId();
        this.productId = item.getProductId();
        this.supplierProductId = item.getSupplierProductId();
        this.parentProductId = item.getParentProductId();
        this.externalId = item.getExternalId();
        this.itemType = item.getItemType();
        this.itemGroup = item.getItemGroup();
        this.productCategoryId = item.getProductCategoryId();
        this.itemDescription = item.itemDescription;
        this.reservStart = item.getReservStart();
        this.reservLength = item.getReservLength();
        this.reservPersons = item.getReservPersons();
        this.accommodationMapId = item.getAccommodationMapId();
        this.accommodationSpotId = item.getAccommodationSpotId();
        this.quantity = item.getQuantity();
        this.setBasePrice(item.getBasePrice());
        this.setDisplayPrice(item.getDisplayPrice());
        this.setRecurringBasePrice(item.getRecurringBasePrice());
        this.setRecurringDisplayPrice(item.getRecurringDisplayPrice());
        this.setSpecialPromoPrice(item.getSpecialPromoPrice());
        this.reserv2ndPPPerc = item.getReserv2ndPPPerc();
        this.reservNthPPPerc = item.getReservNthPPPerc();
        this.listPrice = item.getListPrice();
        this.setIsModifiedPrice(item.getIsModifiedPrice());
        this.selectedAmount = item.getSelectedAmount();
        this.requirementId = item.getRequirementId();
        this.quoteId = item.getQuoteId();
        this.quoteItemSeqId = item.getQuoteItemSeqId();
        this.associatedOrderId = item.getAssociatedOrderId();
        this.associatedOrderItemSeqId = item.getAssociatedOrderItemSeqId();
        this.orderItemAssocTypeId = item.getOrderItemAssocTypeId();
        this.setStatusId(item.getStatusId());
        this.orderItemAttributes = item.getOrderItemAttributes() == null ? new HashMap<>() : new HashMap<>(item.getOrderItemAttributes());
        this.attributes = item.getAttributes() == null ? new HashMap<>() : new HashMap<>(item.getAttributes());
        this.setOrderItemSeqId(item.getOrderItemSeqId());
        this.locale = item.locale;
        this.setShipBeforeDate(item.getShipBeforeDate());
        this.setShipAfterDate(item.getShipAfterDate());
        this.setEstimatedShipDate(item.getEstimatedShipDate());
        this.setCancelBackOrderDate(item.getCancelBackOrderDate());
        this.contactMechIdsMap = item.getOrderItemContactMechIds() == null ? null : new HashMap<>(item.getOrderItemContactMechIds());
        this.orderItemPriceInfos = item.getOrderItemPriceInfos() == null ? null : new LinkedList<>(item.getOrderItemPriceInfos());
        this.itemAdjustments.addAll(item.getAdjustments());
        this.isPromo = item.getIsPromo();
        this.promoQuantityUsed = item.promoQuantityUsed;
        this.quantityUsedPerPromoCandidate = new HashMap<>(item.quantityUsedPerPromoCandidate);
        this.quantityUsedPerPromoFailed = new HashMap<>(item.quantityUsedPerPromoFailed);
        this.quantityUsedPerPromoActual = new HashMap<>(item.quantityUsedPerPromoActual);
        this.additionalProductFeatureAndAppls = item.getAdditionalProductFeatureAndAppls() == null
                ? null : new HashMap<>(item.getAdditionalProductFeatureAndAppls());
        if (item.getAlternativeOptionProductIds() != null) {
            List<String> tempAlternativeOptionProductIds = new LinkedList<>();
            tempAlternativeOptionProductIds.addAll(item.getAlternativeOptionProductIds());
            this.setAlternativeOptionProductIds(tempAlternativeOptionProductIds);
        }
        if (item.configWrapper != null) {
            this.configWrapper = new ProductConfigWrapper(item.configWrapper);
        }
        this.featuresForSupplier.addAll(item.featuresForSupplier);
    }

    /**
     * Cannot create shopping cart item with no parameters
     */
    protected ShoppingCartItem() {
    }

    /**
     * Creates new ShoppingCartItem object.
     * @deprecated Use {@link #ShoppingCartItem(GenericValue, Map, Map, String, Locale, String, ShoppingCartItemGroup, LocalDispatcher)} instead
     */
    @Deprecated
    protected ShoppingCartItem(GenericValue product, Map<String, GenericValue> additionalProductFeatureAndAppls, Map<String, Object> attributes,
                               String prodCatalogId, Locale locale, String itemType, ShoppingCart.ShoppingCartItemGroup itemGroup) {
        this(product, additionalProductFeatureAndAppls, attributes, prodCatalogId, locale, itemType, itemGroup, null);
    }

    /**
     * Creates new ShoppingCartItem object.
     * @param dispatcher TODO
     */
    protected ShoppingCartItem(GenericValue product, Map<String, GenericValue> additionalProductFeatureAndAppls, Map<String, Object> attributes,
                               String prodCatalogId, Locale locale,
                               String itemType, ShoppingCart.ShoppingCartItemGroup itemGroup, LocalDispatcher dispatcher) {
        this(product, additionalProductFeatureAndAppls, attributes, prodCatalogId, null, locale, itemType, itemGroup, null);
        String productName = ProductContentWrapper.getProductContentAsText(product, "PRODUCT_NAME", this.locale, dispatcher, "html");
        // if the productName is null or empty, see if there is an associated virtual product and get the productName of that product
        if (UtilValidate.isEmpty(productName)) {
            GenericValue parentProduct = this.getParentProduct();
            if (parentProduct != null) {
                productName = ProductContentWrapper.getProductContentAsText(parentProduct, "PRODUCT_NAME", this.locale, dispatcher, "html");
            }
        }

        if (productName == null) {
            this.itemDescription = "";
        } else {
            this.itemDescription = productName;
        }
    }

    /**
     * Creates new ShoppingCartItem object.
     */
    protected ShoppingCartItem(GenericValue product, Map<String, GenericValue> additionalProductFeatureAndAppls, Map<String, Object> attributes,
                               String prodCatalogId, ProductConfigWrapper configWrapper, Locale locale, String itemType,
                               ShoppingCart.ShoppingCartItemGroup itemGroup, GenericValue parentProduct) {
        this.product = product;
        this.productId = product.getString("productId");
        this.parentProduct = parentProduct;
        if (parentProduct != null) {
            this.parentProductId = parentProduct.getString("productId");
        }
        if (UtilValidate.isEmpty(itemType)) {
            if (UtilValidate.isNotEmpty(product.getString("productTypeId"))) {
                if ("ASSET_USAGE".equals(product.getString("productTypeId"))) {
                    this.itemType = "RENTAL_ORDER_ITEM";  // will create additional workeffort/asset usage records
                } else if ("ASSET_USAGE_OUT_IN".equals(product.getString("productTypeId"))) {
                    this.itemType = "RENTAL_ORDER_ITEM";
                } else {
                    this.itemType = "PRODUCT_ORDER_ITEM";
                }
            } else {
                // NOTE DEJ20100111: it seems safe to assume here that because a product is passed in that even if the product has no type this type
                // of item still applies; thanks to whoever wrote the previous code, that's a couple of hours tracking this down that I wouldn't
                // have minded doing something else with... :)
                this.itemType = "PRODUCT_ORDER_ITEM";
            }
        } else {
            this.itemType = itemType;
        }
        this.itemGroup = itemGroup;
        this.prodCatalogId = prodCatalogId;
        this.attributes = (attributes == null ? new HashMap<>() : attributes);
        this.delegator = product.getDelegator();
        this.delegatorName = product.getDelegator().getDelegatorName();
        this.addAllProductFeatureAndAppls(additionalProductFeatureAndAppls);
        this.locale = locale;
        if (UtilValidate.isNotEmpty(configWrapper)) {
            this.configWrapper = configWrapper;
            if (UtilValidate.isEmpty(configWrapper.getConfigId())) { //new product configuration. Persist it
                ProductConfigWorker.storeProductConfigWrapper(configWrapper, getDelegator());
            }
        }
    }

    /**
     * Creates new ShopingCartItem object.
     */
    protected ShoppingCartItem(Delegator delegator, String itemTypeId, String description, String categoryId, BigDecimal basePrice,
                               Map<String, Object> attributes, String prodCatalogId, Locale locale, ShoppingCart.ShoppingCartItemGroup itemGroup) {
        this.delegator = delegator;
        this.itemType = itemTypeId;
        this.itemGroup = itemGroup;
        this.itemDescription = description;
        this.productCategoryId = categoryId;
        if (basePrice != null) {
            this.setBasePrice(basePrice);
            this.setDisplayPrice(basePrice);
        }
        this.attributes = (attributes == null ? new HashMap<>() : attributes);
        this.prodCatalogId = prodCatalogId;
        this.delegatorName = delegator.getDelegatorName();
        this.locale = locale;
    }

    /**
     * Makes a ShoppingCartItem for a purchase order item and adds it to the cart.
     * NOTE: This method will get the product entity and check to make sure it can be purchased.
     * @param cartLocation                     The location to place this item; null will place at the end
     * @param productId                        The primary key of the product being added
     * @param quantity                         The quantity to add
     * @param additionalProductFeatureAndAppls Product feature/appls map
     * @param attributes                       All unique attributes for this item (NOT features)
     * @param prodCatalogId                    The catalog this item was added from
     * @param configWrapper                    The product configuration wrapper (null if the product is not configurable)
     * @param dispatcher                       LocalDispatcher object for doing promotions, etc
     * @param cart                             The parent shopping cart object this item will belong to
     * @param supplierProduct                  GenericValue of SupplierProduct entity, containing product description and prices
     * @param shipBeforeDate                   Request that the shipment be made before this date
     * @param shipAfterDate                    Request that the shipment be made after this date
     * @param cancelBackOrderDate              The date which if crossed causes order cancellation
     * @return a new ShoppingCartItem object
     * @throws CartItemModifyException
     */
    public static ShoppingCartItem makePurchaseOrderItem(Integer cartLocation, String productId, BigDecimal selectedAmount, BigDecimal quantity,
                                                         Map<String, GenericValue> additionalProductFeatureAndAppls, Map<String, Object> attributes,
                                                         String prodCatalogId, ProductConfigWrapper configWrapper, String itemType,
                                                         ShoppingCart.ShoppingCartItemGroup itemGroup, LocalDispatcher dispatcher, ShoppingCart cart,
                                                         GenericValue supplierProduct, Timestamp shipBeforeDate, Timestamp shipAfterDate,
                                                         Timestamp cancelBackOrderDate)
            throws CartItemModifyException, ItemNotFoundException {
        Delegator delegator = cart.getDelegator();
        GenericValue product = null;

        try {
            product = EntityQuery.use(delegator).from("Product").where("productId", productId).cache().queryOne();
        } catch (GenericEntityException e) {
            Debug.logWarning(e.toString(), MODULE);
        }

        if (product == null) {
            Map<String, Object> messageMap = UtilMisc.<String, Object>toMap("productId", productId);

            String excMsg = UtilProperties.getMessage(RES_ERROR, "item.product_not_found", messageMap, cart.getLocale());

            Debug.logWarning(excMsg, MODULE);
            throw new ItemNotFoundException(excMsg);
        }
        ShoppingCartItem newItem = new ShoppingCartItem(product, additionalProductFeatureAndAppls, attributes, prodCatalogId, configWrapper,
                cart.getLocale(), itemType, itemGroup, null);

        // check to see if product is virtual
        if ("Y".equals(product.getString("isVirtual"))) {
            Map<String, Object> messageMap = UtilMisc.<String, Object>toMap("productName", product.getString("productName"), "productId",
                    product.getString("productId"));

            String excMsg = UtilProperties.getMessage(RES_ERROR, "item.cannot_add_product_virtual", messageMap, cart.getLocale());

            Debug.logWarning(excMsg, MODULE);
            throw new CartItemModifyException(excMsg);
        }

        // check to see if the product is fully configured
        if ("AGGREGATED".equals(product.getString("productTypeId")) || "AGGREGATED_SERVICE".equals(product.getString("productTypeId"))) {
            if (configWrapper == null || !configWrapper.isCompleted()) {
                Map<String, Object> messageMap = UtilMisc.<String, Object>toMap("productName", product.getString("productName"), "productId",
                        product.getString("productId"));

                String excMsg = UtilProperties.getMessage(RES_ERROR, "item.cannot_add_product_not_configured_correctly", messageMap,
                        cart.getLocale());

                Debug.logWarning(excMsg, MODULE);
                throw new CartItemModifyException(excMsg);
            }
        }

        // add to cart before setting quantity so that we can get order total, etc
        if (cartLocation == null) {
            cart.addItemToEnd(newItem);
        } else {
            cart.addItem(cartLocation, newItem);
        }

        if (selectedAmount != null) {
            newItem.setSelectedAmount(selectedAmount);
        }

        // set the ship before/after/dates and cancel back order date.  this needs to happen before setQuantity because setQuantity causes the ship
        // group's dates to be checked versus the cart item's
        newItem.setShipBeforeDate(shipBeforeDate != null ? shipBeforeDate : cart.getDefaultShipBeforeDate());
        newItem.setShipAfterDate(shipAfterDate != null ? shipAfterDate : cart.getDefaultShipAfterDate());
        newItem.setCancelBackOrderDate(cancelBackOrderDate != null ? cancelBackOrderDate : cart.getCancelBackOrderDate());

        try {
            newItem.setQuantity(quantity, dispatcher, cart, true, false);
            cart.setItemShipGroupQty(newItem, quantity, 0);
        } catch (CartItemModifyException e) {
            cart.removeCartItem(cart.getItemIndex(newItem), dispatcher);
            cart.clearItemShipInfo(newItem);
            cart.removeEmptyCartItems();
            throw e;
        }

        // specific for purchase orders - description is set to supplierProductId + supplierProductName, price set to lastPrice of SupplierProduct
        // if supplierProduct has no supplierProductName, use the regular supplierProductId
        if (supplierProduct != null) {
            newItem.setSupplierProductId(supplierProduct.getString("supplierProductId"));
            newItem.setName(getPurchaseOrderItemDescription(product, supplierProduct, cart.getLocale(), dispatcher));
            if (newItem.getBasePrice().compareTo(BigDecimal.ZERO) == 0) {
                newItem.setBasePrice(supplierProduct.getBigDecimal("lastPrice"));
            }
        } else {
            newItem.setName(product.getString("internalName"));
        }
        return newItem;

    }

    /**
     * Makes a ShoppingCartItem and adds it to the cart.
     * NOTE: This method will get the product entity and check to make sure it can be purchased.
     * @param cartLocation The location to place this item; null will place at the end
     * @param productId The primary key of the product being added
     * @param selectedAmount Optional. Defaults to 0.0. If a selectedAmount is needed (complements the quantity value), pass it in here.
     * @param quantity Required. The quantity to add.
     * @param unitPrice Optional. Defaults to 0.0, which causes calculation of price.
     * @param reservStart Optional. The start of the reservation.
     * @param reservLength Optional. The length of the reservation.
     * @param reservPersons Optional. The number of persons taking advantage of the reservation.
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
     * @return a new ShoppingCartItem object
     * @throws CartItemModifyException
     */
    public static ShoppingCartItem makeItem(Integer cartLocation, String productId, BigDecimal selectedAmount, BigDecimal quantity,
                                            BigDecimal unitPrice,
                                            Timestamp reservStart, BigDecimal reservLength, BigDecimal reservPersons, Timestamp shipBeforeDate,
                                            Timestamp shipAfterDate,
                                            Map<String, GenericValue> additionalProductFeatureAndAppls, Map<String, Object> attributes,
                                            String prodCatalogId, ProductConfigWrapper configWrapper,
                                            String itemType, ShoppingCart.ShoppingCartItemGroup itemGroup, LocalDispatcher dispatcher,
                                            ShoppingCart cart, Boolean triggerExternalOpsBool, Boolean triggerPriceRulesBool, String parentProductId,
                                            Boolean skipInventoryChecks, Boolean skipProductChecks)
            throws CartItemModifyException, ItemNotFoundException {

        return makeItem(cartLocation, productId, selectedAmount, quantity, unitPrice,
                reservStart, reservLength, reservPersons, null, null, shipBeforeDate, shipAfterDate, null,
                additionalProductFeatureAndAppls, attributes, prodCatalogId, configWrapper,
                itemType, itemGroup, dispatcher, cart, triggerExternalOpsBool, triggerPriceRulesBool,
                parentProductId, skipInventoryChecks, skipProductChecks);

    }

    /*
     * Makes a ShoppingCartItem and adds it to the cart.
     * @param reserveAfterDate Optional.
     */
    public static ShoppingCartItem makeItem(Integer cartLocation, String productId, BigDecimal selectedAmount, BigDecimal quantity,
            BigDecimal unitPrice, Timestamp reservStart, BigDecimal reservLength, BigDecimal reservPersons, String accommodationMapId,
            String accommodationSpotId, Timestamp shipBeforeDate, Timestamp shipAfterDate,
            Map<String, GenericValue> additionalProductFeatureAndAppls, Map<String, Object> attributes, String prodCatalogId,
            ProductConfigWrapper configWrapper, String itemType, ShoppingCart.ShoppingCartItemGroup itemGroup, LocalDispatcher dispatcher,
            ShoppingCart cart, Boolean triggerExternalOpsBool, Boolean triggerPriceRulesBool, String parentProductId, Boolean skipInventoryChecks,
            Boolean skipProductChecks) throws CartItemModifyException, ItemNotFoundException {
        return makeItem(cartLocation, productId, selectedAmount, quantity, unitPrice,
                reservStart, reservLength, reservPersons, accommodationMapId, accommodationSpotId, shipBeforeDate, shipAfterDate, null,
                additionalProductFeatureAndAppls, attributes, prodCatalogId, configWrapper,
                itemType, itemGroup, dispatcher, cart, triggerExternalOpsBool, triggerPriceRulesBool,
                parentProductId, skipInventoryChecks, skipProductChecks);
    }

    /**
     * Makes a ShoppingCartItem and adds it to the cart.
     * @param accommodationMapId  Optional. reservations add into workeffort
     * @param accommodationSpotId Optional. reservations add into workeffort
     */
    public static ShoppingCartItem makeItem(Integer cartLocation, String productId, BigDecimal selectedAmount, BigDecimal quantity,
                                            BigDecimal unitPrice,
                                            Timestamp reservStart, BigDecimal reservLength, BigDecimal reservPersons, String accommodationMapId,
                                            String accommodationSpotId, Timestamp shipBeforeDate, Timestamp shipAfterDate, Timestamp reserveAfterDate,
                                            Map<String, GenericValue> additionalProductFeatureAndAppls, Map<String, Object> attributes,
                                            String prodCatalogId, ProductConfigWrapper configWrapper,
                                            String itemType, ShoppingCart.ShoppingCartItemGroup itemGroup, LocalDispatcher dispatcher,
                                            ShoppingCart cart, Boolean triggerExternalOpsBool, Boolean triggerPriceRulesBool, String parentProductId,
                                            Boolean skipInventoryChecks, Boolean skipProductChecks)
            throws CartItemModifyException, ItemNotFoundException {
        Delegator delegator = cart.getDelegator();
        GenericValue product = findProduct(delegator, skipProductChecks, prodCatalogId, productId, cart.getLocale());
        GenericValue parentProduct = null;

        if (parentProductId != null) {
            try {
                parentProduct = EntityQuery.use(delegator).from("Product").where("productId", parentProductId).cache().queryOne();
            } catch (GenericEntityException e) {
                Debug.logWarning(e.toString(), MODULE);
            }
        }
        return makeItem(cartLocation, product, selectedAmount, quantity, unitPrice,
                reservStart, reservLength, reservPersons, accommodationMapId, accommodationSpotId, shipBeforeDate, shipAfterDate, reserveAfterDate,
                additionalProductFeatureAndAppls, attributes, prodCatalogId, configWrapper,
                itemType, itemGroup, dispatcher, cart, triggerExternalOpsBool, triggerPriceRulesBool, parentProduct, skipInventoryChecks,
                skipProductChecks);
    }

    /**
     * Makes a ShoppingCartItem and adds it to the cart.
     * WARNING: This method does not check if the product is in a purchase category.
     * rental fields were added.
     * @param cartLocation The location to place this item; null will place at the end
     * @param product The product entity relating to the product being added
     * @param selectedAmount Optional. Defaults to 0.0. If a selectedAmount is needed (complements the quantity value), pass it in here.
     * @param quantity Required. The quantity to add.
     * @param unitPrice Optional. Defaults to 0.0, which causes calculation of price.
     * @param reservStart Optional. The start of the reservation.
     * @param reservLength Optional. The length of the reservation.
     * @param reservPersons Optional. The number of persons taking advantage of the reservation.
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
     * @return a new ShoppingCartItem object
     * @throws CartItemModifyException
     */
    public static ShoppingCartItem makeItem(Integer cartLocation, GenericValue product, BigDecimal selectedAmount,
                                            BigDecimal quantity, BigDecimal unitPrice, Timestamp reservStart, BigDecimal reservLength,
                                            BigDecimal reservPersons,
                                            Timestamp shipBeforeDate, Timestamp shipAfterDate, Map<String,
                                            GenericValue> additionalProductFeatureAndAppls, Map<String, Object> attributes,
                                            String prodCatalogId, ProductConfigWrapper configWrapper, String itemType,
                                            ShoppingCart.ShoppingCartItemGroup itemGroup, LocalDispatcher dispatcher,
                                            ShoppingCart cart, Boolean triggerExternalOpsBool, Boolean triggerPriceRulesBool,
                                            GenericValue parentProduct, Boolean skipInventoryChecks, Boolean skipProductChecks)
            throws CartItemModifyException {

        return makeItem(cartLocation, product, selectedAmount,
                quantity, unitPrice, reservStart, reservLength, reservPersons,
                null, null, shipBeforeDate, shipAfterDate, null, additionalProductFeatureAndAppls, attributes,
                prodCatalogId, configWrapper, itemType, itemGroup, dispatcher, cart,
                triggerExternalOpsBool, triggerPriceRulesBool, parentProduct, skipInventoryChecks, skipProductChecks);
    }

    /**
     * Makes a ShoppingCartItem and adds it to the cart.
     * @param accommodationMapId  Optional. reservations add into workeffort
     * @param accommodationSpotId Optional. reservations add into workeffort
     */
    public static ShoppingCartItem makeItem(Integer cartLocation, GenericValue product, BigDecimal selectedAmount,
                                            BigDecimal quantity, BigDecimal unitPrice, Timestamp reservStart, BigDecimal reservLength,
                                            BigDecimal reservPersons, String accommodationMapId, String accommodationSpotId,
                                            Timestamp shipBeforeDate, Timestamp shipAfterDate, Timestamp reserveAfterDate,
                                            Map<String, GenericValue> additionalProductFeatureAndAppls, Map<String, Object> attributes,
                                            String prodCatalogId, ProductConfigWrapper configWrapper, String itemType,
                                            ShoppingCart.ShoppingCartItemGroup itemGroup, LocalDispatcher dispatcher,
                                            ShoppingCart cart, Boolean triggerExternalOpsBool, Boolean triggerPriceRulesBool,
                                            GenericValue parentProduct, Boolean skipInventoryChecks, Boolean skipProductChecks)
            throws CartItemModifyException {

        ShoppingCartItem newItem = new ShoppingCartItem(product, additionalProductFeatureAndAppls, attributes, prodCatalogId, configWrapper,
                cart.getLocale(), itemType, itemGroup, parentProduct);

        selectedAmount = selectedAmount == null ? BigDecimal.ZERO : selectedAmount;
        unitPrice = unitPrice == null ? BigDecimal.ZERO : unitPrice;
        reservLength = reservLength == null ? BigDecimal.ZERO : reservLength;
        reservPersons = reservPersons == null ? BigDecimal.ZERO : reservPersons;
        boolean triggerPriceRules = triggerPriceRulesBool == null ? true : triggerPriceRulesBool;
        boolean triggerExternalOps = triggerExternalOpsBool == null ? true : triggerExternalOpsBool;

        // check to see if product is virtual
        if ("Y".equals(product.getString("isVirtual"))) {
            Map<String, Object> messageMap = UtilMisc.<String, Object>toMap("productName", product.getString("productName"), "productId",
                    product.getString("productId"));

            String excMsg = UtilProperties.getMessage(RES_ERROR, "item.cannot_add_product_virtual", messageMap, cart.getLocale());

            Debug.logWarning(excMsg, MODULE);
            throw new CartItemModifyException(excMsg);
        }

        java.sql.Timestamp nowTimestamp = UtilDateTime.nowTimestamp();

        if (!skipProductChecks) {
            isValidCartProduct(configWrapper, product, nowTimestamp, cart.getLocale());
        }

        // check to see if the product is a rental item
        if ("ASSET_USAGE".equals(product.getString("productTypeId")) || "ASSET_USAGE_OUT_IN".equals(product.getString("productTypeId"))) {
            if (reservStart == null) {
                String excMsg = UtilProperties.getMessage(RES_ERROR, "item.missing_reservation_starting_date", cart.getLocale());
                throw new CartItemModifyException(excMsg);
            }

            if (reservStart.before(UtilDateTime.nowTimestamp())) {
                String excMsg = UtilProperties.getMessage(RES_ERROR, "item.reservation_from_tomorrow", cart.getLocale());
                throw new CartItemModifyException(excMsg);
            }
            newItem.setReservStart(reservStart);

            if (reservLength.compareTo(BigDecimal.ONE) < 0) {
                String excMsg = UtilProperties.getMessage(RES_ERROR, "item.number_of_days", cart.getLocale());
                throw new CartItemModifyException(excMsg);
            }
            newItem.setReservLength(reservLength);

            if (product.get("reservMaxPersons") != null) {
                BigDecimal reservMaxPersons = product.getBigDecimal("reservMaxPersons");
                if (reservMaxPersons.compareTo(reservPersons) < 0) {
                    Map<String, Object> messageMap = UtilMisc.<String, Object>toMap("reservMaxPersons", product.getString("reservMaxPersons"),
                            "reservPersons", reservPersons);
                    String excMsg = UtilProperties.getMessage(RES_ERROR, "item.maximum_number_of_person_renting", messageMap, cart.getLocale());

                    Debug.logInfo(excMsg, MODULE);
                    throw new CartItemModifyException(excMsg);
                }
            }
            newItem.setReservPersons(reservPersons);

            if (product.get("reserv2ndPPPerc") != null) {
                newItem.setReserv2ndPPPerc(product.getBigDecimal("reserv2ndPPPerc"));
            }

            if (product.get("reservNthPPPerc") != null) {
                newItem.setReservNthPPPerc(product.getBigDecimal("reservNthPPPerc"));
            }

            if ((accommodationMapId != null) && (accommodationSpotId != null)) {
                newItem.setAccommodationId(accommodationMapId, accommodationSpotId);
            }

            // check to see if the related fixed asset is available for rent
            String isAvailable = checkAvailability(product.getString("productId"), quantity, reservStart, reservLength, cart);
            if (isAvailable.compareTo("OK") != 0) {
                Map<String, Object> messageMap = UtilMisc.<String, Object>toMap("productId", product.getString("productId"), "availableMessage",
                        isAvailable);
                String excMsg = UtilProperties.getMessage(RES_ERROR, "item.product_not_available", messageMap, cart.getLocale());
                Debug.logInfo(excMsg, MODULE);
                throw new CartItemModifyException(isAvailable);
            }
        }

        // set the ship before and after dates (defaults to cart ship before/after dates)
        newItem.setShipBeforeDate(shipBeforeDate != null ? shipBeforeDate : cart.getDefaultShipBeforeDate());
        newItem.setShipAfterDate(shipAfterDate != null ? shipAfterDate : cart.getDefaultShipAfterDate());
        newItem.setReserveAfterDate(reserveAfterDate != null ? reserveAfterDate : cart.getDefaultReserveAfterDate());

        // set the product unit price as base price
        // if triggerPriceRules is true this price will be overriden
        newItem.setBasePrice(unitPrice);

        // add to cart before setting quantity so that we can get order total, etc
        if (cartLocation == null) {
            cart.addItemToEnd(newItem);
        } else {
            cart.addItem(cartLocation, newItem);
        }

        // We have to set the selectedAmount before calling setQuantity because
        // selectedAmount changes the item's base price (used in the updatePrice
        // method called inside the setQuantity method)
        if (selectedAmount.compareTo(BigDecimal.ZERO) > 0) {
            newItem.setSelectedAmount(selectedAmount);
        }

        try {
            newItem.setQuantity(quantity, dispatcher, cart, triggerExternalOps, true, triggerPriceRules, skipInventoryChecks);
        } catch (CartItemModifyException e) {
            Debug.logWarning(e.getMessage(), MODULE);
            cart.removeCartItem(cart.getItemIndex(newItem), dispatcher);
            cart.clearItemShipInfo(newItem);
            cart.removeEmptyCartItems();
            throw e;
        }

        return newItem;
    }

    public static GenericValue findProduct(Delegator delegator, boolean skipProductChecks, String prodCatalogId,
                                           String productId, Locale locale) throws ItemNotFoundException {
        GenericValue product;

        try {
            product = EntityQuery.use(delegator).from("Product").where("productId", productId).cache().queryOne();

            // first see if there is a purchase allow category and if this product is in it or not
            String purchaseProductCategoryId = CatalogWorker.getCatalogPurchaseAllowCategoryId(delegator, prodCatalogId);
            if (!skipProductChecks && product != null && purchaseProductCategoryId != null) {
                if (!CategoryWorker.isProductInCategory(delegator, product.getString("productId"), purchaseProductCategoryId)) {
                    // a Purchase allow productCategoryId was found, but the product is not in the category, axe it...
                    Debug.logWarning("Product [" + productId + "] is not in the purchase allow category [" + purchaseProductCategoryId
                            + "] and cannot be purchased", MODULE);
                    product = null;
                }
            }
        } catch (GenericEntityException e) {
            Debug.logWarning(e.toString(), MODULE);
            product = null;
        }

        if (product == null) {
            Map<String, Object> messageMap = UtilMisc.<String, Object>toMap("productId", productId);
            String excMsg = UtilProperties.getMessage(RES_ERROR, "item.product_not_found", messageMap, locale);

            Debug.logWarning(excMsg, MODULE);
            throw new ItemNotFoundException(excMsg);
        }
        return product;
    }

    public static void isValidCartProduct(ProductConfigWrapper configWrapper, GenericValue product, Timestamp nowTimestamp, Locale locale)
            throws CartItemModifyException {
        // check to see if introductionDate hasn't passed yet
        if (product.get("introductionDate") != null && nowTimestamp.before(product.getTimestamp("introductionDate"))) {
            Map<String, Object> messageMap = UtilMisc.<String, Object>toMap("productName", product.getString("productName"),
                    "productId", product.getString("productId"));

            String excMsg = UtilProperties.getMessage(RES_ERROR, "item.cannot_add_product_not_yet_available",
                    messageMap, locale);

            Debug.logWarning(excMsg, MODULE);
            throw new CartItemModifyException(excMsg);
        }

        // check to see if salesDiscontinuationDate has passed
        if (product.get("salesDiscontinuationDate") != null && nowTimestamp.after(product.getTimestamp("salesDiscontinuationDate"))) {
            Map<String, Object> messageMap = UtilMisc.<String, Object>toMap("productName", product.getString("productName"),
                    "productId", product.getString("productId"));

            String excMsg = UtilProperties.getMessage(RES_ERROR, "item.cannot_add_product_no_longer_available",
                    messageMap, locale);

            Debug.logWarning(excMsg, MODULE);
            throw new CartItemModifyException(excMsg);
        }

        // check to see if the product is fully configured
        if ("AGGREGATED".equals(product.getString("productTypeId")) || "AGGREGATED_SERVICE".equals(product.getString("productTypeId"))) {
            if (configWrapper == null || !configWrapper.isCompleted()) {
                Map<String, Object> messageMap = UtilMisc.<String, Object>toMap("productName", product.getString("productName"),
                        "productId", product.getString("productId"));
                String excMsg = UtilProperties.getMessage(RES_ERROR, "item.cannot_add_product_not_configured_correctly",
                        messageMap, locale);
                Debug.logWarning(excMsg, MODULE);
                throw new CartItemModifyException(excMsg);
            }
        }
    }

    /**
     * Makes a non-product ShoppingCartItem and adds it to the cart.
     * NOTE: This is only for non-product items; items without a product entity (work items, bulk items, etc)
     * @param cartLocation           The location to place this item; null will place at the end
     * @param itemType               The OrderItemTypeId for the item being added
     * @param itemDescription        The optional description of the item
     * @param productCategoryId      The optional category the product *will* go in
     * @param basePrice              The price for this item
     * @param selectedAmount
     * @param quantity               The quantity to add
     * @param attributes             All unique attributes for this item (NOT features)
     * @param prodCatalogId          The catalog this item was added from
     * @param dispatcher             LocalDispatcher object for doing promotions, etc
     * @param cart                   The parent shopping cart object this item will belong to
     * @param triggerExternalOpsBool Indicates if we should run external operations (promotions, auto-save, etc)
     * @return a new ShoppingCartItem object
     * @throws CartItemModifyException
     */
    public static ShoppingCartItem makeItem(Integer cartLocation, String itemType, String itemDescription, String productCategoryId,
                                            BigDecimal basePrice, BigDecimal selectedAmount, BigDecimal quantity, Map<String, Object> attributes,
                                            String prodCatalogId, ShoppingCart.ShoppingCartItemGroup itemGroup,
                                            LocalDispatcher dispatcher, ShoppingCart cart, Boolean triggerExternalOpsBool)
            throws CartItemModifyException {

        Delegator delegator = cart.getDelegator();
        ShoppingCartItem newItem = new ShoppingCartItem(delegator, itemType, itemDescription, productCategoryId, basePrice, attributes,
                prodCatalogId, cart.getLocale(), itemGroup);

        // add to cart before setting quantity so that we can get order total, etc
        if (cartLocation == null) {
            cart.addItemToEnd(newItem);
        } else {
            cart.addItem(cartLocation, newItem);
        }

        boolean triggerExternalOps = triggerExternalOpsBool == null ? true : triggerExternalOpsBool;

        try {
            newItem.setQuantity(quantity, dispatcher, cart, triggerExternalOps);
        } catch (CartItemModifyException e) {
            cart.removeEmptyCartItems();
            throw e;
        }

        if (selectedAmount != null) {
            newItem.setSelectedAmount(selectedAmount);
        }
        return newItem;
    }

    /**
     * returns "OK" when the product can be booked or returns a string with the dates the related fixed Asset is not available
     */
    public static String checkAvailability(String productId, BigDecimal quantity, Timestamp reservStart, BigDecimal reservLength,
                                           ShoppingCart cart) {
        Delegator delegator = cart.getDelegator();
        // find related fixedAsset
        List<GenericValue> selFixedAssetProduct = null;
        GenericValue fixedAssetProduct = null;
        try {
            selFixedAssetProduct = EntityQuery.use(delegator).from("FixedAssetProduct").where("productId", productId,
                    "fixedAssetProductTypeId", "FAPT_USE").filterByDate(UtilDateTime.nowTimestamp(), "fromDate", "thruDate").queryList();
        } catch (GenericEntityException e) {
            Map<String, Object> messageMap = UtilMisc.<String, Object>toMap("productId", productId);
            String msg = UtilProperties.getMessage(RES_ERROR, "item.cannot_find_Fixed_Asset", messageMap, cart.getLocale());
            return msg;
        }
        if (UtilValidate.isNotEmpty(selFixedAssetProduct)) {
            Iterator<GenericValue> firstOne = selFixedAssetProduct.iterator();
            fixedAssetProduct = firstOne.next();
        } else {
            Map<String, Object> messageMap = UtilMisc.<String, Object>toMap("productId", productId);
            String msg = UtilProperties.getMessage(RES_ERROR, "item.cannot_find_Fixed_Asset", messageMap, cart.getLocale());
            return msg;
        }

        // find the fixed asset itself
        GenericValue fixedAsset = null;
        try {
            fixedAsset = fixedAssetProduct.getRelatedOne("FixedAsset", false);
        } catch (GenericEntityException e) {
            Map<String, Object> messageMap = UtilMisc.<String, Object>toMap("fixedAssetId", fixedAssetProduct.getString("fixedAssetId"));
            String msg = UtilProperties.getMessage(RES_ERROR, "item.fixed_Asset_not_found", messageMap, cart.getLocale());
            return msg;
        }
        if (fixedAsset == null) {
            Map<String, Object> messageMap = UtilMisc.<String, Object>toMap("fixedAssetId", fixedAssetProduct.getString("fixedAssetId"));
            String msg = UtilProperties.getMessage(RES_ERROR, "item.fixed_Asset_not_found", messageMap, cart.getLocale());
            return msg;
        }

        // see if this fixed asset has a calendar, when no create one and attach to fixed asset
        // DEJ20050725 this isn't being used anywhere, commenting out for now and not assigning from the getRelatedOne:
        // GenericValue techDataCalendar = null;
        GenericValue techDataCalendar = null;
        try {
            techDataCalendar = fixedAsset.getRelatedOne("TechDataCalendar", false);
        } catch (GenericEntityException e) {
            Debug.logWarning(e, MODULE);
        }
        if (techDataCalendar == null) {
            // no calendar ok, when not more that total capacity
            if (fixedAsset.getBigDecimal("productionCapacity").compareTo(quantity) >= 0) {
                String msg = UtilProperties.getMessage(RES_ERROR, "item.availableOk", cart.getLocale());
                return msg;
            }
            Map<String, Object> messageMap = UtilMisc.<String, Object>toMap("quantityReq", quantity,
                    "quantityAvail", fixedAsset.getString("productionCapacity"));
            String msg = UtilProperties.getMessage(RES_ERROR, "item.availableQnt", messageMap, cart.getLocale());
            return msg;
        }
        // now find all the dates and check the availabilty for each date
        // please note that calendarId is the same for (TechData) Calendar, CalendarExcDay and CalendarExWeek
        long dayCount = 0;
        String resultMessage = "";
        while (BigDecimal.valueOf(dayCount).compareTo(reservLength) < 0) {
            GenericValue techDataCalendarExcDay = null;
            // find an existing Day exception record
            Timestamp exceptionDateStartTime = new Timestamp((reservStart.getTime() + (dayCount++ * 86400000)));
            try {
                techDataCalendarExcDay = EntityQuery.use(delegator).from("TechDataCalendarExcDay").where("calendarId", fixedAsset.get("calendarId"),
                        "exceptionDateStartTime", exceptionDateStartTime).queryOne();
            } catch (GenericEntityException e) {
                Debug.logWarning(e, MODULE);
            }
            if (techDataCalendarExcDay == null) {
                if (fixedAsset.get("productionCapacity") != null && fixedAsset.getBigDecimal("productionCapacity").compareTo(quantity) < 0) {
                    resultMessage = resultMessage.concat(exceptionDateStartTime.toString().substring(0, 10) + ", ");
                }
            } else {
                // see if we can get the number of assets available
                // first try techDataCalendarExcDay(exceptionCapacity) and then FixedAsset(productionCapacity)
                // if still zero, do not check availability
                BigDecimal exceptionCapacity = BigDecimal.ZERO;
                if (techDataCalendarExcDay.get("exceptionCapacity") != null) {
                    exceptionCapacity = techDataCalendarExcDay.getBigDecimal("exceptionCapacity");
                }
                if (exceptionCapacity.compareTo(BigDecimal.ZERO) == 0 && fixedAsset.get("productionCapacity") != null) {
                    exceptionCapacity = fixedAsset.getBigDecimal("productionCapacity");
                }
                if (exceptionCapacity.compareTo(BigDecimal.ZERO) != 0) {
                    BigDecimal usedCapacity = BigDecimal.ZERO;
                    if (techDataCalendarExcDay.get("usedCapacity") != null) {
                        usedCapacity = techDataCalendarExcDay.getBigDecimal("usedCapacity");
                    }
                    if (exceptionCapacity.compareTo(quantity.add(usedCapacity)) < 0) {
                        resultMessage = resultMessage.concat(exceptionDateStartTime.toString().substring(0, 10) + ", ");
                        Debug.logInfo("No rental fixed Asset available: " + exceptionCapacity
                                + " already used: " + usedCapacity + " Requested now: " + quantity, MODULE);
                    }
                }
            }
        }
        if (resultMessage.compareTo("") == 0) {
            String msg = UtilProperties.getMessage(RES_ERROR, "item.availableOk", cart.getLocale());
            return msg;
        }
        Map<String, Object> messageMap = UtilMisc.<String, Object>toMap("resultMessage", resultMessage);
        String msg = UtilProperties.getMessage(RES_ERROR, "item.notAvailable", messageMap, cart.getLocale());
        return msg;
    }

    public static String getPurchaseOrderItemDescription(GenericValue product, GenericValue supplierProduct, Locale locale,
                                                         LocalDispatcher dispatcher) {

        String itemDescription = null;

        if (supplierProduct != null) {
            itemDescription = supplierProduct.getString("supplierProductName");
        }

        if (UtilValidate.isEmpty(itemDescription)) {
            itemDescription = ProductContentWrapper.getProductContentAsText(product, "PRODUCT_NAME", locale, dispatcher, "html");
        }

        return itemDescription;
    }

    /**
     * Gets prod catalog id.
     * @return the prod catalog id
     */
    public String getProdCatalogId() {
        return this.prodCatalogId;
    }

    /**
     * Gets external id.
     * @return the external id
     */
    public String getExternalId() {
        return this.externalId;
    }

    /**
     * Sets external id.
     * @param externalId the external id
     */
    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    /**
     * Returns the user selected amount
     */
    public BigDecimal getSelectedAmount() {
        return this.selectedAmount;
    }

    /**
     * Sets the user selected amount
     */
    public void setSelectedAmount(BigDecimal selectedAmount) {
        this.selectedAmount = selectedAmount;
    }

    /**
     * Sets accommodationId using the reservation
     */
    public void setAccommodationId(String accommodationMapId, String accommodationSpotId) {
        this.accommodationMapId = accommodationMapId;
        this.accommodationSpotId = accommodationSpotId;
    }

    /**
     * Sets the quantity for the item and validates the change in quantity, etc
     */
    public void setQuantity(BigDecimal quantity, LocalDispatcher dispatcher, ShoppingCart cart) throws CartItemModifyException {
        this.setQuantity(quantity, dispatcher, cart, true);
    }

    /**
     * Sets the quantity for the item and validates the change in quantity, etc
     */
    public void setQuantity(BigDecimal quantity, LocalDispatcher dispatcher, ShoppingCart cart, boolean triggerExternalOps)
            throws CartItemModifyException {
        this.setQuantity(quantity, dispatcher, cart, triggerExternalOps, true);
    }

    /**
     * Sets the quantity for the item and validates the change in quantity, etc
     */
    public void setQuantity(BigDecimal quantity, LocalDispatcher dispatcher, ShoppingCart cart, boolean triggerExternalOps, boolean resetShipGroup)
            throws CartItemModifyException {
        this.setQuantity(quantity, dispatcher, cart, triggerExternalOps, resetShipGroup, true, false);
    }

    /**
     * Sets the quantity for the item and validates the change in quantity, etc
     */
    public void setQuantity(BigDecimal quantity, LocalDispatcher dispatcher, ShoppingCart cart, boolean triggerExternalOps, boolean resetShipGroup,
                            boolean updateProductPrice) throws CartItemModifyException {
        this.setQuantity(quantity, dispatcher, cart, triggerExternalOps, resetShipGroup, updateProductPrice, false);
    }

    /**
     * Is inventory available or not required boolean.
     * @param quantity the quantity
     * @param productStoreId the product store id
     * @param dispatcher the dispatcher
     * @return the boolean
     * @throws CartItemModifyException the cart item modify exception
     */
    protected boolean isInventoryAvailableOrNotRequired(BigDecimal quantity, String productStoreId, LocalDispatcher dispatcher)
            throws CartItemModifyException {
        boolean inventoryAvailable = true;
        try {
            Map<String, Object> invReqResult = dispatcher.runSync("isStoreInventoryAvailableOrNotRequired",
                    UtilMisc.<String, Object>toMap("productStoreId", productStoreId, "productId", productId, "product", this.getProduct(),
                            "quantity", quantity));
            if (ServiceUtil.isError(invReqResult)) {
                String errorMessage = ServiceUtil.getErrorMessage(invReqResult);
                Debug.logError(errorMessage, MODULE);
                throw new CartItemModifyException(errorMessage);
            }
            inventoryAvailable = "Y".equals(invReqResult.get("availableOrNotRequired"));
        } catch (GenericServiceException e) {
            String errMsg = "Fatal error calling inventory checking services: " + e.toString();
            Debug.logError(e, errMsg, MODULE);
            throw new CartItemModifyException(errMsg);
        }
        return inventoryAvailable;
    }

    /**
     * Sets quantity.
     * @param quantity the quantity
     * @param dispatcher the dispatcher
     * @param cart the cart
     * @param triggerExternalOps the trigger external ops
     * @param resetShipGroup the reset ship group
     * @param updateProductPrice the update product price
     * @param skipInventoryChecks the skip inventory checks
     * @throws CartItemModifyException the cart item modify exception
     */
    protected void setQuantity(BigDecimal quantity, LocalDispatcher dispatcher, ShoppingCart cart, boolean triggerExternalOps, boolean resetShipGroup,
                               boolean updateProductPrice, boolean skipInventoryChecks) throws CartItemModifyException {
        if (this.quantity.compareTo(quantity) == 0) {
            return;
        }

        if (this.isPromo) {
            Map<String, Object> messageMap = UtilMisc.<String, Object>toMap("productName", this.getName(dispatcher), "productId", productId);
            String excMsg = UtilProperties.getMessage(RESOURCE, "OrderCannotChangeQuantityInPromotion", messageMap, cart.getLocale());
            throw new CartItemModifyException(excMsg);
        }

        // needed for inventory checking and auto-save
        String productStoreId = cart.getProductStoreId();

        if (!skipInventoryChecks && !"PURCHASE_ORDER".equals(cart.getOrderType())) {
            // check inventory if new quantity is greater than old quantity; don't worry about inventory getting pulled out from under,
            // that will be handled at checkout time
            if (product != null && quantity.compareTo(this.quantity) > 0) {
                if (!isInventoryAvailableOrNotRequired(quantity, productStoreId, dispatcher)) {
                    Map<String, Object> messageMap = UtilMisc.<String, Object>toMap("requestedQuantity",
                            UtilFormatOut.formatQuantity(quantity.doubleValue()), "productName", this.getName(dispatcher), "productId", productId);
                    String excMsg = UtilProperties.getMessage(RESOURCE, "OrderDoNotHaveEnoughProducts", messageMap, cart.getLocale());
                    Debug.logWarning(excMsg, MODULE);
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

        calcDepositAdjustments();
        if (!"PURCHASE_ORDER".equals(cart.getOrderType())) {
            // store the auto-save cart
            if (triggerExternalOps && ProductStoreWorker.autoSaveCart(delegator, productStoreId)) {
                try {
                    ShoppingListEvents.fillAutoSaveList(cart, dispatcher);
                } catch (GeneralException e) {
                    Debug.logWarning(e, UtilProperties.getMessage(RES_ERROR, "OrderUnableToStoreAutoSaveCart", locale));
                }
            }
        }

        // set the item ship group
        if (resetShipGroup) {
            int itemId = cart.getItemIndex(this);
            int shipGroupIndex = 0;
            if (itemId != -1) {
                shipGroupIndex = cart.getItemShipGroupIndex(itemId);
            }
            cart.clearItemShipInfo(this);
            cart.setItemShipGroupQty(this, quantity, shipGroupIndex);
        }
    }

    /**
     * Calc deposit adjustments.
     */
    public void calcDepositAdjustments() {
        List<GenericValue> itemAdjustments = this.getAdjustments();
        try {
            GenericValue depositAmount = EntityQuery.use(delegator).from("ProductPrice")
                    .where("productId", this.getProductId(), "productPricePurposeId", "DEPOSIT", "productPriceTypeId", "DEFAULT_PRICE")
                    .filterByDate().queryFirst();
            if (UtilValidate.isNotEmpty(depositAmount)) {
                Boolean updatedDepositAmount = false;
                BigDecimal adjustmentAmount = depositAmount.getBigDecimal("price").multiply(this.getQuantity(), GEN_ROUNDING);
                // itemAdjustments is a reference so directly setting updated amount to the same.
                for (GenericValue itemAdjustment : itemAdjustments) {
                    if ("DEPOSIT_ADJUSTMENT".equals(itemAdjustment.getString("orderAdjustmentTypeId"))) {
                        itemAdjustment.set("amount", adjustmentAmount);
                        updatedDepositAmount = true;
                    }
                }
                if (!updatedDepositAmount) {
                    GenericValue orderAdjustment = delegator.makeValue("OrderAdjustment");
                    orderAdjustment.set("orderAdjustmentTypeId", "DEPOSIT_ADJUSTMENT");
                    orderAdjustment.set("description", "Surcharge Adjustment");
                    orderAdjustment.set("amount", adjustmentAmount);
                    this.addAdjustment(orderAdjustment);
                }
            }
        } catch (GenericEntityException e) {
            Debug.logError("Error in fetching deposite price details!!", MODULE);
        }
    }

    /**
     * Update price.
     * @param dispatcher the dispatcher
     * @param cart the cart
     * @throws CartItemModifyException the cart item modify exception
     */
    public void updatePrice(LocalDispatcher dispatcher, ShoppingCart cart) throws CartItemModifyException {
        // set basePrice using the calculateProductPrice service
        if (product != null && isModifiedPrice == false) {
            try {
                Map<String, Object> priceContext = new HashMap<>();

                String partyId = cart.getPartyId();
                if (partyId != null) {
                    priceContext.put("partyId", partyId);
                }
                GenericValue userLogin = cart.getUserLogin();
                if (userLogin != null) {
                    priceContext.put("userLogin", userLogin);
                }
                GenericValue autoUserLogin = cart.getAutoUserLogin();
                if (autoUserLogin != null) {
                    priceContext.put("autoUserLogin", autoUserLogin);
                }

                // check alternative packaging
                boolean isAlternativePacking = ProductWorker.isAlternativePacking(delegator, this.productId, this.getParentProductId());
                BigDecimal pieces = BigDecimal.ONE;
                if (isAlternativePacking && UtilValidate.isNotEmpty(this.getParentProduct())) {
                    GenericValue originalProduct = this.getParentProduct();
                    if (originalProduct != null) {
                        pieces = new BigDecimal(originalProduct.getLong("piecesIncluded"));
                    }
                    priceContext.put("product", originalProduct);
                    this.parentProduct = null;
                } else {
                    priceContext.put("product", this.getProduct());
                }

                priceContext.put("quantity", this.getQuantity());
                priceContext.put("amount", this.getSelectedAmount());

                if ("PURCHASE_ORDER".equals(cart.getOrderType())) {
                    priceContext.put("currencyUomId", cart.getCurrency());
                    priceContext.put("agreementId", cart.getAgreementId());
                    Map<String, Object> priceResult = dispatcher.runSync("calculatePurchasePrice", priceContext);
                    if (ServiceUtil.isError(priceResult)) {
                        String errorMessage = ServiceUtil.getErrorMessage(priceResult);
                        Debug.logError(errorMessage, MODULE);
                        throw new CartItemModifyException("There was an error while calculating the price: "
                                + ServiceUtil.getErrorMessage(priceResult));
                    }
                    Boolean validPriceFound = (Boolean) priceResult.get("validPriceFound");
                    if (!validPriceFound) {
                        throw new CartItemModifyException("Could not find a valid price for the product with ID [" + this.getProductId()
                                + "] and supplier with ID [" + partyId + "], not adding to cart.");
                    }

                    if (isAlternativePacking) {
                        this.setBasePrice(((BigDecimal) priceResult.get("price")).divide(pieces, RoundingMode.HALF_UP));
                    } else {
                        this.setBasePrice(((BigDecimal) priceResult.get("price")));
                    }

                    this.setDisplayPrice(this.basePrice);
                    this.orderItemPriceInfos = UtilGenerics.cast(priceResult.get("orderItemPriceInfos"));
                } else {
                    if (productId != null) {
                        String productStoreId = cart.getProductStoreId();
                        List<GenericValue> productSurvey = ProductStoreWorker.getProductSurveys(delegator, productStoreId, productId, "CART_ADD",
                                parentProductId);
                        if (UtilValidate.isNotEmpty(productSurvey) && UtilValidate.isNotEmpty(attributes)) {
                            List<String> surveyResponses = UtilGenerics.cast(attributes.get("surveyResponses"));
                            if (UtilValidate.isNotEmpty(surveyResponses)) {
                                for (String surveyResponseId : surveyResponses) {
                                    // TODO: implement multiple survey per product
                                    if (UtilValidate.isNotEmpty(surveyResponseId)) {
                                        priceContext.put("surveyResponseId", surveyResponseId);
                                        break;
                                    }
                                }
                            }
                        }
                    }
                    if ("true".equals(EntityUtilProperties.getPropertyValue("catalog", "convertProductPriceCurrency", delegator))) {
                        priceContext.put("currencyUomIdTo", cart.getCurrency());
                    } else {
                        priceContext.put("currencyUomId", cart.getCurrency());
                    }
                    priceContext.put("prodCatalogId", this.getProdCatalogId());
                    priceContext.put("webSiteId", cart.getWebSiteId());
                    priceContext.put("productStoreId", cart.getProductStoreId());
                    priceContext.put("agreementId", cart.getAgreementId());
                    priceContext.put("productPricePurposeId", "PURCHASE");
                    priceContext.put("checkIncludeVat", "Y");

                    // check if a survey is associated with the item and add to the price calculation
                    List<String> surveyResponses = UtilGenerics.cast(getAttribute("surveyResponses"));
                    if (UtilValidate.isNotEmpty(surveyResponses)) {
                        priceContext.put("surveyResponseId", surveyResponses.get(0));
                    }

                    Map<String, Object> priceResult = dispatcher.runSync("calculateProductPrice", priceContext);
                    if (ServiceUtil.isError(priceResult)) {
                        String errorMessage = ServiceUtil.getErrorMessage(priceResult);
                        Debug.logError(errorMessage, MODULE);
                        throw new CartItemModifyException("There was an error while calculating the price: "
                                + ServiceUtil.getErrorMessage(priceResult));
                    }
                    Boolean validPriceFound = (Boolean) priceResult.get("validPriceFound");
                    if (Boolean.FALSE.equals(validPriceFound)) {
                        throw new CartItemModifyException("Could not find a valid price for the product with ID [" + this.getProductId()
                                + "], not adding to cart.");
                    }

                    //set alternative product price
                    if (isAlternativePacking) {
                        int decimals = 2;
                        if (priceResult.get("listPrice") != null) {
                            this.listPrice = ((BigDecimal) priceResult.get("listPrice")).divide(pieces, decimals, RoundingMode.HALF_UP);
                        }

                        if (priceResult.get("basePrice") != null) {
                            this.setBasePrice(((BigDecimal) priceResult.get("basePrice")).divide(pieces, decimals, RoundingMode.HALF_UP));
                        }

                        if (priceResult.get("price") != null) {
                            this.setDisplayPrice(((BigDecimal) priceResult.get("price")).divide(pieces, decimals, RoundingMode.HALF_UP));
                        }

                        if (priceResult.get("specialPromoPrice") != null) {
                            this.setSpecialPromoPrice(((BigDecimal) priceResult.get("specialPromoPrice")).divide(pieces, decimals,
                                    RoundingMode.HALF_UP));
                        }
                    } else {
                        if (priceResult.get("listPrice") != null) {
                            this.listPrice = ((BigDecimal) priceResult.get("listPrice"));
                        }

                        if (priceResult.get("basePrice") != null) {
                            this.setBasePrice(((BigDecimal) priceResult.get("basePrice")));
                        }

                        if (priceResult.get("price") != null) {
                            this.setDisplayPrice(((BigDecimal) priceResult.get("price")));
                        }

                        this.setSpecialPromoPrice((BigDecimal) priceResult.get("specialPromoPrice"));
                    }

                    this.orderItemPriceInfos = UtilGenerics.cast(priceResult.get("orderItemPriceInfos"));

                    // If product is configurable, the price is taken from the configWrapper.
                    if (configWrapper != null) {
                        // TODO: for configurable products need to do something to make them VAT aware...
                        //  for now base and display prices are the same
                        this.setBasePrice(configWrapper.getTotalPrice());
                        // Check if price display with taxes
                        GenericValue productStore = ProductStoreWorker.getProductStore(cart.getProductStoreId(), delegator);
                        if (productStore != null && "Y".equals(productStore.get("showPricesWithVatTax"))) {
                            BigDecimal totalPrice = configWrapper.getTotalPrice();
                            // Get Taxes
                            Map<String, Object> totalPriceWithTaxMap = dispatcher.runSync("calcTaxForDisplay", UtilMisc.toMap("basePrice",
                                    totalPrice, "productId", this.productId, "productStoreId", cart.getProductStoreId()));
                            if (ServiceUtil.isError(totalPriceWithTaxMap)) {
                                String errorMessage = ServiceUtil.getErrorMessage(totalPriceWithTaxMap);
                                Debug.logError(errorMessage, MODULE);
                                throw new CartItemModifyException("There was an error while calculating tax: "
                                        + ServiceUtil.getErrorMessage(priceResult));
                            }
                            this.setDisplayPrice((BigDecimal) totalPriceWithTaxMap.get("priceWithTax"));
                        } else {
                            this.setDisplayPrice(configWrapper.getTotalPrice());
                        }
                    }

                    // no try to do a recurring price calculation; not all products have recurring prices so may be null
                    Map<String, Object> recurringPriceContext = new HashMap<>();
                    recurringPriceContext.putAll(priceContext);
                    recurringPriceContext.put("productPricePurposeId", "RECURRING_CHARGE");
                    Map<String, Object> recurringPriceResult = dispatcher.runSync("calculateProductPrice", recurringPriceContext);
                    if (ServiceUtil.isError(recurringPriceResult)) {
                        String errorMessage = ServiceUtil.getErrorMessage(recurringPriceResult);
                        Debug.logError(errorMessage, MODULE);
                        throw new CartItemModifyException("There was an error while calculating price: " + ServiceUtil.getErrorMessage(priceResult));
                    }
                    // for the recurring price only set the values iff validPriceFound is true
                    Boolean validRecurringPriceFound = (Boolean) recurringPriceResult.get("validPriceFound");
                    if (Boolean.TRUE.equals(validRecurringPriceFound)) {
                        if (recurringPriceResult.get("basePrice") != null) {
                            this.setRecurringBasePrice((BigDecimal) recurringPriceResult.get("basePrice"));
                        }
                        if (recurringPriceResult.get("price") != null) {
                            this.setRecurringDisplayPrice((BigDecimal) recurringPriceResult.get("price"));
                        }
                    }
                }
            } catch (GenericServiceException e) {
                throw new CartItemModifyException("There was an error while calculating the price", e);
            }
        }
    }

    /**
     * Returns the quantity.
     */
    public BigDecimal getQuantity() {
        return this.quantity;
    }

    /**
     * Returns the reservation start date.
     */
    public Timestamp getReservStart() {
        return this.getReservStart(BigDecimal.ZERO);
    }

    /**
     * Sets the reservation start date
     */
    public void setReservStart(Timestamp reservStart) {
        this.reservStart = reservStart != null ? (Timestamp) reservStart.clone() : null;
    }

    /**
     * Returns the reservation start date with a number of days added.
     */
    public Timestamp getReservStart(BigDecimal addDays) {
        if (addDays.compareTo(BigDecimal.ZERO) == 0) {
            return this.reservStart != null ? (Timestamp) this.reservStart.clone() : null;
        }
        if (this.reservStart != null) {
            return new Timestamp((long) (this.reservStart.getTime() + (addDays.doubleValue() * 86400000.0)));
        }
        return null;
    }

    /**
     * Returns the reservation length.
     */
    public BigDecimal getReservLength() {
        return this.reservLength;
    }

    /**
     * Sets the reservation length
     */
    public void setReservLength(BigDecimal reservLength) {
        this.reservLength = reservLength;
    }

    /**
     * Returns the reservation number of persons.
     */
    public BigDecimal getReservPersons() {
        return this.reservPersons;
    }

    /**
     * Sets number of persons using the reservation
     */
    public void setReservPersons(BigDecimal reservPersons) {
        this.reservPersons = reservPersons;
    }

    /**
     * Returns accommodationMapId
     */
    public String getAccommodationMapId() {
        return this.accommodationMapId;
    }

    /**
     * Returns accommodationSpotId
     */
    public String getAccommodationSpotId() {
        return this.accommodationSpotId;
    }

    /**
     * Gets promo quantity used.
     * @return the promo quantity used
     */
    public synchronized BigDecimal getPromoQuantityUsed() {
        if (this.getIsPromo()) {
            return this.quantity;
        }
        return this.promoQuantityUsed;
    }

    /**
     * Gets promo quantity available.
     * @return the promo quantity available
     */
    public synchronized BigDecimal getPromoQuantityAvailable() {
        if (this.getIsPromo()) {
            return BigDecimal.ZERO;
        }
        return this.quantity.subtract(this.promoQuantityUsed);
    }

    /**
     * Gets quantity used per promo actual iter.
     * @return the quantity used per promo actual iter
     */
    public Iterator<Map.Entry<GenericPK, BigDecimal>> getQuantityUsedPerPromoActualIter() {
        return this.quantityUsedPerPromoActual.entrySet().iterator();
    }

    /**
     * Gets quantity used per promo candidate iter.
     * @return the quantity used per promo candidate iter
     */
    public Iterator<Map.Entry<GenericPK, BigDecimal>> getQuantityUsedPerPromoCandidateIter() {
        return this.quantityUsedPerPromoCandidate.entrySet().iterator();
    }

    /**
     * Gets quantity used per promo failed iter.
     * @return the quantity used per promo failed iter
     */
    public Iterator<Map.Entry<GenericPK, BigDecimal>> getQuantityUsedPerPromoFailedIter() {
        return this.quantityUsedPerPromoFailed.entrySet().iterator();
    }

    /**
     * Add promo quantity candidate use big decimal.
     * @param quantityDesired the quantity desired
     * @param productPromoCondAction the product promo cond action
     * @param checkAvailableOnly the check available only
     * @return the big decimal
     */
    public synchronized BigDecimal addPromoQuantityCandidateUse(BigDecimal quantityDesired, GenericValue productPromoCondAction,
                                                                boolean checkAvailableOnly) {
        if (quantityDesired.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal promoQuantityAvailable = this.getPromoQuantityAvailable();
        BigDecimal promoQuantityToUse = quantityDesired;
        if (promoQuantityAvailable.compareTo(BigDecimal.ZERO) > 0) {
            if (promoQuantityToUse.compareTo(promoQuantityAvailable) > 0) {
                promoQuantityToUse = promoQuantityAvailable;
            }

            if (!checkAvailableOnly) {
                // keep track of candidate promo uses on cartItem
                GenericPK productPromoCondActionPK = productPromoCondAction.getPrimaryKey();
                BigDecimal existingValue = this.quantityUsedPerPromoCandidate.get(productPromoCondActionPK);
                if (existingValue == null) {
                    this.quantityUsedPerPromoCandidate.put(productPromoCondActionPK, promoQuantityToUse);
                } else {
                    this.quantityUsedPerPromoCandidate.put(productPromoCondActionPK, promoQuantityToUse.add(existingValue));
                }

                this.promoQuantityUsed = this.promoQuantityUsed.add(promoQuantityToUse);
            }

            return promoQuantityToUse;
        }
        return BigDecimal.ZERO;
    }

    /**
     * Gets promo quantity candidate use.
     * @param productPromoCondAction the product promo cond action
     * @return the promo quantity candidate use
     */
    public BigDecimal getPromoQuantityCandidateUse(GenericValue productPromoCondAction) {
        GenericPK productPromoCondActionPK = productPromoCondAction.getPrimaryKey();
        BigDecimal existingValue = this.quantityUsedPerPromoCandidate.get(productPromoCondActionPK);
        if (existingValue == null) {
            return BigDecimal.ZERO;
        }
        return existingValue;
    }

    /**
     * Gets promo quantity candidate use action and all conds.
     * @param productPromoAction the product promo action
     * @return the promo quantity candidate use action and all conds
     */
    public BigDecimal getPromoQuantityCandidateUseActionAndAllConds(GenericValue productPromoAction) {
        BigDecimal totalUse = BigDecimal.ZERO;
        String productPromoId = productPromoAction.getString("productPromoId");
        String productPromoRuleId = productPromoAction.getString("productPromoRuleId");

        GenericPK productPromoActionPK = productPromoAction.getPrimaryKey();
        BigDecimal existingValue = this.quantityUsedPerPromoCandidate.get(productPromoActionPK);
        if (existingValue != null) {
            totalUse = existingValue;
        }

        for (Map.Entry<GenericPK, BigDecimal> entry : this.quantityUsedPerPromoCandidate.entrySet()) {
            GenericPK productPromoCondActionPK = entry.getKey();
            BigDecimal quantityUsed = entry.getValue();
            if (quantityUsed != null) {
                // must be in the same rule and be a condition
                if (productPromoId.equals(productPromoCondActionPK.getString("productPromoId"))
                        && productPromoRuleId.equals(productPromoCondActionPK.getString("productPromoRuleId"))
                        && productPromoCondActionPK.containsKey("productPromoCondSeqId")) {
                    totalUse = totalUse.add(quantityUsed);
                }
            }
        }

        return totalUse;
    }

    /**
     * Reset promo rule use.
     * @param productPromoId     the product promo id
     * @param productPromoRuleId the product promo rule id
     */
    public synchronized void resetPromoRuleUse(String productPromoId, String productPromoRuleId) {
        Iterator<Map.Entry<GenericPK, BigDecimal>> entryIter = this.quantityUsedPerPromoCandidate.entrySet().iterator();
        while (entryIter.hasNext()) {
            Map.Entry<GenericPK, BigDecimal> entry = entryIter.next();
            GenericPK productPromoCondActionPK = entry.getKey();
            BigDecimal quantityUsed = entry.getValue();
            if (productPromoId.equals(productPromoCondActionPK.getString("productPromoId"))
                    && productPromoRuleId.equals(productPromoCondActionPK.getString("productPromoRuleId"))) {
                entryIter.remove();
                BigDecimal existingValue = this.quantityUsedPerPromoFailed.get(productPromoCondActionPK);
                if (existingValue == null) {
                    this.quantityUsedPerPromoFailed.put(productPromoCondActionPK, quantityUsed);
                } else {
                    this.quantityUsedPerPromoFailed.put(productPromoCondActionPK, quantityUsed.add(existingValue));
                }
                this.promoQuantityUsed = this.promoQuantityUsed.subtract(quantityUsed);
            }
        }
    }

    /**
     * Confirm promo rule use.
     * @param productPromoId     the product promo id
     * @param productPromoRuleId the product promo rule id
     */
    public synchronized void confirmPromoRuleUse(String productPromoId, String productPromoRuleId) {
        Iterator<Map.Entry<GenericPK, BigDecimal>> entryIter = this.quantityUsedPerPromoCandidate.entrySet().iterator();
        while (entryIter.hasNext()) {
            Map.Entry<GenericPK, BigDecimal> entry = entryIter.next();
            GenericPK productPromoCondActionPK = entry.getKey();
            BigDecimal quantityUsed = entry.getValue();
            if (productPromoId.equals(productPromoCondActionPK.getString("productPromoId"))
                    && productPromoRuleId.equals(productPromoCondActionPK.getString("productPromoRuleId"))) {
                entryIter.remove();
                BigDecimal existingValue = this.quantityUsedPerPromoActual.get(productPromoCondActionPK);
                if (existingValue == null) {
                    this.quantityUsedPerPromoActual.put(productPromoCondActionPK, quantityUsed);
                } else {
                    this.quantityUsedPerPromoActual.put(productPromoCondActionPK, quantityUsed.add(existingValue));
                }
            }
        }
    }

    /**
     * Clear promo rule use info.
     */
    public synchronized void clearPromoRuleUseInfo() {
        this.quantityUsedPerPromoActual.clear();
        this.quantityUsedPerPromoCandidate.clear();
        this.quantityUsedPerPromoFailed.clear();
        this.promoQuantityUsed = this.getIsPromo() ? this.quantity : BigDecimal.ZERO;
    }

    /**
     * Returns the item's comment.
     */
    public String getItemComment() {
        return (String) this.getAttribute("itemComment");
    }

    /**
     * Sets the item comment.
     */
    public void setItemComment(String itemComment) {
        this.setAttribute("itemComment", itemComment);
    }

    /**
     * Returns the item's customer desired delivery date.
     */
    public Timestamp getDesiredDeliveryDate() {
        String ddDate = (String) this.getAttribute("itemDesiredDeliveryDate");

        if (ddDate != null) {
            try {
                return Timestamp.valueOf(ddDate);
            } catch (IllegalArgumentException e) {
                Debug.logWarning(e, UtilProperties.getMessage(RES_ERROR, "OrderProblemGettingItemDesiredDeliveryDateFor",
                        UtilMisc.toMap("productId", this.getProductId()), locale));
                return null;
            }
        }
        return null;
    }

    /**
     * Sets the item's customer desired delivery date.
     */
    public void setDesiredDeliveryDate(Timestamp ddDate) {
        if (ddDate != null) {
            this.setAttribute("itemDesiredDeliveryDate", ddDate.toString());
        }
    }

    /**
     * Returns the date to ship before
     */
    public Timestamp getShipBeforeDate() {
        return this.shipBeforeDate != null ? (Timestamp) this.shipBeforeDate.clone() : null;
    }

    /**
     * Sets the date to ship before
     */
    public void setShipBeforeDate(Timestamp date) {
        this.shipBeforeDate = date != null ? (Timestamp) date.clone() : null;

    }

    /**
     * Returns the date to ship after
     */
    public Timestamp getShipAfterDate() {
        return this.shipAfterDate != null ? (Timestamp) this.shipAfterDate.clone() : null;
    }

    /**
     * Sets the date to ship after
     */
    public void setShipAfterDate(Timestamp date) {
        this.shipAfterDate = date != null ? (Timestamp) date.clone() : null;
    }

    /**
     * Returns the date to ship after
     */
    public Timestamp getReserveAfterDate() {
        return this.reserveAfterDate != null ? (Timestamp) this.reserveAfterDate.clone() : null;
    }

    /**
     * Sets the date to ship after
     */
    public void setReserveAfterDate(Timestamp date) {
        this.reserveAfterDate = date != null ? (Timestamp) date.clone() : null;
    }

    /**
     * Returns the cancel back order date
     */
    public Timestamp getCancelBackOrderDate() {
        return this.cancelBackOrderDate != null ? (Timestamp) this.cancelBackOrderDate.clone() : null;
    }

    /**
     * Sets the cancel back order date
     */
    public void setCancelBackOrderDate(Timestamp date) {
        this.cancelBackOrderDate = date != null ? (Timestamp) date.clone() : null;
    }

    /**
     * Returns the date to EstimatedShipDate
     */
    public Timestamp getEstimatedShipDate() {
        return this.estimatedShipDate != null ? (Timestamp) this.estimatedShipDate.clone() : null;
    }

    /**
     * Sets the date to EstimatedShipDate
     */
    public void setEstimatedShipDate(Timestamp date) {
        this.estimatedShipDate = date != null ? (Timestamp) date.clone() : null;
    }

    /**
     * Returns the item type.
     */
    public String getItemType() {
        return this.itemType;
    }

    /**
     * Sets the item type.
     */
    public void setItemType(String itemType) {
        this.itemType = itemType;
    }

    /**
     * Returns the item type.
     */
    public GenericValue getItemTypeGenericValue() {
        try {
            return this.getDelegator().findOne("OrderItemType", UtilMisc.toMap("orderItemTypeId", this.itemType), true);
        } catch (GenericEntityException e) {
            Debug.logError(e, "Error getting ShippingCartItem's OrderItemType", MODULE);
            return null;
        }
    }

    /**
     * Sets the item group.
     */
    public void setItemGroup(String groupNumber, ShoppingCart cart) {
        this.itemGroup = cart.getItemGroupByNumber(groupNumber);
    }

    /**
     * Returns the item group.
     */
    public ShoppingCart.ShoppingCartItemGroup getItemGroup() {
        return this.itemGroup;
    }

    /**
     * Sets the item group.
     */
    public void setItemGroup(ShoppingCart.ShoppingCartItemGroup itemGroup) {
        this.itemGroup = itemGroup;
    }

    /**
     * Is in item group boolean.
     * @param groupNumber the group number
     * @return the boolean
     */
    public boolean isInItemGroup(String groupNumber) {
        return !(this.itemGroup == null) && this.itemGroup.getGroupNumber().equals(groupNumber);
    }

    /**
     * Returns the item type description.
     */
    public String getItemTypeDescription() {
        GenericValue orderItemType = null;
        if (this.getItemType() != null) {
            try {
                orderItemType = this.getDelegator().findOne("OrderItemType", UtilMisc.toMap("orderItemTypeId", this.getItemType()), true);
            } catch (GenericEntityException e) {
                Debug.logWarning(e, UtilProperties.getMessage(RES_ERROR, "OrderProblemsGettingOrderItemTypeFor",
                        UtilMisc.toMap("orderItemTypeId", this.getItemType()), locale));
            }
        }
        if (orderItemType != null) {
            return orderItemType.getString("description");
        }
        return null;
    }

    /**
     * Returns the productCategoryId for the item or null if none.
     */
    public String getProductCategoryId() {
        return this.productCategoryId;
    }

    /**
     * Sets product category id.
     * @param productCategoryId the product category id
     */
    public void setProductCategoryId(String productCategoryId) {
        this.productCategoryId = productCategoryId;
    }

    /**
     * Gets order item seq id.
     * @return the order item seq id
     */
    public String getOrderItemSeqId() {
        return orderItemSeqId;
    }

    /**
     * Sets order item seq id.
     * @param orderItemSeqId the order item seq id
     */
    public void setOrderItemSeqId(String orderItemSeqId) {
        Debug.logInfo("Setting orderItemSeqId - " + orderItemSeqId, MODULE);
        this.orderItemSeqId = orderItemSeqId;
    }

    /**
     * Sets shopping list.
     * @param shoppingListId the shopping list id
     * @param itemSeqId the item seq id
     */
    public void setShoppingList(String shoppingListId, String itemSeqId) {
        attributes.put("shoppingListId", shoppingListId);
        attributes.put("shoppingListItemSeqId", itemSeqId);
    }

    /**
     * Gets shopping list id.
     * @return the shopping list id
     */
    public String getShoppingListId() {
        return (String) attributes.get("shoppingListId");
    }

    /**
     * Gets shopping list item seq id.
     * @return the shopping list item seq id
     */
    public String getShoppingListItemSeqId() {
        return (String) attributes.get("shoppingListItemSeqId");
    }

    /**
     * Returns the requirementId.
     */
    public String getRequirementId() {
        return this.requirementId;
    }

    /**
     * Sets the requirementId.
     */
    public void setRequirementId(String requirementId) {
        this.requirementId = requirementId;
    }

    /**
     * Returns the quoteId.
     */
    public String getQuoteId() {
        return this.quoteId;
    }

    /**
     * Sets the quoteId.
     */
    public void setQuoteId(String quoteId) {
        this.quoteId = quoteId;
    }

    /**
     * Returns the quoteItemSeqId.
     */
    public String getQuoteItemSeqId() {
        return this.quoteItemSeqId;
    }

    /**
     * Sets the quoteItemSeqId.
     */
    public void setQuoteItemSeqId(String quoteItemSeqId) {
        this.quoteItemSeqId = quoteItemSeqId;
    }

    /**
     * Returns the OrderItemAssocTypeId.
     */
    public String getOrderItemAssocTypeId() {
        return this.orderItemAssocTypeId;
    }

    /**
     * Sets the orderItemAssocTypeId.
     */
    public void setOrderItemAssocTypeId(String orderItemAssocTypeId) {
        if (orderItemAssocTypeId != null) {
            this.orderItemAssocTypeId = orderItemAssocTypeId;
        }
    }

    /**
     * Returns the associatedId.
     */
    public String getAssociatedOrderId() {
        return this.associatedOrderId;
    }

    /**
     * Sets the associatedOrderId.
     */
    public void setAssociatedOrderId(String associatedOrderId) {
        this.associatedOrderId = associatedOrderId;
    }

    /**
     * Returns the associatedOrderItemSeqId.
     */
    public String getAssociatedOrderItemSeqId() {
        return this.associatedOrderItemSeqId;
    }

    /**
     * Sets the associatedOrderItemSeqId.
     */
    public void setAssociatedOrderItemSeqId(String associatedOrderItemSeqId) {
        this.associatedOrderItemSeqId = associatedOrderItemSeqId;
    }

    /**
     * Gets status id.
     * @return the status id
     */
    public String getStatusId() {
        return this.statusId;
    }

    /**
     * Sets status id.
     * @param statusId the status id
     */
    public void setStatusId(String statusId) {
        this.statusId = statusId;
    }

    /**
     * Returns true if shipping charges apply to this item.
     */
    public boolean shippingApplies() {
        GenericValue product = getProduct();
        if (product != null) {
            return ProductWorker.shippingApplies(product);
        }
        // we don't ship non-product items
        return false;
    }

    /**
     * Returns true if tax charges apply to this item.
     */
    public boolean taxApplies() {
        GenericValue product = getProduct();
        if (product != null) {
            return ProductWorker.taxApplies(product);
        }
        // we do tax non-product items
        return true;
    }

    /** Returns the item's productId. */
    public String getProductId() {
        return productId;
    }

    /** Returns the item's supplierProductId. */
    public String getSupplierProductId() {
        return supplierProductId;
    }

    /** Set the item's supplierProductId. */
    public void setSupplierProductId(String supplierProductId) {
        this.supplierProductId = supplierProductId;
    }

    /** Set the item's locale (from ShoppingCart.setLocale) */
    protected void setLocale(Locale locale) {
        this.locale = locale;
    }

    /**
     * Returns the item's description.
     * @deprecated use getName(LocalDispatcher dispatcher)
     **/
    @Deprecated
    public String getName() {
        return itemDescription;
    }

    /**
     * Set the item's description.
     */
    public void setName(String itemName) {
        this.itemDescription = itemName;
    }

    /**
     * Returns the item's description or PRODUCT_NAME from content.
     */
    public String getName(LocalDispatcher dispatcher) {
        if (itemDescription != null) {
            return itemDescription;
        }
        GenericValue product = getProduct();
        if (product != null) {
            String productName = ProductContentWrapper.getProductContentAsText(product, "PRODUCT_NAME", this.locale, dispatcher, "html");
            // if the productName is null or empty, see if there is an associated virtual product and get the productName of that product
            if (UtilValidate.isEmpty(productName)) {
                GenericValue parentProduct = this.getParentProduct();
                if (parentProduct != null) {
                    productName = ProductContentWrapper.getProductContentAsText(parentProduct, "PRODUCT_NAME", this.locale, dispatcher, "html");
                }
            }
            if (productName == null) {
                return "";
            }
            return productName;
        }
        return "";
    }

    /**
     * Returns the item's description.
     */
    public String getDescription(LocalDispatcher dispatcher) {
        GenericValue product = getProduct();

        if (product != null) {
            String description = ProductContentWrapper.getProductContentAsText(product, "DESCRIPTION", this.locale, dispatcher, "html");

            // if the description is null or empty, see if there is an associated virtual product and get the description of that product
            if (UtilValidate.isEmpty(description)) {
                GenericValue parentProduct = this.getParentProduct();
                if (parentProduct != null) {
                    description = ProductContentWrapper.getProductContentAsText(parentProduct, "DESCRIPTION", this.locale, dispatcher, "html");
                }
            }

            if (description == null) {
                return "";
            }
            return description;
        }
        return null;
    }

    /**
     * Gets config wrapper.
     * @return the config wrapper
     */
    public ProductConfigWrapper getConfigWrapper() {
        return configWrapper;
    }

    /**
     * Returns the item's unit weight
     */
    public BigDecimal getWeight() {
        GenericValue product = getProduct();
        if (product != null) {
            BigDecimal weight = product.getBigDecimal("productWeight");

            // if the weight is null, see if there is an associated virtual product and get the weight of that product
            if (weight == null) {
                GenericValue parentProduct = this.getParentProduct();
                if (parentProduct != null) {
                    weight = parentProduct.getBigDecimal("productWeight");
                }
            }

            if (weight == null) {
                return BigDecimal.ZERO;
            }
            return weight;
        }
        // non-product items have 0 weight
        return BigDecimal.ZERO;
    }

    /**
     * Returns the item's pieces included
     */
    public long getPiecesIncluded() {
        GenericValue product = getProduct();
        if (product != null) {
            Long pieces = product.getLong("piecesIncluded");

            // if the piecesIncluded is null, see if there is an associated virtual product and get the piecesIncluded of that product
            if (pieces == null) {
                GenericValue parentProduct = this.getParentProduct();
                if (parentProduct != null) {
                    pieces = parentProduct.getLong("piecesIncluded");
                }
            }

            if (pieces == null) {
                return 1;
            }
            return pieces;
        }
        // non-product item assumed 1 piece
        return 1;
    }

    /**
     * Returns a Set of the item's features
     */
    public Set<String> getFeatureSet() {
        Set<String> featureSet = new LinkedHashSet<>();
        GenericValue product = this.getProduct();
        if (product != null) {
            List<GenericValue> featureAppls = null;
            try {
                featureAppls = product.getRelated("ProductFeatureAppl", null, null, false);
                List<EntityExpr> filterExprs = UtilMisc.toList(EntityCondition.makeCondition("productFeatureApplTypeId",
                        EntityOperator.EQUALS, "STANDARD_FEATURE"));
                filterExprs.add(EntityCondition.makeCondition("productFeatureApplTypeId", EntityOperator.EQUALS, "REQUIRED_FEATURE"));
                featureAppls = EntityUtil.filterByOr(featureAppls, filterExprs);
            } catch (GenericEntityException e) {
                Debug.logError(e, "Unable to get features from product : " + product.get("productId"), MODULE);
            }
            if (featureAppls != null) {
                for (GenericValue appl : featureAppls) {
                    featureSet.add(appl.getString("productFeatureId"));
                }
            }
        }
        if (this.additionalProductFeatureAndAppls != null) {
            for (GenericValue appl : this.additionalProductFeatureAndAppls.values()) {
                featureSet.add(appl.getString("productFeatureId"));
            }
        }
        return featureSet;
    }

    /**
     * Returns a list of the item's standard features
     */
    public List<GenericValue> getStandardFeatureList() {
        List<GenericValue> features = null;
        GenericValue product = this.getProduct();
        if (product != null) {
            try {
                List<GenericValue> featureAppls = product.getRelated("ProductFeatureAndAppl", null, null, false);
                features = EntityUtil.filterByAnd(featureAppls, UtilMisc.toMap("productFeatureApplTypeId", "STANDARD_FEATURE"));
            } catch (GenericEntityException e) {
                Debug.logError(e, "Unable to get features from product : " + product.get("productId"), MODULE);
            }
        }
        return features;
    }

    /**
     * Returns a List of the item's features for supplier
     */
    public List<GenericValue> getFeaturesForSupplier(LocalDispatcher dispatcher, String partyId) {
        List<GenericValue> featureAppls = getStandardFeatureList();
        if (UtilValidate.isNotEmpty(featureAppls)) {
            try {
                Map<String, Object> result = dispatcher.runSync("convertFeaturesForSupplier", UtilMisc.toMap("partyId", partyId,
                        "productFeatures", featureAppls));
                if (ServiceUtil.isError(result)) {
                    String errorMessage = ServiceUtil.getErrorMessage(result);
                    Debug.logError(errorMessage, MODULE);
                }
                featuresForSupplier = UtilGenerics.cast(result.get("convertedProductFeatures"));
            } catch (GenericServiceException e) {
                Debug.logError(e, "Unable to get features for supplier from product : " + this.productId, MODULE);
            }
        }
        return featuresForSupplier;
    }

    /**
     * Returns the item's size (length + girth)
     */
    public BigDecimal getSize() {
        GenericValue product = getProduct();
        if (product != null) {
            BigDecimal height = product.getBigDecimal("shippingHeight");
            BigDecimal width = product.getBigDecimal("shippingWidth");
            BigDecimal depth = product.getBigDecimal("shippingDepth");

            // if all are null, see if there is an associated virtual product and get the info of that product
            if (height == null && width == null && depth == null) {
                GenericValue parentProduct = this.getParentProduct();
                if (parentProduct != null) {
                    height = parentProduct.getBigDecimal("shippingHeight");
                    width = parentProduct.getBigDecimal("shippingWidth");
                    depth = parentProduct.getBigDecimal("shippingDepth");
                }
            }

            if (height == null) {
                height = BigDecimal.ZERO;
            }
            if (width == null) {
                width = BigDecimal.ZERO;
            }
            if (depth == null) {
                depth = BigDecimal.ZERO;
            }

            // determine girth (longest field is length)
            BigDecimal[] sizeInfo = {height, width, depth};
            Arrays.sort(sizeInfo);

            return (sizeInfo[0].add(sizeInfo[0])).add(sizeInfo[1].add(sizeInfo[1])).add(sizeInfo[2]);
        }
        // non-product items have 0 size
        return BigDecimal.ZERO;
    }

    /**
     * Gets item product info.
     * @return the item product info
     */
    public Map<String, Object> getItemProductInfo() {
        Map<String, Object> itemInfo = new HashMap<>();
        itemInfo.put("productId", this.getProductId());
        itemInfo.put("weight", this.getWeight());
        itemInfo.put("weightUomId", this.getProduct().getString("weightUomId"));
        itemInfo.put("size", this.getSize());
        itemInfo.put("piecesIncluded", this.getPiecesIncluded());
        itemInfo.put("featureSet", this.getFeatureSet());
        GenericValue product = getProduct();
        if (product != null) {
            itemInfo.put("inShippingBox", product.getString("inShippingBox"));
            if (product.getString("inShippingBox") != null && "Y".equals(product.getString("inShippingBox"))) {
                itemInfo.put("shippingHeight", product.getBigDecimal("shippingHeight"));
                itemInfo.put("shippingWidth", product.getBigDecimal("shippingWidth"));
                itemInfo.put("shippingDepth", product.getBigDecimal("shippingDepth"));
            }
        }
        return itemInfo;
    }

    /**
     * Returns the base price.
     * @return the base price
     */
    public BigDecimal getBasePrice() {
        BigDecimal curBasePrice;
        if (selectedAmount.compareTo(BigDecimal.ZERO) > 0) {
            curBasePrice = basePrice.multiply(selectedAmount);
        } else {
            curBasePrice = basePrice;
        }
        return curBasePrice;
    }

    /**
     * Sets the base price for the item; use with caution
     * @param basePrice the base price
     */
    public void setBasePrice(BigDecimal basePrice) {
        this.basePrice = basePrice;
    }

    /**
     * Gets display price.
     * @return the display price
     */
    public BigDecimal getDisplayPrice() {
        BigDecimal curDisplayPrice;
        if (this.displayPrice == null) {
            curDisplayPrice = this.getBasePrice();
        } else {
            if (selectedAmount.compareTo(BigDecimal.ZERO) > 0) {
                curDisplayPrice = this.displayPrice.multiply(this.selectedAmount);
            } else {
                curDisplayPrice = this.displayPrice;
            }
        }
        return curDisplayPrice;
    }

    /**
     * Sets the display price for the item; use with caution
     * @param displayPrice the display price
     */
    public void setDisplayPrice(BigDecimal displayPrice) {
        this.displayPrice = displayPrice;
    }

    /**
     * Gets special promo price.
     * @return the special promo price
     */
    public BigDecimal getSpecialPromoPrice() {
        return this.specialPromoPrice;
    }

    /**
     * Sets special promo price.
     * @param specialPromoPrice the special promo price
     */
    public void setSpecialPromoPrice(BigDecimal specialPromoPrice) {
        this.specialPromoPrice = specialPromoPrice;
    }

    /**
     * Gets recurring base price.
     * @return the recurring base price
     */
    public BigDecimal getRecurringBasePrice() {
        if (this.recurringBasePrice == null) {
            return null;
        }

        if (selectedAmount.compareTo(BigDecimal.ZERO) > 0) {
            return this.recurringBasePrice.multiply(selectedAmount);
        }
        return this.recurringBasePrice;
    }

    /**
     * Sets the base price for the item; use with caution
     */
    public void setRecurringBasePrice(BigDecimal recurringBasePrice) {
        this.recurringBasePrice = recurringBasePrice;
    }

    /**
     * Gets recurring display price.
     * @return the recurring display price
     */
    public BigDecimal getRecurringDisplayPrice() {
        if (this.recurringDisplayPrice == null) {
            return this.getRecurringBasePrice();
        }

        if (selectedAmount.compareTo(BigDecimal.ZERO) > 0) {
            return this.recurringDisplayPrice.multiply(this.selectedAmount);
        }
        return this.recurringDisplayPrice;
    }

    /**
     * Sets the display price for the item; use with caution
     */
    public void setRecurringDisplayPrice(BigDecimal recurringDisplayPrice) {
        this.recurringDisplayPrice = recurringDisplayPrice;
    }

    /**
     * Returns the list price.
     */
    public BigDecimal getListPrice() {
        return listPrice;
    }

    /**
     * Sets list price.
     * @param listPrice the list price
     */
    public void setListPrice(BigDecimal listPrice) {
        this.listPrice = listPrice;
    }

    /**
     * Returns isModifiedPrice
     */
    public boolean getIsModifiedPrice() {
        return isModifiedPrice;
    }

    /**
     * Set isModifiedPrice
     */
    public void setIsModifiedPrice(boolean isModifiedPrice) {
        this.isModifiedPrice = isModifiedPrice;
    }

    /**
     * get the percentage for the second person
     */
    public BigDecimal getReserv2ndPPPerc() {
        return reserv2ndPPPerc;
    }

    /**
     * Sets the extra % for second person
     */
    public void setReserv2ndPPPerc(BigDecimal reserv2ndPPPerc) {
        this.reserv2ndPPPerc = reserv2ndPPPerc;
    }

    /**
     * get the percentage for the third and following person
     */
    public BigDecimal getReservNthPPPerc() {
        return reservNthPPPerc;
    }

    /**
     * Sets the extra % for third and following person
     */
    public void setReservNthPPPerc(BigDecimal reservNthPPPerc) {
        this.reservNthPPPerc = reservNthPPPerc;
    }

    /**
     * Returns the "other" adjustments.
     */
    public BigDecimal getOtherAdjustments() {
        return OrderReadHelper.calcItemAdjustments(quantity, getBasePrice(), this.getAdjustments(), true, false, false, false, false);
    }

    /**
     * Returns the "other" adjustments.
     */
    public BigDecimal getOtherAdjustmentsRecurring() {
        return OrderReadHelper.calcItemAdjustmentsRecurringBd(quantity, getRecurringBasePrice() == null ? BigDecimal.ZERO : getRecurringBasePrice(),
                this.getAdjustments(), true, false, false, false, false);
    }

    /**
     * calculates for a reservation the percentage/100 extra for more than 1 person.
     */
    // similar code at EditShoppingList.groovy
    public BigDecimal getRentalAdjustment() {
        if (!"RENTAL_ORDER_ITEM".equals(this.itemType)) {
            // not a rental item?
            return BigDecimal.ONE;
        }
        BigDecimal persons = this.getReservPersons();
        BigDecimal rentalValue = BigDecimal.ZERO;
        if (persons.compareTo(BigDecimal.ONE) > 0) {
            if (persons.compareTo(new BigDecimal("2")) > 0) {
                persons = persons.subtract(new BigDecimal("2"));
                if (getReservNthPPPerc().compareTo(BigDecimal.ZERO) > 0) {
                    rentalValue = persons.multiply(getReservNthPPPerc());
                } else {
                    rentalValue = persons.multiply(getReserv2ndPPPerc());
                }
                persons = new BigDecimal("2");
            }
            if (persons.compareTo(new BigDecimal("2")) == 0) {
                rentalValue = rentalValue.add(getReserv2ndPPPerc());
            }
        }
        rentalValue = rentalValue.add(new BigDecimal("100"));    // add final 100 percent for first person
        return rentalValue.movePointLeft(2).multiply(getReservLength()); // return total rental adjustment
    }

    /**
     * Returns the total line price.
     */
    public BigDecimal getItemSubTotal(BigDecimal quantity) {
        BigDecimal basePrice = getBasePrice();
        BigDecimal rentalAdj = getRentalAdjustment();
        BigDecimal otherAdj = getOtherAdjustments();
        return basePrice.multiply(quantity).multiply(rentalAdj).add(otherAdj);
    }

    /**
     * Gets item sub total.
     * @return the item sub total
     */
    public BigDecimal getItemSubTotal() {
        return this.getItemSubTotal(this.getQuantity());
    }

    /**
     * Gets display item sub total.
     * @return the display item sub total
     */
    public BigDecimal getDisplayItemSubTotal() {
        return this.getDisplayPrice().multiply(this.getQuantity()).multiply(this.getRentalAdjustment()).add(this.getOtherAdjustments());
    }

    /**
     * Gets display item sub total no adj.
     * @return the display item sub total no adj
     */
    public BigDecimal getDisplayItemSubTotalNoAdj() {
        return this.getDisplayPrice().multiply(this.getQuantity());
    }

    /**
     * Gets display item recurring sub total.
     * @return the display item recurring sub total
     */
    public BigDecimal getDisplayItemRecurringSubTotal() {
        BigDecimal curRecurringDisplayPrice = this.getRecurringDisplayPrice();

        if (curRecurringDisplayPrice == null) {
            return this.getOtherAdjustmentsRecurring();
        }

        return curRecurringDisplayPrice.multiply(this.getQuantity()).add(this.getOtherAdjustmentsRecurring());
    }

    /**
     * Gets display item recurring sub total no adj.
     * @return the display item recurring sub total no adj
     */
    public BigDecimal getDisplayItemRecurringSubTotalNoAdj() {
        BigDecimal curRecurringDisplayPrice = this.getRecurringDisplayPrice();
        if (curRecurringDisplayPrice == null) {
            return BigDecimal.ZERO;
        }

        return curRecurringDisplayPrice.multiply(this.getQuantity());
    }

    /**
     * Add all product feature and appls.
     * @param productFeatureAndApplsToAdd the product feature and appls to add
     */
    public void addAllProductFeatureAndAppls(Map<String, GenericValue> productFeatureAndApplsToAdd) {
        if (productFeatureAndApplsToAdd == null) {
            return;
        }
        for (GenericValue additionalProductFeatureAndAppl : productFeatureAndApplsToAdd.values()) {
            this.putAdditionalProductFeatureAndAppl(additionalProductFeatureAndAppl);
        }
    }

    /**
     * Put additional product feature and appl.
     * @param additionalProductFeatureAndAppl the additional product feature and appl
     */
    public void putAdditionalProductFeatureAndAppl(GenericValue additionalProductFeatureAndAppl) {
        if (additionalProductFeatureAndAppl == null) {
            return;
        }

        // if one already exists with the given type, remove it with the corresponding adjustment
        removeAdditionalProductFeatureAndAppl(additionalProductFeatureAndAppl.getString("productFeatureTypeId"));

        // adds to additional map and creates an adjustment with given price
        String featureType = additionalProductFeatureAndAppl.getString("productFeatureTypeId");
        this.additionalProductFeatureAndAppls.put(featureType, additionalProductFeatureAndAppl);

        GenericValue orderAdjustment = this.getDelegator().makeValue("OrderAdjustment");
        orderAdjustment.set("orderAdjustmentTypeId", "ADDITIONAL_FEATURE");
        orderAdjustment.set("description", additionalProductFeatureAndAppl.get("description"));
        orderAdjustment.set("productFeatureId", additionalProductFeatureAndAppl.get("productFeatureId"));

        // NOTE: this is a VERY simple pricing scheme for additional features and will likely need to be extended for most real applications
        BigDecimal amount = (BigDecimal) additionalProductFeatureAndAppl.get("amount");
        if (amount != null) {
            amount = amount.multiply(this.getQuantity());
            orderAdjustment.set("amount", amount);
        }

        BigDecimal recurringAmount = (BigDecimal) additionalProductFeatureAndAppl.get("recurringAmount");
        if (recurringAmount != null) {
            recurringAmount = recurringAmount.multiply(this.getQuantity());
            orderAdjustment.set("recurringAmount", recurringAmount);
        }

        if (amount == null && recurringAmount == null) {
            Debug.logWarning("In putAdditionalProductFeatureAndAppl the amount and recurringAmount are null for this adjustment: "
                    + orderAdjustment, MODULE);
        }

        this.addAdjustment(orderAdjustment);
    }

    /**
     * Gets additional product feature and appl.
     * @param productFeatureTypeId the product feature type id
     * @return the additional product feature and appl
     */
    public GenericValue getAdditionalProductFeatureAndAppl(String productFeatureTypeId) {
        if (this.additionalProductFeatureAndAppls == null) {
            return null;
        }
        return this.additionalProductFeatureAndAppls.get(productFeatureTypeId);
    }

    /**
     * Remove additional product feature and appl generic value.
     * @param productFeatureTypeId the product feature type id
     * @return the generic value
     */
    public GenericValue removeAdditionalProductFeatureAndAppl(String productFeatureTypeId) {
        if (this.additionalProductFeatureAndAppls == null) {
            return null;
        }

        GenericValue oldAdditionalProductFeatureAndAppl = this.additionalProductFeatureAndAppls.remove(productFeatureTypeId);

        if (oldAdditionalProductFeatureAndAppl != null) {
            removeFeatureAdjustment(oldAdditionalProductFeatureAndAppl.getString("productFeatureId"));
        }

        return oldAdditionalProductFeatureAndAppl;
    }

    /**
     * Gets additional product feature and appls.
     * @return the additional product feature and appls
     */
    public Map<String, GenericValue> getAdditionalProductFeatureAndAppls() {
        return this.additionalProductFeatureAndAppls;
    }

    /**
     * Gets feature id qty map.
     * @param quantity the quantity
     * @return the feature id qty map
     */
    public Map<String, BigDecimal> getFeatureIdQtyMap(BigDecimal quantity) {
        Map<String, BigDecimal> featureMap = new HashMap<>();
        GenericValue product = this.getProduct();
        if (product != null) {
            List<GenericValue> featureAppls = null;
            try {
                featureAppls = product.getRelated("ProductFeatureAppl", null, null, false);
                List<EntityExpr> filterExprs = UtilMisc.toList(EntityCondition.makeCondition("productFeatureApplTypeId",
                        EntityOperator.EQUALS, "STANDARD_FEATURE"));
                filterExprs.add(EntityCondition.makeCondition("productFeatureApplTypeId", EntityOperator.EQUALS, "REQUIRED_FEATURE"));
                filterExprs.add(EntityCondition.makeCondition("productFeatureApplTypeId", EntityOperator.EQUALS, "DISTINGUISHING_FEAT"));
                featureAppls = EntityUtil.filterByOr(featureAppls, filterExprs);
            } catch (GenericEntityException e) {
                Debug.logError(e, "Unable to get features from product : " + product.get("productId"), MODULE);
            }
            if (featureAppls != null) {
                for (GenericValue appl : featureAppls) {
                    BigDecimal lastQuantity = featureMap.get(appl.getString("productFeatureId"));
                    if (lastQuantity == null) {
                        lastQuantity = BigDecimal.ZERO;
                    }
                    BigDecimal newQuantity = lastQuantity.add(quantity);
                    featureMap.put(appl.getString("productFeatureId"), newQuantity);
                }
            }
        }
        if (this.additionalProductFeatureAndAppls != null) {
            for (GenericValue appl : this.additionalProductFeatureAndAppls.values()) {
                BigDecimal lastQuantity = featureMap.get(appl.getString("productFeatureId"));
                if (lastQuantity == null) {
                    lastQuantity = BigDecimal.ZERO;
                }
                BigDecimal newQuantity = lastQuantity.add(quantity);
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
    public Map<String, Object> getAttributes() {
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
        if (orderItemAttributes == null) {
            orderItemAttributes = new HashMap<>();
        }
        this.orderItemAttributes.put(name, value);
    }

    /**
     * Return an OrderItemAttribute.
     */
    public String getOrderItemAttribute(String name) {
        if (orderItemAttributes == null) {
            return null;
        }
        return this.orderItemAttributes.get(name);
    }

    /**
     * Gets order item attributes.
     * @return the order item attributes
     */
    public Map<String, String> getOrderItemAttributes() {
        Map<String, String> attrs = new HashMap<>();
        if (orderItemAttributes != null) {
            attrs.putAll(orderItemAttributes);
        }
        return attrs;
    }

    /** Add an adjustment to the order item; don't worry about setting the orderId, orderItemSeqId or orderAdjustmentId;
     * they will be set when the order is created */
    public int addAdjustment(GenericValue adjustment) {
        itemAdjustments.add(adjustment);
        return itemAdjustments.indexOf(adjustment);
    }

    /**
     * Remove adjustment.
     * @param adjustment the adjustment
     */
    public void removeAdjustment(GenericValue adjustment) {
        itemAdjustments.remove(adjustment);
    }

    /**
     * Remove adjustment.
     * @param index the index
     */
    public void removeAdjustment(int index) {
        itemAdjustments.remove(index);
    }

    /**
     * Gets adjustments.
     * @return the adjustments
     */
    public List<GenericValue> getAdjustments() {
        return itemAdjustments;
    }

    /**
     * Remove feature adjustment.
     * @param productFeatureId the product feature id
     */
    public void removeFeatureAdjustment(String productFeatureId) {
        if (productFeatureId == null) {
            return;
        }

        itemAdjustments.removeIf(itemAdjustment -> productFeatureId.equals(itemAdjustment.getString("productFeatureId")));
    }

    /**
     * Gets order item price infos.
     * @return the order item price infos
     */
    public List<GenericValue> getOrderItemPriceInfos() {
        return orderItemPriceInfos;
    }

    /** Add a contact mech to this purpose; the contactMechPurposeTypeId is required */
    public void addContactMech(String contactMechPurposeTypeId, String contactMechId) {
        if (contactMechPurposeTypeId == null) {
            throw new IllegalArgumentException("You must specify a contactMechPurposeTypeId to add a ContactMech");
        }
        contactMechIdsMap.put(contactMechPurposeTypeId, contactMechId);
    }

    /** Get the contactMechId for this item given the contactMechPurposeTypeId */
    public String getContactMech(String contactMechPurposeTypeId) {
        return contactMechIdsMap.get(contactMechPurposeTypeId);
    }

    /** Remove the contactMechId from this item given the contactMechPurposeTypeId */
    public String removeContactMech(String contactMechPurposeTypeId) {
        return contactMechIdsMap.remove(contactMechPurposeTypeId);
    }

    /**
     * Gets order item contact mech ids.
     * @return the order item contact mech ids
     */
    public Map<String, String> getOrderItemContactMechIds() {
        return contactMechIdsMap;
    }

    /**
     * Gets is promo.
     * @return the is promo
     */
    public boolean getIsPromo() {
        return this.isPromo;
    }

    /**
     * Sets is promo.
     * @param isPromo the is promo
     */
    public void setIsPromo(boolean isPromo) {
        this.isPromo = isPromo;
    }

    /**
     * Gets alternative option product ids.
     * @return the alternative option product ids
     */
    public List<String> getAlternativeOptionProductIds() {
        return this.alternativeOptionProductIds;
    }

    /**
     * Sets alternative option product ids.
     * @param alternativeOptionProductIds the alternative option product ids
     */
    public void setAlternativeOptionProductIds(List<String> alternativeOptionProductIds) {
        this.alternativeOptionProductIds = alternativeOptionProductIds;
    }

    /**
     * Equals boolean.
     * @param item the item
     * @return the boolean
     */
    public boolean equals(ShoppingCartItem item) {
        if (item == null) {
            return false;
        }
        return this.equals(item.getProductId(), item.additionalProductFeatureAndAppls, item.attributes, item.prodCatalogId, item.selectedAmount,
                item.getItemType(), item.getItemGroup(), item.getIsPromo());
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    /** Compares the specified object with this cart item. Defaults isPromo to false. Default to no itemGroup. */
    public boolean equals(String productId, Map<String, GenericValue> additionalProductFeatureAndAppls, Map<String, Object> attributes,
                          String prodCatalogId, BigDecimal selectedAmount) {
        return equals(productId, additionalProductFeatureAndAppls, attributes, prodCatalogId, selectedAmount, null, null, false);
    }

    /** Compares the specified object with this cart item. Defaults isPromo to false. */
    public boolean equals(String productId, Map<String, GenericValue> additionalProductFeatureAndAppls, Map<String, Object> attributes,
                          String prodCatalogId, ProductConfigWrapper configWrapper, String itemType, ShoppingCart.ShoppingCartItemGroup itemGroup,
                          BigDecimal selectedAmount) {
        return equals(productId, null, BigDecimal.ZERO, BigDecimal.ZERO, null, null, additionalProductFeatureAndAppls, attributes, prodCatalogId,
                selectedAmount, configWrapper, itemType, itemGroup, false);
    }

    /**
     * Compares the specified object with this cart item including rental data. Defaults isPromo to false.
     */
    public boolean equals(String productId, Timestamp reservStart, BigDecimal reservLength, BigDecimal reservPersons, Map<String,
            GenericValue> additionalProductFeatureAndAppls, Map<String, Object> attributes, String prodCatalogId,
            ProductConfigWrapper configWrapper, String itemType, ShoppingCart.ShoppingCartItemGroup itemGroup, BigDecimal selectedAmount) {
        return equals(productId, reservStart, reservLength, reservPersons, null, null, additionalProductFeatureAndAppls, attributes, prodCatalogId,
                selectedAmount, configWrapper, itemType, itemGroup, false);
    }

    /** Compares the specified object with this cart item. Defaults isPromo to false. */
    public boolean equals(String productId, Map<String, GenericValue> additionalProductFeatureAndAppls, Map<String, Object> attributes,
                          String prodCatalogId, BigDecimal selectedAmount, String itemType, ShoppingCart.ShoppingCartItemGroup itemGroup,
                          boolean isPromo) {
        return equals(productId, null, BigDecimal.ZERO, BigDecimal.ZERO, null, null, additionalProductFeatureAndAppls, attributes, prodCatalogId,
                selectedAmount, null, itemType, itemGroup, isPromo);
    }

    /** Compares the specified object with this cart item. */
    public boolean equals(String productId, Timestamp reservStart, BigDecimal reservLength, BigDecimal reservPersons, String accommodationMapId,
                          String accommodationSpotId,
                          Map<String, GenericValue> additionalProductFeatureAndAppls, Map<String, Object> attributes, String prodCatalogId,
                          BigDecimal selectedAmount,
                          ProductConfigWrapper configWrapper, String itemType, ShoppingCart.ShoppingCartItemGroup itemGroup, boolean isPromo) {
        return equals(productId, reservStart, reservLength, reservPersons, accommodationMapId, accommodationSpotId, additionalProductFeatureAndAppls,
                attributes, null, prodCatalogId, selectedAmount, configWrapper, itemType, itemGroup, isPromo);
    }

    /** Compares the specified object order item attributes. */
    public boolean equals(String productId, Timestamp reservStart, BigDecimal reservLength, BigDecimal reservPersons, String accommodationMapId,
                          String accommodationSpotId,
                          Map<String, GenericValue> additionalProductFeatureAndAppls, Map<String, Object> attributes, Map<String,
            String> orderItemAttributes, String prodCatalogId, BigDecimal selectedAmount,
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

        if (selectedAmount != null && this.selectedAmount.compareTo(selectedAmount) != 0) {
            return false;
        }

        if ((this.reservStart == null && reservStart != null) || (this.reservStart != null && reservStart == null)) {
            return false;
        }
        if (this.reservStart != null && reservStart != null && !this.reservStart.equals(reservStart)) {
            return false;
        }

        if (reservLength != null && this.reservLength.compareTo(reservLength) != 0) {
            return false;
        }

        if (reservPersons != null && this.reservPersons.compareTo(reservPersons) != 0) {
            return false;
        }

        if (this.accommodationMapId != null && !this.accommodationMapId.equals(accommodationMapId)) {
            return false;
        }
        if (this.accommodationSpotId != null && !this.accommodationSpotId.equals(accommodationSpotId)) {
            return false;
        }

        if (this.isPromo != isPromo) {
            return false;
        }

        if ((this.additionalProductFeatureAndAppls == null && UtilValidate.isNotEmpty(additionalProductFeatureAndAppls))
                || (UtilValidate.isNotEmpty(this.additionalProductFeatureAndAppls) && additionalProductFeatureAndAppls == null)
                || (this.additionalProductFeatureAndAppls != null && additionalProductFeatureAndAppls != null
                && (this.additionalProductFeatureAndAppls.size() != additionalProductFeatureAndAppls.size()
                || !(this.additionalProductFeatureAndAppls.equals(additionalProductFeatureAndAppls))))) {
            return false;
        }

        if ((this.attributes == null && UtilValidate.isNotEmpty(attributes)) || (UtilValidate.isNotEmpty(this.attributes) && attributes == null)
                || (this.attributes != null && attributes != null && (this.attributes.size() != attributes.size()
                || !(this.attributes.equals(attributes))))) {
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

        // order item attribute unique
        return !((this.orderItemAttributes == null && UtilValidate.isNotEmpty(orderItemAttributes))
                || (UtilValidate.isNotEmpty(this.orderItemAttributes) && orderItemAttributes == null)
                || (this.orderItemAttributes != null && orderItemAttributes != null && (this.orderItemAttributes.size() != orderItemAttributes.size()
                || !(this.orderItemAttributes.equals(orderItemAttributes)))));
    }

    /** Gets the Product entity. If it is not already retreived gets it from the delegator */
    public GenericValue getProduct() {
        if (this.product != null) {
            return this.product;
        }
        if (this.productId != null) {
            try {
                this.product = this.getDelegator().findOne("Product", UtilMisc.toMap("productId", productId), true);
            } catch (GenericEntityException e) {
                throw new RuntimeException("Entity Engine error getting Product (" + e.getMessage() + ")");
            }
        }
        return this.product;
    }

    /**
     * Gets parent product.
     * @return the parent product
     */
    public GenericValue getParentProduct() {
        if (this.parentProduct != null) {
            return this.parentProduct;
        }
        if (this.productId == null) {
            throw new IllegalStateException("Bad product id");
        }

        this.parentProduct = ProductWorker.getParentProduct(productId, this.getDelegator());

        return this.parentProduct;
    }

    /**
     * Gets parent product id.
     * @return the parent product id
     */
    public String getParentProductId() {
        GenericValue parentProduct = this.getParentProduct();
        if (parentProduct != null) {
            return parentProduct.getString("productId");
        }
        return null;
    }

    /**
     * Gets optional product features.
     * @return the optional product features
     */
    public Map<String, List<GenericValue>> getOptionalProductFeatures() {
        if (product != null) {
            return ProductWorker.getOptionalProductFeatures(getDelegator(), this.productId);
        }
        // non-product items do not have features
        return new HashMap<>();
    }

    /**
     * Gets delegator.
     * @return the delegator
     */
    public Delegator getDelegator() {
        if (delegator == null) {
            if (UtilValidate.isEmpty(delegatorName)) {
                throw new IllegalStateException("No delegator or delegatorName on ShoppingCartItem, somehow was not setup right.");
            }
            delegator = DelegatorFactory.getDelegator(delegatorName);
        }
        return delegator;
    }

    /**
     * Explode item list.
     * @param cart       the cart
     * @param dispatcher the dispatcher
     * @return the list
     * @throws CartItemModifyException the cart item modify exception
     */
    public List<ShoppingCartItem> explodeItem(ShoppingCart cart, LocalDispatcher dispatcher) throws CartItemModifyException {
        BigDecimal baseQuantity = this.getQuantity();
        List<ShoppingCartItem> newItems = new ArrayList<>();

        if (baseQuantity.compareTo(BigDecimal.ONE) > 0) {
            for (int i = 1; i < baseQuantity.intValue(); i++) {
                // clone the item
                ShoppingCartItem item = new ShoppingCartItem(this);

                // set the new item's quantity
                item.setQuantity(BigDecimal.ONE, dispatcher, cart, false);
                // now copy/calc the adjustments
                Debug.logInfo("Clone's adj: " + item.getAdjustments(), MODULE);
                if (UtilValidate.isNotEmpty(item.getAdjustments())) {
                    List<GenericValue> adjustments = UtilMisc.makeListWritable(item.getAdjustments());
                    for (GenericValue adjustment : adjustments) {

                        if (adjustment != null) {
                            item.removeAdjustment(adjustment);
                            GenericValue newAdjustment = GenericValue.create(adjustment);
                            BigDecimal adjAmount = newAdjustment.getBigDecimal("amount");

                            // we use != because adjustments can be +/-
                            if (adjAmount != null && adjAmount.compareTo(BigDecimal.ZERO) != 0) {
                                newAdjustment.set("amount", adjAmount.divide(baseQuantity, GEN_ROUNDING));
                            }
                            Debug.logInfo("Cloned adj: " + newAdjustment, MODULE);
                            item.addAdjustment(newAdjustment);
                        } else {
                            Debug.logInfo("Clone Adjustment is null", MODULE);
                        }
                    }
                }
                newItems.add(item);
            }

            // set this item's quantity
            this.setQuantity(BigDecimal.ONE, dispatcher, cart, false);

            Debug.logInfo("BaseQuantity: " + baseQuantity, MODULE);
            Debug.logInfo("Item's Adj: " + this.getAdjustments(), MODULE);

            // re-calc this item's adjustments
            if (UtilValidate.isNotEmpty(this.getAdjustments())) {
                List<GenericValue> adjustments = UtilMisc.makeListWritable(this.getAdjustments());
                for (GenericValue adjustment : adjustments) {

                    if (adjustment != null) {
                        this.removeAdjustment(adjustment);
                        GenericValue newAdjustment = GenericValue.create(adjustment);
                        BigDecimal adjAmount = newAdjustment.getBigDecimal("amount");

                        // we use != becuase adjustments can be +/-
                        if (adjAmount != null && adjAmount.compareTo(BigDecimal.ZERO) != 0) {
                            newAdjustment.set("amount", adjAmount.divide(baseQuantity, GEN_ROUNDING));
                        }
                        Debug.logInfo("Updated adj: " + newAdjustment, MODULE);
                        this.addAdjustment(newAdjustment);
                    }
                }
            }

        }
        return newItems;
    }
}
