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
  ${cardNumberDisplay?if_exists}
</#macro>

<div class="screenlet">
  <div class="screenlet-title-bar">
      <ul><li class="h3">&nbsp;${uiLabelMap.AccountingPaymentInformation}</li></ul>
      <br class="clear"/>
  </div>
  <div class="screenlet-body">
     <table class="basic-table" cellspacing='0'>
     <#-- order payment status -->
     <tr>
       <td align="center" valign="top" width="29%" class="label">&nbsp;${uiLabelMap.OrderStatusHistory}</td>
       <td width="1%">&nbsp;</td>
       <td width="60%">
         <#assign orderPaymentStatuses = orderReadHelper.getOrderPaymentStatuses()>
         <#if orderPaymentStatuses?has_content>
           <#list orderPaymentStatuses as orderPaymentStatus>
             <#assign statusItem = orderPaymentStatus.getRelatedOne("StatusItem")?if_exists>
             <#if statusItem?has_content>
                <div>
                  ${statusItem.get("description",locale)} - ${orderPaymentStatus.statusDatetime?default("0000-00-00 00:00:00")?string}
                  &nbsp;
                  ${uiLabelMap.CommonBy} - [${orderPaymentStatus.statusUserLogin?if_exists}]
                </div>
             </#if>
           </#list>
         </#if>
       </td>
       <td width="10%">&nbsp;</td>
     </tr>
     <tr><td colspan="4"><hr/></td></tr>
     <#if orderPaymentPreferences?has_content || billingAccount?has_content || invoices?has_content>
        <#list orderPaymentPreferences as orderPaymentPreference>
          <#assign pmBillingAddress = {}>
          <#assign oppStatusItem = orderPaymentPreference.getRelatedOne("StatusItem")>
          <#if outputted?default("false") == "true">
            <tr><td colspan="4"><hr/></td></tr>
          </#if>
          <#assign outputted = "true">
          <#-- try the paymentMethod first; if paymentMethodId is specified it overrides paymentMethodTypeId -->
          <#assign paymentMethod = orderPaymentPreference.getRelatedOne("PaymentMethod")?if_exists>
          <#if !paymentMethod?has_content>
            <#assign paymentMethodType = orderPaymentPreference.getRelatedOne("PaymentMethodType")>
            <#if paymentMethodType.paymentMethodTypeId == "EXT_BILLACT">
              <#assign outputted = "false">
            <#elseif paymentMethodType.paymentMethodTypeId == "FIN_ACCOUNT">
              <#assign finAccount = orderPaymentPreference.getRelatedOne("FinAccount")?if_exists/>
              <#if (finAccount?has_content)>
                <#assign gatewayResponses = orderPaymentPreference.getRelated("PaymentGatewayResponse")>
                <#assign finAccountType = finAccount.getRelatedOne("FinAccountType")?if_exists/>
                <tr>
                  <td align="right" valign="top" width="29%">
                    <div>
                    <span class="label">&nbsp;${uiLabelMap.AccountingFinAccount}</span>
                    <#if orderPaymentPreference.maxAmount?has_content>
                       <br/>${uiLabelMap.OrderPaymentMaximumAmount}: <@ofbizCurrency amount=orderPaymentPreference.maxAmount?default(0.00) isoCode=currencyUomId/>
                    </#if>
                    </div>
                  </td>
                  <td width="1%">&nbsp;</td>
                  <td valign="top" width="60%">
                    <div>
                      <#if (finAccountType?has_content)>
                        ${finAccountType.description?default(finAccountType.finAccountTypeId)}&nbsp;
                      </#if>
                      #${finAccount.finAccountCode?default(finAccount.finAccountId)} (<a href="/accounting/control/EditFinAccount?finAccountId=${finAccount.finAccountId}&amp;externalLoginKey=${externalLoginKey}" class="buttontext">${finAccount.finAccountId}</a>)
                      <br/>
                      ${finAccount.finAccountName?if_exists}
                      <br/>

                      <#-- Authorize and Capture transactions -->
                      <div>
                        <#if orderPaymentPreference.statusId != "PAYMENT_SETTLED">
                          <a href="/accounting/control/AuthorizeTransaction?orderId=${orderId?if_exists}&orderPaymentPreferenceId=${orderPaymentPreference.orderPaymentPreferenceId}&amp;externalLoginKey=${externalLoginKey}" class="buttontext">${uiLabelMap.AccountingAuthorize}</a>
                        </#if>
                        <#if orderPaymentPreference.statusId == "PAYMENT_AUTHORIZED">
                          <a href="/accounting/control/CaptureTransaction?orderId=${orderId?if_exists}&orderPaymentPreferenceId=${orderPaymentPreference.orderPaymentPreferenceId}&amp;externalLoginKey=${externalLoginKey}" class="buttontext">${uiLabelMap.AccountingCapture}</a>
                        </#if>
                      </div>
                    </div>
                    <#if gatewayResponses?has_content>
                      <div>
                        <hr/>
                        <#list gatewayResponses as gatewayResponse>
                          <#assign transactionCode = gatewayResponse.getRelatedOne("TranCodeEnumeration")>
                          ${(transactionCode.get("description",locale))?default("Unknown")}:
                          ${gatewayResponse.transactionDate.toString()}
                          <@ofbizCurrency amount=gatewayResponse.amount isoCode=currencyUomId/><br/>
                          (<span class"label">${uiLabelMap.OrderReference}</span>&nbsp;${gatewayResponse.referenceNum?if_exists}
                          <span class"label">${uiLabelMap.OrderAvs}</span>&nbsp;${gatewayResponse.gatewayAvsResult?default("N/A")}
                          <span class"label">${uiLabelMap.OrderScore}</span>&nbsp;${gatewayResponse.gatewayScoreResult?default("N/A")})
                          <a href="/accounting/control/ViewGatewayResponse?paymentGatewayResponseId=${gatewayResponse.paymentGatewayResponseId}&amp;externalLoginKey=${externalLoginKey}" class="buttontext">${uiLabelMap.CommonDetails}</a>
                          <#if gatewayResponse_has_next><hr/></#if>
                        </#list>
                      </div>
                    </#if>
                  </td>
                  <td width="10%">
                    <#if (!orderHeader.statusId.equals("ORDER_COMPLETED")) && !(orderHeader.statusId.equals("ORDER_REJECTED")) && !(orderHeader.statusId.equals("ORDER_CANCELLED"))>
                     <#if orderPaymentPreference.statusId != "PAYMENT_SETTLED">
                        <div>
                          <a href="javascript:document.CancelOrderPaymentPreference_${orderPaymentPreference.orderPaymentPreferenceId}.submit()" class="buttontext">${uiLabelMap.CommonCancel}</a>
                          <form name="CancelOrderPaymentPreference_${orderPaymentPreference.orderPaymentPreferenceId}" method="post" action="<@ofbizUrl>updateOrderPaymentPreference</@ofbizUrl>">
                            <input type="hidden" name="orderId" value="${orderId}">
                            <input type="hidden" name="orderPaymentPreferenceId" value="${orderPaymentPreference.orderPaymentPreferenceId}">
                            <input type="hidden" name="statusId" value="PAYMENT_CANCELLED">
                            <input type="hidden" name="checkOutPaymentId" value="${paymentMethod.paymentMethodTypeId?if_exists}">
                          </form>
                        </div>
                     </#if>
                    </#if>
                  </td>
                </tr>
              </#if>
            <#else>
              <tr>
                <td align="right" valign="top" width="29%">
                  <div>&nbsp;<span class"label">${paymentMethodType.get("description",locale)?if_exists}</span>&nbsp;
                  <#if orderPaymentPreference.maxAmount?has_content>
                  <br/>${uiLabelMap.OrderPaymentMaximumAmount}: <@ofbizCurrency amount=orderPaymentPreference.maxAmount?default(0.00) isoCode=currencyUomId/>
                  </#if>
                  </div>
                </td>
                <td width="1%">&nbsp;</td>
                <#if paymentMethodType.paymentMethodTypeId != "EXT_OFFLINE" && paymentMethodType.paymentMethodTypeId != "EXT_PAYPAL" && paymentMethodType.paymentMethodTypeId != "EXT_COD">
                  <td width="60%">
                    <div>
                      <#if orderPaymentPreference.maxAmount?has_content>
                         <br/>${uiLabelMap.OrderPaymentMaximumAmount}: <@ofbizCurrency amount=orderPaymentPreference.maxAmount?default(0.00) isoCode=currencyUomId/>
                      </#if>
                      <br/>&nbsp;[<#if oppStatusItem?exists>${oppStatusItem.get("description",locale)}<#else>${orderPaymentPreference.statusId}</#if>]
                    </div>
                    <#--
                    <div><@ofbizCurrency amount=orderPaymentPreference.maxAmount?default(0.00) isoCode=currencyUomId/>&nbsp;-&nbsp;${(orderPaymentPreference.authDate.toString())?if_exists}</div>
                    <div>&nbsp;<#if orderPaymentPreference.authRefNum?exists>(${uiLabelMap.OrderReference}: ${orderPaymentPreference.authRefNum})</#if></div>
                    -->
                  </td>
                <#else>
                  <td align="right" width="60%">
                    <a href="<@ofbizUrl>receivepayment?${paramString}</@ofbizUrl>" class="buttontext">${uiLabelMap.AccountingReceivePayment}</a>
                  </td>
                </#if>
                  <td width="10%">
                   <#if (!orderHeader.statusId.equals("ORDER_COMPLETED")) && !(orderHeader.statusId.equals("ORDER_REJECTED")) && !(orderHeader.statusId.equals("ORDER_CANCELLED"))>
                    <#if orderPaymentPreference.statusId != "PAYMENT_SETTLED">
                      <div>
                        <a href="javascript:document.CancelOrderPaymentPreference_${orderPaymentPreference.orderPaymentPreferenceId}.submit()" class="buttontext">${uiLabelMap.CommonCancel}</a>
                        <form name="CancelOrderPaymentPreference_${orderPaymentPreference.orderPaymentPreferenceId}" method="post" action="<@ofbizUrl>updateOrderPaymentPreference</@ofbizUrl>">
                          <input type="hidden" name="orderId" value="${orderId}">
                          <input type="hidden" name="orderPaymentPreferenceId" value="${orderPaymentPreference.orderPaymentPreferenceId}">
                          <input type="hidden" name="statusId" value="PAYMENT_CANCELLED">
                          <input type="hidden" name="checkOutPaymentId" value="${paymentMethod.paymentMethodTypeId?if_exists}">
                        </form>
                      </div>
                    </#if>
                   </#if>
                  </td>
                </tr>
            </#if>
          <#else>
            <#if paymentMethod.paymentMethodTypeId?if_exists == "CREDIT_CARD">
              <#assign gatewayResponses = orderPaymentPreference.getRelated("PaymentGatewayResponse")>
              <#assign creditCard = paymentMethod.getRelatedOne("CreditCard")?if_exists>
              <#if creditCard?has_content>
                <#assign pmBillingAddress = creditCard.getRelatedOne("PostalAddress")?if_exists>
              </#if>
              <tr>
                <td align="right" valign="top" width="29%">
                  <div>&nbsp;<span class="label">${uiLabelMap.AccountingCreditCard}</span>
                  <#if orderPaymentPreference.maxAmount?has_content>
                     <br/>${uiLabelMap.OrderPaymentMaximumAmount}: <@ofbizCurrency amount=orderPaymentPreference.maxAmount?default(0.00) isoCode=currencyUomId/>
                  </#if>
                  </div>
                </td>
                <td width="1%">&nbsp;</td>
                <td valign="top" width="60%">
                  <div>
                    <#if creditCard?has_content>
                      <#if creditCard.companyNameOnCard?exists>${creditCard.companyNameOnCard}<br/></#if>
                      <#if creditCard.titleOnCard?has_content>${creditCard.titleOnCard}&nbsp</#if>
                      ${creditCard.firstNameOnCard}&nbsp;
                      <#if creditCard.middleNameOnCard?has_content>${creditCard.middleNameOnCard}&nbsp</#if>
                      ${creditCard.lastNameOnCard?default("N/A")}
                      <#if creditCard.suffixOnCard?has_content>&nbsp;${creditCard.suffixOnCard}</#if>
                      <br/>

                      <#if security.hasEntityPermission("PAY_INFO", "_VIEW", session)>
                        ${creditCard.cardType}
                        <@maskSensitiveNumber cardNumber=creditCard.cardNumber?if_exists/>
                        ${creditCard.expireDate}
                        &nbsp;[<#if oppStatusItem?exists>${oppStatusItem.get("description",locale)}<#else>${orderPaymentPreference.statusId}</#if>]
                      <#else>
                        ${Static["org.ofbiz.party.contact.ContactHelper"].formatCreditCard(creditCard)}
                        &nbsp;[<#if oppStatusItem?exists>${oppStatusItem.get("description",locale)}<#else>${orderPaymentPreference.statusId}</#if>]
                      </#if>
                      <br/>

                      <#-- Authorize and Capture transactions -->
                      <div>
                        <#if orderPaymentPreference.statusId != "PAYMENT_SETTLED">
                          <a href="/accounting/control/AuthorizeTransaction?orderId=${orderId?if_exists}&orderPaymentPreferenceId=${orderPaymentPreference.orderPaymentPreferenceId}&amp;externalLoginKey=${externalLoginKey}" class="buttontext">${uiLabelMap.AccountingAuthorize}</a>
                        </#if>
                        <#if orderPaymentPreference.statusId == "PAYMENT_AUTHORIZED">
                          <a href="/accounting/control/CaptureTransaction?orderId=${orderId?if_exists}&orderPaymentPreferenceId=${orderPaymentPreference.orderPaymentPreferenceId}&amp;externalLoginKey=${externalLoginKey}" class="buttontext">${uiLabelMap.AccountingCapture}</a>
                        </#if>
                      </div>
                    <#else>
                      ${uiLabelMap.CommonInformation} ${uiLabelMap.CommonNot} ${uiLabelMap.CommonAvailable}
                    </#if>
                  </div>
                  <#if gatewayResponses?has_content>
                    <div>
                      <hr/>
                      <#list gatewayResponses as gatewayResponse>
                        <#assign transactionCode = gatewayResponse.getRelatedOne("TranCodeEnumeration")>
                        ${(transactionCode.get("description",locale))?default("Unknown")}:
                        ${gatewayResponse.transactionDate.toString()}
                        <@ofbizCurrency amount=gatewayResponse.amount isoCode=currencyUomId/><br/>
                        (<span class="label">${uiLabelMap.OrderReference}</span>&nbsp;${gatewayResponse.referenceNum?if_exists}
                        <span class="label">${uiLabelMap.OrderAvs}</span>&nbsp;${gatewayResponse.gatewayAvsResult?default("N/A")}
                        <span class="label">${uiLabelMap.OrderScore}</span>&nbsp;${gatewayResponse.gatewayScoreResult?default("N/A")})
                        <a href="/accounting/control/ViewGatewayResponse?paymentGatewayResponseId=${gatewayResponse.paymentGatewayResponseId}&amp;externalLoginKey=${externalLoginKey}" class="buttontext">${uiLabelMap.CommonDetails}</a>
                        <#if gatewayResponse_has_next><hr/></#if>
                      </#list>
                    </div>
                  </#if>
                </td>
                <td width="10%">
                  <#if (!orderHeader.statusId.equals("ORDER_COMPLETED")) && !(orderHeader.statusId.equals("ORDER_REJECTED")) && !(orderHeader.statusId.equals("ORDER_CANCELLED"))>
                   <#if orderPaymentPreference.statusId != "PAYMENT_SETTLED">
                      <a href="javascript:document.CancelOrderPaymentPreference_${orderPaymentPreference.orderPaymentPreferenceId}.submit()" class="buttontext">${uiLabelMap.CommonCancel}</a>
                      <form name="CancelOrderPaymentPreference_${orderPaymentPreference.orderPaymentPreferenceId}" method="post" action="<@ofbizUrl>updateOrderPaymentPreference</@ofbizUrl>">
                        <input type="hidden" name="orderId" value="${orderId}">
                        <input type="hidden" name="orderPaymentPreferenceId" value="${orderPaymentPreference.orderPaymentPreferenceId}">
                        <input type="hidden" name="statusId" value="PAYMENT_CANCELLED">
                        <input type="hidden" name="checkOutPaymentId" value="${paymentMethod.paymentMethodTypeId?if_exists}">
                      </form>
                   </#if>
                  </#if>
                </td>
              </tr>
            <#elseif paymentMethod.paymentMethodTypeId?if_exists == "EFT_ACCOUNT">
              <#assign eftAccount = paymentMethod.getRelatedOne("EftAccount")>
              <#if eftAccount?has_content>
                <#assign pmBillingAddress = eftAccount.getRelatedOne("PostalAddress")?if_exists>
              </#if>
              <tr>
                <td align="right" valign="top" width="29%">
                  <div>&nbsp;<span class="label">${uiLabelMap.AccountingEFTAccount}</span>
                  <#if orderPaymentPreference.maxAmount?has_content>
                  <br/>${uiLabelMap.OrderPaymentMaximumAmount}: <@ofbizCurrency amount=orderPaymentPreference.maxAmount?default(0.00) isoCode=currencyUomId/>
                  </#if>
                  </div>
                </td>
                <td width="1%">&nbsp;</td>
                <td valign="top" width="60%">
                  <div>
                    <#if eftAccount?has_content>
                      ${eftAccount.nameOnAccount?if_exists}<br/>
                      <#if eftAccount.companyNameOnAccount?exists>${eftAccount.companyNameOnAccount}<br/></#if>
                      ${uiLabelMap.AccountingBankName}: ${eftAccount.bankName}, ${eftAccount.routingNumber}<br/>
                      ${uiLabelMap.AccountingAccount}#: ${eftAccount.accountNumber}
                    <#else>
                      ${uiLabelMap.CommonInformation} ${uiLabelMap.CommonNot} ${uiLabelMap.CommonAvailable}
                    </#if>
                  </div>
                </td>
                <td width="10%">
                  <#if (!orderHeader.statusId.equals("ORDER_COMPLETED")) && !(orderHeader.statusId.equals("ORDER_REJECTED")) && !(orderHeader.statusId.equals("ORDER_CANCELLED"))>
                   <#if orderPaymentPreference.statusId != "PAYMENT_SETTLED">
                      <a href="javascript:document.CancelOrderPaymentPreference_${orderPaymentPreference.orderPaymentPreferenceId}.submit()" class="buttontext">${uiLabelMap.CommonCancel}</a>
                      <form name="CancelOrderPaymentPreference_${orderPaymentPreference.orderPaymentPreferenceId}" method="post" action="<@ofbizUrl>updateOrderPaymentPreference</@ofbizUrl>">
                        <input type="hidden" name="orderId" value="${orderId}">
                        <input type="hidden" name="orderPaymentPreferenceId" value="${orderPaymentPreference.orderPaymentPreferenceId}">
                        <input type="hidden" name="statusId" value="PAYMENT_CANCELLED">
                        <input type="hidden" name="checkOutPaymentId" value="${paymentMethod.paymentMethodTypeId?if_exists}">
                      </form>
                   </#if>
                  </#if>
                </td>
              </tr>
            <#elseif paymentMethod.paymentMethodTypeId?if_exists == "GIFT_CARD">
              <#assign giftCard = paymentMethod.getRelatedOne("GiftCard")>
              <#if giftCard?exists>
                <#assign pmBillingAddress = giftCard.getRelatedOne("PostalAddress")?if_exists>
              </#if>
              <tr>
                <td align="right" valign="top" width="29%">
                  <div>&nbsp;<span class="label">${uiLabelMap.OrderGiftCard}</span>
                  <#if orderPaymentPreference.maxAmount?has_content>
                  <br/>${uiLabelMap.OrderPaymentMaximumAmount}: <@ofbizCurrency amount=orderPaymentPreference.maxAmount?default(0.00) isoCode=currencyUomId/>
                  </#if>
                  </div>
                </td>
                <td width="1%">&nbsp;</td>
                <td valign="top" width="60%">
                  <div>
                    <#if giftCard?has_content>
                      <#if security.hasEntityPermission("PAY_INFO", "_VIEW", session)>
                        ${giftCard.cardNumber?default("N/A")} [${giftCard.pinNumber?default("N/A")}]
                        &nbsp;[<#if oppStatusItem?exists>${oppStatusItem.get("description",locale)}<#else>${orderPaymentPreference.statusId}</#if>]
                      <#else>
                      <@maskSensitiveNumber cardNumber=giftCard.cardNumber?if_exists/>
                      <#if !cardNumberDisplay?has_content>N/A</#if>
                        &nbsp;[<#if oppStatusItem?exists>${oppStatusItem.get("description",locale)}<#else>${orderPaymentPreference.statusId}</#if>]
                      </#if>
                    <#else>
                      ${uiLabelMap.CommonInformation} ${uiLabelMap.CommonNot} ${uiLabelMap.CommonAvailable}
                    </#if>
                  </div>
                </td>
                <td width="10%">
                  <#if (!orderHeader.statusId.equals("ORDER_COMPLETED")) && !(orderHeader.statusId.equals("ORDER_REJECTED")) && !(orderHeader.statusId.equals("ORDER_CANCELLED"))>
                   <#if orderPaymentPreference.statusId != "PAYMENT_SETTLED">
                      <a href="javascript:document.CancelOrderPaymentPreference_${orderPaymentPreference.orderPaymentPreferenceId}.submit()" class="buttontext">${uiLabelMap.CommonCancel}</a>
                      <form name="CancelOrderPaymentPreference_${orderPaymentPreference.orderPaymentPreferenceId}" method="post" action="<@ofbizUrl>updateOrderPaymentPreference</@ofbizUrl>">
                        <input type="hidden" name="orderId" value="${orderId}">
                        <input type="hidden" name="orderPaymentPreferenceId" value="${orderPaymentPreference.orderPaymentPreferenceId}">
                        <input type="hidden" name="statusId" value="PAYMENT_CANCELLED">
                        <input type="hidden" name="checkOutPaymentId" value="${paymentMethod.paymentMethodTypeId?if_exists}">
                      </form>
                   </#if>
                  </#if>
                </td>
              </tr>
            </#if>
          </#if>
          <#if pmBillingAddress?has_content>
            <tr><td>&nbsp;</td><td>&nbsp;</td><td colspan="3"><hr/></td></tr>
            <tr>
              <td align="right" valign="top" width="29%">&nbsp;</td>
              <td width="1%">&nbsp;</td>
              <td valign="top" width="60%">
                <div>
                  <#if pmBillingAddress.toName?has_content><span class="label">${uiLabelMap.CommonTo}</span>&nbsp;${pmBillingAddress.toName}<br/></#if>
                  <#if pmBillingAddress.attnName?has_content><span class="label">${uiLabelMap.CommonAttn}</span>&nbsp;${pmBillingAddress.attnName}<br/></#if>
                  ${pmBillingAddress.address1}<br/>
                  <#if pmBillingAddress.address2?has_content>${pmBillingAddress.address2}<br/></#if>
                  ${pmBillingAddress.city}<#if pmBillingAddress.stateProvinceGeoId?has_content>, ${pmBillingAddress.stateProvinceGeoId} </#if>
                  ${pmBillingAddress.postalCode?if_exists}<br/>
                  ${pmBillingAddress.countryGeoId?if_exists}
                </div>
              </td>
              <td width="10%">&nbsp;</td>
            </tr>
          </#if>
        </#list>

        <#-- billing account -->
        <#if billingAccount?exists>
          <#if outputted?default("false") == "true">
            <tr><td colspan="4"><hr/></td></tr>
          </#if>
          <tr>
            <td align="right" valign="top" width="29%">
              <#-- billing accounts require a special OrderPaymentPreference because it is skipped from above section of OPPs -->
              <div>&nbsp;<span class="label">${uiLabelMap.AccountingBillingAccount}</span>&nbsp;
                  <#if billingAccountMaxAmount?has_content>
                  <br/>${uiLabelMap.OrderPaymentMaximumAmount}: <@ofbizCurrency amount=billingAccountMaxAmount?default(0.00) isoCode=currencyUomId/>
                  </#if>
                  </div>
            </td>
            <td width="1%">&nbsp;</td>
            <td valign="top" width="60%">
                #<a href="/accounting/control/EditBillingAccount?billingAccountId=${billingAccount.billingAccountId}&amp;externalLoginKey=${externalLoginKey}" class="buttontext">${billingAccount.billingAccountId}</a>  - ${billingAccount.description?if_exists}
            </td>
            <td width="10%">&nbsp;</td>
          </tr>
        </#if>
        <#if customerPoNumber?has_content>
          <tr><td colspan="4"><hr/></td></tr>
          <tr>
            <td align="right" valign="top" width="29%"><span class="label">${uiLabelMap.OrderPONumber}</span></td>
            <td width="1%">&nbsp;</td>
            <td valign="top" width="60%">${customerPoNumber?if_exists}</td>
            <td width="10%">&nbsp;</td>
          </tr>
        </#if>

        <#-- invoices -->
        <#if invoices?has_content>
          <tr><td colspan="4"><hr/></td></tr>
          <tr>
            <td align="right" valign="top" width="29%">&nbsp;<span class="label">${uiLabelMap.OrderInvoices}</span></td>
            <td width="1%">&nbsp;</td>
            <td valign="top" width="60%">
              <#list invoices as invoice>
                <div>${uiLabelMap.CommonNbr}<a href="/accounting/control/invoiceOverview?invoiceId=${invoice}&amp;externalLoginKey=${externalLoginKey}" class="buttontext">${invoice}</a>
                (<a href="/accounting/control/invoice.pdf?invoiceId=${invoice}" class="buttontext">PDF</a>)</div>
              </#list>
            </td>
            <td width="10%">&nbsp;</td>
          </tr>
        </#if>
   <#else>
    <tr>
     <td colspan="4" align="center">${uiLabelMap.OrderNoOrderPaymentPreferences}</td>
    </tr>
   </#if>
   <#if (!orderHeader.statusId.equals("ORDER_COMPLETED")) && !(orderHeader.statusId.equals("ORDER_REJECTED")) && !(orderHeader.statusId.equals("ORDER_CANCELLED")) && (paymentMethodValueMaps?has_content)>
   <tr><td colspan="4"><hr/></td></tr>
   <tr><td colspan="4">
   <form name="addPaymentMethodToOrder" method="post" action="<@ofbizUrl>addPaymentMethodToOrder</@ofbizUrl>">
   <input type="hidden" name="orderId" value="${orderId?if_exists}"/>
   <table class="basic-table" cellspacing='0'>
   <tr>
      <td width="29%" align="right" nowrap="nowrap"><span class="label">${uiLabelMap.AccountingPaymentMethod}</span></td>
      <td width="1%">&nbsp;</td>
      <td width="60%" nowrap="nowrap">
         <select name="paymentMethodId">
           <#list paymentMethodValueMaps as paymentMethodValueMap>
             <#assign paymentMethod = paymentMethodValueMap.paymentMethod/>
             <option value="${paymentMethod.get("paymentMethodId")?if_exists}">
               <#if "CREDIT_CARD" == paymentMethod.paymentMethodTypeId>
                 <#assign creditCard = paymentMethodValueMap.creditCard/>
                 <#if (creditCard?has_content)>
                   <#if security.hasEntityPermission("PAY_INFO", "_VIEW", session)>
                     ${creditCard.cardType?if_exists} <@maskSensitiveNumber cardNumber=creditCard.cardNumber?if_exists/> ${creditCard.expireDate?if_exists}
                   <#else>
                     ${Static["org.ofbiz.party.contact.ContactHelper"].formatCreditCard(creditCard)}
                   </#if>
                 </#if>
               <#else>
                 ${paymentMethod.paymentMethodTypeId?if_exists}
                 <#if paymentMethod.description?exists>${paymentMethod.description}</#if>
                   (${paymentMethod.paymentMethodId})
                 </#if>
               </option>
           </#list>
         </select>
      </td>
      <td width="10%">&nbsp;</td>
   </tr>
   <#assign openAmount = orderReadHelper.getOrderOpenAmount()>
   <tr>
      <td width="29%" align="right"><span class="label">${uiLabelMap.AccountingAmount}</span></td>
      <td width="1%">&nbsp;</td>
      <td width="60%" nowrap="nowrap">
         <input type="text" name="maxAmount" value="${openAmount}"/>
      </td>
      <td width="10%">&nbsp;</td>
   </tr>
   <tr>
      <td align="right" valign="top" width="29%">&nbsp;</td>
      <td width="1%">&nbsp;</td>
      <td valign="top" width="60%">
        <input type="submit" value="${uiLabelMap.CommonAdd}" class="smallSubmit"/>
      </td>
      <td width="10%">&nbsp;</td>
   </tr>
   </table>
   </form>
   </td></tr>
</#if>
</table>
</div>
</div>