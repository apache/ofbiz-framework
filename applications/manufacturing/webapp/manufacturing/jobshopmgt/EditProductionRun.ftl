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

<#if productionRunId?has_content>
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
        <td>
            <#-- ProductionRun Update sub-screen -->
            <table border="0" width="100%" cellspacing="0" cellpadding="0" class="boxoutside">
                <tr>
                    <td>
                        <div class="boxtop">
                            <div class="boxhead-left">
                                ${uiLabelMap.ManufacturingProductionRunId}: ${productionRunId}
                            </div>
                            <div class="boxhead-right" align="right">
                                <a href="<@ofbizUrl>changeProductionRunStatusToPrinted?productionRunId=${productionRunId}</@ofbizUrl>" class="submenutext">${uiLabelMap.ManufacturingConfirmProductionRun}</a>
                                <a href="<@ofbizUrl>quickChangeProductionRunStatus?productionRunId=${productionRunId}&statusId=PRUN_COMPLETED</@ofbizUrl>" class="submenutext">${uiLabelMap.ManufacturingQuickComplete}</a>
                                <a href="<@ofbizUrl>quickChangeProductionRunStatus?productionRunId=${productionRunId}&statusId=PRUN_CLOSED</@ofbizUrl>" class="submenutext">${uiLabelMap.ManufacturingQuickClose}</a>
                                <a href="<@ofbizUrl>cancelProductionRun?productionRunId=${productionRunId}</@ofbizUrl>" class="submenutextright">${uiLabelMap.ManufacturingCancel}</a>
                            </div>
                            <div class="boxhead-fill">&nbsp;</div>
                        </div>
                        ${updateProductionRunWrapper.renderFormString(context)}
                    </td>
                </tr>
                <#if orderItems?has_content>
                <tr>
                    <td align="left">
                        <table border="0" cellpadding="2" cellspacing="0">
                            <tr>
                                <th width="20%" align="right">
                                    ${uiLabelMap.ManufacturingOrderItems}
                                </th>
                                <td>&nbsp;</td>
                                <td width="80%" align="left">
                                    <span>
                                        <#list orderItems as orderItem>
                                            <a href="/ordermgr/control/orderview?orderId=${orderItem.getString("orderId")}" class="buttontext" target="_blank">
                                                ${orderItem.getString("orderId")}/${orderItem.getString("orderItemSeqId")}
                                            </a>&nbsp;
                                        </#list>
                                    </span>
                                </td>
                            </tr>
                        </table>
                    </td>
                </tr>
                </#if>
            </table>
        </td>
        <#-- RoutingTask sub-screen  Update or Add  -->
        <#if routingTaskId?has_content || actionForm=="AddRoutingTask">
            <td> &nbsp; </td>
            <td>
                <table border="0" width="100%" cellspacing="0" cellpadding="0" class="boxoutside">
                  <tr><td>
                    <table width="100%" border="0" cellspacing="0" cellpadding="0" class="boxtop">
                        <tr>
                        <#if routingTaskId?has_content> <#-- RoutingTask Update  -->
                            <td><div class="boxhead">${uiLabelMap.CommonEdit}&nbsp;${uiLabelMap.ManufacturingRoutingTaskId} : ${routingTaskId}</div></td>
                        <#else> <#-- RoutingTask Add         -->
                            <td><div class="boxhead">${uiLabelMap.ManufacturingAddRoutingTask}</div></td>
                        </#if>
                        </tr>
                    </table>
                    ${editPrRoutingTaskWrapper.renderFormString(context)}
                  </td></tr>
                </table>
            </td>
        </#if>
        </tr>
    </table>
    <br/>
    
<#else>
  <h1>${uiLabelMap.ManufacturingNoProductionRunSelected}</h1>
</#if>
