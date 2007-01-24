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

<div class="head1">${uiLabelMap.PartyRoles} <span class="head2">${uiLabelMap.CommonFor} "${(facilityGroup.facilityGroupName)?if_exists}" [${uiLabelMap.CommonId} :${facilityGroupId?if_exists}]</span></div>
<a href="<@ofbizUrl>EditFacilityGroup</@ofbizUrl>" class="buttontext">[${uiLabelMap.ProductNewGroup}]</a>
<br/>
<br/>

<table border="1" cellpadding="2" cellspacing="0">
  <tr>
    <td><div class="tabletext"><b>${uiLabelMap.PartyPartyId}</b></div></td>
    <td><div class="tabletext"><b>${uiLabelMap.PartyRoleType}</b></div></td>  
    <td><div class="tabletext"><b>&nbsp;</b></div></td>
  </tr>

<#list facilityRoles as facilityGroupRole>
  <#assign roleType = facilityGroupRole.getRelatedOne("RoleType")?if_exists>
  <tr valign="middle">
    <td><a href="/partymgr/control/viewprofile?party_id=${facilityGroupRole.partyId}" class="buttontext">${facilityGroupRole.partyId}</a></td>    
    <td><div class="tabletext">${roleType.get("description",locale)}</div></td>
    <td align="center">
      <a href="<@ofbizUrl>removePartyFromFacilityGroup?facilityGroupId=${facilityGroupRole.facilityGroupId}&partyId=${facilityGroupRole.partyId}&roleTypeId=${facilityGroupRole.roleTypeId}</@ofbizUrl>" class="buttontext">
      [${uiLabelMap.CommonDelete}]</a>
    </td>
  </tr>
</#list>
</table>

<br/>
<form method="post" action="<@ofbizUrl>addPartyToFacilityGroup</@ofbizUrl>" style="margin: 0;">
  <input type="hidden" name="facilityGroupId" value="${facilityGroupId}">  
  <div class="head2">${uiLabelMap.ProductAddFacilityGroupPartyRole} :</div>
  <div class="tabletext">
    ${uiLabelMap.PartyPartyId} : <input type="text" class="inputBox" size="20" name="partyId">
    ${uiLabelMap.PartyRoleType} :
    <select name="roleTypeId" class="selectBox"><option></option>
      <#list roles as role>
        <option value="${role.roleTypeId}">${role.get("description",locale)}</option>
      </#list>
    </select>
    <input type="submit" value="${uiLabelMap.CommonAdd}">
  </div>
</form>

<br/>
