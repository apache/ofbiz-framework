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

<div id="shipToServerError" class="errorMessage"></div>
<form id="editShipToPostalAddress" name="editShipToPostalAddress" method="post" action="<@ofbizUrl></@ofbizUrl>">
  <div>
    <input type="hidden" name="setShippingPurpose" value="Y"/>
    <input type="hidden" name="contactMechId" value="${parameters.shipToContactMechId?if_exists}"/>
    <#assign productStoreId = Static["org.ofbiz.product.store.ProductStoreWorker"].getProductStoreId(request)/>
    <input type="hidden" name="productStoreId" value="${productStoreId?if_exists}"/>
    <div class="form-row">
      <label>${uiLabelMap.PartyAddressLine1}*</label>
      <span>
        <input type="text" class="left required" name="address1" id="shipToAddress1" value="${parameters.shipToAddress1?if_exists}" size="30" maxlength="30">
        <span id="advice-required-shipToAddress1" style="display: none" class="errorMessage">(required)</span>
      </span>
    </div>
    <div class="form-row">
      <label>${uiLabelMap.PartyAddressLine2}</label>
      <span>
        <input type="text" class="left" name="address2" value="${parameters.shipToAddress2?if_exists}" size="30" maxlength="30">    
      </span>
    </div>
    <div class="form-row">
      <label>${uiLabelMap.PartyCity}*</label>
      <span>
        <input type="text" class="left required" name="city" id="shipToCity" value="${parameters.shipToCity?if_exists}" size="30" maxlength="30">
        <span id="advice-required-shipToCity" style="display: none" class="errorMessage">(required)</span>
      </span>
    </div>
    <div class="form-row">
      <label>${uiLabelMap.PartyZipCode}*</label>
      <span>
        <input type="text" class="left required" name="postalCode" id="shipToPostalCode" value="${parameters.shipToPostalCode?if_exists}" size="12" maxlength="10">
        <span id="advice-required-shipToPostalCode" style="display: none" class="errorMessage">(required)</span>
      </span>
    </div>
    <div class="form-row">
      <label>${uiLabelMap.PartyState}*</label>
      <span>
        <select name="stateProvinceGeoId" id="shipToStateProvinceGeoId" class="left required" style="width: 70%">
          <#if parameters.shipToStateProvinceGeoId?exists>
            <option value='${parameters.shipToStateProvinceGeoId}'>${parameters.shipToStateProvinceGeo?default(parameters.shipToStateProvinceGeoId)}</option>
          </#if>
          <option value="">${uiLabelMap.PartyNoState}</option>
          ${screens.render("component://common/widget/CommonScreens.xml#states")}
        </select>
        <span id="advice-required-shipToStateProvinceGeoId" style="display: none" class="errorMessage">(required)</span>
      </span>
    </div>
    <div class="form-row">
      <label>${uiLabelMap.PartyCountry}*</label>
      <span>
        <select name="countryGeoId" id="shipToCountryGeoId" class="left required" style="width: 70%">
          <#if parameters.shipToCountryGeoId?exists>
            <option value='${parameters.shipToCountryGeoId}'>${parameters.shipToCountryProvinceGeo?default(parameters.shipToCountryGeoId)}</option>
          </#if>
          ${screens.render("component://common/widget/CommonScreens.xml#countries")}
        </select>
        <span id="advice-required-shipToCountryGeoId" style="display: none" class="errorMessage">(required)</span>
      </span>
    </div>
    <#if shipToTelecomNumber?has_content>
      <div class="form-row">
        <div class="field-label">
          <label for="phoneNumber_${shipToTelecomNumber.contactMechId}">${uiLabelMap.PartyPhoneNumber}*</label>
        </div>
        <div>
          <input type="hidden" name="phoneContactMechId" value="${shipToTelecomNumber.contactMechId?if_exists}"/>
          <input type="text" name="countryCode" id="countryCode_${shipToTelecomNumber.contactMechId}" class="required" value="${shipToTelecomNumber.countryCode?if_exists}" size="3" maxlength="3"/>
          - <input type="text" name="areaCode" id="areaCode_${shipToTelecomNumber.contactMechId}" class="required" value="${shipToTelecomNumber.areaCode?if_exists}" size="3" maxlength="3"/>
          - <input type="text" name="contactNumber" id="contactNumber_${shipToTelecomNumber.contactMechId}" class="required" value="${contactNumber?default("${shipToTelecomNumber.contactNumber?if_exists}")}" size="6" maxlength="7"/>
          - <input type="text" name="extension" id="extension_${shipToTelecomNumber.contactMechId}" value="${extension?default("${shipToExtension?if_exists}")}" size="3" maxlength="3"/>
        </div>
      </div>    
    </#if>
    <div class="form-row">
      <b>${uiLabelMap.EcommerceMyDefaultBillingAddress}</b>
      <input type="checkbox" name="setBillingPurpose" value="Y" <#if setBillingPurpose?exists>checked</#if>/>
    </div>
    <div class="form-row">
      <a name="submitEditShipToPostalAddress" id="submitEditShipToPostalAddress" class="buttontext" onclick="updatePartyShipToPostalAddress('submitEditShipToPostalAddress')">${uiLabelMap.CommonSubmit}</a>
      <form action="">
        <input class="popup_closebox buttontext" type="button" value="${uiLabelMap.CommonClose}"/>
      </form>
    </div>
  </div>
</form>
