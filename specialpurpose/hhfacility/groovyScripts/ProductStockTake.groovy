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

import org.ofbiz.entity.*;
import org.ofbiz.entity.condition.*;

productId = request.getParameter("productId");
if (!productId) {
    productId = session.getAttribute("productId");
}

if (productId) {
    product = from("Product").where("productId", productId).cache(true).queryOne();
    context.product = product;

    facilityId = request.getParameter("facilityId");
    resultOutput = runService('getInventoryAvailableByFacility', [productId : productId, facilityId : facilityId]);
    quantitySummary = [:];
    quantitySummary.facilityId = facilityId;
    quantitySummary.atp_qoh = ((Double)resultOutput.availableToPromiseTotal).intValue() + " / " +
            ((Double)resultOutput.quantityOnHandTotal).intValue();
    context.quantitySummary = quantitySummary;

    // For now this just generates a visual list of locations set against the product for this facility.
    // todo: Will need to be able to edit and change these values at some point in the future.
    productFacilityLocList = from("ProductFacilityLocation").where("productId", productId, "facilityId", facilityId).queryList();
    facStr = null;
    productFacilityLocList.each { facilityLoc ->
        if (!facStr) {
            facStr = facilityLoc.locationSeqId;
        } else {
            facStr = facStr + ", " + facilityLoc.locationSeqId;
        }
    }
    context.productFacilityLocations = facStr;


    // Now we build a list of locations for inventory items against the facility.
    // todo: change this to a select from inv_items where productId and facilityId matches distinct (locationSeqId).
    invItemList = from("InventoryItem").where("productId", productId, "facilityId", facilityId).queryList();

    locations = [:];

    boolean negativeQOH = false;
    invItemList.each { invItem ->
        int qoh = ((Double)invItem.quantityOnHandTotal).intValue();
        if (qoh < 0) {
            negativeQOH = true;
        }
        locationFound = (String)invItem.locationSeqId;
        if (!locationFound) {
            locationFound = "nullField";
        }
        if (!locations.get(locationFound)) {
            locations.put(locationFound, locationFound);
        }
    }

    // Go through and build the list of atp/qoh against each location
    productFacilityLocations = new ArrayList();
    locationsIter = locations.keySet().iterator();
    while (locationsIter.hasNext()) {
        location = locationsIter.next();
        resultOutput = runService('getInventoryAvailableByLocation', [productId : productId, facilityId : facilityId, locationSeqId : location]);
        quantitySummary = [:];
        quantitySummary.productId = productId;
        quantitySummary.facilityId = facilityId;
        if ("nullField".equals( location ) == true) {
            quantitySummary.locationSeqId = "";
        } else {
            quantitySummary.locationSeqId = location;
        }
        quantitySummary.atp_qoh = ((Double)resultOutput.availableToPromiseTotal).intValue() + " / " +
                ((Double)resultOutput.quantityOnHandTotal).intValue();
        productFacilityLocations.add(quantitySummary);
    }

    context.productQtyByLocations = productFacilityLocations;
    if (negativeQOH) {
        context.negativeQOH = "true";
    }
}
