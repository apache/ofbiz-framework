<#--
 *  Copyright (c) 2001-2005 The Open For Business Project - www.ofbiz.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a 
 *  copy of this software and associated documentation files (the "Software"), 
 *  to deal in the Software without restriction, including without limitation 
 *  the rights to use, copy, modify, merge, publish, distribute, sublicense, 
 *  and/or sell copies of the Software, and to permit persons to whom the 
 *  Software is furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included 
 *  in all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS 
 *  OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF 
 *  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. 
 *  IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY 
 *  CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT 
 *  OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR 
 *  THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 *@author     Johan Isacsson
 *@created    May 19 2003
 *@author     Eric.Barbier@nereide.biz (migration to uiLabelMap)
 *@version    1.0
-->
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
