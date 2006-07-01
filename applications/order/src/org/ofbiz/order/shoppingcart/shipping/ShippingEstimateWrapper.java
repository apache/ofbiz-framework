/*
 * $Id: ShippingEstimateWrapper.java 5462 2005-08-05 18:35:48Z jonesde $
 *
 * Copyright (c) 2004 The Open For Business Project - www.ofbiz.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 * CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT
 * OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 * THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */
package org.ofbiz.order.shoppingcart.shipping;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.ofbiz.base.util.Debug;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.order.shoppingcart.ShoppingCart;
import org.ofbiz.product.store.ProductStoreWorker;
import org.ofbiz.service.LocalDispatcher;

/**
 * 
 * @author     <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a>
 * @version    $Rev$
 * @since      3.2
 */
public class ShippingEstimateWrapper {

    public static final String module = ShippingEstimateWrapper.class.getName();

    protected GenericDelegator delegator = null;
    protected LocalDispatcher dispatcher = null;

    protected Map shippingEstimates = null;
    protected List shippingMethods = null;

    protected GenericValue shippingAddress = null;
    protected Map shippableItemFeatures = null;
    protected List shippableItemSizes = null;
    protected List shippableItemInfo = null;
    protected String productStoreId = null;
    protected double shippableQuantity = 0;
    protected double shippableWeight = 0;
    protected double shippableTotal = 0;

    public static ShippingEstimateWrapper getWrapper(LocalDispatcher dispatcher, ShoppingCart cart, int shipGroup) {
        return new ShippingEstimateWrapper(dispatcher, cart, shipGroup);
    }

    public ShippingEstimateWrapper(LocalDispatcher dispatcher, ShoppingCart cart, int shipGroup) {
        this.dispatcher = dispatcher;
        this.delegator = cart.getDelegator();

        this.shippableItemFeatures = cart.getFeatureIdQtyMap(shipGroup);
        this.shippableItemSizes = cart.getShippableSizes(shipGroup);
        this.shippableItemInfo = cart.getShippableItemInfo(shipGroup);
        this.shippableQuantity = cart.getShippableQuantity(shipGroup);
        this.shippableWeight = cart.getShippableWeight(shipGroup);
        this.shippableTotal = cart.getShippableTotal(shipGroup);
        this.shippingAddress = cart.getShippingAddress(shipGroup);
        this.productStoreId = cart.getProductStoreId();

        this.loadShippingMethods();
        this.loadEstimates();
    }

    protected void loadShippingMethods() {
        try {
            this.shippingMethods = ProductStoreWorker.getAvailableStoreShippingMethods(delegator, productStoreId,
                    shippingAddress, shippableItemSizes, shippableItemFeatures, shippableWeight, shippableTotal);
        } catch (Throwable t) {
            Debug.logError(t, module);
        }
    }

    protected void loadEstimates() {
        this.shippingEstimates = new HashMap();
        if (shippingMethods != null) {
            Iterator i = shippingMethods.iterator();
            while (i.hasNext()) {
                GenericValue shipMethod = (GenericValue) i.next();
                String shippingMethodTypeId = shipMethod.getString("shipmentMethodTypeId");
                String carrierRoleTypeId = shipMethod.getString("roleTypeId");
                String carrierPartyId = shipMethod.getString("partyId");
                String shippingCmId = shippingAddress != null ? shippingAddress.getString("contactMechId") : null;

                Map estimateMap = ShippingEvents.getShipGroupEstimate(dispatcher, delegator, "SALES_ORDER",
                        shippingMethodTypeId, carrierPartyId, carrierRoleTypeId, shippingCmId, productStoreId,
                        shippableItemInfo, shippableWeight, shippableQuantity, shippableTotal);

                Double shippingTotal = (Double) estimateMap.get("shippingTotal");
                shippingEstimates.put(shipMethod, shippingTotal);
            }
        }
    }

    public List getShippingMethods() {
        return shippingMethods;
    }

    public Map getAllEstimates() {
        return shippingEstimates;
    }

    public Double getShippingEstimate(GenericValue storeCarrierShipMethod) {
        return (Double) shippingEstimates.get(storeCarrierShipMethod);
    }

}
