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
<#if ! userLoginId??>
    <#assign userLoginId = requestParameters.USERNAME!>
    <#if ! userLoginId?? && autoUserLogin??>
        <#assign userLoginId = autoUserLogin.userLoginId>
    </#if>
</#if>
    <div id="loginBar">
        <div id="company-logo"></div>
    </div>

    <div class="screenlet login-screenlet">
        <h3>${messageTitle}</h3>
        <div class="screenlet-body">
            <p>${uiLabelMap.CommonReceivePasswordEmail}
            <form method="post" action="<@ofbizUrl>${forgotPasswordTarget?default("forgotPassword")}</@ofbizUrl>" name="forgotpassword">
                <label>
                    ${uiLabelMap.CommonUsername}
                    <input type="text" name="USERNAME" value="${userLoginId!}"/>
                </label>
        
                <#if requestParameters.token??>
                    <input type="hidden" name="token" value="${requestParameters.token}"/>
                <label>
                    ${uiLabelMap.CommonNewPassword}
                    <input type="password" name="newPassword" autocomplete="off" value=""/>
                </label>

                <label>
                    ${uiLabelMap.CommonNewPassword}
                    <input type="password" name="newPassword" autocomplete="off" value=""/>
                </label>

                <label>
                    ${uiLabelMap.CommonNewPasswordVerify}
                    <input type="password" name="newPasswordVerify" autocomplete="off" value=""/>
                </label>

                <input type="submit" class="smallSubmit" value="${uiLabelMap.CommonContinue}"/>

                <#else>
                <div class="button-group">
                    <a href='#' class="buttontext" onclick="window.history.back();">${uiLabelMap.CommonGoBack}</a>
                    <input type="submit" name="EMAIL_PASSWORD" class="smallSubmit" value="${uiLabelMap.CommonSend}" />
                </div>                
                <input type="submit" name="GET_PASSWORD_HINT" value="${uiLabelMap.CommonGetPasswordHint}" class="link" />
                </#if>
                <input type="hidden" name="JavaScriptEnabled" value="N" />
            </form>
        </div>
    </div>
