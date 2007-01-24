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

<#if fulfillContactMech?has_content>

<#if "POSTAL_ADDRESS" == fulfillContactMech.contactMechTypeId>
  <#assign label = uiLabelMap.PartyAddressMailingShipping>
  <#assign postalAddress = fulfillContactMech.getRelatedOneCache("PostalAddress")?if_exists>
<#elseif "EMAIL_ADDRESS" == fulfillContactMech.contactMechTypeId>
  <#assign label = uiLabelMap.PartyToEmailAddress>
  <#assign emailAddress = fulfillContactMech.infoString?if_exists>
<#elseif "TELECOM_NUMBER" == fulfillContactMech.contactMechTypeId>
  <#assign label = uiLabelMap.PartyPhoneNumber>
  <#assign telecomNumber = fulfillContactMech.getRelatedOneCache("TelecomNumber")?if_exists>
</#if>

<div class="screenlet">
    <div class="screenlet-header">
        <div class="boxhead">&nbsp;${uiLabelMap.PartyContactInformation}</div>
    </div>
    <div class="screenlet-body">
        <table width="100%" border="0" cellpadding="1">
            <tr>
                <td align="right" valign="top" width="25%">
                  <div class="tabletext">&nbsp;<b>${label?default(uiLabelMap.PartyUnknown)}</b></div>
                </td>
                <td width="5">&nbsp;</td>
                <td align="left" valign="top" width="70%">
                    <div class="tabletext">

                      <#if emailAddress?has_content>${emailAddress}</#if>

                      <#if postalAddress?has_content>
                        <#if postalAddress.toName?has_content><b>${uiLabelMap.PartyAddrToName}:</b> ${postalAddress.toName}<br/></#if>
                        <#if postalAddress.attnName?has_content><b>${uiLabelMap.PartyAddrAttnName}:</b> ${postalAddress.attnName}<br/></#if>
                        ${postalAddress.address1?if_exists}<br/>
                        <#if postalAddress.address2?has_content>${postalAddress.address2}<br/></#if>
                        ${postalAddress.city?if_exists},
                        <#if postalAddress.stateProvinceGeoId?has_content>
                            <#assign stateProvince = postalAddress.getRelatedOneCache("StateProvinceGeo")>
                            ${stateProvince.abbreviation?default(stateProvince.geoId)}
                        </#if>
                        ${postalAddress.postalCode?if_exists}
                        <#if postalAddress.countryGeoId?has_content><br/>
                             <#assign country = postalAddress.getRelatedOneCache("CountryGeo")>
                             ${country.geoName?default(country.geoId)}
                        </#if>
                      </#if>

                      <#if telecomNumber?has_content>
                        ${telecomNumber.countryCode?if_exists}
                        <#if telecomNumber.areaCode?has_content>${telecomNumber.areaCode?default("000")}-</#if>${telecomNumber.contactNumber?default("000-0000")}
                        <#if (telecomNumber?has_content && !telecomNumber.countryCode?has_content) || telecomNumber.countryCode = "011">
                          <a target="_blank" href="http://www.anywho.com/qry/wp_rl?npa=${telecomNumber.areaCode?if_exists}&telephone=${telecomNumber.contactNumber?if_exists}&btnsubmit.x=20&btnsubmit.y=8" class="linktext">(lookup:anywho.com)</a>
                          <a target="_blank" href="http://whitepages.com/find_person_results.pl?fid=p&ac=${telecomNumber.areaCode?if_exists}&s=&p=${telecomNumber.contactNumber?if_exists}&pt=b&x=40&y=9" class="linktext">(lookup:whitepages.com)</a>
                        </#if>
                      </#if>

                    </div>
                </td>
            </tr>
        </table>
    </div>
</div>

</#if>
