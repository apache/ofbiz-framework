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

        <#if calendarWeek?has_content>
            <h1>${uiLabelMap.ManufacturingUpdateCalendarWeek}</h1>
            <div>
                <a href="<@ofbizUrl>EditCalendarWeek</@ofbizUrl>" class="buttontext">${uiLabelMap.ManufacturingNewCalendarWeek}</a>
            </div>
            <br/>
            <form name="calendarWeekform" method="post" action="<@ofbizUrl>UpdateCalendarWeek</@ofbizUrl>">
            <table width="90%" border="0" cellpadding="2" cellspacing="0">
              <tr>
                  <td width="26%" align="right" valign="top">${uiLabelMap.ManufacturingCalendarWeekId}</td>
                  <td width="5">&nbsp;</td>
                <input type="hidden" name="calendarWeekId" value="${calendarWeek.calendarWeekId}">
                  <td width="74%" valign="top" colspan="5"><b>${calendarWeek.calendarWeekId?if_exists}</b> (${uiLabelMap.CommonNotModifRecreat})</td>
        <#else>
            <h1>${uiLabelMap.ManufacturingCreateCalendarWeek}</h1>
            <div>
                <a href="<@ofbizUrl>EditCalendarWeek</@ofbizUrl>" class="buttontext">${uiLabelMap.ManufacturingNewCalendarWeek}</a>
            </div>
            <br/>
              <form name="calendarWeekform" method="post" action="<@ofbizUrl>CreateCalendarWeek</@ofbizUrl>">
            <table width="90%" border="0" cellpadding="2" cellspacing="0">
              <tr>
                  <td width="26%" align="right" valign="top">${uiLabelMap.ManufacturingCalendarWeekId}</td>
                  <td width="5">&nbsp;</td>
                  <td width="74%" colspan="5"><input type="text" size="12" name="calendarWeekId" value="${calendarWeekData.calendarWeekId?if_exists}"></td>
        </#if>
            </tr>
            <tr>
                  <td width="26%" align="right" valign="top">${uiLabelMap.CommonDescription}</td>
                  <td width="5">&nbsp;</td>
                  <td width="74%" colspan="5"><input type="text" size="30" name="description" value="${calendarWeekData.description?if_exists}"></td>
            </tr>
            <tr>
                  <td width="26%" align="right" valign="top"><b>${uiLabelMap.CommonMonday}</b>&nbsp;${uiLabelMap.ManufacturingStartTime}</td>
                  <td width="5">&nbsp;</td>
                  <td width="1%"><input type="text" size="12" name="mondayStartTime" value="${calendarWeekData.mondayStartTime?if_exists}"></td>
                  <td width="5">&nbsp;</td>
                  <td width="1%" align="right" valign="top">${uiLabelMap.ManufacturingCalendarCapacity}</td>
                  <td width="1%">&nbsp;</td>
                  <td width="40%"><input type="text" size="8" name="mondayCapacity" value="${calendarWeekData.mondayCapacity?if_exists}"></td>
            </tr>
             <tr>
                  <td width="26%" align="right" valign="top"><b>${uiLabelMap.CommonTuesday}</b>&nbsp;${uiLabelMap.ManufacturingStartTime}</td>
                  <td width="5">&nbsp;</td>
                  <td width="1%"><input type="text" size="12" name="tuesdayStartTime" value="${calendarWeekData.tuesdayStartTime?if_exists}"></td>
                  <td width="5">&nbsp;</td>
                  <td width="1%" align="right" valign="top">${uiLabelMap.ManufacturingCalendarCapacity}</td>
                  <td width="1%">&nbsp;</td>
                  <td width="40%"><input type="text" size="8" name="tuesdayCapacity" value="${calendarWeekData.tuesdayCapacity?if_exists}"></td>
            </tr>
             <tr>
                  <td width="26%" align="right" valign="top"><b>${uiLabelMap.CommonWednesday}</b>&nbsp;${uiLabelMap.ManufacturingStartTime}</td>
                  <td width="5">&nbsp;</td>
                  <td width="1%"><input type="text" size="12" name="wednesdayStartTime" value="${calendarWeekData.wednesdayStartTime?if_exists}"></td>
                  <td width="5">&nbsp;</td>
                  <td width="1%" align="right" valign="top">${uiLabelMap.ManufacturingCalendarCapacity}</td>
                  <td width="1%">&nbsp;</td>
                  <td width="40%"><input type="text" size="8" name="wednesdayCapacity" value="${calendarWeekData.wednesdayCapacity?if_exists}"></td>
            </tr>
             <tr>
                  <td width="26%" align="right" valign="top"><b>${uiLabelMap.CommonThursday}</b>&nbsp;${uiLabelMap.ManufacturingStartTime}</td>
                  <td width="5">&nbsp;</td>
                  <td width="1%"><input type="text" size="12" name="thursdayStartTime" value="${calendarWeekData.thursdayStartTime?if_exists}"></td>
                  <td width="5">&nbsp;</td>
                  <td width="1%" align="right" valign="top">${uiLabelMap.ManufacturingCalendarCapacity}</td>
                  <td width="1%">&nbsp;</td>
                  <td width="40%"><input type="text" size="8" name="thursdayCapacity" value="${calendarWeekData.thursdayCapacity?if_exists}"></td>
            </tr>
             <tr>
                  <td width="26%" align="right" valign="top"><b>${uiLabelMap.CommonFriday}</b>&nbsp;${uiLabelMap.ManufacturingStartTime}</td>
                  <td width="5">&nbsp;</td>
                  <td width="1%"><input type="text" size="12" name="fridayStartTime" value="${calendarWeekData.fridayStartTime?if_exists}"></td>
                  <td width="5">&nbsp;</td>
                  <td width="1%" align="right" valign="top">${uiLabelMap.ManufacturingCalendarCapacity}</td>
                  <td width="1%">&nbsp;</td>
                  <td width="40%"><input type="text" size="8" name="fridayCapacity" value="${calendarWeekData.fridayCapacity?if_exists}"></td>
            </tr>
             <tr>
                  <td width="26%" align="right" valign="top"><b>${uiLabelMap.CommonSaturday}</b>&nbsp;${uiLabelMap.ManufacturingStartTime}</td>
                  <td width="5">&nbsp;</td>
                  <td width="1%"><input type="text" size="12" name="saturdayStartTime" value="${calendarWeekData.saturdayStartTime?if_exists}"></td>
                  <td width="5">&nbsp;</td>
                  <td width="1%" align="right" valign="top">${uiLabelMap.ManufacturingCalendarCapacity}</td>
                  <td width="1%">&nbsp;</td>
                  <td width="40%"><input type="text" size="8" name="saturdayCapacity" value="${calendarWeekData.saturdayCapacity?if_exists}"></td>
            </tr>
             <tr>
                  <td width="26%" align="right" valign="top"><b>${uiLabelMap.CommonSunday}</b>&nbsp;${uiLabelMap.ManufacturingStartTime}</td>
                  <td width="5">&nbsp;</td>
                  <td width="1%"><input type="text" size="12" name="sundayStartTime" value="${calendarWeekData.sundayStartTime?if_exists}"></td>
                  <td width="5">&nbsp;</td>
                  <td width="1%" align="right" valign="top">${uiLabelMap.ManufacturingCalendarCapacity}</td>
                  <td width="1%">&nbsp;</td>
                  <td width="40%"><input type="text" size="8" name="sundayCapacity" value="${calendarWeekData.sundayCapacity?if_exists}"></td>
            </tr>
            <tr>
                  <td width="26%" align="right" valign="top">
                  <td width="5">&nbsp;</td>
                  <td width="74%" colspan="5"><input type="submit" value="${uiLabelMap.CommonUpdate}"/></td>
            </tr>
        </table>
    <br/>


    