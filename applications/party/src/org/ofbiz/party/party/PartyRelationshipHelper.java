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

package org.ofbiz.party.party;

import java.util.List;
import java.util.Map;

import org.ofbiz.base.util.Debug;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.util.EntityUtil;

/**
 * PartyRelationshipHelper
 */
public class PartyRelationshipHelper {

    public static final String module = PartyRelationshipHelper.class.getName();

    /**
     * Return A List of the active Party Relationships
     *
     * @param delegator
     * @param partyRelationshipValues Map containing the input parameters
     * @return List of the active Party Relationships
     */
    public static List<GenericValue> getPartyRelationships(Delegator delegator, Map<String, ?> partyRelationshipValues) {
        return getPartyRelationships(delegator, partyRelationshipValues, true);
    }

    /**
     * Return A List of the Party Relationships
     *
     * @param delegator
     * @param partyRelationshipValues Map containing the input parameters
     * @param activeOnly
     * @return List of the active Party Relationships
     */
    public static List<GenericValue> getPartyRelationships(Delegator delegator, Map<String, ?> partyRelationshipValues, boolean activeOnly) {
        List<GenericValue> partyRelationships = null;
        try {
            partyRelationships = delegator.findByAndCache("PartyRelationship", partyRelationshipValues);
            if (activeOnly){
                partyRelationships = EntityUtil.filterByDate(partyRelationships);
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, "Problem finding PartyRelationships. ", module);
        }

        return partyRelationships;
    }

    @Deprecated
    /**
     * Return A List of the active Party Relationships (ie with valid from and thru dates)
     *
     * @param delegator needed Delegator
     * @param partyRelationshipValues Map containing the input parameters (primaries keys + partyRelationshipTypeId)
     * @return List of the active Party Relationships
     */
    public static List<GenericValue> getActivePartyRelationships(Delegator delegator, Map<String, ?> partyRelationshipValues) {
        return getPartyRelationships(delegator, partyRelationshipValues, true);
    }
}
