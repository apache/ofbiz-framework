package org.ofbiz.assetmaint;

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

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.product.product.ProductWorker;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;

public class FixedAssetMaintServices {
    
    public static final String module = FixedAssetMaintServices.class.getName();

    public static Map addPartFixedAssetMaint(DispatchContext ctx, Map context) {
        LocalDispatcher dispatcher = ctx.getDispatcher();
        GenericDelegator delegator = ctx.getDelegator();
        Locale locale = (Locale) context.get("locale");
        String fixedAssetId = (String)context.get("fixedAssetId");
        String maintHistSeqId = (String)context.get("maintHistSeqId");
        String productId = (String)context.get("productId");
        String facilityId = (String)context.get("facilityId");
        Double quantity = (Double)context.get("quantity");
        double requestedQty = quantity.doubleValue();
        
        try {
            GenericValue product = ProductWorker.findProduct(delegator, productId);
            if (product == null) {
                return ServiceUtil.returnError
                (UtilProperties.getMessage("AssetMaintUiLabels","AssetMaintInvalidPartProductIdError", UtilMisc.toMap("productId", productId), locale));
            }
            Map findCurrInventoryParams =  UtilMisc.toMap("productId", productId, "facilityId", facilityId);
            GenericValue userLogin = (GenericValue) context.get("userLogin");
            // Call issuance service
            Map result = dispatcher.runSync("getInventoryAvailableByFacility", findCurrInventoryParams);
            if (ServiceUtil.isError(result)) {
                return ServiceUtil.returnError("Problem in getting Inventory level for " + productId , null, null, result);
            }
            Object atpObj = result.get("availableToPromiseTotal");
            double atp = 0.0;
            if (atpObj != null) {
                atp = Double.parseDouble(atpObj.toString());
            }
            if (requestedQty > atp) {
                return ServiceUtil.returnError
                (UtilProperties.getMessage("AssetMaintUiLabels","AssetMaintLowPartInventoryError",UtilMisc.toMap("productId", productId , "quantity", Double.toString(atp)), locale));
            }
            List inventoryItems = delegator.findByAnd("InventoryItem", UtilMisc.toList(
                    new EntityExpr("productId", EntityOperator.EQUALS, productId),
                    new EntityExpr("facilityId", EntityOperator.EQUALS, facilityId),
                    new EntityExpr("availableToPromiseTotal", EntityOperator.GREATER_THAN, "0")));   //&& inventoryItems.size() > 0
            Iterator itr = inventoryItems.iterator();
            while (requestedQty > 0 && itr.hasNext()) {
                GenericValue inventoryItem = (GenericValue)itr.next();
                String inventoryItemId = inventoryItem.getString("inventoryItemId");
                atp = inventoryItem.getDouble("availableToPromiseTotal").doubleValue();
                findCurrInventoryParams =  UtilMisc.toMap("inventoryItemId",inventoryItemId);
                Double issueQuantity = null;
                if (requestedQty > atp) {
                    issueQuantity = new Double(atp);
                } else {
                    issueQuantity = new Double(requestedQty);
                }
                Map itemIssuanceCtx = new HashMap();
                itemIssuanceCtx.put("userLogin", userLogin);
                itemIssuanceCtx.put("inventoryItemId", inventoryItemId);
                itemIssuanceCtx.put("fixedAssetId", fixedAssetId);
                itemIssuanceCtx.put("maintHistSeqId", maintHistSeqId);
                itemIssuanceCtx.put("quantity", issueQuantity);
                // Call issuance service
                result = dispatcher.runSync("issueInventoryItemToFixedAssetMaint",itemIssuanceCtx);
                if (ServiceUtil.isError(result)) {
                    return ServiceUtil.returnError("Problem in calling service issueInventoryItemToFixedAssetMaint", null, null, result);
                }
                requestedQty = requestedQty - issueQuantity.doubleValue();
            }
        } catch (GenericEntityException e) {
            Debug.logError("Problem in retriving data from database", module);
        } catch (GenericServiceException e) {
            String msg = "Problem in calling service issueInventoryItemToFixedAssetMaint";
            Debug.logError(msg, module);
            return ServiceUtil.returnError(msg);
        }       
        return ServiceUtil.returnSuccess();
    }
}
