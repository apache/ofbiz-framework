<#--
 *  Copyright (c) 2003-2005 The Open For Business Project - www.ofbiz.org
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
 *@version    $Rev$
 *@since      2.1
-->
<#if miniProduct?exists>
    <a href="<@ofbizUrl>product/~product_id=${miniProduct.productId}</@ofbizUrl>" class="linktext">${miniProductContentWrapper.get("PRODUCT_NAME")?default("No Name Available")}</a>
    <div class="tabletext"><b>${miniProduct.productId}</b>
      <#if (priceResult.price?default(0) > 0 && miniProduct.requireAmount?default("N") == "N")>
        <#if "Y" = miniProduct.isVirtual?if_exists> ${uiLabelMap.CommonFrom} </#if><b><span class="<#if priceResult.isSale>salePrice<#else>normalPrice</#if>"><@ofbizCurrency amount=priceResult.price isoCode=priceResult.currencyUsed/></span></b>
      </#if>
    </div>

    <div style="margin-top: 4px;">
    <#if (miniProduct.introductionDate?exists) && (nowTimeLong < miniProduct.introductionDate.getTime())>
        <#-- check to see if introductionDate hasn't passed yet -->
        <div class="tabletext" style="color: red;">${uiLabelMap.ProductNotYetAvailable}</div>
    <#elseif (miniProduct.salesDiscontinuationDate?exists) && (nowTimeLong > miniProduct.salesDiscontinuationDate.getTime())>
        <#-- check to see if salesDiscontinuationDate has passed -->
        <div class="tabletext" style="color: red;">${uiLabelMap.ProductNoLongerAvailable}</div>
    <#elseif miniProduct.isVirtual?default("N") == "Y">
        <a href="<@ofbizUrl>product/<#if requestParameters.category_id?exists>~category_id=${requestParameters.category_id}/</#if>~product_id=${miniProduct.productId}</@ofbizUrl>" class="buttontext"><nobr>${uiLabelMap.EcommerceChooseVariations}...</nobr></a>
    <#elseif miniProduct.requireAmount?default("N") == "Y">
        <a href="<@ofbizUrl>product/<#if requestParameters.category_id?exists>~category_id=${requestParameters.category_id}/</#if>~product_id=${miniProduct.productId}</@ofbizUrl>" class="buttontext"><nobr>${uiLabelMap.EcommerceChooseAmount}...</nobr></a>
    <#else>
        <form method="post" action="<@ofbizUrl>additem<#if requestAttributes._CURRENT_VIEW_?has_content>/${requestAttributes._CURRENT_VIEW_}</#if></@ofbizUrl>" name="${miniProdFormName}" style="margin: 0;">
            <input type="hidden" name="add_product_id" value="${miniProduct.productId}"/>
            <input type="hidden" name="quantity" value="${miniProdQuantity?default("1")}"/>
            <#if requestParameters.orderId?has_content><input type="hidden" name="orderId" value="${requestParameters.orderId}"/></#if>
            <#if requestParameters.product_id?has_content><input type="hidden" name="product_id" value="${requestParameters.product_id}"/></#if>
            <#if requestParameters.category_id?has_content><input type="hidden" name="category_id" value="${requestParameters.category_id}"/></#if>
            <#if requestParameters.VIEW_INDEX?has_content><input type="hidden" name="VIEW_INDEX" value="${requestParameters.VIEW_INDEX}"/></#if>
            <#if requestParameters.VIEW_SIZE?has_content><input type="hidden" name="VIEW_SIZE" value="${requestParameters.VIEW_SIZE}"/></#if>
            <input type="hidden" name="clearSearch" value="N"/>
            <a href="javascript:document.${miniProdFormName}.submit()" class="buttontext"><nobr>${uiLabelMap.CommonAdd} ${miniProdQuantity} ${uiLabelMap.EcommerceToCart}</nobr></a>
        </form>
    </#if>
    </div>
</#if>
