/*******************************************************************************
 * Licensed partyIdTo the Apache Software Foundation (ASF) under one
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

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.service.ServiceUtil;


/**
 * Services for Party Relationship maintenance
 */
public class PartyRelationshipServices {

    private static final String MODULE = PartyRelationshipServices.class.getName();
    private static final String RESOURCE = "PartyUiLabels";
    private static final String RES_ERROR = "PartyErrorUiLabels";

    /** Creates and updates a PartyRelationship creating related PartyRoles if needed.
     *  A side of the relationship is checked to maintain history
     *@param ctx The DispatchContext that this service is operating in
     *@param context Map containing the input parameters
     *@return Map with the result of the service, the output parameters
     */
    public static Map<String, Object> createUpdatePartyRelationshipAndRoles(DispatchContext ctx, Map<String, ? extends Object> context) {
        Map<String, Object> result = new HashMap<>();
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        Locale locale = (Locale) context.get("locale");

        try {
            List<GenericValue> partyRelationShipList = PartyRelationshipHelper.getActivePartyRelationships(delegator, context);
            if (UtilValidate.isEmpty(partyRelationShipList)) { // If already exists and active nothing to do: keep the current one
                String partyId = (String) context.get("partyId");
                String partyIdFrom = (String) context.get("partyIdFrom");
                String partyIdTo = (String) context.get("partyIdTo");
                String roleTypeIdFrom = (String) context.get("roleTypeIdFrom");
                String roleTypeIdTo = (String) context.get("roleTypeIdTo");
                String partyRelationshipTypeId = (String) context.get("partyRelationshipTypeId");

                // Before creating the partyRelationShip, create the partyRoles if they don't exist
                GenericValue partyToRole = null;
                partyToRole = EntityQuery.use(delegator).from("PartyRole").where("partyId", partyIdTo, "roleTypeId", roleTypeIdTo).queryOne();
                if (partyToRole == null) {
                    partyToRole = delegator.makeValue("PartyRole", UtilMisc.toMap("partyId", partyIdTo, "roleTypeId", roleTypeIdTo));
                    partyToRole.create();
                }

                GenericValue partyFromRole = null;
                partyFromRole = EntityQuery.use(delegator).from("PartyRole").where("partyId", partyIdFrom, "roleTypeId", roleTypeIdFrom).queryOne();
                if (partyFromRole == null) {
                    partyFromRole = delegator.makeValue("PartyRole", UtilMisc.toMap("partyId", partyIdFrom, "roleTypeId", roleTypeIdFrom));
                    partyFromRole.create();
                }

                // Check if there is already a partyRelationship of that type with another party from the side indicated
                String sideChecked = partyIdFrom.equals(partyId) ? "partyIdFrom" : "partyIdTo";
                // We consider the last one (in time) as sole active (we try to maintain a unique relationship and keep changes history)
                GenericValue oldPartyRelationShip = EntityQuery.use(delegator).from("PartyRelationship")
                        .where(sideChecked, partyId, "roleTypeIdFrom", roleTypeIdFrom, "roleTypeIdTo", roleTypeIdTo, "partyRelationshipTypeId",
                                partyRelationshipTypeId).filterByDate().queryFirst();
                if (oldPartyRelationShip != null) {
                    oldPartyRelationShip.setFields(UtilMisc.toMap("thruDate", UtilDateTime.nowTimestamp())); // Current becomes inactive
                    oldPartyRelationShip.store();
                }
                try {
                    Map<String, Object> resultMap = dispatcher.runSync("createPartyRelationship", context); // Create new one
                    if (ServiceUtil.isError(resultMap)) {
                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(resultMap));
                    }
                } catch (GenericServiceException e) {
                    Debug.logWarning(e.getMessage(), MODULE);
                    return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                            "partyrelationshipservices.could_not_create_party_role_write",
                            UtilMisc.toMap("errorString", e.getMessage()), locale));
                }
            }
        } catch (GenericEntityException e) {
            Debug.logWarning(e.getMessage(), MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    "partyrelationshipservices.could_not_create_party_role_write",
                    UtilMisc.toMap("errorString", e.getMessage()), locale));
        }
        result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
        return result;
    }
}
