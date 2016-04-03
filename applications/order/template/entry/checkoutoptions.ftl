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
function submitForm(form, mode, value) {
    if (mode == "DN") {
        // done action; checkout
        form.action="<@ofbizUrl>checkout</@ofbizUrl>";
        form.submit();
    } else if (mode == "CS") {
        // continue shopping
        form.action="<@ofbizUrl>updateCheckoutOptions/showcart</@ofbizUrl>";
        form.submit();
    } else if (mode == "NA") {
        // new address
        form.action="<@ofbizUrl>updateCheckoutOptions/editcontactmech?DONE_PAGE=quickcheckout&partyId=${shoppingCart.getPartyId()}&preContactMechTypeId=POSTAL_ADDRESS&contactMechPurposeTypeId=SHIPPING_LOCATION</@ofbizUrl>";
        form.submit();
    } else if (mode == "EA") {
        // edit address
        form.action="<@ofbizUrl>updateCheckoutOptions/editcontactmech?DONE_PAGE=quickcheckout&partyId=${shoppingCart.getPartyId()}&contactMechId="+value+"</@ofbizUrl>";
        form.submit();
    } else if (mode == "NC") {
        // new credit card
        form.action="<@ofbizUrl>updateCheckoutOptions/editcreditcard?DONE_PAGE=quickcheckout&partyId=${shoppingCart.getPartyId()}</@ofbizUrl>";
        form.submit();
    } else if (mode == "EC") {
        // edit credit card
        form.action="<@ofbizUrl>updateCheckoutOptions/editcreditcard?DONE_PAGE=quickcheckout&partyId=${shoppingCart.getPartyId()}&paymentMethodId="+value+"</@ofbizUrl>";
        form.submit();
    } else if (mode == "GC") {
        // edit gift card
        form.action="<@ofbizUrl>updateCheckoutOptions/editgiftcard?DONE_PAGE=quickcheckout&partyId=${shoppingCart.getPartyId()}&paymentMethodId="+value+"</@ofbizUrl>";
        form.submit();
    } else if (mode == "NE") {
        // new eft account
        form.action="<@ofbizUrl>updateCheckoutOptions/editeftaccount?DONE_PAGE=quickcheckout&partyId=${shoppingCart.getPartyId()}</@ofbizUrl>";
        form.submit();
    } else if (mode == "EE") {
        // edit eft account
        form.action="<@ofbizUrl>updateCheckoutOptions/editeftaccount?DONE_PAGE=quickcheckout&partyId=${shoppingCart.getPartyId()}&paymentMethodId="+value+"</@ofbizUrl>";
        form.submit();
    } else if (mode == "SP") {
        // split payment
        form.action="<@ofbizUrl>updateCheckoutOptions/checkoutpayment?partyId=${shoppingCart.getPartyId()}</@ofbizUrl>";
        form.submit();
    } else if (mode == "SA") {
        // selected shipping address
        form.action="<@ofbizUrl>updateCheckoutOptions/quickcheckout</@ofbizUrl>";
        form.submit();
    } else if (mode == "SC") {
        // selected ship to party
        form.action="<@ofbizUrl>cartUpdateShipToCustomerParty</@ofbizUrl>";
        form.submit();
    }
}
//]]>
</script>

<#assign shipping = !shoppingCart.containAllWorkEffortCartItems()> <#-- contains items which need shipping? -->
<form method="post" name="checkoutInfoForm" style="margin:0;">
  <input type="hidden" name="checkoutpage" value="quick"/>
  <input type="hidden" name="BACK_PAGE" value="quickcheckout"/>

  <table width="100%" border="0" cellpadding="0" cellspacing="0">
    <tr valign="top">
      <td height="100%">
        <div class="screenlet" style="height: 100%;">
            <div class="screenlet-title-bar">
                <#if shipping == true>
                    <div class="h3">1)&nbsp;${uiLabelMap.OrderWhereShallWeShipIt}?</div>
                <#else>
                    <div class="h3">1)&nbsp;${uiLabelMap.OrderInformationAboutYou}</div>
                </#if>
            </div>
            <div class="screenlet-body" style="height: 100%;">
                <table width="100%" border="0" cellpadding="1" cellspacing="0">
                  <tr>
                    <td colspan="2">
                      <span>${uiLabelMap.OrderShipToParty}:</span>
                      <select name="shipToCustomerPartyId" onchange="javascript:submitForm(document.checkoutInfoForm, 'SC', null);">
                          <#list cartParties as cartParty>
                          <option value="${cartParty}">${cartParty}</option>
                          </#list>
                      </select>
                    </td>
                  </tr>
                  <tr>
                    <td colspan="2">
                      <span>${uiLabelMap.CommonAdd}:</span>
                      <a href="javascript:submitForm(document.checkoutInfoForm, 'NA', '');" class="buttontext">${uiLabelMap.PartyAddNewAddress}</a>
                    </td>
                  </tr>
                  <#if (shoppingCart.getTotalQuantity() > 1) && !shoppingCart.containAllWorkEffortCartItems()> <#-- no splitting when only rental items -->
                    <tr><td colspan="2"><hr /></td></tr>
                    <tr>
                      <td colspan="2" align="center">
                        <a href="<@ofbizUrl>splitship</@ofbizUrl>" class="buttontext">${uiLabelMap.OrderSplitIntoMultipleShipments}</a>
                        <#if (shoppingCart.getShipGroupSize() > 1)>
                          <div style="color: red;">${uiLabelMap.OrderNOTEMultipleShipmentsExist}.</div>
                        </#if>
                      </td>
                    </tr>
                  </#if>
                   <#if shippingContactMechList?has_content>
                     <tr><td colspan="2"><hr /></td></tr>
                     <#list shippingContactMechList as shippingContactMech>
                       <#assign shippingAddress = shippingContactMech.getRelatedOne("PostalAddress", false)>
                       <tr>
                         <td valign="top" width="1%">
                           <input type="radio" name="shipping_contact_mech_id" value="${shippingAddress.contactMechId}" onclick="javascript:submitForm(document.checkoutInfoForm, 'SA', null);"<#if shoppingCart.getShippingContactMechId()?default("") == shippingAddress.contactMechId> checked="checked"</#if>/>
                         </td>
                         <td valign="top" width="99%">
                           <div>
                             <#if shippingAddress.toName?has_content><b>${uiLabelMap.CommonTo}:</b>&nbsp;${shippingAddress.toName}<br /></#if>
                             <#if shippingAddress.attnName?has_content><b>${uiLabelMap.PartyAddrAttnName}:</b>&nbsp;${shippingAddress.attnName}<br /></#if>
                             <#if shippingAddress.address1?has_content>${shippingAddress.address1}<br /></#if>
                             <#if shippingAddress.address2?has_content>${shippingAddress.address2}<br /></#if>
                             <#if shippingAddress.city?has_content>${shippingAddress.city}</#if>
                             <#if shippingAddress.stateProvinceGeoId?has_content><br />${shippingAddress.stateProvinceGeoId}</#if>
                             <#if shippingAddress.postalCode?has_content><br />${shippingAddress.postalCode}</#if>
                             <#if shippingAddress.countryGeoId?has_content><br />${shippingAddress.countryGeoId}</#if>
                             <a href="javascript:submitForm(document.checkoutInfoForm, 'EA', '${shippingAddress.contactMechId}');" class="buttontext">${uiLabelMap.CommonUpdate}</a>
                           </div>
                         </td>
                       </tr>
                       <#if shippingContactMech_has_next>
                         <tr><td colspan="2"><hr /></td></tr>
                       </#if>
                     </#list>
                   </#if>
                 </table>

                <#-- Party Tax Info -->
                <#-- commented out by default because the TaxAuthority drop-down is just too wide...
                <hr />
                <div>&nbsp;${uiLabelMap.PartyTaxIdentification}</div>
                ${screens.render("component://order/widget/ordermgr/OrderEntryOrderScreens.xml#customertaxinfo")}
                -->
            </div>
        </div>
      </td>
      <td bgcolor="white" width="1">&nbsp;&nbsp;</td>
      <td height="100%">
        <div class="screenlet" style="height: 100%;">
            <div class="screenlet-title-bar">
                <#if shipping == true>
                    <div class="h3">2)&nbsp;${uiLabelMap.OrderHowShallWeShipIt}?</div>
                <#else>
                    <div class="h3">2)&nbsp;${uiLabelMap.OrderOptions}?</div>
                </#if>
            </div>
            <div class="screenlet-body" style="height: 100%;">
                <table width="100%" cellpadding="1" border="0" cellpadding="0" cellspacing="0">
                 <#if shipping == true>
                  <#list carrierShipmentMethodList as carrierShipmentMethod>
                    <#assign shippingMethod = carrierShipmentMethod.shipmentMethodTypeId + "@" + carrierShipmentMethod.partyId>
                    <tr>
                      <td width="1%" valign="top">
                        <input type="radio" name="shipping_method" value="${shippingMethod}" <#if shippingMethod == chosenShippingMethod?default("N@A")>checked="checked"</#if>/>
                      </td>
                      <td valign="top">
                        <div>
                          <#if shoppingCart.getShippingContactMechId()??>
                            <#assign shippingEst = shippingEstWpr.getShippingEstimate(carrierShipmentMethod)?default(-1)>
                          </#if>
                          <#if carrierShipmentMethod.partyId != "_NA_">${carrierShipmentMethod.partyId!}&nbsp;</#if>${carrierShipmentMethod.description!}
                          <#if shippingEst?has_content> - <#if (shippingEst > -1)><@ofbizCurrency amount=shippingEst isoCode=shoppingCart.getCurrency()/><#else>${uiLabelMap.OrderCalculatedOffline}</#if></#if>
                        </div>
                      </td>
                    </tr>
                  </#list>
                  <#if !carrierShipmentMethodList?? || carrierShipmentMethodList?size == 0>
                    <tr>
                      <td width="1%" valign="top">
                        <input type="radio" name="shipping_method" value="Default" checked="checked"/>
                      </td>
                      <td valign="top">
                        <div>${uiLabelMap.OrderUseDefault}.</div>
                      </td>
                    </tr>
                  </#if>
                  <tr><td colspan="2"><hr /></td></tr>
                  <tr>
                    <td colspan="2">
                      <h2>${uiLabelMap.OrderShipAllAtOnce}?</h2>
                    </td>
                  </tr>
                  <tr>
                    <td valign="top">
                      <input type="radio" <#if shoppingCart.getMaySplit()?default("N") == "N">checked="checked"</#if> name="may_split" value="false"/>
                    </td>
                    <td valign="top">
                      <div>${uiLabelMap.OrderPleaseWaitUntilBeforeShipping}.</div>
                    </td>
                  </tr>
                  <tr>
                    <td valign="top">
                      <input <#if shoppingCart.getMaySplit()?default("N") == "Y">checked="checked"</#if> type="radio" name="may_split" value="true"/>
                    </td>
                    <td valign="top">
                      <div>${uiLabelMap.OrderPleaseShipItemsBecomeAvailable}.</div>
                    </td>
                  </tr>
                  <tr><td colspan="2"><hr /></td></tr>
                 <#else/>
                    <input type="hidden" name="shipping_method" value="NO_SHIPPING@_NA_"/>
                    <input type="hidden" name="may_split" value="false"/>
                    <input type="hidden" name="is_gift" value="false"/>
                 </#if>
                  <tr>
                    <td colspan="2">
                      <h2>${uiLabelMap.OrderSpecialInstructions}</h2>
                    </td>
                  </tr>
                  <tr>
                    <td colspan="2">
                      <textarea cols="30" rows="3" wrap="hard" name="shipping_instructions">${shoppingCart.getShippingInstructions()!}</textarea>
                    </td>
                  </tr>
                 <#if shipping == true>
                  <#if productStore.showCheckoutGiftOptions! != "N" && giftEnable! != "N">
                  <tr><td colspan="2"><hr /></td></tr>
                  <tr>
                    <td colspan="2">
                      <div>
                        <span class="h2"><b>${uiLabelMap.OrderIsThisGift}</b></span>
                        <input type="radio" <#if shoppingCart.getIsGift()?default("Y") == "Y">checked="checked"</#if> name="is_gift" value="true"><span>${uiLabelMap.CommonYes}</span>
                        <input type="radio" <#if shoppingCart.getIsGift()?default("N") == "N">checked="checked"</#if> name="is_gift" value="false"><span>${uiLabelMap.CommonNo}</span>
                      </div>
                    </td>
                  </tr>
                  <tr><td colspan="2"><hr /></td></tr>
                  <tr>
                    <td colspan="2">
                      <h2>${uiLabelMap.OrderGiftMessage}</h2>
                    </td>
                  </tr>
                  <tr>
                    <td colspan="2">
                      <textarea cols="30" rows="3" wrap="hard" name="gift_message">${shoppingCart.getGiftMessage()!}</textarea>
                    </td>
                  </tr>
                  <#else/>
                  <input type="hidden" name="is_gift" value="false"/>
                  </#if>
                 </#if>
                  <tr><td colspan="2"><hr /></td></tr>
                  <tr>
                    <td colspan="2">
                      <h2>${uiLabelMap.PartyEmailAddresses}</h2>
                    </td>
                  </tr>
                  <tr>
                    <td colspan="2">
                      <div>${uiLabelMap.OrderEmailSentToFollowingAddresses}:</div>
                      <div>
                      <b>
                      <#list emailList as email>
                        ${email.infoString!}<#if email_has_next>,</#if>
                      </#list>
                      </b>
                      </div>
                      <div>${uiLabelMap.OrderUpdateEmailAddress} <a href="<#if customerDetailLink??>${customerDetailLink}${shoppingCart.getPartyId()}" target="partymgr"
                        <#else><@ofbizUrl>viewprofile?DONE_PAGE=quickcheckout</@ofbizUrl>"</#if> class="buttontext">${uiLabelMap.PartyProfile}</a>.</div>
                      <br />
                      <div>${uiLabelMap.OrderCommaSeperatedEmailAddresses}:</div>
                      <input type="text" size="30" name="order_additional_emails" value="${shoppingCart.getOrderAdditionalEmails()!}"/>
                    </td>
                  </tr>
                </table>
            </div>
        </div>

      </td>
      <td bgcolor="white" width="1">&nbsp;&nbsp;</td>
      <td height="100%">
          <#-- Payment Method Selection -->

        <div class="screenlet" style="height: 100%;">
            <div class="screenlet-title-bar">
                <div class="h3">3)${uiLabelMap.OrderHowShallYouPay}?</div>
            </div>
            <div class="screenlet-body" style="height: 100%;">
                <table width="100%" cellpadding="1" cellspacing="0" border="0">
                  <tr>
                    <td colspan="2">
                      <span>${uiLabelMap.CommonAdd}:</span>
                      <#if productStorePaymentMethodTypeIdMap.CREDIT_CARD??>
                        <a href="javascript:submitForm(document.checkoutInfoForm, 'NC', '');" class="buttontext">${uiLabelMap.AccountingCreditCard}</a>
                      </#if>
                      <#if productStorePaymentMethodTypeIdMap.EFT_ACCOUNT??>
                        <a href="javascript:submitForm(document.checkoutInfoForm, 'NE', '');" class="buttontext">${uiLabelMap.AccountingEFTAccount}</a>
                      </#if>
                    </td>
                  </tr>
                  <tr><td colspan="2"><hr /></td></tr>
                  <tr>
                    <td colspan="2" align="center">
                      <a href="javascript:submitForm(document.checkoutInfoForm, 'SP', '');" class="buttontext">${uiLabelMap.AccountingSplitPayment}</a>
                    </td>
                  </tr>
                  <tr><td colspan="2"><hr /></td></tr>
                  <#if productStorePaymentMethodTypeIdMap.EXT_OFFLINE??>
                  <tr>
                    <td width="1%">
                      <input type="radio" name="checkOutPaymentId" value="EXT_OFFLINE" <#if "EXT_OFFLINE" == checkOutPaymentId>checked="checked"</#if>/>
                    </td>
                    <td width="50%">
                      <span>${uiLabelMap.OrderMoneyOrder}</span>
                    </td>
                  </tr>
                  </#if>
                  <#if productStorePaymentMethodTypeIdMap.EXT_COD??>
                  <tr>
                    <td width="1%">
                      <input type="radio" name="checkOutPaymentId" value="EXT_COD" <#if "EXT_COD" == checkOutPaymentId>checked="checked"</#if>/>
                    </td>
                    <td width="50%">
                      <span>${uiLabelMap.OrderCOD}</span>
                    </td>
                  </tr>
                  </#if>
                  <#if productStorePaymentMethodTypeIdMap.EXT_WORLDPAY??>
                  <tr>
                    <td width="1%">
                      <input type="radio" name="checkOutPaymentId" value="EXT_WORLDPAY" <#if "EXT_WORLDPAY" == checkOutPaymentId>checked="checked"</#if>/>
                    </td>
                    <td width="50%">
                      <span>${uiLabelMap.AccountingPayWithWorldPay}</span>
                    </td>
                  </tr>
                  </#if>
                  <#if productStorePaymentMethodTypeIdMap.EXT_PAYPAL??>
                  <tr>
                    <td width="1%">
                      <input type="radio" name="checkOutPaymentId" value="EXT_PAYPAL" <#if "EXT_PAYPAL" == checkOutPaymentId>checked="checked"</#if>/>
                    </td>
                    <td width="50%">
                      <span>${uiLabelMap.AccountingPayWithPayPal}</span>
                    </td>
                  </tr>
                  </#if>
                  <tr><td colspan="2"><hr /></td></tr>

                  <#-- financial accounts -->
                  <#list finAccounts as finAccount>
                      <tr>
                        <td width="1%">
                          <input type="radio" name="checkOutPaymentId" value="FIN_ACCOUNT|${finAccount.finAccountId}" <#if "FIN_ACCOUNT" == checkOutPaymentId>checked="checked"</#if>/>
                        </td>
                        <td width="50%">
                          <span>${uiLabelMap.AccountingFinAccount} #${finAccount.finAccountId}</span>
                        </td>
                      </tr>
                  </#list>

                  <#if !paymentMethodList?has_content>
                    <#if (!finAccounts?has_content)>
                      <tr>
                        <td colspan="2">
                          <div><b>${uiLabelMap.AccountingNoPaymentMethods}</b></div>
                        </td>
                      </tr>
                    </#if>
                  <#else>
                  <#list paymentMethodList as paymentMethod>
                    <#if paymentMethod.paymentMethodTypeId == "CREDIT_CARD">
                     <#if productStorePaymentMethodTypeIdMap.CREDIT_CARD??>
                      <#assign creditCard = paymentMethod.getRelatedOne("CreditCard", false)>
                      <tr>
                        <td width="1%">
                          <input type="radio" name="checkOutPaymentId" value="${paymentMethod.paymentMethodId}" <#if shoppingCart.isPaymentSelected(paymentMethod.paymentMethodId)>checked="checked"</#if>/>
                        </td>
                        <td width="50%">
                          <span>CC:&nbsp;${Static["org.ofbiz.party.contact.ContactHelper"].formatCreditCard(creditCard)}</span>
                          <a href="javascript:submitForm(document.checkoutInfoForm, 'EC', '${paymentMethod.paymentMethodId}');" class="buttontext">${uiLabelMap.CommonUpdate}</a>
                          <#if paymentMethod.description?has_content><br /><span>(${paymentMethod.description})</span></#if>
                          &nbsp;${uiLabelMap.OrderCardSecurityCode}&nbsp;<input type="text" size="5" maxlength="10" name="securityCode_${paymentMethod.paymentMethodId}" value=""/>
                        </td>
                      </tr>
                     </#if>
                    <#elseif paymentMethod.paymentMethodTypeId == "EFT_ACCOUNT">
                     <#if productStorePaymentMethodTypeIdMap.EFT_ACCOUNT??>
                      <#assign eftAccount = paymentMethod.getRelatedOne("EftAccount", false)>
                      <tr>
                        <td width="1%">
                          <input type="radio" name="checkOutPaymentId" value="${paymentMethod.paymentMethodId}" <#if shoppingCart.isPaymentSelected(paymentMethod.paymentMethodId)>checked="checked"</#if>/>
                        </td>
                        <td width="50%">
                          <span>${uiLabelMap.AccountingEFTAccount}:&nbsp;${eftAccount.bankName!}: ${eftAccount.accountNumber!}</span>
                          <a href="javascript:submitForm(document.checkoutInfoForm, 'EE', '${paymentMethod.paymentMethodId}');" class="buttontext">${uiLabelMap.CommonUpdate}</a>
                          <#if paymentMethod.description?has_content><br /><span>(${paymentMethod.description})</span></#if>
                        </td>
                      </tr>
                     </#if>
                    <#elseif paymentMethod.paymentMethodTypeId == "GIFT_CARD">
                     <#if productStorePaymentMethodTypeIdMap.GIFT_CARD??>
                      <#assign giftCard = paymentMethod.getRelatedOne("GiftCard", false)>

                      <#if giftCard?has_content && giftCard.cardNumber?has_content>
                        <#assign giftCardNumber = "">
                        <#assign pcardNumber = giftCard.cardNumber>
                        <#if pcardNumber?has_content>
                          <#assign psize = pcardNumber?length - 4>
                          <#if 0 < psize>
                            <#list 0 .. psize-1 as foo>
                              <#assign giftCardNumber = giftCardNumber + "*">
                            </#list>
                            <#assign giftCardNumber = giftCardNumber + pcardNumber[psize .. psize + 3]>
                          <#else>
                            <#assign giftCardNumber = pcardNumber>
                          </#if>
                        </#if>
                      </#if>

                      <tr>
                        <td width="1%">
                          <input type="radio" name="checkOutPaymentId" value="${paymentMethod.paymentMethodId}" <#if shoppingCart.isPaymentSelected(paymentMethod.paymentMethodId)>checked="checked"</#if>/>
                        </td>
                        <td width="50%">
                          <span>${uiLabelMap.AccountingGift}:&nbsp;${giftCardNumber}</span>
                          <a href="javascript:submitForm(document.checkoutInfoForm, 'EG', '${paymentMethod.paymentMethodId}');" class="buttontext">[${uiLabelMap.CommonUpdate}]</a>
                          <#if paymentMethod.description?has_content><br /><span>(${paymentMethod.description})</span></#if>
                        </td>
                      </tr>
                     </#if>
                    </#if>
                  </#list>
                  </#if>

                <#-- special billing account functionality to allow use w/ a payment method -->
                <#if productStorePaymentMethodTypeIdMap.EXT_BILLACT??>
                  <#if billingAccountList?has_content>
                    <tr><td colspan="2"><hr /></td></tr>
                    <tr>
                      <td width="1%">
                        <select name="billingAccountId">
                          <option value=""></option>
                            <#list billingAccountList as billingAccount>
                              <#assign availableAmount = billingAccount.accountBalance?double>
                              <#assign accountLimit = billingAccount.accountLimit?double>
                              <option value="${billingAccount.billingAccountId}" <#if billingAccount.billingAccountId == selectedBillingAccountId?default("")>selected="selected"</#if>>${billingAccount.description?default("")} [${billingAccount.billingAccountId}] Available: <@ofbizCurrency amount=availableAmount isoCode=billingAccount.accountCurrencyUomId/> Limit: <@ofbizCurrency amount=accountLimit isoCode=billingAccount.accountCurrencyUomId/></option>
                            </#list>
                        </select>
                      </td>
                      <td width="50%">
                        <span>${uiLabelMap.FormFieldTitle_billingAccountId}</span>
                      </td>
                    </tr>
                    <tr>
                      <td width="1%" align="right">
                        <input type="text" size="5" name="billingAccountAmount" value=""/>
                      </td>
                      <td width="50%">
                        ${uiLabelMap.OrderBillUpTo}
                      </td>
                    </tr>
                  </#if>
                </#if>
                <#-- end of special billing account functionality -->

                <#if productStorePaymentMethodTypeIdMap.GIFT_CARD??>
                  <tr><td colspan="2"><hr /></td></tr>
                  <tr>
                    <td width="1%">
                      <input type="checkbox" name="addGiftCard" value="Y"/>
                    </td>
                    <td width="50%">
                      <span>${uiLabelMap.AccountingUseGiftCardNotOnFile}</span>
                    </td>
                  </tr>
                  <tr>
                    <td width="1%">
                      <div>${uiLabelMap.AccountingNumber}</div>
                    </td>
                    <td width="50%">
                      <input type="text" size="15" name="giftCardNumber" value="${(requestParameters.giftCardNumber)!}" onFocus="document.checkoutInfoForm.addGiftCard.checked=true;"/>
                    </td>
                  </tr>
                  <#if shoppingCart.isPinRequiredForGC(delegator)>
                  <tr>
                    <td width="1%">
                      <div>${uiLabelMap.AccountingPIN}</div>
                    </td>
                    <td width="50%">
                      <input type="text" size="10" name="giftCardPin" value="${(requestParameters.giftCardPin)!}" onFocus="document.checkoutInfoForm.addGiftCard.checked=true;"/>
                    </td>
                  </tr>
                  </#if>
                  <tr>
                    <td width="1%">
                      <div>${uiLabelMap.AccountingAmount}</div>
                    </td>
                    <td width="50%">
                      <input type="text" size="6" name="giftCardAmount" value="${(requestParameters.giftCardAmount)!}" onFocus="document.checkoutInfoForm.addGiftCard.checked=true;"/>
                    </td>
                  </tr>
                </#if>
                </table>
            </div>
        </div>
        <#-- End Payment Method Selection -->
      </td>
    </tr>
  </table>
</form>

<table width="100%">
  <tr valign="top">
    <td>
      &nbsp;<a href="javascript:submitForm(document.checkoutInfoForm, 'CS', '');" class="buttontextbig">${uiLabelMap.OrderBacktoShoppingCart}</a>
    </td>
    <td align="right">
      <a href="javascript:submitForm(document.checkoutInfoForm, 'DN', '');" class="buttontextbig">${uiLabelMap.OrderContinueToFinalOrderReview}</a>
    </td>
  </tr>
</table>
