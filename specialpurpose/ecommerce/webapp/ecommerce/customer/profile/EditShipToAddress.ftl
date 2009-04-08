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
    <input type="hidden" name="contactMechId" value="${shipToContactMechId?if_exists}"/>
    <#assign productStoreId = Static["org.ofbiz.product.store.ProductStoreWorker"].getProductStoreId(request)/>
    <input type="hidden" name="productStoreId" value="${productStoreId?if_exists}"/>
    <div class="form-row">
      <label>${uiLabelMap.PartyAddressLine1}*</label>
      <span>
        <input type="text" class="left required" name="address1" id="shipToAddress1" value="${shipToAddress1?if_exists}" size="30" maxlength="30">
        <span id="advice-required-shipToAddress1" style="display: none" class="errorMessage">(required)</span>
      </span>
    </div>
    <div class="form-row">
      <label>${uiLabelMap.PartyAddressLine2}</label>
      <span>
        <input type="text" class="left" name="address2" value="${shipToAddress2?if_exists}" size="30" maxlength="30">
      </span>
    </div>
    <div class="form-row">
      <label>${uiLabelMap.PartyCity}*</label>
      <span>
        <input type="text" class="left required" name="city" id="shipToCity" value="${shipToCity?if_exists}" size="30" maxlength="30">
        <span id="advice-required-shipToCity" style="display: none" class="errorMessage">(required)</span>
      </span>
    </div>
    <div class="form-row">
      <label>${uiLabelMap.PartyZipCode}*</label>
      <span>
        <input type="text" class="left required" name="postalCode" id="shipToPostalCode" value="${shipToPostalCode?if_exists}" size="12" maxlength="10">
        <span id="advice-required-shipToPostalCode" style="display: none" class="errorMessage">(required)</span>
      </span>
    </div>
    <div class="form-row">
      <label>${uiLabelMap.PartyCountry}*</label>
      <span>
        <select name="countryGeoId" id="shipToCountryGeoId" class="left required" style="width: 70%">
          <#if shipToCountryGeoId??>
            <option value="${shipToCountryGeoId!}">${shipToCountryProvinceGeo!(shipToCountryGeoId!)}</option>
          </#if>
          ${screens.render("component://common/widget/CommonScreens.xml#countries")}
        </select>
        <span id="advice-required-shipToCountryGeoId" style="display: none" class="errorMessage">(required)</span>
      </span>
    </div>
    <div id="shipToStates" class="form-row">
      <label>${uiLabelMap.PartyState}*<span id="advice-required-shipToStateProvinceGeoId" style="display: none" class="errorMessage">(required)</span></label>
      <span>
        <select name="stateProvinceGeoId" id="shipToStateProvinceGeoId" style="width: 70%">
          <#if shipToStateProvinceGeoId?has_content>
            <option value='${shipToStateProvinceGeoId!}'>${shipToStateProvinceGeo!(shipToStateProvinceGeoId!)}</option>
          <#else>
            <option value="_NA_">${uiLabelMap.PartyNoState}</option>
          </#if>
        </select>
      </span>
    </div>
    <#if shipToTelecomNumber?has_content>
      <div class="form-row">
        <div class="field-label">
          <label>${uiLabelMap.PartyPhoneNumber}*</label>
          <span id="advice-required-shipToCountryCode" style="display:none" class="errorMessage"></span>
          <span id="advice-required-shipToAreaCode" style="display:none" class="errorMessage"></span>
          <span id="advice-required-shipToContactNumber" style="display:none" class="errorMessage"></span>
          <span id="shipToPhoneRequired" style="display: none;" class="errorMessage">(required)</span>
        </div>
        <div>
          <input type="hidden" name="phoneContactMechId" value="${shipToTelecomNumber.contactMechId?if_exists}"/>
          <input type="text" name="countryCode" id="shipToCountryCode" class="required" value="${shipToTelecomNumber.countryCode?if_exists}" size="3" maxlength="3"/>
          - <input type="text" name="areaCode" id="shipToAreaCode" class="required" value="${shipToTelecomNumber.areaCode?if_exists}" size="3" maxlength="3"/>
          - <input type="text" name="contactNumber" id="shipToContactNumber" class="required" value="${contactNumber?default("${shipToTelecomNumber.contactNumber?if_exists}")}" size="6" maxlength="7"/>
          - <input type="text" name="extension" value="${extension?default("${shipToExtension?if_exists}")}" size="3" maxlength="3"/>
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
