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
<#assign externalKeyParam = "&amp;externalLoginKey=" + requestAttributes.externalLoginKey!>
<div class="screenlet">
  <#if product??>
    <div class="screenlet-title-bar">
        <h3>${uiLabelMap.ProductInventoryItems} ${uiLabelMap.CommonFor} <#if product??>${(product.internalName)!} </#if> [${uiLabelMap.CommonId}:${productId!}]</h3>
    </div>
    <div class="screenlet-body">
        <#if productId?has_content>
            <a href="/facility/control/EditInventoryItem?productId=${productId}${StringUtil.wrapString(externalKeyParam)}" class="buttontext">${uiLabelMap.ProductCreateNewInventoryItemProduct}</a>
            <#if showEmpty>
                <a href="<@ofbizUrl>EditProductInventoryItems?productId=${productId}</@ofbizUrl>" class="buttontext">${uiLabelMap.ProductHideEmptyItems}</a>
            <#else>
                <a href="<@ofbizUrl>EditProductInventoryItems?productId=${productId}&amp;showEmpty=true</@ofbizUrl>" class="buttontext">${uiLabelMap.ProductShowEmptyItems}</a>
            </#if>
        </#if>
        <br />
        <#if productId??>
            <table cellspacing="0" class="basic-table">
            <tr class="header-row">
                <td><b>${uiLabelMap.ProductItemId}</b></td>
                <td><b>${uiLabelMap.ProductItemType}</b></td>
                <td><b>${uiLabelMap.CommonStatus}</b></td>
                <td><b>${uiLabelMap.CommonReceived}</b></td>
                <td><b>${uiLabelMap.CommonExpire}</b></td>
                <td><b>${uiLabelMap.ProductFacilityContainerId}</b></td>
                <td><b>${uiLabelMap.ProductLocation}</b></td>
                <td><b>${uiLabelMap.ProductLotId}</b></td>
                <td><b>${uiLabelMap.ProductBinNum}</b></td>
                <td align="right"><b>${uiLabelMap.ProductPerUnitPrice}</b></td>
                <td>&nbsp;</td>
                <td align="right"><b>${uiLabelMap.ProductInventoryItemInitialQuantity}</b></td>
                <td align="right"><b>${uiLabelMap.ProductAtpQohSerial}</b></td>
            </tr>
            <#assign rowClass = "2">
            <#list productInventoryItems as inventoryItem>
               <#-- NOTE: Delivered for serialized inventory means shipped to customer so they should not be displayed here any more -->
               <#if showEmpty || (inventoryItem.inventoryItemTypeId! == "SERIALIZED_INV_ITEM" && inventoryItem.statusId! != "INV_DELIVERED")
                              || (inventoryItem.inventoryItemTypeId! == "NON_SERIAL_INV_ITEM" && ((inventoryItem.availableToPromiseTotal?? && inventoryItem.availableToPromiseTotal != 0) || (inventoryItem.quantityOnHandTotal?? && inventoryItem.quantityOnHandTotal != 0)))>
                    <#assign curInventoryItemType = inventoryItem.getRelatedOne("InventoryItemType", false)>
                    <#assign curStatusItem = inventoryItem.getRelatedOne("StatusItem", true)!>
                    <#assign facilityLocation = inventoryItem.getRelatedOne("FacilityLocation", false)!>
                    <#assign facilityLocationTypeEnum = (facilityLocation.getRelatedOne("TypeEnumeration", true))!>
                    <#assign inventoryItemDetailFirst = Static["org.apache.ofbiz.entity.util.EntityUtil"].getFirst(inventoryItem.getRelated("InventoryItemDetail", null, Static["org.apache.ofbiz.base.util.UtilMisc"].toList("effectiveDate"), false))!>
                    <#if curInventoryItemType??>
                        <tr valign="middle"<#if rowClass == "1"> class="alternate-row"</#if>>
                            <td><a href="/facility/control/EditInventoryItem?inventoryItemId=${(inventoryItem.inventoryItemId)!}${StringUtil.wrapString(externalKeyParam)}" class="buttontext">${(inventoryItem.inventoryItemId)!}</a></td>
                            <td>&nbsp;${(curInventoryItemType.get("description",locale))!}</td>
                            <td>
                                <div>
                                    <#if curStatusItem?has_content>
                                        ${(curStatusItem.get("description",locale))!}
                                    <#elseif inventoryItem.statusId?has_content>
                                        [${inventoryItem.statusId}]
                                    <#else>
                                        ${uiLabelMap.CommonNotSet}&nbsp;
                                    </#if>
                                </div>
                            </td>
                            <td>&nbsp;${(inventoryItem.datetimeReceived)!}</td>
                            <td>&nbsp;${(inventoryItem.expireDate)!}</td>
                            <#if inventoryItem.facilityId?? && inventoryItem.containerId??>
                                <td style="color: red;">${uiLabelMap.ProductErrorFacility} (${inventoryItem.facilityId})
                                    ${uiLabelMap.ProductAndContainer} (${inventoryItem.containerId}) ${uiLabelMap.CommonSpecified}</td>
                            <#elseif inventoryItem.facilityId??>
                                <td>${uiLabelMap.ProductFacilityLetter}:&nbsp;<a href="/facility/control/EditFacility?facilityId=${inventoryItem.facilityId}${StringUtil.wrapString(externalKeyParam)}" class="linktext">${inventoryItem.facilityId}</a></td>
                            <#elseif (inventoryItem.containerId)??>
                                <td>${uiLabelMap.ProductContainerLetter}:&nbsp;<a href="<@ofbizUrl>EditContainer?containerId=${inventoryItem.containerId }</@ofbizUrl>" class="linktext">${inventoryItem.containerId}</a></td>
                            <#else>
                                <td>&nbsp;</td>
                            </#if>
                            <td><a href="/facility/control/EditFacilityLocation?facilityId=${(inventoryItem.facilityId)!}&amp;locationSeqId=${(inventoryItem.locationSeqId)!}${StringUtil.wrapString(externalKeyParam)}" class="linktext"><#if facilityLocation??>${facilityLocation.areaId!}:${facilityLocation.aisleId!}:${facilityLocation.sectionId!}:${facilityLocation.levelId!}:${facilityLocation.positionId!}</#if><#if facilityLocationTypeEnum?has_content> (${facilityLocationTypeEnum.get("description",locale)})</#if> [${(inventoryItem.locationSeqId)!}]</a></td>
                            <td>&nbsp;${(inventoryItem.lotId)!}</td>
                            <td>&nbsp;${(inventoryItem.binNumber)!}</td>
                            <td align="right"><@ofbizCurrency amount=inventoryItem.unitCost isoCode=inventoryItem.currencyUomId/></td>
                            <td>
                                <#if inventoryItemDetailFirst?? && inventoryItemDetailFirst.workEffortId??>
                                    <b>${uiLabelMap.ProductionRunId}</b> ${inventoryItemDetailFirst.workEffortId}
                                <#elseif inventoryItemDetailFirst?? && inventoryItemDetailFirst.orderId??>
                                    <b>${uiLabelMap.OrderId}</b> ${inventoryItemDetailFirst.orderId}
                                </#if>
                            </td>
                            <td align="right">${(inventoryItemDetailFirst.quantityOnHandDiff)!}</td>
                            <#if inventoryItem.inventoryItemTypeId! == "NON_SERIAL_INV_ITEM">
                                <td align="right">
                                    <div>${(inventoryItem.availableToPromiseTotal)?default("NA")}
                                    / ${(inventoryItem.quantityOnHandTotal)?default("NA")}</div>
                                </td>
                            <#elseif inventoryItem.inventoryItemTypeId! == "SERIALIZED_INV_ITEM">
                                <td align="right">&nbsp;${(inventoryItem.serialNumber)!}</td>
                            <#else>
                                <td align="right" style="color: red;">${uiLabelMap.ProductErrorType} ${(inventoryItem.inventoryItemTypeId)!} ${uiLabelMap.ProductUnknownSerialNumber} (${(inventoryItem.serialNumber)!})
                                    ${uiLabelMap.ProductAndQuantityOnHand} (${(inventoryItem.quantityOnHandTotal)!} ${uiLabelMap.CommonSpecified}</td>
                            </#if>
                        </tr>
                    </#if>
                </#if>
                <#-- toggle the row color -->
                <#if rowClass == "2">
                    <#assign rowClass = "1">
                <#else>
                    <#assign rowClass = "2">
                </#if>
            </#list>
          </table>
        </#if>
    </div>
  <#else>
    <h2>${uiLabelMap.ProductProductNotFound} ${productId!}!</h2>
  </#if>
</div>
