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

<items>
<#list googleCart.items as item>
    <item>
        <item-name>${item.itemName}</item-name>
        <item-description>${item.description}</item-description>
        <unit-price currency="USD">${item.unitPrice}</unit-price>
        <quantity>${item.quantity}</quantity>

        <#if item.merchantItemId??>
            <merchantItemId>${item.merchantItemId}</merchantItemId>
        </#if>
        <#if item.merchantPrivateItemData??>
        <merchant-private-item-data>
            <#list item.merchantPrivateItemData as itemData>
                <${itemData.name}>${itemData.value}</${itemData.name}>
            </#list>
        </merchant-private-item-data>
        </#if>
        <#if item.taxTable??>
        <tax-table-selector>${taxTable}</tax-table-selector>
        </#if>
    </item>
</#list>
</items>
