<#--
 *  Copyright (c) 2003-2006 The Open For Business Project - www.ofbiz.org
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
 *
 *@author     Andy Zeneski (jaz@ofbiz.org)
 *@author     David E. Jones (jonesde@ofbiz.org)
 *@author     Olivier Heintz (olivier.heintz@nereide.biz)
 *@version    $Rev$
 *@since      2.1
-->

<div class="head1">${uiLabelMap.AccountingBillingAccounts}</div>
<div><a href="<@ofbizUrl>EditBillingAccount<#if (requestParameters.partyId)?has_content>?partyId=${requestParameters.partyId}&roleTypeId=BILL_TO_CUSTOMER</#if></@ofbizUrl>" class="buttontext">${uiLabelMap.AccountingNewAccount}</a></div>

<br/>
<table width="100%" border="0" cellpadding="0" cellspacing="0"> 
  <tr>
    <td><div class="tableheadtext">${uiLabelMap.AccountingAccountId}</div></td>
    <td><div class="tableheadtext">${uiLabelMap.AccountingAccountLimit}</div></td>
    <#if billingAccountRolesByParty?has_content>
      <#assign colSpan = "4">
      <td><div class="tableheadtext">${uiLabelMap.PartyRoleTypeId}</div></td>
    <#else>
      <#assign colSpan = "3">
    </#if>
    <td>&nbsp;</td>
  </tr>  
  <tr><td colspan="${colSpan}"><hr class="sepbar"></td></tr>    
  <#if billingAccountRolesByParty?has_content>
    <#list billingAccountRolesByParty as role>
      <#assign billingAccount = role.getRelatedOne("BillingAccount")>
      <#assign roleType = role.getRelatedOne("RoleType")>
      <tr>
        <td><div class="tabletext">${billingAccount.billingAccountId}</div></td>
        <td><div class="tabletext"><@ofbizCurrency amount=billingAccount.accountLimit isoCode=billingAccount.accountCurrencyUomId/></div></td>
        <td><div class="tabletext">${roleType.get("description",locale)}</div></td>
        <td align="right">
          <a href="<@ofbizUrl>EditBillingAccount?billingAccountId=${billingAccount.billingAccountId}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonEdit}</a>
        </td>
      </tr>
    </#list>
  <#elseif billingAccounts?has_content>
    <#list billingAccounts as billingAccount>
      <tr>
        <td><div class="tabletext">${billingAccount.billingAccountId}</div></td>
        <td><div class="tabletext"><@ofbizCurrency amount=billingAccount.accountLimit isoCode=billingAccount.accountCurrencyUomId/></div></td>
        <td align="right">
          <a href="<@ofbizUrl>EditBillingAccount?billingAccountId=${billingAccount.billingAccountId}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonEdit}</a>
        </td>        
      </tr>
    </#list>
  <#else>
    <tr>
      <td colspan='3'><div class="tabletext">${uiLabelMap.AccountingNoBillingAccountFound}</div></td>
    </tr>    
  </#if>
</table>
