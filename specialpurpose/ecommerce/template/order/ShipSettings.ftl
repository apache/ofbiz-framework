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
<#assign cart = sessionAttributes.shoppingCart!>
  <h3>${uiLabelMap.OrderShippingInformation}</h3>
  <div id="shippingFormServerError" class="errorMessage"></div>
  <form id="editShippingContact" method="post" action="<@ofbizUrl>processShipSettings</@ofbizUrl>" name="${parameters.formNameValue}">
    <fieldset><legend>${uiLabelMap.OrderShippingInformation}</legend>
      <input type="hidden" name="shippingContactMechId" value="${parameters.shippingContactMechId!}"/>
      <input type="hidden" name="partyId" value="${cart.getPartyId()?default("_NA_")}"/>
      <div>
        <label for="address1">${uiLabelMap.PartyAddressLine1}*</label>
        <input id="address1" name="address1" class="required" type="text" value="${address1!}"/>
        <span id="advice-required-address1" class="custom-advice errorMessage" style="display:none"> (${uiLabelMap.CommonRequired})</span>
      </div>
      <div>
        <label for="address2">${uiLabelMap.PartyAddressLine2}</label>
        <input id="address2" name="address2" type="text" value="${address2!}"/>
      </div>
      <div>
        <label for="city">${uiLabelMap.CommonCity}*</label>
        <input id="city" name="city" class="required" type="text" value="${city!}"/>
        <span id="advice-required-city" class="custom-advice errorMessage" style="display:none"> (${uiLabelMap.CommonRequired})</span>
      </div>
      <div>
        <label for="postalCode">${uiLabelMap.PartyZipCode}*</label>
        <input id="postalCode" name="postalCode" class="required" type="text" value="${postalCode!}" size="12" maxlength="10"/>
        <span id="advice-required-postalCode" class="custom-advice errorMessage" style="display:none"> (${uiLabelMap.CommonRequired})</span>
      </div>
      <div>
        <label for="countryGeoId">${uiLabelMap.CommonCountry}*</label>
        <select name="countryGeoId" id="countryGeoId">
          <#if countryGeoId??>
            <option value="${countryGeoId!}">${countryProvinceGeo!(countryGeoId!)}</option>
          </#if>
          ${screens.render("component://common/widget/CommonScreens.xml#countries")}
        </select>
        <span id="advice-required-countryGeoId" style="display:none" class="errorMessage"> (${uiLabelMap.CommonRequired})</span>
      </div>
      <div>
        <label for="state">${uiLabelMap.CommonState}*</label>
        <select id="stateProvinceGeoId" name="stateProvinceGeoId">
          <#if stateProvinceGeoId?has_content>
            <option value='${stateProvinceGeoId!}'>${stateProvinceGeo!(stateProvinceGeoId!)}</option>
          <#else>
            <option value="_NA_">${uiLabelMap.PartyNoState}</option>
          </#if>
          ${screens.render("component://common/widget/CommonScreens.xml#states")}
        </select>
        <span id="advice-required-stateProvinceGeoId" style="display:none" class="errorMessage">(${uiLabelMap.CommonRequired})</span>
      </div>
      <div class="buttons">
        <input type="submit" class="smallsubmit" value="${uiLabelMap.CommonContinue}"/>
      </div>
    </fieldset>
  </form>
