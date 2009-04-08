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
        <h3>${uiLabelMap.ProductSerializedInventorySummary}</h3>
    </div>
    <div class="screenlet-body">
        <table id="serialized-inventory" class="basic-table" cellspacing="0" cellpadding="2">
            <thead>
                <tr class="header-row">
                    <td>${uiLabelMap.ProductInventoryItemId}</td>
                    <td>${uiLabelMap.ProductProductName}</td>
                    <td>${uiLabelMap.ProductSerialNumber}</td>
                    <td>${uiLabelMap.ProductSoftIdentifier}</td>
                    <td>${uiLabelMap.ProductActivationNumber}</td>
                    <td>${uiLabelMap.ProductActivationNumber} ${uiLabelMap.CommonValidThruDate}</td>
                </tr>
            </thead>
            <tbody>
                <#list inventoryItemList as inventoryItem>
                    <#assign product = inventoryItem.getRelatedOne('Product')?if_exists>
                    <tr>
                        <td><a href="/facility/control/EditInventoryItem?inventoryItemId=${inventoryItem.inventoryItemId}&externalLoginKey=${requestAttributes.externalLoginKey?if_exists}" class="linktext">${inventoryItem.inventoryItemId}</a></td>
                        <td>
                            <#if product?has_content>
                                <#if product.isVariant?default('N') == 'Y'>
                                    <#assign product = Static['org.ofbiz.product.product.ProductWorker'].getParentProduct(product.productId, delegator)?if_exists>
                                </#if>
                                <#if product?has_content>
                                    <#assign productName = Static['org.ofbiz.product.product.ProductContentWrapper'].getProductContentAsText(product, 'PRODUCT_NAME', request)?if_exists>
                                    <a href="/catalog/control/EditProduct?productId=${product.productId}&externalLoginKey=${requestAttributes.externalLoginKey?if_exists}">${productName?default(product.productId)}</a>
                                </#if>
                            </#if>
                        </td>
                        <td>${inventoryItem.serialNumber?if_exists}</td>
                        <td>
                          ${inventoryItem.softIdentifier?if_exists}
                          <#if (inventoryItem.softIdentifier?has_content && inventoryItem.softIdentifier?matches("\\d+"))>
                            <#assign sid = Static["java.lang.Long"].decode(inventoryItem.softIdentifier)/>
                            (0x${Static["java.lang.Long"].toHexString(sid)})
                          </#if>
                        </td>
                        <td>${inventoryItem.activationNumber?if_exists}</td>
                        <td>${inventoryItem.activationValidThru?if_exists}</td>
                    </tr>
                </#list>
            </tbody>
        </table>
    </div>
</div>

