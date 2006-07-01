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
 *@author     Andy Zeneski (jaz@ofbiz.org)
 *@author     David E. Jones (jonesde@ofbiz.org)
 *@version    $Rev$
 *@since      2.1
-->
<#if product?exists>
    <#-- variable setup -->
    <#assign targetRequestName = "product">
    <#if requestAttributes.targetRequestName?has_content>
        <#assign targetRequestName = requestAttributes.targetRequestName>
    </#if>
    <#assign smallImageUrl = productContentWrapper.get("SMALL_IMAGE_URL")?if_exists>
    <#if !smallImageUrl?has_content><#assign smallImageUrl = "/images/defaultImage.jpg"></#if>
    <#-- end variable setup -->

    <div class="productsummary">
        <div class="smallimage">
            <a href="<@ofbizUrl>${targetRequestName}/<#if categoryId?exists>~category_id=${categoryId}/</#if>~product_id=${product.productId}</@ofbizUrl>">
                <img src="<@ofbizContentUrl>${contentPathPrefix?if_exists}${smallImageUrl}</@ofbizContentUrl>" alt="Small Image"/>
            </a>
        </div>
        <div class="productbuy">
          <#-- check to see if introductionDate hasn't passed yet -->
          <#if product.introductionDate?exists && nowTimestamp.before(product.introductionDate)>
            <div class="tabletext" style="color: red;">${uiLabelMap.ProductNotYetAvailable}</div>
          <#-- check to see if salesDiscontinuationDate has passed -->
          <#elseif product.salesDiscontinuationDate?exists && nowTimestamp.after(product.salesDiscontinuationDate)>
            <div class="tabletext" style="color: red;">${uiLabelMap.ProductNoLongerAvailable}</div>
          <#-- check to see if it is a rental item; will enter parameters on the detail screen-->
          <#elseif product.productTypeId?if_exists == "ASSET_USAGE">
            <a href="<@ofbizUrl>product/<#if categoryId?exists>~category_id=${categoryId}/</#if>~product_id=${product.productId}</@ofbizUrl>" class="buttontext">${uiLabelMap.EcommerceMakeBooking}...</a>
          <#-- check to see if it is an aggregated or configurable product; will enter parameters on the detail screen-->
          <#elseif product.productTypeId?if_exists == "AGGREGATED">
            <a href="<@ofbizUrl>product/<#if categoryId?exists>~category_id=${categoryId}/</#if>~product_id=${product.productId}</@ofbizUrl>" class="buttontext">${uiLabelMap.EcommerceConfigure}...</a>
          <#-- check to see if the product is a virtual product -->
          <#elseif product.isVirtual?exists && product.isVirtual == "Y">
            <a href="<@ofbizUrl>product/<#if categoryId?exists>~category_id=${categoryId}/</#if>~product_id=${product.productId}</@ofbizUrl>" class="buttontext">${uiLabelMap.EcommerceChooseVariations}...</a>
          <#-- check to see if the product requires an amount -->
          <#elseif product.requireAmount?exists && product.requireAmount == "Y">
            <a href="<@ofbizUrl>product/<#if categoryId?exists>~category_id=${categoryId}/</#if>~product_id=${product.productId}</@ofbizUrl>" class="buttontext">${uiLabelMap.EcommerceChooseAmount}...</a>
          <#else>
            <form method="post" action="<@ofbizUrl>additem<#if requestAttributes._CURRENT_VIEW_?exists>/${requestAttributes._CURRENT_VIEW_}</#if></@ofbizUrl>" name="the${requestAttributes.formNamePrefix?if_exists}${requestAttributes.listIndex?if_exists}form" style="margin: 0;">
              <input type="hidden" name="add_product_id" value="${product.productId}"/>
              <input type="text" class="inputBox" size="5" name="quantity" value="1"/>
              <#if requestParameters.product_id?has_content><input type="hidden" name="product_id" value="${requestParameters.product_id}"/></#if>
              <#if requestParameters.category_id?has_content><input type="hidden" name="category_id" value="${requestParameters.category_id}"/></#if>
              <#if requestParameters.productPromoId?has_content><input type="hidden" name="productPromoId" value="${requestParameters.productPromoId}"/></#if>
              <#if requestParameters.VIEW_INDEX?has_content><input type="hidden" name="VIEW_INDEX" value="${requestParameters.VIEW_INDEX}"/></#if>
              <#if requestParameters.VIEW_SIZE?has_content><input type="hidden" name="VIEW_SIZE" value="${requestParameters.VIEW_SIZE}"/></#if>
              <input type="hidden" name="clearSearch" value="N"/>              
              <a href="javascript:document.the${requestAttributes.formNamePrefix?if_exists}${requestAttributes.listIndex?if_exists}form.submit()" class="buttontext">${uiLabelMap.EcommerceAddtoCart}</a>
            </form>

            <#if requestAttributes.productCategoryMember?exists>
                <#assign prodCatMem = requestAttributes.productCategoryMember>
                <#if prodCatMem?exists && prodCatMem.quantity?exists && 0.00 < prodCatMem.quantity?double>
                <form method="post" action="<@ofbizUrl>additem<#if requestAttributes._CURRENT_VIEW_?exists>/${requestAttributes._CURRENT_VIEW_}</#if></@ofbizUrl>" name="the${requestAttributes.formNamePrefix?if_exists}${requestAttributes.listIndex?if_exists}defaultform" style="margin: 0;">
                  <input type="hidden" name="add_product_id" value="${prodCatMem.productId?if_exists}"/>
                  <input type="hidden" name="quantity" value="${prodCatMem.quantity?if_exists}"/>
                  <#if requestParameters.product_id?has_content><input type="hidden" name="product_id" value="${requestParameters.product_id}"/></#if>
                  <#if requestParameters.category_id?has_content><input type="hidden" name="category_id" value="${requestParameters.category_id}"/></#if>
                  <#if requestParameters.productPromoId?has_content><input type="hidden" name="productPromoId" value="${requestParameters.productPromoId}"/></#if>
                  <#if requestParameters.VIEW_INDEX?has_content><input type="hidden" name="VIEW_INDEX" value="${requestParameters.VIEW_INDEX}"/></#if>
                  <#if requestParameters.VIEW_SIZE?has_content><input type="hidden" name="VIEW_SIZE" value="${requestParameters.VIEW_SIZE}"/></#if>
                  <input type="hidden" name="clearSearch" value="N"/>
                  <a href="javascript:document.the${requestAttributes.formNamePrefix?if_exists}${requestAttributes.listIndex?if_exists}defaultform.submit()" class="buttontext">${uiLabelMap.CommonAddDefault}(${prodCatMem.quantity?string.number}) ${uiLabelMap.EcommerceToCart}</a>
                </form>
              </#if>
            </#if>
          </#if>
        </div>
        <div class="productinfo">
          <div class="tabletext">
            <a href="<@ofbizUrl>${targetRequestName}/<#if categoryId?exists>~category_id=${categoryId}/</#if>~product_id=${product.productId}</@ofbizUrl>" class="linktext">${productContentWrapper.get("PRODUCT_NAME")?if_exists}</a>
          </div>
          <div class="tabletext">${productContentWrapper.get("DESCRIPTION")?if_exists}<#if daysToShip?exists>&nbsp;-&nbsp;${uiLabelMap.ProductUsuallyShipsIn} <b>${daysToShip}</b> ${uiLabelMap.CommonDays}!</#if></div>
          <#-- example of showing a certain type of feature with the product -->
          <#if sizeProductFeatureAndAppls?has_content>
            <div class="tabletext">
              <#if (sizeProductFeatureAndAppls?size == 1)>
                Size:
              <#else>
                Sizes Available:
              </#if>
              <#list sizeProductFeatureAndAppls as sizeProductFeatureAndAppl>
                ${sizeProductFeatureAndAppl.abbrev?default(sizeProductFeatureAndAppl.description?default(sizeProductFeatureAndAppl.productFeatureId))}<#if sizeProductFeatureAndAppl_has_next>,</#if>
              </#list>
            </div>
          </#if>
          <div class="tabletext">
              <b>${product.productId?if_exists}</b>
                <#if price.competitivePrice?exists && price.price?exists && price.price?double < price.competitivePrice?double>
                  ${uiLabelMap.ProductCompareAtPrice}: <span class='basePrice'><@ofbizCurrency amount=price.competitivePrice isoCode=price.currencyUsed/></span>
                </#if>
                <#if price.listPrice?exists && price.price?exists && price.price?double < price.listPrice?double>
                  ${uiLabelMap.ProductListPrice}: <span class="basePrice"><@ofbizCurrency amount=price.listPrice isoCode=price.currencyUsed/></span>
                </#if>
                <b>
                  <#if price.isSale?exists && price.isSale>
                    <span class="salePrice">${uiLabelMap.EcommerceOnSale}!</span>
                    <#assign priceStyle = "salePrice">
                  <#else>
                    <#assign priceStyle = "regularPrice">
                  </#if>

                  <#if (price.price?default(0) > 0 && product.requireAmount?default("N") == "N")>
                    ${uiLabelMap.EcommerceYourPrice}: <#if "Y" = product.isVirtual?if_exists> ${uiLabelMap.CommonFrom} </#if><span class="${priceStyle}"><@ofbizCurrency amount=price.price isoCode=price.currencyUsed/></span>
                  </#if>
                </b>
                <#if price.listPrice?exists && price.price?exists && price.price?double < price.listPrice?double>
                  <#assign priceSaved = price.listPrice?double - price.price?double>
                  <#assign percentSaved = (priceSaved?double / price.listPrice?double) * 100>
                    ${uiLabelMap.EcommerceSave}: <span class="basePrice"><@ofbizCurrency amount=priceSaved isoCode=price.currencyUsed/> (${percentSaved?int}%)</span>
                </#if>
                <#-- show price details ("showPriceDetails" field can be set in the screen definition) -->
                <#if (showPriceDetails?exists && showPriceDetails?default("N") == "Y")>
                    <#if price.orderItemPriceInfos?exists>
                        <#list price.orderItemPriceInfos as orderItemPriceInfo>
                            <div class="tabletext">${orderItemPriceInfo.description?if_exists}</div>
                        </#list>
                    </#if>
                </#if>
          </div>
          <#if averageRating?exists && (averageRating?double > 0) && numRatings?exists && (numRatings?long > 2)>
              <div class="tabletext">${uiLabelMap.EcommerceAverageRating}: ${averageRating} (${uiLabelMap.CommonFrom} ${numRatings} ${uiLabelMap.EcommerceRatings})</div>
          </#if>
        </div>
    </div>
<#else>
&nbsp;${uiLabelMap.ProductErrorProductNotFound}.<br/>
</#if>
