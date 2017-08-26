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


import org.apache.ofbiz.base.util.ObjectType

if (security.hasEntityPermission("MANUFACTURING", "_VIEW", session)) {
    context.hasPermission = Boolean.TRUE
} else {
    context.hasPermission = Boolean.FALSE
}
techDataCalendar = [:]
calendarExceptionWeeks = []

calendarId = parameters.calendarId ?: request.getAttribute("calendarId")
if (calendarId) {
    techDataCalendar = from("TechDataCalendar").where("calendarId", calendarId).queryOne()
    context.techDataCalendar = techDataCalendar
}
if (techDataCalendar) {
    calendarExceptionWeeks = techDataCalendar.getRelated("TechDataCalendarExcWeek", null, null, false)
}
calendarExceptionWeeksDatas = []
calendarExceptionWeeks.each { calendarExceptionWeek ->
    calendarWeek = calendarExceptionWeek.getRelatedOne("TechDataCalendarWeek", false)
    calendarExceptionWeeksDatas.add([calendarExceptionWeek : calendarExceptionWeek , calendarWeek : calendarWeek])
    context.calendarExceptionWeeksDatas = calendarExceptionWeeksDatas
}

exceptionDateStart = parameters.exceptionDateStart ?: request.getAttribute("exceptionDateStart")
exceptionDateStart = ObjectType.simpleTypeConvert(exceptionDateStart, "java.sql.Date", null, null)

if (exceptionDateStart) {
    calendarExceptionWeek = from("TechDataCalendarExcWeek").where("calendarId", calendarId , "exceptionDateStart", exceptionDateStart).queryOne()
    if (calendarExceptionWeek) {
        context.calendarExceptionWeek = calendarExceptionWeek
    }
}