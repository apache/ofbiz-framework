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

<!-- begin viewProfile.ftl -->
<#if party?has_content>
  <div class="align-float">
    <#if showOld>
      <a href="<@ofbizUrl>viewprofile?partyId=${party.partyId}</@ofbizUrl>" class="smallSubmit">${uiLabelMap.PartyHideOld}</a>
    <#else>
      <a href="<@ofbizUrl>viewprofile?partyId=${party.partyId}&SHOW_OLD=true</@ofbizUrl>" class="smallSubmit">${uiLabelMap.PartyShowOld}</a>
    </#if>
  </div>
  <br class="clear" />
  <br/>
  <div id="partyInformation" class="screenlet">
    <div class="screenlet-title-bar">
      <ul>
        <#if lookupPerson?has_content>
          <h3>${uiLabelMap.PartyPersonalInformation}</h3>
          <#if security.hasEntityPermission("PARTYMGR", "_UPDATE", session)>
            <li><a href="<@ofbizUrl>editperson?partyId=${party.partyId}</@ofbizUrl>">${uiLabelMap.CommonUpdate}</a></li>
          </#if>
        </#if>
        <#if lookupGroup?has_content>
          <#assign lookupPartyType = party.getRelatedOneCache("PartyType")>
          <h3>${uiLabelMap.PartyPartyGroupInformation}</h3>
          <#if security.hasEntityPermission("PARTYMGR", "_UPDATE", session)>
            <li><a href="<@ofbizUrl>editpartygroup?partyId=${party.partyId}</@ofbizUrl>">${uiLabelMap.CommonUpdate}</a></li>
          </#if>
        </#if>
      </ul>
      <br class="clear" />
    </div>
    <div class="screenlet-body">
      <#if lookupPerson?has_content>
        <table class="basic-table" cellspacing="0">
          <tr>
            <td class="label">${uiLabelMap.PartyName}</td>
            <td>
              ${lookupPerson.personalTitle?if_exists}
              ${lookupPerson.firstName?if_exists}
              ${lookupPerson.middleName?if_exists}
              ${lookupPerson.lastName?if_exists}
              ${lookupPerson.suffix?if_exists}
            </td>
          </tr>
          <#if lookupPerson.nickname?has_content>
            <tr><td class="label">${uiLabelMap.PartyNickname}</td><td>${lookupPerson.nickname}</td></tr>
          </#if>
      	  <#if lookupPerson.gender?has_content>
            <tr><td class="label">${uiLabelMap.PartyGender}</td><td>${lookupPerson.gender}</td></tr>
          </#if>
          <#if lookupPerson.birthDate?has_content>
            <tr><td class="label">${uiLabelMap.PartyBirthDate}</td><td>${lookupPerson.birthDate.toString()}</td></tr>
          </#if>
          <#if lookupPerson.height?has_content>
            <tr><td class="label">${uiLabelMap.PartyHeight}</td><td>${lookupPerson.height}</td></tr>
          </#if>
          <#if lookupPerson.weight?has_content>
            <tr><td class="label">${uiLabelMap.PartyWeight}</td><td>${lookupPerson.weight}</td></tr>
          </#if>
          <#if lookupPerson.mothersMaidenName?has_content>
            <tr><td class="label">${uiLabelMap.PartyMothersMaidenName}</td><td>${lookupPerson.mothersMaidenName}</td></tr>
          </#if>
          <#if lookupPerson.maritalStatus?has_content>
            <tr><td class="label">${uiLabelMap.PartyMaritalStatus}</td><td>${lookupPerson.maritalStatus}</td></tr>
          </#if>
          <#if lookupPerson.socialSecurityNumber?has_content>
            <tr><td class="label">${uiLabelMap.PartySocialSecurityNumber}</td><td>${lookupPerson.socialSecurityNumber}</td></tr>
          </#if>
          <#if lookupPerson.passportNumber?has_content>
            <tr><td class="label">${uiLabelMap.PartyPassportNumber}</td><td>${lookupPerson.passportNumber}</td></tr>
          </#if>
          <#if lookupPerson.passportExpireDate?has_content>
            <tr><td class="label">${uiLabelMap.PartyPassportExpire}</td><td>${lookupPerson.passportExpireDate.toString()}</td></tr>
          </#if>
          <#if lookupPerson.totalYearsWorkExperience?has_content>
            <tr><td class="label">${uiLabelMap.PartyYearsWork}</td><td>${lookupPerson.totalYearsWorkExperience}</td></tr>
          </#if>
          <#if lookupPerson.comments?has_content>
            <tr><td class="label">${uiLabelMap.PartyComments}</td><td>${lookupPerson.comments}</td></tr>
          </#if>
        </table>
      <#elseif lookupGroup?has_content>
        <div>${lookupGroup.groupName} (${(lookupPartyType.get("description",locale))?if_exists})</div>
      <#else>
        <div>${uiLabelMap.PartyInformationNotFound}</div>
      </#if>
      <#if partyNameHistoryList?has_content>
        <div><hr/></div>
        <div>${uiLabelMap.PartyHistoryName}</div>
        <#list partyNameHistoryList as partyNameHistory>
          <#if lookupPerson?has_content>
            <div>${uiLabelMap.PartyHistoryWas}: ${partyNameHistory.personalTitle?if_exists} ${partyNameHistory.firstName?if_exists} ${partyNameHistory.middleName?if_exists} ${partyNameHistory.lastName?if_exists} ${partyNameHistory.suffix?if_exists} (${uiLabelMap.PartyHistoryChanged}: ${partyNameHistory.changeDate})</div>
          <#elseif lookupGroup?has_content>
            <div>${uiLabelMap.PartyHistoryWas}: ${partyNameHistory.groupName?if_exists} (${uiLabelMap.PartyHistoryChanged}: ${partyNameHistory.changeDate})</div>
          </#if>
        </#list>
      </#if>
    </div>
  </div>

<#-- ============================================================= -->
<#-- This is just sales over the last 12 months 
  <#if monthsToInclude?exists && totalSubRemainingAmount?exists && totalOrders?exists>
    <div id="totalOrders" class="screenlet">
      <div class="screenlet-title-bar">
        <h3>${uiLabelMap.PartyLoyaltyPoints}</h3>
      </div>
      <div class="screenlet-body">
        ${uiLabelMap.PartyYouHave} ${totalSubRemainingAmount} ${uiLabelMap.PartyPointsFrom} ${totalOrders} ${uiLabelMap.PartyOrderInLast} ${monthsToInclude} ${uiLabelMap.CommonMonths}.
      </div>
    </div>
  </#if>
  -->
<#-- ============================================================= -->
  <div id="partyContactInfo" class="screenlet">
    <div class="screenlet-title-bar">
      <ul>
        <h3>${uiLabelMap.PartyContactInformation}</h3>
        <#if security.hasEntityPermission("PARTYMGR", "_CREATE", session)>
          <li><a href="<@ofbizUrl>editcontactmech?partyId=${party.partyId}</@ofbizUrl>">${uiLabelMap.CommonCreateNew}</a></li>
        </#if>
      </ul>
      <br class="clear" />
    </div>
    <div class="screenlet-body">
      <#if contactMeches?has_content>
        <table class="basic-table" cellspacing="0">
          <tr>
            <th>${uiLabelMap.PartyContactType}</th>
            <th>${uiLabelMap.PartyContactInformation}</th>
            <th>${uiLabelMap.PartyContactSolicitingOk}</th>
            <th>&nbsp;</th>
          </tr>
          <#list contactMeches as contactMechMap>
            <#assign contactMech = contactMechMap.contactMech>
            <#assign partyContactMech = contactMechMap.partyContactMech>
            <tr><td colspan="4"><hr/></td></tr>
            <tr>
              <td class="label align-top">${contactMechMap.contactMechType.get("description",locale)}</td>
              <td>
                <#list contactMechMap.partyContactMechPurposes as partyContactMechPurpose>
                  <#assign contactMechPurposeType = partyContactMechPurpose.getRelatedOneCache("ContactMechPurposeType")>
                  <div>
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
                  <div>
                    <#if postalAddress.toName?has_content><b>${uiLabelMap.PartyAddrToName}:</b> ${postalAddress.toName}<br /></#if>
                    <#if postalAddress.attnName?has_content><b>${uiLabelMap.PartyAddrAttnName}:</b> ${postalAddress.attnName}<br /></#if>
                    ${postalAddress.address1?if_exists}<br />
                    <#if postalAddress.address2?has_content>${postalAddress.address2}<br /></#if>
                    ${postalAddress.city?if_exists},
                    <#if postalAddress.stateProvinceGeoId?has_content>
                      <#assign stateProvince = postalAddress.getRelatedOneCache("StateProvinceGeo")>
                      ${stateProvince.abbreviation?default(stateProvince.geoId)}
                    </#if>
                    ${postalAddress.postalCode?if_exists}
                    <#if postalAddress.countryGeoId?has_content><br />
                      <#assign country = postalAddress.getRelatedOneCache("CountryGeo")>
                      ${country.geoName?default(country.geoId)}
                    </#if>
                  </div>
                  <#if (postalAddress?has_content && !postalAddress.countryGeoId?has_content) || postalAddress.countryGeoId = "USA">
                    <#assign addr1 = postalAddress.address1?if_exists>
                    <#if (addr1.indexOf(" ") > 0)>
                      <#assign addressNum = addr1.substring(0, addr1.indexOf(" "))>
                      <#assign addressOther = addr1.substring(addr1.indexOf(" ")+1)>
                      <a target="_blank" href="http://www.whitepages.com/find_person_results.pl?fid=a&s_n=${addressNum}&s_a=${addressOther}&c=${postalAddress.city?if_exists}&s=${postalAddress.stateProvinceGeoId?if_exists}&x=29&y=18">(lookup:whitepages.com)</a>
                    </#if>
                  </#if>
                <#elseif "TELECOM_NUMBER" = contactMech.contactMechTypeId>
                  <#assign telecomNumber = contactMechMap.telecomNumber>
                  <div>
                    ${telecomNumber.countryCode?if_exists}
                    <#if telecomNumber.areaCode?has_content>${telecomNumber.areaCode?default("000")}-</#if>${telecomNumber.contactNumber?default("000-0000")}
                    <#if partyContactMech.extension?has_content>${uiLabelMap.PartyContactExt}&nbsp;${partyContactMech.extension}</#if>
                    <#if (telecomNumber?has_content && !telecomNumber.countryCode?has_content) || telecomNumber.countryCode = "011">
                      <a target="_blank" href="http://www.anywho.com/qry/wp_rl?npa=${telecomNumber.areaCode?if_exists}&telephone=${telecomNumber.contactNumber?if_exists}&btnsubmit.x=20&btnsubmit.y=8">(lookup:anywho.com)</a>
                      <a target="_blank" href="http://whitepages.com/find_person_results.pl?fid=p&ac=${telecomNumber.areaCode?if_exists}&s=&p=${telecomNumber.contactNumber?if_exists}&pt=b&x=40&y=9">(lookup:whitepages.com)</a>
                    </#if>
                  </div>
                <#elseif "EMAIL_ADDRESS" = contactMech.contactMechTypeId>
                  <div>
                    ${contactMech.infoString?if_exists}
                    <a href="<@ofbizUrl>EditCommunicationEvent?partyIdFrom=${userLogin.partyId}&partyId=${party.partyId}&communicationEventTypeId=EMAIL_COMMUNICATION&contactMechIdTo=${contactMech.contactMechId}&contactMechTypeId=EMAIL_ADDRESS<#if thisUserPrimaryEmail?has_content>&contactMechIdFrom=${thisUserPrimaryEmail.contactMechId}</#if></@ofbizUrl>">(${uiLabelMap.CommonSendEmail})</a>
                  </div>
                <#elseif "WEB_ADDRESS" = contactMech.contactMechTypeId>
                  <div>
                    ${contactMech.infoString?if_exists}
                    <#assign openAddress = contactMech.infoString?default("")>
                    <#if !openAddress?starts_with("http") && !openAddress?starts_with("HTTP")><#assign openAddress = "http://" + openAddress></#if>
                    <a target="_blank" href="${openAddress}">(${uiLabelMap.CommonOpenPageNewWindow})</a>
                  </div>
                <#else>
                  <div>${contactMech.infoString?if_exists}</div>
                </#if>
                <div>(${uiLabelMap.CommonUpdated}:&nbsp;${partyContactMech.fromDate.toString()})</div>
                <#if partyContactMech.thruDate?has_content><div><b>${uiLabelMap.PartyContactEffectiveThru}:&nbsp;${partyContactMech.thruDate.toString()}</b></div></#if>
                <#-- create cust request -->
                <#if custRequestTypes?exists>
                  <form name="createCustRequestForm" action="<@ofbizUrl>createCustRequest</@ofbizUrl>" method="POST">
                    <input type="hidden" name="partyId" value="${party.partyId}"/>
                    <input type="hidden" name="fromPartyId" value="${party.partyId}"/>
                    <input type="hidden" name="fulfillContactMechId" value="${contactMech.contactMechId}"/>
                    <select name="custRequestTypeId">
                      <#list custRequestTypes as type>
                        <option value="${type.custRequestTypeId}">${type.get("description", locale)}</option>
                      </#list>
                    </select>
                    <input type="submit" class="smallSubmit" value="${uiLabelMap.PartyCreateNewCustRequest}"/>
                  </form>
                </#if>
              </td>
              <td valign="top"><b>(${partyContactMech.allowSolicitation?if_exists})</b></td>
              <td class="button-col align-float">
                <#if security.hasEntityPermission("PARTYMGR", "_UPDATE", session)>
                  <a href="<@ofbizUrl>editcontactmech?partyId=${party.partyId}&contactMechId=${contactMech.contactMechId}</@ofbizUrl>">${uiLabelMap.CommonUpdate}</a>
                </#if>
                <#if security.hasEntityPermission("PARTYMGR", "_DELETE", session)>
                  <a href="<@ofbizUrl>deleteContactMech/viewprofile?partyId=${party.partyId}&contactMechId=${contactMech.contactMechId}</@ofbizUrl>">${uiLabelMap.CommonExpire}</a>
                </#if>
              </td>
            </tr>
          </#list>
        </table>
      <#else>
        ${uiLabelMap.PartyNoContactInformation}
      </#if>
    </div>
  </div>

<#-- Payment Info ============================================================= -->
  <div id="partyPaymentMethod" class="screenlet">
    <div class="screenlet-title-bar">
      <ul>
        <h3>${uiLabelMap.PartyPaymentMethodInformation}</h3>
        <#if security.hasEntityPermission("PAY_INFO", "_CREATE", session)>
          <li><a href="<@ofbizUrl>editeftaccount?partyId=${party.partyId}</@ofbizUrl>">${uiLabelMap.AccountingCreateNewEftAccount}</a></li>
          <li><a href="<@ofbizUrl>editgiftcard?partyId=${party.partyId}</@ofbizUrl>">${uiLabelMap.AccountingCreateNewGiftCard}</a></li>
          <li><a href="<@ofbizUrl>editcreditcard?partyId=${party.partyId}</@ofbizUrl>">${uiLabelMap.AccountingCreateNewCreditCard}</a></li>
        </#if>
      </ul>
      <br class="clear" />
    </div>
    <div class="screenlet-body">
      <#if paymentMethodValueMaps?has_content>
        <table class="basic-table" cellspacing="0">
          <#list paymentMethodValueMaps as paymentMethodValueMap>
            <#assign paymentMethod = paymentMethodValueMap.paymentMethod/>
            <tr>
              <#if "CREDIT_CARD" == paymentMethod.paymentMethodTypeId>
                <#assign creditCard = paymentMethodValueMap.creditCard/>
                <td class="label">
                  ${uiLabelMap.AccountingCreditCard}
                </td>
                <td>
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
                  <#if paymentMethod.description?has_content>(${paymentMethod.description})</#if>
                  <#if paymentMethod.glAccountId?has_content>(for GL Account ${paymentMethod.glAccountId})</#if>
                  <#if paymentMethod.fromDate?has_content>(${uiLabelMap.CommonUpdated}:&nbsp;${paymentMethod.fromDate?if_exists})</#if>
                  <#if paymentMethod.thruDate?has_content><b>(${uiLabelMap.PartyContactEffectiveThru}:&nbsp;${paymentMethod.thruDate})</#if>
                </td>
                <td class="button-col align-float">
                  <#if security.hasEntityPermission("MANUAL", "_PAYMENT", session)>
                    <a href="/accounting/control/manualETx?paymentMethodId=${paymentMethod.paymentMethodId}${externalKeyParam}">Manual Tx</a>
                  </#if>
                  <#if security.hasEntityPermission("PAY_INFO", "_UPDATE", session)>
                    <a href="<@ofbizUrl>editcreditcard?partyId=${party.partyId}&paymentMethodId=${paymentMethod.paymentMethodId}</@ofbizUrl>">${uiLabelMap.CommonUpdate}</a>
                  </#if>
                <#-- </td> -->
              <#elseif "GIFT_CARD" == paymentMethod.paymentMethodTypeId>
                <#assign giftCard = paymentMethodValueMap.giftCard>
                <td class="label" valign="top">
                  ${uiLabelMap.AccountingGiftCard}
                </td>
                <td>
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
                  <#if paymentMethod.description?has_content>(${paymentMethod.description})</#if>
                  <#if paymentMethod.glAccountId?has_content>(for GL Account ${paymentMethod.glAccountId})</#if>
                  <#if paymentMethod.fromDate?has_content>(${uiLabelMap.CommonUpdated}:&nbsp;${paymentMethod.fromDate?if_exists})</#if>
                  <#if paymentMethod.thruDate?has_content><b>(${uiLabelMap.PartyContactEffectiveThru}:&nbsp;${paymentMethod.thruDate.toString()}</b></#if>
                </td>
                <td class="button-col align-float">
                  <#if security.hasEntityPermission("PAY_INFO", "_UPDATE", session)>
                    <a href="<@ofbizUrl>editgiftcard?partyId=${party.partyId}&paymentMethodId=${paymentMethod.paymentMethodId}</@ofbizUrl>">${uiLabelMap.CommonUpdate}</a>
                  </#if>
                <#-- </td> -->
              <#elseif "EFT_ACCOUNT" == paymentMethod.paymentMethodTypeId>
                <#assign eftAccount = paymentMethodValueMap.eftAccount>
                <td class="label" valign="top">
                    ${uiLabelMap.PartyEftAccount}
                </td>
                <td>
                  ${eftAccount.nameOnAccount} - <#if eftAccount.bankName?has_content>${uiLabelMap.PartyBank}: ${eftAccount.bankName}</#if> <#if eftAccount.accountNumber?has_content>${uiLabelMap.PartyAccount} #: ${eftAccount.accountNumber}</#if>                  <#if paymentMethod.description?has_content>(${paymentMethod.description})</#if>
                  <#if paymentMethod.glAccountId?has_content>(for GL Account ${paymentMethod.glAccountId})</#if>
                  <#if paymentMethod.fromDate?has_content>(${uiLabelMap.CommonUpdated}:&nbsp;${paymentMethod.fromDate?if_exists})</#if>
                  <#if paymentMethod.thruDate?has_content><b>(${uiLabelMap.PartyContactEffectiveThru}:&nbsp;${paymentMethod.thruDate.toString()}</#if>
                </td>
                <td class="button-col align-float">
                  <#if security.hasEntityPermission("PAY_INFO", "_UPDATE", session)>
                    <a href="<@ofbizUrl>editeftaccount?partyId=${party.partyId}&paymentMethodId=${paymentMethod.paymentMethodId}</@ofbizUrl>">${uiLabelMap.CommonUpdate}</a>
                  </#if>
                <#-- </td> -->
              <#elseif "COMPANY_CHECK" == paymentMethod.paymentMethodTypeId>
                <td class="label" valign="top">
                  <#-- TODO: Convert hard-coded text to UI label properties -->
                  Company Check
                </td>
                <td>
                  <#if paymentMethod.description?has_content>(${paymentMethod.description})</#if>
                  <#if paymentMethod.glAccountId?has_content>(for GL Account ${paymentMethod.glAccountId})</#if>
                  <#if paymentMethod.fromDate?has_content>(${uiLabelMap.CommonUpdated}:&nbsp;${paymentMethod.fromDate?if_exists})</#if>
                  <#if paymentMethod.thruDate?has_content>(${uiLabelMap.PartyContactEffectiveThru}:&nbsp;${paymentMethod.thruDate.toString()}</#if>
                </td>
                <td class="button-col align-float">
                  &nbsp;
                <#-- </td> -->
              </#if>
              <#if security.hasEntityPermission("PAY_INFO", "_DELETE", session)>
                <a href="<@ofbizUrl>deletePaymentMethod/viewprofile?partyId=${party.partyId}&paymentMethodId=${paymentMethod.paymentMethodId}</@ofbizUrl>">${uiLabelMap.CommonExpire}</a>
              <#else>
                &nbsp;
              </#if>
              </td> <#-- closes out orphaned <td> elements inside conditionals -->
            </tr>
          </#list>
        </table>
      <#else>
        ${uiLabelMap.PartyNoPaymentMethodInformation}
      </#if>
    </div>
  </div>

<#-- AVS Strings -->
  <div id="partyAVS" class="screenlet">
    <div class="screenlet-title-bar">
      <h3>${uiLabelMap.PartyAvsOver}</h3>
    </div>
    <div class="screenlet-body">
      <span class="label">${uiLabelMap.PartyAvsString}</span>${(avsOverride.avsDeclineString)?default("${uiLabelMap.CommonGlobal}")}
      <#if security.hasEntityPermission("PARTYMGR", "_UPDATE", session)>
        <a href="<@ofbizUrl>editAvsOverride?partyId=${party.partyId}</@ofbizUrl>" class="smallSubmit">${uiLabelMap.CommonEdit}</a>
        <#if avsOverride?exists>
          <a href="<@ofbizUrl>resetAvsOverride?partyId=${party.partyId}</@ofbizUrl>" class="smallSubmit">${uiLabelMap.CommonReset}</a>
        </#if>
      </#if>
    </div>
  </div>

<#-- UserLogins -->
  <div id="partyUserLogins" class="screenlet">
    <div class="screenlet-title-bar">
      <ul>
        <h3>${uiLabelMap.PartyUserName}</h3>
        <#if security.hasEntityPermission("PARTYMGR", "_CREATE", session)>
          <li><a href="<@ofbizUrl>createnewlogin?partyId=${party.partyId}</@ofbizUrl>">${uiLabelMap.CommonCreateNew}</a></li>
        </#if>
      </ul>
      <br class="clear" />
    </div>
    <div class="screenlet-body">
      <#if userLogins?exists>
        <table class="basic-table" cellspacing="0">
          <#list userLogins as userUserLogin>
            <tr>
              <td class="label">${uiLabelMap.PartyUserLogin}</td>
              <td>${userUserLogin.userLoginId}</td>
              <td>
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
              </td>
              <td class="button-col align-float">
                <#if security.hasEntityPermission("PARTYMGR", "_CREATE", session)>
                  <a href="<@ofbizUrl>editlogin?partyId=${party.partyId}&userLoginId=${userUserLogin.userLoginId}</@ofbizUrl>">${uiLabelMap.CommonEdit}</a>
                </#if>
                <#if security.hasEntityPermission("SECURITY", "_VIEW", session)>
                  <a href="<@ofbizUrl>EditUserLoginSecurityGroups?partyId=${party.partyId}&userLoginId=${userUserLogin.userLoginId}</@ofbizUrl>">${uiLabelMap.PartySecurityGroups}</a>
                </#if>
              </td>
            </tr>
          </#list>
        </table>
      <#else>
        ${uiLabelMap.PartyNoUserLogin}
      </#if>
    </div>
  </div>

<#-- Party Attributes -->
  <div id="partyAttributes" class="screenlet">
    <div class="screenlet-title-bar">
      <ul>
        <h3>${uiLabelMap.PartyAttributes}</h3>
        <#if security.hasEntityPermission("PARTYMGR", "_CREATE", session)>
          <li><a href="<@ofbizUrl>editPartyAttribute?partyId=${party.partyId}</@ofbizUrl>">${uiLabelMap.CommonCreateNew}</a></li>
        </#if>
      </ul>
      <br class="clear" />
    </div>
    <div class="screenlet-body">
      <#if attributes?has_content>
        <table class="basic-table" cellspacing="0">
          <#list attributes as attr>
            <tr>
              <td class="label">
                ${uiLabelMap.CommonName}: ${attr.attrName}
              </td>
              <td>
                ${uiLabelMap.CommonValue}: ${attr.attrValue}
              </td>
              <td class="button-col align-float">
                <a href="<@ofbizUrl>editPartyAttribute?partyId=${partyId}&attrName=${attr.attrName}</@ofbizUrl>">${uiLabelMap.CommonEdit}</a>
              </td>
            </tr>
          </#list>
        </table>
      <#else>
        ${uiLabelMap.PartyNoPartyAttributesFound}
      </#if>
    </div>
  </div>

<#-- Visits -->
  <div id="partyVisits" class="screenlet">
    <div class="screenlet-title-bar">
      <ul>
        <h3>${uiLabelMap.PartyLastVisit}</h3>
        <li><a href="<@ofbizUrl>showvisits?partyId=${partyId}</@ofbizUrl>">${uiLabelMap.CommonListAll}</a></li>
      </ul>
      <br class="clear" />
    </div>
    <div class="screenlet-body">
      <#if visits?has_content>
        <table class="basic-table" cellspacing="0">
          <tr class="header-row">
            <td>${uiLabelMap.PartyVisitId}</td>
            <td>${uiLabelMap.PartyUserLogin}</td>
            <td>${uiLabelMap.PartyNewUser}</td>
            <td>${uiLabelMap.PartyWebApp}</td>
            <td>${uiLabelMap.PartyClientIP}</td>
            <td>${uiLabelMap.CommonFromDate}</td>
            <td>${uiLabelMap.CommonThruDate}</td>
          </tr>
          <#list visits as visitObj>
            <#if (visitObj_index > 4)><#break></#if>
              <tr>
                <td class="button-col">
                  <a href="<@ofbizUrl>visitdetail?visitId=${visitObj.visitId?if_exists}</@ofbizUrl>">${visitObj.visitId?if_exists}</a>
                </td>
                <td>${visitObj.userLoginId?if_exists}</td>
                <td>${visitObj.userCreated?if_exists}</td>
                <td>${visitObj.webappName?if_exists}</td>
                <td>${visitObj.clientIpAddress?if_exists}</td>
                <td>${(visitObj.fromDate.toString())?if_exists}</td>
                <td>${(visitObj.thruDate.toString())?if_exists}</td>
              </tr>
          </#list>
        </table>
      <#else>
        ${uiLabelMap.PartyNoVisitFound}
      </#if>
    </div>
  </div>

<#-- Current Cart -->
  <#if isCustomer?exists>
    <div id="partyShoppingCart" class="screenlet">
      <div class="screenlet-title-bar">
        <ul>
          <h3>${uiLabelMap.PartyCurrentShoppingCart}</h3>
          <#if security.hasEntityPermission("PARTYMGR", "_UPDATE", session)>
            <#if savedCartListId?has_content>
              <#assign listParam = "&shoppingListId=" + savedCartListId>
            <#else>
              <#assign listParam = "">
            </#if>
            <li><a href="<@ofbizUrl>editShoppingList?partyId=${partyId}${listParam}</@ofbizUrl>">${uiLabelMap.CommonEdit}</a></li>
          </#if>
        </ul>
      <br class="clear" />
      </div>
      <div class="screenlet-body">
        <#if savedCartItems?has_content>
          <table class="basic-table" cellspacing="0">
            <tr class="header-row">
              <td>${uiLabelMap.PartySequenceId}</td>
              <td>${uiLabelMap.PartyProductId}</td>
              <td>${uiLabelMap.PartyQuantity}</td>
              <td>${uiLabelMap.PartyQuantityPurchased}</td>
            </tr>
            <#list savedCartItems as savedCartItem>
              <tr>
                <td>${savedCartItem.shoppingListItemSeqId?if_exists}</td>
                <td class="button-col"><a href="/catalog/control/EditProduct?productId=${savedCartItem.productId}&externalLoginKey=${requestAttributes.externalLoginKey}">${savedCartItem.productId?if_exists}</a></td>
                <td>${savedCartItem.quantity?if_exists}</td>
                <td>${savedCartItem.quantityPurchased?if_exists}</td>
              </tr>
            </#list>
          </table>
        <#else>
          ${uiLabelMap.PartyNoShoppingCartSavedForParty}
        </#if>
      </div>
    </div>
  </#if>

<#-- Party Content -->
  <div id="partyContent" class="screenlet">
    <div class="screenlet-title-bar">
      <h3>${uiLabelMap.PartyContent}</h3>
    </div>
    <div class="screenlet-body">
      <#if partyContent?has_content>
        <table class="basic-table" cellspacing="0">
          <#list partyContent as pContent>
            <#assign content = pContent.getRelatedOne("Content")>
            <#assign contentType = content.getRelatedOneCache("ContentType")>
            <#assign mimeType = content.getRelatedOneCache("MimeType")?if_exists>
            <#assign status = content.getRelatedOneCache("StatusItem")>
            <#assign pcPurpose = pContent.getRelatedOne("Enumeration")>
            <tr>
              <td class="button-col"><a href="<@ofbizUrl>EditPartyContents?contentId=${pContent.contentId}&partyId=${pContent.partyId}</@ofbizUrl>">${content.contentId}</a></td>
              <td>${pcPurpose.description?if_exists}</td>
              <td>${content.contentName?if_exists}</td>
              <td>${(contentType.get("description",locale))?if_exists}</td>
              <td>${(mimeType.description)?if_exists}</td>
              <td>${(status.get("description",locale))?if_exists}</td>
              <#-- <td>${contentRole.fromDate?if_exists}</td> -->
              <td class="button-col align-float">
                <a href="<@ofbizUrl>img/${content.contentName}?imgId=${content.dataResourceId}</@ofbizUrl>">${uiLabelMap.CommonView}</a>
                <a href="<@ofbizUrl>removePartyContent/viewprofile?contentId=${pContent.contentId}&partyId=${pContent.partyId}</@ofbizUrl>">${uiLabelMap.CommonRemove}</a>
              </td>
            </tr>
          </#list>
        </table>
      <#else>
        ${uiLabelMap.PartyNoContent}
      </#if>
      <hr/>
      <div class="label">${uiLabelMap.PartyAttachContent}</div>
      <form method="post" enctype="multipart/form-data" action="<@ofbizUrl>uploadPartyContent</@ofbizUrl>">
        <input type="hidden" name="dataCategoryId" value="PERSONAL"/>
        <input type="hidden" name="contentTypeId" value="DOCUMENT"/>
        <input type="hidden" name="statusId" value="CTNT_PUBLISHED"/>
        <input type="hidden" name="partyId" value="${partyId}"/>
        <input type="file" name="uploadedFile" size="20"/>
        <select name="contentPurposeEnumId">
          <#-- TODO: Convert hard-coded text to UI label properties -->
          <option value="">Select Purpose</option>
          <#list contentPurposes as contentPurpose>
            <option value="${contentPurpose.enumId}">${contentPurpose.description?default(contentPurpose.enumId)}</option>          
          </#list>
        </select>
        <select name="roleTypeId">
          <#-- TODO: Convert hard-coded text to UI label properties -->
          <option value="">Select Role</option>
          <#list roles as role>
            <option value="${role.roleTypeId}">${role.description?default(role.roleTypeId)}</option>
          </#list>
        </select>
        <input type="submit" value="${uiLabelMap.CommonUpload}"/>
      </form>
    </div>
  </div>

<#-- Party Notes -->
  <div id="partyNotes" class="screenlet">
    <div class="screenlet-title-bar">
      <ul>
        <h3>${uiLabelMap.CommonNotes}</h3>
        <#if security.hasEntityPermission("PARTYMGR", "_NOTE", session)>
          <li><a href="<@ofbizUrl>AddPartyNote?partyId=${partyId}</@ofbizUrl>">${uiLabelMap.CommonCreateNew}</a></li>
        </#if>
      </ul>
      <br class="clear" />
    </div>
    <div class="screenlet-body">
      <#if notes?has_content>
        <table width="100%" border="0" cellpadding="1">
          <#list notes as noteRef>
            <tr>
              <td>
                <div><b>${uiLabelMap.CommonBy}: </b>${Static["org.ofbiz.party.party.PartyHelper"].getPartyName(delegator, noteRef.noteParty, true)}</div>
                <div><b>${uiLabelMap.CommonAt}: </b>${noteRef.noteDateTime.toString()}</div>
              </td>
              <td>
                ${noteRef.noteInfo}
              </td>
            </tr>
            <#if noteRef_has_next>
              <tr><td colspan="2"><hr></td></tr>
            </#if>
          </#list>
        </table>
      <#else>
        ${uiLabelMap.PartyNoNotesForParty}
      </#if>
    </div>
  </div>

<#else>
  ${uiLabelMap.PartyNoPartyFoundWithPartyId}: ${parameters.partyId?if_exists}
</#if>
