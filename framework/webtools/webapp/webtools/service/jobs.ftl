<#--
Copyright 2001-2006 The Apache Software Foundation

Licensed under the Apache License, Version 2.0 (the "License"); you may not
use this file except in compliance with the License. You may obtain a copy of
the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
License for the specific language governing permissions and limitations
under the License.
-->

<table cellpadding="2" cellspacing="0" border="1" width="100%">
  <tr>
    <td><div class="tableheadtext">${uiLabelMap.WebtoolsJob}</div></td>
    <td><div class="tableheadtext">${uiLabelMap.WebtoolsPool}</div></td>
    <td><div class="tableheadtext">${uiLabelMap.WebtoolsRunTime}</div></td>
    <td><div class="tableheadtext">${uiLabelMap.CommonStartDateTime}</div></td>
    <td><div class="tableheadtext">${uiLabelMap.WebtoolsService}</div></td>
    <td><div class="tableheadtext">${uiLabelMap.CommonEndDateTime}</div></td>
    <td>&nbsp;</td>
  </tr>
  <#list jobs as job>
  <tr>
    <td><div class="tabletext">${job.jobName?if_exists}&nbsp;</td>
    <td><div class="tabletext">${job.poolId?if_exists}&nbsp;</div></td>
    <td><div class="tabletext">${job.runTime?if_exists}&nbsp;</div></td>
    <td><div class="tabletext">${job.startDateTime?if_exists}&nbsp;</div></td>
    <td><div class="tabletext"><a href="<@ofbizUrl>availableServices?sel_service_name=${job.serviceName?if_exists}</@ofbizUrl>" class="buttontext">${job.serviceName?if_exists}</a></div></td>
    <td>
      <div class="tabletext">
        <#if job.cancelDateTime?exists>
        <font color="red">${job.cancelDateTime}</font>
        <#else>
        ${job.finishDateTime?if_exists}
        </#if>
      </div>
    </td>
    <td align="center">
      <#if !(job.startDateTime?exists) && !(job.finishDateTime?exists) && !(job.cancelDateTime?exists)>
      <a href="<@ofbizUrl>cancelJob?jobId=${job.jobId}</@ofbizUrl>" class="buttontext">${uiLabelMap.WebtoolsCancelJob}</a>
      </#if>
      &nbsp;
    </td>
  </tr>
  </#list>
</table>
