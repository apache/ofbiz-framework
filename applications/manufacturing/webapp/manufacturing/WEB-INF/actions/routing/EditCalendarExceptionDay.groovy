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
import org.ofbiz.base.util.*;
import org.ofbiz.entity.*;
import org.ofbiz.widget.html.*;

if (security.hasEntityPermission("MANUFACTURING", "_VIEW", session)) {
    context.hasPermission = Boolean.TRUE;
} else {
    context.hasPermission = Boolean.FALSE;
}
techDataCalendar = [:];
calendarExceptionDays = [];

calendarId = parameters.calendarId ?: request.getAttribute("calendarId");
if (calendarId) {
    techDataCalendar = delegator.findByPrimaryKey("TechDataCalendar", [calendarId : calendarId]);
}
if (techDataCalendar) {
    calendarExceptionDays = techDataCalendar.getRelated("TechDataCalendarExcDay");
}
HtmlFormWrapper listCalendarExceptionDayWrapper = new HtmlFormWrapper("component://manufacturing/widget/manufacturing/CalendarForms.xml", "ListCalendarExceptionDay", request, response);
listCalendarExceptionDayWrapper.putInContext("calendarExceptionDays", calendarExceptionDays);

HtmlFormWrapper addCalendarExceptionDayWrapper = new HtmlFormWrapper("component://manufacturing/widget/manufacturing/CalendarForms.xml", "AddCalendarExceptionDay", request, response);
addCalendarExceptionDayWrapper.putInContext("techDataCalendar", techDataCalendar);

context.techDataCalendar = techDataCalendar;
context.listCalendarExceptionDayWrapper = listCalendarExceptionDayWrapper;
context.addCalendarExceptionDayWrapper = addCalendarExceptionDayWrapper;

exceptionDateStartTime = parameters.exceptionDateStartTime ?: request.getAttribute("exceptionDateStartTime");
exceptionDateStartTime = ObjectType.simpleTypeConvert(exceptionDateStartTime, "Timestamp", null, null);

if (exceptionDateStartTime) {
    calendarExceptionDay = delegator.findByPrimaryKey("TechDataCalendarExcDay", [calendarId : calendarId , exceptionDateStartTime : exceptionDateStartTime]);
    if (calendarExceptionDay) {
        HtmlFormWrapper updateCalendarExceptionDayWrapper = new HtmlFormWrapper("component://manufacturing/widget/manufacturing/CalendarForms.xml", "UpdateCalendarExceptionDay", request, response);
        updateCalendarExceptionDayWrapper.putInContext("calendarExceptionDay", calendarExceptionDay);
        context.calendarExceptionDay = calendarExceptionDay;
        context.updateCalendarExceptionDayWrapper =  updateCalendarExceptionDayWrapper;
    }
}
