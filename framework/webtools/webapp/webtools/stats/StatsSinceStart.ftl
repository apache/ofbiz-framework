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
<div id="stats-bins-history" class="screenlet">
    <div class="screenlet-title-bar">
      <div class="h3">${uiLabelMap.WebtoolsStatsMainPageTitle}</div>
    </div>
    <div class="screenlet-body">
      <div class="button-bar">
        <a href="<@ofbizUrl>StatsSinceStart?clear=true</@ofbizUrl>" class="buttontext">${uiLabelMap.WebtoolsStatsClearSince}</a>
        <a href="<@ofbizUrl>StatsSinceStart</@ofbizUrl>" class="buttontext refresh">${uiLabelMap.WebtoolsStatsReloadPage}</a>
      </div>
      <p><span class="label">${uiLabelMap.WebtoolsStatsCurrentTime}</span> ${nowTimestamp}</p>
    </div>
</div>
<#if security.hasPermission("SERVER_STATS_VIEW", session)>
  <#-- Request Table -->
  <div id="request-statistics" class="screenlet">
    <div class="screenlet-title-bar">
      <div class="h3">${uiLabelMap.WebtoolsStatsRequestStats}</div>
    </div>
    <#if requestList?has_content>
    <div class="screenlet-body">
      <#if (requestList?size > 2)>
        <table class="basic-table light-grid hover-bar" cellspacing="0">
      <#else>
        <table class="basic-table hover-bar" cellspacing="0">
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
          <td>&nbsp;</td>
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
            <td class="button-col"><a href="<@ofbizUrl>StatBinsHistory?statsId=${requestRow.requestId}&type=${requestRow.requestType}</@ofbizUrl>">${uiLabelMap.WebtoolsStatsViewBins}</a></td>
          </tr>
          <#if rowNum == "2">
            <#assign rowNum = "1">
          <#else>
            <#assign rowNum = "2">
          </#if>
        </#list>
      </table>
    </div>  
    <#else>
      <div class="screenlet-body">${uiLabelMap.WebtoolsStatsNoRequests}</div>
    </#if>
  </div>
  <br />
  <#-- Event Table -->
  <div id="event-statistics" class="screenlet">
    <div class="screenlet-title-bar">
      <div class="h3">${uiLabelMap.WebtoolsStatsEventStats}</div>
    </div>
    <#if eventList?has_content>
    <div class="screenlet-body">
      <#if (eventList?size > 2)>
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
          <td>&nbsp;</td>
        </tr>
        <#assign rowNum = "2">
        <#list eventList as requestRow>
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
            <td class="button-col"><a href="<@ofbizUrl>StatBinsHistory?statsId=${requestRow.requestId}&type=${requestRow.requestType}</@ofbizUrl>">${uiLabelMap.WebtoolsStatsViewBins}</a></td>
          </tr>
          <#if rowNum == "2">
            <#assign rowNum = "1">
          <#else>
            <#assign rowNum = "2">
          </#if>
        </#list>
      </table>
    </div>
    <#else>
      <div class="screenlet-body">${uiLabelMap.WebtoolsStatsNoEvents}</div>
    </#if>
  </div>
  <br />
  <#-- View Table -->
  <div id="view-statistics" class="screenlet">
    <div class="screenlet-title-bar">
      <div class="h3">${uiLabelMap.WebtoolsStatsViewStats}</div>
    </div>
    <#if viewList?has_content>
    <div class="screenlet-body">
      <#if (viewList?size > 2)>
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
          <td>&nbsp;</td>
        </tr>
        <#assign rowNum = "2">
        <#list viewList as requestRow>
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
            <td class="button-col"><a href="<@ofbizUrl>StatBinsHistory?statsId=${requestRow.requestId}&type=${requestRow.requestType}</@ofbizUrl>">${uiLabelMap.WebtoolsStatsViewBins}</a></td>
          </tr>
          <#if rowNum == "2">
            <#assign rowNum = "1">
          <#else>
            <#assign rowNum = "2">
          </#if>
        </#list>
      </table>
    </div>
    <#else>
      <div class="screenlet-body">${uiLabelMap.WebtoolsStatsNoViews}</div>
    </#if>
  </div>
<#else>
  <h3>${uiLabelMap.WebtoolsStatsPermissionMsg}</h3>
</#if>
<!-- end StatsSinceStart.ftl -->