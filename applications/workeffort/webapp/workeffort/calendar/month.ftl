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
<style type="text/css">
.calendar tr td {
height: 8em;
width: 10em;
vertical-align: top;
padding: 0.5em;
}
.calendar .header-row td {
height: auto;
}
</style>

  <div class="screenlet-title-bar">
    <ul>
      <li class="h3">${start?date?string("MMMM yyyy")?cap_first}</li>
      <li><a href='<@ofbizUrl>month?start=${next.time?string("#")}<#if eventsParam?has_content>&${eventsParam}</#if>${addlParam?if_exists}</@ofbizUrl>'>${uiLabelMap.WorkEffortNextMonth}</a></li>
      <li><a href='<@ofbizUrl>month?start=${nowTimestamp.time?string("#")}<#if eventsParam?has_content>&${eventsParam}</#if>${addlParam?if_exists}</@ofbizUrl>'>${uiLabelMap.WorkEffortThisMonth}</a></li>
      <li><a href='<@ofbizUrl>month?start=${prev.time?string("#")}<#if eventsParam?has_content>&${eventsParam}</#if>${addlParam?if_exists}</@ofbizUrl>'>${uiLabelMap.WorkEffortPreviousMonth}</a></li>
    </ul>
    <br class="clear"/>
  </div>
<#if periods?has_content>
<table cellspacing="0" class="basic-table calendar">
  <tr class="header-row">
    <td width="1%">&nbsp;</td>
    <#list periods as day>
      <td>${day.start?date?string("EEEE")?cap_first}</td>
      <#if (day_index > 5)><#break></#if>
    </#list>
  </tr>
  <#list periods as period>
    <#assign currentPeriod = false/>
    <#if (nowTimestamp >= period.start) && (nowTimestamp <= period.end)><#assign currentPeriod = true/></#if>
    <#assign indexMod7 = period_index % 7>
    <#if indexMod7 = 0>
      <tr>
        <td class="label">
          <a href='<@ofbizUrl>week?start=${period.start.time?string("#")}<#if eventsParam?has_content>&${eventsParam}</#if>${addlParam?if_exists}</@ofbizUrl>'>${uiLabelMap.CommonWeek} ${period.start?date?string("w")}</a>
        </td>
    </#if>
    <td<#if currentPeriod> class="current-period"<#else><#if (period.calendarEntries?size > 0)> class="active-period"</#if></#if>>
      <span class="h1"><a href='<@ofbizUrl>day?start=${period.start.time?string("#")}<#if eventsParam?has_content>&${eventsParam}</#if>${addlParam?if_exists}</@ofbizUrl>'>${period.start?date?string("d")?cap_first}</a></span>
      <a class="add-new" href='<@ofbizUrl>EditWorkEffort?workEffortTypeId=EVENT&currentStatusId=CAL_TENTATIVE&estimatedStartDate=${period.start?string("yyyy-MM-dd HH:mm:ss")}&estimatedCompletionDate=${period.end?string("yyyy-MM-dd HH:mm:ss")}${addlParam?if_exists}</@ofbizUrl>'>${uiLabelMap.CommonAddNew}</a>
      <br class="clear"/>
      <#list period.calendarEntries as calEntry>
        <hr/>
        <#if (calEntry.workEffort.estimatedStartDate.compareTo(period.start)  <= 0 && calEntry.workEffort.estimatedCompletionDate.compareTo(period.end) >= 0)>
          ${uiLabelMap.CommonAllDay}
        <#elseif calEntry.workEffort.estimatedStartDate.before(period.start)>
          ${uiLabelMap.CommonUntil} ${calEntry.workEffort.estimatedCompletionDate?time?string.short}
        <#elseif calEntry.workEffort.estimatedCompletionDate.after(period.end)>
          ${uiLabelMap.CommonFrom} ${calEntry.workEffort.estimatedStartDate?time?string.short}
        <#else>
          ${calEntry.workEffort.estimatedStartDate?time?string.short}-${calEntry.workEffort.estimatedCompletionDate?time?string.short}
        </#if>
        <br/>
        <a href="<@ofbizUrl>WorkEffortSummary?workEffortId=${calEntry.workEffort.workEffortId}${addlParam?if_exists}</@ofbizUrl>" class="event">${calEntry.workEffort.workEffortName?default("Undefined")}</a>&nbsp;${calEntry.eventStatus?default("&nbsp;")}
        <br/>
      </#list>
    </td>

<#--
    <td valign="top">
      <table width="100%" cellspacing="0" cellpadding="0" border="0">
        <tr>
          <td nowrap class="monthdaynumber"><a href='<@ofbizUrl>day?start=${period.start.time?string("#")}<#if eventsParam?has_content>&${eventsParam}</#if>${addlParam?if_exists}</@ofbizUrl>' class="monthdaynumber">${period.start?date?string("d")?cap_first}</a></td>
          <td align="right"><a href='<@ofbizUrl>EditWorkEffort?workEffortTypeId=EVENT&currentStatusId=CAL_TENTATIVE&estimatedStartDate=${period.start?string("yyyy-MM-dd HH:mm:ss")}&estimatedCompletionDate=${period.end?string("yyyy-MM-dd HH:mm:ss")}${addlParam?if_exists}</@ofbizUrl>' class="add">${uiLabelMap.CommonAddNew}</a>&nbsp;&nbsp;</td>
        </tr>
      </table>
      <#list period.calendarEntries as calEntry>
      <table width="100%" cellspacing="0" cellpadding="0" border="0">
        <tr width="100%">
          <td class='monthcalendarentry' width="100%" valign='top'>
            <#if (calEntry.workEffort.estimatedStartDate.compareTo(period.start)  <= 0 && calEntry.workEffort.estimatedCompletionDate.compareTo(period.end) >= 0)>
              ${uiLabelMap.CommonAllDay}
            <#elseif calEntry.workEffort.estimatedStartDate.before(period.start)>
              ${uiLabelMap.CommonUntil} ${calEntry.workEffort.estimatedCompletionDate?time?string.short}
            <#elseif calEntry.workEffort.estimatedCompletionDate.after(period.end)>
              ${uiLabelMap.CommonFrom} ${calEntry.workEffort.estimatedStartDate?time?string.short}
            <#else>
              ${calEntry.workEffort.estimatedStartDate?time?string.short}-${calEntry.workEffort.estimatedCompletionDate?time?string.short}
            </#if>
            <br/>
            <a href="<@ofbizUrl>WorkEffortSummary?workEffortId=${calEntry.workEffort.workEffortId}${addlParam?if_exists}</@ofbizUrl>" class="event">${calEntry.workEffort.workEffortName?default("Undefined")}</a>&nbsp;
          </td>
        </tr>
      </table>
      </#list>
    </td>
-->
    <#if !period_has_next && indexMod7 != 6>
    <td colspan='${6 - (indexMod7)}'>&nbsp;</td>
    </#if>
  <#if indexMod7 = 6 || !period_has_next>
  </tr>
  </#if>
  </#list>
</table>

<#else>
  <div class="screenlet-body">${uiLabelMap.WorkEffortFailedCalendarEntries}!</div>
</#if>
