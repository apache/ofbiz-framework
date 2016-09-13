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
  <div>
    <#if postalAddress.toName?has_content><b>${uiLabelMap.PartyAddrToName}:</b> ${postalAddress.toName}<br /></#if>
    <#if postalAddress.attnName?has_content><b>${uiLabelMap.PartyAddrAttnName}:</b> ${postalAddress.attnName}<br /></#if>
    ${postalAddress.address1!}<br />
    <#if postalAddress.address2?has_content>${postalAddress.address2}<br /></#if>
    ${postalAddress.city!},
    <#if postalAddress.stateProvinceGeoId?has_content>
      <#assign stateProvince = postalAddress.getRelatedOne("StateProvinceGeo", true)>
      ${stateProvince.geoName?default(stateProvince.abbreviation)}
    </#if>
    <#if postalAddress.postalCodeGeoId?has_content>
      <#assign postalCodeGeo = postalAddress.getRelatedOne("PostalCodeGeo", true)>
      ${postalCodeGeo.geoName}
    <#else>
      ${postalAddress.postalCode!}
    </#if>
    <#if postalAddress.countryGeoId?has_content><br />
      <#assign country = postalAddress.getRelatedOne("CountryGeo", true)>
      ${country.get("geoName", locale)?default(country.geoId)}
    </#if>
    </div>
    <#if !postalAddress.countryGeoId?has_content>
    <#assign addr1 = postalAddress.address1!>
    <#if addr1?has_content && (addr1.indexOf(" ") > 0)>
      <#assign addressNum = addr1.substring(0, addr1.indexOf(" "))>
      <#assign addressOther = addr1.substring(addr1.indexOf(" ")+1)>
      <a target="_blank" href="${uiLabelMap.CommonLookupWhitepagesAddressLink}" class="buttontext">${uiLabelMap.CommonLookupWhitepages}</a>
    </#if>
  </#if>

