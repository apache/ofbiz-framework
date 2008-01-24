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

<#assign selected = tabButtonItem?default("void")>

<#if productionRun?has_content>
<div class="button-bar tab-bar">    
    <ul>
        <#if productionRun.getString("currentStatusId") == "PRUN_CREATED" || productionRun.getString("currentStatusId") == "PRUN_SCHEDULED">
            <li<#if selected == "edit"> class="selected"</#if>><a href="<@ofbizUrl>EditProductionRun?productionRunId=${productionRunId}</@ofbizUrl>">${uiLabelMap.ManufacturingEditProductionRun}</a></li>
            <li<#if selected == "tasks"> class="selected"</#if>><a href="<@ofbizUrl>ProductionRunTasks?productionRunId=${productionRunId}</@ofbizUrl>">${uiLabelMap.ManufacturingListOfProductionRunRoutingTasks}</a></li>
            <li<#if selected == "components"> class="selected"</#if>><a href="<@ofbizUrl>ProductionRunComponents?productionRunId=${productionRunId}</@ofbizUrl>">${uiLabelMap.ManufacturingMaterials}</a></li>
            <li<#if selected == "fixedAssets"> class="selected"</#if>><a href="<@ofbizUrl>ProductionRunFixedAssets?productionRunId=${productionRunId}</@ofbizUrl>">${uiLabelMap.AccountingFixedAssets}</a></li>
        <#else>
            <li<#if selected == "declaration"> class="selected"</#if>><a href="<@ofbizUrl>ProductionRunDeclaration?productionRunId=${productionRunId}</@ofbizUrl>">${uiLabelMap.ManufacturingProductionRunDeclaration}</a></li>
            <li<#if selected == "actualComponents"> class="selected"</#if>><a href="<@ofbizUrl>ProductionRunActualComponents?productionRunId=${productionRunId}</@ofbizUrl>">${uiLabelMap.ManufacturingActualMaterials}</a></li>
        </#if>
        <li<#if selected == "assocs"> class="selected"</#if>><a href="<@ofbizUrl>ProductionRunAssocs?productionRunId=${productionRunId}</@ofbizUrl>">${uiLabelMap.ManufacturingProductionRunAssocs}</a></li>
        <li<#if selected == "costs"> class="selected"</#if>><a href="<@ofbizUrl>ProductionRunCosts?productionRunId=${productionRunId}</@ofbizUrl>">${uiLabelMap.ManufacturingActualCosts}</a></li>
    </ul>
    <br class="clear"/>
</div>
</#if>