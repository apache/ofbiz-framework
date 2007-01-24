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

<div class="head2">Process Detail List</div>
<#if processes?has_content>
  <#assign proc1 = processes[0]>
  <div>&nbsp;</div>
  <div class="tabletext"><b>Process :</b> ${proc1.name()} - "${proc1.description()?default("N/A")}"</div>

  <table cellpadding="2" cellspacing="0" border="1">
    <tr>
      <td><div class="tableheadtext">ID</div></td>
      <td><div class="tableheadtext">State</div></td>
      <td><div class="tableheadtext">Priority</div></td>
      <td><div class="tableheadtext">Steps</div></td>
      <td>&nbsp;</td>
      <td>&nbsp;</td>
      <td>&nbsp;</td>
      <td>&nbsp;</td>
      <td>&nbsp;</td>
    </tr>
    <#list processes as proc>
      <tr>
        <td align="left"><div class="tabletext">${proc.key()}</div></td>
        <td align="left"><div class="tabletext">${proc.state()}</div></td>
        <td align="center"><div class="tabletext">${proc.priority()}</div></td>
        <td align="center"><div class="tabletext">${proc.how_many_step()}</div></td>
        <#if proc.state() != "open.not_running.not_started">
          <td align="center"><a href="<@ofbizUrl>processHistory?process=${proc.key()}</@ofbizUrl>" class="buttontext">History</a></td>
        <#else>
          <td>&nbsp;</td>
        </#if>
        <#if proc.state() == "open.running">
          <td align="center"><a href="<@ofbizUrl>processDetailList?manager=${manager?replace("#", "%23")?if_exists}&terminate=${proc.key()}</@ofbizUrl>" class="buttontext">Terminate</a></td>
          <td align="center"><a href="<@ofbizUrl>processDetailList?manager=${manager?replace("#", "%23")?if_exists}&abort=${proc.key()}</@ofbizUrl>" class="buttontext">Abort</a></td>
          <td align="center"><a href="<@ofbizUrl>processDetailList?manager=${manager?replace("#", "%23")?if_exists}&suspend=${proc.key()}</@ofbizUrl>" class="buttontext">Suspend</a></td>
        <#else>
          <td>&nbsp;</td>
          <td>&nbsp;</td>
          <#if proc.state() == "open.not_running.not_started">
            <td align="center"><a href="<@ofbizUrl>processDetailList?manager=${manager?replace("#", "%23")?if_exists}&start=${proc.key()}</@ofbizUrl>" class="buttontext">Start</a></td>
            <td align="center"><a href="<@ofbizUrl>processDetailList?manager=${manager?replace("#", "%23")?if_exists}&delete=${proc.key()}</@ofbizUrl>" class="buttontext">Delete</a></td>
          <#elseif proc.state() == "open.not_running.suspended">
            <td align="center"><a href="<@ofbizUrl>processDetailList?manager=${manager?replace("#", "%23")?if_exists}&resume=${proc.key()}</@ofbizUrl>" class="buttontext">Resume</a></td>
            <td align="center"><a href="<@ofbizUrl>processDetailList?manager=${manager?replace("#", "%23")?if_exists}&delete=${proc.key()}</@ofbizUrl>" class="buttontext">Delete</a></td>
          <#else>
            <td>&nbsp;</td>
          </#if>
        </#if>
        <td align="center"><a href="<@ofbizUrl>processSteps?process=${proc.key()}</@ofbizUrl>" class="buttontext">Activities</a></td>
      </tr>
    </#list>
  </table>
<#else>
  <div class="tabletext">No running processes.</div>
</#if>
