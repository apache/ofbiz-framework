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
<div id="microCart">
    <#if (shoppingCartSize > 0)>
      <table class="basic-table" cellspacing="1" cellpadding="2">
        <tr>
          <td><b>${uiLabelMap.WebPosTransactionId}</b></td>
          <td><b>${transactionId?default("NA")}</b></td>
          <td><b>${(paymentCash.get("description", locale))?if_exists}</b></td>
          <td align="right"><b><@ofbizCurrency amount=cashAmount isoCode=shoppingCart.getCurrency()/></b></td>
          <td><b>${uiLabelMap.OrderSalesTax}</b></td>
          <td align="right"><b><@ofbizCurrency amount=shoppingCart.getTotalSalesTax() isoCode=shoppingCart.getCurrency()/></b></td>
        </tr>
        <tr>
          <td><b>${uiLabelMap.WebPosDrawer}</b></td>
          <td><b>${drawerNumber?default(0)}</b></td>
          <td><b>${(paymentCheck.get("description", locale))?if_exists}</b></td>
          <td align="right"><b><@ofbizCurrency amount=checkAmount isoCode=shoppingCart.getCurrency()/></b></td>
          <td><b>${uiLabelMap.WebPosCartTotal}</b></td>
          <td align="right"><b><@ofbizCurrency amount=shoppingCart.getDisplayGrandTotal() isoCode=shoppingCart.getCurrency()/></b></td>
        </tr>
        <tr>
          <td><b>${uiLabelMap.WebPosEmployee}</b></td>
          <td><b>${userLoginId?default("NA")}</b></td>
          <td><b>${(paymentGift.get("description", locale))?if_exists}</b></td>
          <td align="right"><b><@ofbizCurrency amount=giftAmount isoCode=shoppingCart.getCurrency()/></b></td>
          <td colspan="2">&nbsp;</td>
        </tr>
        <tr>
          <td><b>${uiLabelMap.WebPosTransactionDate}</b></td>
          <td><b>${transactionDate?default("NA")}</b></td>
          <td><b>${(paymentCredit.get("description", locale))?if_exists}</b></td>
          <td align="right"><b><@ofbizCurrency amount=creditAmount isoCode=shoppingCart.getCurrency()/></b></td>
          <td><b>${uiLabelMap.WebPosTransactionTotalDue}</b></td>
          <td align="right"><b><@ofbizCurrency amount=totalDue isoCode=shoppingCart.getCurrency()/></b></td>
        </tr>
      </table>
    <#else>
      &nbsp;
    </#if>
</div>