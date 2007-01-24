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

<div class="head2">Process List</div>
<#if processMgrs?has_content>
  <div>&nbsp;</div>
  <table cellpadding="2" cellspacing="0" border="1">
    <tr>
      <td><div class="tableheadtext">Name</div></td>
      <td><div class="tableheadtext">Version</div></td>
      <td><div class="tableheadtext">Access</div></td>
      <td><div class="tableheadtext">Enabled</div></td>
      <td><div class="tableheadtext">Running</div></td>
      <td>&nbsp;</td>
      <td>&nbsp;</td>
      <td>&nbsp;</td>
    </tr>
    <#list processMgrs as mgr>
      <#assign state = mgr.process_mgr_state().value()>
      <tr>
        <td align="left"><div class="tabletext">${mgr.name()}</div></td>
        <td align="left"><div class="tabletext">${mgr.version()}</div></td>
        <td align="left"><div class="tabletext">${mgr.category()}</div></td>
        <td align="center"><div class="tabletext"><#if state == enabledCode>Y<#else>N</#if></div></td>
        <td align="center"><div class="tabletext">${mgr.how_many_process()}</div></td>
        <#if state == enabledCode>
          <td align="center"><a href="<@ofbizUrl>processes?disable=${mgr.name()?replace("#", "%23")}</@ofbizUrl>" class="buttontext">Disable</a></td>
        <#else>
          <td align="center"><a href="<@ofbizUrl>processes?enable=${mgr.name()?replace("#", "%23")}</@ofbizUrl>" class="buttontext">Enable</a></td>
        </#if>
        <td align="center">
          <#if (mgr.category()?upper_case == "PUBLIC" && enabledCode == state)>
            <a href="<@ofbizUrl>processes?create=${mgr.name()?replace("#", "%23")}</@ofbizUrl>" class="buttontext">Create</a>
          <#else>
            &nbsp;
          </#if>
        </td>
        <#if (mgr.how_many_process() > 0)>
          <td align="center"><a href="<@ofbizUrl>processDetailList?manager=${mgr.name()?replace("#", "%23")}</@ofbizUrl>" class="buttontext">View</a></td>
        <#else>
          <td>&nbsp;</td>
        </#if>
      </tr>
    </#list>
  </table>
  <td>&nbsp;</td>
    <a href="<@ofbizUrl>processes?deleteFinished</@ofbizUrl>" class="buttontext">Delete All Finished</a>
<#else>
  <div class="tabletext">No loaded processes.</div>
</#if>
