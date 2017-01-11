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
  <form id="editUserForm" method="post" action="<@ofbizUrl>updateCustomerProfile</@ofbizUrl>">
    <fieldset class="left center">
      <input type="hidden" name="emailContactMechPurposeTypeId" value="PRIMARY_EMAIL" />
      <input type="hidden" name="emailContactMechId" value="${emailContactMechId!}" />
      <h3>${uiLabelMap.PartyContactInformation}</h3>
      <div>
        <label for="firstName">
          ${uiLabelMap.PartyFirstName}*
          <span id="advice-required-firstName" style="display: none" class="errorMessage">
            (${uiLabelMap.CommonRequired})
          </span>
        </label>
        <input type="text" name="firstName" id="firstName" class="required" value="${firstName!}" maxlength="30" />
      </div>
      <div>
        <label for="lastName">
          ${uiLabelMap.PartyLastName}*
          <span id="advice-required-lastName" style="display: none" class="errorMessage">
            (${uiLabelMap.CommonRequired})
          </span>
        </label>
        <input type="text" name="lastName" id="lastName" class="required" value="${lastName!}" maxlength="30" />
      </div>
      <div>
        <label for="emailAddress">${uiLabelMap.CommonEmail}*
          <span id="advice-required-emailAddress" style="display: none" class="errorMessage">
            (${uiLabelMap.CommonRequired})
          </span>
          <span id="advice-validate-email-emailAddress" class="errorMessage" style="display:none">
            ${uiLabelMap.PartyEmailAddressNotFormattedCorrectly}
          </span>
        </label>
        <input type="text" class="required validate-email" name="emailAddress" id="emailAddress"
            value="${emailAddress!}" maxlength="255" />
      </div>
    </fieldset>

    <fieldset class="center right">
      <h3>${uiLabelMap.EcommerceAccountInformation}</h3>
      <div>
        <label for="userLoginId">${uiLabelMap.CommonUsername}*</label>
        <input type="text" name="userLoginId" id="userLoginId" value="${userLogin.userLoginId!}"
            maxlength="255" <#if userLogin.userLoginId??>disabled="disabled"</#if> />
      </div>
      <div>
        <label for="currentPassword">${uiLabelMap.CommonCurrentPassword}*</label>
        <input type="password" name="currentPassword" id="currentPassword" value="" maxlength="16" />
      </div>
      <div>
        <label for="newPassword">${uiLabelMap.CommonNewPassword}*</label>
        <input type="password" name="newPassword" id="newPassword" value="" maxlength="16" />
      </div>
      <div>
        <label for="newPasswordVerify">${uiLabelMap.CommonNewPasswordVerify}*</label>
        <input type="password" name="newPasswordVerify" id="newPasswordVerify" value="" maxlength="16" />
      </div>
    </fieldset>
    <div>
      <input type="submit" id="submitEditUserForm" class="button" vlaue="${uiLabelMap.CommonSubmit}">
      <a id="cancelEditUserForm" href="<@ofbizUrl>viewprofile</@ofbizUrl>" class="button">
        ${uiLabelMap.CommonCancel}
      </a>
    </div>
  </form>
</div>