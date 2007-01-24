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
