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
import org.ofbiz.base.util.*;
import java.sql.*;
import java.util.Calendar;

fromDateStr = parameters.fromDate;
cal = Calendar.getInstance();
cal.setTime(Date.valueOf(fromDateStr));
int week = cal.get(Calendar.WEEK_OF_YEAR);
int month = cal.get(Calendar.MONTH) + 1;
int year = cal.get(Calendar.YEAR);

birtParameters = [:];
try {
    birtParameters.reportBy = parameters.reportBy;
    birtParameters.fromDate = (Date.valueOf(fromDateStr))-2;
    birtParameters.thruDate = Date.valueOf(fromDateStr);
    birtParameters.lastDate = (Date.valueOf(fromDateStr))-7;
    birtParameters.thruWeek = week;
    birtParameters.thruMonth = month;
    birtParameters.thisYear = year;
} catch (e) {
    Debug.logError(e, "");
}

request.setAttribute("birtParameters", birtParameters);

return "success";
