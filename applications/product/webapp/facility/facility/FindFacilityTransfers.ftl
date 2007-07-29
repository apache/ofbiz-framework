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

    <span class="head1">${uiLabelMap.ProductInventoryTransfersFor}</span> <span class="head2"><#if facility?exists>${(facility.facilityName)?if_exists}</#if> [${uiLabelMap.CommonId}:${facilityId?if_exists}]</span>
    <div class="button-bar">
      <a href="<@ofbizUrl>EditFacility</@ofbizUrl>" class="buttontext">${uiLabelMap.ProductNewFacility}</a>
      <#if activeOnly>
          <a href="<@ofbizUrl>FindFacilityTransfers?facilityId=${facilityId}&activeOnly=false</@ofbizUrl>" class="buttontext">${uiLabelMap.ProductActiveAndInactive}</a>
      <#else>
          <a href="<@ofbizUrl>FindFacilityTransfers?facilityId=${facilityId}&activeOnly=true</@ofbizUrl>" class="buttontext">${uiLabelMap.ProductActiveOnly}</a>
      </#if>
      <a href="<@ofbizUrl>FindFacilityTransfers?facilityId=${facilityId}&completeRequested=true</@ofbizUrl>" class="buttontext">${uiLabelMap.ProductCompleteRequestedTransfers}</a>
      <a href="<@ofbizUrl>TransferInventoryItem?facilityId=${facilityId}</@ofbizUrl>" class="buttontext">${uiLabelMap.ProductInventoryTransfer}</a>
    </div>
    
    <br/>
    <#if (toTransfers.size() > 0)>
      <div class="screenlet">
        <div class="screenlet-title-bar">
          <h3>${uiLabelMap.CommonTo}: <#if facility?exists>${(facility.facilityName)?if_exists}</#if> [${uiLabelMap.CommonId}:${facilityId?if_exists}]</h3>
        </div>
        <table class="basic-table hover-bar" cellspacing="0">
          <tr class="header-row">
            <td><${uiLabelMap.ProductTransferId}</td>
            <td>${uiLabelMap.ProductItem}</td>      
            <td>${uiLabelMap.CommonFrom}</td>
            <td>${uiLabelMap.CommonSendDate}</td>
            <td>${uiLabelMap.CommonStatus}</td>
            <td>&nbsp;</td>
          </tr>
          <#assign alt_row = false>
          <#list toTransfers as transfer>
            <tr<#if alt_row> class="alternate-row"</#if>>
              <td><a href="<@ofbizUrl>TransferInventoryItem?inventoryTransferId=${(transfer.inventoryTransferId)?if_exists}</@ofbizUrl>" class="buttontext">&nbsp;${(transfer.inventoryTransferId)?if_exists}</a></td>
              <td><a href="<@ofbizUrl>EditInventoryItem?inventoryItemId=${(transfer.inventoryItemId)?if_exists}</@ofbizUrl>" class="buttontext">&nbsp;${(transfer.inventoryItemId)?if_exists}</a></td>
              <td>
                <#assign fac = delegator.findByPrimaryKey("Facility", Static["org.ofbiz.base.util.UtilMisc"].toMap("facilityId", transfer.getString("facilityId")))>
                <a href="<@ofbizUrl>EditFacility?facilityId=${(transfer.facilityId)?if_exists}</@ofbizUrl>" class="buttontext">&nbsp;<#if fac?exists>${(fac.facilityName)?if_exists}</#if>&nbsp;[${(transfer.facilityId)?if_exists}]</a>
              </td>
              <td>${(transfer.sendDate)?if_exists}</td>
              <td>
                <#if (transfer.statusId)?exists>
                    <#assign transferStatus = delegator.findByPrimaryKey("StatusItem", Static["org.ofbiz.base.util.UtilMisc"].toMap("statusId", transfer.statusId))>
                    ${(transferStatus.get("description",locale))?if_exists}
                </#if>
              </td>
              <td align="center"><a href="<@ofbizUrl>TransferInventoryItem?inventoryTransferId=${(transfer.inventoryTransferId)?if_exists}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonEdit}</a></td>
            </tr>
            <#assign alt_row = !alt_row>
          </#list>
        </table>
      </div>
    </#if>
    
    <#if (fromTransfers.size() > 0)>
    <#if completeRequested>
    <form name="CompleteRequested" method="post" action="CompleteRequestedTransfers?completeRequested=true&facilityId=${facility.facilityId}">
    </#if>
      <div class="screenlet">
        <div class="screenlet-title-bar">
          <h3>${uiLabelMap.CommonFrom}: <#if facility?exists>${(facility.facilityName)?if_exists}</#if> [${uiLabelMap.CommonId}:${facilityId?if_exists}]</h3>
        </div>
        <table class="basic-table" cellspacing="0">
          <tr class="header-row">
            <td>${uiLabelMap.ProductTransferId}<</td>
            <td>${uiLabelMap.ProductItem}</td>      
            <td>${uiLabelMap.ProductProductId}</td>      
            <td>${uiLabelMap.ProductInternalName}<</td>      
            <td>${uiLabelMap.ProductSerialAtpQoh}</td>      
            <td>${uiLabelMap.CommonTo}</td>
            <td><${uiLabelMap.CommonSendDate}</td>
            <#if !completeRequested>
              <td><${uiLabelMap.CommonStatus}</td>
            </#if>
            <td align="center">
              <#if completeRequested>
                Select<br><input name="selectAll" value="Y" onclick="javascript:toggleAll(this, 'CompleteRequested');" type="checkbox">
              <#else>
                &nbsp;
              </#if>
            </td>
          </tr>
        
          <#assign alt_row = false>
          <#list fromTransfers as transfer>
            <#assign inventoryItem = transfer.getRelatedOne("InventoryItem")?if_exists>
            <#if inventoryItem?has_content>
              <#assign product = inventoryItem.getRelatedOne("Product")?if_exists>
            </#if>
            <tr<#if alt_row> class="alternate-row"</#if>>
            <td><a href="<@ofbizUrl>TransferInventoryItem?inventoryTransferId=${(transfer.inventoryTransferId)?if_exists}</@ofbizUrl>" class="buttontext">&nbsp;${(transfer.inventoryTransferId)?if_exists}</a></td>
            <td><a href="<@ofbizUrl>EditInventoryItem?inventoryItemId=${(transfer.inventoryItemId)?if_exists}</@ofbizUrl>" class="buttontext">&nbsp;${(transfer.inventoryItemId)?if_exists}</a></td>
            <td>${(product.productId)?default("&nbsp;")}</td>
            <td>${(product.internalName)?default("&nbsp;")}</td>
            <td>
              <#if inventoryItem?exists && inventoryItem.inventoryItemTypeId.equals("NON_SERIAL_INV_ITEM")>
                ${(inventoryItem.availableToPromiseTotal)?if_exists}&nbsp;/&nbsp;${(inventoryItem.quantityOnHandTotal)?if_exists}
              <#elseif inventoryItem?exists && inventoryItem.inventoryItemTypeId.equals("SERIALIZED_INV_ITEM")>
                ${inventoryItem.serialNumber?if_exists}
              </#if>
            </td>
            <td>
                <#assign fac = delegator.findByPrimaryKey("Facility", Static["org.ofbiz.base.util.UtilMisc"].toMap("facilityId", transfer.getString("facilityIdTo")))>
                <a href="<@ofbizUrl>EditFacility?facilityId=${(transfer.facilityIdTo)?if_exists}</@ofbizUrl>" class="buttontext">&nbsp;<#if fac?exists>${(fac.facilityName)?if_exists}</#if>&nbsp;[${(transfer.facilityIdTo)?if_exists}]</a>
            </td>
            <td>${(transfer.sendDate)?default("&nbsp;")}</td>
            <#if !completeRequested>
            <td>
                <#if (transfer.statusId)?exists>
                    <#assign transferStatus = delegator.findByPrimaryKey("StatusItem", Static["org.ofbiz.base.util.UtilMisc"].toMap("statusId", transfer.statusId))>
                    ${(transferStatus.get("description",locale))?default("&nbsp;")}
                </#if>
            </td>
            </#if>
            <td align="center">
                <#if completeRequested>
                <input type="hidden" name="inventoryTransferId_o_${transfer_index}" value="${transfer.inventoryTransferId}">
                <input type="hidden" name="inventoryItemId_o_${transfer_index}" value="${transfer.inventoryItemId}">
                <input type="hidden" name="statusId_o_${transfer_index}" value="IXF_COMPLETE">
                <input name="_rowSubmit_o_${transfer_index}" value="Y" type="checkbox">
                <#else>
                <a href="<@ofbizUrl>TransferInventoryItem?inventoryTransferId=${(transfer.inventoryTransferId)?if_exists}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonEdit}</a>
                </#if>
            </td>
            </tr>
            <#assign alt_row = !alt_row>
            <#assign rowCount = transfer_index + 1>
          </#list>
          <#if completeRequested>
            <tr>
              <td colspan="8" align="right">
                <input type="hidden" name="_rowCount" value="${rowCount}">
                <input type="hidden" name="_useRowSubmit" value="Y"/>
                <input type="submit" value="${uiLabelMap.ProductComplete}"/>
              </td>
            </tr>
          </#if>
        </table>
      </div>
      <#if completeRequested>
      </form>
      </#if>
    </#if>
