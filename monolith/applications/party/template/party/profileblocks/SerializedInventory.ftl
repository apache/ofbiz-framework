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
      <ul>
        <li class="h3">${uiLabelMap.ProductSerializedInventorySummary}</li>
      </ul>
      <br class="clear" />
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
                    <#assign product = inventoryItem.getRelatedOne('Product', false)!>
                    <tr>
                        <td><a href="<@ofbizUrl controlPath="/facility/control">EditInventoryItem?inventoryItemId=${inventoryItem.inventoryItemId}&amp;externalLoginKey=${requestAttributes.externalLoginKey!}</@ofbizUrl>" class="linktext">${inventoryItem.inventoryItemId}</a></td>
                        <td>
                            <#if product?has_content>
                                <#if product.isVariant?default('N') == 'Y'>
                                    <#assign product = Static['org.apache.ofbiz.product.product.ProductWorker'].getParentProduct(product.productId, delegator)!>
                                </#if>
                                <#if product?has_content>
                                    <#assign productName = Static['org.apache.ofbiz.product.product.ProductContentWrapper'].getProductContentAsText(product, 'PRODUCT_NAME', request, "html")!>
                                    <a href="<@ofbizUrl controlPath="/catalog/control">EditProduct?productId=${product.productId}&amp;externalLoginKey=${requestAttributes.externalLoginKey!}</@ofbizUrl>">${productName?default(product.productId)}</a>
                                </#if>
                            </#if>
                        </td>
                        <td>${inventoryItem.serialNumber!}</td>
                        <td>
                          ${inventoryItem.softIdentifier!}
                          <#if (inventoryItem.softIdentifier?has_content && inventoryItem.softIdentifier?matches("\\d+"))>
                            <#assign sid = Static["java.lang.Long"].decode(inventoryItem.softIdentifier)/>
                            (0x${Static["java.lang.Long"].toHexString(sid)})
                          </#if>
                        </td>
                        <td>${inventoryItem.activationNumber!}</td>
                        <td>${inventoryItem.activationValidThru!}</td>
                    </tr>
                </#list>
            </tbody>
        </table>
    </div>
</div>

