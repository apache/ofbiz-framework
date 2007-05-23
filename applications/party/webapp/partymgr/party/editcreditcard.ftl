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

<!-- begin editcreditcard.ftl -->
    <#if !creditCard?exists>
      <h1>${uiLabelMap.AccountingAddNewCreditCard}</h1>
      <form method="post" action="<@ofbizUrl>createCreditCard?DONE_PAGE=${donePage}</@ofbizUrl>" name="editcreditcardform" style="margin: 0;">
    <#else>
      <h1>${uiLabelMap.AccountingEditCreditCard}</h1>
      <form method="post" action="<@ofbizUrl>updateCreditCard?DONE_PAGE=${donePage}</@ofbizUrl>" name="editcreditcardform" style="margin: 0;">
        <input type="hidden" name="paymentMethodId" value="${paymentMethodId}">
    </#if>
    <div class="button-bar">
      <a href="<@ofbizUrl>${donePage}?partyId=${partyId}</@ofbizUrl>" class="smallSubmit">${uiLabelMap.CommonCancelDone}</a>
      <a href="javascript:document.editcreditcardform.submit()" class="smallSubmit">${uiLabelMap.CommonSave}</a>
    </div>
  <input type="hidden" name="partyId" value="${partyId}"/>
  <table class="basic-table" cellspacing="0">
    <tr>
      <td class="label">${uiLabelMap.AccountingCompanyNameCard}</td>
      <td>
        <input type="text" size="30" maxlength="60" name="companyNameOnCard" value="${creditCardData.companyNameOnCard?if_exists}">
      </td>
    </tr>
    <tr>
      <td class="label">${uiLabelMap.AccountingPrefixCard}</td>
      <td>
        <select name="titleOnCard">
          <option value="">${uiLabelMap.CommonSelectOne}</option>
          <option<#if (creditCardData.titleOnCard?default("") == "${uiLabelMap.CommonTitleMr}")> checked</#if>>${uiLabelMap.CommonTitleMr}</option>
          <option<#if (creditCardData.titleOnCard?default("") == "Mrs.")> checked</#if>>${uiLabelMap.CommonTitleMrs}</option>
          <option<#if (creditCardData.titleOnCard?default("") == "Ms.")> checked</#if>>${uiLabelMap.CommonTitleMs}</option>
          <option<#if (creditCardData.titleOnCard?default("") == "Dr.")> checked</#if>>${uiLabelMap.CommonTitleDr}</option>
        </select>
      </td>
    </tr>
    <tr>
      <td class="label">${uiLabelMap.AccountingFirstNameCard}</td>
      <td>
        <input type="text" class="required" size="20" maxlength="60" name="firstNameOnCard" value="${(creditCardData.firstNameOnCard)?if_exists}">
        <span class="tooltip">${uiLabelMap.CommonRequired}</span>
      </td>
    </tr>
    <tr>
      <td class="label">${uiLabelMap.AccountingMiddleNameCard}</td>
      <td>
        <input type="text" size="15" maxlength="60" name="middleNameOnCard" value="${(creditCardData.middleNameOnCard)?if_exists}">
      </td>
    </tr>
    <tr>
      <td class="label">${uiLabelMap.AccountingLastNameCard}</td>
      <td>
        <input type="text" class="required" size="20" maxlength="60" name="lastNameOnCard" value="${(creditCardData.lastNameOnCard)?if_exists}">
        <span class="tooltip">${uiLabelMap.CommonRequired}</span>
      </td>
    </tr>
    <tr>
      <td class="label">${uiLabelMap.AccountingSuffixCard}</td>
      <td>
        <select name="suffixOnCard">
          <option value="">${uiLabelMap.CommonSelectOne}</option>
          <option<#if (creditCardData.suffixOnCard?default("") == "Jr.")> checked</#if>>Jr.</option>
          <option<#if (creditCardData.suffixOnCard?default("") == "Sr.")> checked</#if>>Sr.</option>
          <option<#if (creditCardData.suffixOnCard?default("") == "I")> checked</#if>>I</option>
          <option<#if (creditCardData.suffixOnCard?default("") == "II")> checked</#if>>II</option>
          <option<#if (creditCardData.suffixOnCard?default("") == "III")> checked</#if>>III</option>
          <option<#if (creditCardData.suffixOnCard?default("") == "IV")> checked</#if>>IV</option>
          <option<#if (creditCardData.suffixOnCard?default("") == "V")> checked</#if>>V</option>
        </select>
      </td>
    </tr>
    <tr>
      <td class="label">${uiLabelMap.AccountingCardType}</td>
      <td>
        <select name="cardType" class="required">
          <option>${creditCardData.cardType?if_exists}</option>
          <option></option>
          ${screens.render("component://common/widget/CommonScreens.xml#cctypes")}
        </select>
        <span class="tooltip">${uiLabelMap.CommonRequired}</span>
      </td>
    </tr>
    <tr>
      <td class="label">${uiLabelMap.AccountingCardNumber}</td>
      <td>
        <#if creditCardData?has_content>
          <#-- create a display version of the card where all but the last four digits are * -->
          <#assign cardNumberDisplay = "">
          <#assign cardNumber = creditCardData.cardNumber?if_exists>
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
        </#if>
        <input type="text" class="required" size="20" maxlength="30" name="cardNumber" onfocus="javascript:this.value = '';" value="${cardNumberDisplay?if_exists}">
        <span class="tooltip">${uiLabelMap.CommonRequired}</span>
      </td>
    </tr>
    <#--<tr>
      <td class="label">${uiLabelMap.AccountingCardSecurityCode}</td>
      <td>
        <input type="text" size="5" maxlength="10" name="cardSecurityCode" value="${creditCardData.cardSecurityCode?if_exists}">
      </td>
    </tr>-->
    <tr>
      <td class="label">${uiLabelMap.AccountingExpirationDate}</td>        
      <td>
        <#assign expMonth = "">
        <#assign expYear = "">
        <#if creditCard?exists>
          <#assign expDate = creditCard.expireDate>
          <#if (expDate?exists && expDate.indexOf("/") > 0)>
            <#assign expMonth = expDate.substring(0,expDate.indexOf("/"))>
            <#assign expYear = expDate.substring(expDate.indexOf("/")+1)>
          </#if>
        </#if>
        <select name="expMonth" class="required">
          <option><#if tryEntity>${expMonth?if_exists}<#else>${requestParameters.expMonth?if_exists}</#if></option>
          ${screens.render("component://common/widget/CommonScreens.xml#ccmonths")}
        </select>
        <select name="expYear" class="required">
          <option><#if tryEntity>${expYear?if_exists}<#else>${requestParameters.expYear?if_exists}</#if></option>
          ${screens.render("component://common/widget/CommonScreens.xml#ccyears")}
        </select>
        <span class="tooltip">${uiLabelMap.CommonRequired}</span>
      </td>
    </tr>
    <tr>
      <td class="label">${uiLabelMap.CommonDescription}</td>
      <td>
        <input type="text" size="30" maxlength="60" name="description" value="${paymentMethodData.description?if_exists}">
      </td>
    </tr>
    <tr>
      <td class="label">${uiLabelMap.AccountingBillingAddress}</td>
      <td>
        <#-- Removed because is confusing, can add but would have to come back here with all data populated as before...
        <a href="<@ofbizUrl>editcontactmech</@ofbizUrl>" class="smallSubmit">
          [Create New Address]</a>&nbsp;&nbsp;
        -->
        <table cellspacing="0">
        <#assign hasCurrent = false>
        <#if curPostalAddress?has_content>
          <#assign hasCurrent = true>
          <tr>
            <td class="button-col">
              <input type="radio" name="contactMechId" value="${curContactMechId}" checked>
            </td>
            <td>
              <p><b>${uiLabelMap.PartyUseCurrentAddress}:</b></p>
              <#list curPartyContactMechPurposes as curPartyContactMechPurpose> 
                <#assign curContactMechPurposeType = curPartyContactMechPurpose.getRelatedOneCache("ContactMechPurposeType")>
                <p>
                  <b>${curContactMechPurposeType.get("description",locale)?if_exists}</b>
                  <#if curPartyContactMechPurpose.thruDate?exists>
                    (${uiLabelMap.CommonExpire}:${curPartyContactMechPurpose.thruDate.toString()})
                  </#if>
                </p>
              </#list>
              <#if curPostalAddress.toName?exists><p><b>${uiLabelMap.CommonTo}:</b> ${curPostalAddress.toName}</p></#if>
              <#if curPostalAddress.attnName?exists><p><b>${uiLabelMap.PartyAddrAttnName}:</b> ${curPostalAddress.attnName}</p></#if>
              <#if curPostalAddress.address1?exists><p>${curPostalAddress.address1}</p></#if>
              <#if curPostalAddress.address2?exists><p>${curPostalAddress.address2}</p></#if>
              <p>${curPostalAddress.city?if_exists}<#if curPostalAddress.stateProvinceGeoId?has_content>,&nbsp;${curPostalAddress.stateProvinceGeoId?if_exists}</#if>&nbsp;${curPostalAddress.postalCode?if_exists}</p>
              <#if curPostalAddress.countryGeoId?exists><p>${curPostalAddress.countryGeoId}</p></#if>
              <p>(${uiLabelMap.CommonUpdated}:&nbsp;${(curPartyContactMech.fromDate.toString())?if_exists})</p>
              <#if curPartyContactMech.thruDate?exists><p><b>${uiLabelMap.CommonDelete}:&nbsp;${curPartyContactMech.thruDate.toString()}</b></p></#if>
            </td>
          </tr>
        <#else>
           <#-- <tr>
            <td align="left" valign="top" colspan="2">
              ${uiLabelMap.PartyBillingAddressNotSelected}
            </td>
          </tr> -->
        </#if>
          <#-- is confusing
          <tr>
            <td align="left" valign="top" colspan="2">
              <b>Select a New Billing Address:</b>
            </td>
          </tr>
          -->
          <#list postalAddressInfos as postalAddressInfo>
            <#assign contactMech = postalAddressInfo.contactMech>
            <#assign partyContactMechPurposes = postalAddressInfo.partyContactMechPurposes>
            <#assign postalAddress = postalAddressInfo.postalAddress>
            <#assign partyContactMech = postalAddressInfo.partyContactMech>
            <tr>
              <td class="button-col">
                <input type="radio" name="contactMechId" value="${contactMech.contactMechId}">
              </td>
              <td>
                <#list partyContactMechPurposes as partyContactMechPurpose>
                  <#assign contactMechPurposeType = partyContactMechPurpose.getRelatedOneCache("ContactMechPurposeType")>
                  <p>
                    <b>${contactMechPurposeType.get("description",locale)?if_exists}</b>
                    <#if partyContactMechPurpose.thruDate?exists>(${uiLabelMap.CommonExpire}:${partyContactMechPurpose.thruDate})</#if>
                  </p>
                </#list>
                <#if postalAddress.toName?exists><p><b>${uiLabelMap.CommonTo}:</b> ${postalAddress.toName}</p></#if>
                <#if postalAddress.attnName?exists><p><b>${uiLabelMap.PartyAddrAttnName}:</b> ${postalAddress.attnName}</p></#if>
                <#if postalAddress.address1?exists><p>${postalAddress.address1}</p></#if>
                <#if postalAddress.address2?exists><p>${postalAddress.address2}</p></#if>
                <p>${postalAddress.city}<#if postalAddress.stateProvinceGeoId?has_content>,&nbsp;${postalAddress.stateProvinceGeoId}</#if>&nbsp;${postalAddress.postalCode?if_exists}</p>
                <#if postalAddress.countryGeoId?exists><p>${postalAddress.countryGeoId}</p></#if>
                <p>(${uiLabelMap.CommonUpdated}:&nbsp;${(partyContactMech.fromDate.toString())?if_exists})</p>
                <#if partyContactMech.thruDate?exists><p><b>${uiLabelMap.CommonDelete}:&nbsp;${partyContactMech.thruDate.toString()}</b></p></#if>
              </td>
            </tr>
          </#list>
          <#if !postalAddressInfos?has_content && !curContactMech?exists>
              <tr><td colspan="2">${uiLabelMap.PartyNoContactInformation}.</td></tr>
          </#if>
          <#-- not yet supported in party manager
          <tr>
            <td align="right" valigh="top" width="1%">
              <input type="radio" name="contactMechId" value="_NEW_" <#if !hasCurrent>checked</#if>>
            </td>
            <td align="left" valign="middle" width="80%">
              ${uiLabelMap.PartyCreateNewBillingAddress}.
            </td>
          </tr>
          -->
        </table>
      </td>
    </tr>
  </table>
  </form>

  <div class="button-bar">
    <a href="<@ofbizUrl>${donePage}?partyId=${partyId}</@ofbizUrl>" class="smallSubmit">${uiLabelMap.CommonCancelDone}</a>
    <a href="javascript:document.editcreditcardform.submit()" class="smallSubmit">${uiLabelMap.CommonSave}</a>
  </div>
<!-- end editcreditcard.ftl -->

