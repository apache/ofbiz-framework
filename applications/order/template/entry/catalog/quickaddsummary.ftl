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

<#if product??>
  <span class="pid">
    <div>
      <b>${product.productId}</b>
    </div>
  </span>
  <span class="name">
      <a href="<@ofbizUrl>product?product_id=${product.productId}</@ofbizUrl>" class="buttontext">${productContentWrapper.get("PRODUCT_NAME", "html")!}</a>
  </span>
  <span class="listPrice">
      <#if price.listPrice?? && price.price?? && price.price?double < price.listPrice?double>
        ${uiLabelMap.ProductListPrice}: <@ofbizCurrency amount=price.listPrice isoCode=price.currencyUsed/>
      <#else>
        &nbsp;
      </#if>
  </span>
  <span class="totalPrice">
    <#if totalPrice??>
        <div>${uiLabelMap.ProductAggregatedPrice}: <span class='basePrice'><@ofbizCurrency amount=totalPrice isoCode=totalPrice.currencyUsed/></span></div>
    <#else>
      <div class="<#if price.isSale?? && price.isSale>salePrice<#else>normalPrice</#if>">
        <b><@ofbizCurrency amount=price.price isoCode=price.currencyUsed/></b>
      </div>
    </#if>
  </span>
  <span class="qty">
    <#-- check to see if introductionDate hasn't passed yet -->
    <#if product.introductionDate?? && nowTimestamp.before(product.introductionDate)>
      ${uiLabelMap.ProductNotYetAvailable}
    <#-- check to see if salesDiscontinuationDate has passed -->
    <#elseif product.salesDiscontinuationDate?? && nowTimestamp.before(product.salesDiscontinuationDate)>
      ${uiLabelMap.ProductNoLongerAvailable}
    <#-- check to see if the product is a virtual product -->
    <#elseif product.isVirtual?default("N") == "Y">
        <a href="<@ofbizUrl>product?<#if categoryId??>category_id=${categoryId}&amp;</#if>product_id=${product.productId}</@ofbizUrl>" class="buttontext">${uiLabelMap.OrderChooseVariations}...</a>
    <#else>
        <input type="text" size="5" name="quantity_${product.productId}" value="" />
    </#if>
  </span>
<#else>
  <h1>${uiLabelMap.ProductErrorProductNotFound}.</h1>
</#if>


