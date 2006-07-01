<#--
 *  Copyright (c) 2003-2005 The Open For Business Project - www.ofbiz.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a 
 *  copy of this software and associated documentation files (the "Software"), 
 *  to deal in the Software without restriction, including without limitation 
 *  the rights to use, copy, modify, merge, publish, distribute, sublicense, 
 *  and/or sell copies of the Software, and to permit persons to whom the 
 *  Software is furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included 
 *  in all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS 
 *  OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF 
 *  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. 
 *  IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY 
 *  CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT 
 *  OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR 
 *  THE USE OR OTHER DEALINGS IN THE SOFTWARE.
-->
<div class="tabletext">
<table>
  <tr>
     <td colspan="3"><h1 class="head1">${uiLabelMap.AccountingAgreements}</h1></td>
  </tr>
  
  <tr>
     <td colspan="3">  
<ul>
<li><a href="<@ofbizUrl>FindAgreement</@ofbizUrl>">${uiLabelMap.AccountingAgreementAvailable}</a></li>
</ul>
<br/>
     </td>
  </tr>
  <tr>
     <td colspan="3"><h1 class="head1">${uiLabelMap.AccountingBillingMenu}</h1></td>
  </tr>
  
  <tr>
     <td colspan="3">  
<ul>
<li><a href="<@ofbizUrl>FindBillingAccount</@ofbizUrl>">${uiLabelMap.CommonShow} ${uiLabelMap.AccountingCustomer} ${uiLabelMap.AccountingBillingAccount}</a></li>
</ul>
<br/>
     </td>
  </tr>
  
  <tr>
     <td colspan="3"><h1 class="head1">${uiLabelMap.AccountingFixedAssets}</h1></td>
  </tr>
  
  <tr>
     <td colspan="3">  
<ul>
<li><a href="<@ofbizUrl>ListFixedAssets</@ofbizUrl>">${uiLabelMap.AccountingShowAllFixedAssets}</a></li>
</ul>
<br/>
     </td>
  </tr>
  
  <tr>
     <td colspan="3"><h1 class="head1">${uiLabelMap.AccountingInvoicesMenu}</h1></td>
  </tr>
  
  <tr valign="top">
<td>
<ul>
<li><a href="<@ofbizUrl>findInvoices?noConditionFind=Y&lookupFlag=Y</@ofbizUrl>">${uiLabelMap.AccountingShowAllInvoices}</a></li>
</ul>
</td>

<td>
<ul>
<#list invoiceTypes as invoiceType>
<li><a href="<@ofbizUrl>findInvoices?lookupFlag=Y&invoiceTypeId=${invoiceType.invoiceTypeId}</@ofbizUrl>">${uiLabelMap.AccountingShowInvoices} ${invoiceType.get("description",locale)}</a></li>
</#list>
</ul>
</td>
<td>
<ul>
<#list invoiceStatus as status>
<li><a href="<@ofbizUrl>findInvoices?lookupFlag=Y&statusId=${status.statusId}</@ofbizUrl>">${uiLabelMap.AccountingShowInvoices} ${status.get("description",locale)}</a></li>
</#list>
</ul>
</td>
</tr>

  <tr>
     <td colspan="3"><h1 class="head1">${uiLabelMap.AccountingPaymentsMenu}</h1></td>
  </tr>

<tr valign="top">
<td>
<ul>
<li><a href="<@ofbizUrl>findPayments?noConditionFind=Y&lookupFlag=Y</@ofbizUrl>">${uiLabelMap.AccountingShowAllPayments}</a></li>
</ul>
</td>
<td>

<ul>
<#list paymentTypes as paymentType>
<li><a href="<@ofbizUrl>findPayments?lookupFlag=Y&paymentTypeId=${paymentType.paymentTypeId}</@ofbizUrl>">${uiLabelMap.AccountingShowPayments} ${paymentType.get("description",locale)}</a></li>
</#list>
</ul>
</td>


<td>
<ul>
<#list paymentMethodTypes as paymentMethodType>
<li><a href="<@ofbizUrl>findPayments?lookupFlag=Y&paymentMethodTypeId=${paymentMethodType.paymentMethodTypeId}</@ofbizUrl>">${uiLabelMap.AccountingShowPayments} ${paymentMethodType.get("description",locale)}</a></li>
</#list>
</ul>
</td>

<td>
<ul>
<#list paymentStatus as status>
<li><a href="<@ofbizUrl>findPayments?lookupFlag=Y&statusId=${status.statusId}</@ofbizUrl>">${uiLabelMap.AccountingShowPayments} ${status.get("description",locale)}</a></li>
</#list>
</ul>
</td>
</tr>
</table>

<p><b>${uiLabelMap.AccountingMainPageNote1}</b><br/>
${uiLabelMap.AccountingMainPageNote2} 
<a href="http://www.opensourcestrategies.com/ofbiz/accounting.php">${uiLabelMap.AccountingMainPageNote3}</a> ${uiLabelMap.AccountingMainPageNote4}</p>
</div>
