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

<script type="text/javascript">
//<![CDATA[
function shipBillAddr() {
    if (document.checkoutsetupform.useShipAddr.checked) {
        window.location = "<@ofbizUrl>setBilling?createNew=Y&finalizeMode=payment&paymentMethodType=${paymentMethodType!}&useShipAddr=Y</@ofbizUrl>";
    } else {
        window.location = "<@ofbizUrl>setBilling?createNew=Y&finalizeMode=payment&paymentMethodType=${paymentMethodType!}</@ofbizUrl>";
    }
}

function makeExpDate() {
    document.checkoutsetupform.expireDate.value = document.checkoutsetupform.expMonth.options[document.checkoutsetupform.expMonth.selectedIndex].value + "/" + document.checkoutsetupform.expYear.options[document.checkoutsetupform.expYear.selectedIndex].value;
}
//]]>
</script>

<#if security.hasEntityPermission("ORDERMGR", "_CREATE", session) || security.hasEntityPermission("ORDERMGR", "_PURCHASE_CREATE", session)>
<div class="screenlet">
    <div class="screenlet-body">
        <#if request.getAttribute("paymentMethodId")?? || ( (paymentMethodList?has_content || billingAccountList?has_content) && !requestParameters.createNew??)>
          <#-- initial screen when we have a associated party -->
          <form method="post" action="<@ofbizUrl>finalizeOrder</@ofbizUrl>" name="checkoutsetupform">
            <input type="hidden" name="finalizeMode" value="payment"/>
            <table width="100%" cellpadding="1" cellspacing="0" border="0">
              <tr>
                <td colspan="2">
                  <a href="<@ofbizUrl>setBilling?createNew=Y</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonCreateNew}</a>
                </td>
              </tr>
              <tr><td colspan="3"><hr /></td></tr>
              <#if billingAccountList?has_content>
                <tr>
                  <td>
                    <span>${uiLabelMap.FormFieldTitle_billingAccountId}</span>
                    <select name="billingAccountId">
                      <option value=""></option>
                        <#list billingAccountList as billingAccount>
                          <#assign availableAmount = billingAccount.accountBalance?double>
                          <#if (billingAccount.accountLimit)??>
                              <#assign accountLimit = billingAccount.accountLimit?double />
                          <#else>
                              <#assign accountLimit = 0.00 />
                          </#if> 
                          <option value="${billingAccount.billingAccountId}" <#if billingAccount.billingAccountId == selectedBillingAccountId?default("")>selected="selected"</#if>>${billingAccount.description?default("")} [${billingAccount.billingAccountId}] Available: <@ofbizCurrency amount=availableAmount isoCode=billingAccount.accountCurrencyUomId/> Limit: <@ofbizCurrency amount=accountLimit isoCode=billingAccount.accountCurrencyUomId/></option>
                        </#list>
                    </select>
                  </td>
                  <td>&nbsp;</td>
                </tr>
                <tr>
                  <td>
                    ${uiLabelMap.OrderBillUpTo}
                    <input type="text" size="5" name="billingAccountAmount" value=""/>
                  </td>
                  <td>&nbsp;</td>
                </tr>
                <tr><td colspan="3"><hr /></td></tr>
              </#if>
              <tr>
                <td>
                  <label for="checkOutPaymentId_EXT_OFFLINE">
                  <input type="radio" id="checkOutPaymentId_EXT_OFFLINE" name="checkOutPaymentId" value="EXT_OFFLINE" <#if checkOutPaymentId?? && "EXT_OFFLINE" == checkOutPaymentId>checked="checked"</#if>/>
                  ${uiLabelMap.OrderPaymentOfflineCheckMoney}</label>
                </td>
              </tr>
             <tr><td colspan="3"><hr /></td></tr>
              <tr>
                <td>
                  <label for="checkOutPaymentId_EXT_COD">
                  <input type="radio" id="checkOutPaymentId_EXT_COD" name="checkOutPaymentId" value="EXT_COD" <#if checkOutPaymentId?? && "EXT_COD" == checkOutPaymentId>checked="checked"</#if>/>
                  ${uiLabelMap.OrderCOD}</label>
                </td>
              </tr>
             <tr><td colspan="3"><hr /></td></tr>
              <#if paymentMethodList?has_content>
                <#list paymentMethodList as paymentMethod>
                  <#if "CREDIT_CARD" == paymentMethod.paymentMethodTypeId>
                    <#assign creditCard = paymentMethod.getRelatedOne("CreditCard", false)>
                    <tr>
                      <td>
                        <label for="checkOutPaymentId_CREDIT_CARD_${paymentMethod.paymentMethodId}">
                        <input type="radio" id="checkOutPaymentId_CREDIT_CARD_${paymentMethod.paymentMethodId}" name="checkOutPaymentId" value="${paymentMethod.paymentMethodId}" <#if checkOutPaymentId?? && paymentMethod.paymentMethodId == checkOutPaymentId>checked="checked"</#if>/>
                          CC:&nbsp;${Static["org.apache.ofbiz.party.contact.ContactHelper"].formatCreditCard(creditCard)}
                          <#if paymentMethod.description?has_content>(${paymentMethod.description})</#if>
                        </label>
                        <span>
                          ${uiLabelMap.OrderCardSecurityCode}&nbsp;<input type="text" size="5" maxlength="10" name="securityCode_${paymentMethod.paymentMethodId}" value=""/>
                        </span>
                      </td>
                      <td align="right"><a href="/partymgr/control/editcreditcard?party_id=${orderParty.partyId}&amp;paymentMethodId=${paymentMethod.paymentMethodId}" target="_blank" class="buttontext">${uiLabelMap.CommonUpdate}</a></td>
                    </tr>
                  <#elseif "EFT_ACCOUNT" == paymentMethod.paymentMethodTypeId>
                    <#assign eftAccount = paymentMethod.getRelatedOne("EftAccount", false)>
                    <tr>
                      <td>
                        <label for="checkOutPaymentId_EFT_ACCOUNT_${paymentMethod.paymentMethodId}">
                        <input type="radio" id="checkOutPaymentId_EFT_ACCOUNT_${paymentMethod.paymentMethodId}" name="checkOutPaymentId" value="${paymentMethod.paymentMethodId}" <#if checkOutPaymentId?? && paymentMethod.paymentMethodId == checkOutPaymentId>checked="checked"</#if>/>
                          EFT:&nbsp;${eftAccount.bankName!}: ${eftAccount.accountNumber!}
                          <#if paymentMethod.description?has_content>(${paymentMethod.description})</#if>
                        </label>
                      </td>
                      <td align="right"><a href="/partymgr/control/editeftaccount?party_id=${orderParty.partyId}&amp;paymentMethodId=${paymentMethod.paymentMethodId}" target="_blank" class="buttontext">${uiLabelMap.CommonUpdate}</a></td>
                    </tr>
                    <tr><td colspan="2"><hr /></td></tr>
                  </#if>
                </#list>
              <#else>
                <div><b>${uiLabelMap.AccountingNoPaymentMethods}</b></div>
              </#if>
            </table>
          </form>
        <#elseif paymentMethodType?? || "payment" == finalizeMode?default("")>
          <#-- after initial screen; show detailed screens for selected type -->
          <#if "CC" == paymentMethodType>
            <#if postalAddress?has_content>
              <form method="post" action="<@ofbizUrl>updateCreditCardAndPostalAddress</@ofbizUrl>" name="checkoutsetupform">
                <input type="hidden" name="paymentMethodId" value="${creditCard.paymentMethodId!}"/>
                <input type="hidden" name="contactMechId" value="${postalAddress.contactMechId!}"/>
            <#elseif requestParameters.useShipAddr??>
              <form method="post" action="<@ofbizUrl>createCreditCardOrderEntry</@ofbizUrl>" name="checkoutsetupform">
            <#else>
              <form method="post" action="<@ofbizUrl>createCreditCardAndPostalAddress</@ofbizUrl>" name="checkoutsetupform">
            </#if>
          </#if>
          <#if "EFT" == paymentMethodType>
            <#if postalAddress?has_content>
              <form method="post" action="<@ofbizUrl>updateEftAndPostalAddress</@ofbizUrl>" name="checkoutsetupform">
                <input type="hidden" name="paymentMethodId" value="${eftAccount.paymentMethodId!}"/>
                <input type="hidden" name="contactMechId" value="${postalAddress.contactMechId!}"/>
            <#elseif requestParameters.useShipAddr??>
              <form method="post" action="<@ofbizUrl>createEftAccount</@ofbizUrl>" name="checkoutsetupform">
            <#else>
              <form method="post" action="<@ofbizUrl>createEftAndPostalAddress</@ofbizUrl>" name="checkoutsetupform">
            </#if>
          </#if>

          <input type="hidden" name="contactMechTypeId" value="POSTAL_ADDRESS"/>
          <input type="hidden" name="partyId" value="${cart.getPartyId()}"/>
          <input type="hidden" name="paymentMethodType" value="${paymentMethodType}"/>
          <input type="hidden" name="finalizeMode" value="payment"/>
          <input type="hidden" name="createNew" value="Y"/>
          <#if requestParameters.useShipAddr??>
            <input type="hidden" name="contactMechId" value="${postalFields.contactMechId}"/>
          </#if>

          <table width="100%" border="0" cellpadding="1" cellspacing="0">
            <#if cart.getShippingContactMechId()??>
            <tr>
              <td align="right"= valign="top">
                <label>
                <input type="checkbox" name="useShipAddr" value="Y" onclick="javascript:shipBillAddr();" <#if requestParameters.useShipAddr??>checked="checked"</#if>/>
                ${uiLabelMap.FacilityBillingAddressSameShipping}
                </label>
              </td>
            </tr>
            <tr>
              <td colspan="3"><hr /></td>
            </tr>
            </#if>

            <#if orderPerson?has_content>
              <#assign toName = "">
              <#if orderPerson.personalTitle?has_content><#assign toName = orderPerson.personalTitle + " "></#if>
              <#assign toName = toName + orderPerson.firstName + " ">
              <#if orderPerson.middleName?has_content><#assign toName = toName + orderPerson.middleName + " "></#if>
              <#assign toName = toName + orderPerson.lastName>
              <#if orderPerson.suffix?has_content><#assign toName = toName + " " + orderPerson.suffix></#if>
            <#else>
              <#assign toName = postalFields.toName?default("")>
            </#if>

            <#-- generic address information -->
            <tr>
              <td width="26%" align="right" valign="top"><div>${uiLabelMap.CommonToName}</div></td>
              <td width="5">&nbsp;</td>
              <td width="74%">
                <input type="text" size="30" maxlength="60" name="toName" value="${toName}" <#if requestParameters.useShipAddr??>disabled="disabled"</#if>/>
              </td>
            </tr>
            <tr>
              <td width="26%" align="right" valign="top"><div>${uiLabelMap.CommonAttentionName}</div></td>
              <td width="5">&nbsp;</td>
              <td width="74%">
                <input type="text" size="30" maxlength="60" name="attnName" value="${postalFields.attnName!}" <#if requestParameters.useShipAddr??>disabled="disabled"</#if>/>
              </td>
            </tr>
            <tr>
              <td width="26%" align="right" valign="top"><div>${uiLabelMap.CommonAddressLine} 1</div></td>
              <td width="5">&nbsp;</td>
              <td width="74%">
                <input type="text" size="30" maxlength="30" name="address1" value="${postalFields.address1!}" <#if requestParameters.useShipAddr??>disabled="disabled"</#if>/>
              *</td>
            </tr>
            <tr>
              <td width="26%" align="right" valign="top"><div>${uiLabelMap.CommonAddressLine} 2</div></td>
              <td width="5">&nbsp;</td>
              <td width="74%">
                <input type="text" size="30" maxlength="30" name="address2" value="${postalFields.address2!}" <#if requestParameters.useShipAddr??>disabled="disabled"</#if>/>
              </td>
            </tr>
            <tr>
              <td width="26%" align="right" valign="top"><div>${uiLabelMap.CommonCity}</div></td>
              <td width="5">&nbsp;</td>
              <td width="74%">
                <input type="text" size="30" maxlength="30" name="city" value="${postalFields.city!}" <#if requestParameters.useShipAddr??>disabled="disabled"</#if>/>
              *</td>
            </tr>
            <tr>
              <td width="26%" align="right" valign="top"><div>${uiLabelMap.CommonStateProvince}</div></td>
              <td width="5">&nbsp;</td>
              <td width="74%">
                <select id="checkoutsetupform_stateProvinceGeoId" name="stateProvinceGeoId" <#if requestParameters.useShipAddr??>disabled="disabled"</#if>>
                  <#if postalFields.stateProvinceGeoId??>
                  <option>${postalFields.stateProvinceGeoId}</option>
                  <option value="${postalFields.stateProvinceGeoId}">---</option>
                  </#if>
                  <option value=""></option>
                  ${screens.render("component://common/widget/CommonScreens.xml#states")}
                </select>
              </td>
            </tr>
            <tr>
              <td width="26%" align="right" valign="top"><div>${uiLabelMap.CommonZipPostalCode}</div></td>
              <td width="5">&nbsp;</td>
              <td width="74%">
                <input type="text" size="12" maxlength="10" name="postalCode" value="${postalFields.postalCode!}" <#if requestParameters.useShipAddr??>disabled="disabled"</#if>/>
              *</td>
            </tr>
            <tr>
              <td width="26%" align="right" valign="top"><div>${uiLabelMap.CommonCountry}</div></td>
              <td width="5">&nbsp;</td>
              <td width="74%">
                <select id="checkoutsetupform_countryGeoId" name="countryGeoId" <#if requestParameters.useShipAddr??>disabled="disabled"</#if>>
                  <#if postalFields.countryGeoId??>
                  <option>${postalFields.countryGeoId}</option>
                  <option value="${postalFields.countryGeoId}">---</option>
                  </#if>
                  ${screens.render("component://common/widget/CommonScreens.xml#countries")}
                </select>
              *</td>
            </tr>

            <#-- credit card fields -->
            <#if "CC" == paymentMethodType>
              <#if !creditCard?has_content>
                <#assign creditCard = requestParameters>
              </#if>
              <input type="hidden" name="expireDate" value="${creditCard.expireDate!}"/>
              <tr>
                <td colspan="3"><hr /></td>
              </tr>

                  <tr>
                <td width="26%" align="right" valign="middle"><div>${uiLabelMap.AccountingCompanyNameCard}</div></td>
                <td width="5">&nbsp;</td>
                <td width="74%">
                  <input type="text" class='inputBox' size="30" maxlength="60" name="companyNameOnCard" value="${creditCard.companyNameOnCard!}"/>
                </td>
              </tr>
              <tr>
                <td width="26%" align="right" valign="middle"><div>${uiLabelMap.AccountingPrefixCard}</div></td>
                <td width="5">&nbsp;</td>
                <td width="74%">
                  <select name="titleOnCard">
                    <option value="">${uiLabelMap.CommonSelectOne}</option>
                    <option<#if ("Mr." == (creditCard.titleOnCard)?default(""))> checked="checked"</#if>>${uiLabelMap.CommonTitleMr}</option>
                    <option<#if ("Mrs." == (creditCard.titleOnCard)?default(""))> checked="checked"</#if>>${uiLabelMap.CommonTitleMrs}</option>
                    <option<#if ("Ms." == (creditCard.titleOnCard)?default(""))> checked="checked"</#if>>${uiLabelMap.CommonTitleMs}</option>
                    <option<#if ("Dr." == (creditCard.titleOnCard)?default(""))> checked="checked"</#if>>${uiLabelMap.CommonTitleDr}</option>
                   </select>
                </td>
              </tr>
              <tr>
                <td width="26%" align="right" valign="middle"><div>${uiLabelMap.AccountingFirstNameCard}</div></td>
                <td width="5">&nbsp;</td>
                <td width="74%">
                  <input type="text" size="20" maxlength="60" name="firstNameOnCard" value="${(creditCard.firstNameOnCard)!}"/>
                *</td>
              </tr>
              <tr>
                <td width="26%" align="right" valign="middle"><div>${uiLabelMap.AccountingMiddleNameCard}</div></td>
                <td width="5">&nbsp;</td>
                <td width="74%">
                  <input type="text" size="15" maxlength="60" name="middleNameOnCard" value="${(creditCard.middleNameOnCard)!}"/>
                </td>
              </tr>
              <tr>
                <td width="26%" align="right" valign="middle"><div>${uiLabelMap.AccountingLastNameCard}</div></td>
                <td width="5">&nbsp;</td>
                <td width="74%">
                  <input type="text" size="20" maxlength="60" name="lastNameOnCard" value="${(creditCard.lastNameOnCard)!}"/>
                *</td>
              </tr>
              <tr>
                <td width="26%" align="right" valign="middle"><div>${uiLabelMap.AccountingSuffixCard}</div></td>
                <td width="5">&nbsp;</td>
                <td width="74%">
                  <select name="suffixOnCard">
                    <option value="">${uiLabelMap.CommonSelectOne}</option>
                    <option<#if ("Jr." == (creditCard.suffixOnCard)?default(""))> checked="checked"</#if>>Jr.</option>
                    <option<#if ("Sr." == (creditCard.suffixOnCard)?default(""))> checked="checked"</#if>>Sr.</option>
                    <option<#if ("I" == (creditCard.suffixOnCard)?default(""))> checked="checked"</#if>>I</option>
                    <option<#if ("II" == (creditCard.suffixOnCard)?default(""))> checked="checked"</#if>>II</option>
                    <option<#if ("III" == (creditCard.suffixOnCard)?default(""))> checked="checked"</#if>>III</option>
                    <option<#if ("IV" == (creditCard.suffixOnCard)?default(""))> checked="checked"</#if>>IV</option>
                    <option<#if ("V" == (creditCard.suffixOnCard)?default(""))> checked="checked"</#if>>V</option>
                  </select>
                </td>
              </tr>

              <tr>
                <td width="26%" align="right" valign="top"><div>${uiLabelMap.AccountingCardType}</div></td>
                <td width="5">&nbsp;</td>
                <td width="74%">
                  <select name="cardType">
                    <#if creditCard.cartType??>
                    <option>${creditCard.cardType}</option>
                    <option value="${creditCard.cardType}">---</option>
                    </#if>
                    ${screens.render("component://common/widget/CommonScreens.xml#cctypes")}
                  </select>
                *</td>
              </tr>
              <tr>
                <td width="26%" align="right" valign="top"><div>${uiLabelMap.AccountingCardNumber}</div></td>
                <td width="5">&nbsp;</td>
                <td width="74%">
                  <input type="text" size="20" maxlength="30" name="cardNumber" value="${creditCard.cardNumber!}"/>
                *</td>
              </tr>
              <tr>
                <td width="26%" align="right" valign="top"><div>${uiLabelMap.AccountingExpirationDate}</div></td>
                <td width="5">&nbsp;</td>
                <td width="74%">
                  <#assign expMonth = "">
                  <#assign expYear = "">
                  <#if creditCard?? && creditCard.expDate??>
                    <#assign expDate = creditCard.expireDate>
                    <#if (expDate?? && expDate.indexOf("/") > 0)>
                      <#assign expMonth = expDate.substring(0,expDate.indexOf("/"))>
                      <#assign expYear = expDate.substring(expDate.indexOf("/")+1)>
                    </#if>
                  </#if>
                  <select name="expMonth" onchange="javascript:makeExpDate();">
                    <#if creditCard?has_content && expMonth?has_content><#assign ccExprMonth = expMonth><#else><#assign ccExprMonth = requestParameters.expMonth!></#if>
                    <#if ccExprMonth?has_content>
                      <option value="${ccExprMonth!}">${ccExprMonth!}</option>
                    </#if>
                    ${screens.render("component://common/widget/CommonScreens.xml#ccmonths")}
                  </select>
                  <select name="expYear" onchange="javascript:makeExpDate();">
                    <#if creditCard?has_content && expYear?has_content><#assign ccExprYear = expYear><#else><#assign ccExprYear = requestParameters.expYear!></#if>
                    <#if ccExprYear?has_content>
                      <option value="${ccExprYear!}">${ccExprYear!}</option>
                    </#if>
                    ${screens.render("component://common/widget/CommonScreens.xml#ccyears")}
                  </select>
                *</td>
              </tr>
              <tr>
                <td width="26%" align="right" valign="top"><div>${uiLabelMap.CommonDescription}</div></td>
                <td width="5">&nbsp;</td>
                <td width="74%">
                  <input type="text" size="20" maxlength="30" name="description" value="${creditCard.description!}"/>
                </td>
              </tr>
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
                <td width="26%" align="right" valign="top"><div>${uiLabelMap.AccountingNameAccount}</div></td>
                <td width="5">&nbsp;</td>
                <td width="74%">
                  <input type="text" size="30" maxlength="60" name="nameOnAccount" value="${eftAccount.nameOnAccount!}"/>
                *</td>
              </tr>
              <tr>
                <td width="26%" align="right" valign="top"><div>${uiLabelMap.AccountingCompanyNameAccount}</div></td>
                <td width="5">&nbsp;</td>
                <td width="74%">
                  <input type="text" size="30" maxlength="60" name="companyNameOnAccount" value="${eftAccount.companyNameOnAccount!}"/>
                </td>
              </tr>
              <tr>
                <td width="26%" align="right" valign="top"><div>${uiLabelMap.AccountingBankName}</div></td>
                <td width="5">&nbsp;</td>
                <td width="74%">
                  <input type="text" size="30" maxlength="60" name="bankName" value="${eftAccount.bankName!}"/>
                *</td>
              </tr>
              <tr>
                <td width="26%" align="right" valign="top"><div>${uiLabelMap.AccountingRoutingNumber}</div></td>
                <td width="5">&nbsp;</td>
                <td width="74%">
                  <input type="text" size="10" maxlength="30" name="routingNumber" value="${eftAccount.routingNumber!}"/>
                *</td>
              </tr>
              <tr>
                <td width="26%" align="right" valign="top"><div>${uiLabelMap.AccountingAccountType}</div></td>
                <td width="5">&nbsp;</td>
                <td width="74%">
                  <select name="accountType">
                    <option>${eftAccount.accountType!}</option>
                    <option></option>
                    <option>Checking</option>
                    <option>Savings</option>
                  </select>
                *</td>
              </tr>
              <tr>
                <td width="26%" align="right" valign="top"><div>${uiLabelMap.AccountingAccountNumber}</div></td>
                <td width="5">&nbsp;</td>
                <td width="74%">
                  <input type="text" size="20" maxlength="40" name="accountNumber" value="${eftAccount.accountNumber!}"/>
                *</td>
              </tr>
              <tr>
                <td width="26%" align="right" valign="top"><div>${uiLabelMap.CommonDescription}</div></td>
                <td width="5">&nbsp;</td>
                <td width="74%">
                  <input type="text" size="30" maxlength="60" name="description" value="${eftAccount.description!}"/>
                </td>
              </tr>
            </#if>
          </table>
        <#else>
          <#-- initial screen show a list of options -->

          <script type="text/javascript">

              function setCheckoutPaymentId( selectedValue ) {
                  checkoutForm = document.getElementById('checkoutsetupform');
                  if( selectedValue.match('^EXT_.*') ) {
                      checkoutForm.action = '<@ofbizUrl>finalizeOrder</@ofbizUrl>?checkOutPaymentId=' + selectedValue ;
                  } else {
                      checkoutForm.action = '<@ofbizUrl>setBilling</@ofbizUrl>?paymentMethodType=' + selectedValue ;
                  }
              }
          </script>

          <form method="post" action="<@ofbizUrl>finalizeOrder</@ofbizUrl>" name="checkoutsetupform" id="checkoutsetupform">
            <input type="hidden" name="finalizeMode" value="payment"/>
            <input type="hidden" name="createNew" value="${(requestParameters.createNew)!}"/>
            <table width="100%" border="0" cellpadding="1" cellspacing="0">
              <#if "Y" != requestParameters.createNew?default("")>
                <#assign paymentMethodTypeAndIds = productStorePaymentMethodTypeIdMap.keySet() />
                <#list paymentMethodTypeAndIds as paymentMethodTypeAndId>
                  <#assign paymentMethodTypeAndIdList = EntityQuery.use(delegator).from("PaymentMethodType").where("paymentMethodTypeId", paymentMethodTypeAndId).queryOne()!/>
                  <tr>
                    <td width="1%" nowrap="nowrap">
                      <label>
                        <input type="radio" name="paymentMethodTypeAndId" value=${paymentMethodTypeAndId!} <#if checkOutPaymentId?? && paymentMethodTypeAndId == checkOutPaymentId>checked="checked"</#if> onchange="setCheckoutPaymentId(this.value)" onclick="setCheckoutPaymentId(this.value)"/>
                        ${paymentMethodTypeAndIdList.get("description")!}
                      </label>
                    </td>
                  </tr>
                </#list>
              <#else>
                <tr>
                  <td width='1%' nowrap="nowrap"><label><input type="radio" name="paymentMethodTypeAndId" value="CC" onchange="setCheckoutPaymentId(this.value)" onclick="setCheckoutPaymentId(this.value)"/>${uiLabelMap.AccountingVisaMastercardAmexDiscover}</label></td>
                </tr>
                <tr><td colspan="2"><hr /></td></tr>
                <tr>
                  <td width='1%' nowrap="nowrap"><label><input type="radio" name="paymentMethodTypeAndId" value="EFT" onchange="setCheckoutPaymentId(this.value)" onclick="setCheckoutPaymentId(this.value)"/>${uiLabelMap.AccountingAHCElectronicCheck}</label></td>
                </tr>
              </#if>
            </table>
          </form>
        </#if>
    </div>
</div>
<#else>
  <h3>${uiLabelMap.OrderViewPermissionError}</h3>
</#if>
