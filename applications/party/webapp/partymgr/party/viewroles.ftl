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

<#-- Party Roles -->
<!-- begin viewroles.ftl -->
<br/>
<div id="partyRoles" class="screenlet">
  <div class="screenlet-title-bar">
    <h3>${uiLabelMap.PartyMemberRoles}</h3>
  </div>
  <div class="screenlet-body">
    <table cellspacing="0" class="basic-table">
      <#if partyRoles?has_content>
        <#list partyRoles as userRole>
          <tr>
            <td class="label">${uiLabelMap.PartyRole}</td>
            <td>${userRole.get("description",locale)} [${userRole.roleTypeId}]</td>
            <#if hasDeletePermission>
              <td class="button-col align-float">
                <a href="<@ofbizUrl>deleterole?partyId=${partyId}&roleTypeId=${userRole.roleTypeId}</@ofbizUrl>">${uiLabelMap.CommonRemove}</a>&nbsp;
              </td>
            <#else>
              <td>&nbsp;</td>
            </#if>
          </tr>
        </#list>
      <#else>
        ${uiLabelMap.PartyNoPartyRolesFound}
      </#if>
      <#if hasUpdatePermission>
        <tr><td colspan="3"><hr></td></tr>
        <tr>
          <td class="label">
            ${uiLabelMap.PartyAddToRole}
          </td>
          <td>
            <form name="addPartyRole" method="post" action="<@ofbizUrl>addrole/viewroles</@ofbizUrl>">
              <input type="hidden" name="partyId" value="${partyId}"/>
              <select name="roleTypeId">
                <#list roles as role>
                  <option value="${role.roleTypeId}">${role.get("description",locale)}</option>
                </#list>
              </select>
              <a href="javascript:document.addPartyRole.submit()" class="smallSubmit">${uiLabelMap.CommonAdd}</a>
            </form>
          </td>
          <td>&nbsp;</td>
        </tr>
      </#if>
    </table>
  </div>
</div>

<#-- Add role type -->
<#if hasCreatePermission>
<br/>
<div id="newRoleType" class="screenlet">
  <div class="screenlet-title-bar">
    <h3>${uiLabelMap.PartyNewRoleType}</h3>
  </div>
  <div class="screenlet-body">
    <form method="post" action="<@ofbizUrl>createroletype</@ofbizUrl>" name="createroleform">
      <input type='hidden' name='party_id' value='${partyId}'>
      <input type='hidden' name='partyId' value='${partyId}'>
      <table cellspacing="0" class="basic-table">
        <tr>
          <td class="label">${uiLabelMap.PartyRoleTypeId}</td>
          <td>
            <input type="text" name="roleTypeId" size="20" class="required"><span class="tooltip">${uiLabelMap.CommonRequired}</span>
          </td>
        <tr>
          <td class="label">${uiLabelMap.CommonDescription}</td>
          <td>
            <input type="text" name="description" size="30" class="required"><span class="tooltip">${uiLabelMap.CommonRequired}</span>
          </td>
        </tr>
        <tr>
          <td>&nbsp;</td>
          <td><a href="javascript:document.createroleform.submit()" class="smallSubmit">${uiLabelMap.CommonSave}</a></td>
      </table>
    </form>
  </div>
</div>
</#if>
