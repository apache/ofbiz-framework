/*
 *
 * Copyright 2001-2006 The Apache Software Foundation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
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

        if (quantity == null) {
            quantity = new Double(1);
        }

        Debug.log("Pack input [" + productId + "] @ [" + quantity + "]", module);
        
        try {
            session.addOrIncreaseLine(orderId, null, shipGroupSeqId, productId, quantity.doubleValue(), packageSeq.intValue());
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

        // set the instructions -- will clear out previous if now null
        String instructions = (String) context.get("handlingInstructions");
        session.setHandlingInstructions(instructions);

        Map prdInfo = (Map) context.get("prdInfo");
        Map qtyInfo = (Map) context.get("qtyInfo");
        Map pkgInfo = (Map) context.get("pkgInfo");
        Map selInfo = (Map) context.get("selInfo");
        if (selInfo != null) {
            Iterator i = selInfo.keySet().iterator();
            while (i.hasNext()) {
                String orderItemSeqId = (String) i.next();
                String qtyStr = (String) qtyInfo.get(orderItemSeqId);
                String pkgStr = (String) pkgInfo.get(orderItemSeqId);
                String prdStr = (String) prdInfo.get(orderItemSeqId);
                if (UtilValidate.isEmpty(prdStr)) {
                    // set the productId to null if empty
                    prdStr = null;
                }
                Debug.log("Item: " + orderItemSeqId + " / Product: " + prdStr + " / Quantity: " + qtyStr + " /  Package: " + pkgStr, module);

                double quantity = 0;
                int packageSeq = 0;
                try {
                    quantity = Double.parseDouble(qtyStr);
                    packageSeq = Integer.parseInt(pkgStr);
                } catch (Exception e) {
                    return ServiceUtil.returnError(e.getMessage());
                }

                try {
                    session.addOrIncreaseLine(orderId, orderItemSeqId, shipGroupSeqId, prdStr, quantity, packageSeq);
                } catch (GeneralException e) {
                    Debug.logError(e, module);
                    return ServiceUtil.returnError(e.getMessage());
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
        session.clear();

        return ServiceUtil.returnSuccess();
    }


    public static Map completePack(DispatchContext dctx, Map context) {
        PackingSession session = (PackingSession) context.get("packingSession");

        // set the instructions -- will clear out previous if now null
        String instructions = (String) context.get("handlingInstructions");
        session.setHandlingInstructions(instructions);

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
