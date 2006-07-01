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
 *@author     Andy Zeneski (jaz@ofbiz.org)
 *@author     Jean-Luc.Malet@nereide.biz (migration to uiLabelMap)
 *@version    $Rev$
 *@since      2.2
-->

<#assign shoppingCart = sessionAttributes.shoppingCart?if_exists>
<#if shoppingCart?has_content>
    <#assign shoppingCartSize = shoppingCart.size()>
<#else>
    <#assign shoppingCartSize = 0>
</#if>

<div class="screenlet">
    <div class="screenlet-header">
        <div class='boxhead'><b>${uiLabelMap.EcommerceCartSummary}</b></div>
    </div>
    <div class="screenlet-body">
        <#if (shoppingCartSize > 0)>
          <#if !hidetoplinks?exists>
            <div><a href="<@ofbizUrl>view/showcart</@ofbizUrl>" class="buttontext">${uiLabelMap.EcommerceViewCart}</a>&nbsp;<a href="<@ofbizUrl>checkoutoptions</@ofbizUrl>" class="buttontext">${uiLabelMap.EcommerceCheckout}</a></div>
          </#if>
          <table width="100%" cellpadding="0" cellspacing="2">
            <tr>
              <td valign="bottom"><div class="tabletext"><b>${uiLabelMap.EcommerceNbr}</b></div></td>
              <td valign="bottom"><div class="tabletext"><b>${uiLabelMap.EcommerceItem}</b></div></td>
              <td valign="bottom" align="right"><div class="tabletext"><b>${uiLabelMap.CommonSubtotal}</b></div></td>
            </tr>
            <#list shoppingCart.items() as cartLine>
              <tr>
                <td valign="top"><div class="tabletext">${cartLine.getQuantity()?string.number}</div></td>
                <td valign="top">
                  <#if cartLine.getProductId()?exists>
                    <div><a href="<@ofbizUrl>product?product_id=${cartLine.getProductId()}</@ofbizUrl>" class="linktext">${cartLine.getName()}</a></div>
                  <#else>
                    <div class="tabletext"><b>${cartLine.getItemTypeDescription()?if_exists}</b></div>
                  </#if>
                </td>
                <td align="right" valign="top"><div class="tabletext"><@ofbizCurrency amount=cartLine.getDisplayItemSubTotal() isoCode=shoppingCart.getCurrency()/></div></td>
              </tr>
              <#if cartLine.getReservStart()?exists>
                <tr><td>&nbsp;</td><td colspan="2"><div class="tabletext">(${cartLine.getReservStart()?string("yyyy-MM-dd")}, ${cartLine.getReservLength()} ${uiLabelMap.CommonDays})</div></td></tr>
              </#if>
            </#list>
            <tr>
              <td colspan="3" align="right">
                <div class="tabletext"><b>${uiLabelMap.EcommerceTotal}: <@ofbizCurrency amount=shoppingCart.getGrandTotal() isoCode=shoppingCart.getCurrency()/></b></div>
              </td>
            </tr>
          </table>
          <#if !hidebottomlinks?exists>
            <div><a href="<@ofbizUrl>view/showcart</@ofbizUrl>" class="buttontext">${uiLabelMap.EcommerceViewCart}</a>&nbsp;<a href="<@ofbizUrl>checkoutoptions</@ofbizUrl>" class="buttontext">${uiLabelMap.EcommerceCheckout}</a></div>
            <div style="margin-top: 4px;"><a href="<@ofbizUrl>quickcheckout</@ofbizUrl>" class="buttontext">${uiLabelMap.EcommerceCheckoutQuick}</a></div>
          </#if>
        <#else>
          <div class="tabletext">${uiLabelMap.EcommerceShoppingCartEmpty}</div>
        </#if>
    </div>
</div>
