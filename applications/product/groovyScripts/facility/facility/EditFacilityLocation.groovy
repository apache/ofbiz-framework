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

import org.apache.ofbiz.entity.condition.*

facilityId = request.getParameter("facilityId")
locationSeqId = request.getParameter("locationSeqId")
facility = null
facilityLocation = null

if (!facilityId && request.getAttribute("facilityId")) {
    facilityId = request.getAttribute("facilityId")
}

if (!locationSeqId && request.getAttribute("locationSeqId")) {
    locationSeqId = request.getAttribute("locationSeqId")
}

if (facilityId && locationSeqId) {
    facilityLocation = from("FacilityLocation").where("facilityId", facilityId, "locationSeqId", locationSeqId).queryOne()
}
if (facilityId) {
    facility = from("Facility").where("facilityId", facilityId).queryOne()
}

locationTypeEnums = from("Enumeration").where("enumTypeId", "FACLOC_TYPE").queryList()

// ProductFacilityLocation stuff
productFacilityLocations = null
if (facilityLocation) {
    productFacilityLocations = facilityLocation.getRelated("ProductFacilityLocation", null, ['productId'], false)
}

context.facilityId = facilityId
context.locationSeqId = locationSeqId
context.facility = facility
context.facilityLocation = facilityLocation
context.locationTypeEnums = locationTypeEnums
context.productFacilityLocations = productFacilityLocations
