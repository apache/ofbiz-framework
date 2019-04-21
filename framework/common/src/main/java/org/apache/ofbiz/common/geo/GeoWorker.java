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
package org.apache.ofbiz.common.geo;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtil;

/**
 * Worker methods for Geos
 */
public final class GeoWorker {

    public static final String module = GeoWorker.class.getName();

    private GeoWorker() {}

    public static List<GenericValue> expandGeoGroup(String geoId, Delegator delegator) {
        GenericValue geo = null;
        try {
            geo = EntityQuery.use(delegator).from("Geo").where("geoId", geoId).cache().queryOne();
        } catch (GenericEntityException e) {
            Debug.logError(e, "Unable to look up Geo from geoId : " + geoId, module);
        }
        return expandGeoGroup(geo);
    }

    public static List<GenericValue> expandGeoGroup(GenericValue geo) {
        if (geo == null) {
            return new LinkedList<>();
        }
        if (!"GROUP".equals(geo.getString("geoTypeId"))) {
            return UtilMisc.toList(geo);
        }

        List<GenericValue> geoList = new LinkedList<>();
        List<GenericValue> thisGeoAssoc = null;
        try {
            thisGeoAssoc = geo.getRelated("AssocGeoAssoc", UtilMisc.toMap("geoAssocTypeId", "GROUP_MEMBER"), null, false);
        } catch (GenericEntityException e) {
            Debug.logError(e, "Unable to get associated Geo GROUP_MEMBER relationship(s)", module);
        }
        if (UtilValidate.isNotEmpty(thisGeoAssoc)) {
            for (GenericValue nextGeoAssoc: thisGeoAssoc) {
                GenericValue nextGeo = null;
                try {
                    nextGeo = nextGeoAssoc.getRelatedOne("MainGeo", false);
                } catch (GenericEntityException e) {
                    Debug.logError(e, "Unable to get related Geo", module);
                }
                geoList.addAll(expandGeoGroup(nextGeo));
            }
        } 
        return geoList;
    }

    public static Map<String, String> expandGeoRegionDeep(Map<String, String> geoIdByTypeMapOrig, Delegator delegator) throws GenericEntityException {
        if (UtilValidate.isEmpty(geoIdByTypeMapOrig)) {
            return geoIdByTypeMapOrig;
        }
        Map<String, String> geoIdByTypeMapTemp =  new LinkedHashMap<>();
        for (Map.Entry<String, String> geoIdByTypeEntry: geoIdByTypeMapOrig.entrySet()) {
            List<GenericValue> geoAssocList = EntityQuery.use(delegator)
                                                         .from("GeoAssoc")
                                                         .where("geoIdTo", geoIdByTypeEntry.getValue(), "geoAssocTypeId", "REGIONS")
                                                         .cache(true)
                                                         .queryList();
            for (GenericValue geoAssoc: geoAssocList) {
                GenericValue newGeo = EntityQuery.use(delegator).from("Geo").where("geoId", geoAssoc.get("geoId")).cache().queryOne();
                geoIdByTypeMapTemp.put(newGeo.getString("geoTypeId"), newGeo.getString("geoId"));
            }
        }
        geoIdByTypeMapTemp = expandGeoRegionDeep(geoIdByTypeMapTemp, delegator);
        Map<String, String> geoIdByTypeMapNew =  new LinkedHashMap<>();
        // add the temp Map first, then the original over top of it, ie give the original priority over the sub/expanded values
        geoIdByTypeMapNew.putAll(geoIdByTypeMapTemp);
        geoIdByTypeMapNew.putAll(geoIdByTypeMapOrig);
        return geoIdByTypeMapNew;
    }

    public static boolean containsGeo(List<GenericValue> geoList, String geoId, Delegator delegator) {
        GenericValue geo = null;
        try {
            geo = EntityQuery.use(delegator).from("Geo").where("geoId", geoId).cache().queryOne();
        } catch (GenericEntityException e) {
            Debug.logError(e, "Unable to look up Geo from geoId : " + geoId, module);
        }
        return containsGeo(geoList, geo);
    }

    public static boolean containsGeo(List<GenericValue> geoList, GenericValue geo) {
        if (geoList == null || geo == null) {
            return false;
        }
        return geoList.contains(geo);
    }

    public static GenericValue findLatestGeoPoint(Delegator delegator, String entityName, String mainId, String mainValueId, String secondId, String secondValueId) {
        List<GenericValue> gptList = null;
        if (UtilValidate.isNotEmpty(secondId) && UtilValidate.isNotEmpty(secondValueId)) {
            try {
                gptList = EntityQuery.use(delegator)
                                     .from(entityName)
                                     .where(mainId, mainValueId, secondId, secondValueId)
                                     .orderBy("-fromDate")
                                     .queryList();
            } catch (GenericEntityException e) {
                Debug.logError(e, "Error while finding latest GeoPoint for " + mainId + " with Id [" + mainValueId + "] and " + secondId + " Id [" + secondValueId + "] " + e.toString(), module);
            }
        } else {
            try {
                gptList = EntityQuery.use(delegator).from(entityName).where(mainId, mainValueId).orderBy("-fromDate").queryList();
            } catch (GenericEntityException e) {
                Debug.logError(e, "Error while finding latest GeoPoint for " + mainId + " with Id [" + mainValueId + "] " + e.toString(), module);
            }
        }
        if (UtilValidate.isNotEmpty(gptList)) {
            gptList = EntityUtil.filterByDate(gptList);
            return EntityUtil.getFirst(gptList);
        }
        return null;
    }
}
