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

<#-- eft fields -->
<#if !eftAccount?has_content><#assign eftAccount = requestParameters></#if>
<tr>
  <td colspan="3">
    <hr/>
    <input type="hidden" name="paymentMethodId" value="${parameters.paymentMethodId!}"/>
  </td>
</tr>
<tr>
  <td width="26%" align="right" valign="top">
    <div class="tableheadtext">${uiLabelMap.AccountingEFTAccountInformation}</div>
  </td>
  <td width="5">&nbsp;</td>
  <td width="74%">&nbsp;</td>
</tr>
<tr>
  <td width="26%" align="right" valign="middle">
    <div>${uiLabelMap.AccountingNameOnAccount}</div>
  </td>
  <td width="5">&nbsp;</td>
  <td width="74%">
    <input type="text" class="inputBox" size="30" maxlength="60" name="nameOnAccount"
        value="${eftAccount.nameOnAccount!}"/>*
  </td>
</tr>
<tr>
  <td width="26%" align="right" valign="middle">
    <div>${uiLabelMap.AccountingCompanyNameOnAccount}</div>
  </td>
  <td width="5">&nbsp;</td>
  <td width="74%">
    <input type="text" class="inputBox" size="30" maxlength="60" name="companyNameOnAccount"
        value="${eftAccount.companyNameOnAccount!}"/>
  </td>
</tr>
<tr>
  <td width="26%" align="right" valign="middle">
    <div>${uiLabelMap.AccountingBankName}</div>
  </td>
  <td width="5">&nbsp;</td>
  <td width="74%">
    <input type="text" class="inputBox" size="30" maxlength="60" name="bankName"
        value="${eftAccount.bankName!}"/>*
  </td>
</tr>
<tr>
  <td width="26%" align="right" valign="middle">
    <div>${uiLabelMap.AccountingRoutingNumber}</div>
  </td>
  <td width="5">&nbsp;</td>
  <td width="74%">
    <input type="text" class="inputBox" size="10" maxlength="30" name="routingNumber"
        value="${eftAccount.routingNumber!}"/>*
  </td>
</tr>
<tr>
  <td width="26%" align="right" valign="middle">
    <div>${uiLabelMap.AccountingAccountType}</div>
  </td>
  <td width="5">&nbsp;</td>
  <td width="74%">
    <select name="accountType" class='selectBox'>
      <option>${eftAccount.accountType!}</option>
      <option></option>
      <option>Checking</option>
      <option>Savings</option>
    </select>*
  </td>
</tr>
<tr>
  <td width="26%" align="right" valign="middle">
    <div>${uiLabelMap.AccountingAccountNumber}</div>
  </td>
  <td width="5">&nbsp;</td>
  <td width="74%">
    <input type="text" class="inputBox" size="20" maxlength="40" name="accountNumber"
        value="${eftAccount.accountNumber!}"/>*
  </td>
</tr>
<tr>
  <td width="26%" align="right" valign="middle">
    <div>${uiLabelMap.CommonDescription}</div>
  </td>
  <td width="5">&nbsp;</td>
  <td width="74%">
    <input type="text" class="inputBox" size="30" maxlength="60" name="description"
        value="${eftAccount.description!}"/>
  </td>
</tr>
