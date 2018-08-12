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
function togglePaymentId(master) {
    var payments = jQuery("#paymentBatchForm :checkbox[name='paymentIds']");

    jQuery.each(payments, function() {
        this.checked = master.checked;
    });
    getPaymentRunningTotal();
}
function getPaymentRunningTotal() {
    var payments = jQuery("#paymentBatchForm :checkbox[name='paymentIds']");

    //test if all checkboxes are checked
    var allChecked = true;
    jQuery.each(payments, function() {
        if (!jQuery(this).is(':checked')) {
            allChecked = false;
            return false;
        }
    });

    if(allChecked) {
        jQuery('#checkAllPayments').attr('checked', true);
    } else {
        jQuery('#checkAllPayments').attr('checked', false);
    }

    // check if any checkbox is checked
    var anyChecked = false;
    jQuery.each(payments, function() {
        if (jQuery(this).is(':checked')) {
            anyChecked = true;
            return false;
        }
    });

    if(anyChecked) {
        jQuery({
            url: 'getPaymentRunningTotal',
            async: true,
            data: jQuery('#paymentBatchForm').serialize(),
            success: function(data) {
                jQuery('#showPaymentRunningTotal').html(data.paymentRunningTotal);
            }
        });

        if(jQuery('#serviceName').val() != "") {
            jQuery('#submitButton').removeAttr('disabled');
        } else {
            jQuery('#submitButton').attr('disabled', true);
        }

    } else {
        jQuery('#submitButton').attr('disabled', true);
        jQuery('#showPaymentRunningTotal').html("");
    }
}
function setServiceName(selection) {
    if (selection.value == 'massPaymentsToNotPaid' || selection.value == 'massPaymentsToReceived' || selection.value == 'massPaymentsToConfirmed' || selection.value == 'massPaymentsToCancelled' || selection.value == 'massPaymentsToVoid') {
        jQuery('#paymentBatchForm').attr('action', jQuery('#paymentStatusChange').val());
    }
    else {
        jQuery('#paymentBatchForm').attr('action', selection.value);
    }
    if (selection.value == 'massPaymentsToNotPaid') {
        jQuery('#statusId').val("PMNT_NOT_PAID");
    } else if (selection.value == 'massPaymentsToReceived') {
        jQuery('#statusId').val("PMNT_RECEIVED");
    }else if (selection.value == 'massPaymentsToConfirmed') {
        jQuery('#statusId').val("PMNT_CONFIRMED");
    }else if (selection.value == 'massPaymentsToCancelled') {
        jQuery('#statusId').val("PMNT_CANCELLED");
    }else if (selection.value == 'massPaymentsToVoid') {
        jQuery('#statusId').val("PMNT_VOID");
    }
    if (jQuery('#processBatchPayment').is(':selected')) {
        jQuery('#createPaymentBatch').fadeOut('slow');
    } else {
        jQuery('#createPaymentBatch').fadeIn('slow');
    }

    var payments = jQuery("#paymentBatchForm :checkbox[name='paymentIds']");
    // check if any checkbox is checked
    var anyChecked = false;
    jQuery.each(payments, function() {
        if (jQuery(this).is(':checked')) {
            anyChecked = true;
            return false;
        }
    });

    if(anyChecked && (jQuery('#serviceName').val() != "")) {
        jQuery('#submitButton').removeAttr('disabled');
    } else {
       jQuery('#submitButton').attr('disabled' , true);
    }

}
//]]>

</script>
<div class="screenlet">
    <div class="screenlet-body">
        <form id="paymentBatchForm" method="post" action="">
            <#if paymentList?has_content>
                <div class="clearfix">
                <div class="float-left">
                    <span class="label">${uiLabelMap.AccountingRunningTotal} :</span>
                    <span class="label" id="showPaymentRunningTotal"></span>
                </div>
                <div class="align-float">
                    <select name="serviceName" id="serviceName" onchange="javascript:setServiceName(this);">
                        <option value="">${uiLabelMap.AccountingSelectAction}</option>
                        <option value="<@ofbizUrl>createPaymentBatch</@ofbizUrl>" id="processBatchPayment">${uiLabelMap.AccountingCreateBatch}</option>
                        <option value="massPaymentsToNotPaid">${uiLabelMap.AccountingPaymentStatusToNotPaid}</option>
                        <option value="massPaymentsToReceived">${uiLabelMap.AccountingInvoiceStatusToReceived}</option>
                        <option value="massPaymentsToConfirmed">${uiLabelMap.AccountingPaymentTabStatusToConfirmed}</option>
                        <option value="massPaymentsToCancelled">${uiLabelMap.AccountingPaymentTabStatusToCancelled}</option>
                        <option value="massPaymentsToVoid">${uiLabelMap.AccountingPaymentTabStatusToVoid}</option>
                    </select>
                    <input id="submitButton" type="button" onclick="javascript:jQuery('#paymentBatchForm').submit();" value="${uiLabelMap.CommonRun}" disabled="disabled" />
                    <input type="hidden" name='organizationPartyId' value="${organizationPartyId!}" />
                    <input type="hidden" name='paymentGroupTypeId' value="BATCH_PAYMENT" />
                    <input type="hidden" name="groupInOneTransaction" value="Y" />
                    <input type="hidden" name="paymentStatusChange" id="paymentStatusChange" value="<@ofbizUrl>massChangePaymentStatus</@ofbizUrl>" />
                    <input type="hidden" name="statusId" id="statusId" value="${parameters.statusId!}" />
                    <#if finAccountId?has_content>
                        <input type="hidden" name='finAccountId' value="${finAccountId!}" />
                    </#if>
                    <input type="hidden" name='paymentMethodTypeId' value="${paymentMethodTypeId!}" />
                    <input type="hidden" name='cardType' value="${cardType!}" />
                    <input type="hidden" name='partyIdFrom' value="${partyIdFrom!}" />
                    <input type="hidden" name='fromDate' value="${fromDate!}" />
                    <input type="hidden" name='thruDate' value="${thruDate!}" />
                </div>
                </div>
                <div id="createPaymentBatch" style="display: none;" class="align-float">
                    <label for="paymentGroupName">${uiLabelMap.AccountingPaymentGroupName}</label>
                    <input type="text" size='25' id="paymentGroupName" name='paymentGroupName' />
                    <#if finAccounts?has_content>
                        <label for="finAccountId">${uiLabelMap.AccountingBankAccount}</label>
                        <select name="finAccountId" id="finAccountId">
                            <#list finAccounts as finAccount>
                              <#if ("FNACT_MANFROZEN" != finAccount.statusId) && ("FNACT_CANCELLED" != finAccount.statusId)>
                                <option value="${finAccount.get("finAccountId")}">${finAccount.get("finAccountName")} [${finAccount.get("finAccountId")}]</option>
                              </#if>
                            </#list>
                        </select>
                    </#if>
                </div>
                <table class="basic-table hover-bar">
                  <thead>
                    <tr class="header-row-2">
                      <th>${uiLabelMap.FormFieldTitle_paymentId}</th>
                      <th>${uiLabelMap.AccountingPaymentType}</th>
                      <th>${uiLabelMap.CommonStatus}</th>
                      <th>${uiLabelMap.CommonComments}</th>
                      <th>${uiLabelMap.AccountingFromParty}</th>
                      <th>${uiLabelMap.AccountingToParty}</th>
                      <th>${uiLabelMap.AccountingEffectiveDate}</th>
                      <th>${uiLabelMap.AccountingAmount}</th>
                      <th>${uiLabelMap.FormFieldTitle_amountToApply}</th>
                      <th>${uiLabelMap.CommonPaymentMethodType}</th>
                      <th>
                        <label>
                          ${uiLabelMap.CommonSelectAll}
                          <input type="checkbox" id="checkAllPayments" name="checkAllPayments" onchange="javascript:togglePaymentId(this);"/>
                        </label>
                      </th>
                    </tr>
                  </thead>
                  <tbody>
                    <#assign alt_row = false>
                    <#list paymentList as payment>
                      <tr <#if alt_row> class="alternate-row"</#if>>
                        <td><a href="<@ofbizUrl>paymentOverview?paymentId=${payment.paymentId}</@ofbizUrl>" class="buttontext">${payment.paymentId}</a></td>
                        <td>
                          ${payment.paymentTypeDesc?default(payment.paymentTypeId)}
                        </td>
                        <td>
                          ${payment.statusDesc?default(payment.statusId)}
                        </td>
                        <td>${(payment.comments)!}</td>
                        <td>
                          <a href="/partymgr/control/viewprofile?partyId=${payment.partyIdFrom}">${(payment.partyFromFirstName)!} ${(payment.partyFromLastName)!} ${(payment.partyFromGroupName)!}[${(payment.partyIdFrom)!}]</a>
                        </td>
                        <td>
                          <a href="/partymgr/control/viewprofile?partyId=${payment.partyIdTo}">${(payment.partyToFirstName)!} ${(payment.partyToLastName)!} ${(payment.partyToGroupName)!}[${(payment.partyIdTo)!}]</a>
                        </td>
                        <td>${payment.effectiveDate!}</td>
                        <td><@ofbizCurrency amount = payment.amount isoCode = payment.currencyUomId /></td>
                        <td>
                          <#assign amountToApply = Static["org.apache.ofbiz.accounting.payment.PaymentWorker"].getPaymentNotApplied(payment) />
                          <@ofbizCurrency amount = amountToApply isoCode = amountToApply.currencyUomId />
                        </td>
                        <td>
                          <#assign creditCard = (delegator.findOne("CreditCard", {"paymentMethodId" : payment.paymentMethodId}, false))! />
                          ${payment.paymentMethodTypeDesc?default(payment.paymentMethodTypeId)}
                          <#if creditCard?has_content>/${(creditCard.cardType)!}</#if>
                        </td>
                        <td>
                          <input type="checkbox" id="paymentId_${payment_index}" name="paymentIds" value="${payment.paymentId}" onclick="javascript:getPaymentRunningTotal('paymentId_${payment_index}');" />
                        </td>
                      </tr>
                        <#assign alt_row = !alt_row>
                    </#list>
                  </tbody>
                </table>
            <#else>
                <h3>${uiLabelMap.CommonNoRecordFound}</h3>
            </#if>
        </form>
    </div>
</div>
