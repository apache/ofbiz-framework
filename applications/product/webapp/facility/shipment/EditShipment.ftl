<#--
 *  Copyright (c) 2003-2005 The Open For Business Project - www.ofbiz.org
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

<#-- This is now done through ECAs (see secas_shipment.xml), but we may want to allow manual usage in the future too, so leaving commented just in case
<#if (shipment.primaryOrderId)?has_content>
    <div><a href="<@ofbizUrl>setShipmentSettingsFromPrimaryOrder?shipmentId=${shipmentId}</@ofbizUrl>" class="buttontext">${uiLabelMap.ProductSettingsFromPrimaryOrder} [${shipment.primaryOrderId}]</a></div>
</#if>
-->
<#if shipmentId?has_content>
    <div><a href="<@ofbizUrl>ShipmentManifestReport.pdf?shipmentId=${shipmentId}</@ofbizUrl>" class="buttontext" target="_blank">${uiLabelMap.ProductGenerateShipmentManifestReport}</a></div>
</#if>

${editShipmentWrapper.renderFormString(context)}
