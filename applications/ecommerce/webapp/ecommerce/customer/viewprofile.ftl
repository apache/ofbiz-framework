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

<#if party?exists>
<#-- Main Heading -->
<table width="100%" cellpadding="0" cellspacing="0" border="0">
  <tr>
    <td align="left">
      <div class="head1">${uiLabelMap.PartyTheProfileOf}
        <#if person?exists>
          ${person.personalTitle?if_exists}
          ${person.firstName?if_exists}
          ${person.middleName?if_exists}
          ${person.lastName?if_exists}
          ${person.suffix?if_exists}
        <#else>
          "${uiLabelMap.PartyNewUser}"
        </#if>
      </div>
    </td>
    <td align="right">
      <#if showOld>
        <a href="<@ofbizUrl>viewprofile</@ofbizUrl>" class="buttontext">${uiLabelMap.PartyHideOld}</a>&nbsp;&nbsp;
      <#else>
        <a href="<@ofbizUrl>viewprofile?SHOW_OLD=true</@ofbizUrl>" class="buttontext">${uiLabelMap.PartyShowOld}</a>&nbsp;&nbsp;
      </#if>
      <#if (productStore.enableDigProdUpload)?if_exists == "Y">
      &nbsp;<a href="<@ofbizUrl>digitalproductlist</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonDigitalProductUpload}</a>
      </#if>
    </td>
  </tr>
</table>
<br/>

<div class="screenlet">
    <div class="screenlet-header">
        <div class="boxlink">
            <a href="<@ofbizUrl>editperson</@ofbizUrl>" class="submenutextright">
            <#if person?exists>${uiLabelMap.CommonUpdate}<#else>${uiLabelMap.CommonCreate}</#if></a>
        </div>
        <div class="boxhead">&nbsp;${uiLabelMap.PartyPersonalInformation}</div>
    </div>
    <div class="screenlet-body">
<#if person?exists>
<div>
  <table width="100%" border="0" cellpadding="0" cellspacing="0">
    <tr>
      <td align="right" width="10%"><div class="tabletext"><b>${uiLabelMap.PartyName}</b></div></td>
      <td width="5">&nbsp;</td>
      <td align="left" width="90%">
        <div class="tabletext">
          ${person.personalTitle?if_exists}
          ${person.firstName?if_exists}
          ${person.middleName?if_exists}
          ${person.lastName?if_exists}
          ${person.suffix?if_exists}
        </div>
      </td>
    </tr>
    <#if person.nickname?has_content><tr><td align="right" nowrap><div class="tabletext"><b>${uiLabelMap.PartyNickName}</b></div></td><td>&nbsp;</td><td align="left"><div class="tabletext">${person.nickname}</div></td></tr></#if>
    <#if person.gender?has_content><tr><td align="right" nowrap><div class="tabletext"><b>${uiLabelMap.PartyGender}</b></div></td><td>&nbsp;</td><td align="left"><div class="tabletext">${person.gender}</div></td></tr></#if>
    <#if person.birthDate?exists><tr><td align="right" nowrap><div class="tabletext"><b>${uiLabelMap.PartyBirthDate}</b></div></td><td>&nbsp;</td><td align="left"><div class="tabletext">${person.birthDate.toString()}</div></td></tr></#if>
    <#if person.height?exists><tr><td align="right" nowrap><div class="tabletext"><b>${uiLabelMap.PartyHeight}</b></div></td><td>&nbsp;</td><td align="left"><div class="tabletext">${person.height}</div></td></tr></#if>
    <#if person.weight?exists><tr><td align="right" nowrap><div class="tabletext"><b>${uiLabelMap.PartyWeight}</b></div></td><td>&nbsp;</td><td align="left"><div class="tabletext">${person.weight}</div></td></tr></#if>
    <#if person.mothersMaidenName?has_content><tr><td align="right" nowrap><div class="tabletext"><b>${uiLabelMap.PartyMaidenName}</b></div></td><td>&nbsp;</td><td align="left"><div class="tabletext">${person.mothersMaidenName}</div></td></tr></#if>
    <#if person.maritalStatus?has_content><tr><td align="right" nowrap><div class="tabletext"><b>${uiLabelMap.PartyMaritalStatus}</b></div></td><td>&nbsp;</td><td align="left"><div class="tabletext">${person.maritalStatus}</div></td></tr></#if>
    <#if person.socialSecurityNumber?has_content><tr><td align="right" nowrap><div class="tabletext"><b>${uiLabelMap.PartySocialSecurityNumber}</b></div></td><td>&nbsp;</td><td align="left"><div class="tabletext">${person.socialSecurityNumber}</div></td></tr></#if>
    <#if person.passportNumber?has_content><tr><td align="right" nowrap><div class="tabletext"><b>${uiLabelMap.PartyPassportNumber}</b></div></td><td>&nbsp;</td><td align="left"><div class="tabletext">${person.passportNumber}</div></td></tr></#if>
    <#if person.passportExpireDate?exists><tr><td align="right" nowrap><div class="tabletext"><b>${uiLabelMap.PartyPassportExpireDate}</b></div></td><td>&nbsp;</td><td align="left"><div class="tabletext">${person.passportExpireDate.toString()}</div></td></tr></#if>
    <#if person.totalYearsWorkExperience?exists><tr><td align="right" nowrap><div class="tabletext"><b>${uiLabelMap.PartyYearsWork}</b></div></td><td>&nbsp;</td><td align="left"><div class="tabletext">${person.totalYearsWorkExperience}</div></td></tr></#if>
    <#if person.comments?has_content><tr><td align="right" nowrap><div class="tabletext"><b>${uiLabelMap.CommonComments}</b></div></td><td>&nbsp;</td><td align="left"><div class="tabletext">${person.comments}</div></td></tr></#if>
  </table>
</div>
<#else>
<div class="tabletext">${uiLabelMap.PartyPersonalInformationNotFound}</div>
</#if>
    </div>
</div>

<#-- ============================================================= -->
<#if monthsToInclude?exists && totalSubRemainingAmount?exists && totalOrders?exists>
<div class="screenlet">
    <div class="screenlet-header">
        <div class="boxhead">&nbsp;${uiLabelMap.EcommerceLoyaltyPoints}</div>
    </div>
    <div class="screenlet-body">
        <div class="tabletext">${uiLabelMap.EcommerceYouHave} ${totalSubRemainingAmount} ${uiLabelMap.EcommercePointsFrom} ${totalOrders} ${uiLabelMap.EcommerceOrderInLast} ${monthsToInclude} ${uiLabelMap.EcommerceMonths}</div>
    </div>
</div>
</#if>

<#-- ============================================================= -->
<div class="screenlet">
    <div class="screenlet-header">
        <div class="boxlink">
            <a href="<@ofbizUrl>editcontactmech</@ofbizUrl>" class="submenutextright">${uiLabelMap.CommonCreateNew}</a>
        </div>
        <div class="boxhead">&nbsp;${uiLabelMap.PartyContactInformation}</div>
    </div>
    <div class="screenlet-body">
  <#if partyContactMechValueMaps?has_content>
    <table width="100%" border="0" cellpadding="0">
      <tr align="left" valign="bottom">
        <th class="tableheadtext">${uiLabelMap.PartyContactType}</th>
        <th class="tableheadtext" width="5">&nbsp;</th>
        <th class="tableheadtext">${uiLabelMap.CommonInformation}</th>
        <th class="tableheadtext" colspan="2">${uiLabelMap.PartySolicitingOk}?</th>
        <th class="tableheadtext">&nbsp;</th>
        <th class="tableheadtext">&nbsp;</th>
        <th class="tableheadtext">&nbsp;</th>
      </tr>
      <#list partyContactMechValueMaps as partyContactMechValueMap>
        <#assign contactMech = partyContactMechValueMap.contactMech?if_exists>
        <#assign contactMechType = partyContactMechValueMap.contactMechType?if_exists>
        <#assign partyContactMech = partyContactMechValueMap.partyContactMech?if_exists>
          <tr><td colspan="7"><hr class="sepbar"/></td></tr>
          <tr>
            <td align="right" valign="top" width="10%">
              <div class="tabletext">&nbsp;<b>${contactMechType.get("description",locale)}</b></div>
            </td>
            <td width="5">&nbsp;</td>
            <td align="left" valign="top" width="80%">
              <#list partyContactMechValueMap.partyContactMechPurposes?if_exists as partyContactMechPurpose> 
                  <#assign contactMechPurposeType = partyContactMechPurpose.getRelatedOneCache("ContactMechPurposeType")>
                    <div class="tabletext">
                      <#if contactMechPurposeType?exists>
                        <b>${contactMechPurposeType.get("description",locale)}</b>
                        <#if contactMechPurposeType.contactMechPurposeTypeId == "SHIPPING_LOCATION" && (profiledefs.defaultShipAddr)?default("") == contactMech.contactMechId>
                          <span class="buttontextdisabled">${uiLabelMap.EcommerceIsDefault}</span>
                        <#elseif contactMechPurposeType.contactMechPurposeTypeId == "SHIPPING_LOCATION">
                          <a href="<@ofbizUrl>setprofiledefault/viewprofile?productStoreId=${productStoreId}&defaultShipAddr=${contactMech.contactMechId}&partyId=${party.partyId}</@ofbizUrl>" class="buttontext">${uiLabelMap.EcommerceSetDefault}</a>
                        </#if>
                      <#else>
                        <b>${uiLabelMap.PartyPurposeTypeNotFound}: "${partyContactMechPurpose.contactMechPurposeTypeId}"</b>
                      </#if>
                      <#if partyContactMechPurpose.thruDate?exists>(${uiLabelMap.CommonExpire}:${partyContactMechPurpose.thruDate.toString()})</#if>
                    </div>
              </#list>
              <#if contactMech.contactMechTypeId?if_exists = "POSTAL_ADDRESS">
                  <#assign postalAddress = partyContactMechValueMap.postalAddress?if_exists>
                  <div class="tabletext">
                  <#if postalAddress?exists>
                    <#if postalAddress.toName?has_content><b>${uiLabelMap.CommonTo}:</b> ${postalAddress.toName}<br/></#if>
                    <#if postalAddress.attnName?has_content><b>${uiLabelMap.PartyAddrAttnName}:</b> ${postalAddress.attnName}<br/></#if>
                    ${postalAddress.address1}<br/>
                    <#if postalAddress.address2?has_content>${postalAddress.address2}<br/></#if>
                    ${postalAddress.city}<#if postalAddress.stateProvinceGeoId?has_content>,&nbsp;${postalAddress.stateProvinceGeoId}</#if>&nbsp;${postalAddress.postalCode?if_exists}
                    <#if postalAddress.countryGeoId?has_content><br/>${postalAddress.countryGeoId}</#if>
                    <#if (!postalAddress.countryGeoId?has_content || postalAddress.countryGeoId?if_exists = "USA")>
                      <#assign addr1 = postalAddress.address1?if_exists>
                      <#if (addr1.indexOf(" ") > 0)>
                        <#assign addressNum = addr1.substring(0, addr1.indexOf(" "))>
                        <#assign addressOther = addr1.substring(addr1.indexOf(" ")+1)>
                        <a target="_blank" href="${uiLabelMap.EcommerceLookupWhitepagesLink}" class="linktext">(${uiLabelMap.EcommerceLookupWhitepages})</a>
                      </#if>
                    </#if>
                  <#else>
                    ${uiLabelMap.PartyPostalInformationNotFound}.
                  </#if>
                  </div>
              <#elseif contactMech.contactMechTypeId?if_exists = "TELECOM_NUMBER">
                  <#assign telecomNumber = partyContactMechValueMap.telecomNumber?if_exists>
                  <div class="tabletext">
                  <#if telecomNumber?exists>
                    ${telecomNumber.countryCode?if_exists}
                    <#if telecomNumber.areaCode?has_content>${telecomNumber.areaCode}-</#if>${telecomNumber.contactNumber?if_exists}
                    <#if partyContactMech.extension?has_content>ext&nbsp;${partyContactMech.extension}</#if>
                    <#if (!telecomNumber.countryCode?has_content || telecomNumber.countryCode = "011")>
                      <a target="_blank" href="http://www.anywho.com/qry/wp_rl?npa=${telecomNumber.areaCode?if_exists}&telephone=${telecomNumber.contactNumber?if_exists}&btnsubmit.x=20&btnsubmit.y=8" class="linktext">(lookup:anywho.com)</a>
                      <a target="_blank" href="http://whitepages.com/find_person_results.pl?fid=p&ac=${telecomNumber.areaCode?if_exists}&s=&p=${telecomNumber.contactNumber?if_exists}&pt=b&x=40&y=9" class="linktext">(lookup:whitepages.com)</a>
                    </#if>
                  <#else>
                    ${uiLabelMap.PartyPhoneNumberInfoNotFound}.
                  </#if>
                  </div>
              <#elseif contactMech.contactMechTypeId?if_exists = "EMAIL_ADDRESS">
                  <div class="tabletext">
                    ${contactMech.infoString}
                    <a href="mailto:${contactMech.infoString}" class="linktext">(${uiLabelMap.PartySendEmail})</a>
                  </div>
              <#elseif contactMech.contactMechTypeId?if_exists = "WEB_ADDRESS">
                  <div class="tabletext">
                    ${contactMech.infoString}
                    <#assign openAddress = contactMech.infoString?if_exists>
                    <#if !openAddress.startsWith("http") && !openAddress.startsWith("HTTP")><#assign openAddress = "http://" + openAddress></#if>
                    <a target="_blank" href="${openAddress}" class="linktext">(${uiLabelMap.CommonOpenNewWindow})</a>
                  </div>
              <#else>
                  <div class="tabletext">${contactMech.infoString?if_exists}</div>
              </#if>
              <div class="tabletext">(${uiLabelMap.CommonUpdated}:&nbsp;${partyContactMech.fromDate.toString()})</div>
              <#if partyContactMech.thruDate?exists><div class="tabletext"><b>${uiLabelMap.CommonDelete}:&nbsp;${partyContactMech.thruDate.toString()}</b></div></#if>
            </td>
            <td align="center" valign="top" nowrap width="1%"><div class="tabletext"><b>(${partyContactMech.allowSolicitation?if_exists})</b></div></td>
            <td width="5">&nbsp;</td>
            <td align="right" valign="top" nowrap width="1%" nowrap>
              <div><a href="<@ofbizUrl>editcontactmech?contactMechId=${contactMech.contactMechId}</@ofbizUrl>" class="buttontext">
              ${uiLabelMap.CommonUpdate}</a>&nbsp;</div>
            </td>
            <td align="right" valign="top" width="1%" nowrap>
              <div><a href="<@ofbizUrl>deleteContactMech/viewprofile?contactMechId=${contactMech.contactMechId}</@ofbizUrl>" class="buttontext">
              ${uiLabelMap.CommonExpire}</a>&nbsp;&nbsp;</div>
            </td>
          </tr>
      </#list>
    </table>
  <#else>
    <p>${uiLabelMap.PartyNoContactInformation}.</p><br/>
  </#if>
    </div>
</div>
<#-- ============================================================= -->

<div class="screenlet">
    <div class="screenlet-header">
        <div class="boxlink">
            <a href="<@ofbizUrl>editcreditcard</@ofbizUrl>" class="submenutext">${uiLabelMap.PartyCreateNewCreditCard}</a><a href="<@ofbizUrl>editgiftcard</@ofbizUrl>" class="submenutext">${uiLabelMap.PartyCreateNewGiftCard}</a><a href="<@ofbizUrl>editeftaccount</@ofbizUrl>" class="submenutextright">${uiLabelMap.PartyCreateNewEftAccount}</a>
        </div>
        <div class="boxhead">&nbsp;${uiLabelMap.AccountingPaymentMethodInformation}</div>
    </div>
    <div class="screenlet-body">
      <table width="100%" border="0" cellpadding="1">
        <tr>
          <td align="left">
            <#if paymentMethodValueMaps?has_content>
              <table width="100%" cellpadding="2" cellspacing="0" border="0">
                <#list paymentMethodValueMaps as paymentMethodValueMap>
                    <#assign paymentMethod = paymentMethodValueMap.paymentMethod?if_exists>
                    <#assign creditCard = paymentMethodValueMap.creditCard?if_exists>
                    <#assign giftCard = paymentMethodValueMap.giftCard?if_exists>
                    <#assign eftAccount = paymentMethodValueMap.eftAccount?if_exists>
                    <tr>
                      <#if paymentMethod.paymentMethodTypeId?if_exists == "CREDIT_CARD">
                          <td width="80%" valign="top">
                            <div class="tabletext">
                              <b>
                                ${uiLabelMap.AccountingCreditCard}:&nbsp;
                                <#if creditCard.companyNameOnCard?has_content>${creditCard.companyNameOnCard}&nbsp;</#if>
                                <#if creditCard.titleOnCard?has_content>${creditCard.titleOnCard}&nbsp</#if>
                                ${creditCard.firstNameOnCard}&nbsp;
                                <#if creditCard.middleNameOnCard?has_content>${creditCard.middleNameOnCard}&nbsp</#if>
                                ${creditCard.lastNameOnCard}
                                <#if creditCard.suffixOnCard?has_content>&nbsp;${creditCard.suffixOnCard}</#if>
                                &nbsp;${Static["org.ofbiz.party.contact.ContactHelper"].formatCreditCard(creditCard)}
                              </b>
                              <#if paymentMethod.description?has_content>(${paymentMethod.description})</#if>
                              <#if paymentMethod.fromDate?has_content>(${uiLabelMap.CommonUpdated}:&nbsp;${paymentMethod.fromDate.toString()})</#if>
                              <#if paymentMethod.thruDate?exists><b>(${uiLabelMap.CommonDelete}:&nbsp;${paymentMethod.thruDate.toString()})</b></#if>
                            </div>
                          </td>
                          <td width="5">&nbsp;</td>
                          <td align="right" valign="top" width="1%" nowrap>
                            <div><a href="<@ofbizUrl>editcreditcard?paymentMethodId=${paymentMethod.paymentMethodId}</@ofbizUrl>" class="buttontext">
                            ${uiLabelMap.CommonUpdate}</a></div>
                          </td>
                      <#elseif paymentMethod.paymentMethodTypeId?if_exists == "GIFT_CARD">
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

                          <td width="80%" valign="top">
                            <div class="tabletext">
                              <b>${uiLabelMap.AccountingGiftCard}: ${giftCardNumber}</b>
                              <#if paymentMethod.description?has_content>(${paymentMethod.description})</#if>
                              <#if paymentMethod.fromDate?has_content>(${uiLabelMap.CommonUpdated}:&nbsp;${paymentMethod.fromDate.toString()})</#if>
                              <#if paymentMethod.thruDate?exists><b>(${uiLabelMap.CommonDelete}:&nbsp;${paymentMethod.thruDate.toString()})</b></#if>
                            </div>
                          </td>
                          <td width="5">&nbsp;</td>
                          <td align="right" valign="top" width="1%" nowrap>
                            <div><a href="<@ofbizUrl>editgiftcard?paymentMethodId=${paymentMethod.paymentMethodId}</@ofbizUrl>" class="buttontext">
                            ${uiLabelMap.CommonUpdate}</a></div>
                          </td>
                      <#elseif paymentMethod.paymentMethodTypeId?if_exists == "EFT_ACCOUNT">
                          <td width="80%" valign="top">
                            <div class="tabletext">
                              <b>${uiLabelMap.AccountingEftAccount}: ${eftAccount.nameOnAccount?if_exists} - <#if eftAccount.bankName?has_content>Bank: ${eftAccount.bankName}</#if> <#if eftAccount.accountNumber?has_content>${uiLabelMap.AccountingAccount} #: ${eftAccount.accountNumber}</#if></b>
                              <#if paymentMethod.description?has_content>(${paymentMethod.description})</#if>
                              <#if paymentMethod.fromDate?has_content>(${uiLabelMap.CommonUpdated}:&nbsp;${paymentMethod.fromDate.toString()})</#if>
                              <#if paymentMethod.thruDate?exists><b>(${uiLabelMap.CommonDelete}:&nbsp;${paymentMethod.thruDate.toString()})</b></#if>
                            </div>
                          </td>
                          <td width="5">&nbsp;</td>
                          <td align="right" valign="top" width="1%" nowrap>
                            <div><a href="<@ofbizUrl>editeftaccount?paymentMethodId=${paymentMethod.paymentMethodId}</@ofbizUrl>" class="buttontext">
                            ${uiLabelMap.CommonUpdate}</a></div>
                          </td>
                      </#if>
                      <td align="right" valign="top" width="1%" nowrap>
                        <div><a href="<@ofbizUrl>deletePaymentMethod/viewprofile?paymentMethodId=${paymentMethod.paymentMethodId}</@ofbizUrl>" class="buttontext">
                        ${uiLabelMap.CommonExpire}</a></div>
                      </td>
                      <td align="right" valign="top" width="1%" nowrap>
                        <#if (profiledefs.defaultPayMeth)?default("") == paymentMethod.paymentMethodId>
                          <span class="buttontextdisabled">${uiLabelMap.EcommerceIsDefault}</span>
                        <#else>
                          <div><a href="<@ofbizUrl>setprofiledefault/viewprofile?productStoreId=${productStoreId}&defaultPayMeth=${paymentMethod.paymentMethodId}&partyId=${party.partyId}</@ofbizUrl>" class="buttontext">
                          ${uiLabelMap.EcommerceSetDefault}</a></div>
                        </#if>
                      </td>
                    </tr>
                </#list>
              </table>
            <#else>
              <div class="tabletext">${uiLabelMap.AccountingNoPaymentMethodInformation}.</div>
            </#if>
          </td>
        </tr>
      </table>
    </div>
</div>

<#-- ============================================================= -->
<div class="screenlet">
    <div class="screenlet-header">
        <div class="boxhead">&nbsp;${uiLabelMap.PartyTaxIdentification}</div>
    </div>
    <div class="screenlet-body">
        <form method="post" action="<@ofbizUrl>createCustomerTaxAuthInfo</@ofbizUrl>" name="createCustTaxAuthInfoForm">
            <input type="hidden" name="partyId" value="${party.partyId}"/>
            ${screens.render("component://order/widget/ordermgr/OrderEntryOrderScreens.xml#customertaxinfo")}
            <input type="submit" value="${uiLabelMap.CommonAdd}" class="smallSubmit"/>
        </form>
    </div>
</div>

<#-- ============================================================= -->
<div class="screenlet">
    <div class="screenlet-header">
        <div class="boxlink">
            <a href="<@ofbizUrl>changepassword</@ofbizUrl>" class="submenutextright">${uiLabelMap.PartyChangePassword}</a>
        </div>
        <div class="boxhead">&nbsp;${uiLabelMap.CommonUsername} & ${uiLabelMap.CommonPassword}</div>
    </div>
    <div class="screenlet-body">
        <table width="100%" border="0" cellpadding="1">
          <tr>
            <td align="right" valign="top" width="10%" nowrap><div class="tabletext"><b>${uiLabelMap.CommonUsername}</b></div></td>
            <td width="5">&nbsp;</td>
            <td align="left" valign="top" width="90%"><div class="tabletext">${userLogin.userLoginId}</div></td>
          </tr>
        </table>
    </div>
</div>

<#-- ============================================================= -->
<form name="setdefaultshipmeth" action="<@ofbizUrl>setprofiledefault/viewprofile</@ofbizUrl>" method="post">
<input type="hidden" name="productStoreId" value="${productStoreId}">
<div class="screenlet">
    <div class="screenlet-header">
        <div class="boxlink">
            <#if profiledefs?has_content && profiledefs.defaultShipAddr?has_content && carrierShipMethods?has_content><a href="javascript:document.setdefaultshipmeth.submit();" class="submenutextright">${uiLabelMap.EcommerceSetDefault}</a></#if>
        </div>
        <div class="boxhead">&nbsp;${uiLabelMap.OrderDefaultShipmentMethod}</div>
    </div>
    <div class="screenlet-body">
        <table width="100%" border="0" cellpadding="1">
          <#if profiledefs?has_content && profiledefs.defaultShipAddr?has_content && carrierShipMethods?has_content>
            <#list carrierShipMethods as shipMeth>
              <#assign shippingMethod = shipMeth.shipmentMethodTypeId + "@" + shipMeth.partyId>
              <tr>
                <td width="5%">&nbsp;</td>
                <td width="1">
                  <div class="tabletext"><span style="white-space: nowrap;"><#if shipMeth.partyId != "_NA_">${shipMeth.partyId?if_exists}&nbsp;</#if>${shipMeth.get("description",locale)?if_exists}</span></div>
                </td>
                <td><input type="radio" name="defaultShipMeth" value="${shippingMethod}" <#if profiledefs.defaultShipMeth?default("") == shippingMethod>checked</#if>></td>
              </tr>
            </#list>
          <#else>
            <div class="tabletext">${uiLabelMap.OrderDefaultShipmentMethodMsg}</div>
          </#if>
        </table>
    </div>
</div>
</form>

<#-- ============================================================= -->
<div class="screenlet">
    <div class="screenlet-header">
        <div class="boxhead">&nbsp;${uiLabelMap.EcommerceFileManager}</div>
    </div>
    <div class="screenlet-body">
        <table width="100%" border="0" cellpadding="1">
          <#if partyContent?has_content>
            <#list partyContent as contentRole>
              <#assign content = contentRole.getRelatedOne("Content")>
              <#assign contentType = content.getRelatedOneCache("ContentType")>
              <#assign mimeType = content.getRelatedOneCache("MimeType")>
              <#assign status = content.getRelatedOneCache("StatusItem")>
              <tr>
                <td><a href="<@ofbizUrl>img/${content.contentName}?imgId=${content.dataResourceId}</@ofbizUrl>" class="buttontext">${content.contentId}</a>
                <td><div class="tabletext">${content.contentName?if_exists}</div></td>
                <td><div class="tabletext">${(contentType.get("description",locale))?if_exists}</div></td>
                <td><div class="tabletext">${(mimeType.description)?if_exists}</div></td>
                <td><div class="tabletext">${(status.get("description",locale))?if_exists}</div></td>
                <td><div class="tabletext">${contentRole.fromDate?if_exists}</div></td>
                <td align="right">
                  <a href="<@ofbizUrl>img/${content.contentName}?imgId=${content.dataResourceId}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonView}</a>
                  <a href="<@ofbizUrl>removePartyAsset?contentId=${contentRole.contentId}&partyId=${contentRole.partyId}&roleTypeId=${contentRole.roleTypeId}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonRemove}</a>
                </td>
              </tr>
            </#list>
          <#else>
            <div class="tabletext">${uiLabelMap.EcommerceNoFiles}</div>
          </#if>
        </table>
        <div>&nbsp;</div>
        <div align="right" class="head3"><b><u>${uiLabelMap.EcommerceUploadNewFile}</u></b>
          <div>&nbsp;</div>
          <form method="post" enctype="multipart/form-data" action="<@ofbizUrl>createPartyAsset</@ofbizUrl>" style="margin: 0;">
            <input type="hidden" name="dataCategoryId" value="PERSONAL"/>
            <input type="hidden" name="contentTypeId" value="DOCUMENT"/>
            <input type="hidden" name="statusId" value="CTNT_PUBLISHED"/>
            <input type="file" name="uploadedFile" size="50" class="inputBox"/>
            <input type="submit" value="${uiLabelMap.CommonUpload}" class="smallSubmit"/>
          </form>
        </div>
    </div>
</div>

<#-- ============================================================= -->
<div class="screenlet">
    <div class="screenlet-header">
        <div class="boxhead">&nbsp;${uiLabelMap.PartyContactLists}</div>
    </div>
    <div class="screenlet-body">
        <table width="100%" border="0" cellpadding="1" cellspacing="0">
            <tr>
              <td width="15%" nowrap><div class="tableheadtext">${uiLabelMap.EcommerceListName}</div></td>
              <#-- <td width="15%" nowrap><div class="tableheadtext">${uiLabelMap.EcommerceListType}</div></td> -->
              <td width="15%" nowrap><div class="tableheadtext">${uiLabelMap.CommonFromDate}</div></td>
              <td width="15%" nowrap><div class="tableheadtext">${uiLabelMap.CommonThruDate}</div></td>
              <td width="15%" nowrap><div class="tableheadtext">${uiLabelMap.CommonStatus}</div></td>
              <td width="5">&nbsp;</td>
              <td width="20%" nowrap><div class="tableheadtext"><b>&nbsp;</b></div></td>
            </tr>
          <#list contactListPartyList as contactListParty>
            <#assign contactList = contactListParty.getRelatedOne("ContactList")/>
            <#assign statusItem = contactListParty.getRelatedOneCache("StatusItem")?if_exists/>
            <#-- <#assign contactListType = contactList.getRelatedOneCache("ContactListType")/> -->
            <tr><td colspan="6"><hr class="sepbar"/></td></tr>
            <tr>
              <td width="15%"><div class="tabletext"><b>${contactList.contactListName?if_exists}</b><#if contactList.description?has_content>&nbsp;-&nbsp;${contactList.description}</#if></div></td>
              <#-- <td width="15%"><div class="tabletext">${contactListType.get("description",locale)?if_exists}</div></td> -->
              <td width="15%"><div class="tabletext">${contactListParty.fromDate?if_exists}</div></td>
              <td width="15%"><div class="tabletext">${contactListParty.thruDate?if_exists}</div></td>
              <td width="15%"><div class="tabletext">${(statusItem.get("description",locale))?if_exists}</div></td>
              <td width="5">&nbsp;</td>
              <td width="20%" nowrap>
              <#if (contactListParty.statusId?if_exists == "CLPT_ACCEPTED")>
                <a href="<@ofbizUrl>updateContactListParty?partyId=${party.partyId}&amp;contactListId=${contactListParty.contactListId}&amp;fromDate=${contactListParty.fromDate}&amp;statusId=CLPT_REJECTED</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonUnsubscribe}</a>
              <#elseif (contactListParty.statusId?if_exists == "CLPT_PENDING")>
                <form method="post" action="<@ofbizUrl>updateContactListParty</@ofbizUrl>" name="clistAcceptForm${contactListParty_index}">
                  <input type="hidden" name="partyId" value="${party.partyId}"/>
                  <input type="hidden" name="contactListId" value="${contactListParty.contactListId}"/>
                  <input type="hidden" name="fromDate" value="${contactListParty.fromDate}"/>
                  <input type="hidden" name="statusId" value="CLPT_ACCEPTED"/>
                  <input type="text" size="10" name="optInVerifyCode" value="" class="inputBox"/>
                  <input type="submit" value="${uiLabelMap.CommonVerifySubscription}" class="smallSubmit"/>
                </form>
              <#elseif (contactListParty.statusId?if_exists == "CLPT_REJECTED")>
                <a href="<@ofbizUrl>updateContactListParty?partyId=${party.partyId}&amp;contactListId=${contactListParty.contactListId}&amp;fromDate=${contactListParty.fromDate}&amp;statusId=CLPT_PENDING</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonSubscribe}</a>
              </#if>
              </td>
            </tr>
          </#list>
        </table>
        <hr class="sepbar"/>
        <div>
          <form method="post" action="<@ofbizUrl>createContactListParty</@ofbizUrl>" name="clistPendingForm">
            <input type="hidden" name="partyId" value="${party.partyId}"/>
            <input type="hidden" name="statusId" value="CLPT_PENDING"/>
            <span class="tableheadtext">${uiLabelMap.CommonNewListSubscription}: </span>
            <select name="contactListId" class="selectBox">
              <#list publicContactLists as publicContactList>
                <#-- <#assign publicContactListType = publicContactList.getRelatedOneCache("ContactListType")> -->
                <#assign publicContactMechType = publicContactList.getRelatedOneCache("ContactMechType")?if_exists>
                <option value="${publicContactList.contactListId}">${publicContactList.contactListName?if_exists} <#-- ${publicContactListType.get("description",locale)} --> <#if publicContactMechType?has_content>[${publicContactMechType.get("description",locale)}]</#if></option>
              </#list>
            </select>
            <select name="preferredContactMechId" class="selectBox">
              <#-- <option></option> -->
              <#list partyAndContactMechList as partyAndContactMech>
                <option value="${partyAndContactMech.contactMechId}"><#if partyAndContactMech.infoString?has_content>${partyAndContactMech.infoString}<#elseif partyAndContactMech.tnContactNumber?has_content>${partyAndContactMech.tnCountryCode?if_exists}-${partyAndContactMech.tnAreaCode?if_exists}-${partyAndContactMech.tnContactNumber}<#elseif partyAndContactMech.paAddress1?has_content>${partyAndContactMech.paAddress1}, ${partyAndContactMech.paAddress2?if_exists}, ${partyAndContactMech.paCity?if_exists}, ${partyAndContactMech.paStateProvinceGeoId?if_exists}, ${partyAndContactMech.paPostalCode?if_exists}, ${partyAndContactMech.paPostalCodeExt?if_exists} ${partyAndContactMech.paCountryGeoId?if_exists}</#if></option>
              </#list>
            </select>
            <input type="submit" value="${uiLabelMap.CommonSubscribe}" class="smallSubmit"/>
          </form>
        </div>
        <div class="tabletext">
        ${uiLabelMap.EcommerceListNote}
        </div>
    </div>
</div>

<#-- ============================================================= -->
<#if surveys?has_content>
<div class="screenlet">
    <div class="screenlet-header">
        <div class="boxhead">&nbsp;${uiLabelMap.EcommerceSurveys}</div>
    </div>
    <div class="screenlet-body">
        <table width="100%" border="0" cellpadding="1">
          <#list surveys as surveyAppl>
            <#assign survey = surveyAppl.getRelatedOne("Survey")>
            <tr>
              <td>&nbsp;</td>
              <td align="left" valign="top" width="10%" nowrap><div class="tabletext"><b>${survey.surveyName?if_exists}</b>&nbsp;-&nbsp;${survey.description?if_exists}</div></td>
              <td width="5">&nbsp;</td>
              <td align="left" valign="top" width="70%">
                <#assign responses = Static["org.ofbiz.product.store.ProductStoreWorker"].checkSurveyResponse(request, survey.surveyId)?default(0)>
                <div class="tabletext"><#if (responses < 1)><font color="red"><b>${uiLabelMap.EcommerceNotCompleted}</b><#else>${uiLabelMap.EcommerceCompleted}</#if></div>
              </td>
              <#if (responses == 0 || survey.allowMultiple?default("N") == "Y")>
                <#assign surveyLabel = uiLabelMap.EcommerceTakeSurvey>
                <#if (responses > 0 && survey.allowUpdate?default("N") == "Y")>
                  <#assign surveyLabel = uiLabelMap.EcommerceUpdateSurvey>
                </#if>
                <td align="right" width="10%" nowrap><a href="<@ofbizUrl>takesurvey?productStoreSurveyId=${surveyAppl.productStoreSurveyId}</@ofbizUrl>" class="buttontext">${surveyLabel}</a></td>
              <#else>
                &nbsp;
              </#if>
            </tr>
          </#list>
        </table>
    </div>
</div>
</#if>

<#-- ============================================================= -->
<#-- only 5 messages will show; edit the viewprofile.bsh to change this number -->
${screens.render("component://ecommerce/widget/CustomerScreens.xml#messagelist-include")}

${screens.render("component://ecommerce/widget/CustomerScreens.xml#FinAccountList-include")}

<#else>
    <div class="head3">${uiLabelMap.PartyNoPartyForCurrentUserName}: ${userLogin.userLoginId}</div>
</#if>
