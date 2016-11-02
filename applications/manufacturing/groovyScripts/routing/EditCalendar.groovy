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

import org.apache.ofbiz.base.util.UtilHttp

requestParams = UtilHttp.getParameterMap(request)
calendarId = requestParams.get("calendarId") ?: request.getAttribute("calendarId")
if (calendarId != null) {
    techDataCalendar = from("TechDataCalendar").where("calendarId", calendarId).queryOne()
    context.techDataCalendar = techDataCalendar
}

tryEntity = true
errorMessage = request.getAttribute("_ERROR_MESSAGE_")
if (errorMessage) {
    tryEntity = false
}

calendarData = context.techDataCalendar
if (!tryEntity) {
    calendarData = requestParams ?: [:]
}
if (!calendarData) {
    calendarData = [:]
}
context.calendarData = calendarData

allCalendarWeek = from("TechDataCalendarWeek").queryList()
context.calendarWeeks = allCalendarWeek
