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

<h1>Entity Engine Performance Tests</h1>
<br />
<div class="screenlet">
  <div class="screenlet-title-bar">
    <h3>Test Results</h3>
  </div>
  <div class="screenlet-body">  
    <#if security.hasPermission("ENTITY_MAINT", session)>
      <p>NOTE: These performance results may vary a great deal for different
      databases, JDBC drivers, JTA implementations (transaction managers), connection pools, 
      local vs. remote deployment configurations, and hardware (app server hardware, database 
      server hardware, network connections).</p>
      <br/>
      <#if performanceList?has_content>
        <table class="basic-table" cellspacing="0">
          <tr class="header-row">
            <td>Operation</td>
            <td>Entity</td>
            <td>Calls</td>
            <td>Seconds</td>
            <td>Seconds/Call</td>
            <td>Calls/Second</td>
          </tr>
          <#assign rowNum = "2">
          <#list performanceList as perfRow>
            <tr<#if rowNum == "1"> class="alternate-row"</#if>>
              <td>${perfRow.operation}</td>
              <td>${perfRow.entity}</td>
              <td>${perfRow.calls}</td>
              <td>${perfRow.seconds}</td>
              <td>${perfRow.secsPerCall}</td>
              <td>${perfRow.callsPerSecond}</td>
            </tr>
          </#list>
        </table>
      <#else>
        No performance tests found.
      </#if>
    <#else>
      ERROR: You do not have permission to use this page (ENTITY_MAINT needed)
    </#if>
  </div>
</div>
