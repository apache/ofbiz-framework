<#--

Copyright 2001-2006 The Apache Software Foundation

Licensed under the Apache License, Version 2.0 (the "License"); you may not
use this file except in compliance with the License. You may obtain a copy of
the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
License for the specific language governing permissions and limitations
under the License.
-->

<#if orderHeader?has_content>

<table width="100%" border="0" cellpadding="0" cellspacing="0">
  <tr>
    <td width="50%" valign="top" align="left">
      <#-- header box -->

        <div class="screenlet">
            <div class="screenlet-header">
                <div style="float: right;">
                    <#if currentStatus.statusId == "ORDER_CREATED" || currentStatus.statusId == "ORDER_PROCESSING">
                        <div class="tabletext"><a href="<@ofbizUrl>changeOrderItemStatus?statusId=ITEM_APPROVED&${paramString}</@ofbizUrl>" class="submenutextright">${uiLabelMap.OrderApproveOrder}</a></div>
                    </#if>
                    <#if setOrderCompleteOption>
                    	  <div class="tabletext"><a href="<@ofbizUrl>changeOrderStatus?orderId=${orderId}&statusId=ORDER_COMPLETED</@ofbizUrl>" class="submenutextright">${uiLabelMap.OrderCompleteOrder}</a></div>
                    </#if>
                </div>
                <#if orderHeader.externalId?has_content>
                  <#assign externalOrder = "(" + orderHeader.externalId + ")"/>
                </#if>
                <div class="boxhead">&nbsp;${uiLabelMap.OrderOrder} #${orderId} ${externalOrder?if_exists} ${uiLabelMap.CommonInformation} [<a href="<@ofbizUrl>order.pdf?orderId=${orderId}</@ofbizUrl>" class="submenutextright" target="_blank">PDF</a> ]</div>
            </div>
            <div class="screenlet-body">
                  <table width="100%" border="0" cellpadding="1" cellspacing="0">
                    <#-- order name -->
                    <#if orderHeader.orderName?has_content>
                    <tr>
                      <td align="right" valign="top" width="15%">
                        <div class="tabletext">&nbsp;<b>${uiLabelMap.OrderOrderName}</b></div>
                      </td>
                      <td width="5">&nbsp;</td>
                      <td align="left" valign="top" width="80%">
                        <div class="tabletext">${orderHeader.orderName}</div> 
                      </td>  
                    </tr>    
                    <tr><td colspan="7"><hr class="sepbar"></td></tr>
                    </#if>   
                    <#-- order status history -->
                    <tr>
                      <td align="right" valign="top" width="15%">
                        <div class="tabletext">&nbsp;<b>${uiLabelMap.OrderStatusHistory}</b></div>
                      </td>
                      <td width="5">&nbsp;</td>
                      <td align="left" valign="top" width="80%">
                        <div class="tabletext">${uiLabelMap.OrderCurrentStatus}: ${currentStatus.get("description",locale)}</div>
                        <#if orderHeaderStatuses?has_content>
                          <hr class="sepbar">
                          <#list orderHeaderStatuses as orderHeaderStatus>
                            <#assign loopStatusItem = orderHeaderStatus.getRelatedOne("StatusItem")>
                            <div class="tabletext">
                              ${loopStatusItem.get("description",locale)} - ${orderHeaderStatus.statusDatetime?default("0000-00-00 00:00:00")?string}
                            </div>
                          </#list>
                        </#if>
                      </td>
                    </tr>
                    <tr><td colspan="7"><hr class="sepbar"></td></tr>
                    <tr>
                      <td align="right" valign="top" width="15%">
                        <div class="tabletext">&nbsp;<b>${uiLabelMap.OrderDateOrdered}</b></div>
                      </td>
                      <td width="5">&nbsp;</td>
                      <td align="left" valign="top" width="80%">
                        <div class="tabletext">
                          ${orderHeader.orderDate.toString()}
                        </div>
                      </td>
                    </tr>
                    <tr><td colspan="7"><hr class="sepbar"></td></tr>
                    <tr>
                      <td align="right" valign="top" width="15%">
                        <div class="tabletext">&nbsp;<b>${uiLabelMap.CommonCurrency}</b></div>
                      </td>
                      <td width="5">&nbsp;</td>
                      <td align="left" valign="top" width="80%">
                        <div class="tabletext">
                          ${orderHeader.currencyUom?default("???")}
                        </div>
                      </td>
                    </tr>
                    <#if orderHeader.internalCode?has_content>
                    <tr><td colspan="7"><hr class="sepbar"></td></tr>
                    <tr>
                      <td align="right" valign="top" width="15%">
                        <div class="tabletext">&nbsp;<b>${uiLabelMap.OrderInternalCode}</b></div>
                      </td>
                      <td width="5">&nbsp;</td>
                      <td align="left" valign="top" width="80%">
                        <div class="tabletext">
                          ${orderHeader.internalCode}
                        </div>
                      </td>
                    </tr>
                    </#if>
                    <tr><td colspan="7"><hr class="sepbar"></td></tr>
                    <tr>
                      <td align="right" valign="top" width="15%">
                        <div class="tabletext">&nbsp;<b>${uiLabelMap.OrderSalesChannel}</b></div>
                      </td>
                      <td width="5">&nbsp;</td>
                      <td align="left" valign="top" width="80%">
                        <div class="tabletext">
                          <#if orderHeader.salesChannelEnumId?has_content>
                            <#assign channel = orderHeader.getRelatedOne("SalesChannelEnumeration")>
                            ${(channel.get("description",locale))?default("N/A")}
                          <#else>
                            ${uiLabelMap.CommonNA}
                          </#if>
                        </div>
                      </td>
                    </tr>
                    <tr><td colspan="7"><hr class="sepbar"></td></tr>
                    <tr>
                      <td align="right" valign="top" width="15%">
                        <div class="tabletext">&nbsp;<b>${uiLabelMap.OrderProductStore}</b></div>
                      </td>
                      <td width="5">&nbsp;</td>
                      <td align="left" valign="top" width="80%">
                        <div class="tabletext">
                          <#if orderHeader.productStoreId?has_content>
                            <a href="/catalog/control/EditProductStore?productStoreId=${orderHeader.productStoreId}" target="catalogmgr" class="buttontext">${orderHeader.productStoreId}</a>
                          <#else>
                            ${uiLabelMap.CommonNA}
                          </#if>
                        </div>
                      </td>
                    </tr>
                    <tr><td colspan="7"><hr class="sepbar"></td></tr>
                    <tr>
                      <td align="right" valign="top" width="15%">
                        <div class="tabletext">&nbsp;<b>${uiLabelMap.OrderOriginFacility}</b></div>
                      </td>
                      <td width="5">&nbsp;</td>
                      <td align="left" valign="top" width="80%">
                        <div class="tabletext">
                          <#if orderHeader.originFacilityId?has_content>
                            <a href="/facility/control/EditFacility?facilityId=${orderHeader.originFacilityId}${externalKeyParam}" target="facilitymgr" class="buttontext">${orderHeader.originFacilityId}</a>
                          <#else>
                            ${uiLabelMap.CommonNA}
                          </#if>
                        </div>
                      </td>
                    </tr>
                    <tr><td colspan="7"><hr class="sepbar"></td></tr>
                    <tr>
                      <td align="right" valign="top" width="15%">
                        <div class="tabletext">&nbsp;<b>${uiLabelMap.OrderCreatedBy}</b></div>
                      </td>
                      <td width="5">&nbsp;</td>
                      <td align="left" valign="top" width="80%">
                        <div class="tabletext">
                          <#if orderHeader.createdBy?has_content>
                            <a href="/partymgr/control/viewprofile?userlogin_id=${orderHeader.createdBy}" target="partymgr" class="buttontext">${orderHeader.createdBy}</a>
                          <#else>
                            [${uiLabelMap.CommonNotSet}]
                          </#if>
                        </div>
                      </td>
                    </tr>

                    <#if distributorId?exists>
                    <tr><td colspan="7"><hr class="sepbar"></td></tr>
                    <tr>
                      <td align="right" valign="top" width="15%">
                        <div class="tabletext">&nbsp;<b>${uiLabelMap.OrderDistributor}</b></div>
                      </td>
                      <td width="5">&nbsp;</td>
                      <td align="left" valign="top" width="80%">
                        <div class="tabletext">
                          <#assign distPartyNameResult = dispatcher.runSync("getPartyNameForDate", Static["org.ofbiz.base.util.UtilMisc"].toMap("partyId", distributorId, "compareDate", orderHeader.orderDate, "userLogin", userLogin))/>
                          ${distPartyNameResult.fullName?default("[${uiLabelMap.OrderPartyNameNotFound}]")}
                        </div>
                      </td>
                    </tr>
                    </#if>
                    <#if affiliateId?exists>
                    <tr><td colspan="7"><hr class="sepbar"></td></tr>
                    <tr>
                      <td align="right" valign="top" width="15%">
                        <div class="tabletext">&nbsp;<b>${uiLabelMap.OrderAffiliate}</b></div>
                      </td>
                      <td width="5">&nbsp;</td>
                      <td align="left" valign="top" width="80%">
                        <div class="tabletext">
                          <#assign affPartyNameResult = dispatcher.runSync("getPartyNameForDate", Static["org.ofbiz.base.util.UtilMisc"].toMap("partyId", affiliateId, "compareDate", orderHeader.orderDate, "userLogin", userLogin))/>
                          ${affPartyNameResult.fullName?default("[${uiLabelMap.OrderPartyNameNotFound}]")}
                        </div>
                      </td>
                    </tr>
                    </#if>

                    <#if orderContentWrapper.get("IMAGE_URL")?has_content>
                    <tr><td colspan="7"><hr class="sepbar"></td></tr>
                    <tr>
                      <td align="right" valign="top" width="15%">
                        <div class="tabletext">&nbsp;<b>${uiLabelMap.OrderImage}</b></div>
                      </td>
                      <td width="5">&nbsp;</td>
                      <td align="left" valign="top" width="80%">
                        <div class="tabletext">
                          <a href="<@ofbizUrl>viewimage?orderId=${orderId}&orderContentTypeId=IMAGE_URL</@ofbizUrl>" target="_orderImage" class="buttontext">${uiLabelMap.OrderViewImage}</a>
                        </div>
                      </td>
                    </tr>
                    </#if>
                  </table>
            </div>
        </div>
      <#-- end of header box -->
      <#-- box for order terms -->
      <#if orderTerms?has_content>
        <div class="screenlet">
            <div class="screenlet-header">
                <div class="boxhead">&nbsp;${uiLabelMap.OrderOrderTerms}</div>
            </div>
            <div class="screenlet-body">
             <table border="0" width="100%" cellspacing="0" cellpadding="0">
              <tr>
                <td width="60%" align="left"><div class="tabletext"><b>${uiLabelMap.OrderOrderTermType}</b></div></td>
                <td width="20%" align="center"><div class="tabletext"><b>${uiLabelMap.OrderOrderTermValue}</b></div></td>
                <td width="20%" align="center"><div class="tabletext"><b>${uiLabelMap.OrderOrderTermDays}</b></div></td>
              </tr>
              <tr><td colspan="3"><hr class='sepbar'></td></tr>
              <#list orderTerms as orderTerm>
                  <tr>
                    <td width="60%" align="left"><div class="tabletext">${orderTerm.getRelatedOne("TermType").get("description", locale)}</div></td>
                    <td width="20%" align="center"><div class="tabletext">${orderTerm.termValue?default("")}</div></td>
                    <td width="20%" align="center"><div class="tabletext">${orderTerm.termDays?default("")}</div></td>
                  </tr>
                  <tr><td colspan="3">&nbsp;</td></tr>
              </#list>
             </table>
            </div>
        </div>
      </#if>
      <#-- end of order terms box -->

      <#-- payment box -->
      <#if orderPaymentPreferences?has_content || billingAccount?has_content || invoices?has_content>
        <div class="screenlet">
            <div class="screenlet-header">
                <div class="boxhead">&nbsp;${uiLabelMap.AccountingPaymentInformation}</div>
            </div>
            <div class="screenlet-body">
                <table width="100%" border="0" cellpadding="1" cellspacing="0">
                <#list orderPaymentPreferences as orderPaymentPreference>
                  <#assign oppStatusItem = orderPaymentPreference.getRelatedOne("StatusItem")>
                  <#if outputted?default("false") == "true">
                    <tr><td colspan="7"><hr class="sepbar"></td></tr>
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
                        <#if paymentMethodType.paymentMethodTypeId != "EXT_OFFLINE" && paymentMethodType.paymentMethodTypeId != "EXT_PAYPAL">
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
                            <a valign="top" href="<@ofbizUrl>receivepayment?${paramString}</@ofbizUrl>" class="buttontext">${uiLabelMap.AccountingReceivePayment}</a>
                          </td>
                        </#if>
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
                              <hr />
                              <#list gatewayResponses as gatewayResponse>
                                <#assign transactionCode = gatewayResponse.getRelatedOne("TranCodeEnumeration")>
                                ${(transactionCode.get("description",locale))?default("Unknown")}:
                                ${gatewayResponse.transactionDate.toString()}
                                <@ofbizCurrency amount=gatewayResponse.amount isoCode=currencyUomId/><br/>
                                (<b>${uiLabelMap.OrderReference}:</b> ${gatewayResponse.referenceNum?if_exists}
                                <b>${uiLabelMap.OrderAvs}:</b> ${gatewayResponse.gatewayAvsResult?default("N/A")}
                                <b>${uiLabelMap.OrderScore}:</b> ${gatewayResponse.gatewayScoreResult?default("N/A")})
                                [<a href="/accounting/control/ViewGatewayResponse?paymentGatewayResponseId=${gatewayResponse.paymentGatewayResponseId}">${uiLabelMap.CommonDetails}</a>]
                                <#if gatewayResponse_has_next><hr /></#if>
                              </#list>
                            </div>
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
                      </tr>
                    </#if>
                  </#if>
                  <#if pmBillingAddress?has_content>
                    <tr><td>&nbsp;</td><td>&nbsp;</td><td colspan="5"><hr class="sepbar"></td></tr>
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
                    <tr><td colspan="7"><hr class="sepbar"></td></tr>
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
                        #<a href="/accounting/control/EditBillingAccount?billingAccountId=${billingAccount.billingAccountId}" class="buttontext">${billingAccount.billingAccountId}</a>  - ${billingAccount.description?if_exists}
                      </div>
                    </td>
                  </tr>
                  <tr><td colspan="7"><hr class="sepbar"></td></tr>
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
                  <tr><td colspan="7"><hr class="sepbar"></td></tr>
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
                </table>
            </div>
        </div>
      </#if>
      <#-- end of payment box -->
    </td>
    <td width="1">&nbsp;&nbsp;</td>
    <td width="50%" valign="top" align="left">
      <#-- contact box -->
      <#if displayParty?has_content || orderContactMechValueMaps?has_content>
        <div class="screenlet">
            <div class="screenlet-header">
                <div class="boxhead">&nbsp;${uiLabelMap.OrderContactInformation}</div>
            </div>
            <div class="screenlet-body">
              <table width="100%" border="0" cellpadding="1" cellspacing="0">
                <tr>
                  <td align="right" valign="top" width="15%">
                    <div class="tabletext">&nbsp;<b>${uiLabelMap.CommonName}</b></div>
                  </td>
                  <td width="5">&nbsp;</td>
                  <td align="left" valign="top" width="80%">
                    <div class="tabletext">
                      <#if displayParty?has_content>
                        <#assign displayPartyNameResult = dispatcher.runSync("getPartyNameForDate", Static["org.ofbiz.base.util.UtilMisc"].toMap("partyId", displayParty.partyId, "compareDate", orderHeader.orderDate, "userLogin", userLogin))/>
                        ${displayPartyNameResult.fullName?default("[${uiLabelMap.OrderPartyNameNotFound}]")}
                      </#if>
                      <#if partyId?exists>
                        <span>&nbsp;(<a href="${customerDetailLink}${partyId}" target="partymgr" class="buttontext">${partyId}</a>)</span>
                        <span align="right">
                           <a href="<@ofbizUrl>/orderentry?partyId=${partyId}&orderTypeId=${orderHeader.orderTypeId}</@ofbizUrl>" class="buttontext">${uiLabelMap.OrderNewOrder}</a>
                           <a href="<@ofbizUrl>/findorders?lookupFlag=Y&hideFields=Y&partyId=${partyId}</@ofbizUrl>" class="buttontext">${uiLabelMap.OrderOtherOrders}</a>
                        </span>
                      </#if>
                    </div>
                  </td>
                </tr>
                <#list orderContactMechValueMaps as orderContactMechValueMap>
                  <#assign contactMech = orderContactMechValueMap.contactMech>
                  <#assign contactMechPurpose = orderContactMechValueMap.contactMechPurposeType>
                  <#--<#assign partyContactMech = orderContactMechValueMap.partyContactMech>-->
                  <tr><td colspan="7"><hr class="sepbar"></td></tr>
                  <tr>
                    <td align="right" valign="top" width="15%">
                      <div class="tabletext">&nbsp;<b>${contactMechPurpose.get("description",locale)}</b></div>
                    </td>
                    <td width="5">&nbsp;</td>
                    <td align="left" valign="top" width="80%">
                      <#if contactMech.contactMechTypeId == "POSTAL_ADDRESS">
                        <#assign postalAddress = orderContactMechValueMap.postalAddress>
                        <#if postalAddress?has_content>
                          <div class="tabletext">
                            <#if postalAddress.toName?has_content><b>${uiLabelMap.CommonTo}:</b> ${postalAddress.toName}<br/></#if>
                            <#if postalAddress.attnName?has_content><b>${uiLabelMap.CommonAttn}:</b> ${postalAddress.attnName}<br/></#if>
                            ${postalAddress.address1}<br/>
                            <#if postalAddress.address2?has_content>${postalAddress.address2}<br/></#if>
                            ${postalAddress.city}<#if postalAddress.stateProvinceGeoId?has_content>, ${postalAddress.stateProvinceGeoId} </#if>
                            ${postalAddress.postalCode?if_exists}<br/>
                            ${postalAddress.countryGeoId?if_exists}<br/>
                            <#if !postalAddress.countryGeoId?exists || postalAddress.countryGeoId == "USA">
                              <#assign addr1 = postalAddress.address1?if_exists>
                              <#if (addr1.indexOf(" ") > 0)>
                                <#assign addressNum = addr1.substring(0, addr1.indexOf(" "))>
                                <#assign addressOther = addr1.substring(addr1.indexOf(" ")+1)>
                                <a target="_blank" href="http://www.whitepages.com/find_person_results.pl?fid=a&s_n=${addressNum}&s_a=${addressOther}&c=${postalAddress.city?if_exists}&s=${postalAddress.stateProvinceGeoId?if_exists}&x=29&y=18" class="buttontext">(lookup:whitepages.com)</a>
                              </#if>
                            </#if>
                          </div>
                        </#if>
                      <#elseif contactMech.contactMechTypeId == "TELECOM_NUMBER">
                        <#assign telecomNumber = orderContactMechValueMap.telecomNumber>
                        <div class="tabletext">
                          ${telecomNumber.countryCode?if_exists}
                          <#if telecomNumber.areaCode?exists>${telecomNumber.areaCode}-</#if>${telecomNumber.contactNumber}
                          <#--<#if partyContactMech.extension?exists>ext&nbsp;${partyContactMech.extension}</#if>-->
                          <#if !telecomNumber.countryCode?exists || telecomNumber.countryCode == "011" || telecomNumber.countryCode == "1">
                            <a target="_blank" href="http://www.anywho.com/qry/wp_rl?npa=${telecomNumber.areaCode?if_exists}&telephone=${telecomNumber.contactNumber?if_exists}&btnsubmit.x=20&btnsubmit.y=8" class="buttontext">(lookup:anywho.com)</a>
                           <a target="_blank" href="http://whitepages.com/find_person_results.pl?fid=p&ac=${telecomNumber.areaCode}&s=&p=${telecomNumber.contactNumber}&pt=b&x=40&y=9" class="buttontext">(lookup:whitepages.com)</a>
                          </#if>
                        </div>
                      <#elseif contactMech.contactMechTypeId == "EMAIL_ADDRESS">
                        <div class="tabletext">
                          ${contactMech.infoString}
                          <#if security.hasEntityPermission("ORDERMGR", "_SEND_CONFIRMATION", session)>
                             <br/>(<a href="<@ofbizUrl>confirmationmailedit?orderId=${orderId}&partyId=${partyId}&sendTo=${contactMech.infoString}</@ofbizUrl>" class="buttontext">${uiLabelMap.OrderSendConfirmationEmail}</a>)
                          <#else>
                             <a href="mailto:${contactMech.infoString}" class="buttontext">(${uiLabelMap.OrderSendEmail})</a>
                          </#if>
                        </div>
                      <#elseif contactMech.contactMechTypeId == "WEB_ADDRESS">
                        <div class="tabletext">
                          ${contactMech.infoString}
                          <#assign openString = contactMech.infoString>
                          <#if !openString?starts_with("http") && !openString?starts_with("HTTP")>
                            <#assign openString = "http://" + openString>
                          </#if>
                          <a target="_blank" href="${openString}" class="buttontext">(open&nbsp;page&nbsp;in&nbsp;new&nbsp;window)</a>
                        </div>
                      <#else>
                        <div class="tabletext">
                          ${contactMech.infoString?if_exists}
                        </div>
                      </#if>
                    </td>
                  </tr>
                </#list>
              </table>
            </div>
        </div>
      </#if>
      <#-- end of contact box -->
      <#-- shipping info box -->
      <#if shipGroups?has_content>
        <#list shipGroups as shipGroup>
          <#assign shipmentMethodType = shipGroup.getRelatedOne("ShipmentMethodType")?if_exists>
          <#assign shipGroupAddress = shipGroup.getRelatedOne("PostalAddress")?if_exists>
          <div class="screenlet">
            <div class="screenlet-header">
               <div class="boxhead">&nbsp;${uiLabelMap.OrderShipmentInformation} - ${shipGroup.shipGroupSeqId}</div>
            </div>
            <div class="screenlet-body">
              <table width="100%" border="0" cellpadding="1" cellspacing="0">
                 <form name="updateOrderItemShipGroup" method="post" action="<@ofbizUrl>updateOrderItemShipGroup</@ofbizUrl>">
                    <input type="hidden" name="orderId" value="${orderId?if_exists}">
                    <input type="hidden" name="shipGroupSeqId" value="${shipGroup.shipGroupSeqId?if_exists}">
                    <input type="hidden" name="contactMechPurposeTypeId" value="SHIPPING_LOCATION">         
                    <input type="hidden" name="oldContactMechId" value="${shipGroup.contactMechId?if_exists}">         
                <#if shipGroup.contactMechId?has_content>
                  <tr>
                    <td align="right" valign="top" width="15%">
                      <div class="tabletext">&nbsp;<b>${uiLabelMap.OrderAddress}</b></div>
                    </td>
                    <td width="5">&nbsp;</td>
                    <td align="left" valign="top" width="80%">
                      <div class="tabletext">
                      <#if orderHeader?has_content && orderHeader.statusId != "ORDER_CANCELLED" && orderHeader.statusId != "ORDER_COMPLETED" && orderHeader.statusId != "ORDER_REJECTED">            
                         <select name="contactMechId" class="selectBox">
                           <option value="${shipGroup.contactMechId?if_exists}">${(shipGroupAddress.address1)?default("")} - ${shipGroupAddress.city?default("")}</option>
                            <option value="${shipGroup.contactMechId?if_exists}"></option>
                            <#list shippingContactMechList as shippingContactMech>
                            <#assign shippingPostalAddress = shippingContactMech.getRelatedOne("PostalAddress")?if_exists>
                               <#if shippingContactMech.contactMechId?has_content>
                               <option value="${shippingContactMech.contactMechId?if_exists}">${(shippingPostalAddress.address1)?default("")} - ${shippingPostalAddress.city?default("")}</option>
                               </#if>
                            </#list>
                         </select>
                      <#else>
                         ${(shipGroupAddress.address1)?default("")}
                      </#if>   
                      </div>
                    </td>
                  </tr>
                </#if>

                <#if shipGroup.shipmentMethodTypeId?has_content>
                  <tr>
                    <td align="right" valign="top" width="15%">
                      <div class="tabletext">&nbsp;<b>${uiLabelMap.CommonMethod}</b></div>
                    </td>
                    <td width="5">&nbsp;</td>
                    <td align="left" valign="top" width="80%">
                      <#if shipGroup.carrierPartyId?has_content || shipmentMethodType?has_content>
                        <div class="tabletext">
                        <#if orderHeader?has_content && orderHeader.statusId != "ORDER_CANCELLED" && orderHeader.statusId != "ORDER_COMPLETED" && orderHeader.statusId != "ORDER_REJECTED">            
                           <#-- passing the shipmentMethod value as the combination of two fields value 
                                i.e shipmentMethodTypeId & carrierPartyId and this two field values are separated bye
                                "@" symbol.
                            -->
                           <select name="shipmentMethod" class="selectBox">
                             <option value="${shipGroup.shipmentMethodTypeId}@${shipGroup.carrierPartyId?if_exists}"><#if shipGroup.carrierPartyId != "_NA_">${shipGroup.carrierPartyId?if_exists}</#if>&nbsp;${shipmentMethodType.get("description",locale)?default("")}</option>
                             <#list productStoreShipmentMethList as productStoreShipmentMethod>
	                            <#assign shipmentMethodTypeAndParty = productStoreShipmentMethod.shipmentMethodTypeId + "@" + productStoreShipmentMethod.partyId>
	                            <#if productStoreShipmentMethod.partyId?has_content || productStoreShipmentMethod?has_content>
	                               <option value="${shipmentMethodTypeAndParty?if_exists}"><#if productStoreShipmentMethod.partyId != "_NA_">${productStoreShipmentMethod.partyId?if_exists}</#if>&nbsp;${productStoreShipmentMethod.get("description",locale)?default("")}</option>
	                            </#if>
                             </#list>
                           </select>  
                        <#else>  
                           <#if shipGroup.carrierPartyId != "_NA_">
                             ${shipGroup.carrierPartyId?if_exists}
                           </#if>
                           ${shipmentMethodType.get("description",locale)?default("")}
                        </#if>
                        </div>
                      </#if>
                    </td>
                  </tr>
                </#if>
                <#if orderHeader?has_content && orderHeader.statusId != "ORDER_CANCELLED" && orderHeader.statusId != "ORDER_COMPLETED" && orderHeader.statusId != "ORDER_REJECTED">                       
                   <tr>
                      <td align="right" valign="top" width="15%">
                         <div class="tabletext">&nbsp;</div>
                      </td>
                      <td width="5">&nbsp;</td>
                      <td align="left" valign="top" width="80%">
                         <div class="tabletext">
                            <input type="submit" value="${uiLabelMap.CommonUpdate}" class="smallSubmit"/>
                         </div>
                       </td>
                   </tr>
                </#if>
                </form>
                <#if !shipGroup.contactMechId?has_content && !shipGroup.shipmentMethodTypeId?has_content>
                  <#assign noShipment = "true">
                  <tr>
                    <td colspan="3" align="center">
                      <div class="tableheadtext">${uiLabelMap.OrderNotShipped}</div>
                    </td>
                  </tr>
                </#if>

                <#if shipGroup.supplierPartyId?has_content>
                  <#assign supplier =  delegator.findByPrimaryKey("PartyGroup", Static["org.ofbiz.base.util.UtilMisc"].toMap("partyId", shipGroup.supplierPartyId))?if_exists />
                  <tr><td colspan="7"><hr class="sepbar"></td></tr>
                  <tr>
                    <td align="right" valign="top" width="15%">
                      <div class="tabletext">&nbsp;<b>${uiLabelMap.ProductDropShipment} - ${uiLabelMap.PartySupplier}</b></div>
                    </td>
                    <td width="5">&nbsp;</td>
                    <td align="left" valign="top" width="80%">
                      <div class="tabletext"><#if supplier?has_content> - ${supplier.description?default(shipGroup.supplierPartyId)}</#if></div>
                    </td>
                  </tr>
                </#if>

                <#-- tracking number -->
                <#if shipGroup.trackingNumber?has_content || orderShipmentInfoSummaryList?has_content>
                  <tr><td colspan="7"><hr class='sepbar'></td></tr>
                  <tr>
                    <td align="right" valign="top" width="15%">
                      <div class="tabletext">&nbsp;<b>${uiLabelMap.OrderTrackingNumber}</b></div>
                    </td>
                    <td width="5">&nbsp;</td>
                    <td align="left" valign="top" width="80%">
                      <#-- TODO: add links to UPS/FEDEX/etc based on carrier partyId  -->
                      <#if shipGroup.trackingNumber?has_content>
                        <div class="tabletext">${shipGroup.trackingNumber}</div>
                      </#if>
                      <#if orderShipmentInfoSummaryList?has_content>
                        <#list orderShipmentInfoSummaryList as orderShipmentInfoSummary>
                          <#if orderShipmentInfoSummary.shipGroupSeqId?if_exists == shipGroup.shipGroupSeqId?if_exists>
                            <div class="tabletext">
                              <#if (orderShipmentInfoSummaryList?size > 1)>${orderShipmentInfoSummary.shipmentPackageSeqId}: </#if>
                              ${uiLabelMap.CommonIdCode}: ${orderShipmentInfoSummary.trackingCode?default("[${uiLabelMap.OrderNotYetKnown}]")}
                              <#if orderShipmentInfoSummary.boxNumber?has_content> ${uiLabelMap.ProductBox} #${orderShipmentInfoSummary.boxNumber}</#if>
                              <#if orderShipmentInfoSummary.carrierPartyId?has_content>((${uiLabelMap.ProductCarrier}: ${orderShipmentInfoSummary.carrierPartyId})</#if>
                            </div>
                          </#if>
                        </#list>
                      </#if>
                    </td>
                  </tr>
                </#if>
                <#if shipGroup.maySplit?has_content && noShipment?default("false") != "true">
                  <tr><td colspan="7"><hr class="sepbar"></td></tr>
                  <tr>
                    <td align="right" valign="top" width="15%">
                      <div class="tabletext">&nbsp;<b>${uiLabelMap.OrderSplittingPreference}</b></div>
                    </td>
                    <td width="5">&nbsp;</td>
                    <td align="left" valign="top" width="80%">
                      <div class="tabletext">
                        <#if shipGroup.maySplit?upper_case == "N">
                            ${uiLabelMap.FacilityWaitEntireOrderReady}
                            <#if security.hasEntityPermission("ORDERMGR", "_UPDATE", session)>
                              <#if orderHeader.statusId != "ORDER_COMPLETED" && orderHeader.statusId != "ORDER_CANCELLED"><a href="<@ofbizUrl>allowordersplit?shipGroupSeqId=${shipGroup.shipGroupSeqId}&${paramString}</@ofbizUrl>" class="buttontext">${uiLabelMap.OrderAllowSplit}</a></#if>
                            </#if>
                        <#else>
                            ${uiLabelMap.FacilityShipAvailable}
                        </#if>
                      </div>
                    </td>
                  </tr>
                </#if>
                <#if shipGroup.shippingInstructions?has_content>
                  <tr><td colspan="7"><hr class="sepbar"></td></tr>
                  <tr>
                    <td align="right" valign="top" width="15%">
                      <div class="tabletext">&nbsp;<b>${uiLabelMap.CommonInstructions}</b></div>
                    </td>
                    <td width="5">&nbsp;</td>
                    <td align="left" valign="top" width="80%">
                      <div class="tabletext">${shipGroup.shippingInstructions}</div>
                    </td>
                  </tr>
                </#if>
                <#if shipGroup.isGift?has_content && noShipment?default("false") != "true">
                  <tr><td colspan="7"><hr class="sepbar"></td></tr>
                  <tr>
                    <td align="right" valign="top" width="15%">
                      <div class="tabletext">&nbsp;<b>${uiLabelMap.OrderGift}?</b></div>
                    </td>
                    <td width="5">&nbsp;</td>
                    <td align="left" valign="top" width="80%">
                      <div class="tabletext">
                        <#if shipGroup.isGift?upper_case == "N">${uiLabelMap.OrderThisOrderNotGift}<#else>${uiLabelMap.OrderThisOrderGift}</#if>
                      </div>
                    </td>
                  </tr>
                </#if>
                <#if shipGroup.giftMessage?has_content>
                  <tr><td colspan="7"><hr class="sepbar"></td></tr>
                  <tr>
                    <td align="right" valign="top" width="15%">
                      <div class="tabletext">&nbsp;<b>${uiLabelMap.OrderGiftMessage}</b></div>
                    </td>
                    <td width="5">&nbsp;</td>
                    <td align="left" valign="top" width="80%">
                      <div class="tabletext">${shipGroup.giftMessage}</div>
                    </td>
                  </tr>
                </#if>
                 <#if shipGroup.shipAfterDate?has_content>
                 <tr><td colspan="7"><hr class="sepbar"></td></tr>
                 <tr>
                    <td align="right" valign="top" width="15%">
                      <div class="tabletext">&nbsp;<b>${uiLabelMap.OrderShipAfterDate}</b></div>
                    </td>
                    <td width="5">&nbsp;</td>
                    <td align="left" valign="top" width="80%">
                      <div class="tabletext">${shipGroup.shipAfterDate}</div>
                    </td>
                 </tr>
                 </#if>
                <#if shipGroup.shipByDate?has_content>
                <tr><td colspan="7"><hr class="sepbar"></td></tr>
                <tr>
                    <td align="right" valign="top" width="15%">
                      <div class="tabletext">&nbsp;<b>${uiLabelMap.OrderShipBeforeDate}</b></div>
                    </td>
                    <td width="5">&nbsp;</td>
                    <td align="left" valign="top" width="80%">
                      <div class="tabletext">${shipGroup.shipByDate}</div>
                    </td>
                 </tr>
                 </#if>
               <#assign shipGroupShipments = shipGroup.getRelated("PrimaryShipment")>
               <#if shipGroupShipments?has_content>
                  <tr><td colspan="7"><hr class="sepbar"></td></tr>
                  <tr>
                    <td align="right" valign="top" width="15%">
                      <div class="tabletext">&nbsp;<b>${uiLabelMap.FacilityShipments}</b></div>
                    </td>
                    <td width="5">&nbsp;</td>
                    <td align="left" valign="top" width="80%">
                        <#list shipGroupShipments as shipment>
                            <div class="tabletext">${uiLabelMap.OrderNbr}<a href="/facility/control/ViewShipment?shipmentId=${shipment.shipmentId}&externalLoginKey=${externalLoginKey}" class="buttontext">${shipment.shipmentId}</a>&nbsp;&nbsp;<a href="/facility/control/PackingSlip.pdf?shipmentId=${shipment.shipmentId}&externalLoginKey=${externalLoginKey}" class="buttontext">${uiLabelMap.ProductPackingSlip}</a></div>
                        </#list>
                    </td>
                  </tr>
               </#if>

               <#-- shipment actions -->
               <#if security.hasEntityPermission("ORDERMGR", "_UPDATE", session) && ((orderHeader.statusId == "ORDER_APPROVED") || (orderHeader.statusId == "ORDER_SENT"))>

                 <#-- Special shipment options -->
                 <#if security.hasEntityPermission("FACILITY", "_CREATE", session)>
                 <tr><td colspan="7"><hr class="sepbar"></td></tr>
                 <tr>
                   <td align="right" valign="top" width="15%">
                     <div class="tabletext">&nbsp;<#if orderHeader.orderTypeId == "PURCHASE_ORDER"><b>${uiLabelMap.ProductDestinationFacility}</b></#if></div>
                   </td>
                   <td width="5">&nbsp;</td>
                   <td align="left" valign="top" width="80%">
                     <div class="tabletext">
                       <#if orderHeader.orderTypeId == "SALES_ORDER">
                         <#if !shipGroup.supplierPartyId?has_content>
                           <a href="<@ofbizUrl>quickShipOrder?${paramString}</@ofbizUrl>" class="buttontext">${uiLabelMap.OrderQuickShipEntireOrder}</a>
                         </#if>
                       <#else> <#-- PURCHASE_ORDER -->
                         <#assign facilities = facilitiesForShipGroup.get(shipGroup.shipGroupSeqId)>
                         <#if facilities?has_content>
                         <form action="/facility/control/quickShipPurchaseOrder" method="POST">
                           <input type="hidden" name="initialSelected" value="Y"/>
                           <input type="hidden" name="orderId" value="${orderId}"/>
                           <#-- destination form (/facility/control/ReceiveInventory) wants purchaseOrderId instead of orderId, so we set it here as a workaround -->
                           <input type="hidden" name="purchaseOrderId" value="${orderId}"/>
                           <select name="facilityId" class="selectBox">
                             <#list facilities as facility>
                               <option value="${facility.facilityId}">${facility.facilityName}</option>
                             </#list>
                           </select>
                           <input type="submit" class="smallSubmit" value="${uiLabelMap.OrderQuickReceivePurchaseOrder}">
                         </form>
                         </#if>
                       </#if>
                     </div>
                   </td>
                 </tr>
                </#if>
   
                 <#-- Manual shipment options -->
                 <tr><td colspan="7"><hr class="sepbar"></td></tr>
                 <tr>
                   <td align="right" valign="top" width="15%">
                     <div class="tabletext">&nbsp;</div>
                   </td>
                   <td width="5">&nbsp;</td>
                   <td align="left" valign="top" width="80%">

                     <#if orderHeader.orderTypeId == "SALES_ORDER">
                       <#if !shipGroup.supplierPartyId?has_content>
                       <div class="tabletext"><a href="/facility/control/PackOrder?facilityId=${storeFacilityId?if_exists}&orderId=${orderId}&shipGroupSeqId=${shipGroup.shipGroupSeqId}&externalLoginKey=${externalLoginKey}" class="buttontext">${uiLabelMap.OrderPackShipmentForShipGroup} [${shipGroup.shipGroupSeqId}]</a></div>
                       <div class="tabletext"><a href="/facility/control/createShipment?primaryOrderId=${orderId}&primaryShipGroupSeqId=${shipGroup.shipGroupSeqId}&statusId=SHIPMENT_INPUT&originFacilityId=${storeFacilityId}&externalLoginKey=${externalLoginKey}" class="buttontext">${uiLabelMap.OrderNewShipmentForShipGroup} [${shipGroup.shipGroupSeqId}]</a></div>
                       </#if>
                     <#else>
                       <div class="tabletext">
                       <#assign facilities = facilitiesForShipGroup.get(shipGroup.shipGroupSeqId)>
                       <#if facilities?has_content>
                       <form action="/facility/control/createShipment" method="GET">
                           <input type="hidden" name="primaryOrderId" value="${orderId}"/>
                           <input type="hidden" name="primaryShipGroupSeqId" value="${shipGroup.shipGroupSeqId}"/>
                           <input type="hidden" name="shipmentTypeId" value="PURCHASE_SHIPMENT"/>
                           <input type="hidden" name="statusId" value="PURCH_SHIP_CREATED"/>
                           <input type="hidden" name="externalLoginKey" value="${externalLoginKey}"/>
                           <select name="destinationFacilityId" class="selectBox">
                             <#list facilities as facility>
                               <option value="${facility.facilityId}">${facility.facilityName}</option>
                             </#list>
                           </select>
                           <input type="submit" class="smallSubmit" value="${uiLabelMap.OrderNewShipmentForShipGroup} [${shipGroup.shipGroupSeqId}]">
                         </div>
                       </form>
                       <#else>
                           <div class="tabletext"><a href="/facility/control/createShipment?primaryOrderId=${orderId}&amp;primaryShipGroupSeqId=${shipGroup.shipGroupSeqId}&amp;shipmentTypeId=DROP_SHIPMENT&amp;statusId=PURCH_SHIP_CREATED&amp;externalLoginKey=${externalLoginKey}" class="buttontext">${uiLabelMap.OrderNewDropShipmentForShipGroup} [${shipGroup.shipGroupSeqId}]</a></div>
                       </#if>
                       </div>
                     </#if>
                   </td>
                 </tr>

               </#if>

               <#-- Refunds/Returns for Sales Orders and Delivery Schedules -->
               <#if !shipGroup_has_next>
                 <tr><td colspan="7"><hr class="sepbar"></td></tr>
                 <tr>
                   <td align="right" valign="top" width="15%">
                     <div class="tabletext">
                       &nbsp;
                     </div>
                   </td>
                   <td width="5">&nbsp;</td>
                   <td align="left" valign="top" width="80%">
                     <#if security.hasEntityPermission("ORDERMGR", "_UPDATE", session)>
                       <#if orderHeader.statusId != "ORDER_COMPLETED" && orderHeader.statusId != "ORDER_CANCELLED">
                         <div class="tabletext"><a href="<@ofbizUrl>OrderDeliveryScheduleInfo?orderId=${orderId}</@ofbizUrl>" class="buttontext">${uiLabelMap.OrderViewEditDeliveryScheduleInfo}</a></div>
                       </#if>
                       <#if security.hasEntityPermission("ORDERMGR", "_RETURN", session) && orderHeader.statusId == "ORDER_COMPLETED">
                         <div><a href="<@ofbizUrl>quickRefundOrder?orderId=${orderId}&receiveReturn=true&returnHeaderTypeId=${returnHeaderTypeId}</@ofbizUrl>" class="buttontext">${uiLabelMap.OrderQuickRefundEntireOrder}</a></div>
                         <div><a href="<@ofbizUrl>quickreturn?orderId=${orderId}&party_id=${partyId?if_exists}&returnHeaderTypeId=${returnHeaderTypeId}</@ofbizUrl>" class="buttontext">${uiLabelMap.OrderCreateReturn}</a></div>
                       </#if>
                     </#if>
                   </td>
                 </tr>
               </#if>
              </table>
            </div>
        </div>
      </#list>
     </#if>
      <#-- end of shipping info box -->
    </td>
  </tr>
</table>

<#else/>
    <div class="head2">${uiLabelMap.OrderNoOrderFound} ${uiLabelMap.CommonWith} ${uiLabelMap.CommonId}: [${orderId?if_exists}]</div>
</#if>
