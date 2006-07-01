<#--
$Id: processSteps.ftl 7426 2006-04-26 23:35:58Z jonesde $

Copyright 2004-2006 The Apache Software Foundation

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

<div class="head2">Process Steps</div>
<#if steps?has_content>
  <div>&nbsp;</div>
  <div class="tabletext"><b>Process :</b> ${process.name()} - ${process.description()?default("N/A")} [${process.key()}]</div>

  <table cellpadding="2" cellspacing="0" border="1">
    <tr>
      <td><div class="tableheadtext">ID</div></td>
      <td><div class="tableheadtext">Name</div></td>
      <td><div class="tableheadtext">Description</div></td>
      <td><div class="tableheadtext">State</div></td>
      <td><div class="tableheadtext">Last State</div></td>
      <td><div class="tableheadtext">Priority</div></td>
      <td><div class="tableheadtext">Assignments</div></td>

    </tr>
    <#list steps as step>
      <#assign time = step.last_state_time().getTime()>
      <tr>
        <td align="left"><div class="tabletext">${step.key()}</div></td>
        <td align="left"><div class="tabletext">${step.name()}</div></td>
        <td align="left"><div class="tabletext">${step.description()}</div></td>
        <td align="left"><div class="tabletext">${step.state()}</div></td>
        <td align="left"><div class="tabletext">${Static["org.ofbiz.base.util.UtilDateTime"].getTimestamp(time)}</div></td>
        <td align="center"><div class="tabletext">${step.priority()}</div></td>
        <td align="center"><div class="tabletext">${step.how_many_assignment()}</div></td>
      </tr>
    </#list>
  </table>
<#else>
  <div class="tabletext">No running activities.</div>
</#if>