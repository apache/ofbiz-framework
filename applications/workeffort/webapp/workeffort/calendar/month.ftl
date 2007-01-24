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
<#-- I'm not quite sure why these should be "en". But I'm sure it works with it and don't without. 
I assume it's because we calculate date before in this locale (set from command line) - JLR 21/10/2006
Please see https://issues.apache.org/jira/browse/OFBIZ-392-->
<#setting locale="en"> 
<#assign locale="en"> 

<table border='0' width='100%' cellspacing='0' cellpadding='0' class='boxoutside'>
  <tr> 
    <td width='100%'> 
      <table width='100%' border='0' cellspacing='0' cellpadding='0' class='boxtop'>
        <tr> 
          <td align="left" class="boxhead">${uiLabelMap.WorkEffortCalendarMonthView}</td>
        </tr>
      </table>
    </td>
  </tr>
</table>
<table width="100%" border="0" cellspacing="0" cellpadding="0" class="monthheadertable">
  <tr>
    <td width="100%" class="monthheadertext">${start?date?string("MMMM yyyy")?cap_first}</td>
    <td nowrap class="previousnextmiddle"><a href='<@ofbizUrl>month?start=${prev.time?string("#")}<#if eventsParam?has_content>&${eventsParam}</#if></@ofbizUrl>' class="previousnext">${uiLabelMap.WorkEffortPreviousMonth}</a> | <a href='<@ofbizUrl>month?start=${next.time?string("#")}<#if eventsParam?has_content>&${eventsParam}</#if></@ofbizUrl>' class="previousnext">${uiLabelMap.WorkEffortNextMonth}</a> | <a href='<@ofbizUrl>month?start=${now.time?string("#")}<#if eventsParam?has_content>&${eventsParam}</#if></@ofbizUrl>' class="previousnext">${uiLabelMap.WorkEffortThisMonth}</a></td>
  </tr>
</table>
<#if periods?has_content> 
<table width="100%" cellspacing="1" border="0" cellpadding="1" class="calendar">
  <tr class="bg">
    <td width="1%" class="monthdayheader">&nbsp;<br/>
      <img src="<@ofbizContentUrl>/images/spacer.gif</@ofbizContentUrl>" alt="" height="1" width="88"></td>
    <#list periods as day>
    <td width="14%" class="monthdayheader">${day.start?date?string("EEEE")?cap_first}<br/>
      <img src="<@ofbizContentUrl>/images/spacer.gif</@ofbizContentUrl>" alt="" height="1" width="1"></td>
    <#if (day_index > 5)><#break></#if>
    </#list>
  </tr>
  <#list periods as period>
  <#assign indexMod7 = period_index % 7>
  <#if indexMod7 = 0>
  <tr class="bg">
    <td valign="top" height="60" nowrap class="monthweekheader"><a href='<@ofbizUrl>week?start=${period.start.time?string("#")}<#if eventsParam?has_content>&${eventsParam}</#if></@ofbizUrl>' class="monthweeknumber">${uiLabelMap.CommonWeek} ${period.start?date?string("w")}</a></td>
  </#if>
    <td valign="top">
      <table width="100%" cellspacing="0" cellpadding="0" border="0">
        <tr>
          <td nowrap class="monthdaynumber"><a href='<@ofbizUrl>day?start=${period.start.time?string("#")}<#if eventsParam?has_content>&${eventsParam}</#if></@ofbizUrl>' class="monthdaynumber">${period.start?date?string("d")?cap_first}</a></td>
          <td align="right"><a href='<@ofbizUrl>EditWorkEffort?workEffortTypeId=EVENT&currentStatusId=CAL_TENTATIVE&estimatedStartDate=${period.start?string("yyyy-MM-dd HH:mm:ss")}&estimatedCompletionDate=${period.end?string("yyyy-MM-dd HH:mm:ss")}</@ofbizUrl>' class="add">${uiLabelMap.CommonAddNew}</a>&nbsp;&nbsp;</td>
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
            <a href="<@ofbizUrl>WorkEffortSummary?workEffortId=${calEntry.workEffort.workEffortId}</@ofbizUrl>" class="event">${calEntry.workEffort.workEffortName?default("Undefined")}</a>&nbsp;
          </td>
        </tr>
      </table>
      </#list>
    </td>
    <#if !period_has_next && indexMod7 != 6>
    <td colspan='${6 - (indexMod7)}'>&nbsp;</td>
    </#if>
  <#if indexMod7 = 6 || !period_has_next>
  </tr>
  </#if>
  </#list>
</table>

<#else> 
<p>${uiLabelMap.WorkEffortFailedCalendarEntries}!</p>
</#if>
