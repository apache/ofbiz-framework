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

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.ComponentList;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.Dur;
import net.fortuna.ical4j.model.Parameter;
import net.fortuna.ical4j.model.ParameterList;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.PropertyList;
import net.fortuna.ical4j.model.TimeZoneRegistry;
import net.fortuna.ical4j.model.TimeZoneRegistryFactory;
import net.fortuna.ical4j.model.ValidationException;
import net.fortuna.ical4j.model.component.VAlarm;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.component.VToDo;
import net.fortuna.ical4j.model.parameter.Cn;
import net.fortuna.ical4j.model.parameter.PartStat;
import net.fortuna.ical4j.model.parameter.XParameter;
import net.fortuna.ical4j.model.property.Action;
import net.fortuna.ical4j.model.property.Attendee;
import net.fortuna.ical4j.model.property.CalScale;
import net.fortuna.ical4j.model.property.Clazz;
import net.fortuna.ical4j.model.property.Completed;
import net.fortuna.ical4j.model.property.Created;
import net.fortuna.ical4j.model.property.Description;
import net.fortuna.ical4j.model.property.DtEnd;
import net.fortuna.ical4j.model.property.DtStamp;
import net.fortuna.ical4j.model.property.DtStart;
import net.fortuna.ical4j.model.property.Duration;
import net.fortuna.ical4j.model.property.LastModified;
import net.fortuna.ical4j.model.property.Location;
import net.fortuna.ical4j.model.property.Organizer;
import net.fortuna.ical4j.model.property.PercentComplete;
import net.fortuna.ical4j.model.property.Priority;
import net.fortuna.ical4j.model.property.ProdId;
import net.fortuna.ical4j.model.property.Status;
import net.fortuna.ical4j.model.property.Summary;
import net.fortuna.ical4j.model.property.Uid;
import net.fortuna.ical4j.model.property.Version;
import net.fortuna.ical4j.model.property.XProperty;

import org.ofbiz.base.util.DateRange;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.ObjectType;
import org.ofbiz.base.util.TimeDuration;
import org.ofbiz.base.util.UtilGenerics;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.util.EntityQuery;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ModelParam;
import org.ofbiz.service.ModelService;
import org.ofbiz.service.ServiceUtil;
import org.ofbiz.service.calendar.TemporalExpression;
import org.ofbiz.service.calendar.TemporalExpressionWorker;
import org.ofbiz.workeffort.workeffort.ICalWorker.ResponseProperties;

/** iCalendar converter class. This class uses the <a href="http://ical4j.sourceforge.net/index.html">
 * iCal4J</a> library.
 */
public class ICalConverter {

    protected static final String module = ICalConverter.class.getName();
    protected static final String partyIdXParamName = "X-ORG-APACHE-OFBIZ-PARTY-ID";
    protected static final ProdId prodId = new ProdId("-//Apache OFBiz//Work Effort Calendar//EN");
    protected static final String workEffortIdParamName = "X-ORG-APACHE-OFBIZ-WORKEFFORT-ID";
    protected static final String uidPrefix = "ORG-APACHE-OFBIZ-WE-";
    protected static final String workEffortIdXPropName = "X-ORG-APACHE-OFBIZ-WORKEFFORT-ID";
    protected static final String reminderXPropName = "X-ORG-APACHE-OFBIZ-REMINDER-ID";
    protected static final Map<String, String> fromStatusMap = UtilMisc.toMap("TENTATIVE", "CAL_TENTATIVE",
            "CONFIRMED", "CAL_CONFIRMED", "CANCELLED", "CAL_CANCELLED", "NEEDS-ACTION", "CAL_NEEDS_ACTION",
            "COMPLETED", "CAL_COMPLETED", "IN-PROCESS", "CAL_ACCEPTED");
    protected static final Map<String, Status> toStatusMap = UtilMisc.toMap("CAL_TENTATIVE", Status.VEVENT_TENTATIVE,
            "CAL_CONFIRMED", Status.VEVENT_CONFIRMED, "CAL_CANCELLED", Status.VEVENT_CANCELLED,
            "CAL_NEEDS_ACTION", Status.VTODO_NEEDS_ACTION, "CAL_COMPLETED", Status.VTODO_COMPLETED,
            "CAL_ACCEPTED", Status.VTODO_IN_PROCESS);
    protected static final Map<String, PartStat> toPartStatusMap = UtilMisc.toMap(
            "PRTYASGN_OFFERED", PartStat.TENTATIVE, "PRTYASGN_ASSIGNED", PartStat.ACCEPTED);
    protected static final Map<String, String> fromPartStatusMap = UtilMisc.toMap(
            "TENTATIVE", "PRTYASGN_OFFERED", "ACCEPTED", "PRTYASGN_ASSIGNED");
    protected static final Map<String, String> fromRoleMap = UtilMisc.toMap("ATTENDEE", "CAL_ATTENDEE",
            "CONTACT", "CONTACT", "ORGANIZER", "CAL_ORGANIZER");

    protected static VAlarm createAlarm(GenericValue workEffortEventReminder) {
        VAlarm alarm = null;
        Timestamp reminderStamp = workEffortEventReminder.getTimestamp("reminderDateTime");
        if (reminderStamp != null) {
            alarm = new VAlarm(new DateTime(reminderStamp));
        } else {
            TimeDuration duration = TimeDuration.fromNumber(workEffortEventReminder.getLong("reminderOffset"));
            alarm = new VAlarm(new Dur(duration.days(), duration.hours(), duration.minutes(), duration.seconds()));
        }
        return alarm;
    }

    protected static Attendee createAttendee(GenericValue partyValue, Map<String, Object> context) {
        Attendee attendee = new Attendee();
        loadPartyAssignment(attendee, partyValue, context);
        return attendee;
    }

    protected static Organizer createOrganizer(GenericValue partyValue, Map<String, Object> context) {
        Organizer organizer = new Organizer();
        loadPartyAssignment(organizer, partyValue, context);
        return organizer;
    }

    protected static ResponseProperties createWorkEffort(Component component, Map<String, Object> context) {
        Map<String, Object> serviceMap = new HashMap<String, Object>();
        setWorkEffortServiceMap(component, serviceMap);
        serviceMap.put("workEffortTypeId", "VTODO".equals(component.getName()) ? "TASK" : "EVENT");
        serviceMap.put("currentStatusId", "VTODO".equals(component.getName()) ? "CAL_NEEDS_ACTION" : "CAL_TENTATIVE");
        serviceMap.put("partyId", ((GenericValue) context.get("userLogin")).get("partyId"));
        serviceMap.put("roleTypeId", "CAL_OWNER");
        serviceMap.put("statusId", "PRTYASGN_ASSIGNED");
        Map<String, Object> serviceResult = invokeService("createWorkEffortAndPartyAssign", serviceMap, context);
        if (ServiceUtil.isError(serviceResult)) {
            return ICalWorker.createPartialContentResponse(ServiceUtil.getErrorMessage(serviceResult));
        }
        String workEffortId = (String) serviceResult.get("workEffortId");
        if (workEffortId != null) {
            replaceProperty(component.getProperties(), toXProperty(workEffortIdXPropName, workEffortId));
            serviceMap.clear();
            serviceMap.put("workEffortIdFrom", context.get("workEffortId"));
            serviceMap.put("workEffortIdTo", workEffortId);
            serviceMap.put("workEffortAssocTypeId", "WORK_EFF_DEPENDENCY");
            serviceMap.put("fromDate", new Timestamp(System.currentTimeMillis()));
            serviceResult = invokeService("createWorkEffortAssoc", serviceMap, context);
            if (ServiceUtil.isError(serviceResult)) {
                return ICalWorker.createPartialContentResponse(ServiceUtil.getErrorMessage(serviceResult));
            }
            storePartyAssignments(workEffortId, component, context);
        }
        return null;
    }

    protected static String fromClazz(PropertyList propertyList) {
        Clazz iCalObj = (Clazz) propertyList.getProperty(Clazz.CLASS);
        if (iCalObj == null) {
            return null;
        }
        return "WES_".concat(iCalObj.getValue());
    }

    protected static Timestamp fromCompleted(PropertyList propertyList) {
        Completed iCalObj = (Completed) propertyList.getProperty(Completed.COMPLETED);
        if (iCalObj == null) {
            return null;
        }
        Date date = iCalObj.getDate();
        return new Timestamp(date.getTime());
    }

    protected static String fromDescription(PropertyList propertyList) {
        Description iCalObj = (Description) propertyList.getProperty(Description.DESCRIPTION);
        if (iCalObj == null) {
            return null;
        }
        return iCalObj.getValue();
    }

    protected static Timestamp fromDtEnd(PropertyList propertyList) {
        DtEnd iCalObj = (DtEnd) propertyList.getProperty(DtEnd.DTEND);
        if (iCalObj == null) {
            return null;
        }
        Date date = iCalObj.getDate();
        return new Timestamp(date.getTime());
    }

    protected static Timestamp fromDtStart(PropertyList propertyList) {
        DtStart iCalObj = (DtStart) propertyList.getProperty(DtStart.DTSTART);
        if (iCalObj == null) {
            return null;
        }
        Date date = iCalObj.getDate();
        return new Timestamp(date.getTime());
    }

    protected static Double fromDuration(PropertyList propertyList) {
        Duration iCalObj = (Duration) propertyList.getProperty(Duration.DURATION);
        if (iCalObj == null) {
            return null;
        }
        Dur dur = iCalObj.getDuration();
        TimeDuration td = new TimeDuration(0, 0, (dur.getWeeks() * 7) + dur.getDays(), dur.getHours(), dur.getMinutes(), dur.getSeconds(), 0);
        return new Double(TimeDuration.toLong(td));
    }

    protected static Timestamp fromLastModified(PropertyList propertyList) {
        LastModified iCalObj = (LastModified) propertyList.getProperty(LastModified.LAST_MODIFIED);
        if (iCalObj == null) {
            return null;
        }
        Date date = iCalObj.getDate();
        return new Timestamp(date.getTime());
    }

    protected static String fromLocation(PropertyList propertyList) {
        Location iCalObj = (Location) propertyList.getProperty(Location.LOCATION);
        if (iCalObj == null) {
            return null;
        }
        return iCalObj.getValue();
    }

    protected static String fromParticipationStatus(Parameter status) {
        if (status == null) {
            return null;
        }
        return fromPartStatusMap.get(status.getValue());
    }

    protected static Long fromPercentComplete(PropertyList propertyList) {
        PercentComplete iCalObj = (PercentComplete) propertyList.getProperty(PercentComplete.PERCENT_COMPLETE);
        if (iCalObj == null) {
            return null;
        }
        return new Long(iCalObj.getPercentage());
    }

    protected static Double fromPriority(PropertyList propertyList) {
        Priority iCalObj = (Priority) propertyList.getProperty(Priority.PRIORITY);
        if (iCalObj == null) {
            return null;
        }
        return new Double(iCalObj.getLevel());
    }

    protected static String fromStatus(PropertyList propertyList) {
        Status iCalObj = (Status) propertyList.getProperty(Status.STATUS);
        if (iCalObj == null) {
            return null;
        }
        return fromStatusMap.get(iCalObj.getValue());
    }

    protected static String fromSummary(PropertyList propertyList) {
        Summary iCalObj = (Summary) propertyList.getProperty(Summary.SUMMARY);
        if (iCalObj == null) {
            return null;
        }
        return iCalObj.getValue();
    }

    protected static String fromUid(PropertyList propertyList) {
        Uid iCalObj = (Uid) propertyList.getProperty(Uid.UID);
        if (iCalObj == null) {
            return null;
        }
        return iCalObj.getValue();
    }

    protected static String fromXParameter(ParameterList parameterList, String parameterName) {
        if (parameterName == null) {
            return null;
        }
        Parameter parameter = parameterList.getParameter(parameterName);
        if (parameter != null) {
            return parameter.getValue();
        }
        return null;
    }

    protected static String fromXProperty(PropertyList propertyList, String propertyName) {
        if (propertyName == null) {
            return null;
        }
        Property property = propertyList.getProperty(propertyName);
        if (property != null) {
            return property.getValue();
        }
        return null;
    }

    protected static void getAlarms(GenericValue workEffort, ComponentList alarms) throws GenericEntityException {
        Description description = null;
        if (workEffort.get("description") != null) {
            description = new Description(workEffort.getString("description"));
        } else {
            description = new Description(workEffort.getString("workEffortName"));
        }
        Summary summary = new Summary(UtilProperties.getMessage("WorkEffortUiLabels", "WorkEffortEventReminder", Locale.getDefault()));
        Delegator delegator = workEffort.getDelegator();
        String workEffortId = workEffort.getString("workEffortId");
        List<GenericValue> reminderList = EntityQuery.use(delegator).from("WorkEffortEventReminder").where("workEffortId", workEffort.get("workEffortId")).queryList();
        for (GenericValue reminder : reminderList) {
            String reminderId = workEffortId + "-" + reminder.getString("sequenceId");
            VAlarm alarm = null;
            PropertyList alarmProps = null;
            boolean newAlarm = true;
            Iterator<VAlarm> i = UtilGenerics.cast(alarms.iterator());
            while (i.hasNext()) {
                alarm = i.next();
                Property xProperty = alarm.getProperty(reminderXPropName);
                if (xProperty != null && reminderId.equals(xProperty.getValue())) {
                    newAlarm = false;
                    alarmProps = alarm.getProperties();
                    // TODO: Write update code. For now, just re-create
                    alarmProps.clear();
                    break;
                }
            }
            if (newAlarm) {
                alarm = createAlarm(reminder);
                alarms.add(alarm);
                alarmProps = alarm.getProperties();
                alarmProps.add(new XProperty(reminderXPropName, reminderId));
            }
            GenericValue contactMech = reminder.getRelatedOne("ContactMech", false);
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
        }
    }

    /** Returns a calendar derived from a Work Effort calendar publish point.
     * @param workEffortId ID of a work effort with <code>workEffortTypeId</code> equal to
     * <code>PUBLISH_PROPS</code>.
     * @param context The conversion context
     * @return An iCalendar as a <code>String</code>, or <code>null</code>
     * if <code>workEffortId</code> is invalid.
     * @throws GenericEntityException
     */
    public static ResponseProperties getICalendar(String workEffortId, Map<String, Object> context) throws GenericEntityException {
        Delegator delegator = (Delegator) context.get("delegator");
        GenericValue publishProperties = EntityQuery.use(delegator).from("WorkEffort").where("workEffortId", workEffortId).queryOne();
        if (!isCalendarPublished(publishProperties)) {
            Debug.logInfo("WorkEffort calendar is not published: " + workEffortId, module);
            return ICalWorker.createNotFoundResponse(null);
        }
        if (!"WES_PUBLIC".equals(publishProperties.get("scopeEnumId"))) {
            if (context.get("userLogin") == null) {
                return ICalWorker.createNotAuthorizedResponse(null);
            }
            if (!hasPermission(workEffortId, "VIEW", context)) {
                return ICalWorker.createForbiddenResponse(null);
            }
        }
        Calendar calendar = makeCalendar(publishProperties, context);
        ComponentList components = calendar.getComponents();
        List<GenericValue> workEfforts = getRelatedWorkEfforts(publishProperties, context);
        if (workEfforts != null) {
            for (GenericValue workEffort : workEfforts) {
                ResponseProperties responseProps = toCalendarComponent(components, workEffort, context);
                if (responseProps != null) {
                    return responseProps;
                }
            }
        }
        if (Debug.verboseOn()) {
            try {
                calendar.validate(true);
                Debug.logVerbose("iCalendar passes validation", module);
            } catch (ValidationException e) {
                Debug.logVerbose("iCalendar fails validation: " + e, module);
            }
        }
        return ICalWorker.createOkResponse(calendar.toString());
    }

    protected static void getPartyUrl(Property property, GenericValue partyAssign, Map<String, Object> context) {
        Map<String, ? extends Object> serviceMap = UtilMisc.toMap("partyId", partyAssign.get("partyId"));
        Map<String, Object> resultMap = invokeService("getPartyICalUrl", serviceMap, context);
        String iCalUrl = (String) resultMap.get("iCalUrl");
        if (iCalUrl != null) {
            if (!iCalUrl.contains(":") && iCalUrl.contains("@")) {
                iCalUrl = "MAILTO:".concat(iCalUrl);
            }
            try {
                property.setValue(iCalUrl);
            } catch (Exception e) {
                Debug.logError(e, "Error while setting party URI: ", module);
            }
        }
    }

    protected static List<GenericValue> getRelatedWorkEfforts(GenericValue workEffort, Map<String, Object> context) throws GenericEntityException {
        Map<String, ? extends Object> serviceMap = UtilMisc.toMap("workEffortId", workEffort.getString("workEffortId"));
        Map<String, Object> resultMap = invokeService("getICalWorkEfforts", serviceMap, context);
        List<GenericValue> workEfforts = UtilGenerics.checkList(resultMap.get("workEfforts"), GenericValue.class);
        if (workEfforts != null) {
            return WorkEffortWorker.removeDuplicateWorkEfforts(workEfforts);
        }
        return null;
    }

    protected static boolean hasPermission(String workEffortId, String action, Map<String, Object> context) {
        if (context.get("userLogin") == null) {
            return false;
        }
        Map<String, ? extends Object> serviceMap = UtilMisc.toMap("workEffortId", workEffortId, "mainAction", action);
        Map<String, Object> serviceResult = invokeService("workEffortICalendarPermission", serviceMap, context);
        Boolean hasPermission = (Boolean) serviceResult.get("hasPermission");
        if (hasPermission != null) {
            return hasPermission.booleanValue();
        } else {
            return false;
        }
    }

    protected static Map<String, Object> invokeService(String serviceName, Map<String, ? extends Object> serviceMap, Map<String, Object> context) {
        LocalDispatcher dispatcher = (LocalDispatcher) context.get("dispatcher");
        Map<String, Object> localMap = new HashMap<String, Object>();
        try {
            ModelService modelService = null;
            modelService = dispatcher.getDispatchContext().getModelService(serviceName);
            for (ModelParam modelParam: modelService.getInModelParamList()) {
                if (serviceMap.containsKey(modelParam.name)) {
                    Object value = serviceMap.get(modelParam.name);
                    if (UtilValidate.isNotEmpty(modelParam.type)) {
                        value = ObjectType.simpleTypeConvert(value, modelParam.type, null, null, null, true);
                    }
                    localMap.put(modelParam.name, value);
                }
            }
        } catch (Exception e) {
            String errMsg = "Error while creating service Map for service " + serviceName + ": ";
            Debug.logError(e, errMsg, module);
            return ServiceUtil.returnError(errMsg + e);
        }
        if (context.get("userLogin") != null) {
            localMap.put("userLogin", context.get("userLogin"));
        }
        localMap.put("locale", context.get("locale"));
        try {
            return dispatcher.runSync(serviceName, localMap);
        } catch (GenericServiceException e) {
            String errMsg = "Error while invoking service " + serviceName + ": ";
            Debug.logError(e, errMsg, module);
            return ServiceUtil.returnError(errMsg + e);
        }
    }

    protected static boolean isCalendarPublished(GenericValue publishProperties) {
        if (publishProperties == null || !"PUBLISH_PROPS".equals(publishProperties.get("workEffortTypeId"))) {
            return false;
        }
        DateRange range = new DateRange(publishProperties.getTimestamp("actualStartDate"), publishProperties.getTimestamp("actualCompletionDate"));
        return range.includesDate(new Date());
    }

    protected static void loadPartyAssignment(Property property, GenericValue partyAssign, Map<String, Object> context) {
        getPartyUrl(property, partyAssign, context);
        if (UtilValidate.isEmpty(property.getValue())) {
            try {
                // RFC 2445 4.8.4.1 and 4.8.4.3 Value must be a URL
                property.setValue("MAILTO:ofbiz-test@example.com");
            } catch (Exception e) {
                Debug.logError(e, "Error while setting Property value: ", module);
            }
        }
        ParameterList parameterList = property.getParameters();
        if (partyAssign != null) {
            replaceParameter(parameterList, toXParameter(partyIdXParamName, partyAssign.getString("partyId")));
            replaceParameter(parameterList, new Cn(makePartyName(partyAssign)));
            replaceParameter(parameterList, toParticipationStatus(partyAssign.getString("assignmentStatusId")));
        }
    }

    protected static void loadRelatedParties(List<GenericValue> relatedParties, PropertyList componentProps, Map<String, Object> context) {
        PropertyList attendees = componentProps.getProperties("ATTENDEE");
        for (GenericValue partyValue : relatedParties) {
            if ("CAL_ORGANIZER~CAL_OWNER".contains(partyValue.getString("roleTypeId"))) {
                // RFC 2445 4.6.1, 4.6.2, and 4.6.3 ORGANIZER can appear only once
                replaceProperty(componentProps, createOrganizer(partyValue, context));
            } else {
                String partyId = partyValue.getString("partyId");
                boolean newAttendee = true;
                Attendee attendee = null;
                Iterator<Attendee> i = UtilGenerics.cast(attendees.iterator());
                while (i.hasNext()) {
                    attendee = i.next();
                    Parameter xParameter = attendee.getParameter(partyIdXParamName);
                    if (xParameter != null && partyId.equals(xParameter.getValue())) {
                        loadPartyAssignment(attendee, partyValue, context);
                        newAttendee = false;
                        break;
                    }
                }
                if (newAttendee) {
                    attendee = createAttendee(partyValue, context);
                    componentProps.add(attendee);
                }
            }
        }
    }

    protected static void loadWorkEffort(PropertyList componentProps, GenericValue workEffort) {
        replaceProperty(componentProps, new DtStamp()); // iCalendar object created date/time
        replaceProperty(componentProps, toClazz(workEffort.getString("scopeEnumId")));
        replaceProperty(componentProps, toCreated(workEffort.getTimestamp("createdDate")));
        replaceProperty(componentProps, toDescription(workEffort.getString("description")));
        replaceProperty(componentProps, toDtStart(workEffort.getTimestamp("estimatedStartDate")));
        replaceProperty(componentProps, toLastModified(workEffort.getTimestamp("lastModifiedDate")));
        replaceProperty(componentProps, toPriority(workEffort.getLong("priority")));
        replaceProperty(componentProps, toLocation(workEffort.getString("locationDesc")));
        replaceProperty(componentProps, toStatus(workEffort.getString("currentStatusId")));
        replaceProperty(componentProps, toSummary(workEffort.getString("workEffortName")));
        Property uid = componentProps.getProperty(Uid.UID);
        if (uid == null) {
            // Don't overwrite UIDs created by calendar clients
            replaceProperty(componentProps, toUid(workEffort.getString("workEffortId")));
        }
        replaceProperty(componentProps, toXProperty(workEffortIdXPropName, workEffort.getString("workEffortId")));
    }

    protected static Calendar makeCalendar(GenericValue workEffort, Map<String, Object> context) throws GenericEntityException {
        String iCalData = null;
        GenericValue iCalValue = workEffort.getRelatedOne("WorkEffortIcalData", false);
        if (iCalValue != null) {
            iCalData = iCalValue.getString("icalData");
        }
        boolean newCalendar = true;
        Calendar calendar = null;
        if (iCalData == null) {
            Debug.logVerbose("iCalendar Data not found, creating new Calendar", module);
            calendar = new Calendar();
        } else {
            Debug.logVerbose("iCalendar Data found, using saved Calendar", module);
            StringReader reader = new StringReader(iCalData);
            CalendarBuilder builder = new CalendarBuilder();
            try {
                calendar = builder.build(reader);
                newCalendar = false;
            } catch (Exception e) {
                Debug.logError(e, "Error while parsing saved iCalendar, creating new iCalendar: ", module);
                calendar = new Calendar();
            }
        }
        PropertyList propList = calendar.getProperties();
        replaceProperty(propList, prodId);
        replaceProperty(propList, new XProperty(workEffortIdXPropName, workEffort.getString("workEffortId")));
        if (newCalendar) {
            propList.add(Version.VERSION_2_0);
            propList.add(CalScale.GREGORIAN);
            // TODO: Get time zone from publish properties value
            java.util.TimeZone tz = java.util.TimeZone.getDefault();
            TimeZoneRegistry registry = TimeZoneRegistryFactory.getInstance().createRegistry();
            net.fortuna.ical4j.model.TimeZone timezone = registry.getTimeZone(tz.getID());
            calendar.getComponents().add(timezone.getVTimeZone());
        }
        return calendar;
    }

    protected static String makePartyName(GenericValue partyAssign) {
        String partyName = partyAssign.getString("groupName");
        if (UtilValidate.isEmpty(partyName)) {
            partyName = partyAssign.getString("firstName") + " " + partyAssign.getString("lastName");
        }
        return partyName;
    }

    protected static void replaceParameter(ParameterList parameterList, Parameter parameter) {
        if (parameter == null) {
            return;
        }
        Parameter existingParam = parameterList.getParameter(parameter.getName());
        if (existingParam != null) {
            parameterList.remove(existingParam);
        }
        parameterList.add(parameter);
    }

    protected static void replaceProperty(PropertyList propertyList, Property property) {
        if (property == null) {
            return;
        }
        Property existingProp = propertyList.getProperty(property.getName());
        if (existingProp != null) {
            propertyList.remove(existingProp);
        }
        propertyList.add(property);
    }

    protected static void setMapElement(Map<String, Object> map, String key, Object value) {
        if (map == null || key == null || value == null) {
            return;
        }
        map.put(key, value);
    }

    protected static void setPartyIdFromUrl(Property property, Map<String, Object> context) {
        Map<String, ? extends Object> serviceMap = UtilMisc.toMap("address", property.getValue(), "caseInsensitive", "Y");
        Map<String, Object> resultMap = invokeService("findPartyFromEmailAddress", serviceMap, context);
        String partyId = (String) resultMap.get("partyId");
        if (partyId != null) {
            replaceParameter(property.getParameters(), toXParameter(partyIdXParamName, partyId));
        }
    }

    protected static void setWorkEffortServiceMap(Component component, Map<String, Object> serviceMap) {
        PropertyList propertyList = component.getProperties();
        setMapElement(serviceMap, "scopeEnumId", fromClazz(propertyList));
        setMapElement(serviceMap, "description", fromDescription(propertyList));
        setMapElement(serviceMap, "locationDesc", fromLocation(propertyList));
        setMapElement(serviceMap, "priority", fromPriority(propertyList));
        setMapElement(serviceMap, "currentStatusId", fromStatus(propertyList));
        setMapElement(serviceMap, "workEffortName", fromSummary(propertyList));
        setMapElement(serviceMap, "universalId", fromUid(propertyList));
        // Set some fields to null so calendar clients can revert changes
        serviceMap.put("estimatedStartDate", null);
        serviceMap.put("estimatedCompletionDate", null);
        serviceMap.put("estimatedMilliSeconds", null);
        serviceMap.put("lastModifiedDate", null);
        serviceMap.put("actualCompletionDate", null);
        serviceMap.put("percentComplete", null);
        setMapElement(serviceMap, "estimatedStartDate", fromDtStart(propertyList));
        setMapElement(serviceMap, "estimatedMilliSeconds", fromDuration(propertyList));
        setMapElement(serviceMap, "lastModifiedDate", fromLastModified(propertyList));
        if ("VTODO".equals(component.getName())) {
            setMapElement(serviceMap, "actualCompletionDate", fromCompleted(propertyList));
            setMapElement(serviceMap, "percentComplete", fromPercentComplete(propertyList));
        } else {
            setMapElement(serviceMap, "estimatedCompletionDate", fromDtEnd(propertyList));
        }
    }

    /** Update work efforts from an incoming iCalendar request.
     * @param is
     * @param context
     * @throws IOException
     * @throws ParserException
     * @throws GenericEntityException
     * @throws GenericServiceException
     */
    public static ResponseProperties storeCalendar(InputStream is, Map<String, Object> context) throws IOException, ParserException, GenericEntityException, GenericServiceException {
        CalendarBuilder builder = new CalendarBuilder();
        Calendar calendar = null;
        try {
            calendar = builder.build(is);
        } finally {
            if (is != null) {
                is.close();
            }
        }
        if (Debug.verboseOn()) {
            Debug.logVerbose("Processing calendar:\r\n" + calendar, module);
        }
        String workEffortId = fromXProperty(calendar.getProperties(), workEffortIdXPropName);
        if (workEffortId == null) {
            workEffortId = (String) context.get("workEffortId");
        }
        if (!workEffortId.equals(context.get("workEffortId"))) {
            Debug.logWarning("Spoof attempt: received calendar workEffortId " + workEffortId +
                    " on URL workEffortId " + context.get("workEffortId"), module);
            return ICalWorker.createForbiddenResponse(null);
        }
        Delegator delegator = (Delegator) context.get("delegator");
        GenericValue publishProperties = EntityQuery.use(delegator).from("WorkEffort").where("workEffortId", workEffortId).queryOne();
        if (!isCalendarPublished(publishProperties)) {
            Debug.logInfo("WorkEffort calendar is not published: " + workEffortId, module);
            return ICalWorker.createNotFoundResponse(null);
        }
        if (context.get("userLogin") == null) {
            return ICalWorker.createNotAuthorizedResponse(null);
        }
        if (!hasPermission(workEffortId, "UPDATE", context)) {
            return ICalWorker.createForbiddenResponse(null);
        }
        boolean hasCreatePermission = hasPermission(workEffortId, "CREATE", context);
        List<GenericValue> workEfforts = getRelatedWorkEfforts(publishProperties, context);
        Set<String> validWorkEfforts = new HashSet<String>();
        if (UtilValidate.isNotEmpty(workEfforts)) {
            // Security issue: make sure only related work efforts get updated
            for (GenericValue workEffort : workEfforts) {
                validWorkEfforts.add(workEffort.getString("workEffortId"));
            }
        }
        List<Component> components = UtilGenerics.checkList(calendar.getComponents(), Component.class);
        ResponseProperties responseProps = null;
        for (Component component : components) {
            if (Component.VEVENT.equals(component.getName()) || Component.VTODO.equals(component.getName())) {
                workEffortId = fromXProperty(component.getProperties(), workEffortIdXPropName);
                if (workEffortId == null) {
                    Property uid = component.getProperty(Uid.UID);
                    if (uid != null) {
                        GenericValue workEffort = EntityQuery.use(delegator).from("WorkEffort").where("universalId", uid.getValue()).queryFirst();
                        if (workEffort != null) {
                            workEffortId = workEffort.getString("workEffortId");
                        }
                    }
                }
                if (workEffortId != null) {
                    if (validWorkEfforts.contains(workEffortId)) {
                        replaceProperty(component.getProperties(), toXProperty(workEffortIdXPropName, workEffortId));
                        responseProps = storeWorkEffort(component, context);
                    } else {
                        Debug.logWarning("Spoof attempt: unrelated workEffortId " + workEffortId +
                                " on URL workEffortId " + context.get("workEffortId"), module);
                        responseProps = ICalWorker.createForbiddenResponse(null);
                    }
                } else if (hasCreatePermission) {
                    responseProps = createWorkEffort(component, context);
                }
                if (responseProps != null) {
                    return responseProps;
                }
            }
        }
        Map<String, ? extends Object> serviceMap = UtilMisc.toMap("workEffortId", context.get("workEffortId"), "icalData", calendar.toString());
        GenericValue iCalData = publishProperties.getRelatedOne("WorkEffortIcalData", false);
        Map<String, Object> serviceResult = null;
        if (iCalData == null) {
            serviceResult = invokeService("createWorkEffortICalData", serviceMap, context);
        } else {
            serviceResult = invokeService("updateWorkEffortICalData", serviceMap, context);
        }
        if (ServiceUtil.isError(serviceResult)) {
            return ICalWorker.createPartialContentResponse(ServiceUtil.getErrorMessage(serviceResult));
        }
        return ICalWorker.createOkResponse(null);
    }

    protected static ResponseProperties storePartyAssignments(String workEffortId, Component component, Map<String, Object> context) {
        ResponseProperties responseProps = null;
        Map<String, Object> serviceMap = new HashMap<String, Object>();
        List<Property> partyList = new LinkedList<Property>();
        partyList.addAll(UtilGenerics.checkList(component.getProperties("ATTENDEE"), Property.class));
        partyList.addAll(UtilGenerics.checkList(component.getProperties("CONTACT"), Property.class));
        partyList.addAll(UtilGenerics.checkList(component.getProperties("ORGANIZER"), Property.class));
        for (Property property : partyList) {
            String partyId = fromXParameter(property.getParameters(), partyIdXParamName);
            if (partyId == null) {
                serviceMap.clear();
                String address = property.getValue();
                if (address.toUpperCase().startsWith("MAILTO:")) {
                    address = address.substring(7);
                }
                serviceMap.put("address", address);
                Map<String, Object> result = invokeService("findPartyFromEmailAddress", serviceMap, context);
                partyId = (String) result.get("partyId");
                if (partyId == null) {
                    continue;
                }
                replaceParameter(property.getParameters(), toXParameter(partyIdXParamName, partyId));
            }
            serviceMap.clear();
            serviceMap.put("workEffortId", workEffortId);
            serviceMap.put("partyId", partyId);
            serviceMap.put("roleTypeId", fromRoleMap.get(property.getName()));
            Delegator delegator = (Delegator) context.get("delegator");
            List<GenericValue> assignments = null;
            try {
                assignments = EntityQuery.use(delegator).from("WorkEffortPartyAssignment").where(serviceMap).filterByDate().queryList();
                if (assignments.size() == 0) {
                    serviceMap.put("statusId", "PRTYASGN_OFFERED");
                    serviceMap.put("fromDate", new Timestamp(System.currentTimeMillis()));
                    invokeService("assignPartyToWorkEffort", serviceMap, context);
                }
            } catch (GenericEntityException e) {
                responseProps = ICalWorker.createPartialContentResponse(e.getMessage());
                break;
            }
        }
        return responseProps;
    }

    protected static ResponseProperties storeWorkEffort(Component component, Map<String, Object> context) throws GenericEntityException, GenericServiceException {
        PropertyList propertyList = component.getProperties();
        String workEffortId = fromXProperty(propertyList, workEffortIdXPropName);
        Delegator delegator = (Delegator) context.get("delegator");
        GenericValue workEffort = EntityQuery.use(delegator).from("WorkEffort").where("workEffortId", workEffortId).queryOne();
        if (workEffort == null) {
            return ICalWorker.createNotFoundResponse(null);
        }
        if (!hasPermission(workEffortId, "UPDATE", context)) {
            return null;
        }
        Map<String, Object> serviceMap = new HashMap<String, Object>();
        serviceMap.put("workEffortId", workEffortId);
        setWorkEffortServiceMap(component, serviceMap);
        invokeService("updateWorkEffort", serviceMap, context);
        return storePartyAssignments(workEffortId, component, context);
    }

    protected static ResponseProperties toCalendarComponent(ComponentList components, GenericValue workEffort, Map<String, Object> context) throws GenericEntityException {
        Delegator delegator = workEffort.getDelegator();
        String workEffortId = workEffort.getString("workEffortId");
        String workEffortUid = workEffort.getString("universalId");
        String workEffortTypeId = workEffort.getString("workEffortTypeId");
        GenericValue typeValue = EntityQuery.use(delegator).from("WorkEffortType").where("workEffortTypeId", workEffortTypeId).cache().queryOne();
        boolean isTask = false;
        boolean newComponent = true;
        ComponentList resultList = null;
        ComponentList alarms = null;
        Component result = null;
        if ("TASK".equals(workEffortTypeId) || (typeValue != null && "TASK".equals(typeValue.get("parentTypeId")))) {
            isTask = true;
            resultList = components.getComponents("VTODO");
        } else if ("EVENT".equals(workEffortTypeId) || (typeValue != null && "EVENT".equals(typeValue.get("parentTypeId")))) {
            resultList = components.getComponents("VEVENT");
        } else {
            return null;
        }
        Iterator<Component> i = UtilGenerics.cast(resultList.iterator());
        while (i.hasNext()) {
            result = i.next();
            Property xProperty = result.getProperty(workEffortIdXPropName);
            if (xProperty != null && workEffortId.equals(xProperty.getValue())) {
                newComponent = false;
                break;
            }
            Property uid = result.getProperty(Uid.UID);
            if (uid != null && uid.getValue().equals(workEffortUid)) {
                newComponent = false;
                break;
            }
        }
        if (isTask) {
            VToDo toDo = null;
            if (newComponent) {
                toDo = new VToDo();
                result = toDo;
            } else {
                toDo = (VToDo) result;
            }
            alarms = toDo.getAlarms();
        } else {
            VEvent event = null;
            if (newComponent) {
                event = new VEvent();
                result = event;
            } else {
                event = (VEvent) result;
            }
            alarms = event.getAlarms();
        }
        if (newComponent) {
            components.add(result);
        }
        PropertyList componentProps = result.getProperties();
        loadWorkEffort(componentProps, workEffort);
        if (isTask) {
            replaceProperty(componentProps, toCompleted(workEffort.getTimestamp("actualCompletionDate")));
            replaceProperty(componentProps, toPercentComplete(workEffort.getLong("percentComplete")));
        } else {
            replaceProperty(componentProps, toDtEnd(workEffort.getTimestamp("estimatedCompletionDate")));
        }
        if (workEffort.get("estimatedCompletionDate") == null) {
            replaceProperty(componentProps, toDuration(workEffort.getDouble("estimatedMilliSeconds")));
        }
        List<GenericValue> relatedParties = EntityQuery.use(delegator).from("WorkEffortPartyAssignView").where("workEffortId", workEffortId).cache(true).filterByDate().queryList();
        if (relatedParties.size() > 0) {
            loadRelatedParties(relatedParties, componentProps, context);
        }
        if (newComponent) {
            if (UtilValidate.isNotEmpty(workEffort.getString("tempExprId"))) {
                TemporalExpression tempExpr = TemporalExpressionWorker.getTemporalExpression(delegator, workEffort.getString("tempExprId"));
                if (tempExpr != null) {
                    try {
                        ICalRecurConverter.convert(tempExpr, componentProps);
                    } catch (Exception e) {
                        replaceProperty(componentProps, new Description("Error while converting recurrence: " + e));
                    }
                }
            }
            getAlarms(workEffort, alarms);
        }
        if (Debug.verboseOn()) {
            try {
                result.validate(true);
                Debug.logVerbose("iCalendar component passes validation", module);
            } catch (ValidationException e) {
                Debug.logVerbose(e, "iCalendar component fails validation: ", module);
            }
        }
        return null;
    }

    protected static Clazz toClazz(String javaObj) {
        if (javaObj == null) {
            return null;
        }
        return new Clazz(javaObj.replace("WES_", ""));
    }

    protected static Completed toCompleted(Timestamp javaObj) {
        if (javaObj == null) {
            return null;
        }
        return new Completed(new DateTime(javaObj));
    }

    protected static Created toCreated(Timestamp javaObj) {
        if (javaObj == null) {
            return null;
        }
        return new Created(new DateTime(javaObj));
    }

    protected static Description toDescription(String javaObj) {
        if (javaObj == null) {
            return null;
        }
        return new Description(javaObj);
    }

    protected static DtEnd toDtEnd(Timestamp javaObj) {
        if (javaObj == null) {
            return null;
        }
        return new DtEnd(new DateTime(javaObj));
    }

    protected static DtStart toDtStart(Timestamp javaObj) {
        if (javaObj == null) {
            return null;
        }
        return new DtStart(new DateTime(javaObj));
    }

    protected static Duration toDuration(Double javaObj) {
        if (javaObj == null) {
            return null;
        }
        TimeDuration duration = TimeDuration.fromNumber(javaObj);
        return new Duration(new Dur(duration.days(), duration.hours(), duration.minutes(), duration.seconds()));
    }

    protected static LastModified toLastModified(Timestamp javaObj) {
        if (javaObj == null) {
            return null;
        }
        return new LastModified(new DateTime(javaObj));
    }

    protected static Location toLocation(String javaObj) {
        if (javaObj == null) {
            return null;
        }
        return new Location(javaObj);
    }

    protected static PartStat toParticipationStatus(String statusId) {
        if (statusId == null) {
            return null;
        }
        return toPartStatusMap.get(statusId);
    }

    protected static PercentComplete toPercentComplete(Long javaObj) {
        if (javaObj == null) {
            return null;
        }
        return new PercentComplete(javaObj.intValue());
    }

    protected static Priority toPriority(Long javaObj) {
        if (javaObj == null) {
            return null;
        }
        return new Priority(javaObj.intValue());
    }

    protected static Status toStatus(String javaObj) {
        if (javaObj == null) {
            return null;
        }
        return toStatusMap.get(javaObj);
    }

    protected static Summary toSummary(String javaObj) {
        if (javaObj == null) {
            return null;
        }
        return new Summary(javaObj);
    }

    protected static Uid toUid(String javaObj) {
        if (javaObj == null) {
            return null;
        }
        return new Uid(uidPrefix.concat(javaObj));
    }

    protected static XParameter toXParameter(String name, String value) {
        if (name == null || value == null) {
            return null;
        }
        return new XParameter(name, value);
    }

    protected static XProperty toXProperty(String name, String value) {
        if (name == null || value == null) {
            return null;
        }
        return new XProperty(name, value);
    }
}
