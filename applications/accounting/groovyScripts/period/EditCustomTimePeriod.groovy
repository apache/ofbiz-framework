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

currentCustomTimePeriod = currentCustomTimePeriodId ? from("CustomTimePeriod").where("customTimePeriodId", currentCustomTimePeriodId).queryOne() : null;
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

customTimePeriods = from("CustomTimePeriod").where(findMap).orderBy(["periodTypeId", "periodNum", "fromDate"]).queryList();
context.customTimePeriods = customTimePeriods;

allCustomTimePeriods = from("CustomTimePeriod").orderBy(["organizationPartyId", "parentPeriodId", "periodTypeId", "periodNum", "fromDate"]).queryList();
context.allCustomTimePeriods = allCustomTimePeriods;

periodTypes = from("PeriodType").orderBy("description").cache(true).queryList();
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
