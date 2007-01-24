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

<br/>
<#if productPromoCode?exists>
    <#if productPromoCode.requireEmailOrParty?if_exists == "N">
        <div class="tableheadtext">${uiLabelMap.ProductNoteRequireEmailParty}</div>
    </#if>
    <div class="head3">${uiLabelMap.ProductPromoCodeEmails}</div>
    <#list productPromoCodeEmails as productPromoCodeEmail>
        <div class="tabletext"><a href="<@ofbizUrl>deleteProductPromoCodeEmail?productPromoCodeId=${productPromoCodeEmail.productPromoCodeId}&emailAddress=${productPromoCodeEmail.emailAddress}</@ofbizUrl>" class="buttontext">[X]</a>&nbsp;${productPromoCodeEmail.emailAddress}</div>
    </#list>
    <div class="tabletext">
        <form method="post" action="<@ofbizUrl>createProductPromoCodeEmail</@ofbizUrl>" style="margin: 0;">
            <input type="hidden" name="productPromoCodeId" value="${productPromoCodeId?if_exists}"/>
            ${uiLabelMap.ProductAddEmail} : <input type="text" size="40" name="emailAddress" class="inputBox">
            <input type="submit" value="${uiLabelMap.CommonAdd}">
        </form>
    </div>

    <div class="head3">${uiLabelMap.ProductPromoCodeParties}</div>
    <#list productPromoCodeParties as productPromoCodeParty>
        <div class="tabletext"><a href="<@ofbizUrl>deleteProductPromoCodeParty?productPromoCodeId=${productPromoCodeParty.productPromoCodeId}&partyId=${productPromoCodeParty.partyId}</@ofbizUrl>" class="buttontext">[X]</a>&nbsp;${productPromoCodeParty.partyId}</div>
    </#list>
    <div class="tabletext">
        <form method="post" action="<@ofbizUrl>createProductPromoCodeParty</@ofbizUrl>" style="margin: 0;">
            <input type="hidden" name="productPromoCodeId" value="${productPromoCodeId?if_exists}"/>
            ${uiLabelMap.ProductAddPartyId} : <input type="text" size="10" name="partyId" class="inputBox">
            <input type="submit" value="${uiLabelMap.CommonAdd}">
        </form>
    </div>
</#if>
