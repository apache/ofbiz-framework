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

<#if !mechMap.facilityContactMech?exists && mechMap.contactMech?exists>
  <p><h3>${uiLabelMap.PartyContactInfoNotBelongToYou}.</h3></p>
  &nbsp;<a href="<@ofbizUrl>authview/${donePage}?facilityId=${facilityId}</@ofbizUrl>" class="buttontext">[${uiLabelMapCommon.Back}]</a>
<#else>
  <#if !mechMap.contactMech?exists>
    <#-- When creating a new contact mech, first select the type, then actually create -->
    <#if !preContactMechTypeId?has_content>
    <p class="head1">${uiLabelMap.PartyCreateNewContact}</p>
    <form method="post" action='<@ofbizUrl>EditContactMech</@ofbizUrl>' name="createcontactmechform">
      <input type='hidden' name='facilityId' value='${facilityId}'>
      <input type='hidden' name='DONE_PAGE' value='${donePage?if_exists}'>
      <table width="90%" border="0" cellpadding="2" cellspacing="0">
        <tr>
          <td width="26%"><div class="tabletext">${uiLabelMap.PartySelectContactType}:</div></td>
          <td width="74%">
            <select name="preContactMechTypeId" class="selectBox">
              <#list mechMap.contactMechTypes as contactMechType>
                <option value='${contactMechType.contactMechTypeId}'>${contactMechType.get("description",locale)}</option>
              </#list>
            </select>&nbsp;<a href="javascript:document.createcontactmechform.submit()" class="buttontext">[${uiLabelMap.CommonCreate}]</a>
          </td>
        </tr>
      </table>
    </form>
    </#if>
  </#if>

  <#if mechMap.contactMechTypeId?has_content>
    <#if !mechMap.contactMech?has_content>
      <p class="head1">${uiLabelMap.PartyCreateNewContact}</p>
    &nbsp;<a href='<@ofbizUrl>authview/${donePage}?facilityId=${facilityId}</@ofbizUrl>' class="buttontext">[${uiLabelMap.CommonGoBack}]</a>
    &nbsp;<a href="javascript:document.editcontactmechform.submit()" class="buttontext">[${uiLabelMap.CommonSave}]</a>
      <#if contactMechPurposeType?exists>
        <div>(${uiLabelMap.PartyMsgContactHavePurpose}<b>"${contactMechPurposeType.get("description",locale)?if_exists}"</b>)</div>
      </#if>
      <table width="90%" border="0" cellpadding="2" cellspacing="0">
        <form method="post" action='<@ofbizUrl>${mechMap.requestName}</@ofbizUrl>' name="editcontactmechform">
        <input type='hidden' name='DONE_PAGE' value='${donePage}'>
        <input type='hidden' name='contactMechTypeId' value='${mechMap.contactMechTypeId}'>
        <input type='hidden' name='facilityId' value='${facilityId}'>        
        <#if preContactMechTypeId?exists><input type='hidden' name='preContactMechTypeId' value='${preContactMechTypeId}'></#if>
        <#if contactMechPurposeTypeId?exists><input type='hidden' name='contactMechPurposeTypeId' value='${contactMechPurposeTypeId?if_exists}'></#if>
        
        <#if paymentMethodId?exists><input type='hidden' name='paymentMethodId' value='${paymentMethodId}'></#if>
    <#else>
      <p class="head1">${uiLabelMap.PartyEditContactInformation}</p>
    &nbsp;<a href='<@ofbizUrl>authview/${donePage}?facilityId=${facilityId}</@ofbizUrl>' class='buttontext'>[${uiLabelMap.CommonGoBack}]</a>
    &nbsp;<a href="javascript:document.editcontactmechform.submit()" class="buttontext">[${uiLabelMap.CommonSave}]</a>
      <table width="90%" border="0" cellpadding="2" cellspacing="0">
        <#if mechMap.purposeTypes?has_content>
        <tr>
          <td width="26%" align="right" valign="top"><div class="tabletext">${uiLabelMap.PartyContactPurposes}</div></td>
          <td width="5">&nbsp;</td>
          <td width="74%">
            <table border='0' cellspacing='1' bgcolor='black'>  
            <#if mechMap.facilityContactMechPurposes?has_content>          
              <#list mechMap.facilityContactMechPurposes as facilityContactMechPurpose>
                <#assign contactMechPurposeType = facilityContactMechPurpose.getRelatedOneCache("ContactMechPurposeType")>
                <tr>
                  <td bgcolor='white'>
                    <div class="tabletext">&nbsp;
                      <#if contactMechPurposeType?has_content>
                        <b>${contactMechPurposeType.get("description",locale)}</b>
                      <#else>
                        <b>${uiLabelMap.PartyMechPurposeTypeNotFound}: "${facilityContactMechPurpose.contactMechPurposeTypeId}"</b>
                      </#if>
                      (${uiLabelMap.CommonSince}:${facilityContactMechPurpose.fromDate.toString()})
                      <#if facilityContactMechPurpose.thruDate?has_content>(${uiLabelMap.CommonExpires}: ${facilityContactMechPurpose.thruDate.toString()}</#if>
                    &nbsp;</div></td>
                  <td bgcolor='white'><div><a href='<@ofbizUrl>deleteFacilityContactMechPurpose?facilityId=${facilityId}&contactMechId=${contactMechId}&contactMechPurposeTypeId=${facilityContactMechPurpose.contactMechPurposeTypeId}&fromDate=${facilityContactMechPurpose.fromDate.toString()}&DONE_PAGE=${donePage}&useValues=true</@ofbizUrl>' class='buttontext'>&nbsp;${uiLabelMap.CommonDelete}&nbsp;</a></div></td>
                </tr>
              </#list>
              </#if>              
           
              <tr>
                <form method="post" action='<@ofbizUrl>createFacilityContactMechPurpose?DONE_PAGE=${donePage}&useValues=true</@ofbizUrl>' name='newpurposeform'>
                <input type="hidden" name='facilityId' value='${facilityId}'>
                <input type="hidden" name='contactMechId' value='${contactMechId?if_exists}'>
                  <td bgcolor='white'>
                    <select name='contactMechPurposeTypeId' class="selectBox">
                      <option></option>
                      <#list mechMap.purposeTypes as contactMechPurposeType>
                        <option value='${contactMechPurposeType.contactMechPurposeTypeId}'>${contactMechPurposeType.get("description",locale)}</option>
                      </#list>
                    </select>
                  </td>
                </form>
                <td bgcolor='white'><div><a href='javascript:document.newpurposeform.submit()' class='buttontext'>${uiLabelMap.PartyAddPurpose}</a></div></td>
              </tr>
            </table>
          </td>
        </tr>
        </#if>
        <form method="post" action='<@ofbizUrl>${mechMap.requestName}</@ofbizUrl>' name="editcontactmechform">
        <input type="hidden" name="contactMechId" value='${contactMechId}'>
        <input type="hidden" name="contactMechTypeId" value='${mechMap.contactMechTypeId}'>
        <input type="hidden" name='facilityId' value='${facilityId}'>
    </#if>
  
  <#if "POSTAL_ADDRESS" = mechMap.contactMechTypeId?if_exists>
    <tr>
      <td width="26%" align="right" valign="top"><div class="tabletext">${uiLabelMap.PartyToName}</div></td>
      <td width="5">&nbsp;</td>
      <td width="74%">
        <input type="text" class="inputBox" size="30" maxlength="60" name="toName" value="${(mechMap.postalAddress.toName)?default(request.getParameter('toName')?if_exists)}">
      </td>
    </tr>
    <tr>
      <td width="26%" align="right" valign="top"><div class="tabletext">${uiLabelMap.PartyAttentionName}</div></td>
      <td width="5">&nbsp;</td>
      <td width="74%">
        <input type="text" class="inputBox" size="30" maxlength="60" name="attnName" value="${(mechMap.postalAddress.attnName)?default(request.getParameter('attnName')?if_exists)}">
      </td>
    </tr>
    <tr>
      <td width="26%" align="right" valign="top"><div class="tabletext">${uiLabelMap.PartyAddressLine1}</div></td>
      <td width="5">&nbsp;</td>
      <td width="74%">
        <input type="text" class="inputBox" size="30" maxlength="30" name="address1" value="${(mechMap.postalAddress.address1)?default(request.getParameter('address1')?if_exists)}">
      *</td>
    </tr>
    <tr>
      <td width="26%" align="right" valign="top"><div class="tabletext">${uiLabelMap.PartyAddressLine2}</div></td>
      <td width="5">&nbsp;</td>
      <td width="74%">
          <input type="text" class="inputBox" size="30" maxlength="30" name="address2" value="${(mechMap.postalAddress.address2)?default(request.getParameter('address2')?if_exists)}">
      </td>
    </tr>
    <tr>
      <td width="26%" align="right" valign="top"><div class="tabletext">${uiLabelMap.PartyCity}</div></td>
      <td width="5">&nbsp;</td>
      <td width="74%">
          <input type="text" class="inputBox" size="30" maxlength="30" name="city" value="${(mechMap.postalAddress.city)?default(request.getParameter('city')?if_exists)}">
      *</td>
    </tr>
    <tr>
      <td width="26%" align="right" valign="top"><div class="tabletext">${uiLabelMap.PartyState}</div></td>
      <td width="5">&nbsp;</td>
      <td width="74%">
        <select name="stateProvinceGeoId" class="selectBox">
          <option>${(mechMap.postalAddress.stateProvinceGeoId)?if_exists}</option>
          <option></option>
           ${screens.render("component://common/widget/CommonScreens.xml#states")}
        </select>
      *</td>
    </tr>
    <tr>
      <td width="26%" align="right" valign="top"><div class="tabletext">${uiLabelMap.PartyZipCode}</div></td>
      <td width="5">&nbsp;</td>
      <td width="74%">
        <input type="text" class="inputBox" size="12" maxlength="10" name="postalCode" value="${(mechMap.postalAddress.postalCode)?default(request.getParameter('postalCode')?if_exists)}">
      *</td>
    </tr>
    <tr>
      <td width="26%" align="right" valign="top"><div class="tabletext">${uiLabelMap.PartyCountry}</div></td>
      <td width="5">&nbsp;</td>
      <td width="74%">
        <select name="countryGeoId" class="selectBox">
          <option>${(mechMap.postalAddress.countryGeoId)?if_exists}</option>
          <option></option>
          ${screens.render("component://common/widget/CommonScreens.xml#countries")}
        </select>
      *</td>
    </tr>
  <#elseif "TELECOM_NUMBER" = mechMap.contactMechTypeId?if_exists>
    <tr>
      <td width="26%" align="right" valign="top"><div class="tabletext">${uiLabelMap.PartyPhoneNumber}</div></td>
      <td width="5">&nbsp;</td>
      <td width="74%">
        <input type="text" class="inputBox" size="4" maxlength="10" name="countryCode" value="${(mechMap.telecomNumber.countryCode)?default(request.getParameter('countryCode')?if_exists)}">
        -&nbsp;<input type="text" class="inputBox" size="4" maxlength="10" name="areaCode" value="${(mechMap.telecomNumber.areaCode)?default(request.getParameter('areaCode')?if_exists)}">
        -&nbsp;<input type="text" class="inputBox" size="15" maxlength="15" name="contactNumber" value="${(mechMap.telecomNumber.contactNumber)?default(request.getParameter('contactNumber')?if_exists)}">
        &nbsp;ext&nbsp;<input type="text" class="inputBox" size="6" maxlength="10" name="extension" value="${(mechMap.facilityContactMech.extension)?default(request.getParameter('extension')?if_exists)}">
      </td>
    </tr>
    <tr>
      <td width="26%" align="right" valign="top"><div class="tabletext"></div></td>
      <td width="5">&nbsp;</td>
      <td><div class="tabletext">[${uiLabelMap.PartyCountryCode}] [${uiLabelMap.PartyAreaCode}] [${uiLabelMap.PartyContactNumber}] [${uiLabelMap.PartyExtension}]</div></td>
    </tr>
  <#elseif "EMAIL_ADDRESS" = mechMap.contactMechTypeId?if_exists>
    <tr>
      <td width="26%" align="right" valign="top"><div class="tabletext">${uiLabelMap.PartyEmailAddress}</div></td>
      <td width="5">&nbsp;</td>
      <td width="74%">
          <input type="text" class="inputBox" size="60" maxlength="255" name="emailAddress" value="${(mechMap.contactMech.infoString)?default(request.getParameter('emailAddress')?if_exists)}">
      *</td>
    </tr>
  <#else>
    <tr>
      <td width="26%" align="right" valign="top"><div class="tabletext">${mechMap.contactMechType.get("description",locale)}</div></td>
      <td width="5">&nbsp;</td>
      <td width="74%">
          <input type="text" class="inputBox" size="60" maxlength="255" name="infoString" value="${(mechMap.contactMech.infoString)?if_exists}">
      *</td>
    </tr>
  </#if>   
  </form>
  </table>

    &nbsp;<a href='<@ofbizUrl>authview/${donePage}?facilityId=${facilityId}</@ofbizUrl>' class="buttontext">[${uiLabelMap.CommonGoBack}]</a>
    &nbsp;<a href="javascript:document.editcontactmechform.submit()" class="buttontext">[${uiLabelMap.CommonSave}]</a>
  <#else>
    &nbsp;<a href='<@ofbizUrl>authview/${donePage}?facilityId=${facilityId}</@ofbizUrl>' class="buttontext">[${uiLabelMap.CommonGoBack}]</a>
  </#if>
</#if>
