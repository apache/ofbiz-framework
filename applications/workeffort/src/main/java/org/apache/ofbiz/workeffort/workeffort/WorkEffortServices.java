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

package org.apache.ofbiz.workeffort.workeffort;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.ofbiz.base.util.DateRange;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.TimeDuration;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityConditionList;
import org.apache.ofbiz.entity.condition.EntityExpr;
import org.apache.ofbiz.entity.condition.EntityJoinOperator;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.util.EntityListIterator;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.security.Security;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;
import org.apache.ofbiz.service.calendar.TemporalExpression;
import org.apache.ofbiz.service.calendar.TemporalExpressionWorker;

import com.ibm.icu.util.Calendar;

/**
 * WorkEffortServices - WorkEffort related Services
 */
public class WorkEffortServices {

    public static final String module = WorkEffortServices.class.getName();
    public static final String resourceError = "WorkEffortUiLabels";

    public static Map<String, Object> getWorkEffortAssignedEventsForRole(DispatchContext ctx, Map<String, ? extends Object> context) {
        Delegator delegator = ctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String roleTypeId = (String) context.get("roleTypeId");
        Locale locale = (Locale) context.get("locale");

        List<GenericValue> validWorkEfforts = null;

        if (userLogin != null && userLogin.get("partyId") != null) {
            try {
                EntityConditionList<EntityExpr> ecl = EntityCondition.makeCondition(
                        EntityOperator.AND,
                        EntityCondition.makeCondition("partyId", EntityOperator.EQUALS, userLogin.get("partyId")),
                        EntityCondition.makeCondition("roleTypeId", EntityOperator.EQUALS, roleTypeId),
                        EntityCondition.makeCondition("workEffortTypeId", EntityOperator.EQUALS, "EVENT"),
                        EntityCondition.makeCondition("currentStatusId", EntityOperator.NOT_EQUAL, "CAL_DECLINED"),
                        EntityCondition.makeCondition("currentStatusId", EntityOperator.NOT_EQUAL, "CAL_DELEGATED"),
                        EntityCondition.makeCondition("currentStatusId", EntityOperator.NOT_EQUAL, "CAL_COMPLETED"),
                        EntityCondition.makeCondition("currentStatusId", EntityOperator.NOT_EQUAL, "CAL_CANCELLED")
               );
                validWorkEfforts = EntityQuery.use(delegator).from("WorkEffortAndPartyAssign").where(ecl).orderBy("estimatedStartDate", "priority").filterByDate().queryList();
            } catch (GenericEntityException e) {
                Debug.logWarning(e, module);
                return ServiceUtil.returnError(UtilProperties.getMessage(resourceError,
                        "WorkEffortNotFound", UtilMisc.toMap("errorString", e.toString()), locale));
            }
        }

        Map<String, Object> result = new HashMap<>();
        if (validWorkEfforts == null) {
            validWorkEfforts = new LinkedList<>();
        }
        result.put("events", validWorkEfforts);
        return result;
    }

    public static Map<String, Object> getWorkEffortAssignedEventsForRoleOfAllParties(DispatchContext ctx, Map<String, ? extends Object> context) {
        Delegator delegator = ctx.getDelegator();
        String roleTypeId = (String) context.get("roleTypeId");
        Locale locale = (Locale) context.get("locale");

        List<GenericValue> validWorkEfforts = null;

        try {
            List<EntityExpr> conditionList = new LinkedList<>();
            conditionList.add(EntityCondition.makeCondition("roleTypeId", EntityOperator.EQUALS, roleTypeId));
            conditionList.add(EntityCondition.makeCondition("workEffortTypeId", EntityOperator.EQUALS, "EVENT"));
            conditionList.add(EntityCondition.makeCondition("currentStatusId", EntityOperator.NOT_EQUAL, "CAL_DECLINED"));
            conditionList.add(EntityCondition.makeCondition("currentStatusId", EntityOperator.NOT_EQUAL, "CAL_DELEGATED"));
            conditionList.add(EntityCondition.makeCondition("currentStatusId", EntityOperator.NOT_EQUAL, "CAL_COMPLETED"));
            conditionList.add(EntityCondition.makeCondition("currentStatusId", EntityOperator.NOT_EQUAL, "CAL_CANCELLED"));

            EntityConditionList<EntityExpr> ecl = EntityCondition.makeCondition(conditionList, EntityOperator.AND);
            validWorkEfforts = EntityQuery.use(delegator).from("WorkEffortAndPartyAssign").where(ecl).orderBy("estimatedStartDate", "priority").filterByDate().queryList();
        } catch (GenericEntityException e) {
            Debug.logWarning(e, module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError,
                    "WorkEffortNotFound", UtilMisc.toMap("errorString", e.toString()), locale));
        }

        Map<String, Object> result = new HashMap<>();
        if (validWorkEfforts == null) {
            validWorkEfforts = new LinkedList<>();
        }
        result.put("events", validWorkEfforts);
        return result;
    }

    public static Map<String, Object> getWorkEffortAssignedTasks(DispatchContext ctx, Map<String, ? extends Object> context) {
        Delegator delegator = ctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");

        List<GenericValue> validWorkEfforts = null;

        if (userLogin != null && userLogin.get("partyId") != null) {
            try {
                EntityConditionList<EntityExpr> ecl = EntityCondition.makeCondition(
                        EntityOperator.AND,
                        EntityCondition.makeCondition("partyId", EntityOperator.EQUALS, userLogin.get("partyId")),
                        EntityCondition.makeCondition("workEffortTypeId", EntityOperator.EQUALS, "TASK"),
                        EntityCondition.makeCondition("currentStatusId", EntityOperator.NOT_EQUAL, "CAL_DECLINED"),
                        EntityCondition.makeCondition("currentStatusId", EntityOperator.NOT_EQUAL, "CAL_DELEGATED"),
                        EntityCondition.makeCondition("currentStatusId", EntityOperator.NOT_EQUAL, "CAL_COMPLETED"),
                        EntityCondition.makeCondition("currentStatusId", EntityOperator.NOT_EQUAL, "CAL_CANCELLED"),
                        EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "PRTYASGN_UNASSIGNED"));
                validWorkEfforts = EntityQuery.use(delegator).from("WorkEffortAndPartyAssign").where(ecl).orderBy("priority").filterByDate().queryList();
                ecl = EntityCondition.makeCondition(
                        EntityOperator.AND,
                        EntityCondition.makeCondition("partyId", EntityOperator.EQUALS, userLogin.get("partyId")),
                        EntityCondition.makeCondition("workEffortTypeId", EntityOperator.EQUALS, "PROD_ORDER_TASK"),
                        EntityCondition.makeCondition("currentStatusId", EntityOperator.NOT_EQUAL, "PRUN_CANCELLED "),
                        EntityCondition.makeCondition("currentStatusId", EntityOperator.NOT_EQUAL, "PRUN_COMPLETED"),
                        EntityCondition.makeCondition("currentStatusId", EntityOperator.NOT_EQUAL, "PRUN_CLOSED"));
                validWorkEfforts.addAll(EntityQuery.use(delegator).from("WorkEffortAndPartyAssign").where(ecl).orderBy("createdDate DESC").filterByDate().queryList());
            } catch (GenericEntityException e) {
                Debug.logWarning(e, module);
                return ServiceUtil.returnError(UtilProperties.getMessage(resourceError,
                        "WorkEffortNotFound", UtilMisc.toMap("errorString", e.toString()), locale));
            }
        }

        Map<String, Object> result = new HashMap<>();
        if (validWorkEfforts == null) {
            validWorkEfforts = new LinkedList<>();
        }
        validWorkEfforts = WorkEffortWorker.removeDuplicateWorkEfforts(validWorkEfforts);
        result.put("tasks", validWorkEfforts);
        return result;
    }

    public static Map<String, Object> getWorkEffortAssignedActivities(DispatchContext ctx, Map<String, ? extends Object> context) {
        Delegator delegator = ctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");

        List<GenericValue> validWorkEfforts = null;

        if (userLogin != null && userLogin.get("partyId") != null) {
            try {
                List<EntityExpr> constraints = new LinkedList<>();

                constraints.add(EntityCondition.makeCondition("partyId", EntityOperator.EQUALS, userLogin.get("partyId")));
                constraints.add(EntityCondition.makeCondition("workEffortTypeId", EntityOperator.EQUALS, "ACTIVITY"));
                constraints.add(EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "CAL_DECLINED"));
                constraints.add(EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "CAL_DELEGATED"));
                constraints.add(EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "CAL_COMPLETED"));
                constraints.add(EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "CAL_CANCELLED"));
                constraints.add(EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "PRTYASGN_UNASSIGNED"));
                constraints.add(EntityCondition.makeCondition("currentStatusId", EntityOperator.NOT_EQUAL, "WF_COMPLETED"));
                constraints.add(EntityCondition.makeCondition("currentStatusId", EntityOperator.NOT_EQUAL, "WF_TERMINATED"));
                constraints.add(EntityCondition.makeCondition("currentStatusId", EntityOperator.NOT_EQUAL, "WF_ABORTED"));

                validWorkEfforts = EntityQuery.use(delegator).from("WorkEffortAndPartyAssign").where(constraints).orderBy("priority").filterByDate().queryList();
            } catch (GenericEntityException e) {
                Debug.logWarning(e, module);
                return ServiceUtil.returnError(UtilProperties.getMessage(resourceError,
                        "WorkEffortNotFound", UtilMisc.toMap("errorString", e.toString()), locale));
            }
        }

        Map<String, Object> result = new HashMap<>();
        if (validWorkEfforts == null) {
            validWorkEfforts = new LinkedList<>();
        }
        result.put("activities", validWorkEfforts);
        return result;
    }

    public static Map<String, Object> getWorkEffortAssignedActivitiesByRole(DispatchContext ctx, Map<String, ? extends Object> context) {
        Delegator delegator = ctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");

        List<GenericValue> roleWorkEfforts = null;

        if (userLogin != null && userLogin.get("partyId") != null) {
            try {
                List<EntityExpr> constraints = new LinkedList<>();

                constraints.add(EntityCondition.makeCondition("partyId", EntityOperator.EQUALS, userLogin.get("partyId")));
                constraints.add(EntityCondition.makeCondition("workEffortTypeId", EntityOperator.EQUALS, "ACTIVITY"));
                constraints.add(EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "CAL_DECLINED"));
                constraints.add(EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "CAL_DELEGATED"));
                constraints.add(EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "CAL_COMPLETED"));
                constraints.add(EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "CAL_CANCELLED"));
                constraints.add(EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "PRTYASGN_UNASSIGNED"));
                constraints.add(EntityCondition.makeCondition("currentStatusId", EntityOperator.NOT_EQUAL, "WF_COMPLETED"));
                constraints.add(EntityCondition.makeCondition("currentStatusId", EntityOperator.NOT_EQUAL, "WF_TERMINATED"));
                constraints.add(EntityCondition.makeCondition("currentStatusId", EntityOperator.NOT_EQUAL, "WF_ABORTED"));

                roleWorkEfforts = EntityQuery.use(delegator).from("WorkEffortPartyAssignByRole").where(constraints).orderBy("priority").filterByDate().queryList();
            } catch (GenericEntityException e) {
                Debug.logWarning(e, module);
                return ServiceUtil.returnError(UtilProperties.getMessage(resourceError,
                        "WorkEffortNotFound", UtilMisc.toMap("errorString", e.toString()), locale));
            }
        }

        Map<String, Object> result = new HashMap<>();
        if (roleWorkEfforts == null) {
            roleWorkEfforts = new LinkedList<>();
        }
        result.put("roleActivities", roleWorkEfforts);
        return result;
    }

    public static Map<String, Object> getWorkEffortAssignedActivitiesByGroup(DispatchContext ctx, Map<String, ? extends Object> context) {
        Delegator delegator = ctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");

        List<GenericValue> groupWorkEfforts = null;

        if (userLogin != null && userLogin.get("partyId") != null) {
            try {
                List<EntityExpr> constraints = new LinkedList<>();

                constraints.add(EntityCondition.makeCondition("partyId", EntityOperator.EQUALS, userLogin.get("partyId")));
                constraints.add(EntityCondition.makeCondition("workEffortTypeId", EntityOperator.EQUALS, "ACTIVITY"));
                constraints.add(EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "CAL_DECLINED"));
                constraints.add(EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "CAL_DELEGATED"));
                constraints.add(EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "CAL_COMPLETED"));
                constraints.add(EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "CAL_CANCELLED"));
                constraints.add(EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "PRTYASGN_UNASSIGNED"));
                constraints.add(EntityCondition.makeCondition("currentStatusId", EntityOperator.NOT_EQUAL, "WF_COMPLETED"));
                constraints.add(EntityCondition.makeCondition("currentStatusId", EntityOperator.NOT_EQUAL, "WF_TERMINATED"));
                constraints.add(EntityCondition.makeCondition("currentStatusId", EntityOperator.NOT_EQUAL, "WF_ABORTED"));

                groupWorkEfforts = EntityQuery.use(delegator).from("WorkEffortPartyAssignByGroup").where(constraints).orderBy("priority").filterByDate().queryList();
            } catch (GenericEntityException e) {
                Debug.logWarning(e, module);
                return ServiceUtil.returnError(UtilProperties.getMessage(resourceError,
                        "WorkEffortNotFound", UtilMisc.toMap("errorString", e.toString()), locale));
            }
        }

        Map<String, Object> result = new HashMap<>();
        if (groupWorkEfforts == null) {
            groupWorkEfforts = new LinkedList<>();
        }
        result.put("groupActivities", groupWorkEfforts);
        return result;
    }

    public static Map<String, Object> getWorkEffort(DispatchContext ctx, Map<String, ? extends Object> context) {
        Delegator delegator = ctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Security security = ctx.getSecurity();
        Map<String, Object> resultMap = new HashMap<>();

        String workEffortId = (String) context.get("workEffortId");
        GenericValue workEffort = null;

        try {
            workEffort = EntityQuery.use(delegator).from("WorkEffort").where("workEffortId", workEffortId).queryOne();
        } catch (GenericEntityException e) {
            Debug.logWarning(e, module);
        }

        Boolean canView = null;
        List<GenericValue> workEffortPartyAssignments = null;
        Boolean tryEntity = null;
        GenericValue currentStatus = null;

        if (workEffort == null) {
            tryEntity = Boolean.FALSE;
            canView = Boolean.TRUE;

            String statusId = (String) context.get("currentStatusId");

            if (UtilValidate.isNotEmpty(statusId)) {
                try {
                    currentStatus = EntityQuery.use(delegator).from("StatusItem").where("statusId", statusId).cache().queryOne();
                } catch (GenericEntityException e) {
                    Debug.logWarning(e, module);
                }
            }
        } else {
            // get a list of workEffortPartyAssignments, if empty then this user CANNOT view the event, unless they have permission to view all
            if (userLogin != null && userLogin.get("partyId") != null && workEffortId != null) {
                try {
                    workEffortPartyAssignments = EntityQuery.use(delegator).from("WorkEffortPartyAssignment").where("workEffortId", workEffortId, "partyId", userLogin.get("partyId")).queryList();
                } catch (GenericEntityException e) {
                    Debug.logWarning(e, module);
                }
            }
            canView = (UtilValidate.isNotEmpty(workEffortPartyAssignments)) ? Boolean.TRUE : Boolean.FALSE;
            if (!canView && security.hasEntityPermission("WORKEFFORTMGR", "_VIEW", userLogin)) {
                canView = Boolean.TRUE;
            }

            tryEntity = Boolean.TRUE;

            if (workEffort.get("currentStatusId") != null) {
                try {
                    currentStatus = EntityQuery.use(delegator).from("StatusItem").where("statusId", workEffort.get("currentStatusId")).cache().queryOne();
                } catch (GenericEntityException e) {
                    Debug.logWarning(e, module);
                }
            }
        }

        if (workEffortId != null) {
            resultMap.put("workEffortId", workEffortId);
        }
        if (workEffort != null) {
            resultMap.put("workEffort", workEffort);
        }
        if (canView != null) {
            resultMap.put("canView", canView);
        }
        if (workEffortPartyAssignments != null) {
            resultMap.put("partyAssigns", workEffortPartyAssignments);
        }
        if (tryEntity != null) {
            resultMap.put("tryEntity", tryEntity);
        }
        if (currentStatus != null) {
            resultMap.put("currentStatusItem", currentStatus);
        }
        return resultMap;
    }

    private static TreeMap<DateRange, List<Map<String, Object>>> groupCalendarEntriesByDateRange(DateRange inDateRange, List<Map<String, Object>> calendarEntries) {
        TreeMap<DateRange, List<Map<String, Object>>> calendarEntriesByDateRange = new TreeMap<>();
        Set<Date> dateBoundaries = new TreeSet<>();
        if (inDateRange != null) {
            dateBoundaries.add(inDateRange.start());
            dateBoundaries.add(inDateRange.end());
        }
        for (Map<String, Object>calendarEntry: calendarEntries) {
            DateRange calEntryRange = (DateRange)calendarEntry.get("calEntryRange");
            dateBoundaries.add(calEntryRange.start());
            dateBoundaries.add(calEntryRange.end());
        }
        Date prevDateBoundary = null;
        for (Date dateBoundary: dateBoundaries) {
            if (prevDateBoundary != null) {
                DateRange dateRange = new DateRange(prevDateBoundary, dateBoundary);
                for (Map<String, Object>calendarEntry: calendarEntries) {
                    DateRange calEntryRange = (DateRange)calendarEntry.get("calEntryRange");
                    if (calEntryRange.intersectsRange(dateRange) && !(calEntryRange.end().equals(dateRange.start()) || calEntryRange.start().equals(dateRange.end()))) {
                        List<Map<String, Object>> calendarEntryByDateRangeList = calendarEntriesByDateRange.get(dateRange);
                        if (calendarEntryByDateRangeList == null) {
                            calendarEntryByDateRangeList = new LinkedList<>();
                        }
                        calendarEntryByDateRangeList.add(calendarEntry);
                        calendarEntriesByDateRange.put(dateRange, calendarEntryByDateRangeList);
                    }
                }
            }
            prevDateBoundary = dateBoundary;
        }
        return calendarEntriesByDateRange;
    }

    private static List<EntityCondition> getDefaultWorkEffortExprList(String calendarType, Collection<String> partyIds, String workEffortTypeId, List<EntityCondition> cancelledCheckAndList) {
        List<EntityCondition> entityExprList = new LinkedList<>();
        if (cancelledCheckAndList != null) {
            entityExprList.addAll(cancelledCheckAndList);
        }
        List<EntityExpr> typesList = new LinkedList<>();
        if (UtilValidate.isNotEmpty(workEffortTypeId)) {
            typesList.add(EntityCondition.makeCondition("workEffortTypeId", EntityOperator.EQUALS, workEffortTypeId));
        }
        if ("CAL_PERSONAL".equals(calendarType)) {
            // public events are always included to the "personal calendar"
            List<EntityCondition> publicEvents = UtilMisc.<EntityCondition>toList(
                    EntityCondition.makeCondition("scopeEnumId", EntityOperator.EQUALS, "WES_PUBLIC"),
                    EntityCondition.makeCondition("parentTypeId", EntityOperator.EQUALS, "EVENT")
                    );
            if (UtilValidate.isNotEmpty(partyIds)) {
                entityExprList.add(
                        EntityCondition.makeCondition(UtilMisc.toList(
                                EntityCondition.makeCondition("partyId", EntityOperator.IN, partyIds),
                                EntityCondition.makeCondition(publicEvents, EntityJoinOperator.AND)
                        ), EntityJoinOperator.OR));
            }
        }
        if ("CAL_MANUFACTURING".equals(calendarType)) {
            entityExprList.add(
                    EntityCondition.makeCondition(UtilMisc.toList(
                            EntityCondition.makeCondition("workEffortTypeId", EntityOperator.EQUALS, "PROD_ORDER_HEADER"),
                            EntityCondition.makeCondition("workEffortTypeId", EntityOperator.EQUALS, "PROD_ORDER_TASK")
                    ), EntityJoinOperator.OR));
        }
        EntityCondition typesCondition = null;
        if (typesList.size() == 0) {
            return entityExprList;
        } else if (typesList.size() == 1) {
            typesCondition = typesList.get(0);
        } else {
            typesCondition = EntityCondition.makeCondition(typesList, EntityJoinOperator.OR);
        }
        entityExprList.add(typesCondition);
        return entityExprList;
    }

    /**
     * Get Work Efforts by period.
     * <p>
     * This method takes the following parameters:
     * </p>
     * <ul>
     *   <li>start - TimeStamp (Period start date/time)</li>
     *   <li>numPeriods - Integer</li>
     *   <li>periodType - Integer (see java.util.Calendar)</li>
     *   <li>eventStatus - String</li>
     *   <li>partyId - String</li>
     *   <li>partyIds - List</li>
     *   <li>facilityId - String</li>
     *   <li>fixedAssetId - String</li>
     *   <li>filterOutCanceledEvents - Boolean</li>
     *   <li>entityExprList - List</li>
     * </ul>
     * <p>
     * The method will find all matching Work Effort events and return them as a List called
     * <b>periods</b> - one List element per period. It also returns a
     * <b>maxConcurrentEntries</b> Integer - which indicates the maximum number of
     * Work Efforts found in one period.
     * </p>
     * <p>
     * Each <b>periods</b> list element is a Map containing the following
     * key/value pairs:
     * </p>
     * <ul>
     *   <li>start - TimeStamp (Period start date/time)</li>
     *   <li>end - TimeStamp (Period end date/time)</li>
     *   <li>calendarEntries - List of Maps. Each Map contains the following key/value pairs</li>
     *   <li><ul>
     *       <li>workEffort - GenericValue</li>
     *       <li>periodSpan - Integer (Number of periods this Work Effort spans)</li>
     *       <li>startOfPeriod - Boolean (true if this is the first occurrence in the period range)</li>
     *   </ul></li>
     * </ul>
     */

    public static Map<String, Object> getWorkEffortEventsByPeriod(DispatchContext ctx, Map<String, ? extends Object> context) {

        /*
         To create testdata for  this function for  fixedasset/facility

        1) go to Manufacturing -> JobShop, then click on "create new Production run":
                https://localhost:8443/manufacturing/control/CreateProductionRun
        2) enter as productId "PROD_MANUF", quantity 1, start date tomorrow and press the submit button
    `    3) in the next screen, click on the "Confirm" link (top part of the sccreen)

        Now you have a confirmed production run (starting tomorrow) happening in facility "WebStoreWarehouse",
        with a task happening in fixed asset "WORKCENTER_COST"

        In the calendars screen, selecting the proper facility you should see the work effort associated to the production run;
        if you select the proper fixed asset you should see the task.

         */
        Delegator delegator = ctx.getDelegator();
        Security security = ctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");
        TimeZone timeZone = (TimeZone) context.get("timeZone");

        Timestamp startDay = (Timestamp) context.get("start");
        Integer numPeriodsInteger = (Integer) context.get("numPeriods");

        String calendarType = (String) context.get("calendarType");
        if (UtilValidate.isEmpty(calendarType)) {
            // This is a bad idea. This causes the service to return only those work efforts that are assigned
            // to the current user even when the service parameters have nothing to do with the current user.
            calendarType = "CAL_PERSONAL";
        }
        String partyId = (String) context.get("partyId");
        Collection<String> partyIds = UtilGenerics.checkCollection(context.get("partyIds"));
        String facilityId = (String) context.get("facilityId");
        String fixedAssetId = (String) context.get("fixedAssetId");
        String workEffortTypeId = (String) context.get("workEffortTypeId");
        Boolean filterOutCanceledEvents = (Boolean) context.get("filterOutCanceledEvents");
        if (filterOutCanceledEvents == null) {
            filterOutCanceledEvents = Boolean.FALSE;
        }

        // To be returned, the max concurrent entries for a single period
        int maxConcurrentEntries = 0;

        Integer periodTypeObject = (Integer) context.get("periodType");
        int periodType = 0;
        if (periodTypeObject != null) {
            periodType = periodTypeObject;
        }

        int numPeriods = 0;
        if (numPeriodsInteger != null) {
            numPeriods = numPeriodsInteger;
        }

        // get a timestamp (date) for the beginning of today and for beginning of numDays+1 days from now
        // Commenting this out because it interferes with periods that do not start at the beginning of the day
        Timestamp startStamp = startDay;
        Timestamp endStamp = UtilDateTime.adjustTimestamp(startStamp, periodType, 1, timeZone, locale);
        long periodLen = endStamp.getTime() - startStamp.getTime();
        endStamp = UtilDateTime.adjustTimestamp(startStamp, periodType, numPeriods, timeZone, locale);

        // Get the WorkEfforts
        List<GenericValue> validWorkEfforts = null;
        Collection<String> partyIdsToUse = partyIds;
        if (partyIdsToUse == null) {
            partyIdsToUse = new HashSet<>();
        }
        if (UtilValidate.isNotEmpty(partyId)) {
            if (partyId.equals(userLogin.getString("partyId")) || security.hasEntityPermission("WORKEFFORTMGR", "_VIEW", userLogin)) {
                partyIdsToUse.add(partyId);
            } else {
                return ServiceUtil.returnError(UtilProperties.getMessage(resourceError,
                        "WorkEffortPartyPermissionError", UtilMisc.toMap("partyId", partyId), locale));
            }
        } else {
            if ("CAL_PERSONAL".equals(calendarType) && UtilValidate.isNotEmpty(userLogin.getString("partyId"))) {
                partyIdsToUse.add(userLogin.getString("partyId"));
            }
        }

        // cancelled status id's
        List<EntityCondition> cancelledCheckAndList = UtilMisc.<EntityCondition>toList(
                EntityCondition.makeCondition("currentStatusId", EntityOperator.NOT_EQUAL, "EVENT_CANCELLED"),
                EntityCondition.makeCondition("currentStatusId", EntityOperator.NOT_EQUAL, "CAL_CANCELLED"),
                EntityCondition.makeCondition("currentStatusId", EntityOperator.NOT_EQUAL, "PRUN_CANCELLED"));


        List<EntityCondition> entityExprList = UtilGenerics.cast(context.get("entityExprList"));
        if (entityExprList == null) {
            entityExprList = getDefaultWorkEffortExprList(calendarType, partyIdsToUse, workEffortTypeId, cancelledCheckAndList);
        }

        if (UtilValidate.isNotEmpty(facilityId)) {
            entityExprList.add(EntityCondition.makeCondition("facilityId", EntityOperator.EQUALS, facilityId));
        }
        if (UtilValidate.isNotEmpty(fixedAssetId)) {
            entityExprList.add(EntityCondition.makeCondition("fixedAssetId", EntityOperator.EQUALS, fixedAssetId));
        }

        // should have at least a start date
        EntityCondition startDateRequired = EntityCondition.makeCondition(UtilMisc.<EntityCondition>toList(
                EntityCondition.makeCondition("estimatedStartDate", EntityOperator.NOT_EQUAL, null),
                EntityCondition.makeCondition("actualStartDate", EntityOperator.NOT_EQUAL, null)
        ), EntityJoinOperator.OR);

        List<EntityCondition> periodCheckAndlList = UtilMisc.<EntityCondition>toList(
                startDateRequired,
                // the startdate should be less than the period end
                EntityCondition.makeCondition(UtilMisc.<EntityCondition>toList(
                        EntityCondition.makeCondition(UtilMisc.<EntityCondition>toList(
                                EntityCondition.makeCondition("actualStartDate", EntityOperator.EQUALS, null),
                                EntityCondition.makeCondition("estimatedStartDate", EntityOperator.NOT_EQUAL, null),
                                EntityCondition.makeCondition("estimatedStartDate", EntityOperator.LESS_THAN_EQUAL_TO, endStamp)
                        ), EntityJoinOperator.AND),
                        EntityCondition.makeCondition(UtilMisc.<EntityCondition>toList(
                                EntityCondition.makeCondition("actualStartDate", EntityOperator.NOT_EQUAL, null),
                                EntityCondition.makeCondition("actualStartDate", EntityOperator.LESS_THAN_EQUAL_TO, endStamp)
                        ), EntityJoinOperator.AND)
                ), EntityJoinOperator.OR),
                // if the completion date is not null then it should be larger than the period start
                EntityCondition.makeCondition(UtilMisc.<EntityCondition>toList(
                        // can also be empty
                        EntityCondition.makeCondition(UtilMisc.<EntityCondition>toList(
                                EntityCondition.makeCondition("estimatedCompletionDate", EntityOperator.EQUALS, null),
                                EntityCondition.makeCondition("actualCompletionDate", EntityOperator.EQUALS, null)
                        ), EntityJoinOperator.AND),
                        // check estimated value if the actual is not provided
                        EntityCondition.makeCondition(UtilMisc.<EntityCondition>toList(
                                EntityCondition.makeCondition("actualCompletionDate", EntityOperator.EQUALS, null),
                                EntityCondition.makeCondition("estimatedCompletionDate", EntityOperator.NOT_EQUAL, null),
                                EntityCondition.makeCondition("estimatedCompletionDate", EntityOperator.GREATER_THAN_EQUAL_TO, startStamp)
                        ), EntityJoinOperator.AND),
                        // at last check the actual value
                        EntityCondition.makeCondition(UtilMisc.<EntityCondition>toList(
                                EntityCondition.makeCondition("actualCompletionDate", EntityOperator.NOT_EQUAL, null),
                                EntityCondition.makeCondition("actualCompletionDate", EntityOperator.GREATER_THAN_EQUAL_TO, startStamp)
                        ), EntityJoinOperator.AND)
                ), EntityJoinOperator.OR));

        entityExprList.addAll(periodCheckAndlList);

        try {
            List<GenericValue> tempWorkEfforts = null;
            if (UtilValidate.isNotEmpty(partyIdsToUse)) {
                tempWorkEfforts = EntityQuery.use(delegator).from("WorkEffortAndPartyAssignAndType").where(entityExprList).orderBy("estimatedStartDate").filterByDate().queryList();
            } else {
                tempWorkEfforts = EntityQuery.use(delegator).from("WorkEffort").where(entityExprList).orderBy("estimatedStartDate").queryList();
            }
            if (!"CAL_PERSONAL".equals(calendarType) && UtilValidate.isNotEmpty(fixedAssetId)) {
                // Get "new style" work efforts
                tempWorkEfforts.addAll(EntityQuery.use(delegator).from("WorkEffortAndFixedAssetAssign").where(entityExprList).orderBy("estimatedStartDate").filterByDate().queryList());
            }
            validWorkEfforts = WorkEffortWorker.removeDuplicateWorkEfforts(tempWorkEfforts);
        } catch (GenericEntityException e) {
            Debug.logWarning(e, module);
        }

        // Split the WorkEffort list into a map with entries for each period, period start is the key
        List<Map<String, Object>> periods = new LinkedList<>();
        if (validWorkEfforts != null) {
            List<DateRange> periodRanges = new LinkedList<>();
            for (int i = 0; i < numPeriods; i++) {
                Timestamp curPeriodStart = UtilDateTime.adjustTimestamp(startStamp, periodType, i, timeZone, locale);
                Timestamp curPeriodEnd = UtilDateTime.adjustTimestamp(curPeriodStart, periodType, 1, timeZone, locale);
                curPeriodEnd = new Timestamp(curPeriodEnd.getTime() - 1);
                periodRanges.add(new DateRange(curPeriodStart, curPeriodEnd));
            }
            try {
                // Process recurring work efforts
                Set<GenericValue> exclusions = new HashSet<>();
                Set<GenericValue> inclusions = new HashSet<>();
                DateRange range = new DateRange(startStamp, endStamp);
                Calendar cal = UtilDateTime.toCalendar(startStamp, timeZone, locale);
                for (GenericValue workEffort : validWorkEfforts) {
                    if (UtilValidate.isNotEmpty(workEffort.getString("tempExprId"))) {
                        // check if either the workeffort is public or the requested party is a member
                        if (UtilValidate.isNotEmpty(partyIdsToUse) && !"WES_PUBLIC".equals(workEffort.getString("scopeEnumId")) && !partyIdsToUse.contains(workEffort.getString("partyId"))) {
                            continue;
                        }
                        // if the workeffort has actual date time, using temporal expression has no sense
                        if (UtilValidate.isNotEmpty(workEffort.getTimestamp("actualStartDate")) || UtilValidate.isNotEmpty(workEffort.getTimestamp("actualCompletionDate"))) {
                            continue;
                        }
                        TemporalExpression tempExpr = TemporalExpressionWorker.getTemporalExpression(delegator, workEffort.getString("tempExprId"));
                        DateRange weRange = new DateRange(workEffort.getTimestamp("estimatedStartDate"), workEffort.getTimestamp("estimatedCompletionDate"));

                        Set<Date> occurrences = tempExpr.getRange(range, cal);
                        for (Date occurrence : occurrences) {
                            for (DateRange periodRange : periodRanges) {
                                if (periodRange.includesDate(occurrence)) {
                                    GenericValue cloneWorkEffort = (GenericValue) workEffort.clone();
                                    TimeDuration duration = TimeDuration.fromNumber(workEffort.getDouble("estimatedMilliSeconds"));
                                    if (!duration.isZero()) {
                                        Calendar endCal = UtilDateTime.toCalendar(occurrence, timeZone, locale);
                                        Date endDate = duration.addToCalendar(endCal).getTime();
                                        cloneWorkEffort.set("estimatedStartDate", new Timestamp(occurrence.getTime()));
                                        cloneWorkEffort.set("estimatedCompletionDate", new Timestamp(endDate.getTime()));
                                    } else {
                                        cloneWorkEffort.set("estimatedStartDate", periodRange.startStamp());
                                        cloneWorkEffort.set("estimatedCompletionDate", periodRange.endStamp());
                                    }
                                    if (weRange.includes(cloneWorkEffort.getTimestamp("estimatedStartDate"))) {
                                       inclusions.add(cloneWorkEffort);
                                    }
                                }
                            }
                        }
                        exclusions.add(workEffort);
                    }
                }
                validWorkEfforts.removeAll(exclusions);
                validWorkEfforts.addAll(inclusions);
            } catch (GenericEntityException e) {
                Debug.logWarning(e, module);
            }

            // For each period in the set we check all work efforts to see if they fall within range
            boolean firstEntry = true;
            for (DateRange periodRange : periodRanges) {
                List<Map<String, Object>> curWorkEfforts = new LinkedList<>();
                Map<String, Object> entry = new HashMap<>();
                for (GenericValue workEffort : validWorkEfforts) {
                    Timestamp startDate = workEffort.getTimestamp("estimatedStartDate");
                    if (workEffort.getTimestamp("actualStartDate") != null) {
                        startDate = workEffort.getTimestamp("actualStartDate");
                    }
                    Timestamp endDate = workEffort.getTimestamp("estimatedCompletionDate");
                    if (workEffort.getTimestamp("actualCompletionDate") != null) {
                        endDate = workEffort.getTimestamp("actualCompletionDate");
                    }
                    if (endDate == null) {
                        endDate = startDate;
                    }
                    DateRange weRange = new DateRange(startDate, endDate);
                    if (periodRange.intersectsRange(weRange)) {
                        Map<String, Object> calEntry = new HashMap<>();
                        calEntry.put("workEffort", workEffort);
                        long length = ((weRange.end().after(endStamp) ? endStamp.getTime() : weRange.end().getTime()) - (weRange.start().before(startStamp) ? startStamp.getTime() : weRange.start().getTime()));
                        int periodSpan = (int) Math.ceil((double) length / periodLen);
                        if (length % periodLen == 0 && startDate.getTime() > periodRange.start().getTime()) {
                            periodSpan++;
                        }
                        calEntry.put("periodSpan", periodSpan);
                        DateRange calEntryRange = new DateRange((weRange.start().before(startStamp) ? startStamp : weRange.start()), (weRange.end().after(endStamp) ? endStamp : weRange.end()));
                        calEntry.put("calEntryRange", calEntryRange);
                        if (firstEntry) {
                            // If this is the first period any valid entry is starting here
                            calEntry.put("startOfPeriod", Boolean.TRUE);
                            firstEntry = false;
                        } else {
                            boolean startOfPeriod = ((weRange.start().getTime() - periodRange.start().getTime()) >= 0);
                            calEntry.put("startOfPeriod", startOfPeriod);
                        }
                        curWorkEfforts.add(calEntry);
                    }
                }
                int numEntries = curWorkEfforts.size();
                if (numEntries > maxConcurrentEntries) {
                    maxConcurrentEntries = numEntries;
                }
                entry.put("start", periodRange.startStamp());
                entry.put("end", periodRange.endStamp());
                entry.put("calendarEntries", curWorkEfforts);
                entry.put("calendarEntriesByDateRange", groupCalendarEntriesByDateRange(periodRange, curWorkEfforts));
                periods.add(entry);
            }
        }
        Map<String, Object> result = new HashMap<>();
        result.put("periods", periods);
        result.put("maxConcurrentEntries", maxConcurrentEntries);
        return result;
    }

    public static Map<String, Object> getProductManufacturingSummaryByFacility(DispatchContext ctx, Map<String, ? extends Object> context) {
        Delegator delegator = ctx.getDelegator();
        String productId = (String) context.get("productId");
        String facilityId = (String) context.get("facilityId"); // optional
        Locale locale = (Locale) context.get("locale");

        Map<String, Map<String, Object>> summaryInByFacility = new HashMap<>();
        Map<String, Map<String, Object>> summaryOutByFacility = new HashMap<>();
        try {
            //
            // Information about the running production runs that are going
            // to produce units of productId by facility.
            //
            List<EntityCondition> findIncomingProductionRunsConds = new LinkedList<>();

            findIncomingProductionRunsConds.add(EntityCondition.makeCondition("productId", EntityOperator.EQUALS, productId));
            findIncomingProductionRunsConds.add(EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, "WEGS_CREATED"));
            findIncomingProductionRunsConds.add(EntityCondition.makeCondition("workEffortGoodStdTypeId", EntityOperator.EQUALS, "PRUN_PROD_DELIV"));
            if (facilityId != null) {
                findIncomingProductionRunsConds.add(EntityCondition.makeCondition("facilityId", EntityOperator.EQUALS, facilityId));
            }

            List<EntityCondition> findIncomingProductionRunsStatusConds = new LinkedList<>();
            findIncomingProductionRunsStatusConds.add(EntityCondition.makeCondition("currentStatusId", EntityOperator.EQUALS, "PRUN_CREATED"));
            findIncomingProductionRunsStatusConds.add(EntityCondition.makeCondition("currentStatusId", EntityOperator.EQUALS, "PRUN_SCHEDULED"));
            findIncomingProductionRunsStatusConds.add(EntityCondition.makeCondition("currentStatusId", EntityOperator.EQUALS, "PRUN_DOC_PRINTED"));
            findIncomingProductionRunsStatusConds.add(EntityCondition.makeCondition("currentStatusId", EntityOperator.EQUALS, "PRUN_RUNNING"));
            findIncomingProductionRunsConds.add(EntityCondition.makeCondition(findIncomingProductionRunsStatusConds, EntityOperator.OR));

            List<GenericValue> incomingProductionRuns = EntityQuery.use(delegator).from("WorkEffortAndGoods").where(findIncomingProductionRunsConds).orderBy("-estimatedCompletionDate").queryList();
            for (GenericValue incomingProductionRun: incomingProductionRuns) {
                double producedQtyTot = 0.0;
                if ("PRUN_COMPLETED".equals(incomingProductionRun.getString("currentStatusId"))) {
                    List<GenericValue> inventoryItems = EntityQuery.use(delegator).from("WorkEffortAndInventoryProduced").where("productId", productId, "workEffortId", incomingProductionRun.getString("workEffortId")).queryList();
                    for (GenericValue inventoryItem: inventoryItems) {
                        GenericValue inventoryItemDetail = EntityQuery.use(delegator).from("InventoryItemDetail").where("inventoryItemId", inventoryItem.getString("inventoryItemId")).orderBy("inventoryItemDetailSeqId").queryFirst();
                        if (inventoryItemDetail != null && inventoryItemDetail.get("quantityOnHandDiff") != null) {
                            Double inventoryItemQty = inventoryItemDetail.getDouble("quantityOnHandDiff");
                            producedQtyTot = producedQtyTot + inventoryItemQty;
                        }
                    }
                }
                double estimatedQuantity = 0.0;
                if (incomingProductionRun.get("estimatedQuantity") != null) {
                    estimatedQuantity = incomingProductionRun.getDouble("estimatedQuantity");
                }
                double remainingQuantity = estimatedQuantity - producedQtyTot; // the qty that still needs to be produced
                if (remainingQuantity > 0) {
                    incomingProductionRun.set("estimatedQuantity", remainingQuantity);
                } else {
                    continue;
                }
                String weFacilityId = incomingProductionRun.getString("facilityId");

                Map<String, Object> quantitySummary = UtilGenerics.cast(summaryInByFacility.get(weFacilityId));
                if (quantitySummary == null) {
                    quantitySummary = new HashMap<>();
                    quantitySummary.put("facilityId", weFacilityId);
                    summaryInByFacility.put(weFacilityId, quantitySummary);
                }
                Double remainingQuantityTot = (Double)quantitySummary.get("estimatedQuantityTotal");
                if (remainingQuantityTot == null) {
                    quantitySummary.put("estimatedQuantityTotal", remainingQuantity);
                } else {
                    quantitySummary.put("estimatedQuantityTotal", remainingQuantity + remainingQuantityTot);
                }

                List<GenericValue> incomingProductionRunList = UtilGenerics.cast(quantitySummary.get("incomingProductionRunList"));
                if (incomingProductionRunList == null) {
                    incomingProductionRunList = new LinkedList<>();
                    quantitySummary.put("incomingProductionRunList", incomingProductionRunList);
                }
                incomingProductionRunList.add(incomingProductionRun);
            }
            //
            // Information about the running production runs that are going
            // to consume units of productId by facility.
            //
            List<EntityCondition> findOutgoingProductionRunsConds = new LinkedList<>();

            findOutgoingProductionRunsConds.add(EntityCondition.makeCondition("productId", EntityOperator.EQUALS, productId));
            findOutgoingProductionRunsConds.add(EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, "WEGS_CREATED"));
            findOutgoingProductionRunsConds.add(EntityCondition.makeCondition("workEffortGoodStdTypeId", EntityOperator.EQUALS, "PRUNT_PROD_NEEDED"));
            if (facilityId != null) {
                findOutgoingProductionRunsConds.add(EntityCondition.makeCondition("facilityId", EntityOperator.EQUALS, facilityId));
            }

            List<EntityCondition> findOutgoingProductionRunsStatusConds = new LinkedList<>();
            findOutgoingProductionRunsStatusConds.add(EntityCondition.makeCondition("currentStatusId", EntityOperator.EQUALS, "PRUN_CREATED"));
            findOutgoingProductionRunsStatusConds.add(EntityCondition.makeCondition("currentStatusId", EntityOperator.EQUALS, "PRUN_SCHEDULED"));
            findOutgoingProductionRunsStatusConds.add(EntityCondition.makeCondition("currentStatusId", EntityOperator.EQUALS, "PRUN_DOC_PRINTED"));
            findOutgoingProductionRunsStatusConds.add(EntityCondition.makeCondition("currentStatusId", EntityOperator.EQUALS, "PRUN_RUNNING"));
            findOutgoingProductionRunsConds.add(EntityCondition.makeCondition(findOutgoingProductionRunsStatusConds, EntityOperator.OR));

            List<GenericValue> outgoingProductionRuns = EntityQuery.use(delegator).from("WorkEffortAndGoods").where(findOutgoingProductionRunsConds).orderBy("-estimatedStartDate").queryList();
            for (GenericValue outgoingProductionRun: outgoingProductionRuns) {
                String weFacilityId = outgoingProductionRun.getString("facilityId");
                Double neededQuantity = outgoingProductionRun.getDouble("estimatedQuantity");
                if (neededQuantity == null) {
                    neededQuantity = (double) 0;
                }

                Map<String, Object> quantitySummary = UtilGenerics.cast(summaryOutByFacility.get(weFacilityId));
                if (quantitySummary == null) {
                    quantitySummary = new HashMap<>();
                    quantitySummary.put("facilityId", weFacilityId);
                    summaryOutByFacility.put(weFacilityId, quantitySummary);
                }
                Double remainingQuantityTot = (Double)quantitySummary.get("estimatedQuantityTotal");
                if (remainingQuantityTot == null) {
                    quantitySummary.put("estimatedQuantityTotal", neededQuantity);
                } else {
                    quantitySummary.put("estimatedQuantityTotal", neededQuantity + remainingQuantityTot);
                }

                List<GenericValue> outgoingProductionRunList = UtilGenerics.cast(quantitySummary.get("outgoingProductionRunList"));
                if (outgoingProductionRunList == null) {
                    outgoingProductionRunList = new LinkedList<>();
                    quantitySummary.put("outgoingProductionRunList", outgoingProductionRunList);
                }
                outgoingProductionRunList.add(outgoingProductionRun);
            }

        } catch (GenericEntityException gee) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError,
                    "WorkEffortManufacturingError", UtilMisc.toMap("productId", productId, "errorString", gee.getMessage()), locale));
        }
        Map<String, Object> resultMap = ServiceUtil.returnSuccess();
        resultMap.put("summaryInByFacility", summaryInByFacility);
        resultMap.put("summaryOutByFacility", summaryOutByFacility);
        return resultMap;
    }

    /** Process work effort event reminders. This service is used by the job scheduler.
     * @param ctx the dispatch context
     * @param context the context
     * @return returns the result of the service execution
     */
    public static Map<String, Object> processWorkEffortEventReminders(DispatchContext ctx, Map<String, ? extends Object> context) {
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        Locale localePar = (Locale) context.get("locale");
        Timestamp now = new Timestamp(System.currentTimeMillis());
        List<GenericValue> eventReminders = null;
        try {
            eventReminders = EntityQuery.use(delegator).from("WorkEffortEventReminder")
                    .where(EntityCondition.makeCondition(UtilMisc.<EntityCondition>toList(EntityCondition.makeCondition("reminderDateTime", EntityOperator.EQUALS, null),
                            EntityCondition.makeCondition("reminderDateTime", EntityOperator.LESS_THAN_EQUAL_TO, now)), EntityOperator.OR))
                            .queryList();
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError,
                    "WorkEffortEventRemindersRetrivingError", UtilMisc.toMap("errorString", e), localePar));
        }
        for (GenericValue reminder : eventReminders) {
            if (UtilValidate.isEmpty(reminder.get("contactMechId"))) {
                continue;
            }
            int repeatCount = reminder.get("repeatCount") == null ? 0 : reminder.getLong("repeatCount").intValue();
            int currentCount = reminder.get("currentCount") == null ? 0 : reminder.getLong("currentCount").intValue();
            GenericValue workEffort = null;
            try {
                workEffort = reminder.getRelatedOne("WorkEffort", false);
            } catch (GenericEntityException e) {
                Debug.logWarning("Error while getting work effort: " + e, module);
            }
            if (workEffort == null) {
                try {
                    reminder.remove();
                } catch (GenericEntityException e) {
                    Debug.logWarning("Error while removing work effort event reminder: " + e, module);
                }
                continue;
            }
            Locale locale = reminder.getString("localeId") == null ? Locale.getDefault() : new Locale(reminder.getString("localeId"));
            TimeZone timeZone = reminder.getString("timeZoneId") == null ? TimeZone.getDefault() : TimeZone.getTimeZone(reminder.getString("timeZoneId"));
            Map<String, Object> parameters = UtilMisc.toMap("locale", locale, "timeZone", timeZone, "workEffortId", reminder.get("workEffortId"));

            Map<String, Object> processCtx = UtilMisc.toMap("reminder", reminder, "bodyParameters", parameters, "userLogin", context.get("userLogin"));

            Calendar cal = UtilDateTime.toCalendar(now, timeZone, locale);
            Timestamp reminderStamp = reminder.getTimestamp("reminderDateTime");
            Date eventDateTime = workEffort.getTimestamp("estimatedStartDate");
            String tempExprId = workEffort.getString("tempExprId");
            if (UtilValidate.isNotEmpty(tempExprId)) {
                TemporalExpression temporalExpression = null;
                try {
                    temporalExpression = TemporalExpressionWorker.getTemporalExpression(delegator, tempExprId);
                } catch (GenericEntityException e) {
                    Debug.logWarning("Error while getting temporal expression, id = " + tempExprId + ": " + e, module);
                }
                if (temporalExpression != null) {
                    eventDateTime = temporalExpression.first(cal).getTime();
                    Date reminderDateTime = null;
                    long reminderOffset = reminder.get("reminderOffset") == null ? 0 : reminder.getLong("reminderOffset");
                    if (reminderStamp == null) {
                        if (reminderOffset != 0) {
                            cal.setTime(eventDateTime);
                            TimeDuration duration = TimeDuration.fromLong(reminderOffset);
                            duration.addToCalendar(cal);
                            reminderDateTime = cal.getTime();
                        } else {
                            reminderDateTime = eventDateTime;
                        }
                    } else {
                        reminderDateTime = new Date(reminderStamp.getTime());
                    }
                    if (reminderDateTime.before(now) && reminderStamp != null) {
                        try {
                            parameters.put("eventDateTime", new Timestamp(eventDateTime.getTime()));

                            Map<String, Object> result = dispatcher.runSync("processWorkEffortEventReminder", processCtx);
                            if (ServiceUtil.isError(result)) {
                                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(result));
                            }
                            if (repeatCount != 0 && currentCount + 1 >= repeatCount) {
                                reminder.remove();
                            } else {
                                cal.setTime(reminderDateTime);
                                Date newReminderDateTime = null;
                                if (reminderOffset != 0) {
                                    TimeDuration duration = TimeDuration.fromLong(-reminderOffset);
                                    duration.addToCalendar(cal);
                                    cal.setTime(temporalExpression.next(cal).getTime());
                                    duration = TimeDuration.fromLong(reminderOffset);
                                    duration.addToCalendar(cal);
                                    newReminderDateTime = cal.getTime();
                                } else {
                                    newReminderDateTime = temporalExpression.next(cal).getTime();
                                }
                                reminder.set("currentCount", (long) (currentCount + 1));
                                reminder.set("reminderDateTime", new Timestamp(newReminderDateTime.getTime()));
                                reminder.store();
                            }
                        } catch (GenericEntityException e) {
                            Debug.logWarning("Error while processing temporal expression reminder, id = " + tempExprId + ": " + e, module);
                        } catch (GenericServiceException e) {
                            Debug.logError(e, module);
                        }
                    } else if (reminderStamp == null) {
                        try {
                            reminder.set("reminderDateTime", new Timestamp(reminderDateTime.getTime()));
                            reminder.store();
                        } catch (GenericEntityException e) {
                            Debug.logWarning("Error while processing temporal expression reminder, id = " + tempExprId + ": " + e, module);
                        }
                    }
                }
                continue;
            }
            if (reminderStamp != null) {
                Date reminderDateTime = new Date(reminderStamp.getTime());
                if (reminderDateTime.before(now)) {
                    try {
                        parameters.put("eventDateTime", eventDateTime);
                        Map<String, Object> result = dispatcher.runSync("processWorkEffortEventReminder", processCtx);
                        if (ServiceUtil.isError(result)) {
                            return ServiceUtil.returnError(ServiceUtil.getErrorMessage(result));
                        }
                        TimeDuration duration = TimeDuration.fromNumber(reminder.getLong("repeatInterval"));
                        if ((repeatCount != 0 && currentCount + 1 >= repeatCount) || duration.isZero()) {
                            reminder.remove();
                        } else {
                            cal.setTime(now);
                            duration.addToCalendar(cal);
                            reminderDateTime = cal.getTime();
                            reminder.set("currentCount", (long) (currentCount + 1));
                            reminder.set("reminderDateTime", new Timestamp(reminderDateTime.getTime()));
                            reminder.store();
                        }
                    } catch (GenericEntityException e) {
                        Debug.logWarning("Error while processing event reminder: " + e, module);
                    } catch (GenericServiceException e) {
                        Debug.logError(e, module);
                    }
                }
            }
        }
        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> processWorkEffortEventReminder(DispatchContext dctx, Map<String, ? extends Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        Map<String, Object> parameters = UtilGenerics.cast(context.get("bodyParameters"));
        GenericValue reminder = (GenericValue) context.get("reminder");
        GenericValue contactMech = null;
        try {
            contactMech = reminder.getRelatedOne("ContactMech", false);
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
        if (contactMech != null && "EMAIL_ADDRESS".equals(contactMech.get("contactMechTypeId"))) {
            String toAddress = contactMech.getString("infoString");

            GenericValue emailTemplateSetting = null;
            try {
                emailTemplateSetting = EntityQuery.use(delegator).from("EmailTemplateSetting").where("emailTemplateSettingId", "WEFF_EVENT_REMINDER").cache().queryOne();
            } catch (GenericEntityException e1) {
                Debug.logError(e1, module);
            }
            if (emailTemplateSetting != null) {
                Map<String, Object> emailCtx = UtilMisc.toMap("emailTemplateSettingId", "WEFF_EVENT_REMINDER", "sendTo", toAddress, "bodyParameters", parameters);
                try {
                    dispatcher.runAsync("sendMailFromTemplateSetting", emailCtx);
                } catch (GenericServiceException e) {
                    Debug.logWarning("Error while emailing event reminder - workEffortId = " + reminder.get("workEffortId") + ", contactMechId = " + reminder.get("contactMechId") + ": " + e, module);
                }
            } else {
                Debug.logError("No email template (WEFF_EVENT_REMINDER) has been configured, reminder cannot be send.", module);
            }
            return ServiceUtil.returnSuccess();
        }
        // TODO: Other contact mechanism types
        Debug.logWarning("Invalid event reminder contact mech, workEffortId = " + reminder.get("workEffortId") + ", contactMechId = " + reminder.get("contactMechId"), module);
        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> removeDuplicateWorkEfforts(DispatchContext ctx, Map<String, ? extends Object> context) {
        List<GenericValue> resultList = null;
        try (EntityListIterator eli = (EntityListIterator) context.get("workEffortIterator")) {
            if (eli != null) {
                Set<String> keys = new HashSet<>();
                resultList = new LinkedList<>();
                GenericValue workEffort = eli.next();
                while (workEffort != null) {
                    String workEffortId = workEffort.getString("workEffortId");
                    if (!keys.contains(workEffortId)) {
                        resultList.add(workEffort);
                        keys.add(workEffortId);
                    }
                    workEffort = eli.next();
                }
            } else {
                List<GenericValue> workEfforts = UtilGenerics.cast(context.get("workEfforts"));
                if (workEfforts != null) {
                    resultList = WorkEffortWorker.removeDuplicateWorkEfforts(workEfforts);
                }
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put("workEfforts", resultList);
        return result;
    }
}
