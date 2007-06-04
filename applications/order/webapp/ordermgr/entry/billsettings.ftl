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
    if (document.checkoutsetupform.useShipAddr.checked) {
        window.location = "<@ofbizUrl>setBilling?createNew=Y&finalizeMode=payment&paymentMethodType=${paymentMethodType?if_exists}&useShipAddr=Y</@ofbizUrl>";
    } else { 
        window.location = "<@ofbizUrl>setBilling?createNew=Y&finalizeMode=payment&paymentMethodType=${paymentMethodType?if_exists}</@ofbizUrl>";
    }
}

function makeExpDate() {
    document.checkoutsetupform.expireDate.value = document.checkoutsetupform.expMonth.options[document.checkoutsetupform.expMonth.selectedIndex].value + "/" + document.checkoutsetupform.expYear.options[document.checkoutsetupform.expYear.selectedIndex].value;
}
</script>

<#if security.hasEntityPermission("ORDERMGR", "_CREATE", session) || security.hasEntityPermission("ORDERMGR", "_PURCHASE_CREATE", session)>
<div class="screenlet">
    <div class="screenlet-body">
        <#if request.getAttribute("paymentMethodId")?exists || ( (paymentMethodList?has_content || billingAccountList?has_content) && !requestParameters.createNew?exists)>
          <#-- initial screen when we have a associated party -->
          <form method="post" action="<@ofbizUrl>finalizeOrder</@ofbizUrl>" name="checkoutsetupform">
            <input type="hidden" name="finalizeMode" value="payment"/>
            <table width="100%" cellpadding="1" cellspacing="0" border="0">
              <tr>
                <td colspan="2">
                  <a href="<@ofbizUrl>setBilling?createNew=Y</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonCreateNew}</a>
                </td>
              </tr>
              <tr><td colspan="3"><hr class="sepbar"/></td></tr>
              <#if billingAccountList?has_content>
                <tr>
                  <td width="1%">
                    <select name="billingAccountId">
                      <option value=""></option>
                        <#list billingAccountList as billingAccount>
                          <#assign availableAmount = billingAccount.accountBalance?double>
                          <#assign accountLimit = billingAccount.accountLimit?double>
                          <option value="${billingAccount.billingAccountId}" <#if billingAccount.billingAccountId == selectedBillingAccountId?default("")>selected</#if>>${billingAccount.description?default("")} [${billingAccount.billingAccountId}] with available amount of <@ofbizCurrency amount=availableAmount isoCode=billingAccount.accountCurrencyUomId/> and account limit of <@ofbizCurrency amount=accountLimit isoCode=billingAccount.accountCurrencyUomId/></option>
                        </#list>
                    </select>
                  </td>
                  <td width="50%">
                    <span class="tabletext">${uiLabelMap.FormFieldTitle_billingAccountId}</span>
                  </td>
                  <td>&nbsp;</td>
                </tr>
                <tr>
                  <td width="1%" align="right">
                    <input type="text" size="5" name="billingAccountAmount" value=""/>
                  </td>
                  <td width="50%">
                    ${uiLabelMap.OrderBillUpTo}
                  </td>
                  <td>&nbsp;</td>
                </tr>
                <tr><td colspan="3"><hr class="sepbar"/></td></tr>
              </#if>                                    
              <tr>
                <td width="1%">
                  <input type="radio" name="checkOutPaymentId" value="EXT_OFFLINE" <#if checkOutPaymentId?exists && checkOutPaymentId == "EXT_OFFLINE">checked="checked"</#if>/>
                </td>
                <td colspan="2" width="50%">
                  <span class="tabletext">${uiLabelMap.OrderOfflineCheckMoney}</span>
                </td>
              </tr>
             <tr><td colspan="3"><hr class="sepbar"/></td></tr>                  
              <tr>
                <td width="1%">
                  <input type="radio" name="checkOutPaymentId" value="EXT_COD" <#if checkOutPaymentId?exists && checkOutPaymentId == "EXT_COD">checked="checked"</#if>/>
                </td>
                <td colspan="2" width="50%">
                  <span class="tabletext">${uiLabelMap.OrderCOD}</span>
                </td>
              </tr>
             <tr><td colspan="3"><hr class="sepbar"/></td></tr>
              <#if paymentMethodList?has_content>
                <#list paymentMethodList as paymentMethod>
                  <#if paymentMethod.paymentMethodTypeId == "CREDIT_CARD">
                    <#assign creditCard = paymentMethod.getRelatedOne("CreditCard")>
                    <tr>                 
                      <td width="1%">
                        <input type="radio" name="checkOutPaymentId" value="${paymentMethod.paymentMethodId}" <#if checkOutPaymentId?exists && paymentMethod.paymentMethodId == checkOutPaymentId>checked="checked"</#if>/>
                      </td>
                      <td width="50%">
                        <span class="tabletext">
                          CC:&nbsp;${Static["org.ofbiz.party.contact.ContactHelper"].formatCreditCard(creditCard)}
                          <#if paymentMethod.description?has_content>(${paymentMethod.description})</#if>
                        </span>                            
                      </td>
                      <td align="right"><a href="/partymgr/control/editcreditcard?party_id=${orderParty.partyId}&amp;paymentMethodId=${paymentMethod.paymentMethodId}" target="_blank" class="buttontext">${uiLabelMap.CommonUpdate}</a></td>
                    </tr>
                  <#elseif paymentMethod.paymentMethodTypeId == "EFT_ACCOUNT">
                    <#assign eftAccount = paymentMethod.getRelatedOne("EftAccount")>
                    <tr>
                      <td width="1%">
                        <input type="radio" name="checkOutPaymentId" value="${paymentMethod.paymentMethodId}" <#if checkOutPaymentId?exists && paymentMethod.paymentMethodId == checkOutPaymentId>checked="checked"</#if>/>
                      </td>
                      <td width="50%">
                        <span class="tabletext">
                          EFT:&nbsp;${eftAccount.bankName?if_exists}: ${eftAccount.accountNumber?if_exists}
                          <#if paymentMethod.description?has_content>(${paymentMethod.description})</#if>
                        </span>
                      </td>
                      <td align="right"><a href="/partymgr/control/editeftaccount?party_id=${orderParty.partyId}&paymentMethodId=${paymentMethod.paymentMethodId}" target="_blank" class="buttontext">${uiLabelMap.CommonUpdate}</a></td>
                    </tr>
                    <tr><td colspan="2"><hr class="sepbar"/></td></tr>
                  </#if>
                </#list>  
              <#else>
                <div class="tabletext"><b>${uiLabelMap.AccountingNoPaymentMethods}</b></div>                                                     
              </#if>
            </table>
          </form>  
        <#elseif paymentMethodType?exists || finalizeMode?default("") == "payment">
          <#-- after initial screen; show detailed screens for selected type -->
          <#if paymentMethodType == "CC">
            <#if postalAddress?has_content>
              <form method="post" action="<@ofbizUrl>updateCreditCardAndPostalAddress</@ofbizUrl>" name="checkoutsetupform">
                <input type="hidden" name="paymentMethodId" value="${creditCard.paymentMethodId?if_exists}"/>
                <input type="hidden" name="contactMechId" value="${postalAddress.contactMechId?if_exists}"/>
            <#elseif requestParameters.useShipAddr?exists>
              <form method="post" action="<@ofbizUrl>createCreditCardOrderEntry</@ofbizUrl>" name="checkoutsetupform">
            <#else>
              <form method="post" action="<@ofbizUrl>createCreditCardAndPostalAddress</@ofbizUrl>" name="checkoutsetupform">
            </#if>
          </#if>
          <#if paymentMethodType == "EFT">
            <#if postalAddress?has_content>                  
              <form method="post" action="<@ofbizUrl>updateEftAndPostalAddress</@ofbizUrl>" name="checkoutsetupform">
                <input type="hidden" name="paymentMethodId" value="${eftAccount.paymentMethodId?if_exists}"/>
                <input type="hidden" name="contactMechId" value="${postalAddress.contactMechId?if_exists}"/>
            <#elseif requestParameters.useShipAddr?exists>
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
          <#if requestParameters.useShipAddr?exists>
            <input type="hidden" name="contactMechId" value="${postalFields.contactMechId}"/>
          </#if>
            
          <table width="100%" border="0" cellpadding="1" cellspacing="0">
            <#if cart.getShippingContactMechId()?exists>
            <tr>
              <td width="26%" align="right"= valign="top">
                <input type="checkbox" name="useShipAddr" value="Y" onClick="javascript:shipBillAddr();" <#if requestParameters.useShipAddr?exists>checked="checked"</#if>/>
              </td>
              <td colspan="2" align="left" valign="center">
                <div class="tabletext">${uiLabelMap.FacilityBillingAddressSameShipping}</div>
              </td>
            </tr>
            <tr>
              <td colspan="3"><hr class="sepbar"/></td>
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
              <td width="26%" align="right" valign="top"><div class="tabletext">${uiLabelMap.CommonToName}</div></td>
              <td width="5">&nbsp;</td>
              <td width="74%">
                <input type="text" class="inputBox" size="30" maxlength="60" name="toName" value="${toName}" <#if requestParameters.useShipAddr?exists>disabled="disabled"</#if>/>
              </td>
            </tr>
            <tr>
              <td width="26%" align="right" valign="top"><div class="tabletext">${uiLabelMap.CommonAttentionName}</div></td>
              <td width="5">&nbsp;</td>
              <td width="74%">
                <input type="text" class="inputBox" size="30" maxlength="60" name="attnName" value="${postalFields.attnName?if_exists}" <#if requestParameters.useShipAddr?exists>disabled="disabled"</#if>/>
              </td>
            </tr>
            <tr>
              <td width="26%" align="right" valign="top"><div class="tabletext">${uiLabelMap.CommonAddressLine} 1</div></td>
              <td width="5">&nbsp;</td>
              <td width="74%">
                <input type="text" class="inputBox" size="30" maxlength="30" name="address1" value="${postalFields.address1?if_exists}" <#if requestParameters.useShipAddr?exists>disabled="disabled"</#if>/>
              *</td>
            </tr>
            <tr>
              <td width="26%" align="right" valign="top"><div class="tabletext">${uiLabelMap.CommonAddressLine} 2</div></td>
              <td width="5">&nbsp;</td>
              <td width="74%">
                <input type="text" class="inputBox" size="30" maxlength="30" name="address2" value="${postalFields.address2?if_exists}" <#if requestParameters.useShipAddr?exists>disabled="disabled"</#if>/>
              </td>
            </tr>
            <tr>
              <td width="26%" align="right" valign="top"><div class="tabletext">${uiLabelMap.CommonCity}</div></td>
              <td width="5">&nbsp;</td>
              <td width="74%">
                <input type="text" class="inputBox" size="30" maxlength="30" name="city" value="${postalFields.city?if_exists}" <#if requestParameters.useShipAddr?exists>disabled="disabled"</#if>/>
              *</td>
            </tr>
            <tr>
              <td width="26%" align="right" valign="top"><div class="tabletext">${uiLabelMap.CommonStateProvince}</div></td>
              <td width="5">&nbsp;</td>
              <td width="74%">
                <select name="stateProvinceGeoId" class="selectBox" <#if requestParameters.useShipAddr?exists>disabled="disabled"</#if>>
                  <#if postalFields.stateProvinceGeoId?exists>
                  <option>${postalFields.stateProvinceGeoId}</option>
                  <option value="${postalFields.stateProvinceGeoId}">---</option>
                  </#if>
                  <option value=""></option>
                  ${screens.render("component://common/widget/CommonScreens.xml#states")}
                </select>
              </td>
            </tr>
            <tr>
              <td width="26%" align="right" valign="top"><div class="tabletext">${uiLabelMap.CommonZipPostalCode}</div></td>
              <td width="5">&nbsp;</td>
              <td width="74%">
                <input type="text" class="inputBox" size="12" maxlength="10" name="postalCode" value="${postalFields.postalCode?if_exists}" <#if requestParameters.useShipAddr?exists>disabled="disabled"</#if>/>
              *</td>
            </tr>
            <tr>
              <td width="26%" align="right" valign="top"><div class="tabletext">${uiLabelMap.CommonCountry}</div></td>
              <td width="5">&nbsp;</td>
              <td width="74%">
                <select name="countryGeoId" class="selectBox" <#if requestParameters.useShipAddr?exists>disabled="disabled"</#if>>
                  <#if postalFields.countryGeoId?exists>
                  <option>${postalFields.countryGeoId}</option>
                  <option value="${postalFields.countryGeoId}">---</option>
                  </#if>
                  ${screens.render("component://common/widget/CommonScreens.xml#countries")}
                </select>
              *</td>
            </tr> 
            
            <#-- credit card fields -->
            <#if paymentMethodType == "CC">
              <#if !creditCard?has_content>
                <#assign creditCard = requestParameters>
              </#if>
              <input type="hidden" name="expireDate" value="${creditCard.expireDate?if_exists}"/>
              <tr>
                <td colspan="3"><hr class="sepbar"/></td>
              </tr>

  	              <tr>
                <td width="26%" align="right" valign="middle"><div class="tabletext">${uiLabelMap.AccountingCompanyNameCard}</div></td>
                <td width="5">&nbsp;</td>
                <td width="74%">
                  <input type="text" class='inputBox' size="30" maxlength="60" name="companyNameOnCard" value="${creditCard.companyNameOnCard?if_exists}"/>
                </td>
              </tr>
              <tr>
                <td width="26%" align="right" valign="middle"><div class="tabletext">${uiLabelMap.AccountingPrefixCard}</div></td>
                <td width="5">&nbsp;</td>
                <td width="74%">
                  <select name="titleOnCard" class="selectBox">
                    <option value="">${uiLabelMap.CommonSelectOne}</option>
                    <option<#if ((creditCard.titleOnCard)?default("") == "Mr.")> checked</#if>>${uiLabelMap.CommonTitleMr}</option>
                    <option<#if ((creditCard.titleOnCard)?default("") == "Mrs.")> checked</#if>>${uiLabelMap.CommonTitleMrs}</option>
                    <option<#if ((creditCard.titleOnCard)?default("") == "Ms.")> checked</#if>>${uiLabelMap.CommonTitleMs}</option>
                    <option<#if ((creditCard.titleOnCard)?default("") == "Dr.")> checked</#if>>${uiLabelMap.CommonTitleDr}</option>
                   </select>
                </td>
              </tr>
              <tr>
                <td width="26%" align="right" valign="middle"><div class="tabletext">${uiLabelMap.AccountingFirstNameCard}</div></td>
                <td width="5">&nbsp;</td>
                <td width="74%">
                  <input type="text" class="inputBox" size="20" maxlength="60" name="firstNameOnCard" value="${(creditCard.firstNameOnCard)?if_exists}"/>
                *</td>
              </tr>
              <tr>
                <td width="26%" align="right" valign="middle"><div class="tabletext">${uiLabelMap.AccountingMiddleNameCard}</div></td>
                <td width="5">&nbsp;</td>
                <td width="74%">
                  <input type="text" class="inputBox" size="15" maxlength="60" name="middleNameOnCard" value="${(creditCard.middleNameOnCard)?if_exists}"/>
                </td>
              </tr>
              <tr>
                <td width="26%" align="right" valign="middle"><div class="tabletext">${uiLabelMap.AccountingLastNameCard}</div></td>
                <td width="5">&nbsp;</td>
                <td width="74%">
                  <input type="text" class="inputBox" size="20" maxlength="60" name="lastNameOnCard" value="${(creditCard.lastNameOnCard)?if_exists}"/>
                *</td>
              </tr>
              <tr>
                <td width="26%" align="right" valign="middle"><div class="tabletext">${uiLabelMap.AccountingSuffixCard}</div></td>
                <td width="5">&nbsp;</td>
                <td width="74%">
                  <select name="suffixOnCard" class="selectBox">
                    <option value="">${uiLabelMap.CommonSelectOne}</option>
                    <option<#if ((creditCard.suffixOnCard)?default("") == "Jr.")> checked="checked"</#if>>Jr.</option>
                    <option<#if ((creditCard.suffixOnCard)?default("") == "Sr.")> checked="checked"</#if>>Sr.</option>
                    <option<#if ((creditCard.suffixOnCard)?default("") == "I")> checked="checked"</#if>>I</option>
                    <option<#if ((creditCard.suffixOnCard)?default("") == "II")> checked="checked"</#if>>II</option>
                    <option<#if ((creditCard.suffixOnCard)?default("") == "III")> checked="checked"</#if>>III</option>
                    <option<#if ((creditCard.suffixOnCard)?default("") == "IV")> checked="checked"</#if>>IV</option>
                    <option<#if ((creditCard.suffixOnCard)?default("") == "V")> checked="checked"</#if>>V</option>
                  </select>
                </td>
              </tr>

              <tr>
                <td width="26%" align="right" valign="top"><div class="tabletext">${uiLabelMap.AccountingCardType}</div></td>
                <td width="5">&nbsp;</td>
                <td width="74%">
                  <select name="cardType" class="selectBox">
                    <#if creditCard.cartType?exists>
                    <option>${creditCard.cardType}</option>
                    <option value="${creditCard.cardType}">---</option>
                    </#if>
                    <option>Visa</option>
                    <option value='MasterCard'>Master Card</option>
                    <option value='AmericanExpress'>American Express</option>
                    <option value='DinersClub'>Diners Club</option>
                    <option>Discover</option>
                    <option>EnRoute</option>
                    <option>JCB</option>
                  </select>
                *</td>
              </tr>
              <tr>
                <td width="26%" align="right" valign="top"><div class="tabletext">${uiLabelMap.AccountingCardNumber}</div></td>
                <td width="5">&nbsp;</td>
                <td width="74%">
                  <input type="text" class="inputBox" size="20" maxlength="30" name="cardNumber" value="${creditCard.cardNumber?if_exists}"/>
                *</td>
              </tr>
              <#--<tr>
                <td width="26%" align="right" valign="top"><div class="tabletext">${uiLabelMap.OrderCardSecurityCode}</div></td>
                <td width="5">&nbsp;</td>
                <td width="74%">
                  <input type="text" size="5" maxlength="10" name="cardSecurityCode" value=""/>
                </td>
              </tr>-->
              <tr>
                <td width="26%" align="right" valign="top"><div class="tabletext">${uiLabelMap.AccountingExpirationDate}</div></td>        
                <td width="5">&nbsp;</td>                    
                <td width="74%">
                  <#assign expMonth = "">
                  <#assign expYear = "">
                  <#if creditCard?exists && creditCard.expDate?exists>
                    <#assign expDate = creditCard.expireDate>
                    <#if (expDate?exists && expDate.indexOf("/") > 0)>
                      <#assign expMonth = expDate.substring(0,expDate.indexOf("/"))>
                      <#assign expYear = expDate.substring(expDate.indexOf("/")+1)>
                    </#if>
                  </#if>
                  <select name="expMonth" class='selectBox' onchange="javascript:makeExpDate();">
                    <#if creditCard?has_content && expMonth?has_content><#assign ccExprMonth = expMonth><#else><#assign ccExprMonth = requestParameters.expMonth?if_exists></#if>                        
                    <option value="${ccExprMonth?if_exists}">${ccExprMonth?if_exists}</option>
                    <option></option>
                    <option value="01">01</option>
                    <option value="02">02</option>
                    <option value="03">03</option>
                    <option value="04">04</option>
                    <option value="05">05</option>
                    <option value="06">06</option>
                    <option value="07">07</option>
                    <option value="08">08</option>
                    <option value="09">09</option>
                    <option value="10">10</option>
                    <option value="11">11</option>
                    <option value="12">12</option>
                  </select>
                  <select name="expYear" class='selectBox' onchange="javascript:makeExpDate();">
                    <#if creditCard?has_content && expYear?has_content><#assign ccExprYear = expYear><#else><#assign ccExprYear = requestParameters.expYear?if_exists></#if> 
                    <option value="${ccExprYear?if_exists}">${ccExprYear?if_exists}</option>
                    <option></option>          
                    <option value="2005">2005</option>
                    <option value="2006">2006</option>
                    <option value="2007">2007</option>
                    <option value="2008">2008</option>
                    <option value="2009">2009</option>
                    <option value="2010">2010</option>
                    <option value="2011">2011</option>
                    <option value="2012">2012</option>
                    <option value="2013">2013</option>
                  </select>
                *</td>                                                       
              </tr>
              <tr>
                <td width="26%" align="right" valign="top"><div class="tabletext">${uiLabelMap.CommonDescription}</div></td>
                <td width="5">&nbsp;</td>
                <td width="74%">
                  <input type="text" class="inputBox" size="20" maxlength="30" name="description" value="${creditCard.description?if_exists}"/>
                </td>
              </tr>
  	            </#if>  
  	            
  	            <#-- eft fields -->
  	            <#if paymentMethodType =="EFT">
  	              <#if !eftAccount?has_content>
  	                <#assign eftAccount = requestParameters>
  	              </#if>
  	              <tr>
                <td colspan="3"><hr class="sepbar"/></td>
              </tr>
              <tr>
                <td width="26%" align="right" valign="top"><div class="tabletext">${uiLabelMap.AccountingNameAccount}</div></td>
                <td width="5">&nbsp;</td>
                <td width="74%">
                  <input type="text" class="inputBox" size="30" maxlength="60" name="nameOnAccount" value="${eftAccount.nameOnAccount?if_exists}"/>
                *</td>
              </tr>
              <tr>
                <td width="26%" align="right" valign="top"><div class="tabletext">${uiLabelMap.AccountingCompanyNameAccount}</div></td>
                <td width="5">&nbsp;</td>
                <td width="74%">
                  <input type="text" class="inputBox" size="30" maxlength="60" name="companyNameOnAccount" value="${eftAccount.companyNameOnAccount?if_exists}"/>
                </td>
              </tr>
              <tr>
                <td width="26%" align="right" valign="top"><div class="tabletext">${uiLabelMap.AccountingBankName}</div></td>
                <td width="5">&nbsp;</td>
                <td width="74%">
                  <input type="text" class="inputBox" size="30" maxlength="60" name="bankName" value="${eftAccount.bankName?if_exists}"/>
                *</td>
              </tr>
              <tr>
                <td width="26%" align="right" valign="top"><div class="tabletext">${uiLabelMap.AccountingRoutingNumber}</div></td>
                <td width="5">&nbsp;</td>
                <td width="74%">
                  <input type="text" class="inputBox" size="10" maxlength="30" name="routingNumber" value="${eftAccount.routingNumber?if_exists}"/>
                *</td>
              </tr>
              <tr>
                <td width="26%" align="right" valign="top"><div class="tabletext">${uiLabelMap.AccountingAccountType}</div></td>
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
                <td width="26%" align="right" valign="top"><div class="tabletext">${uiLabelMap.AccountingAccountNumber}</div></td>
                <td width="5">&nbsp;</td>
                <td width="74%">
                  <input type="text" class="inputBox" size="20" maxlength="40" name="accountNumber" value="${eftAccount.accountNumber?if_exists}"/>
                *</td>
              </tr>
              <tr>
                <td width="26%" align="right" valign="top"><div class="tabletext">${uiLabelMap.CommonDescription}</div></td>
                <td width="5">&nbsp;</td>
                <td width="74%">
                  <input type="text" class="inputBox" size="30" maxlength="60" name="description" value="${eftAccount.description?if_exists}"/>
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
            <input type="hidden" name="createNew" value="Y"/>
            <table width="100%" border="0" cellpadding="1" cellspacing="0"> 
              <#if !requestParameters.createNew?exists>                                    
              <tr>
                <td width='1%' nowrap><input type="radio" name="paymentMethodTypeAndId" value="EXT_OFFLINE" <#if checkOutPaymentId?exists && checkOutPaymentId == "EXT_OFFLINE">checked="checked"</#if> onchange="setCheckoutPaymentId(this.value)" onClick="setCheckoutPaymentId(this.value)"/></td>
                <td width='50%'nowrap><div class="tabletext">${uiLabelMap.OrderPaymentOfflineCheckMoney}</div></td>
              </tr>
              <tr><td colspan="2"><hr class="sepbar"/></td></tr>
              <tr>
                <td width="1%" nowrap><input type="radio" name="paymentMethodTypeAndId" value="EXT_COD" <#if checkOutPaymentId?exists && checkOutPaymentId == "EXT_COD">checked="checked"</#if> onchange="setCheckoutPaymentId(this.value)" onClick="setCheckoutPaymentId(this.value)"/></td>
                <td width="50%"nowrap><div class="tabletext">${uiLabelMap.OrderCOD}</div></td>
              </tr>
              <tr><td colspan="2"><hr class="sepbar"/></td></tr>
              </#if>
              <tr>
                <td width='1%' nowrap><input type="radio" name="paymentMethodTypeAndId" value="CC" onchange="setCheckoutPaymentId(this.value)" onClick="setCheckoutPaymentId(this.value)"/>
                <td width='50%' nowrap><div class="tabletext">${uiLabelMap.AccountingVisaMastercardAmexDiscover}</div></td>
              </tr>
              <tr><td colspan="2"><hr class="sepbar"/></td></tr>
              <tr>
                <td width='1%' nowrap><input type="radio" name="paymentMethodTypeAndId" value="EFT" onchange="setCheckoutPaymentId(this.value)" onClick="setCheckoutPaymentId(this.value)"/>
                <td width='50%' nowrap><div class="tabletext">${uiLabelMap.AccountingAHCElectronicCheck}</div></td>
              </tr>
            </table>
          </form>
        </#if>
    </div>
</div>
<#else>
  <h3>${uiLabelMap.OrderViewPermissionError}</h3>
</#if>
