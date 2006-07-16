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

<div class="head1">${uiLabelMap.AccountingBillingAccountInvoices}</div>

<br/>
<table width="100%" border="0" cellpadding="0" cellspacing="0"> 
  <tr>
    <td><div class="tableheadtext">${uiLabelMap.AccountingInvoice} #</div></td>
    <td><div class="tableheadtext">${uiLabelMap.AccountingInvoiceDate}</div></td>
    <td><div class="tableheadtext">${uiLabelMap.AccountingDueDate}</div></td>
    <td align="right"><div class="tableheadtext">${uiLabelMap.CommonTotal}</div></td>
    <td>&nbsp;</td>
  </tr> 
  <tr><td colspan="5"><hr class="sepbar"></td></tr>
  <#list invoices as invoice>
  <tr>
    <td><div class="tabletext">${invoice.invoiceId?if_exists}</div></td>
    <td><div class="tabletext">${invoice.invoiceDate?if_exists}</div></td>
    <td><div class="tabletext">${invoice.dueDate?if_exists}</div></td>
    <td align="right"><div class="tabletext">${invoice.invoiceTotal}</div></td>
    <td align="right">
      <a href="<@ofbizUrl>invoiceOverview?invoiceId=${invoice.invoiceId}</@ofbizUrl>" class="buttontext">[${uiLabelMap.CommonEdit}]</a>
    </td>
  </tr>
  </#list>
</table>
