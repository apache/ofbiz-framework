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

<div class="screenlet">
  <h3>${uiLabelMap.EcommerceMyAccount}</h3>
  <div class="screenlet-body clearfix">
    <div>
      <a class="button" href="<@ofbizUrl>editProfile</@ofbizUrl>">${uiLabelMap.EcommerceEditProfile}</a>
      <h3>${uiLabelMap.PartyContactInformation}</h3>
      <label>${firstName?if_exists} ${lastName?if_exists}</label>
      <input type="hidden" id="updatedEmailContactMechId" name="emailContactMechId" value="${emailContactMechId?if_exists}" />
      <input type="hidden" id="updatedEmailAddress" name="updatedEmailAddress" value="${emailAddress?if_exists}" />
      <#if emailAddress?exists>
        <label>${emailAddress?if_exists}</label>
        <a href="mailto:${emailAddress?if_exists}" class="linktext">(${uiLabelMap.PartySendEmail})</a>
      </#if>
      <div id="serverError_${emailContactMechId?if_exists}" class="errorMessage"></div>
    </div>
    <#-- Manage Addresses -->
    <div>
      <a class="button" href="<@ofbizUrl>manageAddress</@ofbizUrl>">${uiLabelMap.EcommerceManageAddresses}</a>
      <h3>${uiLabelMap.EcommerceAddressBook}</h3>
      <div class="left center">
        <h3>${uiLabelMap.EcommercePrimaryShippingAddress}</h3>
          <ul>
          <#if shipToContactMechId?exists>
            <li>${shipToAddress1?if_exists}</li>
            <#if shipToAddress2?has_content><li>${shipToAddress2?if_exists}</li></#if>
            <li>
              <ul>
                <li>
                  <#if shipToStateProvinceGeoId?has_content && shipToStateProvinceGeoId != "_NA_">
                    ${shipToStateProvinceGeoId}
                  </#if>
                  ${shipToCity?if_exists},
                  ${shipToPostalCode?if_exists}
                </li>
                <li>${shipToCountryGeoId?if_exists}</li>
              </ul>
            </li>
            <#if shipToTelecomNumber?has_content>
            <li>
              ${shipToTelecomNumber.countryCode?if_exists}-
              ${shipToTelecomNumber.areaCode?if_exists}-
              ${shipToTelecomNumber.contactNumber?if_exists}
              <#if shipToExtension?exists>-${shipToExtension?if_exists}</#if>
            </li>
            </#if>
          <#else>
            <li>${uiLabelMap.PartyPostalInformationNotFound}</li>
          </#if>
          </ul>
      </div>
      <div class="right center">
        <h3>${uiLabelMap.EcommercePrimaryBillingAddress}</h3>
          <ul>
          <#if billToContactMechId?exists>
            <li>${billToAddress1?if_exists}</li>
            <#if billToAddress2?has_content><li>${billToAddress2?if_exists}</li></#if>
            <li>
              <ul>
                <li>
                  <#if billToStateProvinceGeoId?has_content && billToStateProvinceGeoId != "_NA_">
                    ${billToStateProvinceGeoId}
                  </#if>
                  ${billToCity?if_exists},
                  ${billToPostalCode?if_exists}
                </li>
                <li>${billToCountryGeoId?if_exists}</li>
              </ul>
            </li>
            <#if billToTelecomNumber?has_content>
            <li>
              ${billToTelecomNumber.countryCode?if_exists}-
              ${billToTelecomNumber.areaCode?if_exists}-
              ${billToTelecomNumber.contactNumber?if_exists}
              <#if billToExtension?exists>-${billToExtension?if_exists}</#if>
            </li>
            </#if>
          <#else>
            <li>${uiLabelMap.PartyPostalInformationNotFound}</li>
          </#if>
          </ul>
      </div>
    </div>
  </div>
</div>