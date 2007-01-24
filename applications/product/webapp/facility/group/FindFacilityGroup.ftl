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

<div class="head1">${uiLabelMap.ProductFacilityGroupList}</div>

<div><a href='<@ofbizUrl>EditFacilityGroup</@ofbizUrl>' class="buttontext">[${uiLabelMap.ProductNewGroup}]</a></div>
<br/>
<table border="1" cellpadding='2' cellspacing='0'>
  <tr>
    <td><div class="tabletext"><b>${uiLabelMap.ProductFacilityGroupNameId}</b></div></td>
    <td><div class="tabletext"><b>${uiLabelMap.ProductFacilityGroupType}</b></div></td>   
    <td><div class="tabletext"><b>${uiLabelMap.ProductDescription}</b></div></td>
    <td><div class="tabletext">&nbsp;</div></td>
  </tr>
<#list facilityGroups as facilityGroup>
  <#if facilityGroup.facilityGroupId?exists && facilityGroup.facilityGroupId != "_NA_">
    <#assign facilityGroupType = facilityGroup.getRelatedOne("FacilityGroupType")?if_exists>
    <tr valign="middle">
      <td><div class='tabletext'>&nbsp;<a href='<@ofbizUrl>EditFacilityGroup?facilityGroupId=${facilityGroup.facilityGroupId?if_exists}</@ofbizUrl>' class="buttontext">${facilityGroup.facilityGroupName?if_exists} [${facilityGroup.facilityGroupId?if_exists}]</a></div></td>
      <td><div class='tabletext'>&nbsp;${facilityGroupType.get("description",locale)?if_exists}</div></td>
      <td><div class='tabletext'>&nbsp;${facilityGroup.description?if_exists}</div></td>
      <td>
        <a href='<@ofbizUrl>EditFacilityGroup?facilityGroupId=${facilityGroup.facilityGroupId?if_exists}</@ofbizUrl>' class="buttontext">
        [${uiLabelMap.CommonEdit}]</a>
      </td>
    </tr>
  </#if>
</#list>
</table>

