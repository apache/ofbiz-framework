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

<div id="form-container" class="screenlet">
  <div class="screenlet-header">
    <div class='boxhead'>&nbsp;${uiLabelMap.EcommerceMyAccount}</div>
  </div>
  <div class="screenlet-body">
    <form id="editUserForm" name="editUserForm" method="post" action="<@ofbizUrl>updateCustomerProfile</@ofbizUrl>">
      <input type="hidden" name="emailContactMechPurposeTypeId" value="PRIMARY_EMAIL"/>
      <input type="hidden" name="emailContactMechId" value="${emailContactMechId?if_exists}"/>

      <div class="left center">
        <div class="screenlet-header"><div class='boxhead'>&nbsp;${uiLabelMap.PartyContactInformation}</div></div>
        <div class="screenlet-body">
          <div class="form-row">
            <div class="field-label"><label for="firstName">${uiLabelMap.PartyFirstName}*<span id="advice-required-firstName" style="display: none" class="errorMessage">(required)</span></label></div>
            <div class="form-field"><input type="text" name="firstName" id="firstName" class="required" value="${firstName?if_exists}" size="30" maxlength="30"></div>
          </div>
          <div class="form-row">
            <div class="field-label"><label for="lastName">${uiLabelMap.PartyLastName}*<span id="advice-required-lastName" style="display: none" class="errorMessage">(required)</span></label></div>
            <div class="form-field"><input type="text" name="lastName" id="lastName" class="required" value="${lastName?if_exists}" size="30" maxlength="30"></div>
          </div>
          <div class="form-row">
            <div class="field-label">
              <label for="emailAddress">
                ${uiLabelMap.CommonEmail}*
                <span id="advice-required-emailAddress" style="display: none" class="errorMessage">(required)</span>
                <span id="advice-validate-email-emailAddress" class="errorMessage" style="display:none">${uiLabelMap.PartyEmailAddressNotFormattedCorrectly}</span>
              </label>
            </div>
            <div class="form-field"><input type="text" class="required validate-email" name="emailAddress" id="emailAddress" value="${emailAddress?if_exists}" size="30" maxlength="255"/></div>
          </div>
        </div>
      </div>

      <div class="center right">
        <div class="screenlet-header"><div class='boxhead'>&nbsp;${uiLabelMap.EcommerceAccountInformation}</div></div>
        <div class="screenlet-body">
          <div class="form-row">
            <div class="field-label"><label for="userName">${uiLabelMap.CommonUsername}*</label></div>
            <div class="form-field"><input type="text" name="userLoginId" id="userLoginId" value="${userLogin.userLoginId?if_exists}" size="30" maxlength="255" <#if userLogin.userLoginId?exists>disabled</#if>></div>
          </div>
          <div class="form-row">
            <div class="field-label"><label for="currentPassword">${uiLabelMap.CommonCurrentPassword}*</label></div>
            <div class="form-field"><input type="password" name="currentPassword" id="currentPassword" value="" size="30" maxlength="16"></div>
          </div>
          <div class="form-row">
            <div class="field-label"><label for="newPassword">${uiLabelMap.CommonNewPassword}*</label></div>
            <div class="form-field"><input type="password" name="newPassword" id="newPassword" value="" size="30" maxlength="16"></div>
          </div>
          <div class="form-row">
            <div class="field-label"><label for="newPasswordVerify">${uiLabelMap.CommonNewPasswordVerify}*</label></div>
            <div class="form-field"><input type="password" name="newPasswordVerify" id="newPasswordVerify" value="" size="30" maxlength="16"></div>
          </div>
        </div>
      </div>
      <div class="form-row">&nbsp;<a id="submitEditUserForm" href="javascript:void(0);" class="buttontext">${uiLabelMap.CommonSubmit}</a></div>
    </form>
  </div>
</div>