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

<h1>${uiLabelMap.WebtoolsStatsBinsPageTitle}</h1>
<br />
<div class="button-bar"><a href="<@ofbizUrl>StatsSinceStart</@ofbizUrl>" class="smallSubmit">${uiLabelMap.WebtoolsStatsMainPageTitle}</a>
<a href="<@ofbizUrl>StatBinsHistory?statsId=${parameters.statsId}&type=${parameters.type}</@ofbizUrl>" class="smallSubmit">${uiLabelMap.WebtoolsStatsReloadPage}</a></div>
<p>${uiLabelMap.WebtoolsStatsCurrentTime}: ${Static["org.ofbiz.base.util.UtilDateTime"].nowTimestamp().toString()}</p>
<br />
<#if security.hasPermission("SERVER_STATS_VIEW", session)>

  <div id="stats-bins-history" class="screenlet">
    <div class="screenlet-title-bar">
      <h3>${uiLabelMap.WebtoolsStatsRequestStats}</h3>
    </div>
    <#if requestList?has_content>
      <#if (requestList?size > 2)>
        <table class="basic-table light-grid hover-bar" cellspacing="0">
      <#else>
        <table class="basic-table light-grid" cellspacing="0">
      </#if>
        <tr class="header-row">
          <td>${uiLabelMap.WebtoolsStatsRequestId}</td>
          <td>${uiLabelMap.WebtoolsStatsStart}</td>
          <td>${uiLabelMap.WebtoolsStatsStop}</td>
          <td>${uiLabelMap.WebtoolsStatsMinutes}</td>
          <td>${uiLabelMap.WebtoolsStatsHits}</td>
          <td>${uiLabelMap.WebtoolsStatsMin}</td>
          <td>${uiLabelMap.WebtoolsStatsAvg}</td>
          <td>${uiLabelMap.WebtoolsStatsMax}</td>
          <td>${uiLabelMap.WebtoolsStatsHitsPerMin}</td>
        </tr>
        <#assign rowNum = "2">
        <#list requestList as requestRow>
          <tr<#if rowNum == "1"> class="alternate-row"</#if>>
            <td>${requestRow.requestId}</td>
            <td>${requestRow.startTime}</td>
            <td>${requestRow.endTime}</td>
            <td>${requestRow.lengthMins}</td>
            <td>${requestRow.numberHits}</td>
            <td>${requestRow.minTime}</td>
            <td>${requestRow.avgTime}</td>
            <td>${requestRow.maxTime}</td>
            <td>${requestRow.hitsPerMin}</td>
          </tr>
          <#if rowNum == "2">
            <#assign rowNum = "1">
          <#else>
            <#assign rowNum = "2">
          </#if>
        </#list>
      </table>
    <#else>
      <div class="screenlet-body">${uiLabelMap.WebtoolsStatsBinsErrorMsg}</div>
    </#if>
  </div>
<#else>
  <h3>${uiLabelMap.WebtoolsStatsPermissionMsg}</h3>
</#if>
<!-- end StatsBinsHistory.ftl -->
