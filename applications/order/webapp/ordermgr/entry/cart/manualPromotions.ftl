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
 -->

<#if allProductPromos?has_content>
<div class="screenlet">
    <div class="screenlet-header">
        <div class="boxhead">&nbsp;${uiLabelMap.OrderManualPromotions}</div>
    </div>
    <div class="screenlet-body">
      <div class="tabletext">
        <form method="post" action="<@ofbizUrl>doManualPromotions</@ofbizUrl>" name="domanualpromotions" style="margin: 0;">
          <!-- to enter more than two manual promotions, just add a new select box with name="productPromoId_n" -->
          <select name="productPromoId_1" class="selectBox">
            <option value=""></option>
            <#list allProductPromos as productPromo>
              <option value="${productPromo.productPromoId}">${productPromo.promoName?if_exists}</option>
            </#list>
          </select>
          <select name="productPromoId_2" class="selectBox">
            <option value=""></option>
            <#list allProductPromos as productPromo>
              <option value="${productPromo.productPromoId}">${productPromo.promoName?if_exists}</option>
            </#list>
          </select>
          <input type="submit" class="smallSubmit" value="${uiLabelMap.OrderDoPromotions}">
        </form>
      </div>
    </div>
</div>
</#if>