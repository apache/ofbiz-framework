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

<#-- Mandatory work efforts -->
<#if mandatoryWorkEfforts?has_content>
    <p>
    ${uiLabelMap.ManufacturingMandatoryProductionRuns}:
    <#list mandatoryWorkEfforts as mandatoryWorkEffortAssoc>
        <#assign mandatoryWorkEffort = mandatoryWorkEffortAssoc.getRelatedOne("FromWorkEffort")>
        <#if "PRUN_COMPLETED" == mandatoryWorkEffort.getString("currentStatusId") || "PRUN_CLOSED" == mandatoryWorkEffort.getString("currentStatusId")>
            <a href="<@ofbizUrl>ProductionRunDeclaration?productionRunId=${mandatoryWorkEffort.workEffortId}</@ofbizUrl>" class="buttontext">${mandatoryWorkEffort.workEffortName}</a>&nbsp;
        <#else>
            <#if "PRUN_CREATED" == mandatoryWorkEffort.getString("currentStatusId")>
                <a href="<@ofbizUrl>EditProductionRun?productionRunId=${mandatoryWorkEffort.workEffortId}</@ofbizUrl>" class="buttontext">${mandatoryWorkEffort.workEffortName}</a>[*]&nbsp;
            <#else>
                <a href="<@ofbizUrl>ProductionRunDeclaration?productionRunId=${mandatoryWorkEffort.workEffortId}</@ofbizUrl>" class="buttontext">${mandatoryWorkEffort.workEffortName}</a>[*]&nbsp;
            </#if>
        </#if>
    </#list>
    </p>
</#if>
<#-- Dependent work efforts -->
<#if dependentWorkEfforts?has_content>
    <p>
    ${uiLabelMap.ManufacturingDependentProductionRuns}: 
    <#list dependentWorkEfforts as dependentWorkEffortAssoc>
        <#assign dependentWorkEffort = dependentWorkEffortAssoc.getRelatedOne("ToWorkEffort")>
        <#if "PRUN_COMPLETED" == dependentWorkEffort.currentStatusId || "PRUN_CLOSED" == dependentWorkEffort.currentStatusId>
            <a href="<@ofbizUrl>ProductionRunDeclaration?productionRunId=${dependentWorkEffort.workEffortId}</@ofbizUrl>" class="buttontext">${dependentWorkEffort.workEffortName}</a>&nbsp;
        <#else>
            <#if "PRUN_CREATED" == dependentWorkEffort.getString("currentStatusId")>
                <a href="<@ofbizUrl>EditProductionRun?productionRunId=${dependentWorkEffort.workEffortId}</@ofbizUrl>" class="buttontext">${dependentWorkEffort.workEffortName}</a>[*]&nbsp;
            <#else>
                <a href="<@ofbizUrl>ProductionRunDeclaration?productionRunId=${dependentWorkEffort.workEffortId}</@ofbizUrl>" class="buttontext">${dependentWorkEffort.workEffortName}</a>[*]&nbsp;
            </#if>
        </#if>
    </#list>
    </p>
</#if>

<table border="0" width="100%" cellspacing="0" cellpadding="0">
    <tr valign="top">
        <#-- RoutingTask sub-screen  Update or Add  -->
        <#if routingTaskId?has_content || actionForm=="AddRoutingTask">
        <td> &nbsp; </td>
        <td>
            <table border="0" width="100%" cellspacing="0" cellpadding="0" class="boxoutside">
                <tr>
                    <td>
                        <table width="100%" border="0" cellspacing="0" cellpadding="0" class="boxtop">
                            <tr>
                                <#-- RoutingTask Update  -->
                                <#if routingTaskId?has_content>
                                <td>
                                    <div class="boxhead">${uiLabelMap.CommonEdit}&nbsp;${uiLabelMap.ManufacturingRoutingTaskId}: ${routingTaskId}</div>
                                </td>
                                <#else>
                                <#-- RoutingTask Add -->
                                <td>
                                    <div class="boxhead">${uiLabelMap.ManufacturingAddRoutingTask}</div>
                                </td>
                                </#if>
                            </tr>
                        </table>
                        ${editPrRoutingTaskWrapper.renderFormString(context)}
                        <br/>
                        ${createRoutingTaskDelivProductForm.renderFormString(context)}
                        <#if prunInventoryProduced?has_content>
                            <br/>
                            ${prunInventoryProducedForm.renderFormString(context)}
                        </#if>
                    </td>
                </tr>
            </table>
            </td>
        </#if>
        </tr>
    </table>
    <br/>
    
              <#-- List Of ProductionRun RoutingTasks  sub-screen -->
    <table border="0" width="100%" cellspacing="0" cellpadding="0" class="boxoutside">
        <tr>
            <td>
                <table width="100%" border="0" cellspacing="0" cellpadding="0" class="boxtop">
                    <tr>
                        <td><div class="boxhead">${uiLabelMap.ManufacturingListOfProductionRunRoutingTasks}</div></td>
                        <td align="right">
                            <div class="boxhead-right" align="right">
                                <a href="<@ofbizUrl>quickRunAllProductionRunTasks?productionRunId=${productionRunId}</@ofbizUrl>" class="submenutext">${uiLabelMap.ManufacturingQuickRunAllTasks}</a>
                            </div>
                        </td>
                    </tr>
                </table>
            </td>
        </tr>
        <tr>
            <td>
                ${ListProductionRunRoutingTasksWrapper.renderFormString(context)}
            </td>
        </tr>
    </table>

        <#-- List Of ProductionRun Components  sub-screen -->
    <table border="0" width="100%" cellspacing="0" cellpadding="0" class="boxoutside">
      <tr><td>
        <table width="100%" border="0" cellspacing="0" cellpadding="0" class="boxtop">
            <tr>
                <td><div class="boxhead">${uiLabelMap.ManufacturingListOfProductionRunComponents}</div></td>
                <td align="right"></td>
            </tr>
        </table>
        ${ListProductionRunComponentsWrapper.renderFormString(context)}
      </td></tr>
    </table>

    <#-- List of ProductionRun Fixed Assets sub-screen -->
    <table border="0" width="100%" cellspacing="0" cellpadding="0" class="boxoutside">
      <tr><td>
        <table width="100%" border="0" cellspacing="0" cellpadding="0" class="boxtop">
            <tr>
                <td><div class="boxhead">${uiLabelMap.ManufacturingListOfProductionRunFixedAssets}</div></td>
            </tr>
        </table>
        ${ListProductionRunFixedAssetsWrapper.renderFormString(context)}
      </td></tr>
    </table>
