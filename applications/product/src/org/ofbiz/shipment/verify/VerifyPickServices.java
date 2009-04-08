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

package org.ofbiz.shipment.verify;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import javolution.util.FastList;
import javolution.util.FastMap;

import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilGenerics;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.ServiceUtil;
import org.ofbiz.service.LocalDispatcher;

public class VerifyPickServices {

    private static BigDecimal ZERO = BigDecimal.ZERO;

    public static Map<String, Object> verifySingleItem(DispatchContext dctx, Map<String, ? extends Object> context) {
        GenericDelegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        VerifyPickSession pickSession = (VerifyPickSession) context.get("verifyPickSession");
        String orderId = (String) context.get("orderId");
        String facilityId = (String) context.get("facilityId");
        String shipGroupSeqId = (String) context.get("shipGroupSeqId");
        String productId = (String) context.get("productId");
        BigDecimal quantity = (BigDecimal) context.get("quantity");
        List<String> orderItemSeqIds = FastList.newInstance();
        boolean isProductId = false;
        BigDecimal qtyToVerify = ZERO;
        BigDecimal qtyToVerified = ZERO;
        BigDecimal verifiedQuantity = ZERO;
        BigDecimal readyToVerifyQty = ZERO;
        int counter = 0;
        try {
            List<GenericValue> orderItems = delegator.findByAnd("OrderItem", UtilMisc.toMap("orderId", orderId));
            for (GenericValue orderItem : orderItems) {
                if (productId.equals(orderItem.getString("productId"))) {
                    orderItemSeqIds.add(orderItem.getString("orderItemSeqId"));
                    isProductId = true;
                }
            }
            if (isProductId) {
                for (String orderItemSeqId : orderItemSeqIds) {
                    counter++;
                    if (quantity.compareTo(ZERO) > 0) {
                        GenericValue orderItem = delegator.findOne("OrderItem", UtilMisc.toMap("orderId", orderId , "orderItemSeqId", orderItemSeqId), false);
                        BigDecimal orderedQuantity = orderItem.getBigDecimal("quantity");
                        List<GenericValue> shipments = delegator.findByAnd("Shipment", UtilMisc.toMap("primaryOrderId", orderId , "statusId", "SHIPMENT_PICKED"));
                        for(GenericValue shipment : shipments) {
                            List<GenericValue> orderShipments = shipment.getRelatedByAnd("OrderShipment" , UtilMisc.toMap("orderId", orderId, "orderItemSeqId", orderItemSeqId));
                            for(GenericValue orderShipment : orderShipments) {
                                verifiedQuantity = verifiedQuantity.add(orderShipment.getBigDecimal("quantity"));
                            }
                        }
                        qtyToVerify = orderedQuantity.subtract(verifiedQuantity);
                        if (qtyToVerify.compareTo(quantity) > -1) {
                            readyToVerifyQty = pickSession.getReadyToVerifyQuantity(orderId, orderItemSeqId);
                            qtyToVerified =  orderedQuantity.subtract(readyToVerifyQty);
                            if (qtyToVerified.compareTo(quantity) > -1) {
                                pickSession.createRow(orderId, orderItemSeqId, shipGroupSeqId, productId, quantity, facilityId, orderItem);
                                counter--;
                                break;
                            } else if (orderItems.size() == counter) {
                                throw new GeneralException("No valid order item found for product ["+productId+"] with quantity: "+quantity);
                            }
                        } else if (orderItemSeqIds.size() == counter) {
                            throw new GeneralException("No valid order item found for product ["+productId+"] with quantity: "+quantity);
                        }
                    }
                }
            } else {
                throw new GeneralException("No valid order item found for product ["+productId+"] with quantity: "+quantity);
            }
        } catch (Exception ex) {
            return ServiceUtil.returnError(ex.getMessage());
        }
        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> verifyBulkItem(DispatchContext dctx, Map<String, ? extends Object> context) {
        GenericDelegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        VerifyPickSession pickSession = (VerifyPickSession) context.get("verifyPickSession");
        String orderId = (String) context.get("orderId");
        String facilityId = (String) context.get("facilityId");
        String shipGroupSeqId = (String) context.get("shipGroupSeqId");
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Map<String, ?> selectedMap = UtilGenerics.checkMap(context.get("selectedMap"));
        Map<String, String> itemMap = UtilGenerics.checkMap(context.get("itemMap"));
        Map<String, String> productMap = UtilGenerics.checkMap(context.get("productMap"));
        Map<String, String> quantityMap = UtilGenerics.checkMap(context.get("quantityMap"));
        if (selectedMap != null) {
            for (String rowKey : selectedMap.keySet()) {
                String orderItemSeqId = itemMap.get(rowKey);
                String productId = productMap.get(rowKey);
                BigDecimal qtyToVerify = ZERO;
                BigDecimal qtyToVerified = ZERO;
                BigDecimal verifiedQuantity = ZERO;
                BigDecimal readyToVerifyQty = ZERO;
                BigDecimal quantity = new BigDecimal(quantityMap.get(rowKey));
                if (quantity.compareTo(ZERO) > 0) {
                    try {
                        GenericValue orderItem = delegator.findOne("OrderItem", UtilMisc.toMap("orderId", orderId , "orderItemSeqId", orderItemSeqId), false);
                        BigDecimal orderedQuantity = orderItem.getBigDecimal("quantity");
                        List<GenericValue> shipments = delegator.findByAnd("Shipment", UtilMisc.toMap("primaryOrderId", orderId , "statusId", "SHIPMENT_PICKED"));
                        for(GenericValue shipment : shipments) {
                            List<GenericValue> orderShipments = shipment.getRelatedByAnd("OrderShipment" , UtilMisc.toMap("orderId", orderId, "orderItemSeqId", orderItemSeqId));
                            for(GenericValue orderShipment : orderShipments) {
                                verifiedQuantity = verifiedQuantity.add(orderShipment.getBigDecimal("quantity"));
                            }
                        }
                        qtyToVerify = orderedQuantity.subtract(verifiedQuantity);
                        if (qtyToVerify.compareTo(quantity) > -1) {
                            readyToVerifyQty = pickSession.getReadyToVerifyQuantity(orderId,orderItemSeqId);
                            qtyToVerified =  orderedQuantity.subtract(readyToVerifyQty);
                            if (qtyToVerified.compareTo(quantity) > -1) {
                                pickSession.createRow(orderId, orderItemSeqId, shipGroupSeqId, productId, quantity, facilityId, orderItem);
                            } else {
                                throw new GeneralException("Quantity to Verify is more than the Quantity left to Verify (orderedQuantity - readyToVerifiedQty)");
                            }
                        } else {
                            throw new GeneralException("Quantity to Verify is more than the Quantity left to Verify (orderedQuantity - verifiedQuantity)");
                        }
                    } catch (Exception ex) {
                        return ServiceUtil.returnError(ex.getMessage());
                    }
                }
            }
        }
        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> completeVerifiedPick(DispatchContext dctx, Map<String, ? extends Object> context) {
        GenericDelegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        String shipmentId = null;
        VerifyPickSession pickSession = (VerifyPickSession) context.get("verifyPickSession");
        String orderId = (String) context.get("orderId");
        String facilityId = (String) context.get("facilityId");
        String shipGroupSeqId = (String) context.get("shipGroupSeqId");
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        try {
            shipmentId = pickSession.complete(orderId);
            Map<String, Object> shipment = FastMap.newInstance();
            shipment.put("shipmentId", shipmentId);
            pickSession.clearAllRows();
            return shipment;
        } catch (GeneralException ex) {
            return ServiceUtil.returnError(ex.getMessage(), ex.getMessageList());
        }
    }

    public static Map<String, Object> cancelAllRows(DispatchContext dctx, Map<String, ? extends Object> context) {
        VerifyPickSession session = (VerifyPickSession) context.get("verifyPickSession");
        session.clearAllRows();
        return ServiceUtil.returnSuccess();
    }
}