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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.order.order.OrderReadHelper;
import org.ofbiz.order.shoppingcart.ShoppingCart;
import org.ofbiz.product.store.ProductStoreWorker;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ModelService;
import org.ofbiz.service.ServiceUtil;
import org.ofbiz.base.util.UtilMisc;

/**
 * ShippingEvents - Events used for processing shipping fees
 */
public class ShippingEvents {

    public static final String module = ShippingEvents.class.getName();

    public static String getShipEstimate(HttpServletRequest request, HttpServletResponse response) {
        ShoppingCart cart = (ShoppingCart) request.getSession().getAttribute("shoppingCart");
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        GenericDelegator delegator = (GenericDelegator) request.getAttribute("delegator");

        int shipGroups = cart.getShipGroupSize();
        for (int i = 0; i < shipGroups; i++) {
            String shipmentMethodTypeId = cart.getShipmentMethodTypeId(i);
            if(UtilValidate.isEmpty(shipmentMethodTypeId)){
                continue;
            }
            Map result = getShipGroupEstimate(dispatcher, delegator, cart, i);
            ServiceUtil.getMessages(request, result, null, "", "", "", "", null, null);
            if (result.get(ModelService.RESPONSE_MESSAGE).equals(ModelService.RESPOND_ERROR)) {
                return "error";
            }

            Double shippingTotal = (Double) result.get("shippingTotal");
            if (shippingTotal == null) {
                shippingTotal = new Double(0.00);
            }
            cart.setItemShipGroupEstimate(shippingTotal.doubleValue(), i);
        }

        // all done
        return "success";
    }

    public static Map getShipGroupEstimate(LocalDispatcher dispatcher, GenericDelegator delegator, ShoppingCart cart, int groupNo) {
        // check for shippable items
        if (!cart.shippingApplies()) {
            Map responseResult = ServiceUtil.returnSuccess();
            responseResult.put("shippingTotal", new Double(0.00));
            return responseResult;
        }

        String shipmentMethodTypeId = cart.getShipmentMethodTypeId(groupNo);
        String carrierPartyId = cart.getCarrierPartyId(groupNo);

        return getShipGroupEstimate(dispatcher, delegator, cart.getOrderType(), shipmentMethodTypeId, carrierPartyId, null,
                cart.getShippingContactMechId(groupNo), cart.getProductStoreId(), cart.getShippableItemInfo(groupNo),
                cart.getShippableWeight(groupNo), cart.getShippableQuantity(groupNo), cart.getShippableTotal(groupNo));
    }

    public static Map getShipEstimate(LocalDispatcher dispatcher, GenericDelegator delegator, OrderReadHelper orh, String shipGroupSeqId) {
        // check for shippable items
        if (!orh.shippingApplies()) {
            Map responseResult = ServiceUtil.returnSuccess();
            responseResult.put("shippingTotal", new Double(0.00));
            return responseResult;
        }

        GenericValue shipGroup = orh.getOrderItemShipGroup(shipGroupSeqId);
        String shipmentMethodTypeId = shipGroup.getString("shipmentMethodTypeId");
        String carrierRoleTypeId = shipGroup.getString("carrierRoleTypeId");
        String carrierPartyId = shipGroup.getString("carrierPartyId");

        GenericValue shipAddr = orh.getShippingAddress(shipGroupSeqId);
        if (shipAddr == null) {
            return UtilMisc.toMap("shippingTotal", new Double(0));
        }

        String contactMechId = shipAddr.getString("contactMechId");
        return getShipGroupEstimate(dispatcher, delegator, orh.getOrderTypeId(), shipmentMethodTypeId, carrierPartyId, carrierRoleTypeId,
                contactMechId, orh.getProductStoreId(), orh.getShippableItemInfo(shipGroupSeqId), orh.getShippableWeight(shipGroupSeqId).doubleValue(),
                orh.getShippableQuantity(shipGroupSeqId).doubleValue(), orh.getShippableTotal(shipGroupSeqId).doubleValue());
    }

    public static Map getShipGroupEstimate(LocalDispatcher dispatcher, GenericDelegator delegator, String orderTypeId,
            String shipmentMethodTypeId, String carrierPartyId, String carrierRoleTypeId, String shippingContactMechId,
            String productStoreId, List itemInfo, double shippableWeight, double shippableQuantity,
            double shippableTotal) {
        String standardMessage = "A problem occurred calculating shipping. Fees will be calculated offline.";
        List errorMessageList = new ArrayList();

        if (shipmentMethodTypeId == null || carrierPartyId == null) {
            if ("SALES_ORDER".equals(orderTypeId)) {
                errorMessageList.add("Please Select Your Shipping Method.");
                return ServiceUtil.returnError(errorMessageList);
            } else {
                return ServiceUtil.returnSuccess();
            }
        }

        if (carrierRoleTypeId == null) {
            carrierRoleTypeId = "CARRIER";
        }

//  ShipmentCostEstimate entity allows null value for geoIdTo field. So if geoIdTo is null we should be using orderFlatPrice for shipping cost.
//  So now calcShipmentCostEstimate service requires shippingContactMechId only if geoIdTo field has not null value.        
//        if (shippingContactMechId == null) {
//            errorMessageList.add("Please Select Your Shipping Address.");
//            return ServiceUtil.returnError(errorMessageList);
//        }

        // no shippable items; we won't change any shipping at all
        if (shippableQuantity == 0) {
            Map result = ServiceUtil.returnSuccess();
            result.put("shippingTotal", new Double(0));
            return result;
        }

        // check for an external service call
        GenericValue storeShipMethod = ProductStoreWorker.getProductStoreShipmentMethod(delegator, productStoreId,
                shipmentMethodTypeId, carrierPartyId, carrierRoleTypeId);

        if (storeShipMethod == null) {
            errorMessageList.add("No applicable shipment method found.");
            return ServiceUtil.returnError(errorMessageList);
        }

        // the initial amount before manual estimates
        double shippingTotal = 0.00;

        // prepare the service invocation fields
        Map serviceFields = new HashMap();
        serviceFields.put("initialEstimateAmt", new Double(shippingTotal));
        serviceFields.put("shippableTotal", new Double(shippableTotal));
        serviceFields.put("shippableQuantity", new Double(shippableQuantity));
        serviceFields.put("shippableWeight", new Double(shippableWeight));        
        serviceFields.put("shippableItemInfo", itemInfo);
        serviceFields.put("productStoreId", productStoreId);
        serviceFields.put("carrierRoleTypeId", "CARRIER");
        serviceFields.put("carrierPartyId", carrierPartyId);
        serviceFields.put("shipmentMethodTypeId", shipmentMethodTypeId);
        serviceFields.put("shippingContactMechId", shippingContactMechId);

        // call the external shipping service
        try {
            Double externalAmt = getExternalShipEstimate(dispatcher, storeShipMethod, serviceFields);
            if (externalAmt != null) {
                shippingTotal += externalAmt.doubleValue();
            }
        } catch (GeneralException e) {
            return ServiceUtil.returnError(standardMessage);
        }

        // update the initial amount
        serviceFields.put("initialEstimateAmt", new Double(shippingTotal));

        // call the generic estimate service
        try {
            Double genericAmt = getGenericShipEstimate(dispatcher, storeShipMethod, serviceFields);
            if (genericAmt != null) {
                shippingTotal += genericAmt.doubleValue();
            }
        } catch (GeneralException e) {
            return ServiceUtil.returnError(standardMessage);
        }

        // return the totals
        Map responseResult = ServiceUtil.returnSuccess();
        responseResult.put("shippingTotal", new Double(shippingTotal));
        return responseResult;
    }

    public static Double getGenericShipEstimate(LocalDispatcher dispatcher, GenericValue storeShipMeth, Map context) throws GeneralException {
        // invoke the generic estimate service next -- append to estimate amount
        Map genericEstimate = null;
        Double genericShipAmt = null;
        try {
            genericEstimate = dispatcher.runSync("calcShipmentCostEstimate", context);
        } catch (GenericServiceException e) {
            Debug.logError(e, "Shipment Service Error", module);
            throw new GeneralException();
        }
        if (ServiceUtil.isError(genericEstimate) || ServiceUtil.isFailure(genericEstimate)) {
            Debug.logError(ServiceUtil.getErrorMessage(genericEstimate), module);
            throw new GeneralException();
        } else if (ServiceUtil.isFailure(genericEstimate)) {
            genericShipAmt = new Double(-1);
        } else {
            genericShipAmt = (Double) genericEstimate.get("shippingEstimateAmount");
        }
        return genericShipAmt;
    }

    public static Double getExternalShipEstimate(LocalDispatcher dispatcher, GenericValue storeShipMeth, Map context) throws GeneralException {
        // invoke the external shipping estimate service
        String serviceName = (String)storeShipMeth.get("serviceName");
        Double externalShipAmt = null;
        if(serviceName != null){
            String doEstimates = UtilProperties.getPropertyValue("shipment.properties", "shipment.doratecheck", "true");
            //If all estimates are not turned off, check for the individual one
            if("true".equals(doEstimates)){
                String dothisEstimate = UtilProperties.getPropertyValue("shipment.properties", "shipment.doratecheck." + serviceName, "true");
                if("false".equals(dothisEstimate))
                 serviceName = null;
            } else {
                //Rate checks inhibited
                serviceName = null;
            }
        }
        if (( serviceName != null)) {
            String configProps = storeShipMeth.getString("configProps");
            if (UtilValidate.isNotEmpty(serviceName)) {
                // prepare the external service context
                context.put("serviceConfigProps", configProps);

                // invoke the service
                Map serviceResp = null;
                try {
                    Debug.log("Service : " + serviceName + " / " + configProps + " -- " + context, module);
                    // because we don't want to blow up too big or rollback the transaction when this happens, always have it run in its own transaction...
                    serviceResp = dispatcher.runSync(serviceName, context, 0, true);
                } catch (GenericServiceException e) {
                    Debug.logError(e, "Shipment Service Error", module);
                    throw new GeneralException(e);
                }
                if (ServiceUtil.isError(serviceResp)) {
                    String errMsg = "Error getting external shipment cost estimate: " + ServiceUtil.getErrorMessage(serviceResp); 
                    Debug.logError(errMsg, module);
                    throw new GeneralException(errMsg);
                } else if (ServiceUtil.isFailure(serviceResp)) {
                    String errMsg = "Failure getting external shipment cost estimate: " + ServiceUtil.getErrorMessage(serviceResp); 
                    Debug.logError(errMsg, module);
                    throw new GeneralException(errMsg);
                } else {
                    externalShipAmt = (Double) serviceResp.get("shippingEstimateAmount");
                }
            }
        }
        return externalShipAmt;
    }
}



