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
  <h3>${uiLabelMap.PartyContactInfoNotBelongToYou}.</h3>
  <a href="<@ofbizUrl>${donePage}</@ofbizUrl>" class="button">${uiLabelMap.CommonBack}</a>
<#else>
  <#if !contactMech??>
    <#-- When creating a new contact mech, first select the type, then actually create -->
    <#if !requestParameters.preContactMechTypeId?? && !preContactMechTypeId??>
    <h2>${uiLabelMap.PartyCreateNewContactInfo}</h2>
    <form method="post" action='<@ofbizUrl>editcontactmechnosave</@ofbizUrl>' name="createcontactmechform">
      <div>
      <table width="90%" border="0" cellpadding="2" cellspacing="0">
        <tr>
          <td>${uiLabelMap.PartySelectContactType}:</td>
          <td>
            <select name="preContactMechTypeId" class='selectBox'>
              <#list contactMechTypes as contactMechType>
                <option value='${contactMechType.contactMechTypeId}'>${contactMechType.get("description",locale)}</option>
              </#list>
            </select>&nbsp;<a href="javascript:document.createcontactmechform.submit()" class="button">${uiLabelMap.CommonCreate}</a>
          </td>
        </tr>
      </table>
      </div>
    </form>
    <#-- <p><h3>ERROR: Contact information with ID "${contactMechId}" not found!</h3></p> -->
    </#if>
  </#if>

  <#if contactMechTypeId??>
    <#if !contactMech??>
      <h2>${uiLabelMap.PartyCreateNewContactInfo}</h2>
      <a href='<@ofbizUrl>${donePage}</@ofbizUrl>' class="button">${uiLabelMap.CommonGoBack}</a>
      <a href="javascript:document.editcontactmechform.submit()" class="button">${uiLabelMap.CommonSave}</a>
      <table width="90%" border="0" cellpadding="2" cellspacing="0">
        <form method="post" action='<@ofbizUrl>${reqName}</@ofbizUrl>' name="editcontactmechform" id="editcontactmechform">
        <div>
          <input type='hidden' name='contactMechTypeId' value='${contactMechTypeId}' />
          <#if contactMechPurposeType??>
            <div>(${uiLabelMap.PartyNewContactHavePurpose} "${contactMechPurposeType.get("description",locale)!}")</div>
          </#if>
          <#if cmNewPurposeTypeId?has_content><input type='hidden' name='contactMechPurposeTypeId' value='${cmNewPurposeTypeId}' /></#if>
          <#if preContactMechTypeId?has_content><input type='hidden' name='preContactMechTypeId' value='${preContactMechTypeId}' /></#if>
          <#if paymentMethodId?has_content><input type='hidden' name='paymentMethodId' value='${paymentMethodId}' /></#if>
    <#else>
      <h2>${uiLabelMap.PartyEditContactInfo}</h2>      
      <a href="<@ofbizUrl>${donePage}</@ofbizUrl>" class="button">${uiLabelMap.CommonGoBack}</a>
      <a href="javascript:document.editcontactmechform.submit()" class="button">${uiLabelMap.CommonSave}</a>
      <table width="90%" border="0" cellpadding="2" cellspacing="0">
        <tr>
          <td align="right" valign="top">${uiLabelMap.PartyContactPurposes}</td>
          <td>&nbsp;</td>
          <td>
            <table border="0" cellspacing="1">
              <#list partyContactMechPurposes! as partyContactMechPurpose>
                <#assign contactMechPurposeType = partyContactMechPurpose.getRelatedOne("ContactMechPurposeType", true) />
                <tr>
                  <td>
                    <#if contactMechPurposeType??>
                      ${contactMechPurposeType.get("description",locale)}
                    <#else>
                      ${uiLabelMap.PartyPurposeTypeNotFound}: "${partyContactMechPurpose.contactMechPurposeTypeId}"
                    </#if>
                     (${uiLabelMap.CommonSince}:${partyContactMechPurpose.fromDate.toString()})
                    <#if partyContactMechPurpose.thruDate??>(${uiLabelMap.CommonExpires}:${partyContactMechPurpose.thruDate.toString()})</#if>
                  </td>
                  <td>
                      <form name="deletePartyContactMechPurpose_${partyContactMechPurpose.contactMechPurposeTypeId}" method="post" action="<@ofbizUrl>deletePartyContactMechPurpose</@ofbizUrl>">
                        <div>
                          <input type="hidden" name="contactMechId" value="${contactMechId}"/>
                          <input type="hidden" name="contactMechPurposeTypeId" value="${partyContactMechPurpose.contactMechPurposeTypeId}"/>
                          <input type="hidden" name="fromDate" value="${partyContactMechPurpose.fromDate}"/>
                          <input type="hidden" name="useValues" value="true"/>
                          <a href='javascript:document.deletePartyContactMechPurpose_${partyContactMechPurpose.contactMechPurposeTypeId}.submit()' class='button'>&nbsp;${uiLabelMap.CommonDelete}&nbsp;</a>
                        </div>
                      </form> 
                  </td>
                </tr>
              </#list>
              <#if purposeTypes?has_content>
              <tr>
                <td>
                  <form method="post" action='<@ofbizUrl>createPartyContactMechPurpose</@ofbizUrl>' name='newpurposeform'>
                    <div>
                    <input type="hidden" name="contactMechId" value="${contactMechId}"/>
                    <input type="hidden" name="useValues" value="true"/>
                      <select name='contactMechPurposeTypeId' class='selectBox'>
                        <option></option>
                        <#list purposeTypes as contactMechPurposeType>
                          <option value='${contactMechPurposeType.contactMechPurposeTypeId}'>${contactMechPurposeType.get("description",locale)}</option>
                        </#list>
                      </select>
                      </div>
                  </form>
                </td>
                <td><a href='javascript:document.newpurposeform.submit()' class='button'>${uiLabelMap.PartyAddPurpose}</a></td>
              </tr>
              </#if>
            </table>
          </td>
        </tr>
        <form method="post" action='<@ofbizUrl>${reqName}</@ofbizUrl>' name="editcontactmechform" id="editcontactmechform">
          <div>
          <input type="hidden" name="contactMechId" value='${contactMechId}' />
          <input type="hidden" name="contactMechTypeId" value='${contactMechTypeId}' />
    </#if>

    <#if contactMechTypeId = "POSTAL_ADDRESS">
      <tr>
        <td align="right" valign="top">${uiLabelMap.PartyToName}</td>
        <td>&nbsp;</td>
        <td>
          <input type="text" class='inputBox' size="30" maxlength="60" name="toName" value="${postalAddressData.toName!}" />
        </td>
      </tr>
      <tr>
        <td align="right" valign="top">${uiLabelMap.PartyAttentionName}</td>
        <td>&nbsp;</td>
        <td>
          <input type="text" class='inputBox' size="30" maxlength="60" name="attnName" value="${postalAddressData.attnName!}" />
        </td>
      </tr>
      <tr>
        <td align="right" valign="top">${uiLabelMap.PartyAddressLine1}</td>
        <td>&nbsp;</td>
        <td>
          <input type="text" class='inputBox' size="30" maxlength="30" name="address1" value="${postalAddressData.address1!}" />
        *</td>
      </tr>
      <tr>
        <td align="right" valign="top">${uiLabelMap.PartyAddressLine2}</td>
        <td>&nbsp;</td>
        <td>
            <input type="text" class='inputBox' size="30" maxlength="30" name="address2" value="${postalAddressData.address2!}" />
        </td>
      </tr>
      <tr>
        <td align="right" valign="top">${uiLabelMap.PartyCity}</td>
        <td>&nbsp;</td>
        <td>
            <input type="text" class='inputBox' size="30" maxlength="30" name="city" value="${postalAddressData.city!}" />
        *</td>
      </tr>
      <tr>
        <td align="right" valign="top"> ${uiLabelMap.PartyState}
        <td>&nbsp;</td>
        <td>       
          <select name="stateProvinceGeoId" id="editcontactmechform_stateProvinceGeoId">
          </select>
        </td>
      </tr>      
      <tr>
        <td align="right" valign="top">${uiLabelMap.PartyZipCode}</td>
        <td >&nbsp;</td>
        <td>
          <input type="text" class='inputBox' size="12" maxlength="10" name="postalCode" value="${postalAddressData.postalCode!}" />
        *</td>
      </tr>
      <tr>   
        <td align="right" valign="top">${uiLabelMap.CommonCountry}</td>
        <td>&nbsp;</td>
        <td>
          <select name="countryGeoId" id="editcontactmechform_countryGeoId">
          ${screens.render("component://common/widget/CommonScreens.xml#countries")}        
          <#if (postalAddress??) && (postalAddress.countryGeoId??)>
            <#assign defaultCountryGeoId = postalAddress.countryGeoId>
          <#else>
            <#assign defaultCountryGeoId = Static["org.ofbiz.entity.util.EntityUtilProperties"].getPropertyValue("general", "country.geo.id.default", delegator)>
          </#if>
          <option selected="selected" value="${defaultCountryGeoId}">
          <#assign countryGeo = delegator.findOne("Geo",Static["org.ofbiz.base.util.UtilMisc"].toMap("geoId",defaultCountryGeoId), false)>
            ${countryGeo.get("geoName",locale)}
          </option>
          </select>
        </td>
      </tr>
    <#elseif contactMechTypeId = "TELECOM_NUMBER">
      <tr>
        <td align="right" valign="top">${uiLabelMap.PartyPhoneNumber}</td>
        <td>&nbsp;</td>
        <td>
          <input type="text" class='inputBox' size="4" maxlength="10" name="countryCode" value="${telecomNumberData.countryCode!}" />
          -&nbsp;<input type="text" class='inputBox' size="4" maxlength="10" name="areaCode" value="${telecomNumberData.areaCode!}" />
          -&nbsp;<input type="text" class='inputBox' size="15" maxlength="15" name="contactNumber" value="${telecomNumberData.contactNumber!}" />
          &nbsp;${uiLabelMap.PartyExtension}&nbsp;<input type="text" class='inputBox' size="6" maxlength="10" name="extension" value="${partyContactMechData.extension!}" />
        </td>
      </tr>
      <tr>
        <td align="right" valign="top"></td>
        <td>&nbsp;</td>
        <td>[${uiLabelMap.CommonCountryCode}] [${uiLabelMap.PartyAreaCode}] [${uiLabelMap.PartyContactNumber}] [${uiLabelMap.PartyExtension}]</td>
      </tr>
    <#elseif contactMechTypeId = "EMAIL_ADDRESS">
      <tr>
        <td align="right" valign="top">${uiLabelMap.PartyEmailAddress}</td>
        <td>&nbsp;</td>
        <td>
          <input type="text" class='inputBox' size="60" maxlength="255" name="emailAddress" value="<#if tryEntity>${contactMech.infoString!}<#else>${requestParameters.emailAddress!}</#if>" />
        *</td>
      </tr>
    <#else>
      <tr>
        <td align="right" valign="top">${contactMechType.get("description",locale)!}</td>
        <td>&nbsp;</td>
        <td>
            <input type="text" class='inputBox' size="60" maxlength="255" name="infoString" value="${contactMechData.infoString!}" />
        *</td>
      </tr>
    </#if>
      <tr>
        <td align="right" valign="top">${uiLabelMap.PartyAllowSolicitation}?</td>
        <td>&nbsp;</td>
        <td>
          <select name="allowSolicitation" class='selectBox'>
            <#if (((partyContactMechData.allowSolicitation)!"") == "Y")><option value="Y">${uiLabelMap.CommonY}</option></#if>
            <#if (((partyContactMechData.allowSolicitation)!"") == "N")><option value="N">${uiLabelMap.CommonN}</option></#if>
            <option></option>
            <option value="Y">${uiLabelMap.CommonY}</option>
            <option value="N">${uiLabelMap.CommonN}</option>
          </select>
        </td>
      </tr>
      </div>
    </form>
  </table>

  <a href="<@ofbizUrl>${donePage}</@ofbizUrl>" class="button">${uiLabelMap.CommonGoBack}</a>
  <a href="javascript:document.editcontactmechform.submit()" class="button">${uiLabelMap.CommonSave}</a>
  <#else>    
    <a href="<@ofbizUrl>${donePage}</@ofbizUrl>" class="button">${uiLabelMap.CommonGoBack}</a>
  </#if>
</#if>
