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

<div>
    <h1>${uiLabelMap.OrderPromotionInformation}:</h1>
    <h3>${uiLabelMap.OrderPromotionsApplied}:</h3>
        <ul>    
            <#list shoppingCart.getProductPromoUseInfoIter() as productPromoUseInfo>
                <li>
                    <#-- TODO: when promo pretty print is done show promo short description here -->
                       ${uiLabelMap.OrderPromotion} <a href="<@ofbizUrl>showPromotionDetails?productPromoId=${productPromoUseInfo.productPromoId!}</@ofbizUrl>" class="button">${uiLabelMap.CommonDetails}</a>
                    <#if productPromoUseInfo.productPromoCodeId?has_content> - ${uiLabelMap.OrderWithPromoCode} [${productPromoUseInfo.productPromoCodeId}]</#if>
                    <#if (productPromoUseInfo.totalDiscountAmount != 0)> - ${uiLabelMap.CommonTotalValue} <@ofbizCurrency amount=(-1*productPromoUseInfo.totalDiscountAmount) isoCode=shoppingCart.getCurrency()/></#if>
                    <#if productPromoUseInfo.productPromoCodeId?has_content>
                        <a href="<@ofbizUrl>removePromotion?promoCode=${productPromoUseInfo.productPromoCodeId!}</@ofbizUrl>" class="button">${uiLabelMap.OrderRemovePromotion}</a>
                    </#if>
                </li>
                <#if (productPromoUseInfo.quantityLeftInActions > 0)>
                    <li>- Could be used for ${productPromoUseInfo.quantityLeftInActions} more discounted item<#if (productPromoUseInfo.quantityLeftInActions > 1)>s</#if> if added to your cart.</li>
                </#if>
            </#list>
        </ul>
    <h3>${uiLabelMap.OrderCartItemUseinPromotions}:</h3>
    <ul>
        <#list shoppingCart.items() as cartLine>
            <#assign cartLineIndex = shoppingCart.getItemIndex(cartLine)>
            <#if cartLine.getIsPromo()>
                <li>${uiLabelMap.OrderItemN} ${cartLineIndex+1} [${cartLine.getProductId()!}] - ${uiLabelMap.OrderIsAPromotionalItem}</li>
            <#else>
                <li>${uiLabelMap.OrderItemN} ${cartLineIndex+1} [${cartLine.getProductId()!}] - ${cartLine.getPromoQuantityUsed()?string.number}/${cartLine.getQuantity()?string.number} ${uiLabelMap.CommonUsed} - ${cartLine.getPromoQuantityAvailable()?string.number} ${uiLabelMap.CommonAvailable}
                    <ul>
                        <#list cartLine.getQuantityUsedPerPromoActualIter() as quantityUsedPerPromoActualEntry>
                            <#assign productPromoActualPK = quantityUsedPerPromoActualEntry.getKey()>
                            <#assign actualQuantityUsed = quantityUsedPerPromoActualEntry.getValue()>
                            <#assign isQualifier = "ProductPromoCond" == productPromoActualPK.getEntityName()>
                            <li>&nbsp;&nbsp;-&nbsp;${actualQuantityUsed} ${uiLabelMap.CommonUsedAs} <#if isQualifier>${uiLabelMap.CommonQualifier}<#else>${uiLabelMap.CommonBenefit}</#if> ${uiLabelMap.OrderOfPromotion} <a href="<@ofbizUrl>showPromotionDetails?productPromoId=${productPromoActualPK.productPromoId}</@ofbizUrl>" class="button">${uiLabelMap.CommonDetails}</a></li>
                            <!-- productPromoActualPK ${productPromoActualPK.toString()} -->
                        </#list>
                    </ul>
                    <ul>
                        <#list cartLine.getQuantityUsedPerPromoFailedIter() as quantityUsedPerPromoFailedEntry>
                            <#assign productPromoFailedPK = quantityUsedPerPromoFailedEntry.getKey()>
                            <#assign failedQuantityUsed = quantityUsedPerPromoFailedEntry.getValue()>
                            <#assign isQualifier = "ProductPromoCond" == productPromoFailedPK.getEntityName()>
                            <li>&nbsp;&nbsp;-&nbsp;${uiLabelMap.CommonCouldBeUsedAs} <#if isQualifier>${uiLabelMap.CommonQualifier}<#else>${uiLabelMap.CommonBenefit}</#if> ${uiLabelMap.OrderOfPromotion} <a href="<@ofbizUrl>showPromotionDetails?productPromoId=${productPromoFailedPK.productPromoId}</@ofbizUrl>" class="button">${uiLabelMap.CommonDetails}</a></li>
                            <!-- Total times checked but failed: ${failedQuantityUsed}, productPromoFailedPK ${productPromoFailedPK.toString()} -->
                        </#list>
                    </ul>
                    <#list cartLine.getQuantityUsedPerPromoCandidateIter() as quantityUsedPerPromoCandidateEntry>
                        <#assign productPromoCandidatePK = quantityUsedPerPromoCandidateEntry.getKey()>
                        <#assign candidateQuantityUsed = quantityUsedPerPromoCandidateEntry.getValue()>
                        <#assign isQualifier = "ProductPromoCond" == productPromoCandidatePK.getEntityName()>
                        <!-- Left over not reset or confirmed, shouldn't happen: ${candidateQuantityUsed} Might be Used (Candidate) as <#if isQualifier>${uiLabelMap.CommonQualifier}<#else>${uiLabelMap.CommonBenefit}</#if> ${uiLabelMap.OrderOfPromotion} [${productPromoCandidatePK.productPromoId}] -->
                        <!-- productPromoCandidatePK ${productPromoCandidatePK.toString()} -->
                    </#list>
                </li>
            </#if>
        </#list>
    </ul>
</div>

