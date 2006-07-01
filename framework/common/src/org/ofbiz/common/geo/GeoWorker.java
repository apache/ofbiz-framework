/*
 * $Id: GeoWorker.java 6190 2005-11-24 10:16:14Z jonesde $
 *
 *  Copyright (c) 2003 The Open For Business Project - www.ofbiz.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a
 *  copy of this software and associated documentation files (the "Software"),
 *  to deal in the Software without restriction, including without limitation
 *  the rights to use, copy, modify, merge, publish, distribute, sublicense,
 *  and/or sell copies of the Software, and to permit persons to whom the
 *  Software is furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included
 *  in all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 *  OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 *  IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 *  CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT
 *  OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 *  THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
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
 *
 * @author     <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a>
 * @version    $Rev$
 * @since      3.0
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
