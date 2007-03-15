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

<div class="screenlet">
  <div class="screenlet-header">
      <div class="boxhead">&nbsp;${uiLabelMap.AccountingPaymentInformation}</div>
  </div>
  <div class="screenlet-body">
     <table width="100%" border="0" cellpadding="1" cellspacing="0">
     <#if orderPaymentPreferences?has_content || billingAccount?has_content || invoices?has_content>
        <#list orderPaymentPreferences as orderPaymentPreference>
          <#assign pmBillingAddress = {}>
          <#assign oppStatusItem = orderPaymentPreference.getRelatedOne("StatusItem")>
          <#if outputted?default("false") == "true">
            <tr><td colspan="7"><hr class="sepbar"/></td></tr>
          </#if>
          <#assign outputted = "true">
          <#-- try the paymentMethod first; if paymentMethodId is specified it overrides paymentMethodTypeId -->
          <#assign paymentMethod = orderPaymentPreference.getRelatedOne("PaymentMethod")?if_exists>
          <#if !paymentMethod?has_content>
            <#assign paymentMethodType = orderPaymentPreference.getRelatedOne("PaymentMethodType")>
            <#if paymentMethodType.paymentMethodTypeId == "EXT_BILLACT">
              <#assign outputted = "false">
            <#else>
              <tr>
                <td align="right" valign="top" width="15%">
                  <div class="tabletext">&nbsp;<b>${paymentMethodType.get("description",locale)?if_exists}</b>
                  <#if orderPaymentPreference.maxAmount?has_content>
                  <br/>${uiLabelMap.OrderPaymentMaximumAmount}: <@ofbizCurrency amount=orderPaymentPreference.maxAmount?default(0.00) isoCode=currencyUomId/>
                  </#if>
                  </div>
                </td>
                <td width="5">&nbsp;</td>
                <#if paymentMethodType.paymentMethodTypeId != "EXT_OFFLINE" && paymentMethodType.paymentMethodTypeId != "EXT_PAYPAL" && paymentMethodType.paymentMethodTypeId != "EXT_COD">
                  <td align="left">
                    <div class="tabletext">
                      <#if orderPaymentPreference.maxAmount?has_content>
                         <br/>${uiLabelMap.OrderPaymentMaximumAmount}: <@ofbizCurrency amount=orderPaymentPreference.maxAmount?default(0.00) isoCode=currencyUomId/>
                      </#if>
                      <br/>&nbsp;[<#if oppStatusItem?exists>${oppStatusItem.get("description",locale)}<#else>${orderPaymentPreference.statusId}</#if>]
                    </div>
                    <#--
                    <div class="tabletext"><@ofbizCurrency amount=orderPaymentPreference.maxAmount?default(0.00) isoCode=currencyUomId/>&nbsp;-&nbsp;${(orderPaymentPreference.authDate.toString())?if_exists}</div>
                    <div class="tabletext">&nbsp;<#if orderPaymentPreference.authRefNum?exists>(${uiLabelMap.OrderReference}: ${orderPaymentPreference.authRefNum})</#if></div>
                    -->
                 </td>
                <#else>
                  <td align="right">
                    <a href="<@ofbizUrl>receivepayment?${paramString}</@ofbizUrl>" class="buttontext">${uiLabelMap.AccountingReceivePayment}</a>
                  </td>
                </#if>
                <td>
                   <#if (!orderHeader.statusId.equals("ORDER_COMPLETED")) && !(orderHeader.statusId.equals("ORDER_REJECTED")) && !(orderHeader.statusId.equals("ORDER_CANCELLED"))>
                   <#if orderPaymentPreference.statusId != "PAYMENT_SETTLED">                        
                      <a href="<@ofbizUrl>updateOrderPaymentPreference?orderId=${orderId}&amp;orderPaymentPreferenceId=${orderPaymentPreference.orderPaymentPreferenceId}&amp;statusId=PAYMENT_CANCELLED&amp;checkOutPaymentId=${paymentMethod.paymentMethodTypeId?if_exists}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonCancel}</a>&nbsp;
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
                <td align="right" valign="top" width="15%">
                  <div class="tabletext">&nbsp;<b>${uiLabelMap.AccountingCreditCard}</b>
                  <#if orderPaymentPreference.maxAmount?has_content>
                     <br/>${uiLabelMap.OrderPaymentMaximumAmount}: <@ofbizCurrency amount=orderPaymentPreference.maxAmount?default(0.00) isoCode=currencyUomId/>
                  </#if>
                  </div>
                </td>
                <td width="5">&nbsp;</td>
                <td align="left" valign="top" width="80%">
                  <div class="tabletext">
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
                        ${creditCard.cardNumber}
                        ${creditCard.expireDate}
                        &nbsp;[<#if oppStatusItem?exists>${oppStatusItem.get("description",locale)}<#else>${orderPaymentPreference.statusId}</#if>]
                      <#else>
                        ${Static["org.ofbiz.party.contact.ContactHelper"].formatCreditCard(creditCard)}
                        &nbsp;[<#if oppStatusItem?exists>${oppStatusItem.get("description",locale)}<#else>${orderPaymentPreference.statusId}</#if>]
                      </#if>
                      <br/>

                      <#-- Authorize and Capture transactions -->
                                  <div class="tabletext">
                        <#if orderPaymentPreference.statusId != "PAYMENT_SETTLED">
                          <a href="/accounting/control/AuthorizeTransaction?orderId=${orderId?if_exists}&orderPaymentPreferenceId=${orderPaymentPreference.orderPaymentPreferenceId}&externalLoginKey=${externalLoginKey}" class="buttontext">${uiLabelMap.AccountingAuthorize}</a>
                        </#if>
                        <#if orderPaymentPreference.statusId == "PAYMENT_AUTHORIZED">
                          <a href="/accounting/control/CaptureTransaction?orderId=${orderId?if_exists}&orderPaymentPreferenceId=${orderPaymentPreference.orderPaymentPreferenceId}&externalLoginKey=${externalLoginKey}" class="buttontext">${uiLabelMap.AccountingCapture}</a>
                        </#if>
                      </div>

                    <#else>
                      ${uiLabelMap.CommonInformation} ${uiLabelMap.CommonNot} ${uiLabelMap.CommonAvailable}
                    </#if>
                  </div>
                  <#if gatewayResponses?has_content>
                    <div class="tabletext">
                      <hr class="sepbar"/>
                      <#list gatewayResponses as gatewayResponse>
                        <#assign transactionCode = gatewayResponse.getRelatedOne("TranCodeEnumeration")>
                        ${(transactionCode.get("description",locale))?default("Unknown")}:
                        ${gatewayResponse.transactionDate.toString()}
                        <@ofbizCurrency amount=gatewayResponse.amount isoCode=currencyUomId/><br/>
                        (<b>${uiLabelMap.OrderReference}:</b> ${gatewayResponse.referenceNum?if_exists}
                        <b>${uiLabelMap.OrderAvs}:</b> ${gatewayResponse.gatewayAvsResult?default("N/A")}
                        <b>${uiLabelMap.OrderScore}:</b> ${gatewayResponse.gatewayScoreResult?default("N/A")})
                        <a href="/accounting/control/ViewGatewayResponse?paymentGatewayResponseId=${gatewayResponse.paymentGatewayResponseId}&externalLoginKey=${externalLoginKey}" class="buttontext">${uiLabelMap.CommonDetails}</a>
                        <#if gatewayResponse_has_next><hr class="sepbar"/></#if>
                      </#list>
                    </div>
                  </#if>
                </td>
                <td>
                   <#if (!orderHeader.statusId.equals("ORDER_COMPLETED")) && !(orderHeader.statusId.equals("ORDER_REJECTED")) && !(orderHeader.statusId.equals("ORDER_CANCELLED"))>
                   <#if orderPaymentPreference.statusId != "PAYMENT_SETTLED">                        
                      <a href="<@ofbizUrl>updateOrderPaymentPreference?orderId=${orderId}&orderPaymentPreferenceId=${orderPaymentPreference.orderPaymentPreferenceId}&statusId=PAYMENT_CANCELLED&checkOutPaymentId=${paymentMethod.paymentMethodTypeId?if_exists}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonCancel}</a>&nbsp;
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
                <td align="right" valign="top" width="15%">
                  <div class="tabletext">&nbsp;<b>${uiLabelMap.AccountingEFTAccount}</b>
                  <#if orderPaymentPreference.maxAmount?has_content>
                  <br/>${uiLabelMap.OrderPaymentMaximumAmount}: <@ofbizCurrency amount=orderPaymentPreference.maxAmount?default(0.00) isoCode=currencyUomId/>
                  </#if>
                  </div>
                </td>
                <td width="5">&nbsp;</td>
                <td align="left" valign="top" width="80%">
                  <div class="tabletext">
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
                <td>
                   <#if (!orderHeader.statusId.equals("ORDER_COMPLETED")) && !(orderHeader.statusId.equals("ORDER_REJECTED")) && !(orderHeader.statusId.equals("ORDER_CANCELLED"))>
                   <#if orderPaymentPreference.statusId != "PAYMENT_SETTLED">                        
                      <a href="<@ofbizUrl>updateOrderPaymentPreference?orderId=${orderId}&orderPaymentPreferenceId=${orderPaymentPreference.orderPaymentPreferenceId}&statusId=PAYMENT_CANCELLED&checkOutPaymentId=${paymentMethod.paymentMethodTypeId?if_exists}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonCancel}</a>&nbsp;
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
                <td align="right" valign="top" width="15%">
                  <div class="tabletext">&nbsp;<b>${uiLabelMap.OrderGiftCard}</b>
                  <#if orderPaymentPreference.maxAmount?has_content>
                  <br/>${uiLabelMap.OrderPaymentMaximumAmount}: <@ofbizCurrency amount=orderPaymentPreference.maxAmount?default(0.00) isoCode=currencyUomId/>
                  </#if>
                  </div>
                </td>
                <td width="5">&nbsp;</td>
                <td align="left" valign="top" width="80%">
                  <div class="tabletext">
                    <#if giftCard?has_content>
                      <#if security.hasEntityPermission("PAY_INFO", "_VIEW", session)>
                        ${giftCard.cardNumber?default("N/A")} [${giftCard.pinNumber?default("N/A")}]
                        &nbsp;[<#if oppStatusItem?exists>${oppStatusItem.get("description",locale)}<#else>${orderPaymentPreference.statusId}</#if>]
                      <#else>
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
                        ${giftCardNumber?default("N/A")}
                        &nbsp;[<#if oppStatusItem?exists>${oppStatusItem.get("description",locale)}<#else>${orderPaymentPreference.statusId}</#if>]
                      </#if>
                    <#else>
                      ${uiLabelMap.CommonInformation} ${uiLabelMap.CommonNot} ${uiLabelMap.CommonAvailable}
                    </#if>
                  </div>
                </td>
                <td>
                   <#if (!orderHeader.statusId.equals("ORDER_COMPLETED")) && !(orderHeader.statusId.equals("ORDER_REJECTED")) && !(orderHeader.statusId.equals("ORDER_CANCELLED"))>
                   <#if orderPaymentPreference.statusId != "PAYMENT_SETTLED">                        
                      <a href="<@ofbizUrl>updateOrderPaymentPreference?orderId=${orderId}&orderPaymentPreferenceId=${orderPaymentPreference.orderPaymentPreferenceId}&statusId=PAYMENT_CANCELLED&checkOutPaymentId=${paymentMethod.paymentMethodTypeId?if_exists}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonCancel}</a>&nbsp;
                   </#if>
                   </#if>
                </td>
              </tr>
            </#if>
          </#if>
          <#if pmBillingAddress?has_content>
            <tr><td>&nbsp;</td><td>&nbsp;</td><td colspan="5"><hr class="sepbar"/></td></tr>
            <tr>
              <td align="right" valign="top" width="15%">
                <div class="tabletext">&nbsp;</div>
              </td>
              <td width="5">&nbsp;</td>
              <td align="left" valign="top" width="80%">
                <div class="tabletext">
                  <#if pmBillingAddress.toName?has_content><b>${uiLabelMap.CommonTo}:</b> ${pmBillingAddress.toName}<br/></#if>
                  <#if pmBillingAddress.attnName?has_content><b>${uiLabelMap.CommonAttn}:</b> ${pmBillingAddress.attnName}<br/></#if>
                  ${pmBillingAddress.address1}<br/>
                  <#if pmBillingAddress.address2?has_content>${pmBillingAddress.address2}<br/></#if>
                  ${pmBillingAddress.city}<#if pmBillingAddress.stateProvinceGeoId?has_content>, ${pmBillingAddress.stateProvinceGeoId} </#if>
                  ${pmBillingAddress.postalCode?if_exists}<br/>
                  ${pmBillingAddress.countryGeoId?if_exists}
                </div>
              </td>
            </tr>
          </#if>
        </#list>

        <#-- billing account -->
        <#if billingAccount?exists>
          <#if outputted?default("false") == "true">
            <tr><td colspan="7"><hr class="sepbar"/></td></tr>
          </#if>
          <tr>
            <td align="right" valign="top" width="15%">
              <#-- billing accounts require a special OrderPaymentPreference because it is skipped from above section of OPPs -->
              <div class="tabletext">&nbsp;<b>${uiLabelMap.AccountingBillingAccount}</b>
                  <#if billingAccountMaxAmount?has_content>
                  <br/>${uiLabelMap.OrderPaymentMaximumAmount}: <@ofbizCurrency amount=billingAccountMaxAmount?default(0.00) isoCode=currencyUomId/>
                  </#if>
                  </div>
            </td>
            <td width="5">&nbsp;</td>
            <td align="left" valign="top" width="80%">
              <div class="tabletext">
                #<a href="/accounting/control/EditBillingAccount?billingAccountId=${billingAccount.billingAccountId}&amp;externalLoginKey=${externalLoginKey}" class="buttontext">${billingAccount.billingAccountId}</a>  - ${billingAccount.description?if_exists}
              </div>
            </td>
          </tr>
          <tr><td colspan="7"><hr class="sepbar"/></td></tr>
          <tr>
            <td align="right" valign="top" width="15%">
              <div class="tabletext">&nbsp;<b>${uiLabelMap.OrderPurchaseOrderNumber}</b></div>
            </td>
            <td width="5">&nbsp;</td>
            <td align="left" valign="top" width="80%">
              <div class="tabletext">${customerPoNumber?if_exists}</div>
            </td>
          </tr>
        </#if>

        <#-- invoices -->
        <#if invoices?has_content>
          <tr><td colspan="7"><hr class="sepbar"/></td></tr>
          <tr>
            <td align="right" valign="top" width="15%">
              <div class="tabletext">&nbsp;<b>${uiLabelMap.OrderInvoices}</b></div>
            </td>
            <td width="5">&nbsp;</td>
            <td align="left" valign="top" width="80%">
              <#list invoices as invoice>
                <div class="tabletext">${uiLabelMap.OrderNbr}<a href="/accounting/control/invoiceOverview?invoiceId=${invoice}&externalLoginKey=${externalLoginKey}" class="buttontext">${invoice}</a>
                (<a href="/accounting/control/invoice.pdf?invoiceId=${invoice}" class="buttontext">PDF</a>)</div>
              </#list>
            </td>
          </tr>
        </#if>
   <#else>
    <tr>
     <td colspan="7" align="center" class="tabletext">${uiLabelMap.OrderNoOrderPaymentPreferences}</td>
    </tr>
   </#if>
   <#if (!orderHeader.statusId.equals("ORDER_COMPLETED")) && !(orderHeader.statusId.equals("ORDER_REJECTED")) && !(orderHeader.statusId.equals("ORDER_CANCELLED")) && (paymentMethodValueMaps?has_content)>
   <tr><td colspan="7"><hr class="sepbar"/></td></tr>                      
   <form name="addPaymentMethodToOrder" method="post" action="<@ofbizUrl>addPaymentMethodToOrder</@ofbizUrl>">           
   <input type="hidden" name="orderId" value="${orderId?if_exists}"/>
   <tr>
      <td width="15%" align="right" nowrap><div class="tableheadtext">${uiLabelMap.AccountingPaymentMethod} </div></td>
      <td width="5">&nbsp;</td>
      <td nowrap>
         <select name="paymentMethodId" class="selectBox">
         <#list paymentMethodValueMaps as paymentMethodValueMap>
         <#assign paymentMethod = paymentMethodValueMap.paymentMethod/>
         <option value="${paymentMethod.get("paymentMethodId")?if_exists}">
         <#if "CREDIT_CARD" == paymentMethod.paymentMethodTypeId>
            <#assign creditCard = paymentMethodValueMap.creditCard/>
            ${creditCard.cardType?if_exists} ${creditCard.cardNumber?if_exists} ${creditCard.expireDate?if_exists}                    
         <#else>
            ${paymentMethod.paymentMethodTypeId?if_exists} 
            <#if paymentMethod.description?exists>${paymentMethod.description}</#if>
            (${paymentMethod.paymentMethodId})
         </#if> 
         </option>
         </#list>
         </select>
      </td>
   </tr>                    
   <#assign openAmount = orderReadHelper.getOrderOpenAmount()>
   <tr>
      <td width="20%" align="right"><div class="tableheadtext">${uiLabelMap.AccountingAmount} </div></td>
      <td width="2%">&nbsp;</td>
      <td nowrap>
         <input type="text" class="inputBox" name="maxAmount" value="${openAmount}"/>
      </td>
   </tr>
   <tr>
      <td align="right" valign="top" width="15%">
         <div class="tabletext">&nbsp;</div>
      </td>
      <td width="5">&nbsp;</td>
      <td align="left" valign="top" width="80%">
         <div class="tabletext">
            <input type="submit" value="${uiLabelMap.CommonAdd}" class="smallSubmit"/>
         </div>
      </td>
   </tr>
</form>     
</#if>
</table>
</div>
</div>
