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
<#if canNotView>
  <p><h3>${uiLabelMap.PartyContactInfoNotBelongToYou}.</h3></p>
  &nbsp;<a href="<@ofbizUrl>authview/${donePage}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonBack}</a>
<#else>

  <#if !contactMech?exists>
    <#-- When creating a new contact mech, first select the type, then actually create -->
    <#if !requestParameters.preContactMechTypeId?exists && !preContactMechTypeId?exists>
    <p class="head1">${uiLabelMap.PartyCreateNewContactInfo}</p>
    <form method="post" action='<@ofbizUrl>editcontactmech?DONE_PAGE=${donePage}</@ofbizUrl>' name="createcontactmechform">
      <table width="90%" border="0" cellpadding="2" cellspacing="0">
        <tr>
          <td width="26%"><div class="tabletext">${uiLabelMap.PartySelectContactType}:</div></td>
          <td width="74%">
            <select name="preContactMechTypeId" class='selectBox'>
              <#list contactMechTypes as contactMechType>
                <option value='${contactMechType.contactMechTypeId}'>${contactMechType.get("description",locale)}</option>
              </#list>
            </select>&nbsp;<a href="javascript:document.createcontactmechform.submit()" class="buttontext">${uiLabelMap.CommonCreate}</a>
          </td>
        </tr>
      </table>
    </form>
    <#-- <p><h3>ERROR: Contact information with ID "${contactMechId}" not found!</h3></p> -->
    </#if>
  </#if>

  <#if contactMechTypeId?exists>
    <#if !contactMech?exists>
      <div class="head1">${uiLabelMap.PartyCreateNewContactInfo}</div>
      &nbsp;<a href='<@ofbizUrl>authview/${donePage}</@ofbizUrl>' class="buttontext">${uiLabelMap.CommonGoBack}</a>
      &nbsp;<a href="javascript:document.editcontactmechform.submit()" class="buttontext">${uiLabelMap.CommonSave}</a>
      <table width="90%" border="0" cellpadding="2" cellspacing="0">
        <form method="post" action='<@ofbizUrl>${requestName}</@ofbizUrl>' name="editcontactmechform">
        <input type='hidden' name='DONE_PAGE' value='${donePage}'>
        <input type='hidden' name='contactMechTypeId' value='${contactMechTypeId}'>
        <#if contactMechPurposeType?exists>
            <div class="tabletext">(${uiLabelMap.PartyNewContactHavePurpose} <b>"${contactMechPurposeType.get("description",locale)?if_exists}"</b>)</div>
        </#if>
        <#if cmNewPurposeTypeId?has_content><input type='hidden' name='contactMechPurposeTypeId' value='${cmNewPurposeTypeId}'></#if>
        <#if preContactMechTypeId?has_content><input type='hidden' name='preContactMechTypeId' value='${preContactMechTypeId}'></#if>
        <#if paymentMethodId?has_content><input type='hidden' name='paymentMethodId' value='${paymentMethodId}'></#if>
    <#else>
      <p class="head1">${uiLabelMap.PartyEditContactInfo}</p>
      &nbsp;<a href="<@ofbizUrl>authview/${donePage}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonGoBack}</a>
      &nbsp;<a href="javascript:document.editcontactmechform.submit()" class="buttontext">${uiLabelMap.CommonSave}</a>
      <table width="90%" border="0" cellpadding="2" cellspacing="0">
        <tr>
          <td width="26%" align="right" valign="top"><div class="tabletext">${uiLabelMap.PartyContactPurposes}</div></td>
          <td width="5">&nbsp;</td>
          <td width="74%">
            <table border="0" cellspacing="1" bgcolor="black">
              <#list partyContactMechPurposes?if_exists as partyContactMechPurpose>
                <#assign contactMechPurposeType = partyContactMechPurpose.getRelatedOneCache("ContactMechPurposeType")>
                <tr>
                  <td bgcolor='white'>
                    <div class="tabletext">&nbsp;
                      <#if contactMechPurposeType?exists>
                        <b>${contactMechPurposeType.get("description",locale)}</b>
                      <#else>
                        <b>${uiLabelMap.PartyPurposeTypeNotFound}: "${partyContactMechPurpose.contactMechPurposeTypeId}"</b>
                      </#if>
                      (Since:${partyContactMechPurpose.fromDate.toString()})
                      <#if partyContactMechPurpose.thruDate?exists>(${uiLabelMap.CommonExpires}:${partyContactMechPurpose.thruDate.toString()})</#if>
                    &nbsp;</div></td>
                  <td bgcolor='white'><div><a href='<@ofbizUrl>deletePartyContactMechPurpose?contactMechId=${contactMechId}&contactMechPurposeTypeId=${partyContactMechPurpose.contactMechPurposeTypeId}&fromDate=${partyContactMechPurpose.fromDate.toString()?html}&DONE_PAGE=${donePage}&useValues=true</@ofbizUrl>' class='buttontext'>&nbsp;${uiLabelMap.CommonDelete}&nbsp;</a></div></td>
                </tr>
              </#list>
              <#if purposeTypes?has_content>
              <tr>
                <form method="post" action='<@ofbizUrl>createPartyContactMechPurpose?contactMechId=${contactMechId}&DONE_PAGE=${donePage}&useValues=true</@ofbizUrl>' name='newpurposeform'>
                  <td bgcolor='white'>
                    <select name='contactMechPurposeTypeId' class='selectBox'>
                      <option></option>
                      <#list purposeTypes as contactMechPurposeType>
                        <OPTION value='${contactMechPurposeType.contactMechPurposeTypeId}'>${contactMechPurposeType.get("description",locale)}</OPTION>
                      </#list>
                    </select>
                  </td>
                </form>
                <td bgcolor='white'><div><a href='javascript:document.newpurposeform.submit()' class='buttontext'>${uiLabelMap.PartyAddPurpose}&nbsp;</a></div></td>
              </tr>
              </#if>
            </table>
          </td>
        </tr>
        <form method="post" action='<@ofbizUrl>${requestName}</@ofbizUrl>' name="editcontactmechform">
        <input type="hidden" name="DONE_PAGE" value='${donePage}'>
        <input type="hidden" name="contactMechId" value='${contactMechId}'>
        <input type="hidden" name="contactMechTypeId" value='${contactMechTypeId}'>
    </#if>

  <#if contactMechTypeId = "POSTAL_ADDRESS">
    <tr>
      <td width="26%" align="right" valign="top"><div class="tabletext">${uiLabelMap.PartyToName}</div></td>
      <td width="5">&nbsp;</td>
      <td width="74%">
        <input type="text" class='inputBox' size="30" maxlength="60" name="toName" value="${postalAddressData.toName?if_exists}">
      </td>
    </tr>
    <tr>
      <td width="26%" align="right" valign="top"><div class="tabletext">${uiLabelMap.PartyAttentionName}</div></td>
      <td width="5">&nbsp;</td>
      <td width="74%">
        <input type="text" class='inputBox' size="30" maxlength="60" name="attnName" value="${postalAddressData.attnName?if_exists}">
      </td>
    </tr>
    <tr>
      <td width="26%" align="right" valign="top"><div class="tabletext">${uiLabelMap.PartyAddressLine1}</div></td>
      <td width="5">&nbsp;</td>
      <td width="74%">
        <input type="text" class='inputBox' size="30" maxlength="30" name="address1" value="${postalAddressData.address1?if_exists}">
      *</td>
    </tr>
    <tr>
      <td width="26%" align="right" valign="top"><div class="tabletext">${uiLabelMap.PartyAddressLine2}</div></td>
      <td width="5">&nbsp;</td>
      <td width="74%">
          <input type="text" class='inputBox' size="30" maxlength="30" name="address2" value="${postalAddressData.address2?if_exists}">
      </td>
    </tr>
    <tr>
      <td width="26%" align="right" valign="top"><div class="tabletext">${uiLabelMap.PartyCity}</div></td>
      <td width="5">&nbsp;</td>
      <td width="74%">
          <input type="text" class='inputBox' size="30" maxlength="30" name="city" value="${postalAddressData.city?if_exists}">
      *</td>
    </tr>
    <tr>
      <td width="26%" align="right" valign="top"><div class="tabletext">${uiLabelMap.PartyState}</div></td>
      <td width="5">&nbsp;</td>
      <td width="74%">
        <select name="stateProvinceGeoId" class='selectBox'>
          <#if postalAddressData.stateProvinceGeoId?exists><option value='${postalAddressData.stateProvinceGeoId}'>${selectedStateName?default(postalAddressData.stateProvinceGeoId)}</option></#if>
          <option value="">${uiLabelMap.PartyNoState}</option>          
          ${screens.render("component://common/widget/CommonScreens.xml#states")}
        </select>
      *</td>
    </tr>
    <tr>
      <td width="26%" align="right" valign="top"><div class="tabletext">${uiLabelMap.PartyZipCode}</div></td>
      <td width="5">&nbsp;</td>
      <td width="74%">
        <input type="text" class='inputBox' size="12" maxlength="10" name="postalCode" value="${postalAddressData.postalCode?if_exists}">
      </td>
    </tr>
    <tr>
      <td width="26%" align="right" valign="top"><div class="tabletext">${uiLabelMap.PartyCountry}</div></td>
      <td width="5">&nbsp;</td>
      <td width="74%">
        <select name="countryGeoId" class='selectBox'>
          <#if postalAddressData.countryGeoId?exists><option value='${postalAddressData.countryGeoId}'>${selectedCountryName?default(postalAddressData.countryGeoId)}</option></#if>
          ${screens.render("component://common/widget/CommonScreens.xml#countries")}
        </select>
      *</td>
    </tr>
  <#elseif contactMechTypeId = "TELECOM_NUMBER">
    <tr>
      <td width="26%" align="right" valign="top"><div class="tabletext">${uiLabelMap.PartyPhoneNumber}</div></td>
      <td width="5">&nbsp;</td>
      <td width="74%">
        <div class="tabletext">
        <input type="text" class='inputBox' size="4" maxlength="10" name="countryCode" value="${telecomNumberData.countryCode?if_exists}">
        -&nbsp;<input type="text" class='inputBox' size="4" maxlength="10" name="areaCode" value="${telecomNumberData.areaCode?if_exists}">
        -&nbsp;<input type="text" class='inputBox' size="15" maxlength="15" name="contactNumber" value="${telecomNumberData.contactNumber?if_exists}">
        &nbsp;ext&nbsp;<input type="text" class='inputBox' size="6" maxlength="10" name="extension" value="${partyContactMechData.extension?if_exists}">
        </div>
      </td>
    </tr>
    <tr>
      <td width="26%" align="right" valign="top"><div class="tabletext"></div></td>
      <td width="5">&nbsp;</td>
      <td><div class="tabletext">[${uiLabelMap.PartyCountryCode}] [${uiLabelMap.PartyAreaCode}] [${uiLabelMap.PartyContactNumber}] [${uiLabelMap.PartyExtension}]</div></td>
    </tr>
  <#elseif contactMechTypeId = "EMAIL_ADDRESS">
    <tr>
      <td width="26%" align="right" valign="top"><div class="tabletext">${uiLabelMap.PartyEmailAddress}</div></td>
      <td width="5">&nbsp;</td>
      <td width="74%">
          <input type="text" class='inputBox' size="60" maxlength="255" name="emailAddress" value="<#if tryEntity>${contactMech.infoString?if_exists}<#else>${requestParameters.emailAddress?if_exists}</#if>">
      *</td>
    </tr>
  <#else>
    <tr>
      <td width="26%" align="right" valign="top"><div class="tabletext">${contactMechType.get("description",locale)?if_exists}</div></td>
      <td width="5">&nbsp;</td>
      <td width="74%">
          <input type="text" class='inputBox' size="60" maxlength="255" name="infoString" value="${contactMechData.infoString?if_exists}">
      *</td>
    </tr>
  </#if>
    <tr>
      <td width="26%" align="right" valign="top"><div class="tabletext">${uiLabelMap.PartyAllowSolicitation}?</div></td>
      <td width="5">&nbsp;</td>
      <td width="74%">
        <select name="allowSolicitation" class='selectBox'>
          <option>${partyContactMechData.allowSolicitation?if_exists}</option>
          <option></option><option>${uiLabelMap.CommonY}</option><option>${uiLabelMap.CommonN}</option>
        </select>
      </td>
    </tr>
  </form>
  </table>

    &nbsp;<a href="<@ofbizUrl>authview/${donePage}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonGoBack}</a>
    &nbsp;<a href="javascript:document.editcontactmechform.submit()" class="buttontext">${uiLabelMap.CommonSave}</a>
  <#else>
    &nbsp;<a href="<@ofbizUrl>authview/${donePage}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonGoBack}</a>
  </#if>
</#if>



