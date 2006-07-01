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
 *@author     Jacopo Cappellato (tiz@sastau.it)
 *@version    $Rev$
 *@since      2.2
-->
<#if showPromoText?exists && showPromoText>
<div class="screenlet">
    <div class="screenlet-header">
        <div class="boxhead">&nbsp;${uiLabelMap.EcommerceSpecialOffers}</div>
    </div>
    <div class="screenlet-body">
        <table cellspacing="0" cellpadding="1" border="0">
          <#-- show promotions text -->
          <#list productPromos as productPromo>
            <tr>
              <td>
                <div class="tabletext"><a href="<@ofbizUrl>showPromotionDetails?productPromoId=${productPromo.productPromoId}</@ofbizUrl>" class="linktext">${uiLabelMap.CommonDetails}</a> ${productPromo.promoText?if_exists}</div>
              </td>
            </tr>
            <#if productPromo_has_next>
              <tr><td><hr class="sepbar"/></td></tr>
            </#if>
          </#list>
          <tr><td><hr class="sepbar"/></td></tr>
          <tr>
            <td>
              <div class="tabletext"><a href="<@ofbizUrl>showAllPromotions</@ofbizUrl>" class="buttontext">${uiLabelMap.EcommerceViewAllPromotions}</a></div>
            </td>
          </tr>
        </table>
    </div>
</div>
</#if>
