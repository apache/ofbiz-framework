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

<h1>${uiLabelMap.AccountingBillingAccounts}</h1>
<div class="button-bar"><a href="<@ofbizUrl>EditBillingAccount<#if (requestParameters.partyId)?has_content>?partyId=${requestParameters.partyId}&roleTypeId=BILL_TO_CUSTOMER</#if></@ofbizUrl>" class="smallSubmit">${uiLabelMap.AccountingNewAccount}</a></div>

<br/>
<table class="basic-table" cellspacing="0"> 
  <tr class="header-row">
    <td>${uiLabelMap.AccountingAccountId}</td>
    <td>${uiLabelMap.AccountingAccountLimit}</td>
    <#if billingAccountRolesByParty?has_content>
      <#assign colSpan = "4">
      <td>${uiLabelMap.PartyRoleTypeId}</td>
    <#else>
      <#assign colSpan = "3">
    </#if>
    <td>&nbsp;</td>
  </tr>  
  <#if billingAccountRolesByParty?has_content>
    <#list billingAccountRolesByParty as role>
      <#assign billingAccount = role.getRelatedOne("BillingAccount")>
      <#assign roleType = role.getRelatedOne("RoleType")>
      <tr>
        <td>${billingAccount.billingAccountId}</td>
        <td><@ofbizCurrency amount=billingAccount.accountLimit isoCode=billingAccount.accountCurrencyUomId/></td>
        <td>${roleType.get("description",locale)}</td>
        <td class="button-col">
          <a href="<@ofbizUrl>EditBillingAccount?billingAccountId=${billingAccount.billingAccountId}</@ofbizUrl>">${uiLabelMap.CommonEdit}</a>
        </td>
      </tr>
    </#list>
  <#elseif billingAccounts?has_content>
    <#list billingAccounts as billingAccount>
      <tr>
        <td>${billingAccount.billingAccountId}</td>
        <td><@ofbizCurrency amount=billingAccount.accountLimit isoCode=billingAccount.accountCurrencyUomId/></td>
        <td class="button-col">
          <a href="<@ofbizUrl>EditBillingAccount?billingAccountId=${billingAccount.billingAccountId}</@ofbizUrl>">${uiLabelMap.CommonEdit}</a>
        </td>        
      </tr>
    </#list>
  <#else>
    <tr>
      <td colspan='3'>${uiLabelMap.AccountingNoBillingAccountFound}</td>
    </tr>    
  </#if>
</table>
