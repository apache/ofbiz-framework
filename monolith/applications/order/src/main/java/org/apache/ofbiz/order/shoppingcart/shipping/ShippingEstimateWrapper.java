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
package org.apache.ofbiz.order.shoppingcart.shipping;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.order.shoppingcart.ShoppingCart;
import org.apache.ofbiz.order.shoppingcart.ShoppingCartItem;
import org.apache.ofbiz.product.store.ProductStoreWorker;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;

public class ShippingEstimateWrapper {

    private static final String MODULE = ShippingEstimateWrapper.class.getName();

    private Delegator delegator = null;
    private LocalDispatcher dispatcher = null;

    private Map<GenericValue, BigDecimal> shippingEstimates = null;
    private List<GenericValue> shippingTimeEstimates = null;
    private List<GenericValue> shippingMethods = null;

    private GenericValue shippingAddress = null;
    private GenericValue originAddress = null;
    private Map<String, BigDecimal> shippableItemFeatures = null;
    private List<BigDecimal> shippableItemSizes = null;
    private List<Map<String, Object>> shippableItemInfo = null;
    private String productStoreId = null;
    private BigDecimal shippableQuantity = BigDecimal.ZERO;
    private BigDecimal shippableWeight = BigDecimal.ZERO;
    private BigDecimal shippableTotal = BigDecimal.ZERO;
    private String partyId = null;
    private String supplierPartyId = null;

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
        this.originAddress = cart.getOriginAddress(shipGroup);
        this.productStoreId = cart.getProductStoreId();
        this.partyId = cart.getPartyId();
        this.supplierPartyId = cart.getSupplierPartyId(shipGroup);

        BigDecimal totalAllowance = BigDecimal.ZERO;
        if (UtilValidate.isNotEmpty(cart.getShipGroupItems(shipGroup))) {
            try {
                for (ShoppingCartItem item : cart.getShipGroupItems(shipGroup).keySet()) {
                    GenericValue allowanceProductPrice = EntityQuery.use(delegator).from("ProductPrice").where("productPriceTypeId",
                            "SHIPPING_ALLOWANCE", "productId", item.getProductId()).filterByDate().queryFirst();
                    if (allowanceProductPrice != null && UtilValidate.isNotEmpty(allowanceProductPrice.get("price"))) {
                        totalAllowance = totalAllowance.add(allowanceProductPrice.getBigDecimal("price")).multiply(item.getQuantity());
                    }
                }
            } catch (GenericEntityException gee) {
                Debug.logError(gee.getMessage(), MODULE);
            }
        }
        this.loadShippingMethods();
        this.loadEstimates(totalAllowance);
        this.loadShipmentTimeEstimates();
    }

    /**
     * Load shipping methods.
     */
    protected void loadShippingMethods() {
        try {
            this.shippingMethods = ProductStoreWorker.getAvailableStoreShippingMethods(delegator, productStoreId,
                    shippingAddress, shippableItemSizes, shippableItemFeatures, shippableWeight, shippableTotal);
        } catch (Throwable t) {
            Debug.logError(t, MODULE);
        }
    }

    /**
     * Load estimates.
     * @param totalAllowance the total allowance
     */
    protected void loadEstimates(BigDecimal totalAllowance) {
        this.shippingEstimates = new HashMap<>();
        if (shippingMethods != null) {
            for (GenericValue shipMethod : shippingMethods) {
                String shippingMethodTypeId = shipMethod.getString("shipmentMethodTypeId");
                String carrierRoleTypeId = shipMethod.getString("roleTypeId");
                String carrierPartyId = shipMethod.getString("partyId");
                String productStoreShipMethId = shipMethod.getString("productStoreShipMethId");
                String shippingCmId = shippingAddress != null ? shippingAddress.getString("contactMechId") : null;

                Map<String, Object> estimateMap = ShippingEvents.getShipGroupEstimate(dispatcher, delegator, "SALES_ORDER",
                        shippingMethodTypeId, carrierPartyId, carrierRoleTypeId, shippingCmId, productStoreId,
                        supplierPartyId, shippableItemInfo, shippableWeight, shippableQuantity, shippableTotal, partyId, productStoreShipMethId,
                        totalAllowance);

                if (ServiceUtil.isSuccess(estimateMap)) {
                    BigDecimal shippingTotal = (BigDecimal) estimateMap.get("shippingTotal");
                    shippingEstimates.put(shipMethod, shippingTotal);
                }
            }
        }
    }

    /**
     * Load shipment time estimates.
     */
    protected void loadShipmentTimeEstimates() {
        this.shippingTimeEstimates = new LinkedList<>();

        if (shippingMethods != null) {
            for (GenericValue shipMethod : shippingMethods) {
                String shipmentMethodTypeId = shipMethod.getString("shipmentMethodTypeId");
                String carrierRoleTypeId = shipMethod.getString("roleTypeId");
                String carrierPartyId = shipMethod.getString("partyId");

                shippingTimeEstimates.addAll(ShippingEvents.getShipmentTimeEstimates(delegator, shipmentMethodTypeId, carrierPartyId,
                        carrierRoleTypeId, shippingAddress, originAddress));
            }
        }
    }

    /**
     * Gets shipping methods.
     * @return the shipping methods
     */
    public List<GenericValue> getShippingMethods() {
        return shippingMethods;
    }

    /**
     * Gets all estimates.
     * @return the all estimates
     */
    public Map<GenericValue, BigDecimal> getAllEstimates() {
        return shippingEstimates;
    }

    /**
     * Gets shipping estimate.
     * @param storeCarrierShipMethod the store carrier ship method
     * @return the shipping estimate
     */
    public BigDecimal getShippingEstimate(GenericValue storeCarrierShipMethod) {
        return shippingEstimates.get(storeCarrierShipMethod);
    }

    /**
     * Gets shipping time estimates.
     * @return the shipping time estimates
     */
    public List<GenericValue> getShippingTimeEstimates() {
        return shippingTimeEstimates;
    }

    /**
     * Gets shipping time estimate in day.
     * @param storeCarrierShipMethod the store carrier ship method
     * @return the shipping time estimate in day
     */
    public Double getShippingTimeEstimateInDay(GenericValue storeCarrierShipMethod) {
        return ShippingEvents.getShippingTimeEstimateInDay(dispatcher, storeCarrierShipMethod, shippingTimeEstimates);
    }
}
