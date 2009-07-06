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
<div class="screenlet">
    <div class="screenlet-body">
        <form method="post" action="<@ofbizUrl>createPaymentBatch</@ofbizUrl>" name='paymentBatchForm'>
            <input type="hidden" name="_useRowSubmit" value="Y">
            <#if paymentList?has_content>
                <div>
                    <span class="label">${uiLabelMap.AccountingPayment} ${uiLabelMap.PartyPartyGroupName}</span>
                    <input type="text" size='25' name='paymentGroupName'>
                    <input type="hidden" name='organizationPartyId' value="${organizationPartyId?if_exists}">
                </div>
                <table class="basic-table">
                    <tr class="header-row">
                        <td>${uiLabelMap.FormFieldTitle_paymentId}</td>
                        <td>${uiLabelMap.Party}</td>
                        <td>${uiLabelMap.CommonAmount}</td>
                        <td>${uiLabelMap.CommonDate}</td>
                        <td align="right">
                            ${uiLabelMap.ProductSelectAll}&nbsp;
                            <input id="selectAll" type="checkbox" name="selectAll" value="Y" onclick="javascript:toggleAll(this, 'paymentBatchForm');"/>
                        </td>
                    </tr>
                    <#list paymentList as payment>
                        <input type="hidden" name="paymentId_o_${payment_index}" value="${payment.paymentId}"/>
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
                                <input type="checkbox" name="_rowSubmit_o_${payment_index}" value="Y" onclick="javascript:checkToggle(this, 'paymentBatchForm');">
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