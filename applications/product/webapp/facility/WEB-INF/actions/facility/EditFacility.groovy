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

import org.ofbiz.entity.condition.*;

facilityId = parameters.facilityId;
if (!facilityId && request.getAttribute("facilityId")) {
  facilityId = request.getAttribute("facilityId");
}
facility = from("Facility").where("facilityId", facilityId).queryOne();
if (!facility) {
  facility = delegator.makeValue("Facility");
  facilityType = delegator.makeValue("FacilityType");
} else {
  facilityType = facility.getRelatedOne("FacilityType", false);
}
context.facility = facility;
context.facilityType = facilityType;
context.facilityId = facilityId;

//Facility types
facilityTypes = from("FacilityType").queryList();
if (facilityTypes) {
  context.facilityTypes = facilityTypes;
}

// all possible inventory item types
context.inventoryItemTypes = from("InventoryItemType").orderBy("description").cache(true).queryList();

// weight unit of measures
context.weightUomList = from("Uom").where("uomTypeId", "WEIGHT_MEASURE").cache(true).queryList();

// area unit of measures
context.areaUomList = from("Uom").where("uomTypeId", "AREA_MEASURE").cache(true).queryList();
