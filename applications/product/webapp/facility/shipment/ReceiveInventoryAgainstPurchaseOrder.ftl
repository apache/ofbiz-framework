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

    <#assign productId = parameters.productId?if_exists/>
    <div class="head3">${uiLabelMap.ProductReceiveInventoryAgainstPurchaseOrder}</div>

    <div class="errorMessage">
        <#if ! isPurchaseShipment>
            <#assign uiLabelWithVar=uiLabelMap.ProductErrorShipmentNotPurchaseShipment?interpret><@uiLabelWithVar/>
        <#elseif orderId?has_content && !orderHeader?exists>
            <#assign uiLabelWithVar=uiLabelMap.ProductErrorOrderIdNotFound?interpret><@uiLabelWithVar/>
        <#elseif orderHeader?exists && orderHeader.orderTypeId != "PURCHASE_ORDER">
            <#assign uiLabelWithVar=uiLabelMap.ProductErrorOrderNotPurchaseOrder?interpret><@uiLabelWithVar/>
        <#elseif ProductReceiveInventoryAgainstPurchaseOrderProductNotFound?exists>
            <#assign uiLabelWithVar=uiLabelMap.ProductReceiveInventoryAgainstPurchaseOrderProductNotFound?interpret><@uiLabelWithVar/>
        <#elseif ProductReceiveInventoryAgainstPurchaseOrderQuantityExceedsAvailableToReceive?exists>
            <#assign uiLabelWithVar=uiLabelMap.ProductReceiveInventoryAgainstPurchaseOrderQuantityExceedsAvailableToReceive?interpret><@uiLabelWithVar/>
        </#if>
    </div>

    <form name="ReceiveInventoryAgainstPurchaseOrder" action="<@ofbizUrl>ReceiveInventoryAgainstPurchaseOrder</@ofbizUrl>">
        <input type="hidden" name="clearAll" value="Y"/>
        <div class="tabletext">
            ${uiLabelMap.ProductShipmentId} : <input type="text" class='inputBox' size="20" name="shipmentId" value="${shipmentId?if_exists}"/>
            ${uiLabelMap.ProductOrderId} : <input type="text" class='inputBox' size="20" name="purchaseOrderId" value="${orderId?if_exists}"/>
            <span class='tabletext'>
                <a href="javascript:call_fieldlookup2(document.additemsfromorder.orderId,'LookupOrderHeaderAndShipInfo');">
                    <img src='/images/fieldlookup.gif' width='15' height='14' border='0' alt='Click here For Field Lookup'>
                </a>
            </span>
            ${uiLabelMap.ProductOrderShipGroupId} : <input type="text" class='inputBox' size="20" name="shipGroupSeqId" value="${shipGroupSeqId?if_exists}"/>
            <input type="submit" value="${uiLabelMap.CommonSelect}" class="smallSubmit"/>
        </div>
    </form>
    
    <#if isPurchaseShipment>
    
        <#assign itemsAvailableToReceive = totalAvailableToReceive?default(0) &gt; 0/>
        <#if orderItemDatas?exists>
            <#assign rowCount = 0>
            <#assign totalReadyToReceive = 0/>
            <form action="<@ofbizUrl>receiveInventoryProduct/ReceiveInventoryAgainstPurchaseOrder?clearAll=Y</@ofbizUrl>" method="post" name="selectAllForm">
                <input type="hidden" name="facilityId" value="${facilityId}"/>
                <input type="hidden" name="purchaseOrderId" value="${orderId}"/>
                <input type="hidden" name="shipmentId" value="${shipmentId}">
                <input type="hidden" name="_useRowSubmit" value="Y"/>
                <table width="100%" cellpadding="2" cellspacing="0" border="1">
                    <tr>
                        <td><div class="tableheadtext">${uiLabelMap.ProductProduct}</div></td>
                        <td><div class="tableheadtext">${uiLabelMap.OrderOrder}</div></td>
                        <td><div class="tableheadtext">${uiLabelMap.CommonReceived}</div></td>
                        <td><div class="tableheadtext">${uiLabelMap.ProductOpenQuantity}</div></td>
                        <td><div class="tableheadtext">${uiLabelMap.CommonReceive}</div></td>
                        <td><div class="tableheadtext">${uiLabelMap.ProductInventoryItemType}</div></td>
                        <#if itemsAvailableToReceive>
                            <td colspan="2" align="right">
                                <div class="tableheadtext">${uiLabelMap.CommonAll}<input type="checkbox" name="selectAll" value="${uiLabelMap.CommonY}" onclick="javascript:toggleAll(this, 'selectAllForm');"></div>
                            </td>
                        </#if>
                    </tr>
                    <#list orderItemDatas?if_exists as orderItemData>
                        <#assign orderItemAndShipGroupAssoc = orderItemData.orderItemAndShipGroupAssoc>
                        <#assign product = orderItemData.product?if_exists>
                        <#assign totalQuantityReceived = orderItemData.totalQuantityReceived?default(0)>
                        <#assign availableToReceive = orderItemData.availableToReceive?default(0)>
                        
                        <tr>
                            <td><div class="tabletext">${(product.internalName)?if_exists} [${orderItemAndShipGroupAssoc.productId?default("N/A")}]</div></td>
                            <td>
                                <div class="tabletext">
                                    ${orderItemAndShipGroupAssoc.quantity}
                                </div>
                            </td>
                            <td>
                                <div class="tabletext">${totalQuantityReceived}</div>
                            </td>
                            <td>
                                <div class="tabletext">
                                    ${orderItemAndShipGroupAssoc.quantity - totalQuantityReceived}
                                </div>
                            </td>
                            <#if availableToReceive &gt; 0 >
                                <td>
                                    <input type="hidden" name="productId_o_${rowCount}" value="${(product.productId)?if_exists}"/>
                                    <input type="hidden" name="facilityId_o_${rowCount}" value="${facilityId}"/>
                                    <input type="hidden" name="shipmentId_o_${rowCount}" value="${shipmentId}"/>
                                    <input type="hidden" name="orderId_o_${rowCount}" value="${orderItemAndShipGroupAssoc.orderId}"/>
                                    <input type="hidden" name="shipGroupSeqId_o_${rowCount}" value="${orderItemAndShipGroupAssoc.shipGroupSeqId}"/>
                                    <input type="hidden" name="orderItemSeqId_o_${rowCount}" value="${orderItemAndShipGroupAssoc.orderItemSeqId}"/>
                                    <input type="hidden" name="unitCost_o_${rowCount}" value="${orderItemAndShipGroupAssoc.unitPrice?default(0)}"/>
                                    <input type="hidden" name="currencyUomId_o_${rowCount}" value="${currencyUomId?default("")}"/>
                                    <input type="hidden" name="ownerPartyId_o_${rowCount}" value="${(facility.ownerPartyId)?if_exists}"/>
                                    <input type="hidden" name="datetimeReceived_o_${rowCount}" value="${now}"/>
                                    <input type="hidden" name="quantityRejected_o_${rowCount}" value="0"/>
                                    <#if itemQuantitiesToReceive?exists && itemQuantitiesToReceive.get(orderItemAndShipGroupAssoc.orderItemSeqId)?exists>
                                        <#assign quantityToReceive = itemQuantitiesToReceive.get(orderItemAndShipGroupAssoc.orderItemSeqId)>
                                    <#else>
                                        <#assign quantityToReceive = 0>
                                    </#if>
                                    <#assign totalReadyToReceive = totalReadyToReceive + quantityToReceive/>
                                    <input type="text" class='inputBox' size="5" name="quantityAccepted_o_${rowCount}" value="${quantityToReceive}"/>
                                </td>
                                <td>              
                                    <select name="inventoryItemTypeId_o_${rowCount}" class="selectBox">
                                      <#list inventoryItemTypes as inventoryItemType>
                                      <option value="${inventoryItemType.inventoryItemTypeId}"
                                          <#if (facility.defaultInventoryItemTypeId?has_content) && (inventoryItemType.inventoryItemTypeId == facility.defaultInventoryItemTypeId)>
                                              selected="selected"
                                          </#if>    
                                      >${inventoryItemType.get("description",locale)?default(inventoryItemType.inventoryItemTypeId)}</option>
                                      </#list>
                                    </select>
                                </td>
                                <td align="right">              
                                    <a href="<@ofbizUrl>ReceiveInventoryAgainstPurchaseOrder?shipmentId=${shipmentId}&purchaseOrderId=${orderId}&productId=${product.productId}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonClear}</a>
                                </td>
                                <td align="right">              
                                  <input type="checkbox" name="_rowSubmit_o_${rowCount}" value="Y" onclick="javascript:checkToggle(this, 'selectAllForm');">
                                </td>
                                <#assign rowCount = rowCount + 1>   
                            </#if>
                        </tr>
                    </#list>
                    <#if itemsAvailableToReceive>
                        <tr>
                            <td colspan="7" align="right">
                                <a href="<@ofbizUrl>ReceiveInventoryAgainstPurchaseOrder?shipmentId=${shipmentId}&purchaseOrderId=${orderId}&clearAll=Y</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonClearAll}</a>
                            </td>
                            <td align="right">
                                <input type="submit" class="smallSubmit" value="${uiLabelMap.ProductReceiveItem}"/>
                            </td>
                        </tr>
                    </#if>
                </table>
                <input type="hidden" name="_rowCount" value="${rowCount}">
            </form>
            <script language="JavaScript" type="text/javascript">selectAll('selectAllForm');</script>
        </#if>
        <#if itemsAvailableToReceive && totalReadyToReceive < totalAvailableToReceive>
            <div class="head3">${uiLabelMap.ProductReceiveInventoryAddProductToReceive}</div>
            <form name="addProductToReceive" method="post" action="<@ofbizUrl>ReceiveInventoryAgainstPurchaseOrder</@ofbizUrl>">
                <input type="hidden" name="shipmentId" value="${shipmentId}"/>
                <input type="hidden" name="purchaseOrderId" value="${orderId}"/>
                <div class="tabletext">
                    <span class="tabletext">
                        ${uiLabelMap.ProductProductId} <input type="text" class="inputBox" size="20" id="productId" name="productId" value=""/>
                        @
                        <input type="text" class="inputBox" name="quantity" size="6" maxlength="6" value="1" tabindex="0"/>
                        <input type="submit" value="${uiLabelMap.CommonAdd}" class="smallSubmit"/>
                    </span>
                </div>
            </form>
            <script language="javascript">
                document.getElementById('productId').focus();
            </script>
        </#if>
    </#if>
<#else>
  <h3>${uiLabelMap.ProductShipmentNotFoundId}: [${shipmentId?if_exists}]</h3>
</#if>
