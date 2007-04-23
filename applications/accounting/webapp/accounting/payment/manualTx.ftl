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

<h1>${uiLabelMap.AccountingManualTransaction}</h1>
<br/>

<#if security.hasEntityPermission("MANUAL", "_PAYMENT", session)>
  ${setRequestAttribute("validTx", "false")}
  <form name="manualTxForm" method="get" action="<@ofbizUrl>manualETx</@ofbizUrl>">
    <#if requestParameters.paymentMethodId?exists>
      <input type="hidden" name="paymentMethodId" value="${requestParameters.paymentMethodId}">
    </#if>

    <table border='0' cellpadding='2' cellspacing='0'>
      <tr>
        <td width="26%" align="right" valign="middle"><b>${uiLabelMap.AccountingPaymentMethodType}</b></td>
        <td width="5">&nbsp;</td>
        <td width='74%'>
          <#if paymentMethodType?has_content>
            <div>${paymentMethodType.get("description",locale)}</div>
            <input type="hidden" name="paymentMethodTypeId" value="${paymentMethodType.paymentMethodTypeId}">
          <#else>
            <select name="paymentMethodTypeId">
              <option value="CREDIT_CARD">${uiLabelMap.AccountingCreditCard}</option>
            </select>
          </#if>
        </td>
      </tr>
      <tr>
        <td width="26%" align="right" valign="middle"><b>${uiLabelMap.ProductProductStore}</b></td>
        <td width="5">&nbsp;</td>
        <td width='74%'>
          <#if currentStore?has_content>
            <div>${currentStore.storeName}</div>
            <input type="hidden" name="productStoreId" value="${currentStore.productStoreId}">
          <#else>
            <select name="productStoreId">
              <#list productStores as productStore>
                <option value="${productStore.productStoreId}">${productStore.storeName}</option>
              </#list>
            </select>
          </#if>
        </td>
      </tr>
      <tr>
        <td width="26%" align="right" valign="middle"><b>${uiLabelMap.AccountingTransactionType}</b></td>
        <td width="5">&nbsp;</td>
        <td width='74%'>
          <#if currentTx?has_content>
            <div>${currentTx.get("description",locale)}</div>
            <input type="hidden" name="transactionType" value="${currentTx.enumId}">
          <#else>
            <select name="transactionType" onchange="javascript:document.manualTxForm.submit();">
              <#list paymentSettings as setting>
                <option value="${setting.enumId}">${setting.get("description",locale)}</option>
              </#list>
            </select>
          </#if>
        </td>
      </tr>

      <#-- payment method information -->
      <#if paymentMethodType?has_content && paymentMethodTypeId == "CREDIT_CARD">
        ${screens.render("component://accounting/widget/PaymentScreens.xml#manualCCTx")}
      <#elseif paymentMethodType?has_content && paymentMethodTypeId == "GIFT_CARD">
        ${screens.render("component://accounting/widget/PaymentScreens.xml#manualGCTx")}
      </#if>

     <#if requestAttributes.validTx?default("false") == "true">
        <tr><td colspan="3"><hr></td></tr>

        <#-- amount field -->
        <#if txType != "PRDS_PAY_RELEASE">
          <tr>
            <td width="26%" align="right" valign="middle"><b>${uiLabelMap.CommonAmount}</b></td>
            <td width="5">&nbsp;</td>
            <td width="74%">
              <input type="text" size="20" maxlength="30" name="amount">
            *</td>
          </tr>
        </#if>

        <#-- submit button -->
        <tr>
          <td width="26%" align="right" valign=middle>&nbsp;</td>
          <td width="5">&nbsp;</td>
          <td width="74%">
            <input type="submit" value="${uiLabelMap.CommonSubmit}">
          </td>
        </tr>
      <#elseif txType?has_content>
        <tr>
          <td colspan="3" align="center">
            <br/>
            <div class="head2">${uiLabelMap.AccountingTransactionTypeNotYetSupported}</div>
            <br/>
          </td>
        </tr>
      </#if>
    </table>
  </form>
<#else>
  <h3>${uiLabelMap.AccountingPermissionError}</h3>
</#if>
