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
      <li class="h3">${uiLabelMap.ProductIssueInventoryItemsToShipment}: [${shipmentId!}]</li>
    </ul>
  </div>
  <div class="screenlet-body">
    <table cellspacing="0" cellpadding="2" class="basic-table hover-bar">
      <tr class="header-row">
        <td>${uiLabelMap.CommonReturn} ${uiLabelMap.CommonDescription}</td>
        <td>${uiLabelMap.ProductProduct}</td>
        <td>${uiLabelMap.OrderReturnQty}</td>
        <td>${uiLabelMap.ProductShipmentQty}</td>
        <td>${uiLabelMap.ProductTotIssuedQuantity}</td>
        <td></td>
        <td>${uiLabelMap.CommonQty} ${uiLabelMap.CommonNot} ${uiLabelMap.ManufacturingIssuedQuantity}</td>
        <td>${uiLabelMap.ProductInventoryItemId} ${uiLabelMap.CommonQty} ${uiLabelMap.CommonSubmit}</td>
      </tr>
      <#list items as item>
        <tr>
          <td><a href="/ordermgr/control/returnMain?returnId=${item.returnId}" class="buttontext">${item.returnId}</a> [${item.returnItemSeqId}]</td>
          <td><a href="/catalog/control/EditProductInventoryItems?productId=${item.productId}" class="buttontext">${item.productId}</a> ${item.internalName!}</td>
          <td>${item.returnQuantity}</td>
          <td>${item.shipmentItemQty}</td>
          <td>${item.totalQtyIssued}</td>
          <td>
            <#if item.issuedItems?has_content>
              <#list item.issuedItems as issuedItem>
                <div><a href="/facility/control/EditInventoryItem?inventoryItemId=${issuedItem.inventoryItemId}" class="buttontext">${issuedItem.inventoryItemId}</a> ${issuedItem.quantity}</div>
              </#list>
            </#if>
          </td>
          <td>${item.qtyStillNeedToBeIssued}</td>
          <#if (item.shipmentItemQty > item.totalQtyIssued)>
            <td>
              <div>
                <form name="issueInventoryItemToShipment_${item_index}" action="<@ofbizUrl>issueInventoryItemToShipment</@ofbizUrl>" method="post">
                  <input type="hidden" name="shipmentId" value="${shipmentId}"/>
                  <input type="hidden" name="shipmentItemSeqId" value="${item.shipmentItemSeqId}"/>
                  <input type="hidden" name="totalIssuedQty" value="${item.totalQtyIssued}"/>
                  <span>
                    <@htmlTemplate.lookupField formName="issueInventoryItemToShipment_${item_index}" name="inventoryItemId" id="inventoryItemId" fieldFormName="LookupInventoryItem?orderId=${item.orderId}&partyId=${item.partyId}&productId=${item.productId}"/>
                  </span>
                  <input type="text" size="5" name="quantity"/>
                  <input type="submit" value="${uiLabelMap.CommonSubmit}" class="smallSubmit"/>
                </form>
              </div>
            </td>
          </#if>
        </tr>
      </#list>
    </table>
  </div>
</div>
