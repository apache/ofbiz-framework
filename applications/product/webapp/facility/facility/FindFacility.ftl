<#--

Copyright 2001-2006 The Apache Software Foundation

Licensed under the Apache License, Version 2.0 (the "License"); you may not
use this file except in compliance with the License. You may obtain a copy of
the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
License for the specific language governing permissions and limitations
under the License.
-->

<#if security.hasEntityPermission("FACILITY", "_VIEW", session)>

<div class="head1">${uiLabelMap.ProductFacilitiesList}</div>

<div><a href='<@ofbizUrl>EditFacility</@ofbizUrl>' class="buttontext">${uiLabelMap.ProductCreateNewFacility}</a></div>
<br/>
<table border="1" cellpadding='2' cellspacing='0'>
  <tr>
    <td><div class="tabletext"><b>${uiLabelMap.ProductFacilityNameId}</b></div></td>
    <td><div class="tabletext"><b>${uiLabelMap.ProductFacilityType}</b></div></td>
    <td><div class="tabletext"><b>${uiLabelMap.ProductFacilityOwner}</b></div></td>
    <td><div class="tabletext"><b>${uiLabelMap.ProductSqFt}</b></div></td>
    <td><div class="tabletext"><b>${uiLabelMap.ProductDescription}</b></div></td>
    <td><div class="tabletext">&nbsp;</div></td>
  </tr>
<#list facilities as facility>
  <#assign facilityType = facility.getRelatedOne("FacilityType")?if_exists>
  <tr valign="middle">
    <td><div class='tabletext'>&nbsp;${facility.facilityName?if_exists} <a href='<@ofbizUrl>EditFacility?facilityId=${facility.facilityId?if_exists}</@ofbizUrl>' class="buttontext">${facility.facilityId?if_exists}</a></div></td>
    <td><div class='tabletext'>&nbsp;${facilityType.get("description",locale)?if_exists}</div></td>
    <td><div class='tabletext'>&nbsp;${facility.ownerPartyId?if_exists}</div></td>
    <td><div class='tabletext'>&nbsp;${facility.squareFootage?if_exists}</div></td>
    <td><div class='tabletext'>&nbsp;${facility.description?if_exists}</div></td>
    <td>
      <a href='<@ofbizUrl>EditFacility?facilityId=${facility.facilityId?if_exists}</@ofbizUrl>' class="buttontext">${uiLabelMap.CommonEdit}</a>
    </td>
  </tr>
</#list>
</table>
<br/>

<#else>
  <h3>${uiLabelMap.ProductFacilityViewPermissionError}</h3>
</#if>

