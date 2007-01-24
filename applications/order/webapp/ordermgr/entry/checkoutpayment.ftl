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

<script language="javascript" type="text/javascript">
<!--
function submitForm(form, mode, value) {
    if (mode == "DN") {
        // done action; checkout
        form.action="<@ofbizUrl>checkoutoptions</@ofbizUrl>";
        form.submit();
    } else if (mode == "CS") {
        // continue shopping
        form.action="<@ofbizUrl>updateCheckoutOptions/showcart</@ofbizUrl>";
        form.submit();
    } else if (mode == "NC") {
        // new credit card
        form.action="<@ofbizUrl>updateCheckoutOptions/editcreditcard?DONE_PAGE=checkoutpayment</@ofbizUrl>";
        form.submit();
    } else if (mode == "EC") {
        // edit credit card
        form.action="<@ofbizUrl>updateCheckoutOptions/editcreditcard?DONE_PAGE=checkoutpayment&paymentMethodId="+value+"</@ofbizUrl>";
        form.submit();
    } else if (mode == "GC") {
        // edit gift card
        form.action="<@ofbizUrl>updateCheckoutOptions/editgiftcard?paymentMethodId="+value+"</@ofbizUrl>";
        form.submit();
    } else if (mode == "NE") {
        // new eft account
        form.action="<@ofbizUrl>updateCheckoutOptions/editeftaccount?DONE_PAGE=checkoutpayment</@ofbizUrl>";
        form.submit();
    } else if (mode == "EE") {
        // edit eft account
        form.action="<@ofbizUrl>updateCheckoutOptions/editeftaccount?DONE_PAGE=checkoutpayment&paymentMethodId="+value+"</@ofbizUrl>";
        form.submit();
    }else if(mode = "EG")
    //edit gift card
        form.action="<@ofbizUrl>updateCheckoutOptions/editgiftcard?DONE_PAGE=checkoutpayment&paymentMethodId="+value+"</@ofbizUrl>";
        form.submit();
}

function toggleBillingAccount(box) {
    var amountName = "amount_" + box.value;
    box.checked = true;
    box.form.elements[amountName].disabled = false;

    for (var i = 0; i < box.form.elements[box.name].length; i++) {
        if (!box.form.elements[box.name][i].checked) {
            box.form.elements["amount_" + box.form.elements[box.name][i].value].disabled = true;
        }
    }
}

// -->
</script>

<#assign cart = shoppingCart?if_exists>

<form method="post" name="checkoutInfoForm" style="margin:0;">
    <input type="hidden" name="checkoutpage" value="payment"/>
    <input type="hidden" name="BACK_PAGE" value="checkoutoptions"/>

    <div class="screenlet" style="height: 100%;">
        <div class="screenlet-header">
            <div class="boxhead">3)&nbsp;${uiLabelMap.OrderHowShallYouPay}?</div>
        </div>
        <div class="screenlet-body" style="height: 100%;">
            <#-- Payment Method Selection -->
            <table width="100%" cellpadding="1" cellspacing="0" border="0">
              <tr><td colspan="2">
                <span class='tabletext'>${uiLabelMap.CommonAdd}:</span>
                <#if productStorePaymentMethodTypeIdMap.CREDIT_CARD?exists>
                  <a href="javascript:submitForm(document.checkoutInfoForm, 'NC', '');" class="buttontext">${uiLabelMap.AccountingCreditCard}</a>
                </#if>
                <#if productStorePaymentMethodTypeIdMap.EFT_ACCOUNT?exists>
                  <a href="javascript:submitForm(document.checkoutInfoForm, 'NE', '');" class="buttontext">${uiLabelMap.AccountingEftAccount}</a>
                </#if>
              </td></tr>
              <tr><td colspan="3"><hr class="sepbar"/></td></tr>
              <#if productStorePaymentMethodTypeIdMap.EXT_OFFLINE?exists>
              <tr>
                <td width="1" nowrap>
                  <input type="radio" name="checkOutPaymentId" value="EXT_OFFLINE" <#if "EXT_OFFLINE" == checkOutPaymentId>checked</#if>>
                </td>
                <td width="1" nowrap>
                  <span class="tabletext">${uiLabelMap.OrderMoneyOrder}</span>
                </td>
                <td width="1" nowrap>&nbsp;</td>
              </tr>
              </#if>
              <#if productStorePaymentMethodTypeIdMap.EXT_COD?exists>
              <tr>
                <td width="1" nowrap>
                  <input type="radio" name="checkOutPaymentId" value="EXT_COD" <#if "EXT_COD" == checkOutPaymentId>checked</#if>>
                </td>
                <td width="1" nowrap>
                  <span class="tabletext">${uiLabelMap.OrderCOD}</span>
                </td>
                <td width="1" nowrap>&nbsp;</td>
              </tr>
              </#if>
              <#if productStorePaymentMethodTypeIdMap.EXT_WORLDPAY?exists>
              <tr>
                <td width="1" nowrap>
                  <input type="radio" name="checkOutPaymentId" value="EXT_WORLDPAY" <#if "EXT_WORLDPAY" == checkOutPaymentId>checked</#if>>
                </td>
                <td width="1" nowrap>
                  <span class="tabletext">${uiLabelMap.AccountingPayWithWorldPay}</span>
                </td>
                <td width="1" nowrap>&nbsp;</td>
              </tr>
              </#if>
              <#if productStorePaymentMethodTypeIdMap.EXT_PAYPAL?exists>
              <tr>
                <td width="1" nowrap>
                  <input type="radio" name="checkOutPaymentId" value="EXT_PAYPAL" <#if "EXT_PAYPAL" == checkOutPaymentId>checked</#if>>
                </td>
                <td width="1" nowrap>
                  <span class="tabletext">${uiLabelMap.AccountingPayWithPayPal}</span>
                </td>
                <td width="1" nowrap>&nbsp;</td>
              </tr>
              </#if>
              <tr><td colspan="3"><hr class="sepbar"/></td></tr>
            <#if !paymentMethodList?has_content>
              <tr>
                <td colspan="3">
                  <div class='tabletext'><b>${uiLabelMap.AccountingNoPaymentMethodsOnFile}.</b></div>
                </td>
              </tr>
            <#else/>
              <#list paymentMethodList as paymentMethod>
                <#if paymentMethod.paymentMethodTypeId == "GIFT_CARD">
                 <#if productStorePaymentMethodTypeIdMap.GIFT_CARD?exists>
                  <#assign giftCard = paymentMethod.getRelatedOne("GiftCard")>

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
                    <td width="1%" nowrap>
                      <input type="checkbox" name="checkOutPaymentId" value="${paymentMethod.paymentMethodId}" <#if cart.isPaymentSelected(paymentMethod.paymentMethodId)>checked</#if>>
                    </td>
                    <td width="1%" nowrap>
                      <span class="tabletext">${uiLabelMap.AccountingGift}:&nbsp;${giftCardNumber}
                        <#if paymentMethod.description?has_content>(${paymentMethod.description})</#if>
                      </span>
                      <span class="tabletext" align="right">
                        <a href="javascript:submitForm(document.checkoutInfoForm, 'EG', '${paymentMethod.paymentMethodId}');" class="buttontext">${uiLabelMap.CommonUpdate}</a>
                      </span>
                    </td>
                    <td>
                      &nbsp;
                      <span class="tabletext">
                        <b>${uiLabelMap.OrderBillUpTo}:</b> <input type="text" size="5" class="inputBox" name="amount_${paymentMethod.paymentMethodId}" value="<#if (cart.getPaymentAmount(paymentMethod.paymentMethodId)?default(0) > 0)>${cart.getPaymentAmount(paymentMethod.paymentMethodId)?double?string("##0.00")}</#if>">
                      </span>
                    </td>
                  </tr>
                 </#if>
                <#elseif paymentMethod.paymentMethodTypeId == "CREDIT_CARD">
                 <#if productStorePaymentMethodTypeIdMap.CREDIT_CARD?exists>
                  <#assign creditCard = paymentMethod.getRelatedOne("CreditCard")>
                  <tr>
                    <td width="1%" nowrap>
                      <input type="checkbox" name="checkOutPaymentId" value="${paymentMethod.paymentMethodId}" <#if cart.isPaymentSelected(paymentMethod.paymentMethodId)>checked</#if>>
                    </td>
                    <td width="1%" nowrap>
                      <span class="tabletext">CC:&nbsp;${Static["org.ofbiz.party.contact.ContactHelper"].formatCreditCard(creditCard)}
                        <#if paymentMethod.description?has_content>(${paymentMethod.description})</#if>
                      </span>
                      <span class="tabletext" align="right">
                        <a href="javascript:submitForm(document.checkoutInfoForm, 'EC', '${paymentMethod.paymentMethodId}');" class="buttontext">${uiLabelMap.CommonUpdate}</a>
                      </span>
                    </td>
                    <td>
                      &nbsp;
                      <span class="tabletext">
                        <b>${uiLabelMap.OrderBillUpTo}:</b> <input type="text" size="5" class="inputBox" name="amount_${paymentMethod.paymentMethodId}" value="<#if (cart.getPaymentAmount(paymentMethod.paymentMethodId)?default(0) > 0)>${cart.getPaymentAmount(paymentMethod.paymentMethodId)?double?string("##0.00")}</#if>">
                      </span>
                    </td>
                  </tr>
                 </#if>
                <#elseif paymentMethod.paymentMethodTypeId == "EFT_ACCOUNT">
                 <#if productStorePaymentMethodTypeIdMap.EFT_ACCOUNT?exists>
                  <#assign eftAccount = paymentMethod.getRelatedOne("EftAccount")>
                  <tr>
                    <td width="1%" nowrap>
                      <input type="radio" name="checkOutPaymentId" value="${paymentMethod.paymentMethodId}" <#if paymentMethod.paymentMethodId == checkOutPaymentId>checked</#if>>
                    </td>
                    <td width="1%" nowrap>
                      <span class="tabletext">EFT:&nbsp;${eftAccount.bankName?if_exists}: ${eftAccount.accountNumber?if_exists}
                        <#if paymentMethod.description?has_content>(${paymentMethod.description})</#if>
                      </span>
                      <a href="javascript:submitForm(document.checkoutInfoForm, 'EE', '${paymentMethod.paymentMethodId}');" class="buttontext">${uiLabelMap.CommonUpdate}</a>
                    </td>
                    <td>&nbsp;</td>
                  </tr>
                  <tr><td colspan="3"><hr class="sepbar"/></td></tr>
                 </#if>
                </#if>
              </#list>
            </#if>

            <#-- special billing account functionality to allow use w/ a payment method -->
            <#if productStorePaymentMethodTypeIdMap.EXT_BILLACT?exists>
              <#if billingAccountList?has_content>
                <tr><td colspan="3"><hr class="sepbar"/></td></tr>
                <tr>
                  <td width="1%" nowrap>
                    <input type="radio" name="checkOutPaymentId" value="EXT_BILLACT" <#if "EXT_BILLACT" == checkOutPaymentId>checked</#if>></hr>
                  </td>
                  <td width="50%" nowrap>
                    <span class="tabletext">${uiLabelMap.AccountingPayOnlyWithBillingAccount}</span>
                  </td>
                  <td>&nbsp;</td>
                </tr>
                <tr><td colspan="3"><hr class="sepbar"/></td></tr>
                <#list billingAccountList as billingAccount>
                  <#assign availableAmount = billingAccount.accountLimit?double - billingAccount.accountBalance?double>
                  <tr>
                    <td align="left" valign="top" width="1%" nowrap>
                      <input type="radio" onClick="javascript:toggleBillingAccount(this);" name="billingAccountId" value="${billingAccount.billingAccountId}" <#if (billingAccount.billingAccountId == selectedBillingAccountId?default(""))>checked</#if>>
                    </td>
                    <td align="left" valign="top" width="99%" nowrap>
                      <div class="tabletext">
                       ${billingAccount.description?default("Bill Account")} [${uiLabelMap.OrderNbr}<b>${billingAccount.billingAccountId}</b>]&nbsp;(<@ofbizCurrency amount=availableAmount isoCode=billingAccount.accountCurrencyUomId?default(cart.getCurrency())/>)<br/>
                       <b>${uiLabelMap.OrderBillUpTo}:</b> <input type="text" size="8" class="inputBox" name="amount_${billingAccount.billingAccountId}" value="${availableAmount?double?string("##0.00")}" <#if !(billingAccount.billingAccountId == selectedBillingAccountId?default(""))>disabled</#if>>
                      </div>
                    </td>
                    <td>&nbsp;</td>
                  </tr>
                </#list>
                <tr>
                  <td align="left" valign="top" width="1%" nowrap>
                    <input type="radio" onClick="javascript:toggleBillingAccount(this);" name="billingAccountId" value="_NA_" <#if (selectedBillingAccountId?default("") == "N")>checked</#if>>
                    <input type="hidden" name="_NA_amount" value="0.00">
                  </td>
                  <td align="left" valign="top" width="99%" nowrap>
                    <div class="tabletext">${uiLabelMap.AccountingNoBillingAccount}</div>
                   </td>
                   <td>&nbsp;</td>
                </tr>
              </#if>
            </#if>
            <#-- end of special billing account functionality -->

            <#if productStorePaymentMethodTypeIdMap.GIFT_CARD?exists>
              <tr><td colspan="3"><hr class="sepbar"/></td></tr>
              <tr>
                <td width="1%" nowrap>
                  <input type="checkbox" name="addGiftCard" value="Y">
                  <input type="hidden" name="singleUseGiftCard" value="Y">
                </td>
                <td colspan="2"nowrap>
                  <span class="tabletext">${uiLabelMap.AccountingUseGiftCardNotOnFile}</span>
                </td>
              </tr>
              <tr>
                <td>&nbsp;</td>
                <td width="1%" nowrap>
                  <div class="tabletext">${uiLabelMap.AccountingNumber}</div>
                </td>
                <td width="50%" nowrap>
                  <input type="text" size="15" class="inputBox" name="giftCardNumber" value="${(requestParameters.giftCardNumber)?if_exists}" onFocus="document.checkoutInfoForm.addGiftCard.checked=true;">
                </td>
              </tr>
              <#if cart.isPinRequiredForGC(delegator)>
              <tr>
                <td>&nbsp;</td>
                <td width="1%" nowrap>
                  <div class="tabletext">${uiLabelMap.AccountingPIN}</div>
                </td>
                <td width="50%" nowrap>
                  <input type="text" size="10" class="inputBox" name="giftCardPin" value="${(requestParameters.giftCardPin)?if_exists}" onFocus="document.checkoutInfoForm.addGiftCard.checked=true;">
                </td>
              </tr>
              </#if>
              <tr>
                <td>&nbsp;</td>
                <td width="1%" nowrap>
                  <div class="tabletext">${uiLabelMap.AccountingAmount}</div>
                </td>
                <td width="50%" nowrap>
                  <input type="text" size="6" class="inputBox" name="giftCardAmount" value="${(requestParameters.giftCardAmount)?if_exists}" onFocus="document.checkoutInfoForm.addGiftCard.checked=true;">
                </td>
              </tr>
            </#if>

              <tr><td colspan="3"><hr class="sepbar"/></td></tr>
              <tr>
                <td colspan="3">
                  <div class='tabletext' valign='middle'>
                    <#if productStorePaymentMethodTypeIdMap.CREDIT_CARD?exists><a href="<@ofbizUrl>setBilling?paymentMethodType=CC&singleUsePayment=Y</@ofbizUrl>" class="buttontext">${uiLabelMap.AccountingSingleUseCreditCard}</a>&nbsp;</#if>
                    <#if productStorePaymentMethodTypeIdMap.GIFT_CARD?exists><a href="<@ofbizUrl>setBilling?paymentMethodType=GC&singleUsePayment=Y</@ofbizUrl>" class="buttontext">${uiLabelMap.AccountingSingleUseGiftCard}</a>&nbsp;</#if>
                    <#if productStorePaymentMethodTypeIdMap.EFT_ACCOUNT?exists><a href="<@ofbizUrl>setBilling?paymentMethodType=EFT&singleUsePayment=Y</@ofbizUrl>" class="buttontext">${uiLabelMap.AccountingSingleUseEFTAccount}</a>&nbsp;</#if>
                  </div>
                </td>
              </tr>
            </table>
            <#-- End Payment Method Selection -->
        </div>
    </div>
</form>

<table width="100%">
  <tr valign="top">
    <td align="left">
      &nbsp;<a href="javascript:submitForm(document.checkoutInfoForm, 'CS', '');" class="buttontextbig">${uiLabelMap.OrderBacktoShoppingCart}</a>
    </td>
    <td align="right">
      <a href="javascript:submitForm(document.checkoutInfoForm, 'DN', '');" class="buttontextbig">${uiLabelMap.OrderContinueToFinalOrderReview}</a>
    </td>
  </tr>
</table>
