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

<#assign accountCurrencyUomId = finAccount.currencyUomId/>
<#if finAccountTransList?has_content && parameters.noConditionFind?? && parameters.noConditionFind == 'Y'>
  <#if !grandTotal??>
      <div>
      <span class="label">${uiLabelMap.AccountingRunningTotal} :</span>
      <span class="label" id="showFinAccountTransRunningTotal"></span>
    </div>
  </#if>

  <#assign glReconciliations = EntityQuery.use(delegator).from("GlReconciliation").where("glAccountId", finAccount.postToGlAccountId!, "statusId", "GLREC_CREATED").orderBy("reconciledDate DESC").queryList()!>

  <table class="basic-table hover-bar" cellspacing="0">
    <tr class="header-row-2">
      <th>${uiLabelMap.CommonId}</th>
      <th>${uiLabelMap.CommonType}</th>
      <th>${uiLabelMap.CommonParty}</th>
      <th>${uiLabelMap.FormFieldTitle_glReconciliationName}</th>
      <th>${uiLabelMap.FormFieldTitle_transactionDate}</th>
      <th>${uiLabelMap.FormFieldTitle_entryDate}</th>
      <th>${uiLabelMap.CommonAmount}</th>
      <th>${uiLabelMap.CommonPayment}</th>
      <th>${uiLabelMap.OrderPaymentType}</th>
      <th>${uiLabelMap.CommonMethod}</th>
      <th>${uiLabelMap.CommonStatus}</th>
      <th>${uiLabelMap.CommonComments}</th>
    </tr>
    <#assign alt_row = false>
    <#list finAccountTransList as finAccountTrans>
    <#assign payment = "">
    <#assign payments = "">
    <#assign status = "">
    <#assign paymentType = "">
    <#assign paymentMethodType = "">
    <#assign glReconciliation = "">
    <#assign partyName = "">
    <#if finAccountTrans.paymentId?has_content>
      <#assign payment = EntityQuery.use(delegator).from("Payment").where("paymentId", finAccountTrans.paymentId!).cache().queryOne()!>
    <#else>
      <#assign payments = EntityQuery.use(delegator).from("Payment").where("finAccountTransId", finAccountTrans.finAccountTransId!).queryList()!>
    </#if>
    <#assign finAccountTransType = EntityQuery.use(delegator).from("FinAccountTransType").where("finAccountTransTypeId", finAccountTrans.finAccountTransTypeId!).cache().queryOne()!>
    <#if payment?has_content && payment.paymentTypeId?has_content>
      <#assign paymentType = EntityQuery.use(delegator).from("PaymentType").where("paymentTypeId", payment.paymentTypeId!).cache().queryOne()!>
    </#if>
    <#if payment?has_content && payment.paymentMethodTypeId?has_content>
      <#assign paymentMethodType = EntityQuery.use(delegator).from("PaymentMethodType").where("paymentMethodTypeId", payment.paymentMethodTypeId!).cache().queryOne()!>
    </#if>
    <#if finAccountTrans.glReconciliationId?has_content>
        <#assign glReconciliation = EntityQuery.use(delegator).from("GlReconciliation").where("glReconciliationId", finAccountTrans.glReconciliationId!).cache().queryOne()!>
    </#if>
    <#if finAccountTrans.partyId?has_content>
        <#assign partyName = EntityQuery.use(delegator).from("PartyNameView").where("partyId", finAccountTrans.partyId!).cache().queryOne()!!>
    </#if>
    <#if finAccountTrans.statusId?has_content>
      <#assign status = EntityQuery.use(delegator).from("StatusItem").where("statusId", finAccountTrans.statusId!).cache().queryOne()!>
    </#if>
    <tr valign="middle"<#if alt_row> class="alternate-row"</#if>>
      <td>
        <#if payments?has_content>
          <div id="displayPayments_${finAccountTrans.finAccountTransId}" style="display: none;width: 650px;">
            <table class="basic-table hover-bar" cellspacing="0" style"width :">
              <tr class="header-row-2">
                <th>${uiLabelMap.AccountingDepositSlipId}</th>
                <th>${uiLabelMap.CommonPayment}</th>
                <th>${uiLabelMap.OrderPaymentType}</th>
                <th>${uiLabelMap.CommonMethod}</th>
                <th>${uiLabelMap.CommonAmount}</th>
                <th>${uiLabelMap.CommonFrom}</th>
                <th>${uiLabelMap.CommonTo}</th>
              </tr>
              <#list payments as payment>
                <#if payment?? && payment.paymentTypeId?has_content>
                  <#assign paymentType = EntityQuery.use(delegator).from("PaymentType").where("paymentTypeId", payment.paymentTypeId!).cache().queryOne()!>
                </#if>
                <#if payment?has_content && payment.paymentMethodTypeId?has_content>
                  <#assign paymentMethodType = EntityQuery.use(delegator).from("PaymentMethodType").where("paymentMethodTypeId", payment.paymentMethodTypeId!).cache().queryOne()!>
                </#if>
                <#if payment?has_content>
                  <#assign paymentGroupMembers = Static["org.apache.ofbiz.entity.util.EntityUtil"].filterByDate(payment.getRelated("PaymentGroupMember", null, null, false)!) />
                  <#assign fromParty = payment.getRelatedOne("FromParty", false)! />
                  <#assign fromPartyName = EntityQuery.use(delegator).from("PartyNameView").where("partyId", fromParty.partyId!).cache().queryOne()!/>
                  <#assign toParty = payment.getRelatedOne("ToParty", false)! />
                  <#assign toPartyName =EntityQuery.use(delegator).from("PartyNameView").where("partyId", toParty.partyId!).cache().queryOne()!/>
                  <#if paymentGroupMembers?has_content>
                  <#assign paymentGroupMember = Static["org.apache.ofbiz.entity.util.EntityUtil"].getFirst(paymentGroupMembers) />
                  </#if>
                </#if>
                <tr valign="middle"<#if alt_row> class="alternate-row"</#if>>
                  <td><#if paymentGroupMember?has_content><a href="<@ofbizUrl>EditDepositSlipAndMembers?paymentGroupId=${paymentGroupMember.paymentGroupId!}&amp;finAccountId=${parameters.finAccountId!}</@ofbizUrl>">${paymentGroupMember.paymentGroupId!}</a></#if></td>
                  <td><#if payment?has_content><a href="<@ofbizUrl>paymentOverview?paymentId=${payment.paymentId!}</@ofbizUrl>">${payment.paymentId!}</a></#if></td>
                  <td><#if paymentType?has_content>${paymentType.description!}</#if></td>
                  <td><#if paymentMethodType?has_content>${paymentMethodType.description!}</#if></td>
                  <td><@ofbizCurrency amount=payment.amount! isoCode=accountCurrencyUomId/></td>
                  <td><#if fromPartyName?has_content><a href="<@ofbizUrl controlPath="/partymgr/control">viewprofile?partyId=${fromPartyName.partyId!}</@ofbizUrl>">[${fromPartyName.partyId!}]</a> ${fromPartyName.groupName!}${fromPartyName.firstName!} ${fromPartyName.lastName!}</#if></td>
                  <td><#if toPartyName?has_content><a href="<@ofbizUrl controlPath="/partymgr/control">viewprofile?partyId=${toPartyName.partyId!}</@ofbizUrl>">[${toPartyName.partyId!}]</a> ${toPartyName.groupName!}${toPartyName.firstName!} ${toPartyName.lastName!}</#if></td>
                </tr>
              </#list>
            </table>
          </div>
        <a href="<@ofbizUrl>DepositSlip.pdf?finAccountTransId=${finAccountTrans.finAccountTransId}</@ofbizUrl>" target="_BLANK" class="buttontext">${uiLabelMap.AccountingDepositSlip}</a>
      <#else>
        ${finAccountTrans.finAccountTransId}
      </#if>
      </td>
      <td>${finAccountTransType.description!}</td>
      <td><#if partyName?has_content><a href="<@ofbizUrl controlPath="/partymgr/control">viewprofile?partyId=${partyName.partyId}</@ofbizUrl>">[${(partyName.partyId)!}]</a> ${(partyName.groupName)!}${(partyName.firstName)!} ${(partyName.lastName)!}</#if></td>
      <td><#if glReconciliation?has_content><a href="ViewGlReconciliationWithTransaction?glReconciliationId=${glReconciliation.glReconciliationId!}&amp;finAccountId=${parameters.finAccountId!}"> [${glReconciliation.glReconciliationId!}] </a> ${glReconciliation.glReconciliationName!}</#if></td>
      <td>${finAccountTrans.transactionDate!}</td>
      <td>${finAccountTrans.entryDate!}</td>
      <td align="right"><@ofbizCurrency amount=finAccountTrans.amount isoCode=accountCurrencyUomId/></td>
      <td>
        <#if finAccountTrans.paymentId?has_content>
          <a href="<@ofbizUrl>paymentOverview?paymentId=${finAccountTrans.paymentId}</@ofbizUrl>">${finAccountTrans.paymentId}</a>
        </#if>
      </td>
      <td><#if paymentType?has_content>${paymentType.description!}</#if></td>
      <td><#if paymentMethodType?has_content>${paymentMethodType.description!}</#if></td>
      <td><#if status?has_content>${status.description!}</#if></td>
      <td>${finAccountTrans.comments!}</td>
    </tr>
    <#-- toggle the row color -->
    <#assign alt_row = !alt_row>
    </#list>
  </table>
  <#include "FinAccountTransTotals.ftl">
<#else>
  <h2>${uiLabelMap.CommonNoRecordFound}</h2>
</#if>
