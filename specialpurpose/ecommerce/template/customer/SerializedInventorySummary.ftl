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

<div id="serialized-inventory-summary" class="screenlet">
    <div class="screenlet-title-bar">
        <span class="h3">${uiLabelMap.ProductSerializedInventorySummary}</span>
    </div>
    <div class="screenlet-body">
        <table width="100%" cellspacing="0" cellpadding="2">
            <thead>
                <tr class="header-row">
                    <td><div class="tableheadtext">${uiLabelMap.ProductInventoryItemId}</div></td>
                    <td><div class="tableheadtext">${uiLabelMap.ProductProductName}</div></td>
                    <td><div class="tableheadtext">${uiLabelMap.ProductSerialNumber}</div></td>
                    <td><div class="tableheadtext">${uiLabelMap.ProductSoftIdentifier}</div></td>
                    <td><div class="tableheadtext">${uiLabelMap.ProductActivationNumber}</div></td>
                    <td><div class="tableheadtext">${uiLabelMap.ProductActivationNumber} ${uiLabelMap.CommonValidThruDate}</div></td>
                </tr>
            </thead>
            <tbody>
                <#list inventoryItemList as inventoryItem>
                    <#assign product = inventoryItem.getRelatedOne('Product', false)!>
                    <tr>
                        <td>${inventoryItem.inventoryItemId}</td>
                        <td>
                            <#if product?has_content>
                                <#if product.isVariant?default('N') == 'Y'>
                                    <#assign product = Static['org.ofbiz.product.product.ProductWorker'].getParentProduct(product.productId, delegator)!>
                                </#if>
                                <#if product?has_content>
                                    <#assign productName = Static['org.ofbiz.product.product.ProductContentWrapper'].getProductContentAsText(product, 'PRODUCT_NAME', request, "html")!>
                                    <a href="<@ofbizUrl>product?product_id=${product.productId}</@ofbizUrl>" class="linktext">${productName?default(product.productId)}</a>
                                </#if>
                            </#if>
                        </td>
                        <td>${inventoryItem.serialNumber!}</td>
                        <td>${inventoryItem.softIdentifier!}</td>
                        <td>${inventoryItem.activationNumber!}</td>
                        <td>${inventoryItem.activationValidThru!}</td>
                    </tr>
                </#list>
            </tbody>
        </table>
    </div>
</div>

