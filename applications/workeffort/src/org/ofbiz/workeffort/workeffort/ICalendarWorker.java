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

package org.ofbiz.workeffort.workeffort;

import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import javolution.util.FastList;

import net.fortuna.ical4j.model.*;
import net.fortuna.ical4j.model.component.*;
import net.fortuna.ical4j.model.parameter.*;
import net.fortuna.ical4j.model.property.*;

import org.ofbiz.base.util.DateRange;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.TimeDuration;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityConditionList;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.service.calendar.TemporalExpression;
import org.ofbiz.service.calendar.TemporalExpressionWorker;

/** iCalendar worker class. */
public class ICalendarWorker {
    public static final String module = ICalendarWorker.class.getName();
    protected static ProdId prodId = new ProdId("-//Apache Open For Business//Work Effort Calendar//EN");
    protected static Map<String, Status> statusMap = UtilMisc.toMap("CAL_TENTATIVE", Status.VEVENT_TENTATIVE,
            "CAL_CONFIRMED", Status.VEVENT_CONFIRMED, "CAL_CANCELLED", Status.VEVENT_CANCELLED);
    protected static String workEffortIdPropName = "X-ORG-OFBIZ-WORKEFFORT-ID";

    public static net.fortuna.ical4j.model.Calendar getICalendar(GenericDelegator delegator, String workEffortId) throws GenericEntityException {
        GenericValue calendarProperties = delegator.findByPrimaryKey("WorkEffort", UtilMisc.toMap("workEffortId", workEffortId));
        if (calendarProperties == null || !"PUBLISH_PROPS".equals(calendarProperties.get("workEffortTypeId"))) {
            return null;
        }
        net.fortuna.ical4j.model.Calendar calendar = makeCalendar(calendarProperties);
        ComponentList components = calendar.getComponents();
        List<GenericValue> workEfforts = getRelatedWorkEfforts(calendarProperties);
        for (GenericValue workEffort : workEfforts) {
            components.add(makeEvent(workEffort));
        }
        return calendar;
    }

    public static List<GenericValue> getRelatedWorkEfforts(GenericValue workEffort) throws GenericEntityException {
        GenericDelegator delegator = workEffort.getDelegator();
        String workEffortId = workEffort.getString("workEffortId");
        List<GenericValue> relatedParties = EntityUtil.filterByDate(delegator.findList("WorkEffortPartyAssignment", EntityCondition.makeCondition("workEffortId", EntityOperator.EQUALS, workEffortId), null, null, null, false));
        List<GenericValue> relatedFixedAssets = EntityUtil.filterByDate(delegator.findList("WorkEffortFixedAssetAssign", EntityCondition.makeCondition("workEffortId", EntityOperator.EQUALS, workEffortId), null, null, null, false));
        List<GenericValue> workEfforts = FastList.newInstance();
        List<EntityCondition> conditionList = UtilMisc.<EntityCondition>toList(
                EntityCondition.makeCondition("scopeEnumId", EntityOperator.EQUALS, "WES_PUBLIC"),
                EntityCondition.makeCondition("workEffortTypeId", EntityOperator.NOT_EQUAL, "PUBLISH_PROPS"));
        EntityExpr variableExpr = EntityCondition.makeCondition("partyId", EntityOperator.EQUALS, "");
        conditionList.add(variableExpr);
        EntityCondition workEffortCond = EntityCondition.makeCondition(conditionList);
        for (GenericValue partyValue : relatedParties) {
            variableExpr.init("partyId", EntityOperator.EQUALS, partyValue.get("partyId"));
            workEfforts.addAll(EntityUtil.filterByDate(delegator.findList("WorkEffortAndPartyAssign", workEffortCond, null, null, null, false)));
        }
        for (GenericValue fixedAssetValue : relatedFixedAssets) {
            variableExpr.init("fixedAssetId", EntityOperator.EQUALS, fixedAssetValue.get("fixedAssetId"));
            workEfforts.addAll(delegator.findList("WorkEffort", workEffortCond, null, null, null, false));
            workEfforts.addAll(EntityUtil.filterByDate(delegator.findList("WorkEffortAndFixedAssetAssign", workEffortCond, null, null, null, false)));
        }
        workEfforts.addAll(EntityUtil.filterByDate(delegator.findList("WorkEffortAssocToView", EntityCondition.makeCondition("workEffortIdFrom", EntityOperator.EQUALS, workEffortId), null, null, null, false)));
        return WorkEffortWorker.removeDuplicateWorkEfforts(workEfforts);
    }

    public static VEvent makeEvent(GenericValue workEffort) throws GenericEntityException {
        GenericDelegator delegator = workEffort.getDelegator();
        String workEffortId = workEffort.getString("workEffortId");
        PropertyList eventProps = new PropertyList();
        eventProps.add(new DtStamp()); // iCalendar object created date/time
        if (workEffort.getTimestamp("createdDate") != null) {
            eventProps.add(new Created(new DateTime(workEffort.getTimestamp("createdDate"))));
        }
        if (workEffort.getTimestamp("lastModifiedDate") != null) {
            eventProps.add(new LastModified(new DateTime(workEffort.getTimestamp("lastModifiedDate"))));
        }
        eventProps.add(new XProperty(workEffortIdPropName, workEffort.getString("workEffortId")));
        eventProps.add(new Summary(workEffort.getString("workEffortName")));
        Status eventStatus = statusMap.get(workEffort.getString("currentStatusId"));
        if (eventStatus != null) {
            eventProps.add(statusMap.get(workEffort.getString("currentStatusId")));
        }
        Double durationMillis = workEffort.getDouble("estimatedMilliSeconds");
        if (durationMillis != null) {
            TimeDuration duration = TimeDuration.fromLong(durationMillis.longValue());
            eventProps.add(new Duration(new Dur(duration.days(), duration.hours(), duration.minutes(), duration.seconds())));
        }
        List<GenericValue> relatedParties = EntityUtil.filterByDate(delegator.findList("WorkEffortPartyAssignView", EntityCondition.makeCondition("workEffortId", EntityOperator.EQUALS, workEffortId), null, null, null, false));
        for (GenericValue partyValue : relatedParties) {
            ParameterList paramList = new ParameterList();
            String partyName = partyValue.getString("groupName");
            if (UtilValidate.isEmpty(partyName)) {
                partyName = partyValue.getString("firstName") + " " + partyValue.getString("lastName");
            }
            paramList.add(new Cn(partyName));
            // paramList.add(new XParameter(partyIdPropName, partyValue.getString("partyId")));
            try {
                if ("CAL_ORGANIZER~CAL_OWNER".contains(partyValue.getString("roleTypeId"))) {
                    eventProps.add(new Organizer(paramList, ""));
                } else {
                    eventProps.add(new Attendee(paramList, ""));
                }
            } catch (Exception e) {}
        }
        DateRange range = new DateRange(workEffort.getTimestamp("estimatedStartDate"), workEffort.getTimestamp("estimatedCompletionDate"));
        eventProps.add(new DtStart(new DateTime(range.start())));
        if (UtilValidate.isNotEmpty(workEffort.getString("tempExprId"))) {
            TemporalExpression tempExpr = TemporalExpressionWorker.getTemporalExpression(delegator, workEffort.getString("tempExprId"));
            if (tempExpr != null) {
                try {
                    ICalRecurConverter.convert(tempExpr, eventProps);
                } catch (Exception e) {
                    eventProps.add(new Description("Error while converting recurrence: " + e));
                    eventProps.add(new DtStart());
                    eventProps.add(new DtEnd());
                    return new VEvent(eventProps);
                }
            }
        } else {
            eventProps.add(new DtEnd(new DateTime(range.end())));
        }
        if (workEffort.getString("description") != null) {
            eventProps.add(new Description(workEffort.getString("description")));
        }
        return new VEvent(eventProps);
    }

    public static net.fortuna.ical4j.model.Calendar makeCalendar(GenericValue workEffort) throws GenericEntityException {
        net.fortuna.ical4j.model.Calendar calendar = new net.fortuna.ical4j.model.Calendar();
        PropertyList propList = calendar.getProperties();
        propList.add(prodId);
        propList.add(Version.VERSION_2_0);
        propList.add(CalScale.GREGORIAN);
        if (workEffort.get("description") != null) {
            propList.add(new Description(workEffort.getString("description")));
        } else {
            propList.add(new Description(workEffort.getString("workEffortName")));
        }
        // TODO: Get time zone from publish properties value
        java.util.TimeZone tz = java.util.TimeZone.getDefault();
        TimeZoneRegistry registry = TimeZoneRegistryFactory.getInstance().createRegistry();
        net.fortuna.ical4j.model.TimeZone timezone = registry.getTimeZone(tz.getID());
        calendar.getComponents().add(timezone.getVTimeZone());
        return calendar;
    }
}
