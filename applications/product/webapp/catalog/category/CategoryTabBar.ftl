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
 *@author     Olivier.Heintz@nereide.biz (migration to UiLabel )
 *@version    $Rev$
 *@since      2.2
-->

<#if requestAttributes.uiLabelMap?exists><#assign uiLabelMap = requestAttributes.uiLabelMap></#if>
<#assign unselectedClassName = "tabButton">
<#assign selectedClassMap = {page.tabButtonItem?default("void") : "tabButtonSelected"}>

<#if productCategory?has_content> 
    <div class="tabContainer">
        <a href="<@ofbizUrl>EditCategory?productCategoryId=${productCategoryId}</@ofbizUrl>" class="${selectedClassMap.EditCategory?default(unselectedClassName)}">${uiLabelMap.ProductCategory}</a>
        <a href="<@ofbizUrl>EditCategoryContent?productCategoryId=${productCategoryId}</@ofbizUrl>" class="${selectedClassMap.EditCategoryContent?default(unselectedClassName)}">${uiLabelMap.ProductCategoryContent}</a>
        <a href="<@ofbizUrl>EditCategoryRollup?showProductCategoryId=${productCategoryId}</@ofbizUrl>" class="${selectedClassMap.EditCategoryRollup?default(unselectedClassName)}">${uiLabelMap.ProductRollupShort}</a>
        <a href="<@ofbizUrl>EditCategoryProducts?productCategoryId=${productCategoryId}</@ofbizUrl>" class="${selectedClassMap.EditCategoryProducts?default(unselectedClassName)}">${uiLabelMap.ProductProducts}</a>
        <a href="<@ofbizUrl>EditCategoryProdCatalogs?productCategoryId=${productCategoryId}</@ofbizUrl>" class="${selectedClassMap.EditCategoryProdCatalogs?default(unselectedClassName)}">${uiLabelMap.ProductCatalogs}</a>
        <a href="<@ofbizUrl>EditCategoryFeatureCats?productCategoryId=${productCategoryId}</@ofbizUrl>" class="${selectedClassMap.EditCategoryFeatureCats?default(unselectedClassName)}">${uiLabelMap.ProductFeatureCats}</a>
        <a href="<@ofbizUrl>EditCategoryParties?productCategoryId=${productCategoryId}</@ofbizUrl>" class="${selectedClassMap.EditCategoryParties?default(unselectedClassName)}">${uiLabelMap.PartyParties}</a>
        <a href="<@ofbizUrl>EditCategoryAttributes?productCategoryId=${productCategoryId}</@ofbizUrl>" class="${selectedClassMap.EditCategoryAttributes?default(unselectedClassName)}">${uiLabelMap.ProductAttributes}</a>
    </div>
</#if> 
