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
function togglePaymentId(master) {
    var form = document.depositWithdrawPaymentsForm;
    var payments = form.elements.length;
    for (var i = 0; i < payments; i++) {
        var element = form.elements[i];
        if ("paymentIds" == element.name) {
            element.checked = master.checked;
        }
    }
    getPaymentRunningTotal();
}
function getPaymentRunningTotal() {
    var form = document.depositWithdrawPaymentsForm;
    var payments = form.elements.length;
    var isSingle = true;
    var isAllSelected = true;
    for (var i = 0; i < payments; i++) {
        var element = form.elements[i];
        if ("paymentIds" == element.name) {
            if (element.checked) {
                isSingle = false;
            } else {
                isAllSelected = false;
            }
        }
    }

    if (isAllSelected) {
        jQuery('#checkAllPayments').attr('checked', true);
    } else {
        jQuery('#checkAllPayments').attr('checked', false);
    }
    if (!isSingle) {
        jQuery('#submitButton').removeAttr('disabled');
        jQuery.ajax({
            url: 'getPaymentRunningTotal',
            async: false,
            type: 'POST',
            data: jQuery('#depositWithdrawPaymentsForm').serialize(),
            success: function(data) {
                jQuery('#showPaymentRunningTotal').html(data.paymentRunningTotal);
            }
        });
    } else {
        jQuery('#showPaymentRunningTotal').html("");
        jQuery('#submitButton').attr('disabled', true);
    }
}


</script>
<div class="screenlet">
    <div class="screenlet-body">
        <form id="depositWithdrawPaymentsForm" name="depositWithdrawPaymentsForm" method="post" action="<@ofbizUrl>depositWithdrawPayments</@ofbizUrl>">
            <#if paymentList?has_content>
                <input type="hidden" name='organizationPartyId' value="${organizationPartyId!}" />
                <input type="hidden" name='finAccountId' value="${finAccountId!}" />
                <input type="hidden" name='paymentMethodTypeId' value="${paymentMethodTypeId!}" />
                <input type="hidden" name='cardType' value="${cardType!}" />
                <input type="hidden" name='partyIdFrom' value="${partyIdFrom!}" />
                <input type="hidden" name='fromDate' value="${fromDate!}" />
                <input type="hidden" name='thruDate' value="${thruDate!}" />
                <input type="hidden" name='paymentGroupTypeId' value="BATCH_PAYMENT" />
                <div>
                    <span class="label">${uiLabelMap.AccountingRunningTotal} :</span>
                    <span class="label" id="showPaymentRunningTotal"></span>
                </div>
                <table class="basic-table">
                    <tr class="header-row-2">
                        <td>${uiLabelMap.FormFieldTitle_paymentId}</td>
                        <td>${uiLabelMap.AccountingPaymentType}</td>
                        <td>${uiLabelMap.AccountingFromParty}</td>
                        <td>${uiLabelMap.AccountingToParty}</td>
                        <td>${uiLabelMap.CommonAmount}</td>
                        <td>${uiLabelMap.CommonDate}</td>
                        <td align="right"><label>${uiLabelMap.CommonSelectAll}<input type="checkbox" id="checkAllPayments" name="checkAllPayments" onchange="javascript:togglePaymentId(this);"/></label></td>
                    </tr>
                    <#assign alt_row = false>
                    <#list paymentList as payment>
                        <tr <#if alt_row> class="alternate-row"</#if>>
                            <td><a href="<@ofbizUrl>paymentOverview?paymentId=${payment.paymentId}</@ofbizUrl>">${payment.paymentId}</a></td>
                            <td>${payment.paymentTypeDesc!}</td>
                            <td>${(payment.partyFromFirstName)!} ${(payment.partyFromLastName)!} ${(payment.partyFromGroupName)!}</td>
                            <td>${(payment.partyToFirstName)!} ${(payment.partyToLastName)!} ${(payment.partyToGroupName)!}</td>
                            <td><@ofbizCurrency amount=payment.amount isoCode=payment.currencyUomId/></td>
                            <td>${payment.effectiveDate!}</td>
                            <td align="right"><label>${uiLabelMap.AccountingDeposit}<input type="checkbox" id="paymentId_${payment_index}" name="paymentIds" value="${payment.paymentId}" onclick="javascript:getPaymentRunningTotal();"/></label></td>
                        </tr>
                        <#assign alt_row = !alt_row>
                    </#list>
                    <div align="right">
                        <span class="label">${uiLabelMap.AccountingPayment} ${uiLabelMap.PartyPartyGroupName}</span>
                        <input type="text" size='25' id="paymentGroupName" name='paymentGroupName' />
                        <label><span class="label">${uiLabelMap.AccountingGroupInOneTransaction}</span>
                        <input type="checkbox" name="groupInOneTransaction" value="Y" checked="checked" /></label>
                        <input id="submitButton" type="button"  onclick="javascript:document.depositWithdrawPaymentsForm.submit();" value="${uiLabelMap.AccountingDepositWithdraw}" disabled="disabled"/>
                    </div>
                </table>
            <#else>
                <span class="label">${uiLabelMap.CommonNoRecordFound}</span>
            </#if>
        </form>
    </div>
</div>
