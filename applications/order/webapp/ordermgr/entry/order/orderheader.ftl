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

<#if orderHeader?exists>
  <div class="boxtop">
    <div class="boxhead" align="left">
      &nbsp;${uiLabelMap.OrderOrderConfirmation}&nbsp;#<a href="<@ofbizUrl>orderview?orderId=${orderHeader.orderId}</@ofbizUrl>" class="lightbuttontext">${orderHeader.orderId}</a>
    </div>
  </div>
</#if>

<div class="screenlet">
    <div class="screenlet-body">
         <table width="100%" border="0" cellpadding="1">
           <#-- order name -->
           <#if (orderName?exists)>
             <tr>
               <td align="right" valign="top" width="15%">
                 <span class="tabletext">&nbsp;<b>${uiLabelMap.OrderOrderName}</b> </span>
               </td>
               <td width="5">&nbsp;</td>
               <td align="left" valign="top" width="80%" class="tabletext">
                 ${orderName}
               </td>
            </tr>
            <tr><td colspan="7"><hr class="sepbar"/></td></tr>
          </#if>
          <#-- order for party -->
           <#if (orderForParty?exists)>
             <tr>
               <td align="right" valign="top" width="15%">
                 <span class="tabletext">&nbsp;<b>${uiLabelMap.OrderOrderFor}</b> </span>
               </td>
               <td width="5">&nbsp;</td>
               <td align="left" valign="top" width="80%" class="tabletext">
               ${Static["org.ofbiz.party.party.PartyHelper"].getPartyName(orderForParty, false)} [${orderForParty.partyId}]
              </td>
            </tr>
            <tr><td colspan="7"><hr class="sepbar"/></td></tr>
          </#if>
            <#if orderTerms?has_content>
              <tr>
                <td align="right" valign="top" width="15%">
                  <div class="tabletext">&nbsp;<b>${uiLabelMap.OrderOrderTerms}</b></div>
                </td>
                <td width="5">&nbsp;</td>
                <td align="left" valign="top" width="80%">
                   <table>
                     <tr>
                       <td width="33%"><div class="tabletext"><b>${uiLabelMap.OrderOrderTermType}</b></div></td>
                       <td width="33%"><div class="tabletext"><b>${uiLabelMap.OrderOrderTermValue}</b></div></td>
                       <td width="33%"><div class="tabletext"><b>${uiLabelMap.OrderOrderTermDays}</b></div></td>
                     </tr>
                     <tr><td colspan="3"><hr class="sepbar"/></td></tr>
                     <#assign index=0/>
                     <#list orderTerms as orderTerm>
                       <tr>
                         <td width="33%"><div class="tabletext">${orderTerm.getRelatedOne("TermType").get("description",locale)}</div></td>
                         <td width="33%"><div class="tabletext">${orderTerm.termValue?default("")}</div></td>
                         <td width="33%"><div class="tabletext">${orderTerm.termDays?default("")}</div></td>
                       </tr>
                       <#if orderTerms.size()&lt;index>
                         <tr><td colspan="3"><hr class="sepbar"/></td></tr>
                       </#if>
                       <#assign index=index+1/>
                     </#list>
                </table>
              </td>
            </tr>
            <tr><td colspan="7"><hr class="sepbar"/></td></tr>
          </#if>
            <#-- tracking number -->
            <#if trackingNumber?has_content>
              <tr>
                <td align="right" valign="top" width="15%">
                  <div class="tabletext">&nbsp;<b>${uiLabelMap.FacilityTrackingNumber}</b></div>
                </td>
                <td width="5">&nbsp;</td>
                <td align="left" valign="top" width="80%">
                  <#-- TODO: add links to UPS/FEDEX/etc based on carrier partyId  -->
                  <div class="tabletext">${trackingNumber}</div>
                </td>
              </tr>
            <tr><td colspan="7"><hr class="sepbar"/></td></tr>
            </#if>

            <#-- splitting preference -->
            <tr>
              <td align="right" valign="top" width="15%">
                <div class="tabletext">&nbsp;<b>${uiLabelMap.OrderSplittingPreference}</b></div>
              </td>
              <td width="5">&nbsp;</td>
              <td align="left" valign="top" width="80%">
                <div class="tabletext">
                  <#if maySplit?default("N") == "N">${uiLabelMap.FacilityWaitEntireOrderReady}</#if>
                  <#if maySplit?default("Y") == "Y">${uiLabelMap.FacilityShipAvailable}</#if>
                </div>
              </td>
            </tr>
            <#-- shipping instructions -->
            <#if shippingInstructions?has_content>
              <tr><td colspan="7"><hr class="sepbar"/></td></tr>
              <tr>
                <td align="right" valign="top" width="15%">
                  <div class="tabletext">&nbsp;<b>${uiLabelMap.CommonInstructions}</b></div>
                </td>
                <td width="5">&nbsp;</td>
                <td align="left" valign="top" width="80%">
                      <div class="tabletext">${shippingInstructions}</div>
                    </td>
              </tr>
            </#if>
                <tr><td colspan="7"><hr class="sepbar"/></td></tr>
              <#if orderType != "PURCHASE_ORDER">
                <#-- gift settings -->
            <tr>
                  <td align="right" valign="top" width="15%">
                    <div class="tabletext">&nbsp;<b>${uiLabelMap.OrderGift}?</b></div>
              </td>
                  <td width="5">&nbsp;</td>
                  <td align="left" valign="top" width="80%">
                    <div class="tabletext">
                      <#if isGift?default("N") == "N">${uiLabelMap.OrderThisOrderNotGift}</#if>
                      <#if isGift?default("N") == "Y">${uiLabelMap.OrderThisOrderGift}</#if>
                </div>
              </td>
            </tr>
                  <tr><td colspan="7"><hr class="sepbar"/></td></tr>
                <#if giftMessage?has_content>
              <tr>
                    <td align="right" valign="top" width="15%">
                      <div class="tabletext">&nbsp;<b>${uiLabelMap.OrderGiftMessage}</b></div>
                </td>
                    <td width="5">&nbsp;</td>
                    <td align="left" valign="top" width="80%">
                      <div class="tabletext">${giftMessage}</div>
                </td>
              </tr>
                   <tr><td colspan="7"><hr class="sepbar"/></td></tr>
            </#if>
          </#if>
            <#if shipAfterDate?has_content>
             <tr>
                <td align="right" valign="top" width="15%">
                    <div class="tabletext">&nbsp;<b>${uiLabelMap.OrderShipAfterDate}</b></div>
                </td>
                <td width="5">&nbsp;</td>
                <td align="left" valign="top" width="80%">
                    <div class="tabletext">${shipAfterDate}</div>
                </td>
            </tr>
            </#if>
            <#if shipBeforeDate?has_content>
            <tr>
               <td align="right" valign="top" width="15%">
                   <div class="tabletext">&nbsp;<b>${uiLabelMap.OrderShipBeforeDate}</b></div>
               </td>
               <td width="5">&nbsp;</td>
               <td align="left" valign="top" width="80%">
                   <div class="tabletext">${shipBeforeDate}</div>
               </td>
             </tr>
           </#if>
        </table>
    </div>
</div>
   
<#if paymentMethod?has_content || paymentMethodType?has_content || billingAccount?has_content>
<div class="screenlet">
    <div class="screenlet-header">
        <div class="boxhead">&nbsp;${uiLabelMap.AccountingPaymentInformation}</div>
    </div>
    <div class="screenlet-body">
        <#-- order payment info -->
      <table width="100%" border="0" cellpadding="1">
        <#-- offline payment address infomation :: change this to use Company's address -->
        <#if !paymentMethod?has_content && paymentMethodType?has_content>
          <tr>
            <#if paymentMethodType.paymentMethodTypeId == "EXT_OFFLINE">
              <td colspan="3" valign="top">
                <div class="tabletext" align="center"><b>${uiLabelMap.AccountingOfflinePayment}</b></div>                            
                <#if orderHeader?has_content && paymentAddress?has_content> 
                  <div class="tabletext" align="center"><hr class="sepbar"/></div>
                  <div class="tabletext" align="center"><b>${uiLabelMap.AccountingPleaseSendPaymentTo}:</b></div>
                  <#if paymentAddress.toName?has_content><div class="tabletext" align="center">${paymentAddress.toName}</div></#if>
                  <#if paymentAddress.attnName?has_content><div class="tabletext" align="center"><b>${uiLabelMap.CommonAttn}:</b> ${paymentAddress.attnName}</div></#if>
                  <div class="tabletext" align="center">${paymentAddress.address1}</div>
                  <#if paymentAddress.address2?has_content><div class="tabletext" align="center">${paymentAddress.address2}</div></#if>                            
                  <div class="tabletext" align="center">${paymentAddress.city}<#if paymentAddress.stateProvinceGeoId?has_content>, ${paymentAddress.stateProvinceGeoId}</#if> ${paymentAddress.postalCode}
                  <div class="tabletext" align="center">${paymentAddress.countryGeoId}</div>                                                                                                                
                  <div class="tabletext" align="center"><hr class="sepbar"/></div>
                  <div class="tabletext" align="center"><b>${uiLabelMap.OrderBeSureIncludeOrder} #</b></div>
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
        <#if paymentMethod?has_content>
          <#assign outputted = true>
          <#-- credit card info -->                     
          <#if creditCard?has_content>
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
          <#-- EFT account info -->
          <#elseif eftAccount?has_content>
            <tr>
              <td align="right" valign="top" width="15%">
                <div class="tabletext">&nbsp;<b>${uiLabelMap.AccountingEFTAccount}</b></div>
              </td>
              <td width="5">&nbsp;</td>
              <td align="left" valign="top" width="80%">
                <div class="tabletext">
                  ${eftAccount.nameOnAccount}<br/>
                  <#if eftAccount.companyNameOnAccount?has_content>${eftAccount.companyNameOnAccount}<br/></#if>
                  Bank: ${eftAccount.bankName}, ${eftAccount.routingNumber}<br/>
                  Account #: ${eftAccount.accountNumber}
                </div>
              </td>
            </tr>
          </#if>
        </#if>
        <#-- billing account info -->
        <#if billingAccount?has_content>
          <#if outputted?default(false)>
            <tr><td colspan="3"><hr class="sepbar"/></td></tr>
          </#if>
          <#assign outputted = true/>
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
      </table>
    </div>
</div>
</#if>
