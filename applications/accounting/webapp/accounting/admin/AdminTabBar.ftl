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
<#if (parameters.organizationPartyId)?exists><#assign organizationPartyId = parameters.organizationPartyId></#if>
<#assign selected = page.tabButtonItem?default("void")>
<h1>${title?if_exists} ${labelTitleProperty?if_exists} ${uiLabelMap.CommonFor} ${uiLabelMap.Organization}: ${organizationPartyId}</h1>
<div class="button-bar button-style-1">
  <ul>
    <li<#if selected == "TimePeriods"> class="selected"</#if>><a href="<@ofbizUrl>TimePeriods?organizationPartyId=${organizationPartyId}</@ofbizUrl>">${uiLabelMap.TimePeriod}</a></li>
    <li<#if selected == "PartyAcctgPreference"> class="selected"</#if>><a href="<@ofbizUrl>PartyAcctgPreference?organizationPartyId=${organizationPartyId}</@ofbizUrl>">${uiLabelMap.AccountingPreference}</a></li>
    <li<#if selected == "ChecksTabButton"> class="selected"</#if>><a href="<@ofbizUrl>listChecksToPrint?organizationPartyId=${organizationPartyId}</@ofbizUrl>">${uiLabelMap.AccountingChecks}</a></li>
    <li<#if selected == "ViewFXConversions"> class="selected"</#if>><a href="<@ofbizUrl>viewFXConversions?organizationPartyId=${organizationPartyId}</@ofbizUrl>">${uiLabelMap.AccountingFX}</a></li>
    <li<#if selected == "EditGlJournalEntry"> class="selected"</#if>><a href="<@ofbizUrl>EditGlJournalEntry?organizationPartyId=${organizationPartyId}</@ofbizUrl>">${uiLabelMap.AccountingManualJournalEntry}</a></li>
    <li<#if selected == "ListUnpostedAcctgTrans"> class="selected"</#if>><a href="<@ofbizUrl>ListUnpostedAcctgTrans?organizationPartyId=${organizationPartyId}</@ofbizUrl>">${uiLabelMap.AccountingManualPostTrans}</a></li>
    <li<#if selected == "GlAccountAssignment"> class="selected"</#if>><a href="<@ofbizUrl>GlAccountAssignment?organizationPartyId=${organizationPartyId}</@ofbizUrl>">${uiLabelMap.AccountingGlAccountDefault}</a></li>
  </ul>
  <br class="clear"/>
</div>
<#if selected == "GlAccountAssignment">
  <div class="button-bar button-style-2">
    <ul>
      <li><a href="<@ofbizUrl>GlAccountSalInvoice?organizationPartyId=${organizationPartyId}</@ofbizUrl>">${uiLabelMap.AccountingInvoiceSales}</a></li>
      <li><a href="<@ofbizUrl>GlAccountPurInvoice?organizationPartyId=${organizationPartyId}</@ofbizUrl>">${uiLabelMap.AccountingInvoicePurchase}</a></li>
      <li><a href="<@ofbizUrl>GlAccountTypePaymentType?organizationPartyId=${organizationPartyId}</@ofbizUrl>">${uiLabelMap.AccountingPaymentType}/${uiLabelMap.FormFieldTitle_glAccountTypeId}</a></li>
      <li><a href="<@ofbizUrl>GlAccountNrPaymentMethod?organizationPartyId=${organizationPartyId}</@ofbizUrl>">${uiLabelMap.AccountingPaymentMethodId}/${uiLabelMap.AccountingGlAccountId}</a></li>
    </ul>
    <br class="clear"/>
  </div>
  <br/>
</#if>
