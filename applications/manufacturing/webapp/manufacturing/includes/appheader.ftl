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
<#assign selectedLeftClassMap = {page.headerItem?default("void") : "headerButtonLeftSelected"}>
<#assign selectedRightClassMap = {page.headerItem?default("void") : "headerButtonRightSelected"}>

<div class="apptitle">&nbsp;${uiLabelMap.ManufacturingManagerApplication}&nbsp;</div>
<div class="row">
  <#if security.hasEntityPermission("MANUFACTURING", "_CREATE", session)>
    <div class="col"><a href="<@ofbizUrl>FindProductionRun</@ofbizUrl>" class="${selectedLeftClassMap.jobshop?default(unselectedLeftClassName)}">${uiLabelMap.ManufacturingJobShop}</a></div>
    <div class="col"><a href="<@ofbizUrl>FindRouting</@ofbizUrl>" class="${selectedLeftClassMap.routing?default(unselectedLeftClassName)}">${uiLabelMap.ManufacturingRouting}</a></div>
    <div class="col"><a href="<@ofbizUrl>FindRoutingTask</@ofbizUrl>" class="${selectedLeftClassMap.routingTask?default(unselectedLeftClassName)}">${uiLabelMap.ManufacturingRoutingTask}</a></div>
    <div class="col"><a href="<@ofbizUrl>FindCalendar</@ofbizUrl>" class="${selectedLeftClassMap.calendar?default(unselectedLeftClassName)}">${uiLabelMap.ManufacturingCalendar}</a></div>
    <div class="col"><a href="<@ofbizUrl>EditCostCalcs</@ofbizUrl>" class="${selectedLeftClassMap.costs?default(unselectedLeftClassName)}">${uiLabelMap.ManufacturingCostCalcs}</a></div>
    <div class="col"><a href="<@ofbizUrl>BomSimulation</@ofbizUrl>" class="${selectedLeftClassMap.bom?default(unselectedLeftClassName)}">${uiLabelMap.ManufacturingBillOfMaterials}</a></div>
    <div class="col"><a href="<@ofbizUrl>FindInventoryEventPlan</@ofbizUrl>" class="${selectedLeftClassMap.mrp?default(unselectedLeftClassName)}">${uiLabelMap.ManufacturingMrp}</a></div>
    <div class="col"><a href="<@ofbizUrl>WorkWithShipmentPlans</@ofbizUrl>" class="${selectedLeftClassMap.ShipmentPlans?default(unselectedLeftClassName)}">${uiLabelMap.ManufacturingShipmentPlans}</a></div>
    <div class="col"><a href="<@ofbizUrl>ManufacturingReports</@ofbizUrl>" class="${selectedLeftClassMap.ManufacturingReports?default(unselectedLeftClassName)}">${uiLabelMap.ManufacturingReports}</a></div>
  </#if>  
  <#if userLogin?has_content>
    <div class="col-right"><a href="<@ofbizUrl>logout</@ofbizUrl>" class="${selectedRightClassMap.logout?default(unselectedRightClassName)}">${uiLabelMap.CommonLogout}</a></div>
  <#else>
    <div class="col-right"><a href='<@ofbizUrl>${checkLoginUrl?if_exists}</@ofbizUrl>' class='${selectedRightClassMap.login?default(unselectedRightClassName)}'>${uiLabelMap.CommonLogin}</a></div>
  </#if>
  <div class="col-fill">&nbsp;</div>
</div>
