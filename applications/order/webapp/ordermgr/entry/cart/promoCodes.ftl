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
 *@author     David E. Jones (jonesde@ofbiz.org)
 *@author     Andy Zeneski (jaz@ofbiz.org)
 *@author     Jean-Luc.Malet@nereide.biz (migration to uiLabelMap)
 *@author     Jacopo Cappellato (tiz@sastau.it)
 *@version    $Rev$
 *@since      2.2
-->

<#if shoppingCart.getOrderType() == "SALES_ORDER">
<div class="screenlet">
    <div class="screenlet-header">
        <div class="boxhead">&nbsp;${uiLabelMap.OrderPromotionCouponCodes}</div>
    </div>
    <div class="screenlet-body">
      <div class="tabletext">
        <form method="post" action="<@ofbizUrl>addpromocode<#if requestAttributes._CURRENT_VIEW_?has_content>/${requestAttributes._CURRENT_VIEW_}</#if></@ofbizUrl>" name="addpromocodeform" style="margin: 0;">
          <input type="text" class="inputBox" size="15" name="productPromoCodeId" value="">
          <input type="submit" class="smallSubmit" value="${uiLabelMap.OrderAddCode}">
          <#assign productPromoCodeIds = (shoppingCart.getProductPromoCodesEntered())?if_exists>
          <#if productPromoCodeIds?has_content>
            ${uiLabelMap.OrderEnteredPromoCodes}:
            <#list productPromoCodeIds as productPromoCodeId>
              ${productPromoCodeId}
            </#list>
          </#if>
        </form>
      </div>
    </div>
</div>
</#if>
