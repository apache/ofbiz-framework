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
    <h3>${uiLabelMap.WebtoolsEntityEnginePerformanceTests} - ${uiLabelMap.WebtoolsTestResults}</h3>
  </div>
  <div class="screenlet-body">
    <#if security.hasPermission("ENTITY_MAINT", session)>
      <p>${uiLabelMap.WebtoolsNotePerformanceResultsMayVary}</p>
      <br />
      <#if performanceList?has_content>
        <table class="basic-table hover-bar" cellspacing="0">
          <tr class="header-row">
            <td>${uiLabelMap.WebtoolsPerformanceOperation}</td>
            <td>${uiLabelMap.WebtoolsEntity}</td>
            <td>${uiLabelMap.WebtoolsPerformanceCalls}</td>
            <td>${uiLabelMap.WebtoolsPerformanceSeconds}</td>
            <td>${uiLabelMap.WebtoolsPerformanceSecondsCall}</td>
            <td>${uiLabelMap.WebtoolsPerformanceCallsSecond}</td>
          </tr>
          <#assign alt_row = false>
          <#list performanceList as perfRow>
            <tr valign="middle"<#if alt_row> class="alternate-row"</#if>>
              <td>${perfRow.operation}</td>
              <td>${perfRow.entity}</td>
              <td>${perfRow.calls}</td>
              <td>${perfRow.seconds}</td>
              <td>${perfRow.secsPerCall}</td>
              <td>${perfRow.callsPerSecond}</td>
            </tr>
            <#-- toggle the row color -->
            <#assign alt_row = !alt_row>
          </#list>
        </table>
      <#else>
        ${uiLabelMap.WebtoolsPerformanceNotFound}.
      </#if>
    <#else>
      ${uiLabelMap.WebtoolsPermissionMaint}
    </#if>
  </div>
</div>