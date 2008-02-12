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

  <#if savedCartItems?has_content>
    <div id="partyShoppingCart" class="screenlet">
      <div class="screenlet-title-bar">
        <ul>
          <li class="h3">${uiLabelMap.PartyCurrentShoppingCart}</li>
          <#if security.hasEntityPermission("PARTYMGR", "_UPDATE", session)>
            <#if savedCartListId?has_content>
              <#assign listParam = "&shoppingListId=" + savedCartListId>
            <#else>
              <#assign listParam = "">
            </#if>
            <li><a href="<@ofbizUrl>editShoppingList?partyId=${partyId}${listParam}</@ofbizUrl>">${uiLabelMap.CommonEdit}</a></li>
          </#if>
        </ul>
      <br class="clear" />
      </div>
      <div class="screenlet-body">
        <#if savedCartItems?has_content>
          <table class="basic-table" cellspacing="0">
            <tr class="header-row">
              <td>${uiLabelMap.PartySequenceId}</td>
              <td>${uiLabelMap.PartyProductId}</td>
              <td>${uiLabelMap.PartyQuantity}</td>
              <td>${uiLabelMap.PartyQuantityPurchased}</td>
            </tr>
            <#list savedCartItems as savedCartItem>
              <tr>
                <td>${savedCartItem.shoppingListItemSeqId?if_exists}</td>
                <td class="button-col"><a href="/catalog/control/EditProduct?productId=${savedCartItem.productId}&externalLoginKey=${requestAttributes.externalLoginKey}">${savedCartItem.productId?if_exists}</a></td>
                <td>${savedCartItem.quantity?if_exists}</td>
                <td>${savedCartItem.quantityPurchased?if_exists}</td>
              </tr>
            </#list>
          </table>
        <#else>
          ${uiLabelMap.PartyNoShoppingCartSavedForParty}
        </#if>
      </div>
    </div>
  </#if>
