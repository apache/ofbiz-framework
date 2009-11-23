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
package org.ofbiz.common.geo;

import java.util.List;
import java.util.Set;

import javolution.util.FastList;
import javolution.util.FastSet;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.util.EntityUtil;

/**
 * Worker methods for Geos
 */
public class GeoWorker {

    public static final String module = GeoWorker.class.getName();

    public static List<GenericValue> expandGeoGroup(String geoId, Delegator delegator) {
        GenericValue geo = null;
        try {
            geo = delegator.findByPrimaryKeyCache("Geo", UtilMisc.toMap("geoId", geoId));
        } catch (GenericEntityException e) {
            Debug.logError(e, "Unable to look up Geo from geoId : " + geoId, module);
        }
        return expandGeoGroup(geo);
    }

    public static List<GenericValue> expandGeoGroup(GenericValue geo) {
        if (geo == null) {
            return FastList.newInstance();
        }
        if (!"GROUP".equals(geo.getString("geoTypeId"))) {
            return UtilMisc.toList(geo);
        }

        //Debug.log("Expanding geo : " + geo, module);

        List<GenericValue> geoList = FastList.newInstance();
        List<GenericValue> thisGeoAssoc = null;
        try {
            thisGeoAssoc = geo.getRelated("AssocGeoAssoc", UtilMisc.toMap("geoAssocTypeId", "GROUP_MEMBER"), null);
        } catch (GenericEntityException e) {
            Debug.logError(e, "Unable to get associated Geo GROUP_MEMBER relationship(s)", module);
        }
        if (UtilValidate.isNotEmpty(thisGeoAssoc)) {
            for (GenericValue nextGeoAssoc: thisGeoAssoc) {
                GenericValue nextGeo = null;
                try {
                    nextGeo = nextGeoAssoc.getRelatedOne("MainGeo");
                } catch (GenericEntityException e) {
                    Debug.logError(e, "Unable to get related Geo", module);
                }
                geoList.addAll(expandGeoGroup(nextGeo));
            }
        } else {
            //Debug.log("No associated geos with this group", module);
        }

        //Debug.log("Expanded to : " + geoList, module);

        return geoList;
    }

    public static Set<String> expandGeoRegionDeep(Set<String> geoIdSet, Delegator delegator) throws GenericEntityException {
        if (UtilValidate.isEmpty(geoIdSet)) {
            return geoIdSet;
        }
        Set<String> geoIdSetTemp = FastSet.newInstance();
        for (String curGeoId: geoIdSet) {
            List<GenericValue> geoAssocList = delegator.findByAndCache("GeoAssoc", UtilMisc.toMap("geoIdTo", curGeoId, "geoAssocTypeId", "REGIONS"));
            for (GenericValue geoAssoc: geoAssocList) {
                geoIdSetTemp.add(geoAssoc.getString("geoId"));
            }
        }
        geoIdSetTemp = expandGeoRegionDeep(geoIdSetTemp, delegator);
        Set<String> geoIdSetNew = FastSet.newInstance();
        geoIdSetNew.addAll(geoIdSet);
        geoIdSetNew.addAll(geoIdSetTemp);
        return geoIdSetNew;
    }

    public static boolean containsGeo(List<GenericValue> geoList, String geoId, Delegator delegator) {
        GenericValue geo = null;
        try {
            geo = delegator.findByPrimaryKeyCache("Geo", UtilMisc.toMap("geoId", geoId));
        } catch (GenericEntityException e) {
            Debug.logError(e, "Unable to look up Geo from geoId : " + geoId, module);
        }
        return containsGeo(geoList, geo);
    }

    public static boolean containsGeo(List<GenericValue> geoList, GenericValue geo) {
        if (geoList == null || geo == null) {
            return false;
        }
        //Debug.log("Contains Geo : " + geoList.contains(geo));
        return geoList.contains(geo);
    }

    public static GenericValue findLatestGeoPoint(Delegator delegator, String Entity, String mainId, String mainValueId, String secondId, String secondValueId) {
        List<GenericValue> gptList = null;
        if (UtilValidate.isNotEmpty(secondId) && UtilValidate.isNotEmpty(secondValueId)) {
            try {
                gptList = delegator.findByAnd(Entity, UtilMisc.toMap(mainId, mainValueId, secondId, secondValueId), UtilMisc.toList("-fromDate"));
            } catch (GenericEntityException e) {
                Debug.logError(e, "Error while finding latest GeoPoint for " + mainId + " with Id [" + mainValueId + "] and " + secondId + " Id [" + secondValueId + "] " + e.toString(), module);
            }
        } else {
            try {
                gptList = delegator.findByAnd(Entity, UtilMisc.toMap(mainId, mainValueId), UtilMisc.toList("-fromDate"));
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
