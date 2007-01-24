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
  <td align="left" valign="middle" width="5%">
    <div class="tabletext">
      <b>${product.productId}</b>
    </div>
  </td>
  <td align="left" valign="middle" width="90%">
    <a href="<@ofbizUrl>product?product_id=${product.productId}</@ofbizUrl>" class="buttontext">${productContentWrapper.get("PRODUCT_NAME")?if_exists}</a>
  </td>
  <td align="left" valign="middle" width="5%">
    <div class="tabletext">
      <#if price.listPrice?exists && price.price?exists && price.price?double < price.listPrice?double>
        ${uiLabelMap.ProductListPrice}:<@ofbizCurrency amount=price.listPrice isoCode=price.currencyUsed/>
      <#else>
        &nbsp;
      </#if>
    </div>
  </td>
  <td align="right" valign="middle" width="5%">
    <div class="<#if price.isSale?exists && price.isSale>salePrice<#else>normalPrice</#if>">
      <b><@ofbizCurrency amount=price.price isoCode=price.currencyUsed/></b>
    </div>
  </td>                                 
  <td align="right" valign="middle">
    <#-- check to see if introductionDate hasn't passed yet -->
    <#if product.introductionDate?exists && nowTimestamp.before(product.introductionDate)>
      <div class="tabletext" style="color: red;">${uiLabelMap.ProductNotYetAvailable}</div>
    <#-- check to see if salesDiscontinuationDate has passed -->
    <#elseif product.salesDiscontinuationDate?exists && nowTimestamp.before(product.salesDiscontinuationDate)>
      <div class="tabletext" style="color: red;">${uiLabelMap.ProductNoLongerAvailable}</div>          
    <#-- check to see if the product is a virtual product -->
    <#elseif product.isVirtual?default("N") == "Y">
      <a href="<@ofbizUrl>product?<#if categoryId?exists>category_id=${categoryId}&</#if>product_id=${product.productId}</@ofbizUrl>" class="buttontext">${uiLabelMap.EcommerceChooseVariations}...</a>
    <#else>                                  
      <input type="text" size="5" class="inputBox" name="quantity_${product.productId}" value="">
    </#if>
  </td>
<#else>
  <div class="head1">${uiLabelMap.ProductErrorProductNotFound}.</div>
</#if>

