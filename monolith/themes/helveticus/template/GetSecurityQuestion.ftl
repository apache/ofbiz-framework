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

<#assign messageTitle = uiLabelMap.CommonForgotYourPassword>
<div id="loginBar"><span>${messageTitle}</span><div id="company-logo"></div></div>
<center>
  <div class="screenlet login-screenlet">
    <div class="screenlet-title-bar">
        <h3>${messageTitle}</h3>
    </div>
    <div class="screenlet-body">
      <form method="post" action="<@ofbizUrl>forgotPassword_step3${previousParams?if_exists}</@ofbizUrl>" name="forgotpassword">
        <table class="basic-table" cellspacing="0">
          <#if userLoginId?has_content>
            <tr>
              <td class="label">${uiLabelMap.CommonUsername}</td>
              <td><input type="text" size="20" name="USERNAME" value="<#if requestParameters.USERNAME?has_content>${requestParameters.USERNAME}<#elseif autoUserLogin?has_content>${autoUserLogin.userLoginId}</#if>" /></td>
            </tr>
            <tr>
              <td colspan="2" align="center">
                <input type="submit" name="GET_PASSWORD_HINT" class="smallSubmit" value="${uiLabelMap.CommonGetPasswordHint}" />&nbsp;
                <input type="submit" name="EMAIL_PASSWORD" class="smallSubmit" value="${uiLabelMap.CommonEmailPassword}" />
              </td>
            </tr>
          <#else>
            <tr>
              <td colspan="2" align="center">
                ${uiLabelMap.PartyUserLoginMissingError}
              </td>
            </tr>
          </#if>
          <tr>
            <td colspan="2" align="center">
              <a href='#' class="buttontext" onclick="window.history.back();">${uiLabelMap.CommonGoBack}</a>
            </td>
          </tr>
        </table>
        <input type="hidden" name="JavaScriptEnabled" value="N" />
      </form>
    </div>
  </div>
</center>