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

<script language="JavaScript" type="text/javascript">
function shipBillAddr() {
    <#if requestParameters.singleUsePayment?default("N") == "Y">
      <#assign singleUse = "&amp;singleUsePayment=Y">
    <#else>
      <#assign singleUse = "">
    </#if>
    if (document.billsetupform.useShipAddr.checked) {
        window.location.replace("setBilling?createNew=Y&amp;finalizeMode=payment&amp;useGc=${requestParameters.useGc?if_exists}&amp;paymentMethodType=${paymentMethodType?if_exists}&amp;useShipAddr=Y${singleUse}");
    } else {
        window.location.replace("setBilling?createNew=Y&amp;finalizeMode=payment&amp;useGc=${requestParameters.useGc?if_exists}&amp;paymentMethodType=${paymentMethodType?if_exists}${singleUse}");
    }
}
</script>

<div class="screenlet">
    <div class="screenlet-title-bar">
        <div class="boxlink">
            <#if requestParameters.singleUsePayment?default("N") != "Y">
              <div class="tabletext">
                ${screens.render(anonymoustrailScreen)}
              </div>
            </#if>
        </div>
        <div class='h3'>${uiLabelMap.AccountingPaymentInformation}</div>
    </div>
    <div class="screenlet-body">
        <#if (paymentMethodType?exists && !requestParameters.resetType?has_content) || finalizeMode?default("") == "payment">
          <#-- after initial screen; show detailed screens for selected type -->
          <#if paymentMethodType == "CC">
            <#if creditCard?has_content && postalAddress?has_content>
              <form method="post" action="<@ofbizUrl>changeCreditCardAndBillingAddress</@ofbizUrl>" name="billsetupform">
                <input type="hidden" name="paymentMethodId" value="${creditCard.paymentMethodId?if_exists}" />
                <input type="hidden" name="contactMechId" value="${postalAddress.contactMechId?if_exists}" />
            <#elseif requestParameters.useShipAddr?exists>
              <form method="post" action="<@ofbizUrl>enterCreditCard</@ofbizUrl>" name="billsetupform">
            <#else>
              <form method="post" action="<@ofbizUrl>enterCreditCardAndBillingAddress</@ofbizUrl>" name="billsetupform">
            </#if>
          </#if>
          <#if paymentMethodType == "EFT">
            <#if eftAccount?has_content && postalAddress?has_content>
              <form method="post" action="<@ofbizUrl>changeEftAccountAndBillingAddress</@ofbizUrl>" name="billsetupform">
                <input type="hidden" name="paymentMethodId" value="${eftAccount.paymentMethodId?if_exists}" />
                <input type="hidden" name="contactMechId" value="${postalAddress.contactMechId?if_exists}" />
            <#elseif requestParameters.useShipAddr?exists>
              <form method="post" action="<@ofbizUrl>enterEftAccount</@ofbizUrl>" name="billsetupform">
            <#else>
              <form method="post" action="<@ofbizUrl>enterEftAccountAndBillingAddress</@ofbizUrl>" name="billsetupform">
            </#if>
          </#if>
          <#if paymentMethodType == "GC">
            <form method="post" action="<@ofbizUrl>finalizeOrder</@ofbizUrl>" name="billsetupform">
          </#if>

          <#if requestParameters.singleUsePayment?default("N") == "Y">
            <input type="hidden" name="singleUsePayment" value="Y" />
            <input type="hidden" name="appendPayment" value="Y" />
          </#if>

          <input type="hidden" name="contactMechTypeId" value="POSTAL_ADDRESS" />
          <input type="hidden" name="partyId" value="${partyId}" />
          <input type="hidden" name="paymentMethodType" value="${paymentMethodType}" />
          <input type="hidden" name="finalizeMode" value="payment" />
          <input type="hidden" name="createNew" value="Y" />
          <#if requestParameters.useShipAddr?exists>
            <input type="hidden" name="contactMechId" value="${postalFields.contactMechId}" />
          </#if>

          <table width="100%" border="0" cellpadding="1" cellspacing="0">
            <#if cart.getShippingContactMechId()?exists && paymentMethodType != "GC">
              <tr>
                <td width="26%" align="right"= valign="top">
                  <input type="checkbox" name="useShipAddr" value="Y" onClick="javascript:shipBillAddr();" <#if requestParameters.useShipAddr?exists>checked</#if> />
                </td>
                <td colspan="2" valign="center">
                  <div class="tabletext">${uiLabelMap.FacilityBillingAddressSameShipping}</div>
                </td>
              </tr>
              <tr>
                <td colspan="3"><hr /></td>
              </tr>
            </#if>

            <#if (paymentMethodType == "CC" || paymentMethodType == "EFT")>
              <tr>
                <td width="26%" align="right" valign="top"><div class="tableheadtext">${uiLabelMap.PartyBillingAddress}</div></td>
                <td width="5">&nbsp;</td>
                <td width="74%">&nbsp;</td>
              </tr>
              ${screens.render("component://ecommerce/widget/OrderScreens.xml#genericaddress")}
            </#if>

            <#-- credit card fields -->
            <#if paymentMethodType == "CC">
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
            <#if paymentMethodType =="EFT">
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
                <td width="26%" align="right" valign=middle><div class="tabletext">${uiLabelMap.AccountingNameOnAccount}</div></td>
                <td width="5">&nbsp;</td>
                <td width="74%">
                  <input type="text" class="inputBox" size="30" maxlength="60" name="nameOnAccount" value="${eftAccount.nameOnAccount?if_exists}" />
                *</td>
              </tr>
              <tr>
                <td width="26%" align="right" valign=middle><div class="tabletext">${uiLabelMap.AccountingCompanyNameOnAccount}</div></td>
                <td width="5">&nbsp;</td>
                <td width="74%">
                  <input type="text" class="inputBox" size="30" maxlength="60" name="companyNameOnAccount" value="${eftAccount.companyNameOnAccount?if_exists}" />
                </td>
              </tr>
              <tr>
                <td width="26%" align="right" valign=middle><div class="tabletext">${uiLabelMap.AccountingBankName}</div></td>
                <td width="5">&nbsp;</td>
                <td width="74%">
                  <input type="text" class="inputBox" size="30" maxlength="60" name="bankName" value="${eftAccount.bankName?if_exists}" />
                *</td>
              </tr>
              <tr>
                <td width="26%" align="right" valign=middle><div class="tabletext">${uiLabelMap.AccountingRoutingNumber}</div></td>
                <td width="5">&nbsp;</td>
                <td width="74%">
                  <input type="text" class="inputBox" size="10" maxlength="30" name="routingNumber" value="${eftAccount.routingNumber?if_exists}" />
                *</td>
              </tr>
              <tr>
                <td width="26%" align="right" valign=middle><div class="tabletext">${uiLabelMap.AccountingAccountType}</div></td>
                <td width="5">&nbsp;</td>
                <td width="74%">
                  <select name="accountType" class='selectBox'>
                    <option>${eftAccount.accountType?if_exists}</option>
                    <option></option>
                    <option>Checking</option>
                    <option>Savings</option>
                  </select>
                *</td>
              </tr>
              <tr>
                <td width="26%" align="right" valign=middle><div class="tabletext">${uiLabelMap.AccountingAccountNumber}</div></td>
                <td width="5">&nbsp;</td>
                <td width="74%">
                  <input type="text" class="inputBox" size="20" maxlength="40" name="accountNumber" value="${eftAccount.accountNumber?if_exists}" />
                *</td>
              </tr>
              <tr>
                <td width="26%" align="right" valign=middle><div class="tabletext">${uiLabelMap.CommonDescription}</div></td>
                <td width="5">&nbsp;</td>
                <td width="74%">
                  <input type="text" class="inputBox" size="30" maxlength="60" name="description" value="${eftAccount.description?if_exists}" />
                </td>
              </tr>
            </#if>

            <#-- gift card fields -->
            <#if requestParameters.useGc?default("") == "GC" || paymentMethodType == "GC">
              <#assign giftCard = requestParameters>
              <input type="hidden" name="addGiftCard" value="Y" />
              <#if paymentMethodType != "GC">
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
                <td width="26%" align="right" valign=middle><div class="tabletext">${uiLabelMap.AccountingGiftCardNumber}</div></td>
                <td width="5">&nbsp;</td>
                <td width="74%">
                  <input type="text" class="inputBox" size="20" maxlength="60" name="giftCardNumber" value="${giftCard.cardNumber?if_exists}" />
                *</td>
              </tr>
              <tr>
                <td width="26%" align="right" valign=middle><div class="tabletext">${uiLabelMap.AccountingPINNumber}</div></td>
                <td width="5">&nbsp;</td>
                <td width="74%">
                  <input type="text" class="inputBox" size="10" maxlength="60" name="giftCardPin" value="${giftCard.pinNumber?if_exists}" />
                *</td>
              </tr>
              <tr>
                <td width="26%" align="right" valign=middle><div class="tabletext">${uiLabelMap.CommonDescription}</div></td>
                <td width="5">&nbsp;</td>
                <td width="74%">
                  <input type="text" class="inputBox" size="30" maxlength="60" name="description" value="${giftCard.description?if_exists}" />
                </td>
              </tr>
              <#if paymentMethodType != "GC">
                <tr>
                  <td width="26%" align="right" valign=middle><div class="tabletext">${uiLabelMap.AccountingAmountToUse}</div></td>
                  <td width="5">&nbsp;</td>
                  <td width="74%">
                    <input type="text" class="inputBox" size="5" maxlength="10" name="giftCardAmount" value="${giftCard.pinNumber?if_exists}" />
                  *</td>
                </tr>
              </#if>
            </#if>

            <tr>
              <td align="center" colspan="3">
                <input type="submit" class="smallsubmit" value="Continue" />
              </td>
            </tr>
          </table>
        <#else>
          <#-- initial screen show a list of options -->
          <form method="post" action="<@ofbizUrl>finalizeOrder</@ofbizUrl>" name="billsetupform">
            <input type="hidden" name="finalizeMode" value="payment" />
            <input type="hidden" name="createNew" value="Y" />
            <table width="100%" border="0" cellpadding="1" cellspacing="0">
              <#if productStorePaymentMethodTypeIdMap.GIFT_CARD?exists>
              <tr>
                <td width='5%' nowrap="nowrap"><input type="checkbox" name="useGc" value="GC" <#if paymentMethodType?exists && paymentMethodType == "GC">checked="checked"</#if> /></td>
                <td width='95%' nowrap="nowrap"><div class="tabletext">${uiLabelMap.AccountingCheckGiftCard}</div></td>
              </tr>
              <tr><td colspan="2"><hr /></td></tr>
              </#if>
              <#if productStorePaymentMethodTypeIdMap.EXT_OFFLINE?exists>
              <tr>
                <td width='5%' nowrap="nowrap"><input type="radio" name="paymentMethodType" value="offline" <#if paymentMethodType?exists && paymentMethodType == "offline">checked="checked"</#if> /></td>
                <td width='95%'nowrap="nowrap"><div class="tabletext">${uiLabelMap.OrderPaymentOfflineCheckMoney}</div></td>
              </tr>
              <tr><td colspan="2"><hr /></td></tr>
              </#if>
              <#if productStorePaymentMethodTypeIdMap.CREDIT_CARD?exists>
              <tr>
                <td width='5%' nowrap="nowrap"><input type="radio" name="paymentMethodType" value="CC" <#if paymentMethodType?exists && paymentMethodType == "CC">checked="checked"</#if> /></td>
                <td width='95%' nowrap="nowrap"><div class="tabletext">${uiLabelMap.AccountingVisaMastercardAmexDiscover}</div></td>
              </tr>
              <tr><td colspan="2"><hr /></td></tr>
              </#if>
              <#if productStorePaymentMethodTypeIdMap.EFT_ACCOUNT?exists>
              <tr>
                <td width='5%' nowrap="nowrap"><input type="radio" name="paymentMethodType" value="EFT" <#if paymentMethodType?exists && paymentMethodType == "EFT">checked="checked"</#if> /></td>
                <td width='95%' nowrap="nowrap"><div class="tabletext">${uiLabelMap.AccountingAHCElectronicCheck}</div></td>
              </tr>
              </#if>
              <tr>
                <td align="center" colspan="2">
                  <input type="submit" class="smallsubmit" value="${uiLabelMap.CommonContinue}" />
                </td>
              </tr>
            </table>
          </form>
        </#if>
    </div>
</div>
