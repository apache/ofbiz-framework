<#--
Copyright 2001-2006 The Apache Software Foundation

Licensed under the Apache License, Version 2.0 (the "License"); you may not
use this file except in compliance with the License. You may obtain a copy of
the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
License for the specific language governing permissions and limitations
under the License.
-->

<#assign unselectedClassName = "tabButton">
<#assign selectedClassMap = {page.tabButtonItem?default("void") : "tabButtonSelected"}>

<#if billingAccount?has_content>
    <div class='tabContainer'>
        <a href="<@ofbizUrl>EditBillingAccount?billingAccountId=${billingAccount.billingAccountId}</@ofbizUrl>" class="${selectedClassMap.EditBillingAccount?default(unselectedClassName)}">${uiLabelMap.AccountingAccount}</a>
        <a href="<@ofbizUrl>EditBillingAccountRoles?billingAccountId=${billingAccount.billingAccountId}</@ofbizUrl>" class="${selectedClassMap.EditBillingAccountRoles?default(unselectedClassName)}">${uiLabelMap.PartyRoles}</a>
        <a href="<@ofbizUrl>EditBillingAccountTerms?billingAccountId=${billingAccount.billingAccountId}</@ofbizUrl>" class="${selectedClassMap.EditBillingAccountTerms?default(unselectedClassName)}">${uiLabelMap.PartyTerms}</a>
        <a href="<@ofbizUrl>BillingAccountInvoices?billingAccountId=${billingAccount.billingAccountId}</@ofbizUrl>" class="${selectedClassMap.BillingAccountInvoices?default(unselectedClassName)}">${uiLabelMap.AccountingInvoices}</a>
        <a href="<@ofbizUrl>BillingAccountPayments?billingAccountId=${billingAccount.billingAccountId}</@ofbizUrl>" class="${selectedClassMap.BillingAccountPayments?default(unselectedClassName)}">${uiLabelMap.AccountingPayments}</a>
    </div>
</#if>
