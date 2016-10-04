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

<#macro maskSensitiveNumber cardNumber>
  <#assign cardNumberDisplay = "">
  <#if cardNumber?has_content>
    <#assign size = cardNumber?length - 4>
    <#if (size > 0)>
      <#list 0 .. size-1 as foo>
        <#assign cardNumberDisplay = cardNumberDisplay + "*">
      </#list>
      <#assign cardNumberDisplay = cardNumberDisplay + cardNumber[size .. size + 3]>
    <#else>
      <#-- but if the card number has less than four digits (ie, it was entered incorrectly), display it in full -->
      <#assign cardNumberDisplay = cardNumber>
    </#if>
  </#if>
  ${cardNumberDisplay!}
</#macro>

  <div id="partyPaymentMethod" class="screenlet">
    <div class="screenlet-title-bar">
      <ul>
        <li class="h3">${uiLabelMap.AccountingPaymentMethod}</li>
        <#if security.hasEntityPermission("PAY_INFO", "_CREATE", session) || security.hasEntityPermission("ACCOUNTING", "_CREATE", session)>
          <li><a href="<@ofbizUrl>editeftaccount?partyId=${partyId}</@ofbizUrl>">${uiLabelMap.AccountingCreateEftAccount}</a></li>
          <li><a href="<@ofbizUrl>editgiftcard?partyId=${partyId}</@ofbizUrl>">${uiLabelMap.AccountingCreateGiftCard}</a></li>
          <li><a href="<@ofbizUrl>editcreditcard?partyId=${partyId}</@ofbizUrl>">${uiLabelMap.AccountingCreateCreditCard}</a></li>
          <li><a href="<@ofbizUrl>EditBillingAccount?partyId=${partyId}</@ofbizUrl>">${uiLabelMap.AccountingCreateBillingAccount}</a></li>
          <li><a href="<@ofbizUrl>AddCheckAccount?partyId=${partyId}</@ofbizUrl>">${uiLabelMap.AccountingAddCheckAccount}</a></li>
        </#if>
      </ul>
      <br class="clear" />
    </div>
    <div class="screenlet-body">
      <#if paymentMethodValueMaps?has_content || billingAccounts?has_content>
        <table class="basic-table" cellspacing="0">
        <#if paymentMethodValueMaps?has_content>
          <#list paymentMethodValueMaps as paymentMethodValueMap>
            <#assign paymentMethod = paymentMethodValueMap.paymentMethod/>
            <tr>
              <#if "CREDIT_CARD" == paymentMethod.paymentMethodTypeId && paymentMethodValueMap.creditCard?has_content>
                <#assign creditCard = paymentMethodValueMap.creditCard/>
                <td class="label">
                  ${uiLabelMap.AccountingCreditCard}
                </td>
                <td>
                  <#if creditCard.companyNameOnCard?has_content>${creditCard.companyNameOnCard}&nbsp;</#if>
                  <#if creditCard.titleOnCard?has_content>${creditCard.titleOnCard}&nbsp;</#if>
                  ${creditCard.firstNameOnCard}&nbsp;
                  <#if creditCard.middleNameOnCard?has_content>${creditCard.middleNameOnCard}&nbsp;</#if>
                  ${creditCard.lastNameOnCard}
                  <#if creditCard.suffixOnCard?has_content>&nbsp;${creditCard.suffixOnCard}</#if>
                  &nbsp;-&nbsp;
                  <#if security.hasEntityPermission("PAY_INFO", "_VIEW", session) || security.hasEntityPermission("ACCOUNTING", "_VIEW", session)>
                    ${creditCard.cardType}
                    <@maskSensitiveNumber cardNumber=creditCard.cardNumber!/>
                    ${creditCard.expireDate}
                  <#else>
                    ${Static["org.apache.ofbiz.party.contact.ContactHelper"].formatCreditCard(creditCard)}
                  </#if>
                  <#if paymentMethod.description?has_content>(${paymentMethod.description})</#if>
                  <#if paymentMethod.glAccountId?has_content>(for GL Account ${paymentMethod.glAccountId})</#if>
                  <#if paymentMethod.fromDate?has_content>(${uiLabelMap.CommonUpdated}:&nbsp;${paymentMethod.fromDate!})</#if>
                  <#if paymentMethod.thruDate?has_content><b>(${uiLabelMap.PartyContactEffectiveThru}:&nbsp;${paymentMethod.thruDate})</#if>
                </td>
                <td class="button-col">
                  <#if security.hasEntityPermission("MANUAL", "_PAYMENT", session)>
                    <a href="/accounting/control/manualETx?paymentMethodId=${paymentMethod.paymentMethodId}${StringUtil.wrapString(externalKeyParam)}">${uiLabelMap.PartyManualTx}</a>
                  </#if>
                  <#if security.hasEntityPermission("PAY_INFO", "_UPDATE", session) || security.hasEntityPermission("ACCOUNTING", "_UPDATE", session)>
                    <a href="<@ofbizUrl>editcreditcard?partyId=${partyId}&amp;paymentMethodId=${paymentMethod.paymentMethodId}</@ofbizUrl>">${uiLabelMap.CommonUpdate}</a>
                  </#if>
              <#elseif "GIFT_CARD" == paymentMethod.paymentMethodTypeId>
                <#assign giftCard = paymentMethodValueMap.giftCard>
                <td class="label" valign="top">
                  ${uiLabelMap.AccountingGiftCard}
                </td>
                <td>
                  <#if security.hasEntityPermission("PAY_INFO", "_VIEW", session) || security.hasEntityPermission("ACCOUNTING", "_VIEW", session)>
                    ${giftCard.cardNumber?default("N/A")} [${giftCard.pinNumber?default("N/A")}]
                  <#else>
                    <@maskSensitiveNumber cardNumber=giftCard.cardNumber!/>
                    <#if !cardNumberDisplay?has_content>N/A</#if>
                  </#if>
                  <#if paymentMethod.description?has_content>(${paymentMethod.description})</#if>
                  <#if paymentMethod.glAccountId?has_content>(for GL Account ${paymentMethod.glAccountId})</#if>
                  <#if paymentMethod.fromDate?has_content>(${uiLabelMap.CommonUpdated}:&nbsp;${paymentMethod.fromDate!})</#if>
                  <#if paymentMethod.thruDate?has_content><b>(${uiLabelMap.PartyContactEffectiveThru}:&nbsp;${paymentMethod.thruDate.toString()}</b></#if>
                </td>
                <td class="button-col">
                  <#if security.hasEntityPermission("PAY_INFO", "_UPDATE", session) || security.hasEntityPermission("ACCOUNTING", "_UPDATE", session)>
                    <a href="<@ofbizUrl>editgiftcard?partyId=${partyId}&amp;paymentMethodId=${paymentMethod.paymentMethodId}</@ofbizUrl>">${uiLabelMap.CommonUpdate}</a>
                  </#if>
              <#elseif "EFT_ACCOUNT" == paymentMethod.paymentMethodTypeId>
                <#assign eftAccount = paymentMethodValueMap.eftAccount>
                <td class="label" valign="top">
                    ${uiLabelMap.PartyEftAccount}
                </td>
                <td>
                  ${eftAccount.nameOnAccount} - <#if eftAccount.bankName?has_content>${uiLabelMap.PartyBank}: ${eftAccount.bankName}</#if> <#if eftAccount.accountNumber?has_content>${uiLabelMap.PartyAccount} #: ${eftAccount.accountNumber}</#if>                  <#if paymentMethod.description?has_content>(${paymentMethod.description})</#if>
                  <#if paymentMethod.glAccountId?has_content>(for GL Account ${paymentMethod.glAccountId})</#if>
                  <#if paymentMethod.fromDate?has_content>(${uiLabelMap.CommonUpdated}:&nbsp;${paymentMethod.fromDate!})</#if>
                  <#if paymentMethod.thruDate?has_content><b>(${uiLabelMap.PartyContactEffectiveThru}:&nbsp;${paymentMethod.thruDate.toString()}</#if>
                </td>
                <td class="button-col">
                  <#if security.hasEntityPermission("PAY_INFO", "_UPDATE", session) || security.hasEntityPermission("ACCOUNTING", "_UPDATE", session)>
                    <a href="<@ofbizUrl>editeftaccount?partyId=${partyId}&amp;paymentMethodId=${paymentMethod.paymentMethodId}</@ofbizUrl>">${uiLabelMap.CommonUpdate}</a>
                  </#if>
              <#elseif "COMPANY_CHECK" == paymentMethod.paymentMethodTypeId>
                <#if paymentMethodValueMap.companyCheckAccount?has_content>
                  <#assign checkAccount = paymentMethodValueMap.companyCheckAccount>
                </#if>
                <#if checkAccount?has_content>
                  <td class="label" valign="top">
                    <#-- TODO: Convert hard-coded text to UI label properties -->
                    Company Check
                  </td>
                  <td>
                    ${checkAccount.nameOnAccount} - <#if checkAccount.bankName?has_content>${uiLabelMap.PartyBank}: ${checkAccount.bankName}</#if>
                    <#if checkAccount.accountNumber?has_content>${uiLabelMap.PartyAccount} #: ${checkAccount.accountNumber}</#if>
                    <#if paymentMethod.description?has_content>(${paymentMethod.description})</#if>
                    <#if paymentMethod.glAccountId?has_content>(for GL Account ${paymentMethod.glAccountId})</#if>
                    <#if paymentMethod.fromDate?has_content>(${uiLabelMap.CommonUpdated}:&nbsp;${paymentMethod.fromDate!})</#if>
                    <#if paymentMethod.thruDate?has_content>(${uiLabelMap.PartyContactEffectiveThru}:&nbsp;${paymentMethod.thruDate.toString()}</#if>
                  </td>
                  <td class="button-col">
                  <#if security.hasEntityPermission("PAY_INFO", "_UPDATE", session) || security.hasEntityPermission("ACCOUNTING", "_UPDATE", session)>
                      <a href="<@ofbizUrl>AddCheckAccount?partyId=${partyId}&amp;paymentMethodId=${paymentMethod.paymentMethodId}</@ofbizUrl>">${uiLabelMap.CommonUpdate}</a>
                  </#if>
                </#if>
              <#elseif "PERSONAL_CHECK" == paymentMethod.paymentMethodTypeId>
                <#if paymentMethodValueMap.personalCheckAccount?has_content>
                  <#assign checkAccount = paymentMethodValueMap.personalCheckAccount>
                </#if>
                <#if checkAccount?has_content>
                  <td class="label" valign="top">
                    Personal Check
                  </td>
                  <td>
                    ${checkAccount.nameOnAccount} - <#if checkAccount.bankName?has_content>${uiLabelMap.PartyBank}: ${checkAccount.bankName}</#if>
                    <#if checkAccount.accountNumber?has_content>${uiLabelMap.PartyAccount} #: ${checkAccount.accountNumber}</#if>
                    <#if paymentMethod.description?has_content>(${paymentMethod.description})</#if>
                    <#if paymentMethod.glAccountId?has_content>(for GL Account ${paymentMethod.glAccountId})</#if>
                    <#if paymentMethod.fromDate?has_content>(${uiLabelMap.CommonUpdated}:&nbsp;${paymentMethod.fromDate!})</#if>
                    <#if paymentMethod.thruDate?has_content>(${uiLabelMap.PartyContactEffectiveThru}:&nbsp;${paymentMethod.thruDate.toString()}</#if>
                  </td>
                  <td class="button-col">
                  <#if security.hasEntityPermission("PAY_INFO", "_UPDATE", session) || security.hasEntityPermission("ACCOUNTING", "_UPDATE", session)>
                      <a href="<@ofbizUrl>AddCheckAccount?partyId=${partyId}&amp;paymentMethodId=${paymentMethod.paymentMethodId}</@ofbizUrl>">${uiLabelMap.CommonUpdate}</a>
                  </#if>
                </#if>
              <#elseif "CERTIFIED_CHECK" == paymentMethod.paymentMethodTypeId>
                <#if paymentMethodValueMap.certifiedCheckAccount?has_content>
                  <#assign checkAccount = paymentMethodValueMap.certifiedCheckAccount>
                </#if>
                <#if checkAccount?has_content>
                  <td class="label" valign="top">
                  Certified Check
                  </td>
                  <td>
                    ${checkAccount.nameOnAccount} - <#if checkAccount.bankName?has_content>${uiLabelMap.PartyBank}: ${checkAccount.bankName}</#if>
                    <#if checkAccount.accountNumber?has_content>${uiLabelMap.PartyAccount} #: ${checkAccount.accountNumber}</#if>
                    <#if paymentMethod.description?has_content>(${paymentMethod.description})</#if>
                    <#if paymentMethod.glAccountId?has_content>(for GL Account ${paymentMethod.glAccountId})</#if>
                    <#if paymentMethod.fromDate?has_content>(${uiLabelMap.CommonUpdated}:&nbsp;${paymentMethod.fromDate!})</#if>
                    <#if paymentMethod.thruDate?has_content>(${uiLabelMap.PartyContactEffectiveThru}:&nbsp;${paymentMethod.thruDate.toString()}</#if>
                  </td>
                  <td class="button-col">
                  <#if security.hasEntityPermission("PAY_INFO", "_UPDATE", session) || security.hasEntityPermission("ACCOUNTING", "_UPDATE", session)>
                      <a href="<@ofbizUrl>AddCheckAccount?partyId=${partyId}&amp;paymentMethodId=${paymentMethod.paymentMethodId}</@ofbizUrl>">${uiLabelMap.CommonUpdate}</a>
                  </#if>
                </#if>
              <#else>
                <td class="button-col">
                  &nbsp;
              </#if>
              <#if security.hasEntityPermission("PAY_INFO", "_DELETE", session) || security.hasEntityPermission("ACCOUNTING", "_DELETE", session)>
                <a href="<@ofbizUrl>deletePaymentMethod/viewprofile?partyId=${partyId}&amp;paymentMethodId=${paymentMethod.paymentMethodId}</@ofbizUrl>">${uiLabelMap.CommonExpire}</a>
              <#else>
                &nbsp;
              </#if>
              </td> <#-- closes out orphaned <td> elements inside conditionals -->
            </tr>
          </#list>
        </#if>
        <#-- Billing list-->
        <#if billingAccounts?has_content>
            <#list billingAccounts as billing>
            <tr>
              <td class="label" valign="top">${uiLabelMap.AccountingBilling}</td>
              <td>
                  <#if billing.billingAccountId?has_content>${billing.billingAccountId}</#if>
                  <#if billing.description?has_content>(${billing.description})</#if>
                  <#if billing.accountLimit?has_content>(${uiLabelMap.AccountingAccountLimit} $${billing.accountLimit})</#if>
                  <#if billing.accountBalance?has_content>(${uiLabelMap.AccountingBillingAvailableBalance} $${billing.accountBalance})</#if>
                  <#if billing.fromDate?has_content>(${uiLabelMap.CommonUpdated}:&nbsp;${billing.fromDate!})</#if>
                  <#if billing.thruDate?has_content><b>(${uiLabelMap.PartyContactEffectiveThru}:&nbsp;${billing.thruDate.toString()}</b></#if>
              </td>
              <td class="button-col">
                <a href="<@ofbizUrl>EditBillingAccount?billingAccountId=${billing.billingAccountId}&amp;partyId=${partyId}</@ofbizUrl>">${uiLabelMap.CommonUpdate}</a>
                <a href="<@ofbizUrl>deleteBillingAccount?partyId=${partyId}&amp;billingAccountId=${billing.billingAccountId}</@ofbizUrl>">${uiLabelMap.CommonExpire}</a>
              </td>
          </tr>
          </#list>
        </#if>
        </table>
      <#else>
        ${uiLabelMap.PartyNoPaymentMethodInformation}
      </#if>
    </div>
  </div>
