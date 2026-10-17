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
        <b>${uiLabelMap.AccountingAccountNumber}:</b> <a href="<@ofbizUrl controlPath="/accounting/control">EditFinAccount?finAccountId=${ownedFinAccount.finAccountId}</@ofbizUrl>"
             class="smallSubmit">${ownedFinAccount.finAccountId}</a>
        <b>${uiLabelMap.AccountingAccountType}:</b> ${(ownedFinAccountType.description)?default('N/A')}
        <b>${uiLabelMap.FormFieldTitle_finAccountName}:</b> ${ownedFinAccount.finAccountName!}
    </p>
    <p>
        <b>${uiLabelMap.AccountingCurrency}:</b> ${(accountCurrencyUom.description)!} [${ownedFinAccount.currencyUomId!}]
        <b>${uiLabelMap.AccountingDateOpened}:</b> ${ownedFinAccount.fromDate!}
        <b>${uiLabelMap.CommonStatus}:</b> ${(finAccountStatusItem.description)?default("Active")}
        <#if ownedFinAccount.replenishLevel??>
            <b>${uiLabelMap.FormFieldTitle_replenishLevel}:</b> <@ofbizCurrency amount=ownedFinAccount.replenishLevel isoCode=ownedFinAccount.currencyUomId/>
        </#if>
    </p>
    <br />
    <table id="fa-transactions" class="basic-table" cellspacing="0" cellpadding="2">
        <thead>
            <tr class="header-row">
                <td>${uiLabelMap.FormFieldTitle_transactionDate}</td>
                <td>${uiLabelMap.CommonId}</td>
                <td>${uiLabelMap.OrderItem}</td>
                <td>${uiLabelMap.AccountingPayment}</td>
                <td>${uiLabelMap.AccountingType}</td>
                <td>${uiLabelMap.AccountingAmount}</td>
            </tr>
        </thead>
        <tbody>
            <#list ownedFinAccountTransList as ownedFinAccountTrans>
                <#assign finAccountTransType = ownedFinAccountTrans.getRelatedOne('FinAccountTransType', false)>
                <#assign displayAmount = ownedFinAccountTrans.amount>
                <#if ownedFinAccountTrans.finAccountTransTypeId == 'WITHDRAWAL'>
                    <#assign displayAmount = -displayAmount>
                </#if>
                <tr>
                    <td>${ownedFinAccountTrans.transactionDate!}</td>
                    <td>${ownedFinAccountTrans.finAccountTransId}</td>
                    <td>${ownedFinAccountTrans.orderId!}:${ownedFinAccountTrans.orderItemSeqId!}</td>
                    <td>${ownedFinAccountTrans.paymentId!}</td>
                    <td>${finAccountTransType.description?default(ownedFinAccountTrans.finAccountTransTypeId)!}</td>
                    <td><@ofbizCurrency amount=displayAmount isoCode=ownedFinAccount.currencyUomId/></td>
                </tr>
            </#list>
        </tbody>
        <tfoot>
            <tr><td colspan="6"><hr /></td></tr>
            <tr>
                <td colspan="5"><b>${uiLabelMap.FormFieldTitle_actualBalance}</b></td>
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
                    <td>${uiLabelMap.FormFieldTitle_authorizationDate}</td>
                    <td>${uiLabelMap.CommonId}</td>
                    <td>${uiLabelMap.CommonExpires}</td>
                    <td>${uiLabelMap.AccountingAmount}</td>
                </tr>
            </thead>
            <tbody>
                <#list ownedFinAccountAuthList as ownedFinAccountAuth>
                    <tr>
                        <td>${ownedFinAccountAuth.authorizationDate!}</td>
                        <td>${ownedFinAccountAuth.finAccountAuthId}</td>
                        <td>${ownedFinAccountAuth.thruDate!}</td>
                        <td><@ofbizCurrency amount=-ownedFinAccountAuth.amount isoCode=ownedFinAccount.currencyUomId/></td>
                    </tr>
                </#list>
            </tbody>
            <tfoot>
                <tr><td colspan="4"><hr /></td></tr>
                <tr>
                    <td colspan="3"><b>${uiLabelMap.FormFieldTitle_actualBalance}</b></td>
                    <td><b><@ofbizCurrency amount=ownedFinAccount.actualBalance isoCode=ownedFinAccount.currencyUomId/></b></td>
                </tr>
                <tr>
                    <td colspan="3"><b>${uiLabelMap.FormFieldTitle_availableBalance}</b></td>
                    <td><b><@ofbizCurrency amount=ownedFinAccount.availableBalance isoCode=ownedFinAccount.currencyUomId/></b></td>
                </tr>
            </tfoot>
        </table>
    </div>
</#if>

