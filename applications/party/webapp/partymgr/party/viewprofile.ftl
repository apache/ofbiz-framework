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

<#if party?has_content>
<div style="text-align: right;">
  <#if showOld>
    <a href="<@ofbizUrl>viewprofile?partyId=${party.partyId}</@ofbizUrl>" class="buttontext">${uiLabelMap.PartyHideOld}</a>
  <#else>
    <a href="<@ofbizUrl>viewprofile?partyId=${party.partyId}&SHOW_OLD=true</@ofbizUrl>" class="buttontext">${uiLabelMap.PartyShowOld}</a>
  </#if>
</div>
<br/>

<div class="screenlet">
    <div class="screenlet-header">
        <#if lookupPerson?has_content>
            <div style="float: right;">
            <#if security.hasEntityPermission("PARTYMGR", "_UPDATE", session)>
              <a href="<@ofbizUrl>editperson?partyId=${party.partyId}</@ofbizUrl>" class="submenutextright">
              <#if lookupPerson?has_content>${uiLabelMap.CommonUpdate}</#if></a>
            </#if>
            </div>
            <div class="boxhead">&nbsp;${uiLabelMap.PartyPersonalInformation}</div>
        </#if>
        <#if lookupGroup?has_content>
            <#assign lookupPartyType = party.getRelatedOneCache("PartyType")>
            <div style="float: right;">
            <#if security.hasEntityPermission("PARTYMGR", "_UPDATE", session)>
              <a href="<@ofbizUrl>editpartygroup?partyId=${party.partyId}</@ofbizUrl>" class="submenutextright">
              <#if lookupGroup?has_content>${uiLabelMap.CommonUpdate}</#if></a>
            </#if>
            </div>
            <div class="boxhead">&nbsp;${uiLabelMap.PartyPartyGroupInformation}</div>
        </#if>
    </div>
    <div class="screenlet-body">
<#if lookupPerson?has_content>
  <table width="100%" border="0" cellpadding="0" cellspacing="0">
    <tr>
      <td align="right" width="10%"><div class="tabletext"><b>${uiLabelMap.PartyName}</b></div></td>
      <td width="5">&nbsp;</td>
      <td align="left" width="90%">
        <div class="tabletext">
          ${lookupPerson.personalTitle?if_exists}
          ${lookupPerson.firstName?if_exists}
          ${lookupPerson.middleName?if_exists}
          ${lookupPerson.lastName?if_exists}
          ${lookupPerson.suffix?if_exists}
        </div>
      </td>
    </tr>
    <#if lookupPerson.nickname?has_content>
    <tr><td align="right" nowrap><div class="tabletext"><b>${uiLabelMap.PartyNickname}</b></div></td><td>&nbsp;</td><td align="left"><div class="tabletext">${lookupPerson.nickname}</div></td></tr>
    </#if>
    <#if lookupPerson.gender?has_content>
    <tr><td align="right" nowrap><div class="tabletext"><b>${uiLabelMap.PartyGender}</b></div></td><td>&nbsp;</td><td align="left"><div class="tabletext">${lookupPerson.gender}</div></td></tr>
    </#if>
    <#if lookupPerson.birthDate?has_content>
    <tr><td align="right" nowrap><div class="tabletext"><b>${uiLabelMap.PartyBirthDate}</b></div></td><td>&nbsp;</td><td align="left"><div class="tabletext">${lookupPerson.birthDate.toString()}</div></td></tr>
    </#if>
    <#if lookupPerson.height?has_content>
    <tr><td align="right" nowrap><div class="tabletext"><b>${uiLabelMap.PartyHeight}</b></div></td><td>&nbsp;</td><td align="left"><div class="tabletext">${lookupPerson.height}</div></td></tr>
    </#if>
    <#if lookupPerson.weight?has_content>
    <tr><td align="right" nowrap><div class="tabletext"><b>${uiLabelMap.PartyWeight}</b></div></td><td>&nbsp;</td><td align="left"><div class="tabletext">${lookupPerson.weight}</div></td></tr>
    </#if>
    <#if lookupPerson.mothersMaidenName?has_content>
    <tr><td align="right" nowrap><div class="tabletext"><b>${uiLabelMap.PartyMothersMaidenName}</b></div></td><td>&nbsp;</td><td align="left"><div class="tabletext">${lookupPerson.mothersMaidenName}</div></td></tr>
    </#if>
    <#if lookupPerson.maritalStatus?has_content>
    <tr><td align="right" nowrap><div class="tabletext"><b>${uiLabelMap.PartyMaritalStatus}</b></div></td><td>&nbsp;</td><td align="left"><div class="tabletext">${lookupPerson.maritalStatus}</div></td></tr>
    </#if>
    <#if lookupPerson.socialSecurityNumber?has_content>
    <tr><td align="right" nowrap><div class="tabletext"><b>${uiLabelMap.PartySocialSecurityNumber}</b></div></td><td>&nbsp;</td><td align="left"><div class="tabletext">${lookupPerson.socialSecurityNumber}</div></td></tr>
    </#if>
    <#if lookupPerson.passportNumber?has_content>
    <tr><td align="right" nowrap><div class="tabletext"><b>${uiLabelMap.PartyPassportNumber}</b></div></td><td>&nbsp;</td><td align="left"><div class="tabletext">${lookupPerson.passportNumber}</div></td></tr>
    </#if>
    <#if lookupPerson.passportExpireDate?has_content>
    <tr><td align="right" nowrap><div class="tabletext"><b>${uiLabelMap.PartyPassportExpire}</b></div></td><td>&nbsp;</td><td align="left"><div class="tabletext">${lookupPerson.passportExpireDate.toString()}</div></td></tr>
    </#if>
    <#if lookupPerson.totalYearsWorkExperience?has_content>
    <tr><td align="right" nowrap><div class="tabletext"><b>${uiLabelMap.PartyYearsWork}</b></div></td><td>&nbsp;</td><td align="left"><div class="tabletext">${lookupPerson.totalYearsWorkExperience}</div></td></tr>
    </#if>
    <#if lookupPerson.comments?has_content>
    <tr><td align="right" nowrap><div class="tabletext"><b>${uiLabelMap.PartyComments}</b></div></td><td>&nbsp;</td><td align="left"><div class="tabletext">${lookupPerson.comments}</div></td></tr>
    </#if>
  </table>
<#elseif lookupGroup?has_content>
    <div class="tabletext">${lookupGroup.groupName} (${(lookupPartyType.get("description",locale))?if_exists})</div>
<#else>
    <div class="tabletext">${uiLabelMap.PartyInformationNotFound}</div>
</#if>
    <#if partyNameHistoryList?has_content>
        <div><hr class="sepbar"/></div>
        <div class="tableheadtext">${uiLabelMap.PartyHistoryName}</div>
        <#list partyNameHistoryList as partyNameHistory>
            <#if lookupPerson?has_content>
                <div class="tabletext">${uiLabelMap.PartyHistoryWas}: ${partyNameHistory.personalTitle?if_exists} ${partyNameHistory.firstName?if_exists} ${partyNameHistory.middleName?if_exists} ${partyNameHistory.lastName?if_exists} ${partyNameHistory.suffix?if_exists} (${uiLabelMap.PartyHistoryChanged}: ${partyNameHistory.changeDate})</div>
            <#elseif lookupGroup?has_content>
                <div class="tabletext">${uiLabelMap.PartyHistoryWas}: ${partyNameHistory.groupName?if_exists} (${uiLabelMap.PartyHistoryChanged}: ${partyNameHistory.changeDate})</div>
            </#if>
        </#list>
    </#if>
    </div>
</div>

<#-- ============================================================= -->
<#if monthsToInclude?exists && totalSubRemainingAmount?exists && totalOrders?exists>
<div class="screenlet">
    <div class="screenlet-header">
        <div class="boxhead">&nbsp;${uiLabelMap.PartyLoyaltyPoints}</div>
    </div>
    <div class="screenlet-body">
        <div class="tabletext">${uiLabelMap.PartyYouHave} ${totalSubRemainingAmount} ${uiLabelMap.PartyPointsFrom} ${totalOrders} ${uiLabelMap.PartyOrderInLast} ${monthsToInclude} ${uiLabelMap.CommonMonths}.</div>
    </div>
</div>
</#if>

<#-- ============================================================= -->
<div class="screenlet">
    <div class="screenlet-header">
        <div style="float: right;">
          <#if security.hasEntityPermission("PARTYMGR", "_CREATE", session)>
              <a href="<@ofbizUrl>editcontactmech?partyId=${party.partyId}</@ofbizUrl>" class="submenutextright">${uiLabelMap.CommonCreateNew}</a>
          </#if>
        </div>
        <div class="boxhead">&nbsp;${uiLabelMap.PartyContactInformation}</div>
    </div>
    <div class="screenlet-body">
  <#if contactMeches?has_content>
    <table width="100%" border="0" cellpadding="0">
      <tr align="left" valign=bottom>
        <th><div class="tableheadtext">${uiLabelMap.PartyContactType}</th>
        <th width="5">&nbsp;</th>
        <th><div class="tableheadtext">${uiLabelMap.PartyContactInformation}</th>
        <th colspan="2"><div class="tableheadtext">${uiLabelMap.PartyContactSolicitingOk}</th>
        <th>&nbsp;</th>
      </tr>
      <#list contactMeches as contactMechMap>
          <#assign contactMech = contactMechMap.contactMech>
          <#assign partyContactMech = contactMechMap.partyContactMech>
          <tr><td colspan="7"><hr class="sepbar"></td></tr>
          <tr>
            <td align="right" valign="top" width="10%">
              <div class="tabletext">&nbsp;<b>${contactMechMap.contactMechType.get("description",locale)}</b></div>
            </td>
            <td width="5">&nbsp;</td>
            <td align="left" valign="top" width="80%">
              <#list contactMechMap.partyContactMechPurposes as partyContactMechPurpose>
                  <#assign contactMechPurposeType = partyContactMechPurpose.getRelatedOneCache("ContactMechPurposeType")>
                    <div class="tabletext">
                      <#if contactMechPurposeType?has_content>
                        <b>${contactMechPurposeType.get("description",locale)}</b>
                      <#else>
                        <b>${uiLabelMap.PartyMechPurposeTypeNotFound}: "${partyContactMechPurpose.contactMechPurposeTypeId}"</b>
                      </#if>
                      <#if partyContactMechPurpose.thruDate?has_content>
                      (${uiLabelMap.CommonExpire}: ${partyContactMechPurpose.thruDate.toString()})
                      </#if>
                    </div>
              </#list>
              <#if "POSTAL_ADDRESS" = contactMech.contactMechTypeId>
                  <#assign postalAddress = contactMechMap.postalAddress>
                  <div class="tabletext">
                    <#if postalAddress.toName?has_content><b>${uiLabelMap.PartyAddrToName}:</b> ${postalAddress.toName}<br/></#if>
                    <#if postalAddress.attnName?has_content><b>${uiLabelMap.PartyAddrAttnName}:</b> ${postalAddress.attnName}<br/></#if>
                    ${postalAddress.address1?if_exists}<br/>
                    <#if postalAddress.address2?has_content>${postalAddress.address2}<br/></#if>
                    ${postalAddress.city?if_exists},
                    ${postalAddress.stateProvinceGeoId?if_exists}
                    ${postalAddress.postalCode?if_exists}
                    <#if postalAddress.countryGeoId?has_content><br/>${postalAddress.countryGeoId}</#if>
                  </div>
                  <#if (postalAddress?has_content && !postalAddress.countryGeoId?has_content) || postalAddress.countryGeoId = "USA">
                      <#assign addr1 = postalAddress.address1?if_exists>
                      <#if (addr1.indexOf(" ") > 0)>
                        <#assign addressNum = addr1.substring(0, addr1.indexOf(" "))>
                        <#assign addressOther = addr1.substring(addr1.indexOf(" ")+1)>
                        <a target="_blank" href="http://www.whitepages.com/find_person_results.pl?fid=a&s_n=${addressNum}&s_a=${addressOther}&c=${postalAddress.city?if_exists}&s=${postalAddress.stateProvinceGeoId?if_exists}&x=29&y=18" class="linktext">(lookup:whitepages.com)</a>
                      </#if>
                  </#if>
              <#elseif "TELECOM_NUMBER" = contactMech.contactMechTypeId>
                  <#assign telecomNumber = contactMechMap.telecomNumber>
                  <div class="tabletext">
                    ${telecomNumber.countryCode?if_exists}
                    <#if telecomNumber.areaCode?has_content>${telecomNumber.areaCode?default("000")}-</#if>${telecomNumber.contactNumber?default("000-0000")}
                    <#if partyContactMech.extension?has_content>${uiLabelMap.PartyContactExt}&nbsp;${partyContactMech.extension}</#if>
                    <#if (telecomNumber?has_content && !telecomNumber.countryCode?has_content) || telecomNumber.countryCode = "011">
                      <a target="_blank" href="http://www.anywho.com/qry/wp_rl?npa=${telecomNumber.areaCode?if_exists}&telephone=${telecomNumber.contactNumber?if_exists}&btnsubmit.x=20&btnsubmit.y=8" class="linktext">(lookup:anywho.com)</a>
                      <a target="_blank" href="http://whitepages.com/find_person_results.pl?fid=p&ac=${telecomNumber.areaCode?if_exists}&s=&p=${telecomNumber.contactNumber?if_exists}&pt=b&x=40&y=9" class="linktext">(lookup:whitepages.com)</a>
                    </#if>
                  </div>
              <#elseif "EMAIL_ADDRESS" = contactMech.contactMechTypeId>
                  <div class="tabletext">
                    ${contactMech.infoString?if_exists}
                    <a href="<@ofbizUrl>EditCommunicationEvent?partyIdFrom=${userLogin.partyId}&partyId=${party.partyId}&communicationEventTypeId=EMAIL_COMMUNICATION&contactMechIdTo=${contactMech.contactMechId}&contactMechTypeId=EMAIL_ADDRESS<#if thisUserPrimaryEmail?has_content>&contactMechIdFrom=${thisUserPrimaryEmail.contactMechId}</#if></@ofbizUrl>" class="linktext">(${uiLabelMap.CommonSendEmail})</a>
                  </div>
              <#elseif "WEB_ADDRESS" = contactMech.contactMechTypeId>
                  <div class="tabletext">
                    ${contactMech.infoString?if_exists}
                    <#assign openAddress = contactMech.infoString?default("")>
                    <#if !openAddress?starts_with("http") && !openAddress?starts_with("HTTP")><#assign openAddress = "http://" + openAddress></#if>
                    <a target="_blank" href="${openAddress}" class="linktext">(${uiLabelMap.CommonOpenPageNewWindow})</a>
                  </div>
              <#else>
                  <div class="tabletext">
                    ${contactMech.infoString?if_exists}
                  </div>
              </#if>
              <div class="tabletext">(${uiLabelMap.CommonUpdated}:&nbsp;${partyContactMech.fromDate.toString()})</div>
              <#if partyContactMech.thruDate?has_content><div class="tabletext"><b>${uiLabelMap.PartyContactEffectiveThru}:&nbsp;${partyContactMech.thruDate.toString()}</b></div></#if>
            </td>
            <td align="center" valign="top" nowrap width="1%"><div class="tabletext"><b>(${partyContactMech.allowSolicitation?if_exists})</b></div></td>
            <td width="5">&nbsp;</td>
            <#if security.hasEntityPermission("PARTYMGR", "_UPDATE", session)>
            <td align="right" valign="top" nowrap width="1%">
              <div><a href="<@ofbizUrl>editcontactmech?partyId=${party.partyId}&contactMechId=${contactMech.contactMechId}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonUpdate}</a>&nbsp;</div>
            </td>
            </#if>
            <#if security.hasEntityPermission("PARTYMGR", "_DELETE", session)>
            <td align="right" valign="top" width="1%">
              <div><a href="<@ofbizUrl>deleteContactMech/viewprofile?partyId=${party.partyId}&contactMechId=${contactMech.contactMechId}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonExpire}</a>&nbsp;&nbsp;</div>
            </td>
            </#if>
          </tr>
      </#list>
    </table>
  <#else>
    <div class="tabletext">${uiLabelMap.PartyNoContactInformation}</div>
  </#if>
    </div>
</div>

<#-- Payment Info ============================================================= -->
<div class="screenlet">
    <div class="screenlet-header">
        <div style="float: right;">
          <#if security.hasEntityPermission("PAY_INFO", "_CREATE", session)>
            <a href="<@ofbizUrl>editcreditcard?partyId=${party.partyId}</@ofbizUrl>" class="submenutext">${uiLabelMap.AccountingCreateNewCreditCard}</a>
            <a href="<@ofbizUrl>editgiftcard?partyId=${party.partyId}</@ofbizUrl>" class="submenutext">${uiLabelMap.AccountingCreateNewGiftCard}</a>
            <a href="<@ofbizUrl>editeftaccount?partyId=${party.partyId}</@ofbizUrl>" class="submenutextright">${uiLabelMap.AccountingCreateNewEftAccount}</a>
          </#if>
        </div>
        <div class="boxhead">&nbsp;${uiLabelMap.PartyPaymentMethodInformation}</div>
    </div>
    <div class="screenlet-body">
        <#if paymentMethodValueMaps?has_content>
          <table width="100%" border="0" cellpadding="1">
            <tr>
              <td align="left">
                  <table width="100%" cellpadding="2" cellspacing="0" border="0">
                    <#list paymentMethodValueMaps as paymentMethodValueMap>
                        <#assign paymentMethod = paymentMethodValueMap.paymentMethod/>
                        <tr>
                          <#if "CREDIT_CARD" == paymentMethod.paymentMethodTypeId>
                              <#assign creditCard = paymentMethodValueMap.creditCard/>
                              <td width="90%" valign="top">
                                <div class="tabletext">
                                  <b>
                                    ${uiLabelMap.AccountingCreditCard}:&nbsp;
                                    <#if creditCard.companyNameOnCard?has_content>${creditCard.companyNameOnCard}&nbsp;</#if>
                                    <#if creditCard.titleOnCard?has_content>${creditCard.titleOnCard}&nbsp</#if>
                                    ${creditCard.firstNameOnCard}&nbsp;
                                    <#if creditCard.middleNameOnCard?has_content>${creditCard.middleNameOnCard}&nbsp</#if>
                                    ${creditCard.lastNameOnCard}
                                    <#if creditCard.suffixOnCard?has_content>&nbsp;${creditCard.suffixOnCard}</#if>
                                    &nbsp;-&nbsp;
                                    <#if security.hasEntityPermission("PAY_INFO", "_VIEW", session)>
                                        ${creditCard.cardType}
                                        ${creditCard.cardNumber}
                                        ${creditCard.expireDate}
                                    <#else>
                                        ${Static["org.ofbiz.party.contact.ContactHelper"].formatCreditCard(creditCard)}
                                    </#if>
                                  </b>
                                  <#if paymentMethod.description?has_content>(${paymentMethod.description})</#if>
                                  <#if paymentMethod.glAccountId?has_content>(for GL Account ${paymentMethod.glAccountId})</#if>
                                  <#if paymentMethod.fromDate?has_content>(${uiLabelMap.CommonUpdated}:&nbsp;${paymentMethod.fromDate?if_exists})</#if>
                                  <#if paymentMethod.thruDate?has_content><b>(${uiLabelMap.PartyContactEffectiveThru}:&nbsp;${paymentMethod.thruDate})</b></#if>
                                </div>
                              </td>
                              <td width="5">&nbsp;</td>
                              <td align="right" valign="top" width="1%" nowrap>
                                <div>
                                <#if security.hasEntityPermission("MANUAL", "_PAYMENT", session)>
                                  <a href="/accounting/control/manualETx?paymentMethodId=${paymentMethod.paymentMethodId}${externalKeyParam}" class="buttontext">Manual Tx</a>
                                </#if>
                                <#if security.hasEntityPermission("PAY_INFO", "_UPDATE", session)>
                                    <a href="<@ofbizUrl>editcreditcard?partyId=${party.partyId}&paymentMethodId=${paymentMethod.paymentMethodId}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonUpdate}</a>
                                </#if>
                                </div>
                              </td>
                          <#elseif "GIFT_CARD" == paymentMethod.paymentMethodTypeId>
                              <#assign giftCard = paymentMethodValueMap.giftCard>
                              <td width="90%" valign="top">
                                <div class="tabletext">
                                  <b>
                                    ${uiLabelMap.AccountingGiftCard}:
                                    <#if security.hasEntityPermission("PAY_INFO", "_VIEW", session)>
                                        ${giftCard.cardNumber?default("N/A")} [${giftCard.pinNumber?default("N/A")}]
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
                                    </#if>
                                  </b>
                                  <#if paymentMethod.description?has_content>(${paymentMethod.description})</#if>
                                  <#if paymentMethod.glAccountId?has_content>(for GL Account ${paymentMethod.glAccountId})</#if>
                                  <#if paymentMethod.fromDate?has_content>(${uiLabelMap.CommonUpdated}:&nbsp;${paymentMethod.fromDate?if_exists})</#if>
                                  <#if paymentMethod.thruDate?has_content><b>(${uiLabelMap.PartyContactEffectiveThru}:&nbsp;${paymentMethod.thruDate.toString()}</b></#if>
                                </div>
                              </td>
                              <td width="5">&nbsp;</td>
                              <td align="right" valign="top" width="1%" nowrap>
                                <#if security.hasEntityPermission("PAY_INFO", "_UPDATE", session)>
                                    <div><a href="<@ofbizUrl>editgiftcard?partyId=${party.partyId}&paymentMethodId=${paymentMethod.paymentMethodId}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonUpdate}</a></div>
                                </#if>
                              </td>
                          <#elseif "EFT_ACCOUNT" == paymentMethod.paymentMethodTypeId>
                              <#assign eftAccount = paymentMethodValueMap.eftAccount>
                              <td width="90%" valign="top">
                                <div class="tabletext">
                                  <b>
                                    ${uiLabelMap.PartyEftAccount}: ${eftAccount.nameOnAccount} - <#if eftAccount.bankName?has_content>${uiLabelMap.PartyBank}: ${eftAccount.bankName}</#if> <#if eftAccount.accountNumber?has_content>${uiLabelMap.PartyAccount} #: ${eftAccount.accountNumber}</#if>
                                  </b>
                                  <#if paymentMethod.description?has_content>(${paymentMethod.description})</#if>
                                  <#if paymentMethod.glAccountId?has_content>(for GL Account ${paymentMethod.glAccountId})</#if>
                                  <#if paymentMethod.fromDate?has_content>(${uiLabelMap.CommonUpdated}:&nbsp;${paymentMethod.fromDate?if_exists})</#if>
                                  <#if paymentMethod.thruDate?has_content><b>(${uiLabelMap.PartyContactEffectiveThru}:&nbsp;${paymentMethod.thruDate.toString()}</b></#if>
                                </div>
                              </td>
                              <td width="5">&nbsp;</td>
                              <td align="right" valign="top" width="1%" nowrap>
                                <#if security.hasEntityPermission("PAY_INFO", "_UPDATE", session)>
                                    <div><a href="<@ofbizUrl>editeftaccount?partyId=${party.partyId}&paymentMethodId=${paymentMethod.paymentMethodId}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonUpdate}</a></div>
                                </#if>
                              </td>
                          <#elseif "COMPANY_CHECK" == paymentMethod.paymentMethodTypeId>
                              <td width="90%" valign="top">
                                <div class="tabletext">
                                  <b>Company Check</b> 
                                  <#if paymentMethod.description?has_content>(${paymentMethod.description})</#if>
                                  <#if paymentMethod.glAccountId?has_content>(for GL Account ${paymentMethod.glAccountId})</#if>
                                  <#if paymentMethod.fromDate?has_content>(${uiLabelMap.CommonUpdated}:&nbsp;${paymentMethod.fromDate?if_exists})</#if>
                                  <#if paymentMethod.thruDate?has_content><b>(${uiLabelMap.PartyContactEffectiveThru}:&nbsp;${paymentMethod.thruDate.toString()}</b></#if>
                                </div>
                              </td>
                              <td width="5">&nbsp;</td>
                              <td align="right" valign="top" width="1%" nowrap>&nbsp;</td>
                          </#if>
                          <td align="right" valign="top" width="1%">
                            <#if security.hasEntityPermission("PAY_INFO", "_DELETE", session)>
                                <div><a href="<@ofbizUrl>deletePaymentMethod/viewprofile?partyId=${party.partyId}&paymentMethodId=${paymentMethod.paymentMethodId}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonExpire}</a></div>
                            </#if>
                          </td>
                        </tr>
                    </#list>
                  </table>
              </td>
            </tr>
          </table>
        <#else>
            <div class="tabletext">${uiLabelMap.PartyNoPaymentMethodInformation}</div>
        </#if>
    </div>
</div>

<#-- AVS Strings -->
<div class="screenlet">
    <div class="screenlet-header">
        <div class="boxhead">&nbsp;${uiLabelMap.PartyCybersourceAvsOver}</div>
    </div>
    <div class="screenlet-body">
        <div class="tabletext">
            <b>${uiLabelMap.PartyAvsString}:</b>&nbsp;${(avsOverride.avsDeclineString)?default("${uiLabelMap.CommonGlobal}")}
            <#if security.hasEntityPermission("PARTYMGR", "_UPDATE", session)>
                <a href="<@ofbizUrl>editAvsOverride?partyId=${party.partyId}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonEdit}</a>
                <#if avsOverride?exists>
                    <a href="<@ofbizUrl>resetAvsOverride?partyId=${party.partyId}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonReset}</a>
                </#if>
            </#if>
        </div>
    </div>
</div>

<#-- UserLogins -->
<div class="screenlet">
    <div class="screenlet-header">
        <div style="float: right;">
          <#if security.hasEntityPermission("PARTYMGR", "_CREATE", session)>
            <a href="<@ofbizUrl>createnewlogin?partyId=${party.partyId}</@ofbizUrl>" class="submenutextright">${uiLabelMap.CommonCreateNew}</a>
          </#if>
        </div>
        <div class="boxhead">&nbsp;${uiLabelMap.PartyUserName}</div>
    </div>
    <div class="screenlet-body">
        <#if userLogins?exists>
        <table width="100%" border="0" cellpadding="1">
          <#list userLogins as userUserLogin>
          <tr>
            <td align="right" valign="top" width="10%" nowrap><div class="tabletext"><b>${uiLabelMap.PartyUserLogin}</b></div></td>
            <td width="5">&nbsp;</td>
            <td align="left" valign="top" width="40%"><div class="tabletext">${userUserLogin.userLoginId}</div></td>
            <td align="left" valign="top" width="30%">
              <div class="tabletext">
                <#assign enabled = uiLabelMap.PartyEnabled>
                <#if (userUserLogin.enabled)?default("Y") == "N">
                  <#if userUserLogin.disabledDateTime?exists>
                    <#assign disabledTime = userUserLogin.disabledDateTime.toString()>
                  <#else>
                    <#assign disabledTime = "??">
                  </#if>
                  <#assign enabled = uiLabelMap.PartyDisabled + " - " + disabledTime>
                </#if>
                ${enabled}
              </div>
            </td>
            <td align="right" valign="top" width="20%">
              <#if security.hasEntityPermission("PARTYMGR", "_CREATE", session)>
                  <a href="<@ofbizUrl>editlogin?partyId=${party.partyId}&userLoginId=${userUserLogin.userLoginId}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonEdit}</a>&nbsp;
              </#if>
              <#if security.hasEntityPermission("SECURITY", "_VIEW", session)>
                  <a href="<@ofbizUrl>EditUserLoginSecurityGroups?partyId=${party.partyId}&userLoginId=${userUserLogin.userLoginId}</@ofbizUrl>" class="buttontext">${uiLabelMap.PartySecurityGroups}</a>&nbsp;
              </#if>
            </td>
          </tr>
          </#list>
        </table>
        <#else>
          <div class="tabletext">${uiLabelMap.PartyNoUserLogin}</div>
        </#if>
    </div>
</div>

<#-- Party Attributes -->
<div class="screenlet">
    <div class="screenlet-header">
        <div style="float: right;">
          <#if security.hasEntityPermission("PARTYMGR", "_CREATE", session)>
            <a href="<@ofbizUrl>editPartyAttribute?partyId=${party.partyId}</@ofbizUrl>" class="submenutextright">${uiLabelMap.CommonCreateNew}</a>
          </#if>
        </div>
        <div class="boxhead">&nbsp;${uiLabelMap.PartyAttributes}</div>
    </div>
    <div class="screenlet-body">
        <#if attributes?has_content>
            <table width="100%" border="0" cellpadding="1">
              <#list attributes as attr>
                <tr>
                  <td nowrap align="left" valign="top" width="1%">
                    <div class="tabletext">&nbsp;<b>${uiLabelMap.CommonName}: </b>${attr.attrName}</div>
                  </td>
                  <td align="left" valign="top" width="98%">
                    <div class="tabletext">&nbsp;<b>${uiLabelMap.CommonValue}: </b>${attr.attrValue}</div>
                  </td>
                  <td align="right" valign="top" width="1%">
                    <a href="<@ofbizUrl>editPartyAttribute?partyId=${partyId}&attrName=${attr.attrName}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonEdit}</a>
                  </td>
                </tr>
              </#list>
            </table>
        <#else>
            <div class="tabletext">${uiLabelMap.PartyNoPartyAttributesFound}</div>
        </#if>
    </div>
</div>

<#-- Visits -->
<div class="screenlet">
    <div class="screenlet-header">
        <div style="float: right;">
            <a href="<@ofbizUrl>showvisits?partyId=${partyId}</@ofbizUrl>" class="submenutextright">${uiLabelMap.CommonListAll}</a>
        </div>
        <div class="boxhead">&nbsp;${uiLabelMap.PartyLastVisit}</div>
    </div>
    <div class="screenlet-body">
        <#if visits?exists>
        <table width="100%" border="0" cellpadding="2" cellspacing="0">
          <tr>
            <td><div class="tableheadtext">${uiLabelMap.PartyVisitId}</div></td>
            <td><div class="tableheadtext">${uiLabelMap.PartyUserLogin}</div></td>
            <td><div class="tableheadtext">${uiLabelMap.PartyNewUser}</div></td>
            <td><div class="tableheadtext">${uiLabelMap.PartyWebApp}</div></td>
            <td><div class="tableheadtext">${uiLabelMap.PartyClientIP}</div></td>
            <td><div class="tableheadtext">${uiLabelMap.CommonFromDate}</div></td>
            <td><div class="tableheadtext">${uiLabelMap.CommonThruDate}</div></td>
          </tr>
          <tr>
            <td colspan="7"><hr class="sepbar"></td>
          </tr>
          <#list visits as visitObj>
          <#if (visitObj_index > 4)><#break></#if>
          <tr>
            <td><a href="<@ofbizUrl>visitdetail?visitId=${visitObj.visitId?if_exists}</@ofbizUrl>" class="buttontext">${visitObj.visitId?if_exists}</a></td>
            <td><div class="tabletext">${visitObj.userLoginId?if_exists}</div></td>
            <td><div class="tabletext">${visitObj.userCreated?if_exists}</div></td>
            <td><div class="tabletext">${visitObj.webappName?if_exists}</div></td>
            <td><div class="tabletext">${visitObj.clientIpAddress?if_exists}</div></td>
            <td><div class="tabletext">${(visitObj.fromDate.toString())?if_exists}</div></td>
            <td><div class="tabletext">${(visitObj.thruDate.toString())?if_exists}</div></td>
          </tr>
          </#list>
        </table>
        <#else>
          <div class="tabletext">${uiLabelMap.PartyNoVisitFound}</div>
        </#if>
    </div>
</div>

<#-- Current Cart -->
<#if isCustomer?exists>
<div class="screenlet">
    <div class="screenlet-header">
        <div style="float: right;">
          <#if security.hasEntityPermission("PARTYMGR", "_UPDATE", session)>
              <#if savedCartListId?has_content>
                <#assign listParam = "&shoppingListId=" + savedCartListId>
              <#else>
                <#assign listParam = "">
              </#if>
              <a href="<@ofbizUrl>editShoppingList?partyId=${partyId}${listParam}</@ofbizUrl>" class="submenutextright">${uiLabelMap.CommonEdit}</a>
          </#if>
        </div>
        <div class="boxhead">&nbsp;${uiLabelMap.PartyCurrentShoppingCart}</div>
    </div>
    <div class="screenlet-body">
        <#if savedCartItems?has_content>
          <table width="100%" border="0" cellpadding="2" cellspacing="0">
            <tr>
              <td><div class="tableheadtext">${uiLabelMap.PartySequenceId}</div></td>
              <td><div class="tableheadtext">${uiLabelMap.PartyProductId}</div></td>
              <td><div class="tableheadtext">${uiLabelMap.PartyQuantity}</div></td>
              <td><div class="tableheadtext">${uiLabelMap.PartyQuantityPurchased}</div></td>
              <td>&nbsp;</td>
              <td>&nbsp;</td>
            </tr>
            <tr>
              <td colspan="7"><hr class="sepbar"></td>
            </tr>
            <#list savedCartItems as savedCartItem>
              <tr>
                <td><div class="tabletext">${savedCartItem.shoppingListItemSeqId?if_exists}</div></td>
                <td><a href="/catalog/control/EditProduct?productId=${savedCartItem.productId}&externalLoginKey=${requestAttributes.externalLoginKey}" class="buttontext">${savedCartItem.productId?if_exists}</a</td>
                <td><div class="tabletext">${savedCartItem.quantity?if_exists}</div></td>
                <td><div class="tabletext">${savedCartItem.quantityPurchased?if_exists}</div></td>
              </tr>
            </#list>
          </table>
        <#else>
          <div class="tabletext">${uiLabelMap.PartyNoShoppingCartSavedForParty}</div>
        </#if>
    </div>
</div>
</#if>

<#-- Party Notes -->
<div class="screenlet">
    <div class="screenlet-header">
        <div style="float: right;">
          <#if security.hasEntityPermission("PARTYMGR", "_NOTE", session)>
            <a href="<@ofbizUrl>AddPartyNote?partyId=${partyId}</@ofbizUrl>" class="submenutextright">${uiLabelMap.CommonCreateNew}</a>
          </#if>
        </div>
        <div class="boxhead">&nbsp;${uiLabelMap.CommonNotes}</div>
    </div>
    <div class="screenlet-body">
        <#if notes?has_content>
        <table width="100%" border="0" cellpadding="1">
          <#list notes as noteRef>
            <tr>
              <td align="left" valign="top" width="35%">
                <div class="tabletext">&nbsp;<b>${uiLabelMap.CommonBy}: </b>${Static["org.ofbiz.party.party.PartyHelper"].getPartyName(delegator, noteRef.noteParty, true)}</div>
                <div class="tabletext">&nbsp;<b>${uiLabelMap.CommonAt}: </b>${noteRef.noteDateTime.toString()}</div>
              </td>
              <td align="left" valign="top" width="65%">
                <div class="tabletext">${noteRef.noteInfo}</div>
              </td>
            </tr>
            <#if noteRef_has_next>
              <tr><td colspan="2"><hr class="sepbar"></td></tr>
            </#if>
          </#list>
        </table>
        <#else>
          <div class="tabletext">${uiLabelMap.PartyNoNotesForParty}</div>
        </#if>
    </div>
</div>

<#else>
    ${uiLabelMap.PartyNoPartyFoundWithPartyId}: ${partyId?if_exists}
</#if>
