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
<input type="hidden" id="selectedItem" name="selectedItem" value="${selectedItem?default(0)}"/>
<input type="hidden" id="cartSize" name="cartSize" value="${shoppingCartSize?default(0)}"/>

<#if (shoppingCartSize > 0)>
<div id="CartHeader">
  <table class="basic-table" cellspacing="1" cellpadding="1">
    <input type="hidden" id="totalDue" value="${totalDue}"/>
    <input type="hidden" id="totalCash" value="${cashAmount}"/>
    <input type="hidden" id="totalCheck" value="${checkAmount}"/>
    <input type="hidden" id="totalGift" value="${giftAmount}"/>
    <input type="hidden" id="totalCredit" value="${creditAmount}"/>
    <input type="hidden" id="totalDueFormatted" value="<@ofbizCurrency amount=totalDue isoCode=shoppingCart.getCurrency()/>"/>
    <input type="hidden" id="totalCashFormatted" value="<@ofbizCurrency amount=cashAmount isoCode=shoppingCart.getCurrency()/>"/>
    <input type="hidden" id="totalCheckFormatted" value="<@ofbizCurrency amount=checkAmount isoCode=shoppingCart.getCurrency()/>"/>
    <input type="hidden" id="totalGiftFormatted" value="<@ofbizCurrency amount=giftAmount isoCode=shoppingCart.getCurrency()/>"/>
    <input type="hidden" id="totalCreditFormatted" value="<@ofbizCurrency amount=creditAmount isoCode=shoppingCart.getCurrency()/>"/>
    <tr>
      <td>${uiLabelMap.WebPosTransactionId}</td>
      <td><b>${transactionId?default("NA")}</b></td>
      <td>${(paymentCash.get("description", locale))!}</td>
      <td align="right"><b><@ofbizCurrency amount=cashAmount isoCode=shoppingCart.getCurrency()/></b></td>
      <td>${uiLabelMap.WebPosTotalItemSubTotal}</td>
      <td align="right"><b><@ofbizCurrency amount=shoppingCart.getDisplaySubTotal() isoCode=shoppingCart.getCurrency()/></b></td>
    </tr>
    <tr>
      <td>${uiLabelMap.WebPosDrawer}</td>
      <td><b>${drawerNumber?default(0)}</b></td>
      <td>${(paymentCheck.get("description", locale))!}</td>
      <td align="right"><b><@ofbizCurrency amount=checkAmount isoCode=shoppingCart.getCurrency()/></b></td>
      <td>${uiLabelMap.WebPosTotalPromotions}</td>
      <td align="right"><b><@ofbizCurrency amount=shoppingCart.getOrderOtherAdjustmentTotal() isoCode=shoppingCart.getCurrency()/></b></td>
    </tr>
    <tr>
      <td>${uiLabelMap.WebPosTerminal}</td>
      <td><b><#if isOpen>${uiLabelMap.WebPosTerminalOpen}<#else>${uiLabelMap.WebPosTerminalClose}</#if></b></td>
      <td>${(paymentGift.get("description", locale))!}</td>
      <td align="right"><b><@ofbizCurrency amount=giftAmount isoCode=shoppingCart.getCurrency()/></b></td>
      <td>${uiLabelMap.WebPosTotalSalesTax}</td>
      <td align="right"><b><@ofbizCurrency amount=shoppingCart.getTotalSalesTax() isoCode=shoppingCart.getCurrency()/></b></td>
    </tr>
    <tr>
      <td></td>
      <td></td>
      <td>${(paymentCredit.get("description", locale))!}</td>
      <td align="right"><b><@ofbizCurrency amount=creditAmount isoCode=shoppingCart.getCurrency()/></b></td>
      <td>${uiLabelMap.WebPosTotalShipping}</td>
      <td align="right"><b><@ofbizCurrency amount=shoppingCart.getTotalShipping() isoCode=shoppingCart.getCurrency()/></b></td>
    </tr>
    <tr>
      <td>${uiLabelMap.WebPosTransactionTotalDue}</td>
      <td align="right"><b><@ofbizCurrency amount=totalDue isoCode=shoppingCart.getCurrency()/></b></td>
      <td>${uiLabelMap.WebPosTransactionTotalPay}</td>
      <td align="right"><b><@ofbizCurrency amount=totalPay isoCode=shoppingCart.getCurrency()/></b></td>
      <td>${uiLabelMap.WebPosTotal}</td>
      <td align="right"><b><@ofbizCurrency amount=shoppingCart.getDisplayGrandTotal() isoCode=shoppingCart.getCurrency()/></b></td>
    </tr>
  </table>
</div>
</#if>
<div id="Cart">
  <table class="basic-table" cellspacing="0">
    <thead class="CartHead">
      <tr class="header-row">
        <td nowrap><b>${uiLabelMap.OrderProduct}</b></td>
        <td nowrap align="center"><b>${uiLabelMap.CommonQuantity}</b></td>
        <td nowrap align="right"><b>${uiLabelMap.WebPosUnitPrice}</b></td>
        <td nowrap align="right"><b>${uiLabelMap.WebPosAdjustments}</b></td>
        <td nowrap align="right"><b>${uiLabelMap.WebPosItemTotal}</b></td>
        <td nowrap align="center"><b>${uiLabelMap.CommonRemove}</b></td>
      </tr>
    </thead>
    <#if (shoppingCartSize > 0)>
    <tbody class="CartBody">
      <#-- set initial row color -->
      <#assign alt_row = false>
      <#list shoppingCart.items() as cartLine>
        <#assign cartLineIndex = shoppingCart.getItemIndex(cartLine)>
        <tr id="cartLine${cartLineIndex}" <#if alt_row>class="pos-cart-even"<#else>class="pos-cart-odd"</#if>>
          <td>
            <div>
              <#if cartLine.getProductId()??>
                <#-- product item -->
                <#-- start code to display a small image of the product -->
                <#if cartLine.getParentProductId()??>
                  <#assign parentProductId = cartLine.getParentProductId()/>
                <#else>
                  <#assign parentProductId = cartLine.getProductId()/>
                </#if>
                <#assign smallImageUrl = Static["org.apache.ofbiz.product.product.ProductContentWrapper"].getProductContentAsText(cartLine.getProduct(), "SMALL_IMAGE_URL", locale, dispatcher, "url")!>
                <#if !smallImageUrl?string?has_content><#assign smallImageUrl = "/images/defaultImage.jpg"></#if>
                <#if smallImageUrl?string?has_content>
                  <img src="<@ofbizContentUrl>${requestAttributes.contentPathPrefix!}${smallImageUrl}</@ofbizContentUrl>" align="left" class="cssImgSmall" />
                </#if>
                <#-- end code to display a small image of the product -->
                ${cartLine.getProductId()} - ${cartLine.getName()!} : ${cartLine.getDescription()!}
              <#else>
                <#-- this is a non-product item -->
                <b>${cartLine.getItemTypeDescription()!}</b> : ${cartLine.getName()!}
              </#if>
            </div>
          </td>
          <td nowrap align="center">
            ${cartLine.getQuantity()?string.number}
          </td>
          <td nowrap align="right"><div><@ofbizCurrency amount=cartLine.getDisplayPrice() isoCode=shoppingCart.getCurrency()/></div></td>
          <td nowrap align="right"><div><@ofbizCurrency amount=cartLine.getOtherAdjustments() isoCode=shoppingCart.getCurrency()/></div></td>
          <td nowrap align="right"><div><@ofbizCurrency amount=cartLine.getDisplayItemSubTotal() isoCode=shoppingCart.getCurrency()/></div></td>
          <td nowrap align="center"><a href="javascript:deleteCartItem('${cartLineIndex}');"><img src="/images/mini-trash.png" /></a></td>
        </tr>
        <#-- toggle the row color -->
        <#assign alt_row = !alt_row>
      </#list>
    <tbody>
    <tfoot class="CartFoot">
      <tr>
        <td colspan="6"><b><hr/></b></td>
      </tr>
      <tr id="CartTotal">
        <td align="left"><b>${shoppingCartSize?default(0)}</b></td>
        <td align="center"><b>${totalQuantity?default(0)}</b></td>
        <td align="right">&nbsp;</td>
        <td align="right">&nbsp;</td>
        <td align="right"><b><@ofbizCurrency amount=shoppingCart.getDisplaySubTotal() isoCode=shoppingCart.getCurrency()/></b></td>
        <td align="right">&nbsp;</td>
      </tr>
    <tfoot>
    </#if>
  </table>
</div>
<script language="JavaScript" type="text/javascript">
  selectCartItem();
</script>
