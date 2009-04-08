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
  <div class="screenlet-title-bar">
    <ul>
      <li class="h3">${uiLabelMap.CommonWeek} ${start?date?string("w")}</li>
      <li><a href="<@ofbizUrl>week?start=${next.time?string("#")}<#if eventsParam?has_content>&${eventsParam}</#if>${addlParam?if_exists}</@ofbizUrl>">${uiLabelMap.WorkEffortNextWeek}</a></li>
      <li><a href="<@ofbizUrl>week?start=${nowTimestamp.time?string("#")}<#if eventsParam?has_content>&${eventsParam}</#if>${addlParam?if_exists}</@ofbizUrl>">${uiLabelMap.WorkEffortThisWeek}</a></li>
      <li><a href="<@ofbizUrl>week?start=${prev.time?string("#")}<#if eventsParam?has_content>&${eventsParam}</#if>${addlParam?if_exists}</@ofbizUrl>">${uiLabelMap.WorkEffortPreviousWeek}</a></li>
    </ul>
    <br class="clear"/>
  </div>
<#if periods?has_content>
  <#if (maxConcurrentEntries < 2)>
    <#assign entryWidth = 100>
  <#else>
    <#assign entryWidth = (100 / (maxConcurrentEntries))>
  </#if>
<table cellspacing="0" class="basic-table calendar">
  <tr class="header-row">
    <td>${uiLabelMap.CommonTime}</td>
    <td colspan=${maxConcurrentEntries}>${uiLabelMap.WorkEffortCalendarEntries}</td>
  </tr>
  <#list periods as period>
    <#assign currentPeriod = false/>
    <#if (nowTimestamp >= period.start) && (nowTimestamp <= period.end)><#assign currentPeriod = true/></#if>
  <tr<#if currentPeriod> class="current-period"<#else><#if (period.calendarEntries?size > 0)> class="active-period"</#if></#if>>
    <td class="centered" width="1%">
      <a href="<@ofbizUrl>day?start=${period.start.time?string("#")}<#if eventsParam?has_content>&${eventsParam}</#if>${addlParam?if_exists}</@ofbizUrl>">${period.start?date?string("EEEE")?cap_first}&nbsp;${period.start?date?string.short}</a><br/>
      <a href="<@ofbizUrl>EditWorkEffort?workEffortTypeId=EVENT&currentStatusId=CAL_TENTATIVE&estimatedStartDate=${period.start?string("yyyy-MM-dd HH:mm:ss")}&estimatedCompletionDate=${period.end?string("yyyy-MM-dd HH:mm:ss")}${addlParam?if_exists}</@ofbizUrl>">${uiLabelMap.CommonAddNew}</a>
    </td>
    <#list period.calendarEntries as calEntry>
    <#if calEntry.startOfPeriod>
    <td<#if (calEntry.periodSpan > 1)> rowspan="${calEntry.periodSpan}"</#if> width="${entryWidth?string("#")}%">
    <#if (calEntry.workEffort.estimatedStartDate.compareTo(start)  <= 0 && calEntry.workEffort.estimatedCompletionDate.compareTo(next) >= 0)>
      ${uiLabelMap.CommonAllWeek}
    <#elseif (calEntry.workEffort.estimatedStartDate.compareTo(period.start)  = 0 && calEntry.workEffort.estimatedCompletionDate.compareTo(period.end) = 0)>
      ${uiLabelMap.CommonAllDay}
    <#elseif calEntry.workEffort.estimatedStartDate.before(start)>
      ${uiLabelMap.CommonUntil} ${calEntry.workEffort.estimatedCompletionDate?datetime?string.short}
    <#elseif calEntry.workEffort.estimatedCompletionDate.after(next)>
      ${uiLabelMap.CommonFrom} ${calEntry.workEffort.estimatedStartDate?time?string.short}
    <#else>
      ${calEntry.workEffort.estimatedStartDate?time?string.short}-${calEntry.workEffort.estimatedCompletionDate?time?string.short}
    </#if>
      <br/><a href="<@ofbizUrl>WorkEffortSummary?workEffortId=${calEntry.workEffort.workEffortId}${addlParam?if_exists}</@ofbizUrl>" class="event">${calEntry.workEffort.workEffortName?default("Undefined")}</a>&nbsp;</td>
    </#if>
    </#list>
    <#if (period.calendarEntries?size < maxConcurrentEntries)>
      <#assign emptySlots = (maxConcurrentEntries - period.calendarEntries?size)>
        <td<#if (emptySlots > 1)> colspan="${emptySlots}"</#if>>&nbsp;</td>
    </#if>
    <#if maxConcurrentEntries = 0>
      <td width="${entryWidth?string("#")}%">&nbsp;</td>
    </#if>
  </tr>
  </#list>
</table>
<#else>
  <div class="screenlet-body">${uiLabelMap.WorkEffortFailedCalendarEntries}!</div>
</#if>
