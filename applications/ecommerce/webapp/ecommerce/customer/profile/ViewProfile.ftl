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
        <div class="form-field">${parameters.firstName?if_exists} ${parameters.lastName?if_exists}</div>
      </div>

      <div class="form-row">
        <input type="hidden" id="updatedEmailContactMechId" name="emailContactMechId" value="${parameters.emailContactMechId}">
        <input type="hidden" id="updatedEmailAddress" name="updatedEmailAddress" value="${parameters.emailAddress}">
        <div class="form-field" id="emailAddress">${parameters.emailAddress}</div>
        <a href="mailto:${parameters.emailAddress}" class="linktext">(${uiLabelMap.PartySendEmail})</a>&nbsp;
      </div>
      <div class="form-row"><div id="serverError_${parameters.emailContactMechId}" class="errorMessage"></div></div>
    </div>

    <#-- Manage Addresses -->
    <div class="screenlet-header"><div class='boxhead'>&nbsp;${uiLabelMap.EcommerceAddressBook}</div></div>
    <div class="screenlet-body">
      <div align="right"><a class="buttontext" href="<@ofbizUrl>manageAddress</@ofbizUrl>">${uiLabelMap.EcommerceManageAddresses}</a>&nbsp;</div>
      <div class="left center">
        <div class="screenlet-header"><div class='boxhead'>${uiLabelMap.EcommercePrimary} ${uiLabelMap.OrderShippingAddress}</div></div>
        <div class="screenlet-body">
          <#if parameters.shipToContactMechId?exists>
            ${parameters.shipToAddress1?if_exists}<br/>
            <#if parameters.shipToAddress2?has_content>${parameters.shipToAddress2?if_exists}<br/></#if>
            ${parameters.shipToCity?if_exists},
            ${parameters.shipToStateProvinceGeoId?if_exists}
            ${parameters.shipToPostalCode?if_exists}<br/>
            ${parameters.shipToCountryGeoId?if_exists}<br/>
            <#if shipToTelecomNumber?has_content>
              ${shipToTelecomNumber.countryCode?if_exists}-
              ${shipToTelecomNumber.areaCode?if_exists}-
              ${shipToTelecomNumber.contactNumber?if_exists}
              <#if shipToExtension?exists>-${shipToExtension?if_exists}</#if><br/>
            </#if>
          <#else>
            ${uiLabelMap.OrderShippingAddress} ${uiLabelMap.EcommerceNotExists}
          </#if>
        </div>
      </div>

      <div class="center right">
        <div class="screenlet-header"><div class='boxhead'>&nbsp;${uiLabelMap.EcommercePrimary} ${uiLabelMap.PartyBillingAddress}</div></div>
        <div class="screenlet-body">
          <#if parameters.billToContactMechId?exists>
            ${parameters.billToAddress1?if_exists}<br/>
            <#if parameters.billToAddress2?has_content>${parameters.billToAddress2?if_exists}<br/></#if>
            ${parameters.billToCity?if_exists},
            ${parameters.billToStateProvinceGeoId?if_exists}
            ${parameters.billToPostalCode?if_exists}<br/>
            ${parameters.billToCountryGeoId?if_exists}<br/>
            <#if billToTelecomNumber?has_content>
              ${billToTelecomNumber.countryCode?if_exists}-
              ${billToTelecomNumber.areaCode?if_exists}-
              ${billToTelecomNumber.contactNumber?if_exists}
              <#if billToExtension?exists>-${billToExtension?if_exists}</#if><br/>
            </#if>
          <#else>
            ${uiLabelMap.PartyBillingAddress} ${uiLabelMap.EcommerceNotExists}
          </#if>
        </div>
      </div>
    </div>
    <div class="form-row"></div>
  </div>
</div>