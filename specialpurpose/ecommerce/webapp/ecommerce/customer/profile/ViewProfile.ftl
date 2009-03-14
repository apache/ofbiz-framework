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
  <div class="screenlet-header">
    <div class="boxhead">&nbsp;${uiLabelMap.EcommerceMyAccount}</div>
  </div>
  <div class="screenlet-body">
    <div align="right"><a class="buttontext" href="<@ofbizUrl>editProfile</@ofbizUrl>">${uiLabelMap.EcommerceEditProfile}</a>&nbsp;</div><br/>
    <div class="screenlet-header"><div class="boxhead">&nbsp;${uiLabelMap.PartyContactInformation}</div></div>
    <div class="screenlet-body">
      <div class="form-row">
        <div class="form-field">${firstName?if_exists} ${lastName?if_exists}</div>
      </div>

      <div class="form-row">
        <input type="hidden" id="updatedEmailContactMechId" name="emailContactMechId" value="${emailContactMechId}">
        <input type="hidden" id="updatedEmailAddress" name="updatedEmailAddress" value="${emailAddress}">
        <div class="form-field" id="emailAddress">${emailAddress}</div>
        <a href="mailto:${emailAddress}" class="linktext">(${uiLabelMap.PartySendEmail})</a>&nbsp;
      </div>
      <div class="form-row"><div id="serverError_${emailContactMechId}" class="errorMessage"></div></div>
    </div>

    <#-- Manage Addresses -->
    <div class="screenlet-header"><div class='boxhead'>&nbsp;${uiLabelMap.EcommerceAddressBook}</div></div>
    <div class="screenlet-body">
      <div align="right"><a class="buttontext" href="<@ofbizUrl>manageAddress</@ofbizUrl>">${uiLabelMap.EcommerceManageAddresses}</a>&nbsp;</div>
      <div class="left center">
        <div class="screenlet-header"><div class='boxhead'>${uiLabelMap.EcommercePrimaryShippingAddress}</div></div>
        <div class="screenlet-body">
          <#if shipToContactMechId?exists>
            ${shipToAddress1?if_exists}<br/>
            <#if shipToAddress2?has_content>${shipToAddress2?if_exists}<br/></#if>
            <#if shipToStateProvinceGeoId?has_content && shipToStateProvinceGeoId != "_NA_">
              ${shipToStateProvinceGeoId}
            </#if>
              ${shipToCity?if_exists},
              ${shipToPostalCode?if_exists}<br/>
              ${shipToCountryGeoId?if_exists}<br/>
            <#if shipToTelecomNumber?has_content>
              ${shipToTelecomNumber.countryCode?if_exists}-
              ${shipToTelecomNumber.areaCode?if_exists}-
              ${shipToTelecomNumber.contactNumber?if_exists}
              <#if shipToExtension?exists>-${shipToExtension?if_exists}</#if><br/>
            </#if>
          <#else>
            ${uiLabelMap.PartyPostalInformationNotFound}
          </#if>
        </div>
      </div>

      <div class="center right">
        <div class="screenlet-header"><div class='boxhead'>&nbsp;${uiLabelMap.EcommercePrimaryBillingAddress}</div></div>
        <div class="screenlet-body">
          <#if billToContactMechId?exists>
            ${billToAddress1?if_exists}<br/>
            <#if billToAddress2?has_content>${billToAddress2?if_exists}<br/></#if>
            <#if billToStateProvinceGeoId?has_content && billToStateProvinceGeoId != "_NA_">
              ${billToStateProvinceGeoId}
            </#if>
              ${billToCity?if_exists},
              ${billToPostalCode?if_exists}<br/>
              ${billToCountryGeoId?if_exists}<br/>
            <#if billToTelecomNumber?has_content>
              ${billToTelecomNumber.countryCode?if_exists}-
              ${billToTelecomNumber.areaCode?if_exists}-
              ${billToTelecomNumber.contactNumber?if_exists}
              <#if billToExtension?exists>-${billToExtension?if_exists}</#if><br/>
            </#if>
          <#else>
            ${uiLabelMap.PartyPostalInformationNotFound}
          </#if>
        </div>
      </div>
    </div>
    <div class="form-row"></div>
  </div>
</div>