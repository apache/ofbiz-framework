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

<#assign shoppingCart = sessionAttributes.shoppingCart!>

<div class="screenlet">
    <div class="screenlet-title-bar">
        <div class="h3">${uiLabelMap.OrderOrderShortcuts}</div>
    </div>
    <div class="screenlet-body">
        <ul>
            <#if shoppingCart.getOrderType() == "PURCHASE_ORDER">
              <li><a href="<@ofbizUrl>RequirementsForSupplier</@ofbizUrl>" class="buttontext">${uiLabelMap.OrderRequirements}</a></li>
            </#if>
            <#if shoppingCart.getOrderType()?has_content && shoppingCart.items()?has_content>
              <li><a href="<@ofbizUrl>createQuoteFromCart?destroyCart=Y</@ofbizUrl>" class="buttontext">${uiLabelMap.OrderCreateQuoteFromCart}</a></li>
              <li><a href="<@ofbizUrl>FindQuoteForCart</@ofbizUrl>" class="buttontext">${uiLabelMap.OrderOrderQuotes}</a></li>
            </#if>
            <#if shoppingCart.getOrderType() == "SALES_ORDER">
              <li><a href="<@ofbizUrl>createCustRequestFromCart?destroyCart=Y</@ofbizUrl>" class="buttontext">${uiLabelMap.OrderCreateCustRequestFromCart}</a></li>
            </#if>
            <li><a href="/partymgr/control/findparty?${externalKeyParam!}" class="buttontext">${uiLabelMap.PartyFindParty}</a></li>
            <#if shoppingCart.getOrderType() == "SALES_ORDER">
              <li><a href="<@ofbizUrl>setCustomer</@ofbizUrl>" class="buttontext">${uiLabelMap.PartyCreateNewCustomer}</a></li>
            </#if>
            <li><a href="<@ofbizUrl>checkinits</@ofbizUrl>" class="buttontext">${uiLabelMap.PartyChangeParty}</a></li>
            <#if security.hasEntityPermission("CATALOG", "_CREATE", session)>
               <li><a href="/catalog/control/EditProduct?${externalKeyParam!}" target="catalog" class="buttontext">${uiLabelMap.ProductCreateNewProduct}</a></li>
            </#if>
            <li><a href="<@ofbizUrl>quickadd</@ofbizUrl>" class="buttontext">${uiLabelMap.OrderQuickAdd}</a></li>
            <#if shoppingLists??>
              <li><a href="<@ofbizUrl>viewPartyShoppingLists?partyId=${partyId}</@ofbizUrl>" class="buttontext">${uiLabelMap.PageTitleShoppingList}</a></li>
            </#if>
        </ul>
    </div>
</div>
<br />
