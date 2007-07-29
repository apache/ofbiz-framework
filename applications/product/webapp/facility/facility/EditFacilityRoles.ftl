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
    
<span class="head1">${uiLabelMap.PartyRoleFor}</span> <span class="head2"><#if facility?exists>${(facility.facilityName)?if_exists}</#if> [${uiLabelMap.CommonId}:${facilityId?if_exists}]</span>
<div class="button-bar">
    <a href="<@ofbizUrl>EditFacility</@ofbizUrl>" class="buttontext">${uiLabelMap.ProductNewFacility}</a>
</div>

<div class="screenlet">
  <div class="screenlet-title-bar"><h3>${uiLabelMap.ProductFacilityRoleMemberMaintenance}</h3></div>
  <table class="basic-table" cellspacing="0">
    <tr class="header-row">
        <td>${uiLabelMap.PartyPartyId}</td>
        <td>${uiLabelMap.PartyRoleType}</td>  
        <td>&nbsp;</td>
    </tr>
    
    <#list facilityRoles as facilityRole>  
    <#assign roleType = facilityRole.getRelatedOne("RoleType")>
    <tr valign="middle">
        <td><a href="/partymgr/control/viewprofile?party_id=${(facilityRole.partyId)?if_exists}" class="buttontext">${(facilityRole.partyId)?if_exists}</a></td>    
        <td>${(roleType.get("description",locale))?if_exists}</td>    
        <td align="center">
        <a href="<@ofbizUrl>removePartyFromFacility?facilityId=${(facilityRole.facilityId)?if_exists}&partyId=${(facilityRole.partyId)?if_exists}&roleTypeId=${(facilityRole.roleTypeId)?if_exists}</@ofbizUrl>" class="buttontext">
        ${uiLabelMap.CommonDelete}</a>
        </td>
    </tr>
    </#list>
  </table>
</div>
    
    <br/>
    <h2>${uiLabelMap.ProductAddFacilityPartyRole}:</h2>
    <form method="post" action="<@ofbizUrl>addPartyToFacility</@ofbizUrl>">
        <input type="hidden" name="facilityId" value="${facilityId}">  
        ${uiLabelMap.PartyPartyId}: <input type="text" size="20" name="partyId">
        ${uiLabelMap.PartyRoleType}:
        <select name="roleTypeId">
            <option></option>
            <#list roles as role>
                <option value="${(role.roleTypeId)?if_exists}">${(role.get("description",locale))?if_exists}</option>
            </#list>
        </select>
        <input type="submit" value="${uiLabelMap.CommonAdd}">
    </form>
