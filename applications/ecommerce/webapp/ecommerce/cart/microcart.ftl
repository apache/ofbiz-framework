<#--
 *  Copyright (c) 2001-2005 The Open For Business Project - www.ofbiz.org
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
<#assign shoppingCart = sessionAttributes.shoppingCart?if_exists>
<#if shoppingCart?has_content>
    <#assign shoppingCartSize = shoppingCart.size()>
<#else>
    <#assign shoppingCartSize = 0>
</#if>
<div id="microcart">
    <div>
        <#if (shoppingCartSize > 0)>
            ${uiLabelMap.EcommerceCartHas} ${shoppingCart.getTotalQuantity()}
            <#if shoppingCart.getTotalQuantity() == 1>${uiLabelMap.EcommerceItem}<#else/>${uiLabelMap.EcommerceItems}</#if>,
            <@ofbizCurrency amount=shoppingCart.getGrandTotal() isoCode=shoppingCart.getCurrency()/>
        <#else>
            ${uiLabelMap.EcommerceShoppingCartEmpty}
        </#if>
        &nbsp;&nbsp;
    </div>
    <div>
      <a href="<@ofbizUrl>view/showcart</@ofbizUrl>">[${uiLabelMap.EcommerceViewCart}]</a>
      <#if (shoppingCartSize > 0)>
          <a href="<@ofbizUrl>quickcheckout</@ofbizUrl>">[${uiLabelMap.EcommerceCheckoutQuick}]</a>
      <#else>
          <span class="disabled">[${uiLabelMap.EcommerceCheckoutQuick}]</span>
      </#if>
      &nbsp;&nbsp;
    </div>
</div>
