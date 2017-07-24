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

import java.sql.Timestamp
import org.apache.ofbiz.base.util.UtilDateTime
import org.apache.ofbiz.base.util.UtilMisc
import org.apache.ofbiz.base.util.UtilValidate
import org.apache.ofbiz.service.ModelService

String startParam = parameters.startTime
Timestamp start = null
if (UtilValidate.isNotEmpty(startParam)) {
    start = new Timestamp(Long.parseLong(startParam))
}
if (start == null) {
    start = UtilDateTime.getWeekStart(nowTimestamp, timeZone, locale)
} else {
    start = UtilDateTime.getWeekStart(start, timeZone, locale)
}
Timestamp prev = UtilDateTime.getDayStart(start, -7, timeZone, locale)
context.prevMillis = new Long(prev.getTime()).toString()
Timestamp next = UtilDateTime.getDayStart(start, 7, timeZone, locale)
context.nextMillis = new Long(next.getTime()).toString()
Timestamp end = UtilDateTime.getDayStart(start, 6, timeZone, locale)
Map serviceCtx = dispatcher.getDispatchContext().makeValidContext("getWorkEffortEventsByPeriod", ModelService.IN_PARAM, parameters)
serviceCtx.putAll(UtilMisc.toMap("userLogin", userLogin, "start", start, "numPeriods", 7, "periodType", Calendar.DATE, "locale", locale, "timeZone", timeZone))
if (context.entityExprList) {
    serviceCtx.entityExprList = entityExprList
}
Map result = runService('getWorkEffortEventsByPeriod',serviceCtx)
context.put("periods",result.get("periods"))
context.put("maxConcurrentEntries",result.get("maxConcurrentEntries"))
context.put("start",start)
context.put("end",end)
context.put("prev",prev)
context.put("next",next)
