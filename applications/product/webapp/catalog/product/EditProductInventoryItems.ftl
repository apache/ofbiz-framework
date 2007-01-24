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
<#assign externalKeyParam = "&externalLoginKey=" + requestAttributes.externalLoginKey?if_exists>
<#if parameters.showAllFacilities?exists>
<a href="EditProductInventoryItems?productId=${productId}" class="buttontext">${uiLabelMap.ProductShowProductFacilities}</a>
<#else>
<a href="EditProductInventoryItems?productId=${productId}&amp;showAllFacilities=Y" class="buttontext">${uiLabelMap.ProductShowAllFacilities}</a>
</#if>
<table border="1" cellpadding="2" cellspacing="0">
    <tr>
        <td><div class="tabletext"><b>${uiLabelMap.ProductFacility}</b></div></td>
        <td><div class="tabletext"><b>${uiLabelMap.ProductAtp}</b></div></td>
        <td><div class="tabletext"><b>${uiLabelMap.ProductQoh}</b></div></td>
        <td><div class="tabletext"><b>${uiLabelMap.ProductMarketingPackageATP}</b></div></td>
        <td><div class="tabletext"><b>${uiLabelMap.ProductMarketingPackageQOH}</b></div></td>
        <td><div class="tabletext"><b>${uiLabelMap.ProductIncomingShipments}</b></div></td>
        <td><div class="tabletext"><b>${uiLabelMap.ProductIncomingProductionRuns}</b></div></td>
        <td><div class="tabletext"><b>${uiLabelMap.ProductOutgoingProductionRuns}</b></div></td>
    </tr>
    <#list quantitySummaryByFacility.values() as quantitySummary>
        <#if quantitySummary.facilityId?exists>
            <#assign facilityId = quantitySummary.facilityId>
            <#assign facility = delegator.findByPrimaryKey("Facility", Static["org.ofbiz.base.util.UtilMisc"].toMap("facilityId", facilityId))>
            <#assign manufacturingInQuantitySummary = manufacturingInQuantitySummaryByFacility.get(facilityId)?if_exists>
            <#assign manufacturingOutQuantitySummary = manufacturingOutQuantitySummaryByFacility.get(facilityId)?if_exists>
            <#assign totalQuantityOnHand = quantitySummary.totalQuantityOnHand?if_exists>
            <#assign totalAvailableToPromise = quantitySummary.totalAvailableToPromise?if_exists>
            <#assign mktgPkgATP = quantitySummary.mktgPkgATP?if_exists>
            <#assign mktgPkgQOH = quantitySummary.mktgPkgQOH?if_exists>
            <#assign incomingShipmentAndItemList = quantitySummary.incomingShipmentAndItemList?if_exists>
            <#assign incomingProductionRunList = manufacturingInQuantitySummary.incomingProductionRunList?if_exists>
            <#assign incomingQuantityTotal = manufacturingInQuantitySummary.estimatedQuantityTotal?if_exists>
            <#assign outgoingProductionRunList = manufacturingOutQuantitySummary.outgoingProductionRunList?if_exists>
            <#assign outgoingQuantityTotal = manufacturingOutQuantitySummary.estimatedQuantityTotal?if_exists>
    
            <tr>
                <td><div class="tabletext">${(facility.facilityName)?if_exists} [${facilityId?default("[No Facility]")}] 
                <a href="/facility/control/ReceiveInventory?facilityId=${facilityId}&productId=${productId}&externLoginKey=${externalLoginKey}" class="buttontext">${uiLabelMap.ProductInventoryReceive}</a></div></td>
                <td><div class="tabletext"><#if totalAvailableToPromise?exists>${totalAvailableToPromise}<#else>&nbsp;</#if></div></td>
                <td><div class="tabletext"><#if totalQuantityOnHand?exists>${totalQuantityOnHand}<#else>&nbsp;</#if></div></td>
                <td><div class="tabletext"><#if mktgPkgATP?exists>${mktgPkgATP}<#else>&nbsp;</#if></div></td>
                <td><div class="tabletext"><#if mktgPkgQOH?exists>${mktgPkgQOH}<#else>&nbsp;</#if></div></td>
                <td>
                    <#if incomingShipmentAndItemList?has_content>
                        <#list incomingShipmentAndItemList as incomingShipmentAndItem>
                            <div class="tabletext">${incomingShipmentAndItem.shipmentId}:${incomingShipmentAndItem.shipmentItemSeqId}-${(incomingShipmentAndItem.estimatedArrivalDate.toString())?if_exists}-<#if incomingShipmentAndItem.quantity?exists>${incomingShipmentAndItem.quantity?string.number}<#else>[${uiLabelMap.ProductQuantityNotSet}]</#if></div>
                        </#list>
                    <#else>
                        <div class="tabletext">&nbsp;</div>
                    </#if>
                </td>
                <td>
                    <#if incomingProductionRunList?has_content>
                        <#list incomingProductionRunList as incomingProductionRun>
                            <div class="tabletext">${incomingProductionRun.workEffortId}-${(incomingProductionRun.estimatedCompletionDate.toString())?if_exists}-<#if incomingProductionRun.estimatedQuantity?exists>${incomingProductionRun.estimatedQuantity?string.number}<#else>[${uiLabelMap.ProductQuantityNotSet}]</#if></div>
                        </#list>
                        <div class="tabletext"><b>${uiLabelMap.CommonTotal}:&nbsp;${incomingQuantityTotal?if_exists}</b></div>
                    <#else>
                        <div class="tabletext">&nbsp;</div>
                    </#if>
                </td>
                <td>
                    <#if outgoingProductionRunList?has_content>
                        <#list outgoingProductionRunList as outgoingProductionRun>
                            <div class="tabletext">${outgoingProductionRun.workEffortParentId}:${outgoingProductionRun.workEffortId}-${(outgoingProductionRun.estimatedStartDate.toString())?if_exists}-<#if outgoingProductionRun.estimatedQuantity?exists>${outgoingProductionRun.estimatedQuantity?string.number}<#else>[${uiLabelMap.ProductQuantityNotSet}]</#if></div>
                        </#list>
                        <div class="tabletext"><b>${uiLabelMap.CommonTotal}:&nbsp;${outgoingQuantityTotal?if_exists}</b></div>
                    <#else>
                        <div class="tabletext">&nbsp;</div>
                    </#if>
                </td>
            </tr>
        </#if>
    </#list>
</table>

<hr class="sepbar"/>

<div class="head1">${uiLabelMap.ProductInventoryItems} <span class="head2">${uiLabelMap.CommonFor} <#if product?exists>${(product.internalName)?if_exists} </#if> [${uiLabelMap.CommonId}:${productId?if_exists}]</span></div>
<#if productId?has_content>
    <a href="/facility/control/EditInventoryItem?productId=${productId}${externalKeyParam}" class="buttontext">${uiLabelMap.ProductCreateNewInventoryItemProduct}</a>
    <#if showEmpty>
        <a href="<@ofbizUrl>EditProductInventoryItems?productId=${productId}</@ofbizUrl>" class="buttontext">${uiLabelMap.ProductHideEmptyItems}</a>
    <#else>
        <a href="<@ofbizUrl>EditProductInventoryItems?productId=${productId}&showEmpty=true</@ofbizUrl>" class="buttontext">${uiLabelMap.ProductShowEmptyItems}</a>
    </#if>
</#if>
<br/>
<#if productId?exists>
    <table border="1" cellpadding="2" cellspacing="0">
    <tr>
        <td><div class="tabletext"><b>${uiLabelMap.ProductItemId}</b></div></td>
        <td><div class="tabletext"><b>${uiLabelMap.ProductItemType}</b></div></td>
        <td><div class="tabletext"><b>${uiLabelMap.ProductStatus}</b></div></td>
        <td><div class="tabletext"><b>${uiLabelMap.CommonReceived}</b></div></td>
        <td><div class="tabletext"><b>${uiLabelMap.CommonExpire}</b></div></td>
        <td><div class="tabletext"><b>${uiLabelMap.ProductFacilityContainerId}</b></div></td>
        <td><div class="tabletext"><b>${uiLabelMap.ProductLocation}</b></div></td>
        <td><div class="tabletext"><b>${uiLabelMap.ProductLotId}</b></div></td>
        <td><div class="tabletext"><b>${uiLabelMap.ProductBinNum}</b></div></td>
        <td><div class="tabletext"><b>${uiLabelMap.ProductPerUnitPrice}</b></div></td>
        <td><div class="tabletext"><b>${uiLabelMap.ProductAtpQohSerial}</b></div></td>
    </tr>
    <#list productInventoryItems as inventoryItem>
       <#-- NOTE: Delivered for serialized inventory means shipped to customer so they should not be displayed here any more -->
       <#if showEmpty || (inventoryItem.inventoryItemTypeId?if_exists == "SERIALIZED_INV_ITEM" && inventoryItem.statusId?if_exists != "INV_DELIVERED")
                      || (inventoryItem.inventoryItemTypeId?if_exists == "NON_SERIAL_INV_ITEM" && ((inventoryItem.availableToPromiseTotal?exists && inventoryItem.availableToPromiseTotal != 0) || (inventoryItem.quantityOnHandTotal?exists && inventoryItem.quantityOnHandTotal != 0)))>
            <#assign curInventoryItemType = inventoryItem.getRelatedOne("InventoryItemType")>
            <#if inventoryItem.inventoryItemTypeId?if_exists == "SERIALIZED_INV_ITEM">
                <#assign curStatusItem = inventoryItem.getRelatedOneCache("StatusItem")?if_exists>
            </#if>
            <#assign facilityLocation = inventoryItem.getRelatedOne("FacilityLocation")?if_exists>
            <#assign facilityLocationTypeEnum = (facilityLocation.getRelatedOneCache("TypeEnumeration"))?if_exists>
            <#if curInventoryItemType?exists>
                <tr valign="middle">
                    <td><a href="/facility/control/EditInventoryItem?inventoryItemId=${(inventoryItem.inventoryItemId)?if_exists}${externalKeyParam}" class="buttontext">${(inventoryItem.inventoryItemId)?if_exists}</a></td>
                    <td><div class="tabletext">&nbsp;${(curInventoryItemType.get("description",locale))?if_exists}</div></td>
                    <td>
                        <div class="tabletext">
                            <#if inventoryItem.inventoryItemTypeId?if_exists == "SERIALIZED_INV_ITEM">
                                <#if curStatusItem?has_content>
                                    ${(curStatusItem.get("description",locale))?if_exists}
                                <#elseif inventoryItem.statusId?has_content>
                                    [${inventoryItem.statusId}]
                                <#else>
                                    ${uiLabelMap.CommonNotSet}&nbsp;
                                </#if>
                            <#else>
                                &nbsp;
                            </#if>
                        </div>
                    </td>
                    <td><div class="tabletext">&nbsp;${(inventoryItem.datetimeReceived)?if_exists}</div></td>
                    <td><div class="tabletext">&nbsp;${(inventoryItem.expireDate)?if_exists}</div></td>
                    <#if inventoryItem.facilityId?exists && inventoryItem.containerId?exists>
                        <td><div class="tabletext" style="color: red;">${uiLabelMap.ProductErrorFacility} (${inventoryItem.facilityId})
                            ${uiLabelMap.ProductAndContainer} (${inventoryItem.containerId}) ${uiLabelMap.CommonSpecified}</div></td>
                    <#elseif inventoryItem.facilityId?exists>
                        <td><span class="tabletext">${uiLabelMap.ProductFacilityLetter}:&nbsp;</span><a href="/facility/control/EditFacility?facilityId=${inventoryItem.facilityId}${externalKeyParam}" class="linktext">${inventoryItem.facilityId}</a></td>
                    <#elseif (inventoryItem.containerId)?exists>
                        <td><span class="tabletext">${uiLabelMap.ProductContainerLetter}:&nbsp;</span><a href="<@ofbizUrl>EditContainer?containerId=${inventoryItem.containerId }</@ofbizUrl>" class="linktext">${inventoryItem.containerId}</a></td>
                    <#else>
                        <td>&nbsp;</td>
                    </#if>
                    <td><div class="tabletext"><a href="/facility/control/EditFacilityLocation?facilityId=${(inventoryItem.facilityId)?if_exists}&locationSeqId=${(inventoryItem.locationSeqId)?if_exists}${externalKeyParam}" class="linktext"><#if facilityLocation?exists>${facilityLocation.areaId?if_exists}:${facilityLocation.aisleId?if_exists}:${facilityLocation.sectionId?if_exists}:${facilityLocation.levelId?if_exists}:${facilityLocation.positionId?if_exists}</#if><#if facilityLocationTypeEnum?has_content> (${facilityLocationTypeEnum.get("description",locale)})</#if> [${(inventoryItem.locationSeqId)?if_exists}]</a></div></td>
                    <td><div class="tabletext">&nbsp;${(inventoryItem.lotId)?if_exists}</div></td>
                    <td><div class="tabletext">&nbsp;${(inventoryItem.binNumber)?if_exists}</div></td>
                    <td><div class="tabletextright">&nbsp;<@ofbizCurrency amount=inventoryItem.unitCost isoCode=inventoryItem.currencyUomId/></div></td>
                    <#if inventoryItem.inventoryItemTypeId?if_exists == "NON_SERIAL_INV_ITEM">
                        <td>
                            <div class="tabletext">${(inventoryItem.availableToPromiseTotal)?default("NA")}
                            / ${(inventoryItem.quantityOnHandTotal)?default("NA")}</div>
                        </td>
                    <#elseif inventoryItem.inventoryItemTypeId?if_exists == "SERIALIZED_INV_ITEM">
                        <td><div class="tabletext">&nbsp;${(inventoryItem.serialNumber)?if_exists}</div></td>
                    <#else>
                        <td><div class="tabletext" style="color: red;">${uiLabelMap.ProductErrorType} ${(inventoryItem.inventoryItemTypeId)?if_exists} ${uiLabelMap.ProductUnknownSerialNumber} (${(inventoryItem.serialNumber)?if_exists})
                            ${uiLabelMap.ProductAndQuantityOnHand} (${(inventoryItem.quantityOnHandTotal)?if_exists} ${uiLabelMap.CommonSpecified}</div></td>
                        <td>&nbsp;</td>
                    </#if>
                </tr>
            </#if>
        </#if>
    </#list>
  </table>
</#if>
