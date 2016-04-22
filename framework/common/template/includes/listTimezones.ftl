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
<div class="screenlet">
  <div class="screenlet-title-bar">
    <ul>
      <li class="h3">${uiLabelMap.CommonTimeZone}</li>
      <li><a href="<@ofbizUrl>main</@ofbizUrl>">${uiLabelMap.CommonCancel}</a></li>
    </ul>
    <br class="clear"/>
  </div>
  <table cellspacing="0" class="basic-table hover-bar">
    <#assign altRow = true>
    <#assign displayStyle = Static["java.util.TimeZone"].LONG>
    <#assign availableTimeZones = Static["org.ofbiz.base.util.UtilDateTime"].availableTimeZones()/>
    <#list availableTimeZones as availableTz>
      <#assign altRow = !altRow>
      <tr<#if altRow> class="alternate-row"</#if>>
        <td>
          <a href="<@ofbizUrl>setSessionTimeZone</@ofbizUrl>?tzId=${availableTz.getID()}">${availableTz.getDisplayName(availableTz.useDaylightTime(), displayStyle, locale)} (${availableTz.getID()})</a>
        </td>
      </tr>
    </#list>
  </table>
</div>
