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

<script type="text/javascript">
//<![CDATA[
function togglePaymentId(master) {
    var payments = $('paymentBatchForm').getInputs('checkbox','paymentIds');
    payments.each(function(payment){
        payment.checked = master.checked;
    });
    getPaymentRunningTotal();
}
function getPaymentRunningTotal() {
    var payments = $('paymentBatchForm').getInputs('checkbox','paymentIds');
    if(payments.pluck('checked').all()) {
        $('checkAllPayments').checked = true;
    } else {
        $('checkAllPayments').checked = false;
    }

    if(payments.pluck('checked').any()) {
        new Ajax.Request('getPaymentRunningTotal', {
            asynchronous: false,
            onSuccess: function(transport) {
                var data = transport.responseText.evalJSON(true);
                $('showPaymentRunningTotal').update(data.paymentRunningTotal);
            }, 
            parameters: $('paymentBatchForm').serialize(), 
            requestHeaders: {Accept: 'application/json'}
        });
        if($F('serviceName') != "") {
            $('submitButton').disabled = false;
        }
        
    } else {
        $('submitButton').disabled = true;
        $('showPaymentRunningTotal').update("");
    }
}
function setServiceName(selection) {
    if (selection.value == 'massPaymentsToNotPaid' || selection.value == 'massPaymentsToReceived' || selection.value == 'massPaymentsToConfirmed' || selection.value == 'massPaymentsToCancelled' || selection.value == 'massPaymentsToVoid') {
        $('paymentBatchForm').action = $('paymentStatusChange').value;
    }
    else {
        $('paymentBatchForm').action = selection.value;
    }
    if (selection.value == 'massPaymentsToNotPaid') {
        $('statusId').value = "PMNT_NOT_PAID";
    } else if (selection.value == 'massPaymentsToReceived') {
        $('statusId').value = "PMNT_RECEIVED";
    }else if (selection.value == 'massPaymentsToConfirmed') {
        $('statusId').value = "PMNT_CONFIRMED";
    }else if (selection.value == 'massPaymentsToCancelled') {
        $('statusId').value = "PMNT_CANCELLED";
    }else if (selection.value == 'massPaymentsToVoid') {
        $('statusId').value = "PMNT_VOID";
    }
    if ($('processBatchPayment').selected) {
        Effect.BlindDown('createPaymentBatch');
    } else {
        Effect.BlindUp('createPaymentBatch');
    }
    if($('paymentBatchForm').getInputs('checkbox','paymentIds').pluck('checked').any() && ($F('serviceName') != "")) {
            $('submitButton').disabled = false;
    } else {
        $('submitButton').disabled = true;
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
                        <option value="">${uiLabelMap.AccountingSelectAction}</options>
                        <option value="<@ofbizUrl>createPaymentBatch</@ofbizUrl>" id="processBatchPayment">${uiLabelMap.AccountingCreateBatch}</option>
                        <option value="massPaymentsToNotPaid">${uiLabelMap.AccountingPaymentStatusToNotPaid}</option>
                        <option value="massPaymentsToReceived">${uiLabelMap.AccountingInvoiceStatusToReceived}</option>
                        <option value="massPaymentsToConfirmed">${uiLabelMap.AccountingPaymentTabStatusToConfirmed}</option>
                        <option value="massPaymentsToCancelled">${uiLabelMap.AccountingPaymentTabStatusToCancelled}</option>
                        <option value="massPaymentsToVoid">${uiLabelMap.AccountingPaymentTabStatusToVoid}</option>
                    </select>
                    <input id="submitButton" type="button" onclick="javascript:$('paymentBatchForm').submit();" value="${uiLabelMap.CommonRun}" disabled="disabled" />
                    <input type="hidden" name='organizationPartyId' value="${organizationPartyId?if_exists}" />
                    <input type="hidden" name='paymentGroupTypeId' value="BATCH_PAYMENT" />
                    <input type="hidden" name="groupInOneTransaction" value="Y" />
                    <input type="hidden" name="paymentStatusChange" id="paymentStatusChange" value="<@ofbizUrl>massChangePaymentStatus</@ofbizUrl>" />
                    <input type="hidden" name="statusId" id="statusId" value="${parameters.statusId?if_exists}" />
                    <#if finAccountId?has_content>
                        <input type="hidden" name='finAccountId' value="${finAccountId?if_exists}" />
                    </#if>
                    <input type="hidden" name='paymentMethodTypeId' value="${paymentMethodTypeId?if_exists}" />
                    <input type="hidden" name='cardType' value="${cardType?if_exists}" />
                    <input type="hidden" name='partyIdFrom' value="${partyIdFrom?if_exists}" />
                    <input type="hidden" name='fromDate' value="${fromDate?if_exists}" />
                    <input type="hidden" name='thruDate' value="${thruDate?if_exists}" />
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
                        ${uiLabelMap.CommonSelectAll}
                        <input type="checkbox" id="checkAllPayments" name="checkAllPayments" onchange="javascript:togglePaymentId(this);"/>
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
                        <td>${(payment.comments)?if_exists}</td>
                        <td>
                          <a href="/partymgr/control/viewprofile?partyId=${payment.partyIdFrom}">${(payment.partyFromFirstName)?if_exists} ${(payment.partyFromLastName)?if_exists} ${(payment.partyFromGroupName)?if_exists}[${(payment.partyIdFrom)?if_exists}]</a>
                        </td>
                        <td>
                          <a href="/partymgr/control/viewprofile?partyId=${payment.partyIdTo}">${(payment.partyToFirstName)?if_exists} ${(payment.partyToLastName)?if_exists} ${(payment.partyToGroupName)?if_exists}[${(payment.partyIdTo)?if_exists}]</a>
                        </td>
                        <td>${payment.effectiveDate?if_exists}</td>
                        <td><@ofbizCurrency amount = payment.amount isoCode = payment.currencyUomId /></td>
                        <td>
                          <#assign amountToApply = Static["org.ofbiz.accounting.payment.PaymentWorker"].getPaymentNotApplied(payment) />
                          <@ofbizCurrency amount = amountToApply isoCode = amountToApply.currencyUomId />
                        </td>
                        <td>
                          <#assign creditCard = (delegator.findOne("CreditCard", {"paymentMethodId" : payment.paymentMethodId}, false))?if_exists />
                          ${payment.paymentMethodTypeDesc?default(payment.paymentMethodTypeId)}
                          <#if creditCard?has_content>/${(creditCard.cardType)?if_exists}</#if>
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
