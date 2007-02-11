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

security = request.getAttribute("security");
delegator = request.getAttribute("delegator");

if(security.hasEntityPermission("MANUFACTURING", "_VIEW", session)) {
    context.put("hasPermission", Boolean.TRUE);
} else {
    context.put("hasPermission", Boolean.FALSE);
}
GenericValue techDataCalendar = null;
List calendarExceptionDays = null;

String calendarId = request.getParameter("calendarId");
if (calendarId == null) {
    calendarId = request.getAttribute("calendarId");
}
if (calendarId != null) {
    techDataCalendar = delegator.findByPrimaryKey("TechDataCalendar", UtilMisc.toMap("calendarId", calendarId));
}
if (techDataCalendar != null) {
    calendarExceptionDays = techDataCalendar.getRelated("TechDataCalendarExcDay");
}
HtmlFormWrapper listCalendarExceptionDayWrapper = new HtmlFormWrapper("component://manufacturing/webapp/manufacturing/routing/CalendarForms.xml", "ListCalendarExceptionDay", request, response);
listCalendarExceptionDayWrapper.putInContext("calendarExceptionDays", calendarExceptionDays);

HtmlFormWrapper addCalendarExceptionDayWrapper = new HtmlFormWrapper("component://manufacturing/webapp/manufacturing/routing/CalendarForms.xml", "AddCalendarExceptionDay", request, response);
addCalendarExceptionDayWrapper.putInContext("techDataCalendar", techDataCalendar);

context.put("techDataCalendar", techDataCalendar);
context.put("listCalendarExceptionDayWrapper", listCalendarExceptionDayWrapper);
context.put("addCalendarExceptionDayWrapper", addCalendarExceptionDayWrapper);

String exceptionDateStartTime = request.getParameter("exceptionDateStartTime");
if (exceptionDateStartTime == null)
    exceptionDateStartTime = request.getAttribute("exceptionDateStartTime");
if (exceptionDateStartTime != null) {
    calendarExceptionDay = delegator.findByPrimaryKey("TechDataCalendarExcDay", UtilMisc.toMap("calendarId", calendarId,"exceptionDateStartTime", exceptionDateStartTime));
    if (calendarExceptionDay != null) {
        HtmlFormWrapper updateCalendarExceptionDayWrapper = new HtmlFormWrapper("component://manufacturing/webapp/manufacturing/routing/CalendarForms.xml", "UpdateCalendarExceptionDay", request, response);
        updateCalendarExceptionDayWrapper.putInContext("calendarExceptionDay", calendarExceptionDay);
        context.put("calendarExceptionDay", calendarExceptionDay);
        context.put("updateCalendarExceptionDayWrapper", updateCalendarExceptionDayWrapper);
    }
}
