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

import java.util.*;
import java.net.*;
import org.ofbiz.security.*;
import org.ofbiz.entity.*;
import org.ofbiz.base.util.*;

findOrganizationPartyId = parameters.findOrganizationPartyId;
if (findOrganizationPartyId) {
    context.findOrganizationPartyId = findOrganizationPartyId;
}

currentCustomTimePeriodId = parameters.currentCustomTimePeriodId;
if (currentCustomTimePeriodId) {
    context.currentCustomTimePeriodId = currentCustomTimePeriodId;
}

currentCustomTimePeriod = currentCustomTimePeriodId ? delegator.findOne("CustomTimePeriod", [customTimePeriodId : currentCustomTimePeriodId], false) : null;
if (currentCustomTimePeriod) {
    context.currentCustomTimePeriod = currentCustomTimePeriod;
}

currentPeriodType = currentCustomTimePeriod ? currentCustomTimePeriod.getRelatedOne("PeriodType", true) : null;
if (currentPeriodType) {
    context.currentPeriodType = currentPeriodType;
}

findMap = [ : ];
if (findOrganizationPartyId) findMap.organizationPartyId = findOrganizationPartyId;
if (currentCustomTimePeriodId) findMap.parentPeriodId = currentCustomTimePeriodId;

customTimePeriods = delegator.findByAnd("CustomTimePeriod", findMap, ["periodTypeId", "periodNum", "fromDate"], false);
context.customTimePeriods = customTimePeriods;

allCustomTimePeriods = delegator.findList("CustomTimePeriod", null, null, ["organizationPartyId", "parentPeriodId", "periodTypeId", "periodNum", "fromDate"], null, false);
context.allCustomTimePeriods = allCustomTimePeriods;

periodTypes = delegator.findList("PeriodType", null, null, ["description"], null, true);
context.periodTypes = periodTypes;

newPeriodTypeId = "FISCAL_YEAR";
if ("FISCAL_YEAR".equals(currentCustomTimePeriod?.periodTypeId)) {
    newPeriodTypeId = "FISCAL_QUARTER";
}
if ("FISCAL_QUARTER".equals(currentCustomTimePeriod?.periodTypeId)) {
    newPeriodTypeId = "FISCAL_MONTH";
}
if ("FISCAL_MONTH".equals(currentCustomTimePeriod?.periodTypeId)) {
    newPeriodTypeId = "FISCAL_WEEK";
}
if ("FISCAL_BIWEEK".equals(currentCustomTimePeriod?.periodTypeId)) {
    newPeriodTypeId = "FISCAL_WEEK";
}
if ("FISCAL_WEEK".equals(currentCustomTimePeriod?.periodTypeId)) {
    newPeriodTypeId = "";
}

context.newPeriodTypeId = newPeriodTypeId;
