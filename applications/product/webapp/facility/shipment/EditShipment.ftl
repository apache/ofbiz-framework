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

<#-- This is now done through ECAs (see secas_shipment.xml), but we may want to allow manual usage in the future too, so leaving commented just in case
<#if (shipment.primaryOrderId)?has_content>
    <div><a href="<@ofbizUrl>setShipmentSettingsFromPrimaryOrder?shipmentId=${shipmentId}</@ofbizUrl>" class="buttontext">${uiLabelMap.ProductSettingsFromPrimaryOrder} [${shipment.primaryOrderId}]</a></div>
</#if>
-->
<#if shipmentId?has_content>
    <div><a href="<@ofbizUrl>ShipmentManifestReport.pdf?shipmentId=${shipmentId}</@ofbizUrl>" class="buttontext" target="_blank">${uiLabelMap.ProductGenerateShipmentManifestReport}</a></div>
</#if>

${editShipmentWrapper.renderFormString(context)}
