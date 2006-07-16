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
<#assign unselectedClassName = "tabButton">
<#assign selectedClassMap = {page.tabButtonItem?default("void") : "tabButtonSelected"}>

<#if facilityId?has_content>
  <div class='tabContainer'>
    <a href="<@ofbizUrl>EditFacility?facilityId=${facilityId}</@ofbizUrl>" class="${selectedClassMap.EditFacility?default(unselectedClassName)}">${uiLabelMap.ProductFacility}</a>
    <a href="<@ofbizUrl>ViewContactMechs?facilityId=${facilityId}</@ofbizUrl>" class="${selectedClassMap.ViewContactMechs?default(unselectedClassName)}">${uiLabelMap.PartyContactMechs}</a>
    <a href="<@ofbizUrl>EditFacilityGroups?facilityId=${facilityId}</@ofbizUrl>" class="${selectedClassMap.EditFacilityGroups?default(unselectedClassName)}">${uiLabelMap.ProductGroups}</a>
    <a href="<@ofbizUrl>FindFacilityLocation?facilityId=${facilityId}</@ofbizUrl>" class="${selectedClassMap.FindFacilityLocation?default(unselectedClassName)}">${uiLabelMap.ProductLocations}</a>
    <a href="<@ofbizUrl>EditFacilityRoles?facilityId=${facilityId}</@ofbizUrl>" class="${selectedClassMap.EditFacilityRoles?default(unselectedClassName)}">${uiLabelMap.PartyRoles}</a>
    <a href="<@ofbizUrl>ViewFacilityInventoryByProduct?facilityId=${facilityId}</@ofbizUrl>" class="${selectedClassMap.EditFacilityInventoryItems?default(unselectedClassName)}">${uiLabelMap.ProductInventory}</a>
    <a href="<@ofbizUrl>ReceiveInventory?facilityId=${facilityId}</@ofbizUrl>" class="${selectedClassMap.ReceiveInventory?default(unselectedClassName)}">${uiLabelMap.ProductInventoryReceive}</a>
    <a href="<@ofbizUrl>FindFacilityTransfers?facilityId=${facilityId}</@ofbizUrl>" class="${selectedClassMap.FindFacilityTransfers?default(unselectedClassName)}">${uiLabelMap.ProductInventoryXfers}</a>
    <a href="<@ofbizUrl>ReceiveReturn?facilityId=${facilityId}</@ofbizUrl>" class="${selectedClassMap.ReceiveReturn?default(unselectedClassName)}">${uiLabelMap.ProductReceiveReturn}</a>
    <a href="<@ofbizUrl>PicklistOptions?facilityId=${facilityId}</@ofbizUrl>" class="${selectedClassMap.PicklistOptions?default(unselectedClassName)}">${uiLabelMap.ProductPicking}</a>
    <a href="<@ofbizUrl>PackOrder?facilityId=${facilityId}</@ofbizUrl>" class="${selectedClassMap.PackOrder?default(unselectedClassName)}">${uiLabelMap.ProductPacking}</a>
    <a href="<@ofbizUrl>Scheduling?facilityId=${facilityId}</@ofbizUrl>" class="${selectedClassMap.Scheduling?default(unselectedClassName)}">${uiLabelMap.ProductScheduling}</a>
    <a href="<@ofbizUrl>FindShipment?destinationFacilityId=${facilityId}&amp;lookupFlag=Y</@ofbizUrl>" class="${selectedClassMap.FindShipment?default(unselectedClassName)}">${uiLabelMap.ProductIncomingShipments}</a>
    <a href="<@ofbizUrl>FindShipment?originFacilityId=${facilityId}&amp;lookupFlag=Y</@ofbizUrl>" class="${selectedClassMap.FindShipment?default(unselectedClassName)}">${uiLabelMap.ProductOutgoingShipments}</a>
  </div>
</#if>
