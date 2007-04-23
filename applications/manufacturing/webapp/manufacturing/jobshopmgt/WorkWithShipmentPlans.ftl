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
  <h1>${uiLabelMap.ManufacturingWorkWithShipmentPlans}: ${shipment.shipmentId}</h1>
  ${listShipmentPlanForm.renderFormString(context)}
  <#if workInProgress>
    <br/>
    <div><a href="<@ofbizUrl>ShipmentWorkEffortTasks.pdf?shipmentId=${shipmentId}</@ofbizUrl>" class="buttontext" target="_report">${uiLabelMap.ManufacturingTasksReport}</a></div>
    <div><a href="<@ofbizUrl>CuttingListReport.pdf?shipmentId=${shipmentId}</@ofbizUrl>" target="_report" class="buttontext">${uiLabelMap.ManufacturingCuttingListReport}</a></div>
  <#else>
    <div><a href="<@ofbizUrl>createProductionRunsForShipment?shipmentId=${shipmentId}</@ofbizUrl>" class="buttontext">${uiLabelMap.ManufacturingCreateProductionRun}</a></div>
    <br/>
    <div><a href="<@ofbizUrl>ShipmentPlanStockReport.pdf?shipmentId=${shipmentId}</@ofbizUrl>" target="_report" class="buttontext">${uiLabelMap.ManufacturingShipmentPlanStockReport}</a></div>
  </#if>

  <div><a href="<@ofbizUrl>ShipmentLabel.pdf?shipmentId=${shipmentId}</@ofbizUrl>" class="buttontext" target="_report">${uiLabelMap.ManufacturingPackageLabelsReport}</a></div>
  <#-- new reports -->
  <#--
  <div><a href="<@ofbizUrl>SPPRunsProductsByFeature.pdf?shipmentId=${shipmentId}&taskNamePar=O-LAV_01b&productFeatureTypeIdPar=Strutt&productCategoryIdPar=</@ofbizUrl>" class="buttontext" target="_report">${uiLabelMap.ManufacturingSPPRunsProductsByFeature}</a></div>
  <div><a href="<@ofbizUrl>SPPRunsComponentsByFeature.pdf?showLocation=Y&shipmentId=${shipmentId}&taskNamePar=O-PREL_L&productFeatureTypeIdPar=Strutt&productCategoryIdPar=PANNELLI</@ofbizUrl>" class="buttontext" target="_report">${uiLabelMap.ManufacturingSPPRunsComponentsByFeature2}</a></div>
  <div><a href="<@ofbizUrl>SPPRunsComponentsByFeature.pdf?showLocation=N&shipmentId=${shipmentId}&taskNamePar=O-PREL_L&productFeatureTypeIdPar=Strutt&productCategoryIdPar=PEZZI</@ofbizUrl>" class="buttontext" target="_report">${uiLabelMap.ManufacturingSPPRunsComponentsByFeature3}</a></div>
  <div><a href="<@ofbizUrl>PackageContentsAndOrder.pdf?shipmentId=${shipmentId}&taskNamePar=&productFeatureTypeIdPar=&productCategoryIdPar=</@ofbizUrl>" class="buttontext" target="_report">${uiLabelMap.ManufacturingPackageContentsAndOrder}</a></div>
  <div><a href="<@ofbizUrl>PRunsProductsAndOrder.pdf?shipmentId=${shipmentId}&taskNamePar=&productFeatureTypeIdPar=&productCategoryIdPar=ANTA</@ofbizUrl>" class="buttontext" target="_report">${uiLabelMap.ManufacturingPRunsProductsAndOrder}</a></div>
  <div><a href="<@ofbizUrl>PRunsInfoAndOrder.pdf?shipmentId=${shipmentId}&taskNamePar=O-PREL_L&productFeatureTypeIdPar=&productCategoryIdPar=HARDWARE</@ofbizUrl>" class="buttontext" target="_report">${uiLabelMap.ManufacturingPRunsInfoAndOrder}</a></div>
  -->
<#else>
<h1>${uiLabelMap.ManufacturingWorkWithShipmentPlans}</h1>
<#if listShipmentPlansForm?has_content>
  ${listShipmentPlansForm.renderFormString(context)}
</#if>

</#if>

