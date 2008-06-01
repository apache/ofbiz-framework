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
List calendarExceptionWeeks = null;

String calendarId = request.getParameter("calendarId");
if (calendarId == null) {
    calendarId = request.getAttribute("calendarId");
}
if (calendarId != null) {
    techDataCalendar = delegator.findByPrimaryKey("TechDataCalendar", UtilMisc.toMap("calendarId", calendarId));
}
if (techDataCalendar != null) {
    calendarExceptionWeeks = techDataCalendar.getRelated("TechDataCalendarExcWeek");
}
List calendarExceptionWeeksDatas = new LinkedList();
Iterator calendarExceptionWeeksIter = calendarExceptionWeeks.iterator();
while (calendarExceptionWeeksIter.hasNext()) {
    GenericValue calendarExceptionWeek = (GenericValue) calendarExceptionWeeksIter.next();
    GenericValue calendarWeek = calendarExceptionWeek.getRelatedOne("TechDataCalendarWeek");
    calendarExceptionWeeksDatas.add(UtilMisc.toMap("calendarExceptionWeek", calendarExceptionWeek, "calendarWeek", calendarWeek));
}

HtmlFormWrapper listCalendarExceptionWeekWrapper = new HtmlFormWrapper("component://manufacturing/webapp/manufacturing/routing/CalendarForms.xml", "ListCalendarExceptionWeek", request, response);
listCalendarExceptionWeekWrapper.putInContext("calendarExceptionWeeksDatas", calendarExceptionWeeksDatas);

HtmlFormWrapper addCalendarExceptionWeekWrapper = new HtmlFormWrapper("component://manufacturing/webapp/manufacturing/routing/CalendarForms.xml", "AddCalendarExceptionWeek", request, response);
addCalendarExceptionWeekWrapper.putInContext("techDatacalendar", techDataCalendar);

context.put("techDataCalendar", techDataCalendar);
context.put("listCalendarExceptionWeekWrapper", listCalendarExceptionWeekWrapper);
context.put("addCalendarExceptionWeekWrapper", addCalendarExceptionWeekWrapper);

String exceptionDateStart = request.getParameter("exceptionDateStart");
if (exceptionDateStart == null)
    exceptionDateStart = request.getAttribute("exceptionDateStart");
if (exceptionDateStart != null) {
    calendarExceptionWeek = delegator.findByPrimaryKey("TechDataCalendarExcWeek", UtilMisc.toMap("calendarId", calendarId,"exceptionDateStart", exceptionDateStart));
    if (calendarExceptionWeek != null) {
        HtmlFormWrapper updateCalendarExceptionWeekWrapper = new HtmlFormWrapper("component://manufacturing/webapp/manufacturing/routing/CalendarForms.xml", "UpdateCalendarExceptionWeek", request, response);
        updateCalendarExceptionWeekWrapper.putInContext("calendarExceptionWeek", calendarExceptionWeek);
        context.put("calendarExceptionWeek", calendarExceptionWeek);
        context.put("updateCalendarExceptionWeekWrapper", updateCalendarExceptionWeekWrapper);
    }
}
