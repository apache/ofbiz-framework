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

<#if techDataCalendar?has_content>
  <h1>${uiLabelMap.ManufacturingUpdateCalendar} </h1>
  <form name="calendarform" method="post" action="<@ofbizUrl>UpdateCalendar</@ofbizUrl>">
    <input type="hidden" name="calendarId" value="${techDataCalendar.calendarId}">
<#else>
  <h1>${uiLabelMap.ManufacturingCreateCalendar}</h1>
  <form name="calendarform" method="post" action="<@ofbizUrl>CreateCalendar</@ofbizUrl>">
</#if>

  <br/>
  <table width="90%" border="0" cellpadding="2" cellspacing="0">
    <#if techDataCalendar?has_content>
    <tr>
      <td width='26%' align='right' valign='top'>${uiLabelMap.ManufacturingCalendarId}</td>
      <td width="5">&nbsp;</td>
      <td width="74%" valign="top"><b>${techDataCalendar.calendarId?if_exists}</b> (${uiLabelMap.CommonNotModifRecreat})</td>
    </tr>
    <#else>
    <tr>
      <td width='26%' align='right' valign="top">${uiLabelMap.ManufacturingCalendarId}</td>
      <td width="5">&nbsp;</td>
      <td width="74%"><input type="text" size="12" name="calendarId" value="${calendarData.calendarId?if_exists}"></td>
    </tr>
    </#if>
    <tr>
      <td width='26%' align='right' valign='top'>${uiLabelMap.CommonDescription}</td>
      <td width="5">&nbsp;</td>
      <td width="74%"><input type="text" size="40" name="description" value="${calendarData.description?if_exists}"></td>
    </tr>
    <tr>
      <td width='26%' align='right' valign='top'>${uiLabelMap.ManufacturingCalendarWeekId}</td>
      <td width="5">&nbsp;</td>
      <td width="74%">
         <select name="calendarWeekId">
          <#list calendarWeeks as calendarWeek>
          <option value="${calendarWeek.calendarWeekId}" <#if calendarData?has_content && calendarData.calendarWeekId?default("") == calendarWeek.calendarWeekId>SELECTED</#if>>${(calendarWeek.get("description",locale))?if_exists}</option>
          </#list>
        </select>
    </tr>
    <tr>
      <td width="26%" align="right" valign="top">
      <td width="5">&nbsp;</td>
      <td width="74%"><input type="submit" value="${uiLabelMap.CommonUpdate}"></td>
    </tr>
  </table>
</form>
<table width="90%" border="0" cellpadding="2" cellspacing="0">
    <tr>
        <td width='100%' align='right' valign='top'>
            <a href="<@ofbizUrl>EditCalendar</@ofbizUrl>" class="buttontext">${uiLabelMap.ManufacturingNewCalendar}</a>
        </td>
    </tr>
</table>
