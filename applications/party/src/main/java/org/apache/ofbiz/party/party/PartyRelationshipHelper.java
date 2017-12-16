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

package org.apache.ofbiz.party.party;

import java.sql.Timestamp;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.util.EntityQuery;

/**
 * PartyRelationshipHelper
 */
public final class PartyRelationshipHelper {

    public static final String module = PartyRelationshipHelper.class.getName();
    private PartyRelationshipHelper() {}

    /** Return A List of the active Party Relationships (ie with valid from and thru dates)
     *@param delegator needed Delegator
     *@param partyRelationshipValues Map containing the input parameters (primaries keys + partyRelationshipTypeId)
     *@return List of the active Party Relationships
     */
    public static List<GenericValue> getActivePartyRelationships(Delegator delegator, Map<String, ?> partyRelationshipValues) {
        String partyIdFrom = (String) partyRelationshipValues.get("partyIdFrom") ;
        String partyIdTo = (String) partyRelationshipValues.get("partyIdTo") ;
        String roleTypeIdFrom = (String) partyRelationshipValues.get("roleTypeIdFrom") ;
        String roleTypeIdTo = (String) partyRelationshipValues.get("roleTypeIdTo") ;
        String partyRelationshipTypeId = (String) partyRelationshipValues.get("partyRelationshipTypeId") ;
        Timestamp fromDate = UtilDateTime.nowTimestamp();

        List<EntityCondition> condList = new LinkedList<>();
        condList.add(EntityCondition.makeCondition("partyIdFrom", partyIdFrom));
        condList.add(EntityCondition.makeCondition("partyIdTo", partyIdTo));
        condList.add(EntityCondition.makeCondition("roleTypeIdFrom", roleTypeIdFrom));
        condList.add(EntityCondition.makeCondition("roleTypeIdTo", roleTypeIdTo));
        condList.add(EntityCondition.makeCondition("partyRelationshipTypeId", partyRelationshipTypeId));
        condList.add(EntityCondition.makeCondition("fromDate", EntityOperator.LESS_THAN_EQUAL_TO, fromDate));
        EntityCondition thruCond = EntityCondition.makeCondition(UtilMisc.toList(
                EntityCondition.makeCondition("thruDate", null),
                EntityCondition.makeCondition("thruDate", EntityOperator.GREATER_THAN, fromDate)),
                EntityOperator.OR);
        condList.add(thruCond);
        EntityCondition condition = EntityCondition.makeCondition(condList);

        List<GenericValue> partyRelationships = null;
        try {
            partyRelationships = EntityQuery.use(delegator).from("PartyRelationship").where(condition).queryList();
        } catch (GenericEntityException e) {
            Debug.logError(e, "Problem finding PartyRelationships. ", module);
            return null;
        }
        if (UtilValidate.isNotEmpty(partyRelationships)) {
           return partyRelationships;
        }
        return null;
    }
}
