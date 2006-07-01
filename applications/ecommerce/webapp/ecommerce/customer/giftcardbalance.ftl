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
 *
 *@author     Andy Zeneski (jaz@ofbiz.org)
 *@version    $Rev$
 *@since      3.0
-->

<div class="head1">${uiLabelMap.AccountingGiftCardBalance}</div>
<br/>
<div class="tabletext">${uiLabelMap.AccountingEnterGiftCardNumber}</div>
<br/>

<br/>
<table align="center">
  <#if requestAttributes.processResult?exists>
    <tr>
      <td colspan="2">
        <div align="center" class="tabletext">
          ${uiLabelMap.AccountingCurrentBalance}
        </div>
      </td>
    </tr>
    <tr>
      <td colspan="2">
        <div class="graybox">
          <#if (requestAttributes.balance?default(0) > 0)>
            ${requestAttributes.balance}
          <#else>
            ${uiLabelMap.AccountingCurrentBalanceProblem}
          </#if>
        </div>
      </td>
    </tr>
    <tr><td colspan="2">&nbsp;</td></tr>
  </#if>
  <form method="post" action="<@ofbizUrl>querygcbalance</@ofbizUrl>">
    <input type="hidden" name="currency" value="USD">
    <input type="hidden" name="paymentConfig" value="${paymentProperties?default("payment.properties")}">
    <tr>
      <td><div class="tableheadtext">${uiLabelMap.AccountingCardNumber}</div></td>
      <td><input type="text" class="inputBox" name="cardNumber" size="20" value="${(requestParameters.cardNumber)?if_exists}"></td>
    </tr>
    <tr>
      <td><div class="tableheadtext">${uiLabelMap.AccountingPINNumber}</div></td>
      <td><input type="text" class="inputBox" name="pin" size="15" value="${(requestParameters.pin)?if_exists}"></td>
    </tr>
    <tr><td colspan="2">&nbsp;</td></tr>
    <tr>
      <td colspan="2" align="center"><input type="submit" class="smallSubmit" value="${uiLabelMap.EcommerceCheckBalance}"></td>
    </tr>
  </form>
</table>
<br/>
