<#--
 *  Copyright (c) 2003 The Open For Business Project - www.ofbiz.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a 
 *  copy of this software and associated documentation files (the "Software"), 
 *  to deal in the Software without restriction, including without limitation 
 *  the rights to use, copy, modify, merge, publish, distribute, sublicense, 
 *  and/or sell copies of the Software, and to permit persons to whom the 
 *  Software is furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included 
 *  in all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS 
 *  OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF 
 *  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. 
 *  IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY 
 *  CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT 
 *  OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR 
 *  THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 *@author     David E. Jones
 *@author     Brad Steiner
 *@author     thierry.grauss@etu.univ-tours.fr (migration to uiLabelMap)
 *@version    $Rev$
 *@since      2.2
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
