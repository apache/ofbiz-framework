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
 *@author     Andy Zeneski (jaz@ofbiz.org)
 *@version    $Rev$
 *@since      2.1
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

