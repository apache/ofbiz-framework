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

<p>
  <a href="<@ofbizUrl>EncryptValue</@ofbizUrl>" class="buttontext">&larr; ${uiLabelMap.WebtoolsSecretManagerTools}</a>
</p>

<p>
  ${uiLabelMap.WebtoolsSecretUsageTotalHits}: <strong>${usageSummary.totalHits!0}</strong> &nbsp;|&nbsp;
  ${uiLabelMap.WebtoolsSecretUsageTotalMisses}: <strong>${usageSummary.totalMisses!0}</strong> &nbsp;|&nbsp;
  ${uiLabelMap.WebtoolsSecretUsageTotalLookups}: <strong>${usageSummary.totalLookups!0}</strong> &nbsp;|&nbsp;
  ${uiLabelMap.WebtoolsSecretUsageCachedKeys}: <strong>${usageSummary.cachedKeys!0}</strong>
</p>

<#if usageReportRows?has_content>
  <table class="basic-table hover-bar" cellspacing="0">
    <thead>
      <tr class="header-row">
        <td>${uiLabelMap.WebtoolsSecretUsageKey}</td>
        <td>${uiLabelMap.WebtoolsSecretUsageHits}</td>
        <td>${uiLabelMap.WebtoolsSecretUsageMisses}</td>
        <td>${uiLabelMap.WebtoolsSecretUsageTotal}</td>
        <td>${uiLabelMap.WebtoolsSecretUsageLastAccessed}</td>
      </tr>
    </thead>
    <tbody>
      <#list usageReportRows as row>
        <tr>
          <td title="${row[0]}">${row[0]}</td>
          <td>${row[1]}</td>
          <td>${row[2]}</td>
          <td>${row[3]}</td>
          <td>${row[4]}</td>
        </tr>
      </#list>
    </tbody>
  </table>
<#else>
  <p>${uiLabelMap.WebtoolsSecretUsageNoStats}</p>
</#if>

<hr/>

<p>${uiLabelMap.WebtoolsSecretUsageStatsResetInfo}</p>
<form class="basic-form" method="post" action="<@ofbizUrl>resetSecretUsageStats</@ofbizUrl>">
  <input type="submit" value="${uiLabelMap.WebtoolsSecretUsageStatsReset}"/>
</form>
