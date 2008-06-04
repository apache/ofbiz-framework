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
import org.ofbiz.security.*;
import org.ofbiz.entity.*;
import org.ofbiz.entity.condition.*;
import org.ofbiz.base.util.*;
import org.ofbiz.webapp.pseudotag.*;
import java.sql.Timestamp;
import java.util.Calendar;
import java.text.NumberFormat;

fixedAssetId = parameters.fixedAssetId;
fixedAsset = delegator.findByPrimaryKeyCache("FixedAsset", [fixedAssetId: fixedAssetId]);

startMonth = parameters.month; //optional command to change the month
currentMonth = session.getAttribute("currentMonth");    // the month displayed the last time.

now = null;
if (!startMonth || !currentMonth)    // a fresh start
    now = UtilDateTime.getMonthStart(UtilDateTime.nowTimestamp());
else if (startMonth.equals("1") && currentMonth)
    now = UtilDateTime.getMonthStart(UtilDateTime.getMonthStart(currentMonth, 35));
else if (startMonth.equals("-1") && currentMonth)
    now = UtilDateTime.getMonthStart(UtilDateTime.getMonthStart(currentMonth, -15));
else if (startMonth.equals("3") && currentMonth)
    now = UtilDateTime.getMonthStart(UtilDateTime.getMonthStart(currentMonth, 100));
else if (startMonth.equals("-3") && currentMonth)
    now = UtilDateTime.getMonthStart(UtilDateTime.getMonthStart(currentMonth, -75));
else
    now = UtilDateTime.getMonthStart(UtilDateTime.nowTimestamp());

currentMonth = now;
nextMonth = UtilDateTime.getMonthStart(UtilDateTime.getMonthStart(now, 35));

condition = [];
condition.add(EntityCondition.makeCondition("calendarId", EntityOperator.EQUALS, fixedAsset.getString("calendarId")));
condition.add(EntityCondition.makeCondition("exceptionDateStartTime",EntityOperator.GREATER_THAN, now));
condition.add(EntityCondition.makeCondition("exceptionDateStartTime",EntityOperator.LESS_THAN, nextMonth));
ecl = EntityCondition.makeCondition(condition, EntityOperator.AND);
allDates = delegator.findList("TechDataCalendarExcDay", ecl, null, null, null, false);
dbInt = allDates.iterator();
dbValid = false;    // flag to see if the current dbInt is ok
excDayRecord = null;
if (dbInt.hasNext())    {
    excDayRecord = dbInt.next();
    dbValid = true;
}

calendarStartWeek = UtilDateTime.getWeekStart(now);
calendarEndDay = UtilDateTime.getWeekStart(nextMonth);

currentWeek = calendarStartWeek;
weeks = [];
while ( currentWeek.compareTo(calendarEndDay) <= 0 ){
    days = [week : UtilDateTime.weekNumber(currentWeek)];
    
    for (int day = 1; day < 8 ; day++)    {
        String extraText = "";
        available = "N/A";
        if (dbValid == true && UtilDateTime.getDayStart(currentWeek,day).compareTo(excDayRecord.getTimestamp("exceptionDateStartTime")) == 0) {
            if (fixedAsset.productionCapacity)
                available = fixedAsset.productionCapacity + "*"; // default value
            if (excDayRecord.exceptionCapacity)
                available = excDayRecord.exceptionCapacity;
            extraText = "Avail.: " + available + "<br/>Allocated: " + excDayRecord.usedCapacity;
            if (dbInt.hasNext()) {
                excDayRecord = dbInt.next();
                dbValid = true;
            } else {
                dbValid = false;
            }
        }
        days.put(UtilDateTime.days[day-1], UtilDateTime.getDayStart(currentWeek,day).toString().substring(8,10));
        days.put(UtilDateTime.days[day-1]+"Data", extraText);
        
    }
    weeks.add(days);
    currentWeek = UtilDateTime.getWeekStart(currentWeek,7);
}
monthNr = NumberFormat.getNumberInstance().parse(now.toString().substring(5,7)).intValue();
context.month = UtilDateTime.months[monthNr-1];
context.year = now.toString().substring(0,4);
context.weeks = weeks;
context.now = now;
context.nextMonth = nextMonth;
session.setAttribute("currentMonth",currentMonth);
