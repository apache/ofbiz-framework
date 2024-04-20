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

<#if invoices?has_content >
  <form name="listSalesInvoices" id="listSalesInvoices" method="post">
    <#if salesRepPartyList?has_content>
      ${setRequestAttribute("partyIds", salesRepPartyList)}
    </#if>
    <table class="basic-table hover-bar" cellspacing="0">
      <#-- Header Begins -->
      <tr class="header-row-2">
        <td width="6%">${uiLabelMap.FormFieldTitle_invoiceId}</td>
        <td width="10%">${uiLabelMap.AccountingFromParty}</td>
        <td width="14%">${uiLabelMap.AccountingToParty}</td>
        <td width="4%">${uiLabelMap.CommonStatus}</td>
        <td width="9%">${uiLabelMap.AccountingReferenceNumber}</td>
        <td width="12%">${uiLabelMap.CommonDescription}</td>
        <td width="9%">${uiLabelMap.AccountingInvoiceDate}</td>
        <td width="8%">${uiLabelMap.AccountingDueDate}</td>
        <td width="8%">${uiLabelMap.AccountingAmount}</td>
        <td width="8%">${uiLabelMap.FormFieldTitle_paidAmount}</td>
        <td width="8%">${uiLabelMap.FormFieldTitle_outstandingAmount}</td>
      </tr>
      <#-- Header Ends-->
      <#assign alt_row = false>
      <#list invoices as invoice>
        <#assign invoicePaymentInfoList = dispatcher.runSync("getInvoicePaymentInfoList", Static["org.apache.ofbiz.base.util.UtilMisc"].toMap("invoiceId", invoice.invoiceId, "userLogin", userLogin))/>
        <#assign invoicePaymentInfo = invoicePaymentInfoList.get("invoicePaymentInfoList").get(0)!>
        <#assign statusItem = delegator.findOne("StatusItem", {"statusId" : invoice.statusId}, false)!/>
        <tr valign="middle"<#if alt_row> class="alternate-row"</#if>>
          <td><a class="buttontext" href="<@ofbizUrl>viewInvoice?invoiceId=${invoice.invoiceId}</@ofbizUrl>">${invoice.get("invoiceId")}</a></td>
          <td><a href="<@ofbizUrl controlPath="/partymgr/control">viewprofile?partyId=${invoice.partyIdFrom}</@ofbizUrl>">${Static["org.apache.ofbiz.party.party.PartyHelper"].getPartyName(delegator, invoice.partyIdFrom, false)!}</a></td>
          <td><a href="<@ofbizUrl controlPath="/partymgr/control">viewprofile?partyId=${invoice.invoiceRolePartyId}</@ofbizUrl>">${Static["org.apache.ofbiz.party.party.PartyHelper"].getPartyName(delegator, invoice.invoiceRolePartyId, false)!}</a></td>
          <td>${statusItem.get("description")!}</td>
          <td>${invoice.get("referenceNumber")!}</td>
          <td>${invoice.get("description")!}</td>
          <td>${invoice.get("invoiceDate")!}</td>
          <td>${invoice.get("dueDate")!}</td>
          <td><@ofbizCurrency amount=invoicePaymentInfo.amount isoCode=defaultOrganizationPartyCurrencyUomId/></td>
          <td><@ofbizCurrency amount=invoicePaymentInfo.paidAmount isoCode=defaultOrganizationPartyCurrencyUomId/></td>
          <td><@ofbizCurrency amount=invoicePaymentInfo.outstandingAmount isoCode=defaultOrganizationPartyCurrencyUomId/></td>
        </tr>
        <#-- toggle the row color -->
        <#assign alt_row = !alt_row>
      </#list>
    </table>
  </form>
<#else>
  <h3>${uiLabelMap.AccountingNoInvoicesFound}</h3>
</#if>
