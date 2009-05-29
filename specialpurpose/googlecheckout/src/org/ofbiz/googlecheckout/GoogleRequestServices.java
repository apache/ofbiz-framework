/**
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
**/

package org.ofbiz.googlecheckout;

import java.util.List;
import java.util.Map;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.ServiceUtil;

import com.google.checkout.CheckoutException;
import com.google.checkout.MerchantInfo;
import com.google.checkout.orderprocessing.AddMerchantOrderNumberRequest;
import com.google.checkout.orderprocessing.ArchiveOrderRequest;
import com.google.checkout.orderprocessing.AuthorizeOrderRequest;
import com.google.checkout.orderprocessing.CancelOrderRequest;
import com.google.checkout.orderprocessing.ChargeOrderRequest;
import com.google.checkout.orderprocessing.RefundOrderRequest;
import com.google.checkout.orderprocessing.UnarchiveOrderRequest;
import com.google.checkout.orderprocessing.lineitem.CancelItemsRequest;
import com.google.checkout.orderprocessing.lineitem.ShipItemsRequest;

public class GoogleRequestServices {
    
    private static final String module = GoogleRequestServices.class.getName();
    
    public static Map<String, Object> sendOrderNumberRequest(DispatchContext dctx, Map<String, ? extends Object> context) {
        GenericDelegator delegator = dctx.getDelegator();
        String orderId = (String) context.get("orderId");
        GenericValue order = findGoogleOrder(delegator, orderId);
        if (order != null) {
            MerchantInfo mInfo = getMerchantInfo();
            String externalId = order.getString("externalId");
            if (UtilValidate.isNotEmpty(externalId)) {
                AddMerchantOrderNumberRequest aor = new AddMerchantOrderNumberRequest(mInfo, externalId, orderId);
                try {
                    aor.send();
                } catch (CheckoutException e) {
                    Debug.logError(e, module);
                }
            }
        }
        
        return ServiceUtil.returnSuccess();
    }
    
    public static Map<String, Object> sendAuthorizeRequest(DispatchContext dctx, Map<String, ? extends Object> context) {
        GenericDelegator delegator = dctx.getDelegator();
        String orderId = (String) context.get("orderId");
        GenericValue order = findGoogleOrder(delegator, orderId);
        if (order != null) {
            MerchantInfo mInfo = getMerchantInfo();
            String externalId = order.getString("externalId");
            if (UtilValidate.isNotEmpty(externalId)) {  
                AuthorizeOrderRequest aor = new AuthorizeOrderRequest(mInfo, externalId);
                try {
                    aor.send();
                } catch (CheckoutException e) {
                    Debug.logError(e, module);
                }
            }
        }
        
        return ServiceUtil.returnSuccess();
    }
    
    // trigger on captureOrderPayments
    public static Map<String, Object> sendChargeRequest(DispatchContext dctx, Map<String, ? extends Object> context) {
        GenericDelegator delegator = dctx.getDelegator();
        String orderId = (String) context.get("orderId");
        GenericValue order = findGoogleOrder(delegator, orderId);
        if (order != null) {
            MerchantInfo mInfo = getMerchantInfo();
            String externalId = order.getString("externalId");
            if (UtilValidate.isNotEmpty(externalId)) {
                Double amountToCharge = (Double) context.get("captureAmount");
                
                ChargeOrderRequest cor = new ChargeOrderRequest(mInfo, externalId, amountToCharge.floatValue());
                try {
                    cor.send();
                } catch (CheckoutException e) {
                    Debug.logError(e, module);
                }
            }
        }
        
        return ServiceUtil.returnSuccess();
    }
    
    // NOT IMPLEMENTED
    public static Map<String, Object> sendReturnRequest(DispatchContext dctx, Map<String, ? extends Object> context) {
        // Implement to use ReturnItemsRequest - send each item being returned
        
        // Check to see if the return is a refund return -- if so also send a RefundOrderRequest
          
        return ServiceUtil.returnSuccess();
    }
    
    public static Map<String, Object> sendShipRequest(DispatchContext dctx, Map<String, ? extends Object> context) {
        GenericDelegator delegator = dctx.getDelegator();
        String shipmentId = (String) context.get("shipmentId");
        try {
            sendItemsShipped(delegator, shipmentId);
        } catch (GeneralException e) {
            // TODO: handle the error
        }
                
        return ServiceUtil.returnSuccess();
    }
    
    public static Map<String, Object> sendOrderCancelRequest(DispatchContext dctx, Map<String, ? extends Object> context) {
        GenericDelegator delegator = dctx.getDelegator();
        String orderId = (String) context.get("orderId");
        GenericValue order = findGoogleOrder(delegator, orderId);
        if (order != null) {
            MerchantInfo mInfo = getMerchantInfo();
            String externalId = order.getString("externalId");
            if (UtilValidate.isNotEmpty(externalId)) {
                CancelOrderRequest cor = new CancelOrderRequest(mInfo, externalId, "Order Cancelled", ""); // TODO: configure the reason and comment
                try {
                    cor.send();
                } catch (CheckoutException e) {
                    Debug.logError(e, module);
                }
            }
        }
        
        return ServiceUtil.returnSuccess();
    }
    
    public static Map<String, Object> sendOrderItemCancelRequest(DispatchContext dctx, Map<String, ? extends Object> context) {
        GenericDelegator delegator = dctx.getDelegator();
        String orderId = (String) context.get("orderId");
        String orderItemSeqId = (String) context.get("orderItemSeqId");
        GenericValue order = findGoogleOrder(delegator, orderId);
        if (order != null) {
            GenericValue orderItem = null;
            try {
                orderItem = delegator.findOne("OrderItem", UtilMisc.toMap("orderId", orderId, "orderItemSeqId", orderItemSeqId), false);
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
            }
            
            if (orderItem != null) {
                MerchantInfo mInfo = getMerchantInfo();
                String externalId = order.getString("externalId");
                if (UtilValidate.isNotEmpty(externalId)) {
                    CancelItemsRequest cir = new CancelItemsRequest(mInfo, externalId, "Item Cancelled", ""); // TODO: configure the reason and comment
                    cir.addItem(orderItem.getString("productId"));
                    try {
                        cir.send();
                    } catch (CheckoutException e) {
                        Debug.logError(e, module);
                    }
                }
            }
        }
        
        return ServiceUtil.returnSuccess();
    }
    
    public static Map<String, Object> sendArchiveOrderRequest(DispatchContext dctx, Map<String, ? extends Object> context) {
        GenericDelegator delegator = dctx.getDelegator();
        String orderId = (String) context.get("orderId");
        GenericValue order = findGoogleOrder(delegator, orderId);
        if (order != null) {
            MerchantInfo mInfo = getMerchantInfo();
            String externalId = order.getString("externalId");
            if (UtilValidate.isNotEmpty(externalId)) {   
                ArchiveOrderRequest aor = new ArchiveOrderRequest(mInfo, externalId);
                try {
                    aor.send();
                } catch (CheckoutException e) {
                    Debug.logError(e, module);
                }
            }
        }
        
        return ServiceUtil.returnSuccess();
    }
    
    public static Map<String, Object> sendUnarchiveOrderRequest(DispatchContext dctx, Map<String, ? extends Object> context) {
        GenericDelegator delegator = dctx.getDelegator();
        String orderId = (String) context.get("orderId");
        GenericValue order = findGoogleOrder(delegator, orderId);
        if (order != null) {
            MerchantInfo mInfo = getMerchantInfo();
            String externalId = order.getString("externalId");
            if (UtilValidate.isNotEmpty(externalId)) {   
                UnarchiveOrderRequest uor = new UnarchiveOrderRequest(mInfo, externalId);
                try {
                    uor.send();
                } catch (CheckoutException e) {
                    Debug.logError(e, module);
                }
            }
        }
        
        return ServiceUtil.returnSuccess();
    }
    
    private static void sendItemsShipped(GenericDelegator delegator, String shipmentId) throws GeneralException {
        List<GenericValue> issued = delegator.findByAnd("ItemIssuance", UtilMisc.toMap("shipmentId", shipmentId));
        if (issued != null && issued.size() > 0) {
            for (GenericValue issue : issued) {
                String shipmentItemSeqId = issue.getString("shipmentItemSeqId"); 
                String productId = issue.getString("productId");
                String orderId = issue.getString("orderId");
                GenericValue order = findGoogleOrder(delegator, orderId);
                String externalId = order.getString("externalId");
                  
                if (externalId != null) {
                    // locate the shipment package content record
                    Map<String, ? extends Object> spcLup = UtilMisc.toMap("shipmentId", shipmentId, "shipmentItemSeqId", shipmentItemSeqId);

                    List<GenericValue> spc = delegator.findByAnd("ShipmentPackageContent", spcLup);
                    GenericValue packageContent = EntityUtil.getFirst(spc);
                    String carrier = null;
                    
                    if (packageContent != null) {
                        GenericValue shipPackage = packageContent.getRelatedOne("ShipmentPackage");

                        if (shipPackage != null) {
                            List<GenericValue> prs = shipPackage.getRelated("ShipmentPackageRouteSeg");
                            GenericValue packageRoute = EntityUtil.getFirst(prs);

                            if (packageRoute != null) {
                                List<GenericValue> srs = packageRoute.getRelated("ShipmentRouteSegment");
                                GenericValue route = EntityUtil.getFirst(srs);
                                String track = packageRoute.getString("trackingCode");

                                if (route != null) { 
                                    carrier = route.getString("carrierPartyId");
                                    if (UtilValidate.isEmpty(track)) {
                                        track = route.getString("trackingIdNumber");
                                    }

                                    try {
                                        ShipItemsRequest isr = new ShipItemsRequest(getMerchantInfo(), externalId);
                                        isr.addItemShippingInformation(productId, carrier, track);
                                        isr.send();
                                    } catch (CheckoutException e) {
                                        Debug.logError(e, module);
                                        throw new GeneralException(e);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    public static GenericValue findGoogleOrder(GenericDelegator delegator, String orderId) {
        GenericValue order = null;
        try {
            order = delegator.findOne("OrderHeader", false, "orderId", orderId);
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
        
        if (order != null) {
            String salesChannel = order.getString("salesChannelEnumId");
            if (GoogleCheckoutHelper.SALES_CHANNEL.equals(salesChannel)) {
                return order;
            }
        }
        
        return null;
    }
               
    public static MerchantInfo getMerchantInfo() {
        // TODO: implement this to pull data from the properties file
        MerchantInfo info = null;
        
        return info;
    }
}
