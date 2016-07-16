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
<#if parameters.showAllFacilities??>
<a href="EditProductInventoryItems?productId=${productId}" class="buttontext">${uiLabelMap.ProductShowProductFacilities}</a>
<#else>
<a href="EditProductInventoryItems?productId=${productId}&amp;showAllFacilities=Y" class="buttontext">${uiLabelMap.ProductShowAllFacilities}</a>
</#if>
<div class="screenlet">
  <#if product??>
    <div class="screenlet-title-bar">
        <h3>${uiLabelMap.ProductInventorySummary}</h3>
    </div>
    <div class="screenlet-body">
        <table cellspacing="0" class="basic-table">
            <tr class="header-row">
                <td><b>${uiLabelMap.ProductFacility}</b></td>
                <td><b>${uiLabelMap.ProductAtp}</b></td>
                <td><b>${uiLabelMap.ProductQoh}</b></td>
                <#if isMarketingPackage == "true">
                <td><b>${uiLabelMap.ProductMarketingPackageATP}</b></td>
                <td><b>${uiLabelMap.ProductMarketingPackageQOH}</b></td>
                </#if>
                <td><b>${uiLabelMap.ProductIncomingShipments}</b></td>
                <td><b>${uiLabelMap.ProductIncomingProductionRuns}</b></td>
                <td><b>${uiLabelMap.ProductOutgoingProductionRuns}</b></td>
            </tr>
            <#assign rowClass = "2">
            <#list quantitySummaryByFacility.values() as quantitySummary>
                <#if quantitySummary.facilityId??>
                    <#assign facilityId = quantitySummary.facilityId>
                    <#assign facility = delegator.findOne("Facility", Static["org.apache.ofbiz.base.util.UtilMisc"].toMap("facilityId", facilityId), false)>
                    <#assign manufacturingInQuantitySummary = manufacturingInQuantitySummaryByFacility.get(facilityId)!>
                    <#assign manufacturingOutQuantitySummary = manufacturingOutQuantitySummaryByFacility.get(facilityId)!>
                    <#assign totalQuantityOnHand = quantitySummary.totalQuantityOnHand!>
                    <#assign totalAvailableToPromise = quantitySummary.totalAvailableToPromise!>
                    <#assign mktgPkgATP = quantitySummary.mktgPkgATP!>
                    <#assign mktgPkgQOH = quantitySummary.mktgPkgQOH!>
                    <#assign incomingShipmentAndItemList = quantitySummary.incomingShipmentAndItemList!>
                    <#assign incomingProductionRunList = manufacturingInQuantitySummary.incomingProductionRunList!>
                    <#assign incomingQuantityTotal = manufacturingInQuantitySummary.estimatedQuantityTotal!>
                    <#assign outgoingProductionRunList = manufacturingOutQuantitySummary.outgoingProductionRunList!>
                    <#assign outgoingQuantityTotal = manufacturingOutQuantitySummary.estimatedQuantityTotal!>
                    <tr valign="middle"<#if rowClass == "1"> class="alternate-row"</#if>>
                        <td>${(facility.facilityName)!} [${facilityId?default("[No Facility]")}]
                        <a href="/facility/control/ReceiveInventory?facilityId=${facilityId}&amp;productId=${productId}&amp;externLoginKey=${externalLoginKey}" class="buttontext">${uiLabelMap.ProductInventoryReceive}</a></td>
                        <td><#if totalAvailableToPromise??>${totalAvailableToPromise}<#else>&nbsp;</#if></td>
                        <td><#if totalQuantityOnHand??>${totalQuantityOnHand}<#else>&nbsp;</#if></td>
                        <#if isMarketingPackage == "true">
                        <td><#if mktgPkgATP??>${mktgPkgATP}<#else>&nbsp;</#if></td>
                        <td><#if mktgPkgQOH??>${mktgPkgQOH}<#else>&nbsp;</#if></td>
                        </#if>
                        <td>
                            <#if incomingShipmentAndItemList?has_content>
                                <#list incomingShipmentAndItemList as incomingShipmentAndItem>
                                    <div>${incomingShipmentAndItem.shipmentId}:${incomingShipmentAndItem.shipmentItemSeqId}-${(incomingShipmentAndItem.estimatedArrivalDate.toString())!}-<#if incomingShipmentAndItem.quantity??>${incomingShipmentAndItem.quantity?string.number}<#else>[${uiLabelMap.ProductQuantityNotSet}]</#if></div>
                                </#list>
                            <#else>
                                <div>&nbsp;</div>
                            </#if>
                        </td>
                        <td>
                            <#if incomingProductionRunList?has_content>
                                <#list incomingProductionRunList as incomingProductionRun>
                                    <div>${incomingProductionRun.workEffortId}-${(incomingProductionRun.estimatedCompletionDate.toString())!}-<#if incomingProductionRun.estimatedQuantity??>${incomingProductionRun.estimatedQuantity?string.number}<#else>[${uiLabelMap.ProductQuantityNotSet}]</#if></div>
                                </#list>
                                <div><b>${uiLabelMap.CommonTotal}:&nbsp;${incomingQuantityTotal!}</b></div>
                            <#else>
                                <div>&nbsp;</div>
                            </#if>
                        </td>
                        <td>
                            <#if outgoingProductionRunList?has_content>
                                <#list outgoingProductionRunList as outgoingProductionRun>
                                    <div>${outgoingProductionRun.workEffortParentId?default("")}:${outgoingProductionRun.workEffortId}-${(outgoingProductionRun.estimatedStartDate.toString())!}-<#if outgoingProductionRun.estimatedQuantity??>${outgoingProductionRun.estimatedQuantity?string.number}<#else>[${uiLabelMap.ProductQuantityNotSet}]</#if></div>
                                </#list>
                                <div><b>${uiLabelMap.CommonTotal}:&nbsp;${outgoingQuantityTotal!}</b></div>
                            <#else>
                                <div>&nbsp;</div>
                            </#if>
                        </td>
                    </tr>

                </#if>
                <#-- toggle the row color -->
                <#if rowClass == "2">
                    <#assign rowClass = "1">
                <#else>
                    <#assign rowClass = "2">
                </#if>
            </#list>
        </table>
    </div>
  <#else>
    <h2>${uiLabelMap.ProductProductNotFound} ${productId!}!</h2>
  </#if>
</div>
