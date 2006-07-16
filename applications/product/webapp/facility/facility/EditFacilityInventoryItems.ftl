<#--

Copyright 2001-2006 The Apache Software Foundation

Licensed under the Apache License, Version 2.0 (the "License"); you may not
use this file except in compliance with the License. You may obtain a copy of
the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
License for the specific language governing permissions and limitations
under the License.
-->

    <div class="head1">${uiLabelMap.ProductInventoryItemsFor} <span class="head2"><#if facility?exists>${(facility.facilityName)?if_exists}</#if> [${uiLabelMap.CommonId}:${facilityId?if_exists}]</span></div>

    <a href="<@ofbizUrl>EditFacility</@ofbizUrl>" class="buttontext">${uiLabelMap.ProductNewFacility}</a>
    <a href="<@ofbizUrl>EditInventoryItem?facilityId=${facilityId}</@ofbizUrl>" class="buttontext">${uiLabelMap.ProductCreateNewInventoryItemFacility}</a>
    <a href="<@ofbizUrl>ViewFacilityInventoryByProduct?facilityId=${facilityId}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonView} ${uiLabelMap.ProductInventoryByProduct}</a>
    <a href="<@ofbizUrl>SearchInventoryItems?facilityId=${facilityId}</@ofbizUrl>" class="buttontext">${uiLabelMap.PageTitleSearchInventoryItems}</a>

    <#if facilityInventoryItems?exists && (facilityInventoryItems.size() > 0)>
        <table border="0" width="100%" cellpadding="2">
            <tr>
            <td align="right">
                <b>
                <#if (viewIndex > 0)>
                <a href="<@ofbizUrl>EditFacilityInventoryItems?facilityId=${facilityId?if_exists}&VIEW_SIZE=${viewSize}&VIEW_INDEX=${viewIndex-1}</@ofbizUrl>" class="buttontext">[${uiLabelMap.CommonPrevious}]</a> |
                </#if>
                <#if (listSize > 0)>
                    ${lowIndex+1} - ${highIndex} ${uiLabelMap.CommonOf} ${listSize}
                </#if>
                <#if (listSize > highIndex)>
                | <a href="<@ofbizUrl>EditFacilityInventoryItems?facilityId=${facilityId}&VIEW_SIZE=${viewSize}&VIEW_INDEX=${viewIndex+1}</@ofbizUrl>" class="buttontext">[${uiLabelMap.CommonNext}]</a>
                </#if>
                </b>
            </td>
            </tr>
        </table>
    </#if>
    <#if facilityId?exists>
        <table border="1" cellpadding="2" cellspacing="0" width="100%">
        <tr>
            <td><div class="tabletext"><b>${uiLabelMap.ProductItemId}</b></div></td>
            <td><div class="tabletext"><b>${uiLabelMap.ProductItemType}</b></div></td>
            <td><div class="tabletext"><b>${uiLabelMap.ProductStatus}</b></div></td>
            <td><div class="tabletext"><b>${uiLabelMap.CommonReceived}</b></div></td>
            <td><div class="tabletext"><b>${uiLabelMap.CommonExpire}</b></div></td>
            <td><div class="tabletext"><b>${uiLabelMap.ProductProductId}</b></div></td>
            <td><div class="tabletext"><b>${uiLabelMap.ProductLocation}</b></div></td>
            <td><div class="tabletext"><b>${uiLabelMap.ProductLotId}</b></div></td>
            <td><div class="tabletext"><b>${uiLabelMap.ProductBinNum}</b></div></td>
            <td><div class="tabletext"><b>${uiLabelMap.ProductAtpQohSerial}</b></div></td>
            <td>&nbsp;</td>
            <td>&nbsp;</td>
            <#-- <td>&nbsp;</td> -->
        </tr>
        <#if facilityInventoryItems?has_content>
        <#list facilityInventoryItems[lowIndex..highIndex-1] as inventoryItem>
        <#assign curInventoryItemType = inventoryItem.getRelatedOne("InventoryItemType")>
        <#assign facilityLocation = inventoryItem.getRelatedOne("FacilityLocation")?if_exists>
        <#assign facilityLocationTypeEnum = (facilityLocation.getRelatedOneCache("TypeEnumeration"))?if_exists>
        <#assign isQuantity = false>
        <#if (inventoryItem.quantityOnHandTotal)?exists && (!(inventoryItem.serialNumber)?exists || (inventoryItem.serialNumber.length() == 0))><#assign isQuantity=true></#if>
        <tr valign="middle">
            <td><div class="tabletext">&nbsp;<a href="<@ofbizUrl>EditInventoryItem?inventoryItemId=${(inventoryItem.inventoryItemId)?if_exists}&facilityId=${facilityId}</@ofbizUrl>" class="buttontext">${(inventoryItem.inventoryItemId)?if_exists}</a></div></td>
            <td><div class="tabletext">&nbsp;${(curInventoryItemType.get("description",locale))?if_exists}</div></td>
            <td><div class="tabletext">&nbsp;${(inventoryItem.statusId)?if_exists}</div></td>
            <td><div class="tabletext">&nbsp;${(inventoryItem.datetimeReceived)?if_exists}</div></td>
            <td><div class="tabletext">&nbsp;${(inventoryItem.expireDate)?if_exists}</div></td>
            <td><a href="/catalog/control/EditProduct?productId=${(inventoryItem.productId)?if_exists}&externalLoginKey=${externalLoginKey?if_exists}" class="buttontext">${(inventoryItem.productId)?if_exists}</a></td>
            <td><div class="tabletext">&nbsp;<a href="<@ofbizUrl>EditFacilityLocation?facilityId=${facilityId}&locationSeqId=${(inventoryItem.locationSeqId)?if_exists}</@ofbizUrl>" class="buttontext"><#if facilityLocation?exists>${facilityLocation.areaId?if_exists}:${facilityLocation.aisleId?if_exists}:${facilityLocation.sectionId?if_exists}:${facilityLocation.levelId?if_exists}:${facilityLocation.positionId?if_exists}</#if><#if facilityLocationTypeEnum?has_content>(${facilityLocationTypeEnum.get("description",locale)})</#if>[${(inventoryItem.locationSeqId)?if_exists}]</a></div></td>
            <td><div class="tabletext">&nbsp;${(inventoryItem.lotId)?if_exists}</div></td>
            <td><div class="tabletext">&nbsp;${(inventoryItem.binNumber)?if_exists}</div></td>
            <#if (inventoryItem.inventoryItemTypeId)?exists && (inventoryItem.inventoryItemTypeId.equals("NON_SERIAL_INV_ITEM"))>
                <td>
                    <div class="tabletext">${(inventoryItem.availableToPromiseTotal)?if_exists}
                    / ${(inventoryItem.quantityOnHandTotal)?if_exists}</div>
                </td>
            <#elseif (inventoryItem.inventoryItemTypeId)?exists && inventoryItem.inventoryItemTypeId.equals("SERIALIZED_INV_ITEM")>
                    <td><div class="tabletext">&nbsp;${(inventoryItem.serialNumber)?if_exists}</div></td>
            <#else>
                <td><div class="tabletext" style="color: red;">Error: type ${(inventoryItem.inventoryItemTypeId)?if_exists} unknown, serialNumber (${(inventoryItem.serialNumber)?if_exists}) AND quantityOnHand (${(inventoryItem.quantityOnHandTotal)?if_exists}) specified</div></td>
                <td>&nbsp;</td>
            </#if>
            <td>
            <a href="<@ofbizUrl>EditInventoryItem?facilityId=${facilityId}&inventoryItemId=${(inventoryItem.inventoryItemId)?if_exists}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonEdit}</a>
            </td>
            <td>
            <a href="<@ofbizUrl>TransferInventoryItem?facilityId=${facilityId}&inventoryItemId=${(inventoryItem.inventoryItemId)?if_exists}</@ofbizUrl>" class="buttontext">${uiLabelMap.ProductTransfer}</a>
            </td>
            <#-- <td>
            <a href="<@ofbizUrl>DeleteFacilityInventoryItem?facilityId=${facilityId}&inventoryItemId=${(inventoryItem.inventoryItemId)?if_exists}</@ofbizUrl>" class="buttontext">
            [${uiLabelMap.CommonDelete}]</a>
            </td> -->
        </tr>
        </#list>
        </#if>
        </table>
        <#if (facilityInventoryItems.size() > 0)>
        <table border="0" width="100%" cellpadding="2">
            <tr>
            <td align="right">
                <b>
                <#if (viewIndex > 0)>
                <a href="<@ofbizUrl>EditFacilityInventoryItems?facilityId=${facilityId}&VIEW_SIZE=${viewSize}&VIEW_INDEX=${viewIndex-1}</@ofbizUrl>" class="buttontext">[${uiLabelMap.CommonPrevious}]</a> |
                </#if>
                <#if (listSize > 0)>
                ${lowIndex+1} - ${highIndex} ${uiLabelMap.CommonOf} ${listSize}
                </#if>
                <#if (listSize > highIndex)>
                | <a href="<@ofbizUrl>EditFacilityInventoryItems?facilityId=${facilityId}&VIEW_SIZE=${viewSize}&VIEW_INDEX=${viewIndex+1}</@ofbizUrl>" class="buttontext">[${uiLabelMap.CommonNext}]</a>
                </#if>
                </b>
            </td>
            </tr>
        </table>
        </#if>
        <br/>
    </#if>
