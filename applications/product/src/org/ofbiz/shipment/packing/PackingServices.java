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
package org.ofbiz.shipment.packing;

import java.util.Iterator;
import java.util.Map;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.ServiceUtil;

public class PackingServices {

    public static final String module = PackingServices.class.getName();

    public static Map addPackLine(DispatchContext dctx, Map context) {
        PackingSession session = (PackingSession) context.get("packingSession");
        String shipGroupSeqId = (String) context.get("shipGroupSeqId");
        String orderId = (String) context.get("orderId");
        String productId = (String) context.get("productId");
        Double quantity = (Double) context.get("quantity");
        Double weight = (Double) context.get("weight");
        Integer packageSeq = (Integer) context.get("packageSeq");

        // set the instructions -- will clear out previous if now null
        String instructions = (String) context.get("handlingInstructions");
        session.setHandlingInstructions(instructions);

        // set the picker party id -- will clear out previous if now null
        String pickerPartyId = (String) context.get("pickerPartyId");
        session.setPickerPartyId(pickerPartyId);

        if (quantity == null) {
            quantity = new Double(1);
        }

        Debug.log("OrderId [" + orderId + "] ship group [" + shipGroupSeqId + "] Pack input [" + productId + "] @ [" + quantity + "] packageSeq [" + packageSeq + "] weight [" + weight +"]", module);
        
        if (weight == null) {
            Debug.logWarning("OrderId [" + orderId + "] ship group [" + shipGroupSeqId + "] product [" + productId + "] being packed without a weight, assuming 0", module); 
            weight = new Double(0.0);
        }

        try {
            session.addOrIncreaseLine(orderId, null, shipGroupSeqId, productId, quantity.doubleValue(), packageSeq.intValue(), weight.doubleValue(), false);
        } catch (GeneralException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        return ServiceUtil.returnSuccess();
    }

    public static Map packBulk(DispatchContext dctx, Map context) {
        PackingSession session = (PackingSession) context.get("packingSession");
        String orderId = (String) context.get("orderId");
        String shipGroupSeqId = (String) context.get("shipGroupSeqId");
        Boolean updateQuantity = (Boolean) context.get("updateQuantity");
        if (updateQuantity == null) {
            updateQuantity = Boolean.FALSE;
        }

        // set the instructions -- will clear out previous if now null
        String instructions = (String) context.get("handlingInstructions");
        session.setHandlingInstructions(instructions);

        // set the picker party id -- will clear out previous if now null
        String pickerPartyId = (String) context.get("pickerPartyId");
        session.setPickerPartyId(pickerPartyId);

        Map selInfo = (Map) context.get("selInfo");
        Map prdInfo = (Map) context.get("prdInfo");
        Map qtyInfo = (Map) context.get("qtyInfo");
        Map pkgInfo = (Map) context.get("pkgInfo");
        Map wgtInfo = (Map) context.get("wgtInfo");

        if (selInfo != null) {
            Iterator i = selInfo.keySet().iterator();
            while (i.hasNext()) {
                String orderItemSeqId = (String) i.next();
                String prdStr = (String) prdInfo.get(orderItemSeqId);
                if (UtilValidate.isEmpty(prdStr)) {
                    // set the productId to null if empty
                    prdStr = null;
                }

                // base package/quantity/weight strings
                String pkgStr = (String) pkgInfo.get(orderItemSeqId);
                String qtyStr = (String) qtyInfo.get(orderItemSeqId);
                String wgtStr = (String) wgtInfo.get(orderItemSeqId);

                Debug.log("Item: " + orderItemSeqId + " / Product: " + prdStr + " / Quantity: " + qtyStr + " /  Package: " + pkgStr + " / Weight: " + wgtStr, module);

                // array place holders
                String[] quantities;
                String[] packages;
                String[] weights;

                // process the package array
                if (pkgStr.indexOf(",") != -1) {
                    // this is a multi-box update
                    packages = pkgStr.split(",");
                } else {
                    packages = new String[] { pkgStr };
                }

                // check to make sure there is at least one package
                if (packages == null || packages.length == 0) {
                    return ServiceUtil.returnError("No packages defined for processing.");
                }

                // process the quantity array
                if (qtyStr == null) {
                    quantities = new String[packages.length];
                    for (int p = 0; p < packages.length; p++) {
                        quantities[p] = (String) qtyInfo.get(orderItemSeqId + ":" + packages[p]);
                    }
                    if (quantities.length != packages.length) {
                        return ServiceUtil.returnError("Packages and quantities do not match.");
                    }
                } else {
                    quantities = new String[] { qtyStr };
                }
                
                // process the weight array
                if (UtilValidate.isEmpty(wgtStr)) wgtStr = "0";
                weights = new String[] { wgtStr };

                for (int p = 0; p < packages.length; p++) {
                    double quantity;
                    int packageSeq;
                    double weightSeq;
                    try {
                        quantity = Double.parseDouble(quantities[p]);
                        packageSeq = Integer.parseInt(packages[p]);
                        weightSeq = Double.parseDouble(weights[p]);
                    } catch (Exception e) {
                        return ServiceUtil.returnError(e.getMessage());
                    }

                    try {
                        session.addOrIncreaseLine(orderId, orderItemSeqId, shipGroupSeqId, prdStr, quantity, packageSeq, weightSeq, updateQuantity.booleanValue());
                    } catch (GeneralException e) {
                        Debug.logError(e, module);
                        return ServiceUtil.returnError(e.getMessage());
                    }
                }
            }
        }

        return ServiceUtil.returnSuccess();
    }

    public static Map incrementPackageSeq(DispatchContext dctx, Map context) {
        PackingSession session = (PackingSession) context.get("packingSession");
        int nextSeq = session.nextPackageSeq();
        Map result = ServiceUtil.returnSuccess();
        result.put("nextPackageSeq", new Integer(nextSeq));
        return result;
    }

    public static Map clearLastPackage(DispatchContext dctx, Map context) {
        PackingSession session = (PackingSession) context.get("packingSession");
        int nextSeq = session.clearLastPackage();
        Map result = ServiceUtil.returnSuccess();
        result.put("nextPackageSeq", new Integer(nextSeq));
        return result;
    }

    public static Map clearPackLine(DispatchContext dctx, Map context) {
        PackingSession session = (PackingSession) context.get("packingSession");
        String orderId = (String) context.get("orderId");
        String orderItemSeqId = (String) context.get("orderItemSeqId");
        String shipGroupSeqId = (String) context.get("shipGroupSeqId");
        String inventoryItemId = (String) context.get("inventoryItemId");
        Integer packageSeqId = (Integer) context.get("packageSeqId");

        PackingSessionLine line = session.findLine(orderId, orderItemSeqId, shipGroupSeqId,
                inventoryItemId, packageSeqId.intValue());

        // remove the line
        if (line != null) {
            session.clearLine(line);
        } else {
            return ServiceUtil.returnError("Packing line item not found; cannot clear.");
        }

        return ServiceUtil.returnSuccess();
    }

    public static Map clearPackAll(DispatchContext dctx, Map context) {
        PackingSession session = (PackingSession) context.get("packingSession");
        session.clearAllLines();

        return ServiceUtil.returnSuccess();
    }

    public static Map calcPackSessionAdditionalShippingCharge(DispatchContext dctx, Map context) {
        PackingSession session = (PackingSession) context.get("packingSession");
        Map packageWeights = (Map) context.get("packageWeights");
        String weightUomId = (String) context.get("weightUomId");
        String shippingContactMechId = (String) context.get("shippingContactMechId");
        String shipmentMethodTypeId = (String) context.get("shipmentMethodTypeId");
        String carrierPartyId = (String) context.get("carrierPartyId");
        String carrierRoleTypeId = (String) context.get("carrierRoleTypeId");
        String productStoreId = (String) context.get("productStoreId");
        
        double shippableWeight = setSessionPackageWeights(session, packageWeights);
        Double estimatedShipCost = session.getShipmentCostEstimate(shippingContactMechId, shipmentMethodTypeId, carrierPartyId, carrierRoleTypeId, productStoreId, null, null, new Double(shippableWeight), null);
        session.setAdditionalShippingCharge(estimatedShipCost);
        session.setWeightUomId(weightUomId);

        Map result = ServiceUtil.returnSuccess();
        result.put("additionalShippingCharge", estimatedShipCost);
        return result;
    }


    public static Map completePack(DispatchContext dctx, Map context) {
        PackingSession session = (PackingSession) context.get("packingSession");

        // set the instructions -- will clear out previous if now null
        String instructions = (String) context.get("handlingInstructions");
        String pickerPartyId = (String) context.get("pickerPartyId");
        Double additionalShippingCharge = (Double) context.get("additionalShippingCharge");
        Map packageWeights = (Map) context.get("packageWeights");
        String weightUomId = (String) context.get("weightUomId");
        session.setHandlingInstructions(instructions);
        session.setPickerPartyId(pickerPartyId);
        session.setAdditionalShippingCharge(additionalShippingCharge);
        session.setWeightUomId(weightUomId);
        setSessionPackageWeights(session, packageWeights);

        Boolean force = (Boolean) context.get("forceComplete");
        if (force == null) {
            force = Boolean.FALSE;
        }

        String shipmentId = null;
        try {
            shipmentId = session.complete(force.booleanValue());
        } catch (GeneralException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage(), e.getMessageList());
        }

        Map resp;
        if ("EMPTY".equals(shipmentId)) {
            resp = ServiceUtil.returnError("No items currently set to be shipped. Cannot complete!");
        } else {
            resp = ServiceUtil.returnSuccess("Shipment #" + shipmentId + " created and marked as PACKED.");
        }
        
        resp.put("shipmentId", shipmentId);
        return resp;
    }

    private static double setSessionPackageWeights(PackingSession session, Map packageWeights) {
        double shippableWeight = 0;
        if (! UtilValidate.isEmpty(packageWeights)) {
            Iterator pwit = packageWeights.keySet().iterator();
            while (pwit.hasNext()) {
                String packageSeqId = (String) pwit.next();
                String packageWeightStr = (String) packageWeights.get(packageSeqId);
                if (UtilValidate.isNotEmpty(packageWeightStr)) {
                    double packageWeight = UtilMisc.toDouble(packageWeights.get(packageSeqId));
                    session.setPackageWeight(Integer.parseInt(packageSeqId), new Double(packageWeight));
                    shippableWeight += packageWeight;
                } else {
                    session.setPackageWeight(Integer.parseInt(packageSeqId), null);
                }
            }
        }
        return shippableWeight;
    }
}
