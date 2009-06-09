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
<#assign cart = sessionAttributes.shoppingCart?if_exists>
  <h3>${uiLabelMap.OrderShippingInformation}</h3>
  <div id="shippingFormServerError" class="errorMessage"></div>
  <form id="editShippingContact" method="post" action="<@ofbizUrl>processShipSettings</@ofbizUrl>" name="${parameters.formNameValue}">
    <fieldset><legend>${uiLabelMap.OrderShippingInformation}</legend>
      <input type="hidden" name="shippingContactMechId" value="${parameters.shippingContactMechId?if_exists}"/>
      <input type="hidden" name="partyId" value="${cart.getPartyId()?default("_NA_")}"/>
      <div>
        <label for="shipToAddress1">${uiLabelMap.PartyAddressLine1}*</label>
        <input id="shipToAddress1" name="shipToAddress1" class="required" type="text" value="${shipToAddress1?if_exists}"/>
        <span id="advice-required-shipToAddress1" class="custom-advice errorMessage" style="display:none"> (required)</span>
      </div>
      <div>
        <label for="shipToAddress2">${uiLabelMap.PartyAddressLine2}</label>
        <input id="shipToAddress2" name="shipToAddress2" type="text" value="${shipToAddress2?if_exists}"/>
      </div>
      <div>
        <label for="shipToCity">${uiLabelMap.CommonCity}*</label>
        <input id="shipToCity" name="shipToCity" class="required" type="text" value="${shipToCity?if_exists}"/>
        <span id="advice-required-shipToCity" class="custom-advice errorMessage" style="display:none"> (required)</span>
      </div>
      <div>
        <label for="shipToPostalCode">${uiLabelMap.PartyZipCode}*</label>
        <input id="shipToPostalCode" name="shipToPostalCode" class="required" type="text" value="${shipToPostalCode?if_exists}" size="12" maxlength="10"/>
        <span id="advice-required-shipToPostalCode" class="custom-advice errorMessage" style="display:none"> (required)</span>
      </div>
      <div>
        <label for="shipToCountryGeoId">${uiLabelMap.PartyCountry}*</label>
        <select name="shipToCountryGeoId" id="shipToCountryGeoId">
          <#if shipToCountryGeoId??>
            <option value="${shipToCountryGeoId!}">${shipToCountryProvinceGeo!(shipToCountryGeoId!)}</option>
          </#if>
          ${screens.render("component://common/widget/CommonScreens.xml#countries")}
        </select>
        <span id="advice-required-shipToCountryGeo" style="display:none" class="errorMessage"> (required)</span>
      </div>
      <div>
        <label for="state">${uiLabelMap.CommonState}*</label>
        <select id="shipToStateProvinceGeoId" name="shipToStateProvinceGeoId">
          <#if shipToStateProvinceGeoId?has_content>
            <option value='${shipToStateProvinceGeoId!}'>${shipToStateProvinceGeo!(shipToStateProvinceGeoId!)}</option>
          <#else>
            <option value="_NA_">${uiLabelMap.PartyNoState}</option>
          </#if>
          ${screens.render("component://common/widget/CommonScreens.xml#states")}
        </select>
        <span id="advice-required-shipToStateProvinceGeoId" style="display:none" class="errorMessage">(required)</span>
      </div>
      <div class="buttons">
        <input type="submit" class="smallsubmit" value="${uiLabelMap.CommonContinue}"/>
      </div>
    </fieldset>
  </form>
