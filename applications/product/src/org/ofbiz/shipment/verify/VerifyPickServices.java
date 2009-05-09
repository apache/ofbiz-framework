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
import java.util.Locale;
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
        Locale locale = (Locale) context.get("locale");
        VerifyPickSession pickSession = (VerifyPickSession) context.get("verifyPickSession");
        String orderId = (String) context.get("orderId");
        String shipGroupSeqId = (String) context.get("shipGroupSeqId");
        String productId = (String) context.get("productId");
        BigDecimal quantity = (BigDecimal) context.get("quantity");
        if (quantity != null) {
            try {
                pickSession.createRow(orderId, null, shipGroupSeqId, productId, quantity, locale);
            } catch (GeneralException e) {
                return ServiceUtil.returnError(e.getMessage());
            }
        }
        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> verifyBulkItem(DispatchContext dctx, Map<String, ? extends Object> context) {
        GenericDelegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Locale locale = (Locale) context.get("locale");
        VerifyPickSession pickSession = (VerifyPickSession) context.get("verifyPickSession");
        String orderId = (String) context.get("orderId");
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
                BigDecimal quantity = new BigDecimal(quantityMap.get(rowKey));
                if (quantity.compareTo(ZERO) > 0) {
                    try {
                        pickSession.createRow(orderId, orderItemSeqId, shipGroupSeqId, productId, quantity, locale);
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
        Locale locale = (Locale) context.get("locale");
        String shipmentId = null;
        VerifyPickSession pickSession = (VerifyPickSession) context.get("verifyPickSession");
        String orderId = (String) context.get("orderId");
        try {
            shipmentId = pickSession.complete(orderId, locale);
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