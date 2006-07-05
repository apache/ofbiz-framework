<#--
 *  Copyright (c) 2003 The Open For Business Project - www.ofbiz.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a 
 *  copy of this software and associated documentation files (the "Software"), 
 *  to deal in the Software without restriction, including without limitation 
 *  the rights to use, copy, modify, merge, publish, distribute, sublicense, 
 *  and/or sell copies of the Software, and to permit persons to whom the 
 *  Software is furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included 
 *  in all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS 
 *  OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF 
 *  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. 
 *  IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY 
 *  CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT 
 *  OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR 
 *  THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 *@author     David E. Jones (jonesde@ofbiz.org)
 *@author     Catherine.Heintz@nereide.biz (migration to UiLabel)
 *@version    $Rev$
 *@since      2.2
-->
<#if (requestAttributes.uiLabelMap)?exists><#assign uiLabelMap = requestAttributes.uiLabelMap></#if>
<#assign unselectedClassName = "tabButton">
<#assign selectedClassMap = {page.tabButtonItem?default("void") : "tabButtonSelected"}>

<#if product?has_content>
    <div class="tabContainer">
        <a href="<@ofbizUrl>EditProduct?productId=${productId}</@ofbizUrl>" class="${selectedClassMap.EditProduct?default(unselectedClassName)}">${uiLabelMap.ProductProduct}</a>
        <a href="<@ofbizUrl>EditProductPrices?productId=${productId}</@ofbizUrl>" class="${selectedClassMap.EditProductPrices?default(unselectedClassName)}">${uiLabelMap.ProductPrices}</a>
        <a href="<@ofbizUrl>EditProductContent?productId=${productId}</@ofbizUrl>" class="${selectedClassMap.EditProductContent?default(unselectedClassName)}">${uiLabelMap.ProductContent}</a>
        <a href="<@ofbizUrl>EditProductGoodIdentifications?productId=${productId}</@ofbizUrl>" class="${selectedClassMap.EditProductGoodIdentifications?default(unselectedClassName)}">${uiLabelMap.CommonIds}</a>
        <a href="<@ofbizUrl>EditProductCategories?productId=${productId}</@ofbizUrl>" class="${selectedClassMap.EditProductCategories?default(unselectedClassName)}">${uiLabelMap.ProductCategories}</a>
        <a href="<@ofbizUrl>EditProductKeyword?productId=${productId}</@ofbizUrl>" class="${selectedClassMap.EditProductKeyword?default(unselectedClassName)}">${uiLabelMap.ProductKeywords}</a>
        <a href="<@ofbizUrl>EditProductAssoc?productId=${productId}</@ofbizUrl>" class="${selectedClassMap.EditProductAssoc?default(unselectedClassName)}">${uiLabelMap.ProductAssociations}</a>
        <a href="<@ofbizUrl>ViewProductManufacturing?productId=${productId}</@ofbizUrl>" class="${selectedClassMap.ViewProductManufacturing?default(unselectedClassName)}">${uiLabelMap.ProductManufacturing}</a>
        <a href="<@ofbizUrl>EditProductCosts?productId=${productId}</@ofbizUrl>" class="${selectedClassMap.EditProductCosts?default(unselectedClassName)}">${uiLabelMap.ProductCosts}</a>
        <a href="<@ofbizUrl>EditProductAttributes?productId=${productId}</@ofbizUrl>" class="${selectedClassMap.EditProductAttributes?default(unselectedClassName)}">${uiLabelMap.ProductAttributes}</a>
        <a href="<@ofbizUrl>EditProductFeatures?productId=${productId}</@ofbizUrl>" class="${selectedClassMap.EditProductFeatures?default(unselectedClassName)}">${uiLabelMap.ProductFeatures}</a>
        <a href="<@ofbizUrl>EditProductFacilities?productId=${productId}</@ofbizUrl>" class="${selectedClassMap.EditProductFacilities?default(unselectedClassName)}">${uiLabelMap.ProductFacilities}</a>
        <a href="<@ofbizUrl>EditProductFacilityLocations?productId=${productId}</@ofbizUrl>" class="${selectedClassMap.EditProductFacilityLocations?default(unselectedClassName)}">${uiLabelMap.ProductLocations}</a>
        <a href="<@ofbizUrl>EditProductInventoryItems?productId=${productId}</@ofbizUrl>" class="${selectedClassMap.EditProductInventoryItems?default(unselectedClassName)}">${uiLabelMap.ProductInventory}</a>
        <a href="<@ofbizUrl>EditProductSuppliers?productId=${productId}</@ofbizUrl>" class="${selectedClassMap.EditSupplierProduct?default(unselectedClassName)}">${uiLabelMap.ProductSuppliers}</a>
        <a href="<@ofbizUrl>EditProductGlAccounts?productId=${productId}</@ofbizUrl>" class="${selectedClassMap.EditProductGlAccounts?default(unselectedClassName)}">${uiLabelMap.ProductAccounts}</a>
        <a href="<@ofbizUrl>EditProductPaymentMethodTypes?productId=${productId}</@ofbizUrl>" class="${selectedClassMap.EditProductPaymentMethodTypes?default(unselectedClassName)}">${uiLabelMap.ProductPaymentTypes}</a>
        <a href="<@ofbizUrl>EditProductMaints?productId=${productId}</@ofbizUrl>" class="${selectedClassMap.EditProductMaints?default(unselectedClassName)}">${uiLabelMap.ProductMaintenance}</a>
        <a href="<@ofbizUrl>EditProductMeters?productId=${productId}</@ofbizUrl>" class="${selectedClassMap.EditProductMeters?default(unselectedClassName)}">${uiLabelMap.ProductMeters}</a>
        <a href="<@ofbizUrl>EditProductSubscriptionResources?productId=${productId}</@ofbizUrl>" class="${selectedClassMap.EditProductSubscriptionResources?default(unselectedClassName)}">${uiLabelMap.ProductSubscriptionResources}</a>
        <a href="<@ofbizUrl>EditProductQuickAdmin?productId=${productId}</@ofbizUrl>" class="${selectedClassMap.EditProductQuickAdmin?default(unselectedClassName)}">${uiLabelMap.ProductQuickAdmin}</a>
        <#if product?exists && product.isVirtual?if_exists == "Y">
            <a href="<@ofbizUrl>QuickAddVariants?productId=${productId}</@ofbizUrl>" class="${selectedClassMap.QuickAddVariants?default(unselectedClassName)}">${uiLabelMap.ProductVariants}</a>
        </#if>
        <#if product?exists && product.productTypeId?if_exists == "AGGREGATED">
            <a href="<@ofbizUrl>EditProductConfigs?productId=${productId}</@ofbizUrl>" class="${selectedClassMap.EditProductConfigs?default(unselectedClassName)}">${uiLabelMap.ProductConfigs}</a>
        </#if>
        <#if product?exists && product.productTypeId?if_exists == "ASSET_USAGE">
            <a href="<@ofbizUrl>EditProductAssetUsage?productId=${productId}</@ofbizUrl>" class="${selectedClassMap.EditProductAssetUsage?default(unselectedClassName)}">${uiLabelMap.ProductAssetUsage}</a>
        </#if>
    </div>
</#if>
