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
        <ul>
            <li class="h3">${uiLabelMap.PageTitleAddItemsFromOrder}</li>
        </ul>
        <br class="clear"/>
    </div>
    <div class="screenlet-body">
        <form name="additemsfromorder" action="<@ofbizUrl>AddItemsFromOrder</@ofbizUrl>">
            <input type="hidden" name="shipmentId" value="${shipmentId}"/>
            <div>
                <span class="label">${uiLabelMap.ProductOrderId}</span>
                <input type="text" size="20" name="orderId" value="${orderId?if_exists}"/>
                <span>
                    <a href="javascript:call_fieldlookup2(document.additemsfromorder.orderId,'LookupOrderHeaderAndShipInfo');">
                        <img src='/images/fieldlookup.gif' width='15' height='14' border='0' alt="${uiLabelMap.CommonClickHereForFieldLookup}">
                    </a>
                </span>
                <span class="label">${uiLabelMap.ProductOrderShipGroupId}</span>
                <input type="text" size="20" name="shipGroupSeqId" value="${shipGroupSeqId?if_exists}"/>
                <input type="submit" value="${uiLabelMap.CommonSelect}" class="smallSubmit"/>
            </div>
        </form>
    </div>
</div>
<div class="screenlet">
    <div class="screenlet-title-bar">
        <ul>
            <li class="h3">${uiLabelMap.ProductAddItemsShipment}: [${shipmentId?if_exists}]; ${uiLabelMap.ProductFromAnOrder}: [${orderId?if_exists}], ${uiLabelMap.ProductOrderShipGroupId}: [${shipGroupSeqId?if_exists}]</li>
        </ul>
        <br class="clear"/>
    </div>
    <div class="screenlet-body">
    <#if orderId?has_content && !orderHeader?exists>
        <h3 style="color: red;"><#assign uiLabelWithVar=uiLabelMap.ProductErrorOrderIdNotFound?interpret><@uiLabelWithVar/>.</h3>
    </#if>
    <#if orderHeader?exists>
        <#if orderHeader.orderTypeId == "SALES_ORDER" && shipment.shipmentTypeId?if_exists != "SALES_SHIPMENT">
            <h3 style="color: red;">${uiLabelMap.ProductWarningOrderType} ${(orderType.get("description",locale))?default(orderHeader.orderTypeId?if_exists)}, ${uiLabelMap.ProductNotSalesShipment}.</h3>
        <#elseif orderHeader.orderTypeId == "PURCHASE_ORDER" && shipment.shipmentTypeId?if_exists != "PURCHASE_SHIPMENT" && shipment.shipmentTypeId?if_exists != "DROP_SHIPMENT">
            <h3 style="color: red;">${uiLabelMap.ProductWarningOrderType} ${(orderType.get("description",locale))?default(orderHeader.orderTypeId?if_exists)}, ${uiLabelMap.ProductNotPurchaseShipment}.</h3>
        <#else>
            <h3>${uiLabelMap.ProductNoteOrderType} ${(orderType.get("description",locale))?default(orderHeader.orderTypeId?if_exists)}.</h3>
            <h3>${uiLabelMap.ProductShipmentType}: ${shipment.shipmentTypeId?if_exists}.</h3>
        </#if>
        <#if shipment.shipmentTypeId?if_exists == "SALES_SHIPMENT">
            <h3>${uiLabelMap.ProductOriginFacilityIs}: <#if originFacility?exists>${originFacility.facilityName?if_exists} [${originFacility.facilityId}]<#else><span style="color: red;">${uiLabelMap.ProductNotSet}</span></#if></h3>
        <#elseif shipment.shipmentTypeId?if_exists == "PURCHASE_SHIPMENT">
            <h3>${uiLabelMap.ProductDestinationFacilityIs}: <#if destinationFacility?exists>${destinationFacility.facilityName?if_exists} [${destinationFacility.facilityId}]<#else><span style="color: red;">${uiLabelMap.ProductNotSet}</span></#if></h3>
        </#if>
        <#if "ORDER_APPROVED" == orderHeader.statusId || "ORDER_BACKORDERED" == orderHeader.statusId>
            <h3>${uiLabelMap.ProductNoteOrderStatus} ${(orderHeaderStatus.get("description",locale))?default(orderHeader.statusId?if_exists)}.</h3>
        <#elseif "ORDER_COMPLETED" == orderHeader.statusId>
            <h3>${uiLabelMap.ProductNoteOrderStatus} ${(orderHeaderStatus.get("description",locale))?default(orderHeader.statusId?if_exists)}, ${uiLabelMap.ProductNoItemsLeft}.</h3>
        <#else>
            <h3 style="color: red;">${uiLabelMap.ProductWarningOrderStatus} ${(orderHeaderStatus.get("description",locale))?default(orderHeader.statusId?if_exists)}; ${uiLabelMap.ProductApprovedBeforeShipping}.</h3>
        </#if>
    </#if>
    <br />
    <#if orderItemDatas?exists>
        <#assign rowCount = 0>
        <#if isSalesOrder>
            <form action="<@ofbizUrl>issueOrderItemShipGrpInvResToShipment</@ofbizUrl>" method="post" name="selectAllForm">
        <#else>
            <form action="<@ofbizUrl>issueOrderItemToShipment</@ofbizUrl>" method="post" name="selectAllForm">
        </#if>
        <input type="hidden" name="shipmentId" value="${shipmentId}">
        <input type="hidden" name="_useRowSubmit" value="Y">
        <table cellspacing="0" cellpadding="2" class="basic-table hover-bar">
            <tr class="header-row">
                <td>${uiLabelMap.ProductOrderId}<br />${uiLabelMap.ProductOrderShipGroupId}<br />${uiLabelMap.ProductOrderItem}</td>
                <td>${uiLabelMap.ProductProduct}</td>
                <#if isSalesOrder>
                    <td>${uiLabelMap.ProductItemsIssuedReserved}</td>
                    <td>${uiLabelMap.ProductIssuedReservedTotalOrdered}</td>
                    <td>${uiLabelMap.ProductReserved}</td>
                    <td>${uiLabelMap.ProductNotAvailable}</td>
                <#else>
                    <td>${uiLabelMap.ProductItemsIssued}</td>
                    <td>${uiLabelMap.ProductIssuedOrdered}</td>
                </#if>
                <td>${uiLabelMap.ProductIssue}</td>
                <td align="right">
                    <div>${uiLabelMap.CommonSubmit} ?</div>
                    <div>${uiLabelMap.CommonAll}<input type="checkbox" name="selectAll" value="${uiLabelMap.CommonY}" onclick="javascript:toggleAll(this, 'selectAllForm');highlightAllRows(this, 'orderItemData_tableRow_', 'selectAllForm');"></div>
                </td>
            </tr>
            <#assign alt_row = false>
            <#list orderItemDatas?if_exists as orderItemData>
                <#assign orderItemAndShipGroupAssoc = orderItemData.orderItemAndShipGroupAssoc>
                <#assign product = orderItemData.product?if_exists>
                <#assign itemIssuances = orderItemData.itemIssuances>
                <#assign totalQuantityIssued = orderItemData.totalQuantityIssued>
                <#assign orderItemShipGrpInvResDatas = orderItemData.orderItemShipGrpInvResDatas?if_exists>
                <#assign totalQuantityReserved = orderItemData.totalQuantityReserved?if_exists>
                <#assign totalQuantityIssuedAndReserved = orderItemData.totalQuantityIssuedAndReserved?if_exists>
                <tr id="orderItemData_tableRow_${rowCount}" valign="middle"<#if alt_row> class="alternate-row"</#if>>
                    <td><div>${orderItemAndShipGroupAssoc.orderId} / ${orderItemAndShipGroupAssoc.shipGroupSeqId} / ${orderItemAndShipGroupAssoc.orderItemSeqId}</div></td>
                    <td><div>${(product.internalName)?if_exists} [${orderItemAndShipGroupAssoc.productId?default("N/A")}]</div></td>
                    <td>
                        <#if itemIssuances?has_content>
                            <#list itemIssuances as itemIssuance>
                                <div><b>[${itemIssuance.quantity?if_exists}]</b>${itemIssuance.shipmentId?if_exists}:${itemIssuance.shipmentItemSeqId?if_exists} ${uiLabelMap.CommonOn} [${(itemIssuance.issuedDateTime.toString())?if_exists}] ${uiLabelMap.CommonBy} [${(itemIssuance.issuedByUserLoginId)?if_exists}]</div>
                            </#list>
                        <#else>
                            <div>&nbsp;</div>
                        </#if>
                    </td>
                    <td>
                        <div>
                            <#if isSalesOrder>
                                <#if (totalQuantityIssuedAndReserved != orderItemAndShipGroupAssoc.quantity)>
                                <span style="color: red;">
                                <#else>
                                <span>
                                </#if>
                                    [${totalQuantityIssued} + ${totalQuantityReserved} = ${totalQuantityIssuedAndReserved}]
                                    <b>
                                        <#if (totalQuantityIssuedAndReserved > orderItemAndShipGroupAssoc.quantity)>&gt;<#else><#if (totalQuantityIssuedAndReserved < orderItemAndShipGroupAssoc.quantity)>&lt;<#else>=</#if></#if>
                                        ${orderItemAndShipGroupAssoc.quantity}
                                    </b>
                                </span>
                            <#else>
                                <#if (totalQuantityIssued > orderItemAndShipGroupAssoc.quantity)>
                                <span style="color: red;">
                                <#else>
                                <span>
                                </#if>
                                    ${totalQuantityIssued}
                                    <b>
                                        <#if (totalQuantityIssued > orderItemAndShipGroupAssoc.quantity)>&gt;<#else><#if (totalQuantityIssued < orderItemAndShipGroupAssoc.quantity)>&lt;<#else>=</#if></#if>
                                        ${orderItemAndShipGroupAssoc.quantity}
                                    </b>
                                </span>
                            </#if>
                        </div>
                    </td>
                    <#if isSalesOrder>
                        <td>&nbsp;</td>
                        <td>&nbsp;</td>
                        <td>&nbsp;</td>
                        <td>&nbsp;</td>
                    <#else>
                        <#assign quantityNotIssued = orderItemAndShipGroupAssoc.quantity - totalQuantityIssued>
                        <#if (quantityNotIssued > 0)>
                            <td>
                                <input type="hidden" name="shipmentId_o_${rowCount}" value="${shipmentId}"/>
                                <input type="hidden" name="orderId_o_${rowCount}" value="${orderItemAndShipGroupAssoc.orderId}"/>
                                <input type="hidden" name="shipGroupSeqId_o_${rowCount}" value="${orderItemAndShipGroupAssoc.shipGroupSeqId}"/>
                                <input type="hidden" name="orderItemSeqId_o_${rowCount}" value="${orderItemAndShipGroupAssoc.orderItemSeqId}"/>
                                <input type="text" size="5" name="quantity_o_${rowCount}" value="${quantityNotIssued}"/>
                            </td>
                            <td align="right">
                              <input type="checkbox" name="_rowSubmit_o_${rowCount}" value="Y" onclick="javascript:checkToggle(this, 'selectAllForm');highlightRow(this,'orderItemData_tableRow_${rowCount}');">
                            </td>
                            <#assign rowCount = rowCount + 1>
                        <#else>
                            <td>&nbsp;</td>
                            <td>&nbsp;</td>
                        </#if>
                    </#if>
                </tr>
                <#if isSalesOrder>
                    <#list orderItemShipGrpInvResDatas as orderItemShipGrpInvResData>
                        <#assign orderItemShipGrpInvRes = orderItemShipGrpInvResData.orderItemShipGrpInvRes>
                        <#assign inventoryItem = orderItemShipGrpInvResData.inventoryItem>
                        <#assign inventoryItemFacility = orderItemShipGrpInvResData.inventoryItemFacility>
                        <#assign availableQuantity = orderItemShipGrpInvRes.quantity - (orderItemShipGrpInvRes.quantityNotAvailable?default(0))>
                        <#if availableQuantity < 0>
                            <#assign availableQuantity = 0>
                        </#if>
                        <tr id="orderItemData_tableRow_${rowCount}">
                            <td>&nbsp;</td>
                            <td>&nbsp;</td>
                            <td>
                                <div>
                                    ${orderItemShipGrpInvRes.inventoryItemId}
                                    <#if inventoryItem.facilityId?has_content>
                                        <span<#if originFacility?exists && originFacility.facilityId != inventoryItem.facilityId> style="color: red;"</#if>>[${(inventoryItemFacility.facilityName)?default(inventoryItem.facilityId)}]</span>
                                    <#else>
                                        <span style="color: red;">[${uiLabelMap.ProductNoFacility}]</span>
                                    </#if>
                                </div>
                            </td>
                            <td>&nbsp;</td>
                            <td>${orderItemShipGrpInvRes.quantity}</td>
                            <td>${orderItemShipGrpInvRes.quantityNotAvailable?default("&nbsp;")}</td>
                            <#if originFacility?exists && originFacility.facilityId == inventoryItem.facilityId?if_exists>
                                <td>
                                    <input type="hidden" name="shipmentId_o_${rowCount}" value="${shipmentId}"/>
                                    <input type="hidden" name="orderId_o_${rowCount}" value="${orderItemShipGrpInvRes.orderId}"/>
                                    <input type="hidden" name="shipGroupSeqId_o_${rowCount}" value="${orderItemShipGrpInvRes.shipGroupSeqId}"/>
                                    <input type="hidden" name="orderItemSeqId_o_${rowCount}" value="${orderItemShipGrpInvRes.orderItemSeqId}"/>
                                    <input type="hidden" name="inventoryItemId_o_${rowCount}" value="${orderItemShipGrpInvRes.inventoryItemId}"/>
                                    <input type="text" size="5" name="quantity_o_${rowCount}" value="${(orderItemShipGrpInvResData.shipmentPlanQuantity)?default(availableQuantity)}"/>
                                </td>
                                <td align="right">
                                  <input type="checkbox" name="_rowSubmit_o_${rowCount}" value="Y" onclick="javascript:checkToggle(this, 'selectAllForm');highlightRow(this,'orderItemData_tableRow_${rowCount}');">
                                </td>
                                <#assign rowCount = rowCount + 1>
                            <#else>
                                <td>${uiLabelMap.ProductNotOriginFacility}</td>
                                <td>&nbsp;</td>
                            </#if>
                        </tr>
                    </#list>
                </#if>
                <#-- toggle the row color -->
                <#assign alt_row = !alt_row>
            </#list>
        </table>
        <div align="right"><input type="submit" class="smallSubmit" value="${uiLabelMap.ProductIssueAll}"/></div>
        <input type="hidden" name="_rowCount" value="${rowCount}">
        </form>
        <script language="JavaScript" type="text/javascript">selectAll('selectAllForm');</script>
    </#if>
    </div>
</div>
<#else>
<div class="screenlet">
    <div class="screenlet-title-bar">
        <ul>
            <li class="h3">${uiLabelMap.ProductShipmentNotFoundId}: [${shipmentId?if_exists}]</li>
        </ul>
        <br class="clear"/>
    </div>
</div>
</#if>