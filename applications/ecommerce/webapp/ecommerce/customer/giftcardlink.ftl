<#--
 *  Copyright (c) 2003 The Open For Business Project - www.ofbiz.org
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

<div class="head1">${uiLabelMap.AccountingGiftCardLink}</div>
<br/>
<div class="tabletext">${uiLabelMap.AccountingEnterGiftCardLink}.</div>
<br/>

<form name="gclink" method="post" action="<@ofbizUrl>linkgiftcard</@ofbizUrl>">
  <input type="hidden" name="paymentConfig" value="${paymentProperties?default("payment.properties")}">
  <#if userLogin?has_content>
    <input type="hidden" name="partyId" value="${userLogin.partyId}">
  </#if>
  <table align="center">
    <tr>
      <td colspan="2" align="center">
        <div class="tableheadtext">${uiLabelMap.AccountingPhysicalCard}</div>
      </td>
    </tr>
    <tr>
      <td><div class="tabletext">${uiLabelMap.AccountingCardNumber}</div></td>
      <td><input type="text" class="inputBox" name="physicalCard" size="20"></td>
    </tr>
    <tr>
      <td><div class="tabletext">${uiLabelMap.AccountingPINNumber}</div></td>
      <td><input type="text" class="inputBox" name="physicalPin" size="20"></td>
    </tr>
    <tr>
      <td colspan="2">&nbsp;</td>
    </tr>
    <tr>
      <td colspan="2" align="center">
        <div class="tableheadtext">${uiLabelMap.AccountingVirtualCard}</div>
      </td>
    </tr>
    <tr>
      <td><div class="tabletext">${uiLabelMap.AccountingCardNumber}</div></td>
      <td><input type="text" class="inputBox" name="virtualCard" size="20"></td>
    </tr>
    <tr>
      <td><div class="tabletext">${uiLabelMap.AccountingPINNumber}</div></td>
      <td><input type="text" class="inputBox" name="virtualPin" size="20"></td>
    </tr>
    <tr>
      <td colspan="2">&nbsp;</td>
    </tr>
    <tr>
      <td colspan="2" align="center"><input type="submit" class="smallSubmit" value="${uiLabelMap.EcommerceLinkCards}"></td>
    </tr>
  </table>
</form>
<br/>
