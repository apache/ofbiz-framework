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
 *@author     David E. Jones (jonesde@ofbiz.org)
 *@author     thierry.grauss@etu.univ-tours.fr (migration to uiLabelMap)
 *@version    $Rev$
 *@since      2.2
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
