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
    <div align="right"><a class="buttontext" href="<@ofbizUrl>editProfile</@ofbizUrl>">${uiLabelMap.CommonEdit} ${uiLabelMap.CommonProfile}</a>&nbsp;</div><br/>
    <div class="screenlet-header"><div class="boxhead">&nbsp;${uiLabelMap.PartyContactInformation}</div></div>
    <div class="screenlet-body">
      <div class="form-row">
        <div class="form-field">${parameters.firstName?if_exists} ${parameters.lastName?if_exists}</div>
      </div>

      <#assign emailContactMech = delegator.findOne("ContactMech", Static["org.ofbiz.base.util.UtilMisc"].toMap("contactMechId", parameters.emailContactMechId), true)>
      <#assign emailContactMechType = emailContactMech.getRelatedOneCache("ContactMechType")>

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
            ${parameters.shipToAddress2?if_exists}<br/>
            ${parameters.shipToCity?if_exists},
            ${parameters.shipToStateProvinceGeoId?if_exists}
            ${parameters.shipToPostalCode?if_exists}<br/>
            ${parameters.shipToCountryGeoId?if_exists}<br/>
            <#assign pcmps = Static["org.ofbiz.entity.util.EntityUtil"].filterByDate(party.getRelatedByAnd("PartyContactMechPurpose", Static["org.ofbiz.base.util.UtilMisc"].toMap("contactMechPurposeTypeId", "PHONE_SHIPPING")))>
            <#if pcmps?has_content>
              <#assign pcmp = Static["org.ofbiz.entity.util.EntityUtil"].getFirst(pcmps)/>
              <#assign telecomNumber = pcmp.getRelatedOne("TelecomNumber")/>
            </#if>
            <#if telecomNumber?has_content>
              <#assign pcm = Static["org.ofbiz.entity.util.EntityUtil"].getFirst(telecomNumber.getRelated("PartyContactMech"))/>
              ${telecomNumber.countryCode?if_exists}-
              ${telecomNumber.areaCode?if_exists}-
              ${telecomNumber.contactNumber?if_exists}-
              ${pcm.extension?if_exists}
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
            ${parameters.billToAddress2?if_exists}<br/>
            ${parameters.billToCity?if_exists},
            ${parameters.billToStateProvinceGeoId?if_exists}
            ${parameters.billToPostalCode?if_exists}<br/>
            ${parameters.billToCountryGeoId?if_exists}<br/>
            <#assign pcmps = Static["org.ofbiz.entity.util.EntityUtil"].filterByDate(party.getRelatedByAnd("PartyContactMechPurpose", Static["org.ofbiz.base.util.UtilMisc"].toMap("contactMechPurposeTypeId", "PHONE_BILLING")))>
            <#if pcmps?has_content>
              <#assign pcmp = Static["org.ofbiz.entity.util.EntityUtil"].getFirst(pcmps)/>
              <#assign telecomNumber = pcmp.getRelatedOne("TelecomNumber")/>
            </#if>
            <#if telecomNumber?has_content>
              <#assign pcm = Static["org.ofbiz.entity.util.EntityUtil"].getFirst(telecomNumber.getRelated("PartyContactMech"))/>
              ${telecomNumber.countryCode?if_exists}-
              ${telecomNumber.areaCode?if_exists}-
              ${telecomNumber.contactNumber?if_exists}-
              ${pcm.extension?if_exists}
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