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

    <div class="head1">${uiLabelMap.ProductFacilityGroup}<span class="head2">&nbsp;<#if facilityGroup?exists>${(facilityGroup.facilityGroupName)?if_exists}</#if> [${uiLabelMap.CommonId}:${facilityGroupId?if_exists}]</span></div>
    <a href="<@ofbizUrl>EditFacilityGroup</@ofbizUrl>" class="buttontext">[${uiLabelMap.ProductNewGroup}]</a>
    
    <#if !(facilityGroup?exists)>
        <#if facilityGroupId?exists>
            <form action="<@ofbizUrl>CreateFacilityGroup</@ofbizUrl>" method="post" style="margin: 0;">
            <table border="0" cellpadding="2" cellspacing="0">
            <h3>${uiLabelMap.ProductCouldNotFindFacilityWithId} "${facilityGroupId}".</h3>
        <#else>
            <form action="<@ofbizUrl>CreateFacilityGroup</@ofbizUrl>" method="post" style="margin: 0;">
            <table border="0" cellpadding="2" cellspacing="0">
        </#if>
    <#else>
        <form action="<@ofbizUrl>UpdateFacilityGroup</@ofbizUrl>" method="post" style="margin: 0;">
        <table border="0" cellpadding="2" cellspacing="0">
        <input type="hidden" name="facilityGroupId" value="${facilityGroupId?if_exists}">
        <tr>
            <td align="right"><div class="tabletext">${uiLabelMap.ProductFacilityGroupId}</div></td>
            <td>&nbsp;</td>
            <td>
            <b>${facilityGroupId?if_exists}</b> ${uiLabelMap.ProductNotModificationRecrationFacilityGroup}
            </td>
        </tr>
    </#if>
    <tr>
        <td width="26%" align="right"><div class="tabletext">${uiLabelMap.ProductFacilityGroupType}</div></td>
        <td>&nbsp;</td>
        <td width="74%">
        <select name="facilityGroupTypeId" size="1" class="selectBox">
            <option selected value="${(facilityGroupType.facilityGroupTypeId)?if_exists}">${(facilityGroupType.get("description",locale))?if_exists}</option>
            <#list facilityGroupTypes as nextFacilityGroupType>
            <option value="${(nextFacilityGroupType.facilityGroupTypeId)?if_exists}">${(nextFacilityGroupType.get("description",locale))?if_exists}</option>
            </#list>
        </select>
        </td>
    </tr>

    <tr>
        <td width="26%" align="right"><div class="tabletext">${uiLabelMap.ProductPrimaryParentGroup}</div></td>
        <td>&nbsp;</td>
        <td width="74%">
        <select name="primaryParentGroupId" size="1" class="selectBox">
            <#if facilityGroup?exists> 
                <#if (facilityGroup.getRelatedOne("PrimaryParentFacilityGroup"))?exists>
                    <#assign currentPrimaryParent = facilityGroup.getRelatedOne("PrimaryParentFacilityGroup")>
                    <option selected value="${(facilityGroup.primaryParentGroupId)?if_exists}">${(currentPrimaryParent.description)?if_exists}</option>
                </#if>
            </#if> 
            <option value="${(facilityGroup.primaryParentGroupId)?if_exists}"></option>            
            <#list facilityGroups as nextFacilityGroup>
            <#if !(nextFacilityGroup.facilityGroupId.equals("_NA_"))>
                <option value="${(nextFacilityGroup.facilityGroupId)?if_exists}">${(nextFacilityGroup.facilityGroupName)?if_exists}</option>
            </#if>
            </#list>
        </select>
        </td>
    </tr>

    <tr>
        <td width="26%" align="right"><div class="tabletext">${uiLabelMap.ProductName}</div></td>
        <td>&nbsp;</td>
        <td width="74%"><input type="text" class="inputBox" name="facilityGroupName" value="${(facilityGroup.facilityGroupName)?if_exists}" size="30" maxlength="60"></td>
    </tr>    
    <tr>
        <td width="26%" align="right"><div class="tabletext">${uiLabelMap.CommonDescription}</div></td>
        <td>&nbsp;</td>
        <td width="74%"><input type="text" class="inputBox" name="description" value="${(facilityGroup.description)?if_exists}" size="60" maxlength="250"></td>
    </tr>
    <tr>
        <td colspan="2">&nbsp;</td>
        <td colspan="1" align="left"><input type="submit" name="Update" value="${uiLabelMap.CommonUpdate}"></td>
    </tr>
    </table>
    </form>
