<#--
 *  Copyright (c) 2003-2005 The Open For Business Project - www.ofbiz.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a 
 *  copy of this software and associated documentation files (the "Software"), 
 *  to deal in the Software without restriction, including without limitation 
 *  the rights to use, copy, modify, merge, publish, distribute, sublicense, 
 *  and/or sell copies of the Software, and to permit persons to whom the 
 *  Software is furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included 
 *  in all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS 
 *  OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF 
 *  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. 
 *  IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY 
 *  CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT 
 *  OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR 
 *  THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 *@author     Andy Zeneski (jaz@ofbiz.org)
 *@author     Catherine.Heintz@nereide.biz (migration to UiLabel)
 *@version    $Rev$
 *@since      2.1
-->
<#if (requestAttributes.uiLabelMap)?exists><#assign uiLabelMap = requestAttributes.uiLabelMap></#if>
<#if (requestAttributes.security)?exists><#assign security = requestAttributes.security></#if>
<#if (requestAttributes.userLogin)?exists><#assign userLogin = requestAttributes.userLogin></#if>
<#if (requestAttributes.checkLoginUrl)?exists><#assign checkLoginUrl = requestAttributes.checkLoginUrl></#if>

<#assign unselectedLeftClassName = "headerButtonLeft">
<#assign unselectedRightClassName = "headerButtonRight">
<#assign selectedLeftClassMap = {page.headerItem?default("void") : "headerButtonLeftSelected"}>
<#assign selectedRightClassMap = {page.headerItem?default("void") : "headerButtonRightSelected"}>

<div class="apptitle">${uiLabelMap.AccountingManagerApplication}</div>
<div class="row">
  <div class="col"><a href="<@ofbizUrl>main</@ofbizUrl>" class="${selectedLeftClassMap.main?default(unselectedLeftClassName)}">${uiLabelMap.AccountingMainMenu}</a></div>  
  <div class="col"><a href="<@ofbizUrl>FindAgreement</@ofbizUrl>" class="${selectedLeftClassMap.agreement?default(unselectedLeftClassName)}">${uiLabelMap.AccountingAgreements}</a></div>
  <div class="col"><a href="<@ofbizUrl>FindBillingAccount</@ofbizUrl>" class="${selectedLeftClassMap.billingaccount?default(unselectedLeftClassName)}">${uiLabelMap.AccountingBillingMenu}</a></div>
  <div class="col"><a href="<@ofbizUrl>findInvoices</@ofbizUrl>" class="${selectedLeftClassMap.invoices?default(unselectedLeftClassName)}">${uiLabelMap.AccountingInvoicesMenu}</a></div>
  <div class="col"><a href="<@ofbizUrl>findPayments</@ofbizUrl>" class="${selectedLeftClassMap.payments?default(unselectedLeftClassName)}">${uiLabelMap.AccountingPaymentsMenu}</a></div>
  <#if security.hasEntityPermission("MANUAL", "_PAYMENT", session)>
    <div class="col"><a href="<@ofbizUrl>FindGatewayResponses</@ofbizUrl>" class="${selectedLeftClassMap.transaction?default(unselectedLeftClassName)}">${uiLabelMap.AccountingTransactions}</a></div>
  </#if>
  <div class="col"><a href="<@ofbizUrl>FindGlobalGlAccount</@ofbizUrl>" class="${selectedLeftClassMap.chartofaccounts?default(unselectedLeftClassName)}">${uiLabelMap.AccountingChartOfAcctsMenu}</a></div>
  <div class="col"><a href="<@ofbizUrl>ListFixedAssets</@ofbizUrl>" class="${selectedLeftClassMap.ListFixedAssets?default(unselectedLeftClassName)}">${uiLabelMap.AccountingFixedAssets}</a></div>
  <div class="col"><a href="<@ofbizUrl>FindTaxAuthority</@ofbizUrl>" class="${selectedLeftClassMap.TaxAuthorities?default(unselectedLeftClassName)}">${uiLabelMap.AccountingTaxAuthorities}</a></div>
  <div class="col"><a href="<@ofbizUrl>ListCompanies</@ofbizUrl>" class="${selectedLeftClassMap.companies?default(unselectedLeftClassName)}">${uiLabelMap.AccountingCompanies}</a></div>

  <#if userLogin?has_content>
    <div class="col-right"><a href="<@ofbizUrl>logout</@ofbizUrl>" class="${selectedRightClassMap.logout?default(unselectedRightClassName)}">${uiLabelMap.CommonLogout}</a></div>
  <#else>
    <div class="col-right"><a href='<@ofbizUrl>${checkLoginUrl?if_exists}</@ofbizUrl>' class='${selectedRightClassMap.login?default(unselectedRightClassName)}'>${uiLabelMap.CommonLogin}</a></div>
  </#if>
  <div class="col-fill">&nbsp;</div>
</div>
