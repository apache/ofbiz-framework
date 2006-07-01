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
 *@author     Brad Steiner (bsteiner@thehungersite.com)
 *@author     Catherine.Heintz@nereide.biz (migration to UiLabel)
 *@version    $Rev$
 *@since      2.2
-->
<div class="head1">${uiLabelMap.ProductProductPromotionsList}</div>
<div class="tabletext">
    <a href="<@ofbizUrl>EditProductPromo</@ofbizUrl>" class="buttontext">[${uiLabelMap.ProductCreateNewProductPromo}]</a>
    <#if manualOnly?if_exists == "Y">
        <a href="<@ofbizUrl>FindProductPromo?manualOnly=N</@ofbizUrl>" class="buttontext">[${uiLabelMap.ProductPromotionManualImported}]</a>
    <#else>
        <a href="<@ofbizUrl>FindProductPromo?manualOnly=Y</@ofbizUrl>" class="buttontext">[${uiLabelMap.ProductPromotionManual}]</a>
    </#if>
</div>
<div class="tabletext">
    <form method="post" action="<@ofbizUrl>EditProductPromoCode</@ofbizUrl>" style="margin: 0;">
        ${uiLabelMap.ProductPromotionCode}: <input type="text" size="10" name="productPromoCodeId" class="inputBox">
        <input type="submit" value="${uiLabelMap.CommonEdit}">
    </form>
</div>
<br/>
<table border="1" cellpadding="2" cellspacing="0">
    <tr>
        <td><div class="tabletext"><b>${uiLabelMap.ProductPromoNameId}</b></div></td>
        <td><div class="tabletext"><b>${uiLabelMap.ProductPromoText}</b></div></td>
        <td><div class="tabletext"><b>${uiLabelMap.ProductPromotionReqCode}?</b></div></td>
        <td><div class="tabletext"><b>${uiLabelMap.CommonCreated}</b></div></td>
        <td><div class="tabletext">&nbsp;</div></td>
    </tr>
    <#list productPromos as productPromo>
        <tr valign="middle">
            <td><div class='tabletext'>&nbsp;<a href="<@ofbizUrl>EditProductPromo?productPromoId=${(productPromo.productPromoId)?if_exists}</@ofbizUrl>" class="buttontext">${(productPromo.promoName)?if_exists} [${(productPromo.productPromoId)?if_exists}]</a></div></td>
            <td><div class='tabletext'>&nbsp;${(productPromo.promoText)?if_exists}</div></td>
            <td><div class='tabletext'>&nbsp;${(productPromo.requireCode)?default("N")}</div></td>
            <td><div class='tabletext'>&nbsp;${(productPromo.createdDate)?if_exists}</div></td>
            <td>
                <a href='<@ofbizUrl>EditProductPromo?productPromoId=${(productPromo.productPromoId)?if_exists}</@ofbizUrl>' class="buttontext">[${uiLabelMap.CommonEdit}]</a>
            </td>
        </tr>
    </#list>
</table>
