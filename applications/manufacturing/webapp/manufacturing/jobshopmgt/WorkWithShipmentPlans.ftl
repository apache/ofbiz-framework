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

<#if shipment?exists>
<div class="screenlet">
  <div class="screenlet-title-bar">
    <h3>${uiLabelMap.ManufacturingWorkWithShipmentPlans} ${shipment.shipmentId}</h3>        
  </div>
  <div class="screenlet-body">    
    ${listShipmentPlanForm.renderFormString(context)}
    <#if workInProgress>
        <br/>
        <div>
          <form name= "shipmentWorkEffortTasks" method= "post" action= "<@ofbizUrl>ShipmentWorkEffortTasks.pdf</@ofbizUrl>" >
            <input type= "hidden" name= "shipmentId" value= "${shipmentId}">
            <a href="javascript:document.shipmentWorkEffortTasks.submit()" class="buttontext" target="_report">${uiLabelMap.ManufacturingTasksReport}</a>
          </form>
        </div>
        <div>
          <form name= "cuttingListReport" method= "post" action= "<@ofbizUrl>CuttingListReport.pdf</@ofbizUrl>" >
            <input type= "hidden" name= "shipmentId" value= "${shipmentId}">
            <a href="javascript:document.cuttingListReport.submit()" target="_report" class="buttontext">${uiLabelMap.ManufacturingCuttingListReport}</a>
          </form>
        </div>
    <#else>
        <div>
          <form name= "createProductionRunsForShipment" method= "post" action= "<@ofbizUrl>createProductionRunsForShipment</@ofbizUrl>">
            <input type= "hidden" name= "shipmentId" value= "${shipmentId}">
            <a href="javascript:document.createProductionRunsForShipment.submit()" class="buttontext">${uiLabelMap.ManufacturingCreateProductionRun}</a>
          </form>
        </div>
        <br/>
        <div>
          <form name= "shipmentPlanStockReport" method= "post" action= "<@ofbizUrl>ShipmentPlanStockReport.pdf</@ofbizUrl>">
            <input type= "hidden" name= "shipmentId" value= "${shipmentId}">
            <a href="javascript:document.shipmentPlanStockReport.submit()" target="_report" class="buttontext">${uiLabelMap.ManufacturingShipmentPlanStockReport}</a></div>
          </form>
        </div>
    </#if>
        <div>
          <form name= "shipmentLabel" method= "post" action= "<@ofbizUrl>ShipmentLabel.pdf</@ofbizUrl>">
            <input type= "hidden" name= "shipmentId" value= "${shipmentId}">
            <a href="javascript:document.shipmentLabel.submit()" class="buttontext" target="_report">${uiLabelMap.ManufacturingPackageLabelsReport}</a></div>
          </form>
        </div>
</div>
<#else>
<div class="screenlet">
  <div class="screenlet-title-bar">
    <h3>${uiLabelMap.ManufacturingWorkWithShipmentPlans}</h3>        
  </div>
  <div class="screenlet-body">    
    <#if listShipmentPlansForm?has_content>
        ${listShipmentPlansForm.renderFormString(context)}
    </#if>
  </div>
</div>
</#if>