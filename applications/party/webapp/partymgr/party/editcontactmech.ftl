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

<!-- begin editcontactmech.ftl -->
<#if !mechMap.contactMech?exists>
  <#-- When creating a new contact mech, first select the type, then actually create -->
  <#if !preContactMechTypeId?has_content>
    <h1>${uiLabelMap.PartyCreateNewContact}</h1>
    <form method="post" action="<@ofbizUrl>editcontactmech</@ofbizUrl>" name="createcontactmechform">
      <input type="hidden" name="DONE_PAGE" value="${donePage}">
      <input type="hidden" name="partyId" value="${partyId}">
      <table class="basic-table" cellspacing="0">
        <tr>
          <td class="label">${uiLabelMap.PartySelectContactType}</td>
          <td>
            <select name="preContactMechTypeId"">
              <#list mechMap.contactMechTypes as contactMechType>
                <option value="${contactMechType.contactMechTypeId}">${contactMechType.get("description",locale)}</option>
              </#list>
            </select>
            <a href="javascript:document.createcontactmechform.submit()" class="smallSubmit">${uiLabelMap.CommonCreate}</a>
          </td>
        </tr>
      </table>
    </form>
  </#if>
</#if>
<#if mechMap.contactMechTypeId?has_content>
  <#if !mechMap.contactMech?has_content>
    <h1>${uiLabelMap.PartyCreateNewContact}</h1>
    <div class="button-bar">
      <a href="<@ofbizUrl>authview/${donePage?if_exists}</@ofbizUrl>" class="smallSubmit">${uiLabelMap.CommonGoBack}</a>
      <a href="javascript:document.editcontactmechform.submit()" class="smallSubmit">${uiLabelMap.CommonSave}</a>
    </div>
    <#if contactMechPurposeType?exists>
      <p>(${uiLabelMap.PartyMsgContactHavePurpose} <b>"${contactMechPurposeType.get("description",locale)?if_exists}"</b>)</p>
    </#if>
    <table cellspacing="0">
      <form method="post" action="<@ofbizUrl>${mechMap.requestName}</@ofbizUrl>" name="editcontactmechform">
        <input type="hidden" name="DONE_PAGE" value="${donePage}">
        <input type="hidden" name="contactMechTypeId" value="${mechMap.contactMechTypeId}">
        <input type="hidden" name="partyId" value="${partyId}">        
        <#if cmNewPurposeTypeId?has_content><input type="hidden" name="contactMechPurposeTypeId" value="${cmNewPurposeTypeId}"></#if>
        <#if preContactMechTypeId?exists><input type="hidden" name="preContactMechTypeId" value="${preContactMechTypeId}"></#if>
        <#if contactMechPurposeTypeId?exists><input type="hidden" name="contactMechPurposeTypeId" value="${contactMechPurposeTypeId?if_exists}"></#if>
        <#if paymentMethodId?has_content><input type='hidden' name='paymentMethodId' value='${paymentMethodId}'></#if>
  <#else>
    <h1>${uiLabelMap.PartyEditContactInformation}</h1>
    <div class="button-bar">
      <a href="<@ofbizUrl>authview/${donePage}</@ofbizUrl>" class="smallSubmit">${uiLabelMap.CommonGoBack}</a>
      <a href="javascript:document.editcontactmechform.submit()" class="smallSubmit">${uiLabelMap.CommonSave}</a>
    </div>
    <div id="mech-purpose-types">
      <table cellspacing="0">  
      <#if mechMap.purposeTypes?has_content>
        <tr>
          <td class="label">${uiLabelMap.PartyContactPurposes}</td>
          <td>
            <table class="basic-table dark-grid" cellspacing="0">  
              <#if mechMap.partyContactMechPurposes?has_content>          
                <#list mechMap.partyContactMechPurposes as partyContactMechPurpose>
                  <#assign contactMechPurposeType = partyContactMechPurpose.getRelatedOneCache("ContactMechPurposeType")>
                  <tr>
                    <td>
                      <#if contactMechPurposeType?has_content>
                        ${contactMechPurposeType.get("description",locale)}
                      <#else>
                        ${uiLabelMap.PartyPurposeTypeNotFound}: "${partyContactMechPurpose.contactMechPurposeTypeId}"
                      </#if>
                      (${uiLabelMap.CommonSince}:${partyContactMechPurpose.fromDate.toString()})
                      <#if partyContactMechPurpose.thruDate?has_content>(${uiLabelMap.CommonExpire}: ${partyContactMechPurpose.thruDate.toString()}</#if>
                    </td>
                    <td class="button-col"><a href="<@ofbizUrl>deletePartyContactMechPurpose?partyId=${partyId}&contactMechId=${contactMechId}&contactMechPurposeTypeId=${partyContactMechPurpose.contactMechPurposeTypeId}&fromDate=${partyContactMechPurpose.fromDate.toString()}&DONE_PAGE=${donePage?replace("=","%3d")}&useValues=true</@ofbizUrl>" class="smallSubmit">${uiLabelMap.CommonDelete}</a></td>
                  </tr>
                </#list>
              </#if>              
              <tr>
                <form method="post" action="<@ofbizUrl>createPartyContactMechPurpose</@ofbizUrl>" name="newpurposeform">
                  <input type="hidden" name="partyId" value="${partyId}">
                  <input type="hidden" name="DONE_PAGE" value="${donePage}">
                  <input type="hidden" name="useValues" value="true">
                  <input type="hidden" name="contactMechId" value="${contactMechId?if_exists}">
                  <td class="button-col">
                    <select name="contactMechPurposeTypeId">
                      <option></option>
                      <#list mechMap.purposeTypes as contactMechPurposeType>
                        <option value="${contactMechPurposeType.contactMechPurposeTypeId}">${contactMechPurposeType.get("description",locale)}</option>
                      </#list>
                    </select>
                  </td>
                </form>
                <td><a href="javascript:document.newpurposeform.submit()" class="smallSubmit">${uiLabelMap.PartyAddPurpose}</a></td>
              </tr>
            </table>
          </tr>
      </#if>
      <form method="post" action="<@ofbizUrl>${mechMap.requestName}</@ofbizUrl>" name="editcontactmechform">
        <input type="hidden" name="contactMechId" value="${contactMechId}">
        <input type="hidden" name="contactMechTypeId" value="${mechMap.contactMechTypeId}">
        <input type="hidden" name="partyId" value="${partyId}">
        <input type="hidden" name="DONE_PAGE" value="${donePage?if_exists}">
  </#if>
  <#if "POSTAL_ADDRESS" = mechMap.contactMechTypeId?if_exists>
    <tr>
      <td class="label">${uiLabelMap.PartyToName}</td>
      <td>
        <input type="text" size="30" maxlength="60" name="toName" value="${(mechMap.postalAddress.toName)?default(request.getParameter('toName')?if_exists)}">
      </td>
    </tr>
    <tr>
      <td class="label">${uiLabelMap.PartyAttentionName}</td>
      <td>
        <input type="text" size="30" maxlength="60" name="attnName" value="${(mechMap.postalAddress.attnName)?default(request.getParameter('attnName')?if_exists)}">
      </td>
    </tr>
    <tr>
      <td class="label">${uiLabelMap.PartyAddressLine1}</td>
      <td>
        <input type="text" size="30" maxlength="30" name="address1" value="${(mechMap.postalAddress.address1)?default(request.getParameter('address1')?if_exists)}">
      </td>
    </tr>
    <tr>
      <td class="label">${uiLabelMap.PartyAddressLine2}</td>
      <td>
        <input type="text" size="30" maxlength="30" name="address2" value="${(mechMap.postalAddress.address2)?default(request.getParameter('address2')?if_exists)}">
      </td>
    </tr>
    <tr>
      <td class="label">${uiLabelMap.PartyCity}</td>
      <td>
        <input type="text" size="30" maxlength="30" name="city" value="${(mechMap.postalAddress.city)?default(request.getParameter('city')?if_exists)}">
      </td>
    </tr>
    <tr>
      <td class="label">${uiLabelMap.PartyState}</td>
      <td>
        <select name="stateProvinceGeoId">
          <option>${(mechMap.postalAddress.stateProvinceGeoId)?if_exists}</option>
          <option></option>
          ${screens.render("component://common/widget/CommonScreens.xml#states")}
        </select>
      </td>
    </tr>
    <tr>
      <td class="label">${uiLabelMap.PartyZipCode}</td>
      <td>
        <input type="text" size="12" maxlength="10" name="postalCode" value="${(mechMap.postalAddress.postalCode)?default(request.getParameter('postalCode')?if_exists)}">
      </td>
    </tr>
    <tr>
      <td class="label">${uiLabelMap.CommonCountry}</td>
      <td>
        <select name="countryGeoId">
          <#if (mechMap.postalAddress?exists) && (mechMap.postalAddress.countryGeoId?exists)>
            <#assign defaultCountryGeoId = (mechMap.postalAddress.countryGeoId)>
            <option selected value="${defaultCountryGeoId}">
          <#else>
           <#assign defaultCountryGeoId = Static["org.ofbiz.base.util.UtilProperties"].getPropertyValue("general.properties", "country.geo.id.default")>
           <option selected value="${defaultCountryGeoId}">
          </#if>
          <#assign countryGeo = delegator.findByPrimaryKey("Geo",Static["org.ofbiz.base.util.UtilMisc"].toMap("geoId",defaultCountryGeoId))>
          ${countryGeo.get("geoName",locale)}
          </option>
          <option></option>
          ${screens.render("component://common/widget/CommonScreens.xml#countries")}
        </select>
      </td>
    </tr>
  <#elseif "TELECOM_NUMBER" = mechMap.contactMechTypeId?if_exists>
    <tr>
      <td class="label">${uiLabelMap.PartyPhoneNumber}</td>
      <td>
        <input type="text" size="4" maxlength="10" name="countryCode" value="${(mechMap.telecomNumber.countryCode)?default(request.getParameter('countryCode')?if_exists)}">
        -&nbsp;<input type="text" size="4" maxlength="10" name="areaCode" value="${(mechMap.telecomNumber.areaCode)?default(request.getParameter('areaCode')?if_exists)}">
        -&nbsp;<input type="text" size="15" maxlength="15" name="contactNumber" value="${(mechMap.telecomNumber.contactNumber)?default(request.getParameter('contactNumber')?if_exists)}">
        &nbsp;${uiLabelMap.PartyContactExt}&nbsp;<input type="text" size="6" maxlength="10" name="extension" value="${(mechMap.partyContactMech.extension)?default(request.getParameter('extension')?if_exists)}">
      </td>
    </tr>
    <tr>
      <td class="label"></td>
      <td>[${uiLabelMap.PartyCountryCode}] [${uiLabelMap.PartyAreaCode}] [${uiLabelMap.PartyContactNumber}] [${uiLabelMap.PartyContactExt}]</td>
    </tr>
  <#elseif "EMAIL_ADDRESS" = mechMap.contactMechTypeId?if_exists>
    <tr>
      <td class="label">${mechMap.contactMechType.get("description",locale)}</td>
      <td>
        <input type="text" size="60" maxlength="255" name="emailAddress" value="${(mechMap.contactMech.infoString)?default(request.getParameter('emailAddress')?if_exists)}">
      </td>
    </tr>
  <#else>
    <tr>
      <td class="label">${mechMap.contactMechType.get("description",locale)}</td>
      <td>
        <input type="text" size="60" maxlength="255" name="infoString" value="${(mechMap.contactMech.infoString)?if_exists}">
      </td>
    </tr>
  </#if>
  <tr>
    <td class="label">${uiLabelMap.PartyContactAllowSolicitation}?</td>
    <td>
      <select name="allowSolicitation">
        <option>${(mechMap.partyContactMech.allowSolicitation)?if_exists}</option>
        <option></option><option>${uiLabelMap.CommonY}</option><option>${uiLabelMap.CommonN}</option>
      </select>
    </td>
  </tr>
  </form>
  </table>
  <div class="button-bar">
    <a href="<@ofbizUrl>authview/${donePage}</@ofbizUrl>" class="smallSubmit">${uiLabelMap.CommonGoBack}</a>
    <a href="javascript:document.editcontactmechform.submit()" class="smallSubmit">${uiLabelMap.CommonSave}</a>
  </div>
  </div>
<#else>
  <a href="<@ofbizUrl>authview/${donePage}</@ofbizUrl>" class="smallSubmit">${uiLabelMap.CommonGoBack}</a>
</#if>
<!-- end editcontactmech.ftl -->
