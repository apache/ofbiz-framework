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
    <div class="screenlet-title-bar">
        <ul>
            <li class="h3">${uiLabelMap.ProductPicklistManage}</li>
        </ul>
        <br class="clear"/>
    </div>
    <div class="screenlet-body">
        <#if picklistInfoList?has_content>
            <#list picklistInfoList as picklistInfo>
                <#assign picklist = picklistInfo.picklist>

                <#-- Picklist -->
                <div>
                    <span class="label">${uiLabelMap.ProductPickList}</span> ${picklist.picklistId}
                    <span class="label">${uiLabelMap.CommonDate}</span> ${picklist.picklistDate}
                    <form method="post" action="<@ofbizUrl>updatePicklist</@ofbizUrl>" style="display: inline;">
                        <input type="hidden" name="facilityId" value="${facilityId}"/>
                        <input type="hidden" name="picklistId" value="${picklist.picklistId}"/>
                        <select name="statusId">
                            <option value="${picklistInfo.statusItem.statusId}" selected>${picklistInfo.statusItem.get("description",locale)}</option>
                            <option value="${picklistInfo.statusItem.statusId}">---</option>
                            <#list picklistInfo.statusValidChangeToDetailList as statusValidChangeToDetail>
                                <option value="${statusValidChangeToDetail.get("statusIdTo", locale)}">${statusValidChangeToDetail.get("description", locale)} (${statusValidChangeToDetail.get("transitionName", locale)})</option>
                            </#list>
                        </select>
                        <input type="submit" value="${uiLabelMap.CommonUpdate}" class="smallSubmit"/>
                    </form>
                    <span class="label">${uiLabelMap.ProductCreatedModifiedBy}</span> ${picklist.createdByUserLogin}/${picklist.lastModifiedByUserLogin}
                    <a href="<@ofbizUrl>PicklistReport.pdf?picklistId=${picklist.picklistId}</@ofbizUrl>" target="_blank" class="buttontext">${uiLabelMap.ProductPick}/${uiLabelMap.ProductPacking} ${uiLabelMap.CommonReports}</a>
                    <hr />
                </div>
                <#if picklistInfo.shipmentMethodType?has_content>
                    <div style="margin-left: 15px;">
                        <span class="label">${uiLabelMap.CommonFor} ${uiLabelMap.ProductShipmentMethodType}</span> ${picklistInfo.shipmentMethodType.description?default(picklistInfo.shipmentMethodType.shipmentMethodTypeId)}
                    </div>
                </#if>

                <#-- PicklistRole -->
                <#list picklistInfo.picklistRoleInfoList?if_exists as picklistRoleInfo>
                    <div style="margin-left: 15px;">
                        <span class="label">${uiLabelMap.PartyParty}</span> ${picklistRoleInfo.partyNameView.firstName?if_exists} ${picklistRoleInfo.partyNameView.middleName?if_exists} ${picklistRoleInfo.partyNameView.lastName?if_exists} ${picklistRoleInfo.partyNameView.groupName?if_exists}
                        <span class="label">${uiLabelMap.PartyRole}</span> ${picklistRoleInfo.roleType.description}
                        <span class="label">${uiLabelMap.CommonFrom}</span> ${picklistRoleInfo.picklistRole.fromDate}
                        <#if picklistRoleInfo.picklistRole.thruDate?exists><span class="label">${uiLabelMap.CommonThru}</span> ${picklistRoleInfo.picklistRole.thruDate}</#if>
                    </div>
                </#list>
                <div style="margin-left: 15px;">
                    <span class="label">${uiLabelMap.ProductAssignPicker}</span>
                    <form method="post" action="<@ofbizUrl>createPicklistRole</@ofbizUrl>" style="display: inline;">
                        <input type="hidden" name="facilityId" value="${facilityId}"/>
                        <input type="hidden" name="picklistId" value="${picklist.picklistId}"/>
                        <input type="hidden" name="roleTypeId" value="PICKER"/>
                        <select name="partyId">
                            <#list partyRoleAndPartyDetailList as partyRoleAndPartyDetail>
                                <option value="${partyRoleAndPartyDetail.partyId}">${partyRoleAndPartyDetail.firstName?if_exists} ${partyRoleAndPartyDetail.middleName?if_exists} ${partyRoleAndPartyDetail.lastName?if_exists} ${partyRoleAndPartyDetail.groupName?if_exists} [${partyRoleAndPartyDetail.partyId}]</option>
                            </#list>
                        </select>
                        <input type="submit" value="${uiLabelMap.CommonAdd}" class="smallSubmit"/>
                    </form>
                </div>

                <#-- PicklistStatusHistory -->
                <#list picklistInfo.picklistStatusHistoryInfoList?if_exists as picklistStatusHistoryInfo>
                    <div style="margin-left: 15px;">
                        <span class="label">${uiLabelMap.CommonStatus}</span> ${uiLabelMap.CommonChange} ${uiLabelMap.CommonFrom} ${picklistStatusHistoryInfo.statusItem.get("description",locale)}
                        ${uiLabelMap.CommonTo} ${picklistStatusHistoryInfo.statusItemTo.description}
                        ${uiLabelMap.CommonOn} ${picklistStatusHistoryInfo.picklistStatusHistory.changeDate}
                        ${uiLabelMap.CommonBy} ${picklistStatusHistoryInfo.picklistStatusHistory.changeUserLoginId}
                    </div>
                </#list>
                <hr />
                <#-- PicklistBin -->
                <#list picklistInfo.picklistBinInfoList?if_exists as picklistBinInfo>
                    <#assign isBinComplete = Static["org.ofbiz.shipment.picklist.PickListServices"].isBinComplete(delegator, picklistBinInfo.picklistBin.picklistBinId)/>
                    <#if (!isBinComplete)>
                        <div style="margin-left: 15px;">
                            <span class="label">${uiLabelMap.ProductBinNum}</span> ${picklistBinInfo.picklistBin.binLocationNumber}&nbsp;(${picklistBinInfo.picklistBin.picklistBinId})
                            <#if picklistBinInfo.primaryOrderHeader?exists><span class="label">${uiLabelMap.ProductPrimaryOrderId}</span> ${picklistBinInfo.primaryOrderHeader.orderId}</#if>
                            <#if picklistBinInfo.primaryOrderItemShipGroup?exists><span class="label">${uiLabelMap.ProductPrimaryShipGroupSeqId}</span> ${picklistBinInfo.primaryOrderItemShipGroup.shipGroupSeqId}</#if>
                            <#if !picklistBinInfo.picklistItemInfoList?has_content><a href="<@ofbizUrl>deletePicklistBin?picklistBinId=${picklistBinInfo.picklistBin.picklistBinId}&amp;facilityId=${facilityId?if_exists}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonDelete}</a></#if>
                        </div>
                        <div style="margin-left: 30px;">
                            <span class="label">${uiLabelMap.CommonUpdate} ${uiLabelMap.ProductBinNum}</span>
                            <form method="post" action="<@ofbizUrl>updatePicklistBin</@ofbizUrl>" style="display: inline;">
                                <input type="hidden" name="facilityId" value="${facilityId}"/>
                                <input type="hidden" name="picklistBinId" value="${picklistBinInfo.picklistBin.picklistBinId}"/>
                                <span class="label">${uiLabelMap.ProductLocation}#</span>
                                <input type"text" size="2" name="binLocationNumber" value="${picklistBinInfo.picklistBin.binLocationNumber}"/>
                                <span class="label">${uiLabelMap.PageTitlePickList}</span>
                                <select name="picklistId">
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
                                    <tr class="header-row"
                                        <td>${uiLabelMap.ProductOrderId}</td>
                                        <td>${uiLabelMap.ProductOrderShipGroupId}</td>
                                        <td>${uiLabelMap.ProductOrderItem}</td>
                                        <td>${uiLabelMap.ProductProduct}</td>
                                        <td>${uiLabelMap.ProductInventoryItem}</td>
                                        <td>${uiLabelMap.ProductLocation}</td>
                                        <td>${uiLabelMap.ProductQuantity}</td>
                                        <td>&nbsp;</td>
                                    </tr>
                                <#assign alt_row = false>
                                <#list picklistBinInfo.picklistItemInfoList?if_exists as picklistItemInfo>
                                <#assign picklistItem = picklistItemInfo.picklistItem>
                                <#assign inventoryItemAndLocation = picklistItemInfo.inventoryItemAndLocation>
                                    <tr valign="middle"<#if alt_row> class="alternate-row"</#if>>
                                        <td>${picklistItem.orderId}</td>
                                        <td>${picklistItem.shipGroupSeqId}</td>
                                        <td>${picklistItem.orderItemSeqId}</td>
                                        <td>${picklistItemInfo.orderItem.productId}<#if picklistItemInfo.orderItem.productId != inventoryItemAndLocation.productId>&nbsp;[${inventoryItemAndLocation.productId}]</#if></td>
                                        <td>${inventoryItemAndLocation.inventoryItemId}</td>
                                        <td>${inventoryItemAndLocation.areaId?if_exists}-${inventoryItemAndLocation.aisleId?if_exists}-${inventoryItemAndLocation.sectionId?if_exists}-${inventoryItemAndLocation.levelId?if_exists}-${inventoryItemAndLocation.positionId?if_exists}</td>
                                        <td>${picklistItem.quantity}</td>
                                        <#if !picklistItemInfo.itemIssuanceList?has_content>
                                            <td>
                                                <form name="deletePicklistItem_${picklist.picklistId}_${picklistItem.orderId}_${picklistItemInfo_index}" method="post" action="<@ofbizUrl>deletePicklistItem</@ofbizUrl>">
                                                    <input type="hidden" name="picklistBinId" value="${picklistItemInfo.picklistItem.picklistBinId}"/>
                                                    <input type="hidden" name="orderId" value= "${picklistItemInfo.picklistItem.orderId}"/>
                                                    <input type="hidden" name="orderItemSeqId" value="${picklistItemInfo.picklistItem.orderItemSeqId}"/>
                                                    <input type="hidden" name="shipGroupSeqId" value="${picklistItemInfo.picklistItem.shipGroupSeqId}"/>
                                                    <input type="hidden" name="inventoryItemId" value="${picklistItemInfo.picklistItem.inventoryItemId}"/>
                                                    <input type="hidden" name="facilityId" value="${facilityId?if_exists}"/>
                                                    <a href='javascript:document.deletePicklistItem_${picklist.picklistId}_${picklistItem.orderId}_${picklistItemInfo_index}.submit()' class='buttontext'>&nbsp;${uiLabelMap.CommonDelete}&nbsp;</a>
                                                </form>
                                            </td>
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
                                    <#-- toggle the row color -->
                                    <#assign alt_row = !alt_row>
                                </#list>
                                </table>
                            </div>
                        </#if>
                    </#if>
                </#list>
                <#if picklistInfo_has_next>
                   <hr />
                </#if>
            </#list>
        <#else/>
            <h3>${uiLabelMap.ProductNoPicksStarted}.</h3>
        </#if>
    </div>
</div>
