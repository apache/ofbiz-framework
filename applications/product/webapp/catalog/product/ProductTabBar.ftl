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

<#if (requestAttributes.uiLabelMap)?exists><#assign uiLabelMap = requestAttributes.uiLabelMap></#if>
<#assign selected = tabButtonItem?default("void")>

<#if product?has_content>
    <br/>
    <div class="button-bar tab-bar">
        <ul>
            <li<#if selected="EditProduct"> class="selected"</#if>><a href="<@ofbizUrl>EditProduct?productId=${productId}</@ofbizUrl>">${uiLabelMap.ProductProduct}</a><li>
            <li<#if selected="EditProductPrices"> class="selected"</#if>><a href="<@ofbizUrl>EditProductPrices?productId=${productId}</@ofbizUrl>">${uiLabelMap.ProductPrices}</a><li>
            <li<#if selected="EditProductContent"> class="selected"</#if>><a href="<@ofbizUrl>EditProductContent?productId=${productId}</@ofbizUrl>">${uiLabelMap.ProductContent}</a><li>
            <li<#if selected="EditProductGeos"> class="selected"</#if>><a href="<@ofbizUrl>EditProductGeos?productId=${productId}</@ofbizUrl>">${uiLabelMap.CommonGeos}</a><li>
            <li<#if selected="EditProductGoodIdentifications"> class="selected"</#if>><a href="<@ofbizUrl>EditProductGoodIdentifications?productId=${productId}</@ofbizUrl>">${uiLabelMap.CommonIds}</a><li>
            <li<#if selected="EditProductCategories"> class="selected"</#if>><a href="<@ofbizUrl>EditProductCategories?productId=${productId}</@ofbizUrl>">${uiLabelMap.ProductCategories}</a><li>
            <li<#if selected="EditProductKeyword"> class="selected"</#if>><a href="<@ofbizUrl>EditProductKeyword?productId=${productId}</@ofbizUrl>">${uiLabelMap.ProductKeywords}</a><li>
            <li<#if selected="EditProductAssoc"> class="selected"</#if>><a href="<@ofbizUrl>EditProductAssoc?productId=${productId}</@ofbizUrl>">${uiLabelMap.ProductAssociations}</a><li>
            <li<#if selected="ViewProductManufacturing"> class="selected"</#if>><a href="<@ofbizUrl>ViewProductManufacturing?productId=${productId}</@ofbizUrl>">${uiLabelMap.ProductManufacturing}</a><li>
            <li<#if selected="EditProductCosts"> class="selected"</#if>><a href="<@ofbizUrl>EditProductCosts?productId=${productId}</@ofbizUrl>">${uiLabelMap.ProductCosts}</a><li>
            <li<#if selected="EditProductAttributes"> class="selected"</#if>><a href="<@ofbizUrl>EditProductAttributes?productId=${productId}</@ofbizUrl>">${uiLabelMap.ProductAttributes}</a><li>
            <li<#if selected="EditProductFeatures"> class="selected"</#if>><a href="<@ofbizUrl>EditProductFeatures?productId=${productId}</@ofbizUrl>">${uiLabelMap.ProductFeatures}</a><li>
            <li<#if selected="EditProductFacilities"> class="selected"</#if>><a href="<@ofbizUrl>EditProductFacilities?productId=${productId}</@ofbizUrl>">${uiLabelMap.ProductFacilities}</a><li>
            <li<#if selected="EditProductFacilityLocations"> class="selected"</#if>><a href="<@ofbizUrl>EditProductFacilityLocations?productId=${productId}</@ofbizUrl>">${uiLabelMap.ProductLocations}</a><li>
            <li<#if selected="EditProductInventoryItems"> class="selected"</#if>><a href="<@ofbizUrl>EditProductInventoryItems?productId=${productId}&showAllFacilities=Y</@ofbizUrl>">${uiLabelMap.ProductInventory}</a><li>
            <li<#if selected="EditSupplierProduct"> class="selected"</#if>><a href="<@ofbizUrl>EditProductSuppliers?productId=${productId}</@ofbizUrl>">${uiLabelMap.ProductVendorProduct}</a><li>
            <li<#if selected="ViewProductAgreements"> class="selected"</#if>><a href="<@ofbizUrl>ViewProductAgreements?productId=${productId}</@ofbizUrl>">${uiLabelMap.ProductAgreements}</a><li>
            <li<#if selected="EditProductGlAccounts"> class="selected"</#if>><a href="<@ofbizUrl>EditProductGlAccounts?productId=${productId}</@ofbizUrl>">${uiLabelMap.ProductAccounts}</a><li>
            <li<#if selected="EditProductPaymentMethodTypes"> class="selected"</#if>><a href="<@ofbizUrl>EditProductPaymentMethodTypes?productId=${productId}</@ofbizUrl>">${uiLabelMap.ProductPaymentTypes}</a><li>
            <li<#if selected="EditProductMaints"> class="selected"</#if>><a href="<@ofbizUrl>EditProductMaints?productId=${productId}</@ofbizUrl>">${uiLabelMap.ProductMaintenance}</a><li>
            <li<#if selected="EditProductMeters"> class="selected"</#if>><a href="<@ofbizUrl>EditProductMeters?productId=${productId}</@ofbizUrl>">${uiLabelMap.ProductMeters}</a><li>
            <li<#if selected="EditProductSubscriptionResources"> class="selected"</#if>><a href="<@ofbizUrl>EditProductSubscriptionResources?productId=${productId}</@ofbizUrl>">${uiLabelMap.ProductSubscriptionResources}</a><li>
            <li<#if selected="EditProductQuickAdmin"> class="selected"</#if>><a href="<@ofbizUrl>EditProductQuickAdmin?productId=${productId}</@ofbizUrl>">${uiLabelMap.ProductQuickAdmin}</a><li>
            <li<#if selected="EditVendorProduct"> class="selected"</#if>><a href="<@ofbizUrl>EditVendorProduct?productId=${productId}</@ofbizUrl>">${uiLabelMap.PartyVendor}</a><li>

            <#if product?exists && product.isVirtual?if_exists == "Y">
                <li<#if selected="QuickAddVariants"> class="selected"</#if>><a href="<@ofbizUrl>QuickAddVariants?productId=${productId}</@ofbizUrl>">${uiLabelMap.ProductVariants}</a><li>
            </#if>
            <#if product?exists && product.productTypeId?if_exists == "AGGREGATED">
                <li<#if selected="EditProductConfigs"> class="selected"</#if>><a href="<@ofbizUrl>EditProductConfigs?productId=${productId}</@ofbizUrl>">${uiLabelMap.ProductConfigs}</a><li>
            </#if>
            <#if product?exists && product.productTypeId?if_exists == "ASSET_USAGE">
                <li<#if selected="EditProductAssetUsage"> class="selected"</#if>><a href="<@ofbizUrl>EditProductAssetUsage?productId=${productId}</@ofbizUrl>">${uiLabelMap.ProductAssetUsage}</a><li>
            </#if>
            <li<#if selected="EditProductWorkEfforts"> class="selected"</#if>><a href="<@ofbizUrl>EditProductWorkEfforts?productId=${productId}</@ofbizUrl>">${uiLabelMap.WorkEffortWorkEffort}</a><li>
            <li<#if selected="EditProductParties"> class="selected"</#if>><a href="<@ofbizUrl>EditProductParties?productId=${productId}</@ofbizUrl>">${uiLabelMap.PartyParties}</a><li>
        </ul>
      <br/>                
    </div>
    <br/>
</#if>