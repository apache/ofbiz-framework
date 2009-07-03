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
<div class="button-bar button-style-2">
  <ul>
    <li><a href="<@ofbizUrl>newInvoice</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonCreateNew}</a></li>
    <#if (invoice.invoiceId)?has_content>
      <li>
        <a href="javascript:document.InvoiceSubTabBar_copyInvoice.submit()" class="buttontext">${uiLabelMap.CommonCopy}</a>
        <form method="post" action="<@ofbizUrl>copyInvoice</@ofbizUrl>" onSubmit="javascript:submitFormDisableSubmits(this)" name="InvoiceSubTabBar_copyInvoice">
          <input type="hidden" name="invoiceIdToCopyFrom" value="${invoice.invoiceId}"/>
        </form>
      </li>
      <li><a target="_BLANK" href="<@ofbizUrl>invoice.pdf?invoiceId=${invoice.invoiceId}</@ofbizUrl>">${uiLabelMap.AccountingInvoicePDF}</a></li>
      <#if (invoice.currencyUomId)?if_exists != defaultOrganizationPartyCurrencyUomId>
        <li><a target="_BLANK" href="<@ofbizUrl>invoice.pdf?invoiceId=${invoice.invoiceId}&currency=${defaultOrganizationPartyCurrencyUomId}</@ofbizUrl>">${uiLabelMap.AccountingInvoicePDFDefaultCur}(${defaultOrganizationPartyCurrencyUomId})</a></li>
      </#if>
      <#if invoice.statusId == "INVOICE_IN_PROCESS" || invoice.statusId == "INVOICE_RECEIVED">
        <li>
          <a href="javascript:document.InvoiceSubTabBar_statusToApproved.submit()" class="buttontext">${uiLabelMap.AccountingInvoiceStatusToApproved}</a>
          <form method="post" action="<@ofbizUrl>setInvoiceStatus</@ofbizUrl>" onSubmit="javascript:submitFormDisableSubmits(this)" name="InvoiceSubTabBar_statusToApproved">
            <input type="hidden" name="invoiceId" value="${invoice.invoiceId}"/>
            <input type="hidden" name="statusId" value="INVOICE_APPROVED"/>
          </form>
        </li>
      </#if>
      <#if invoice.statusId == "INVOICE_IN_PROCESS">
        <#if invoice.invoiceTypeId == "PURCHASE_INVOICE" || invoice.invoiceTypeId == "CUST_RTN_INVOICE"> 
          <li>
            <a href="javascript:document.InvoiceSubTabBar_statusToReceived.submit()" class="buttontext">${uiLabelMap.AccountingInvoiceStatusToReceived}</a>
            <form method="post" action="<@ofbizUrl>setInvoiceStatus</@ofbizUrl>" onSubmit="javascript:submitFormDisableSubmits(this)" name="InvoiceSubTabBar_statusToReceived">
              <input type="hidden" name="invoiceId" value="${invoice.invoiceId}"/>
              <input type="hidden" name="statusId" value="INVOICE_RECEIVED"/>
            </form>
          </li>
        </#if>
      </#if>
      <#if invoice.invoiceTypeId == "SALES_INVOICE" && (invoice.statusId == "INVOICE_IN_PROCESS" || invoice.statusId == "INVOICE_APPROVED")>
        <li>
          <a href="javascript:document.InvoiceSubTabBar_statusToSent.submit()" class="buttontext">${uiLabelMap.AccountingInvoiceStatusToSent}</a>
          <form method="post" action="<@ofbizUrl>setInvoiceStatus</@ofbizUrl>" onSubmit="javascript:submitFormDisableSubmits(this)" name="InvoiceSubTabBar_statusToSent">
            <input type="hidden" name="invoiceId" value="${invoice.invoiceId}"/>
            <input type="hidden" name="statusId" value="INVOICE_SENT"/>
          </form>
        </li>
      </#if>
      <#if invoice.statusId == "INVOICE_IN_PROCESS" || invoice.statusId == "INVOICE_SENT" || invoice.statusId == "INVOICE_RECEIVED" || invoice.statusId == "INVOICE_APPROVED">
        <li>
          <a href="javascript:document.InvoiceSubTabBar_statusToReady.submit()" class="buttontext">${uiLabelMap.AccountingInvoiceStatusToReady}</a>
          <form method="post" action="<@ofbizUrl>setInvoiceStatus</@ofbizUrl>" onSubmit="javascript:submitFormDisableSubmits(this)" name="InvoiceSubTabBar_statusToReady">
            <input type="hidden" name="invoiceId" value="${invoice.invoiceId}"/>
            <input type="hidden" name="statusId" value="INVOICE_READY"/>
          </form>
        </li>
      </#if>
      <#if invoice.statusId == "INVOICE_READY">
        <li>
          <a href="javascript:document.InvoiceSubTabBar_statusToPaid.submit()" class="buttontext">${uiLabelMap.AccountingInvoiceStatusToPaid}</a>
          <form method="post" action="<@ofbizUrl>setInvoiceStatus</@ofbizUrl>" onSubmit="javascript:submitFormDisableSubmits(this)" name="InvoiceSubTabBar_statusToPaid">
            <input type="hidden" name="invoiceId" value="${invoice.invoiceId}"/>
            <input type="hidden" name="statusId" value="INVOICE_PAID"/>
          </form>
        </li>
        <li>
          <a href="javascript:confirmActionFormLink('You want to writeoff this invoice number ${invoice.invoiceId}?','InvoiceSubTabBar_statusToWriteoff')" class="buttontext">${uiLabelMap.AccountingInvoiceStatusToWriteoff}</a>
          <form method="post" action="<@ofbizUrl>setInvoiceStatus</@ofbizUrl>" onSubmit="javascript:submitFormDisableSubmits(this)" name="InvoiceSubTabBar_statusToWriteoff">
            <input type="hidden" name="invoiceId" value="${invoice.invoiceId}"/>
            <input type="hidden" name="statusId" value="INVOICE_WRITEOFF"/>
          </form>
        </li>
      </#if>
      <#if invoice.statusId == "INVOICE_SENT" || invoice.statusId == "INVOICE_RECEIVED">
        <li>
          <a href="javascript:document.InvoiceSubTabBar_statusToInProcess.submit()" class="buttontext">${uiLabelMap.AccountingInvoiceStatusToInProcess}</a>
          <form method="post" action="<@ofbizUrl>setInvoiceStatus</@ofbizUrl>" onSubmit="javascript:submitFormDisableSubmits(this)" name="InvoiceSubTabBar_statusToInProcess">
            <input type="hidden" name="invoiceId" value="${invoice.invoiceId}"/>
            <input type="hidden" name="statusId" value="INVOICE_IN_PROCESS"/>
          </form>
        </li>
      </#if>
      <#if invoice.statusId == "INVOICE_SENT" || invoice.statusId == "INVOICE_RECEIVED" || invoice.statusId == "INVOICE_IN_PROCESS" || invoice.statusId == "INVOICE_READY">
        <li>
          <a href="javascript:confirmActionFormLink('You want to cancel this invoice number ${invoice.invoiceId}?','InvoiceSubTabBar_statusToCancelled')" class="buttontext">${uiLabelMap.AccountingInvoiceStatusToCancelled}</a>
          <form method="post" action="<@ofbizUrl>setInvoiceStatus</@ofbizUrl>" onSubmit="javascript:submitFormDisableSubmits(this)" name="InvoiceSubTabBar_statusToCancelled">
            <input type="hidden" name="invoiceId" value="${invoice.invoiceId}"/>
            <input type="hidden" name="statusId" value="INVOICE_CANCELLED"/>
          </form>
        </li>
      </#if>
      <#if invoice.invoiceTypeId == "SALES_INVOICE" || invoice.invoiceTypeId == "PURCHASE_INVOICE">
        <li>
          <a href="javascript:document.InvoiceSubTabBar_saveInvoiceAsTemplate.submit()" class="buttontext">${uiLabelMap.ProjectMgrSaveAsTemplate}</a>
          <form method="post" action="<@ofbizUrl>setInvoiceStatus</@ofbizUrl>" onSubmit="javascript:submitFormDisableSubmits(this)" name="InvoiceSubTabBar_saveInvoiceAsTemplate">
            <input type="hidden" name="invoiceId" value="${invoice.invoiceId}"/>
            <input type="hidden" name="invoiceTypeId" value="${invoice.invoiceTypeId}"/>
          </form>
        </li>
      </#if>
    </#if>
  </ul>
</div>

