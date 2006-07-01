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
 *@author     David E. Jones
 *@author     Johan Isacsson
 *@created    May 19 2003
 *@author     Eric.Barbier@nereide.biz (migration to uiLabelMap)
-->

<table border="0" width="100%" cellspacing="0" cellpadding="0" class="boxoutside">
  <tr>
    <td width="100%">
      <table width="100%" border="0" cellspacing="0" cellpadding="0" class="boxtop">
        <tr>
          <td align="left" class="boxhead">${uiLabelMap.WorkEffortCalendarUpComingEventsView}</td>
        </tr>
      </table>
    </td>
  </tr>
  <tr>
    <td width="100%">
      <table width="100%" border="0" cellspacing="0" cellpadding="0" class="boxbottom">
        <tr>
          <td>
          <#if days?has_content>
              <table width="100%" cellpadding="2" cellspacing="0" border="0">
                <tr>
                  <td><div class="tabletext"><b>${uiLabelMap.CommonStartDateTime}</b></div></td>
                  <td><div class="tabletext"><b>${uiLabelMap.CommonEndDateTime}</b></div></td>
                  <td><div class="tabletext"><b>${uiLabelMap.WorkEffortEventName}</b></div></td>
                </tr>                
                <#list days as day>
                  <#assign workEfforts = day.calendarEntries>
                  <#if workEfforts?has_content>
                    <tr><td colspan="3"><hr class="sepbar"/></td></tr>
                    <#list workEfforts as calendarEntry>
                      <#assign workEffort = calendarEntry.workEffort>
                      <tr>
                        <td><div class="tabletext">${workEffort.estimatedStartDate}</div></td>
                        <td><div class="tabletext">${workEffort.estimatedCompletionDate}</div></td>
                        <td><a class="buttontext" href="<@ofbizUrl>EditWorkEffort?workEffortId=${workEffort.workEffortId}</@ofbizUrl>">${workEffort.workEffortName}</a></div></td>
                      </tr>
                    </#list>
                  </#if>
                </#list>
              </table>
            <#else>
              <div class="tabletext">${uiLabelMap.WorkEffortNoEventsFound}.</div>
            </#if>
          </td>
        </tr>
      </table>
    </td>
  </tr>
</table>
