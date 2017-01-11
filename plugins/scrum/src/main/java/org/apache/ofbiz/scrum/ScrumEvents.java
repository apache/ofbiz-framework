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
package org.apache.ofbiz.scrum;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilHttp;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityConditionList;
import org.apache.ofbiz.entity.condition.EntityExpr;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.util.EntityQuery;

/**
 * ScrumEvents - Check The Warning Message. 
 */
public class ScrumEvents {

    public static final String module = ScrumEvents.class.getName();

    public static String timeSheetChecker(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession();
        Delegator delegator = (Delegator) session.getAttribute("delegator");
        GenericValue userLogin = (GenericValue) session.getAttribute("userLogin");
        List<Map<String, Object>> noTimeEntryList = new LinkedList<Map<String,Object>>();
        String partyId = userLogin.getString("partyId");
        Timestamp now = UtilDateTime.nowTimestamp();
        Timestamp weekStart = UtilDateTime.getWeekStart(now);

        if (UtilValidate.isEmpty(delegator)) {
            delegator = (Delegator) request.getAttribute("delegator");
        }

        try {
            // should be scrum team or scrum master.
            EntityConditionList<EntityExpr> exprOrs = EntityCondition.makeCondition(UtilMisc.toList(EntityCondition.makeCondition("roleTypeId", EntityOperator.EQUALS, "SCRUM_TEAM"), EntityCondition.makeCondition("roleTypeId", EntityOperator.EQUALS, "SCRUM_MASTER")), EntityOperator.OR);
            EntityConditionList<EntityCondition> exprAnds = EntityCondition.makeCondition(UtilMisc.toList(exprOrs, EntityCondition.makeCondition("partyId", EntityOperator.EQUALS, partyId)), EntityOperator.AND);
            List<GenericValue> partyRoleList = EntityQuery.use(delegator).from("PartyRole").where(exprAnds).queryList();
            if (UtilValidate.isNotEmpty(partyRoleList)) {
                List<GenericValue> timesheetList = EntityQuery.use(delegator).from("Timesheet").where("partyId", partyId, "statusId", "TIMESHEET_IN_PROCESS").cache(true).queryList();
                if (UtilValidate.isNotEmpty(timesheetList)) {
                    for (GenericValue timesheetMap : timesheetList) {
                        String timesheetId = timesheetMap.getString("timesheetId");
                        Timestamp timesheetDate = timesheetMap.getTimestamp("fromDate");
                        //check monday - friday
                        for (int i = 0; i < 5; i++) {
                            Timestamp realTimeDate = UtilDateTime.addDaysToTimestamp(timesheetDate, i);
                            Timestamp nowStartDate = UtilDateTime.getDayStart(now);
                            //compare week and compare date
                            if ((timesheetDate.compareTo(weekStart) <= 0) && (realTimeDate.compareTo(nowStartDate) < 0)) {
                                //check time entry
                                List<GenericValue> timeEntryList = timesheetMap.getRelated("TimeEntry", UtilMisc.toMap("partyId", partyId, "timesheetId",timesheetId, "fromDate",realTimeDate), null, false);
                                //check EmplLeave
                                List<GenericValue> emplLeaveList = EntityQuery.use(delegator).from("EmplLeave").where("partyId", partyId, "fromDate", realTimeDate).cache(true).queryList();
                                if (UtilValidate.isEmpty(timeEntryList) && UtilValidate.isEmpty(emplLeaveList)) {
                                    Map<String, Object> noEntryMap = new HashMap<String, Object>();
                                    noEntryMap.put("timesheetId", timesheetId);
                                    noTimeEntryList.add(noEntryMap);
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        } catch (GenericEntityException EntEx) {
            EntEx.printStackTrace();
            Debug.logError(EntEx.getMessage(), module);
        }
        if (UtilValidate.isNotEmpty(noTimeEntryList)) {
            StringBuilder warningDataBuffer = new StringBuilder();
            int size = noTimeEntryList.size();
            for (Map<String, Object> dataMap : noTimeEntryList) {
                if (--size == 0) {
                    warningDataBuffer.append(dataMap.get("timesheetId"));
                } else {
                    warningDataBuffer.append(dataMap.get("timesheetId")).append(", ");
                }
                warningDataBuffer.append(dataMap.get("timesheetId"));
            }
            String warningData = warningDataBuffer.toString();
            Debug.logInfo("The following time sheet no time entry: [" + warningData + "]", module);
            request.setAttribute("_ERROR_MESSAGE_", UtilProperties.getMessage("scrumUiLabels", "ScrumTimesheetWarningMessage", UtilMisc.toMap("warningMessage", warningData), UtilHttp.getLocale(request)));
        }
        return "success";
    }
}
