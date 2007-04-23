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

<#if mrpName?exists>
  <h1>${uiLabelMap.ManufacturingMrpName}: ${mrpName?if_exists}</h1>
  <#--
  <div><a href="<@ofbizUrl>MRPPRunsProductsByFeature.pdf?mrpName=${mrpName}&taskNamePar=O-LAV_01b&productCategoryIdPar=</@ofbizUrl>" class="buttontext" target="_report">[${uiLabelMap.ManufacturingMRPPRunsProductsByFeature}]</a></div>
  <div><a href="<@ofbizUrl>PRunsComponentsByFeature.pdf?showLocation=Y&mrpName=${mrpName}&taskNamePar=O-PREL_L&productCategoryIdPar=PANNELLI</@ofbizUrl>" class="buttontext" target="_report">[${uiLabelMap.ManufacturingPRunsComponentsByFeature}]</a></div>
  <div><a href="<@ofbizUrl>PRunsComponentsByFeature.pdf?showLocation=N&mrpName=${mrpName}&taskNamePar=O-PREL_L&productCategoryIdPar=PEZZI</@ofbizUrl>" class="buttontext" target="_report">[${uiLabelMap.ManufacturingPRunsComponentsByFeature1}]</a></div>
  <div><a href="<@ofbizUrl>PRunsProductsStacks.pdf?mrpName=${mrpName}&taskNamePar=O-LAV_01b&productCategoryIdPar=</@ofbizUrl>" class="buttontext" target="_report">[${uiLabelMap.ManufacturingPRunsProductsStacks}]</a></div>
  -->
</#if>

