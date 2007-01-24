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

<#-- NOTE: this template is used for the orderstatus screen in ecommerce AND for order notification emails through the OrderNoticeEmail.ftl file -->
<#-- the "urlPrefix" value will be prepended to URLs by the ofbizUrl transform if/when there is no "request" object in the context -->
<#if baseEcommerceSecureUrl?exists><#assign urlPrefix = baseEcommerceSecureUrl/></#if>

<table width="100%" border="0" cellpadding="0" cellspacing="0">
  <tr>
    <#-- left side -->
    <td width="50%" valign="top" align="left">

    <div class="screenlet">
        <div class="screenlet-header">
            <div class="boxlink">
                <#if maySelectItems?default("N") == "Y" && returnLink?default("N") == "Y" && (orderHeader.statusId)?if_exists == "ORDER_COMPLETED">
                    <a href="<@ofbizUrl>makeReturn?orderId=${orderHeader.orderId}</@ofbizUrl>" class="submenutextright">${uiLabelMap.OrderRequestReturn}</a>
                </#if>
            </div>
            <div class="boxhead">&nbsp;${uiLabelMap.OrderOrder}&nbsp;<#if orderHeader?has_content>${uiLabelMap.OrderNbr}<a href="<@ofbizUrl>orderstatus?orderId=${orderHeader.orderId}</@ofbizUrl>" class="lightbuttontext">${orderHeader.orderId}</a>&nbsp;</#if>${uiLabelMap.CommonInformation}</div>
        </div>
        <div class="screenlet-body">
            <table width="100%" border="0" cellpadding="1">
                <#-- placing customer information -->
                <#if localOrderReadHelper?exists && orderHeader?has_content>
                  <#assign displayParty = localOrderReadHelper.getPlacingParty()?if_exists/>
                  <#if displayParty?has_content>
                      <#assign displayPartyNameResult = dispatcher.runSync("getPartyNameForDate", Static["org.ofbiz.base.util.UtilMisc"].toMap("partyId", displayParty.partyId, "compareDate", orderHeader.orderDate, "userLogin", userLogin))/>
                  </#if>
                  <tr>
                    <td align="right" valign="top" width="15%">
                      <div class="tabletext">&nbsp;<b>${uiLabelMap.PartyName}</b></div>
                    </td>
                    <td width="5">&nbsp;</td>
                    <td align="left" valign="top" width="80%">
                      <div class="tabletext">
                        ${(displayPartyNameResult.fullName)?default("[Name Not Found]")}
                      </div>
                    </td>
                  </tr>
                  <tr><td colspan="7"><hr class="sepbar"/></td></tr>
                </#if>
                <#-- order status information -->
                <tr>
                  <td align="right" valign="top" width="15%">
                    <div class="tabletext">&nbsp;<b>${uiLabelMap.CommonStatus}</b></div>
                  </td>
                  <td width="5">&nbsp;</td>
                  <td align="left" valign="top" width="80%">
                    <#if orderHeader?has_content>                                                
                      <div class="tabletext">${localOrderReadHelper.getStatusString()}</div>
                    <#else>
                      <div class="tabletext"><b>${uiLabelMap.OrderNotYetOrdered}</b></div>
                    </#if>
                  </td>
                </tr>
                <#-- ordered date -->
                <#if orderHeader?has_content>
                  <tr><td colspan="7"><hr class="sepbar"/></td></tr>
                  <tr>
                    <td align="right" valign="top" width="15%">
                      <div class="tabletext">&nbsp;<b>${uiLabelMap.CommonDate}</b></div>
                    </td>
                    <td width="5">&nbsp;</td>
                    <td align="left" valign="top" width="80%">
                      <div class="tabletext">${orderHeader.orderDate.toString()}</div>
                    </td>
                  </tr>
                </#if>
                <#if distributorId?exists>
                  <tr><td colspan="7"><hr class="sepbar"/></td></tr>
                  <tr>
                    <td align="right" valign="top" width="15%">
                      <div class="tabletext">&nbsp;<b>${uiLabelMap.OrderDistributor}</b></div>
                    </td>
                    <td width="5">&nbsp;</td>
                    <td align="left" valign="top" width="80%">
                      <div class="tabletext">${distributorId}</div>
                    </td>
                  </tr>
                </#if>
            </table>
        </div>
    </div>
      
      <#if paymentMethods?has_content || paymentMethodType?has_content || billingAccount?has_content>
        <#-- order payment info -->
    <div class="screenlet">
        <div class="screenlet-header">
            <div class="boxhead">&nbsp;${uiLabelMap.AccountingPaymentInformation}</div>
        </div>
        <div class="screenlet-body">
            <table width="100%" border="0" cellpadding="1">
                <#-- offline payment address infomation :: change this to use Company's address -->
                <#if !paymentMethod?has_content && paymentMethodType?has_content>
                  <tr>
                    <#if paymentMethodType.paymentMethodTypeId == "EXT_OFFLINE">
                      <td colspan="3" valign="top">
                        <div class="tabletext" align="center"><b>${uiLabelMap.AccountingOfflinePayment}</b></div>                            
                        <#if orderHeader?has_content && paymentAddress?has_content> 
                          <div class="tabletext" align="center"><hr class="sepbar"/></div>
                          <div class="tabletext" align="center"><b>${uiLabelMap.OrderSendPaymentTo}:</b></div>
                          <#if paymentAddress.toName?has_content><div class="tabletext" align="center">${paymentAddress.toName}</div></#if>
                          <#if paymentAddress.attnName?has_content><div class="tabletext" align="center"><b>${uiLabelMap.PartyAddrAttnName}:</b> ${paymentAddress.attnName}</div></#if>
                          <div class="tabletext" align="center">${paymentAddress.address1}</div>
                          <#if paymentAddress.address2?has_content><div class="tabletext" align="center">${paymentAddress.address2}</div></#if>                            
                          <div class="tabletext" align="center">${paymentAddress.city}<#if paymentAddress.stateProvinceGeoId?has_content>, ${paymentAddress.stateProvinceGeoId}</#if> ${paymentAddress.postalCode?if_exists}
                          <div class="tabletext" align="center">${paymentAddress.countryGeoId}</div>                                                                                                                
                          <div class="tabletext" align="center"><hr class="sepbar"/></div>
                          <div class="tabletext" align="center"><b>${uiLabelMap.OrderBeSureToIncludeYourOrderNb}</b></div>
                        </#if>                         
                      </td>                  
                    <#else>
                      <#assign outputted = true>
                      <td colspan="3" valign="top">
                        <div class="tabletext" align="center"><b>${uiLabelMap.AccountingPaymentVia} ${paymentMethodType.get("description",locale)}</b></div>
                      </td>
                    </#if>
                  </tr>
                </#if>
                <#if paymentMethods?has_content>
                  <#list paymentMethods as paymentMethod>
                    <#if "CREDIT_CARD" == paymentMethod.paymentMethodTypeId>
                      <#assign creditCard = paymentMethod.getRelatedOne("CreditCard")>
                      <#assign formattedCardNumber = Static["org.ofbiz.party.contact.ContactHelper"].formatCreditCard(creditCard)>
                    <#elseif "GIFT_CARD" == paymentMethod.paymentMethodTypeId>
                      <#assign giftCard = paymentMethod.getRelatedOne("GiftCard")>
                    <#elseif "EFT_ACCOUNT" == paymentMethod.paymentMethodTypeId>
                      <#assign eftAccount = paymentMethod.getRelatedOne("EftAccount")>
                    </#if>

                    <#-- credit card info -->
                    <#if "CREDIT_CARD" == paymentMethod.paymentMethodTypeId && creditCard?has_content>
                      <#if outputted?default(false)>
                        <tr><td colspan="3"><hr class="sepbar"/></td></tr>
                      </#if>
                      <#assign pmBillingAddress = creditCard.getRelatedOne("PostalAddress")>
                      <tr>
                        <td align="right" valign="top" width="15%">
                          <div class="tabletext">&nbsp;<b>${uiLabelMap.AccountingCreditCard}</b></div>
                        </td>
                        <td width="5">&nbsp;</td>
                        <td align="left" valign="top" width="80%">
                          <div class="tabletext">
                            <#if creditCard.companyNameOnCard?has_content>${creditCard.companyNameOnCard}<br/></#if>
                            <#if creditCard.titleOnCard?has_content>${creditCard.titleOnCard}&nbsp</#if>
                            ${creditCard.firstNameOnCard}&nbsp;
                            <#if creditCard.middleNameOnCard?has_content>${creditCard.middleNameOnCard}&nbsp</#if>
                            ${creditCard.lastNameOnCard}
                            <#if creditCard.suffixOnCard?has_content>&nbsp;${creditCard.suffixOnCard}</#if>
                            <br/>
                            ${formattedCardNumber}
                          </div>
                        </td>
                      </tr>
                    <#-- Gift Card info -->
                    <#elseif "GIFT_CARD" == paymentMethod.paymentMethodTypeId && giftCard?has_content>
                      <#if outputted?default(false)>
                        <tr><td colspan="3"><hr class="sepbar"/></td></tr>
                      </#if>
                      <#if giftCard?has_content && giftCard.cardNumber?has_content>
                        <#assign pmBillingAddress = giftCard.getRelatedOne("PostalAddress")?if_exists>
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
                        <td align="right" valign="top" width="15%">
                          <div class="tabletext">&nbsp;<b>${uiLabelMap.AccountingGiftCard}</b></div>
                        </td>
                        <td width="5">&nbsp;</td>
                        <td align="left" valign="top" width="80%">
                          <div class="tabletext">
                            ${giftCardNumber}
                          </div>
                        </td>
                      </tr>
                    <#-- EFT account info -->
                    <#elseif "EFT_ACCOUNT" == paymentMethod.paymentMethodTypeId && eftAccount?has_content>
                      <#if outputted?default(false)>
                        <tr><td colspan="3"><hr class="sepbar"/></td></tr>
                      </#if>
                      <#assign pmBillingAddress = eftAccount.getRelatedOne("PostalAddress")>
                      <tr>
                        <td align="right" valign="top" width="15%">
                          <div class="tabletext">&nbsp;<b>${uiLabelMap.AccountingEftAccount}</b></div>
                        </td>
                        <td width="5">&nbsp;</td>
                        <td align="left" valign="top" width="80%">
                          <div class="tabletext">
                            ${eftAccount.nameOnAccount?if_exists}<br/>
                            <#if eftAccount.companyNameOnAccount?has_content>${eftAccount.companyNameOnAccount}<br/></#if>
                            ${uiLabelMap.AccountingBank}: ${eftAccount.bankName}, ${eftAccount.routingNumber}<br/>
                            ${uiLabelMap.AccountingAccount} #: ${eftAccount.accountNumber}
                          </div>
                        </td>
                      </tr>
                    </#if>
                    <#if pmBillingAddress?has_content>
                      <tr><td>&nbsp;</td><td colspan="2"><hr class="sepbar"/></td></tr>
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
                    <#assign outputted = true>
                  </#list>
                </#if>
                <#-- billing account info -->
                <#if billingAccount?has_content>
                  <#if outputted?default(false)>
                    <tr><td colspan="3"><hr class="sepbar"/></td></tr>
                  </#if>
                  <#assign outputted = true>
                  <tr>
                    <td align="right" valign="top" width="15%">
                      <div class="tabletext">&nbsp;<b>${uiLabelMap.AccountingBillingAccount}</b></div>
                    </td>
                    <td width="5">&nbsp;</td>
                    <td align="left" valign="top" width="80%">
                      <div class="tabletext">
                        #${billingAccount.billingAccountId?if_exists} - ${billingAccount.description?if_exists}
                      </div>
                    </td>
                  </tr>
                </#if>
                <#if (customerPoNumberSet?has_content)>
                  <tr><td colspan="7"><hr class="sepbar"/></td></tr>
                  <tr>
                    <td align="right" valign="top" width="15%">
                      <div class="tabletext">&nbsp;<b>${uiLabelMap.OrderPurchaseOrderNumber}</b></div>
                    </td>
                    <td width="5">&nbsp;</td>
                    <td align="left" valign="top" width="80%">
                      <#list customerPoNumberSet as customerPoNumber>
                        <div class="tabletext">${customerPoNumber?if_exists}</div>
                      </#list>
                    </td>
                  </tr>
                </#if>
            </table>
        </div>
    </div>
      </#if>
    </td>

    <td width="1">&nbsp;&nbsp;</td>
    <#-- right side -->

    <td width="50%" valign="top" align="left">
      <#if orderItemShipGroups?has_content>

    <div class="screenlet">
        <div class="screenlet-header">
            <div class="boxhead">&nbsp;${uiLabelMap.OrderShippingInformation}</div>
        </div>
        <div class="screenlet-body">
        <#-- shipping address -->
            <#assign groupIdx = 0>
            <#list orderItemShipGroups as shipGroup>
                <#if orderHeader?has_content>
                  <#assign shippingAddress = shipGroup.getRelatedOne("PostalAddress")?if_exists>
                  <#assign groupNumber = shipGroup.shipGroupSeqId?if_exists>
                <#else>
                  <#assign shippingAddress = cart.getShippingAddress(groupIdx)?if_exists>
                  <#assign groupNumber = groupIdx + 1>
                </#if>

              <table width="100%" border="0" cellpadding="1">
                <#if shippingAddress?has_content>
                  <tr>
                    <td align="right" valign="top" width="15%">
                      <div class="tabletext">&nbsp;<b>${uiLabelMap.OrderDestination}</b> [${groupNumber}]</div>
                    </td>
                    <td width="5">&nbsp;</td>
                    <td align="left" valign="top" width="80%">
                      <div class="tabletext">
                        <#if shippingAddress.toName?has_content><b>${uiLabelMap.CommonTo}:</b> ${shippingAddress.toName}<br/></#if>
                        <#if shippingAddress.attnName?has_content><b>${uiLabelMap.PartyAddrAttnName}:</b> ${shippingAddress.attnName}<br/></#if>
                        ${shippingAddress.address1}<br/>
                        <#if shippingAddress.address2?has_content>${shippingAddress.address2}<br/></#if>                            
                        ${shippingAddress.city}<#if shippingAddress.stateProvinceGeoId?has_content>, ${shippingAddress.stateProvinceGeoId} </#if>
                        ${shippingAddress.postalCode?if_exists}<br/>
                        ${shippingAddress.countryGeoId?if_exists}
                      </div>
                    </td>
                  </tr>
                  <tr><td colspan="7"><hr class="sepbar"/></td></tr>
                </#if>
                <tr>
                  <td align="right" valign="top" width="15%">
                    <div class="tabletext">&nbsp;<b>${uiLabelMap.OrderMethod}</b></div>
                  </td>
                  <td width="5">&nbsp;</td>
                  <td align="left" valign="top" width="80%">
                    <div class="tabletext">
                      <#if orderHeader?has_content>
                        <#assign shipmentMethodType = shipGroup.getRelatedOne("ShipmentMethodType")?if_exists>
                        <#assign carrierPartyId = shipGroup.carrierPartyId?if_exists>
                      <#else>
                        <#assign shipmentMethodType = cart.getShipmentMethodType(groupIdx)?if_exists>
                        <#assign carrierPartyId = cart.getCarrierPartyId(groupIdx)?if_exists>
                      </#if>

                      <#if carrierPartyId?exists && carrierPartyId != "_NA_">${carrierPartyId?if_exists}</#if>
                      ${(shipmentMethodType.description)?default("N/A")}
                      <#if shippingAccount?exists><br/>${uiLabelMap.AccountingUseAccount}: ${shippingAccount}</#if>
                    </div>
                  </td>
                </tr>
                <#-- tracking number -->
                <#if trackingNumber?has_content || orderShipmentInfoSummaryList?has_content>
                  <tr><td colspan="7"><hr class="sepbar"/></td></tr>
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
                          <div class="tabletext">
                            <#if (orderShipmentInfoSummaryList?size > 1)>${orderShipmentInfoSummary.shipmentPackageSeqId}: </#if>
                            Code: ${orderShipmentInfoSummary.trackingCode?default("[Not Yet Known]")}
                            <#if orderShipmentInfoSummary.boxNumber?has_content>${uiLabelMap.OrderBoxNubmer}${orderShipmentInfoSummary.boxNumber}</#if> 
                            <#if orderShipmentInfoSummary.carrierPartyId?has_content>(${uiLabelMap.ProductCarrier}: ${orderShipmentInfoSummary.carrierPartyId})</#if>
                          </div>
                        </#list>
                      </#if>
                    </td>
                  </tr>
                </#if>
                <tr><td colspan="7"><hr class="sepbar"/></td></tr>
                <#-- splitting preference -->
                <#if orderHeader?has_content>
                  <#assign maySplit = shipGroup.maySplit?default("N")>
                <#else>
                  <#assign maySplit = cart.getMaySplit(groupIdx)?default("N")>
                </#if>
                <tr>
                  <td align="right" valign="top" width="15%">
                    <div class="tabletext">&nbsp;<b>${uiLabelMap.OrderSplittingPreference}</b></div>
                  </td>
                  <td width="5">&nbsp;</td>
                  <td align="left" valign="top" width="80%">
                    <div class="tabletext">
                      <#if maySplit?default("N") == "N">${uiLabelMap.OrderPleaseWaitUntilBeforeShipping}.</#if>
                      <#if maySplit?default("N") == "Y">${uiLabelMap.OrderPleaseShipItemsBecomeAvailable}.</#if>
                    </div>
                  </td>
                </tr>
                <#-- shipping instructions -->
                <#if orderHeader?has_content>
                  <#assign shippingInstructions = shipGroup.shippingInstructions?if_exists>
                <#else>
                  <#assign shippingInstructions =  cart.getShippingInstructions(groupIdx)?if_exists>
                </#if>

                <#if shippingInstructions?has_content>
                  <tr><td colspan="7"><hr class="sepbar"/></td></tr>
                  <tr>
                    <td align="right" valign="top" width="15%">
                      <div class="tabletext">&nbsp;<b>${uiLabelMap.OrderIntructions}</b></div>
                    </td>
                    <td width="5">&nbsp;</td>
                    <td align="left" valign="top" width="80%">
                      <div class="tabletext">${shippingInstructions}</div>
                    </td>
                  </tr>
                </#if>
                <tr><td colspan="7"><hr class="sepbar"/></td></tr>
                <#-- gift settings -->
                <#if orderHeader?has_content>
                  <#assign isGift = shipGroup.isGift?default("N")>
                  <#assign giftMessage = shipGroup.giftMessage?if_exists>
                <#else>
                  <#assign isGift = cart.getIsGift(groupIdx)?default("N")>
                  <#assign giftMessage = cart.getGiftMessage(groupIdx)?if_exists>
                </#if>

               <#if productStore.showCheckoutGiftOptions?if_exists != "N">
                <tr>
                  <td align="right" valign="top" width="15%">
                    <div class="tabletext">&nbsp;<b>${uiLabelMap.OrderGift}?</b></div>
                  </td>
                  <td width="5">&nbsp;</td>
                  <td align="left" valign="top" width="80%">
                    <div class="tabletext">
                      <#if isGift?default("N") == "N">${uiLabelMap.OrderThisIsNotGift}.</#if>
                      <#if isGift?default("N") == "Y">${uiLabelMap.OrderThisIsGift}.</#if>
                    </div>
                  </td>
                </tr>
                <#if giftMessage?has_content>
                  <tr><td colspan="7"><hr class="sepbar"/></td></tr>
                  <tr>
                    <td align="right" valign="top" width="15%">
                      <div class="tabletext">&nbsp;<b>${uiLabelMap.OrderGiftMessage}</b></div>
                    </td>
                    <td width="5">&nbsp;</td>
                    <td align="left" valign="top" width="80%">
                      <div class="tabletext">${giftMessage}</div>
                    </td>
                  </tr>
                </#if>
               </#if>
                <#if shipGroup_has_next>
                  <tr><td colspan="7"><hr class="sepbar"/></td></tr>
                  <tr><td colspan="7"><hr class="sepbar"/></td></tr>
                </#if>
              </table>

                <#assign groupIdx = groupIdx + 1>
            </#list><#-- end list of orderItemShipGroups -->
        </div>
    </div>

      </#if>
    </td>
  </tr>
</table>
