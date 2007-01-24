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
        <a href="<@ofbizUrl>product/<#if requestParameters.category_id?exists>~category_id=${requestParameters.category_id}/</#if>~product_id=${miniProduct.productId}</@ofbizUrl>" class="buttontext"><span style="white-space: nowrap;">${uiLabelMap.EcommerceChooseVariations}...</span></a>
    <#elseif miniProduct.requireAmount?default("N") == "Y">
        <a href="<@ofbizUrl>product/<#if requestParameters.category_id?exists>~category_id=${requestParameters.category_id}/</#if>~product_id=${miniProduct.productId}</@ofbizUrl>" class="buttontext"><span style="white-space: nowrap;">${uiLabelMap.EcommerceChooseAmount}...</span></a>
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
            <a href="javascript:document.${miniProdFormName}.submit()" class="buttontext"><span style="white-space: nowrap;">${uiLabelMap.CommonAdd} ${miniProdQuantity} ${uiLabelMap.EcommerceToCart}</span></a>
        </form>
    </#if>
    </div>
</#if>
