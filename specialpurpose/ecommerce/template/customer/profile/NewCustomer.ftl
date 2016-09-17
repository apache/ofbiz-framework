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
  <div class="screenlet-body">
    <form id="newUserForm" method="post" action="<@ofbizUrl>createCustomerProfile</@ofbizUrl>">
      <fieldset class="left center">
        <legend>${uiLabelMap.PartyContactInformation}</legend>
        <input type="hidden" name="roleTypeId" value="CUSTOMER"/>
        <input type="hidden" name="emailContactMechPurposeTypeId" value="PRIMARY_EMAIL"/>
        <#assign productStoreId =
            Static["org.apache.ofbiz.product.store.ProductStoreWorker"].getProductStoreId(request) />
        <input type="hidden" name="productStoreId" value="${productStoreId!}"/>
        <div>
          <label for="firstName">${uiLabelMap.PartyFirstName}*
            <span id="advice-required-firstName" style="display: none" class="errorMessage">(${uiLabelMap.CommonRequired})</span>
          </label>
          <input type="text" name="firstName" id="firstName" class="required" value="${parameters.firstName!}"
              maxlength="30"/>
        </div>
        <div>
          <label for="lastName">${uiLabelMap.PartyLastName}*
            <span id="advice-required-lastName" style="display: none" class="errorMessage">
              (${uiLabelMap.CommonRequired})
            </span>
          </label>
          <input type="text" name="lastName" id="lastName" class="required" value="${parameters.lastName!}"
              maxlength="30"/>
        </div>
        <div>
          <label for="emailAddress">${uiLabelMap.CommonEmail}*
            <span id="advice-required-emailAddress" style="display: none" class="errorMessage">
              (${uiLabelMap.CommonRequired})
            </span>
          </label>
          <input type="text" class="required validate-email" name="emailAddress" id="emailAddress"
              value="${parameters.emailAddress!}" maxlength="255"/>
        </div>
        <span id="advice-validate-email-emailAddress" class="errorMessage" style="display:none">
          ${uiLabelMap.PartyEmailAddressNotFormattedCorrectly}
        </span>
      </fieldset>
      <fieldset class="center right" id="userNameAndPasswordPanel">
        <legend>${uiLabelMap.EcommerceAccountInformation}</legend>
        <div>
          <label for="username">${uiLabelMap.CommonUsername}*
            <span id="advice-required-username" style="display: none" class="errorMessage">
              (${uiLabelMap.CommonRequired})
            </span>
          </label>
          <input type="text" name="username" id="username" class="required" value="${parameters.username!}"
              maxlength="255"/>
        </div>
        <div>
          <label for="password">${uiLabelMap.CommonPassword}*
            <span id="advice-required-password" style="display: none" class="errorMessage">
              (${uiLabelMap.CommonRequired})
            </span>
          </label>
          <input type="password" name="password" id="password" class="required validate-password"
              value="${parameters.password!}" maxlength="16"/>
          <span id="advice-validate-password-password" class="errorMessage" style="display:none">
            ${uiLabelMap["loginservices.password_may_not_equal_username"]}
          </span>
        </div>
        <div>
          <label for="passwordVerify">${uiLabelMap.PartyRepeatPassword}*
            <span id="advice-required-passwordVerify" style="display: none" class="errorMessage">
              (${uiLabelMap.CommonRequired})
            </span>
          </label>
          <input type="password" name="passwordVerify" id="passwordVerify" class="required validate-passwordVerify"
              value="${parameters.passwordVerify!}" maxlength="16"/>
          <span id="advice-validate-passwordVerify-passwordVerify" class="errorMessage" style="display:none">
            ${uiLabelMap["loginservices.password_did_not_match_verify_password"]}
          </span>
        </div>
      </fieldset>
      <fieldset class="left center">
        <legend>${uiLabelMap.OrderShippingInformation}</legend>
        <div>
          <label for="shipToAddress1">${uiLabelMap.PartyAddressLine1}*
            <span id="advice-required-shipToAddress1" style="display: none" class="errorMessage">
              (${uiLabelMap.CommonRequired})
            </span>
          </label>
          <input type="text" name="shipToAddress1" id="shipToAddress1" class="required"
              value="${parameters.shipToAddress1!}"/>
        </div>
        <div>
          <label for="shipToAddress2">${uiLabelMap.PartyAddressLine2}</label>
          <input type="text" name="shipToAddress2" id="shipToAddress2" value="${parameters.shipToAddress2!}"/>
        </div>
        <div>
          <label for="shipToCity">${uiLabelMap.CommonCity}*
            <span id="advice-required-shipToCity" style="display: none" class="errorMessage">
              (${uiLabelMap.CommonRequired})
            </span>
          </label>
          <input type="text" name="shipToCity" id="shipToCity" class="required" value="${parameters.shipToCity!}"/>
        </div>
        <div>
          <label for="shipToPostalCode">${uiLabelMap.PartyZipCode}*
            <span id="advice-required-shipToPostalCode" style="display: none" class="errorMessage">
              (${uiLabelMap.CommonRequired})
            </span>
          </label>
          <input type="text" name="shipToPostalCode" id="shipToPostalCode" class="required"
              value="${parameters.shipToPostalCode!}" maxlength="10"/>
        </div>
        <div>
          <label for="shipToCountryGeoId">${uiLabelMap.CommonCountry}*
            <span id="advice-required-shipToCountryGeoId" style="display: none" class="errorMessage">
              (${uiLabelMap.CommonRequired})
            </span>
          </label>
          <div>
            <select name="shipToCountryGeoId" id="shipToCountryGeoId">
              <#if shipToCountryGeoId??>
                <option value="${shipToCountryGeoId!}">${shipToCountryProvinceGeo!(shipToCountryGeoId!)}</option>
              </#if>
              ${screens.render("component://common/widget/CommonScreens.xml#countries")}
            </select>
          </div>
        </div>
        <div id='shipToStates'>
          <label for="shipToStateProvinceGeoId">${uiLabelMap.CommonState}*
            <span id="advice-required-shipToStateProvinceGeoId" style="display: none" class="errorMessage">
              (${uiLabelMap.CommonRequired})
            </span>
          </label>
          <div>
            <select id="shipToStateProvinceGeoId" name="shipToStateProvinceGeoId">
              <#if shipToStateProvinceGeoId?has_content>
                <option value='${shipToStateProvinceGeoId!}'>
                  ${shipToStateProvinceGeo!(shipToStateProvinceGeoId!)}
                </option>
              <#else>
                <option value="_NA_">${uiLabelMap.PartyNoState}</option>
              </#if>
            </select>
          </div>
        </div>
        <div>
          <label>${uiLabelMap.PartyPhoneNumber}*</label>
          <span id="advice-required-shipToCountryCode" style="display:none" class="errorMessage"></span>
          <span id="advice-required-shipToAreaCode" style="display:none" class="errorMessage"></span>
          <span id="advice-required-shipToContactNumber" style="display:none" class="errorMessage"></span>
          <span id="shipToPhoneRequired" style="display: none;" class="errorMessage">(${uiLabelMap.CommonRequired})</span>
          <input type="text" name="shipToCountryCode" id="shipToCountryCode" value="${parameters.shipToCountryCode!}"
              size="3" maxlength="3"/>
          - <input type="text" name="shipToAreaCode" id="shipToAreaCode" value="${parameters.shipToAreaCode!}" size="3"
                maxlength="3"/>
          - <input type="text" name="shipToContactNumber" id="shipToContactNumber"
                value="${contactNumber?default("${parameters.shipToContactNumber!}")}" size="6" maxlength="7"/>
          - <input type="text" name="shipToExtension" id="shipToExtension"
                value="${extension?default("${parameters.shipToExtension!}")}" size="3" maxlength="3"/>
        </div>
        <div class="inline">
          <input type="checkbox" class="checkbox" name="useShippingAddressForBilling" id="useShippingAddressForBilling"
              value="Y" <#if parameters.useShippingAddressForBilling?has_content &&
              parameters.useShippingAddressForBilling?default("")=="Y">checked="checked"</#if>/>
          <label for="useShippingAddressForBilling">${uiLabelMap.FacilityBillingAddressSameShipping}</label>
        </div>
      </fieldset>
      <fieldset class="center right" id="billingAddress">
        <legend>${uiLabelMap.PageTitleBillingInformation}</legend>
        <div>
          <label for="billToAddress1">${uiLabelMap.PartyAddressLine1}*
            <span id="advice-required-billToAddress1" style="display: none" class="errorMessage">
              (${uiLabelMap.CommonRequired})
            </span>
          </label>
          <input type="text" name="billToAddress1" id="billToAddress1" class="required"
              value="${parameters.billToAddress1!}"/>
        </div>
        <div>
          <label for="billToAddress2">${uiLabelMap.PartyAddressLine2}</label>
          <input type="text" name="billToAddress2" id="billToAddress2" value="${parameters.billToAddress2!}"/>
        </div>
        <div>
          <label for="billToCity">${uiLabelMap.CommonCity}*
            <span id="advice-required-billToCity" style="display: none" class="errorMessage">
              (${uiLabelMap.CommonRequired})
            </span>
          </label>
          <input type="text" name="billToCity" id="billToCity" class="required" value="${parameters.billToCity!}"/>
        </div>
        <div>
          <label for="billToPostalCode">${uiLabelMap.PartyZipCode}*
            <span id="advice-required-billToPostalCode" style="display: none" class="errorMessage">
              (${uiLabelMap.CommonRequired})
            </span>
          </label>
          <input type="text" name="billToPostalCode" id="billToPostalCode" class="required"
              value="${parameters.billToPostalCode!}" maxlength="10"/>
        </div>
        <div>
          <label for="billToCountryGeoId">${uiLabelMap.CommonCountry}*
            <span id="advice-required-billToCountryGeoId" style="display: none" class="errorMessage">
              (${uiLabelMap.CommonRequired})
            </span>
          </label>
          <select name="billToCountryGeoId" id="billToCountryGeoId" class='required selectBox'>
            <#if billToCountryGeoId??>
              <option value='${billToCountryGeoId!}'>${billToCountryProvinceGeo!(billToCountryGeoId!)}</option>
            </#if>
            ${screens.render("component://common/widget/CommonScreens.xml#countries")}
          </select>
        </div>
        <div id='billToStates'>
          <label for="billToStateProvinceGeoId">${uiLabelMap.CommonState}*
            <span id="advice-required-billToStateProvinceGeoId" style="display: none" class="errorMessage">
              (${uiLabelMap.CommonRequired})
            </span>
          </label>
          <div>
            <select id="billToStateProvinceGeoId" name="billToStateProvinceGeoId">
            <#if billToStateProvinceGeoId?has_content>
              <option value='${billToStateProvinceGeoId!}'>
                ${billToStateProvinceGeo!(billToStateProvinceGeoId!)}
              </option>
            <#else>
              <option value="_NA_">${uiLabelMap.PartyNoState}</option>
            </#if>
            </select>
          </div>
        </div>
        <div>
          <label>${uiLabelMap.PartyPhoneNumber}*</label>
          <span id="advice-required-billToCountryCode" style="display:none" class="errorMessage"></span>
          <span id="advice-required-billToAreaCode" style="display:none" class="errorMessage"></span>
          <span id="advice-required-billToContactNumber" style="display:none" class="errorMessage"></span>
          <span id="billToPhoneRequired" style="display: none;" class="errorMessage">(${uiLabelMap.CommonRequired})</span>
          <input type="text" name="billToCountryCode" id="billToCountryCode" value="${parameters.billToCountryCode!}"
              size="3" maxlength="3"/>
          - <input type="text" name="billToAreaCode" id="billToAreaCode" value="${parameters.billToAreaCode!}" size="3"
                maxlength="3"/>
          - <input type="text" name="billToContactNumber" id="billToContactNumber"
                value="${contactNumber?default("${parameters.billToContactNumber!}")}" size="6" maxlength="7"/>
          - <input type="text" name="billToExtension" id="billToExtension"
                value="${extension?default("${parameters.billToExtension!}")}" size="3" maxlength="3"/>
        </div>
      </fieldset>
      <div><a id="submitNewUserForm" href="javascript:void(0);" class="button">${uiLabelMap.CommonSubmit}</a></div>
    </form>
  </div>
</div>
