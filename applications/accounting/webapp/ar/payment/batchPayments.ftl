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
    var form = document.paymentBatchForm;
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
    if (!($(e).checked)) {
        $('checkAllPayments').checked = false;
    }
    new Ajax.Request('getPaymentRunningTotal', {
        asynchronous: false,
        onSuccess: function(transport) {
            var data = transport.responseText.evalJSON(true);
            $('showPaymentRunningTotal').update(data.paymentRunningTotal);
        }, parameters: $('paymentBatchForm').serialize(), requestHeaders: {Accept: 'application/json'}
    });
}
// -->

</script>
<div class="screenlet">
    <div class="screenlet-body">
        <form id="paymentBatchForm" name="paymentBatchForm" method="post" action="<@ofbizUrl>createPaymentBatch</@ofbizUrl>">
            <#if paymentList?has_content>
                <div>
                    <span class="label">${uiLabelMap.AccountingPayment} ${uiLabelMap.PartyPartyGroupName}</span>
                    <input type="text" size='25' name='paymentGroupName'>
                    <input type="hidden" name='organizationPartyId' value="${organizationPartyId?if_exists}">
                </div>
                <div>
                    <span class="label">${uiLabelMap.AccountingRunningTotal} :</span>
                    <span class="label" id="showPaymentRunningTotal"></span>
                </div>
                <table class="basic-table">
                    <tr class="header-row">
                        <td>${uiLabelMap.FormFieldTitle_paymentId}</td>
                        <td>${uiLabelMap.Party}</td>
                        <td>${uiLabelMap.CommonAmount}</td>
                        <td>${uiLabelMap.CommonDate}</td>
                        <td align="right">
                            ${uiLabelMap.ProductSelectAll}&nbsp;
                            <input type="checkbox" id="checkAllPayments" name="checkAllPayments" onchange="javascript:togglePaymentId(this);"/>
                        </td>
                    </tr>
                    <#list paymentList as payment>
                        <tr>
                            <td><a href="<@ofbizUrl>paymentOverview?paymentId=${payment.paymentId}</@ofbizUrl>">${payment.paymentId}</a></td>
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
                                <input type="checkbox" id="paymentId_${payment_index}" name="paymentIds" value="${payment.paymentId}" onclick="javascript:getPaymentRunningTotal('paymentId_${payment_index}');"/>
                            </td>
                        </tr>
                    </#list>
                    <tr>
                        <td align="right">
                            <a href="javascript:document.paymentBatchForm.submit();" class="buttontext">${uiLabelMap.AccountingCreateBatch}</a>
                        </td>
                    </tr>
                </table>
            </#if>
        </form>
    </div>
</div>