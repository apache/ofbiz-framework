<#--
Copyright 2001-2006 The Apache Software Foundation

Licensed under the Apache License, Version 2.0 (the "License"); you may not
use this file except in compliance with the License. You may obtain a copy of
the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
License for the specific language governing permissions and limitations
under the License.
-->

<div class="head1">${uiLabelMap.AccountingBillingAccountPayments}</div>

<br/>
<table width="100%" border="0" cellpadding="0" cellspacing="0"> 
    <tr>
        <td><div class="tableheadtext">${uiLabelMap.AccountingPayment} #</div></td>
        <td><div class="tableheadtext">${uiLabelMap.CommonType}</div></td>  
        <td><div class="tableheadtext">${uiLabelMap.AccountingInvoice} #</div></td>
        <td><div class="tableheadtext">${uiLabelMap.AccountingInvoiceItem}</div></td>
        <td><div class="tableheadtext">${uiLabelMap.AccountingPaymentDate}</div></td>
        <td align="right"><div class="tableheadtext">${uiLabelMap.AccountingAmount}</div></td>
    </tr> 
    <tr><td colspan="6"><hr class="sepbar"></td></tr>
    <#list payments as payment>
        <#assign paymentMethodType = payment.getRelatedOne("PaymentMethodType")>
        <tr>
            <td><div class="tabletext">${payment.paymentId?if_exists}</div></td>
            <td><div class="tabletext">${paymentMethodType.get("description",locale)?default(uiLabelMap.CommonNA)}</div></td>  
            <td><div class="tabletext">${payment.invoiceId?default(uiLabelMap.CommonNA)}</div></td>
            <td><div class="tabletext">${payment.invoiceItemSeqId?default(uiLabelMap.CommonNA)}</div></td>
            <td><div class="tabletext">${payment.effectiveDate?string}</div></td>
            <td align="right"><div class="tabletext"><@ofbizCurrency amount=payment.amountApplied isoCode=payment.currencyUomId?if_exists/> of <@ofbizCurrency amount=payment.amount isoCode=payment.currencyUomId?if_exists/></div></td>
        </tr>
    </#list>
</table>
