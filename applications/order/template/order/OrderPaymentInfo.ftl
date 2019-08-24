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
  <#local cardNumberDisplay = "">
  <#if cardNumber?has_content>
    <#local size = cardNumber?length - 4>
    <#if (size > 0)>
      <#list 0 .. size-1 as foo>
        <#local cardNumberDisplay = cardNumberDisplay + "*">
      </#list>
      <#local cardNumberDisplay = cardNumberDisplay + cardNumber[size .. size + 3]>
    <#else>
      <#-- but if the card number has less than four digits (ie, it was entered incorrectly), display it in full -->
      <#local cardNumberDisplay = cardNumber>
    </#if>
  </#if>
  ${cardNumberDisplay!}
</#macro>

<div class="screenlet">
  <div class="screenlet-title-bar">
      <ul><li class="h3">&nbsp;${uiLabelMap.AccountingPaymentInformation}</li></ul>
      <br class="clear"/>
  </div>
  <div class="screenlet-body">
     <table class="basic-table form-table" cellspacing='0'>
     <#assign orderTypeId = orderReadHelper.getOrderTypeId()>
     <#if "PURCHASE_ORDER" == orderTypeId>
       <tr>
         <th>${uiLabelMap.AccountingPaymentID}</th>
         <th>${uiLabelMap.CommonTo}</th>
         <th>${uiLabelMap.CommonAmount}</th>
         <th>${uiLabelMap.CommonStatus}</th>
       </tr>
       <#list orderPaymentPreferences as orderPaymentPreference>
         <#assign payments = orderPaymentPreference.getRelated("Payment", null, null, false)>
         <#list payments as payment>
           <#assign statusItem = payment.getRelatedOne("StatusItem", false)>
           <#assign partyName = delegator.findOne("PartyNameView", {"partyId" : payment.partyIdTo}, true)>
           <tr>
             <#if security.hasEntityPermission("PAY_INFO", "_VIEW", session) || security.hasEntityPermission("ACCOUNTING", "_VIEW", session)>
               <td class="label"><a href="/accounting/control/paymentOverview?paymentId=${payment.paymentId}">${payment.paymentId}</a></td>
             <#else>
               <td class="label">${payment.paymentId}</td>
             </#if>
             <td>${partyName.groupName!}${partyName.lastName!} ${partyName.firstName!} ${partyName.middleName!}
             <#if security.hasPermission("PARTYMGR_VIEW", session) || security.hasPermission("PARTYMGR_ADMIN", session)>
               [<a href="/partymgr/control/viewprofile?partyId=${partyId}">${partyId}</a>]
             <#else>
               [${partyId}]
             </#if>
             </td>
             <td><@ofbizCurrency amount=payment.amount!/></td>
             <td>${statusItem.description}</td>
           </tr>
         </#list>
       </#list>
       <#-- invoices -->
       <#if invoices?has_content>
         <tr><td colspan="4"><hr /></td></tr>
         <tr>
           <td class="label"><label>${uiLabelMap.OrderInvoices}</label></td>
           <td>
             <#list invoices as invoice>
               <div>${uiLabelMap.CommonNbr}<a href="/accounting/control/invoiceOverview?invoiceId=${invoice}${StringUtil.wrapString(externalKeyParam)}" class="buttontext">${invoice}</a>
               (<a target="_BLANK" href="/accounting/control/invoice.pdf?invoiceId=${invoice}${StringUtil.wrapString(externalKeyParam)}" class="buttontext">${uiLabelMap.CommonPdf}</a>)</div>
             </#list>
           </td>
         </tr>
       </#if>
     <#else>

     <#-- order payment status -->
     <tr>
       <td class="label">&nbsp;${uiLabelMap.OrderStatusHistory}</td>
       <td>
         <#assign orderPaymentStatuses = orderReadHelper.getOrderPaymentStatuses()>
         <#if orderPaymentStatuses?has_content>
           <#list orderPaymentStatuses as orderPaymentStatus>
             <#assign statusItem = orderPaymentStatus.getRelatedOne("StatusItem", false)!>
             <#if statusItem?has_content>
                <div>
                  ${statusItem.get("description",locale)} <#if orderPaymentStatus.statusDatetime?has_content>- ${Static["org.apache.ofbiz.base.util.UtilFormatOut"].formatDateTime(orderPaymentStatus.statusDatetime, "", locale, timeZone)!}</#if>
                  &nbsp;
                  ${uiLabelMap.CommonBy} - [${orderPaymentStatus.statusUserLogin!}]
                </div>
             </#if>
           </#list>
         </#if>
       </td>
       <td>&nbsp;</td>
     </tr>
     <tr><td colspan="4"><hr /></td></tr>
     <#if orderPaymentPreferences?has_content || billingAccount?has_content || invoices?has_content>
        <#list orderPaymentPreferences as orderPaymentPreference>
          <#assign paymentList = orderPaymentPreference.getRelated("Payment", null, null, false)>
          <#assign pmBillingAddress = {}>
          <#assign oppStatusItem = orderPaymentPreference.getRelatedOne("StatusItem", false)>
          <#if "true" == outputted?default("false")>
            <tr><td colspan="4"><hr /></td></tr>
          </#if>
          <#assign outputted = "true">
          <#-- try the paymentMethod first; if paymentMethodId is specified it overrides paymentMethodTypeId -->
          <#assign paymentMethod = orderPaymentPreference.getRelatedOne("PaymentMethod", false)!>
          <#if !paymentMethod?has_content>
            <#assign paymentMethodType = orderPaymentPreference.getRelatedOne("PaymentMethodType", false)>
            <#if "EXT_BILLACT" == paymentMethodType.paymentMethodTypeId>
                <#assign outputted = "false">
                <#-- billing account -->
                <#if billingAccount??>
                  <#if "true" == outputted?default("false")>
                    <tr><td colspan="4"><hr /></td></tr>
                  </#if>
                  <tr>
                    <td class="label">
                      <#-- billing accounts require a special OrderPaymentPreference because it is skipped from above section of OPPs -->
                      <div>&nbsp;<span class="label">${uiLabelMap.AccountingBillingAccount}</span>&nbsp;
                          <#if billingAccountMaxAmount?has_content>
                          <br />${uiLabelMap.OrderPaymentMaximumAmount}: <@ofbizCurrency amount=billingAccountMaxAmount?default(0.00) isoCode=currencyUomId/>
                          </#if>
                          </div>
                    </td>
                    <td>
                        <table class="basic-table" cellspacing='0'>
                            <tr>
                                <td>
                                    ${uiLabelMap.CommonNbr}<a href="/accounting/control/EditBillingAccount?billingAccountId=${billingAccount.billingAccountId}${StringUtil.wrapString(externalKeyParam)}" class="buttontext">${billingAccount.billingAccountId}</a>  - ${billingAccount.description!}
                                </td>
                                <td>
                                    <#if orderPaymentPreference.statusId != "PAYMENT_SETTLED" && orderPaymentPreference.statusId != "PAYMENT_RECEIVED">
                                        <a href="<@ofbizUrl>receivepayment?${paramString}</@ofbizUrl>" class="buttontext">${uiLabelMap.AccountingReceivePayment}</a>
                                    </#if>
                                </td>
                            </tr>
                        </table>
                    </td>
                    <td>
                        <#if ("ORDER_COMPLETED" != orderHeader.statusId) && "ORDER_REJECTED" != orderHeader.statusId && "ORDER_CANCELLED" != orderHeader.statusId>
                            <#if orderPaymentPreference.statusId != "PAYMENT_SETTLED">
                              <div>
                                <a href="javascript:document.CancelOrderPaymentPreference_${orderPaymentPreference.orderPaymentPreferenceId}.submit()" class="buttontext">${uiLabelMap.CommonCancel}</a>
                                <form name="CancelOrderPaymentPreference_${orderPaymentPreference.orderPaymentPreferenceId}" method="post" action="<@ofbizUrl>updateOrderPaymentPreference</@ofbizUrl>">
                                  <input type="hidden" name="orderId" value="${orderId}" />
                                  <input type="hidden" name="orderPaymentPreferenceId" value="${orderPaymentPreference.orderPaymentPreferenceId}" />
                                  <input type="hidden" name="statusId" value="PAYMENT_CANCELLED" />
                                  <input type="hidden" name="checkOutPaymentId" value="${paymentMethod.paymentMethodTypeId!}" />
                                </form>
                              </div>
                            </#if>
                        </#if>
                    </td>
                  </tr>
                </#if>
            <#elseif "FIN_ACCOUNT" == paymentMethodType.paymentMethodTypeId>
              <#assign finAccount = orderPaymentPreference.getRelatedOne("FinAccount", false)!/>
              <#if (finAccount?has_content)>
                <#assign gatewayResponses = orderPaymentPreference.getRelated("PaymentGatewayResponse", null, null, false)>
                <#assign finAccountType = finAccount.getRelatedOne("FinAccountType", false)!/>
                <tr>
                  <td>
                    <div>
                    <span class="label">&nbsp;${uiLabelMap.AccountingFinAccount}</span>
                    <#if orderPaymentPreference.maxAmount?has_content>
                       <br />${uiLabelMap.OrderPaymentMaximumAmount}: <@ofbizCurrency amount=orderPaymentPreference.maxAmount?default(0.00) isoCode=currencyUomId/>
                    </#if>
                    </div>
                  </td>
                  <td>
                    <div>
                      <#if (finAccountType?has_content)>
                        ${finAccountType.description?default(finAccountType.finAccountTypeId)}&nbsp;
                      </#if>
                      #${finAccount.finAccountCode?default(finAccount.finAccountId)} (<a href="/accounting/control/EditFinAccount?finAccountId=${finAccount.finAccountId}${StringUtil.wrapString(externalKeyParam)}" class="buttontext">${finAccount.finAccountId}</a>)
                      <br />
                      ${finAccount.finAccountName!}
                      <br />

                      <#-- Authorize and Capture transactions -->
                      <div>
                        <#if "PAYMENT_SETTLED" != orderPaymentPreference.statusId>
                          <a href="/accounting/control/AuthorizeTransaction?orderId=${orderId!}&amp;orderPaymentPreferenceId=${orderPaymentPreference.orderPaymentPreferenceId}${StringUtil.wrapString(externalKeyParam)}" class="buttontext">${uiLabelMap.AccountingAuthorize}</a>
                        </#if>
                        <#if "PAYMENT_AUTHORIZED" == orderPaymentPreference.statusId>
                          <a href="/accounting/control/CaptureTransaction?orderId=${orderId!}&amp;orderPaymentPreferenceId=${orderPaymentPreference.orderPaymentPreferenceId}${StringUtil.wrapString(externalKeyParam)}" class="buttontext">${uiLabelMap.AccountingCapture}</a>
                        </#if>
                      </div>
                    </div>
                    <#if gatewayResponses?has_content>
                      <div>
                        <hr />
                        <#list gatewayResponses as gatewayResponse>
                          <#assign transactionCode = gatewayResponse.getRelatedOne("TranCodeEnumeration", false)>
                          ${(transactionCode.get("description",locale))?default("Unknown")}:
                          <#if gatewayResponse.transactionDate?has_content>${Static["org.apache.ofbiz.base.util.UtilFormatOut"].formatDateTime(gatewayResponse.transactionDate, "", locale, timeZone)!} </#if>
                          <@ofbizCurrency amount=gatewayResponse.amount isoCode=currencyUomId/><br />
                          (<span class="label">${uiLabelMap.OrderReference}</span>&nbsp;${gatewayResponse.referenceNum!}
                          <span class="label">${uiLabelMap.OrderAvs}</span>&nbsp;${gatewayResponse.gatewayAvsResult?default("N/A")}
                          <span class="label">${uiLabelMap.OrderScore}</span>&nbsp;${gatewayResponse.gatewayScoreResult?default("N/A")})
                          <a href="/accounting/control/ViewGatewayResponse?paymentGatewayResponseId=${gatewayResponse.paymentGatewayResponseId}${StringUtil.wrapString(externalKeyParam)}" class="buttontext">${uiLabelMap.CommonDetails}</a>
                          <#if gatewayResponse_has_next><hr /></#if>
                        </#list>
                      </div>
                    </#if>
                  </td>
                  <td>
                    <#if ("ORDER_COMPLETED" != orderHeader.statusId) && "ORDER_REJECTED" != orderHeader.statusId && "ORDER_CANCELLED" != orderHeader.statusId>
                     <#if orderPaymentPreference.statusId != "PAYMENT_SETTLED">
                        <div>
                          <a href="javascript:document.CancelOrderPaymentPreference_${orderPaymentPreference.orderPaymentPreferenceId}.submit()" class="buttontext">${uiLabelMap.CommonCancel}</a>
                          <form name="CancelOrderPaymentPreference_${orderPaymentPreference.orderPaymentPreferenceId}" method="post" action="<@ofbizUrl>updateOrderPaymentPreference</@ofbizUrl>">
                            <input type="hidden" name="orderId" value="${orderId}" />
                            <input type="hidden" name="orderPaymentPreferenceId" value="${orderPaymentPreference.orderPaymentPreferenceId}" />
                            <input type="hidden" name="statusId" value="PAYMENT_CANCELLED" />
                            <input type="hidden" name="checkOutPaymentId" value="${paymentMethod.paymentMethodTypeId!}" />
                          </form>
                        </div>
                     </#if>
                    </#if>
                  </td>
                </tr>
                <#if paymentList?has_content>
                    <tr>
                    <td>
                      <div>&nbsp;<span class="label">${uiLabelMap.AccountingInvoicePayments}</span></div>
                    </td>
                      <td>
                        <div>
                            <#list paymentList as paymentMap>
                                <a href="/accounting/control/paymentOverview?paymentId=${paymentMap.paymentId}${StringUtil.wrapString(externalKeyParam)}" class="buttontext">${paymentMap.paymentId}</a><#if paymentMap_has_next><br /></#if>
                            </#list>
                        </div>
                      </td>
                    </tr>
                </#if>
              </#if>
            <#else>
              <tr>
                <td>
                  <div>&nbsp;<span class="label">${paymentMethodType.get("description",locale)!}</span>&nbsp;
                  <#if orderPaymentPreference.maxAmount?has_content>
                  <br />${uiLabelMap.OrderPaymentMaximumAmount}: <@ofbizCurrency amount=orderPaymentPreference.maxAmount?default(0.00) isoCode=currencyUomId/>
                  </#if>
                  </div>
                </td>
                <#if paymentMethodType.paymentMethodTypeId != "EXT_OFFLINE" && paymentMethodType.paymentMethodTypeId != "EXT_PAYPAL" && paymentMethodType.paymentMethodTypeId != "EXT_COD">
                  <td>
                    <div>
                      <#if orderPaymentPreference.maxAmount?has_content>
                         <br />${uiLabelMap.OrderPaymentMaximumAmount}: <@ofbizCurrency amount=orderPaymentPreference.maxAmount?default(0.00) isoCode=currencyUomId/>
                      </#if>
                      <br />&nbsp;[<#if oppStatusItem??>${oppStatusItem.get("description",locale)}<#else>${orderPaymentPreference.statusId}</#if>]
                    </div>
                  </td>
                <#else>
                  <td>
                    <a href="<@ofbizUrl>receivepayment?${paramString}</@ofbizUrl>" class="buttontext">${uiLabelMap.AccountingReceivePayment}</a>
                  </td>
                </#if>
                  <td>
                   <#if ("ORDER_COMPLETED" != orderHeader.statusId) && "ORDER_REJECTED" != orderHeader.statusId && "ORDER_CANCELLED" != orderHeader.statusId>
                    <#if orderPaymentPreference.statusId != "PAYMENT_SETTLED">
                      <div>
                        <a href="javascript:document.CancelOrderPaymentPreference_${orderPaymentPreference.orderPaymentPreferenceId}.submit()" class="buttontext">${uiLabelMap.CommonCancel}</a>
                        <form name="CancelOrderPaymentPreference_${orderPaymentPreference.orderPaymentPreferenceId}" method="post" action="<@ofbizUrl>updateOrderPaymentPreference</@ofbizUrl>">
                          <input type="hidden" name="orderId" value="${orderId}" />
                          <input type="hidden" name="orderPaymentPreferenceId" value="${orderPaymentPreference.orderPaymentPreferenceId}" />
                          <input type="hidden" name="statusId" value="PAYMENT_CANCELLED" />
                          <input type="hidden" name="checkOutPaymentId" value="${paymentMethod.paymentMethodTypeId!}" />
                        </form>
                      </div>
                    </#if>
                   </#if>
                  </td>
                </tr>
                <#if paymentList?has_content>
                    <tr>
                    <td>
                      <div>&nbsp;<span class="label">${uiLabelMap.AccountingInvoicePayments}</span></div>
                    </td>
                      <td>
                        <div>
                            <#list paymentList as paymentMap>
                                <a href="/accounting/control/paymentOverview?paymentId=${paymentMap.paymentId}${StringUtil.wrapString(externalKeyParam)}" class="buttontext">${paymentMap.paymentId}</a><#if paymentMap_has_next><br /></#if>
                            </#list>
                        </div>
                      </td>
                    </tr>
                </#if>
            </#if>
          <#else>
            <#if "CREDIT_CARD" == paymentMethod.paymentMethodTypeId!>
              <#assign gatewayResponses = orderPaymentPreference.getRelated("PaymentGatewayResponse", null, null, false)>
              <#assign creditCard = paymentMethod.getRelatedOne("CreditCard", false)!>
              <#if creditCard?has_content>
                <#assign pmBillingAddress = creditCard.getRelatedOne("PostalAddress", false)!>
              </#if>
              <tr>
                <td class="label">
                  <div><label>${uiLabelMap.AccountingCreditCard}</label>
                  <#if orderPaymentPreference.maxAmount?has_content>
                     <br /><p>${uiLabelMap.OrderPaymentMaximumAmount}: <@ofbizCurrency amount=orderPaymentPreference.maxAmount?default(0.00) isoCode=currencyUomId/></p>
                  </#if>
                  </div>
                </td>
                <td>
                  <div>
                    <#if creditCard?has_content>
                      <#if creditCard.companyNameOnCard??>${creditCard.companyNameOnCard}<br /></#if>
                      <#if creditCard.titleOnCard?has_content>${creditCard.titleOnCard}&nbsp;</#if>
                      ${creditCard.firstNameOnCard?default("N/A")}&nbsp;
                      <#if creditCard.middleNameOnCard?has_content>${creditCard.middleNameOnCard}&nbsp;</#if>
                      ${creditCard.lastNameOnCard?default("N/A")}
                      <#if creditCard.suffixOnCard?has_content>&nbsp;${creditCard.suffixOnCard}</#if>
                      <br />

                      <#if security.hasEntityPermission("PAY_INFO", "_VIEW", session) || security.hasEntityPermission("ACCOUNTING", "_VIEW", session)>
                        ${creditCard.cardType}
                        <@maskSensitiveNumber cardNumber=creditCard.cardNumber!/>
                        ${creditCard.expireDate}
                        &nbsp;[<#if oppStatusItem??>${oppStatusItem.get("description",locale)}<#else>${orderPaymentPreference.statusId}</#if>]
                      <#else>
                        ${Static["org.apache.ofbiz.party.contact.ContactHelper"].formatCreditCard(creditCard)}
                        &nbsp;[<#if oppStatusItem??>${oppStatusItem.get("description",locale)}<#else>${orderPaymentPreference.statusId}</#if>]
                      </#if>
                      <br />

                      <#-- Authorize and Capture transactions -->
                      <div>
                        <#if "PAYMENT_SETTLED" != orderPaymentPreference.statusId>
                          <a href="/accounting/control/AuthorizeTransaction?orderId=${orderId!}&amp;orderPaymentPreferenceId=${orderPaymentPreference.orderPaymentPreferenceId}${StringUtil.wrapString(externalKeyParam)}" class="buttontext">${uiLabelMap.AccountingAuthorize}</a>
                        </#if>
                        <#if "PAYMENT_AUTHORIZED" == orderPaymentPreference.statusId>
                          <a href="/accounting/control/CaptureTransaction?orderId=${orderId!}&amp;orderPaymentPreferenceId=${orderPaymentPreference.orderPaymentPreferenceId}${StringUtil.wrapString(externalKeyParam)}" class="buttontext">${uiLabelMap.AccountingCapture}</a>
                        </#if>
                      </div>
                    <#else>
                      ${uiLabelMap.CommonInformation} ${uiLabelMap.CommonNot} ${uiLabelMap.CommonAvailable}
                    </#if>
                  </div>
                  <#if gatewayResponses?has_content>
                    <div>
                      <hr />
                      <#list gatewayResponses as gatewayResponse>
                        <#assign transactionCode = gatewayResponse.getRelatedOne("TranCodeEnumeration", false)>
                        ${(transactionCode.get("description",locale))?default("Unknown")}:
                        <#if gatewayResponse.transactionDate?has_content>${Static["org.apache.ofbiz.base.util.UtilFormatOut"].formatDateTime(gatewayResponse.transactionDate, "", locale, timeZone)!} </#if>
                        <@ofbizCurrency amount=gatewayResponse.amount isoCode=currencyUomId/><br />
                        (<span class="label">${uiLabelMap.OrderReference}</span>&nbsp;${gatewayResponse.referenceNum!}
                        <span class="label">${uiLabelMap.OrderAvs}</span>&nbsp;${gatewayResponse.gatewayAvsResult?default("N/A")}
                        <span class="label">${uiLabelMap.OrderScore}</span>&nbsp;${gatewayResponse.gatewayScoreResult?default("N/A")})
                        <a href="/accounting/control/ViewGatewayResponse?paymentGatewayResponseId=${gatewayResponse.paymentGatewayResponseId}${StringUtil.wrapString(externalKeyParam)}" class="buttontext">${uiLabelMap.CommonDetails}</a>
                        <#if gatewayResponse_has_next><hr /></#if>
                      </#list>
                    </div>
                  </#if>
                </td>
                <td>
                  <#if ("ORDER_COMPLETED" != orderHeader.statusId) && "ORDER_REJECTED" != orderHeader.statusId && "ORDER_CANCELLED" != orderHeader.statusId>
                   <#if orderPaymentPreference.statusId != "PAYMENT_SETTLED">
                      <a href="javascript:document.CancelOrderPaymentPreference_${orderPaymentPreference.orderPaymentPreferenceId}.submit()" class="buttontext">${uiLabelMap.CommonCancel}</a>
                      <form name="CancelOrderPaymentPreference_${orderPaymentPreference.orderPaymentPreferenceId}" method="post" action="<@ofbizUrl>updateOrderPaymentPreference</@ofbizUrl>">
                        <input type="hidden" name="orderId" value="${orderId}" />
                        <input type="hidden" name="orderPaymentPreferenceId" value="${orderPaymentPreference.orderPaymentPreferenceId}" />
                        <input type="hidden" name="statusId" value="PAYMENT_CANCELLED" />
                        <input type="hidden" name="checkOutPaymentId" value="${paymentMethod.paymentMethodTypeId!}" />
                      </form>
                   </#if>
                  </#if>
                </td>
              </tr>
            <#elseif "EFT_ACCOUNT" == paymentMethod.paymentMethodTypeId!>
              <#assign eftAccount = paymentMethod.getRelatedOne("EftAccount", false)>
              <#if eftAccount?has_content>
                <#assign pmBillingAddress = eftAccount.getRelatedOne("PostalAddress", false)!>
              </#if>
              <tr>
                <td>
                  <div>&nbsp;<span class="label">${uiLabelMap.AccountingEFTAccount}</span>
                  <#if orderPaymentPreference.maxAmount?has_content>
                  <br />${uiLabelMap.OrderPaymentMaximumAmount}: <@ofbizCurrency amount=orderPaymentPreference.maxAmount?default(0.00) isoCode=currencyUomId/>
                  </#if>
                  </div>
                </td>
                <td>
                  <div>
                    <#if eftAccount?has_content>
                      ${eftAccount.nameOnAccount!}<br />
                      <#if eftAccount.companyNameOnAccount??>${eftAccount.companyNameOnAccount}<br /></#if>
                      ${uiLabelMap.AccountingBankName}: ${eftAccount.bankName}, ${eftAccount.routingNumber}<br />
                      ${uiLabelMap.AccountingAccount}#: ${eftAccount.accountNumber}
                    <#else>
                      ${uiLabelMap.CommonInformation} ${uiLabelMap.CommonNot} ${uiLabelMap.CommonAvailable}
                    </#if>
                  </div>
                </td>
                <td>
                  <#if ("ORDER_COMPLETED" != orderHeader.statusId) && "ORDER_REJECTED" != orderHeader.statusId && "ORDER_CANCELLED" != orderHeader.statusId>
                   <#if orderPaymentPreference.statusId != "PAYMENT_SETTLED">
                      <a href="javascript:document.CancelOrderPaymentPreference_${orderPaymentPreference.orderPaymentPreferenceId}.submit()" class="buttontext">${uiLabelMap.CommonCancel}</a>
                      <form name="CancelOrderPaymentPreference_${orderPaymentPreference.orderPaymentPreferenceId}" method="post" action="<@ofbizUrl>updateOrderPaymentPreference</@ofbizUrl>">
                        <input type="hidden" name="orderId" value="${orderId}" />
                        <input type="hidden" name="orderPaymentPreferenceId" value="${orderPaymentPreference.orderPaymentPreferenceId}" />
                        <input type="hidden" name="statusId" value="PAYMENT_CANCELLED" />
                        <input type="hidden" name="checkOutPaymentId" value="${paymentMethod.paymentMethodTypeId!}" />
                      </form>
                   </#if>
                  </#if>
                </td>
              </tr>
              <#if paymentList?has_content>
                <tr>
                <td>
                  <div>&nbsp;<span class="label">${uiLabelMap.AccountingInvoicePayments}</span></div>
                </td>
                  <td>
                    <div>
                        <#list paymentList as paymentMap>
                            <a href="/accounting/control/paymentOverview?paymentId=${paymentMap.paymentId}${StringUtil.wrapString(externalKeyParam)}" class="buttontext">${paymentMap.paymentId}</a><#if paymentMap_has_next><br /></#if>
                        </#list>
                    </div>
                  </td>
                </tr>
              </#if>
            <#elseif "GIFT_CARD" == paymentMethod.paymentMethodTypeId!>
              <#assign giftCard = paymentMethod.getRelatedOne("GiftCard", false)>
              <#if giftCard??>
                <#assign pmBillingAddress = giftCard.getRelatedOne("PostalAddress", false)!>
              </#if>
              <tr>
                <td>
                  <div>&nbsp;<span class="label">${uiLabelMap.OrderGiftCard}</span>
                  <#if orderPaymentPreference.maxAmount?has_content>
                  <br />${uiLabelMap.OrderPaymentMaximumAmount}: <@ofbizCurrency amount=orderPaymentPreference.maxAmount?default(0.00) isoCode=currencyUomId/>
                  </#if>
                  </div>
                </td>
                <td>
                  <div>
                    <#if giftCard?has_content>
                      <#if security.hasEntityPermission("PAY_INFO", "_VIEW", session) || security.hasEntityPermission("ACCOUNTING", "_VIEW", session)>
                        ${giftCard.cardNumber?default("N/A")} [${giftCard.pinNumber?default("N/A")}]
                        &nbsp;[<#if oppStatusItem??>${oppStatusItem.get("description",locale)}<#else>${orderPaymentPreference.statusId}</#if>]
                      <#else>
                      <@maskSensitiveNumber cardNumber=giftCard.cardNumber!/>
                      <#if !cardNumberDisplay?has_content>N/A</#if>
                        &nbsp;[<#if oppStatusItem??>${oppStatusItem.get("description",locale)}<#else>${orderPaymentPreference.statusId}</#if>]
                      </#if>
                    <#else>
                      ${uiLabelMap.CommonInformation} ${uiLabelMap.CommonNot} ${uiLabelMap.CommonAvailable}
                    </#if>
                  </div>
                </td>
                <td>
                  <#if ("ORDER_COMPLETED" != orderHeader.statusId) && "ORDER_REJECTED" != orderHeader.statusId && "ORDER_CANCELLED" != orderHeader.statusId>
                   <#if orderPaymentPreference.statusId != "PAYMENT_SETTLED">
                      <a href="javascript:document.CancelOrderPaymentPreference_${orderPaymentPreference.orderPaymentPreferenceId}.submit()" class="buttontext">${uiLabelMap.CommonCancel}</a>
                      <form name="CancelOrderPaymentPreference_${orderPaymentPreference.orderPaymentPreferenceId}" method="post" action="<@ofbizUrl>updateOrderPaymentPreference</@ofbizUrl>">
                        <input type="hidden" name="orderId" value="${orderId}" />
                        <input type="hidden" name="orderPaymentPreferenceId" value="${orderPaymentPreference.orderPaymentPreferenceId}" />
                        <input type="hidden" name="statusId" value="PAYMENT_CANCELLED" />
                        <input type="hidden" name="checkOutPaymentId" value="${paymentMethod.paymentMethodTypeId!}" />
                      </form>
                   </#if>
                  </#if>
                </td>
              </tr>
              <#if paymentList?has_content>
                <tr>
                <td>
                  <div>&nbsp;<span class="label">${uiLabelMap.AccountingInvoicePayments}</span></div>
                </td>
                  <td>
                    <div>
                        <#list paymentList as paymentMap>
                            <a href="/accounting/control/paymentOverview?paymentId=${paymentMap.paymentId}${StringUtil.wrapString(externalKeyParam)}" class="buttontext">${paymentMap.paymentId}</a><#if paymentMap_has_next><br /></#if>
                        </#list>
                    </div>
                  </td>
                </tr>
              </#if>
            </#if>
          </#if>
          <#if pmBillingAddress?has_content>
            <tr><td>&nbsp;</td><td colspan="1"><hr /></td><td></td></tr>
            <tr>
              <td>&nbsp;</td>
              <td>
                <div>
                  <#if pmBillingAddress.toName?has_content><b>${uiLabelMap.CommonTo}:</b>&nbsp;${pmBillingAddress.toName}<br /></#if>
                  <#if pmBillingAddress.attnName?has_content><span class="label">${uiLabelMap.CommonAttn}</span>&nbsp;${pmBillingAddress.attnName}<br /></#if>
                  ${pmBillingAddress.address1}<br />
                  <#if pmBillingAddress.address2?has_content>${pmBillingAddress.address2}<br /></#if>
                  ${pmBillingAddress.city}<#if pmBillingAddress.stateProvinceGeoId?has_content>, ${pmBillingAddress.stateProvinceGeoId} </#if>
                  ${pmBillingAddress.postalCode!}<br />
                  ${pmBillingAddress.countryGeoId!}
                </div>
              </td>
              <td>&nbsp;</td>
            </tr>
            <#if paymentList?has_content>
            <tr>
            <td>
              <div>&nbsp;<span class="label">${uiLabelMap.AccountingInvoicePayments}</span></div>
            </td>
              <td>
                <div>
                    <#list paymentList as paymentMap>
                        <a href="/accounting/control/paymentOverview?paymentId=${paymentMap.paymentId}${StringUtil.wrapString(externalKeyParam)}" class="buttontext">${paymentMap.paymentId}</a><#if paymentMap_has_next><br /></#if>
                    </#list>
                </div>
              </td>
            </tr>
            </#if>
          </#if>
        </#list>

        <#if customerPoNumber?has_content>
          <tr><td colspan="4"><hr /></td></tr>
          <tr>
            <td><span class="label">${uiLabelMap.OrderPONumber}</span></td>
            <td>${customerPoNumber!}</td>
            <td>&nbsp;</td>
          </tr>
        </#if>

        <#-- invoices -->
        <#if invoices?has_content>
          <tr><td colspan="4"><hr /></td></tr>
          <tr>
            <td>&nbsp;<span class="label">${uiLabelMap.OrderInvoices}</span></td>
            <td>
              <#list invoices as invoice>
                <div>${uiLabelMap.CommonNbr}<a href="/accounting/control/invoiceOverview?invoiceId=${invoice}${StringUtil.wrapString(externalKeyParam)}" class="buttontext">${invoice}</a>
                (<a target="_BLANK" href="/accounting/control/invoice.pdf?invoiceId=${invoice}${StringUtil.wrapString(externalKeyParam)}" class="buttontext">${uiLabelMap.CommonPdf}</a>)</div>
              </#list>
            </td>
            <td>&nbsp;</td>
          </tr>
        </#if>
   <#else>
    <tr>
     <td colspan="4" align="center">${uiLabelMap.OrderNoOrderPaymentPreferences}</td>
    </tr>
   </#if>
   <#if ("ORDER_COMPLETED" != orderHeader.statusId) && "ORDER_REJECTED" != orderHeader.statusId && "ORDER_CANCELLED" != orderHeader.statusId && (paymentMethodValueMaps?has_content)>
   <tr><td colspan="4"><hr /></td></tr>
   <tr><td colspan="4">
   <form name="addPaymentMethodToOrder" method="post" action="<@ofbizUrl>addPaymentMethodToOrder</@ofbizUrl>">
   <input type="hidden" name="orderId" value="${orderId!}"/>
   <table class="basic-table form-table" cellspacing='0'>
   <tr>
      <td class="label"><span class="label">${uiLabelMap.AccountingPaymentMethod}</span></td>
      <td>
         <select name="paymentMethodId">
           <#list paymentMethodValueMaps as paymentMethodValueMap>
             <#assign paymentMethod = paymentMethodValueMap.paymentMethod/>
             <option value="${paymentMethod.get("paymentMethodId")!}">
               <#if "CREDIT_CARD" == paymentMethod.paymentMethodTypeId>
                 <#assign creditCard = paymentMethodValueMap.creditCard/>
                 <#if (creditCard?has_content)>
                   <#if security.hasEntityPermission("PAY_INFO", "_VIEW", session) || security.hasEntityPermission("ACCOUNTING", "_VIEW", session)>
                     ${creditCard.cardType!} <@maskSensitiveNumber cardNumber=creditCard.cardNumber!/> ${creditCard.expireDate!}
                   <#else>
                     ${Static["org.apache.ofbiz.party.contact.ContactHelper"].formatCreditCard(creditCard)}
                   </#if>
                 </#if>
               <#else>
                 ${paymentMethod.paymentMethodTypeId!}
                 <#if paymentMethod.description??>${paymentMethod.description}</#if>
                   (${paymentMethod.paymentMethodId})
                 </#if>
               </option>
           </#list>
         </select>
      </td>
      <td>&nbsp;</td>
   </tr>
   <#assign openAmount = orderReadHelper.getOrderOpenAmount()>
   <tr>
      <td class="label"><span class="label">${uiLabelMap.AccountingAmount}</span></td>
      <td>
         <input type="text" name="maxAmount" value="${openAmount}"/>
      </td>
      <td>&nbsp;</td>
   </tr>
   <tr>
      <td>&nbsp;</td>
      <td>
        <input type="submit" value="${uiLabelMap.CommonAdd}" class="smallSubmit"/>
      </td>
      <td>&nbsp;</td>
   </tr>
   </table>
   </form>
   </td></tr>
</#if>
</#if>
</table>
</div>
</div>
