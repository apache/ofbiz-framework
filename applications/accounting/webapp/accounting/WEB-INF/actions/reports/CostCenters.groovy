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
import org.ofbiz.accounting.util.UtilAccounting;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.entity.condition.EntityCondition;

if (organizationPartyId) {

    customTimePeriods = delegator.findByAnd("CustomTimePeriod", [organizationPartyId : organizationPartyId, periodTypeId : "FISCAL_YEAR"]);
    if (UtilValidate.isNotEmpty(customTimePeriods)) {
        context.customTimePeriods = customTimePeriods;
    }
    onlyIncludePeriodTypeIdList = [];
    onlyIncludePeriodTypeIdList.add("FISCAL_YEAR");
    customTimePeriodResults = dispatcher.runSync("findCustomTimePeriods", [findDate : UtilDateTime.nowTimestamp(), organizationPartyId : organizationPartyId, onlyIncludePeriodTypeIdList : onlyIncludePeriodTypeIdList, userLogin : userLogin]);
    customTimePeriodList = customTimePeriodResults.customTimePeriodList;
    if (UtilValidate.isNotEmpty(customTimePeriodList)) {
        context.timePeriod = (EntityUtil.getFirst(customTimePeriodList)).customTimePeriodId;
    }
    resultFromPartyAcctgPref = dispatcher.runSync("getPartyAccountingPreferences", [organizationPartyId : organizationPartyId, userLogin : request.getAttribute("userLogin")]);
    partyAcctgPreference = resultFromPartyAcctgPref.partyAccountingPreference;
    context.currencyUomId = partyAcctgPreference.baseCurrencyUomId;
    context.glAccountCategories = delegator.findList("GlAccountCategory", EntityCondition.makeCondition([glAccountCategoryTypeId : "COST_CENTER"]), null, ["glAccountCategoryId"], null, false);
}
