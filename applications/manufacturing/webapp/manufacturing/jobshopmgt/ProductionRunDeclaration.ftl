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
                                <#assign currentStatus = productionRun.getString("currentStatusId")/>
                                <span class="submenutextdisabled">${uiLabelMap.ManufacturingConfirmProductionRun}</span>
                                <#if currentStatus == "PRUN_COMPLETED">
                                    <a href="<@ofbizUrl>changeProductionRunStatusToClosed?productionRunId=${productionRunId}</@ofbizUrl>" class="submenutext">${uiLabelMap.ManufacturingProductionRunClose}</a>
                                <#else>
                                    <span class="submenutextdisabled">${uiLabelMap.ManufacturingProductionRunClose}</span>
                                </#if>
                                <#if currentStatus == "PRUN_CANCELLED" || currentStatus == "PRUN_COMPLETED" || currentStatus == "PRUN_CLOSED">
                                    <span class="submenutextdisabled">${uiLabelMap.ManufacturingQuickComplete}</span>
                                <#else>
                                    <a href="<@ofbizUrl>quickChangeProductionRunStatus?productionRunId=${productionRunId}&statusId=PRUN_COMPLETED</@ofbizUrl>" class="submenutext">${uiLabelMap.ManufacturingQuickComplete}</a>
                                </#if>
                                <#if currentStatus == "PRUN_CANCELLED" || currentStatus == "PRUN_CLOSED">
                                    <span class="submenutextdisabled">${uiLabelMap.ManufacturingQuickClose}</span>
                                <#else>
                                    <a href="<@ofbizUrl>quickChangeProductionRunStatus?productionRunId=${productionRunId}&statusId=PRUN_CLOSED</@ofbizUrl>" class="submenutext">${uiLabelMap.ManufacturingQuickClose}</a>
                                </#if>
                                <#if currentStatus == "PRUN_CANCELLED" || currentStatus == "PRUN_COMPLETED" || currentStatus == "PRUN_CLOSED">
                                    <span class="submenutextdisabled">${uiLabelMap.ManufacturingCancel}</span>
                                <#else>
                                    <a href="<@ofbizUrl>cancelProductionRun?productionRunId=${productionRunId}</@ofbizUrl>" class="submenutextright">${uiLabelMap.ManufacturingCancel}</a>
                                </#if>
                            </div>
                            <div class="boxhead-fill">&nbsp;</div>
                        </div>
                        ${updateProductionRunWrapper.renderFormString(context)}
                    </td>
                </tr>
                <#if inventoryItems?has_content>
                <tr>
                    <td align="left">
                        <table border="0" cellpadding="2" cellspacing="0">
                            <tr>
                                <th width="20%" align="right">
                                    ${uiLabelMap.ManufacturingInventoryItemsProduced}
                                </th>
                                <td>&nbsp;</td>
                                <td width="80%" align="left">
                                    <span class="tabletext">
                                        <#list inventoryItems as inventoryItem>
                                            <a href="/facility/control/EditInventoryItem?inventoryItemId=${inventoryItem.getString("inventoryItemId")}" class="buttontext" target="_blank">
                                                ${inventoryItem.getString("inventoryItemId")}
                                            </a>&nbsp;
                                        </#list>
                                    </span>
                                </td>
                            </tr>
                        </table>
                    </td>
                </tr>
                </#if>
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
                                    <span class="tabletext">
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
                <#-- Product component sub-screen  Update or Add  -->
                <#if productId?has_content || actionForm=="AddProductComponent">

            <td> &nbsp; </td>
            <td>
                <table border="0" width="100%" cellspacing="0" cellpadding="0" class="boxoutside">
                  <tr><td>
                    <table width="100%" border="0" cellspacing="0" cellpadding="0" class="boxtop">
                        <tr>
                        <#if productId?has_content> <#-- Product component Update  -->
                            <td><div class="boxhead">${uiLabelMap.CommonEdit}&nbsp;${uiLabelMap.ManufacturingProductionRunProductComponent} : ${productId}</div></td>
                        <#else> <#-- Product component Add         -->
                            <td><div class="boxhead">${uiLabelMap.ManufacturingAddProductionRunProductComponent}</div></td>
                        </#if>
                        </tr>
                    </table>
                    ${editPrProductComponentWrapper.renderFormString(context)}
                  </td></tr>
                </table>
            </td>
        </#if>
        <#-- Fixed Asset assign sub-screen  Update or Add  -->
        <#if fixedAssetId?has_content || actionForm=="AddFixedAsset">
            <td> &nbsp; </td>
            <td>
                <table border="0" width="100%" cellspacing="0" cellpadding="0" class="boxoutside">
                    <tr><td>
                    	<table width="100%" border="0" cellspacing="0" cellpadding="0" class="boxtop">
                        	<tr>
                        	<#if fixedAssetId?has_content> <#-- Fixed Asset Assign Update  -->
                            	<td><div class="boxhead">${uiLabelMap.CommonEdit}&nbsp;${uiLabelMap.ManufacturingProductionRunFixedAssetAssign}</div></td>
                        	<#else> <#-- Fixed Asset Assign Add -->
                            	<td><div class="boxhead">${uiLabelMap.ManufacturingAddProductionRunFixedAssetAssign}</div></td>
                        	</#if>
                        	</tr>
                    	</table>
                    	${editProdRunFixedAssetWrapper.renderFormString(context)}
                    </td></tr>
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
<#else>
  <h1>${uiLabelMap.ManufacturingNoProductionRunSelected}</h1>
</#if>
