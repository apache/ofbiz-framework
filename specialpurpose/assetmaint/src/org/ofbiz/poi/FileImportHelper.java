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

package org.ofbiz.poi;

import java.util.HashMap;
import java.util.Map;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;

public class FileImportHelper {

    static String module = FileImportHelper.class.getName();

    // prepare the product map
    public static Map prepareProduct(String productId) {
        Map fields = new HashMap();
        fields.put("productId", productId);
        fields.put("productTypeId", "FINISHED_GOOD");
        fields.put("internalName", "Product_" + productId);
        fields.put("isVirtual", "N");
        fields.put("isVariant", "N");
        return fields;
    }

    // prepare the inventoryItem map
    public static Map prepareInventoryItem(String productId,
            double quantityOnHand, String inventoryItemId) {
        Map fields = new HashMap();
        fields.put("inventoryItemId", inventoryItemId);
        fields.put("inventoryItemTypeId", "NON_SERIAL_INV_ITEM");
        fields.put("productId", productId);
        fields.put("ownerPartyId", "Company");
        fields.put("facilityId", "WebStoreWarehouse");
        fields.put("quantityOnHandTotal", new Double(quantityOnHand));
        fields.put("availableToPromiseTotal", new Double(quantityOnHand));
        return fields;
    }

    // check if product already exists in database
    public static boolean checkProductExists(String productId,
            GenericDelegator delegator) {
        GenericValue tmpProductGV;
        boolean productExists = false;
        try {
            tmpProductGV = delegator.findByPrimaryKey("Product", UtilMisc
                .toMap("productId", productId));
            if (tmpProductGV != null
                    && tmpProductGV.getString("productId") == productId)
                productExists = true;
        } catch (GenericEntityException e) {
            Debug.logError("Problem in reading data of product", module);
        }
        return productExists;
    }
}