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

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.GeneralRuntimeException;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilFormatOut;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilNumber;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.common.DataModelConstants;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.DelegatorFactory;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericPK;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.entity.util.EntityUtilProperties;
import org.apache.ofbiz.order.finaccount.FinAccountHelper;
import org.apache.ofbiz.order.order.OrderReadHelper;
import org.apache.ofbiz.order.shoppingcart.product.ProductPromoWorker;
import org.apache.ofbiz.order.shoppingcart.shipping.ShippingEstimateWrapper;
import org.apache.ofbiz.order.shoppingcart.shipping.ShippingEvents;
import org.apache.ofbiz.order.shoppinglist.ShoppingListEvents;
import org.apache.ofbiz.party.contact.ContactHelper;
import org.apache.ofbiz.party.contact.ContactMechWorker;
import org.apache.ofbiz.product.category.CategoryWorker;
import org.apache.ofbiz.product.config.ProductConfigWrapper;
import org.apache.ofbiz.product.product.ProductWorker;
import org.apache.ofbiz.product.store.ProductStoreWorker;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;

/**
 * Shopping Cart Object
 */
@SuppressWarnings("serial")
public class ShoppingCart implements Iterable<ShoppingCartItem>, Serializable {

    public static final String module = ShoppingCart.class.getName();
    public static final String resource_error = "OrderErrorUiLabels";

    // modes for getting OrderItemAttributes
    public static final int ALL = 1;
    public static final int EMPTY_ONLY = 2;
    public static final int FILLED_ONLY = 3;

    // scales and rounding modes for BigDecimal math
    public static final int scale = UtilNumber.getBigDecimalScale("order.decimals");
    public static final RoundingMode rounding = UtilNumber.getRoundingMode("order.rounding");
    public static final int taxCalcScale = UtilNumber.getBigDecimalScale("salestax.calc.decimals");
    public static final int taxFinalScale = UtilNumber.getBigDecimalScale("salestax.final.decimals");
    public static final RoundingMode taxRounding = UtilNumber.getRoundingMode("salestax.rounding");
    public static final MathContext generalRounding = new MathContext(10);


    private String orderType = "SALES_ORDER"; // default orderType
    private String channel = "UNKNWN_SALES_CHANNEL"; // default channel enum

    private String poNumber = null;
    private String orderId = null;
    private String orderName = null;
    private String orderStatusId = null;
    private String orderStatusString = null;
    private String firstAttemptOrderId = null;
    private String externalId = null;
    private String internalCode = null;
    private String billingAccountId = null;
    private BigDecimal billingAccountAmt = BigDecimal.ZERO;
    private String agreementId = null;
    private String quoteId = null;
    private String workEffortId = null;
    private long nextItemSeq = 1;

    private String defaultItemDeliveryDate = null;
    private String defaultItemComment = null;

    private String orderAdditionalEmails = null;
    private boolean viewCartOnAdd = false;
    private boolean readOnlyCart = false;

    private Timestamp lastListRestore = null;
    private String autoSaveListId = null;

    /** Holds value of order adjustments. */
    private List<GenericValue> adjustments = new LinkedList<>();
    // OrderTerms
    private boolean orderTermSet = false;
    private List<GenericValue> orderTerms = new LinkedList<>();

    private List<ShoppingCartItem> cartLines = new LinkedList<>();
    private Map<String, ShoppingCartItemGroup> itemGroupByNumberMap = new HashMap<>();
    protected long nextGroupNumber = 1;
    protected List<CartPaymentInfo> paymentInfo = new LinkedList<>();
    private List<CartShipInfo> shipInfo = new LinkedList<>();
    private Map<String, String> contactMechIdsMap = new HashMap<>();
    private Map<String, String> orderAttributes = new HashMap<>();
    private Map<String, Object> attributes = new HashMap<>(); // user defined attributes
    // Lists of internal/public notes: when the order is stored they are transformed into OrderHeaderNotes
    private List<String> internalOrderNotes = new LinkedList<>(); // internal notes
    private List<String> orderNotes = new LinkedList<>(); // public notes (printed on documents etc.)

    /** contains a list of partyId for each roleTypeId (key) */
    private Map<String, List<String>> additionalPartyRole = new HashMap<>();

    /** these are defaults for all ship groups */
    private Timestamp defaultShipAfterDate = null;
    private Timestamp defaultShipBeforeDate = null;
    private Timestamp defaultReserveAfterDate = null;

    /** Contains a List for each productPromoId (key) containing a productPromoCodeId (or empty string for no code) for each use of the productPromoId */
    private List<ProductPromoUseInfo> productPromoUseInfoList = new LinkedList<>();
    /** Contains the promo codes entered */
    private Set<String> productPromoCodes = new HashSet<>();
    private List<GenericValue> freeShippingProductPromoActions = new ArrayList<>();
    /** Note that even though this is promotion info, it should NOT be cleared when the promos are cleared, it is a preference that will be used in the next promo calculation */
    private Map<GenericPK, String> desiredAlternateGiftByAction = new HashMap<>();
    private Timestamp cartCreatedTs = UtilDateTime.nowTimestamp();

    private transient Delegator delegator = null;
    private String delegatorName = null;

    protected String productStoreId = null;
    protected boolean doPromotions = true;
    protected String transactionId = null;
    protected String facilityId = null;
    protected String webSiteId = null;
    protected String terminalId = null;
    protected String autoOrderShoppingListId = null;

    /** General partyId for the Order, all other IDs default to this one if not specified explicitly */
    protected String orderPartyId = null;

    // sales order parties
    protected String placingCustomerPartyId = null;
    protected String billToCustomerPartyId = null;
    protected String shipToCustomerPartyId = null;
    protected String endUserCustomerPartyId = null;

    // purchase order parties
    protected String billFromVendorPartyId = null;
    protected String shipFromVendorPartyId = null;
    protected String supplierAgentPartyId = null;

    protected GenericValue userLogin = null;
    protected GenericValue autoUserLogin = null;

    protected Locale locale;  // holds the locale from the user session
    protected String currencyUom = null;
    protected boolean holdOrder = false;
    protected Timestamp orderDate = null;
    protected Timestamp cancelBackOrderDate = null;

    /** don't allow empty constructor */
    protected ShoppingCart() {}

    /** Creates a new cloned ShoppingCart Object. */
    public ShoppingCart(ShoppingCart cart) {
        this.delegator = cart.getDelegator();
        this.delegatorName = delegator.getDelegatorName();
        this.productStoreId = cart.getProductStoreId();
        this.doPromotions = cart.getDoPromotions();
        this.poNumber = cart.getPoNumber();
        this.orderId = cart.getOrderId();
        this.orderName = "Copy of " + cart.getOrderName();
        this.workEffortId = cart.getWorkEffortId();
        this.firstAttemptOrderId = cart.getFirstAttemptOrderId();
        this.billingAccountId = cart.getBillingAccountId();
        this.agreementId = cart.getAgreementId();
        this.quoteId = cart.getQuoteId();
        this.orderAdditionalEmails = cart.getOrderAdditionalEmails();
        this.adjustments.addAll(cart.getAdjustments());
        this.contactMechIdsMap = new HashMap<>(cart.getOrderContactMechIds());
        this.freeShippingProductPromoActions = new ArrayList<>(cart.getFreeShippingProductPromoActions());
        this.desiredAlternateGiftByAction = cart.getAllDesiredAlternateGiftByActionCopy();
        this.productPromoUseInfoList.addAll(cart.productPromoUseInfoList);
        this.productPromoCodes = new HashSet<>(cart.productPromoCodes);
        this.locale = cart.getLocale();
        this.currencyUom = cart.getCurrency();
        this.externalId = cart.getExternalId();
        this.internalCode = cart.getInternalCode();
        this.viewCartOnAdd = cart.viewCartOnAdd();
        this.defaultShipAfterDate = cart.getDefaultShipAfterDate();
        this.defaultShipBeforeDate = cart.getDefaultShipBeforeDate();
        this.defaultReserveAfterDate = cart.getDefaultReserveAfterDate();
        this.cancelBackOrderDate = cart.getCancelBackOrderDate();

        this.terminalId = cart.getTerminalId();
        this.transactionId = cart.getTransactionId();
        this.autoOrderShoppingListId = cart.getAutoOrderShoppingListId();

        // clone the additionalPartyRoleMap
        this.additionalPartyRole = new HashMap<>();
        for (Map.Entry<String, List<String>> me : cart.additionalPartyRole.entrySet()) {
            this.additionalPartyRole.put(me.getKey(), new LinkedList<>(me.getValue()));
        }

        // clone the groups
        for (ShoppingCartItemGroup itemGroup : cart.itemGroupByNumberMap.values()) {
            // get the new parent group by number from the existing set; as before the parent must come before all children to work...
            ShoppingCartItemGroup parentGroup = null;
            if (itemGroup.getParentGroup() != null) {
                parentGroup = this.getItemGroupByNumber(itemGroup.getParentGroup().getGroupNumber());
            }
            ShoppingCartItemGroup newGroup = new ShoppingCartItemGroup(itemGroup, parentGroup);
            itemGroupByNumberMap.put(newGroup.getGroupNumber(), newGroup);
        }

        // clone the items
        for (ShoppingCartItem item : cart.items()) {
            cartLines.add(new ShoppingCartItem(item));
        }

        this.facilityId = cart.facilityId;
        this.webSiteId = cart.webSiteId;
    }

    /** Creates new empty ShoppingCart object. */
    public ShoppingCart(Delegator delegator, String productStoreId, String webSiteId, Locale locale, String currencyUom, String billToCustomerPartyId, String billFromVendorPartyId) {

        this.delegator = delegator;
        this.delegatorName = delegator.getDelegatorName();
        this.productStoreId = productStoreId;
        this.webSiteId = webSiteId;
        this.locale = (locale != null) ? locale : Locale.getDefault();
        this.currencyUom = (currencyUom != null) ? currencyUom : EntityUtilProperties.getPropertyValue("general", "currency.uom.id.default", "USD", delegator);
        this.billToCustomerPartyId = billToCustomerPartyId;
        this.billFromVendorPartyId = billFromVendorPartyId;

        if (productStoreId != null) {

            // set the default view cart on add for this store
            GenericValue productStore = ProductStoreWorker.getProductStore(productStoreId, delegator);
            if (productStore == null) {
                throw new IllegalArgumentException("Unable to locate ProductStore by ID [" + productStoreId + "]");
            }

            String storeViewCartOnAdd = productStore.getString("viewCartOnAdd");
            if (storeViewCartOnAdd != null && "Y".equalsIgnoreCase(storeViewCartOnAdd)) {
                this.viewCartOnAdd = true;
            }

            if (billFromVendorPartyId == null) {
                // since default cart is of type SALES_ORDER, set to store's payToPartyId
                this.billFromVendorPartyId = productStore.getString("payToPartyId");
            }
            this.facilityId = productStore.getString("inventoryFacilityId");
        }

    }


    /** Creates new empty ShoppingCart object. */
    public ShoppingCart(Delegator delegator, String productStoreId, String webSiteId, Locale locale, String currencyUom) {
        this(delegator, productStoreId, webSiteId, locale, currencyUom, null, null);
    }

    /** Creates a new empty ShoppingCart object. */
    public ShoppingCart(Delegator delegator, String productStoreId, Locale locale, String currencyUom) {
        this(delegator, productStoreId, null, locale, currencyUom);
    }

    public Delegator getDelegator() {
        if (delegator == null) {
            delegator = DelegatorFactory.getDelegator(delegatorName);
        }
        return delegator;
    }

    public String getProductStoreId() {
        return this.productStoreId;
    }

    public boolean getDoPromotions() {
        return this.doPromotions;
    }

    public void setDoPromotions(boolean doPromotions) {
        this.doPromotions = doPromotions;
    }

    /**
     * This is somewhat of a dangerous method, changing the productStoreId changes a lot of stuff including:
     * - some items in the cart may not be valid in any catalog in the new store
     * - promotions need to be recalculated for the products that remain
     * - what else? lots of settings on the ProductStore...
     *
     * So for now this can only be called if the cart is empty... otherwise it wil throw an exception
     *
     */
    public void setProductStoreId(String productStoreId) {
        if ((productStoreId == null && this.productStoreId == null) || (productStoreId != null && productStoreId.equals(this.productStoreId))) {
            return;
        }

        if (this.size() == 0) {
            this.productStoreId = productStoreId;
        } else {
            throw new IllegalArgumentException("Cannot set productStoreId when the cart is not empty; cart size is " + this.size());
        }
    }

    public String getTransactionId() {
        return this.transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getTerminalId() {
        return this.terminalId;
    }

    public void setTerminalId(String terminalId) {
        this.terminalId = terminalId;
    }

    public String getAutoOrderShoppingListId() {
        return this.autoOrderShoppingListId;
    }

    public void setAutoOrderShoppingListId(String autoOrderShoppingListId) {
        this.autoOrderShoppingListId = autoOrderShoppingListId;
    }

    public String getFacilityId() {
        return this.facilityId;
    }

    public void setFacilityId(String facilityId) {
        this.facilityId = facilityId;
    }

    public Locale getLocale() {
        return locale;
    }

    public void setLocale(Locale locale) {
        this.locale = locale;
        for (ShoppingCartItem cartItem : cartLines) {
            cartItem.setLocale(locale);
        }
    }

    public void setOrderName(String orderName) {
        this.orderName = orderName;
    }

    public String getOrderName() {
        return orderName;
    }

    public void setWorkEffortId(String workEffortId) {
        this.workEffortId = workEffortId;
    }

    public String getWorkEffortId() {
        return workEffortId;
    }

    public void setAttribute(String name, Object value) {
        this.attributes.put(name, value);
    }

    public void removeAttribute(String name) {
        this.attributes.remove(name);
    }

    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String name) {
        return (T) this.attributes.get(name);
    }

    public void removeOrderAttribute(String name) {
        this.orderAttributes.remove(name);
    }

    public void setOrderAttribute(String name, String value) {
        this.orderAttributes.put(name, value);
    }

    public String getOrderAttribute(String name) {
        return this.orderAttributes.get(name);
    }

    public void setHoldOrder(boolean b) {
        this.holdOrder = b;
    }

    public boolean getHoldOrder() {
        return this.holdOrder;
    }

    public void setOrderDate(Timestamp t) {
        this.orderDate = t != null ? (Timestamp) t.clone() : null;
    }

    public Timestamp getOrderDate() {
        return this.orderDate != null ? (Timestamp) this.orderDate.clone() : null;
    }

    /** Sets the currency for the cart. */
    public void setCurrency(LocalDispatcher dispatcher, String currencyUom) throws CartItemModifyException {
        if (isReadOnlyCart()) {
           throw new CartItemModifyException("Cart items cannot be changed");
        }
        String previousCurrency = this.currencyUom;
        this.currencyUom = currencyUom;
        if (!previousCurrency.equals(this.currencyUom)) {
            for (ShoppingCartItem item : this) {
                item.updatePrice(dispatcher, this);
            }
        }
    }

    /** Get the current currency setting. */
    public String getCurrency() {
        if (this.currencyUom != null) {
            return this.currencyUom;
        }
        // uh oh, not good, should always be passed in on init, we can't really do
        // anything without it, so throw an exception
        throw new IllegalStateException(
                "The Currency UOM is not set in the shopping cart, this is not a valid state, it should always be passed in when the cart is created.");
    }

    public Timestamp getCartCreatedTime() {
        return this.cartCreatedTs != null ? (Timestamp) this.cartCreatedTs.clone() : null;
    }

    public GenericValue getSupplierProduct(String productId, BigDecimal quantity, LocalDispatcher dispatcher) {
        GenericValue supplierProduct = null;
        Map<String, Object> params = UtilMisc.<String, Object>toMap("productId", productId,
                                    "partyId", this.getPartyId(),
                                    "currencyUomId", this.getCurrency(),
                                    "quantity", quantity);
        try {
            Map<String, Object> result = dispatcher.runSync("getSuppliersForProduct", params);
            if (ServiceUtil.isError(result)) {
                Debug.logError(ServiceUtil.getErrorMessage(result), module);
                return null;
            }
            List<GenericValue> productSuppliers = UtilGenerics.checkList(result.get("supplierProducts"));
            if ((productSuppliers != null) && (productSuppliers.size() > 0)) {
                supplierProduct = productSuppliers.get(0);
            }
        } catch (GenericServiceException  e) {
            Debug.logWarning(UtilProperties.getMessage(resource_error,"OrderRunServiceGetSuppliersForProductError", locale) + e.getMessage(), module);
        }
        return supplierProduct;
    }

    // =======================================================================
    // Methods for cart items
    // =======================================================================

    /** Add an item to the shopping cart, or if already there, increase the quantity.
     * @return the new/increased item index
     * @throws CartItemModifyException
     */
    public int addOrIncreaseItem(String productId, BigDecimal selectedAmount, BigDecimal quantity, Timestamp reservStart, BigDecimal reservLength, BigDecimal reservPersons,
            Timestamp shipBeforeDate, Timestamp shipAfterDate, Map<String, GenericValue> features, Map<String, Object> attributes, String prodCatalogId,
            ProductConfigWrapper configWrapper, String itemType, String itemGroupNumber, String parentProductId, LocalDispatcher dispatcher) throws CartItemModifyException, ItemNotFoundException {

       return addOrIncreaseItem(productId,selectedAmount,quantity,reservStart,reservLength,reservPersons, null,null,shipBeforeDate,shipAfterDate,features,attributes,prodCatalogId,
                configWrapper,itemType,itemGroupNumber,parentProductId,dispatcher);
    }

    /** add rental (with accommodation) item to cart  */
    public int addOrIncreaseItem(String productId, BigDecimal selectedAmount, BigDecimal quantity, Timestamp reservStart, BigDecimal reservLength, BigDecimal reservPersons,
               String accommodationMapId, String accommodationSpotId, Timestamp shipBeforeDate, Timestamp shipAfterDate, Map<String, GenericValue> features, Map<String, Object> attributes,
               String prodCatalogId, ProductConfigWrapper configWrapper, String itemType, String itemGroupNumber, String parentProductId, LocalDispatcher dispatcher) throws CartItemModifyException, ItemNotFoundException {
        return addOrIncreaseItem(productId, selectedAmount, quantity, reservStart, reservLength, reservPersons, accommodationMapId, accommodationSpotId, shipBeforeDate, shipAfterDate, features, attributes, null, prodCatalogId, configWrapper, itemType, itemGroupNumber, parentProductId, dispatcher);
    }

    /** add rental (with accommodation) item to cart and order item attributes*/
    // TODO change method signature, this one is really scary, above are not bad too :/ !
    public int addOrIncreaseItem(String productId, BigDecimal selectedAmount, BigDecimal quantity, Timestamp reservStart, BigDecimal reservLength, BigDecimal reservPersons,
               String accommodationMapId, String accommodationSpotId, Timestamp shipBeforeDate, Timestamp shipAfterDate, Map<String, GenericValue> features, Map<String,Object> attributes,
               Map<String, String> orderItemAttributes, String prodCatalogId, ProductConfigWrapper configWrapper, String itemType, String itemGroupNumber,
               String parentProductId, LocalDispatcher dispatcher) throws CartItemModifyException, ItemNotFoundException {
        return addOrIncreaseItem(productId, selectedAmount, quantity, reservStart, reservLength, reservPersons, accommodationMapId, accommodationSpotId, shipBeforeDate, shipAfterDate, null, features, attributes, orderItemAttributes, prodCatalogId, configWrapper, itemType, itemGroupNumber, parentProductId, dispatcher);
    }

    /** add rental (with accommodation) item to cart and order item attributes*/
    // TODO change method signature, this one is really scary, above are not bad too :/ !
    public int addOrIncreaseItem(String productId, BigDecimal selectedAmount, BigDecimal quantity, Timestamp reservStart, BigDecimal reservLength, BigDecimal reservPersons,
               String accommodationMapId, String accommodationSpotId, Timestamp shipBeforeDate, Timestamp shipAfterDate, Timestamp reserveAfterDate, Map<String, GenericValue> features, Map<String,Object> attributes,
               Map<String, String> orderItemAttributes, String prodCatalogId, ProductConfigWrapper configWrapper, String itemType, String itemGroupNumber,
               String parentProductId, LocalDispatcher dispatcher) throws CartItemModifyException, ItemNotFoundException {
        if (isReadOnlyCart()) {
           throw new CartItemModifyException("Cart items cannot be changed");
        }

        selectedAmount = selectedAmount == null ? BigDecimal.ZERO : selectedAmount;
        reservLength = reservLength == null ? BigDecimal.ZERO : reservLength;
        reservPersons = reservPersons == null ? BigDecimal.ZERO : reservPersons;

        ShoppingCart.ShoppingCartItemGroup itemGroup = this.getItemGroupByNumber(itemGroupNumber);
        GenericValue supplierProduct = null;
        // Check for existing cart item.
        for (int i = 0; i < this.cartLines.size(); i++) {
            ShoppingCartItem sci = cartLines.get(i);


            if (sci.equals(productId, reservStart, reservLength, reservPersons, accommodationMapId, accommodationSpotId, features, attributes, orderItemAttributes, prodCatalogId,selectedAmount, configWrapper, itemType, itemGroup, false)) {
                BigDecimal newQuantity = sci.getQuantity().add(quantity);
                try {
                    BigDecimal minQuantity = getMinimumOrderQuantity(getDelegator(),sci.getBasePrice(), productId);
                    if(newQuantity.compareTo(minQuantity) < 0) {
                        newQuantity = minQuantity;
                    }
                } catch (GenericEntityException e) {
                    Debug.logError(e, module);
                }
                if ("RENTAL_ORDER_ITEM".equals(sci.getItemType())) {
                    // check to see if the related fixed asset is available for the new quantity
                    String isAvailable = ShoppingCartItem.checkAvailability(productId, newQuantity, reservStart, reservLength, this);
                    if (isAvailable.compareTo("OK") != 0) {
                        Map<String, Object> messageMap = UtilMisc.<String, Object>toMap("productId", productId, "availableMessage", isAvailable);
                        String excMsg = UtilProperties.getMessage(ShoppingCartItem.resource, "item.product_not_available", messageMap, this.getLocale());
                        Debug.logInfo(excMsg, module);
                        throw new CartItemModifyException(isAvailable);
                    }
                }

                if (Debug.verboseOn()) {
                    Debug.logVerbose("Found a match for id " + productId + " on line " + i + ", updating quantity to " + newQuantity, module);
                }
                sci.setQuantity(newQuantity, dispatcher, this);

                if ("PURCHASE_ORDER".equals(getOrderType())) {
                    supplierProduct = getSupplierProduct(productId, newQuantity, dispatcher);
                    if (supplierProduct != null && supplierProduct.getBigDecimal("lastPrice") != null) {
                        sci.setSupplierProductId(supplierProduct.getString("supplierProductId"));
                        sci.setBasePrice(supplierProduct.getBigDecimal("lastPrice"));
                        sci.setName(ShoppingCartItem.getPurchaseOrderItemDescription(sci.getProduct(), supplierProduct, this.getLocale(), dispatcher));
                    } else {
                       throw new CartItemModifyException("SupplierProduct not found");
                    }
                 }
                return i;
            }
        }
        // Add the new item to the shopping cart if it wasn't found.
        ShoppingCartItem item = null;
        if ("PURCHASE_ORDER".equals(getOrderType())) {
            supplierProduct = getSupplierProduct(productId, quantity, dispatcher);
            if (supplierProduct != null || "_NA_".equals(this.getPartyId())) {
                 item = ShoppingCartItem.makePurchaseOrderItem(0, productId, selectedAmount, quantity, features, attributes, prodCatalogId, configWrapper, itemType, itemGroup, dispatcher, this, supplierProduct, shipBeforeDate, shipAfterDate, cancelBackOrderDate);
            } else {
                throw new CartItemModifyException("SupplierProduct not found");
            }
        } else {
            try {
                BigDecimal minQuantity = getMinimumOrderQuantity(getDelegator(),null, productId);
                if(quantity.compareTo(minQuantity) < 0) {
                    quantity = minQuantity;
                }
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
            }
            item = ShoppingCartItem.makeItem(0, productId, selectedAmount, quantity, null,
                    reservStart, reservLength, reservPersons, accommodationMapId, accommodationSpotId, shipBeforeDate, shipAfterDate, reserveAfterDate,
                    features, attributes, prodCatalogId, configWrapper, itemType, itemGroup, dispatcher,
                    this, Boolean.TRUE, Boolean.TRUE, parentProductId, Boolean.FALSE, Boolean.FALSE);
        }
        // add order item attributes
        if (UtilValidate.isNotEmpty(orderItemAttributes)) {
            for (Entry<String, String> entry : orderItemAttributes.entrySet()) {
                item.setOrderItemAttribute(entry.getKey(), entry.getValue());
            }
        }

        return this.addItem(0, item);

    }

    /** Add a non-product item to the shopping cart.
     * @return the new item index
     * @throws CartItemModifyException
     */
    public int addNonProductItem(String itemType, String description, String categoryId, BigDecimal price, BigDecimal quantity,
            Map<String, Object> attributes, String prodCatalogId, String itemGroupNumber, LocalDispatcher dispatcher) throws CartItemModifyException {
        ShoppingCart.ShoppingCartItemGroup itemGroup = this.getItemGroupByNumber(itemGroupNumber);
        return this.addItem(0, ShoppingCartItem.makeItem(0, itemType, description, categoryId, price, null, quantity, attributes, prodCatalogId, itemGroup, dispatcher, this, Boolean.TRUE));
    }

    /** Add an item to the shopping cart. */
    public int addItem(int index, ShoppingCartItem item) throws CartItemModifyException {
        if (isReadOnlyCart()) {
           throw new CartItemModifyException("Cart items cannot be changed");
        }
        if (!cartLines.contains(item)) {
            // If the billing address is already set, verify if the new product
            // is available in the address' geo
            GenericValue product = item.getProduct();
            if (product != null && isSalesOrder()) {
                GenericValue billingAddress = this.getBillingAddress();
                if (billingAddress != null) {
                    if (!ProductWorker.isBillableToAddress(product, billingAddress)) {
                        throw new CartItemModifyException("The billing address is not compatible with ProductGeos rules of this product.");
                    }
                }
            }
            cartLines.add(index, item);
            return index;
        }
        return this.getItemIndex(item);
    }

    /** Add an item to the shopping cart. */
    public int addItemToEnd(String productId, BigDecimal amount, BigDecimal quantity, BigDecimal unitPrice, HashMap<String, GenericValue> features, HashMap<String, Object> attributes, String prodCatalogId, String itemType, ProductConfigWrapper configWrapper, LocalDispatcher dispatcher, Boolean triggerExternalOps, Boolean triggerPriceRules) throws CartItemModifyException, ItemNotFoundException {
        return addItemToEnd(ShoppingCartItem.makeItem(null, productId, amount, quantity, unitPrice, null, null, null, null, null, features, attributes, prodCatalogId, configWrapper, itemType, null, dispatcher, this, triggerExternalOps, triggerPriceRules, null, Boolean.FALSE, Boolean.FALSE));
    }

    /** Add an item to the shopping cart. */
    public int addItemToEnd(String productId, BigDecimal amount, BigDecimal quantity, BigDecimal unitPrice, HashMap<String, GenericValue> features, HashMap<String, Object> attributes, String prodCatalogId, String itemType, LocalDispatcher dispatcher, Boolean triggerExternalOps, Boolean triggerPriceRules) throws CartItemModifyException, ItemNotFoundException {
        return addItemToEnd(productId, amount, quantity, unitPrice, features, attributes, prodCatalogId, itemType, dispatcher, triggerExternalOps, triggerPriceRules, Boolean.FALSE, Boolean.FALSE);
    }

    /** Add an (rental)item to the shopping cart. */
    public int addItemToEnd(String productId, BigDecimal amount, BigDecimal quantity, BigDecimal unitPrice, Timestamp reservStart, BigDecimal reservLength, BigDecimal reservPersons, HashMap<String, GenericValue> features, HashMap<String, Object> attributes, String prodCatalogId, String itemType, LocalDispatcher dispatcher, Boolean triggerExternalOps, Boolean triggerPriceRules) throws CartItemModifyException, ItemNotFoundException {
        return addItemToEnd(ShoppingCartItem.makeItem(null, productId, amount, quantity, unitPrice, reservStart, reservLength, reservPersons, null, null, features, attributes, prodCatalogId, null, itemType, null, dispatcher, this, triggerExternalOps, triggerPriceRules, null, Boolean.FALSE, Boolean.FALSE));
    }

    /** Add an (rental)item to the shopping cart. */
    public int addItemToEnd(String productId, BigDecimal amount, BigDecimal quantity, BigDecimal unitPrice, Timestamp reservStart, BigDecimal reservLength, BigDecimal reservPersons, HashMap<String, GenericValue> features, HashMap<String, Object> attributes, String prodCatalogId, String itemType, LocalDispatcher dispatcher, Boolean triggerExternalOps, Boolean triggerPriceRules, Boolean skipInventoryChecks, Boolean skipProductChecks) throws CartItemModifyException, ItemNotFoundException {
        return addItemToEnd(ShoppingCartItem.makeItem(null, productId, amount, quantity, unitPrice, reservStart, reservLength, reservPersons, null, null, features, attributes, prodCatalogId, null, itemType, null, dispatcher, this, triggerExternalOps, triggerPriceRules, null, skipInventoryChecks, skipProductChecks));
    }

    /** Add an (rental/aggregated)item to the shopping cart. */
    public int addItemToEnd(String productId, BigDecimal amount, BigDecimal quantity, BigDecimal unitPrice, Timestamp reservStart, BigDecimal reservLength, BigDecimal reservPersons, HashMap<String, GenericValue> features, HashMap<String, Object> attributes, String prodCatalogId, ProductConfigWrapper configWrapper, String itemType, LocalDispatcher dispatcher, Boolean triggerExternalOps, Boolean triggerPriceRules, Boolean skipInventoryChecks, Boolean skipProductChecks) throws CartItemModifyException, ItemNotFoundException {
        return addItemToEnd(ShoppingCartItem.makeItem(null, productId, amount, quantity, unitPrice, reservStart, reservLength, reservPersons, null, null, features, attributes, prodCatalogId, configWrapper, itemType, null, dispatcher, this, triggerExternalOps, triggerPriceRules, null, skipInventoryChecks, skipProductChecks));
    }

    /** Add an accommodation(rental)item to the shopping cart. */
    public int addItemToEnd(String productId, BigDecimal amount, BigDecimal quantity, BigDecimal unitPrice, Timestamp reservStart, BigDecimal reservLength, BigDecimal reservPersons, String accommodationMapId, String accommodationSpotId, HashMap<String, GenericValue> features, HashMap<String, Object> attributes, String prodCatalogId, String itemType, LocalDispatcher dispatcher, Boolean triggerExternalOps, Boolean triggerPriceRules) throws CartItemModifyException, ItemNotFoundException {
        return addItemToEnd(ShoppingCartItem.makeItem(null, productId, amount, quantity, unitPrice, reservStart, reservLength, reservPersons, accommodationMapId, accommodationSpotId, null, null, features, attributes, prodCatalogId, null, itemType, null, dispatcher, this, triggerExternalOps, triggerPriceRules, null, Boolean.FALSE, Boolean.FALSE));
    }

    /** Add an accommodation(rental)item to the shopping cart. */
    public int addItemToEnd(String productId, BigDecimal amount, BigDecimal quantity, BigDecimal unitPrice, Timestamp reservStart, BigDecimal reservLength, BigDecimal reservPersons, String accommodationMapId, String accommodationSpotId, HashMap<String, GenericValue> features, HashMap<String, Object> attributes, String prodCatalogId, String itemType, LocalDispatcher dispatcher, Boolean triggerExternalOps, Boolean triggerPriceRules, Boolean skipInventoryChecks, Boolean skipProductChecks) throws CartItemModifyException, ItemNotFoundException {
        return addItemToEnd(ShoppingCartItem.makeItem(null, productId, amount, quantity, unitPrice, reservStart, reservLength, reservPersons, accommodationMapId, accommodationSpotId, null, null, features, attributes, prodCatalogId, null, itemType, null, dispatcher, this, triggerExternalOps, triggerPriceRules, null, skipInventoryChecks, skipProductChecks));
    }

    /** Add an accommodation(rental/aggregated)item to the shopping cart. */
    public int addItemToEnd(String productId, BigDecimal amount, BigDecimal quantity, BigDecimal unitPrice, Timestamp reservStart, BigDecimal reservLength, BigDecimal reservPersonsDbl,String accommodationMapId, String accommodationSpotId, HashMap<String, GenericValue> features, HashMap<String, Object> attributes, String prodCatalogId, ProductConfigWrapper configWrapper, String itemType, LocalDispatcher dispatcher, Boolean triggerExternalOps, Boolean triggerPriceRules, Boolean skipInventoryChecks, Boolean skipProductChecks) throws CartItemModifyException, ItemNotFoundException {
        return addItemToEnd(ShoppingCartItem.makeItem(null, productId, amount, quantity, unitPrice, reservStart, reservLength, reservPersonsDbl, accommodationMapId, accommodationSpotId, null, null, features, attributes, prodCatalogId, configWrapper, itemType, null, dispatcher, this, triggerExternalOps, triggerPriceRules, null, skipInventoryChecks, skipProductChecks));
    }

    /** Add an item to the shopping cart. */
    public int addItemToEnd(String productId, BigDecimal amount, BigDecimal quantity, BigDecimal unitPrice, HashMap<String, GenericValue> features, HashMap<String, Object> attributes, String prodCatalogId, String itemType, LocalDispatcher dispatcher, Boolean triggerExternalOps, Boolean triggerPriceRules, Boolean skipInventoryChecks, Boolean skipProductChecks) throws CartItemModifyException, ItemNotFoundException {
        return addItemToEnd(ShoppingCartItem.makeItem(null, productId, amount, quantity, unitPrice, null, null, null, null, null, features, attributes, prodCatalogId, null, itemType, null, dispatcher, this, triggerExternalOps, triggerPriceRules, null, skipInventoryChecks, skipProductChecks));
    }

    /** Add an item to the shopping cart. */
    public int addItemToEnd(ShoppingCartItem item) throws CartItemModifyException {
        return addItem(cartLines.size(), item);
    }

    /** Get a ShoppingCartItem from the cart object. */
    public ShoppingCartItem findCartItem(String productId, Map<String, GenericValue> features, Map<String, Object> attributes, String prodCatalogId, BigDecimal selectedAmount) {
        // Check for existing cart item.
        for (int i = 0; i < this.cartLines.size(); i++) {
            ShoppingCartItem cartItem = cartLines.get(i);

            if (cartItem.equals(productId, features, attributes, prodCatalogId, selectedAmount)) {
                return cartItem;
            }
        }
        return null;
    }

    /** Get all ShoppingCartItems from the cart object with the given productId. */
    public List<ShoppingCartItem> findAllCartItems(String productId) {
        return this.findAllCartItems(productId, null);
    }
    /** Get all ShoppingCartItems from the cart object with the given productId and optional groupNumber to limit it to a specific item group */
    public List<ShoppingCartItem> findAllCartItems(String productId, String groupNumber) {
        if (productId == null) {
            return this.items();
        }

        List<ShoppingCartItem> itemsToReturn = new LinkedList<>();
        // Check for existing cart item.
        for (ShoppingCartItem cartItem : cartLines) {
            if (UtilValidate.isNotEmpty(groupNumber) && !cartItem.isInItemGroup(groupNumber)) {
                continue;
            }
            if (productId.equals(cartItem.getProductId())) {
                itemsToReturn.add(cartItem);
            }
        }
        return itemsToReturn;
    }

    /** Get all ShoppingCartItems from the cart object with the given productCategoryId and optional groupNumber to limit it to a specific item group */
    public List<ShoppingCartItem> findAllCartItemsInCategory(String productCategoryId, String groupNumber) {
        if (productCategoryId == null) {
            return this.items();
        }

        Delegator delegator = this.getDelegator();
        List<ShoppingCartItem> itemsToReturn = new LinkedList<>();
        try {
            // Check for existing cart item
            for (ShoppingCartItem cartItem : cartLines) {

                if (UtilValidate.isNotEmpty(groupNumber) && !cartItem.isInItemGroup(groupNumber)) {
                    continue;
                }
                if (CategoryWorker.isProductInCategory(delegator, cartItem.getProductId(), productCategoryId)) {
                    itemsToReturn.add(cartItem);
                } else {
                }
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, "Error getting cart items that are in a category: " + e.toString(), module);
        }
        return itemsToReturn;
    }

    /** Remove quantity 0 ShoppingCartItems from the cart object. */
    public void removeEmptyCartItems() {
        // Check for existing cart item.
        for (int i = 0; i < this.cartLines.size();) {
            ShoppingCartItem cartItem = cartLines.get(i);

            if (cartItem.getQuantity().compareTo(BigDecimal.ZERO) == 0) {
                this.clearItemShipInfo(cartItem);
                cartLines.remove(i);
            } else {
                i++;
            }
        }
    }

    // =============== some misc utility methods, mostly for dealing with lists of items =================
    public void removeExtraItems(List<ShoppingCartItem> multipleItems, LocalDispatcher dispatcher, int maxItems) throws CartItemModifyException {
        // if 1 or 0 items, do nothing
        if (multipleItems.size() <= maxItems) {
            return;
        }

        // remove all except first <maxItems> in list from the cart, first because new cart items are added to the beginning...
        List<ShoppingCartItem> localList = new LinkedList<>();
        localList.addAll(multipleItems);
        // the ones to keep...
        for (int i=0; i<maxItems; i++) {
            localList.remove(0);
        }
        for (ShoppingCartItem item : localList) {
            this.removeCartItem(item, dispatcher);
        }
    }

    public static BigDecimal getItemsTotalQuantity(List<ShoppingCartItem> cartItems) {
        BigDecimal totalQuantity = BigDecimal.ZERO;
        for (ShoppingCartItem item : cartItems) {
            totalQuantity = totalQuantity.add(item.getQuantity());
        }
        return totalQuantity;
    }

    public static List<GenericValue> getItemsProducts(List<ShoppingCartItem> cartItems) {
        List<GenericValue> productList = new LinkedList<>();
        for (ShoppingCartItem item : cartItems) {
            GenericValue product = item.getProduct();
            if (product != null) {
                productList.add(product);
            }
        }
        return productList;
    }

    public void ensureItemsQuantity(List<ShoppingCartItem> cartItems, LocalDispatcher dispatcher, BigDecimal quantity) throws CartItemModifyException {
        for (ShoppingCartItem item : cartItems) {
            if (item.getQuantity() != quantity) {
                item.setQuantity(quantity, dispatcher, this);
            }
        }
    }

    public BigDecimal ensureItemsTotalQuantity(List<ShoppingCartItem> cartItems, LocalDispatcher dispatcher, BigDecimal quantity) throws CartItemModifyException {
        BigDecimal quantityRemoved = BigDecimal.ZERO;
        // go through the items and reduce quantityToKeep by the item quantities until it is 0, then remove the remaining...
        BigDecimal quantityToKeep = quantity;
        for (ShoppingCartItem item : cartItems) {
            if (quantityToKeep.compareTo(item.getQuantity()) >= 0) {
                // quantityToKeep sufficient to keep it all... just reduce quantityToKeep and move on
                quantityToKeep = quantityToKeep.subtract(item.getQuantity());
            } else {
                // there is more in this than we want to keep, so reduce the quantity, or remove altogether...
                if (quantityToKeep.compareTo(BigDecimal.ZERO) == 0) {
                    // nothing left to keep, just remove it...
                    quantityRemoved = quantityRemoved.add(item.getQuantity());
                    this.removeCartItem(item, dispatcher);
                } else {
                    // there is some to keep, so reduce quantity to quantityToKeep, at this point we know we'll take up all of the rest of the quantityToKeep
                    quantityRemoved = quantityRemoved.add(item.getQuantity().subtract(quantityToKeep));
                    item.setQuantity(quantityToKeep, dispatcher, this);
                    quantityToKeep = BigDecimal.ZERO;
                }
            }
        }
        return quantityRemoved;
    }

    // ============== WorkEffort related methods ===============
    public boolean containAnyWorkEffortCartItems() {
        // Check for existing cart item.
        for (ShoppingCartItem cartItem : this.cartLines) {
            if ("RENTAL_ORDER_ITEM".equals(cartItem.getItemType())) {  // create workeffort items?
                return true;
            }
        }
        return false;
    }

    public boolean containAllWorkEffortCartItems() {
        // Check for existing cart item.
        for (ShoppingCartItem cartItem : this.cartLines) {
            if (!"RENTAL_ORDER_ITEM".equals(cartItem.getItemType())) { // not a item to create workefforts?
                return false;
            }
        }
        return true;
    }

    /**
     * Check to see if the cart contains only Digital Goods, ie no Finished Goods and no Finished/Digital Goods, et cetera.
     * This is determined by making sure no Product has a type where ProductType.isPhysical!=N.
     */
    public boolean containOnlyDigitalGoods() {
        for (ShoppingCartItem cartItem : this.cartLines) {
            GenericValue product = cartItem.getProduct();
            try {
                GenericValue productType = product.getRelatedOne("ProductType", true);
                if (productType == null || !"N".equals(productType.getString("isPhysical"))) {
                    return false;
                }
            } catch (GenericEntityException e) {
                Debug.logError(e, "Error looking up ProductType: " + e.toString(), module);
                // consider this not a digital good if we don't have "proof"
                return false;
            }
        }
        return true;
    }

    /**
     * Check to see if the ship group contains only Digital Goods, ie no Finished Goods and no Finished/Digital Goods, et cetera.
     * This is determined by making sure no Product has a type where ProductType.isPhysical!=N.
     */
    public boolean containOnlyDigitalGoods(int shipGroupIdx) {
        CartShipInfo shipInfo = getShipInfo(shipGroupIdx);
        for (ShoppingCartItem cartItem: shipInfo.getShipItems()) {
            GenericValue product = cartItem.getProduct();
            try {
                GenericValue productType = product.getRelatedOne("ProductType", true);
                if (productType == null || !"N".equals(productType.getString("isPhysical"))) {
                    return false;
                }
            } catch (GenericEntityException e) {
                Debug.logError(e, "Error looking up ProductType: " + e.toString(), module);
                // consider this not a digital good if we don't have "proof"
                return false;
            }
        }
        return true;
    }

    /** Returns this item's index. */
    public int getItemIndex(ShoppingCartItem item) {
        return cartLines.indexOf(item);
    }

    /** Get a ShoppingCartItem from the cart object. */
    public ShoppingCartItem findCartItem(int index) {
        if (cartLines.size() <= index) {
            return null;
        }
        return cartLines.get(index);

    }

    public ShoppingCartItem findCartItem(String orderItemSeqId) {
        if (orderItemSeqId != null) {
            for (int i = 0; i < this.cartLines.size(); i++) {
                ShoppingCartItem cartItem = cartLines.get(i);
                String itemSeqId = cartItem.getOrderItemSeqId();
                if (itemSeqId != null && orderItemSeqId.equals(itemSeqId)) {
                    return cartItem;
                }
            }
        }
        return null;
    }

    public void removeCartItem(ShoppingCartItem item, LocalDispatcher dispatcher) throws CartItemModifyException {
        if (item == null) {
            return;
        }
        this.removeCartItem(this.getItemIndex(item), dispatcher);
    }

    /** Remove an item from the cart object. */
    public void removeCartItem(int index, LocalDispatcher dispatcher) throws CartItemModifyException {
        if (isReadOnlyCart()) {
           throw new CartItemModifyException("Cart items cannot be changed");
        }
        if (index < 0) {
            return;
        }
        if (cartLines.size() <= index) {
            return;
        }
        ShoppingCartItem item = cartLines.remove(index);

        // set quantity to 0 to trigger necessary events, but skip price calc and inventory checks
        item.setQuantity(BigDecimal.ZERO, dispatcher, this, true, true, false, true);
    }

    /** Moves a line item to a different index. */
    public void moveCartItem(int fromIndex, int toIndex) {
        if (toIndex < fromIndex) {
            cartLines.add(toIndex, cartLines.remove(fromIndex));
        } else if (toIndex > fromIndex) {
            cartLines.add(toIndex - 1, cartLines.remove(fromIndex));
        }
    }

    /** Returns the number of items in the cart object. */
    public int size() {
        return cartLines.size();
    }

    /** Returns a Collection of items in the cart object. */
    public List<ShoppingCartItem> items() {
        List<ShoppingCartItem> result = new LinkedList<>();
        result.addAll(cartLines);
        return result;
    }

    /** Returns an iterator of cart items. */
    @Override
    public Iterator<ShoppingCartItem> iterator() {
        return cartLines.iterator();
    }

    public ShoppingCart.ShoppingCartItemGroup getItemGroupByNumber(String groupNumber) {
        if (UtilValidate.isEmpty(groupNumber)) {
            return null;
        }
        return this.itemGroupByNumberMap.get(groupNumber);
    }

    /** Creates a new Item Group and returns the groupNumber that represents it */
    public String addItemGroup(String groupName, String parentGroupNumber) {
        ShoppingCart.ShoppingCartItemGroup parentGroup = this.getItemGroupByNumber(parentGroupNumber);
        ShoppingCart.ShoppingCartItemGroup newGroup = new ShoppingCart.ShoppingCartItemGroup(this.nextGroupNumber, groupName, parentGroup);
        this.nextGroupNumber++;
        this.itemGroupByNumberMap.put(newGroup.getGroupNumber(), newGroup);
        return newGroup.getGroupNumber();
    }

    public ShoppingCartItemGroup addItemGroup(GenericValue itemGroupValue) throws GenericEntityException {
        if (itemGroupValue == null) {
            return null;
        }
        String itemGroupNumber = itemGroupValue.getString("orderItemGroupSeqId");
        ShoppingCartItemGroup itemGroup = this.getItemGroupByNumber(itemGroupNumber);
        if (itemGroup == null) {
            ShoppingCartItemGroup parentGroup = addItemGroup(itemGroupValue.getRelatedOne("ParentOrderItemGroup", true));
            itemGroup = new ShoppingCartItemGroup(itemGroupNumber, itemGroupValue.getString("groupName"), parentGroup);
            int parsedGroupNumber = Integer.parseInt(itemGroupNumber);
            if (parsedGroupNumber > this.nextGroupNumber) {
                this.nextGroupNumber = parsedGroupNumber + 1;
            }
            this.itemGroupByNumberMap.put(itemGroupNumber, itemGroup);
        }
        return itemGroup;
    }

    public List<ShoppingCartItem> getCartItemsInNoGroup() {
        List<ShoppingCartItem> cartItemList = new LinkedList<>();
        for (ShoppingCartItem cartItem : cartLines) {
            if (cartItem.getItemGroup() == null) {
                cartItemList.add(cartItem);
            }
        }
        return cartItemList;
    }

    public List<ShoppingCartItem> getCartItemsInGroup(String groupNumber) {
        List<ShoppingCartItem> cartItemList = new LinkedList<>();
        ShoppingCart.ShoppingCartItemGroup itemGroup = this.getItemGroupByNumber(groupNumber);
        if (itemGroup != null) {
            for (ShoppingCartItem cartItem : cartLines) {
                if (itemGroup.equals(cartItem.getItemGroup())) {
                    cartItemList.add(cartItem);
                }
            }
        }
        return cartItemList;
    }

    public void deleteItemGroup(String groupNumber) {
        ShoppingCartItemGroup itemGroup = this.getItemGroupByNumber(groupNumber);
        if (itemGroup != null) {
            // go through all cart items and remove from group if they are in it
            List<ShoppingCartItem> cartItemList = this.getCartItemsInGroup(groupNumber);
            for (ShoppingCartItem cartItem : cartItemList) {
                cartItem.setItemGroup(null);
            }

            // if this is a parent of any set them to this group's parent (or null)
            for (ShoppingCartItemGroup otherItemGroup : this.itemGroupByNumberMap.values()) {
                if (itemGroup.equals(otherItemGroup.getParentGroup())) {
                    otherItemGroup.inheritParentsParent();
                }
            }

            // finally, remove the itemGroup...
            this.itemGroupByNumberMap.remove(groupNumber);
        }
    }

    //=======================================================
    // Other General Info Maintenance Methods
    //=======================================================

    /** Gets the userLogin associated with the cart; may be null */
    public GenericValue getUserLogin() {
        return this.userLogin;
    }

    public void setUserLogin(GenericValue userLogin, LocalDispatcher dispatcher) throws CartItemModifyException {
        this.userLogin = userLogin;
        this.handleNewUser(dispatcher);
    }

    protected void setUserLogin(GenericValue userLogin) {
        if (this.userLogin == null) {
            this.userLogin = userLogin;
        } else {
            throw new IllegalArgumentException("Cannot change UserLogin object with this method");
        }
    }

    public GenericValue getAutoUserLogin() {
        return this.autoUserLogin;
    }

    public void setAutoUserLogin(GenericValue autoUserLogin, LocalDispatcher dispatcher) throws CartItemModifyException {
        this.autoUserLogin = autoUserLogin;
        if (getUserLogin() == null) {
            this.handleNewUser(dispatcher);
        }
    }

    protected void setAutoUserLogin(GenericValue autoUserLogin) {
        if (this.autoUserLogin == null) {
            this.autoUserLogin = autoUserLogin;
        } else {
            throw new IllegalArgumentException("Cannot change AutoUserLogin object with this method");
        }
    }

    public void handleNewUser(LocalDispatcher dispatcher) throws CartItemModifyException {
        String partyId = this.getPartyId();
        if (UtilValidate.isNotEmpty(partyId)) {
            // recalculate all prices
            for (ShoppingCartItem cartItem : this) {
                cartItem.updatePrice(dispatcher, this);
            }

            // check all promo codes, remove on failed check
            Iterator<String> promoCodeIter = this.productPromoCodes.iterator();
            while (promoCodeIter.hasNext()) {
                String promoCode = promoCodeIter.next();
                String checkResult = ProductPromoWorker.checkCanUsePromoCode(promoCode, partyId, this.getDelegator(), locale);
                if (checkResult != null) {
                    promoCodeIter.remove();
                    Debug.logWarning(UtilProperties.getMessage(resource_error,"OrderOnUserChangePromoCodeWasRemovedBecause", UtilMisc.toMap("checkResult",checkResult), locale), module);
                }
            }

            // rerun promotions
            ProductPromoWorker.doPromotions(this, dispatcher);
        }
    }

    public String getExternalId() {
        return this.externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public String getInternalCode() {
        return this.internalCode;
    }

    public void setInternalCode(String internalCode) {
        this.internalCode = internalCode;
    }

    public String getWebSiteId() {
        return this.webSiteId;
    }

    public void setWebSiteId(String webSiteId) {
        this.webSiteId = webSiteId;
    }

    /**
     * Set ship before date for a particular ship group
     * @param idx
     * @param shipBeforeDate
     */
   public void setShipBeforeDate(int idx, Timestamp shipBeforeDate) {
       CartShipInfo csi = this.getShipInfo(idx);
       csi.shipBeforeDate  = shipBeforeDate;
   }

   /**
    * Set ship before date for ship group 0
    * @param shipBeforeDate
    */
   public void setShipBeforeDate(Timestamp shipBeforeDate) {
       this.setShipBeforeDate(0, shipBeforeDate);
   }

   /**
    * Get ship before date for a particular ship group
    * @param idx the ship group number
    * @return ship before date for a given ship group
    */
   public Timestamp getShipBeforeDate(int idx) {
       CartShipInfo csi = this.getShipInfo(idx);
       return csi.shipBeforeDate;
   }

   /**
    * Get ship before date for ship group 0
    * @return ship before date for the first ship group
    */
   public Timestamp getShipBeforeDate() {
       return this.getShipBeforeDate(0);
   }

   /**
    * Set ship after date for a particular ship group
    * @param idx the ship group number
    * @param shipAfterDate the ship after date to be set for the given ship group
    */
   public void setShipAfterDate(int idx, Timestamp shipAfterDate) {
       CartShipInfo csi = this.getShipInfo(idx);
       csi.shipAfterDate  = shipAfterDate;
   }

   /**
    * Set ship after date for a particular ship group
    * @param shipAfterDate the ship after date to be set for the first ship group
    */
   public void setShipAfterDate(Timestamp shipAfterDate) {
       this.setShipAfterDate(0, shipAfterDate);
   }

   /**
    * Get ship after date for a particular ship group
    * @param idx the ship group number
    * @return return the ship after date for the given ship group
    */
   public Timestamp getShipAfterDate(int idx) {
       CartShipInfo csi = this.getShipInfo(idx);
       return csi.shipAfterDate;
   }

   /**
    * Get ship after date for ship group 0
    * @return return the ship after date for the first ship group
    */
   public Timestamp getShipAfterDate() {
       return this.getShipAfterDate(0);
   }

   public void setDefaultShipBeforeDate(Timestamp defaultShipBeforeDate) {
      this.defaultShipBeforeDate = defaultShipBeforeDate != null ? (Timestamp) defaultShipBeforeDate.clone() : null;
   }

   public Timestamp getDefaultShipBeforeDate() {
       return this.defaultShipBeforeDate != null ? (Timestamp) this.defaultShipBeforeDate.clone() : null;
   }

   public void setDefaultShipAfterDate(Timestamp defaultShipAfterDate) {
       this.defaultShipAfterDate = defaultShipAfterDate != null ? (Timestamp) defaultShipAfterDate.clone() : null;
   }

    public void setCancelBackOrderDate(Timestamp cancelBackOrderDate) {
        this.cancelBackOrderDate = cancelBackOrderDate != null ? (Timestamp) cancelBackOrderDate.clone() : null;
    }

    public Timestamp getCancelBackOrderDate() {
        return this.cancelBackOrderDate != null ? (Timestamp) this.cancelBackOrderDate.clone() : null;
    }

   public Timestamp getDefaultShipAfterDate() {
       return this.defaultShipAfterDate != null ? (Timestamp) this.defaultShipAfterDate.clone() : null;
   }
   
   public void setDefaultReserveAfterDate(Timestamp defaultReserveAfterDate) {
       this.defaultReserveAfterDate = defaultReserveAfterDate != null ? (Timestamp) defaultReserveAfterDate.clone() : null;
   }

   public Timestamp getDefaultReserveAfterDate() {
       return this.defaultReserveAfterDate != null ? (Timestamp) this.defaultReserveAfterDate.clone() : null;
   }

    public String getOrderPartyId() {
        return this.orderPartyId != null ? this.orderPartyId : this.getPartyId();
    }

    public void setOrderPartyId(String orderPartyId) {
        this.orderPartyId = orderPartyId;
    }

    public String getPlacingCustomerPartyId() {
        return this.placingCustomerPartyId != null ? this.placingCustomerPartyId : this.getPartyId();
    }

    public void setPlacingCustomerPartyId(String placingCustomerPartyId) {
        this.placingCustomerPartyId = placingCustomerPartyId;
        if (UtilValidate.isEmpty(this.orderPartyId)) {
            this.orderPartyId = placingCustomerPartyId;
        }
    }

    public String getBillToCustomerPartyId() {
        return this.billToCustomerPartyId != null ? this.billToCustomerPartyId : this.getPartyId();
    }

    public void setBillToCustomerPartyId(String billToCustomerPartyId) {
        this.billToCustomerPartyId = billToCustomerPartyId;
        if ((UtilValidate.isEmpty(this.orderPartyId)) && !("PURCHASE_ORDER".equals(orderType))) {
            this.orderPartyId = billToCustomerPartyId;  // orderPartyId should be bill-to-customer when it is not a purchase order
        }
    }

    public String getShipToCustomerPartyId() {
        return this.shipToCustomerPartyId != null ? this.shipToCustomerPartyId : this.getPartyId();
    }

    public void setShipToCustomerPartyId(String shipToCustomerPartyId) {
        this.shipToCustomerPartyId = shipToCustomerPartyId;
        if (UtilValidate.isEmpty(this.orderPartyId)) {
            this.orderPartyId = shipToCustomerPartyId;
        }
    }

    public String getEndUserCustomerPartyId() {
        return this.endUserCustomerPartyId != null ? this.endUserCustomerPartyId : this.getPartyId();
    }

    public void setEndUserCustomerPartyId(String endUserCustomerPartyId) {
        this.endUserCustomerPartyId = endUserCustomerPartyId;
        if (UtilValidate.isEmpty(this.orderPartyId)) {
            this.orderPartyId = endUserCustomerPartyId;
        }
    }

    public String getBillFromVendorPartyId() {
        return this.billFromVendorPartyId != null ? this.billFromVendorPartyId : this.getPartyId();
    }

    public void setBillFromVendorPartyId(String billFromVendorPartyId) {
        this.billFromVendorPartyId = billFromVendorPartyId;
        if ((UtilValidate.isEmpty(this.orderPartyId)) && ("PURCHASE_ORDER".equals(orderType))) {
            this.orderPartyId = billFromVendorPartyId;  // orderPartyId should be bill-from-vendor when it is a purchase order
        }

    }

    public String getShipFromVendorPartyId() {
        return this.shipFromVendorPartyId != null ? this.shipFromVendorPartyId : this.getPartyId();
    }

    public void setShipFromVendorPartyId(String shipFromVendorPartyId) {
        this.shipFromVendorPartyId = shipFromVendorPartyId;
        if (UtilValidate.isEmpty(this.orderPartyId)) {
            this.orderPartyId = shipFromVendorPartyId;
        }
    }

    public String getSupplierAgentPartyId() {
        return this.supplierAgentPartyId != null ? this.supplierAgentPartyId : this.getPartyId();
    }

    public void setSupplierAgentPartyId(String supplierAgentPartyId) {
        this.supplierAgentPartyId = supplierAgentPartyId;
        if (UtilValidate.isEmpty(this.orderPartyId)) {
            this.orderPartyId = supplierAgentPartyId;
        }
    }

    public String getPartyId() {
        String partyId = this.orderPartyId;

        if (partyId == null && getUserLogin() != null) {
            partyId = getUserLogin().getString("partyId");
        }
        if (partyId == null && getAutoUserLogin() != null) {
            partyId = getAutoUserLogin().getString("partyId");
        }
        return partyId;
    }

    public void setAutoSaveListId(String id) {
        this.autoSaveListId = id;
    }

    public String getAutoSaveListId() {
        return this.autoSaveListId;
    }

    public void setLastListRestore(Timestamp time) {
        this.lastListRestore = time != null ? (Timestamp) time.clone() : null;
    }

    public Timestamp getLastListRestore() {
        return this.lastListRestore != null ? (Timestamp) this.lastListRestore.clone() : null;
    }

    public BigDecimal getPartyDaysSinceCreated(Timestamp nowTimestamp) {
        String partyId = this.getPartyId();
        if (UtilValidate.isEmpty(partyId)) {
            return null;
        }
        try {
            GenericValue party = this.getDelegator().findOne("Party", UtilMisc.toMap("partyId", partyId), true);
            if (party == null) {
                return null;
            }
            Timestamp createdDate = party.getTimestamp("createdDate");
            if (createdDate == null) {
                return null;
            }
            BigDecimal diffMillis = new BigDecimal(nowTimestamp.getTime() - createdDate.getTime());
            // millis per day: 1000.0 * 60.0 * 60.0 * 24.0 = 86400000.0
            return (diffMillis).divide(new BigDecimal("86400000"), generalRounding);
        } catch (GenericEntityException e) {
            Debug.logError(e, "Error looking up party when getting createdDate", module);
            return null;
        }
    }

    // =======================================================================
    // Methods for cart fields
    // =======================================================================

    /** Clears out the cart. */
    public void clear() {
        this.poNumber = null;
        this.orderId = null;
        this.firstAttemptOrderId = null;
        this.billingAccountId = null;
        this.billingAccountAmt = BigDecimal.ZERO;
        this.nextItemSeq = 1;

        this.agreementId = null;
        this.quoteId = null;

        this.defaultItemDeliveryDate = null;
        this.defaultItemComment = null;
        this.orderAdditionalEmails = null;

        this.readOnlyCart = false;

        this.lastListRestore = null;

        this.orderTermSet = false;
        this.orderTerms.clear();

        this.adjustments.clear();

        this.expireSingleUsePayments();
        this.cartLines.clear();
        this.itemGroupByNumberMap.clear();
        this.clearPayments();
        this.shipInfo.clear();
        this.contactMechIdsMap.clear();
        this.internalOrderNotes.clear();
        this.orderNotes.clear();
        this.attributes.clear();
        this.orderAttributes.clear();

        // clear the additionalPartyRole Map
        for (Map.Entry<String, List<String>> me : this.additionalPartyRole.entrySet()) {
            ((LinkedList<String>) me.getValue()).clear();
        }
        this.additionalPartyRole.clear();

        this.freeShippingProductPromoActions.clear();
        this.desiredAlternateGiftByAction.clear();
        this.productPromoUseInfoList.clear();
        this.productPromoCodes.clear();

        // clear the auto-save info
        if (ProductStoreWorker.autoSaveCart(this.getDelegator(), this.getProductStoreId())) {
            GenericValue ul = this.getUserLogin();
            if (ul == null) {
                ul = this.getAutoUserLogin();
            }
            // autoSaveListId shouldn't be set to null for anonymous user until the list is not cleared from the database
            if (ul != null && !"anonymous".equals(ul.getString("userLoginId"))) {
                this.autoSaveListId = null;
            }
            // load the auto-save list ID
            if (autoSaveListId == null) {
                try {
                    autoSaveListId = ShoppingListEvents.getAutoSaveListId(this.getDelegator(), null, null, ul, this.getProductStoreId());
                } catch (GeneralException e) {
                    Debug.logError(e, module);
                }
            }

            // clear the list
            if (autoSaveListId != null) {
                try {
                    org.apache.ofbiz.order.shoppinglist.ShoppingListEvents.clearListInfo(this.getDelegator(), autoSaveListId);
                } catch (GenericEntityException e) {
                    Debug.logError(e, module);
                }
            }
            this.lastListRestore = null;
            this.autoSaveListId = null;
        }
    }

    /** Sets the order type. */
    public void setOrderType(String orderType) {
        this.orderType = orderType;
    }

    /** Returns the order type. */
    public String getOrderType() {
        return this.orderType;
    }

    public void setChannelType(String channelType) {
        this.channel = channelType;
    }

    public String getChannelType() {
        return this.channel;
    }

    public boolean isPurchaseOrder() {
        return "PURCHASE_ORDER".equals(this.orderType);
    }

    public boolean isSalesOrder() {
        return "SALES_ORDER".equals(this.orderType);
    }

    /** Sets the PO Number in the cart. */
    public void setPoNumber(String poNumber) {
        this.poNumber = poNumber;
    }

    /** Returns the po number. */
    public String getPoNumber() {
        return poNumber;
    }

    public void setDefaultItemDeliveryDate(String date) {
        this.defaultItemDeliveryDate = date;
    }

    public String getDefaultItemDeliveryDate() {
        return this.defaultItemDeliveryDate;
    }

    public void setDefaultItemComment(String comment) {
        this.defaultItemComment = comment;
    }

    public String getDefaultItemComment() {
        return this.defaultItemComment;
    }

    public void setAgreementId(String agreementId) {
        this.agreementId = agreementId;
    }

    public String getAgreementId() {
        return this.agreementId;
    }

    public void setQuoteId(String quoteId) {
        this.quoteId = quoteId;
    }

    public String getQuoteId() {
        return this.quoteId;
    }

    // =======================================================================
    // Payment Method
    // =======================================================================

    public String getPaymentMethodTypeId(String paymentMethodId) {
        try {
            GenericValue pm = this.getDelegator().findOne("PaymentMethod", UtilMisc.toMap("paymentMethodId", paymentMethodId), false);
            if (pm != null) {
                return pm.getString("paymentMethodTypeId");
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
        return null;
    }

    /** Creates a CartPaymentInfo object */
    public CartPaymentInfo makePaymentInfo(String id, String refNum, BigDecimal amount) {
        CartPaymentInfo inf = new CartPaymentInfo();
        inf.refNum[0] = refNum;
        inf.amount = amount;

        if (!isPaymentMethodType(id)) {
            inf.paymentMethodTypeId = this.getPaymentMethodTypeId(id);
            inf.paymentMethodId = id;
        } else {
            inf.paymentMethodTypeId = id;
        }
        return inf;
    }

    /** Creates a CartPaymentInfo object with a possible authCode (may be null) */
    public CartPaymentInfo makePaymentInfo(String id, String refNum, String authCode, BigDecimal amount) {
        CartPaymentInfo inf = new CartPaymentInfo();
        inf.refNum[0] = refNum;
        inf.refNum[1] = authCode;
        inf.amount = amount;

        if (!isPaymentMethodType(id)) {
            inf.paymentMethodTypeId = this.getPaymentMethodTypeId(id);
            inf.paymentMethodId = id;
        } else {
            inf.paymentMethodTypeId = id;
        }
        return inf;
    }

    /** Locates the index of an existing CartPaymentInfo object or -1 if none found */
    public int getPaymentInfoIndex(String id, String refNum) {
        CartPaymentInfo thisInf = this.makePaymentInfo(id, refNum, null);
        for (int i = 0; i < paymentInfo.size(); i++) {
            CartPaymentInfo inf = paymentInfo.get(i);
            if (inf.compareTo(thisInf) == 0) {
                return i;
            }
        }
        return -1;
    }

    /** Returns the CartPaymentInfo objects which have matching fields */
    public List<CartPaymentInfo> getPaymentInfos(boolean isPaymentMethod, boolean isPaymentMethodType, boolean hasRefNum) {
        List<CartPaymentInfo> foundRecords = new LinkedList<>();
        for (CartPaymentInfo inf : paymentInfo) {
            if (isPaymentMethod && inf.paymentMethodId != null) {
                if (hasRefNum && inf.refNum != null) {
                    foundRecords.add(inf);
                } else if (!hasRefNum && inf.refNum == null) {
                    foundRecords.add(inf);
                }
            } else if (isPaymentMethodType && inf.paymentMethodTypeId != null) {
                if (hasRefNum && inf.refNum != null) {
                    foundRecords.add(inf);
                } else if (!hasRefNum && inf.refNum == null) {
                    foundRecords.add(inf);
                }
            }
        }
        return foundRecords;
    }

    /** Locates an existing CartPaymentInfo object by index */
    public CartPaymentInfo getPaymentInfo(int index) {
        return paymentInfo.get(index);
    }

    /** Locates an existing (or creates a new) CartPaymentInfo object */
    public CartPaymentInfo getPaymentInfo(String id, String refNum, String authCode, BigDecimal amount, boolean update) {
        CartPaymentInfo thisInf = this.makePaymentInfo(id, refNum, authCode, amount);
        for (CartPaymentInfo inf : paymentInfo) {
            if (inf.compareTo(thisInf) == 0) {
                // update the info
                if (update) {
                    inf.refNum[0] = refNum;
                    inf.refNum[1] = authCode;
                    inf.amount = amount;
                }
                Debug.logInfo("Returned existing PaymentInfo - " + inf.toString(), module);
                return inf;
            }
        }

        Debug.logInfo("Returned new PaymentInfo - " + thisInf.toString(), module);
        return thisInf;
    }

    /** Locates an existing (or creates a new) CartPaymentInfo object */
    public CartPaymentInfo getPaymentInfo(String id, String refNum, String authCode, BigDecimal amount) {
        return this.getPaymentInfo(id, refNum, authCode, amount, false);
    }

    /** Locates an existing (or creates a new) CartPaymentInfo object */
    public CartPaymentInfo getPaymentInfo(String id) {
        return this.getPaymentInfo(id, null, null, null, false);
    }

    /** adds a payment method/payment method type */
    public CartPaymentInfo addPaymentAmount(String id, BigDecimal amount, String refNum, String authCode, boolean isSingleUse, boolean isPresent, boolean replace) {
        CartPaymentInfo inf = this.getPaymentInfo(id, refNum, authCode, amount, replace);
        if (isSalesOrder()) {
            GenericValue billingAddress = inf.getBillingAddress(this.getDelegator());
            if (billingAddress != null) {
                // this payment method will set the billing address for the order;
                // before it is set we have to verify if the billing address is
                // compatible with the ProductGeos
                for (GenericValue product : ShoppingCart.getItemsProducts(this.cartLines)) {
                    if (!ProductWorker.isBillableToAddress(product, billingAddress)) {
                        throw new IllegalArgumentException("The billing address is not compatible with ProductGeos rules.");
                    }
                }
            }
        }
        inf.singleUse = isSingleUse;
        inf.isPresent = isPresent;
        if (replace) {
            paymentInfo.remove(inf);
        }
        paymentInfo.add(inf);

        return inf;
    }

    /** adds a payment method/payment method type */
    public CartPaymentInfo addPaymentAmount(String id, BigDecimal amount, boolean isSingleUse) {
        return this.addPaymentAmount(id, amount, null, null, isSingleUse, false, true);
    }

    /** adds a payment method/payment method type */
    public CartPaymentInfo addPaymentAmount(String id, BigDecimal amount) {
        return this.addPaymentAmount(id, amount, false);
    }

    /** adds a payment method/payment method type */
    public CartPaymentInfo addPayment(String id) {
        return this.addPaymentAmount(id, null, false);
    }

    /** returns the payment method/payment method type amount */
    public BigDecimal getPaymentAmount(String id) {
        return this.getPaymentInfo(id).amount;
    }

    public void addPaymentRef(String id, String ref, String authCode) {
        this.getPaymentInfo(id).refNum[0] = ref;
        this.getPaymentInfo(id).refNum[1] = authCode;
    }

    public String getPaymentRef(String id) {
        for (CartPaymentInfo inf : paymentInfo) {
            if (inf.paymentMethodId.equals(id) || inf.paymentMethodTypeId.equals(id)) {
                return inf.refNum[0];
            }
        }
        return null;
    }

    /** returns the total payment amounts */
    public BigDecimal getPaymentTotal() {
        BigDecimal total = BigDecimal.ZERO;
        for (CartPaymentInfo inf : paymentInfo) {
            if (inf.amount != null) {
                total = total.add(inf.amount);
            }
        }
        return total;
    }

    public int selectedPayments() {
        return paymentInfo.size();
    }

    public boolean isPaymentSelected(String id) {
        CartPaymentInfo inf = this.getPaymentInfo(id);
        return paymentInfo.contains(inf);
    }

    /** removes a specific payment method/payment method type */
    public void clearPayment(String id) {
        CartPaymentInfo inf = this.getPaymentInfo(id);
        paymentInfo.remove(inf);
    }

    /** removes a specific payment info from the list */
    public void clearPayment(int index) {
        paymentInfo.remove(index);
    }

    /** clears all payment method/payment method types */
    public void clearPayments() {
        this.expireSingleUsePayments();
        paymentInfo.clear();
    }

    /** remove all the paymentMethods based on the paymentMethodIds */
    public void clearPaymentMethodsById(List<String> paymentMethodIdsToRemove) {
        if (UtilValidate.isEmpty(paymentMethodIdsToRemove)) {
            return;
        }
        for (Iterator<CartPaymentInfo> iter = paymentInfo.iterator(); iter.hasNext();) {
            CartPaymentInfo info = iter.next();
            if (paymentMethodIdsToRemove.contains(info.paymentMethodId)) {
                iter.remove();
            }
        }
    }

    /** remove declined payment methods for an order from cart.  The idea is to call this after an attempted order is rejected */
    public void clearDeclinedPaymentMethods(Delegator delegator) {
        String orderId = this.getOrderId();
        if (UtilValidate.isNotEmpty(orderId)) {
            try {
                List<GenericValue> declinedPaymentMethods = EntityQuery.use(delegator).from("OrderPaymentPreference").where("orderId", orderId, "statusId", "PAYMENT_DECLINED").queryList();
                if (UtilValidate.isNotEmpty(declinedPaymentMethods)) {
                    List<String> paymentMethodIdsToRemove = new ArrayList<>();
                    for (GenericValue opp : declinedPaymentMethods) {
                        paymentMethodIdsToRemove.add(opp.getString("paymentMethodId"));
                    }
                    clearPaymentMethodsById(paymentMethodIdsToRemove);
                }
            } catch (GenericEntityException ex) {
                Debug.logError("Unable to remove declined payment methods from cart due to " + ex.getMessage(), module);
                return;
            }
        }
    }

    private void expireSingleUsePayments() {
        Timestamp now = UtilDateTime.nowTimestamp();
        for (CartPaymentInfo inf : paymentInfo) {
            if (inf.paymentMethodId == null || !inf.singleUse) {
                continue;
            }

            GenericValue paymentMethod = null;
            try {
                paymentMethod = this.getDelegator().findOne("PaymentMethod", UtilMisc.toMap("paymentMethodId", inf.paymentMethodId), false);
            } catch (GenericEntityException e) {
                Debug.logError(e, "ERROR: Unable to get payment method record to expire : " + inf.paymentMethodId, module);
            }
            if (paymentMethod != null) {
                paymentMethod.set("thruDate", now);
                try {
                    paymentMethod.store();
                } catch (GenericEntityException e) {
                    Debug.logError(e, "Unable to store single use PaymentMethod record : " + paymentMethod, module);
                }
            } else {
                Debug.logError("ERROR: Received back a null payment method record for expired ID : " + inf.paymentMethodId, module);
            }
        }
    }

    /** Returns the Payment Method Ids */
    public List<String> getPaymentMethodIds() {
        List<String> pmi = new LinkedList<>();
        for (CartPaymentInfo inf : paymentInfo) {
            if (inf.paymentMethodId != null) {
                pmi.add(inf.paymentMethodId);
            }
        }
        return pmi;
    }

    /** Returns the Payment Method Type Ids */
    public List<String> getPaymentMethodTypeIds() {
        List<String> pmt = new LinkedList<>();
        for (CartPaymentInfo inf : paymentInfo) {
            if (inf.paymentMethodTypeId != null) {
                pmt.add(inf.paymentMethodTypeId);
            }
        }
        return pmt;
    }

    /** Returns a list of PaymentMethod value objects selected in the cart */
    public List<GenericValue> getPaymentMethods() {
        List<GenericValue> methods = new LinkedList<>();
        if (UtilValidate.isNotEmpty(paymentInfo)) {
            for (String paymentMethodId : getPaymentMethodIds()) {
                try {
                    GenericValue paymentMethod = this.getDelegator().findOne("PaymentMethod", UtilMisc.toMap("paymentMethodId", paymentMethodId), true);
                    if (paymentMethod != null) {
                        methods.add(paymentMethod);
                    } else {
                        Debug.logError("Error getting cart payment methods, the paymentMethodId [" + paymentMethodId +"] is not valid", module);
                    }
                } catch (GenericEntityException e) {
                    Debug.logError(e, "Unable to get payment method from the database", module);
                }
            }
        }

        return methods;
    }

    /** Returns a list of PaymentMethodType value objects selected in the cart */
    public List<GenericValue> getPaymentMethodTypes() {
        List<GenericValue> types = new LinkedList<>();
        if (UtilValidate.isNotEmpty(paymentInfo)) {
            for (String id : getPaymentMethodTypeIds()) {
                try {
                    types.add(this.getDelegator().findOne("PaymentMethodType", UtilMisc.toMap("paymentMethodTypeId", id), true));
                } catch (GenericEntityException e) {
                    Debug.logError(e, "Unable to get payment method type from the database", module);
                }
            }
        }

        return types;
    }

    public List<GenericValue> getCreditCards() {
        List<GenericValue> paymentMethods = this.getPaymentMethods();
        List<GenericValue> creditCards = new LinkedList<>();
        for (GenericValue pm : paymentMethods) {
            if ("CREDIT_CARD".equals(pm.getString("paymentMethodTypeId"))) {
                try {
                    GenericValue cc = pm.getRelatedOne("CreditCard", false);
                    creditCards.add(cc);
                } catch (GenericEntityException e) {
                    Debug.logError(e, "Unable to get credit card record from payment method : " + pm, module);
                }
            }
        }

        return creditCards;
    }

    public List<GenericValue> getGiftCards() {
        List<GenericValue> paymentMethods = this.getPaymentMethods();
        List<GenericValue> giftCards = new LinkedList<>();
        for (GenericValue pm : paymentMethods) {
            if ("GIFT_CARD".equals(pm.getString("paymentMethodTypeId"))) {
                try {
                    GenericValue gc = pm.getRelatedOne("GiftCard", false);
                    giftCards.add(gc);
                } catch (GenericEntityException e) {
                    Debug.logError(e, "Unable to get gift card record from payment method : " + pm, module);
                }
            }
        }

        return giftCards;
    }

    /* determines if the id supplied is a payment method or not by searching in the entity engine */
    public boolean isPaymentMethodType(String id) {
        GenericValue paymentMethodType = null;
        try {
            paymentMethodType = this.getDelegator().findOne("PaymentMethodType", UtilMisc.toMap("paymentMethodTypeId", id), true);
        } catch (GenericEntityException e) {
            Debug.logInfo(e, "Problems getting PaymentMethodType", module);
        }
        return paymentMethodType != null;
    }

    public GenericValue getBillingAddress() {
        GenericValue billingAddress = null;
        for (CartPaymentInfo inf : paymentInfo) {
            billingAddress = inf.getBillingAddress(this.getDelegator());
            if (billingAddress != null) {
                break;
            }
        }
        return billingAddress;
    }

    /**
     * Returns ProductStoreFinActSetting based on cart's productStoreId and FinAccountHelper's defined giftCertFinAcctTypeId
     * @param delegator the delegator
     * @return returns ProductStoreFinActSetting based on cart's productStoreId
     * @throws GenericEntityException
     */
    public GenericValue getGiftCertSettingFromStore(Delegator delegator) throws GenericEntityException {
        return EntityQuery.use(delegator).from("ProductStoreFinActSetting").where("productStoreId", getProductStoreId(), "finAccountTypeId", FinAccountHelper.giftCertFinAccountTypeId).cache().queryOne();
    }

    /**
     * Determines whether pin numbers are required for gift cards, based on ProductStoreFinActSetting.  Default to true.
     * @param delegator the delegator
     * @return returns true whether pin numbers are required for gift card
     */
    public boolean isPinRequiredForGC(Delegator delegator) {
        try {
            GenericValue giftCertSettings = getGiftCertSettingFromStore(delegator);
            if (giftCertSettings != null) {
                return "Y".equals(giftCertSettings.getString("requirePinCode"));
            }
            Debug.logWarning("No product store gift certificate settings found for store [" + getProductStoreId() + "]",
                    module);
            return true;
        } catch (GenericEntityException ex) {
            Debug.logError("Error checking if store requires pin number for GC: " + ex.getMessage(), module);
            return true;
        }
    }

    /**
     * Returns whether the cart should validate gift cards against FinAccount (ie, internal gift certificates).  Defaults to false.
     * @param delegator the delegator
     * @return returns true whether the cart should validate gift cards against FinAccount
     */
    public boolean isValidateGCFinAccount(Delegator delegator) {
        try {
            GenericValue giftCertSettings = getGiftCertSettingFromStore(delegator);
            if (giftCertSettings != null) {
                return "Y".equals(giftCertSettings.getString("validateGCFinAcct"));
            }
            Debug.logWarning("No product store gift certificate settings found for store [" + getProductStoreId() + "]",
                    module);
            return false;
        } catch (GenericEntityException ex) {
            Debug.logError("Error checking if store requires pin number for GC: " + ex.getMessage(), module);
            return false;
        }
    }

    // =======================================================================
    // Billing Accounts
    // =======================================================================

    /** Sets the billing account id string. */
    public void setBillingAccount(String billingAccountId, BigDecimal amount) {
        this.billingAccountId = billingAccountId;
        this.billingAccountAmt = amount;
    }

    /** Returns the billing message string. */
    public String getBillingAccountId() {
        return this.billingAccountId;
    }

    /** Returns the amount to be billed to the billing account.*/
    public BigDecimal getBillingAccountAmount() {
        return this.billingAccountAmt;
    }

    // =======================================================================
    // Shipping Charges
    // =======================================================================

    /** Returns the order level shipping amount */
    public BigDecimal getOrderShipping() {
        return OrderReadHelper.calcOrderAdjustments(this.getAdjustments(), this.getSubTotal(), false, false, true);
    }

    // ----------------------------------------
    // Ship Group Methods
    // ----------------------------------------

    public int addShipInfo() {
        CartShipInfo csi = new CartShipInfo();
        csi.orderTypeId = getOrderType();
        shipInfo.add(csi);
        return (shipInfo.size() - 1);
    }

    public List<CartShipInfo> getShipGroups() {
        return this.shipInfo;
    }

    public Map<Integer, BigDecimal> getShipGroups(ShoppingCartItem item) {
        Map<Integer, BigDecimal> shipGroups = new LinkedHashMap<>();
        if (item != null) {
            for (int i = 0; i < this.shipInfo.size(); i++) {
                CartShipInfo csi = shipInfo.get(i);
                CartShipInfo.CartShipItemInfo csii = csi.shipItemInfo.get(item);
                if (csii != null) {
                    if (this.checkShipItemInfo(csi, csii)) {
                        shipGroups.put(i, csii.quantity);
                    }
                }
            }
        }
        return shipGroups;
    }

    public Map<Integer, BigDecimal> getShipGroups(int itemIndex) {
        return this.getShipGroups(this.findCartItem(itemIndex));
    }

    public CartShipInfo getShipInfo(int idx) {
        if (idx == -1) {
            return null;
        }

        if (shipInfo.size() == idx) {
            CartShipInfo csi = new CartShipInfo();
            csi.orderTypeId = getOrderType();
            shipInfo.add(csi);
        }

        return shipInfo.get(idx);
    }

    public int getShipGroupSize() {
        return this.shipInfo.size();
    }

    /** Returns the ShoppingCartItem (key) and quantity (value) associated with the ship group */
    public Map<ShoppingCartItem, BigDecimal> getShipGroupItems(int idx) {
        CartShipInfo csi = this.getShipInfo(idx);
        Map<ShoppingCartItem, BigDecimal> qtyMap = new HashMap<>();
        for (ShoppingCartItem item : csi.shipItemInfo.keySet()) {
            CartShipInfo.CartShipItemInfo csii = csi.shipItemInfo.get(item);
            qtyMap.put(item, csii.quantity);
        }
        return qtyMap;
    }

    public void clearItemShipInfo(ShoppingCartItem item) {
        for (int i = 0; i < shipInfo.size(); i++) {
            CartShipInfo csi = this.getShipInfo(i);
            csi.shipItemInfo.remove(item);
        }

        // DEJ20100107: commenting this out because we do NOT want to clear out ship group info since there is information there that will be lost; good enough to clear the item/group association which can be restored later (though questionable, the whole processes using this should be rewritten to not destroy information!
        // this.cleanUpShipGroups();
    }

    public void setItemShipGroupEstimate(BigDecimal amount, int idx) {
        CartShipInfo csi = this.getShipInfo(idx);
        csi.shipEstimate = amount;
    }

    /**
     * Updates the shipBefore and shipAfterDates of all ship groups that the item belongs to, re-setting
     * ship group ship before date if item ship before date is before it and ship group ship after date if
     * item ship after date is before it.
     * @param item
     */
    public void setShipGroupShipDatesFromItem(ShoppingCartItem item) {
        Map<Integer, BigDecimal> shipGroups = this.getShipGroups(item);

        if (shipGroups.keySet() != null) {
            for (Integer shipGroup : shipGroups.keySet()) {
                CartShipInfo cartShipInfo = this.getShipInfo(shipGroup);

                cartShipInfo.resetShipAfterDateIfBefore(item.getShipAfterDate());
                cartShipInfo.resetShipBeforeDateIfAfter(item.getShipBeforeDate());
            }
        }
    }

    public BigDecimal getItemShipGroupEstimate(int idx) {
        CartShipInfo csi = this.getShipInfo(idx);
        return csi.shipEstimate;
    }

    public void setItemShipGroupQty(int itemIndex, BigDecimal quantity, int idx) {
        ShoppingCartItem itemIdx = this.findCartItem(itemIndex);
        if(itemIdx != null) {
            this.setItemShipGroupQty(itemIdx, itemIndex, quantity, idx);
        }
    }

    public void setItemShipGroupQty(ShoppingCartItem item, BigDecimal quantity, int idx) {
        this.setItemShipGroupQty(item, this.getItemIndex(item), quantity, idx);
    }

    public void setItemShipGroupQty(ShoppingCartItem item, int itemIndex, BigDecimal quantity, int idx) {
        if (itemIndex > -1) {
            CartShipInfo csi = this.getShipInfo(idx);

            // never set less than zero
            if (quantity.compareTo(BigDecimal.ZERO) < 0) {
                quantity = BigDecimal.ZERO;
            }

            // never set more than quantity ordered
            if (item != null) {
                if (quantity.compareTo(item.getQuantity()) > 0) {
                    quantity = item.getQuantity();
                }


                // re-set the ship group's before and after dates based on the item's
                csi.resetShipBeforeDateIfAfter(item.getShipBeforeDate());
                csi.resetShipAfterDateIfBefore(item.getShipAfterDate());

                CartShipInfo.CartShipItemInfo csii = csi.setItemInfo(item, quantity);
                this.checkShipItemInfo(csi, csii);
            }
        }
    }

    public BigDecimal getItemShipGroupQty(ShoppingCartItem item, int idx) {
        if (item != null) {
            CartShipInfo csi = this.getShipInfo(idx);
            CartShipInfo.CartShipItemInfo csii = csi.shipItemInfo.get(item);
            if (csii != null) {
                return csii.quantity;
            }
        }
        return BigDecimal.ZERO;
    }

    public BigDecimal getItemShipGroupQty(int itemIndex, int idx) {
        return this.getItemShipGroupQty(this.findCartItem(itemIndex), idx);
    }

    public void positionItemToGroup(int itemIndex, BigDecimal quantity, int fromIndex, int toIndex, boolean clearEmptyGroups) {
        this.positionItemToGroup(this.findCartItem(itemIndex), quantity, fromIndex, toIndex, clearEmptyGroups);
    }

    public void positionItemToGroup(ShoppingCartItem item, BigDecimal quantity, int fromIndex, int toIndex, boolean clearEmptyGroups) {
        if (fromIndex == toIndex || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            // do nothing
            return;
        }

        // get the ship groups; create the TO group if needed
        CartShipInfo fromGroup = this.getShipInfo(fromIndex);
        CartShipInfo toGroup = null;
        if (toIndex == -1) {
            toGroup = new CartShipInfo();
            toGroup.orderTypeId = getOrderType();
            this.shipInfo.add(toGroup);
            toIndex = this.shipInfo.size() - 1;
        } else {
            toGroup = this.getShipInfo(toIndex);
        }

        // adjust the quantities
        if (fromGroup != null && toGroup != null) {
            BigDecimal fromQty = this.getItemShipGroupQty(item, fromIndex);
            BigDecimal toQty = this.getItemShipGroupQty(item, toIndex);
            if (fromQty.compareTo(BigDecimal.ZERO) > 0) {
                if (quantity.compareTo(fromQty) > 0) {
                    quantity = fromQty;
                }
                fromQty = fromQty.subtract(quantity);
                toQty = toQty.add(quantity);
                this.setItemShipGroupQty(item, fromQty, fromIndex);
                this.setItemShipGroupQty(item, toQty, toIndex);
            }

            if (clearEmptyGroups) {
                // remove any empty ship groups
                this.cleanUpShipGroups();
            }
        }
    }

    // removes 0 quantity items
    protected boolean checkShipItemInfo(CartShipInfo csi, CartShipInfo.CartShipItemInfo csii) {
        if (csii.quantity.compareTo(BigDecimal.ZERO) == 0 || csii.item.getQuantity().compareTo(BigDecimal.ZERO) == 0) {
            csi.shipItemInfo.remove(csii.item);
            return false;
        }
        return true;
    }

    public void cleanUpShipGroups() {
        Iterator<CartShipInfo> csi = this.shipInfo.iterator();
        while (csi.hasNext()) {
            CartShipInfo info = csi.next();
            Iterator<ShoppingCartItem> si = info.shipItemInfo.keySet().iterator();
            while (si.hasNext()) {
                ShoppingCartItem item = si.next();
                if (item.getQuantity().compareTo(BigDecimal.ZERO) == 0) {
                    si.remove();
                }
            }
            if (info.shipItemInfo.size() == 0) {
                csi.remove();
            }
        }
    }

    public int getShipInfoIndex (String shipGroupSeqId) {
        int idx = -1;
        for (int i=0; i<shipInfo.size(); i++) {
            CartShipInfo csi = shipInfo.get(i);
            if (shipGroupSeqId.equals(csi.shipGroupSeqId)) {
                idx = i;
                break;
            }
        }
        return idx;
    }

    /**
    * Return index of the ship group where the item is located
    * @return
    */
    public int getItemShipGroupIndex(int itemId) {
    int shipGroupIndex = this.getShipGroupSize() - 1;
    ShoppingCartItem item = this.findCartItem(itemId);
    int result=0;
    for (int i = 0; i <(shipGroupIndex + 1); i++) {
       CartShipInfo csi = this.getShipInfo(i);
       Iterator<ShoppingCartItem> it = csi.shipItemInfo.keySet().iterator();
        while (it.hasNext()) {
            ShoppingCartItem item2 = it.next();
            if (item.equals(item2) ) {
                result = i;
            }
        }
    }
    return result;
    }

    /** Sets the shipping contact mech id. */
    public void setShippingContactMechId(int idx, String shippingContactMechId) {
        CartShipInfo csi = this.getShipInfo(idx);
        if (isSalesOrder() && UtilValidate.isNotEmpty(shippingContactMechId)) {
            // Verify if the new address is compatible with the ProductGeos rules of
            // the products already in the cart
            GenericValue shippingAddress = null;
            try {
                shippingAddress = this.getDelegator().findOne("PostalAddress", UtilMisc.toMap("contactMechId", shippingContactMechId), false);
            } catch (GenericEntityException gee) {
                Debug.logError(gee, "Error retrieving the shipping address for contactMechId [" + shippingContactMechId + "].", module);
            }
            if (shippingAddress != null) {
                Set<ShoppingCartItem> shipItems = csi.getShipItems();
                if (UtilValidate.isNotEmpty(shipItems)) {
                    for (ShoppingCartItem cartItem : shipItems) {
                        GenericValue product = cartItem.getProduct();
                        if (product != null) {
                            if (!ProductWorker.isShippableToAddress(product, shippingAddress)) {
                                throw new IllegalArgumentException("The shipping address is not compatible with ProductGeos rules.");
                            }
                        }
                    }
                }
            }
        }
        csi.setContactMechId(shippingContactMechId);
    }

    /**
     * Sets @param shippingContactMechId in all ShipInfo(ShipGroups) associated
     * with this ShoppingCart
     * <p>
     * @param shippingContactMechId
     */
    public void setAllShippingContactMechId(String shippingContactMechId) {
        for (int x=0; x < shipInfo.size(); x++) {
            this.setShippingContactMechId(x, shippingContactMechId);
        }
    }

    /** Returns the shipping contact mech id. */
    public String getShippingContactMechId(int idx) {
        CartShipInfo csi = this.getShipInfo(idx);
        return csi.getContactMechId();
    }

    public String getShippingContactMechId() {
        return this.getShippingContactMechId(0);
    }

    /** Sets the shipment method type. */
    public void setShipmentMethodTypeId(int idx, String shipmentMethodTypeId) {
        CartShipInfo csi = this.getShipInfo(idx);
        csi.shipmentMethodTypeId = shipmentMethodTypeId;
    }

    /**
     * Sets @param shipmentMethodTypeId in all ShipInfo(ShipGroups) associated
     * with this ShoppingCart
     * <p>
     * @param shipmentMethodTypeId
     */
    public void setAllShipmentMethodTypeId(String shipmentMethodTypeId) {
        for (int x=0; x < shipInfo.size(); x++) {
            this.setShipmentMethodTypeId(x, shipmentMethodTypeId);
        }
    }

    /** Returns the shipment method type ID */
    public String getShipmentMethodTypeId(int idx) {
        CartShipInfo csi = this.getShipInfo(idx);
        return csi.shipmentMethodTypeId;
    }

    public String getShipmentMethodTypeId() {
        return this.getShipmentMethodTypeId(0);
    }

    /** Returns the shipment method type. */
    public GenericValue getShipmentMethodType(int idx) {
        String shipmentMethodTypeId = this.getShipmentMethodTypeId(idx);
        if (UtilValidate.isNotEmpty(shipmentMethodTypeId)) {
            try {
                return this.getDelegator().findOne("ShipmentMethodType",
                        UtilMisc.toMap("shipmentMethodTypeId", shipmentMethodTypeId), false);
            } catch (GenericEntityException e) {
                Debug.logWarning(e, module);
            }
        }
        return null;
    }

    /** Sets the supplier for the given ship group (drop shipment). */
    public void setSupplierPartyId(int idx, String supplierPartyId) {
        CartShipInfo csi = this.getShipInfo(idx);
        // TODO: before we set the value we have to verify if all the products
        //       already in this ship group are drop shippable from the supplier
        csi.supplierPartyId = supplierPartyId;
    }

    /** Returns the supplier for the given ship group (drop shipment). */
    public String getSupplierPartyId(int idx) {
        CartShipInfo csi = this.getShipInfo(idx);
        return csi.supplierPartyId;
    }

    /** Sets the supplier agreement for the given ship group (drop shipment). */
    public void setSupplierAgreementId(int idx, String supplierAgreementId) {
        CartShipInfo csi = this.getShipInfo(idx);
        csi.supplierAgreementId = supplierAgreementId;
    }

    /** Returns the supplier Agreement for the given ship group (drop shipment). */
    public String getSupplierAgreementId(int idx) {
        CartShipInfo csi = this.getShipInfo(idx);
        return csi.supplierAgreementId;
    }

    /** Sets the shipping instructions. */
    public void setShippingInstructions(int idx, String shippingInstructions) {
        CartShipInfo csi = this.getShipInfo(idx);
        csi.shippingInstructions = shippingInstructions;
    }

    /**
     * Sets @param shippingInstructions in all ShipInfo(ShipGroups) associated
     * with this ShoppingCart
     * <p>
     * @param shippingInstructions
     */
    public void setAllShippingInstructions(String shippingInstructions) {
        for (int x=0; x < shipInfo.size(); x++) {
            this.setShippingInstructions(x, shippingInstructions);
        }
    }

    /** Returns the shipping instructions. */
    public String getShippingInstructions(int idx) {
        CartShipInfo csi = this.getShipInfo(idx);
        return csi.shippingInstructions;
    }

    public String getShippingInstructions() {
        return this.getShippingInstructions(0);
    }

    public void setMaySplit(int idx, Boolean maySplit) {
        CartShipInfo csi = this.getShipInfo(idx);
        if (UtilValidate.isNotEmpty(maySplit)) {
            csi.setMaySplit(maySplit);
        }
    }

    /**
     * Sets @param maySplit in all ShipInfo(ShipGroups) associated
     * with this ShoppingCart
     * <p>
     * @param maySplit
     */
    public void setAllMaySplit(Boolean maySplit) {
        for (int x=0; x < shipInfo.size(); x++) {
            this.setMaySplit(x, maySplit);
        }
    }


    /** Returns Boolean.TRUE if the order may be split (null if unspecified) */
    public String getMaySplit(int idx) {
        CartShipInfo csi = this.getShipInfo(idx);
        return csi.maySplit;
    }

    public String getMaySplit() {
        return this.getMaySplit(0);
    }

    public void setGiftMessage(int idx, String giftMessage) {
        CartShipInfo csi = this.getShipInfo(idx);
        csi.giftMessage = giftMessage;
    }

    /**
     * Sets @param giftMessage in all ShipInfo(ShipGroups) associated
     * with this ShoppingCart
     * <p>
     * @param giftMessage
     */
    public void setAllGiftMessage(String giftMessage) {
        for (int x=0; x < shipInfo.size(); x++) {
            this.setGiftMessage(x, giftMessage);
        }
    }

    public String getGiftMessage(int idx) {
        CartShipInfo csi = this.getShipInfo(idx);
        return csi.giftMessage;
    }

    public String getGiftMessage() {
        return this.getGiftMessage(0);
    }

    public void setIsGift(int idx, Boolean isGift) {
        CartShipInfo csi = this.getShipInfo(idx);
        if (UtilValidate.isNotEmpty(isGift)) {
            csi.isGift = isGift ? "Y" : "N";
        }
    }

    /**
     * Sets @param isGift in all ShipInfo(ShipGroups) associated
     * with this ShoppingCart
     * <p>
     * @param isGift
     */
    public void setAllIsGift(Boolean isGift) {
        for (int x=0; x < shipInfo.size(); x++) {
            this.setIsGift(x, isGift);
        }
    }

    public String getIsGift(int idx) {
        CartShipInfo csi = this.getShipInfo(idx);
        return csi.isGift;
    }

    public String getIsGift() {
        return this.getIsGift(0);
    }

    public void setCarrierPartyId(int idx, String carrierPartyId) {
        CartShipInfo csi = this.getShipInfo(idx);
        csi.carrierPartyId = carrierPartyId;
    }

    /**
     * Sets @param carrierPartyId in all ShipInfo(ShipGroups) associated
     * with this ShoppingCart
     * <p>
     * @param carrierPartyId
     */
    public void setAllCarrierPartyId(String carrierPartyId) {
        for (int x=0; x < shipInfo.size(); x++) {
            this.setCarrierPartyId(x, carrierPartyId);
        }
    }

    public String getCarrierPartyId(int idx) {
        CartShipInfo csi = this.getShipInfo(idx);
        return csi.carrierPartyId;
    }

    public String getCarrierPartyId() {
        return this.getCarrierPartyId(0);
    }

    public String getProductStoreShipMethId(int idx) {
        CartShipInfo csi = this.getShipInfo(idx);
        return csi.productStoreShipMethId;
    }

    public String getProductStoreShipMethId() {
        return this.getProductStoreShipMethId(0);
    }

    public void setProductStoreShipMethId(int idx, String productStoreShipMethId) {
        CartShipInfo csi = this.getShipInfo(idx);
        csi.productStoreShipMethId = productStoreShipMethId;
    }

    /**
     * Sets @param productStoreShipMethId in all ShipInfo(ShipGroups) associated
     * with this ShoppingCart
     * <p>
     * @param productStoreShipMethId
     */
    public void setAllProductStoreShipMethId(String productStoreShipMethId) {
        for (int x=0; x < shipInfo.size(); x++) {
            this.setProductStoreShipMethId(x, productStoreShipMethId);
        }
    }

    public void setShipGroupFacilityId(int idx, String facilityId) {
        CartShipInfo csi = this.getShipInfo(idx);
        csi.facilityId = facilityId;
    }

    public String getShipGroupFacilityId(int idx) {
        CartShipInfo csi = this.getShipInfo(idx);
        return csi.facilityId;
    }

    public void setShipGroupVendorPartyId(int idx, String vendorPartyId) {
        CartShipInfo csi = this.getShipInfo(idx);
        csi.vendorPartyId = vendorPartyId;
    }

    public String getShipGroupVendorPartyId(int idx) {
        CartShipInfo csi = this.getShipInfo(idx);
        return csi.vendorPartyId;
    }

    public void setShipGroupSeqId(int idx, String shipGroupSeqId) {
        CartShipInfo csi = this.getShipInfo(idx);
        csi.shipGroupSeqId = shipGroupSeqId;
    }

    public String getShipGroupSeqId(int idx) {
        CartShipInfo csi = this.getShipInfo(idx);
        return csi.shipGroupSeqId;
    }

    public void setOrderAdditionalEmails(String orderAdditionalEmails) {
        this.orderAdditionalEmails = orderAdditionalEmails;
    }

    public String getOrderAdditionalEmails() {
        return orderAdditionalEmails;
    }

    public GenericValue getShippingAddress(int idx) {
        if (this.getShippingContactMechId(idx) != null) {
            try {
                return getDelegator().findOne("PostalAddress", UtilMisc.toMap("contactMechId", this.getShippingContactMechId(idx)), false);
            } catch (GenericEntityException e) {
                Debug.logWarning(e.toString(), module);
                return null;
            }
        }
        return null;
    }

    public GenericValue getOriginAddress(int idx) {
        CartShipInfo cartShipInfo = getShipInfo(idx);
        if (cartShipInfo == null) return null;
        return cartShipInfo.getOriginAddress(this);
    }

    public GenericValue getShippingAddress() {
        return this.getShippingAddress(0);
    }

    // ----------------------------------------
    // internal/public notes
    // ----------------------------------------

    public List<String> getInternalOrderNotes() {
        return this.internalOrderNotes;
    }

    public List<String> getOrderNotes() {
        return this.orderNotes;
    }

    public void addInternalOrderNote(String note) {
        this.internalOrderNotes.add(note);
    }

    public void clearInternalOrderNotes() {
        this.internalOrderNotes.clear();
    }
    public void clearOrderNotes() {
        this.orderNotes.clear();
    }

    public void addOrderNote(String note) {
        this.orderNotes.add(note);
    }

    // Preset with default values some of the checkout options to get a quicker checkout process.
    public void setDefaultCheckoutOptions(LocalDispatcher dispatcher) {
        // skip the add party screen
        this.setAttribute("addpty", "Y");
        if ("SALES_ORDER".equals(getOrderType())) {
            // checkout options for sales orders
            // set as the default shipping location the first from the list of available shipping locations
            if (this.getPartyId() != null && !"_NA_".equals(this.getPartyId())) {
                try {
                    GenericValue orderParty = this.getDelegator().findOne("Party", UtilMisc.toMap("partyId", this.getPartyId()), false);
                    Collection<GenericValue> shippingContactMechList = ContactHelper.getContactMech(orderParty, "SHIPPING_LOCATION", "POSTAL_ADDRESS", false);
                    if (UtilValidate.isNotEmpty(shippingContactMechList)) {
                        GenericValue shippingContactMech = (shippingContactMechList.iterator()).next();
                        this.setAllShippingContactMechId(shippingContactMech.getString("contactMechId"));
                    }
                } catch (GenericEntityException e) {
                    Debug.logError(e, "Error setting shippingContactMechId in setDefaultCheckoutOptions() method.", module);
                }
            }
            // set the default shipment method
            ShippingEstimateWrapper shipEstimateWrapper = org.apache.ofbiz.order.shoppingcart.shipping.ShippingEstimateWrapper.getWrapper(dispatcher, this, 0);
            GenericValue carrierShipmentMethod = EntityUtil.getFirst(shipEstimateWrapper.getShippingMethods());
            if (carrierShipmentMethod != null) {
                this.setAllShipmentMethodTypeId(carrierShipmentMethod.getString("shipmentMethodTypeId"));
                this.setAllCarrierPartyId(carrierShipmentMethod.getString("partyId"));
            }
        } else {
            // checkout options for purchase orders
            // TODO: should we select a default agreement? For now we don't do this.
            // skip the order terms selection step
            this.setOrderTermSet(true);
            // set as the default shipping location the first from the list of available shipping locations
            String companyId = this.getBillToCustomerPartyId();
            if (companyId != null) {
                // the facilityId should be set prior to triggering default options, otherwise we do not set up facility information
                String defaultFacilityId = getFacilityId();
                if (defaultFacilityId != null) {
                    GenericValue facilityContactMech = ContactMechWorker.getFacilityContactMechByPurpose(this.getDelegator(), facilityId, UtilMisc.toList("SHIPPING_LOCATION", "PRIMARY_LOCATION"));
                    if (facilityContactMech != null) {
                        this.setShippingContactMechId(0, facilityContactMech.getString("contactMechId"));
                    }
                }
            }
            // shipping options
            this.setAllShipmentMethodTypeId("NO_SHIPPING");
            this.setAllCarrierPartyId("_NA_");
            this.setAllShippingInstructions("");
            this.setAllGiftMessage("");
            this.setAllMaySplit(Boolean.TRUE);
            this.setAllIsGift(Boolean.FALSE);
        }
    }

    // Returns the tax amount for a ship group. */
    public BigDecimal getTotalSalesTax(int shipGroup) {
        CartShipInfo csi = this.getShipInfo(shipGroup);
        return csi.getTotalTax(this);
    }

    /** Returns the tax amount from the cart object. */
    public BigDecimal getTotalSalesTax() {
        BigDecimal totalTax = BigDecimal.ZERO;
        for (int i = 0; i < shipInfo.size(); i++) {
            CartShipInfo csi = this.getShipInfo(i);
            totalTax = totalTax.add(csi.getTotalTax(this)).setScale(taxCalcScale, taxRounding);
        }
        return totalTax.setScale(taxFinalScale, taxRounding);
    }

    /** Returns the shipping amount from the cart object. */
    public BigDecimal getTotalShipping() {
        BigDecimal tempShipping = BigDecimal.ZERO;

        for (CartShipInfo csi : this.shipInfo) {
            tempShipping = tempShipping.add(csi.shipEstimate);
        }

        return tempShipping;
    }

    /** Returns the item-total in the cart (not including discount/tax/shipping). */
    public BigDecimal getItemTotal() {
        BigDecimal itemTotal = BigDecimal.ZERO;
        for (ShoppingCartItem cartItem : this.cartLines) {
            itemTotal = itemTotal.add(cartItem.getBasePrice());
        }
        return itemTotal;
    }

    /** Returns the sub-total in the cart (item-total - discount). */
    public BigDecimal getSubTotal() {
        BigDecimal itemsTotal = BigDecimal.ZERO;
        for (ShoppingCartItem cartItem : this.cartLines) {
            itemsTotal = itemsTotal.add(cartItem.getItemSubTotal());
        }
        return itemsTotal;
    }

    /** Returns the total from the cart, including tax/shipping. */
    public BigDecimal getGrandTotal() {
        // sales tax and shipping are not stored as adjustments but rather as part of the ship group
        return this.getSubTotal().add(this.getTotalShipping()).add(this.getTotalSalesTax()).add(this.getOrderOtherAdjustmentTotal()).add(this.getOrderGlobalAdjustments());
    }

    public BigDecimal getDisplaySubTotal() {
        BigDecimal itemsTotal = BigDecimal.ZERO;
        for (ShoppingCartItem cartItem : this.cartLines) {
            itemsTotal = itemsTotal.add(cartItem.getDisplayItemSubTotal());
        }
        return itemsTotal;
    }
    public BigDecimal getOrderGlobalAdjustments() {
        List<GenericValue> cartAdjustments = this.getAdjustments();
        List<GenericValue> tempAdjustmentsList = new LinkedList<>();
        if (cartAdjustments != null) {
            Iterator<GenericValue> cartAdjustmentIter = cartAdjustments.iterator();
            while (cartAdjustmentIter.hasNext()) {
                GenericValue checkOrderAdjustment = cartAdjustmentIter.next();
                if (UtilValidate.isEmpty(checkOrderAdjustment.getString("shipGroupSeqId")) || DataModelConstants.SEQ_ID_NA.equals(checkOrderAdjustment.getString("shipGroupSeqId"))) {
                    tempAdjustmentsList.add(checkOrderAdjustment);
                }
            }
        }
        return OrderReadHelper.calcOrderAdjustments(tempAdjustmentsList, this.getSubTotal(), false, true, true);
    }
    public BigDecimal getDisplayTaxIncluded() {
        BigDecimal taxIncluded  = getDisplaySubTotal().subtract(getSubTotal());
        return taxIncluded.setScale(taxFinalScale, taxRounding);
    }

    public BigDecimal getDisplayRecurringSubTotal() {
        BigDecimal itemsTotal = BigDecimal.ZERO;
        for (ShoppingCartItem cartItem : this.cartLines) {
            itemsTotal = itemsTotal.add(cartItem.getDisplayItemRecurringSubTotal());
        }
        return itemsTotal;
    }

    /** Returns the total from the cart, including tax/shipping. */
    public BigDecimal getDisplayGrandTotal() {
        return this.getDisplaySubTotal().add(this.getTotalShipping()).add(this.getTotalSalesTax()).add(this.getOrderOtherAdjustmentTotal()).add(this.getOrderGlobalAdjustments());
    }

    public BigDecimal getOrderOtherAdjustmentTotal() {
        return OrderReadHelper.calcOrderAdjustments(this.getAdjustments(), this.getSubTotal(), true, false, false);
    }

    /** Returns the sub-total in the cart (item-total - discount). */
    public BigDecimal getSubTotalForPromotions() {
        BigDecimal itemsTotal = BigDecimal.ZERO;
        for (ShoppingCartItem cartItem : this.cartLines) {
            GenericValue product = cartItem.getProduct();
            if (product != null && "N".equals(product.getString("includeInPromotions"))) {
                // don't include in total if this is the case...
                continue;
            }
            itemsTotal = itemsTotal.add(cartItem.getItemSubTotal());
        }
        return itemsTotal.add(this.getOrderOtherAdjustmentTotal());
    }
    public BigDecimal getSubTotalForPromotions(Set<String> productIds) {
        BigDecimal itemsTotal = BigDecimal.ZERO;
        for (ShoppingCartItem cartItem : this.cartLines) {
            GenericValue product = cartItem.getProduct();
            if (product == null || "N".equals(product.getString("includeInPromotions")) || !productIds.contains(cartItem.getProductId())) {
                // don't include in total if this is the case...
                continue;
            }
            itemsTotal = itemsTotal.add(cartItem.getItemSubTotal());
        }
        return itemsTotal;
    }

    /**
     * Get the total payment amount by payment type.  Specify null to get amount
     * over all types.
     */
    public BigDecimal getOrderPaymentPreferenceTotalByType(String paymentMethodTypeId) {
        BigDecimal total = BigDecimal.ZERO;
        String thisPaymentMethodTypeId = null;
        for (CartPaymentInfo payment : paymentInfo) {
            if (payment.amount == null) {
                continue;
            }
            if (payment.paymentMethodId != null) {
                try {
                    // need to determine the payment method type from the payment method
                    GenericValue paymentMethod = this.getDelegator().findOne("PaymentMethod", UtilMisc.toMap("paymentMethodId", payment.paymentMethodId), true);
                    if (paymentMethod != null) {
                        thisPaymentMethodTypeId = paymentMethod.getString("paymentMethodTypeId");
                    }
                } catch (GenericEntityException e) {
                    Debug.logError(e, e.getMessage(), module);
                }
            } else {
                thisPaymentMethodTypeId = payment.paymentMethodTypeId;
            }

            // add the amount according to paymentMethodType
            if (paymentMethodTypeId == null || paymentMethodTypeId.equals(thisPaymentMethodTypeId)) {
                total = total.add(payment.amount);
            }
        }
        return total;
    }

    public BigDecimal getCreditCardPaymentPreferenceTotal() {
        return getOrderPaymentPreferenceTotalByType("CREDIT_CARD");
    }

    public BigDecimal getBillingAccountPaymentPreferenceTotal() {
        return getOrderPaymentPreferenceTotalByType("EXT_BILLACT");
    }

    public BigDecimal getGiftCardPaymentPreferenceTotal() {
        return getOrderPaymentPreferenceTotalByType("GIFT_CARD");
    }

    /** Add a contact mech to this purpose; the contactMechPurposeTypeId is required */
    public void addContactMech(String contactMechPurposeTypeId, String contactMechId) {
        if (contactMechPurposeTypeId == null) {
            throw new IllegalArgumentException("You must specify a contactMechPurposeTypeId to add a ContactMech");
        }
        contactMechIdsMap.put(contactMechPurposeTypeId, contactMechId);
    }

    /** Get the contactMechId for this cart given the contactMechPurposeTypeId */
    public String getContactMech(String contactMechPurposeTypeId) {
        return contactMechIdsMap.get(contactMechPurposeTypeId);
    }

    /** Remove the contactMechId from this cart given the contactMechPurposeTypeId */
    public String removeContactMech(String contactMechPurposeTypeId) {
        return contactMechIdsMap.remove(contactMechPurposeTypeId);
    }

    public Map<String, String> getOrderContactMechIds() {
        return this.contactMechIdsMap;
    }

    /** Get a List of adjustments on the order (ie cart) */
    public List<GenericValue> getAdjustments() {
        return adjustments;
    }

    public int getAdjustmentPromoIndex(String productPromoId) {
        if (UtilValidate.isEmpty(productPromoId)) {
            return -1;
        }
        int index = adjustments.size();
        while (index > 0) {
            index--;
            if (productPromoId.equals(adjustments.get(index).getString("productPromoId"))) {
                return (index);
            }
        }
        return -1;
    }

    /** Add an adjustment to the order; don't worry about setting the orderId, orderItemSeqId or orderAdjustmentId; they will be set when the order is created */
    public int addAdjustment(GenericValue adjustment) {
        adjustments.add(adjustment);
        return adjustments.indexOf(adjustment);
    }

    public void removeAdjustment(int index) {
        adjustments.remove(index);
    }

    public GenericValue getAdjustment(int index) {
        return adjustments.get(index);
    }

    /** Get a List of orderTerms on the order (ie cart) */
    public List<GenericValue> getOrderTerms() {
        return orderTerms;
    }

    /** Add an orderTerm to the order */
    public int addOrderTerm(String termTypeId, BigDecimal termValue, Long termDays) {
        return addOrderTerm(termTypeId, termValue, termDays, null);
    }

    /** Add an orderTerm to the order */
    public int addOrderTerm(String termTypeId, BigDecimal termValue, Long termDays, String textValue) {
        GenericValue orderTerm = this.getDelegator().makeValue("OrderTerm");
        orderTerm.put("termTypeId", termTypeId);
        orderTerm.put("termValue", termValue);
        orderTerm.put("termDays", termDays);
        orderTerm.put("textValue", textValue);
        return addOrderTerm(orderTerm);
    }

    public int addOrderTerm(String termTypeId, String orderItemSeqId,BigDecimal termValue, Long termDays, String textValue, String description) {
        GenericValue orderTerm = this.getDelegator().makeValue("OrderTerm");
        orderTerm.put("termTypeId", termTypeId);
        if (UtilValidate.isEmpty(orderItemSeqId)) {
            orderItemSeqId = "_NA_";
        }
        orderTerm.put("orderItemSeqId", orderItemSeqId);
        orderTerm.put("termValue", termValue);
        orderTerm.put("termDays", termDays);
        orderTerm.put("textValue", textValue);
        orderTerm.put("description", description);
        return addOrderTerm(orderTerm);
    }

    /** Add an orderTerm to the order */
    public int addOrderTerm(GenericValue orderTerm) {
        orderTerms.add(orderTerm);
        return orderTerms.indexOf(orderTerm);
    }

    public void removeOrderTerm(int index) {
        orderTerms.remove(index);
    }

    public void removeOrderTerms() {
        orderTerms.clear();
    }

    public boolean isOrderTermSet() {
       return orderTermSet;
    }

    public void setOrderTermSet(boolean orderTermSet) {
         this.orderTermSet = orderTermSet;
     }

    public boolean hasOrderTerm(String termTypeId) {
        if (termTypeId == null) {
            return false;
        }
        for (GenericValue orderTerm : orderTerms) {
            if (termTypeId.equals(orderTerm.getString("termTypeId"))) {
                return true;
            }
        }
        return false;
    }

    public boolean isReadOnlyCart() {
       return readOnlyCart;
    }

    public void setReadOnlyCart(boolean readOnlyCart) {
         this.readOnlyCart = readOnlyCart;
     }

    /** go through the order adjustments and remove all adjustments with the given type */
    public void removeAdjustmentByType(String orderAdjustmentTypeId) {
        if (orderAdjustmentTypeId == null) {
            return;
        }

        // make a list of adjustment lists including the cart adjustments and the cartItem adjustments for each item
        List<List<GenericValue>> adjsLists = new LinkedList<>();

        adjsLists.add(this.getAdjustments());

        for (ShoppingCartItem item : this) {
            if (item.getAdjustments() != null) {
                adjsLists.add(item.getAdjustments());
            }
        }

        for (List<GenericValue> adjs: adjsLists) {

            if (adjs != null) {
                for (int i = 0; i < adjs.size();) {
                    GenericValue orderAdjustment = adjs.get(i);

                    if (orderAdjustmentTypeId.equals(orderAdjustment.getString("orderAdjustmentTypeId"))) {
                        adjs.remove(i);
                    } else {
                        i++;
                    }
                }
            }
        }
    }

    /** Returns the total weight in the cart. */
    public BigDecimal getTotalWeight() {
        BigDecimal weight = BigDecimal.ZERO;

        for (ShoppingCartItem item : this.cartLines) {
            weight = weight.add(item.getWeight().multiply(item.getQuantity()));
        }
        return weight;
    }

    /** Returns the total quantity in the cart. */
    public BigDecimal getTotalQuantity() {
        BigDecimal count = BigDecimal.ZERO;

        for (ShoppingCartItem item : this.cartLines) {
            count = count.add(item.getQuantity());
        }
        return count;
    }

    /** Returns the SHIPPABLE item-total in the cart for a specific ship group. */
    public BigDecimal getShippableTotal(int idx) {
        CartShipInfo info = this.getShipInfo(idx);
        BigDecimal itemTotal = BigDecimal.ZERO;

        for (ShoppingCartItem item : info.shipItemInfo.keySet()) {
            CartShipInfo.CartShipItemInfo csii = info.shipItemInfo.get(item);
            if (csii != null && csii.quantity.compareTo(BigDecimal.ZERO) > 0) {
                if (item.shippingApplies()) {
                    itemTotal = itemTotal.add(item.getItemSubTotal(csii.quantity));
                }
            }
        }

        return itemTotal;
    }

    /** Returns the total SHIPPABLE quantity in the cart for a specific ship group. */
    public BigDecimal getShippableQuantity(int idx) {
        CartShipInfo info = this.getShipInfo(idx);
        BigDecimal count = BigDecimal.ZERO;

        for (ShoppingCartItem item : info.shipItemInfo.keySet()) {
            CartShipInfo.CartShipItemInfo csii = info.shipItemInfo.get(item);
            if (csii != null && csii.quantity.compareTo(BigDecimal.ZERO) > 0) {
                if (item.shippingApplies()) {
                    count = count.add(csii.quantity);
                }
            }
        }

        return count;
    }

    /** Returns the total SHIPPABLE weight in the cart for a specific ship group. */
    public BigDecimal getShippableWeight(int idx) {
        CartShipInfo info = this.getShipInfo(idx);
        BigDecimal weight = BigDecimal.ZERO;

        for (ShoppingCartItem item : info.shipItemInfo.keySet()) {
            CartShipInfo.CartShipItemInfo csii = info.shipItemInfo.get(item);
            if (csii != null && csii.quantity.compareTo(BigDecimal.ZERO) > 0) {
                if (item.shippingApplies()) {
                    weight = weight.add(item.getWeight().multiply(csii.quantity));
                }
            }
        }

        return weight;
    }

    /** Returns a List of shippable item's size for a specific ship group. */
    public List<BigDecimal> getShippableSizes(int idx) {
        CartShipInfo info = this.getShipInfo(idx);
        List<BigDecimal> shippableSizes = new LinkedList<>();

        for (ShoppingCartItem item : info.shipItemInfo.keySet()) {
            CartShipInfo.CartShipItemInfo csii = info.shipItemInfo.get(item);
            if (csii != null && csii.quantity.compareTo(BigDecimal.ZERO) > 0) {
                if (item.shippingApplies()) {
                    shippableSizes.add(item.getSize());
                }
            }
        }

        return shippableSizes;
    }

    /** Returns a List of shippable item info (quantity, size, weight) for a specific ship group */
    public List<Map<String, Object>> getShippableItemInfo(int idx) {
        CartShipInfo info = this.getShipInfo(idx);
        List<Map<String, Object>> itemInfos = new LinkedList<>();

        for (ShoppingCartItem item : info.shipItemInfo.keySet()) {
            CartShipInfo.CartShipItemInfo csii = info.shipItemInfo.get(item);
            if (csii != null && csii.quantity.compareTo(BigDecimal.ZERO) > 0) {
                if (item.shippingApplies()) {
                    Map<String, Object> itemInfo = item.getItemProductInfo();
                    itemInfo.put("quantity", csii.quantity);
                    itemInfos.add(itemInfo);
                }
            }
        }

        return itemInfos;
    }

    /** Returns true when there are shippable items in the cart */
    public boolean shippingApplies() {
        boolean shippingApplies = false;
        for (ShoppingCartItem item : this) {
            if (item.shippingApplies()) {
                shippingApplies = true;
                break;
            }
        }
        return shippingApplies;
    }

    /** Returns true when there are taxable items in the cart */
    public boolean taxApplies() {
        boolean taxApplies = false;
        for (ShoppingCartItem item : this) {
            if (item.taxApplies()) {
                taxApplies = true;
                break;
            }
        }
        return taxApplies;
    }

    /** Returns a Map of all features applied to products in the cart with quantities for a specific ship group. */
    public Map<String, BigDecimal> getFeatureIdQtyMap(int idx) {
        CartShipInfo info = this.getShipInfo(idx);
        Map<String, BigDecimal> featureMap = new HashMap<>();

        for (ShoppingCartItem item : info.shipItemInfo.keySet()) {
            CartShipInfo.CartShipItemInfo csii = info.shipItemInfo.get(item);
            if (csii != null && csii.quantity.compareTo(BigDecimal.ZERO) > 0) {
                featureMap.putAll(item.getFeatureIdQtyMap(csii.quantity));
            }
        }

        return featureMap;
    }

    /** Returns true if the user wishes to view the cart everytime an item is added. */
    public boolean viewCartOnAdd() {
        return viewCartOnAdd;
    }

    /** Returns true if the user wishes to view the cart everytime an item is added. */
    public void setViewCartOnAdd(boolean viewCartOnAdd) {
        this.viewCartOnAdd = viewCartOnAdd;
    }

    /** Returns the order ID associated with this cart or null if no order has been created yet. */
    public String getOrderId() {
        return this.orderId;
    }

    /** Returns the first attempt order ID associated with this cart or null if no order has been created yet. */
    public String getFirstAttemptOrderId() {
        return this.firstAttemptOrderId;
    }

    /** Sets the orderId associated with this cart. */
    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public void setNextItemSeq(long seq) throws GeneralException {
        if (this.nextItemSeq != 1) {
            throw new GeneralException("Cannot set the item sequence once the sequence has been incremented!");
        }
        this.nextItemSeq = seq;
    }

    /** TODO: Sets the first attempt orderId for this cart. */
    public void setFirstAttemptOrderId(String orderId) {
        this.firstAttemptOrderId = orderId;
    }

    public void removeAllFreeShippingProductPromoActions() {
        this.freeShippingProductPromoActions.clear();
    }
    /** Removes a free shipping ProductPromoAction by trying to find one in the list with the same primary key. */
    public void removeFreeShippingProductPromoAction(GenericPK productPromoActionPK) {
        if (productPromoActionPK == null) {
            return;
        }

        Iterator<GenericValue> fsppas = this.freeShippingProductPromoActions.iterator();
        while (fsppas.hasNext()) {
            if (productPromoActionPK.equals((fsppas.next()).getPrimaryKey())) {
                fsppas.remove();
            }
        }
    }
    /** Adds a ProductPromoAction to be used for free shipping (must be of type free shipping, or nothing will be done). */
    public void addFreeShippingProductPromoAction(GenericValue productPromoAction) {
        if (productPromoAction == null) {
            return;
        }
        // is this a free shipping action?
        if (!"PROMO_FREE_SHIPPING".equals(productPromoAction.getString("productPromoActionEnumId")))
         {
            return; // Changed 1-5-04 by Si Chen
        }

        // to easily make sure that no duplicate exists, do a remove first
        this.removeFreeShippingProductPromoAction(productPromoAction.getPrimaryKey());
        this.freeShippingProductPromoActions.add(productPromoAction);
    }
    public List<GenericValue> getFreeShippingProductPromoActions() {
        return this.freeShippingProductPromoActions;
    }

    public void removeAllDesiredAlternateGiftByActions() {
        this.desiredAlternateGiftByAction.clear();
    }
    public void setDesiredAlternateGiftByAction(GenericPK productPromoActionPK, String productId) {
        this.desiredAlternateGiftByAction.put(productPromoActionPK, productId);
    }
    public String getDesiredAlternateGiftByAction(GenericPK productPromoActionPK) {
        return this.desiredAlternateGiftByAction.get(productPromoActionPK);
    }
    public Map<GenericPK, String> getAllDesiredAlternateGiftByActionCopy() {
        return new HashMap<>(this.desiredAlternateGiftByAction);
    }

    public void addProductPromoUse(String productPromoId, String productPromoCodeId, BigDecimal totalDiscountAmount, BigDecimal quantityLeftInActions, Map<ShoppingCartItem,BigDecimal> usageInfoMap) {
        if (UtilValidate.isNotEmpty(productPromoCodeId) && !this.productPromoCodes.contains(productPromoCodeId)) {
            throw new IllegalStateException("Cannot add a use to a promo code use for a code that has not been entered.");
        }
        if (Debug.verboseOn()) {
            Debug.logVerbose("Used promotion [" + productPromoId + "] with code [" + productPromoCodeId + "] for total discount [" + totalDiscountAmount + "] and quantity left in actions [" + quantityLeftInActions + "]", module);
        }
        this.productPromoUseInfoList.add(new ProductPromoUseInfo(productPromoId, productPromoCodeId, totalDiscountAmount, quantityLeftInActions, usageInfoMap));
    }

    public void removeProductPromoUse(String productPromoId) {
        if (!productPromoId.isEmpty()) {
            int index = -1;
            for (ProductPromoUseInfo productPromoUseInfo : this.productPromoUseInfoList) {
                if (productPromoId.equals(productPromoUseInfo.productPromoId)) {
                    index = this.productPromoUseInfoList.indexOf(productPromoUseInfo);
                    break;
                }
            }
            if (index != -1) {
                this.productPromoUseInfoList.remove(index);
            }
        }
    }

    public void clearProductPromoUseInfo() {
        // clear out info for general promo use
        this.productPromoUseInfoList.clear();
    }

    public void clearCartItemUseInPromoInfo() {
        // clear out info about which cart items have been used in promos
        for (ShoppingCartItem cartLine : this) {
            cartLine.clearPromoRuleUseInfo();
        }
    }

    public Iterator<ProductPromoUseInfo> getProductPromoUseInfoIter() {
        return productPromoUseInfoList.iterator();
    }

    public BigDecimal getProductPromoTotal() {
        BigDecimal totalDiscount = BigDecimal.ZERO;
        List<GenericValue> cartAdjustments = this.getAdjustments();
        if (cartAdjustments != null) {
            for (GenericValue checkOrderAdjustment : cartAdjustments) {
                if (UtilValidate.isNotEmpty(checkOrderAdjustment.getString("productPromoId")) &&
                        UtilValidate.isNotEmpty(checkOrderAdjustment.getString("productPromoRuleId")) &&
                        UtilValidate.isNotEmpty(checkOrderAdjustment.getString("productPromoActionSeqId"))) {
                    if (checkOrderAdjustment.get("amount") != null) {
                        totalDiscount = totalDiscount.add(checkOrderAdjustment.getBigDecimal("amount"));
                    }
                }
            }
        }

        // add cart line adjustments from promo actions
        for (ShoppingCartItem checkItem : this) {
            Iterator<GenericValue> checkOrderAdjustments = UtilMisc.toIterator(checkItem.getAdjustments());
            while (checkOrderAdjustments != null && checkOrderAdjustments.hasNext()) {
                GenericValue checkOrderAdjustment = checkOrderAdjustments.next();
                if (UtilValidate.isNotEmpty(checkOrderAdjustment.getString("productPromoId")) &&
                        UtilValidate.isNotEmpty(checkOrderAdjustment.getString("productPromoRuleId")) &&
                        UtilValidate.isNotEmpty(checkOrderAdjustment.getString("productPromoActionSeqId"))) {
                    if (checkOrderAdjustment.get("amount") != null) {
                        totalDiscount = totalDiscount.add(checkOrderAdjustment.getBigDecimal("amount"));
                    }
                }
            }
        }

        return totalDiscount;
    }

    /** Get total discount for a given ProductPromo, or for ANY ProductPromo if the passed in productPromoId is null. */
    public BigDecimal getProductPromoUseTotalDiscount(String productPromoId) {
        BigDecimal totalDiscount = BigDecimal.ZERO;
        for (ProductPromoUseInfo productPromoUseInfo: this.productPromoUseInfoList) {
            if (productPromoId == null || productPromoId.equals(productPromoUseInfo.productPromoId)) {
                totalDiscount = totalDiscount.add(productPromoUseInfo.getTotalDiscountAmount());
            }
        }
        return totalDiscount;
    }

    public int getProductPromoUseCount(String productPromoId) {
        if (productPromoId == null) {
            return 0;
        }
        int useCount = 0;
        for (ProductPromoUseInfo productPromoUseInfo: this.productPromoUseInfoList) {
            if (productPromoId.equals(productPromoUseInfo.productPromoId)) {
                useCount++;
            }
        }
        return useCount;
    }

    public int getProductPromoCodeUse(String productPromoCodeId) {
        if (productPromoCodeId == null) {
            return 0;
        }
        int useCount = 0;
        for (ProductPromoUseInfo productPromoUseInfo: this.productPromoUseInfoList) {
            if (productPromoCodeId.equals(productPromoUseInfo.productPromoCodeId)) {
                useCount++;
            }
        }
        return useCount;
    }

    public void clearAllPromotionInformation() {
        this.clearAllPromotionAdjustments();

        // remove all free shipping promo actions
        this.removeAllFreeShippingProductPromoActions();

        // clear promo uses & reset promo code uses, and reset info about cart items used for promos (ie qualifiers and benefiters)
        this.clearProductPromoUseInfo();
        this.clearCartItemUseInPromoInfo();
    }

    public void clearAllPromotionAdjustments() {
        // remove cart adjustments from promo actions
        List<GenericValue> cartAdjustments = this.getAdjustments();
        if (cartAdjustments != null) {
            Iterator<GenericValue> cartAdjustmentIter = cartAdjustments.iterator();
            while (cartAdjustmentIter.hasNext()) {
                GenericValue checkOrderAdjustment = cartAdjustmentIter.next();
                if (UtilValidate.isNotEmpty(checkOrderAdjustment.getString("productPromoId")) &&
                        UtilValidate.isNotEmpty(checkOrderAdjustment.getString("productPromoRuleId")) &&
                        UtilValidate.isNotEmpty(checkOrderAdjustment.getString("productPromoActionSeqId"))) {
                    cartAdjustmentIter.remove();
                }
            }
        }

        // remove cart lines that are promos (ie GWPs) and cart line adjustments from promo actions
        Iterator<ShoppingCartItem> cartItemIter = this.iterator();
        while (cartItemIter.hasNext()) {
            ShoppingCartItem checkItem = cartItemIter.next();
            if (checkItem.getIsPromo()) {
                this.clearItemShipInfo(checkItem);
                cartItemIter.remove();
            } else {
                // found a promo item with the productId, see if it has a matching adjustment on it
                Iterator<GenericValue> checkOrderAdjustments = UtilMisc.toIterator(checkItem.getAdjustments());
                while (checkOrderAdjustments != null && checkOrderAdjustments.hasNext()) {
                    GenericValue checkOrderAdjustment = checkOrderAdjustments.next();
                    if (UtilValidate.isNotEmpty(checkOrderAdjustment.getString("productPromoId")) &&
                            UtilValidate.isNotEmpty(checkOrderAdjustment.getString("productPromoRuleId")) &&
                            UtilValidate.isNotEmpty(checkOrderAdjustment.getString("productPromoActionSeqId"))) {
                        checkOrderAdjustments.remove();
                    }
                }
            }
        }
    }

    public void clearAllAdjustments() {
        // remove all the promotion information (including adjustments)
        clearAllPromotionInformation();
        // remove all cart adjustments
        this.adjustments.clear();
        // remove all cart item adjustments
        for (ShoppingCartItem checkItem : this) {
            checkItem.getAdjustments().clear();
        }
    }

    public void clearAllItemStatus() {
        for (ShoppingCartItem item : this) {
            item.setStatusId(null);
        }
    }

    /** Adds a promotion code to the cart, checking if it is valid. If it is valid this will return null, otherwise it will return a message stating why it was not valid
     * @param productPromoCodeId The promotion code to check and add
     * @return String that is null if valid, and added to cart, or an error message of the code was not valid and not added to the cart.
     */
    public String addProductPromoCode(String productPromoCodeId, LocalDispatcher dispatcher) {
        if (this.productPromoCodes.contains(productPromoCodeId)) {
            return UtilProperties.getMessage(resource_error, "productpromoworker.promotion_code_already_been_entered", UtilMisc.toMap("productPromoCodeId", productPromoCodeId), locale);
        }
        if (!this.getDoPromotions()) {
            this.productPromoCodes.add(productPromoCodeId);
            return null;
        }
        // if the promo code requires it make sure the code is valid
        String checkResult = ProductPromoWorker.checkCanUsePromoCode(productPromoCodeId, this.getPartyId(), this.getDelegator(), this, locale);
        if (checkResult == null) {
            this.productPromoCodes.add(productPromoCodeId);
            // new promo code, re-evaluate promos
            ProductPromoWorker.doPromotions(this, dispatcher);
            return null;
        }
        return checkResult;
    }

    public Set<String> getProductPromoCodesEntered() {
        return this.productPromoCodes;
    }

    public synchronized void resetPromoRuleUse(String productPromoId, String productPromoRuleId) {
        for (ShoppingCartItem cartItem : this) {
            cartItem.resetPromoRuleUse(productPromoId, productPromoRuleId);
        }
    }

    public synchronized void confirmPromoRuleUse(String productPromoId, String productPromoRuleId) {
        for (ShoppingCartItem cartItem : this) {
            cartItem.confirmPromoRuleUse(productPromoId, productPromoRuleId);
        }
    }

    /**
     * Associates a party with a role to the order.
     * @param partyId identifier of the party to associate to order
     * @param roleTypeId identifier of the role used in party-order association
     */
    public void addAdditionalPartyRole(String partyId, String roleTypeId) {
        // search if there is an existing entry
        List<String> parties = additionalPartyRole.get(roleTypeId);
        if (parties != null) {
            for (String pi : parties) {
                if (pi.equals(partyId)) {
                    return;
                }
            }
        } else {
            parties = new LinkedList<>();
            additionalPartyRole.put(roleTypeId, parties);
        }

        parties.add(0, partyId);
    }

    /**
     * Removes a previously associated party to the order.
     * @param partyId identifier of the party to associate to order
     * @param roleTypeId identifier of the role used in party-order association
     */
    public void removeAdditionalPartyRole(String partyId, String roleTypeId) {
        List<String> parties = additionalPartyRole.get(roleTypeId);

        if (parties != null) {
            Iterator<String> it = parties.iterator();
            while (it.hasNext()) {
                if ((it.next()).equals(partyId)) {
                    it.remove();

                    if (parties.isEmpty()) {
                        additionalPartyRole.remove(roleTypeId);
                    }
                    return;
                }
            }
        }
    }

    public Map<String, List<String>> getAdditionalPartyRoleMap() {
        return additionalPartyRole;
    }

    // =======================================================================
    // Methods used for order creation
    // =======================================================================

    /**
     * Returns the Id of an AGGREGATED_CONF product having exact configId.
     * If AGGREGATED_CONF product do not exist, creates one, associates it to the AGGREGATED product, and copy its production run template.
     * @param item
     * @param dispatcher
     */
    public String getAggregatedInstanceId (ShoppingCartItem item, LocalDispatcher dispatcher) {
        if (UtilValidate.isEmpty(item.getConfigWrapper()) || UtilValidate.isEmpty(item.getConfigWrapper().getConfigId())) {
            return null;
        }
        String newProductId = null;
        String configId = item.getConfigWrapper().getConfigId();
        try {
            //first search for existing productId
            newProductId = ProductWorker.getAggregatedInstanceId(getDelegator(), item.getProductId(), configId);
            if (newProductId != null) {
                return newProductId;
            }

            Delegator delegator = this.getDelegator();

            //create new product and associate it
            GenericValue product = item.getProduct();
            String productName = product.getString("productName");
            String description = product.getString("description");
            Map<String, Object> serviceContext = new HashMap<>();
            GenericValue permUserLogin = EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "system").queryOne();
            String internalName = item.getProductId() + "_" + configId;
            serviceContext.put("internalName", internalName);
            serviceContext.put("productName", productName);
            serviceContext.put("description", description);
            if(ProductWorker.isAggregateService(delegator, item.getProductId())) {
                serviceContext.put("productTypeId", "AGGREGATEDSERV_CONF");
            }
            else {
                serviceContext.put("productTypeId", "AGGREGATED_CONF");
            }

            serviceContext.put("configId", configId);
            if (UtilValidate.isNotEmpty(product.getString("requirementMethodEnumId"))) {
                serviceContext.put("requirementMethodEnumId", product.getString("requirementMethodEnumId"));
            }
            serviceContext.put("userLogin", permUserLogin);

            Map<String, Object> result = dispatcher.runSync("createProduct", serviceContext);
            if (ServiceUtil.isError(result)) {
                Debug.logError(ServiceUtil.getErrorMessage(result), module);
                return null;
            }

            serviceContext.clear();
            newProductId = (String) result.get("productId");
            serviceContext.put("productId", item.getProductId());
            serviceContext.put("productIdTo", newProductId);
            serviceContext.put("productAssocTypeId", "PRODUCT_CONF");
            serviceContext.put("fromDate", UtilDateTime.nowTimestamp());
            serviceContext.put("userLogin", permUserLogin);

            result = dispatcher.runSync("createProductAssoc", serviceContext);
            if (ServiceUtil.isError(result)) {
                Debug.logError(ServiceUtil.getErrorMessage(result), module);
                return null;
            }

            //create a new WorkEffortGoodStandard based on existing one of AGGREGATED product .
            //Another approach could be to get WorkEffortGoodStandard of the AGGREGATED product while creating production run.
            GenericValue productionRunTemplate = EntityQuery.use(delegator).from("WorkEffortGoodStandard").where("productId", item.getProductId(), "workEffortGoodStdTypeId", "ROU_PROD_TEMPLATE", "statusId", "WEGS_CREATED").filterByDate().queryFirst();
            if (productionRunTemplate != null) {
                serviceContext.clear();
                serviceContext.put("workEffortId", productionRunTemplate.getString("workEffortId"));
                serviceContext.put("productId", newProductId);
                serviceContext.put("workEffortGoodStdTypeId", "ROU_PROD_TEMPLATE");
                serviceContext.put("statusId", "WEGS_CREATED");
                serviceContext.put("userLogin", permUserLogin);

                result = dispatcher.runSync("createWorkEffortGoodStandard", serviceContext);
                if (ServiceUtil.isError(result)) {
                    Debug.logError(ServiceUtil.getErrorMessage(result), module);
                    return null;
                }
            }
        } catch (GenericEntityException | GenericServiceException e) {
            Debug.logError(e, module);
        }
        return newProductId;
    }

    public List<GenericValue> makeOrderItemGroups() {
        List<GenericValue> result = new LinkedList<>();
        for (ShoppingCart.ShoppingCartItemGroup itemGroup : this.itemGroupByNumberMap.values()) {
            result.add(itemGroup.makeOrderItemGroup(this.getDelegator()));
        }
        return result;
    }

    private void explodeItems(LocalDispatcher dispatcher) {
        if (dispatcher == null) {
            return;
        }
        synchronized (cartLines) {
            List<ShoppingCartItem> cartLineItems = new LinkedList<>(cartLines);
            for (ShoppingCartItem item : cartLineItems) {
                try {
                    int thisIndex = items().indexOf(item);
                    List<ShoppingCartItem> explodedItems = item.explodeItem(this, dispatcher);

                    // Add exploded items into cart with order item sequence id and item ship group quantity
                    for (ShoppingCartItem explodedItem : explodedItems) {
                        String orderItemSeqId = UtilFormatOut.formatPaddedNumber(nextItemSeq, 5);
                        explodedItem.setOrderItemSeqId(orderItemSeqId);
                        addItemToEnd(explodedItem);
                        setItemShipGroupQty(explodedItem, BigDecimal.ONE, thisIndex);
                        nextItemSeq++;
                    }
                } catch (CartItemModifyException e) {
                    Debug.logError(e, "Problem exploding item! Item not exploded.", module);
                }
            }
        }
    }

    /**
     * Does an "explode", or "unitize" operation on a list of cart items.
     * Resulting state for each item with quantity X is X items of quantity 1.
     *
     * @param shoppingCartItems
     * @param dispatcher
     */
    public void explodeItems(List<ShoppingCartItem> shoppingCartItems, LocalDispatcher dispatcher) {
        if (dispatcher == null) {
            return;
        }
        synchronized (cartLines) {
            for (ShoppingCartItem item : shoppingCartItems) {
                try {
                    int thisIndex = items().indexOf(item);
                    List<ShoppingCartItem> explodedItems = item.explodeItem(this, dispatcher);

                    // Add exploded items into cart with order item sequence id and item ship group quantity
                    for (ShoppingCartItem explodedItem : explodedItems) {
                        String orderItemSeqId = UtilFormatOut.formatPaddedNumber(nextItemSeq, 5);
                        explodedItem.setOrderItemSeqId(orderItemSeqId);
                        addItemToEnd(explodedItem);
                        setItemShipGroupQty(explodedItem, BigDecimal.ONE, thisIndex);
                        nextItemSeq++;
                    }
                } catch (CartItemModifyException e) {
                    Debug.logError(e, "Problem exploding (unitizing) item! Item not exploded.", module);
                }
            }
        }
    }

    public List<GenericValue> makeOrderItems(LocalDispatcher dispatcher) {
        return makeOrderItems(false, false, dispatcher);
    }

    public List<GenericValue> makeOrderItems(boolean explodeItems, boolean replaceAggregatedId, LocalDispatcher dispatcher) {
        // do the explosion
        if (explodeItems && dispatcher != null) {
            explodeItems(dispatcher);
        }

        // now build the lines
        synchronized (cartLines) {
            List<GenericValue> result = new LinkedList<>();

            for (ShoppingCartItem item : cartLines) {
                if (UtilValidate.isEmpty(item.getOrderItemSeqId())) {
                    String orderItemSeqId = UtilFormatOut.formatPaddedNumber(nextItemSeq, 5);
                    item.setOrderItemSeqId(orderItemSeqId);
                    nextItemSeq++;
                } else {
                    try {
                        int thisSeqId = Integer.parseInt(item.getOrderItemSeqId());
                        if (thisSeqId > nextItemSeq) {
                            nextItemSeq = thisSeqId + 1;
                        }
                    } catch (NumberFormatException e) {
                        Debug.logError(e, module);
                    }
                }

                // the initial status for all item types
                String initialStatus = "ITEM_CREATED";
                String status = item.getStatusId();
                if (status == null) {
                    status = initialStatus;
                }
                //check for aggregated products
                String aggregatedInstanceId = null;
                if (replaceAggregatedId && UtilValidate.isNotEmpty(item.getConfigWrapper())) {
                    aggregatedInstanceId = getAggregatedInstanceId(item, dispatcher);
                }

                GenericValue orderItem = getDelegator().makeValue("OrderItem");
                orderItem.set("orderItemSeqId", item.getOrderItemSeqId());
                orderItem.set("externalId", item.getExternalId());
                orderItem.set("orderItemTypeId", item.getItemType());
                if (item.getItemGroup() != null) {
                    orderItem.set("orderItemGroupSeqId", item.getItemGroup().getGroupNumber());
                }
                orderItem.set("productId", UtilValidate.isNotEmpty(aggregatedInstanceId) ? aggregatedInstanceId : item.getProductId());
                orderItem.set("supplierProductId", item.getSupplierProductId());
                orderItem.set("prodCatalogId", item.getProdCatalogId());
                orderItem.set("productCategoryId", item.getProductCategoryId());
                orderItem.set("quantity", item.getQuantity());
                orderItem.set("selectedAmount", item.getSelectedAmount());
                orderItem.set("unitPrice", item.getBasePrice());
                orderItem.set("unitListPrice", item.getListPrice());
                orderItem.set("isModifiedPrice",item.getIsModifiedPrice() ? "Y" : "N");
                orderItem.set("isPromo", item.getIsPromo() ? "Y" : "N");

                orderItem.set("shoppingListId", item.getShoppingListId());
                orderItem.set("shoppingListItemSeqId", item.getShoppingListItemSeqId());

                orderItem.set("itemDescription", item.getName(dispatcher));
                orderItem.set("comments", item.getItemComment());
                orderItem.set("estimatedDeliveryDate", item.getDesiredDeliveryDate());
                orderItem.set("correspondingPoId", this.getPoNumber());
                orderItem.set("quoteId", item.getQuoteId());
                orderItem.set("quoteItemSeqId", item.getQuoteItemSeqId());
                orderItem.set("statusId", status);

                orderItem.set("shipBeforeDate", item.getShipBeforeDate());
                orderItem.set("shipAfterDate", item.getShipAfterDate());
                orderItem.set("reserveAfterDate", item.getReserveAfterDate());
                orderItem.set("estimatedShipDate", item.getEstimatedShipDate());
                orderItem.set("cancelBackOrderDate", item.getCancelBackOrderDate());
                if (this.getUserLogin() != null) {
                    orderItem.set("changeByUserLoginId", this.getUserLogin().get("userLoginId"));
                }

                String fromInventoryItemId = (String) item.getAttribute("fromInventoryItemId");
                if (fromInventoryItemId != null) {
                    orderItem.set("fromInventoryItemId", fromInventoryItemId);
                }

                result.add(orderItem);
                // don't do anything with adjustments here, those will be added below in makeAllAdjustments
            }
            return result;
        }
    }

    /** create WorkEfforts from the shoppingcart items when itemType = RENTAL_ORDER_ITEM */
    public List<GenericValue> makeWorkEfforts() {
        List<GenericValue> allWorkEfforts = new LinkedList<>();
        for (ShoppingCartItem item : cartLines) {
            if ("RENTAL_ORDER_ITEM".equals(item.getItemType())) {         // prepare workeffort when the order item is a rental item
                GenericValue workEffort = getDelegator().makeValue("WorkEffort");
                workEffort.set("workEffortId",item.getOrderItemSeqId());  // fill temporary with sequence number
                workEffort.set("estimatedStartDate",item.getReservStart());
                workEffort.set("estimatedCompletionDate",item.getReservStart(item.getReservLength()));
                workEffort.set("reservPersons", item.getReservPersons());
                workEffort.set("reserv2ndPPPerc", item.getReserv2ndPPPerc());
                workEffort.set("reservNthPPPerc", item.getReservNthPPPerc());
                workEffort.set("accommodationMapId", item.getAccommodationMapId());
                workEffort.set("accommodationSpotId",item.getAccommodationSpotId());
                allWorkEfforts.add(workEffort);
            }
        }
        return allWorkEfforts;
    }

    /** make a list of all adjustments including order adjustments, order line adjustments, and special adjustments (shipping and tax if applicable) */
    public List<GenericValue> makeAllAdjustments() {
        List<GenericValue> allAdjs = new LinkedList<>();

        // before returning adjustments, go through them to find all that need counter adjustments (for instance: free shipping)
        for (GenericValue orderAdjustment: this.getAdjustments()) {

            allAdjs.add(orderAdjustment);

            if ("SHIPPING_CHARGES".equals(orderAdjustment.get("orderAdjustmentTypeId"))) {
                Iterator<GenericValue> fsppas = this.freeShippingProductPromoActions.iterator();

                while (fsppas.hasNext()) {
                    // TODO - we need to change the way free shipping promotions work
                }
            }
        }

        // add all of the item adjustments to this list too
        for (ShoppingCartItem item : cartLines) {
            Collection<GenericValue> adjs = item.getAdjustments();

            if (adjs != null) {
                for (GenericValue orderAdjustment: adjs) {

                    orderAdjustment.set("orderItemSeqId", item.getOrderItemSeqId());
                    allAdjs.add(orderAdjustment);

                    if ("SHIPPING_CHARGES".equals(orderAdjustment.get("orderAdjustmentTypeId"))) {
                        Iterator<GenericValue> fsppas = this.freeShippingProductPromoActions.iterator();

                        while (fsppas.hasNext()) {
                            // TODO - fix the free shipping promotions!!
                        }
                    }
                }
            }
        }

        return allAdjs;
    }

    /** make a list of all quote adjustments including header adjustments, line adjustments, and special adjustments (shipping and tax if applicable).
     *  Internally, the quote adjustments are created from the order adjustments.
     */
    public List<GenericValue> makeAllQuoteAdjustments() {
        List<GenericValue> quoteAdjs = new LinkedList<>();

        for (GenericValue orderAdj: makeAllAdjustments()) {
            GenericValue quoteAdj = this.getDelegator().makeValue("QuoteAdjustment");
            quoteAdj.put("quoteAdjustmentId", orderAdj.get("orderAdjustmentId"));
            quoteAdj.put("quoteAdjustmentTypeId", orderAdj.get("orderAdjustmentTypeId"));
            quoteAdj.put("quoteItemSeqId", orderAdj.get("orderItemSeqId"));
            quoteAdj.put("comments", orderAdj.get("comments"));
            quoteAdj.put("description", orderAdj.get("description"));
            quoteAdj.put("amount", orderAdj.get("amount"));
            quoteAdj.put("productPromoId", orderAdj.get("productPromoId"));
            quoteAdj.put("productPromoRuleId", orderAdj.get("productPromoRuleId"));
            quoteAdj.put("productPromoActionSeqId", orderAdj.get("productPromoActionSeqId"));
            quoteAdj.put("productFeatureId", orderAdj.get("productFeatureId"));
            quoteAdj.put("correspondingProductId", orderAdj.get("correspondingProductId"));
            quoteAdj.put("sourceReferenceId", orderAdj.get("sourceReferenceId"));
            quoteAdj.put("sourcePercentage", orderAdj.get("sourcePercentage"));
            quoteAdj.put("customerReferenceId", orderAdj.get("customerReferenceId"));
            quoteAdj.put("primaryGeoId", orderAdj.get("primaryGeoId"));
            quoteAdj.put("secondaryGeoId", orderAdj.get("secondaryGeoId"));
            quoteAdj.put("exemptAmount", orderAdj.get("exemptAmount"));
            quoteAdj.put("taxAuthGeoId", orderAdj.get("taxAuthGeoId"));
            quoteAdj.put("taxAuthPartyId", orderAdj.get("taxAuthPartyId"));
            quoteAdj.put("overrideGlAccountId", orderAdj.get("overrideGlAccountId"));
            quoteAdj.put("includeInTax", orderAdj.get("includeInTax"));
            quoteAdj.put("includeInShipping", orderAdj.get("includeInShipping"));
            quoteAdj.put("createdDate", orderAdj.get("createdDate"));
            quoteAdj.put("createdByUserLogin", orderAdj.get("createdByUserLogin"));
            quoteAdjs.add(quoteAdj);
        }

        return quoteAdjs;
    }

    /** make a list of all OrderPaymentPreferences and Billing info including all payment methods and types */
    public List<GenericValue> makeAllOrderPaymentInfos(LocalDispatcher dispatcher) {
        Delegator delegator = this.getDelegator();
        List<GenericValue> allOpPrefs = new LinkedList<>();
        BigDecimal remainingAmount = this.getGrandTotal().subtract(this.getPaymentTotal());
        remainingAmount = remainingAmount.setScale(2, RoundingMode.HALF_UP);
        if (getBillingAccountId() != null && this.billingAccountAmt.compareTo(BigDecimal.ZERO) <= 0) {
            BigDecimal billingAccountAvailableAmount = CheckOutHelper.availableAccountBalance(getBillingAccountId(), dispatcher);
            if (this.billingAccountAmt.compareTo(BigDecimal.ZERO) == 0 && billingAccountAvailableAmount.compareTo(BigDecimal.ZERO) > 0) {
                this.billingAccountAmt = billingAccountAvailableAmount;
            }
            if (remainingAmount.compareTo(getBillingAccountAmount()) < 0) {
                this.billingAccountAmt = remainingAmount;
            }
            if (billingAccountAvailableAmount.compareTo(getBillingAccountAmount()) < 0) {
                this.billingAccountAmt = billingAccountAvailableAmount;
            }
        }
        for (CartPaymentInfo inf : paymentInfo) {
            if (inf.amount == null) {
                inf.amount = remainingAmount;
                remainingAmount = BigDecimal.ZERO;
            }
            allOpPrefs.addAll(inf.makeOrderPaymentInfos(delegator, this));
        }
        return allOpPrefs;
    }

    /** make a list of OrderItemPriceInfos from the ShoppingCartItems */
    public List<GenericValue> makeAllOrderItemPriceInfos() {
        List<GenericValue> allInfos = new LinkedList<>();

        // add all of the item adjustments to this list too
        for (ShoppingCartItem item : cartLines) {
            Collection<GenericValue> infos = item.getOrderItemPriceInfos();

            if (infos != null) {
                for (GenericValue orderItemPriceInfo : infos) {
                    orderItemPriceInfo.set("orderItemSeqId", item.getOrderItemSeqId());
                    allInfos.add(orderItemPriceInfo);
                }
            }
        }

        return allInfos;
    }

    public List<GenericValue> makeProductPromoUses() {
        List<GenericValue> productPromoUses = new LinkedList<>();
        String partyId = this.getPartyId();
        int sequenceValue = 0;
        for (ProductPromoUseInfo productPromoUseInfo: this.productPromoUseInfoList) {
            GenericValue productPromoUse = this.getDelegator().makeValue("ProductPromoUse");
            productPromoUse.set("promoSequenceId", UtilFormatOut.formatPaddedNumber(sequenceValue, 5));
            productPromoUse.set("productPromoId", productPromoUseInfo.getProductPromoId());
            productPromoUse.set("productPromoCodeId", productPromoUseInfo.getProductPromoCodeId());
            productPromoUse.set("totalDiscountAmount", productPromoUseInfo.getTotalDiscountAmount());
            productPromoUse.set("quantityLeftInActions", productPromoUseInfo.getQuantityLeftInActions());
            productPromoUse.set("partyId", partyId);
            productPromoUses.add(productPromoUse);
            sequenceValue++;
        }
        return productPromoUses;
    }

    /** make a list of SurveyResponse object to update with order information set */
    public List<GenericValue> makeAllOrderItemSurveyResponses() {
        List<GenericValue> allInfos = new LinkedList<>();
        for (ShoppingCartItem item : this) {
            List<String> responses = UtilGenerics.checkList(item.getAttribute("surveyResponses"));
            if (responses != null) {
                try {
                    List<GenericValue> surveyResponses = EntityQuery.use(getDelegator()).from("SurveyResponse").where(EntityCondition.makeCondition("surveyResponseId", EntityOperator.IN, responses)).queryList();
                    if (surveyResponses != null) {
                        for (GenericValue surveyResponse : surveyResponses) {
                            surveyResponse.set("orderItemSeqId", item.getOrderItemSeqId());
                            allInfos.add(surveyResponse);
                        }
                    }
                } catch (GenericEntityException e) {
                    Debug.logError(e, "Unable to obtain SurveyResponse record", module);
                }
            }
        }
        return allInfos;
    }

    /** make a list of OrderContactMechs from the ShoppingCart and the ShoppingCartItems */
    public List<GenericValue> makeAllOrderContactMechs() {
        List<GenericValue> allOrderContactMechs = new LinkedList<>();

        Map<String, String> contactMechIds = this.getOrderContactMechIds();

        if (contactMechIds != null) {
            for (Map.Entry<String, String> entry : contactMechIds.entrySet()) {
                GenericValue orderContactMech = getDelegator().makeValue("OrderContactMech");
                orderContactMech.set("contactMechPurposeTypeId", entry.getKey());
                orderContactMech.set("contactMechId", entry.getValue());
                allOrderContactMechs.add(orderContactMech);
            }
        }

        return allOrderContactMechs;
    }

    /** make a list of OrderContactMechs from the ShoppingCart and the ShoppingCartItems */
    public List<GenericValue> makeAllOrderItemContactMechs() {
        List<GenericValue> allOrderContactMechs = new LinkedList<>();

        for (ShoppingCartItem item : cartLines) {
            Map<String, String> itemContactMechIds = item.getOrderItemContactMechIds();

            if (itemContactMechIds != null) {
                for (Map.Entry<String, String> entry: itemContactMechIds.entrySet()) {
                    GenericValue orderContactMech = getDelegator().makeValue("OrderItemContactMech");

                    orderContactMech.set("contactMechPurposeTypeId", entry.getKey());
                    orderContactMech.set("contactMechId", entry.getValue());
                    orderContactMech.set("orderItemSeqId", item.getOrderItemSeqId());
                    allOrderContactMechs.add(orderContactMech);
                }
            }
        }

        return allOrderContactMechs;
    }

    /**
     * Return all OrderItemShipGroup, OrderContactMech, OrderAdjustment and OrderItemShipGroupAssoc from ShoppingCart
     * in a single list of {@link GenericValue}
     * @param dispatcher
     * @return
     */
    public List<GenericValue> makeAllShipGroupInfos(LocalDispatcher dispatcher) {
        List<GenericValue> groups = new LinkedList<>();
        long seqId = 1;
        for (CartShipInfo csi : this.shipInfo) {
            String shipGroupSeqId = csi.shipGroupSeqId;
            if (shipGroupSeqId != null) {
                groups.addAll(csi.makeItemShipGroupAndAssoc(dispatcher, this.getDelegator(), this, shipGroupSeqId));
            } else {
                groups.addAll(csi.makeItemShipGroupAndAssoc(dispatcher, this.getDelegator(), this, UtilFormatOut.formatPaddedNumber(seqId, 5)));
            }
            seqId++;
        }
        return groups;
    }

    public int getShipInfoSize() {
        return this.shipInfo.size();
    }

    public List<GenericValue> makeAllOrderItemAttributes() {
        return makeAllOrderItemAttributes(null, ALL);
    }

    public List<GenericValue> makeAllOrderItemAttributes(String orderId, int mode) {

        // now build order item attributes
        synchronized (cartLines) {
            List<GenericValue> result = new LinkedList<>();

            for (ShoppingCartItem item : cartLines) {
                Map<String, String> orderItemAttributes = item.getOrderItemAttributes();
                for (Entry<String, String> entry : orderItemAttributes.entrySet()) {
                    String value = entry.getValue();
                    String key = entry.getKey();

                    if (ALL == mode || (FILLED_ONLY == mode && UtilValidate.isNotEmpty(value)) || (EMPTY_ONLY == mode && UtilValidate.isEmpty(value))
                            || (mode != FILLED_ONLY && mode != EMPTY_ONLY)) {

                        GenericValue orderItemAttribute = getDelegator().makeValue("OrderItemAttribute");
                        if (UtilValidate.isNotEmpty(orderId)) {
                            orderItemAttribute.set("orderId", orderId);
                        }
                        orderItemAttribute.set("orderItemSeqId", item.getOrderItemSeqId());
                        orderItemAttribute.set("attrName", key);
                        orderItemAttribute.set("attrValue", value);

                        result.add(orderItemAttribute);
                    }
                }
            }
            return result;
        }
    }

    public List<GenericValue> makeAllOrderAttributes() {

        return makeAllOrderAttributes(null, ALL);
    }

    public List<GenericValue> makeAllOrderAttributes(String orderId, int mode) {

        List<GenericValue> allOrderAttributes = new LinkedList<>();

        for (Map.Entry<String, String> entry: orderAttributes.entrySet()) {
            GenericValue orderAtt = this.getDelegator().makeValue("OrderAttribute");
            if (UtilValidate.isNotEmpty(orderId)) {
                orderAtt.set("orderId", orderId);
            }
            String key = entry.getKey();
            String value = entry.getValue();

            orderAtt.put("attrName", key);
            orderAtt.put("attrValue", value);

            switch (mode) {
            case FILLED_ONLY:
                if (UtilValidate.isNotEmpty(value)) {
                    allOrderAttributes.add(orderAtt);
                }
                break;
            case EMPTY_ONLY:
                if (UtilValidate.isEmpty(value)) {
                    allOrderAttributes.add(orderAtt);
                }
                break;
            case ALL:
            default:
                allOrderAttributes.add(orderAtt);
                break;
            }

        }
        return allOrderAttributes;
    }

    public List<GenericValue> makeAllOrderItemAssociations() {
        List<GenericValue> allOrderItemAssociations = new LinkedList<>();

        for (CartShipInfo csi : shipInfo) {
            Set<ShoppingCartItem> items = csi.getShipItems();
            for (ShoppingCartItem item : items) {
                String requirementId = item.getRequirementId();
                if (requirementId != null) {
                    try {
                        // TODO: multiple commitments for the same requirement are still not supported
                        GenericValue commitment = EntityQuery.use(getDelegator())
                                                         .from("OrderRequirementCommitment")
                                                         .where("requirementId", requirementId)
                                                         .queryFirst();
                        if (commitment != null) {
                            GenericValue orderItemAssociation = getDelegator().makeValue("OrderItemAssoc");
                            orderItemAssociation.set("orderId", commitment.getString("orderId"));
                            orderItemAssociation.set("orderItemSeqId", commitment.getString("orderItemSeqId"));
                            orderItemAssociation.set("shipGroupSeqId", "_NA_");
                            orderItemAssociation.set("toOrderId", this.getOrderId());
                            orderItemAssociation.set("toOrderItemSeqId", item.getOrderItemSeqId());
                            orderItemAssociation.set("toShipGroupSeqId", "_NA_");
                            orderItemAssociation.set("orderItemAssocTypeId", "PURCHASE_ORDER");
                            allOrderItemAssociations.add(orderItemAssociation);
                        }
                    } catch (GenericEntityException e) {
                        Debug.logError(e, "Unable to load OrderRequirementCommitment records for requirement ID : " + requirementId, module);
                    }
                }
                if (item.getAssociatedOrderId() != null && item.getAssociatedOrderItemSeqId() != null) {
                    GenericValue orderItemAssociation = getDelegator().makeValue("OrderItemAssoc");
                    orderItemAssociation.set("orderId", item.getAssociatedOrderId());
                    orderItemAssociation.set("orderItemSeqId", item.getAssociatedOrderItemSeqId());
                    orderItemAssociation.set("shipGroupSeqId", csi.getAssociatedShipGroupSeqId() != null ? csi.getAssociatedShipGroupSeqId() : "_NA_");
                    orderItemAssociation.set("toOrderId", this.getOrderId());
                    orderItemAssociation.set("toOrderItemSeqId", item.getOrderItemSeqId());
                    orderItemAssociation.set("toShipGroupSeqId", csi.getShipGroupSeqId() != null ? csi.getShipGroupSeqId() : "_NA_");
                    orderItemAssociation.set("orderItemAssocTypeId", item.getOrderItemAssocTypeId());
                    allOrderItemAssociations.add(orderItemAssociation);
                }
            }
        }
        return allOrderItemAssociations;
    }

    /** Returns a Map of cart values to pass to the storeOrder service */
    public Map<String, Object> makeCartMap(LocalDispatcher dispatcher, boolean explodeItems) {
        Map<String, Object> result = new HashMap<>();

        result.put("orderTypeId", this.getOrderType());
        result.put("orderName", this.getOrderName());
        result.put("externalId", this.getExternalId());
        result.put("orderDate", this.getOrderDate());
        result.put("internalCode", this.getInternalCode());
        result.put("salesChannelEnumId", this.getChannelType());
        result.put("orderItemGroups", this.makeOrderItemGroups());
        result.put("orderItems", this.makeOrderItems(explodeItems, Boolean.TRUE, dispatcher));
        result.put("workEfforts", this.makeWorkEfforts());
        result.put("orderAdjustments", this.makeAllAdjustments());
        result.put("orderTerms", this.getOrderTerms());
        result.put("orderItemPriceInfos", this.makeAllOrderItemPriceInfos());
        result.put("orderProductPromoUses", this.makeProductPromoUses());
        result.put("orderProductPromoCodes", this.getProductPromoCodesEntered());

        result.put("orderAttributes", this.makeAllOrderAttributes());
        result.put("orderItemAttributes", this.makeAllOrderItemAttributes());
        result.put("orderContactMechs", this.makeAllOrderContactMechs());
        result.put("orderItemContactMechs", this.makeAllOrderItemContactMechs());
        result.put("orderPaymentInfo", this.makeAllOrderPaymentInfos(dispatcher));
        result.put("orderItemShipGroupInfo", this.makeAllShipGroupInfos(dispatcher));
        result.put("orderItemSurveyResponses", this.makeAllOrderItemSurveyResponses());
        result.put("orderAdditionalPartyRoleMap", this.getAdditionalPartyRoleMap());
        result.put("orderItemAssociations", this.makeAllOrderItemAssociations());
        result.put("orderInternalNotes", this.getInternalOrderNotes());
        result.put("orderNotes", this.getOrderNotes());

        result.put("firstAttemptOrderId", this.getFirstAttemptOrderId());
        result.put("currencyUom", this.getCurrency());
        result.put("billingAccountId", this.getBillingAccountId());

        result.put("partyId", this.getPartyId());
        result.put("productStoreId", this.getProductStoreId());
        result.put("transactionId", this.getTransactionId());
        result.put("originFacilityId", this.getFacilityId());
        result.put("terminalId", this.getTerminalId());
        result.put("workEffortId", this.getWorkEffortId());
        result.put("autoOrderShoppingListId", this.getAutoOrderShoppingListId());

        result.put("billToCustomerPartyId", this.getBillToCustomerPartyId());
        result.put("billFromVendorPartyId", this.getBillFromVendorPartyId());

        if (this.isSalesOrder()) {
            result.put("placingCustomerPartyId", this.getPlacingCustomerPartyId());
            result.put("shipToCustomerPartyId", this.getShipToCustomerPartyId());
            result.put("endUserCustomerPartyId", this.getEndUserCustomerPartyId());
        }

        if (this.isPurchaseOrder()) {
            result.put("shipFromVendorPartyId", this.getShipFromVendorPartyId());
            result.put("supplierAgentPartyId", this.getSupplierAgentPartyId());
        }

        return result;
    }

    public List<ShoppingCartItem> getLineListOrderedByBasePrice(boolean ascending) {
        List<ShoppingCartItem> result = new ArrayList<>(this.cartLines);
        Collections.sort(result, new BasePriceOrderComparator(ascending));
        return result;
    }

    public TreeMap<Integer, CartShipInfo> getShipGroupsBySupplier(String supplierPartyId) {
        TreeMap<Integer, CartShipInfo> shipGroups = new TreeMap<>();
        for (int i = 0; i < this.shipInfo.size(); i++) {
            CartShipInfo csi = shipInfo.get(i);
            if ((csi.supplierPartyId == null && supplierPartyId == null) ||
                (UtilValidate.isNotEmpty(csi.supplierPartyId) && csi.supplierPartyId.equals(supplierPartyId))) {
                    shipGroups.put(i, csi);
            }
        }
        return shipGroups;
    }

    /**
     * Examine each item of each ship group and create new ship groups if the item should be drop shipped
     * @param dispatcher
     * @throws CartItemModifyException
     */
    public void createDropShipGroups(LocalDispatcher dispatcher) throws CartItemModifyException {

        // Retrieve the facilityId from the cart's productStoreId because ShoppingCart.setFacilityId() doesn't seem to be used anywhere
        String facilityId = null;
        String requirementMethodEnumId = null;
        if (UtilValidate.isNotEmpty(this.getProductStoreId())) {
            try {
                GenericValue productStore = this.getDelegator().findOne("ProductStore", UtilMisc.toMap("productStoreId", this.getProductStoreId()), true);
                facilityId = productStore.getString("inventoryFacilityId");
                requirementMethodEnumId = productStore.getString("requirementMethodEnumId");
            } catch (GenericEntityException gee) {
                Debug.logError(UtilProperties.getMessage(resource_error,"OrderProblemGettingProductStoreRecords", locale) + gee.getMessage(), module);
                return;
            }
        }

        List<CartShipInfo> shipGroups = getShipGroups();
        if (shipGroups == null) {
            return;
        }

        // Intermediate structure supplierPartyId -> { ShoppingCartItem = { originalShipGroupIndex = dropShipQuantity } } to collect drop-shippable items
        Map<String, Map<ShoppingCartItem, Map<Integer, BigDecimal>>> dropShipItems = new HashMap<>();

        for (int shipGroupIndex = 0; shipGroupIndex < shipGroups.size(); shipGroupIndex++) {

            CartShipInfo shipInfo = shipGroups.get(shipGroupIndex);

            // Ignore ship groups that are already drop shipped
            String shipGroupSupplierPartyId = shipInfo.getSupplierPartyId();
            if (UtilValidate.isNotEmpty(shipGroupSupplierPartyId)) {
                continue;
            }

            // Ignore empty ship groups
            Set<ShoppingCartItem> shipItems = shipInfo.getShipItems();
            if (UtilValidate.isEmpty(shipItems)) {
                continue;
            }

            for (ShoppingCartItem cartItem : shipItems) {
                BigDecimal itemQuantity = cartItem.getQuantity();
                BigDecimal dropShipQuantity = BigDecimal.ZERO;

                GenericValue product = cartItem.getProduct();
                if (product == null) {
                    continue;
                }
                String productId = product.getString("productId");

                GenericValue productFacility = null;
                try {
                    productFacility = EntityQuery.use(delegator).from("ProductFacility").where("productId", productId, "facilityId", shipInfo.getFacilityId()).queryOne();
                } catch(GenericEntityException e) {
                    Debug.logError("Error :" +e.getMessage(), module);
                    e.printStackTrace();
                }
                
                if (productFacility != null && UtilValidate.isNotEmpty(productFacility.getString("requirementMethodEnumId"))){
                    requirementMethodEnumId = productFacility.getString("requirementMethodEnumId");
                }

                if (UtilValidate.isNotEmpty(product.getString("requirementMethodEnumId"))) {
                    requirementMethodEnumId = product.getString("requirementMethodEnumId");
                }

                if ("PRODRQM_DS".equals(requirementMethodEnumId)) {

                    // Drop ship the full quantity if the product is marked drop-ship only
                    dropShipQuantity = itemQuantity;

                } else if ("PRODRQM_DSATP".equals(requirementMethodEnumId)) {

                    // Drop ship the quantity not available in inventory if the product is marked drop-ship on low inventory
                    try {

                        // Get ATP for the product
                        Map<String, Object> getProductInventoryAvailableResult = dispatcher.runSync("getInventoryAvailableByFacility", UtilMisc.toMap("productId", productId, "facilityId", facilityId));
                        if (ServiceUtil.isError(getProductInventoryAvailableResult)) {
                            String errorMessage = ServiceUtil.getErrorMessage(getProductInventoryAvailableResult);
                            Debug.logError(errorMessage, module);
                            return;
                        }
                        BigDecimal availableToPromise = (BigDecimal) getProductInventoryAvailableResult.get("availableToPromiseTotal");

                        if (itemQuantity.compareTo(availableToPromise) <= 0) {
                            dropShipQuantity = BigDecimal.ZERO;
                        } else {
                            dropShipQuantity = itemQuantity.subtract(availableToPromise);
                        }

                    } catch (GenericServiceException gee) {
                        Debug.logWarning(UtilProperties.getMessage(resource_error,"OrderRunServiceGetInventoryAvailableByFacilityError", locale) + gee.getMessage(), module);
                    }
                } else {

                    // Don't drop ship anything if the product isn't so marked
                    dropShipQuantity = BigDecimal.ZERO;
                }

                if (dropShipQuantity.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }

                // Find a supplier for the product
                String supplierPartyId = null;
                try {
                    Map<String, Object> getSuppliersForProductResult = dispatcher.runSync("getSuppliersForProduct", UtilMisc.<String, Object>toMap("productId", productId, "quantity", dropShipQuantity, "canDropShip", "Y", "currencyUomId", getCurrency()));
                    if (ServiceUtil.isError(getSuppliersForProductResult)) {
                        String errorMessage = ServiceUtil.getErrorMessage(getSuppliersForProductResult);
                        Debug.logError(errorMessage, module);
                        return;
                    }
                    List<GenericValue> supplierProducts = UtilGenerics.checkList(getSuppliersForProductResult.get("supplierProducts"));

                    // Order suppliers by supplierPrefOrderId so that preferred suppliers are used first
                    supplierProducts = EntityUtil.orderBy(supplierProducts, UtilMisc.toList("supplierPrefOrderId"));
                    GenericValue supplierProduct = EntityUtil.getFirst(supplierProducts);
                    if (! UtilValidate.isEmpty(supplierProduct)) {
                        supplierPartyId = supplierProduct.getString("partyId");
                    }
                } catch (GenericServiceException e) {
                    Debug.logWarning(UtilProperties.getMessage(resource_error,"OrderRunServiceGetSuppliersForProductError", locale) + e.getMessage(), module);
                }

                // Leave the items untouched if we couldn't find a supplier
                if (UtilValidate.isEmpty(supplierPartyId)) {
                    continue;
                }

                if (! dropShipItems.containsKey(supplierPartyId)) {
                    dropShipItems.put(supplierPartyId, new HashMap<ShoppingCartItem, Map<Integer, BigDecimal>>());
                }
                Map<ShoppingCartItem, Map<Integer, BigDecimal>> supplierCartItems = UtilGenerics.checkMap(dropShipItems.get(supplierPartyId));

                if (! supplierCartItems.containsKey(cartItem)) {
                    supplierCartItems.put(cartItem, new HashMap<Integer, BigDecimal>());
                }
                Map<Integer, BigDecimal> cartItemGroupQuantities = UtilGenerics.checkMap(supplierCartItems.get(cartItem));

                cartItemGroupQuantities.put(shipGroupIndex, dropShipQuantity);
            }
        }

        // Reassign the drop-shippable item quantities to new or existing drop-ship groups
        for (Entry<String, Map<ShoppingCartItem, Map<Integer, BigDecimal>>> supplierPartyEntry : dropShipItems.entrySet()) {
            String supplierPartyId = supplierPartyEntry.getKey();
            CartShipInfo shipInfo = null;
            int newShipGroupIndex = -1 ;

            // Attempt to get the first ship group for the supplierPartyId
            TreeMap<Integer, CartShipInfo> supplierShipGroups = this.getShipGroupsBySupplier(supplierPartyId);
            if (! UtilValidate.isEmpty(supplierShipGroups)) {
                newShipGroupIndex = supplierShipGroups.firstKey();
                shipInfo = supplierShipGroups.get(supplierShipGroups.firstKey());
            }
            if (newShipGroupIndex == -1) {
                newShipGroupIndex = addShipInfo();
                shipInfo = this.shipInfo.get(newShipGroupIndex);
            }
            shipInfo.supplierPartyId = supplierPartyId;
            shipInfo.supplierAgreementId = null;

            Map<ShoppingCartItem, Map<Integer, BigDecimal>> supplierCartItems = UtilGenerics.checkMap(supplierPartyEntry.getValue());
            for (Entry<ShoppingCartItem, Map<Integer, BigDecimal>> cartItemEntry : supplierCartItems.entrySet()) {
                ShoppingCartItem cartItem = cartItemEntry.getKey();
                Map<Integer, BigDecimal> cartItemGroupQuantities = UtilGenerics.checkMap(cartItemEntry.getValue());
                for (Entry<Integer, BigDecimal> previousShipGroupIndexEntry : cartItemGroupQuantities.entrySet()) {
                    Integer previousShipGroupIndex = previousShipGroupIndexEntry.getKey();
                    BigDecimal dropShipQuantity = previousShipGroupIndexEntry.getValue();
                    positionItemToGroup(cartItem, dropShipQuantity, previousShipGroupIndex, newShipGroupIndex, true);
                }
            }
        }
    }

    static class BasePriceOrderComparator implements Comparator<Object>, Serializable {
        private boolean ascending = false;

        BasePriceOrderComparator(boolean ascending) {
            this.ascending = ascending;
        }

        @Override
        public int compare(java.lang.Object obj, java.lang.Object obj1) {
            ShoppingCartItem cartItem = (ShoppingCartItem) obj;
            ShoppingCartItem cartItem1 = (ShoppingCartItem) obj1;

            return this.ascending
                    ? cartItem.getBasePrice().compareTo(cartItem1.getBasePrice())
                    : cartItem1.getBasePrice().compareTo(cartItem.getBasePrice());
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (ascending ? 1231 : 1237);
            return result;
        }

        @Override
        public boolean equals(java.lang.Object obj) {
            if (obj instanceof BasePriceOrderComparator) {
                return this.ascending == ((BasePriceOrderComparator) obj).ascending;
            }
            return false;
        }
    }

    public static class ShoppingCartItemGroup implements Serializable {
        private String groupNumber;
        private String groupName;
        private ShoppingCartItemGroup parentGroup;

        // don't allow empty constructor
        @SuppressWarnings("unused")
        private ShoppingCartItemGroup() {}

        protected ShoppingCartItemGroup(long groupNumber, String groupName) {
            this(groupNumber, groupName, null);
        }

        /** Note that to avoid foreign key issues when the groups are created a parentGroup should have a lower number than the child group. */
        protected ShoppingCartItemGroup(long groupNumber, String groupName, ShoppingCartItemGroup parentGroup) {
            this(UtilFormatOut.formatPaddedNumber(groupNumber, 2), groupName, parentGroup);
        }

        protected ShoppingCartItemGroup(String groupNumber, String groupName, ShoppingCartItemGroup parentGroup) {
            this.groupNumber = groupNumber;
            this.groupName = groupName;
            this.parentGroup = parentGroup;
        }

        protected ShoppingCartItemGroup(ShoppingCartItemGroup itemGroup, ShoppingCartItemGroup parentGroup) {
            this.groupNumber = itemGroup.groupNumber;
            this.groupName = itemGroup.groupName;
            this.parentGroup = parentGroup;
        }

        public String getGroupNumber() {
            return this.groupNumber;
        }

        public String getGroupName() {
            return this.groupName;
        }

        public void setGroupName(String str) {
            this.groupName = str;
        }

        public ShoppingCartItemGroup getParentGroup () {
            return this.parentGroup;
        }

        protected GenericValue makeOrderItemGroup(Delegator delegator) {
            GenericValue orderItemGroup = delegator.makeValue("OrderItemGroup");
            orderItemGroup.set("orderItemGroupSeqId", this.getGroupNumber());
            orderItemGroup.set("groupName", this.getGroupName());
            if (this.parentGroup != null) {
                orderItemGroup.set("parentGroupSeqId", this.parentGroup.getGroupNumber());
            }
            return orderItemGroup;
        }

        public void inheritParentsParent() {
            if (this.parentGroup != null) {
                this.parentGroup = this.parentGroup.getParentGroup();
            }
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((groupName == null) ? 0 : groupName.hashCode());
            result = prime * result + ((groupNumber == null) ? 0 : groupNumber.hashCode());
            result = prime * result + ((parentGroup == null) ? 0 : parentGroup.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof ShoppingCartItemGroup) {
                ShoppingCartItemGroup that = (ShoppingCartItemGroup) obj;
                if (that.groupNumber.equals(this.groupNumber)) {
                    return true;
                }
            }
            return false;
        }
    }

    public static class ProductPromoUseInfo implements Serializable, Comparable<ProductPromoUseInfo> {
        public String productPromoId = null;
        public String productPromoCodeId = null;
        public BigDecimal totalDiscountAmount = BigDecimal.ZERO;
        public BigDecimal quantityLeftInActions = BigDecimal.ZERO;
        private Map<ShoppingCartItem,BigDecimal> usageInfoMap = null;

        public ProductPromoUseInfo(String productPromoId, String productPromoCodeId, BigDecimal totalDiscountAmount, BigDecimal quantityLeftInActions, Map<ShoppingCartItem,BigDecimal> usageInfoMap) {
            this.productPromoId = productPromoId;
            this.productPromoCodeId = productPromoCodeId;
            this.totalDiscountAmount = totalDiscountAmount;
            this.quantityLeftInActions = quantityLeftInActions;
            this.usageInfoMap = usageInfoMap;
        }

        public String getProductPromoId() { return this.productPromoId; }
        public String getProductPromoCodeId() { return this.productPromoCodeId; }
        public BigDecimal getTotalDiscountAmount() { return this.totalDiscountAmount; }
        public BigDecimal getQuantityLeftInActions() { return this.quantityLeftInActions; }
        public Map<ShoppingCartItem,BigDecimal> getUsageInfoMap() { return this.usageInfoMap; }
        public BigDecimal getUsageWeight() {
            Iterator<Entry<ShoppingCartItem, BigDecimal>> lineItems = this.usageInfoMap.entrySet().iterator();
            BigDecimal totalAmount = BigDecimal.ZERO;
            while (lineItems.hasNext()) {
                ShoppingCartItem lineItem = lineItems.next().getKey();
                totalAmount = totalAmount.add(lineItem.getBasePrice().multiply(usageInfoMap.get(lineItem)));
            }
            if (totalAmount.compareTo(BigDecimal.ZERO) == 0) {
                return BigDecimal.ZERO;
            }
            return getTotalDiscountAmount().negate().divide(totalAmount, scale, rounding);
        }

        @Override
        public int compareTo(ProductPromoUseInfo other) {
                return other.getUsageWeight().compareTo(getUsageWeight());
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((productPromoCodeId == null) ? 0 : productPromoCodeId.hashCode());
            result = prime * result + ((productPromoId == null) ? 0 : productPromoId.hashCode());
            result = prime * result + ((quantityLeftInActions == null) ? 0 : quantityLeftInActions.hashCode());
            result = prime * result + ((totalDiscountAmount == null) ? 0 : totalDiscountAmount.hashCode());
            result = prime * result + ((usageInfoMap == null) ? 0 : usageInfoMap.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            ProductPromoUseInfo other = (ProductPromoUseInfo) obj;
            if (productPromoCodeId == null) {
                if (other.productPromoCodeId != null) {
                    return false;
                }
            } else if (!productPromoCodeId.equals(other.productPromoCodeId)) {
                return false;
            }
            if (productPromoId == null) {
                if (other.productPromoId != null) {
                    return false;
                }
            } else if (!productPromoId.equals(other.productPromoId)) {
                return false;
            }
            if (quantityLeftInActions == null) {
                if (other.quantityLeftInActions != null) {
                    return false;
                }
            } else if (!quantityLeftInActions.equals(other.quantityLeftInActions)) {
                return false;
            }
            if (totalDiscountAmount == null) {
                if (other.totalDiscountAmount != null) {
                    return false;
                }
            } else if (!totalDiscountAmount.equals(other.totalDiscountAmount)) {
                return false;
            }
            if (usageInfoMap == null) {
                if (other.usageInfoMap != null) {
                    return false;
                }
            } else if (!usageInfoMap.equals(other.usageInfoMap)) {
                return false;
            }
            return true;
        }
    }

    public static class CartShipInfo implements Serializable {
        public Map<ShoppingCartItem, CartShipItemInfo> shipItemInfo = new HashMap<>();
        public List<GenericValue> shipTaxAdj = new LinkedList<>();
        public String orderTypeId = null;
        private String internalContactMechId = null;
        public String telecomContactMechId = null;
        public String shipmentMethodTypeId = null;
        public String supplierPartyId = null;
        public String supplierAgreementId = null;
        public String carrierRoleTypeId = null;
        public String carrierPartyId = null;
        private String facilityId = null;
        public String giftMessage = null;
        public String shippingInstructions = null;
        public String maySplit = "N";
        public String isGift = "N";
        public BigDecimal shipEstimate = BigDecimal.ZERO;
        public Timestamp shipBeforeDate = null;
        public Timestamp shipAfterDate = null;
        private String shipGroupSeqId = null;
        private String associatedShipGroupSeqId = null;
        public String vendorPartyId = null;
        public String productStoreShipMethId = null;
        public Map<String, Object> attributes = new HashMap<>();

        public void setAttribute(String name, Object value) {
            this.attributes.put(name, value);
        }

        public void removeAttribute(String name) {
            this.attributes.remove(name);
        }

        @SuppressWarnings("unchecked")
        public <T> T getAttribute(String name) {
            return (T) this.attributes.get(name);
        }

        public String getOrderTypeId() { return orderTypeId; }

        public String getContactMechId() { return internalContactMechId; }
        public void setContactMechId(String contactMechId) {
            this.internalContactMechId = contactMechId;
        }

        public String getCarrierPartyId() { return carrierPartyId; }
        public String getSupplierPartyId() { return supplierPartyId; }
        public String getSupplierAgreementId() { return supplierAgreementId; }
        public String getShipmentMethodTypeId() { return shipmentMethodTypeId; }
        public BigDecimal getShipEstimate() { return shipEstimate; }

        public String getShipGroupSeqId() { return shipGroupSeqId; }
        public void setShipGroupSeqId(String shipGroupSeqId) {
            this.shipGroupSeqId = shipGroupSeqId;
        }

        public String getAssociatedShipGroupSeqId() { return associatedShipGroupSeqId; }
        public void setAssociatedShipGroupSeqId(String shipGroupSeqId) {
            this.associatedShipGroupSeqId = shipGroupSeqId;
        }

        public String getFacilityId() { return facilityId; }
        public void setFacilityId(String facilityId) {
            this.facilityId = facilityId;
        }

        public String getVendorPartyId() { return vendorPartyId;}
        public void setVendorPartyId(String vendorPartyId) {
            this.vendorPartyId = vendorPartyId;
        }

        public void setMaySplit(Boolean maySplit) {
            if (UtilValidate.isNotEmpty(maySplit)) {
                this.maySplit = maySplit ? "Y" : "N";
            }
        }

        public GenericValue getOriginAddress(ShoppingCart cart) {
            GenericValue originAddress = null;
            Delegator delegator = cart.delegator;
            //check information from the cart ship info
            try {
                if (originAddress == null && facilityId != null) {
                    originAddress = ShippingEvents.getShippingOriginContactMechFromFacility(delegator, facilityId);
                }
                if (originAddress == null && supplierPartyId != null) {
                    originAddress = ShippingEvents.getShippingOriginContactMech(delegator, supplierPartyId);
                }
                if (originAddress == null && vendorPartyId != null) {
                    originAddress = ShippingEvents.getShippingOriginContactMech(delegator, vendorPartyId);
                }

                if (originAddress == null) {
                    //try now to resolve from the cart
                    if (cart.getFacilityId() != null) {
                        originAddress = ShippingEvents.getShippingOriginContactMechFromFacility(delegator, cart.getFacilityId());
                    }
                    if (originAddress == null && cart.getShipFromVendorPartyId() != null) {
                        originAddress = ShippingEvents.getShippingOriginContactMech(delegator, cart.getShipFromVendorPartyId());
                    }
                    if (originAddress == null && cart.getBillFromVendorPartyId() != null) {
                        originAddress = ShippingEvents.getShippingOriginContactMech(delegator, cart.getBillFromVendorPartyId());
                    }
                }
            } catch (GeneralException e) {
                Debug.logError("Impossible to resolve an originAddress " + e.toString(), module);
            }
            return originAddress;
        }
        public void clearAllTaxInfo() {
            this.shipTaxAdj.clear();
            for (CartShipItemInfo itemInfo : shipItemInfo.values()) {
                itemInfo.itemTaxAdj.clear();
            }
        }

        @Deprecated
        public List<GenericValue> makeItemShipGroupAndAssoc(LocalDispatcher dispatcher, Delegator delegator, ShoppingCart cart, String shipGroupSeqId, boolean newShipGroup) {
            return makeItemShipGroupAndAssoc(dispatcher, delegator, cart, shipGroupSeqId);
        }

        public List<GenericValue> makeItemShipGroupAndAssoc(LocalDispatcher dispatcher, Delegator delegator, ShoppingCart cart, String shipGroupSeqId) {
            List<GenericValue> values = new LinkedList<>();

            // create order contact mech for shipping address
            if (this.internalContactMechId != null) {
                GenericValue orderCm = delegator.makeValue("OrderContactMech");
                orderCm.set("contactMechPurposeTypeId", "SHIPPING_LOCATION");
                orderCm.set("contactMechId", this.internalContactMechId);
                values.add(orderCm);
            }

            // create the ship group
            GenericValue shipGroup = delegator.makeValue("OrderItemShipGroup");
            shipGroup.set("shipmentMethodTypeId", shipmentMethodTypeId);
            shipGroup.set("carrierRoleTypeId", carrierRoleTypeId);
            shipGroup.set("carrierPartyId", carrierPartyId);
            shipGroup.set("supplierPartyId", supplierPartyId);
            shipGroup.set("supplierAgreementId", supplierAgreementId);
            shipGroup.set("shippingInstructions", shippingInstructions);
            shipGroup.set("giftMessage", giftMessage);
            shipGroup.set("contactMechId", this.internalContactMechId);
            shipGroup.set("telecomContactMechId", this.telecomContactMechId);
            shipGroup.set("maySplit", maySplit);
            shipGroup.set("isGift", isGift);
            shipGroup.set("shipGroupSeqId", shipGroupSeqId);
            shipGroup.set("vendorPartyId", vendorPartyId);
            shipGroup.set("facilityId", facilityId);

            // use the cart's default ship before and after dates here
            if ((shipBeforeDate == null) && (cart.getDefaultShipBeforeDate() != null)) {
                shipGroup.set("shipByDate", cart.getDefaultShipBeforeDate());
            } else {
                shipGroup.set("shipByDate", shipBeforeDate);
            }
            if ((shipAfterDate == null) && (cart.getDefaultShipAfterDate() != null)) {
                shipGroup.set("shipAfterDate", cart.getDefaultShipAfterDate());
            } else {
                shipGroup.set("shipAfterDate", shipAfterDate);
            }

            values.add(shipGroup);

            //set estimated ship dates
            LinkedList<Timestamp> estimatedShipDates = new LinkedList<>();
            for (ShoppingCartItem item : shipItemInfo.keySet()) {
                Timestamp estimatedShipDate = item.getEstimatedShipDate();
                if (estimatedShipDate != null) {
                    estimatedShipDates.add(estimatedShipDate);
                }
            }
            Timestamp estimatedShipDate = null;
            if (estimatedShipDates.size() > 0) {
                Collections.sort(estimatedShipDates);
                estimatedShipDate  = estimatedShipDates.getLast();
                shipGroup.set("estimatedShipDate", estimatedShipDate);
            }

            //set estimated delivery dates
            LinkedList<Timestamp> estimatedDeliveryDates = new LinkedList<>();
            for (ShoppingCartItem item : shipItemInfo.keySet()) {
                Timestamp estimatedDeliveryDate = item.getDesiredDeliveryDate();
                if (estimatedDeliveryDate != null) {
                    estimatedDeliveryDates.add(estimatedDeliveryDate);
                }
            }
            Timestamp estimatedDeliveryDate = null;
            if (UtilValidate.isNotEmpty(estimatedDeliveryDates)) {
                Collections.sort(estimatedDeliveryDates);
                estimatedDeliveryDate = estimatedDeliveryDates.getLast();
            } else {

                // check if a shipment time estimate exists to resolve the estimated delivery date
                long shipTimeEstimateSize = 0;
                try {
                    shipTimeEstimateSize = EntityQuery.use(delegator)
                            .from("ShipmentTimeEstimate")
                            .where("shipmentMethodTypeId", shipmentMethodTypeId)
                            .filterByDate()
                            .queryCount();
                } catch (GenericEntityException e) {
                    Debug.logError("Error to resolve ShipmentTimeEstimate quantity", module);
                }
                if (shipTimeEstimateSize != 0) {
                    try {
                        GenericValue shippingAddress = EntityQuery.use(delegator).from("PostalAddress").where("contactMechId", this.internalContactMechId).cache().queryOne();
                        GenericValue originAddress = getOriginAddress(cart);
                        List<GenericValue> shipmentTimeEstimates = ShippingEvents.getShipmentTimeEstimates(delegator, shipmentMethodTypeId, carrierPartyId, carrierRoleTypeId, shippingAddress, originAddress);
                        GenericValue carrierShipmentMethod = delegator.makeValidValue("CarrierShipmentMethod", shipGroup);
                        carrierShipmentMethod.put("partyId", carrierPartyId);
                        if (carrierRoleTypeId == null) carrierRoleTypeId = "CARRIER";
                        carrierShipmentMethod.put("roleTypeId", carrierRoleTypeId);
                        Double estimatedDays = ShippingEvents.getShippingTimeEstimateInDay(dispatcher, carrierShipmentMethod, shipmentTimeEstimates);
                        if (estimatedDays != null) {
                            Timestamp referenceDate = Stream.of(estimatedShipDate, shipBeforeDate, shipAfterDate, UtilDateTime.nowTimestamp())
                                    .filter(Objects::nonNull).findFirst().get();
                            estimatedDeliveryDate = UtilDateTime.addDaysToTimestamp(referenceDate, estimatedDays);
                        }
                    } catch (GenericEntityException e) {
                        Debug.logError("Error to resolve ShipmentTimeEstimate days", module);
                    }
                }
            }
            if (estimatedDeliveryDate != null) {
                shipGroup.set("estimatedDeliveryDate", estimatedDeliveryDate);
            }

            // create the shipping estimate adjustments
            if (shipEstimate.compareTo(BigDecimal.ZERO) != 0) {
                GenericValue shipAdj = delegator.makeValue("OrderAdjustment");
                shipAdj.set("orderAdjustmentTypeId", "SHIPPING_CHARGES");
                shipAdj.set("amount", shipEstimate);
                if("PURCHASE_ORDER".equals(cart.getOrderType())) {
                    shipAdj.set("isManual", "Y");
                }
                shipAdj.set("shipGroupSeqId", shipGroupSeqId);
                values.add(shipAdj);
            }

            // create the top level tax adjustments
            for (GenericValue taxAdj : shipTaxAdj) {
                taxAdj.set("shipGroupSeqId", shipGroupSeqId);
                values.add(taxAdj);
            }

            // create the ship group item associations
            for (Entry<ShoppingCartItem, CartShipItemInfo> entry : shipItemInfo.entrySet()) {
                ShoppingCartItem item = entry.getKey();
                CartShipItemInfo itemInfo = entry.getValue();

                GenericValue assoc = delegator.makeValue("OrderItemShipGroupAssoc");
                assoc.set("orderItemSeqId", item.getOrderItemSeqId());
                assoc.set("shipGroupSeqId", shipGroupSeqId);
                assoc.set("quantity", itemInfo.quantity);
                values.add(assoc);

                // create the item tax adjustment
                for (GenericValue taxAdj : itemInfo.itemTaxAdj) {
                    taxAdj.set("orderItemSeqId", item.getOrderItemSeqId());
                    taxAdj.set("shipGroupSeqId", shipGroupSeqId);
                    values.add(taxAdj);
                }
            }

            return values;
        }

        public CartShipItemInfo setItemInfo(ShoppingCartItem item, BigDecimal quantity, List<GenericValue> taxAdj) {
            CartShipItemInfo itemInfo = shipItemInfo.get(item);
            if (itemInfo == null) {
                if (!isShippableToAddress(item)) {
                    throw new IllegalArgumentException("The shipping address is not compatible with ProductGeos rules.");
                }
                itemInfo = new CartShipItemInfo();
                itemInfo.item = item;
                shipItemInfo.put(item, itemInfo);
            }
            if (quantity.compareTo(BigDecimal.ZERO) >= 0) {
                itemInfo.quantity = quantity;
            }
            if (taxAdj != null) {
                itemInfo.itemTaxAdj.clear();
                itemInfo.itemTaxAdj.addAll(taxAdj);
            }
            return itemInfo;
        }

        public CartShipItemInfo setItemInfo(ShoppingCartItem item, List<GenericValue> taxAdj) {
            return setItemInfo(item, BigDecimal.ONE.negate(), taxAdj);
        }

        public CartShipItemInfo setItemInfo(ShoppingCartItem item, BigDecimal quantity) {
            return setItemInfo(item, quantity, null);
        }

        public CartShipItemInfo getShipItemInfo(ShoppingCartItem item) {
            return shipItemInfo.get(item);
        }

        public Set<ShoppingCartItem> getShipItems() {
            return shipItemInfo.keySet();
        }

        private boolean isShippableToAddress(ShoppingCartItem item) {
            if ("SALES_ORDER".equals(getOrderTypeId())) {
                // Verify if the new address is compatible with the ProductGeos rules of
                // the products already in the cart
                GenericValue shippingAddress = null;
                try {
                    shippingAddress = item.getDelegator().findOne("PostalAddress", UtilMisc.toMap("contactMechId", this.internalContactMechId), false);
                } catch (GenericEntityException gee) {
                    Debug.logError(gee, "Error retrieving the shipping address for contactMechId [" + this.internalContactMechId + "].", module);
                }
                if (shippingAddress != null) {
                    GenericValue product = item.getProduct();
                    if (product != null) {
                        return ProductWorker.isShippableToAddress(product, shippingAddress);
                    }
                }
            }
            return true;
        }

        /**
         * Reset the ship group's shipBeforeDate if it is after the parameter
         * @param newShipBeforeDate the ship group's shipBeforeDate to be reset
         */
        public void resetShipBeforeDateIfAfter(Timestamp newShipBeforeDate) {
                if (newShipBeforeDate != null) {
                if ((this.shipBeforeDate == null) || (!this.shipBeforeDate.before(newShipBeforeDate))) {
                    this.shipBeforeDate = (Timestamp) newShipBeforeDate.clone();
                }
            }
        }

        /**
         * Reset the ship group's shipAfterDate if it is before the parameter
         * @param newShipAfterDate the ship group's shipAfterDate to be reset
         */
        public void resetShipAfterDateIfBefore(Timestamp newShipAfterDate) {
            if (newShipAfterDate != null) {
                if ((this.shipAfterDate == null) || (!this.shipAfterDate.after(newShipAfterDate))) {
                    this.shipAfterDate = (Timestamp) newShipAfterDate.clone();
                }
            }
        }

        public BigDecimal getTotalTax(ShoppingCart cart) {
            List<GenericValue> taxAdjustments = new LinkedList<>();
            taxAdjustments.addAll(shipTaxAdj);
            for (CartShipItemInfo info : shipItemInfo.values()) {
                taxAdjustments.addAll(info.itemTaxAdj);
            }
            Map<String, Object> taxByAuthority = OrderReadHelper.getOrderTaxByTaxAuthGeoAndParty(taxAdjustments);
            BigDecimal taxTotal = (BigDecimal) taxByAuthority.get("taxGrandTotal");
            return taxTotal;
        }

        public BigDecimal getTotal() {
            BigDecimal shipItemTotal = BigDecimal.ZERO;
            for (CartShipItemInfo info : shipItemInfo.values()) {
                shipItemTotal = shipItemTotal.add(info.getItemSubTotal());
            }

            return shipItemTotal;
        }

        public static class CartShipItemInfo implements Serializable {
            public List<GenericValue> itemTaxAdj = new LinkedList<>();
            public ShoppingCartItem item = null;
            public BigDecimal quantity = BigDecimal.ZERO;

            public BigDecimal getItemTax(ShoppingCart cart) {
                BigDecimal itemTax = BigDecimal.ZERO;

                for (int i = 0; i < itemTaxAdj.size(); i++) {
                    GenericValue v = itemTaxAdj.get(i);
                    itemTax = itemTax.add(OrderReadHelper.calcItemAdjustment(v, quantity, item.getBasePrice()));
                }

                return itemTax.setScale(taxCalcScale, taxRounding);
            }

            public ShoppingCartItem getItem() {
                return this.item;
            }

            public BigDecimal getItemQuantity() {
                return this.quantity;
            }

            public BigDecimal getItemSubTotal() {
                return item.getItemSubTotal(quantity);
            }
        }
    }

    public static class CartPaymentInfo implements Serializable, Comparable<Object> {
        public String paymentMethodTypeId = null;
        public String paymentMethodId = null;
        public String finAccountId = null;
        public String securityCode = null;
        public String postalCode = null;
        public String[] refNum = new String[2];
        public String track2 = null;
        public BigDecimal amount = null;
        public boolean singleUse = false;
        public boolean isPresent = false;
        public boolean isSwiped = false;
        public boolean overflow = false;

        public GenericValue getValueObject(Delegator delegator) {
            String entityName = null;
            Map<String, String> lookupFields = null;
            if (paymentMethodId != null) {
                lookupFields = UtilMisc.<String, String>toMap("paymentMethodId", paymentMethodId);
                entityName = "PaymentMethod";
            } else if (paymentMethodTypeId != null) {
                lookupFields = UtilMisc.<String, String>toMap("paymentMethodTypeId", paymentMethodTypeId);
                entityName = "PaymentMethodType";
            } else {
                throw new IllegalArgumentException("Could not create value object because paymentMethodId and paymentMethodTypeId are null");
            }

            try {
                return EntityQuery.use(delegator).from(entityName).where(lookupFields).cache(true).queryOne();
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
            }

            return null;
        }

        public GenericValue getBillingAddress(Delegator delegator) {
            GenericValue valueObj = this.getValueObject(delegator);
            GenericValue postalAddress = null;

            if ("PaymentMethod".equals(valueObj.getEntityName())) {
                String paymentMethodTypeId = valueObj.getString("paymentMethodTypeId");
                String paymentMethodId = valueObj.getString("paymentMethodId");

                // billing account, credit card, gift card, eft account all have postal address
                try {
                    GenericValue pmObj = null;
                    if ("CREDIT_CARD".equals(paymentMethodTypeId)) {
                        pmObj = EntityQuery.use(delegator).from("CreditCard").where("paymentMethodId", paymentMethodId).queryOne();
                    } else if ("GIFT_CARD".equals(paymentMethodTypeId)) {
                        pmObj = EntityQuery.use(delegator).from("GiftCard").where("paymentMethodId", paymentMethodId).queryOne();
                    } else if ("EFT_ACCOUNT".equals(paymentMethodTypeId)) {
                        pmObj = EntityQuery.use(delegator).from("EftAccount").where("paymentMethodId", paymentMethodId).queryOne();
                    } else if ("EXT_BILLACT".equals(paymentMethodTypeId)) {
                        pmObj = EntityQuery.use(delegator).from("BillingAccount").where("paymentMethodId", paymentMethodId).queryOne();
                    } else if ("EXT_PAYPAL".equals(paymentMethodTypeId)) {
                        pmObj = EntityQuery.use(delegator).from("PayPalPaymentMethod").where("paymentMethodId", paymentMethodId).queryOne();
                    }
                    if (pmObj != null) {
                        postalAddress = pmObj.getRelatedOne("PostalAddress", false);
                    } else {
                        Debug.logInfo("No PaymentMethod Object Found - " + paymentMethodId, module);
                    }
                } catch (GenericEntityException e) {
                    Debug.logError(e, module);
                }
            }

            return postalAddress;
        }

        public List<GenericValue> makeOrderPaymentInfos(Delegator delegator, ShoppingCart cart) {
            BigDecimal maxAmount = BigDecimal.ZERO;
            GenericValue valueObj = this.getValueObject(delegator);
            List<GenericValue> values = new LinkedList<>();
            if (valueObj != null) {
                // first create a BILLING_LOCATION for the payment method address if there is one
                if ("PaymentMethod".equals(valueObj.getEntityName())) {
                    String billingAddressId = null;

                    GenericValue billingAddress = this.getBillingAddress(delegator);
                    if (billingAddress != null) {
                        billingAddressId = billingAddress.getString("contactMechId");
                    }

                    if (UtilValidate.isNotEmpty(billingAddressId)) {
                        GenericValue orderCm = delegator.makeValue("OrderContactMech");
                        orderCm.set("contactMechPurposeTypeId", "BILLING_LOCATION");
                        orderCm.set("contactMechId", billingAddressId);
                        values.add(orderCm);
                    }
                }

                GenericValue productStore = null;
                String splitPayPrefPerShpGrp = null;
                try {
                    productStore = EntityQuery.use(delegator).from("ProductStore").where("productStoreId", cart.getProductStoreId()).queryOne();
                } catch (GenericEntityException e) {
                    Debug.logError(e.toString(), module);
                }
                if (productStore != null) {
                    splitPayPrefPerShpGrp = productStore.getString("splitPayPrefPerShpGrp");
                }
                if (splitPayPrefPerShpGrp == null) {
                    splitPayPrefPerShpGrp = "N";
                }
                if ("Y".equals(splitPayPrefPerShpGrp) && cart.paymentInfo.size() > 1) {
                    throw new GeneralRuntimeException("Split Payment Preference per Ship Group does not yet support multiple Payment Methods");
                }
                if ("Y".equals(splitPayPrefPerShpGrp)  && cart.paymentInfo.size() == 1) {
                    for (CartShipInfo csi : cart.getShipGroups()) {
                        maxAmount = csi.getTotal().add(cart.getOrderOtherAdjustmentTotal().add(cart.getOrderGlobalAdjustments()).divide(new BigDecimal(cart.getShipGroupSize()), generalRounding)).add(csi.getShipEstimate().add(csi.getTotalTax(cart)));
                        maxAmount = maxAmount.setScale(scale, rounding);

                        // create the OrderPaymentPreference record
                        GenericValue opp = delegator.makeValue("OrderPaymentPreference");
                        opp.set("paymentMethodTypeId", valueObj.getString("paymentMethodTypeId"));
                        opp.set("presentFlag", isPresent ? "Y" : "N");
                        opp.set("swipedFlag", isSwiped ? "Y" : "N");
                        opp.set("overflowFlag", overflow ? "Y" : "N");
                        opp.set("paymentMethodId", paymentMethodId);
                        opp.set("finAccountId", finAccountId);
                        opp.set("billingPostalCode", postalCode);
                        opp.set("maxAmount", maxAmount);
                        opp.set("shipGroupSeqId", csi.getShipGroupSeqId());
                        if (refNum != null) {
                            opp.set("manualRefNum", refNum[0]);
                            opp.set("manualAuthCode", refNum[1]);
                        }
                        if (securityCode != null) {
                            opp.set("securityCode", securityCode);
                        }
                        if (track2 != null) {
                           opp.set("track2", track2);
                        }
                        if (paymentMethodId != null || "FIN_ACCOUNT".equals(paymentMethodTypeId)) {
                            opp.set("statusId", "PAYMENT_NOT_AUTH");
                        } else if (paymentMethodTypeId != null) {
                            // external payment method types require notification when received
                            // internal payment method types are assumed to be in-hand
                            if (paymentMethodTypeId.startsWith("EXT_")) {
                                opp.set("statusId", "PAYMENT_NOT_RECEIVED");
                            } else {
                                opp.set("statusId", "PAYMENT_RECEIVED");
                            }
                        }
                        Debug.logInfo("ShipGroup [" + csi.getShipGroupSeqId() +"]", module);
                        Debug.logInfo("Creating OrderPaymentPreference - " + opp, module);
                        values.add(opp);
                    }
                } else if ("N".equals(splitPayPrefPerShpGrp)) {
                    maxAmount = maxAmount.add(amount);
                    maxAmount = maxAmount.setScale(scale, rounding);

                    // create the OrderPaymentPreference record
                    GenericValue opp = delegator.makeValue("OrderPaymentPreference");
                    opp.set("paymentMethodTypeId", valueObj.getString("paymentMethodTypeId"));
                    opp.set("presentFlag", isPresent ? "Y" : "N");
                    opp.set("swipedFlag", isSwiped ? "Y" : "N");
                    opp.set("overflowFlag", overflow ? "Y" : "N");
                    opp.set("paymentMethodId", paymentMethodId);
                    opp.set("finAccountId", finAccountId);
                    opp.set("billingPostalCode", postalCode);
                    opp.set("maxAmount", maxAmount);
                    if (refNum != null) {
                        opp.set("manualRefNum", refNum[0]);
                        opp.set("manualAuthCode", refNum[1]);
                    }
                    if (securityCode != null) {
                        opp.set("securityCode", securityCode);
                    }
                    if (track2 != null) {
                        opp.set("track2", securityCode);
                    }
                    if (paymentMethodId != null || "FIN_ACCOUNT".equals(paymentMethodTypeId)) {
                        opp.set("statusId", "PAYMENT_NOT_AUTH");
                    } else if (paymentMethodTypeId != null) {
                        // external payment method types require notification when received
                        // internal payment method types are assumed to be in-hand
                        if (paymentMethodTypeId.startsWith("EXT_")) {
                            opp.set("statusId", "PAYMENT_NOT_RECEIVED");
                        } else {
                            opp.set("statusId", "PAYMENT_RECEIVED");
                        }
                    }
                    Debug.logInfo("Creating OrderPaymentPreference - " + opp, module);
                    values.add(opp);
                }
            }

            return values;
        }

        @Override
        public int compareTo(Object o) {
            CartPaymentInfo that = (CartPaymentInfo) o;
            Debug.logInfo("Compare [" + this.toString() + "] to [" + that.toString() + "]", module);

            if (this.finAccountId == null) {
                if (that.finAccountId != null) {
                    return -1;
                }
            } else if (!this.finAccountId.equals(that.finAccountId)) {
                return -1;
            }

            if (this.paymentMethodId != null) {
                if (that.paymentMethodId == null) {
                    return 1;
                }
                int pmCmp = this.paymentMethodId.compareTo(that.paymentMethodId);
                if (pmCmp == 0) {
                    if (this.refNum != null && this.refNum[0] != null) {
                        if (that.refNum != null && that.refNum[0] != null) {
                            return this.refNum[0].compareTo(that.refNum[0]);
                        }
                        return 1;
                    }
                    if (that.refNum != null && that.refNum[0] != null) {
                        return -1;
                    }
                    return 0;
                }
                return pmCmp;
            }
            if (that.paymentMethodId != null) {
                return -1;
            }

            int pmtCmp = this.paymentMethodTypeId.compareTo(that.paymentMethodTypeId);
            if (pmtCmp == 0) {
                if (this.refNum != null && this.refNum[0] != null) {
                    if (that.refNum != null && that.refNum[0] != null) {
                        return this.refNum[0].compareTo(that.refNum[0]);
                    }
                    return 1;
                }
                if (that.refNum != null && that.refNum[0] != null) {
                    return -1;
                }
                return 0;
            }
            return pmtCmp;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((amount == null) ? 0 : amount.hashCode());
            result = prime * result + ((finAccountId == null) ? 0 : finAccountId.hashCode());
            result = prime * result + (isPresent ? 1231 : 1237);
            result = prime * result + (isSwiped ? 1231 : 1237);
            result = prime * result + (overflow ? 1231 : 1237);
            result = prime * result + ((paymentMethodId == null) ? 0 : paymentMethodId.hashCode());
            result = prime * result + ((paymentMethodTypeId == null) ? 0 : paymentMethodTypeId.hashCode());
            result = prime * result + ((postalCode == null) ? 0 : postalCode.hashCode());
            result = prime * result + Arrays.hashCode(refNum);
            result = prime * result + ((securityCode == null) ? 0 : securityCode.hashCode());
            result = prime * result + (singleUse ? 1231 : 1237);
            result = prime * result + ((track2 == null) ? 0 : track2.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            CartPaymentInfo other = (CartPaymentInfo) obj;
            if (amount == null) {
                if (other.amount != null) {
                    return false;
                }
            } else if (!amount.equals(other.amount)) {
                return false;
            }
            if (finAccountId == null) {
                if (other.finAccountId != null) {
                    return false;
                }
            } else if (!finAccountId.equals(other.finAccountId)) {
                return false;
            }
            if (isPresent != other.isPresent) {
                return false;
            }
            if (isSwiped != other.isSwiped) {
                return false;
            }
            if (overflow != other.overflow) {
                return false;
            }
            if (paymentMethodId == null) {
                if (other.paymentMethodId != null) {
                    return false;
                }
            } else if (!paymentMethodId.equals(other.paymentMethodId)) {
                return false;
            }
            if (paymentMethodTypeId == null) {
                if (other.paymentMethodTypeId != null) {
                    return false;
                }
            } else if (!paymentMethodTypeId.equals(other.paymentMethodTypeId)) {
                return false;
            }
            if (postalCode == null) {
                if (other.postalCode != null) {
                    return false;
                }
            } else if (!postalCode.equals(other.postalCode)) {
                return false;
            }
            if (!Arrays.equals(refNum, other.refNum)) {
                return false;
            }
            if (securityCode == null) {
                if (other.securityCode != null) {
                    return false;
                }
            } else if (!securityCode.equals(other.securityCode)) {
                return false;
            }
            if (singleUse != other.singleUse) {
                return false;
            }
            if (track2 == null) {
                if (other.track2 != null) {
                    return false;
                }
            } else if (!track2.equals(other.track2)) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            return "Pm: " + paymentMethodId + " / PmType: " + paymentMethodTypeId + " / Amt: " + amount + " / Ref: " + refNum[0] + "!" + refNum[1];
        }
    }

    public Map<String, String> getOrderAttributes() {
        return orderAttributes;
    }

    public void setOrderAttributes(Map<String, String> orderAttributes) {
        this.orderAttributes = orderAttributes;
    }

    public String getOrderStatusId() {
        return orderStatusId;
    }

    public void setOrderStatusId(String orderStatusId) {
        this.orderStatusId = orderStatusId;
    }

    public String getOrderStatusString() {
        return orderStatusString;
    }

    public void setOrderStatusString(String orderStatusString) {
        this.orderStatusString = orderStatusString;
    }

    public static BigDecimal getMinimumOrderQuantity(Delegator delegator, BigDecimal itemBasePrice, String itemProductId) throws GenericEntityException {
        BigDecimal minQuantity = BigDecimal.ZERO;
        BigDecimal minimumOrderPrice = BigDecimal.ZERO;

        List<GenericValue> minimumOrderPriceList =  EntityQuery.use(delegator).from("ProductPrice")
                                                        .where("productId", itemProductId, "productPriceTypeId", "MINIMUM_ORDER_PRICE")
                                                        .filterByDate()
                                                        .queryList();
        if (itemBasePrice == null) {
            List<GenericValue> productPriceList = EntityQuery.use(delegator).from("ProductPrice")
                                                      .where("productId", itemProductId)
                                                      .filterByDate()
                                                      .queryList();
            Map<String, BigDecimal> productPriceMap = new HashMap<>();
            for (GenericValue productPrice : productPriceList) {
                productPriceMap.put(productPrice.getString("productPriceTypeId"), productPrice.getBigDecimal("price"));
            }
            if (UtilValidate.isNotEmpty(productPriceMap.get("SPECIAL_PROMO_PRICE"))) {
                itemBasePrice = productPriceMap.get("SPECIAL_PROMO_PRICE");
            } else if (UtilValidate.isNotEmpty(productPriceMap.get("PROMO_PRICE"))) {
                itemBasePrice = productPriceMap.get("PROMO_PRICE");
            } else if (UtilValidate.isNotEmpty(productPriceMap.get("DEFAULT_PRICE"))) {
                itemBasePrice = productPriceMap.get("DEFAULT_PRICE");
            } else if (UtilValidate.isNotEmpty(productPriceMap.get("LIST_PRICE"))) {
                itemBasePrice = productPriceMap.get("LIST_PRICE");
            }
        }
        if (UtilValidate.isNotEmpty(minimumOrderPriceList)) {
            minimumOrderPrice = EntityUtil.getFirst(minimumOrderPriceList).getBigDecimal("price");
        }
        if (itemBasePrice != null && minimumOrderPrice.compareTo(itemBasePrice) > 0) {
            minQuantity = minimumOrderPrice.divide(itemBasePrice, 0, RoundingMode.UP);
        }
        return minQuantity;
    }
}
