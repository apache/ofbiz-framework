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

<div style="border-bottom: 1px solid #ccc; margin-bottom: 20px">
    <p>
        <b>Account Number:</b> <a href="/accounting/control/EditFinAccount?finAccountId=${ownedFinAccount.finAccountId}${externalKeyParam}"
             class="smallSubmit">${ownedFinAccount.finAccountId}</a>
        <b>Type:</b> ${(ownedFinAccountType.description)?default('N/A')}
        <b>Name:</b> ${ownedFinAccount.finAccountName?if_exists}
    </p>
    <p>
        <b>Currency:</b> ${(accountCurrencyUom.description)?if_exists} [${ownedFinAccount.currencyUomId?if_exists}]
        <b>Date Opened:</b> ${ownedFinAccount.fromDate?if_exists}
        <b>Status:</b> ${(finAccountStatusItem.description)?default("Active")}
        <#if ownedFinAccount.replenishLevel?exists>
            <b>Replenish Level:</b> <@ofbizCurrency amount=ownedFinAccount.replenishLevel isoCode=ownedFinAccount.currencyUomId/>
        </#if>
    </p>
    <br />
    <table id="fa-transactions" class="basic-table" cellspacing="0" cellpadding="2">
        <thead>
            <tr class="header-row">
                <td>Transaction ${uiLabelMap.CommonDate}</td>
                <td>ID</td>
                <td>Order Item</td>
                <td>Payment</td>
                <td>Type</td>
                <td>Amount</td>
            </tr>
        </thead>
        <tbody>
            <#list ownedFinAccountTransList as ownedFinAccountTrans>
                <#assign finAccountTransType = ownedFinAccountTrans.getRelatedOne('FinAccountTransType')>
                <#assign displayAmount = ownedFinAccountTrans.amount>
                <#if ownedFinAccountTrans.finAccountTransTypeId == 'WITHDRAWAL'>
                    <#assign displayAmount = -displayAmount>
                </#if>
                <tr>
                    <td>${ownedFinAccountTrans.transactionDate?if_exists}</td>
                    <td>${ownedFinAccountTrans.finAccountTransId}</td>
                    <td>${ownedFinAccountTrans.orderId?if_exists}:${ownedFinAccountTrans.orderItemSeqId?if_exists}</td>
                    <td>${ownedFinAccountTrans.paymentId?if_exists}</td>
                    <td>${finAccountTransType.description?default(ownedFinAccountTrans.finAccountTransTypeId)?if_exists}</td>
                    <td><@ofbizCurrency amount=displayAmount isoCode=ownedFinAccount.currencyUomId/></td>
                </tr>
            </#list>
        </tbody>
        <tfoot>
            <tr><td colspan="6"><hr /></td></tr>
            <tr>
                <td colspan="5"><b>Actual Balance</b></td>
                <td><b><@ofbizCurrency amount=ownedFinAccount.actualBalance isoCode=ownedFinAccount.currencyUomId/></b></td>
            </tr>
        </tfoot>
    </table>
</div>

<#if ownedFinAccountAuthList?has_content>
    <div style="border-bottom: 1px solid #ccc; margin-bottom: 20px">
        <table id="fa-authorizations" class="basic-table" cellspacing="0" cellpadding="2">
            <thead>
                <tr class="header-row">
                    <td>Authorization ${uiLabelMap.CommonDate}</td>
                    <td>ID</td>
                    <td>Expires</td>
                    <td>Amount</td>
                </tr>
            </thead>
            <tbody>
                <#list ownedFinAccountAuthList as ownedFinAccountAuth>
                    <tr>
                        <td>${ownedFinAccountAuth.authorizationDate?if_exists}</td>
                        <td>${ownedFinAccountAuth.finAccountAuthId}</td>
                        <td>${ownedFinAccountAuth.thruDate?if_exists}</td>
                        <td><@ofbizCurrency amount=-ownedFinAccountAuth.amount isoCode=ownedFinAccount.currencyUomId/></td>
                    </tr>
                </#list>
            </tbody>
            <tfoot>
                <tr><td colspan="4"><hr /></td></tr>
                <tr>
                    <td colspan="3"><b>Actual Balance</b></td>
                    <td><b><@ofbizCurrency amount=ownedFinAccount.actualBalance isoCode=ownedFinAccount.currencyUomId/></b></td>
                </tr>
                <tr>
                    <td colspan="3"><b>Available Balance</b></td>
                    <td><b><@ofbizCurrency amount=ownedFinAccount.availableBalance isoCode=ownedFinAccount.currencyUomId/></b></td>
                </tr>
            </tfoot>
        </table>
    </div>
</#if>

