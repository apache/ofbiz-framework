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
<#assign unselectedClassName = "">
<#assign selectedClassMap = {page.tabButtonItem?default("void") : "selected"}>

<#if product?has_content>
    <br/>
    <div class="button-bar tab-bar">
        <ul>
            <li><a href="<@ofbizUrl>EditProduct?productId=${productId}</@ofbizUrl>" class="${selectedClassMap.EditProduct?default(unselectedClassName)}">${uiLabelMap.ProductProduct}</a><li>
            <li><a href="<@ofbizUrl>EditProductPrices?productId=${productId}</@ofbizUrl>" class="${selectedClassMap.EditProductPrices?default(unselectedClassName)}">${uiLabelMap.ProductPrices}</a><li>
            <li><a href="<@ofbizUrl>EditProductContent?productId=${productId}</@ofbizUrl>" class="${selectedClassMap.EditProductContent?default(unselectedClassName)}">${uiLabelMap.ProductContent}</a><li>
            <li><a href="<@ofbizUrl>EditProductGeos?productId=${productId}</@ofbizUrl>" class="${selectedClassMap.EditProductGeos?default(unselectedClassName)}">${uiLabelMap.CommonGeos}</a><li>
            <li><a href="<@ofbizUrl>EditProductGoodIdentifications?productId=${productId}</@ofbizUrl>" class="${selectedClassMap.EditProductGoodIdentifications?default(unselectedClassName)}">${uiLabelMap.CommonIds}</a><li>
            <li><a href="<@ofbizUrl>EditProductCategories?productId=${productId}</@ofbizUrl>" class="${selectedClassMap.EditProductCategories?default(unselectedClassName)}">${uiLabelMap.ProductCategories}</a><li>
            <li><a href="<@ofbizUrl>EditProductKeyword?productId=${productId}</@ofbizUrl>" class="${selectedClassMap.EditProductKeyword?default(unselectedClassName)}">${uiLabelMap.ProductKeywords}</a><li>
            <li><a href="<@ofbizUrl>EditProductAssoc?productId=${productId}</@ofbizUrl>" class="${selectedClassMap.EditProductAssoc?default(unselectedClassName)}">${uiLabelMap.ProductAssociations}</a><li>
            <li><a href="<@ofbizUrl>ViewProductManufacturing?productId=${productId}</@ofbizUrl>" class="${selectedClassMap.ViewProductManufacturing?default(unselectedClassName)}">${uiLabelMap.ProductManufacturing}</a><li>
            <li><a href="<@ofbizUrl>EditProductCosts?productId=${productId}</@ofbizUrl>" class="${selectedClassMap.EditProductCosts?default(unselectedClassName)}">${uiLabelMap.ProductCosts}</a><li>
            <li><a href="<@ofbizUrl>EditProductAttributes?productId=${productId}</@ofbizUrl>" class="${selectedClassMap.EditProductAttributes?default(unselectedClassName)}">${uiLabelMap.ProductAttributes}</a><li>
            <li><a href="<@ofbizUrl>EditProductFeatures?productId=${productId}</@ofbizUrl>" class="${selectedClassMap.EditProductFeatures?default(unselectedClassName)}">${uiLabelMap.ProductFeatures}</a><li>
            <li><a href="<@ofbizUrl>EditProductFacilities?productId=${productId}</@ofbizUrl>" class="${selectedClassMap.EditProductFacilities?default(unselectedClassName)}">${uiLabelMap.ProductFacilities}</a><li>
            <li><a href="<@ofbizUrl>EditProductFacilityLocations?productId=${productId}</@ofbizUrl>" class="${selectedClassMap.EditProductFacilityLocations?default(unselectedClassName)}">${uiLabelMap.ProductLocations}</a><li>
            <li><a href="<@ofbizUrl>EditProductInventoryItems?productId=${productId}&showAllFacilities=Y</@ofbizUrl>" class="${selectedClassMap.EditProductInventoryItems?default(unselectedClassName)}">${uiLabelMap.ProductInventory}</a><li>
            <li><a href="<@ofbizUrl>EditProductSuppliers?productId=${productId}</@ofbizUrl>" class="${selectedClassMap.EditSupplierProduct?default(unselectedClassName)}">${uiLabelMap.ProductVendorProduct}</a><li>
            <li><a href="<@ofbizUrl>ViewProductAgreements?productId=${productId}</@ofbizUrl>" class="${selectedClassMap.ViewProductAgreements?default(unselectedClassName)}">${uiLabelMap.ProductAgreements}</a><li>
            <li><a href="<@ofbizUrl>EditProductGlAccounts?productId=${productId}</@ofbizUrl>" class="${selectedClassMap.EditProductGlAccounts?default(unselectedClassName)}">${uiLabelMap.ProductAccounts}</a><li>
            <li><a href="<@ofbizUrl>EditProductPaymentMethodTypes?productId=${productId}</@ofbizUrl>" class="${selectedClassMap.EditProductPaymentMethodTypes?default(unselectedClassName)}">${uiLabelMap.ProductPaymentTypes}</a><li>
            <li><a href="<@ofbizUrl>EditProductMaints?productId=${productId}</@ofbizUrl>" class="${selectedClassMap.EditProductMaints?default(unselectedClassName)}">${uiLabelMap.ProductMaintenance}</a><li>
            <li><a href="<@ofbizUrl>EditProductMeters?productId=${productId}</@ofbizUrl>" class="${selectedClassMap.EditProductMeters?default(unselectedClassName)}">${uiLabelMap.ProductMeters}</a><li>
            <li><a href="<@ofbizUrl>EditProductSubscriptionResources?productId=${productId}</@ofbizUrl>" class="${selectedClassMap.EditProductSubscriptionResources?default(unselectedClassName)}">${uiLabelMap.ProductSubscriptionResources}</a><li>
            <li><a href="<@ofbizUrl>EditProductQuickAdmin?productId=${productId}</@ofbizUrl>" class="${selectedClassMap.EditProductQuickAdmin?default(unselectedClassName)}">${uiLabelMap.ProductQuickAdmin}</a><li>
            <li><a href="<@ofbizUrl>EditVendorProduct?productId=${productId}</@ofbizUrl>" class="${selectedClassMap.EditVendorProduct?default(unselectedClassName)}">${uiLabelMap.PartyVendor}</a><li>

            <#if product?exists && product.isVirtual?if_exists == "Y">
                <li><a href="<@ofbizUrl>QuickAddVariants?productId=${productId}</@ofbizUrl>" class="${selectedClassMap.QuickAddVariants?default(unselectedClassName)}">${uiLabelMap.ProductVariants}</a><li>
            </#if>
            <#if product?exists && product.productTypeId?if_exists == "AGGREGATED">
                <li><a href="<@ofbizUrl>EditProductConfigs?productId=${productId}</@ofbizUrl>" class="${selectedClassMap.EditProductConfigs?default(unselectedClassName)}">${uiLabelMap.ProductConfigs}</a><li>
            </#if>
            <#if product?exists && product.productTypeId?if_exists == "ASSET_USAGE">
                <li><a href="<@ofbizUrl>EditProductAssetUsage?productId=${productId}</@ofbizUrl>" class="${selectedClassMap.EditProductAssetUsage?default(unselectedClassName)}">${uiLabelMap.ProductAssetUsage}</a><li>
            </#if>
            <li><a href="<@ofbizUrl>EditProductWorkEfforts?productId=${productId}</@ofbizUrl>" class="${selectedClassMap.EditProductWorkEfforts?default(unselectedClassName)}">${uiLabelMap.WorkEffortWorkEffort}</a><li>
            <li><a href="<@ofbizUrl>EditProductParties?productId=${productId}</@ofbizUrl>" class="${selectedClassMap.EditProductParties?default(unselectedClassName)}">${uiLabelMap.PartyParties}</a><li>
        </ul>    
    </div>
</#if>
