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
        <div class="boxhead">${uiLabelMap.ProductPicklistManage}</div>
    </div>
    <div class="screenlet-body">
        <#if picklistInfoList?has_content>
            <#list picklistInfoList as picklistInfo>
                <#assign picklist = picklistInfo.picklist>

                <#-- Picklist -->
                <div class="tabletext">
                    <b>${uiLabelMap.ProductPickList}</b> <span class="head2">${picklist.picklistId}</span>
                    <b>${uiLabelMap.CommonDate}</b> ${picklist.picklistDate}
                    <form method="post" action="<@ofbizUrl>updatePicklist</@ofbizUrl>" style="display: inline;">
                        <input type="hidden" name="facilityId" value="${facilityId}"/>
                        <input type="hidden" name="picklistId" value="${picklist.picklistId}"/>
                        <select name="statusId" class="smallSelect">
                            <option value="${picklistInfo.statusItem.statusId}" selected>${picklistInfo.statusItem.get("description",locale)}</option>
                            <option value="${picklistInfo.statusItem.statusId}">---</option>
                            <#list picklistInfo.statusValidChangeToDetailList as statusValidChangeToDetail>
                                <option value="${statusValidChangeToDetail.statusIdTo}">${statusValidChangeToDetail.description} (${statusValidChangeToDetail.transitionName})</option>
                            </#list>
                        </select>
                        <input type="submit" value="${uiLabelMap.CommonUpdate}" class="smallSubmit"/>
                    </form>
                    <b>${uiLabelMap.ProductCreatedModifiedBy}</b> ${picklist.createdByUserLogin}/${picklist.lastModifiedByUserLogin}
                    <a href="<@ofbizUrl>PicklistReport.pdf?picklistId=${picklist.picklistId}</@ofbizUrl>" target="_blank" class="buttontext">[${uiLabelMap.ProductPick}/${uiLabelMap.ProductPacking} ${uiLabelMap.CommonReports}]</a>
                </div>
                <#if picklistInfo.shipmentMethodType?has_content>
                    <div class="tabletext" style="margin-left: 15px;">
                        <b>${uiLabelMap.CommonFor} ${uiLabelMap.ProductShipmentMethodType}</b> ${picklistInfo.shipmentMethodType.description?default(picklistInfo.shipmentMethodType.shipmentMethodTypeId)}
                    </div>
                </#if>

                <#-- PicklistRole -->
                <#list picklistInfo.picklistRoleInfoList?if_exists as picklistRoleInfo>
                    <div class="tabletext" style="margin-left: 15px;">
                        <b>${uiLabelMap.PartyParty}</b> ${picklistRoleInfo.partyNameView.firstName?if_exists} ${picklistRoleInfo.partyNameView.middleName?if_exists} ${picklistRoleInfo.partyNameView.lastName?if_exists} ${picklistRoleInfo.partyNameView.groupName?if_exists}
                        <b>${uiLabelMap.PartyRole}</b> ${picklistRoleInfo.roleType.description}
                        <b>${uiLabelMap.CommonFrom}</b> ${picklistRoleInfo.picklistRole.fromDate}
                        <#if picklistRoleInfo.picklistRole.thruDate?exists><b>thru</b> ${picklistRoleInfo.picklistRole.thruDate}</#if>
                    </div>
                </#list>
                <div class="tabletext" style="margin-left: 15px;">
                    <b>${uiLabelMap.ProductAssignPicker}:</b>
                    <form method="post" action="<@ofbizUrl>createPicklistRole</@ofbizUrl>" style="display: inline;">
                        <input type="hidden" name="facilityId" value="${facilityId}"/>
                        <input type="hidden" name="picklistId" value="${picklist.picklistId}"/>
                        <input type="hidden" name="roleTypeId" value="PICKER"/>
                        <select name="partyId" class="smallSelect">
                            <#list partyRoleAndPartyDetailList as partyRoleAndPartyDetail>
                                <option value="${partyRoleAndPartyDetail.partyId}">${partyRoleAndPartyDetail.firstName?if_exists} ${partyRoleAndPartyDetail.middleName?if_exists} ${partyRoleAndPartyDetail.lastName?if_exists} ${partyRoleAndPartyDetail.groupName?if_exists} [${partyRoleAndPartyDetail.partyId}]</option>
                            </#list>
                        </select>
                        <input type="submit" value="${uiLabelMap.CommonAdd}" class="smallSubmit"/>
                    </form>
                </div>

                <#-- PicklistStatusHistory -->
                <#list picklistInfo.picklistStatusHistoryInfoList?if_exists as picklistStatusHistoryInfo>
                    <div class="tabletext" style="margin-left: 15px;">
                        <b>${uiLabelMap.CommonStatus}</b> ${uiLabelMap.CommonChange} ${uiLabelMap.CommonFrom} ${picklistStatusHistoryInfo.statusItem.get("description",locale)}
                        ${uiLabelMap.CommonTo} ${picklistStatusHistoryInfo.statusItemTo.description}
                        ${uiLabelMap.CommonOn} ${picklistStatusHistoryInfo.picklistStatusHistory.changeDate}
                        ${uiLabelMap.CommonBy} ${picklistStatusHistoryInfo.picklistStatusHistory.changeUserLoginId}
                    </div>
                </#list>

                <#-- PicklistBin -->
                <#list picklistInfo.picklistBinInfoList?if_exists as picklistBinInfo>
                    <#assign isBinComplete = Static["org.ofbiz.shipment.picklist.PickListServices"].isBinComplete(delegator, picklistBinInfo.picklistBin.picklistBinId)/>
                    <#if (!isBinComplete)>
                        <div class="tabletext" style="margin-left: 15px;">
                            <b>${uiLabelMap.ProductBinNum}</b> ${picklistBinInfo.picklistBin.binLocationNumber}&nbsp;(${picklistBinInfo.picklistBin.picklistBinId})
                            <#if picklistBinInfo.primaryOrderHeader?exists><b>${uiLabelMap.ProductPrimaryOrderId}</b> ${picklistBinInfo.primaryOrderHeader.orderId}</#if>
                            <#if picklistBinInfo.primaryOrderItemShipGroup?exists><b>${uiLabelMap.ProductPrimaryShipGroupSeqId}</b> ${picklistBinInfo.primaryOrderItemShipGroup.shipGroupSeqId}</#if>
                            <#if !picklistBinInfo.picklistItemInfoList?has_content><a href="<@ofbizUrl>deletePicklistBin?picklistBinId=${picklistBinInfo.picklistBin.picklistBinId}&amp;facilityId=${facilityId?if_exists}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonDelete}</a></#if>
                        </div>
                        <div class="tabletext" style="margin-left: 30px;">
                            <b>${uiLabelMap.CommonUpdate} ${uiLabelMap.ProductBinNum}:</b>
                            <form method="post" action="<@ofbizUrl>updatePicklistBin</@ofbizUrl>" style="display: inline;">
                                <input type="hidden" name="facilityId" value="${facilityId}"/>
                                <input type="hidden" name="picklistBinId" value="${picklistBinInfo.picklistBin.picklistBinId}"/>
                                ${uiLabelMap.ProductLocation}#:
                                <input type"text" size="2" name="binLocationNumber" value="${picklistBinInfo.picklistBin.binLocationNumber}"/>
                                ${uiLabelMap.PageTitlePickList}:
                                <select name="picklistId" class="smallSelect">
                                    <#list picklistActiveList as picklistActive>
                                        <#assign picklistActiveStatusItem = picklistActive.getRelatedOneCache("StatusItem")>
                                        <option value="${picklistActive.picklistId}"<#if picklistActive.picklistId == picklist.picklistId> selected</#if>>${picklistActive.picklistId} [${uiLabelMap.CommonDate}:${picklistActive.picklistDate},${uiLabelMap.CommonStatus}:${picklistActiveStatusItem.get("description",locale)}]</option>
                                    </#list>
                                </select>
                                <input type="submit" value="${uiLabelMap.CommonUpdate}" class="smallSubmit"/>
                            </form>
                        </div>
                        <#if picklistBinInfo.picklistItemInfoList?has_content>
                            <div style="margin-left: 30px;">
                                <table class="basic-table" cellspacing="0">
                                    <tr>
                                        <b>
                                            <th>${uiLabelMap.ProductOrderId}</th>
                                            <th>${uiLabelMap.OrderShipGroup}</th>
                                            <th>${uiLabelMap.ProductOrderItem}</th>
                                            <th>${uiLabelMap.ProductProduct}</th>
                                            <th>${uiLabelMap.ProductInventoryItem}</th>
                                            <th>${uiLabelMap.ProductLocation}</th>
                                            <th>${uiLabelMap.ProductQuantity}</th>
                                            <th>&nbsp;</th>
                                        </b>
                                    </tr>
                                <#list picklistBinInfo.picklistItemInfoList?if_exists as picklistItemInfo>
                                <#assign picklistItem = picklistItemInfo.picklistItem>
                                <#assign inventoryItemAndLocation = picklistItemInfo.inventoryItemAndLocation>
                                    <tr>
                                        <td>${picklistItem.orderId}</td>
                                        <td>${picklistItem.shipGroupSeqId}</td>
                                        <td>${picklistItem.orderItemSeqId}</td>
                                        <td>${picklistItemInfo.orderItem.productId}<#if picklistItemInfo.orderItem.productId != inventoryItemAndLocation.productId>&nbsp;[${inventoryItemAndLocation.productId}]</#if></td>
                                        <td>${inventoryItemAndLocation.inventoryItemId}</td>
                                        <td>${inventoryItemAndLocation.areaId?if_exists}-${inventoryItemAndLocation.aisleId?if_exists}-${inventoryItemAndLocation.sectionId?if_exists}-${inventoryItemAndLocation.levelId?if_exists}-${inventoryItemAndLocation.positionId?if_exists}</td>
                                        <td>${picklistItem.quantity}</td>
                                        <#if !picklistItemInfo.itemIssuanceList?has_content>
                                            <td><a href="<@ofbizUrl>deletePicklistItem?picklistBinId=${picklistItemInfo.picklistItem.picklistBinId}&amp;orderId=${picklistItemInfo.picklistItem.orderId}&amp;orderItemSeqId=${picklistItemInfo.picklistItem.orderItemSeqId}&amp;shipGroupSeqId=${picklistItemInfo.picklistItem.shipGroupSeqId}&amp;inventoryItemId=${picklistItemInfo.picklistItem.inventoryItemId}&amp;facilityId=${facilityId?if_exists}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonDelete}</a></td>
                                        </#if>
                                        <td>
                                            <#-- picklistItem.orderItemShipGrpInvRes (do we want to display any of this info?) -->
                                            <#-- picklistItemInfo.itemIssuanceList -->
                                            <#list picklistItemInfo.itemIssuanceList?if_exists as itemIssuance>
                                                <b>${uiLabelMap.ProductIssue} ${uiLabelMap.CommonTo} ${uiLabelMap.ProductShipmentItemSeqId}:</b> ${itemIssuance.shipmentId}:${itemIssuance.shipmentItemSeqId}
                                                <b>${uiLabelMap.ProductQuantity}:</b> ${itemIssuance.quantity}
                                                <b>${uiLabelMap.CommonDate}: </b> ${itemIssuance.issuedDateTime}
                                            </#list>
                                        </td>
                                    </tr>
                                </#list>
                                </table>
                            </div>
                        </#if>
                    </#if>
                </#list>

                <#if picklistInfo_has_next>
                   <hr class="sepbar"/>
                </#if>
            </#list>
        <#else/>
            <h3>${uiLabelMap.ProductNoPicksStarted}.</h3>
        </#if>
    </div>
</div>
