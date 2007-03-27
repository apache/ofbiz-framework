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

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.*;

import javolution.util.FastList;
import javolution.util.FastMap;

import org.apache.commons.collections.map.LinkedMap;
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
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.party.contact.ContactHelper;
import org.ofbiz.party.contact.ContactMechWorker;
import org.ofbiz.order.order.OrderReadHelper;
import org.ofbiz.order.finaccount.FinAccountHelper;
import org.ofbiz.order.shoppingcart.product.ProductPromoWorker;
import org.ofbiz.order.shoppingcart.shipping.ShippingEstimateWrapper;
import org.ofbiz.order.shoppinglist.ShoppingListEvents;
import org.ofbiz.product.category.CategoryWorker;
import org.ofbiz.product.config.ProductConfigWrapper;
import org.ofbiz.product.store.ProductStoreWorker;
import org.ofbiz.service.LocalDispatcher;

/**
 * Shopping Cart Object
 */
public class ShoppingCart implements Serializable {

    public static final String module = ShoppingCart.class.getName();
    public static final String resource_error = "OrderErrorUiLabels";

    private String orderType = "SALES_ORDER"; // default orderType
    private String channel = "UNKNWN_SALES_CHANNEL"; // default channel enum

    private String poNumber = null;
    private String orderId = null;
    private String orderName = null;
    private String firstAttemptOrderId = null;
    private String externalId = null;
    private String internalCode = null;
    private String billingAccountId = null;
    private double billingAccountAmt = 0.00;
    private String agreementId = null;
    private String quoteId = null;
    private long nextItemSeq = 1;

    private String defaultItemDeliveryDate = null;
    private String defaultItemComment = null;

    private String orderAdditionalEmails = null;
    private boolean viewCartOnAdd = false;
    private boolean readOnlyCart = false;

    private Timestamp lastListRestore = null;
    private String autoSaveListId = null;

    /** Holds value of order adjustments. */
    private List adjustments = new LinkedList();
    // OrderTerms
    private boolean orderTermSet = false;
    private List orderTerms = new LinkedList();

    private List cartLines = FastList.newInstance();
    private Map itemGroupByNumberMap = FastMap.newInstance();
    protected long nextGroupNumber = 1;
    private List paymentInfo = FastList.newInstance();
    private List shipInfo = FastList.newInstance();
    private Map contactMechIdsMap = new HashMap();
    private Map orderAttributes = new HashMap();
    private Map attributes = new HashMap(); // user defined attributes
    // Lists of internal/public notes: when the order is stored they are transformed into OrderHeaderNotes
    private List internalOrderNotes = FastList.newInstance(); // internal notes
    private List orderNotes = FastList.newInstance(); // public notes (printed on documents etc.)

    /** contains a list of partyId for each roleTypeId (key) */
    private Map additionalPartyRole = new HashMap();

    /** these are defaults for all ship groups */
    private Timestamp defaultShipAfterDate = null;
    private Timestamp defaultShipBeforeDate = null;
    
    /** Contains a List for each productPromoId (key) containing a productPromoCodeId (or empty string for no code) for each use of the productPromoId */
    private List productPromoUseInfoList = new LinkedList();
    /** Contains the promo codes entered */
    private Set productPromoCodes = new HashSet();
    private List freeShippingProductPromoActions = new ArrayList();
    /** Note that even though this is promotion info, it should NOT be cleared when the promos are cleared, it is a preference that will be used in the next promo calculation */
    private Map desiredAlternateGiftByAction = new HashMap();
    private Timestamp cartCreatedTs = UtilDateTime.nowTimestamp();

    private transient GenericDelegator delegator = null;
    private String delegatorName = null;

    protected String productStoreId = null;
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

    /** don't allow empty constructor */
    protected ShoppingCart() {}

    /** Creates a new cloned ShoppingCart Object. */
    public ShoppingCart(ShoppingCart cart) {
        this.delegator = cart.getDelegator();
        this.delegatorName = delegator.getDelegatorName();
        this.productStoreId = cart.getProductStoreId();
        this.poNumber = cart.getPoNumber();
        this.orderId = cart.getOrderId();
        this.orderName = "Copy of " + cart.getOrderName();
        this.firstAttemptOrderId = cart.getFirstAttemptOrderId();
        this.billingAccountId = cart.getBillingAccountId();
        this.agreementId = cart.getAgreementId();
        this.quoteId = cart.getQuoteId();
        this.orderAdditionalEmails = cart.getOrderAdditionalEmails();
        this.adjustments = new LinkedList(cart.getAdjustments());
        this.contactMechIdsMap = new HashMap(cart.getOrderContactMechIds());
        this.freeShippingProductPromoActions = new ArrayList(cart.getFreeShippingProductPromoActions());
        this.desiredAlternateGiftByAction = cart.getAllDesiredAlternateGiftByActionCopy();
        this.productPromoUseInfoList = new LinkedList(cart.productPromoUseInfoList);
        this.productPromoCodes = new HashSet(cart.productPromoCodes);
        this.locale = cart.getLocale();
        this.currencyUom = cart.getCurrency();
        this.externalId = cart.getExternalId();
        this.internalCode = cart.getInternalCode();
        this.viewCartOnAdd = cart.viewCartOnAdd();
        this.defaultShipAfterDate = cart.getDefaultShipAfterDate();
        this.defaultShipBeforeDate = cart.getDefaultShipBeforeDate();
        
        this.terminalId = cart.getTerminalId();
        this.transactionId = cart.getTransactionId();
        this.autoOrderShoppingListId = cart.getAutoOrderShoppingListId();
        
        // clone the additionalPartyRoleMap
        this.additionalPartyRole = new HashMap();
        Iterator it = cart.additionalPartyRole.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry me = (Map.Entry) it.next();
            this.additionalPartyRole.put(me.getKey(), new LinkedList((Collection) me.getValue()));
        }

        // clone the groups
        Iterator groupIt = cart.itemGroupByNumberMap.values().iterator();
        while (groupIt.hasNext()) {
            ShoppingCartItemGroup itemGroup = (ShoppingCartItemGroup) groupIt.next();
            // get the new parent group by number from the existing set; as before the parent must come before all children to work...
            ShoppingCartItemGroup parentGroup = null;
            if (itemGroup.getParentGroup() != null) parentGroup = this.getItemGroupByNumber(itemGroup.getParentGroup().getGroupNumber());
            ShoppingCartItemGroup newGroup = new ShoppingCartItemGroup(itemGroup, parentGroup);
            itemGroupByNumberMap.put(newGroup.getGroupNumber(), newGroup);
        }

        // clone the items
        List items = cart.items();
        Iterator itIt = items.iterator();
        while (itIt.hasNext()) {
            cartLines.add(new ShoppingCartItem((ShoppingCartItem) itIt.next()));
        }
    }

    /** Creates new empty ShoppingCart object. */
    public ShoppingCart(GenericDelegator delegator, String productStoreId, String webSiteId, Locale locale, String currencyUom, String billToCustomerPartyId, String billFromVendorPartyId) {
        this.delegator = delegator;
        this.delegatorName = delegator.getDelegatorName();
        this.productStoreId = productStoreId;
        this.webSiteId = webSiteId;
        this.currencyUom = currencyUom;
        this.locale = locale;
        if (this.locale == null) {
            this.locale = Locale.getDefault();
        }

        if (productStoreId == null) {
            throw new IllegalArgumentException("productStoreId cannot be null");
        }

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
        } else {
            this.billFromVendorPartyId = billFromVendorPartyId;
        }
        this.billToCustomerPartyId = billToCustomerPartyId;
    }


    /** Creates new empty ShoppingCart object. */
    public ShoppingCart(GenericDelegator delegator, String productStoreId, String webSiteId, Locale locale, String currencyUom) {
        this(delegator, productStoreId, webSiteId, locale, currencyUom, null, null);
    }

    /** Creates a new empty ShoppingCart object. */
    public ShoppingCart(GenericDelegator delegator, String productStoreId, Locale locale, String currencyUom) {
        this(delegator, productStoreId, null, locale, currencyUom);
    }

    public GenericDelegator getDelegator() {
        if (delegator == null) {
            delegator = GenericDelegator.getGenericDelegator(delegatorName);
        }
        return delegator;
    }

    public String getProductStoreId() {
        return this.productStoreId;
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
    }

    public void setOrderName(String orderName) {
        this.orderName = orderName;
    }
    
    public String getOrderName() {
        return orderName;
    }
    
    public void setAttribute(String name, Object value) {
        this.attributes.put(name, value);
    }

    public Object getAttribute(String name) {
        return this.attributes.get(name);
    }

    public void removeOrderAttribute(String name) {
        this.orderAttributes.remove(name);
    }

    public void setOrderAttribute(String name, String value) {
        this.orderAttributes.put(name, value);
    }

    public String getOrderAttribute(String name) {
        return (String) this.orderAttributes.get(name);
    }

    public void setHoldOrder(boolean b) {
        this.holdOrder = b;
    }

    public boolean getHoldOrder() {
        return this.holdOrder;
    }

    public void setOrderDate(Timestamp t) {
        this.orderDate = t;
    }

    public Timestamp getOrderDate() {
        return this.orderDate;
    }
    
    /** Sets the currency for the cart. */
    public void setCurrency(LocalDispatcher dispatcher, String currencyUom) throws CartItemModifyException {
        if (isReadOnlyCart()) {
           throw new CartItemModifyException("Cart items cannot be changed");
        }
        String previousCurrency = this.currencyUom;
        this.currencyUom = currencyUom;
        if (!previousCurrency.equals(this.currencyUom)) {
            Iterator itemIterator = this.iterator();
            while (itemIterator.hasNext()) {
                ShoppingCartItem item = (ShoppingCartItem) itemIterator.next();
                item.updatePrice(dispatcher, this);
            }
        }
    }

    /** Get the current currency setting. */
    public String getCurrency() {
        if (this.currencyUom != null) {
            return this.currencyUom;
        } else {
            // uh oh, not good, should always be passed in on init, we can't really do anything without it, so throw an exception
            throw new IllegalStateException("The Currency UOM is not set in the shopping cart, this is not a valid state, it should always be passed in when the cart is created.");
        }
    }

    public Timestamp getCartCreatedTime() {
        return this.cartCreatedTs;
    }

    private GenericValue getSupplierProduct(String productId, double quantity, LocalDispatcher dispatcher) {
        GenericValue supplierProduct = null;
        Map params = UtilMisc.toMap("productId", productId,
                                    "partyId", this.getPartyId(),
                                    "currencyUomId", this.getCurrency(),
                                    "quantity", new Double(quantity));
        try {
            Map result = dispatcher.runSync("getSuppliersForProduct", params);
            List productSuppliers = (List)result.get("supplierProducts");
            if ((productSuppliers != null) && (productSuppliers.size() > 0)) {
                supplierProduct = (GenericValue) productSuppliers.get(0);
            }
            //} catch (GenericServiceException e) {
        } catch (Exception e) {
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
    public int addOrIncreaseItem(String productId, Double selectedAmountDbl, double quantity, Timestamp reservStart, Double reservLengthDbl, Double reservPersonsDbl, 
            Timestamp shipBeforeDate, Timestamp shipAfterDate, Map features, Map attributes, String prodCatalogId, 
            ProductConfigWrapper configWrapper, String itemType, String itemGroupNumber, String parentProductId, LocalDispatcher dispatcher) throws CartItemModifyException, ItemNotFoundException {
        if (isReadOnlyCart()) {
           throw new CartItemModifyException("Cart items cannot be changed");
        }
        
        double selectedAmount = selectedAmountDbl == null ? 0.0 : selectedAmountDbl.doubleValue();
        double reservLength = reservLengthDbl == null ? 0.0 : reservLengthDbl.doubleValue();
        double reservPersons = reservPersonsDbl == null ? 0.0 : reservPersonsDbl.doubleValue();
        
        ShoppingCart.ShoppingCartItemGroup itemGroup = this.getItemGroupByNumber(itemGroupNumber);
        GenericValue supplierProduct = null;
        // Check for existing cart item.
        for (int i = 0; i < this.cartLines.size(); i++) {
            ShoppingCartItem sci = (ShoppingCartItem) cartLines.get(i);

            
            if (sci.equals(productId, reservStart, reservLength, reservPersons, features, attributes, prodCatalogId, configWrapper, itemType, itemGroup, selectedAmount)) {
                double newQuantity = sci.getQuantity() + quantity;

                if (Debug.verboseOn()) Debug.logVerbose("Found a match for id " + productId + " on line " + i + ", updating quantity to " + newQuantity, module);
                sci.setQuantity(newQuantity, dispatcher, this);

                if (getOrderType().equals("PURCHASE_ORDER")) {
                    supplierProduct = getSupplierProduct(productId, newQuantity, dispatcher);
                    if (supplierProduct != null && supplierProduct.getDouble("lastPrice") != null) {
                        sci.setBasePrice(supplierProduct.getDouble("lastPrice").doubleValue());
                        sci.setName(ShoppingCartItem.getPurchaseOrderItemDescription(sci.getProduct(), supplierProduct, this.getLocale()));
                    } else {
                       throw new CartItemModifyException("SupplierProduct not found");
                    }
                 }
                return i;
            }
        }
        // Add the new item to the shopping cart if it wasn't found.
        if (getOrderType().equals("PURCHASE_ORDER")) {
            //GenericValue productSupplier = null;
            supplierProduct = getSupplierProduct(productId, quantity, dispatcher);
            if (supplierProduct != null || "_NA_".equals(this.getPartyId())) {
                 return this.addItem(0, ShoppingCartItem.makePurchaseOrderItem(new Integer(0), productId, selectedAmountDbl, quantity, features, attributes, prodCatalogId, configWrapper, itemType, itemGroup, dispatcher, this, supplierProduct, shipBeforeDate, shipAfterDate));
            } else {
                throw new CartItemModifyException("SupplierProduct not found");
            }
        } else {
            return this.addItem(0, ShoppingCartItem.makeItem(new Integer(0), productId, selectedAmountDbl, quantity, null, 
                    reservStart, reservLengthDbl, reservPersonsDbl, shipBeforeDate, shipAfterDate, 
                    features, attributes, prodCatalogId, configWrapper, itemType, itemGroup, dispatcher, 
                    this, Boolean.TRUE, Boolean.TRUE, parentProductId, Boolean.FALSE, Boolean.FALSE));
        }
    }

    /** Add a non-product item to the shopping cart.
     * @return the new item index
     * @throws CartItemModifyException
     */
    public int addNonProductItem(String itemType, String description, String categoryId, Double price, double quantity, 
            Map attributes, String prodCatalogId, String itemGroupNumber, LocalDispatcher dispatcher) throws CartItemModifyException {
        ShoppingCart.ShoppingCartItemGroup itemGroup = this.getItemGroupByNumber(itemGroupNumber);
        return this.addItem(0, ShoppingCartItem.makeItem(new Integer(0), itemType, description, categoryId, price, null, quantity, attributes, prodCatalogId, itemGroup, dispatcher, this, Boolean.TRUE));
    }

    /** Add an item to the shopping cart. */
    public int addItem(int index, ShoppingCartItem item) throws CartItemModifyException {
        if (isReadOnlyCart()) {
           throw new CartItemModifyException("Cart items cannot be changed");
        }
        if (!cartLines.contains(item)) {
            cartLines.add(index, item);
            return index;
        } else {
            return this.getItemIndex(item);
        }
    }

    /** Add an item to the shopping cart. */
    public int addItemToEnd(String productId, Double amount, double quantity, Double unitPrice, HashMap features, HashMap attributes, String prodCatalogId, String itemType, LocalDispatcher dispatcher, Boolean triggerExternalOps, Boolean triggerPriceRules) throws CartItemModifyException, ItemNotFoundException {
        return addItemToEnd(productId, amount, quantity, unitPrice, features, attributes, prodCatalogId, itemType, dispatcher, triggerExternalOps, triggerPriceRules, Boolean.FALSE, Boolean.FALSE);
    }

    /** Add an item to the shopping cart. */
    public int addItemToEnd(String productId, Double amount, double quantity, Double unitPrice, HashMap features, HashMap attributes, String prodCatalogId, String itemType, LocalDispatcher dispatcher, Boolean triggerExternalOps, Boolean triggerPriceRules, Boolean skipInventoryChecks, Boolean skipProductChecks) throws CartItemModifyException, ItemNotFoundException {
        return addItemToEnd(ShoppingCartItem.makeItem(null, productId, amount, quantity, unitPrice, null, null, null, null, null, features, attributes, prodCatalogId, null, itemType, null, dispatcher, this, triggerExternalOps, triggerPriceRules, null, skipInventoryChecks, skipProductChecks));
    }

    /** Add an item to the shopping cart. */
    public int addItemToEnd(ShoppingCartItem item) throws CartItemModifyException {
        if (isReadOnlyCart()) {
           throw new CartItemModifyException("Cart items cannot be changed");
        }
        if (!cartLines.contains(item)) {
            cartLines.add(item);
            return cartLines.size() - 1;
        } else {
            return this.getItemIndex(item);
        }
    }

    /** Get a ShoppingCartItem from the cart object. */
    public ShoppingCartItem findCartItem(String productId, Map features, Map attributes, String prodCatalogId, double selectedAmount) {
        // Check for existing cart item.
        for (int i = 0; i < this.cartLines.size(); i++) {
            ShoppingCartItem cartItem = (ShoppingCartItem) cartLines.get(i);

            if (cartItem.equals(productId, features, attributes, prodCatalogId, selectedAmount)) {
                return cartItem;
            }
        }
        return null;
    }

    /** Get all ShoppingCartItems from the cart object with the given productId. */
    public List findAllCartItems(String productId) {
        return this.findAllCartItems(productId, null);
    }
    /** Get all ShoppingCartItems from the cart object with the given productId and optional groupNumber to limit it to a specific item group */
    public List findAllCartItems(String productId, String groupNumber) {
        if (productId == null) return this.items();

        List itemsToReturn = FastList.newInstance();
        // Check for existing cart item.
        Iterator cartItemIter = this.cartLines.iterator();
        while (cartItemIter.hasNext()) {
            ShoppingCartItem cartItem = (ShoppingCartItem) cartItemIter.next();
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
    public List findAllCartItemsInCategory(String productCategoryId, String groupNumber) {
        if (productCategoryId == null) return this.items();

        GenericDelegator delegator = this.getDelegator();
        List itemsToReturn = FastList.newInstance();
        try {
            // Check for existing cart item
            Iterator cartItemIter = this.cartLines.iterator();
            while (cartItemIter.hasNext()) {
                ShoppingCartItem cartItem = (ShoppingCartItem) cartItemIter.next();
                //Debug.logInfo("Checking cartItem with product [" + cartItem.getProductId() + "] becuase that is in group [" + (cartItem.getItemGroup()==null ? "no group" : cartItem.getItemGroup().getGroupNumber()) + "]", module);
                
                if (UtilValidate.isNotEmpty(groupNumber) && !cartItem.isInItemGroup(groupNumber)) {
                    //Debug.logInfo("Not using cartItem with product [" + cartItem.getProductId() + "] becuase not in group [" + groupNumber + "]", module);
                    continue;
                }
                if (CategoryWorker.isProductInCategory(delegator, cartItem.getProductId(), productCategoryId)) {
                    itemsToReturn.add(cartItem);
                } else {
                    //Debug.logInfo("Not using cartItem with product [" + cartItem.getProductId() + "] becuase not in category [" + productCategoryId + "]", module);
                }
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, "Error getting cart items that are in a category: " + e.toString(), module);
        }
        //Debug.logInfo("Got [" + itemsToReturn.size() + "] cart items in category [" + productCategoryId + "] and item group [" + groupNumber + "]", module);
        return itemsToReturn;
    }

    /** Remove quantity 0 ShoppingCartItems from the cart object. */
    public void removeEmptyCartItems() {
        // Check for existing cart item.
        for (int i = 0; i < this.cartLines.size();) {
            ShoppingCartItem cartItem = (ShoppingCartItem) cartLines.get(i);

            if (cartItem.getQuantity() == 0.0) {
                this.clearItemShipInfo(cartItem);
                cartLines.remove(i);
            } else {
                i++;
            }
        }
    }

    // =============== some misc utility methods, mostly for dealing with lists of items =================
    public void removeExtraItems(List multipleItems, LocalDispatcher dispatcher, int maxItems) throws CartItemModifyException {
        // if 1 or 0 items, do nothing
        if (multipleItems.size() <= maxItems) return;
        
        // remove all except first <maxItems> in list from the cart, first because new cart items are added to the beginning...
        List localList = FastList.newInstance();
        localList.addAll(multipleItems);
        // the ones to keep...
        for (int i=0; i<maxItems; i++) localList.remove(0);
        Iterator localIter = localList.iterator();
        while (localIter.hasNext()) {
            ShoppingCartItem item = (ShoppingCartItem) localIter.next();
            this.removeCartItem(item, dispatcher);
        }
    }

    public static double getItemsTotalQuantity(List cartItems) {
        double totalQuantity = 0;
        Iterator localIter = cartItems.iterator();
        while (localIter.hasNext()) {
            ShoppingCartItem item = (ShoppingCartItem) localIter.next();
            totalQuantity += item.getQuantity();
        }
        return totalQuantity;
    }
    
    public static List getItemsProducts(List cartItems) {
        List productList = FastList.newInstance();
        Iterator localIter = cartItems.iterator();
        while (localIter.hasNext()) {
            ShoppingCartItem item = (ShoppingCartItem) localIter.next();
            GenericValue product = item.getProduct();
            if (product != null) {
                productList.add(product);
            }
        }
        return productList;
    }
    
    public void ensureItemsQuantity(List cartItems, LocalDispatcher dispatcher, double quantity) throws CartItemModifyException {
        Iterator localIter = cartItems.iterator();
        while (localIter.hasNext()) {
            ShoppingCartItem item = (ShoppingCartItem) localIter.next();
            if (item.getQuantity() != quantity) {
                item.setQuantity(quantity, dispatcher, this);
            }
        }
    }
    
    public double ensureItemsTotalQuantity(List cartItems, LocalDispatcher dispatcher, double quantity) throws CartItemModifyException {
        double quantityRemoved = 0;
        // go through the items and reduce quantityToKeep by the item quantities until it is 0, then remove the remaining...
        double quantityToKeep = quantity;
        Iterator localIter = cartItems.iterator();
        while (localIter.hasNext()) {
            ShoppingCartItem item = (ShoppingCartItem) localIter.next();
            
            if (quantityToKeep >= item.getQuantity()) {
                // quantityToKeep sufficient to keep it all... just reduce quantityToKeep and move on  
                quantityToKeep = quantityToKeep - item.getQuantity();
            } else {
                // there is more in this than we want to keep, so reduce the quantity, or remove altogether...
                if (quantityToKeep == 0) {
                    // nothing left to keep, just remove it...
                    quantityRemoved += item.getQuantity();
                    this.removeCartItem(item, dispatcher);
                } else {
                    // there is some to keep, so reduce quantity to quantityToKeep, at this point we know we'll take up all of the rest of the quantityToKeep
                    quantityRemoved += (item.getQuantity() - quantityToKeep);
                    item.setQuantity(quantityToKeep, dispatcher, this);
                    quantityToKeep = 0;
                }
            }
        }
        return quantityRemoved;
    }
    
    // ============== WorkEffort related methods ===============
    public boolean containAnyWorkEffortCartItems() {
        // Check for existing cart item.
        for (int i = 0; i < this.cartLines.size(); i++) {
            ShoppingCartItem cartItem = (ShoppingCartItem) cartLines.get(i);
            if (cartItem.getItemType().equals("RENTAL_ORDER_ITEM")) {  // create workeffort items?
                return true;
            }
        }
        return false;
    }

    public boolean containAllWorkEffortCartItems() {
        // Check for existing cart item.
        for (int i = 0; i < this.cartLines.size(); i++) {
            ShoppingCartItem cartItem = (ShoppingCartItem) cartLines.get(i);
            if (!cartItem.getItemType().equals("RENTAL_ORDER_ITEM")) { // not a item to create workefforts?
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
        for (int i = 0; i < this.cartLines.size(); i++) {
            ShoppingCartItem cartItem = (ShoppingCartItem) cartLines.get(i);
            GenericValue product = cartItem.getProduct();
            try {
                GenericValue productType = product.getRelatedOneCache("ProductType");
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
        return (ShoppingCartItem) cartLines.get(index);
    }

    public ShoppingCartItem findCartItem(String orderItemSeqId) {
        if (orderItemSeqId != null) {
            for (int i = 0; i < this.cartLines.size(); i++) {
                ShoppingCartItem cartItem = (ShoppingCartItem) cartLines.get(i);
                String itemSeqId = cartItem.getOrderItemSeqId();
                if (itemSeqId != null && orderItemSeqId.equals(itemSeqId)) {
                    return cartItem;
                }
            }
        }
        return null;
    }

    public void removeCartItem(ShoppingCartItem item, LocalDispatcher dispatcher) throws CartItemModifyException {
        if (item == null) return;
        this.removeCartItem(this.getItemIndex(item), dispatcher);
    }

    /** Remove an item from the cart object. */
    public void removeCartItem(int index, LocalDispatcher dispatcher) throws CartItemModifyException {
        if (isReadOnlyCart()) {
           throw new CartItemModifyException("Cart items cannot be changed");
        }
        if (index < 0) return;
        if (cartLines.size() <= index) return;
        ShoppingCartItem item = (ShoppingCartItem) cartLines.remove(index);

        // set quantity to 0 to trigger necessary events
        item.setQuantity(0.0, dispatcher, this);
    }

    /** Moves a line item to a differnt index. */
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
    public List items() {
        List result = FastList.newInstance();
        result.addAll(cartLines);
        return result;
    }

    /** Returns an iterator of cart items. */
    public Iterator iterator() {
        return cartLines.iterator();
    }
    
    public ShoppingCart.ShoppingCartItemGroup getItemGroupByNumber(String groupNumber) {
        if (UtilValidate.isEmpty(groupNumber)) return null;
        return (ShoppingCart.ShoppingCartItemGroup) this.itemGroupByNumberMap.get(groupNumber);
    }

    /** Creates a new Item Group and returns the groupNumber that represents it */
    public String addItemGroup(String groupName, String parentGroupNumber) {
        ShoppingCart.ShoppingCartItemGroup parentGroup = this.getItemGroupByNumber(parentGroupNumber);
        ShoppingCart.ShoppingCartItemGroup newGroup = new ShoppingCart.ShoppingCartItemGroup(this.nextGroupNumber, groupName, parentGroup);
        this.nextGroupNumber++;
        this.itemGroupByNumberMap.put(newGroup.getGroupNumber(), newGroup);
        return newGroup.getGroupNumber();
    }
    
    public List getCartItemsInNoGroup() {
        List cartItemList = FastList.newInstance();
        Iterator cartLineIter = this.cartLines.iterator();
        while (cartLineIter.hasNext()) {
            ShoppingCartItem cartItem = (ShoppingCartItem) cartLineIter.next();
            if (cartItem.getItemGroup() == null) {
                cartItemList.add(cartItem);
            }
        }
        return cartItemList;
    }
    
    public List getCartItemsInGroup(String groupNumber) {
        List cartItemList = FastList.newInstance();
        ShoppingCart.ShoppingCartItemGroup itemGroup = this.getItemGroupByNumber(groupNumber);
        if (itemGroup != null) {
            Iterator cartLineIter = this.cartLines.iterator();
            while (cartLineIter.hasNext()) {
                ShoppingCartItem cartItem = (ShoppingCartItem) cartLineIter.next();
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
            List cartItemList = this.getCartItemsInGroup(groupNumber);
            Iterator cartItemIter = cartItemList.iterator();
            while (cartItemIter.hasNext()) {
                ShoppingCartItem cartItem = (ShoppingCartItem) cartItemIter.next();
                cartItem.setItemGroup(null);
            }
            
            // if this is a parent of any set them to this group's parent (or null)
            Iterator itemGroupIter = this.itemGroupByNumberMap.values().iterator();
            while (itemGroupIter.hasNext()) {
                ShoppingCartItemGroup otherItemGroup = (ShoppingCartItemGroup) itemGroupIter.next();
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
            Iterator cartItemIter = this.iterator();
            while (cartItemIter.hasNext()) {
                ShoppingCartItem cartItem = (ShoppingCartItem) cartItemIter.next();
                cartItem.updatePrice(dispatcher, this);
            }

            // check all promo codes, remove on failed check
            Iterator promoCodeIter = this.productPromoCodes.iterator();
            while (promoCodeIter.hasNext()) {
                String promoCode = (String) promoCodeIter.next();
                String checkResult = ProductPromoWorker.checkCanUsePromoCode(promoCode, partyId, this.getDelegator());
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
    * @param idx
    * @return
    */
   public Timestamp getShipBeforeDate(int idx) {
       CartShipInfo csi = this.getShipInfo(idx);
       return csi.shipBeforeDate;
   }

   /**
    * Get ship before date for ship group 0
    * @return
    */
   public Timestamp getShipBeforeDate() {
       return this.getShipBeforeDate(0);
   }

   /**
    * Set ship after date for a particular ship group
    * @param idx
    * @param shipAfterDate
    */
   public void setShipAfterDate(int idx, Timestamp shipAfterDate) {
       CartShipInfo csi = this.getShipInfo(idx);
       csi.shipAfterDate  = shipAfterDate;
   }

   /**
    * Set ship after date for a particular ship group
    * @param shipAfterDate
    */
   public void setShipAfterDate(Timestamp shipAfterDate) {
       this.setShipAfterDate(0, shipAfterDate);
   }

   /**
    * Get ship after date for a particular ship group
    * @param idx
    * @return
    */
   public Timestamp getShipAfterDate(int idx) {
       CartShipInfo csi = this.getShipInfo(idx);
       return csi.shipAfterDate;
   }

   /**
    * Get ship after date for ship group 0
    * @return
    */
   public Timestamp getShipAfterDate() {
       return this.getShipAfterDate(0);
   }

   public void setDefaultShipBeforeDate(Timestamp defaultShipBeforeDate) {
      this.defaultShipBeforeDate = defaultShipBeforeDate;    
   }
   
   public Timestamp getDefaultShipBeforeDate() {
       return this.defaultShipBeforeDate;
   }
   
   public void setDefaultShipAfterDate(Timestamp defaultShipAfterDate) {
       this.defaultShipAfterDate = defaultShipAfterDate;
   }
   
   public Timestamp getDefaultShipAfterDate() {
       return this.defaultShipAfterDate;
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
        if (UtilValidate.isEmpty(this.orderPartyId)) this.orderPartyId = placingCustomerPartyId;
    }

    public String getBillToCustomerPartyId() {
        return this.billToCustomerPartyId != null ? this.billToCustomerPartyId : this.getPartyId();
    }

    public void setBillToCustomerPartyId(String billToCustomerPartyId) {
        this.billToCustomerPartyId = billToCustomerPartyId;
        if ((UtilValidate.isEmpty(this.orderPartyId)) && !(orderType.equals("PURCHASE_ORDER"))) {
            this.orderPartyId = billToCustomerPartyId;  // orderPartyId should be bill-to-customer when it is not a purchase order
        }
    }

    public String getShipToCustomerPartyId() {
        return this.shipToCustomerPartyId != null ? this.shipToCustomerPartyId : this.getPartyId();
    }

    public void setShipToCustomerPartyId(String shipToCustomerPartyId) {
        this.shipToCustomerPartyId = shipToCustomerPartyId;
        if (UtilValidate.isEmpty(this.orderPartyId)) this.orderPartyId = shipToCustomerPartyId;
    }

    public String getEndUserCustomerPartyId() {
        return this.endUserCustomerPartyId != null ? this.endUserCustomerPartyId : this.getPartyId();
    }

    public void setEndUserCustomerPartyId(String endUserCustomerPartyId) {
        this.endUserCustomerPartyId = endUserCustomerPartyId;
        if (UtilValidate.isEmpty(this.orderPartyId)) this.orderPartyId = endUserCustomerPartyId;
    }

//    protected String billFromVendorPartyId = null;
  //  protected String shipFromVendorPartyId = null;
    //protected String supplierAgentPartyId = null;

    public String getBillFromVendorPartyId() {
        return this.billFromVendorPartyId != null ? this.billFromVendorPartyId : this.getPartyId();
    }

    public void setBillFromVendorPartyId(String billFromVendorPartyId) {
        this.billFromVendorPartyId = billFromVendorPartyId;
        if ((UtilValidate.isEmpty(this.orderPartyId)) && (orderType.equals("PURCHASE_ORDER"))) {
            this.orderPartyId = billFromVendorPartyId;  // orderPartyId should be bill-from-vendor when it is a purchase order
        }

    }

    public String getShipFromVendorPartyId() {
        return this.shipFromVendorPartyId != null ? this.shipFromVendorPartyId : this.getPartyId();
    }

    public void setShipFromVendorPartyId(String shipFromVendorPartyId) {
        this.shipFromVendorPartyId = shipFromVendorPartyId;
        if (UtilValidate.isEmpty(this.orderPartyId)) this.orderPartyId = shipFromVendorPartyId;
    }

    public String getSupplierAgentPartyId() {
        return this.supplierAgentPartyId != null ? this.supplierAgentPartyId : this.getPartyId();
    }

    public void setSupplierAgentPartyId(String supplierAgentPartyId) {
        this.supplierAgentPartyId = supplierAgentPartyId;
        if (UtilValidate.isEmpty(this.orderPartyId)) this.orderPartyId = supplierAgentPartyId;
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
        this.lastListRestore = time;
    }

    public Timestamp getLastListRestore() {
        return this.lastListRestore;
    }

    public Double getPartyDaysSinceCreated(Timestamp nowTimestamp) {
        String partyId = this.getPartyId();
        if (UtilValidate.isEmpty(partyId)) {
            return null;
        }
        try {
            GenericValue party = this.getDelegator().findByPrimaryKeyCache("Party", UtilMisc.toMap("partyId", partyId));
            if (party == null) {
                return null;
            }
            Timestamp createdDate = party.getTimestamp("createdDate");
            if (createdDate == null) {
                return null;
            }
            double diffMillis = nowTimestamp.getTime() - createdDate.getTime();
            // millis per day: 1000.0 * 60.0 * 60.0 * 24.0 = 86400000.0
            return new Double((diffMillis) / 86400000.0);
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
        this.billingAccountAmt = 0.00;
        this.nextItemSeq = 1;

        this.agreementId = null;
        this.quoteId = null;

        this.defaultItemDeliveryDate = null;
        this.defaultItemComment = null;
        this.orderAdditionalEmails = null;

        //this.viewCartOnAdd = false;
        this.readOnlyCart = false;

        this.lastListRestore = null;
        this.autoSaveListId = null;

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

        // clear the additionalPartyRole Map
        Iterator it = this.additionalPartyRole.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry me = (Map.Entry) it.next();
            ((LinkedList) me.getValue()).clear();
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
                    org.ofbiz.order.shoppinglist.ShoppingListEvents.clearListInfo(this.getDelegator(), autoSaveListId);
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
            GenericValue pm = this.getDelegator().findByPrimaryKey("PaymentMethod", UtilMisc.toMap("paymentMethodId", paymentMethodId));
            if (pm != null) {
                return pm.getString("paymentMethodTypeId");
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
        return null;
    }

    /** Creates a CartPaymentInfo object */
    public CartPaymentInfo makePaymentInfo(String id, String refNum, Double amount) {
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

    /** Locates the index of an existing CartPaymentInfo object or -1 if none found */
    public int getPaymentInfoIndex(String id, String refNum) {
        CartPaymentInfo thisInf = this.makePaymentInfo(id, refNum, null);
        for (int i = 0; i < paymentInfo.size(); i++) {
            CartPaymentInfo inf = (CartPaymentInfo) paymentInfo.get(i);
            if (inf.compareTo(thisInf) == 0) {
                return i;
            }
        }
        return -1;
    }

    /** Returns the CartPaymentInfo objects which have matching fields */
    public List getPaymentInfos(boolean isPaymentMethod, boolean isPaymentMethodType, boolean hasRefNum) {
        List foundRecords = new LinkedList();
        Iterator i = paymentInfo.iterator();
        while (i.hasNext()) {
            CartPaymentInfo inf = (CartPaymentInfo) i.next();
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
        return (CartPaymentInfo) paymentInfo.get(index);
    }

    /** Locates an existing (or creates a new) CartPaymentInfo object */
    public CartPaymentInfo getPaymentInfo(String id, String refNum, String authCode, Double amount, boolean update) {
        CartPaymentInfo thisInf = this.makePaymentInfo(id, refNum, amount);
        Iterator i = paymentInfo.iterator();
        while (i.hasNext()) {
            CartPaymentInfo inf = (CartPaymentInfo) i.next();
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
    public CartPaymentInfo getPaymentInfo(String id, String refNum, String authCode, Double amount) {
        return this.getPaymentInfo(id, refNum, authCode, amount, false);
    }

    /** Locates an existing (or creates a new) CartPaymentInfo object */
    public CartPaymentInfo getPaymentInfo(String id) {
        return this.getPaymentInfo(id, null, null, null, false);
    }

    /** adds a payment method/payment method type */
    public CartPaymentInfo addPaymentAmount(String id, Double amount, String refNum, String authCode, boolean isSingleUse, boolean isPresent, boolean replace) {
        CartPaymentInfo inf = this.getPaymentInfo(id, refNum, authCode, amount, true);
        inf.singleUse = isSingleUse;
        if (replace) {
            paymentInfo.remove(inf);
        }
        paymentInfo.add(inf);

        return inf;
    }

    /** adds a payment method/payment method type */
    public CartPaymentInfo addPaymentAmount(String id, Double amount, boolean isSingleUse) {
        return this.addPaymentAmount(id, amount, null, null, isSingleUse, false, true);
    }

    /** adds a payment method/payment method type */
    public CartPaymentInfo addPaymentAmount(String id, double amount, boolean isSingleUse) {
        return this.addPaymentAmount(id, new Double(amount), isSingleUse);
    }

    /** adds a payment method/payment method type */
    public CartPaymentInfo addPaymentAmount(String id, Double amount) {
        return this.addPaymentAmount(id, amount, false);
    }

    /** adds a payment method/payment method type */
    public CartPaymentInfo addPaymentAmount(String id, double amount) {
        return this.addPaymentAmount(id, new Double(amount), false);
    }

    /** adds a payment method/payment method type */
    public CartPaymentInfo addPayment(String id) {
        return this.addPaymentAmount(id, null, false);
    }

    /** returns the payment method/payment method type amount */
    public Double getPaymentAmount(String id) {
        return this.getPaymentInfo(id).amount;
    }

    public void addPaymentRef(String id, String ref, String authCode) {
        this.getPaymentInfo(id).refNum[0] = ref;
        this.getPaymentInfo(id).refNum[1] = authCode;
    }

    public String getPaymentRef(String id) {
        Iterator i = paymentInfo.iterator();
        while (i.hasNext()) {
            CartPaymentInfo inf = (CartPaymentInfo) i.next();
            if (inf.paymentMethodId.equals(id) || inf.paymentMethodTypeId.equals(id)) {
                return inf.refNum[0];
            }
        }
        return null;
    }
    
    /** returns the total payment amounts */
    public double getPaymentTotal() {
        double total = 0.00;
        Iterator i = paymentInfo.iterator();
        while (i.hasNext()) {
            CartPaymentInfo inf = (CartPaymentInfo) i.next();
            if (inf.amount != null) {
                total += inf.amount.doubleValue();
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
    public void clearPaymentMethodsById(List paymentMethodIdsToRemove) {
        if (UtilValidate.isEmpty(paymentMethodIdsToRemove)) return;
        for (Iterator iter = paymentInfo.iterator(); iter.hasNext(); ) {
            CartPaymentInfo info = (CartPaymentInfo) iter.next();
            if (paymentMethodIdsToRemove.contains(info.paymentMethodId)) {
                iter.remove();
            }
        }
    }

    /** remove declined payment methods for an order from cart.  The idea is to call this after an attempted order is rejected */
    public void clearDeclinedPaymentMethodsFromOrder(GenericDelegator delegator, String orderId) {
        try {
            List declinedPaymentMethods = delegator.findByAnd("OrderPaymentPreference", UtilMisc.toMap("orderId", orderId, "statusId", "PAYMENT_DECLINED"));
            if (!UtilValidate.isEmpty(declinedPaymentMethods)) {
                List paymentMethodIdsToRemove = new ArrayList();
                for (Iterator iter = declinedPaymentMethods.iterator(); iter.hasNext(); ) {
                    GenericValue opp = (GenericValue) iter.next(); 
                    paymentMethodIdsToRemove.add(opp.getString("paymentMethodId"));
                }
                clearPaymentMethodsById(paymentMethodIdsToRemove);
            }
        } catch (GenericEntityException ex) {
            Debug.logError("Unable to remove declined payment methods from cart due to " + ex.getMessage(), module);
            return; 
        }
    }

    private void expireSingleUsePayments() {
        Timestamp now = UtilDateTime.nowTimestamp();
        Iterator i = paymentInfo.iterator();
        while (i.hasNext()) {
            CartPaymentInfo inf = (CartPaymentInfo) i.next();
            if (inf.paymentMethodId == null || !inf.singleUse) {
                continue;
            }

            GenericValue paymentMethod = null;
            try {
                paymentMethod = this.getDelegator().findByPrimaryKey("PaymentMethod", UtilMisc.toMap("paymentMethodId", inf.paymentMethodId));
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
    public List getPaymentMethodIds() {
        List pmi = new LinkedList();
        Iterator i = paymentInfo.iterator();
        while (i.hasNext()) {
            CartPaymentInfo inf = (CartPaymentInfo) i.next();
            if (inf.paymentMethodId != null) {
                pmi.add(inf.paymentMethodId);
            }
        }
        return pmi;
    }

    /** Returns the Payment Method Ids */
    public List getPaymentMethodTypeIds() {
        List pmt = FastList.newInstance();
        Iterator i = paymentInfo.iterator();
        while (i.hasNext()) {
            CartPaymentInfo inf = (CartPaymentInfo) i.next();
            if (inf.paymentMethodTypeId != null) {
                pmt.add(inf.paymentMethodTypeId);
            }
        }
        return pmt;
    }

    /** Returns a list of PaymentMethod value objects selected in the cart */
    public List getPaymentMethods() {
        List methods = FastList.newInstance();
        if (paymentInfo != null && paymentInfo.size() > 0) {
            Iterator paymentMethodIdIter = getPaymentMethodIds().iterator();
            while (paymentMethodIdIter.hasNext()) {
                String paymentMethodId = (String) paymentMethodIdIter.next();
                try {
                    GenericValue paymentMethod = this.getDelegator().findByPrimaryKeyCache("PaymentMethod", UtilMisc.toMap("paymentMethodId", paymentMethodId));
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
    public List getPaymentMethodTypes() {
        List types = new LinkedList();
        if (paymentInfo != null && paymentInfo.size() > 0) {
            Iterator i = getPaymentMethodTypeIds().iterator();
            while (i.hasNext()) {
                String id = (String) i.next();
                try {
                    types.add(this.getDelegator().findByPrimaryKeyCache("PaymentMethodType", UtilMisc.toMap("paymentMethodTypeId", id)));
                } catch (GenericEntityException e) {
                    Debug.logError(e, "Unable to get payment method type from the database", module);
                }
            }
        }

        return types;
    }

    public List getCreditCards() {
        List paymentMethods = this.getPaymentMethods();
        List creditCards = new LinkedList();
        if (paymentMethods != null) {
            Iterator i = paymentMethods.iterator();
            while (i.hasNext()) {
                GenericValue pm = (GenericValue) i.next();
                if ("CREDIT_CARD".equals(pm.getString("paymentMethodTypeId"))) {
                    try {
                        GenericValue cc = pm.getRelatedOne("CreditCard");
                        creditCards.add(cc);
                    } catch (GenericEntityException e) {
                        Debug.logError(e, "Unable to get credit card record from payment method : " + pm, module);
                    }
                }
            }
        }

        return creditCards;
    }

    public List getGiftCards() {
        List paymentMethods = this.getPaymentMethods();
        List giftCards = new LinkedList();
        if (paymentMethods != null) {
            Iterator i = paymentMethods.iterator();
            while (i.hasNext()) {
                GenericValue pm = (GenericValue) i.next();
                if ("GIFT_CARD".equals(pm.getString("paymentMethodTypeId"))) {
                    try {
                        GenericValue gc = pm.getRelatedOne("GiftCard");
                        giftCards.add(gc);
                    } catch (GenericEntityException e) {
                        Debug.logError(e, "Unable to get gift card record from payment method : " + pm, module);
                    }
                }
            }
        }

        return giftCards;
    }

    /* determines if the id supplied is a payment method or not by searching in the entity engine */
    public boolean isPaymentMethodType(String id) {
        GenericValue paymentMethodType = null;
        try {
            paymentMethodType = this.getDelegator().findByPrimaryKeyCache("PaymentMethodType", UtilMisc.toMap("paymentMethodTypeId", id));
        } catch (GenericEntityException e) {
            Debug.logInfo(e, "Problems getting PaymentMethodType", module);
        }
        if (paymentMethodType == null) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Returns ProductStoreFinActSetting based on cart's productStoreId and FinAccountHelper's defined giftCertFinAcctTypeId
     * @param delegator
     * @return
     * @throws GenericEntityException
     */
    public GenericValue getGiftCertSettingFromStore(GenericDelegator delegator) throws GenericEntityException {
        return delegator.findByPrimaryKeyCache("ProductStoreFinActSetting", UtilMisc.toMap("productStoreId", getProductStoreId(), "finAccountTypeId", FinAccountHelper.giftCertFinAccountTypeId));
    }
    
    /**
     * Determines whether pin numbers are required for gift cards, based on ProductStoreFinActSetting.  Default to true.
     * @return
     */
    public boolean isPinRequiredForGC(GenericDelegator delegator) {
        try {
            GenericValue giftCertSettings = getGiftCertSettingFromStore(delegator);
            if (giftCertSettings != null) {
                if ("Y".equals(giftCertSettings.getString("requirePinCode"))) {
                    return true;
                } else {
                    return false;
                }
            } else {
                Debug.logWarning("No product store gift certificate settings found for store [" + getProductStoreId() + "]", module);
                return true;
            }
        } catch (GenericEntityException ex) {
            Debug.logError("Error checking if store requires pin number for GC: " + ex.getMessage(), module);
            return true;
        }
    }
    
    /**
     * Returns whether the cart should validate gift cards against FinAccount (ie, internal gift certificates).  Defaults to false.
     * @param delegator
     * @return
     */
    public boolean isValidateGCFinAccount(GenericDelegator delegator) {
        try {
            GenericValue giftCertSettings = getGiftCertSettingFromStore(delegator);
            if (giftCertSettings != null) {
                if ("Y".equals(giftCertSettings.getString("validateGCFinAcct"))) {
                    return true;
                } else {
                    return false;
                }
            } else {
                Debug.logWarning("No product store gift certificate settings found for store [" + getProductStoreId() + "]", module);
                return false;
            }
        } catch (GenericEntityException ex) {
            Debug.logError("Error checking if store requires pin number for GC: " + ex.getMessage(), module);
            return false;
        }
    }
    
    // =======================================================================
    // Billing Accounts
    // =======================================================================

    /** Sets the billing account id string. */
    public void setBillingAccount(String billingAccountId, double amount) {
        this.billingAccountId = billingAccountId;
        this.billingAccountAmt = amount;
    }

    /** Returns the billing message string. */
    public String getBillingAccountId() {
        return this.billingAccountId;
    }

    /** Returns the amount to be billed to the billing account.*/
    public double getBillingAccountAmount() {
        return this.billingAccountAmt;
    }

    // =======================================================================
    // Shipping Charges
    // =======================================================================

    /** Returns the order level shipping amount */
    public double getOrderShipping() {
        return OrderReadHelper.calcOrderAdjustments(this.getAdjustments(), this.getSubTotal(), false, false, true);
    }

    // ----------------------------------------
    // Ship Group Methods
    // ----------------------------------------

    public int addShipInfo() {
        shipInfo.add(new CartShipInfo());
        return (shipInfo.size() - 1);
    }

    public List getShipGroups() {
        return this.shipInfo;
    }

    public Map getShipGroups(ShoppingCartItem item) {
        Map shipGroups = new LinkedMap();
        if (item != null) {
            for (int i = 0; i < this.shipInfo.size(); i++) {
                CartShipInfo csi = (CartShipInfo) shipInfo.get(i);
                CartShipInfo.CartShipItemInfo csii = (CartShipInfo.CartShipItemInfo) csi.shipItemInfo.get(item);
                if (csii != null) {
                    if (this.checkShipItemInfo(csi, csii)) {
                        shipGroups.put(new Integer(i), new Double(csii.quantity));
                    }
                }
            }
        }
        return shipGroups;
    }

    public Map getShipGroups(int itemIndex) {
        return this.getShipGroups(this.findCartItem(itemIndex));
    }

    public CartShipInfo getShipInfo(int idx) {
        if (idx == -1 ) {
            return null;
        }

        if (shipInfo.size() == 0) {
            shipInfo.add(new CartShipInfo());
        }

        return (CartShipInfo) shipInfo.get(idx);
    }

    public int getShipGroupSize() {
        return this.shipInfo.size();
    }

    /** Returns the ShoppingCartItem (key) and quantity (value) associated with the ship group */
    public Map getShipGroupItems(int idx) {
        CartShipInfo csi = this.getShipInfo(idx);
        Map qtyMap = new HashMap();
        Iterator i = csi.shipItemInfo.keySet().iterator();
        while (i.hasNext()) {
            ShoppingCartItem item = (ShoppingCartItem) i.next();
            CartShipInfo.CartShipItemInfo csii = (CartShipInfo.CartShipItemInfo) csi.shipItemInfo.get(item);
            qtyMap.put(item, new Double(csii.quantity));
        }
        return qtyMap;
    }

    public void clearItemShipInfo(ShoppingCartItem item) {
        for (int i = 0; i < shipInfo.size(); i++) {
            CartShipInfo csi = this.getShipInfo(i);
            csi.shipItemInfo.remove(item);
        }
        this.cleanUpShipGroups();
    }

    public void setItemShipGroupEstimate(double amount, int idx) {
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
        Map shipGroups = this.getShipGroups(item);
        
        if ((shipGroups != null) && (shipGroups.keySet() != null)) {
            for (Iterator shipGroupKeys = shipGroups.keySet().iterator(); shipGroupKeys.hasNext(); ) {
                Integer shipGroup = (Integer) shipGroupKeys.next();
                CartShipInfo cartShipInfo = this.getShipInfo(shipGroup.intValue());
                
                cartShipInfo.resetShipAfterDateIfBefore(item.getShipAfterDate());
                cartShipInfo.resetShipBeforeDateIfAfter(item.getShipBeforeDate());
            }
        }
    }
    
    public double getItemShipGroupEstimate(int idx) {
        CartShipInfo csi = this.getShipInfo(idx);
        return csi.shipEstimate;
    }

    public void setItemShipGroupQty(int itemIndex, double quantity, int idx) {
        this.setItemShipGroupQty(this.findCartItem(itemIndex), itemIndex, quantity, idx);
    }

    public void setItemShipGroupQty(ShoppingCartItem item, double quantity, int idx) {
        this.setItemShipGroupQty(item, this.getItemIndex(item), quantity, idx);
    }

    public void setItemShipGroupQty(ShoppingCartItem item, int itemIndex, double quantity, int idx) {
        if (itemIndex > -1) {
            CartShipInfo csi = this.getShipInfo(idx);

            // never set less than zero
            if (quantity < 0) {
                quantity = 0;
            }

            // never set more than quantity ordered
            if (quantity > item.getQuantity()) {
                quantity = item.getQuantity();
            }
            
            // re-set the ship group's before and after dates based on the item's
            csi.resetShipBeforeDateIfAfter(item.getShipBeforeDate());
            csi.resetShipAfterDateIfBefore(item.getShipAfterDate());
            
            CartShipInfo.CartShipItemInfo csii = csi.setItemInfo(item, quantity);
            this.checkShipItemInfo(csi, csii);
        }
    }

    public double getItemShipGroupQty(ShoppingCartItem item, int idx) {
        if (item != null) {
            CartShipInfo csi = this.getShipInfo(idx);
            CartShipInfo.CartShipItemInfo csii = (CartShipInfo.CartShipItemInfo) csi.shipItemInfo.get(item);
            if (csii != null) {
                return csii.quantity;
            }
        }
        return 0;
    }

    public double getItemShipGroupQty(int itemIndex, int idx) {
        return this.getItemShipGroupQty(this.findCartItem(itemIndex), idx);
    }

    public void positionItemToGroup(int itemIndex, double quantity, int fromIndex, int toIndex, boolean clearEmptyGroups) {
        this.positionItemToGroup(this.findCartItem(itemIndex), quantity, fromIndex, toIndex, clearEmptyGroups);
    }

    public void positionItemToGroup(ShoppingCartItem item, double quantity, int fromIndex, int toIndex, boolean clearEmptyGroups) {
        if (fromIndex == toIndex || quantity <= 0) {
            // do nothing
            return;
        }

        // get the ship groups; create the TO group if needed
        CartShipInfo fromGroup = this.getShipInfo(fromIndex);
        CartShipInfo toGroup = null;
        if (toIndex == -1) {
            toGroup = new CartShipInfo();
            this.shipInfo.add(toGroup);
            toIndex = this.shipInfo.size() - 1;
        } else {
            toGroup = this.getShipInfo(toIndex);
        }

        // adjust the quantities
        if (fromGroup != null && toGroup != null) {
            double fromQty = this.getItemShipGroupQty(item, fromIndex);
            double toQty = this.getItemShipGroupQty(item, toIndex);
            if (fromQty > 0) {
                if (quantity > fromQty) {
                    quantity = fromQty;
                }
                fromQty -= quantity;
                toQty += quantity;
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
        if (csii.quantity == 0 || csii.item.getQuantity() == 0) {
            csi.shipItemInfo.remove(csii.item);
            return false;
        }
        return true;
    }

    protected void cleanUpShipGroups() {
        for (int i = 0; i < this.shipInfo.size(); i++) {
            CartShipInfo csi = this.getShipInfo(i);
            Iterator si = csi.shipItemInfo.keySet().iterator();
            while (si.hasNext()) {
                ShoppingCartItem item = (ShoppingCartItem) si.next();
                if (item.getQuantity() == 0.0) {
                    si.remove();
                }
            }
            if (csi.shipItemInfo.size() == 0) {
                this.shipInfo.remove(csi);
            }
        }
    }

    /** Sets the shipping contact mech id. */
    public void setShippingContactMechId(int idx, String shippingContactMechId) {
        CartShipInfo csi = this.getShipInfo(idx);
        csi.contactMechId = shippingContactMechId;
    }

    public void setShippingContactMechId(String shippingContactMechId) {
        this.setShippingContactMechId(0, shippingContactMechId);
    }

    /** Returns the shipping contact mech id. */
    public String getShippingContactMechId(int idx) {
        CartShipInfo csi = this.getShipInfo(idx);
        return csi.contactMechId;
    }

    public String getShippingContactMechId() {
        return this.getShippingContactMechId(0);
    }

    /** Sets the shipment method type. */
    public void setShipmentMethodTypeId(int idx, String shipmentMethodTypeId) {
        CartShipInfo csi = this.getShipInfo(idx);
        csi.shipmentMethodTypeId = shipmentMethodTypeId;
    }

    public void setShipmentMethodTypeId(String shipmentMethodTypeId) {
        this.setShipmentMethodTypeId(0, shipmentMethodTypeId);
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
                return this.getDelegator().findByPrimaryKey("ShipmentMethodType",
                        UtilMisc.toMap("shipmentMethodTypeId", shipmentMethodTypeId));
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

    /** Sets the shipping instructions. */
    public void setShippingInstructions(int idx, String shippingInstructions) {
        CartShipInfo csi = this.getShipInfo(idx);
        csi.shippingInstructions = shippingInstructions;
    }

    public void setShippingInstructions(String shippingInstructions) {
        this.setShippingInstructions(0, shippingInstructions);
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
        csi.maySplit = maySplit.booleanValue() ? "Y" : "N";
    }

    public void setMaySplit(Boolean maySplit) {
        this.setMaySplit(0, maySplit);
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

    public void setGiftMessage(String giftMessage) {
        this.setGiftMessage(0, giftMessage);
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
        csi.isGift = isGift.booleanValue() ? "Y" : "N";
    }

    public void setIsGift(Boolean isGift) {
        this.setIsGift(0, isGift);
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

    public void setCarrierPartyId(String carrierPartyId) {
        this.setCarrierPartyId(0, carrierPartyId);
    }

    public String getCarrierPartyId(int idx) {
        CartShipInfo csi = this.getShipInfo(idx);
        return csi.carrierPartyId;
    }

    public String getCarrierPartyId() {
        return this.getCarrierPartyId(0);
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
                return getDelegator().findByPrimaryKey("PostalAddress", UtilMisc.toMap("contactMechId", this.getShippingContactMechId(idx)));
            } catch (GenericEntityException e) {
                Debug.logWarning(e.toString(), module);
                return null;
            }
        } else {
            return null;
        }
    }

    public GenericValue getShippingAddress() {
        return this.getShippingAddress(0);
    }

    // ----------------------------------------
    // internal/public notes
    // ----------------------------------------

    public List getInternalOrderNotes() {
        return this.internalOrderNotes;
    }
    
    public List getOrderNotes() {
        return this.orderNotes;
    }

    public void addInternalOrderNote(String note) {
        this.internalOrderNotes.add(note);
    }

    public void addOrderNote(String note) {
        this.orderNotes.add(note);
    }

    // Preset with default values some of the checkout options to get a quicker checkout process.
    public void setDefaultCheckoutOptions(LocalDispatcher dispatcher) {
        // skip the add party screen
        this.setAttribute("addpty", "Y");
        if (getOrderType().equals("SALES_ORDER")) {
            // checkout options for sales orders
            // set as the default shipping location the first from the list of available shipping locations
            if (this.getPartyId() != null && !this.getPartyId().equals("_NA_")) {
                try {
                    GenericValue orderParty = delegator.findByPrimaryKey("Party", UtilMisc.toMap("partyId", this.getPartyId()));
                    Collection shippingContactMechList = ContactHelper.getContactMech(orderParty, "SHIPPING_LOCATION", "POSTAL_ADDRESS", false);
                    if (shippingContactMechList != null && shippingContactMechList.size() > 0) {
                        GenericValue shippingContactMech = (GenericValue)(shippingContactMechList.iterator()).next();
                        this.setShippingContactMechId(shippingContactMech.getString("contactMechId"));
                    }
                } catch (GenericEntityException e) {
                    Debug.logError(e, "Error setting shippingContactMechId in setDefaultCheckoutOptions() method.", module);
                }
            }
            // set the default shipment method
            ShippingEstimateWrapper shipEstimateWrapper = org.ofbiz.order.shoppingcart.shipping.ShippingEstimateWrapper.getWrapper(dispatcher, this, 0);
            GenericValue carrierShipmentMethod = EntityUtil.getFirst(shipEstimateWrapper.getShippingMethods());
            if (carrierShipmentMethod != null) {
                this.setShipmentMethodTypeId(carrierShipmentMethod.getString("shipmentMethodTypeId"));
                this.setCarrierPartyId(carrierShipmentMethod.getString("partyId"));
            }
        } else {
            // checkout options for purchase orders
            // TODO: should we select a default agreement? For now we don't do this.
            // skip the order terms selection step
            this.setOrderTermSet(true);
            // set as the default shipping location the first from the list of available shipping locations
            String companyId = this.getBillToCustomerPartyId();
            if (companyId != null) {
                try {
                    List facilities = delegator.findByAndCache("Facility", UtilMisc.toMap("ownerPartyId", companyId));
                    // TODO: improve code here to select the best ship to facility (from requirements?);
                    //       for now, we consider the first one
                    GenericValue facility = EntityUtil.getFirst(facilities);
                    if (facility != null) {
                        List facilityContactMechValueMaps = ContactMechWorker.getFacilityContactMechValueMaps(delegator, facility.getString("facilityId"), false, null);
                        if (facilityContactMechValueMaps != null) {
                            Iterator facilityContactMechValueMapsIt = facilityContactMechValueMaps.iterator();
                            while (facilityContactMechValueMapsIt.hasNext()) {
                                Map facilityContactMechValueMap = (Map)facilityContactMechValueMapsIt.next();
                                if (facilityContactMechValueMap.get("postalAddress") != null) {
                                    GenericValue postalAddress = (GenericValue)facilityContactMechValueMap.get("postalAddress");
                                    this.setShippingContactMechId(0, postalAddress.getString("contactMechId"));
                                    break;
                                }
                            }
                        }
                    }
                } catch (GenericEntityException e) {
                    Debug.logError(e, "Error setting shippingContactMechId in setDefaultCheckoutOptions() method.", module);
                }
            }
            // shipping options
            this.setShipmentMethodTypeId(0, "NO_SHIPPING");
            this.setCarrierPartyId(0, "_NA_");
            this.setShippingInstructions(0, "");
            this.setGiftMessage(0, "");
            this.setMaySplit(0, Boolean.TRUE);
            this.setIsGift(0, Boolean.FALSE);
            //this.setInternalCode(internalCode);
        }
    }

    // Returns the tax amount for a ship group. */
    public double getTotalSalesTax(int shipGroup) {
        CartShipInfo csi = this.getShipInfo(shipGroup);
        return csi.getTotalTax(this);
    }

    /** Returns the tax amount from the cart object. */
    public double getTotalSalesTax() {
        double totalTax = 0.00;
        for (int i = 0; i < shipInfo.size(); i++) {
            CartShipInfo csi = this.getShipInfo(i);
            totalTax += csi.getTotalTax(this);
        }
        return totalTax;
    }

    /** Returns the shipping amount from the cart object. */
    public double getTotalShipping() {
        double tempShipping = 0.0;

        Iterator shipIter = this.shipInfo.iterator();
        while (shipIter.hasNext()) {
            CartShipInfo csi = (CartShipInfo) shipIter.next();
            tempShipping += csi.shipEstimate;

        }

        return tempShipping;
    }

    /** Returns the item-total in the cart (not including discount/tax/shipping). */
    public double getItemTotal() {
        double itemTotal = 0.00;
        Iterator i = iterator();

        while (i.hasNext()) {
            itemTotal += ((ShoppingCartItem) i.next()).getBasePrice();
        }
        return itemTotal;
    }

    /** Returns the sub-total in the cart (item-total - discount). */
    public double getSubTotal() {
        double itemsTotal = 0.00;
        Iterator i = iterator();

        while (i.hasNext()) {
            itemsTotal += ((ShoppingCartItem) i.next()).getItemSubTotal();
        }
        return itemsTotal;
    }

    /** Returns the total from the cart, including tax/shipping. */
    public double getGrandTotal() {
        // sales tax and shipping are not stored as adjustments but rather as part of the ship group
        // Debug.logInfo("Subtotal:" + this.getSubTotal() + " Shipping:" + this.getTotalShipping() + "SalesTax: "+ this.getTotalSalesTax() + " others: " + this.getOrderOtherAdjustmentTotal(),module);
        return this.getSubTotal() + this.getTotalShipping() + this.getTotalSalesTax() + this.getOrderOtherAdjustmentTotal();
    }

    public double getDisplaySubTotal() {
        double itemsTotal = 0.00;
        Iterator i = iterator();
        while (i.hasNext()) {
            itemsTotal += ((ShoppingCartItem) i.next()).getDisplayItemSubTotal();
        }
        return itemsTotal;
    }

    public double getDisplayRecurringSubTotal() {
        double itemsTotal = 0.00;
        Iterator i = iterator();
        while (i.hasNext()) {
            itemsTotal += ((ShoppingCartItem) i.next()).getDisplayItemRecurringSubTotal();
        }
        return itemsTotal;
    }

    /** Returns the total from the cart, including tax/shipping. */
    public double getDisplayGrandTotal() {
        return this.getDisplaySubTotal() + this.getTotalShipping() + this.getTotalSalesTax() + this.getOrderOtherAdjustmentTotal();
    }

    public double getOrderOtherAdjustmentTotal() {
        return OrderReadHelper.calcOrderAdjustmentsBd(this.getAdjustments(), new BigDecimal(this.getSubTotal()), true, false, false).doubleValue();
    }

    /** Returns the sub-total in the cart (item-total - discount). */
    public double getSubTotalForPromotions() {
        double itemsTotal = 0.00;
        Iterator i = iterator();

        while (i.hasNext()) {
            ShoppingCartItem cartItem = (ShoppingCartItem) i.next();
            GenericValue product = cartItem.getProduct();
            if (product != null && "N".equals(product.getString("includeInPromotions"))) {
                // don't include in total if this is the case...
                continue;
            }
            itemsTotal += cartItem.getItemSubTotal();
        }
        return itemsTotal;
    }

    /**
     * Get the total payment amount by payment type.  Specify null to get amount
     * over all types.
     */
    public double getOrderPaymentPreferenceTotalByType(String paymentMethodTypeId) {
        double total = 0.0;
        String thisPaymentMethodTypeId = null;
        for (Iterator iter = paymentInfo.iterator(); iter.hasNext(); ) {
            CartPaymentInfo payment = (CartPaymentInfo) iter.next();
            if (payment.amount == null) continue;
            if (payment.paymentMethodId != null) {
                try {
                    // need to determine the payment method type from the payment method
                    GenericValue paymentMethod = this.getDelegator().findByPrimaryKeyCache("PaymentMethod", UtilMisc.toMap("paymentMethodId", payment.paymentMethodId));
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
                total += payment.amount.doubleValue();
            }
        }
        return total;
    }

    public double getCreditCardPaymentPreferenceTotal() {
        return getOrderPaymentPreferenceTotalByType("CREDIT_CARD");
    }

    public double getBillingAccountPaymentPreferenceTotal() {
        return getOrderPaymentPreferenceTotalByType("EXT_BILLACT");
    }

    public double getGiftCardPaymentPreferenceTotal() {
        return getOrderPaymentPreferenceTotalByType("GIFT_CARD");
    }

    /** Add a contact mech to this purpose; the contactMechPurposeTypeId is required */
    public void addContactMech(String contactMechPurposeTypeId, String contactMechId) {
        if (contactMechPurposeTypeId == null) throw new IllegalArgumentException("You must specify a contactMechPurposeTypeId to add a ContactMech");
        contactMechIdsMap.put(contactMechPurposeTypeId, contactMechId);
    }

    /** Get the contactMechId for this cart given the contactMechPurposeTypeId */
    public String getContactMech(String contactMechPurposeTypeId) {
        return (String) contactMechIdsMap.get(contactMechPurposeTypeId);
    }

    /** Remove the contactMechId from this cart given the contactMechPurposeTypeId */
    public String removeContactMech(String contactMechPurposeTypeId) {
        return (String) contactMechIdsMap.remove(contactMechPurposeTypeId);
    }

    public Map getOrderContactMechIds() {
        return this.contactMechIdsMap;
    }

    /** Get a List of adjustments on the order (ie cart) */
    public List getAdjustments() {
        return adjustments;
    }

    /** Add an adjustment to the order; don't worry about setting the orderId, orderItemSeqId or orderAdjustmentId; they will be set when the order is created */
    public int addAdjustment(GenericValue adjustment) {
        adjustments.add(adjustment);
        return adjustments.indexOf(adjustment);
    }

    public void removeAdjustment(int index) {
        adjustments.remove(index);
    }

    /** Get a List of orderTerms on the order (ie cart) */
    public List getOrderTerms() {
        return orderTerms;
    }

    /** Add an orderTerm to the order */
    public int addOrderTerm(String termTypeId,Double termValue,Long termDays) {
        return addOrderTerm(termTypeId, termValue, termDays, null);
    }
    
    /** Add an orderTerm to the order */
    public int addOrderTerm(String termTypeId,Double termValue,Long termDays, String description) {
        GenericValue orderTerm = GenericValue.create(delegator.getModelEntity("OrderTerm"));
        orderTerm.put("termTypeId", termTypeId);
        orderTerm.put("termValue", termValue);
        orderTerm.put("termDays", termDays);
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

    public boolean isOrderTermSet(){
       return orderTermSet;
    }

    public void setOrderTermSet(boolean orderTermSet){
         this.orderTermSet = orderTermSet;
     }

    public boolean isReadOnlyCart(){
       return readOnlyCart;
    }

    public void setReadOnlyCart(boolean readOnlyCart){
         this.readOnlyCart = readOnlyCart;
     }

    /** go through the order adjustments and remove all adjustments with the given type */
    public void removeAdjustmentByType(String orderAdjustmentTypeId) {
        if (orderAdjustmentTypeId == null) return;

        // make a list of adjustment lists including the cart adjustments and the cartItem adjustments for each item
        List adjsLists = new LinkedList();

        if (this.getAdjustments() != null) {
            adjsLists.add(this.getAdjustments());
        }
        Iterator cartIterator = this.iterator();

        while (cartIterator.hasNext()) {
            ShoppingCartItem item = (ShoppingCartItem) cartIterator.next();

            if (item.getAdjustments() != null) {
                adjsLists.add(item.getAdjustments());
            }
        }

        Iterator adjsListsIter = adjsLists.iterator();

        while (adjsListsIter.hasNext()) {
            List adjs = (List) adjsListsIter.next();

            if (adjs != null) {
                for (int i = 0; i < adjs.size();) {
                    GenericValue orderAdjustment = (GenericValue) adjs.get(i);

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
    public double getTotalWeight() {
        double weight = 0.0;
        Iterator i = iterator();

        while (i.hasNext()) {
            ShoppingCartItem item = (ShoppingCartItem) i.next();

            weight += (item.getWeight() * item.getQuantity());
        }
        return weight;
    }

    /** Returns the total quantity in the cart. */
    public double getTotalQuantity() {
        double count = 0.0;
        Iterator i = iterator();

        while (i.hasNext()) {
            count += ((ShoppingCartItem) i.next()).getQuantity();
        }
        return count;
    }

    /** Returns the SHIPPABLE item-total in the cart for a specific ship group. */
    public double getShippableTotal(int idx) {
        CartShipInfo info = this.getShipInfo(idx);
        double itemTotal = 0.0;

        Iterator i = info.shipItemInfo.keySet().iterator();
        while (i.hasNext()) {
            ShoppingCartItem item = (ShoppingCartItem) i.next();
            CartShipInfo.CartShipItemInfo csii = (CartShipInfo.CartShipItemInfo) info.shipItemInfo.get(item);
            if (csii != null && csii.quantity > 0) {
                if (item.shippingApplies()) {
                    itemTotal += item.getItemSubTotal(csii.quantity);
                }
            }
        }

        return itemTotal;
    }

    /** Returns the total SHIPPABLE quantity in the cart for a specific ship group. */
    public double getShippableQuantity(int idx) {
        CartShipInfo info = this.getShipInfo(idx);
        double count = 0.0;

        Iterator i = info.shipItemInfo.keySet().iterator();
        while (i.hasNext()) {
            ShoppingCartItem item = (ShoppingCartItem) i.next();
            CartShipInfo.CartShipItemInfo csii = (CartShipInfo.CartShipItemInfo) info.shipItemInfo.get(item);
            if (csii != null && csii.quantity > 0) {
                if (item.shippingApplies()) {
                    count += csii.quantity;
                }
            }
        }

        return count;
    }

    /** Returns the total SHIPPABLE weight in the cart for a specific ship group. */
    public double getShippableWeight(int idx) {
        CartShipInfo info = this.getShipInfo(idx);
        double weight = 0.0;

        Iterator i = info.shipItemInfo.keySet().iterator();
        while (i.hasNext()) {
            ShoppingCartItem item = (ShoppingCartItem) i.next();
            CartShipInfo.CartShipItemInfo csii = (CartShipInfo.CartShipItemInfo) info.shipItemInfo.get(item);
            if (csii != null && csii.quantity > 0) {
                if (item.shippingApplies()) {
                    weight += (item.getWeight() * csii.quantity);
                }
            }
        }

        return weight;
    }

    /** Returns a List of shippable item's size for a specific ship group. */
    public List getShippableSizes(int idx) {
        CartShipInfo info = this.getShipInfo(idx);
        List shippableSizes = new LinkedList();

        Iterator i = info.shipItemInfo.keySet().iterator();
        while (i.hasNext()) {
            ShoppingCartItem item = (ShoppingCartItem) i.next();
            CartShipInfo.CartShipItemInfo csii = (CartShipInfo.CartShipItemInfo) info.shipItemInfo.get(item);
            if (csii != null && csii.quantity > 0) {
                if (item.shippingApplies()) {
                    shippableSizes.add(new Double(item.getSize()));
                }
            }
        }

        return shippableSizes;
    }

    /** Returns a List of shippable item info (quantity, size, weight) for a specific ship group */
    public List getShippableItemInfo(int idx) {
        CartShipInfo info = this.getShipInfo(idx);
        List itemInfos = new LinkedList();

        Iterator i = info.shipItemInfo.keySet().iterator();
        while (i.hasNext()) {
            ShoppingCartItem item = (ShoppingCartItem) i.next();
            CartShipInfo.CartShipItemInfo csii = (CartShipInfo.CartShipItemInfo) info.shipItemInfo.get(item);
            if (csii != null && csii.quantity > 0) {
                if (item.shippingApplies()) {
                    Map itemInfo = item.getItemProductInfo();
                    itemInfo.put("quantity", new Double(csii.quantity));
                    itemInfos.add(itemInfo);
                }
            }
        }

        return itemInfos;
    }

    /** Returns true when there are shippable items in the cart */
    public boolean shippingApplies() {
        boolean shippingApplies = false;
        Iterator i = this.iterator();
        while (i.hasNext()) {
            ShoppingCartItem item = (ShoppingCartItem) i.next();
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
        Iterator i = this.iterator();
        while (i.hasNext()) {
            ShoppingCartItem item = (ShoppingCartItem) i.next();
            if (item.taxApplies()) {
                taxApplies = true;
                break;
            }
        }
        return taxApplies;
    }

    /** Returns a Map of all features applied to products in the cart with quantities for a specific ship group. */
    public Map getFeatureIdQtyMap(int idx) {
        CartShipInfo info = this.getShipInfo(idx);
        Map featureMap = new HashMap();

        Iterator i = info.shipItemInfo.keySet().iterator();
        while (i.hasNext()) {
            ShoppingCartItem item = (ShoppingCartItem) i.next();
            CartShipInfo.CartShipItemInfo csii = (CartShipInfo.CartShipItemInfo) info.shipItemInfo.get(item);
            if (csii != null && csii.quantity > 0) {
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
        } else {
            this.nextItemSeq = seq;
        }
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
        if (productPromoActionPK == null) return;

        Iterator fsppas = this.freeShippingProductPromoActions.iterator();
        while (fsppas.hasNext()) {
            if (productPromoActionPK.equals(((GenericValue) fsppas.next()).getPrimaryKey())) {
                fsppas.remove();
            }
        }
    }
    /** Adds a ProductPromoAction to be used for free shipping (must be of type free shipping, or nothing will be done). */
    public void addFreeShippingProductPromoAction(GenericValue productPromoAction) {
        if (productPromoAction == null) return;
        // is this a free shipping action?
        if (!"PROMO_FREE_SHIPPING".equals(productPromoAction.getString("productPromoActionEnumId"))) return; // Changed 1-5-04 by Si Chen

        // to easily make sure that no duplicate exists, do a remove first
        this.removeFreeShippingProductPromoAction(productPromoAction.getPrimaryKey());
        this.freeShippingProductPromoActions.add(productPromoAction);
    }
    public List getFreeShippingProductPromoActions() {
        return this.freeShippingProductPromoActions;
    }

    public void removeAllDesiredAlternateGiftByActions() {
        this.desiredAlternateGiftByAction.clear();
    }
    public void setDesiredAlternateGiftByAction(GenericPK productPromoActionPK, String productId) {
        this.desiredAlternateGiftByAction.put(productPromoActionPK, productId);
    }
    public String getDesiredAlternateGiftByAction(GenericPK productPromoActionPK) {
        return (String) this.desiredAlternateGiftByAction.get(productPromoActionPK);
    }
    public Map getAllDesiredAlternateGiftByActionCopy() {
        return new HashMap(this.desiredAlternateGiftByAction);
    }

    public void addProductPromoUse(String productPromoId, String productPromoCodeId, double totalDiscountAmount, double quantityLeftInActions) {
        if (UtilValidate.isNotEmpty(productPromoCodeId) && !this.productPromoCodes.contains(productPromoCodeId)) {
            throw new IllegalStateException("Cannot add a use to a promo code use for a code that has not been entered.");
        }
        if (Debug.verboseOn()) Debug.logVerbose("Used promotion [" + productPromoId + "] with code [" + productPromoCodeId + "] for total discount [" + totalDiscountAmount + "] and quantity left in actions [" + quantityLeftInActions + "]", module);
        this.productPromoUseInfoList.add(new ProductPromoUseInfo(productPromoId, productPromoCodeId, totalDiscountAmount, quantityLeftInActions));
    }

    public void clearProductPromoUseInfo() {
        // clear out info for general promo use
        this.productPromoUseInfoList.clear();
    }

    public void clearCartItemUseInPromoInfo() {
        // clear out info about which cart items have been used in promos
        Iterator cartLineIter = this.iterator();
        while (cartLineIter.hasNext()) {
            ShoppingCartItem cartLine = (ShoppingCartItem) cartLineIter.next();
            cartLine.clearPromoRuleUseInfo();
        }
    }

    public Iterator getProductPromoUseInfoIter() {
        return productPromoUseInfoList.iterator();
    }

    public double getProductPromoUseTotalDiscount(String productPromoId) {
        if (productPromoId == null) return 0;
        double totalDiscount = 0;
        Iterator productPromoUseInfoIter = this.productPromoUseInfoList.iterator();
        while (productPromoUseInfoIter.hasNext()) {
            ProductPromoUseInfo productPromoUseInfo = (ProductPromoUseInfo) productPromoUseInfoIter.next();
            if (productPromoId.equals(productPromoUseInfo.productPromoId)) {
                totalDiscount += productPromoUseInfo.getTotalDiscountAmount();
            }
        }
        return totalDiscount;
    }

    public int getProductPromoUseCount(String productPromoId) {
        if (productPromoId == null) return 0;
        int useCount = 0;
        Iterator productPromoUseInfoIter = this.productPromoUseInfoList.iterator();
        while (productPromoUseInfoIter.hasNext()) {
            ProductPromoUseInfo productPromoUseInfo = (ProductPromoUseInfo) productPromoUseInfoIter.next();
            if (productPromoId.equals(productPromoUseInfo.productPromoId)) {
                useCount++;
            }
        }
        return useCount;
    }

    public int getProductPromoCodeUse(String productPromoCodeId) {
        if (productPromoCodeId == null) return 0;
        int useCount = 0;
        Iterator productPromoUseInfoIter = this.productPromoUseInfoList.iterator();
        while (productPromoUseInfoIter.hasNext()) {
            ProductPromoUseInfo productPromoUseInfo = (ProductPromoUseInfo) productPromoUseInfoIter.next();
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
        List cartAdjustments = this.getAdjustments();
        if (cartAdjustments != null) {
            Iterator cartAdjustmentIter = cartAdjustments.iterator();
            while (cartAdjustmentIter.hasNext()) {
                GenericValue checkOrderAdjustment = (GenericValue) cartAdjustmentIter.next();
                if (UtilValidate.isNotEmpty(checkOrderAdjustment.getString("productPromoId")) &&
                        UtilValidate.isNotEmpty(checkOrderAdjustment.getString("productPromoRuleId")) &&
                        UtilValidate.isNotEmpty(checkOrderAdjustment.getString("productPromoActionSeqId"))) {
                    cartAdjustmentIter.remove();
                }
            }
        }

        // remove cart lines that are promos (ie GWPs) and cart line adjustments from promo actions
        Iterator cartItemIter = this.iterator();
        while (cartItemIter.hasNext()) {
            ShoppingCartItem checkItem = (ShoppingCartItem) cartItemIter.next();
            if (checkItem.getIsPromo()) {
                this.clearItemShipInfo(checkItem);
                cartItemIter.remove();
            } else {
                // found a promo item with the productId, see if it has a matching adjustment on it
                Iterator checkOrderAdjustments = UtilMisc.toIterator(checkItem.getAdjustments());
                while (checkOrderAdjustments != null && checkOrderAdjustments.hasNext()) {
                    GenericValue checkOrderAdjustment = (GenericValue) checkOrderAdjustments.next();
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
        Iterator cartItemIter = this.iterator();
        while (cartItemIter.hasNext()) {
            ShoppingCartItem checkItem = (ShoppingCartItem) cartItemIter.next();
            checkItem.getAdjustments().clear();
        }
    }

    public void clearAllItemStatus() {
        Iterator lineIter = this.iterator();
        while (lineIter.hasNext()) {
            ShoppingCartItem item = (ShoppingCartItem) lineIter.next();
            item.setStatusId(null);
        }
    }

    /** Adds a promotion code to the cart, checking if it is valid. If it is valid this will return null, otherwise it will return a message stating why it was not valid
     * @param productPromoCodeId The promotion code to check and add
     * @return String that is null if valid, and added to cart, or an error message of the code was not valid and not added to the cart.
     */
    public String addProductPromoCode(String productPromoCodeId, LocalDispatcher dispatcher) {
        if (this.productPromoCodes.contains(productPromoCodeId)) {
            return "The promotion code [" + productPromoCodeId + "] has already been entered.";
        }
        // if the promo code requires it make sure the code is valid
        String checkResult = ProductPromoWorker.checkCanUsePromoCode(productPromoCodeId, this.getPartyId(), this.getDelegator());
        if (checkResult == null) {
            this.productPromoCodes.add(productPromoCodeId);
            // new promo code, re-evaluate promos
            ProductPromoWorker.doPromotions(this, dispatcher);
            return null;
        } else {
            return checkResult;
        }
    }

    public Set getProductPromoCodesEntered() {
        return this.productPromoCodes;
    }

    public synchronized void resetPromoRuleUse(String productPromoId, String productPromoRuleId) {
        Iterator lineIter = this.iterator();
        while (lineIter.hasNext()) {
            ShoppingCartItem cartItem = (ShoppingCartItem) lineIter.next();
            cartItem.resetPromoRuleUse(productPromoId, productPromoRuleId);
        }
    }

    public synchronized void confirmPromoRuleUse(String productPromoId, String productPromoRuleId) {
        Iterator lineIter = this.iterator();
        while (lineIter.hasNext()) {
            ShoppingCartItem cartItem = (ShoppingCartItem) lineIter.next();
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
        List parties = (List) additionalPartyRole.get(roleTypeId);
        if (parties != null) {
            Iterator it = parties.iterator();
            while (it.hasNext()) {
                if (((String) it.next()).equals(partyId)) {
                    return;
                }
            }
        } else {
            parties = new LinkedList();
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
        List parties = (List) additionalPartyRole.get(roleTypeId);

        if (parties != null) {
            Iterator it = parties.iterator();
            while (it.hasNext()) {
                if (((String) it.next()).equals(partyId)) {
                    it.remove();

                    if (parties.isEmpty()) {
                        additionalPartyRole.remove(roleTypeId);
                    }
                    return;
                }
            }
        }
    }

    public Map getAdditionalPartyRoleMap() {
        return additionalPartyRole;
    }

    // =======================================================================
    // Methods used for order creation
    // =======================================================================
    
    public List makeOrderItemGroups() {
        List result = FastList.newInstance();
        Iterator groupValueIter = this.itemGroupByNumberMap.values().iterator();
        while (groupValueIter.hasNext()) {
            ShoppingCart.ShoppingCartItemGroup itemGroup = (ShoppingCart.ShoppingCartItemGroup) groupValueIter.next();
            result.add(itemGroup.makeOrderItemGroup(this.getDelegator()));
        }
        return result;
    }

    private void explodeItems(LocalDispatcher dispatcher) {
        if (dispatcher == null) return;
        synchronized (cartLines) {
            List cartLineItems = new LinkedList(cartLines);
            Iterator itemIter = cartLineItems.iterator();

            while (itemIter.hasNext()) {
                ShoppingCartItem item = (ShoppingCartItem) itemIter.next();

                //Debug.logInfo("Item qty: " + item.getQuantity(), module);
                try {
                    item.explodeItem(this, dispatcher);
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
    public void explodeItems(List shoppingCartItems, LocalDispatcher dispatcher) {
        if (dispatcher == null) return;
        synchronized (cartLines) {
            Iterator itemIter = shoppingCartItems.iterator();
            while (itemIter.hasNext()) {
                ShoppingCartItem item = (ShoppingCartItem) itemIter.next();

                //Debug.logInfo("Item qty: " + item.getQuantity(), module);
                try {
                    item.explodeItem(this, dispatcher);
                } catch (CartItemModifyException e) {
                    Debug.logError(e, "Problem exploding (unitizing) item! Item not exploded.", module);
                }
            }
        }
    }

    public List makeOrderItems() {
        return makeOrderItems(false, null);
    }

    public List makeOrderItems(boolean explodeItems, LocalDispatcher dispatcher) {
        // do the explosion
        if (explodeItems && dispatcher != null) {
            explodeItems(dispatcher);
        }

        // now build the lines
        synchronized (cartLines) {
            List result = FastList.newInstance();

            Iterator itemIter = cartLines.iterator();
            while (itemIter.hasNext()) {
                ShoppingCartItem item = (ShoppingCartItem) itemIter.next();

                if (UtilValidate.isEmpty(item.getOrderItemSeqId())) {
                    String orderItemSeqId = UtilFormatOut.formatPaddedNumber(nextItemSeq, 5);
                    item.setOrderItemSeqId(orderItemSeqId);
                } else {
                    try {
                        int thisSeqId = Integer.parseInt(item.getOrderItemSeqId());
                        if (thisSeqId > nextItemSeq) {
                            nextItemSeq = thisSeqId;
                        }
                    } catch (NumberFormatException e) {
                        Debug.logError(e, module);
                    }
                }
                nextItemSeq++;

                // the initial status for all item types
                String initialStatus = "ITEM_CREATED";
                String status = item.getStatusId();
                if (status == null) {
                    status = initialStatus;
                }

                GenericValue orderItem = getDelegator().makeValue("OrderItem", null);
                orderItem.set("orderItemSeqId", item.getOrderItemSeqId());
                orderItem.set("externalId", item.getExternalId());
                orderItem.set("orderItemTypeId", item.getItemType());
                if (item.getItemGroup() != null) orderItem.set("orderItemGroupSeqId", item.getItemGroup().getGroupNumber());
                orderItem.set("productId", item.getProductId());
                orderItem.set("prodCatalogId", item.getProdCatalogId());
                orderItem.set("productCategoryId", item.getProductCategoryId());
                orderItem.set("quantity", new Double(item.getQuantity()));
                orderItem.set("selectedAmount", new Double(item.getSelectedAmount()));
                orderItem.set("unitPrice", new Double(item.getBasePrice()));
                orderItem.set("unitListPrice", new Double(item.getListPrice()));
                orderItem.set("isModifiedPrice",item.getIsModifiedPrice() ? "Y" : "N");
                orderItem.set("isPromo", item.getIsPromo() ? "Y" : "N");

                orderItem.set("shoppingListId", item.getShoppingListId());
                orderItem.set("shoppingListItemSeqId", item.getShoppingListItemSeqId());

                orderItem.set("itemDescription", item.getName());
                orderItem.set("comments", item.getItemComment());
                orderItem.set("estimatedDeliveryDate", item.getDesiredDeliveryDate());
                orderItem.set("correspondingPoId", this.getPoNumber());
                orderItem.set("quoteId", item.getQuoteId());
                orderItem.set("quoteItemSeqId", item.getQuoteItemSeqId());
                orderItem.set("statusId", status);

                orderItem.set("shipBeforeDate", item.getShipBeforeDate());
                orderItem.set("shipAfterDate", item.getShipAfterDate());

                result.add(orderItem);
                // don't do anything with adjustments here, those will be added below in makeAllAdjustments
            }
            return result;
        }
    }

    /** create WorkEfforts from the shoppingcart items when itemType = RENTAL_ORDER_ITEM */
    public List makeWorkEfforts() {
        List allWorkEfforts = new LinkedList();
        Iterator itemIter = cartLines.iterator();

        while (itemIter.hasNext()) {
            ShoppingCartItem item = (ShoppingCartItem) itemIter.next();
            if ("RENTAL_ORDER_ITEM".equals(item.getItemType())) {         // prepare workeffort when the order item is a rental item
                GenericValue workEffort = getDelegator().makeValue("WorkEffort", null);
                workEffort.set("workEffortId",item.getOrderItemSeqId());  // fill temporary with sequence number
                workEffort.set("estimatedStartDate",item.getReservStart());
                workEffort.set("estimatedCompletionDate",item.getReservStart(item.getReservLength()));
                workEffort.set("reservPersons",new Double(item.getReservPersons()));
                workEffort.set("reserv2ndPPPerc", new Double(item.getReserv2ndPPPerc()));
                workEffort.set("reservNthPPPerc", new Double(item.getReservNthPPPerc()));

                allWorkEfforts.add(workEffort);
            }
        }
        return allWorkEfforts;
    }

    /** make a list of all adjustments including order adjustments, order line adjustments, and special adjustments (shipping and tax if applicable) */
    public List makeAllAdjustments() {
        List allAdjs = new LinkedList();

        // before returning adjustments, go through them to find all that need counter adjustments (for instance: free shipping)
        Iterator allAdjsIter = this.getAdjustments().iterator();

        while (allAdjsIter.hasNext()) {
            GenericValue orderAdjustment = (GenericValue) allAdjsIter.next();

            allAdjs.add(orderAdjustment);

            if ("SHIPPING_CHARGES".equals(orderAdjustment.get("orderAdjustmentTypeId"))) {
                Iterator fsppas = this.freeShippingProductPromoActions.iterator();

                while (fsppas.hasNext()) {
                    GenericValue productPromoAction = (GenericValue) fsppas.next();

                    // TODO - we need to change the way free shipping promotions work
                    /*
                    if ((productPromoAction.get("productId") == null || productPromoAction.getString("productId").equals(this.getShipmentMethodTypeId())) &&
                        (productPromoAction.get("partyId") == null || productPromoAction.getString("partyId").equals(this.getCarrierPartyId()))) {
                        Double shippingAmount = new Double(-OrderReadHelper.calcOrderAdjustment(orderAdjustment, getSubTotal()));
                        // always set orderAdjustmentTypeId to SHIPPING_CHARGES for free shipping adjustments
                        GenericValue fsOrderAdjustment = getDelegator().makeValue("OrderAdjustment",
                                UtilMisc.toMap("orderItemSeqId", orderAdjustment.get("orderItemSeqId"), "orderAdjustmentTypeId", "SHIPPING_CHARGES", "amount", shippingAmount,
                                    "productPromoId", productPromoAction.get("productPromoId"), "productPromoRuleId", productPromoAction.get("productPromoRuleId"),
                                    "productPromoActionSeqId", productPromoAction.get("productPromoActionSeqId")));

                        allAdjs.add(fsOrderAdjustment);

                        // if free shipping IS applied to this orderAdjustment, break
                        // out of the loop so that even if there are multiple free
                        // shipping adjustments that apply to this orderAdjustment it
                        // will only be compensated for once
                        break;
                    }
                    */
                }
            }
        }

        // add all of the item adjustments to this list too
        Iterator itemIter = cartLines.iterator();

        while (itemIter.hasNext()) {
            ShoppingCartItem item = (ShoppingCartItem) itemIter.next();
            Collection adjs = item.getAdjustments();

            if (adjs != null) {
                Iterator adjIter = adjs.iterator();

                while (adjIter.hasNext()) {
                    GenericValue orderAdjustment = (GenericValue) adjIter.next();

                    orderAdjustment.set("orderItemSeqId", item.getOrderItemSeqId());
                    allAdjs.add(orderAdjustment);

                    if ("SHIPPING_CHARGES".equals(orderAdjustment.get("orderAdjustmentTypeId"))) {
                        Iterator fsppas = this.freeShippingProductPromoActions.iterator();

                        while (fsppas.hasNext()) {
                            GenericValue productPromoAction = (GenericValue) fsppas.next();

                            // TODO - fix the free shipping promotions!!
                            /*
                            if ((productPromoAction.get("productId") == null || productPromoAction.getString("productId").equals(item.getShipmentMethodTypeId())) &&
                                (productPromoAction.get("partyId") == null || productPromoAction.getString("partyId").equals(item.getCarrierPartyId()))) {
                                Double shippingAmount = new Double(-OrderReadHelper.calcItemAdjustment(orderAdjustment, new Double(item.getQuantity()), new Double(item.getItemSubTotal())));
                                // always set orderAdjustmentTypeId to SHIPPING_CHARGES for free shipping adjustments
                                GenericValue fsOrderAdjustment = getDelegator().makeValue("OrderAdjustment",
                                        UtilMisc.toMap("orderItemSeqId", orderAdjustment.get("orderItemSeqId"), "orderAdjustmentTypeId", "SHIPPING_CHARGES", "amount", shippingAmount,
                                            "productPromoId", productPromoAction.get("productPromoId"), "productPromoRuleId", productPromoAction.get("productPromoRuleId"),
                                            "productPromoActionSeqId", productPromoAction.get("productPromoActionSeqId")));

                                allAdjs.add(fsOrderAdjustment);

                                // if free shipping IS applied to this orderAdjustment, break
                                // out of the loop so that even if there are multiple free
                                // shipping adjustments that apply to this orderAdjustment it
                                // will only be compensated for once
                                break;
                            }
                            */
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
    public List makeAllQuoteAdjustments() {
        List quoteAdjs = new LinkedList();

        List orderAdjs = makeAllAdjustments();
        Iterator orderAdjsIter = orderAdjs.iterator();

        while (orderAdjsIter.hasNext()) {
            GenericValue orderAdj = (GenericValue) orderAdjsIter.next();
            GenericValue quoteAdj = this.getDelegator().makeValue("QuoteAdjustment", null);
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
    public List makeAllOrderPaymentInfos() {
        List allOpPrefs = new LinkedList();
        Iterator i = paymentInfo.iterator();
        while (i.hasNext()) {
            CartPaymentInfo inf = (CartPaymentInfo) i.next();
            allOpPrefs.addAll(inf.makeOrderPaymentInfos(this.getDelegator()));
        }
        if (getBillingAccountId() != null) {
            GenericValue opp = delegator.makeValue("OrderPaymentPreference", new HashMap());
            opp.set("paymentMethodTypeId", "EXT_BILLACT");
            opp.set("presentFlag", "N");
            opp.set("overflowFlag", "N");
            opp.set("maxAmount", new Double(getBillingAccountAmount()));
            opp.set("statusId", "PAYMENT_NOT_RECEIVED");
            allOpPrefs.add(opp);
        }
        return allOpPrefs;
    }

    /** make a list of OrderItemPriceInfos from the ShoppingCartItems */
    public List makeAllOrderItemPriceInfos() {
        List allInfos = new LinkedList();

        // add all of the item adjustments to this list too
        Iterator itemIter = cartLines.iterator();

        while (itemIter.hasNext()) {
            ShoppingCartItem item = (ShoppingCartItem) itemIter.next();
            Collection infos = item.getOrderItemPriceInfos();

            if (infos != null) {
                Iterator infosIter = infos.iterator();

                while (infosIter.hasNext()) {
                    GenericValue orderItemPriceInfo = (GenericValue) infosIter.next();

                    orderItemPriceInfo.set("orderItemSeqId", item.getOrderItemSeqId());
                    allInfos.add(orderItemPriceInfo);
                }
            }
        }

        return allInfos;
    }

    public List makeProductPromoUses() {
        List productPromoUses = new ArrayList(this.productPromoUseInfoList.size());
        String partyId = this.getPartyId();
        int sequenceValue = 0;
        Iterator productPromoUseInfoIter = this.productPromoUseInfoList.iterator();
        while (productPromoUseInfoIter.hasNext()) {
            ProductPromoUseInfo productPromoUseInfo = (ProductPromoUseInfo) productPromoUseInfoIter.next();
            GenericValue productPromoUse = this.getDelegator().makeValue("ProductPromoUse", null);
            productPromoUse.set("promoSequenceId", UtilFormatOut.formatPaddedNumber(sequenceValue, 5));
            productPromoUse.set("productPromoId", productPromoUseInfo.getProductPromoId());
            productPromoUse.set("productPromoCodeId", productPromoUseInfo.getProductPromoCodeId());
            productPromoUse.set("totalDiscountAmount", new Double(productPromoUseInfo.getTotalDiscountAmount()));
            productPromoUse.set("quantityLeftInActions", new Double(productPromoUseInfo.getQuantityLeftInActions()));
            productPromoUse.set("partyId", partyId);
            productPromoUses.add(productPromoUse);
            sequenceValue++;
        }
        return productPromoUses;
    }

    /** make a list of SurveyResponse object to update with order information set */
    public List makeAllOrderItemSurveyResponses() {
        List allInfos = new LinkedList();
        Iterator itemIter = this.iterator();
        while (itemIter.hasNext()) {
            ShoppingCartItem item = (ShoppingCartItem) itemIter.next();
            List responses = (List) item.getAttribute("surveyResponses");
            if (responses != null) {
                Iterator ri = responses.iterator();
                while (ri.hasNext()) {
                    String responseId = (String) ri.next();
                    GenericValue response = null;
                    try {
                        response = this.getDelegator().findByPrimaryKey("SurveyResponse", UtilMisc.toMap("surveyResponseId", responseId));
                    } catch (GenericEntityException e) {
                        Debug.logError(e, "Unable to obtain SurveyResponse record for ID : " + responseId, module);
                    }
                    if (response != null) {
                        response.set("orderItemSeqId", item.getOrderItemSeqId());
                        allInfos.add(response);
                    }
                }
            }
        }
        return allInfos;
    }

    /** make a list of OrderContactMechs from the ShoppingCart and the ShoppingCartItems */
    public List makeAllOrderContactMechs() {
        List allOrderContactMechs = new LinkedList();

        Map contactMechIds = this.getOrderContactMechIds();

        if (contactMechIds != null) {
            Iterator cMechIdsIter = contactMechIds.entrySet().iterator();

            while (cMechIdsIter.hasNext()) {
                Map.Entry entry = (Map.Entry) cMechIdsIter.next();
                GenericValue orderContactMech = getDelegator().makeValue("OrderContactMech", null);

                orderContactMech.set("contactMechPurposeTypeId", entry.getKey());
                orderContactMech.set("contactMechId", entry.getValue());
                allOrderContactMechs.add(orderContactMech);
            }
        }

        return allOrderContactMechs;
    }

    /** make a list of OrderContactMechs from the ShoppingCart and the ShoppingCartItems */
    public List makeAllOrderItemContactMechs() {
        List allOrderContactMechs = new LinkedList();

        Iterator itemIter = cartLines.iterator();

        while (itemIter.hasNext()) {
            ShoppingCartItem item = (ShoppingCartItem) itemIter.next();
            Map itemContactMechIds = item.getOrderItemContactMechIds();

            if (itemContactMechIds != null) {
                Iterator cMechIdsIter = itemContactMechIds.entrySet().iterator();

                while (cMechIdsIter.hasNext()) {
                    Map.Entry entry = (Map.Entry) cMechIdsIter.next();
                    GenericValue orderContactMech = getDelegator().makeValue("OrderItemContactMech", null);

                    orderContactMech.set("contactMechPurposeTypeId", entry.getKey());
                    orderContactMech.set("contactMechId", entry.getValue());
                    orderContactMech.set("orderItemSeqId", item.getOrderItemSeqId());
                    allOrderContactMechs.add(orderContactMech);
                }
            }
        }

        return allOrderContactMechs;
    }

    public List makeAllShipGroupInfos() {
        List groups = new LinkedList();
        Iterator grpIterator = this.shipInfo.iterator();
        long seqId = 1;
        while (grpIterator.hasNext()) {
            CartShipInfo csi = (CartShipInfo) grpIterator.next();
            groups.addAll(csi.makeItemShipGroupAndAssoc(this.getDelegator(), this, seqId));
            seqId++;
        }
        return groups;
    }

    public List makeAllOrderItemAttributes() {
        List allOrderItemAttributes = new LinkedList();

        Iterator itemIter = cartLines.iterator();
        while (itemIter.hasNext()) {
            ShoppingCartItem item = (ShoppingCartItem) itemIter.next();
            Map attrs = item.getOrderItemAttributes();
            Iterator i = attrs.entrySet().iterator();
            while (i.hasNext()) {
                Map.Entry entry = (Map.Entry) i.next();
                GenericValue itemAtt = this.getDelegator().makeValue("OrderItemAttribute", null);
                itemAtt.set("orderItemSeqId", item.getOrderItemSeqId());
                itemAtt.set("attrName", entry.getKey());
                itemAtt.set("attrValue", entry.getValue());
                allOrderItemAttributes.add(itemAtt);
            }
        }
        return allOrderItemAttributes;
    }

    public List makeAllOrderAttributes() {
        List allOrderAttributes = new LinkedList();

        Iterator i = orderAttributes.entrySet().iterator();
        while (i.hasNext()) {
            Map.Entry entry = (Map.Entry) i.next();
            GenericValue orderAtt = this.getDelegator().makeValue("OrderAttribute", null);
            orderAtt.put("attrName", entry.getKey());
            orderAtt.put("attrValue", entry.getValue());
            allOrderAttributes.add(orderAtt);
        }
        return allOrderAttributes;
    }

    public List makeAllOrderItemAssociations() {
        List allOrderItemAssociations = new LinkedList();

        Iterator itemIter = cartLines.iterator();

        while (itemIter.hasNext()) {
            ShoppingCartItem item = (ShoppingCartItem) itemIter.next();
            String requirementId = item.getRequirementId();
            if (requirementId != null) {
                try {
                    List commitments = getDelegator().findByAnd("OrderRequirementCommitment", UtilMisc.toMap("requirementId", requirementId));
                    // TODO: multiple commitments for the same requirement are still not supported
                    GenericValue commitment = EntityUtil.getFirst(commitments);
                    if (commitment != null) {
                        GenericValue orderItemAssociation = getDelegator().makeValue("OrderItemAssoc", null);
                        orderItemAssociation.set("orderId", commitment.getString("orderId"));
                        orderItemAssociation.set("orderItemSeqId", commitment.getString("orderItemSeqId"));
                        orderItemAssociation.set("shipGroupSeqId", "_NA_");
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
                GenericValue orderItemAssociation = getDelegator().makeValue("OrderItemAssoc", null);
                orderItemAssociation.set("orderId", item.getAssociatedOrderId());
                orderItemAssociation.set("orderItemSeqId", item.getAssociatedOrderItemSeqId());
                orderItemAssociation.set("shipGroupSeqId", "_NA_");
                orderItemAssociation.set("toOrderItemSeqId", item.getOrderItemSeqId());
                orderItemAssociation.set("toShipGroupSeqId", "_NA_");
                orderItemAssociation.set("orderItemAssocTypeId", item.getOrderItemAssocTypeId());
                allOrderItemAssociations.add(orderItemAssociation);
            }
        }
        return allOrderItemAssociations;
    }

    /** Returns a Map of cart values to pass to the storeOrder service */
    public Map makeCartMap(LocalDispatcher dispatcher, boolean explodeItems) {
        Map result = new HashMap();

        result.put("orderTypeId", this.getOrderType());
        result.put("orderName", this.getOrderName());
        result.put("externalId", this.getExternalId());
        result.put("orderDate", this.getOrderDate());
        result.put("internalCode", this.getInternalCode());
        result.put("salesChannelEnumId", this.getChannelType());
        result.put("orderItemGroups", this.makeOrderItemGroups());
        result.put("orderItems", this.makeOrderItems(explodeItems, dispatcher));
        result.put("workEfforts", this.makeWorkEfforts());
        result.put("orderAdjustments", this.makeAllAdjustments());
        result.put("orderTerms", this.getOrderTerms());
        result.put("orderItemPriceInfos", this.makeAllOrderItemPriceInfos());
        result.put("orderProductPromoUses", this.makeProductPromoUses());

        result.put("orderAttributes", this.makeAllOrderAttributes());
        result.put("orderItemAttributes", this.makeAllOrderItemAttributes());
        result.put("orderContactMechs", this.makeAllOrderContactMechs());
        result.put("orderItemContactMechs", this.makeAllOrderItemContactMechs());
        result.put("orderPaymentInfo", this.makeAllOrderPaymentInfos());
        result.put("orderItemShipGroupInfo", this.makeAllShipGroupInfos());
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

    public List getLineListOrderedByBasePrice(boolean ascending) {
        List result = new ArrayList(this.cartLines);
        Collections.sort(result, new BasePriceOrderComparator(ascending));
        return result;
    }

    public Map getShipGroupsBySupplier(String supplierPartyId) {
        Map shipGroups = new TreeMap();
        for (int i = 0; i < this.shipInfo.size(); i++) {
            CartShipInfo csi = (CartShipInfo) shipInfo.get(i);
            if ((csi.supplierPartyId == null && supplierPartyId == null) ||
                (UtilValidate.isNotEmpty(csi.supplierPartyId) && csi.supplierPartyId.equals(supplierPartyId))) {
                shipGroups.put(new Integer(i), csi);
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
        if (UtilValidate.isNotEmpty(this.getProductStoreId())) {
            try {
                GenericValue productStore = delegator.findByPrimaryKeyCache("ProductStore", UtilMisc.toMap("productStoreId", this.getProductStoreId()));
                facilityId = productStore.getString("inventoryFacilityId");
            } catch (Exception e) {
                Debug.logError(UtilProperties.getMessage(resource_error,"OrderProblemGettingProductStoreRecords", locale) + e.getMessage(), module);
                return;
            }
        }
        
        List shipGroups = getShipGroups();
        if (shipGroups == null) return;

        // Intermediate structure supplierPartyId -> { ShoppingCartItem = { originalShipGroupIndex = dropShipQuantity } } to collect drop-shippable items
        Map dropShipItems = new HashMap();
        
        for (int shipGroupIndex = 0; shipGroupIndex < shipGroups.size(); shipGroupIndex++) {
            
            CartShipInfo shipInfo = (CartShipInfo) shipGroups.get(shipGroupIndex);
            
            // Ignore ship groups that are already drop shipped
            String shipGroupSupplierPartyId = shipInfo.getSupplierPartyId();
            if (UtilValidate.isNotEmpty(shipGroupSupplierPartyId)) continue;

            // Ignore empty ship groups
            Set shipItems = shipInfo.getShipItems();
            if (UtilValidate.isEmpty(shipItems)) continue;

            Iterator siit = shipItems.iterator();
            while (siit.hasNext()) {
                
                ShoppingCartItem cartItem = (ShoppingCartItem) siit.next();

                double itemQuantity = cartItem.getQuantity();
                double dropShipQuantity = 0;

                GenericValue product = cartItem.getProduct();
                String productId = product.getString("productId");
                String requirementMethodEnumId = product.getString("requirementMethodEnumId");

                if ("PRODRQM_DS".equals(requirementMethodEnumId)) {
                    
                    // Drop ship the full quantity if the product is marked drop-ship only
                    dropShipQuantity = itemQuantity;
                    
                } else if ("PRODRQM_DSATP".equals(requirementMethodEnumId)) {
                    
                    // Drop ship the quantity not available in inventory if the product is marked drop-ship on low inventory
                    try {

                        // Get ATP for the product
                        Map getProductInventoryAvailableResult = dispatcher.runSync("getInventoryAvailableByFacility", UtilMisc.toMap("productId", productId, "facilityId", facilityId));
                        double availableToPromise = ((Double) getProductInventoryAvailableResult.get("availableToPromiseTotal")).doubleValue();

                        if (itemQuantity <= availableToPromise) {
                            dropShipQuantity = 0;
                        } else {
                            dropShipQuantity = itemQuantity - availableToPromise;
                        }

                    } catch (Exception e) {
                        Debug.logWarning(UtilProperties.getMessage(resource_error,"OrderRunServiceGetInventoryAvailableByFacilityError", locale) + e.getMessage(), module);
                    }
                } else {
                    
                    // Don't drop ship anything if the product isn't so marked
                    dropShipQuantity = 0;
                }

                if (dropShipQuantity <= 0) continue;
                
                // Find a supplier for the product
                String supplierPartyId = null;
                try {
                    Map getSuppliersForProductResult = dispatcher.runSync("getSuppliersForProduct", UtilMisc.toMap("productId", productId, "quantity", new Double(dropShipQuantity), "canDropShip", "Y", "currencyUomId", getCurrency()));
                    List supplierProducts = (List) getSuppliersForProductResult.get("supplierProducts");
                    
                    // Order suppliers by supplierPrefOrderId so that preferred suppliers are used first
                    supplierProducts = EntityUtil.orderBy(supplierProducts, UtilMisc.toList("supplierPrefOrderId"));
                    GenericValue supplierProduct = EntityUtil.getFirst(supplierProducts);
                    if (! UtilValidate.isEmpty(supplierProduct)) {
                        supplierPartyId = supplierProduct.getString("partyId");
                    }
                } catch (Exception e) {
                    Debug.logWarning(UtilProperties.getMessage(resource_error,"OrderRunServiceGetSuppliersForProductError", locale) + e.getMessage(), module);
                }
                
                // Leave the items untouched if we couldn't find a supplier
                if (UtilValidate.isEmpty(supplierPartyId)) continue;
                
                if (! dropShipItems.containsKey(supplierPartyId)) dropShipItems.put(supplierPartyId, new HashMap());
                Map supplierCartItems = (Map) dropShipItems.get(supplierPartyId);

                if (! supplierCartItems.containsKey(cartItem)) supplierCartItems.put(cartItem, new HashMap());
                Map cartItemGroupQuantities = (Map) supplierCartItems.get(cartItem);

                cartItemGroupQuantities.put(new Integer(shipGroupIndex), new Double(dropShipQuantity));
            }
        }

        // Reassign the drop-shippable item quantities to new or existing drop-ship groups
        Iterator dsit = dropShipItems.keySet().iterator();
        while (dsit.hasNext()) {
            String supplierPartyId = (String) dsit.next();

            CartShipInfo shipInfo = null;
            int newShipGroupIndex = -1 ;
            
            // Attempt to get the first ship group for the supplierPartyId
            TreeMap supplierShipGroups = (TreeMap) this.getShipGroupsBySupplier(supplierPartyId);
            if (! UtilValidate.isEmpty(supplierShipGroups)) {
                newShipGroupIndex = ((Integer) supplierShipGroups.firstKey()).intValue();
                shipInfo = (CartShipInfo) supplierShipGroups.get(supplierShipGroups.firstKey());
            }
            if (newShipGroupIndex == -1) {
                newShipGroupIndex = addShipInfo();
                shipInfo = (CartShipInfo) this.shipInfo.get(newShipGroupIndex);
            }
            shipInfo.supplierPartyId = supplierPartyId;

            Map supplierCartItems = (Map) dropShipItems.get(supplierPartyId);
            Iterator itit = supplierCartItems.keySet().iterator();
            while (itit.hasNext()) {

                ShoppingCartItem cartItem = (ShoppingCartItem) itit.next();
                Map cartItemGroupQuantities = (Map) supplierCartItems.get(cartItem);
                Iterator cigit = cartItemGroupQuantities.keySet().iterator();
                while (cigit.hasNext()) {

                    Integer previousShipGroupIndex = (Integer) cigit.next();
                    double dropShipQuantity = ((Double) cartItemGroupQuantities.get(previousShipGroupIndex)).doubleValue();
                    positionItemToGroup(cartItem, dropShipQuantity, previousShipGroupIndex.intValue(), newShipGroupIndex, true);
                }
            }
        }
    }
    
    static class BasePriceOrderComparator implements Comparator, Serializable {
        private boolean ascending = false;

        BasePriceOrderComparator(boolean ascending) {
            this.ascending = ascending;
        }

        public int compare(java.lang.Object obj, java.lang.Object obj1) {
            ShoppingCartItem cartItem = (ShoppingCartItem) obj;
            ShoppingCartItem cartItem1 = (ShoppingCartItem) obj1;

            int compareValue = new Double(cartItem.getBasePrice()).compareTo(new Double(cartItem1.getBasePrice()));
            if (this.ascending) {
                return compareValue;
            } else {
                return -compareValue;
            }
        }

        public boolean equals(java.lang.Object obj) {
            if (obj instanceof BasePriceOrderComparator) {
                return this.ascending == ((BasePriceOrderComparator) obj).ascending;
            } else {
                return false;
            }
        }
    }

    public static class ShoppingCartItemGroup implements Serializable {
        private long groupNumber;
        private String groupName;
        private ShoppingCartItemGroup parentGroup;

        // don't allow empty constructor
        private ShoppingCartItemGroup() {}

        protected ShoppingCartItemGroup(long groupNumber, String groupName) {
            this(groupNumber, groupName, null);
        }
        
        /** Note that to avoid foreign key issues when the groups are created a parentGroup should have a lower number than the child group. */
        protected ShoppingCartItemGroup(long groupNumber, String groupName, ShoppingCartItemGroup parentGroup) {
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
            return UtilFormatOut.formatPaddedNumber(this.groupNumber, 2);
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
        
        protected GenericValue makeOrderItemGroup(GenericDelegator delegator) {
            GenericValue orderItemGroup = delegator.makeValue("OrderItemGroup", null);
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
        
        public boolean equals(Object obj) {
            if (obj == null) return false;
            ShoppingCartItemGroup that = (ShoppingCartItemGroup) obj;
            if (that.groupNumber == this.groupNumber) {
                return true;
            }
            return false;
        }
    }

    public static class ProductPromoUseInfo implements Serializable {
        public String productPromoId = null;
        public String productPromoCodeId = null;
        public double totalDiscountAmount = 0;
        public double quantityLeftInActions = 0;

        public ProductPromoUseInfo(String productPromoId, String productPromoCodeId, double totalDiscountAmount, double quantityLeftInActions) {
            this.productPromoId = productPromoId;
            this.productPromoCodeId = productPromoCodeId;
            this.totalDiscountAmount = totalDiscountAmount;
            this.quantityLeftInActions = quantityLeftInActions;
        }

        public String getProductPromoId() { return this.productPromoId; }
        public String getProductPromoCodeId() { return this.productPromoCodeId; }
        public double getTotalDiscountAmount() { return this.totalDiscountAmount; }
        public double getQuantityLeftInActions() { return this.quantityLeftInActions; }
    }

    public static class CartShipInfo implements Serializable {
        public LinkedMap shipItemInfo = new LinkedMap();
        public List shipTaxAdj = new LinkedList();
        public String contactMechId = null;
        public String shipmentMethodTypeId = null;
        public String supplierPartyId = null;
        public String carrierRoleTypeId = null;
        public String carrierPartyId = null;
        public String giftMessage = null;
        public String shippingInstructions = null;
        public String maySplit = "N";
        public String isGift = "N";
        public double shipEstimate = 0.00;
        public Timestamp shipBeforeDate = null;
        public Timestamp shipAfterDate = null;

        public String getContactMechId() { return contactMechId; }
        public String getCarrierPartyId() { return carrierPartyId; }
        public String getSupplierPartyId() { return supplierPartyId; }
        public String getShipmentMethodTypeId() { return shipmentMethodTypeId; }

        public List makeItemShipGroupAndAssoc(GenericDelegator delegator, ShoppingCart cart, long groupIndex) {
            String shipGroupSeqId = UtilFormatOut.formatPaddedNumber(groupIndex, 5);
            List values = new LinkedList();
            
            // create order contact mech for shipping address
            if (contactMechId != null) {
                GenericValue orderCm = delegator.makeValue("OrderContactMech", null);
                orderCm.set("contactMechPurposeTypeId", "SHIPPING_LOCATION");
                orderCm.set("contactMechId", contactMechId);
                values.add(orderCm);
            }

            // create the ship group
            GenericValue shipGroup = delegator.makeValue("OrderItemShipGroup", null);
            shipGroup.set("shipmentMethodTypeId", shipmentMethodTypeId);
            shipGroup.set("carrierRoleTypeId", carrierRoleTypeId);
            shipGroup.set("carrierPartyId", carrierPartyId);
            shipGroup.set("supplierPartyId", supplierPartyId);
            shipGroup.set("shippingInstructions", shippingInstructions);
            shipGroup.set("giftMessage", giftMessage);
            shipGroup.set("contactMechId", contactMechId);
            shipGroup.set("maySplit", maySplit);
            shipGroup.set("isGift", isGift);
            shipGroup.set("shipGroupSeqId", shipGroupSeqId);
            
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

            // create the shipping estimate adjustments
            if (shipEstimate != 0) {
                GenericValue shipAdj = delegator.makeValue("OrderAdjustment", null);
                shipAdj.set("orderAdjustmentTypeId", "SHIPPING_CHARGES");
                shipAdj.set("amount", new Double(shipEstimate));
                shipAdj.set("shipGroupSeqId", shipGroupSeqId);
                values.add(shipAdj);
            }

            // create the top level tax adjustments
            Iterator ti = shipTaxAdj.iterator();
            while (ti.hasNext()) {
                GenericValue taxAdj = (GenericValue) ti.next();
                taxAdj.set("shipGroupSeqId", shipGroupSeqId);
                values.add(taxAdj);
            }

            // create the ship group item associations
            Iterator i = shipItemInfo.keySet().iterator();
            while (i.hasNext()) {
                ShoppingCartItem item = (ShoppingCartItem) i.next();
                CartShipItemInfo itemInfo = (CartShipItemInfo) shipItemInfo.get(item);

                GenericValue assoc = delegator.makeValue("OrderItemShipGroupAssoc", null);
                assoc.set("orderItemSeqId", item.getOrderItemSeqId());
                assoc.set("shipGroupSeqId", shipGroupSeqId);
                assoc.set("quantity", new Double(itemInfo.quantity));
                values.add(assoc);

                // create the item tax adjustment
                Iterator iti = itemInfo.itemTaxAdj.iterator();
                while (iti.hasNext()) {
                    GenericValue taxAdj = (GenericValue) iti.next();
                    taxAdj.set("orderItemSeqId", item.getOrderItemSeqId());
                    taxAdj.set("shipGroupSeqId", shipGroupSeqId);
                    values.add(taxAdj);
                }
            }

            return values;
        }

        public CartShipItemInfo setItemInfo(ShoppingCartItem item, double quantity, List taxAdj) {
            CartShipItemInfo itemInfo = (CartShipItemInfo) shipItemInfo.get(item);
            if (itemInfo == null) {
                itemInfo = new CartShipItemInfo();
                itemInfo.item = item;
                shipItemInfo.put(item, itemInfo);
            }
            itemInfo.quantity = quantity;
            itemInfo.itemTaxAdj.clear();
            if (taxAdj == null) {
                taxAdj = new LinkedList();
            }
            itemInfo.itemTaxAdj.addAll(taxAdj);
            return itemInfo;
        }

        public CartShipItemInfo setItemInfo(ShoppingCartItem item, List taxAdj) {
            CartShipItemInfo itemInfo = (CartShipItemInfo) shipItemInfo.get(item);
            if (itemInfo == null) {
                itemInfo = new CartShipItemInfo();
                itemInfo.item = item;
                shipItemInfo.put(item, itemInfo);
            }
            itemInfo.itemTaxAdj.clear();
            if (taxAdj == null) {
                taxAdj = new LinkedList();
            }
            itemInfo.itemTaxAdj.addAll(taxAdj);
            return itemInfo;
        }

        public CartShipItemInfo setItemInfo(ShoppingCartItem item, double quantity) {
            CartShipItemInfo itemInfo = (CartShipItemInfo) shipItemInfo.get(item);
            if (itemInfo == null) {
                itemInfo = new CartShipItemInfo();
                itemInfo.item = item;
                shipItemInfo.put(item, itemInfo);
            }
            itemInfo.quantity = quantity;
            return itemInfo;
        }

        public CartShipItemInfo getShipItemInfo(ShoppingCartItem item) {
            return (CartShipItemInfo) shipItemInfo.get(item);
        }

        public Set getShipItems() {
            return shipItemInfo.keySet();
        }

        /**
         * Reset the ship group's shipBeforeDate if it is after the parameter 
         * @param newShipBeforeDate
         */
        public void resetShipBeforeDateIfAfter(Timestamp newShipBeforeDate) {
                if (newShipBeforeDate != null) {
                if ((this.shipBeforeDate == null) || (!this.shipBeforeDate.before(newShipBeforeDate))) {
                    this.shipBeforeDate = newShipBeforeDate;
                }
            }
        }
        
        /**
         * Reset the ship group's shipAfterDate if it is before the parameter 
         * @param newShipBeforeDate
         */
        public void resetShipAfterDateIfBefore(Timestamp newShipAfterDate) {
            if (newShipAfterDate != null) {
                if ((this.shipAfterDate == null) || (!this.shipAfterDate.after(newShipAfterDate))) {
                    this.shipAfterDate = newShipAfterDate;
                }
            }
        }
        
        public double getTotalTax(ShoppingCart cart) {
            double taxTotal = 0.00;
            for (int i = 0; i < shipTaxAdj.size(); i++) {
                GenericValue v = (GenericValue) shipTaxAdj.get(i);
                taxTotal += OrderReadHelper.calcOrderAdjustment(v, cart.getSubTotal());
            }

            Iterator iter = shipItemInfo.values().iterator();
            while (iter.hasNext()) {
                CartShipItemInfo info = (CartShipItemInfo) iter.next();
                taxTotal += info.getItemTax(cart);
            }

            return taxTotal;
        }

        public static class CartShipItemInfo implements Serializable {
            public List itemTaxAdj = new LinkedList();
            public ShoppingCartItem item = null;
            public double quantity = 0;

            public double getItemTax(ShoppingCart cart) {
                double itemTax = 0.00;

                for (int i = 0; i < itemTaxAdj.size(); i++) {
                    GenericValue v = (GenericValue) itemTaxAdj.get(i);
                    itemTax += OrderReadHelper.calcItemAdjustment(v, new Double(quantity), new Double(item.getBasePrice()));
                }

                return itemTax;
            }

            public double getItemQuantity() {
                return this.quantity;
            }
        }
    }

    public static class CartPaymentInfo implements Serializable, Comparable {
        public String paymentMethodTypeId = null;
        public String paymentMethodId = null;
        public String finAccountId = null;
        public String securityCode = null;
        public String postalCode = null;
        public String[] refNum = new String[2];
        public Double amount = null;
        public boolean singleUse = false;
        public boolean isPresent = false;
        public boolean overflow = false;

        public GenericValue getValueObject(GenericDelegator delegator) {
            String entityName = null;
            Map lookupFields = null;
            if (paymentMethodId != null) {
                lookupFields = UtilMisc.toMap("paymentMethodId", paymentMethodId);
                entityName = "PaymentMethod";
            } else if (paymentMethodTypeId != null) {
                lookupFields = UtilMisc.toMap("paymentMethodTypeId", paymentMethodTypeId);
                entityName = "PaymentMethodType";
            } else {
                throw new IllegalArgumentException("Could not create value object because paymentMethodId and paymentMethodTypeId are null");
            }

            try {
                return delegator.findByPrimaryKeyCache(entityName, lookupFields);
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
            }

            return null;
        }

        public GenericValue getBillingAddress(GenericDelegator delegator) {
            GenericValue valueObj = this.getValueObject(delegator);
            GenericValue postalAddress = null;

            if ("PaymentMethod".equals(valueObj.getEntityName())) {
                String paymentMethodTypeId = valueObj.getString("paymentMethodTypeId");
                String paymentMethodId = valueObj.getString("paymentMethodId");
                Map lookupFields = UtilMisc.toMap("paymentMethodId", paymentMethodId);

                // billing account, credit card, gift card, eft account all have postal address
                try {
                    GenericValue pmObj = null;
                    if ("CREDIT_CARD".equals(paymentMethodTypeId)) {
                        pmObj = delegator.findByPrimaryKey("CreditCard", lookupFields);
                    } else if ("GIFT_CARD".equals(paymentMethodTypeId)) {
                        pmObj = delegator.findByPrimaryKey("GiftCard", lookupFields);
                    } else if ("EFT_ACCOUNT".equals(paymentMethodTypeId)) {
                        pmObj = delegator.findByPrimaryKey("EftAccount", lookupFields);
                    } else if ("EXT_BILLACT".equals(paymentMethodTypeId)) {
                        pmObj = delegator.findByPrimaryKey("BillingAccount", lookupFields);
                    }
                    if (pmObj != null) {
                        postalAddress = pmObj.getRelatedOne("PostalAddress");
                    } else {
                        Debug.logInfo("No PaymentMethod Object Found - " + paymentMethodId, module);
                    }
                } catch (GenericEntityException e) {
                    Debug.logError(e, module);
                }
            }

            return postalAddress;
        }

        public List makeOrderPaymentInfos(GenericDelegator delegator) {
            GenericValue valueObj = this.getValueObject(delegator);
            List values = new LinkedList();
            if (valueObj != null) {
                // first create a BILLING_LOCATION for the payment method address if there is one
                if ("PaymentMethod".equals(valueObj.getEntityName())) {
                    //String paymentMethodTypeId = valueObj.getString("paymentMethodTypeId");
                    //String paymentMethodId = valueObj.getString("paymentMethodId");
                    //Map lookupFields = UtilMisc.toMap("paymentMethodId", paymentMethodId);
                    String billingAddressId = null;

                    GenericValue billingAddress = this.getBillingAddress(delegator);
                    if (billingAddress != null) {
                        billingAddressId = billingAddress.getString("contactMechId");
                    }

                    if (UtilValidate.isNotEmpty(billingAddressId)) {
                        GenericValue orderCm = delegator.makeValue("OrderContactMech", null);
                        orderCm.set("contactMechPurposeTypeId", "BILLING_LOCATION");
                        orderCm.set("contactMechId", billingAddressId);
                        values.add(orderCm);
                    }
                }

                // create the OrderPaymentPreference record
                GenericValue opp = delegator.makeValue("OrderPaymentPreference", new HashMap());
                opp.set("paymentMethodTypeId", valueObj.getString("paymentMethodTypeId"));
                opp.set("presentFlag", isPresent ? "Y" : "N");
                opp.set("overflowFlag", overflow ? "Y" : "N");
                opp.set("paymentMethodId", paymentMethodId);
                opp.set("finAccountId", finAccountId);
                opp.set("billingPostalCode", postalCode);
                opp.set("maxAmount", amount);
                if (refNum != null) {
                    opp.set("manualRefNum", refNum[0]);
                    opp.set("manualAuthCode", refNum[1]);
                }
                if (securityCode != null) {
                    opp.set("securityCode", securityCode);
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
                Debug.log("Creating OrderPaymentPreference - " + opp, module);
                values.add(opp);
            }

            return values;
        }

        public int compareTo(Object o) {
            CartPaymentInfo that = (CartPaymentInfo) o;
            Debug.logInfo("Compare [" + this.toString() + "] to [" + that.toString() + "]", module);
            if (this.paymentMethodId != null) {
                if (that.paymentMethodId == null) {
                    return 1;
                } else {
                    int pmCmp = this.paymentMethodId.compareTo(that.paymentMethodId);
                    if (pmCmp == 0) {
                        if (this.refNum != null && this.refNum[0] != null) {
                            if (that.refNum != null && that.refNum[0] != null) {
                                return this.refNum[0].compareTo(that.refNum[0]);
                            } else {
                                return 1;
                            }
                        } else {
                            if (that.refNum != null && that.refNum[0] != null) {
                                return -1;
                            } else {
                                return 0;
                            }
                        }
                    } else {
                        return pmCmp;
                    }
                }
            } else {
                if (that.paymentMethodId != null) {
                    return -1;
                } else {
                    int pmtCmp = this.paymentMethodTypeId.compareTo(that.paymentMethodTypeId);
                    if (pmtCmp == 0) {
                        if (this.refNum != null && this.refNum[0] != null) {
                            if (that.refNum != null && that.refNum[0] != null) {
                                return this.refNum[0].compareTo(that.refNum[0]);
                            } else {
                                return 1;
                            }
                        } else {
                            if (that.refNum != null && that.refNum[0] != null) {
                                return -1;
                            } else {
                                return 0;
                            }
                        }
                    } else {
                        return pmtCmp;
                    }
                }
            }
        }

        public String toString() {
            return "Pm: " + paymentMethodId + " / PmType: " + paymentMethodTypeId + " / Amt: " + amount + " / Ref: " + refNum[0] + "!" + refNum[1];
        }
    }
    
    protected void finalize() throws Throwable {
        // DEJ20050518 we should not call clear because it kills the auto-save shopping list and is unnecessary given that when this object is GC'ed it will cause everything it points to that isn't referenced anywhere else to be GC'ed too: this.clear();
        super.finalize();
    }
}
