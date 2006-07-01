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
 *@author     Catherine.Heintz@nereide.biz (migration to UiLabel)
 *@version    $Rev$
 *@since      2.2
-->
<#if requestAttributes.uiLabelMap?exists>
<#assign uiLabelMap = requestAttributes.uiLabelMap>
</#if>
<#assign unselectedClassName = "tabButton">
<#assign selectedClassMap = {page.tabButtonItem?default("void") : "tabButtonSelected"}>

<#if shipmentId?has_content>
  <div class='tabContainer'>
    <a href="<@ofbizUrl>ViewShipment?shipmentId=${shipmentId}</@ofbizUrl>" class="${selectedClassMap.ViewShipment?default(unselectedClassName)}">${uiLabelMap.CommonView}</a>
    <a href="<@ofbizUrl>EditShipment?shipmentId=${shipmentId}</@ofbizUrl>" class="${selectedClassMap.EditShipment?default(unselectedClassName)}">${uiLabelMap.CommonEdit}</a>
    <#if shipment.shipmentTypeId?exists && shipment.shipmentTypeId='SALES_SHIPMENT'>
      <a href="<@ofbizUrl>EditShipmentPlan?shipmentId=${shipmentId}</@ofbizUrl>" class="${selectedClassMap.EditShipmentPlan?default(unselectedClassName)}">${uiLabelMap.ProductShipmentPlan}</a>
    </#if>
    <a href="<@ofbizUrl>AddItemsFromOrder?shipmentId=${shipmentId}</@ofbizUrl>" class="${selectedClassMap.AddItemsFromOrder?default(unselectedClassName)}">${uiLabelMap.ProductOrderItems}</a>
    <#if shipment.shipmentTypeId?exists && shipment.shipmentTypeId='PURCHASE_SHIPMENT'>
      <#if shipment.destinationFacilityId?exists>
        <a href="<@ofbizUrl>ReceiveInventory?shipmentId=${shipmentId}&facilityId=${shipment.destinationFacilityId?if_exists}<#if shipment.primaryOrderId?exists>&purchaseOrderId=${shipment.primaryOrderId}</#if></@ofbizUrl>" class="${selectedClassMap.ReceiveInventory?default(unselectedClassName)}">${uiLabelMap.ProductReceiveInventory}</a>
      </#if>
    <#else>
      <a href="<@ofbizUrl>EditShipmentItems?shipmentId=${shipmentId}</@ofbizUrl>" class="${selectedClassMap.EditShipmentItems?default(unselectedClassName)}">${uiLabelMap.ProductItems}</a>
      <a href="<@ofbizUrl>EditShipmentPackages?shipmentId=${shipmentId}</@ofbizUrl>" class="${selectedClassMap.EditShipmentPackages?default(unselectedClassName)}">${uiLabelMap.ProductPackages}</a>
      <a href="<@ofbizUrl>EditShipmentRouteSegments?shipmentId=${shipmentId}</@ofbizUrl>" class="${selectedClassMap.EditShipmentRouteSegments?default(unselectedClassName)}">${uiLabelMap.ProductRouteSegments}</a>
    </#if>
  </div>
</#if>
