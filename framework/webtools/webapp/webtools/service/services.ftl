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
    <h3>${uiLabelMap.PageTitleServiceList}</h3>
  </div>
  <table class="basic-table hover-bar" cellspacing="0">
    <tr class="header-row">
      <td>${uiLabelMap.WebtoolsServiceName}</td>
      <td>${uiLabelMap.WebtoolsDispatcherName}</td>
      <td>${uiLabelMap.WebtoolsMode}</td>
      <td>${uiLabelMap.CommonStartDateTime}</td>
      <td>${uiLabelMap.CommonEndDateTime}</td>
    </tr>
    <#assign alt_row = false>
    <#list services as service>
      <tr<#if alt_row> class="alternate-row"</#if>>
        <td>${service.serviceName?if_exists}</td>
        <td>${service.localName?if_exists}</td>
        <td>${service.modeStr?default("[none]")}</td>
        <td>${service.startTime?if_exists}</td>
        <td>${service.endTime?default("[running]")}</td>
      </tr>
      <#if alt_row>
        <#assign alt_row = false>
      <#else>
        <#assign alt_row = true>
      </#if>
    </#list>
  </table>
</div>
