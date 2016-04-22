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
<#if requestParameters.paymentMethodTypeId?has_content>
   <#assign paymentMethodTypeId = "${requestParameters.paymentMethodTypeId!}">
</#if>
<script language="JavaScript" type="text/javascript">
function shipBillAddr() {
    <#if requestParameters.singleUsePayment?default("N") == "Y">
      <#assign singleUse = "&amp;singleUsePayment=Y">
    <#else>
      <#assign singleUse = "">
    </#if>
    if (document.billsetupform.useShipAddr.checked) {
        window.location.replace("setPaymentInformation?createNew=Y&amp;addGiftCard=${requestParameters.addGiftCard!}&amp;paymentMethodTypeId=${paymentMethodTypeId!}&amp;useShipAddr=Y${singleUse}");
    } else {
        window.location.replace("setPaymentInformation?createNew=Y&amp;addGiftCard=${requestParameters.addGiftCard!}&amp;paymentMethodTypeId=${paymentMethodTypeId!}${singleUse}");
    }
}
</script>

<div class="screenlet">
    <div class="screenlet-title-bar">
        <div class='h3'>${uiLabelMap.AccountingPaymentInformation}</div>
    </div>
    <div class="screenlet-body">
          <#-- after initial screen; show detailed screens for selected type -->
          <#if paymentMethodTypeId! == "CREDIT_CARD">
            <#if creditCard?has_content && postalAddress?has_content && !requestParameters.useShipAddr??>
              <form method="post" action="<@ofbizUrl>changeCreditCardAndBillingAddress</@ofbizUrl>" name="${parameters.formNameValue}">
                <input type="hidden" name="paymentMethodId" value="${creditCard.paymentMethodId!}"/>
                <input type="hidden" name="contactMechId" value="${postalAddress.contactMechId!}"/>
            <#elseif requestParameters.useShipAddr??>
              <form method="post" action="<@ofbizUrl>enterCreditCard</@ofbizUrl>" name="${parameters.formNameValue}">
            <#else>
              <form method="post" action="<@ofbizUrl>enterCreditCardAndBillingAddress</@ofbizUrl>" name="${parameters.formNameValue}">
            </#if>
          <#elseif paymentMethodTypeId! == "EFT_ACCOUNT">
            <#if eftAccount?has_content && postalAddress?has_content>
              <form method="post" action="<@ofbizUrl>changeEftAccountAndBillingAddress</@ofbizUrl>" name="${parameters.formNameValue}">
                <input type="hidden" name="paymentMethodId" value="${eftAccount.paymentMethodId!}"/>
                <input type="hidden" name="contactMechId" value="${postalAddress.contactMechId!}"/>
            <#elseif requestParameters.useShipAddr??>
              <form method="post" action="<@ofbizUrl>enterEftAccount</@ofbizUrl>" name="${parameters.formNameValue}">
            <#else>
              <form method="post" action="<@ofbizUrl>enterEftAccountAndBillingAddress</@ofbizUrl>" name="${parameters.formNameValue}">
            </#if>
          <#elseif paymentMethodTypeId! == "GIFT_CARD"> <#--Don't know much how this is handled -->
            <form method="post" action="<@ofbizUrl>enterGiftCard</@ofbizUrl>" name="${parameters.formNameValue}">
          <#elseif paymentMethodTypeId! == "EXT_OFFLINE">
            <form method="post" action="<@ofbizUrl>processPaymentSettings</@ofbizUrl>" name="${parameters.formNameValue}">
          <#else>
            <div>${uiLabelMap.AccountingPaymentMethodTypeNotHandled} ${paymentMethodTypeId!}</div>
          </#if>

          <#if requestParameters.singleUsePayment?default("N") == "Y">
            <input type="hidden" name="singleUsePayment" value="Y"/>
            <input type="hidden" name="appendPayment" value="Y"/>
          </#if>
          <input type="hidden" name="contactMechTypeId" value="POSTAL_ADDRESS"/>
          <input type="hidden" name="partyId" value="${partyId}"/>
          <input type="hidden" name="paymentMethodTypeId" value="${paymentMethodTypeId!}"/>
          <input type="hidden" name="createNew" value="Y"/>
          <#if requestParameters.useShipAddr??>
            <input type="hidden" name="contactMechId" value="${parameters.contactMechId!}"/>
          </#if>

          <table width="100%" border="0" cellpadding="1" cellspacing="0">
            <#if cart.getShippingContactMechId()?? && paymentMethodTypeId! != "GIFT_CARD">
              <tr>
                <td width="26%" align="right" valign="top">
                  <input type="checkbox" name="useShipAddr" value="Y" onclick="javascript:shipBillAddr();" <#if useShipAddr??>checked="checked"</#if>/>
                </td>
                <td colspan="2" valign="middle">
                  <div>${uiLabelMap.FacilityBillingAddressSameShipping}</div>
                </td>
              </tr>
              <tr>
                <td colspan="3"><hr /></td>
              </tr>
            </#if>

            <#if (paymentMethodTypeId! == "CREDIT_CARD" || paymentMethodTypeId! == "EFT_ACCOUNT")>
              <tr>
                <td width="26%" align="right" valign="top"><div class="tableheadtext">${uiLabelMap.PartyBillingAddress}</div></td>
                <td width="5">&nbsp;</td>
                <td width="74%">&nbsp;</td>
              </tr>
              ${screens.render("component://ecommerce/widget/OrderScreens.xml#genericaddress")}
            </#if>

            <#-- credit card fields -->
            <#if paymentMethodTypeId! == "CREDIT_CARD">
              <#if !creditCard?has_content>
                <#assign creditCard = requestParameters>
              </#if>
              <tr>
                <td colspan="3"><hr /></td>
              </tr>
              <tr>
                <td width="26%" align="right" valign="top"><div class="tableheadtext">${uiLabelMap.AccountingCreditCardInformation}</div></td>
                <td width="5">&nbsp;</td>
                <td width="74%">&nbsp;</td>
              </tr>

              ${screens.render("component://accounting/widget/CommonScreens.xml#creditCardFields")}
            </#if>

            <#-- eft fields -->
            <#if paymentMethodTypeId! =="EFT_ACCOUNT">
              <#if !eftAccount?has_content>
                <#assign eftAccount = requestParameters>
              </#if>
              <tr>
                <td colspan="3"><hr /></td>
              </tr>
              <tr>
                <td width="26%" align="right" valign="top"><div class="tableheadtext">${uiLabelMap.AccountingEFTAccountInformation}</div></td>
                <td width="5">&nbsp;</td>
                <td width="74%">&nbsp;</td>
              </tr>
              <tr>
                <td width="26%" align="right" valign="middle"><div>${uiLabelMap.AccountingNameOnAccount}</div></td>
                <td width="5">&nbsp;</td>
                <td width="74%">
                  <input type="text" class="inputBox" size="30" maxlength="60" name="nameOnAccount" value="${eftAccount.nameOnAccount!}"/>
                *</td>
              </tr>
              <tr>
                <td width="26%" align="right" valign="middle"><div>${uiLabelMap.AccountingCompanyNameOnAccount}</div></td>
                <td width="5">&nbsp;</td>
                <td width="74%">
                  <input type="text" class="inputBox" size="30" maxlength="60" name="companyNameOnAccount" value="${eftAccount.companyNameOnAccount!}"/>
                </td>
              </tr>
              <tr>
                <td width="26%" align="right" valign="middle"><div>${uiLabelMap.AccountingBankName}</div></td>
                <td width="5">&nbsp;</td>
                <td width="74%">
                  <input type="text" class="inputBox" size="30" maxlength="60" name="bankName" value="${eftAccount.bankName!}"/>
                *</td>
              </tr>
              <tr>
                <td width="26%" align="right" valign="middle"><div>${uiLabelMap.AccountingRoutingNumber}</div></td>
                <td width="5">&nbsp;</td>
                <td width="74%">
                  <input type="text" class="inputBox" size="10" maxlength="30" name="routingNumber" value="${eftAccount.routingNumber!}"/>
                *</td>
              </tr>
              <tr>
                <td width="26%" align="right" valign="middle"><div>${uiLabelMap.AccountingAccountType}</div></td>
                <td width="5">&nbsp;</td>
                <td width="74%">
                  <select name="accountType" class="selectBox">
                    <option>${eftAccount.accountType!}</option>
                    <option></option>
                    <option>Checking</option>
                    <option>Savings</option>
                  </select>
                *</td>
              </tr>
              <tr>
                <td width="26%" align="right" valign="middle"><div>${uiLabelMap.AccountingAccountNumber}</div></td>
                <td width="5">&nbsp;</td>
                <td width="74%">
                  <input type="text" class="inputBox" size="20" maxlength="40" name="accountNumber" value="${eftAccount.accountNumber!}"/>
                *</td>
              </tr>
              <tr>
                <td width="26%" align="right" valign="middle"><div>${uiLabelMap.CommonDescription}</div></td>
                <td width="5">&nbsp;</td>
                <td width="74%">
                  <input type="text" class="inputBox" size="30" maxlength="60" name="description" value="${eftAccount.description!}"/>
                </td>
              </tr>
            </#if>

            <#-- gift card fields -->
            <#if requestParameters.addGiftCard?default("") == "Y" || paymentMethodTypeId! == "GIFT_CARD">
              <input type="hidden" name="addGiftCard" value="Y"/>
              <#assign giftCard = giftCard!>
              <#if paymentMethodTypeId! != "GIFT_CARD">
                <tr>
                  <td colspan="3"><hr /></td>
                </tr>
              </#if>
              <tr>
                <td width="26%" align="right" valign="top"><div class="tableheadtext">${uiLabelMap.AccountingGiftCardInformation}</div></td>
                <td width="5">&nbsp;</td>
                <td width="74%">&nbsp;</td>
              </tr>
              <tr>
                <td width="26%" align="right" valign="middle"><div>${uiLabelMap.AccountingGiftCardNumber}</div></td>
                <td width="5">&nbsp;</td>
                <td width="74%">
                  <input type="text" class="inputBox" size="20" maxlength="60" name="giftCardNumber" value="${giftCard.cardNumber!}"/>
                *</td>
              </tr>
              <tr>
                <td width="26%" align="right" valign="middle"><div>${uiLabelMap.AccountingPINNumber}</div></td>
                <td width="5">&nbsp;</td>
                <td width="74%">
                  <input type="text" class="inputBox" size="10" maxlength="60" name="giftCardPin" value="${giftCard.pinNumber!}"/>
                *</td>
              </tr>
              <tr>
                <td width="26%" align="right" valign="middle"><div>${uiLabelMap.CommonDescription}</div></td>
                <td width="5">&nbsp;</td>
                <td width="74%">
                  <input type="text" class="inputBox" size="30" maxlength="60" name="description" value="${giftCard.description!}"/>
                </td>
              </tr>
              <#if paymentMethodTypeId! != "GIFT_CARD">
                <tr>
                  <td width="26%" align="right" valign="middle"><div>${uiLabelMap.AccountingAmountToUse}</div></td>
                  <td width="5">&nbsp;</td>
                  <td width="74%">
                    <input type="text" class="inputBox" size="5" maxlength="10" name="giftCardAmount" value="${giftCard.pinNumber!}"/>
                  *</td>
                </tr>
              </#if>
            </#if>

            <tr>
              <td align="center" colspan="3">
                <input type="submit" class="smallsubmit" value="Continue"/>
              </td>
            </tr>
          </table>
        </form>
    </div>
</div>
