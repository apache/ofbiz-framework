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
    
    <div class="head1">${uiLabelMap.PartyRoleFor} <span class="head2"><#if facility?exists>${(facility.facilityName)?if_exists}</#if> [${uiLabelMap.CommonId}:${facilityId?if_exists}]</span></div>
    <a href="<@ofbizUrl>EditFacility</@ofbizUrl>" class="buttontext">[${uiLabelMap.ProductNewFacility}]</a>
    <p>
    
    <p class="head2">${uiLabelMap.ProductFacilityRoleMemberMaintenance}</p>
    <table border="1" cellpadding="2" cellspacing="0">
    <tr>
        <td><div class="tabletext"><b>${uiLabelMap.PartyPartyId}</b></div></td>
        <td><div class="tabletext"><b>${uiLabelMap.PartyRoleType}</b></div></td>  
        <td><div class="tabletext"><b>&nbsp;</b></div></td>
    </tr>
    
    <#list facilityRoles as facilityRole>  
    <#assign roleType = facilityRole.getRelatedOne("RoleType")>
    <tr valign="middle">
        <td><a href="/partymgr/control/viewprofile?party_id=${(facilityRole.partyId)?if_exists}" class="buttontext">${(facilityRole.partyId)?if_exists}</a></td>    
        <td><div class="tabletext">${(roleType.get("description",locale))?if_exists}</div></td>    
        <td align="center">
        <a href="<@ofbizUrl>removePartyFromFacility?facilityId=${(facilityRole.facilityId)?if_exists}&partyId=${(facilityRole.partyId)?if_exists}&roleTypeId=${(facilityRole.roleTypeId)?if_exists}</@ofbizUrl>" class="buttontext">
        [${uiLabelMap.CommonDelete}]</a>
        </td>
    </tr>
    </#list>
    </table>
    
    <br/>
    <form method="post" action="<@ofbizUrl>addPartyToFacility</@ofbizUrl>" style="margin: 0;">
    <input type="hidden" name="facilityId" value="${facilityId}">  
    <div class="head2">${uiLabelMap.ProductAddFacilityPartyRole}:</div>
    <div class="tabletext">
        ${uiLabelMap.PartyPartyId}: <input type="text" class="inputBox" size="20" name="partyId">
        ${uiLabelMap.PartyRoleType}:
        <select name="roleTypeId" class="selectBox"><option></option>
        <#list roles as role>
            <option value="${(role.roleTypeId)?if_exists}">${(role.get("description",locale))?if_exists}</option>
        </#list>
        </select>
        <input type="submit" value="${uiLabelMap.CommonAdd}">
    </div>
    </form>
