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
    var invoices = jQuery("#listPurchaseInvoices :checkbox[name='invoiceIds']");

    jQuery.each(invoices, function() {
        // this a normal html object (not a jquery object)
        this.checked = master.checked;
    });
    getInvoiceRunningTotal();
}

function getInvoiceRunningTotal() {
    var invoices = jQuery("#listPurchaseInvoices :checkbox[name='invoiceIds']");

    //test if all checkboxes are checked
    var allChecked = true;
    jQuery.each(invoices, function() {
        if (!jQuery(this).is(':checked')) {
            allChecked = false;
            return false;
        }
    });

    if(allChecked) {
        jQuery('#checkAllInvoices').attr('checked', true);
    } else {
        jQuery('#checkAllInvoices').attr('checked', false);
    }

    // check if any checkbox is checked
    var anyChecked = false;
    jQuery.each(invoices, function() {
        if (jQuery(this).is(':checked')) {
            anyChecked = true;
            return false;
        }
    });

    if(anyChecked) {
        jQuery.ajax({
            url: 'getInvoiceRunningTotal',
            type: 'POST',
            data: jQuery('#listPurchaseInvoices').serialize(),
            async: false,
            success: function(data) {
                jQuery('#showInvoiceRunningTotal').html(data.invoiceRunningTotal);
            }
        });

        if(jQuery('#serviceName').val() != "") {
            jQuery('#submitButton').removeAttr('disabled');
        }

    } else {
        jQuery('#submitButton').attr('disabled', true);
        jQuery('#showInvoiceRunningTotal').html("");
    }
}

function setServiceName(selection) {
    if ( selection.value == 'massInvoicesToApprove' || selection.value == 'massInvoicesToReceive' || selection.value == 'massInvoicesToReady' || selection.value == 'massInvoicesToPaid' || selection.value == 'massInvoicesToWriteoff' || selection.value == 'massInvoicesToCancel') {
        document.listPurchaseInvoices.action = 'massChangeInvoiceStatus';
    } else {
        document.listPurchaseInvoices.action = selection.value;
    }
    if (selection.value == 'massInvoicesToApprove') {
        jQuery('#statusId').val("INVOICE_APPROVED");
    } else if (selection.value == 'massInvoicesToReceive') {
        jQuery('#statusId').val("INVOICE_RECEIVED");
    }else if (selection.value == 'massInvoicesToReady') {
        jQuery('#statusId').val("INVOICE_READY");
    }else if (selection.value == 'massInvoicesToPaid') {
        jQuery('#statusId').val("INVOICE_PAID");
    }else if (selection.value == 'massInvoicesToWriteoff') {
        jQuery('#statusId').val("INVOICE_WRITEOFF");
    }else if (selection.value == 'massInvoicesToCancel') {
        jQuery('#statusId').val("INVOICE_CANCELLED");
    }
    if (selection.value.indexOf('processMassCheckRun') >= 0) {
        jQuery('#issueChecks').fadeIn('slow');
    } else {
        jQuery('#issueChecks').fadeOut('slow');
    }

    var invoices = jQuery("#listPurchaseInvoices :checkbox[name='invoiceIds']");
    // check if any checkbox is checked
    var anyChecked = false;
    jQuery.each(invoices, function() {
        if (jQuery(this).is(':checked')) {
            anyChecked = true;
            return false;
        }
    });

    if(anyChecked && (jQuery('#serviceName').val() != "")) {
            jQuery('#submitButton').removeAttr('disabled');
    }
}

function runAction() {
    jQuery('#listPurchaseInvoices').submit();
}

-->
</script>

<#if invoices?has_content >
  <div>
    <span class="label">${uiLabelMap.AccountingRunningTotalOutstanding} :</span>
    <span class="label" id="showInvoiceRunningTotal"></span>
  </div>
  <form name="listPurchaseInvoices" id="listPurchaseInvoices"  method="post" action="javascript:void(0);">
    <div align="right">
      <!-- May add some more options in future like cancel selected invoices-->
      <select name="serviceName" id="serviceName" onchange="javascript:setServiceName(this);">
        <option value="">${uiLabelMap.AccountingSelectAction}</option>
        <option value="<@ofbizUrl>processMassCheckRun</@ofbizUrl>" id="processMassCheckRun">${uiLabelMap.AccountingIssueCheck}</option>
        <option value="<@ofbizUrl>PrintInvoices</@ofbizUrl>">${uiLabelMap.AccountingPrintInvoices}</option>
        <option value="massInvoicesToApprove">${uiLabelMap.AccountingInvoiceStatusToApproved}</option>
        <option value="massInvoicesToReceive">${uiLabelMap.AccountingInvoiceStatusToReceived}</option>
        <option value="massInvoicesToReady">${uiLabelMap.AccountingInvoiceStatusToReady}</option>
        <option value="massInvoicesToPaid">${uiLabelMap.AccountingInvoiceStatusToPaid}</option>
        <option value="massInvoicesToWriteoff">${uiLabelMap.AccountingInvoiceStatusToWriteoff}</option>
        <option value="massInvoicesToCancel">${uiLabelMap.AccountingInvoiceStatusToCancelled}</option>
      </select>
      <input id="submitButton" type="button" onclick="javascript:runAction();" value="${uiLabelMap.CommonRun}" disabled="disabled" />
    </div>
    <input type="hidden" name="invoiceStatusChange" id="invoiceStatusChange" value="<@ofbizUrl>massChangeInvoiceStatus</@ofbizUrl>"/>
    <input type="hidden" name="organizationPartyId" value="${organizationPartyId}"/>
    <input type="hidden" name="partyIdFrom" value="${parameters.partyIdFrom!}"/>
    <input type="hidden" name="statusId" id="statusId" value="${parameters.statusId!}"/>
    <input type="hidden" name="fromInvoiceDate" value="${parameters.fromInvoiceDate!}"/>
    <input type="hidden" name="thruInvoiceDate" value="${parameters.thruInvoiceDate!}"/>
    <input type="hidden" name="fromDueDate" value="${parameters.fromDueDate!}"/>
    <input type="hidden" name="thruDueDate" value="${parameters.thruDueDate!}"/>
    <div id="issueChecks" style="display: none;" align="right">
      <span class="label">${uiLabelMap.AccountingVendorPaymentMethod}</span>
      <select name="paymentMethodId">
        <#if paymentMethods?has_content>
          <#list paymentMethods as paymentMethod>
            <#if paymentMethod.finAccountId?has_content>
              <#assign finAccount = delegator.findOne("FinAccount", {"finAccountId" : paymentMethod.finAccountId}, true) />
              <#if finAccount?has_content>
                <#if (finAccount.statusId != 'FNACT_MANFROZEN') && (finAccount.statusId != 'FNACT_CANCELLED')>
                  <option value="${paymentMethod.get("paymentMethodId")}"><#if paymentMethod.get("description")?has_content>${paymentMethod.get("description")}</#if>[${paymentMethod.get("paymentMethodId")}]</option>
                </#if>
              </#if>
            </#if>
          </#list>
        </#if>
      </select>
      <span class="label">${uiLabelMap.AccountingCheckNumber}</span>
      <input type="text" name="checkStartNumber"/>
    </div>
    <table class="basic-table hover-bar" cellspacing="0">
      <#-- Header Begins -->
      <tr class="header-row-2">
        <td>${uiLabelMap.FormFieldTitle_invoiceId}</td>
        <td>${uiLabelMap.AccountingInvoiceDate}</td>
        <td>${uiLabelMap.AccountingDueDate}</td>
        <td>${uiLabelMap.CommonStatus}</td>
        <td>${uiLabelMap.AccountingReferenceNumber}</td>
        <td>${uiLabelMap.CommonDescription}</td>
        <td>${uiLabelMap.AccountingVendorParty}</td>
        <td>${uiLabelMap.AccountingToParty}</td>
        <td>${uiLabelMap.AccountingAmount}</td>
        <td>${uiLabelMap.FormFieldTitle_paidAmount}</td>
        <td>${uiLabelMap.FormFieldTitle_outstandingAmount}</td> 
        <td><label>${uiLabelMap.CommonSelectAll} <input type="checkbox" id="checkAllInvoices" name="checkAllInvoices" onchange="javascript:toggleInvoiceId(this);"/></label></td>
      </tr>
      <#-- Header Ends-->
      <#assign alt_row = false>
      <#list invoices as invoice>
        <#assign invoicePaymentInfoList = dispatcher.runSync("getInvoicePaymentInfoList", Static["org.ofbiz.base.util.UtilMisc"].toMap("invoiceId", invoice.invoiceId, "userLogin", userLogin))/>
        <#assign invoicePaymentInfo = invoicePaymentInfoList.get("invoicePaymentInfoList").get(0)!>
          <#assign statusItem = invoice.getRelatedOne("StatusItem", true)>
          <tr valign="middle"<#if alt_row> class="alternate-row"</#if>>
            <td><a class="buttontext" href="<@ofbizUrl>invoiceOverview?invoiceId=${invoice.invoiceId}</@ofbizUrl>">${invoice.get("invoiceId")}</a></td>
            <td><#if invoice.get("invoiceDate")?has_content>${invoice.get("invoiceDate")?date}</td></#if>
            <td><#if invoice.get("dueDate")?has_content>${invoice.get("dueDate")?date}</td></#if>
            <td>${statusItem.description?default(invoice.statusId)}</td>
            <td>${invoice.get("referenceNumber")!}</td>
            <td>${(invoice.description)!}</td>
            <td><a href="/partymgr/control/viewprofile?partyId=${invoice.partyIdFrom}">${Static["org.ofbiz.party.party.PartyHelper"].getPartyName(delegator, invoice.partyIdFrom, false)!} [${(invoice.partyIdFrom)!}] </a></td>
            <td><a href="/partymgr/control/viewprofile?partyId=${invoice.partyId}">${Static["org.ofbiz.party.party.PartyHelper"].getPartyName(delegator, invoice.partyId, false)!} [${(invoice.partyId)!}]</a></td>
            <td><@ofbizCurrency amount=invoicePaymentInfo.amount isoCode=defaultOrganizationPartyCurrencyUomId/></td>
            <td><@ofbizCurrency amount=invoicePaymentInfo.paidAmount isoCode=defaultOrganizationPartyCurrencyUomId/></td>
            <td><@ofbizCurrency amount=invoicePaymentInfo.outstandingAmount isoCode=defaultOrganizationPartyCurrencyUomId/></td>
            <td align="right"><label><input type="checkbox" id="invoiceId_${invoice_index}" name="invoiceIds" value="${invoice.invoiceId}" onclick="javascript:getInvoiceRunningTotal();"/></label></td>
          </tr>
          <#-- toggle the row color -->
          <#assign alt_row = !alt_row>
      </#list>
    </table>
  </form>
<#else>
  <h3>${uiLabelMap.AccountingNoInvoicesFound}</h3>
</#if>
