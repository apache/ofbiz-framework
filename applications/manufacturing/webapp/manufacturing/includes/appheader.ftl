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

<#assign selected = headerItem?default("void")>

<div id="app-navigation">
  <h2>${uiLabelMap.ManufacturingManagerApplication}</h2>
  <ul>
    <#if security.hasEntityPermission("MANUFACTURING", "_CREATE", session)>
      <li<#if selected = "jobshop"> class="selected"</#if>><a href="<@ofbizUrl>FindProductionRun</@ofbizUrl>">${uiLabelMap.ManufacturingJobShop}</a></li>
      <li<#if selected = "routing"> class="selected"</#if>><a href="<@ofbizUrl>FindRouting</@ofbizUrl>">${uiLabelMap.ManufacturingRouting}</a></li>
      <li<#if selected = "routingTask"> class="selected"</#if>><a href="<@ofbizUrl>FindRoutingTask</@ofbizUrl>">${uiLabelMap.ManufacturingRoutingTask}</a></li>
      <li<#if selected = "calendar"> class="selected"</#if>><a href="<@ofbizUrl>FindCalendar</@ofbizUrl>">${uiLabelMap.ManufacturingCalendar}</a></li>
      <li<#if selected = "costs"> class="selected"</#if>><a href="<@ofbizUrl>EditCostCalcs</@ofbizUrl>">${uiLabelMap.ManufacturingCostCalcs}</a></li>
      <li<#if selected = "bom"> class="selected"</#if>><a href="<@ofbizUrl>BomSimulation</@ofbizUrl>">${uiLabelMap.ManufacturingBillOfMaterials}</a></li>
      <li<#if selected = "mrp"> class="selected"</#if>><a href="<@ofbizUrl>FindInventoryEventPlan</@ofbizUrl>">${uiLabelMap.ManufacturingMrp}</a></li>
      <li<#if selected = "ShipmentPlans"> class="selected"</#if>><a href="<@ofbizUrl>WorkWithShipmentPlans</@ofbizUrl>">${uiLabelMap.ManufacturingShipmentPlans}</a></li>
      <li<#if selected = "ManufacturingReports"> class="selected"</#if>><a href="<@ofbizUrl>ManufacturingReports</@ofbizUrl>">${uiLabelMap.ManufacturingReports}</a></li>
    </#if>  
    <#if userLogin?has_content>
      <li class="opposed"><a href="<@ofbizUrl>logout</@ofbizUrl>">${uiLabelMap.CommonLogout}</a></li>
    <#else>
      <li class="opposed"><a href="<@ofbizUrl>${checkLoginUrl?if_exists}</@ofbizUrl>">${uiLabelMap.CommonLogin}</a></li>
    </#if>
  </ul>
  <br class="clear" />
</div>
