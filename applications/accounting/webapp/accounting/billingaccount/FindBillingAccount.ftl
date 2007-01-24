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

<div class="head1">${uiLabelMap.AccountingBillingAccounts}</div>
<div><a href="<@ofbizUrl>EditBillingAccount<#if (requestParameters.partyId)?has_content>?partyId=${requestParameters.partyId}&roleTypeId=BILL_TO_CUSTOMER</#if></@ofbizUrl>" class="buttontext">${uiLabelMap.AccountingNewAccount}</a></div>

<br/>
<table width="100%" border="0" cellpadding="0" cellspacing="0"> 
  <tr>
    <td><div class="tableheadtext">${uiLabelMap.AccountingAccountId}</div></td>
    <td><div class="tableheadtext">${uiLabelMap.AccountingAccountLimit}</div></td>
    <#if billingAccountRolesByParty?has_content>
      <#assign colSpan = "4">
      <td><div class="tableheadtext">${uiLabelMap.PartyRoleTypeId}</div></td>
    <#else>
      <#assign colSpan = "3">
    </#if>
    <td>&nbsp;</td>
  </tr>  
  <tr><td colspan="${colSpan}"><hr class="sepbar"></td></tr>    
  <#if billingAccountRolesByParty?has_content>
    <#list billingAccountRolesByParty as role>
      <#assign billingAccount = role.getRelatedOne("BillingAccount")>
      <#assign roleType = role.getRelatedOne("RoleType")>
      <tr>
        <td><div class="tabletext">${billingAccount.billingAccountId}</div></td>
        <td><div class="tabletext"><@ofbizCurrency amount=billingAccount.accountLimit isoCode=billingAccount.accountCurrencyUomId/></div></td>
        <td><div class="tabletext">${roleType.get("description",locale)}</div></td>
        <td align="right">
          <a href="<@ofbizUrl>EditBillingAccount?billingAccountId=${billingAccount.billingAccountId}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonEdit}</a>
        </td>
      </tr>
    </#list>
  <#elseif billingAccounts?has_content>
    <#list billingAccounts as billingAccount>
      <tr>
        <td><div class="tabletext">${billingAccount.billingAccountId}</div></td>
        <td><div class="tabletext"><@ofbizCurrency amount=billingAccount.accountLimit isoCode=billingAccount.accountCurrencyUomId/></div></td>
        <td align="right">
          <a href="<@ofbizUrl>EditBillingAccount?billingAccountId=${billingAccount.billingAccountId}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonEdit}</a>
        </td>        
      </tr>
    </#list>
  <#else>
    <tr>
      <td colspan='3'><div class="tabletext">${uiLabelMap.AccountingNoBillingAccountFound}</div></td>
    </tr>    
  </#if>
</table>
