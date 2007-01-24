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
   <#assign paymentMethodTypeId = "${requestParameters.paymentMethodTypeId?if_exists}">
</#if>
<script language="JavaScript" type="text/javascript">
function shipBillAddr() {
    <#if requestParameters.singleUsePayment?default("N") == "Y">
      <#assign singleUse = "&singleUsePayment=Y">
    <#else>
      <#assign singleUse = "">
    </#if>
    if (document.billsetupform.useShipAddr.checked) {
        window.location.replace("setPaymentInformation?createNew=Y&addGiftCard=${requestParameters.addGiftCard?if_exists}&paymentMethodTypeId=${paymentMethodTypeId?if_exists}&useShipAddr=Y${singleUse}");
    } else {
        window.location.replace("setPaymentInformation?createNew=Y&addGiftCard=${requestParameters.addGiftCard?if_exists}&paymentMethodTypeId=${paymentMethodTypeId?if_exists}${singleUse}");
    }
}
</script>

<div class="screenlet">
    <div class="screenlet-header">
        <div class='boxhead'>&nbsp;${uiLabelMap.AccountingPaymentInformation}</div>
    </div>
    <div class="screenlet-body">
          <#-- after initial screen; show detailed screens for selected type -->
          <#if paymentMethodTypeId?if_exists == "CREDIT_CARD">
            <#if creditCard?has_content && postalAddress?has_content && !requestParameters.useShipAddr?exists>
              <form method="post" action="<@ofbizUrl>changeCreditCardAndBillingAddress</@ofbizUrl>" name="${parameters.formNameValue}">
                <input type="hidden" name="paymentMethodId" value="${creditCard.paymentMethodId?if_exists}"/>
                <input type="hidden" name="contactMechId" value="${postalAddress.contactMechId?if_exists}"/>
            <#elseif requestParameters.useShipAddr?exists>
              <form method="post" action="<@ofbizUrl>enterCreditCard</@ofbizUrl>" name="${parameters.formNameValue}">
            <#else>
              <form method="post" action="<@ofbizUrl>enterCreditCardAndBillingAddress</@ofbizUrl>" name="${parameters.formNameValue}">
            </#if>
          </#if>
          <#if paymentMethodTypeId?if_exists == "EFT_ACCOUNT">
            <#if eftAccount?has_content && postalAddress?has_content>
              <form method="post" action="<@ofbizUrl>changeEftAccountAndBillingAddress</@ofbizUrl>" name="${parameters.formNameValue}">
                <input type="hidden" name="paymentMethodId" value="${eftAccount.paymentMethodId?if_exists}"/>
                <input type="hidden" name="contactMechId" value="${postalAddress.contactMechId?if_exists}"/>
            <#elseif requestParameters.useShipAddr?exists>
              <form method="post" action="<@ofbizUrl>enterEftAccount</@ofbizUrl>" name="${parameters.formNameValue}">
            <#else>
              <form method="post" action="<@ofbizUrl>enterEftAccountAndBillingAddress</@ofbizUrl>" name="${parameters.formNameValue}">
            </#if>
          </#if>
          <#if paymentMethodTypeId?if_exists == "GIFT_CARD"> <#--Don't know much how this is handled -->
            <form method="post" action="<@ofbizUrl>enterGiftCard</@ofbizUrl>" name="${parameters.formNameValue}">
          </#if>

          <#if requestParameters.singleUsePayment?default("N") == "Y">
            <input type="hidden" name="singleUsePayment" value="Y"/>
            <input type="hidden" name="appendPayment" value="Y"/>
          </#if>
          <input type="hidden" name="contactMechTypeId" value="POSTAL_ADDRESS"/>
          <input type="hidden" name="partyId" value="${partyId}"/>
          <input type="hidden" name="paymentMethodTypeId" value="${paymentMethodTypeId?if_exists}"/>
          <input type="hidden" name="createNew" value="Y"/>
          <#if requestParameters.useShipAddr?exists>
            <input type="hidden" name="contactMechId" value="${parameters.contactMechId?if_exists}"/>
          </#if>

          <table width="100%" border="0" cellpadding="1" cellspacing="0">
            <#if cart.getShippingContactMechId()?exists && paymentMethodTypeId?if_exists != "GIFT_CARD">
              <tr>
                <td width="26%" align="right"= valign="top">
                  <input type="checkbox" name="useShipAddr" value="Y" onClick="javascript:shipBillAddr();" <#if useShipAddr?exists>checked</#if>/>
                </td>
                <td colspan="2" align="left" valign="center">
                  <div class="tabletext">${uiLabelMap.FacilityBillingAddressSameShipping}</div>
                </td>
              </tr>
              <tr>
                <td colspan="3"><hr class="sepbar"/></td>
              </tr>
            </#if>

            <#if (paymentMethodTypeId?if_exists == "CREDIT_CARD" || paymentMethodTypeId?if_exists == "EFT_ACCOUNT")>
              <tr>
                <td width="26%" align="right" valign="top"><div class="tableheadtext">${uiLabelMap.PartyBillingAddress}</div></td>
                <td width="5">&nbsp;</td>
                <td width="74%">&nbsp;</td>
              </tr>
              ${screens.render("component://ecommerce/widget/OrderScreens.xml#genericaddress")}
            </#if>

            <#-- credit card fields -->
            <#if paymentMethodTypeId?if_exists == "CREDIT_CARD">
              <#if !creditCard?has_content>
                <#assign creditCard = requestParameters>
              </#if>
              <tr>
                <td colspan="3"><hr class="sepbar"/></td>
              </tr>
              <tr>
                <td width="26%" align="right" valign="top"><div class="tableheadtext">${uiLabelMap.AccountingCreditCardInformation}</div></td>
                <td width="5">&nbsp;</td>
                <td width="74%">&nbsp;</td>
              </tr>

              <tr>
                <td width="26%" align="right" valign=middle><div class="tabletext">${uiLabelMap.AccountingCompanyNameOnCard}</div></td>
                <td width="5">&nbsp;</td>
                <td width="74%">
                  <input type="text" class='inputBox' size="30" maxlength="60" name="companyNameOnCard" value="${creditCard.companyNameOnCard?if_exists}"/>
                </td>
              </tr>
              <tr>
                <td width="26%" align="right" valign=middle><div class="tabletext">${uiLabelMap.AccountingPrefixCard}</div></td>
                <td width="5">&nbsp;</td>
                <td width="74%">
                  <select name="titleOnCard" class="selectBox">
                    <option value="">Select One</option>
                    <option<#if ((creditCard.titleOnCard)?default("") == "Mr.")> selected</#if>>${uiLabelMap.CommonTitleMr}</option>
                    <option<#if ((creditCard.titleOnCard)?default("") == "Mrs.")> selected</#if>>${uiLabelMap.CommonTitleMrs}</option>
                    <option<#if ((creditCard.titleOnCard)?default("") == "Ms.")> selected</#if>>${uiLabelMap.CommonTitleMs}</option>
                    <option<#if ((creditCard.titleOnCard)?default("") == "Dr.")> selected</#if>>${uiLabelMap.CommonTitleDr}</option>
                  </select>
                </td>
              </tr>
              <tr>
                <td width="26%" align="right" valign=middle><div class="tabletext">${uiLabelMap.AccountingFirstNameCard}</div></td>
                <td width="5">&nbsp;</td>
                <td width="74%">
                  <input type="text" class="inputBox" size="20" maxlength="60" name="firstNameOnCard" value="${(creditCard.firstNameOnCard)?if_exists}"/>
                *</td>
              </tr>
              <tr>
                <td width="26%" align="right" valign=middle><div class="tabletext">${uiLabelMap.AccountingMiddleNameCard}</div></td>
                <td width="5">&nbsp;</td>
                <td width="74%">
                  <input type="text" class="inputBox" size="15" maxlength="60" name="middleNameOnCard" value="${(creditCard.middleNameOnCard)?if_exists}"/>
                </td>
              </tr>
              <tr>
                <td width="26%" align="right" valign=middle><div class="tabletext">${uiLabelMap.AccountingLastNameCard}</div></td>
                <td width="5">&nbsp;</td>
                <td width="74%">
                  <input type="text" class="inputBox" size="20" maxlength="60" name="lastNameOnCard" value="${(creditCard.lastNameOnCard)?if_exists}"/>
                *</td>
              </tr>
              <tr>
                <td width="26%" align="right" valign=middle><div class="tabletext">${uiLabelMap.AccountingSuffixCard}</div></td>
                <td width="5">&nbsp;</td>
                <td width="74%">
                  <select name="suffixOnCard" class="selectBox">
                    <option value="">Select One</option>
                    <option<#if ((creditCard.suffixOnCard)?default("") == "Jr.")> selected</#if>>Jr.</option>
                    <option<#if ((creditCard.suffixOnCard)?default("") == "Sr.")> selected</#if>>Sr.</option>
                    <option<#if ((creditCard.suffixOnCard)?default("") == "I")> selected</#if>>I</option>
                    <option<#if ((creditCard.suffixOnCard)?default("") == "II")> selected</#if>>II</option>
                    <option<#if ((creditCard.suffixOnCard)?default("") == "III")> selected</#if>>III</option>
                    <option<#if ((creditCard.suffixOnCard)?default("") == "IV")> selected</#if>>IV</option>
                    <option<#if ((creditCard.suffixOnCard)?default("") == "V")> selected</#if>>V</option>
                  </select>
                </td>
              </tr>

              <tr>
                <td width="26%" align="right" valign=middle><div class="tabletext">${uiLabelMap.AccountingCardType}</div></td>
                <td width="5">&nbsp;</td>
                <td width="74%">
                  <select name="cardType" class="selectBox">
                    <#if creditCard.cardType?exists>
                      <option>${creditCard.cardType}</option>
                      <option value="${creditCard.cardType}">---</option>
                    </#if>
                    ${screens.render("component://common/widget/CommonScreens.xml#cctypes")}
                  </select>
                *</td>
              </tr>
              <tr>
                <td width="26%" align="right" valign=middle><div class="tabletext">${uiLabelMap.AccountingCardNumber}</div></td>
                <td width="5">&nbsp;</td>
                <td width="74%">
                  <input type="text" class="inputBox" size="20" maxlength="30" name="cardNumber" value="${creditCard.cardNumber?if_exists}"/>
                *</td>
              </tr>
              <#--<tr>
                <td width="26%" align="right" valign=middle><div class="tabletext">${uiLabelMap.AccountingCardSecurityCode}</div></td>
                <td width="5">&nbsp;</td>
                <td width="74%">
                  <input type="text" size="5" maxlength="10" name="cardSecurityCode" value="">
                </td>
              </tr>-->
              <tr>
                <td width="26%" align="right" valign=middle><div class="tabletext">${uiLabelMap.AccountingExpirationDate}</div></td>
                <td width="5">&nbsp;</td>
                <td width="74%">
                  <#assign expMonth = "">
                  <#assign expYear = "">
                  <#if creditCard?exists && creditCard.expireDate?exists>
                    <#assign expDate = creditCard.expireDate>
                    <#if (expDate?exists && expDate.indexOf("/") > 0)>
                      <#assign expMonth = expDate.substring(0,expDate.indexOf("/"))>
                      <#assign expYear = expDate.substring(expDate.indexOf("/")+1)>
                    </#if>
                  </#if>
                  <select name="expMonth" class='selectBox'>
                    <#if creditCard?has_content && expMonth?has_content>
                      <#assign ccExprMonth = expMonth>
                    <#else>
                      <#assign ccExprMonth = requestParameters.expMonth?if_exists>
                    </#if>
                    <#if ccExprMonth?has_content>
                      <option value="${ccExprMonth?if_exists}">${ccExprMonth?if_exists}</option>
                    </#if>
                    ${screens.render("component://common/widget/CommonScreens.xml#ccmonths")}
                  </select>
                  <select name="expYear" class='selectBox'>
                    <#if creditCard?has_content && expYear?has_content>
                      <#assign ccExprYear = expYear>
                    <#else>
                      <#assign ccExprYear = requestParameters.expYear?if_exists>
                    </#if>
                    <#if ccExprYear?has_content>
                      <option value="${ccExprYear?if_exists}">${ccExprYear?if_exists}</option>
                    </#if>
                    ${screens.render("component://common/widget/CommonScreens.xml#ccyears")}
                  </select>
                *</td>
              </tr>
              <tr>
                <td width="26%" align="right" valign=middle><div class="tabletext">${uiLabelMap.CommonDescription}</div></td>
                <td width="5">&nbsp;</td>
                <td width="74%">
                  <input type="text" class="inputBox" size="20" maxlength="30" name="description" value="${creditCard.description?if_exists}"/>
                </td>
              </tr>
            </#if>

            <#-- eft fields -->
            <#if paymentMethodTypeId?if_exists =="EFT_ACCOUNT">
              <#if !eftAccount?has_content>
                <#assign eftAccount = requestParameters>
              </#if>
              <tr>
                <td colspan="3"><hr class="sepbar"/></td>
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
                  <input type="text" class="inputBox" size="30" maxlength="60" name="nameOnAccount" value="${eftAccount.nameOnAccount?if_exists}"/>
                *</td>
              </tr>
              <tr>
                <td width="26%" align="right" valign=middle><div class="tabletext">${uiLabelMap.AccountingCompanyNameOnAccount}</div></td>
                <td width="5">&nbsp;</td>
                <td width="74%">
                  <input type="text" class="inputBox" size="30" maxlength="60" name="companyNameOnAccount" value="${eftAccount.companyNameOnAccount?if_exists}"/>
                </td>
              </tr>
              <tr>
                <td width="26%" align="right" valign=middle><div class="tabletext">${uiLabelMap.AccountingBankName}</div></td>
                <td width="5">&nbsp;</td>
                <td width="74%">
                  <input type="text" class="inputBox" size="30" maxlength="60" name="bankName" value="${eftAccount.bankName?if_exists}"/>
                *</td>
              </tr>
              <tr>
                <td width="26%" align="right" valign=middle><div class="tabletext">${uiLabelMap.AccountingRoutingNumber}</div></td>
                <td width="5">&nbsp;</td>
                <td width="74%">
                  <input type="text" class="inputBox" size="10" maxlength="30" name="routingNumber" value="${eftAccount.routingNumber?if_exists}"/>
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
                  <input type="text" class="inputBox" size="20" maxlength="40" name="accountNumber" value="${eftAccount.accountNumber?if_exists}"/>
                *</td>
              </tr>
              <tr>
                <td width="26%" align="right" valign=middle><div class="tabletext">${uiLabelMap.CommonDescription}</div></td>
                <td width="5">&nbsp;</td>
                <td width="74%">
                  <input type="text" class="inputBox" size="30" maxlength="60" name="description" value="${eftAccount.description?if_exists}"/>
                </td>
              </tr>
            </#if>

            <#-- gift card fields -->
            <#if requestParameters.addGiftCard?default("") == "Y" || paymentMethodTypeId?if_exists == "GIFT_CARD">
              <input type="hidden" name="addGiftCard" value="Y"/>
              <#assign giftCard = giftCard?if_exists>
              <#if paymentMethodTypeId?if_exists != "GIFT_CARD">
                <tr>
                  <td colspan="3"><hr class="sepbar"/></td>
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
                  <input type="text" class="inputBox" size="20" maxlength="60" name="giftCardNumber" value="${giftCard.cardNumber?if_exists}"/>
                *</td>
              </tr>
              <tr>
                <td width="26%" align="right" valign=middle><div class="tabletext">${uiLabelMap.AccountingPINNumber}</div></td>
                <td width="5">&nbsp;</td>
                <td width="74%">
                  <input type="text" class="inputBox" size="10" maxlength="60" name="giftCardPin" value="${giftCard.pinNumber?if_exists}"/>
                *</td>
              </tr>
              <tr>
                <td width="26%" align="right" valign=middle><div class="tabletext">${uiLabelMap.CommonDescription}</div></td>
                <td width="5">&nbsp;</td>
                <td width="74%">
                  <input type="text" class="inputBox" size="30" maxlength="60" name="description" value="${giftCard.description?if_exists}"/>
                </td>
              </tr>
              <#if paymentMethodTypeId?if_exists != "GIFT_CARD">
                <tr>
                  <td width="26%" align="right" valign=middle><div class="tabletext">${uiLabelMap.AccountingAmountToUse}</div></td>
                  <td width="5">&nbsp;</td>
                  <td width="74%">
                    <input type="text" class="inputBox" size="5" maxlength="10" name="giftCardAmount" value="${giftCard.pinNumber?if_exists}"/>
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
    </div>
</div>
