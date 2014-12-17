/*
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
 */

import java.math.MathContext;
import java.sql.Timestamp;


if (parameters.fromDateSellThrough) {
    Timestamp fromDate = null; 
    Timestamp thruDate = null; 
    try {
        fromDate = Timestamp.valueOf(parameters.fromDateSellThrough);
        thruDate = Timestamp.valueOf(parameters.thruDateSellThrough);
    } catch(Exception e) {}
    inventoryCountResult = runService('countProductInventoryOnHand', [productId : productId, facilityId : facilityId, inventoryCountDate : fromDate, userLogin : userLogin]);
    if (inventoryCountResult.quantityOnHandTotal && inventoryCountResult.quantityOnHandTotal > 0) {
        inventoryShippedForSalesResult = runService('countProductInventoryShippedForSales', [productId : productId, facilityId : facilityId, fromDate : fromDate, thruDate : thruDate, userLogin : userLogin]);
        context.sellThroughInitialInventory = inventoryCountResult.quantityOnHandTotal;
        context.sellThroughInventorySold = inventoryShippedForSalesResult.quantityOnHandTotal;
        context.sellThroughPercentage = new BigDecimal(inventoryShippedForSalesResult.quantityOnHandTotal / inventoryCountResult.quantityOnHandTotal * 100, new java.math.MathContext(2));
    }
}
