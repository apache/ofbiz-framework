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
package org.apache.ofbiz.common;

import java.util.LinkedList;
import java.util.List;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.StringUtil;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityExpr;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityTypeUtil;
import org.apache.ofbiz.entity.util.EntityUtilProperties;

/**
 * Common Workers
 */
public final class CommonWorkers {

    public final static String module = CommonWorkers.class.getName();

    private CommonWorkers() {}

    public static List<GenericValue> getCountryList(Delegator delegator) {
        List<GenericValue> geoList = new LinkedList<>();
        String defaultCountry = EntityUtilProperties.getPropertyValue("general", "country.geo.id.default", delegator);
        GenericValue defaultGeo = null;
        if (UtilValidate.isNotEmpty(defaultCountry)) {
            try {
                defaultGeo = EntityQuery.use(delegator).from("Geo").where("geoId", defaultCountry).cache().queryOne();
            } catch (GenericEntityException e) {
                Debug.logError(e, "Cannot lookup Geo", module);
            }
        }

        List<EntityExpr> exprs = UtilMisc.toList(EntityCondition.makeCondition("geoTypeId", EntityOperator.EQUALS, "COUNTRY"));
        List<String> countriesAvailable = StringUtil.split(EntityUtilProperties.getPropertyValue("general", "countries.geo.id.available", delegator), ",");
        if (UtilValidate.isNotEmpty(countriesAvailable)) {
            // only available countries (we don't verify the list of geoId in countries.geo.id.available)
            exprs.add(EntityCondition.makeCondition("geoId", EntityOperator.IN, countriesAvailable));
        }

        List<GenericValue> countriesList = new LinkedList<>();
        try {
            countriesList = EntityQuery.use(delegator).from("Geo").where(exprs).orderBy("geoName").cache(true).queryList();
        } catch (GenericEntityException e) {
            Debug.logError(e, "Cannot lookup Geo", module);
        }
        if (defaultGeo != null) {
            geoList.add(defaultGeo);
            boolean removeDefaultGeo = UtilValidate.isEmpty(countriesList);
            if (!removeDefaultGeo) {
                for (GenericValue country  : countriesList) {
                    if (country.get("geoId").equals(defaultGeo.get("geoId"))) {
                        removeDefaultGeo = true;
                    }
                }
            }
            if (removeDefaultGeo) {
                geoList.remove(0); // Remove default country to avoid double rows in drop-down, from 1st place to keep alphabetical order
            }
            geoList.addAll(countriesList);
        } else {
            geoList = countriesList;
        }
        return geoList;
    }

    public static List<GenericValue> getStateList(Delegator delegator) {
        List<GenericValue> geoList = new LinkedList<>();
        EntityCondition condition = EntityCondition.makeCondition(EntityOperator.OR, EntityCondition.makeCondition("geoTypeId", "STATE"), EntityCondition.makeCondition("geoTypeId", "PROVINCE"), EntityCondition.makeCondition("geoTypeId", "TERRITORY"),
                EntityCondition.makeCondition("geoTypeId", "MUNICIPALITY"));
        try {
            geoList = EntityQuery.use(delegator).from("Geo").where(condition).orderBy("geoName").cache(true).queryList();
        } catch (GenericEntityException e) {
            Debug.logError(e, "Cannot lookup State Geos: " + e.toString(), module);
        }
        return geoList;
    }

    public static List<GenericValue> getAssociatedStateList(Delegator delegator, String country) {
        return getAssociatedStateList(delegator, country, null);
    }

    /**
     * Returns a list of regional geo associations.
     */
    public static List<GenericValue> getAssociatedStateList(Delegator delegator, String country, String listOrderBy) {
        if (UtilValidate.isEmpty(country)) {
            // Load the system default country
            country = EntityUtilProperties.getPropertyValue("general", "country.geo.id.default", delegator);
        }

        if (UtilValidate.isEmpty(listOrderBy)) {
            listOrderBy = "geoId";
        }
        List<String> sortList = UtilMisc.toList(listOrderBy);

        List<GenericValue> geoList = new LinkedList<>();
        try {
            // Check if the country is a country group and get recursively the
            // states
            List<GenericValue> regionList = EntityQuery.use(delegator)
                                                       .from("GeoAssocAndGeoToWithState")
                                                       .where("geoIdFrom", country, "geoAssocTypeId", "GROUP_MEMBER", "geoTypeId", "GROUP")
                                                       .orderBy(sortList)
                                                       .cache(true)
                                                       .queryList();
            if (regionList.size() == 1) {
                for (GenericValue region : regionList) {
                    List<GenericValue> tmpState = EntityQuery.use(delegator)
                                                             .from("GeoAssocAndGeoTo")
                                                             .where("geoId", region.getString("geoIdFrom"))
                                                             .orderBy(sortList)
                                                             .cache(true)
                                                             .queryList();
                    for (GenericValue state : tmpState) {
                        geoList.addAll(getAssociatedStateList(delegator, state.getString("geoIdFrom"), listOrderBy));
                    }
                }
            }

            // get all related states
            EntityCondition stateProvinceFindCond = EntityCondition.makeCondition(
                    EntityCondition.makeCondition("geoIdFrom", country),
                    EntityCondition.makeCondition("geoAssocTypeId", "REGIONS"),
                    EntityCondition.makeCondition(EntityOperator.OR, EntityCondition.makeCondition("geoTypeId", "STATE"), EntityCondition.makeCondition("geoTypeId", "PROVINCE"), EntityCondition.makeCondition("geoTypeId", "MUNICIPALITY"),
                    		EntityCondition.makeCondition("geoTypeId", "TERRITORY"), EntityCondition.makeCondition("geoTypeId", "COUNTY")));
            geoList.addAll(EntityQuery.use(delegator)
                                      .from("GeoAssocAndGeoToWithState")
                                      .where(stateProvinceFindCond)
                                      .orderBy(sortList)
                                      .cache(true)
                                      .queryList()
                          );
        } catch (GenericEntityException e){
            Debug.logError(e, "Cannot lookup Geo", module);
        }

        return geoList;
    }

    /**
     * A generic method to be used on Type enities, e.g. ProductType.  Recurse to the root level in the type hierarchy
     * and checks if the specified type childType has parentType as its parent somewhere in the hierarchy.
     *
     * @param delegator       The Delegator object.
     * @param entityName      Name of the Type entity on which check is performed.
     * @param primaryKey      Primary Key field of the Type entity.
     * @param childType       Type value for which the check is performed.
     * @param parentTypeField Field in Type entity which stores the parent type.
     * @param parentType      Value of the parent type against which check is performed.
     * @return boolean value based on the check results.
     *
     * @deprecated Moved to {@link org.apache.ofbiz.entity.util.EntityTypeUtil#hasParentType(Delegator, String, String, String, String, String) EntityTypeUtil}
     */
    @Deprecated
    public static boolean hasParentType(Delegator delegator, String entityName, String primaryKey, String childType, String parentTypeField, String parentType) {
        return EntityTypeUtil.hasParentType(delegator, entityName, primaryKey, childType, parentTypeField, parentType);
    }
}
