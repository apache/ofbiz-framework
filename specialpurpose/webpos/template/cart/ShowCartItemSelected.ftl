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
<#assign focusOnQuantity = requestParameters.focusOnQuantity!/>
<#assign cartLineIndex = requestParameters.cartLineIndex!/>
<#if cartLineIndex?? && cartLineIndex?has_content>
  <#assign isInteger = Static["org.ofbiz.base.util.UtilValidate"].isInteger(cartLineIndex)>
  <#if isInteger>
    <#assign idx = cartLineIndex?number>
    <#assign cartLine = shoppingCart.findCartItem(idx)!>
    <#if cartLine?? && cartLine?has_content>
      <#if cartLine.getProductId()??>
        <#assign smallImageUrl = Static["org.ofbiz.product.product.ProductContentWrapper"].getProductContentAsText(cartLine.getProduct(), "SMALL_IMAGE_URL", locale, dispatcher, "url")!>
        <#if !smallImageUrl?string?has_content>
          <#assign smallImageUrl = "/images/defaultImage.jpg">
        </#if>
        <#if smallImageUrl?string?has_content>
        <div id="CartItemSelectedLeft">
          <img src="<@ofbizContentUrl>${requestAttributes.contentPathPrefix!}${smallImageUrl}</@ofbizContentUrl>" align="left" class="cssImgSmall" />
        </div>
        </#if>
        <div id="CartItemSelectedRight">
          <#if cartLine.getProductId()?has_content>
            ${cartLine.getProductId()}
            <br/>
          </#if>
          <#if cartLine.getName()?has_content>
            ${cartLine.getName()}
          <#else>
            <#if cartLine.getDescription()?has_content>
            ${cartLine.getDescription()}
            </#if>
          </#if>
      <#else>
        <div id="CartItemSelectedRight">
          <#-- this is a non-product item -->
          <b>${cartLine.getItemTypeDescription()!}</b> : ${cartLine.getName()!}
      </#if>
      <br/>
      <b>${uiLabelMap.CommonQuantity}</b>&nbsp;
      <input type="text" id="itemQuantity" name="itemQuantity" value="${cartLine.getQuantity()}" size="5" maxlength="5"/>
      <a href="javascript:void(0);" id="incrementQuantity"><img src="/images/expand.gif"></a>
      <a href="javascript:void(0);" id="decrementQuantity"><img src="/images/collapse.gif"></a>
      <br/>
      <#if isManager?default(false)>
        <b>${uiLabelMap.WebPosManagerModifyPriceNewPrice}</b>&nbsp;
        <input type="hidden" id="cartLineIdx" name="cartLineIdx" value="${idx}"/>
        <input type="text" id="modifyPrice" name="modifyPrice" value="${cartLine.getDisplayPrice()}" size="8"/>
        <br/>
      </#if>
      <input type="hidden" id="lineIndex" name="lineIndex" value="${cartLineIndex}"/>
      <a id="updateCartItem" name="updateCartItem" href="javascript:updateCartItem();" class="buttontext">${uiLabelMap.CommonUpdate}</a>
      <a id="deleteCartItem" name="deleteCartItem" href="javascript:deleteCartItem('${cartLineIndex}');" class="buttontext">${uiLabelMap.CommonDelete}</a>
    </div>
    <script language="JavaScript" type="text/javascript">
      cartItemSelectedEvents('${focusOnQuantity}');
    </script>
    </#if>
  </#if>
</#if>
