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

<h2>Process Steps</h2>
<#if steps?has_content>
  <div>&nbsp;</div>
  <div><b>Process :</b> ${process.name()} - ${process.description()?default("N/A")} [${process.key()}]</div>

  <table cellpadding="2" cellspacing="0" border="1">
    <tr>
      <td><div>ID</div></td>
      <td><div>Name</div></td>
      <td><div>Description</div></td>
      <td><div>State</div></td>
      <td><div>Last State</div></td>
      <td><div>Priority</div></td>
      <td><div>Assignments</div></td>

    </tr>
    <#list steps as step>
      <#assign time = step.last_state_time().getTime()>
      <tr>
        <td><div>${step.key()}</div></td>
        <td><div>${step.name()}</div></td>
        <td><div>${step.description()}</div></td>
        <td><div>${step.state()}</div></td>
        <td><div>${Static["org.ofbiz.base.util.UtilDateTime"].getTimestamp(time)}</div></td>
        <td align="center"><div>${step.priority()}</div></td>
        <td align="center"><div>${step.how_many_assignment()}</div></td>
      </tr>
    </#list>
  </table>
<#else>
  <div>No running activities.</div>
</#if>
