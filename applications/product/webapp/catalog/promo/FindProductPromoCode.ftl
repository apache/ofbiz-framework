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
 *@version    $Rev$
 *@since      3.0
-->
<#if productPromoId?exists>
    <div>
        <#if manualOnly?if_exists == "Y">
            <a href="<@ofbizUrl>FindProductPromoCode?manualOnly=N&productPromoId=${productPromoId?if_exists}</@ofbizUrl>" class="buttontext">[${uiLabelMap.ProductPromotionManualImported}]</a>
        <#else>
            <a href="<@ofbizUrl>FindProductPromoCode?manualOnly=Y&productPromoId=${productPromoId?if_exists}</@ofbizUrl>" class="buttontext">[${uiLabelMap.ProductPromotionManual}</a>
        </#if>
    </div>
    <br/>
    <table border="1" cellpadding="2" cellspacing="0">
        <tr>
            <td><div class="tabletext"><b>${uiLabelMap.ProductPromotionCode}</b></div></td>
            <td><div class="tabletext"><b>${uiLabelMap.ProductPromotionPerCode}</b></div></td>
            <td><div class="tabletext"><b>${uiLabelMap.ProductPromotionPerCustomer}</b></div></td>
            <td><div class="tabletext"><b>${uiLabelMap.ProductPromotionReqEmailOrParty}</b></div></td>
            <td><div class="tabletext"><b>${uiLabelMap.CommonCreated}</b></div></td>
            <td><div class="tabletext">&nbsp;</div></td>
        </tr>
        <#list productPromoCodes as productPromoCode>
            <#assign productPromo = productPromoCode.getRelatedOne("ProductPromo")>
            <tr valign="middle">
                <td><div class='tabletext'>&nbsp;<a href="<@ofbizUrl>EditProductPromoCode?productPromoCodeId=${(productPromoCode.productPromoCodeId)?if_exists}</@ofbizUrl>" class="buttontext">[${(productPromoCode.productPromoCodeId)?if_exists}]</a></div></td>
                <td><div class='tabletext'>&nbsp;${(productPromoCode.useLimitPerCode)?if_exists}</div></td>
                <td><div class='tabletext'>&nbsp;${(productPromoCode.useLimitPerCustomer)?if_exists}</div></td>
                <td><div class='tabletext'>&nbsp;${(productPromoCode.requireEmailOrParty)?if_exists}</div></td>
                <td><div class='tabletext'>&nbsp;${(productPromoCode.createdDate)?if_exists}</div></td>
                <td>
                    <a href='<@ofbizUrl>EditProductPromoCode?productPromoCodeId=${(productPromoCode.productPromoCodeId)?if_exists}</@ofbizUrl>' class="buttontext">[${uiLabelMap.CommonEdit}]</a>
                    <a href='<@ofbizUrl>deleteProductPromoCode?productPromoCodeId=${(productPromoCode.productPromoCodeId)?if_exists}&productPromoId=${productPromoId?if_exists}</@ofbizUrl>' class="buttontext">[${uiLabelMap.CommonDelete}]</a>
                </td>
            </tr>
        </#list>
    </table>
    <br/>
    <div class="head3">${uiLabelMap.ProductPromotionAddSetOfPromotionCodes}:</div>
    <div class="tabletext">
        <form method="post" action="<@ofbizUrl>createProductPromoCodeSet</@ofbizUrl>" style="margin: 0;">
            <input type="hidden" name="userEntered" value="N"/>
            <input type="hidden" name="requireEmailOrParty" value="N"/>
            <input type="hidden" name="productPromoId" value="${productPromoId}"/>
            ${uiLabelMap.CommonQuantity}: <input type="text" size="5" name="quantity" class="inputBox">
            ${uiLabelMap.ProductPromotionUseLimits}:
            ${uiLabelMap.ProductPromotionPerCode}<input type="text" size="5" name="useLimitPerCode" class="inputBox">
            ${uiLabelMap.ProductPromotionPerCustomer}<input type="text" size="5" name="useLimitPerCustomer" class="inputBox">
            <input type="submit" value="${uiLabelMap.CommonAdd}">
        </form>
    </div>
</#if>
    
