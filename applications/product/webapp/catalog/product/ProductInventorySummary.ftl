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
<#if parameters.showAllFacilities?exists>
<a href="EditProductInventoryItems?productId=${productId}" class="buttontext">${uiLabelMap.ProductShowProductFacilities}</a>
<#else>
<a href="EditProductInventoryItems?productId=${productId}&amp;showAllFacilities=Y" class="buttontext">${uiLabelMap.ProductShowAllFacilities}</a>
</#if>
<div class="screenlet">
  <#if product?exists>
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
                <#if quantitySummary.facilityId?exists>
                    <#assign facilityId = quantitySummary.facilityId>
                    <#assign facility = delegator.findOne("Facility", Static["org.ofbiz.base.util.UtilMisc"].toMap("facilityId", facilityId), false)>
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
                    <tr valign="middle"<#if rowClass == "1"> class="alternate-row"</#if>>
                        <td>${(facility.facilityName)?if_exists} [${facilityId?default("[No Facility]")}]
                        <a href="/facility/control/ReceiveInventory?facilityId=${facilityId}&amp;productId=${productId}&amp;externLoginKey=${externalLoginKey}" class="buttontext">${uiLabelMap.ProductInventoryReceive}</a></td>
                        <td><#if totalAvailableToPromise?exists>${totalAvailableToPromise}<#else>&nbsp;</#if></td>
                        <td><#if totalQuantityOnHand?exists>${totalQuantityOnHand}<#else>&nbsp;</#if></td>
                        <#if isMarketingPackage == "true">
                        <td><#if mktgPkgATP?exists>${mktgPkgATP}<#else>&nbsp;</#if></td>
                        <td><#if mktgPkgQOH?exists>${mktgPkgQOH}<#else>&nbsp;</#if></td>
                        </#if>
                        <td>
                            <#if incomingShipmentAndItemList?has_content>
                                <#list incomingShipmentAndItemList as incomingShipmentAndItem>
                                    <div>${incomingShipmentAndItem.shipmentId}:${incomingShipmentAndItem.shipmentItemSeqId}-${(incomingShipmentAndItem.estimatedArrivalDate.toString())?if_exists}-<#if incomingShipmentAndItem.quantity?exists>${incomingShipmentAndItem.quantity?string.number}<#else>[${uiLabelMap.ProductQuantityNotSet}]</#if></div>
                                </#list>
                            <#else>
                                <div>&nbsp;</div>
                            </#if>
                        </td>
                        <td>
                            <#if incomingProductionRunList?has_content>
                                <#list incomingProductionRunList as incomingProductionRun>
                                    <div>${incomingProductionRun.workEffortId}-${(incomingProductionRun.estimatedCompletionDate.toString())?if_exists}-<#if incomingProductionRun.estimatedQuantity?exists>${incomingProductionRun.estimatedQuantity?string.number}<#else>[${uiLabelMap.ProductQuantityNotSet}]</#if></div>
                                </#list>
                                <div><b>${uiLabelMap.CommonTotal}:&nbsp;${incomingQuantityTotal?if_exists}</b></div>
                            <#else>
                                <div>&nbsp;</div>
                            </#if>
                        </td>
                        <td>
                            <#if outgoingProductionRunList?has_content>
                                <#list outgoingProductionRunList as outgoingProductionRun>
                                    <div>${outgoingProductionRun.workEffortParentId?default("")}:${outgoingProductionRun.workEffortId}-${(outgoingProductionRun.estimatedStartDate.toString())?if_exists}-<#if outgoingProductionRun.estimatedQuantity?exists>${outgoingProductionRun.estimatedQuantity?string.number}<#else>[${uiLabelMap.ProductQuantityNotSet}]</#if></div>
                                </#list>
                                <div><b>${uiLabelMap.CommonTotal}:&nbsp;${outgoingQuantityTotal?if_exists}</b></div>
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
    <h2>${uiLabelMap.ProductProductNotFound} ${productId?if_exists}!</h2>
  </#if>
</div>
