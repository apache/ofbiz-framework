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
<#escape x as x?xml>
  <fo:block>${postalAddress.address1!}</fo:block>
  <#if postalAddress.address2?has_content><fo:block>${postalAddress.address2}</fo:block></#if>
  <fo:block>${postalAddress.city!}<#if postalAddress.stateProvinceGeoId?has_content>, ${postalAddress.stateProvinceGeoId}</#if> ${postalAddress.postalCode!}</fo:block>
  <#if postalAddress.countryGeoId?has_content>
    <fo:block>
      <#assign country = postalAddress.getRelatedOne("CountryGeo", true)>
      ${country.get("geoName", locale)?default(country.geoId)}
    </fo:block>
  </#if>
</#escape>

