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

<div id="partyProductStores" class="screenlet">
  <div class="screenlet-title-bar">
    <ul>
      <li class="h3">${uiLabelMap.ProductStores}</li>
    </ul>
    <br class="clear" />
  </div>
  <div class="screenlet-body">
    <#if productStoreRoles?has_content>
      <table class="basic-table" cellspacing="0">
        <tr class="header-row">
          <td>${uiLabelMap.ProductStoreNameId}</td>
          <td>${uiLabelMap.PartyRoleType}</td>
        </tr>
        <#list productStoreRoles as productStoreRole>
          <#assign productStore = delegator.findOne("ProductStore", {"productStoreId" : productStoreRole.productStoreId}, true) />
          <#assign roleType = delegator.findOne("RoleType", {"roleTypeId" : productStoreRole.roleTypeId}, true) />
          <tr>
            <td class="button-col">
              <a href="/catalog/control/FindProductStoreRoles?partyId=${productStoreRole.partyId}&amp;productStoreId=${productStore.productStoreId}">${productStore.storeName?default("${uiLabelMap.ProductNoDescription}")} (${productStore.productStoreId})</a>
            </td>
            <td>${roleType.description!}</td>
          </tr>
        </#list>
      </table>
    <#else>
      ${uiLabelMap.PartyNoProductStoreFoundForThisParty}
    </#if>
  </div>
</div>