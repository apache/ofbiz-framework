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
<!-- //
function togglePaymentId(master) {
    var form = document.depositWithdrawPaymentsForm;
    var payments = form.elements.length;
    for (var i = 0; i < payments; i++) {
        var element = form.elements[i];
        if (element.name == "paymentIds") {
            element.checked = master.checked;
        }
    }
    getPaymentRunningTotal(master);
}
function getPaymentRunningTotal(e) {
    var form = document.depositWithdrawPaymentsForm;
    var payments = form.elements.length;
    var isSingle = true;
    var isAllSelected = true;
    for (var i = 0; i < payments; i++) {
        var element = form.elements[i];
        if (element.name == "paymentIds") {
            if (element.checked) {
                isSingle = false;
            } else {
                isAllSelected = false;
            }
        }
    }
    if (!($(e).checked)) {
        $('checkAllPayments').checked = false;
    } else if (isAllSelected) {
        $('checkAllPayments').checked = true;
    }
    if (!isSingle) {
        $('submitButton').disabled = false;
        new Ajax.Request('getPaymentRunningTotal', {
            asynchronous: false,
            onSuccess: function(transport) {
                var data = transport.responseText.evalJSON(true);
                $('showPaymentRunningTotal').update(data.paymentRunningTotal);
            }, parameters: $('depositWithdrawPaymentsForm').serialize(), requestHeaders: {Accept: 'application/json'}
        });
    } else {
        $('showPaymentRunningTotal').update("");
        $('submitButton').disabled = true;
    }
}
// -->

</script>
<div class="screenlet">
    <div class="screenlet-body">
        <form id="depositWithdrawPaymentsForm" name="depositWithdrawPaymentsForm" method="post" action="<@ofbizUrl>depositWithdrawPayments</@ofbizUrl>">
            <#if paymentList?has_content>
                <input type="hidden" name='organizationPartyId' value="${organizationPartyId?if_exists}">
                <input type="hidden" name='finAccountId' value="${finAccountId?if_exists}">
                <input type="hidden" name='paymentMethodTypeId' value="${paymentMethodTypeId?if_exists}">
                <input type="hidden" name='cardType' value="${cardType?if_exists}">
                <input type="hidden" name='partyIdFrom' value="${partyIdFrom?if_exists}">
                <input type="hidden" name='fromDate' value="${fromDate?if_exists}">
                <input type="hidden" name='thruDate' value="${thruDate?if_exists}">
                <input type="hidden" name='paymentGroupTypeId' value="BATCH_PAYMENT">
                <div>
                    <span class="label">${uiLabelMap.AccountingRunningTotal} :</span>
                    <span class="label" id="showPaymentRunningTotal"></span>
                </div>
                <table class="basic-table">
                    <tr class="header-row">
                        <td>${uiLabelMap.FormFieldTitle_paymentId}</td>
                        <td>${uiLabelMap.AccountingPaymentType}</td>
                        <td>${uiLabelMap.Party}</td>
                        <td>${uiLabelMap.CommonAmount}</td>
                        <td>${uiLabelMap.CommonDate}</td>
                        <td align="right">
                            ${uiLabelMap.CommonSelectAll}&nbsp;
                            <input type="checkbox" id="checkAllPayments" name="checkAllPayments" onchange="javascript:togglePaymentId(this);"/>
                        </td>
                    </tr>
                    <#list paymentList as payment>
                        <#if payment.paymentTypeId?has_content>
                            <#assign paymentType = delegator.findOne("PaymentType", {"paymentTypeId" : payment.paymentTypeId}, false)/>
                        </#if>
                        <tr>
                            <td><a href="<@ofbizUrl>paymentOverview?paymentId=${payment.paymentId}</@ofbizUrl>">${payment.paymentId}</a></td>
                            <td>
                                ${paymentType.description?if_exists}
                            </td>
                            <td>
                                <#assign partyName = (delegator.findOne("PartyNameView", {"partyId" : payment.partyIdFrom}, false))!>
                                <#if partyName.partyTypeId == "PERSON">
                                    ${(partyName.firstName)!} ${(partyName.lastName)!}
                                <#elseif (partyName.partyTypeId)! == "PARTY_GROUP">
                                    ${(partyName.groupName)!}
                                </#if>
                            </td>
                            <td><@ofbizCurrency amount=payment.amount isoCode=payment.currencyUomId/></td>
                            <td>${payment.effectiveDate?if_exists}</td>
                            <td align="right">
                                ${uiLabelMap.AccountingDeposit}&nbsp;
                                <input type="checkbox" id="paymentId_${payment_index}" name="paymentIds" value="${payment.paymentId}" onclick="javascript:getPaymentRunningTotal('paymentId_${payment_index}');"/>
                            </td>
                        </tr>
                    </#list>
                    <div align="right">
                        ${uiLabelMap.AccountingGroupInOneTransaction}
                        <input type="checkbox" name="groupInOneTransaction" value="Y" checked/>
                        <input id="submitButton" type="button"  onclick="javascript:document.depositWithdrawPaymentsForm.submit();" value="${uiLabelMap.AccountingDepositWithdraw}" disabled/>
                    </div>
                </table>
            <#else>
                <span class="label">${uiLabelMap.AccountingNoRecordFound}</span>
            </#if>
        </form>
    </div>
</div>
