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

<div><a href="<@ofbizUrl>EditCalendar</@ofbizUrl>" class="buttontext">${uiLabelMap.ManufacturingNewCalendar}</a></div>

<br/>
<table width="100%" border="0" cellpadding="0" cellspacing="0"> 
  <tr>
    <th>${uiLabelMap.ManufacturingCalendarId}</th>
    <th>${uiLabelMap.CommonDescription}</th>
    <th>${uiLabelMap.ManufacturingCalendarWeekId}</th>
    <td>&nbsp;</td>
  </tr>  
  <tr><td colspan="4"><hr class="sepbar"/></td></tr>    
  <#if techDataCalendars?has_content>
    <#list techDataCalendars as techDataCalendar>
      <tr>
        <td>${techDataCalendar.calendarId}</td>
        <td>${techDataCalendar.get("description",locale)?if_exists}</td>
        <td>${techDataCalendar.calendarWeekId?if_exists}</td>
        <td align="right">
          <a href="<@ofbizUrl>EditCalendar?calendarId=${techDataCalendar.calendarId}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonEdit}</a>
          <a href="<@ofbizUrl>RemoveCalendar?calendarId=${techDataCalendar.calendarId}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonDelete}</a>
        </td>        
      </tr>
    </#list>
  <#else>
    <tr>
      <td colspan="4">${uiLabelMap.ManufacturingNoCalendarFound}</td>
    </tr>    
  </#if>
</table>
