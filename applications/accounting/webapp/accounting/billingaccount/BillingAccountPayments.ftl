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

<h1>${uiLabelMap.AccountingBillingAccountPayments}</h1>
<br/>
<table class="basic-table" cellspacing="0"> 
    <tr class="header-row">
        <td>${uiLabelMap.AccountingPayment} #</td>
        <td>${uiLabelMap.CommonType}</td>
        <td>${uiLabelMap.AccountingInvoice} #</td>
        <td>${uiLabelMap.AccountingInvoiceItem}</td>
        <td>${uiLabelMap.AccountingPaymentDate}</td>
        <td class="align-text">${uiLabelMap.AccountingAmount}</td>
    </tr> 
    <#list payments as payment>
        <#assign paymentMethodType = payment.getRelatedOne("PaymentMethodType")>
        <tr>
            <td>${payment.paymentId?if_exists}</td>
            <td>${paymentMethodType.get("description",locale)?default(uiLabelMap.CommonNA)}</td>  
            <td>${payment.invoiceId?default(uiLabelMap.CommonNA)}</td>
            <td>${payment.invoiceItemSeqId?default(uiLabelMap.CommonNA)}</td>
            <td>${payment.effectiveDate?string}</td>
            <td class="align-text"><@ofbizCurrency amount=payment.amountApplied isoCode=payment.currencyUomId?if_exists/> of <@ofbizCurrency amount=payment.amount isoCode=payment.currencyUomId?if_exists/></td>
        </tr>
    </#list>
</table>
