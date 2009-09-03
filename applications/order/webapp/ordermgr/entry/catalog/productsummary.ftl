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
<#if product?exists>
    <#-- variable setup -->
    <#assign productUrl = Static["org.ofbiz.product.category.CatalogUrlServlet"].makeCatalogUrl(request, product.productId, categoryId, "")/>

    <#if requestAttributes.productCategoryMember?exists>
        <#assign prodCatMem = requestAttributes.productCategoryMember>
    </#if>
    <#assign smallImageUrl = productContentWrapper.get("SMALL_IMAGE_URL")?if_exists>
    <#if !smallImageUrl?string?has_content><#assign smallImageUrl = "/images/defaultImage.jpg"></#if>
    <#-- end variable setup -->
    <#assign productInfoLinkId = "productInfoLink">
    <#assign productInfoLinkId = productInfoLinkId + product.productId/>
    <#assign productDetailId = "productDetailId"/>
    <#assign productDetailId = productDetailId + product.productId/>
    <div class="productsummary">
        <div class="smallimage">
            <a href="${productUrl}">
                <span id="${productInfoLinkId}" class="popup_link"><img src="<@ofbizContentUrl>${contentPathPrefix?if_exists}${smallImageUrl}</@ofbizContentUrl>" alt="Small Image"/></span>
            </a>
        </div>
        <div id="${productDetailId}" class="popup" >
          <table>
            <tr valign="top">
              <td>
                <img src="<@ofbizContentUrl>${contentPathPrefix?if_exists}${smallImageUrl}</@ofbizContentUrl>" alt="Small Image"/><br/>
                ${uiLabelMap.ProductProductId}   : ${product.productId?if_exists}<br/>
                ${uiLabelMap.ProductProductName} : ${product.productName?if_exists}<br/>
                ${uiLabelMap.CommonDescription}  : ${product.description?if_exists}
              </td>
            </tr>
          </table>
        </div>
        <script type="text/javascript">
          new Popup('${productDetailId}','${productInfoLinkId}', {position: 'none'})
        </script>
        <div class="productbuy">
          <#-- check to see if introductionDate hasn't passed yet -->
          <#if product.introductionDate?exists && nowTimestamp.before(product.introductionDate)>
            <div style="color: red;">${uiLabelMap.ProductNotYetAvailable}</div>
          <#-- check to see if salesDiscontinuationDate has passed -->
          <#elseif product.salesDiscontinuationDate?exists && nowTimestamp.after(product.salesDiscontinuationDate)>
            <div style="color: red;">${uiLabelMap.ProductNoLongerAvailable}</div>
          <#-- check to see if it is a rental item; will enter parameters on the detail screen-->
          <#elseif product.productTypeId?if_exists == "ASSET_USAGE">
            <a href="${productUrl}" class="buttontext">${uiLabelMap.OrderMakeBooking}...</a>
          <#-- check to see if it is an aggregated or configurable product; will enter parameters on the detail screen-->
          <#elseif product.productTypeId?if_exists == "AGGREGATED">
            <a href="${productUrl}" class="buttontext">${uiLabelMap.OrderConfigure}...</a>
          <#-- check to see if the product is a virtual product -->
          <#elseif product.isVirtual?exists && product.isVirtual == "Y">
            <a href="${productUrl}" class="buttontext">${uiLabelMap.OrderChooseVariations}...</a>
          <#-- check to see if the product requires an amount -->
          <#elseif product.requireAmount?exists && product.requireAmount == "Y">
            <a href="${productUrl}" class="buttontext">${uiLabelMap.OrderChooseAmount}...</a>
          <#else>
            <form method="post" action="<@ofbizUrl>additem</@ofbizUrl>" name="the${requestAttributes.formNamePrefix?if_exists}${requestAttributes.listIndex?if_exists}form" style="margin: 0;">
              <input type="hidden" name="add_product_id" value="${product.productId}"/>
              <input type="text" size="5" name="quantity" value="1"/>
              <input type="hidden" name="clearSearch" value="N"/>
              <a href="javascript:document.the${requestAttributes.formNamePrefix?if_exists}${requestAttributes.listIndex?if_exists}form.submit()" class="buttontext">${uiLabelMap.OrderAddToCart}</a>
            </form>

              <#if prodCatMem?exists && prodCatMem.quantity?exists && 0.00 < prodCatMem.quantity?double>
                <form method="post" action="<@ofbizUrl>additem</@ofbizUrl>" name="the${requestAttributes.formNamePrefix?if_exists}${requestAttributes.listIndex?if_exists}defaultform" style="margin: 0;">
                  <input type="hidden" name="add_product_id" value="${prodCatMem.productId?if_exists}"/>
                  <input type="hidden" name="quantity" value="${prodCatMem.quantity?if_exists}"/>
                  <input type="hidden" name="clearSearch" value="N"/>
                  <a href="javascript:document.the${requestAttributes.formNamePrefix?if_exists}${requestAttributes.listIndex?if_exists}defaultform.submit()" class="buttontext">${uiLabelMap.CommonAddDefault}(${prodCatMem.quantity?string.number}) ${uiLabelMap.OrderToCart}</a>
                </form>
              </#if>
          </#if>
        </div>
        <div class="productinfo">
          <div>
            <a href="${productUrl}" class="linktext">${productContentWrapper.get("PRODUCT_NAME")?if_exists}</a>
          </div>
          <div>${productContentWrapper.get("DESCRIPTION")?if_exists}<#if daysToShip?exists>&nbsp;-&nbsp;${uiLabelMap.ProductUsuallyShipsIn} <b>${daysToShip}</b> ${uiLabelMap.CommonDays}!</#if></div>

          <#-- Display category-specific product comments -->
          <#if prodCatMem?exists && prodCatMem.comments?has_content>
          <div>${prodCatMem.comments}</div>
          </#if>

          <#-- example of showing a certain type of feature with the product -->
          <#if sizeProductFeatureAndAppls?has_content>
            <div>
              <#if (sizeProductFeatureAndAppls?size == 1)>
                ${uiLabelMap.SizeAvailableSingle}:
              <#else>
                ${uiLabelMap.SizeAvailableMultiple}:
              </#if>
              <#list sizeProductFeatureAndAppls as sizeProductFeatureAndAppl>
                ${sizeProductFeatureAndAppl.abbrev?default(sizeProductFeatureAndAppl.description?default(sizeProductFeatureAndAppl.productFeatureId))}<#if sizeProductFeatureAndAppl_has_next>,</#if>
              </#list>
            </div>
          </#if>
          <div>
              <b>${product.productId?if_exists}</b>
                <#if totalPrice?exists>
                  <div>${uiLabelMap.ProductAggregatedPrice}: <span class='basePrice'><@ofbizCurrency amount=totalPrice isoCode=totalPrice.currencyUsed/></span></div>
                <#else>
                <#if price.competitivePrice?exists && price.price?exists && price.price?double < price.competitivePrice?double>
                  ${uiLabelMap.ProductCompareAtPrice}: <span class='basePrice'><@ofbizCurrency amount=price.competitivePrice isoCode=price.currencyUsed/></span>
                </#if>
                <#if price.listPrice?exists && price.price?exists && price.price?double < price.listPrice?double>
                  ${uiLabelMap.ProductListPrice}: <span class="basePrice"><@ofbizCurrency amount=price.listPrice isoCode=price.currencyUsed/></span>
                </#if>
                <b>
                  <#if price.isSale?exists && price.isSale>
                    <span class="salePrice">${uiLabelMap.OrderOnSale}!</span>
                    <#assign priceStyle = "salePrice">
                  <#else>
                    <#assign priceStyle = "regularPrice">
                  </#if>

                  <#if (price.price?default(0) > 0 && product.requireAmount?default("N") == "N")>
                    ${uiLabelMap.OrderYourPrice}: <#if "Y" = product.isVirtual?if_exists> ${uiLabelMap.CommonFrom} </#if><span class="${priceStyle}"><@ofbizCurrency amount=price.price isoCode=price.currencyUsed/></span>
                  </#if>
                </b>
                <#if price.listPrice?exists && price.price?exists && price.price?double < price.listPrice?double>
                  <#assign priceSaved = price.listPrice?double - price.price?double>
                  <#assign percentSaved = (priceSaved?double / price.listPrice?double) * 100>
                    ${uiLabelMap.OrderSave}: <span class="basePrice"><@ofbizCurrency amount=priceSaved isoCode=price.currencyUsed/> (${percentSaved?int}%)</span>
                </#if>
                </#if>
                <#-- show price details ("showPriceDetails" field can be set in the screen definition) -->
                <#if (showPriceDetails?exists && showPriceDetails?default("N") == "Y")>
                    <#if price.orderItemPriceInfos?exists>
                        <#list price.orderItemPriceInfos as orderItemPriceInfo>
                            <div>${orderItemPriceInfo.description?if_exists}</div>
                        </#list>
                    </#if>
                </#if>
          </div>
          <#if averageRating?exists && (averageRating?double > 0) && numRatings?exists && (numRatings?long > 2)>
              <div>${uiLabelMap.OrderAverageRating}: ${averageRating} (${uiLabelMap.CommonFrom} ${numRatings} ${uiLabelMap.OrderRatings})</div>
          </#if>
          <form method="post" action="<@ofbizUrl secure="${request.isSecure()?string}">addToCompare</@ofbizUrl>" name="addToCompare${requestAttributes.listIndex?if_exists}form">
              <input type="hidden" name="productId" value="${product.productId}"/>
          </form>
          <a href="javascript:document.addToCompare${requestAttributes.listIndex?if_exists}form.submit()" class="buttontext">${uiLabelMap.ProductAddToCompare}</a>
        </div>
    </div>
<#else>
&nbsp;${uiLabelMap.ProductErrorProductNotFound}.<br/>
</#if>
