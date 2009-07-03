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
<div class="button-bar button-style-2">
  <ul>
    <#assign isDisbursement = Static["org.ofbiz.accounting.util.UtilAccounting"].isDisbursement(payment)/>
    <#if (payment.paymentId)?has_content>
      <li>
        <a href="javascript:document.PaymentSubTabBar_newPayment.submit()" class="buttontext">${uiLabelMap.CommonCreateNew}</a>
        <form method="post" action="<@ofbizUrl>newPayment</@ofbizUrl>" onSubmit="javascript:submitFormDisableSubmits(this)" name="PaymentSubTabBar_newPayment">
        </form>
      </li>
      <#if isDisbursement == true && payment.statusId == "PMNT_NOT_PAID">
        <li>
          <a href="javascript:document.PaymentSubTabBar_statusToSend.submit()" class="buttontext">${uiLabelMap.AccountingPaymentTabStatusToSent}</a>
          <form method="post" action="<@ofbizUrl>setPaymentStatus</@ofbizUrl>" onSubmit="javascript:submitFormDisableSubmits(this)" name="PaymentSubTabBar_statusToSend">
            <input type="hidden" name="paymentId" value="${payment.paymentId}"/>
            <input type="hidden" name="statusId" value="PMNT_SENT"/>
          </form>
        </li>
      </#if>
      <#if isDisbursement == false && payment.statusId == "PMNT_NOT_PAID">
        <li>
          <a href="javascript:document.PaymentSubTabBar_statusToReceived.submit()" class="buttontext">${uiLabelMap.AccountingPaymentTabStatusToReceived}</a>
          <form method="post" action="<@ofbizUrl>setPaymentStatus</@ofbizUrl>" onSubmit="javascript:submitFormDisableSubmits(this)" name="PaymentSubTabBar_statusToReceived">
            <input type="hidden" name="paymentId" value="${payment.paymentId}"/>
            <input type="hidden" name="statusId" value="PMNT_RECEIVED"/>
          </form>
        </li>
      </#if>
      <#if payment.statusId == "PMNT_RECEIVED" || payment.statusId == "PMNT_SENT">
        <li>
          <a href="javascript:document.PaymentSubTabBar_statusToConfirmed.submit()" class="buttontext">${uiLabelMap.AccountingPaymentTabStatusToConfirmed}</a>
          <form method="post" action="<@ofbizUrl>setPaymentStatus</@ofbizUrl>" onSubmit="javascript:submitFormDisableSubmits(this)" name="PaymentSubTabBar_statusToConfirmed">
            <input type="hidden" name="paymentId" value="${payment.paymentId}"/>
            <input type="hidden" name="statusId" value="PMNT_CONFIRMED"/>
          </form>
        </li>
      </#if>
      <#if payment.statusId == "PMNT_NOT_PAID">
        <li>
          <a href="javascript:confirmActionFormLink('You want to cancel this payment number ${payment.paymentId}?','PaymentSubTabBar_statusToCancelled')" class="buttontext">${uiLabelMap.AccountingPaymentTabStatusToCancelled}</a>
          <form method="post" action="<@ofbizUrl>setInvoiceStatus</@ofbizUrl>" onSubmit="javascript:submitFormDisableSubmits(this)" name="PaymentSubTabBar_statusToCancelled">
            <input type="hidden" name="paymentId" value="${payment.paymentId}"/>
            <input type="hidden" name="statusId" value="PMNT_CANCELLED"/>
          </form>
        </li>
        <li><a target="_BLANK" href="<@ofbizUrl>printChecks.pdf?paymentId=${payment.paymentId}</@ofbizUrl>">${uiLabelMap.AccountingPrintAsCheck}</a></li>
      </#if>
      <#if payment.statusId != "PMNT_VOID">
        <li>
          <a id="void_payment" href="javascript:confirmActionFormLink('You want to void this payment number ${payment.paymentId}?','PaymentSubTabBar_statusToVoidPayment')" class="buttontext" onclick="javascript:Effect.Fade('void_payment')">${uiLabelMap.AccountingPaymentTabStatusToVoid}</a>
          <form method="post" action="<@ofbizUrl>voidPayment</@ofbizUrl>" onSubmit="javascript:submitFormDisableSubmits(this)" name="PaymentSubTabBar_statusToVoidPayment">
            <input type="hidden" name="paymentId" value="${payment.paymentId}"/>
          </form>
        </li>
      </#if>
    </#if>
  </ul>
</div>  