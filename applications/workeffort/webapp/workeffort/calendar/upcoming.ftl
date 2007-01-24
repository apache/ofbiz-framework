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
