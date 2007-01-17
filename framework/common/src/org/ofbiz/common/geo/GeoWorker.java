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

import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.Debug;

import java.util.List;
import java.util.LinkedList;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Set;

import javolution.util.FastSet;

/**
 * Worker methods for Geos
 */
public class GeoWorker {

    public static final String module = GeoWorker.class.getName();

    public static List expandGeoGroup(String geoId, GenericDelegator delegator) {
        GenericValue geo = null;
        try {
            geo = delegator.findByPrimaryKeyCache("Geo", UtilMisc.toMap("geoId", geoId));
        } catch (GenericEntityException e) {
            Debug.logError(e, "Unable to look up Geo from geoId : " + geoId, module);
        }
        return expandGeoGroup(geo);
    }

    public static List expandGeoGroup(GenericValue geo) {
        if (geo == null) {
            return new ArrayList();
        }
        if (!"GROUP".equals(geo.getString("geoTypeId"))) {
            return UtilMisc.toList(geo);
        }

        //Debug.log("Expanding geo : " + geo, module);

        List geoList = new LinkedList();
        List thisGeoAssoc = null;
        try {
            thisGeoAssoc = geo.getRelated("AssocGeoAssoc", UtilMisc.toMap("geoAssocTypeId", "GROUP_MEMBER"), null);
        } catch (GenericEntityException e) {
            Debug.logError(e, "Unable to get associated Geo GROUP_MEMBER relationship(s)", module);
        }
        if (thisGeoAssoc != null && thisGeoAssoc.size() > 0) {
            Iterator gi = thisGeoAssoc.iterator();
            while (gi.hasNext()) {
                GenericValue nextGeoAssoc = (GenericValue) gi.next();
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
    
    public static Set expandGeoRegionDeep(Set geoIdSet, GenericDelegator delegator) throws GenericEntityException {
        if (geoIdSet == null || geoIdSet.size() == 0) {
            return geoIdSet;
        }
        Set geoIdSetTemp = FastSet.newInstance();
        Iterator geoIdIter = geoIdSet.iterator();
        while (geoIdIter.hasNext()) {
            String curGeoId = (String) geoIdIter.next();
            List geoAssocList = delegator.findByAndCache("GeoAssoc", UtilMisc.toMap("geoIdTo", curGeoId, "geoAssocTypeId", "REGIONS"));
            Iterator geoAssocIter = geoAssocList.iterator();
            while (geoAssocIter.hasNext()) {
                GenericValue geoAssoc = (GenericValue) geoAssocIter.next();
                geoIdSetTemp.add(geoAssoc.get("geoId"));
            }
        }
        geoIdSetTemp = expandGeoRegionDeep(geoIdSetTemp, delegator);
        Set geoIdSetNew = FastSet.newInstance();
        geoIdSetNew.addAll(geoIdSet);
        geoIdSetNew.addAll(geoIdSetTemp);
        return geoIdSetNew;
    }

    public static boolean containsGeo(List geoList, String geoId, GenericDelegator delegator) {
        GenericValue geo = null;
        try {
            geo = delegator.findByPrimaryKeyCache("Geo", UtilMisc.toMap("geoId", geoId));
        } catch (GenericEntityException e) {
            Debug.logError(e, "Unable to look up Geo from geoId : " + geoId, module);
        }
        return containsGeo(geoList, geo);
    }

    public static boolean containsGeo(List geoList, GenericValue geo) {
        if (geoList == null || geo == null) {
            return false;
        }
        //Debug.log("Contains Geo : " + geoList.contains(geo));
        return geoList.contains(geo);
    }
}
