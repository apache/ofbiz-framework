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

<#assign shoppingCart = sessionAttributes.shoppingCart?if_exists>

<table border="0" width='100%' cellspacing='0' cellpadding='0' class='boxoutside'>
  <tr>
    <td width='100%'>
      <table width='100%' border='0' cellspacing='0' cellpadding='0' class='boxtop'>
        <tr>
          <td valign="middle" align="center">
            <div class='boxhead'><b>${uiLabelMap.OrderOrderShortcuts}</b></div>
          </td>
        </tr>
      </table>
    </td>
  </tr>
  <tr>
    <td width='100%'>
      <table width='100%' border='0' cellspacing='0' cellpadding='0' class='boxbottom'>
        <tr>
          <td>
            <table width="100%" border="0" cellpadding="0" cellspacing="0">
                <#if shoppingCart.getOrderType() == "PURCHASE_ORDER">
                  <tr>
                    <td>
                      <a href="<@ofbizUrl>RequirementsForSupplier</@ofbizUrl>" class="buttontext">${uiLabelMap.OrderRequirements}</a>
                    </td>
                  </tr>
                </#if>
                <#if shoppingCart.getOrderType() == "SALES_ORDER">
                  <tr>
                    <td>
                      <a href="<@ofbizUrl>FindQuoteForCart</@ofbizUrl>" class="buttontext">${uiLabelMap.OrderOrderQuotes}</a>
                    </td>
                  </tr>
                  <tr>
                    <td>
                      <a href="<@ofbizUrl>createQuoteFromCart?destroyCart=Y</@ofbizUrl>" class="buttontext">${uiLabelMap.OrderCreateQuoteFromCart}</a>
                    </td>
                  </tr>
                  <tr>
                    <td>
                      <a href="<@ofbizUrl>createCustRequestFromCart?destroyCart=Y</@ofbizUrl>" class="buttontext">${uiLabelMap.OrderCreateCustRequestFromCart}</a>
                    </td>
                  </tr>
                </#if>
                <tr>
                  <td>
                    <a href="/partymgr/control/findparty?${externalKeyParam?if_exists}" class="buttontext">${uiLabelMap.PartyFindParty}</a>
                  </td>
                </tr>
                <#if shoppingCart.getOrderType() == "SALES_ORDER" && shoppingCart.items()?has_content>
                  <tr>
                    <td>
                      <a href="<@ofbizUrl>setCustomer</@ofbizUrl>" class="buttontext">${uiLabelMap.PartyCreateNewCustomer}</a>
                    </td>
                  </tr>
                </#if>
                <tr>
                  <td>
                    <a href="<@ofbizUrl>checkinits</@ofbizUrl>" class="buttontext">${uiLabelMap.PartyChangeParty}</a>
                  </td>
                </tr>
                <#if security.hasEntityPermission("CATALOG", "_CREATE", session)>
                  <tr>
                    <td>
                      <a href="/catalog/control/EditProduct?${externalKeyParam?if_exists}" target="catalog" class="buttontext">${uiLabelMap.ProductCreateNewProduct}</a>
                    </td>
                  </tr>
                </#if>
                <tr>
                  <td>
                    <a href="<@ofbizUrl>quickadd</@ofbizUrl>" class="buttontext">${uiLabelMap.EcommerceQuickAdd}</a>
                  </td>
                </tr>
                <#if shoppingLists?exists>
                  <tr>
                    <td>
                      <a href="<@ofbizUrl>viewPartyShoppingLists?partyId=${partyId}</@ofbizUrl>" class="buttontext">${uiLabelMap.PageTitleShoppingList}</a>
                    </td>
                  </tr>
                </#if>
            </table>
          </td>
        </tr>
      </table>
    </td>
  </tr>
</table>
<br/>
