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
<script language="JavaScript" type="text/javascript">
<!--
function toggleInvoiceId(master) {
    var form = document.listPurchaseInvoices;
    var invoices = form.elements.length;
    for (var i = 0; i < invoices; i++) {
        var element = form.elements[i];
        if (element.name == "invoiceIds") {
            element.checked = master.checked;
        }
    }
    getInvoiceRunningTotal(master);
}

function getInvoiceRunningTotal(e) {
    var form = document.listPurchaseInvoices;
    var invoices = form.elements.length;
    var isSingle = true;
    var isAllSelected = true;
    for (var i = 0; i < invoices; i++) {
        var element = form.elements[i];
        if (element.name == "invoiceIds") {
            if (element.checked) {
                isSingle = false;
            } else {
                isAllSelected = false;
            }
        }
    }
    if (!($(e).checked)) {
        $('checkAllInvoices').checked = false;
    } else if (isAllSelected) {
        $('checkAllInvoices').checked = true;
    }
    if (!isSingle) {
        new Ajax.Request('getInvoiceRunningTotal', {
            asynchronous: false,
            onSuccess: function(transport) {
                var data = transport.responseText.evalJSON(true);
                $('showInvoiceRunningTotal').update(data.invoiceRunningTotal);
            }, parameters: $('listPurchaseInvoices').serialize(), requestHeaders: {Accept: 'application/json'}
        });
    } else {
        $('showInvoiceRunningTotal').update("");
    }
}

function setServiceName(selection) {
    document.listPurchaseInvoices.action = '<@ofbizUrl>'+selection.value+'</@ofbizUrl>';
    showIssueChecks(selection);
}

function runAction() {
    var form = document.listPurchaseInvoices;
    var invoices = form.elements.length;
    for (var i = 0; i < invoices; i++) {
        var element = form.elements[i];
        if (element.name == "invoiceIds") {
            element.disabled = false;
        }
    }
    form.submit();
}

function showIssueChecks(selection) {
    if (selection.value == 'processMassCheckRun') {
        Effect.BlindDown('issueChecks',{duration: 0.0});
    } else {
        Effect.BlindUp('issueChecks',{duration: 0.0});
    }
}
-->
</script>

<#if invoices?has_content >
  <div>
    <span class="label">${uiLabelMap.AccountingRunningTotal} :</span>
    <span class="label" id="showInvoiceRunningTotal"></span>
  </div>
  <form name="listPurchaseInvoices" id="listPurchaseInvoices"  method="post" action="javascript:void();">
    <div align="right">
      <!-- May add some more options in future like cancel selected invoices-->
      <select name="serviceName" id="serviceName" onchange="javascript:setServiceName(this);">
        <option value=""/>
        <option value="processMassCheckRun">${uiLabelMap.AccountingIssueCheck}</option>
      </select>
      <a href="javascript:runAction();" id="runAction" class="buttontext">${uiLabelMap.OrderRunAction}</a>
    </div>
    <input type="hidden" name="organizationPartyId" value="${organizationPartyId}"/>
    <div id="issueChecks" style="display: none;" align="right">
      <span class="label">${uiLabelMap.AccountingVendorPaymentMethod}</span>
      <select name="paymentMethodTypeId">
        <option value=""></option>
        <#if paymentMethodType?has_content>
          <option value="${paymentMethodType.paymentMethodTypeId}">${paymentMethodType.description}</option>
        </#if>
      </select>
      <span class="label">${uiLabelMap.AccountingBankAccount}</span>
      <select name="bankAccount">
        <option value=""></option>
        <#if finAccounts?has_content>
          <#list finAccounts as finAccount>
            <option value="${finAccount.get("finAccountId")}">${finAccount.get("finAccountName")} [${finAccount.get("finAccountId")}]</option>
          </#list>
        </#if>
      </select>
      <span class="label">${uiLabelMap.AccountingCheckNumber}</span>
      <input type="text" name="checkStartNumber"/>
    </div>
    <table class="basic-table hover-bar" cellspacing="0">
      <#-- Header Begins -->
      <tr class="header-row-2">
        <td width="8%"><input type="checkbox" id="checkAllInvoices" name="checkAllInvoices" onchange="javascript:toggleInvoiceId(this);"/> ${uiLabelMap.CommonSelectAll}</td>
        <td width="10%">${uiLabelMap.FormFieldTitle_invoiceId}</td>
        <td width="15%">${uiLabelMap.AccountingVendorParty}</td>
        <td width="10%">${uiLabelMap.CommonStatus}</td>
        <td width="10%">${uiLabelMap.AccountingReferenceNumber}</td>
        <td width="10%">${uiLabelMap.AccountingInvoiceDate}</td>
        <td width="10%">${uiLabelMap.AccountingDueDate}</td>
        <td width="9%">${uiLabelMap.AccountingAmount}</td>
        <td width="9%">${uiLabelMap.FormFieldTitle_paidAmount}</td>
        <td width="9%">${uiLabelMap.FormFieldTitle_outstandingAmount}</td>
      </tr>
      <#-- Header Ends-->
      <#assign alt_row = false>
      <#list invoices as invoice>
        <#assign invoicePaymentInfoList = dispatcher.runSync("getInvoicePaymentInfoList", Static["org.ofbiz.base.util.UtilMisc"].toMap("invoiceId", invoice.invoiceId, "userLogin", userLogin))/>
        <#assign invoicePaymentInfo = invoicePaymentInfoList.get("invoicePaymentInfoList").get(0)?if_exists>
        <#if invoicePaymentInfo.outstandingAmount != 0>
          <#assign statusItem = invoice.getRelatedOneCache("StatusItem")>
          <tr valign="middle"<#if alt_row> class="alternate-row"</#if>>
            <td><input type="checkbox" id="invoiceId_${invoice_index}" name="invoiceIds" value="${invoice.invoiceId}" onclick="javascript:getInvoiceRunningTotal('invoiceId_${invoice_index}');"/></td>
            <td><a class="buttontext" href="<@ofbizUrl>invoiceOverview?invoiceId=${invoice.invoiceId}</@ofbizUrl>">${invoice.get("invoiceId")}</a></td>
            <td>${Static["org.ofbiz.party.party.PartyHelper"].getPartyName(delegator, invoice.partyIdFrom, false)?if_exists}</td>
            <td>${statusItem.get("description")?if_exists}</td>
            <td>${invoice.get("referenceNumber")?if_exists}</td>
            <td>${invoice.get("invoiceDate")?if_exists}</td>
            <td>${invoice.get("dueDate")?if_exists}</td>
            <td><@ofbizCurrency amount=invoicePaymentInfo.amount isoCode=defaultOrganizationPartyCurrencyUomId/></td>
            <td><@ofbizCurrency amount=invoicePaymentInfo.paidAmount isoCode=defaultOrganizationPartyCurrencyUomId/></td>
            <td><@ofbizCurrency amount=invoicePaymentInfo.outstandingAmount isoCode=defaultOrganizationPartyCurrencyUomId/></td>
          </tr>
          <#-- toggle the row color -->
          <#assign alt_row = !alt_row>
        </#if>
      </#list>
    </table>
  </form>
<#else>
  <td colspan='4'><h3>${uiLabelMap.AccountingNoInvoicesFound}</h3></td>
</#if>
