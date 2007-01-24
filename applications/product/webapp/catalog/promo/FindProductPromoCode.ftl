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
    
