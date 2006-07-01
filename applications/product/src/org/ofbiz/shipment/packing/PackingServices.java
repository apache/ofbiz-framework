/*
 * $Id$
 *
 * Copyright (c) 2001-2005 The Open For Business Project - www.ofbiz.org
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
package org.ofbiz.shipment.packing;

import java.util.Iterator;
import java.util.Map;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.ServiceUtil;

/**
 *
 * @author     <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a>
 * @version    $Rev$
 * @since      Sep 1, 2005
 */
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
