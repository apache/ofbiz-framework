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
