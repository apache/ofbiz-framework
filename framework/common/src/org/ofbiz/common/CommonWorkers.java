/*
 * $Id: CommonWorkers.java 6538 2006-01-23 01:47:50Z jonesde $
 *
 * Copyright (c) 2001-2005 The Open For Business Project - www.ofbiz.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 * CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT
 * OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 * THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */
package org.ofbiz.common;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javolution.util.FastList;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityConditionList;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityOperator;

/**
 * Common Workers
 *
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @author     <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a>
 * @version    $Rev$
 * @since      2.0
 */
public class CommonWorkers {
    
    public final static String module = CommonWorkers.class.getName();

    public static List getCountryList(GenericDelegator delegator) {
        List geoList = FastList.newInstance();
        String defaultCountry = UtilProperties.getPropertyValue("general.properties", "country.geo.id.default");
        GenericValue defaultGeo = null;
        if (defaultCountry != null && defaultCountry.length() > 0) { 
            try {
                defaultGeo = delegator.findByPrimaryKeyCache("Geo", UtilMisc.toMap("geoId", defaultCountry));
            } catch (GenericEntityException e) {
                Debug.logError(e, "Cannot lookup Geo", module);
            }       
        }        
        try {
        	List countryGeoList = delegator.findByAndCache("Geo", UtilMisc.toMap("geoTypeId", "COUNTRY"), UtilMisc.toList("geoName"));
            if (defaultGeo != null) {
                geoList.add(defaultGeo);
            	geoList.addAll(countryGeoList);
            } else {
            	geoList = countryGeoList;
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, "Cannot lookup Geo", module);
        }        
        return geoList;            
    }
    
    public static List getStateList(GenericDelegator delegator) {
        List geoList = new ArrayList();       
        EntityCondition condition = new EntityConditionList(UtilMisc.toList(
                new EntityExpr("geoTypeId", EntityOperator.EQUALS, "STATE"), new EntityExpr("geoTypeId", EntityOperator.EQUALS, "PROVINCE")), EntityOperator.OR);
        List sortList = UtilMisc.toList("geoName");
        try {
            geoList = delegator.findByConditionCache("Geo", condition, null, sortList);
        } catch (GenericEntityException e) {
            Debug.logError(e, "Cannot lookup State Geos: " + e.toString(), module);
        }                        
        return geoList;            
    }    

    /**
     * Returns a list of regional geo associations.
     */
    public static List getAssociatedStateList(GenericDelegator delegator, String country) {
      if ( country == null || country.length() == 0 ) {
        // Load the system default country
        country = UtilProperties.getPropertyValue("general.properties", "country.geo.id.default");
      }
      List geoList = new ArrayList();
      Map geoAssocFindMap = UtilMisc.toMap("geoId", country, "geoAssocTypeId", "REGIONS");
      List sortList = UtilMisc.toList("geoIdTo");
      try {
          geoList = delegator.findByAndCache("GeoAssoc", geoAssocFindMap, sortList);
      } catch (GenericEntityException e) {
          Debug.logError(e, "Cannot lookup Geo", module);
      }                        
      return geoList;            

    }    
}
