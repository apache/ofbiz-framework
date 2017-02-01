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

 import org.apache.ofbiz.base.util.*
 import org.apache.ofbiz.common.geo.*
 import org.apache.ofbiz.entity.*

facilityId = parameters.facilityId
locationSeqId = parameters.locationSeqId

uiLabelMap = UtilProperties.getResourceBundleMap("ProductUiLabels", locale)
uiLabelMap.addBottomResourceBundle("CommonUiLabels")

if (facilityId && locationSeqId) {
    latestGeoPoint = GeoWorker.findLatestGeoPoint(delegator, "FacilityLocationAndGeoPoint", "facilityId", facilityId, "locationSeqId", locationSeqId)
    context.latestGeoPoint = latestGeoPoint

    List geoCenter = UtilMisc.toList(UtilMisc.toMap("lat", latestGeoPoint.latitude, "lon", latestGeoPoint.longitude, "zoom", "13"))

    if (UtilValidate.isNotEmpty(latestGeoPoint) && latestGeoPoint.containsKey("latitude") && latestGeoPoint.containsKey("longitude")) {
        List geoPoints = UtilMisc.toList(UtilMisc.toMap("lat", latestGeoPoint.latitude, "lon", latestGeoPoint.longitude, "facilityId", facilityId,
                "link", UtilMisc.toMap("url", "EditFacilityLocation?facilityId="+ facilityId + "&locationSeqId=" + locationSeqId,
                "label", uiLabelMap.ProductFacilityLocation  + " " + uiLabelMap.CommonOf + " " + facilityId + "/" + locationSeqId)))

        Map geoChart = UtilMisc.toMap("width", "500px", "height", "450px", "controlUI" , "small", "dataSourceId", latestGeoPoint.dataSourceId, "points", geoPoints)
        context.geoChart = geoChart
    }
    if (latestGeoPoint && latestGeoPoint.elevationUomId) {
        elevationUom = from("Uom").where("uomId", latestGeoPoint.elevationUomId).queryOne()
        context.elevationUomAbbr = elevationUom.abbreviation
    }
}
