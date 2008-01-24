<#--
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
-->

<#assign selected = tabButtonItem?default("void")>

<div class="button-bar tab-bar">
    <ul>
        <li<#if selected == "Calendar"> class="selected"</#if>><a href="<@ofbizUrl>FindCalendar</@ofbizUrl>">${uiLabelMap.ManufacturingCalendars}</a></li>
        <li<#if selected == "CalendarWeek"> class="selected"</#if>><a href="<@ofbizUrl>ListCalendarWeek</@ofbizUrl>">${uiLabelMap.ManufacturingCalendarWeeks}</a></li>
    </ul>
    <br class="clear"/>
</div>
<#if techDataCalendar?has_content>
    <br/>
    <div class="button-bar tab-bar">
        <ul>
            <li<#if selected == "calendar"> class="selected"</#if>><a href="<@ofbizUrl>EditCalendar?calendarId=${techDataCalendar.calendarId}</@ofbizUrl>">${uiLabelMap.CommonEdit}</a></li>
            <li<#if selected == "calendarExceptionDay"> class="selected"</#if>><a href="<@ofbizUrl>EditCalendarExceptionDay?calendarId=${techDataCalendar.calendarId}</@ofbizUrl>">${uiLabelMap.ManufacturingCalendarExceptionDate}</a></li>
            <li<#if selected == "calendarExceptionWeek"> class="selected"</#if>><a href="<@ofbizUrl>EditCalendarExceptionWeek?calendarId=${techDataCalendar.calendarId}</@ofbizUrl>">${uiLabelMap.ManufacturingCalendarExceptionWeek}</a></li>
        </ul>
        <br class="clear"/>
    </div>
</#if>