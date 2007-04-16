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

<div class="screenlet">
    <div class="screenlet-header">
        <div class="simple-right-half">
            <a href="<@ofbizUrl>PickMoveStockSimple?facilityId=${facilityId?if_exists}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonPrint}</a>
        </div>
        <div class="boxhead">${uiLabelMap.ProductStockMovesNeeded}</div>
    </div>
    <div class="screenlet-body">
          <form method="post" action="<@ofbizUrl>processPhysicalStockMove</@ofbizUrl>" name='selectAllForm' style='margin: 0;'>
              <#-- general request fields -->
              <input type="hidden" name="facilityId" value="${facilityId?if_exists}">   
              <input type="hidden" name="_useRowSubmit" value="Y">
              <#assign rowCount = 0>
              <table border="1" cellspacing="0" cellpadding="2">
                <tr>
                    <td><div class="tableheadtext">${uiLabelMap.ProductProductId}</div></td>
                    <td><div class="tableheadtext">${uiLabelMap.ProductProduct}</div></td>
                    <td><div class="tableheadtext">${uiLabelMap.ProductFromLocation}</div></td>
                    <td><div class="tableheadtext">${uiLabelMap.ProductQoh}</div></td>
                    <td><div class="tableheadtext">${uiLabelMap.ProductAtp}</div></td>
                    <td><div class="tableheadtext">${uiLabelMap.ProductToLocation}</div></td>
                    <td><div class="tableheadtext">${uiLabelMap.ProductQoh}</div></td>
                    <td><div class="tableheadtext">${uiLabelMap.ProductAtp}</div></td>
                    <td><div class="tableheadtext">${uiLabelMap.ProductMinimumStock}</div></td>
                    <td><div class="tableheadtext">${uiLabelMap.ProductMoveQuantity}</div></td>
                    <td><div class="tableheadtext">${uiLabelMap.OrderConfirm}</div></td>
                    <td align="right">
                        <span class="tableheadtext">${uiLabelMap.ProductSelectAll}</span>&nbsp;
                        <input type="checkbox" name="selectAll" value="Y" onclick="javascript:toggleAll(this, 'selectAllForm');">
                    </td>
                </tr>
                <#if moveByOisgirInfoList?has_content || moveByPflInfoList?has_content>
                    <#list moveByOisgirInfoList?if_exists as moveByOisgirInfo>
                        <#assign product = moveByOisgirInfo.product>
                        <#assign facilityLocationFrom = moveByOisgirInfo.facilityLocationFrom>
                        <#assign facilityLocationTypeEnumFrom = (facilityLocationFrom.getRelatedOneCache("TypeEnumeration"))?if_exists>
                        <#assign facilityLocationTo = moveByOisgirInfo.facilityLocationTo>
                        <#assign targetProductFacilityLocation = moveByOisgirInfo.targetProductFacilityLocation>
                        <#assign facilityLocationTypeEnumTo = (facilityLocationTo.getRelatedOneCache("TypeEnumeration"))?if_exists>
                        <#assign totalQuantity = moveByOisgirInfo.totalQuantity>
                        <tr>
                            <td><div class="tabletext">${product.productId}</div></td>
                            <td><div class="tabletext">${product.internalName?if_exists}</div></td>
                            <td><div class="tabletext">${facilityLocationFrom.areaId?if_exists}:${facilityLocationFrom.aisleId?if_exists}:${facilityLocationFrom.sectionId?if_exists}:${facilityLocationFrom.levelId?if_exists}:${facilityLocationFrom.positionId?if_exists}<#if facilityLocationTypeEnumFrom?has_content>(${facilityLocationTypeEnumFrom.description})</#if>[${facilityLocationFrom.locationSeqId}]</div></td>
                            <td><div class="tabletext">${moveByOisgirInfo.quantityOnHandTotalFrom?if_exists}</div></td>
                            <td><div class="tabletext">${moveByOisgirInfo.availableToPromiseTotalFrom?if_exists}</div></td>
                            <td><div class="tabletext">${facilityLocationTo.areaId?if_exists}:${facilityLocationTo.aisleId?if_exists}:${facilityLocationTo.sectionId?if_exists}:${facilityLocationTo.levelId?if_exists}:${facilityLocationTo.positionId?if_exists}<#if facilityLocationTypeEnumTo?has_content>(${facilityLocationTypeEnumTo.description})</#if>[${facilityLocationTo.locationSeqId}]</div></td>
                            <td><div class="tabletext">${moveByOisgirInfo.quantityOnHandTotalTo?if_exists}</div></td>
                            <td><div class="tabletext">${moveByOisgirInfo.availableToPromiseTotalTo?if_exists}</div></td>
                            <td><div class="tabletext">${targetProductFacilityLocation.minimumStock?if_exists}</div></td>
                            <td><div class="tabletext">${targetProductFacilityLocation.moveQuantity?if_exists}</div></td>
                            <td align="right">              
                                <input type="hidden" name="productId_o_${rowCount}" value="${product.productId?if_exists}">
                                <input type="hidden" name="facilityId_o_${rowCount}" value="${facilityId?if_exists}">
                                <input type="hidden" name="locationSeqId_o_${rowCount}" value="${facilityLocationFrom.locationSeqId?if_exists}">
                                <input type="hidden" name="targetLocationSeqId_o_${rowCount}" value="${facilityLocationTo.locationSeqId?if_exists}">
                                <input type="text" class="inputBox" name="quantityMoved_o_${rowCount}" size="6" value="${totalQuantity?string.number}">
                            </td>
                            <td align="right">              
                                <input type="checkbox" name="_rowSubmit_o_${rowCount}" value="Y" onclick="javascript:checkToggle(this, 'selectAllForm');">
                            </td>
                        </tr>
                        <#assign rowCount = rowCount + 1>   
                    </#list>
                    <#list moveByPflInfoList?if_exists as moveByPflInfo>
                        <#assign product = moveByPflInfo.product>
                        <#assign facilityLocationFrom = moveByPflInfo.facilityLocationFrom>
                        <#assign facilityLocationTypeEnumFrom = (facilityLocationFrom.getRelatedOneCache("TypeEnumeration"))?if_exists>
                        <#assign facilityLocationTo = moveByPflInfo.facilityLocationTo>
                        <#assign targetProductFacilityLocation = moveByPflInfo.targetProductFacilityLocation>
                        <#assign facilityLocationTypeEnumTo = (facilityLocationTo.getRelatedOneCache("TypeEnumeration"))?if_exists>
                        <#assign totalQuantity = moveByPflInfo.totalQuantity>
                        <tr>
                            <td><div class="tabletext">${product.productId}</div></td>
                            <td><div class="tabletext">${product.internalName?if_exists}</div></td>
                            <td><div class="tabletext">${facilityLocationFrom.areaId?if_exists}:${facilityLocationFrom.aisleId?if_exists}:${facilityLocationFrom.sectionId?if_exists}:${facilityLocationFrom.levelId?if_exists}:${facilityLocationFrom.positionId?if_exists}<#if facilityLocationTypeEnumFrom?has_content>(${facilityLocationTypeEnumFrom.description})</#if>[${facilityLocationFrom.locationSeqId}]</div></td>
                            <td><div class="tabletext">${moveByPflInfo.quantityOnHandTotalFrom?if_exists}</div></td>
                            <td><div class="tabletext">${moveByPflInfo.availableToPromiseTotalFrom?if_exists}</div></td>
                            <td><div class="tabletext">${facilityLocationTo.areaId?if_exists}:${facilityLocationTo.aisleId?if_exists}:${facilityLocationTo.sectionId?if_exists}:${facilityLocationTo.levelId?if_exists}:${facilityLocationTo.positionId?if_exists}<#if facilityLocationTypeEnumTo?has_content>(${facilityLocationTypeEnumTo.description})</#if>[${facilityLocationTo.locationSeqId}]</div></td>
                            <td><div class="tabletext">${moveByPflInfo.quantityOnHandTotalTo?if_exists}</div></td>
                            <td><div class="tabletext">${moveByPflInfo.availableToPromiseTotalTo?if_exists}</div></td>
                            <td><div class="tabletext">${targetProductFacilityLocation.minimumStock?if_exists}</div></td>
                            <td><div class="tabletext">${targetProductFacilityLocation.moveQuantity?if_exists}</div></td>
                            <td align="right">              
                                <input type="hidden" name="productId_o_${rowCount}" value="${product.productId?if_exists}">
                                <input type="hidden" name="facilityId_o_${rowCount}" value="${facilityId?if_exists}">
                                <input type="hidden" name="locationSeqId_o_${rowCount}" value="${facilityLocationFrom.locationSeqId?if_exists}">
                                <input type="hidden" name="targetLocationSeqId_o_${rowCount}" value="${facilityLocationTo.locationSeqId?if_exists}">
                                <input type="text" class="inputBox" name="quantityMoved_o_${rowCount}" size="6" value="${totalQuantity?string.number}">
                            </td>
                            <td align="right">              
                                <input type="checkbox" name="_rowSubmit_o_${rowCount}" value="Y" onclick="javascript:checkToggle(this, 'selectAllForm');">
                            </td>
                        </tr>
                        <#assign rowCount = rowCount + 1>   
                    </#list>
                    <tr>
                        <td colspan="13" align="right">
                            <a href="javascript:document.selectAllForm.submit();" class="buttontext">${uiLabelMap.ProductConfirmSelectedMoves}</a>
                        </td>
                    </tr>
                <#else>
                    <tr><td colspan="13"><div class="head3">${uiLabelMap.ProductNoStockMovesNeeded}.</div></td></tr>
                </#if>
                <#assign messageCount = 0>
                <#list pflWarningMessageList?if_exists as pflWarningMessage>
                    <#assign messageCount = messageCount + 1>   
                    <tr><td colspan="13"><div class="head3">${messageCount}:${pflWarningMessage}.</div></td></tr>
                </#list>
            </table>
            <input type="hidden" name="_rowCount" value="${rowCount}">
        </form>
    </div>
</div>
