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
<#if (requestAttributes.uiLabelMap)?exists><#assign uiLabelMap = requestAttributes.uiLabelMap></#if>
<#if (parameters.organizationPartyId)?exists><#assign organizationPartyId = parameters.organizationPartyId></#if>
<#assign unselectedClassName = "tabButton">
<#assign selectedClassMap = {page.tabButtonItem?default("void") : "tabButtonSelected"}>
<div class="head1">${title?if_exists} ${labelTitleProperty?if_exists} ${uiLabelMap.CommonFor} ${uiLabelMap.Organization}: ${organizationPartyId}</div>
<div class="tabContainer">
    <a href="<@ofbizUrl>TimePeriods?organizationPartyId=${organizationPartyId}</@ofbizUrl>" class="${selectedClassMap.TimePeriods?default(unselectedClassName)}">${uiLabelMap.TimePeriod}</a>
    <a href="<@ofbizUrl>PartyAcctgPreference?organizationPartyId=${organizationPartyId}</@ofbizUrl>" class="${selectedClassMap.PartyAcctgPreference?default(unselectedClassName)}">${uiLabelMap.AccountingPreference}</a>
    <a href="<@ofbizUrl>listChecksToPrint?organizationPartyId=${organizationPartyId}</@ofbizUrl>" class="${selectedClassMap.ChecksTabButton?default(unselectedClassName)}">${uiLabelMap.AccountingChecks}</a>
    <a href="<@ofbizUrl>viewFXConversions?organizationPartyId=${organizationPartyId}</@ofbizUrl>" class="${selectedClassMap.ViewFXConversions?default(unselectedClassName)}">${uiLabelMap.AccountingFX}</a>
    <a href="<@ofbizUrl>EditGlJournalEntry?organizationPartyId=${organizationPartyId}</@ofbizUrl>" class="${selectedClassMap.EditGlJournalEntry?default(unselectedClassName)}">${uiLabelMap.AccountingManualJournalEntry}</a>
    <a href="<@ofbizUrl>ListUnpostedAcctgTrans?organizationPartyId=${organizationPartyId}</@ofbizUrl>" class="${selectedClassMap.ListUnpostedAcctgTrans?default(unselectedClassName)}">${uiLabelMap.AccountingManualPostTrans}</a>
    <a href="<@ofbizUrl>GlAccountAssignment?organizationPartyId=${organizationPartyId}</@ofbizUrl>" class="${selectedClassMap.GlAccountAssignment?default(unselectedClassName)}">${uiLabelMap.AccountingGlAccountDefault}</a>
</div>
<#if (page.tabButtonItem)?exists && page.tabButtonItem == "GlAccountAssignment">
<div>
    <a href="<@ofbizUrl>GlAccountSalInvoice?organizationPartyId=${organizationPartyId}</@ofbizUrl>" class="buttontext">[${uiLabelMap.AccountingInvoiceSales}]</a>
    <a href="<@ofbizUrl>GlAccountPurInvoice?organizationPartyId=${organizationPartyId}</@ofbizUrl>" class="buttontext">[${uiLabelMap.AccountingInvoicePurchase}]</a>
    <a href="<@ofbizUrl>GlAccountTypePaymentType?organizationPartyId=${organizationPartyId}</@ofbizUrl>" class="buttontext">[${uiLabelMap.AccountingPaymentType}/${uiLabelMap.FormFieldTitle_glAccountTypeId}]</a>
    <a href="<@ofbizUrl>GlAccountNrPaymentMethod?organizationPartyId=${organizationPartyId}</@ofbizUrl>" class="buttontext">[${uiLabelMap.AccountingPaymentMethodId}/${uiLabelMap.AccountingGlAccountId}]</a>
</div>
<br/>
</#if>
