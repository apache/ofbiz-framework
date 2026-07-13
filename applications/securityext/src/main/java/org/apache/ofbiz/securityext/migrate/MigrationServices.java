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
package org.apache.ofbiz.securityext.migrate;

import java.util.List;
import java.util.Map;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.transaction.TransactionUtil;
import org.apache.ofbiz.entity.util.EntityListIterator;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.party.party.PartyHelper;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.ServiceUtil;

public class MigrationServices {
    private static final String MODULE = MigrationServices.class.getName();

    public static Map<String, Object> migrateUserLoginUserFullName(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        List<EntityCondition> conditions = UtilMisc.toList(EntityCondition.makeCondition("userFullName", null));
        if (UtilValidate.isNotEmpty(context.get("userLoginId"))) {
            conditions.add(EntityCondition.makeCondition("userLoginId", context.get("userLoginId")));
        }
        if (UtilValidate.isNotEmpty(context.get("partyId"))) {
            conditions.add(EntityCondition.makeCondition("partyId", context.get("partyId")));
        } else {
            conditions.add(EntityCondition.makeCondition("partyId", EntityOperator.NOT_EQUAL, null));
        }
        EntityQuery eq = EntityQuery.use(delegator).from("UserLogin").where(conditions);

        try (EntityListIterator eli = eq.queryIterator()) {
            GenericValue userLogin;
            while ((userLogin = eli.next()) != null) {
                GenericValue userLoginToUpdate = userLogin;
                String userLoginId = userLoginToUpdate.getString("userLoginId");
                String errorMessage = "Error populating userFullName for userLoginId [" + userLoginId + "]";
                try {
                    TransactionUtil.doNewTransaction(() -> {
                        String userFullName = PartyHelper.getPartyName(delegator, userLoginToUpdate.getString("partyId"), false);
                        userLoginToUpdate.set("userFullName", userFullName);
                        userLoginToUpdate.store();
                        Debug.logInfo("Populated userFullName for userLoginId [" + userLoginId + "]", MODULE);
                        return null;
                    }, errorMessage, 0, false);
                } catch (Exception e) {
                    Debug.logError(e, errorMessage, MODULE);
                }
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }
        return ServiceUtil.returnSuccess();
    }
}
