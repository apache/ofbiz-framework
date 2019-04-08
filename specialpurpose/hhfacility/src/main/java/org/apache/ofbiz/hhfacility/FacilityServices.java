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

package org.apache.ofbiz.hhfacility;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralRuntimeException;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.product.product.ProductWorker;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;

public class FacilityServices {

    public static final String module = FacilityServices.class.getName();
    private static final String resource = "ProductUiLabels";

    public static Map<String, Object> findProductsById(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        String idValue = (String) context.get("idValue");
        List<GenericValue> productsFound = null;

        try {
            productsFound = ProductWorker.findProductsById(delegator, idValue, null, false, true);
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            throw new GeneralRuntimeException(e.getMessage());
        }

        // Send back the results
        Map<String, Object> result = ServiceUtil.returnSuccess();
        if (UtilValidate.isNotEmpty(productsFound)) {
            result.put("productList", productsFound);
        }
        return result;
    }

    public static Map<String, Object> fixProductNegativeQOH(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Locale locale = (Locale) context.get("locale");
        String facilityId = (String) context.get("facilityId");
        String productId = (String) context.get("productId");
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        // Now we build a list of inventory items against the facility and product.
        // todo: change this to a select from inv_items where productId and facilityId matches distinct (locationSeqId).
        List<GenericValue> invItemList = null;
        try {
            invItemList = EntityQuery.use(delegator).from("InventoryItem").where("productId", productId, "facilityId", facilityId).queryList();
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            throw new GeneralRuntimeException(e.getMessage());
        }

        for (GenericValue invItem : invItemList) {
            if (invItem != null) {
                int qoh = ((Double)invItem.get("quantityOnHandTotal")).intValue();

                if (qoh < 0) {
                    // Got a negative qoh so lets balance if off to zero.
                    Map<String, Object> contextInput = UtilMisc.toMap("userLogin", userLogin, "inventoryItemId", invItem.get("inventoryItemId"), "varianceReasonId", "VAR_LOST", "availableToPromiseVar", new Double(qoh*-1), "quantityOnHandVar", new Double(qoh*-1), "comments", "QOH < 0 stocktake correction");
                    try {
                        dispatcher.runSync("createPhysicalInventoryAndVariance",contextInput);
                    } catch (GenericServiceException e) {
                        Debug.logError(e, "fixProductNegativeQOH failed on createPhysicalInventoryAndVariance invItemId"+invItem.get("inventoryItemId"), module);
                        return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ProductErrorCreatePhysicalInventoryAndVariance", UtilMisc.toMap("inventoryItemId", invItem.get("inventoryItemId")), locale));
                    }
                }
            }
        }
        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> updateProductStocktake(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Locale locale = (Locale) context.get("locale");
        String facilityId = (String) context.get("facilityId");
        String productId = (String) context.get("productId");
        String locationSeqId = (String) context.get("locationSeqId");
        BigDecimal quantity = (BigDecimal) context.get("quantity");
        if (UtilValidate.isEmpty(productId) || UtilValidate.isEmpty(facilityId)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ProductIdOrFacilityIdNotFound", locale));
        }

        // First identify the location and get a list of inventoryItemIds for that location.
        if (UtilValidate.isEmpty(locationSeqId)) {
            // Assume this is the null field version
            locationSeqId = "nullField";
        }

        // Get the current atp/qoh values for the location(s).
        Map<String, Object> contextInput = UtilMisc.toMap("productId", (Object) productId, "facilityId", facilityId, "locationSeqId", locationSeqId);
        Map<String, Object> invAvailability = null;
        try {
            invAvailability = dispatcher.runSync("getInventoryAvailableByLocation",contextInput);
        } catch (GenericServiceException e) {
            Debug.logError(e, "updateProductStocktake failed getting inventory counts", module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ProductErrorUpdateProductStockTake", locale));
        }
        int qoh = ((BigDecimal)invAvailability.get("quantityOnHandTotal")).intValue();
        if (quantity.intValue() == qoh) {
            // No change required.
            Debug.logInfo("updateProductStocktake No change required quantity("+quantity+") = qoh("+qoh+")", module);
            return ServiceUtil.returnSuccess();
        }

        // Now get the inventory items that are found for that location, facility and product
        List<GenericValue> invItemList = null;
        try {
            invItemList = EntityQuery.use(delegator).from("InventoryItem").where("productId", productId, "facilityId", facilityId, "locationSeqId", locationSeqId).queryList();
        } catch (GenericEntityException e) {
            Debug.logError(e, "updateProductStocktake failed getting inventory items", module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ProductErrorFailedProductStockTake", locale));
        }

        for (GenericValue invItem : invItemList) {
            String locationFound = invItem.getString("locationSeqId");
            Debug.logInfo("updateProductStocktake: InvItemId("+invItem.getString("inventoryItemId")+")", module);
            if (locationFound == null) {
                locationFound = "nullField";
            }
        }
        // Check if there is a request to change the locationSeqId
        try {
            dispatcher.runSync("getInventoryAvailableByFacility", UtilMisc.toMap("productId", productId, "facilityId", facilityId));
        } catch (GenericServiceException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ProductInventoryItemLookupProblem", UtilMisc.toMap("errorString", e.getMessage()), locale));
        }
        return ServiceUtil.returnSuccess();
    }
}
