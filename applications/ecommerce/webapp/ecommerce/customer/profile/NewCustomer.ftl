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

<div id="form-container">
  <form id="newUserForm" name="newUserForm" method="post" action="<@ofbizUrl>createCustomerProfile</@ofbizUrl>">
    <input type="hidden" name="roleTypeId" value="CUSTOMER"/>
    <input type="hidden" name="emailContactMechPurposeTypeId" value="PRIMARY_EMAIL"/>
    <#assign productStoreId = Static["org.ofbiz.product.store.ProductStoreWorker"].getProductStoreId(request)/>
    <input type="hidden" name="productStoreId" value="${productStoreId?if_exists}"/>

    <div><h1>${uiLabelMap.PartyRequestNewAccount}</h1></div>

    <div class="left center">
      <div class="screenlet-header"><div class='boxhead'>&nbsp;${uiLabelMap.PartyContactInformation}</div></div>
      <div class="form-row">
        <div class="field-label"><label for="firstName">${uiLabelMap.PartyFirstName}*<span id="advice-required-firstName" style="display: none" class="errorMessage">(required)</span></label></div>
        <div class="field-widget"><input type="text" name="firstName" id="firstName" class="inputBox required" value="${parameters.firstName?if_exists}" size="30" maxlength="30"></div>
      </div>
      <div class="form-row">
        <div class="field-label"><label for="lastName">${uiLabelMap.PartyLastName}*<span id="advice-required-lastName" style="display: none" class="errorMessage">(required)</span></label></div>
        <div class="field-widget"><input type="text" name="lastName" id="lastName" class="inputBox required" value="${parameters.lastName?if_exists}" size="30" maxlength="30"></div>
      </div>
      <div class="form-row">
        <div class="field-label"><label for="emailAddress">${uiLabelMap.PartyEmailAddress}*<span id="advice-required-emailAddress" style="display: none" class="errorMessage">(required)</span></label></div>
        <div class="field-widget"><input type="text" class="inputBox required" name="emailAddress" id="emailAddress" value="${parameters.emailAddress?if_exists}" size="30" maxlength="255"/></div>
      </div>
    </div>

    <div class="center right">
      <div class="screenlet-header"><div class='boxhead'>&nbsp;${uiLabelMap.AccountInformation}</div></div>
      <div id="userNameAndPasswordPanel">
        <div class="form-row">
          <div class="field-label"><label for="userName">${uiLabelMap.CommonUsername}*<span id="advice-required-username" style="display: none" class="errorMessage">(required)</span></label></div>
          <div class="field-widget"><input type="text" name="username" id="username" class="inputBox required" value="${parameters.username?if_exists}" size="30" maxlength="255"></div>
        </div>
        <div class="form-row">
          <div class="field-label"><label for="password">${uiLabelMap.CommonPassword}*<span id="advice-required-password" style="display: none" class="errorMessage">(required)</span></label></div>
          <div class="field-widget"><input type="password" name="password" id="password" class="inputBox required" value="${parameters.password?if_exists}" maxlength="16"></div>
        </div>
        <div class="form-row">
          <div class="field-label"><label for="passwordVerify">${uiLabelMap.CommonConfirm} ${uiLabelMap.CommonPassword}*<span id="advice-required-passwordVerify" style="display: none" class="errorMessage">(required)</span></label></div>
          <div class="field-widget"><input type="password" name="passwordVerify" id="passwordVerify" class="inputBox required" value="${parameters.passwordVerify?if_exists}" maxlength="16"></div>
        </div>
        <div class="form-row">
          <div class="field-label"><label for="currentPassword">${uiLabelMap.FormFieldTitle_passwordHint}</label></div>
          <div class="field-widget"><input type="text" name="passwordHint" id="passwordHintId" class="inputBox" value="${parameters.passwordHint?if_exists}" maxlength="16"></div>
        </div>
      </div>
    </div>

    <div class="form-row"><hr class="sepbar"/></div>
    <div class="bothclear"></div>    

    <div class="left center">
      <div class="screenlet-header"><div class='boxhead'>&nbsp;${uiLabelMap.OrderShippingInformation}</div></div>
      <div class="form-row">
        <div class="field-label"><label for="shipToAddress1">${uiLabelMap.PartyAddressLine1}*<span id="advice-required-shipToAddress1" style="display: none" class="errorMessage">(required)</span></label></div>
        <div class="field-widget"><input type="text" name="shipToAddress1" id="shipToAddress1" class="inputBox required" value="${parameters.shipToAddress1?if_exists}"/></div>
      </div>  
      <div class="form-row">
        <div class="field-label"><label for="shipToAddress2">${uiLabelMap.PartyAddressLine2}</label></div>
        <div class="field-widget"><input type="text" name="shipToAddress2" id="shipToAddress2" class="inputBox" value="${parameters.shipToAddress2?if_exists}"/></div>
      </div>  
      <div class="form-row">
        <div class="field-label"><label for="shipToCity">${uiLabelMap.CommonCity}*<span id="advice-required-shipToCity" style="display: none" class="errorMessage">(required)</span></label></div>
        <div class="field-widget"><input type="text" name="shipToCity" id="shipToCity" class="inputBox required" value="${parameters.shipToCity?if_exists}"/></div>
      </div>  
      <div class="form-row">
        <div class="field-label"><label for="shipToStateProvinceGeoId">${uiLabelMap.CommonState}*<span id="advice-required-shipToStateProvinceGeoId" style="display: none" class="errorMessage">(required)</span></label></div>
        <div class="field-widget">
          <select name="shipToStateProvinceGeoId" id="shipToStateProvinceGeoId" class='selectBox'>
            <#if parameters.shipToStateProvinceGeoId?exists><option>${parameters.shipToStateProvinceGeoId?if_exists}</option></#if>
            ${screens.render("component://common/widget/CommonScreens.xml#states")}
          </select>
        </div>
      </div>  
      <div class="form-row">
        <div class="field-label"><label for="shipToPostalCode">${uiLabelMap.PartyZipCode}*<span id="advice-required-shipToPostalCode" style="display: none" class="errorMessage">(required)</span></label></div>
        <div class="field-widget"><input type="text" name="shipToPostalCode" id="shipToPostalCode" class="inputBox required" value="${parameters.shipToPostalCode?if_exists}"/></div>
      </div>  
      <div class="form-row">
        <div class="field-label"><label for="shipToCountryGeoId">${uiLabelMap.PartyCountry}*<span id="advice-required-shipToCountryGeoId" style="display: none" class="errorMessage">(required)</span></label></div>
        <div class="field-widget">
          <select name="shipToCountryGeoId" id="shipToCountryGeoId" class='selectBox'>
            <#if parameters.shipToCountryGeoId?exists><option>${parameters.shipToCountryGeoId?if_exists}</option></#if>
            ${screens.render("component://common/widget/CommonScreens.xml#countries")}
          </select>
        </div>
      </div>  
      <div class="form-row">
        <div class="field-label">
          <label for="shipToCountryCode">${uiLabelMap.PartyCountry}<span>*</span><span id="advice-required-shipToCountryCode" style="display:none" class="errorMessage">(required)</span></label>
          <label for="shipToAreaCode">${uiLabelMap.PartyAreaCode}<span>*</span><span id="advice-required-shipToAreaCode" style="display:none" class="errorMessage">(required)</span></label>
          <label for="shipToContactNumber">${uiLabelMap.PartyContactNumber}<span>*</span><span id="advice-required-shipToContactNumber" style="display:none" class="errorMessage">(required)</span></label>
          <label for="shipToExtension">${uiLabelMap.PartyExtension}</label>
        </div>
        <div class="field-widget">
          <input type="text" name="shipToCountryCode" id="shipToCountryCode" class="inputBox required" value="${parameters.countryCode?if_exists}" size="3"  maxlength="3"/>
          - <input type="text" name="shipToAreaCode" id="shipToAreaCode" class="inputBox required" value="${parameters.areaCode?if_exists}" size="3" maxlength="3"/>
          - <input type="text" name="shipToContactNumber" id="shipToContactNumber" class="inputBox required" value="${contactNumber?default("${parameters.contactNumber?if_exists}")}" size="6" maxlength="7"/>
          - <input type="text" name="shipToExtension" id="shipToExtension" class="inputBox" value="${extension?default("${parameters.extension?if_exists}")}" size="3" maxlength="3"/>
        </div>
      </div>
      <div class="form-row">
        <div class="field-widget"><input type="checkbox" class="checkbox" name="useShippingAddressForBilling" id="useShippingAddressForBilling" value="Y" <#if parameters.useShippingAddressForBilling?has_content && parameters.useShippingAddressForBilling?default("")=="Y">checked</#if>/>&nbsp;&nbsp;${uiLabelMap.FacilityBillingAddressSameShipping}</div>
      </div>  
    </div>

    <div class="center right">
      <div class="screenlet-header"><div class='boxhead'>&nbsp;${uiLabelMap.PageTitleBillingInformation}</div></div>
      <div id="billingAddress">
        <div class="form-row">
          <div class="field-label"><label for="billToAddress1">${uiLabelMap.PartyAddressLine1}*<span id="advice-required-billToAddress1" style="display: none" class="errorMessage">(required)</span></label></div>
          <div class="field-widget"><input type="text" name="billToAddress1" id="billToAddress1" class="inputBox required" value="${parameters.billToAddress1?if_exists}"/></div>
        </div>  
        <div class="form-row">
          <div class="field-label"><label for="billToAddress2">${uiLabelMap.PartyAddressLine2}</label></div>
          <div class="field-widget"><input type="text" name="billToAddress2" id="billToAddress2" class="inputBox" value="${parameters.billToAddress2?if_exists}"/></div>
        </div>  
        <div class="form-row">
          <div class="field-label"><label for="billToCity">${uiLabelMap.CommonCity}*<span id="advice-required-billToCity" style="display: none" class="errorMessage">(required)</span></label></div>
          <div class="field-widget"><input type="text" name="billToCity" id="billToCity" class="inputBox required" value="${parameters.billToCity?if_exists}"/></div>
        </div>  
        <div class="form-row">
          <div class="field-label"><label for="billToStateProvinceGeoId">${uiLabelMap.CommonState}*<span id="advice-required-billToStateProvinceGeoId" style="display: none" class="errorMessage">(required)</span></label></div>
          <div class="field-widget">
            <select name="billToStateProvinceGeoId" id="billToStateProvinceGeoId" class="selectBox required">
              <#if parameters.billToStateProvinceGeoId?exists><option>${parameters.billToStateProvinceGeoId?if_exists}</option></#if>
              ${screens.render("component://common/widget/CommonScreens.xml#states")}
            </select>
          </div>
        </div>
        <div class="form-row">
          <div class="field-label"><label for="billToPostalCode">${uiLabelMap.PartyZipCode}*<span id="advice-required-billToPostalCode" style="display: none" class="errorMessage">(required)</span></label></div>
          <div class="field-widget"><input type="text" name="billToPostalCode" id="billToPostalCode" class="inputBox required" value="${parameters.billToPostalCode?if_exists}"/></div>
        </div>  
        <div class="form-row">
          <div class="field-label"><label for="billToCountryGeoId">${uiLabelMap.PartyCountry}*<span id="advice-required-billToCountryGeoId" style="display: none" class="errorMessage">(required)</span></label></div>
          <div class="field-widget">
            <select name="billToCountryGeoId" id="billToCountryGeoId" class="selectBox required">
              <#if parameters.billToCountryGeoId?exists><option>${parameters.billToCountryGeoId?if_exists}</option></#if>
              ${screens.render("component://common/widget/CommonScreens.xml#countries")}
            </select>
          </div>
        </div>
        <div class="form-row">
          <div class="field-label">
            <label for="billToCountryCode">${uiLabelMap.PartyCountry}<span>*</span><span id="advice-required-billToCountryCode" style="display:none" class="errorMessage">(required)</span></label>
            <label for="billToAreaCode">${uiLabelMap.PartyAreaCode}<span>*</span><span id="advice-required-billToAreaCode" style="display:none" class="errorMessage">(required)</span></label>
            <label for="billToContactNumber">${uiLabelMap.PartyContactNumber}<span>*</span><span id="advice-required-billToContactNumber" style="display:none" class="errorMessage">(required)</span></label>
            <label for="billToExtension">${uiLabelMap.PartyExtension}</label>
          </div>
          <div class="field-widget">
            <input type="text" name="billToCountryCode" id="billToCountryCode" class="inputBox required" value="${parameters.countryCode?if_exists}" size="3"  maxlength="3"/>
            - <input type="text" name="billToAreaCode" id="billToAreaCode" class="inputBox required" value="${parameters.areaCode?if_exists}" size="3" maxlength="3"/>
            - <input type="text" name="billToContactNumber" id="billToContactNumber" class="inputBox required" value="${contactNumber?default("${parameters.contactNumber?if_exists}")}" size="6" maxlength="7"/>
            - <input type="text" name="billToExtension" id="billToExtension" class="inputBox" value="${extension?default("${parameters.extension?if_exists}")}" size="3" maxlength="3"/>
          </div>
        </div>
      </div>  
    </div>

    <div class="bothclear"></div>
    <div class="form-row">&nbsp;&nbsp;<a id="submitNewUserForm" href="javascript:void(0);" class="buttontext">${uiLabelMap.CommonSave}</a></div>
  </form>
</div>