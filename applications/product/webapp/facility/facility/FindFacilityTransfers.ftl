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

    <div class="head1">${uiLabelMap.ProductInventoryTransfersFor} <span class="head2"><#if facility?exists>${(facility.facilityName)?if_exists}</#if> [${uiLabelMap.CommonId}:${facilityId?if_exists}]</span></div>
    <a href="<@ofbizUrl>EditFacility</@ofbizUrl>" class="buttontext">[${uiLabelMap.ProductNewFacility}]</a>
    <#if activeOnly>
        <a href="<@ofbizUrl>FindFacilityTransfers?facilityId=${facilityId}&activeOnly=false</@ofbizUrl>" class="buttontext">[${uiLabelMap.ProductActiveAndInactive}]</a>
    <#else>
        <a href="<@ofbizUrl>FindFacilityTransfers?facilityId=${facilityId}&activeOnly=true</@ofbizUrl>" class="buttontext">[${uiLabelMap.ProductActiveOnly}]</a>
    </#if>
    <a href="<@ofbizUrl>FindFacilityTransfers?facilityId=${facilityId}&completeRequested=true</@ofbizUrl>" class="buttontext">[${uiLabelMap.ProductCompleteRequestedTransfers}]</a>
    <a href="<@ofbizUrl>TransferInventoryItem?facilityId=${facilityId}</@ofbizUrl>" class="buttontext">[${uiLabelMap.ProductInventoryTransfer}]</a>
    
    <br/>
    <#if (toTransfers.size() > 0)>
        <br/>
        <div class="head1">${uiLabelMap.CommonTo}:<span class="head2">&nbsp;<#if facility?exists>${(facility.facilityName)?if_exists}</#if> [${uiLabelMap.CommonId}:${facilityId?if_exists}]</span></div>
        <table border="1" cellpadding="2" cellspacing="0" width="100%">
            <tr>
            <td><div class="tabletext"><b>${uiLabelMap.ProductTransferId}</b></div></td>
            <td><div class="tabletext"><b>${uiLabelMap.ProductItem}</b></div></td>      
            <td><div class="tabletext"><b>${uiLabelMap.CommonFrom}</b></div></td>
            <td><div class="tabletext"><b>${uiLabelMap.CommonSendDate}</b></div></td>
            <td><div class="tabletext"><b>${uiLabelMap.CommonStatus}</b></div></td>
            <td>&nbsp;</td>
            </tr>
        
            <#list toTransfers as transfer>
            <tr>
            <td><div class="tabletext"><a href="<@ofbizUrl>TransferInventoryItem?inventoryTransferId=${(transfer.inventoryTransferId)?if_exists}</@ofbizUrl>" class="buttontext">&nbsp;${(transfer.inventoryTransferId)?if_exists}</a></div></td>
            <td><div class="tabletext"><a href="<@ofbizUrl>EditInventoryItem?inventoryItemId=${(transfer.inventoryItemId)?if_exists}</@ofbizUrl>" class="buttontext">&nbsp;${(transfer.inventoryItemId)?if_exists}</a></div></td>      
            <td>
                <#assign fac = delegator.findByPrimaryKey("Facility", Static["org.ofbiz.base.util.UtilMisc"].toMap("facilityId", transfer.getString("facilityId")))>
                <div class="tabletext"><a href="<@ofbizUrl>EditFacility?facilityId=${(transfer.facilityId)?if_exists}</@ofbizUrl>" class="buttontext">&nbsp;<#if fac?exists>${(fac.facilityName)?if_exists}</#if>&nbsp;[${(transfer.facilityId)?if_exists}]</a></div>
            </td>
            <td><div class="tabletext">&nbsp;${(transfer.sendDate)?if_exists}</div></td>
            <td>
                <#if (transfer.statusId)?exists>
                    <#assign transferStatus = delegator.findByPrimaryKey("StatusItem", Static["org.ofbiz.base.util.UtilMisc"].toMap("statusId", transfer.statusId))>
                    <div class="tabletext">&nbsp;${(transferStatus.get("description",locale))?if_exists}</div>
                </#if>
            </td>
            <td align="center"><div class="tabletext"><a href="<@ofbizUrl>TransferInventoryItem?inventoryTransferId=${(transfer.inventoryTransferId)?if_exists}</@ofbizUrl>" class="buttontext">[${uiLabelMap.CommonEdit}]</a></div></td>
            </tr>
            </#list>
        </table>
    </#if>
    
    <#if (fromTransfers.size() > 0)>
    <#if completeRequested>
    <form name="CompleteRequested" method="post" action="CompleteRequestedTransfers?completeRequested=true&facilityId=${facility.facilityId}">
    </#if>
        <br/>
        <div class="head1">${uiLabelMap.CommonFrom}:<span class="head2">&nbsp;<#if facility?exists>${(facility.facilityName)?if_exists}</#if> [${uiLabelMap.CommonId}:${facilityId?if_exists}]</span></div>
        <table border="1" cellpadding="2" cellspacing="0" width="100%">
            <tr>
            <td><div class="tabletext"><b>${uiLabelMap.ProductTransferId}</b></div></td>
            <td><div class="tabletext"><b>${uiLabelMap.ProductItem}</b></div></td>      
            <td><div class="tabletext"><b>${uiLabelMap.ProductProductId}</b></div></td>      
            <td><div class="tabletext"><b>${uiLabelMap.ProductInternalName}</b></div></td>      
            <td><div class="tabletext"><b>${uiLabelMap.ProductSerialAtpQoh}</b></div></td>      
            <td><div class="tabletext"><b>${uiLabelMap.CommonTo}</b></div></td>
            <td><div class="tabletext"><b>${uiLabelMap.CommonSendDate}</b></div></td>
            <#if !completeRequested>
            <td><div class="tabletext"><b>${uiLabelMap.CommonStatus}</b></div></td>
            </#if>
            <td align="center">
              <#if completeRequested>
              <span class="tableheadtext">Select<br><input name="selectAll" value="Y" onclick="javascript:toggleAll(this, 'CompleteRequested');" type="checkbox"></span>
              <#else>
              &nbsp;
              </#if>
            </td>
            </tr>
        
            <#list fromTransfers as transfer>
            <#assign inventoryItem = transfer.getRelatedOne("InventoryItem")?if_exists>
            <#if inventoryItem?has_content>
              <#assign product = inventoryItem.getRelatedOne("Product")?if_exists>
            </#if>
            <tr>
            <td><div class="tabletext"><a href="<@ofbizUrl>TransferInventoryItem?inventoryTransferId=${(transfer.inventoryTransferId)?if_exists}</@ofbizUrl>" class="buttontext">&nbsp;${(transfer.inventoryTransferId)?if_exists}</a></div></td>
            <td><div class="tabletext"><a href="<@ofbizUrl>EditInventoryItem?inventoryItemId=${(transfer.inventoryItemId)?if_exists}</@ofbizUrl>" class="buttontext">&nbsp;${(transfer.inventoryItemId)?if_exists}</a></div></td>      
            <td>
              <#if product?exists>
              <span class="tabletext">${product.productId}</span>
              </#if>
            </td>
            <td>
              <#if product?exists>
              <span class="tabletext">${product.internalName?if_exists}</span>
              </#if>
            </td>
            <td class="tabletext">
              <#if inventoryItem?exists && inventoryItem.inventoryItemTypeId.equals("NON_SERIAL_INV_ITEM")>
                ${(inventoryItem.availableToPromiseTotal)?if_exists}&nbsp;/&nbsp;${(inventoryItem.quantityOnHandTotal)?if_exists}
              <#elseif inventoryItem?exists && inventoryItem.inventoryItemTypeId.equals("SERIALIZED_INV_ITEM")>
                ${inventoryItem.serialNumber?if_exists}
              </#if>
            </td>
            <td>
                <#assign fac = delegator.findByPrimaryKey("Facility", Static["org.ofbiz.base.util.UtilMisc"].toMap("facilityId", transfer.getString("facilityIdTo")))>
                <div class="tabletext"><a href="<@ofbizUrl>EditFacility?facilityId=${(transfer.facilityIdTo)?if_exists}</@ofbizUrl>" class="buttontext">&nbsp;<#if fac?exists>${(fac.facilityName)?if_exists}</#if>&nbsp;[${(transfer.facilityIdTo)?if_exists}]</a></div>
            </td>
            <td><div class="tabletext">&nbsp;${(transfer.sendDate)?if_exists}</div></td>
            <#if !completeRequested>
            <td>
                <#if (transfer.statusId)?exists>
                    <#assign transferStatus = delegator.findByPrimaryKey("StatusItem", Static["org.ofbiz.base.util.UtilMisc"].toMap("statusId", transfer.statusId))>
                    <div class="tabletext">&nbsp;${(transferStatus.get("description",locale))?if_exists}</div>
                </#if>
            </td>
            </#if>
            <td align="center"><div class="tabletext">
                <#if completeRequested>
                <input type="hidden" name="inventoryTransferId_o_${transfer_index}" value="${transfer.inventoryTransferId}">
                <input type="hidden" name="inventoryItemId_o_${transfer_index}" value="${transfer.inventoryItemId}">
                <input type="hidden" name="statusId_o_${transfer_index}" value="IXF_COMPLETE">
                <input name="_rowSubmit_o_${transfer_index}" value="Y" type="checkbox">
                <#else>
                <a href="<@ofbizUrl>TransferInventoryItem?inventoryTransferId=${(transfer.inventoryTransferId)?if_exists}</@ofbizUrl>" class="buttontext">[${uiLabelMap.CommonEdit}]</a>
                </#if>
            </div></td>
            </tr>
            <#assign rowCount = transfer_index + 1>
            </#list>
            <#if completeRequested>
            <tr><td colspan="8" align="right">
                <input type="hidden" name="_rowCount" value="${rowCount}">
                <input type="hidden" name="_useRowSubmit" value="Y"/>
                <input type="submit" class="smallSubmit" value="${uiLabelMap.ProductComplete}"/>
            </td></tr>
            </#if>
        </table>
      <#if completeRequested>
      </form>
      </#if>
    </#if>
