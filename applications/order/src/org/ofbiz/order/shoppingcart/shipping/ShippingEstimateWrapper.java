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
