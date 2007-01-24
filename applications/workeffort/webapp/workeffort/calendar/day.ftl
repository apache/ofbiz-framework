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

<table border="0" width="100%" cellspacing="0" cellpadding="0" class="boxoutside">
  <tr> 
    <td width="100%"> 
      <table width="100%" border="0" cellspacing="0" cellpadding="0" class="boxtop">
        <tr>
          <td align="left" class="boxhead">${uiLabelMap.WorkEffortCalendarDayView}</td>
        </tr>
      </table>
    </td>
  </tr>
</table>
<table width="100%" border="0" cellspacing="0" cellpadding="0" class="monthheadertable">
  <tr>
    <td width="100%" class="monthheadertext">${start?date?string("EEEE")?cap_first} ${start?date?string.long}</td>
    <td nowrap class="previousnextmiddle"><a href="<@ofbizUrl>day?start=${prev.time?string("#")}<#if eventsParam?has_content>&${eventsParam}</#if></@ofbizUrl>" class="previousnext">${uiLabelMap.WorkEffortPreviousDay}</a> | <a href="<@ofbizUrl>day?start=${next.time?string("#")}<#if eventsParam?has_content>&${eventsParam}</#if></@ofbizUrl>" class="previousnext">${uiLabelMap.WorkEffortNextDay}</a> | <a href="<@ofbizUrl>day?start=${now.time?string("#")}<#if eventsParam?has_content>&${eventsParam}</#if></@ofbizUrl>" class="previousnext">${uiLabelMap.CommonToday}</a></td>
  </tr>
</table>
<#if periods?has_content>

<#if (maxConcurrentEntries = 0)>
  <#assign entryWidth = 100>
<#elseif (maxConcurrentEntries < 2)>
  <#assign entryWidth = (100 / (maxConcurrentEntries + 1))>
<#else> 
  <#assign entryWidth = (100 / (maxConcurrentEntries))>
</#if>
<table width="100%" cellspacing="1" border="0" cellpadding="1" class="calendar">                
  <tr>             
    <td nowrap class="monthdayheader">${uiLabelMap.CommonTime}<br/>
      <img src="<@ofbizContentUrl>/images/spacer.gif</@ofbizContentUrl>" alt="" height="1" width="88"></td>
    <td colspan=${maxConcurrentEntries} class="monthdayheader">${uiLabelMap.WorkEffortCalendarEntries}<br/>
      <img src="<@ofbizContentUrl>/images/spacer.gif</@ofbizContentUrl>" alt="" height="1" width="88"></td>
  </tr>
  <#list periods as period>              
  <tr>                  
    <td valign="top" nowrap width="1%" class="monthweekheader" height="36"><span class="monthweeknumber">${period.start?time?string.short}</span><br/>
      <a href="<@ofbizUrl>EditWorkEffort?workEffortTypeId=EVENT&currentStatusId=CAL_TENTATIVE&estimatedStartDate=${period.start?string("yyyy-MM-dd HH:mm:ss")}&estimatedCompletionDate=${period.end?string("yyyy-MM-dd HH:mm:ss")}</@ofbizUrl>">${uiLabelMap.CommonAddNew}</a></td>
    <#list period.calendarEntries as calEntry>
    <#if calEntry.startOfPeriod>
    <td class="calendarentry" rowspan="${calEntry.periodSpan}" colspan="1" width="${entryWidth?string("#")}%" valign="top">
    <#if (calEntry.workEffort.estimatedStartDate.compareTo(start)  <= 0 && calEntry.workEffort.estimatedCompletionDate.compareTo(next) >= 0)>
      ${uiLabelMap.CommonAllDay}
    <#elseif calEntry.workEffort.estimatedStartDate.before(start)>
      ${uiLabelMap.CommonUntil}${calEntry.workEffort.estimatedCompletionDate?time?string.short}
    <#elseif calEntry.workEffort.estimatedCompletionDate.after(next)>
      ${uiLabelMap.CommonFrom} ${calEntry.workEffort.estimatedStartDate?time?string.short}
    <#else>
      ${calEntry.workEffort.estimatedStartDate?time?string.short}-${calEntry.workEffort.estimatedCompletionDate?time?string.short}
    </#if>
    <br/><a href="<@ofbizUrl>WorkEffortSummary?workEffortId=${calEntry.workEffort.workEffortId}</@ofbizUrl>" class="event">${calEntry.workEffort.workEffortName?default("Undefined")}</a>&nbsp;</td>
    </#if>
    </#list>
    <#if period.calendarEntries?size < maxConcurrentEntries>
    <#assign emptySlots = (maxConcurrentEntries - period.calendarEntries?size)>
    <#list 1..emptySlots as num>
      <td width="${entryWidth?string("#")}%"  class="calendarempty"><br/></td>
    </#list>
    </#if>
    <#if maxConcurrentEntries < 2>
    <td width="${entryWidth?string("#")}" class="calendarempty">&nbsp;</td>
    </#if>
  </tr>
  </#list>                  
</table>
<#else>               
  <p>${uiLabelMap.WorkEffortFailedCalendarEntries}!</p>
</#if>
