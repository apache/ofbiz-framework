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

<#assign selected = page.headerItem?default("void")>

<div id="app-navigation">
  <h2>${uiLabelMap.AccountingManagerApplication}</h2>
  <ul>
    <li<#if selected == "main"> class="selected"</#if>><a href="<@ofbizUrl>main</@ofbizUrl>">${uiLabelMap.CommonMain}</a></li>
    <li<#if selected == "agreement"> class="selected"</#if>><a href="<@ofbizUrl>FindAgreement</@ofbizUrl>">${uiLabelMap.AccountingAgreements}</a></li>
    <li<#if selected == "billingaccount"> class="selected"</#if>><a href="<@ofbizUrl>FindBillingAccount</@ofbizUrl>">${uiLabelMap.AccountingBillingMenu}</a></li>
    <li<#if selected == "invoices"> class="selected"</#if>><a href="<@ofbizUrl>findInvoices</@ofbizUrl>">${uiLabelMap.AccountingInvoicesMenu}</a></li>
    <li<#if selected == "payments"> class="selected"</#if>><a href="<@ofbizUrl>findPayments</@ofbizUrl>">${uiLabelMap.AccountingPaymentsMenu}</a></li>
    <#if security.hasEntityPermission("MANUAL", "_PAYMENT", session)>
      <li<#if selected == "transaction"> class="selected"</#if>><a href="<@ofbizUrl>FindGatewayResponses</@ofbizUrl>">${uiLabelMap.AccountingTransactions}</a></li>
    </#if>
    <li<#if selected == "chartofaccounts"> class="selected"</#if>><a href="<@ofbizUrl>FindGlobalGlAccount</@ofbizUrl>">${uiLabelMap.AccountingChartOfAcctsMenu}</a></li>
    <li<#if selected == "ListFixedAssets"> class="selected"</#if>><a href="<@ofbizUrl>ListFixedAssets</@ofbizUrl>">${uiLabelMap.AccountingFixedAssets}</a></li>
    <li<#if selected == "TaxAuthorities"> class="selected"</#if>><a href="<@ofbizUrl>FindTaxAuthority</@ofbizUrl>">${uiLabelMap.AccountingTaxAuthorities}</a></li>
    <li<#if selected == "companies"> class="selected"</#if>><a href="<@ofbizUrl>ListCompanies</@ofbizUrl>">${uiLabelMap.AccountingCompanies}</a></li>
    <li<#if selected == "FindFinAccount"> class="selected"</#if>><a href="<@ofbizUrl>FindFinAccount</@ofbizUrl>">${uiLabelMap.AccountingFinAccount}</a></li>
    <#if userLogin?has_content>
      <li class="opposed"><a href="<@ofbizUrl>logout</@ofbizUrl>">${uiLabelMap.CommonLogout}</a></li>
    <#else>
      <li class="opposed"><a href="<@ofbizUrl>${checkLoginUrl?if_exists}</@ofbizUrl>">${uiLabelMap.CommonLogin}</a></li>
    </#if>
  </ul>
  <br class="clear"/>
</div>
