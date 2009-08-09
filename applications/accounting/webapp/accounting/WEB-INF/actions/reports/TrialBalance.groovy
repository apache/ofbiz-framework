/*
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
 */

import java.math.BigDecimal;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.party.party.PartyHelper;

glAccountOrganizationCond = EntityCondition.makeCondition([EntityCondition.makeCondition("organizationPartyId", EntityOperator.IN, partyIds),
                                    EntityCondition.makeCondition("postedBalance", EntityOperator.NOT_EQUAL, null)], EntityOperator.AND);
trialBalances = [];
glAccountOrganizations = delegator.findList("GlAccountOrganization", glAccountOrganizationCond, null, null, null, false);
glAccountIds = EntityUtil.getFieldListFromEntityList(glAccountOrganizations, "glAccountId", true);
glAccountIds.each { glAccountId ->
    BigDecimal postedBalance = 0;
    glAccountOrganizations.each { glAccountOrganization ->
        if (glAccountOrganization.glAccountId.equals(glAccountId)) {
            postedBalance += glAccountOrganization.getBigDecimal("postedBalance");
        }
    }
    trialBalances.add([glAccountId : glAccountId , postedBalance : postedBalance]);
}
partyNameList = [];
parties.each { party ->
    partyName = PartyHelper.getPartyName(party);
    partyNameList.add(partyName);
}
context.trialBalances = trialBalances;
context.partyNameList = partyNameList;
