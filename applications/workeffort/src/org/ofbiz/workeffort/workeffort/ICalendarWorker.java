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

import java.net.URISyntaxException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javolution.util.FastList;

import net.fortuna.ical4j.model.*;
import net.fortuna.ical4j.model.component.*;
import net.fortuna.ical4j.model.parameter.*;
import net.fortuna.ical4j.model.property.*;

import org.ofbiz.base.util.DateRange;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.TimeDuration;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.service.calendar.TemporalExpression;
import org.ofbiz.service.calendar.TemporalExpressionWorker;

/** iCalendar worker class. This class uses the <a href="http://ical4j.sourceforge.net/index.html">
 * iCal4J</a> library. */
public class ICalendarWorker {
    public static final String module = ICalendarWorker.class.getName();
    
    protected static final ProdId prodId = new ProdId("-//Apache Open For Business//Work Effort Calendar//EN");
    protected static final Map<String, Status> statusMap = UtilMisc.toMap("CAL_TENTATIVE", Status.VEVENT_TENTATIVE,
            "CAL_CONFIRMED", Status.VEVENT_CONFIRMED, "CAL_CANCELLED", Status.VEVENT_CANCELLED);
    protected static final String uidPrefix = "org-apache-ofbiz-we-";

    /** Returns a calendar derived from a Work Effort calendar publish point. 
     * 
     * @param delegator
     * @param workEffortId ID of a work effort with <code>workEffortTypeId</code> equal to
     * <code>PUBLISH_PROPS</code>.
     * @return A <code>net.fortuna.ical4j.model.Calendar</code> instance, or <code>null</code>
     * if <code>workEffortId</code> is invalid.
     * @throws GenericEntityException
     */
    public static net.fortuna.ical4j.model.Calendar getICalendar(GenericDelegator delegator, String workEffortId) throws GenericEntityException {
        GenericValue publishProperties = delegator.findByPrimaryKey("WorkEffort", UtilMisc.toMap("workEffortId", workEffortId));
        if (publishProperties == null || !"PUBLISH_PROPS".equals(publishProperties.get("workEffortTypeId"))) {
            return null;
        }
        net.fortuna.ical4j.model.Calendar calendar = makeCalendar(publishProperties);
        ComponentList components = calendar.getComponents();
        List<GenericValue> workEfforts = getRelatedWorkEfforts(publishProperties);
        for (GenericValue workEffort : workEfforts) {
            components.add(makeCalendarComponent(workEffort));
        }
        if (Debug.verboseOn()) {
            try {
                calendar.validate(true);
                Debug.logVerbose("iCalendar passes validation", module);
            } catch (ValidationException e) {
                Debug.logVerbose("iCalendar fails validation: " + e, module);
            }
        }
        return calendar;
    }

    /** Returns a <code>List</code> of work efforts related to a work effort calendar
     * publish point.<p>The <code>List</code> includes:<ul><li>All public work efforts of all
     * parties related to the publish point work effort</li><li>All public work efforts
     * of all fixed assets related to the publish point work effort</li><li>All
     * child work efforts of the publish point work effort</li></ul></p> 
     * 
     * @param workEffort
     * @return A <code>List</code> of related work efforts
     * @throws GenericEntityException
     */
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

    /** Returns a <code>Component</code> instance based on a work effort.
     * If the work effort is a task, then a <code>VToDo</code> is returned,
     * otherwise a <code>VEvent</code> is returned.
     * 
     * @param workEffort
     * @return A <code>VToDo</code> or <code>VEvent</code> instance
     * @throws GenericEntityException
     */
    public static Component makeCalendarComponent(GenericValue workEffort) throws GenericEntityException {
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
        eventProps.add(new Uid(uidPrefix.concat(workEffortId)));
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
                    eventProps.add(new Organizer("CN:".concat(partyName)));
                } else {
                    eventProps.add(new Attendee("CN:".concat(partyName)));
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
        ComponentList alarms = null;
        Component result = null;
        if ("TASK".equals(workEffort.get("workEffortTypeId"))) {
            VToDo toDo = new VToDo(eventProps);
            alarms = toDo.getAlarms();
            result = toDo;
        } else {
            VEvent event = new VEvent(eventProps);
            alarms = event.getAlarms();
            result = event;
        }
        getAlarms(workEffort, alarms);
        if (Debug.verboseOn()) {
            try {
                result.validate(true);
                Debug.logVerbose("iCalendar component passes validation", module);
            } catch (ValidationException e) {
                Debug.logVerbose("iCalendar component fails validation: " + e, module);
            }
        }
        return result;
    }

    /** Returns a new <code>net.fortuna.ical4j.model.Calendar</code> instance,
     * based on a work effort calendar publish point.
     * 
     * @param workEffort
     * @return
     * @throws GenericEntityException
     */
    public static net.fortuna.ical4j.model.Calendar makeCalendar(GenericValue workEffort) throws GenericEntityException {
        net.fortuna.ical4j.model.Calendar calendar = new net.fortuna.ical4j.model.Calendar();
        PropertyList propList = calendar.getProperties();
        propList.add(prodId);
        propList.add(Version.VERSION_2_0);
        propList.add(CalScale.GREGORIAN);
        // TODO: Get time zone from publish properties value
        java.util.TimeZone tz = java.util.TimeZone.getDefault();
        TimeZoneRegistry registry = TimeZoneRegistryFactory.getInstance().createRegistry();
        net.fortuna.ical4j.model.TimeZone timezone = registry.getTimeZone(tz.getID());
        calendar.getComponents().add(timezone.getVTimeZone());
        return calendar;
    }

    /** Converts <code>WorkEffortEventReminder</code> entities to <code>VAlarm</code>
     * instances, and adds them to a <code>ComponentList</code>.
     * 
     * @param workEffort The work effort to get the event reminders for
     * @param alarms The <code>ComponentList</code> that will contain the
     * <code>VAlarm</code> instances
     * @throws GenericEntityException
     */
    public static void getAlarms(GenericValue workEffort, ComponentList alarms) throws GenericEntityException {
        Description description = null;
        if (workEffort.get("description") != null) {
            description = new Description(workEffort.getString("description"));
        } else {
            description = new Description(workEffort.getString("workEffortName"));
        }
        Summary summary = new Summary(UtilProperties.getMessage("WorkEffortUiLabels", "WorkEffortEventReminder", Locale.getDefault()));
        GenericDelegator delegator = workEffort.getDelegator();
        List<GenericValue> reminderList = delegator.findList("WorkEffortEventReminder", EntityCondition.makeCondition("workEffortId", EntityOperator.EQUALS, workEffort.get("workEffortId")), null, null, null, false);
        for (GenericValue reminder : reminderList) {
            VAlarm alarm = createAlarm(reminder);
            PropertyList alarmProps = alarm.getProperties();
            GenericValue contactMech = reminder.getRelatedOne("ContactMech");
            if (contactMech != null && "EMAIL_ADDRESS".equals(contactMech.get("contactMechTypeId"))) {
                try {
                    alarmProps.add(new Attendee(contactMech.getString("infoString")));
                    alarmProps.add(Action.EMAIL);
                    alarmProps.add(summary);
                    alarmProps.add(description);
                } catch (URISyntaxException e) {
                    alarmProps.add(Action.DISPLAY);
                    alarmProps.add(new Description("Error encountered while creating iCalendar: " + e));
                }
            } else {
                alarmProps.add(Action.DISPLAY);
                alarmProps.add(description);
            }
            if (Debug.verboseOn()) {
                try {
                    alarm.validate(true);
                    Debug.logVerbose("iCalendar alarm passes validation", module);
                } catch (ValidationException e) {
                    Debug.logVerbose("iCalendar alarm fails validation: " + e, module);
                }
            }
            alarms.add(alarm);
        }
    }

    /** Converts a <code>WorkEffortEventReminder</code> entity to a
     * <code>VAlarm</code> instance.
     * 
     * @param workEffortEventReminder
     * @return A <code>VAlarm</code> instance
     * @throws GenericEntityException
     */
    public static VAlarm createAlarm(GenericValue workEffortEventReminder) {
        VAlarm alarm = null;
        Timestamp reminderStamp = workEffortEventReminder.getTimestamp("reminderDateTime");
        if (reminderStamp != null) {
            alarm = new VAlarm(new DateTime(reminderStamp));
        } else {
            long reminderOffset = workEffortEventReminder.get("reminderOffset") == null ? 0 : workEffortEventReminder.getLong("reminderOffset").longValue();
            TimeDuration duration = TimeDuration.fromLong(reminderOffset);
            alarm = new VAlarm(new Dur(duration.days(), duration.hours(), duration.minutes(), duration.seconds()));
        }
        return alarm;
    }
}
