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
<script type="application/javascript">
//<![CDATA[

    function toggleInvoiceId(master) {
        var invoices = jQuery("#listInvoices :checkbox[name='invoiceIds']");

        jQuery.each(invoices, function() {
            this.checked = master.checked;
        });
        getInvoiceRunningTotal();
    }

    function getInvoiceRunningTotal() {
        var invoices = jQuery("#listInvoices :checkbox[name='invoiceIds']");

        //test if all checkboxes are checked
        var allChecked = true;
        jQuery.each(invoices, function() {
            if (!jQuery(this).is(':checked')) {
                allChecked = false;
                return false;
            }
        });

        if(allChecked) {
            jQuery('#checkAllInvoices').prop('checked', true);
        } else {
            jQuery('#checkAllInvoices').prop('checked', false);
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
                async: true,
                data: jQuery('#listInvoices').serialize(),
                success: function(data) { jQuery('#showInvoiceRunningTotal').html(data.invoiceRunningTotal) }
            });

            if(jQuery('#serviceName').val() != "") {
                jQuery('#submitButton').prop('disabled', false);
            }

        } else {
            jQuery('#submitButton').prop('disabled', true);
            jQuery('#showInvoiceRunningTotal').html("");
        }
    }

    function setServiceName(selection) {
        if ( selection.value == 'massInvoicesToApprove' || selection.value == 'massInvoicesToSent' || selection.value == 'massInvoicesToReady' || selection.value == 'massInvoicesToPaid' || selection.value == 'massInvoicesToWriteoff' || selection.value == 'massInvoicesToCancel') {
            jQuery('#listInvoices').attr('action', jQuery('#invoiceStatusChange').val());
        } else {
            jQuery('#listInvoices').attr('action', selection.value);
        }
        if (selection.value == 'massInvoicesToApprove') {
            jQuery('#statusId').val("INVOICE_APPROVED");
        } else if (selection.value == 'massInvoicesToSent') {
            jQuery('#statusId').val("INVOICE_SENT");
        } else if (selection.value == 'massInvoicesToReady') {
            jQuery('#statusId').val("INVOICE_READY");
        } else if (selection.value == 'massInvoicesToPaid') {
            jQuery('#statusId').val("INVOICE_PAID");
        } else if (selection.value == 'massInvoicesToWriteoff') {
            jQuery('#statusId').val("INVOICE_WRITEOFF");
        } else if (selection.value == 'massInvoicesToCancel') {
            jQuery('#statusId').val("INVOICE_CANCELLED");
        }

        var invoices = jQuery("#listInvoices :checkbox[name='invoiceIds']");
        // check if any checkbox is checked
        var anyChecked = false;
        jQuery.each(invoices, function() {
            if (jQuery(this).is(':checked')) {
                anyChecked = true;
                return false;
            }
        });

        if(anyChecked && (jQuery('#serviceName').val() != "")) {
            jQuery('#submitButton').prop('disabled', false);
        } else {
            jQuery('#submitButton').prop('disabled', true);
        }
    }
//]]>
</script>
<#if invoices?has_content>
  <#assign invoiceList  =  invoices.getCompleteList() />
  <#assign eliClose = invoices.close() />
</#if>
<#if invoiceList?has_content && (parameters.noConditionFind)! == 'Y'>
  <div>
    <span class="label">${uiLabelMap.AccountingRunningTotalOutstanding} :</span>
    <span class="label" id="showInvoiceRunningTotal"></span>
  </div>
  <form name="listInvoices" id="listInvoices"  method="post" action="">
    <div align="right">
      <select name="serviceName" id="serviceName" onchange="javascript:setServiceName(this);">
        <option value="">${uiLabelMap.AccountingSelectAction}</option>
        <option value="<@ofbizUrl>PrintInvoices</@ofbizUrl>">${uiLabelMap.AccountingPrintInvoices}</option>
        <option value="massInvoicesToApprove">${uiLabelMap.AccountingInvoiceStatusToApproved}</option>
        <option value="massInvoicesToSent">${uiLabelMap.AccountingInvoiceStatusToSent}</option>
        <option value="massInvoicesToReady">${uiLabelMap.AccountingInvoiceStatusToReady}</option>
        <option value="massInvoicesToPaid">${uiLabelMap.AccountingInvoiceStatusToPaid}</option>
        <option value="massInvoicesToWriteoff">${uiLabelMap.AccountingInvoiceStatusToWriteoff}</option>
        <option value="massInvoicesToCancel">${uiLabelMap.AccountingInvoiceStatusToCancelled}</option>
      </select>
      <input id="submitButton" type="submit" value="${uiLabelMap.CommonRun}" disabled="disabled" />
      <input type="hidden" name="organizationPartyId" value="${defaultOrganizationPartyId}"/>
      <input type="hidden" name="partyIdFrom" value="${parameters.partyIdFrom!}"/>
      <input type="hidden" name="statusId" id="statusId" value="${parameters.statusId!}"/>
      <input type="hidden" name="fromInvoiceDate" value="${parameters.fromInvoiceDate!}"/>
      <input type="hidden" name="thruInvoiceDate" value="${parameters.thruInvoiceDate!}"/>
      <input type="hidden" name="fromDueDate" value="${parameters.fromDueDate!}"/>
      <input type="hidden" name="thruDueDate" value="${parameters.thruDueDate!}"/>
      <input type="hidden" name="invoiceStatusChange" id="invoiceStatusChange" value="<@ofbizUrl>massChangeInvoiceStatus</@ofbizUrl>"/>
    </div>

    <table class="basic-table hover-bar" cellspacing="0">
      <thead>
        <tr class="header-row-2">
          <td>${uiLabelMap.FormFieldTitle_invoiceId}</td>
          <td>${uiLabelMap.FormFieldTitle_invoiceTypeId}</td>
          <td>${uiLabelMap.AccountingInvoiceDate}</td>
          <td>${uiLabelMap.CommonStatus}</td>
          <td>${uiLabelMap.CommonDescription}</td>
          <td>${uiLabelMap.AccountingVendorParty}</td>
          <td>${uiLabelMap.AccountingToParty}</td>
          <td>${uiLabelMap.AccountingAmount}</td>
          <td>${uiLabelMap.FormFieldTitle_paidAmount}</td>
          <td>${uiLabelMap.FormFieldTitle_outstandingAmount}</td> 
          <td align="right"><label>${uiLabelMap.CommonSelectAll} <input type="checkbox" id="checkAllInvoices" name="checkAllInvoices" onchange="javascript:toggleInvoiceId(this);"/></label></td>
        </tr>
      </thead>
      <tbody>
        <#assign alt_row = false>
        <#list invoiceList as invoice>
          <#assign invoicePaymentInfoList = dispatcher.runSync("getInvoicePaymentInfoList", Static["org.apache.ofbiz.base.util.UtilMisc"].toMap("invoiceId", invoice.invoiceId, "userLogin", userLogin))/>
          <#assign invoicePaymentInfo = invoicePaymentInfoList.get("invoicePaymentInfoList").get(0)!>
            <tr valign="middle"<#if alt_row> class="alternate-row"</#if>>
              <td><a class="buttontext" href="<@ofbizUrl>invoiceOverview?invoiceId=${invoice.invoiceId}</@ofbizUrl>">${invoice.get("invoiceId")}</a></td>
              <td>
                <#assign invoiceType = delegator.findOne("InvoiceType", {"invoiceTypeId" : invoice.invoiceTypeId}, true) />
                ${invoiceType.description?default(invoice.invoiceTypeId)}
              </td>
              <td>${(invoice.invoiceDate)!}</td>
              <td>
                <#assign statusItem = delegator.findOne("StatusItem", {"statusId" : invoice.statusId}, true) />
                ${statusItem.description?default(invoice.statusId)}
              </td>
              <td>${(invoice.description)!}</td>
              <td><a href="<@ofbizUrl controlPath="/partymgr/control">viewprofile?partyId=${invoice.partyIdFrom}</@ofbizUrl>">${Static["org.apache.ofbiz.party.party.PartyHelper"].getPartyName(delegator, invoice.partyIdFrom, false)!} [${(invoice.partyIdFrom)!}] </a></td>
              <td><a href="<@ofbizUrl controlPath="/partymgr/control">viewprofile?partyId=${invoice.partyId}</@ofbizUrl>">${Static["org.apache.ofbiz.party.party.PartyHelper"].getPartyName(delegator, invoice.partyId, false)!} [${(invoice.partyId)!}]</a></td>
              <td><@ofbizCurrency amount=invoicePaymentInfo.amount isoCode=defaultOrganizationPartyCurrencyUomId/></td>
              <td><@ofbizCurrency amount=invoicePaymentInfo.paidAmount isoCode=defaultOrganizationPartyCurrencyUomId/></td>
              <td><@ofbizCurrency amount=invoicePaymentInfo.outstandingAmount isoCode=defaultOrganizationPartyCurrencyUomId/></td>
              <td align="right"><label><input type="checkbox" id="invoiceId_${invoice_index}" name="invoiceIds" value="${invoice.invoiceId}" onclick="javascript:getInvoiceRunningTotal();"/></label></td>
            </tr>
            <#-- toggle the row color -->
            <#assign alt_row = !alt_row>
        </#list>
      </tbody>
    </table>
  </form>
<#else>
  <h3>${uiLabelMap.AccountingNoInvoicesFound}</h3>
</#if>
