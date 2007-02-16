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

        Debug.log("Pack input [" + productId + "] @ [" + quantity + "]", module);
        
        try {
            session.addOrIncreaseLine(orderId, null, shipGroupSeqId, productId, quantity.doubleValue(), packageSeq.intValue(), false);
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

        if (selInfo != null) {
            Iterator i = selInfo.keySet().iterator();
            while (i.hasNext()) {
                String orderItemSeqId = (String) i.next();
                String prdStr = (String) prdInfo.get(orderItemSeqId);
                if (UtilValidate.isEmpty(prdStr)) {
                    // set the productId to null if empty
                    prdStr = null;
                }

                // base package/quantity strings
                String pkgStr = (String) pkgInfo.get(orderItemSeqId);
                String qtyStr = (String) qtyInfo.get(orderItemSeqId);

                Debug.log("Item: " + orderItemSeqId + " / Product: " + prdStr + " / Quantity: " + qtyStr + " /  Package: " + pkgStr, module);

                // array place holders
                String[] quantities;
                String[] packages;

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

                for (int p = 0; p < packages.length; p++) {
                    double quantity;
                    int packageSeq;
                    try {
                        quantity = Double.parseDouble(quantities[p]);
                        packageSeq = Integer.parseInt(packages[p]);
                    } catch (Exception e) {
                        return ServiceUtil.returnError(e.getMessage());
                    }

                    try {
                        session.addOrIncreaseLine(orderId, orderItemSeqId, shipGroupSeqId, prdStr, quantity, packageSeq, updateQuantity.booleanValue());
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


    public static Map completePack(DispatchContext dctx, Map context) {
        PackingSession session = (PackingSession) context.get("packingSession");

        // set the instructions -- will clear out previous if now null
        String instructions = (String) context.get("handlingInstructions");
        String pickerPartyId = (String) context.get("pickerPartyId");
        Double additionalShippingCharge = (Double) context.get("additionalShippingCharge");
        session.setHandlingInstructions(instructions);
        session.setPickerPartyId(pickerPartyId);
        session.setAdditionalShippingCharge(additionalShippingCharge);

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
}
