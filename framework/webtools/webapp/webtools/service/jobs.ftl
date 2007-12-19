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
    <h3>${uiLabelMap.PageTitleJobList}</h3>
  </div>
  <table class="basic-table hover-bar" cellspacing="0">
    <tr class="header-row">
      <td>${uiLabelMap.WebtoolsJob}</td>
      <td>${uiLabelMap.CommonId}</td>
      <td>${uiLabelMap.CommonStatus}</td>
      <td>${uiLabelMap.WebtoolsPool}</td>
      <td>${uiLabelMap.WebtoolsRunTime}</td>
      <td>${uiLabelMap.CommonStartDateTime}</td>
      <td>${uiLabelMap.WebtoolsService}</td>
      <td>${uiLabelMap.CommonEndDateTime}</td>
      <td>&nbsp;</td>
    </tr>
    <#assign alt_row = false>
    <#list jobs as job>
      <#assign statusItem = job.getRelatedOne("StatusItem")?if_exists/>
      <tr<#if alt_row> class="alternate-row"</#if>>
        <td>${job.jobName?if_exists}&nbsp;</td>
        <td>${job.jobId?if_exists}&nbsp;</td>
        <td>${(statusItem.description)?if_exists}</td>
        <td>${job.poolId?if_exists}&nbsp;</td>
        <td>${job.runTime?if_exists}&nbsp;</td>
        <td>${job.startDateTime?if_exists}&nbsp;</td>
        <td class="button-col"><a href="<@ofbizUrl>availableServices?sel_service_name=${job.serviceName?if_exists}</@ofbizUrl>">${job.serviceName?if_exists}</a></td>
        <#if job.cancelDateTime?exists>
          <td class="alert">${job.cancelDateTime}
        <#else>
          <td>${job.finishDateTime?if_exists}
        </#if>
        </td>
        <td class="button-col">
          <#if (job.statusId?has_content && (job.statusId == 'SERVICE_RUNNING' || job.statusId == 'SERVICE_QUEUED'))>
            <a href="<@ofbizUrl>resetJob?jobId=${job.jobId}</@ofbizUrl>">${uiLabelMap.WebtoolsResetJob}</a>
          <#elseif (!job.statusId?has_content || job.statusId == 'SERVICE_PENDING')>
            <a href="<@ofbizUrl>cancelJob?jobId=${job.jobId}</@ofbizUrl>">${uiLabelMap.WebtoolsCancelJob}</a>
          <#else>
            &nbsp;
          </#if>
        </td>
      </tr>
      <#assign alt_row = !alt_row>
    </#list>
  </table>
</div>
