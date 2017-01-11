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

<h1>${uiLabelMap.AccountingGiftCardLink}</h1>
<br/>
<div>${uiLabelMap.AccountingEnterGiftCardLink}.</div>
<br/>

<form name="gclink" method="post" action="<@ofbizUrl>linkgiftcard</@ofbizUrl>">
  <input type="hidden" name="paymentConfig" value="${paymentProperties?default("payment.properties")}"/>
  <#if userLogin?has_content>
    <input type="hidden" name="partyId" value="${userLogin.partyId}"/>
  </#if>
  <table align="center">
    <tr>
      <td colspan="2" align="center">
        <div class="tableheadtext">${uiLabelMap.AccountingPhysicalCard}</div>
      </td>
    </tr>
    <tr>
      <td>
        <div>${uiLabelMap.AccountingCardNumber}</div>
      </td>
      <td><input type="text" class="inputBox" name="physicalCard" size="20"/></td>
    </tr>
    <tr>
      <td>
        <div>${uiLabelMap.AccountingPINNumber}</div>
      </td>
      <td><input type="text" class="inputBox" name="physicalPin" size="20"/></td>
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
      <td>
        <div>${uiLabelMap.AccountingCardNumber}</div>
      </td>
      <td><input type="text" class="inputBox" name="virtualCard" size="20"/></td>
    </tr>
    <tr>
      <td>
        <div>${uiLabelMap.AccountingPINNumber}</div>
      </td>
      <td><input type="text" class="inputBox" name="virtualPin" size="20"/></td>
    </tr>
    <tr>
      <td colspan="2">&nbsp;</td>
    </tr>
    <tr>
      <td colspan="2" align="center"><input type="submit" class="smallSubmit" value="${uiLabelMap.EcommerceLinkCards}"/>
      </td>
    </tr>
  </table>
</form>
<br/>
