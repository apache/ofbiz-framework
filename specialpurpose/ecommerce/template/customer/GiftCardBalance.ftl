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

<h1>${uiLabelMap.AccountingGiftCardBalance}</h1>
<br />
<div>${uiLabelMap.AccountingEnterGiftCardNumber}</div>
<br />

<br />
<table align="center">
  <#if requestAttributes.processResult??>
    <tr>
      <td colspan="2">
        <div align="center">
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
    <input type="hidden" name="currency" value="USD" />
    <input type="hidden" name="paymentConfig" value="${paymentProperties?default("payment.properties")}" />
    <tr>
      <td><div class="tableheadtext">${uiLabelMap.AccountingCardNumber}</div></td>
      <td><input type="text" class="inputBox" name="cardNumber" size="20" value="${(requestParameters.cardNumber)!}" /></td>
    </tr>
    <tr>
      <td><div class="tableheadtext">${uiLabelMap.AccountingPINNumber}</div></td>
      <td><input type="text" class="inputBox" name="pin" size="15" value="${(requestParameters.pin)!}" /></td>
    </tr>
    <tr><td colspan="2">&nbsp;</td></tr>
    <tr>
      <td colspan="2" align="center"><input type="submit" class="smallSubmit" value="${uiLabelMap.EcommerceCheckBalance}" /></td>
    </tr>
  </form>
</table>
<br />
