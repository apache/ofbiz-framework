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
<#assign unselectedLeftClassName = "headerButtonLeft">
<#assign unselectedRightClassName = "headerButtonRight">
<#assign selectedLeftClassMap = {(page.headerItem)?default("void") : "headerButtonLeftSelected"}>
<#assign selectedRightClassMap = {(page.headerItem)?default("void") : "headerButtonRightSelected"}>

<div class="apptitle">&nbsp;${uiLabelMap.ProductFacilityManagerApplication}&nbsp;</div>
<div class="row"> 
  <div class="col"><a href="<@ofbizUrl>main</@ofbizUrl>" class="${selectedLeftClassMap.main?default(unselectedLeftClassName)}">${uiLabelMap.ProductMain}</a></div>
  <div class="col"><a href="<@ofbizUrl>FindFacility</@ofbizUrl>" class="${selectedLeftClassMap.facility?default(unselectedLeftClassName)}">${uiLabelMap.ProductFacilities}</a></div> 
  <div class="col"><a href="<@ofbizUrl>FindFacilityGroup</@ofbizUrl>" class="${selectedLeftClassMap.facilityGroup?default(unselectedLeftClassName)}">${uiLabelMap.ProductFacilityGroups}</a></div>
  <div class="col"><a href="<@ofbizUrl>FindShipment</@ofbizUrl>" class="${selectedLeftClassMap.shipment?default(unselectedLeftClassName)}">${uiLabelMap.ProductShipments}</a></div> 
  
  <#if userLogin?has_content>
    <div class="col-right"><a href="<@ofbizUrl>logout</@ofbizUrl>" class="${selectedRightClassMap.logout?default(unselectedRightClassName)}">${uiLabelMap.CommonLogout}</a></div>
  <#else>
    <div class="col-right"><a href='<@ofbizUrl>${checkLoginUrl?if_exists}</@ofbizUrl>' class='${selectedRightClassMap.login?default(unselectedRightClassName)}'>${uiLabelMap.CommonLogin}</a></div>
  </#if>
  <#if facilityId?has_content>
    <div class="col-right"><a href="<@ofbizUrl>InventoryReports?facilityId=${facilityId}&action=SEARCH</@ofbizUrl>" class="${selectedRightClassMap.reports?default(unselectedRightClassName)}">${uiLabelMap.CommonReports}</a></div>  
  </#if>
  <div class="col-fill">&nbsp;</div>
</div>

