<#--

Copyright 2001-2006 The Apache Software Foundation

Licensed under the Apache License, Version 2.0 (the "License"); you may not
use this file except in compliance with the License. You may obtain a copy of
the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
License for the specific language governing permissions and limitations
under the License.
-->

<#assign unselectedClassName = "tabButton">
<#assign selectedClassMap = {page.tabButtonItem?default("void") : "tabButtonSelected"}>

<#if techDataCalendar?has_content>
    <div class='tabContainer'>
        <a href="<@ofbizUrl>EditCalendar?calendarId=${techDataCalendar.calendarId}</@ofbizUrl>" class="${selectedClassMap.calendar?default(unselectedClassName)}">${uiLabelMap.ManufacturingCalendar}</a>
        <a href="<@ofbizUrl>EditCalendarExceptionDay?calendarId=${techDataCalendar.calendarId}</@ofbizUrl>" class="${selectedClassMap.calendarExceptionDay?default(unselectedClassName)}">${uiLabelMap.ManufacturingCalendarExceptionDate}</a>
        <a href="<@ofbizUrl>EditCalendarExceptionWeek?calendarId=${techDataCalendar.calendarId}</@ofbizUrl>" class="${selectedClassMap.calendarExceptionWeek?default(unselectedClassName)}">${uiLabelMap.ManufacturingCalendarExceptionWeek}</a>
        <a href="<@ofbizUrl>ListCalendarWeek</@ofbizUrl>" class="${selectedClassMap.CalendarWeek?default(unselectedClassName)}">${uiLabelMap.ManufacturingCalendarWeek}</a>
    </div>
</#if>
