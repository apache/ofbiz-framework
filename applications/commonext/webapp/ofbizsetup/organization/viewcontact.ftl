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

  <div id="partyContactInfo" class="screenlet">
    <div class="screenlet-title-bar">
      <ul>
        <li class="h3">${uiLabelMap.PartyContactInformation}</li>
        <#--if security.hasEntityPermission("PARTYMGR", "_CREATE", session) || userLogin.partyId == partyId>
          <li><a href="<@ofbizUrl>editcontactmech?partyId=${partyId}</@ofbizUrl>">${uiLabelMap.CommonCreateNew}</a></li>
        </#if-->
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
            <tr><td colspan="4"><hr /></td></tr>
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
                      (${uiLabelMap.CommonExpire}: ${partyContactMechPurpose.thruDate})
                    </#if>
                  </div>
                </#list>
                <#if "POSTAL_ADDRESS" = contactMech.contactMechTypeId>
                  <#assign postalAddress = contactMechMap.postalAddress>
                  <#if postalAddress?has_content>
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
                  </#if>
                  <#if (postalAddress?has_content && !postalAddress.countryGeoId?has_content) || postalAddress.countryGeoId = "USA">
                    <#assign addr1 = postalAddress.address1?if_exists>
                    <#if addr1?has_content && (addr1.indexOf(" ") > 0)>
                      <#assign addressNum = addr1.substring(0, addr1.indexOf(" "))>
                      <#assign addressOther = addr1.substring(addr1.indexOf(" ")+1)>
                      <a target="_blank" href="${uiLabelMap.CommonLookupWhitepagesAddressLink}" class="buttontext">${uiLabelMap.CommonLookupWhitepages}</a>
                    </#if>
                  </#if>
                  <#if postalAddress.geoPointId?has_content>
                    <#if contactMechPurposeType?has_content>
                      <#assign popUptitle = contactMechPurposeType.get("description",locale) + uiLabelMap.CommonGeoLocation>
                    </#if>
                    <a href="javascript:popUp('<@ofbizUrl>geoLocation?geoPointId=${postalAddress.geoPointId}</@ofbizUrl>', '${popUptitle?if_exists}', '450', '550')" class="buttontext">${uiLabelMap.CommonGeoLocation}</a>
                  </#if>
                <#elseif "TELECOM_NUMBER" = contactMech.contactMechTypeId>
                  <#assign telecomNumber = contactMechMap.telecomNumber>
                  <div>
                    ${telecomNumber.countryCode?if_exists}
                    <#if telecomNumber.areaCode?has_content>${telecomNumber.areaCode?default("000")}-</#if>${telecomNumber.contactNumber?default("000-0000")}
                    <#if partyContactMech.extension?has_content>${uiLabelMap.PartyContactExt}&nbsp;${partyContactMech.extension}</#if>
                    <#if (telecomNumber?has_content && !telecomNumber.countryCode?has_content) || telecomNumber.countryCode = "011">
                      <a target="_blank" href="${uiLabelMap.CommonLookupAnywhoLink}" class="buttontext">${uiLabelMap.CommonLookupAnywho}</a>
                      <a target="_blank" href="${uiLabelMap.CommonLookupWhitepagesTelNumberLink}" class="buttontext">${uiLabelMap.CommonLookupWhitepages}</a>
                    </#if>
                  </div>
                <#elseif "EMAIL_ADDRESS" = contactMech.contactMechTypeId>
                  <div>
                    ${contactMech.infoString?if_exists}
                    <#--a href="<@ofbizUrl>EditCommunicationEvent?partyIdFrom=${userLogin.partyId}&amp;partyIdTo=${partyId}&amp;communicationEventTypeId=EMAIL_COMMUNICATION&amp;contactMechIdTo=${contactMech.contactMechId}&amp;contactMechTypeId=EMAIL_ADDRESS<#if thisUserPrimaryEmail?has_content>&amp;contactMechIdFrom=${thisUserPrimaryEmail.contactMechId}</#if></@ofbizUrl>" class="buttontext">${uiLabelMap.CommonSendEmail}</a-->
                  </div>
                <#elseif "WEB_ADDRESS" = contactMech.contactMechTypeId>
                  <div>
                    ${contactMech.infoString?if_exists}
                    <#assign openAddress = contactMech.infoString?default("")>
                    <#if !openAddress?starts_with("http") && !openAddress?starts_with("HTTP")><#assign openAddress = "http://" + openAddress></#if>
                    <a target="_blank" href="${openAddress}" class="buttontext">${uiLabelMap.CommonOpenPageNewWindow}</a>
                  </div>
                <#else>
                  <div>${contactMech.infoString?if_exists}</div>
                </#if>
                <div>(${uiLabelMap.CommonUpdated}:&nbsp;${partyContactMech.fromDate})</div>
                <#if partyContactMech.thruDate?has_content><div><b>${uiLabelMap.PartyContactEffectiveThru}:&nbsp;${partyContactMech.thruDate}</b></div></#if>
                <#-- create cust request -->
                <#if custRequestTypes?exists>
                  <form name="createCustRequestForm" action="<@ofbizUrl>createCustRequest</@ofbizUrl>" method="post" onsubmit="javascript:submitFormDisableSubmits(this)">
                    <input type="hidden" name="partyId" value="${partyId}"/>
                    <input type="hidden" name="fromPartyId" value="${partyId}"/>
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
              <td class="button-col">
                <#--if security.hasEntityPermission("PARTYMGR", "_UPDATE", session) || userLogin.partyId == partyId>
                  <a href="<@ofbizUrl>editcontactmech?partyId=${partyId}&amp;contactMechId=${contactMech.contactMechId}</@ofbizUrl>">${uiLabelMap.CommonUpdate}</a>
                </#if>
                <#if security.hasEntityPermission("PARTYMGR", "_DELETE", session) || userLogin.partyId == partyId>
                  <form name="partyDeleteContact" method="post" action="<@ofbizUrl>deleteContactMech</@ofbizUrl>" onsubmit="javascript:submitFormDisableSubmits(this)">
                    <input name="partyId" value="${partyId}" type="hidden"/>
                    <input name="contactMechId" value="${contactMech.contactMechId}" type="hidden"/>
                    <input type="submit" class="smallSubmit" value="${uiLabelMap.CommonExpire}"/>
                  </form>
                </#if-->
              </td>
            </tr>
          </#list>
        </table>
      <#else>
        ${uiLabelMap.PartyNoContactInformation}
      </#if>
    </div>
  </div>