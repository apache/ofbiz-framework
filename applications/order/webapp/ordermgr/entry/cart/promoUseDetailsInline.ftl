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

<div class="screenlet">
    <div class="screenlet-header">
        <div class="boxhead">&nbsp;${uiLabelMap.EcommercePromotionInformation}:</div>
    </div>
    <div class="screenlet-body">
        <div style="float: left; width: 40%;">
            <div class="tableheadtext">${uiLabelMap.EcommercePromotionsApplied}:</div>
            <#list shoppingCart.getProductPromoUseInfoIter() as productPromoUseInfo>
                <div class="tabletext">
                    <#-- TODO: when promo pretty print is done show promo short description here -->
                       ${uiLabelMap.EcommercePromotion} <a href="<@ofbizUrl>showPromotionDetails?productPromoId=${productPromoUseInfo.productPromoId?if_exists}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonDetails}</a>
                    <#if productPromoUseInfo.productPromoCodeId?has_content> - ${uiLabelMap.OrderWithPromoCode} [${productPromoUseInfo.productPromoCodeId}]</#if>
                    <#if (productPromoUseInfo.totalDiscountAmount != 0)> - ${uiLabelMap.CommonTotalValue} <@ofbizCurrency amount=(-1*productPromoUseInfo.totalDiscountAmount) isoCode=shoppingCart.getCurrency()/></#if>
                </div>
                <#if (productPromoUseInfo.quantityLeftInActions > 0)>
                    <div class="tabletext">- Could be used for ${productPromoUseInfo.quantityLeftInActions} more discounted item<#if (productPromoUseInfo.quantityLeftInActions > 1)>s</#if> if added to your cart.</div>
                </#if>
            </#list>
        </div>
        <div style="float: right; width: 55%; padding-left: 10px; border-left: 1px solid #999999;">
            <div class="tableheadtext">${uiLabelMap.EcommerceCartItemUseinPromotions}:</div>
            <#list shoppingCart.items() as cartLine>
                <#assign cartLineIndex = shoppingCart.getItemIndex(cartLine)>
                <#if cartLine.getIsPromo()>
                    <div class="tabletext">${uiLabelMap.EcommerceItemN} ${cartLineIndex+1} [${cartLine.getProductId()?if_exists}] - ${uiLabelMap.EcommerceIsAPromotionalItem}</div>
                <#else>
                    <div class="tabletext">${uiLabelMap.EcommerceItemN} ${cartLineIndex+1} [${cartLine.getProductId()?if_exists}] - ${cartLine.getPromoQuantityUsed()?string.number}/${cartLine.getQuantity()?string.number} ${uiLabelMap.CommonUsed} - ${cartLine.getPromoQuantityAvailable()?string.number} ${uiLabelMap.CommonAvailable}</div>
                    <#list cartLine.getQuantityUsedPerPromoActualIter() as quantityUsedPerPromoActualEntry>
                        <#assign productPromoActualPK = quantityUsedPerPromoActualEntry.getKey()>
                        <#assign actualQuantityUsed = quantityUsedPerPromoActualEntry.getValue()>
                        <#assign isQualifier = "ProductPromoCond" == productPromoActualPK.getEntityName()>
                        <div class="tabletext">&nbsp;&nbsp;-&nbsp;${actualQuantityUsed} ${uiLabelMap.CommonUsedAs} <#if isQualifier>${uiLabelMap.CommonQualifier}<#else>${uiLabelMap.CommonBenefit}</#if> ${uiLabelMap.EcommerceOfPromotion} <a href="<@ofbizUrl>showPromotionDetails?productPromoId=${productPromoActualPK.productPromoId}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonDetails}</a></div>
                        <!-- productPromoActualPK ${productPromoActualPK.toString()} -->
                    </#list>
                    <#list cartLine.getQuantityUsedPerPromoFailedIter() as quantityUsedPerPromoFailedEntry>
                        <#assign productPromoFailedPK = quantityUsedPerPromoFailedEntry.getKey()>
                        <#assign failedQuantityUsed = quantityUsedPerPromoFailedEntry.getValue()>
                        <#assign isQualifier = "ProductPromoCond" == productPromoFailedPK.getEntityName()>
                        <div class="tabletext">&nbsp;&nbsp;-&nbsp;${uiLabelMap.CommonCouldBeUsedAs} <#if isQualifier>${uiLabelMap.CommonQualifier}<#else>${uiLabelMap.CommonBenefit}</#if> ${uiLabelMap.EcommerceOfPromotion} <a href="<@ofbizUrl>showPromotionDetails?productPromoId=${productPromoFailedPK.productPromoId}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonDetails}</a></div>
                        <!-- Total times checked but failed: ${failedQuantityUsed}, productPromoFailedPK ${productPromoFailedPK.toString()} -->
                    </#list>
                    <#list cartLine.getQuantityUsedPerPromoCandidateIter() as quantityUsedPerPromoCandidateEntry>
                        <#assign productPromoCandidatePK = quantityUsedPerPromoCandidateEntry.getKey()>
                        <#assign candidateQuantityUsed = quantityUsedPerPromoCandidateEntry.getValue()>
                        <#assign isQualifier = "ProductPromoCond" == productPromoCandidatePK.getEntityName()>
                        <!-- Left over not reset or confirmed, shouldn't happen: ${candidateQuantityUsed} Might be Used (Candidate) as <#if isQualifier>${uiLabelMap.CommonQualifier}<#else>${uiLabelMap.CommonBenefit}</#if> ${uiLabelMap.EcommerceOfPromotion} [${productPromoCandidatePK.productPromoId}] -->
                        <!-- productPromoCandidatePK ${productPromoCandidatePK.toString()} -->
                    </#list>
                </#if>
            </#list>
        </div>
        <div class="endcolumns"><span> </span></div>
    </div>
</div>
                