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

<table class="basic-table hover-bar" cellspacing="0">
  <tr class="header-row-2">
    <td>${uiLabelMap.ProductInventoryItemId}</td>
    <td>${uiLabelMap.ProductFacilityId}</td>
    <td>${uiLabelMap.ProductLocationSeqId}</td>
    <td>${uiLabelMap.ProductQoh}</td>
    <td>${uiLabelMap.ProductAtp}</td>
    <td>${uiLabelMap.FormFieldTitle_unitCost}</td>
  </tr>
  <tr><td colspan="6"><hr /></td></tr>
  <#if (inventoryItemsForPo?? && inventoryItemsForPo?has_content)>
    <tr class="header-row-2"><td colspan="6"><span class="label">&nbsp;${uiLabelMap.ProductInventoryItemsFor} ${uiLabelMap.ProductPurchaseOrder} - ${orderId}</span></td></tr>
    <#list inventoryItemsForPo as inventoryItem>
      <tr>
        <td><a class="buttontext" href="javascript:set_value('${inventoryItem.inventoryItemId}')">${inventoryItem.inventoryItemId}</a></td>
        <td>${inventoryItem.facilityId!}</td>
        <td>${inventoryItem.locationSeqId!}</td>
        <td>${inventoryItem.quantityOnHandTotal!}</td>
        <td>${inventoryItem.availableToPromiseTotal!}</td>
        <td>${inventoryItem.unitCost!}</td>
      </tr>
    </#list>
  </#if>
  <#if (inventoryItemsForSupplier?? && inventoryItemsForSupplier?has_content)>
    <tr class="header-row-2"><td colspan="6"><span class="label centered">&nbsp;${uiLabelMap.ProductInventoryItemsFor} ${uiLabelMap.ProductSupplier} - ${partyId}</span></td></tr>
    <#list inventoryItemsForSupplier as inventoryItem>
      <tr>
        <td><a class="buttontext" href="javascript:set_value('${inventoryItem.inventoryItemId}')">${inventoryItem.inventoryItemId}</a></td>
        <td>${inventoryItem.facilityId!}</td>
        <td>${inventoryItem.locationSeqId!}</td>
        <td>${inventoryItem.quantityOnHandTotal!}</td>
        <td>${inventoryItem.availableToPromiseTotal!}</td>
        <td>${inventoryItem.unitCost!}</td>
      </tr>
    </#list>
  </#if>
  <#if (inventoryItemsForProduct?? && inventoryItemsForProduct?has_content)>
    <tr class="header-row-2"><td colspan="6"><span class="label">&nbsp;${uiLabelMap.ProductInventoryItemsFor} ${uiLabelMap.ProductProduct} - ${internalName!} [${productId}]</span></td></tr>
    <#list inventoryItemsForProduct as inventoryItem>
      <tr>
        <td><a class="buttontext" href="javascript:set_value('${inventoryItem.inventoryItemId}')">${inventoryItem.inventoryItemId}</a></td>
        <td>${inventoryItem.facilityId!}</td>
        <td>${inventoryItem.locationSeqId!}</td>
        <td>${inventoryItem.quantityOnHandTotal!}</td>
        <td>${inventoryItem.availableToPromiseTotal!}</td>
        <td>${inventoryItem.unitCost!}</td>
      </tr>
    </#list>
    <tr>
  </#if>
  <#if !(inventoryItemsForPo?? && inventoryItemsForPo?has_content) && !(inventoryItemsForSupplier?? && inventoryItemsForSupplier?has_content) && !(inventoryItemsForProduct?? && inventoryItemsForProduct?has_content)>
    <tr><td><span class="label">${uiLabelMap.CommonNo} ${uiLabelMap.ProductInventoryItems} ${uiLabelMap.ProductAvailable}.</span></td></tr>
  </#if>
</table>
