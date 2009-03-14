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
    <div class='boxhead'>&nbsp;${uiLabelMap.EcommerceMyAccount}</div>
  </div>
  <div class="screenlet-body">
  <form id="newUserForm" name="newUserForm" method="post" action="<@ofbizUrl>createCustomerProfile</@ofbizUrl>">
    <input type="hidden" name="roleTypeId" value="CUSTOMER"/>
    <input type="hidden" name="emailContactMechPurposeTypeId" value="PRIMARY_EMAIL"/>
    <#assign productStoreId = Static["org.ofbiz.product.store.ProductStoreWorker"].getProductStoreId(request)/>
    <input type="hidden" name="productStoreId" value="${productStoreId?if_exists}"/>

    <div class="left center">
      <div class="screenlet-header"><div class='boxhead'>&nbsp;${uiLabelMap.PartyContactInformation}</div></div>
      <div class="form-row">
        <div class="field-label"><label for="firstName">${uiLabelMap.PartyFirstName}* <span id="advice-required-firstName" style="display: none" class="errorMessage">(required)</span></label></div>
        <div><input type="text" name="firstName" id="firstName" class="required" value="${parameters.firstName?if_exists}" size="30" maxlength="30"></div>
      </div>
      <div class="form-row">
        <div class="field-label"><label for="lastName">${uiLabelMap.PartyLastName}* <span id="advice-required-lastName" style="display: none" class="errorMessage">(required)</span></label></div>
        <div><input type="text" name="lastName" id="lastName" class="required" value="${parameters.lastName?if_exists}" size="30" maxlength="30"></div>
      </div>
      <div class="form-row">
        <div class="field-label">
          <label for="emailAddress">${uiLabelMap.CommonEmail}*
            <span id="advice-required-emailAddress" style="display: none" class="errorMessage">(required)</span>
          </label>
        </div>
        <div><input type="text" class="required validate-email" name="emailAddress" id="emailAddress" value="${parameters.emailAddress?if_exists}" size="30" maxlength="255"/></div>
      </div>
    </div>
    <div class="center right">
      <div class="screenlet-header"><div class='boxhead'>&nbsp;${uiLabelMap.EcommerceAccountInformation}</div></div>
      <div id="userNameAndPasswordPanel">
        <div class="form-row">
          <div class="field-label"><label for="userName">${uiLabelMap.CommonUsername}* <span id="advice-required-username" style="display: none" class="errorMessage">(required)</span></label></div>
          <div><input type="text" name="username" id="username" class="required" value="${parameters.username?if_exists}" size="30" maxlength="255"></div>
        </div>
        <div class="form-row">
          <div class="field-label"><label for="password">${uiLabelMap.CommonPassword}* <span id="advice-required-password" style="display: none" class="errorMessage">(required)</span></label></div>
          <div><input type="password" name="password" id="password" class="required validate-password" value="${parameters.password?if_exists}" maxlength="16"></div>
          <span id="advice-validate-password-password" class="errorMessage" style="display:none">${uiLabelMap["loginservices.password_may_not_equal_username"]}</span>
        </div>
        <div class="form-row">
          <div class="field-label"><label for="passwordVerify">${uiLabelMap.PartyRepeatPassword}* <span id="advice-required-passwordVerify" style="display: none" class="errorMessage">(required)</span></label></div>
          <div><input type="password" name="passwordVerify" id="passwordVerify" class="required validate-passwordVerify" value="${parameters.passwordVerify?if_exists}" maxlength="16"></div>
          <span id="advice-validate-passwordVerify-passwordVerify" class="errorMessage" style="display:none">${uiLabelMap["loginservices.password_did_not_match_verify_password"]}</span>
        </div>
      </div>
    </div>
    <div class="form-row"></div>
    <span id="advice-validate-email-emailAddress" class="errorMessage" style="display:none">${uiLabelMap.PartyEmailAddressNotFormattedCorrectly}</span>
    <div class="form-row"><hr class="sepbar"/></div>
    <div class="bothclear"></div>    

    <div class="left center">
      <div class="screenlet-header"><div class='boxhead'>&nbsp;${uiLabelMap.OrderShippingInformation}</div></div>
      <div class="form-row">
        <div class="field-label"><label for="shipToAddress1">${uiLabelMap.PartyAddressLine1}* <span id="advice-required-shipToAddress1" style="display: none" class="errorMessage">(required)</span></label></div>
        <div><input type="text" name="shipToAddress1" id="shipToAddress1" class="required" value="${parameters.shipToAddress1?if_exists}"/></div>
      </div>  
      <div class="form-row">
        <div class="field-label"><label for="shipToAddress2">${uiLabelMap.PartyAddressLine2}</label></div>
        <div><input type="text" name="shipToAddress2" id="shipToAddress2" value="${parameters.shipToAddress2?if_exists}"/></div>
      </div>  
      <div class="form-row">
        <div class="field-label"><label for="shipToCity">${uiLabelMap.CommonCity}* <span id="advice-required-shipToCity" style="display: none" class="errorMessage">(required)</span></label></div>
        <div><input type="text" name="shipToCity" id="shipToCity" class="required" value="${parameters.shipToCity?if_exists}"/></div>
      </div> 
      <div class="form-row">
        <div class="field-label"><label for="shipToPostalCode">${uiLabelMap.PartyZipCode}* <span id="advice-required-shipToPostalCode" style="display: none" class="errorMessage">(required)</span></label></div>
        <div><input type="text" name="shipToPostalCode" id="shipToPostalCode" class="required" value="${parameters.shipToPostalCode?if_exists}" maxlength="10"/></div>
      </div> 
      <div class="form-row">
        <div class="field-label"><label for="shipToCountryGeoId">${uiLabelMap.PartyCountry}* <span id="advice-required-shipToCountryGeoId" style="display: none" class="errorMessage">(required)</span></label></div>
        <div>
          <select name="shipToCountryGeoId" id="shipToCountryGeoId">
            <#if shipToCountryGeoId??>
              <option value="${shipToCountryGeoId!}">${shipToCountryProvinceGeo!(shipToCountryGeoId!)}</option>
            </#if>
            ${screens.render("component://common/widget/CommonScreens.xml#countries")}
          </select>
        </div>
      </div>
      <div id='shipToStates' class="form-row">
        <div class="field-label"><label for="state">${uiLabelMap.CommonState}*<span id="advice-required-shipToStateProvinceGeoId" style="display: none" class="errorMessage">(required)</span></label></div>
        <div>
          <select id="shipToStateProvinceGeoId" name="shipToStateProvinceGeoId">
            <#if shipToStateProvinceGeoId?has_content>
              <option value='${shipToStateProvinceGeoId!}'>${shipToStateProvinceGeo!(shipToStateProvinceGeoId!)}</option>
            <#else>
               <option value="_NA_">${uiLabelMap.PartyNoState}</option>
            </#if>
          </select>
        </div>
      </div>  
      <div class="form-row">
        <div class="field-label">
          <label>${uiLabelMap.PartyPhoneNumber}*</label>
          <span id="advice-required-shipToCountryCode" style="display:none" class="errorMessage"></span>
          <span id="advice-required-shipToAreaCode" style="display:none" class="errorMessage"></span>
          <span id="advice-required-shipToContactNumber" style="display:none" class="errorMessage"></span>
          <span id="shipToPhoneRequired" style="display: none;" class="errorMessage">(required)</span>
        </div>
        <div>
          <input type="text" name="shipToCountryCode" id="shipToCountryCode" class="required" value="${parameters.shipToCountryCode?if_exists}" size="3" maxlength="3"/>
          - <input type="text" name="shipToAreaCode" id="shipToAreaCode" class="required" value="${parameters.shipToAreaCode?if_exists}" size="3" maxlength="3"/>
          - <input type="text" name="shipToContactNumber" id="shipToContactNumber" class="required" value="${contactNumber?default("${parameters.shipToContactNumber?if_exists}")}" size="6" maxlength="7"/>
          - <input type="text" name="shipToExtension" id="shipToExtension" value="${extension?default("${parameters.shipToExtension?if_exists}")}" size="3" maxlength="3"/>
        </div>
      </div>
      <div class="form-row">
        <div><input type="checkbox" class="checkbox" name="useShippingAddressForBilling" id="useShippingAddressForBilling" value="Y" <#if parameters.useShippingAddressForBilling?has_content && parameters.useShippingAddressForBilling?default("")=="Y">checked</#if>/>&nbsp;&nbsp;${uiLabelMap.FacilityBillingAddressSameShipping}</div>
      </div>  
    </div>

    <div class="center right">
      <div class="screenlet-header"><div class='boxhead'>&nbsp;${uiLabelMap.PageTitleBillingInformation}</div></div>
      <div id="billingAddress">
        <div class="form-row">
          <div class="field-label"><label for="billToAddress1">${uiLabelMap.PartyAddressLine1}* <span id="advice-required-billToAddress1" style="display: none" class="errorMessage">(required)</span></label></div>
          <div><input type="text" name="billToAddress1" id="billToAddress1" class="required" value="${parameters.billToAddress1?if_exists}"/></div>
        </div>  
        <div class="form-row">
          <div class="field-label"><label for="billToAddress2">${uiLabelMap.PartyAddressLine2}</label></div>
          <div><input type="text" name="billToAddress2" id="billToAddress2" value="${parameters.billToAddress2?if_exists}"/></div>
        </div>  
        <div class="form-row">
          <div class="field-label"><label for="billToCity">${uiLabelMap.CommonCity}* <span id="advice-required-billToCity" style="display: none" class="errorMessage">(required)</span></label></div>
          <div><input type="text" name="billToCity" id="billToCity" class="required" value="${parameters.billToCity?if_exists}"/></div>
        </div>
        <div class="form-row">
          <div class="field-label"><label for="billToPostalCode">${uiLabelMap.PartyZipCode}* <span id="advice-required-billToPostalCode" style="display: none" class="errorMessage">(required)</span></label></div>
          <div><input type="text" name="billToPostalCode" id="billToPostalCode" class="required" value="${parameters.billToPostalCode?if_exists}" maxlength="10"/></div>
        </div> 
        <div class="form-row">
        <div class="field-label"><label for="billToCountryGeoId">${uiLabelMap.PartyCountry}* <span id="advice-required-billToCountryGeoId" style="display: none" class="errorMessage">(required)</span></label></div>
        <div>
          <select name="billToCountryGeoId" id="billToCountryGeoId" class='required selectBox'>
            <#if billToCountryGeoId??>
              <option value='${billToCountryGeoId!}'>${billToCountryProvinceGeo!(billToCountryGeoId!)}</option>
            </#if>
            ${screens.render("component://common/widget/CommonScreens.xml#countries")}
          </select>
        </div>
      </div>
      <div id='billToStates' class="form-row">
        <div class="field-label"><label for="state">${uiLabelMap.CommonState}*<span id="advice-required-billToStateProvinceGeoId" style="display: none" class="errorMessage">(required)</span></label></div>
        <div>
          <select id="billToStateProvinceGeoId" name="billToStateProvinceGeoId">
            <#if billToStateProvinceGeoId?has_content>
              <option value='${billToStateProvinceGeoId!}'>${billToStateProvinceGeo!(billToStateProvinceGeoId!)}</option>
            <#else>
              <option value="_NA_">${uiLabelMap.PartyNoState}</option>
            </#if>
          </select>
        </div>
      </div>
        <div class="form-row">
          <div class="field-label">
            <label>${uiLabelMap.PartyPhoneNumber}*</label>
            <span id="advice-required-billToCountryCode" style="display:none" class="errorMessage"></span>
            <span id="advice-required-billToAreaCode" style="display:none" class="errorMessage"></span>
            <span id="advice-required-billToContactNumber" style="display:none" class="errorMessage"></span>
            <span id="billToPhoneRequired" style="display: none;" class="errorMessage">(required)</span>
          </div>
          <div>
            <input type="text" name="billToCountryCode" id="billToCountryCode" class="required" value="${parameters.billToCountryCode?if_exists}" size="3" maxlength="3"/>
            - <input type="text" name="billToAreaCode" id="billToAreaCode" class="required" value="${parameters.billToAreaCode?if_exists}" size="3" maxlength="3"/>
            - <input type="text" name="billToContactNumber" id="billToContactNumber" class="required" value="${contactNumber?default("${parameters.billToContactNumber?if_exists}")}" size="6" maxlength="7"/>
            - <input type="text" name="billToExtension" id="billToExtension" value="${extension?default("${parameters.billToExtension?if_exists}")}" size="3" maxlength="3"/>
          </div>
        </div>
      </div>  
    </div>

    <div class="bothclear"></div>
    <div class="form-row">&nbsp;&nbsp;<a id="submitNewUserForm" href="javascript:void(0);" class="buttontext">${uiLabelMap.CommonSubmit}</a></div>
  </form>
  </div>
</div>