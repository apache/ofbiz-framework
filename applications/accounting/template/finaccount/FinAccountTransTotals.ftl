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
<#if grandTotal??>
  <table class="basic-table">
    <tr>
      <th>${uiLabelMap.FormFieldTitle_grandTotal} / ${uiLabelMap.AccountingNumberOfTransaction}</th>
      <th>${uiLabelMap.AccountingCreatedGrandTotal} / ${uiLabelMap.AccountingNumberOfTransaction}</th>
      <th>${uiLabelMap.AccountingApprovedGrandTotal} / ${uiLabelMap.AccountingNumberOfTransaction}</th>
      <th>${uiLabelMap.AccountingCreatedApprovedGrandTotal} / ${uiLabelMap.AccountingNumberOfTransaction}</th>
    </tr>
    <tr>
      <td><@ofbizCurrency amount=grandTotal isoCode=accountCurrencyUomId/> / ${searchedNumberOfRecords}</td>
      <td><@ofbizCurrency amount=createdGrandTotal isoCode=accountCurrencyUomId/> / ${totalCreatedTransactions}</td>
      <td><@ofbizCurrency amount=approvedGrandTotal isoCode=accountCurrencyUomId/> / ${totalApprovedTransactions}</td>
      <td><@ofbizCurrency amount=createdApprovedGrandTotal isoCode=accountCurrencyUomId/> / ${totalCreatedApprovedTransactions}</td>
    </tr>
  </table>
<#else>
  <table class="basic-table">
    <tr>
      <th>${uiLabelMap.AccountingRunningTotal} / ${uiLabelMap.AccountingNumberOfTransaction}</th>
      <th>${uiLabelMap.AccountingOpeningBalance}</th>
      <th>${uiLabelMap.FormFieldTitle_reconciledBalance}</th>
      <th>${uiLabelMap.FormFieldTitle_closingBalance}</th>
    </tr>
    <tr>
      <td>
        <span id="finAccountTransRunningTotal"></span> /
        <span id="numberOfFinAccountTransaction"></span>
      </td>
      <td> <@ofbizCurrency amount=glReconciliation.openingBalance?default('0') isoCode=accountCurrencyUomId/></td>
      <td><@ofbizCurrency amount=glReconciliation.reconciledBalance?default('0') isoCode=accountCurrencyUomId/></td>
      <td id="endingBalance"><@ofbizCurrency amount=glReconciliationApprovedGrandTotal! isoCode=accountCurrencyUomId/></td>
    </tr>
  </table>
</#if>