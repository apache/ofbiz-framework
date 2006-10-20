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
    <td><div class="tableheadtext">${uiLabelMap.WebtoolsThread}</div></td>
    <td><div class="tableheadtext">${uiLabelMap.CommonStatus}</div></td>
    <td><div class="tableheadtext">${uiLabelMap.WebtoolsJob}</div></td>
    <td><div class="tableheadtext">${uiLabelMap.WebtoolsService}</div></td>
    <td><div class="tableheadtext">${uiLabelMap.CommonTime} (ms)</div></td>
  </tr>
  <#list threads as thread>
  <tr>
    <td><div class="tabletext">${thread.threadName?if_exists}&nbsp;</div></td>
    <td><div class="tabletext">${thread.status?if_exists}&nbsp;</div></td>
    <td><div class="tabletext">${thread.jobName?default("[${uiLabelMap.CommonNone}]")}</div></td>
    <td><div class="tabletext">${thread.serviceName?default("[${uiLabelMap.CommonNone}]")}</div></td>
    <td><div class="tabletext">${thread.runTime?if_exists}&nbsp;</div></td>
  </tr>
  </#list>
</table>
